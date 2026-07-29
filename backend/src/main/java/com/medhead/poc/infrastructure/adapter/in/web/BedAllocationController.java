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

@RestController
public class BedAllocationController {

    private final AllocateBedUseCase allocateBedUseCase;

    public BedAllocationController(AllocateBedUseCase allocateBedUseCase) {
        this.allocateBedUseCase = allocateBedUseCase;
    }

    @PostMapping("/api/bed-allocations")
    public BedAllocationResponseDto allocate(@RequestBody BedAllocationRequestDto requestDto) {
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
