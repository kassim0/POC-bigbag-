package com.bingbang.wallet;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/** A game of the RAWG catalogue placed in a GamesList (a snapshot of what is displayed is kept). */
@Entity
public class GameEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "list_id")
    private GamesList list;

    @Column(nullable = false)
    private long rawgId;

    @Column(nullable = false, length = 300)
    private String name;

    @Column(length = 20)
    private String released;

    @Column(length = 1000)
    private String backgroundImage;

    protected GameEntry() {
    }

    public GameEntry(GamesList list, long rawgId, String name, String released, String backgroundImage) {
        this.list = list;
        this.rawgId = rawgId;
        this.name = name;
        this.released = released;
        this.backgroundImage = backgroundImage;
    }

    public long getRawgId() {
        return rawgId;
    }

    public String getName() {
        return name;
    }

    public String getReleased() {
        return released;
    }

    public String getBackgroundImage() {
        return backgroundImage;
    }
}
