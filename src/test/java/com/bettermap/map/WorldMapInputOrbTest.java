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
import com.bettermap.BetterMapPlugin;
import com.google.gson.Gson;
import java.awt.Canvas;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import net.runelite.api.coords.WorldPoint;
import org.junit.Before;
import org.junit.Test;

public class WorldMapInputOrbTest
{
	@org.junit.BeforeClass
	public static void loadMapData()
	{
		com.bettermap.map.MapData.load();
		org.junit.Assert.assertTrue("Map catalogs must load before testing", com.bettermap.map.MapData.isReady());
	}

	private final Canvas source = new Canvas();
	private final Rectangle orb = new Rectangle(700, 120, 26, 26);

	private MapCamera camera;
	private WorldMapInput input;
	private boolean showFinderOrb;

	@Before
	public void setUp()
	{
		camera = new MapCamera();
		showFinderOrb = true;

		final BetterMapConfig config = new BetterMapConfig()
		{
			@Override
			public boolean showFinderOrb()
			{
				return showFinderOrb;
			}
		};

		final BetterMapPlugin plugin = new BetterMapPlugin()
		{
			@Override
			public WorldPoint getPlayerLocation()
			{
				return new WorldPoint(3200, 3200, 0);
			}
		};

		final MonsterIndex monsterIndex = new MonsterIndex(new Gson());
		monsterIndex.load();
		final MapFinder finder = new MapFinder(new PoiIndex(), monsterIndex);
		input = new WorldMapInput(camera, config, () -> plugin, finder);
	}

	private MouseEvent leftPressAt(int x, int y)
	{
		return new MouseEvent(source, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(),
			MouseEvent.BUTTON1_DOWN_MASK, x, y, 1, false, MouseEvent.BUTTON1);
	}

	private MouseEvent leftReleaseAt(int x, int y)
	{
		return new MouseEvent(source, MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(),
			0, x, y, 1, false, MouseEvent.BUTTON1);
	}

	/** A press with no drag toggles on release now (press only arms the drag). */
	private void orbClick(int x, int y)
	{
		input.mousePressed(leftPressAt(x, y));
		input.mouseReleased(leftReleaseAt(x, y));
	}

	@Test
	public void orbClickTogglesTheStandaloneFinder()
	{
		camera.setQuickFinderOrb(orb);

		final MouseEvent open = leftPressAt(710, 130);
		input.mousePressed(open);
		assertTrue("press over the orb is consumed", open.isConsumed());
		input.mouseReleased(leftReleaseAt(710, 130));
		assertTrue(camera.isFinderStandalone());
		assertTrue(camera.isFinderPanelOpen());

		orbClick(710, 130);
		assertFalse(camera.isFinderStandalone());
		assertFalse(camera.isFinderPanelOpen());
	}

	@Test
	public void draggingTheOrbMovesItWithoutTogglingTheCard()
	{
		camera.setQuickFinderOrb(orb);

		input.mousePressed(leftPressAt(710, 130));
		input.mouseDragged(new MouseEvent(source, MouseEvent.MOUSE_DRAGGED, System.currentTimeMillis(),
			MouseEvent.BUTTON1_DOWN_MASK, 750, 200, 1, false));
		input.mouseReleased(leftReleaseAt(750, 200));

		assertFalse("a drag must not open the card", camera.isFinderPanelOpen());
		assertEquals(40, camera.getFinderOrbOffsetX());
		assertEquals(70, camera.getFinderOrbOffsetY());
	}

	@Test
	public void orbIsInertWhileTheWorldMapIsOpen()
	{
		camera.setViewport(new Rectangle(0, 0, 800, 600));
		camera.setActive(true);
		camera.setQuickFinderOrb(orb);

		input.mousePressed(leftPressAt(710, 130));

		assertFalse(camera.isFinderStandalone());
		assertFalse(camera.isFinderPanelOpen());
	}

	@Test
	public void orbClickIgnoredWhenDisabledInConfig()
	{
		showFinderOrb = false;
		camera.setQuickFinderOrb(orb);

		input.mousePressed(leftPressAt(710, 130));

		assertFalse(camera.isFinderStandalone());
		assertFalse(camera.isFinderPanelOpen());
	}

	@Test
	public void pressOutsideTheOrbDoesNotOpenIt()
	{
		camera.setQuickFinderOrb(orb);

		input.mousePressed(leftPressAt(100, 400));

		assertFalse(camera.isFinderStandalone());
		assertFalse(camera.isFinderPanelOpen());
	}
}
