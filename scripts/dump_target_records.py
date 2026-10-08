import json

with open('docs/poi-runtime-verification-2026-10-08.json', 'r', encoding='utf-8') as f:
    data = json.load(f)

targets = [
    'poi_002661', 'poi_002664', 'poi_002665',
    'poi_002829',
    'poi_002735', 'poi_002738',
    'poi_002841', 'poi_002885',
    'poi_003255', 'poi_003269', 'poi_003274',
    'poi_003026',
    'poi_002649'
]

recs = data.get('records', [])
for r in recs:
    pid = r.get('poi_id')
    if pid in targets:
        print(json.dumps(r, indent=2))
