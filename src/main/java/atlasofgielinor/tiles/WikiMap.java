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
package atlasofgielinor.tiles;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import atlasofgielinor.data.io.BundledTsv;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * The map layers published by the OSRS Wiki map service, taken from its own
 * {@code data/dataloader.json} catalogue. Bounds are plain game coordinates, so no
 * georeferencing is needed - the tile grid and the game share one coordinate system.
 *
 * <p>{@link #SURFACE} is the overworld; every other entry is a dungeon or instanced area that
 * the game shows on its own separate world map.
 */
@Slf4j
public final class WikiMap
{
	private static final Map<String, WikiMap> entries = new LinkedHashMap<>();
	public static final WikiMap SURFACE = of("SURFACE", new Metadata(0, "RuneScape Surface", 896, 1984, 4096, 4200));
	public static final WikiMap BRAINDEATH_ISLAND = of("BRAINDEATH_ISLAND", null);
	public static final WikiMap WATERBIRTH_DUNGEON = of("WATERBIRTH_DUNGEON", null);
	public static volatile WikiMap[] VALUES = {SURFACE};

	@AllArgsConstructor
	private static final class Metadata
	{
		private final int mapId;
		private final String displayName;
		private final int minX;
		private final int minY;
		private final int maxX;
		private final int maxY;
	}

	private final String id;
	private final Metadata bootstrap;
	private static volatile Map<WikiMap, Metadata> catalog = Collections.emptyMap();

	private WikiMap(String id, Metadata bootstrap)
	{
		this.id = id;
		this.bootstrap = bootstrap;
	}

	private static synchronized WikiMap of(String id, Metadata bootstrap)
	{
		return entries.computeIfAbsent(id, k -> new WikiMap(k, bootstrap));
	}

	public String name()
	{
		return id;
	}

	@Override
	public String toString()
	{
		return id;
	}

	public static WikiMap[] values()
	{
		return VALUES;
	}

	public static WikiMap valueOf(String id)
	{
		final WikiMap entry = entries.get(id);
		if (entry == null) throw new IllegalArgumentException("Unknown wiki map: " + id);
		return entry;
	}

	public static boolean isLoaded()
	{
		return !catalog.isEmpty();
	}

	public static synchronized void load()
	{
		if (isLoaded()) return;
		final Map<WikiMap, Metadata> prepared = new LinkedHashMap<>();
		try
		{
			for (String[] f : BundledTsv.read("/atlasofgielinor/dungeons/wiki-maps.tsv", 7))
			{
				final Metadata metadata = new Metadata(Integer.parseInt(f[1]), f[2], Integer.parseInt(f[3]),
					Integer.parseInt(f[4]), Integer.parseInt(f[5]), Integer.parseInt(f[6]));
				if (prepared.put(of(f[0], null), metadata) != null) throw new IOException("Duplicate map bounds");
			}
			if (prepared.isEmpty()) throw new IOException("Incomplete map bounds");
			if (Thread.currentThread().isInterrupted()) return;
			catalog = Collections.unmodifiableMap(prepared);
			VALUES = prepared.keySet().toArray(new WikiMap[0]);
		}
		catch (IOException | RuntimeException e)
		{
			log.debug("Could not load map bounds", e);
		}
	}

	private Metadata metadata()
	{
		final Metadata result = catalog.getOrDefault(this, bootstrap);
		if (result == null) throw new IllegalStateException("Map bounds have not been loaded");
		return result;
	}

	public int getMapId()
	{
		return metadata().mapId;
	}

	public String getDisplayName()
	{
		return metadata().displayName;
	}

	public int getMinX()
	{
		return metadata().minX;
	}

	public int getMinY()
	{
		return metadata().minY;
	}

	public int getMaxX()
	{
		return metadata().maxX;
	}

	public int getMaxY()
	{
		return metadata().maxY;
	}

	public boolean contains(int worldX, int worldY)
	{
		final Metadata bounds = metadata();
		return worldX >= bounds.minX && worldX <= bounds.maxX && worldY >= bounds.minY && worldY <= bounds.maxY;
	}

	public long area()
	{
		final Metadata bounds = metadata();
		return (long) (bounds.maxX - bounds.minX) * (bounds.maxY - bounds.minY);
	}
}
