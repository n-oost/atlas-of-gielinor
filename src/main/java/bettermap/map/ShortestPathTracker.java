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
package bettermap.map;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

import bettermap.BetterMapConfig;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.events.PluginMessage;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginManager;

/** Client-thread message bridge to the separately installed Shortest Path Hub plugin. */
@Singleton
public class ShortestPathTracker
{
	private final PluginManager pluginManager;
	private final EventBus eventBus;
	private final BetterMapConfig config;
	private final String session = "atlas-" + UUID.randomUUID();
	private List<WorldPoint> route = Collections.emptyList();
	private Set<WorldPoint> targets = Collections.emptySet();
	private WorldPoint anchor;
	private WorldPoint externalStart;
	private String targetRequest;
	private String pathRequest;
	private int tick;
	private int queryTick;
	private int pollTick = -5;
	private long revision;
	private boolean refresh;
	private boolean readFailed;
	private String status = "IDLE";

	@Inject
	public ShortestPathTracker(PluginManager pluginManager, EventBus eventBus, BetterMapConfig config)
	{
		this.pluginManager = pluginManager;
		this.eventBus = eventBus;
		this.config = config;
	}

	public void startUp()
	{
		clear();
		pollTick = tick - 5;
	}

	public void shutDown()
	{
		clear();
	}

	public boolean isAvailable()
	{
		if (pluginManager == null) return false;
		for (Plugin plugin : pluginManager.getPlugins())
		{
			if ("shortestpath.ShortestPathPlugin".equals(plugin.getClass().getName())
				&& pluginManager.isPluginEnabled(plugin)) return true;
		}
		return false;
	}

	public boolean isRoutingEnabled()
	{
		return config.enableShortestPath() && isAvailable();
	}

	public List<WorldPoint> route()
	{
		return route;
	}

	public WorldPoint target()
	{
		return targets.isEmpty() ? null : targets.iterator().next();
	}

	public boolean hasTarget()
	{
		return !targets.isEmpty();
	}

	public boolean readFailed()
	{
		return readFailed;
	}

	public Map<String, Object> snapshot()
	{
		Map<String, Object> state = new HashMap<>();
		state.put("revision", revision);
		state.put("requestedTarget", target());
		state.put("targets", targets.stream().map(point -> (point.getX() & 0x7FFF)
			| ((point.getY() & 0x7FFF) << 15) | ((point.getPlane() & 3) << 30))
			.collect(Collectors.toUnmodifiableSet()));
		state.put("target", target());
		state.put("path", route);
		state.put("enabled", isRoutingEnabled());
		state.put("readFailed", readFailed);
		state.put("status", !isRoutingEnabled() ? "DISABLED" : status);
		return Collections.unmodifiableMap(state);
	}

	/** Poll the external destination; only ask for coordinates when its route needs refreshing. */
	public void update(WorldPoint player)
	{
		tick++;
		if (!isRoutingEnabled())
		{
			clear();
			return;
		}
		if (tick - pollTick >= 5)
		{
			pollTick = tick;
			targetRequest = session + "-target-" + tick;
			eventBus.post(new PluginMessage("shortestpath", "getTarget", Map.of("id", targetRequest)));
		}
		if (pathRequest != null && tick - queryTick >= 50)
		{
			pathRequest = null;
			readFailed = true;
			status = "FAILED";
		}
		if (player != null && hasTarget() && pathRequest == null && tick - queryTick >= 5
			&& (refresh || anchor == null || player.getPlane() != anchor.getPlane()
				|| player.distanceTo2D(anchor) >= 5))
		{
			refresh = false;
			anchor = player;
			queryTick = tick;
			pathRequest = session + "-path-" + (++revision);
			status = "CALCULATING";
			eventBus.post(new PluginMessage("shortestpath", "query",
				Map.of("id", pathRequest, "start", player, "target", targets)));
		}
	}

	public boolean routeTo(WorldPoint destination)
	{
		if (destination == null || !isRoutingEnabled()) return false;
		clear();
		targets = Collections.singleton(destination);
		refresh = true;
		status = "CALCULATING";
		pollTick = queryTick = tick;
		eventBus.post(new PluginMessage("shortestpath", "path", Map.of("target", destination)));
		return true;
	}

	public void cancelRoute()
	{
		clear();
		if (isAvailable()) eventBus.post(new PluginMessage("shortestpath", "clear"));
	}

	public void refreshRoute()
	{
		pathRequest = null;
		refresh = true;
		pollTick = tick - 5;
		if (!isRoutingEnabled()) clear();
	}

	public void onPluginMessage(PluginMessage event)
	{
		if (!"shortestpath".equals(event.getNamespace()) || !isRoutingEnabled()) return;
		Map<String, Object> data = event.getData();
		if ("currentTarget".equals(event.getName()) && targetRequest != null
			&& targetRequest.equals(data.get("id")))
		{
			targetRequest = null;
			if (!Boolean.TRUE.equals(data.get("set")))
			{
				clear();
				return;
			}
			Object value = data.get("target");
			if (!(value instanceof List<?>)) return;
			Set<WorldPoint> next = new LinkedHashSet<>();
			for (Object point : (List<?>) value)
			{
				if (!(point instanceof WorldPoint)) return;
				next.add((WorldPoint) point);
			}
			if (next.isEmpty()) return;
			WorldPoint start = data.get("start") instanceof WorldPoint ? (WorldPoint) data.get("start") : null;
			if (!next.equals(targets))
			{
				clear();
				targets = Collections.unmodifiableSet(next);
				status = "CALCULATING";
				refresh = true;
			}
			if (!java.util.Objects.equals(start, externalStart)) refresh = true;
			externalStart = start;
		}
		else if ("result".equals(event.getName()) && pathRequest != null
			&& pathRequest.equals(data.get("id")))
		{
			pathRequest = null;
			Object value = data.get("path");
			if (!(value instanceof List<?>))
			{
				readFailed = true;
				status = "FAILED";
				return;
			}
			List<?> points = (List<?>) value;
			for (Object point : points)
			{
				if (!(point instanceof WorldPoint))
				{
					readFailed = true;
					status = "FAILED";
					return;
				}
			}
			@SuppressWarnings("unchecked")
			List<WorldPoint> path = (List<WorldPoint>) points;
			route = List.copyOf(path);
			readFailed = path.isEmpty() && !Boolean.TRUE.equals(data.get("reached"));
			status = readFailed ? "FAILED" : Boolean.TRUE.equals(data.get("reached")) ? "READY" : "UNREACHABLE";
			if ("CANCELLED".equals(data.get("reason"))) refresh = true;
		}
		else if ("clear".equals(event.getName())) clear();
	}

	private void clear()
	{
		revision++;
		targets = Collections.emptySet();
		route = Collections.emptyList();
		targetRequest = null;
		pathRequest = null;
		anchor = null;
		externalStart = null;
		refresh = false;
		readFailed = false;
		status = "IDLE";
	}
}
