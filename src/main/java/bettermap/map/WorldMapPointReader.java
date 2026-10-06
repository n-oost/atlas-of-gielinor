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
import java.util.List;
import javax.inject.Singleton;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;

/**
 * Reads the registered world map markers.
 *
 * <p>Better Map has to draw those markers itself, because it replaces the client's map camera and
 * RuneLite's own world map overlay would put every icon in the wrong place. The manager's list is
 * package-private ({@code @Getter(AccessLevel.PACKAGE)}) and there is no public accessor, so the
 * list is read through the one public method that hands every element out: {@link
 * WorldMapPointManager#removeIf(java.util.function.Predicate)}.
 *
 * <p>The predicate collects each point and always answers {@code false}, so nothing is removed. The
 * backing list is a {@code CopyOnWriteArrayList}, whose {@code removeIf} only replaces the array
 * when at least one element matched - a predicate that never matches leaves it untouched.
 *
 * <p>Callers within one overlay frame share a single snapshot via {@link #beginPass()} — the map
 * draw and tooltip used to each walk {@code removeIf} independently (~2× per frame).
 *
 * <p>Points owned by RuneLite's World Map plugin are omitted from Better Map's snapshot. Better
 * Map supplies its own POIs and details; other plugins' markers remain available. The manager
 * itself is unchanged, so RuneLite's native map can still use its registered points.
 */
@Singleton
public class WorldMapPointReader
{
	private final List<WorldMapPoint> scratch = new ArrayList<>();
	private List<WorldMapPoint> snapshot = Collections.emptyList();
	private boolean filledThisPass;

	/** Marks the start of an overlay frame so subsequent {@link #points} calls share one scan. */
	public void beginPass()
	{
		filledThisPass = false;
	}

	public List<WorldMapPoint> points(WorldMapPointManager manager)
	{
		if (manager == null)
		{
			return Collections.emptyList();
		}

		if (filledThisPass)
		{
			return snapshot;
		}

		scratch.clear();
		manager.removeIf(point ->
		{
			// MapPoint is package-private, so identify its owning package without importing it.
			if (!point.getClass().getName().startsWith("net.runelite.client.plugins.worldmap."))
			{
				scratch.add(point);
			}
			return false;
		});
		snapshot = scratch;
		filledThisPass = true;
		return snapshot;
	}
}
