import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { settle } from '../testing';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let http: HttpClient;
  let server: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptors([authInterceptor])), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpClient);
    server = TestBed.inject(HttpTestingController);
  });

  afterEach(() => server.verify());

  it('creates one guest session and sends its token with every API call', async () => {
    const bodies: unknown[] = [];
    http.get('/api/wallet').subscribe((body) => bodies.push(body));
    http.get('/api/lists').subscribe((body) => bodies.push(body));

    server.expectOne({ method: 'POST', url: '/api/session/guest' }).flush({ token: 'guest-1' });
    await settle();

    const wallet = server.expectOne('/api/wallet');
    const lists = server.expectOne('/api/lists');
    expect(wallet.request.headers.get('Authorization')).toBe('Bearer guest-1');
    expect(lists.request.headers.get('Authorization')).toBe('Bearer guest-1');
    wallet.flush({ id: 'w' });
    lists.flush([]);
    expect(bodies).toEqual([{ id: 'w' }, []]);
  });

  it('starts a new guest session when the server no longer knows the token', async () => {
    let body: unknown;
    http.get('/api/wallet').subscribe((b) => (body = b));
    server.expectOne('/api/session/guest').flush({ token: 'stale' });
    await settle();
    server.expectOne('/api/wallet').flush({ error: 'Unknown wallet token' }, { status: 401, statusText: 'Unauthorized' });
    await settle();

    server.expectOne('/api/session/guest').flush({ token: 'fresh' });
    await settle();
    const retry = server.expectOne('/api/wallet');
    expect(retry.request.headers.get('Authorization')).toBe('Bearer fresh');
    retry.flush({ id: 'new' });
    expect(body).toEqual({ id: 'new' });
  });

  it('gives up after one retry', async () => {
    let status = 0;
    http.get('/api/wallet').subscribe({ error: (e) => (status = e.status) });
    for (const token of ['a', 'b']) {
      server.expectOne('/api/session/guest').flush({ token });
      await settle();
      server.expectOne('/api/wallet').flush(null, { status: 401, statusText: 'Unauthorized' });
      await settle();
    }
    expect(status).toBe(401);
  });
});
