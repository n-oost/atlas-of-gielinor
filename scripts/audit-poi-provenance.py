"""Inventory inherited curated POIs against the bundled RuneLite snapshot.

Writes a review queue, not a list of proven errors. Does not edit runtime data.
Run from any directory: python scripts/audit-poi-provenance.py
"""
import collections
import csv
import json
import math
import pathlib
import re

BASE = pathlib.Path(__file__).resolve().parents[1]
ROOT = BASE / 'src/main/resources/atlasofgielinor'


def table(name):
    with (ROOT / 'catalog' / (name + '.tsv')).open(encoding='utf-8', newline='') as stream:
        return list(csv.DictReader(stream, delimiter='\t'))


def creature_names(text):
    return {match.strip().casefold() for match in
            re.findall(r'([^,;\n()]+?)\s*\(\d+\)', text.replace('<br>', '\n'))}


def main():
    pois = {row['id']: row for row in table('pois')}
    locations = table('locations')
    snapshot = json.loads((ROOT / 'poi/runelite-world-map-data.json').read_text(encoding='utf-8'))
    groups = collections.defaultdict(list)
    for row in snapshot['rows']:
        groups[(row['key'], row['plane'])].append(row)
    output = []
    for location in locations:
        poi = pois[location['poi_id']]
        if 'curated-details.tsv:' not in poi['source_ref']:
            continue
        candidates = groups[(location['icon_key'], int(location['plane']))]
        x, y = int(location['x']), int(location['y'])
        nearest = min(candidates, key=lambda row: (row['x'] - x)**2 + (row['y'] - y)**2,
                      default=None)
        distance = math.hypot(nearest['x'] - x, nearest['y'] - y) if nearest else None
        details = poi['details'].replace('\\n', '\n')
        targets = next((line[8:] for line in details.splitlines() if line.startswith('Target: ')), '')
        target_names = creature_names(targets)
        native_names = creature_names(nearest['tooltip']) if nearest else set()
        flags = []
        if nearest is None or distance >= 12:
            flags.append('no_same_type_snapshot_within_12_tiles')
        if (location['icon_key'] == 'hunter_training' and distance is not None and distance < 12
                and target_names and native_names and target_names.isdisjoint(native_names)):
            flags.append('hunter_target_list_disjoint_from_snapshot')
        output.append([
            location['id'], location['poi_id'], location['icon_key'], x, y, location['plane'],
            location['rendered'], location['searchable'], poi['status'], poi['detail_title'],
            poi['details'], poi['source_ref'],
            nearest['dataset'] + '.' + nearest['constant'] if nearest else '',
            nearest['tooltip'] if nearest else '', round(distance, 3) if distance is not None else '',
            ';'.join(flags),
        ])
    destination = BASE / 'docs/poi-provenance-review.tsv'
    with destination.open('w', encoding='utf-8', newline='') as stream:
        writer = csv.writer(stream, delimiter='\t', lineterminator='\n')
        writer.writerow(['location_id', 'poi_id', 'icon_key', 'x', 'y', 'plane', 'rendered',
                         'searchable', 'identity_status', 'detail_title', 'details', 'source_ref',
                         'nearest_snapshot_id', 'snapshot_tooltip', 'distance_tiles', 'review_flags'])
        writer.writerows(output)
    print(json.dumps({
        'snapshot_revision': snapshot['sourceRevision'],
        'identities': len(pois),
        'identity_status': dict(collections.Counter(row['status'] for row in pois.values())),
        'locations': len(locations),
        'location_status': dict(collections.Counter(row['status'] for row in locations)),
        'curated_identities': sum('curated-details.tsv:' in row['source_ref'] for row in pois.values()),
        'curated_locations': len(output),
        'curated_rendered_locations': sum(row[6] == 'true' for row in output),
        'review_flag_counts': dict(collections.Counter(flag for row in output
                                                    for flag in row[-1].split(';') if flag)),
        'output': str(destination),
    }, indent=2))


if __name__ == '__main__':
    main()
