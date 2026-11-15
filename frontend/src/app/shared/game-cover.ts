import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { Game } from '../core/models';

/** Thumbnail of a game (RAWG picture, or a placeholder when there is none). */
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

  /** RAWG serves resized pictures under /media/crop/{w}/{h}/. */
  protected readonly src = computed(() => {
    const url = this.game().backgroundImage;
    return url?.replace(/^(https:\/\/media\.rawg\.io\/media)\/(?!crop\/|resize\/)/, '$1/crop/600/400/') ?? null;
  });
}
