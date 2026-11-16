# BingBang

Personal **games wallet**: create as many GamesLists as you want, fill them with games from BingBang's own
catalogue (imported from [IGDB](https://api-docs.igdb.com)), comment them and share them with a read-only link.

- `backend/` – Spring Boot (Java 21) REST API
- `frontend/` – Angular single page app

## Run it

```bash
# 1. backend (http://localhost:8080)
cd backend && mvn spring-boot:run

# 2. frontend (http://localhost:4200, proxies /api to the backend)
cd frontend && npm install && npx ng serve
```

## Games catalogue (IGDB import)

The app only searches its own database: run the import at least once, or the search finds nothing.
A batch copies IGDB's games (with genres, platforms, cover, release date, rating) into BingBang's own
database, then keeps it up to date. It needs a Twitch application (client id + secret):

```bash
cd backend && IGDB_CLIENT_ID=xxx IGDB_CLIENT_SECRET=yyy IGDB_IMPORT_ON_STARTUP=true mvn spring-boot:run
# or on a schedule, e.g. every night at 4:00
IGDB_IMPORT_CRON="0 0 4 * * *"
```

See [PROGRESS.md](PROGRESS.md) for the state of the work and the technical choices.
