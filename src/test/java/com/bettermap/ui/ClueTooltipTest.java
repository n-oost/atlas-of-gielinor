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
import com.bettermap.map.ClueScrollTracker;
import com.bettermap.map.MapCamera;
import com.bettermap.map.WorldMapInput;
import com.bettermap.ui.tooltips.PoiTooltipBuilder;
import com.bettermap.ui.tooltips.TooltipCard;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class ClueTooltipTest
{
	@org.junit.BeforeClass
	public static void loadMapData()
	{
		com.bettermap.map.MapData.load();
		org.junit.Assert.assertTrue("Map catalogs must load before testing", com.bettermap.map.MapData.isReady());
	}

	private static class TooltipTestConfig implements BetterMapConfig
	{
		boolean showTooltips = true;
		boolean showClueScroll = true;
		boolean showBoatLocations = false;
		boolean showPortNoticeBoards = false;
		boolean showSailingPorts = false;
		boolean showBossLocations = false;
		boolean showGroundItems = false;
		boolean showMonsterZones = false;

		@Override
		public boolean showTooltips()
		{
			return showTooltips;
		}

		@Override
		public boolean showClueScroll()
		{
			return showClueScroll;
		}

		@Override
		public boolean showBoatLocations()
		{
			return showBoatLocations;
		}

		@Override
		public boolean showPortNoticeBoards()
		{
			return showPortNoticeBoards;
		}

		@Override
		public boolean showSailingPorts()
		{
			return showSailingPorts;
		}

		@Override
		public boolean showBossLocations()
		{
			return showBossLocations;
		}

		@Override
		public boolean showGroundItems()
		{
			return showGroundItems;
		}

		@Override
		public boolean showMonsterZones()
		{
			return showMonsterZones;
		}

	}

	private static class TestClueTracker extends ClueScrollTracker
	{
		List<WorldPoint> targets = Collections.singletonList(new WorldPoint(3200, 3200, 0));
		PanelComponent panel;
		String enemyText;
		boolean reqSpade;
		boolean reqLight;

		TestClueTracker()
		{
			super(null);
		}

		@Override
		public List<WorldPoint> locations()
		{
			return targets;
		}

		@Override
		public String label()
		{
			return "Emote clue";
		}

		@Override
		public PanelComponent cluePanel()
		{
			return panel;
		}

		@Override
		public String enemy()
		{
			return enemyText;
		}

		@Override
		public boolean isRequiresSpade()
		{
			return reqSpade;
		}

		@Override
		public boolean isRequiresLight()
		{
			return reqLight;
		}
	}

	@Test
	public void tooltipRendersClueInformationWhenHoveringClueLocation()
	{
		final TooltipTestConfig config = new TooltipTestConfig();
		final MapCamera camera = new MapCamera();
		final WorldMapInput input = new WorldMapInput(camera, config, null, null);
		final TestClueTracker tracker = new TestClueTracker();
		final PanelComponent hintPanel = new PanelComponent();
		hintPanel.getChildren().add(TitleComponent.builder().text("Emotes:").build());
		hintPanel.getChildren().add(LineComponent.builder().left("Think").build());
		hintPanel.getChildren().add(LineComponent.builder().left("Spin").build());
		hintPanel.getChildren().add(LineComponent.builder().left("STASH Unit:").right("\u2717").build());
		hintPanel.getChildren().add(LineComponent.builder().left("Mithril chainbody").right("\u2713").build());
		hintPanel.getChildren().add(LineComponent.builder().left("\u2694 Kill the Double Agent (lvl 65)").build());
		tracker.panel = hintPanel;
		tracker.enemyText = "Kill the Double Agent (lvl 65)";
		tracker.reqSpade = true;

		final MapTooltipRenderer renderer = new MapTooltipRenderer(
			null, config, camera, input, null, null, null, null, null,
			tracker, null, null, null, null, null, null
		);
		final PoiTooltipBuilder tooltipBuilder = new PoiTooltipBuilder();
		final TooltipCard clueCard = tooltipBuilder.buildClueCard(
			new WorldPoint(3200, 3200, 0), tracker.label(), tracker.cluePanel());
		assertTrue("Clue hint panel must be included in the tooltip card",
			clueCard.getTrailingPanel() == hintPanel);

		final Rectangle bounds = new Rectangle(0, 0, 1000, 800);
		camera.setActive(true);
		camera.centerOn(3200, 3200); camera.beginFrame();
		camera.setZoom(1.0);
		camera.setPlane(0);
		camera.setViewport(bounds);

		// Position cursor at map center (which maps to world 3200, 3200)
		input.mouseMoved(new java.awt.event.MouseEvent(
			new java.awt.Component() {}, java.awt.event.MouseEvent.MOUSE_MOVED,
			System.currentTimeMillis(), 0, 500, 400, 0, false
		));

		final BufferedImage img = new BufferedImage(1000, 800, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = img.createGraphics();

		// Ensure drawTooltip runs cleanly without exceptions
		renderer.drawTooltip(g, bounds);

		// The clue hint panel must actually have been rendered (bounds set by PanelComponent.render).
		boolean tooltipDrawn = false;
		for (int y = 0; y < img.getHeight() && !tooltipDrawn; y++)
		{
			for (int x = 0; x < img.getWidth(); x++)
			{
				if ((img.getRGB(x, y) >>> 24) != 0)
				{
					tooltipDrawn = true;
					break;
				}
			}
		}
		assertTrue("Clue tooltip must be drawn", tooltipDrawn);

		// Now test suppression when hovering over the clue panel
		final Rectangle cluePanel = new Rectangle(8, 66, 200, 120);
		camera.setCluePanelBounds(cluePanel);
		input.mouseMoved(new java.awt.event.MouseEvent(
			new java.awt.Component() {}, java.awt.event.MouseEvent.MOUSE_MOVED,
			System.currentTimeMillis(), 0, 50, 80, 0, false
		));

		renderer.drawTooltip(g, bounds);
	}
}
