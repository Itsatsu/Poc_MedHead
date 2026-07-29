package com.medhead.poc.infrastructure.config;

import com.medhead.poc.domain.service.AllocateBedUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class UseCaseConfigurationTest {

    @Autowired
    private AllocateBedUseCase allocateBedUseCase;

    @Test
    void allocateBedUseCaseIsWired() {
        assertThat(allocateBedUseCase).isNotNull();
    }
}
