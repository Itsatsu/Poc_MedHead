package com.medhead.poc.infrastructure.adapter.out.distance;

import com.medhead.poc.domain.model.Distance;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GoogleMapsDistanceCalculatorTest {

    private static final String COMPUTE_ROUTES_URI = "https://routes.googleapis.com/directions/v2:computeRoutes";

    @Test
    void returnsRealDistanceFromRoutesApiResponse() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(COMPUTE_ROUTES_URI))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header("X-Goog-Api-Key", "test-api-key"))
                .andRespond(withSuccess("{\"routes\":[{\"distanceMeters\":4200}]}", MediaType.APPLICATION_JSON));

        GoogleMapsDistanceCalculator calculator = new GoogleMapsDistanceCalculator(builder.build(), "test-api-key");

        Distance distance = calculator.distanceTo(48.85, 2.35, 48.90, 2.40);

        assertThat(distance.km()).isEqualTo(4.2);
        assertThat(distance.precision()).isEqualTo("reelle");
        server.verify();
    }

    @Test
    void throwsDistanceCalculationExceptionWhenApiCallFails() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(COMPUTE_ROUTES_URI))
                .andRespond(withServerError());

        GoogleMapsDistanceCalculator calculator = new GoogleMapsDistanceCalculator(builder.build(), "test-api-key");

        assertThatThrownBy(() -> calculator.distanceTo(48.85, 2.35, 48.90, 2.40))
                .isInstanceOf(DistanceCalculationException.class);
    }

    @Test
    void throwsDistanceCalculationExceptionWhenNoRouteFound() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(COMPUTE_ROUTES_URI))
                .andRespond(withSuccess("{\"routes\":[]}", MediaType.APPLICATION_JSON));

        GoogleMapsDistanceCalculator calculator = new GoogleMapsDistanceCalculator(builder.build(), "test-api-key");

        assertThatThrownBy(() -> calculator.distanceTo(48.85, 2.35, 48.90, 2.40))
                .isInstanceOf(DistanceCalculationException.class);
    }
}
