package com.medhead.poc.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record BedReservationEvent(UUID id, String hospitalId, String specialty, Instant reservedAt) {

    public BedReservationEvent {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(hospitalId, "hospitalId must not be null");
        Objects.requireNonNull(specialty, "specialty must not be null");
        Objects.requireNonNull(reservedAt, "reservedAt must not be null");
    }
}
