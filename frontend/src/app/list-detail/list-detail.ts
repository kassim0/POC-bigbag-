import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { Game, listTitle } from '../core/models';
import { WalletStore } from '../core/wallet-store';
import { GameCover } from '../shared/game-cover';
import { GameSearch } from '../shared/game-search';

/** Detail of a GamesList: rename it, add or remove games, delete it. */
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

  protected async deleteList(): Promise<void> {
    if (await this.store.deleteList(Number(this.id()))) {
      await this.router.navigateByUrl('/');
    }
  }
}
