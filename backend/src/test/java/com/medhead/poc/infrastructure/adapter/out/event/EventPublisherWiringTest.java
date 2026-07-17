package com.medhead.poc.infrastructure.adapter.out.event;

import com.medhead.poc.domain.port.EventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class EventPublisherWiringTest {

    @Autowired
    private EventPublisher eventPublisher;

    @Test
    void springWiresInMemoryAdapterAsEventPublisher() {
        assertThat(eventPublisher).isInstanceOf(InMemoryEventPublisherAdapter.class);
    }
}
