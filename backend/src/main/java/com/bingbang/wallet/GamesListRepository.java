package com.bingbang.wallet;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GamesListRepository extends JpaRepository<GamesList, Long> {

    List<GamesList> findByWalletIdOrderByIdAsc(String walletId);

    Optional<GamesList> findByIdAndWalletId(Long id, String walletId);
}
