package com.bingbang.session;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bingbang.wallet.Wallet;
import com.bingbang.wallet.WalletRepository;

@Service
@Transactional
public class SessionService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AccessTokenRepository tokens;
    private final WalletRepository wallets;

    public SessionService(AccessTokenRepository tokens, WalletRepository wallets) {
        this.tokens = tokens;
        this.wallets = wallets;
    }

    /** Creates an empty wallet and the guest token that owns it. */
    public IssuedToken createGuest() {
        Wallet wallet = wallets.save(new Wallet());
        return issue(wallet.getId(), null);
    }

    public IssuedToken issue(String walletId, Long userId) {
        String token = randomToken();
        tokens.save(new AccessToken(hash(token), walletId, userId));
        return new IssuedToken(token, walletId);
    }

    @Transactional(readOnly = true)
    public Optional<Session> resolve(String token) {
        return tokens.findById(hash(token))
                .map(t -> new Session(t.getWalletId(), t.getUserId(), t.getTokenHash()));
    }

    public static String randomToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public record IssuedToken(String token, String walletId) {
    }
}
