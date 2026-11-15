package com.bingbang.account;

import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.bingbang.account.AccountService.LoginResult;
import com.bingbang.session.Session;
import com.bingbang.session.SessionService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/auth")
public class AccountController {

    private final AccountService accounts;
    private final SessionService sessions;

    public AccountController(AccountService accounts, SessionService sessions) {
        this.accounts = accounts;
        this.sessions = sessions;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> register(Session session, @Valid @RequestBody RegisterRequest request) {
        return Map.of("username", accounts.register(session, request.username(), request.password()));
    }

    /** The bearer token is optional here: it only serves to bring the guest lists into the account. */
    @PostMapping("/login")
    public LoginResult login(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
                             @Valid @RequestBody LoginRequest request) {
        Optional<Session> current = Optional.ofNullable(authorization)
                .filter(header -> header.startsWith("Bearer "))
                .flatMap(header -> sessions.resolve(header.substring(7).trim()));
        return accounts.login(current, request.username(), request.password());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(Session session) {
        accounts.logout(session);
    }

    // 72 bytes is the most BCrypt takes into account.
    public record RegisterRequest(
            @NotNull @Pattern(regexp = "\\s*[A-Za-z0-9_.-]{3,30}\\s*",
                    message = "must be 3 to 30 letters, digits, '_', '.' or '-'") String username,
            @NotNull @Size(min = 8, max = 72, message = "must be 8 to 72 characters") String password) {
    }

    public record LoginRequest(@NotBlank @Size(max = 100) String username,
                               @NotBlank @Size(max = 72) String password) {
    }
}
