"""Prototipo dos indicadores situacionais + ranking de oportunidades (rounds em jogo)."""
import json, sys, os, glob, re, math, collections as C

root = sys.argv[1]
FOCUS = sys.argv[2] if len(sys.argv) > 2 else 'ufu_saints'
files = sorted(glob.glob(os.path.join(root, '**', '*.json'), recursive=True))
FN = re.compile(r'^(?P<prefix>.+?)_(?P<num>\d+[a-z]?)_(?P<date>\d\d-\d\d-\d\d)_(?P<a>.+)_(?P<sa>\d+)_x_(?P<sb>\d+)_(?P<b>.+)_(?P<map>[a-z]+)$')
TRADE_MS = 5000

def buy(v):
    return 'eco' if v < 5000 else 'semi' if v < 15000 else 'semi-full' if v < 20000 else 'full'

def wilson(w, n, z=1.96):
    if n == 0: return (0, 0)
    p = w / n; den = 1 + z * z / n
    c = (p + z * z / (2 * n)) / den; h = z * math.sqrt(p * (1 - p) / n + z * z / (4 * n * n)) / den
    return (c - h, c + h)

# situacao -> time -> [vitorias, ocorrencias]; e lista de (arquivo, round) para "fila de VOD"
S = C.defaultdict(lambda: C.defaultdict(lambda: [0, 0]))
EVID = C.defaultdict(list)
ply = C.defaultdict(C.Counter)
pname = {}

def add(sit, team, won, ref=None):
    S[sit][team][0] += won; S[sit][team][1] += 1
    if ref and team == FOCUS and not won: EVID[sit].append(ref)

for f in files:
    base = os.path.splitext(os.path.basename(f))[0]
    m = FN.match(base)
    d = json.load(open(f, encoding='utf-8'))
    sa, sb = int(m['sa']), int(m['sb'])
    name = {t['teamId']: (m['a'] if t['roundsWon'] == sa else m['b']) for t in d['teams']}
    pl = [p for p in d['players'] if not p.get('isObserver')]
    team = {p['puuid']: p['teamId'] for p in pl}
    for p in pl: pname[p['puuid']] = p['gameName']
    prev_winner = None
    for r in d['roundResults']:
        rn = r['roundNum']; win = r['winningTeam']
        role = {win: r['winningTeamRole']}
        role[[s for s in ('Red', 'Blue') if s != win][0]] = 'Defender' if r['winningTeamRole'] == 'Attacker' else 'Attacker'
        ref = f"{base} | round {rn + 1}"
        kills = sorted((k for ps in r['playerStats'] for k in ps['kills']
                        if k['killer'] != k['victim'] and team.get(k['killer']) != team.get(k['victim'])),
                       key=lambda k: k['timeSinceRoundStartMillis'])
        load = C.Counter()
        for ps in r['playerStats']:
            if ps['puuid'] in team: load[team[ps['puuid']]] += (ps.get('economy') or {}).get('loadoutValue') or 0
        pistol = rn in (0, 12)
        for side in ('Red', 'Blue'):
            opp = 'Blue' if side == 'Red' else 'Red'
            t = name[side]; won = win == side
            add('round', t, won)
            add(f"{role[side].lower()}", t, won)
            if pistol: add('pistol', t, won, ref)
            if rn in (1, 13) and prev_winner is not None:
                pass
            if not pistol and buy(load[side]) in ('full', 'semi-full') and buy(load[opp]) == 'eco':
                add('anti-eco (full vs eco)', t, won, ref)
            if not pistol and buy(load[side]) in ('eco', 'semi') and buy(load[opp]) in ('full', 'semi-full'):
                add('force/eco vs full', t, won, ref)
            if r.get('plantSite'):
                if role[side] == 'Attacker': add('pos-plant (ataque)', t, won, ref)
                else: add('retake (defesa)', t, won, ref)
        if kills:
            fb = kills[0]; fbs = team[fb['killer']]; fds = team[fb['victim']]
            add('converter first blood', name[fbs], win == fbs, ref)
            add('reagir a first blood sofrido', name[fds], win == fds, ref)
            traded = any(k['victim'] == fb['killer'] and k['timeSinceRoundStartMillis'] - fb['timeSinceRoundStartMillis'] <= TRADE_MS for k in kills[1:])
            add('trade do primeiro abatido (<=5s)', name[fds], traded, ref)
            ply[fb['killer']]['fk'] += 1; ply[fb['victim']]['fd'] += 1
            # clutch: ultimo vivo de um time contra >=2
            alive = {'Red': 5, 'Blue': 5}; clutch_logged = set()
            for k in kills:
                alive[team[k['victim']]] -= 1
                for s in ('Red', 'Blue'):
                    o = 'Blue' if s == 'Red' else 'Red'
                    if alive[s] == 1 and alive[o] >= 2 and s not in clutch_logged:
                        clutch_logged.add(s); add('clutch 1vX (X>=2)', name[s], win == s, ref)
        prev_winner = win
        # KAST por jogador
        for ps in r['playerStats']:
            p = ps['puuid']
            if p not in team: continue
            k_ = any(k['killer'] == p for k in kills); a_ = any(p in (k.get('assistants') or []) for k in kills)
            death = next((k for k in kills if k['victim'] == p), None)
            tr = death is not None and any(k['victim'] == death['killer'] and 0 <= k['timeSinceRoundStartMillis'] - death['timeSinceRoundStartMillis'] <= TRADE_MS for k in kills)
            ply[p]['rounds'] += 1; ply[p]['kast'] += (k_ or a_ or death is None or tr)
            for dm in ps['damage']:
                ply[p]['dmg'] += dm['damage']; ply[p]['hs'] += dm['headshots']; ply[p]['shots'] += dm['headshots'] + dm['bodyshots'] + dm['legshots']

print(f"=== Ranking de oportunidades: {FOCUS} (comparado a media da liga) ===")
rows = []
for sit, teams in S.items():
    lw = sum(v[0] for v in teams.values()); ln = sum(v[1] for v in teams.values())
    w, n = teams.get(FOCUS, [0, 0])
    if n == 0: continue
    base = lw / ln; rate = w / n
    rows.append((sit, rate, base, n, (base - rate) * n, wilson(w, n)))
for sit, rate, base, n, gap, (lo, hi) in sorted(rows, key=lambda x: -x[4]):
    flag = 'AMOSTRA PEQUENA' if n < 20 else ('significativo' if (hi < base or lo > base) else 'dentro do IC')
    print(f"  {sit:34s} time={100*rate:5.1f}%  liga={100*base:5.1f}%  n={n:4d}  rounds_em_jogo={gap:+6.1f}  IC95=[{100*lo:.0f}-{100*hi:.0f}]  {flag}")
top = sorted(rows, key=lambda x: -x[4])[0][0]
print(f"\nFila de VOD para '{top}' (rounds perdidos):", EVID[top][:6], '...', len(EVID[top]))
print("\nJogadores do time (KAST, ADR, HS%, FK/FD):")
roster = set()
for f in files:
    pass
for p, c in sorted(ply.items(), key=lambda x: -x[1]['rounds']):
    pass
R = sum(c['rounds'] for c in ply.values())
print(f"  liga: KAST medio {100*sum(c['kast'] for c in ply.values())/R:.1f}%  ADR medio {sum(c['dmg'] for c in ply.values())/R:.1f}  HS% {100*sum(c['hs'] for c in ply.values())/sum(c['shots'] for c in ply.values()):.1f}%")
