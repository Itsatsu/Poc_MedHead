package com.medhead.poc.domain.model;

/**
 * Demande d'allocation d'un lit d'urgence : position du patient (latitude/longitude)
 * et spécialité NHS requise (voir {@link NhsSpecialty}).
 */
public record BedAllocationRequest(double latitude, double longitude, String specialty) {
}
