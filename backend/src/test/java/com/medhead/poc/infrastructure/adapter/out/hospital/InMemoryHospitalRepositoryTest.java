package com.medhead.poc.infrastructure.adapter.out.hospital;

import com.medhead.poc.domain.model.Hospital;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryHospitalRepositoryTest {

    @Test
    void returnsTheThreeReferenceHospitals() {
        InMemoryHospitalRepository repository = new InMemoryHospitalRepository();

        List<Hospital> hospitals = repository.findAll();

        assertThat(hospitals).hasSize(3);
        assertThat(hospitals).extracting(Hospital::id)
                .containsExactlyInAnyOrder("fred-brooks", "julia-crusher", "beverly-bashir");
        assertThat(hospitals).filteredOn(h -> h.id().equals("julia-crusher"))
                .first()
                .satisfies(h -> assertThat(h.availableBeds()).isZero());
    }
}
