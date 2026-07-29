package com.medhead.poc.domain.service;

import com.medhead.poc.domain.model.BedAllocationRequest;
import com.medhead.poc.domain.model.BedAllocationResult;
import com.medhead.poc.domain.model.BedReservationEvent;
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

        Hospital hospital = nearest(hospitals.stream()
                .filter(h -> hasSpecialty(h, request.specialty()))
                .filter(h -> h.availableBeds() > 0), request)
                .orElseThrow(() -> new NoHospitalAvailableException(
                        "No hospital with an available bed and specialty: " + request.specialty()));

        eventPublisher.publish(new BedReservationEvent(
                UUID.randomUUID(), hospital.id(), request.specialty(), Instant.now()));
        return toResult(hospital, request);
    }

    private boolean hasSpecialty(Hospital hospital, String specialty) {
        return hospital.specialties().stream().anyMatch(s -> s.equalsIgnoreCase(specialty));
    }

    private Optional<Hospital> nearest(Stream<Hospital> hospitals, BedAllocationRequest request) {
        return hospitals.min(Comparator.comparingDouble(h -> distanceCalculator.distanceKm(
                request.latitude(), request.longitude(), h.latitude(), h.longitude())));
    }

    private BedAllocationResult toResult(Hospital hospital, BedAllocationRequest request) {
        double distanceKm = distanceCalculator.distanceKm(
                request.latitude(), request.longitude(), hospital.latitude(), hospital.longitude());
        return new BedAllocationResult(hospital, "estimee", distanceKm);
    }
}
