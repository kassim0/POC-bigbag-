import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, effect, inject, input, signal } from '@angular/core';
import { Api } from '../core/api';
import { SharedView, listTitle } from '../core/models';
import { Theme } from '../core/theme';
import { GameCover } from '../shared/game-cover';
import { ListCard } from '../shared/list-card';

/** What people see through a share link: the shared wallet or lists, strictly read-only. */
@Component({
  selector: 'app-shared-view',
  imports: [ListCard, GameCover],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './shared-view.html',
})
export class SharedViewPage {
  private readonly api = inject(Api);
  private readonly theme = inject(Theme);

  /** Route parameter. */
  readonly shareId = input.required<string>();

  protected readonly view = signal<SharedView | null>(null);
  protected readonly failure = signal<'gone' | 'error' | null>(null);
  protected readonly openedId = signal<number | null>(null);
  protected readonly opened = computed(
    () => this.view()?.lists.find((list) => list.id === this.openedId()) ?? null,
  );
  protected readonly title = listTitle;

  constructor() {
    effect(() => void this.load(this.shareId()));
  }

  private async load(shareId: string): Promise<void> {
    this.view.set(null);
    this.failure.set(null);
    this.openedId.set(null);
    try {
      const view = await this.api.sharedView(shareId);
      this.view.set(view);
      this.theme.set(view.background);
    } catch (error) {
      this.failure.set(error instanceof HttpErrorResponse && error.status === 404 ? 'gone' : 'error');
    }
  }
}
