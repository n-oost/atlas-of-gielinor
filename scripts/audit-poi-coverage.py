"""Compare the canonical catalog with the bundled RuneLite world-map snapshot.

Writes docs/poi-coverage-audit.tsv; does not change catalog records.
"""
import collections
import csv
import json
import pathlib

base = pathlib.Path(__file__).resolve().parents[1]
root = base / 'src/main/resources/atlasofgielinor'
def read(name):
    with (root / 'catalog' / (name + '.tsv')).open(encoding='utf-8', newline='') as stream:
        return list(csv.DictReader(stream, delimiter='\t'))
locations = read('locations')
pois = {row['id']: row for row in read('pois')}
source = json.loads((root / 'poi/runelite-world-map-data.json').read_text(encoding='utf-8'))['rows']
findings = []
for row in source:
    keys = {row['key']}
    if row['key'] in ('dungeon', 'basement', 'dungeon_link'):
        keys = {'dungeon', 'basement', 'dungeon_link'}
    candidates = [location for location in locations if location['icon_key'] in keys
                  and int(location['plane']) == row['plane']
                  and abs(int(location['x']) - row['x']) <= 6 and abs(int(location['y']) - row['y']) <= 6]
    if not candidates:
        findings.append(['source_without_nearby_icon', row['dataset'] + '.' + row['constant'], row['tooltip'], row['key'],
                         row['x'], row['y'], row['plane'], 'Snapshot icon not within 6 tiles; inspect other labels and tuned positions'])
aliases = {'teleport': 'house_portal', 'runecrafting_altar': 'altar', 'salvaging': 'cache_icon_4947',
           'basement': 'dungeon_link', 'services': 'task_master'}
for location in locations:
    poi = pois[location['poi_id']]
    # PoiIndex generates chest icons for STASH units and thieving chests.
    if location['rendered'] == 'true' and location['icon_key'] not in ('region_label', 'stash_unit', 'thieving'):
        asset = aliases.get(location['icon_key'], location['icon_key'])
        if not (root / 'poi/icons' / (asset + '.png')).exists():
            findings.append(['missing_icon_asset', location['id'], poi['name'], location['icon_key'],
                             location['x'], location['y'], location['plane'], 'Bundled icon absent; on-disk supplementary assets may override'])
    if 'No additional details available' in poi['details']:
        findings.append(['generic_catalog_details', location['id'], poi['name'], location['icon_key'],
                         location['x'], location['y'], location['plane'], 'Runtime shop/travel enrichment and simple labels may make this intentional'])
with (base / 'docs/poi-coverage-audit.tsv').open('w', encoding='utf-8', newline='') as stream:
    writer = csv.writer(stream, delimiter='\t', lineterminator='\n')
    writer.writerow(['finding', 'id', 'name', 'icon_key', 'x', 'y', 'plane', 'note'])
    writer.writerows(findings)
print(dict(collections.Counter(row[0] for row in findings)))
