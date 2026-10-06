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
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import lombok.Getter;

/**
 * Ground item spawns, from the bundled wiki-derived dataset: 370 items over 4,361 world tiles.
 *
 * <p>Indexed by tile, not by item. A tile carrying two spawns is one marker with two items in it,
 * because that is what the player sees standing there.
 *
 * <p>Nothing is filtered at load: the dataset keeps buckets and cabbages alongside rune items, and
 * {@link bettermap.BetterMapConfig#groundItemMinValue()} decides at draw time what is
 * worth showing. A tile's {@link Spawn#getValue() value} is the best single item on it.
 */
@Slf4j
@Singleton
public class GroundItemIndex
{
	private final Gson gson;

	@Inject
	public GroundItemIndex(Gson gson)
	{
		this.gson = gson;
	}

	private static final String RESOURCE = "/bettermap/data/ground_items.json.gz";

	private final List<Spawn> spawns = new ArrayList<>();
	private SpatialIndex<Spawn> spatialIndex = new SpatialIndex<>(
		Collections.emptyList(), Spawn::getPlane, Spawn::getX, Spawn::getY);

	@Getter
	private volatile boolean loaded;
	@Getter
	private volatile long dataVersion;

	public synchronized int size()
	{
		return spawns.size();
	}

	public synchronized List<Spawn> all()
	{
		return Collections.unmodifiableList(spawns);
	}

	/** Parses the bundled dataset. Call off the client thread. */
	public void load()
	{
		final List<Spawn> parsed = new ArrayList<>();

		try
		{
			final Dataset dataset = BundledJson.readGzip(gson, RESOURCE, Dataset.class);

			if (dataset == null || dataset.spawns == null)
			{
				return;
			}

			for (Spawn spawn : dataset.spawns)
			{
				if (spawn == null || spawn.items == null || spawn.items.isEmpty())
				{
					continue;
				}
				if (spawn.x <= 0 || spawn.y <= 0)
				{
					continue;
				}
				spawn.plane = Math.max(0, Math.min(3, spawn.plane));
				spawn.items = Collections.unmodifiableList(spawn.items);
				parsed.add(spawn);
			}
		}
		catch (IOException | RuntimeException e)
		{
			log.warn("Could not read the ground item dataset", e);
			return;
		}

		final SpatialIndex<Spawn> spatial = new SpatialIndex<>(
			parsed, Spawn::getPlane, Spawn::getX, Spawn::getY);

		synchronized (this)
		{
			spawns.clear();
			spawns.addAll(parsed);
			spatialIndex = spatial;
		}

		loaded = true;
		dataVersion++;
		log.info("Loaded {} ground item spawn tiles", parsed.size());
	}

	/**
	 * Spawn tiles carrying an item whose name contains {@code query} (case-insensitive), at most
	 * {@code cap}. Not value-filtered: an explicit item search returns the item at any price.
	 */
	public synchronized List<Spawn> searchByName(String query, int cap)
	{
		final List<Spawn> matches = new ArrayList<>();
		if (query == null || query.isEmpty() || cap <= 0)
		{
			return matches;
		}

		final String lower = query.toLowerCase(Locale.ROOT);
		for (Spawn spawn : spawns)
		{
			if (spawn.matchingItemName(lower) != null)
			{
				matches.add(spawn);
				if (matches.size() >= cap)
				{
					break;
				}
			}
		}

		return matches;
	}

	/** The spawn tile nearest this point on the given plane worth at least {@code minValue}, or null. */
	public synchronized Spawn nearest(int worldX, int worldY, int plane, int radius, int minValue)
	{
		return spatialIndex.nearest(plane, worldX, worldY, radius, s -> s.getValue() >= minValue);
	}

	/**
	 * Invokes {@code consumer} for every spawn tile on {@code plane} inside the world bounding box.
	 * Chunk-indexed so the overlay never scans all 4,361 tiles per frame.
	 */
	public synchronized void forEachInArea(int plane, int minWorldX, int maxWorldX, int minWorldY, int maxWorldY,
		Consumer<Spawn> consumer)
	{
		spatialIndex.forEachInArea(plane, minWorldX, maxWorldX, minWorldY, maxWorldY, consumer);
	}

	/** One world tile and everything that respawns on it. */
	public static final class Spawn
	{
		@Getter
		private int x;
		@Getter
		private int y;
		@Getter
		private int plane;
		@Getter
		private String location;
		@Getter
		private boolean members;
		@Getter
		private List<Item> items;

		/** The item the marker is drawn as: the dataset sorts most valuable first. */
		public Item getPrimary()
		{
			return items.get(0);
		}

		/**
		 * The name of the first item on this tile that contains {@code lowerQuery}, or null.
		 * {@code lowerQuery} must already be lower-cased.
		 */
		public String matchingItemName(String lowerQuery)
		{
			if (lowerQuery == null || lowerQuery.isEmpty())
			{
				return null;
			}
			for (Item item : items)
			{
				if (item.name != null && item.name.toLowerCase(Locale.ROOT).contains(lowerQuery))
				{
					return item.name;
				}
			}
			return null;
		}

		/** Best single item value on the tile, in gp. What the min-value slider filters on. */
		public int getValue()
		{
			int best = 0;
			for (Item item : items)
			{
				best = Math.max(best, item.getValue());
			}
			return best;
		}
	}

	/** One item on a spawn tile. */
	public static final class Item
	{
		@Getter
		private int id;
		@Getter
		private String name;
		@SerializedName("qty")
		private int quantity;
		@SerializedName("alch")
		@Getter
		private int highAlch;
		@SerializedName("ge")
		@Getter
		private int gePrice;

		public int getQuantity()
		{
			return Math.max(1, quantity);
		}

		/** GE price where there is one, else high alch — untradeables have no GE price. */
		public int getValue()
		{
			return Math.max(gePrice, highAlch);
		}
	}

	private static final class Dataset
	{
		private List<Spawn> spawns;
	}
}
