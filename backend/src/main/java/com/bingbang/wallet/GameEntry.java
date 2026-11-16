package com.bingbang.wallet;

import com.bingbang.catalog.CatalogGame;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** A game of the catalogue placed in a GamesList. */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"list_id", "game_id"}))
public class GameEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "list_id")
    private GamesList list;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id")
    private CatalogGame game;

    protected GameEntry() {
    }

    public GameEntry(GamesList list, CatalogGame game) {
        this.list = list;
        this.game = game;
    }

    public CatalogGame getGame() {
        return game;
    }

    public long getGameId() {
        return game.getIgdbId();
    }
}
