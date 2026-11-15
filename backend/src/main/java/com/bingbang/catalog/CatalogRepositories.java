package com.bingbang.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repositories of the catalogue. */
public final class CatalogRepositories {

    private CatalogRepositories() {
    }

    public interface Games extends JpaRepository<CatalogGame, Long> {
    }

    public interface Genres extends JpaRepository<CatalogGenre, Long> {
    }

    public interface Platforms extends JpaRepository<CatalogPlatform, Long> {
    }

    public interface States extends JpaRepository<ImportState, String> {
    }
}
