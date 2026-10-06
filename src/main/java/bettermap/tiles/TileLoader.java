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
package bettermap.tiles;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import javax.imageio.ImageIO;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.util.Filepath;
import lombok.Getter;

/** Decodes the verified installed asset pack and bundled cavern tiles off the client thread. */
@Slf4j
@Singleton
public class TileLoader
{
	// Each decoded 256x256 tile uses roughly 192-256 KiB depending on image type. Allow room
	// for large viewports and overlapping map layers without constantly evicting visible tiles.
	private static final int MEMORY_CACHE_SIZE = 512;
	private static final Set<String> BUNDLED_CAVERN_TILES = Set.of(
		"0/3/0_80_268.png", "0/3/0_80_269.png", "0/3/0_80_270.png",
		"0/3/0_81_268.png", "0/3/0_81_269.png");

	/**
	 * Coarsest zoom levels kept in memory for the whole session, as the backdrop the hierarchical
	 * fallback synthesises missing detail from.
	 *
	 * <p>Stops at -2 deliberately. Zoom -3 through -2 is 155 tiles (~30MB) and covers the entire
	 * world; including -1 and 0 as well would be 1,214 tiles and ~233MB pinned for the session,
	 * which is most of a default heap. Those two levels load through the FIFO cache like everything else,
	 * and until they arrive a coarser parent is upscaled in their place.
	 */
	private static final int BASE_PREWARM_MAX_ZOOM = -2;
	private static final int WORKER_THREADS = Math.min(4, Math.max(2, Runtime.getRuntime().availableProcessors()));

	private final Map<Long, BufferedImage> memory = Collections.synchronizedMap(
		new LinkedHashMap<Long, BufferedImage>(MEMORY_CACHE_SIZE, 0.75f, false)
		{
			@Override
			protected boolean removeEldestEntry(Map.Entry<Long, BufferedImage> eldest)
			{
				return size() > MEMORY_CACHE_SIZE;
			}
		});
	private final Map<Long, BufferedImage> baseTiles = new ConcurrentHashMap<>(256);
	private final Set<Long> inFlight = ConcurrentHashMap.newKeySet();
	private final Set<Long> missing = ConcurrentHashMap.newKeySet();

	private volatile NavigableSet<Integer> availableZooms = Collections.emptyNavigableSet();
	@Getter
	private volatile Filepath tileDir;
	private volatile List<String> installedPaths = Collections.emptyList();
	private volatile Set<String> installedTilePaths = Collections.emptySet();
	@Getter
	private volatile String status = "Map assets are not installed.";
	private volatile ExecutorService executor;

	public synchronized void startUp()
	{
		if (executor == null || executor.isShutdown())
		{
			executor = Executors.newFixedThreadPool(WORKER_THREADS,
				new ThreadFactoryBuilder().setDaemon(true).setNameFormat("better-map-tiles").build());
			preWarm();
		}
	}

	public synchronized void preWarm()
	{
		if (executor == null)
		{
			return;
		}

		final ExecutorService worker = executor;
		final Filepath directory = tileDir;
		final List<String> paths = installedPaths;
		worker.execute(() ->
		{
			final NavigableSet<Integer> zooms = new TreeSet<>();
			for (String path : paths)
			{
				if (path.startsWith("0/"))
				{
					zooms.add(parseCachePath(path)[1]);
				}
			}
			synchronized (this)
			{
				if (executor != worker || tileDir != directory)
				{
					return;
				}
				availableZooms = Collections.unmodifiableNavigableSet(zooms);
			}
			int loadedBase = 0;
			for (final String path : paths)
			{
				if (Thread.currentThread().isInterrupted())
				{
					return;
				}
				final int[] coords = parseCachePath(path);
				if (!path.startsWith("0/") || coords == null || coords[1] > BASE_PREWARM_MAX_ZOOM)
				{
					continue;
				}
				final BufferedImage img = TileStore.read(directory, path);
				if (img != null)
				{
					final long k = key(coords[0], coords[1], coords[2], coords[3]);
					synchronized (this)
					{
						if (executor != worker || tileDir != directory)
						{
							return;
						}
						baseTiles.put(k, img);
						memory.put(k, img);
					}
					loadedBase++;
				}
			}
			log.info("TileLoader pre-warmed {} base tiles ({} to {}) permanently in memory",
				loadedBase, WikiMapTiles.MIN_ZOOM, BASE_PREWARM_MAX_ZOOM);
		});
	}

	/**
	 * Splits {@code <map>/<zoom>/<plane>_<x>_<y>.png} into plane, zoom, x, y. Null when the path is
	 * not shaped like a tile - the manifest is generated, but a hand-edited one should not throw.
	 */
	static int[] parseCachePath(String path)
	{
		try
		{
			final int lastSlash = path.lastIndexOf('/');
			final int prevSlash = path.lastIndexOf('/', lastSlash - 1);
			if (lastSlash < 0 || prevSlash < 0 || !path.endsWith(".png"))
			{
				return null;
			}
			final int zoom = Integer.parseInt(path.substring(prevSlash + 1, lastSlash));
			final String name = path.substring(lastSlash + 1, path.length() - 4);
			final String[] parts = name.split("_");
			if (parts.length != 3)
			{
				return null;
			}
			return new int[]{
				Integer.parseInt(parts[0]),
				zoom,
				Integer.parseInt(parts[1]),
				Integer.parseInt(parts[2]),
			};
		}
		catch (RuntimeException e)
		{
			return null;
		}
	}

	public synchronized void shutDown()
	{
		if (executor != null)
		{
			executor.shutdownNow();
			executor = null;
		}
		inFlight.clear();
		missing.clear();
		availableZooms = Collections.emptyNavigableSet();
		installedPaths = Collections.emptyList();
		installedTilePaths = Collections.emptySet();
		tileDir = null;
		baseTiles.clear();
		memory.clear();
	}

	/** Which zoom levels this layer has on disk, as a readable list for the debug panel. */
	public String describeLevels()
	{
		final StringBuilder text = new StringBuilder();
		for (int zoom : availableZooms)
		{
			if (text.length() > 0)
			{
				text.append(',');
			}
			text.append(zoom);
		}
		return text.length() == 0 ? "none" : text.toString();
	}

	/** Activated only after the downloader has validated every indexed file. */
	public synchronized void install(Filepath directory, List<String> paths)
	{
		shutDown();
		tileDir = directory;
		installedPaths = List.copyOf(paths);
		installedTilePaths = Set.copyOf(installedPaths);
		status = "Loading map assets...";
		startUp();
	}

	public void setStatus(String message)
	{
		status = message;
	}


	/** True only after a verified installed pack has been activated. */
	public boolean hasTiles()
	{
		return !availableZooms.isEmpty();
	}

	/**
	 * The most detailed zoom level present in the installed pack.
	 */
	public int maxAvailableZoom()
	{
		final NavigableSet<Integer> zooms = availableZooms;
		return zooms.isEmpty() ? WikiMapTiles.MIN_ZOOM : zooms.last();
	}

	public synchronized BufferedImage get(int plane, int zoom, int tileX, int tileY)
	{
		if (!hasTiles())
		{
			return null;
		}
		final long key = key(plane, zoom, tileX, tileY);

		final BufferedImage cached = memory.get(key);
		if (cached != null)
		{
			return cached;
		}

		final BufferedImage base = baseTiles.get(key);
		if (base != null)
		{
			return base;
		}

		final ExecutorService worker = executor;
		final Filepath directory = tileDir;
		final Set<String> paths = installedTilePaths;
		if (!missing.contains(key) && worker != null && inFlight.add(key))
		{
			try
			{
				worker.execute(() -> read(worker, directory, paths, key, plane, zoom, tileX, tileY));
			}
			catch (RejectedExecutionException e)
			{
				// Shutting down mid-frame. Drop the claim so the tile is retried if we come back.
				inFlight.remove(key);
			}
		}

		// Not loaded yet: find an ancestor zoom level and return a nearest-scaled sub-quadrant of
		// it. Nearest, not bilinear: map art is pixel art, and this synthetic tile gets cached
		// under the real tile's key.
		for (int z = zoom - 1; z >= WikiMapTiles.MIN_ZOOM; z--)
		{
			final int shift = zoom - z;
			final int parentX = tileX >> shift;
			final int parentY = tileY >> shift;
			final long parentKey = key(plane, z, parentX, parentY);
			BufferedImage parentImg = memory.get(parentKey);
			if (parentImg == null)
			{
				parentImg = baseTiles.get(parentKey);
			}

			final BufferedImage upscaled = cropAncestor(parentImg, shift, tileX, tileY);
			if (upscaled != null)
			{
				// Cache fallback immediately so render loop does not repeat allocations & scaling.
				final BufferedImage existing = memory.putIfAbsent(key, upscaled);
				if (existing != null)
				{
					return existing;
				}
				return upscaled;
			}
		}

		return null;
	}

	public static long key(int plane, int zoom, int tileX, int tileY)
	{
		return (((long) plane & 0x7L) << 38)
			| (((long) (zoom - WikiMapTiles.MIN_ZOOM) & 0xFL) << 34)
			| (((long) tileX & 0x1FFFFL) << 17)
			| ((long) tileY & 0x1FFFFL);
	}

	private void read(ExecutorService worker, Filepath directory, Set<String> paths,
		long key, int plane, int zoom, int tileX, int tileY)
	{
		try
		{
			final String path = WikiMapTiles.cachePath(plane, zoom, tileX, tileY);
			final BufferedImage image = paths.contains(path) ? TileStore.read(directory, path) : readBundledCavernTile(path);
			if (image != null)
			{
				publishTile(worker, key, image);
				return;
			}

			// Packs may omit an intermediate zoom. Try each ancestor instead of
			// treating an absent immediate parent as absent terrain.
			if (zoom >= 1)
			{
				for (int parentZoom = zoom - 1; parentZoom >= 0; parentZoom--)
				{
					final int parentX = tileX >> (zoom - parentZoom);
					final int parentY = tileY >> (zoom - parentZoom);
					final long parentKey = key(plane, parentZoom, parentX, parentY);
					BufferedImage parentImg = memory.get(parentKey);
					if (parentImg == null)
					{
						parentImg = baseTiles.get(parentKey);
					}
					if (parentImg == null)
					{
						final String parentPath = WikiMapTiles.cachePath(plane, parentZoom, parentX, parentY);
						parentImg = paths.contains(parentPath) ? TileStore.read(directory, parentPath) : null;
					}
					final BufferedImage upscaled = cropAncestor(parentImg, zoom - parentZoom, tileX, tileY);
					if (upscaled == null)
					{
						continue;
					}

					publishTile(worker, key, upscaled);
					return;
				}
			}

			publishTile(worker, key, null);
		}
		catch (RuntimeException e)
		{
			// TileStore already swallows IO errors; this is the decode itself handing back a
			// broken raster. One bad tile must not take the worker thread down with it.
			log.debug("Unreadable tile {}", WikiMapTiles.cachePath(plane, zoom, tileX, tileY), e);
			publishTile(worker, key, null);
		}
		finally
		{
			synchronized (this)
			{
				if (executor == worker)
				{
					inFlight.remove(key);
				}
			}
		}
	}

	/** Crop the child's quadrant, reversing game Y into image Y, and enlarge without filtering. */
	private static BufferedImage cropAncestor(BufferedImage parent, int shift, int tileX, int tileY)
	{
		final int tileSize = WikiMapTiles.TILE_SIZE;
		if (parent == null || parent.getWidth() < tileSize || parent.getHeight() < tileSize || shift >= 8)
		{
			return null;
		}
		final int factor = 1 << shift;
		final int size = tileSize / factor;
		final int x = (tileX & (factor - 1)) * size;
		final int y = (factor - 1 - (tileY & (factor - 1))) * size;
		final BufferedImage sub = parent.getSubimage(x, y, size, size);
		final BufferedImage upscaled = new BufferedImage(tileSize, tileSize, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D graphics = upscaled.createGraphics();
		try
		{
			graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
			graphics.drawImage(sub, 0, 0, tileSize, tileSize, null);
		}
		finally
		{
			graphics.dispose();
		}
		return upscaled;
	}

	static BufferedImage readBundledCavernTile(String path)
	{
		if (!BUNDLED_CAVERN_TILES.contains(path))
		{
			return null;
		}
		final String fileName = path.substring(path.lastIndexOf('/') + 1);
		try (InputStream in = TileLoader.class.getResourceAsStream("/com/bettermap/tiles/wyrmscraig/" + fileName))
		{
			return in == null ? null : ImageIO.read(in);
		}
		catch (IOException e)
		{
			log.debug("Unreadable bundled cavern tile {}", path, e);
			return null;
		}
	}

	private synchronized void publishTile(ExecutorService worker, long key, BufferedImage image)
	{
		if (executor == worker)
		{
			if (image == null)
			{
				missing.add(key);
			}
			else
			{
				memory.put(key, image);
			}
		}
	}
}
