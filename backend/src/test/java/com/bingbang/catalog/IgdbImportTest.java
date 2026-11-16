package com.bingbang.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;

import com.bingbang.catalog.CatalogRepositories.Games;
import com.bingbang.catalog.CatalogRepositories.Genres;
import com.bingbang.catalog.CatalogRepositories.Platforms;
import com.bingbang.catalog.CatalogRepositories.States;
import com.bingbang.catalog.IgdbImportJob.Outcome;
import com.bingbang.catalog.IgdbImportJob.Result;

/** The import against a simulated IGDB (no network, no real credentials) and the real database layer. */
@SpringBootTest
class IgdbImportTest {

    private static final String API = "https://igdb.test/v4/games";
    private static final String TOKEN_URL = "https://twitch.test/oauth2/token";

    @Autowired
    private CatalogWriter writer;
    @Autowired
    private Games games;
    @Autowired
    private Genres genres;
    @Autowired
    private Platforms platforms;
    @Autowired
    private States states;
    @Autowired
    private com.bingbang.wallet.GamesListRepository lists;
    @Autowired
    private TransactionTemplate tx;

    private MockRestServiceServer igdb;
    private IgdbClient client;

    @BeforeEach
    void setUp() {
        // Other tests put games of the catalogue in lists: clear those first.
        tx.executeWithoutResult(status -> lists.deleteAll());
        games.deleteAll();
        genres.deleteAll();
        platforms.deleteAll();
        states.deleteAll();
        client = client("my-client-id", "my-secret", "");
    }

    private IgdbClient client(String id, String secret, String where) {
        RestClient.Builder builder = RestClient.builder();
        igdb = MockRestServiceServer.bindTo(builder).build();
        return new IgdbClient(builder, "https://igdb.test/v4", TOKEN_URL, id, secret, where, 0);
    }

    private IgdbImportJob job(int pageSize, int maxPages) {
        return new IgdbImportJob(client, writer, pageSize, 0, maxPages, 3600, false);
    }

    private void expectToken(String token) {
        igdb.expect(once(), requestTo(TOKEN_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("client_id=my-client-id")))
                .andExpect(content().string(containsString("client_secret=my-secret")))
                .andExpect(content().string(containsString("grant_type=client_credentials")))
                .andRespond(withSuccess("{\"access_token\": \"%s\", \"expires_in\": 5000000, \"token_type\": \"bearer\"}"
                        .formatted(token), MediaType.APPLICATION_JSON));
    }

    /** Expects the query for the page after {@code afterId} and answers with these games. */
    private void expectPage(String token, String where, int limit, String... gamesJson) {
        igdb.expect(once(), requestTo(API))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Client-ID", "my-client-id"))
                .andExpect(header("Authorization", "Bearer " + token))
                .andExpect(content().string("fields " + IgdbClient.FIELDS + "; where " + where + "; sort id asc; limit " + limit + ";"))
                .andRespond(withSuccess("[" + String.join(",", gamesJson) + "]", MediaType.APPLICATION_JSON));
    }

    private static String game(long id, String name) {
        return "{\"id\": %d, \"name\": \"%s\", \"updated_at\": 1700000000}".formatted(id, name);
    }

    private List<String> names() {
        return games.findAll().stream().map(CatalogGame::getName).sorted().collect(Collectors.toList());
    }

    @Test
    void importsGamesWithTheirRelatedData() {
        expectToken("tok");
        expectPage("tok", "id > 0", 500, """
                {"id": 7346, "name": "The Legend of Zelda: Breath of the Wild", "slug": "botw",
                 "summary": "Step into a world of discovery.", "first_release_date": 1488499200,
                 "cover": {"id": 1, "image_id": "co3p2d"},
                 "genres": [{"id": 12, "name": "Role-playing (RPG)"}, {"id": 31, "name": "Adventure"}],
                 "platforms": [{"id": 130, "name": "Nintendo Switch", "abbreviation": "Switch"}, {"id": 41, "name": "Wii U"}],
                 "total_rating": 92.5, "total_rating_count": 1500, "updated_at": 1700000000, "unknown": 1}""",
                """
                {"id": 7400, "name": "Bare game", "updated_at": 1700000001}""",
                """
                {"id": 7401, "updated_at": 1700000002}""");

        Result result = job(500, 0).runImport();

        assertThat(result).isEqualTo(new Result(Outcome.COMPLETED, 1, 3));
        igdb.verify();
        tx.executeWithoutResult(status -> {
            CatalogGame zelda = games.findById(7346L).orElseThrow();
            assertThat(zelda.getName()).isEqualTo("The Legend of Zelda: Breath of the Wild");
            assertThat(zelda.getSlug()).isEqualTo("botw");
            assertThat(zelda.getSummary()).isEqualTo("Step into a world of discovery.");
            assertThat(zelda.getFirstReleaseDate()).isEqualTo(LocalDate.of(2017, 3, 3));
            assertThat(zelda.getCoverImageId()).isEqualTo("co3p2d");
            assertThat(zelda.getRating()).isEqualTo(92.5);
            assertThat(zelda.getRatingCount()).isEqualTo(1500);
            assertThat(zelda.getIgdbUpdatedAt()).isEqualTo(1700000000L);
            assertThat(zelda.getGenres()).extracting(CatalogGenre::getName)
                    .containsExactlyInAnyOrder("Role-playing (RPG)", "Adventure");
            assertThat(zelda.getPlatforms()).extracting(CatalogPlatform::getName, CatalogPlatform::getAbbreviation)
                    .containsExactlyInAnyOrder(org.assertj.core.groups.Tuple.tuple("Nintendo Switch", "Switch"),
                            org.assertj.core.groups.Tuple.tuple("Wii U", null));
            CatalogGame bare = games.findById(7400L).orElseThrow();
            assertThat(bare.getGenres()).isEmpty();
            assertThat(bare.getFirstReleaseDate()).isNull();
        });
        assertThat(games.existsById(7401L)).as("a record without a name is skipped").isFalse();
        assertThat(states.findById(ImportState.IGDB_GAMES).orElseThrow().getGamesSaved()).isEqualTo(2);
    }

    @Test
    void walksTheWholeDatabasePageAfterPageByAscendingId() {
        expectToken("tok");
        expectPage("tok", "id > 0", 2, game(1, "A"), game(5, "B"));
        expectPage("tok", "id > 5", 2, game(8, "C"), game(9, "D"));
        expectPage("tok", "id > 9", 2, game(20, "E")); // a short page: this was the last one

        Result result = job(2, 0).runImport();

        igdb.verify();
        assertThat(result).isEqualTo(new Result(Outcome.COMPLETED, 3, 5));
        assertThat(names()).containsExactly("A", "B", "C", "D", "E");
    }

    @Test
    void interruptedRunResumesAfterItsLastSavedPage() {
        expectToken("tok");
        expectPage("tok", "id > 0", 2, game(1, "A"), game(2, "B"));
        igdb.expect(once(), requestTo(API)).andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> job(2, 0).runImport()).isInstanceOf(IgdbClient.IgdbException.class);
        assertThat(names()).containsExactly("A", "B");
        ImportState interrupted = states.findById(ImportState.IGDB_GAMES).orElseThrow();
        assertThat(interrupted.getCursorId()).isEqualTo(2);
        assertThat(interrupted.getRunStartedAt()).isNotNull();

        // Next run (e.g. after a restart): no request for what is already saved.
        client = client("my-client-id", "my-secret", "");
        expectToken("tok2");
        expectPage("tok2", "id > 2", 2, game(3, "C"));
        assertThat(job(2, 0).runImport().outcome()).isEqualTo(Outcome.COMPLETED);
        igdb.verify();
        assertThat(names()).containsExactly("A", "B", "C");
    }

    @Test
    void runCanBeCappedAndCarriedOnLater() {
        expectToken("tok");
        expectPage("tok", "id > 0", 1, game(1, "A"));
        expectPage("tok", "id > 1", 1, game(2, "B"));
        assertThat(job(1, 2).runImport()).isEqualTo(new Result(Outcome.PAUSED, 2, 2));
        igdb.verify();
        igdb.reset(); // same client (its token is still valid), new expectations

        expectPage("tok", "id > 2", 1, game(3, "C"));
        expectPage("tok", "id > 3", 1);
        assertThat(job(1, 2).runImport()).isEqualTo(new Result(Outcome.COMPLETED, 1, 1));
        igdb.verify();
    }

    @Test
    void followingRunsOnlyFetchWhatChangedAndUpdateExistingGames() {
        expectToken("tok");
        expectPage("tok", "id > 0", 500, """
                {"id": 1, "name": "Old name", "genres": [{"id": 12, "name": "RPG"}], "updated_at": 1700000000}""",
                game(2, "Untouched"));
        job(500, 0).runImport();
        long since = states.findById(ImportState.IGDB_GAMES).orElseThrow().getUpdatedSince();
        long now = System.currentTimeMillis() / 1000;
        // One hour of overlap with the previous run, so that no update can fall between two runs.
        assertThat(since).isBetween(now - 3600 - 60, now - 3600);
        igdb.verify();
        igdb.reset(); // same client (its token is still valid), new expectations

        expectPage("tok", "id > 0 & updated_at > " + since, 500, """
                {"id": 1, "name": "New name", "genres": [{"id": 31, "name": "Adventure"}], "updated_at": 1800000000}""");
        Result second = job(500, 0).runImport();

        igdb.verify();
        assertThat(second).isEqualTo(new Result(Outcome.COMPLETED, 1, 1));
        assertThat(names()).containsExactly("New name", "Untouched");
        tx.executeWithoutResult(status -> assertThat(games.findById(1L).orElseThrow().getGenres())
                .extracting(CatalogGenre::getName).containsExactly("Adventure"));
    }

    @Test
    void retriesWhenRateLimitedAndRenewsARejectedToken() {
        expectToken("old");
        igdb.expect(once(), requestTo(API)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        igdb.expect(once(), requestTo(API)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        igdb.expect(once(), requestTo(API)).andExpect(header("Authorization", "Bearer old"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        expectToken("new");
        expectPage("new", "id > 0", 500, game(1, "A"));

        assertThat(job(500, 0).runImport().outcome()).isEqualTo(Outcome.COMPLETED);
        igdb.verify();
        assertThat(names()).containsExactly("A");
    }

    @Test
    void optionalFilterRestrictsWhatIsImported() {
        client = client("my-client-id", "my-secret", "total_rating_count > 0");
        expectToken("tok");
        expectPage("tok", "id > 0 & (total_rating_count > 0)", 500, game(1, "A"));
        job(500, 0).runImport();
        igdb.verify();
    }

    @Test
    void doesNothingWithoutCredentialsAndNeverLeaksTheSecret() {
        client = client("", "", "");
        assertThat(job(500, 0).runImport().outcome()).isEqualTo(Outcome.NOT_CONFIGURED);
        igdb.verify(); // no request at all

        client = client("my-client-id", "my-secret", "");
        igdb.expect(once(), requestTo(TOKEN_URL)).andExpect(requestTo(not(containsString("my-secret"))))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));
        assertThatThrownBy(() -> job(500, 0).runImport())
                .isInstanceOf(IgdbClient.IgdbException.class)
                .hasMessageNotContaining("my-secret")
                .hasNoCause();
    }
}
