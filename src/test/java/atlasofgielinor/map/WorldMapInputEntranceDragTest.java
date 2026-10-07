package atlasofgielinor.map;

import atlasofgielinor.AtlasOfGielinorConfig;
import atlasofgielinor.data.dungeons.DungeonFloor;
import atlasofgielinor.data.dungeons.UndergroundZone;
import atlasofgielinor.ui.input.PanButton;
import java.awt.Canvas;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class WorldMapInputEntranceDragTest
{
	private final Canvas canvas = new Canvas();
	private MapCamera camera;
	private WorldMapInput input;
	private PanButton panButton = PanButton.LEFT;
	private boolean dragToPan = true;

	@BeforeClass
	public static void loadMapData()
	{
		MapCatalogLoader.load();
		assertTrue(MapCatalogLoader.isReady());
	}

	@Before
	public void setUp()
	{
		camera = new MapCamera();
		camera.setViewport(new Rectangle(0, 0, 1476, 1000));
		camera.setActive(true);
		camera.centerOn(3060, 3192);
		camera.setZoom(0.14);
		camera.setLayerSymbolTargets(Arrays.asList(
			new MapCamera.LayerSymbolTarget(new Rectangle(694, 419, 14, 14), UndergroundZone.KELDAGRIM, true),
			new MapCamera.LayerSymbolTarget(new Rectangle(687, 417, 14, 14), UndergroundZone.BRINE_RAT_CAVERN, true)));
		input = new WorldMapInput(camera, new AtlasOfGielinorConfig()
		{
			@Override public boolean dragToPan() { return dragToPan; }
			@Override public PanButton panButton() { return panButton; }
		}, null, null);
	}

	private MouseEvent event(int type, int x, int y, int button)
	{
		return new MouseEvent(canvas, type, System.currentTimeMillis(), 0, x, y, 1, false, button);
	}

	private void press(int x, int y, int button)
	{
		assertTrue(input.mousePressed(event(MouseEvent.MOUSE_PRESSED, x, y, button)).isConsumed());
	}

	private void drag(int x, int y)
	{
		input.mouseDragged(event(MouseEvent.MOUSE_DRAGGED, x, y, MouseEvent.NOBUTTON));
	}

	private void release(int x, int y, int button)
	{
		input.mouseReleased(event(MouseEvent.MOUSE_RELEASED, x, y, button));
	}

	private void assertPanned()
	{
		assertFalse(camera.isUndergroundModeActive());
		assertEquals(0.14, camera.getZoom(), 1e-9);
		assertEquals(3060 - 50 / 0.14, camera.getCenterX(), 1e-9);
		assertEquals(3192 + 20 / 0.14, camera.getCenterY(), 1e-9);
		assertFalse(input.getDragController().isDraggingMap());
	}

	@Test public void originalOverlappingEntranceGesturePans()
	{
		press(700, 420, MouseEvent.BUTTON1);
		assertFalse(camera.isUndergroundModeActive());
		for (int step = 1; step <= 10; step++) drag(700 + step * 5, 420 + step * 2);
		release(750, 440, MouseEvent.BUTTON1);
		assertPanned();
	}

	@Test public void clickWithFivePixelJitterKeepsPressTargetAndCamera()
	{
		press(700, 420, MouseEvent.BUTTON1);
		drag(703, 424);
		assertEquals(3060, camera.getCenterX(), 0);
		assertEquals(3192, camera.getCenterY(), 0);
		// Rendering can publish new targets between press and release.
		camera.setLayerSymbolTargets(Collections.singletonList(
			new MapCamera.LayerSymbolTarget(new Rectangle(694, 419, 14, 14), UndergroundZone.BRINE_RAT_CAVERN, true)));
		release(703, 424, MouseEvent.BUTTON1);
		assertEquals(UndergroundZone.KELDAGRIM, camera.getActiveUndergroundZone());
	}

	@Test public void dragReturningToPressPointDoesNotBecomeClick()
	{
		press(700, 420, MouseEvent.BUTTON1);
		drag(750, 440);
		drag(700, 420);
		release(700, 420, MouseEvent.BUTTON1);
		assertFalse(camera.isUndergroundModeActive());
	}

	@Test public void backgroundDragIsUnchanged()
	{
		press(500, 350, MouseEvent.BUTTON1);
		drag(550, 370);
		release(550, 370, MouseEvent.BUTTON1);
		assertPanned();
	}

	@Test public void configuredRightAndMiddlePanButtonsPanOverEntrances()
	{
		for (PanButton configured : Arrays.asList(PanButton.RIGHT, PanButton.MIDDLE))
		{
			setUp();
			panButton = configured;
			int button = configured == PanButton.RIGHT ? MouseEvent.BUTTON3 : MouseEvent.BUTTON2;
			press(700, 420, button);
			drag(750, 440);
			release(750, 440, button);
			assertPanned();
		}
	}

	@Test public void leftClickStillActivatesWhenAnotherPanButtonIsConfigured()
	{
		for (PanButton configured : Arrays.asList(PanButton.RIGHT, PanButton.MIDDLE))
		{
			setUp();
			panButton = configured;
			press(700, 420, MouseEvent.BUTTON1);
			assertEquals(UndergroundZone.KELDAGRIM, camera.getActiveUndergroundZone());
		}
	}

	@Test public void disabledPanningKeepsPressActivation()
	{
		dragToPan = false;
		press(700, 420, MouseEvent.BUTTON1);
		assertEquals(UndergroundZone.KELDAGRIM, camera.getActiveUndergroundZone());
	}

	@Test public void focusLossAndInactiveMapCancelPendingClick()
	{
		press(700, 420, MouseEvent.BUTTON1);
		input.focusLost();
		release(700, 420, MouseEvent.BUTTON1);
		assertFalse(camera.isUndergroundModeActive());
		press(700, 420, MouseEvent.BUTTON1);
		camera.setActive(false);
		drag(701, 420);
		camera.setActive(true);
		release(700, 420, MouseEvent.BUTTON1);
		assertFalse(camera.isUndergroundModeActive());
	}

	@Test public void toolbarAndFloorControlsStillActOnPress()
	{
		camera.setChatButton(new Rectangle(694, 419, 14, 14));
		press(700, 420, MouseEvent.BUTTON1);
		assertTrue(camera.isChatHidden());
		assertFalse(camera.isUndergroundModeActive());
		camera.setChatButton(null);
		DungeonFloor floor = DungeonFloor.all().stream().filter(f -> f.zone == UndergroundZone.LUMBRIDGE_CELLAR).findFirst().get();
		camera.setLayerSymbolTargets(Collections.singletonList(new MapCamera.LayerSymbolTarget(new Rectangle(694, 419, 14, 14), floor)));
		press(700, 420, MouseEvent.BUTTON1);
		assertEquals(Integer.valueOf(floor.layerId), camera.floorLayerFor(floor.zone));
		assertFalse(input.getDragController().isDraggingMap());
	}

	@Test public void releaseOutsideTargetOrWithoutDragEventsDoesNotActivate()
	{
		press(700, 420, MouseEvent.BUTTON1);
		release(750, 440, MouseEvent.BUTTON1);
		assertFalse(camera.isUndergroundModeActive());
		press(700, 420, MouseEvent.BUTTON1);
		release(693, 420, MouseEvent.BUTTON1);
		assertFalse(camera.isUndergroundModeActive());
	}
}
