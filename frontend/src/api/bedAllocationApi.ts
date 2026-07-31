/** Payload envoyé à l'API backend pour demander l'allocation d'un lit. */
export interface BedAllocationRequest {
  latitude: number;
  longitude: number;
  specialty: string;
}

/** Réponse de l'API : hôpital retenu, distance et précision du calcul (réelle ou estimée). */
export interface BedAllocationResult {
  hospital: { id: string; name: string };
  precision: string;
  distanceKm: number;
}

/** Erreur levée quand l'API répond avec un statut HTTP d'échec, porte le code pour permettre un affichage adapté. */
export class BedAllocationApiError extends Error {
  readonly status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

// URL de base configurable via VITE_API_BASE_URL (frontend/.env.local) pour cibler un backend
// tournant sur un autre port que 8080, par exemple si ce port est déjà occupé localement.
const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

/**
 * Appelle POST /api/bed-allocations et traduit les codes d'erreur HTTP connus
 * en messages compréhensibles pour l'utilisateur final.
 */
export async function allocateBed(request: BedAllocationRequest): Promise<BedAllocationResult> {
  const response = await fetch(`${BASE_URL}/api/bed-allocations`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  });

  if (response.status === 400) {
    throw new BedAllocationApiError(400, 'Spécialité ou coordonnées invalides.');
  }
  if (response.status === 404) {
    // Aucun hôpital disponible pour la spécialité demandée : ce n'est pas une erreur technique,
    // mais un résultat métier possible qu'il faut distinguer d'une vraie panne serveur.
    throw new BedAllocationApiError(
      404,
      "Aucun hôpital n'a la spécialité demandée avec un lit disponible pour le moment.",
    );
  }
  if (!response.ok) {
    throw new BedAllocationApiError(response.status, 'Erreur inattendue du serveur.');
  }

  return (await response.json()) as BedAllocationResult;
}
