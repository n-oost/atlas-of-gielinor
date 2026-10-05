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

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

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


	private static final String RESOURCE = "/com/bettermap/data/shops.json.gz";
	private static final int CLIP = 88;

	private final List<Shop> shops = new ArrayList<>();
	private final Map<Long, List<Shop>> chunkMap = new HashMap<>();

	private volatile boolean loaded;
	private volatile long dataVersion;

	public long getDataVersion()
	{
		return dataVersion;
	}

	private static long chunkKey(int plane, int chunkX, int chunkY)
	{
		return (((long) plane & 0x3L) << 32) | (((long) (chunkX & 0xFFFF) << 16)) | ((long) (chunkY & 0xFFFF));
	}

	public boolean isLoaded()
	{
		return loaded;
	}

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
		final int minChunkX = minWorldX >> 6;
		final int maxChunkX = maxWorldX >> 6;
		final int minChunkY = minWorldY >> 6;
		final int maxChunkY = maxWorldY >> 6;

		for (int cx = minChunkX; cx <= maxChunkX; cx++)
		{
			for (int cy = minChunkY; cy <= maxChunkY; cy++)
			{
				final List<Shop> chunkShops = chunkMap.get(chunkKey(plane, cx, cy));
				if (chunkShops == null)
				{
					continue;
				}

				for (Shop shop : chunkShops)
				{
					if (shop.x >= minWorldX && shop.x <= maxWorldX
						&& shop.y >= minWorldY && shop.y <= maxWorldY)
					{
						consumer.accept(shop);
					}
				}
			}
		}
	}

	/** Parses the bundled dataset. Call off the client thread. */
	public void load()
	{
		final List<Shop> parsed = new ArrayList<>();

		try (InputStream rawIn = ShopIndex.class.getResourceAsStream(RESOURCE))
		{
			if (rawIn == null)
			{
				log.warn("Shop dataset {} is missing from the plugin resources", RESOURCE);
				return;
			}

			final InputStream in = new java.util.zip.GZIPInputStream(rawIn);
			final Dataset dataset = gson.fromJson(
				new InputStreamReader(in, StandardCharsets.UTF_8), Dataset.class);

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

		final Map<Long, List<Shop>> spatial = new HashMap<>();
		for (Shop shop : consolidated)
		{
			spatial.computeIfAbsent(chunkKey(shop.plane, shop.x >> 6, shop.y >> 6), k -> new ArrayList<>())
				.add(shop);
		}

		synchronized (this)
		{
			shops.clear();
			shops.addAll(consolidated);
			chunkMap.clear();
			chunkMap.putAll(spatial);
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
		final int minChunkX = ((int) Math.floor(worldX - radius)) >> 6;
		final int maxChunkX = ((int) Math.ceil(worldX + radius)) >> 6;
		final int minChunkY = ((int) Math.floor(worldY - radius)) >> 6;
		final int maxChunkY = ((int) Math.ceil(worldY + radius)) >> 6;

		Shop best = null;
		double bestDistance = Double.MAX_VALUE;

		for (int cx = minChunkX; cx <= maxChunkX; cx++)
		{
			for (int cy = minChunkY; cy <= maxChunkY; cy++)
			{
				final List<Shop> chunkShops = chunkMap.get(chunkKey(plane, cx, cy));
				if (chunkShops == null)
				{
					continue;
				}

				for (Shop shop : chunkShops)
				{
					final double sx = shop.x + 0.5 + shop.displayOffsetX;
					final double sy = shop.y + 0.5 + shop.displayOffsetY;
					final double dx = sx - worldX;
					final double dy = sy - worldY;
					if (Math.abs(dx) > radius || Math.abs(dy) > radius)
					{
						continue;
					}

					final double distance = dx * dx + dy * dy;
					if (distance < bestDistance)
					{
						bestDistance = distance;
						best = shop;
					}
				}
			}
		}

		return best;
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
		final int minChunkX = (of.x - tiles) >> 6;
		final int maxChunkX = (of.x + tiles) >> 6;
		final int minChunkY = (of.y - tiles) >> 6;
		final int maxChunkY = (of.y + tiles) >> 6;

		for (int cx = minChunkX; cx <= maxChunkX && found.size() < limit; cx++)
		{
			for (int cy = minChunkY; cy <= maxChunkY && found.size() < limit; cy++)
			{
				final List<Shop> chunkShops = chunkMap.get(chunkKey(of.plane, cx, cy));
				if (chunkShops == null)
				{
					continue;
				}

				for (Shop shop : chunkShops)
				{
					if (shop == of)
					{
						continue;
					}
					final int dx = Math.abs(shop.x - of.x);
					final int dy = Math.abs(shop.y - of.y);
					if (dx <= tiles && dy <= tiles)
					{
						found.add(shop);
						if (found.size() >= limit)
						{
							return found;
						}
					}
				}
			}
		}

		return found;
	}

	/** One SKU in a shop's inventory, with default stock and shop sell price when known. */
	public static final class StockItem
	{
		private String name;
		private int stock;
		/** Shop sell price in coins at default stock, or {@code 0} when unknown. */
		private int sell;

		public String getName()
		{
			return name;
		}

		public int getStock()
		{
			return stock;
		}

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
		private String name;
		private String owner;
		private String location;
		private String icon;
		private boolean members;
		private String special;
		private List<String> services;
		private List<String> notable;
		private String bulk;
		@SerializedName("bulk_stock")
		private int bulkStock;
		@SerializedName("stock_total")
		private int stockTotal;
		/** Full inventory rows from the wiki StoreLine table (may be empty for service shops). */
		private List<StockItem> stock;
		@SerializedName("sell_pct")
		private int sellPct;
		@SerializedName("buy_pct")
		private int buyPct;
		private int x;
		private int y;
		private int plane;
		private boolean approx;

		private double displayOffsetX;
		private double displayOffsetY;

		public double getDisplayOffsetX()
		{
			return displayOffsetX;
		}

		public double getDisplayOffsetY()
		{
			return displayOffsetY;
		}

		public void setDisplayOffset(double dx, double dy)
		{
			this.displayOffsetX = dx;
			this.displayOffsetY = dy;
		}

		public String getName()
		{
			return name;
		}

		public String getOwner()
		{
			return owner;
		}

		public String getLocation()
		{
			return location;
		}

		public String getIcon()
		{
			return icon;
		}

		public boolean isMembers()
		{
			return members;
		}

		public String getSpecial()
		{
			return special;
		}

		public List<String> getServices()
		{
			return services == null ? Collections.emptyList() : services;
		}

		public List<String> getNotable()
		{
			return notable == null ? Collections.emptyList() : notable;
		}

		public String getBulk()
		{
			return bulk;
		}

		public int getBulkStock()
		{
			return bulkStock;
		}

		public int getStockTotal()
		{
			return stockTotal;
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

		public boolean isApprox()
		{
			return approx;
		}

		public List<StockItem> getStock()
		{
			return stock == null ? Collections.emptyList() : stock;
		}

		public int getSellPct()
		{
			return sellPct;
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
