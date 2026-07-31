package com.medhead.poc.domain.service;

/**
 * Levée lorsqu'aucun hôpital ne dispose à la fois de la spécialité demandée et d'un lit
 * libre. Interceptée par {@code BedAllocationController} et traduite en HTTP 404.
 */
public class NoHospitalAvailableException extends RuntimeException {

    public NoHospitalAvailableException(String message) {
        super(message);
    }
}
