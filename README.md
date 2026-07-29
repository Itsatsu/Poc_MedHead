# Poc_MedHead

Preuve de concept — système d'intervention d'urgence en temps réel (allocation de lits d'urgence) pour le consortium MedHead.

Le détail fonctionnel, la conformité RGPD et l'architecture sont documentés dans [`info.md`](info.md).

## Structure du repo

```
backend/    API Java / Spring Boot, architecture hexagonale (domain / infrastructure)
frontend/   Interface React / TypeScript (Vite) qui consomme l'API
docs/       Specs et plans d'implémentation (docs/superpowers/)
```

## Backend

### Prérequis

- Java 25 (LTS)
- Maven

Si `java`/`mvn` ne sont pas sur le `PATH` (ex. seul un JDK embarqué IntelliJ est disponible), exporter avant toute commande :

```bash
export JAVA_HOME="<chemin vers un JDK 25>"
export PATH="<chemin vers Maven>/bin:$JAVA_HOME/bin:$PATH"
```

PowerShell (Windows) :

```powershell
$env:JAVA_HOME = "<chemin vers un JDK 25>"
$env:PATH = "<chemin vers Maven>\bin;$env:JAVA_HOME\bin;$env:PATH"
```

Exemple avec le JDK/Maven embarqués dans IntelliJ IDEA :

```powershell
$env:JAVA_HOME = "C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.4\jbr"
$env:PATH = "C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.4\plugins\maven\lib\maven3\bin;$env:JAVA_HOME\bin;$env:PATH"
```

### Exécuter les tests

Depuis `backend/` :

```bash
mvn -B verify
```

Lance l'ensemble de la suite (tests unitaires du domaine sans contexte Spring + tests d'intégration Spring) et produit le jar. Les tests n'appellent jamais Google Maps réellement (voir ci-dessous) : ils utilisent une implémentation en mémoire du calcul de distance. Pour cibler une classe de test précise :

```bash
mvn -B test -Dtest=NomDeLaClasseTest
```

### Lancer l'application

Depuis `backend/`, une fois `JAVA_HOME`/`PATH` positionnés comme ci-dessus :

```bash
mvn spring-boot:run
```

L'API écoute sur `http://localhost:8080`. Le calcul de distance réelle (Google Maps Routes API) nécessite une clé API, sinon le service bascule automatiquement sur une estimation à vol d'oiseau (`precision: "estimee"`, voir `info.md` section 3) :

```powershell
$env:GOOGLE_MAPS_API_KEY = "<votre clé>"
mvn spring-boot:run
```

**Ne jamais commiter cette clé.** Elle n'est lue que via la variable d'environnement (`application.yml` référence `${GOOGLE_MAPS_API_KEY:}`).

Si le port 8080 est déjà occupé :

```powershell
# Identifier puis arrêter le processus qui l'occupe
Get-NetTCPConnection -LocalPort 8080 -State Listen | Select-Object OwningProcess
Stop-Process -Id <PID_trouvé> -Force

# ou démarrer sur un autre port
mvn spring-boot:run "-Dspring-boot.run.arguments=--server.port=8081"
```

Une fois lancée, l'API peut être testée avec la [collection Postman](postman/MedHead-PoC.postman_collection.json) fournie (variable `baseUrl`, par défaut `http://localhost:8080` — à adapter si un autre port est utilisé).

### Tests de charge

`backend/load-tests/` : plan JMeter validant l'exigence <200ms à 800 req/s sur `POST /api/bed-allocations`. Voir [`backend/load-tests/README.md`](backend/load-tests/README.md) pour le lancement et l'interprétation des résultats.

## Frontend

### Prérequis

- Node.js 22+ / npm

### Exécuter les tests

Depuis `frontend/` :

```bash
npm ci
npm test
```

Tests de composant (Vitest + React Testing Library), `fetch` mocké — aucun appel réel au backend. Pour le lint et le build :

```bash
npm run lint
npm run build
```

### Lancer l'application

Le backend doit tourner (voir ci-dessus) et autoriser l'origine du frontend en CORS — configurable via `MEDHEAD_CORS_ALLOWED_ORIGINS` côté backend (défaut : `http://localhost:5173`, l'origine du serveur de dev Vite).

Depuis `frontend/` :

```bash
npm ci
npm run dev
```

Ouvre `http://localhost:5173`. Par défaut l'appli appelle l'API sur `http://localhost:8080` ; pour cibler un autre port (ex. si le 8080 est occupé), créer `frontend/.env.local` :

```
VITE_API_BASE_URL=http://localhost:8082
```

## Pipeline CI/CD

`.github/workflows/ci-backend.yml` : déclenché sur push vers `main` et sur toute Pull Request qui touche `backend/**`. Exécute `mvn -B verify` (JDK 25 Temurin) depuis `backend/`.

`.github/workflows/ci-frontend.yml` : déclenché sur push vers `main` et sur toute Pull Request qui touche `frontend/**`. Exécute `npm ci`, `npm run lint`, `npm test`, `npm run build` (Node 22) depuis `frontend/`.

Une PR ne peut pas être mergée si l'un de ces jobs échoue.

## Workflow Git

Ce repo suit **GitHub Flow** :

1. `main` est toujours déployable — on n'y commite jamais directement.
2. Toute tâche part d'une branche créée depuis `main`, nommée `<type>/<sujet-court>` :
   - `feat/...` — nouvelle fonctionnalité
   - `fix/...` — correction de bug
   - `chore/...` — tâche technique (config, dépendances, CI...)
   - `docs/...` — documentation
3. Une fois la tâche terminée et les tests passants localement, ouvrir une Pull Request vers `main`.
4. La CI doit passer sur la PR avant merge.
5. Merge dans `main` seulement après validation explicite (revue de code). Pas de merge sans review.
6. Supprimer la branche après le merge.

## Documentation technique

- Specs et plans d'implémentation : `docs/superpowers/specs/` et `docs/superpowers/plans/`.
- Ce README est mis à jour à chaque changement structurant (nouvelle stack, nouvelle commande de test, évolution du pipeline ou du workflow Git).
