package com.medhead.poc.domain.model;

import java.util.Objects;

/**
 * Distance calculée entre le patient et un hôpital. Le champ {@code precision}
 * indique si la valeur provient d'un service de calcul d'itinéraire réel
 * ("reelle") ou d'une estimation de secours à vol d'oiseau ("estimee").
 */
public record Distance(double km, String precision) {

    public Distance {
        Objects.requireNonNull(precision, "precision must not be null");
    }
}
