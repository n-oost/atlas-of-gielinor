"""
Batch remediation script for remaining POI catalog candidates.
Applies factual corrections to pois.tsv and poi_aliases.tsv.
"""
import csv
import json

POIS_PATH = 'src/main/resources/atlasofgielinor/catalog/pois.tsv'
ALIASES_PATH = 'src/main/resources/atlasofgielinor/catalog/poi_aliases.tsv'

# Define updates dictionary
# pid -> {field: new_value}
updates = {
    # Phase A.1: Charter ships & Transportation (plane 0 authored + plane 1 canonical)
    'poi_005846': {
        'detail_title': 'Ship to Brimhaven / Rimmington',
        'details': 'Operator: Captain Barnaby\\nDestinations: Brimhaven (30 coins), Rimmington\\nRequirements: None for Brimhaven (Rocking Out for Rimmington)',
        'source_ref': 'Pre-consolidation PoiIndex/PoiDetails snapshot; identity not globally verified; poi/curated-details.tsv:1488; description enriched 2026-10-08 against https://oldschool.runescape.wiki/w/Captain_Barnaby'
    },
    'poi_004321': {
        'detail_title': 'Ship to Brimhaven / Rimmington',
        'details': 'Operator: Captain Barnaby\\nDestinations: Brimhaven (30 coins), Rimmington\\nRequirements: None for Brimhaven (Rocking Out for Rimmington)',
        'source_ref': 'Pre-consolidation PoiIndex/PoiDetails snapshot; identity not globally verified; description enriched 2026-10-08 against https://oldschool.runescape.wiki/w/Captain_Barnaby'
    },
    'poi_005847': {
        'detail_title': 'Charter Ship (Aldarin)',
        'details': "Network: Trader Stan's Charter Ships\\nDestinations: Ports across Gielinor\\nRequirements: Children of the Sun\\nFares: Standard distance-based fares (half price with Cabin Fever or Ring of charos (a))",
        'source_ref': 'Pre-consolidation PoiIndex/PoiDetails snapshot; identity not globally verified; poi/curated-details.tsv:1511; description enriched 2026-10-08 against https://oldschool.runescape.wiki/w/Charter_ship'
    },
    'poi_003644': {
        'detail_title': 'Charter Ship (Aldarin)',
        'details': "Network: Trader Stan's Charter Ships\\nDestinations: Ports across Gielinor\\nRequirements: Children of the Sun\\nFares: Standard distance-based fares (half price with Cabin Fever or Ring of charos (a))",
        'source_ref': 'Pre-consolidation PoiIndex/PoiDetails snapshot; identity not globally verified; description enriched 2026-10-08 against https://oldschool.runescape.wiki/w/Charter_ship'
    },
    'poi_005848': {
        'detail_title': 'Charter Ship (Civitas illa Fortis)',
        'details': "Network: Trader Stan's Charter Ships\\nDestinations: Ports across Gielinor\\nRequirements: Children of the Sun\\nFares: Standard distance-based fares (half price with Cabin Fever or Ring of charos (a))",
        'source_ref': 'Pre-consolidation PoiIndex/PoiDetails snapshot; identity not globally verified; poi/curated-details.tsv:1514; description enriched 2026-10-08 against https://oldschool.runescape.wiki/w/Charter_ship'
    },
    'poi_003678': {
        'detail_title': 'Charter Ship (Civitas illa Fortis)',
        'details': "Network: Trader Stan's Charter Ships\\nDestinations: Ports across Gielinor\\nRequirements: Children of the Sun\\nFares: Standard distance-based fares (half price with Cabin Fever or Ring of charos (a))",
        'source_ref': 'Pre-consolidation PoiIndex/PoiDetails snapshot; identity not globally verified; description enriched 2026-10-08 against https://oldschool.runescape.wiki/w/Charter_ship'
    },
    'poi_005849': {
        'detail_title': 'Charter Ship (Deepfin Point)',
        'details': "Network: Trader Stan's Charter Ships\\nDestinations: Ports across Gielinor\\nRequirements: Must visit Deepfin Point first\\nFares: Standard distance-based fares (half price with Cabin Fever or Ring of charos (a))",
        'source_ref': 'Pre-consolidation PoiIndex/PoiDetails snapshot; identity not globally verified; poi/curated-details.tsv:1516; description enriched 2026-10-08 against https://oldschool.runescape.wiki/w/Charter_ship'
    },
    'poi_003608': {
        'detail_title': 'Charter Ship (Deepfin Point)',
        'details': "Network: Trader Stan's Charter Ships\\nDestinations: Ports across Gielinor\\nRequirements: Must visit Deepfin Point first\\nFares: Standard distance-based fares (half price with Cabin Fever or Ring of charos (a))",
        'source_ref': 'Pre-consolidation PoiIndex/PoiDetails snapshot; identity not globally verified; description enriched 2026-10-08 against https://oldschool.runescape.wiki/w/Charter_ship'
    },
    'poi_005850': {
        'detail_title': 'Charter Ship (Port Tyras)',
        'details': "Network: Trader Stan's Charter Ships\\nDestinations: Ports across Gielinor\\nRequirements: Regicide\\nFares: Standard distance-based fares (half price with Cabin Fever or Ring of charos (a))",
        'source_ref': 'Pre-consolidation PoiIndex/PoiDetails snapshot; identity not globally verified; poi/curated-details.tsv:1523; description enriched 2026-10-08 against https://oldschool.runescape.wiki/w/Charter_ship'
    },
    'poi_003674': {
        'detail_title': 'Charter Ship (Port Tyras)',
        'details': "Network: Trader Stan's Charter Ships\\nDestinations: Ports across Gielinor\\nRequirements: Regicide\\nFares: Standard distance-based fares (half price with Cabin Fever or Ring of charos (a))",
        'source_ref': 'Pre-consolidation PoiIndex/PoiDetails snapshot; identity not globally verified; description enriched 2026-10-08 against https://oldschool.runescape.wiki/w/Charter_ship'
    },
    'poi_005851': {
        'detail_title': 'Charter Ship (Prifddinas)',
        'details': "Network: Trader Stan's Charter Ships\\nDestinations: Ports across Gielinor\\nRequirements: Song of the Elves\\nFares: Standard distance-based fares (half price with Cabin Fever or Ring of charos (a))",
        'source_ref': 'Pre-consolidation PoiIndex/PoiDetails snapshot; identity not globally verified; poi/curated-details.tsv:1525; description enriched 2026-10-08 against https://oldschool.runescape.wiki/w/Charter_ship'
    },
    'poi_003748': {
        'detail_title': 'Charter Ship (Prifddinas)',
        'details': "Network: Trader Stan's Charter Ships\\nDestinations: Ports across Gielinor\\nRequirements: Song of the Elves\\nFares: Standard distance-based fares (half price with Cabin Fever or Ring of charos (a))",
        'source_ref': 'Pre-consolidation PoiIndex/PoiDetails snapshot; identity not globally verified; description enriched 2026-10-08 against https://oldschool.runescape.wiki/w/Charter_ship'
    },
    'poi_005852': {
        'detail_title': 'Charter Ship (Prifddinas Quest Instance)',
        'details': "Network: Trader Stan's Charter Ships\\nDestinations: Ports across Gielinor\\nRequirements: Song of the Elves (quest instance)\\nFares: Standard distance-based fares",
        'source_ref': 'Pre-consolidation PoiIndex/PoiDetails snapshot; identity not globally verified; poi/curated-details.tsv:1526; description enriched 2026-10-08 against https://oldschool.runescape.wiki/w/Charter_ship'
    },
    'poi_003940': {
        'detail_title': 'Charter Ship (Prifddinas Quest Instance)',
        'details': "Network: Trader Stan's Charter Ships\\nDestinations: Ports across Gielinor\\nRequirements: Song of the Elves (quest instance)\\nFares: Standard distance-based fares",
        'source_ref': 'Pre-consolidation PoiIndex/PoiDetails snapshot; identity not globally verified; description enriched 2026-10-08 against https://oldschool.runescape.wiki/w/Charter_ship'
    },
    'poi_005853': {
        'detail_title': 'Charter Ship (The Summer Shore)',
        'details': "Network: Trader Stan's Charter Ships\\nDestinations: Ports across Gielinor\\nRequirements: Must visit The Summer Shore first\\nFares: Standard distance-based fares (half price with Cabin Fever or Ring of charos (a))",
        'source_ref': 'Pre-consolidation PoiIndex/PoiDetails snapshot; identity not globally verified; poi/curated-details.tsv:1527; description enriched 2026-10-08 against https://oldschool.runescape.wiki/w/Charter_ship'
    },
    'poi_003593': {
        'detail_title': 'Charter Ship (The Summer Shore)',
        'details': "Network: Trader Stan's Charter Ships\\nDestinations: Ports across Gielinor\\nRequirements: Must visit The Summer Shore first\\nFares: Standard distance-based fares (half price with Cabin Fever or Ring of charos (a))",
        'source_ref': 'Pre-consolidation PoiIndex/PoiDetails snapshot; identity not globally verified; description enriched 2026-10-08 against https://oldschool.runescape.wiki/w/Charter_ship'
    },
    'poi_005854': {
        'detail_title': 'Charter Ship (Sunset Coast)',
        'details': "Network: Trader Stan's Charter Ships\\nDestinations: Ports across Gielinor\\nRequirements: Children of the Sun\\nFares: Standard distance-based fares (half price with Cabin Fever or Ring of charos (a))",
        'source_ref': 'Pre-consolidation PoiIndex/PoiDetails snapshot; identity not globally verified; poi/curated-details.tsv:1528; description enriched 2026-10-08 against https://oldschool.runescape.wiki/w/Charter_ship'
    },
    'poi_003647': {
        'detail_title': 'Charter Ship (Sunset Coast)',
        'details': "Network: Trader Stan's Charter Ships\\nDestinations: Ports across Gielinor\\nRequirements: Children of the Sun\\nFares: Standard distance-based fares (half price with Cabin Fever or Ring of charos (a))",
        'source_ref': 'Pre-consolidation PoiIndex/PoiDetails snapshot; identity not globally verified; description enriched 2026-10-08 against https://oldschool.runescape.wiki/w/Charter_ship'
    },
    'poi_005855': {
        'details': 'Network: Lovakengj Minecart Network\\nLocation: Lovakengj central station\\nFares: 50 coins per trip (free after delivering control scrolls to Miriam)',
        'source_ref': 'Pre-consolidation PoiIndex/PoiDetails snapshot; identity not globally verified; poi/curated-details.tsv:1538; description enriched 2026-10-08 against https://oldschool.runescape.wiki/w/Lovakengj_Minecart_Network'
    },

    # Phase A.2: Farming & Mining Sites
    'poi_005801': {
        'details': 'Ores available: Clay, Gold\\nTool needed: Pickaxe (Bronze to Crystal)\\nRegion: Vatrachos Island (Sailing)\\n1 Clay\\n2 Gold',
        'source_ref': 'Pre-consolidation PoiIndex/PoiDetails snapshot; identity not globally verified; description corrected 2026-10-08 via live server scene inspection at (1886, 2984)'
    },
    'poi_005816': {
        'details': 'Patches: Coral\\nLevel requirement: Check Farming skill guide for coral levels\\nTools needed: Spade, Seed dibber, Rake\\nRegion: Ocean Reef (Sailing)',
        'source_ref': 'Pre-consolidation PoiIndex/PoiDetails snapshot; identity not globally verified; description corrected 2026-10-08 against live OSRS Sailing coral farming mechanics'
    },
    'poi_005817': {
        'details': 'Patches: Hespori\\nLevel requirement: Level 65 Farming\\nTools needed: Spade, Seed dibber\\nLocation: Farming Guild (West wing)\\nGrowth time: 22–32 hours (fights Hespori boss upon full growth)',
        'source_ref': 'Pre-consolidation PoiIndex/PoiDetails snapshot; identity not globally verified; description corrected 2026-10-08 against https://oldschool.runescape.wiki/w/Hespori'
    },
    'poi_005818': {
        'details': 'Patches: Seaweed (2 patches)\\nLevel requirement: Level 23 Farming\\nTools needed: Spade, Seed dibber, Rake\\nEquipment: Fishbowl helmet & Diving apparatus\\nLocation: Fossil Island underwater',
        'source_ref': 'Pre-consolidation PoiIndex/PoiDetails snapshot; identity not globally verified; description corrected 2026-10-08 against https://oldschool.runescape.wiki/w/Seaweed_patch'
    },
    'poi_005819': {
        'details': 'Patches: Spirit tree\\nLevel requirement: Level 83 Farming\\nTools needed: Spade, Seed dibber, Rake\\nLocation: Mushroom Meadow (Fossil Island)',
        'source_ref': 'Pre-consolidation PoiIndex/PoiDetails snapshot; identity not globally verified; description corrected 2026-10-08 against https://oldschool.runescape.wiki/w/Spirit_tree_(Farming)'
    },
    'poi_005831': {
        'details': 'Ores available: Copper, Tin, Iron, Coal\\nTool needed: Pickaxe (Bronze to Crystal)\\nCheck Mining skill guide for ore levels\\n4 Copper\\n4 Tin\\n3 Iron\\n4 Coal',
        'source_ref': 'Pre-consolidation PoiIndex/PoiDetails snapshot; identity not globally verified; description corrected 2026-10-08 against live server scene inspection and RuneLite MiningSiteLocation.DESERT_MINING_CAMP_SURFACE'
    },
    'poi_005844': {
        'name': 'Trahaearn Mining Site',
        'detail_title': 'Trahaearn Mining Site',
        'details': 'Ores available: Iron, Silver, Coal, Gold, Mithril, Soft clay, Adamantite, Runite\\nTool needed: Pickaxe (Bronze to Crystal)\\nCheck Mining skill guide for ore levels\\n26 Iron\\n8 Silver\\n19 Coal\\n14 Gold\\n7 Mithril\\n10 Soft clay\\n7 Adamantite\\n4 Runite',
        'source_ref': 'Pre-consolidation PoiIndex/PoiDetails snapshot; identity not globally verified; description corrected 2026-10-08 against RuneLite MiningSiteLocation.TRAHEARN and https://oldschool.runescape.wiki/w/Trahaearn_mine'
    },

    # Phase B.2 & B.3: Hunter spots
    'poi_005807': {
        'name': 'Charred Island - Red Salamanders (Level 59)',
        'detail_title': 'Charred Island - Red Salamanders (Level 59)',
        'details': 'Target: Red salamander\\nLevel requirement: Level 59 Hunter\\nCatch method: Net trap (Rope & Small fishing net on young tree)\\nRegion: Charred Island (Sailing)',
        'source_ref': 'Pre-consolidation PoiIndex/PoiDetails snapshot; identity not globally verified; description corrected 2026-10-08 via live server inspection observing Red salamander at (2643, 2397)'
    },
    'poi_005809': {
        'name': "Minotaurs' Rest - Tropical Wagtails (Level 19)",
        'detail_title': "Minotaurs' Rest - Tropical Wagtails (Level 19)",
        'details': 'Target: Tropical wagtail (Stripy feathers)\\nLevel requirement: Level 19 Hunter\\nCatch method: Bird snare\\nRegion: Minotaurs\' Rest (Sailing)',
        'source_ref': 'Pre-consolidation PoiIndex/PoiDetails snapshot; identity not globally verified; description corrected 2026-10-08 via live server inspection observing Tropical wagtail at (1970, 3103)'
    },
    'poi_005772': {
        'name': "Eagle's Peak - Ferrets (Level 27)",
        'detail_title': "Eagle's Peak - Ferrets (Level 27)",
        'details': "Target: Ferret (Requires completion of Eagles' Peak)\\nLevel requirement: Level 27 Hunter\\nCatch method: Box trap\\nRegion: Kandarin (Eagle's Peak)",
        'source_ref': "Pre-consolidation PoiIndex/PoiDetails snapshot; identity not globally verified; poi/curated-details.tsv:805; description corrected 2026-10-08 against https://oldschool.runescape.wiki/w/Ferret and live server inspection at (2330, 3500)"
    },

    # Phase B.1: 13 Fishing Spots
    'poi_002661': {
        'details': "Fish: Sacred eel\\nLevel requirement: Level 87 Fishing\\nTools: Fishing rod & Fishing bait\\nDissecting with a knife requires Cooking and yields Zulrah's scales",
        'source_ref': 'Pre-consolidation PoiIndex/PoiDetails snapshot; identity not globally verified; description corrected 2026-10-08 against https://oldschool.runescape.wiki/w/Sacred_eel'
    },
    'poi_002664': {
        'details': "Fish: Sacred eel\\nLevel requirement: Level 87 Fishing\\nTools: Fishing rod & Fishing bait\\nDissecting with a knife requires Cooking and yields Zulrah's scales",
        'source_ref': 'Pre-consolidation PoiIndex/PoiDetails snapshot; identity not globally verified; description corrected 2026-10-08 against https://oldschool.runescape.wiki/w/Sacred_eel'
    },
    'poi_002665': {
        'details': "Fish: Sacred eel\\nLevel requirement: Level 87 Fishing\\nTools: Fishing rod & Fishing bait\\nDissecting with a knife requires Cooking and yields Zulrah's scales",
        'source_ref': 'Pre-consolidation PoiIndex/PoiDetails snapshot; identity not globally verified; description corrected 2026-10-08 against https://oldschool.runescape.wiki/w/Sacred_eel'
    },
    'poi_002735': {
        'details': 'Fish: Infernal eel\\nLevel requirement: Level 80 Fishing\\nTools: Oily fishing rod & Fishing bait\\nRequirements: Fire cape to enter Mor Ul Rek inner city',
        'source_ref': 'Pre-consolidation PoiIndex/PoiDetails snapshot; identity not globally verified; description corrected 2026-10-08 against https://oldschool.runescape.wiki/w/Infernal_eel'
    },
    'poi_002738': {
        'details': 'Fish: Infernal eel\\nLevel requirement: Level 80 Fishing\\nTools: Oily fishing rod & Fishing bait\\nRequirements: Fire cape to enter Mor Ul Rek inner city',
        'source_ref': 'Pre-consolidation PoiIndex/PoiDetails snapshot; identity not globally verified; description corrected 2026-10-08 against https://oldschool.runescape.wiki/w/Infernal_eel'
    },
    'poi_002829': {
        'detail_title': 'Fishing Spot (Shrimp & Sardine)',
        'details': 'Method / Fish: Net & Bait (Shrimp, Anchovies, Sardine, Herring)\\nTools: Small fishing net or Fishing rod & Fishing bait\\nLevel requirement: Level 1 Fishing (Shrimp), Level 5 (Sardine), Level 10 (Herring), Level 15 (Anchovies)',
        'source_ref': 'Pre-consolidation PoiIndex/PoiDetails snapshot; identity not globally verified; description corrected 2026-10-08 via live server inspection observing NPC 10513 Net/Bait spot at (1971, 5855)'
    },
    'poi_002841': {
        'details': 'Fish: Camdozaal tetra\\nLevel requirement: Level 33 Fishing\\nTools: Small fishing net\\nRegion: Ruins of Camdozaal (Below Ice Mountain)',
        'source_ref': 'Pre-consolidation PoiIndex/PoiDetails snapshot; identity not globally verified; description corrected 2026-10-08 against https://oldschool.runescape.wiki/w/Camdozaal_tetra'
    },
    'poi_002885': {
        'details': 'Fish: Camdozaal tetra\\nLevel requirement: Level 33 Fishing\\nTools: Small fishing net\\nRegion: Ruins of Camdozaal (Below Ice Mountain)',
        'source_ref': 'Pre-consolidation PoiIndex/PoiDetails snapshot; identity not globally verified; description corrected 2026-10-08 against https://oldschool.runescape.wiki/w/Camdozaal_tetra'
    },
    'poi_003255': {
        'details': 'Fish: Cave eel\\nLevel requirement: Level 38 Fishing\\nTools: Fishing rod & Fishing bait\\nRequirements: Light source and Spiny helmet in Lumbridge Swamp Caves',
        'source_ref': 'Pre-consolidation PoiIndex/PoiDetails snapshot; identity not globally verified; description corrected 2026-10-08 against https://oldschool.runescape.wiki/w/Cave_eel'
    },
    'poi_003269': {
        'details': 'Fish: Cave eel\\nLevel requirement: Level 38 Fishing\\nTools: Fishing rod & Fishing bait\\nRequirements: Light source and Spiny helmet in Lumbridge Swamp Caves',
        'source_ref': 'Pre-consolidation PoiIndex/PoiDetails snapshot; identity not globally verified; description corrected 2026-10-08 against https://oldschool.runescape.wiki/w/Cave_eel'
    },
    'poi_003274': {
        'details': 'Fish: Cave eel\\nLevel requirement: Level 38 Fishing\\nTools: Fishing rod & Fishing bait\\nRequirements: Light source and Spiny helmet in Lumbridge Swamp Caves',
        'source_ref': 'Pre-consolidation PoiIndex/PoiDetails snapshot; identity not globally verified; description corrected 2026-10-08 against https://oldschool.runescape.wiki/w/Cave_eel'
    },
    'poi_003026': {
        'details': 'Fish: Shrimp & Anchovies\\nLevel requirement: Level 1 Fishing (Shrimp), Level 15 Fishing (Anchovies)\\nTools: Small fishing net',
        'source_ref': 'Pre-consolidation PoiIndex/PoiDetails snapshot; identity not globally verified; description corrected 2026-10-08 against https://oldschool.runescape.wiki/w/Shrimp'
    },
    'poi_002649': {
        'details': 'Method: Drift net fishing\\nLevel requirement: Level 44 Fishing and Level 47 Hunter\\nTools: Drift net\\nEquipment: Fishbowl helmet & Diving apparatus',
        'source_ref': 'Pre-consolidation PoiIndex/PoiDetails snapshot; identity not globally verified; description corrected 2026-10-08 against https://oldschool.runescape.wiki/w/Drift_net_fishing'
    },

    # Phase A.3: Remaining Authored Quests
    'poi_005857': {
        'details': 'Start: Gertrude west of Varrock\\nDifficulty: Novice • Length: Very Short\\nReward: 1 Quest Point\\nRequirements: 1x Raw sardine, 1x Bucket of milk',
        'source_ref': 'Pre-consolidation PoiIndex/PoiDetails snapshot; identity not globally verified; poi/curated-details.tsv:1623; description aligned 2026-10-08 against quest-details.tsv'
    },
    'poi_005858': {
        'details': 'Start: Guildmaster in Champions\' Guild\\nDifficulty: Experienced • Length: Medium\\nReward: 2 Quest Points\\nRequirements: 32 Quest Points, Anti-dragon shield',
        'source_ref': 'Pre-consolidation PoiIndex/PoiDetails snapshot; identity not globally verified; poi/curated-details.tsv:1624; description aligned 2026-10-08 against quest-details.tsv'
    },
    'poi_005859': {
        'details': 'Start: Redbeard Frank in Port Sarim\\nDifficulty: Novice • Length: Very Short\\nReward: 2 Quest Points\\nRequirements: Karamja rum, White apron',
        'source_ref': 'Pre-consolidation PoiIndex/PoiDetails snapshot; identity not globally verified; poi/curated-details.tsv:1632; description aligned 2026-10-08 against quest-details.tsv'
    },
    'poi_005860': {
        'category': 'Quest Start • Intermediate',
        'details': 'Start: Morgan in Draynor Village\\nDifficulty: Intermediate • Length: Very Short\\nReward: 3 Quest Points\\nRequirements: 1x Garlic, 1x Wooden stake',
        'source_ref': 'Pre-consolidation PoiIndex/PoiDetails snapshot; identity not globally verified; poi/curated-details.tsv:1635; category and description aligned 2026-10-08 against quest-details.tsv'
    },
    'poi_005862': {
        'details': 'Start: Battered book from bookcase in Seers\' Village\\nDifficulty: Novice • Length: Very Short\\nReward: 1 Quest Point\\nRequirements: 20 Mining, 20 Smithing, 20 Crafting',
        'source_ref': 'Pre-consolidation PoiIndex/PoiDetails snapshot; identity not globally verified; poi/curated-details.tsv:1641; description aligned 2026-10-08 against quest-details.tsv'
    },
    'poi_005863': {
        'details': 'Start: Almera south-west of Baxtorian Falls\\nDifficulty: Intermediate • Length: Short\\nReward: 1 Quest Point\\nRequirements: Rope, 6x Air, 6x Water, 6x Earth runes\\nReward: +13,750 Attack & Strength XP',
        'source_ref': 'Pre-consolidation PoiIndex/PoiDetails snapshot; identity not globally verified; poi/curated-details.tsv:1642; description aligned 2026-10-08 against quest-details.tsv'
    },
    'poi_005864': {
        'details': 'Start: King Arthur in Camelot Castle\\nDifficulty: Intermediate • Length: Short\\nReward: 6 Quest Points\\nRequirements: Excalibur sword, Black candle, Bat bones, Tinderbox',
        'source_ref': 'Pre-consolidation PoiIndex/PoiDetails snapshot; identity not globally verified; poi/curated-details.tsv:1643; description aligned 2026-10-08 against quest-details.tsv'
    },
    'poi_005865': {
        'details': 'Start: King Arthur in Camelot Castle\\nDifficulty: Intermediate • Length: Short\\nReward: 2 Quest Points\\nRequirements: Merlin\'s Crystal completion, 20 Attack',
        'source_ref': 'Pre-consolidation PoiIndex/PoiDetails snapshot; identity not globally verified; poi/curated-details.tsv:1644; description aligned 2026-10-08 against quest-details.tsv'
    },
    'poi_005866': {
        'details': 'Start: King Roald in Varrock Palace / Drezel at Paterdomus\\nDifficulty: Novice • Length: Short\\nReward: 1 Quest Point\\nRequirements: 50x Pure or Rune essence, Access to Morytania',
        'source_ref': 'Pre-consolidation PoiIndex/PoiDetails snapshot; identity not globally verified; poi/curated-details.tsv:1645; description aligned 2026-10-08 against quest-details.tsv'
    },
    'poi_005867': {
        'category': 'Quest Start • Intermediate',
        'details': 'Start: Drezel in Paterdomus temple cellar\\nDifficulty: Intermediate • Length: Short\\nReward: 2 Quest Points\\nRequirements: Priest in Peril completion, 18 Crafting',
        'source_ref': 'Pre-consolidation PoiIndex/PoiDetails snapshot; identity not globally verified; poi/curated-details.tsv:1646; category and description aligned 2026-10-08 against quest-details.tsv'
    },
    'poi_005872': {
        'details': 'Start: Islwyn and Eluned in Isafdar\\nDifficulty: Experienced • Length: Short\\nReward: 1 Quest Point\\nRequirements: Regicide, Waterfall Quest, 56 Agility',
        'source_ref': 'Pre-consolidation PoiIndex/PoiDetails snapshot; identity not globally verified; poi/curated-details.tsv:1652; description aligned 2026-10-08 against quest-details.tsv'
    },
    'poi_005873': {
        'details': 'Start: Arianwyn in Lletya\\nDifficulty: Master • Length: Medium\\nReward: 2 Quest Points\\nRequirements: Roving Elves, Big Chompy Bird Hunting, 60 Ranged, 50 Thieving',
        'source_ref': 'Pre-consolidation PoiIndex/PoiDetails snapshot; identity not globally verified; poi/curated-details.tsv:1653; description aligned 2026-10-08 against quest-details.tsv'
    },
    'poi_005878': {
        'details': 'Start: Jamila in Sophanem\\nDifficulty: Master • Length: Medium\\nReward: 2 Quest Points\\nRequirements: Contact!, Scorpia, 62 Agility, 55 Crafting, 55 Firemaking',
        'source_ref': 'Pre-consolidation PoiIndex/PoiDetails snapshot; identity not globally verified; poi/curated-details.tsv:1659; description aligned 2026-10-08 against quest-details.tsv'
    },
    'poi_005880': {
        'details': 'Start: Denulth in Burthorpe\\nDifficulty: Intermediate • Length: Short\\nReward: 1 Quest Point\\nRequirements: Death Plateau, 15 Agility, 30 Thieving',
        'source_ref': 'Pre-consolidation PoiIndex/PoiDetails snapshot; identity not globally verified; poi/curated-details.tsv:1662; description aligned 2026-10-08 against quest-details.tsv'
    },
    'poi_005883': {
        'details': 'Start: Burntmeat in Troll Stronghold kitchen\\nDifficulty: Master • Length: Medium\\nReward: 2 Quest Points\\nRequirements: My Arm\'s Big Adventure, Swan Song, Cold War,\\nRomeo & Juliet, 66 Firemaking, 72 Mining, 35 Construction, 68 Agility',
        'source_ref': 'Pre-consolidation PoiIndex/PoiDetails snapshot; identity not globally verified; poi/curated-details.tsv:1665; description aligned 2026-10-08 against quest-details.tsv'
    },
    'poi_005884': {
        'details': 'Start: Kaqemeex at stone circle north of Taverley\\nDifficulty: Novice • Length: Very Short\\nReward: 4 Quest Points\\nRequirements: Raw bear meat, Raw beef, Raw chicken, Raw rat meat\\nUnlocks: Herblore skill',
        'source_ref': 'Pre-consolidation PoiIndex/PoiDetails snapshot; identity not globally verified; poi/curated-details.tsv:1666; description aligned 2026-10-08 against quest-details.tsv'
    },
    'poi_005885': {
        'details': 'Start: Achietties outside Heroes\' Guild\\nDifficulty: Experienced • Length: Medium\\nReward: 1 Quest Point\\nRequirements: Shield of Arrav, Lost City, Merlin\'s Crystal,\\nDragon Slayer I, 55 QP, 53 Cooking, 53 Fishing, 25 Herblore, 50 Mining',
        'source_ref': 'Pre-consolidation PoiIndex/PoiDetails snapshot; identity not globally verified; poi/curated-details.tsv:1667; description aligned 2026-10-08 against quest-details.tsv'
    },
    'poi_005887': {
        'details': 'Start: Boy outside Witch\'s House in Taverley\\nDifficulty: Intermediate • Length: Very Short\\nReward: 4 Quest Points\\nRequirements: 1x Cheese, Leather gloves',
        'source_ref': 'Pre-consolidation PoiIndex/PoiDetails snapshot; identity not globally verified; poi/curated-details.tsv:1669; description aligned 2026-10-08 against quest-details.tsv'
    },
    'poi_005891': {
        'details': 'Start: Servant Caoimhe in Civitas illa Fortis\\nDifficulty: Intermediate • Length: Short\\nReward: 1 Quest Point\\nRequirements: Children of the Sun, Unlocks deeper Varlamore areas',
        'source_ref': 'Pre-consolidation PoiIndex/PoiDetails snapshot; identity not globally verified; poi/curated-details.tsv:1673; description aligned 2026-10-08 against quest-details.tsv'
    },
    'poi_005893': {
        'details': 'Start: Frog Citizen in Varlamore quagmire\\nDifficulty: Novice • Length: Very Short\\nReward: 1 Quest Point\\nRequirements: 15 Woodcutting, +2,000 Woodcutting XP',
        'source_ref': 'Pre-consolidation PoiIndex/PoiDetails snapshot; identity not globally verified; poi/curated-details.tsv:1675; description aligned 2026-10-08 against quest-details.tsv'
    },
    'poi_005896': {
        'details': 'Start: Queen Ameyalli in Civitas illa Fortis palace\\nDifficulty: Experienced • Length: Medium\\nReward: 2 Quest Points\\nRequirements: Twilight\'s Promise, Perilous Moons, 55 Thieving,\\n48 Slayer, 46 Mining, 46 Agility, 46 Crafting',
        'source_ref': 'Pre-consolidation PoiIndex/PoiDetails snapshot; identity not globally verified; poi/curated-details.tsv:1678; description aligned 2026-10-08 against quest-details.tsv'
    }
}

# Read existing pois.tsv
with open(POIS_PATH, 'r', encoding='utf-8') as f:
    reader = csv.DictReader(f, delimiter='\t')
    fieldnames = reader.fieldnames
    rows = list(reader)

diff_log = []
modified_count = 0
for row in rows:
    pid = row['id']
    if pid in updates:
        before = dict(row)
        for k, v in updates[pid].items():
            row[k] = v
        after = dict(row)
        diff_log.append({
            'id': pid,
            'name': row['name'],
            'before': before,
            'after': after
        })
        modified_count += 1

print(f"Applied updates to {modified_count} POI records (expected {len(updates)}).")
assert modified_count == len(updates), f"Expected {len(updates)} updates but got {modified_count}"

# Write back pois.tsv
with open(POIS_PATH, 'w', encoding='utf-8', newline='') as f:
    writer = csv.DictWriter(f, fieldnames=fieldnames, delimiter='\t', lineterminator='\n')
    writer.writeheader()
    writer.writerows(rows)
print(f"Successfully wrote {POIS_PATH}.")

# Save diff log to scratch
with open('scripts/applied_diffs.json', 'w', encoding='utf-8') as f:
    json.dump(diff_log, f, indent=2)
print("Saved scripts/applied_diffs.json.")
