package com.medhead.poc.infrastructure.adapter.out.distance;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

/*
  Disjoncteur simple à deux états (fermé/ouvert) protégeant l'appel à l'API Google Maps.
  Objectif : éviter de multiplier les appels lents/coûteux vers un service externe en
  panne et respecter l'exigence de latence (<200ms@800rps) en basculant rapidement
  sur l'estimation de secours pendant la fenêtre {@code openDuration}.

  <p>Fermé (isOpen=false) : les appels au calculateur primaire sont tentés normalement.
  Après {@code failureThreshold} échecs consécutifs, le circuit s'ouvre.
  Ouvert (isOpen=true) : le calculateur primaire n'est plus sollicité, on bascule
  directement sur le fallback jusqu'à expiration de {@code openDuration}, après quoi
  le circuit se referme automatiquement (pas d'état "semi-ouvert" distinct ici).
 */
public class CircuitBreaker {

    private final int failureThreshold;
    private final Duration openDuration;
    private final Clock clock;
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private volatile Instant openedAt;

    public CircuitBreaker(int failureThreshold, Duration openDuration, Clock clock) {
        this.failureThreshold = failureThreshold;
        this.openDuration = openDuration;
        this.clock = clock;
    }

    public boolean isOpen() {
        if (openedAt == null) {
            return false;
        }
        // La fenêtre d'ouverture est expirée : on referme le circuit et on redonne
        // sa chance au calculateur primaire au prochain appel.
        if (clock.instant().isAfter(openedAt.plus(openDuration))) {
            reset();
            return false;
        }
        return true;
    }

    public void recordSuccess() {
        reset();
    }

    public void recordFailure() {
        // Seules les défaillances CONSÉCUTIVES comptent : un succès réinitialise
        // le compteur (voir recordSuccess), une défaillance isolée n'ouvre donc pas le circuit.
        if (consecutiveFailures.incrementAndGet() >= failureThreshold) {
            openedAt = clock.instant();
        }
    }

    private void reset() {
        consecutiveFailures.set(0);
        openedAt = null;
    }
}
