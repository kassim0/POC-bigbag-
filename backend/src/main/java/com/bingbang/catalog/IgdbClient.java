package com.bingbang.catalog;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Client of the IGDB API (https://api-docs.igdb.com). Authentication is a Twitch "client credentials"
 * app token, fetched on demand and renewed when it expires or is rejected.
 */
@Component
public class IgdbClient {

    /** What BingBang keeps of a game. Dotted fields make IGDB expand the related records inline. */
    static final String FIELDS = "name,slug,summary,first_release_date,cover.image_id,genres.name,"
            + "platforms.name,platforms.abbreviation,total_rating,total_rating_count,updated_at";

    private static final int MAX_ATTEMPTS = 5;

    private final RestClient rest;
    private final String apiUrl;
    private final String tokenUrl;
    private final String clientId;
    private final String clientSecret;
    private final String extraFilter;
    private final long retryPauseMs;

    private String accessToken;
    private Instant tokenExpiry = Instant.MIN;

    public IgdbClient(RestClient.Builder builder,
                      @Value("${bingbang.igdb.api-url:https://api.igdb.com/v4}") String apiUrl,
                      @Value("${bingbang.igdb.token-url:https://id.twitch.tv/oauth2/token}") String tokenUrl,
                      @Value("${bingbang.igdb.client-id:}") String clientId,
                      @Value("${bingbang.igdb.client-secret:}") String clientSecret,
                      @Value("${bingbang.igdb.import.where:}") String extraFilter,
                      @Value("${bingbang.igdb.import.retry-pause-ms:1000}") long retryPauseMs) {
        this.rest = builder.build();
        this.apiUrl = apiUrl;
        this.tokenUrl = tokenUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.extraFilter = extraFilter == null ? "" : extraFilter.trim();
        this.retryPauseMs = retryPauseMs;
    }

    public boolean isConfigured() {
        return !clientId.isBlank() && !clientSecret.isBlank();
    }

    /**
     * One page of games whose id is above {@code afterId}, by ascending id, restricted to those
     * updated after {@code updatedSince} (epoch seconds, 0 = no restriction).
     * Paging on the id rather than on an offset stays correct while IGDB's data changes under us.
     */
    public List<IgdbGame> fetchGames(long afterId, long updatedSince, int limit) {
        String query = buildQuery(afterId, updatedSince, limit);
        boolean tokenRenewed = false;
        for (int attempt = 1; ; attempt++) {
            try {
                List<IgdbGame> games = rest.post()
                        .uri(apiUrl + "/games")
                        .header("Client-ID", clientId)
                        .headers(h -> h.setBearerAuth(token()))
                        .contentType(MediaType.TEXT_PLAIN)
                        .accept(MediaType.APPLICATION_JSON)
                        .body(query)
                        .retrieve()
                        .body(new ParameterizedTypeReference<List<IgdbGame>>() {
                        });
                return games == null ? List.of() : games;
            } catch (HttpStatusCodeException e) {
                int status = e.getStatusCode().value();
                if (status == HttpStatus.UNAUTHORIZED.value() && !tokenRenewed) {
                    tokenRenewed = true; // the token was revoked or expired early: get a new one, once
                    invalidateToken();
                } else if ((status == HttpStatus.TOO_MANY_REQUESTS.value() || status >= 500) && attempt < MAX_ATTEMPTS) {
                    pause(retryPauseMs * (1L << (attempt - 1)));
                } else {
                    throw new IgdbException("IGDB answered " + status + " to the games query", e);
                }
            } catch (RestClientException e) {
                if (attempt >= MAX_ATTEMPTS) {
                    throw new IgdbException("IGDB is unreachable", e);
                }
                pause(retryPauseMs * (1L << (attempt - 1)));
            }
        }
    }

    String buildQuery(long afterId, long updatedSince, int limit) {
        StringBuilder where = new StringBuilder("id > ").append(afterId);
        if (updatedSince > 0) {
            where.append(" & updated_at > ").append(updatedSince);
        }
        if (!extraFilter.isEmpty()) {
            where.append(" & (").append(extraFilter).append(')');
        }
        return "fields " + FIELDS + "; where " + where + "; sort id asc; limit " + limit + ";";
    }

    private synchronized String token() {
        if (accessToken == null || Instant.now().isAfter(tokenExpiry)) {
            if (!isConfigured()) {
                throw new IgdbException("IGDB credentials are not configured (IGDB_CLIENT_ID / IGDB_CLIENT_SECRET)", null);
            }
            // Sent as a form body, not in the URL, so that the secret stays out of access logs.
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("client_id", clientId);
            form.add("client_secret", clientSecret);
            form.add("grant_type", "client_credentials");
            TokenResponse response;
            try {
                response = rest.post().uri(tokenUrl)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .body(form)
                        .retrieve()
                        .body(TokenResponse.class);
            } catch (RestClientException e) {
                // No cause attached: its message could echo the request.
                throw new IgdbException("Twitch refused to deliver an IGDB access token", null);
            }
            if (response == null || response.accessToken() == null) {
                throw new IgdbException("Twitch returned no IGDB access token", null);
            }
            accessToken = response.accessToken();
            // Renew a minute early rather than have a request fail on an expired token.
            tokenExpiry = Instant.now().plus(Duration.ofSeconds(Math.max(0, response.expiresIn() - 60)));
        }
        return accessToken;
    }

    private synchronized void invalidateToken() {
        accessToken = null;
    }

    private static void pause(long millis) {
        if (millis <= 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IgdbException("Interrupted while waiting to retry IGDB", e);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TokenResponse(@JsonProperty("access_token") String accessToken, @JsonProperty("expires_in") long expiresIn) {
    }

    public static class IgdbException extends RuntimeException {
        public IgdbException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
