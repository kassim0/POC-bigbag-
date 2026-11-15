# BingBang

Personal **games wallet**: create as many GamesLists as you want, fill them with games from the
[RAWG](https://rawg.io/apidocs) catalogue, comment them and share them with a read-only link.

- `backend/` – Spring Boot (Java 21) REST API
- `frontend/` – Angular single page app

## Run it

```bash
# 1. backend (http://localhost:8080) – needs a RAWG API key
cd backend && RAWG_API_KEY=your-key mvn spring-boot:run

# 2. frontend (http://localhost:4200, proxies /api to the backend)
cd frontend && npm install && npx ng serve
```

See [PROGRESS.md](PROGRESS.md) for the state of the work and the technical choices.
