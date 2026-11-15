# BingBang – suivi d'avancement

Source fonctionnelle : wiki `kassim0/bingbang.wiki` (page **V0.1** ; « Future-ideas » ignorée).
Stack : backend Spring Boot 3.5 / Java 21 (`backend/`), frontend Angular (`frontend/`), catalogue RAWG.

## Phases

| # | Phase | État |
|---|-------|------|
| 1 | Backend + recherche de jeux RAWG | ✅ fait |
| 2 | CRUD GamesList (nom, ajout/retrait de jeux) | ✅ fait |
| 3 | Frontend : accueil (3 premiers jeux par liste), détail/édition | ⏳ en cours |
| 4 | Mode invité persistant | à faire |
| 5 | Commentaires par jeu et par liste | à faire |
| 6 | Compte utilisateur | à faire |
| 7 | Partage en lecture seule par lien | à faire |

## Choix techniques

- **RAWG** : `RawgClient` (Spring `RestClient`) appelle `GET /games?key=…&search=…`. La clé est lue dans la
  variable d'environnement `RAWG_API_KEY` (jamais commitée). Endpoint exposé : `GET /api/games/search?q=&page=&pageSize=`.
  Sans clé → 503 ; erreur RAWG → 502 (message générique, l'URL amont contenant la clé).
- **Wallet & jeton** : toutes les données sont côté serveur. Un wallet est possédé par un jeton porteur
  (`Authorization: Bearer …`, seul son SHA-256 est stocké, table `AccessToken`). `POST /api/session/guest`
  crée un wallet vide + son jeton invité. Les contrôleurs reçoivent un paramètre `Session` (résolu par
  `SessionArgumentResolver`). Ce socle sert au mode invité (phase 4), aux comptes (6) et au partage (7).
- **API listes** (toutes limitées au wallet de l'appelant, 404 sinon) : `GET/PATCH /api/wallet` (fond),
  `GET/POST /api/lists`, `GET/PATCH/DELETE /api/lists/{id}`, `POST /api/lists/{id}/games`,
  `DELETE /api/lists/{id}/games/{rawgId}`. Règle wiki : une liste a besoin d'un nom **ou** de jeux.
  Un jeu (`GameEntry`) garde une copie de nom/date/image RAWG ; un même jeu une seule fois par liste (409).
- **Persistance** : JPA + H2 fichier (`backend/data/`, ignoré par git) ; H2 en mémoire pour les tests.
- **Erreurs** : `ApiException` + `ApiExceptionHandler` → corps JSON `{"error": "..."}`.
- **CORS** : `bingbang.cors.allowed-origins` (par défaut `http://localhost:4200`).

## Commandes

- Backend : `cd backend && mvn test` · lancer : `RAWG_API_KEY=xxx mvn spring-boot:run`

## Points d'attention

- RAWG n'accepte la clé qu'en paramètre d'URL `key=`. Dans la session cloud, le secret réseau enregistré pour
  `api.rawg.io` est injecté en en-tête HTTP par le proxy : il ne peut donc pas servir à un appel réel.
  L'intégration RAWG est couverte par des tests avec serveur HTTP simulé ; à valider en local avec une vraie clé.
