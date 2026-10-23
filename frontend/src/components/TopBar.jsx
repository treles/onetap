import { useState } from 'react'
import { Menu, Moon, Sun, Swords, X } from 'lucide-react'

const SECTION_LINKS = [
  ['Mapas', 'mapas'],
  ['Situações', 'melhores-e-piores'],
  ['Fila de VOD', 'vod'],
]

/** Barra do topo: marca, navegação (com menu no celular) e troca de tema. */
export function TopBar({ dark, onToggleTheme, onHome, onMethodology, onSection, page, teamOpen }) {
  const [open, setOpen] = useState(false)
  const close = () => setOpen(false)

  return (
    <header className="topbar">
      <div className="shell nav-inner">
        <button type="button" className="brand" aria-label="Liga Coach, voltar para a página inicial" onClick={onHome}>
          <span className="brand-mark">
            <Swords size={18} />
          </span>
          <span>LIGA COACH</span>
        </button>
        <button
          type="button"
          className="mobile-menu-toggle"
          aria-label={open ? 'Fechar menu' : 'Abrir menu'}
          aria-expanded={open}
          onClick={() => setOpen((o) => !o)}
        >
          {open ? <X size={20} /> : <Menu size={20} />}
        </button>
        <nav className={open ? 'nav-links mobile-open' : 'nav-links'} aria-label="Navegação principal" onClick={close}>
          <a
            href="#"
            className={page === 'pauta' && !teamOpen ? 'active' : undefined}
            onClick={(e) => {
              e.preventDefault()
              onHome()
            }}
          >
            Visão geral
          </a>
          {teamOpen &&
            SECTION_LINKS.map(([label, id]) => (
              <a
                key={id}
                href={`#${id}`}
                onClick={(e) => {
                  e.preventDefault()
                  onSection(id)
                }}
              >
                {label}
              </a>
            ))}
          <button
            type="button"
            className={`nav-methodology ${page === 'metodo' ? 'active' : ''}`}
            onClick={onMethodology}
          >
            Como calculamos
          </button>
        </nav>
        <div className="nav-actions">
          <button
            type="button"
            className="icon-button"
            onClick={onToggleTheme}
            aria-label={dark ? 'Ativar modo claro' : 'Ativar modo escuro'}
            title={dark ? 'Modo claro' : 'Modo escuro'}
          >
            {dark ? <Sun size={18} /> : <Moon size={18} />}
          </button>
        </div>
      </div>
    </header>
  )
}
