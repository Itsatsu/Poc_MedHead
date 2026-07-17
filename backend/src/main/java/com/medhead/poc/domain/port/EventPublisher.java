package com.medhead.poc.domain.port;

import com.medhead.poc.domain.model.BedReservationEvent;

public interface EventPublisher {

    void publish(BedReservationEvent event);
}
