package com.medhead.poc.infrastructure.adapter.out.distance;

import com.medhead.poc.domain.model.Distance;
import com.medhead.poc.domain.port.DistanceCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResilientDistanceCalculator implements DistanceCalculator {

    private static final Logger log = LoggerFactory.getLogger(ResilientDistanceCalculator.class);

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
                log.warn("Primary distance calculator failed, falling back to estimated distance: {}",
                        e.getMessage(), e);
            }
        } else {
            log.warn("Circuit breaker is open, using estimated distance without calling the primary calculator");
        }
        return fallback.distanceTo(lat1, lon1, lat2, lon2);
    }
}
