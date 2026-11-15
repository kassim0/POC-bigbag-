package com.bingbang.wallet;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Request and response payloads of the wallet API. */
public final class Dtos {

    private Dtos() {
    }

    public record GameRequest(@NotNull Long rawgId,
                              @NotBlank @Size(max = 300) String name,
                              @Size(max = 20) String released,
                              @Size(max = 1000) String backgroundImage) {
    }

    public record CreateListRequest(@Size(max = GamesList.NAME_MAX_LENGTH) String name,
                                    @Valid List<GameRequest> games) {
    }

    public record RenameListRequest(@NotNull @Size(max = GamesList.NAME_MAX_LENGTH) String name) {
    }

    public record CommentRequest(@NotBlank @Size(max = GameComment.MAX_LENGTH) String text) {
    }

    public record TransferCommentRequest(@NotNull Long targetListId) {
    }

    public record BackgroundRequest(@NotBlank @Size(max = 50) String background) {
    }

    public record GameDto(long rawgId, String name, String released, String backgroundImage, String comment) {
    }

    public record GamesListDto(long id, String name, List<GameDto> games) {
    }

    /** username: the account that owns the wallet, null for a guest wallet. */
    public record WalletDto(String id, String background, String username, List<GamesListDto> lists) {
    }
}
