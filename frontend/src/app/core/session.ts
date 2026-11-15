import { HttpBackend, HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

export const TOKEN_KEY = 'bingbang.token';

/**
 * Holds the bearer token that owns the user's wallet.
 *
 * A guest wallet is created on the server the first time a token is needed. The token is kept in
 * localStorage, so a guest finds their wallet again after closing the tab or the browser.
 */
@Injectable({ providedIn: 'root' })
export class SessionService {
  // Built on HttpBackend so that these calls skip the auth interceptor.
  private readonly http = new HttpClient(inject(HttpBackend));
  private current: Promise<string> | null = null;

  token(): Promise<string> {
    this.current ??= this.restoreOrCreate().catch((error) => {
      this.current = null;
      throw error;
    });
    return this.current;
  }

  /** Switches to another wallet token (after a login for instance). */
  use(token: string): void {
    this.store(token);
    this.current = Promise.resolve(token);
  }

  /** Forgets a token the server rejected, so that the next call starts a new guest session. */
  async invalidate(token: string): Promise<void> {
    if (this.current && (await this.current.catch(() => null)) === token) {
      this.clear();
    }
  }

  /** Forgets the current token: the next call starts a new guest session. */
  clear(): void {
    this.current = null;
    try {
      localStorage.removeItem(TOKEN_KEY);
    } catch {
      // storage unavailable: nothing to forget
    }
  }

  private async restoreOrCreate(): Promise<string> {
    const saved = this.read();
    if (saved) {
      return saved;
    }
    const session = await firstValueFrom(this.http.post<{ token: string }>('/api/session/guest', null));
    this.store(session.token);
    return session.token;
  }

  private read(): string | null {
    try {
      return localStorage.getItem(TOKEN_KEY);
    } catch {
      return null; // e.g. storage blocked: the wallet then only lasts as long as the tab
    }
  }

  private store(token: string): void {
    try {
      localStorage.setItem(TOKEN_KEY, token);
    } catch {
      // see read()
    }
  }
}
