package com.medhead.poc.domain.service;

public class InvalidBedAllocationRequestException extends RuntimeException {

    public InvalidBedAllocationRequestException(String message) {
        super(message);
    }
}
