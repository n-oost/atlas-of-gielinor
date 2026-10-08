import json

with open('docs/poi-continuation-verification-2026-10-08.json', 'r', encoding='utf-8') as f:
    data = json.load(f)

all_settled = [
    'poi_005888', 'poi_005858', 'poi_005862', 'poi_002112', 'poi_007113',
    'poi_005871', 'poi_005872', 'poi_005881', 'poi_005882', 'poi_005883',
    'poi_005894', 'poi_005895', 'poi_005896'
]
data['settled_corrections'] = all_settled

# Also update verification items / details if present
with open('docs/poi-continuation-verification-2026-10-08.json', 'w', encoding='utf-8') as f:
    json.dump(data, f, indent=2)

print('Updated poi-continuation-verification-2026-10-08.json with 13 settled corrections')
