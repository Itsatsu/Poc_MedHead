package com.medhead.poc.domain.service;

import com.medhead.poc.domain.model.AllocationStatus;
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

        Optional<Hospital> confirmed = nearest(hospitals.stream()
                .filter(h -> hasSpecialty(h, request.specialty()))
                .filter(h -> h.availableBeds() > 0), request);
        if (confirmed.isPresent()) {
            Hospital hospital = confirmed.get();
            eventPublisher.publish(new BedReservationEvent(
                    UUID.randomUUID(), hospital.id(), request.specialty(), Instant.now()));
            return toResult(hospital, AllocationStatus.CONFIRMED, request);
        }

        Optional<Hospital> specialtyOnly = nearest(hospitals.stream()
                .filter(h -> hasSpecialty(h, request.specialty())), request);
        if (specialtyOnly.isPresent()) {
            return toResult(specialtyOnly.get(), AllocationStatus.BED_NOT_CONFIRMED, request);
        }

        Hospital anyHospital = nearest(hospitals.stream(), request)
                .orElseThrow(() -> new IllegalStateException("No hospital available"));
        return toResult(anyHospital, AllocationStatus.SPECIALTY_NOT_AVAILABLE, request);
    }

    private boolean hasSpecialty(Hospital hospital, String specialty) {
        return hospital.specialties().stream().anyMatch(s -> s.equalsIgnoreCase(specialty));
    }

    private Optional<Hospital> nearest(Stream<Hospital> hospitals, BedAllocationRequest request) {
        return hospitals.min(Comparator.comparingDouble(h -> distanceCalculator.distanceKm(
                request.latitude(), request.longitude(), h.latitude(), h.longitude())));
    }

    private BedAllocationResult toResult(Hospital hospital, AllocationStatus status,
                                          BedAllocationRequest request) {
        double distanceKm = distanceCalculator.distanceKm(
                request.latitude(), request.longitude(), hospital.latitude(), hospital.longitude());
        return new BedAllocationResult(hospital, status, "estimee", distanceKm);
    }
}
