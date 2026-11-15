import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { WalletStore } from '../core/wallet-store';

/** Create an account, log in or log out. The app is fully usable without an account. */
@Component({
  selector: 'app-account',
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './account.html',
})
export class AccountPage {
  protected readonly store = inject(WalletStore);
  private readonly router = inject(Router);

  protected readonly mode = signal<'register' | 'login'>('register');
  protected readonly busy = signal(false);

  constructor() {
    void this.store.ensureLoaded();
  }

  protected async submit(event: Event, username: string, password: string): Promise<void> {
    event.preventDefault();
    this.busy.set(true);
    const done =
      this.mode() === 'register'
        ? await this.store.register(username, password)
        : await this.store.login(username, password);
    this.busy.set(false);
    if (done) {
      await this.router.navigateByUrl('/');
    }
  }

  protected async logout(): Promise<void> {
    this.busy.set(true);
    await this.store.logout();
    this.busy.set(false);
  }
}
