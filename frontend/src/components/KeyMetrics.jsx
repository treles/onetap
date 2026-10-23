import { Crosshair, Shield, Zap } from 'lucide-react'
import { pct } from '../api.js'
import { KEY_METRICS } from '../situations.js'
import { stakeText } from './SituationRow.jsx'
import { hasEvidence } from './VerdictTag.jsx'

const ICONS = { 'first-blood-conversion': Crosshair, 'post-plant': Zap, retake: Shield }

function status(item) {
  if (item.verdict === 'SMALL_SAMPLE') return ['muted', 'Amostra pequena']
  if (!hasEvidence(item.verdict)) return ['muted', 'Sem evidência']
  return item.atStake > 0 ? ['bad', 'Atenção'] : ['good', 'Ponto forte']
}

/** First Blood, Pós-plant e Retake, lidos das situações do relatório (nada é calculado aqui). */
export function KeyMetrics({ situations, shortRef, onOpen }) {
  const byId = Object.fromEntries(situations.map((s) => [s.situationId, s]))
  return (
    <div className="metrics-grid">
      {KEY_METRICS.map(({ id, title, subtitle }) => {
        const item = byId[id]
        const Icon = ICONS[id]
        if (!item) {
          return (
            <article className="metric-card" key={id}>
              <span className="metric-label">{title}</span>
              <p>Esta situação não ocorreu nas partidas do time.</p>
            </article>
          )
        }
        const [tone, label] = status(item)
        return (
          <button type="button" className="metric-card" key={id} onClick={() => onOpen(item)}>
            <div className="metric-top">
              <span className="metric-icon">
                <Icon size={17} />
              </span>
              <span className={`status ${tone}`}>{label}</span>
            </div>
            <span className="metric-label">{title}</span>
            <p>{subtitle}</p>
            <strong className="metric-value">{pct(item.teamRate)}</strong>
            <div className="metric-bottom">
              <span>
                vs <b>{pct(item.benchmarkRate)}</b> {shortRef.toLowerCase()}
              </span>
              <span>Saldo {stakeText(item)}</span>
            </div>
            <div className="progress">
              <i style={{ width: `${Math.round(item.teamRate * 100)}%` }} />
            </div>
            <small>
              {item.successes} de {item.n} rounds
            </small>
          </button>
        )
      })}
    </div>
  )
}
