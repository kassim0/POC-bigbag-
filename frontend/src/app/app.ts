import { DOCUMENT } from '@angular/common';
import { ChangeDetectionStrategy, Component, effect, inject } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';
import { Theme } from './core/theme';
import { WalletStore } from './core/wallet-store';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <nav class="topbar">
      <a class="brand" routerLink="/">🎮 BingBang</a>
      @if (store.wallet(); as wallet) {
        <a class="pill" routerLink="/account">{{ wallet.username ?? 'Create account / Log in' }}</a>
      }
    </nav>
    @if (store.error(); as message) {
      <p class="toast error" role="alert">
        {{ message }}
        <button type="button" class="icon" aria-label="Dismiss" (click)="store.error.set(null)">×</button>
      </p>
    }
    <main>
      <router-outlet />
    </main>
  `,
})
export class App {
  protected readonly store = inject(WalletStore);

  constructor() {
    const theme = inject(Theme);
    effect(() => theme.set(this.store.wallet()?.background));

    // Coming back to the tab: pick up what was changed from another device.
    const document = inject(DOCUMENT);
    document.addEventListener('visibilitychange', () => {
      if (document.visibilityState === 'visible' && this.store.wallet()?.username) {
        void this.store.reload();
      }
    });
  }
}
