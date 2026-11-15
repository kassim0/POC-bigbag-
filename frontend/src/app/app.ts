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
  }
}
