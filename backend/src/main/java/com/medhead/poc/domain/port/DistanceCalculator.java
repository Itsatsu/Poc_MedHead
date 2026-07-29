package com.medhead.poc.domain.port;

import com.medhead.poc.domain.model.Distance;

public interface DistanceCalculator {

    Distance distanceTo(double latitude1, double longitude1, double latitude2, double longitude2);
}
