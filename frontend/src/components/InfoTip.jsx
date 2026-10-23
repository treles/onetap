/**
 * Dica que aparece ao passar o mouse ou focar o "i", em 150 ms. Fica ligada ao botão por
 * aria-describedby, então leitores de tela também a anunciam. Oculta, não ocupa espaço na página.
 */
export function InfoTip({ id, text, label }) {
  return (
    <span className="info-tip">
      <button type="button" className="info-tip-trigger" aria-describedby={id} aria-label={label}>
        i
      </button>
      <span role="tooltip" id={id} className="info-tip-box">
        {text}
      </span>
    </span>
  )
}
