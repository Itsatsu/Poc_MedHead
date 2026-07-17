package com.medhead.poc.domain.model;

public record BedAllocationResult(Hospital hospital, AllocationStatus allocationStatus,
                                   String precision, double distanceKm) {
}
