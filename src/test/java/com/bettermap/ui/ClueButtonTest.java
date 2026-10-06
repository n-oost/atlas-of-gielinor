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

import static bettermap.ui.MapStyle.LEFT_TOOLBAR_BUTTON_SIZE;
import static bettermap.ui.MapStyle.LEFT_TOOLBAR_FINDER;
import static bettermap.ui.MapStyle.LEFT_TOOLBAR_LAYERS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.components.LayoutableRenderableEntity;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;
import org.junit.Test;

import bettermap.BetterMapConfig;
import bettermap.data.UndergroundZone;
import bettermap.map.ClueScrollTracker;
import bettermap.map.MapCamera;
import bettermap.map.QuestHelperTracker;
import bettermap.map.WorldMapInput;
import bettermap.map.WorldMapPointReader;
import bettermap.ui.MapChromeRenderer;
import bettermap.ui.MapLayout;

public class ClueButtonTest
{
	@org.junit.BeforeClass
	public static void loadMapData()
	{
		bettermap.map.MapData.load();
		org.junit.Assert.assertTrue("Map catalogs must load before testing", bettermap.map.MapData.isReady());
	}

	private static class TestConfig implements BetterMapConfig
	{
		boolean showClueScroll = true;
		boolean showCoordinateOverlay = false;
		boolean fullscreenMap = false;

		@Override
		public boolean showClueScroll()
		{
			return showClueScroll;
		}

		@Override
		public boolean showCoordinateOverlay()
		{
			return showCoordinateOverlay;
		}

		@Override
		public boolean fullscreenMap()
		{
			return fullscreenMap;
		}
	}

	private static class TestClueTracker extends ClueScrollTracker
	{
		List<WorldPoint> targets = Collections.singletonList(new WorldPoint(3200, 3200, 0));
		PanelComponent panel;

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
		public PanelComponent cluePanel()
		{
			return panel;
		}

	}

	private static PanelComponent panelOf(LayoutableRenderableEntity... children)
	{
		final PanelComponent p = new PanelComponent();
		p.getChildren().addAll(Arrays.asList(children));
		return p;
	}

	private static LineComponent line(String left)
	{
		return LineComponent.builder().left(left).build();
	}

	@Test
	public void clueButtonSitsBesideStatusChipAndDoesNotOverlapLeftToolbar()
	{
		final TestConfig config = new TestConfig();
		final MapCamera camera = new MapCamera();
		final WorldMapInput input = new WorldMapInput(camera, config, null, null);
		final MapLayout layout = new MapLayout(null, config);
		final TestClueTracker tracker = new TestClueTracker();

		final MapChromeRenderer renderer = new MapChromeRenderer(config, camera, input, layout, tracker, noQuestStep());

		final BufferedImage img = new BufferedImage(1000, 800, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = img.createGraphics();
		final Rectangle bounds = new Rectangle(0, 0, 1000, 800);

		renderer.drawStatusChip(g, bounds);
		renderer.drawClueButton(g, bounds);

		final Rectangle clueBtn = camera.getClueButton();
		assertNotNull("Clue button must be present when active clue targets exist", clueBtn);

		// Must sit on the top chip row (y = bounds.y + 8), not toolbar level (y = 34) or below.
		assertEquals("Clue button Y must align with top chip row", bounds.y + 8, clueBtn.y);
		assertEquals("Clue button height must match chip height", 20, clueBtn.height);

		// Must be placed to the right of the status chip (status chip starts at x=8, min width ~80).
		assertTrue("Clue button must be placed after status chip", clueBtn.x > bounds.x + 80);

		// Left toolbar buttons
		final int toolbarTop = layout.leftToolbarTop(bounds);
		final int size = LEFT_TOOLBAR_BUTTON_SIZE;
		final Rectangle layersBtn = new Rectangle(layout.leftToolbarButtonX(bounds, LEFT_TOOLBAR_LAYERS), toolbarTop, size, size);
		final Rectangle finderBtn = new Rectangle(layout.leftToolbarButtonX(bounds, LEFT_TOOLBAR_FINDER), toolbarTop, size, size);

		// Clue button must not overlap either toolbar button
		assertFalse("Clue button must not overlap Layers button", clueBtn.intersects(layersBtn));
		assertFalse("Clue button must not overlap Finder button", clueBtn.intersects(finderBtn));

		// And must be strictly above toolbar top (34)
		assertTrue("Clue button bottom must be above toolbar top", clueBtn.y + clueBtn.height <= toolbarTop);
	}

	@Test
	public void clueButtonSitsAfterUndergroundReturnButtonWhenActive()
	{
		final TestConfig config = new TestConfig();
		final MapCamera camera = new MapCamera();
		final WorldMapInput input = new WorldMapInput(camera, config, null, null);
		final MapLayout layout = new MapLayout(null, config);
		final TestClueTracker tracker = new TestClueTracker();

		// Activate underground mode
		camera.setUndergroundMode(UndergroundZone.ALL_ZONES.get(0));

		final MapChromeRenderer renderer = new MapChromeRenderer(config, camera, input, layout, tracker, noQuestStep());

		final BufferedImage img = new BufferedImage(1000, 800, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = img.createGraphics();
		final Rectangle bounds = new Rectangle(0, 0, 1000, 800);

		renderer.drawStatusChip(g, bounds);
		final Rectangle returnBtn = camera.getUndergroundReturnButton();
		assertNotNull("Underground return button must be set", returnBtn);

		renderer.drawClueButton(g, bounds);
		final Rectangle clueBtn = camera.getClueButton();
		assertNotNull("Clue button must be set", clueBtn);

		// Clue button must sit after the return button
		assertTrue("Clue button X must be to the right of return button",
			clueBtn.x >= returnBtn.x + returnBtn.width + 8);
		assertEquals("Clue button Y must align with top chip row", bounds.y + 8, clueBtn.y);
	}

	@Test
	public void clueButtonClearedWhenNoClueOrDisabled()
	{
		final TestConfig config = new TestConfig();
		final MapCamera camera = new MapCamera();
		final WorldMapInput input = new WorldMapInput(camera, config, null, null);
		final MapLayout layout = new MapLayout(null, config);
		final TestClueTracker tracker = new TestClueTracker();

		final MapChromeRenderer renderer = new MapChromeRenderer(config, camera, input, layout, tracker, noQuestStep());

		final BufferedImage img = new BufferedImage(1000, 800, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = img.createGraphics();
		final Rectangle bounds = new Rectangle(0, 0, 1000, 800);

		// Config disabled
		config.showClueScroll = false;
		renderer.drawStatusChip(g, bounds);
		renderer.drawClueButton(g, bounds);
		assertNull("Clue button must be null when config disabled", camera.getClueButton());

		// Empty locations
		config.showClueScroll = true;
		tracker.targets = Collections.emptyList();
		renderer.drawClueButton(g, bounds);
		assertNull("Clue button must be null when no locations", camera.getClueButton());
	}

	@Test
	public void cluePanelRendersOnFullscreenMapAndYieldsToMenus()
	{
		final TestConfig config = new TestConfig();
		config.fullscreenMap = true;
		final MapCamera camera = new MapCamera();
		final WorldMapInput input = new WorldMapInput(camera, config, null, null);
		final MapLayout layout = new MapLayout(null, config);
		final TestClueTracker tracker = new TestClueTracker();
		tracker.panel = panelOf(
			TitleComponent.builder().text("Emotes:").build(),
			line("Think"),
			LineComponent.builder().left("STASH Unit:").right("\u2713").build(),
			LineComponent.builder().left("Mithril chainbody").right("\u2713").build(),
			line("\u2694 Kill the Double Agent (lvl 65)")
		);

		final MapChromeRenderer renderer = new MapChromeRenderer(config, camera, input, layout, tracker, noQuestStep());
		final BufferedImage img = new BufferedImage(1000, 800, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = img.createGraphics();
		final Rectangle bounds = new Rectangle(0, 0, 1000, 800);

		// Expanded state
		camera.setCluePanelCollapsed(false);
		renderer.drawCluePanel(g, bounds);
		final Rectangle expandedBounds = camera.getCluePanelBounds();
		assertNotNull("Expanded clue panel bounds must not be null", expandedBounds);
		assertTrue("Expanded panel height must be greater than header height (24)", expandedBounds.height > 24);
		assertNotNull("Header bounds must not be null", camera.getCluePanelHeaderBounds());

		// Collapsed state
		camera.setCluePanelCollapsed(true);
		renderer.drawCluePanel(g, bounds);
		final Rectangle collapsedBounds = camera.getCluePanelBounds();
		assertNotNull("Collapsed clue panel bounds must not be null", collapsedBounds);
		assertEquals("Collapsed panel height must equal header height (24)", 24, collapsedBounds.height);

		// Empty native hint panel: no custom fallback card.
		camera.setCluePanelCollapsed(false);
		tracker.panel = new PanelComponent();
		renderer.drawCluePanel(g, bounds);
		assertNull("Clue panel needs native hint content", camera.getCluePanelBounds());
		tracker.panel = panelOf(line("Think"));

		// Yield when layers panel is open
		camera.setLayersPanelOpen(true);
		renderer.drawCluePanel(g, bounds);
		assertNull("Clue panel must yield when layers panel is open", camera.getCluePanelBounds());
		camera.setLayersPanelOpen(false);

		// Yield when finder panel is open
		camera.setFinderPanelOpen(true);
		renderer.drawCluePanel(g, bounds);
		assertNull("Clue panel must yield when finder panel is open", camera.getCluePanelBounds());
		camera.setFinderPanelOpen(false);

		// Not drawn when fullscreenMap is false
		config.fullscreenMap = false;
		renderer.drawCluePanel(g, bounds);
		assertNull("Clue panel must not draw in windowed mode", camera.getCluePanelBounds());
	}

	@Test
	public void cluePanelHeightGrowsWithHintContent()
	{
		final TestConfig config = new TestConfig();
		config.fullscreenMap = true;
		final MapLayout layout = new MapLayout(null, config);
		final BufferedImage img = new BufferedImage(1000, 800, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = img.createGraphics();
		final Rectangle bounds = new Rectangle(0, 0, 1000, 800);

		final MapCamera smallCam = new MapCamera();
		final TestClueTracker small = new TestClueTracker();
		small.panel = panelOf(line("Think"), line("Dance"));
		smallCam.setCluePanelCollapsed(false);
		new MapChromeRenderer(config, smallCam,
			new WorldMapInput(smallCam, config, null, null), layout, small, noQuestStep()).drawCluePanel(g, bounds);
		final int smallH = smallCam.getCluePanelBounds().height;

		final MapCamera bigCam = new MapCamera();
		final TestClueTracker big = new TestClueTracker();
		big.panel = panelOf(line("Think"), line("Dance"), line("Spin"), line("Cry"), line("Bow"), line("Wave"));
		bigCam.setCluePanelCollapsed(false);
		new MapChromeRenderer(config, bigCam,
			new WorldMapInput(bigCam, config, null, null), layout, big, noQuestStep()).drawCluePanel(g, bounds);
		final int bigH = bigCam.getCluePanelBounds().height;

		assertTrue("A 6-line hint panel must produce a taller card than a 2-line one (measure pass works): "
			+ bigH + " vs " + smallH, bigH > smallH);
	}

	@Test
	public void cluePanelClickInteractionInWorldMapInput()
	{
		final TestConfig config = new TestConfig();
		config.fullscreenMap = true;
		final MapCamera camera = new MapCamera();
		final WorldMapInput input = new WorldMapInput(camera, config, null, null);
		final MapLayout layout = new MapLayout(null, config);
		final TestClueTracker tracker = new TestClueTracker();
		tracker.panel = panelOf(line("Think"));

		final MapChromeRenderer renderer = new MapChromeRenderer(config, camera, input, layout, tracker, noQuestStep());
		final BufferedImage img = new BufferedImage(1000, 800, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = img.createGraphics();
		final Rectangle bounds = new Rectangle(0, 0, 1000, 800);

		camera.setActive(true);
		camera.setViewport(bounds);
		renderer.drawCluePanel(g, bounds);

		final java.awt.Component source = new java.awt.Component() {};
		// Header click toggles collapse
		camera.setCluePanelCollapsed(false);
		final Rectangle header = camera.getCluePanelHeaderBounds();
		assertNotNull(header);
		final java.awt.event.MouseEvent headerClick = new java.awt.event.MouseEvent(
			source, java.awt.event.MouseEvent.MOUSE_PRESSED,
			System.currentTimeMillis(), java.awt.event.MouseEvent.BUTTON1_DOWN_MASK,
			header.x + 2, header.y + 2, 1, false, java.awt.event.MouseEvent.BUTTON1
		);
		input.mousePressed(headerClick);
		assertTrue("Clicking header must collapse panel", camera.isCluePanelCollapsed());
		assertTrue("Clicking header must consume event", headerClick.isConsumed());

		// Panel body click consumes to prevent map pan
		final Rectangle panel = camera.getCluePanelBounds();
		final java.awt.event.MouseEvent panelClick = new java.awt.event.MouseEvent(
			source, java.awt.event.MouseEvent.MOUSE_PRESSED,
			System.currentTimeMillis(), java.awt.event.MouseEvent.BUTTON1_DOWN_MASK,
			panel.x + 10, panel.y + 10, 1, false, java.awt.event.MouseEvent.BUTTON1
		);
		input.mousePressed(panelClick);
		assertTrue("Clicking clue panel must consume event", panelClick.isConsumed());
	}

	/** A tracker with no world map point manager: never reports a quest step. */
	private static QuestHelperTracker noQuestStep()
	{
		return new QuestHelperTracker(null, new WorldMapPointReader());
	}
}
