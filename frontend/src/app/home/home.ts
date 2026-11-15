import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { BACKGROUNDS, Game } from '../core/models';
import { WalletStore } from '../core/wallet-store';
import { GameSearch } from '../shared/game-search';
import { ListCard } from '../shared/list-card';

/** Home: the user's gamesWallet, with every GamesList and its 3 first games. */
@Component({
  selector: 'app-home',
  imports: [RouterLink, ListCard, GameSearch],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './home.html',
})
export class Home {
  protected readonly store = inject(WalletStore);
  protected readonly backgrounds = BACKGROUNDS;

  protected readonly newName = signal('');
  protected readonly newGames = signal<Game[]>([]);
  protected readonly newGameIds = computed(() => this.newGames().map((g) => g.rawgId));
  /** Wiki V0.1: a list needs at least a name or games. */
  protected readonly canCreate = computed(() => this.newName().trim().length > 0 || this.newGames().length > 0);

  constructor() {
    void this.store.ensureLoaded();
  }

  protected pick(game: Game): void {
    this.newGames.update((games) => (games.some((g) => g.rawgId === game.rawgId) ? games : [...games, game]));
  }

  protected unpick(game: Game): void {
    this.newGames.update((games) => games.filter((g) => g.rawgId !== game.rawgId));
  }

  protected async create(event: Event): Promise<void> {
    event.preventDefault();
    if (!this.canCreate()) {
      return;
    }
    if (await this.store.createList(this.newName().trim(), this.newGames())) {
      this.newName.set('');
      this.newGames.set([]);
    }
  }
}
