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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;

import atlasofgielinor.data.io.BundledTsv;

import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import lombok.Getter;

/**
 * Curated details for points of interest: quest names, difficulty and starting NPCs, rare trees
 * (Varlamore camphor and jatoba among them), dungeons, skilling level requirements, and the tools
 * a spot needs.
 */
@Slf4j
public final class PoiDetails
{
	public static final class Detail
	{
		@Getter
		private final String title;
		@Getter
		private final String category;
		@Getter
		private final List<String> lines;

		public Detail(String title, String category, List<String> lines)
		{
			this.title = title;
			this.category = category;
			this.lines = lines != null ? lines : Collections.emptyList();
		}
	}

	static final class Entry
	{
		final String type;
		final String title;
		final String category;
		final List<String> lines;
		final int x;
		final int y;
		final int plane;

		Entry(String type, String title, String category, List<String> lines, int x, int y, int plane)
		{
			this.type = type;
			this.title = title;
			this.category = category;
			this.lines = lines;
			this.x = x;
			this.y = y;
			this.plane = plane;
		}
	}

	private static final class Indexes
	{
		private final List<Entry> entries = new ArrayList<>(1200);
		private final Set<String> entryKeys = new HashSet<>(1200);
		private final Map<Long, Entry> exactMap = new HashMap<>(1200);
		private final Map<Long, List<Entry>> chunkMap = new HashMap<>(1200);
		private final Map<String, String> tooltipAliases = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		private final List<String> tooltipPrefixes = new ArrayList<>();
		private final List<String[]> compactTooltipNames = new ArrayList<>();
		private boolean loaded;
	}

	private static volatile Indexes data = new Indexes();

	public static boolean isLoaded()
	{
		return data.loaded;
	}

	public static String tooltipNameAlias(String kind, String name)
	{
		return data.tooltipAliases.getOrDefault(kind + '\t' + name, name);
	}

	public static String cleanTooltipName(String title)
	{
		if (title == null) return "";
		final Indexes loaded = data;
		final String alias = loaded.tooltipAliases.get("name\t" + title);
		if (alias != null) return alias;
		for (String prefix : loaded.tooltipPrefixes)
		{
			if (!title.regionMatches(true, 0, prefix, 0, prefix.length())) continue;
			String stripped = title.substring(prefix.length()).trim();
			if (stripped.startsWith("-") || stripped.startsWith("–") || stripped.startsWith("—"))
			{
				stripped = stripped.substring(1).trim();
			}
			if (!stripped.isEmpty())
			{
				return Character.toUpperCase(stripped.charAt(0)) + stripped.substring(1);
			}
		}
		return title;
	}

	/** First matching display rule wins; unmatched external titles keep their normal formatting. */
	public static String compactTooltipName(String kind, String key, String title)
	{
		final String lowerTitle = title.toLowerCase();
		for (String[] rule : data.compactTooltipNames)
		{
			if (kind.equals(rule[0])
				&& (!rule[1].isEmpty() && rule[1].equals(key) || !rule[2].isEmpty() && lowerTitle.contains(rule[2]))
				&& (rule[3].isEmpty() || lowerTitle.contains(rule[3])))
			{
				return rule[4];
			}
		}
		return null;
	}

	private static long pointKey(int x, int y, int plane)
	{
		return (((long) plane & 0x3L) << 32) | (((long) x & 0xFFFFL) << 16) | ((long) y & 0xFFFFL);
	}

	private static long chunkKey(int plane, int chunkX, int chunkY)
	{
		return MapChunkKey.of(plane, chunkX, chunkY);
	}

	private static boolean isDungeonKey(String key)
	{
		return "dungeon".equals(key) || "dungeon_link".equals(key) || "basement".equals(key);
	}

	private static boolean isTravelKey(String key)
	{
		return "transportation".equals(key) || "canoe_station".equals(key)
			|| "hot_air_balloon".equals(key) || "magic_mushtree".equals(key)
			|| "magic_carpet".equals(key) || "minecart_network".equals(key);
	}

	private static boolean typeMatches(String entryType, String typeHint)
	{
		if (typeHint == null || typeHint.equals(entryType))
		{
			return true;
		}
		if (isDungeonKey(typeHint) && isDungeonKey(entryType))
		{
			return true;
		}
		return isTravelKey(typeHint) && isTravelKey(entryType);
	}

	private static Detail entryToDetail(Entry entry)
	{
		return new Detail(entry.title, entry.category, entry.lines);
	}

	private static void addEntry(Indexes prepared, String type, String title, String category, int x, int y, int plane, String... lines)
	{
		final String key = type + '\u0000' + title + '\u0000' + pointKey(x, y, plane);
		if (!prepared.entryKeys.add(key))
		{
			return;
		}
		final List<String> lineList = new ArrayList<>(lines.length);
		for (String l : lines)
		{
			if (l != null && !l.isEmpty())
			{
				lineList.add(l);
			}
		}
		final Entry entry = new Entry(type, title, category, Collections.unmodifiableList(lineList), x, y, plane);
		prepared.entries.add(entry);
		prepared.exactMap.put(pointKey(x, y, plane), entry);
		prepared.chunkMap.computeIfAbsent(chunkKey(plane, x >> 6, y >> 6), k -> new ArrayList<>()).add(entry);
	}

	/** Load on the POI startup worker; publish only the complete set of indexes. */
	public static void load()
	{
		if (data.loaded)
		{
			return;
		}
		final Indexes prepared = new Indexes();
		try
		{
			MapCatalog.load();
			final Set<String> tooltipRules = new HashSet<>();
			for (String[] fields : BundledTsv.read("/atlasofgielinor/poi/tooltip-name-rules.tsv", 5))
			{
				if (fields.length != 5 || fields[1].isEmpty() && fields[2].isEmpty()
					|| !tooltipRules.add(String.join("\t", Arrays.copyOf(fields, 4))))
				{
					throw new IOException("Invalid or duplicate tooltip name rule");
				}
				if ("compact".equals(fields[0]) || "mooring".equals(fields[0]))
				{
					if (fields[4].isEmpty()) throw new IOException("Missing compact tooltip name");
					prepared.compactTooltipNames.add(fields);
				}
				else
				{
					if (fields[1].isEmpty() || !fields[2].isEmpty() || !fields[3].isEmpty())
					{
						throw new IOException("Invalid tooltip alias or prefix");
					}
					if ("prefix".equals(fields[0]))
					{
						if (!fields[4].isEmpty()) throw new IOException("Tooltip prefixes cannot replace names");
						prepared.tooltipPrefixes.add(fields[1]);
					}
					else if ("name".equals(fields[0]) || "fish".equals(fields[0]))
					{
						if (fields[4].isEmpty() || prepared.tooltipAliases.putIfAbsent(
							fields[0] + '\t' + fields[1], fields[4]) != null)
						{
							throw new IOException("Missing or duplicate tooltip alias");
						}
					}
					else throw new IOException("Unknown tooltip name rule: " + fields[0]);
				}
			}
			for (MapCatalog.Location location : MapCatalog.current().locations.values())
			{
				addEntry(prepared, location.iconKey, location.detailTitle, location.category,
					location.x, location.y, location.plane, MapCatalog.current().detailLines(location).toArray(new String[0]));
			}
			if (Thread.currentThread().isInterrupted()) return;
			prepared.loaded = true;
			data = prepared;
		}
		catch (IOException | IllegalArgumentException e)
		{
			log.debug("Could not load curated POI details", e);
		}
	}

	public static List<PoiIndex.Poi> getAllPois()
	{
		final List<PoiIndex.Poi> pois = new ArrayList<>();
		for (MapCatalog.Location location : MapCatalog.current().locations.values())
		{
			if (!location.rendered && location.searchable) continue;
			pois.add(new PoiIndex.Poi(location.x, location.y, location.plane,
				location.iconKey, location.detailTitle, location.id));
		}
		return Collections.unmodifiableList(pois);
	}

	/**
	 * Looks up rich game details for a given point-of-interest.
	 */
	public static Detail getDetail(PoiIndex.Poi poi, int worldX, int worldY, int plane)
	{
		if (poi != null && poi.getLocationId() != null)
		{
			final MapCatalog.Location location = MapCatalog.current().locations.get(poi.getLocationId());
			if (location != null)
				return new Detail(location.detailTitle, location.category, MapCatalog.current().detailLines(location));
		}
		final Detail nativeDetail = getNativeDetail(poi, worldX, worldY, plane);
		if (poi == null || "mooring_point".equals(poi.getKey()))
		{
			return nativeDetail;
		}
		return nativeDetail;
	}

	private static Detail getNativeDetail(PoiIndex.Poi poi, int worldX, int worldY, int plane)
	{
		if (poi == null) return getDetailByPosition(worldX, worldY, plane, 6);
		// Compatibility for positional callers. Indexed POIs always resolve by ID before this path.
		final Entry nearby = nearestEntry(worldX, worldY, plane, 144, poi.getKey());
		if (nearby != null) return entryToDetail(nearby);
		if ("agility_short-cut".equals(poi.getKey()))
		{
			return new Detail(poi.getName(), PoiCategory.of(poi.getKey()).getDisplayName(), List.of(
				"Agility shortcuts connect nearby areas.",
				"Check the Agility skill guide for the level requirement."
			));
		}
		return new Detail(poi.getName(), PoiCategory.of(poi.getKey()).getDisplayName(), List.of(
			"Type: " + poi.getKey(),
			"Location: " + poi.getX() + ", " + poi.getY() + " (Plane " + poi.getPlane() + ")",
			"No additional details available."
		));
	}

	public static Detail getDetailByPosition(int worldX, int worldY, int plane, int radius)
	{
		// Exact coordinate match
		final Entry exact = data.exactMap.get(pointKey(worldX, worldY, plane));
		if (exact != null)
		{
			return entryToDetail(exact);
		}

		final Entry best = nearestEntry(worldX, worldY, plane, (radius + 2) * (radius + 2), null);
		if (best != null)
		{
			return entryToDetail(best);
		}

		return null;
	}

	/**
	 * Nearest curated entry within {@code maxDistSq} of (x, y) on {@code plane}.
	 *
	 * <p>When {@code typeHint} is non-null, an entry is accepted only if its type matches the hint
	 * or it is within 4 tiles ({@code dist <= 16}). Uses strict {@code <} for ties so the first
	 * visited entry wins (chunk order), matching the spatial-index parity tests.</p>
	 */
	static Entry nearestEntry(int worldX, int worldY, int plane, int maxDistSq, String typeHint)
	{
		final int radius = (int) Math.ceil(Math.sqrt(maxDistSq));
		final int minChunkX = (worldX - radius) >> 6;
		final int maxChunkX = (worldX + radius) >> 6;
		final int minChunkY = (worldY - radius) >> 6;
		final int maxChunkY = (worldY + radius) >> 6;

		Entry best = null;
		int bestDist = maxDistSq;

		for (int cx = minChunkX; cx <= maxChunkX; cx++)
		{
			for (int cy = minChunkY; cy <= maxChunkY; cy++)
			{
				final List<Entry> chunkEntries = data.chunkMap.get(chunkKey(plane, cx, cy));
				if (chunkEntries == null)
				{
					continue;
				}

				for (Entry e : chunkEntries)
				{
					final int dx = e.x - worldX;
					final int dy = e.y - worldY;
					final int dist = dx * dx + dy * dy;
					if (dist >= bestDist)
					{
						continue;
					}
					if (typeHint != null && !typeMatches(e.type, typeHint))
					{
						continue;
					}
					bestDist = dist;
					best = e;
				}
			}
		}

		return best;
	}

}
