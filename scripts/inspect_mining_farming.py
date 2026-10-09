import csv

pois = {}
with open('src/main/resources/atlasofgielinor/catalog/pois.tsv', 'r', encoding='utf-8') as f:
    for row in csv.DictReader(f, delimiter='\t'):
        pois[row['id']] = row

locs = {}
with open('src/main/resources/atlasofgielinor/catalog/locations.tsv', 'r', encoding='utf-8') as f:
    for row in csv.DictReader(f, delimiter='\t'):
        locs[row['id']] = row

print(f"Total range 5811-5845: {5845 - 5811 + 1} items")
for i in range(5811, 5846):
    pid = f"poi_{i:06d}"
    p = pois.get(pid)
    l = locs.get(pid)
    if not p:
        print(f"{pid}: MISSING POI")
        continue
    x = l['x'] if l else 'NO_LOC'
    y = l['y'] if l else ''
    plane = l['plane'] if l else ''
    rend = l['rendered'] if l else ''
    print(f"{pid} | {p['category']} | {p['name']} | ({x}, {y}, {plane}) rend={rend} | {p['detail_title']} | {repr(p.get('details', ''))}")
