package com.medhead.poc.infrastructure.adapter.in.web;

import com.medhead.poc.domain.port.DistanceCalculator;
import com.medhead.poc.infrastructure.adapter.out.distance.HaversineDistanceCalculator;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BedAllocationControllerTest {

    @TestConfiguration
    static class NoExternalCallsConfiguration {

        @Bean
        @Primary
        DistanceCalculator testDistanceCalculator() {
            return new HaversineDistanceCalculator();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void allocatesFredBrooksForCardiologyRequestNearFredBrooks() throws Exception {
        String requestBody = objectMapper.writeValueAsString(
                new BedAllocationRequestDto(48.8566, 2.3522, "Cardiologie"));

        mockMvc.perform(post("/api/bed-allocations")
                        .contentType(APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hospital.id", is("fred-brooks")))
                .andExpect(jsonPath("$.precision", is("estimee")));
    }

    @Test
    void rejectsUnknownSpecialtyWith400() throws Exception {
        String requestBody = objectMapper.writeValueAsString(
                new BedAllocationRequestDto(48.8566, 2.3522, "Astrologie"));

        mockMvc.perform(post("/api/bed-allocations")
                        .contentType(APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returns404WhenNoHospitalHasSpecialtyWithAvailableBed() throws Exception {
        String requestBody = objectMapper.writeValueAsString(
                new BedAllocationRequestDto(48.8566, 2.3522, "Neurochirurgie"));

        mockMvc.perform(post("/api/bed-allocations")
                        .contentType(APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound());
    }
}
