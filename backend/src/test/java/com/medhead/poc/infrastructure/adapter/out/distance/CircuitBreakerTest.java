package com.medhead.poc.infrastructure.adapter.out.distance;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class CircuitBreakerTest {

    @Test
    void staysClosedBelowFailureThreshold() {
        CircuitBreaker breaker = new CircuitBreaker(3, Duration.ofSeconds(30), Clock.systemUTC());

        breaker.recordFailure();
        breaker.recordFailure();

        assertThat(breaker.isOpen()).isFalse();
    }

    @Test
    void opensAtFailureThreshold() {
        CircuitBreaker breaker = new CircuitBreaker(3, Duration.ofSeconds(30), Clock.systemUTC());

        breaker.recordFailure();
        breaker.recordFailure();
        breaker.recordFailure();

        assertThat(breaker.isOpen()).isTrue();
    }

    @Test
    void closesAgainAfterOpenDurationElapses() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        CircuitBreaker breaker = new CircuitBreaker(1, Duration.ofSeconds(10), clock);

        breaker.recordFailure();
        assertThat(breaker.isOpen()).isTrue();

        clock.advance(Duration.ofSeconds(11));

        assertThat(breaker.isOpen()).isFalse();
    }

    @Test
    void successResetsFailureCount() {
        CircuitBreaker breaker = new CircuitBreaker(3, Duration.ofSeconds(30), Clock.systemUTC());

        breaker.recordFailure();
        breaker.recordFailure();
        breaker.recordSuccess();
        breaker.recordFailure();
        breaker.recordFailure();

        assertThat(breaker.isOpen()).isFalse();
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
