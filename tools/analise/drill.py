import json, sys, os, glob, re, collections as C
root, T = sys.argv[1], sys.argv[2]
FN = re.compile(r'^(?P<prefix>.+?)_(?P<num>\d+[a-z]?)_(?P<date>\d\d-\d\d-\d\d)_(?P<a>.+)_(?P<sa>\d+)_x_(?P<sb>\d+)_(?P<b>.+)_(?P<map>[a-z]+)$')
pref = C.Counter(); bymap = C.defaultdict(lambda: [0,0,0,0]); sites = C.Counter(); siteswon = C.Counter(); noplant = [0,0]
for f in sorted(glob.glob(os.path.join(root, '**', '*.json'), recursive=True)):
    m = FN.match(os.path.splitext(os.path.basename(f))[0]); pref[m['prefix']] += 1
    if T not in (m['a'], m['b']): continue
    d = json.load(open(f, encoding='utf-8'))
    side = [t['teamId'] for t in d['teams'] if t['roundsWon'] == (int(m['sa']) if m['a'] == T else int(m['sb']))][0]
    for r in d['roundResults']:
        att = (r['winningTeam'] == side) == (r['winningTeamRole'] == 'Attacker')
        won = r['winningTeam'] == side
        i = 0 if att else 2
        bymap[m['map']][i] += won; bymap[m['map']][i+1] += 1
        if att:
            if r['plantSite']: sites[r['plantSite']] += 1; siteswon[r['plantSite']] += won
            else: noplant[0] += won; noplant[1] += 1
print('prefixes', pref)
print(T, 'map: attack won/total | defense won/total')
for k, v in bymap.items(): print(f'  {k:8s} ATK {v[0]}/{v[1]} ({100*v[0]/v[1]:.0f}%)  DEF {v[2]}/{v[3]} ({100*v[2]/v[3]:.0f}%)')
print('  attack rounds with plant by site', {s: f'{siteswon[s]}/{n}' for s, n in sites.items()}, ' no plant won/total', noplant)
