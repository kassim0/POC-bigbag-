// Minimal stand-in for Twitch's token endpoint and IGDB's /games endpoint, so that the end-to-end
// checks need neither credentials nor the network: the backend's import batch fills its catalogue
// from here at startup.
// Usage: node e2e/fake-igdb.mjs [port]   then start the backend with
//   IGDB_CLIENT_ID=fake IGDB_CLIENT_SECRET=fake IGDB_IMPORT_ON_STARTUP=true
//   --bingbang.igdb.api-url=http://localhost:9099/v4 --bingbang.igdb.token-url=http://localhost:9099/oauth2/token
import { createServer } from 'node:http';

const GAMES = [
  'Hades', 'Hades II', 'Celeste', 'Tunic', 'Outer Wilds', 'Portal', 'Portal 2', 'Hollow Knight',
  'Stardew Valley', 'Disco Elysium', 'Inscryption', 'Return of the Obra Dinn',
].map((name, index) => ({
  id: index + 1,
  name,
  slug: name.toLowerCase().replaceAll(' ', '-'),
  first_release_date: Date.UTC(2010 + index, 2, 1 + (index % 9)) / 1000,
  genres: [{ id: 31, name: 'Adventure' }],
  platforms: [{ id: 6, name: 'PC (Microsoft Windows)', abbreviation: 'PC' }],
  total_rating: 90 - index,
  total_rating_count: 1000 - index * 10,
  updated_at: 1700000000 + index,
}));

const port = Number(process.argv[2] ?? 9099);
createServer((req, res) => {
  let body = '';
  req.on('data', (chunk) => (body += chunk));
  req.on('end', () => {
    res.setHeader('Content-Type', 'application/json');
    if (req.url.startsWith('/oauth2/token')) {
      return res.end('{"access_token": "fake-token", "expires_in": 5000000, "token_type": "bearer"}');
    }
    if (req.url !== '/v4/games' || req.method !== 'POST') {
      res.statusCode = 404;
      return res.end('[]');
    }
    if (!req.headers['client-id'] || req.headers.authorization !== 'Bearer fake-token') {
      res.statusCode = 401;
      return res.end('{"message": "Authorization Failure."}');
    }
    // Understands the part of the query language the import uses: "where id > N ...; limit M;"
    const afterId = Number(/where id > (\d+)/.exec(body)?.[1] ?? 0);
    const limit = Number(/limit (\d+)/.exec(body)?.[1] ?? 10);
    res.end(JSON.stringify(GAMES.filter((g) => g.id > afterId).slice(0, limit)));
  });
}).listen(port, () => console.log(`fake IGDB listening on ${port}`));
