package com.medhead.poc.domain.service;

import com.medhead.poc.domain.model.AllocationStatus;
import com.medhead.poc.domain.model.BedAllocationRequest;
import com.medhead.poc.domain.model.BedAllocationResult;
import com.medhead.poc.domain.model.BedReservationEvent;
import com.medhead.poc.domain.model.Hospital;
import com.medhead.poc.domain.port.DistanceCalculator;
import com.medhead.poc.domain.port.EventPublisher;
import com.medhead.poc.domain.port.HospitalRepository;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AllocateBedUseCaseTest {

    private static final Hospital NEAR_HOSPITAL_WITH_BED = new Hospital(
            "near-with-bed", "Near With Bed", Set.of("Cardiologie"), 2, 48.85, 2.35);
    private static final Hospital FAR_HOSPITAL_WITH_BED = new Hospital(
            "far-with-bed", "Far With Bed", Set.of("Cardiologie"), 3, 48.90, 2.40);
    private static final Hospital NEAR_HOSPITAL_NO_BED = new Hospital(
            "near-no-bed", "Near No Bed", Set.of("Cardiologie"), 0, 48.86, 2.34);
    private static final Hospital HOSPITAL_OTHER_SPECIALTY = new Hospital(
            "other-specialty", "Other Specialty", Set.of("Urologie"), 5, 48.87, 2.30);

    @Test
    void confirmsNearestHospitalWithSpecialtyAndBed() {
        StubHospitalRepository repository = new StubHospitalRepository(
                List.of(FAR_HOSPITAL_WITH_BED, NEAR_HOSPITAL_WITH_BED));
        StubDistanceCalculator distances = new StubDistanceCalculator();
        distances.distanceTo(NEAR_HOSPITAL_WITH_BED, 1.0);
        distances.distanceTo(FAR_HOSPITAL_WITH_BED, 5.0);
        RecordingEventPublisher events = new RecordingEventPublisher();
        AllocateBedUseCase useCase = new AllocateBedUseCase(repository, distances, events);

        BedAllocationResult result = useCase.allocate(
                new BedAllocationRequest(48.85, 2.35, "Cardiologie"));

        assertThat(result.hospital()).isEqualTo(NEAR_HOSPITAL_WITH_BED);
        assertThat(result.allocationStatus()).isEqualTo(AllocationStatus.CONFIRMED);
        assertThat(result.precision()).isEqualTo("estimee");
        assertThat(result.distanceKm()).isEqualTo(1.0);
        assertThat(events.getPublishedEvents()).hasSize(1);
    }

    @Test
    void returnsBedNotConfirmedWhenSpecialtyHasNoAvailableBed() {
        StubHospitalRepository repository = new StubHospitalRepository(List.of(NEAR_HOSPITAL_NO_BED));
        StubDistanceCalculator distances = new StubDistanceCalculator();
        distances.distanceTo(NEAR_HOSPITAL_NO_BED, 2.0);
        RecordingEventPublisher events = new RecordingEventPublisher();
        AllocateBedUseCase useCase = new AllocateBedUseCase(repository, distances, events);

        BedAllocationResult result = useCase.allocate(
                new BedAllocationRequest(48.86, 2.34, "Cardiologie"));

        assertThat(result.hospital()).isEqualTo(NEAR_HOSPITAL_NO_BED);
        assertThat(result.allocationStatus()).isEqualTo(AllocationStatus.BED_NOT_CONFIRMED);
        assertThat(events.getPublishedEvents()).isEmpty();
    }

    @Test
    void returnsSpecialtyNotAvailableWhenNoHospitalHasSpecialty() {
        StubHospitalRepository repository = new StubHospitalRepository(List.of(HOSPITAL_OTHER_SPECIALTY));
        StubDistanceCalculator distances = new StubDistanceCalculator();
        distances.distanceTo(HOSPITAL_OTHER_SPECIALTY, 3.0);
        RecordingEventPublisher events = new RecordingEventPublisher();
        AllocateBedUseCase useCase = new AllocateBedUseCase(repository, distances, events);

        BedAllocationResult result = useCase.allocate(
                new BedAllocationRequest(48.87, 2.30, "Cardiologie"));

        assertThat(result.hospital()).isEqualTo(HOSPITAL_OTHER_SPECIALTY);
        assertThat(result.allocationStatus()).isEqualTo(AllocationStatus.SPECIALTY_NOT_AVAILABLE);
        assertThat(events.getPublishedEvents()).isEmpty();
    }

    @Test
    void rejectsUnknownSpecialty() {
        AllocateBedUseCase useCase = new AllocateBedUseCase(
                new StubHospitalRepository(List.of(NEAR_HOSPITAL_WITH_BED)),
                new StubDistanceCalculator(),
                new RecordingEventPublisher());

        assertThatThrownBy(() -> useCase.allocate(
                new BedAllocationRequest(48.85, 2.35, "Astrologie")))
                .isInstanceOf(InvalidBedAllocationRequestException.class);
    }

    private static final class StubHospitalRepository implements HospitalRepository {
        private final List<Hospital> hospitals;

        private StubHospitalRepository(List<Hospital> hospitals) {
            this.hospitals = hospitals;
        }

        @Override
        public List<Hospital> findAll() {
            return hospitals;
        }
    }

    private static final class StubDistanceCalculator implements DistanceCalculator {
        private final Map<String, Double> distances = new HashMap<>();

        private void distanceTo(Hospital hospital, double distanceKm) {
            distances.put(key(hospital.latitude(), hospital.longitude()), distanceKm);
        }

        @Override
        public double distanceKm(double lat1, double lon1, double lat2, double lon2) {
            return distances.getOrDefault(key(lat2, lon2), Double.MAX_VALUE);
        }

        private static String key(double lat, double lon) {
            return lat + ":" + lon;
        }
    }

    private static final class RecordingEventPublisher implements EventPublisher {
        private final List<BedReservationEvent> publishedEvents = new CopyOnWriteArrayList<>();

        @Override
        public void publish(BedReservationEvent event) {
            publishedEvents.add(event);
        }

        List<BedReservationEvent> getPublishedEvents() {
            return publishedEvents;
        }
    }
}
