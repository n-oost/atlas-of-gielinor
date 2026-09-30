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
import com.bettermap.map.PoiDetails;
import com.bettermap.map.PoiIndex;
import com.bettermap.map.WorldMapInput;
import com.bettermap.map.WorldMapPointReader;
import com.bettermap.ui.tooltips.PoiTooltipBuilder;
import com.bettermap.ui.tooltips.TooltipCard;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DualTooltipBugFixTest
{
	private static class BugFixTestConfig implements BetterMapConfig
	{
		@Override
		public boolean showTooltips()
		{
			return true;
		}

		@Override
		public boolean expandedTooltips()
		{
			return false; // compact mode
		}

		@Override
		public boolean showClueScroll()
		{
			return false;
		}

		@Override
		public boolean showBoatLocations()
		{
			return false;
		}

		@Override
		public boolean showPortNoticeBoards()
		{
			return false;
		}

		@Override
		public boolean showSailingPorts()
		{
			return false;
		}

		@Override
		public boolean showBossLocations()
		{
			return false;
		}

		@Override
		public boolean showGroundItems()
		{
			return false;
		}

		@Override
		public boolean showMonsterZones()
		{
			return false;
		}
	}

	private BugFixTestConfig config;
	private MapCamera camera;
	private WorldMapInput input;
	private PoiTooltipBuilder poiTooltipBuilder;
	private BufferedImage testIcon;

	@Before
	public void setUp()
	{
		config = new BugFixTestConfig();
		camera = new MapCamera();
		input = new WorldMapInput(camera, config, null, null);
		poiTooltipBuilder = new PoiTooltipBuilder();
		testIcon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
	}

	@Test
	public void invisibleRuneLiteOverlayPointDoesNotTriggerPhantomCardOnAdjacentGrass()
	{
		// Simulate RuneLite's invisible WorldMapPoint: image == null, name or tooltip set (e.g. "wyrmscraig goats 60")
		final WorldMapPoint invisiblePoint = new WorldMapPoint(new WorldPoint(2572, 2195, 0), null);
		invisiblePoint.setName("Wyrmscraig goats");
		invisiblePoint.setTooltip("60");

		final WorldMapPointReader reader = new WorldMapPointReader()
		{
			@Override
			public List<WorldMapPoint> points(WorldMapPointManager manager)
			{
				return Collections.singletonList(invisiblePoint);
			}
		};

		final MapTooltipRenderer renderer = new MapTooltipRenderer(
			null, config, camera, input, null, null, null, null, null,
			null, null, null, null, null, null, reader
		);

		final Rectangle bounds = new Rectangle(0, 0, 1000, 800);
		camera.setActive(true);
		camera.centerOn(2572, 2195);
		camera.beginFrame();
		camera.setZoom(1.0);
		camera.setPlane(0);
		camera.setViewport(bounds);

		// Cursor moved slightly off (3 tiles away) into grass at (2575, 2195)
		final int cursorX = (int) Math.round(camera.screenX(2575.5, 2195.5, bounds));
		final int cursorY = (int) Math.round(camera.screenY(2575.5, 2195.5, bounds));

		input.mouseMoved(new MouseEvent(
			new Component() {}, MouseEvent.MOUSE_MOVED,
			System.currentTimeMillis(), 0, cursorX, cursorY, 0, false
		));

		final BufferedImage buffer = new BufferedImage(1000, 800, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = buffer.createGraphics();
		renderer.drawTooltip(g, bounds);

		// Verify nothing was drawn onto the buffer at empty terrain
		boolean anyPixelDrawn = false;
		for (int y = 0; y < buffer.getHeight() && !anyPixelDrawn; y++)
		{
			for (int x = 0; x < buffer.getWidth(); x++)
			{
				if ((buffer.getRGB(x, y) >>> 24) != 0)
				{
					anyPixelDrawn = true;
					break;
				}
			}
		}
		assertFalse("No phantom tooltip card should be drawn on empty grass", anyPixelDrawn);
	}

	@Test
	public void visibleRuneLiteMarkerWithImageTriggersTooltipCard()
	{
		// Marker that actually has an icon (e.g. quest helper or clue icon)
		final WorldMapPoint visiblePoint = new WorldMapPoint(new WorldPoint(2572, 2195, 0), testIcon);
		visiblePoint.setName("Quest Marker");

		final WorldMapPointReader reader = new WorldMapPointReader()
		{
			@Override
			public List<WorldMapPoint> points(WorldMapPointManager manager)
			{
				return Collections.singletonList(visiblePoint);
			}
		};

		final MapTooltipRenderer renderer = new MapTooltipRenderer(
			null, config, camera, input, null, null, null, null, null,
			null, null, null, null, null, null, reader
		);

		final Rectangle bounds = new Rectangle(0, 0, 1000, 800);
		camera.setActive(true);
		camera.centerOn(2572, 2195);
		camera.beginFrame();
		camera.setZoom(1.0);
		camera.setPlane(0);
		camera.setViewport(bounds);

		// Cursor exactly on tile (2572, 2195)
		final int cursorX = (int) Math.round(camera.screenX(2572.5, 2195.5, bounds));
		final int cursorY = (int) Math.round(camera.screenY(2572.5, 2195.5, bounds));

		input.mouseMoved(new MouseEvent(
			new Component() {}, MouseEvent.MOUSE_MOVED,
			System.currentTimeMillis(), 0, cursorX, cursorY, 0, false
		));

		final BufferedImage buffer = new BufferedImage(1000, 800, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = buffer.createGraphics();
		renderer.drawTooltip(g, bounds);

		boolean anyPixelDrawn = false;
		for (int y = 0; y < buffer.getHeight() && !anyPixelDrawn; y++)
		{
			for (int x = 0; x < buffer.getWidth(); x++)
			{
				if ((buffer.getRGB(x, y) >>> 24) != 0)
				{
					anyPixelDrawn = true;
					break;
				}
			}
		}
		assertTrue("Visible marker with icon should trigger a drawn tooltip card", anyPixelDrawn);
	}

	@Test
	public void compactTooltipDisplaysBasicIconAndNameOnlyWithoutWyrmscraigPrefix()
	{
		// Hunter training POI at Wyrmscraig
		final PoiIndex.Poi goatPoi = new PoiIndex.Poi(2572, 2195, 0, "hunter_training", "Goats");
		final PoiDetails.Detail goatDetail = PoiDetails.getDetail(goatPoi, 2572, 2195, 0);

		final TooltipCard compactCard = poiTooltipBuilder.buildPoiCard(goatDetail, goatPoi, testIcon, true);
		assertNotNull(compactCard);
		// Compact title should just be "<NAME>": "Goats"
		assertEquals("Goats", compactCard.getTitle());
		// Compact card should have icon
		assertEquals(testIcon, compactCard.getIcon());
		// Compact form should have basic <ICON> <NAME> format with 0 extra lines
		assertEquals(0, compactCard.getLines().size());

		// Expanded form should preserve detail lines and level info
		final TooltipCard expandedCard = poiTooltipBuilder.buildPoiCard(goatDetail, goatPoi, testIcon, false);
		assertNotNull(expandedCard);
		assertEquals("Goats (Level 60)", expandedCard.getTitle());
		assertTrue(expandedCard.getLines().size() > 0);
	}
}
