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
package com.bettermap.ui;

/**
 * Per-frame draw counters, shared by the renderers that increment them and the debug panel
 * that reports them. One holder rather than a copy per renderer, so the panel still sees the
 * whole frame after {@link BetterWorldMapOverlay}'s draw stages were split up.
 */
public class MapRenderStats
{
	public int tilesDrawn;
	public int tilesMissing;
	public int iconsDrawn;
	public int iconsSkipped;
	public int iconsFiltered;
	public int markersDrawn;

	/** Tile zoom level the last tile pass drew at; survives a frame that draws no tiles. */
	public int tileZoomInUse;

	/** Zeroes exactly the counters {@code render()} zeroed before the split — not the tile zoom. */
	public void reset()
	{
		tilesDrawn = 0;
		tilesMissing = 0;
		iconsDrawn = 0;
		iconsSkipped = 0;
		iconsFiltered = 0;
		markersDrawn = 0;
	}
}
