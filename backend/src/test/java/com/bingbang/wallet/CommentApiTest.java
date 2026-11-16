package com.bingbang.wallet;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import com.bingbang.ApiTestSupport;

class CommentApiTest extends ApiTestSupport {

    private ResultActions comment(String token, long listId, long gameId, String text) throws Exception {
        return mvc.perform(as(token, body(put("/api/lists/%d/games/%d/comment".formatted(listId, gameId)),
                "{\"text\": \"%s\"}".formatted(text))));
    }

    @Test
    void commentIsSetUpdatedAndDeleted() throws Exception {
        String token = newGuestToken();
        long id = createList(token, "Liked", game(1, "Hades"), game(2, "Celeste"));

        mvc.perform(as(token, get("/api/lists/" + id))).andExpect(jsonPath("$.games[0].comment").isEmpty());

        comment(token, id, 1, " Best roguelike ").andExpect(status().isOk())
                .andExpect(jsonPath("$.games[0].comment").value("Best roguelike"))
                .andExpect(jsonPath("$.games[1].comment").isEmpty());
        comment(token, id, 1, "Still the best").andExpect(jsonPath("$.games[0].comment").value("Still the best"));
        mvc.perform(as(token, get("/api/wallet")))
                .andExpect(jsonPath("$.lists[0].games[0].comment").value("Still the best"));

        mvc.perform(as(token, delete("/api/lists/" + id + "/games/1/comment")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.games[0].comment").isEmpty());
        mvc.perform(as(token, delete("/api/lists/" + id + "/games/1/comment"))).andExpect(status().isNotFound());
    }

    @Test
    void commentNeedsTextAndAGameOfTheList() throws Exception {
        String token = newGuestToken();
        long id = createList(token, "Liked", game(1, "Hades"));

        comment(token, id, 1, "  ").andExpect(status().isBadRequest());
        comment(token, id, 99, "not in the list").andExpect(status().isNotFound());
        comment(newGuestToken(), id, 1, "someone else").andExpect(status().isNotFound());
    }

    @Test
    void commentSurvivesRemovingTheGameAndComesBackWhenItIsAddedAgain() throws Exception {
        String token = newGuestToken();
        long id = createList(token, "Liked", game(1, "Hades"));
        comment(token, id, 1, "Keep me");

        mvc.perform(as(token, delete("/api/lists/" + id + "/games/1")))
                .andExpect(jsonPath("$.games.length()").value(0));
        mvc.perform(as(token, body(post("/api/lists/" + id + "/games"), game(1, "Hades"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.games[0].comment").value("Keep me"));

        // Deleting the commentary itself is what removes it for good.
        mvc.perform(as(token, delete("/api/lists/" + id + "/games/1/comment"))).andExpect(status().isOk());
        mvc.perform(as(token, delete("/api/lists/" + id + "/games/1"))).andExpect(status().isOk());
        mvc.perform(as(token, body(post("/api/lists/" + id + "/games"), game(1, "Hades"))))
                .andExpect(jsonPath("$.games[0].comment").isEmpty());
    }

    @Test
    void commentOfARemovedGameCanStillBeDeleted() throws Exception {
        String token = newGuestToken();
        long id = createList(token, "Liked", game(1, "Hades"), game(2, "Celeste"));
        comment(token, id, 1, "Forget me");
        mvc.perform(as(token, delete("/api/lists/" + id + "/games/1"))).andExpect(status().isOk());

        mvc.perform(as(token, delete("/api/lists/" + id + "/games/1/comment"))).andExpect(status().isOk());
        mvc.perform(as(token, body(post("/api/lists/" + id + "/games"), game(1, "Hades"))))
                .andExpect(jsonPath("$.games[1].comment").isEmpty());
    }

    @Test
    void sameGameHasADifferentCommentInEachList() throws Exception {
        String token = newGuestToken();
        long liked = createList(token, "Liked", game(1, "Hades"));
        long replay = createList(token, "To replay", game(1, "Hades"));

        comment(token, liked, 1, "Loved it");
        comment(token, replay, 1, "Try the hard mode");

        mvc.perform(as(token, get("/api/lists/" + liked))).andExpect(jsonPath("$.games[0].comment").value("Loved it"));
        mvc.perform(as(token, get("/api/lists/" + replay)))
                .andExpect(jsonPath("$.games[0].comment").value("Try the hard mode"));
    }

    @Test
    void commentCanBeTransferredToAnotherList() throws Exception {
        String token = newGuestToken();
        long liked = createList(token, "Liked", game(1, "Hades"), game(2, "Celeste"));
        long replay = createList(token, "To replay", game(1, "Hades"));
        comment(token, liked, 1, "Loved it");
        comment(token, liked, 2, "Hard but fair");
        comment(token, replay, 1, "Old note");

        String transfer = "/api/lists/" + liked + "/games/%d/comment/transfer";
        mvc.perform(as(token, body(post(transfer.formatted(1)), "{\"targetListId\": " + replay + "}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(replay))
                .andExpect(jsonPath("$.games[0].comment").value("Loved it"));
        // The source keeps its commentary.
        mvc.perform(as(token, get("/api/lists/" + liked))).andExpect(jsonPath("$.games[0].comment").value("Loved it"));

        // Target must contain the game, differ from the source, and belong to the same wallet.
        mvc.perform(as(token, body(post(transfer.formatted(2)), "{\"targetListId\": " + replay + "}")))
                .andExpect(status().isNotFound());
        mvc.perform(as(token, body(post(transfer.formatted(1)), "{\"targetListId\": " + liked + "}")))
                .andExpect(status().isBadRequest());
        String other = newGuestToken();
        long foreign = createList(other, "Foreign", game(1, "Hades"));
        mvc.perform(as(token, body(post(transfer.formatted(1)), "{\"targetListId\": " + foreign + "}")))
                .andExpect(status().isNotFound());
        mvc.perform(as(other, get("/api/lists/" + foreign))).andExpect(jsonPath("$.games[0].comment").isEmpty());
    }

    @Test
    void deletingAListDeletesItsComments() throws Exception {
        String token = newGuestToken();
        long id = createList(token, "Liked", game(1, "Hades"));
        comment(token, id, 1, "Bye");
        mvc.perform(as(token, delete("/api/lists/" + id))).andExpect(status().isNoContent());
        mvc.perform(as(token, get("/api/lists"))).andExpect(jsonPath("$.length()").value(0));
    }
}
