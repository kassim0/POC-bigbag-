import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Api } from '../core/api';
import { Wallet } from '../core/models';
import { game, gamesList, settle } from '../testing';
import { Home } from './home';

describe('Home', () => {
  let api: { wallet: ReturnType<typeof vi.fn>; createList: ReturnType<typeof vi.fn>; searchGames: ReturnType<typeof vi.fn> };

  async function render(wallet: Wallet) {
    api = {
      wallet: vi.fn().mockResolvedValue(wallet),
      createList: vi.fn(),
      searchGames: vi.fn(),
    };
    TestBed.configureTestingModule({ providers: [provideRouter([]), { provide: Api, useValue: api }] });
    const fixture = TestBed.createComponent(Home);
    await settle();
    await fixture.whenStable();
    return { fixture, page: fixture.nativeElement as HTMLElement };
  }

  it('shows every list with only its 3 first games', async () => {
    const { page } = await render({
      id: 'w',
      background: 'cozy', username: null,
      lists: [
        gamesList(1, 'Liked', game(1, 'Hades'), game(2, 'Celeste'), game(3, 'Tunic'), game(4, 'Outer Wilds'), game(5)),
        gamesList(2, '', game(9, 'Portal')),
      ],
    });

    const cards = page.querySelectorAll('.list-card');
    expect(cards.length).toBe(2);
    expect(cards[0].querySelector('h2')?.textContent).toBe('Liked');
    expect(cards[0].getAttribute('href')).toBe('/lists/1');
    const names = [...cards[0].querySelectorAll('.preview li > span')].map((el) => el.textContent?.trim());
    expect(names).toEqual(['Hades', 'Celeste', 'Tunic']);
    expect(cards[0].querySelector('.more')?.textContent).toContain('+ 2 more');
    expect(cards[0].textContent).toContain('5 games');

    expect(cards[1].querySelector('h2')?.textContent).toBe('Untitled list');
    expect(cards[1].querySelector('.more')).toBeNull();
    expect(cards[1].textContent).toContain('1 game');
  });

  it('creates a list from a name and shows it', async () => {
    const { fixture, page } = await render({ id: 'w', background: 'cozy', username: null, lists: [] });
    const submit = page.querySelector<HTMLButtonElement>('.new-list button[type=submit]')!;
    expect(submit.disabled).toBe(true); // a list needs a name or games

    const input = page.querySelector<HTMLInputElement>('.new-list input[name=name]')!;
    input.value = '  To play ';
    input.dispatchEvent(new Event('input'));
    await fixture.whenStable();
    expect(submit.disabled).toBe(false);

    api.createList.mockResolvedValue(gamesList(7, 'To play'));
    page.querySelector('form')!.dispatchEvent(new Event('submit'));
    await settle();
    await fixture.whenStable();

    expect(api.createList).toHaveBeenCalledWith('To play', []);
    expect(page.querySelector('.list-card h2')?.textContent).toBe('To play');
    expect(input.value).toBe('');
  });

  it('creates a list from picked games only', async () => {
    const { fixture, page } = await render({ id: 'w', background: 'cozy', username: null, lists: [] });
    api.searchGames.mockResolvedValue({ count: 1, page: 1, hasNext: false, results: [game(3, 'Hades')] });
    const search = page.querySelector<HTMLInputElement>('app-game-search input')!;
    search.value = 'hades';
    search.dispatchEvent(new Event('input'));
    await new Promise((resolve) => setTimeout(resolve, 350)); // search debounce
    await fixture.whenStable();
    expect(api.searchGames).toHaveBeenCalledWith('hades', 1);

    page.querySelector<HTMLButtonElement>('.results button')!.click();
    await fixture.whenStable();
    expect(page.querySelector('.chips')?.textContent).toContain('Hades');
    expect(page.querySelector('.results')?.textContent).toContain('Added');

    api.createList.mockResolvedValue(gamesList(8, '', game(3, 'Hades')));
    page.querySelector('form')!.dispatchEvent(new Event('submit'));
    await settle();
    await fixture.whenStable();
    expect(api.createList).toHaveBeenCalledWith('', [game(3, 'Hades')]);
  });
});
