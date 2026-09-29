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

import com.bettermap.BetterMapConfig;
import com.bettermap.data.OverlayCluster;
import com.bettermap.data.UndergroundZone;
import com.bettermap.ui.BetterWorldMapOverlay;
import java.io.File;
import java.util.Collections;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class BetterWorldMapOverlayPoiTest
{
	private BetterMapConfig createConfig(
		final boolean iconSkilling,
		final boolean iconBanks,
		final boolean iconShops)
	{
		return new BetterMapConfig()
		{
			@Override
			public boolean iconSkilling()
			{
				return iconSkilling;
			}

			@Override
			public boolean iconBanks()
			{
				return iconBanks;
			}

			@Override
			public boolean iconShops()
			{
				return iconShops;
			}
		};
	}

	private BetterWorldMapOverlay createOverlay(BetterMapConfig config)
	{
		return new BetterWorldMapOverlay(
			null, config, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
			null, null);
	}

	@Test
	public void defaultSettingsAllowSkillingWhileBlockingOtherCategories()
	{
		// Default settings: iconSkilling is true, iconBanks and iconShops are false
		final BetterMapConfig config = createConfig(true, false, false);
		final BetterWorldMapOverlay overlay = createOverlay(config);

		final PoiIndex.Poi agilityCourse = new PoiIndex.Poi(3103, 3279, 0, "agility_training", "Draynor Rooftop");
		final PoiIndex.Poi agilityShortcut = new PoiIndex.Poi(3111, 3252, 0, "agility_short-cut", "Fence Shortcut");
		final PoiIndex.Poi hunterSpot = new PoiIndex.Poi(2550, 2900, 0, "hunter_training", "Feldip Hills");
		final PoiIndex.Poi fishing = new PoiIndex.Poi(3240, 3150, 0, "fishing_spot", "Lumbridge Fishing");
		final PoiIndex.Poi bank = new PoiIndex.Poi(3208, 3218, 0, "bank", "Lumbridge Bank");
		final PoiIndex.Poi shop = new PoiIndex.Poi(3210, 3245, 0, "general_store", "Lumbridge General Store");

		assertTrue("agility_training must draw by default", overlay.shouldDrawPoiIcon(agilityCourse));
		assertTrue("agility_short-cut must draw by default", overlay.shouldDrawPoiIcon(agilityShortcut));
		assertTrue("hunter_training must draw by default", overlay.shouldDrawPoiIcon(hunterSpot));
		assertTrue("fishing_spot must draw by default when skilling is on", overlay.shouldDrawPoiIcon(fishing));

		assertFalse("bank badges are disabled by default", overlay.shouldDrawPoiIcon(bank));
		assertFalse("shop badges are disabled by default", overlay.shouldDrawPoiIcon(shop));
	}

	@Test
	public void disablingSkillingHidesAllSkillingAndDefaultKeyBadges()
	{
		final BetterMapConfig config = createConfig(false, false, false);
		final BetterWorldMapOverlay overlay = createOverlay(config);

		final PoiIndex.Poi agilityCourse = new PoiIndex.Poi(3103, 3279, 0, "agility_training", "Draynor Rooftop");
		final PoiIndex.Poi agilityShortcut = new PoiIndex.Poi(3111, 3252, 0, "agility_short-cut", "Fence Shortcut");
		final PoiIndex.Poi hunterSpot = new PoiIndex.Poi(2550, 2900, 0, "hunter_training", "Feldip Hills");
		final PoiIndex.Poi fishing = new PoiIndex.Poi(3240, 3150, 0, "fishing_spot", "Lumbridge Fishing");

		assertFalse("skilling toggle off disables agility training", overlay.shouldDrawPoiIcon(agilityCourse));
		assertFalse("skilling toggle off disables agility shortcuts", overlay.shouldDrawPoiIcon(agilityShortcut));
		assertFalse("skilling toggle off disables hunter training", overlay.shouldDrawPoiIcon(hunterSpot));
		assertFalse("skilling toggle off disables generic skilling badges", overlay.shouldDrawPoiIcon(fishing));
	}

	@Test
	public void enablingBanksWithSkillingOffDrawsBanksOnly()
	{
		final BetterMapConfig config = createConfig(false, true, false);
		final BetterWorldMapOverlay overlay = createOverlay(config);

		final PoiIndex.Poi bank = new PoiIndex.Poi(3208, 3218, 0, "bank", "Lumbridge Bank");
		final PoiIndex.Poi shop = new PoiIndex.Poi(3210, 3245, 0, "general_store", "Lumbridge General Store");
		final PoiIndex.Poi agilityCourse = new PoiIndex.Poi(3103, 3279, 0, "agility_training", "Draynor Rooftop");

		assertTrue("bank draws when iconBanks is true", overlay.shouldDrawPoiIcon(bank));
		assertFalse("shop remains hidden when iconShops is false", overlay.shouldDrawPoiIcon(shop));
		assertFalse("agility training remains hidden when iconSkilling is false", overlay.shouldDrawPoiIcon(agilityCourse));
	}

	@Test
	public void regionLabelNeverDraws()
	{
		final BetterMapConfig config = createConfig(true, true, true);
		final BetterWorldMapOverlay overlay = createOverlay(config);

		final PoiIndex.Poi regionLabel = new PoiIndex.Poi(3200, 3200, 0, "region_label", "Lumbridge");

		assertFalse("region_label has no category toggle and never draws as an icon badge", overlay.shouldDrawPoiIcon(regionLabel));
	}

	@Test
	public void nullPoiHandlingIsSafe()
	{
		final BetterMapConfig config = createConfig(true, true, true);
		final BetterWorldMapOverlay overlay = createOverlay(config);

		assertFalse(overlay.shouldDrawPoiIcon(null));
		assertFalse(overlay.shouldDrawPoiIcon(new PoiIndex.Poi(0, 0, 0, null, null)));
	}

	@Test
	public void largeLayerSymbolsHideDungeonPoiNearZoneEntrances()
	{
		final BetterMapConfig config = new BetterMapConfig()
		{
			@Override
			public boolean iconDungeons()
			{
				return true;
			}

			@Override
			public boolean showLargeUndergroundSymbols()
			{
				return true;
			}
		};
		final BetterWorldMapOverlay overlay = createOverlay(config);
		final PoiIndex.Poi cellar = new PoiIndex.Poi(3218, 3218, 0, "dungeon", "Lumbridge Cellar");
		final PoiIndex.Poi basement = new PoiIndex.Poi(3218, 3218, 0, "basement", "Lumbridge Cellar");
		final PoiIndex.Poi far = new PoiIndex.Poi(100, 100, 0, "dungeon", "Far dungeon");
		assertFalse("zone entrance uses the overlay dungeon ! instead of a second POI badge",
			overlay.shouldDrawPoiIcon(cellar));
		assertTrue("basements use their separate link marker", overlay.shouldDrawPoiIcon(basement));
		assertTrue("dungeon POIs away from authored zones still draw", overlay.shouldDrawPoiIcon(far));
		final PoiIndex.Poi isleEntrance = new PoiIndex.Poi(2309, 2919, 0, "dungeon", "Isle of Souls Dungeon");
		assertFalse("the canonical entrance uses one interactive layer symbol",
			overlay.shouldDrawPoiIcon(isleEntrance));
	}

	private BetterWorldMapOverlay createOverlay(
		BetterMapConfig config, MapCamera camera, DungeonPieceIndex dungeonPieceIndex)
	{
		return new BetterWorldMapOverlay(
			null, config, null, camera, null, null, null, null, null, null, null, null, null, null, null, null,
			null, null, dungeonPieceIndex);
	}

	@Test
	public void dungeonPoiDrawsAtZoneEntranceWhenLargeSymbolsAreOff()
	{
		final BetterMapConfig config = new BetterMapConfig()
		{
			@Override
			public boolean iconDungeons()
			{
				return true;
			}

			@Override
			public boolean showLargeUndergroundSymbols()
			{
				return false;
			}
		};
		final BetterWorldMapOverlay overlay = createOverlay(config);
		final PoiIndex.Poi cellar = new PoiIndex.Poi(3218, 3218, 0, "dungeon", "Lumbridge Cellar");
		assertTrue(overlay.shouldDrawPoiIcon(cellar));
	}

	@Test
	public void regionalDungeonPreviewHidesOnlyPoisCoveredByGeometry()
	{
		final BetterMapConfig config = createConfig(false, true, false);
		final MapCamera camera = new MapCamera();
		final DungeonPieceIndex pieces = new DungeonPieceIndex();
		pieces.load(new File("nonexistent_dir"));
		camera.setHoveredOverlayCluster(new OverlayCluster(
			"asgarnia", "Asgarnia underground", 2800, 3120, 3110, 3525,
			Collections.singletonList(UndergroundZone.STRONGHOLD_OF_SECURITY), 2992, 3408));
		final BetterWorldMapOverlay overlay = createOverlay(config, camera, pieces);

		assertFalse("POI underneath the projected Vault of War must be hidden",
			overlay.shouldDrawPoiIcon(new PoiIndex.Poi(3108, 3393, 0, "bank", "Covered bank")));
		assertTrue("POI outside the projected dungeon remains visible",
			overlay.shouldDrawPoiIcon(new PoiIndex.Poi(3200, 3393, 0, "bank", "Outside bank")));
	}

	@Test
	public void allIconTogglesDisabledHidesAllPoiCategoriesAndUnknownKeys()
	{
		final BetterMapConfig allDisabled = new BetterMapConfig()
		{
			@Override
			public boolean iconBanks()
			{
				return false;
			}

			@Override
			public boolean iconShops()
			{
				return false;
			}

			@Override
			public boolean iconSkilling()
			{
				return false;
			}

			@Override
			public boolean iconTravel()
			{
				return false;
			}

			@Override
			public boolean iconQuests()
			{
				return false;
			}

			@Override
			public boolean iconAltars()
			{
				return false;
			}

			@Override
			public boolean iconDungeons()
			{
				return false;
			}

			@Override
			public boolean iconServices()
			{
				return false;
			}

			@Override
			public boolean showPluginMarkers()
			{
				return false;
			}

			@Override
			public boolean showPlayerMarker()
			{
				return false;
			}

			@Override
			public boolean showBossLocations()
			{
				return false;
			}

			@Override
			public boolean showMonsterZones()
			{
				return false;
			}

			@Override
			public boolean showMonsterIcons()
			{
				return false;
			}

			@Override
			public boolean showMonsterLabels()
			{
				return false;
			}

			@Override
			public boolean highlightSlayerTask()
			{
				return false;
			}

			@Override
			public boolean showBoatLocations()
			{
				return false;
			}

			@Override
			public boolean showBoatNames()
			{
				return false;
			}

			@Override
			public boolean showSailingPorts()
			{
				return false;
			}

			@Override
			public boolean showPortNoticeBoards()
			{
				return false;
			}

			@Override
			public boolean showLargeUndergroundSymbols()
			{
				return false;
			}

			@Override
			public boolean showTravelRoutes()
			{
				return false;
			}
		};

		final BetterWorldMapOverlay overlay = createOverlay(allDisabled);

		final PoiIndex.Poi bank = new PoiIndex.Poi(3208, 3218, 0, "bank", "Lumbridge Bank");
		final PoiIndex.Poi shop = new PoiIndex.Poi(3210, 3245, 0, "general_store", "Lumbridge General Store");
		final PoiIndex.Poi skilling = new PoiIndex.Poi(3240, 3150, 0, "fishing_spot", "Lumbridge Fishing");
		final PoiIndex.Poi unknown = new PoiIndex.Poi(3200, 3200, 0, "completely_unknown_key_xyz", "Unknown Spot");

		assertFalse("bank must return false when all toggles are disabled", overlay.shouldDrawPoiIcon(bank));
		assertFalse("shop must return false when all toggles are disabled", overlay.shouldDrawPoiIcon(shop));
		assertFalse("skilling must return false when all toggles are disabled", overlay.shouldDrawPoiIcon(skilling));
		assertFalse("unknown key must return false when all toggles are disabled", overlay.shouldDrawPoiIcon(unknown));
	}
}
