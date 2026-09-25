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

import com.bettermap.data.UndergroundZone;
import java.util.EnumMap;
import java.util.Map;
import net.runelite.api.coords.WorldPoint;

/**
 * Live, in-memory nudges for the dungeon layer, driven from the keyboard while
 * {@link com.bettermap.BetterMapConfig#undergroundTuner()} is on. Lets a dungeon's tile layer be
 * slid over the faint surface until it lines up, and its clip box tightened or loosened, then reads
 * the corrected numbers back so they can be pasted into {@link UndergroundZone}. Nothing here
 * persists — it is a coordinate-finding aid, not a runtime override.
 */
public final class DungeonTuner
{
	/** Per zone: {offsetX, offsetY, clipTrim}. offset shifts the sampled tiles; clipTrim shrinks
	 *  every clip box edge by that many tiles (negative widens). */
	private final Map<UndergroundZone, int[]> adjustments = new EnumMap<>(UndergroundZone.class);

	private int[] slot(UndergroundZone zone)
	{
		return adjustments.computeIfAbsent(zone, k -> new int[3]);
	}

	public boolean isTuned(UndergroundZone zone)
	{
		return zone != null && adjustments.containsKey(zone);
	}

	public int offsetX(UndergroundZone zone)
	{
		return zone == null ? 0 : slot(zone)[0];
	}

	public int offsetY(UndergroundZone zone)
	{
		return zone == null ? 0 : slot(zone)[1];
	}

	public int clipTrim(UndergroundZone zone)
	{
		return zone == null ? 0 : slot(zone)[2];
	}

	public void nudge(UndergroundZone zone, int dx, int dy)
	{
		if (zone == null)
		{
			return;
		}
		final int[] v = slot(zone);
		v[0] += dx;
		v[1] += dy;
	}

	public void trim(UndergroundZone zone, int delta)
	{
		if (zone != null)
		{
			slot(zone)[2] += delta;
		}
	}

	public void reset(UndergroundZone zone)
	{
		if (zone != null)
		{
			adjustments.remove(zone);
		}
	}

	/** The interior point this zone should carry once the current nudge is baked in. */
	public WorldPoint tunedInterior(UndergroundZone zone)
	{
		final WorldPoint p = zone.getUndergroundPoint();
		return new WorldPoint(p.getX() + offsetX(zone), p.getY() + offsetY(zone), p.getPlane());
	}

	/** One-line, paste-ready summary for the chat log / readout. */
	public String describe(UndergroundZone zone)
	{
		final WorldPoint t = tunedInterior(zone);
		return zone.name() + "  new WorldPoint(" + t.getX() + ", " + t.getY() + ", " + t.getPlane() + ")"
			+ "   nudge=(" + offsetX(zone) + ", " + offsetY(zone) + ")  clipTrim=" + clipTrim(zone);
	}
}
