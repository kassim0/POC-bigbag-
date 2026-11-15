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
    setComment: vi.fn(),
    deleteComment: vi.fn(),
    transferComment: vi.fn(),
    searchGames: vi.fn(),
  };

  async function render(id: string) {
    vi.resetAllMocks();
    api.wallet.mockResolvedValue({
      id: 'w',
      background: 'cozy',
      lists: [
        gamesList(1, 'Liked', { ...game(1, 'Hades'), comment: 'Loved it' }, game(2, 'Celeste')),
        gamesList(2, 'To replay', game(1, 'Hades')),
      ],
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

  it('shows, saves and deletes the commentary of a game', async () => {
    const { fixture, page } = await render('1');
    const hades = page.querySelector<HTMLTextAreaElement>('[aria-label="Commentary on Hades"]')!;
    const celeste = page.querySelector<HTMLTextAreaElement>('[aria-label="Commentary on Celeste"]')!;
    expect(hades.value).toBe('Loved it');
    expect(celeste.value).toBe('');
    const buttons = (label: string) =>
      [...page.querySelectorAll<HTMLButtonElement>('.game button')].filter((b) => b.textContent?.trim() === label);
    expect(buttons('Delete commentary').length).toBe(1);

    api.setComment.mockResolvedValue(
      gamesList(1, 'Liked', { ...game(1, 'Hades'), comment: 'Loved it' }, { ...game(2, 'Celeste'), comment: 'Hard' }),
    );
    celeste.value = ' Hard ';
    buttons('Save commentary')[1].click();
    await settle();
    await fixture.whenStable();
    expect(api.setComment).toHaveBeenCalledWith(1, 2, 'Hard');
    expect(buttons('Delete commentary').length).toBe(2);

    api.deleteComment.mockResolvedValue(gamesList(1, 'Liked', game(1, 'Hades'), { ...game(2, 'Celeste'), comment: 'Hard' }));
    buttons('Delete commentary')[0].click();
    await settle();
    await fixture.whenStable();
    expect(api.deleteComment).toHaveBeenCalledWith(1, 1);
    expect(hades.value).toBe('');
  });

  it('copies a commentary to another list that contains the same game', async () => {
    const { fixture, page } = await render('1');
    const copy = [...page.querySelectorAll<HTMLButtonElement>('.game button')].filter((b) =>
      b.textContent?.includes('Copy to'),
    );
    expect(copy.map((b) => b.textContent?.trim())).toEqual(['Copy to “To replay”']); // Celeste is only in this list

    api.transferComment.mockResolvedValue(gamesList(2, 'To replay', { ...game(1, 'Hades'), comment: 'Loved it' }));
    copy[0].click();
    await settle();
    await fixture.whenStable();
    expect(api.transferComment).toHaveBeenCalledWith(1, 1, 2);
    expect(page.querySelector('[role=status]')?.textContent).toContain('copied to “To replay”');
  });

  it('says so when the list does not exist', async () => {
    const { page } = await render('99');
    expect(page.textContent).toContain('does not exist');
  });
});
