// Gera um snapshot estático (JSON pré-calculado) a partir da API rodando, para publicar o protótipo
// sem servidor Java e sem a base bruta: só agregados e referências de rounds. Uso (com o backend rodando):
//   node scripts/gerar-snapshot.mjs [http://localhost:8080]
// Saída: frontend/public/snapshot/ (um arquivo por time)  ->  depois: cd frontend && npm run build:static
import { mkdir, rm, writeFile } from 'node:fs/promises'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const API = process.argv[2] ?? 'http://localhost:8080'
const OUT = join(dirname(fileURLToPath(import.meta.url)), '..', 'frontend', 'public', 'snapshot')
const REFS = ['league', 'top25']

async function get(path) {
  const res = await fetch(API + path)
  if (!res.ok) throw new Error(`${res.status} em ${path}`)
  return res.json()
}
async function post(path, body) {
  const res = await fetch(API + path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  if (!res.ok) throw new Error(`${res.status} em ${path}`)
  return res.json()
}

// Assistente: respostas das perguntas sugeridas e das perguntas seguintes que elas oferecem (até 2 níveis).
async function assistant(t, ref) {
  const suggestions = await get(`/api/teams/${t}/assistant/suggestions?benchmark=${ref}`)
  const answers = {}
  let queue = suggestions.map((s) => s.question)
  for (let depth = 0; depth < 2 && queue.length; depth++) {
    const next = []
    for (const question of queue) {
      if (answers[question]) continue
      answers[question] = await post(`/api/teams/${t}/assistant`, { question, benchmark: ref })
      next.push(...answers[question].followUps)
    }
    queue = next
  }
  return { suggestions, answers }
}

let files = 0
let bytes = 0
async function save(path, data) {
  const file = join(OUT, `${path}.json`)
  const body = JSON.stringify(data)
  await mkdir(dirname(file), { recursive: true })
  await writeFile(file, body)
  files++
  bytes += body.length
}

await rm(OUT, { recursive: true, force: true })
const teams = await get('/api/teams')
const methodology = await get('/api/methodology')
await save('health', await get('/api/health/data'))
await save('teams', teams)
await save('methodology', methodology)

for (const { team } of teams) {
  const t = encodeURIComponent(team)
  const bundle = { losses: await get(`/api/teams/${t}/losses`), report: {}, situations: {}, vod: {}, assistant: {} }
  for (const ref of REFS) {
    bundle.report[ref] = await get(`/api/teams/${t}/report?benchmark=${ref}`)
    bundle.assistant[ref] = await assistant(t, ref)
  }
  for (const s of methodology.situations) {
    try {
      const byRef = {}
      for (const ref of REFS) {
        byRef[ref] = await get(`/api/teams/${t}/situations/${s.id}?benchmark=${ref}`)
      }
      bundle.situations[s.id] = byRef
      bundle.vod[s.id] = await get(`/api/teams/${t}/situations/${s.id}/vod`)
    } catch {
      // situação que nunca ocorreu para o time
    }
  }
  await save(`teams/${team}`, bundle)
  process.stdout.write('.')
}
console.log(`\n${files} arquivos, ${(bytes / 1024 / 1024).toFixed(1)} MB em ${OUT}`)
