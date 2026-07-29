package com.medhead.poc.domain.service;

public class NoHospitalAvailableException extends RuntimeException {

    public NoHospitalAvailableException(String message) {
        super(message);
    }
}
