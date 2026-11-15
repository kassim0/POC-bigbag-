import { HttpBackend, HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

/**
 * Holds the bearer token that owns the user's wallet.
 * A guest wallet is created on the server the first time a token is needed.
 */
@Injectable({ providedIn: 'root' })
export class SessionService {
  // Built on HttpBackend so that these calls skip the auth interceptor.
  private readonly http = new HttpClient(inject(HttpBackend));
  private current: Promise<string> | null = null;

  token(): Promise<string> {
    this.current ??= this.createGuest().catch((error) => {
      this.current = null;
      throw error;
    });
    return this.current;
  }

  /** Forgets a token the server rejected, so that the next call starts a new guest session. */
  async invalidate(token: string): Promise<void> {
    if (this.current && (await this.current.catch(() => null)) === token) {
      this.current = null;
    }
  }

  private async createGuest(): Promise<string> {
    const session = await firstValueFrom(this.http.post<{ token: string }>('/api/session/guest', null));
    return session.token;
  }
}
