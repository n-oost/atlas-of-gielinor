package atlasofgielinor.ui;

import com.google.gson.Gson;

import atlasofgielinor.AtlasOfGielinorConfig;
import atlasofgielinor.AtlasOfGielinorPlugin;
import atlasofgielinor.map.MapCamera;
import atlasofgielinor.map.MapFinder;
import atlasofgielinor.map.catalog.MonsterIndex;
import atlasofgielinor.map.catalog.PoiIndex;
import atlasofgielinor.map.WorldMapInput;
import atlasofgielinor.ui.AtlasOfGielinorOverlay;
import atlasofgielinor.ui.input.MapLayerInputHandler;

import java.awt.Point;
import net.runelite.api.coords.WorldPoint;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
		atlasofgielinor.map.MapCatalogLoader.load();
		org.junit.Assert.assertTrue("Map catalogs must load before testing", atlasofgielinor.map.MapCatalogLoader.isReady());
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

		final AtlasOfGielinorConfig config = new AtlasOfGielinorConfig()
		{
			@Override
			public boolean showFinderButton()
			{
				return true;
			}
		};

		final WorldMapInput input = new WorldMapInput(camera, config, () -> null, finder);
		final AtlasOfGielinorOverlay overlay = new AtlasOfGielinorOverlay(
			null, config, null, camera, input, null, null, null, null, null, null, null, null, null, null, null,
			null, finder, null);

		final BufferedImage img = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g2d = img.createGraphics();
		final Rectangle bounds = new Rectangle(0, 0, 800, 600);

		// Directly invoke private drawFinder(Graphics2D, Rectangle)
		finder.clearQuery();
		final Method drawFinderMethod = AtlasOfGielinorOverlay.class.getDeclaredMethod("drawFinder", Graphics2D.class, Rectangle.class);
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

		final AtlasOfGielinorConfig config = new AtlasOfGielinorConfig()
		{
			@Override
			public boolean showFinderButton()
			{
				return true;
			}
		};

		final WorldMapInput input = new WorldMapInput(camera, config, () -> null, finder);
		final AtlasOfGielinorOverlay overlay = new AtlasOfGielinorOverlay(
			null, config, null, camera, input, null, null, null, null, null, null, null, null, null, null, null,
			null, finder, null);

		final BufferedImage img = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g2d = img.createGraphics();
		final Rectangle bounds = new Rectangle(0, 0, 800, 600);

		finder.clearQuery();
		final Method drawFinderMethod = AtlasOfGielinorOverlay.class.getDeclaredMethod("drawFinder", Graphics2D.class, Rectangle.class);
		drawFinderMethod.setAccessible(true);
		drawFinderMethod.invoke(overlay, g2d, bounds);

		assertNotNull("Flyout bounds must be published when row hovered", camera.getFinderFlyoutBounds());
		final List<MapCamera.FlyoutTarget> flyoutTargets = camera.getFlyoutTargets();
		assertFalse("Flyout targets must be published", flyoutTargets.isEmpty());
		g2d.dispose();
	}
}
