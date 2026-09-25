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
	CORPOREAL_BEAST("Corporeal Beast", 785, "Spears / Stat Drains (DWH, BGS)", "Corporeal Beast Lair", new WorldPoint(2967, 4382, 2), "Elysian Sigil, Spectral Sigil, Arcane Sigil"),
	DAGANNOTH_KINGS("Dagannoth Kings", 303, "Tribrid (Melee, Ranged, Magic)", "Waterbirth Island Dungeon", new WorldPoint(2631, 9667, 0), "Berserker Ring, Archer Ring, Seers Ring"),
	ABYSSAL_SIRE("Abyssal Sire", 350, "Shadow Spells / Arclight", "Abyssal Nexus", new WorldPoint(3039, 4788, 0), "Unsired, Abyssal Bludgeon Pieces");

	private final String name;
	private final int combatLevel;
	private final String weaknessStrategy;
	private final String locationName;
	private final WorldPoint worldPoint;
	private final String keyDrops;

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
		return name + " (Lvl " + combatLevel + ")";
	}
}
