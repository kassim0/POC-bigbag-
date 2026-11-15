import { ChangeDetectionStrategy, Component, inject, input, output, signal } from '@angular/core';
import { Api } from '../core/api';
import { errorMessage } from '../core/errors';
import { Game } from '../core/models';
import { GameCover } from './game-cover';

/** Search box over the RAWG catalogue; emits the game the user picks. */
@Component({
  selector: 'app-game-search',
  imports: [GameCover],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <label class="field">
      <span>{{ label() }}</span>
      <input
        type="search"
        placeholder="Search the RAWG catalogue…"
        [value]="query()"
        (input)="onInput($any($event.target).value)"
      />
    </label>
    @if (error(); as message) {
      <p class="error" role="alert">{{ message }}</p>
    }
    @if (searching()) {
      <p class="muted">Searching…</p>
    }
    @if (searched() && !searching() && !error() && results().length === 0) {
      <p class="muted">No game found.</p>
    }
    @if (results().length) {
      <ul class="results">
        @for (game of results(); track game.rawgId) {
          <li>
            <app-game-cover [game]="game" />
            <span class="grow">
              {{ game.name }}
              @if (game.released) {
                <small class="muted">({{ game.released.slice(0, 4) }})</small>
              }
            </span>
            @if (excluded().includes(game.rawgId)) {
              <small class="muted">Added</small>
            } @else {
              <button type="button" class="small" (click)="picked.emit(game)">Add</button>
            }
          </li>
        }
      </ul>
      @if (hasNext()) {
        <button type="button" class="ghost small" (click)="more()">More results</button>
      }
    }
  `,
})
export class GameSearch {
  private readonly api = inject(Api);
  private timer: ReturnType<typeof setTimeout> | undefined;
  private page = 1;
  private requestId = 0;

  readonly label = input('Add a game');
  /** rawgIds already in the list: shown as "Added" instead of an Add button. */
  readonly excluded = input<number[]>([]);
  readonly debounceMs = input(300);
  readonly picked = output<Game>();

  protected readonly query = signal('');
  protected readonly results = signal<Game[]>([]);
  protected readonly hasNext = signal(false);
  protected readonly searching = signal(false);
  protected readonly searched = signal(false);
  protected readonly error = signal<string | null>(null);

  protected onInput(value: string): void {
    this.query.set(value);
    clearTimeout(this.timer);
    this.timer = setTimeout(() => void this.search(1), this.debounceMs());
  }

  protected more(): void {
    void this.search(this.page + 1);
  }

  private async search(page: number): Promise<void> {
    const query = this.query().trim();
    const id = ++this.requestId;
    if (!query) {
      this.results.set([]);
      this.hasNext.set(false);
      this.searched.set(false);
      this.error.set(null);
      return;
    }
    this.searching.set(true);
    try {
      const found = await this.api.searchGames(query, page);
      if (id !== this.requestId) {
        return; // a newer search has started
      }
      this.page = page;
      this.results.update((current) => (page === 1 ? found.results : [...current, ...found.results]));
      this.hasNext.set(found.hasNext);
      this.error.set(null);
    } catch (error) {
      if (id === this.requestId) {
        this.error.set(errorMessage(error));
      }
    } finally {
      if (id === this.requestId) {
        this.searching.set(false);
        this.searched.set(true);
      }
    }
  }
}
