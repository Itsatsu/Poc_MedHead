package com.medhead.poc.infrastructure.config;

import com.medhead.poc.domain.port.DistanceCalculator;
import com.medhead.poc.infrastructure.adapter.out.distance.CircuitBreaker;
import com.medhead.poc.infrastructure.adapter.out.distance.GoogleMapsDistanceCalculator;
import com.medhead.poc.infrastructure.adapter.out.distance.HaversineDistanceCalculator;
import com.medhead.poc.infrastructure.adapter.out.distance.ResilientDistanceCalculator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Duration;

@Configuration
public class DistanceCalculatorConfiguration {

    @Bean
    public DistanceCalculator distanceCalculator(
            @Value("${medhead.distance.google-maps.api-key}") String googleMapsApiKey,
            @Value("${medhead.distance.circuit-breaker.failure-threshold}") int failureThreshold,
            @Value("${medhead.distance.circuit-breaker.open-duration-seconds}") long openDurationSeconds) {
        GoogleMapsDistanceCalculator googleMaps = new GoogleMapsDistanceCalculator(
                RestClient.create(), googleMapsApiKey);
        HaversineDistanceCalculator haversine = new HaversineDistanceCalculator();
        CircuitBreaker circuitBreaker = new CircuitBreaker(
                failureThreshold, Duration.ofSeconds(openDurationSeconds), Clock.systemUTC());
        return new ResilientDistanceCalculator(googleMaps, haversine, circuitBreaker);
    }
}
