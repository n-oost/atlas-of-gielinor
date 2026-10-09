import csv

ALIASES_PATH = 'src/main/resources/atlasofgielinor/catalog/poi_aliases.tsv'

with open(ALIASES_PATH, 'r', encoding='utf-8') as f:
    reader = csv.DictReader(f, delimiter='\t')
    rows = list(reader)

new_rows = []
for r in rows:
    # Remove old corrupt alias
    if r['poi_id'] == 'poi_002829' and 'Crystal Eel' in r['alias']:
        continue
    new_rows.append(r)

# Add new aliases
additions = [
    ('poi_002829', 'Fishing Spot (Shrimp & Sardine)'),
    ('poi_002829', 'Saltfish'),
    ('poi_005772', "Eagle's Peak - Ferrets (Level 27)"),
    ('poi_005772', 'Ferret'),
    ('poi_005807', 'Charred Island - Red Salamanders (Level 59)'),
    ('poi_005807', 'Red salamander'),
    ('poi_005809', "Minotaurs' Rest - Tropical Wagtails (Level 19)"),
    ('poi_005809', 'Tropical wagtail'),
    ('poi_005844', 'Trahaearn Mining Site'),
    ('poi_005844', 'Trahaearn mine'),
    ('poi_005846', 'Captain Barnaby'),
    ('poi_005846', 'Ship to Brimhaven / Rimmington'),
    ('poi_005847', 'Charter Ship (Aldarin)'),
    ('poi_005848', 'Charter Ship (Civitas illa Fortis)'),
    ('poi_005849', 'Charter Ship (Deepfin Point)'),
    ('poi_005850', 'Charter Ship (Port Tyras)'),
    ('poi_005851', 'Charter Ship (Prifddinas)'),
    ('poi_005852', 'Charter Ship (Prifddinas Quest Instance)'),
    ('poi_005853', 'Charter Ship (The Summer Shore)'),
    ('poi_005854', 'Charter Ship (Sunset Coast)')
]

for pid, alias in additions:
    if not any(r['poi_id'] == pid and r['alias'] == alias for r in new_rows):
        new_rows.append({'poi_id': pid, 'alias': alias})

with open(ALIASES_PATH, 'w', encoding='utf-8', newline='') as f:
    writer = csv.DictWriter(f, fieldnames=['poi_id', 'alias'], delimiter='\t', lineterminator='\n')
    writer.writeheader()
    writer.writerows(new_rows)

print(f"Updated {ALIASES_PATH} with {len(additions)} additions.")
