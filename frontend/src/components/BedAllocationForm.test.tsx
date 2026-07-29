import { describe, test, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { BedAllocationForm } from './BedAllocationForm';

describe('BedAllocationForm', () => {
  beforeEach(() => {
    vi.unstubAllGlobals();
  });

  test('affiche l\'hôpital proposé au succès', async () => {
    const user = userEvent.setup();
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => ({
        hospital: { id: 'fred-brooks', name: 'Hopital Fred Brooks' },
        precision: 'estimee',
        distanceKm: 3.2,
      }),
    }));

    render(<BedAllocationForm />);

    await user.selectOptions(screen.getByLabelText('Spécialité'), 'Cardiologie');
    await user.type(screen.getByLabelText('Latitude'), '48.8566');
    await user.type(screen.getByLabelText('Longitude'), '2.3522');
    await user.click(screen.getByRole('button', { name: /trouver un hôpital/i }));

    expect(await screen.findByText(/Hopital Fred Brooks/)).toBeInTheDocument();
    expect(screen.getByText(/3\.2 km/)).toBeInTheDocument();
    expect(screen.getByText(/estimation/)).toBeInTheDocument();
  });

  test('affiche un message clair quand aucun hôpital ne correspond (404)', async () => {
    const user = userEvent.setup();
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: false, status: 404 }));

    render(<BedAllocationForm />);

    await user.selectOptions(screen.getByLabelText('Spécialité'), 'Cardiologie');
    await user.type(screen.getByLabelText('Latitude'), '48.8566');
    await user.type(screen.getByLabelText('Longitude'), '2.3522');
    await user.click(screen.getByRole('button', { name: /trouver un hôpital/i }));

    expect(await screen.findByRole('alert')).toHaveTextContent(/aucun hôpital/i);
  });

  test('affiche un message clair pour une requête invalide (400)', async () => {
    const user = userEvent.setup();
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: false, status: 400 }));

    render(<BedAllocationForm />);

    await user.selectOptions(screen.getByLabelText('Spécialité'), 'Cardiologie');
    await user.type(screen.getByLabelText('Latitude'), '999');
    await user.type(screen.getByLabelText('Longitude'), '2.3522');
    await user.click(screen.getByRole('button', { name: /trouver un hôpital/i }));

    expect(await screen.findByRole('alert')).toHaveTextContent(/invalide/i);
  });

  test('bloque la soumission côté client si des champs sont manquants, sans appeler l\'API', () => {
    const fetchMock = vi.fn();
    vi.stubGlobal('fetch', fetchMock);

    render(<BedAllocationForm />);

    fireEvent.submit(screen.getByTestId('allocation-form'));

    expect(screen.getByRole('alert')).toHaveTextContent(/veuillez renseigner/i);
    expect(fetchMock).not.toHaveBeenCalled();
  });
});
