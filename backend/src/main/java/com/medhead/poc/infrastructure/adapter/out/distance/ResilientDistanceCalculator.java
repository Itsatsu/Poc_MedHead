package com.medhead.poc.infrastructure.adapter.out.distance;

import com.medhead.poc.domain.model.Distance;
import com.medhead.poc.domain.port.DistanceCalculator;

public class ResilientDistanceCalculator implements DistanceCalculator {

    private final DistanceCalculator primary;
    private final DistanceCalculator fallback;
    private final CircuitBreaker circuitBreaker;

    public ResilientDistanceCalculator(DistanceCalculator primary, DistanceCalculator fallback,
                                        CircuitBreaker circuitBreaker) {
        this.primary = primary;
        this.fallback = fallback;
        this.circuitBreaker = circuitBreaker;
    }

    @Override
    public Distance distanceTo(double lat1, double lon1, double lat2, double lon2) {
        if (!circuitBreaker.isOpen()) {
            try {
                Distance distance = primary.distanceTo(lat1, lon1, lat2, lon2);
                circuitBreaker.recordSuccess();
                return distance;
            } catch (RuntimeException e) {
                circuitBreaker.recordFailure();
            }
        }
        return fallback.distanceTo(lat1, lon1, lat2, lon2);
    }
}
