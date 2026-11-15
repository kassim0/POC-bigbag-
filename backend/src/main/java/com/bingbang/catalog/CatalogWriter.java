package com.bingbang.catalog;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.bingbang.catalog.CatalogRepositories.Games;
import com.bingbang.catalog.CatalogRepositories.Genres;
import com.bingbang.catalog.CatalogRepositories.Platforms;
import com.bingbang.catalog.CatalogRepositories.States;

/** Writes the import to the database; each method is one transaction. */
@Component
public class CatalogWriter {

    private final Games games;
    private final Genres genres;
    private final Platforms platforms;
    private final States states;

    public CatalogWriter(Games games, Genres genres, Platforms platforms, States states) {
        this.games = games;
        this.genres = genres;
        this.platforms = platforms;
        this.states = states;
    }

    /** Returns the state of the run to carry on, starting a new run if none is in progress. */
    @Transactional
    public ImportState beginOrResume(long now) {
        ImportState state = states.findById(ImportState.IGDB_GAMES)
                .orElseGet(() -> new ImportState(ImportState.IGDB_GAMES));
        if (state.getRunStartedAt() == null) {
            state.startRun(now);
        }
        return states.save(state);
    }

    /**
     * Creates or updates the games of a page, with their genres and platforms, and moves the cursor
     * in the same transaction: after a crash, the run resumes exactly after the last saved page.
     */
    @Transactional
    public ImportState savePage(List<IgdbGame> page) {
        Map<Long, CatalogGenre> knownGenres = byId(genres.findAllById(ids(page, IgdbGame::genres, IgdbGame.Genre::id)),
                CatalogGenre::getIgdbId);
        Map<Long, CatalogPlatform> knownPlatforms = byId(
                platforms.findAllById(ids(page, IgdbGame::platforms, IgdbGame.Platform::id)), CatalogPlatform::getIgdbId);
        Map<Long, CatalogGame> knownGames = byId(games.findAllById(page.stream().map(IgdbGame::id).toList()),
                CatalogGame::getIgdbId);

        int saved = 0;
        long cursor = 0;
        for (IgdbGame source : page) {
            cursor = Math.max(cursor, source.id());
            if (source.name() == null || source.name().isBlank()) {
                continue; // nothing to show for such a record
            }
            CatalogGame game = knownGames.computeIfAbsent(source.id(), CatalogGame::new);
            game.setName(truncate(source.name().trim(), CatalogGame.NAME_MAX_LENGTH));
            game.setSlug(truncate(source.slug(), 500));
            game.setSummary(truncate(source.summary(), CatalogGame.SUMMARY_MAX_LENGTH));
            game.setFirstReleaseDate(source.firstReleaseDate() == null ? null
                    : LocalDate.ofInstant(Instant.ofEpochSecond(source.firstReleaseDate()), ZoneOffset.UTC));
            game.setCoverImageId(source.cover() == null ? null : truncate(source.cover().imageId(), 100));
            game.setRating(source.totalRating());
            game.setRatingCount(source.totalRatingCount());
            game.setIgdbUpdatedAt(source.updatedAt() == null ? 0 : source.updatedAt());

            game.getGenres().clear();
            for (IgdbGame.Genre genre : orEmpty(source.genres())) {
                if (genre.name() != null) {
                    CatalogGenre entity = knownGenres.computeIfAbsent(genre.id(),
                            id -> genres.save(new CatalogGenre(id, truncate(genre.name(), 200))));
                    entity.setName(truncate(genre.name(), 200));
                    game.getGenres().add(entity);
                }
            }
            game.getPlatforms().clear();
            for (IgdbGame.Platform platform : orEmpty(source.platforms())) {
                if (platform.name() != null) {
                    CatalogPlatform entity = knownPlatforms.computeIfAbsent(platform.id(), id -> platforms.save(
                            new CatalogPlatform(id, truncate(platform.name(), 200), truncate(platform.abbreviation(), 100))));
                    entity.update(truncate(platform.name(), 200), truncate(platform.abbreviation(), 100));
                    game.getPlatforms().add(entity);
                }
            }
            games.save(game);
            saved++;
        }
        ImportState state = states.findById(ImportState.IGDB_GAMES).orElseThrow();
        state.advance(cursor, saved);
        return state;
    }

    @Transactional
    public ImportState complete(long overlapSeconds) {
        ImportState state = states.findById(ImportState.IGDB_GAMES).orElseThrow();
        state.completeRun(overlapSeconds);
        return state;
    }

    private static <T> List<T> orEmpty(List<T> list) {
        return list == null ? List.of() : list;
    }

    private static <T> List<Long> ids(List<IgdbGame> page, Function<IgdbGame, List<T>> related, Function<T, Long> id) {
        return page.stream().flatMap(g -> orEmpty(related.apply(g)).stream()).filter(Objects::nonNull)
                .map(id).distinct().toList();
    }

    private static <T> Map<Long, T> byId(List<T> entities, Function<T, Long> id) {
        return entities.stream().collect(Collectors.toMap(id, Function.identity(), (a, b) -> a, HashMap::new));
    }

    private static String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }
}
