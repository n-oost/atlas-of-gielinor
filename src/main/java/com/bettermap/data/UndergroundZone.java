/*
 * Copyright (c) 2026, n-oost
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.bettermap.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.coords.WorldPoint;

/**
 * The major underground zones and dungeons, each pairing a surface entrance with the coordinates
 * of its interior. The mapping runs both ways, which is what lets the map peek at a dungeon on
 * hover and swap to it on click.
 */
public enum UndergroundZone
{
	LUMBRIDGE_CELLAR(
		"lumbridge_cellar",
		"Lumbridge Castle Cellar",
		new WorldPoint(3218, 3218, 0),
		new WorldPoint(3210, 9616, 0),
		6400,
		50,
		"Under Lumbridge Castle • Culinaromancer's Chest, Recipe for Disaster"
	),
	VARROCK_SEWERS(
		"varrock_sewers",
		"Varrock Sewers",
		new WorldPoint(3237, 3459, 0),
		new WorldPoint(3237, 9859, 0),
		6400,
		80,
		"Under Varrock • Moss Giants, Skeletons, Vannaka shortcut, Bryophyta",
		new WorldPoint(3229, 3504, 0)
	),
	TAVERLEY_DUNGEON(
		"taverley_dungeon",
		"Taverley Dungeon",
		new WorldPoint(2884, 3398, 0),
		new WorldPoint(2884, 9798, 0),
		6400,
		90,
		"Under Taverley • Blue Dragons, Black Knights, Chaos Druids, Cerberus",
		2769,
		9670,
		2978,
		9854,
		new WorldPoint(2841, 3424, 0)
	),
	DWARVEN_MINES(
		"dwarven_mines",
		"Dwarven Mine",
		new WorldPoint(3018, 3450, 0),
		new WorldPoint(3018, 9850, 0),
		6400,
		70,
		"Under Ice Mountain & Falador • Mining Guild door, Motherlode mouth",
		2979,
		9757,
		3062,
		9853,
		new WorldPoint(3058, 3377, 0)
	),
	EDGEVILLE_DUNGEON(
		"edgeville_dungeon",
		"Edgeville Dungeon",
		new WorldPoint(3096, 3469, 0),
		new WorldPoint(3096, 9869, 0),
		6400,
		75,
		"Under Edgeville • Vannaka, Hill Giants, Obor, Wilderness Gate",
		new WorldPoint(3087, 3571, 0),
		new WorldPoint(3115, 3452, 0)
	),
	CATACOMBS_OF_KOUREND(
		"catacombs_of_kourend",
		"Catacombs of Kourend",
		new WorldPoint(1636, 3673, 0),
		new WorldPoint(1664, 10048, 0),
		6375,
		120,
		"Under Great Kourend • Skotizo, Dark Beasts, Brutal Black Dragons, Abyssal Demons",
		1590,
		9979,
		1736,
		10111
	),
	BRIMHAVEN_DUNGEON(
		"brimhaven_dungeon",
		"Brimhaven Dungeon",
		new WorldPoint(2744, 3154, 0),
		new WorldPoint(2713, 9564, 0),
		6410,
		90,
		"Under Karamja • Saniboch, metal dragons, fire giants, wild dogs, vines",
		2630,
		9425,
		2760,
		9600,
		new WorldPoint(2759, 3062, 0)
	),
	FREMENNIK_SLAYER_DUNGEON(
		"fremennik_slayer_dungeon",
		"Fremennik Slayer Dungeon",
		new WorldPoint(2796, 3615, 0),
		new WorldPoint(2808, 10002, 0),
		6387,
		85,
		"Under Rellekka • Cave Crawlers, Turoths, Kurasks, Cave Horrors link",
		2688,
		9960,
		2812,
		10045
	),
	ASGARNIA_ICE_CAVE(
		"asgarnia_ice_cave",
		"Asgarnian Ice Dungeon",
		new WorldPoint(3007, 3150, 0),
		new WorldPoint(3007, 9550, 0),
		6400,
		65,
		"Under Mudskipper Point • Ice Giants, Ice Warriors, Skeletal Wyverns",
		2944,
		9536,
		3081,
		9598
	),
	STRONGHOLD_OF_SECURITY(
		"stronghold_of_security",
		"Stronghold of Security",
		new WorldPoint(3081, 3420, 0),
		new WorldPoint(1860, 5230, 0),
		0,
		60,
		"Under Barbarian Village • 4 Floors of Combat, Security Questions, Fancy Boots"
	),
	KELDAGRIM(
		"keldagrim",
		"Keldagrim (Dwarven Capital)",
		new WorldPoint(2728, 3715, 0),
		new WorldPoint(2867, 10190, 0),
		6400,
		110,
		"Under Troll Mountain Range • Consortium, Blast Furnace, Cart Station",
		2760,
		10100,
		2943,
		10239,
		new WorldPoint(2795, 3718, 0)
	),
	ARDOUGNE_UNDERGROUND(
		"ardougne_underground",
		"Ardougne Sewers",
		new WorldPoint(2632, 3294, 0),
		new WorldPoint(2632, 9694, 0),
		6400,
		45,
		"Under East & West Ardougne • three disconnected sewer sections",
		2513,
		9664,
		2685,
		9727,
		new WorldPoint(2528, 3303, 0),
		new WorldPoint(2586, 3235, 0)
	),
	WITCHAVEN_DUNGEON(
		"witchaven_dungeon",
		"Witchaven Dungeon",
		new WorldPoint(2696, 3283, 0),
		new WorldPoint(2696, 9683, 0),
		6400,
		40,
		"East of Ardougne • sea slugs, Kennith's Concerns",
		2692,
		9665,
		2748,
		9719
	),
	CHAOS_DRUID_TOWER(
		"chaos_druid_tower",
		"Chaos Druid Tower Dungeon",
		new WorldPoint(2562, 3356, 0),
		new WorldPoint(2562, 9756, 0),
		6400,
		35,
		"North of East Ardougne • chaos druids, Watchtower teleport",
		2559,
		9729,
		2593,
		9757
	),
	CLOCK_TOWER_DUNGEON(
		"clock_tower_dungeon",
		"Clock Tower Dungeon",
		new WorldPoint(2568, 3229, 0),
		new WorldPoint(2568, 9627, 0),
		6400,
		40,
		"South of East Ardougne • Clock Tower quest, coloured tiles",
		2560,
		9600,
		2623,
		9663
	),
	ELEMENTAL_WORKSHOP(
		"elemental_workshop",
		"Elemental Workshop",
		new WorldPoint(2709, 3498, 0),
		new WorldPoint(2716, 9888, 0),
		6400,
		35,
		"Under Seers' Village • odd-looking wall, elemental workshop quests",
		2689,
		9863,
		2740,
		9917
	),
	GLARIALS_TOMB(
		"glarials_tomb",
		"Glarial's Tomb",
		new WorldPoint(2557, 3444, 0),
		new WorldPoint(2557, 9844, 0),
		6400,
		35,
		"West of Baxtorian Falls • Waterfall Quest cairn tomb",
		2525,
		9808,
		2557,
		9848
	),
	GOBLIN_CAVE(
		"goblin_cave",
		"Goblin Cave",
		new WorldPoint(2622, 3393, 0),
		new WorldPoint(2622, 9793, 0),
		6400,
		40,
		"East of the Fishing Guild • goblins, Plain of Mud, Goblin Temple",
		2561,
		9793,
		2622,
		9854
	),
	TEMPLE_OF_IKOV(
		"temple_of_ikov",
		"Temple of Ikov",
		new WorldPoint(2676, 3404, 0),
		new WorldPoint(2676, 9804, 0),
		6400,
		50,
		"Lucien's temple; Shiny key required at McGrubor's Wood shortcut",
		2626,
		9784,
		2748,
		9862,
		new WorldPoint(2659, 3492, 0)
	),
	LEGENDS_GUILD_DUNGEON(
		"legends_guild_dungeon",
		"Legends' Guild Dungeon",
		new WorldPoint(2724, 3375, 0),
		new WorldPoint(2724, 9775, 0),
		6400,
		35,
		"Below Legends' Guild • Legends' Quest, shadow warriors, mithril and coal rocks",
		2688,
		9728,
		2751,
		9786
	),
	KRAKEN_COVE(
		"kraken_cove",
		"Kraken Cove",
		new WorldPoint(2278, 3611, 0),
		new WorldPoint(2278, 10011, 0),
		6400,
		45,
		"cave kraken, Kraken boss",
		2241,
		9985,
		2301,
		10043
	),
	MISCELLANIA_UNDERGROUND(
		"miscellania_underground",
		"Miscellania Underground",
		new WorldPoint(2512, 3865, 0),
		new WorldPoint(2512, 10265, 0),
		6400,
		60,
		"Mining Caverns, Fishing Caverns, Royal Trouble",
		2500,
		10240,
		2623,
		10303,
		new WorldPoint(2508, 3846, 0),
		new WorldPoint(2619, 3865, 0)
	),
	FORTHOS_DUNGEON(
		"forthos_dungeon",
		"Forthos Dungeon",
		new WorldPoint(1701, 3574, 0),
		new WorldPoint(1800, 9950, 0),
		6376,
		80,
		"Sarachnis, Red Dragons, Temple of the Moon",
		1781,
		9881,
		1866,
		9994,
		new WorldPoint(1669, 3567, 0)
	),
	CAM_TORUM(
		"cam_torum",
		"Cam Torum",
		new WorldPoint(1435, 3128, 0),
		new WorldPoint(1439, 9509, 1),
		6381,
		85,
		"Under Avium Savannah • Cam Torum subterranean dwarven city",
		1240,
		9440,
		1580,
		9614
	),
	TZHAAR_CITY(
		"tzhaar_city",
		"TzHaar City / Mor Ul Rek",
		new WorldPoint(2860, 3170, 0),
		new WorldPoint(2496, 5140, 0),
		0,
		170,
		"Under Karamja Volcano • Fight Cave, Inferno, TzHaar-Ket-Rak",
		// The wiki's TzHaar Area box stops at y 5248 and cuts the Inferno (y ~5337) off. Its own
		// box covers the whole Mor Ul Rek complex: Fight Cave, Fight Pit, Inferno, TzHaar-Ket-Rak.
		2250, 5030, 2590, 5370
	),
	GOD_WARS_DUNGEON(
		"god_wars_dungeon",
		"God Wars Dungeon",
		new WorldPoint(2917, 3747, 0),
		new WorldPoint(2881, 5310, 2),
		0,
		90,
		"Under Trollheim Peak • General Graardor, Kree'arra, Zilyana, K'ril"
	),
	WATERBIRTH_DUNGEON(
		"waterbirth_dungeon",
		"Waterbirth Island Dungeon",
		new WorldPoint(2545, 3755, 0),
		new WorldPoint(2520, 9920, 0),
		0,
		170,
		"Under Waterbirth Island • Dagannoth Kings, Wallasalkis, Rock Lobsters",
		2440,
		9600,
		2650,
		10200,
		new WorldPoint(2520, 3740, 0),
		new WorldPoint(2542, 3741, 0)
	),
	FOSSIL_ISLAND_UNDERGROUND(
		"fossil_island_underground",
		"Fossil Island Caverns / Wyvern Cave",
		new WorldPoint(3680, 3854, 0),
		new WorldPoint(3660, 10250, 0),
		6400,
		150,
		"Under Fossil Island • Wyvern Cave, Underwater area, Volcanic Mine",
		3600,
		10122,
		3839,
		10303
	),
	ZANARIS(
		"zanaris",
		"Zanaris",
		new WorldPoint(3201, 3169, 0),
		new WorldPoint(2412, 4434, 0),
		0,
		80,
		"Lost City • Fairy rings, cosmic altar, market, Puro-Puro crop circle"
	),
	ANCIENT_CAVERN(
		"ancient_cavern",
		"Ancient Cavern",
		new WorldPoint(2511, 3511, 0),
		new WorldPoint(1768, 5366, 0),
		0,
		55,
		"Under Baxtorian Falls • Mithril dragons, brutal green dragons, Whirlpool",
		1733,
		5318,
		1790,
		5373
	),
	MOLE_HOLE(
		"mole_hole",
		"Mole Hole",
		new WorldPoint(2985, 3387, 0),
		new WorldPoint(1760, 5180, 0),
		0,
		55,
		"Under Falador Park • Giant Mole",
		1731,
		5131,
		1787,
		5244,
		new WorldPoint(2997, 3376, 0)
	),
	OURANIA_ALTAR(
		"ourania_altar",
		"Ourania Altar (ZMI)",
		new WorldPoint(2451, 3231, 0),
		new WorldPoint(3022, 5582, 0),
		0,
		55,
		"Ourania Cave • Ourania / ZMI runecrafting altar",
		3008,
		5568,
		3070,
		5630
	),
	SMOKE_DEVIL_DUNGEON(
		"smoke_devil_dungeon",
		"Smoke Devil Dungeon",
		new WorldPoint(2412, 3061, 0),
		new WorldPoint(2412, 9461, 0),
		6400,
		45,
		"West of Castle Wars • smoke devils; Thermonuclear boss instance is entrance-only",
		2376,
		9414,
		2427,
		9468
	),
	OBSERVATORY_DUNGEON(
		"observatory_dungeon",
		"Observatory Dungeon",
		new WorldPoint(2458, 3185, 0),
		new WorldPoint(2355, 9394, 0),
		0,
		45,
		"Ruined-building entrance • maze to the Observatory; tower exit is offset on this map",
		2305,
		9344,
		2365,
		9405,
		new WorldPoint(2436, 3163, 0)
	),
	TOWER_OF_LIFE_BASEMENT(
		"tower_of_life_basement",
		"Tower of Life Basement",
		new WorldPoint(2649, 3213, 0),
		new WorldPoint(3059, 4397, 0),
		0,
		45,
		"Under the Tower of Life • Creature Creation",
		3009,
		4353,
		3068,
		4413
	),
	TREE_GNOME_VILLAGE_DUNGEON(
		"tree_gnome_village_dungeon",
		"Tree Gnome Village Dungeon",
		new WorldPoint(2533, 3155, 0),
		new WorldPoint(2533, 9556, 0),
		6400,
		40,
		"Under Tree Gnome Village • Waterfall Quest dungeon; post-quest instance is entrance-only",
		2507,
		9551,
		2553,
		9584
	),
	UNDERGROUND_PASS(
		"underground_pass",
		"Underground Pass",
		new WorldPoint(2433, 3315, 0),
		new WorldPoint(2433, 9715, 0),
		6400,
		55,
		"West of Ardougne to Isafdar • Full cavern pass, Well of Voyage, Iban's Lair",
		2312,
		9601,
		2497,
		9728,
		new WorldPoint(2312, 3215, 0)
	),
	CAMDOZAAL(
		"camdozaal",
		"Ruins of Camdozaal",
		new WorldPoint(2998, 3493, 0),
		new WorldPoint(2976, 5824, 0),
		0,
		80,
		"Under Ice Mountain • Barronite, Camdozaal mines, Lassar Undercity",
		2905,
		5761,
		3031,
		5856
	),
	CORPOREAL_BEAST(
		"corporeal_beast",
		"Corporeal Beast Lair",
		new WorldPoint(3202, 3681, 0),
		new WorldPoint(2967, 4382, 2),
		0,
		55,
		"Wilderness • Corporeal Beast, spirit shields",
		2963,
		4370,
		2998,
		4396
	),
	HALLOWED_SEPULCHRE(
		"hallowed_sepulchre",
		"Hallowed Sepulchre",
		new WorldPoint(3655, 3386, 0),
		new WorldPoint(2400, 5984, 0),
		0,
		70,
		"Under Darkmeyer • Hallowed Sepulchre agility crypt"
	),
	DORGESH_KAAN(
		"dorgesh_kaan",
		"Dorgesh-Kaan",
		new WorldPoint(3230, 3215, 0),
		new WorldPoint(2752, 5344, 0),
		0,
		85,
		"Under Lumbridge Swamp • Dorgeshuun city, agility course, caves"
	),
	DORGESH_KAAN_SOUTH(
		"dorgesh_kaan_south",
		"Dorgesh-Kaan South Dungeon",
		new WorldPoint(3193, 3112, 0),
		new WorldPoint(2715, 5241, 0),
		0,
		70,
		"South of Dorgesh-Kaan • molanisks, Kalphite Hive tunnel"
	),
	ROGUES_DEN(
		"rogues_den",
		"Rogues' Den",
		new WorldPoint(2906, 3537, 0),
		new WorldPoint(3061, 4985, 1),
		0,
		55,
		"Under Burthorpe inn • Rogues' Den maze, bank"
	),
	ABYSS(
		"abyss",
		"The Abyss",
		new WorldPoint(3105, 3558, 0),
		new WorldPoint(3040, 4832, 0),
		0,
		70,
		"Wilderness Mage of Zamorak • Abyssal runecrafting, inner/outer ring"
	),
	PYRAMID_PLUNDER(
		"pyramid_plunder",
		"Pyramid Plunder",
		new WorldPoint(3280, 2795, 0),
		new WorldPoint(1928, 4455, 0),
		1653,
		50,
		"Sophanem • Jalsavrah Pyramid Plunder thieving minigame",
		1796,
		4416,
		1980,
		4543
	),
	BARBARIAN_ASSAULT(
		"barbarian_assault",
		"Barbarian Assault",
		new WorldPoint(2533, 3573, 0),
		new WorldPoint(1888, 5440, 0),
		0,
		55,
		"Barbarian Outpost • Barbarian Assault minigame"
	),
	TARN_LAIR(
		"tarn_lair",
		"Lair of Tarn Razorlor",
		new WorldPoint(3439, 3232, 0),
		new WorldPoint(3246, 4590, 0),
		0,
		60,
		"Abandoned Mine • Tarn Razorlor, Salve amulet (e)",
		3264,
		4608,
		3391,
		4671
	),
	PRIFDDINAS_GRAND_LIBRARY(
		"prifddinas_grand_library",
		"Prifddinas Grand Library",
		new WorldPoint(2272, 3341, 0),
		new WorldPoint(3232, 12512, 0),
		9184,
		45,
		"Under the Tower of Voices • Grand Library, memory crystals",
		3208,
		12510,
		3305,
		12590
	),
	PRIFDDINAS_UNDERGROUND(
		"prifddinas_underground",
		"Prifddinas Underground",
		new WorldPoint(2211, 3303, 0),
		new WorldPoint(3232, 12450, 0),
		9184,
		50,
		"Under Prifddinas • The Gauntlet staging, crystal singing, Iorwerth dungeon link",
		3160,
		12400,
		3315,
		12480
	),
	SMOKE_DUNGEON(
		"smoke_dungeon",
		"Smoke Dungeon",
		new WorldPoint(3310, 2962, 0),
		new WorldPoint(3246, 9378, 0),
		6418,
		60,
		"Under Pollnivneach • Desert Treasure smoky well, fire giants, smoke devils",
		3200,
		9344,
		3326,
		9406
	),
	KLENTERS_PYRAMID(
		"klenters_pyramid",
		"Klenter's Pyramid",
		new WorldPoint(3280, 2780, 0),
		new WorldPoint(3280, 9180, 0),
		6400,
		30,
		"Under Sophanem • Icthlarin's Little Helper, Klenter tomb",
		3272,
		9169,
		3312,
		9205,
		new WorldPoint(3294, 2780, 0)
	),
	DESERT_EAGLE_LAIR(
		"desert_eagle_lair",
		"Desert Eagle Lair",
		new WorldPoint(3400, 3160, 0),
		new WorldPoint(3418, 9571, 0),
		6411,
		25,
		"Under Uzer Plateau • Eagle transport system desert roost",
		3412,
		9560,
		3435,
		9580
	),
	SOPHANEM_DUNGEON(
		"sophanem_dungeon",
		"Sophanem Dungeon",
		new WorldPoint(3315, 2795, 0),
		new WorldPoint(2272, 4319, 0),
		1524,
		65,
		"Under Sophanem • Contact! dungeon, scarab swarms, High Priest",
		2176,
		4240,
		2368,
		4347
	),
	REVENANT_CAVES(
		"revenant_caves",
		"Revenant Caves",
		new WorldPoint(3126, 3832, 0),
		new WorldPoint(3250, 10235, 0),
		6403,
		100,
		"Under the Wilderness • Revenants, ancient artefacts, high-risk PvP dungeon",
		3144,
		10053,
		3262,
		10236,
		new WorldPoint(3067, 3741, 0),
		new WorldPoint(3074, 3655, 0)
	),
	WILDERNESS_SLAYER_CAVE(
		"wilderness_slayer_cave",
		"Wilderness Slayer Cave",
		new WorldPoint(3260, 3710, 0),
		new WorldPoint(3373, 10106, 0),
		6396,
		90,
		"Under the Wilderness • Krystilia Slayer tasks, abyssal demons, nechryaels, Larran's chest",
		3329,
		10049,
		3453,
		10173,
		new WorldPoint(3259, 3666, 0),
		new WorldPoint(3292, 3746, 0)
	),
	WILDERNESS_GOD_WARS_DUNGEON(
		"wilderness_god_wars_dungeon",
		"Wilderness God Wars Dungeon",
		new WorldPoint(3017, 3738, 0),
		new WorldPoint(3017, 10138, 0),
		6400,
		50,
		"Under the Wilderness • God Wars dungeon entrance hole, ecumenical keys",
		3015,
		10115,
		3067,
		10166
	),
	DEEP_WILDERNESS_DUNGEON(
		"deep_wilderness_dungeon",
		"Deep Wilderness Dungeon",
		new WorldPoint(3045, 3927, 0),
		new WorldPoint(3045, 10327, 0),
		6400,
		50,
		"Deep Wilderness • Fire giants, shadow spiders, Rogue's Castle passage",
		3008,
		10304,
		3071,
		10367
	),
	MAGE_ARENA_BANK(
		"mage_arena_bank",
		"Mage Arena Bank",
		new WorldPoint(3099, 3934, 0),
		new WorldPoint(2525, 4705, 0),
		771,
		35,
		"Under Mage Arena • Kolodion cavern bank, god staves & capes",
		2500,
		4680,
		2556,
		4730,
		new WorldPoint(3089, 3956, 0)
	),
	CHASM_OF_FIRE(
		"chasm_of_fire",
		"Chasm of Fire",
		new WorldPoint(1435, 3671, 0),
		new WorldPoint(1435, 10071, 0),
		6400,
		45,
		"Under Shayzien • Lesser/Greater/Black demons, Konar Slayer tasks",
		1409,
		10048,
		1468,
		10110
	),
	THE_WARRENS(
		"the_warrens",
		"The Warrens",
		new WorldPoint(1775, 3730, 0),
		new WorldPoint(1775, 10130, 0),
		6400,
		65,
		"Under Hosidius • The Ascent of Arceuus, King's sewer hideout",
		1727,
		10109,
		1816,
		10176,
		new WorldPoint(1812, 3745, 0)
	),
	KARUULM_SLAYER_DUNGEON(
		"karuulm_slayer_dungeon",
		"Karuulm Slayer Dungeon",
		new WorldPoint(1311, 3807, 0),
		new WorldPoint(1311, 10207, 0),
		6400,
		75,
		"Under Mount Karuulm • Wyrms, Drakes, Hydras, Alchemical Hydra boss",
		1254,
		10174,
		1380,
		10281
	),
	WOODCUTTING_GUILD_DUNGEON(
		"woodcutting_guild_dungeon",
		"Woodcutting Guild Dungeon",
		new WorldPoint(1580, 3480, 0),
		new WorldPoint(1580, 9880, 0),
		6400,
		40,
		"Under Hosidius Woodcutting Guild • Ent dungeon",
		1540,
		9860,
		1599,
		9915,
		new WorldPoint(1603, 3508, 0)
	),
	NEYPOTZLI(
		"neypotzli",
		"Neypotzli (Moons of Peril)",
		new WorldPoint(1435, 3128, 0),
		new WorldPoint(1439, 9660, 0),
		6381,
		85,
		"Under Cam Torum • Moons of Peril, Twilight Temple ritual chambers",
		1344,
		9614,
		1535,
		9742
	),
	TONALI_CAVERN(
		"tonali_cavern",
		"Tonali Cavern",
		new WorldPoint(1309, 3104, 0),
		new WorldPoint(1309, 9414, 0),
		6310,
		45,
		"Under Tlati Rainforest • Crypt of Tonali, Earthen Nagua, Doom of Mokhaiotl",
		1280,
		9344,
		1343,
		9423,
		new WorldPoint(1305, 3033, 0)
	),
	DRAGON_NEST(
		"dragon_nest",
		"Dragon Nest",
		new WorldPoint(1289, 3134, 0),
		new WorldPoint(1247, 9503, 0),
		6369,
		40,
		"Under Tlati Rainforest • Red and blue dragons, baby dragons",
		1216,
		9472,
		1271,
		9535
	),
	LIGHTHOUSE_DUNGEON(
		"lighthouse_dungeon",
		"Lighthouse Dungeon (Dagannoths)",
		new WorldPoint(2509, 3644, 0),
		new WorldPoint(2518, 10024, 0),
		6380,
		50,
		"Under the Lighthouse • Dagannoths, Horror from the Deep lair, prayer book altar",
		2498,
		9986,
		2542,
		10041
	),
	TOMBS_OF_AMASCUT(
		"tombs_of_amascut",
		"Tombs of Amascut Lobby",
		new WorldPoint(3356, 2712, 0),
		new WorldPoint(3344, 9130, 0),
		6400,
		40,
		"Under Necropolis • Tombs of Amascut raid lobby",
		3344,
		9102,
		3375,
		9130
	),
	LIZARDMAN_CAVES(
		"lizardman_caves",
		"Lizardman Caves",
		new WorldPoint(1306, 3574, 0),
		new WorldPoint(1308, 9963, 0),
		6400,
		45,
		"Under Lizardman Settlement • Molch lizardman caves",
		1280,
		9935,
		1343,
		9984
	),
	LIZARDMAN_TEMPLE(
		"lizardman_temple",
		"Lizardman Temple",
		new WorldPoint(1214, 3559, 0),
		new WorldPoint(1281, 10048, 0),
		6489,
		45,
		"Under Mount Quidamortem • Lizardman Shamans & stone chests",
		1281,
		10048,
		1342,
		10111,
		new WorldPoint(1329, 3669, 0),
		new WorldPoint(1311, 3686, 0),
		new WorldPoint(1313, 3663, 0),
		new WorldPoint(1291, 3657, 0)
	),
	GIANTS_DEN(
		"giants_den",
		"Giants' Den",
		new WorldPoint(1420, 3588, 0),
		new WorldPoint(1432, 9913, 0),
		6325,
		60,
		"Under Shayzien • Hill Giants, Moss Giants, Fire Giants",
		1412,
		9856,
		1471,
		9915,
		new WorldPoint(1419, 3588, 0)
	),
	FORTIS_BARRACKS(
		"fortis_barracks",
		"Fortis Barracks",
		new WorldPoint(1640, 3166, 0),
		new WorldPoint(1641, 9576, 0),
		6410,
		60,
		"Under Civitas illa Fortis • Fortis Barracks & subterranean tunnels",
		1612,
		9484,
		1691,
		9579
	),
	FENKENSTRAIN_DUNGEON(
		"fenkenstrain_dungeon",
		"Fenkenstrain's Castle Dungeon",
		new WorldPoint(3509, 3448, 0),
		new WorldPoint(3509, 9848, 0),
		6400,
		45,
		"Under Castle Fenkenstrain • Experiments cave, memorial gravestone link",
		3520,
		9856,
		3583,
		9919
	),
	WILDERNESS_AGILITY_DUNGEON(
		"wilderness_agility_dungeon",
		"Wilderness Agility Dungeon",
		new WorldPoint(2854, 3944, 0),
		new WorldPoint(3008, 10367, 0),
		6423,
		45,
		"Under Deep Wilderness • Wilderness Agility Course escape tunnel & Pirate's Hideout",
		2991,
		10339,
		3071,
		10367,
		new WorldPoint(3004, 3963, 0)
	),
	DAEYALT_ESSENCE_MINE(
		"daeyalt_essence_mine",
		"Daeyalt Essence Mine",
		new WorldPoint(3725, 3356, 0),
		new WorldPoint(3649, 9820, 0),
		6464,
		45,
		"Under Darkmeyer • Daeyalt essence extraction caverns",
		3609,
		9795,
		3710,
		9850
	),
	MOLCH_PEARL_CAVES(
		"molch_pearl_caves",
		"Molch Pearl Caves",
		new WorldPoint(1382, 3825, 0),
		new WorldPoint(1280, 9983, 0),
		6158,
		45,
		"Under Lake Molch • Cormorant fishing cavern & lizardman nests",
		1280,
		9935,
		1343,
		9983
	),

	// --- Additional dungeons. Interior names come from the curated dungeon-interior POIs in
	// PoiDetails; surface + interior coordinates are the vendored Shortest Path transport pairs
	// (transports.tsv) that land within a tile of that POI. Radius is a fixed framing default — it
	// only nudges dungeonTargetZoom within its [2.5, 6.0] clamp.
	KALPHITE_LAIR(
		"kalphite_lair",
		"Kalphite Lair",
		new WorldPoint(3226, 3108, 0),
		new WorldPoint(3483, 9510, 0),
		6402,
		60,
		"Under the Kharidian Desert • Kalphite Queen, kalphite soldiers",
		3464,
		9480,
		3511,
		9519
	),
	KALPHITE_CAVE(
		"kalphite_cave",
		"Kalphite Cave",
		new WorldPoint(3319, 3122, 0),
		new WorldPoint(3282, 9493, 0),
		6383,
		70,
		"Under the Shantay Pass • kalphite workers/soldiers, DT2 access",
		3264,
		9408,
		3387,
		9565
	),
	GIANTS_FOUNDRY(
		"giants_foundry",
		"Giants' Foundry",
		new WorldPoint(3360, 3150, 0),
		new WorldPoint(3374, 11493, 0),
		8343,
		55,
		"Under Giants' Plateau • Smithing minigame",
		3328,
		11456,
		3423,
		11519
	),
	APE_ATOLL_DUNGEON(
		"ape_atoll_dungeon",
		"Ape Atoll Dungeon",
		new WorldPoint(2763, 2703, 0),
		new WorldPoint(2763, 9103, 0),
		6400,
		80,
		"Under Ape Atoll • skeletons, zombies, dagannoth, Kruk's Dungeon link",
		2309,
		9088,
		2815,
		9277
	),
	WIZARDS_GUILD_BASEMENT(
		"wizards_guild_basement",
		"Wizards' Guild Basement",
		new WorldPoint(2594, 3085, 0),
		new WorldPoint(2594, 9485, 0),
		6400,
		15,
		"Under the Wizards' Guild • Wizard Frumscone's zombie cage; separate from Yanille Dungeon",
		2582,
		9484,
		2594,
		9493
	),
	YANILLE_AGILITY_DUNGEON(
		"yanille_agility_dungeon",
		"Yanille Agility Dungeon",
		new WorldPoint(2569, 3121, 0),
		new WorldPoint(2569, 9525, 0),
		6400,
		80,
		"Under Yanille • agility shortcut course, Salarin the Twisted",
		2560,
		9475,
		2622,
		9586,
		new WorldPoint(2603, 3078, 0)
	),
	MOS_LE_HARMLESS_CAVE(
		"mos_le_harmless_cave",
		"Mos Le'Harmless Cave",
		new WorldPoint(3749, 2973, 0),
		new WorldPoint(3748, 9373, 0),
		6400,
		80,
		"Under Mos Le'Harmless • cave horrors, black candles",
		3715,
		9348,
		3838,
		9470,
		new WorldPoint(3814, 3062, 0),
		new WorldPoint(3829, 3062, 0)
	),
	KARAMJA_DUNGEON(
		"karamja_dungeon",
		"Karamja and Crandor Dungeon",
		new WorldPoint(2855, 3168, 0),
		new WorldPoint(2855, 9568, 0),
		6400,
		80,
		"Under Karamja & Crandor • lesser demons, moss giants, Elvarg lair",
		2810,
		9500,
		2885,
		9695
	),
	CRANDOR_DUNGEON(
		"crandor_dungeon",
		"Crandor and Karamja Dungeon",
		new WorldPoint(2832, 3255, 0),
		new WorldPoint(2833, 9658, 0),
		6400,
		80,
		"Under Crandor • Elvarg, Dragon Slayer, deep Karamja Dungeon",
		2810,
		9500,
		2885,
		9695
	),
	LUMBRIDGE_SWAMP_CAVES(
		"lumbridge_swamp_caves",
		"Lumbridge Swamp Caves",
		new WorldPoint(3168, 3172, 0),
		new WorldPoint(3168, 9572, 0),
		6400,
		80,
		"Under Lumbridge Swamp • cave slimes, cave bugs, Dorgesh-Kaan link"
	),
	CRABCLAW_CAVES(
		"crabclaw_caves",
		"Crabclaw Caves",
		new WorldPoint(1646, 3449, 0),
		new WorldPoint(1646, 9848, 0),
		6399,
		80,
		"Under Hosidius coast • sandy/king sand crabs",
		1635,
		9800,
		1724,
		9855
	),
	LAVA_MAZE_DUNGEON(
		"lava_maze_dungeon",
		"Lava Maze Dungeon",
		new WorldPoint(3069, 3856, 0),
		new WorldPoint(3068, 10256, 0),
		6400,
		60,
		"Under the Lava Maze • lava dragons link, poison spiders, KBD lever",
		3009,
		10240,
		3071,
		10291
	),
	FEROX_ENCLAVE_DUNGEON(
		"ferox_enclave_dungeon",
		"Ferox Enclave Dungeon",
		new WorldPoint(3152, 3644, 0),
		new WorldPoint(3164, 10043, 0),
		6400,
		80,
		"Under Ferox Enclave • spiders, PvP staging caves"
	),
	WEB_CHASM(
		"web_chasm",
		"Web Chasm",
		new WorldPoint(3184, 3745, 0),
		new WorldPoint(3187, 10127, 0),
		6382,
		80,
		"Under the Wilderness • Wilderness God Wars Dungeon approach"
	),
	SCORPIA_CAVE(
		"scorpia_cave",
		"Scorpia cave",
		new WorldPoint(3244, 3949, 0),
		new WorldPoint(3243, 10349, 0),
		6400,
		35,
		"Under the Wilderness • Scorpia boss cave",
		3216,
		10328,
		3247,
		10355,
		new WorldPoint(3231, 3952, 0),
		new WorldPoint(3232, 3936, 0)
	),
	SILK_CHASM(
		"silk_chasm",
		"Silk Chasm",
		new WorldPoint(3320, 3798, 0),
		new WorldPoint(3423, 10203, 2),
		6405,
		35,
		"Under the Wilderness • Venenatis boss lair",
		3400,
		10180,
		3447,
		10231
	),
	ESCAPE_CAVES(
		"escape_caves",
		"Escape Caves",
		new WorldPoint(3260, 3833, 0),
		new WorldPoint(3284, 10208, 0),
		6375,
		55,
		"Under the Wilderness • escape network linking the Wilderness boss caves",
		3264,
		10176,
		3327,
		10239,
		new WorldPoint(3320, 3831, 0),
		new WorldPoint(3284, 3808, 0),
		new WorldPoint(3282, 3775, 0)
	),
	CALLISTOS_DEN(
		"callistos_den",
		"Callisto's Den",
		new WorldPoint(3292, 3850, 0),
		new WorldPoint(3292, 10250, 0),
		6400,
		40,
		"Under the Wilderness • Callisto boss lair",
		3264,
		10240,
		3327,
		10303
	),
	STRONGHOLD_SLAYER_DUNGEON(
		"stronghold_slayer_dungeon",
		"Stronghold Slayer Dungeon",
		new WorldPoint(2430, 3424, 0),
		new WorldPoint(2429, 9824, 0),
		6400,
		50,
		"Under the Tree Gnome Stronghold • common slayer tasks, Turael-adjacent",
		2387,
		9767,
		2495,
		9838
	),
	BRIMSTAIL_CAVE(
		"brimstail_cave",
		"Brimstail's Cave",
		new WorldPoint(2402, 3419, 0),
		new WorldPoint(2409, 9812, 0),
		6400,
		35,
		"West Tree Gnome Stronghold • Brimstail, crystal chest, spirit tree",
		2378,
		9807,
		2414,
		9832
	),
	GRAND_TREE_TUNNELS(
		"grand_tree_tunnels",
		"Grand Tree Tunnels",
		new WorldPoint(2464, 3497, 0),
		new WorldPoint(2464, 9897, 0),
		6400,
		80,
		"Under the Grand Tree • black demons, Glough's tunnels",
		2432,
		9856,
		2495,
		9919
	),
	HEROES_GUILD_MINE(
		"heroes_guild_mine",
		"Heroes' Guild Mine",
		new WorldPoint(2893, 3507, 0),
		new WorldPoint(2893, 9907, 0),
		6400,
		40,
		"Under the Heroes' Guild • mithril, adamantite, runite",
		2884,
		9881,
		2943,
		9918
	),
	HAM_HIDEOUT(
		"ham_hideout",
		"H.A.M. Hideout",
		new WorldPoint(3165, 3252, 0),
		new WorldPoint(3149, 9652, 0),
		6400,
		70,
		"Under the H.A.M. trapdoor • H.A.M. members, thieving, Death to the Dorgeshuun"
	),
	DRAYNOR_SEWERS(
		"draynor_sewers",
		"Draynor Sewers",
		new WorldPoint(3118, 3244, 0),
		new WorldPoint(3118, 9644, 0),
		6400,
		70,
		"Under Draynor • skeletons, zombies, Draynor Manor link",
		new WorldPoint(3083, 3272, 0)
	),
	ICE_QUEEN_LAIR(
		"ice_queen_lair",
		"Ice Queen's Lair",
		new WorldPoint(2847, 3516, 0),
		new WorldPoint(2847, 9916, 0),
		6400,
		65,
		"Under White Wolf Mountain • Ice Queen (Heroes' Quest), ice warriors/giants",
		2816,
		9897,
		2896,
		9976,
		new WorldPoint(2822, 3510, 0),
		new WorldPoint(2856, 3519, 0)
	),
	CORSAIR_COVE_DUNGEON(
		"corsair_cove_dungeon",
		"Corsair Cove Dungeon",
		new WorldPoint(2483, 2889, 0),
		new WorldPoint(1971, 9035, 1),
		0,
		75,
		"Under Corsair Cove • scarabs, ogress warriors, Ogress Shaman",
		1950,
		8970,
		2071,
		9070,
		new WorldPoint(2522, 2861, 0)
	),
	ISLE_OF_SOULS_DUNGEON(
		"isle_of_souls_dungeon",
		"Isle of Souls Dungeon",
		new WorldPoint(2309, 2919, 0),
		new WorldPoint(2135, 9320, 0),
		6400,
		110,
		"Under the east Isle of Souls • demons, fire giants, blue dragons, iron dragons",
		2113,
		9285,
		2173,
		9341
	),
	WIZARDS_TOWER_BASEMENT(
		"wizards_tower_basement",
		"Wizards' Tower Basement",
		new WorldPoint(3104, 3162, 0),
		new WorldPoint(3104, 9576, 0),
		6400,
		40,
		"Under Wizards' Tower • Fairytale II, rune essence mine"
	),
	TOLNAS_RIFT(
		"tolnas_rift",
		"Tolna's Rift",
		new WorldPoint(3309, 3450, 0),
		new WorldPoint(3297, 9824, 0),
		6400,
		35,
		"East of Varrock • A Soul's Bane instance; entrance overlay only",
		3288,
		9816,
		3308,
		9836
	),
	SOURHOG_CAVE(
		"sourhog_cave",
		"Sourhog Cave",
		new WorldPoint(3149, 3347, 0),
		new WorldPoint(3149, 9747, 0),
		6400,
		40,
		"West of Lumbridge • sourhogs, Animal Magnetism"
	),
	PATERDOMUS_BASEMENT(
		"paterdomus_basement",
		"Paterdomus Basement",
		new WorldPoint(3405, 3506, 0),
		new WorldPoint(3405, 9906, 0),
		6400,
		50,
		"Under Paterdomus • Priest in Peril, Drezel, holy barrier to Morytania",
		new WorldPoint(3421, 3484, 0)
	),
	WATER_MILL_CELLAR(
		"water_mill_cellar",
		"Water mill cellar",
		new WorldPoint(3166, 3307, 0),
		new WorldPoint(3166, 9695, 0),
		6400,
		35,
		"Under the mill north of Lumbridge • mill cellar, goblins, quest"
	),
	DIGSITE_DUNGEON(
		"digsite_dungeon",
		"Digsite Dungeon",
		new WorldPoint(3354, 3425, 0),
		new WorldPoint(3356, 9822, 0),
		6400,
		45,
		"Under the Digsite • winch, Doug Deeping, Varrock Museum",
		new WorldPoint(3370, 3428, 0)
	),
	CHASM_OF_TEARS(
		"chasm_of_tears",
		"Chasm of Tears",
		new WorldPoint(3219, 3132, 0),
		new WorldPoint(3219, 9532, 2),
		6400,
		35,
		"Plane 2 off Lumbridge Swamp Caves • Juna, Tears of Guthix",
		3210,
		9487,
		3262,
		9533
	),
	MINING_GUILD(
		"mining_guild",
		"Mining Guild",
		new WorldPoint(3019, 3339, 0),
		new WorldPoint(3019, 9739, 0),
		6400,
		40,
		"Under Falador • 60 Mining, amethyst, Dwarven Mine door",
		3001,
		9698,
		3057,
		9756
	),
	MELZARS_MAZE_BASEMENT(
		"melzars_maze_basement",
		"Melzar's Maze Basement",
		new WorldPoint(2924, 3250, 0),
		new WorldPoint(2924, 9650, 0),
		6400,
		35,
		"Under Melzar's Maze • lesser demons, Dragon Slayer",
		2920,
		9638,
		2941,
		9659
	),
	MOTHERLODE_MINE(
		"motherlode_mine",
		"Motherlode Mine",
		new WorldPoint(3060, 3366, 0),
		new WorldPoint(3728, 5692, 0),
		0,
		35,
		"Hop from Dwarven Mine • pay-dirt; entrance overlay only",
		3718,
		5682,
		3738,
		5702
	),
	SCRUBFOOT_CAVE(
		"scrubfoot_cave",
		"Scrubfoot's Cave",
		new WorldPoint(2946, 3507, 0),
		new WorldPoint(2946, 9907, 0),
		6400,
		35,
		"West of Goblin Village • sled course; entrance overlay only",
		2944,
		9897,
		2966,
		9917
	),
	WARRIORS_GUILD_BASEMENT(
		"warriors_guild_basement",
		"Warriors' Guild Basement",
		new WorldPoint(2834, 3542, 0),
		new WorldPoint(2907, 9968, 0),
		0,
		35,
		"West of the Warriors' Guild • dragon defender cyclopes",
		2904,
		9954,
		2943,
		9976
	),
	WATERFALL_DUNGEON(
		"waterfall_dungeon",
		"Waterfall Dungeon",
		new WorldPoint(2511, 3463, 0),
		new WorldPoint(2575, 9861, 0),
		0,
		45,
		"Baxtorian Falls • Fire giants, shadow/mithril seeds, Glarial's urn",
		2560,
		9860,
		2611,
		9919
	),
	EAGLES_PEAK_DUNGEON(
		"eagles_peak_dungeon",
		"Eagles' Peak Dungeon",
		new WorldPoint(2328, 3496, 0),
		new WorldPoint(2080, 5024, 0),
		0,
		45,
		"Under Eagles' Peak • Giant eagles, kebbit puzzle, silver/golden feather",
		2048,
		4996,
		2107,
		5047
	),
	OGRE_ENCLAVE(
		"ogre_enclave",
		"Ogre Enclave",
		new WorldPoint(2506, 3039, 0),
		new WorldPoint(2506, 9439, 0),
		6400,
		60,
		"Under Gu'Tanoth • Watchtower quest, Blue dragons, Greater demons, Skavid",
		2496,
		9408,
		2559,
		9471
	),
	JIGGIG_DUNGEON(
		"jiggig_dungeon",
		"Jiggig Dungeon",
		new WorldPoint(2443, 3051, 0),
		new WorldPoint(2443, 9451, 0),
		6400,
		45,
		"Under Jiggig • Zogre Flesh Eaters, Slash Bash tomb, coffins",
		2436,
		9412,
		2491,
		9460
	),
	RED_CHINCHOMPA_HUNTING_GROUND(
		"red_chinchompa_hunting_ground",
		"Red Chinchompa Hunting Ground",
		new WorldPoint(2525, 2894, 0),
		new WorldPoint(2525, 9294, 0),
		6400,
		30,
		"Under Feldip Hills • Private red chinchompa hunting cave (Western Provinces Diary)",
		2508,
		9284,
		2535,
		9308
	),
	JUNGLE_EAGLE_LAIR(
		"jungle_eagle_lair",
		"Jungle Eagle Lair",
		new WorldPoint(2518, 2921, 0),
		new WorldPoint(2518, 9321, 0),
		6400,
		30,
		"Under Feldip Hills • Eagle transport system, jungle eagle nest",
		2508,
		9312,
		2527,
		9331
	),
	SKAVID_CAVES(
		"skavid_caves",
		"Skavid Caves",
		new WorldPoint(2562, 3024, 0),
		new WorldPoint(2562, 9424, 0),
		6400,
		35,
		"Under Gu'Tanoth cliffs • Watchtower quest, mad skavids, cave nightshade",
		2560,
		9420,
		2585,
		9470,
		new WorldPoint(2500, 2991, 0),
		new WorldPoint(2521, 3070, 0),
		new WorldPoint(2528, 3014, 0),
		new WorldPoint(2540, 3054, 0),
		new WorldPoint(2551, 3035, 0),
		new WorldPoint(2552, 3054, 0),
		new WorldPoint(2574, 3028, 0)
	),
	SHILO_VILLAGE_GEM_MINE(
		"shilo_village_gem_mine",
		"Shilo Village Gem Mine",
		new WorldPoint(2824, 2998, 0),
		new WorldPoint(2838, 9387, 0),
		6389,
		35,
		"Under Shilo Village • Gem rocks, Shilo Village quest / diary underground mine",
		2824,
		9376,
		2859,
		9400
	),
	RASHILIYIAS_TOMB(
		"rashiliyias_tomb",
		"Rashiliyia's Tomb",
		new WorldPoint(2758, 2983, 0),
		new WorldPoint(2758, 9183, 0),
		6200,
		50,
		"Under Cairn Isle • Shilo Village quest, Ah Za Rhoon, Tomb of Rashiliyia, bone key",
		2752,
		9152,
		2815,
		9343
	),
	CRASH_ISLAND_DUNGEON(
		"crash_island_dungeon",
		"Crash Island Dungeon",
		new WorldPoint(2920, 2721, 0),
		new WorldPoint(2920, 9121, 0),
		6400,
		35,
		"Under Crash Island • Monkey Madness II, demonic gorillas cavern link",
		2892,
		9092,
		2927,
		9147
	),
	KHARAZI_CAVES(
		"kharazi_caves",
		"Kharazi Caves",
		new WorldPoint(2838, 2845, 0),
		new WorldPoint(2838, 9245, 0),
		6400,
		45,
		"Under Kharazi Jungle • Legends' Quest, Viyeldi caves, Echned Zolin, Nezikchened",
		2817,
		9217,
		2865,
		9296
	),
	JATIZSO_MINE(
		"jatizso_mine",
		"Jatizso Mine",
		new WorldPoint(2398, 3813, 0),
		new WorldPoint(2404, 10189, 0),
		6376,
		40,
		"Under Jatizso • The Fremennik Isles, mithril and adamantite mining",
		2370,
		10180,
		2430,
		10235
	),
	ICE_TROLL_CAVES(
		"ice_troll_caves",
		"Ice Troll Caves",
		new WorldPoint(2398, 3898, 0),
		new WorldPoint(2394, 10300, 0),
		6411,
		35,
		"Under Northern Neitiznot • The Fremennik Isles, Ice Troll King cavern link",
		2368,
		10260,
		2432,
		10320,
		new WorldPoint(2315, 3894, 0),
		new WorldPoint(2400, 3889, 0)
	),
	LUNAR_ISLE_MINE(
		"lunar_isle_mine",
		"Lunar Isle Mine",
		new WorldPoint(2142, 3944, 0),
		new WorldPoint(2141, 10344, 0),
		6400,
		45,
		"Under north-east Lunar Isle • lunar ore, gem rocks, rune essence",
		2108,
		10304,
		2180,
		10367
	),
	TROLL_STRONGHOLD(
		"troll_stronghold",
		"Troll Stronghold",
		new WorldPoint(2839, 3690, 0),
		new WorldPoint(3012, 10112, 0),
		6422,
		55,
		"Inside Troll Stronghold • upper level, kitchen, prison, storeroom",
		3012,
		10112,
		3067,
		10167,
		new WorldPoint(2831, 3677, 0),
		new WorldPoint(2827, 3647, 0)
	),
	HAUNTED_MINE(
		"haunted_mine",
		"Haunted Mine",
		new WorldPoint(3441, 3232, 0),
		new WorldPoint(3436, 9637, 0),
		6400,
		45,
		"Under Mort Myre Swamp • Haunted Mine, Treus Dayth, glowing fungus",
		3400,
		9609,
		3447,
		9662
	),
	BARROWS_CRYPTS(
		"barrows_crypts",
		"Barrows Crypts & Tunnels",
		new WorldPoint(3565, 3306, 0),
		new WorldPoint(3565, 9706, 0),
		6400,
		80,
		"Under Barrows • 6 Barrows brothers crypts, puzzle doors, rewards chest",
		3512,
		9672,
		3640,
		9827
	),
	MEIYERDITCH_LABORATORIES(
		"meiyerditch_laboratories",
		"Meiyerditch Laboratories",
		new WorldPoint(3628, 3350, 0),
		new WorldPoint(3628, 9750, 0),
		6400,
		100,
		"Under Meiyerditch & Darkmeyer • Daeyalt Essence Mine, vampyre laboratories, Sins of the Father",
		3450,
		9665,
		3645,
		9950
	),
	NATURE_GROTTO(
		"nature_grotto",
		"Nature Grotto",
		new WorldPoint(3438, 3337, 0),
		new WorldPoint(3440, 9740, 0),
		6397,
		25,
		"Under Mort Myre Swamp • Nature Spirit altar, Filliman Tarlock",
		3434,
		9732,
		3449,
		9747
	),
	ECTOFUNTUS_DUNGEON(
		"ectofuntus_dungeon",
		"Ectofuntus Dungeon",
		new WorldPoint(3659, 3524, 0),
		new WorldPoint(3674, 9893, 0),
		6369,
		30,
		"Under Port Phasmatys Ectofuntus • Pool of Slime, slime buckets",
		3670,
		9875,
		3692,
		9901,
		new WorldPoint(3651, 3519, 0)
	),
	MORYTANIA_SPIDER_CAVE(
		"morytania_spider_cave",
		"Morytania Spider Cave",
		new WorldPoint(3657, 3407, 0),
		new WorldPoint(3670, 9830, 0),
		6400,
		40,
		"South of Port Phasmatys • Spiders, red spiders' eggs, cave crawling",
		3652,
		9792,
		3711,
		9867
	),
	SHADE_CATACOMBS(
		"shade_catacombs",
		"Shade Catacombs",
		new WorldPoint(3485, 3321, 0),
		new WorldPoint(3484, 9721, 0),
		6400,
		45,
		"Under Mort'ton • shade remains, sacred oil, shade chests",
		3456,
		9664,
		3519,
		9727
	),
	VETIONS_REST(
		"vetions_rest",
		"Vet'ion's Rest",
		new WorldPoint(3221, 3788, 0),
		new WorldPoint(3295, 10202, 0),
		6414,
		35,
		"Under the Wilderness • Vet'ion boss lair",
		3284,
		10192,
		3307,
		10211
	),
	ZEMOUREGALS_BASE(
		"zemouregals_base",
		"Zemouregal's Base",
		new WorldPoint(3343, 3515, 0),
		new WorldPoint(3578, 4582, 0),
		1067,
		65,
		"Under Silvarea • Defender of Varrock, armoured zombies",
		3520,
		4520,
		3640,
		4640
	),
	WHITE_KNIGHTS_CASTLE_CRYPT(
		"white_knights_castle_crypt",
		"White Knight's Castle Crypt",
		new WorldPoint(2964, 3331, 0),
		new WorldPoint(2964, 9731, 0),
		6400,
		35,
		"Under White Knights' Castle • Sir Owen, Wanted!",
		2957,
		9729,
		2977,
		9745
	),
	WHITE_WOLF_TUNNEL(
		"white_wolf_tunnel",
		"White Wolf Tunnel",
		new WorldPoint(2876, 3482, 0),
		new WorldPoint(2876, 9878, 0),
		6400,
		40,
		"Under White Wolf Mountain • Taverley–Catherby stairs",
		2816,
		9860,
		2878,
		9886,
		new WorldPoint(2819, 3484, 0)
	),
	POISON_WASTE_DUNGEON(
		"poison_waste_dungeon",
		"Poison Waste Sewer",
		new WorldPoint(2330, 3106, 0),
		new WorldPoint(1504, 4235, 1),
		0,
		80,
		"Under Poison Waste • Warped terrorbirds & tortoises (The Path of Glouphrie)",
		1349,
		4224,
		1535,
		4351
	),
	SISTERHOOD_SANCTUARY(
		"sisterhood_sanctuary",
		"Sisterhood Sanctuary",
		new WorldPoint(3727, 3300, 0),
		new WorldPoint(3727, 9700, 1),
		6400,
		80,
		"Under Slepe Church • The Nightmare of Ashihama & Sisterhood crypts",
		3715,
		9664,
		3903,
		10302,
		new WorldPoint(3724, 3356, 0)
	),
	JORMUNGANDS_PRISON(
		"jormungands_prison",
		"Jormungand's Prison",
		new WorldPoint(2465, 4010, 0),
		new WorldPoint(2445, 10419, 0),
		6400,
		55,
		"Under Island of Stone • The Fremennik Exiles, Jormungand & Basilisks",
		2398,
		10370,
		2493,
		10468
	),
	GHORROCK_DUNGEON(
		"ghorrock_dungeon",
		"Ghorrock Dungeon",
		new WorldPoint(2847, 4043, 0),
		new WorldPoint(2888, 10465, 0),
		6400,
		45,
		"Under Ghorrock fortress • Ice demons, Frozen Waste, Desert Treasure II",
		2844,
		10441,
		2932,
		10489
	),
	HUNTER_GUILD(
		"hunter_guild",
		"Hunter Guild Caverns",
		new WorldPoint(1558, 3047, 0),
		new WorldPoint(1558, 9451, 0),
		6404,
		40,
		"Under Hunter Guild • Hunters' Rumours, Guild master, whistling birds",
		1524,
		9408,
		1579,
		9471,
		new WorldPoint(1533, 3050, 0)
	),
	LITHKREN_VAULT(
		"lithkren_vault",
		"Lithkren Vault",
		new WorldPoint(3555, 4002, 0),
		new WorldPoint(3549, 10448, 0),
		6446,
		45,
		"Under Lithkren • Dragon Slayer II, Adamant & Rune dragons, dragon forge",
		3540,
		10440,
		3560,
		10488
	),
	WEISS_SALT_MINE(
		"weiss_salt_mine",
		"Weiss Salt Mine",
		new WorldPoint(2869, 3941, 0),
		new WorldPoint(2845, 10351, 0),
		6410,
		35,
		"Under Weiss • Making Friends with My Arm, basalt & icy basalt mining",
		2824,
		10324,
		2860,
		10356
	),
	JALDRAOCHT_PYRAMID(
		"jaldraocht_pyramid",
		"Jaldraocht Pyramid",
		new WorldPoint(3233, 2898, 0),
		new WorldPoint(2913, 4954, 3),
		2056,
		50,
		"Under Kharidian Desert • Desert Treasure, ancient magick pyramid & Azzanadra",
		2865,
		4906,
		2962,
		5003,
		new WorldPoint(3233, 2887, 0)
	),
	RIVER_ELID_DUNGEON(
		"river_elid_dungeon",
		"River Elid Dungeon",
		new WorldPoint(3371, 3129, 0),
		new WorldPoint(3349, 9536, 0),
		6407,
		35,
		"Under River Elid • Spirits of the Elid, water spirits & ancestral shrine",
		3338,
		9528,
		3365,
		9561,
		new WorldPoint(3370, 3132, 0)
	),
	SHADOW_DUNGEON(
		"shadow_dungeon",
		"Shadow Dungeon",
		new WorldPoint(2547, 3421, 0),
		new WorldPoint(2630, 5071, 0),
		1650,
		55,
		"South of Baxtorian Falls • Desert Treasure I shadow diamond & shadow hounds",
		2624,
		5056,
		2752,
		5120
	),
	DRAYNOR_MANOR_BASEMENT(
		"draynor_manor_basement",
		"Draynor Manor Basement",
		new WorldPoint(3092, 3361, 0),
		new WorldPoint(3117, 9753, 0),
		6392,
		30,
		"Under Draynor Manor • Ernest the Chicken, Professor Oddenstein machine levers",
		3088,
		9744,
		3120,
		9768,
		new WorldPoint(3091, 3362, 0),
		new WorldPoint(3114, 3357, 0)
	),
	ENAKHRAS_TEMPLE(
		"enakhras_temple",
		"Enakhra's Temple",
		new WorldPoint(3194, 2925, 0),
		new WorldPoint(3124, 9328, 1),
		6403,
		50,
		"Under Kharidian Desert • Enakhra's Lament, bone sacrifice & Mahjarrat temple",
		3072,
		9280,
		3201,
		9400,
		new WorldPoint(3148, 2938, 0),
		new WorldPoint(3146, 2909, 0)
	),
	PORT_SARIM_RAT_PITS(
		"port_sarim_rat_pits",
		"Port Sarim Rat Pits",
		new WorldPoint(3018, 3231, 0),
		new WorldPoint(2962, 9650, 0),
		6419,
		30,
		"Under Port Sarim • Ratcatchers, Felkrash & Port Sarim sewer rat pits",
		2952,
		9624,
		2981,
		9660,
		new WorldPoint(3017, 3232, 0)
	),
	KING_BLACK_DRAGON_LAIR(
		"king_black_dragon_lair",
		"King Black Dragon Lair",
		new WorldPoint(3017, 3849, 0),
		new WorldPoint(2272, 4705, 0),
		856,
		45,
		"Under Wilderness • King Black Dragon boss lair, pulled lever entrance",
		2252,
		4676,
		2292,
		4716,
		new WorldPoint(3016, 3849, 0)
	),
	HESPORI_CAVE(
		"hespori_cave",
		"Hespori Cave",
		new WorldPoint(1248, 3737, 0),
		new WorldPoint(1248, 10089, 0),
		6352,
		30,
		"Under Farming Guild • Hespori boss arena & Bottomless compost bucket",
		1236,
		10076,
		1260,
		10100
	),
	STALKER_DEN(
		"stalker_den",
		"Stalker Den",
		new WorldPoint(1296, 3374, 0),
		new WorldPoint(1286, 9775, 0),
		6401,
		45,
		"Under Darkfrost • Twilight Emissaries, Stalker boss dungeon",
		1240,
		9728,
		1340,
		9908,
		new WorldPoint(1324, 3364, 0)
	),
	WYRMSCRAIG_CAVERN(
		"wyrmscraig_cavern",
		"Wyrmscraig Cavern",
		new WorldPoint(2540, 2213, 0),
		new WorldPoint(2580, 8613, 0),
		6400,
		55,
		"Under Wyrmscraig • Mortimer, wyrms, mining and the Ardeaglais basement",
		2560,
		8576,
		2624,
		8672,
		new WorldPoint(2530, 2205, 0),
		new WorldPoint(2562, 2203, 0)
	),
	DEEPFIN_MINE(
		"deepfin_mine",
		"Deepfin Mine",
		new WorldPoint(1924, 2758, 0),
		new WorldPoint(2015, 9187, 0),
		6400,
		55,
		"Under Deepfin Point • Deepfin Mine"
	),
	PANDEMONIUM_CAVE(
		"pandemonium_cave",
		"Pandemonium Cave",
		new WorldPoint(3070, 2986, 0),
		new WorldPoint(3043, 9384, 0),
		6400,
		55,
		"Under The Pandemonium • low-level cavern"
	),
	HERE_BE_MINOTAURS(
		"here_be_minotaurs",
		"Here be minotaurs",
		new WorldPoint(1959, 3117, 0),
		new WorldPoint(1947, 9505, 0),
		6400,
		55,
		"Under Minotaurs' Rest • minotaur cavern"
	),
	YNYSDAIL_CAVERN(
		"ynysdail_cavern",
		"Ynysdail Cavern",
		new WorldPoint(2223, 3466, 0),
		new WorldPoint(2271, 9875, 0),
		6400,
		55,
		"Under Ynysdail • Aquanite cavern"
	),
	GRYPHON_CAVE(
		"gryphon_cave",
		"Gryphon Cave",
		new WorldPoint(3120, 2490, 0),
		new WorldPoint(3227, 8861, 0),
		6400,
		55,
		"Under The Great Conch • gryphon caves",
		new WorldPoint(3234, 2418, 0)
	),
	KURASK_LAIR(
		"kurask_lair",
		"Kurask Lair",
		new WorldPoint(1203, 2733, 0),
		new WorldPoint(1181, 9195, 0),
		6400,
		55,
		"Under Laguna Aurorae • Kurask lair"
	),
	SHELLBANE_GRYPHON_CAVE(
		"shellbane_gryphon_cave",
		"Shellbane Gryphon Cave",
		new WorldPoint(3175, 2478, 0),
		new WorldPoint(3177, 8873, 0),
		6400,
		55,
		"Under The Great Conch • Shellbane gryphon cave"
	),
	SUNBLEAK_CAVE(
		"sunbleak_cave",
		"Sunbleak Cave",
		new WorldPoint(2190, 2327, 0),
		new WorldPoint(2225, 8737, 0),
		6400,
		55,
		"Under Sunbleak Island • mining cavern"
	),
	JOGRE_DUNGEON(
		"jogre_dungeon",
		"Jogre Dungeon",
		new WorldPoint(2824, 3118, 0),
		new WorldPoint(2848, 9504, 0),
		6400,
		55,
		"Under Karamja • Jogre Dungeon"
	),
	SCABARAS_DUNGEON(
		"scabaras_dungeon",
		"Scabaras Dungeon",
		new WorldPoint(3407, 2848, 0),
		new WorldPoint(3403, 9247, 0),
		6400,
		55,
		"Under the Desert • Scabaras Dungeon"
	),
	WEREWOLF_AGILITY_COURSE(
		"werewolf_agility_course",
		"Werewolf Agility Course",
		new WorldPoint(3542, 3461, 0),
		new WorldPoint(3551, 9887, 0),
		6400,
		55,
		"Under Morytania • Werewolf Agility Course"
	),
	MYREQUE_HIDEOUT(
		"myreque_hideout",
		"Myreque Hideout",
		new WorldPoint(3508, 3448, 0),
		new WorldPoint(3495, 9835, 0),
		6400,
		55,
		"Under Morytania • Myreque Hideout",
		new WorldPoint(3494, 3464, 0)
	),
	SHAYZIEN_PRISON(
		"shayzien_prison",
		"Shayzien Prison",
		new WorldPoint(1464, 3569, 0),
		new WorldPoint(1437, 9951, 0),
		6400,
		55,
		"Under Shayzien • prison dungeon"
	),
	XERICS_LOOKOUT_BASEMENT(
		"xerics_lookout_basement",
		"Xeric's Lookout basement",
		new WorldPoint(1589, 3526, 0),
		new WorldPoint(1563, 9953, 0),
		6400,
		55,
		"Under Xeric's Lookout"
	),
	FORTIS_COLOSSEUM(
		"fortis_colosseum",
		"Fortis Colosseum",
		new WorldPoint(1824, 3107, 0),
		new WorldPoint(1825, 9507, 0),
		6400,
		55,
		"Under Civitas illa Fortis • Fortis Colosseum"
	),
	ENTRANA_DUNGEON(
		"entrana_dungeon",
		"Entrana Dungeon",
		new WorldPoint(2819, 3374, 0),
		new WorldPoint(2847, 9755, 0),
		6400,
		55,
		"Under Entrana • Entrana Dungeon"
	),
	ALCHEMICAL_SOCIETY(
		"alchemical_society",
		"Alchemical Society",
		new WorldPoint(1389, 2918, 0),
		new WorldPoint(1373, 9315, 0),
		6400,
		55,
		"Under Varlamore • Alchemical Society"
	),
	QUIDAMORTEM_CAVE(
		"quidamortem_cave",
		"Quidamortem Cave",
		new WorldPoint(1213, 3559, 0),
		new WorldPoint(1180, 9956, 0),
		6400,
		55,
		"Under Mount Quidamortem • cave entrance"
	),
	RUINS_OF_UZER_BASEMENT(
		"ruins_of_uzer_basement",
		"Ruins of Uzer basement",
		new WorldPoint(3492, 3090, 0),
		new WorldPoint(2721, 4900, 0),
		6400,
		55,
		"Under the Ruins of Uzer"
	),
	TUTORIAL_ISLAND_DUNGEON(
		"tutorial_island_dungeon",
		"Tutorial Island dungeon",
		new WorldPoint(3087, 3119, 0),
		new WorldPoint(3093, 9513, 0),
		6400,
		40,
		"Under Tutorial Island • mine and combat training area",
		new WorldPoint(3110, 3126, 0)
	),
	CORAL_NURSERIES(
		"coral_nurseries",
		"Coral Nurseries",
		new WorldPoint(3272, 2463, 0),
		new WorldPoint(3297, 8859, 0),
		6400,
		55,
		"Under The Great Conch • Coral Nurseries"
	);

	private final String id;
	private final String name;
	private final WorldPoint surfacePoint;
	private final List<WorldPoint> surfacePoints;
	private final WorldPoint undergroundPoint;
	private final int yOffset;
	private final int radius;
	private final String description;
	/** Hand-authored world-tile clip box; all zero means "use the wiki-box union instead". */
	private final int clipMinX;
	private final int clipMinY;
	private final int clipMaxX;
	private final int clipMaxY;

	UndergroundZone(
		String id,
		String name,
		WorldPoint surfacePoint,
		WorldPoint undergroundPoint,
		int yOffset,
		int radius,
		String description)
	{
		this(id, name, surfacePoint, undergroundPoint, yOffset, radius, description, 0, 0, 0, 0);
	}

	UndergroundZone(
		String id,
		String name,
		WorldPoint surfacePoint,
		WorldPoint undergroundPoint,
		int yOffset,
		int radius,
		String description,
		WorldPoint... alternateSurfacePoints)
	{
		this(id, name, surfacePoint, undergroundPoint, yOffset, radius, description,
			0, 0, 0, 0, alternateSurfacePoints);
	}

	UndergroundZone(
		String id,
		String name,
		WorldPoint surfacePoint,
		WorldPoint undergroundPoint,
		int yOffset,
		int radius,
		String description,
		int clipMinX,
		int clipMinY,
		int clipMaxX,
		int clipMaxY,
		WorldPoint... alternateSurfacePoints)
	{
		this.id = id;
		this.name = name;
		this.surfacePoint = surfacePoint;
		final List<WorldPoint> entrances = new ArrayList<>();
		entrances.add(surfacePoint);
		if (alternateSurfacePoints != null)
		{
			Collections.addAll(entrances, alternateSurfacePoints);
		}
		this.surfacePoints = Collections.unmodifiableList(entrances);
		this.undergroundPoint = undergroundPoint;
		this.yOffset = yOffset;
		this.radius = radius;
		this.description = description;
		this.clipMinX = clipMinX;
		this.clipMinY = clipMinY;
		this.clipMaxX = clipMaxX;
		this.clipMaxY = clipMaxY;
	}

	/** True when this zone carries its own clip box (the wiki boxes do not frame it well). */
	public boolean hasClipOverride()
	{
		return clipMaxX > clipMinX && clipMaxY > clipMinY;
	}

	public int getClipMinX()
	{
		return clipMinX;
	}

	public int getClipMinY()
	{
		return clipMinY;
	}

	public int getClipMaxX()
	{
		return clipMaxX;
	}

	public int getClipMaxY()
	{
		return clipMaxY;
	}

	public String getId()
	{
		return id;
	}

	public String getName()
	{
		return name;
	}

	public WorldPoint getSurfacePoint()
	{
		return surfacePoint;
	}

	/** Canonical entrance first, followed by explicit alternate surface entrances. */
	public List<WorldPoint> getSurfacePoints()
	{
		return surfacePoints;
	}

	public WorldPoint nearestSurfacePoint(int worldX, int worldY)
	{
		WorldPoint nearest = surfacePoint;
		long nearestDistance = Long.MAX_VALUE;
		for (WorldPoint point : surfacePoints)
		{
			final long dx = (long) point.getX() - worldX;
			final long dy = (long) point.getY() - worldY;
			final long distance = dx * dx + dy * dy;
			if (distance < nearestDistance)
			{
				nearest = point;
				nearestDistance = distance;
			}
		}
		return nearest;
	}

	public WorldPoint getUndergroundPoint()
	{
		return undergroundPoint;
	}

	public int getYOffset()
	{
		return yOffset;
	}

	public int getDeltaX()
	{
		return undergroundPoint.getX() - surfacePoint.getX();
	}

	public int getDeltaY()
	{
		return undergroundPoint.getY() - surfacePoint.getY();
	}

	public int getOffsetX()
	{
		return getDeltaX();
	}

	public int getOffsetY()
	{
		return getDeltaY();
	}

	public int getRadius()
	{
		return radius;
	}

	public String getDescription()
	{
		return description;
	}

	public static final List<UndergroundZone> ALL_ZONES;
	private static final Map<String, UndergroundZone> BY_ID;
	private static final Map<String, String> SHARED_ZONE_IDS;

	static
	{
		final List<UndergroundZone> list = new ArrayList<>();
		Collections.addAll(list, values());
		ALL_ZONES = Collections.unmodifiableList(list);
		final Map<String, UndergroundZone> ids = new HashMap<>();
		for (UndergroundZone zone : list)
		{
			ids.put(zone.id, zone);
		}
		BY_ID = Collections.unmodifiableMap(ids);

		final Map<String, String> shared = new HashMap<>();
		shared.put("edgeville_dungeon", "varrock_sewers");
		shared.put("crandor_dungeon", "karamja_dungeon");
		SHARED_ZONE_IDS = Collections.unmodifiableMap(shared);
	}

	public static String canonicalZoneId(String zoneId)
	{
		if (zoneId == null)
		{
			return null;
		}
		return SHARED_ZONE_IDS.getOrDefault(zoneId, zoneId);
	}

	public static boolean zonesMatch(String a, String b)
	{
		if (a == null || b == null)
		{
			return false;
		}
		if (a.equals(b))
		{
			return true;
		}
		return canonicalZoneId(a).equals(canonicalZoneId(b));
	}

	public String getCanonicalId()
	{
		return canonicalZoneId(this.id);
	}

	public static UndergroundZone byId(String id)
	{
		return id == null ? null : BY_ID.get(id);
	}

	/**
	 * Finds the nearest underground zone anchored to this surface world point within maxDistance.
	 */
	public static UndergroundZone forSurfacePoint(int worldX, int worldY, int maxDistance)
	{
		UndergroundZone best = null;
		int bestDistSq = maxDistance * maxDistance;

		for (UndergroundZone zone : ALL_ZONES)
		{
			for (WorldPoint surface : zone.surfacePoints)
			{
				final int dx = surface.getX() - worldX;
				final int dy = surface.getY() - worldY;
				final int distSq = dx * dx + dy * dy;
				if (distSq <= bestDistSq)
				{
					bestDistSq = distSq;
					best = zone;
				}
			}
		}

		return best;
	}

	/**
	 * Finds the nearest underground zone matching this lower plane dungeon world point within maxDistance.
	 */
	public static UndergroundZone forUndergroundPoint(int worldX, int worldY, int maxDistance)
	{
		UndergroundZone best = null;
		int bestDistSq = maxDistance * maxDistance;

		for (UndergroundZone zone : ALL_ZONES)
		{
			final int dx = zone.undergroundPoint.getX() - worldX;
			final int dy = zone.undergroundPoint.getY() - worldY;
			final int distSq = dx * dx + dy * dy;
			if (distSq <= bestDistSq)
			{
				bestDistSq = distSq;
				best = zone;
			}
		}

		return best;
	}
}
