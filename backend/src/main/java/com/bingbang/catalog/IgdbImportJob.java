package com.bingbang.catalog;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Batch that copies IGDB's games into BingBang's own catalogue.
 *
 * <ul>
 *   <li>First run: walks the whole IGDB database, by ascending id, {@code page-size} games at a time.</li>
 *   <li>Following runs: only the games IGDB changed since the previous run started.</li>
 *   <li>Its position is saved with every page, so an interrupted run resumes where it stopped.</li>
 *   <li>It stays under IGDB's rate limit (4 requests per second) by pausing between pages.</li>
 * </ul>
 */
@Component
public class IgdbImportJob implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(IgdbImportJob.class);

    private final IgdbClient igdb;
    private final CatalogWriter writer;
    private final int pageSize;
    private final long pauseMs;
    private final int maxPagesPerRun;
    private final long overlapSeconds;
    private final boolean runOnStartup;
    private final AtomicBoolean running = new AtomicBoolean();

    public IgdbImportJob(IgdbClient igdb, CatalogWriter writer,
                         @Value("${bingbang.igdb.import.page-size:500}") int pageSize,
                         @Value("${bingbang.igdb.import.pause-ms:300}") long pauseMs,
                         @Value("${bingbang.igdb.import.max-pages-per-run:0}") int maxPagesPerRun,
                         @Value("${bingbang.igdb.import.overlap-seconds:3600}") long overlapSeconds,
                         @Value("${bingbang.igdb.import.run-on-startup:false}") boolean runOnStartup) {
        if (pageSize < 1 || pageSize > 500) {
            throw new IllegalArgumentException("bingbang.igdb.import.page-size must be between 1 and 500 (IGDB's maximum)");
        }
        this.igdb = igdb;
        this.writer = writer;
        this.pageSize = pageSize;
        this.pauseMs = pauseMs;
        this.maxPagesPerRun = maxPagesPerRun;
        this.overlapSeconds = overlapSeconds;
        this.runOnStartup = runOnStartup;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (runOnStartup) {
            // In its own thread: a first import takes a while and must not delay the application.
            Thread.ofPlatform().name("igdb-import").daemon().start(this::runSafely);
        }
    }

    /** Disabled unless bingbang.igdb.import.cron is set ("-" means never). */
    @Scheduled(cron = "${bingbang.igdb.import.cron:-}")
    public void runSafely() {
        try {
            runImport();
        } catch (RuntimeException e) {
            log.error("IGDB import stopped: {} (it will resume from its last saved page)", e.getMessage());
        }
    }

    /** Runs (or resumes) an import; returns what it did. Does nothing if one is already running. */
    public Result runImport() {
        if (!igdb.isConfigured()) {
            log.warn("IGDB import skipped: set IGDB_CLIENT_ID and IGDB_CLIENT_SECRET");
            return new Result(Outcome.NOT_CONFIGURED, 0, 0);
        }
        if (!running.compareAndSet(false, true)) {
            return new Result(Outcome.ALREADY_RUNNING, 0, 0);
        }
        try {
            ImportState state = writer.beginOrResume(Instant.now().getEpochSecond());
            long since = state.getUpdatedSince();
            long cursor = state.getCursorId();
            log.info("IGDB import: {} from id {}", since == 0 ? "full import" : "changes since " + Instant.ofEpochSecond(since), cursor);

            int pages = 0;
            long games = 0;
            while (true) {
                List<IgdbGame> page = igdb.fetchGames(cursor, since, pageSize);
                if (!page.isEmpty()) {
                    cursor = writer.savePage(page).getCursorId();
                    pages++;
                    games += page.size();
                    log.info("IGDB import: {} games fetched so far, up to id {}", games, cursor);
                }
                if (page.size() < pageSize) {
                    writer.complete(overlapSeconds);
                    log.info("IGDB import complete: {} games in {} pages", games, pages);
                    return new Result(Outcome.COMPLETED, pages, games);
                }
                if (maxPagesPerRun > 0 && pages >= maxPagesPerRun) {
                    log.info("IGDB import paused after {} pages; the next run carries on from id {}", pages, cursor);
                    return new Result(Outcome.PAUSED, pages, games);
                }
                pause();
            }
        } finally {
            running.set(false);
        }
    }

    private void pause() {
        if (pauseMs <= 0) {
            return;
        }
        try {
            Thread.sleep(pauseMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IgdbClient.IgdbException("IGDB import interrupted", e);
        }
    }

    public enum Outcome { COMPLETED, PAUSED, ALREADY_RUNNING, NOT_CONFIGURED }

    public record Result(Outcome outcome, int pages, long games) {
    }
}
