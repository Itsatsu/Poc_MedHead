package com.medhead.poc.infrastructure.config;

import com.medhead.poc.domain.port.DistanceCalculator;
import com.medhead.poc.domain.port.EventPublisher;
import com.medhead.poc.domain.port.HospitalRepository;
import com.medhead.poc.domain.service.AllocateBedUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfiguration {

    @Bean
    public AllocateBedUseCase allocateBedUseCase(HospitalRepository hospitalRepository,
                                                   DistanceCalculator distanceCalculator,
                                                   EventPublisher eventPublisher) {
        return new AllocateBedUseCase(hospitalRepository, distanceCalculator, eventPublisher);
    }
}
