package com.bingbang.wallet;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bingbang.account.UserAccount;
import com.bingbang.account.UserAccountRepository;
import com.bingbang.common.ApiException;
import com.bingbang.wallet.Dtos.CreateListRequest;
import com.bingbang.wallet.Dtos.GameDto;
import com.bingbang.wallet.Dtos.GameRequest;
import com.bingbang.wallet.Dtos.GamesListDto;
import com.bingbang.wallet.Dtos.WalletDto;

@Service
@Transactional
public class WalletService {

    private final WalletRepository wallets;
    private final GamesListRepository lists;

    private final UserAccountRepository users;

    public WalletService(WalletRepository wallets, GamesListRepository lists, UserAccountRepository users) {
        this.wallets = wallets;
        this.lists = lists;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public WalletDto getWallet(String walletId) {
        Wallet wallet = wallet(walletId);
        String username = users.findByWalletId(walletId).map(UserAccount::getUsername).orElse(null);
        return new WalletDto(wallet.getId(), wallet.getBackground(), username, getLists(walletId));
    }

    public WalletDto setBackground(String walletId, String background) {
        wallet(walletId).setBackground(background.trim());
        return getWallet(walletId);
    }

    @Transactional(readOnly = true)
    public List<GamesListDto> getLists(String walletId) {
        return lists.findByWalletIdOrderByIdAsc(walletId).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public GamesListDto getList(String walletId, long listId) {
        return toDto(list(walletId, listId));
    }

    /** Wiki V0.1: "for creating a gamesList they need at least a name or games". */
    public GamesListDto createList(String walletId, CreateListRequest request) {
        String name = request.name() == null ? "" : request.name().trim();
        List<GameRequest> games = request.games() == null ? List.of() : request.games();
        if (name.isEmpty() && games.isEmpty()) {
            throw ApiException.badRequest("A games list needs at least a name or a game");
        }
        GamesList list = new GamesList(wallet(walletId), name);
        for (GameRequest game : games) {
            if (!list.contains(game.rawgId())) {
                list.getGames().add(toEntry(list, game));
            }
        }
        return toDto(lists.save(list));
    }

    public GamesListDto renameList(String walletId, long listId, String newName) {
        GamesList list = list(walletId, listId);
        String name = newName.trim();
        if (name.isEmpty() && list.getGames().isEmpty()) {
            throw ApiException.badRequest("A games list needs at least a name or a game");
        }
        list.setName(name);
        return toDto(list);
    }

    public void deleteList(String walletId, long listId) {
        lists.delete(list(walletId, listId));
    }

    public GamesListDto addGame(String walletId, long listId, GameRequest game) {
        GamesList list = list(walletId, listId);
        if (list.contains(game.rawgId())) {
            throw new ApiException(HttpStatus.CONFLICT, "This game is already in the list");
        }
        list.getGames().add(toEntry(list, game));
        lists.saveAndFlush(list);
        return toDto(list);
    }

    public GamesListDto removeGame(String walletId, long listId, long rawgId) {
        GamesList list = list(walletId, listId);
        if (!list.getGames().removeIf(g -> g.getRawgId() == rawgId)) {
            throw ApiException.notFound("This game is not in the list");
        }
        return toDto(list);
    }

    public GamesListDto setComment(String walletId, long listId, long rawgId, String text) {
        GamesList list = listWithGame(walletId, listId, rawgId);
        writeComment(list, rawgId, text.trim());
        return toDto(list);
    }

    /** Deletes the commentary itself (removing the game does not). */
    public GamesListDto deleteComment(String walletId, long listId, long rawgId) {
        GamesList list = list(walletId, listId);
        if (!list.getComments().removeIf(c -> c.getRawgId() == rawgId)) {
            throw ApiException.notFound("This game has no commentary");
        }
        return toDto(list);
    }

    /**
     * Copies the commentary of a game to another list of the wallet that contains the same game,
     * replacing the commentary it may have there. Returns the target list.
     */
    public GamesListDto transferComment(String walletId, long listId, long rawgId, long targetListId) {
        if (listId == targetListId) {
            throw ApiException.badRequest("The commentary is already in this list");
        }
        String text = list(walletId, listId).commentOf(rawgId)
                .orElseThrow(() -> ApiException.notFound("This game has no commentary"))
                .getText();
        GamesList target = listWithGame(walletId, targetListId, rawgId);
        writeComment(target, rawgId, text);
        return toDto(target);
    }

    private GamesList listWithGame(String walletId, long listId, long rawgId) {
        GamesList list = list(walletId, listId);
        if (!list.contains(rawgId)) {
            throw ApiException.notFound("This game is not in the list");
        }
        return list;
    }

    private static void writeComment(GamesList list, long rawgId, String text) {
        list.commentOf(rawgId).ifPresentOrElse(
                c -> c.setText(text),
                () -> list.getComments().add(new GameComment(list, rawgId, text)));
    }

    private Wallet wallet(String walletId) {
        return wallets.findById(walletId).orElseThrow(() -> ApiException.notFound("Wallet not found"));
    }

    GamesList list(String walletId, long listId) {
        return lists.findByIdAndWalletId(listId, walletId)
                .orElseThrow(() -> ApiException.notFound("Games list not found"));
    }

    private static GameEntry toEntry(GamesList list, GameRequest game) {
        return new GameEntry(list, game.rawgId(), game.name().trim(), game.released(), game.backgroundImage());
    }

    GamesListDto toDto(GamesList list) {
        List<GameDto> games = list.getGames().stream()
                .map(g -> new GameDto(g.getRawgId(), g.getName(), g.getReleased(), g.getBackgroundImage(),
                        list.commentOf(g.getRawgId()).map(GameComment::getText).orElse(null)))
                .toList();
        return new GamesListDto(list.getId(), list.getName(), games);
    }
}
