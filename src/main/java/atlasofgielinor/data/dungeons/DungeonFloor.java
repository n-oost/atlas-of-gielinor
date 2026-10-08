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
package atlasofgielinor.data.dungeons;

import atlasofgielinor.data.io.BundledTsv;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import net.runelite.api.coords.WorldPoint;

/**
 * A floor / sub-layer hover point sitting on a dungeon's composed footprint, not on the
 * entrance chip. Runtime data lives in {@code floors.tsv}.
 */
public final class DungeonFloor
{
	private static final String RESOURCE = "/atlasofgielinor/dungeons/floors.tsv";
	private static volatile List<DungeonFloor> ALL = Collections.emptyList();
	private static volatile boolean loaded;
	private static volatile Map<String, DungeonFloor> ENTRANCE_FLOORS = Collections.emptyMap();

	public final String name;
	public final UndergroundZone zone;
	public final int plane;
	public final int worldX;
	public final int worldY;
	/** Optional authored layer, for distinct floors sharing a game plane. */
	public final Integer layerId;

	public DungeonFloor(String name, UndergroundZone zone, int plane, int worldX, int worldY)
	{
		this(name, zone, plane, worldX, worldY, null);
	}

	public DungeonFloor(String name, UndergroundZone zone, int plane, int worldX, int worldY, Integer layerId)
	{
		this.name = name;
		this.zone = zone;
		this.plane = plane;
		this.worldX = worldX;
		this.worldY = worldY;
		this.layerId = layerId;
	}

	/** Initialize on the map data worker; frame-time reads never open resources. */
	public static void load()
	{
		if (loaded) return;
		final List<DungeonFloor> prepared = loadResource();
		final Map<String, DungeonFloor> entrances = new HashMap<>();
		try
		{
			for (String[] row : BundledTsv.read("/atlasofgielinor/dungeons/entrance-floors.tsv", 4))
			{
				for (DungeonFloor floor : prepared)
				{
					if (floor.zone.getId().equals(row[0]) && floor.plane == Integer.parseInt(row[2])
						&& floor.layerId != null && floor.layerId == Integer.parseInt(row[3]))
					{
						entrances.put(row[0] + "\t" + row[1], floor);
						break;
					}
				}
			}
		}
		catch (IOException | NumberFormatException ignored)
		{
			// Unmapped entrances retain the zone's default floor.
		}
		if (Thread.currentThread().isInterrupted()) return;
		ALL = prepared;
		ENTRANCE_FLOORS = Collections.unmodifiableMap(entrances);
		loaded = true;
	}

	public static List<DungeonFloor> all()
	{
		return ALL;
	}

	/** Floor reached by a particular surface entrance, using the original navigation point. */
	public static DungeonFloor forEntrance(UndergroundZone zone, WorldPoint entrance)
	{
		if (zone == null || entrance == null) return null;
		final List<WorldPoint> points = zone.getSurfacePoints();
		for (int i = 0; i < points.size(); i++)
			if (points.get(i).equals(entrance)) return ENTRANCE_FLOORS.get(zone.getId() + "\t" + i);
		return null;
	}

	/** Whether a zone's authored floors span more than one game plane. */
	public static boolean hasMultiplePlanes(UndergroundZone zone)
	{
		int firstPlane = -1;
		for (DungeonFloor floor : all())
		{
			if (floor.zone != zone)
			{
				continue;
			}
			if (firstPlane < 0)
			{
				firstPlane = floor.plane;
			}
			else if (floor.plane != firstPlane)
			{
				return true;
			}
		}
		return false;
	}

	/** First authored sub-layer shown before the user chooses a floor chip. */
	public static Integer defaultLayerFor(UndergroundZone zone)
	{
		for (DungeonFloor floor : all())
		{
			if (floor.zone == zone && floor.layerId != null)
			{
				return floor.layerId;
			}
		}
		return null;
	}

	public static List<DungeonFloor> parse(String text) throws IOException
	{
		return parse(new StringReader(text));
	}

	public static List<DungeonFloor> parse(Reader source) throws IOException
	{
		final List<DungeonFloor> out = new ArrayList<>();
		try (BufferedReader reader = source instanceof BufferedReader
			? (BufferedReader) source
			: new BufferedReader(source))
		{
			String line;
			while ((line = reader.readLine()) != null)
			{
				if (line.isEmpty() || line.charAt(0) == '#')
				{
					continue;
				}
				final String[] f = line.split("\t", -1);
				if (f.length < 5)
				{
					continue;
				}
				final UndergroundZone zone = UndergroundZone.byId(f[0].trim());
				if (zone == null)
				{
					continue;
				}
				try
				{
					out.add(new DungeonFloor(
						f[1],
						zone,
						Integer.parseInt(f[2].trim()),
						Integer.parseInt(f[3].trim()),
						Integer.parseInt(f[4].trim()),
						f.length > 5 && !f[5].trim().isEmpty() ? Integer.valueOf(f[5].trim()) : null));
				}
				catch (NumberFormatException ignored)
				{
					// skip a malformed row
				}
			}
		}
		return out;
	}

	private static List<DungeonFloor> loadResource()
	{
		final InputStream in = DungeonFloor.class.getResourceAsStream(RESOURCE);
		if (in == null)
		{
			return Collections.emptyList();
		}
		try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
		{
			final List<DungeonFloor> floors = parse(reader);
			for (String[] row : BundledTsv.read("/atlasofgielinor/dungeons/deleted-pois.tsv", 1))
			{
				if (row.length == 6 && "floor".equals(row[0]))
				{
					// Coordinates can change between exports; zone, plane and name identify the floor.
					floors.removeIf(floor -> floor.zone.getId().equals(row[1])
						&& Integer.toString(floor.plane).equals(row[2]) && floor.name.equals(row[5]));
				}
			}
			return floors;
		}
		catch (IOException e)
		{
			return Collections.emptyList();
		}
	}
}
