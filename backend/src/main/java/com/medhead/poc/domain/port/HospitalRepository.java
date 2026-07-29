package com.medhead.poc.domain.port;

import com.medhead.poc.domain.model.Hospital;

import java.util.List;

public interface HospitalRepository {

    List<Hospital> findAll();
}
