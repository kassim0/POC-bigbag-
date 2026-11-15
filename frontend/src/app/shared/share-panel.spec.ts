import { TestBed } from '@angular/core/testing';
import { Api } from '../core/api';
import { ShareLink } from '../core/models';
import { WalletStore } from '../core/wallet-store';
import { gamesList, settle } from '../testing';
import { SharePanel } from './share-panel';

describe('SharePanel', () => {
  const api = { shares: vi.fn(), createShare: vi.fn(), revokeShare: vi.fn() };
  const link = (id: string, ...listIds: number[]): ShareLink => ({
    id,
    wholeWallet: listIds.length === 0,
    listIds,
    createdAt: '2026-01-01T00:00:00Z',
  });

  async function render(existing: ShareLink[], onlyList: number | null = null) {
    vi.resetAllMocks();
    api.shares.mockResolvedValue(existing);
    TestBed.configureTestingModule({ providers: [{ provide: Api, useValue: api }] });
    TestBed.inject(WalletStore).wallet.set({
      id: 'w',
      background: 'cozy',
      username: null,
      lists: [gamesList(1, 'Liked'), gamesList(2, 'To play'), gamesList(3, '')],
    });
    const fixture = TestBed.createComponent(SharePanel);
    fixture.componentRef.setInput('onlyList', onlyList);
    await settle();
    await fixture.whenStable();
    const page = fixture.nativeElement as HTMLElement;
    const createButton = () =>
      [...page.querySelectorAll<HTMLButtonElement>('button')].find((b) => b.textContent?.includes('Create a share link'))!;
    const urls = () => [...page.querySelectorAll<HTMLInputElement>('.share-links input')].map((i) => i.value);
    return { fixture, page, createButton, urls };
  }

  it('creates a link to the whole wallet', async () => {
    const { fixture, createButton, urls, page } = await render([]);
    api.createShare.mockResolvedValue(link('whole'));
    createButton().click();
    await settle();
    await fixture.whenStable();
    expect(api.createShare).toHaveBeenCalledWith([]);
    expect(urls()).toEqual([new URL('shared/whole', document.baseURI).toString()]);
    expect(page.querySelector('.share-links strong')?.textContent).toBe('My whole wallet');
  });

  it('creates one link for several selected lists', async () => {
    const { fixture, page, createButton } = await render([]);
    page.querySelectorAll<HTMLInputElement>('input[type=radio]')[1].dispatchEvent(new Event('change'));
    await fixture.whenStable();
    expect(createButton().disabled).toBe(true); // nothing selected yet

    const boxes = page.querySelectorAll<HTMLInputElement>('input[type=checkbox]');
    boxes[0].dispatchEvent(new Event('change'));
    boxes[2].dispatchEvent(new Event('change'));
    await fixture.whenStable();
    api.createShare.mockResolvedValue(link('sel', 1, 3));
    createButton().click();
    await settle();
    await fixture.whenStable();
    expect(api.createShare).toHaveBeenCalledWith([1, 3]);
    expect(page.querySelector('.share-links strong')?.textContent).toBe('Liked, Untitled list');
  });

  it('deals with a single list when asked to', async () => {
    const { fixture, page, createButton, urls } = await render([link('whole'), link('two', 2), link('both', 1, 2)], 2);
    expect(page.querySelector('input[type=radio]')).toBeNull();
    expect(urls()).toEqual([new URL('shared/two', document.baseURI).toString()]);

    api.createShare.mockResolvedValue(link('two-bis', 2));
    createButton().click();
    await settle();
    await fixture.whenStable();
    expect(api.createShare).toHaveBeenCalledWith([2]);
    expect(urls().length).toBe(2);
  });

  it('revokes a link', async () => {
    const { fixture, page, urls } = await render([link('whole'), link('one', 1)]);
    api.revokeShare.mockResolvedValue(undefined);
    page.querySelector<HTMLButtonElement>('.share-links .danger-text')!.click();
    await settle();
    await fixture.whenStable();
    expect(api.revokeShare).toHaveBeenCalledWith('whole');
    expect(urls()).toEqual([new URL('shared/one', document.baseURI).toString()]);
  });
});
