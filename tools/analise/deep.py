import json, sys
f = sys.argv[1]
d = json.load(open(f, encoding='utf-8'))
print('TOP KEYS', list(d.keys()))
print('premierMatchInfo', d['matchInfo'].get('premierMatchInfo'))
p = d['players'][0]
print('player keys', list(p.keys()))
print('abilityCasts', p['stats'].get('abilityCasts'))
print('teams', d['teams'])
print('coaches', len(d['coaches']))
r = d['roundResults'][3]
print('round keys', list(r.keys()))
for k in r:
    if k != 'playerStats':
        print('  ', k, '=', json.dumps(r[k])[:200])
ps = r['playerStats'][0]
print('playerStats keys', list(ps.keys()))
for k, v in ps.items():
    print('  PS', k, '=', json.dumps(v)[:600])
# a playerStats with kills
for ps in r['playerStats']:
    if ps['kills']:
        print('KILL EVENT', json.dumps(ps['kills'][0], indent=1)[:2500])
        break
for k in d:
    if k not in ('matchInfo', 'players', 'coaches', 'teams', 'roundResults'):
        v = d[k]
        print('EXTRA TOP', k, type(v).__name__, len(v) if hasattr(v, '__len__') else '', json.dumps(v)[:1500])
