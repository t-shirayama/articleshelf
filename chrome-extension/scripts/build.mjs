import { createWriteStream } from 'node:fs'
import { copyFile, cp, mkdir, readFile, rm, writeFile } from 'node:fs/promises'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import archiver from 'archiver'

const scriptDir = dirname(fileURLToPath(import.meta.url))
const rootDir = join(scriptDir, '..')
const srcDir = join(rootDir, 'src')
const distDir = join(rootDir, 'dist')
const frontendDownloadsDir = join(rootDir, '..', 'frontend', 'public', 'downloads')
const productionExtensionKey =
  'MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAsYnnEXb3ic6SFRGCv9L+hk3EPJAJ6QfBVFlbsCmdeJADdi/FLMXQOLcgTyXGsa2Ic9E2fsl5bHymYzoqeKhjeXfJSJtXynjKZORz//bHdb0MbcEQg7Fmd9fqOjvkqaE/it7EB5wquUQcqNVJ3GAmtFtuGlnyOw/+0089Op01KHpsoNxh9FCI885epPubGVwx8BX4IkKEHdHA1TUkDAnUc7C1oyZL84pao8tVwrNUFBeMXhTwlniL+mrhA7chhMkxiUY7S+RcCQxcm3YdWg4unanJGbwgirA3M24268y8Z9dqjnXLMr8ZOYWfuY5H2A7QXYIZ0o40u3NTn1QkCKXj4QIDAQAB'
const localExtensionKey =
  'MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA2taV9A8GdAbe84qfvU1zgMNpjAXzbYe6igjSkCElEfe61+PnPkx5wSGggwYIeCitNHKqp6zXr+9BD2PPUOn88XLMVPNbLcEQ+JksgSMz359fiFlJSmLdOhtZ1SikBdRV9Iing8f6HF4Sq2h6ZaeexPtJ/1DpIEWuTIdiMQnhytpv4GW+v8Ms4ksFS4Cn5Q8B0ltC/nJt+rZ5SqBTHNmvnvyjupHkjibtp3ItFU0I2kGJTGMKxpCAGBNUCQpMBHBA/Lz1uzdAuVJUYnkl7HcghYWWiS/FSXGm3AD/lRxdrimZ5tW+fp0/xoWCwfZVXVJlt7pDTIvEb3GlbY9NQiKMowIDAQAB'
const variants = [
  {
    name: 'production',
    appBaseUrl: 'https://articleshelf.pages.dev',
    apiBaseUrl: 'https://articleshelf-api.onrender.com',
    clientId: 'articleshelf-chrome-extension',
    extensionId: 'bpkppkfmcfdpfbododebdbaaoodglnde',
    redirectUri: 'https://bpkppkfmcfdpfbododebdbaaoodglnde.chromiumapp.org/',
    extensionKey: productionExtensionKey,
    hostPermissions: ['https://articleshelf-api.onrender.com/*'],
    extensionName: '__MSG_extensionName__',
    actionTitle: '__MSG_actionTitle__',
    packageName: 'articleshelf-chrome-extension',
    mirrorToFrontend: false
  },
  {
    name: 'local',
    appBaseUrl: 'http://localhost:5173',
    apiBaseUrl: 'http://localhost:8080',
    clientId: 'articleshelf-chrome-extension-local',
    extensionId: 'ncdpeooneagfjhgnhenhakjnfflmpdbj',
    redirectUri: 'https://ncdpeooneagfjhgnhenhakjnfflmpdbj.chromiumapp.org/',
    extensionKey: localExtensionKey,
    hostPermissions: ['http://localhost:8080/*'],
    extensionName: '__MSG_extensionNameLocal__',
    actionTitle: '__MSG_actionTitleLocal__',
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

  const manifest = JSON.parse(await readFile(manifestPath, 'utf8'))
  manifest.name = variant.extensionName
  manifest.key = variant.extensionKey
  manifest.host_permissions = variant.hostPermissions
  manifest.action.default_title = variant.actionTitle
  await writeFile(manifestPath, `${JSON.stringify(manifest, null, 2)}\n`)

  const popupSource = await readFile(popupPath, 'utf8')
  await writeFile(
    popupPath,
    popupSource
      .replace("const APP_BASE_URL = 'https://articleshelf.pages.dev'", `const APP_BASE_URL = '${variant.appBaseUrl}'`)
      .replace("const API_BASE_URL = 'https://articleshelf-api.onrender.com'", `const API_BASE_URL = '${variant.apiBaseUrl}'`)
      .replace("const CLIENT_ID = 'articleshelf-chrome-extension'", `const CLIENT_ID = '${variant.clientId}'`)
      .replace("const EXTENSION_ID = 'bpkppkfmcfdpfbododebdbaaoodglnde'", `const EXTENSION_ID = '${variant.extensionId}'`)
      .replace("const REDIRECT_URI = 'https://bpkppkfmcfdpfbododebdbaaoodglnde.chromiumapp.org/'", `const REDIRECT_URI = '${variant.redirectUri}'`)
  )

  console.log(`Packaging ${variant.name} Chrome extension zip: ${zipPath}`)
  await createZipArchive(unpackedDir, zipPath, variant.packageName)

  if (variant.mirrorToFrontend) {
    const mirroredZipPath = join(frontendDownloadsDir, `${variant.packageName}.zip`)
    await rm(mirroredZipPath, { force: true })
    await copyFile(zipPath, mirroredZipPath)
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
