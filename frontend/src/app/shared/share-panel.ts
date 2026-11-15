import { DOCUMENT } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { Api } from '../core/api';
import { ShareLink, listTitle } from '../core/models';
import { WalletStore } from '../core/wallet-store';

/**
 * Creates and revokes read-only share links: to the whole wallet, to a selection of lists, or
 * (with `onlyList`) to a single list.
 */
@Component({
  selector: 'app-share-panel',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <h2>{{ onlyList() === null ? 'Share my games wallet' : 'Share this list' }}</h2>
    <p class="muted">Anyone with a link can look, nobody can change anything. Links stay up to date.</p>

    @if (onlyList() === null) {
      <fieldset class="share-scope">
        <legend class="sr-only">What to share</legend>
        <label>
          <input type="radio" name="scope" [checked]="scope() === 'wallet'" (change)="scope.set('wallet')" />
          My whole wallet
        </label>
        <label>
          <input type="radio" name="scope" [checked]="scope() === 'lists'" (change)="scope.set('lists')" />
          Only the lists I select
        </label>
        @if (scope() === 'lists') {
          <ul class="share-lists">
            @for (list of lists(); track list.id) {
              <li>
                <label>
                  <input type="checkbox" [checked]="selected().has(list.id)" (change)="toggle(list.id)" />
                  {{ title(list) }}
                </label>
              </li>
            } @empty {
              <li class="muted">You have no list yet.</li>
            }
          </ul>
        }
      </fieldset>
    }
    <button type="button" class="small" [disabled]="!canCreate()" (click)="create()">Create a share link</button>

    @if (visibleLinks().length) {
      <ul class="share-links">
        @for (link of visibleLinks(); track link.id) {
          <li>
            <span class="grow">
              <strong>{{ describe(link) }}</strong>
              <input readonly [value]="url(link)" [attr.aria-label]="'Share link: ' + describe(link)" (focus)="$any($event.target).select()" />
            </span>
            <button type="button" class="small" (click)="copy(link)">
              {{ copied() === link.id ? 'Copied!' : 'Copy' }}
            </button>
            <button type="button" class="ghost small danger-text" (click)="revoke(link)">Revoke</button>
          </li>
        }
      </ul>
    }
  `,
})
export class SharePanel {
  private readonly api = inject(Api);
  private readonly store = inject(WalletStore);
  private readonly document = inject(DOCUMENT);

  /** When set, the panel only deals with links to this single list. */
  readonly onlyList = input<number | null>(null);

  protected readonly title = listTitle;
  protected readonly scope = signal<'wallet' | 'lists'>('wallet');
  protected readonly selected = signal<ReadonlySet<number>>(new Set());
  protected readonly copied = signal<string | null>(null);
  private readonly links = signal<ShareLink[]>([]);

  protected readonly lists = computed(() => this.store.wallet()?.lists ?? []);
  protected readonly visibleLinks = computed(() => {
    const only = this.onlyList();
    return this.links().filter(
      (link) => only === null || (!link.wholeWallet && link.listIds.length === 1 && link.listIds[0] === only),
    );
  });
  protected readonly canCreate = computed(
    () => this.onlyList() !== null || this.scope() === 'wallet' || this.selected().size > 0,
  );

  constructor() {
    void this.store.attempt(async () => this.links.set(await this.api.shares()));
  }

  protected toggle(listId: number): void {
    this.selected.update((current) => {
      const next = new Set(current);
      if (!next.delete(listId)) {
        next.add(listId);
      }
      return next;
    });
  }

  protected async create(): Promise<void> {
    const only = this.onlyList();
    const listIds = only !== null ? [only] : this.scope() === 'wallet' ? [] : [...this.selected()];
    await this.store.attempt(async () => {
      const link = await this.api.createShare(listIds);
      this.links.update((links) => [...links, link]);
    });
  }

  protected async revoke(link: ShareLink): Promise<void> {
    await this.store.attempt(async () => {
      await this.api.revokeShare(link.id);
      this.links.update((links) => links.filter((l) => l.id !== link.id));
    });
  }

  protected url(link: ShareLink): string {
    return new URL(`shared/${link.id}`, this.document.baseURI).toString();
  }

  protected async copy(link: ShareLink): Promise<void> {
    try {
      await this.document.defaultView?.navigator.clipboard.writeText(this.url(link));
      this.copied.set(link.id);
    } catch {
      this.copied.set(null); // clipboard unavailable: the field can still be selected and copied by hand
    }
  }

  protected describe(link: ShareLink): string {
    if (link.wholeWallet) {
      return 'My whole wallet';
    }
    const names = this.lists()
      .filter((list) => link.listIds.includes(list.id))
      .map(listTitle);
    return names.join(', ') || 'Selected lists';
  }
}
