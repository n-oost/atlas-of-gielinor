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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.cluescrolls.ClueScrollPlugin;
import net.runelite.client.plugins.cluescrolls.clues.ClueScroll;
import net.runelite.client.plugins.cluescrolls.clues.LocationClueScroll;
import net.runelite.client.plugins.cluescrolls.clues.LocationsClueScroll;
import net.runelite.client.ui.overlay.components.PanelComponent;

/** Caches RuneLite's active clue locations and hint panel on the client thread. */
@Slf4j
@Singleton
public class ClueScrollTracker
{
	private final PluginManager pluginManager;

	private volatile List<WorldPoint> locations = Collections.emptyList();
	private volatile PanelComponent cluePanel;

	@Inject
	public ClueScrollTracker(@Nullable PluginManager pluginManager)
	{
		this.pluginManager = pluginManager;
	}

	/**
	 * Refreshes the cached clue target from the clue scroll plugin. Call once per game tick on
	 * the client thread; the render loop only reads {@link #locations()} and
	 * {@link #cluePanel()}.
	 */
	public void update()
	{
		final ClueScrollPlugin plugin = findEnabledCluePlugin();
		if (plugin == null)
		{
			clear();
			return;
		}

		final ClueScroll clue;
		try
		{
			clue = plugin.getClue();
		}
		catch (RuntimeException | LinkageError e)
		{
			log.debug("[BetterMap:clue] getClue() failed", e);
			clear();
			return;
		}

		if (clue == null)
		{
			clear();
			return;
		}

		final Set<WorldPoint> points = new LinkedHashSet<>();
		try
		{
			if (clue instanceof LocationsClueScroll)
			{
				addAll(points, ((LocationsClueScroll) clue).getLocations(plugin));
			}
			if (clue instanceof LocationClueScroll)
			{
				addAll(points, ((LocationClueScroll) clue).getLocations(plugin));
			}
		}
		catch (RuntimeException | LinkageError e)
		{
			log.debug("[BetterMap:clue] resolving clue locations failed", e);
		}

		this.locations = new ArrayList<>(points);

		// Keep the native hint even for clue steps without a map location.
		final PanelComponent panel = new PanelComponent();
		try
		{
			clue.makeOverlayHint(panel, plugin);
		}
		catch (RuntimeException | LinkageError e)
		{
			log.debug("[BetterMap:clue] clue.makeOverlayHint failed", e);
		}

		this.cluePanel = panel;
	}

	/** Target tiles for the active clue step, empty when there is no clue or its step is unsolved. */
	public List<WorldPoint> locations()
	{
		return locations;
	}

	/**
	 * The active clue step's hint panel, as filled by the clue plugin's own
	 * {@code makeOverlayHint}. Null when there is
	 * no clue. Renderers call {@code render(Graphics2D)} on it directly; its width is an input
	 * (set {@code preferredSize} before rendering) and its height is only known after a render,
	 * so a scratch measuring pass is needed before laying out a box around it.
	 */
	@Nullable
	public PanelComponent cluePanel()
	{
		return cluePanel;
	}

	private void clear()
	{
		if (!locations.isEmpty())
		{
			locations = Collections.emptyList();
		}
		cluePanel = null;
	}

	private static void addAll(Set<WorldPoint> into, @Nullable WorldPoint[] from)
	{
		if (from == null)
		{
			return;
		}
		for (WorldPoint p : from)
		{
			if (p != null)
			{
				into.add(p);
			}
		}
	}

	@Nullable
	private ClueScrollPlugin findEnabledCluePlugin()
	{
		if (pluginManager == null)
		{
			return null;
		}
		try
		{
			for (final Plugin p : pluginManager.getPlugins())
			{
				if (p instanceof ClueScrollPlugin && pluginManager.isPluginEnabled(p))
				{
					return (ClueScrollPlugin) p;
				}
			}
		}
		catch (RuntimeException | LinkageError e)
		{
			log.debug("[BetterMap:clue] pluginManager lookup failed", e);
		}
		return null;
	}

}
