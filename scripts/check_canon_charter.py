import csv

locs = {}
with open('src/main/resources/atlasofgielinor/catalog/locations.tsv', 'r', encoding='utf-8') as f:
    for row in csv.DictReader(f, delimiter='\t'):
        locs[row['id']] = row

pois = {}
with open('src/main/resources/atlasofgielinor/catalog/pois.tsv', 'r', encoding='utf-8') as f:
    for row in csv.DictReader(f, delimiter='\t'):
        pois[row['id']] = row

canon_ids = ['poi_004247', 'poi_004250', 'poi_004261', 'poi_004263', 'poi_004270', 'poi_004271', 'poi_004273', 'poi_004308', 'poi_004311', 'poi_004312']
for cid in canon_ids:
    l = locs.get(cid, {})
    p = pois.get(cid, {})
    print(f"{cid}: name=\"{p.get('name')}\" title=\"{p.get('detail_title')}\" loc=({l.get('x')}, {l.get('y')}, {l.get('plane')}) rend={l.get('rendered')} srch={l.get('searchable')} det={repr(p.get('details'))}")
