package com.bingbang.rawg;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bingbang.common.ApiException;

@RestController
@RequestMapping("/api/games")
public class GameSearchController {

    private static final int MAX_PAGE_SIZE = 40;

    private final RawgClient rawg;

    public GameSearchController(RawgClient rawg) {
        this.rawg = rawg;
    }

    @GetMapping("/search")
    public GameSearchResult search(@RequestParam("q") String query,
                                   @RequestParam(defaultValue = "1") int page,
                                   @RequestParam(defaultValue = "12") int pageSize) {
        String q = query.trim();
        if (q.isEmpty()) {
            throw ApiException.badRequest("The search query must not be empty");
        }
        if (page < 1 || pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw ApiException.badRequest("Invalid pagination");
        }
        return rawg.search(q, page, pageSize);
    }
}
