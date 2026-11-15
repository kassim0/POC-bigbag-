import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { Game, GameSearchResult, GamesList, Wallet } from './models';

/** Typed access to the BingBang backend. */
@Injectable({ providedIn: 'root' })
export class Api {
  private readonly http = inject(HttpClient);

  searchGames(query: string, page = 1): Promise<GameSearchResult> {
    return firstValueFrom(
      this.http.get<GameSearchResult>('/api/games/search', { params: { q: query, page, pageSize: 8 } }),
    );
  }

  wallet(): Promise<Wallet> {
    return firstValueFrom(this.http.get<Wallet>('/api/wallet'));
  }

  setBackground(background: string): Promise<Wallet> {
    return firstValueFrom(this.http.patch<Wallet>('/api/wallet', { background }));
  }

  createList(name: string, games: Game[]): Promise<GamesList> {
    return firstValueFrom(this.http.post<GamesList>('/api/lists', { name, games }));
  }

  renameList(listId: number, name: string): Promise<GamesList> {
    return firstValueFrom(this.http.patch<GamesList>(`/api/lists/${listId}`, { name }));
  }

  deleteList(listId: number): Promise<void> {
    return firstValueFrom(this.http.delete<void>(`/api/lists/${listId}`));
  }

  addGame(listId: number, game: Game): Promise<GamesList> {
    return firstValueFrom(this.http.post<GamesList>(`/api/lists/${listId}/games`, game));
  }

  removeGame(listId: number, rawgId: number): Promise<GamesList> {
    return firstValueFrom(this.http.delete<GamesList>(`/api/lists/${listId}/games/${rawgId}`));
  }

  setComment(listId: number, rawgId: number, text: string): Promise<GamesList> {
    return firstValueFrom(this.http.put<GamesList>(`/api/lists/${listId}/games/${rawgId}/comment`, { text }));
  }

  deleteComment(listId: number, rawgId: number): Promise<GamesList> {
    return firstValueFrom(this.http.delete<GamesList>(`/api/lists/${listId}/games/${rawgId}/comment`));
  }

  /** Copies the commentary to the same game in another list; resolves with that other list. */
  transferComment(listId: number, rawgId: number, targetListId: number): Promise<GamesList> {
    return firstValueFrom(
      this.http.post<GamesList>(`/api/lists/${listId}/games/${rawgId}/comment/transfer`, { targetListId }),
    );
  }

  /** Turns the current guest wallet into the wallet of a new account. */
  register(username: string, password: string): Promise<{ username: string }> {
    return firstValueFrom(this.http.post<{ username: string }>('/api/auth/register', { username, password }));
  }

  login(username: string, password: string): Promise<{ token: string; username: string }> {
    return firstValueFrom(
      this.http.post<{ token: string; username: string }>('/api/auth/login', { username, password }),
    );
  }

  logout(): Promise<void> {
    return firstValueFrom(this.http.post<void>('/api/auth/logout', null));
  }
}
