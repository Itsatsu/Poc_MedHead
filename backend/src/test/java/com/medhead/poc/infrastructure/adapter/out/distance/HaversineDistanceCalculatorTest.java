package com.medhead.poc.infrastructure.adapter.out.distance;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class HaversineDistanceCalculatorTest {

    private final HaversineDistanceCalculator calculator = new HaversineDistanceCalculator();

    @Test
    void oneDegreeOfLatitudeIsApproximately111Km() {
        double distance = calculator.distanceKm(48.0, 2.0, 49.0, 2.0);

        assertThat(distance).isCloseTo(111.19, within(0.05));
    }

    @Test
    void distanceBetweenIdenticalPointsIsZero() {
        double distance = calculator.distanceKm(48.8566, 2.3522, 48.8566, 2.3522);

        assertThat(distance).isCloseTo(0.0, within(1e-9));
    }
}
