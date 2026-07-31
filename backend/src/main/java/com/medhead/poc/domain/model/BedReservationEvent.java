package com.medhead.poc.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Événement métier émis lorsqu'un lit est réservé suite à une allocation réussie.
 * Publié via le port {@link com.medhead.poc.domain.port.EventPublisher}.
 */
public record BedReservationEvent(UUID id, String hospitalId, String specialty, Instant reservedAt) {

    public BedReservationEvent {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(hospitalId, "hospitalId must not be null");
        Objects.requireNonNull(specialty, "specialty must not be null");
        Objects.requireNonNull(reservedAt, "reservedAt must not be null");
    }
}
