import { cp, mkdir, rm } from 'node:fs/promises'
import { spawnSync } from 'node:child_process'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const scriptDir = dirname(fileURLToPath(import.meta.url))
const rootDir = join(scriptDir, '..')
const srcDir = join(rootDir, 'src')
const distDir = join(rootDir, 'dist')
const unpackedDir = join(distDir, 'articleshelf-chrome-extension')
const zipPath = join(distDir, 'articleshelf-chrome-extension.zip')
const frontendDownloadsDir = join(rootDir, '..', 'frontend', 'public', 'downloads')
const mirroredZipPath = join(frontendDownloadsDir, 'articleshelf-chrome-extension.zip')

await rm(unpackedDir, { recursive: true, force: true })
await rm(zipPath, { force: true })
await mkdir(unpackedDir, { recursive: true })
await mkdir(frontendDownloadsDir, { recursive: true })
await cp(srcDir, unpackedDir, { recursive: true })

if (process.platform === 'win32') {
  const command = `Compress-Archive -Path '${unpackedDir}\\*' -DestinationPath '${zipPath}' -Force`
  const result = spawnSync('powershell.exe', ['-NoProfile', '-Command', command], {
    stdio: 'inherit'
  })

  if (result.status !== 0) {
    throw new Error('Failed to package the Chrome extension zip archive.')
  }

  await cp(zipPath, mirroredZipPath)
} else {
  console.warn('Zip packaging is only scripted on Windows. The unpacked extension was generated in dist/.')
}
