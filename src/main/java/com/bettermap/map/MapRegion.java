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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import lombok.Getter;

/**
 * The named places the Finder browses by — "Varrock", "Falador", "Hosidius".
 *
 * <p>The game cache dump in {@code poi/pois-cache.tsv} carries 854 distinct {@code region_label}
 * rows, but they are unusable as a menu: they mix kingdoms ("Kingdom of Misthalin"), cities
 * ("Varrock"), single rooms ("Jail", "Throne room", "Market") and outright noise ("A", "B",
 * "Anger", "Nothing interesting happens here"), and none of them carry bounds — only a point. So
 * this table is curated. The <b>centres are the cache's own label coordinates</b>, which is why
 * they line up with what the map draws; only the extents are authored here.
 *
 * <p>Boxes are allowed to overlap and to nest. {@link #of(int, int, int)} resolves a point to the
 * <b>smallest</b> box containing it, ties broken by distance to centre, so Grand Exchange wins over
 * Varrock and Varrock wins over Misthalin without any of them needing to be carved around each
 * other. Requiring a non-overlapping partition was tried and is not maintainable by hand — a single
 * tweak to one city silently pushes POIs out of its neighbour.
 */
public enum MapRegion
{
	// ---- Misthalin ----
	VARROCK("Varrock", Kingdom.MISTHALIN, 3211, 3450, 75, 75),
	GRAND_EXCHANGE("Grand Exchange", Kingdom.MISTHALIN, 3168, 3477, 30, 30),
	LUMBRIDGE("Lumbridge", Kingdom.MISTHALIN, 3239, 3234, 30, 70),
	DRAYNOR_VILLAGE("Draynor Village", Kingdom.MISTHALIN, 3120, 3267, 45, 45),
	EDGEVILLE("Edgeville", Kingdom.MISTHALIN, 3098, 3488, 40, 40),
	BARBARIAN_VILLAGE("Barbarian Village", Kingdom.MISTHALIN, 3063, 3416, 32, 32),
	AL_KHARID("Al Kharid", Kingdom.MISTHALIN, 3313, 3171, 60, 80),

	// ---- Asgarnia ----
	FALADOR("Falador", Kingdom.ASGARNIA, 2989, 3355, 70, 60),
	PORT_SARIM("Port Sarim", Kingdom.ASGARNIA, 3047, 3221, 40, 60),
	RIMMINGTON("Rimmington", Kingdom.ASGARNIA, 2960, 3222, 45, 45),
	TAVERLEY("Taverley", Kingdom.ASGARNIA, 2900, 3458, 55, 60),
	BURTHORPE("Burthorpe", Kingdom.ASGARNIA, 2895, 3549, 50, 50),
	ICE_MOUNTAIN("Ice Mountain", Kingdom.ASGARNIA, 3009, 3487, 40, 40),
	ENTRANA("Entrana", Kingdom.ASGARNIA, 2850, 3379, 50, 50),

	// ---- Kandarin ----
	CATHERBY("Catherby", Kingdom.KANDARIN, 2836, 3457, 45, 40),
	SEERS_VILLAGE("Seers' Village", Kingdom.KANDARIN, 2692, 3489, 55, 55),
	CAMELOT("Camelot", Kingdom.KANDARIN, 2763, 3505, 30, 45),
	EAST_ARDOUGNE("East Ardougne", Kingdom.KANDARIN, 2597, 3298, 65, 70),
	WEST_ARDOUGNE("West Ardougne", Kingdom.KANDARIN, 2524, 3308, 45, 60),
	YANILLE("Yanille", Kingdom.KANDARIN, 2549, 3091, 75, 40),
	HEMENSTER("Hemenster", Kingdom.KANDARIN, 2634, 3426, 35, 35),
	WITCHAVEN("Witchaven", Kingdom.KANDARIN, 2710, 3291, 30, 30),
	TREE_GNOME_STRONGHOLD("Tree Gnome Stronghold", Kingdom.KANDARIN, 2433, 3454, 70, 70),
	PISCATORIS("Piscatoris Fishing Colony", Kingdom.KANDARIN, 2345, 3693, 55, 55),

	// ---- Fremennik ----
	RELLEKKA("Rellekka", Kingdom.FREMENNIK, 2672, 3681, 60, 60),
	KELDAGRIM("Keldagrim Entrance", Kingdom.FREMENNIK, 2728, 3715, 40, 40),
	NEITIZNOT("Neitiznot", Kingdom.FREMENNIK, 2320, 3821, 50, 50),
	JATIZSO("Jatizso", Kingdom.FREMENNIK, 2392, 3822, 50, 50),
	MISCELLANIA("Miscellania", Kingdom.FREMENNIK, 2545, 3879, 70, 70),
	LUNAR_ISLE("Lunar Isle", Kingdom.FREMENNIK, 2141, 3877, 70, 70),
	WEISS("Weiss", Kingdom.FREMENNIK, 2878, 3940, 50, 50),
	TROLLHEIM("Trollheim", Kingdom.FREMENNIK, 2892, 3678, 35, 35),
	TROLL_COUNTRY("Troll Country", Kingdom.FREMENNIK, 2846, 3736, 90, 90),

	// ---- Karamja ----
	BRIMHAVEN("Brimhaven", Kingdom.KARAMJA, 2779, 3174, 55, 55),
	MUSA_POINT("Musa Point", Kingdom.KARAMJA, 2914, 3161, 40, 40),
	TAI_BWO_WANNAI("Tai Bwo Wannai", Kingdom.KARAMJA, 2794, 3066, 45, 45),
	SHILO_VILLAGE("Shilo Village", Kingdom.KARAMJA, 2849, 2986, 50, 40),

	// ---- Kharidian Desert ----
	POLLNIVNEACH("Pollnivneach", Kingdom.DESERT, 3356, 2980, 45, 55),
	NARDAH("Nardah", Kingdom.DESERT, 3429, 2902, 45, 45),
	SOPHANEM("Sophanem", Kingdom.DESERT, 3298, 2783, 50, 50),
	MENAPHOS("Menaphos", Kingdom.DESERT, 3211, 2762, 60, 60),

	// ---- Morytania ----
	CANIFIS("Canifis", Kingdom.MORYTANIA, 3491, 3487, 45, 40),
	PORT_PHASMATYS("Port Phasmatys", Kingdom.MORYTANIA, 3673, 3487, 50, 50),
	DARKMEYER("Darkmeyer", Kingdom.MORYTANIA, 3626, 3363, 50, 55),
	MEIYERDITCH("Meiyerditch", Kingdom.MORYTANIA, 3619, 3263, 50, 70),
	BURGH_DE_ROTT("Burgh de Rott", Kingdom.MORYTANIA, 3497, 3219, 45, 45),
	MORTTON("Mort'ton", Kingdom.MORYTANIA, 3491, 3281, 40, 40),
	SLEPE("Slepe", Kingdom.MORYTANIA, 3718, 3330, 50, 50),
	VER_SINHAZA("Ver Sinhaza", Kingdom.MORYTANIA, 3662, 3220, 40, 40),
	FOSSIL_ISLAND("Fossil Island", Kingdom.MORYTANIA, 3721, 3781, 130, 130),

	// ---- Tirannwn ----
	PRIFDDINAS("Prifddinas", Kingdom.TIRANNWN, 2240, 3328, 70, 70),
	LLETYA("Lletya", Kingdom.TIRANNWN, 2346, 3180, 45, 45),

	// ---- Great Kourend ----
	HOSIDIUS("Hosidius", Kingdom.KOUREND, 1750, 3607, 130, 110),
	SHAYZIEN("Shayzien", Kingdom.KOUREND, 1536, 3552, 110, 110),
	LOVAKENGJ("Lovakengj", Kingdom.KOUREND, 1503, 3798, 110, 110),
	ARCEUUS("Arceuus", Kingdom.KOUREND, 1678, 3752, 110, 110),
	PORT_PISCARILIUS("Port Piscarilius", Kingdom.KOUREND, 1824, 3704, 90, 90),
	KOUREND_CASTLE("Kourend Castle", Kingdom.KOUREND, 1626, 3676, 60, 60),

	// ---- Varlamore ----
	CIVITAS_ILLA_FORTIS("Civitas illa Fortis", Kingdom.VARLAMORE, 1713, 3127, 110, 110),
	ALDARIN("Aldarin", Kingdom.VARLAMORE, 1391, 2942, 70, 70),
	AUBURNVALE("Auburnvale", Kingdom.VARLAMORE, 1405, 3363, 90, 90),

	// ---- Southern isles and Feldip ----
	FELDIP_HILLS("Feldip Hills", Kingdom.ISLANDS, 2546, 2950, 110, 110),
	GU_TANOTH("Gu'Tanoth", Kingdom.ISLANDS, 2528, 3046, 35, 32),
	JIGGIG("Jiggig", Kingdom.ISLANDS, 2464, 3047, 40, 40),
	CORSAIR_COVE("Corsair Cove", Kingdom.ISLANDS, 2570, 2858, 50, 50),
	ISLE_OF_SOULS("Isle of Souls", Kingdom.ISLANDS, 2209, 2878, 130, 130),
	APE_ATOLL("Ape Atoll", Kingdom.ISLANDS, 2755, 2755, 80, 80),
	MOS_LE_HARMLESS("Mos Le'Harmless", Kingdom.ISLANDS, 3725, 3028, 80, 80),

	// ---- Kingdom-scale fallbacks. Sized large on purpose: a point only lands on one of these
	// when no city box claimed it, because of() prefers the smallest containing box. ----
	WILDERNESS("Wilderness", Kingdom.WILDERNESS, 3168, 3812, 224, 288, true),
	MISTHALIN("Misthalin", Kingdom.MISTHALIN, 3217, 3321, 200, 230, true),
	ASGARNIA("Asgarnia", Kingdom.ASGARNIA, 2988, 3412, 160, 200, true),
	KANDARIN("Kandarin", Kingdom.KANDARIN, 2578, 3370, 230, 280, true),
	KARAMJA("Karamja", Kingdom.KARAMJA, 2860, 3059, 190, 230, true),
	KHARIDIAN_DESERT("Kharidian Desert", Kingdom.DESERT, 3311, 2930, 250, 250, true),
	FREMENNIK_PROVINCE("Fremennik Province", Kingdom.FREMENNIK, 2679, 3646, 180, 180, true),
	TIRANNWN_REGION("Tirannwn", Kingdom.TIRANNWN, 2240, 3264, 180, 220, true),
	GREAT_KOUREND("Great Kourend", Kingdom.KOUREND, 1600, 3700, 300, 250, true),
	VARLAMORE_REGION("Varlamore", Kingdom.VARLAMORE, 1510, 3195, 300, 300, true);

	/** Top-level grouping, so the browse list reads as a map rather than an alphabet. */
	public enum Kingdom
	{
		MISTHALIN("Misthalin"),
		ASGARNIA("Asgarnia"),
		KANDARIN("Kandarin"),
		FREMENNIK("Fremennik"),
		KARAMJA("Karamja"),
		DESERT("Kharidian Desert"),
		MORYTANIA("Morytania"),
		TIRANNWN("Tirannwn"),
		KOUREND("Great Kourend"),
		VARLAMORE("Varlamore"),
		ISLANDS("Islands"),
		WILDERNESS("Wilderness");

		@Getter
		private final String displayName;

		Kingdom(String displayName)
		{
			this.displayName = displayName;
		}

	}

	@Getter
	private final String displayName;
	@Getter
	private final Kingdom kingdom;
	@Getter
	private final int centerX;
	@Getter
	private final int centerY;
	private final int halfWidth;
	private final int halfHeight;
	private final boolean fallback;

	MapRegion(String displayName, Kingdom kingdom, int centerX, int centerY, int halfWidth, int halfHeight)
	{
		this(displayName, kingdom, centerX, centerY, halfWidth, halfHeight, false);
	}

	MapRegion(String displayName, Kingdom kingdom, int centerX, int centerY, int halfWidth, int halfHeight,
		boolean fallback)
	{
		this.displayName = displayName;
		this.kingdom = kingdom;
		this.centerX = centerX;
		this.centerY = centerY;
		this.halfWidth = halfWidth;
		this.halfHeight = halfHeight;
		this.fallback = fallback;
	}

	/**
	 * True for the kingdom-scale catch-all boxes. Their label points sit inside their own cities —
	 * the "Asgarnia" label is drawn on top of Falador — so unlike a city box a fallback is not
	 * expected to own its own centre. It exists to give a name to everything the cities miss.
	 */
	public boolean isFallback()
	{
		return fallback;
	}





	public int getMinX()
	{
		return centerX - halfWidth;
	}

	public int getMaxX()
	{
		return centerX + halfWidth;
	}

	public int getMinY()
	{
		return centerY - halfHeight;
	}

	public int getMaxY()
	{
		return centerY + halfHeight;
	}

	/** Box area in tiles, the primary tiebreak in {@link #of(int, int, int)}. */
	public long area()
	{
		return (long) (2 * halfWidth) * (2 * halfHeight);
	}

	public boolean contains(int x, int y)
	{
		return x >= getMinX() && x <= getMaxX() && y >= getMinY() && y <= getMaxY();
	}

	private long centreDistanceSq(int x, int y)
	{
		final long dx = x - centerX;
		final long dy = y - centerY;
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
			if (region.displayName.toLowerCase(Locale.ROOT).equals(target))
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
				if (region.kingdom == kingdom)
				{
					ordered.add(region);
				}
			}
		}
		return Collections.unmodifiableList(ordered);
	}
}
