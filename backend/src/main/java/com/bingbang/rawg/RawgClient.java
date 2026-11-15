package com.bingbang.rawg;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.bingbang.common.ApiException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Thin client for the RAWG video games database (https://rawg.io/apidocs). */
@Component
public class RawgClient {

    private final RestClient rest;
    private final String apiKey;

    public RawgClient(RestClient.Builder builder,
                      @Value("${rawg.base-url}") String baseUrl,
                      @Value("${rawg.api-key:}") String apiKey) {
        this.rest = builder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
    }

    public GameSearchResult search(String query, int page, int pageSize) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "The RAWG API key is not configured (RAWG_API_KEY)");
        }
        RawgPage body;
        try {
            body = rest.get()
                    .uri(uri -> uri.path("/games")
                            .queryParam("key", apiKey)
                            .queryParam("search", query)
                            .queryParam("page", page)
                            .queryParam("page_size", pageSize)
                            .build())
                    .retrieve()
                    .body(RawgPage.class);
        } catch (RestClientException e) {
            // The message is deliberately generic: the upstream URL contains the API key.
            throw new ApiException(HttpStatus.BAD_GATEWAY, "The RAWG catalogue is unavailable");
        }
        if (body == null || body.results() == null) {
            return new GameSearchResult(0, page, false, List.of());
        }
        List<GameSummary> games = body.results().stream()
                .map(g -> new GameSummary(g.id(), g.name(), g.released(), g.backgroundImage(), g.rating()))
                .toList();
        return new GameSearchResult(body.count(), page, body.next() != null, games);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RawgPage(int count, String next, List<RawgGame> results) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RawgGame(long id, String name, String released,
                    @JsonProperty("background_image") String backgroundImage, Double rating) {
    }
}
