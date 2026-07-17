package com.medhead.poc.infrastructure.adapter.out.event;

import com.medhead.poc.domain.model.BedReservationEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryEventPublisherAdapterTest {

    @Test
    void publishAddsEventToPublishedEvents() {
        InMemoryEventPublisherAdapter adapter = new InMemoryEventPublisherAdapter();
        BedReservationEvent event = new BedReservationEvent(
                UUID.randomUUID(), "hospital-1", "cardiology", Instant.now());

        adapter.publish(event);

        assertThat(adapter.getPublishedEvents()).containsExactly(event);
    }

    @Test
    void publishPreservesOrderAcrossMultipleEvents() {
        InMemoryEventPublisherAdapter adapter = new InMemoryEventPublisherAdapter();
        BedReservationEvent first = new BedReservationEvent(
                UUID.randomUUID(), "hospital-1", "cardiology", Instant.now());
        BedReservationEvent second = new BedReservationEvent(
                UUID.randomUUID(), "hospital-2", "neurology", Instant.now());

        adapter.publish(first);
        adapter.publish(second);

        assertThat(adapter.getPublishedEvents()).containsExactly(first, second);
    }
}
