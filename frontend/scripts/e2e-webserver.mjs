import { spawn, spawnSync } from 'node:child_process'
import { setTimeout as delay } from 'node:timers/promises'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const projectName = process.env.E2E_COMPOSE_PROJECT_NAME ?? 'articleshelf-e2e'
const scriptDir = path.dirname(fileURLToPath(import.meta.url))
const rootDir = path.resolve(scriptDir, '..', '..')
const composeArgs = ['compose', '-p', projectName, '-f', 'docker-compose.e2e.yml']

function runDocker(args, options = {}) {
  return spawnSync('docker', [...composeArgs, ...args], {
    cwd: rootDir,
    encoding: 'utf8',
    ...options
  })
}

function requireDocker(args) {
  const result = runDocker(args, { stdio: 'inherit' })
  if (result.status !== 0) {
    process.exit(result.status ?? 1)
  }
}

function serviceContainerId(serviceName) {
  const result = runDocker(['ps', '-q', serviceName])
  return result.stdout.trim()
}

function inspect(containerId, format) {
  if (!containerId) return ''
  const result = spawnSync('docker', ['inspect', '-f', format, containerId], {
    cwd: rootDir,
    encoding: 'utf8'
  })
  return result.status === 0 ? result.stdout.trim() : ''
}

async function waitForServices() {
  for (let attempt = 1; attempt <= 90; attempt += 1) {
    const backendContainer = serviceContainerId('backend')
    const frontendContainer = serviceContainerId('frontend')
    const backendHealth = inspect(
      backendContainer,
      '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}'
    )
    const frontendStatus = inspect(frontendContainer, '{{.State.Status}}')

    if (backendHealth === 'healthy' && frontendStatus === 'running') {
      return
    }

    await delay(2000)
  }

  runDocker(['ps'], { stdio: 'inherit' })
  runDocker(['logs', '--tail', '200'], { stdio: 'inherit' })
  process.exit(1)
}

runDocker(['down', '-v', '--remove-orphans'], { stdio: 'ignore' })
requireDocker(['up', '--build', '-d'])
await waitForServices()

const logs = spawn('docker', [...composeArgs, 'logs', '-f'], {
  cwd: rootDir,
  stdio: 'inherit'
})

for (const signal of ['SIGINT', 'SIGTERM']) {
  process.on(signal, () => {
    logs.kill(signal)
    process.exit(0)
  })
}

logs.on('exit', (code) => {
  process.exit(code ?? 0)
})
