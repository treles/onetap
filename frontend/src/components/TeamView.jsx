import { useEffect, useRef, useState } from 'react'
import { ChevronDown } from 'lucide-react'
import { api, pct } from '../api.js'
import { AssistantBubble } from './AssistantBubble.jsx'
import { BestWorst } from './BestWorst.jsx'
import { FocusSection } from './FocusSection.jsx'
import { InfoTip } from './InfoTip.jsx'
import { KeyMetrics } from './KeyMetrics.jsx'
import { LossBreakdown } from './LossBreakdown.jsx'
import { MapAnalysis } from './MapAnalysis.jsx'
import { VodPanel } from './VodPanel.jsx'

export const BENCHMARKS = [
  ['league', 'Média da liga', 'Liga'],
  ['top25', 'Quartil superior', 'Top 25%'],
]
// Texto revisado pela Maria (commits "teste" na main), mantido no front novo.
const QUARTIL_TIP =
  'Os 25% dos times com maior taxa de rounds vencidos, entre os que jogaram pelo menos 8 mapas. ' +
  'A média da liga mostra o padrão geral, incluindo os times mais fracos. O próprio time não entra na própria referência.'
const ANCHORS = [
  ['Métricas-chave', 'metricas-chave'],
  ['Melhores e piores', 'melhores-e-piores'],
  ['Mapas', 'mapas'],
  ['Fila de VOD', 'vod'],
  ['Todas as situações', 'todas-as-situacoes'],
  ['Onde perdemos', 'onde-perdemos'],
]

export const scrollToId = (id) => document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' })

/** Página do time: placar, foco do treino, métricas, comparação, perdas, mapas e fila de VOD. */
export function TeamView({ teams, team, benchmark, onTeam, onBenchmark }) {
  const [report, setReport] = useState(null)
  const [losses, setLosses] = useState(null)
  const [vod, setVod] = useState(null) // { situationId, map }
  const [error, setError] = useState(null)
  const pendingScroll = useRef(false)

  useEffect(() => {
    setReport(null)
    setError(null)
    api
      .report(team, benchmark)
      .then((r) => {
        setReport(r)
        // a fila começa no principal ponto de atenção (ou na primeira situação listada)
        setVod((v) => v ?? { situationId: (r.attention[0] ?? r.situations[0]).situationId, map: r.attention[0]?.map ?? null })
      })
      .catch((e) => setError(e.message))
  }, [team, benchmark])

  useEffect(() => {
    api.losses(team).then(setLosses).catch((e) => setError(e.message))
  }, [team])

  useEffect(() => {
    if (pendingScroll.current && vod) {
      pendingScroll.current = false
      scrollToId('vod')
    }
  }, [vod])

  const openVod = (selection) => {
    pendingScroll.current = true
    setVod(selection)
  }
  const openItem = (item) => openVod({ situationId: item.situationId, map: item.map ?? null })
  const shortRef = BENCHMARKS.find(([id]) => id === benchmark)[2]
  const t = teams.find((x) => x.team === team)

  return (
    <>
      <section className="hero-row">
        <div>
          <div className="eyebrow">
            <span className="live-dot" /> ANÁLISE DE PERFORMANCE · VALORANT
          </div>
          <h1>{t?.displayName ?? report?.displayName ?? team}</h1>
          <p className="hero-copy">
            {report
              ? `${report.overview.maps} mapas, ${report.overview.mapsWon} vencidos, ${pct(report.overview.roundWinRate)} dos rounds. ` +
                `Comparado com ${report.benchmarkLabel.toLowerCase()} (${report.benchmarkTeams} times).`
              : 'Montando a análise…'}
          </p>
        </div>
        <div className="controls">
          <label className="select-control">
            <span>TIME</span>
            <select value={team} onChange={(e) => onTeam(e.target.value)}>
              {teams.map((x) => (
                <option key={x.team} value={x.team}>
                  {x.displayName}
                </option>
              ))}
            </select>
            <ChevronDown size={16} />
          </label>
          <label className="select-control">
            <span>
              REFERÊNCIA
              <InfoTip id="tip-quartil" text={QUARTIL_TIP} label="O que é o quartil superior?" />
            </span>
            <select value={benchmark} onChange={(e) => onBenchmark(e.target.value)} aria-describedby="tip-quartil">
              {BENCHMARKS.map(([id, label]) => (
                <option key={id} value={id}>
                  {label}
                </option>
              ))}
            </select>
            <ChevronDown size={16} />
          </label>
        </div>
      </section>

      {error && <p className="error-banner">{error}</p>}

      {report && (
        <>
          <FocusSection
            key={`${team}-${benchmark}`}
            report={report}
            benchmark={benchmark}
            shortRef={shortRef}
            onOpen={openItem}
            onBenchmark={onBenchmark}
          />

          <nav className="anchor-nav" aria-label="Seções da análise">
            {ANCHORS.map(([label, id]) => (
              <a
                key={id}
                href={`#${id}`}
                onClick={(e) => {
                  e.preventDefault()
                  scrollToId(id)
                }}
              >
                {label}
              </a>
            ))}
          </nav>

          <section className="section-block" id="metricas-chave">
            <div className="section-heading">
              <div>
                <span className="eyebrow">MÉTRICAS-CHAVE</span>
                <h2>Os momentos que mais decidem rounds</h2>
                <p>Comparados com a referência ({shortRef.toLowerCase()}), ajustada aos mapas que o time jogou.</p>
              </div>
            </div>
            <KeyMetrics situations={report.situations} shortRef={shortRef} onOpen={openItem} />
          </section>

          <section className="section-block" id="melhores-e-piores">
            <div className="section-heading">
              <div>
                <span className="eyebrow">COMPARAÇÃO</span>
                <h2>Onde a diferença aparece</h2>
                <p>
                  Saldo = diferença para a referência multiplicada pelas ocorrências. Itens com evidência aparecem
                  primeiro; os demais mostram que a diferença pode ser acaso.
                </p>
              </div>
            </div>
            <BestWorst report={report} onOpen={openItem} />
          </section>

          {losses && (
            <section className="section-block losses-section" id="onde-perdemos">
              <div className="section-heading">
                <div>
                  <span className="eyebrow">DIAGNÓSTICO DE DERROTAS</span>
                  <h2>Onde os rounds foram perdidos</h2>
                  <p>Barra: fatia das derrotas do time em cada tipo de round. Traço: a fatia típica na liga.</p>
                </div>
              </div>
              <LossBreakdown data={losses} />
            </section>
          )}

          <section className="section-block" id="mapas">
            <MapAnalysis team={team} benchmark={benchmark} onOpenVod={openVod} />
          </section>

          {vod && (
            <section id="vod">
              <VodPanel
                team={team}
                teamName={report.displayName}
                benchmark={benchmark}
                situations={report.situations}
                selection={vod}
                onChange={setVod}
              />
            </section>
          )}
        </>
      )}

      <AssistantBubble
        team={team}
        teamName={t?.displayName ?? report?.displayName ?? team}
        benchmark={benchmark}
        onOpenVod={openVod}
      />
    </>
  )
}
