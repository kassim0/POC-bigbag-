// Minimal stand-in for the RAWG API, so that the end-to-end checks need neither a key nor the network.
// Usage: node e2e/fake-rawg.mjs [port]   then start the backend with
//   --rawg.base-url=http://localhost:9099/api --rawg.api-key=fake
import { createServer } from 'node:http';

const GAMES = [
  'Hades', 'Hades II', 'Celeste', 'Tunic', 'Outer Wilds', 'Portal', 'Portal 2', 'Hollow Knight',
  'Stardew Valley', 'Disco Elysium', 'Inscryption', 'Return of the Obra Dinn',
].map((name, index) => ({
  id: index + 1,
  name,
  released: `20${10 + index}-03-0${(index % 9) + 1}`,
  background_image: null,
  rating: 4.5,
}));

const port = Number(process.argv[2] ?? 9099);
createServer((req, res) => {
  const url = new URL(req.url, 'http://localhost');
  res.setHeader('Content-Type', 'application/json');
  if (url.pathname !== '/api/games') {
    res.statusCode = 404;
    return res.end('{"detail": "Not found."}');
  }
  if (!url.searchParams.get('key')) {
    res.statusCode = 401;
    return res.end('{"error": "The key parameter is not provided"}');
  }
  const search = (url.searchParams.get('search') ?? '').toLowerCase();
  const page = Number(url.searchParams.get('page') ?? 1);
  const size = Number(url.searchParams.get('page_size') ?? 20);
  const found = GAMES.filter((g) => g.name.toLowerCase().includes(search));
  const results = found.slice((page - 1) * size, page * size);
  res.end(JSON.stringify({ count: found.length, next: page * size < found.length ? 'next' : null, results }));
}).listen(port, () => console.log(`fake RAWG listening on ${port}`));
