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

import com.bettermap.BetterMapConfig;
import com.bettermap.pathfinding.WorldPointUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.events.PluginMessage;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginManager;

/** Owns Better Map's route snapshot and shares destination requests with Shortest Path. */
@Slf4j
@Singleton
public class ShortestPathTracker
{
	private static final String PLUGIN_CLASS = "shortestpath.ShortestPathPlugin";
	private static final String MESSAGE_SOURCE = "bettermapSource";

	private final PluginManager pluginManager;
	private final EventBus eventBus;
	private final Provider<RoutePlanner> planner;
	private final Provider<SyncedPathfindingConfig> routingConfig;
	private final BetterMapConfig mapConfig;
	private volatile List<WorldPoint> route = Collections.emptyList();
	private volatile boolean readFailed;
	private RouteStatus routeStatus;
	private volatile WorldPoint target;
	private volatile Set<Integer> targets = Collections.emptySet();
	private WorldPoint player;
	private WorldPoint repathAnchor;
	private long revision;

	public enum RouteStatus
	{
		READY, UNREACHABLE, FAILED
	}

	@Inject
	public ShortestPathTracker(PluginManager pluginManager, EventBus eventBus,
		Provider<RoutePlanner> planner, Provider<SyncedPathfindingConfig> routingConfig,
		BetterMapConfig mapConfig)
	{
		this.pluginManager = pluginManager;
		this.eventBus = eventBus;
		this.planner = planner;
		this.routingConfig = routingConfig;
		this.mapConfig = mapConfig;
	}

	/** Refresh after the player location is sampled on the client thread. */
	public void update(WorldPoint player)
	{
		this.player = player;
		WorldPoint destination = target;
		if (destination == null || player == null)
		{
			return;
		}
		SyncedPathfindingConfig config = routingConfig.get();
		if (!isRoutingEnabled())
		{
			return;
		}
		if (config.reachedDistance() >= 0 && targets.stream().anyMatch(point ->
		{
			WorldPoint candidate = WorldPointUtil.unpackWorldPoint(point);
			return player.getPlane() == candidate.getPlane()
				&& player.distanceTo2D(candidate) <= config.reachedDistance();
		}))
		{
			clear();
			return;
		}
		int distance = config.recalculateDistance();
		if (distance >= 0 && (repathAnchor == null || repathAnchor.getPlane() != player.getPlane()
			|| repathAnchor.distanceTo2D(player) > distance))
		{
			if (config.cancelInstead())
			{
				clear();
				return;
			}
			repathAnchor = player;
			planner.get().computeRoute(destination);
		}
	}

	public List<WorldPoint> route()
	{
		return route;
	}

	/** Target destination point of the current route, or null when not routing. */
	public WorldPoint target()
	{
		final List<WorldPoint> currentRoute = route;
		if (!currentRoute.isEmpty())
		{
			return currentRoute.get(currentRoute.size() - 1);
		}
		return target;
	}

	public boolean readFailed()
	{
		return readFailed;
	}

	/** Consistent, immutable state for cross-plugin route consumers. WorldPoint values are global. */
	public synchronized Map<String, Object> snapshot()
	{
		Map<String, Object> state = new HashMap<>();
		state.put("revision", revision);
		state.put("requestedTarget", target);
		state.put("targets", targets);
		state.put("target", target());
		state.put("path", route);
		state.put("enabled", isRoutingEnabled());
		state.put("readFailed", readFailed);
		state.put("status", target == null ? "IDLE" : !isRoutingEnabled() ? "DISABLED"
			: routeStatus == null ? "CALCULATING" : routeStatus.name());
		return Collections.unmodifiableMap(state);
	}

	public void startUp()
	{
		planner.get().startUp();
	}

	public void shutDown()
	{
		clear();
		planner.get().shutdown();
	}

	/** External mode never silently falls back to Better Map's own settings. */
	public boolean isRoutingEnabled()
	{
		return mapConfig.useExternalShortestPathSettings()
			? isAvailable() : mapConfig.enableShortestPath();
	}

	public boolean isAvailable()
	{
		if (pluginManager == null)
		{
			return false;
		}
		try
		{
			for (Plugin plugin : pluginManager.getPlugins())
			{
				if (plugin.getClass().getName().equals(PLUGIN_CLASS) && pluginManager.isPluginEnabled(plugin))
				{
					return true;
				}
			}
		}
		catch (RuntimeException | LinkageError e)
		{
			log.debug("[BetterMap:route] Could not check Shortest Path availability", e);
		}
		return false;
	}

	/** Calculate Better Map's route and hand the target to Shortest Path when enabled. */
	public boolean routeTo(WorldPoint destination)
	{
		if (destination == null || !isRoutingEnabled())
		{
			return false;
		}
		setTargets(Collections.singleton(WorldPointUtil.packWorldPoint(destination)));
		postTargetIfAvailable(destination);
		return true;
	}

	/** Mirror requests from helper plugins without sending them back to Shortest Path. */
	public void onPluginMessage(PluginMessage event)
	{
		if (!"shortestpath".equals(event.getNamespace()) || !isRoutingEnabled()
			|| Boolean.TRUE.equals(event.getData().get(MESSAGE_SOURCE)))
		{
			return;
		}
		if ("clear".equals(event.getName()))
		{
			clear();
			return;
		}
		if (!"path".equals(event.getName()))
		{
			return;
		}
		Object value = event.getData().get("target");
		Set<?> requested = value instanceof Set<?> ? (Set<?>) value : Collections.singleton(value);
		Set<Integer> destinations = new LinkedHashSet<>();
		for (Object point : requested)
		{
			int packed = point instanceof WorldPoint ? WorldPointUtil.packWorldPoint((WorldPoint) point)
				: point instanceof Integer ? (Integer) point : WorldPointUtil.UNDEFINED;
			if (packed == WorldPointUtil.UNDEFINED)
			{
				return;
			}
			destinations.add(packed);
		}
		if (!destinations.isEmpty())
		{
			setTargets(destinations);
		}
	}

	private void setTargets(Set<Integer> destinations)
	{
		WorldPoint destination = WorldPointUtil.unpackWorldPoint(destinations.iterator().next());
		synchronized (this)
		{
			revision++;
			target = destination;
			routeStatus = null;
			targets = Collections.unmodifiableSet(new LinkedHashSet<>(destinations));
			route = Collections.emptyList();
			repathAnchor = player;
		}
		readFailed = false;
		planner.get().computeRoute(destination);
	}

	public synchronized Set<Integer> getRouteTargets(WorldPoint expectedTarget)
	{
		return expectedTarget != null && expectedTarget.equals(target) ? targets : Collections.emptySet();
	}

	/** Recalculate with the active plugin's current routing settings. */
	public void refreshRoute()
	{
		WorldPoint destination = target;
		if (destination == null)
		{
			return;
		}
		synchronized (this)
		{
			revision++;
			route = Collections.emptyList();
			routeStatus = null;
			repathAnchor = player;
			readFailed = false;
		}
		if (isRoutingEnabled())
		{
			planner.get().computeRoute(destination);
		}
		else
		{
			planner.get().cancel();
		}
	}

	public void repostTarget()
	{
		WorldPoint destination = target;
		if (destination != null && isRoutingEnabled())
		{
			postTargetIfAvailable(destination);
		}
	}

	public boolean hasTarget()
	{
		return target != null;
	}

	/** Cancel both the local calculation and the route shared with Shortest Path. */
	public void cancelRoute()
	{
		clear();
		if (isAvailable())
		{
			try
			{
				eventBus.post(new PluginMessage("shortestpath", "clear", Collections.singletonMap(MESSAGE_SOURCE, true)));
			}
			catch (RuntimeException | LinkageError e)
			{
				log.debug("[BetterMap:route] Could not cancel route in Shortest Path", e);
			}
		}
	}

	private void postTargetIfAvailable(WorldPoint destination)
	{
		if (isAvailable())
		{
			try
			{
				Map<String, Object> data = new HashMap<>();
				data.put("target", targets.size() > 1 ? targets : destination);
				data.put(MESSAGE_SOURCE, true);
				eventBus.post(new PluginMessage("shortestpath", "path", data));
			}
			catch (RuntimeException | LinkageError e)
			{
				log.debug("[BetterMap:route] Could not request route from Shortest Path", e);
			}
		}
	}

	public synchronized long getRouteRevision(WorldPoint expectedTarget)
	{
		return expectedTarget != null && expectedTarget.equals(target) ? revision : -1;
	}

	public synchronized void publishRouteIfCurrent(long expectedRevision, int[] points, RouteStatus status)
	{
		if (expectedRevision != revision || target == null)
		{
			return;
		}
		routeStatus = status;
		if (points == null || points.length == 0)
		{
			route = Collections.emptyList();
			readFailed = status == RouteStatus.FAILED;
			return;
		}
		List<WorldPoint> snapshot = new ArrayList<>(points.length);
		for (int point : points)
		{
			snapshot.add(WorldPointUtil.unpackWorldPoint(point));
		}
		route = Collections.unmodifiableList(snapshot);
		readFailed = false;
	}

	private void clear()
	{
		synchronized (this)
		{
			revision++;
			target = null;
			routeStatus = null;
			targets = Collections.emptySet();
			repathAnchor = null;
			route = Collections.emptyList();
			readFailed = false;
		}
		planner.get().cancel();
	}
}
