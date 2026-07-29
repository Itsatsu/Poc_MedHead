package com.medhead.poc.infrastructure.adapter.out.hospital;

import com.medhead.poc.domain.model.Hospital;
import com.medhead.poc.domain.port.HospitalRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class InMemoryHospitalRepository implements HospitalRepository {

    private static final List<Hospital> HOSPITALS = List.of(
            new Hospital("fred-brooks", "Hopital Fred Brooks",
                    Set.of("Cardiologie", "Immunologie"), 2, 48.8566, 2.3522),
            new Hospital("julia-crusher", "Hopital Julia Crusher",
                    Set.of("Cardiologie"), 0, 48.8606, 2.3376),
            new Hospital("beverly-bashir", "Hopital Beverly Bashir",
                    Set.of("Immunologie", "Neuropathologie diagnostique"), 5, 48.8738, 2.2950)
    );

    @Override
    public List<Hospital> findAll() {
        return HOSPITALS;
    }
}
