import { Injectable, inject, signal } from '@angular/core';
import { Api } from './api';
import { errorMessage } from './errors';
import { Game, GamesList, Wallet } from './models';
import { SessionService } from './session';

/** The user's wallet, shared by the pages. Failed actions end up in `error`. */
@Injectable({ providedIn: 'root' })
export class WalletStore {
  private readonly api = inject(Api);
  private readonly session = inject(SessionService);
  private loading: Promise<void> | null = null;

  readonly wallet = signal<Wallet | null>(null);
  readonly error = signal<string | null>(null);

  /** Loads the wallet once; later calls reuse it. */
  ensureLoaded(): Promise<void> {
    this.loading ??= this.reload();
    return this.loading;
  }

  reload(): Promise<void> {
    this.loading = this.attempt(async () => this.wallet.set(await this.api.wallet())).then(() => undefined);
    return this.loading;
  }

  setBackground(background: string): Promise<boolean> {
    return this.attempt(async () => this.wallet.set(await this.api.setBackground(background)));
  }

  async createList(name: string, games: Game[]): Promise<GamesList | null> {
    let created: GamesList | null = null;
    await this.attempt(async () => {
      created = await this.api.createList(name, games);
      this.patchLists((lists) => [...lists, created!]);
    });
    return created;
  }

  renameList(listId: number, name: string): Promise<boolean> {
    return this.attempt(async () => this.replace(await this.api.renameList(listId, name)));
  }

  deleteList(listId: number): Promise<boolean> {
    return this.attempt(async () => {
      await this.api.deleteList(listId);
      this.patchLists((lists) => lists.filter((l) => l.id !== listId));
    });
  }

  addGame(listId: number, game: Game): Promise<boolean> {
    return this.attempt(async () => this.replace(await this.api.addGame(listId, game)));
  }

  removeGame(listId: number, gameId: number): Promise<boolean> {
    return this.attempt(async () => this.replace(await this.api.removeGame(listId, gameId)));
  }

  setComment(listId: number, gameId: number, text: string): Promise<boolean> {
    return this.attempt(async () => this.replace(await this.api.setComment(listId, gameId, text)));
  }

  deleteComment(listId: number, gameId: number): Promise<boolean> {
    return this.attempt(async () => this.replace(await this.api.deleteComment(listId, gameId)));
  }

  transferComment(listId: number, gameId: number, targetListId: number): Promise<boolean> {
    return this.attempt(async () => this.replace(await this.api.transferComment(listId, gameId, targetListId)));
  }

  /** Creates an account that takes over the current (guest) wallet. */
  register(username: string, password: string): Promise<boolean> {
    return this.attempt(async () => {
      const account = await this.api.register(username, password);
      this.wallet.update((wallet) => (wallet ? { ...wallet, username: account.username } : wallet));
    });
  }

  /** Switches to the wallet of an account; the guest lists of this device are moved into it. */
  login(username: string, password: string): Promise<boolean> {
    return this.attempt(async () => {
      const { token } = await this.api.login(username, password);
      this.session.use(token);
      this.wallet.set(await this.api.wallet());
    });
  }

  /** Ends the login session of this device and goes back to a new, empty guest wallet. */
  logout(): Promise<boolean> {
    return this.attempt(async () => {
      await this.api.logout();
      this.session.clear();
      this.wallet.set(await this.api.wallet());
    });
  }

  /** Runs an action; on failure, publishes the message and returns false. */
  async attempt(action: () => Promise<unknown>): Promise<boolean> {
    try {
      await action();
      this.error.set(null);
      return true;
    } catch (error) {
      this.error.set(errorMessage(error));
      return false;
    }
  }

  replace(list: GamesList): void {
    this.patchLists((lists) => lists.map((l) => (l.id === list.id ? list : l)));
  }

  private patchLists(change: (lists: GamesList[]) => GamesList[]): void {
    this.wallet.update((wallet) => (wallet ? { ...wallet, lists: change(wallet.lists) } : wallet));
  }
}
