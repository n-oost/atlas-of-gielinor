package com.bettermap.ui;

import com.google.gson.Gson;
import com.bettermap.BetterMapConfig;
import com.bettermap.BetterMapPlugin;
import com.bettermap.ui.input.MapLayerInputHandler;
import java.awt.Point;
import net.runelite.api.coords.WorldPoint;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import com.bettermap.map.MapCamera;
import com.bettermap.map.MapFinder;
import com.bettermap.map.MonsterIndex;
import com.bettermap.map.PoiIndex;
import com.bettermap.map.WorldMapInput;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Method;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import org.junit.Before;
import org.junit.Test;

public class FinderPanelReworkTest
{
	@org.junit.BeforeClass
	public static void loadMapData()
	{
		com.bettermap.map.MapData.load();
		org.junit.Assert.assertTrue("Map catalogs must load before testing", com.bettermap.map.MapData.isReady());
	}

	private MapCamera camera;
	private MapFinder finder;

	@Before
	public void setUp()
	{
		camera = new MapCamera();
		camera.setActive(true);
		camera.setViewport(new Rectangle(0, 0, 800, 600));

		final PoiIndex poiIndex = new PoiIndex();
		poiIndex.load(new File("nonexistent_dir"));
		final MonsterIndex monsterIndex = new MonsterIndex(new Gson());
		monsterIndex.load();
		finder = new MapFinder(poiIndex, monsterIndex);
	}

	@Test
	public void browseModePublishesRegionsAndQuickFindChips() throws Exception
	{
		camera.setFinderPanelOpen(true);
		camera.setFinderButton(new Rectangle(700, 8, 22, 22));

		final BetterMapConfig config = new BetterMapConfig()
		{
			@Override
			public boolean showFinderButton()
			{
				return true;
			}
		};

		final WorldMapInput input = new WorldMapInput(camera, config, () -> null, finder);
		final BetterWorldMapOverlay overlay = new BetterWorldMapOverlay(
			null, config, null, camera, input, null, null, null, null, null, null, null, null, null, null, null,
			null, finder, null);

		final BufferedImage img = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g2d = img.createGraphics();
		final Rectangle bounds = new Rectangle(0, 0, 800, 600);

		// Directly invoke private drawFinder(Graphics2D, Rectangle)
		finder.clearQuery();
		final Method drawFinderMethod = BetterWorldMapOverlay.class.getDeclaredMethod("drawFinder", Graphics2D.class, Rectangle.class);
		drawFinderMethod.setAccessible(true);
		drawFinderMethod.invoke(overlay, g2d, bounds);

		assertNotNull("Finder panel bounds must be published", camera.getFinderPanelBounds());
		assertEquals("Panel width must be 300", 300, camera.getFinderPanelBounds().width);
		assertNotNull("Bank chip must be published", camera.getFinderChipBank());
		assertNotNull("Slayer chip must be published", camera.getFinderChipSlayer());
		assertNotNull("Travel chip must be published", camera.getFinderChipTravel());
		assertNotNull("Last chip must be published", camera.getFinderChipLast());

		final List<MapCamera.FinderResultTarget> targets = camera.getFinderResultTargets();
		assertFalse("Region browse targets must not be empty", targets.isEmpty());
		assertNotNull("Target must have region associated", targets.get(0).getRegion());
		g2d.dispose();
	}

	@Test
	public void flyoutSubmenuPublishesTargetsWhenRowIsHovered() throws Exception
	{
		camera.setFinderPanelOpen(true);
		camera.setFinderButton(new Rectangle(700, 8, 22, 22));
		camera.setHoveredRowKey("VARROCK");

		final BetterMapConfig config = new BetterMapConfig()
		{
			@Override
			public boolean showFinderButton()
			{
				return true;
			}
		};

		final WorldMapInput input = new WorldMapInput(camera, config, () -> null, finder);
		final BetterWorldMapOverlay overlay = new BetterWorldMapOverlay(
			null, config, null, camera, input, null, null, null, null, null, null, null, null, null, null, null,
			null, finder, null);

		final BufferedImage img = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g2d = img.createGraphics();
		final Rectangle bounds = new Rectangle(0, 0, 800, 600);

		finder.clearQuery();
		final Method drawFinderMethod = BetterWorldMapOverlay.class.getDeclaredMethod("drawFinder", Graphics2D.class, Rectangle.class);
		drawFinderMethod.setAccessible(true);
		drawFinderMethod.invoke(overlay, g2d, bounds);

		assertNotNull("Flyout bounds must be published when row hovered", camera.getFinderFlyoutBounds());
		final List<MapCamera.FlyoutTarget> flyoutTargets = camera.getFlyoutTargets();
		assertFalse("Flyout targets must be published", flyoutTargets.isEmpty());
		g2d.dispose();
	}
	@Test
	public void quickFinderPublishesSeparateMapAndRouteActionsWithMapButtonDisabled()
	{
		camera.setActive(false);
		camera.setFinderStandalone(true);
		camera.setFinderPanelOpen(true);
		camera.setHoveredRowKey("VARROCK");
		final BetterMapConfig config = new BetterMapConfig()
		{
			@Override
			public boolean showFinderButton()
			{
				return false;
			}

			@Override
			public boolean showFinderOrb()
			{
				return true;
			}
		};
		final WorldMapInput input = new WorldMapInput(camera, config, () -> null, finder);
		final MapFinderRenderer renderer = new MapFinderRenderer(null, config, camera, input, null, null, finder);
		final Graphics2D graphics = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB).createGraphics();
		try
		{
			renderer.drawFinder(graphics, new Rectangle(0, 0, 800, 600), new Rectangle(700, 8, 22, 22), false);
			assertTrue(camera.isFinderPanelOpen());
			assertFalse(camera.getFinderResultTargets().isEmpty());
			final MapCamera.FinderResultTarget row = camera.getFinderResultTargets().get(0);
			assertNotNull(row.getMapBounds());
			assertFalse(row.getMapBounds().intersects(row.getWalkBounds()));
			assertTrue(row.getRowBounds().contains(row.getMapBounds()));
			assertFalse(camera.getFlyoutTargets().isEmpty());
			final MapCamera.FlyoutTarget child = camera.getFlyoutTargets().get(0);
			assertNotNull(child.getMapBounds());
			assertFalse(child.getMapBounds().intersects(child.getWalkBounds()));
			assertEquals(30, child.getRowBounds().height);

			final WorldPoint[] opened = {null};
			final WorldPoint[] routed = {null};
			final BetterMapPlugin plugin = new BetterMapPlugin()
			{
				@Override
				public void openMapAt(WorldPoint point)
				{
					opened[0] = point;
				}

				@Override
				public void routeTo(WorldPoint point, boolean openMap)
				{
					assertTrue(openMap);
					routed[0] = point;
				}
			};
			final MapLayerInputHandler handler = new MapLayerInputHandler(camera, () -> plugin, finder, null, null, null);
			assertTrue(handler.clickedFinderResult(new Point(row.getMapBounds().x + 1, row.getMapBounds().y + 1), false, 1));
			assertEquals(row.getPoint(), opened[0]);
			assertNull(routed[0]);
			assertTrue(handler.clickedFlyoutItem(new Point(child.getWalkBounds().x + 1, child.getWalkBounds().y + 1), false, 1));
			assertEquals(child.getPoint(), routed[0]);
		}
		finally
		{
			graphics.dispose();
		}
	}

}
