package com.medhead.poc.infrastructure.adapter.in.web;

/**
 * DTO du corps de la requête HTTP. Utilise des types wrapper (Double) plutôt que des
 * primitifs afin de pouvoir détecter l'absence d'un champ (null) et la distinguer
 * d'une valeur 0.0.
 */
public record BedAllocationRequestDto(Double latitude, Double longitude, String specialty) {
}
