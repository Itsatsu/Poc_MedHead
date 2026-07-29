package com.medhead.poc.domain.model;

import java.util.Objects;

public record Distance(double km, String precision) {

    public Distance {
        Objects.requireNonNull(precision, "precision must not be null");
    }
}
