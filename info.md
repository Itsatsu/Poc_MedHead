Document de synthèse — PoC Allocation de lits d'urgence (MedHead)
1. Scope de la PoC
   Fonctionnel — dans le périmètre
   •	API REST : reçoit localisation (patient) + spécialité médicale recherchée
   •	Retourne : hôpital le plus proche disposant d'un lit et de la spécialité demandée
   •	Publie un événement de réservation de lit (asynchrone, découplé de la réponse HTTP synchrone)
   •	Front minimaliste : sélection de spécialité + saisie de localisation, consommant l'API
   Explicitement hors périmètre
   •	Identification/modélisation du patient (pas de nom, pas d'ID persistant)
   •	Tri par symptômes (hors-projet selon le Document de définition de l'architecture)
   •	Intégration réelle avec les systèmes tiers (Ursa Major, Jupiter, Schedule Shed) — factices/mockés
   •	Vrai bus d'événements (Kafka/RabbitMQ) — remplacé par un adaptateur mock (log + liste mémoire)
   •	Multi-fournisseur de géolocalisation en fallback — recommandation pour la production, non codée dans la PoC
   Stack imposée
   •	Backend : Java / Spring Boot, prêt pour intégration microservice
   •	Frontend : React
   •	Architecture métier : hexagonale (ports/adaptateurs), conforme au Principe B2 et à la couche « Services et capacités limités au domaine » du Document de définition de l'architecture

2. Conformité RGPD (démonstration par la conception)
   •	Minimisation des données (Art. 5) : aucune donnée patient non nécessaire au calcul n'est collectée. Pas de nom, pas d'identifiant patient persistant.
   •	Limitation de la conservation (Art. 5.1.e) : la localisation est traitée en mémoire pour le seul temps du calcul de distance, jamais persistée en base.

3. Architecture — points clés
   Publication d'événement (réservation de lit)
   •	Port PublicateurEvenement (interface), respectant le pattern ports/adaptateurs
   •	Un seul adaptateur pour la PoC : mock — enregistre l'événement dans un log + une liste en mémoire, exploitable dans les tests (assertThat(eventsPublies)...)
   •	Un vrai adaptateur (Kafka/RabbitMQ) n'est pas codé dans la PoC : cohérent avec le Principe C4 (isolation des données/systèmes de production) et les simplifications suggérées de la Déclaration des travaux d'architecture
   Calcul de distance réelle
   •	Point de vigilance : distance réelle (temps de trajet), jamais à vol d'oiseau en fonctionnement normal
   •	Port CalculateurDistance, adaptateur réel = appel à un service externe de géolocalisation/routing
   •	Résilience : circuit breaker autour de l'appel externe ; cache des positions des hôpitaux (données stables) pour limiter les appels externes
   •	Fallback en cas de panne/latence : distance à vol d'oiseau, avec indicateur explicite de dégradation dans la réponse (ex. champ precision: "estimee"), pour respecter le principe « d'abord ne pas nuire » (A4) en évitant qu'une donnée dégradée soit prise pour une donnée fiable
   •	Multi-fournisseur externe en cascade : recommandation documentée pour la production, non implémentée dans la PoC (hors scope/délai)

4. Critères de succès (KPI) et méthode de démonstration
#	Critère (source : Exigences PoC)	Démontrable seule ?	Méthode de démonstration
1	> 90 % des cas acheminés vers l'hôpital compétent le plus proche	Oui	Jeu de données de test contrôlé (scénario Fred Brooks / Julia Crusher / Beverly Bashir) + tests automatisés
2	Temps moyen de traitement d'une urgence : 18,25 min → 12 min	Non, pas seule	KPI métier global incluant des étapes hors périmètre (transport, prise en charge sur site...). La PoC ne contribue qu'à la portion décision/orientation — à mentionner et justifier, pas à prétendre démontrer intégralement.
3	< 200 ms à 800 req/s par instance	Oui	Tests de charge JMeter sur l'endpoint principal
4	Respect des normes imposées	Oui	Deux piliers : (a) RGPD — section 2 ; (b) architecture hexagonale/microservice-ready — section 3, démontrable via tests unitaires du domaine sans dépendance à Spring
5	Délai imparti respecté	Oui (engagement propre)	Planning fixé et suivi faute de deadline imposée par le sujet — voir section 5


5. Planning (engagement personnel, chef de projet)
   Aucune deadline n'étant imposée par le sujet, le délai a été fixé par l'étudiant en tant que chef de projet de la PoC.
   Total : ~33 jours ouvrés (~6,5 semaines)
   Étape	Durée	Justification
1. Setup (repos Git, env de dev)	3 j	—
2. Backend (API, tests, tests de charge)	10 j	—
3. Frontend (React)	10 j	Rallongé — stack moins maîtrisée
4. CI/CD	5 j	Rallongé — livrable directement évalué en soutenance
5. Documentation / reporting	5 j	Rallongé — livrable directement évalué en soutenance, doit justifier RGPD, architecture, résultats


6. En attente — non traité
   Mise en place des repos Git + template des services : à définir avant l'exécution de l'étape 1.
   •	Nombre de repos et rôle de chacun (rappel : 2 attendus — code / architecture)
   •	Workflow Git choisi (Gitflow / GitHub Flow / trunk-based) et justification
   •	Contenu du « template de service » (arborescence seulement, ou starter Spring Boot avec conventions)
