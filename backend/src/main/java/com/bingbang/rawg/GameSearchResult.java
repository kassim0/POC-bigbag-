package com.bingbang.rawg;

import java.util.List;

public record GameSearchResult(int count, int page, boolean hasNext, List<GameSummary> results) {
}
