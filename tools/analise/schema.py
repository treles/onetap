import json, sys, os, glob, collections

root = sys.argv[1]
files = sorted(glob.glob(os.path.join(root, '**', '*.json'), recursive=True))
print('total files', len(files))
by_dir = collections.Counter(os.path.basename(os.path.dirname(f)) for f in files)
print(by_dir)

def shape(o, depth=0, maxd=6, path=''):
    pad = '  ' * depth
    if isinstance(o, dict):
        for k, v in list(o.items())[:80]:
            t = type(v).__name__
            extra = ''
            if isinstance(v, list):
                extra = f' len={len(v)}'
            elif not isinstance(v, dict):
                s = repr(v)
                extra = ' = ' + (s[:70])
            print(f'{pad}{k}: {t}{extra}')
            if depth < maxd and isinstance(v, (dict, list)):
                shape(v, depth + 1, maxd, path + '.' + k)
    elif isinstance(o, list) and o:
        print(f'{pad}[0] of {len(o)}: {type(o[0]).__name__}')
        if isinstance(o[0], (dict, list)):
            shape(o[0], depth + 1, maxd, path + '[0]')

f = files[0] if len(sys.argv) < 3 else files[int(sys.argv[2])]
print('FILE', f, os.path.getsize(f))
with open(f, encoding='utf-8') as fh:
    d = json.load(fh)
shape(d, maxd=int(os.environ.get('MAXD', 4)))
