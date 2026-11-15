package com.bingbang.share;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ShareLinkRepository extends JpaRepository<ShareLink, String> {

    List<ShareLink> findByWalletIdOrderByCreatedAtAsc(String walletId);

    Optional<ShareLink> findByIdAndWalletId(String id, String walletId);
}
