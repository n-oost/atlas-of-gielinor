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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import bettermap.data.BundledTsv;

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
	private static final Pattern LEVEL_REQUIREMENT = Pattern.compile("(?i)(?:level requirement:|requires:)?\\s*(?:level\\s*)?(\\d+)\\s+([a-z ]+?)(?:\\s*[,.(].*)?$");

	public static List<String> compactSkillingLines(String title, List<String> lines)
	{
		for (String line : lines)
		{
			final Matcher matcher = LEVEL_REQUIREMENT.matcher(line.trim());
			if (matcher.matches())
			{
				return List.of("Level " + matcher.group(1) + " " + matcher.group(2).trim());
			}
		}
		final Matcher titleLevel = Pattern.compile("(?i).*\\(Level (\\d+)\\).*").matcher(title);
		if (titleLevel.matches())
		{
			return List.of("Level " + titleLevel.group(1));
		}
		return Collections.emptyList();
	}

	/** Known spell requirements for teleport POIs; other travel modes have no rune spell cost. */
	public static List<String> compactTeleportLines(String title)
	{
		switch (title)
		{
			case "Teleport to Rune Essence":
				return List.of("Wizard NPC • Rune Mysteries");
			case "Teleport to Sorceress's Garden":
				return List.of("Minigame teleport • Prince Ali Rescue");
			case "Teleport to Library Archive":
				return List.of("2 Earth, 1 Law rune • 6 Magic");
			case "Teleport to Prifddinas Library":
				return List.of("Teleport crystal • Song of the Elves");
			default:
				return Collections.emptyList();
		}
	}

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
		private final Map<String, List<String>> dungeonRequirements = new HashMap<>();
		private final Map<String, List<String>> mooringRequirements = new HashMap<>();
		private int[][] moorings = new int[0][];
		private Map<String, Detail> questDetails = Collections.emptyMap();
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
		List<String> lines = entry.lines;
		if (isDungeonKey(entry.type))
		{
			final List<String> specific = data.dungeonRequirements.get(entry.title);
			if (specific != null)
			{
				lines = specific;
			}
		}
		return new Detail(entry.title, entry.category, lines);
	}

	private static void addEntry(Indexes prepared, String type, String title, String category, int x, int y, int plane, String... lines)
	{
		// Basements and cellars use the link marker, not the red dungeon marker.
		if ("dungeon".equals(type) && (title.toLowerCase(Locale.ROOT).contains("basement")
			|| title.toLowerCase(Locale.ROOT).contains("cellar")))
		{
			type = "basement";
		}
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
			final Set<String> tooltipRules = new HashSet<>();
			for (String[] fields : BundledTsv.read("/bettermap/poi/tooltip-name-rules.tsv", 5))
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
			for (String[] fields : BundledTsv.read("/com/bettermap/poi/curated-details.tsv", 6))
			{
				addEntry(prepared, fields[0], fields[1], fields[2],
					Integer.parseInt(fields[3]), Integer.parseInt(fields[4]), Integer.parseInt(fields[5]),
					Arrays.copyOfRange(fields, 6, fields.length));
			}
			for (String[] fields : BundledTsv.read("/com/bettermap/poi/dungeon-requirements.tsv", 1))
			{
				prepared.dungeonRequirements.put(fields[0], List.of(Arrays.copyOfRange(fields, 1, fields.length)));
			}
			final List<int[]> moorings = new ArrayList<>();
			for (String[] fields : BundledTsv.read("/com/bettermap/poi/mooring-levels.tsv", 3))
			{
				moorings.add(new int[]{Integer.parseInt(fields[0]), Integer.parseInt(fields[1]), Integer.parseInt(fields[2])});
				if (fields.length > 4)
				{
					prepared.mooringRequirements.put(fields[3], List.of(Arrays.copyOfRange(fields, 4, fields.length)));
				}
			}
			prepared.moorings = moorings.toArray(new int[0][]);
			prepared.questDetails = QuestDetailsData.load();
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
		final List<PoiIndex.Poi> pois = new ArrayList<>(data.entries.size());
		for (Entry e : data.entries)
		{
			pois.add(new PoiIndex.Poi(e.x, e.y, e.plane, e.type, e.title));
		}
		return Collections.unmodifiableList(pois);
	}

	/**
	 * Looks up rich game details for a given point-of-interest.
	 */
	public static Detail getDetail(PoiIndex.Poi poi, int worldX, int worldY, int plane)
	{
		final Detail nativeDetail = getNativeDetail(poi, worldX, worldY, plane);
		if (poi == null || "mooring_point".equals(poi.getKey()))
		{
			return nativeDetail;
		}
		final Detail imported = WorldMapSupplement.detail(poi);
		if (imported == null || nativeDetail == null)
		{
			return nativeDetail == null ? imported : nativeDetail;
		}
		final List<String> lines = new ArrayList<>(nativeDetail.getLines());
		for (String line : imported.getLines())
		{
			if (!lines.contains(line) && !line.equals(nativeDetail.getTitle()))
			{
				lines.add(line);
			}
		}
		return new Detail(nativeDetail.getTitle(), nativeDetail.getCategory(), lines);
	}

	private static Detail getNativeDetail(PoiIndex.Poi poi, int worldX, int worldY, int plane)
	{
		if (poi == null)
		{
			return getDetailByPosition(worldX, worldY, plane, 6);
		}

		// Mooring markers can share a tile with curated island activities. Their cache marker
		// identity is the dock; an exact-coordinate detail for a mine, cave, or skilling spot
		// must not replace the mooring tooltip.
		if ("mooring_point".equals(poi.getKey()))
		{
			return availableDetail(poi);
		}

		// 1. Direct exact lookup by coordinate key (matching marker type)
		final Entry exact = data.exactMap.get(pointKey(worldX, worldY, plane));
		if (exact != null && typeMatches(exact.type, poi.getKey()))
		{
			return entryToDetail(exact);
		}

		// 2. Exact match on POI's own world coordinate (matching marker type)
		final Entry poiExact = data.exactMap.get(pointKey(poi.getX(), poi.getY(), poi.getPlane()));
		if (poiExact != null && typeMatches(poiExact.type, poi.getKey()))
		{
			return entryToDetail(poiExact);
		}

		if ("quest_start".equals(poi.getKey()))
		{
			final Detail questDetail = data.questDetails.get(poi.getName());
			if (questDetail != null)
			{
				return questDetail;
			}
		}

		// 3. Proximity lookup within 12 tiles (chunked; dungeon_link matches dungeon entries)
		final Entry near = nearestEntry(worldX, worldY, plane, 144, poi.getKey());
		if (near != null)
		{
			return entryToDetail(near);
		}

		// 3b. Dungeon icons from the wiki often use dungeon_link while curated entries use dungeon
		if (isDungeonKey(poi.getKey()))
		{
			final Entry dungeonNear = nearestEntry(worldX, worldY, plane, 256, "dungeon");
			if (dungeonNear != null)
			{
				return entryToDetail(dungeonNear);
			}
		}

		return availableDetail(poi);
	}

	/** Use existing detail data, then show the marker's raw fields when no description exists. */
	private static Detail availableDetail(PoiIndex.Poi poi)
	{
		if ("mooring_point".equals(poi.getKey()))
		{
			return mooringDetail(poi);
		}
		final Detail imported = WorldMapSupplement.detail(poi);
		if (imported != null)
		{
			return imported;
		}
		final List<String> requirements = data.dungeonRequirements.get(poi.getName());
		if (requirements != null)
		{
			return new Detail(poi.getName(), "Dungeons", requirements);
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

	/** Linear scan kept for the spatial-index parity test. Same acceptance rule as {@link #nearestEntry}. */
	static Entry nearestEntryLinear(int worldX, int worldY, int plane, int maxDistSq, String typeHint)
	{
		Entry best = null;
		int bestDist = maxDistSq;
		for (Entry e : data.entries)
		{
			if (e.plane != plane)
			{
				continue;
			}
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
		return best;
	}

	private static int getMooringLevel(int x, int y)
	{
		int bestDist = 30 * 30;
		int bestLvl = 0;
		for (int[] m : data.moorings)
		{
			int dx = m[0] - x;
			int dy = m[1] - y;
			int dist = dx * dx + dy * dy;
			if (dist <= bestDist)
			{
				bestDist = dist;
				bestLvl = m[2];
			}
		}
		return bestLvl;
	}

	private static Detail mooringDetail(PoiIndex.Poi poi)
	{
		final List<String> lines = new ArrayList<>();
		lines.add("Type: " + poi.getKey());
		lines.add("Location: " + poi.getX() + ", " + poi.getY() + " (Plane " + poi.getPlane() + ")");
		final int level = "Mooring buoy".equalsIgnoreCase(poi.getName())
			|| "Ship boarding plank".equalsIgnoreCase(poi.getName())
			? 0 : getMooringLevel(poi.getX(), poi.getY());
		if (level > 0)
		{
			lines.add("Requires Level " + level + " Sailing");
		}
		final List<String> requirements = data.mooringRequirements.get(poi.getName());
		if (requirements != null)
		{
			lines.addAll(requirements);
		}
		if (level <= 0 && (requirements == null || requirements.isEmpty()))
		{
			lines.add("No additional details available.");
		}
		return new Detail(poi.getName(), "Travel", lines);
	}
}
