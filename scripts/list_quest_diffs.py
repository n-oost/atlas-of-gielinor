import csv, re

quest_details = {}
with open('src/main/resources/atlasofgielinor/poi/quest-details.tsv', 'r', encoding='utf-8') as f:
    for line in f:
        line = line.strip()
        if not line or line.startswith('#'): continue
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

for q in authored_quests:
    qname = q['name']
    ref = quest_details.get(qname)
    if not ref: continue
    
    cat_det = q['details']
    ref_dl = ref['difficulty_length'] # e.g. "Difficulty: Intermediate • Length: Short"
    ref_rew = ref['reward']
    
    # Extract QP from ref
    qp_match = re.search(r'(\d+)\s+Quest Points?', ref_rew)
    ref_qp = qp_match.group(0) if qp_match else None
    
    # Extract current difficulty & length from cat_det
    cat_dl_match = re.search(r'Difficulty:\s*([^•\n]+)•\s*Length:\s*([^\n]+)', cat_det)
    cat_qp_match = re.search(r'Reward:\s*(\d+\s+Quest Points?)', cat_det)
    
    diff_or_len_wrong = False
    ref_dl_clean = ref_dl.replace('•', '').replace('  ', ' ')
    cat_dl_clean = (cat_dl_match.group(0) if cat_dl_match else '').replace('•', '').replace('  ', ' ')
    if ref_dl.split('•')[0].strip() not in cat_det or ref_dl.split('•')[1].strip() not in cat_det:
        diff_or_len_wrong = True
        
    qp_wrong = False
    if ref_qp and ref_qp not in cat_det:
        qp_wrong = True
        
    if diff_or_len_wrong or qp_wrong:
        print(f"ID: {q['id']} | Quest: {qname}")
        print(f"  Current: {cat_det}")
        print(f"  Ref D&L: {ref_dl}")
        print(f"  Ref Rew: {ref_rew}")
        print(f"  Ref Reqs: {ref['requirements']}")
        print(f"  Ref Start: {ref['start']}")
        print()
