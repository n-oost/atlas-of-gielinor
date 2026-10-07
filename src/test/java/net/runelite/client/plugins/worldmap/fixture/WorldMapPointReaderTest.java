package net.runelite.client.plugins.worldmap.fixture;

import com.bettermap.map.WorldMapPointReader;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;
import org.junit.Test;
import questhelper.tools.FakeQuestHelperWorldMapPoint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/** Uses a fixture subpackage to avoid adding unsigned classes to RuneLite's signed package. */
public class WorldMapPointReaderTest
{
	private static class VanillaPoint extends WorldMapPoint
	{
		private VanillaPoint(WorldPoint location, BufferedImage image)
		{
			super(location, image);
		}
	}

	@Test
	public void excludesVanillaPointsWithoutRemovingRegisteredMarkers()
	{
		final WorldPoint location = new WorldPoint(3273, 3195, 0);
		final WorldMapPoint teleport = new VanillaPoint(location,
			new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB));
		final WorldMapPoint tooltipOnly = new VanillaPoint(location, null);
		tooltipOnly.setTooltip("Vanilla course tooltip");
		final WorldMapPoint pin = new WorldMapPoint(location, null);
		pin.setName("World Map");
		final WorldMapPoint questHelper = new FakeQuestHelperWorldMapPoint(location);
		final WorldMapPointManager manager = new WorldMapPointManager();
		final List<WorldMapPoint> registered = Arrays.asList(teleport, tooltipOnly, pin, questHelper);
		registered.forEach(manager::add);
		final WorldMapPointReader reader = new WorldMapPointReader();

		assertEquals(Arrays.asList(pin, questHelper), reader.points(manager));
		reader.beginPass();
		assertEquals(Arrays.asList(pin, questHelper), reader.points(manager));

		final List<WorldMapPoint> retained = new ArrayList<>();
		manager.removeIf(point ->
		{
			retained.add(point);
			return false;
		});
		assertEquals(registered, retained);
	}

	@Test
	public void sharesSnapshotWithinPassAndRefreshesExternalPointsOnNextPass()
	{
		final WorldMapPointManager manager = new WorldMapPointManager();
		final WorldMapPointReader reader = new WorldMapPointReader();
		final List<WorldMapPoint> first = reader.points(manager);
		final WorldMapPoint pin = new WorldMapPoint(new WorldPoint(3200, 3200, 1), null);
		manager.add(pin);

		assertSame(first, reader.points(manager));
		assertEquals(Collections.emptyList(), reader.points(manager));
		reader.beginPass();
		assertEquals(Collections.singletonList(pin), reader.points(manager));
	}
}
