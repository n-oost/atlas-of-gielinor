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

import com.google.gson.Gson;
import com.bettermap.BetterMapConfig;
import com.bettermap.BetterMapPlugin;
import com.bettermap.ui.input.PanButton;
import com.bettermap.data.OverlayCluster;
import com.bettermap.data.UndergroundZone;
import java.awt.Canvas;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.Collections;
import net.runelite.api.coords.WorldPoint;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

public class WorldMapInputDungeonExitTest
{
	@org.junit.BeforeClass
	public static void loadMapData()
	{
		com.bettermap.map.MapData.load();
		org.junit.Assert.assertTrue("Map catalogs must load before testing", com.bettermap.map.MapData.isReady());
	}

	private static final double DELTA = 1e-6;
	private final Canvas source = new Canvas();

	private MapCamera camera;
	private WorldMapInput input;
	private MapFinder finder;
	private PanButton configuredPanButton = PanButton.LEFT;

	@Before
	public void setUp()
	{
		camera = new MapCamera();
		camera.setViewport(new Rectangle(0, 0, 800, 600));
		camera.setActive(true);

		final BetterMapConfig config = new BetterMapConfig()
		{
			@Override
			public boolean dragToPan()
			{
				return true;
			}

			@Override
			public PanButton panButton()
			{
				return configuredPanButton;
			}

		};

		final BetterMapPlugin plugin = new BetterMapPlugin();
		finder = new MapFinder(new PoiIndex(), new MonsterIndex(new Gson()));
		input = new WorldMapInput(camera, config, () -> plugin, finder);
	}

	private MouseEvent rightPressAt(int x, int y)
	{
		return new MouseEvent(source, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(),
			MouseEvent.BUTTON3_DOWN_MASK, x, y, 1, false, MouseEvent.BUTTON3);
	}

	private MouseEvent leftPressAt(int x, int y)
	{
		return new MouseEvent(source, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(),
			MouseEvent.BUTTON1_DOWN_MASK, x, y, 1, false, MouseEvent.BUTTON1);
	}

	@Test
	public void rightClickInDungeonLeavesTheContextMenuAvailable()
	{
		camera.centerOn(UndergroundZone.TAVERLEY_DUNGEON.getSurfacePoint().getX(),
			UndergroundZone.TAVERLEY_DUNGEON.getSurfacePoint().getY());
		camera.setZoom(1.0);

		// Click surface layer symbol to enter Taverley Dungeon
		camera.setLayerSymbolTargets(Collections.singletonList(
			new MapCamera.LayerSymbolTarget(new Rectangle(400, 300, 24, 24), UndergroundZone.TAVERLEY_DUNGEON, true)));

		final MouseEvent enterEvent = leftPressAt(405, 305);
		input.mousePressed(enterEvent);

		assertTrue("enter click must be consumed", enterEvent.isConsumed());
		assertTrue("underground mode must be active", camera.isUndergroundModeActive());
		assertEquals("camera plane must match dungeon", UndergroundZone.TAVERLEY_DUNGEON.getUndergroundPoint().getPlane(), camera.getPlane());
		assertTrue("camera zoom must zoom in to frame dungeon larger", camera.getZoom() > 1.0);

		// Simulate player adjusting zoom further while exploring
		camera.setZoom(5.0);

		// Right click while viewing dungeon layer. RuneLite must receive it so Better Map can add
		// the explicit "Return to surface" menu entry.
		final MouseEvent rightClick = rightPressAt(450, 350);
		input.mousePressed(rightClick);

		assertFalse("right click must reach RuneLite's context menu", rightClick.isConsumed());
		assertTrue("the menu action owns the dungeon exit", camera.isUndergroundModeActive());
	}

	@Test
	public void rightClickWhenViewingRawUndergroundCoordinatesLeavesTheContextMenuAvailable()
	{
		// Camera looking directly at underground coordinates without explicit activeUndergroundZone
		camera.centerOn(2884, 9798);
		camera.setZoom(1.5);
		assertFalse(camera.isUndergroundModeActive());
		assertTrue(camera.isViewingDungeonLayer());

		final MouseEvent rightClick = rightPressAt(400, 300);
		input.mousePressed(rightClick);

		assertFalse(rightClick.isConsumed());
		assertEquals(2884, camera.getCenterX(), DELTA);
		assertEquals(9798, camera.getCenterY(), DELTA);
	}

	@Test
	public void rightClickWhenPlayerIsInKnownDungeonLeavesTheContextMenuAvailable()
	{
		camera.centerOn(3222, 3218);
		camera.setZoom(2.0);
		camera.setPlayerLocation(UndergroundZone.TAVERLEY_DUNGEON.getUndergroundPoint());
		assertFalse(camera.isViewingDungeonLayer());

		final MouseEvent rightClick = rightPressAt(400, 300);
		input.mousePressed(rightClick);

		assertFalse("right click must reach the Open underground menu", rightClick.isConsumed());
		assertEquals(3222, camera.getCenterX(), DELTA);
		assertEquals(3218, camera.getCenterY(), DELTA);
	}

	@Test
	public void rightClickInUnmappedInstanceLeavesTheContextMenuAvailable()
	{
		// An instance whose resolved coords land in no wiki-map box and near no UndergroundZone has
		// no entrance pin to snap to, so exit lands on wherever the player last stood on the
		// surface — the cave mouth they walked in through.
		final WorldPoint caveMouth = new WorldPoint(2770, 2703, 0);
		camera.setLastSurfaceLocation(caveMouth);
		camera.centerOn(3950, 9000);
		camera.setZoom(2.0);
		assertFalse(camera.isUndergroundModeActive());
		assertTrue(camera.isViewingDungeonLayer());

		final MouseEvent rightClick = rightPressAt(400, 300);
		input.mousePressed(rightClick);

		assertFalse(rightClick.isConsumed());
		assertEquals(3950, camera.getCenterX(), DELTA);
		assertEquals(9000, camera.getCenterY(), DELTA);
	}

	@Test
	public void rightClickPanDoesNotExitDungeon()
	{
		configuredPanButton = PanButton.RIGHT;
		camera.setUndergroundMode(UndergroundZone.TAVERLEY_DUNGEON);
		assertTrue(camera.isUndergroundModeActive());

		final MouseEvent rightClick = rightPressAt(400, 300);
		input.mousePressed(rightClick);

		assertTrue("right click pan must be consumed for pan drag", rightClick.isConsumed());
		assertTrue("underground mode must remain active when right click is pan button", camera.isUndergroundModeActive());
	}

	@Test
	public void rightClickOnOverworldLeavesContextMenuAvailable()
	{
		camera.centerOn(3222, 3218);
		camera.setZoom(2.0);
		assertFalse(camera.isViewingDungeonLayer());

		final MouseEvent rightClick = rightPressAt(400, 300);
		input.mousePressed(rightClick);

		assertFalse("right click on overworld must not be consumed so context menu can open", rightClick.isConsumed());
	}

	@Test
	public void returnButtonAndUpwardSymbolAlsoRestoreZoomAndCenterOnSurface()
	{
		camera.setZoom(1.2);
		camera.setUndergroundMode(UndergroundZone.LUMBRIDGE_CELLAR);
		assertTrue(camera.getZoom() > 1.2);

		// Click return button
		camera.setUndergroundReturnButton(new Rectangle(200, 10, 150, 20));
		final MouseEvent clickReturn = leftPressAt(220, 15);
		input.mousePressed(clickReturn);

		assertTrue(clickReturn.isConsumed());
		assertFalse(camera.isUndergroundModeActive());
		assertEquals(UndergroundZone.LUMBRIDGE_CELLAR.getSurfacePoint().getX(), camera.getCenterX(), DELTA);
		assertEquals(UndergroundZone.LUMBRIDGE_CELLAR.getSurfacePoint().getY(), camera.getCenterY(), DELTA);
		assertEquals(1.2, camera.getZoom(), DELTA);
	}

	@Test
	public void clickingRegionalCompositeAgainKeepsItsDungeonsOpen()
	{
		final OverlayCluster cluster = new OverlayCluster(
			"asgarnia", "Asgarnia underground", 2800, 3120, 3110, 3525,
			Collections.singletonList(UndergroundZone.TAVERLEY_DUNGEON), 2992, 3408);
		camera.setLayerSymbolTargets(Collections.singletonList(
			new MapCamera.LayerSymbolTarget(new Rectangle(390, 290, 20, 20), cluster)));

		input.mousePressed(leftPressAt(400, 300));
		assertEquals(cluster, camera.getActiveOverlayCluster());

		input.mousePressed(leftPressAt(400, 300));
		assertEquals(cluster, camera.getActiveOverlayCluster());
		assertTrue(camera.isUndergroundZoneOpen(UndergroundZone.TAVERLEY_DUNGEON));
	}

	@Test
	public void clickingDungeonReturnSymbolExitsCommittedLayer()
	{
		camera.setUndergroundMode(UndergroundZone.TAVERLEY_DUNGEON);
		camera.setLayerSymbolTargets(Collections.singletonList(
			new MapCamera.LayerSymbolTarget(
				new Rectangle(390, 290, 20, 20), UndergroundZone.TAVERLEY_DUNGEON, false)));

		input.mousePressed(leftPressAt(400, 300));

		assertFalse(camera.isUndergroundModeActive());
		assertEquals(UndergroundZone.TAVERLEY_DUNGEON.getSurfacePoint().getX(), camera.getCenterX(), DELTA);
		assertEquals(UndergroundZone.TAVERLEY_DUNGEON.getSurfacePoint().getY(), camera.getCenterY(), DELTA);
	}

}
