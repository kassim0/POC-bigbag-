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

const profile = mkdtempSync(join(tmpdir(), 'bingbang-e2e-'));
const browser = await chromium.launchPersistentContext(profile);
try {
  const page = browser.pages()[0] ?? (await browser.newPage());
  await createListAndFillIt(page);
} finally {
  await browser.close();
}
console.log('All end-to-end checks passed.');
