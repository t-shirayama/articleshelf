import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  test: {
    environment: 'jsdom',
    globals: true,
    include: [
      'src/features/articles/domain/articleFilters.test.ts',
      'src/features/articles/domain/calendar.test.ts',
      'src/features/articles/domain/renderMarkdown.test.ts',
      'src/features/articles/composables/useArticleActions.test.ts',
      'src/features/articles/composables/useArticleSearchDebounce.test.ts',
      'src/features/articles/composables/useWorkspaceAccountActions.test.ts',
      'src/features/auth/services/proactiveRefreshTimer.test.ts',
      'src/shared/api/client.test.ts',
      'src/shared/auth/jwt.test.ts'
    ],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'html', 'lcov'],
      reportsDirectory: 'coverage/focused',
      include: [
        'src/features/articles/domain/articleFilters.ts',
        'src/features/articles/domain/calendar.ts',
        'src/features/articles/domain/renderMarkdown.ts',
        'src/features/articles/composables/useArticleActions.ts',
        'src/features/articles/composables/useArticleSearchDebounce.ts',
        'src/features/articles/composables/useWorkspaceAccountActions.ts',
        'src/features/auth/services/proactiveRefreshTimer.ts',
        'src/shared/api/client.ts',
        'src/shared/auth/jwt.ts'
      ],
      exclude: ['src/**/*.test.ts', 'src/**/*.integration.test.ts'],
      thresholds: {
        lines: 85,
        statements: 85,
        functions: 90,
        branches: 70
      }
    }
  }
})
