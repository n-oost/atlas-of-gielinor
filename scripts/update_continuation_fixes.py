import json

with open('docs/poi-continuation-fixes-2026-10-08.json', 'r', encoding='utf-8') as f:
    fixes_data = json.load(f)

new_fixes = [
    {
        "poi_id": "poi_005871",
        "name": "Regicide",
        "canonical_id": "poi_005871",
        "location_id": "loc_005871",
        "category_before": "Quest Start • Master",
        "category_after": "Quest Start • Experienced",
        "description_before": "Start: King Lathas in East Ardougne Castle\\nDifficulty: Master • Length: Long\\nReward: 3 Quest Points, 13,750 Agility XP, Access to Tirannwn / Port Tyras\\nRequirements: 56 Agility, Underground Pass",
        "description_after": "Start: King Lathas in East Ardougne Castle\\nDifficulty: Experienced • Length: Long\\nReward: 3 Quest Points, 13,750 Agility XP, Access to Tirannwn / Port Tyras\\nRequirements: 56 Agility, Underground Pass",
        "evidence": "Official OSRS quest log / quest-details.tsv line for Regicide (Difficulty: Experienced). Authored entry erroneously assigned Master.",
        "rationale": "Regicide is an Experienced tier quest in the Elf quest series, not Master."
    },
    {
        "poi_id": "poi_005872",
        "name": "Roving Elves",
        "canonical_id": "poi_005872",
        "location_id": "loc_005872",
        "category_before": "Quest Start • Master",
        "category_after": "Quest Start • Experienced",
        "description_before": "Start: Islwyn and Eluned in Isafdar\\nDifficulty: Master • Length: Medium\\nReward: 1 Quest Point, Crystal bow or Crystal shield\\nRequirements: 56 Agility, Regicide, Waterfall Quest",
        "description_after": "Start: Islwyn and Eluned in Isafdar\\nDifficulty: Experienced • Length: Medium\\nReward: 1 Quest Point, Crystal bow or Crystal shield\\nRequirements: 56 Agility, Regicide, Waterfall Quest",
        "evidence": "Official OSRS quest log / quest-details.tsv line for Roving Elves (Difficulty: Experienced). Authored entry erroneously assigned Master.",
        "rationale": "Roving Elves is an Experienced tier quest, not Master."
    },
    {
        "poi_id": "poi_005881",
        "name": "Eadgar's Ruse",
        "canonical_id": "poi_005881",
        "location_id": "loc_005881",
        "category_before": "Quest Start • Experienced",
        "category_after": "Quest Start • Intermediate",
        "description_before": "Start: Sanfew in Taverley herb shop\\nDifficulty: Experienced • Length: Medium\\nReward: 1 Quest Point, 11,000 Herblore XP, Trollheim Teleport\\nRequirements: 31 Herblore, 44 Agility, Druidic Ritual, Troll Stronghold",
        "description_after": "Start: Sanfew in Taverley herb shop\\nDifficulty: Intermediate • Length: Medium\\nReward: 1 Quest Point, 11,000 Herblore XP, Trollheim Teleport\\nRequirements: 31 Herblore, 44 Agility, Druidic Ritual, Troll Stronghold",
        "evidence": "Official OSRS quest log / quest-details.tsv line for Eadgar's Ruse (Difficulty: Intermediate). Authored entry erroneously assigned Experienced.",
        "rationale": "Eadgar's Ruse is an Intermediate tier quest, not Experienced."
    },
    {
        "poi_id": "poi_005882",
        "name": "My Arm's Big Adventure",
        "canonical_id": "poi_005882",
        "location_id": "loc_005882",
        "category_before": "Quest Start • Intermediate",
        "category_after": "Quest Start • Experienced",
        "description_before": "Start: Burntmeat in Troll Stronghold kitchen\\nDifficulty: Intermediate • Length: Medium\\nReward: 1 Quest Point, 10,000 Herblore XP, 10,000 Farming XP, Disease-free herb patch\\nRequirements: 29 Farming, 61 Cooking, Eadgar's Ruse, The Feud",
        "description_after": "Start: Burntmeat in Troll Stronghold kitchen\\nDifficulty: Experienced • Length: Medium\\nReward: 1 Quest Point, 10,000 Herblore XP, 10,000 Farming XP, Disease-free herb patch\\nRequirements: 29 Farming, 61 Cooking, Eadgar's Ruse, The Feud",
        "evidence": "Official OSRS quest log / quest-details.tsv line for My Arm's Big Adventure (Difficulty: Experienced). Authored entry erroneously assigned Intermediate.",
        "rationale": "My Arm's Big Adventure is an Experienced tier quest, not Intermediate."
    },
    {
        "poi_id": "poi_005883",
        "name": "Making Friends with My Arm",
        "canonical_id": "poi_005883",
        "location_id": "loc_005883",
        "category_before": "Quest Start • Experienced",
        "category_after": "Quest Start • Master",
        "description_before": "Start: Burntmeat in Troll Stronghold kitchen / My Arm in Weiss\\nDifficulty: Experienced • Length: Long\\nReward: 2 Quest Points, 50,000 Mining XP, 40,000 Firemaking XP, 40,000 Agility XP, Weiss herb patch\\nRequirements: 66 Mining, 68 Firemaking, 72 Woodcutting, 35 Construction, My Arm's Big Adventure, Swan Song, Cold War",
        "description_after": "Start: Burntmeat in Troll Stronghold kitchen\\nDifficulty: Master • Length: Long\\nReward: 2 Quest Points, 50,000 Mining XP, 40,000 Firemaking XP, 40,000 Agility XP, Weiss herb patch\\nRequirements: 66 Mining, 68 Firemaking, 72 Woodcutting, 35 Construction, My Arm's Big Adventure, Swan Song, Cold War",
        "evidence": "Official OSRS quest log / quest-details.tsv line for Making Friends with My Arm (Difficulty: Master; Start: Burntmeat in Troll Stronghold kitchen). Authored entry erroneously assigned Experienced and ambiguous start.",
        "rationale": "Making Friends with My Arm is a Master tier quest starting with Burntmeat in Troll Stronghold."
    },
    {
        "poi_id": "poi_005894",
        "name": "Ethically Acquired Antiquities",
        "canonical_id": "poi_005894",
        "location_id": "loc_005894",
        "category_before": "Quest Start • Intermediate",
        "category_after": "Quest Start • Novice",
        "description_before": "Start: Curator in Hunter Guild / Auburnvale\\nDifficulty: Intermediate • Length: Short\\nReward: 1 Quest Point, 1,500 Thieving XP\\nRequirements: 25 Thieving, Children of the Sun",
        "description_after": "Start: Curator in Hunter Guild / Auburnvale\\nDifficulty: Novice • Length: Short\\nReward: 1 Quest Point, 1,500 Thieving XP\\nRequirements: 25 Thieving, Children of the Sun",
        "evidence": "Official OSRS quest log / quest-details.tsv line for Ethically Acquired Antiquities (Difficulty: Novice). Authored entry erroneously assigned Intermediate.",
        "rationale": "Ethically Acquired Antiquities is a Novice tier Varlamore quest."
    },
    {
        "poi_id": "poi_005895",
        "name": "Death on the Isle",
        "canonical_id": "poi_005895",
        "location_id": "loc_005895",
        "category_before": "Quest Start • Experienced",
        "category_after": "Quest Start • Intermediate",
        "description_before": "Start: Detective in Aldarin\\nDifficulty: Experienced • Length: Medium\\nReward: 1 Quest Point, 6,000 Thieving XP, 4,000 Agility XP, 3,000 Crafting XP\\nRequirements: 34 Agility, 32 Thieving, Children of the Sun",
        "description_after": "Start: Detective in Aldarin\\nDifficulty: Intermediate • Length: Medium\\nReward: 2 Quest Points, 6,000 Thieving XP, 4,000 Agility XP, 3,000 Crafting XP\\nRequirements: 34 Agility, 32 Thieving, Children of the Sun",
        "evidence": "Official OSRS quest log / quest-details.tsv line for Death on the Isle (Difficulty: Intermediate, Reward: 2 Quest Points). Authored entry erroneously assigned Experienced and 1 QP.",
        "rationale": "Death on the Isle awards 2 Quest Points and is classified as Intermediate."
    },
    {
        "poi_id": "poi_005896",
        "name": "The Heart of Darkness",
        "canonical_id": "poi_005896",
        "location_id": "loc_005896",
        "category_before": "Quest Start • Master",
        "category_after": "Quest Start • Experienced",
        "description_before": "Start: Queen Ameyalli in Civitas illa Fortis palace\\nDifficulty: Master • Length: Long\\nReward: 2 Quest Points, 35,000 Thieving XP, 25,000 Agility XP, 25,000 Mining XP\\nRequirements: 55 Agility, 48 Mining, 46 Thieving, Twilight's Promise, Perilous Moons",
        "description_after": "Start: Queen Ameyalli in Civitas illa Fortis palace\\nDifficulty: Experienced • Length: Long\\nReward: 2 Quest Points, 35,000 Thieving XP, 25,000 Agility XP, 25,000 Mining XP\\nRequirements: 55 Agility, 48 Mining, 46 Thieving, Twilight's Promise, Perilous Moons",
        "evidence": "Official OSRS quest log / quest-details.tsv line for The Heart of Darkness (Difficulty: Experienced). Authored entry erroneously assigned Master.",
        "rationale": "The Heart of Darkness is an Experienced tier Varlamore quest, not Master."
    }
]

existing_ids = {fix['poi_id'] for fix in fixes_data['fixes']}
for nf in new_fixes:
    if nf['poi_id'] not in existing_ids:
        fixes_data['fixes'].append(nf)

fixes_data['total_fixes'] = len(fixes_data['fixes'])
with open('docs/poi-continuation-fixes-2026-10-08.json', 'w', encoding='utf-8') as f:
    json.dump(fixes_data, f, indent=2)

print('Updated poi-continuation-fixes-2026-10-08.json. Total fixes:', fixes_data['total_fixes'])
