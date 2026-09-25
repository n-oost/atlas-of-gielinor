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

import net.runelite.api.coords.WorldPoint;

/**
 * Prifddinas exists twice in the cache: the walkable city is an instance around
 * ({@code 3264}, {@code 6080}), while the in-game world map shows an empty octagon in
 * Tirannwn around ({@code 2240}, {@code 3328}).
 *
 * <p>The offset is a measured pair of the same Gwenith cows: instance {@code (3213, 6170)}
 * versus overworld {@code (2189, 3418)} → {@code +1024}, {@code +2752}. This class maps
 * instance coordinates onto that overworld slot so the built city can be drawn where the
 * world map puts it.
 *
 * <p>The box is region-aligned and stops short of Lletya ({@code 2346}, {@code 3180}).
 */
public final class PrifddinasShift
{
	public static final int OFFSET_X = 1024;
	public static final int OFFSET_Y = 2752;

	public static final int INSTANCE_MIN_X = 3136;
	public static final int INSTANCE_MAX_X = 3392;
	public static final int INSTANCE_MIN_Y = 5952;
	public static final int INSTANCE_MAX_Y = 6208;

	public static final int OVERWORLD_MIN_X = INSTANCE_MIN_X - OFFSET_X;
	public static final int OVERWORLD_MAX_X = INSTANCE_MAX_X - OFFSET_X;
	public static final int OVERWORLD_MIN_Y = INSTANCE_MIN_Y - OFFSET_Y;
	public static final int OVERWORLD_MAX_Y = INSTANCE_MAX_Y - OFFSET_Y;

	private PrifddinasShift()
	{
	}

	public static boolean containsInstance(double worldX, double worldY)
	{
		return worldX >= INSTANCE_MIN_X && worldX < INSTANCE_MAX_X
			&& worldY >= INSTANCE_MIN_Y && worldY < INSTANCE_MAX_Y;
	}

	public static boolean containsOverworld(double worldX, double worldY)
	{
		return worldX >= OVERWORLD_MIN_X && worldX < OVERWORLD_MAX_X
			&& worldY >= OVERWORLD_MIN_Y && worldY < OVERWORLD_MAX_Y;
	}

	public static boolean overlapsOverworld(double minX, double maxX, double minY, double maxY)
	{
		return minX < OVERWORLD_MAX_X && maxX >= OVERWORLD_MIN_X
			&& minY < OVERWORLD_MAX_Y && maxY >= OVERWORLD_MIN_Y;
	}

	public static double toDisplayX(double worldX, double worldY)
	{
		return containsInstance(worldX, worldY) ? worldX - OFFSET_X : worldX;
	}

	public static double toDisplayY(double worldX, double worldY)
	{
		return containsInstance(worldX, worldY) ? worldY - OFFSET_Y : worldY;
	}

	public static int toDisplayX(int worldX, int worldY)
	{
		return (int) toDisplayX((double) worldX, worldY);
	}

	public static int toDisplayY(int worldX, int worldY)
	{
		return (int) toDisplayY((double) worldX, worldY);
	}

	public static double toWorldX(double displayX, double displayY)
	{
		return containsOverworld(displayX, displayY) ? displayX + OFFSET_X : displayX;
	}

	public static double toWorldY(double displayX, double displayY)
	{
		return containsOverworld(displayX, displayY) ? displayY + OFFSET_Y : displayY;
	}

	public static WorldPoint toDisplay(WorldPoint point)
	{
		if (point == null || !containsInstance(point.getX(), point.getY()))
		{
			return point;
		}
		return new WorldPoint(point.getX() - OFFSET_X, point.getY() - OFFSET_Y, point.getPlane());
	}

	public static WorldPoint toWorld(int displayX, int displayY, int plane)
	{
		if (!containsOverworld(displayX, displayY))
		{
			return new WorldPoint(displayX, displayY, plane);
		}
		return new WorldPoint(displayX + OFFSET_X, displayY + OFFSET_Y, plane);
	}

	/** True when this game point's display position sits inside the view AABB. */
	public static boolean inView(int worldX, int worldY, double minX, double maxX, double minY, double maxY)
	{
		final double dx = toDisplayX(worldX, worldY);
		final double dy = toDisplayY(worldX, worldY);
		return dx >= minX && dx <= maxX && dy >= minY && dy <= maxY;
	}

	/**
	 * Runs {@code lookup} at the display point, then at the matching instance point when the
	 * display point is the overworld Prifddinas slot. Used for hover so both wiki (instance)
	 * and cache (overworld) records can hit.
	 */
	public static <T> T firstHit(int displayX, int displayY, java.util.function.BiFunction<Integer, Integer, T> lookup)
	{
		final T hit = lookup.apply(displayX, displayY);
		if (hit != null || !containsOverworld(displayX, displayY))
		{
			return hit;
		}
		return lookup.apply(displayX + OFFSET_X, displayY + OFFSET_Y);
	}
}
