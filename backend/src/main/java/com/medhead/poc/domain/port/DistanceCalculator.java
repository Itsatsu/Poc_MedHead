package com.medhead.poc.domain.port;

public interface DistanceCalculator {

    double distanceKm(double latitude1, double longitude1, double latitude2, double longitude2);
}
