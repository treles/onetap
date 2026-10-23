import { Trophy } from 'lucide-react'
import { mapName, pct } from '../api.js'
import { situationOnly } from './Home.jsx'

/** Placar com o principal ponto de atenção e o foco sugerido para o treino. */
export function FocusSection({ report, benchmark, shortRef, onOpen, onBenchmark }) {
  const top = report.attention[0]
  const { focus, strength, headline, vodRounds } = report.recommendation

  if (!top) {
    return (
      <section className="focus-callout">
        <div>
          <span>NENHUM PONTO DE ATENÇÃO</span>
          <h2>Nada abaixo da referência com evidência suficiente.</h2>
          {strength && <p className="positive">▲ {strength}</p>}
        </div>
        {benchmark === 'league' && (
          <button type="button" className="cta-button" onClick={() => onBenchmark('top25')}>
            Comparar com o quartil superior
          </button>
        )}
      </section>
    )
  }

  return (
    <>
      <section className="scoreboard" aria-label="Principal ponto de atenção">
        <div className="score-side team-side">
          <small>SEU TIME</small>
          <strong>{pct(top.teamRate)}</strong>
          <span>
            {top.successes} de {top.n} com sucesso
          </span>
        </div>
        <div className="score-center">
          <Trophy size={18} />
          <strong>{situationOnly(top)}</strong>
          <b>{top.map ? mapName(top.map) : 'Geral'}</b>
          <span>
            {top.n} ocorrências, {top.verdict === 'STRONG' ? 'evidência forte' : 'indício'}
          </span>
        </div>
        <div className="score-side ref-side">
          <small>REFERÊNCIA · {shortRef.toUpperCase()}</small>
          <strong>{pct(top.benchmarkRate)}</strong>
          <span>{top.map ? 'no mesmo mapa' : 'ajustada aos mapas do time'}</span>
        </div>
      </section>
      <section className="focus-callout">
        <div>
          <span>FOCO SUGERIDO PARA O TREINO</span>
          <h2>{focus ? `${focus[0].toUpperCase()}${focus.slice(1)}.` : headline}</h2>
          {strength && <p className="positive">▲ {strength}</p>}
        </div>
        <button type="button" className="cta-button" onClick={() => onOpen(top)}>
          Abrir os {vodRounds} rounds para rever
        </button>
      </section>
    </>
  )
}
