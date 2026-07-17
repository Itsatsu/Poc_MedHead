package com.medhead.poc.domain.model;

import java.util.Objects;
import java.util.Set;

public record Hospital(String id, String name, Set<String> specialties, int availableBeds,
                        double latitude, double longitude) {

    public Hospital {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(specialties, "specialties must not be null");
        if (availableBeds < 0) {
            throw new IllegalArgumentException("availableBeds must not be negative");
        }
    }
}
