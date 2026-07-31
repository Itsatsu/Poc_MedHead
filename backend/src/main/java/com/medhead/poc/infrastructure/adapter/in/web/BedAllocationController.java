package com.medhead.poc.infrastructure.adapter.in.web;

import com.medhead.poc.domain.model.BedAllocationRequest;
import com.medhead.poc.domain.model.BedAllocationResult;
import com.medhead.poc.domain.service.AllocateBedUseCase;
import com.medhead.poc.domain.service.InvalidBedAllocationRequestException;
import com.medhead.poc.domain.service.NoHospitalAvailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptateur d'entrée (hexagonal) exposant le cas d'usage {@link AllocateBedUseCase}
 * via une API REST. Traduit les DTO HTTP en modèles du domaine et les exceptions
 * métier en codes de statut HTTP.
 */
@RestController
public class BedAllocationController {

    private final AllocateBedUseCase allocateBedUseCase;

    public BedAllocationController(AllocateBedUseCase allocateBedUseCase) {
        this.allocateBedUseCase = allocateBedUseCase;
    }

    @PostMapping("/api/bed-allocations")
    public BedAllocationResponseDto allocate(@RequestBody BedAllocationRequestDto requestDto) {
        // Validation de présence des champs au niveau adaptateur (types wrapper nullables
        // dans le DTO) ; la validation métier (spécialité connue, coordonnées valides)
        // est déléguée au domaine dans AllocateBedUseCase.
        if (requestDto.latitude() == null || requestDto.longitude() == null
                || requestDto.specialty() == null) {
            throw new InvalidBedAllocationRequestException(
                    "latitude, longitude and specialty are required");
        }
        BedAllocationResult result = allocateBedUseCase.allocate(new BedAllocationRequest(
                requestDto.latitude(), requestDto.longitude(), requestDto.specialty()));
        return toDto(result);
    }

    @ExceptionHandler(InvalidBedAllocationRequestException.class)
    public ResponseEntity<String> handleInvalidRequest(InvalidBedAllocationRequestException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(exception.getMessage());
    }

    @ExceptionHandler(NoHospitalAvailableException.class)
    public ResponseEntity<String> handleNoHospitalAvailable(NoHospitalAvailableException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception.getMessage());
    }

    private BedAllocationResponseDto toDto(BedAllocationResult result) {
        return new BedAllocationResponseDto(
                new BedAllocationResponseDto.HospitalDto(result.hospital().id(), result.hospital().name()),
                result.precision(),
                result.distanceKm());
    }
}
