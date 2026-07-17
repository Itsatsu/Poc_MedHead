package com.medhead.poc.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NhsSpecialtyTest {

    @Test
    void acceptsKnownSpecialty() {
        assertThat(NhsSpecialty.isValid("Cardiologie")).isTrue();
    }

    @Test
    void isCaseInsensitive() {
        assertThat(NhsSpecialty.isValid("cardiologie")).isTrue();
    }

    @Test
    void rejectsUnknownSpecialty() {
        assertThat(NhsSpecialty.isValid("Astrologie")).isFalse();
    }

    @Test
    void rejectsNullSpecialty() {
        assertThat(NhsSpecialty.isValid(null)).isFalse();
    }
}
