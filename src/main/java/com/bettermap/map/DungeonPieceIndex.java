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

import com.bettermap.data.DungeonPiece;
import com.bettermap.data.UndergroundZone;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

/**
 * Arranged dungeon pieces exported by the Swing tuner. Used when a dungeon layer is hovered or
 * opened so each cave island can keep its own placement, rotation and flip.
 */
@Slf4j
@Singleton
public class DungeonPieceIndex
{
	private static final String RESOURCE = "/com/bettermap/dungeons/pieces.tsv";

	private final Map<String, List<DungeonPiece>> byZone = new HashMap<>();
	private boolean loaded;

	public static File file(File tileDir)
	{
		return new File(tileDir, "dungeons/pieces.tsv");
	}

	public void load(File tileDir)
	{
		final DungeonPieceIndex prepared = new DungeonPieceIndex();
		prepared.loadData(tileDir);
		if (Thread.currentThread().isInterrupted())
		{
			return;
		}
		synchronized (this)
		{
			byZone.clear();
			byZone.putAll(prepared.byZone);
			loaded = true;
		}
	}

	private void loadData(File tileDir)
	{
		byZone.clear();
		loaded = false;

		final InputStream in = open(tileDir);
		if (in == null)
		{
			log.debug("No dungeon-piece TSV on disk or in the jar");
			loaded = true;
			return;
		}

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)))
		{
			index(parse(reader).pieces);
		}
		catch (IOException e)
		{
			log.debug("Could not read dungeon pieces", e);
		}
		loaded = true;
		int n = 0;
		for (List<DungeonPiece> list : byZone.values())
		{
			n += list.size();
		}
		log.debug("Loaded {} dungeon pieces across {} zones", n, byZone.size());
	}

	public synchronized List<DungeonPiece> piecesFor(String zoneId)
	{
		if (!loaded || zoneId == null || zoneId.isEmpty())
		{
			return Collections.emptyList();
		}
		final String canonical = UndergroundZone.canonicalZoneId(zoneId);
		final List<DungeonPiece> list = byZone.get(canonical);
		if (list != null && !list.isEmpty())
		{
			return list;
		}
		final List<DungeonPiece> direct = byZone.get(zoneId);
		return direct == null ? Collections.emptyList() : direct;
	}

	public synchronized boolean hasPieces(String zoneId)
	{
		return !piecesFor(zoneId).isEmpty();
	}

	/** The arranged piece containing a native player coordinate on the displayed floor. */
	public synchronized DungeonPiece pieceAt(String zoneId, int worldX, int worldY, int plane, Integer layer)
	{
		for (DungeonPiece piece : piecesFor(zoneId))
		{
			if (piece.plane == plane
				&& (layer == null || piece.layer == layer)
				&& piece.containsNative(worldX, worldY))
			{
				return piece;
			}
		}
		return null;
	}

	private void index(List<DungeonPiece> pieces)
	{
		for (DungeonPiece piece : pieces)
		{
			if (!piece.hasZone())
			{
				continue;
			}
			final String canonical = UndergroundZone.canonicalZoneId(piece.zoneId);
			byZone.computeIfAbsent(canonical, k -> new ArrayList<>()).add(piece);
			if (!canonical.equals(piece.zoneId))
			{
				byZone.computeIfAbsent(piece.zoneId, k -> new ArrayList<>()).add(piece);
			}
		}
		for (List<DungeonPiece> list : byZone.values())
		{
			list.sort(Comparator.comparingInt(p -> p.layer));
		}
	}

	private static InputStream open(File tileDir)
	{
		if (tileDir != null)
		{
			final File file = file(tileDir);
			if (file.isFile() && file.length() > 0)
			{
				try
				{
					return Files.newInputStream(file.toPath());
				}
				catch (IOException e)
				{
					log.debug("Unreadable dungeon-piece file {}", file, e);
				}
			}
		}
		return DungeonPieceIndex.class.getResourceAsStream(RESOURCE);
	}

	public static ParseResult parse(Reader source) throws IOException
	{
		final List<LayerRow> layers = new ArrayList<>();
		final Map<Integer, MutablePiece> byIndex = new LinkedHashMap<>();
		try (BufferedReader reader = source instanceof BufferedReader
			? (BufferedReader) source
			: new BufferedReader(source))
		{
			String line;
			while ((line = reader.readLine()) != null)
			{
				if (line.startsWith("#L\t"))
				{
					final LayerRow layer = parseLayer(line);
					if (layer != null)
					{
						layers.add(layer);
					}
					continue;
				}
				if (line.isEmpty() || line.charAt(0) == '#')
				{
					continue;
				}
				final String[] f = line.split("\t", -1);
				if (f.length < 11)
				{
					continue;
				}
				try
				{
					final int idx = Integer.parseInt(f[0].trim());
					final MutablePiece p = byIndex.computeIfAbsent(idx, MutablePiece::new);
					p.layer = Integer.parseInt(f[1].trim());
					p.plane = Integer.parseInt(f[2].trim());
					p.label = f[3].equals("-") ? "" : f[3];
					p.rects.add(new int[] {
						Integer.parseInt(f[4].trim()), Integer.parseInt(f[5].trim()),
						Integer.parseInt(f[6].trim()), Integer.parseInt(f[7].trim())
					});
					p.dx = Integer.parseInt(f[8].trim());
					p.dy = Integer.parseInt(f[9].trim());
					p.rot = ((Integer.parseInt(f[10].trim()) % 360) + 360) % 360;
					if (f.length >= 12)
					{
						p.locked = "1".equals(f[11].trim());
					}
					if (f.length >= 13)
					{
						final int flip = Integer.parseInt(f[12].trim());
						p.flipX = (flip & 1) != 0;
						p.flipY = (flip & 2) != 0;
					}
					if (f.length >= 14)
					{
						final String zone = f[13].trim();
						if (!zone.isEmpty() && !"-".equals(zone))
						{
							p.zoneId = zone;
						}
					}
				}
				catch (NumberFormatException ignored)
				{
					// skip a malformed row
				}
			}
		}

		final List<DungeonPiece> pieces = new ArrayList<>();
		for (MutablePiece p : byIndex.values())
		{
			if (p.rects.isEmpty())
			{
				continue;
			}
			pieces.add(new DungeonPiece(
				p.id, p.layer, p.plane, p.label, p.rects.toArray(new int[0][]),
				p.dx, p.dy, p.rot, p.flipX, p.flipY, p.locked, p.zoneId));
		}
		return new ParseResult(layers, pieces);
	}

	public static String toTsv(ParseResult parsed)
	{
		final StringBuilder sb = new StringBuilder();
		sb.append("# Better Map dungeon pieces — region → sub-region → dungeon → deeper floors\n");
		sb.append("# overworld = nativeSrc(plane) + (dstDx, dstDy), then mirrored (`flip`: 1=X 2=Y 3=both) "
			+ "and rotated `rot` deg CW about the piece's source-bounds centre. Higher layer paints on top.\n");
		sb.append("#L format: id  visible  name  parentId\n");
		for (LayerRow layer : parsed.layers)
		{
			sb.append("#L\t").append(layer.id).append('\t').append(layer.visible ? 1 : 0).append('\t')
				.append(layer.name.replace('\t', ' ').replace('\n', ' ')).append('\t')
				.append(layer.parentId).append('\n');
		}
		sb.append("# piece\tlayer\tplane\tlabel\tsrcMinX\tsrcMinY\tsrcMaxX\tsrcMaxY\tdstDx\tdstDy\trot\tlocked\tflip\tzone\n");
		for (DungeonPiece p : parsed.pieces)
		{
			final String zone = p.hasZone() ? p.zoneId : "-";
			for (int[] r : p.rects)
			{
				sb.append(p.id).append('\t')
					.append(p.layer).append('\t')
					.append(p.plane).append('\t')
					.append(p.label.isEmpty() ? "-" : p.label.replace('\t', ' ').replace('\n', ' ')).append('\t')
					.append(r[0]).append('\t').append(r[1]).append('\t')
					.append(r[2]).append('\t').append(r[3]).append('\t')
					.append(p.dx).append('\t').append(p.dy).append('\t')
					.append(p.rot).append('\t')
					.append(p.locked ? 1 : 0).append('\t')
					.append((p.flipX ? 1 : 0) | (p.flipY ? 2 : 0)).append('\t')
					.append(zone).append('\n');
			}
		}
		return sb.toString();
	}

	private static LayerRow parseLayer(String line)
	{
		final String[] lf = line.split("\t", -1);
		if (lf.length < 4)
		{
			return null;
		}
		try
		{
			final int id = Integer.parseInt(lf[1].trim());
			final boolean visible = !"0".equals(lf[2].trim());
			final String name = lf[3];
			final int parentId = lf.length >= 5 && !lf[4].trim().isEmpty()
				? Integer.parseInt(lf[4].trim()) : -1;
			return new LayerRow(id, visible, name, parentId);
		}
		catch (NumberFormatException e)
		{
			return null;
		}
	}

	public static final class LayerRow
	{
		public final int id;
		public final boolean visible;
		public final String name;
		public final int parentId;

		public LayerRow(int id, boolean visible, String name, int parentId)
		{
			this.id = id;
			this.visible = visible;
			this.name = name;
			this.parentId = parentId;
		}
	}

	public static final class ParseResult
	{
		public final List<LayerRow> layers;
		public final List<DungeonPiece> pieces;

		public ParseResult(List<LayerRow> layers, List<DungeonPiece> pieces)
		{
			this.layers = layers;
			this.pieces = pieces;
		}
	}

	private static final class MutablePiece
	{
		final int id;
		int layer;
		int plane;
		String label = "";
		final List<int[]> rects = new ArrayList<>();
		int dx;
		int dy;
		int rot;
		boolean flipX;
		boolean flipY;
		boolean locked;
		String zoneId = "";

		MutablePiece(int id)
		{
			this.id = id;
		}
	}
}
