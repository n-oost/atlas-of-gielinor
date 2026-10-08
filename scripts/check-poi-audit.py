"""Exercise audit controls and independent source parsing without changing project data."""
import contextlib
import csv
import importlib.util
import io
import json
import pathlib
import tempfile

SCRIPTS = pathlib.Path(__file__).resolve().parent


def module(name):
    spec = importlib.util.spec_from_file_location(name.replace('-', '_'), SCRIPTS / (name + '.py'))
    loaded = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(loaded)
    return loaded


def main():
    audit = module('audit-poi-facts')
    verifier = module('verify-poi-audit')
    with tempfile.TemporaryDirectory(prefix='atlas-poi-audit-controls-') as temporary:
        base = pathlib.Path(temporary)
        root = base / 'src/main/resources/atlasofgielinor'
        for folder in (root / 'catalog', root / 'poi', base / 'scripts', base / 'docs'):
            folder.mkdir(parents=True, exist_ok=True)
        (base / 'scripts/poi-audit-facts.json').write_bytes((SCRIPTS / 'poi-audit-facts.json').read_bytes())
        sources = [
            dict(dataset='FishingSpotLocation', constant='RIVER', key='fishing_spot', x=100, y=100,
                 plane=0, tooltip='Salmon', level=0, skill=''),
            dict(dataset='FishingSpotLocation', constant='COAST', key='fishing_spot', x=200, y=100,
                 plane=0, tooltip='Lobster', level=0, skill=''),
            dict(dataset='FishingSpotLocation', constant='SQUID', key='fishing_spot', x=300, y=100,
                 plane=0, tooltip='Squid', level=0, skill=''),
            dict(dataset='FishingSpotLocation', constant='SHARK', key='fishing_spot', x=900, y=100,
                 plane=0, tooltip='Shark', level=0, skill=''),
            dict(dataset='HunterAreaLocation', constant='RED', key='hunter_training', x=400, y=100,
                 plane=0, tooltip='Carnivorous chinchompa (63)', level=0, skill=''),
            dict(dataset='HunterAreaLocation', constant='FOX', key='hunter_training', x=500, y=100,
                 plane=0, tooltip='Pyre Fox (57)', level=0, skill=''),
            dict(dataset='HunterAreaLocation', constant='MOTH', key='hunter_training', x=600, y=100,
                 plane=0, tooltip='Sunlight Moth (65)', level=0, skill=''),
            dict(dataset='HunterAreaLocation', constant='HERBIBOAR', key='hunter_training', x=700, y=100,
                 plane=0, tooltip='Herbiboar (80)', level=0, skill=''),
        ]
        controls = [
            ('trout_control', 'fishing_spot', 100, 'Fishing spot', 'Fish: Trout'),
            ('swordfish_control', 'fishing_spot', 200, 'Fishing spot', 'Fish: Swordfish'),
            ('wrong_fish', 'fishing_spot', 300, 'Fishing spot', 'Fish: Shark'),
            ('hidden_wrong_fish', 'fishing_spot', 300, 'Fishing spot', 'Fish: Shark'),
            ('red_chin_control', 'hunter_training', 400, 'Chinchompas (63)',
             'Target: Chinchompas (63)\\nLevel requirement: Level 63 Hunter'),
            ('wrong_fox_level', 'hunter_training', 500, 'Pyre Fox (57)',
             'Target: Pyre fox\\nLevel requirement: Level 53 Hunter'),
            ('moth_control', 'hunter_training', 600, 'Sunlight Moth (65)',
             'Target: Sunlight moth (restores reduced stats excluding Prayer)'),
            ('herbiboar_control', 'hunter_training', 700, 'Herbiboar (80)',
             'Target: Herbiboar\\nLevel requirement: Level 80 Hunter & 31 Herblore\\nCatch method: Tracking'),
            ('birdhouse_control', 'hunter_training', 700, 'Fossil Island - Birdhouses',
             'Catch method: Clockwork Birdhouses'),
            ('wrong_herbiboar', 'hunter_training', 700, 'Fossil Island - Birdhouses',
             'Catch method: Clockwork Birdhouses\\nHerbiboar (80)'),
        ]
        pois = [dict(id=name, detail_title=title, details=details,
                     source_ref='fixture; curated-details.tsv:1') for name, key, x, title, details in controls]
        locations = [dict(id=name, poi_id=name, icon_key=key, x=x, y=100, plane=0,
                          rendered='true', searchable='true') for name, key, x, title, details in controls]
        next(row for row in pois if row['id'] == 'hidden_wrong_fish')['source_ref'] = 'migration snapshot'
        hidden = next(row for row in locations if row['id'] == 'hidden_wrong_fish')
        hidden['rendered'], hidden['searchable'] = 'false', 'false'
        for name, rows in [('pois', pois), ('locations', locations)]:
            with (root / 'catalog' / (name + '.tsv')).open('w', newline='', encoding='utf-8') as stream:
                writer = csv.DictWriter(stream, fieldnames=list(rows[0]), delimiter='\t')
                writer.writeheader()
                writer.writerows(rows)
        (root / 'poi/runelite-world-map-data.json').write_text(json.dumps(dict(sourceRevision='fixture', rows=sources)))
        audit.BASE, audit.ROOT = base, root
        with contextlib.redirect_stdout(io.StringIO()):
            audit.main()
        findings = json.loads((base / 'docs/poi-factual-audit.json').read_text())['findings']
        observed = {(row['location_id'], row['check']) for row in findings}
        expected = {('wrong_fish', 'resource_identity_disjoint_from_nearby_reference'),
                    ('hidden_wrong_fish', 'resource_identity_disjoint_from_nearby_reference'),
                    ('wrong_herbiboar', 'herbiboar_false_birdhouse_description'),
                    ('wrong_fox_level', 'hunter_single_creature_level_disagrees')}
        if observed != expected:
            raise AssertionError('Unexpected controls result: ' + repr(observed))

    parsed = verifier.parse_sources({
        'FishingSpot': '\tSALMON("Salmon, Trout, Pike", "Salmon", ItemID.RAW_SALMON),\n\t;\n',
        'FishingSpotLocation': '\tRIVER(FishingSpot.SALMON, new WorldPoint(100, 100, 0)),\n\t;\n',
        'HunterAreaLocation': '\tFOX(new WorldPoint(500, 100, 0), HunterCreature.PYRE_FOX),\n\t;\n'
                              '\t\tPYRE_FOX("Pyre Fox", 57),\n',
        'RareTreeLocation': '\tMAPLE("Maple tree", 45, new WorldPoint(700, 100, 0)),\n\t;\n',
    })
    fish = next(row for row in parsed if row['dataset'] == 'FishingSpotLocation')
    if fish['subjects'] != ['pike', 'salmon', 'trout'] or len(parsed) != 3:
        raise AssertionError('Independent parser did not preserve full catch families or point count.')
    print('Passed: six valid controls unflagged; visible and hidden wrong fish, wrong Hunter level and birdhouse/herbiboar conflict detected; source parser verified.')


if __name__ == '__main__':
    main()
