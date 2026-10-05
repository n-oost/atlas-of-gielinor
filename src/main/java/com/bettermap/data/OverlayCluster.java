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
package com.bettermap.data;

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
 * A regional underground overlay: a distinct toggle that composites nearby dungeon geometry.
 * Individual entrances remain available through {@link UndergroundZone} chips.
 */
public final class OverlayCluster
{
	private static final String RESOURCE = "/com/bettermap/dungeons/overlays.tsv";
	private static List<OverlayCluster> ALL;

	public final String id;
	public final String name;
	public final int minX;
	public final int minY;
	public final int maxX;
	public final int maxY;
	/** Overworld tile for the green dungeon icon — a point, not a wash. */
	public final int iconX;
	public final int iconY;
	public final List<UndergroundZone> members;

	public OverlayCluster(
		String id,
		String name,
		int minX,
		int minY,
		int maxX,
		int maxY,
		List<UndergroundZone> members)
	{
		this(id, name, minX, minY, maxX, maxY, members, (minX + maxX) / 2, (minY + maxY) / 2);
	}

	public OverlayCluster(
		String id,
		String name,
		int minX,
		int minY,
		int maxX,
		int maxY,
		List<UndergroundZone> members,
		int iconX,
		int iconY)
	{
		this.id = id;
		this.name = name;
		this.minX = minX;
		this.minY = minY;
		this.maxX = maxX;
		this.maxY = maxY;
		this.iconX = iconX;
		this.iconY = iconY;
		this.members = Collections.unmodifiableList(new ArrayList<>(members));
	}

	public boolean contains(int worldX, int worldY)
	{
		return worldX >= minX && worldX <= maxX && worldY >= minY && worldY <= maxY;
	}

	public int tileArea()
	{
		return (maxX - minX + 1) * (maxY - minY + 1);
	}

	public static synchronized List<OverlayCluster> all()
	{
		if (ALL == null)
		{
			ALL = loadResource();
		}
		return ALL;
	}

	public static OverlayCluster nearestCluster(double x, double y)
	{
		OverlayCluster best = null;
		double bestDist = Double.MAX_VALUE;
		for (OverlayCluster cluster : all())
		{
			if (cluster.members.size() < 2)
			{
				continue;
			}
			double dx = cluster.iconX - x;
			double dy = cluster.iconY - y;
			double d = dx * dx + dy * dy;
			if (d < bestDist)
			{
				bestDist = d;
				best = cluster;
			}
		}
		return best;
	}

	public static OverlayCluster forZone(UndergroundZone zone)
	{
		if (zone == null)
		{
			return null;
		}
		for (OverlayCluster cluster : all())
		{
			if (cluster.members.contains(zone))
			{
				return cluster;
			}
		}
		return null;
	}

	public static List<OverlayCluster> parse(String text) throws IOException
	{
		return parse(new StringReader(text));
	}

	public static List<OverlayCluster> parse(Reader source) throws IOException
	{
		final List<OverlayCluster> out = new ArrayList<>();
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
				if (f.length < 7)
				{
					continue;
				}
				final List<UndergroundZone> members = new ArrayList<>();
				for (String raw : f[6].split(","))
				{
					UndergroundZone zone = UndergroundZone.byId(raw.trim());
					if (zone != null)
					{
						if (zone.getId().startsWith("native_"))
						{
							final int sep = zone.getId().indexOf("__");
							final String candidate = sep < 0 ? zone.getId().substring(7) : zone.getId().substring(sep + 2);
							final UndergroundZone authored = UndergroundZone.byId(candidate);
							if (authored != null && !authored.getId().startsWith("native_"))
							{
								zone = authored;
							}
						}
						if (!members.contains(zone))
						{
							members.add(zone);
						}
					}
				}
				if (members.size() > 1)
				{
					members.removeIf(z -> z.getId().startsWith("native_") && !z.getId().contains("__"));
				}
				if (members.isEmpty())
				{
					continue;
				}
				try
				{
					final int minX = Integer.parseInt(f[2].trim());
					final int minY = Integer.parseInt(f[3].trim());
					final int maxX = Integer.parseInt(f[4].trim());
					final int maxY = Integer.parseInt(f[5].trim());
					int iconX;
					int iconY;
					if (f.length >= 9 && !f[7].trim().isEmpty() && !f[8].trim().isEmpty())
					{
						iconX = Integer.parseInt(f[7].trim());
						iconY = Integer.parseInt(f[8].trim());
					}
					else
					{
						int sx = 0;
						int sy = 0;
						for (UndergroundZone zone : members)
						{
							sx += zone.getSurfacePoint().getX();
							sy += zone.getSurfacePoint().getY();
						}
						iconX = sx / members.size();
						iconY = sy / members.size();
					}
					out.add(new OverlayCluster(
						f[0].trim(),
						f[1],
						minX,
						minY,
						maxX,
						maxY,
						members,
						iconX,
						iconY));
				}
				catch (NumberFormatException ignored)
				{
					// skip a malformed row
				}
			}
		}
		return out;
	}

	private static List<OverlayCluster> loadResource()
	{
		final List<OverlayCluster> clusters = new ArrayList<>(loadResource(RESOURCE));
		clusters.addAll(loadResource("/com/bettermap/dungeons/native-overlays.tsv"));
		return Collections.unmodifiableList(clusters);
	}

	private static List<OverlayCluster> loadResource(String resource)
	{
		final InputStream in = OverlayCluster.class.getResourceAsStream(resource);
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
