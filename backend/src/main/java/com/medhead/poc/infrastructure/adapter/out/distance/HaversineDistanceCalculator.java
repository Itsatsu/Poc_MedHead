package com.medhead.poc.infrastructure.adapter.out.distance;

import com.medhead.poc.domain.model.Distance;
import com.medhead.poc.domain.port.DistanceCalculator;

/**
 * Adaptateur de sortie (hexagonal) calculant une distance "à vol d'oiseau" via la
 * formule de Haversine, sans dépendance externe. Sert de calculateur de secours
 * ({@link ResilientDistanceCalculator}) quand Google Maps est indisponible : moins
 * précis (ignore le réseau routier réel) mais toujours disponible et quasi instantané,
 * ce qui garantit l'exigence de latence même en cas de panne du service externe.
 */
public class HaversineDistanceCalculator implements DistanceCalculator {

    private static final double EARTH_RADIUS_KM = 6371.0;

    @Override
    public Distance distanceTo(double lat1, double lon1, double lat2, double lon2) {
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLat = Math.toRadians(lat2 - lat1);
        double deltaLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad)
                * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double c = 2 * Math.asin(Math.sqrt(a));

        return new Distance(EARTH_RADIUS_KM * c, "estimee");
    }
}
