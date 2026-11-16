package com.bingbang.catalog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Spring Data repositories of the catalogue. */
public final class CatalogRepositories {

    private CatalogRepositories() {
    }

    public interface Games extends JpaRepository<CatalogGame, Long> {

        /**
         * Games whose name contains the text: names starting with it first, then the most rated.
         * The patterns must be lower case, with LIKE wildcards of the user's text escaped by '\'.
         */
        @Query(value = """
                select g from CatalogGame g
                where lower(g.name) like :contains escape '\\'
                order by case when lower(g.name) like :startsWith escape '\\' then 0 else 1 end,
                         coalesce(g.ratingCount, 0) desc, g.name, g.igdbId""",
                countQuery = "select count(g) from CatalogGame g where lower(g.name) like :contains escape '\\'")
        Page<CatalogGame> search(@Param("contains") String contains, @Param("startsWith") String startsWith,
                                 Pageable pageable);
    }

    public interface Genres extends JpaRepository<CatalogGenre, Long> {
    }

    public interface Platforms extends JpaRepository<CatalogPlatform, Long> {
    }

    public interface States extends JpaRepository<ImportState, String> {
    }
}
