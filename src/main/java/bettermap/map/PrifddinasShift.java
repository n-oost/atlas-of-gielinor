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

import java.io.IOException;
import java.util.List;
import bettermap.data.BundledTsv;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
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
@Getter
@Slf4j
public final class PrifddinasShift
{
	private static volatile PrifddinasShift mapping;
	private final int offsetX;
	private final int offsetY;
	private final int instanceMinX;
	private final int instanceMaxX;
	private final int instanceMinY;
	private final int instanceMaxY;
	private final int overworldMinX;
	private final int overworldMaxX;
	private final int overworldMinY;
	private final int overworldMaxY;

	private PrifddinasShift(int[] fields)
	{
		offsetX = fields[0];
		offsetY = fields[1];
		instanceMinX = fields[2];
		instanceMaxX = fields[3];
		instanceMinY = fields[4];
		instanceMaxY = fields[5];
		overworldMinX = Math.subtractExact(instanceMinX, offsetX);
		overworldMaxX = Math.subtractExact(instanceMaxX, offsetX);
		overworldMinY = Math.subtractExact(instanceMinY, offsetY);
		overworldMaxY = Math.subtractExact(instanceMaxY, offsetY);
		if (instanceMinX >= instanceMaxX || instanceMinY >= instanceMaxY
			|| instanceMinX < MapCamera.MIN_WORLD_X || instanceMaxX > MapCamera.MAX_WORLD_X
			|| instanceMinY < MapCamera.MIN_WORLD_Y || instanceMaxY > MapCamera.MAX_WORLD_Y
			|| overworldMinX < MapCamera.MIN_WORLD_X || overworldMaxX > MapCamera.MAX_WORLD_X
			|| overworldMinY < MapCamera.MIN_WORLD_Y || overworldMaxY > MapCamera.MAX_WORLD_Y)
		{
			throw new IllegalArgumentException("Invalid Prifddinas placement");
		}
	}

	/** Load and validate on the startup worker before any dependent catalog is built. */
	public static void load()
	{
		if (isLoaded()) return;
		try
		{
			final List<String[]> rows = BundledTsv.read("/bettermap/data/prifddinas-placement.tsv", 6);
			if (rows.size() != 1 || rows.get(0).length != 6) throw new IOException("Invalid Prifddinas placement row");
			final int[] fields = new int[6];
			for (int i = 0; i < fields.length; i++) fields[i] = Integer.parseInt(rows.get(0)[i]);
			final PrifddinasShift prepared = new PrifddinasShift(fields);
			if (Thread.currentThread().isInterrupted()) return;
			mapping = prepared;
		}
		catch (IOException | RuntimeException e)
		{
			log.debug("Could not load Prifddinas placement", e);
		}
	}

	public static boolean isLoaded()
	{
		return mapping != null;
	}

	public static PrifddinasShift get()
	{
		final PrifddinasShift result = mapping;
		if (result == null) throw new IllegalStateException("Prifddinas placement has not been loaded");
		return result;
	}

	public static boolean containsInstance(double worldX, double worldY)
	{
		if (!isLoaded()) return false;
		return worldX >= get().instanceMinX && worldX < get().instanceMaxX
			&& worldY >= get().instanceMinY && worldY < get().instanceMaxY;
	}

	public static boolean containsOverworld(double worldX, double worldY)
	{
		if (!isLoaded()) return false;
		return worldX >= get().overworldMinX && worldX < get().overworldMaxX
			&& worldY >= get().overworldMinY && worldY < get().overworldMaxY;
	}

	public static boolean overlapsOverworld(double minX, double maxX, double minY, double maxY)
	{
		if (!isLoaded()) return false;
		return minX < get().overworldMaxX && maxX >= get().overworldMinX
			&& minY < get().overworldMaxY && maxY >= get().overworldMinY;
	}

	public static double toDisplayX(double worldX, double worldY)
	{
		return containsInstance(worldX, worldY) ? worldX - get().offsetX : worldX;
	}

	public static double toDisplayY(double worldX, double worldY)
	{
		return containsInstance(worldX, worldY) ? worldY - get().offsetY : worldY;
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
		return containsOverworld(displayX, displayY) ? displayX + get().offsetX : displayX;
	}

	public static double toWorldY(double displayX, double displayY)
	{
		return containsOverworld(displayX, displayY) ? displayY + get().offsetY : displayY;
	}

	public static WorldPoint toDisplay(WorldPoint point)
	{
		if (point == null || !containsInstance(point.getX(), point.getY()))
		{
			return point;
		}
		return new WorldPoint(point.getX() - get().offsetX, point.getY() - get().offsetY, point.getPlane());
	}

	public static WorldPoint toWorld(int displayX, int displayY, int plane)
	{
		if (!containsOverworld(displayX, displayY))
		{
			return new WorldPoint(displayX, displayY, plane);
		}
		return new WorldPoint(displayX + get().offsetX, displayY + get().offsetY, plane);
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
		return lookup.apply(displayX + get().offsetX, displayY + get().offsetY);
	}
}
