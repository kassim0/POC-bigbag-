// End-to-end check of BingBang in a real browser.
// Needs: fake RAWG (node e2e/fake-rawg.mjs), the backend pointed at it, and `ng serve` on :4200.
// Usage: node e2e/e2e.mjs [baseUrl]
import assert from 'node:assert/strict';
import { mkdtempSync } from 'node:fs';
import { createRequire } from 'node:module';
import { tmpdir } from 'node:os';
import { join } from 'node:path';

// Playwright is expected to be installed globally (it is not a dependency of the project).
const { chromium } = createRequire(import.meta.url)('playwright');

const BASE = process.argv[2] ?? 'http://localhost:4200';
const SHOTS = process.env.E2E_SCREENSHOTS; // optional directory for screenshots

const step = (name) => console.log(`✓ ${name}`);
const shot = async (page, name) => SHOTS && page.screenshot({ path: join(SHOTS, `${name}.png`), fullPage: true });

async function addGame(page, query, name) {
  await page.getByPlaceholder('Search the RAWG catalogue…').fill(query);
  await page.locator('.results li', { hasText: name }).first().getByRole('button', { name: 'Add' }).click();
}

async function createListAndFillIt(page) {
  await page.goto(BASE);
  await page.getByPlaceholder('Games I liked, To play…').fill('Games I liked');
  await page.getByRole('button', { name: 'Create list' }).click();
  await page.locator('.list-card', { hasText: 'Games I liked' }).click();
  await page.waitForURL(/\/lists\/\d+$/);

  for (const [query, name] of [['hades', 'Hades'], ['celeste', 'Celeste'], ['tunic', 'Tunic'], ['outer', 'Outer Wilds']]) {
    await addGame(page, query, name);
    await page.locator('.game h3', { hasText: name }).first().waitFor();
  }
  assert.equal(await page.locator('.game').count(), 4);
  step('create a list, search the catalogue and add 4 games');

  await page.getByRole('button', { name: 'Remove Tunic' }).click();
  await page.locator('.game h3', { hasText: 'Tunic' }).waitFor({ state: 'detached' });
  await addGame(page, 'portal', 'Portal 2');
  await page.locator('.game h3', { hasText: 'Portal 2' }).waitFor();
  step('remove a game, add another');

  const title = page.locator('.title-input');
  await title.fill('Games I loved');
  await title.press('Enter');
  await shot(page, 'detail');
  await page.getByRole('link', { name: '← My games wallet' }).click();
  const card = page.locator('.list-card', { hasText: 'Games I loved' });
  await card.waitFor();
  assert.deepEqual(await card.locator('.preview li > span').allTextContents(), ['Hades', 'Celeste', 'Outer Wilds']);
  assert.match(await card.innerText(), /\+ 1 more/);
  step('rename the list; home shows its 3 first games');
  await shot(page, 'home');
}

async function comments(page) {
  await page.goto(BASE);
  await page.locator('.list-card', { hasText: 'Games I loved' }).click();
  const hades = page.locator('.game', { hasText: 'Hades' }).first();
  await hades.getByLabel('Commentary on Hades').fill('Best roguelike ever');
  await hades.getByRole('button', { name: 'Save commentary' }).click();
  await hades.getByRole('button', { name: 'Delete commentary' }).waitFor();

  await page.getByRole('button', { name: 'Remove Hades', exact: true }).click();
  await page.locator('.game h3', { hasText: /^Hades$/ }).waitFor({ state: 'detached' });
  await addGame(page, 'hades', 'Hades');
  const back = page.locator('.game', { has: page.locator('h3', { hasText: /^Hades$/ }) });
  await back.waitFor();
  assert.equal(await back.getByLabel('Commentary on Hades').inputValue(), 'Best roguelike ever');
  step('a commentary survives removing the game and comes back when it is re-added');

  // Same game in a second list: its own commentary, and the first one can be copied there.
  await page.goto(BASE);
  await page.getByPlaceholder('Search the RAWG catalogue…').fill('hades');
  await page.locator('.results li', { hasText: 'Hades' }).first().getByRole('button', { name: 'Add' }).click();
  await page.getByPlaceholder('Games I liked, To play…').fill('To replay');
  await page.getByRole('button', { name: 'Create list' }).click();
  await page.locator('.list-card', { hasText: 'To replay' }).click();
  await page.waitForURL(/\/lists\/\d+$/);
  assert.equal(await page.getByLabel('Commentary on Hades').inputValue(), '');
  await page.getByRole('link', { name: '← My games wallet' }).click();
  await page.locator('.list-card', { hasText: 'Games I loved' }).click();
  await page.getByRole('button', { name: 'Copy to “To replay”' }).click();
  await page.getByRole('status').waitFor();
  await shot(page, 'comments');
  await page.getByRole('link', { name: '← My games wallet' }).click();
  await page.locator('.list-card', { hasText: 'To replay' }).click();
  await page.waitForFunction(
    () => document.querySelector('[aria-label="Commentary on Hades"]')?.value === 'Best roguelike ever',
  );
  step('a commentary is per list and can be copied to another list');

  await page.getByRole('button', { name: 'Delete commentary' }).click();
  await page.getByRole('button', { name: 'Delete commentary' }).waitFor({ state: 'detached' });
  assert.equal(await page.getByLabel('Commentary on Hades').inputValue(), '');
  step('a commentary can be deleted');
}

async function reopenBrowser(profile) {
  // Same profile directory = same browser storage, as when a user closes then reopens their browser.
  const browser = await chromium.launchPersistentContext(profile);
  const page = browser.pages()[0] ?? (await browser.newPage());
  return { browser, page };
}

const profile = mkdtempSync(join(tmpdir(), 'bingbang-e2e-'));
let { browser, page } = await reopenBrowser(profile);
try {
  await createListAndFillIt(page);

  await browser.close();
  ({ browser, page } = await reopenBrowser(profile));
  await page.goto(BASE);
  const card = page.locator('.list-card', { hasText: 'Games I loved' });
  await card.waitFor();
  assert.deepEqual(await card.locator('.preview li > span').allTextContents(), ['Hades', 'Celeste', 'Outer Wilds']);
  step('guest wallet is still there after closing and reopening the browser');

  await comments(page);
} finally {
  await browser.close();
}
console.log('All end-to-end checks passed.');
