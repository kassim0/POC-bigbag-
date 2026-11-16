package com.bingbang.catalog;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.bingbang.ApiTestSupport;

class GameSearchTest extends ApiTestSupport {

    private void seed(long id, String name, Integer ratingCount) {
        CatalogGame game = new CatalogGame(id);
        game.setName(name);
        game.setRatingCount(ratingCount);
        game.setRating(ratingCount == null ? null : 80.0);
        game.setFirstReleaseDate(LocalDate.of(2017, 3, 3));
        game.setCoverImageId("cover" + id);
        catalog.save(game);
    }

    @BeforeEach
    void seedCatalogue() {
        seed(900001, "Zzqx Legends", 10);
        seed(900002, "Zzqx", 500);
        seed(900003, "The Zzqx Chronicles", 9000);
        seed(900004, "ZZQX II", null);
        seed(900005, "Unrelated 100%_done", 1);
    }

    @Test
    void findsGamesByNameWithoutCaringAboutCase() throws Exception {
        mvc.perform(get("/api/games/search").param("q", " zZqX "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(4))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.hasNext").value(false))
                // Names starting with the text first, then the most rated.
                .andExpect(jsonPath("$.results[*].gameId", contains(900002, 900001, 900004, 900003)))
                .andExpect(jsonPath("$.results[0].name").value("Zzqx"))
                .andExpect(jsonPath("$.results[0].released").value("2017-03-03"))
                .andExpect(jsonPath("$.results[0].coverUrl")
                        .value("https://images.igdb.com/igdb/image/upload/t_cover_big/cover900002.jpg"))
                .andExpect(jsonPath("$.results[0].rating").value(80.0));
    }

    @Test
    void resultsArePaged() throws Exception {
        mvc.perform(get("/api/games/search").param("q", "zzqx").param("pageSize", "3"))
                .andExpect(jsonPath("$.results.length()").value(3))
                .andExpect(jsonPath("$.count").value(4))
                .andExpect(jsonPath("$.hasNext").value(true));
        mvc.perform(get("/api/games/search").param("q", "zzqx").param("pageSize", "3").param("page", "2"))
                .andExpect(jsonPath("$.results[*].gameId", contains(900003)))
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    void wildcardsOfTheUserAreSearchedLiterally() throws Exception {
        mvc.perform(get("/api/games/search").param("q", "100%_d"))
                .andExpect(jsonPath("$.results[*].gameId", contains(900005)));
        mvc.perform(get("/api/games/search").param("q", "zz_x")).andExpect(jsonPath("$.count").value(0));
        mvc.perform(get("/api/games/search").param("q", "z%x")).andExpect(jsonPath("$.count").value(0));
    }

    @Test
    void unknownNameFindsNothing() throws Exception {
        mvc.perform(get("/api/games/search").param("q", "no such game qqq"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0))
                .andExpect(jsonPath("$.results.length()").value(0));
    }

    @Test
    void invalidQueriesAreRejected() throws Exception {
        mvc.perform(get("/api/games/search").param("q", "  ")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/games/search")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/games/search").param("q", "x".repeat(101))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/games/search").param("q", "zzqx").param("pageSize", "500")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/games/search").param("q", "zzqx").param("page", "0")).andExpect(status().isBadRequest());
    }
}
