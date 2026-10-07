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

import atlasofgielinor.map.MapCatalogLoader;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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

import atlasofgielinor.data.io.BundledTsv;
import lombok.extern.slf4j.Slf4j;
import lombok.Getter;

/** Loads markers and search aliases from the normalized bundled map catalog. */
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
	private SpatialIndex<Poi> spatialIndex = new SpatialIndex<>(
		Collections.emptyList(), Poi::getPlane, Poi::getX, Poi::getY);
	private final Map<String, BufferedImage> icons = new HashMap<>();

	private File tileDir;
	private boolean loaded;
	@Getter
	private volatile long dataVersion;


	/** Root of the bundled POI data inside the jar. */
	private static final String RESOURCE_ROOT = "/atlasofgielinor/poi/";

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

	public void load(File tileDir)
	{
		final PoiIndex prepared = new PoiIndex();
		prepared.loadData(tileDir);
		if (!MapCatalogLoader.isReady() || Thread.currentThread().isInterrupted())
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
			spatialIndex = prepared.spatialIndex;
			icons.clear();
			icons.putAll(prepared.icons);
			loaded = true;
			dataVersion++;
		}
	}

	private void loadData(File tileDir)
	{
		MapCatalogLoader.load();
		if (!MapCatalogLoader.isReady()) return;
		this.tileDir = tileDir;
		pois.clear();
		searchPois.clear();
		searchIdentities.clear();
		byKey.clear();
		icons.clear();
		loaded = false;

		// One authoritative row supplies marker identity, search aliases and visibility.
		for (MapCatalog.Location location : MapCatalog.current().locations.values())
		{
			final Poi poi = new Poi(location.x, location.y, location.plane, location.iconKey, location.label, location.id);
			if (location.rendered) addPoi(poi, location.searchable);
			else if (location.searchable) addSearchPoi(poi);
			if (location.searchable)
				for (String alias : location.aliases)
					addSearchPoi(new Poi(location.x, location.y, location.plane, location.iconKey, alias, location.id));
		}

		try
		{
			for (String[] row : BundledTsv.read("/atlasofgielinor/poi/display-offsets.tsv", 6))
			{
				final int x = Integer.parseInt(row[1]), y = Integer.parseInt(row[2]), plane = Integer.parseInt(row[3]);
				final java.awt.geom.Point2D point = new java.awt.geom.Point2D.Double(
					x + 0.5 + Double.parseDouble(row[4]), y + 0.5 + Double.parseDouble(row[5]));
				for (int i = 0; i < pois.size(); i++)
				{
					final Poi poi = pois.get(i);
					if (poi.x != x || poi.y != y || poi.plane != plane || !poi.key.equals(row[0])) continue;
					final Poi offset = poi.withDisplayPoint(point, false);
					pois.set(i, offset);
					final List<Poi> group = byKey.get(poi.key);
					group.set(group.indexOf(poi), offset);
				}
			}
		}
		catch (IOException | IllegalArgumentException e)
		{
			log.debug("Unreadable POI display offsets", e);
		}

		spatialIndex = new SpatialIndex<>(pois, Poi::getPlane, Poi::getX, Poi::getY);

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

	/** Supplementary sources often place the same icon a few tiles from its native centre. */

	/** Modeled interiors render on their layer, but Finder must target the canonical surface alias. */

	private void addPoi(Poi poi, boolean searchable)
	{
		pois.add(poi);
		if (searchable)
		{
			addSearchPoi(poi);
		}
		byKey.computeIfAbsent(poi.key, k -> new ArrayList<>()).add(poi);
	}

	private void addSearchPoi(Poi poi)
	{
		final String searchKey = Set.of("dungeon", "dungeon_link", "basement").contains(poi.key) ? "dungeon" : poi.key;
		final String identity = poi.plane + "\t" + poi.x + "\t" + poi.y + "\t" + searchKey + "\t" + poi.name;
		if (searchIdentities.add(identity))
		{
			searchPois.add(poi);
		}
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

	/** Search targets, including aliases that intentionally have no second rendered icon. */
	public synchronized List<Poi> searchEntries()
	{
		return Collections.unmodifiableList(searchPois);
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
		return spatialIndex.nearest(plane, worldX, worldY, radius, null);
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
		spatialIndex.forEachInArea(plane, minWorldX, maxWorldX, minWorldY, maxWorldY, consumer);
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
		@Getter
		private final int x;
		@Getter
		private final int y;
		@Getter
		private final int plane;
		@Getter
		private final String key;
		@Getter
		private final String name;
		@Getter
		private final String locationId;
		@Getter
		private java.awt.geom.Point2D displayPoint;
		@Getter
		private boolean nativeLayout;

		public Poi withDisplayPoint(java.awt.geom.Point2D point, boolean nativeLayout)
		{
			final Poi projected = new Poi(x, y, plane, key, name, locationId);
			projected.displayPoint = point;
			projected.nativeLayout = nativeLayout;
			return projected;
		}

		public Poi(int x, int y, int plane, String key, String name)
		{
			this(x, y, plane, key, name, canonicalLocationId(x, y, plane, key));
		}

		private static String canonicalLocationId(int x, int y, int plane, String key)
		{
			MapCatalog.Location location = MapCatalog.current().locationAt(x, y, plane, key);
			if (location == null && Set.of("dungeon", "dungeon_link", "basement").contains(key))
			{
				for (String related : List.of("dungeon", "dungeon_link", "basement"))
				{
					location = MapCatalog.current().locationAt(x, y, plane, related);
					if (location != null) break;
				}
			}
			return location == null ? null : location.id;
		}

		Poi(int x, int y, int plane, String key, String name, String locationId)
		{
			this.x = x;
			this.y = y;
			this.plane = plane;
			this.key = key;
			this.name = name;
			this.locationId = locationId;
		}
	}
}
