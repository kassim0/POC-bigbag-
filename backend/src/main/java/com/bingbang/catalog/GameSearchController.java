package com.bingbang.catalog;

import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bingbang.common.ApiException;

/** Search over BingBang's own games catalogue: no external service is called. */
@RestController
@RequestMapping("/api/games")
public class GameSearchController {

    private static final int MAX_PAGE_SIZE = 40;
    private static final int MAX_QUERY_LENGTH = 100;

    private final CatalogRepositories.Games games;

    public GameSearchController(CatalogRepositories.Games games) {
        this.games = games;
    }

    @GetMapping("/search")
    @Transactional(readOnly = true)
    public GameSearchResult search(@RequestParam("q") String query,
                                   @RequestParam(defaultValue = "1") int page,
                                   @RequestParam(defaultValue = "12") int pageSize) {
        String q = query.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty() || q.length() > MAX_QUERY_LENGTH) {
            throw ApiException.badRequest("The search query must be 1 to " + MAX_QUERY_LENGTH + " characters");
        }
        if (page < 1 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw ApiException.badRequest("Invalid pagination");
        }
        // The user's text is searched literally: % and _ are not wildcards for them.
        String literal = q.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        Page<CatalogGame> found = games.search("%" + literal + "%", literal + "%", PageRequest.of(page - 1, pageSize));
        List<GameSummary> results = found.getContent().stream()
                .map(g -> new GameSummary(g.getIgdbId(), g.getName(), g.getReleased(), g.getCoverUrl(), g.getRating()))
                .toList();
        return new GameSearchResult(found.getTotalElements(), page, found.hasNext(), results);
    }

    public record GameSummary(long gameId, String name, String released, String coverUrl, Double rating) {
    }

    public record GameSearchResult(long count, int page, boolean hasNext, List<GameSummary> results) {
    }
}
