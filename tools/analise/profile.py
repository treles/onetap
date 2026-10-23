import json, sys, os, glob, re, collections as C

root = sys.argv[1]
files = sorted(glob.glob(os.path.join(root, '**', '*.json'), recursive=True))
FN = re.compile(r'^(?P<prefix>.+?)_(?P<num>\d+[a-z]?)_(?P<date>\d\d-\d\d-\d\d)_(?P<a>.+)_(?P<sa>\d+)_x_(?P<sb>\d+)_(?P<b>.+)_(?P<map>[a-z]+)$')

stats = C.Counter()
folders = C.Counter()
maps = C.Counter(); mapids = C.Counter()
match_ids = C.Counter()
result_codes = C.Counter(); ceremonies = C.Counter(); round_results = C.Counter()
chars = C.Counter(); weapons_kill = C.Counter(); weapons_buy = C.Counter(); armors = C.Counter()
damage_types = C.Counter()
versions = C.Counter(); flows = C.Counter(); modes = C.Counter()
players_per_match = C.Counter(); coaches_per_match = C.Counter(); observers = 0
rounds_per_match = []
team_names = C.Counter()
team_puuids = C.defaultdict(set)
puuid_matches = C.Counter()
fn_fail = []
side_map_fail = []
mismatch_kills = 0; mismatch_total = 0
null_econ = 0; econ_total = 0
dmg_sample = None
plant_sites = C.Counter()
null_fields = C.Counter()
total_rounds = 0; total_kill_events = 0
team_tournaments = C.defaultdict(set)
dup_names = C.Counter()

for f in files:
    folder = os.path.relpath(os.path.dirname(f), root)
    folders[folder] += 1
    base = os.path.splitext(os.path.basename(f))[0]
    m = FN.match(base)
    d = json.load(open(f, encoding='utf-8'))
    mi = d['matchInfo']
    match_ids[mi['matchId']] += 1
    mapids[mi['mapId']] += 1
    versions[mi['gameVersion'].split('-')[1]] += 1
    flows[mi['provisioningFlowId']] += 1
    modes[mi['gameMode'].split('/')[-1]] += 1
    if not mi.get('isCompleted'): stats['not_completed'] += 1
    players = [p for p in d['players']]
    obs = [p for p in players if p.get('isObserver')]
    observers += len(obs)
    players_per_match[len(players)] += 1
    coaches_per_match[len(d.get('coaches') or [])] += 1
    rr = d['roundResults']
    rounds_per_match.append(len(rr))
    total_rounds += len(rr)
    teams = {t['teamId']: t for t in d['teams']}
    # filename -> team names
    if not m:
        fn_fail.append(base)
    else:
        maps[m['map']] += 1
        sa, sb = int(m['sa']), int(m['sb'])
        name_by_side = {}
        for side, t in teams.items():
            if t['roundsWon'] == sa and sa != sb: name_by_side[side] = m['a']
            elif t['roundsWon'] == sb and sa != sb: name_by_side[side] = m['b']
        if len(set(name_by_side.values())) != 2:
            side_map_fail.append((base, {k: v['roundsWon'] for k, v in teams.items()}))
        for p in players:
            n = name_by_side.get(p['teamId'])
            if n:
                team_puuids[n].add(p['puuid'])
        for n in (m['a'], m['b']):
            team_names[n] += 1
            team_tournaments[n].add(folder.split(os.sep)[0])
        dup_names[(m['date'], m['a'], m['b'], m['map'], m['sa'], m['sb'])] += 1
    for p in players:
        puuid_matches[p['puuid']] += 1
        chars[p['characterId']] += 1
        for k, v in p.items():
            if v is None: null_fields['player.' + k] += 1
    # kills consistency
    ev_kills = C.Counter()
    for r in rr:
        result_codes[r.get('roundResultCode')] += 1
        ceremonies[r.get('roundCeremony')] += 1
        round_results[r.get('roundResult')] += 1
        if r.get('plantSite'): plant_sites[r['plantSite']] += 1
        for k, v in r.items():
            if v is None: null_fields['round.' + k] += 1
        for ps in r['playerStats']:
            econ_total += 1
            e = ps.get('economy')
            if not e or e.get('weapon') in (None, ''):
                null_econ += 1
            if e:
                weapons_buy[e.get('weapon')] += 1
                armors[e.get('armor')] += 1
            for k in ps['kills']:
                total_kill_events += 1
                ev_kills[k['killer']] += 1
                fd = k.get('finishingDamage') or {}
                damage_types[fd.get('damageType')] += 1
                weapons_kill[fd.get('damageItem')] += 1
            if ps['damage'] and dmg_sample is None:
                dmg_sample = ps['damage'][0]
    for p in players:
        mismatch_total += 1
        if p.get('stats') is None:
            stats['player_stats_null'] += 1; stats['null_stats_observer_'+str(p.get('isObserver'))] += 1; continue
        if ev_kills[p['puuid']] != p['stats']['kills']:
            mismatch_kills += 1

print('FILES', len(files)); print('FOLDERS', dict(folders))
print('DUP matchId', {k: v for k, v in match_ids.items() if v > 1})
print('DUP by filename tuple', {k: v for k, v in dup_names.items() if v > 1})
print('filename parse failures', fn_fail)
print('side mapping failures', side_map_fail[:10], len(side_map_fail))
print('MAPS(filename)', maps.most_common()); print('MAPIDS', mapids.most_common())
print('versions', versions.most_common()); print('flows', flows); print('modes', modes)
print('players/match', players_per_match, 'observers', observers, 'coaches/match', coaches_per_match)
print('rounds total', total_rounds, 'min/avg/max', min(rounds_per_match), sum(rounds_per_match)/len(rounds_per_match), max(rounds_per_match))
print('kill events', total_kill_events, 'player-match kill mismatches', mismatch_kills, '/', mismatch_total)
print('econ rows', econ_total, 'missing weapon/econ', null_econ)
print('roundResultCode', result_codes); print('roundResult', round_results); print('ceremony', ceremonies)
print('plant sites', plant_sites)
print('damage types (kills)', damage_types)
print('distinct agents', len(chars), chars.most_common())
print('distinct kill weapons', len(weapons_kill), weapons_kill.most_common(25))
print('distinct buy weapons', len(weapons_buy)); print('armors', armors.most_common())
print('null fields', null_fields.most_common(30))
print('damage sample', dmg_sample)
print('distinct puuids', len(puuid_matches), 'appear >=5 matches', sum(1 for v in puuid_matches.values() if v >= 5))
print('distinct team names', len(team_names))
for n, c in team_names.most_common():
    print(f'  {n:35s} matches={c:3d} players={len(team_puuids[n]):2d} tourn={sorted(team_tournaments[n])}')
print('misc', stats)
