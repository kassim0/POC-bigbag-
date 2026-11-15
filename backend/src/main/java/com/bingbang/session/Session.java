package com.bingbang.session;

/** The authenticated caller: a wallet, plus the account that owns it when the caller is logged in. */
public record Session(String walletId, Long userId, String tokenHash) {
}
