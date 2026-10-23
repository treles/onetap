import { num, pct, unitLabel } from '../api.js'
import { VerdictTag, hasEvidence } from './VerdictTag.jsx'

/** Saldo com sinal: "−9,4 rounds" quando o time está abaixo da referência. */
export function stakeText(item) {
  const abs = Math.abs(item.atStake)
  const sign = abs < 0.05 ? '' : item.atStake > 0 ? '−' : '+'
  return `${sign}${num(abs, 1)} ${unitLabel(item.unit)}`
}

/** Uma situação: taxa do time contra a referência, ocorrências, saldo e veredito. */
export function SituationRow({ item, selected, onOpen }) {
  const evidence = hasEvidence(item.verdict)
  const tone = evidence ? (item.atStake > 0 ? 'negative' : 'positive') : 'neutral'
  const content = (
    <>
      <span className="situation-name">
        <strong>{item.label}</strong>
        <small>{item.definition}</small>
      </span>
      <span className="situation-rate">
        <i style={{ width: `${Math.round(item.teamRate * 100)}%` }} />
        <em>
          {pct(item.teamRate)} <small>vs {pct(item.benchmarkRate)}</small>
        </em>
      </span>
      <span className="situation-sample">{item.n} rounds</span>
      <span className={`situation-stake ${tone}`}>{stakeText(item)}</span>
      <VerdictTag verdict={item.verdict} atStake={item.atStake} />
    </>
  )
  return onOpen ? (
    <button type="button" className={`situation-row ${selected ? 'selected' : ''}`} onClick={onOpen} aria-pressed={selected}>
      {content}
    </button>
  ) : (
    <div className="situation-row">{content}</div>
  )
}
