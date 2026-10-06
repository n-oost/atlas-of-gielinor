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
package bettermap.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A floor / sub-layer hover point sitting on a dungeon's composed footprint, not on the
 * entrance chip. Runtime data lives in {@code floors.tsv}.
 */
public final class OverlayFloor
{
	private static final String RESOURCE = "/com/bettermap/dungeons/floors.tsv";
	private static volatile List<OverlayFloor> ALL = Collections.emptyList();
	private static volatile boolean loaded;

	public final String name;
	public final UndergroundZone zone;
	public final int plane;
	public final int worldX;
	public final int worldY;
	/** Optional authored layer, for distinct floors sharing a game plane. */
	public final Integer layerId;

	public OverlayFloor(String name, UndergroundZone zone, int plane, int worldX, int worldY)
	{
		this(name, zone, plane, worldX, worldY, null);
	}

	public OverlayFloor(String name, UndergroundZone zone, int plane, int worldX, int worldY, Integer layerId)
	{
		this.name = name;
		this.zone = zone;
		this.plane = plane;
		this.worldX = worldX;
		this.worldY = worldY;
		this.layerId = layerId;
	}

	/** Initialize on the map data worker; frame-time reads never open resources. */
	public static void load()
	{
		if (loaded) return;
		final List<OverlayFloor> prepared = loadResource();
		if (Thread.currentThread().isInterrupted()) return;
		ALL = prepared;
		loaded = true;
	}

	public static List<OverlayFloor> all()
	{
		return ALL;
	}

	/** First authored sub-layer shown before the user chooses a floor chip. */
	public static Integer defaultLayerFor(UndergroundZone zone)
	{
		for (OverlayFloor floor : all())
		{
			if (floor.zone == zone && floor.layerId != null)
			{
				return floor.layerId;
			}
		}
		return null;
	}

	public static List<OverlayFloor> parse(String text) throws IOException
	{
		return parse(new StringReader(text));
	}

	public static List<OverlayFloor> parse(Reader source) throws IOException
	{
		final List<OverlayFloor> out = new ArrayList<>();
		try (BufferedReader reader = source instanceof BufferedReader
			? (BufferedReader) source
			: new BufferedReader(source))
		{
			String line;
			while ((line = reader.readLine()) != null)
			{
				if (line.isEmpty() || line.charAt(0) == '#')
				{
					continue;
				}
				final String[] f = line.split("\t", -1);
				if (f.length < 5)
				{
					continue;
				}
				final UndergroundZone zone = UndergroundZone.byId(f[0].trim());
				if (zone == null)
				{
					continue;
				}
				try
				{
					out.add(new OverlayFloor(
						f[1],
						zone,
						Integer.parseInt(f[2].trim()),
						Integer.parseInt(f[3].trim()),
						Integer.parseInt(f[4].trim()),
						f.length > 5 && !f[5].trim().isEmpty() ? Integer.valueOf(f[5].trim()) : null));
				}
				catch (NumberFormatException ignored)
				{
					// skip a malformed row
				}
			}
		}
		return out;
	}

	private static List<OverlayFloor> loadResource()
	{
		final InputStream in = OverlayFloor.class.getResourceAsStream(RESOURCE);
		if (in == null)
		{
			return Collections.emptyList();
		}
		try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
		{
			return parse(reader);
		}
		catch (IOException e)
		{
			return Collections.emptyList();
		}
	}
}
