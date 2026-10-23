import { useEffect, useState } from 'react'
import { api, pct } from '../api.js'

const thousands = (n) => n.toLocaleString('pt-BR')

/** "Como calculamos": regras, referências, evidência e limites, com os valores da liga vindos da API. */
export function Methodology({ onBack }) {
  const [m, setM] = useState(null)
  const [error, setError] = useState(null)
  useEffect(() => {
    api.methodology().then(setM).catch((e) => setError(e.message))
  }, [])

  return (
    <section className="methodology-page" aria-labelledby="methodology-title">
      <button type="button" className="back-link" onClick={onBack}>
        ← Voltar para a análise
      </button>
      <div className="methodology-header">
        <span className="eyebrow">TRANSPARÊNCIA DOS DADOS</span>
        <h1 id="methodology-title">Como calculamos</h1>
        <p>
          Cada mapa é lido a partir das partidas registradas, sem fonte externa. Reconstruímos cada round com abates,
          tempo, economia dos jogadores e plant ou defuse.
        </p>
      </div>

      {error && <p className="error-banner">{error}</p>}
      {!m && !error && <p className="loading-note">Carregando…</p>}

      {m && (
        <>
          <div className="methodology-grid">
            <article>
              <span className="methodology-index">01</span>
              <h2>O que entra na análise</h2>
              <p>
                Antes de contar qualquer situação, removemos observadores, mortes por bomba ou queda e abates de fogo
                amigo. Depois, conferimos se os abates de cada jogador batem com os registros da partida.
              </p>
            </article>
            <article>
              <span className="methodology-index">02</span>
              <h2>Com quem comparamos</h2>
              <p>
                A média da liga reúne todos os outros times. O quartil superior considera os {m.benchmarks[1].teams}{' '}
                times com maior taxa de rounds vencidos, entre os que jogaram pelo menos 8 mapas (
                {m.benchmarks[1].members.join(', ')}). O próprio time nunca entra na referência.
              </p>
            </article>
            <article>
              <span className="methodology-index">03</span>
              <h2>Mapas ajustados</h2>
              <p>
                A referência considera os mesmos mapas que o time jogou. Assim, um time que joga muito Ascent, onde o
                ataque da liga vence menos, não é comparado com uma média única injusta.
              </p>
            </article>
            <article>
              <span className="methodology-index">04</span>
              <h2>Como ler a evidência</h2>
              <p>
                Usamos um intervalo de confiança de 95%. <strong>Indício</strong> fica fora desse intervalo;{' '}
                <strong>evidência forte</strong> continua significativa depois de corrigirmos o fato de analisarmos
                várias situações ao mesmo tempo (Benjamini-Hochberg, falsas descobertas até {pct(m.fdr)}). Com menos de{' '}
                {m.minSample} ocorrências, não concluímos nada.
              </p>
            </article>
          </div>

          <section className="methodology-table-wrap">
            <div className="methodology-section-heading">
              <div>
                <span className="eyebrow">SITUAÇÕES MEDIDAS</span>
                <h2>O que cada taxa significa</h2>
              </div>
              <span className="methodology-note">Taxa da liga · ocorrências</span>
            </div>
            <div className="methodology-table" role="table" aria-label="Situações medidas e definições">
              <div className="methodology-table-row methodology-table-head" role="row">
                <span role="columnheader">Situação</span>
                <span role="columnheader">O que conta</span>
                <span role="columnheader">Referência</span>
              </div>
              {m.situations.map((s) => (
                <div className="methodology-table-row" role="row" key={s.id}>
                  <strong role="cell">{s.label}</strong>
                  <span role="cell">{s.definition}</span>
                  <b role="cell">
                    {pct(s.leagueRate, 1)} · {thousands(s.leagueTotal)}
                  </b>
                </div>
              ))}
            </div>
          </section>

          <section className="methodology-limits">
            <h2>Saldo e limites</h2>
            <p>
              <strong>Saldo</strong> é a diferença entre a taxa do time e a referência, multiplicada pelas ocorrências:
              uma estimativa de rounds em jogo, não uma previsão.
            </p>
            <ul>
              <li>Os dados mostram onde os rounds se perdem, não por quê. O motivo se confirma no VOD.</li>
              <li>O tipo de compra é estimado pela soma do equipamento dos 5 jogadores no início do round.</li>
              <li>Os nomes vêm dos arquivos; elencos podem mudar entre torneios.</li>
              <li>As sugestões de treino são orientações gerais, não conclusões dos dados.</li>
            </ul>
          </section>
        </>
      )}
    </section>
  )
}
