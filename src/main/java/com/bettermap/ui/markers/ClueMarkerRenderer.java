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
import static com.bettermap.ui.MapStyle.CARD_TITLE;
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
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;

/**
 * Renders clue scroll diamond pins, multi-step numbers, labels,
 * and search highlight flash circle animations.
 */
public class ClueMarkerRenderer
{
	private static final Color CLUE_GLOW = new Color(150, 110, 240, 60);
	private static final long FLASH_MILLIS = 1600L;
	private static final double ICON_ZOOM_THRESHOLD = 0.45;

	private static final Stroke TASK_EDGE = new BasicStroke(1.8f);
	private static final Stroke MARKER_OUTLINE = new BasicStroke(1.0f);
	private static final Stroke ACCENT_RING = new BasicStroke(2.0f);

	/** True for the clue scroll plugin's own world map point (ClueScrollWorldMapPoint). */
	private static final Map<Class<?>, Boolean> CLUE_POINT_CLASS = new IdentityHashMap<>();

	private final BetterMapConfig config;
	private final MapCamera camera;
	private final ClueScrollTracker clueScrollTracker;
	private final MapRenderStats stats;

	public ClueMarkerRenderer(
		BetterMapConfig config,
		MapCamera camera,
		ClueScrollTracker clueScrollTracker,
		MapRenderStats stats)
	{
		this.config = config;
		this.camera = camera;
		this.clueScrollTracker = clueScrollTracker;
		this.stats = stats;
	}

	public static boolean isClueWorldMapPoint(WorldMapPoint point)
	{
		if (point == null)
		{
			return false;
		}
		final Class<?> type = point.getClass();
		final Boolean cached = CLUE_POINT_CLASS.get(type);
		if (cached != null)
		{
			return cached;
		}
		boolean match = false;
		for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass())
		{
			if (c.getName().contains("cluescrolls"))
			{
				match = true;
				break;
			}
		}
		CLUE_POINT_CLASS.put(type, match);
		return match;
	}

	/**
	 * Draws clue scroll diamond pins, numbered steps, and labels.
	 */
	public void drawClueDiamonds(Graphics2D graphics, Rectangle bounds, List<Rectangle> placed)
	{
		if (!config.showClueScroll())
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
		final boolean drawLabels = camera.getFrameZoom() > ICON_ZOOM_THRESHOLD;

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
			if (stats != null)
			{
				stats.markersDrawn++;
			}

			if (drawLabels && name != null)
			{
				String label = many ? name + " (" + (i + 1) + "/" + targets.size() + ")" : name;
				if (loc.getPlane() != plane)
				{
					label += " \u00b7 plane " + loc.getPlane();
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

	/**
	 * Backward compatibility alias for {@link #drawClueDiamonds}.
	 */
	public void drawClueMarkers(Graphics2D graphics, Rectangle bounds, List<Rectangle> placed)
	{
		drawClueDiamonds(graphics, bounds, placed);
	}

	/**
	 * Flashes a location when centered on by search or sidebar clicks.
	 */
	public void drawFlash(Graphics2D graphics, Rectangle bounds)
	{
		final WorldPoint flashPoint = camera.getFlashPoint();
		if (flashPoint == null)
		{
			return;
		}

		final long elapsed = System.currentTimeMillis() - camera.getFlashStartMillis();
		if (elapsed > FLASH_MILLIS)
		{
			camera.clearFlash();
			return;
		}

		if (flashPoint.getPlane() != camera.getPlane())
		{
			return;
		}

		final int sx = (int) Math.round(camera.screenX(flashPoint.getX() + 0.5, flashPoint.getY() + 0.5, bounds));
		final int sy = (int) Math.round(camera.screenY(flashPoint.getX() + 0.5, flashPoint.getY() + 0.5, bounds));

		if (!bounds.contains(sx, sy))
		{
			return;
		}

		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		final Stroke oldStroke = graphics.getStroke();
		graphics.setStroke(ACCENT_RING);

		final double totalPhase = elapsed / (double) FLASH_MILLIS;
		final int coreAlpha = Math.max(0, Math.min(255, (int) (200 * (1.0 - totalPhase))));
		if (coreAlpha > 0)
		{
			graphics.setColor(new Color(CARD_TITLE.getRed(), CARD_TITLE.getGreen(), CARD_TITLE.getBlue(), coreAlpha));
			final int coreRadius = 5;
			graphics.drawOval(sx - coreRadius, sy - coreRadius, coreRadius * 2, coreRadius * 2);
		}

		for (int i = 0; i < 3; i++)
		{
			final double phase = elapsed / (double) FLASH_MILLIS - i * 0.22;
			if (phase < 0 || phase > 1)
			{
				continue;
			}
			final int r = (int) Math.round(6 + 34 * phase);
			final int alpha = Math.max(0, Math.min(255, (int) (200 * (1.0 - phase))));
			graphics.setColor(new Color(CARD_TITLE.getRed(), CARD_TITLE.getGreen(), CARD_TITLE.getBlue(), alpha));
			graphics.drawOval(sx - r, sy - r, r * 2, r * 2);
		}

		graphics.setStroke(oldStroke);
	}
}
