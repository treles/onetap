import { useEffect, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import { Send, Target, X } from 'lucide-react'
import { api, num } from '../api.js'
import { TurretIcon } from './TurretIcon.jsx'
import { KILLJOY, killjoyCloser, killjoyOpener, killjoyThinking } from '../killjoy.js'

const SOURCE_KIND = {
  SITUATION: 'Relatório',
  SITUATION_MAP: 'Relatório por mapa',
  LOSS: 'Onde perdemos',
  DEFINITION: 'Definição',
  DRILL: 'Sugestão de treino',
  METHOD: 'Como calculamos',
}

/**
 * Assistente do treinador, com a persona da Killjoy. Não gera texto: o backend reconhece a pergunta, busca
 * os trechos da análise do time e monta a resposta com frases-modelo; aqui exibimos com as fontes e
 * acrescentamos só a fala de abertura/fechamento da persona (killjoy.js), separada dos números.
 */
export function AssistantBubble({ team, teamName, benchmark, onOpenVod }) {
  const [open, setOpen] = useState(false)
  const [suggestions, setSuggestions] = useState([])
  const [messages, setMessages] = useState([])
  const [input, setInput] = useState('')
  const [busy, setBusy] = useState(false)
  const [host, setHost] = useState(null)
  const logRef = useRef(null)
  const inputRef = useRef(null)

  // O .app só existe no DOM depois da primeira renderização; o tema claro/escuro vem dele.
  useEffect(() => setHost(document.querySelector('.app') ?? document.body), [])

  useEffect(() => {
    // troca rápida de referência: ignora a resposta que chegar atrasada
    let current = true
    api
      .suggestions(team, benchmark)
      .then((s) => current && setSuggestions(s))
      .catch(() => current && setSuggestions([]))
    return () => {
      current = false
    }
  }, [team, benchmark])

  useEffect(() => {
    if (open) inputRef.current?.focus()
  }, [open])

  // Respostas longas (como o plano) são lidas do começo: rola até a pergunta, não até o fim.
  useEffect(() => {
    const log = logRef.current
    if (!log) return
    const questions = log.querySelectorAll('.assistant-user')
    const last = questions[questions.length - 1]
    log.scrollTop = last ? last.offsetTop - 12 : 0
  }, [messages, busy])

  useEffect(() => {
    if (!open) return
    const onKey = (e) => e.key === 'Escape' && setOpen(false)
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [open])

  const ask = async (question) => {
    const q = question.trim()
    if (!q || busy) return
    setInput('')
    setMessages((m) => [...m, { role: 'user', text: q }])
    setBusy(true)
    try {
      const answer = await api.ask(team, benchmark, q)
      setMessages((m) => [...m, { role: 'bot', answer }])
    } catch (e) {
      setMessages((m) => [...m, { role: 'error', text: e.message }])
    } finally {
      setBusy(false)
    }
  }

  const openVod = (action) => {
    onOpenVod({ situationId: action.situationId, map: action.map ?? null })
    setOpen(false)
  }

  if (!host) return null
  // Fora do <main>: o conteúdo cria um contexto de empilhamento (z-index 1) que ficaria sob a barra do topo.
  return createPortal(
    <>
      <div className="assistant-dock">
        {/* balãozinho de fala: atalho visual; para teclado e leitor de tela, a torreta é o botão */}
        {!open && (
          <button type="button" className="assistant-hint" onClick={() => setOpen(true)} tabIndex={-1} aria-hidden="true">
            <strong>{KILLJOY.name}</strong>
            {KILLJOY.hint}
          </button>
        )}
        <button
          type="button"
          className={open ? 'assistant-turret is-open' : 'assistant-turret'}
          onClick={() => setOpen((o) => !o)}
          aria-expanded={open}
          aria-controls="assistant-panel"
          aria-label={open ? `Fechar a ${KILLJOY.name}` : `Abrir a ${KILLJOY.name}, assistente tática`}
          title={open ? 'Fechar' : KILLJOY.name}
        >
          <TurretIcon size={68} alert={open} />
        </button>
      </div>

      {open && (
        <section id="assistant-panel" className="assistant-panel" aria-label={`${KILLJOY.name}, assistente tática`}>
          <header className="assistant-header">
            <span className="assistant-avatar" aria-hidden="true">
              <TurretIcon size={34} />
            </span>
            <div>
              <strong>{KILLJOY.name}</strong>
              <small>
                {KILLJOY.role} · só dados da análise de {teamName}
              </small>
            </div>
            <button type="button" className="assistant-close" onClick={() => setOpen(false)} aria-label="Fechar">
              <X size={16} />
            </button>
          </header>

          <div className="assistant-log" ref={logRef} aria-live="polite">
            <div className="assistant-intro">
              <p className="assistant-voice">{KILLJOY.intro(teamName)}</p>
              <div className="assistant-chips">
                {suggestions.map((s) => (
                  <button key={s.question} type="button" onClick={() => ask(s.question)} disabled={busy}>
                    {s.label}
                  </button>
                ))}
              </div>
            </div>

            {messages.map((m, i) =>
              m.role === 'user' ? (
                <p key={i} className="assistant-user">
                  {m.text}
                </p>
              ) : m.role === 'error' ? (
                <p key={i} className="assistant-error">
                  {m.text}
                </p>
              ) : (
                <AnswerCard key={i} answer={m.answer} onAsk={ask} onOpenVod={openVod} busy={busy} />
              ),
            )}
            {busy && <p className="assistant-wait">{killjoyThinking(String(messages.length))}</p>}
            <p className="assistant-disclaimer">{KILLJOY.disclaimer}</p>
          </div>

          <form
            className="assistant-form"
            onSubmit={(e) => {
              e.preventDefault()
              ask(input)
            }}
          >
            <input
              ref={inputRef}
              value={input}
              onChange={(e) => setInput(e.target.value)}
              maxLength={300}
              placeholder={KILLJOY.placeholder}
              aria-label={`Pergunta para a ${KILLJOY.name}`}
            />
            <button type="submit" disabled={busy || !input.trim()} aria-label="Enviar pergunta">
              <Send size={16} />
            </button>
          </form>
        </section>
      )}
    </>,
    host,
  )
}

function AnswerCard({ answer, onAsk, onOpenVod, busy }) {
  const opener = killjoyOpener(answer)
  const closer = killjoyCloser(answer)
  return (
    <article className="assistant-answer">
      {opener && <p className="assistant-voice">{opener}</p>}
      <h3>{answer.title}</h3>
      {answer.paragraphs.map((p, i) => (
        <p key={i}>{p}</p>
      ))}
      {answer.sections.map((s, i) => (
        <div key={i} className="assistant-section">
          <h4>{s.title}</h4>
          <ul>
            {s.lines.map((line, j) => (
              <li key={j}>{line}</li>
            ))}
          </ul>
          {s.action && <VodButton action={s.action} onOpenVod={onOpenVod} />}
        </div>
      ))}
      {answer.actions.map((a, i) => (
        <VodButton key={i} action={a} onOpenVod={onOpenVod} />
      ))}
      {closer && <p className="assistant-voice closer">{closer}</p>}
      {answer.followUps.length > 0 && (
        <div className="assistant-chips follow">
          {answer.followUps.map((q) => (
            <button key={q} type="button" onClick={() => onAsk(q)} disabled={busy}>
              {q}
            </button>
          ))}
        </div>
      )}
      {answer.intent !== 'OFFLINE' && (
        <details className="assistant-sources">
          <summary>
            {answer.sources.length === 1 ? '1 fonte' : `${answer.sources.length} fontes`} ·{' '}
            {num(answer.tookMs, 1)} ms
          </summary>
          <ul>
            {answer.sources.map((s) => (
              <li key={s.id}>
                <span>{SOURCE_KIND[s.kind] ?? s.kind}</span> {s.title}
                {s.score != null && <small> · relevância {num(s.score, 1)}</small>}
              </li>
            ))}
          </ul>
        </details>
      )}
    </article>
  )
}

function VodButton({ action, onOpenVod }) {
  return (
    <button type="button" className="assistant-vod" onClick={() => onOpenVod(action)}>
      <Target size={14} /> {action.label}
    </button>
  )
}
