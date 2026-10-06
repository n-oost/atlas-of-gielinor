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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import atlasofgielinor.tiles.TileLoader;

public class TileLoaderTest
{
	private TileLoader tileLoader;

	@Before
	public void setUp()
	{
		tileLoader = new TileLoader();
		tileLoader.startUp();
	}

	@After
	public void tearDown()
	{
		tileLoader.shutDown();
	}

	@Test
	public void tileKeyIsUniquePerCoordinateAndZoom()
	{
		final long k1 = TileLoader.key(0, 2, 50, 50);
		final long k2 = TileLoader.key(0, 2, 50, 51);
		final long k3 = TileLoader.key(0, 3, 50, 50);
		final long k4 = TileLoader.key(1, 2, 50, 50);

		assertTrue(k1 != k2);
		assertTrue(k1 != k3);
		assertTrue(k1 != k4);
	}

	@Test
	public void missingTilesReturnNullCleanlyWithoutException()
	{
		final BufferedImage tile = tileLoader.get(0, 2, 9999, 9999);
		assertNull(tile);
	}

	@Test
	public void wyrmscraigCavernTilesAreBundledAndReadable()
	{
		for (String tile : new String[] {
			"0/3/0_80_268.png", "0/3/0_80_269.png", "0/3/0_80_270.png",
			"0/3/0_81_268.png", "0/3/0_81_269.png" })
		{
			final BufferedImage image = TileLoader.readBundledCavernTile(tile);
			assertNotNull(tile, image);
			assertEquals(256, image.getWidth());
			assertEquals(256, image.getHeight());
		}
		assertNull(TileLoader.readBundledCavernTile("0/3/0_82_269.png"));
	}
}
