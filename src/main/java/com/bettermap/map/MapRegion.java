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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import lombok.extern.slf4j.Slf4j;

/**
 * Stable identifiers for Finder places, with metadata loaded from {@code data/regions.json.gz}.
 * Centres match cache map labels; curated boxes can overlap. The smallest containing box wins,
 * with distance to centre breaking ties. Only surface coordinates are classified.
 */
@Slf4j
public enum MapRegion
{
	VARROCK,
	GRAND_EXCHANGE,
	LUMBRIDGE,
	DRAYNOR_VILLAGE,
	EDGEVILLE,
	BARBARIAN_VILLAGE,
	AL_KHARID,
	FALADOR,
	PORT_SARIM,
	RIMMINGTON,
	TAVERLEY,
	BURTHORPE,
	ICE_MOUNTAIN,
	ENTRANA,
	CATHERBY,
	SEERS_VILLAGE,
	CAMELOT,
	EAST_ARDOUGNE,
	WEST_ARDOUGNE,
	YANILLE,
	HEMENSTER,
	WITCHAVEN,
	TREE_GNOME_STRONGHOLD,
	PISCATORIS,
	RELLEKKA,
	KELDAGRIM,
	NEITIZNOT,
	JATIZSO,
	MISCELLANIA,
	LUNAR_ISLE,
	WEISS,
	TROLLHEIM,
	TROLL_COUNTRY,
	BRIMHAVEN,
	MUSA_POINT,
	TAI_BWO_WANNAI,
	SHILO_VILLAGE,
	POLLNIVNEACH,
	NARDAH,
	SOPHANEM,
	MENAPHOS,
	CANIFIS,
	PORT_PHASMATYS,
	DARKMEYER,
	MEIYERDITCH,
	BURGH_DE_ROTT,
	MORTTON,
	SLEPE,
	VER_SINHAZA,
	FOSSIL_ISLAND,
	PRIFDDINAS,
	LLETYA,
	HOSIDIUS,
	SHAYZIEN,
	LOVAKENGJ,
	ARCEUUS,
	PORT_PISCARILIUS,
	KOUREND_CASTLE,
	CIVITAS_ILLA_FORTIS,
	ALDARIN,
	AUBURNVALE,
	FELDIP_HILLS,
	GU_TANOTH,
	JIGGIG,
	CORSAIR_COVE,
	ISLE_OF_SOULS,
	APE_ATOLL,
	MOS_LE_HARMLESS,
	WILDERNESS,
	MISTHALIN,
	ASGARNIA,
	KANDARIN,
	KARAMJA,
	KHARIDIAN_DESERT,
	FREMENNIK_PROVINCE,
	TIRANNWN_REGION,
	GREAT_KOUREND,
	VARLAMORE_REGION;

	/** Top-level grouping used in Finder descriptions. */
	public enum Kingdom
	{
		MISTHALIN,
		ASGARNIA,
		KANDARIN,
		FREMENNIK,
		KARAMJA,
		DESERT,
		MORYTANIA,
		TIRANNWN,
		KOUREND,
		VARLAMORE,
		ISLANDS,
		WILDERNESS;

		public String getDisplayName()
		{
			final String name = kingdomNames.get(this);
			if (name == null) throw new IllegalStateException("Region catalog is not loaded");
			return name;
		}
	}

	private static final String RESOURCE = "/com/bettermap/data/regions.json.gz";
	private static volatile Map<MapRegion, Detail> details = Collections.emptyMap();
	private static volatile Map<Kingdom, String> kingdomNames = Collections.emptyMap();
	private static volatile Set<String> placeNames = Collections.emptySet();

	public static boolean isLoaded()
	{
		return !details.isEmpty();
	}

	/** Call on the startup worker before Finder input or map rendering is enabled. */
	public static synchronized void load()
	{
		if (isLoaded()) return;
		final Map<MapRegion, Detail> parsed = new EnumMap<>(MapRegion.class);
		final Map<Kingdom, String> names = new EnumMap<>(Kingdom.class);
		final Set<String> places = new HashSet<>();
		try (InputStream raw = MapRegion.class.getResourceAsStream(RESOURCE))
		{
			if (raw == null) throw new IOException("Missing bundled region catalog: " + RESOURCE);
			try (InputStreamReader reader = new InputStreamReader(new GZIPInputStream(raw), StandardCharsets.UTF_8))
			{
				final JsonObject dataset = new JsonParser().parse(reader).getAsJsonObject();
				for (JsonElement element : dataset.getAsJsonArray("kingdoms"))
				{
					if (Thread.currentThread().isInterrupted()) return;
					final JsonObject row = element.getAsJsonObject();
					final Kingdom kingdom = Kingdom.valueOf(row.get("id").getAsString());
					final String name = row.get("displayName").getAsString();
					if (name.isEmpty() || names.put(kingdom, name) != null)
						throw new IOException("Invalid kingdom: " + kingdom.name());
					places.add(name.toLowerCase(Locale.ROOT));
				}
				for (JsonElement element : dataset.getAsJsonArray("regions"))
				{
					if (Thread.currentThread().isInterrupted()) return;
					final JsonObject row = element.getAsJsonObject();
					final MapRegion region = valueOf(row.get("id").getAsString());
					final Detail detail = new Detail(row);
					if (parsed.put(region, detail) != null)
						throw new IOException("Duplicate region: " + region.name());
					places.add(detail.displayName.toLowerCase(Locale.ROOT));
				}
			}
			if (parsed.size() != values().length || names.size() != Kingdom.values().length)
				throw new IOException("Incomplete bundled region catalog");
			if (Thread.currentThread().isInterrupted()) return;
			kingdomNames = Collections.unmodifiableMap(names);
			placeNames = Collections.unmodifiableSet(places);
			details = Collections.unmodifiableMap(parsed);
		}
		catch (IOException | RuntimeException e)
		{
			log.warn("Could not load bundled region catalog", e);
		}
	}

	/** Cached region and kingdom names used by the map label filter. Expects lowercase text. */
	public static boolean isPlaceName(String name)
	{
		return placeNames.contains(name);
	}

	public String getDisplayName()
	{
		return detail().displayName;
	}

	public Kingdom getKingdom()
	{
		return detail().kingdom;
	}

	public int getCenterX()
	{
		return detail().centerX;
	}

	public int getCenterY()
	{
		return detail().centerY;
	}

	private Detail detail()
	{
		final Detail detail = details.get(this);
		if (detail == null) throw new IllegalStateException("Region catalog is not loaded");
		return detail;
	}

	private static final class Detail
	{
		private final String displayName;
		private final Kingdom kingdom;
		private final int centerX;
		private final int centerY;
		private final int halfWidth;
		private final int halfHeight;
		private final boolean fallback;

		private Detail(JsonObject row)
		{
			displayName = row.get("displayName").getAsString();
			kingdom = Kingdom.valueOf(row.get("kingdom").getAsString());
			centerX = row.get("centerX").getAsInt();
			centerY = row.get("centerY").getAsInt();
			halfWidth = row.get("halfWidth").getAsInt();
			halfHeight = row.get("halfHeight").getAsInt();
			fallback = row.get("fallback").getAsBoolean();
			if (displayName.isEmpty() || halfWidth <= 0 || halfHeight <= 0)
				throw new IllegalArgumentException("Invalid region bounds or name: " + displayName);
		}
	}

	/**
	 * True for the kingdom-scale catch-all boxes. Their label points sit inside their own cities —
	 * the "Asgarnia" label is drawn on top of Falador — so unlike a city box a fallback is not
	 * expected to own its own centre. It exists to give a name to everything the cities miss.
	 */
	public boolean isFallback()
	{
		return detail().fallback;
	}


	public int getMinX()
	{
		return getCenterX() - detail().halfWidth;
	}

	public int getMaxX()
	{
		return getCenterX() + detail().halfWidth;
	}

	public int getMinY()
	{
		return getCenterY() - detail().halfHeight;
	}

	public int getMaxY()
	{
		return getCenterY() + detail().halfHeight;
	}

	/** Box area in tiles, the primary tiebreak in {@link #of(int, int, int)}. */
	public long area()
	{
		return (long) (2 * detail().halfWidth) * (2 * detail().halfHeight);
	}

	public boolean contains(int x, int y)
	{
		return x >= getMinX() && x <= getMaxX() && y >= getMinY() && y <= getMaxY();
	}

	private long centreDistanceSq(int x, int y)
	{
		final long dx = x - getCenterX();
		final long dy = y - getCenterY();
		return dx * dx + dy * dy;
	}

	/**
	 * The region a world point belongs to, or {@code null} for anywhere uncurated.
	 *
	 * <p>Smallest containing box wins, so nesting resolves to the most specific name; equal-sized
	 * overlapping boxes fall back to whichever centre is nearer.
	 *
	 * <p>Only the surface plane is classified. Dungeon and upper-floor coordinates do not share the
	 * surface's frame of reference, so a surface box would claim them wrongly — better to report
	 * "no region" than a confidently wrong one.
	 */
	public static MapRegion of(int x, int y, int plane)
	{
		if (plane != 0)
		{
			return null;
		}

		MapRegion best = null;
		for (MapRegion region : values())
		{
			if (!region.contains(x, y))
			{
				continue;
			}
			if (best == null)
			{
				best = region;
				continue;
			}
			final long delta = region.area() - best.area();
			if (delta < 0 || (delta == 0 && region.centreDistanceSq(x, y) < best.centreDistanceSq(x, y)))
			{
				best = region;
			}
		}
		return best;
	}

	/** Case-insensitive lookup by display name, for tests and any future saved state. */
	public static MapRegion byName(String name)
	{
		if (name == null)
		{
			return null;
		}
		final String target = name.toLowerCase(Locale.ROOT);
		for (MapRegion region : values())
		{
			if (region.getDisplayName().toLowerCase(Locale.ROOT).equals(target))
			{
				return region;
			}
		}
		return null;
	}

	/**
	 * Every region, grouped by kingdom in declaration order. The browse list renders this directly,
	 * so kingdom order here is the order the user sees.
	 */
	public static List<MapRegion> inKingdomOrder()
	{
		final List<MapRegion> ordered = new ArrayList<>(values().length);
		for (Kingdom kingdom : Kingdom.values())
		{
			for (MapRegion region : values())
			{
				if (region.getKingdom() == kingdom)
				{
					ordered.add(region);
				}
			}
		}
		return Collections.unmodifiableList(ordered);
	}
}
