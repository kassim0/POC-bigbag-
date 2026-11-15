package com.bingbang.session;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.bingbang.session.SessionService.IssuedToken;

@RestController
@RequestMapping("/api/session")
public class SessionController {

    private final SessionService sessions;

    public SessionController(SessionService sessions) {
        this.sessions = sessions;
    }

    /** Starts a guest session: the client keeps the returned token to find its wallet again. */
    @PostMapping("/guest")
    @ResponseStatus(HttpStatus.CREATED)
    public IssuedToken guest() {
        return sessions.createGuest();
    }
}
