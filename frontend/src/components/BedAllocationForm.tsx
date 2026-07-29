import { useState, type FormEvent } from 'react';
import { allocateBed, BedAllocationApiError, type BedAllocationResult } from '../api/bedAllocationApi';
import { NHS_SPECIALTIES } from '../domain/nhsSpecialties';

export function BedAllocationForm() {
  const [specialty, setSpecialty] = useState('');
  const [latitude, setLatitude] = useState('');
  const [longitude, setLongitude] = useState('');
  const [result, setResult] = useState<BedAllocationResult | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setResult(null);
    setError(null);

    const parsedLatitude = Number(latitude);
    const parsedLongitude = Number(longitude);
    if (specialty === '' || latitude === '' || longitude === ''
        || Number.isNaN(parsedLatitude) || Number.isNaN(parsedLongitude)) {
      setError('Veuillez renseigner une spécialité et des coordonnées valides.');
      return;
    }

    setLoading(true);
    try {
      const allocation = await allocateBed({
        latitude: parsedLatitude,
        longitude: parsedLongitude,
        specialty,
      });
      setResult(allocation);
    } catch (err) {
      setError(err instanceof BedAllocationApiError ? err.message : 'Erreur de connexion au serveur.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} data-testid="allocation-form">
      <h1>Allocation de lit d'urgence</h1>

      <div>
        <label htmlFor="specialty">Spécialité</label>
        <select
          id="specialty"
          value={specialty}
          onChange={(event) => setSpecialty(event.target.value)}
        >
          <option value="">-- Sélectionner --</option>
          {NHS_SPECIALTIES.map((s) => (
            <option key={s} value={s}>{s}</option>
          ))}
        </select>
      </div>

      <div>
        <label htmlFor="latitude">Latitude</label>
        <input
          id="latitude"
          type="number"
          step="any"
          value={latitude}
          onChange={(event) => setLatitude(event.target.value)}
        />
      </div>

      <div>
        <label htmlFor="longitude">Longitude</label>
        <input
          id="longitude"
          type="number"
          step="any"
          value={longitude}
          onChange={(event) => setLongitude(event.target.value)}
        />
      </div>

      <button type="submit" disabled={loading}>
        {loading ? 'Recherche…' : 'Trouver un hôpital'}
      </button>

      {error && <p role="alert">{error}</p>}

      {result && (
        <div role="status">
          <p>
            Hôpital proposé : <strong>{result.hospital.name}</strong>
          </p>
          <p>
            Distance : {result.distanceKm.toFixed(1)} km
            {' '}({result.precision === 'reelle' ? 'distance réelle' : 'estimation'})
          </p>
        </div>
      )}
    </form>
  );
}
