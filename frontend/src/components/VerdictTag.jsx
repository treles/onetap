// Veredito com ícone + texto (nunca só cor). A direção (atenção x força) vem do sinal do saldo.
const LABELS = {
  STRONG: 'Evidência forte',
  INDICATION: 'Indício',
  NOT_PROVEN: 'Sem evidência',
  SMALL_SAMPLE: 'Amostra pequena',
}

export const hasEvidence = (verdict) => verdict === 'STRONG' || verdict === 'INDICATION'

export function VerdictTag({ verdict, atStake }) {
  if (!hasEvidence(verdict)) return <span className="verdict verdict-tag muted">{LABELS[verdict]}</span>
  const negative = atStake > 0
  return (
    <span className={`verdict verdict-tag ${negative ? 'attention' : 'strength'}`}>
      <span aria-hidden="true">{negative ? '▼' : '▲'}</span>
      {LABELS[verdict]}
    </span>
  )
}

/** Texto curto do veredito com a seta, para linhas compactas. */
export function verdictText(verdict, atStake) {
  if (!hasEvidence(verdict)) return LABELS[verdict]
  return `${atStake > 0 ? '▼' : '▲'} ${LABELS[verdict]}`
}
