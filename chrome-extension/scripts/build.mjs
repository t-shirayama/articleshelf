import { createWriteStream } from 'node:fs'
import { cp, mkdir, readFile, rm, writeFile } from 'node:fs/promises'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import archiver from 'archiver'

const scriptDir = dirname(fileURLToPath(import.meta.url))
const rootDir = join(scriptDir, '..')
const srcDir = join(rootDir, 'src')
const distDir = join(rootDir, 'dist')
const frontendDownloadsDir = join(rootDir, '..', 'frontend', 'public', 'downloads')
const variants = [
  {
    name: 'production',
    appBaseUrl: 'https://articleshelf.pages.dev',
    extensionName: 'ArticleShelf Quick Add',
    actionTitle: 'Save to ArticleShelf',
    packageName: 'articleshelf-chrome-extension',
    mirrorToFrontend: false
  },
  {
    name: 'local',
    appBaseUrl: 'http://localhost:5173',
    extensionName: 'ArticleShelf Quick Add Local',
    actionTitle: 'Save to local ArticleShelf',
    packageName: 'articleshelf-chrome-extension-local',
    mirrorToFrontend: true
  }
]

await rm(distDir, { recursive: true, force: true })
await mkdir(distDir, { recursive: true })
await writeFile(join(distDir, '.gitkeep'), '')
await mkdir(frontendDownloadsDir, { recursive: true })

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

async function prepareVariant(variant) {
  const unpackedDir = join(distDir, variant.packageName)
  const zipPath = join(distDir, `${variant.packageName}.zip`)
  const manifestPath = join(unpackedDir, 'manifest.json')
  const popupPath = join(unpackedDir, 'popup.js')

  await cp(srcDir, unpackedDir, { recursive: true })

  const manifestSource = await readFile(manifestPath, 'utf8')
  await writeFile(
    manifestPath,
    manifestSource
      .replace('"name": "ArticleShelf Quick Add"', `"name": "${variant.extensionName}"`)
      .replace('"default_title": "Save to ArticleShelf"', `"default_title": "${variant.actionTitle}"`)
  )

  const popupSource = await readFile(popupPath, 'utf8')
  await writeFile(
    popupPath,
    popupSource.replace(
      "const APP_BASE_URL = 'https://articleshelf.pages.dev'",
      `const APP_BASE_URL = '${variant.appBaseUrl}'`
    )
  )

  console.log(`Packaging ${variant.name} Chrome extension zip: ${zipPath}`)
  await createZipArchive(unpackedDir, zipPath, variant.packageName)

  if (variant.mirrorToFrontend) {
    const mirroredZipPath = join(frontendDownloadsDir, `${variant.packageName}.zip`)
    await cp(zipPath, mirroredZipPath)
    console.log(`Copied ${variant.name} zip for local development: ${mirroredZipPath}`)
  }

  return { unpackedDir, zipPath }
}

const outputs = []
for (const variant of variants) {
  outputs.push(await prepareVariant(variant))
}

console.log(`Chrome extension build completed:
${outputs.map((output) => `  dist: ${output.unpackedDir}\n  zip:  ${output.zipPath}`).join('\n')}`)
