import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { Game } from '../core/models';

/** Cover of a game, or a placeholder when it has none. */
@Component({
  selector: 'app-game-cover',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (src(); as url) {
      <img [src]="url" [alt]="game().name" loading="lazy" />
    } @else {
      <span class="no-cover" aria-hidden="true">🎮</span>
    }
  `,
})
export class GameCover {
  readonly game = input.required<Game>();

  protected readonly src = computed(() => this.game().coverUrl);
}
