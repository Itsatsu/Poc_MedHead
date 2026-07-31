package com.medhead.poc.domain.service;

import com.medhead.poc.domain.model.BedAllocationRequest;
import com.medhead.poc.domain.model.BedAllocationResult;
import com.medhead.poc.domain.model.BedReservationEvent;
import com.medhead.poc.domain.model.Distance;
import com.medhead.poc.domain.model.Hospital;
import com.medhead.poc.domain.model.NhsSpecialty;
import com.medhead.poc.domain.port.DistanceCalculator;
import com.medhead.poc.domain.port.EventPublisher;
import com.medhead.poc.domain.port.HospitalRepository;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Cas d'usage central du domaine (couche "application" de l'architecture hexagonale) :
 * alloue le lit d'urgence le plus proche parmi les hôpitaux disposant à la fois de la
 * spécialité NHS demandée et d'un lit libre. Ne dépend que des ports du domaine
 * ({@link HospitalRepository}, {@link DistanceCalculator}, {@link EventPublisher}),
 * jamais des adaptateurs concrets.
 */
public class AllocateBedUseCase {

    private final HospitalRepository hospitalRepository;
    private final DistanceCalculator distanceCalculator;
    private final EventPublisher eventPublisher;

    public AllocateBedUseCase(HospitalRepository hospitalRepository,
                               DistanceCalculator distanceCalculator,
                               EventPublisher eventPublisher) {
        this.hospitalRepository = hospitalRepository;
        this.distanceCalculator = distanceCalculator;
        this.eventPublisher = eventPublisher;
    }

    public BedAllocationResult allocate(BedAllocationRequest request) {
        if (!NhsSpecialty.isValid(request.specialty())) {
            throw new InvalidBedAllocationRequestException(
                    "Unknown specialty: " + request.specialty());
        }
        if (request.latitude() < -90 || request.latitude() > 90
                || request.longitude() < -180 || request.longitude() > 180) {
            throw new InvalidBedAllocationRequestException("Invalid coordinates");
        }

        List<Hospital> hospitals = hospitalRepository.findAll();

        // Un hôpital n'est éligible que s'il propose la spécialité ET a au moins un lit
        // libre ; parmi les éligibles, on retient le plus proche (distance minimale).
        Hospital hospital = nearest(hospitals.stream()
                .filter(h -> hasSpecialty(h, request.specialty()))
                .filter(h -> h.availableBeds() > 0), request)
                .orElseThrow(() -> new NoHospitalAvailableException(
                        "No hospital with an available bed and specialty: " + request.specialty()));

        // La réservation du lit elle-même n'est pas modélisée ici (POC) : on se contente
        // de publier l'événement métier signalant l'allocation.
        eventPublisher.publish(new BedReservationEvent(
                UUID.randomUUID(), hospital.id(), request.specialty(), Instant.now()));
        return toResult(hospital, request);
    }

    private boolean hasSpecialty(Hospital hospital, String specialty) {
        // Comparaison insensible à la casse, comme pour NhsSpecialty.isValid.
        return hospital.specialties().stream().anyMatch(s -> s.equalsIgnoreCase(specialty));
    }

    private Optional<Hospital> nearest(Stream<Hospital> hospitals, BedAllocationRequest request) {
        // Attention : distanceTo() est appelé une fois par hôpital candidat pour la
        // comparaison, ce qui peut déclencher plusieurs appels au calculateur de distance
        // (potentiellement Google Maps) avant de retenir le plus proche.
        return hospitals.min(Comparator.comparingDouble(h -> distanceTo(h, request).km()));
    }

    private BedAllocationResult toResult(Hospital hospital, BedAllocationRequest request) {
        Distance distance = distanceTo(hospital, request);
        return new BedAllocationResult(hospital, distance.precision(), distance.km());
    }

    private Distance distanceTo(Hospital hospital, BedAllocationRequest request) {
        return distanceCalculator.distanceTo(
                request.latitude(), request.longitude(), hospital.latitude(), hospital.longitude());
    }
}
