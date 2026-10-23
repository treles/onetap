import { useEffect, useState } from 'react'
import { ArrowUpRight, ChevronDown } from 'lucide-react'
import { api, mapName, pct } from '../api.js'
import vandal from '../assets/vandal-render.webp'

const FEATURED = 'octacore_python'
const QUICK = ['pucc_cardinals', 'ufu_saints', 'azure_bears_black']

/** Nome da situação sem o sufixo " em <Mapa>". */
export const situationOnly = (item) => (item.map ? item.label.replace(` em ${mapName(item.map)}`, '') : item.label)
const lowerFirst = (s) => s[0].toLowerCase() + s.slice(1)

/** Tela inicial: escolha do time, um exemplo real da análise e três times para começar. */
export function Home({ teams, onPick }) {
  const [previews, setPreviews] = useState({})
  useEffect(() => {
    for (const id of [FEATURED, ...QUICK]) {
      api.report(id, 'league').then((r) => setPreviews((p) => ({ ...p, [id]: r }))).catch(() => {})
    }
  }, [])

  const featured = previews[FEATURED]
  const item = featured?.attention[0]
  const evidence = item?.verdict === 'STRONG' ? 'evidência forte' : 'indício'

  return (
    <section className="home-screen">
      <div className="home-hero">
        <div className="eyebrow">
          <span className="live-dot" /> LIGA COACH · CENTRAL DE ANÁLISE
        </div>
        <h1>
          Do coach
          <br />
          ao <span className="title-highlight">clutch</span>
        </h1>
        <p>
          Cada round conta. Escolha um time do Univavá ou da Rush Series e descubra o que treinar primeiro, com quanta
          certeza e exatamente quais rounds rever.
        </p>
        <label className="home-select">
          <span>ESCOLHA UM TIME PARA COMEÇAR</span>
          <select aria-label="Escolha um time" value="" onChange={(e) => onPick(e.target.value)}>
            <option value="" disabled>
              {teams.length ? `Escolha um dos ${teams.length} times` : 'Carregando os times…'}
            </option>
            {teams.map((t) => (
              <option key={t.team} value={t.team}>
                {t.displayName} ({t.matches} {t.matches === 1 ? 'mapa' : 'mapas'}, {pct(t.roundWinRate)} dos rounds)
              </option>
            ))}
          </select>
          <ChevronDown size={16} />
        </label>
        <div className="home-hero-art" aria-hidden="true">
          <div className="hero-art-glow" />
          <img src={vandal} alt="" />
          <span>PRECISÃO. IMPACTO. EVOLUÇÃO.</span>
        </div>
      </div>

      {item && (
        <button
          type="button"
          className="home-preview"
          onClick={() => onPick(FEATURED)}
          aria-label={`Abrir análise de ${featured.displayName}`}
        >
          <div className="section-heading">
            <div>
              <span className="eyebrow">EXEMPLO DA ANÁLISE</span>
              <h2>Um ponto de atenção, com evidência</h2>
            </div>
            <span className="home-preview-meta">
              {featured.displayName} · {featured.overview.maps} mapas · {pct(featured.overview.roundWinRate)} dos rounds
            </span>
          </div>
          <div className="preview-score">
            <div className="preview-team">
              <small>TIME</small>
              <strong>{pct(item.teamRate)}</strong>
              <b>{featured.displayName}</b>
            </div>
            <div className="preview-focus">
              <span>{situationOnly(item)}</span>
              <strong>{item.map ? mapName(item.map) : 'Geral'}</strong>
              <small>
                {item.n} ocorrências, {evidence}
              </small>
            </div>
            <div className="preview-reference">
              <small>REFERÊNCIA · LIGA</small>
              <strong>{pct(item.benchmarkRate)}</strong>
            </div>
          </div>
          <p className="home-note">
            {item.map && !featured.attention.some((i) => !i.map)
              ? `Na média geral, nada aparece com evidência. O problema está em ${mapName(item.map)}: `
              : 'Principal ponto de atenção: '}
            {lowerFirst(situationOnly(item))}, {item.successes} de {item.n} com sucesso.
          </p>
          <span className="preview-action">
            Abrir análise completa <ArrowUpRight size={15} />
          </span>
        </button>
      )}

      <div className="home-teams">
        <div className="section-heading">
          <div>
            <span className="eyebrow">SELEÇÃO RÁPIDA</span>
            <h2>Outros times para começar</h2>
          </div>
        </div>
        <div className="team-grid">
          {QUICK.map((id) => {
            const t = teams.find((x) => x.team === id)
            const r = previews[id]
            const worse = r?.attention.length > 0
            const top = r?.attention[0] ?? r?.strengths[0]
            const accent = !r ? '' : worse ? (top.verdict === 'STRONG' ? 'red' : 'amber') : 'teal'
            return (
              <button key={id} type="button" className={`team-choice ${accent}`} onClick={() => onPick(id)}>
                <div>
                  <strong>{t?.displayName ?? r?.displayName ?? id}</strong>
                  {t && (
                    <span>
                      {t.matches} mapas · {pct(t.roundWinRate)} dos rounds
                    </span>
                  )}
                </div>
                <p>
                  {top ? (
                    <>
                      <b>{worse ? '▼ Ponto de atenção' : '▲ Ponto forte'}:</b> {top.label}, {pct(top.teamRate)} contra{' '}
                      {pct(top.benchmarkRate)} da liga.
                    </>
                  ) : r ? (
                    'Nenhuma diferença com evidência contra a liga.'
                  ) : (
                    'Carregando a descoberta principal…'
                  )}
                </p>
                <span className="team-link">
                  Abrir a análise <ArrowUpRight size={15} />
                </span>
              </button>
            )
          })}
        </div>
      </div>
    </section>
  )
}
