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
package com.bettermap.ui;

import com.bettermap.BetterMapConfig;
import com.bettermap.map.MapCamera;
import com.bettermap.map.PoiCategory;
import com.bettermap.map.PoiIndex;
import com.bettermap.ui.markers.PoiMarkerRenderer;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.function.Consumer;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PoiZoomTest
{
	private static final String[] KEYS = {
		"bank", "general_store", "fishing_spot", "agility_short-cut", "lookout_point",
		"transportation", "quest_start", "minigame", "raids_lobby", "altar",
		"dungeon", "slayer_master", "combat_tutor", "unknown_native_icon"
	};

	@Test
	public void everyIconCategoryHonorsItsConfiguredThreshold()
	{
		final ThresholdConfig config = new ThresholdConfig();
		final MapCamera camera = new MapCamera();
		camera.centerOn(3222, 3218);
		final PoiMarkerRenderer renderer = renderer(config, camera, null, new MapRenderStats());
		final EnumSet<PoiCategory> covered = EnumSet.of(PoiCategory.PLACES);
		for (String key : KEYS)
		{
			covered.add(PoiCategory.of(key));
			final PoiIndex.Poi poi = new PoiIndex.Poi(3222, 3218, 0, key, key);
			camera.setZoom(3.99);
			assertFalse(key + " below threshold", renderer.shouldDrawPoiIcon(poi));
			camera.setZoom(4.0);
			assertTrue(key + " at threshold", renderer.shouldDrawPoiIcon(poi));
		}
		assertEquals("No category may be omitted", EnumSet.allOf(PoiCategory.class), covered);
		assertFalse("Place names are text, never icon badges", renderer.shouldDrawPoiIcon(
			new PoiIndex.Poi(3222, 3218, 0, "region_label", "Lumbridge")));
	}

	@Test
	public void loweredThresholdDrawsBelowTheOldGlobalTravelCutoff()
	{
		final ThresholdConfig config = new ThresholdConfig();
		config.minimum = 0.0;
		final MapCamera camera = new MapCamera();
		camera.centerOn(3222, 3218);
		camera.setZoom(MapCamera.MIN_ZOOM);
		final Rectangle bounds = new Rectangle(0, 0, 200, 200);
		camera.setViewport(bounds);
		camera.beginFrame();
		final PoiIndex index = new PoiIndex()
		{
			@Override
			public void forEachInArea(int plane, int minX, int maxX, int minY, int maxY, Consumer<Poi> consumer)
			{
				consumer.accept(new Poi(3222, 3218, 0, "bank", "Bank"));
			}

			@Override
			public BufferedImage icon(String key)
			{
				return new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
			}
		};
		final MapRenderStats stats = new MapRenderStats();
		final Graphics2D graphics = new BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB).createGraphics();
		try
		{
			renderer(config, camera, index, stats).drawPoiIcons(graphics, bounds, new ArrayList<>());
			assertEquals(1, stats.iconsDrawn);
		}
		finally
		{
			graphics.dispose();
		}
	}

	@Test
	public void shortcutsAreIndependentAndSlayerMastersFollowSkilling()
	{
		final BetterMapConfig config = new BetterMapConfig()
		{
			@Override
			public boolean iconSkilling() { return false; }
			@Override
			public boolean iconDungeons() { return true; }
		};
		final PoiMarkerRenderer renderer = renderer(config, null, null, new MapRenderStats());
		assertTrue(renderer.shouldDrawPoiIcon(new PoiIndex.Poi(3222, 3218, 0, "agility_short-cut", "Shortcut")));
		assertFalse(renderer.shouldDrawPoiIcon(new PoiIndex.Poi(3222, 3218, 0, "slayer_master", "Slayer Master")));
		assertFalse(renderer.shouldDrawPoiIcon(new PoiIndex.Poi(3222, 3218, 0, "lookout_point", "Lookout")));
	}

	private static PoiMarkerRenderer renderer(BetterMapConfig config, MapCamera camera, PoiIndex index, MapRenderStats stats)
	{
		return new PoiMarkerRenderer(config, camera, index, null, stats, null, new HashSet<>());
	}

	private static class ThresholdConfig implements BetterMapConfig
	{
		private double minimum = 4.0;
		@Override
		public boolean iconBanks() { return true; }
		@Override
		public boolean iconShops() { return true; }
		@Override
		public boolean iconSkilling() { return true; }
		@Override
		public boolean iconShortcuts() { return true; }
		@Override
		public boolean iconSailing() { return true; }
		@Override
		public boolean iconTravel() { return true; }
		@Override
		public boolean iconQuests() { return true; }
		@Override
		public boolean iconAltars() { return true; }
		@Override
		public boolean iconDungeons() { return true; }
		@Override
		public boolean iconServices() { return true; }
		@Override
		public boolean iconOther() { return true; }
		@Override
		public boolean showLargeUndergroundSymbols() { return false; }
		@Override
		public double bankMinZoom() { return minimum; }
		@Override
		public double shopMinZoom() { return minimum; }
		@Override
		public double skillingMinZoom() { return minimum; }
		@Override
		public double shortcutMinZoom() { return minimum; }
		@Override
		public double sailingLookoutMinZoom() { return minimum; }
		@Override
		public double travelMinZoom() { return minimum; }
		@Override
		public double questMinZoom() { return minimum; }
		@Override
		public double activityMinZoom() { return minimum; }
		@Override
		public double altarMinZoom() { return minimum; }
		@Override
		public double dungeonMinZoom() { return minimum; }
		@Override
		public double slayerMasterMinZoom() { return minimum; }
		@Override
		public double serviceMinZoom() { return minimum; }
		@Override
		public double otherMinZoom() { return minimum; }
	}
}
