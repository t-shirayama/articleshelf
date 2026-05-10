import { readdir, stat } from 'node:fs/promises'
const ASSETS_DIR = new URL('../dist/assets/', import.meta.url)
const LIMITS = {
  css: 600 * 1024,
  js: 1300 * 1024
}

const files = await readdir(ASSETS_DIR, { withFileTypes: true }).catch(() => {
  console.error('dist/assets is missing. Run npm run build before npm run check:bundle.')
  process.exit(1)
})

const assets = await Promise.all(
  files
    .filter((file) => file.isFile())
    .map(async (file) => {
      const fullPath = new URL(file.name, ASSETS_DIR)
      const info = await stat(fullPath)
      return { name: file.name, size: info.size }
    })
)

const failures = []

for (const [extension, limit] of Object.entries(LIMITS)) {
  const matchingAssets = assets
    .filter((asset) => asset.name.endsWith(`.${extension}`))
    .sort((left, right) => right.size - left.size)

  if (matchingAssets.length === 0) {
    continue
  }

  const largest = matchingAssets[0]
  console.log(`largest ${extension}: ${formatBytes(largest.size)} (${largest.name})`)

  if (largest.size > limit) {
    failures.push(`${largest.name} is ${formatBytes(largest.size)}, expected <= ${formatBytes(limit)}`)
  }
}

if (failures.length > 0) {
  console.error(`Bundle size check failed:\n${failures.map((failure) => `- ${failure}`).join('\n')}`)
  process.exit(1)
}

console.log('Bundle size check passed.')

function formatBytes(bytes) {
  return `${(bytes / 1024).toFixed(1)} KiB`
}
