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

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.function.BooleanSupplier;
import javax.imageio.ImageIO;
import net.runelite.client.util.Filepath;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import atlasofgielinor.tiles.TileLoader;
import atlasofgielinor.tiles.TileStore;

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

	@Test
	public void diskFallbackSkipsMissingZoomAndPreservesImageOrientation() throws Exception
	{
		Filepath root = Filepath.Unchecked.getRooted(temporary.getRoot().toPath());
		BufferedImage parent = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
		// Child (5, 6) at zoom 2 occupies x=64..127, y=64..127 in parent (1, 1).
		parent.setRGB(64, 64, 0xffff0000);
		parent.setRGB(127, 127, 0xff0000ff);
		writeTile(root, "0/0/0_1_1.png", parent);
		TileLoader loader = new TileLoader();
		loader.install(root, Arrays.asList("0/0/0_1_1.png"));
		try
		{
			await(loader::hasTiles);
			await(() -> loader.get(0, 2, 5, 6) != null);
			BufferedImage child = loader.get(0, 2, 5, 6);
			assertEquals(0xffff0000, child.getRGB(0, 0));
			assertEquals(0xffff0000, child.getRGB(3, 3));
			assertEquals(0xff0000ff, child.getRGB(255, 255));
			assertEquals(0, child.getRGB(4, 4));
		}
		finally
		{
			loader.shutDown();
		}
	}

	@Test
	public void cachedCoarseFallbackPreservesGameYAxis() throws Exception
	{
		Filepath root = Filepath.Unchecked.getRooted(temporary.getRoot().toPath());
		BufferedImage parent = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
		parent.setRGB(128, 0, 0xff336699);
		writeTile(root, "0/-2/0_1_1.png", parent);
		TileLoader loader = new TileLoader();
		loader.install(root, Arrays.asList("0/-2/0_1_1.png"));
		try
		{
			await(loader::hasTiles);
			await(() -> loader.get(0, -2, 1, 1) != null);
			BufferedImage child = loader.get(0, -1, 3, 3);
			assertNotNull(child);
			assertEquals(0xff336699, child.getRGB(0, 0));
			assertEquals(0xff336699, child.getRGB(1, 1));
			assertSame(child, loader.get(0, -1, 3, 3));
		}
		finally
		{
			loader.shutDown();
		}
	}

	@Test
	public void decodedDetailReplacesCachedAncestorFallback() throws Exception
	{
		Filepath root = Filepath.Unchecked.getRooted(temporary.getRoot().toPath());
		BufferedImage parent = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
		BufferedImage detail = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
		detail.setRGB(0, 0, 0xff336699);
		writeTile(root, "0/0/0_1_1.png", parent);
		writeTile(root, "0/2/0_5_6.png", detail);
		TileLoader loader = new TileLoader();
		loader.install(root, Arrays.asList("0/0/0_1_1.png", "0/2/0_5_6.png"));
		try
		{
			await(loader::hasTiles);
			await(() -> loader.get(0, 0, 1, 1) != null);
			BufferedImage fallback = loader.get(0, 2, 5, 6);
			assertNotNull(fallback);
			assertEquals(0xff000000, fallback.getRGB(0, 0));
			await(() -> loader.get(0, 2, 5, 6).getRGB(0, 0) == 0xff336699);
			assertNotSame(fallback, loader.get(0, 2, 5, 6));
		}
		finally
		{
			loader.shutDown();
		}
	}

	@Test
	public void cacheEvictsInInsertionOrderAndKeepsPinnedBackdrop() throws Exception
	{
		Filepath root = Filepath.Unchecked.getRooted(temporary.getRoot().toPath());
		writeTile(root, "0/-3/0_1_1.png", new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB));
		TileLoader loader = new TileLoader();
		loader.install(root, Arrays.asList("0/-3/0_1_1.png"));
		try
		{
			await(loader::hasTiles);
			await(() -> loader.get(0, -3, 1, 1) != null);
			int backdropColor = loader.get(0, -3, 1, 1).getRGB(0, 0);
			BufferedImage first = loader.get(0, 3, 64, 64);
			assertNotNull(first);
			for (int i = 1; i < 512; i++)
			{
				assertNotNull(loader.get(0, 3, 64 + i % 64, 64 + i / 64));
			}
			// Reading the oldest tile must not refresh its eviction position.
			assertSame(first, loader.get(0, 3, 64, 64));
			BufferedImage newest = loader.get(0, 3, 64, 72);
			assertNotNull(newest);
			assertEquals(backdropColor, loader.get(0, -3, 1, 1).getRGB(0, 0));
			assertNotSame(first, loader.get(0, 3, 64, 64));
			assertSame(newest, loader.get(0, 3, 64, 72));
		}
		finally
		{
			loader.shutDown();
		}
	}

	private static void writeTile(Filepath root, String path, BufferedImage image) throws Exception
	{
		Filepath tile = root.join(path);
		tile.getParent().createDirectories();
		try (java.io.OutputStream out = tile.openOutputStream())
		{
			ImageIO.write(image, "png", out);
		}
	}

	private static void await(BooleanSupplier condition)
	{
		long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
		while (!condition.getAsBoolean() && System.nanoTime() < deadline)
		{
			java.util.concurrent.locks.LockSupport.parkNanos(java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(10));
		}
		assertTrue("Timed out waiting for tile", condition.getAsBoolean());
	}
}
