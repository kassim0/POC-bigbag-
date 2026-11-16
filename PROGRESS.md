# BingBang – suivi d'avancement

Source fonctionnelle : wiki `kassim0/bingbang.wiki` (page **V0.1** ; « Future-ideas » ignorée).
Stack : backend Spring Boot 3.5 / Java 21 (`backend/`), frontend Angular (`frontend/`), catalogue de jeux
**local**, alimenté depuis IGDB par un batch (RAWG, prévu au départ, a été abandonné à la demande de l'utilisateur).

## Phases

| # | Phase | État |
|---|-------|------|
| 1 | Backend + recherche de jeux (RAWG à l'origine, aujourd'hui catalogue local) | ✅ fait |
| 2 | CRUD GamesList (nom, ajout/retrait de jeux) | ✅ fait |
| 3 | Frontend : accueil (3 premiers jeux par liste), détail/édition | ✅ fait |
| 4 | Mode invité persistant | ✅ fait |
| 5 | Commentaires par jeu et par liste | ✅ fait |
| 6 | Compte utilisateur | ✅ fait |
| 7 | Partage en lecture seule par lien | ✅ fait |

**Périmètre V0.1 du wiki terminé** (branche `claude/bingbang-v0.1`). Voir « Reste à faire / pistes » en bas.

## Catalogue local (import IGDB) — demandé après la V0.1

| Étape | État |
|-------|------|
| Batch d'import IGDB → base locale (`backend/.../catalog/`) | ✅ fait, vérifié en réel sur 2 000 jeux |
| Recherche de l'appli branchée sur le catalogue local, RAWG retiré | ✅ fait |

- **But** : avoir notre propre base de jeux, sans dépendre d'une source externe à l'exécution.
- **Tables** : `CatalogGame` (clé = id IGDB ; nom, slug, résumé, date de 1re sortie, id d'image de jaquette,
  note /100 + nombre de notes, `updated_at` IGDB), `CatalogGenre`, `CatalogPlatform` (liens N-N), `ImportState`.
  Jaquette : `https://images.igdb.com/igdb/image/upload/t_cover_big/{coverImageId}.jpg`.
- **Parcours** (`IgdbImportJob`) : pagination par id croissant (`where id > curseur; sort id asc; limit 500`),
  plus fiable qu'un offset pendant que la base IGDB bouge. 1er passage = toute la base ; passages suivants =
  seulement `updated_at > début du passage précédent − 1 h` (recouvrement de sécurité). Le curseur est
  enregistré dans la même transaction que chaque page → reprise exacte après une interruption.
  Pause entre pages (limite IGDB : 4 requêtes/s), nouvelles tentatives sur 429/5xx, jeton renouvelé sur 401.
- **Auth** (`IgdbClient`) : jeton d'application Twitch (client credentials) ; `IGDB_CLIENT_ID` et
  `IGDB_CLIENT_SECRET` en variables d'environnement, jamais commités ; le secret part dans le corps du POST.
- **Lancement** : `IGDB_IMPORT_ON_STARTUP=true` (une fois au démarrage, en arrière-plan) et/ou
  `IGDB_IMPORT_CRON="0 0 4 * * *"` (cron Spring ; `-` = jamais, valeur par défaut).
  Réglages `bingbang.igdb.import.*` : `page-size`, `pause-ms`, `max-pages-per-run`, `where` (filtre IGDB
  optionnel pour n'importer qu'une partie, ex. `total_rating_count > 0`).
- **Limites connues** : les jeux supprimés côté IGDB ne sont pas retirés localement ; pas d'import des
  captures d'écran, sociétés, etc. (facile à ajouter dans `IgdbClient.FIELDS` + `CatalogWriter`).
- **Vérifié en réel (2026-11-16)** : le batch a tourné contre le vrai IGDB, 2 passages plafonnés à 2 pages →
  2 000 jeux (ids 1 à 2061), 23 genres, 111 plateformes, reprise correcte au curseur entre les deux passages.
  IGDB annonce ~388 000 jeux, soit ~780 requêtes pour un import complet (de l'ordre de 10–15 min).
  Non vérifié : l'obtention du jeton auprès de Twitch (`id.twitch.tv` bloqué dans la session cloud ; le test
  a utilisé un jeton fourni par l'utilisateur via le proxy) et un import complet.
- **Recherche** : `GET /api/games/search?q=&page=&pageSize=` (`catalog/GameSearchController`) cherche dans
  `CatalogGame` (nom contient, sans casse ; noms commençant par le texte d'abord, puis les plus notés).
  Aucun service externe n'est appelé à l'exécution ; seules les images de jaquettes viennent d'`images.igdb.com`.
  C'est un `LIKE '%texte%'` : suffisant pour un POC, à remplacer par un index plein texte si c'est lent sur ~388 000 jeux.
- **Listes** : `GameEntry` référence un `CatalogGame` ; l'API désigne un jeu par `{gameId}` (id IGDB) et le
  serveur lit nom/date/jaquette dans le catalogue (404 si le jeu n'y est pas). Champs JSON : `gameId`, `coverUrl`.
- **Remise à zéro** : les anciennes listes référençaient des ids RAWG. La base de dev change de fichier
  (`backend/data/bingbang-v2`) : wallets, comptes et listes repartent de zéro (décision validée par l'utilisateur).
- **Catalogue vide = recherche vide** : lancer l'import au moins une fois (voir « Lancement »).

## Choix techniques

- **Wallet & jeton** : toutes les données sont côté serveur. Un wallet est possédé par un jeton porteur
  (`Authorization: Bearer …`, seul son SHA-256 est stocké, table `AccessToken`). `POST /api/session/guest`
  crée un wallet vide + son jeton invité. Les contrôleurs reçoivent un paramètre `Session` (résolu par
  `SessionArgumentResolver`). Ce socle sert au mode invité (phase 4), aux comptes (6) et au partage (7).
- **API listes** (toutes limitées au wallet de l'appelant, 404 sinon) : `GET/PATCH /api/wallet` (fond),
  `GET/POST /api/lists`, `GET/PATCH/DELETE /api/lists/{id}`, `POST /api/lists/{id}/games`,
  `DELETE /api/lists/{id}/games/{gameId}`. Règle wiki : une liste a besoin d'un nom **ou** de jeux.
  Un même jeu une seule fois par liste (409).
- **Persistance** : JPA + H2 fichier (`backend/data/`, ignoré par git) ; H2 en mémoire pour les tests.
- **Erreurs** : `ApiException` + `ApiExceptionHandler` → corps JSON `{"error": "..."}`.
- **CORS** : `bingbang.cors.allowed-origins` (par défaut `http://localhost:4200`).

- **Frontend** (Angular 22, composants standalone, signals, zoneless, tests Vitest) :
  - `core/` : `Api` (appels HTTP typés), `SessionService` (jeton), `authInterceptor` (ajoute le jeton, recrée
    une session invité sur 401), `WalletStore` (état partagé en signals + message d'erreur), `Theme` (fond).
  - `home/` : accueil = wallet, cartes de listes (`shared/list-card` : 3 premiers jeux + « + n more »),
    formulaire de création (nom et/ou jeux), choix du fond (6 fonds prédéfinis, `body[data-bg]`).
  - `list-detail/` : renommer (blur/Entrée), ajouter via `shared/game-search` (recherche dans le catalogue, avec debounce),
    retirer, supprimer la liste (avec confirmation).
  - En dev, `ng serve` proxifie `/api` vers `localhost:8080` (`proxy.conf.json`).
- **Mode invité persistant** : le jeton invité est gardé dans `localStorage` (`bingbang.token`) ; les données
  restent côté serveur (H2 fichier). L'invité retrouve donc son wallet après fermeture de l'onglet ou du
  navigateur (vérifié en E2E avec un profil Chromium persistant fermé puis rouvert). Limite assumée : vider
  les données du site = perdre l'accès au wallet invité (d'où le compte, phase 6).
- **Commentaires** : entité `GameComment` rattachée à (liste, id du jeu) et non au `GameEntry` → retirer un jeu
  conserve le commentaire, qui réapparaît si le jeu est rajouté ; il ne disparaît que par suppression
  explicite (ou suppression de la liste). Un même jeu a un commentaire distinct par liste.
  API : `PUT`/`DELETE /api/lists/{id}/games/{gameId}/comment`, et
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
  `frontend/e2e/fake-igdb.mjs` (faux Twitch + IGDB local : le batch y remplit le catalogue au démarrage,
  donc ni identifiants ni réseau).

## Commandes

- Backend : `cd backend && mvn test` · lancer : `IGDB_CLIENT_ID=… IGDB_CLIENT_SECRET=… mvn spring-boot:run`
- Frontend : `cd frontend && npm install && npx ng build && npx ng test --watch=false` · lancer : `npx ng serve`
- E2E : `node e2e/fake-igdb.mjs` + backend avec `IGDB_CLIENT_ID=fake IGDB_CLIENT_SECRET=fake
  IGDB_IMPORT_ON_STARTUP=true` et `--bingbang.igdb.api-url=http://localhost:9099/v4
  --bingbang.igdb.token-url=http://localhost:9099/oauth2/token` + `npx ng serve`, puis `node e2e/e2e.mjs`

## Reste à faire / pistes (hors périmètre V0.1)

- Lancer un import IGDB complet et vérifier l'obtention du jeton Twitch par l'application (non testés en session cloud).
- Recherche : index plein texte si le `LIKE` est trop lent ; retrait des jeux supprimés côté IGDB.
- Déploiement : servir `frontend/dist` avec un fallback SPA vers `index.html` (liens `/shared/...`, `/lists/...`)
  et router `/api` vers le backend ; passer H2 à une vraie base (PostgreSQL) + migrations (Flyway).
- Compte : changement/réinitialisation de mot de passe, suppression, limitation des tentatives, expiration des sessions.
- Fond « personnel » : aujourd'hui 6 fonds prédéfinis ; image personnalisée possible plus tard.
- Réordonner les jeux/les listes (l'accueil montre les 3 premiers jeux dans l'ordre d'ajout).
- Page wiki « Future-ideas » volontairement ignorée.
