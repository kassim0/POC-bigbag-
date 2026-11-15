package com.bingbang.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class CatalogGenre {

    @Id
    private Long igdbId;

    @Column(nullable = false, length = 200)
    private String name;

    protected CatalogGenre() {
    }

    public CatalogGenre(Long igdbId, String name) {
        this.igdbId = igdbId;
        this.name = name;
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
}
