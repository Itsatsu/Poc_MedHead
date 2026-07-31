import { test, expect } from '@playwright/test';

// Scenario de reference du document d'exigences PoC :
// patient Cardiologie pres de l'Hopital Fred Brooks -> Fred Brooks propose.
test('propose Fred Brooks pour une demande de cardiologie a proximite', async ({ page }) => {
  await page.goto('/');

  await page.getByLabel('Spécialité').selectOption('Cardiologie');
  await page.getByLabel('Latitude').fill('48.86');
  await page.getByLabel('Longitude').fill('2.30');
  await page.getByRole('button', { name: 'Trouver un hôpital' }).click();

  const result = page.getByRole('status');
  await expect(result).toContainText('Hopital Fred Brooks');
  await expect(result).toContainText('km');
});

test("affiche un message clair quand aucun hôpital n'a la spécialité", async ({ page }) => {
  await page.goto('/');

  await page.getByLabel('Spécialité').selectOption('Neurochirurgie');
  await page.getByLabel('Latitude').fill('48.86');
  await page.getByLabel('Longitude').fill('2.30');
  await page.getByRole('button', { name: 'Trouver un hôpital' }).click();

  await expect(page.getByRole('alert')).toContainText('Aucun hôpital');
});

test('rejette une soumission avec des coordonnées invalides sans planter la page', async ({ page }) => {
  await page.goto('/');

  await page.getByLabel('Spécialité').selectOption('Cardiologie');
  await page.getByLabel('Latitude').fill('999');
  await page.getByLabel('Longitude').fill('2.30');
  await page.getByRole('button', { name: 'Trouver un hôpital' }).click();

  await expect(page.getByRole('alert')).toContainText('invalide');
});
