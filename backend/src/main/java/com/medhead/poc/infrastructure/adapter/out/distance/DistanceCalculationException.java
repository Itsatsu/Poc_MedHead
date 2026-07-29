package com.medhead.poc.infrastructure.adapter.out.distance;

public class DistanceCalculationException extends RuntimeException {

    public DistanceCalculationException(String message, Throwable cause) {
        super(message, cause);
    }

    public DistanceCalculationException(String message) {
        super(message);
    }
}
