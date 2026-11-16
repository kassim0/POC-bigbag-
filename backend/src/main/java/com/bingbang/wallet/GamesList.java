package com.bingbang.wallet;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;

@Entity
public class GamesList {

    public static final int NAME_MAX_LENGTH = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id")
    private Wallet wallet;

    @Column(nullable = false, length = NAME_MAX_LENGTH)
    private String name = "";

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "list", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<GameEntry> games = new ArrayList<>();

    /** Commentaries of this list, including those of games that were removed from it. */
    @OneToMany(mappedBy = "list", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GameComment> comments = new ArrayList<>();

    protected GamesList() {
    }

    public GamesList(Wallet wallet, String name) {
        this.wallet = wallet;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public Wallet getWallet() {
        return wallet;
    }

    public void setWallet(Wallet wallet) {
        this.wallet = wallet;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<GameEntry> getGames() {
        return games;
    }

    public List<GameComment> getComments() {
        return comments;
    }

    public Optional<GameComment> commentOf(long gameId) {
        return comments.stream().filter(c -> c.getGameId() == gameId).findFirst();
    }

    public boolean contains(long gameId) {
        return games.stream().anyMatch(g -> g.getGameId() == gameId);
    }
}
