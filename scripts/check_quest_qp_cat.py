import csv, re

quest_details = {}
with open('src/main/resources/atlasofgielinor/poi/quest-details.tsv', 'r', encoding='utf-8') as f:
    for line in f:
        line = line.strip()
        if not line or line.startswith('#'): continue
        parts = line.split('\t')
        if len(parts) >= 6:
            quest_details[parts[1].strip()] = {
                'id': parts[0].strip(),
                'category': parts[2].strip(),
                'start': parts[3].strip(),
                'difficulty_length': parts[4].strip(),
                'reward': parts[5].strip(),
                'requirements': parts[6].strip() if len(parts) > 6 else ''
            }

with open('src/main/resources/atlasofgielinor/catalog/pois.tsv', 'r', encoding='utf-8') as f:
    for row in csv.DictReader(f, delimiter='\t'):
        pid = row['id']
        if pid.startswith('poi_0058') and 'Quest' in row['category']:
            qname = row['name']
            ref = quest_details.get(qname)
            if not ref:
                print(f"UNKNOWN QUEST: {pid} {qname}")
                continue
            cat_det = row['details']
            cat_cat = row['category']
            
            # Check QP
            qp_match = re.search(r'(\d+)\s+Quest Points?', ref['reward'])
            expected_qp = qp_match.group(1) if qp_match else None
            
            # Cat QP
            cat_qp_match = re.search(r'Reward:\s*(\d+)\s+Quest Points?', cat_det)
            cat_qp = cat_qp_match.group(1) if cat_qp_match else None
            
            # Check Category
            ref_tier = ref['difficulty_length'].split('•')[0].replace('Difficulty:', '').strip()
            expected_cat = f"Quests • {ref_tier}"
            
            issues = []
            if expected_qp and cat_qp and expected_qp != cat_qp:
                issues.append(f"QP: expected {expected_qp}, got {cat_qp}")
            if cat_cat != expected_cat:
                issues.append(f"Category: expected '{expected_cat}', got '{cat_cat}'")
            if issues:
                print(f"{pid}: {qname} -> {issues}")
