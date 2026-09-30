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
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.imageio.ImageIO;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

/**
 * Names for the things on the map.
 *
 * <p>Seven sources feed this index, in order:
 * <ol>
 *   <li>world-map icons from the current game cache ({@code poi/pois.tsv}), prefetched offline</li>
 *   <li>curated additions absent from the game's icon layer</li>
 *   <li>older wiki icons not found in the current cache export ({@code poi/pois-legacy.tsv})</li>
 *   <li>mapped banks missing from that icon layer ({@code poi/pois-banks.tsv})</li>
 *   <li>named region labels from the game cache ({@code poi/pois-cache.tsv}), which cover
 *       Varlamore, Prifddinas and underground places the icon layer misses</li>
 *   <li>canonical modeled-dungeon search targets from {@link UndergroundZone}</li>
 *   <li>curated entries from {@link PoiDetails}</li>
 * </ol>
 * Native icon rows win rendering ties. Distinct curated names at the same coordinate remain
 * searchable aliases, so a generic marker does not erase the place's real name.
 *
 * <p>All data is bundled or read from the local tile store; runtime loading makes no web requests.
 */
@Slf4j
@Singleton
public class PoiIndex
{
	/** How far from a cursor a point of interest can be and still be the thing being pointed at. */
	private static final int MATCH_RADIUS = 6;

	private final List<Poi> pois = new ArrayList<>();
	/** Search-only aliases that share a rendered point with an earlier source. */
	private final List<Poi> searchPois = new ArrayList<>();
	private final Set<String> searchIdentities = new HashSet<>();
	private final Map<String, List<Poi>> byKey = new TreeMap<>();
	private final Map<Long, List<Poi>> chunkMap = new HashMap<>();
	private final Map<String, BufferedImage> icons = new HashMap<>();

	private File tileDir;
	private boolean loaded;
	private volatile long dataVersion;

	public long getDataVersion()
	{
		return dataVersion;
	}

	private static long chunkKey(int plane, int chunkX, int chunkY)
	{
		return (((long) plane & 0x3L) << 32) | (((long) (chunkX & 0xFFFF) << 16)) | ((long) (chunkY & 0xFFFF));
	}

	/** Root of the bundled POI data inside the jar. */
	private static final String RESOURCE_ROOT = "/com/bettermap/poi/";

	/**
	 * Opens one bundled POI file, falling back to the on-disk tile store.
	 *
	 * <p>The data ships in the jar, which is never unpacked on disk, so it is read as a stream
	 * rather than as a file. Dev tooling can override supplementary files in the tile store.
	 * The main icon snapshot is always read from the jar when present.
	 */
	private static InputStream open(File tileDir, String name)
	{
		if (tileDir != null)
		{
			final File file = new File(tileDir, "poi/" + name);
			if (file.isFile() && file.length() > 0)
			{
				try
				{
					return Files.newInputStream(file.toPath());
				}
				catch (IOException e)
				{
					log.debug("Unreadable POI file {}", file, e);
				}
			}
		}
		return PoiIndex.class.getResourceAsStream(RESOURCE_ROOT + name);
	}

	public static File file(File tileDir)
	{
		return new File(tileDir, "poi/pois.tsv");
	}

	public static File cacheFile(File tileDir)
	{
		return new File(tileDir, "poi/pois-cache.tsv");
	}

	public static File iconFile(File tileDir, String key)
	{
		return new File(tileDir, "poi/icons/" + key + ".png");
	}

	public void load(File tileDir)
	{
		final PoiIndex prepared = new PoiIndex();
		prepared.loadData(tileDir);
		if (Thread.currentThread().isInterrupted())
		{
			return;
		}
		synchronized (this)
		{
			pois.clear();
			pois.addAll(prepared.pois);
			searchPois.clear();
			searchPois.addAll(prepared.searchPois);
			searchIdentities.clear();
			searchIdentities.addAll(prepared.searchIdentities);
			byKey.clear();
			byKey.putAll(prepared.byKey);
			chunkMap.clear();
			chunkMap.putAll(prepared.chunkMap);
			icons.clear();
			icons.putAll(prepared.icons);
			loaded = true;
			dataVersion++;
		}
	}

	private void loadData(File tileDir)
	{
		this.tileDir = tileDir;
		pois.clear();
		searchPois.clear();
		searchIdentities.clear();
		byKey.clear();
		chunkMap.clear();
		icons.clear();
		loaded = false;

		final Set<Long> existingPoints = new HashSet<>();
		// Downloaded packs can contain an older icon overlay. The bundled snapshot is authoritative.
		InputStream bundledPois = PoiIndex.class.getResourceAsStream(RESOURCE_ROOT + "pois.tsv");
		readTsv(bundledPois != null ? bundledPois : open(tileDir, "pois.tsv"), existingPoints);
		readTsv(PoiIndex.class.getResourceAsStream(RESOURCE_ROOT + "pois-additions.tsv"), existingPoints);
		readTsv(PoiIndex.class.getResourceAsStream(RESOURCE_ROOT + "pois-legacy.tsv"), existingPoints);
		// Keep bank catalogue names searchable without drawing a second icon a tile or two away.
		readTsv(open(tileDir, "pois-banks.tsv"), existingPoints, true);
		// Cache labels fill gaps the native icon layer misses.
		readTsv(open(tileDir, "pois-cache.tsv"), existingPoints);

		// UndergroundZone owns modeled dungeon entrance coordinates. These aliases power Finder;
		// MapMarkerRenderer draws the matching interactive layer symbol, so no second icon is added.
		for (UndergroundZone zone : UndergroundZone.ALL_ZONES)
		{
			addSearchPoi(new Poi(
				zone.getSurfacePoint().getX(),
				zone.getSurfacePoint().getY(),
				zone.getSurfacePoint().getPlane(),
				"dungeon",
				zone.getName()));
		}

		for (Poi p : PoiDetails.getAllPois())
		{
			if (isOneTileDungeonDuplicate(p))
			{
				continue;
			}
			final boolean searchable = !isModeledDungeonInterior(p);
			final long key = packedPoint(p.plane, p.x, p.y);
			if (existingPoints.add(key))
			{
				addPoi(p, searchable);
			}
			else if (searchable)
			{
				// Keep a meaningful curated name searchable without rendering another icon at the
				// same tile. The earlier source remains the single visible marker.
				addSearchPoi(p);
			}
		}

		// Decode on the loading thread; rendering only looks up prepared images.
		for (String key : byKey.keySet())
		{
			if (Thread.currentThread().isInterrupted())
			{
				return;
			}
			loadIcon(key);
		}

		loaded = true;
		log.debug("Loaded {} points of interest into index", pois.size());
	}

	/** The curated dungeon source is one tile west of its matching wiki entrance marker. */
	private boolean isOneTileDungeonDuplicate(Poi poi)
	{
		if (!isDungeonPoi(poi) || poi.y > InstanceMaps.GAP_MIN_Y)
		{
			return false;
		}

		for (Poi existing : pois)
		{
			if (isDungeonPoi(existing) && existing.plane == poi.plane
				&& existing.x == poi.x + 1 && existing.y == poi.y)
			{
				return true;
			}
		}
		return false;
	}

	private static boolean isDungeonPoi(Poi poi)
	{
		return "dungeon".equals(poi.key) || "dungeon_link".equals(poi.key) || "basement".equals(poi.key);
	}

	/** Modeled interiors render on their layer, but Finder must target the canonical surface alias. */
	private static boolean isModeledDungeonInterior(Poi poi)
	{
		if (!isDungeonPoi(poi) || poi.y <= InstanceMaps.GAP_MIN_Y)
		{
			return false;
		}
		for (UndergroundZone zone : UndergroundZone.ALL_ZONES)
		{
			if (zone.getName().equalsIgnoreCase(poi.name))
			{
				return true;
			}
		}
		return false;
	}

	private void readTsv(InputStream source, Set<Long> existingPoints)
	{
		readTsv(source, existingPoints, false);
	}

	private void readTsv(InputStream source, Set<Long> existingPoints, boolean bankSupplement)
	{
		if (source == null)
		{
			return;
		}

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(source, StandardCharsets.UTF_8)))
		{
			String line;
			while ((line = reader.readLine()) != null)
			{
				if (line.isEmpty() || line.charAt(0) == '#')
				{
					continue;
				}

				final String[] parts = line.split("\t", 5);
				if (parts.length < 5)
				{
					continue;
				}

				try
				{
					final Poi poi = new Poi(
						Integer.parseInt(parts[0]),
						Integer.parseInt(parts[1]),
						Integer.parseInt(parts[2]),
						parts[3],
						parts[4]);
					final long key = packedPoint(poi.plane, poi.x, poi.y);
					if (bankSupplement && hasNearbyBank(poi))
					{
						addSearchPoi(poi);
						continue;
					}
					if (!existingPoints.add(key))
					{
						if (bankSupplement)
						{
							addSearchPoi(poi);
						}
						continue;
					}
					addPoi(poi);
				}
				catch (NumberFormatException ignored)
				{
					// Skip a malformed row rather than losing the whole index.
				}
			}
		}
		catch (IOException e)
		{
			log.warn("Could not read {}", source, e);
		}
	}

	private boolean hasNearbyBank(Poi candidate)
	{
		for (Poi poi : pois)
		{
			if ("bank".equals(poi.key) && poi.plane == candidate.plane
				&& Math.abs(poi.x - candidate.x) <= 2 && Math.abs(poi.y - candidate.y) <= 2)
			{
				return true;
			}
		}
		return false;
	}

	private void addPoi(Poi poi)
	{
		addPoi(poi, true);
	}

	private void addPoi(Poi poi, boolean searchable)
	{
		pois.add(poi);
		if (searchable)
		{
			addSearchPoi(poi);
		}
		byKey.computeIfAbsent(poi.key, k -> new ArrayList<>()).add(poi);
		chunkMap.computeIfAbsent(chunkKey(poi.plane, poi.x >> 6, poi.y >> 6), k -> new ArrayList<>()).add(poi);
	}

	private void addSearchPoi(Poi poi)
	{
		final String identity = poi.plane + "\t" + poi.x + "\t" + poi.y + "\t" + poi.key + "\t" + poi.name;
		if (searchIdentities.add(identity))
		{
			searchPois.add(poi);
		}
	}

	private static long packedPoint(int plane, int x, int y)
	{
		return (((long) plane & 0x3L) << 32) | (((long) x & 0xFFFFL) << 16) | ((long) y & 0xFFFFL);
	}

	public synchronized boolean isLoaded()
	{
		return loaded;
	}

	public synchronized int size()
	{
		return pois.size();
	}

	public synchronized List<Poi> all()
	{
		return Collections.unmodifiableList(pois);
	}

	/**
	 * Points of interest grouped by icon, ordered by display name. This is what the legend lists,
	 * and what turns "clothes shop" into the set of every clothes shop on the map.
	 */
	public synchronized Map<String, List<Poi>> groups()
	{
		final List<Map.Entry<String, List<Poi>>> entries = new ArrayList<>(byKey.entrySet());
		entries.sort(Comparator.comparing(e -> e.getValue().get(0).getName().toLowerCase()));

		final Map<String, List<Poi>> ordered = new LinkedHashMap<>();
		for (Map.Entry<String, List<Poi>> entry : entries)
		{
			ordered.put(entry.getKey(), new ArrayList<>(entry.getValue()));
		}
		return ordered;
	}

	/** The icon image for a group, or null when it was not prefetched. */
	public synchronized BufferedImage icon(String key)
	{
		if (!icons.containsKey(key))
		{
			return loadIcon(key);
		}
		return icons.get(key);
	}

	private BufferedImage loadIcon(String key)
	{
		if (icons.containsKey(key))
		{
			return icons.get(key);
		}

		BufferedImage image = null;
		final String iconKey = "basement".equals(key) ? "dungeon_link" : key;
		try (InputStream in = open(tileDir, "icons/" + iconKey + ".png"))
		{
			if (in != null)
			{
				synchronized (ImageIO.class)
				{
					image = ImageIO.read(in);
				}
			}
		}
		catch (IOException e)
		{
			log.debug("Unreadable icon {}", key, e);
		}

		// Cache the miss too, so a missing icon is not re-read every frame.
		icons.put(key, image);
		return image;
	}

	public synchronized Poi nearest(int worldX, int worldY, int plane)
	{
		return nearest(worldX, worldY, plane, MATCH_RADIUS);
	}

	/**
	 * The POI in the {@code key} icon group (e.g. {@code "bank"}) nearest to {@code (fromX, fromY)}
	 * by straight-line distance across every plane, or null when the group is empty. Cheaper than
	 * {@link #groups()}, which sorts every group.
	 */
	public synchronized Poi nearestByKey(String key, int fromX, int fromY)
	{
		final List<Poi> group = byKey.get(key);
		if (group == null || group.isEmpty())
		{
			return null;
		}
		Poi best = group.get(0);
		long bestDist = Long.MAX_VALUE;
		for (Poi poi : group)
		{
			final long dx = poi.getX() - fromX;
			final long dy = poi.getY() - fromY;
			final long d = dx * dx + dy * dy;
			if (d < bestDist)
			{
				bestDist = d;
				best = poi;
			}
		}
		return best;
	}

	/**
	 * The named point of interest closest to this position, or null if nothing is near. The
	 * radius is in game tiles, so callers scale it with zoom to keep the hit area a constant
	 * size on screen.
	 */
	public synchronized Poi nearest(int worldX, int worldY, int plane, int radius)
	{
		final int minChunkX = (worldX - radius) >> 6;
		final int maxChunkX = (worldX + radius) >> 6;
		final int minChunkY = (worldY - radius) >> 6;
		final int maxChunkY = (worldY + radius) >> 6;

		Poi best = null;
		int bestDistance = Integer.MAX_VALUE;

		for (int cx = minChunkX; cx <= maxChunkX; cx++)
		{
			for (int cy = minChunkY; cy <= maxChunkY; cy++)
			{
				final List<Poi> chunkPois = chunkMap.get(chunkKey(plane, cx, cy));
				if (chunkPois == null)
				{
					continue;
				}

				for (Poi poi : chunkPois)
				{
					final int dx = Math.abs(poi.x - worldX);
					final int dy = Math.abs(poi.y - worldY);
					if (dx > radius || dy > radius)
					{
						continue;
					}

					final int distance = dx * dx + dy * dy;
					if (distance < bestDistance)
					{
						bestDistance = distance;
						best = poi;
					}
				}
			}
		}

		return best;
	}

	/**
	 * Returns all points of interest on the given plane within the specified world bounding box.
	 */
	public synchronized List<Poi> inArea(int plane, int minWorldX, int maxWorldX, int minWorldY, int maxWorldY)
	{
		final List<Poi> result = new ArrayList<>();
		forEachInArea(plane, minWorldX, maxWorldX, minWorldY, maxWorldY, result::add);
		return result;
	}

	/**
	 * Invokes {@code consumer} for every POI on {@code plane} inside the world box. Prefer this
	 * over {@link #inArea} on the overlay hot path — it skips the intermediate list allocation.
	 */
	public synchronized void forEachInArea(int plane, int minWorldX, int maxWorldX, int minWorldY, int maxWorldY,
		java.util.function.Consumer<Poi> consumer)
	{
		final int minChunkX = minWorldX >> 6;
		final int maxChunkX = maxWorldX >> 6;
		final int minChunkY = minWorldY >> 6;
		final int maxChunkY = maxWorldY >> 6;

		for (int cx = minChunkX; cx <= maxChunkX; cx++)
		{
			for (int cy = minChunkY; cy <= maxChunkY; cy++)
			{
				final List<Poi> chunkPois = chunkMap.get(chunkKey(plane, cx, cy));
				if (chunkPois == null)
				{
					continue;
				}

				for (Poi poi : chunkPois)
				{
					if (poi.x >= minWorldX && poi.x <= maxWorldX && poi.y >= minWorldY && poi.y <= maxWorldY)
					{
						consumer.accept(poi);
					}
				}
			}
		}
	}

	/** Names containing {@code query} (case-insensitive), at most {@code cap} of them. */
	public synchronized List<Poi> searchByName(String query, int cap)
	{
		final List<Poi> matches = new ArrayList<>();
		if (query == null || query.isEmpty() || cap <= 0)
		{
			return matches;
		}

		final String lower = query.toLowerCase(Locale.ROOT);
		for (Poi poi : searchPois)
		{
			if (poi.getName() != null && poi.getName().toLowerCase(Locale.ROOT).contains(lower))
			{
				matches.add(poi);
				if (matches.size() >= cap)
				{
					break;
				}
			}
		}

		return matches;
	}

	public static final class Poi
	{
		private final int x;
		private final int y;
		private final int plane;
		private final String key;
		private final String name;

		public Poi(int x, int y, int plane, String key, String name)
		{
			this.x = x;
			this.y = y;
			this.plane = plane;
			this.key = key;
			this.name = name;
		}

		public int getX()
		{
			return x;
		}

		public int getY()
		{
			return y;
		}

		public int getPlane()
		{
			return plane;
		}

		public String getKey()
		{
			return key;
		}

		public String getName()
		{
			return name;
		}
	}
}
