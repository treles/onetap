import json, sys, glob, os, collections as C
files = sorted(glob.glob(os.path.join(sys.argv[1], '**', '*.json'), recursive=True))
mm = C.Counter(); tot = 0; dt = C.Counter()
for f in files:
    d = json.load(open(f, encoding='utf-8'))
    pl = [p for p in d['players'] if not p.get('isObserver')]
    team = {p['puuid']: p['teamId'] for p in pl}
    ev = C.Counter()
    for r in d['roundResults']:
        for ps in r['playerStats']:
            for k in ps['kills']:
                if k['killer'] == k['victim']:
                    dt[(k['finishingDamage'] or {}).get('damageType')] += 1; continue
                if team.get(k['killer']) == team.get(k['victim']): continue
                ev[k['killer']] += 1
    for p in pl:
        tot += 1; mm[ev[p['puuid']] - p['stats']['kills']] += 1
print('diff distribution after removing self/team kills:', sorted(mm.items()), 'total', tot)
print('self-kill damage types', dt)
