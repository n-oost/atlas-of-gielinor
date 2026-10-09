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
import javax.inject.Inject;
import com.google.gson.Gson;

import atlasofgielinor.data.dungeons.DungeonPiece;
import atlasofgielinor.data.dungeons.UndergroundZone;
import atlasofgielinor.map.catalog.MapCatalog;
import lombok.extern.slf4j.Slf4j;

/**
 * Arranged dungeon pieces exported by the Swing tuner. Used when a dungeon layer is hovered or
 * opened so each cave island can keep its own placement, rotation and flip.
 */
@Slf4j
@Singleton
public class DungeonPieceIndex
{
	private final Gson gson;

	@Inject
	public DungeonPieceIndex(Gson gson)
	{
		this.gson = gson;
	}

	private static final String RESOURCE = "/atlasofgielinor/dungeons/pieces.tsv";

	private final Map<String, List<DungeonPiece>> byZone = new HashMap<>();
	private boolean loaded;

	public static File file(File tileDir)
	{
		return new File(tileDir, "dungeons/pieces.tsv");
	}

	public void load(File tileDir)
	{
		final DungeonPieceIndex prepared = new DungeonPieceIndex(gson);
		prepared.loadData(tileDir);
		if (!prepared.loaded || Thread.currentThread().isInterrupted())
		{
			return;
		}
		synchronized (this)
		{
			byZone.clear();
			byZone.putAll(prepared.byZone);
			InstanceMaps.setDisplayPieces(new HashMap<>(byZone));
			final List<DungeonPiece> routingPieces = new ArrayList<>();
			for (Map.Entry<String, List<DungeonPiece>> entry : byZone.entrySet())
			{
				if (!entry.getKey().startsWith("native_")
					&& entry.getKey().equals(UndergroundZone.canonicalZoneId(entry.getKey())))
				{
					routingPieces.addAll(entry.getValue());
				}
			}
			InstanceMaps.setRoutingPieces(routingPieces);
			loaded = true;
		}
	}

	private void loadData(File tileDir)
	{
		byZone.clear();
		loaded = false;
		MapCatalogLoader.load(gson);
		if (!MapCatalogLoader.isReady()) return;
		try (InputStream nativeIn = DungeonPieceIndex.class.getResourceAsStream("/atlasofgielinor/dungeons/native-pieces.tsv"))
		{
			if (nativeIn != null)
			{
				indexLegacy(parse(new InputStreamReader(nativeIn, StandardCharsets.UTF_8)).pieces);
			}
		}
		catch (IOException e)
		{
			log.debug("Could not read native map sections", e);
		}

		final InputStream in = open(tileDir);
		if (in == null)
		{
			log.debug("No dungeon-piece TSV on disk or in the jar");
			indexCanonical();
			loaded = true;
			return;
		}

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)))
		{
			indexLegacy(parse(reader).pieces);
		}
		catch (IOException e)
		{
			log.debug("Could not read dungeon pieces", e);
		}
		indexCanonical();
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

	/** Canonical ownership supersedes matching legacy pieces, including stale disk overrides. */
	private void indexLegacy(List<DungeonPiece> pieces)
	{
		final java.util.Set<Integer> managed = new java.util.HashSet<>();
		for (MapCatalog.Layout layout : MapCatalog.current().layouts.values())
			if (layout.legacyPieceId != null) managed.add(layout.legacyPieceId);
		final List<DungeonPiece> remaining = new ArrayList<>();
		for (DungeonPiece piece : pieces)
			if (!managed.contains(piece.id)) remaining.add(piece);
		index(remaining);
	}

	private void indexCanonical()
	{
		final MapCatalog catalog = MapCatalog.current();
		final Map<Integer, DungeonPiece> bundled = new HashMap<>();
		try (InputStream in = DungeonPieceIndex.class.getResourceAsStream(RESOURCE))
		{
			if (in != null)
				for (DungeonPiece piece : parse(new InputStreamReader(in, StandardCharsets.UTF_8)).pieces)
					bundled.put(piece.id, piece);
		}
		catch (IOException e)
		{
			log.debug("Could not read canonical piece rectangles", e);
		}
		final List<DungeonPiece> pieces = new ArrayList<>();
		for (MapCatalog.Layout layout : catalog.layouts.values())
		{
			if (!layout.verified || !"piece".equals(layout.kind)) continue;
			final DungeonPiece authored = bundled.get(layout.legacyPieceId);
			int[][] rects = new int[][]{{layout.minX, layout.minY, layout.maxX, layout.maxY}};
			if (authored != null && layout.placeId.equals(authored.zoneId) && layout.plane == authored.plane)
			{
				final int[] bounds = authored.srcBounds();
				if (bounds[0] == layout.minX && bounds[1] == layout.minY
					&& bounds[2] == layout.maxX && bounds[3] == layout.maxY) rects = authored.rects;
			}
			pieces.add(new DungeonPiece(layout.legacyPieceId, layout.layerId, layout.plane,
				catalog.places.get(layout.placeId).name,
				rects, layout.dx, layout.dy,
				layout.rotation, (layout.flip & 1) != 0, (layout.flip & 2) != 0, true, layout.placeId));
		}
		index(pieces);
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
					log.debug("Legacy dungeon geometry from disk {}; canonical migrated pieces take precedence", file);
					return Files.newInputStream(file.toPath());
				}
				catch (IOException e)
				{
					log.debug("Unreadable dungeon-piece file {}", file, e);
				}
			}
		}
		log.debug("Legacy dungeon geometry from bundled {}", RESOURCE);
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
