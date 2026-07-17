# Squelette backend hexagonal + broker d'événements (mock) — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Poser la fondation Maven/Spring Boot hexagonale du backend de la PoC MedHead et livrer le port `EventPublisher` + son adaptateur mock (log + liste mémoire), avec les tests correspondants.

**Architecture:** Module Maven unique dans `backend/`, package hexagonal (`domain` pur sans dépendance Spring / `infrastructure` pour les adaptateurs et le bootstrap Spring). Le domaine ne connaît que des types Java standard ; l'infrastructure implémente les ports du domaine et porte les annotations Spring.

**Tech Stack:** Java 21, Maven, Spring Boot 3.3.4 (spring-boot-starter + spring-boot-starter-test), JUnit 5, AssertJ (fourni par spring-boot-starter-test).

## Global Constraints

- Aucune classe du package `com.medhead.poc.domain` ne doit importer `org.springframework.*` (contrainte du design — KPI 4 d'info.md : architecture testable sans dépendance à Spring).
- `BedReservationEvent` ne doit contenir aucune donnée patient ni localisation (minimisation RGPD, info.md section 2).
- Noms de classes/packages en anglais (décision actée en brainstorming), groupId `com.medhead.poc`, artifactId `allocation-lits`.
- Aucun skill de normes dédié Java/Spring n'existe dans ce repo (seul `symfony-microservice-standards` existe, pour PHP/Symfony — non applicable ici). Suivre les conventions Spring Boot standard + les contraintes ci-dessus.
- Commande qualité de référence (à exécuter depuis `backend/` à chaque tâche) : `mvn -B verify`.
- Toutes les commandes Maven de ce plan s'exécutent avec le répertoire de travail positionné sur `backend/` (le `pom.xml` y est créé à la Tâche 1).

---

### Task 1: Scaffolding Maven + contexte Spring Boot

**Files:**
- Create: `backend/pom.xml`
- Create: `backend/src/main/resources/application.yml`
- Create: `backend/src/main/java/com/medhead/poc/MedHeadPocApplication.java`
- Test: `backend/src/test/java/com/medhead/poc/MedHeadPocApplicationTests.java`

**Interfaces:**
- Produces: classe `com.medhead.poc.MedHeadPocApplication` (point d'entrée `@SpringBootApplication`), utilisée implicitement par le scan de composants pour toutes les tâches suivantes.

- [ ] **Step 1: Écrire le test qui échoue**

Créer `backend/src/test/java/com/medhead/poc/MedHeadPocApplicationTests.java` :

```java
package com.medhead.poc;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class MedHeadPocApplicationTests {

    @Test
    void contextLoads() {
    }
}
```

- [ ] **Step 2: Créer le `pom.xml`**

Créer `backend/pom.xml` :

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.4</version>
        <relativePath/>
    </parent>

    <groupId>com.medhead.poc</groupId>
    <artifactId>allocation-lits</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>allocation-lits</name>
    <description>PoC MedHead - Allocation de lits d'urgence</description>

    <properties>
        <java.version>21</java.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 3: Créer le fichier de configuration**

Créer `backend/src/main/resources/application.yml` :

```yaml
spring:
  application:
    name: allocation-lits
```

- [ ] **Step 4: Lancer le test pour vérifier qu'il échoue**

Run (depuis `backend/`): `mvn -B test -Dtest=MedHeadPocApplicationTests`
Expected: FAIL — compilation ou erreur de contexte car `MedHeadPocApplication` (classe `@SpringBootApplication`) n'existe pas encore.

- [ ] **Step 5: Créer la classe principale**

Créer `backend/src/main/java/com/medhead/poc/MedHeadPocApplication.java` :

```java
package com.medhead.poc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MedHeadPocApplication {

    public static void main(String[] args) {
        SpringApplication.run(MedHeadPocApplication.class, args);
    }
}
```

- [ ] **Step 6: Lancer le test pour vérifier qu'il passe**

Run (depuis `backend/`): `mvn -B test -Dtest=MedHeadPocApplicationTests`
Expected: PASS — `contextLoads` vert.

- [ ] **Step 7: Commit**

```bash
git add backend/pom.xml backend/src/main/resources/application.yml backend/src/main/java/com/medhead/poc/MedHeadPocApplication.java backend/src/test/java/com/medhead/poc/MedHeadPocApplicationTests.java
git commit -m "chore: scaffold backend Maven/Spring Boot project"
```

**Done criterion:** `mvn -B verify` depuis `backend/` termine en `BUILD SUCCESS` avec le test `contextLoads` exécuté et vert.

---

### Task 2: Modèle de domaine `BedReservationEvent` + port `EventPublisher`

**Files:**
- Create: `backend/src/main/java/com/medhead/poc/domain/model/BedReservationEvent.java`
- Create: `backend/src/main/java/com/medhead/poc/domain/port/EventPublisher.java`
- Test: `backend/src/test/java/com/medhead/poc/domain/model/BedReservationEventTest.java`

**Interfaces:**
- Consumes: rien (package domaine pur, aucune dépendance sur les tâches précédentes hormis l'existence du module Maven de la Tâche 1).
- Produces:
  - `record BedReservationEvent(UUID id, String hospitalId, String specialty, Instant reservedAt)` — champs accessibles via `id()`, `hospitalId()`, `specialty()`, `reservedAt()`.
  - `interface EventPublisher { void publish(BedReservationEvent event); }` — utilisé par la Tâche 3 (adaptateur) et la Tâche 4 (test de câblage Spring).

- [ ] **Step 1: Écrire le test qui échoue**

Créer `backend/src/test/java/com/medhead/poc/domain/model/BedReservationEventTest.java` :

```java
package com.medhead.poc.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BedReservationEventTest {

    @Test
    void storesAllFields() {
        UUID id = UUID.randomUUID();
        Instant reservedAt = Instant.parse("2026-07-17T10:00:00Z");

        BedReservationEvent event = new BedReservationEvent(id, "hospital-1", "cardiology", reservedAt);

        assertThat(event.id()).isEqualTo(id);
        assertThat(event.hospitalId()).isEqualTo("hospital-1");
        assertThat(event.specialty()).isEqualTo("cardiology");
        assertThat(event.reservedAt()).isEqualTo(reservedAt);
    }

    @Test
    void rejectsNullHospitalId() {
        assertThatThrownBy(() ->
                new BedReservationEvent(UUID.randomUUID(), null, "cardiology", Instant.now())
        ).isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsNullSpecialty() {
        assertThatThrownBy(() ->
                new BedReservationEvent(UUID.randomUUID(), "hospital-1", null, Instant.now())
        ).isInstanceOf(NullPointerException.class);
    }
}
```

- [ ] **Step 2: Lancer le test pour vérifier qu'il échoue**

Run (depuis `backend/`): `mvn -B test -Dtest=BedReservationEventTest`
Expected: FAIL — la classe `com.medhead.poc.domain.model.BedReservationEvent` n'existe pas.

- [ ] **Step 3: Implémenter `BedReservationEvent`**

Créer `backend/src/main/java/com/medhead/poc/domain/model/BedReservationEvent.java` :

```java
package com.medhead.poc.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record BedReservationEvent(UUID id, String hospitalId, String specialty, Instant reservedAt) {

    public BedReservationEvent {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(hospitalId, "hospitalId must not be null");
        Objects.requireNonNull(specialty, "specialty must not be null");
        Objects.requireNonNull(reservedAt, "reservedAt must not be null");
    }
}
```

- [ ] **Step 4: Lancer le test pour vérifier qu'il passe**

Run (depuis `backend/`): `mvn -B test -Dtest=BedReservationEventTest`
Expected: PASS — les 3 tests sont verts.

- [ ] **Step 5: Créer le port `EventPublisher`**

Créer `backend/src/main/java/com/medhead/poc/domain/port/EventPublisher.java` :

```java
package com.medhead.poc.domain.port;

import com.medhead.poc.domain.model.BedReservationEvent;

public interface EventPublisher {

    void publish(BedReservationEvent event);
}
```

Pas de test dédié pour cette interface (aucune logique) — elle est couverte par les tests de son implémentation (Tâche 3) et de son câblage Spring (Tâche 4).

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/medhead/poc/domain/model/BedReservationEvent.java backend/src/main/java/com/medhead/poc/domain/port/EventPublisher.java backend/src/test/java/com/medhead/poc/domain/model/BedReservationEventTest.java
git commit -m "feat: add BedReservationEvent domain model and EventPublisher port"
```

**Done criterion:** `mvn -B test -Dtest=BedReservationEventTest` passe (3/3 verts) ; ni `BedReservationEvent.java` ni `EventPublisher.java` n'importent `org.springframework.*`.

---

### Task 3: Adaptateur mock `InMemoryEventPublisherAdapter`

**Files:**
- Create: `backend/src/main/java/com/medhead/poc/infrastructure/adapter/out/event/InMemoryEventPublisherAdapter.java`
- Test: `backend/src/test/java/com/medhead/poc/infrastructure/adapter/out/event/InMemoryEventPublisherAdapterTest.java`

**Interfaces:**
- Consumes: `com.medhead.poc.domain.model.BedReservationEvent` et `com.medhead.poc.domain.port.EventPublisher` (Tâche 2).
- Produces: `class InMemoryEventPublisherAdapter implements EventPublisher` avec méthode `List<BedReservationEvent> getPublishedEvents()` — utilisée par la Tâche 4.

- [ ] **Step 1: Écrire le test qui échoue**

Créer `backend/src/test/java/com/medhead/poc/infrastructure/adapter/out/event/InMemoryEventPublisherAdapterTest.java` :

```java
package com.medhead.poc.infrastructure.adapter.out.event;

import com.medhead.poc.domain.model.BedReservationEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryEventPublisherAdapterTest {

    @Test
    void publishAddsEventToPublishedEvents() {
        InMemoryEventPublisherAdapter adapter = new InMemoryEventPublisherAdapter();
        BedReservationEvent event = new BedReservationEvent(
                UUID.randomUUID(), "hospital-1", "cardiology", Instant.now());

        adapter.publish(event);

        assertThat(adapter.getPublishedEvents()).containsExactly(event);
    }

    @Test
    void publishPreservesOrderAcrossMultipleEvents() {
        InMemoryEventPublisherAdapter adapter = new InMemoryEventPublisherAdapter();
        BedReservationEvent first = new BedReservationEvent(
                UUID.randomUUID(), "hospital-1", "cardiology", Instant.now());
        BedReservationEvent second = new BedReservationEvent(
                UUID.randomUUID(), "hospital-2", "neurology", Instant.now());

        adapter.publish(first);
        adapter.publish(second);

        assertThat(adapter.getPublishedEvents()).containsExactly(first, second);
    }
}
```

- [ ] **Step 2: Lancer le test pour vérifier qu'il échoue**

Run (depuis `backend/`): `mvn -B test -Dtest=InMemoryEventPublisherAdapterTest`
Expected: FAIL — la classe `InMemoryEventPublisherAdapter` n'existe pas.

- [ ] **Step 3: Implémenter l'adaptateur**

Créer `backend/src/main/java/com/medhead/poc/infrastructure/adapter/out/event/InMemoryEventPublisherAdapter.java` :

```java
package com.medhead.poc.infrastructure.adapter.out.event;

import com.medhead.poc.domain.model.BedReservationEvent;
import com.medhead.poc.domain.port.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class InMemoryEventPublisherAdapter implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(InMemoryEventPublisherAdapter.class);

    private final List<BedReservationEvent> publishedEvents = new CopyOnWriteArrayList<>();

    @Override
    public void publish(BedReservationEvent event) {
        log.info("Publishing bed reservation event: {}", event);
        publishedEvents.add(event);
    }

    public List<BedReservationEvent> getPublishedEvents() {
        return Collections.unmodifiableList(publishedEvents);
    }
}
```

- [ ] **Step 4: Lancer le test pour vérifier qu'il passe**

Run (depuis `backend/`): `mvn -B test -Dtest=InMemoryEventPublisherAdapterTest`
Expected: PASS — les 2 tests sont verts.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/medhead/poc/infrastructure/adapter/out/event/InMemoryEventPublisherAdapter.java backend/src/test/java/com/medhead/poc/infrastructure/adapter/out/event/InMemoryEventPublisherAdapterTest.java
git commit -m "feat: add InMemoryEventPublisherAdapter mock event broker"
```

**Done criterion:** `mvn -B test -Dtest=InMemoryEventPublisherAdapterTest` passe (2/2 verts) sans démarrage de contexte Spring (pas de `@SpringBootTest` dans ce test).

---

### Task 4: Vérification du câblage Spring

**Files:**
- Test: `backend/src/test/java/com/medhead/poc/infrastructure/adapter/out/event/EventPublisherWiringTest.java`

**Interfaces:**
- Consumes: `com.medhead.poc.domain.port.EventPublisher` (Tâche 2), `com.medhead.poc.infrastructure.adapter.out.event.InMemoryEventPublisherAdapter` (Tâche 3).
- Produces: rien (tâche de vérification uniquement).

- [ ] **Step 1: Écrire le test**

Créer `backend/src/test/java/com/medhead/poc/infrastructure/adapter/out/event/EventPublisherWiringTest.java` :

```java
package com.medhead.poc.infrastructure.adapter.out.event;

import com.medhead.poc.domain.port.EventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class EventPublisherWiringTest {

    @Autowired
    private EventPublisher eventPublisher;

    @Test
    void springWiresInMemoryAdapterAsEventPublisher() {
        assertThat(eventPublisher).isInstanceOf(InMemoryEventPublisherAdapter.class);
    }
}
```

- [ ] **Step 2: Lancer le test pour vérifier qu'il passe**

Run (depuis `backend/`): `mvn -B test -Dtest=EventPublisherWiringTest`
Expected: PASS — `InMemoryEventPublisherAdapter` étant déjà annoté `@Component` (Tâche 3) et seul à implémenter `EventPublisher`, le contexte Spring l'injecte directement sans configuration supplémentaire. Si le test échoue avec une erreur `NoSuchBeanDefinitionException`, vérifier que `InMemoryEventPublisherAdapter` porte bien l'annotation `@Component`.

- [ ] **Step 3: Commit**

```bash
git add backend/src/test/java/com/medhead/poc/infrastructure/adapter/out/event/EventPublisherWiringTest.java
git commit -m "test: verify Spring wires InMemoryEventPublisherAdapter as EventPublisher"
```

**Done criterion:** `mvn -B test -Dtest=EventPublisherWiringTest` passe (1/1 vert).

---

### Task 5: CI backend

**Files:**
- Modify: `.github/workflows/ci-backend.yml` (fichier vide existant)

- [ ] **Step 1: Écrire le workflow**

Remplacer le contenu de `.github/workflows/ci-backend.yml` par :

```yaml
name: CI Backend

on:
  push:
    branches: [main]
    paths:
      - 'backend/**'
      - '.github/workflows/ci-backend.yml'
  pull_request:
    paths:
      - 'backend/**'
      - '.github/workflows/ci-backend.yml'

jobs:
  build:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: backend
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: 'maven'

      - name: Build and test
        run: mvn -B verify
```

- [ ] **Step 2: Valider la syntaxe YAML**

Ce fichier n'a pas de cycle de test applicatif (c'est de la configuration CI, pas du code de production) — la validation se fait par relecture visuelle de l'indentation (2 espaces, cohérente) et par l'exécution réelle du workflow au prochain push/PR sur GitHub Actions. Vérifier que le fichier ne contient aucune tabulation (uniquement des espaces).

- [ ] **Step 3: Commit**

```bash
git add .github/workflows/ci-backend.yml
git commit -m "ci: run mvn verify on backend changes"
```

**Done criterion:** Le fichier `.github/workflows/ci-backend.yml` n'est plus vide, déclenche `mvn -B verify` depuis `backend/` sur push/PR touchant `backend/**`. La validation complète (exécution réelle) se fera au premier push de la branche vers GitHub.

---

## Spec Coverage Check

- Structure Maven hexagonale (domain/infrastructure) → Task 1, 2, 3.
- `BedReservationEvent` sans donnée patient/localisation → Task 2.
- Port `EventPublisher` → Task 2.
- Adaptateur mock (log + liste mémoire, assertable en test) → Task 3.
- Contrainte "domain sans dépendance Spring" (KPI 4) → Task 2 (aucun import Spring), vérifiable par relecture du fichier produit.
- Test de câblage Spring minimal → Task 4.
- CI `mvn -B verify` → Task 5.
- Hors scope (endpoint REST, calcul de distance, front, vrai broker) : non traité dans ce plan, conforme au design.
