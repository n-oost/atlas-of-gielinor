package atlasofgielinor.integrations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.PluginMessage;
import org.junit.Test;

import atlasofgielinor.AtlasOfGielinorConfig;
import atlasofgielinor.integrations.ShortestPathTracker;

import static org.junit.Assert.*;

public class ShortestPathTrackerTest
{
	private static final AtlasOfGielinorConfig ENABLED = new AtlasOfGielinorConfig() {};

	@Test
	public void routingRequiresEnabledHubPlugin()
	{
		ShortestPathTracker unavailable = new ShortestPathTracker(null, null, ENABLED);
		assertFalse(unavailable.isRoutingEnabled());
		assertFalse(unavailable.routeTo(new WorldPoint(3200, 3200, 0)));
		ShortestPathTracker available = new ShortestPathTracker(null, null, ENABLED)
		{
			@Override
			public boolean isAvailable() { return true; }
		};
		assertTrue(available.isRoutingEnabled());
	}

	@Test
	public void toggleOffDisablesRouting()
	{
		AtlasOfGielinorConfig disabled = new AtlasOfGielinorConfig()
		{
			@Override
			public boolean enableShortestPath() { return false; }
		};
		ShortestPathTracker tracker = new ShortestPathTracker(null, null, disabled)
		{
			@Override
			public boolean isAvailable() { return true; }
		};
		assertFalse(tracker.isRoutingEnabled());
		assertFalse(tracker.routeTo(new WorldPoint(3200, 3200, 0)));
	}

	public static class Hub
	{
		final EventBus bus = new EventBus();
		final List<PluginMessage> queries = new ArrayList<>();
		final ShortestPathTracker tracker;
		WorldPoint destination;

		Hub()
		{
			tracker = new ShortestPathTracker(null, bus, ENABLED)
			{
				@Override
				public boolean isAvailable() { return true; }
			};
			bus.register(this);
		}

		@Subscribe
		public void onPluginMessage(PluginMessage message)
		{
			if ("path".equals(message.getName())) destination = (WorldPoint) message.getData().get("target");
			else if ("clear".equals(message.getName())) destination = null;
			else if ("query".equals(message.getName())) queries.add(message);
			else if ("getTarget".equals(message.getName()) && destination != null)
			{
				tracker.onPluginMessage(new PluginMessage("shortestpath", "currentTarget",
					Map.of("id", message.getData().get("id"), "set", true, "target", List.of(destination))));
			}
		}

		void ticks(WorldPoint player)
		{
			for (int i = 0; i < 5; i++) tracker.update(player);
		}

		void result(PluginMessage query, List<WorldPoint> points)
		{
			tracker.onPluginMessage(new PluginMessage("shortestpath", "result",
				Map.of("id", query.getData().get("id"), "reached", true, "path", points)));
		}
	}

	@Test
	public void routeSnapshotUsesHubCoordinatesAndRejectsOldResults()
	{
		Hub hub = new Hub();
		WorldPoint first = new WorldPoint(3200, 3200, 0);
		WorldPoint second = new WorldPoint(3202, 3200, 0);
		assertTrue(hub.tracker.routeTo(first));
		hub.ticks(first);
		assertTrue(hub.tracker.routeTo(second));
		hub.ticks(first);
		hub.result(hub.queries.get(0), List.of(first));
		assertTrue(hub.tracker.route().isEmpty());
		hub.result(hub.queries.get(1), List.of(first, second));
		assertEquals(List.of(first, second), hub.tracker.route());
		assertFalse(hub.tracker.readFailed());
		hub.tracker.cancelRoute();
		hub.result(hub.queries.get(1), List.of(first, second));
		assertTrue(hub.tracker.route().isEmpty());
	}
}
