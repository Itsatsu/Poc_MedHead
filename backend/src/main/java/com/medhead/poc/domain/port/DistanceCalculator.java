package com.medhead.poc.domain.port;

import com.medhead.poc.domain.model.Distance;

/**
 * Port sortant (hexagonal) permettant au domaine de calculer une distance entre deux
 * points sans connaître l'implémentation concrète (API externe, estimation locale, etc.).
 * Voir les adaptateurs dans {@code infrastructure.adapter.out.distance}.
 */
public interface DistanceCalculator {

    Distance distanceTo(double latitude1, double longitude1, double latitude2, double longitude2);
}
