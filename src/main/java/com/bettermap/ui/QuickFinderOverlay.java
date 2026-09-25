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

import static com.bettermap.ui.MapStyle.CARD_BG;
import static com.bettermap.ui.MapStyle.CARD_EDGE;
import static com.bettermap.ui.MapStyle.CARD_TITLE;
import static com.bettermap.ui.MapStyle.TEXT_DIM;

import com.bettermap.BetterMapConfig;
import com.bettermap.map.MapCamera;
import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * A magnifier orb below the client's world-map orb that opens the Better Map location Finder
 * without the full world map. Always-on: it runs every frame and paints only when the world map
 * is closed and {@link BetterMapConfig#showFinderOrb()} is set.
 *
 * <p>The orb only publishes its hit rect ({@link MapCamera#setQuickFinderOrb}); the click that
 * toggles the card is handled in {@code WorldMapInput}, and the card itself is drawn by the
 * existing {@link BetterWorldMapOverlay#drawStandaloneFinder} path.
 */
@Singleton
public class QuickFinderOverlay extends Overlay
{
	private static final int GAP_BELOW_ORB = 4;
	private static final Stroke ORB_GLYPH_STROKE = new BasicStroke(1.6f);

	private final Client client;
	private final BetterMapConfig config;
	private final MapCamera camera;
	private final BetterWorldMapOverlay worldMapOverlay;

	@Inject
	public QuickFinderOverlay(Client client, BetterMapConfig config, MapCamera camera,
		BetterWorldMapOverlay worldMapOverlay)
	{
		this.client = client;
		this.config = config;
		this.camera = camera;
		this.worldMapOverlay = worldMapOverlay;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPriority(PRIORITY_HIGH);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		// The full world map has its own in-map Find button; the orb is the map-closed entry point.
		if (!config.showFinderOrb() || camera.isActive())
		{
			camera.setQuickFinderOrb(null);
			return null;
		}

		final Rectangle worldMapOrb = MinimapOrbs.worldMapOrbBounds(client);
		if (worldMapOrb == null)
		{
			camera.setQuickFinderOrb(null);
			return null;
		}

		final int size = worldMapOrb.width;
		final Rectangle orb = new Rectangle(
			worldMapOrb.x + camera.getFinderOrbOffsetX(),
			worldMapOrb.y + worldMapOrb.height + GAP_BELOW_ORB + camera.getFinderOrbOffsetY(),
			size, size);

		// Keep it fully on the canvas — the drag offset is unbounded and the canvas resizes.
		final int cw = client.getCanvasWidth();
		final int ch = client.getCanvasHeight();
		if (cw > size && ch > size)
		{
			orb.x = Math.max(0, Math.min(orb.x, cw - size));
			orb.y = Math.max(0, Math.min(orb.y, ch - size));
		}
		camera.setQuickFinderOrb(orb);

		final boolean open = camera.isFinderStandalone() && camera.isFinderPanelOpen();
		drawOrb(graphics, orb, open);

		if (open)
		{
			worldMapOverlay.drawStandaloneFinder(graphics, orb,
				new Rectangle(0, 0, client.getCanvasWidth(), client.getCanvasHeight()));
		}

		return null;
	}

	private static void drawOrb(Graphics2D graphics, Rectangle orb, boolean open)
	{
		final Object aa = graphics.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
		final Stroke stroke = graphics.getStroke();
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		graphics.setColor(CARD_BG);
		graphics.fillOval(orb.x, orb.y, orb.width, orb.height);
		graphics.setColor(open ? CARD_TITLE : CARD_EDGE);
		graphics.drawOval(orb.x, orb.y, orb.width, orb.height);

		// Magnifier glyph, same shape as the in-map Find button (MapFinderRenderer).
		final int r = Math.max(5, orb.width / 3);
		final int gx = orb.x + orb.width / 2 - r / 2 - 1;
		final int gy = orb.y + orb.height / 2 - r / 2 - 1;
		graphics.setColor(open ? CARD_TITLE : TEXT_DIM);
		graphics.setStroke(ORB_GLYPH_STROKE);
		graphics.drawOval(gx, gy, r, r);
		graphics.drawLine(gx + r, gy + r, gx + r + Math.max(3, r / 2), gy + r + Math.max(3, r / 2));

		graphics.setStroke(stroke);
		if (aa != null)
		{
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, aa);
		}
	}
}
