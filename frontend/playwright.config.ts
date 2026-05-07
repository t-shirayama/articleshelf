import { defineConfig, devices } from '@playwright/test'

export default defineConfig({
  testDir: './e2e',
  timeout: 60_000,
  expect: {
    timeout: 10_000
  },
  use: {
    baseURL: 'http://localhost:5173',
    trace: 'retain-on-failure'
  },
  projects: [
    {
      name: 'desktop',
      use: { ...devices['Desktop Chrome'] }
    }
  ],
  webServer: {
    command: 'docker compose up --build',
    cwd: '..',
    url: 'http://localhost:5173',
    reuseExistingServer: !process.env.CI,
    timeout: 120_000
  }
})
