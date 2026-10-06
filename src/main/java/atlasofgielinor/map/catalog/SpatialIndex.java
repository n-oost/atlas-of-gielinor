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
package atlasofgielinor.map.catalog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.function.ToDoubleFunction;

/**
 * Chunk storage and queries for a prepared collection of world points.
 * Build off the client thread, then publish under the owning index's lock.
 * Indexed coordinates must remain unchanged after construction.
 */
final class SpatialIndex<T>
{
	private final Map<Long, List<T>> chunks = new HashMap<>();
	private final ToIntFunction<T> xGetter;
	private final ToIntFunction<T> yGetter;

	SpatialIndex(List<T> items, ToIntFunction<T> planeGetter,
		ToIntFunction<T> xGetter, ToIntFunction<T> yGetter)
	{
		this.xGetter = xGetter;
		this.yGetter = yGetter;
		for (T item : items)
		{
			chunks.computeIfAbsent(MapChunkKey.ofWorldPoint(
				planeGetter.applyAsInt(item), xGetter.applyAsInt(item), yGetter.applyAsInt(item)),
				key -> new ArrayList<>()).add(item);
		}
	}

	T nearest(int plane, int worldX, int worldY, int radius, Predicate<T> filter)
	{
		return MapChunkKey.findNearest(chunks, plane, worldX, worldY, radius, xGetter, yGetter, filter);
	}

	/** Nearest display position; buckets still use the underlying world tile. */
	T nearest(int plane, double worldX, double worldY, double radius,
		ToDoubleFunction<T> displayX, ToDoubleFunction<T> displayY)
	{
		final int minChunkX = ((int) Math.floor(worldX - radius)) >> 6;
		final int maxChunkX = ((int) Math.ceil(worldX + radius)) >> 6;
		final int minChunkY = ((int) Math.floor(worldY - radius)) >> 6;
		final int maxChunkY = ((int) Math.ceil(worldY + radius)) >> 6;

		T best = null;
		double bestDistance = Double.MAX_VALUE;

		for (int cx = minChunkX; cx <= maxChunkX; cx++)
		{
			for (int cy = minChunkY; cy <= maxChunkY; cy++)
			{
				final List<T> chunkItems = chunks.get(MapChunkKey.of(plane, cx, cy));
				if (chunkItems == null)
				{
					continue;
				}

				for (T item : chunkItems)
				{
					final double sx = displayX.applyAsDouble(item);
					final double sy = displayY.applyAsDouble(item);
					final double dx = sx - worldX;
					final double dy = sy - worldY;
					if (Math.abs(dx) > radius || Math.abs(dy) > radius)
					{
						continue;
					}

					final double distance = dx * dx + dy * dy;
					if (distance < bestDistance)
					{
						bestDistance = distance;
						best = item;
					}
				}
			}
		}

		return best;
	}

	void forEachInArea(int plane, int minWorldX, int maxWorldX, int minWorldY, int maxWorldY,
		Consumer<T> consumer)
	{
		MapChunkKey.forEachInArea(chunks, plane, minWorldX, maxWorldX, minWorldY, maxWorldY,
			xGetter, yGetter, consumer);
	}
}
