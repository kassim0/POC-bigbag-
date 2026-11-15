package com.bingbang.account;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import com.bingbang.ApiTestSupport;

class AccountApiTest extends ApiTestSupport {

    private static String uniqueName() {
        return "user_" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static String credentials(String username, String password) {
        return "{\"username\": \"%s\", \"password\": \"%s\"}".formatted(username, password);
    }

    private ResultActions register(String token, String username, String password) throws Exception {
        return mvc.perform(as(token, body(post("/api/auth/register"), credentials(username, password))));
    }

    /** Logs in without any previous session, as a brand new device would; returns the new token. */
    private String loginOnNewDevice(String username, String password) throws Exception {
        return read(mvc.perform(body(post("/api/auth/login"), credentials(username, password)))
                .andExpect(status().isOk()).andReturn()).get("token").asText();
    }

    @Test
    void registeringKeepsTheGuestWallet() throws Exception {
        String token = newGuestToken();
        createList(token, "Liked", game(1, "Hades"));
        mvc.perform(as(token, get("/api/wallet"))).andExpect(jsonPath("$.username").isEmpty());

        String name = uniqueName();
        register(token, " " + name.toUpperCase() + " ", "correct horse")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(name));

        mvc.perform(as(token, get("/api/wallet")))
                .andExpect(jsonPath("$.username").value(name))
                .andExpect(jsonPath("$.lists[0].name").value("Liked"));
    }

    @Test
    void walletIsSharedBetweenDevices() throws Exception {
        String laptop = newGuestToken();
        String name = uniqueName();
        createList(laptop, "Liked", game(1, "Hades"));
        register(laptop, name, "correct horse").andExpect(status().isCreated());

        String phone = loginOnNewDevice(name.toUpperCase(), "correct horse");
        mvc.perform(as(phone, get("/api/wallet")))
                .andExpect(jsonPath("$.username").value(name))
                .andExpect(jsonPath("$.lists[0].games[0].name").value("Hades"));

        // A change on one device is seen on the other.
        createList(phone, "From my phone");
        mvc.perform(as(phone, body(patch("/api/wallet"), "{\"background\": \"night\"}"))).andExpect(status().isOk());
        mvc.perform(as(laptop, get("/api/wallet")))
                .andExpect(jsonPath("$.background").value("night"))
                .andExpect(jsonPath("$.lists.length()").value(2))
                .andExpect(jsonPath("$.lists[1].name").value("From my phone"));
    }

    @Test
    void loginBringsTheGuestListsOfTheDeviceIntoTheAccount() throws Exception {
        String laptop = newGuestToken();
        String name = uniqueName();
        createList(laptop, "Liked");
        register(laptop, name, "correct horse");

        String phoneGuest = newGuestToken();
        createList(phoneGuest, "Made as a guest", game(2, "Celeste"));
        String phone = read(mvc.perform(as(phoneGuest, body(post("/api/auth/login"), credentials(name, "correct horse"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(name))
                .andReturn()).get("token").asText();

        mvc.perform(as(phone, get("/api/wallet")))
                .andExpect(jsonPath("$.lists.length()").value(2))
                .andExpect(jsonPath("$.lists[0].name").value("Liked"))
                .andExpect(jsonPath("$.lists[1].name").value("Made as a guest"))
                .andExpect(jsonPath("$.lists[1].games[0].name").value("Celeste"));
        // The guest token of the device is gone with its (now empty) wallet.
        mvc.perform(as(phoneGuest, get("/api/wallet"))).andExpect(status().isUnauthorized());
    }

    @Test
    void wrongCredentialsAreRejected() throws Exception {
        String name = uniqueName();
        register(newGuestToken(), name, "correct horse");

        mvc.perform(body(post("/api/auth/login"), credentials(name, "wrong password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Wrong username or password"));
        mvc.perform(body(post("/api/auth/login"), credentials("nobody-" + name, "correct horse")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Wrong username or password"));
    }

    @Test
    void usernameMustBeFreeAndValid() throws Exception {
        String name = uniqueName();
        register(newGuestToken(), name, "correct horse").andExpect(status().isCreated());

        register(newGuestToken(), name.toUpperCase(), "another password")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("This username is already taken"));
        register(newGuestToken(), "ab", "correct horse").andExpect(status().isBadRequest());
        register(newGuestToken(), "bad name!", "correct horse").andExpect(status().isBadRequest());
        register(newGuestToken(), uniqueName(), "short").andExpect(status().isBadRequest());
        mvc.perform(body(post("/api/auth/register"), credentials(uniqueName(), "correct horse")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anAccountCannotRegisterTwice() throws Exception {
        String token = newGuestToken();
        register(token, uniqueName(), "correct horse").andExpect(status().isCreated());
        register(token, uniqueName(), "correct horse").andExpect(status().isConflict());
    }

    @Test
    void logoutEndsTheSessionOfThatDeviceOnly() throws Exception {
        String laptop = newGuestToken();
        String name = uniqueName();
        createList(laptop, "Liked");
        register(laptop, name, "correct horse");
        String phone = loginOnNewDevice(name, "correct horse");

        mvc.perform(as(phone, post("/api/auth/logout"))).andExpect(status().isNoContent());
        mvc.perform(as(phone, get("/api/wallet"))).andExpect(status().isUnauthorized());
        mvc.perform(as(laptop, get("/api/wallet"))).andExpect(jsonPath("$.lists[0].name").value("Liked"));

        // The wallet is still there at the next login.
        String again = loginOnNewDevice(name, "correct horse");
        mvc.perform(as(again, get("/api/wallet"))).andExpect(jsonPath("$.lists[0].name").value("Liked"));
    }

    @Test
    void loggingOutAsAGuestKeepsTheGuestWallet() throws Exception {
        String guest = newGuestToken();
        createList(guest, "Mine");
        mvc.perform(as(guest, post("/api/auth/logout"))).andExpect(status().isNoContent());
        mvc.perform(as(guest, get("/api/wallet"))).andExpect(jsonPath("$.lists[0].name").value("Mine"));
    }
}
