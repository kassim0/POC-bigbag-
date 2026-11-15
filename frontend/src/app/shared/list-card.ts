import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { GamesList, listTitle } from '../core/models';
import { GameCover } from './game-cover';

export const PREVIEW_SIZE = 3;

/** Summary of a GamesList on the home page: its name and its 3 first games. */
@Component({
  selector: 'app-list-card',
  imports: [GameCover],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2>{{ title() }}</h2>
    <p class="muted">{{ list().games.length }} {{ list().games.length === 1 ? 'game' : 'games' }}</p>
    <ul class="preview">
      @for (game of preview(); track game.rawgId) {
        <li>
          <app-game-cover [game]="game" />
          <span>{{ game.name }}</span>
        </li>
      }
    </ul>
    @if (hidden() > 0) {
      <p class="muted more">+ {{ hidden() }} more</p>
    }
  `,
})
export class ListCard {
  readonly list = input.required<GamesList>();

  protected readonly title = computed(() => listTitle(this.list()));
  protected readonly preview = computed(() => this.list().games.slice(0, PREVIEW_SIZE));
  protected readonly hidden = computed(() => this.list().games.length - this.preview().length);
}
