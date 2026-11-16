package com.bingbang.wallet;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.bingbang.session.Session;
import com.bingbang.wallet.Dtos.BackgroundRequest;
import com.bingbang.wallet.Dtos.CommentRequest;
import com.bingbang.wallet.Dtos.CreateListRequest;
import com.bingbang.wallet.Dtos.GameRequest;
import com.bingbang.wallet.Dtos.GamesListDto;
import com.bingbang.wallet.Dtos.RenameListRequest;
import com.bingbang.wallet.Dtos.TransferCommentRequest;
import com.bingbang.wallet.Dtos.WalletDto;

import jakarta.validation.Valid;

/** Everything here acts on the wallet of the caller, resolved from its bearer token. */
@RestController
@RequestMapping("/api")
public class WalletController {

    private final WalletService service;

    public WalletController(WalletService service) {
        this.service = service;
    }

    @GetMapping("/wallet")
    public WalletDto wallet(Session session) {
        return service.getWallet(session.walletId());
    }

    @PatchMapping("/wallet")
    public WalletDto setBackground(Session session, @Valid @RequestBody BackgroundRequest request) {
        return service.setBackground(session.walletId(), request.background());
    }

    @GetMapping("/lists")
    public List<GamesListDto> lists(Session session) {
        return service.getLists(session.walletId());
    }

    @PostMapping("/lists")
    @ResponseStatus(HttpStatus.CREATED)
    public GamesListDto create(Session session, @Valid @RequestBody CreateListRequest request) {
        return service.createList(session.walletId(), request);
    }

    @GetMapping("/lists/{listId}")
    public GamesListDto list(Session session, @PathVariable long listId) {
        return service.getList(session.walletId(), listId);
    }

    @PatchMapping("/lists/{listId}")
    public GamesListDto rename(Session session, @PathVariable long listId,
                               @Valid @RequestBody RenameListRequest request) {
        return service.renameList(session.walletId(), listId, request.name());
    }

    @DeleteMapping("/lists/{listId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Session session, @PathVariable long listId) {
        service.deleteList(session.walletId(), listId);
    }

    @PostMapping("/lists/{listId}/games")
    public GamesListDto addGame(Session session, @PathVariable long listId,
                                @Valid @RequestBody GameRequest game) {
        return service.addGame(session.walletId(), listId, game);
    }

    @DeleteMapping("/lists/{listId}/games/{gameId}")
    public GamesListDto removeGame(Session session, @PathVariable long listId, @PathVariable long gameId) {
        return service.removeGame(session.walletId(), listId, gameId);
    }

    @PutMapping("/lists/{listId}/games/{gameId}/comment")
    public GamesListDto setComment(Session session, @PathVariable long listId, @PathVariable long gameId,
                                   @Valid @RequestBody CommentRequest request) {
        return service.setComment(session.walletId(), listId, gameId, request.text());
    }

    @DeleteMapping("/lists/{listId}/games/{gameId}/comment")
    public GamesListDto deleteComment(Session session, @PathVariable long listId, @PathVariable long gameId) {
        return service.deleteComment(session.walletId(), listId, gameId);
    }

    /** Copies the commentary to the same game in another list; returns that other list. */
    @PostMapping("/lists/{listId}/games/{gameId}/comment/transfer")
    public GamesListDto transferComment(Session session, @PathVariable long listId, @PathVariable long gameId,
                                        @Valid @RequestBody TransferCommentRequest request) {
        return service.transferComment(session.walletId(), listId, gameId, request.targetListId());
    }
}
