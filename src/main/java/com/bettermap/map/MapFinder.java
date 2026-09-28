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

import com.bettermap.BetterMapConfig;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.coords.WorldPoint;

@Singleton
public class MapFinder
{
	public static final int MAX_RESULTS = 8;
	static final int MAX_QUERY_LENGTH = 40;

	private final PoiIndex poiIndex;
	private final MonsterIndex monsterIndex;
	private final ShopIndex shopIndex;
	private final GroundItemIndex groundItemIndex;
	private final BetterMapConfig config;


	private volatile String query = "";
	private volatile List<Result> results = Collections.emptyList();
	private List<Result> queryMatches = Collections.emptyList();
	private volatile int matchCount;
	private volatile int locationMatchCount;
	private WorldPoint resultOrigin;
	private long searchDataVersion = Long.MIN_VALUE;
	private volatile int caret;
	/** The last non-empty query, so the "Last" quick-find chip can put it back. */
	private volatile String lastQuery = "";

	/** Raw browse leaves per region, rebuilt whenever either source publishes new data. */
	private final Map<MapRegion, List<Result>> regionLeaves = new HashMap<>();
	private final Map<MapRegion, BrowseCacheEntry> regionContentsCache = new HashMap<>();
	private long browseDataVersion = Long.MIN_VALUE;

	/**
	 * Test constructor: POI and monster search only, no shop / ground-item search and no config
	 * gate. Production uses the {@link Inject}-annotated constructor below.
	 */
	public MapFinder(PoiIndex poiIndex, MonsterIndex monsterIndex)
	{
		this(poiIndex, monsterIndex, null, null, null);
	}

	@Inject
	public MapFinder(PoiIndex poiIndex, MonsterIndex monsterIndex, ShopIndex shopIndex,
		GroundItemIndex groundItemIndex, BetterMapConfig config)
	{
		this.poiIndex = poiIndex;
		this.monsterIndex = monsterIndex;
		this.shopIndex = shopIndex;
		this.groundItemIndex = groundItemIndex;
		this.config = config;
	}

	private boolean itemSearchEnabled()
	{
		return config == null || config.finderSearchItems();
	}

	public String getQuery()
	{
		return query;
	}

	public List<Result> getResults()
	{
		return results;
	}

	public int getMatchCount()
	{
		return matchCount;
	}

	public int getLocationMatchCount()
	{
		return locationMatchCount;
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

	/** Closest physical match across the complete, untruncated candidate set. */
	public synchronized Result nearestResult()
	{
		Result nearest = null;
		for (Result result : queryMatches)
		{
			if (result.getDistanceTiles() >= 0
				&& (nearest == null || result.getDistanceTiles() < nearest.getDistanceTiles()))
			{
				nearest = result;
			}
		}
		return nearest != null ? nearest : selectedResult();
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
		if (cached != null && samePoint(cached.origin, from))
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

		final String lq = lowerQuery.toLowerCase(Locale.ROOT);
		if (name != null)
		{
			final String lowerName = name.toLowerCase(Locale.ROOT);
			if (lowerName.equals(lq))
			{
				return 0;
			}
			if (lowerName.startsWith(lq))
			{
				return 1;
			}
			if (lowerName.contains(lq))
			{
				return 2;
			}
		}

		if (locationName != null)
		{
			final String lowerLoc = locationName.toLowerCase(Locale.ROOT);
			if (lowerLoc.contains(lq))
			{
				return 3;
			}
		}

		return -1;
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
		this.caret = Math.max(0, Math.min(this.caret, next.length()));
		if (!next.isEmpty())
		{
			this.lastQuery = next;
		}

		// Trim only the search term, never the editable text: trimming the stored query ate every
		// trailing space, so a typed "king " came back as "king" and the next character produced
		// "kingb". Multi-word queries were unreachable by typing.
		final String lower = next.trim().toLowerCase(Locale.ROOT);
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

		for (PoiIndex.Poi poi : poiIndex.searchByName(lower, Integer.MAX_VALUE))
		{
			final int tier = matchTier(poi.getName(), null, lower);
			if (tier == -1)
			{
				continue;
			}
			final WorldPoint point = new WorldPoint(poi.getX(), poi.getY(), poi.getPlane());
			final int tiles = distanceTiles(point, from);
			final Result.Kind kind = "mining_site".equals(poi.getKey()) ? Result.Kind.MINERAL : Result.Kind.PLACE;
			candidates.add(new Result(poi.getName(), poi.getName(), point, tiles, tier, kind));
		}

		for (MonsterIndex.Zone zone : monsterIndex.searchByName(lower, Integer.MAX_VALUE))
		{
			final int tier = matchTier(zone.getMonster(), zone.getLocationName(), lower);
			if (tier == -1)
			{
				continue;
			}
			final WorldPoint point = new WorldPoint(zone.getX(), zone.getY(), zone.getPlane());
			final int tiles = distanceTiles(point, from);
			// Keep both the searched monster and its location visible in the result.
			candidates.add(new Result(zone.getMonster(), zone.getMonster(), point, tiles, tier,
				Result.Kind.MONSTER, zone.getLocationName(), 0));
		}

		if (itemSearchEnabled() && shopIndex != null && groundItemIndex != null)
		{
			final String term = next.trim();

			for (ShopIndex.Shop shop : shopIndex.all())
			{
				final ShopIndex.StockItem matched = shop.stockItemMatch(lower);
				final String matchName = matched != null ? matched.getName() : shop.stockMatch(lower);
				final int itemTier = matchTier(matchName, null, lower);
				final int shopTier = matchTier(shop.getName(), null, lower);
				if (itemTier < 0 && shopTier < 0)
				{
					continue;
				}
				final int tier = itemTier >= 0 ? itemTier : shopTier + 4;
				final WorldPoint point = new WorldPoint(shop.getX(), shop.getY(), shop.getPlane());
				final int tiles = distanceTiles(point, from);
				final String detail = matched != null ? matched.toDetailLine() : null;
				final int sellGp = matched != null ? matched.getSell() : 0;
				final String itemName = matchName != null ? matchName : shop.getName();
				final String subtitle = matched != null ? shop.getName() + " · " + matched.toDetailLine() : "Shop name match";
				candidates.add(new Result(itemName, "Shop item: " + itemName,
					point, tiles, tier, Result.Kind.SHOP, subtitle, sellGp));
			}

			for (GroundItemIndex.Spawn spawn : groundItemIndex.searchByName(lower, Integer.MAX_VALUE))
			{
				final String itemName = spawn.matchingItemName(lower);
				int tier = matchTier(itemName, null, lower);
				if (tier < 0)
				{
					tier = 2;
				}
				final WorldPoint point = new WorldPoint(spawn.getX(), spawn.getY(), spawn.getPlane());
				final int tiles = distanceTiles(point, from);
				final String matchedName = itemName != null ? itemName : term;
				final String location = spawn.getLocation() != null && !spawn.getLocation().isEmpty()
					? spawn.getLocation() : "Ground spawn";
				candidates.add(new Result(location, "Ground item: " + matchedName,
					point, tiles, tier, Result.Kind.GROUND_ITEM, "Item: " + matchedName, 0));
			}
		}

		this.queryMatches = Collections.unmodifiableList(candidates);
		this.resultOrigin = from;
		this.searchDataVersion = currentSearchDataVersion();
		publishRankedResults(candidates);
	}

	/** Refresh distance ordering after movement without rescanning source datasets. */
	public synchronized void refresh(WorldPoint from)
	{
		if (query == null || query.trim().isEmpty())
		{
			return;
		}
		if (searchDataVersion != currentSearchDataVersion())
		{
			updateQuery(query, from);
			return;
		}
		if (samePoint(resultOrigin, from))
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
		locationMatchCount = candidates.size();
		matchCount = allGroups.size();
		results = Collections.unmodifiableList(new ArrayList<>(
			allGroups.subList(0, Math.min(MAX_RESULTS, allGroups.size()))));
	}

	public synchronized void appendChar(char c, WorldPoint from)
	{
		caretEnd();
		insert(String.valueOf(c), from);
	}

	public synchronized void appendText(String text, WorldPoint from)
	{
		if (text == null)
		{
			return;
		}
		caretEnd();
		insert(text, from);
	}

	public synchronized void backspace(WorldPoint from)
	{
		caretEnd();
		deleteBackward(from);
	}

	/** Inserts {@code text} at the caret and leaves the caret after it. */
	public synchronized void insert(String text, WorldPoint from)
	{
		if (text == null || text.isEmpty())
		{
			return;
		}
		final int at = clampCaret(caret);
		final String next = query.substring(0, at) + text + query.substring(at);
		final int wanted = at + text.length();
		updateQuery(next, from);
		this.caret = clampCaret(wanted);
	}

	/** Backspace: removes the character before the caret. */
	public synchronized void deleteBackward(WorldPoint from)
	{
		final int at = clampCaret(caret);
		if (at == 0)
		{
			return;
		}
		final String next = query.substring(0, at - 1) + query.substring(at);
		updateQuery(next, from);
		this.caret = clampCaret(at - 1);
	}

	/** Delete: removes the character after the caret. */
	public synchronized void deleteForward(WorldPoint from)
	{
		final int at = clampCaret(caret);
		if (at >= query.length())
		{
			return;
		}
		final String next = query.substring(0, at) + query.substring(at + 1);
		updateQuery(next, from);
		this.caret = clampCaret(at);
	}

	public synchronized void moveCaret(int delta)
	{
		this.caret = clampCaret(caret + delta);
	}

	public synchronized void caretHome()
	{
		this.caret = 0;
	}

	public synchronized void caretEnd()
	{
		this.caret = query.length();
	}

	public int getCaret()
	{
		return clampCaret(caret);
	}

	/** The last non-empty query, for the "Last" quick-find chip. Survives {@link #clearQuery()}. */
	public String getLastQuery()
	{
		return lastQuery;
	}

	private int clampCaret(int value)
	{
		return Math.max(0, Math.min(value, query.length()));
	}

	public synchronized void clearQuery()
	{
		this.query = "";
		this.caret = 0;
		this.results = Collections.emptyList();
		this.queryMatches = Collections.emptyList();
		this.matchCount = 0;
		this.locationMatchCount = 0;
		this.resultOrigin = null;
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

		for (PoiIndex.Poi poi : poiIndex.all())
		{
			final MapRegion region = MapRegion.of(poi.getX(), poi.getY(), poi.getPlane());
			if (region == null)
			{
				continue;
			}
			final WorldPoint point = new WorldPoint(poi.getX(), poi.getY(), poi.getPlane());
			regionLeaves.computeIfAbsent(region, k -> new ArrayList<>())
				.add(new Result(poi.getName(), poi.getName(), point, -1, 0));
		}

		for (MonsterIndex.Zone zone : monsterIndex.getZones())
		{
			final MapRegion region = MapRegion.of(zone.getX(), zone.getY(), zone.getPlane());
			if (region == null)
			{
				continue;
			}
			final WorldPoint point = new WorldPoint(zone.getX(), zone.getY(), zone.getPlane());
			regionLeaves.computeIfAbsent(region, k -> new ArrayList<>())
				.add(new Result(zone.getLocationName(), zone.getMonster(), point, -1, 0));
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

	private static boolean samePoint(WorldPoint a, WorldPoint b)
	{
		return a == null ? b == null : a.equals(b);
	}

	private static final class BrowseCacheEntry
	{
		private final WorldPoint origin;
		private final List<Result> results;

		private BrowseCacheEntry(WorldPoint origin, List<Result> results)
		{
			this.origin = origin;
			this.results = results;
		}
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
			groups.computeIfAbsent(key, k -> new ArrayList<>()).add(leaf);
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
		final String label = resultGroupLabel(nearest, members.size());
		return new Result(label, groupKey(nearest), nearest.getPoint(), nearest.getDistanceTiles(),
			nearest.getTier(), nearest.getKind(), children, nearest.getDetail(), nearest.getSellGp());
	}

	private static String resultGroupLabel(Result result, int count)
	{
		final String key = groupKey(result);
		if (result.getKind() == Result.Kind.SHOP)
		{
			return key + " · " + count + " location" + (count == 1 ? "" : "s");
		}
		if (result.getKind() == Result.Kind.GROUND_ITEM)
		{
			return key + " · " + count + " location" + (count == 1 ? "" : "s");
		}
		return key;
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
		final int distanceOrder = Integer.compare(distanceBand(d1), distanceBand(d2));
		return distanceOrder != 0 ? distanceOrder : Integer.compare(d1, d2);
	}

	private static String resultGroupIdentity(Result result)
	{
		if (result.getKind() == Result.Kind.SHOP || result.getKind() == Result.Kind.GROUND_ITEM)
		{
			return result.getGroupKey();
		}
		return result.getKind() + ":" + groupKey(result);
	}

	private static int distanceBand(int tiles)
	{
		if (tiles < 0) return Integer.MAX_VALUE;
		if (tiles <= 20) return 0;
		if (tiles <= 100) return 1;
		return 2;
	}

	private static int distanceToRegionCenter(MapRegion region, WorldPoint from)
	{
		return distanceTiles(new WorldPoint(region.getCenterX(), region.getCenterY(), 0), from);
	}

	private static int distanceTiles(WorldPoint point, WorldPoint from)
	{
		if (from == null || point == null)
		{
			return -1;
		}
		final int dx = point.getX() - from.getX();
		final int dy = point.getY() - from.getY();
		return (int) Math.sqrt((double) dx * dx + (double) dy * dy);
	}

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
		private final List<Result> children;
		/** Optional hover strip line (e.g. shop item cost/stock); null when unused. */
		private final String detail;
		/** Shop sell price in coins for the matched item, or {@code 0} when unknown / not a shop. */
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
			this.name = name;
			this.groupKey = groupKey;
			this.point = point;
			this.distanceTiles = distanceTiles;
			this.tier = tier;
			this.kind = kind == null ? Kind.PLACE : kind;
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
			return new Result(name, groupKey, point, distanceTiles, tier, kind, children, detail, sellGp);
		}

		public String getName()
		{
			return name;
		}

		public String getGroupKey()
		{
			return groupKey;
		}

		public WorldPoint getPoint()
		{
			return point;
		}

		public int getDistanceTiles()
		{
			return distanceTiles;
		}

		public int getTier()
		{
			return tier;
		}

		public Kind getKind()
		{
			return kind;
		}

		/** Child rows when this result groups duplicates. Empty for a plain leaf. */
		public List<Result> getChildren()
		{
			return children;
		}

		/** Hover description strip line, or null. */
		public String getDetail()
		{
			return detail;
		}

		/** Matched shop-item sell price in coins, or {@code 0}. */
		public int getSellGp()
		{
			return sellGp;
		}

		/** True when the row should show a flyout affordance ({@code ▸}). */
		public boolean hasChildren()
		{
			return children.size() > 1;
		}
	}
}
