package com.bingbang.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class CatalogPlatform {

    @Id
    private Long igdbId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 100)
    private String abbreviation;

    protected CatalogPlatform() {
    }

    public CatalogPlatform(Long igdbId, String name, String abbreviation) {
        this.igdbId = igdbId;
        this.name = name;
        this.abbreviation = abbreviation;
    }

    public Long getIgdbId() {
        return igdbId;
    }

    public String getName() {
        return name;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public void update(String name, String abbreviation) {
        this.name = name;
        this.abbreviation = abbreviation;
    }
}
