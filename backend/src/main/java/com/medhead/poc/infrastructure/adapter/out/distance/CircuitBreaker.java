package com.medhead.poc.infrastructure.adapter.out.distance;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

public class CircuitBreaker {

    private final int failureThreshold;
    private final Duration openDuration;
    private final Clock clock;
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private volatile Instant openedAt;

    public CircuitBreaker(int failureThreshold, Duration openDuration, Clock clock) {
        this.failureThreshold = failureThreshold;
        this.openDuration = openDuration;
        this.clock = clock;
    }

    public boolean isOpen() {
        if (openedAt == null) {
            return false;
        }
        if (clock.instant().isAfter(openedAt.plus(openDuration))) {
            reset();
            return false;
        }
        return true;
    }

    public void recordSuccess() {
        reset();
    }

    public void recordFailure() {
        if (consecutiveFailures.incrementAndGet() >= failureThreshold) {
            openedAt = clock.instant();
        }
    }

    private void reset() {
        consecutiveFailures.set(0);
        openedAt = null;
    }
}
