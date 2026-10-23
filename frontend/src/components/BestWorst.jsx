import { pct } from '../api.js'
import { stakeText } from './SituationRow.jsx'
import { hasEvidence, verdictText } from './VerdictTag.jsx'

const LIMIT = 4

/**
 * Melhores e piores desempenhos contra a referência, a partir dos itens do relatório
 * (situações gerais e destaques por mapa). Itens com evidência vêm primeiro; cada linha mostra o veredito.
 */
export function BestWorst({ report, onOpen }) {
  const pool = new Map()
  for (const i of [...report.situations, ...report.attention, ...report.strengths]) {
    if (i.verdict !== 'SMALL_SAMPLE') pool.set(i.id, i)
  }
  const items = [...pool.values()]
  const rank = (sign) =>
    items
      .filter((i) => sign * i.atStake > 0.05)
      .sort((a, b) => hasEvidence(b.verdict) - hasEvidence(a.verdict) || sign * (b.atStake - a.atStake))
      .slice(0, LIMIT)

  return (
    <div className="insights-grid">
      <Column title="Piores contra a referência" items={rank(1)} positive={false} onOpen={onOpen} />
      <Column title="Melhores contra a referência" items={rank(-1)} positive onOpen={onOpen} />
    </div>
  )
}

function Column({ title, items, positive, onOpen }) {
  return (
    <div>
      <h3>{title}</h3>
      {items.length === 0 && <p className="empty-note">Nenhuma situação com amostra suficiente nesse sentido.</p>}
      {items.map((item) => (
        <button
          type="button"
          key={item.id}
          className={`insight-row ${positive ? 'positive-row' : 'negative-row'}`}
          onClick={() => onOpen(item)}
        >
          <div>
            <strong>{item.label}</strong>
            <span>
              {pct(item.teamRate)} vs {pct(item.benchmarkRate)} em {item.n}
            </span>
          </div>
          <b>{stakeText(item)}</b>
          <small className={hasEvidence(item.verdict) ? (positive ? 'positive' : 'negative') : 'muted-text'}>
            {verdictText(item.verdict, item.atStake)}
          </small>
        </button>
      ))}
    </div>
  )
}
