import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { settle } from '../testing';
import { SessionService, TOKEN_KEY } from './session';

describe('SessionService', () => {
  function setUp() {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    return { session: TestBed.inject(SessionService), server: TestBed.inject(HttpTestingController) };
  }

  beforeEach(() => localStorage.clear());

  it('creates a guest session once and remembers its token', async () => {
    const { session, server } = setUp();
    const first = session.token();
    const second = session.token();
    server.expectOne({ method: 'POST', url: '/api/session/guest' }).flush({ token: 'guest-1' });
    expect(await first).toBe('guest-1');
    expect(await second).toBe('guest-1');
    expect(localStorage.getItem(TOKEN_KEY)).toBe('guest-1');
    server.verify();
  });

  it('finds the guest wallet again after the browser was closed', async () => {
    const before = setUp();
    const token = before.session.token();
    before.server.expectOne('/api/session/guest').flush({ token: 'guest-1' });
    await token;

    // A new application instance, with the same browser storage.
    const after = setUp();
    expect(await after.session.token()).toBe('guest-1');
    after.server.expectNone('/api/session/guest');
  });

  it('starts over as a new guest once the token is invalidated', async () => {
    localStorage.setItem(TOKEN_KEY, 'stale');
    const { session, server } = setUp();
    expect(await session.token()).toBe('stale');

    await session.invalidate('another-token');
    expect(await session.token()).toBe('stale');

    await session.invalidate('stale');
    expect(localStorage.getItem(TOKEN_KEY)).toBeNull();
    const next = session.token();
    server.expectOne('/api/session/guest').flush({ token: 'fresh' });
    expect(await next).toBe('fresh');
    expect(localStorage.getItem(TOKEN_KEY)).toBe('fresh');
  });

  it('retries the guest creation after a network failure', async () => {
    const { session, server } = setUp();
    const failed = session.token();
    server.expectOne('/api/session/guest').error(new ProgressEvent('error'));
    await expect(failed).rejects.toBeTruthy();

    const retried = session.token();
    await settle();
    server.expectOne('/api/session/guest').flush({ token: 'guest-2' });
    expect(await retried).toBe('guest-2');
  });

  it('switches to a given token', async () => {
    const { session } = setUp();
    session.use('account-token');
    expect(await session.token()).toBe('account-token');
    expect(localStorage.getItem(TOKEN_KEY)).toBe('account-token');
  });
});
