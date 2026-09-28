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
package com.bettermap.tiles;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.util.Filepath;

/** Decodes only the verified installed asset pack, off the client thread. */
@Slf4j
@Singleton
public class TileLoader
{
	// A fullscreen view needs roughly 40 detail tiles plus a handful of coarse ones, and every
	// tile decodes to a 256x256 RGB raster - 192KB, whatever it cost on disk. That number is the
	// whole budget here: RuneLite ships with a 512MB heap, so the cache is sized in megabytes
	// rather than in tiles. 256 entries is ~49MB, which leaves a comfortable margin over the
	// working set and still holds several screens' worth of panning.
	private static final int MEMORY_CACHE_SIZE = 256;
	private static final int SCALED_CACHE_SIZE = 256;

	/**
	 * Coarsest zoom levels kept in memory for the whole session, as the backdrop the hierarchical
	 * fallback synthesises missing detail from.
	 *
	 * <p>Stops at -2 deliberately. Zoom -3 through -2 is 155 tiles (~30MB) and covers the entire
	 * world; including -1 and 0 as well would be 1,214 tiles and ~233MB pinned for the session,
	 * which is most of a default heap. Those two levels load through the LRU like everything else,
	 * and until they arrive a coarser parent is upscaled in their place.
	 */
	private static final int BASE_PREWARM_MAX_ZOOM = -2;
	private static final int WORKER_THREADS = Math.min(4, Math.max(2, Runtime.getRuntime().availableProcessors()));

	private final ConcurrentHashMap<Long, BufferedImage> memory = new ConcurrentHashMap<>(MEMORY_CACHE_SIZE);
	private final java.util.concurrent.ConcurrentLinkedDeque<Long> memoryKeys = new java.util.concurrent.ConcurrentLinkedDeque<>();
	private final Map<Long, BufferedImage> baseTiles = new ConcurrentHashMap<>(256);
	private final Map<String, ScaledTile> scaled = lru(SCALED_CACHE_SIZE);

	private static final class ScaledTile
	{
		private final WeakReference<BufferedImage> source;
		private final BufferedImage image;

		private ScaledTile(BufferedImage source, BufferedImage image)
		{
			this.source = new WeakReference<>(source);
			this.image = image;
		}
	}
	private final Set<Long> inFlight = ConcurrentHashMap.newKeySet();
	private final Set<Long> missing = ConcurrentHashMap.newKeySet();

	// Diagnostics only, so the debug panel can show whether tiles are actually flowing.
	private final java.util.concurrent.atomic.AtomicLong hits = new java.util.concurrent.atomic.AtomicLong();
	private final java.util.concurrent.atomic.AtomicLong reads = new java.util.concurrent.atomic.AtomicLong();
	private final java.util.concurrent.atomic.AtomicLong failures = new java.util.concurrent.atomic.AtomicLong();

	private volatile NavigableSet<Integer> availableZooms = Collections.emptyNavigableSet();
	private volatile Filepath tileDir;
	private volatile List<String> installedPaths = Collections.emptyList();
	private volatile String status = "Map assets are not installed.";
	private volatile ExecutorService executor;

	public synchronized void startUp()
	{
		if (executor == null || executor.isShutdown())
		{
			final ThreadFactory factory = r ->
			{
				Thread t = new Thread(r, "better-map-tiles");
				t.setDaemon(true);
				return t;
			};
			executor = Executors.newFixedThreadPool(WORKER_THREADS, factory);
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
						putMemory(k, img);
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

	private void putMemory(long key, BufferedImage image)
	{
		if (image == null)
		{
			return;
		}
		if (memory.put(key, image) == null)
		{
			trackMemoryKey(key);
		}
	}

	private void trackMemoryKey(long key)
	{
		memoryKeys.addLast(key);
		while (memory.size() > MEMORY_CACHE_SIZE)
		{
			final Long oldest = memoryKeys.pollFirst();
			if (oldest == null)
			{
				break;
			}
			memory.remove(oldest);
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
		tileDir = null;
		baseTiles.clear();
		memory.clear();
		memoryKeys.clear();
		clear(scaled);
	}

	public Filepath getTileDir()
	{
		return tileDir;
	}

	/** Cache hits since start-up, for the debug panel. */
	public long getCacheHits()
	{
		return hits.get();
	}

	/** Tiles decoded from disk since start-up. */
	public long getDiskReads()
	{
		return reads.get();
	}

	/** Tiles asked for that are not on disk. Expected to be non-zero: ocean is not published. */
	public long getMissCount()
	{
		return failures.get();
	}

	public int getMemoryCacheSize()
	{
		return memory.size();
	}

	public int getPendingCount()
	{
		return inFlight.size();
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
		installedPaths = Collections.unmodifiableList(new ArrayList<>(paths));
		status = "Loading map assets...";
		startUp();
	}

	public void setStatus(String message)
	{
		status = message;
	}

	public String getStatus()
	{
		return status;
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
			hits.incrementAndGet();
			return cached;
		}

		final BufferedImage base = baseTiles.get(key);
		if (base != null)
		{
			hits.incrementAndGet();
			return base;
		}

		final ExecutorService worker = executor;
		final Filepath directory = tileDir;
		if (!missing.contains(key) && worker != null && inFlight.add(key))
		{
			try
			{
				worker.execute(() -> read(worker, directory, key, plane, zoom, tileX, tileY));
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

			if (parentImg != null && parentImg.getWidth() >= 256 && parentImg.getHeight() >= 256)
			{
				final int factor = 1 << shift;
				final int subSize = 256 / factor;
				if (subSize > 0)
				{
					final int subX = (tileX & (factor - 1)) * subSize;
					final int subY = ((factor - 1) - (tileY & (factor - 1))) * subSize;

					if (subX >= 0 && subY >= 0 && subX + subSize <= parentImg.getWidth() && subY + subSize <= parentImg.getHeight())
					{
						final BufferedImage sub = parentImg.getSubimage(subX, subY, subSize, subSize);
						final BufferedImage upscaled = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
						final Graphics2D g = upscaled.createGraphics();
						g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
						g.drawImage(sub, 0, 0, 256, 256, null);
						g.dispose();

						// Cache fallback immediately so render loop does not repeat allocations & scaling
						final BufferedImage existing = memory.putIfAbsent(key, upscaled);
						if (existing != null)
						{
							return existing;
						}
						trackMemoryKey(key);
						return upscaled;
					}
				}
			}
		}

		return null;
	}

	/**
	 * Returns {@code source} rendered at exactly {@code width} x {@code height}, reusing the
	 * previous result. The map redraws every frame at a fixed scale, so without this the client
	 * would re-filter every tile 50 times a second.
	 */
	public BufferedImage scaled(long tileKey, BufferedImage source, int width, int height)
	{
		if (width <= 0 || height <= 0)
		{
			return null;
		}

		if (width == source.getWidth() && height == source.getHeight())
		{
			return source;
		}

		final String key = tileKey + "@" + width + "x" + height;

		synchronized (scaled)
		{
			ScaledTile cached = scaled.get(key);
			if (cached != null && cached.source.get() == source)
			{
				return cached.image;
			}
		}

		final BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = resized.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.drawImage(source, 0, 0, width, height, null);
		g.dispose();

		synchronized (scaled)
		{
			scaled.put(key, new ScaledTile(source, resized));
		}
		return resized;
	}

	/** Drops the scaled cache; call when the zoom changes so stale sizes do not linger. */
	public void clearScaled()
	{
		clear(scaled);
	}

	public static long key(int plane, int zoom, int tileX, int tileY)
	{
		return (((long) plane & 0x7L) << 38)
			| (((long) (zoom - WikiMapTiles.MIN_ZOOM) & 0xFL) << 34)
			| (((long) tileX & 0x1FFFFL) << 17)
			| ((long) tileY & 0x1FFFFL);
	}

	private void read(ExecutorService worker, Filepath directory, long key, int plane, int zoom, int tileX, int tileY)
	{
		try
		{
			final BufferedImage image = TileStore.read(directory, WikiMapTiles.cachePath(plane, zoom, tileX, tileY));
			if (image != null)
			{
				reads.incrementAndGet();
				publishTile(worker, key, image);
				return;
			}

			// No tile file on disk: synthesize this one from the parent zoom instead.
			if (zoom >= 1)
			{
				final int parentX = tileX >> 1;
				final int parentY = tileY >> 1;
				final long parentKey = key(plane, zoom - 1, parentX, parentY);
				BufferedImage parentImg = memory.get(parentKey);
				if (parentImg == null)
				{
					parentImg = baseTiles.get(parentKey);
				}
				if (parentImg == null)
				{
					parentImg = TileStore.read(directory, WikiMapTiles.cachePath(plane, zoom - 1, parentX, parentY));
				}

				if (parentImg != null && parentImg.getWidth() >= 256 && parentImg.getHeight() >= 256)
				{
					final int subX = (tileX & 1);
					final int subY = (tileY & 1);
					final int cropX = subX * 128;
					final int cropY = (1 - subY) * 128;
					final BufferedImage sub = parentImg.getSubimage(cropX, cropY, 128, 128);

					final BufferedImage upscaled = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
					final Graphics2D g = upscaled.createGraphics();
					g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
					g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
					g.drawImage(sub, 0, 0, 256, 256, null);
					g.dispose();

					reads.incrementAndGet();
					publishTile(worker, key, upscaled);
					return;
				}
			}

			publishTile(worker, key, null);
			failures.incrementAndGet();
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
				putMemory(key, image);
			}
		}
	}

	private static void clear(Map<?, ?> map)
	{
		synchronized (map)
		{
			map.clear();
		}
	}

	private static <K, V> Map<K, V> lru(int maxSize)
	{
		return Collections.synchronizedMap(new LinkedHashMap<K, V>(maxSize, 0.75f, true)
		{
			@Override
			protected boolean removeEldestEntry(Map.Entry<K, V> eldest)
			{
				return size() > maxSize;
			}
		});
	}
}
