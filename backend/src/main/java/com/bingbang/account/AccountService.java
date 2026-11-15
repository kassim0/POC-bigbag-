package com.bingbang.account;

import java.util.Locale;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bingbang.common.ApiException;
import com.bingbang.session.AccessToken;
import com.bingbang.session.AccessTokenRepository;
import com.bingbang.session.Session;
import com.bingbang.session.SessionService;
import com.bingbang.session.SessionService.IssuedToken;
import com.bingbang.wallet.GamesList;
import com.bingbang.wallet.GamesListRepository;
import com.bingbang.wallet.WalletRepository;

@Service
@Transactional
public class AccountService {

    private final PasswordEncoder passwords = new BCryptPasswordEncoder();
    /** Checked when the username is unknown, so that both failures take about the same time. */
    private final String dummyHash = passwords.encode("no-such-user");

    private final UserAccountRepository users;
    private final AccessTokenRepository tokens;
    private final SessionService sessions;
    private final GamesListRepository lists;
    private final WalletRepository wallets;

    public AccountService(UserAccountRepository users, AccessTokenRepository tokens, SessionService sessions,
                          GamesListRepository lists, WalletRepository wallets) {
        this.users = users;
        this.tokens = tokens;
        this.sessions = sessions;
        this.lists = lists;
        this.wallets = wallets;
    }

    /**
     * Turns the caller's guest wallet into the wallet of a new account: nothing is lost, and the
     * caller's token becomes a login session of that account.
     */
    public String register(Session session, String rawUsername, String password) {
        if (session.userId() != null) {
            throw new ApiException(HttpStatus.CONFLICT, "You are already logged in");
        }
        String username = normalize(rawUsername);
        if (users.existsByUsername(username)) {
            throw new ApiException(HttpStatus.CONFLICT, "This username is already taken");
        }
        UserAccount user;
        try {
            user = users.saveAndFlush(new UserAccount(username, passwords.encode(password), session.walletId()));
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(HttpStatus.CONFLICT, "This username is already taken");
        }
        AccessToken token = tokens.findById(session.tokenHash()).orElseThrow();
        token.setUserId(user.getId());
        return user.getUsername();
    }

    /**
     * Opens a login session on the account's wallet. The lists the caller created as a guest on this
     * device (if any) are moved into the account's wallet rather than lost.
     */
    public LoginResult login(Optional<Session> current, String rawUsername, String password) {
        Optional<UserAccount> found = users.findByUsername(normalize(rawUsername));
        boolean valid = passwords.matches(password, found.map(UserAccount::getPasswordHash).orElse(dummyHash));
        if (found.isEmpty() || !valid) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Wrong username or password");
        }
        UserAccount user = found.get();
        current.ifPresent(session -> {
            if (session.userId() == null && !session.walletId().equals(user.getWalletId())) {
                for (GamesList list : lists.findByWalletIdOrderByIdAsc(session.walletId())) {
                    list.setWallet(wallets.getReferenceById(user.getWalletId()));
                }
                lists.flush();
                wallets.deleteById(session.walletId());
            }
            tokens.deleteById(session.tokenHash());
        });
        IssuedToken issued = sessions.issue(user.getWalletId(), user.getId());
        return new LoginResult(issued.token(), user.getUsername());
    }

    /** Ends a login session. Guest tokens are left alone: they are the only key to a guest wallet. */
    public void logout(Session session) {
        if (session.userId() != null) {
            tokens.deleteById(session.tokenHash());
        }
    }

    private static String normalize(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    public record LoginResult(String token, String username) {
    }
}
