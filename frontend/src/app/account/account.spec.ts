import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { Api } from '../core/api';
import { SessionService, TOKEN_KEY } from '../core/session';
import { gamesList, settle } from '../testing';
import { AccountPage } from './account';

describe('AccountPage', () => {
  const api = { wallet: vi.fn(), register: vi.fn(), login: vi.fn(), logout: vi.fn() };
  let navigate: ReturnType<typeof vi.spyOn>;

  async function render(username: string | null) {
    vi.resetAllMocks();
    localStorage.clear();
    api.wallet.mockResolvedValue({ id: 'w', background: 'cozy', username, lists: [gamesList(1, 'Liked')] });
    TestBed.configureTestingModule({ providers: [provideRouter([]), { provide: Api, useValue: api }] });
    navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    const fixture = TestBed.createComponent(AccountPage);
    await settle();
    await fixture.whenStable();
    return { fixture, page: fixture.nativeElement as HTMLElement };
  }

  function fill(page: HTMLElement, username: string, password: string) {
    page.querySelector<HTMLInputElement>('input[name=username]')!.value = username;
    page.querySelector<HTMLInputElement>('input[name=password]')!.value = password;
    page.querySelector('form')!.dispatchEvent(new Event('submit'));
  }

  it('creates an account for the current wallet', async () => {
    const { page } = await render(null);
    expect(page.textContent).toContain('You do not need an account');
    api.register.mockResolvedValue({ username: 'kass' });
    fill(page, 'kass', 'correct horse');
    await settle();
    expect(api.register).toHaveBeenCalledWith('kass', 'correct horse');
    expect(navigate).toHaveBeenCalledWith('/');
  });

  it('logs in, switches to the account token and reloads the wallet', async () => {
    const { fixture, page } = await render(null);
    [...page.querySelectorAll<HTMLButtonElement>('.tabs button')][1].click();
    await fixture.whenStable();
    api.login.mockResolvedValue({ token: 'account-token', username: 'kass' });
    api.wallet.mockResolvedValue({ id: 'acc', background: 'night', username: 'kass', lists: [] });
    fill(page, 'kass', 'correct horse');
    await settle();
    expect(api.login).toHaveBeenCalledWith('kass', 'correct horse');
    expect(localStorage.getItem(TOKEN_KEY)).toBe('account-token');
    expect(api.wallet).toHaveBeenCalledTimes(2);
    expect(navigate).toHaveBeenCalledWith('/');
  });

  it('stays on the page when the login fails', async () => {
    const { fixture, page } = await render(null);
    [...page.querySelectorAll<HTMLButtonElement>('.tabs button')][1].click();
    await fixture.whenStable();
    api.login.mockRejectedValue(new Error('401'));
    fill(page, 'kass', 'nope');
    await settle();
    expect(navigate).not.toHaveBeenCalled();
    expect(localStorage.getItem(TOKEN_KEY)).toBeNull();
  });

  it('logs out and goes back to a guest wallet', async () => {
    const { fixture, page } = await render('kass');
    expect(page.querySelector('h1')?.textContent).toBe('Hello, kass');
    TestBed.inject(SessionService).use('account-token');
    api.logout.mockResolvedValue(undefined);
    api.wallet.mockResolvedValue({ id: 'guest', background: 'cozy', username: null, lists: [] });
    page.querySelector<HTMLButtonElement>('button.ghost')!.click();
    await settle();
    await fixture.whenStable();
    expect(api.logout).toHaveBeenCalled();
    expect(localStorage.getItem(TOKEN_KEY)).toBeNull();
    expect(page.querySelector('h1')?.textContent).toBe('Create an account');
  });
});
