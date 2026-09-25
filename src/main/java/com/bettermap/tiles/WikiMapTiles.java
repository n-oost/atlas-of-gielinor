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

/**
 * Slippy-tile geometry for the bundled map. The tiles themselves are terrain renders produced
 * from the user's local OSRS game cache (see {@code tools/CacheTileDumper}); this class only does
 * the coordinate math, which follows the same slippy scheme the OSRS Wiki map uses.
 *
 * <p>Tiles are 256px and their coordinate system is simply game coordinates: at zoom {@code z}
 * one game tile is {@code 2^z} pixels, so zoom 2 is a 4px-per-tile render and zoom 3 is 8px.
 * There is no projection and no offset to calibrate - a game coordinate divides straight into a
 * tile index.
 *
 * <p>Zoom runs from -3 (the whole world in a few hundred pixels, which is what makes a real
 * zoomed-out view possible) up to 3.
 *
 * <p>Tiles are stored as {@code tiles/{mapID}/{z}/{p}_{x}_{y}.png}, with the y index the plain
 * positive one derived from the game's y axis.
 */
public final class WikiMapTiles
{
	/** Credit for the bundled tiles, which are rendered from the game cache. */
	public static final String ATTRIBUTION = "Map imagery © Jagex Ltd.";
	/** Credit for the bundled POI, shop, monster and ground-item datasets. */
	public static final String DATA_ATTRIBUTION = "POI, shop and monster data © OSRS Wiki";

	public static final int TILE_SIZE = 256;
	public static final int MIN_ZOOM = -3;
	public static final int MAX_ZOOM = 3;
	private static final double ZOOM_HYSTERESIS = 0.05;

	private WikiMapTiles()
	{
	}

	/** Image pixels covering one game tile at {@code zoom}. */
	public static double pixelsPerWorldTile(int zoom)
	{
		return Math.pow(2, zoom);
	}

	/** Game tiles covered by one 256px image tile at {@code zoom}. Always a whole number. */
	public static int worldTilesPerImageTile(int zoom)
	{
		return 1 << (8 - zoom);
	}

	public static int tileIndex(double worldCoordinate, int zoom)
	{
		return (int) Math.floor(worldCoordinate / worldTilesPerImageTile(zoom));
	}

	/** Game coordinate of a tile's low corner - west edge for x, south edge for y. */
	public static double tileOrigin(int tileIndex, int zoom)
	{
		return (double) tileIndex * worldTilesPerImageTile(zoom);
	}

	/**
	 * Lowest zoom whose tiles are still at least as detailed as we are drawing, so tiles get
	 * downscaled rather than blown up. Clamped to what the service actually publishes.
	 */
	public static int bestZoom(double screenPixelsPerWorldTile)
	{
		final int zoom = (int) Math.ceil(Math.log(Math.max(1e-6, screenPixelsPerWorldTile)) / Math.log(2));
		return Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom));
	}

	/**
	 * Stateful tile-level selection. A level changes only after zoom crosses five percent beyond
	 * the shared boundary, preventing wheel/trackpad noise from alternating mip levels each frame.
	 */
	public static int bestZoom(double screenPixelsPerWorldTile, int currentZoom)
	{
		final int candidate = bestZoom(screenPixelsPerWorldTile);
		if (currentZoom < MIN_ZOOM || currentZoom > MAX_ZOOM || candidate == currentZoom)
		{
			return candidate;
		}

		if (candidate > currentZoom)
		{
			final double upperBoundary = pixelsPerWorldTile(currentZoom);
			return screenPixelsPerWorldTile <= upperBoundary * (1.0 + ZOOM_HYSTERESIS)
				? currentZoom : candidate;
		}

		final double lowerBoundary = pixelsPerWorldTile(currentZoom - 1);
		return screenPixelsPerWorldTile >= lowerBoundary * (1.0 - ZOOM_HYSTERESIS)
			? currentZoom : candidate;
	}

	public static int clampZoomLevel(int zoom)
	{
		return Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom));
	}

	/** Cache path, relative to the tile directory. */
	public static String cachePath(int plane, int zoom, int tileX, int tileY)
	{
		return "0/" + zoom + "/" + plane + "_" + tileX + "_" + tileY + ".png";
	}
}
