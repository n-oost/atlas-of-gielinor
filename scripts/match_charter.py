import csv, math

locs = []
with open('src/main/resources/atlasofgielinor/catalog/locations.tsv', 'r', encoding='utf-8') as f:
    for row in csv.DictReader(f, delimiter='\t'):
        locs.append(row)

pois = {}
with open('src/main/resources/atlasofgielinor/catalog/pois.tsv', 'r', encoding='utf-8') as f:
    for row in csv.DictReader(f, delimiter='\t'):
        pois[row['id']] = row

targets = [
    ('poi_005846', 2675, 3275),
    ('poi_005847', 1455, 2968),
    ('poi_005848', 1743, 3136),
    ('poi_005849', 1943, 2753),
    ('poi_005850', 2141, 3123),
    ('poi_005851', 2156, 3331),
    ('poi_005852', 3180, 6083),
    ('poi_005853', 3186, 2367),
    ('poi_005854', 1513, 2970),
]

for pid, tx, ty in targets:
    print(f"=== Matches for {pid} at ({tx}, {ty}) ===")
    for l in locs:
        lx, ly, lp = int(l['x']), int(l['y']), int(l['plane'])
        if math.hypot(lx - tx, ly - ty) <= 5:
            p = pois.get(l['poi_id'], {})
            name = p.get('name')
            rend = l['rendered']
            cat = p.get('category')
            det = repr(p.get('details', '')[:50])
            print(f"   {l['id']} / {l['poi_id']} plane={lp} rend={rend} name=\"{name}\" icon={l.get('icon_key')} cat={cat} details={det}")
