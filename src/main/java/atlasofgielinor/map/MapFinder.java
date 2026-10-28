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
package atlasofgielinor.map;

import atlasofgielinor.map.catalog.GroundItemIndex;
import atlasofgielinor.map.catalog.MapRegion;
import atlasofgielinor.map.catalog.MonsterIndex;
import atlasofgielinor.map.catalog.PoiCategory;
import atlasofgielinor.map.catalog.PoiIndex;
import atlasofgielinor.map.catalog.ShopIndex;
import atlasofgielinor.map.finder.FinderSearchText;
import atlasofgielinor.map.finder.FinderPoiIndex;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;

import atlasofgielinor.AtlasOfGielinorConfig;
import net.runelite.api.coords.WorldPoint;
import lombok.Getter;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class MapFinder
{
	@Getter
	@RequiredArgsConstructor
	public enum BrowseCategory
	{
		BANKS("Banks", "Nearby bank locations"),
		SKILLING("Skilling", "Resources and skill facilities"),
		MONSTERS("Monsters", "Monster names and locations"),
		QUESTS("Quests", "Quest starting locations"),
		SHOPS("Shops", "Shops and trading locations"),
		TRAVEL("Travel", "Teleports, ports and transport"),
		ACTIVITIES("Activities", "Minigames, raids and diary tasks"),
		DUNGEONS("Dungeons", "Dungeon entrances"),
		PLACES("Places", "Browse regions"),
		SERVICES("Services", "Altars, tutors and facilities");

		private final String displayName;
		private final String description;


		public static BrowseCategory of(String key)
		{
			switch (PoiCategory.of(key))
			{
				case BANKS: return BANKS;
				case SKILLING:
				case SHORTCUTS:
				case SAILING: return SKILLING;
				case QUESTS: return "quest_start".equals(key) ? QUESTS : ACTIVITIES;
				case SHOPS: return SHOPS;
				case TRAVEL: return TRAVEL;
				case DUNGEONS: return DUNGEONS;
				case PLACES: return PLACES;
				case ALTARS:
				case SERVICES: return SERVICES;
				default: return null;
			}
		}

		public String getIconKey()
		{
			switch (this)
			{
				case BANKS: return "bank";
				case SKILLING: return "mining_site";
				case MONSTERS: return "slayer_master";
				case QUESTS: return "quest_start";
				case SHOPS: return "general_store";
				case TRAVEL: return "transportation";
				case ACTIVITIES: return "minigame";
				case DUNGEONS: return "dungeon";
				case PLACES: return "map_link";
				case SERVICES: return "altar";
				default: return null;
			}
		}
	}

	@Getter
	private volatile BrowseCategory browseCategory;
	private final Map<BrowseCategory, List<Result>> categoryLeaves = new HashMap<>();

	public synchronized void selectBrowseCategory(BrowseCategory category, WorldPoint from)
	{
		browseCategory = category;
		updateQuery(query, from);
	}

	public static final int MAX_RESULTS = 8;
	static final int MAX_QUERY_LENGTH = 40;

	private final PoiIndex poiIndex;
	private final MonsterIndex monsterIndex;
	private final ShopIndex shopIndex;
	private final GroundItemIndex groundItemIndex;
	private final AtlasOfGielinorConfig config;


	@Getter
	private volatile String query = "";
	@Getter
	private volatile List<Result> results = Collections.emptyList();
	private List<Result> queryMatches = Collections.emptyList();
	@Getter
	private volatile int matchCount;
	@Getter
	private volatile int locationMatchCount;
	private WorldPoint resultOrigin;
	private long searchDataVersion = Long.MIN_VALUE;
	private long normalizedDataVersion = Long.MIN_VALUE;
	private final Map<String, String> normalizedNames = new HashMap<>();
	/** The last non-empty query, so the "Last" quick-find chip can put it back. */
	private volatile String lastQuery = "";

	/** Raw browse leaves per region, rebuilt whenever either source publishes new data. */
	private final Map<MapRegion, List<Result>> regionLeaves = new HashMap<>();
	private final Map<MapRegion, BrowseCacheEntry> regionContentsCache = new HashMap<>();
	private long browseDataVersion = Long.MIN_VALUE;

	/**
	 * Test constructor: POI and monster search only, no shop / ground-item search and no config
	 * gate. Production uses the {@link Inject}-annotated generated constructor.
	 */
	public MapFinder(PoiIndex poiIndex, MonsterIndex monsterIndex)
	{
		this(poiIndex, monsterIndex, null, null, null);
	}


	private boolean itemSearchEnabled()
	{
		return config == null || config.finderSearchItems();
	}





	public boolean isBrowseLoading()
	{
		return !poiIndex.isLoaded() || !monsterIndex.isLoaded();
	}

	/** First displayed row, resolved to its best child when it represents a group. */
	public synchronized Result selectedResult()
	{
		return results.isEmpty() ? null : activationTarget(results.get(0));
	}


	/** Resolves a displayed group row to the same leaf for keyboard and mouse activation. */
	public static Result activationTarget(Result row)
	{
		return row != null && row.hasChildren() ? row.getChildren().get(0) : row;
	}

	/**
	 * Every browsable region, nearest-first from {@code from}. Kingdom-scale fallbacks are omitted.
	 */
	public synchronized List<MapRegion> getRegions(WorldPoint from)
	{
		final List<MapRegion> regions = new ArrayList<>();
		for (MapRegion region : MapRegion.values())
		{
			if (!region.isFallback())
			{
				regions.add(region);
			}
		}
		regions.sort(Comparator.comparingInt(r -> distanceToRegionCenter(r, from)));
		return Collections.unmodifiableList(regions);
	}

	/**
	 * Every POI and monster zone in {@code region}, grouped by name with children for duplicates,
	 * nearest first. Built lazily from a one-time index pass.
	 */
	public synchronized List<Result> getRegionContents(MapRegion region, WorldPoint from)
	{
		ensureBrowseIndex();
		if (isBrowseLoading())
		{
			return Collections.emptyList();
		}
		final List<Result> leaves = regionLeaves.get(region);
		if (leaves == null || leaves.isEmpty())
		{
			return Collections.emptyList();
		}

		final BrowseCacheEntry cached = regionContentsCache.get(region);
		if (cached != null && Objects.equals(cached.origin, from))
		{
			return cached.results;
		}

		final List<Result> withDistance = new ArrayList<>(leaves.size());
		for (Result leaf : leaves)
		{
			final int tiles = distanceTiles(leaf.getPoint(), from);
			withDistance.add(leaf.withDistance(tiles));
		}
		withDistance.sort(Comparator.comparingInt(Result::getDistanceTiles));
		final List<Result> grouped = Collections.unmodifiableList(groupByName(withDistance, Integer.MAX_VALUE));
		regionContentsCache.put(region, new BrowseCacheEntry(from, grouped));
		return grouped;
	}

	static int matchTier(String name, String locationName, String lowerQuery)
	{
		if (lowerQuery == null || lowerQuery.isEmpty())
		{
			return -1;
		}

		final String lq = FinderSearchText.query(lowerQuery);
		if (name != null)
		{
			final int tier = FinderSearchText.tier(FinderSearchText.normalize(name), lq);
			if (tier >= 0) return tier;
		}

		if (locationName != null)
		{
			if (FinderSearchText.tier(FinderSearchText.normalize(locationName), lq) >= 0)
			{
				return 3;
			}
		}

		return -1;
	}

	private int searchTier(String name, String detail, String term)
	{
		final int tier = FinderSearchText.tier(normalizedName(name), term);
		return tier >= 0 ? tier : FinderSearchText.tier(normalizedName(detail), term) >= 0 ? 3 : -1;
	}

	private String normalizedName(String name)
	{
		return name == null ? "" : normalizedNames.computeIfAbsent(name, FinderSearchText::normalize);
	}

	public synchronized void updateQuery(String next, WorldPoint from)
	{
		if (next == null)
		{
			next = "";
		}
		if (next.length() > MAX_QUERY_LENGTH)
		{
			next = next.substring(0, MAX_QUERY_LENGTH);
		}
		this.query = next;
		if (!next.isEmpty())
		{
			this.lastQuery = next;
		}

		// Trim only the search term, never the editable text: trimming the stored query ate every
		// trailing space, so a typed "king " came back as "king" and the next character produced
		// "kingb". Multi-word queries were unreachable by typing.
		final long dataVersion = currentSearchDataVersion();
		if (normalizedDataVersion != dataVersion)
		{
			normalizedNames.clear();
			normalizedDataVersion = dataVersion;
		}
		final String lower = FinderSearchText.query(next);
		final boolean resourceQuery = poiIndex.finderEntries().stream().anyMatch(entry ->
			entry.getSubject().isResource() && primarySubjectTier(entry, lower) >= 0);
		if (browseCategory != null)
		{
			ensureBrowseIndex();
			final List<Result> candidates = new ArrayList<>();
			for (Result leaf : categoryLeaves.getOrDefault(browseCategory, Collections.emptyList()))
			{
				final String term = leaf.getKind() == Result.Kind.MINERAL ? FinderSearchText.mineral(lower) : lower;
				final int tier = lower.isEmpty() ? 0 : leaf.getSubjectEntry() == null
					? searchTier(groupKey(leaf), leaf.getDetail(), term)
					: subjectTier(leaf.getSubjectEntry(), term, resourceQuery);
				if (tier < 0) continue;
				candidates.add(leaf.withRanking(distanceTiles(leaf.getPoint(), from), tier));
			}
			queryMatches = Collections.unmodifiableList(candidates);
			resultOrigin = from;
			searchDataVersion = currentSearchDataVersion();
			publishRankedResults(candidates);
			return;
		}
		if (lower.isEmpty())
		{
			this.results = Collections.emptyList();
			this.queryMatches = Collections.emptyList();
			this.matchCount = 0;
			this.locationMatchCount = 0;
			this.resultOrigin = from;
			return;
		}

		final List<Result> candidates = new ArrayList<>();

		for (FinderPoiIndex.Entry entry : poiIndex.finderEntries())
		{
			final String term = "mining_site".equals(entry.getPoi().getKey()) ? FinderSearchText.mineral(lower) : lower;
			final int tier = subjectTier(entry, term, resourceQuery);
			if (tier >= 0) candidates.add(subjectResult(entry, from, tier));
		}

		for (MonsterIndex.Zone zone : monsterIndex.getZones())
		{
			final int tier = searchTier(zone.getMonster(), zone.getLocationName(), lower);
			if (tier == -1)
			{
				continue;
			}
			final WorldPoint point = new WorldPoint(zone.getX(), zone.getY(), zone.getPlane());
			final int tiles = distanceTiles(point, from);
			// Keep both the searched monster and its location visible in the result.
			candidates.add(new Result(zone.getLocationName(), zone.getMonster(), point, tiles, tier,
				Result.Kind.MONSTER, zone.getLocationName(), 0));
		}

		if (itemSearchEnabled() && shopIndex != null && groundItemIndex != null)
		{
			final String term = next.trim();

			for (ShopIndex.Shop shop : shopIndex.all())
			{
				final ShopIndex.StockItem matched = matchingStock(shop, lower);
				final String matchName = matched != null ? matched.getName() : null;
				final int itemTier = searchTier(matchName, null, lower);
				final int shopTier = searchTier(shop.getName(), null, lower);
				if (itemTier < 0 && shopTier < 0)
				{
					continue;
				}
				final int tier = itemTier >= 0 ? itemTier : shopTier + 4;
				final WorldPoint point = new WorldPoint(shop.getX(), shop.getY(), shop.getPlane());
				final int tiles = distanceTiles(point, from);
				final int sellGp = matched != null ? matched.getSell() : 0;
				final String itemName = matchName != null ? matchName : shop.getName();
				final String subtitle = matched != null ? shop.getName() + " · " + matched.toDetailLine() : "Shop name match";
				candidates.add(new Result(itemName, "Shop item: " + itemName,
					point, tiles, tier, Result.Kind.SHOP, subtitle, sellGp));
			}

			for (GroundItemIndex.Spawn spawn : groundItemIndex.all())
			{
				String itemName = null;
				int tier = Integer.MAX_VALUE;
				for (GroundItemIndex.Item item : spawn.getItems())
				{
					final int itemTier = searchTier(item.getName(), null, lower);
					if (itemTier >= 0 && itemTier < tier)
					{
						itemName = item.getName();
						tier = itemTier;
					}
				}
				if (itemName == null) continue;
				final WorldPoint point = new WorldPoint(spawn.getX(), spawn.getY(), spawn.getPlane());
				final int tiles = distanceTiles(point, from);
				final String matchedName = itemName != null ? itemName : term;
				final String location = spawn.getLocation() != null && !spawn.getLocation().isEmpty()
					? spawn.getLocation() : "Ground spawn";
				candidates.add(new Result(location, "Ground item: " + matchedName,
					point, tiles, tier, Result.Kind.GROUND_ITEM, "Item: " + matchedName, 0));
			}
		}

		if (candidates.isEmpty() && lower.length() >= 5 && lower.indexOf(' ') < 0)
		{
			final String correction = uniquePlaceCorrection(lower);
			if (correction != null)
			{
				updateQuery(correction, from);
				this.query = next;
				this.lastQuery = next;
				return;
			}
		}
		this.queryMatches = Collections.unmodifiableList(candidates);
		this.resultOrigin = from;
		this.searchDataVersion = currentSearchDataVersion();
		publishRankedResults(candidates);
	}

	private String uniquePlaceCorrection(String term)
	{
		String correction = null;
		for (PoiIndex.Poi poi : poiIndex.searchEntries())
		{
			final String name = normalizedName(poi.getName());
			if (name.indexOf(' ') >= 0 || !FinderSearchText.oneEditApart(term, name)) continue;
			if (correction != null && !correction.equals(name)) return null;
			correction = name;
		}
		return correction;
	}

	private ShopIndex.StockItem matchingStock(ShopIndex.Shop shop, String term)
	{
		ShopIndex.StockItem best = null;
		int bestTier = Integer.MAX_VALUE;
		for (ShopIndex.StockItem item : shop.getStock())
		{
			if (item == null) continue;
			final int tier = searchTier(item.getName(), null, term);
			if (tier >= 0 && tier < bestTier)
			{
				best = item;
				bestTier = tier;
				if (tier == 0) return best;
			}
		}
		if (best != null) return best;
		// Retain legacy hints for shops whose full stock is unavailable.
		if (shop.getNotable() != null)
		{
			for (String hint : shop.getNotable())
			{
				if (searchTier(hint, null, term) >= 0) return shop.stockItemMatch(hint.toLowerCase(Locale.ROOT));
			}
		}
		for (String hint : new String[]{shop.getBulk(), shop.getSpecial()})
		{
			if (searchTier(hint, null, term) >= 0) return shop.stockItemMatch(hint.toLowerCase(Locale.ROOT));
		}
		return null;
	}

	private int primarySubjectTier(FinderPoiIndex.Entry entry, String term)
	{
		final FinderPoiIndex.Subject subject = entry.getSubject();
		if (subject.isResource())
		{
			final String type = normalizedName(subject.getType());
			if (term.startsWith(type + " ")) term = term.substring(type.length() + 1);
			if (term.endsWith(" " + type)) term = term.substring(0, term.length() - type.length() - 1);
			if ("Mining".equals(subject.getType())) term = FinderSearchText.mineral(term);
		}
		if (subject.isResource() && "Woodcutting".equals(subject.getType())
			&& searchTier(subject.getName().replaceFirst(" trees$", ""), null, term) == 0) return 0;
		return searchTier(subject.getName(), null, term);
	}

	private int subjectTier(FinderPoiIndex.Entry entry, String term, boolean resourceQuery)
	{
		final FinderPoiIndex.Subject subject = entry.getSubject();
		final int primary = primarySubjectTier(entry, term);
		if (primary >= 0) return primary;
		// A resource query must not match another catch through an old family alias.
		if (resourceQuery && subject.isResource()) return -1;
		if (searchTier(subject.getType(), null, term) >= 0) return 2;
		for (String alias : entry.getAliases())
		{
			final int tier = searchTier(alias, null, term);
			if (tier >= 0) return subject.isResource() ? 3 : tier;
		}
		// Context fields are useful search terms; tool and requirement text is not a source of items.
		for (String line : entry.getDetails().split(" · "))
		{
			if ((line.startsWith("Region:") || line.startsWith("Location:"))
				&& searchTier(line, null, term) >= 0) return 3;
		}
		return -1;
	}

	private static Result subjectResult(FinderPoiIndex.Entry entry, WorldPoint from, int tier)
	{
		final PoiIndex.Poi poi = entry.getPoi();
		final WorldPoint point = new WorldPoint(poi.getX(), poi.getY(), poi.getPlane());
		final Result.Kind kind = "mining_site".equals(poi.getKey()) ? Result.Kind.MINERAL : Result.Kind.PLACE;
		return new Result(entry.getSubject().getName(), entry.getSubject().getName(), point,
			distanceTiles(point, from), tier, kind, Collections.emptyList(), entry.getDetails(), 0, entry);
	}

	/** Refresh distance ordering after movement without rescanning source datasets. */
	public synchronized void refresh(WorldPoint from)
	{
		if (browseCategory == null && (query == null || query.trim().isEmpty()))
		{
			return;
		}
		if (searchDataVersion != currentSearchDataVersion())
		{
			updateQuery(query, from);
			return;
		}
		if (Objects.equals(resultOrigin, from))
		{
			return;
		}
		final List<Result> refreshed = new ArrayList<>(queryMatches.size());
		for (Result match : queryMatches)
		{
			refreshed.add(match.withDistance(distanceTiles(match.getPoint(), from)));
		}
		queryMatches = Collections.unmodifiableList(refreshed);
		resultOrigin = from;
		publishRankedResults(refreshed);
	}

	private void publishRankedResults(List<Result> candidates)
	{
		final List<Result> allGroups = rank(candidates, Integer.MAX_VALUE);
		locationMatchCount = (int) candidates.stream().map(Result::getPoint).distinct().count();
		matchCount = allGroups.size();
		results = Collections.unmodifiableList(allGroups);
	}

	/** The last non-empty query, for the "Last" quick-find chip. Survives {@link #clearQuery()}. */
	public String getLastQuery()
	{
		return lastQuery;
	}

	public synchronized void clearQuery()
	{
		updateQuery("", resultOrigin);
	}

	private void ensureBrowseIndex()
	{
		if (isBrowseLoading())
		{
			return;
		}
		final long version = currentBrowseDataVersion();
		if (browseDataVersion == version)
		{
			return;
		}

		regionLeaves.clear();
		regionContentsCache.clear();
		categoryLeaves.clear();

		for (FinderPoiIndex.Entry entry : poiIndex.finderEntries())
		{
			final PoiIndex.Poi poi = entry.getPoi();
			final MapRegion region = MapRegion.of(poi.getX(), poi.getY(), poi.getPlane());
			final Result leaf = subjectResult(entry, null, 0);
			final String name = leaf.getName();
			final BrowseCategory category = BrowseCategory.of(poi.getKey());
			if (category != null)
			{
				categoryLeaves.computeIfAbsent(category, k -> new ArrayList<>()).add(leaf);
			}
			if ("region_label".equals(poi.getKey())
				&& "wintertodt".equals(FinderSearchText.normalize(name)))
			{
				categoryLeaves.computeIfAbsent(BrowseCategory.ACTIVITIES, k -> new ArrayList<>()).add(leaf);
			}
			if (region != null && entry.isBrowsable())
			{
				regionLeaves.computeIfAbsent(region, k -> new ArrayList<>()).add(leaf);
			}
		}

		for (MonsterIndex.Zone zone : monsterIndex.getZones())
		{
			final MapRegion region = MapRegion.of(zone.getX(), zone.getY(), zone.getPlane());
			final WorldPoint point = new WorldPoint(zone.getX(), zone.getY(), zone.getPlane());
			categoryLeaves.computeIfAbsent(BrowseCategory.MONSTERS, k -> new ArrayList<>())
				.add(new Result(zone.getLocationName(), zone.getMonster(), point, -1, 0,
					Result.Kind.MONSTER, zone.getLocationName(), 0));
			if (region != null)
			{
				regionLeaves.computeIfAbsent(region, k -> new ArrayList<>())
					.add(new Result(zone.getLocationName(), zone.getMonster(), point, -1, 0,
						Result.Kind.MONSTER, zone.getLocationName(), 0));
			}
		}

		browseDataVersion = version;
	}

	private long currentBrowseDataVersion()
	{
		return 31L * poiIndex.getDataVersion() + monsterIndex.getDataVersion();
	}

	private long currentSearchDataVersion()
	{
		long version = currentBrowseDataVersion();
		if (shopIndex != null)
		{
			version = 31L * version + shopIndex.getDataVersion();
		}
		if (groundItemIndex != null)
		{
			version = 31L * version + groundItemIndex.getDataVersion();
		}
		return version;
	}

	@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
	private static final class BrowseCacheEntry
	{
		private final WorldPoint origin;
		private final List<Result> results;

	}

	/** Best textual match first, then distance; group children after per-result relevance ranking. */
	static List<Result> rank(List<Result> candidates, int max)
	{
		if (candidates == null || candidates.isEmpty() || max <= 0)
		{
			return Collections.emptyList();
		}

		final List<Result> sorted = new ArrayList<>(candidates);
		sorted.sort(MapFinder::compareByRelevanceThenDistance);
		return groupByName(sorted, max);
	}

	/**
	 * Collapses rows sharing a groupKey into one parent with children. A singleton group stays a
	 * plain leaf — no affordance. {@code sortedLeaves} must already be ordered so the first member
	 * of each group is its best match.
	 */
	static List<Result> groupByName(List<Result> sortedLeaves, int max)
	{
		if (sortedLeaves == null || sortedLeaves.isEmpty() || max <= 0)
		{
			return Collections.emptyList();
		}

		final Map<String, List<Result>> groups = new LinkedHashMap<>();
		for (Result leaf : sortedLeaves)
		{
			final String key = resultGroupIdentity(leaf).toLowerCase(Locale.ROOT);
			final List<Result> members = groups.computeIfAbsent(key, k -> new ArrayList<>());
			if (members.stream().noneMatch(member -> Objects.equals(member.getPoint(), leaf.getPoint())))
				members.add(leaf);
		}

		final List<Result> ranked = new ArrayList<>(Math.min(groups.size(), max));
		for (List<Result> members : groups.values())
		{
			ranked.add(toGroupedResult(members));
			if (ranked.size() >= max)
			{
				break;
			}
		}
		return ranked;
	}

	private static Result toGroupedResult(List<Result> members)
	{
		if (members.size() == 1)
		{
			return members.get(0);
		}

		final List<Result> children = new ArrayList<>(members);
		children.sort(MapFinder::compareByRelevanceThenDistance);
		final Result nearest = children.get(0);
		final String label = groupKey(nearest);
		return new Result(label, groupKey(nearest), nearest.getPoint(), nearest.getDistanceTiles(),
			nearest.getTier(), nearest.getKind(), children, nearest.getDetail(), nearest.getSellGp(), nearest.getSubjectEntry());
	}

	private static String groupKey(Result result)
	{
		return result.getGroupKey() != null ? result.getGroupKey() : result.getName();
	}

	private static int compareByRelevanceThenDistance(Result r1, Result r2)
	{
		final int t1 = r1.getTier();
		final int t2 = r2.getTier();
		if (t1 != t2)
		{
			return Integer.compare(t1, t2);
		}
		final int d1 = r1.getDistanceTiles();
		final int d2 = r2.getDistanceTiles();
		if (d1 < 0 && d2 < 0)
		{
			return 0;
		}
		if (d1 < 0)
		{
			return 1;
		}
		if (d2 < 0)
		{
			return -1;
		}
		return Integer.compare(d1, d2);
	}

	private static String resultGroupIdentity(Result result)
	{
		if (result.getSubjectEntry() != null) return result.getSubjectEntry().getSubject().getId();
		if (result.getKind() == Result.Kind.SHOP || result.getKind() == Result.Kind.GROUND_ITEM)
		{
			return result.getGroupKey();
		}
		return result.getKind() + ":" + groupKey(result);
	}

	private static int distanceToRegionCenter(MapRegion region, WorldPoint from)
	{
		return distanceTiles(new WorldPoint(region.getCenterX(), region.getCenterY(), 0), from);
	}

	public static int distanceTiles(WorldPoint point, WorldPoint from)
	{
		if (from == null || point == null)
		{
			return -1;
		}
		final int dx = point.getX() - from.getX();
		final int dy = point.getY() - from.getY();
		return (int) Math.sqrt((double) dx * dx + (double) dy * dy);
	}

	@Getter
	public static final class Result
	{
		/** What a result points at, so the panel can label it. */
		public enum Kind
		{
			PLACE,
			MONSTER,
			SHOP,
			GROUND_ITEM,
			MINERAL
		}

		private final String name;
		private final String groupKey;
		private final WorldPoint point;
		private final int distanceTiles;
		private final int tier;
		private final Kind kind;
		private final FinderPoiIndex.Entry subjectEntry;
		/** Child rows when this result groups duplicates. Empty for a plain leaf. */
		private final List<Result> children;
		/**
		 * Optional hover strip line (e.g. shop item cost/stock); null when unused.
		 * Hover description strip line, or null.
		 */
		private final String detail;
		/**
		 * Shop sell price in coins for the matched item, or {@code 0} when unknown / not a shop.
		 * Matched shop-item sell price in coins, or {@code 0}.
		 */
		private final int sellGp;

		public Result(String name, WorldPoint point, int distanceTiles)
		{
			this(name, name, point, distanceTiles, 0);
		}

		public Result(String name, String groupKey, WorldPoint point, int distanceTiles, int tier)
		{
			this(name, groupKey, point, distanceTiles, tier, Kind.PLACE, Collections.emptyList(), null, 0);
		}

		public Result(String name, String groupKey, WorldPoint point, int distanceTiles, int tier, Kind kind)
		{
			this(name, groupKey, point, distanceTiles, tier, kind, Collections.emptyList(), null, 0);
		}

		public Result(String name, String groupKey, WorldPoint point, int distanceTiles, int tier, Kind kind,
			String detail, int sellGp)
		{
			this(name, groupKey, point, distanceTiles, tier, kind, Collections.emptyList(), detail, sellGp);
		}

		Result(String name, String groupKey, WorldPoint point, int distanceTiles, int tier, Kind kind,
			List<Result> children)
		{
			this(name, groupKey, point, distanceTiles, tier, kind, children, null, 0);
		}

		Result(String name, String groupKey, WorldPoint point, int distanceTiles, int tier, Kind kind,
			List<Result> children, String detail, int sellGp)
		{
			this(name, groupKey, point, distanceTiles, tier, kind, children, detail, sellGp, null);
		}

		Result(String name, String groupKey, WorldPoint point, int distanceTiles, int tier, Kind kind,
			List<Result> children, String detail, int sellGp, FinderPoiIndex.Entry subjectEntry)
		{
			this.name = name;
			this.groupKey = groupKey;
			this.point = point;
			this.distanceTiles = distanceTiles;
			this.tier = tier;
			this.kind = kind == null ? Kind.PLACE : kind;
			this.subjectEntry = subjectEntry;
			this.children = children == null || children.isEmpty()
				? Collections.emptyList()
				: Collections.unmodifiableList(new ArrayList<>(children));
			this.detail = detail;
			this.sellGp = Math.max(0, sellGp);
		}

		Result withDistance(int distanceTiles)
		{
			if (this.distanceTiles == distanceTiles && children.isEmpty())
			{
				return this;
			}
			return withRanking(distanceTiles, tier);
		}

		Result withRanking(int distanceTiles, int tier)
		{
			return new Result(name, groupKey, point, distanceTiles, tier, kind, children, detail, sellGp, subjectEntry);
		}

		public String getIdentity()
		{
			return resultGroupIdentity(this);
		}







		/** True when the row should show a flyout affordance ({@code ▸}). */
		public boolean hasChildren()
		{
			return children.size() > 1;
		}
	}
}
