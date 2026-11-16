package com.bingbang.catalog;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

/** A game of BingBang's own catalogue, copied from IGDB by the import batch. */
@Entity
@Table(indexes = {@Index(columnList = "name"), @Index(columnList = "igdbUpdatedAt")})
public class CatalogGame {

    public static final int NAME_MAX_LENGTH = 500;
    public static final int SUMMARY_MAX_LENGTH = 4000;

    /** The IGDB id is the primary key: importing a game again updates it. */
    @Id
    private Long igdbId;

    @Column(nullable = false, length = NAME_MAX_LENGTH)
    private String name;

    @Column(length = 500)
    private String slug;

    @Column(length = SUMMARY_MAX_LENGTH)
    private String summary;

    private LocalDate firstReleaseDate;

    /** IGDB image id, see {@link #getCoverUrl()}. */
    @Column(length = 100)
    private String coverImageId;

    /** Average of users' and critics' ratings, out of 100. */
    private Double rating;

    private Integer ratingCount;

    /** IGDB's "updated_at" (epoch seconds). */
    @Column(nullable = false)
    private long igdbUpdatedAt;

    @ManyToMany
    @JoinTable(name = "catalog_game_genres", joinColumns = @JoinColumn(name = "game_id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id"))
    private Set<CatalogGenre> genres = new LinkedHashSet<>();

    @ManyToMany
    @JoinTable(name = "catalog_game_platforms", joinColumns = @JoinColumn(name = "game_id"),
            inverseJoinColumns = @JoinColumn(name = "platform_id"))
    private Set<CatalogPlatform> platforms = new LinkedHashSet<>();

    protected CatalogGame() {
    }

    public CatalogGame(Long igdbId) {
        this.igdbId = igdbId;
    }

    public Long getIgdbId() {
        return igdbId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public LocalDate getFirstReleaseDate() {
        return firstReleaseDate;
    }

    public void setFirstReleaseDate(LocalDate firstReleaseDate) {
        this.firstReleaseDate = firstReleaseDate;
    }

    public String getCoverImageId() {
        return coverImageId;
    }

    public void setCoverImageId(String coverImageId) {
        this.coverImageId = coverImageId;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public Integer getRatingCount() {
        return ratingCount;
    }

    public void setRatingCount(Integer ratingCount) {
        this.ratingCount = ratingCount;
    }

    public long getIgdbUpdatedAt() {
        return igdbUpdatedAt;
    }

    public void setIgdbUpdatedAt(long igdbUpdatedAt) {
        this.igdbUpdatedAt = igdbUpdatedAt;
    }

    /** ISO date of the first release, or null. */
    public String getReleased() {
        return firstReleaseDate == null ? null : firstReleaseDate.toString();
    }

    /** URL of the cover on IGDB's image server, or null when the game has none. */
    public String getCoverUrl() {
        return coverImageId == null ? null
                : "https://images.igdb.com/igdb/image/upload/t_cover_big/" + coverImageId + ".jpg";
    }

    public Set<CatalogGenre> getGenres() {
        return genres;
    }

    public Set<CatalogPlatform> getPlatforms() {
        return platforms;
    }
}
