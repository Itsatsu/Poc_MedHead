# Design — Squelette backend hexagonal + broker d'événements (mock)

Date : 2026-07-17
Source : `info.md` (Document de synthèse PoC MedHead), sections 1, 2, 3.

## Contexte

Première story du backend de la PoC MedHead. Objectif : poser la fondation hexagonale du backend Spring Boot et livrer le port/adaptateur de publication d'événements (réservation de lit), avec l'adaptateur mock uniquement (pas de vrai broker Kafka/RabbitMQ — hors scope PoC, cf. info.md section 1 et 3).

## Décisions actées (brainstorming)

- **Structure du repo** : mono-repo unique, `backend/` et `frontend/` dans ce même dépôt. Pas de second repo "architecture" pour l'instant.
- **Workflow Git** : GitHub Flow — `main` toujours déployable, une branche `feat|fix|chore/<sujet>` par tâche, PR + review avant merge.
- **Build** : Maven, module unique (pas de multi-module), Java 21, Spring Boot dernière version stable 3.x.
- **GroupId / package racine** : `com.medhead.poc`, artifactId `allocation-lits`.
- **Langue du code** : anglais pour les noms de classes/packages (EventPublisher, BedReservationEvent...), même si le vocabulaire métier source (info.md) est en français.
- **Périmètre de cette story** : squelette hexagonal + port/adaptateur `EventPublisher` mock uniquement. Pas d'endpoint REST, pas de calcul de distance, pas de front dans cette story.

## Architecture

Package hexagonal à l'intérieur d'un module Maven unique :

```
backend/
  pom.xml
  src/main/java/com/medhead/poc/
    domain/
      model/
        BedReservationEvent.java      (record, immutable)
      port/
        EventPublisher.java           (interface)
    infrastructure/
      adapter/out/event/
        InMemoryEventPublisherAdapter.java
    MedHeadPocApplication.java        (@SpringBootApplication)
  src/test/java/com/medhead/poc/
    domain/...
    infrastructure/adapter/out/event/InMemoryEventPublisherAdapterTest.java
```

Contrainte clé : `domain/` ne dépend d'aucune classe Spring. C'est ce qui permet de démontrer le KPI 4 d'info.md (architecture hexagonale testable unitairement sans dépendance à Spring, sans lancer de contexte applicatif).

## Événement de domaine

`BedReservationEvent` (record immuable) :
- `id` : UUID — identifiant de l'événement (pas un identifiant patient)
- `hospitalId` : identifiant de l'hôpital ayant réservé le lit
- `specialty` : spécialité médicale concernée
- `reservedAt` : `Instant` — horodatage de la réservation

Aucune donnée patient, aucune localisation persistée dans l'événement — conforme au principe de minimisation RGPD (info.md section 2), même si le patient et la localisation ne sont pas encore modélisés dans cette story (ils arriveront avec l'endpoint REST, hors scope ici).

## Port + adaptateur

- `EventPublisher` (interface, `domain.port`) : `void publish(BedReservationEvent event)`.
- `InMemoryEventPublisherAdapter` (`infrastructure.adapter.out.event`, `@Component`) : seul adaptateur de la PoC (cf. info.md section 3 — un vrai adaptateur Kafka/RabbitMQ n'est pas codé, conforme au Principe C4 et aux simplifications de la Déclaration des travaux d'architecture).
  - Log l'événement via SLF4J à la publication.
  - Ajoute l'événement à une liste thread-safe en mémoire (`CopyOnWriteArrayList<BedReservationEvent>`).
  - Expose `List<BedReservationEvent> getPublishedEvents()` pour permettre aux tests d'écrire `assertThat(eventPublisher.getPublishedEvents())...` (équivalent à `assertThat(eventsPublies)...` mentionné dans info.md section 3).

## Tests

- Test du domaine (`BedReservationEvent`) sans dépendance Spring, si logique de validation présente.
- Test de `InMemoryEventPublisherAdapter` : publie un événement, vérifie qu'il apparaît dans `getPublishedEvents()`. Test unitaire pur (pas de `@SpringBootTest` nécessaire pour ce composant, cohérent avec le principe d'isolation hexagonale).
- Test de contexte Spring minimal (`@SpringBootTest`) pour vérifier que l'application démarre et que `EventPublisher` est bien injectable — un seul test de ce type, pour ne pas alourdir la suite.

## CI

`.github/workflows/ci-backend.yml` (actuellement vide) : déclenché sur push/PR touchant `backend/**`, exécute `mvn -B verify` (build + tests).

## Hors scope de cette story

- Endpoint REST d'allocation de lit (localisation + spécialité → hôpital).
- Calcul de distance (réel ou à vol d'oiseau), circuit breaker, cache.
- Frontend React.
- Vrai broker d'événements (Kafka/RabbitMQ).
- Modélisation de la localisation patient.

## Critères de "done"

- `mvn -B verify` passe en local et en CI.
- `InMemoryEventPublisherAdapter` testé unitairement sans démarrer de contexte Spring pour la logique métier de publication.
- Aucune classe de `domain/` n'importe de package `org.springframework.*`.
- Aucune donnée patient/localisation dans `BedReservationEvent`.
