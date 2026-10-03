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
 *    and/or other materials provided with the distribution sailing.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.bettermap.BetterMapConfig;
import com.bettermap.BetterMapPlugin;
import com.bettermap.data.sailing.BoatTracker;
import com.bettermap.data.sailing.BoatType;
import com.bettermap.data.sailing.PlayerBoat;
import com.bettermap.data.sailing.SailingPort;
import com.bettermap.map.ClueScrollTracker;
import com.bettermap.map.MapCamera;
import com.bettermap.map.QuestHelperTracker;
import com.bettermap.map.ShortestPathTracker;
import com.bettermap.map.WorldMapInput;
import com.bettermap.map.WorldMapPointReader;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.runelite.api.coords.WorldPoint;
import org.junit.Test;

public class DestinationAndBoatsButtonTest
{
	private static class TestConfig implements BetterMapConfig
	{
		boolean showClueScroll = false;
		boolean showPlayerMarker = true;
		boolean showBoatLocations = true;
		boolean enableShortestPath = true;
		boolean useExternalShortestPathSettings = false;
		boolean fullscreenMap = false;

		@Override
		public boolean showClueScroll()
		{
			return showClueScroll;
		}

		@Override
		public boolean showPlayerMarker()
		{
			return showPlayerMarker;
		}

		@Override
		public boolean showBoatLocations()
		{
			return showBoatLocations;
		}

		@Override
		public boolean enableShortestPath()
		{
			return enableShortestPath;
		}

		@Override
		public boolean useExternalShortestPathSettings()
		{
			return useExternalShortestPathSettings;
		}

		@Override
		public boolean fullscreenMap()
		{
			return fullscreenMap;
		}
	}

	private static class TestShortestPathTracker extends ShortestPathTracker
	{
		WorldPoint targetPoint;
		boolean routingEnabled = true;

		TestShortestPathTracker()
		{
			super(null, null, null, null, null);
		}

		@Override
		public boolean hasTarget()
		{
			return targetPoint != null;
		}

		@Override
		public WorldPoint target()
		{
			return targetPoint;
		}

		@Override
		public boolean isRoutingEnabled()
		{
			return routingEnabled;
		}
	}

	private static class TestBoatTracker extends BoatTracker
	{
		List<PlayerBoat> ownedBoats = new ArrayList<>();

		TestBoatTracker()
		{
			super(null, null, null, null, null);
		}

		@Override
		public List<PlayerBoat> getOwnedBoats()
		{
			return ownedBoats;
		}
	}

	@Test
	public void destinationButtonSitsBesidePlayerButtonAndDoesNotOverlap()
	{
		final TestConfig config = new TestConfig();
		final MapCamera camera = new MapCamera();
		final WorldMapInput input = new WorldMapInput(camera, config, null, null);
		final MapLayout layout = new MapLayout(null, config);
		final TestShortestPathTracker pathTracker = new TestShortestPathTracker();
		pathTracker.targetPoint = new WorldPoint(3210, 3424, 0);

		final MapChromeRenderer renderer = new MapChromeRenderer(
			config, camera, input, layout,
			new ClueScrollTracker(null),
			new QuestHelperTracker(null, new WorldMapPointReader()),
			null, null, pathTracker, null, null
		);

		final BufferedImage img = new BufferedImage(1000, 800, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = img.createGraphics();
		final Rectangle bounds = new Rectangle(0, 0, 1000, 800);

		renderer.drawStatusChip(g, bounds);
		renderer.drawPlayerButton(g, bounds);
		renderer.drawDestinationButton(g, bounds);

		final Rectangle playerBtn = camera.getPlayerButton();
		final Rectangle destBtn = camera.getDestinationButton();

		assertNotNull("Player button must be present", playerBtn);
		assertNotNull("Destination button must be present when target is set", destBtn);
		assertEquals("Destination button Y must align with top chip row", bounds.y + 8, destBtn.y);
		assertEquals("Destination button height must match chip height", 20, destBtn.height);
		assertTrue("Destination button must sit to the right of Player button",
			destBtn.x >= playerBtn.x + playerBtn.width + 6);
	}

	@Test
	public void destinationButtonHiddenWhenNoTargetOrDisabled()
	{
		final TestConfig config = new TestConfig();
		final MapCamera camera = new MapCamera();
		final WorldMapInput input = new WorldMapInput(camera, config, null, null);
		final MapLayout layout = new MapLayout(null, config);
		final TestShortestPathTracker pathTracker = new TestShortestPathTracker();

		final MapChromeRenderer renderer = new MapChromeRenderer(
			config, camera, input, layout,
			new ClueScrollTracker(null),
			new QuestHelperTracker(null, new WorldMapPointReader()),
			null, null, pathTracker, null, null
		);

		final BufferedImage img = new BufferedImage(1000, 800, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = img.createGraphics();
		final Rectangle bounds = new Rectangle(0, 0, 1000, 800);

		// 1. No target set
		renderer.drawDestinationButton(g, bounds);
		assertNull("Destination button must be null when no target", camera.getDestinationButton());

		// 2. Target set, but routing disabled
		pathTracker.targetPoint = new WorldPoint(3210, 3424, 0);
		pathTracker.routingEnabled = false;
		renderer.drawDestinationButton(g, bounds);
		assertNull("Destination button must be null when routing is disabled", camera.getDestinationButton());

		// 3. Re-enable routing
		pathTracker.routingEnabled = true;
		renderer.drawDestinationButton(g, bounds);
		assertNotNull("Destination button must appear when routing is enabled with a target",
			camera.getDestinationButton());
	}

	@Test
	public void destinationButtonClickTriggersGoToDestination()
	{
		final TestConfig config = new TestConfig();
		final MapCamera camera = new MapCamera();
		final boolean[] goToDestCalled = new boolean[1];
		final WorldMapInput input = new WorldMapInput(camera, config, () -> new BetterMapPlugin()
		{
			@Override
			public void goToDestination()
			{
				goToDestCalled[0] = true;
			}
		}, null);
		final MapLayout layout = new MapLayout(null, config);
		final TestShortestPathTracker pathTracker = new TestShortestPathTracker();
		pathTracker.targetPoint = new WorldPoint(3210, 3424, 0);

		final MapChromeRenderer renderer = new MapChromeRenderer(
			config, camera, input, layout,
			new ClueScrollTracker(null),
			new QuestHelperTracker(null, new WorldMapPointReader()),
			null, null, pathTracker, null, null
		);

		final BufferedImage img = new BufferedImage(1000, 800, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = img.createGraphics();
		final Rectangle bounds = new Rectangle(0, 0, 1000, 800);

		camera.setActive(true);
		camera.setViewport(bounds);
		renderer.drawStatusChip(g, bounds);
		renderer.drawPlayerButton(g, bounds);
		renderer.drawDestinationButton(g, bounds);

		final Rectangle destBtn = camera.getDestinationButton();
		assertNotNull(destBtn);

		final Component source = new Component() {};
		final MouseEvent click = new MouseEvent(
			source, MouseEvent.MOUSE_PRESSED,
			System.currentTimeMillis(), MouseEvent.BUTTON1_DOWN_MASK,
			destBtn.x + 2, destBtn.y + 2, 1, false, MouseEvent.BUTTON1
		);
		input.mousePressed(click);
		assertTrue("Clicking destination button must trigger goToDestination", goToDestCalled[0]);
		assertTrue("Clicking destination button must consume event", click.isConsumed());
	}

	@Test
	public void playerBoatsButtonAndDropdownInteraction()
	{
		final TestConfig config = new TestConfig();
		final MapCamera camera = new MapCamera();
		final PlayerBoat[] selectedBoat = new PlayerBoat[1];
		final WorldMapInput input = new WorldMapInput(camera, config, () -> new BetterMapPlugin()
		{
			@Override
			public void goToBoat(PlayerBoat boat)
			{
				selectedBoat[0] = boat;
			}
		}, null);
		final MapLayout layout = new MapLayout(null, config);

		final TestBoatTracker boatTracker = new TestBoatTracker();
		final PlayerBoat boat1 = new PlayerBoat(1);
		boat1.setBoatName("Sea Breeze");
		boat1.setPort(SailingPort.PORT_SARIM);
		boat1.setBoatType(BoatType.SKIFF);
		boat1.setOwned(true);

		final PlayerBoat boat2 = new PlayerBoat(2);
		boat2.setBoatName("Wave Runner");
		boat2.setPort(SailingPort.CATHERBY);
		boat2.setBoatType(BoatType.SLOOP);
		boat2.setOwned(true);

		boatTracker.ownedBoats.add(boat1);
		boatTracker.ownedBoats.add(boat2);

		final MapChromeRenderer renderer = new MapChromeRenderer(
			config, camera, input, layout,
			new ClueScrollTracker(null),
			new QuestHelperTracker(null, new WorldMapPointReader()),
			null, null, null, boatTracker, null
		);

		final BufferedImage img = new BufferedImage(1000, 800, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = img.createGraphics();
		final Rectangle bounds = new Rectangle(0, 0, 1000, 800);

		camera.setActive(true);
		camera.setViewport(bounds);
		renderer.drawStatusChip(g, bounds);
		renderer.drawPlayerButton(g, bounds);
		renderer.drawBoatsButton(g, bounds);

		final Rectangle boatsBtn = camera.getBoatsButton();
		assertNotNull("Player boats button must be present when boats are owned", boatsBtn);
		assertEquals("Player boats button Y must align with top chip row", bounds.y + 8, boatsBtn.y);
		assertFalse("Boats dropdown must be initially closed", camera.isBoatsDropdownOpen());

		// 1. Click chip to open dropdown
		final Component source = new Component() {};
		final MouseEvent openClick = new MouseEvent(
			source, MouseEvent.MOUSE_PRESSED,
			System.currentTimeMillis(), MouseEvent.BUTTON1_DOWN_MASK,
			boatsBtn.x + 2, boatsBtn.y + 2, 1, false, MouseEvent.BUTTON1
		);
		input.mousePressed(openClick);
		assertTrue("Clicking chip must open dropdown", camera.isBoatsDropdownOpen());
		assertTrue("Clicking chip must consume event", openClick.isConsumed());

		// 2. Render dropdown while open
		renderer.drawBoatsButton(g, bounds);
		renderer.drawBoatsDropdown(g, bounds);

		final Rectangle dropdownBounds = camera.getBoatsDropdownBounds();
		assertNotNull("Dropdown bounds must be set when open", dropdownBounds);
		final List<MapCamera.BoatDropdownTarget> targets = camera.getBoatDropdownTargets();
		assertEquals("Dropdown must contain 2 targets for 2 boats", 2, targets.size());
		assertEquals("First boat target must match boat1", boat1, targets.get(0).getBoat());
		assertEquals("Second boat target must match boat2", boat2, targets.get(1).getBoat());

		// 3. Click the second boat in the dropdown
		final Rectangle row2 = targets.get(1).getBounds();
		final MouseEvent selectBoatClick = new MouseEvent(
			source, MouseEvent.MOUSE_PRESSED,
			System.currentTimeMillis(), MouseEvent.BUTTON1_DOWN_MASK,
			row2.x + 10, row2.y + 10, 1, false, MouseEvent.BUTTON1
		);
		input.mousePressed(selectBoatClick);
		assertEquals("Clicking boat row must call goToBoat with that boat", boat2, selectedBoat[0]);
		assertFalse("Clicking boat row must close dropdown", camera.isBoatsDropdownOpen());
		assertTrue("Clicking boat row must consume event", selectBoatClick.isConsumed());

		// 4. Click chip to open again, then click outside to close
		input.mousePressed(openClick);
		assertTrue("Re-opening dropdown must succeed", camera.isBoatsDropdownOpen());
		renderer.drawBoatsDropdown(g, bounds);

		final MouseEvent outsideClick = new MouseEvent(
			source, MouseEvent.MOUSE_PRESSED,
			System.currentTimeMillis(), MouseEvent.BUTTON1_DOWN_MASK,
			bounds.x + 500, bounds.y + 500, 1, false, MouseEvent.BUTTON1
		);
		input.mousePressed(outsideClick);
		assertFalse("Clicking outside dropdown must close dropdown", camera.isBoatsDropdownOpen());
	}

	@Test
	public void boatsButtonHiddenWhenNoOwnedBoatsOrConfigDisabled()
	{
		final TestConfig config = new TestConfig();
		final MapCamera camera = new MapCamera();
		final WorldMapInput input = new WorldMapInput(camera, config, null, null);
		final MapLayout layout = new MapLayout(null, config);
		final TestBoatTracker boatTracker = new TestBoatTracker();

		final MapChromeRenderer renderer = new MapChromeRenderer(
			config, camera, input, layout,
			new ClueScrollTracker(null),
			new QuestHelperTracker(null, new WorldMapPointReader()),
			null, null, null, boatTracker, null
		);

		final BufferedImage img = new BufferedImage(1000, 800, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = img.createGraphics();
		final Rectangle bounds = new Rectangle(0, 0, 1000, 800);

		// No boats
		renderer.drawBoatsButton(g, bounds);
		assertNull("Boats button must be null when no owned boats", camera.getBoatsButton());

		// With boat, but config disabled
		final PlayerBoat boat = new PlayerBoat(1);
		boat.setOwned(true);
		boatTracker.ownedBoats.add(boat);
		config.showBoatLocations = false;

		renderer.drawBoatsButton(g, bounds);
		assertNull("Boats button must be null when showBoatLocations is false", camera.getBoatsButton());
	}
}
