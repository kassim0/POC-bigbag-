import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { Game, GamesList, listTitle } from '../core/models';
import { WalletStore } from '../core/wallet-store';
import { GameCover } from '../shared/game-cover';
import { GameSearch } from '../shared/game-search';

/** Detail of a GamesList: rename it, add or remove games, comment them, delete it. */
@Component({
  selector: 'app-list-detail',
  imports: [RouterLink, GameCover, GameSearch],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './list-detail.html',
})
export class ListDetail {
  protected readonly store = inject(WalletStore);
  private readonly router = inject(Router);

  /** Route parameter. */
  readonly id = input.required<string>();

  protected readonly loaded = signal(false);
  protected readonly list = computed(
    () => this.store.wallet()?.lists.find((l) => l.id === Number(this.id())) ?? null,
  );
  protected readonly title = computed(() => {
    const list = this.list();
    return list ? listTitle(list) : '';
  });
  protected readonly gameIds = computed(() => this.list()?.games.map((g) => g.rawgId) ?? []);
  protected readonly confirmingDelete = signal(false);
  /** rawgId of the game whose commentary was just copied, and where to. */
  protected readonly transferred = signal<{ rawgId: number; to: string } | null>(null);
  protected readonly listTitle = listTitle;

  constructor() {
    void this.store.ensureLoaded().then(() => this.loaded.set(true));
  }

  protected async rename(input: HTMLInputElement): Promise<void> {
    const list = this.list();
    if (!list) {
      return;
    }
    const name = input.value.trim();
    if (name === list.name || !(await this.store.renameList(list.id, name))) {
      input.value = list.name;
    }
  }

  protected add(game: Game): void {
    void this.store.addGame(Number(this.id()), game);
  }

  protected remove(game: Game): void {
    void this.store.removeGame(Number(this.id()), game.rawgId);
  }

  /** An empty text deletes the commentary. */
  protected async saveComment(game: Game, textarea: HTMLTextAreaElement): Promise<void> {
    const text = textarea.value.trim();
    if (text === (game.comment ?? '')) {
      return;
    }
    if (text) {
      await this.store.setComment(Number(this.id()), game.rawgId, text);
    } else {
      await this.deleteComment(game, textarea);
    }
  }

  protected async deleteComment(game: Game, textarea: HTMLTextAreaElement): Promise<void> {
    if (await this.store.deleteComment(Number(this.id()), game.rawgId)) {
      textarea.value = '';
    }
  }

  /** The other lists that contain this game: its commentary can be copied there. */
  protected otherListsWith(game: Game): GamesList[] {
    const id = Number(this.id());
    return (this.store.wallet()?.lists ?? []).filter(
      (l) => l.id !== id && l.games.some((g) => g.rawgId === game.rawgId),
    );
  }

  protected async transfer(game: Game, target: GamesList): Promise<void> {
    if (await this.store.transferComment(Number(this.id()), game.rawgId, target.id)) {
      this.transferred.set({ rawgId: game.rawgId, to: listTitle(target) });
    }
  }

  protected async deleteList(): Promise<void> {
    if (await this.store.deleteList(Number(this.id()))) {
      await this.router.navigateByUrl('/');
    }
  }
}
