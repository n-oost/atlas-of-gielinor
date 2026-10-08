import json

with open('docs/poi-continuation-fixes-2026-10-08.json', 'r', encoding='utf-8') as f:
    fixes_data = json.load(f)

sailing_fixes = [
    {
        "poi_id": "poi_005808",
        "name": "Anglers' Retreat - Ruby Harvests (Level 15)",
        "canonical_id": "poi_005808",
        "location_id": "poi_005808",
        "category_before": "Skilling • Hunter",
        "category_after": "Skilling • Hunter",
        "description_before": "Target: Sailing-island hunter creatures\\nLevel requirement: Check Hunter skill guide for creature levels\\nCatch method: Traps, snares, or tracking\\nRegion: Anglers' Retreat (Sailing)",
        "description_after": "Target: Ruby harvest butterfly\\nLevel requirement: Level 15 Hunter\\nCatch method: Butterfly net & Butterfly jar (or barehanded at 75)\\nRegion: Anglers' Retreat (Sailing)",
        "evidence": "Live server visit confirmed NPC 5556 Ruby harvest at 2470, 2715.",
        "rationale": "Replaced generic placeholder description with verified butterfly species and requirements."
    },
    {
        "poi_id": "poi_005810",
        "name": "Brittle Isle - Snowy Knights (Level 35)",
        "canonical_id": "poi_005810",
        "location_id": "poi_005810",
        "category_before": "Skilling • Hunter",
        "category_after": "Skilling • Hunter",
        "description_before": "Target: Sailing-island hunter creatures\\nLevel requirement: Check Hunter skill guide for creature levels\\nCatch method: Traps, snares, or tracking\\nRegion: Brittle Isle (Sailing)",
        "description_after": "Target: Snowy knight butterfly\\nLevel requirement: Level 35 Hunter\\nCatch method: Butterfly net & Butterfly jar (or barehanded at 85)\\nRegion: Brittle Isle (Sailing)",
        "evidence": "Live server visit confirmed NPC 5554 Snowy knight at 1956, 4058.",
        "rationale": "Replaced generic placeholder description with verified butterfly species and requirements."
    },
    {
        "poi_id": "poi_005791",
        "name": "Dognose Island - Crimson Swifts (Level 1)",
        "canonical_id": "poi_005791",
        "location_id": "poi_005791",
        "category_before": "Skilling • Hunter",
        "category_after": "Skilling • Hunter",
        "description_before": "Target: Sailing-island hunter creatures\\nLevel requirement: Check Hunter skill guide for creature levels\\nCatch method: Traps, snares, or tracking\\nRegion: Turtle Belt (Dognose Island)",
        "description_after": "Target: Crimson swift (Red feathers)\\nLevel requirement: Level 1 Hunter\\nCatch method: Bird snare\\nRegion: Turtle Belt (Dognose Island)",
        "evidence": "Live server visit confirmed NPC 5549 Crimson swift at 3050, 2647.",
        "rationale": "Replaced generic placeholder description with verified bird species and requirements."
    },
    {
        "poi_id": "poi_006968",
        "name": "Shimmering Atoll - Black Warlocks (Level 45)",
        "canonical_id": "poi_006968",
        "location_id": "poi_006968",
        "category_before": "Skilling • Hunter",
        "category_after": "Skilling • Hunter",
        "description_before": "Target: Sailing-island hunter creatures\\nLevel requirement: Check Hunter skill guide for creature levels\\nCatch method: Traps, snares, or tracking\\nRegion: Shimmering Atoll (Sailing)",
        "description_after": "Target: Black warlock butterfly (Strength boost)\\nLevel requirement: Level 45 Hunter\\nCatch method: Butterfly net & Butterfly jar (or barehanded at 85)\\nRegion: Shimmering Atoll (Sailing)",
        "evidence": "Live server visit confirmed NPC 5553 Black warlock at 1565, 2775.",
        "rationale": "Replaced generic placeholder description with verified butterfly species and requirements."
    },
    {
        "poi_id": "poi_002604",
        "name": "Hunter training",
        "canonical_id": "poi_002604",
        "location_id": "poi_002604",
        "category_before": "Skilling • Hunter",
        "category_after": "Skilling • Hunter",
        "description_before": "Target: Sailing-island hunter creatures\\nLevel requirement: Check Hunter skill guide for creature levels\\nCatch method: Traps, snares, or tracking\\nRegion: Brittle Isle (Sailing)",
        "description_after": "Target: Snowy knight butterfly\\nLevel requirement: Level 35 Hunter\\nCatch method: Butterfly net & Butterfly jar\\nRegion: Brittle Isle (Sailing)",
        "evidence": "Live server visit confirmed NPC 5554 Snowy knight at 1956, 4058.",
        "rationale": "Replaced generic placeholder description on duplicate marker."
    }
]

existing_ids = {x['poi_id'] for x in fixes_data['fixes']}
for sf in sailing_fixes:
    if sf['poi_id'] not in existing_ids:
        fixes_data['fixes'].append(sf)
fixes_data['total_fixes'] = len(fixes_data['fixes'])

with open('docs/poi-continuation-fixes-2026-10-08.json', 'w', encoding='utf-8') as f:
    json.dump(fixes_data, f, indent=2)

print('Updated fixes ledger. Total fixes:', fixes_data['total_fixes'])
