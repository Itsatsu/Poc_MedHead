# Endpoint REST d'allocation de lit — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Exposer `POST /api/bed-allocations` qui reçoit localisation + spécialité, détermine l'hôpital le plus proche pertinent via un algorithme en 3 paliers dégradés, et publie un `BedReservationEvent` (port `EventPublisher` existant) uniquement quand un lit est confirmé.

**Architecture:** Extension du package hexagonal existant `com.medhead.poc` (Spring Boot 4.1.0, Java 25) : nouveaux types de domaine purs (`domain/model`, `domain/port`, `domain/service`) et nouveaux adaptateurs (`infrastructure/adapter/out/hospital`, `infrastructure/adapter/out/distance`, `infrastructure/adapter/in/web`, `infrastructure/config`).

**Tech Stack:** Java 25, Maven, Spring Boot 4.1.0 (ajout de `spring-boot-starter-web` pour exposer le contrôleur REST), JUnit 5, AssertJ, MockMvc.

## Global Constraints

- Aucune classe de `com.medhead.poc.domain` ne doit importer `org.springframework.*` (même contrainte que la story précédente).
- La spécialité d'une requête doit toujours être validée contre `NhsSpecialty.VALID_SPECIALTIES` (comparaison insensible à la casse) — jamais acceptée en texte libre.
- La distance exposée est en kilomètres (`distanceKm`), jamais convertie en un temps de trajet fabriqué.
- Chaque résultat porte `precision = "estimee"` (calcul à vol d'oiseau, pas de service de routing réel dans cette PoC).
- Les 3 paliers métier renvoient toujours `200` avec une proposition d'hôpital — jamais `404` (Principe A4 : ne jamais laisser le patient sans rien). Seule une requête structurellement invalide (spécialité inconnue, coordonnées manquantes/hors intervalle) renvoie `400`.
- Aucun skill de normes dédié Java/Spring n'existe dans ce repo. Suivre les conventions Spring Boot standard.
- Commande qualité de référence (à exécuter depuis `backend/` à chaque tâche) : `mvn -B verify`.
- Environnement de build local : ni `mvn` ni `java` ne sont sur le `PATH` de la machine. Utiliser le JDK et Maven fournis avec IntelliJ IDEA :
  - `JAVA_HOME = C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.4\jbr`
  - Maven : `C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.4\plugins\maven\lib\maven3` (ajouter son `bin/` au `PATH`, ou invoquer `mvn.cmd` avec son chemin complet).

---

### Task 1: Ajouter `spring-boot-starter-web`

**Files:**
- Modify: `backend/pom.xml`

**Interfaces:**
- Produces: dépendance `spring-boot-starter-web` disponible pour toutes les tâches suivantes (contrôleur REST, `MockMvc`).

- [ ] **Step 1: Ajouter la dépendance**

Dans `backend/pom.xml`, ajouter cette dépendance dans le bloc `<dependencies>`, avant `spring-boot-starter-test` :

```xml
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
```

- [ ] **Step 2: Vérifier que le build et les tests existants passent toujours**

Run (depuis `backend/`): `mvn -B verify`
Expected: `BUILD SUCCESS`, `Tests run: 7, Failures: 0, Errors: 0, Skipped: 0` (les 7 tests existants, aucun nouveau à ce stade).

- [ ] **Step 3: Commit**

```bash
git add backend/pom.xml
git commit -m "chore: add spring-boot-starter-web dependency"
```

**Done criterion:** `mvn -B verify` toujours au vert avec la nouvelle dépendance présente.

---

### Task 2: Modèle de domaine — `Hospital`, `AllocationStatus`, `BedAllocationRequest`, `BedAllocationResult`

**Files:**
- Create: `backend/src/main/java/com/medhead/poc/domain/model/Hospital.java`
- Create: `backend/src/main/java/com/medhead/poc/domain/model/AllocationStatus.java`
- Create: `backend/src/main/java/com/medhead/poc/domain/model/BedAllocationRequest.java`
- Create: `backend/src/main/java/com/medhead/poc/domain/model/BedAllocationResult.java`
- Test: `backend/src/test/java/com/medhead/poc/domain/model/HospitalTest.java`

**Interfaces:**
- Produces:
  - `record Hospital(String id, String name, Set<String> specialties, int availableBeds, double latitude, double longitude)`
  - `enum AllocationStatus { CONFIRMED, BED_NOT_CONFIRMED, SPECIALTY_NOT_AVAILABLE }`
  - `record BedAllocationRequest(double latitude, double longitude, String specialty)`
  - `record BedAllocationResult(Hospital hospital, AllocationStatus allocationStatus, String precision, double distanceKm)`
  - Utilisés par toutes les tâches suivantes.

- [ ] **Step 1: Écrire le test qui échoue**

Créer `backend/src/test/java/com/medhead/poc/domain/model/HospitalTest.java` :

```java
package com.medhead.poc.domain.model;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HospitalTest {

    @Test
    void storesAllFields() {
        Hospital hospital = new Hospital("h1", "Hopital Test", Set.of("Cardiologie"), 3, 48.85, 2.35);

        assertThat(hospital.id()).isEqualTo("h1");
        assertThat(hospital.name()).isEqualTo("Hopital Test");
        assertThat(hospital.specialties()).containsExactly("Cardiologie");
        assertThat(hospital.availableBeds()).isEqualTo(3);
        assertThat(hospital.latitude()).isEqualTo(48.85);
        assertThat(hospital.longitude()).isEqualTo(2.35);
    }

    @Test
    void rejectsNegativeAvailableBeds() {
        assertThatThrownBy(() -> new Hospital("h1", "Hopital Test", Set.of("Cardiologie"), -1, 48.85, 2.35))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
```

- [ ] **Step 2: Lancer le test pour vérifier qu'il échoue**

Run (depuis `backend/`): `mvn -B test -Dtest=HospitalTest`
Expected: FAIL — la classe `Hospital` n'existe pas.

- [ ] **Step 3: Créer `Hospital`**

Créer `backend/src/main/java/com/medhead/poc/domain/model/Hospital.java` :

```java
package com.medhead.poc.domain.model;

import java.util.Objects;
import java.util.Set;

public record Hospital(String id, String name, Set<String> specialties, int availableBeds,
                        double latitude, double longitude) {

    public Hospital {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(specialties, "specialties must not be null");
        if (availableBeds < 0) {
            throw new IllegalArgumentException("availableBeds must not be negative");
        }
    }
}
```

- [ ] **Step 4: Lancer le test pour vérifier qu'il passe**

Run (depuis `backend/`): `mvn -B test -Dtest=HospitalTest`
Expected: PASS — les 2 tests sont verts.

- [ ] **Step 5: Créer les 3 autres types**

Créer `backend/src/main/java/com/medhead/poc/domain/model/AllocationStatus.java` :

```java
package com.medhead.poc.domain.model;

public enum AllocationStatus {
    CONFIRMED,
    BED_NOT_CONFIRMED,
    SPECIALTY_NOT_AVAILABLE
}
```

Créer `backend/src/main/java/com/medhead/poc/domain/model/BedAllocationRequest.java` :

```java
package com.medhead.poc.domain.model;

public record BedAllocationRequest(double latitude, double longitude, String specialty) {
}
```

Créer `backend/src/main/java/com/medhead/poc/domain/model/BedAllocationResult.java` :

```java
package com.medhead.poc.domain.model;

public record BedAllocationResult(Hospital hospital, AllocationStatus allocationStatus,
                                   String precision, double distanceKm) {
}
```

Ces 3 types sont des porteurs de données simples exercés indirectement par les tests de `AllocateBedUseCase` (Task 5) — pas de test dédié ici. La validation du contenu de `specialty` (contre la liste NHS) et des coordonnées se fait dans `AllocateBedUseCase`, pas dans ces records.

- [ ] **Step 6: Vérifier que tout compile et que les tests passent**

Run (depuis `backend/`): `mvn -B verify`
Expected: `BUILD SUCCESS`, tous les tests verts.

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/com/medhead/poc/domain/model/Hospital.java backend/src/main/java/com/medhead/poc/domain/model/AllocationStatus.java backend/src/main/java/com/medhead/poc/domain/model/BedAllocationRequest.java backend/src/main/java/com/medhead/poc/domain/model/BedAllocationResult.java backend/src/test/java/com/medhead/poc/domain/model/HospitalTest.java
git commit -m "feat: add Hospital, AllocationStatus, BedAllocationRequest, BedAllocationResult domain types"
```

**Done criterion:** `mvn -B verify` au vert ; aucun des 4 fichiers n'importe `org.springframework.*`.

---

### Task 3: Liste de référence NHS — `NhsSpecialty`

**Files:**
- Create: `backend/src/main/java/com/medhead/poc/domain/model/NhsSpecialty.java`
- Test: `backend/src/test/java/com/medhead/poc/domain/model/NhsSpecialtyTest.java`

**Interfaces:**
- Produces: `NhsSpecialty.isValid(String specialty)` (boolean, insensible à la casse) et `NhsSpecialty.VALID_SPECIALTIES` (`Set<String>`) — utilisés par `AllocateBedUseCase` (Task 5).

- [ ] **Step 1: Écrire le test qui échoue**

Créer `backend/src/test/java/com/medhead/poc/domain/model/NhsSpecialtyTest.java` :

```java
package com.medhead.poc.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NhsSpecialtyTest {

    @Test
    void acceptsKnownSpecialty() {
        assertThat(NhsSpecialty.isValid("Cardiologie")).isTrue();
    }

    @Test
    void isCaseInsensitive() {
        assertThat(NhsSpecialty.isValid("cardiologie")).isTrue();
    }

    @Test
    void rejectsUnknownSpecialty() {
        assertThat(NhsSpecialty.isValid("Astrologie")).isFalse();
    }

    @Test
    void rejectsNullSpecialty() {
        assertThat(NhsSpecialty.isValid(null)).isFalse();
    }
}
```

- [ ] **Step 2: Lancer le test pour vérifier qu'il échoue**

Run (depuis `backend/`): `mvn -B test -Dtest=NhsSpecialtyTest`
Expected: FAIL — la classe `NhsSpecialty` n'existe pas.

- [ ] **Step 3: Créer `NhsSpecialty`**

Créer `backend/src/main/java/com/medhead/poc/domain/model/NhsSpecialty.java` :

```java
package com.medhead.poc.domain.model;

import java.util.Set;

public final class NhsSpecialty {

    public static final Set<String> VALID_SPECIALTIES = Set.of(
            "Anesthésie", "Soins intensifs", "Oncologie clinique",
            "Spécialités dentaires supplémentaires", "Radiologie dentaire et maxillo-faciale",
            "Endodontie", "Chirurgie buccale et maxillo-faciale",
            "Pathologie buccale et maxillo-faciale", "Médecine buccale", "Chirurgie buccale",
            "Orthodontie", "Dentisterie pédiatrique", "Parodontie", "Prosthodontie",
            "Dentisterie restauratrice", "Dentisterie de soins spéciaux", "Médecine d'urgence",
            "Médecine interne de soins aigus", "Allergie", "Médecine audiovestibulaire",
            "Cardiologie", "Génétique clinique", "Neurophysiologie clinique",
            "Pharmacologie clinique et thérapeutique", "Dermatologie",
            "Endocrinologie et diabète sucré", "Gastroentérologie", "Médecine générale (interne)",
            "Médecine générale", "Médecine générale (GP) 6 mois", "Médecine génito-urinaire",
            "Médecine gériatrique", "Maladies infectieuses", "Oncologie médicale",
            "Ophtalmologie médicale", "Neurologie", "Médecine du travail", "Autre",
            "Médecine palliative", "Médecine de réadaptation", "Médecine rénale",
            "Médecine respiratoire", "Rhumatologie", "Médecine du sport et de l'exercice",
            "Santé publique sexuelle et procréative", "Cardiologie pédiatrique", "Pédiatrie",
            "Pathologie chimique", "Neuropathologie diagnostique", "Histopathologie médico-légale",
            "Pathologie générale", "Hématologie", "Histopathologie", "Immunologie",
            "Microbiologie médicale", "Pathologie pédiatrique et périnatale", "Virologie",
            "Service de santé communautaire dentaire", "Service de santé communautaire médicale",
            "Santé publique dentaire", "Pratique de l'art dentaire", "Santé publique",
            "Psychiatrie infantile et adolescente", "Psychiatrie légale", "Psychiatrie générale",
            "Psychiatrie de la vieillesse", "Psychiatrie des troubles d'apprentissage",
            "Psychothérapie", "Radiologie clinique", "Médecine nucléaire",
            "Chirurgie cardiothoracique", "Chirurgie générale", "Neurochirurgie",
            "Ophtalmologie", "Otolaryngologie", "Chirurgie pédiatrique", "Chirurgie plastique",
            "Traumatologie et chirurgie orthopédique", "Urologie", "Chirurgie vasculaire"
    );

    private NhsSpecialty() {
    }

    public static boolean isValid(String specialty) {
        if (specialty == null) {
            return false;
        }
        return VALID_SPECIALTIES.stream().anyMatch(valid -> valid.equalsIgnoreCase(specialty));
    }
}
```

Liste transcrite depuis `ne pas git\git\Données de référence sur les spécialités NHS.pdf` (colonne "Spécialité" uniquement, la colonne "Groupe de spécialité" n'est pas utilisée pour la validation).

- [ ] **Step 4: Lancer le test pour vérifier qu'il passe**

Run (depuis `backend/`): `mvn -B test -Dtest=NhsSpecialtyTest`
Expected: PASS — les 4 tests sont verts.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/medhead/poc/domain/model/NhsSpecialty.java backend/src/test/java/com/medhead/poc/domain/model/NhsSpecialtyTest.java
git commit -m "feat: add NhsSpecialty reference list for specialty validation"
```

**Done criterion:** `mvn -B test -Dtest=NhsSpecialtyTest` passe (4/4 verts) ; `Set.of(...)` ne lève pas d'exception au chargement de la classe (pas de doublon dans la liste).

---

### Task 4: Ports — `HospitalRepository`, `DistanceCalculator`

**Files:**
- Create: `backend/src/main/java/com/medhead/poc/domain/port/HospitalRepository.java`
- Create: `backend/src/main/java/com/medhead/poc/domain/port/DistanceCalculator.java`

**Interfaces:**
- Consumes: `com.medhead.poc.domain.model.Hospital` (Task 2).
- Produces:
  - `interface HospitalRepository { List<Hospital> findAll(); }` — implémenté par `InMemoryHospitalRepository` (Task 7), consommé par `AllocateBedUseCase` (Task 5).
  - `interface DistanceCalculator { double distanceKm(double latitude1, double longitude1, double latitude2, double longitude2); }` — implémenté par `HaversineDistanceCalculator` (Task 6), consommé par `AllocateBedUseCase` (Task 5).

- [ ] **Step 1: Créer `HospitalRepository`**

Créer `backend/src/main/java/com/medhead/poc/domain/port/HospitalRepository.java` :

```java
package com.medhead.poc.domain.port;

import com.medhead.poc.domain.model.Hospital;

import java.util.List;

public interface HospitalRepository {

    List<Hospital> findAll();
}
```

- [ ] **Step 2: Créer `DistanceCalculator`**

Créer `backend/src/main/java/com/medhead/poc/domain/port/DistanceCalculator.java` :

```java
package com.medhead.poc.domain.port;

public interface DistanceCalculator {

    double distanceKm(double latitude1, double longitude1, double latitude2, double longitude2);
}
```

Ce sont des interfaces pures, sans logique — pas de test dédié (elles seront exercées via leurs implémentations dans les tâches suivantes).

- [ ] **Step 3: Vérifier que tout compile**

Run (depuis `backend/`): `mvn -B compile`
Expected: `BUILD SUCCESS`.

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/medhead/poc/domain/port/HospitalRepository.java backend/src/main/java/com/medhead/poc/domain/port/DistanceCalculator.java
git commit -m "feat: add HospitalRepository and DistanceCalculator ports"
```

**Done criterion:** `mvn -B compile` réussit ; aucun des 2 fichiers n'importe `org.springframework.*`.

---

### Task 5: `AllocateBedUseCase` — algorithme en 3 paliers

**Files:**
- Create: `backend/src/main/java/com/medhead/poc/domain/service/InvalidBedAllocationRequestException.java`
- Create: `backend/src/main/java/com/medhead/poc/domain/service/AllocateBedUseCase.java`
- Test: `backend/src/test/java/com/medhead/poc/domain/service/AllocateBedUseCaseTest.java`

**Interfaces:**
- Consumes: `Hospital`, `AllocationStatus`, `BedAllocationRequest`, `BedAllocationResult`, `NhsSpecialty` (Task 2, 3) ; `HospitalRepository`, `DistanceCalculator` (Task 4) ; `com.medhead.poc.domain.model.BedReservationEvent` et `com.medhead.poc.domain.port.EventPublisher` (story précédente, déjà en `main`).
- Produces:
  - `class AllocateBedUseCase` avec constructeur `AllocateBedUseCase(HospitalRepository, DistanceCalculator, EventPublisher)` et méthode `BedAllocationResult allocate(BedAllocationRequest request)` — utilisé par `UseCaseConfiguration` (Task 8) et `BedAllocationController` (Task 9).
  - `class InvalidBedAllocationRequestException extends RuntimeException` — utilisé par `BedAllocationController` (Task 9) pour mapper vers `400`.

- [ ] **Step 1: Écrire le test qui échoue**

Créer `backend/src/test/java/com/medhead/poc/domain/service/AllocateBedUseCaseTest.java` :

```java
package com.medhead.poc.domain.service;

import com.medhead.poc.domain.model.AllocationStatus;
import com.medhead.poc.domain.model.BedAllocationRequest;
import com.medhead.poc.domain.model.BedAllocationResult;
import com.medhead.poc.domain.model.BedReservationEvent;
import com.medhead.poc.domain.model.Hospital;
import com.medhead.poc.domain.port.DistanceCalculator;
import com.medhead.poc.domain.port.EventPublisher;
import com.medhead.poc.domain.port.HospitalRepository;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AllocateBedUseCaseTest {

    private static final Hospital NEAR_HOSPITAL_WITH_BED = new Hospital(
            "near-with-bed", "Near With Bed", Set.of("Cardiologie"), 2, 48.85, 2.35);
    private static final Hospital FAR_HOSPITAL_WITH_BED = new Hospital(
            "far-with-bed", "Far With Bed", Set.of("Cardiologie"), 3, 48.90, 2.40);
    private static final Hospital NEAR_HOSPITAL_NO_BED = new Hospital(
            "near-no-bed", "Near No Bed", Set.of("Cardiologie"), 0, 48.86, 2.34);
    private static final Hospital HOSPITAL_OTHER_SPECIALTY = new Hospital(
            "other-specialty", "Other Specialty", Set.of("Urologie"), 5, 48.87, 2.30);

    @Test
    void confirmsNearestHospitalWithSpecialtyAndBed() {
        StubHospitalRepository repository = new StubHospitalRepository(
                List.of(FAR_HOSPITAL_WITH_BED, NEAR_HOSPITAL_WITH_BED));
        StubDistanceCalculator distances = new StubDistanceCalculator();
        distances.distanceTo(NEAR_HOSPITAL_WITH_BED, 1.0);
        distances.distanceTo(FAR_HOSPITAL_WITH_BED, 5.0);
        RecordingEventPublisher events = new RecordingEventPublisher();
        AllocateBedUseCase useCase = new AllocateBedUseCase(repository, distances, events);

        BedAllocationResult result = useCase.allocate(
                new BedAllocationRequest(48.85, 2.35, "Cardiologie"));

        assertThat(result.hospital()).isEqualTo(NEAR_HOSPITAL_WITH_BED);
        assertThat(result.allocationStatus()).isEqualTo(AllocationStatus.CONFIRMED);
        assertThat(result.precision()).isEqualTo("estimee");
        assertThat(result.distanceKm()).isEqualTo(1.0);
        assertThat(events.getPublishedEvents()).hasSize(1);
    }

    @Test
    void returnsBedNotConfirmedWhenSpecialtyHasNoAvailableBed() {
        StubHospitalRepository repository = new StubHospitalRepository(List.of(NEAR_HOSPITAL_NO_BED));
        StubDistanceCalculator distances = new StubDistanceCalculator();
        distances.distanceTo(NEAR_HOSPITAL_NO_BED, 2.0);
        RecordingEventPublisher events = new RecordingEventPublisher();
        AllocateBedUseCase useCase = new AllocateBedUseCase(repository, distances, events);

        BedAllocationResult result = useCase.allocate(
                new BedAllocationRequest(48.86, 2.34, "Cardiologie"));

        assertThat(result.hospital()).isEqualTo(NEAR_HOSPITAL_NO_BED);
        assertThat(result.allocationStatus()).isEqualTo(AllocationStatus.BED_NOT_CONFIRMED);
        assertThat(events.getPublishedEvents()).isEmpty();
    }

    @Test
    void returnsSpecialtyNotAvailableWhenNoHospitalHasSpecialty() {
        StubHospitalRepository repository = new StubHospitalRepository(List.of(HOSPITAL_OTHER_SPECIALTY));
        StubDistanceCalculator distances = new StubDistanceCalculator();
        distances.distanceTo(HOSPITAL_OTHER_SPECIALTY, 3.0);
        RecordingEventPublisher events = new RecordingEventPublisher();
        AllocateBedUseCase useCase = new AllocateBedUseCase(repository, distances, events);

        BedAllocationResult result = useCase.allocate(
                new BedAllocationRequest(48.87, 2.30, "Cardiologie"));

        assertThat(result.hospital()).isEqualTo(HOSPITAL_OTHER_SPECIALTY);
        assertThat(result.allocationStatus()).isEqualTo(AllocationStatus.SPECIALTY_NOT_AVAILABLE);
        assertThat(events.getPublishedEvents()).isEmpty();
    }

    @Test
    void rejectsUnknownSpecialty() {
        AllocateBedUseCase useCase = new AllocateBedUseCase(
                new StubHospitalRepository(List.of(NEAR_HOSPITAL_WITH_BED)),
                new StubDistanceCalculator(),
                new RecordingEventPublisher());

        assertThatThrownBy(() -> useCase.allocate(
                new BedAllocationRequest(48.85, 2.35, "Astrologie")))
                .isInstanceOf(InvalidBedAllocationRequestException.class);
    }

    private static final class StubHospitalRepository implements HospitalRepository {
        private final List<Hospital> hospitals;

        private StubHospitalRepository(List<Hospital> hospitals) {
            this.hospitals = hospitals;
        }

        @Override
        public List<Hospital> findAll() {
            return hospitals;
        }
    }

    private static final class StubDistanceCalculator implements DistanceCalculator {
        private final Map<String, Double> distances = new HashMap<>();

        private void distanceTo(Hospital hospital, double distanceKm) {
            distances.put(key(hospital.latitude(), hospital.longitude()), distanceKm);
        }

        @Override
        public double distanceKm(double lat1, double lon1, double lat2, double lon2) {
            return distances.getOrDefault(key(lat2, lon2), Double.MAX_VALUE);
        }

        private static String key(double lat, double lon) {
            return lat + ":" + lon;
        }
    }

    private static final class RecordingEventPublisher implements EventPublisher {
        private final List<BedReservationEvent> publishedEvents = new CopyOnWriteArrayList<>();

        @Override
        public void publish(BedReservationEvent event) {
            publishedEvents.add(event);
        }

        List<BedReservationEvent> getPublishedEvents() {
            return publishedEvents;
        }
    }
}
```

- [ ] **Step 2: Lancer le test pour vérifier qu'il échoue**

Run (depuis `backend/`): `mvn -B test -Dtest=AllocateBedUseCaseTest`
Expected: FAIL — `AllocateBedUseCase` et `InvalidBedAllocationRequestException` n'existent pas.

- [ ] **Step 3: Créer `InvalidBedAllocationRequestException`**

Créer `backend/src/main/java/com/medhead/poc/domain/service/InvalidBedAllocationRequestException.java` :

```java
package com.medhead.poc.domain.service;

public class InvalidBedAllocationRequestException extends RuntimeException {

    public InvalidBedAllocationRequestException(String message) {
        super(message);
    }
}
```

- [ ] **Step 4: Créer `AllocateBedUseCase`**

Créer `backend/src/main/java/com/medhead/poc/domain/service/AllocateBedUseCase.java` :

```java
package com.medhead.poc.domain.service;

import com.medhead.poc.domain.model.AllocationStatus;
import com.medhead.poc.domain.model.BedAllocationRequest;
import com.medhead.poc.domain.model.BedAllocationResult;
import com.medhead.poc.domain.model.BedReservationEvent;
import com.medhead.poc.domain.model.Hospital;
import com.medhead.poc.domain.model.NhsSpecialty;
import com.medhead.poc.domain.port.DistanceCalculator;
import com.medhead.poc.domain.port.EventPublisher;
import com.medhead.poc.domain.port.HospitalRepository;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public class AllocateBedUseCase {

    private final HospitalRepository hospitalRepository;
    private final DistanceCalculator distanceCalculator;
    private final EventPublisher eventPublisher;

    public AllocateBedUseCase(HospitalRepository hospitalRepository,
                               DistanceCalculator distanceCalculator,
                               EventPublisher eventPublisher) {
        this.hospitalRepository = hospitalRepository;
        this.distanceCalculator = distanceCalculator;
        this.eventPublisher = eventPublisher;
    }

    public BedAllocationResult allocate(BedAllocationRequest request) {
        if (!NhsSpecialty.isValid(request.specialty())) {
            throw new InvalidBedAllocationRequestException(
                    "Unknown specialty: " + request.specialty());
        }
        if (request.latitude() < -90 || request.latitude() > 90
                || request.longitude() < -180 || request.longitude() > 180) {
            throw new InvalidBedAllocationRequestException("Invalid coordinates");
        }

        List<Hospital> hospitals = hospitalRepository.findAll();

        Optional<Hospital> confirmed = nearest(hospitals.stream()
                .filter(h -> hasSpecialty(h, request.specialty()))
                .filter(h -> h.availableBeds() > 0), request);
        if (confirmed.isPresent()) {
            Hospital hospital = confirmed.get();
            eventPublisher.publish(new BedReservationEvent(
                    UUID.randomUUID(), hospital.id(), request.specialty(), Instant.now()));
            return toResult(hospital, AllocationStatus.CONFIRMED, request);
        }

        Optional<Hospital> specialtyOnly = nearest(hospitals.stream()
                .filter(h -> hasSpecialty(h, request.specialty())), request);
        if (specialtyOnly.isPresent()) {
            return toResult(specialtyOnly.get(), AllocationStatus.BED_NOT_CONFIRMED, request);
        }

        Hospital anyHospital = nearest(hospitals.stream(), request)
                .orElseThrow(() -> new IllegalStateException("No hospital available"));
        return toResult(anyHospital, AllocationStatus.SPECIALTY_NOT_AVAILABLE, request);
    }

    private boolean hasSpecialty(Hospital hospital, String specialty) {
        return hospital.specialties().stream().anyMatch(s -> s.equalsIgnoreCase(specialty));
    }

    private Optional<Hospital> nearest(Stream<Hospital> hospitals, BedAllocationRequest request) {
        return hospitals.min(Comparator.comparingDouble(h -> distanceCalculator.distanceKm(
                request.latitude(), request.longitude(), h.latitude(), h.longitude())));
    }

    private BedAllocationResult toResult(Hospital hospital, AllocationStatus status,
                                          BedAllocationRequest request) {
        double distanceKm = distanceCalculator.distanceKm(
                request.latitude(), request.longitude(), hospital.latitude(), hospital.longitude());
        return new BedAllocationResult(hospital, status, "estimee", distanceKm);
    }
}
```

- [ ] **Step 5: Lancer le test pour vérifier qu'il passe**

Run (depuis `backend/`): `mvn -B test -Dtest=AllocateBedUseCaseTest`
Expected: PASS — les 4 tests sont verts.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/medhead/poc/domain/service/InvalidBedAllocationRequestException.java backend/src/main/java/com/medhead/poc/domain/service/AllocateBedUseCase.java backend/src/test/java/com/medhead/poc/domain/service/AllocateBedUseCaseTest.java
git commit -m "feat: add AllocateBedUseCase with 3-tier degraded search"
```

**Done criterion:** `mvn -B test -Dtest=AllocateBedUseCaseTest` passe (4/4 verts) ; l'événement n'est publié que dans le cas `CONFIRMED` (vérifié par le test) ; aucun fichier de ce package n'importe `org.springframework.*`.

---

### Task 6: `HaversineDistanceCalculator`

**Files:**
- Create: `backend/src/main/java/com/medhead/poc/infrastructure/adapter/out/distance/HaversineDistanceCalculator.java`
- Test: `backend/src/test/java/com/medhead/poc/infrastructure/adapter/out/distance/HaversineDistanceCalculatorTest.java`

**Interfaces:**
- Consumes: `com.medhead.poc.domain.port.DistanceCalculator` (Task 4).
- Produces: `class HaversineDistanceCalculator implements DistanceCalculator` — utilisé par `UseCaseConfiguration` (Task 8) via injection Spring.

- [ ] **Step 1: Écrire le test qui échoue**

Créer `backend/src/test/java/com/medhead/poc/infrastructure/adapter/out/distance/HaversineDistanceCalculatorTest.java` :

```java
package com.medhead.poc.infrastructure.adapter.out.distance;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class HaversineDistanceCalculatorTest {

    private final HaversineDistanceCalculator calculator = new HaversineDistanceCalculator();

    @Test
    void oneDegreeOfLatitudeIsApproximately111Km() {
        double distance = calculator.distanceKm(48.0, 2.0, 49.0, 2.0);

        assertThat(distance).isCloseTo(111.19, within(0.05));
    }

    @Test
    void distanceBetweenIdenticalPointsIsZero() {
        double distance = calculator.distanceKm(48.8566, 2.3522, 48.8566, 2.3522);

        assertThat(distance).isCloseTo(0.0, within(1e-9));
    }
}
```

- [ ] **Step 2: Lancer le test pour vérifier qu'il échoue**

Run (depuis `backend/`): `mvn -B test -Dtest=HaversineDistanceCalculatorTest`
Expected: FAIL — la classe `HaversineDistanceCalculator` n'existe pas.

- [ ] **Step 3: Implémenter**

Créer `backend/src/main/java/com/medhead/poc/infrastructure/adapter/out/distance/HaversineDistanceCalculator.java` :

```java
package com.medhead.poc.infrastructure.adapter.out.distance;

import com.medhead.poc.domain.port.DistanceCalculator;
import org.springframework.stereotype.Component;

@Component
public class HaversineDistanceCalculator implements DistanceCalculator {

    private static final double EARTH_RADIUS_KM = 6371.0;

    @Override
    public double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLat = Math.toRadians(lat2 - lat1);
        double deltaLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad)
                * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double c = 2 * Math.asin(Math.sqrt(a));

        return EARTH_RADIUS_KM * c;
    }
}
```

- [ ] **Step 4: Lancer le test pour vérifier qu'il passe**

Run (depuis `backend/`): `mvn -B test -Dtest=HaversineDistanceCalculatorTest`
Expected: PASS — les 2 tests sont verts.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/medhead/poc/infrastructure/adapter/out/distance/HaversineDistanceCalculator.java backend/src/test/java/com/medhead/poc/infrastructure/adapter/out/distance/HaversineDistanceCalculatorTest.java
git commit -m "feat: add HaversineDistanceCalculator adapter"
```

**Done criterion:** `mvn -B test -Dtest=HaversineDistanceCalculatorTest` passe (2/2 verts), sans démarrage de contexte Spring.

---

### Task 7: `InMemoryHospitalRepository`

**Files:**
- Create: `backend/src/main/java/com/medhead/poc/infrastructure/adapter/out/hospital/InMemoryHospitalRepository.java`
- Test: `backend/src/test/java/com/medhead/poc/infrastructure/adapter/out/hospital/InMemoryHospitalRepositoryTest.java`

**Interfaces:**
- Consumes: `com.medhead.poc.domain.model.Hospital` (Task 2), `com.medhead.poc.domain.port.HospitalRepository` (Task 4).
- Produces: `class InMemoryHospitalRepository implements HospitalRepository` — utilisé par `UseCaseConfiguration` (Task 8) via injection Spring, et par le test d'intégration du contrôleur (Task 9).

- [ ] **Step 1: Écrire le test qui échoue**

Créer `backend/src/test/java/com/medhead/poc/infrastructure/adapter/out/hospital/InMemoryHospitalRepositoryTest.java` :

```java
package com.medhead.poc.infrastructure.adapter.out.hospital;

import com.medhead.poc.domain.model.Hospital;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryHospitalRepositoryTest {

    @Test
    void returnsTheThreeReferenceHospitals() {
        InMemoryHospitalRepository repository = new InMemoryHospitalRepository();

        List<Hospital> hospitals = repository.findAll();

        assertThat(hospitals).hasSize(3);
        assertThat(hospitals).extracting(Hospital::id)
                .containsExactlyInAnyOrder("fred-brooks", "julia-crusher", "beverly-bashir");
        assertThat(hospitals).filteredOn(h -> h.id().equals("julia-crusher"))
                .first()
                .satisfies(h -> assertThat(h.availableBeds()).isZero());
    }
}
```

- [ ] **Step 2: Lancer le test pour vérifier qu'il échoue**

Run (depuis `backend/`): `mvn -B test -Dtest=InMemoryHospitalRepositoryTest`
Expected: FAIL — la classe `InMemoryHospitalRepository` n'existe pas.

- [ ] **Step 3: Implémenter**

Créer `backend/src/main/java/com/medhead/poc/infrastructure/adapter/out/hospital/InMemoryHospitalRepository.java` :

```java
package com.medhead.poc.infrastructure.adapter.out.hospital;

import com.medhead.poc.domain.model.Hospital;
import com.medhead.poc.domain.port.HospitalRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class InMemoryHospitalRepository implements HospitalRepository {

    private static final List<Hospital> HOSPITALS = List.of(
            new Hospital("fred-brooks", "Hopital Fred Brooks",
                    Set.of("Cardiologie", "Immunologie"), 2, 48.8566, 2.3522),
            new Hospital("julia-crusher", "Hopital Julia Crusher",
                    Set.of("Cardiologie"), 0, 48.8606, 2.3376),
            new Hospital("beverly-bashir", "Hopital Beverly Bashir",
                    Set.of("Immunologie", "Neuropathologie diagnostique"), 5, 48.8738, 2.2950)
    );

    @Override
    public List<Hospital> findAll() {
        return HOSPITALS;
    }
}
```

- [ ] **Step 4: Lancer le test pour vérifier qu'il passe**

Run (depuis `backend/`): `mvn -B test -Dtest=InMemoryHospitalRepositoryTest`
Expected: PASS — le test est vert.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/medhead/poc/infrastructure/adapter/out/hospital/InMemoryHospitalRepository.java backend/src/test/java/com/medhead/poc/infrastructure/adapter/out/hospital/InMemoryHospitalRepositoryTest.java
git commit -m "feat: add InMemoryHospitalRepository with reference scenario hospitals"
```

**Done criterion:** `mvn -B test -Dtest=InMemoryHospitalRepositoryTest` passe (1/1 vert), sans démarrage de contexte Spring.

---

### Task 8: `UseCaseConfiguration` — câblage Spring

**Files:**
- Create: `backend/src/main/java/com/medhead/poc/infrastructure/config/UseCaseConfiguration.java`
- Test: `backend/src/test/java/com/medhead/poc/infrastructure/config/UseCaseConfigurationTest.java`

**Interfaces:**
- Consumes: `AllocateBedUseCase` (Task 5), `HospitalRepository` implémenté par `InMemoryHospitalRepository` (Task 7), `DistanceCalculator` implémenté par `HaversineDistanceCalculator` (Task 6), `EventPublisher` implémenté par `InMemoryEventPublisherAdapter` (story précédente, déjà `@Component` en `main`).
- Produces: bean Spring `AllocateBedUseCase` disponible pour injection — utilisé par `BedAllocationController` (Task 9).

- [ ] **Step 1: Écrire le test**

Créer `backend/src/test/java/com/medhead/poc/infrastructure/config/UseCaseConfigurationTest.java` :

```java
package com.medhead.poc.infrastructure.config;

import com.medhead.poc.domain.service.AllocateBedUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class UseCaseConfigurationTest {

    @Autowired
    private AllocateBedUseCase allocateBedUseCase;

    @Test
    void allocateBedUseCaseIsWired() {
        assertThat(allocateBedUseCase).isNotNull();
    }
}
```

- [ ] **Step 2: Créer `UseCaseConfiguration`**

Créer `backend/src/main/java/com/medhead/poc/infrastructure/config/UseCaseConfiguration.java` :

```java
package com.medhead.poc.infrastructure.config;

import com.medhead.poc.domain.port.DistanceCalculator;
import com.medhead.poc.domain.port.EventPublisher;
import com.medhead.poc.domain.port.HospitalRepository;
import com.medhead.poc.domain.service.AllocateBedUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfiguration {

    @Bean
    public AllocateBedUseCase allocateBedUseCase(HospitalRepository hospitalRepository,
                                                   DistanceCalculator distanceCalculator,
                                                   EventPublisher eventPublisher) {
        return new AllocateBedUseCase(hospitalRepository, distanceCalculator, eventPublisher);
    }
}
```

- [ ] **Step 3: Lancer le test pour vérifier qu'il passe**

Run (depuis `backend/`): `mvn -B test -Dtest=UseCaseConfigurationTest`
Expected: PASS — le test est vert. Si `NoSuchBeanDefinitionException` : vérifier que `InMemoryHospitalRepository` et `HaversineDistanceCalculator` sont bien annotés `@Component` (Tasks 6 et 7) et qu'un seul bean existe pour chaque port.

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/medhead/poc/infrastructure/config/UseCaseConfiguration.java backend/src/test/java/com/medhead/poc/infrastructure/config/UseCaseConfigurationTest.java
git commit -m "feat: wire AllocateBedUseCase as a Spring bean"
```

**Done criterion:** `mvn -B test -Dtest=UseCaseConfigurationTest` passe (1/1 vert).

---

### Task 9: `BedAllocationController` — endpoint REST

**Files:**
- Create: `backend/src/main/java/com/medhead/poc/infrastructure/adapter/in/web/BedAllocationRequestDto.java`
- Create: `backend/src/main/java/com/medhead/poc/infrastructure/adapter/in/web/BedAllocationResponseDto.java`
- Create: `backend/src/main/java/com/medhead/poc/infrastructure/adapter/in/web/BedAllocationController.java`
- Test: `backend/src/test/java/com/medhead/poc/infrastructure/adapter/in/web/BedAllocationControllerTest.java`

**Interfaces:**
- Consumes: `AllocateBedUseCase` (Task 5, câblé en bean par Task 8), `BedAllocationRequest`, `BedAllocationResult` (Task 2), `InvalidBedAllocationRequestException` (Task 5).
- Produces: endpoint `POST /api/bed-allocations` — terminal, aucune tâche suivante n'en dépend.

- [ ] **Step 1: Écrire le test qui échoue**

Créer `backend/src/test/java/com/medhead/poc/infrastructure/adapter/in/web/BedAllocationControllerTest.java` :

```java
package com.medhead.poc.infrastructure.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BedAllocationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void allocatesFredBrooksForCardiologyRequestNearFredBrooks() throws Exception {
        String requestBody = objectMapper.writeValueAsString(
                new BedAllocationRequestDto(48.8566, 2.3522, "Cardiologie"));

        mockMvc.perform(post("/api/bed-allocations")
                        .contentType(APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hospital.id", is("fred-brooks")))
                .andExpect(jsonPath("$.allocationStatus", is("CONFIRMED")))
                .andExpect(jsonPath("$.precision", is("estimee")));
    }

    @Test
    void rejectsUnknownSpecialtyWith400() throws Exception {
        String requestBody = objectMapper.writeValueAsString(
                new BedAllocationRequestDto(48.8566, 2.3522, "Astrologie"));

        mockMvc.perform(post("/api/bed-allocations")
                        .contentType(APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }
}
```

Ce test suppose l'existence de `BedAllocationRequestDto` — c'est normal en TDD, la compilation échouera avant même l'exécution.

- [ ] **Step 2: Lancer le test pour vérifier qu'il échoue**

Run (depuis `backend/`): `mvn -B test -Dtest=BedAllocationControllerTest`
Expected: FAIL — erreur de compilation, `BedAllocationRequestDto` n'existe pas.

- [ ] **Step 3: Créer les DTOs**

Créer `backend/src/main/java/com/medhead/poc/infrastructure/adapter/in/web/BedAllocationRequestDto.java` :

```java
package com.medhead.poc.infrastructure.adapter.in.web;

public record BedAllocationRequestDto(Double latitude, Double longitude, String specialty) {
}
```

Créer `backend/src/main/java/com/medhead/poc/infrastructure/adapter/in/web/BedAllocationResponseDto.java` :

```java
package com.medhead.poc.infrastructure.adapter.in.web;

public record BedAllocationResponseDto(HospitalDto hospital, String allocationStatus,
                                        String precision, double distanceKm) {

    public record HospitalDto(String id, String name) {
    }
}
```

- [ ] **Step 4: Créer le contrôleur**

Créer `backend/src/main/java/com/medhead/poc/infrastructure/adapter/in/web/BedAllocationController.java` :

```java
package com.medhead.poc.infrastructure.adapter.in.web;

import com.medhead.poc.domain.model.BedAllocationRequest;
import com.medhead.poc.domain.model.BedAllocationResult;
import com.medhead.poc.domain.service.AllocateBedUseCase;
import com.medhead.poc.domain.service.InvalidBedAllocationRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BedAllocationController {

    private final AllocateBedUseCase allocateBedUseCase;

    public BedAllocationController(AllocateBedUseCase allocateBedUseCase) {
        this.allocateBedUseCase = allocateBedUseCase;
    }

    @PostMapping("/api/bed-allocations")
    public BedAllocationResponseDto allocate(@RequestBody BedAllocationRequestDto requestDto) {
        if (requestDto.latitude() == null || requestDto.longitude() == null
                || requestDto.specialty() == null) {
            throw new InvalidBedAllocationRequestException(
                    "latitude, longitude and specialty are required");
        }
        BedAllocationResult result = allocateBedUseCase.allocate(new BedAllocationRequest(
                requestDto.latitude(), requestDto.longitude(), requestDto.specialty()));
        return toDto(result);
    }

    @ExceptionHandler(InvalidBedAllocationRequestException.class)
    public ResponseEntity<String> handleInvalidRequest(InvalidBedAllocationRequestException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
    }

    private BedAllocationResponseDto toDto(BedAllocationResult result) {
        return new BedAllocationResponseDto(
                new BedAllocationResponseDto.HospitalDto(result.hospital().id(), result.hospital().name()),
                result.allocationStatus().name(),
                result.precision(),
                result.distanceKm());
    }
}
```

- [ ] **Step 5: Lancer le test pour vérifier qu'il passe**

Run (depuis `backend/`): `mvn -B test -Dtest=BedAllocationControllerTest`
Expected: PASS — les 2 tests sont verts.

- [ ] **Step 6: Lancer la suite complète**

Run (depuis `backend/`): `mvn -B verify`
Expected: `BUILD SUCCESS`, tous les tests verts (7 existants + tous ceux ajoutés dans ce plan).

- [ ] **Step 7: Commit**

```bash
git add backend/src/main/java/com/medhead/poc/infrastructure/adapter/in/web/BedAllocationRequestDto.java backend/src/main/java/com/medhead/poc/infrastructure/adapter/in/web/BedAllocationResponseDto.java backend/src/main/java/com/medhead/poc/infrastructure/adapter/in/web/BedAllocationController.java backend/src/test/java/com/medhead/poc/infrastructure/adapter/in/web/BedAllocationControllerTest.java
git commit -m "feat: add POST /api/bed-allocations endpoint"
```

**Done criterion:** `mvn -B verify` passe entièrement ; le scénario du document d'exigences (Cardiologie près de Fred Brooks → Fred Brooks, `CONFIRMED`) est couvert par un test automatisé qui passe.

---

## Spec Coverage Check

- Modèle de domaine (`Hospital`, `NhsSpecialty`, `BedAllocationRequest`, `AllocationStatus`, `BedAllocationResult`) → Task 2, 3.
- Ports (`HospitalRepository`, `DistanceCalculator`) → Task 4.
- `AllocateBedUseCase` (algorithme 3 paliers, événement uniquement sur `CONFIRMED`) → Task 5.
- Adaptateurs (`InMemoryHospitalRepository`, `HaversineDistanceCalculator`) → Task 6, 7.
- Câblage Spring → Task 8.
- Contrôleur REST + validation 400 → Task 9.
- Contrainte "domain sans dépendance Spring" → Tasks 2, 3, 4, 5 (aucun import Spring dans les fichiers produits).
- `precision: "estimee"` sur toute réponse → Task 5 (`toResult`), vérifié en Task 9.
- Jamais de 404 sur les 3 paliers métier → Task 5 (toujours un `BedAllocationResult`), Task 9 (le contrôleur ne mappe que les erreurs de validation en 400).
- Hors scope (distance réelle, circuit breaker, cache, frontend, persistance) : non traité, conforme au design.
