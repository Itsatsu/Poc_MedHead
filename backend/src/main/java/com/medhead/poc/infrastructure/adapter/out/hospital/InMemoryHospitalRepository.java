package com.medhead.poc.infrastructure.adapter.out.hospital;

import com.medhead.poc.domain.model.Hospital;
import com.medhead.poc.domain.port.HospitalRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * Adaptateur de sortie (hexagonal) implémentant {@link HospitalRepository} avec un jeu
 * de données statique en mémoire (placé sur rennes), en lieu et place d'une vraie base de données. Permet
 * de faire tourner le POC sans dépendance externe.
 */
@Component
public class InMemoryHospitalRepository implements HospitalRepository {

    // Jeu de données de démonstration fixe (POC) : à remplacer par une source de
    // données persistante dans une implémentation de production.
    private static final List<Hospital> HOSPITALS = List.of(
            new Hospital("fred-brooks", "Hopital Fred Brooks",
                    Set.of("Cardiologie", "Immunologie"), 2, 48.1054336, -1.7298),
            new Hospital("julia-crusher", "Hopital Julia Crusher",
                    Set.of("Cardiologie"), 0, 48.104576, -1.6863629),
            new Hospital("beverly-bashir", "Hopital Beverly Bashir",
                    Set.of("Immunologie", "Neuropathologie diagnostique"), 5, 48.104576, -1.6863629)
    );

    @Override
    public List<Hospital> findAll() {
        return HOSPITALS;
    }
}
