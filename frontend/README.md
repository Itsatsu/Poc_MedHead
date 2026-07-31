# Frontend — MedHead PoC

Interface React 19 / TypeScript (Vite) pour l'allocation de lits d'urgence. Consomme l'API backend (`POST /api/bed-allocations`).

Pour les prérequis, les commandes de build/test complètes et le fonctionnement d'ensemble (backend + frontend + E2E), voir le [README à la racine du repo](../README.md).

## Commandes rapides

```bash
npm ci              # installer les dépendances
npm run dev          # démarrer le serveur de dev (http://localhost:5173)
npm test             # tests de composant (Vitest + React Testing Library)
npm run lint          # Oxlint
npm run build         # build de production (tsc + vite build)
npm run test:e2e       # suite E2E Playwright (démarre aussi le backend)
```

## Stack

- [Vite](https://vite.dev) + [@vitejs/plugin-react](https://github.com/vitejs/vite-plugin-react) (SWC)
- [Oxlint](https://oxc.rs) pour le lint — voir `.oxlintrc.json`
- [Vitest](https://vitest.dev) + [Testing Library](https://testing-library.com/react) pour les tests de composant
- [Playwright](https://playwright.dev) pour les tests E2E (`e2e/`)
