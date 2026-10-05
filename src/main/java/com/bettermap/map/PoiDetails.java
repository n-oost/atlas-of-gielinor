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

import com.bettermap.data.BundledTsv;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
		private int[][] moorings = new int[0][];
		private Map<String, Detail> questDetails = Collections.emptyMap();
		private boolean loaded;
	}

	private static volatile Indexes data = new Indexes();

	public static boolean isLoaded()
	{
		return data.loaded;
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
			return detailFromCategory(poi);
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

		return detailFromCategory(poi);
	}

	/**
	 * Last resort when no curated entry matches: guess the detail from the POI's own key and
	 * name. Ordered most specific first - an explicit key wins, then a name keyword, then the
	 * generic "Point of Interest".
	 */
	private static Detail detailFromCategory(PoiIndex.Poi poi)
	{
		final Detail imported = "mooring_point".equals(poi.getKey()) ? null : WorldMapSupplement.detail(poi);
		if (imported != null)
		{
			return imported;
		}
		final String key = poi.getKey();
		final String name = poi.getName();
		final String lowerName = name.toLowerCase(Locale.ROOT);

		final List<String> dungeonReq = data.dungeonRequirements.get(name);
		if (dungeonReq != null)
		{
			return new Detail(name, "Dungeons", dungeonReq);
		}

		if ("rare_trees".equals(key) || (lowerName.contains("tree") && !lowerName.contains("mushtree")))
		{
			return new Detail(name, "Skilling • Woodcutting", List.of(
				"Skill: Woodcutting",
				"Tool needed: Axe (Bronze to Crystal)",
				"Check Woodcutting skill guide for exact level requirement"
			));
		}

		if ("dungeon".equals(key) || "dungeon_link".equals(key)
			|| lowerName.contains("dungeon") || lowerName.contains("cave"))
		{
			final List<String> specific = data.dungeonRequirements.get(name);
			if (specific != null)
			{
				return new Detail(name, "Dungeons", specific);
			}
			return new Detail(name, "Dungeons", List.of(
				"Type: Dungeon entrance / underground link",
				"May require light source, rope, or combat gear"
			));
		}

		if ("fishing_spot".equals(key) || lowerName.contains("fishing"))
		{
			return new Detail(name, "Skilling • Fishing", List.of(
				"Skill: Fishing",
				"Tools: Net, Rod + Bait, Pot, or Harpoon",
				"Check Fishing skill guide for exact bait and level requirements"
			));
		}

		if ("mining_site".equals(key) || (lowerName.contains("mine") && !lowerName.contains("minecart")))
		{
			return new Detail(name, "Skilling • Mining", List.of(
				"Skill: Mining",
				"Tool needed: Pickaxe (Bronze to Crystal)",
				"Check Mining skill guide for ore levels"
			));
		}

		if ("agility_short-cut".equals(key) || lowerName.contains("short-cut") || lowerName.contains("shortcut"))
		{
			return new Detail(name, "Agility shortcuts", List.of(
				"Agility shortcut: bypass obstacles and travel routes faster",
				"Check Agility skill guide for required level"
			));
		}

		if ("agility_training".equals(key) || lowerName.contains("agility") || lowerName.contains("course"))
		{
			return new Detail(name, "Skilling • Agility", List.of(
				"Skill: Agility",
				"Activity: Obstacle or Rooftop Course",
				"Check Agility skill guide for course level and lap XP"
			));
		}

		if ("hunter_training".equals(key) || lowerName.contains("hunter") || lowerName.contains("hunting"))
		{
			return new Detail(name, "Skilling • Hunter", List.of(
				"Skill: Hunter",
				"Tools: Traps (box/snare/net/pitfall/deadfall), Noose wand, or Butterfly net",
				"Check Hunter skill guide for creature level requirements"
			));
		}

		if ("farming_patch".equals(key) || lowerName.contains("farming"))
		{
			return new Detail(name, "Skilling • Farming", List.of(
				"Skill: Farming",
				"Tools needed: Rake, Spade, Seed dibber, Watering can",
				"Check Farming skill guide for seed levels"
			));
		}

		if ("quest_start".equals(key) || lowerName.contains("quest"))
		{
			final Detail questDetail = data.questDetails.get(name);
			if (questDetail != null)
			{
				return questDetail;
			}
			return new Detail(name, "Quest Start", List.of(
				"Talk to the quest giver NPC nearby to begin",
				"Check Quest List in-game for full requirement breakdown"
			));
		}

		if (key != null)
		{
			if (key.endsWith("_tutor"))
			{
				return new Detail(name, "Tutors and services", List.of(
					"Free starter items and skill advice"
				));
			}

			switch (key)
			{
				case "thieving":
					return new Detail(name, "Skilling • Thieving", List.of(
						"Pick locked chests or steal from stalls for loot",
						"Check Thieving skill guide for the required level"
					));
				case "bank":
					return new Detail(name, "Banks", List.of(
						"Bank booth or chest: deposit, withdraw and note items"
					));
				case "water_source":
					return new Detail(name, "Skilling", List.of(
						"Fill buckets, watering cans, vials and jugs here"
					));
				case "cooking_range":
					return new Detail(name, "Skilling • Cooking", List.of(
						"Range or fire: cook raw food here",
						"Some ranges reduce the chance to burn food"
					));
				case "anvil":
					return new Detail(name, "Skilling • Smithing", List.of(
						"Smithing: needs a hammer and metal bars"
					));
				case "furnace":
					return new Detail(name, "Skilling • Smithing", List.of(
						"Smelting: turns ore into bars",
						"Also used for gold jewellery with a mould"
					));
				case "altar":
					if ("Imbued altar".equalsIgnoreCase(name))
					{
						return new Detail(name, "Altars", List.of(
							"Converts refined tephra into imbued tephra during the Zalcano fight"
						));
					}
					return new Detail(name, "Altars", List.of(
						"Recharges Prayer points"
					));
				case "house_portal":
					return new Detail(name, "Travel", List.of(
						"Player-owned house portal"
					));
				case "mooring_point":
					return mooringDetail(poi, name);
				case "salvaging":
					return new Detail(name, "Skilling • Sailing", List.of(
						"Salvage shipwrecks using a salvaging hook installed on your vessel"
					));
				case "shipwright":
					return new Detail(name, "Shops and trade", List.of(
						"Shipyard services: boat retrieval, ship customization, and vessel upgrades"
					));
				case "cargo_bay":
					return new Detail(name, "Tutors and services", List.of(
						"Port cargo loading bay and shipwreck salvage sorting"
					));
				case "noticeboard":
					if (name.toLowerCase().contains("port task"))
					{
						return new Detail(name, "Noticeboard", List.of(
							"Courier deliveries and maritime bounty contracts"
						));
					}
					return new Detail(name, "Noticeboard", List.of(
						"View local announcements, activities, and task notices"
					));
				case "lookout_point":
					return new Detail(name, "Sailing", List.of(
						"High vantage point for coastal, sea, and territory observation"
					));
				case "singing_bowl":
					return new Detail(name, "Skilling • Crafting", List.of(
						"Sing crystal equipment, tools, and armor using crystal shards",
						"Requires Song of the Elves to access Prifddinas singing bowls"
					));
				case "canoe_station":
					return new Detail(name, "Travel", List.of(
						"River transportation via dugout, canoe, or waka",
						"Craft a canoe using an axe on the fallen tree station"
					));
				case "hot_air_balloon":
					return new Detail(name, "Travel", List.of(
						"Hot air balloon transport network",
						"Requires logs to travel (Willow, Yew, Magic)",
						"Requires Enlightened Journey quest"
					));
				case "magic_mushtree":
					return new Detail(name, "Travel", List.of(
						"Fossil Island rapid travel network between key locations",
						"Requires Bone Voyage quest"
					));
				case "magic_carpet":
					return new Detail(name, "Travel", List.of(
						"Desert and regional carpet transportation",
						"Talk to the Rug merchant to travel (200 coins)"
					));
				case "minecart_network":
					return new Detail(name, "Travel", List.of(
						"Great Kourend minecart transit system",
						"Requires minecart control scroll or 50% Lovakengj favour"
					));
				case "sea_current":
					return new Detail(name, "Travel", List.of(
						"Ocean speed current",
						"Increases vessel speed when traveling in the current's direction"
					));
				case "poll_booth":
					return new Detail(name, "Tutors and services", List.of(
						"Vote in the current poll or read past results"
					));
				case "kourend_task":
					return new Detail(name, "Quests and activities", List.of(
						"Kourend & Kebos Diary task location",
						"Claim diary rewards from Elise at Kourend Castle"
					));
				case "minigame":
					return new Detail(name, "Quests and activities", List.of(
						"Minigame or activity location"
					));
				case "task_master":
					return new Detail(name, "Quests and activities", List.of(
						"Achievement Diary task master: claim tier rewards",
						"Tiers: easy, medium, hard, elite"
					));
				case "dairy_churn":
					return new Detail(name, "Skilling • Cooking", List.of(
						"Churn milk into cream, butter, or cheese"
					));
				case "slayer_master":
					return new Detail(name, "Skilling • Slayer", List.of(
						"Assigns Slayer tasks; each master has a combat level requirement",
						"All Slayer Masters also sell Slayer equipment"
					));
				case "potters_wheel":
					return new Detail(name, "Skilling • Crafting", List.of(
						"Shape soft clay into unbaked pottery"
					));
				case "spinning_wheel":
					return new Detail(name, "Skilling • Crafting", List.of(
						"Spin wool into balls of wool or flax into bowstrings"
					));
				case "tannery":
					return new Detail(name, "Skilling • Crafting", List.of(
						"Tan cowhides and dragonhides into leather for a small fee"
					));
				case "sawmill":
					return new Detail(name, "Skilling • Construction", List.of(
						"Converts logs into planks for Construction"
					));
				case "windmill":
					return new Detail(name, "Skilling • Cooking", List.of(
						"Grind grain into flour using the hopper and millstones"
					));
				case "loom":
					return new Detail(name, "Skilling • Crafting", List.of(
						"Weave jute, flax, or willow branches into cloth and sacks"
					));
				case "sandpit":
					return new Detail(name, "Skilling", List.of(
						"Fill empty buckets with sand for Crafting molten glass"
					));
				case "brewery":
					return new Detail(name, "Skilling • Cooking", List.of(
						"Brew ales and ciders using a brewing vat"
					));
				case "raids_lobby":
					return new Detail(name, "Quests and activities", List.of(
						"Raids lobby: form a party and prepare for raid encounters"
					));
				case "region_label":
					return new Detail(name, "Places", List.of(
						"Named place from the game map"
					));
			}
		}

		return new Detail(name, "Point of Interest", Collections.emptyList());
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

	private static Detail mooringDetail(PoiIndex.Poi poi, String name)
	{
		if ("Mooring buoy".equalsIgnoreCase(name))
		{
			return new Detail(name, "Travel", List.of(
				"Off-shore mooring buoy for docking player vessels",
				"Requires Sailing to dock"
			));
		}
		if ("Ship boarding plank".equalsIgnoreCase(name))
		{
			return new Detail(name, "Travel", List.of(
				"Port dock for boarding and disembarking player ships"
			));
		}
		final int level = poi != null ? getMooringLevel(poi.getX(), poi.getY()) : 0;
		if ("Mooring point - The Summer Shore".equalsIgnoreCase(name))
		{
			final List<String> lines = new ArrayList<>();
			lines.add("Mooring point for docking and disembarking player vessels");
			if (level > 0)
			{
				lines.add("Requires Level " + level + " Sailing");
			}
			lines.add("Requires completion of Troubled Tortugans");
			return new Detail(name, "Travel", lines);
		}
		if (level > 0)
		{
			return new Detail(name, "Travel", List.of(
				"Mooring point for docking and disembarking player vessels",
				"Requires Level " + level + " Sailing"
			));
		}
		return new Detail(name, "Travel", List.of(
			"Mooring point for docking and disembarking player vessels"
		));
	}
}
