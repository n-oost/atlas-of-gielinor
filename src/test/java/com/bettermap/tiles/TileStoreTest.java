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
import java.util.List;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * The bundled tile set is what a Plugin Hub install actually reads, so these run against the real
 * resources rather than a fixture: a manifest that does not match what shipped is a black map.
 */
public class TileStoreTest
{
	@Test
	public void manifestListsBundledTiles()
	{
		final List<String> paths = TileStore.bundledPaths();
		assertFalse("no tiles bundled; run 'gradlew :tools:packTiles'", paths.isEmpty());
	}

	@Test
	public void everyManifestEntryParsesAndLoads()
	{
		final List<String> paths = TileStore.bundledPaths();

		// The first entry of each map/zoom directory is enough to prove the layout; decoding all
		// 4,000 would make this test cost seconds for no extra signal.
		String previousPrefix = null;
		int checked = 0;
		for (final String path : paths)
		{
			final int[] coords = TileLoader.parseCachePath(path);
			assertNotNull("unparseable manifest entry " + path, coords);

			final String prefix = path.substring(0, path.lastIndexOf('/'));
			if (prefix.equals(previousPrefix))
			{
				continue;
			}
			previousPrefix = prefix;

			final BufferedImage tile = TileStore.read(null, path);
			assertNotNull("bundled tile " + path + " is missing or unreadable", tile);
			assertEquals(256, tile.getWidth());
			assertEquals(256, tile.getHeight());
			checked++;
		}
		assertTrue(checked > 0);
	}

	@Test
	public void readReturnsNullForAbsentTile()
	{
		assertNull(TileStore.read(null, "0/-3/9_999_999.png"));
	}

	@Test
	public void parseCachePathReadsPlaneZoomAndTile()
	{
		assertArrayEquals(new int[]{0, -2, 10, 11}, TileLoader.parseCachePath("0/-2/0_10_11.png"));
		assertArrayEquals(new int[]{3, 1, 4, 5}, TileLoader.parseCachePath("28/1/3_4_5.png"));
	}

	@Test
	public void parseCachePathRejectsMalformedInput()
	{
		assertNull(TileLoader.parseCachePath("0/-2/0_10.png"));
		assertNull(TileLoader.parseCachePath("0/-2/0_10_11.txt"));
		assertNull(TileLoader.parseCachePath("nonsense"));
		assertNull(TileLoader.parseCachePath("0/zoom/0_10_11.png"));
	}
}
