import { pct } from '../api.js'

/** Onde o time perdeu rounds: barra = fatia das derrotas do time; traço = fatia típica na liga. */
export function LossBreakdown({ data }) {
  return (
    <div className="losses-card">
      <strong>
        {data.roundsLost} <span>rounds perdidos, cada um contado em uma única categoria.</span>
      </strong>
      {data.categories.map((c) => (
        <div className="loss-row" key={c.id}>
          <span>{c.label}</span>
          <div
            className="loss-track"
            role="img"
            aria-label={`${c.label}: ${c.rounds} rounds, ${pct(c.share)} das derrotas do time; liga ${pct(c.leagueShare)}`}
          >
            <i style={{ width: `${c.share * 100}%` }} />
            <b style={{ left: `${c.leagueShare * 100}%` }} />
          </div>
          <em>
            <strong>{pct(c.share)}</strong> vs {pct(c.leagueShare)}
          </em>
        </div>
      ))}
    </div>
  )
}
