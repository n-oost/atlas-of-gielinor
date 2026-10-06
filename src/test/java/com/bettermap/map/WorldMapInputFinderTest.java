package com.bettermap.map;

import com.google.gson.Gson;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.Collections;
import net.runelite.api.coords.WorldPoint;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.bettermap.BetterMapConfig;
import com.bettermap.BetterMapPlugin;

public class WorldMapInputFinderTest
{
	@org.junit.BeforeClass
	public static void loadMapData()
	{
		com.bettermap.map.MapData.load();
		org.junit.Assert.assertTrue("Map catalogs must load before testing", com.bettermap.map.MapData.isReady());
	}

	private static final double DELTA = 0.0001;

	private MapCamera camera;
	private WorldMapInput input;
	private MapFinder finder;

	private WorldPoint capturedCenterPoint;
	private boolean capturedRoute;
	private boolean clientMenuOpen;
	private boolean dragToPan;
	private WorldPoint mockPlayerLocation;

	@Before
	public void setUp()
	{
		camera = new MapCamera();
		camera.setViewport(new Rectangle(0, 0, 800, 600));
		camera.centerOn(3222, 3218);
		camera.setActive(true);

		final BetterMapConfig config = new BetterMapConfig()
		{
			@Override
			public boolean dragToPan()
			{
				return dragToPan;
			}

			@Override
			public boolean fullscreenMap()
			{
				return true;
			}
		};

		final BetterMapPlugin plugin = new BetterMapPlugin()
		{
			@Override
			public boolean isClientMenuOpen()
			{
				return clientMenuOpen;
			}

			@Override
			public void centerMapOn(WorldPoint point)
			{
				capturedCenterPoint = point;
			}

			@Override
			public void routeTo(WorldPoint point)
			{
				capturedCenterPoint = point;
				capturedRoute = true;
			}

			@Override
			public WorldPoint getPlayerLocation()
			{
				return mockPlayerLocation;
			}
		};

		final MonsterIndex monsterIndex = new MonsterIndex(new Gson());
		monsterIndex.load();
		finder = new MapFinder(new PoiIndex(), monsterIndex);

		input = new WorldMapInput(camera, config, () -> plugin, finder);
		input.flyoutDwellMs = 0;
	}

	private static MouseEvent leftPressAt(int x, int y)
	{
		return new MouseEvent(new java.awt.Canvas(), MouseEvent.MOUSE_PRESSED,
			System.currentTimeMillis(), 0, x, y, 1, false, MouseEvent.BUTTON1);
	}

	private static MouseEvent doubleLeftPressAt(int x, int y)
	{
		return new MouseEvent(new java.awt.Canvas(), MouseEvent.MOUSE_PRESSED,
			System.currentTimeMillis(), 0, x, y, 2, false, MouseEvent.BUTTON1);
	}

	private static MouseEvent shiftLeftPressAt(int x, int y)
	{
		return new MouseEvent(new java.awt.Canvas(), MouseEvent.MOUSE_PRESSED,
			System.currentTimeMillis(), MouseEvent.SHIFT_DOWN_MASK, x, y, 1, false, MouseEvent.BUTTON1);
	}

	private static MouseEvent rightPressAt(int x, int y)
	{
		return new MouseEvent(new java.awt.Canvas(), MouseEvent.MOUSE_PRESSED,
			System.currentTimeMillis(), 0, x, y, 1, false, MouseEvent.BUTTON3);
	}

	private static MouseEvent mouseMoveAt(int x, int y)
	{
		return new MouseEvent(new java.awt.Canvas(), MouseEvent.MOUSE_MOVED,
			System.currentTimeMillis(), 0, x, y, 0, false);
	}

	private static MouseWheelEvent mouseWheelAt(int x, int y, int scrollAmount)
	{
		return new MouseWheelEvent(new java.awt.Canvas(), MouseEvent.MOUSE_WHEEL,
			System.currentTimeMillis(), 0, x, y, 0, false,
			MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, scrollAmount);
	}

	private static KeyEvent keyEvent(int keyCode, char keyChar)
	{
		return new KeyEvent(new java.awt.Canvas(), KeyEvent.KEY_PRESSED,
			System.currentTimeMillis(), 0, keyCode, keyChar);
	}

	private static MapFinder.Result groupResult(WorldPoint point, String name)
	{
		final MapFinder.Result first = new MapFinder.Result(name, point, 0);
		final MapFinder.Result second = new MapFinder.Result(name,
			new WorldPoint(point.getX() + 50, point.getY() + 50, point.getPlane()), 70);
		return new MapFinder.Result(name, name, point, 0, 0, MapFinder.Result.Kind.PLACE,
			java.util.Arrays.asList(first, second));
	}

	@Test
	public void magnifierClickTogglesFinderPanelAndClosesLayersPanel()
	{
		camera.setFinderButton(new Rectangle(670, 8, 22, 22));
		camera.setLayersPanelOpen(true);
		assertFalse(camera.isFinderPanelOpen());

		final MouseEvent openEvent = leftPressAt(680, 18);
		input.mousePressed(openEvent);

		assertTrue(camera.isFinderPanelOpen());
		assertFalse(camera.isLayersPanelOpen());
		assertTrue(openEvent.isConsumed());

		final MouseEvent closeEvent = leftPressAt(680, 18);
		input.mousePressed(closeEvent);

		assertFalse(camera.isFinderPanelOpen());
		assertTrue(closeEvent.isConsumed());
	}

	@Test
	public void clickOnRowBodyCentersWithoutClosingTheMap()
	{
		camera.setFinderPanelOpen(true);
		final WorldPoint point = new WorldPoint(3200, 3200, 0);
		camera.setFinderResultTargets(Collections.singletonList(
			new MapCamera.FinderResultTarget(new Rectangle(100, 100, 200, 20), null, point, "Lumbridge")));

		final MouseEvent event = leftPressAt(150, 110);
		input.mousePressed(event);

		assertEquals(point, capturedCenterPoint);
		assertTrue("Finder actions leave map input active", camera.isActive());
		assertTrue(event.isConsumed());
	}

	@Test
	public void doubleClickOnRowBodyRequestsRouteWithoutClosingMap()
	{
		camera.setFinderPanelOpen(true);
		final WorldPoint point = new WorldPoint(3200, 3200, 0);
		camera.setFinderResultTargets(Collections.singletonList(
			new MapCamera.FinderResultTarget(new Rectangle(100, 100, 200, 20), null, point, "Lumbridge")));

		final MouseEvent second = doubleLeftPressAt(150, 110);
		input.mousePressed(second);

		assertTrue(second.isConsumed());
		assertEquals(point, capturedCenterPoint);
		assertTrue("double click requests a route", capturedRoute);
		assertTrue("Finder actions leave map input active", camera.isActive());
	}

	@Test
	public void clickOnAGroupHeaderActivatesItsSelectedChild()
	{
		camera.setFinderPanelOpen(true);
		final MapRegion region = MapRegion.VARROCK;
		final WorldPoint regCenter = new WorldPoint(region.getCenterX(), region.getCenterY(), 0);
		final MapFinder.Result group = groupResult(regCenter, "Bank");
		camera.setFinderResultTargets(Collections.singletonList(
			new MapCamera.FinderResultTarget(new Rectangle(100, 100, 200, 20), null, regCenter, "Bank", group, null)));

		input.mousePressed(leftPressAt(150, 110));

		assertEquals(regCenter, capturedCenterPoint);
	}

	@Test
	public void shiftClickOnRowBodyCentersWithoutClosingMap()
	{
		camera.setFinderPanelOpen(true);
		final WorldPoint point = new WorldPoint(3200, 3200, 0);
		camera.setFinderResultTargets(Collections.singletonList(
			new MapCamera.FinderResultTarget(new Rectangle(100, 100, 200, 20), null, point, "Lumbridge")));

		final MouseEvent event = shiftLeftPressAt(150, 110);
		input.mousePressed(event);

		assertEquals(point, capturedCenterPoint);
		assertTrue("shift click leaves map input active", camera.isActive());
		assertTrue(event.isConsumed());
	}

	@Test
	public void doubleClickOnBareMapDoesNotConsume()
	{
		final MouseEvent event = doubleLeftPressAt(400, 300);
		input.mousePressed(event);

		assertFalse(event.isConsumed());
	}

	@Test
	public void doubleClickOnOverworldPrifddinasDoesNotConsume()
	{
		camera.centerOn(3264, 6080);
		camera.setZoom(1.0);
		camera.beginFrame();

		final MouseEvent event = doubleLeftPressAt(400, 300);
		input.mousePressed(event);

		assertFalse(event.isConsumed());
	}

	@Test
	public void singleClickOnBareMapDoesNotConsume()
	{
		final MouseEvent event = leftPressAt(400, 300);
		input.mousePressed(event);

		assertFalse(event.isConsumed());
	}

	@Test
	public void openClientMenuReceivesSelectionClick()
	{
		clientMenuOpen = true;
		camera.setFinderPanelOpen(true);
		camera.setFinderResultTargets(Collections.singletonList(
			new MapCamera.FinderResultTarget(new Rectangle(100, 100, 200, 20), null, new WorldPoint(3200, 3200, 0), "Lumbridge")));

		final MouseEvent event = leftPressAt(150, 110);
		input.mousePressed(event);

		assertFalse("the open context menu must receive its selection click", event.isConsumed());
	}

	@Test
	public void keyPressedIsInertWhileTheSearchFieldIsFocused()
	{
		camera.setFinderPanelOpen(true);
		camera.setFinderFieldFocused(true);

		final KeyEvent a = keyEvent(KeyEvent.VK_A, 'a');
		input.keyPressed(a);
		assertFalse("keys must not consume in keyPressed - the chatbox owns them", a.isConsumed());
		assertEquals("", finder.getQuery());

		final KeyEvent enter = keyEvent(KeyEvent.VK_ENTER, '\n');
		input.keyPressed(enter);
		assertFalse(enter.isConsumed());
	}

	@Test
	public void clickOnTravelChipCentersOnNearestTravelNode()
	{
		camera.setFinderPanelOpen(true);
		camera.setFinderChipTravel(new Rectangle(200, 80, 60, 18));
		mockPlayerLocation = new WorldPoint(3222, 3218, 0); // Lumbridge

		final MouseEvent event = leftPressAt(210, 85);
		input.mousePressed(event);

		assertTrue(event.isConsumed());
		assertTrue(capturedCenterPoint != null);
	}

	@Test
	public void clickOnLastChipRestoresLastQuery()
	{
		camera.setFinderPanelOpen(true);
		camera.setFinderChipLast(new Rectangle(270, 80, 60, 18));
		finder.updateQuery("Varrock", null);
		assertEquals("Varrock", finder.getLastQuery());
		finder.clearQuery();
		assertEquals("", finder.getQuery());

		final MouseEvent event = leftPressAt(280, 85);
		input.mousePressed(event);

		assertTrue(event.isConsumed());
		assertEquals("Varrock", finder.getQuery());
	}

	@Test
	public void singleClickOnFlyoutItemCentersMap()
	{
		camera.setFinderPanelOpen(true);
		final WorldPoint point = new WorldPoint(3211, 3450, 0);
		camera.setFlyoutTargets(Collections.singletonList(
			new MapCamera.FlyoutTarget(new Rectangle(100, 100, 200, 20), null, point, "Varrock West Bank")));

		final MouseEvent event = leftPressAt(150, 110);
		input.mousePressed(event);

		assertTrue(event.isConsumed());
		assertEquals(point, capturedCenterPoint);
		assertTrue("Finder actions leave map input active", camera.isActive());
	}

	@Test
	public void doubleClickOnFlyoutItemRequestsRouteWithoutClosingMap()
	{
		camera.setFinderPanelOpen(true);
		final WorldPoint point = new WorldPoint(3211, 3450, 0);
		camera.setFlyoutTargets(Collections.singletonList(
			new MapCamera.FlyoutTarget(new Rectangle(100, 100, 200, 20), null, point, "Varrock West Bank")));

		final MouseEvent event = doubleLeftPressAt(150, 110);
		input.mousePressed(event);

		assertTrue(event.isConsumed());
		assertEquals(point, capturedCenterPoint);
		assertTrue(capturedRoute);
		assertTrue("Finder actions leave map input active", camera.isActive());
	}

	@Test
	public void clickOnRegionRowCentersOnRegionCenter()
	{
		camera.setFinderPanelOpen(true);
		final MapRegion region = MapRegion.VARROCK;
		final WorldPoint regCenter = new WorldPoint(region.getCenterX(), region.getCenterY(), 0);
		camera.setFinderResultTargets(Collections.singletonList(
			new MapCamera.FinderResultTarget(new Rectangle(100, 100, 200, 20), null, regCenter, region.getDisplayName(), null, region)));

		final MouseEvent event = leftPressAt(150, 110);
		input.mousePressed(event);

		assertTrue(event.isConsumed());
		assertEquals(regCenter, capturedCenterPoint);
	}

	@Test
	public void mouseMoveOverExpandableRowUpdatesHoveredRowKeyAfterDwell()
	{
		camera.setFinderPanelOpen(true);
		camera.setFinderPanelBounds(new Rectangle(50, 50, 300, 300));
		final MapRegion region = MapRegion.VARROCK;
		final WorldPoint regCenter = new WorldPoint(region.getCenterX(), region.getCenterY(), 0);
		camera.setFinderResultTargets(Collections.singletonList(
			new MapCamera.FinderResultTarget(new Rectangle(60, 100, 280, 20), null, regCenter, region.getDisplayName(), null, region)));

		input.mouseMoved(mouseMoveAt(100, 110));
		assertEquals(region.name(), camera.getHoveredRowKey());

		// Move inside flyout -> hoveredRowKey remains sticky
		final Rectangle flyout = new Rectangle(360, 50, 240, 200);
		camera.setFinderFlyoutBounds(flyout);
		camera.setFinderFlyoutHitBounds(flyout);
		input.mouseMoved(mouseMoveAt(400, 100));
		assertEquals(region.name(), camera.getHoveredRowKey());

		// Move completely outside panel and flyout -> clears hoveredRowKey
		input.mouseMoved(mouseMoveAt(10, 10));
		assertNull(camera.getHoveredRowKey());
	}

	@Test
	public void flyoutDoesNotOpenBeforeDwellElapses()
	{
		input.flyoutDwellMs = MapCamera.FINDER_FLYOUT_DWELL_MS;
		camera.setFinderPanelOpen(true);
		camera.setFinderPanelBounds(new Rectangle(50, 50, 300, 300));
		final MapRegion region = MapRegion.VARROCK;
		final WorldPoint regCenter = new WorldPoint(region.getCenterX(), region.getCenterY(), 0);
		camera.setFinderResultTargets(Collections.singletonList(
			new MapCamera.FinderResultTarget(new Rectangle(100, 100, 200, 20), null, regCenter, region.getDisplayName(), null, region)));

		final MouseEvent moveOverRow = mouseMoveAt(150, 110);
		input.mouseMoved(moveOverRow);

		assertNull("hoveredRowKey must be null immediately on move", camera.getHoveredRowKey());
	}

	@Test
	public void wheelOverBodyScrollsBodyEvenWhenFlyoutIsOpen()
	{
		camera.setFinderPanelOpen(true);
		camera.setHoveredRowKey(MapRegion.VARROCK.name());
		camera.setFinderBodyViewport(new Rectangle(60, 120, 280, 200));
		camera.setFinderFlyoutViewport(new Rectangle(370, 120, 250, 240));
		camera.setFinderBodyItemCount(20);

		final MouseWheelEvent event = mouseWheelAt(100, 150, 1);
		input.mouseWheelMoved(event);

		assertEquals(1, camera.getFinderBodyScrollOffset());
		assertTrue(event.isConsumed());
	}

	@Test
	public void wheelOverFlyoutScrollsFlyoutNotBody()
	{
		camera.setFinderPanelOpen(true);
		camera.setHoveredRowKey(MapRegion.VARROCK.name());
		camera.setFinderBodyViewport(new Rectangle(60, 120, 280, 200));
		camera.setFinderFlyoutViewport(new Rectangle(370, 120, 250, 240));
		camera.setFinderFlyoutItemCount(20);

		final MouseWheelEvent event = mouseWheelAt(400, 150, 1);
		input.mouseWheelMoved(event);

		assertEquals(1, camera.getFinderFlyoutScrollOffset());
		assertTrue(event.isConsumed());
	}

	@Test
	public void centreOnPlayerWorksAgainOnceTheFieldIsUnfocused()
	{
		camera.setFinderPanelOpen(true);
		camera.setFinderFieldFocused(true);

		final KeyEvent focusLostSpace = keyEvent(KeyEvent.VK_SPACE, ' ');
		input.keyPressed(focusLostSpace);
		assertFalse(focusLostSpace.isConsumed());

		camera.setFinderFieldFocused(false);

		mockPlayerLocation = new WorldPoint(3222, 3218, 0);
		final KeyEvent unfocusedSpace = keyEvent(KeyEvent.VK_SPACE, ' ');
		input.keyPressed(unfocusedSpace);
		assertTrue(unfocusedSpace.isConsumed());
		assertEquals(3222, camera.getCenterX(), DELTA);
		assertEquals(3218, camera.getCenterY(), DELTA);
	}

	@Test
	public void focusLostReleasesTheGrabAndAnyDrag()
	{
		camera.setFinderPanelOpen(true);
		assertTrue(camera.isFinderFieldFocused());

		input.focusLost();

		assertFalse(camera.isFinderFieldFocused());
		assertNull(input.getCursor());
	}
}
