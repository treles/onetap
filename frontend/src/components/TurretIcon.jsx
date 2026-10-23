/**
 * Torreta desenhada para o projeto (inspirada na da Killjoy, sem arte oficial): tripé, corpo amarelo,
 * cano e um sensor que pisca. `alert` deixa o sensor vermelho (balão aberto).
 */
export function TurretIcon({ size = 40, alert = false }) {
  return (
    <svg
      className={alert ? 'turret alert' : 'turret'}
      width={size}
      height={size}
      viewBox="0 0 48 48"
      aria-hidden="true"
      focusable="false"
    >
      <path d="M24 31 L12 45 M24 31 L36 45 M24 31 V46" stroke="#8a8f99" strokeWidth="3" strokeLinecap="round" />
      <rect x="17" y="27" width="14" height="5" rx="1.5" fill="#2a2f38" />
      <g className="turret-head">
        <rect className="turret-barrel" x="33" y="17.5" width="12" height="5" rx="1.2" fill="#17191f" stroke="#f2d23c" />
        <path d="M11 14 H31 L37 20 V25 L34 28 H14 L11 25 Z" fill="#f2d23c" stroke="#17191f" strokeWidth="2" strokeLinejoin="round" />
        <path d="M14 24 H33" stroke="#17191f" strokeWidth="1.5" opacity=".35" />
        <circle cx="22" cy="20" r="3.8" fill="#17191f" />
        <circle className="turret-eye" cx="22" cy="20" r="2" />
        <path d="M15 14 V8.5" stroke="#17191f" strokeWidth="2" strokeLinecap="round" />
        <circle className="turret-eye" cx="15" cy="7.5" r="1.7" />
      </g>
    </svg>
  )
}
