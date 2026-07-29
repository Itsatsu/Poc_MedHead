# Tests de charge — POST /api/bed-allocations

Répond à l'exigence PoC : *"nous obtenons un temps de réponse de moins de 200 millisecondes avec une charge de travail allant jusqu'à 800 requêtes par seconde, par instance de service"* et *"l'API doit être éprouvée avec des tests de stress pour garantir la continuité de l'activité en cas de pic d'utilisation"*.

## Pourquoi sans clé Google Maps

Le test de charge se lance **sans** `GOOGLE_MAPS_API_KEY` (ou avec une clé invalide). Le circuit breaker bascule alors immédiatement et durablement sur l'estimation Haversine (aucune dépendance externe, calcul en mémoire) — c'est la voie qui doit rester rapide sous charge. Le NFR porte sur *notre* service, pas sur la latence de l'API Google (hors de notre contrôle, et il serait coûteux/irréaliste d'envoyer 800 req/s vers un service tiers payant pour un test PoC). La résilience elle-même (bascule Google Maps → Haversine) est déjà couverte par les tests unitaires de `ResilientDistanceCalculatorTest` et `CircuitBreakerTest`.

## Prérequis

- [Apache JMeter](https://jmeter.apache.org/download_jmeter.cgi) 5.6+ (non installé sur cette machine au moment de la rédaction — à installer avant le premier lancement)
- L'application démarrée localement sur le port 8080, **sans** clé Google Maps valide :

```powershell
$env:JAVA_HOME = "C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.4\jbr"
$env:PATH = "C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.4\plugins\maven\lib\maven3\bin;$env:JAVA_HOME\bin;$env:PATH"
Remove-Item Env:\GOOGLE_MAPS_API_KEY -ErrorAction SilentlyContinue
cd backend
mvn spring-boot:run
```

## Lancer le test (headless, recommandé)

Depuis `backend/load-tests/` :

```bash
jmeter -n -t bed-allocation-load-test.jmx -l results.jtl -e -o report
```

- `-n` : mode non-graphique (obligatoire pour un test de charge réel — le mode GUI fausse les résultats)
- `-l results.jtl` : résultats bruts
- `-e -o report` : génère un rapport HTML dans `report/` à la fin du run

Le plan simule 800 req/s pendant 60 secondes (10 s de montée en charge + 60 s de palier), sur un panel de requêtes valides (`bed-allocation-requests.csv`) couvrant les trois hôpitaux du scénario de référence.

## Interpréter les résultats

Ouvrir `report/index.html`, ou lire l'Aggregate Report dans `results.jtl` :

- **Throughput** : doit approcher 800 req/s (le `Constant Throughput Timer` vise cette valeur)
- **95e percentile (p95)** du temps de réponse : c'est la mesure à comparer aux 200 ms de l'exigence — plus représentative qu'une moyenne, qu'une poignée de requêtes lentes ne devrait pas fausser
- **Taux d'erreur** : doit rester à 0 % (assertion "Status 200" sur chaque requête)

## Si le fichier .jmx ne s'ouvre pas correctement

Le fichier a été écrit à la main (pas généré depuis l'IHM JMeter) et vérifié uniquement pour un XML bien formé — s'il y a un souci d'ouverture avec votre version de JMeter, recréez le plan manuellement avec ces paramètres :

- **Thread Group** : 200 threads, ramp-up 10 s, durée 70 s (scheduler activé)
- **Constant Throughput Timer** : 48000 (= 800 × 60, exprimé en requêtes/minute), mode "calculer le débit en fonction de tous les threads actifs"
- **HTTP Request Defaults** : `localhost:8080`
- **CSV Data Set Config** : `bed-allocation-requests.csv`, variables `latitude,longitude,specialty`
- **HTTP Request** : `POST /api/bed-allocations`, header `Content-Type: application/json`, corps `{"latitude": ${latitude}, "longitude": ${longitude}, "specialty": "${specialty}"}`
- **Response Assertion** : code de réponse = 200
- **Aggregate Report** (listener) pour lire throughput / p95 / erreurs
