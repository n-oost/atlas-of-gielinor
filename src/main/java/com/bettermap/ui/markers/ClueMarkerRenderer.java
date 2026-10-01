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
package com.bettermap.ui.markers;

import static com.bettermap.ui.MapStyle.CARD_BG;
import static com.bettermap.ui.MapStyle.CARD_TEXT;
import static com.bettermap.ui.MapStyle.CLUE_EDGE;
import static com.bettermap.ui.MapStyle.CLUE_FILL;
import static com.bettermap.ui.MapStyle.SMALL;

import com.bettermap.BetterMapConfig;
import com.bettermap.map.ClueScrollTracker;
import com.bettermap.map.MapCamera;
import com.bettermap.ui.MapRenderStats;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.util.List;
import java.util.Set;
import net.runelite.api.coords.WorldPoint;

/** Draws clue destinations and their labels. */
public class ClueMarkerRenderer
{
	private final Set<Object> visibleTooltipTargets;

	private static final Color CLUE_GLOW = new Color(150, 110, 240, 60);
	/** Below this many pixels per game tile, icons are more clutter than information. */

	/**
	 * Outline weights, thinnest to thickest. BasicStroke is immutable, so one instance each.
	 * Each renderer keeps its immutable outline strokes for reuse between frames.
	 */
	private static final Stroke MARKER_OUTLINE = new BasicStroke(1.0f);
	private static final Stroke TASK_EDGE = new BasicStroke(1.8f);
	private final BetterMapConfig config;
	private final MapCamera camera;
	private final ClueScrollTracker clueScrollTracker;
	private final MapRenderStats stats;
	public ClueMarkerRenderer(
		BetterMapConfig config,
		MapCamera camera,
		ClueScrollTracker clueScrollTracker,
		MapRenderStats stats,
		Set<Object> visibleTooltipTargets)
	{
		this.config = config;
		this.camera = camera;
		this.clueScrollTracker = clueScrollTracker;
		this.stats = stats;
		this.visibleTooltipTargets = visibleTooltipTargets;
	}

	/**
	 * The solved tile(s) of the player's active clue scroll step, read from RuneLite's clue
	 * scroll plugin via {@link ClueScrollTracker}.
	 *
	 * <p>Drawn as a purple diamond so it does not read as one of the round monster or path pins.
	 * Hot/cold and three-step clues yield several candidate tiles at once, so every location is
	 * marked and numbered. Markers show on any plane — clue targets are almost always on the
	 * surface, and hiding an off-plane one would just look broken — with the plane called out in
	 * the label when it differs from the plane on screen.
	 */
	public void drawClueMarkers(Graphics2D graphics, Rectangle bounds, List<Rectangle> placed)
	{
		if (!config.showClueScroll() || camera.getZoom() < config.clueMinZoom())
		{
			return;
		}

		final List<WorldPoint> targets = clueScrollTracker.locations();
		if (targets.isEmpty())
		{
			return;
		}

		final String name = clueScrollTracker.label();
		final int plane = camera.getPlane();
		final boolean many = targets.size() > 1;
		final boolean drawLabels = camera.getFrameZoom() > config.clueLabelMinZoom();

		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		final Stroke oldStroke = graphics.getStroke();
		graphics.setFont(SMALL);
		final FontMetrics fm = graphics.getFontMetrics();

		for (int i = 0; i < targets.size(); i++)
		{
			final WorldPoint loc = targets.get(i);
			final int sx = (int) Math.round(camera.screenX(loc.getX() + 0.5, loc.getY() + 0.5, bounds));
			final int sy = (int) Math.round(camera.screenY(loc.getX() + 0.5, loc.getY() + 0.5, bounds));

			final int r = 7;
			final Rectangle rect = new Rectangle(sx - r, sy - r, r * 2, r * 2);
			if (!bounds.intersects(rect))
			{
				continue;
			}

			final int[] dx = {sx, sx + r, sx, sx - r};
			final int[] dy = {sy - r, sy, sy + r, sy};

			graphics.setColor(CLUE_GLOW);
			graphics.fillOval(sx - r - 3, sy - r - 3, (r + 3) * 2, (r + 3) * 2);

			graphics.setColor(CLUE_FILL);
			graphics.fillPolygon(dx, dy, 4);

			graphics.setColor(CLUE_EDGE);
			graphics.setStroke(TASK_EDGE);
			graphics.drawPolygon(dx, dy, 4);

			if (many)
			{
				final String tag = Integer.toString(i + 1);
				graphics.setColor(CLUE_EDGE);
				graphics.drawString(tag, sx - fm.stringWidth(tag) / 2, sy + fm.getAscent() / 2 - 1);
			}

			placed.add(rect);
			visibleTooltipTargets.add(loc);
			stats.markersDrawn++;

			if (drawLabels && name != null)
			{
				String label = many ? name + " (" + (i + 1) + "/" + targets.size() + ")" : name;
				if (loc.getPlane() != plane)
				{
					label += " · plane " + loc.getPlane();
				}

				final int textW = fm.stringWidth(label);
				final int textX = sx - textW / 2;
				final int textY = rect.y + rect.height + 12;
				final Rectangle labelRect = new Rectangle(textX - 4, textY - 10, textW + 8, 13);
				if (bounds.intersects(labelRect))
				{
					graphics.setColor(CARD_BG);
					graphics.fillRoundRect(labelRect.x, labelRect.y, labelRect.width, labelRect.height, 4, 4);
					graphics.setColor(CLUE_EDGE);
					graphics.setStroke(MARKER_OUTLINE);
					graphics.drawRoundRect(labelRect.x, labelRect.y, labelRect.width, labelRect.height, 4, 4);
					graphics.setColor(CARD_TEXT);
					graphics.drawString(label, textX, textY);
				}
			}
		}

		graphics.setStroke(oldStroke);
	}

}
