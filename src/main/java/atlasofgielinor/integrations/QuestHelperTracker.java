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
package atlasofgielinor.integrations;

import atlasofgielinor.map.WorldMapPointReader;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;

/**
 * Reads the Quest Helper plugin's current step location, so Atlas of Gielinor can mark it and route to it.
 *
 * <p><b>Why it is read this way.</b> Quest Helper is a Plugin Hub plugin, not part of
 * {@code runelite-client}, so none of its types are on the classpath and it cannot be depended on
 * the way {@link ClueScrollTracker} depends on the bundled clue scroll plugin. Reflection is
 * forbidden by the Hub rules. What is left is the plugin's own public output: Quest Helper
 * registers a {@code QuestHelperWorldMapPoint} with RuneLite's {@link WorldMapPointManager} for
 * the active step (its "Display world map point" setting, on by default) and names it
 * {@value #QUEST_HELPER_POINT_NAME}. Reading a registered {@link WorldMapPoint} through the
 * client's own manager is ordinary API use, and the class-name check below is the same one
 * {@code WorldMapPointReader} uses to identify the owning package of registered points.
 *
 * <p><b>What the point actually is.</b> Quest Helper stores a <i>map-space</i> tile: for steps
 * inside the handful of dungeon regions it remaps onto the surface map, the coordinate is where
 * the world map draws the step, not the tile the player walks to. Atlas of Gielinor routes to the point
 * as read; where that is not a reachable tile, the route comes back
 * {@link MapFinder.RouteStatus#UNREACHABLE} and draws as a partial line, which is the honest
 * signal rather than a silently wrong path.
 *
 * <p>Read once per game tick on the client thread from {@code AtlasOfGielinorPlugin#onGameTick}; the
 * render loop only reads {@link #target()}.
 */
@Slf4j
@Singleton
public class QuestHelperTracker
{
	/** The name Quest Helper gives its own world map point. */
	static final String QUEST_HELPER_POINT_NAME = "Quest Helper";

	/** Shown on the map chip and used as the route label. */
	public static final String LABEL = "Quest step";

	private final WorldMapPointManager worldMapPointManager;
	private final WorldMapPointReader worldMapPointReader;

	private volatile WorldPoint target;

	@Inject
	public QuestHelperTracker(@Nullable WorldMapPointManager worldMapPointManager,
		WorldMapPointReader worldMapPointReader)
	{
		this.worldMapPointManager = worldMapPointManager;
		this.worldMapPointReader = worldMapPointReader;
	}

	/**
	 * Refreshes the cached step location. Call once per game tick on the client thread.
	 *
	 * <p>Quest Helper removes its point when the step ends and when the plugin stops, so a null
	 * target here means there is nothing to follow - no separate "is Quest Helper running" probe
	 * is needed, and Atlas of Gielinor follows exactly what is drawn on the map.
	 */
	public void update()
	{
		if (worldMapPointManager == null)
		{
			target = null;
			return;
		}

		WorldPoint found = null;
		try
		{
			// The reader keeps one snapshot per overlay frame. This runs on the tick, between
			// frames, so the pass is reopened rather than reading the last frame's list back.
			worldMapPointReader.beginPass();
			for (WorldMapPoint point : worldMapPointReader.points(worldMapPointManager))
			{
				if (!isQuestHelperPoint(point))
				{
					continue;
				}
				final WorldPoint location = point.getWorldPoint();
				if (location != null)
				{
					found = location;
					break;
				}
			}
		}
		catch (RuntimeException | LinkageError e)
		{
			log.debug("[AtlasOfGielinor:quest] reading the Quest Helper world map point failed", e);
		}

		target = found;
	}

	/** The active Quest Helper step tile, or null when there is no step to follow. */
	@Nullable
	public WorldPoint target()
	{
		return target;
	}

	/**
	 * True for Quest Helper's own world map point. Matches on the name the plugin sets, and falls
	 * back to the class name for a build that ever stops setting it. This reads {@link Class#getName()} only; it
	 * loads no Quest Helper type and reflects on nothing.
	 */
	static boolean isQuestHelperPoint(@Nullable WorldMapPoint point)
	{
		if (point == null)
		{
			return false;
		}
		if (QUEST_HELPER_POINT_NAME.equals(point.getName()))
		{
			return true;
		}
		for (Class<?> c = point.getClass(); c != null && c != Object.class; c = c.getSuperclass())
		{
			if (c.getName().contains("questhelper"))
			{
				return true;
			}
		}
		return false;
	}
}
