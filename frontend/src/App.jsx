import { useEffect, useState } from 'react'
import { api } from './api.js'
import { Footer } from './components/Footer.jsx'
import { Home } from './components/Home.jsx'
import { Methodology } from './components/Methodology.jsx'
import { TeamView, scrollToId } from './components/TeamView.jsx'
import { TopBar } from './components/TopBar.jsx'

const THEME_KEY = 'liga-coach-theme'

// Estado na URL (#time=...&ref=...&pagina=...) para links diretos na demo.
function readHash() {
  const p = new URLSearchParams(window.location.hash.slice(1))
  return { team: p.get('time') ?? '', ref: p.get('ref') ?? 'league', page: p.get('pagina') ?? 'pauta' }
}
function writeHash({ team, ref, page }) {
  const p = new URLSearchParams()
  if (team) p.set('time', team)
  if (ref !== 'league') p.set('ref', ref)
  if (page !== 'pauta') p.set('pagina', page)
  window.history.replaceState(null, '', `#${p}`)
}
function initialDark() {
  try {
    const saved = localStorage.getItem(THEME_KEY)
    if (saved === 'light' || saved === 'dark') return saved === 'dark'
  } catch {
    // armazenamento indisponível: usa o padrão
  }
  return true
}
const toTop = () => window.scrollTo({ top: 0, behavior: 'smooth' })

export default function App() {
  const initial = readHash()
  const [dark, setDark] = useState(initialDark)
  const [teams, setTeams] = useState([])
  const [team, setTeam] = useState(initial.team)
  const [ref, setRef] = useState(initial.ref)
  const [page, setPage] = useState(initial.page)
  const [error, setError] = useState(null)

  useEffect(() => {
    api
      .teams()
      .then(setTeams)
      .catch((e) => setError(`Não foi possível carregar os times: ${e.message}. Verifique se o backend está rodando.`))
  }, [])

  useEffect(() => writeHash({ team, ref, page }), [team, ref, page])

  useEffect(() => {
    const onHash = () => {
      const h = readHash()
      setTeam(h.team)
      setRef(h.ref)
      setPage(h.page)
    }
    window.addEventListener('hashchange', onHash)
    return () => window.removeEventListener('hashchange', onHash)
  }, [])

  useEffect(() => {
    document.documentElement.style.colorScheme = dark ? 'dark' : 'light'
    try {
      localStorage.setItem(THEME_KEY, dark ? 'dark' : 'light')
    } catch {
      // armazenamento indisponível: o tema vale só nesta visita
    }
  }, [dark])

  const goHome = () => {
    setPage('pauta')
    setTeam('')
    toTop()
  }
  const pickTeam = (id) => {
    if (!id) return
    setPage('pauta')
    setTeam(id)
    toTop()
  }
  const showMethodology = () => {
    setPage('metodo')
    toTop()
  }

  return (
    <div className={dark ? 'app dark valorant-theme' : 'app valorant-theme'}>
      <div className="backdrop" />
      <TopBar
        dark={dark}
        onToggleTheme={() => setDark((d) => !d)}
        onHome={goHome}
        onMethodology={showMethodology}
        onSection={scrollToId}
        page={page}
        teamOpen={page === 'pauta' && Boolean(team)}
      />
      <main className="shell content" id="visao-geral">
        {error && <p className="error-banner">{error}</p>}
        {page === 'metodo' ? (
          <Methodology onBack={() => setPage('pauta')} />
        ) : !team ? (
          <Home teams={teams} onPick={pickTeam} />
        ) : (
          <TeamView key={team} teams={teams} team={team} benchmark={ref} onTeam={pickTeam} onBenchmark={setRef} />
        )}
      </main>
      <Footer />
    </div>
  )
}
