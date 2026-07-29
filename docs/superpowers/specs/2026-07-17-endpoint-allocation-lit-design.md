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
- **Gestion des cas sans correspondance** : les exigences ne décrivent qu'un seul cas métier — l'hôpital le plus proche avec la spécialité **et** un lit disponible. Aucune recherche dégradée (proposer un hôpital sans lit ou sans la spécialité) n'est demandée ; en inventer une reviendrait à ajouter une fonctionnalité hors périmètre, potentiellement dangereuse (suggérer un hôpital incapable de traiter le patient). Une version antérieure de cette spec justifiait un repli en 3 paliers par le Principe A4 ("d'abord ne pas nuire") — relecture du texte du principe : il ne prescrit pas ce comportement précis, c'était une extrapolation. Quand aucun hôpital n'a la spécialité et un lit disponible, le cas est traité comme une absence de résultat (`NoHospitalAvailableException`), pas comme une proposition dégradée.
- **Route HTTP** : `POST /api/bed-allocations` — sémantique REST de création d'une proposition d'allocation, cohérente avec l'effet de bord (publication d'événement) qu'un `GET` ne devrait pas avoir.
- **Statuts HTTP** : `200` sur le cas confirmé, `400` pour une requête invalide (spécialité inconnue de la liste NHS, coordonnées manquantes/hors intervalle), `404` quand aucun hôpital n'a la spécialité avec un lit disponible.

## Architecture

```
backend/src/main/java/com/medhead/poc/
  domain/
    model/
      Hospital.java                 (record, immuable)
      NhsSpecialty.java             (liste de référence, cf. section dédiée)
      BedAllocationRequest.java     (record)
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
- `BedAllocationResult(Hospital hospital, String precision, double distanceKm)`
- `NoHospitalAvailableException` : levée quand aucun hôpital n'a la spécialité avec un lit disponible (→ 404 au niveau du contrôleur)

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

1. Valider `specialty` contre `NhsSpecialty` → sinon `InvalidBedAllocationRequestException` (→ 400 au niveau du contrôleur).
2. Charger tous les hôpitaux via `HospitalRepository.findAll()`.
3. Parmi les hôpitaux ayant la spécialité ET `availableBeds > 0`, prendre celui avec la plus petite `DistanceCalculator.distanceKm(...)`.
4. Si trouvé → publier un `BedReservationEvent` via `EventPublisher`, retourner le résultat (`precision = "estimee"`, `distanceKm` calculé).
5. Sinon → `NoHospitalAvailableException` (→ 404 au niveau du contrôleur), aucun événement publié.

(Le cas où `HospitalRepository.findAll()` est vide n'est pas géré explicitement : la fixture PoC contient toujours 3 hôpitaux, ce cas ne peut pas se produire dans le scope de cette story.)

## API

`POST /api/bed-allocations`

Requête :
```json
{ "latitude": 48.858, "longitude": 2.294, "specialty": "Cardiologie" }
```

Réponse `200` :
```json
{
  "hospital": { "id": "fred-brooks", "name": "Hopital Fred Brooks" },
  "precision": "estimee",
  "distanceKm": 3.2
}
```

Réponse `400` si `specialty` absent de la liste NHS, ou `latitude`/`longitude` absents ou hors intervalle valide (`latitude` ∈ [-90, 90], `longitude` ∈ [-180, 180]).

Réponse `404` si aucun hôpital n'a la spécialité demandée avec un lit disponible.

## Tests

- `AllocateBedUseCase` : cas confirmé (hôpital le plus proche avec spécialité + lit, événement publié), cas sans lit disponible (`NoHospitalAvailableException`), cas sans hôpital ayant la spécialité (`NoHospitalAvailableException`) — avec un `HospitalRepository` et un `EventPublisher` de test (implémentations en mémoire dédiées au test, pas de mock framework nécessaire vu la simplicité des interfaces). Vérifier explicitement qu'aucun événement n'est publié dans les cas d'échec.
- `HaversineDistanceCalculator` : test unitaire avec des coordonnées connues (distance attendue calculable à la main ou via une référence externe).
- `NhsSpecialty` / validation : test qui vérifie qu'une spécialité hors liste est rejetée et qu'une spécialité valide passe (insensible à la casse).
- Test d'intégration `@SpringBootTest` + `MockMvc` sur `BedAllocationController` : golden path (scénario Fred Brooks du document d'exigences), cas de requête invalide (400), cas sans hôpital disponible (404).

## Hors scope de cette story

- Calcul de distance réelle (routing externe), circuit breaker, cache des positions.
- Fallback multi-fournisseur.
- Recherche dégradée (proposer un hôpital sans lit ou sans la spécialité demandée) — non demandé par les exigences, écarté volontairement (voir "Décisions actées").
- Frontend React.
- Persistance des hôpitaux en base de données.

## Critères de "done"

- `mvn -B verify` passe (build + tous les tests).
- Le scénario du document d'exigences (patient cardiologie près de Fred Brooks → Fred Brooks proposé, événement publié) est couvert par un test automatisé.
- Aucune classe de `domain/` n'importe `org.springframework.*`.
- `specialty` toujours validé contre `NhsSpecialty`, jamais accepté en texte libre.
- Aucun hôpital sans la spécialité ou sans lit disponible n'est jamais proposé comme résultat.
