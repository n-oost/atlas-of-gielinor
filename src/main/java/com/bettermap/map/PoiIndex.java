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

import com.bettermap.data.DungeonPoiOverrides;
import com.bettermap.pathfinding.transport.parser.TransportRecord;
import com.bettermap.pathfinding.transport.parser.TsvParser;
import net.runelite.api.coords.WorldPoint;
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
import java.util.LinkedHashSet;
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
	 * Native icon rows win rendering ties, except labeled fairy rings. Distinct curated names remain
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

	private static final Set<Long> CONSOLIDATED_TELEPORT_TILES = Set.of(
		packedPoint(0, 1422, 2963), // POH portal (Varlamore)
		packedPoint(0, 1648, 3665), // Elise (Varlamore Diary)
		packedPoint(0, 2399, 5177), // TzHaar Fight Pit
		packedPoint(0, 2400, 5982), // Hallowed Sepulchre
		packedPoint(0, 2539, 4712), // Mage Arena Bank Lever (underground)
		packedPoint(0, 2659, 3627), // Thorodin (Fremennik Diary)
		packedPoint(0, 2824, 10168), // Dondakan (Between a Rock...)
		packedPoint(0, 2892, 3465), // POH portal (Taverley)
		packedPoint(0, 2952, 3224), // POH portal (Rimmington)
		packedPoint(0, 2978, 3347), // Sir Rebral (Falador Diary)
		packedPoint(0, 3090, 3956), // Mage Arena Bank Lever (surface)
		packedPoint(0, 3096, 3227), // Twiggy O'Korn (Draynor Diary)
		packedPoint(0, 3225, 3415), // Toby (Varrock Diary)
		packedPoint(0, 3234, 3214), // Hatius Cosaintus (Lumbridge Diary)
		packedPoint(0, 3239, 6077), // POH portal (Prifddinas)
		packedPoint(0, 3465, 3478), // Carl Le-sabrè (Desert Diary)
		packedPoint(0, 3683, 9888), // Ectofuntus Pool of Slime
		packedPoint(1, 2869, 2982)  // Duradel (Karamja Slayer Master)
	);

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
		MapData.load();
		if (!MapData.isReady()) return;
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
		readTsv(bundledPois != null ? bundledPois : open(tileDir, "pois.tsv"), existingPoints, false, false);
		readTsv(PoiIndex.class.getResourceAsStream(RESOURCE_ROOT + "pois-additions.tsv"), existingPoints);
		readTsv(PoiIndex.class.getResourceAsStream(RESOURCE_ROOT + "pois-legacy.tsv"), existingPoints);
		// Keep bank catalogue names searchable without drawing a second icon a tile or two away.
		readTsv(open(tileDir, "pois-banks.tsv"), existingPoints, true);
		// Cache labels fill gaps the native icon layer misses.
		readTsv(PoiIndex.class.getResourceAsStream(RESOURCE_ROOT + "pois-native-labels.tsv"), existingPoints);
		readTsv(open(tileDir, "pois-cache.tsv"), existingPoints);

		// UndergroundZone owns modeled dungeon entrance coordinates. These aliases power Finder;
		// MapMarkerRenderer draws the matching interactive layer symbol, so no second icon is added.
		for (UndergroundZone zone : UndergroundZone.ALL_ZONES)
		{
			if (zone.getId().contains("__") || zone.getId().startsWith("native_") || DungeonPoiOverrides.isButtonDeleted(zone, 0))
			{
				continue;
			}
			final WorldPoint p = DungeonPoiOverrides.getButtonPoint(zone, 0);
			addSearchPoi(new Poi(
				p.getX(),
				p.getY(),
				p.getPlane(),
				"dungeon",
				zone.getName()));
		}

		// Fairy rings use their named metadata point and the transportation (blue arrow) icon.
		// Remove the nearby generic cache marker before adding the labeled point.
		for (Poi p : PoiDetails.getAllPois())
		{
			if (!isFairyRing(p)
				|| DungeonPoiOverrides.isRawPoiDeleted(p.getName(), p.getX(), p.getY(), p.getPlane()))
			{
				continue;
			}
			final List<Poi> replaced = new ArrayList<>();
			for (Poi existing : pois)
			{
				if ("transportation".equals(existing.key) && nearby(existing, p, 3))
				{
					replaced.add(existing);
				}
			}
			for (Poi existing : replaced)
			{
				pois.remove(existing);
				byKey.get(existing.key).remove(existing);
				chunkMap.get(chunkKey(existing.plane, existing.x >> 6, existing.y >> 6)).remove(existing);
				existingPoints.remove(packedPoint(existing.plane, existing.x, existing.y));
			}
			if (existingPoints.add(packedPoint(p.plane, p.x, p.y)))
			{
				addPoi(p);
			}
		}

		for (Poi p : PoiDetails.getAllPois())
		{
			if (isFairyRing(p)
				|| DungeonPoiOverrides.isRawPoiDeleted(p.getName(), p.getX(), p.getY(), p.getPlane()))
			{
				continue;
			}
			final boolean searchable = !isModeledDungeonInterior(p);
			final long key = packedPoint(p.plane, p.x, p.y);
			if (hasNearbyPoi(p))
			{
				if (searchable)
				{
					addSearchPoi(p);
				}
			}
			else if (existingPoints.add(key))
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

		loadTeleportLocations();
		loadWorldMapSupplement();

		// Group 10 distinct overlap: offset Laughing Miner pub display position
		for (int i = 0; i < pois.size(); i++)
		{
			final Poi p = pois.get(i);
			if (p.getPlane() == 0 && p.getX() == 2914 && p.getY() == 10193 && "bar".equals(p.getKey()))
			{
				final Poi offsetPoi = p.withDisplayPoint(new java.awt.geom.Point2D.Double(2914.5 - 0.35, 10193.5), false);
				pois.set(i, offsetPoi);
				final List<Poi> byKeyList = byKey.get("bar");
				if (byKeyList != null)
				{
					final int idx = byKeyList.indexOf(p);
					if (idx >= 0) byKeyList.set(idx, offsetPoi);
				}
				final List<Poi> chunkList = chunkMap.get(chunkKey(0, 2914 >> 6, 10193 >> 6));
				if (chunkList != null)
				{
					final int idx = chunkList.indexOf(p);
					if (idx >= 0) chunkList.set(idx, offsetPoi);
				}
				break;
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

	private static boolean isFairyRing(Poi poi)
	{
		return "transportation".equals(poi.key) && poi.name.contains("Fairy Ring (");
	}

	private static boolean nearby(Poi a, Poi b, int radius)
	{
		return a.plane == b.plane && Math.abs(a.x - b.x) <= radius && Math.abs(a.y - b.y) <= radius;
	}

	/** Supplementary sources often place the same icon a few tiles from its native centre. */
	private boolean hasNearbyPoi(Poi poi)
	{
		for (Poi existing : pois)
		{
			if ("region_label".equals(poi.key) && !existing.name.equalsIgnoreCase(poi.name))
			{
				continue;
			}
			if ((existing.key.equals(poi.key) || (isDungeonPoi(existing) && isDungeonPoi(poi)))
				&& nearby(existing, poi, 3))
			{
				return true;
			}
		}
		// A moved dungeon button replaces the original entrance even when it moved more than
		// three tiles (for example Poison Waste). Match each original entrance to its override.
		if (isDungeonPoi(poi) && poi.y <= InstanceMaps.GAP_MIN_Y)
		{
			for (UndergroundZone zone : UndergroundZone.ALL_ZONES)
			{
				if (zone.getId().contains("__") || zone.getId().startsWith("native_")) continue;
				for (int i = 0; i < zone.getSurfacePoints().size(); i++)
				{
					final WorldPoint original = zone.getSurfacePoints().get(i);
					final WorldPoint moved = DungeonPoiOverrides.getButtonPoint(zone, i);
					if (original.getPlane() != poi.plane || moved.equals(original)
						|| Math.abs(original.getX() - poi.x) > 3 || Math.abs(original.getY() - poi.y) > 3)
					{
						continue;
					}
					for (Poi existing : pois)
					{
						if (isDungeonPoi(existing) && existing.plane == moved.getPlane()
							&& Math.abs(existing.x - moved.getX()) <= 3 && Math.abs(existing.y - moved.getY()) <= 3)
						{
							return true;
						}
					}
				}
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
		return isModeledDungeonName(poi);
	}

	private static boolean isModeledDungeonName(Poi poi)
	{
		for (UndergroundZone zone : UndergroundZone.ALL_ZONES)
		{
			if (zone.getId().contains("__") || zone.getId().startsWith("native_")) continue;
			if (zone.getName().equalsIgnoreCase(poi.name))
			{
				return true;
			}
		}
		return false;
	}

	/** Replace icon-type labels with a known dungeon name before indexing the marker. */
	private static Poi resolveDungeonName(Poi poi)
	{
		if (DungeonPoiOverrides.isRawPoiDeleted(poi.name, poi.x, poi.y, poi.plane)
			|| !isDungeonPoi(poi) || !("Dungeon with map link".equalsIgnoreCase(poi.name)
			|| "Dungeon".equalsIgnoreCase(poi.name)))
		{
			return poi;
		}

		String name = null;
		int bestDistance = MATCH_RADIUS * MATCH_RADIUS + 1;
		for (UndergroundZone zone : UndergroundZone.ALL_ZONES)
		{
			if (zone.getId().contains("__") || zone.getId().startsWith("native_")) continue;
			for (int i = 0; i < zone.getSurfacePoints().size(); i++)
			{
				final WorldPoint entrance = DungeonPoiOverrides.getButtonPoint(zone, i);
				final int dx = entrance.getX() - poi.x;
				final int dy = entrance.getY() - poi.y;
				final int distance = dx * dx + dy * dy;
				if (entrance.getPlane() == poi.plane && distance < bestDistance)
				{
					name = zone.getName();
					bestDistance = distance;
				}
			}
		}
		if (name == null)
		{
			final PoiDetails.Entry entry = PoiDetails.nearestEntry(poi.x, poi.y, poi.plane, 256, "dungeon");
			if (entry != null)
			{
				name = entry.title;
			}
		}
		return name == null ? poi : new Poi(poi.x, poi.y, poi.plane, poi.key, name);
	}

	private void readTsv(InputStream source, Set<Long> existingPoints)
	{
		readTsv(source, existingPoints, false);
	}

	private void readTsv(InputStream source, Set<Long> existingPoints, boolean bankSupplement)
	{
		readTsv(source, existingPoints, bankSupplement, true);
	}

	private void readTsv(InputStream source, Set<Long> existingPoints, boolean bankSupplement, boolean supplement)
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
					final Poi rawPoi = new Poi(
						Integer.parseInt(parts[0]),
						Integer.parseInt(parts[1]),
						Integer.parseInt(parts[2]),
						parts[3],
						parts[4]);
					final Poi poi = resolveDungeonName(rawPoi);
					final long key = packedPoint(poi.plane, poi.x, poi.y);
					if ((bankSupplement && hasNearbyBank(poi)) || (supplement && !bankSupplement && hasNearbyPoi(poi)))
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
					// Modeled dungeons already get a canonical surface search target below.
					addPoi(poi, !isModeledDungeonInterior(poi)
						&& !(poi != rawPoi && isModeledDungeonName(poi)));
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
		final String searchKey = isDungeonPoi(poi) ? "dungeon" : poi.key;
		final String identity = poi.plane + "\t" + poi.x + "\t" + poi.y + "\t" + searchKey + "\t" + poi.name;
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

	/** Loads the bundled teleport catalogue on the POI loading thread, grouped by landing tile. */
	private void loadTeleportLocations()
	{
		final Map<String, Set<String>> locations = new LinkedHashMap<>();
		final String[] sources = {
			"teleportation_spells", "teleportation_spells_home", "teleportation_items",
			"teleportation_minigames", "teleportation_portals", "teleportation_portals_poh",
			"teleportation_levers", "teleportation_boxes", "wilderness_obelisks"
		};
		for (String source : sources)
		{
			try (InputStream stream = PoiIndex.class.getResourceAsStream(
				"/com/bettermap/pathfinding/transports/" + source + ".tsv"))
			{
				if (stream == null) continue;
				final String contents = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
				for (TransportRecord record : new TsvParser().parse(contents))
				{
					String label = record.getDisplayInfo();
					if (label == null || label.isBlank())
					{
						label = source.contains("obelisks") ? "Wilderness obelisk"
							: source.contains("levers") ? "Teleport lever"
							: source.contains("boxes") ? "Teleport box" : "Teleport portal";
					}
					final String destination = record.getDestination();
					if (destination != null && !destination.isBlank())
					{
						locations.computeIfAbsent(destination.trim(), key -> new LinkedHashSet<>()).add(label);
					}
					final String origin = record.getOrigin();
					if (origin != null && !origin.isBlank())
					{
						locations.computeIfAbsent(origin.trim(), key -> new LinkedHashSet<>()).add(label);
					}
				}
			}
			catch (IOException e)
			{
				log.debug("Unreadable teleport catalogue {}", source, e);
			}
		}
		for (Map.Entry<String, Set<String>> entry : locations.entrySet())
		{
			final String[] point = entry.getKey().split("\\s+");
			if (point.length != 3) continue;
		try
			{
				final int x = Integer.parseInt(point[0]);
				final int y = Integer.parseInt(point[1]);
				final int plane = Integer.parseInt(point[2]);
				if (x <= 0 || y <= 0 || plane < 0 || plane > 3) continue;
				final String label = String.join(" | ", entry.getValue());
				final long packed = packedPoint(plane, x, y);
				if (CONSOLIDATED_TELEPORT_TILES.contains(packed))
				{
					addSearchPoi(new Poi(x, y, plane, "teleport", label));
					continue;
				}
				if (plane == 0 && x == 2914 && y == 10193)
				{
					addPoi(new Poi(x, y, plane, "teleport", label)
						.withDisplayPoint(new java.awt.geom.Point2D.Double(2914.5 + 0.35, 10193.5), false), false);
					continue;
				}
				addPoi(new Poi(x, y, plane, "teleport", label), false);
			}
			catch (NumberFormatException e)
			{
				log.debug("Invalid teleport point {}", entry.getKey());
			}
		}
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

	private void loadWorldMapSupplement()
	{
		for (WorldMapSupplement.Entry entry : WorldMapSupplement.entries())
		{
			final Poi supplement = entry.poi();
			if (DungeonPoiOverrides.isRawPoiDeleted(supplement.name, supplement.x, supplement.y, supplement.plane))
			{
				continue;
			}
			boolean covered = hasNearbyPoi(supplement);
			if (!covered)
			{
				// Curated aliases describe the same destination even when the vanilla icon is offset.
				final String identity = WorldMapSupplement.identity(supplement.name);
				for (Poi alias : searchPois)
				{
					if (!identity.isEmpty() && WorldMapSupplement.compatible(supplement.key, alias.key)
						&& nearby(alias, supplement, 16)
						&& identity.equals(WorldMapSupplement.identity(alias.name)))
					{
						covered = true;
						break;
					}
				}
			}
			if (!covered)
			{
				for (Poi existing : pois)
				{
					if ((WorldMapSupplement.compatible(supplement.key, existing.key)
						|| "teleport".equals(supplement.key)
							&& CONSOLIDATED_TELEPORT_TILES.contains(packedPoint(existing.plane, existing.x, existing.y)))
						&& nearby(existing, supplement, 3))
					{
						covered = true;
						break;
					}
				}
			}
			if (covered || isModeledDungeonInterior(supplement))
			{
				if (!isModeledDungeonName(supplement))
				{
					addSearchPoi(supplement);
				}
			}
			else
			{
				addPoi(supplement);
			}
		}
	}

	private BufferedImage loadIcon(String key)
	{
		if (icons.containsKey(key))
		{
			return icons.get(key);
		}

		BufferedImage image = null;
		final String iconKey = "teleport".equals(key) ? "house_portal"
			: "runecrafting_altar".equals(key) ? "altar"
			: "salvaging".equals(key) ? "cache_icon_4947"
			: "basement".equals(key) ? "dungeon_link" : key;
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
		private java.awt.geom.Point2D displayPoint;
		private boolean nativeLayout;

		public Poi withDisplayPoint(java.awt.geom.Point2D point, boolean nativeLayout)
		{
			final Poi projected = new Poi(x, y, plane, key, name);
			projected.displayPoint = point;
			projected.nativeLayout = nativeLayout;
			return projected;
		}

		public boolean isNativeLayout()
		{
			return nativeLayout;
		}

		public java.awt.geom.Point2D getDisplayPoint()
		{
			return displayPoint;
		}

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
