package com.medhead.poc.infrastructure.adapter.out.distance;

import com.medhead.poc.domain.model.Distance;
import com.medhead.poc.domain.port.DistanceCalculator;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Adaptateur de sortie (hexagonal) implémentant {@link DistanceCalculator} via l'API
 * Google Maps Routes (distance routière réelle en voiture). Utilisé comme calculateur
 * primaire par {@link ResilientDistanceCalculator}, avec repli automatique en cas
 * d'échec (clé API absente, quota dépassé, service indisponible, etc.).
 */
public class GoogleMapsDistanceCalculator implements DistanceCalculator {

    private static final String COMPUTE_ROUTES_URI = "https://routes.googleapis.com/directions/v2:computeRoutes";

    private final RestClient restClient;
    private final String apiKey;

    public GoogleMapsDistanceCalculator(RestClient restClient, String apiKey) {
        this.restClient = restClient;
        this.apiKey = apiKey;
    }

    @Override
    public Distance distanceTo(double lat1, double lon1, double lat2, double lon2) {
        ComputeRoutesRequest request = new ComputeRoutesRequest(
                new Waypoint(new Location(new LatLng(lat1, lon1))),
                new Waypoint(new Location(new LatLng(lat2, lon2))),
                "DRIVE");

        ComputeRoutesResponse response;
        try {
            response = restClient.post()
                    .uri(COMPUTE_ROUTES_URI)
                    .header("X-Goog-Api-Key", apiKey)
                    .header("X-Goog-FieldMask", "routes.distanceMeters")
                    .body(request)
                    .retrieve()
                    .body(ComputeRoutesResponse.class);
        } catch (RuntimeException e) {
            throw new DistanceCalculationException("Google Maps Routes API call failed", e);
        }

        if (response == null || response.routes() == null || response.routes().isEmpty()) {
            throw new DistanceCalculationException("Google Maps Routes API returned no route");
        }

        // On ne retient que le premier itinéraire proposé (le plus pertinent selon
        // Google) ; distanceMeters est convertie en km pour homogénéité avec le domaine.
        double km = response.routes().get(0).distanceMeters() / 1000.0;
        return new Distance(km, "reelle");
    }

    private record ComputeRoutesRequest(Waypoint origin, Waypoint destination, String travelMode) {
    }

    private record Waypoint(Location location) {
    }

    private record Location(LatLng latLng) {
    }

    private record LatLng(double latitude, double longitude) {
    }

    private record ComputeRoutesResponse(List<Route> routes) {
    }

    private record Route(long distanceMeters) {
    }
}
