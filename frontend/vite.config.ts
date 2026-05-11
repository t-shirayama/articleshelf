import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    host: '0.0.0.0',
    port: 5173
  },
  test: {
    environment: 'jsdom',
    globals: true,
    include: ['src/**/*.test.ts'],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'html', 'lcov'],
      reportsDirectory: 'coverage',
      include: ['src/**/*.{ts,vue}'],
      thresholds: {
        lines: 43,
        functions: 35,
        branches: 29,
        statements: 42
      },
      exclude: [
        'src/**/*.test.ts',
        'src/**/*.integration.test.ts',
        'src/main.ts',
        'src/vite-env.d.ts'
      ]
    }
  }
})
