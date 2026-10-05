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
import com.bettermap.tiles.WikiMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class InstanceMapsTest
{
	@org.junit.BeforeClass
	public static void loadMapData()
	{
		com.bettermap.map.MapData.load();
		org.junit.Assert.assertTrue("Map catalogs must load before testing", com.bettermap.map.MapData.isReady());
	}

	@Test
	public void theOverworldTopIsTheOnlyBoundaryThatMatters()
	{
		assertEquals(WikiMap.SURFACE.getMaxY(), InstanceMaps.GAP_MIN_Y);
		assertTrue(InstanceMaps.cameraOnOverworld(3218));
		assertFalse(InstanceMaps.cameraOnOverworld(5310));
		assertFalse(InstanceMaps.cameraOnOverworld(10155));
	}

	@Test
	public void visibleSwitcherStopsAtDungeonLayer()
	{
		assertEquals(1, InstanceMaps.HIGHEST_VISIBLE_PLANE);
	}

	@Test
	public void zilyanaProjectsOntoTrollheimOnTheSurface()
	{
		final double camX = 3222;
		final double camY = 3218;
		assertEquals(2943, InstanceMaps.toDisplayX(2907, 5265, camX, camY));
		assertEquals(3702, InstanceMaps.toDisplayY(2907, 5265, camX, camY));
		assertFalse(InstanceMaps.hiddenOnSurface(2907, 5265, camX, camY));
	}

	@Test
	public void zilyanaStaysNativeWhenTheCameraIsInGodWars()
	{
		assertEquals(2907, InstanceMaps.toDisplayX(2907, 5265, 2881, 5310));
		assertEquals(5265, InstanceMaps.toDisplayY(2907, 5265, 2881, 5310));
	}

	@Test
	public void abyssalSireProjectsOntoTheWildernessMage()
	{
		assertEquals(3104, InstanceMaps.toDisplayX(3039, 4788, 3222, 3218));
		assertEquals(3514, InstanceMaps.toDisplayY(3039, 4788, 3222, 3218));
	}

	@Test
	public void dagannothKingsProjectOntoWaterbirth()
	{
		assertEquals(2545, InstanceMaps.toDisplayX(2631, 9667, 3222, 3218));
		assertEquals(3755, InstanceMaps.toDisplayY(2631, 9667, 3222, 3218));
	}

	@Test
	public void dagannothKingsStayInTheDungeonWhenTheCameraIsThere()
	{
		assertEquals(2631, InstanceMaps.toDisplayX(2631, 9667, 2545, 10155));
		assertEquals(9667, InstanceMaps.toDisplayY(2631, 9667, 2545, 10155));
	}

	@Test
	public void zanarisFairyRingProjectsOntoTheSwampShed()
	{
		assertEquals(3201, InstanceMaps.toDisplayX(2412, 4434, 3222, 3218));
		assertEquals(3169, InstanceMaps.toDisplayY(2412, 4434, 3222, 3218));
	}

	@Test
	public void prifddinasIsAnOverworldOverlayNotAnInteriorLayer()
	{
		assertTrue(InstanceMaps.isOverworldOverlay(3264, 6080));
		assertFalse(InstanceMaps.isOverworldOverlay(2884, 9798));
	}

	@Test
	public void lmsWildVarrockIsHiddenOnTheSurface()
	{
		assertTrue(InstanceMaps.hiddenOnSurface(3552, 6112, 3222, 3218));
		assertFalse(InstanceMaps.hiddenOnSurface(3552, 6112, 3552, 6112));
	}

	@Test
	public void braindeathFerryLandsNorthEastOfMosLeHarmless()
	{
		assertEquals(2140 - InstanceMaps.BRAINDEATH_OFFSET_X, InstanceMaps.toDisplayX(2140, 5093, 3222, 3218));
		assertEquals(5093 - InstanceMaps.BRAINDEATH_OFFSET_Y, InstanceMaps.toDisplayY(2140, 5093, 3222, 3218));
		assertTrue(InstanceMaps.toDisplayY(2140, 5093, 3222, 3218) < InstanceMaps.GAP_MIN_Y);
	}

	@Test
	public void surfaceMonsterQueryDoesNotPullTaverleyDungeon()
	{
		final boolean[] dungeonBox = { false };
		InstanceMaps.forEachQueryArea(2800, 2950, 3300, 3500, 2884, 3398, false,
			(minX, maxX, minY, maxY) ->
			{
				if (maxY > 9000)
				{
					dungeonBox[0] = true;
				}
			});
		assertFalse("Taverley Dungeon spawns must not be queried while looking at the town", dungeonBox[0]);
	}

	@Test
	public void projectingDungeonsStillQueriesTaverleyDungeonFromTheTown()
	{
		final boolean[] dungeonBox = { false };
		InstanceMaps.forEachQueryArea(2800, 2950, 3300, 3500, 2884, 3398, true,
			(minX, maxX, minY, maxY) ->
			{
				if (maxY > 9000)
				{
					dungeonBox[0] = true;
				}
			});
		assertTrue("POI / travel pins still project the dungeon onto the entrance", dungeonBox[0]);
	}

	@Test
	public void skippingDungeonsStillQueriesPrifddinasOnTheOverworldSlot()
	{
		final boolean[] prifBox = { false };
		InstanceMaps.forEachQueryArea(2100, 2400, 3200, 3500, 2240, 3328, false,
			(minX, maxX, minY, maxY) ->
			{
				if (minX <= 3264 && maxX >= 3264 && minY <= 6080 && maxY >= 6080)
				{
					prifBox[0] = true;
				}
			});
		assertTrue("Prifddinas city markers still composite onto Tirannwn", prifBox[0]);
	}

	@Test
	public void taverleyDungeonPointBelongsToTaverleyNotTheTown()
	{
		assertTrue(InstanceMaps.belongsToZone(2884, 9798, UndergroundZone.TAVERLEY_DUNGEON));
		assertFalse(InstanceMaps.belongsToZone(2884, 3398, UndergroundZone.TAVERLEY_DUNGEON));
		assertFalse(InstanceMaps.belongsToZone(3096, 9869, UndergroundZone.TAVERLEY_DUNGEON));
		assertTrue(InstanceMaps.belongsToZone(3096, 9869, UndergroundZone.EDGEVILLE_DUNGEON));
	}

	@Test
	public void focusedDungeonLayerDropsOverworldAndNeighbouringCaves()
	{
		final UndergroundZone taverley = UndergroundZone.TAVERLEY_DUNGEON;
		assertTrue(InstanceMaps.inFocusedLayer(2884, 9798, taverley, true));
		assertFalse(InstanceMaps.inFocusedLayer(3222, 3218, taverley, true));
		assertFalse(InstanceMaps.inFocusedLayer(3096, 9869, taverley, true));
		assertTrue(InstanceMaps.inFocusedLayer(3222, 3218, taverley, false));
		assertFalse(InstanceMaps.inFocusedLayer(2884, 9798, taverley, false));
		assertTrue(InstanceMaps.inFocusedLayer(3222, 3218, null, true));
	}

	@Test
	public void connectedComplexClipsToEveryOverlappingBoxNotJustTheSmallest()
	{
		// Entering Taverley Dungeon from the Dwarven Mines side must still paint the Taverley half.
		assertTrue(InstanceMaps.clipMapsForZone(UndergroundZone.DWARVEN_MINES)
			.contains(WikiMap.TAVERLEY_UNDERGROUND));
		assertTrue(InstanceMaps.clipMapsForZone(UndergroundZone.DWARVEN_MINES)
			.contains(WikiMap.DWARVEN_MINES));
		assertTrue(InstanceMaps.clipMapsForZone(UndergroundZone.TAVERLEY_DUNGEON)
			.contains(WikiMap.DWARVEN_MINES));

		// One hop only: a zone whose box overlaps nothing keeps its single box (or none).
		assertTrue(InstanceMaps.clipMapsForZone(UndergroundZone.MOLE_HOLE)
			.contains(WikiMap.MOLE_HOLE));
	}

	@Test
	public void morUlRekClipOverrideCoversTheInfernoTheWikiBoxCutsOff()
	{
		final UndergroundZone tz = UndergroundZone.TZHAAR_CITY;
		assertTrue("Mor Ul Rek must carry its own clip box", tz.hasClipOverride());
		// The Inferno (~2272, 5337) sits past the wiki TzHaar Area box (maxY 5248) but inside the
		// override, so its terrain and pins stay on the Mor Ul Rek layer.
		assertTrue(InstanceMaps.belongsToZone(2272, 5337, tz));
		assertTrue(InstanceMaps.belongsToZone(2400, 5092, tz)); // Fight Cave
		assertFalse(InstanceMaps.belongsToZone(2496, 3170, tz)); // the surface entrance is not "inside"
	}

	@Test
	public void clipOverrideBeatsSharedCreatureCreationWikiBox()
	{
		assertEquals(UndergroundZone.TOWER_OF_LIFE_BASEMENT, InstanceMaps.zoneForPoint(3059, 4397));
		assertEquals(UndergroundZone.CORPOREAL_BEAST, InstanceMaps.zoneForPoint(2967, 4382));
		assertEquals(2649, InstanceMaps.toDisplayX(3059, 4397, 3222, 3218));
		assertEquals(3213, InstanceMaps.toDisplayY(3059, 4397, 3222, 3218));
		assertEquals(3202, InstanceMaps.toDisplayX(2967, 4382, 3222, 3218));
		assertEquals(3681, InstanceMaps.toDisplayY(2967, 4382, 3222, 3218));
	}

	@Test
	public void clipOverrideKeepsYanilleOffTreeGnomeWikiBox()
	{
		assertEquals(UndergroundZone.TREE_GNOME_VILLAGE_DUNGEON, InstanceMaps.zoneForPoint(2533, 9556));
		assertEquals(UndergroundZone.YANILLE_AGILITY_DUNGEON, InstanceMaps.zoneForPoint(2569, 9525));
		assertEquals(2533, InstanceMaps.toDisplayX(2533, 9556, 3222, 3218));
		assertEquals(3155, InstanceMaps.toDisplayY(2533, 9556, 3222, 3218));
	}

	@Test
	public void krukTemplateBelongsToApeAtollDungeon()
	{
		assertEquals(UndergroundZone.APE_ATOLL_DUNGEON, InstanceMaps.zoneForPoint(2450, 9150));
		assertTrue(InstanceMaps.belongsToZone(2450, 9150, UndergroundZone.APE_ATOLL_DUNGEON));
	}

	@Test
	public void isleOfSoulsInteriorProjectsToItsCanonicalEntrance()
	{
		assertEquals(2309, InstanceMaps.toDisplayX(2135, 9320, 3222, 3218));
		assertEquals(2919, InstanceMaps.toDisplayY(2135, 9320, 3222, 3218));
	}

	@Test
	public void focusedHitSkipsOverworldWhenPeekingADungeon()
	{
		final String overworld = InstanceMaps.firstHit(2884, 3398, UndergroundZone.TAVERLEY_DUNGEON, true,
			(x, y) -> y < 4200 ? "overworld" : null);
		assertNull(overworld);

		final String dungeon = InstanceMaps.firstHit(2884, 3398, UndergroundZone.TAVERLEY_DUNGEON, true,
			(x, y) -> y > 9000 && InstanceMaps.belongsToZone(x, y, UndergroundZone.TAVERLEY_DUNGEON) ? "dungeon" : null);
		assertEquals("dungeon", dungeon);
	}
}
