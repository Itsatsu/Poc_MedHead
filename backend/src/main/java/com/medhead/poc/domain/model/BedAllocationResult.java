package com.medhead.poc.domain.model;

/**
 * Résultat de l'allocation : hôpital retenu, distance jusqu'à celui-ci et précision
 * du calcul de distance ("reelle" via Google Maps ou "estimee" via l'estimation
 * à vol d'oiseau de secours — voir {@link Distance}).
 */
public record BedAllocationResult(Hospital hospital, String precision, double distanceKm) {
}
