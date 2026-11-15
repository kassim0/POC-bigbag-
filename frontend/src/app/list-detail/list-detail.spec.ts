import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { Api } from '../core/api';
import { game, gamesList, settle } from '../testing';
import { ListDetail } from './list-detail';

describe('ListDetail', () => {
  const api = {
    wallet: vi.fn(),
    renameList: vi.fn(),
    removeGame: vi.fn(),
    deleteList: vi.fn(),
    searchGames: vi.fn(),
  };

  async function render(id: string) {
    vi.resetAllMocks();
    api.wallet.mockResolvedValue({
      id: 'w',
      background: 'cozy',
      lists: [gamesList(1, 'Liked', game(1, 'Hades'), game(2, 'Celeste'))],
    });
    TestBed.configureTestingModule({ providers: [provideRouter([]), { provide: Api, useValue: api }] });
    const fixture = TestBed.createComponent(ListDetail);
    fixture.componentRef.setInput('id', id);
    await settle();
    await fixture.whenStable();
    return { fixture, page: fixture.nativeElement as HTMLElement };
  }

  it('shows the name and all the games of the list', async () => {
    const { page } = await render('1');
    expect(page.querySelector<HTMLInputElement>('.title-input')?.value).toBe('Liked');
    expect([...page.querySelectorAll('.game h3')].map((h) => h.textContent)).toEqual(['Hades', 'Celeste']);
  });

  it('renames the list when the name field loses focus', async () => {
    const { fixture, page } = await render('1');
    api.renameList.mockResolvedValue(gamesList(1, 'Favourites', game(1, 'Hades'), game(2, 'Celeste')));
    const input = page.querySelector<HTMLInputElement>('.title-input')!;
    input.value = 'Favourites ';
    input.dispatchEvent(new Event('blur'));
    await settle();
    await fixture.whenStable();
    expect(api.renameList).toHaveBeenCalledWith(1, 'Favourites');
    expect(input.value).toBe('Favourites');
  });

  it('removes a game', async () => {
    const { fixture, page } = await render('1');
    api.removeGame.mockResolvedValue(gamesList(1, 'Liked', game(2, 'Celeste')));
    page.querySelector<HTMLButtonElement>('[aria-label="Remove Hades"]')!.click();
    await settle();
    await fixture.whenStable();
    expect(api.removeGame).toHaveBeenCalledWith(1, 1);
    expect([...page.querySelectorAll('.game h3')].map((h) => h.textContent)).toEqual(['Celeste']);
  });

  it('deletes the list after confirmation and goes back home', async () => {
    const { fixture, page } = await render('1');
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    api.deleteList.mockResolvedValue(undefined);

    page.querySelector<HTMLButtonElement>('.danger-text')!.click();
    await fixture.whenStable();
    expect(api.deleteList).not.toHaveBeenCalled();
    page.querySelector<HTMLButtonElement>('button.danger')!.click();
    await settle();
    expect(api.deleteList).toHaveBeenCalledWith(1);
    expect(navigate).toHaveBeenCalledWith('/');
  });

  it('says so when the list does not exist', async () => {
    const { page } = await render('99');
    expect(page.textContent).toContain('does not exist');
  });
});
