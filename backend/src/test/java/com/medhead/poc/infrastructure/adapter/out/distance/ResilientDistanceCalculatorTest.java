package com.medhead.poc.infrastructure.adapter.out.distance;

import com.medhead.poc.domain.model.Distance;
import com.medhead.poc.domain.port.DistanceCalculator;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class ResilientDistanceCalculatorTest {

    @Test
    void returnsPrimaryResultWhenItSucceeds() {
        DistanceCalculator primary = (a, b, c, d) -> new Distance(1.0, "reelle");
        DistanceCalculator fallback = (a, b, c, d) -> new Distance(99.0, "estimee");
        ResilientDistanceCalculator calculator = new ResilientDistanceCalculator(
                primary, fallback, new CircuitBreaker(3, Duration.ofSeconds(30), Clock.systemUTC()));

        Distance distance = calculator.distanceTo(0, 0, 0, 0);

        assertThat(distance.km()).isEqualTo(1.0);
        assertThat(distance.precision()).isEqualTo("reelle");
    }

    @Test
    void fallsBackWhenPrimaryThrows() {
        DistanceCalculator primary = (a, b, c, d) -> {
            throw new DistanceCalculationException("boom");
        };
        DistanceCalculator fallback = (a, b, c, d) -> new Distance(99.0, "estimee");
        ResilientDistanceCalculator calculator = new ResilientDistanceCalculator(
                primary, fallback, new CircuitBreaker(3, Duration.ofSeconds(30), Clock.systemUTC()));

        Distance distance = calculator.distanceTo(0, 0, 0, 0);

        assertThat(distance.km()).isEqualTo(99.0);
        assertThat(distance.precision()).isEqualTo("estimee");
    }

    @Test
    void stopsCallingPrimaryOnceCircuitIsOpen() {
        java.util.concurrent.atomic.AtomicInteger primaryCalls = new java.util.concurrent.atomic.AtomicInteger();
        DistanceCalculator primary = (a, b, c, d) -> {
            primaryCalls.incrementAndGet();
            throw new DistanceCalculationException("boom");
        };
        DistanceCalculator fallback = (a, b, c, d) -> new Distance(99.0, "estimee");
        ResilientDistanceCalculator calculator = new ResilientDistanceCalculator(
                primary, fallback, new CircuitBreaker(2, Duration.ofSeconds(30), Clock.systemUTC()));

        calculator.distanceTo(0, 0, 0, 0);
        calculator.distanceTo(0, 0, 0, 0);
        calculator.distanceTo(0, 0, 0, 0);

        assertThat(primaryCalls.get()).isEqualTo(2);
    }
}
