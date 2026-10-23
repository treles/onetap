import { useEffect, useState } from 'react'
import { Clipboard, Target } from 'lucide-react'
import { api, mapName } from '../api.js'
import { REVIEW_REASON } from '../situations.js'
import { SituationRow } from './SituationRow.jsx'

async function copyText(text) {
  try {
    await navigator.clipboard.writeText(text)
  } catch {
    // fallback para navegadores sem a Clipboard API (ex.: página sem HTTPS)
    const area = document.createElement('textarea')
    area.value = text
    document.body.appendChild(area)
    area.select()
    document.execCommand('copy')
    area.remove()
  }
}

/**
 * Fila de VOD: escolha a situação (e o mapa) e veja cada round em que o time não teve sucesso,
 * com o motivo e a referência para copiar e localizar o round no VOD.
 */
export function VodPanel({ team, teamName, benchmark, situations, selection, onChange }) {
  const { situationId, map } = selection
  const [detail, setDetail] = useState(null)
  const [rounds, setRounds] = useState(null)
  const [error, setError] = useState(null)
  const [copied, setCopied] = useState(null)

  useEffect(() => {
    setDetail(null)
    setError(null)
    api.situation(team, situationId, benchmark).then(setDetail).catch((e) => setError(e.message))
  }, [team, situationId, benchmark])

  useEffect(() => {
    setRounds(null)
    api.vod(team, situationId, map).then(setRounds).catch((e) => setError(e.message))
  }, [team, situationId, map])

  const reason = REVIEW_REASON[situationId] ?? 'Sem sucesso na situação'
  const reference = (r) =>
    `${r.tournament}, ${r.date} | ${teamName} ${r.teamScore}–${r.opponentScore} ${r.opponent} | ${mapName(r.map)} | ` +
    `round ${r.round} | ${reason} | arquivo ${r.fileId}`
  const copy = async (text, key) => {
    await copyText(text)
    setCopied(key)
    setTimeout(() => setCopied((c) => (c === key ? null : c)), 1800)
  }
  const maps = detail?.byMap.map((i) => i.map) ?? []
  const next = rounds?.[0]

  return (
    <section className="vod-panel" aria-labelledby="vod-title">
      <div className="section-heading">
        <div>
          <span className="eyebrow">REVISÃO DE VOD</span>
          <h2 id="vod-title">Fila de VOD</h2>
          <p>
            Os rounds em que a situação aconteceu e o time não teve sucesso. Copie a referência para localizar o round no
            VOD; o motivo exato se confirma assistindo.
          </p>
        </div>
        {rounds?.length > 0 && (
          <button className="copy-button" type="button" onClick={() => copy(rounds.map(reference).join('\n'), 'all')}>
            <Clipboard size={15} /> {copied === 'all' ? 'Fila copiada' : 'Copiar fila inteira'}
          </button>
        )}
      </div>

      <div className="vod-toolbar">
        <label>
          <span>SITUAÇÃO</span>
          <select value={situationId} onChange={(e) => onChange({ situationId: e.target.value, map: null })}>
            {situations.map((s) => (
              <option key={s.situationId} value={s.situationId}>
                {s.label}
              </option>
            ))}
          </select>
        </label>
        <div className="map-filter" role="group" aria-label="Filtrar por mapa">
          {[null, ...maps].map((m) => (
            <button
              key={m ?? 'todos'}
              className={map === m ? 'active' : ''}
              type="button"
              aria-pressed={map === m}
              onClick={() => onChange({ situationId, map: m })}
            >
              {m ? mapName(m) : 'Todos os mapas'}
            </button>
          ))}
        </div>
      </div>

      {error && <p className="error-banner">{error}</p>}

      <div className="situation-list" id="todas-as-situacoes">
        {situations.map((s) => (
          <SituationRow
            key={s.id}
            item={s}
            selected={s.situationId === situationId}
            onOpen={() => onChange({ situationId: s.situationId, map: null })}
          />
        ))}
      </div>

      <div className="vod-queue-meta">
        {rounds === null ? (
          <span>Carregando os rounds…</span>
        ) : (
          <>
            <strong>{rounds.length}</strong>
            <span>
              {' '}
              {rounds.length === 1 ? 'round para rever' : 'rounds para rever'}
              {map ? ` em ${mapName(map)}` : ''}. Motivo: {reason.toLowerCase()}.
            </span>
          </>
        )}
      </div>

      {next && (
        <div className="vod-callout">
          <div>
            <span className="eyebrow">PRÓXIMO ROUND DA FILA</span>
            <strong>
              <Target size={16} /> Round {next.round} contra {next.opponent}, {mapName(next.map)}
            </strong>
            <p>
              {next.tournament}, {next.date}. Placar final {next.teamScore}–{next.opponentScore}.
            </p>
          </div>
          <button className="watch-button" type="button" onClick={() => copy(reference(next), 'next')}>
            <Clipboard size={15} /> {copied === 'next' ? 'Copiado' : 'Copiar referência'}
          </button>
        </div>
      )}

      {rounds?.length === 0 && <p className="empty-note">Nenhum round sem sucesso nesta situação.</p>}

      {rounds?.length > 0 && (
        <div className="vod-table-wrap">
          <table className="vod-table">
            <thead>
              <tr>
                <th>Mapa</th>
                <th>Partida</th>
                <th style={{ textAlign: 'right' }}>Round</th>
                <th>Motivo</th>
                <th>
                  <span className="sr-only">Copiar</span>
                </th>
              </tr>
            </thead>
            <tbody>
              {rounds.map((r) => {
                const key = `${r.fileId}-${r.round}`
                const won = r.teamScore > r.opponentScore
                return (
                  <tr key={key}>
                    <td className="map-cell">{mapName(r.map)}</td>
                    <td>
                      <strong>contra {r.opponent}</strong>{' '}
                      <span className={won ? 'positive' : 'negative'}>
                        {r.teamScore}–{r.opponentScore}
                      </span>
                      <span className="match-meta">
                        {r.tournament}, {r.date}
                      </span>
                    </td>
                    <td className="round-cell">{r.round}</td>
                    <td className="reason-cell">{reason}</td>
                    <td className="copy-cell">
                      <button className="copy-button" type="button" onClick={() => copy(reference(r), key)}>
                        <Clipboard size={14} /> {copied === key ? 'Copiado' : 'Copiar referência'}
                      </button>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}
