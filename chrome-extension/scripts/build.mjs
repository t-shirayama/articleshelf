import { createWriteStream } from 'node:fs'
import { cp, mkdir, rm } from 'node:fs/promises'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import archiver from 'archiver'

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

async function createZipArchive(inputDir, outputFile, zipRootName) {
  await new Promise((resolve, reject) => {
    const output = createWriteStream(outputFile)
    const archive = archiver('zip', {
      zlib: { level: 9 }
    })
    const done = (error) => {
      if (error) {
        reject(error)
      } else {
        resolve()
      }
    }

    output.on('close', done)
    output.on('error', reject)
    archive.on('error', reject)
    archive.on('warning', (error) => {
      if (error.code === 'ENOENT') {
        console.warn(error.message)
      } else {
        reject(error)
      }
    })

    archive.pipe(output)
    archive.directory(inputDir, zipRootName)
    archive.finalize()
  })
}

console.log(`Packaging Chrome extension zip: ${zipPath}`)
await createZipArchive(unpackedDir, zipPath, 'articleshelf-chrome-extension')
await cp(zipPath, mirroredZipPath)
console.log(`Copied zip for local development: ${mirroredZipPath}`)

console.log(`Chrome extension build completed:
  dist: ${unpackedDir}
  zip:  ${zipPath}`)
