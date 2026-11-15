package com.bingbang.share;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;

/**
 * A read-only link to a wallet. The id is an unguessable token: knowing it is what grants the view.
 * With no listIds the link shows the whole wallet; otherwise only the selected lists (one or several).
 */
@Entity
public class ShareLink {

    @Id
    @Column(length = 43)
    private String id;

    @Column(nullable = false, length = 36)
    private String walletId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "share_link_lists", joinColumns = @JoinColumn(name = "share_id"))
    @Column(name = "list_id")
    private Set<Long> listIds = new LinkedHashSet<>();

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected ShareLink() {
    }

    public ShareLink(String id, String walletId, Set<Long> listIds) {
        this.id = id;
        this.walletId = walletId;
        this.listIds.addAll(listIds);
    }

    public String getId() {
        return id;
    }

    public String getWalletId() {
        return walletId;
    }

    public void setWalletId(String walletId) {
        this.walletId = walletId;
    }

    public Set<Long> getListIds() {
        return listIds;
    }

    public boolean isWholeWallet() {
        return listIds.isEmpty();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
