with open('src/main/resources/atlasofgielinor/catalog/pois.tsv', 'r', encoding='utf-8') as f:
    pois = {line.strip('\r\n').split('\t')[0]: line.strip('\r\n').split('\t') for line in f if line.strip()}

with open('src/main/resources/atlasofgielinor/catalog/locations.tsv', 'r', encoding='utf-8') as f:
    locs = {line.strip('\r\n').split('\t')[0]: line.strip('\r\n').split('\t') for line in f if line.strip()}

mismatches = []
for pid, p in pois.items():
    loc = locs.get(pid)
    if not loc: continue
    rendered = loc[7] == 'true'
    name = p[1]
    title = p[4] if len(p) > 4 else ''
    cat = p[5] if len(p) > 5 else ''
    desc = p[6] if len(p) > 6 else ''
    
    if 'Target:' in desc:
        target_line = [l for l in desc.split('\\n') if l.startswith('Target:')][0]
        name_clean = name.split('(')[0].strip().lower()
        # ignore if generic name like "hunter training" or "hunter area"
        if 'hunter' in name_clean:
            continue
        words = [w for w in name_clean.split() if len(w) > 3]
        if words and not any(w in target_line.lower() for w in words):
            mismatches.append((pid, rendered, name, target_line, p[7] if len(p) > 7 else ''))

print(f"Target line vs Name mismatches: {len(mismatches)}")
for m in mismatches:
    print(f"{m[0]} (rendered={m[1]}): Name='{m[2]}' vs '{m[3]}' | status={m[4]}")
