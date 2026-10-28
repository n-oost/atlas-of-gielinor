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
import atlasofgielinor.map.catalog.MapCatalog;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import atlasofgielinor.tiles.WikiMapTiles;
import net.runelite.api.coords.WorldPoint;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * The major underground zones and dungeons, each pairing a surface entrance with the coordinates
 * of its interior. The mapping runs both ways, which is what lets the map peek at a dungeon on
 * hover and swap to it on click.
 */
@Slf4j
public final class UndergroundZone
{
	private static final Map<String, UndergroundZone> entries = new LinkedHashMap<>();
	public static final UndergroundZone LUMBRIDGE_CELLAR = of("LUMBRIDGE_CELLAR");
	public static final UndergroundZone TAVERLEY_DUNGEON = of("TAVERLEY_DUNGEON");
	public static final UndergroundZone GOD_WARS_DUNGEON = of("GOD_WARS_DUNGEON");
	public static final UndergroundZone WATERBIRTH_DUNGEON = of("WATERBIRTH_DUNGEON");
	public static final UndergroundZone UNDERGROUND_PASS = of("UNDERGROUND_PASS");
	public static final UndergroundZone SLAYER_TOWER = of("SLAYER_TOWER");
	public static final UndergroundZone PRIFDDINAS_UNDERGROUND = of("PRIFDDINAS_UNDERGROUND");
	public static final UndergroundZone TOMBS_OF_AMASCUT = of("TOMBS_OF_AMASCUT");
	public static final UndergroundZone ISLE_OF_SOULS_DUNGEON = of("ISLE_OF_SOULS_DUNGEON");
	public static final UndergroundZone PATERDOMUS_BASEMENT = of("PATERDOMUS_BASEMENT");
	public static final UndergroundZone CHASM_OF_TEARS = of("CHASM_OF_TEARS");
	public static final UndergroundZone ECTOFUNTUS_DUNGEON = of("ECTOFUNTUS_DUNGEON");
	public static final UndergroundZone GRIMSTONE_DUNGEON = of("GRIMSTONE_DUNGEON");
	public static final UndergroundZone PORT_SARIM_RAT_PITS = of("PORT_SARIM_RAT_PITS");
	public static final UndergroundZone DEEPFIN_MINE = of("DEEPFIN_MINE");
	public static final UndergroundZone NATIVE_BRAINDEATH_ISLAND = of("NATIVE_BRAINDEATH_ISLAND");

	private static final Map<String, String> PARENT_ZONES = Map.of(
		"DORGESH_KAAN_SOUTH", "DORGESH_KAAN",
		"CHASM_OF_TEARS", "LUMBRIDGE_SWAMP_CAVES",
		"NEYPOTZLI", "CAM_TORUM",
		"TARN_LAIR", "HAUNTED_MINE",
		"MOTHERLODE_MINE", "DWARVEN_MINES",
		"LASSAR_UNDERCITY", "CAMDOZAAL");

	private final String enumName;

	private UndergroundZone(String enumName)
	{
		this.enumName = enumName;
	}

	private static synchronized UndergroundZone of(String enumName)
	{
		return entries.computeIfAbsent(enumName, UndergroundZone::new);
	}

	public String name()
	{
		return enumName;
	}

	@Override
	public String toString()
	{
		return enumName;
	}

	public static UndergroundZone[] values()
	{
		return ALL_ZONES.toArray(new UndergroundZone[0]);
	}

	public static UndergroundZone valueOf(String enumName)
	{
		final UndergroundZone zone = entries.get(enumName);
		if (zone == null) throw new IllegalArgumentException("Unknown underground zone: " + enumName);
		return zone;
	}

	@AllArgsConstructor
	private static final class Metadata
	{
		private final String id;
		private final String name;
		private final WorldPoint surfacePoint;
		private final List<WorldPoint> surfacePoints;
		private final List<WorldPoint> entranceMarkerPoints;
		private final Set<Integer> hiddenEntranceMarkers;
		private final WorldPoint undergroundPoint;
		private final int yOffset;
		private final int radius;
		private final String description;
		private int clipMinX;
		private int clipMinY;
		private int clipMaxX;
		private int clipMaxY;
		private final int projectionDx;
		private final int projectionDy;
		private Integer sourceZoom;
	}

	@AllArgsConstructor
	private static final class Catalog
	{
		private final Map<UndergroundZone, Metadata> metadata;
		private final Map<String, UndergroundZone> byId;
		private final Map<UndergroundZone, List<UndergroundZone>> connections;
	}

	private static volatile Catalog catalog = new Catalog(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
	public static volatile List<UndergroundZone> ALL_ZONES = Collections.emptyList();
	private static final Map<String, String> SHARED_ZONE_IDS = Map.of(
		"edgeville_dungeon", "varrock_sewers", "crandor_dungeon", "karamja_dungeon");

	public static boolean isLoaded()
	{
		return !catalog.metadata.isEmpty();
	}

	/** Called on the startup worker before floor and instance indexes are built. */
	public static synchronized void load()
	{
		if (isLoaded())
		{
			return;
		}
		final Map<UndergroundZone, Metadata> prepared = new LinkedHashMap<>();
		final Map<String, UndergroundZone> ids = new HashMap<>();
		try
		{
			MapCatalog.load();
			for (String[] f : BundledTsv.read("/atlasofgielinor/dungeons/zones.tsv", 16))
			{
				if ((f.length - 16) % 3 != 0) throw new IOException("Invalid dungeon entrances");
				final UndergroundZone zone = of(f[0]);
				final WorldPoint originalSurface = point(f, 3);
				final int separator = f[1].indexOf("__");
				final MapCatalog.Location canonicalEntrance =
					MapCatalog.current().entrance(separator < 0 ? f[1] : f[1].substring(separator + 2));
				final WorldPoint surface = canonicalEntrance == null ? originalSurface : canonicalEntrance.point();
				final List<WorldPoint> entrances = new ArrayList<>();
				entrances.add(surface);
				for (int i = 16; i < f.length; i += 3) entrances.add(point(f, i));
				final Metadata metadata = new Metadata(f[1], f[2], surface, Collections.unmodifiableList(entrances),
					new ArrayList<>(entrances), new HashSet<>(),
					point(f, 6), Integer.parseInt(f[9]), Integer.parseInt(f[10]), f[11],
					Integer.parseInt(f[12]), Integer.parseInt(f[13]), Integer.parseInt(f[14]), Integer.parseInt(f[15]),
					Integer.parseInt(f[6]) - originalSurface.getX(), Integer.parseInt(f[7]) - originalSurface.getY(), null);
				if (prepared.put(zone, metadata) != null || ids.put(metadata.id, zone) != null)
				{
					throw new IOException("Duplicate dungeon catalog entry");
				}
			}
			addCanonicalZone(prepared, ids, DEEPFIN_MINE);
			addCanonicalZone(prepared, ids, GRIMSTONE_DUNGEON);
			addCanonicalZone(prepared, ids, ISLE_OF_SOULS_DUNGEON);
			addCanonicalZone(prepared, ids, GOD_WARS_DUNGEON);
			if (prepared.isEmpty()) throw new IOException("Incomplete dungeon catalog");

			for (String[] fields : BundledTsv.read("/atlasofgielinor/dungeons/clips.tsv", 5))
			{
				final UndergroundZone zone = ids.get(fields[0]);
				if (zone == null || fields.length != 5) throw new IOException("Invalid dungeon clip override");
				final Metadata metadata = prepared.get(zone);
				metadata.clipMinX = Integer.parseInt(fields[1]);
				metadata.clipMinY = Integer.parseInt(fields[2]);
				metadata.clipMaxX = Integer.parseInt(fields[3]);
				metadata.clipMaxY = Integer.parseInt(fields[4]);
				if (metadata.clipMaxX <= metadata.clipMinX || metadata.clipMaxY <= metadata.clipMinY)
					throw new IOException("Invalid dungeon clip bounds: " + fields[0]);
			}

			for (String[] fields : BundledTsv.read("/atlasofgielinor/dungeons/tile-zooms.tsv", 2))
			{
				if (fields.length != 2) throw new IOException("Invalid dungeon tile zoom row");
				final UndergroundZone zone = ids.get(fields[0]);
				if (zone == null) throw new IOException("Unknown dungeon tile zoom zone: " + fields[0]);
				final int zoom = Integer.parseInt(fields[1]);
				final Metadata metadata = prepared.get(zone);
				if (zoom < WikiMapTiles.MIN_ZOOM || zoom > WikiMapTiles.MAX_ZOOM || metadata.sourceZoom != null)
				{
					throw new IOException("Invalid or duplicate dungeon tile zoom: " + fields[0]);
				}
				metadata.sourceZoom = zoom;
			}

			for (String[] fields : BundledTsv.read("/atlasofgielinor/dungeons/entrance-markers.tsv", 5))
			{
				final UndergroundZone zone = ids.get(fields[0]);
				if (zone == null) throw new IOException("Unknown entrance marker zone: " + fields[0]);
				final Metadata metadata = prepared.get(zone);
				final int index = Integer.parseInt(fields[1]);
				if (index < 0 || index >= metadata.surfacePoints.size()) throw new IOException("Invalid entrance marker index");
				final WorldPoint original = metadata.surfacePoints.get(index);
				metadata.entranceMarkerPoints.set(index, new WorldPoint(Integer.parseInt(fields[2]),
					Integer.parseInt(fields[3]), original.getPlane()));
				if ("true".equals(fields[4])) metadata.hiddenEntranceMarkers.add(index);
				else if (!"false".equals(fields[4])) throw new IOException("Invalid entrance marker visibility");
			}

			final Map<UndergroundZone, List<UndergroundZone>> connections = new HashMap<>();
			for (String[] fields : BundledTsv.read("/atlasofgielinor/dungeons/dungeon-connections.tsv", 2))
			{
				final List<UndergroundZone> members = new ArrayList<>();
				for (String id : fields[1].split(","))
				{
					final UndergroundZone member = ids.get(id.trim());
					if (member == null) throw new IOException("Unknown connected dungeon: " + id);
					if (!members.contains(member)) members.add(member);
				}
				final List<UndergroundZone> connected = Collections.unmodifiableList(members);
				for (UndergroundZone member : members)
				{
					if (connections.put(member, connected) != null)
					{
						throw new IOException("Dungeon belongs to multiple toggle groups: " + prepared.get(member).id);
					}
				}
			}

			if (Thread.currentThread().isInterrupted()) return;
			catalog = new Catalog(Collections.unmodifiableMap(prepared), Collections.unmodifiableMap(ids), Collections.unmodifiableMap(connections));
			ALL_ZONES = List.copyOf(prepared.keySet());
		}
		catch (IOException | RuntimeException e)
		{
			log.debug("Could not load dungeon catalog", e);
		}
	}

	private static WorldPoint point(String[] fields, int offset)
	{
		return new WorldPoint(Integer.parseInt(fields[offset]), Integer.parseInt(fields[offset + 1]), Integer.parseInt(fields[offset + 2]));
	}

	/** Compatibility adapter: canonical endpoints never determine artwork translation. */
	private static void addCanonicalZone(Map<UndergroundZone, Metadata> prepared,
		Map<String, UndergroundZone> ids, UndergroundZone zone) throws IOException
	{
		final MapCatalog map = MapCatalog.current();
		final String id = zone.name().toLowerCase(java.util.Locale.ROOT);
		final MapCatalog.Place place = map.places.get(id);
		final MapCatalog.Location entrance = map.entrance(id);
		final MapCatalog.Layout layout = map.primaryPiece(id);
		if (place == null || entrance == null || layout == null) throw new IOException("Incomplete canonical dungeon: " + id);
		final List<WorldPoint> entrances = new ArrayList<>();
		for (MapCatalog.Location location : map.locations.values())
		{
			if (location.verified && location.placeId.equals(id) && "entrance".equals(location.role)) entrances.add(location.point());
		}
		WorldPoint interior = new WorldPoint((layout.minX + layout.maxX) / 2, (layout.minY + layout.maxY) / 2, layout.plane);
		// A single verified exit is a navigation endpoint; a rectangle centre is only an artwork reference.
		final List<MapCatalog.Location> exits = new ArrayList<>();
		for (MapCatalog.Location location : map.locations.values())
			if (location.verified && location.placeId.equals(id) && "exit".equals(location.role)) exits.add(location);
		if (exits.size() == 1) interior = exits.get(0).point();
		final int radius = Math.max(layout.maxX - layout.minX, layout.maxY - layout.minY) / 2 + 1;
		final Metadata metadata = new Metadata(id, place.name, entrance.point(), Collections.unmodifiableList(entrances),
			new ArrayList<>(entrances), new HashSet<>(), interior, -layout.dy, radius, place.details,
			layout.minX, layout.minY, layout.maxX, layout.maxY, -layout.dx, -layout.dy, null);
		if (prepared.put(zone, metadata) != null || ids.put(id, zone) != null) throw new IOException("Duplicate canonical dungeon: " + id);
	}

	private Metadata metadata()
	{
		final Metadata result = catalog.metadata.get(this);
		if (result == null) throw new IllegalStateException("Dungeon catalog has not been loaded");
		return result;
	}

	/** Source zoom override for bundled fallback tiles, otherwise the selected pack zoom. */
	public int sourceZoom(int tileZoom)
	{
		final Integer zoom = metadata().sourceZoom;
		return zoom != null ? zoom : tileZoom;
	}

	/** True when this zone carries its own clip box (the wiki boxes do not frame it well). */
	public boolean hasClipOverride()
	{
		return metadata().clipMaxX > metadata().clipMinX && metadata().clipMaxY > metadata().clipMinY;
	}

	public int getClipMinX()
	{
		return metadata().clipMinX;
	}

	public int getClipMinY()
	{
		return metadata().clipMinY;
	}

	public int getClipMaxX()
	{
		return metadata().clipMaxX;
	}

	public int getClipMaxY()
	{
		return metadata().clipMaxY;
	}

	public String getId()
	{
		return metadata().id;
	}

	public String getName()
	{
		return metadata().name;
	}

	public WorldPoint getSurfacePoint()
	{
		return metadata().surfacePoint;
	}

	/** Canonical entrance first, followed by explicit alternate surface entrances. */
	public List<WorldPoint> getSurfacePoints()
	{
		return metadata().surfacePoints;
	}

	/** Authored marker placement; navigation and duplicate filtering retain the original surface tiles. */
	public WorldPoint getEntranceMarkerPoint(int index)
	{
		if (index >= 0 && index < metadata().surfacePoints.size())
		{
			final WorldPoint nativePoint = metadata().surfacePoints.get(index);
			final MapCatalog.Location location = MapCatalog.current().locationAt(
				nativePoint.getX(), nativePoint.getY(), nativePoint.getPlane(), "dungeon_link");
			final MapCatalog.Layout marker = location == null ? null : MapCatalog.current().markerLayout(location.id);
			if (marker != null) return new WorldPoint(nativePoint.getX() + marker.dx, nativePoint.getY() + marker.dy, nativePoint.getPlane());
		}
		final List<WorldPoint> points = metadata().entranceMarkerPoints;
		return index >= 0 && index < points.size() ? points.get(index) : null;
	}

	public boolean isEntranceMarkerHidden(int index)
	{
		return metadata().hiddenEntranceMarkers.contains(index);
	}

	public WorldPoint nearestSurfacePoint(int worldX, int worldY)
	{
		WorldPoint nearest = metadata().surfacePoint;
		long nearestDistance = Long.MAX_VALUE;
		for (WorldPoint point : metadata().surfacePoints)
		{
			final long dx = (long) point.getX() - worldX;
			final long dy = (long) point.getY() - worldY;
			final long distance = dx * dx + dy * dy;
			if (distance < nearestDistance)
			{
				nearest = point;
				nearestDistance = distance;
			}
		}
		return nearest;
	}

	public WorldPoint getUndergroundPoint()
	{
		return metadata().undergroundPoint;
	}

	public int getYOffset()
	{
		return metadata().yOffset;
	}

	public int getDeltaX()
	{
		return metadata().projectionDx;
	}

	public int getDeltaY()
	{
		return metadata().projectionDy;
	}

	public int getOffsetX()
	{
		return getDeltaX();
	}

	public int getOffsetY()
	{
		return getDeltaY();
	}

	public int getRadius()
	{
		return metadata().radius;
	}

	public String getDescription()
	{
		return metadata().description;
	}

	public static String canonicalZoneId(String zoneId)
	{
		if (zoneId == null)
		{
			return null;
		}
		return SHARED_ZONE_IDS.getOrDefault(zoneId, zoneId);
	}

	public static boolean zonesMatch(String a, String b)
	{
		if (a == null || b == null)
		{
			return false;
		}
		if (a.equals(b))
		{
			return true;
		}
		return canonicalZoneId(a).equals(canonicalZoneId(b));
	}

	public String getCanonicalId()
	{
		return canonicalZoneId(metadata().id);
	}

	/** A native section shares its entrance control with the corresponding authored dungeon. */
	public String getSelectionId()
	{
		final String id = getId();
		final int separator = id.indexOf("__");
		return canonicalZoneId(separator < 0 ? id : id.substring(separator + 2));
	}

	/** Native roots without authored entrances retain a control when explicitly listed in the dataset. */
	public boolean hasEntranceToggle()
	{
		if (hasUnresolvedGeometry()) return false;
		return !getId().startsWith("native_") || catalog.connections.containsKey(this);
	}

	/** Retain explicit boss pieces, but do not infer ownership from a quarantined main layout. */
	public boolean hasUnresolvedGeometry()
	{
		return MapCatalog.current().hasUnresolvedGeometry(getSelectionId());
	}

	/** Dungeon entrances share one toggle path; their connected zones come from the bundled dataset. */
	public List<UndergroundZone> getConnectedZones()
	{
		final Catalog current = catalog;
		List<UndergroundZone> connected = current.connections.get(current.byId.get(getSelectionId()));
		if (connected == null) connected = current.connections.get(this);
		return connected == null ? Collections.singletonList(this) : connected;
	}

	public UndergroundZone getParentZone()
	{
		final String parent = PARENT_ZONES.get(enumName);
		return parent == null ? null : entries.get(parent);
	}

	public boolean isSubterranean()
	{
		return getParentZone() != null;
	}

	public static UndergroundZone byId(String id)
	{
		return id == null ? null : catalog.byId.get(id);
	}

	/**
	 * Finds the nearest underground zone anchored to this surface world point within maxDistance.
	 */
	public static UndergroundZone forSurfacePoint(int worldX, int worldY, int maxDistance)
	{
		UndergroundZone best = null;
		int bestDistSq = maxDistance * maxDistance;

		for (UndergroundZone zone : ALL_ZONES)
		{
			if (zone.getId().startsWith("native_") || !zone.hasEntranceToggle()) continue;
			for (WorldPoint surface : zone.getSurfacePoints())
			{
				final int dx = surface.getX() - worldX;
				final int dy = surface.getY() - worldY;
				final int distSq = dx * dx + dy * dy;
				if (distSq <= bestDistSq)
				{
					bestDistSq = distSq;
					best = zone;
				}
			}
		}

		return best;
	}

	/**
	 * Finds the nearest underground zone matching this lower plane dungeon world point within maxDistance.
	 */
	public static UndergroundZone forUndergroundPoint(int worldX, int worldY, int maxDistance)
	{
		UndergroundZone best = null;
		int bestDistSq = maxDistance * maxDistance;

		for (UndergroundZone zone : ALL_ZONES)
		{
			if (zone.getId().startsWith("native_") || zone.hasUnresolvedGeometry()) continue;
			final int dx = zone.getUndergroundPoint().getX() - worldX;
			final int dy = zone.getUndergroundPoint().getY() - worldY;
			final int distSq = dx * dx + dy * dy;
			if (distSq <= bestDistSq)
			{
				bestDistSq = distSq;
				best = zone;
			}
		}

		return best;
	}
}
