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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.runelite.api.coords.WorldPoint;

/**
 * Loads dynamic entrance button position overrides and deleted POIs exported from the dev-tuner.
 */
public final class DungeonPoiOverrides
{
	private static final String BUTTONS_RESOURCE = "/com/bettermap/dungeons/buttons.tsv";
	private static final String DELETED_POIS_RESOURCE = "/com/bettermap/dungeons/deleted-pois.tsv";

	private static final Map<String, WorldPoint> BUTTON_OVERRIDES = loadButtonOverrides();
	private static final Set<String> DELETED_POIS = loadDeletedPois();

	private DungeonPoiOverrides()
	{
	}

	public static WorldPoint getButtonPoint(UndergroundZone zone, int index)
	{
		if (zone == null || index < 0 || index >= zone.getSurfacePoints().size())
		{
			return null;
		}
		final WorldPoint override = BUTTON_OVERRIDES.get(zone.getId() + "\t" + index);
		return override != null ? override : zone.getSurfacePoints().get(index);
	}

	public static boolean isButtonDeleted(UndergroundZone zone, int index)
	{
		return zone != null && DELETED_POIS.contains("button\t" + zone.getId() + "\t" + index);
	}

	public static boolean isFloorDeleted(OverlayFloor floor)
	{
		if (floor == null || floor.zone == null)
		{
			return false;
		}
		return DELETED_POIS.contains("floor\t" + floor.zone.getId() + "\t" + floor.plane + "\t"
			+ floor.worldX + "\t" + floor.worldY + "\t" + floor.name);
	}

	public static boolean isRawPoiDeleted(String name, int x, int y, int plane)
	{
		return DELETED_POIS.contains("raw\t" + name + "\t" + x + "\t" + y + "\t" + plane);
	}

	private static Map<String, WorldPoint> loadButtonOverrides()
	{
		final Map<String, WorldPoint> map = new HashMap<>();
		final InputStream in = DungeonPoiOverrides.class.getResourceAsStream(BUTTONS_RESOURCE);
		if (in == null)
		{
			return Collections.emptyMap();
		}
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)))
		{
			String line;
			while ((line = reader.readLine()) != null)
			{
				line = line.trim();
				if (line.isEmpty() || line.startsWith("#"))
				{
					continue;
				}
				final String[] parts = line.split("\t");
				if (parts.length >= 4)
				{
					final String zoneId = parts[0].trim();
					final int index = Integer.parseInt(parts[1].trim());
					final int x = Integer.parseInt(parts[2].trim());
					final int y = Integer.parseInt(parts[3].trim());
					final UndergroundZone z = UndergroundZone.byId(zoneId);
					final int plane = (z != null && index < z.getSurfacePoints().size())
						? z.getSurfacePoints().get(index).getPlane() : 0;
					map.put(zoneId + "\t" + index, new WorldPoint(x, y, plane));
				}
			}
		}
		catch (Exception ignored)
		{
		}
		return map;
	}

	private static Set<String> loadDeletedPois()
	{
		final Set<String> set = new HashSet<>();
		final InputStream in = DungeonPoiOverrides.class.getResourceAsStream(DELETED_POIS_RESOURCE);
		if (in == null)
		{
			return Collections.emptySet();
		}
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)))
		{
			String line;
			while ((line = reader.readLine()) != null)
			{
				line = line.trim();
				if (!line.isEmpty() && !line.startsWith("#"))
				{
					set.add(line);
				}
			}
		}
		catch (Exception ignored)
		{
		}
		return set;
	}
}
