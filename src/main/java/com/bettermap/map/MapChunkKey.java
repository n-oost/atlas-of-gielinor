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

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/**
 * Shared 64x64 world chunk key computation and spatial query traversal utilities.
 */
public final class MapChunkKey
{
	private MapChunkKey()
	{
	}

	public static long of(int plane, int chunkX, int chunkY)
	{
		return (((long) plane & 0x3L) << 32) | (((long) (chunkX & 0xFFFF) << 16)) | ((long) (chunkY & 0xFFFF));
	}

	public static long ofWorldPoint(int plane, int worldX, int worldY)
	{
		return of(plane, worldX >> 6, worldY >> 6);
	}

	/**
	 * Iterates over all entities in the spatial chunk map within the specified world bounding box.
	 */
	public static <T> void forEachInArea(
		Map<Long, List<T>> chunkMap,
		int plane,
		int minWorldX,
		int maxWorldX,
		int minWorldY,
		int maxWorldY,
		ToIntFunction<T> xGetter,
		ToIntFunction<T> yGetter,
		Consumer<T> consumer)
	{
		final int minChunkX = minWorldX >> 6;
		final int maxChunkX = maxWorldX >> 6;
		final int minChunkY = minWorldY >> 6;
		final int maxChunkY = maxWorldY >> 6;

		for (int cx = minChunkX; cx <= maxChunkX; cx++)
		{
			for (int cy = minChunkY; cy <= maxChunkY; cy++)
			{
				final List<T> chunkItems = chunkMap.get(of(plane, cx, cy));
				if (chunkItems == null)
				{
					continue;
				}

				for (T item : chunkItems)
				{
					final int x = xGetter.applyAsInt(item);
					final int y = yGetter.applyAsInt(item);
					if (x >= minWorldX && x <= maxWorldX && y >= minWorldY && y <= maxWorldY)
					{
						consumer.accept(item);
					}
				}
			}
		}
	}

	/**
	 * Finds the nearest entity to {@code (worldX, worldY)} on {@code plane} within {@code radius}.
	 */
	public static <T> T findNearest(
		Map<Long, List<T>> chunkMap,
		int plane,
		int worldX,
		int worldY,
		int radius,
		ToIntFunction<T> xGetter,
		ToIntFunction<T> yGetter,
		Predicate<T> filter)
	{
		final int minChunkX = (worldX - radius) >> 6;
		final int maxChunkX = (worldX + radius) >> 6;
		final int minChunkY = (worldY - radius) >> 6;
		final int maxChunkY = (worldY + radius) >> 6;

		T best = null;
		int bestDistance = Integer.MAX_VALUE;

		for (int cx = minChunkX; cx <= maxChunkX; cx++)
		{
			for (int cy = minChunkY; cy <= maxChunkY; cy++)
			{
				final List<T> chunkItems = chunkMap.get(of(plane, cx, cy));
				if (chunkItems == null)
				{
					continue;
				}

				for (T item : chunkItems)
				{
					if (filter != null && !filter.test(item))
					{
						continue;
					}

					final int dx = Math.abs(xGetter.applyAsInt(item) - worldX);
					final int dy = Math.abs(yGetter.applyAsInt(item) - worldY);
					if (dx > radius || dy > radius)
					{
						continue;
					}

					final int distance = dx * dx + dy * dy;
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
}
