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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class WikiMapTilesTest
{
	private static final double DELTA = 1e-9;

	@Test
	public void oneGameTileIsTwoToThePowerOfTheZoom()
	{
		assertEquals(8d, WikiMapTiles.pixelsPerWorldTile(3), DELTA);
		assertEquals(4d, WikiMapTiles.pixelsPerWorldTile(2), DELTA);
		assertEquals(1d, WikiMapTiles.pixelsPerWorldTile(0), DELTA);
		assertEquals(0.125d, WikiMapTiles.pixelsPerWorldTile(-3), DELTA);
	}

	@Test
	public void imageTilesCoverAWholeNumberOfGameTiles()
	{
		assertEquals(32, WikiMapTiles.worldTilesPerImageTile(3));
		assertEquals(64, WikiMapTiles.worldTilesPerImageTile(2));
		assertEquals(2048, WikiMapTiles.worldTilesPerImageTile(-3));
	}

	@Test
	public void gameCoordinatesDivideStraightIntoTileIndices()
	{
		// Lumbridge Castle. Verified against the live service: /2/0_50_50.png exists.
		assertEquals(50, WikiMapTiles.tileIndex(3222, 2));
		assertEquals(50, WikiMapTiles.tileIndex(3218, 2));
		assertEquals(100, WikiMapTiles.tileIndex(3222, 3));
		assertEquals(1, WikiMapTiles.tileIndex(3222, -3));
	}

	@Test
	public void tileOriginIsTheInverseOfTileIndex()
	{
		assertEquals(3200d, WikiMapTiles.tileOrigin(50, 2), DELTA);

		for (int zoom = WikiMapTiles.MIN_ZOOM; zoom <= WikiMapTiles.MAX_ZOOM; zoom++)
		{
			final int index = WikiMapTiles.tileIndex(3222, zoom);
			final double origin = WikiMapTiles.tileOrigin(index, zoom);
			assertTrue(origin <= 3222);
			assertTrue(origin + WikiMapTiles.worldTilesPerImageTile(zoom) > 3222);
		}
	}

	@Test
	public void zoomIsPickedSoTilesAreNeverUpscaled()
	{
		assertEquals(3, WikiMapTiles.bestZoom(8));
		assertEquals(2, WikiMapTiles.bestZoom(4));
		assertEquals(2, WikiMapTiles.bestZoom(3));
		assertEquals(0, WikiMapTiles.bestZoom(1));
		assertEquals(-3, WikiMapTiles.bestZoom(0.125));
		assertEquals(WikiMapTiles.MAX_ZOOM, WikiMapTiles.bestZoom(1000));
		assertEquals(WikiMapTiles.MIN_ZOOM, WikiMapTiles.bestZoom(0.0001));

		for (double zoom = 0.05; zoom < 30; zoom += 0.05)
		{
			final int level = WikiMapTiles.bestZoom(zoom);
			assertTrue(level >= WikiMapTiles.MIN_ZOOM && level <= WikiMapTiles.MAX_ZOOM);
		}
	}

	@Test
	public void zoomHysteresisDoesNotOscillateAtPowerOfTwoBoundary()
	{
		int level = WikiMapTiles.bestZoom(3.99);
		assertEquals(2, level);

		level = WikiMapTiles.bestZoom(4.01, level);
		assertEquals(2, level);
		level = WikiMapTiles.bestZoom(3.99, level);
		assertEquals(2, level);
		level = WikiMapTiles.bestZoom(4.21, level);
		assertEquals(3, level);

		level = WikiMapTiles.bestZoom(3.99, level);
		assertEquals(3, level);
		level = WikiMapTiles.bestZoom(3.79, level);
		assertEquals(2, level);
	}

	@Test
	public void cachePathMatchesUnifiedLayout()
	{
		assertEquals("0/2/0_50_50.png", WikiMapTiles.cachePath(0, 2, 50, 50));
	}

	@Test
	public void layerLookupPrefersTheDungeonOverTheSurface()
	{
		assertEquals(WikiMap.SURFACE, WikiMap.forPoint(3222, 3218));

		// General Graardor, which the plugin ships as a boss marker.
		assertEquals(WikiMap.GOD_WARS_DUNGEON, WikiMap.forPoint(2867, 5357));
	}

	@Test
	public void surfaceCoversTheKnownExtentOfTheOverworld()
	{
		assertTrue(WikiMap.SURFACE.contains(3222, 3218));
		assertTrue(WikiMap.SURFACE.contains(1250, 3750));
		assertTrue(WikiMap.SURFACE.contains(3800, 3900));
		assertTrue(!WikiMap.SURFACE.contains(3222, 9000));
	}

	@Test
	public void newLayersCanBeLookedUpById()
	{
		assertEquals(WikiMap.SURFACE, WikiMap.byId(0));
		assertEquals(WikiMap.PRIFDDINAS, WikiMap.byId(29));
		assertEquals(WikiMap.RUINS_OF_CAMDOZAAL, WikiMap.byId(39));
		assertEquals(WikiMap.THE_ABYSS, WikiMap.byId(40));
		assertEquals(WikiMap.PURO_PURO, WikiMap.byId(10128));
		assertEquals(WikiMap.WATERFALL_DUNGEON, WikiMap.byId(10176));
		assertNull(WikiMap.byId(-999));
	}

	@Test
	public void layerLookupFindsExpandedDungeons()
	{
		assertEquals(WikiMap.PRIFDDINAS, WikiMap.forPoint(3264, 6080));
		assertEquals(WikiMap.RUINS_OF_CAMDOZAAL, WikiMap.forPoint(2976, 5824));
		assertEquals(WikiMap.PURO_PURO, WikiMap.forPoint(2592, 4320));
	}

	@Test
	public void layerCountAndIntegrity()
	{
		assertEquals(221, WikiMap.values().length);
		for (WikiMap map : WikiMap.values())
		{
			assertTrue(map.getMaxX() > map.getMinX());
			assertTrue(map.getMaxY() > map.getMinY());
			assertTrue(map.area() > 0);
		}
	}
}
