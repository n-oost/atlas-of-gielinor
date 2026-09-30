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
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.
 */
package com.bettermap.map;

import com.bettermap.BetterMapConfig;
import com.bettermap.pathfinding.WorldPointUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.events.PluginMessage;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginManager;

/** Owns Better Map's route snapshot and sends targets to Shortest Path when it is enabled. */
@Slf4j
@Singleton
public class ShortestPathTracker
{
	private static final String PLUGIN_CLASS = "shortestpath.ShortestPathPlugin";

	private final PluginManager pluginManager;
	private final EventBus eventBus;
	private final Provider<RoutePlanner> planner;
	private final Provider<SyncedPathfindingConfig> routingConfig;
	private final BetterMapConfig mapConfig;
	private volatile List<WorldPoint> route = Collections.emptyList();
	private volatile boolean readFailed;
	private volatile WorldPoint target;
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
		if (config.reachedDistance() >= 0 && player.getPlane() == destination.getPlane()
			&& player.distanceTo2D(destination) <= config.reachedDistance())
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

	public boolean readFailed()
	{
		return readFailed;
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
		synchronized (this)
		{
			revision++;
			target = destination;
			route = Collections.emptyList();
			repathAnchor = player;
		}
		readFailed = false;
		planner.get().computeRoute(destination);
		postTargetIfAvailable(destination);
		return true;
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

	private void postTargetIfAvailable(WorldPoint destination)
	{
		if (isAvailable())
		{
			try
			{
				Map<String, Object> data = new HashMap<>();
				data.put("target", destination);
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
			repathAnchor = null;
			route = Collections.emptyList();
			readFailed = false;
		}
		planner.get().cancel();
	}
}
