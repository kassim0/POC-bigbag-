# BingBang – suivi d'avancement

Source fonctionnelle : wiki `kassim0/bingbang.wiki` (page **V0.1** ; « Future-ideas » ignorée).
Stack : backend Spring Boot 3.5 / Java 21 (`backend/`), frontend Angular (`frontend/`), catalogue RAWG.

## Phases

| # | Phase | État |
|---|-------|------|
| 1 | Backend + recherche de jeux RAWG | ✅ fait |
| 2 | CRUD GamesList (nom, ajout/retrait de jeux) | ✅ fait |
| 3 | Frontend : accueil (3 premiers jeux par liste), détail/édition | ✅ fait |
| 4 | Mode invité persistant | ✅ fait |
| 5 | Commentaires par jeu et par liste | ✅ fait |
| 6 | Compte utilisateur | ✅ fait |
| 7 | Partage en lecture seule par lien | ✅ fait |

**Périmètre V0.1 du wiki terminé** (branche `claude/bingbang-v0.1`). Voir « Reste à faire / pistes » en bas.

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

- **Frontend** (Angular 22, composants standalone, signals, zoneless, tests Vitest) :
  - `core/` : `Api` (appels HTTP typés), `SessionService` (jeton), `authInterceptor` (ajoute le jeton, recrée
    une session invité sur 401), `WalletStore` (état partagé en signals + message d'erreur), `Theme` (fond).
  - `home/` : accueil = wallet, cartes de listes (`shared/list-card` : 3 premiers jeux + « + n more »),
    formulaire de création (nom et/ou jeux), choix du fond (6 fonds prédéfinis, `body[data-bg]`).
  - `list-detail/` : renommer (blur/Entrée), ajouter via `shared/game-search` (recherche RAWG avec debounce),
    retirer, supprimer la liste (avec confirmation).
  - En dev, `ng serve` proxifie `/api` vers `localhost:8080` (`proxy.conf.json`).
- **Mode invité persistant** : le jeton invité est gardé dans `localStorage` (`bingbang.token`) ; les données
  restent côté serveur (H2 fichier). L'invité retrouve donc son wallet après fermeture de l'onglet ou du
  navigateur (vérifié en E2E avec un profil Chromium persistant fermé puis rouvert). Limite assumée : vider
  les données du site = perdre l'accès au wallet invité (d'où le compte, phase 6).
- **Commentaires** : entité `GameComment` rattachée à (liste, rawgId) et non au `GameEntry` → retirer un jeu
  conserve le commentaire, qui réapparaît si le jeu est rajouté ; il ne disparaît que par suppression
  explicite (ou suppression de la liste). Un même jeu a un commentaire distinct par liste.
  API : `PUT`/`DELETE /api/lists/{id}/games/{rawgId}/comment`, et
  `POST …/comment/transfer {targetListId}`. **Choix** : « transférer » = *copier* vers une autre liste du
  wallet qui contient le même jeu (la source garde son commentaire, celui de la cible est remplacé).
  Le commentaire est renvoyé dans `GameDto.comment`.
- **Compte** (`account/`) : identifiant (3–30 caractères, insensible à la casse) + mot de passe (8–72, BCrypt
  via `spring-security-crypto` ; pas de Spring Security complet). Le compte reste facultatif.
  - `POST /api/auth/register` : le wallet invité courant **devient** le wallet du compte (rien n'est perdu),
    le jeton courant devient une session du compte.
  - `POST /api/auth/login` : nouveau jeton sur le wallet du compte. **Choix** : si l'appareil avait un wallet
    invité, ses listes sont *déplacées* dans le wallet du compte (puis wallet/jeton invité supprimés).
  - `POST /api/auth/logout` : supprime la session de cet appareil ; le front repart sur un wallet invité vide.
  - `GET /api/wallet` renvoie `username` (null pour un invité). Le front recharge le wallet quand l'onglet
    redevient visible (changements faits depuis un autre appareil).
  - Hors périmètre V0.1 : changement/réinitialisation de mot de passe, suppression de compte, limitation
    des tentatives de connexion, expiration des sessions.
- **Partage** (`share/`) : `ShareLink` = identifiant aléatoire non devinable (256 bits) + wallet + liste d'ids.
  Sans ids → tout le wallet ; un id → une liste ; plusieurs → sélection.
  - Propriétaire : `POST /api/shares {listIds}`, `GET /api/shares`, `DELETE /api/shares/{id}` (révocation).
  - Visiteur : `GET /api/shared/{id}`, **seul** endpoint accessible par le lien, public et en lecture seule
    (l'id de partage n'est pas un jeton de wallet). La vue est *vivante* (état courant du wallet), contient
    le fond et les commentaires, mais ni l'id du wallet ni le nom du compte.
  - Une liste supprimée sort des liens ; un lien dont toutes les listes ont disparu répond 404.
  - À la connexion depuis un appareil invité : les liens vers des listes suivent les listes ; un lien
    « tout le wallet invité » est supprimé (il n'exposera pas tout le wallet du compte par surprise).
  - Front : `shared/share-panel` (accueil : tout le wallet ou sélection ; détail : cette liste ; copier,
    révoquer) et route publique `/shared/:shareId` (`shared-view/`), sans aucun contrôle d'édition.
    L'intercepteur n'ajoute pas de jeton sur `/api/shared/` : un visiteur ne crée pas de wallet.
- **E2E** : `frontend/e2e/e2e.mjs` (Playwright installé globalement, pas une dépendance du projet) +
  `frontend/e2e/fake-rawg.mjs` (faux RAWG local, donc ni clé ni réseau).

## Commandes

- Backend : `cd backend && mvn test` · lancer : `RAWG_API_KEY=xxx mvn spring-boot:run`
- Frontend : `cd frontend && npm install && npx ng build && npx ng test --watch=false` · lancer : `npx ng serve`
- E2E : `node e2e/fake-rawg.mjs` + backend avec `--rawg.base-url=http://localhost:9099/api --rawg.api-key=fake`
  + `npx ng serve`, puis `node e2e/e2e.mjs`

## Points d'attention

- RAWG n'accepte la clé qu'en paramètre d'URL `key=`. Dans la session cloud, le secret réseau enregistré pour
  `api.rawg.io` est injecté en en-tête HTTP par le proxy : il ne peut donc pas servir à un appel réel.
  L'intégration RAWG est couverte par des tests avec serveur HTTP simulé ; à valider en local avec une vraie clé.

## Reste à faire / pistes (hors périmètre V0.1)

- Valider la recherche avec une vraie clé RAWG (`RAWG_API_KEY`) : non testable dans la session cloud (voir ci-dessus).
- Déploiement : servir `frontend/dist` avec un fallback SPA vers `index.html` (liens `/shared/...`, `/lists/...`)
  et router `/api` vers le backend ; passer H2 à une vraie base (PostgreSQL) + migrations (Flyway).
- Compte : changement/réinitialisation de mot de passe, suppression, limitation des tentatives, expiration des sessions.
- Fond « personnel » : aujourd'hui 6 fonds prédéfinis ; image personnalisée possible plus tard.
- Réordonner les jeux/les listes (l'accueil montre les 3 premiers jeux dans l'ordre d'ajout).
- Page wiki « Future-ideas » volontairement ignorée.
