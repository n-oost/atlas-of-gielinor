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
package bettermap.map;

import bettermap.data.BundledJson;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import lombok.Getter;

/**
 * Shops from the bundled wiki-derived dataset ({@code shops.json}).
 *
 * <p>Kept out of {@link PoiIndex} on purpose: the legend labels a group from its first entry's
 * name, so merging shops in would title the whole Shops group after whichever shop landed first.
 */
@Slf4j
@Singleton
public class ShopIndex
{
	private final Gson gson;

	@Inject
	public ShopIndex(Gson gson)
	{
		this.gson = gson;
	}

	private static final String RESOURCE = "/bettermap/data/shops.json.gz";
	private static final int CLIP = 88;

	private final List<Shop> shops = new ArrayList<>();
	private SpatialIndex<Shop> spatialIndex = new SpatialIndex<>(
		Collections.emptyList(), Shop::getPlane, Shop::getX, Shop::getY);

	@Getter
	private volatile boolean loaded;
	@Getter
	private volatile long dataVersion;

	public synchronized int size()
	{
		return shops.size();
	}

	public synchronized List<Shop> all()
	{
		return Collections.unmodifiableList(shops);
	}

	/** Visits shops on the given plane inside the world-coordinate box. */
	public synchronized void forEachInArea(int plane, int minWorldX, int maxWorldX, int minWorldY, int maxWorldY,
		Consumer<Shop> consumer)
	{
		spatialIndex.forEachInArea(plane, minWorldX, maxWorldX, minWorldY, maxWorldY, consumer);
	}

	/** Parses the bundled dataset. Call off the client thread. */
	public void load()
	{
		final List<Shop> parsed = new ArrayList<>();

		try
		{
			final Dataset dataset = BundledJson.readGzip(gson, RESOURCE, Dataset.class);

			if (dataset == null || dataset.shops == null)
			{
				return;
			}

			for (Shop shop : dataset.shops)
			{
				if (shop == null || shop.name == null || shop.name.isEmpty())
				{
					continue;
				}
				if (shop.x <= 0 || shop.y <= 0)
				{
					continue;
				}
				shop.plane = Math.max(0, Math.min(3, shop.plane));
				if (shop.notable == null)
				{
					shop.notable = Collections.emptyList();
				}
				if (shop.services == null)
				{
					shop.services = Collections.emptyList();
				}
				if (shop.stock == null)
				{
					shop.stock = Collections.emptyList();
				}
				parsed.add(shop);
			}
		}
		catch (IOException | RuntimeException e)
		{
			log.warn("Could not read the shop dataset", e);
			return;
		}

		// Consolidate same-owner shop pairs at the same location (Groups 22, 24, 25, 28, 32)
		final List<Shop> consolidated = consolidateSameOwnerShops(parsed);

		// Assign display-only offsets for distinct shops sharing a location (Groups 23, 26, 27, 29, 30, 31)
		assignDistinctShopOffsets(consolidated);

		final SpatialIndex<Shop> spatial = new SpatialIndex<>(
			consolidated, Shop::getPlane, Shop::getX, Shop::getY);

		synchronized (this)
		{
			shops.clear();
			shops.addAll(consolidated);
			spatialIndex = spatial;
		}

		loaded = true;
		dataVersion++;
		log.info("Loaded {} shops", parsed.size());
	}

	/**
	 * Shops that stock an item matching {@code query} (case-insensitive), at most {@code cap}.
	 * Matches full {@code stock} rows first, then the stock hints ({@code notable}, {@code bulk},
	 * {@code special}), never the shop's own name. Callers should pass a high {@code cap} and
	 * rank by distance themselves — this walk is in dataset order.
	 */
	public synchronized List<Shop> searchByName(String query, int cap)
	{
		final List<Shop> matches = new ArrayList<>();
		if (query == null || query.isEmpty() || cap <= 0)
		{
			return matches;
		}

		final String lower = query.toLowerCase(Locale.ROOT);
		for (Shop shop : shops)
		{
			if (shop.stockItemMatch(lower) != null)
			{
				matches.add(shop);
				if (matches.size() >= cap)
				{
					break;
				}
			}
		}

		return matches;
	}

	/** The shop whose marker is nearest this point on the given plane, or null. */
	public synchronized Shop nearest(double worldX, double worldY, int plane, double radius)
	{
		return spatialIndex.nearest(plane, worldX, worldY, radius,
			shop -> shop.x + 0.5 + shop.displayOffsetX,
			shop -> shop.y + 0.5 + shop.displayOffsetY);
	}

	/** The shop whose marker is nearest this point on the given plane, or null. */
	public synchronized Shop nearest(int worldX, int worldY, int plane, int radius)
	{
		return nearest(worldX + 0.5, worldY + 0.5, plane, (double) radius);
	}

	/** Other shops within {@code tiles} of {@code of}, excluding itself, capped at {@code limit}. */
	public synchronized List<Shop> othersNear(Shop of, int tiles, int limit)
	{
		if (of == null || limit <= 0)
		{
			return Collections.emptyList();
		}

		final List<Shop> found = new ArrayList<>(limit);
		spatialIndex.forEachInArea(of.plane, of.x - tiles, of.x + tiles, of.y - tiles, of.y + tiles, shop ->
		{
			if (shop != of && found.size() < limit)
			{
				found.add(shop);
			}
		});

		return found;
	}

	/** One SKU in a shop's inventory, with default stock and shop sell price when known. */
	public static final class StockItem
	{
		@Getter
		private String name;
		@Getter
		private int stock;
		/** Shop sell price in coins at default stock, or {@code 0} when unknown. */
		private int sell;

		/** Shop sell price in coins, or {@code 0} when unknown. */
		public int getSell()
		{
			return sell;
		}

		/** True when the wiki lists infinite stock ({@code -1}). */
		public boolean isInfinite()
		{
			return stock < 0;
		}

		/** Hover / route line: {@code Bread · 10 stock · 12 gp}. */
		public String toDetailLine()
		{
			final StringBuilder line = new StringBuilder();
			line.append(name != null ? name : "Item");
			line.append(" · ");
			if (isInfinite())
			{
				line.append("∞ stock");
			}
			else
			{
				line.append(stock).append(" stock");
			}
			if (sell > 0)
			{
				line.append(" · ").append(sell).append(" gp");
			}
			return line.toString();
		}
	}

	public static final class Shop
	{
		@Getter
		private String name;
		@Getter
		private String owner;
		@Getter
		private String location;
		@Getter
		private String icon;
		@Getter
		private boolean members;
		@Getter
		private String special;
		private List<String> services;
		private List<String> notable;
		@Getter
		private String bulk;
		@SerializedName("bulk_stock")
		@Getter
		private int bulkStock;
		@SerializedName("stock_total")
		@Getter
		private int stockTotal;
		/** Full inventory rows from the wiki StoreLine table (may be empty for service shops). */
		private List<StockItem> stock;
		@SerializedName("sell_pct")
		@Getter
		private int sellPct;
		@SerializedName("buy_pct")
		private int buyPct;
		@Getter
		private int x;
		@Getter
		private int y;
		@Getter
		private int plane;
		@Getter
		private boolean approx;

		@Getter
		private double displayOffsetX;
		@Getter
		private double displayOffsetY;

		public void setDisplayOffset(double dx, double dy)
		{
			this.displayOffsetX = dx;
			this.displayOffsetY = dy;
		}

		public List<String> getServices()
		{
			return services == null ? Collections.emptyList() : services;
		}

		public List<String> getNotable()
		{
			return notable == null ? Collections.emptyList() : notable;
		}

		public List<StockItem> getStock()
		{
			return stock == null ? Collections.emptyList() : stock;
		}

		/**
		 * Best inventory / hint match for {@code lowerQuery}, or null. Prefers exact name, then
		 * prefix, then contains, scanning full {@code stock} before notable/bulk/special hints.
		 * {@code lowerQuery} must already be lower-cased.
		 */
		public StockItem stockItemMatch(String lowerQuery)
		{
			if (lowerQuery == null || lowerQuery.isEmpty())
			{
				return null;
			}

			StockItem best = null;
			int bestTier = 4;
			if (stock != null)
			{
				for (StockItem item : stock)
				{
					if (item == null || item.name == null || item.name.isEmpty())
					{
						continue;
					}
					final int tier = nameMatchTier(item.name, lowerQuery);
					if (tier >= 0 && tier < bestTier)
					{
						best = item;
						bestTier = tier;
						if (tier == 0)
						{
							return best;
						}
					}
				}
			}
			if (best != null)
			{
				return best;
			}

			// Fall back to notable / bulk / special as synthetic StockItems (no qty/price).
			final String hint = hintMatch(lowerQuery);
			if (hint == null)
			{
				return null;
			}
			final StockItem synthetic = new StockItem();
			synthetic.name = hint;
			synthetic.stock = 0;
			synthetic.sell = 0;
			return synthetic;
		}

		/**
		 * The stock item or hint name that contains {@code lowerQuery}, or null when this shop
		 * does not stock it. {@code lowerQuery} must already be lower-cased.
		 */
		public String stockMatch(String lowerQuery)
		{
			final StockItem item = stockItemMatch(lowerQuery);
			return item != null ? item.getName() : null;
		}

		private String hintMatch(String lowerQuery)
		{
			if (notable != null)
			{
				for (String entry : notable)
				{
					if (entry != null && entry.toLowerCase(Locale.ROOT).contains(lowerQuery))
					{
						return entry;
					}
				}
			}
			if (bulk != null && bulk.toLowerCase(Locale.ROOT).contains(lowerQuery))
			{
				return bulk;
			}
			if (special != null && special.toLowerCase(Locale.ROOT).contains(lowerQuery))
			{
				return special;
			}
			return null;
		}

		/** 0 exact, 1 prefix, 2 contains, or -1. */
		private static int nameMatchTier(String name, String lowerQuery)
		{
			final String lower = name.toLowerCase(Locale.ROOT);
			if (lower.equals(lowerQuery))
			{
				return 0;
			}
			if (lower.startsWith(lowerQuery))
			{
				return 1;
			}
			if (lower.contains(lowerQuery))
			{
				return 2;
			}
			return -1;
		}

		/**
		 * Builds the hover card. Hard budget of 7 content lines before the caller appends coords.
		 */
		public PoiDetails.Detail toDetail(List<Shop> alsoHere)
		{
			final List<String> lines = new ArrayList<>(7);

			if (owner != null && !owner.isEmpty())
			{
				lines.add(clip("Owner: " + owner));
			}
			if (special != null && !special.isEmpty())
			{
				lines.add(clip("Sells: " + special));
			}
			if (stockTotal > 0)
			{
				if (stockTotal <= 6 && notable != null && !notable.isEmpty())
				{
					lines.add(clip("Stock: " + String.join(", ", notable)));
				}
				else if (stockTotal <= 6 && bulk != null)
				{
					lines.add(clip("Stock: " + bulk));
				}
				else
				{
					lines.add("Stock: " + stockTotal + " items");
				}
			}
			if (stockTotal > 6 && notable != null && !notable.isEmpty())
			{
				lines.add(clip("Notable: " + String.join(", ", notable)));
			}
			if (services != null)
			{
				for (String service : services)
				{
					if (service != null && !service.isEmpty() && lines.size() < 7)
					{
						lines.add(clip("Services: " + service));
					}
				}
			}
			if (bulk != null && bulkStock >= 20 && lines.size() < 7)
			{
				lines.add(clip("Best source: " + bulk + " (" + bulkStock + ")"));
			}
			if (members && lines.size() < 7)
			{
				lines.add("Members only");
			}
			if (approx && lines.size() < 7)
			{
				lines.add("Approximate location");
			}
			if (alsoHere != null && !alsoHere.isEmpty() && lines.size() < 7)
			{
				final StringBuilder also = new StringBuilder("Also here: ");
				for (int i = 0; i < alsoHere.size(); i++)
				{
					if (i > 0)
					{
						also.append(", ");
					}
					also.append(alsoHere.get(i).name);
				}
				lines.add(clip(also.toString()));
			}

			while (lines.size() > 7)
			{
				lines.remove(lines.size() - 1);
			}

			final String category = location != null && !location.isEmpty()
				? "Shops • " + location
				: "Shops";
			return new PoiDetails.Detail(name, category, lines);
		}

		private static String clip(String text)
		{
			if (text == null)
			{
				return "";
			}
			if (text.length() <= CLIP)
			{
				return text;
			}
			return text.substring(0, CLIP - 1) + "…";
		}
	}

	private static long tileKey(int plane, int x, int y)
	{
		return (((long) plane & 0x3L) << 32) | (((long) (x & 0xFFFF) << 16)) | ((long) (y & 0xFFFF));
	}

	private static List<Shop> consolidateSameOwnerShops(List<Shop> input)
	{
		final Map<Long, List<Shop>> byTile = new java.util.LinkedHashMap<>();
		for (Shop s : input)
		{
			byTile.computeIfAbsent(tileKey(s.plane, s.x, s.y), k -> new ArrayList<>()).add(s);
		}

		final List<Shop> result = new ArrayList<>(input.size());
		for (List<Shop> tileShops : byTile.values())
		{
			final Map<String, List<Shop>> byOwner = new java.util.LinkedHashMap<>();
			for (Shop s : tileShops)
			{
				final String ownerKey = s.owner != null && !s.owner.isBlank()
					? s.owner.trim().toLowerCase(Locale.ROOT)
					: ("__no_owner__" + s.name);
				byOwner.computeIfAbsent(ownerKey, k -> new ArrayList<>()).add(s);
			}

			for (List<Shop> ownerShops : byOwner.values())
			{
				if (ownerShops.size() == 1)
				{
					result.add(ownerShops.get(0));
				}
				else
				{
					result.add(mergeOwnerShops(ownerShops));
				}
			}
		}
		return result;
	}

	private static Shop mergeOwnerShops(List<Shop> group)
	{
		final Shop primary = group.get(0);
		final java.util.Set<String> notable = new java.util.LinkedHashSet<>();
		final java.util.Set<String> services = new java.util.LinkedHashSet<>();
		final List<StockItem> allStock = new ArrayList<>();
		int totalStock = 0;

		for (Shop s : group)
		{
			if (s.notable != null) notable.addAll(s.notable);
			if (s.services != null) services.addAll(s.services);
			if (s.stock != null) allStock.addAll(s.stock);
			totalStock += s.stockTotal;
		}

		if (primary.owner != null && primary.owner.equalsIgnoreCase("Yrsa"))
		{
			primary.name = "Yrsa's Accoutrements & Shoe Store";
			primary.special = "Clothes & shoes";
		}
		else if (primary.owner != null && primary.owner.equalsIgnoreCase("Gabooty"))
		{
			primary.name = "Gabooty's Cooperative & Drinky Store";
			primary.special = "Machetes, clothing, cocktails";
		}
		else if (primary.owner != null && primary.owner.equalsIgnoreCase("Sir Tiffy Cashien"))
		{
			primary.name = "Initiate & Proselyte Temple Knight Armoury";
			primary.special = "Initiate & Proselyte armour";
		}
		else if (primary.owner != null && primary.owner.equalsIgnoreCase("Thessalia"))
		{
			primary.name = "Thessalia's Fine Clothes & Makeovers";
			primary.special = "Clothes shop & makeovers";
		}
		else if (primary.owner != null && primary.owner.equalsIgnoreCase("Razmire Keelgan"))
		{
			primary.name = "Razmire Builders Merchants & General Store";
			primary.special = "Building supplies, general store";
		}

		primary.notable = new ArrayList<>(notable);
		primary.services = new ArrayList<>(services);
		primary.stock = allStock;
		primary.stockTotal = totalStock;
		return primary;
	}

	private static void assignDistinctShopOffsets(List<Shop> shops)
	{
		final Map<Long, List<Shop>> byTile = new java.util.LinkedHashMap<>();
		for (Shop s : shops)
		{
			byTile.computeIfAbsent(tileKey(s.plane, s.x, s.y), k -> new ArrayList<>()).add(s);
		}

		for (List<Shop> tileShops : byTile.values())
		{
			if (tileShops.size() == 2)
			{
				tileShops.get(0).setDisplayOffset(-0.35, 0.0);
				tileShops.get(1).setDisplayOffset(0.35, 0.0);
			}
			else if (tileShops.size() == 3)
			{
				tileShops.get(0).setDisplayOffset(-0.45, 0.0);
				tileShops.get(1).setDisplayOffset(0.0, 0.0);
				tileShops.get(2).setDisplayOffset(0.45, 0.0);
			}
			else if (tileShops.size() > 3)
			{
				final double step = 0.8 / (tileShops.size() - 1);
				for (int i = 0; i < tileShops.size(); i++)
				{
					tileShops.get(i).setDisplayOffset(-0.4 + i * step, 0.0);
				}
			}
		}
	}

	private static final class Dataset
	{
		private List<Shop> shops;
	}
}
