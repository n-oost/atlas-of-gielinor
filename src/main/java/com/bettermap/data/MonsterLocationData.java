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

import net.runelite.api.coords.WorldPoint;

public enum MonsterLocationData
{
	KING_BLACK_DRAGON("King Black Dragon", 276, "Melee / Dragonfire Shield", "KBD Lair (Wilderness)", new WorldPoint(3005, 3849, 0), "Draconic Visage, KBD Heads, Dragon Pickaxe"),
	ZULRAH("Zulrah", 725, "Ranged / Magic", "Zulrah's Shrine (Zul-Andra)", new WorldPoint(2200, 3055, 0), "Tanzanite Fang, Magic Fang, Serpentine Visage"),
	VORKATH("Vorkath", 732, "Crumble Undead / Dragonfire Protection", "Ungael", new WorldPoint(2272, 4052, 0), "Vorkath's Head, Skeletal Visage, Draconic Visage"),
	GENERAL_GRAARDOR("General Graardor", 624, "Ranged / Magic", "God Wars Dungeon (Bandos)", new WorldPoint(2867, 5357, 2), "Bandos Chestplate, Bandos Tassets, Bandos Boots"),
	KREE_ARRA("Kree'arra", 580, "Ranged", "God Wars Dungeon (Armadyl)", new WorldPoint(2831, 5295, 2), "Armadyl Helmet, Armadyl Chestplate, Armadyl Chainskirt"),
	COMMANDER_ZILYANA("Commander Zilyana", 596, "Ranged (Stamina / Kiting)", "God Wars Dungeon (Saradomin)", new WorldPoint(2907, 5265, 0), "Saradomin Hilt, Armadyl Crossbow"),
	KRIL_TSUTSAROTH("K'ril Tsutsaroth", 650, "Melee / Arclight", "God Wars Dungeon (Zamorak)", new WorldPoint(2925, 5332, 2), "Zamorakian Spear, Staff of the Dead"),
	NEX("Nex", 1001, "Ranged / Melee", "God Wars Dungeon (Ancient Prison)", new WorldPoint(2924, 5203, 0), "Torva Armour, Nihil Horn, Zaryte Vambraces, Ancient Hilt"),
	CORPOREAL_BEAST("Corporeal Beast", 785, "Spears / Stat Drains (DWH, BGS)", "Corporeal Beast Lair", new WorldPoint(2967, 4382, 2), "Elysian Sigil, Spectral Sigil, Arcane Sigil"),
	DAGANNOTH_KINGS("Dagannoth Kings", 303, "Tribrid (Melee, Ranged, Magic)", "Waterbirth Island Dungeon", new WorldPoint(2631, 9667, 0), "Berserker Ring, Archer Ring, Seers Ring"),
	ABYSSAL_SIRE("Abyssal Sire", 350, "Shadow Spells / Arclight", "Abyssal Nexus", new WorldPoint(3039, 4788, 0), "Unsired, Abyssal Bludgeon Pieces"),
	CHAOS_ELEMENTAL("Chaos Elemental", 305, "World - surface", new WorldPoint(3261, 3927, 0)),
	CHAOS_FANATIC("Chaos Fanatic", 202, "World - surface", new WorldPoint(2979, 3846, 0)),
	CRAZY_ARCHAEOLOGIST("Crazy archaeologist", 204, "World - surface", new WorldPoint(2977, 3702, 0)),
	DERANGED_ARCHAEOLOGIST("Deranged archaeologist", 276, "World - surface", new WorldPoint(3683, 3706, 0)),
	THE_HUEYCOATL("The Hueycoatl", 642, "World - surface", new WorldPoint(1509, 3290, 0)),
	GEMSTONE_CRAB("Gemstone Crab", 160, "World - surface", new WorldPoint(1275, 3160, 0)),
	GIANT_MOLE("Giant Mole", 230, "mole hole", new WorldPoint(1759, 5189, 0)),
	SARACHNIS("Sarachnis", 318, "forthos dungeon", new WorldPoint(1840, 9900, 0)),
	KALPHITE_QUEEN("Kalphite Queen", 333, "kalphite lair", new WorldPoint(3486, 9501, 0)),
	BLOOD_MOON("Blood Moon", 329, "neypotzli", new WorldPoint(1392, 9632, 0)),
	BLUE_MOON("Blue Moon", 329, "neypotzli", new WorldPoint(1440, 9680, 0)),
	ECLIPSE_MOON("Eclipse Moon", 329, "neypotzli", new WorldPoint(1488, 9632, 0)),
	AHRIM_THE_BLIGHTED("Ahrim the Blighted", 98, "World - representative crypt placement", new WorldPoint(3534, 9685, 0)),
	DHAROK_THE_WRETCHED("Dharok the Wretched", 115, "World - representative crypt placement", new WorldPoint(3548, 9685, 0)),
	GUTHAN_THE_INFESTED("Guthan the Infested", 115, "World - representative crypt placement", new WorldPoint(3562, 9685, 0)),
	KARIL_THE_TAINTED("Karil the Tainted", 98, "World - representative crypt placement", new WorldPoint(3534, 9709, 0)),
	TORAG_THE_CORRUPTED("Torag the Corrupted", 115, "World - representative crypt placement", new WorldPoint(3548, 9709, 0)),
	VERAC_THE_DEFILED("Verac the Defiled", 115, "World - representative crypt placement", new WorldPoint(3562, 9709, 0)),
	SCORPIA("Scorpia", 225, "scorpia cave", new WorldPoint(3233, 10341, 0)),
	VENENATIS("Venenatis", 464, "silk chasm", new WorldPoint(3423, 10203, 2)),
	CALLISTO("Callisto", 470, "Callisto den entrance; arena unavailable", new WorldPoint(3291, 3849, 0)),
	VET_ION("Vet'ion", 454, "vetions rest", new WorldPoint(3295, 10202, 0)),
	SPINDEL("Spindel", 302, "web chasm", new WorldPoint(3187, 10127, 0)),
	REVENANT_MALEDICTUS("Revenant maledictus", 397, "revenant caves", new WorldPoint(3235, 10160, 0)),
	ARTIO("Artio", 320, "Wilderness - entrance; arena unavailable", new WorldPoint(3116, 3677, 0)),
	CALVAR_ION("Calvar'ion", 264, "Wilderness - entrance; arena unavailable", new WorldPoint(3180, 3682, 0)),
	DAGANNOTH_PRIME("Dagannoth Prime", 303, "waterbirth dungeon", new WorldPoint(2916, 4458, 0)),
	DAGANNOTH_REX("Dagannoth Rex", 303, "waterbirth dungeon", new WorldPoint(2928, 4437, 0)),
	DAGANNOTH_SUPREME("Dagannoth Supreme", 303, "waterbirth dungeon", new WorldPoint(2900, 4443, 0)),
	CERBERUS("Cerberus", 318, "taverley dungeon", new WorldPoint(1304, 1308, 0)),
	ALCHEMICAL_HYDRA("Alchemical Hydra", 426, "karuulm slayer dungeon", new WorldPoint(1364, 10265, 0)),
	KRAKEN("Kraken", 291, "kraken cove", new WorldPoint(2278, 10034, 0)),
	THERMONUCLEAR_SMOKE_DEVIL("Thermonuclear smoke devil", 301, "smoke devil dungeon", new WorldPoint(2360, 9452, 0)),
	ARAXXOR("Araxxor", 890, "morytania spider cave", new WorldPoint(3630, 9813, 0)),
	GROTESQUE_GUARDIANS("Grotesque Guardians", 248, "Slayer - Slayer Tower entrance; rooftop arena unavailable", new WorldPoint(3428, 3541, 0)),
	SHELLBANE_GRYPHON("Shellbane gryphon", 235, "Slayer - approximate Great Conch entrance; needs tuning", new WorldPoint(3187, 2455, 0)),
	OBOR("Obor", 106, "Instanced - arena centre", new WorldPoint(3088, 9806, 0)),
	BRYOPHYTA("Bryophyta", 128, "varrock sewers", new WorldPoint(3220, 9933, 0)),
	SCURRIUS("Scurrius", 200, "varrock sewers", new WorldPoint(3299, 9867, 0)),
	HESPORI("Hespori", 284, "hespori cave", new WorldPoint(1247, 10087, 0)),
	SKOTIZO("Skotizo", 321, "catacombs of kourend", new WorldPoint(2271, 5661, 0)),
	PHANTOM_MUSPAH("Phantom Muspah", 741, "ghorrock dungeon", new WorldPoint(2927, 10347, 0)),
	DUKE_SUCELLUS("Duke Sucellus", 768, "ghorrock dungeon", new WorldPoint(3039, 6454, 0)),
	THE_LEVIATHAN("The Leviathan", 798, "abyss", new WorldPoint(2081, 6372, 0)),
	THE_WHISPERER("The Whisperer", 791, "Instanced - Ice Mountain entrance; cathedral arena unavailable", new WorldPoint(3030, 3457, 0)),
	VARDORVIS("Vardorvis", 784, "Instanced - surface", new WorldPoint(1128, 3417, 0)),
	THE_NIGHTMARE("The Nightmare", 814, "Instanced - arena centre", new WorldPoint(3776, 10271, 0)),
	PHOSANI_S_NIGHTMARE("Phosani's Nightmare", 1024, "Instanced - representative arena placement", new WorldPoint(3745, 10271, 0)),
	YAMA("Yama", 1238, "Instanced - Chasm of Fire entrance; arena unavailable", new WorldPoint(1438, 10079, 0)),
	DOOM_OF_MOKHAIOTL("Doom of Mokhaiotl", 558, "Instanced - Tonali Cavern entrance; arena unavailable", new WorldPoint(1311, 9383, 0)),
	ROYAL_TITANS("Royal Titans", 350, "Instanced - Asgarnian Ice Dungeon entrance; arena unavailable", new WorldPoint(3007, 9550, 0)),
	AMOXLIATL("Amoxliatl", 263, "Instanced - approximate Ruins of Tapoyauik entrance; needs tuning", new WorldPoint(1440, 3200, 0)),
	BRUTUS("Brutus", 30, "Instanced - entrance; arena unavailable", new WorldPoint(3263, 3297, 0)),
	DEMONIC_BRUTUS("Demonic Brutus", 1224, "Instanced - entrance; arena unavailable", new WorldPoint(3267, 3297, 0)),
	MAD_ANGEL("Mad Angel", 588, "Instanced - Ardeaglais representative placement", new WorldPoint(2532, 2215, 0)),
	MAGGOT_KING("Maggot King", 741, "Instanced - approximate Darkmeyer access point; Vampyrium arena unavailable", new WorldPoint(3631, 3343, 0)),
	THE_MIMIC("The Mimic", 186, "Instanced - Watson entrance; arena unavailable", new WorldPoint(1645, 3575, 0)),
	CRYSTALLINE_HUNLLEF("Crystalline Hunllef", 674, "Minigame - Gauntlet entrance; generated arena", new WorldPoint(2206, 3295, 0)),
	CORRUPTED_HUNLLEF("Corrupted Hunllef", 894, "Minigame - Gauntlet entrance; generated arena", new WorldPoint(2210, 3295, 0)),
	TZTOK_JAD("TzTok-Jad", 702, "Minigame - Fight Caves entrance; arena unavailable", new WorldPoint(2438, 5168, 0)),
	TZKAL_ZUK("TzKal-Zuk", 1400, "Minigame - Mor Ul Rek access point; arena unavailable", new WorldPoint(2495, 5117, 0)),
	SOL_HEREDIT("Sol Heredit", 1563, "Minigame - Colosseum entrance; arena unavailable", new WorldPoint(1802, 3105, 0)),
	WINTERTODT("Wintertodt", 0, "Skilling - surface", new WorldPoint(1630, 3982, 0)),
	TEMPOROSS("Tempoross", 0, "Skilling - Ruins of Unkah entrance; arena unavailable", new WorldPoint(3135, 2840, 0)),
	ZALCANO("Zalcano", 336, "Skilling - Prifddinas entrance; arena unavailable", new WorldPoint(2237, 3278, 0)),
	TEKTON("Tekton", 0, "Raids - Chambers of Xeric entrance; generated rooms", new WorldPoint(1252, 3560, 0)),
	VANGUARD("Vanguard", 0, "Raids - Chambers of Xeric entrance; generated rooms", new WorldPoint(1256, 3560, 0)),
	VESPULA("Vespula", 0, "Raids - Chambers of Xeric entrance; generated rooms", new WorldPoint(1260, 3560, 0)),
	VASA_NISTIRIO("Vasa Nistirio", 0, "Raids - Chambers of Xeric entrance; generated rooms", new WorldPoint(1252, 3564, 0)),
	MUTTADILE("Muttadile", 0, "Raids - Chambers of Xeric entrance; generated rooms", new WorldPoint(1256, 3564, 0)),
	GREAT_OLM("Great Olm", 0, "Raids - Chambers of Xeric entrance; generated rooms", new WorldPoint(1260, 3564, 0)),
	THE_MAIDEN_OF_SUGADINTI("The Maiden of Sugadinti", 940, "Raids - Theatre of Blood entrance; room unavailable", new WorldPoint(3662, 3218, 0)),
	PESTILENT_BLOAT("Pestilent Bloat", 312, "Raids - Theatre of Blood entrance; room unavailable", new WorldPoint(3666, 3218, 0)),
	NYLOCAS_VASILIAS("Nylocas Vasilias", 0, "Raids - Theatre of Blood entrance; room unavailable", new WorldPoint(3670, 3218, 0)),
	SOTETSEG("Sotetseg", 336, "Raids - Theatre of Blood entrance; room unavailable", new WorldPoint(3662, 3222, 0)),
	XARPUS("Xarpus", 331, "Raids - Theatre of Blood entrance; room unavailable", new WorldPoint(3666, 3222, 0)),
	VERZIK_VITUR("Verzik Vitur", 425, "Raids - Theatre of Blood entrance; room unavailable", new WorldPoint(3670, 3222, 0)),
	AKKHA("Akkha", 337, "Raids - Tombs of Amascut lobby; room unavailable", new WorldPoint(3350, 9110, 0)),
	BA_BA("Ba-Ba", 359, "Raids - Tombs of Amascut lobby; room unavailable", new WorldPoint(3357, 9110, 0)),
	KEPHRI("Kephri", 341, "Raids - Tombs of Amascut lobby; room unavailable", new WorldPoint(3364, 9110, 0)),
	ZEBAK("Zebak", 371, "Raids - Tombs of Amascut lobby; room unavailable", new WorldPoint(3350, 9117, 0)),
	TUMEKEN_S_WARDEN("Tumeken's Warden", 489, "Raids - Tombs of Amascut lobby; room unavailable", new WorldPoint(3357, 9117, 0)),
	ELIDINIS_WARDEN("Elidinis' Warden", 489, "Raids - Tombs of Amascut lobby; room unavailable", new WorldPoint(3364, 9117, 0));

	private final String name;
	private final int combatLevel;
	private final String weaknessStrategy;
	private final String locationName;
	private final WorldPoint worldPoint;
	private final String keyDrops;

	MonsterLocationData(String name, int combatLevel, String locationName, WorldPoint worldPoint)
	{
		this(name, combatLevel, "", locationName, worldPoint, "");
	}

	MonsterLocationData(String name, int combatLevel, String weaknessStrategy, String locationName, WorldPoint worldPoint, String keyDrops)
	{
		this.name = name;
		this.combatLevel = combatLevel;
		this.weaknessStrategy = weaknessStrategy;
		this.locationName = locationName;
		this.worldPoint = worldPoint;
		this.keyDrops = keyDrops;
	}

	public String getName() { return name; }
	public int getCombatLevel() { return combatLevel; }
	public String getWeaknessStrategy() { return weaknessStrategy; }
	public String getLocationName() { return locationName; }
	public WorldPoint getWorldPoint() { return worldPoint; }
	public String getKeyDrops() { return keyDrops; }

	@Override
	public String toString()
	{
		return combatLevel > 0 ? name + " (Lvl " + combatLevel + ")" : name;
	}
}
