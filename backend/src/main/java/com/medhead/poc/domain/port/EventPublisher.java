package com.medhead.poc.domain.port;

import com.medhead.poc.domain.model.BedReservationEvent;

/**
 * Port sortant (hexagonal) pour la publication d'événements métier. Le domaine ignore
 * comment/où l'événement est propagé (file de messages, log, etc.) ; voir l'adaptateur
 * {@code infrastructure.adapter.out.event}.
 */
public interface EventPublisher {

    void publish(BedReservationEvent event);
}
