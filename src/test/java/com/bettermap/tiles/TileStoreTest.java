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
import java.util.Arrays;
import javax.imageio.ImageIO;
import net.runelite.client.util.Filepath;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class TileStoreTest
{
	@Rule public TemporaryFolder temporary = new TemporaryFolder();

	@Test
	public void missingInstallationDoesNotUseClasspathOrLegacyCache()
	{
		assertNull(TileStore.read(null, "0/-3/0_1_1.png"));
		TileLoader loader = new TileLoader();
		loader.startUp();
		try
		{
			assertFalse(loader.hasTiles());
			assertNull(loader.get(0, 2, 50, 50));
		}
		finally
		{
			loader.shutDown();
		}
	}

	@Test
	public void readsInstalledTileAndRecognizesZoomThree() throws Exception
	{
		Filepath root = Filepath.Unchecked.getRooted(temporary.getRoot().toPath());
		Filepath tile = root.join("0/3/0_1_1.png");
		tile.getParent().createDirectories();
		BufferedImage image = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
		image.setRGB(0, 0, 0xff336699);
		try (java.io.OutputStream out = tile.openOutputStream())
		{
			ImageIO.write(image, "png", out);
		}
		assertEquals(0xff336699, TileStore.read(root, "0/3/0_1_1.png").getRGB(0, 0));
		TileLoader loader = new TileLoader();
		loader.install(root, Arrays.asList("0/3/0_1_1.png"));
		try
		{
			long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
			while (!loader.hasTiles() && System.nanoTime() < deadline)
			{
				Thread.yield();
			}
			assertTrue(loader.hasTiles());
			assertEquals(3, loader.maxAvailableZoom());
		}
		finally
		{
			loader.shutDown();
		}
		assertFalse(loader.hasTiles());
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
