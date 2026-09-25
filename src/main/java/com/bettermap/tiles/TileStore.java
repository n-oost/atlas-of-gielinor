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

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;

/**
 * Where map tiles come from: the plugin jar first, then the on-disk pyramid.
 *
 * <p>Plugin Hub plugins are handed to the client as a jar on the classpath and are never unpacked,
 * so the shipped tiles are read with {@code getResourceAsStream} rather than as files - a
 * {@code getResource} URL would be a jar-URL in a real install and a file-URL in the IDE, which is
 * exactly the trap the Plugin Hub docs warn about.
 *
 * <p>Disk stays as a fallback because the dev tooling in {@code tools/} renders the full-detail
 * pyramid straight into {@code ~/.runelite/cache/better-map/tiles}, at zoom levels finer than the
 * bundled set. A tile present on disk wins nothing and loses nothing by being checked second: the
 * bundled set is the one that is always complete.
 */
@Slf4j
public final class TileStore
{
	/** Root of the bundled pyramid inside the jar. Mirrors the on-disk layout exactly. */
	static final String RESOURCE_ROOT = "/com/bettermap/tiles/";

	/**
	 * Listing of every bundled tile, one cache-relative path per line.
	 *
	 * <p>A jar directory cannot be listed through {@code getResourceAsStream}, and walking the
	 * classpath to find out is both slow and fragile, so the packer writes down what it wrote.
	 */
	static final String MANIFEST = RESOURCE_ROOT + "index.txt";

	private TileStore()
	{
	}

	/**
	 * Reads one tile, or null when neither source has it.
	 *
	 * @param tileDir   on-disk pyramid root, checked after the jar
	 * @param cachePath tile path relative to either root, e.g. {@code 0/-2/0_10_10.png}
	 */
	public static BufferedImage read(File tileDir, String cachePath)
	{
		try (InputStream in = TileStore.class.getResourceAsStream(RESOURCE_ROOT + cachePath))
		{
			if (in != null)
			{
				synchronized (ImageIO.class)
				{
					final BufferedImage image = ImageIO.read(in);
					if (image != null)
					{
						return image;
					}
				}
			}
		}
		catch (IOException e)
		{
			log.debug("[BetterMap] bundled tile {} unreadable", cachePath, e);
		}

		if (tileDir != null)
		{
			final File file = new File(tileDir, cachePath);
			if (file.isFile() && file.length() > 0)
			{
				try
				{
					synchronized (ImageIO.class)
					{
						return ImageIO.read(file);
					}
				}
				catch (IOException e)
				{
					log.debug("[BetterMap] tile file {} unreadable", file, e);
				}
			}
		}
		return null;
	}

	/** Cache-relative paths of every bundled tile, or an empty list when none are bundled. */
	public static List<String> bundledPaths()
	{
		try (InputStream in = TileStore.class.getResourceAsStream(MANIFEST))
		{
			if (in == null)
			{
				return Collections.emptyList();
			}
			final List<String> paths = new ArrayList<>();
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)))
			{
				String line;
				while ((line = reader.readLine()) != null)
				{
					final String trimmed = line.trim();
					if (!trimmed.isEmpty())
					{
						paths.add(trimmed);
					}
				}
			}
			return paths;
		}
		catch (IOException e)
		{
			log.warn("[BetterMap] bundled tile manifest unreadable", e);
			return Collections.emptyList();
		}
	}
}
