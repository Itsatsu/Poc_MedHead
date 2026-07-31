import { defineConfig, devices } from '@playwright/test';

const FRONTEND_PORT = 4173;
const BACKEND_PORT = process.env.BACKEND_PORT ?? '8080';

export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  retries: 0,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: `http://localhost:${FRONTEND_PORT}`,
    trace: 'on-first-retry',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
  webServer: [
    {
      command: 'npm run build && npm run preview -- --port 4173',
      url: `http://localhost:${FRONTEND_PORT}`,
      reuseExistingServer: !process.env.CI,
      timeout: 60_000,
      env: { VITE_API_BASE_URL: `http://localhost:${BACKEND_PORT}` },
    },
    {
      command: `mvn -B spring-boot:run -Dspring-boot.run.arguments=--server.port=${BACKEND_PORT}`,
      cwd: '../backend',
      // TCP check, not HTTP: the only endpoint is POST-only and would never
      // return 2xx on Playwright's readiness GET.
      port: Number(BACKEND_PORT),
      reuseExistingServer: !process.env.CI,
      timeout: 120_000,
      env: { MEDHEAD_CORS_ALLOWED_ORIGINS: `http://localhost:${FRONTEND_PORT}` },
    },
  ],
});
