package com.medhead.poc.domain.port;

import com.medhead.poc.domain.model.Hospital;

import java.util.List;

/**
 * Port sortant (hexagonal) pour l'accès aux données des hôpitaux. Le domaine dépend
 * de cette abstraction, pas de l'implémentation (en mémoire pour ce POC, voir
 * {@code infrastructure.adapter.out.hospital}).
 */
public interface HospitalRepository {

    List<Hospital> findAll();
}
