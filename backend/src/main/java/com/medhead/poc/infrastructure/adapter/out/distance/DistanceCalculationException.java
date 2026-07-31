package com.medhead.poc.infrastructure.adapter.out.distance;

/**
 * Levée par {@link GoogleMapsDistanceCalculator} en cas d'échec d'appel à l'API externe
 * (réseau, réponse vide, etc.). Capturée par {@link ResilientDistanceCalculator}, qui
 * déclenche le disjoncteur et bascule sur le calcul de secours plutôt que de propager
 * l'erreur au domaine.
 */
public class DistanceCalculationException extends RuntimeException {

    public DistanceCalculationException(String message, Throwable cause) {
        super(message, cause);
    }

    public DistanceCalculationException(String message) {
        super(message);
    }
}
