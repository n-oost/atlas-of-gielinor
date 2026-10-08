"""Check audit candidates against independently fetched RuneLite Java definitions.

Resolve master to an immutable SHA, verify both that revision and the bundled
revision, and preserve evidence. Does not change the catalog or certify spawns.
"""
import concurrent.futures
import datetime
import hashlib
import json
import pathlib
import re
import urllib.request

BASE = pathlib.Path(__file__).resolve().parents[1]
RAW = 'https://raw.githubusercontent.com/runelite/runelite/'
JAVA = '/runelite-client/src/main/java/'
PATHS = {name: 'net/runelite/client/plugins/worldmap/' + name + '.java'
         for name in ('HunterAreaLocation', 'FishingSpotLocation', 'RareTreeLocation')}
PATHS['FishingSpot'] = 'net/runelite/client/game/FishingSpot.java'


def fetch(url):
    request = urllib.request.Request(url, headers={'User-Agent': 'Atlas-POI-source-verification'})
    with urllib.request.urlopen(request, timeout=30) as response:
        return response.read()


def blocks(text):
    matches = list(re.finditer(r'^\t([A-Z][A-Z0-9_]*)\(', text, re.M))
    for i, match in enumerate(matches):
        end = matches[i + 1].start() if i + 1 < len(matches) else text.find('\n\t;', match.start())
        if end < 0:
            raise ValueError('Cannot find enum end: ' + match.group(1))
        yield match.group(1), text[match.start():end], text[:match.start()].count('\n') + 1


def parse_sources(sources):
    fish = {}
    for constant, body, _ in blocks(sources['FishingSpot']):
        strings = re.findall(r'"([^"\n]*)"', body)
        fish[constant] = [name.strip().casefold() for name in strings[0].split(',')]
    hunter = {constant: (name.casefold(), int(level)) for constant, name, level in
              re.findall(r'^\t\t([A-Z][A-Z0-9_]*)\("([^"]+)", (\d+)\)',
                         sources['HunterAreaLocation'], re.M)}
    result = []
    for dataset in ('HunterAreaLocation', 'FishingSpotLocation', 'RareTreeLocation'):
        for constant, body, line in blocks(sources[dataset]):
            if dataset == 'HunterAreaLocation':
                entries = [hunter[name] for name in re.findall(r'HunterCreature\.([A-Z0-9_]+)', body)]
                subjects = [name for name, _ in entries]
            elif dataset == 'FishingSpotLocation':
                subjects = sorted({name for category in re.findall(r'FishingSpot\.([A-Z0-9_]+)', body)
                                   for name in fish[category]})
                entries = []
            else:
                match = re.search(r'\("([^"]+)", (\d+)', body)
                if not match:
                    raise ValueError('Unrecognized tree enum: ' + constant)
                subjects = [match.group(1).casefold()]
                entries = [(subjects[0], int(match.group(2)))]
            points = re.findall(r'new WorldPoint\((\d+), (\d+), (\d+)\)', body)
            if not points or not subjects:
                raise ValueError('Empty source definition: ' + dataset + '.' + constant)
            for x, y, plane in points:
                result.append(dict(dataset=dataset, constant=constant, x=int(x), y=int(y),
                                   plane=int(plane), subjects=subjects, levels=dict(entries), line=line))
    return result


def main():
    audit = json.loads((BASE / 'docs/poi-factual-audit.json').read_text(encoding='utf-8'))
    pinned = audit['summary']['snapshot_revision']
    latest = json.loads(fetch('https://api.github.com/repos/runelite/runelite/commits/master'))
    current = latest['sha']
    revisions = list(dict.fromkeys((pinned, current)))
    requests = [(revision, name, RAW + revision + JAVA + path)
                for revision in revisions for name, path in PATHS.items()]
    sources = {revision: {} for revision in revisions}
    manifest = []
    with concurrent.futures.ThreadPoolExecutor(max_workers=4) as executor:
        payloads = list(executor.map(lambda request: fetch(request[2]), requests))
    for (revision, name, url), payload in zip(requests, payloads):
        sources[revision][name] = payload.decode('utf-8')
        manifest.append(dict(revision=revision, name=name, url=url,
                             sha256=hashlib.sha256(payload).hexdigest()))
    references = {revision: parse_sources(source) for revision, source in sources.items()}
    datasets = {'hunter_training': 'HunterAreaLocation', 'fishing_spot': 'FishingSpotLocation',
                'rare_trees': 'RareTreeLocation'}
    output = []
    for finding in audit['findings']:
        if finding['confidence'] != 'identity_conflict':
            continue
        dataset = datasets.get(finding['icon_key'])
        if not dataset or finding['check'] not in {
                'hunter_creatures_not_in_nearby_reference', 'resource_identity_disjoint_from_nearby_reference'}:
            output.append(dict(**finding, verification='unresolved', reason='Verifier lacks this check.'))
            continue
        claims = set(finding['claim'].casefold().split(', '))
        evidence = []
        mismatches = []
        for revision, rows in references.items():
            nearby = [row for row in rows if row['dataset'] == dataset and row['plane'] == finding['plane']
                      and (row['x'] - finding['x'])**2 + (row['y'] - finding['y'])**2 <= 9]
            known = {subject for row in nearby for subject in row['subjects']}
            mismatch = bool(known) and (bool(claims - known) if dataset == 'HunterAreaLocation'
                                       else claims.isdisjoint(known))
            mismatches.append(mismatch)
            evidence.append(dict(revision=revision, claimed=sorted(claims), nearby_subjects=sorted(known),
                                 references=[dict(**row, url='https://github.com/runelite/runelite/blob/'
                                                  + revision + JAVA + PATHS[dataset] + '#L' + str(row['line']))
                                             for row in nearby]))
        status = 'confirmed_reference_conflict' if all(mismatches) else 'unresolved'
        output.append(dict(**finding, verification=status, independently_fetched_evidence=evidence))
    # Reviewed factual rules are kept distinct from independently parsed source comparisons.
    # Their evidence is verified by the assistant, not by scraping arbitrary pages here.
    reviewed = [finding for finding in audit['findings'] if finding['confidence'] == 'sourced_conflict']
    summary = dict(pinned_revision=pinned, current_revision=current,
                   current_commit_date=latest['commit']['committer']['date'],
                   source_comparisons=len(output),
                   confirmed_reference_conflicts=sum(row['verification'] == 'confirmed_reference_conflict'
                                                      for row in output),
                   unresolved_source_comparisons=sum(row['verification'] == 'unresolved' for row in output),
                   factual_rules_requiring_assistant_review=len(reviewed),
                   placement_candidates_retained=sum(finding['confidence'] == 'placement_candidate'
                                                      for finding in audit['findings']))
    evidence_path = BASE / 'docs/poi-audit-verification.json'
    evidence_path.write_text(json.dumps(dict(generated_at=datetime.datetime.now(datetime.timezone.utc).isoformat(),
                                             summary=summary, sources=manifest, findings=output,
                                             reviewed_rule_candidates=reviewed), indent=2,
                                       ensure_ascii=False) + '\n', encoding='utf-8')
    report = ['# POI source verification', '',
              'Independent source files fetched at the bundled revision and current RuneLite master, resolved to SHA.',
              '', '- Pinned revision: `' + pinned + '`', '- Current revision: `' + current + '`',
              '- Current commit date: ' + summary['current_commit_date'],
              '- Confirmed reference conflicts: ' + str(summary['confirmed_reference_conflicts']),
              '- Unresolved source comparisons: ' + str(summary['unresolved_source_comparisons']), '',
              'These confirm catalog descriptions disagree with upstream definitions at the corresponding map '
              'location. They do not verify live NPC spawn tiles or prove an incomplete reference lists every nearby creature.',
              '', '## Findings', '', '| Location | Stored claim | Current source subjects | Result |',
              '| --- | --- | --- | --- |']
    for row in output:
        current_evidence = row.get('independently_fetched_evidence', [{}])[-1]
        report.append('| %s | %s | %s | %s |' % (row['location_id'], row['claim'],
                      ', '.join(current_evidence.get('nearby_subjects', [])), row['verification']))
    report += ['', 'Full coordinates, source line links and SHA-256 hashes are in `poi-audit-verification.json`.', '',
               'The %s factual-rule candidates and placement/coverage queues require separate assistant review; '
               % len(reviewed) +
               'this verifier deliberately does not promote them from absence or distance alone.', '']
    (BASE / 'docs/poi-audit-verification.md').write_text('\n'.join(report), encoding='utf-8')
    print(json.dumps(summary, indent=2))


if __name__ == '__main__':
    main()
