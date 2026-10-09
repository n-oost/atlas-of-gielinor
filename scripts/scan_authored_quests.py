import csv

quest_details = {}
with open('src/main/resources/atlasofgielinor/poi/quest-details.tsv', 'r', encoding='utf-8') as f:
    for line in f:
        line = line.strip()
        if not line or line.startswith('#'):
            continue
        parts = line.split('\t')
        if len(parts) >= 6:
            name = parts[1].strip()
            quest_details[name] = {
                'id': parts[0].strip(),
                'name': name,
                'category': parts[2].strip(),
                'start': parts[3].strip(),
                'difficulty_length': parts[4].strip(),
                'reward': parts[5].strip(),
                'requirements': parts[6].strip() if len(parts) > 6 else ''
            }

authored_quests = []
with open('src/main/resources/atlasofgielinor/catalog/pois.tsv', 'r', encoding='utf-8') as f:
    for row in csv.DictReader(f, delimiter='\t'):
        pid = row['id']
        if pid.startswith('poi_0058') and 'Quest' in row['category']:
            authored_quests.append(row)

print(f"Total authored quests in range poi_005800-poi_005896: {len(authored_quests)}")
mismatches = []
for q in authored_quests:
    qname = q['name']
    ref = quest_details.get(qname)
    print(f"\n=== {q['id']}: {qname} (Ref found: {ref is not None}) ===")
    print("Catalog:", repr(q['details']))
    if ref:
        diff_len = ref.get('difficulty_length')
        reward = ref.get('reward')
        reqs = ref.get('requirements')
        print(f"Ref details: {diff_len} | {reward} | {reqs}")
        cat_det = q['details']
        issues = []
        if diff_len and diff_len not in cat_det:
            issues.append(f"Difficulty/Length mismatch: expected '{diff_len}'")
        # Check reward QP
        import re
        qp_match = re.search(r'(\d+)\s+Quest Points?', reward)
        if qp_match:
            qp_str = qp_match.group(0)
            if qp_str not in cat_det:
                issues.append(f"Reward mismatch: expected '{qp_str}'")
        if issues:
            print("  >>> ISSUES:", issues)
            mismatches.append((q['id'], qname, issues, cat_det, ref))
    else:
        print("  >>> NO REFERENCE ENTRY in quest-details.tsv!")

print(f"\nTotal quests with discrepancies: {len(mismatches)}")
