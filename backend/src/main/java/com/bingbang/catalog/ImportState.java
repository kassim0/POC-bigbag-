package com.bingbang.catalog;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * Where the IGDB import stands, so that it can resume after an interruption and later only fetch
 * what changed.
 */
@Entity
public class ImportState {

    public static final String IGDB_GAMES = "igdb-games";

    @Id
    @Column(length = 50)
    private String id;

    /** Games updated after this instant (epoch seconds) are fetched. 0 = everything (first import). */
    @Column(nullable = false)
    private long updatedSince;

    /** Start of the run in progress (epoch seconds); null when no run is in progress. */
    private Long runStartedAt;

    /** Highest IGDB id already saved by the run in progress. */
    @Column(nullable = false)
    private long cursorId;

    private Instant lastCompletedAt;

    /** Games saved (created or updated) since the very first run. */
    @Column(nullable = false)
    private long gamesSaved;

    protected ImportState() {
    }

    public ImportState(String id) {
        this.id = id;
    }

    public long getUpdatedSince() {
        return updatedSince;
    }

    public Long getRunStartedAt() {
        return runStartedAt;
    }

    public long getCursorId() {
        return cursorId;
    }

    public Instant getLastCompletedAt() {
        return lastCompletedAt;
    }

    public long getGamesSaved() {
        return gamesSaved;
    }

    public void startRun(long now) {
        this.runStartedAt = now;
        this.cursorId = 0;
    }

    public void advance(long cursorId, int saved) {
        this.cursorId = cursorId;
        this.gamesSaved += saved;
    }

    /** The next run starts a little before this one did, so that nothing updated meanwhile is missed. */
    public void completeRun(long overlapSeconds) {
        this.updatedSince = Math.max(0, runStartedAt - overlapSeconds);
        this.runStartedAt = null;
        this.cursorId = 0;
        this.lastCompletedAt = Instant.now();
    }
}
