package com.medhead.poc.domain.service;

/**
 * Levée lorsque la demande d'allocation est invalide (spécialité NHS inconnue ou
 * coordonnées géographiques hors bornes). Interceptée par
 * {@code BedAllocationController} et traduite en HTTP 400.
 */
public class InvalidBedAllocationRequestException extends RuntimeException {

    public InvalidBedAllocationRequestException(String message) {
        super(message);
    }
}
