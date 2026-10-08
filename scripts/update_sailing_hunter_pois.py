with open('src/main/resources/atlasofgielinor/catalog/pois.tsv', 'r', encoding='utf-8') as f:
    lines = f.readlines()

new_lines = []
for line in lines:
    p = line.strip('\r\n').split('\t')
    if len(p) > 0:
        if p[0] == 'poi_005808':
            # Anglers' Retreat - Ruby harvest
            p[1] = "Anglers' Retreat - Ruby Harvests (Level 15)"
            p[4] = "Anglers' Retreat - Ruby Harvests (Level 15)"
            p[6] = "Target: Ruby harvest butterfly\\nLevel requirement: Level 15 Hunter\\nCatch method: Butterfly net & Butterfly jar (or barehanded at 75)\\nRegion: Anglers' Retreat (Sailing)"
            p[8] = "Pre-consolidation PoiIndex/PoiDetails snapshot; identity not globally verified; poi/curated-details.tsv:882; species verified 2026-10-08 via live server visit (NPC 5556 Ruby harvest at 2470, 2715)"
            line = '\t'.join(p) + '\n'
        elif p[0] == 'poi_005810':
            # Brittle Isle - Snowy knight
            p[1] = "Brittle Isle - Snowy Knights (Level 35)"
            p[4] = "Brittle Isle - Snowy Knights (Level 35)"
            p[6] = "Target: Snowy knight butterfly\\nLevel requirement: Level 35 Hunter\\nCatch method: Butterfly net & Butterfly jar (or barehanded at 85)\\nRegion: Brittle Isle (Sailing)"
            p[8] = "Pre-consolidation PoiIndex/PoiDetails snapshot; identity not globally verified; poi/curated-details.tsv:884; species verified 2026-10-08 via live server visit (NPC 5554 Snowy knight at 1956, 4058)"
            line = '\t'.join(p) + '\n'
        elif p[0] == 'poi_005791':
            # Dognose Island - Crimson swift
            p[1] = "Dognose Island - Crimson Swifts (Level 1)"
            p[4] = "Dognose Island - Crimson Swifts (Level 1)"
            p[6] = "Target: Crimson swift (Red feathers)\\nLevel requirement: Level 1 Hunter\\nCatch method: Bird snare\\nRegion: Turtle Belt (Dognose Island)"
            p[8] = "Pre-consolidation PoiIndex/PoiDetails snapshot; identity not globally verified; poi/curated-details.tsv:843; species verified 2026-10-08 via live server visit (NPC 5549 Crimson swift at 3050, 2647)"
            line = '\t'.join(p) + '\n'
        elif p[0] == 'poi_006968':
            # Shimmering Atoll - Black warlock
            p[1] = "Shimmering Atoll - Black Warlocks (Level 45)"
            p[4] = "Shimmering Atoll - Black Warlocks (Level 45)"
            p[6] = "Target: Black warlock butterfly (Strength boost)\\nLevel requirement: Level 45 Hunter\\nCatch method: Butterfly net & Butterfly jar (or barehanded at 85)\\nRegion: Shimmering Atoll (Sailing)"
            p[8] = "Pre-consolidation PoiIndex/PoiDetails snapshot; identity not globally verified; poi/curated-details.tsv:898; species verified 2026-10-08 via live server visit (NPC 5553 Black warlock at 1565, 2775)"
            line = '\t'.join(p) + '\n'
        elif p[0] == 'poi_002604':
            # Brittle Isle hunter training marker
            p[4] = "Brittle Isle - Snowy Knights (Level 35)"
            p[6] = "Target: Snowy knight butterfly\\nLevel requirement: Level 35 Hunter\\nCatch method: Butterfly net & Butterfly jar\\nRegion: Brittle Isle (Sailing)"
            p[8] = "Pre-consolidation PoiIndex/PoiDetails snapshot; identity not globally verified; species verified 2026-10-08 via live server visit (NPC 5554 Snowy knight at 1956, 4058)"
            line = '\t'.join(p) + '\n'
    new_lines.append(line)

with open('src/main/resources/atlasofgielinor/catalog/pois.tsv', 'w', encoding='utf-8', newline='\n') as f:
    f.writelines(new_lines)

print("Updated sailing island hunter POIs with verified species and mechanics.")
