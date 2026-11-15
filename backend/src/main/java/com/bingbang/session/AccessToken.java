package com.bingbang.session;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * Bearer token giving access to a wallet. Only the SHA-256 of the token is stored.
 * A token without userId is a guest token; with a userId it is a login session of that account.
 */
@Entity
public class AccessToken {

    @Id
    @Column(length = 64)
    private String tokenHash;

    @Column(nullable = false, length = 36)
    private String walletId;

    private Long userId;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected AccessToken() {
    }

    public AccessToken(String tokenHash, String walletId, Long userId) {
        this.tokenHash = tokenHash;
        this.walletId = walletId;
        this.userId = userId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public String getWalletId() {
        return walletId;
    }

    public void setWalletId(String walletId) {
        this.walletId = walletId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
