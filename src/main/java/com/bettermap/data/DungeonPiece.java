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
package com.bettermap.data;

/**
 * One cave island from the dungeon-layer tuner. Source rects are native-band tiles (inclusive);
 * {@code overworld = nativeSrc + (dx, dy)}, then optional flip/rotation about the source-bounds
 * centre. Tile drawing uses shift {@code (-dx, -dy)}.
 */
public final class DungeonPiece
{
	public final int id;
	public final int layer;
	public final int plane;
	public final String label;
	/** Inclusive native-band rectangles: {minX, minY, maxX, maxY}. */
	public final int[][] rects;
	public final int dx;
	public final int dy;
	public final int rot;
	public final boolean flipX;
	public final boolean flipY;
	public final boolean locked;
	/** {@link UndergroundZone#getId()}, or empty when unknown. */
	public String zoneId;

	public DungeonPiece(
		int id,
		int layer,
		int plane,
		String label,
		int[][] rects,
		int dx,
		int dy,
		int rot,
		boolean flipX,
		boolean flipY,
		boolean locked,
		String zoneId)
	{
		this.id = id;
		this.layer = layer;
		this.plane = plane;
		this.label = label == null ? "" : label;
		this.rects = rects;
		this.dx = dx;
		this.dy = dy;
		this.rot = rot;
		this.flipX = flipX;
		this.flipY = flipY;
		this.locked = locked;
		this.zoneId = zoneId == null || "-".equals(zoneId) ? "" : zoneId;
	}

	public boolean hasZone()
	{
		return !zoneId.isEmpty();
	}

	public boolean containsNative(int x, int y)
	{
		for (int[] r : rects)
		{
			if (x >= r[0] && x <= r[2] && y >= r[1] && y <= r[3])
			{
				return true;
			}
		}
		return false;
	}

	/** Inclusive native-band AABB of every rect: {minX, minY, maxX, maxY}. */
	public int[] srcBounds()
	{
		int minX = Integer.MAX_VALUE;
		int minY = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int maxY = Integer.MIN_VALUE;
		for (int[] r : rects)
		{
			minX = Math.min(minX, r[0]);
			minY = Math.min(minY, r[1]);
			maxX = Math.max(maxX, r[2]);
			maxY = Math.max(maxY, r[3]);
		}
		return new int[] { minX, minY, maxX, maxY };
	}

	/** Inclusive overworld AABB ignoring rotation: src + (dx, dy). */
	public int[] worldBounds()
	{
		final int[] b = srcBounds();
		return new int[] { b[0] + dx, b[1] + dy, b[2] + dx, b[3] + dy };
	}
}
