"""Audit all catalog locations; write ranked evidence and remaining coverage gaps.

No runtime data is changed. Nearby source differences and placement distances
are candidates, not proof of absent game content. Source rules require review.
"""
import collections
import csv
import datetime
import json
import math
import pathlib
import re

BASE = pathlib.Path(__file__).resolve().parents[1]
ROOT = BASE / 'src/main/resources/atlasofgielinor'


def read_table(name):
    with (ROOT / 'catalog' / (name + '.tsv')).open(encoding='utf-8', newline='') as stream:
        return list(csv.DictReader(stream, delimiter='\t'))


def clean(text):
    return text.replace('\\n', '\n').replace('<br>', '\n')


def point_distance(location, source):
    return math.hypot(int(location['x']) - source['x'], int(location['y']) - source['y'])


def main():
    pois = {row['id']: row for row in read_table('pois')}
    locations = read_table('locations')
    snapshot = json.loads((ROOT / 'poi/runelite-world-map-data.json').read_text(encoding='utf-8'))
    facts = json.loads((BASE / 'scripts/poi-audit-facts.json').read_text(encoding='utf-8'))
    source_url = ('https://github.com/runelite/runelite/blob/' + snapshot['sourceRevision']
                  + '/runelite-client/src/main/java/net/runelite/client/plugins/worldmap/')
    groups = collections.defaultdict(list)
    creatures = collections.defaultdict(list)
    creature_levels = {}
    for source in snapshot['rows']:
        groups[(source['key'], source['plane'])].append(source)
        if source['dataset'] == 'HunterAreaLocation':
            for name, level in re.findall(r'([^\n()]+?)\s*\((\d+)\)', clean(source['tooltip'])):
                name = name.strip().casefold()
                creatures[name].append(source)
                creature_levels[name] = int(level)
    aliases = {name: name for name in creatures}
    aliases.update({'grey chinchompa': 'chinchompa', 'gray chinchompa': 'chinchompa',
                    'red chinchompa': 'carnivorous chinchompa',
                    'black warlock butterfly': 'black warlock'})
    # Longest alternatives prevent generic 'chinchompa' matching a coloured species.
    pattern = re.compile(r'(?<!\w)(' + '|'.join(re.escape(name) for name in
                                              sorted(aliases, key=len, reverse=True))
                         + r')(?:s|es)?(?!\w)', re.I)

    def names(text):
        found = set()
        for match in pattern.finditer(text):
            name = aliases[match.group(1).casefold()]
            # Bare 'Chinchompas (63)' means red, not the grey level-53 creature.
            level = re.match(r'\s*\((?:Level\s+)?(\d+)\)', text[match.end():], re.I)
            if name == 'chinchompa' and level:
                name = {63: 'carnivorous chinchompa', 73: 'black chinchompa'}.get(int(level.group(1)), name)
            found.add(name)
        return found

    resource_vocab = collections.defaultdict(set)

    def fish_labels(tooltip):
        return ', '.join(name for label in re.split(r'[/\n]', clean(tooltip))
                         for name in facts['fishing_families'].get(label.strip(), [label.strip()]))

    for source in snapshot['rows']:
        tooltip = clean(source['tooltip'])
        if source['key'] == 'mining_site':
            resource_vocab['mining_site'].update(name.strip().casefold() for name in
                                                re.findall(r'\d+ ([^\n]+)', tooltip))
        elif source['key'] == 'fishing_spot':
            resource_vocab['fishing_spot'].update(part.strip().casefold() for part in
                                                 re.split(r'[,\n]', fish_labels(tooltip)))
        elif source['key'] == 'rare_trees':
            resource_vocab['rare_trees'].add(re.split(r' - (?:Level|lvl) ', tooltip, flags=re.I)[0].casefold())
    resource_patterns = {key: re.compile(r'(?<!\w)(' + '|'.join(re.escape(name) for name in
                                      sorted(vocab, key=len, reverse=True) if name)
                                      + r')(?:s|es)?(?!\w)', re.I)
                         for key, vocab in resource_vocab.items()}

    def resources(key, text):
        text = re.sub(r'\badamant\b', 'adamantite', text, flags=re.I)
        text = re.sub(r'\bshrimps\b', 'shrimp', text, flags=re.I)
        return {match.group(1).casefold() for match in resource_patterns[key].finditer(text)}

    findings = []
    coverage = collections.Counter()
    for location in locations:
        poi = pois[location['poi_id']]
        details = clean(poi['details'])
        text = poi['detail_title'] + '\n' + details
        source_ref = poi['source_ref']
        curated = 'curated-details.tsv:' in source_ref
        coverage[location['icon_key']] += 1

        def add(check, confidence, claim, expected, evidence, url):
            findings.append(dict(location_id=location['id'], poi_id=poi['id'],
                                 icon_key=location['icon_key'], title=poi['detail_title'],
                                 x=int(location['x']), y=int(location['y']), plane=int(location['plane']),
                                 rendered=location['rendered'] == 'true',
                                 searchable=location['searchable'] == 'true', check=check,
                                 confidence=confidence, claim=claim, expected=expected,
                                 evidence=evidence, source_url=url, inherited_source=source_ref))

        for rule in facts['rules']:
            if location['icon_key'] != rule['key']:
                continue
            if re.search(rule['subject_regex'], text, re.I):
                match = re.search(rule['claim_regex'], text, re.I)
                if match:
                    add(rule['id'], 'sourced_conflict', match.group(), rule['expected'],
                        'Reviewed factual rule on ' + facts['reviewed_on'], rule['source_url'])

        candidates = groups[(location['icon_key'], int(location['plane']))]
        nearest = min(candidates, key=lambda source: point_distance(location, source), default=None)
        distance = point_distance(location, nearest) if nearest else None
        native_url = source_url + nearest['dataset'] + '.java' if nearest else ''
        native_id = nearest['dataset'] + '.' + nearest['constant'] if nearest else ''
        if curated and (distance is None or distance >= 12):
            add('authored_location_without_nearby_reference', 'coverage_gap', poi['detail_title'],
                'Supply independent evidence for this authored marker.',
                (native_id + '; distance=' + str(round(distance, 2)) if nearest else
                 'No same-type/same-plane points in this snapshot.'), native_url)

        if location['icon_key'] == 'hunter_training':
            target = next((line[8:] for line in details.splitlines() if line.startswith('Target: ')), '')
            claimed = names(target) if target else names(poi['detail_title'])
            if claimed and nearest and distance <= 3:
                # Treat a small area of adjacent reference markers as one comparison set.
                nearby = [source for source in candidates if point_distance(location, source) <= 3]
                known = set().union(*(names(source['tooltip']) for source in nearby))
                extra = claimed - known
                if extra:
                    add('hunter_creatures_not_in_nearby_reference', 'identity_conflict',
                        ', '.join(sorted(extra)), ', '.join(sorted(known)),
                        '; '.join(source['constant'] + ': ' + clean(source['tooltip']).replace('\n', ', ')
                                  for source in nearby), source_url + 'HunterAreaLocation.java')
            for name in sorted(claimed):
                same_plane = [source for source in creatures[name]
                              if source['plane'] == int(location['plane'])]
                closest = min(same_plane, key=lambda source: point_distance(location, source), default=None)
                if closest and point_distance(location, closest) > 64:
                    add('hunter_creature_far_from_known_area', 'placement_candidate', name,
                        'Review against ' + closest['constant'],
                        'Closest known area at (%s,%s,%s), %.2f tiles away; snapshot is not exhaustive.'
                        % (closest['x'], closest['y'], closest['plane'], point_distance(location, closest)),
                        source_url + 'HunterAreaLocation.java')
            if len(claimed) == 1:
                requirement = re.search(r'^Level requirement: Level (\d+) Hunter\b', details, re.M)
                name = next(iter(claimed))
                if requirement and int(requirement.group(1)) != creature_levels[name]:
                    add('hunter_single_creature_level_disagrees', 'identity_conflict', requirement.group(),
                        '%s requires Hunter %s' % (name, creature_levels[name]),
                        'RuneLite HunterCreature enum', source_url + 'HunterAreaLocation.java')
        elif nearest and distance <= 3 and nearest['level'] and nearest['skill']:
            # Compare only when the named source subject is also present in the title.
            subject = re.split(r' - (?:Level|lvl) ', nearest['tooltip'], flags=re.I)[0].casefold()
            if subject in poi['detail_title'].casefold():
                requirement = re.search(r'(?:Level requirement: Level|Requires Level) (\d+) '
                                        + re.escape(nearest['skill']) + r'\b', details, re.I)
                if requirement and int(requirement.group(1)) != nearest['level']:
                    add('named_subject_skill_level_disagrees', 'identity_conflict', requirement.group(),
                        str(nearest['level']) + ' ' + nearest['skill'], native_id, native_url)

        # Semantic claims apply even when migration omitted the curated source line.
        # Hidden duplicates can retain the same authored text without that metadata.
        if nearest and distance <= 3 and location['icon_key'] in resource_patterns:
            key = location['icon_key']
            prefixes = {'mining_site': ('Ores available:', 'Ores:', 'Rocks:'),
                        'fishing_spot': ('Method / Fish:', 'Fish:', 'Catch:'),
                        'rare_trees': ('Trees:', 'Tree:')}
            authored = next((line.split(':', 1)[1] for line in details.splitlines()
                             if line.startswith(prefixes[key])), poi['detail_title'])
            claimed = resources(key, authored)
            nearby = [source for source in candidates if point_distance(location, source) <= 3]
            known = set().union(*(resources(key, fish_labels(source['tooltip']) if key == 'fishing_spot'
                                           else source['tooltip']) for source in nearby))
            if claimed and known and claimed.isdisjoint(known):
                add('resource_identity_disjoint_from_nearby_reference', 'identity_conflict',
                    ', '.join(sorted(claimed)), ', '.join(sorted(known)),
                    '; '.join(source['constant'] + ': ' + clean(source['tooltip']).replace('\n', ', ')
                              for source in nearby) + ('; full catches from ' + facts['fishing_families_source']
                                                     if key == 'fishing_spot' else ''), native_url)

    rank = {'sourced_conflict': 0, 'identity_conflict': 1, 'placement_candidate': 2, 'coverage_gap': 3}
    findings.sort(key=lambda row: (rank[row['confidence']], not row['rendered'], row['location_id'], row['check']))
    summary = dict(locations_scanned=len(locations), identities_scanned=len(pois),
                   snapshot_revision=snapshot['sourceRevision'],
                   findings_by_confidence=dict(collections.Counter(row['confidence'] for row in findings)),
                   findings_by_check=dict(collections.Counter(row['check'] for row in findings)),
                   visible_locations_with_strong_findings=len({row['location_id'] for row in findings
                       if row['rendered'] and rank[row['confidence']] <= 1}),
                   locations_with_findings=len({row['location_id'] for row in findings}),
                   category_coverage=dict(sorted(coverage.items())))
    (BASE / 'docs/poi-factual-audit.json').write_text(json.dumps(dict(summary=summary, findings=findings),
                                                             indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
    report = ['# POI factual audit', '', 'Generated ' + datetime.datetime.now(datetime.timezone.utc).isoformat(),
              '', 'Scanned **%s locations** across **%s categories**.' % (len(locations), len(coverage)),
              '', '## Results', '', '| Classification | Findings |', '| --- | ---: |']
    report += ['| %s | %s |' % (key, value) for key, value in summary['findings_by_confidence'].items()]
    report += ['', 'Counts are findings, not unique incorrect POIs. Several findings can describe one location.',
               '', '**%s rendered locations** have sourced or identity conflicts.'
               % summary['visible_locations_with_strong_findings'],
               '', '## Strongest findings', '']
    for row in findings:
        if rank[row['confidence']] > 1:
            continue
        report += ['### ' + row['location_id'] + ' — ' + row['title'], '',
                   '- Check: `' + row['check'] + '` (' + row['confidence'] + ')',
                   '- Stored claim: ' + row['claim'], '- Comparison: ' + row['expected'],
                   '- Evidence: ' + row['evidence'], '- Source: ' + row['source_url'], '']
    report += ['## How to continue the audit', '',
               'The JSON retains every finding, reference and coordinate. Review sourced and identity conflicts first; '
               'then check placement candidates against Wiki or cache evidence. Coverage gaps are the final queue.', '',
               'For an assistant audit pass:', '',
               '1. Run the script and read the strongest findings in this report.',
               '2. Group repeated findings by authored title and claim, including hidden duplicates without curated source lines.',
               '3. Check each claim against the cited source and current Wiki/Jagex or cache evidence. '
               'Record confirmed, dismissed, or unresolved with a reason and source in a dated review document.',
               '4. Expand the reference rules for new error classes, rerun, and retain the remaining candidates. '
               'Do not treat an empty finding list as proof of correctness.', '',
               'Extend scripts/poi-audit-facts.json with reviewed, cited factual rules and rerun '
               '`python scripts/audit-poi-facts.py`. Broad match rules must be reviewed for scope before acceptance.', '',
               '## Limits', '',
               'The bundled reference is a pinned RuneLite snapshot, not a complete current game database. '
               'A creature absent from one nearby area can exist elsewhere nearby. Placement distances refer to area icons, '
               'not NPC spawns. Identity conflicts require review before changing runtime data.', '',
               'Fishing comparisons expand short map labels using RuneLite FishingSpot catch families. '
               'This avoids calling a valid trout or swordfish description wrong because its map label says salmon or lobster.', '',
               'Hunter identity/location/level claims, disjoint mining/fishing/tree resource identities, '
               'and named non-Hunter skill requirements receive semantic checks. '
               'Other categories receive provenance coverage screening; their methods, rewards and regions still require '
               'independent evidence. Unflagged records have not been certified correct. No catalog data was changed.', '']
    (BASE / 'docs/poi-factual-audit.md').write_text('\n'.join(report), encoding='utf-8')
    print(json.dumps(summary, indent=2))


if __name__ == '__main__':
    main()
