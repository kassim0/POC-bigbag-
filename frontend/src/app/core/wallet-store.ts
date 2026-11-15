import { Injectable, inject, signal } from '@angular/core';
import { Api } from './api';
import { errorMessage } from './errors';
import { Game, GamesList, Wallet } from './models';

/** The user's wallet, shared by the pages. Failed actions end up in `error`. */
@Injectable({ providedIn: 'root' })
export class WalletStore {
  private readonly api = inject(Api);
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

  removeGame(listId: number, rawgId: number): Promise<boolean> {
    return this.attempt(async () => this.replace(await this.api.removeGame(listId, rawgId)));
  }

  setComment(listId: number, rawgId: number, text: string): Promise<boolean> {
    return this.attempt(async () => this.replace(await this.api.setComment(listId, rawgId, text)));
  }

  deleteComment(listId: number, rawgId: number): Promise<boolean> {
    return this.attempt(async () => this.replace(await this.api.deleteComment(listId, rawgId)));
  }

  transferComment(listId: number, rawgId: number, targetListId: number): Promise<boolean> {
    return this.attempt(async () => this.replace(await this.api.transferComment(listId, rawgId, targetListId)));
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
