package com.medhead.poc.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BedReservationEventTest {

    @Test
    void storesAllFields() {
        UUID id = UUID.randomUUID();
        Instant reservedAt = Instant.parse("2026-07-17T10:00:00Z");

        BedReservationEvent event = new BedReservationEvent(id, "hospital-1", "cardiology", reservedAt);

        assertThat(event.id()).isEqualTo(id);
        assertThat(event.hospitalId()).isEqualTo("hospital-1");
        assertThat(event.specialty()).isEqualTo("cardiology");
        assertThat(event.reservedAt()).isEqualTo(reservedAt);
    }

    @Test
    void rejectsNullHospitalId() {
        assertThatThrownBy(() ->
                new BedReservationEvent(UUID.randomUUID(), null, "cardiology", Instant.now())
        ).isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsNullSpecialty() {
        assertThatThrownBy(() ->
                new BedReservationEvent(UUID.randomUUID(), "hospital-1", null, Instant.now())
        ).isInstanceOf(NullPointerException.class);
    }
}
