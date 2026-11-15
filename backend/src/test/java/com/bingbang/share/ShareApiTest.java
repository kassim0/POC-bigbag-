package com.bingbang.share;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import com.bingbang.ApiTestSupport;

class ShareApiTest extends ApiTestSupport {

    private String share(String token, String content) throws Exception {
        return read(mvc.perform(as(token, body(post("/api/shares"), content)))
                .andExpect(status().isCreated()).andReturn()).get("id").asText();
    }

    @Test
    void wholeWalletIsSharedLiveAndWithoutAnyToken() throws Exception {
        String token = newGuestToken();
        long liked = createList(token, "Liked", game(1, "Hades"));
        mvc.perform(as(token, body(put("/api/lists/" + liked + "/games/1/comment"), "{\"text\": \"Loved it\"}")));
        mvc.perform(as(token, body(patch("/api/wallet"), "{\"background\": \"night\"}")));

        String shareId = share(token, "{}");

        mvc.perform(get("/api/shared/" + shareId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wholeWallet").value(true))
                .andExpect(jsonPath("$.background").value("night"))
                .andExpect(jsonPath("$.lists", hasSize(1)))
                .andExpect(jsonPath("$.lists[0].name").value("Liked"))
                .andExpect(jsonPath("$.lists[0].games[0].name").value("Hades"))
                .andExpect(jsonPath("$.lists[0].games[0].comment").value("Loved it"))
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.username").doesNotExist());

        // The link shows the wallet as it is now, lists created afterwards included.
        createList(token, "To play");
        mvc.perform(get("/api/shared/" + shareId)).andExpect(jsonPath("$.lists", hasSize(2)));
    }

    @Test
    void singleListAndSelectionOnlyShowWhatWasShared() throws Exception {
        String token = newGuestToken();
        long a = createList(token, "A");
        long b = createList(token, "B");
        long c = createList(token, "C");

        String single = share(token, "{\"listIds\": [" + b + "]}");
        mvc.perform(get("/api/shared/" + single))
                .andExpect(jsonPath("$.wholeWallet").value(false))
                .andExpect(jsonPath("$.lists", hasSize(1)))
                .andExpect(jsonPath("$.lists[0].name").value("B"));

        String selection = share(token, "{\"listIds\": [" + c + ", " + a + "]}");
        mvc.perform(get("/api/shared/" + selection))
                .andExpect(jsonPath("$.lists", hasSize(2)))
                .andExpect(jsonPath("$.lists[0].name").value("A"))
                .andExpect(jsonPath("$.lists[1].name").value("C"));

        // New lists do not leak into a selection.
        createList(token, "D");
        mvc.perform(get("/api/shared/" + selection)).andExpect(jsonPath("$.lists", hasSize(2)));

        mvc.perform(as(token, get("/api/shares")))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(single))
                .andExpect(jsonPath("$[0].wholeWallet").value(false))
                .andExpect(jsonPath("$[0].listIds[0]").value(b))
                .andExpect(jsonPath("$[1].listIds", hasSize(2)));
    }

    @Test
    void shareLinkIsReadOnly() throws Exception {
        String token = newGuestToken();
        long id = createList(token, "Liked", game(1, "Hades"));
        String shareId = share(token, "{}");

        // The link has no write endpoint...
        mvc.perform(body(post("/api/shared/" + shareId), "{}")).andExpect(status().isMethodNotAllowed());
        mvc.perform(body(patch("/api/shared/" + shareId), "{}")).andExpect(status().isMethodNotAllowed());
        mvc.perform(delete("/api/shared/" + shareId)).andExpect(status().isMethodNotAllowed());
        // ...and its id is not a wallet token.
        mvc.perform(as(shareId, get("/api/wallet"))).andExpect(status().isUnauthorized());
        mvc.perform(as(shareId, body(patch("/api/lists/" + id), "{\"name\": \"hacked\"}")))
                .andExpect(status().isUnauthorized());
        mvc.perform(as(shareId, delete("/api/lists/" + id))).andExpect(status().isUnauthorized());
        mvc.perform(as(shareId, delete("/api/shares/" + shareId))).andExpect(status().isUnauthorized());
        mvc.perform(body(patch("/api/lists/" + id), "{\"name\": \"hacked\"}")).andExpect(status().isUnauthorized());

        mvc.perform(get("/api/shared/" + shareId)).andExpect(jsonPath("$.lists[0].name").value("Liked"));
    }

    @Test
    void onlyOwnListsCanBeShared() throws Exception {
        String alice = newGuestToken();
        String bob = newGuestToken();
        long aliceList = createList(alice, "Alice's");
        long bobList = createList(bob, "Bob's");

        mvc.perform(as(bob, body(post("/api/shares"), "{\"listIds\": [" + bobList + ", " + aliceList + "]}")))
                .andExpect(status().isNotFound());
        mvc.perform(as(bob, body(post("/api/shares"), "{\"listIds\": [999999]}"))).andExpect(status().isNotFound());
        mvc.perform(post("/api/shares")).andExpect(status().isUnauthorized());
    }

    @Test
    void revokedOrUnknownLinkShowsNothing() throws Exception {
        String token = newGuestToken();
        createList(token, "Liked");
        String shareId = share(token, "{}");

        mvc.perform(as(newGuestToken(), delete("/api/shares/" + shareId))).andExpect(status().isNotFound());
        mvc.perform(get("/api/shared/" + shareId)).andExpect(status().isOk());

        mvc.perform(as(token, delete("/api/shares/" + shareId))).andExpect(status().isNoContent());
        mvc.perform(get("/api/shared/" + shareId)).andExpect(status().isNotFound());
        mvc.perform(get("/api/shared/nope")).andExpect(status().isNotFound());
        mvc.perform(as(token, get("/api/shares"))).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void deletedListsLeaveTheSharedView() throws Exception {
        String token = newGuestToken();
        long a = createList(token, "A");
        long b = createList(token, "B");
        String shareId = share(token, "{\"listIds\": [" + a + ", " + b + "]}");

        mvc.perform(as(token, delete("/api/lists/" + a))).andExpect(status().isNoContent());
        mvc.perform(get("/api/shared/" + shareId))
                .andExpect(jsonPath("$.lists", hasSize(1)))
                .andExpect(jsonPath("$.lists[0].name").value("B"));
        mvc.perform(as(token, get("/api/shares"))).andExpect(jsonPath("$[0].listIds", hasSize(1)));

        mvc.perform(as(token, delete("/api/lists/" + b))).andExpect(status().isNoContent());
        mvc.perform(get("/api/shared/" + shareId)).andExpect(status().isNotFound());
        mvc.perform(as(token, get("/api/shares"))).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void guestLinksFollowTheListsIntoTheAccountAtLogin() throws Exception {
        String owner = newGuestToken();
        createList(owner, "Private account list");
        String name = "share_" + System.nanoTime();
        String credentials = "{\"username\": \"%s\", \"password\": \"correct horse\"}".formatted(name);
        mvc.perform(as(owner, body(post("/api/auth/register"), credentials))).andExpect(status().isCreated());

        String guest = newGuestToken();
        long guestList = createList(guest, "Guest list");
        String listLink = share(guest, "{\"listIds\": [" + guestList + "]}");
        String walletLink = share(guest, "{}");

        mvc.perform(as(guest, body(post("/api/auth/login"), credentials))).andExpect(status().isOk());

        mvc.perform(get("/api/shared/" + listLink))
                .andExpect(jsonPath("$.lists", hasSize(1)))
                .andExpect(jsonPath("$.lists[0].name").value("Guest list"));
        // The "whole guest wallet" link must not turn into a link to the whole account wallet.
        mvc.perform(get("/api/shared/" + walletLink)).andExpect(status().isNotFound());
    }
}
