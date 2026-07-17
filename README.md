# Poc_MedHead

Preuve de concept — système d'intervention d'urgence en temps réel (allocation de lits d'urgence) pour le consortium MedHead.

Le détail fonctionnel, la conformité RGPD et l'architecture sont documentés dans [`info.md`](info.md).

## Structure du repo

```
backend/    API Java / Spring Boot, architecture hexagonale (domain / infrastructure)
frontend/   Interface React (à venir)
docs/       Specs et plans d'implémentation (docs/superpowers/)
```

## Backend

### Prérequis

- Java 21
- Maven

Si `java`/`mvn` ne sont pas sur le `PATH` (ex. seul un JDK embarqué IntelliJ est disponible), exporter avant toute commande :

```bash
export JAVA_HOME="<chemin vers un JDK 21>"
export PATH="<chemin vers Maven>/bin:$JAVA_HOME/bin:$PATH"
```

### Exécuter les tests

Depuis `backend/` :

```bash
mvn -B verify
```

Lance l'ensemble de la suite (tests unitaires du domaine sans contexte Spring + tests d'intégration Spring) et produit le jar. Pour cibler une classe de test précise :

```bash
mvn -B test -Dtest=NomDeLaClasseTest
```

## Pipeline CI/CD

`.github/workflows/ci-backend.yml` : déclenché sur push vers `main` et sur toute Pull Request qui touche `backend/**`. Exécute `mvn -B verify` (JDK 21 Temurin) depuis `backend/`. Une PR ne peut pas être mergée si ce job échoue.

Le pipeline frontend (`.github/workflows/ci-frontend.yml`) sera renseigné à l'ouverture du chantier frontend.

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
