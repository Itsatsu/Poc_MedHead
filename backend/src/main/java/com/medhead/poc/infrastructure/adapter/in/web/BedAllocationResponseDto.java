package com.medhead.poc.infrastructure.adapter.in.web;

/*
  DTO de réponse HTTP : n'expose que l'identifiant et le nom de l'hôpital (pas ses
  spécialités ni son nombre de lits) afin de limiter la surface d'information renvoyée
  au client.
 */
public record BedAllocationResponseDto(HospitalDto hospital, String precision, double distanceKm) {

    public record HospitalDto(String id, String name) {
    }
}
