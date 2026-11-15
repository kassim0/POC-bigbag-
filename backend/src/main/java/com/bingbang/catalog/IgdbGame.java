package com.bingbang.catalog;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** A game as returned by IGDB's /games endpoint, with the fields the import asks for. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record IgdbGame(long id,
                       String name,
                       String slug,
                       String summary,
                       @JsonProperty("first_release_date") Long firstReleaseDate,
                       Cover cover,
                       List<Genre> genres,
                       List<Platform> platforms,
                       @JsonProperty("total_rating") Double totalRating,
                       @JsonProperty("total_rating_count") Integer totalRatingCount,
                       @JsonProperty("updated_at") Long updatedAt) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Cover(@JsonProperty("image_id") String imageId) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Genre(long id, String name) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Platform(long id, String name, String abbreviation) {
    }
}
