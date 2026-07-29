export interface BedAllocationRequest {
  latitude: number;
  longitude: number;
  specialty: string;
}

export interface BedAllocationResult {
  hospital: { id: string; name: string };
  precision: string;
  distanceKm: number;
}

export class BedAllocationApiError extends Error {
  readonly status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

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
