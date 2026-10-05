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
package com.bettermap.map;

import com.bettermap.data.UndergroundZone;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class UndergroundZoneTest
{
	@BeforeClass
	public static void loadCuratedDetails()
	{
		PoiDetails.load();
	}

	@Test
	public void wyrmscraigEntrancesResolveToItsCavern()
	{
		final UndergroundZone zone = UndergroundZone.WYRMSCRAIG_CAVERN;
		assertEquals(3, zone.getSurfacePoints().size());
		assertEquals(zone, UndergroundZone.forSurfacePoint(2540, 2213, 0));
		assertEquals(zone, UndergroundZone.forSurfacePoint(2530, 2205, 0));
		assertEquals(zone, UndergroundZone.forSurfacePoint(2562, 2203, 0));
		assertEquals(zone, InstanceMaps.zoneForPoint(2593, 8639));
		assertEquals(2553, InstanceMaps.toDisplayX(2593, 8639, 2600, 2240));
		assertEquals(2239, InstanceMaps.toDisplayY(2593, 8639, 2600, 2240));
	}

	@Test
	public void wizardsGuildHasIndependentEntranceAndCut()
	{
		UndergroundZone guild = UndergroundZone.WIZARDS_GUILD_BASEMENT;
		assertEquals(0, guild.getDeltaX());
		assertEquals(6400, guild.getDeltaY());
		assertEquals(2594, guild.getSurfacePoint().getX());
		assertEquals(3085, guild.getSurfacePoint().getY());
		assertEquals(2582, guild.getClipMinX());
		assertEquals(9493, guild.getClipMaxY());
		assertEquals("wizards_guild_basement", UndergroundZone.forSurfacePoint(2594, 3085, 5).getId());
	}

	@Test
	public void observatoryUsesRuinedBuildingEntrance()
	{
		UndergroundZone zone = UndergroundZone.OBSERVATORY_DUNGEON;
		assertEquals(2458, zone.getSurfacePoint().getX());
		assertEquals(3185, zone.getSurfacePoint().getY());
		assertEquals(-103, zone.getDeltaX());
		assertEquals(6209, zone.getDeltaY());
		assertTrue(zone.hasClipOverride());
	}

	@Test
	public void testAllZonesIntegrity()
	{
		assertFalse("Underground zones list should not be empty", UndergroundZone.ALL_ZONES.isEmpty());
		for (UndergroundZone zone : UndergroundZone.ALL_ZONES)
		{
			if (zone.getId().startsWith("native_") || zone.isSubterranean() || zone.getYOffset() != 6400 || zone == UndergroundZone.VTAM_CORPORATION || zone.ordinal() > UndergroundZone.WYRMSCRAIG_CAVERN.ordinal()) continue;
			assertNotNull("Zone id should not be null for " + zone.name(), zone.getId());
			assertNotNull("Zone name should not be null for " + zone.name(), zone.getName());
			assertNotNull("Surface point should not be null for " + zone.name(), zone.getSurfacePoint());
			assertNotNull("Underground point should not be null for " + zone.name(), zone.getUndergroundPoint());
			assertNotNull("Description should not be null for " + zone.name(), zone.getDescription());
			assertTrue("Radius should be positive for " + zone.name(), zone.getRadius() > 0);

			if (zone.getYOffset() == 6400)
			{
				assertTrue("Standard mirror dungeon " + zone.name() + " underground Y should be within regional proximity of surface Y + 6400",
					Math.abs((zone.getSurfacePoint().getY() + 6400) - zone.getUndergroundPoint().getY()) < 150);
			}
		}
	}

	@Test
	public void testSurfaceSpatialLookup()
	{
		// Test Lumbridge surface point lookup
		UndergroundZone lumbridge = UndergroundZone.forSurfacePoint(3218, 3218, 50);
		assertNotNull("Should find Lumbridge Cellar near Lumbridge Castle", lumbridge);
		assertEquals("lumbridge_cellar", lumbridge.getId());

		// Test Varrock surface point lookup
		UndergroundZone varrock = UndergroundZone.forSurfacePoint(3237, 3459, 50);
		assertNotNull("Should find Varrock Sewers near Varrock", varrock);
		assertEquals("varrock_sewers", varrock.getId());

		// Test Taverley surface point lookup
		UndergroundZone taverley = UndergroundZone.forSurfacePoint(2884, 3398, 50);
		assertNotNull("Should find Taverley Dungeon near Taverley", taverley);
		assertEquals("taverley_dungeon", taverley.getId());

		// Test distant point returns null
		UndergroundZone farAway = UndergroundZone.forSurfacePoint(100, 100, 50);
		assertNull("Distant coordinate should return null", farAway);

		assertEquals("zanaris", UndergroundZone.forSurfacePoint(3201, 3169, 50).getId());
		assertEquals("mole_hole", UndergroundZone.forSurfacePoint(2985, 3387, 50).getId());
		assertEquals("camdozaal", UndergroundZone.forSurfacePoint(2998, 3493, 50).getId());
		assertEquals("wizards_tower_basement", UndergroundZone.forSurfacePoint(3104, 3162, 40).getId());
		assertEquals("tolnas_rift", UndergroundZone.forSurfacePoint(3309, 3450, 40).getId());
		assertEquals("sourhog_cave", UndergroundZone.forSurfacePoint(3149, 3347, 40).getId());
		assertEquals("paterdomus_basement", UndergroundZone.forSurfacePoint(3405, 3506, 40).getId());
		assertEquals("water_mill_cellar", UndergroundZone.forSurfacePoint(3166, 3307, 40).getId());
		assertEquals("digsite_dungeon", UndergroundZone.forSurfacePoint(3354, 3425, 40).getId());
		assertEquals("asgarnia_ice_cave", UndergroundZone.forSurfacePoint(3008, 3150, 40).getId());
		assertEquals("dwarven_mines", UndergroundZone.forSurfacePoint(3018, 3450, 40).getId());
		assertEquals("mining_guild", UndergroundZone.forSurfacePoint(3019, 3339, 40).getId());
		assertEquals("heroes_guild_mine", UndergroundZone.forSurfacePoint(2892, 3507, 40).getId());
		assertEquals("melzars_maze_basement", UndergroundZone.forSurfacePoint(2924, 3250, 40).getId());
		assertEquals("motherlode_mine", UndergroundZone.forSurfacePoint(3060, 3366, 40).getId());
		assertEquals("camdozaal", UndergroundZone.forSurfacePoint(2998, 3493, 40).getId());
		assertEquals("scrubfoot_cave", UndergroundZone.forSurfacePoint(2946, 3507, 40).getId());
		assertEquals("taverley_dungeon", UndergroundZone.forSurfacePoint(2884, 3398, 40).getId());
		assertEquals("warriors_guild_basement", UndergroundZone.forSurfacePoint(2832, 3542, 40).getId());
		assertEquals("white_knights_castle_crypt", UndergroundZone.forSurfacePoint(2964, 3331, 40).getId());
		assertEquals("white_wolf_tunnel", UndergroundZone.forSurfacePoint(2876, 3480, 40).getId());
		assertEquals("ancient_cavern", UndergroundZone.forSurfacePoint(2511, 3511, 40).getId());
		assertEquals("ardougne_underground", UndergroundZone.forSurfacePoint(2632, 3294, 40).getId());
		assertEquals("witchaven_dungeon", UndergroundZone.forSurfacePoint(2696, 3283, 40).getId());
		assertEquals("underground_pass", UndergroundZone.forSurfacePoint(2433, 3315, 40).getId());
		assertEquals("brimstail_cave", UndergroundZone.forSurfacePoint(2402, 3419, 40).getId());
		assertEquals("stronghold_slayer_dungeon", UndergroundZone.forSurfacePoint(2430, 3424, 40).getId());
		assertEquals("chaos_druid_tower", UndergroundZone.forSurfacePoint(2562, 3356, 40).getId());
		assertEquals("clock_tower_dungeon", UndergroundZone.forSurfacePoint(2568, 3229, 40).getId());
		assertEquals("elemental_workshop", UndergroundZone.forSurfacePoint(2709, 3498, 40).getId());
		assertEquals("glarials_tomb", UndergroundZone.forSurfacePoint(2557, 3444, 40).getId());
		assertEquals("goblin_cave", UndergroundZone.forSurfacePoint(2622, 3393, 40).getId());
		assertEquals("temple_of_ikov", UndergroundZone.forSurfacePoint(2676, 3404, 40).getId());
		assertEquals("legends_guild_dungeon", UndergroundZone.forSurfacePoint(2724, 3375, 8).getId());
		assertEquals("kraken_cove", UndergroundZone.forSurfacePoint(2278, 3611, 40).getId());
		assertEquals("ourania_altar", UndergroundZone.forSurfacePoint(2451, 3231, 40).getId());
		assertEquals("smoke_devil_dungeon", UndergroundZone.forSurfacePoint(2412, 3061, 40).getId());
		assertEquals("tower_of_life_basement", UndergroundZone.forSurfacePoint(2649, 3213, 40).getId());
		assertEquals("tree_gnome_village_dungeon", UndergroundZone.forSurfacePoint(2533, 3155, 40).getId());
		assertEquals("chasm_of_tears", UndergroundZone.forSurfacePoint(3219, 3132, 40).getId());
		assertEquals("dorgesh_kaan_south", UndergroundZone.forSurfacePoint(3193, 3112, 40).getId());
	}

	@Test
	public void tolnasRiftUsesClipOverrideNotGiantWikiBox()
	{
		assertTrue(UndergroundZone.TOLNAS_RIFT.hasClipOverride());
		assertTrue(UndergroundZone.TOLNAS_RIFT.getClipMaxX() - UndergroundZone.TOLNAS_RIFT.getClipMinX() < 40);
	}

	@Test
	public void chasmOfTearsUsesClipOverrideNotGiantWikiBox()
	{
		assertTrue(UndergroundZone.CHASM_OF_TEARS.hasClipOverride());
		assertTrue(UndergroundZone.CHASM_OF_TEARS.getClipMaxX() - UndergroundZone.CHASM_OF_TEARS.getClipMinX() < 80);
		assertTrue(UndergroundZone.CHASM_OF_TEARS.getClipMaxY() - UndergroundZone.CHASM_OF_TEARS.getClipMinY() < 80);
	}

	@Test
	public void asgarnianIceUsesClipOverrideNotGiantWikiBox()
	{
		assertTrue(UndergroundZone.ASGARNIA_ICE_CAVE.hasClipOverride());
		assertTrue(UndergroundZone.ASGARNIA_ICE_CAVE.getClipMaxX() - UndergroundZone.ASGARNIA_ICE_CAVE.getClipMinX() < 160);
		assertTrue(UndergroundZone.ASGARNIA_ICE_CAVE.getClipMinX() >= 2944);
	}

	@Test
	public void dwarvenMineUsesClipOverrideNotGiantWikiBox()
	{
		assertTrue(UndergroundZone.DWARVEN_MINES.hasClipOverride());
		assertTrue(UndergroundZone.DWARVEN_MINES.getClipMinX() >= 2979);
		assertTrue(UndergroundZone.DWARVEN_MINES.getClipMinY() >= 9757);
	}

	@Test
	public void miningGuildUsesClipOverrideNotGiantWikiBox()
	{
		assertTrue(UndergroundZone.MINING_GUILD.hasClipOverride());
		assertTrue(UndergroundZone.MINING_GUILD.getClipMaxY() <= 9756);
	}

	@Test
	public void heroesGuildMineUsesClipOverrideNotGiantWikiBox()
	{
		assertTrue(UndergroundZone.HEROES_GUILD_MINE.hasClipOverride());
		assertTrue(UndergroundZone.HEROES_GUILD_MINE.getClipMinX() >= 2884);
	}

	@Test
	public void moleHoleUsesClipOverrideNotGiantWikiBox()
	{
		assertTrue(UndergroundZone.MOLE_HOLE.hasClipOverride());
		assertTrue(UndergroundZone.MOLE_HOLE.getClipMaxX() - UndergroundZone.MOLE_HOLE.getClipMinX() < 80);
		assertTrue(UndergroundZone.MOLE_HOLE.getClipMaxY() - UndergroundZone.MOLE_HOLE.getClipMinY() < 130);
		assertTrue(UndergroundZone.MOLE_HOLE.getClipMinY() >= 5131);
		assertTrue(UndergroundZone.MOLE_HOLE.getClipMaxY() <= 5244);
	}

	@Test
	public void motherlodeUsesClipOverrideNotGiantWikiBox()
	{
		assertTrue(UndergroundZone.MOTHERLODE_MINE.hasClipOverride());
		assertTrue(UndergroundZone.MOTHERLODE_MINE.getClipMaxX() - UndergroundZone.MOTHERLODE_MINE.getClipMinX() < 40);
		assertTrue(UndergroundZone.MOTHERLODE_MINE.getClipMaxY() - UndergroundZone.MOTHERLODE_MINE.getClipMinY() < 40);
	}

	@Test
	public void camdozaalUsesClipOverrideNotGiantWikiBox()
	{
		assertTrue(UndergroundZone.CAMDOZAAL.hasClipOverride());
		assertTrue(UndergroundZone.CAMDOZAAL.getClipMaxX() - UndergroundZone.CAMDOZAAL.getClipMinX() < 160);
		assertTrue(UndergroundZone.CAMDOZAAL.getClipMaxY() - UndergroundZone.CAMDOZAAL.getClipMinY() < 120);
	}

	@Test
	public void scrubfootUsesClipOverrideNotGiantWikiBox()
	{
		assertTrue(UndergroundZone.SCRUBFOOT_CAVE.hasClipOverride());
		assertTrue(UndergroundZone.SCRUBFOOT_CAVE.getClipMinX() >= 2944);
		assertTrue(UndergroundZone.SCRUBFOOT_CAVE.getClipMaxX() - UndergroundZone.SCRUBFOOT_CAVE.getClipMinX() < 40);
	}

	@Test
	public void taverleyUsesClipOverrideNotGiantWikiBox()
	{
		assertTrue(UndergroundZone.TAVERLEY_DUNGEON.hasClipOverride());
		assertTrue(UndergroundZone.TAVERLEY_DUNGEON.getClipMaxX() <= 2978);
		assertTrue(UndergroundZone.TAVERLEY_DUNGEON.getClipMaxY() <= 9858);
	}

	@Test
	public void redChinchompaHuntingGroundUsesClipOverride()
	{
		assertTrue(UndergroundZone.RED_CHINCHOMPA_HUNTING_GROUND.hasClipOverride());
		assertEquals(2508, UndergroundZone.RED_CHINCHOMPA_HUNTING_GROUND.getClipMinX());
		assertEquals(2535, UndergroundZone.RED_CHINCHOMPA_HUNTING_GROUND.getClipMaxX());
		assertEquals(9284, UndergroundZone.RED_CHINCHOMPA_HUNTING_GROUND.getClipMinY());
		assertEquals(9308, UndergroundZone.RED_CHINCHOMPA_HUNTING_GROUND.getClipMaxY());
	}

	@Test
	public void tzhaarCityUsesClipOverride()
	{
		assertTrue(UndergroundZone.TZHAAR_CITY.hasClipOverride());
		assertEquals(2250, UndergroundZone.TZHAAR_CITY.getClipMinX());
		assertEquals(2590, UndergroundZone.TZHAAR_CITY.getClipMaxX());
		assertEquals(5030, UndergroundZone.TZHAAR_CITY.getClipMinY());
		assertEquals(5370, UndergroundZone.TZHAAR_CITY.getClipMaxY());
	}

	@Test
	public void karamjaAndCrandorDungeonUsesClipOverride()
	{
		assertTrue(UndergroundZone.KARAMJA_DUNGEON.hasClipOverride());
		assertEquals(2810, UndergroundZone.KARAMJA_DUNGEON.getClipMinX());
		assertEquals(2885, UndergroundZone.KARAMJA_DUNGEON.getClipMaxX());
		assertEquals(9500, UndergroundZone.KARAMJA_DUNGEON.getClipMinY());
		assertEquals(9695, UndergroundZone.KARAMJA_DUNGEON.getClipMaxY());
	}

	@Test
	public void brimhavenDungeonUsesClipOverride()
	{
		assertTrue(UndergroundZone.BRIMHAVEN_DUNGEON.hasClipOverride());
		assertEquals(2630, UndergroundZone.BRIMHAVEN_DUNGEON.getClipMinX());
		assertEquals(2760, UndergroundZone.BRIMHAVEN_DUNGEON.getClipMaxX());
		assertEquals(9425, UndergroundZone.BRIMHAVEN_DUNGEON.getClipMinY());
		assertEquals(9600, UndergroundZone.BRIMHAVEN_DUNGEON.getClipMaxY());
	}

	@Test
	public void waterbirthDungeonUsesClipOverride()
	{
		assertTrue(UndergroundZone.WATERBIRTH_DUNGEON.hasClipOverride());
		assertEquals(2440, UndergroundZone.WATERBIRTH_DUNGEON.getClipMinX());
		assertEquals(2650, UndergroundZone.WATERBIRTH_DUNGEON.getClipMaxX());
		assertEquals(9600, UndergroundZone.WATERBIRTH_DUNGEON.getClipMinY());
		assertEquals(10200, UndergroundZone.WATERBIRTH_DUNGEON.getClipMaxY());
	}

	@Test
	public void fremennikSlayerDungeonUsesClipOverride()
	{
		assertTrue(UndergroundZone.FREMENNIK_SLAYER_DUNGEON.hasClipOverride());
		assertEquals(2688, UndergroundZone.FREMENNIK_SLAYER_DUNGEON.getClipMinX());
		assertEquals(2812, UndergroundZone.FREMENNIK_SLAYER_DUNGEON.getClipMaxX());
		assertEquals(9960, UndergroundZone.FREMENNIK_SLAYER_DUNGEON.getClipMinY());
		assertEquals(10045, UndergroundZone.FREMENNIK_SLAYER_DUNGEON.getClipMaxY());
	}

	@Test
	public void crashIslandDungeonUsesClipOverride()
	{
		assertTrue(UndergroundZone.CRASH_ISLAND_DUNGEON.hasClipOverride());
		assertEquals(2892, UndergroundZone.CRASH_ISLAND_DUNGEON.getClipMinX());
		assertEquals(2927, UndergroundZone.CRASH_ISLAND_DUNGEON.getClipMaxX());
		assertEquals(9092, UndergroundZone.CRASH_ISLAND_DUNGEON.getClipMinY());
		assertEquals(9147, UndergroundZone.CRASH_ISLAND_DUNGEON.getClipMaxY());
	}

	@Test
	public void keldagrimUsesClipOverride()
	{
		assertTrue(UndergroundZone.KELDAGRIM.hasClipOverride());
		assertEquals(2760, UndergroundZone.KELDAGRIM.getClipMinX());
		assertEquals(2943, UndergroundZone.KELDAGRIM.getClipMaxX());
		assertEquals(10100, UndergroundZone.KELDAGRIM.getClipMinY());
		assertEquals(10239, UndergroundZone.KELDAGRIM.getClipMaxY());
	}

	@Test
	public void miscellaniaUndergroundUsesClipOverride()
	{
		assertTrue(UndergroundZone.MISCELLANIA_UNDERGROUND.hasClipOverride());
		assertEquals(2500, UndergroundZone.MISCELLANIA_UNDERGROUND.getClipMinX());
		assertEquals(2623, UndergroundZone.MISCELLANIA_UNDERGROUND.getClipMaxX());
		assertEquals(10240, UndergroundZone.MISCELLANIA_UNDERGROUND.getClipMinY());
		assertEquals(10303, UndergroundZone.MISCELLANIA_UNDERGROUND.getClipMaxY());
	}

	@Test
	public void missingLinkZonesUseClipOverrides()
	{
		assertTrue(UndergroundZone.FENKENSTRAIN_DUNGEON.hasClipOverride());
		assertEquals(3520, UndergroundZone.FENKENSTRAIN_DUNGEON.getClipMinX());
		assertEquals(3583, UndergroundZone.FENKENSTRAIN_DUNGEON.getClipMaxX());
		assertEquals(9856, UndergroundZone.FENKENSTRAIN_DUNGEON.getClipMinY());
		assertEquals(9919, UndergroundZone.FENKENSTRAIN_DUNGEON.getClipMaxY());

		assertTrue(UndergroundZone.WILDERNESS_AGILITY_DUNGEON.hasClipOverride());
		assertEquals(2991, UndergroundZone.WILDERNESS_AGILITY_DUNGEON.getClipMinX());
		assertEquals(3071, UndergroundZone.WILDERNESS_AGILITY_DUNGEON.getClipMaxX());
		assertEquals(10339, UndergroundZone.WILDERNESS_AGILITY_DUNGEON.getClipMinY());
		assertEquals(10367, UndergroundZone.WILDERNESS_AGILITY_DUNGEON.getClipMaxY());

		assertTrue(UndergroundZone.DAEYALT_ESSENCE_MINE.hasClipOverride());
		assertEquals(3609, UndergroundZone.DAEYALT_ESSENCE_MINE.getClipMinX());
		assertEquals(3710, UndergroundZone.DAEYALT_ESSENCE_MINE.getClipMaxX());
		assertEquals(9795, UndergroundZone.DAEYALT_ESSENCE_MINE.getClipMinY());
		assertEquals(9850, UndergroundZone.DAEYALT_ESSENCE_MINE.getClipMaxY());

		assertTrue(UndergroundZone.MOLCH_PEARL_CAVES.hasClipOverride());
		assertEquals(1280, UndergroundZone.MOLCH_PEARL_CAVES.getClipMinX());
		assertEquals(1343, UndergroundZone.MOLCH_PEARL_CAVES.getClipMaxX());
		assertEquals(9935, UndergroundZone.MOLCH_PEARL_CAVES.getClipMinY());
		assertEquals(9983, UndergroundZone.MOLCH_PEARL_CAVES.getClipMaxY());

		assertTrue(UndergroundZone.POISON_WASTE_DUNGEON.hasClipOverride());
		assertEquals(1349, UndergroundZone.POISON_WASTE_DUNGEON.getClipMinX());
		assertEquals(1535, UndergroundZone.POISON_WASTE_DUNGEON.getClipMaxX());
		assertEquals(4224, UndergroundZone.POISON_WASTE_DUNGEON.getClipMinY());
		assertEquals(4351, UndergroundZone.POISON_WASTE_DUNGEON.getClipMaxY());

		assertTrue(UndergroundZone.SISTERHOOD_SANCTUARY.hasClipOverride());
		assertEquals(3715, UndergroundZone.SISTERHOOD_SANCTUARY.getClipMinX());
		assertEquals(3903, UndergroundZone.SISTERHOOD_SANCTUARY.getClipMaxX());

		assertTrue(UndergroundZone.JORMUNGANDS_PRISON.hasClipOverride());
		assertEquals(2398, UndergroundZone.JORMUNGANDS_PRISON.getClipMinX());
		assertEquals(2493, UndergroundZone.JORMUNGANDS_PRISON.getClipMaxX());

		assertTrue(UndergroundZone.GHORROCK_DUNGEON.hasClipOverride());
		assertEquals(2844, UndergroundZone.GHORROCK_DUNGEON.getClipMinX());
		assertEquals(2932, UndergroundZone.GHORROCK_DUNGEON.getClipMaxX());
		assertTrue(UndergroundZone.LIGHTHOUSE_DUNGEON.hasClipOverride());
		assertEquals(2498, UndergroundZone.LIGHTHOUSE_DUNGEON.getClipMinX());
		assertEquals(2542, UndergroundZone.LIGHTHOUSE_DUNGEON.getClipMaxX());
		assertEquals(9986, UndergroundZone.LIGHTHOUSE_DUNGEON.getClipMinY());
		assertEquals(10041, UndergroundZone.LIGHTHOUSE_DUNGEON.getClipMaxY());

		assertTrue(UndergroundZone.TOMBS_OF_AMASCUT.hasClipOverride());
		assertEquals(3344, UndergroundZone.TOMBS_OF_AMASCUT.getClipMinX());
		assertEquals(3375, UndergroundZone.TOMBS_OF_AMASCUT.getClipMaxX());
		assertEquals(9102, UndergroundZone.TOMBS_OF_AMASCUT.getClipMinY());
		assertEquals(9130, UndergroundZone.TOMBS_OF_AMASCUT.getClipMaxY());

		assertTrue(UndergroundZone.LIZARDMAN_CAVES.hasClipOverride());
		assertEquals(1280, UndergroundZone.LIZARDMAN_CAVES.getClipMinX());
		assertEquals(1343, UndergroundZone.LIZARDMAN_CAVES.getClipMaxX());
		assertEquals(9935, UndergroundZone.LIZARDMAN_CAVES.getClipMinY());
		assertEquals(9984, UndergroundZone.LIZARDMAN_CAVES.getClipMaxY());

		assertTrue(UndergroundZone.LIZARDMAN_TEMPLE.hasClipOverride());
		assertEquals(1281, UndergroundZone.LIZARDMAN_TEMPLE.getClipMinX());
		assertEquals(1342, UndergroundZone.LIZARDMAN_TEMPLE.getClipMaxX());
		assertEquals(10048, UndergroundZone.LIZARDMAN_TEMPLE.getClipMinY());
		assertEquals(10111, UndergroundZone.LIZARDMAN_TEMPLE.getClipMaxY());

		assertTrue(UndergroundZone.GIANTS_DEN.hasClipOverride());
		assertEquals(1412, UndergroundZone.GIANTS_DEN.getClipMinX());
		assertEquals(1471, UndergroundZone.GIANTS_DEN.getClipMaxX());
		assertEquals(9856, UndergroundZone.GIANTS_DEN.getClipMinY());
		assertEquals(9915, UndergroundZone.GIANTS_DEN.getClipMaxY());

		assertTrue(UndergroundZone.FORTIS_BARRACKS.hasClipOverride());
		assertEquals(1612, UndergroundZone.FORTIS_BARRACKS.getClipMinX());
		assertEquals(1691, UndergroundZone.FORTIS_BARRACKS.getClipMaxX());
		assertEquals(9484, UndergroundZone.FORTIS_BARRACKS.getClipMinY());
		assertEquals(9579, UndergroundZone.FORTIS_BARRACKS.getClipMaxY());

		assertTrue(UndergroundZone.HUNTER_GUILD.hasClipOverride());
		assertEquals(1524, UndergroundZone.HUNTER_GUILD.getClipMinX());
		assertEquals(1579, UndergroundZone.HUNTER_GUILD.getClipMaxX());
		assertEquals(9408, UndergroundZone.HUNTER_GUILD.getClipMinY());
		assertEquals(9471, UndergroundZone.HUNTER_GUILD.getClipMaxY());

		assertTrue(UndergroundZone.LITHKREN_VAULT.hasClipOverride());
		assertEquals(3540, UndergroundZone.LITHKREN_VAULT.getClipMinX());
		assertEquals(3560, UndergroundZone.LITHKREN_VAULT.getClipMaxX());
		assertEquals(10440, UndergroundZone.LITHKREN_VAULT.getClipMinY());
		assertEquals(10488, UndergroundZone.LITHKREN_VAULT.getClipMaxY());

		assertTrue(UndergroundZone.WEISS_SALT_MINE.hasClipOverride());
		assertEquals(2824, UndergroundZone.WEISS_SALT_MINE.getClipMinX());
		assertEquals(2860, UndergroundZone.WEISS_SALT_MINE.getClipMaxX());
		assertEquals(10324, UndergroundZone.WEISS_SALT_MINE.getClipMinY());
		assertEquals(10356, UndergroundZone.WEISS_SALT_MINE.getClipMaxY());

		assertTrue(UndergroundZone.JALDRAOCHT_PYRAMID.hasClipOverride());
		assertEquals(2865, UndergroundZone.JALDRAOCHT_PYRAMID.getClipMinX());
		assertEquals(2962, UndergroundZone.JALDRAOCHT_PYRAMID.getClipMaxX());
		assertEquals(4906, UndergroundZone.JALDRAOCHT_PYRAMID.getClipMinY());
		assertEquals(5003, UndergroundZone.JALDRAOCHT_PYRAMID.getClipMaxY());

		assertTrue(UndergroundZone.RIVER_ELID_DUNGEON.hasClipOverride());
		assertEquals(3338, UndergroundZone.RIVER_ELID_DUNGEON.getClipMinX());
		assertEquals(3365, UndergroundZone.RIVER_ELID_DUNGEON.getClipMaxX());
		assertEquals(9528, UndergroundZone.RIVER_ELID_DUNGEON.getClipMinY());
		assertEquals(9561, UndergroundZone.RIVER_ELID_DUNGEON.getClipMaxY());

		assertTrue(UndergroundZone.SHADOW_DUNGEON.hasClipOverride());
		assertEquals(2624, UndergroundZone.SHADOW_DUNGEON.getClipMinX());
		assertEquals(2752, UndergroundZone.SHADOW_DUNGEON.getClipMaxX());
		assertEquals(5056, UndergroundZone.SHADOW_DUNGEON.getClipMinY());
		assertEquals(5120, UndergroundZone.SHADOW_DUNGEON.getClipMaxY());

		assertTrue(UndergroundZone.DRAYNOR_MANOR_BASEMENT.hasClipOverride());
		assertEquals(3088, UndergroundZone.DRAYNOR_MANOR_BASEMENT.getClipMinX());
		assertEquals(3120, UndergroundZone.DRAYNOR_MANOR_BASEMENT.getClipMaxX());
		assertEquals(9744, UndergroundZone.DRAYNOR_MANOR_BASEMENT.getClipMinY());
		assertEquals(9768, UndergroundZone.DRAYNOR_MANOR_BASEMENT.getClipMaxY());

		assertTrue(UndergroundZone.ENAKHRAS_TEMPLE.hasClipOverride());
		assertEquals(3072, UndergroundZone.ENAKHRAS_TEMPLE.getClipMinX());
		assertEquals(3201, UndergroundZone.ENAKHRAS_TEMPLE.getClipMaxX());
		assertEquals(9280, UndergroundZone.ENAKHRAS_TEMPLE.getClipMinY());
		assertEquals(9400, UndergroundZone.ENAKHRAS_TEMPLE.getClipMaxY());

		assertTrue(UndergroundZone.PORT_SARIM_RAT_PITS.hasClipOverride());
		assertEquals(2952, UndergroundZone.PORT_SARIM_RAT_PITS.getClipMinX());
		assertEquals(2981, UndergroundZone.PORT_SARIM_RAT_PITS.getClipMaxX());
		assertEquals(9624, UndergroundZone.PORT_SARIM_RAT_PITS.getClipMinY());
		assertEquals(9660, UndergroundZone.PORT_SARIM_RAT_PITS.getClipMaxY());

		assertTrue(UndergroundZone.KING_BLACK_DRAGON_LAIR.hasClipOverride());
		assertEquals(2252, UndergroundZone.KING_BLACK_DRAGON_LAIR.getClipMinX());
		assertEquals(2292, UndergroundZone.KING_BLACK_DRAGON_LAIR.getClipMaxX());
		assertEquals(4676, UndergroundZone.KING_BLACK_DRAGON_LAIR.getClipMinY());
		assertEquals(4716, UndergroundZone.KING_BLACK_DRAGON_LAIR.getClipMaxY());

		assertTrue(UndergroundZone.HESPORI_CAVE.hasClipOverride());
		assertEquals(1236, UndergroundZone.HESPORI_CAVE.getClipMinX());
		assertEquals(1260, UndergroundZone.HESPORI_CAVE.getClipMaxX());
		assertEquals(10076, UndergroundZone.HESPORI_CAVE.getClipMinY());
		assertEquals(10100, UndergroundZone.HESPORI_CAVE.getClipMaxY());

		assertTrue(UndergroundZone.STALKER_DEN.hasClipOverride());
		assertEquals(1240, UndergroundZone.STALKER_DEN.getClipMinX());
		assertEquals(1340, UndergroundZone.STALKER_DEN.getClipMaxX());
		assertEquals(9728, UndergroundZone.STALKER_DEN.getClipMinY());
		assertEquals(9908, UndergroundZone.STALKER_DEN.getClipMaxY());
	}

	@Test
	public void islandZonesUseClipOverrides()
	{
		assertTrue(UndergroundZone.FOSSIL_ISLAND_UNDERGROUND.hasClipOverride());
		assertEquals(3600, UndergroundZone.FOSSIL_ISLAND_UNDERGROUND.getClipMinX());
		assertEquals(3839, UndergroundZone.FOSSIL_ISLAND_UNDERGROUND.getClipMaxX());
		assertEquals(10122, UndergroundZone.FOSSIL_ISLAND_UNDERGROUND.getClipMinY());
		assertEquals(10303, UndergroundZone.FOSSIL_ISLAND_UNDERGROUND.getClipMaxY());

		assertTrue(UndergroundZone.MOS_LE_HARMLESS_CAVE.hasClipOverride());
		assertEquals(3715, UndergroundZone.MOS_LE_HARMLESS_CAVE.getClipMinX());
		assertEquals(3838, UndergroundZone.MOS_LE_HARMLESS_CAVE.getClipMaxX());
		assertEquals(9348, UndergroundZone.MOS_LE_HARMLESS_CAVE.getClipMinY());
		assertEquals(9470, UndergroundZone.MOS_LE_HARMLESS_CAVE.getClipMaxY());

		assertTrue(UndergroundZone.APE_ATOLL_DUNGEON.hasClipOverride());
		assertEquals(2309, UndergroundZone.APE_ATOLL_DUNGEON.getClipMinX());
		assertEquals(2815, UndergroundZone.APE_ATOLL_DUNGEON.getClipMaxX());
		assertEquals(9088, UndergroundZone.APE_ATOLL_DUNGEON.getClipMinY());
		assertEquals(9277, UndergroundZone.APE_ATOLL_DUNGEON.getClipMaxY());

		assertTrue(UndergroundZone.ISLE_OF_SOULS_DUNGEON.hasClipOverride());
		assertEquals(2113, UndergroundZone.ISLE_OF_SOULS_DUNGEON.getClipMinX());
		assertEquals(2173, UndergroundZone.ISLE_OF_SOULS_DUNGEON.getClipMaxX());
		assertEquals(9285, UndergroundZone.ISLE_OF_SOULS_DUNGEON.getClipMinY());
		assertEquals(9341, UndergroundZone.ISLE_OF_SOULS_DUNGEON.getClipMaxY());
	}

	@Test
	public void varlamoreZonesUseClipOverrides()
	{
		assertTrue(UndergroundZone.CAM_TORUM.hasClipOverride());
		assertEquals(1240, UndergroundZone.CAM_TORUM.getClipMinX());
		assertEquals(1580, UndergroundZone.CAM_TORUM.getClipMaxX());
		assertEquals(9440, UndergroundZone.CAM_TORUM.getClipMinY());
		assertEquals(9614, UndergroundZone.CAM_TORUM.getClipMaxY());

		assertTrue(UndergroundZone.NEYPOTZLI.hasClipOverride());
		assertEquals(1344, UndergroundZone.NEYPOTZLI.getClipMinX());
		assertEquals(1535, UndergroundZone.NEYPOTZLI.getClipMaxX());
		assertEquals(9614, UndergroundZone.NEYPOTZLI.getClipMinY());
		assertEquals(9742, UndergroundZone.NEYPOTZLI.getClipMaxY());

		assertTrue(UndergroundZone.TONALI_CAVERN.hasClipOverride());
		assertEquals(1280, UndergroundZone.TONALI_CAVERN.getClipMinX());
		assertEquals(1343, UndergroundZone.TONALI_CAVERN.getClipMaxX());
		assertEquals(9344, UndergroundZone.TONALI_CAVERN.getClipMinY());
		assertEquals(9423, UndergroundZone.TONALI_CAVERN.getClipMaxY());

		assertTrue(UndergroundZone.DRAGON_NEST.hasClipOverride());
		assertEquals(1216, UndergroundZone.DRAGON_NEST.getClipMinX());
		assertEquals(1271, UndergroundZone.DRAGON_NEST.getClipMaxX());
		assertEquals(9472, UndergroundZone.DRAGON_NEST.getClipMinY());
		assertEquals(9535, UndergroundZone.DRAGON_NEST.getClipMaxY());
	}

	@Test
	public void tonaliCavernAndDragonNestAlignment()
	{
		assertEquals(0, UndergroundZone.TONALI_CAVERN.getDeltaX());
		assertEquals(6310, UndergroundZone.TONALI_CAVERN.getDeltaY());
		assertEquals(6310, UndergroundZone.TONALI_CAVERN.getYOffset());
		assertEquals(2, UndergroundZone.TONALI_CAVERN.getSurfacePoints().size());
		assertEquals(1309, UndergroundZone.TONALI_CAVERN.getSurfacePoint().getX());
		assertEquals(3104, UndergroundZone.TONALI_CAVERN.getSurfacePoint().getY());
		assertEquals(1305, UndergroundZone.TONALI_CAVERN.getSurfacePoints().get(1).getX());
		assertEquals(3033, UndergroundZone.TONALI_CAVERN.getSurfacePoints().get(1).getY());

		assertEquals(-42, UndergroundZone.DRAGON_NEST.getDeltaX());
		assertEquals(6369, UndergroundZone.DRAGON_NEST.getDeltaY());
		assertEquals(6369, UndergroundZone.DRAGON_NEST.getYOffset());
		assertEquals(1289, UndergroundZone.DRAGON_NEST.getSurfacePoint().getX());
		assertEquals(3134, UndergroundZone.DRAGON_NEST.getSurfacePoint().getY());
		assertEquals(1247, UndergroundZone.DRAGON_NEST.getUndergroundPoint().getX());
		assertEquals(9503, UndergroundZone.DRAGON_NEST.getUndergroundPoint().getY());
	}

	@Test
	public void kourendZonesUseClipOverrides()
	{
		assertTrue(UndergroundZone.CATACOMBS_OF_KOUREND.hasClipOverride());
		assertEquals(1590, UndergroundZone.CATACOMBS_OF_KOUREND.getClipMinX());
		assertEquals(1736, UndergroundZone.CATACOMBS_OF_KOUREND.getClipMaxX());
		assertEquals(9979, UndergroundZone.CATACOMBS_OF_KOUREND.getClipMinY());
		assertEquals(10111, UndergroundZone.CATACOMBS_OF_KOUREND.getClipMaxY());

		assertTrue(UndergroundZone.FORTHOS_DUNGEON.hasClipOverride());
		assertEquals(1781, UndergroundZone.FORTHOS_DUNGEON.getClipMinX());
		assertEquals(1866, UndergroundZone.FORTHOS_DUNGEON.getClipMaxX());
		assertEquals(9881, UndergroundZone.FORTHOS_DUNGEON.getClipMinY());
		assertEquals(9994, UndergroundZone.FORTHOS_DUNGEON.getClipMaxY());

		assertTrue(UndergroundZone.CRABCLAW_CAVES.hasClipOverride());
		assertEquals(1635, UndergroundZone.CRABCLAW_CAVES.getClipMinX());
		assertEquals(1724, UndergroundZone.CRABCLAW_CAVES.getClipMaxX());
		assertEquals(9800, UndergroundZone.CRABCLAW_CAVES.getClipMinY());
		assertEquals(9855, UndergroundZone.CRABCLAW_CAVES.getClipMaxY());

		assertTrue(UndergroundZone.CHASM_OF_FIRE.hasClipOverride());
		assertEquals(1409, UndergroundZone.CHASM_OF_FIRE.getClipMinX());
		assertEquals(1468, UndergroundZone.CHASM_OF_FIRE.getClipMaxX());
		assertEquals(10048, UndergroundZone.CHASM_OF_FIRE.getClipMinY());
		assertEquals(10110, UndergroundZone.CHASM_OF_FIRE.getClipMaxY());

		assertTrue(UndergroundZone.THE_WARRENS.hasClipOverride());
		assertEquals(1727, UndergroundZone.THE_WARRENS.getClipMinX());
		assertEquals(1816, UndergroundZone.THE_WARRENS.getClipMaxX());
		assertEquals(10109, UndergroundZone.THE_WARRENS.getClipMinY());
		assertEquals(10176, UndergroundZone.THE_WARRENS.getClipMaxY());

		assertTrue(UndergroundZone.KARUULM_SLAYER_DUNGEON.hasClipOverride());
		assertEquals(1254, UndergroundZone.KARUULM_SLAYER_DUNGEON.getClipMinX());
		assertEquals(1380, UndergroundZone.KARUULM_SLAYER_DUNGEON.getClipMaxX());
		assertEquals(10174, UndergroundZone.KARUULM_SLAYER_DUNGEON.getClipMinY());
		assertEquals(10281, UndergroundZone.KARUULM_SLAYER_DUNGEON.getClipMaxY());

		assertTrue(UndergroundZone.WOODCUTTING_GUILD_DUNGEON.hasClipOverride());
		assertEquals(1540, UndergroundZone.WOODCUTTING_GUILD_DUNGEON.getClipMinX());
		assertEquals(1599, UndergroundZone.WOODCUTTING_GUILD_DUNGEON.getClipMaxX());
		assertEquals(9860, UndergroundZone.WOODCUTTING_GUILD_DUNGEON.getClipMinY());
		assertEquals(9915, UndergroundZone.WOODCUTTING_GUILD_DUNGEON.getClipMaxY());
	}

	@Test
	public void wildyZonesUseClipOverrides()
	{
		assertTrue(UndergroundZone.REVENANT_CAVES.hasClipOverride());
		assertEquals(3144, UndergroundZone.REVENANT_CAVES.getClipMinX());
		assertEquals(3262, UndergroundZone.REVENANT_CAVES.getClipMaxX());
		assertEquals(10053, UndergroundZone.REVENANT_CAVES.getClipMinY());
		assertEquals(10236, UndergroundZone.REVENANT_CAVES.getClipMaxY());

		assertTrue(UndergroundZone.WILDERNESS_SLAYER_CAVE.hasClipOverride());
		assertEquals(3329, UndergroundZone.WILDERNESS_SLAYER_CAVE.getClipMinX());
		assertEquals(3453, UndergroundZone.WILDERNESS_SLAYER_CAVE.getClipMaxX());
		assertEquals(10049, UndergroundZone.WILDERNESS_SLAYER_CAVE.getClipMinY());
		assertEquals(10173, UndergroundZone.WILDERNESS_SLAYER_CAVE.getClipMaxY());

		assertTrue(UndergroundZone.WILDERNESS_GOD_WARS_DUNGEON.hasClipOverride());
		assertEquals(3015, UndergroundZone.WILDERNESS_GOD_WARS_DUNGEON.getClipMinX());
		assertEquals(3067, UndergroundZone.WILDERNESS_GOD_WARS_DUNGEON.getClipMaxX());
		assertEquals(10115, UndergroundZone.WILDERNESS_GOD_WARS_DUNGEON.getClipMinY());
		assertEquals(10166, UndergroundZone.WILDERNESS_GOD_WARS_DUNGEON.getClipMaxY());

		assertTrue(UndergroundZone.LAVA_MAZE_DUNGEON.hasClipOverride());
		assertEquals(3009, UndergroundZone.LAVA_MAZE_DUNGEON.getClipMinX());
		assertEquals(3071, UndergroundZone.LAVA_MAZE_DUNGEON.getClipMaxX());
		assertEquals(10240, UndergroundZone.LAVA_MAZE_DUNGEON.getClipMinY());
		assertEquals(10291, UndergroundZone.LAVA_MAZE_DUNGEON.getClipMaxY());

		assertTrue(UndergroundZone.DEEP_WILDERNESS_DUNGEON.hasClipOverride());
		assertEquals(3008, UndergroundZone.DEEP_WILDERNESS_DUNGEON.getClipMinX());
		assertEquals(3071, UndergroundZone.DEEP_WILDERNESS_DUNGEON.getClipMaxX());
		assertEquals(10304, UndergroundZone.DEEP_WILDERNESS_DUNGEON.getClipMinY());
		assertEquals(10367, UndergroundZone.DEEP_WILDERNESS_DUNGEON.getClipMaxY());

		assertTrue(UndergroundZone.MAGE_ARENA_BANK.hasClipOverride());
		assertEquals(2500, UndergroundZone.MAGE_ARENA_BANK.getClipMinX());
		assertEquals(2556, UndergroundZone.MAGE_ARENA_BANK.getClipMaxX());
		assertEquals(4680, UndergroundZone.MAGE_ARENA_BANK.getClipMinY());
		assertEquals(4730, UndergroundZone.MAGE_ARENA_BANK.getClipMaxY());
	}

	@Test
	public void desertZonesUseClipOverrides()
	{
		assertTrue(UndergroundZone.KALPHITE_LAIR.hasClipOverride());
		assertEquals(3464, UndergroundZone.KALPHITE_LAIR.getClipMinX());
		assertEquals(3511, UndergroundZone.KALPHITE_LAIR.getClipMaxX());
		assertEquals(9480, UndergroundZone.KALPHITE_LAIR.getClipMinY());
		assertEquals(9519, UndergroundZone.KALPHITE_LAIR.getClipMaxY());

		assertTrue(UndergroundZone.KALPHITE_CAVE.hasClipOverride());
		assertEquals(3264, UndergroundZone.KALPHITE_CAVE.getClipMinX());
		assertEquals(3387, UndergroundZone.KALPHITE_CAVE.getClipMaxX());
		assertEquals(9408, UndergroundZone.KALPHITE_CAVE.getClipMinY());
		assertEquals(9565, UndergroundZone.KALPHITE_CAVE.getClipMaxY());

		assertTrue(UndergroundZone.SMOKE_DUNGEON.hasClipOverride());
		assertEquals(3200, UndergroundZone.SMOKE_DUNGEON.getClipMinX());
		assertEquals(3326, UndergroundZone.SMOKE_DUNGEON.getClipMaxX());
		assertEquals(9344, UndergroundZone.SMOKE_DUNGEON.getClipMinY());
		assertEquals(9406, UndergroundZone.SMOKE_DUNGEON.getClipMaxY());

		assertTrue(UndergroundZone.KLENTERS_PYRAMID.hasClipOverride());
		assertEquals(3272, UndergroundZone.KLENTERS_PYRAMID.getClipMinX());
		assertEquals(3312, UndergroundZone.KLENTERS_PYRAMID.getClipMaxX());
		assertEquals(9169, UndergroundZone.KLENTERS_PYRAMID.getClipMinY());
		assertEquals(9205, UndergroundZone.KLENTERS_PYRAMID.getClipMaxY());

		assertTrue(UndergroundZone.DESERT_EAGLE_LAIR.hasClipOverride());
		assertEquals(3412, UndergroundZone.DESERT_EAGLE_LAIR.getClipMinX());
		assertEquals(3435, UndergroundZone.DESERT_EAGLE_LAIR.getClipMaxX());
		assertEquals(9560, UndergroundZone.DESERT_EAGLE_LAIR.getClipMinY());
		assertEquals(9580, UndergroundZone.DESERT_EAGLE_LAIR.getClipMaxY());

		assertTrue(UndergroundZone.SOPHANEM_DUNGEON.hasClipOverride());
		assertEquals(2176, UndergroundZone.SOPHANEM_DUNGEON.getClipMinX());
		assertEquals(2368, UndergroundZone.SOPHANEM_DUNGEON.getClipMaxX());
		assertEquals(4240, UndergroundZone.SOPHANEM_DUNGEON.getClipMinY());
		assertEquals(4347, UndergroundZone.SOPHANEM_DUNGEON.getClipMaxY());

		assertTrue(UndergroundZone.PYRAMID_PLUNDER.hasClipOverride());
		assertEquals(1796, UndergroundZone.PYRAMID_PLUNDER.getClipMinX());
		assertEquals(1980, UndergroundZone.PYRAMID_PLUNDER.getClipMaxX());
		assertEquals(4416, UndergroundZone.PYRAMID_PLUNDER.getClipMinY());
		assertEquals(4543, UndergroundZone.PYRAMID_PLUNDER.getClipMaxY());
	}

	@Test
	public void tirannwnZonesUseClipOverrides()
	{
		assertTrue(UndergroundZone.UNDERGROUND_PASS.hasClipOverride());
		assertEquals(2312, UndergroundZone.UNDERGROUND_PASS.getClipMinX());
		assertEquals(2497, UndergroundZone.UNDERGROUND_PASS.getClipMaxX());
		assertEquals(9601, UndergroundZone.UNDERGROUND_PASS.getClipMinY());
		assertEquals(9728, UndergroundZone.UNDERGROUND_PASS.getClipMaxY());

		assertTrue(UndergroundZone.PRIFDDINAS_GRAND_LIBRARY.hasClipOverride());
		assertEquals(3208, UndergroundZone.PRIFDDINAS_GRAND_LIBRARY.getClipMinX());
		assertEquals(3305, UndergroundZone.PRIFDDINAS_GRAND_LIBRARY.getClipMaxX());
		assertEquals(12510, UndergroundZone.PRIFDDINAS_GRAND_LIBRARY.getClipMinY());
		assertEquals(12590, UndergroundZone.PRIFDDINAS_GRAND_LIBRARY.getClipMaxY());

		assertTrue(UndergroundZone.PRIFDDINAS_UNDERGROUND.hasClipOverride());
		assertEquals(3160, UndergroundZone.PRIFDDINAS_UNDERGROUND.getClipMinX());
		assertEquals(3315, UndergroundZone.PRIFDDINAS_UNDERGROUND.getClipMaxX());
		assertEquals(12400, UndergroundZone.PRIFDDINAS_UNDERGROUND.getClipMinY());
		assertEquals(12480, UndergroundZone.PRIFDDINAS_UNDERGROUND.getClipMaxY());
	}

	@Test
	public void morytaniaZonesUseClipOverrides()
	{
		assertTrue(UndergroundZone.HAUNTED_MINE.hasClipOverride());
		assertEquals(3400, UndergroundZone.HAUNTED_MINE.getClipMinX());
		assertEquals(3447, UndergroundZone.HAUNTED_MINE.getClipMaxX());
		assertEquals(9609, UndergroundZone.HAUNTED_MINE.getClipMinY());
		assertEquals(9662, UndergroundZone.HAUNTED_MINE.getClipMaxY());

		assertTrue(UndergroundZone.BARROWS_CRYPTS.hasClipOverride());
		assertEquals(3512, UndergroundZone.BARROWS_CRYPTS.getClipMinX());
		assertEquals(3640, UndergroundZone.BARROWS_CRYPTS.getClipMaxX());
		assertEquals(9672, UndergroundZone.BARROWS_CRYPTS.getClipMinY());
		assertEquals(9827, UndergroundZone.BARROWS_CRYPTS.getClipMaxY());

		assertTrue(UndergroundZone.MEIYERDITCH_LABORATORIES.hasClipOverride());
		assertEquals(3450, UndergroundZone.MEIYERDITCH_LABORATORIES.getClipMinX());
		assertEquals(3645, UndergroundZone.MEIYERDITCH_LABORATORIES.getClipMaxX());
		assertEquals(9665, UndergroundZone.MEIYERDITCH_LABORATORIES.getClipMinY());
		assertEquals(9950, UndergroundZone.MEIYERDITCH_LABORATORIES.getClipMaxY());

		assertTrue(UndergroundZone.NATURE_GROTTO.hasClipOverride());
		assertEquals(3434, UndergroundZone.NATURE_GROTTO.getClipMinX());
		assertEquals(3449, UndergroundZone.NATURE_GROTTO.getClipMaxX());
		assertEquals(9732, UndergroundZone.NATURE_GROTTO.getClipMinY());
		assertEquals(9747, UndergroundZone.NATURE_GROTTO.getClipMaxY());

		assertTrue(UndergroundZone.ECTOFUNTUS_DUNGEON.hasClipOverride());
		assertEquals(3670, UndergroundZone.ECTOFUNTUS_DUNGEON.getClipMinX());
		assertEquals(3692, UndergroundZone.ECTOFUNTUS_DUNGEON.getClipMaxX());
		assertEquals(9875, UndergroundZone.ECTOFUNTUS_DUNGEON.getClipMinY());
		assertEquals(9901, UndergroundZone.ECTOFUNTUS_DUNGEON.getClipMaxY());

		assertTrue(UndergroundZone.MORYTANIA_SPIDER_CAVE.hasClipOverride());
		assertEquals(3652, UndergroundZone.MORYTANIA_SPIDER_CAVE.getClipMinX());
		assertEquals(3711, UndergroundZone.MORYTANIA_SPIDER_CAVE.getClipMaxX());
		assertEquals(9792, UndergroundZone.MORYTANIA_SPIDER_CAVE.getClipMinY());
		assertEquals(9867, UndergroundZone.MORYTANIA_SPIDER_CAVE.getClipMaxY());
	}

	@Test
	public void iceTrollCavesUsesClipOverride()
	{
		assertTrue(UndergroundZone.ICE_TROLL_CAVES.hasClipOverride());
		assertEquals(2368, UndergroundZone.ICE_TROLL_CAVES.getClipMinX());
		assertEquals(2432, UndergroundZone.ICE_TROLL_CAVES.getClipMaxX());
		assertEquals(10260, UndergroundZone.ICE_TROLL_CAVES.getClipMinY());
		assertEquals(10320, UndergroundZone.ICE_TROLL_CAVES.getClipMaxY());
	}

	@Test
	public void jatizsoMineUsesClipOverride()
	{
		assertTrue(UndergroundZone.JATIZSO_MINE.hasClipOverride());
		assertEquals(2370, UndergroundZone.JATIZSO_MINE.getClipMinX());
		assertEquals(2430, UndergroundZone.JATIZSO_MINE.getClipMaxX());
		assertEquals(10180, UndergroundZone.JATIZSO_MINE.getClipMinY());
		assertEquals(10235, UndergroundZone.JATIZSO_MINE.getClipMaxY());
	}

	@Test
	public void kharaziCavesUsesClipOverride()
	{
		assertTrue(UndergroundZone.KHARAZI_CAVES.hasClipOverride());
		assertEquals(2817, UndergroundZone.KHARAZI_CAVES.getClipMinX());
		assertEquals(2865, UndergroundZone.KHARAZI_CAVES.getClipMaxX());
		assertEquals(9217, UndergroundZone.KHARAZI_CAVES.getClipMinY());
		assertEquals(9296, UndergroundZone.KHARAZI_CAVES.getClipMaxY());
	}

	@Test
	public void shiloVillageGemMineUsesClipOverride()
	{
		assertTrue(UndergroundZone.SHILO_VILLAGE_GEM_MINE.hasClipOverride());
		assertEquals(2824, UndergroundZone.SHILO_VILLAGE_GEM_MINE.getClipMinX());
		assertEquals(2859, UndergroundZone.SHILO_VILLAGE_GEM_MINE.getClipMaxX());
		assertEquals(9376, UndergroundZone.SHILO_VILLAGE_GEM_MINE.getClipMinY());
		assertEquals(9400, UndergroundZone.SHILO_VILLAGE_GEM_MINE.getClipMaxY());
	}

	@Test
	public void rashiliyiasTombUsesClipOverride()
	{
		assertTrue(UndergroundZone.RASHILIYIAS_TOMB.hasClipOverride());
		assertEquals(2752, UndergroundZone.RASHILIYIAS_TOMB.getClipMinX());
		assertEquals(2815, UndergroundZone.RASHILIYIAS_TOMB.getClipMaxX());
		assertEquals(9152, UndergroundZone.RASHILIYIAS_TOMB.getClipMinY());
		assertEquals(9343, UndergroundZone.RASHILIYIAS_TOMB.getClipMaxY());
	}

	@Test
	public void skavidCavesUsesClipOverride()
	{
		assertTrue(UndergroundZone.SKAVID_CAVES.hasClipOverride());
		assertEquals(2560, UndergroundZone.SKAVID_CAVES.getClipMinX());
		assertEquals(2585, UndergroundZone.SKAVID_CAVES.getClipMaxX());
		assertEquals(9420, UndergroundZone.SKAVID_CAVES.getClipMinY());
		assertEquals(9470, UndergroundZone.SKAVID_CAVES.getClipMaxY());
	}

	@Test
	public void jungleEagleLairUsesClipOverride()
	{
		assertTrue(UndergroundZone.JUNGLE_EAGLE_LAIR.hasClipOverride());
		assertEquals(2508, UndergroundZone.JUNGLE_EAGLE_LAIR.getClipMinX());
		assertEquals(2527, UndergroundZone.JUNGLE_EAGLE_LAIR.getClipMaxX());
		assertEquals(9312, UndergroundZone.JUNGLE_EAGLE_LAIR.getClipMinY());
		assertEquals(9331, UndergroundZone.JUNGLE_EAGLE_LAIR.getClipMaxY());
	}

	@Test
	public void corsairCoveDungeonUsesClipOverride()
	{
		assertTrue(UndergroundZone.CORSAIR_COVE_DUNGEON.hasClipOverride());
		assertEquals(1950, UndergroundZone.CORSAIR_COVE_DUNGEON.getClipMinX());
		assertEquals(2071, UndergroundZone.CORSAIR_COVE_DUNGEON.getClipMaxX());
		assertEquals(8970, UndergroundZone.CORSAIR_COVE_DUNGEON.getClipMinY());
		assertEquals(9070, UndergroundZone.CORSAIR_COVE_DUNGEON.getClipMaxY());
	}

	@Test
	public void ogreEnclaveUsesClipOverride()
	{
		assertTrue(UndergroundZone.OGRE_ENCLAVE.hasClipOverride());
		assertEquals(2496, UndergroundZone.OGRE_ENCLAVE.getClipMinX());
		assertEquals(2559, UndergroundZone.OGRE_ENCLAVE.getClipMaxX());
		assertEquals(9408, UndergroundZone.OGRE_ENCLAVE.getClipMinY());
		assertEquals(9471, UndergroundZone.OGRE_ENCLAVE.getClipMaxY());
	}

	@Test
	public void jiggigDungeonUsesClipOverride()
	{
		assertTrue(UndergroundZone.JIGGIG_DUNGEON.hasClipOverride());
		assertEquals(2436, UndergroundZone.JIGGIG_DUNGEON.getClipMinX());
		assertEquals(2491, UndergroundZone.JIGGIG_DUNGEON.getClipMaxX());
		assertEquals(9412, UndergroundZone.JIGGIG_DUNGEON.getClipMinY());
		assertEquals(9460, UndergroundZone.JIGGIG_DUNGEON.getClipMaxY());
	}

	@Test
	public void grandTreeTunnelsUsesClipOverride()
	{
		assertTrue(UndergroundZone.GRAND_TREE_TUNNELS.hasClipOverride());
		assertEquals(2432, UndergroundZone.GRAND_TREE_TUNNELS.getClipMinX());
		assertEquals(2495, UndergroundZone.GRAND_TREE_TUNNELS.getClipMaxX());
		assertEquals(9856, UndergroundZone.GRAND_TREE_TUNNELS.getClipMinY());
		assertEquals(9919, UndergroundZone.GRAND_TREE_TUNNELS.getClipMaxY());
	}

	@Test
	public void strongholdSlayerDungeonUsesClipOverride()
	{
		assertTrue(UndergroundZone.STRONGHOLD_SLAYER_DUNGEON.hasClipOverride());
		assertEquals(2387, UndergroundZone.STRONGHOLD_SLAYER_DUNGEON.getClipMinX());
		assertEquals(2495, UndergroundZone.STRONGHOLD_SLAYER_DUNGEON.getClipMaxX());
		assertEquals(9767, UndergroundZone.STRONGHOLD_SLAYER_DUNGEON.getClipMinY());
		assertEquals(9838, UndergroundZone.STRONGHOLD_SLAYER_DUNGEON.getClipMaxY());
	}

	@Test
	public void eaglesPeakDungeonUsesClipOverride()
	{
		assertTrue(UndergroundZone.EAGLES_PEAK_DUNGEON.hasClipOverride());
		assertEquals(2048, UndergroundZone.EAGLES_PEAK_DUNGEON.getClipMinX());
		assertEquals(2107, UndergroundZone.EAGLES_PEAK_DUNGEON.getClipMaxX());
		assertEquals(4996, UndergroundZone.EAGLES_PEAK_DUNGEON.getClipMinY());
		assertEquals(5047, UndergroundZone.EAGLES_PEAK_DUNGEON.getClipMaxY());
	}

	@Test
	public void waterfallDungeonUsesClipOverride()
	{
		assertTrue(UndergroundZone.WATERFALL_DUNGEON.hasClipOverride());
		assertEquals(2560, UndergroundZone.WATERFALL_DUNGEON.getClipMinX());
		assertEquals(2611, UndergroundZone.WATERFALL_DUNGEON.getClipMaxX());
		assertEquals(9860, UndergroundZone.WATERFALL_DUNGEON.getClipMinY());
		assertEquals(9919, UndergroundZone.WATERFALL_DUNGEON.getClipMaxY());
	}

	@Test
	public void warriorsGuildBasementUsesClipOverride()
	{
		assertTrue(UndergroundZone.WARRIORS_GUILD_BASEMENT.hasClipOverride());
		assertTrue(UndergroundZone.WARRIORS_GUILD_BASEMENT.getClipMinX() >= 2904);
		assertTrue(UndergroundZone.WARRIORS_GUILD_BASEMENT.getClipMaxX() <= 2943);
		assertTrue(UndergroundZone.WARRIORS_GUILD_BASEMENT.getClipMinY() >= 9954);
		assertTrue(UndergroundZone.WARRIORS_GUILD_BASEMENT.getClipMaxY() <= 9976);
	}

	@Test
	public void iceQueensLairUsesClipOverride()
	{
		assertTrue(UndergroundZone.ICE_QUEEN_LAIR.hasClipOverride());
		assertEquals(2816, UndergroundZone.ICE_QUEEN_LAIR.getClipMinX());
		assertEquals(2896, UndergroundZone.ICE_QUEEN_LAIR.getClipMaxX());
	}

	@Test
	public void whiteKnightsCryptUsesClipOverride()
	{
		assertTrue(UndergroundZone.WHITE_KNIGHTS_CASTLE_CRYPT.hasClipOverride());
		assertTrue(UndergroundZone.WHITE_KNIGHTS_CASTLE_CRYPT.getClipMaxX() < 2979);
		assertTrue(UndergroundZone.WHITE_KNIGHTS_CASTLE_CRYPT.getClipMaxY() < 9756);
	}

	@Test
	public void whiteWolfTunnelUsesClipOverride()
	{
		assertTrue(UndergroundZone.WHITE_WOLF_TUNNEL.hasClipOverride());
		assertTrue(UndergroundZone.WHITE_WOLF_TUNNEL.getClipMaxX() < 2884);
		assertTrue(UndergroundZone.WHITE_WOLF_TUNNEL.getClipMinY() > 9854);
		assertTrue(UndergroundZone.WHITE_WOLF_TUNNEL.getClipMaxY() < 9942);
	}

	@Test
	public void ancientCavernUsesClipOverride()
	{
		assertTrue(UndergroundZone.ANCIENT_CAVERN.hasClipOverride());
		assertTrue(UndergroundZone.ANCIENT_CAVERN.getClipMaxY() < 5441);
		assertTrue(UndergroundZone.ANCIENT_CAVERN.getClipMinY() > 5244);
	}

	@Test
	public void ardougneSewersUsesClipOverride()
	{
		assertTrue(UndergroundZone.ARDOUGNE_UNDERGROUND.hasClipOverride());
		assertTrue(UndergroundZone.ARDOUGNE_UNDERGROUND.getClipMinY() > 9663);
		assertTrue(UndergroundZone.ARDOUGNE_UNDERGROUND.getClipMaxX() < 2692);
		assertTrue(UndergroundZone.ARDOUGNE_UNDERGROUND.getClipMaxY() < 9740);
	}

	@Test
	public void brimstailCaveUsesClipOverride()
	{
		assertTrue(UndergroundZone.BRIMSTAIL_CAVE.hasClipOverride());
		assertTrue(UndergroundZone.BRIMSTAIL_CAVE.getClipMaxX() < 2415);
		assertTrue(UndergroundZone.BRIMSTAIL_CAVE.getClipMinY() >= 9807);
	}

	@Test
	public void chaosDruidTowerUsesClipOverride()
	{
		assertTrue(UndergroundZone.CHAOS_DRUID_TOWER.hasClipOverride());
		assertTrue(UndergroundZone.CHAOS_DRUID_TOWER.getClipMinY() > 9727);
		assertTrue(UndergroundZone.CHAOS_DRUID_TOWER.getClipMaxY() < 9767);
	}

	@Test
	public void clockTowerDungeonUsesClipOverride()
	{
		assertTrue(UndergroundZone.CLOCK_TOWER_DUNGEON.hasClipOverride());
		assertTrue(UndergroundZone.CLOCK_TOWER_DUNGEON.getClipMaxY() <= 9663);
		assertTrue(UndergroundZone.CLOCK_TOWER_DUNGEON.getClipMinX() >= 2560);
	}

	@Test
	public void elementalWorkshopUsesClipOverrideNotGiantWikiBox()
	{
		assertTrue(UndergroundZone.ELEMENTAL_WORKSHOP.hasClipOverride());
		assertTrue(UndergroundZone.ELEMENTAL_WORKSHOP.getClipMaxX() - UndergroundZone.ELEMENTAL_WORKSHOP.getClipMinX() < 80);
		assertTrue(UndergroundZone.ELEMENTAL_WORKSHOP.getClipMinX() >= 2689);
		assertTrue(UndergroundZone.ELEMENTAL_WORKSHOP.getClipMinY() >= 9863);
	}

	@Test
	public void glarialsTombUsesClipOverrideNotGiantWikiBox()
	{
		assertTrue(UndergroundZone.GLARIALS_TOMB.hasClipOverride());
		assertTrue(UndergroundZone.GLARIALS_TOMB.getClipMaxX() - UndergroundZone.GLARIALS_TOMB.getClipMinX() < 50);
		assertTrue(UndergroundZone.GLARIALS_TOMB.getClipMaxY() <= 9848);
		assertTrue(UndergroundZone.GLARIALS_TOMB.getClipMinY() >= 9808);
	}

	@Test
	public void templeOfIkovUsesClipOverrideNotGiantWikiBox()
	{
		assertTrue(UndergroundZone.TEMPLE_OF_IKOV.hasClipOverride());
		assertEquals(UndergroundZone.TEMPLE_OF_IKOV,
			UndergroundZone.forSurfacePoint(2659, 3492, 0));
		assertTrue(UndergroundZone.TEMPLE_OF_IKOV.getClipMinX() >= 2626);
		assertTrue(UndergroundZone.TEMPLE_OF_IKOV.getClipMaxX() <= 2748);
		assertTrue(UndergroundZone.TEMPLE_OF_IKOV.getClipMinY() >= 9784);
		assertTrue(UndergroundZone.TEMPLE_OF_IKOV.getClipMaxY() <= 9862);
		assertFalse(InstanceMaps.belongsToZone(2622, 9793, UndergroundZone.TEMPLE_OF_IKOV));
		assertFalse(InstanceMaps.belongsToZone(2716, 9888, UndergroundZone.TEMPLE_OF_IKOV));
		assertTrue(InstanceMaps.belongsToZone(2676, 9804, UndergroundZone.TEMPLE_OF_IKOV));
	}

	@Test
	public void krakenCoveUsesClipOverrideNotStrongholdWikiBox()
	{
		assertTrue(UndergroundZone.KRAKEN_COVE.hasClipOverride());
		assertTrue(UndergroundZone.KRAKEN_COVE.getClipMinX() >= 2241);
		assertTrue(UndergroundZone.KRAKEN_COVE.getClipMaxX() <= 2301);
		assertTrue(UndergroundZone.KRAKEN_COVE.getClipMinY() >= 9985);
		assertTrue(UndergroundZone.KRAKEN_COVE.getClipMaxY() <= 10043);
		assertTrue(InstanceMaps.belongsToZone(2278, 10011, UndergroundZone.KRAKEN_COVE));
		assertFalse(InstanceMaps.belongsToZone(2464, 9897, UndergroundZone.KRAKEN_COVE));
	}

	@Test
	public void ouraniaCaveUsesClipOverrideNotGiantWikiBox()
	{
		assertTrue(UndergroundZone.OURANIA_ALTAR.hasClipOverride());
		assertTrue(UndergroundZone.OURANIA_ALTAR.getClipMinX() >= 3008);
		assertTrue(UndergroundZone.OURANIA_ALTAR.getClipMaxX() <= 3070);
		assertTrue(UndergroundZone.OURANIA_ALTAR.getClipMinY() >= 5568);
		assertTrue(UndergroundZone.OURANIA_ALTAR.getClipMaxY() <= 5630);
		assertTrue(InstanceMaps.belongsToZone(3022, 5582, UndergroundZone.OURANIA_ALTAR));
		assertFalse(InstanceMaps.belongsToZone(2451, 3231, UndergroundZone.OURANIA_ALTAR));
	}

	@Test
	public void smokeDevilUsesClipOverrideNotObservatoryWikiBox()
	{
		assertTrue(UndergroundZone.SMOKE_DEVIL_DUNGEON.hasClipOverride());
		assertTrue(UndergroundZone.SMOKE_DEVIL_DUNGEON.getClipMinX() >= 2376);
		assertTrue(UndergroundZone.SMOKE_DEVIL_DUNGEON.getClipMaxX() <= 2427);
		assertTrue(UndergroundZone.SMOKE_DEVIL_DUNGEON.getClipMinY() >= 9414);
		assertTrue(UndergroundZone.SMOKE_DEVIL_DUNGEON.getClipMaxY() <= 9468);
		assertTrue(InstanceMaps.belongsToZone(2412, 9461, UndergroundZone.SMOKE_DEVIL_DUNGEON));
		assertFalse(InstanceMaps.belongsToZone(2335, 9350, UndergroundZone.SMOKE_DEVIL_DUNGEON));
	}

	@Test
	public void towerOfLifeUsesClipOverrideNotGiantWikiBox()
	{
		assertTrue(UndergroundZone.TOWER_OF_LIFE_BASEMENT.hasClipOverride());
		assertTrue(UndergroundZone.TOWER_OF_LIFE_BASEMENT.getClipMinX() >= 3009);
		assertTrue(UndergroundZone.TOWER_OF_LIFE_BASEMENT.getClipMaxX() <= 3068);
		assertTrue(UndergroundZone.TOWER_OF_LIFE_BASEMENT.getClipMinY() >= 4353);
		assertTrue(UndergroundZone.TOWER_OF_LIFE_BASEMENT.getClipMaxY() <= 4413);
		assertTrue(InstanceMaps.belongsToZone(3059, 4397, UndergroundZone.TOWER_OF_LIFE_BASEMENT));
		assertFalse(InstanceMaps.belongsToZone(2967, 4382, UndergroundZone.TOWER_OF_LIFE_BASEMENT));
		assertTrue(InstanceMaps.belongsToZone(2967, 4382, UndergroundZone.CORPOREAL_BEAST));
	}

	@Test
	public void treeGnomeVillageUsesClipOverrideNotYanilleWikiBox()
	{
		assertTrue(UndergroundZone.TREE_GNOME_VILLAGE_DUNGEON.hasClipOverride());
		assertTrue(UndergroundZone.TREE_GNOME_VILLAGE_DUNGEON.getClipMinX() >= 2507);
		assertTrue(UndergroundZone.TREE_GNOME_VILLAGE_DUNGEON.getClipMaxX() <= 2553);
		assertTrue(UndergroundZone.TREE_GNOME_VILLAGE_DUNGEON.getClipMinY() >= 9551);
		assertTrue(UndergroundZone.TREE_GNOME_VILLAGE_DUNGEON.getClipMaxY() <= 9584);
		assertTrue(InstanceMaps.belongsToZone(2533, 9556, UndergroundZone.TREE_GNOME_VILLAGE_DUNGEON));
		assertFalse(InstanceMaps.belongsToZone(2569, 9525, UndergroundZone.TREE_GNOME_VILLAGE_DUNGEON));
		assertTrue(InstanceMaps.belongsToZone(2569, 9525, UndergroundZone.YANILLE_AGILITY_DUNGEON));
	}

	@Test
	public void witchavenUsesClipOverrideNotArdougneWikiBox()
	{
		assertTrue(UndergroundZone.WITCHAVEN_DUNGEON.hasClipOverride());
		assertTrue(UndergroundZone.WITCHAVEN_DUNGEON.getClipMinX() >= 2692);
		assertTrue(UndergroundZone.WITCHAVEN_DUNGEON.getClipMaxX() <= 2748);
		assertTrue(UndergroundZone.WITCHAVEN_DUNGEON.getClipMinY() >= 9665);
		assertTrue(UndergroundZone.WITCHAVEN_DUNGEON.getClipMaxY() <= 9719);
		assertTrue(InstanceMaps.belongsToZone(2696, 9683, UndergroundZone.WITCHAVEN_DUNGEON));
		assertFalse(InstanceMaps.belongsToZone(2632, 9694, UndergroundZone.WITCHAVEN_DUNGEON));
		assertTrue(InstanceMaps.belongsToZone(2632, 9694, UndergroundZone.ARDOUGNE_UNDERGROUND));
	}

	@Test
	public void undergroundPassUsesClipOverrideNotGiantWikiBox()
	{
		assertTrue(UndergroundZone.UNDERGROUND_PASS.hasClipOverride());
		assertTrue(UndergroundZone.UNDERGROUND_PASS.getClipMinX() >= 2312);
		assertTrue(UndergroundZone.UNDERGROUND_PASS.getClipMaxX() <= 2497);
		assertTrue(UndergroundZone.UNDERGROUND_PASS.getClipMinY() >= 9601);
		assertTrue(UndergroundZone.UNDERGROUND_PASS.getClipMaxY() <= 9728);
		assertTrue(InstanceMaps.belongsToZone(2433, 9715, UndergroundZone.UNDERGROUND_PASS));
		assertTrue(InstanceMaps.belongsToZone(2318, 9621, UndergroundZone.UNDERGROUND_PASS));
		assertFalse(InstanceMaps.belongsToZone(2696, 9683, UndergroundZone.UNDERGROUND_PASS));
		assertFalse(InstanceMaps.belongsToZone(2533, 9556, UndergroundZone.UNDERGROUND_PASS));
	}

	@Test
	public void prifddinasCityIsNotADungeonLayer()
	{
		final UndergroundZone nearPrif = UndergroundZone.forSurfacePoint(2240, 3328, 40);
		assertNotNull(nearPrif);
		assertEquals("The city itself stays on the overworld; only the Grand Library is a dungeon layer",
			"prifddinas_grand_library", nearPrif.getId());
	}

	@Test
	public void testUndergroundSpatialLookup()
	{
		// Test Lumbridge cellar underground point lookup
		UndergroundZone lumbridge = UndergroundZone.forUndergroundPoint(3210, 9616, 50);
		assertNotNull("Should find Lumbridge Cellar at dungeon coordinates", lumbridge);
		assertEquals("lumbridge_cellar", lumbridge.getId());

		// Test Catacombs of Kourend underground point lookup
		UndergroundZone catacombs = UndergroundZone.forUndergroundPoint(1664, 10048, 50);
		assertNotNull("Should find Catacombs of Kourend at dungeon coordinates", catacombs);
		assertEquals("catacombs_of_kourend", catacombs.getId());
	}

	@Test
	public void testCameraUndergroundModeTransitions()
	{
		MapCamera camera = new MapCamera();
		assertFalse(camera.isUndergroundModeActive());
		assertNull(camera.getActiveUndergroundZone());

		UndergroundZone taverley = UndergroundZone.TAVERLEY_DUNGEON;
		final int surfaceX = taverley.getSurfacePoint().getX() + 20;
		final int surfaceY = taverley.getSurfacePoint().getY() + 30;
		camera.centerOn(surfaceX, surfaceY);

		camera.setUndergroundMode(taverley);

		assertTrue(camera.isUndergroundModeActive());
		assertEquals(taverley, camera.getActiveUndergroundZone());
		// Opening a dungeon frames the surface entrance and composites the dungeon forward — the
		// camera stays on overworld coordinates rather than leaping to raw +6400 dungeon coords.
		assertEquals(taverley.getSurfacePoint().getX(), (int) camera.getCenterX());
		assertEquals(taverley.getSurfacePoint().getY(), (int) camera.getCenterY());
		assertEquals(taverley.getUndergroundPoint().getPlane(), camera.getPlane());
		assertEquals(taverley, camera.getFocusedUndergroundZone());
		assertTrue(camera.isDungeonContentsFocused());

		camera.setHoveredUnderground(taverley, true);
		assertEquals(taverley, camera.getHoveredUndergroundZone());
		assertTrue(camera.isHoveredSurfaceToUnderground());

		camera.setHoveredUnderground(taverley, false);
		assertEquals(taverley, camera.getHoveredUndergroundZone());
		assertFalse(camera.isHoveredSurfaceToUnderground());

		camera.clearUndergroundMode();
		assertFalse(camera.isUndergroundModeActive());
		assertNull(camera.getActiveUndergroundZone());
		assertEquals(taverley.getSurfacePoint().getX(), (int) camera.getCenterX());
		assertEquals(taverley.getSurfacePoint().getY(), (int) camera.getCenterY());
		assertEquals(taverley.getSurfacePoint().getPlane(), camera.getPlane());
	}

	@Test
	public void enteringADungeonFramesTheSurfaceEntranceAndClearingLeavesItThere()
	{
		MapCamera camera = new MapCamera();
		final UndergroundZone zone = UndergroundZone.TAVERLEY_DUNGEON;
		camera.centerOn(zone.getSurfacePoint().getX() - 12, zone.getSurfacePoint().getY() + 8);

		camera.setUndergroundMode(zone);

		assertEquals(zone.getSurfacePoint().getX(), camera.getCenterX(), 0.001);
		assertEquals(zone.getSurfacePoint().getY(), camera.getCenterY(), 0.001);

		// Clearing must not translate the centre by the zone delta — it never left the surface.
		camera.clearUndergroundMode();
		assertEquals(zone.getSurfacePoint().getX(), camera.getCenterX(), 0.001);
		assertEquals(zone.getSurfacePoint().getY(), camera.getCenterY(), 0.001);
	}

	@Test
	public void openingIkovShortcutFramesTheShortcut()
	{
		MapCamera camera = new MapCamera();
		final net.runelite.api.coords.WorldPoint shortcut = new net.runelite.api.coords.WorldPoint(2659, 3492, 0);
		camera.setUndergroundMode(UndergroundZone.TEMPLE_OF_IKOV, shortcut);
		assertEquals(2659, camera.getCenterX(), 0.001);
		assertEquals(3492, camera.getCenterY(), 0.001);
	}

	@Test
	public void everyZoneAnchorsAboveTheOverworldAndDeltasAreSelfConsistent()
	{
		for (UndergroundZone zone : UndergroundZone.ALL_ZONES)
		{
			if (zone.getId().startsWith("native_") || zone.isSubterranean() || zone.getYOffset() != 6400 || zone == UndergroundZone.VTAM_CORPORATION || zone.ordinal() > UndergroundZone.WYRMSCRAIG_CAVERN.ordinal()) continue;
			// Every interior must sit above the overworld cutoff, or InstanceMaps.hiddenOnSurface
			// cannot keep its pin off the open sea in the overworld view.
			assertTrue(zone.getId() + " interior must sit above the overworld cutoff",
				zone.getUndergroundPoint().getY() > com.bettermap.map.InstanceMaps.GAP_MIN_Y);
			assertTrue(zone.getId() + " surface entrance must be on the overworld",
				zone.getSurfacePoint().getY() <= com.bettermap.map.InstanceMaps.GAP_MIN_Y);
			assertTrue(zone.getId() + " radius must be usable for framing", zone.getRadius() > 0);

			// A +6400-band zone's vertical drop must land within a tile band of one map layer, or a
			// coordinate was transcribed wrong.
			if (zone.getYOffset() == 6400)
			{
				assertTrue(zone.getId() + " looks mis-transcribed: deltaY=" + zone.getDeltaY(),
					zone.getDeltaY() > 6200 && zone.getDeltaY() < 6600);
			}
		}
	}

	@Test
	public void isleOfSoulsUsesItsSingleEastSideEntrance()
	{
		final UndergroundZone zone = UndergroundZone.ISLE_OF_SOULS_DUNGEON;
		assertEquals(2309, zone.getSurfacePoint().getX());
		assertEquals(2919, zone.getSurfacePoint().getY());
		assertEquals(-174, zone.getDeltaX());
		assertEquals(6401, zone.getDeltaY());
		assertEquals(zone, UndergroundZone.forSurfacePoint(2309, 2919, 0));
		assertFalse("the old west-side anchor must not resolve as this dungeon",
			zone == UndergroundZone.forSurfacePoint(2135, 2920, 0));
	}

	@Test
	public void everyAuthoredSurfaceEntranceResolvesToItsZone()
	{
		for (UndergroundZone zone : UndergroundZone.ALL_ZONES)
		{
			assertEquals(zone.getSurfacePoint(), zone.getSurfacePoints().get(0));
			final java.util.Set<net.runelite.api.coords.WorldPoint> unique = new java.util.HashSet<>();
			for (net.runelite.api.coords.WorldPoint entrance : zone.getSurfacePoints())
			{
				assertTrue("Duplicate hover entrance for " + zone.getName() + " at " + entrance,
					unique.add(entrance));
			}
		}
	}

	@Test
	public void correctedDungeonEntrancesDoNotCreateDuplicateLayerSymbols()
	{
		assertNull(UndergroundZone.byId("wilderness_dungeons"));
		assertEquals(1, UndergroundZone.KALPHITE_CAVE.getSurfacePoints().size());
		assertEquals(3319, UndergroundZone.KALPHITE_CAVE.getSurfacePoint().getX());
		assertEquals(3122, UndergroundZone.KALPHITE_CAVE.getSurfacePoint().getY());
		assertEquals(1, UndergroundZone.WEB_CHASM.getSurfacePoints().size());
		assertEquals(1, UndergroundZone.MORYTANIA_SPIDER_CAVE.getSurfacePoints().size());
		assertEquals(3657, UndergroundZone.MORYTANIA_SPIDER_CAVE.getSurfacePoint().getX());
		assertEquals(3407, UndergroundZone.MORYTANIA_SPIDER_CAVE.getSurfacePoint().getY());
	}

	@Test
	public void trollStrongholdKeepsItsThreeRealEntrancesOnOneZone()
	{
		assertEquals(3, UndergroundZone.TROLL_STRONGHOLD.getSurfacePoints().size());
		assertEquals(UndergroundZone.TROLL_STRONGHOLD,
			UndergroundZone.forSurfacePoint(2839, 3690, 0));
		assertEquals(UndergroundZone.TROLL_STRONGHOLD,
			UndergroundZone.forSurfacePoint(2831, 3677, 0));
		assertEquals(UndergroundZone.TROLL_STRONGHOLD,
			UndergroundZone.forSurfacePoint(2827, 3647, 0));
	}

	@Test
	public void reportedMissingHoversAreActualLayerSymbolTargets()
	{
		final Object[][] entrances = {
			{UndergroundZone.LUNAR_ISLE_MINE, 2142, 3944},
			{UndergroundZone.SCORPIA_CAVE, 3244, 3949},
			{UndergroundZone.SCORPIA_CAVE, 3231, 3952},
			{UndergroundZone.SCORPIA_CAVE, 3232, 3936},
			{UndergroundZone.SILK_CHASM, 3320, 3798},
			{UndergroundZone.ESCAPE_CAVES, 3260, 3833},
			{UndergroundZone.ESCAPE_CAVES, 3320, 3831},
			{UndergroundZone.ESCAPE_CAVES, 3284, 3808},
			{UndergroundZone.ESCAPE_CAVES, 3282, 3775},
			{UndergroundZone.CALLISTOS_DEN, 3292, 3850},
			{UndergroundZone.VETIONS_REST, 3221, 3788},
			{UndergroundZone.TROLL_STRONGHOLD, 2839, 3690},
			{UndergroundZone.ZEMOUREGALS_BASE, 3343, 3515},
			{UndergroundZone.SHADE_CATACOMBS, 3485, 3321},
			{UndergroundZone.GIANTS_FOUNDRY, 3360, 3150}
		};
		for (Object[] entrance : entrances)
		{
			assertEquals(entrance[0], UndergroundZone.forSurfacePoint(
				(int) entrance[1], (int) entrance[2], 0));
		}
	}

	@Test
	public void exactNameCuratedSurfaceEntrancesHaveHoverTargets()
	{
		for (PoiIndex.Poi poi : PoiDetails.getAllPois())
		{
			if (!"dungeon".equals(poi.getKey()) || poi.getY() > InstanceMaps.GAP_MIN_Y)
			{
				continue;
			}
			final List<UndergroundZone> matching = new ArrayList<>();
			for (UndergroundZone zone : UndergroundZone.ALL_ZONES)
			{
				if (zone.getId().startsWith("native_") || zone.isSubterranean() || zone.getYOffset() != 6400 || zone == UndergroundZone.VTAM_CORPORATION || zone.ordinal() > UndergroundZone.WYRMSCRAIG_CAVERN.ordinal()) continue;
				if (zone.getName().equalsIgnoreCase(poi.getName()))
				{
					matching.add(zone);
				}
			}
			if (matching.isEmpty())
			{
				continue;
			}
			boolean resolved = false;
			for (UndergroundZone zone : matching)
			{
				for (net.runelite.api.coords.WorldPoint entrance : zone.getSurfacePoints())
				{
					final int dx = entrance.getX() - poi.getX();
					final int dy = entrance.getY() - poi.getY();
					if (dx * dx + dy * dy <= 64)
					{
						resolved = true;
					}
				}
			}
			assertTrue("Missing hover target for " + poi.getName() + " at ("
				+ poi.getX() + "," + poi.getY() + ")", resolved);
		}
	}

	@Test
	public void testLayerSymbolTargets()
	{
		MapCamera camera = new MapCamera();
		List<MapCamera.LayerSymbolTarget> targets = new ArrayList<>();

		Rectangle rect1 = new Rectangle(100, 100, 32, 32);
		Rectangle rect2 = new Rectangle(200, 200, 32, 32);

		targets.add(new MapCamera.LayerSymbolTarget(rect1, UndergroundZone.VARROCK_SEWERS, true));
		targets.add(new MapCamera.LayerSymbolTarget(rect2, UndergroundZone.VARROCK_SEWERS, false));

		camera.setLayerSymbolTargets(targets);
		assertEquals(2, camera.getLayerSymbolTargets().size());
		assertTrue(camera.getLayerSymbolTargets().get(0).isSurfaceToUnderground());
		assertFalse(camera.getLayerSymbolTargets().get(1).isSurfaceToUnderground());
	}

	@Test
	public void testUndergroundZone2DDeltas()
	{
		// Test GWD 2D delta
		UndergroundZone gwd = UndergroundZone.GOD_WARS_DUNGEON;
		assertEquals(gwd.getUndergroundPoint().getX() - gwd.getSurfacePoint().getX(), gwd.getDeltaX());
		assertEquals(gwd.getUndergroundPoint().getY() - gwd.getSurfacePoint().getY(), gwd.getDeltaY());
		assertEquals(-36, gwd.getDeltaX());
		assertEquals(1563, gwd.getDeltaY());

		// Test TzHaar City 2D delta
		UndergroundZone tzhaar = UndergroundZone.TZHAAR_CITY;
		assertEquals(tzhaar.getUndergroundPoint().getX() - tzhaar.getSurfacePoint().getX(), tzhaar.getDeltaX());
		assertEquals(tzhaar.getUndergroundPoint().getY() - tzhaar.getSurfacePoint().getY(), tzhaar.getDeltaY());
		assertEquals(-364, tzhaar.getDeltaX());
		assertEquals(1970, tzhaar.getDeltaY());

		// Test Stronghold of Security 2D delta
		UndergroundZone stronghold = UndergroundZone.STRONGHOLD_OF_SECURITY;
		assertEquals(stronghold.getUndergroundPoint().getX() - stronghold.getSurfacePoint().getX(), stronghold.getDeltaX());
		assertEquals(stronghold.getUndergroundPoint().getY() - stronghold.getSurfacePoint().getY(), stronghold.getDeltaY());
		assertEquals(-1221, stronghold.getDeltaX());
		assertEquals(1810, stronghold.getDeltaY());

		// Test Catacombs of Kourend 2D delta
		UndergroundZone catacombs = UndergroundZone.CATACOMBS_OF_KOUREND;
		assertEquals(catacombs.getUndergroundPoint().getX() - catacombs.getSurfacePoint().getX(), catacombs.getDeltaX());
		assertEquals(catacombs.getUndergroundPoint().getY() - catacombs.getSurfacePoint().getY(), catacombs.getDeltaY());
		assertEquals(28, catacombs.getDeltaX());
		assertEquals(6375, catacombs.getDeltaY());

		// Verify every zone satisfies getOffsetX == getDeltaX and getOffsetY == getDeltaY
		for (UndergroundZone zone : UndergroundZone.ALL_ZONES)
		{
			assertEquals(zone.getDeltaX(), zone.getOffsetX());
			assertEquals(zone.getDeltaY(), zone.getOffsetY());
		}
	}

	@Test
	public void everyZoneHasDungeonPoiOnUndergroundLayer()
	{
		final List<PoiIndex.Poi> pois = PoiDetails.getAllPois();
		final int maxDist = 50;
		for (UndergroundZone zone : UndergroundZone.ALL_ZONES)
		{
			if (zone.getId().startsWith("native_") || zone.isSubterranean() || zone.getYOffset() != 6400 || zone == UndergroundZone.VTAM_CORPORATION || zone.ordinal() > UndergroundZone.WYRMSCRAIG_CAVERN.ordinal()) continue;
			final int ux = zone.getUndergroundPoint().getX();
			final int uy = zone.getUndergroundPoint().getY();
			boolean found = false;
			for (PoiIndex.Poi poi : pois)
			{
				if (!"dungeon".equals(poi.getKey()) && !"basement".equals(poi.getKey()))
				{
					continue;
				}
				final int dx = poi.getX() - ux;
				final int dy = poi.getY() - uy;
				if (dx * dx + dy * dy <= maxDist * maxDist)
				{
					found = true;
					break;
				}
			}
			assertTrue(
				"Missing underground-layer dungeon POI near " + zone.getName()
					+ " at (" + ux + "," + uy + ")",
				found);
		}
	}

	@Test
	public void canonicalZoneAliasingAndMatching()
	{
		assertEquals("varrock_sewers", UndergroundZone.canonicalZoneId("edgeville_dungeon"));
		assertEquals("varrock_sewers", UndergroundZone.canonicalZoneId("varrock_sewers"));
		assertEquals("karamja_dungeon", UndergroundZone.canonicalZoneId("crandor_dungeon"));
		assertEquals("taverley_dungeon", UndergroundZone.canonicalZoneId("taverley_dungeon"));
		assertNull(UndergroundZone.canonicalZoneId(null));

		assertTrue(UndergroundZone.zonesMatch("edgeville_dungeon", "varrock_sewers"));
		assertTrue(UndergroundZone.zonesMatch("varrock_sewers", "edgeville_dungeon"));
		assertTrue(UndergroundZone.zonesMatch("edgeville_dungeon", "edgeville_dungeon"));
		assertTrue(UndergroundZone.zonesMatch("crandor_dungeon", "karamja_dungeon"));
		assertFalse(UndergroundZone.zonesMatch("edgeville_dungeon", "taverley_dungeon"));
		assertFalse(UndergroundZone.zonesMatch(null, "varrock_sewers"));
		assertFalse(UndergroundZone.zonesMatch("edgeville_dungeon", null));

		assertEquals("varrock_sewers", UndergroundZone.EDGEVILLE_DUNGEON.getCanonicalId());
		assertEquals("varrock_sewers", UndergroundZone.VARROCK_SEWERS.getCanonicalId());
	}
}
