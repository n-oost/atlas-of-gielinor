package com.bettermap.map;

import com.bettermap.BetterMapConfig;
import com.bettermap.pathfinding.WorldPointUtil;
import java.util.Arrays;
import net.runelite.api.coords.WorldPoint;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ShortestPathTrackerTest
{
	private static final BetterMapConfig ENABLED = new BetterMapConfig() {};

	@Test
	public void externalModeRequiresEnabledPluginEvenIfLocalToggleAlsoTrue()
	{
		BetterMapConfig external = new BetterMapConfig()
		{
			@Override
			public boolean useExternalShortestPathSettings()
			{
				return true;
			}
		};
		ShortestPathTracker unavailable = new ShortestPathTracker(null, null, null, null, external);
		assertFalse(unavailable.isRoutingEnabled());
		assertFalse(unavailable.routeTo(new WorldPoint(3200, 3200, 0)));
		ShortestPathTracker available = new ShortestPathTracker(null, null, null, null, external)
		{
			@Override
			public boolean isAvailable()
			{
				return true;
			}
		};
		assertTrue(available.isRoutingEnabled());
	}

	@Test
	public void bothTogglesOffDisablesRouting()
	{
		BetterMapConfig disabled = new BetterMapConfig()
		{
			@Override
			public boolean enableShortestPath()
			{
				return false;
			}
		};
		ShortestPathTracker tracker = new ShortestPathTracker(null, null, null, null, disabled);
		assertFalse(tracker.isRoutingEnabled());
		assertFalse(tracker.routeTo(new WorldPoint(3200, 3200, 0)));
	}

	@Test
	public void routeSnapshotUsesLocalPathAndRejectsOldResults()
	{
		RoutePlanner planner = new RoutePlanner(null, null, null, null)
		{
			@Override
			public synchronized void computeRoute(WorldPoint target)
			{
			}
		};
		try
		{
			ShortestPathTracker tracker = new ShortestPathTracker(null, null, () -> planner, () -> null, ENABLED);
			WorldPoint first = new WorldPoint(3200, 3200, 0);
			WorldPoint second = new WorldPoint(3202, 3200, 0);
			assertTrue(tracker.routeTo(first));
			long oldRevision = tracker.getRouteRevision(first);
			assertTrue(tracker.routeTo(second));
			long currentRevision = tracker.getRouteRevision(second);
			int[] points = {WorldPointUtil.packWorldPoint(first), WorldPointUtil.packWorldPoint(second)};
			tracker.publishRouteIfCurrent(oldRevision, points, ShortestPathTracker.RouteStatus.READY);
			assertTrue(tracker.route().isEmpty());
			tracker.publishRouteIfCurrent(currentRevision, points, ShortestPathTracker.RouteStatus.READY);
			assertEquals(Arrays.asList(first, second), tracker.route());
			assertFalse(tracker.readFailed());
		}
		finally
		{
			planner.shutdown();
		}
	}
}
