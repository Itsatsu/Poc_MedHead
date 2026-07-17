package com.medhead.poc.infrastructure.adapter.out.event;

import com.medhead.poc.domain.model.BedReservationEvent;
import com.medhead.poc.domain.port.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class InMemoryEventPublisherAdapter implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(InMemoryEventPublisherAdapter.class);

    private final List<BedReservationEvent> publishedEvents = new CopyOnWriteArrayList<>();

    @Override
    public void publish(BedReservationEvent event) {
        log.info("Publishing bed reservation event: {}", event);
        publishedEvents.add(event);
    }

    public List<BedReservationEvent> getPublishedEvents() {
        return Collections.unmodifiableList(publishedEvents);
    }
}
