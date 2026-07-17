# Design — Endpoint REST d'allocation de lit

Date : 2026-07-17
Sources : `info.md` (sections 1, 3, 4), document d'exigences (`ne pas git\Exigences pour le développement de la PoC.pdf`), `ne pas git\git\Principes de l'architecture.pdf` (Principe A4), `ne pas git\git\Données de référence sur les spécialités NHS.pdf`.

## Contexte

Deuxième story du backend de la PoC MedHead. Objectif : exposer l'API REST qui reçoit la localisation du patient et la spécialité recherchée, détermine l'hôpital le plus proche pertinent, et déclenche la publication de l'événement de réservation de lit (`EventPublisher` / `InMemoryEventPublisherAdapter`, livrés dans la story précédente).

Backend existant : Spring Boot 4.1.0, Java 25, package hexagonal `com.medhead.poc.domain` / `com.medhead.poc.infrastructure`.

## Décisions actées (brainstorming)

- **Stockage des hôpitaux** : liste en dur en mémoire (port `HospitalRepository` + adaptateur in-memory), pas de base de données pour cette PoC.
- **Calcul de distance** : à vol d'oiseau (Haversine) uniquement pour cette version — pas de service externe de routing (hors scope, prévu plus tard selon info.md section 3). Chaque réponse porte un indicateur `precision: "estimee"` pour ne jamais présenter une donnée dégradée comme fiable (Principe A4).
- **Unité exposée : kilomètres, pas un temps de trajet.** info.md distingue explicitement la distance réelle ("temps de trajet", cible future) du fallback à vol d'oiseau. Convertir la distance en un "temps estimé" via une vitesse moyenne arbitraire fabriquerait une donnée qui semble fiable alors qu'elle ne l'est pas — contraire au Principe A4. Le champ exposé est donc `distanceKm`, jamais une durée.
- **Format de la localisation** : latitude/longitude numériques. Aucun document n'impose de format ; le géocodage d'adresse est un sous-système hors scope. Pas de recommandation contraire des exigences (elles demandent seulement "une simple page ... pour saisir la localisation", sans préciser le format).
- **Validation de la spécialité** : contre la liste de référence NHS (`Données de référence sur les spécialités NHS.pdf`), qui l'impose explicitement : *"À utiliser pour les fonctionnalités qui permettent de sélectionner les hôpitaux par spécialité. Les fonctionnalités doivent être ciblées en fonction de ces données."* Pas de texte libre.
- **Gestion des cas sans correspondance parfaite** : jamais de réponse vide (Principe A4 — "d'abord ne pas nuire, ensuite soigner" implique de ne jamais laisser le patient sans proposition). Recherche en 3 paliers dégradés (voir Algorithme).
- **Route HTTP** : `POST /api/bed-allocations` — sémantique REST de création d'une proposition d'allocation, cohérente avec l'effet de bord (publication d'événement) qu'un `GET` ne devrait pas avoir.
- **Statuts HTTP** : `200` sur les 3 paliers métier (jamais `404` — un résultat métier dégradé n'est pas une erreur technique), `400` uniquement pour une requête invalide (spécialité inconnue de la liste NHS, coordonnées manquantes).

## Architecture

```
backend/src/main/java/com/medhead/poc/
  domain/
    model/
      Hospital.java                 (record, immuable)
      NhsSpecialty.java             (liste de référence, cf. section dédiée)
      BedAllocationRequest.java     (record)
      AllocationStatus.java         (enum)
      BedAllocationResult.java      (record)
    port/
      HospitalRepository.java       (interface)
      DistanceCalculator.java       (interface)
    service/
      AllocateBedUseCase.java       (logique métier, aucune dépendance Spring)
  infrastructure/
    adapter/out/hospital/
      InMemoryHospitalRepository.java   (@Component, 3 hôpitaux du scénario de référence)
    adapter/out/distance/
      HaversineDistanceCalculator.java  (@Component)
    adapter/in/web/
      BedAllocationController.java      (@RestController, POST /api/bed-allocations)
      BedAllocationRequestDto.java
      BedAllocationResponseDto.java
    config/
      UseCaseConfiguration.java         (@Configuration, @Bean AllocateBedUseCase)
```

Contrainte identique à la story précédente : `domain/` ne dépend d'aucune classe `org.springframework.*`. `AllocateBedUseCase` est un objet Java pur, instancié comme bean Spring via une factory `@Bean` dans `infrastructure/config`, pas via une annotation `@Service` directement sur la classe.

## Modèle de domaine

- `Hospital(String id, String name, Set<String> specialties, int availableBeds, double latitude, double longitude)`
- `BedAllocationRequest(double latitude, double longitude, String specialty)`
- `AllocationStatus` : enum `CONFIRMED`, `BED_NOT_CONFIRMED`, `SPECIALTY_NOT_AVAILABLE`
- `BedAllocationResult(Hospital hospital, AllocationStatus allocationStatus, String precision, double distanceKm)`

## Liste de référence NHS (`NhsSpecialty`)

Enum Java listant les valeurs de la colonne "Spécialité" du document de référence (le "Groupe de spécialité" n'est pas utilisé pour la validation, seule la spécialité précise l'est) :

```
Anesthésie, Soins intensifs, Oncologie clinique, Spécialités dentaires supplémentaires,
Radiologie dentaire et maxillo-faciale, Endodontie, Chirurgie buccale et maxillo-faciale,
Pathologie buccale et maxillo-faciale, Médecine buccale, Chirurgie buccale, Orthodontie,
Dentisterie pédiatrique, Parodontie, Prosthodontie, Dentisterie restauratrice,
Dentisterie de soins spéciaux, Médecine d'urgence, Médecine interne de soins aigus,
Allergie, Médecine audiovestibulaire, Cardiologie, Génétique clinique,
Neurophysiologie clinique, Pharmacologie clinique et thérapeutique, Dermatologie,
Endocrinologie et diabète sucré, Gastroentérologie, Médecine générale (interne),
Médecine générale, Médecine générale (GP) 6 mois, Médecine génito-urinaire,
Médecine gériatrique, Maladies infectieuses, Oncologie médicale, Ophtalmologie médicale,
Neurologie, Médecine du travail, Autre, Médecine palliative, Médecine de réadaptation,
Médecine rénale, Médecine respiratoire, Rhumatologie, Médecine du sport et de l'exercice,
Santé publique sexuelle et procréative, Cardiologie pédiatrique, Pédiatrie,
Pathologie chimique, Neuropathologie diagnostique, Histopathologie médico-légale,
Pathologie générale, Hématologie, Histopathologie, Immunologie, Microbiologie médicale,
Pathologie pédiatrique et périnatale, Virologie, Service de santé communautaire dentaire,
Service de santé communautaire médicale, Santé publique dentaire, Pratique de l'art dentaire,
Santé publique, Psychiatrie infantile et adolescente, Psychiatrie légale,
Psychiatrie générale, Psychiatrie de la vieillesse, Psychiatrie des troubles d'apprentissage,
Psychothérapie, Radiologie clinique, Médecine nucléaire, Chirurgie cardiothoracique,
Chirurgie générale, Neurochirurgie, Ophtalmologie, Otolaryngologie, Chirurgie pédiatrique,
Chirurgie plastique, Traumatologie et chirurgie orthopédique, Urologie, Chirurgie vasculaire
```

Le `specialty` d'une requête entrante doit correspondre exactement (comparaison insensible à la casse) à l'une de ces valeurs, sinon `400 Bad Request`.

## Données de test (hôpitaux fixtures)

Scénario de référence des exigences PoC, avec coordonnées fictives (région parisienne, pour permettre un calcul Haversine réaliste) :

| Hôpital | Lits | Spécialités (mappées sur la liste NHS) | Latitude | Longitude |
|---|---|---|---|---|
| Hôpital Fred Brooks | 2 | Cardiologie, Immunologie | 48.8566 | 2.3522 |
| Hôpital Julia Crusher | 0 | Cardiologie | 48.8606 | 2.3376 |
| Hôpital Beverly Bashir | 5 | Immunologie, Neuropathologie diagnostique | 48.8738 | 2.2950 |

Note de mapping : les exigences citent "immunologie, neuropathologie, diagnostic" pour Beverly Bashir (document d'exemple, illustratif). La liste NHS n'a pas d'entrée "Diagnostic" isolée ; la valeur NHS la plus proche est "Neuropathologie diagnostique", utilisée pour couvrir les deux termes de l'exemple.

## Algorithme métier (`AllocateBedUseCase.allocate(BedAllocationRequest)`)

1. Valider `specialty` contre `NhsSpecialty` → sinon exception de validation (→ 400 au niveau du contrôleur).
2. Charger tous les hôpitaux via `HospitalRepository.findAll()`.
3. **Palier 1** : parmi les hôpitaux ayant la spécialité ET `availableBeds > 0`, prendre celui avec la plus petite `DistanceCalculator.distanceKm(...)`. Si trouvé → `AllocationStatus.CONFIRMED`, publier un `BedReservationEvent` via `EventPublisher`, retourner le résultat.
4. **Palier 2** : sinon, parmi les hôpitaux ayant la spécialité (lits ou non), prendre le plus proche. Si trouvé → `AllocationStatus.BED_NOT_CONFIRMED`, pas d'événement publié.
5. **Palier 3** : sinon (aucun hôpital n'a la spécialité), prendre l'hôpital le plus proche parmi tous. → `AllocationStatus.SPECIALTY_NOT_AVAILABLE`, pas d'événement publié.
6. Chaque résultat porte `precision = "estimee"` et le `distanceKm` calculé.

(Le cas où `HospitalRepository.findAll()` est vide n'est pas géré explicitement : la fixture PoC contient toujours 3 hôpitaux, ce cas ne peut pas se produire dans le scope de cette story.)

## API

`POST /api/bed-allocations`

Requête :
```json
{ "latitude": 48.858, "longitude": 2.294, "specialty": "Cardiologie" }
```

Réponse `200` (structure identique sur les 3 paliers, seul `allocationStatus` change) :
```json
{
  "hospital": { "id": "fred-brooks", "name": "Hopital Fred Brooks" },
  "allocationStatus": "CONFIRMED",
  "precision": "estimee",
  "distanceKm": 3.2
}
```

Réponse `400` si `specialty` absent de la liste NHS, ou `latitude`/`longitude` absents ou hors intervalle valide (`latitude` ∈ [-90, 90], `longitude` ∈ [-180, 180]).

## Tests

- `AllocateBedUseCase` : un test par palier (CONFIRMED / BED_NOT_CONFIRMED / SPECIALTY_NOT_AVAILABLE), avec un `HospitalRepository` et un `EventPublisher` de test (implémentations en mémoire dédiées au test, pas de mock framework nécessaire vu la simplicité des interfaces). Vérifier explicitement que l'événement n'est publié que dans le cas CONFIRMED.
- `HaversineDistanceCalculator` : test unitaire avec des coordonnées connues (distance attendue calculable à la main ou via une référence externe).
- `NhsSpecialty` / validation : test qui vérifie qu'une spécialité hors liste est rejetée et qu'une spécialité valide passe (insensible à la casse).
- Test d'intégration `@SpringBootTest` + `MockMvc` sur `BedAllocationController` : golden path (scénario Fred Brooks du document d'exigences) et cas de requête invalide (400).

## Hors scope de cette story

- Calcul de distance réelle (routing externe), circuit breaker, cache des positions.
- Fallback multi-fournisseur.
- Frontend React.
- Persistance des hôpitaux en base de données.

## Critères de "done"

- `mvn -B verify` passe (build + tous les tests).
- Le scénario du document d'exigences (patient cardiologie près de Fred Brooks → Fred Brooks proposé, événement publié) est couvert par un test automatisé.
- Aucune classe de `domain/` n'importe `org.springframework.*`.
- `specialty` toujours validé contre `NhsSpecialty`, jamais accepté en texte libre.
- Aucune réponse vide/404 sur les 3 paliers métier — toujours une proposition d'hôpital.
