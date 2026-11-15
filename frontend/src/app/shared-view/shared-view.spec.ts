import { HttpClient, HttpErrorResponse, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Api } from '../core/api';
import { authInterceptor } from '../core/auth.interceptor';
import { TOKEN_KEY } from '../core/session';
import { game, gamesList, settle } from '../testing';
import { SharedViewPage } from './shared-view';

describe('SharedViewPage', () => {
  const api = { sharedView: vi.fn() };

  async function render() {
    TestBed.configureTestingModule({ providers: [{ provide: Api, useValue: api }] });
    const fixture = TestBed.createComponent(SharedViewPage);
    fixture.componentRef.setInput('shareId', 'abc');
    await settle();
    await fixture.whenStable();
    return { fixture, page: fixture.nativeElement as HTMLElement };
  }

  beforeEach(() => vi.resetAllMocks());

  it('shows the shared lists with their 3 first games and the owner background', async () => {
    api.sharedView.mockResolvedValue({
      wholeWallet: true,
      background: 'night',
      lists: [gamesList(1, 'Liked', game(1, 'Hades'), game(2), game(3), game(4)), gamesList(2, 'To play')],
    });
    const { page } = await render();

    expect(api.sharedView).toHaveBeenCalledWith('abc');
    expect(page.querySelector('h1')?.textContent).toBe('A shared games wallet');
    expect(page.textContent).toContain('Read-only');
    expect(page.querySelectorAll('.list-card').length).toBe(2);
    expect(page.querySelectorAll('.list-card')[0].querySelectorAll('.preview li').length).toBe(3);
    expect(document.body.dataset['bg']).toBe('night');
  });

  it('opens a list read-only: games and commentaries, nothing to edit', async () => {
    api.sharedView.mockResolvedValue({
      wholeWallet: false,
      background: 'cozy',
      lists: [gamesList(1, 'Liked', { ...game(1, 'Hades'), comment: 'Loved it' }, game(2, 'Celeste'))],
    });
    const { fixture, page } = await render();
    expect(page.querySelector('h1')?.textContent).toBe('Shared games lists');

    page.querySelector<HTMLButtonElement>('.list-card')!.click();
    await fixture.whenStable();

    expect([...page.querySelectorAll('.game h3')].map((h) => h.textContent)).toEqual(['Hades', 'Celeste']);
    expect(page.querySelector('blockquote')?.textContent).toBe('Loved it');
    expect(page.querySelectorAll('input, textarea, form, app-game-search, app-share-panel').length).toBe(0);
    const buttons = [...page.querySelectorAll('button')].map((b) => b.textContent?.trim());
    expect(buttons).toEqual(['← All shared lists']);
  });

  it('explains when the link is unknown or revoked', async () => {
    api.sharedView.mockRejectedValue(new HttpErrorResponse({ status: 404 }));
    const { page } = await render();
    expect(page.textContent).toContain('This share link does not exist');
  });
});

describe('share links and the wallet token', () => {
  it('does not create a guest wallet nor send a token when a visitor opens a share link', async () => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptors([authInterceptor])), provideHttpClientTesting()],
    });
    const server = TestBed.inject(HttpTestingController);
    TestBed.inject(HttpClient).get('/api/shared/abc').subscribe();
    await settle();

    const request = server.expectOne('/api/shared/abc');
    expect(request.request.headers.has('Authorization')).toBe(false);
    server.expectNone('/api/session/guest');
    expect(localStorage.getItem(TOKEN_KEY)).toBeNull();
  });
});
