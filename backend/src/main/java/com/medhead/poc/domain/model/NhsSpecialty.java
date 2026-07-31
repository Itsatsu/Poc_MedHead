package com.medhead.poc.domain.model;

import java.util.Set;

/**
 * Référentiel des spécialités médicales reconnues par le NHS. Sert à valider
 * qu'une demande d'allocation porte sur une spécialité connue et à faire
 * correspondre les spécialités déclarées par les hôpitaux.
 */
public final class NhsSpecialty {

    // Liste fermée des libellés NHS valides ; toute spécialité absente de cet
    // ensemble est rejetée par AllocateBedUseCase avant même de chercher un hôpital.
    public static final Set<String> VALID_SPECIALTIES = Set.of(
            "Anesthésie", "Soins intensifs", "Oncologie clinique",
            "Spécialités dentaires supplémentaires", "Radiologie dentaire et maxillo-faciale",
            "Endodontie", "Chirurgie buccale et maxillo-faciale",
            "Pathologie buccale et maxillo-faciale", "Médecine buccale", "Chirurgie buccale",
            "Orthodontie", "Dentisterie pédiatrique", "Parodontie", "Prosthodontie",
            "Dentisterie restauratrice", "Dentisterie de soins spéciaux", "Médecine d'urgence",
            "Médecine interne de soins aigus", "Allergie", "Médecine audiovestibulaire",
            "Cardiologie", "Génétique clinique", "Neurophysiologie clinique",
            "Pharmacologie clinique et thérapeutique", "Dermatologie",
            "Endocrinologie et diabète sucré", "Gastroentérologie", "Médecine générale (interne)",
            "Médecine générale", "Médecine générale (GP) 6 mois", "Médecine génito-urinaire",
            "Médecine gériatrique", "Maladies infectieuses", "Oncologie médicale",
            "Ophtalmologie médicale", "Neurologie", "Médecine du travail", "Autre",
            "Médecine palliative", "Médecine de réadaptation", "Médecine rénale",
            "Médecine respiratoire", "Rhumatologie", "Médecine du sport et de l'exercice",
            "Santé publique sexuelle et procréative", "Cardiologie pédiatrique", "Pédiatrie",
            "Pathologie chimique", "Neuropathologie diagnostique", "Histopathologie médico-légale",
            "Pathologie générale", "Hématologie", "Histopathologie", "Immunologie",
            "Microbiologie médicale", "Pathologie pédiatrique et périnatale", "Virologie",
            "Service de santé communautaire dentaire", "Service de santé communautaire médicale",
            "Santé publique dentaire", "Pratique de l'art dentaire", "Santé publique",
            "Psychiatrie infantile et adolescente", "Psychiatrie légale", "Psychiatrie générale",
            "Psychiatrie de la vieillesse", "Psychiatrie des troubles d'apprentissage",
            "Psychothérapie", "Radiologie clinique", "Médecine nucléaire",
            "Chirurgie cardiothoracique", "Chirurgie générale", "Neurochirurgie",
            "Ophtalmologie", "Otolaryngologie", "Chirurgie pédiatrique", "Chirurgie plastique",
            "Traumatologie et chirurgie orthopédique", "Urologie", "Chirurgie vasculaire"
    );

    private NhsSpecialty() {
    }

    public static boolean isValid(String specialty) {
        if (specialty == null) {
            return false;
        }
        // Comparaison insensible à la casse : les clients externes ne sont pas
        // garantis de respecter exactement la casse du référentiel NHS.
        return VALID_SPECIALTIES.stream().anyMatch(valid -> valid.equalsIgnoreCase(specialty));
    }
}
