package com.bingbang.wallet;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/** A gamesWallet: the personal space of a user (guest or account) = its GamesLists + a background. */
@Entity
public class Wallet {

    public static final String DEFAULT_BACKGROUND = "cozy";

    @Id
    @Column(length = 36)
    private String id = UUID.randomUUID().toString();

    @Column(nullable = false, length = 50)
    private String background = DEFAULT_BACKGROUND;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public String getId() {
        return id;
    }

    public String getBackground() {
        return background;
    }

    public void setBackground(String background) {
        this.background = background;
    }
}
