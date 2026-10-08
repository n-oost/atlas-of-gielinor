package atlasofgielinor.map.catalog;

import atlasofgielinor.data.io.BundledTsv;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.coords.WorldPoint;
import lombok.extern.slf4j.Slf4j;

/** Bundled canonical identities, native endpoints, directed links and independent visual layouts. */
@Slf4j
public final class MapCatalog
{
	private static final String ROOT = "/atlasofgielinor/catalog/";
	private static final Set<String> POI_KINDS = Set.of("unknown", "area", "island", "dungeon", "cave",
		"mooring", "bank", "shop", "mining_site", "fishing_spot", "woodcutting_site", "agility_course",
		"agility_shortcut", "hunter_site", "farming_patch", "quest_start", "gate", "stairs", "ladder",
		"boat", "teleport", "boss", "monster", "ground_item", "room", "salvage_site", "port", "stash_unit");
	private static volatile MapCatalog current = empty();
	public final Map<String, Poi> pois;
	public final Map<String, Place> places;
	public final Map<String, Location> locations;
	public final Map<String, Link> links;
	public final Map<String, Layout> layouts;
	private final Map<String, Location> byPoint;
	private final Map<String, Location> entrances;
	private final Map<String, Layout> pieceLayouts;
	private final Set<String> unresolvedGeometry;
	private final Map<String, Layout> markerLayouts;

	private MapCatalog(Map<String, Poi> pois, Map<String, Place> places, Map<String, Location> locations,
		Map<String, Link> links, Map<String, Layout> layouts) throws IOException
	{
		this.pois = Collections.unmodifiableMap(new LinkedHashMap<>(pois));
		this.places = Collections.unmodifiableMap(new LinkedHashMap<>(places));
		this.locations = Collections.unmodifiableMap(new LinkedHashMap<>(locations));
		this.links = Collections.unmodifiableMap(new LinkedHashMap<>(links));
		this.layouts = Collections.unmodifiableMap(new LinkedHashMap<>(layouts));
		byPoint = new HashMap<>();
		entrances = new HashMap<>();
		pieceLayouts = new HashMap<>();
		unresolvedGeometry = new HashSet<>();
		markerLayouts = new HashMap<>();
		for (Poi poi : pois.values())
		{
			Set<String> ancestors = new HashSet<>();
			for (Poi ancestor = poi; ancestor != null; ancestor = pois.get(ancestor.parentId))
			{
				check(ancestors.add(ancestor.id), "Cyclic POI containment: " + poi.id);
				check(ancestor.parentId.isEmpty() || pois.containsKey(ancestor.parentId), "Missing POI parent: " + ancestor.id);
			}
		}
		for (Place place : places.values())
		{
			Set<String> ancestors = new HashSet<>();
			for (Place ancestor = place; ancestor != null; ancestor = places.get(ancestor.parentId))
			{
				check(ancestors.add(ancestor.id), "Cyclic containment: " + place.id);
				check(ancestor.parentId.isEmpty() || places.containsKey(ancestor.parentId), "Missing parent: " + ancestor.id);
			}
		}
		for (Location location : locations.values())
		{
			check(location.placeId.isEmpty() || places.containsKey(location.placeId), "Missing POI place: " + location.id);
			check(Set.of("poi", "entrance", "exit", "interior", "mooring").contains(location.role), "Invalid role: " + location.id);
			check(location.plane >= 0 && location.plane <= 3 && location.x >= 0 && location.x <= 65535
				&& location.y >= 0 && location.y <= 65535, "Invalid point: " + location.id);
			check(byPoint.put(pointKey(location.x, location.y, location.plane, location.iconKey), location) == null,
				"Duplicate endpoint/icon: " + location.id);
		}
		for (Link link : links.values())
		{
			check(locations.containsKey(link.fromLocationId), "Missing link source: " + link.id);
			check(!link.toLocationId.isEmpty() || !link.toPlaceId.isEmpty(), "Missing destination: " + link.id);
			check(link.toLocationId.isEmpty() || locations.containsKey(link.toLocationId), "Missing landing: " + link.id);
			check(link.toPlaceId.isEmpty() || places.containsKey(link.toPlaceId), "Missing destination place: " + link.id);
			if (!link.toLocationId.isEmpty() && !link.toPlaceId.isEmpty())
				check(locations.get(link.toLocationId).placeId.equals(link.toPlaceId), "Conflicting destination: " + link.id);
		}
		for (Layout layout : layouts.values())
		{
			check(places.containsKey(layout.placeId), "Missing layout place: " + layout.id);
			check(layout.plane >= 0 && layout.plane <= 3, "Invalid layout plane: " + layout.id);
			check(layout.rotation >= 0 && layout.rotation < 360
				&& layout.flip >= 0 && layout.flip <= 3, "Invalid transform: " + layout.id);
			if ("marker".equals(layout.kind))
			{
				Location location = locations.get(layout.locationId);
				check(location != null && location.placeId.equals(layout.placeId) && location.plane == layout.plane,
					"Invalid marker endpoint: " + layout.id);
				check(layout.rotation == 0 && layout.flip == 0, "Marker rotation: " + layout.id);
			}
			else
			{
				check("piece".equals(layout.kind) && layout.minX != null && layout.minY != null
					&& layout.maxX != null && layout.maxY != null && layout.minX <= layout.maxX && layout.minY <= layout.maxY,
					"Invalid piece bounds: " + layout.id);
				check(layout.locationId.isEmpty(), "Piece cannot be an endpoint: " + layout.id);
				check(layout.layerId != null && layout.legacyPieceId != null && layout.legacyPieceId >= 0,
					"Missing renderer piece/layer ID: " + layout.id);
			}
		}
		final Set<Integer> rendererIds = new HashSet<>();
		final Set<String> markerLocations = new HashSet<>();
		for (Layout layout : layouts.values())
		{
			if (layout.legacyPieceId != null) check(rendererIds.add(layout.legacyPieceId), "Duplicate renderer piece: " + layout.id);
			if ("marker".equals(layout.kind))
			{
				check(markerLocations.add(layout.locationId), "Duplicate marker layout: " + layout.id);
				if (layout.verified) markerLayouts.put(layout.locationId, layout);
			}
		}
		for (Place place : places.values())
		{
			if (!place.primaryLocationId.isEmpty())
			{
				Location primary = locations.get(place.primaryLocationId);
				check(primary != null && primary.verified && primary.placeId.equals(place.id), "Invalid primary location: " + place.id);
				if ("entrance".equals(primary.role)) entrances.put(place.id, primary);
			}
			if (!place.primaryLayoutId.isEmpty())
			{
				Layout primary = layouts.get(place.primaryLayoutId);
				check(primary != null && primary.verified && "piece".equals(primary.kind) && primary.placeId.equals(place.id),
					"Invalid primary layout: " + place.id);
				pieceLayouts.put(place.id, primary);
			}
		}
		for (Layout layout : layouts.values())
			if (!layout.verified && "piece".equals(layout.kind) && !pieceLayouts.containsKey(layout.placeId))
				unresolvedGeometry.add(layout.placeId);
		final List<Layout> activePieces = new ArrayList<>();
		for (Layout layout : layouts.values())
		{
			if (!layout.verified || !"piece".equals(layout.kind)) continue;
			for (Layout other : activePieces)
			{
				if (layout.plane != other.plane || layout.maxX < other.minX || other.maxX < layout.minX
					|| layout.maxY < other.minY || other.maxY < layout.minY) continue;
				check(containsPlace(layout.placeId, other.placeId) || containsPlace(other.placeId, layout.placeId),
					"Conflicting native geometry ownership: " + layout.id + " / " + other.id);
			}
			activePieces.add(layout);
		}
	}

	private boolean containsPlace(String parentId, String childId)
	{
		for (String id = childId; !id.isEmpty(); id = places.get(id).parentId)
			if (id.equals(parentId)) return true;
		return false;
	}

	private static MapCatalog empty()
	{
		try { return new MapCatalog(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap()); }
		catch (IOException impossible) { throw new IllegalStateException(impossible); }
	}

	public static MapCatalog current() { return current; }
	public static boolean isLoaded() { return !current.pois.isEmpty(); }

	/** Publish only after all normalized tables and references validate. No disk overrides are accepted. */
	public static void load() throws IOException
	{
		if (isLoaded()) return;
		final List<String[]> identityRows = rows("pois", "id\tname\tkind\tparent_id\tdetail_title\tcategory\tdetails\tstatus\tsource_ref");
		final Set<String> identityIds = new HashSet<>();
		for (String[] f : identityRows) check(identityIds.add(f[0]), "Duplicate POI ID: " + f[0]);
		final Map<String, List<String>> aliases = relations("poi_aliases", "alias", identityIds);
		final Map<String, List<String>> tags = relations("poi_tags", "tag", identityIds);
		Map<String, Poi> pois = new LinkedHashMap<>();
		for (String[] f : identityRows)
			put(pois, f[0], new Poi(f, aliases.getOrDefault(f[0], Collections.emptyList()), tags.getOrDefault(f[0], Collections.emptyList())));
		Map<String, Place> places = new LinkedHashMap<>();
		for (String[] f : rows("places", "id\tprimary_location_id\tprimary_layout_id"))
			put(places, f[0], new Place(f, pois.get(f[0])));
		Map<String, Location> locations = new LinkedHashMap<>();
		for (String[] f : rows("locations", "id\tpoi_id\tx\ty\tplane\trole\ticon_key\trendered\tsearchable\tstatus\tsource_ref"))
			put(locations, f[0], new Location(f, pois.get(f[1])));
		Map<String, Link> links = new LinkedHashMap<>();
		for (String[] f : rows("links", "id\tfrom_location_id\tto_location_id\tto_place_id\ttype\tdetails\tstatus\tsource_ref"))
			put(links, f[0], new Link(f));
		Map<String, Layout> layouts = new LinkedHashMap<>();
		for (String[] f : rows("layouts", "id\tplace_id\tlocation_id\tkind\tplane\tmin_x\tmin_y\tmax_x\tmax_y\tdx\tdy\trotation\tflip\tlayer_id\tlegacy_piece_id\tstatus\tsource_ref"))
			put(layouts, f[0], new Layout(f));
		MapCatalog prepared = new MapCatalog(pois, places, locations, links, layouts);
		if (!Thread.currentThread().isInterrupted()) current = prepared;
	}

	private static List<String[]> rows(String table, String header) throws IOException
	{
		final String resource = ROOT + table + ".tsv";
		final byte[] bytes;
		try (InputStream stream = MapCatalog.class.getResourceAsStream(resource))
		{
			check(stream != null, "Missing canonical table: " + resource);
			bytes = stream.readAllBytes();
		}
		try
		{
			final byte[] hash = MessageDigest.getInstance("SHA-256").digest(bytes);
			final StringBuilder hex = new StringBuilder();
			for (byte b : hash) hex.append(String.format("%02x", b & 255));
			log.debug("Canonical map table {} SHA-256 {} (bundled)", resource, hex);
		}
		catch (NoSuchAlgorithmException e) { throw new IOException("SHA-256 unavailable", e); }
		List<String[]> rows = new ArrayList<>();
		BundledTsv.read(new InputStreamReader(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8), -1,
			(fields, lineNumber) -> rows.add(fields));
		check(!rows.isEmpty() && String.join("\t", rows.remove(0)).equals(header), "Invalid header: " + table);
		int columns = header.split("\t").length;
		for (String[] row : rows)
		{
			check(row.length == columns, "Invalid column count: " + table + "/" + row[0]);
			check(row[0].matches("[a-z][a-z0-9_]*"), "Invalid ID: " + row[0]);
			if (header.endsWith("status\tsource_ref"))
				check(Set.of("verified", "unresolved").contains(row[columns - 2]) && !row[columns - 1].isEmpty(),
					"Missing status/source: " + row[0]);
		}
		return rows;
	}

	private static Map<String, List<String>> relations(String table, String valueColumn, Set<String> identityIds) throws IOException
	{
		Map<String, List<String>> result = new HashMap<>();
		Set<String> pairs = new HashSet<>();
		for (String[] f : rows(table, "poi_id\t" + valueColumn))
		{
			check(identityIds.contains(f[0]), "Missing relation POI: " + table + "/" + f[0]);
			String value = text(f[1]);
			check(!value.isBlank() && pairs.add(f[0] + '\0' + value), "Empty or duplicate relation: " + table + "/" + f[0]);
			if ("tag".equals(valueColumn)) check(value.matches("[a-z][a-z0-9_]*"), "Invalid tag: " + value);
			result.computeIfAbsent(f[0], k -> new ArrayList<>()).add(value);
		}
		return result;
	}

	private static <T> void put(Map<String, T> table, String id, T value) throws IOException
	{
		check(table.putIfAbsent(id, value) == null, "Duplicate ID: " + id);
	}

	private static void check(boolean valid, String message) throws IOException
	{
		if (!valid) throw new IOException(message);
	}

	private static int integer(String value) throws IOException
	{
		try { return Integer.parseInt(value); }
		catch (NumberFormatException e) { throw new IOException("Invalid catalog integer: " + value, e); }
	}

	private static Integer optionalInteger(String value) throws IOException { return value.isEmpty() ? null : integer(value); }

	private static List<String> textList(String value, char separator) throws IOException
	{
		List<String> out = new ArrayList<>();
		StringBuilder text = new StringBuilder();
		for (int i = 0; i < value.length(); i++)
		{
			char c = value.charAt(i);
			if (c == '\\')
			{
				check(++i < value.length(), "Incomplete text escape");
				char escaped = value.charAt(i);
				check(escaped == '\\' || escaped == '|' || escaped == 'n' || escaped == 't', "Invalid text escape");
				c = escaped == 'n' ? '\n' : escaped == 't' ? '\t' : escaped;
			}
			else if (c == separator)
			{
				out.add(text.toString()); text.setLength(0); continue;
			}
			text.append(c);
		}
		if (text.length() > 0) out.add(text.toString());
		return Collections.unmodifiableList(out);
	}

	private static String text(String value) throws IOException { return String.join("", textList(value, '\0')); }
	private static String pointKey(int x, int y, int plane, String icon) { return x + ":" + y + ":" + plane + ":" + icon; }
	public Location locationAt(int x, int y, int plane, String icon) { return byPoint.get(pointKey(x, y, plane, icon)); }
	public Location entrance(String placeId) { return entrances.get(placeId); }
	public Layout primaryPiece(String placeId) { return pieceLayouts.get(placeId); }
	public Layout markerLayout(String locationId) { return markerLayouts.get(locationId); }
	public boolean hasUnresolvedGeometry(String placeId)
	{
		return unresolvedGeometry.contains(placeId);
	}

	public String title(Location location)
	{
		return location.label;
	}

	public List<String> detailLines(Location location)
	{
		List<String> lines = new ArrayList<>();
		if (!location.details.isEmpty()) Collections.addAll(lines, location.details.split("\n"));
		for (Link link : links.values())
			if (link.verified && link.fromLocationId.equals(location.id) && !link.details.isEmpty())
				Collections.addAll(lines, link.details.split("\n"));
		return Collections.unmodifiableList(lines);
	}

	/** An identity can have several native locations. Imported identities are kept separate until evidenced. */
	public static final class Poi
	{
		public final String id, name, kind, parentId, detailTitle, category, details, sourceRef;
		public final List<String> aliases, tags;
		public final boolean verified;
		private Poi(String[] f, List<String> aliases, List<String> tags) throws IOException
		{
			id = f[0]; name = text(f[1]); kind = f[2]; parentId = f[3];
			detailTitle = text(f[4]); category = text(f[5]); details = text(f[6]); verified = "verified".equals(f[7]); sourceRef = f[8];
			this.aliases = Collections.unmodifiableList(new ArrayList<>(aliases));
			this.tags = Collections.unmodifiableList(new ArrayList<>(tags));
			check(!name.isBlank() && !detailTitle.isBlank() && POI_KINDS.contains(kind), "Missing POI name or invalid semantic type: " + id);
		}
	}

	/** Compatibility view: area identity belongs to pois.tsv; only default selections live in places.tsv. */
	public static final class Place
	{
		public final String id, name, kind, parentId, primaryLocationId, primaryLayoutId, details, sourceRef;
		public final List<String> aliases, tags;
		public final boolean verified;
		private Place(String[] f, Poi poi) throws IOException
		{
			check(poi != null, "Missing area POI: " + f[0]);
			id = poi.id; name = poi.name; kind = poi.kind; parentId = poi.parentId;
			primaryLocationId = f[1]; primaryLayoutId = f[2];
			aliases = poi.aliases; tags = poi.tags; details = poi.details; verified = poi.verified; sourceRef = poi.sourceRef;
		}
	}

	/** Joined compatibility view for rendering and search; fields are never separately authored here. */
	public static final class Location
	{
		public final String id, poiId, placeId, role, label, detailTitle, category, details, iconKey, sourceRef;
		public final List<String> tags, aliases;
		public final int x, y, plane;
		public final boolean verified, rendered, searchable;
		private Location(String[] f, Poi poi) throws IOException
		{
			check(poi != null, "Missing location POI: " + f[0]);
			id = f[0]; poiId = poi.id; placeId = poi.parentId;
			x = integer(f[2]); y = integer(f[3]); plane = integer(f[4]); role = f[5]; iconKey = f[6];
			label = poi.name; detailTitle = poi.detailTitle; category = poi.category; details = poi.details;
			tags = poi.tags; aliases = poi.aliases;
			check(Set.of("true", "false").contains(f[7]) && Set.of("true", "false").contains(f[8]), "Invalid POI visibility: " + id);
			rendered = Boolean.parseBoolean(f[7]); searchable = Boolean.parseBoolean(f[8]);
			verified = "verified".equals(f[9]); sourceRef = f[10];
			check(!iconKey.isBlank(), "Missing POI icon: " + id);
		}
		public WorldPoint point() { return new WorldPoint(x, y, plane); }
	}

	public static final class Link
	{
		public final String id, fromLocationId, toLocationId, toPlaceId, type, details, sourceRef;
		public final boolean verified;
		private Link(String[] f) throws IOException
		{
			id = f[0]; fromLocationId = f[1]; toLocationId = f[2]; toPlaceId = f[3]; type = f[4];
			details = text(f[5]); verified = "verified".equals(f[6]); sourceRef = f[7];
			check(!type.isEmpty(), "Missing link type: " + id);
		}
	}

	public static final class Layout
	{
		public final String id, placeId, locationId, kind, sourceRef;
		public final int plane, dx, dy, rotation, flip;
		public final Integer minX, minY, maxX, maxY, layerId, legacyPieceId;
		public final boolean verified;
		private Layout(String[] f) throws IOException
		{
			id = f[0]; placeId = f[1]; locationId = f[2]; kind = f[3]; plane = integer(f[4]);
			minX = optionalInteger(f[5]); minY = optionalInteger(f[6]); maxX = optionalInteger(f[7]); maxY = optionalInteger(f[8]);
			dx = integer(f[9]); dy = integer(f[10]); rotation = integer(f[11]); flip = integer(f[12]);
			layerId = optionalInteger(f[13]); legacyPieceId = optionalInteger(f[14]); verified = "verified".equals(f[15]); sourceRef = f[16];
		}
	}
}
