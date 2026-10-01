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
import static com.bettermap.ui.MapStyle.SMALL;

import com.bettermap.BetterMapConfig;
import com.bettermap.map.GroundItemIndex;
import com.bettermap.map.MapCamera;
import com.bettermap.map.MonsterIconManager;
import com.bettermap.ui.MapRenderStats;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Set;
import net.runelite.client.ui.FontManager;

/** Draws ground item spawn markers and their labels. */
public class GroundItemMarkerRenderer
{
	private final Set<Object> visibleTooltipTargets;

	private static final Font TINY = FontManager.getDefaultBoldFont().deriveFont(9f);
	/** Ground item spawns: green, so they never read as a monster (red) or Slayer zone (purple). */
	private static final Color GROUND_ITEM_EDGE = new Color(126, 214, 134, 210);
	private static final Color GROUND_ITEM_DOT = new Color(126, 214, 134, 235);

	/**
	 * Outline weights, thinnest to thickest. BasicStroke is immutable, so one instance each.
	 * Each renderer keeps its immutable outline strokes for reuse between frames.
	 */
	private static final Stroke MARKER_OUTLINE = new BasicStroke(1.0f);
	private final BetterMapConfig config;
	private final MapCamera camera;
	private final MonsterIconManager monsterIconManager;
	private final GroundItemIndex groundItemIndex;
	private final MapRenderStats stats;

	public GroundItemMarkerRenderer(
		BetterMapConfig config,
		MapCamera camera,
		MonsterIconManager monsterIconManager,
		GroundItemIndex groundItemIndex,
		MapRenderStats stats,
		Set<Object> visibleTooltipTargets)
	{
		this.config = config;
		this.camera = camera;
		this.monsterIconManager = monsterIconManager;
		this.groundItemIndex = groundItemIndex;
		this.stats = stats;
		this.visibleTooltipTargets = visibleTooltipTargets;
	}

	/**
	 * Wiki ground item spawn tiles, drawn as the item's own sprite.
	 *
	 * <p>Hidden until the camera is close in — 4,361 tiles at world zoom would be a rash of dots
	 * over every town — and filtered by the min-value slider, which defaults to showing everything.
	 * Shares {@code placed} with the monster and boss markers so a bucket never covers a boss.
	 */
	public void drawGroundItems(Graphics2D graphics, Rectangle bounds, List<Rectangle> placed)
	{
		if (!config.showGroundItems() || !groundItemIndex.isLoaded())
		{
			return;
		}

		final double zoom = camera.getZoom();
		if (zoom < config.groundItemMinZoom())
		{
			return;
		}

		final int plane = camera.getPlane();
		final ViewWindow view = ViewWindow.from(camera, bounds, 4.0);

		final int minValue = config.groundItemMinValue();
		final boolean drawLabels = config.showGroundItemLabels() && zoom >= config.groundItemLabelMinZoom();
		final int chipSize = 16;

		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		final Stroke prevStroke = graphics.getStroke();

		groundItemIndex.forEachInArea(plane, view.minXi, view.maxXi, view.minYi, view.maxYi, spawn ->
		{
			if (spawn.getValue() < minValue || !ViewWindow.isDrawable(camera, spawn.getX(), spawn.getY()))
			{
				return;
			}

			final int sx = (int) Math.round(camera.screenX(spawn.getX() + 0.5, spawn.getY() + 0.5, bounds));
			final int sy = (int) Math.round(camera.screenY(spawn.getX() + 0.5, spawn.getY() + 0.5, bounds));

			final Rectangle rect = new Rectangle(sx - chipSize / 2, sy - chipSize / 2, chipSize, chipSize);
			if (!bounds.intersects(rect) || ViewWindow.overlapsPlaced(placed, rect))
			{
				return;
			}

			final GroundItemIndex.Item primary = spawn.getPrimary();

			graphics.setColor(CARD_BG);
			graphics.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 4, 4);
			graphics.setColor(GROUND_ITEM_EDGE);
			graphics.setStroke(MARKER_OUTLINE);
			graphics.drawRoundRect(rect.x, rect.y, rect.width, rect.height, 4, 4);

			final BufferedImage icon = monsterIconManager.getItemIcon(primary.getId(), 13);
			if (icon != null)
			{
				graphics.drawImage(icon, sx - icon.getWidth() / 2, sy - icon.getHeight() / 2, null);
			}
			else
			{
				// No sprite (offline, or an id the client does not know): a dot still marks the tile.
				graphics.setColor(GROUND_ITEM_DOT);
				graphics.fillOval(sx - 3, sy - 3, 6, 6);
			}

			// A second spawn on the same tile is only ever a count here; the tooltip names both.
			if (spawn.getItems().size() > 1)
			{
				graphics.setFont(TINY);
				graphics.setColor(GROUND_ITEM_EDGE);
				graphics.drawString("+" + (spawn.getItems().size() - 1), rect.x + rect.width - 4, rect.y + 8);
			}

			placed.add(rect);
			visibleTooltipTargets.add(spawn);
			stats.markersDrawn++;

			if (drawLabels)
			{
				final String label = primary.getQuantity() > 1
					? primary.getName() + " ×" + primary.getQuantity()
					: primary.getName();
				graphics.setFont(SMALL);
				final int textW = graphics.getFontMetrics().stringWidth(label);
				final int textX = sx - textW / 2;
				final int textY = rect.y + chipSize + 11;

				final Rectangle labelRect = new Rectangle(textX - 3, textY - 9, textW + 6, 12);
				if (bounds.intersects(labelRect))
				{
					graphics.setColor(CARD_BG);
					graphics.fillRoundRect(labelRect.x, labelRect.y, labelRect.width, labelRect.height, 3, 3);
					graphics.setColor(GROUND_ITEM_EDGE);
					graphics.setStroke(MARKER_OUTLINE);
					graphics.drawRoundRect(labelRect.x, labelRect.y, labelRect.width, labelRect.height, 3, 3);
					graphics.setColor(CARD_TEXT);
					graphics.drawString(label, textX, textY);
				}
			}
		});

		graphics.setStroke(prevStroke);
	}

}
