// Cliente da API. Erros seguem o contrato { code, message, timestamp }.
// Em modo estático (snapshot gerado para deploy sem servidor), lê arquivos JSON pré-calculados.
const STATIC = import.meta.env.VITE_STATIC === 'true'

async function get(path) {
  const res = await fetch(path)
  if (!res.ok) {
    const body = await res.json().catch(() => ({}))
    throw new Error(body.message ?? `Erro ${res.status} ao carregar ${path}`)
  }
  return res.json()
}

const enc = encodeURIComponent
const snap = (path) => get(`${import.meta.env.BASE_URL}snapshot/${path}.json`)

// No modo estático cada time é um único arquivo; guardamos a promessa para baixar só uma vez.
const bundles = new Map()
function bundle(team) {
  if (!bundles.has(team)) bundles.set(team, snap(`teams/${team}`))
  return bundles.get(team)
}
function notObserved(team, id) {
  return new Error(`A situação '${id}' não ocorreu para ${team}`)
}

export const api = STATIC
  ? {
      health: () => snap('health'),
      teams: () => snap('teams'),
      methodology: () => snap('methodology'),
      report: async (team, ref) => (await bundle(team)).report[ref],
      situation: async (team, id, ref) => {
        const s = (await bundle(team)).situations[id]
        if (!s) throw notObserved(team, id)
        return s[ref]
      },
      vod: async (team, id, map) => {
        const all = (await bundle(team)).vod[id] ?? []
        return map ? all.filter((r) => r.map === map) : all
      },
      losses: async (team) => (await bundle(team)).losses,
    }
  : {
      health: () => get('/api/health/data'),
      teams: () => get('/api/teams'),
      methodology: () => get('/api/methodology'),
      report: (team, ref) => get(`/api/teams/${enc(team)}/report?benchmark=${ref}`),
      situation: (team, id, ref) => get(`/api/teams/${enc(team)}/situations/${enc(id)}?benchmark=${ref}`),
      vod: (team, id, map) =>
        get(`/api/teams/${enc(team)}/situations/${enc(id)}/vod${map ? `?map=${enc(map)}` : ''}`),
      losses: (team) => get(`/api/teams/${enc(team)}/losses`),
    }

export const pct = (x, digits = 0) => `${(x * 100).toFixed(digits).replace('.', ',')}%`
export const num = (x, digits = 0) => x.toFixed(digits).replace('.', ',')
export const mapName = (m) => (m ? m[0].toUpperCase() + m.slice(1) : m)
export const unitLabel = (u) => (u === 'plants' ? 'plants' : 'rounds')
