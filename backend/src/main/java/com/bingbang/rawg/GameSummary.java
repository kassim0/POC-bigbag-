package com.bingbang.rawg;

/** A game of the RAWG catalogue, reduced to what BingBang displays. */
public record GameSummary(long rawgId, String name, String released, String backgroundImage, Double rating) {
}
