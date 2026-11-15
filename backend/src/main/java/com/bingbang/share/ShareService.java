package com.bingbang.share;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bingbang.common.ApiException;
import com.bingbang.session.SessionService;
import com.bingbang.wallet.Dtos.GamesListDto;
import com.bingbang.wallet.Dtos.WalletDto;
import com.bingbang.wallet.WalletService;

@Service
@Transactional
public class ShareService {

    private final ShareLinkRepository links;
    private final WalletService wallets;

    public ShareService(ShareLinkRepository links, WalletService wallets) {
        this.links = links;
        this.wallets = wallets;
    }

    /** No listIds = the whole wallet; otherwise the given lists, which must belong to the wallet. */
    public ShareDto create(String walletId, List<Long> listIds) {
        Set<Long> selected = listIds == null ? Set.of() : new LinkedHashSet<>(listIds);
        Set<Long> owned = ownedListIds(walletId);
        if (!owned.containsAll(selected)) {
            throw ApiException.notFound("Games list not found");
        }
        ShareLink link = links.save(new ShareLink(SessionService.randomToken(), walletId, selected));
        return toDto(link, owned);
    }

    @Transactional(readOnly = true)
    public List<ShareDto> list(String walletId) {
        Set<Long> owned = ownedListIds(walletId);
        return links.findByWalletIdOrderByCreatedAtAsc(walletId).stream()
                .filter(link -> isAlive(link, owned))
                .map(link -> toDto(link, owned))
                .toList();
    }

    public void revoke(String walletId, String shareId) {
        links.delete(links.findByIdAndWalletId(shareId, walletId)
                .orElseThrow(() -> ApiException.notFound("Share link not found")));
    }

    /** What a visitor sees: always the current state of the wallet, restricted to what was shared. */
    @Transactional(readOnly = true)
    public SharedViewDto view(String shareId) {
        ShareLink link = links.findById(shareId).orElseThrow(() -> ApiException.notFound("Share link not found"));
        WalletDto wallet = wallets.getWallet(link.getWalletId());
        List<GamesListDto> lists = wallet.lists().stream()
                .filter(list -> link.isWholeWallet() || link.getListIds().contains(list.id()))
                .toList();
        if (!link.isWholeWallet() && lists.isEmpty()) {
            // Every shared list has been deleted since.
            throw ApiException.notFound("Share link not found");
        }
        return new SharedViewDto(link.isWholeWallet(), wallet.background(), lists);
    }

    private Set<Long> ownedListIds(String walletId) {
        return wallets.getLists(walletId).stream().map(GamesListDto::id).collect(Collectors.toSet());
    }

    private static boolean isAlive(ShareLink link, Set<Long> owned) {
        return link.isWholeWallet() || link.getListIds().stream().anyMatch(owned::contains);
    }

    private static ShareDto toDto(ShareLink link, Set<Long> owned) {
        List<Long> listIds = link.getListIds().stream().filter(owned::contains).toList();
        return new ShareDto(link.getId(), link.isWholeWallet(), listIds, link.getCreatedAt());
    }

    public record ShareDto(String id, boolean wholeWallet, List<Long> listIds, Instant createdAt) {
    }

    /** Deliberately without wallet id nor account name: a visitor only gets what is displayed. */
    public record SharedViewDto(boolean wholeWallet, String background, List<GamesListDto> lists) {
    }
}
