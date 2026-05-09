import { defineConfig, devices } from '@playwright/test'

const useExistingServerOnly = process.env.PLAYWRIGHT_USE_EXISTING_SERVER === '1'

export default defineConfig({
  testDir: './e2e',
  timeout: 60_000,
  reporter: [
    ['line'],
    ['html', { open: 'never', outputFolder: 'playwright-report' }]
  ],
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
      testIgnore: /mobile-responsive\.spec\.ts/,
      use: { ...devices['Desktop Chrome'] }
    },
    {
      name: 'mobile',
      testMatch: /mobile-responsive\.spec\.ts/,
      use: { ...devices['Pixel 5'] }
    }
  ],
  webServer: useExistingServerOnly
    ? undefined
    : {
        command: 'sh ./frontend/scripts/e2e-webserver.sh',
        cwd: '..',
        url: 'http://localhost:5173',
        reuseExistingServer: !process.env.CI,
        timeout: 180_000
      }
})
