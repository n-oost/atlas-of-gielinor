package atlasofgielinor.ui;

import atlasofgielinor.AtlasOfGielinorConfig;
import atlasofgielinor.AtlasOfGielinorPlugin;
import atlasofgielinor.data.TravelData;
import atlasofgielinor.map.MapCamera;
import atlasofgielinor.map.MapCatalogLoader;
import atlasofgielinor.map.WorldMapInput;
import atlasofgielinor.map.WorldMapPointReader;
import atlasofgielinor.map.catalog.PoiIndex;
import java.awt.Canvas;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class HiddenTravelTooltipTest
{
	private static final Rectangle BOUNDS = new Rectangle(0, 0, 1476, 1000);
	private final Canvas canvas = new Canvas();
	private TestConfig config;
	private MapCamera camera;
	private WorldMapInput input;
	private MapMarkerRenderers markers;
	private Recorder tooltip;

	private static class TestConfig implements AtlasOfGielinorConfig
	{
		boolean travel;
		@Override public boolean showTravelRoutes() { return travel; }
		@Override public double travelStationMinZoom() { return 3.0; }
		@Override public boolean showBoatLocations() { return false; }
		@Override public boolean showSailingPorts() { return false; }
		@Override public boolean showPortNoticeBoards() { return false; }
		@Override public boolean showBossLocations() { return false; }
		@Override public boolean showGroundItems() { return false; }
		@Override public boolean showMonsterZones() { return false; }
		@Override public boolean iconBanks() { return false; }
		@Override public boolean iconShops() { return false; }
		@Override public boolean iconShortcuts() { return false; }
		@Override public boolean iconSailing() { return false; }
		@Override public boolean iconSkilling() { return false; }
		@Override public boolean iconTravel() { return false; }
		@Override public boolean iconQuests() { return false; }
		@Override public boolean iconAltars() { return false; }
		@Override public boolean iconDungeons() { return false; }
		@Override public boolean iconServices() { return false; }
		@Override public boolean iconOther() { return false; }
	}

	private static class Recorder extends MapTooltipRenderer
	{
		final List<String> titles = new ArrayList<>();
		Recorder(TestConfig config, MapCamera camera, WorldMapInput input, PoiIndex pois)
		{
			super(config, camera, input, pois, null, null, null, null, null, null, null, null, null, new WorldMapPointReader());
		}
		@Override public void drawCard(Graphics2D graphics, Rectangle bounds, Point cursor,
			String title, BufferedImage icon, List<String> lines)
		{
			titles.add(title);
		}
	}

	@BeforeClass public static void loadCatalogs()
	{
		MapCatalogLoader.load();
		assertTrue(MapCatalogLoader.isReady());
	}

	@Before public void setUp()
	{
		config = new TestConfig();
		camera = new MapCamera();
		camera.setViewport(BOUNDS);
		camera.setActive(true);
		camera.setZoom(3.0);
		AtlasOfGielinorPlugin plugin = new AtlasOfGielinorPlugin()
		{
			@Override public AtlasOfGielinorConfig getConfig() { return config; }
		};
		input = new WorldMapInput(camera, config, () -> plugin, null);
		PoiIndex pois = new PoiIndex();
		markers = new MapMarkerRenderers(null, config, camera, pois, null, null, null, null, null, null, null, null, new WorldMapPointReader(), null);
		tooltip = new Recorder(config, camera, input, pois);
	}

	private TravelData.TravelNode center(String name)
	{
		TravelData.TravelNode node = TravelData.ALL_NODES.stream().filter(n -> name.equals(n.getName())).findFirst().get();
		camera.centerOn(node.getLocation().getX(), node.getLocation().getY());
		return node;
	}

	private void hover(TravelData.TravelNode node)
	{
		int x = (int) Math.round(camera.screenX(node.getLocation().getX() + 0.5, BOUNDS));
		int y = (int) Math.round(camera.screenY(node.getLocation().getY() + 0.5, BOUNDS));
		input.mouseMoved(new MouseEvent(canvas, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0, x, y, 0, false));
	}

	private int renderFrame()
	{
		BufferedImage scratch = new BufferedImage(BOUNDS.width, BOUNDS.height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = scratch.createGraphics();
		try
		{
			camera.beginFrame();
			markers.beginFrame();
			markers.routes.drawTravelStations(graphics, BOUNDS);
			int pixels = 0;
			for (int y = 0; y < scratch.getHeight(); y++)
				for (int x = 0; x < scratch.getWidth(); x++)
					if ((scratch.getRGB(x, y) >>> 24) != 0) pixels++;
			tooltip.titles.clear();
			tooltip.drawTooltip(graphics, BOUNDS, markers);
			return pixels;
		}
		finally
		{
			graphics.dispose();
		}
	}

	@Test public void hiddenStationsNeverHoverOrProduceCardsAtAllThreeReportedLocations()
	{
		for (String name : Arrays.asList("Port Sarim Charter", "Port Sarim Ferry", "Entrana Monks (Port Sarim)"))
		{
			for (int attempt = 0; attempt < 3; attempt++)
			{
				setUp();
				TravelData.TravelNode node = center(name);
				hover(node);
				assertEquals(0, renderFrame());
				assertTrue(name, tooltip.titles.isEmpty());
				assertNull(name, camera.getHoveredTravelNode());
			}
		}
	}

	@Test public void disablingLayerHidesStaleCardBeforeAnotherMouseMovement()
	{
		TravelData.TravelNode node = center("Port Sarim Charter");
		config.travel = true;
		hover(node);
		assertTrue(renderFrame() > 0);
		assertEquals(Arrays.asList(node.getName()), tooltip.titles);
		config.travel = false;
		assertEquals(0, renderFrame());
		assertTrue(tooltip.titles.isEmpty());
		hover(node);
		assertNull(camera.getHoveredTravelNode());
		config.travel = true;
		hover(node);
		assertTrue(renderFrame() > 0);
		assertEquals(Arrays.asList(node.getName()), tooltip.titles);
	}

	@Test public void disabledLayerHidesCardEvenWithASelectedTravelNode()
	{
		TravelData.TravelNode node = center("Port Sarim Ferry");
		config.travel = true;
		camera.setSelectedTravelNode(node);
		hover(node);
		renderFrame();
		assertEquals(Arrays.asList(node.getName()), tooltip.titles);
		config.travel = false;
		renderFrame();
		assertTrue(tooltip.titles.isEmpty());
	}

	@Test public void normalMapHonorsMinimumZoomIncludingStaleHover()
	{
		TravelData.TravelNode node = center("Port Sarim Charter");
		config.travel = true;
		hover(node);
		renderFrame();
		assertEquals(Arrays.asList(node.getName()), tooltip.titles);
		camera.setZoom(2.9);
		assertEquals(0, renderFrame());
		assertTrue(tooltip.titles.isEmpty());
		hover(node);
		assertNull(camera.getHoveredTravelNode());
	}

	@Test public void dedicatedTravelViewKeepsStationsAndCardsWithLayerOffBelowMinimumZoom()
	{
		camera.enterTravelView();
		TravelData.TravelNode node = center("Port Sarim Charter");
		camera.setZoom(0.14);
		hover(node);
		assertEquals(node, camera.getHoveredTravelNode());
		assertTrue(renderFrame() > 0);
		assertEquals(Arrays.asList(node.getName()), tooltip.titles);
	}

	@Test public void mouseExitClearsEnabledTravelTooltip()
	{
		TravelData.TravelNode node = center("Port Sarim Charter");
		config.travel = true;
		hover(node);
		renderFrame();
		assertEquals(Arrays.asList(node.getName()), tooltip.titles);
		input.mouseExited(new MouseEvent(canvas, MouseEvent.MOUSE_EXITED, System.currentTimeMillis(), 0, -1, -1, 0, false));
		renderFrame();
		assertNull(camera.getHoveredTravelNode());
		assertTrue(tooltip.titles.isEmpty());
	}
}
