package com.bingbang.wallet;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import com.bingbang.ApiTestSupport;

class GamesListApiTest extends ApiTestSupport {

    @Test
    void walletApiRequiresAToken() throws Exception {
        mvc.perform(get("/api/wallet")).andExpect(status().isUnauthorized());
        mvc.perform(as("not-a-token", get("/api/wallet"))).andExpect(status().isUnauthorized());
    }

    @Test
    void newGuestWalletIsEmpty() throws Exception {
        mvc.perform(as(newGuestToken(), get("/api/wallet")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.background").value("cozy"))
                .andExpect(jsonPath("$.lists.length()").value(0));
    }

    @Test
    void createListWithNameOnly() throws Exception {
        String token = newGuestToken();
        mvc.perform(as(token, body(post("/api/lists"), "{\"name\": \"  Games I liked \"}")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Games I liked"))
                .andExpect(jsonPath("$.games.length()").value(0));
    }

    @Test
    void createListWithGamesOnly() throws Exception {
        String token = newGuestToken();
        String content = "{\"games\": [%s, %s, %s]}".formatted(game(1, "Hades"), game(2, "Celeste"), game(1, "Hades"));
        mvc.perform(as(token, body(post("/api/lists"), content)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(""))
                .andExpect(jsonPath("$.games.length()").value(2))
                .andExpect(jsonPath("$.games[0].name").value("Hades"))
                .andExpect(jsonPath("$.games[0].released").value("2020-01-01"))
                .andExpect(jsonPath("$.games[0].coverUrl").value(coverUrl(1)))
                .andExpect(jsonPath("$.games[1].gameId").value(2));
    }

    @Test
    void createListNeedsANameOrGames() throws Exception {
        String token = newGuestToken();
        mvc.perform(as(token, body(post("/api/lists"), "{\"name\": \"  \", \"games\": []}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("A games list needs at least a name or a game"));
        mvc.perform(as(token, body(post("/api/lists"), "{}"))).andExpect(status().isBadRequest());
    }

    @Test
    void renameAddAndRemoveGames() throws Exception {
        String token = newGuestToken();
        long id = createList(token, "To play");

        mvc.perform(as(token, body(patch("/api/lists/" + id), "{\"name\": \"Wishlist\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Wishlist"));
        mvc.perform(as(token, body(patch("/api/lists/" + id), "{\"name\": \"\"}")))
                .andExpect(status().isBadRequest());

        mvc.perform(as(token, body(post("/api/lists/" + id + "/games"), game(10, "Outer Wilds"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.games.length()").value(1));
        mvc.perform(as(token, body(post("/api/lists/" + id + "/games"), game(10, "Outer Wilds"))))
                .andExpect(status().isConflict());
        mvc.perform(as(token, body(post("/api/lists/" + id + "/games"), game(11, "Tunic"))))
                .andExpect(jsonPath("$.games[1].name").value("Tunic"));
        mvc.perform(as(token, body(post("/api/lists/" + id + "/games"), "{\"name\": \"no id\"}")))
                .andExpect(status().isBadRequest());
        mvc.perform(as(token, body(post("/api/lists/" + id + "/games"), "{\"gameId\": 987654321}")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("This game is not in the catalogue"));

        // A list that has games may lose its name.
        mvc.perform(as(token, body(patch("/api/lists/" + id), "{\"name\": \"\"}")))
                .andExpect(status().isOk());

        mvc.perform(as(token, delete("/api/lists/" + id + "/games/10")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.games.length()").value(1))
                .andExpect(jsonPath("$.games[0].gameId").value(11));
        mvc.perform(as(token, delete("/api/lists/" + id + "/games/10"))).andExpect(status().isNotFound());

        mvc.perform(as(token, get("/api/lists/" + id)))
                .andExpect(jsonPath("$.games.length()").value(1));
    }

    @Test
    void deleteList() throws Exception {
        String token = newGuestToken();
        long keep = createList(token, "Keep", game(1, "Hades"));
        long drop = createList(token, "Drop", game(1, "Hades"));

        mvc.perform(as(token, delete("/api/lists/" + drop))).andExpect(status().isNoContent());
        mvc.perform(as(token, get("/api/lists/" + drop))).andExpect(status().isNotFound());
        mvc.perform(as(token, get("/api/lists")))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(keep));
    }

    @Test
    void walletsAreIsolated() throws Exception {
        String alice = newGuestToken();
        String bob = newGuestToken();
        long id = createList(alice, "Alice's list");

        mvc.perform(as(bob, get("/api/lists/" + id))).andExpect(status().isNotFound());
        mvc.perform(as(bob, body(patch("/api/lists/" + id), "{\"name\": \"hacked\"}"))).andExpect(status().isNotFound());
        mvc.perform(as(bob, body(post("/api/lists/" + id + "/games"), game(1, "Hades")))).andExpect(status().isNotFound());
        mvc.perform(as(bob, delete("/api/lists/" + id))).andExpect(status().isNotFound());
        mvc.perform(as(bob, get("/api/wallet"))).andExpect(jsonPath("$.lists.length()").value(0));
        mvc.perform(as(alice, get("/api/wallet"))).andExpect(jsonPath("$.lists[0].name").value("Alice's list"));
    }

    @Test
    void backgroundCanBeChanged() throws Exception {
        String token = newGuestToken();
        mvc.perform(as(token, body(patch("/api/wallet"), "{\"background\": \"night\"}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.background").value("night"));
        mvc.perform(as(token, get("/api/wallet"))).andExpect(jsonPath("$.background").value("night"));
    }
}
