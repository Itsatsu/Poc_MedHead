package com.medhead.poc.infrastructure.adapter.in.web;

public record BedAllocationResponseDto(HospitalDto hospital, String precision, double distanceKm) {

    public record HospitalDto(String id, String name) {
    }
}
