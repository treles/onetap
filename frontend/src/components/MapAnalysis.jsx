import { useEffect, useMemo, useState } from 'react'
import { ArrowUpRight, Search, Shield, Swords } from 'lucide-react'
import { api, mapName, pct } from '../api.js'
import { hasEvidence } from './VerdictTag.jsx'

// Imagens em src/assets/maps/<mapa>.<ext>. Mapa sem arquivo mostra um aviso, nunca a imagem de outro mapa.
const FILES = import.meta.glob('../assets/maps/*.{jpg,jpeg,png,webp}', { eager: true, import: 'default' })
const IMAGES = Object.fromEntries(
  Object.entries(FILES).map(([path, url]) => [path.split('/').pop().replace(/\.[^.]+$/, '').toLowerCase(), url]),
)
const ACCENTS = {
  ascent: '#ff4655',
  split: '#34d7b0',
  haven: '#68a7c9',
  abyss: '#9a8ee8',
  lotus: '#d47c9c',
}

const SIDE = { attack: 'ataque', defense: 'defesa' }

/** O lado com o maior saldo negativo (time abaixo da referência), ou null se os dois estão acima. */
function weakestSide(m) {
  const sides = ['attack', 'defense'].filter((s) => m[s] && m[s].atStake > 0.05)
  if (sides.length === 0) return null
  return sides.sort((a, b) => m[b].atStake - m[a].atStake)[0]
}

function footer(m) {
  const withEvidence = ['attack', 'defense']
    .filter((s) => m[s] && hasEvidence(m[s].verdict))
    .sort((a, b) => Math.abs(m[b].atStake) - Math.abs(m[a].atStake))
  if (withEvidence.length) {
    const s = withEvidence[0]
    const worse = m[s].atStake > 0
    const word = m[s].verdict === 'STRONG' ? 'Evidência forte' : 'Indício'
    return [worse ? 'negative' : 'positive', `${worse ? '▼' : '▲'} ${word} ${s === 'attack' ? 'no ataque' : 'na defesa'}`]
  }
  const small = ['attack', 'defense'].every((s) => !m[s] || m[s].verdict === 'SMALL_SAMPLE')
  return ['', small ? 'Amostra pequena' : 'Sem evidência']
}

/** Ataque e defesa por mapa (situações "attack" e "defense" quebradas por mapa, direto da API). */
export function MapAnalysis({ team, benchmark, onOpenVod }) {
  const [maps, setMaps] = useState(null)
  const [error, setError] = useState(null)
  const [query, setQuery] = useState('')
  const [selected, setSelected] = useState(null)

  useEffect(() => {
    setMaps(null)
    setError(null)
    Promise.all([api.situation(team, 'attack', benchmark), api.situation(team, 'defense', benchmark)])
      .then(([atk, def]) => {
        const byMap = new Map()
        for (const i of atk.byMap) byMap.set(i.map, { map: i.map, attack: i })
        for (const i of def.byMap) byMap.set(i.map, { ...(byMap.get(i.map) ?? { map: i.map }), defense: i })
        setMaps(
          [...byMap.values()]
            .map((m) => {
              const rounds = (m.attack?.n ?? 0) + (m.defense?.n ?? 0)
              const won = (m.attack?.successes ?? 0) + (m.defense?.successes ?? 0)
              return { ...m, rounds, won, rate: rounds ? won / rounds : 0 }
            })
            .sort((x, y) => y.rounds - x.rounds),
        )
      })
      .catch((e) => setError(e.message))
  }, [team, benchmark])

  const visible = useMemo(
    () => (maps ?? []).filter((m) => mapName(m.map).toLowerCase().includes(query.trim().toLowerCase())),
    [maps, query],
  )
  const current = maps?.find((m) => m.map === selected)
  const weak = current && weakestSide(current)

  return (
    <>
      <div className="section-heading">
        <div>
          <span className="eyebrow">MAP POOL</span>
          <h2>Análise por mapa</h2>
          <p>Rounds vencidos no ataque e na defesa em cada mapa, contra a referência naquele mesmo mapa.</p>
        </div>
        <div className="search-box">
          <Search size={16} />
          <input aria-label="Buscar mapa" value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Buscar mapa" />
        </div>
      </div>

      {error && <p className="error-banner">{error}</p>}
      {!maps && !error && <p className="loading-note">Carregando os mapas…</p>}

      {current && (
        <div className="map-direction">
          <div>
            <span className="eyebrow">DIRECIONAMENTO DE TREINO · {mapName(current.map).toUpperCase()}</span>
            <p>
              {weak
                ? `Ponto mais fraco em ${mapName(current.map)}: ${SIDE[weak]}, ${pct(current[weak].teamRate)} contra ${pct(
                    current[weak].benchmarkRate,
                  )} da referência (${hasEvidence(current[weak].verdict) ? (current[weak].verdict === 'STRONG' ? 'evidência forte' : 'indício') : 'sem evidência ainda'}).`
                : `Em ${mapName(current.map)}, ataque e defesa estão iguais ou acima da referência.`}
            </p>
          </div>
          <button type="button" onClick={() => onOpenVod({ situationId: weak ?? 'attack', map: current.map })}>
            Rever rounds no VOD <ArrowUpRight size={15} />
          </button>
        </div>
      )}

      {maps && visible.length === 0 && <p className="empty-note">Nenhum mapa com esse nome nas partidas do time.</p>}

      <div className="map-grid">
        {visible.map((m) => {
          const src = IMAGES[m.map]
          const name = mapName(m.map)
          const accent = ACCENTS[m.map] ?? 'var(--accent)'
          const [tone, text] = footer(m)
          return (
            <button
              type="button"
              key={m.map}
              className={`map-card ${selected === m.map ? 'selected' : ''}`}
              onClick={() => setSelected(selected === m.map ? null : m.map)}
              aria-pressed={selected === m.map}
            >
              <div className={`map-art ${src ? '' : 'no-image'}`} style={src ? { backgroundImage: `url(${src})` } : undefined}>
                <span className="map-watermark">{name.slice(0, 1)}</span>
                <span className="map-badge">{m.rounds} ROUNDS</span>
                <span className="map-arrow">
                  <ArrowUpRight size={16} />
                </span>
                {!src && <span className="map-noimage">Imagem do mapa não disponível no projeto</span>}
              </div>
              <div className="map-body">
                <div className="map-title">
                  <span>{name}</span>
                  <strong>{pct(m.rate)}</strong>
                </div>
                <div className="map-bars">
                  <SideBar label="Ataque" Icon={Swords} item={m.attack} accent={accent} />
                  <SideBar label="Defesa" Icon={Shield} item={m.defense} accent={accent} />
                </div>
                <div className="map-footer">
                  <span className={tone}>{text}</span>
                  <span>
                    {m.won} de {m.rounds} rounds vencidos
                  </span>
                </div>
              </div>
            </button>
          )
        })}
      </div>
    </>
  )
}

function SideBar({ label, Icon, item, accent }) {
  return (
    <>
      <div>
        <span>
          <Icon size={12} /> {label}
        </span>
        <b>
          {item ? pct(item.teamRate) : '—'}
          {item && <small>vs {pct(item.benchmarkRate)}</small>}
        </b>
      </div>
      <div className="bar">
        <i style={{ width: `${item ? Math.round(item.teamRate * 100) : 0}%`, background: accent }} />
      </div>
    </>
  )
}
