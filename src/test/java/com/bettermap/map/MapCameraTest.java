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
import java.util.Collections;
import net.runelite.api.coords.WorldPoint;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

public class MapCameraTest
{
	private static final double DELTA = 1e-6;
	private static final Rectangle VIEW = new Rectangle(0, 0, 800, 600);

	private MapCamera camera;

	@Before
	public void setUp()
	{
		camera = new MapCamera();
		camera.setViewport(VIEW);
		camera.centerOn(3222, 3218);
		camera.setZoom(2);
		camera.beginFrame();
	}

	@Test
	public void screenAndWorldRoundTrip()
	{
		for (double worldX = 2000; worldX < 3800; worldX += 137)
		{
			assertEquals(worldX, camera.worldX(camera.screenX(worldX, VIEW), VIEW), DELTA);
		}

		for (double worldY = 2600; worldY < 12000; worldY += 537)
		{
			assertEquals(worldY, camera.worldY(camera.screenY(worldY, VIEW), VIEW), DELTA);
		}
	}

	@Test
	public void theCentreSitsInTheMiddleOfTheView()
	{
		assertEquals(VIEW.getCenterX(), camera.screenX(camera.getCenterX(), VIEW), DELTA);
		assertEquals(VIEW.getCenterY(), camera.screenY(camera.getCenterY(), VIEW), DELTA);
	}

	@Test
	public void northIsUpAndEastIsRight()
	{
		final double centreY = camera.screenY(camera.getCenterY(), VIEW);
		final double northY = camera.screenY(camera.getCenterY() + 50, VIEW);
		assertTrue("higher world y must draw further up the screen", northY < centreY);

		final double centreX = camera.screenX(camera.getCenterX(), VIEW);
		final double eastX = camera.screenX(camera.getCenterX() + 50, VIEW);
		assertTrue("higher world x must draw further right", eastX > centreX);
	}

	@Test
	public void draggingMovesTheMapWithTheCursor()
	{
		final double startX = camera.getCenterX();
		final double startY = camera.getCenterY();

		// Dragging right by 100px at 2px per tile pulls the map 50 tiles west.
		camera.panByScreen(100, 0);
		assertEquals(startX - 50, camera.getCenterX(), DELTA);

		// Grab-and-drag: dragging down drags the map down with the cursor, which brings what
		// was north of the view into it, so the centre's world y rises.
		camera.panByScreen(0, 100);
		assertEquals(startY + 50, camera.getCenterY(), DELTA);
	}

	@Test
	public void draggingNorthStopsAtTheTopOfTheOverworld()
	{
		// Far more drag than the world is tall: the empty band above it is not somewhere to land.
		camera.panByScreen(0, 100000);
		assertEquals(InstanceMaps.GAP_MIN_Y, camera.getCenterY(), DELTA);

		// Opening a dungeon layer keeps the camera on the surface entrance (the dungeon is
		// composited forward), and dragging from there is unleashed off the overworld cap.
		camera.centerOn(UndergroundZone.TAVERLEY_DUNGEON.getSurfacePoint().getX(),
			UndergroundZone.TAVERLEY_DUNGEON.getSurfacePoint().getY());
		camera.setUndergroundMode(UndergroundZone.TAVERLEY_DUNGEON);
		final double dungeonY = camera.getCenterY();
		camera.panByScreen(0, 100);
		assertEquals(dungeonY + 100 / camera.getZoom(), camera.getCenterY(), DELTA);
	}

	@Test
	public void openingADungeonFramesTheSurfaceEntranceAndCompositesForward()
	{
		// Clicking a dungeon symbol renders like the hover preview: the camera stays on the
		// surface entrance and the dungeon tiles are shifted onto it, rather than leaping to
		// raw +6400 dungeon coordinates.
		camera.centerOn(3400, 3000);
		camera.setUndergroundMode(UndergroundZone.MOLE_HOLE);

		final WorldPoint entrance = UndergroundZone.MOLE_HOLE.getSurfacePoint();
		assertEquals(entrance.getX(), camera.getCenterX(), DELTA);
		assertEquals(entrance.getY(), camera.getCenterY(), DELTA);
		assertEquals(UndergroundZone.MOLE_HOLE.getUndergroundPoint().getPlane(), camera.getPlane());
	}

	@Test
	public void fractionalZoomRoundTripIsSubPixelStable()
	{
		camera.centerOn(3222.375, 3218.625);
		camera.setZoom(3.14159);
		camera.beginFrame();

		for (double worldX = 3199.125; worldX < 3245; worldX += 0.375)
		{
			final double screenX = camera.screenX(worldX, VIEW);
			assertEquals(worldX, camera.worldX(screenX, VIEW), 1e-10);
		}
		for (double worldY = 3199.125; worldY < 3245; worldY += 0.375)
		{
			final double screenY = camera.screenY(worldY, VIEW);
			assertEquals(worldY, camera.worldY(screenY, VIEW), 1e-10);
		}
	}

	@Test
	public void playerEnteringAndLeavingMappedInteriorSwitchesLayersWhileMapIsOpen()
	{
		camera.setPlayerLocation(UndergroundZone.TAVERLEY_DUNGEON.getUndergroundPoint());
		camera.followPlayerInterior();

		assertSame(UndergroundZone.TAVERLEY_DUNGEON, camera.getActiveUndergroundZone());
		assertEquals(UndergroundZone.TAVERLEY_DUNGEON.getSurfacePoint().getX(), camera.getCenterX(), DELTA);

		camera.setPlayerLocation(new WorldPoint(2890, 3400, 0));
		camera.followPlayerInterior();

		assertFalse(camera.isUndergroundModeActive());
		assertEquals(UndergroundZone.TAVERLEY_DUNGEON.getSurfacePoint().getY(), camera.getCenterY(), DELTA);
	}

	@Test
	public void playerChangingFloorInsideSameZoneUpdatesTheAutomaticLayer()
	{
		final WorldPoint point = UndergroundZone.TAVERLEY_DUNGEON.getUndergroundPoint();
		camera.setPlayerLocation(point);
		camera.followPlayerInterior();

		camera.setPlayerLocation(new WorldPoint(point.getX(), point.getY(), 1));
		camera.followPlayerInterior();

		assertEquals(1, camera.getPlane());
		assertSame(UndergroundZone.TAVERLEY_DUNGEON, camera.getActiveUndergroundZone());
	}

	@Test
	public void manualSurfaceChoiceSticksUntilPlayerChangesArea()
	{
		camera.setPlayerLocation(UndergroundZone.TAVERLEY_DUNGEON.getUndergroundPoint());
		camera.followPlayerInterior();
		camera.exitDungeonToSurface();

		camera.followPlayerInterior();

		assertFalse(camera.isUndergroundModeActive());
		assertTrue(camera.isViewingPlayerInteriorFromSurface());
		assertSame(UndergroundZone.TAVERLEY_DUNGEON, camera.getObservedPlayerUndergroundZone());
	}

	@Test
	public void closingMapClearsAnAutomaticallyFollowedLayerForFreshDetectionOnReopen()
	{
		camera.setPlayerLocation(UndergroundZone.TAVERLEY_DUNGEON.getUndergroundPoint());
		camera.followPlayerInterior();

		camera.resetPlayerInteriorFollow();

		assertFalse(camera.isUndergroundModeActive());
		camera.followPlayerInterior();
		assertSame(UndergroundZone.TAVERLEY_DUNGEON, camera.getActiveUndergroundZone());
	}

	@Test
	public void unmappedInteriorUsesDimmedEntranceFallbackInsteadOfVoidCoordinates()
	{
		final WorldPoint entrance = new WorldPoint(2770, 2703, 0);
		camera.setLastSurfaceLocation(entrance);
		camera.setPlayerLocation(new WorldPoint(3950, 9000, 0));

		camera.followPlayerInterior();

		assertTrue(camera.isInteriorMapUnavailable());
		assertEquals(entrance.getX(), camera.getCenterX(), DELTA);
		assertEquals(entrance.getY(), camera.getCenterY(), DELTA);
	}

	@Test
	public void validInstancedOverworldProjectionDoesNotUseUnavailableFallback()
	{
		camera.setPlayerLocation(new WorldPoint(3264, 6080, 0));

		camera.followPlayerInterior();

		assertFalse(camera.isInteriorMapUnavailable());
		assertFalse(camera.isUndergroundModeActive());
	}

	@Test
	public void centreOnRemapsInstancedPrifddinasOntoTirannwn()
	{
		camera.centerOn(3264, 6080);
		assertEquals(2240, camera.getCenterX(), DELTA);
		assertEquals(3328, camera.getCenterY(), DELTA);
	}

	@Test
	public void threeArgScreenProjectsInstancePrifddinasOntoTheOverworldSlot()
	{
		camera.centerOn(2240, 3328);
		camera.beginFrame();

		assertEquals(camera.screenX(2240.5, VIEW), camera.screenX(3264.5, 6080.5, VIEW), DELTA);
		assertEquals(camera.screenY(3328.5, VIEW), camera.screenY(3264.5, 6080.5, VIEW), DELTA);
	}

	@Test
	public void threeArgScreenProjectsZilyanaOntoTrollheim()
	{
		camera.centerOn(3222, 3218);
		camera.beginFrame();

		assertEquals(camera.screenX(2943.5, VIEW), camera.screenX(2907.5, 5265.5, VIEW), DELTA);
		assertEquals(camera.screenY(3702.5, VIEW), camera.screenY(2907.5, 5265.5, VIEW), DELTA);
	}

	@Test
	public void zoomingKeepsThePointUnderTheCursorStill()
	{
		final int anchorX = 620;
		final int anchorY = 140;

		final double worldXBefore = camera.worldX(anchorX, VIEW);
		final double worldYBefore = camera.worldY(anchorY, VIEW);

		camera.zoomBy(1.7, anchorX, anchorY);
		// The invariant holds frame to frame, so the next frame must be snapshot before checking.
		camera.beginFrame();

		assertEquals(worldXBefore, camera.worldX(anchorX, VIEW), 1e-6);
		assertEquals(worldYBefore, camera.worldY(anchorY, VIEW), 1e-6);
	}

	@Test
	public void projectionIsFrozenBetweenFrames()
	{
		final double before = camera.screenX(3222, VIEW);

		// A drag on the AWT thread mid-frame must not move anything already being drawn.
		camera.panByScreen(120, 0);
		assertEquals("projection must not shift until the next frame is snapshot",
			before, camera.screenX(3222, VIEW), DELTA);

		camera.beginFrame();
		assertTrue("after beginFrame the new pose takes effect",
			camera.screenX(3222, VIEW) > before);
	}

	@Test
	public void zoomIsClamped()
	{
		camera.setZoom(1e9);
		assertEquals(MapCamera.MAX_ZOOM, camera.getZoom(), DELTA);

		camera.setZoom(1e-9);
		assertEquals(MapCamera.MIN_ZOOM, camera.getZoom(), DELTA);
		assertEquals(0.14, MapCamera.MIN_ZOOM, DELTA);
	}

	@Test
	public void fittingShowsTheWholeOverworldAndNoneOfTheBandAboveIt()
	{
		camera.fit(VIEW);

		final double width = (MapCamera.MAX_WORLD_X - MapCamera.MIN_WORLD_X) * camera.getZoom();
		final double height = (InstanceMaps.GAP_MIN_Y - MapCamera.MIN_WORLD_Y) * camera.getZoom();

		assertTrue("the whole world must fit across", width <= VIEW.width + 1);
		assertTrue("the whole world must fit down", height <= VIEW.height + 1);
		assertTrue("the world must not be squeezed into a corner by the empty band above it",
			height >= VIEW.height * 0.5);
		assertTrue("the centre stays on the overworld", InstanceMaps.cameraOnOverworld(camera.getCenterY()));
	}

	@Test
	public void theCentreCannotLeaveTheWorld()
	{
		camera.centerOn(-100000, -100000);
		assertEquals(MapCamera.MIN_WORLD_X, camera.getCenterX(), DELTA);
		assertEquals(MapCamera.MIN_WORLD_Y, camera.getCenterY(), DELTA);

		camera.centerOn(100000, 100000);
		assertEquals(MapCamera.MAX_WORLD_X, camera.getCenterX(), DELTA);
		assertEquals(MapCamera.MAX_WORLD_Y, camera.getCenterY(), DELTA);
	}

	@Test
	public void aFloorPickedByHandSurvivesThePlayerWalkingAround()
	{
		camera.setPlane(2);
		camera.setPlaneFromPlayer(0);
		assertEquals("a hand-picked floor must not be snapped back", 2, camera.getPlane());
	}

	@Test
	public void followingThePlayerWorksUntilTheUserPicksAFloor()
	{
		camera.setPlaneFromPlayer(1);
		assertEquals(1, camera.getPlane());
	}

	@Test
	public void layersPanelStateRoundTrips()
	{
		final Rectangle gear = new Rectangle(700, 8, 22, 22);
		final Rectangle panel = new Rectangle(540, 34, 170, 220);
		final MapCamera.LayerToggleTarget row =
			new MapCamera.LayerToggleTarget(new Rectangle(543, 60, 164, 18), "showTooltips", true);

		camera.setLayersButton(gear);
		camera.setLayersPanelOpen(true);
		camera.setLayerToggleTargets(Collections.singletonList(row));
		camera.setLayersPanelBounds(panel);

		assertSame(gear, camera.getLayersButton());
		assertTrue(camera.isLayersPanelOpen());
		assertEquals(1, camera.getLayerToggleTargets().size());
		assertEquals("showTooltips", camera.getLayerToggleTargets().get(0).getConfigKey());
		assertTrue(camera.getLayerToggleTargets().get(0).getCurrentValue());
		assertSame(panel, camera.getLayersPanelBounds());
	}

	@Test
	public void nullTogglesFallBackToAnEmptyList()
	{
		camera.setLayerToggleTargets(null);
		assertTrue(camera.getLayerToggleTargets().isEmpty());
	}

	@Test
	public void deactivatingClearsTheLayersPanelHitTargets()
	{
		camera.setActive(true);
		camera.setLayersButton(new Rectangle(700, 8, 22, 22));
		camera.setLayersPanelOpen(true);
		camera.setLayerToggleTargets(Collections.singletonList(
			new MapCamera.LayerToggleTarget(new Rectangle(543, 60, 164, 18), "showTooltips", true)));
		camera.setLayersPanelBounds(new Rectangle(540, 34, 170, 220));

		camera.setActive(false);

		assertNull("stale gear rect must not survive the map closing", camera.getLayersButton());
		assertNull(camera.getLayersPanelBounds());
		assertFalse(camera.isLayersPanelOpen());
		assertTrue(camera.getLayerToggleTargets().isEmpty());
	}

	@Test
	public void finderPanelStateRoundTrips()
	{
		final Rectangle button = new Rectangle(672, 8, 22, 22);
		final Rectangle panel = new Rectangle(434, 34, 260, 200);
		final Rectangle field = new Rectangle(444, 58, 240, 20);
		final MapCamera.FinderResultTarget row =
			new MapCamera.FinderResultTarget(new Rectangle(437, 82, 254, 20), new Rectangle(645, 85, 38, 14),
				new net.runelite.api.coords.WorldPoint(3200, 3200, 0), "Lumbridge");

		camera.setFinderButton(button);
		camera.setFinderPanelOpen(true);
		camera.setFinderPanelBounds(panel);
		camera.setFinderFieldBounds(field);
		camera.setFinderResultTargets(Collections.singletonList(row));

		assertSame(button, camera.getFinderButton());
		assertTrue(camera.isFinderPanelOpen());
		assertSame(panel, camera.getFinderPanelBounds());
		assertSame(field, camera.getFinderFieldBounds());
		assertEquals(1, camera.getFinderResultTargets().size());
		assertEquals("Lumbridge", camera.getFinderResultTargets().get(0).getName());
	}

	@Test
	public void nullFinderTargetsFallBackToAnEmptyList()
	{
		camera.setFinderResultTargets(null);
		assertTrue(camera.getFinderResultTargets().isEmpty());
	}

	@Test
	public void deactivatingClearsTheFinderPanelHitTargets()
	{
		camera.setActive(true);
		camera.setFinderButton(new Rectangle(672, 8, 22, 22));
		camera.setFinderPanelOpen(true);
		camera.setFinderPanelBounds(new Rectangle(434, 34, 260, 200));
		camera.setFinderFieldBounds(new Rectangle(444, 58, 240, 20));
		camera.setFinderResultTargets(Collections.singletonList(
			new MapCamera.FinderResultTarget(new Rectangle(437, 82, 254, 20), null,
				new net.runelite.api.coords.WorldPoint(3200, 3200, 0), "Lumbridge")));

		camera.setActive(false);

		assertNull(camera.getFinderButton());
		assertNull(camera.getFinderPanelBounds());
		assertNull(camera.getFinderFieldBounds());
		assertNull(camera.getFinderChipBank());
		assertNull(camera.getFinderChipSlayer());
		assertNull(camera.getFinderChipTravel());
		assertNull(camera.getFinderChipLast());
		assertNull(camera.getHoveredRowKey());
		assertNull(camera.getFinderFlyoutBounds());
		assertTrue(camera.getFlyoutTargets().isEmpty());
		assertFalse(camera.isFinderPanelOpen());
		assertTrue(camera.getFinderResultTargets().isEmpty());
	}

	@Test
	public void finderChipAndFlyoutStateRoundTrips()
	{
		final Rectangle bank = new Rectangle(10, 50, 60, 18);
		final Rectangle slayer = new Rectangle(74, 50, 60, 18);
		final Rectangle travel = new Rectangle(138, 50, 60, 18);
		final Rectangle last = new Rectangle(202, 50, 60, 18);
		final Rectangle flyoutBounds = new Rectangle(100, 100, 240, 200);
		final MapCamera.FlyoutTarget flyoutTarget = new MapCamera.FlyoutTarget(
			new Rectangle(103, 130, 234, 20), null, new WorldPoint(3211, 3450, 0), "Varrock West Bank");

		camera.setFinderChipBank(bank);
		camera.setFinderChipSlayer(slayer);
		camera.setFinderChipTravel(travel);
		camera.setFinderChipLast(last);
		camera.setHoveredRowKey("Varrock");
		camera.setFinderFlyoutBounds(flyoutBounds);
		camera.setFlyoutTargets(Collections.singletonList(flyoutTarget));

		assertSame(bank, camera.getFinderChipBank());
		assertSame(slayer, camera.getFinderChipSlayer());
		assertSame(travel, camera.getFinderChipTravel());
		assertSame(last, camera.getFinderChipLast());
		assertEquals("Varrock", camera.getHoveredRowKey());
		assertSame(flyoutBounds, camera.getFinderFlyoutBounds());
		assertEquals(1, camera.getFlyoutTargets().size());
		assertEquals("Varrock West Bank", camera.getFlyoutTargets().get(0).getName());
	}

	@Test
	public void closingFinderPanelClearsFlyoutAndHoveredRowKey()
	{
		camera.setFinderPanelOpen(true);
		camera.setHoveredRowKey("Varrock");
		camera.setFinderFlyoutBounds(new Rectangle(100, 100, 200, 100));
		camera.setFlyoutTargets(Collections.singletonList(
			new MapCamera.FlyoutTarget(new Rectangle(100, 100, 100, 20), null, new WorldPoint(3200, 3200, 0), "Target")));

		camera.setFinderPanelOpen(false);

		assertFalse(camera.isFinderPanelOpen());
		assertNull(camera.getHoveredRowKey());
		assertNull(camera.getFinderFlyoutBounds());
		assertTrue(camera.getFlyoutTargets().isEmpty());
	}

	@Test
	public void flashPointRoundTripsAndStampsTime()
	{
		final WorldPoint point = new WorldPoint(3222, 3218, 0);
		camera.flashAt(point);
		assertSame(point, camera.getFlashPoint());
		assertTrue("start stamp must be non-zero after flashAt", camera.getFlashStartMillis() > 0);
	}

	@Test
	public void clearFlashNullsPointAndStamp()
	{
		final WorldPoint point = new WorldPoint(3222, 3218, 0);
		camera.flashAt(point);
		camera.clearFlash();
		assertNull(camera.getFlashPoint());
		assertEquals(0L, camera.getFlashStartMillis());
	}

	@Test
	public void flashAtNullClearsFlash()
	{
		final WorldPoint point = new WorldPoint(3222, 3218, 0);
		camera.flashAt(point);
		camera.flashAt(null);
		assertNull(camera.getFlashPoint());
		assertEquals(0L, camera.getFlashStartMillis());
	}

	@Test
	public void deactivatingClearsFlash()
	{
		camera.setActive(true);
		camera.flashAt(new WorldPoint(3222, 3218, 0));
		camera.setActive(false);
		assertNull(camera.getFlashPoint());
		assertEquals(0L, camera.getFlashStartMillis());
	}
}
