package com.bingbang.wallet;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * The commentary of a user on a game, in one GamesList.
 * It is attached to the list and the RAWG id, not to the {@link GameEntry}: removing the game from
 * the list keeps the commentary, which shows up again if the game is added back.
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"list_id", "rawg_id"}))
public class GameComment {

    public static final int MAX_LENGTH = 2000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "list_id")
    private GamesList list;

    @Column(name = "rawg_id", nullable = false)
    private long rawgId;

    @Column(nullable = false, length = MAX_LENGTH)
    private String text;

    protected GameComment() {
    }

    public GameComment(GamesList list, long rawgId, String text) {
        this.list = list;
        this.rawgId = rawgId;
        this.text = text;
    }

    public long getRawgId() {
        return rawgId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
