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

import java.awt.Color;
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
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;

/**
 * Reads the player's active clue scroll from RuneLite's own clue scroll plugin and exposes its
 * target tile(s) for the map to mark, plus the clue's hint panel for the map's cards to draw.
 *
 * <p>Modelled on {@link SlayerTaskTracker}: it reaches into another plugin for state and hands
 * the renderer a ready-made object to draw. Two things make clues different from the Slayer task:
 * <ul>
 *   <li>The clue plugin binds no service interface for its solved location, so the plugin
 *       instance itself is found through {@link PluginManager} (as {@code ShortestPathBridge}
 *       does) rather than injected.</li>
 *   <li>{@code getLocations()} and {@code makeOverlayHint()} on a clue can walk the scene
 *       (hot/cold candidate sets, cryptic object lookups), so they are only ever called from
 *       {@link #update()} on the client thread, never from the render loop.</li>
 * </ul>
 *
 * <p>The hint text is not scraped: {@link ClueScroll#makeOverlayHint(PanelComponent, ClueScrollPlugin)}
 * is public and fills a {@link PanelComponent}, and {@code PanelComponent.render(Graphics2D)} is
 * public, so the map's renderers draw the clue plugin's own panel verbatim. Better Map only
 * <em>appends</em> its "enemy", "spade required" and "light source required" summary lines to
 * that panel (see {@link #update()}); it never reads the panel's children back.
 */
@Slf4j
@Singleton
public class ClueScrollTracker
{
	private final PluginManager pluginManager;

	private volatile List<WorldPoint> locations = Collections.emptyList();
	private volatile String label;
	private volatile PanelComponent cluePanel;
	private volatile String enemy;
	private volatile boolean requiresSpade;
	private volatile boolean requiresLight;

	@Inject
	public ClueScrollTracker(@Nullable PluginManager pluginManager)
	{
		this.pluginManager = pluginManager;
	}

	/**
	 * Refreshes the cached clue target from the clue scroll plugin. Call once per game tick on
	 * the client thread; the render loop only reads {@link #locations()}, {@link #label()} and
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

		if (points.isEmpty())
		{
			clear();
			return;
		}

		this.locations = new ArrayList<>(points);
		this.label = friendlyName(clue);

		// The clue plugin's own hint panel, drawn verbatim by the map's renderers.
		final PanelComponent panel = new PanelComponent();
		try
		{
			clue.makeOverlayHint(panel, plugin);
		}
		catch (RuntimeException | LinkageError e)
		{
			log.debug("[BetterMap:clue] clue.makeOverlayHint failed", e);
		}

		String enemyText = null;
		try
		{
			if (clue.getEnemy() != null)
			{
				enemyText = clue.getEnemy().getText();
			}
		}
		catch (RuntimeException | LinkageError e)
		{
			log.debug("[BetterMap:clue] clue.getEnemy() failed", e);
		}
		this.enemy = enemyText;

		boolean reqSpade = false;
		boolean reqLight = false;
		try
		{
			reqSpade = clue.isRequiresSpade();
			reqLight = clue.isRequiresLight();
		}
		catch (RuntimeException | LinkageError e)
		{
			log.debug("[BetterMap:clue] clue tool requirements check failed", e);
		}
		this.requiresSpade = reqSpade;
		this.requiresLight = reqLight;

		// Better Map's requirement summary. These are appended unconditionally when the flag is
		// set: the panel's existing children cannot be inspected without reflection, so a clue
		// whose own hint already mentions the enemy/spade/light will show that line twice. A
		// duplicate line is preferable to dropping clue information; the distinct colours mark
		// these as Better Map's summary.
		if (enemyText != null)
		{
			panel.getChildren().add(LineComponent.builder()
				.left("⚔ " + enemyText)
				.leftColor(Color.RED)
				.build());
		}
		if (reqSpade)
		{
			panel.getChildren().add(LineComponent.builder()
				.left("Spade required")
				.build());
		}
		if (reqLight)
		{
			panel.getChildren().add(LineComponent.builder()
				.left("Light source required")
				.build());
		}

		this.cluePanel = panel;
	}

	/** Target tiles for the active clue step, empty when there is no clue or its step is unsolved. */
	public List<WorldPoint> locations()
	{
		return locations;
	}

	/** A short human label for the active clue, e.g. "Coordinate clue". Null when there is no clue. */
	@Nullable
	public String label()
	{
		return label;
	}

	/**
	 * The active clue step's hint panel, as filled by the clue plugin's own
	 * {@code makeOverlayHint} plus Better Map's appended requirement lines. Null when there is
	 * no clue. Renderers call {@code render(Graphics2D)} on it directly; its width is an input
	 * (set {@code preferredSize} before rendering) and its height is only known after a render,
	 * so a scratch measuring pass is needed before laying out a box around it.
	 */
	@Nullable
	public PanelComponent cluePanel()
	{
		return cluePanel;
	}

	/** The enemy to kill for this clue step if any (e.g. "Kill the Double Agent (lvl 65)"). */
	@Nullable
	public String enemy()
	{
		return enemy;
	}

	/** True if this clue step requires a spade. */
	public boolean isRequiresSpade()
	{
		return requiresSpade;
	}

	/** True if this clue step requires a light source. */
	public boolean isRequiresLight()
	{
		return requiresLight;
	}

	private void clear()
	{
		if (!locations.isEmpty())
		{
			locations = Collections.emptyList();
		}
		label = null;
		cluePanel = null;
		enemy = null;
		requiresSpade = false;
		requiresLight = false;
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

	/** "CoordinateClue" -> "Coordinate clue"; falls back to a generic label. */
	static String friendlyName(ClueScroll clue)
	{
		String name = clue.getClass().getSimpleName();
		if (name.endsWith("Clue"))
		{
			name = name.substring(0, name.length() - "Clue".length());
		}
		final StringBuilder spaced = new StringBuilder(name.length() + 4);
		for (int i = 0; i < name.length(); i++)
		{
			final char c = name.charAt(i);
			if (i > 0 && Character.isUpperCase(c) && !Character.isUpperCase(name.charAt(i - 1)))
			{
				spaced.append(' ');
			}
			spaced.append(i == 0 ? Character.toUpperCase(c) : Character.toLowerCase(c));
		}
		final String result = spaced.toString().trim();
		return result.isEmpty() ? "Clue scroll" : result + " clue";
	}
}
