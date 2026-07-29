package com.medhead.poc.domain.model;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HospitalTest {

    @Test
    void storesAllFields() {
        Hospital hospital = new Hospital("h1", "Hopital Test", Set.of("Cardiologie"), 3, 48.85, 2.35);

        assertThat(hospital.id()).isEqualTo("h1");
        assertThat(hospital.name()).isEqualTo("Hopital Test");
        assertThat(hospital.specialties()).containsExactly("Cardiologie");
        assertThat(hospital.availableBeds()).isEqualTo(3);
        assertThat(hospital.latitude()).isEqualTo(48.85);
        assertThat(hospital.longitude()).isEqualTo(2.35);
    }

    @Test
    void rejectsNegativeAvailableBeds() {
        assertThatThrownBy(() -> new Hospital("h1", "Hopital Test", Set.of("Cardiologie"), -1, 48.85, 2.35))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
