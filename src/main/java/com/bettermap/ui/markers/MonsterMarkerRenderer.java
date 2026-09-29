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

import static com.bettermap.ui.MapStyle.BOSSES;
import static com.bettermap.ui.MapStyle.CARD_BG;
import static com.bettermap.ui.MapStyle.CARD_TEXT;
import static com.bettermap.ui.MapStyle.CARD_TITLE;
import static com.bettermap.ui.MapStyle.SMALL;

import com.bettermap.BetterMapConfig;
import com.bettermap.data.MonsterLocationData;
import com.bettermap.map.InstanceMaps;
import com.bettermap.map.MapCamera;
import com.bettermap.map.MonsterIconManager;
import com.bettermap.map.MonsterIndex;
import com.bettermap.map.PrifddinasShift;
import com.bettermap.map.SlayerTaskTracker;
import com.bettermap.ui.MapRenderStats;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.function.Consumer;
import net.runelite.api.coords.WorldPoint;

/**
 * Renders boss marker pins, monster habitat zones (spawns/areas), Slayer task highlights,
 * and monster labels.
 */
public class MonsterMarkerRenderer
{
	private static final Color MONSTER_FILL = new Color(224, 72, 72);
	private static final Color MONSTER_EDGE = new Color(18, 18, 18);
	private static final Color MONSTER_ZONE_FILL = new Color(224, 72, 72, 75);
	private static final Color MONSTER_ZONE_EDGE = new Color(180, 45, 45, 130);
	private static final Color MONSTER_ZONE_SLAYER = new Color(180, 90, 220, 80);
	private static final Color MONSTER_ZONE_SLAYER_EDGE = new Color(140, 60, 180, 140);
	private static final Color MONSTER_ZONE_TASK_FILL = new Color(0, 230, 220, 100);
	private static final Color MONSTER_ZONE_TASK_EDGE = new Color(20, 255, 235, 230);
	private static final Color MONSTER_ZONE_TASK_GLOW = new Color(0, 210, 200, 45);

	private static final Color ZONE_CHIP_TEXT = new Color(130, 255, 240);
	private static final Color ZONE_MARK_SLAYER = new Color(180, 90, 220, 220);
	private static final Color ZONE_MARK = new Color(224, 72, 72, 200);
	private static final Color ZONE_CHIP_SLAYER_EDGE = new Color(180, 90, 220, 200);
	private static final Color ZONE_CHIP_EDGE = new Color(224, 72, 72, 180);
	private static final Color ZONE_CHIP_SLAYER_TEXT = new Color(230, 180, 255);

	private static final Stroke MARKER_OUTLINE = new BasicStroke(1.0f);
	private static final Stroke LABEL_BORDER = new BasicStroke(1.2f);
	private static final Stroke MARKER_RING = new BasicStroke(1.5f);
	private static final Stroke TASK_EDGE = new BasicStroke(1.8f);
	private static final Stroke ACCENT_RING = new BasicStroke(2.0f);

	private final BetterMapConfig config;
	private final MapCamera camera;
	private final MonsterIndex monsterIndex;
	private final MonsterIconManager monsterIconManager;
	private final SlayerTaskTracker slayerTaskTracker;
	private final MapRenderStats stats;

	public MonsterMarkerRenderer(
		BetterMapConfig config,
		MapCamera camera,
		MonsterIndex monsterIndex,
		MonsterIconManager monsterIconManager,
		SlayerTaskTracker slayerTaskTracker,
		MapRenderStats stats)
	{
		this.config = config;
		this.camera = camera;
		this.monsterIndex = monsterIndex;
		this.monsterIconManager = monsterIconManager;
		this.slayerTaskTracker = slayerTaskTracker;
		this.stats = stats;
	}

	private void forEachMonsterInView(int plane, int minX, int maxX, int minY, int maxY,
		Consumer<MonsterIndex.Zone> consumer)
	{
		InstanceMaps.forEachQueryArea(minX, maxX, minY, maxY, camera.getCenterX(), camera.getCenterY(), false,
			(qMinX, qMaxX, qMinY, qMaxY) -> monsterIndex.forEachInArea(plane, qMinX, qMaxX, qMinY, qMaxY, zone ->
			{
				if (ViewWindow.isDrawable(camera, zone.getX(), zone.getY()))
				{
					consumer.accept(zone);
				}
			}));
	}

	private double monsterScreenX(double worldX, double worldY, Rectangle bounds)
	{
		return camera.screenX(PrifddinasShift.toDisplayX(worldX, worldY), bounds);
	}

	private double monsterScreenY(double worldX, double worldY, Rectangle bounds)
	{
		return camera.screenY(PrifddinasShift.toDisplayY(worldX, worldY), bounds);
	}

	/**
	 * Draws hard-coded boss markers from MonsterLocationData, with boss icons and pins.
	 */
	public void drawBossPins(Graphics2D graphics, Rectangle bounds, List<Rectangle> placed)
	{
		if (!config.showBossLocations())
		{
			return;
		}

		final int plane = camera.getPlane();
		final ViewWindow view = ViewWindow.from(camera, bounds, 5.0);

		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		final Stroke oldStroke = graphics.getStroke();

		final boolean drawIcons = config.showMonsterIcons();
		final boolean drawLabels = config.showMonsterLabels() && camera.getZoom() >= 0.75;
		final boolean highlightTask = config.highlightSlayerTask();
		final int pinSize = 22;

		for (MonsterLocationData monster : BOSSES)
		{
			final WorldPoint point = monster.getWorldPoint();
			if (point.getPlane() != plane)
			{
				continue;
			}

			final int mx = point.getX();
			final int my = point.getY();
			if (!camera.inView(mx, my, view.minX, view.maxX, view.minY, view.maxY)
				|| !ViewWindow.isDrawable(camera, mx, my))
			{
				continue;
			}

			final int x = (int) Math.round(camera.screenX(point.getX() + 0.5, point.getY() + 0.5, bounds));
			final int y = (int) Math.round(camera.screenY(point.getX() + 0.5, point.getY() + 0.5, bounds));

			final Rectangle rect = new Rectangle(x - pinSize / 2, y - pinSize / 2, pinSize, pinSize);
			if (!bounds.intersects(rect))
			{
				continue;
			}

			final boolean isTaskBoss = highlightTask && slayerTaskTracker.isTaskBoss(monster);

			if (drawIcons)
			{
				// Dark background
				graphics.setColor(CARD_BG);
				graphics.fillOval(rect.x, rect.y, rect.width, rect.height);

				// Outer border: bright cyan if on active Slayer task, gold otherwise
				if (isTaskBoss)
				{
					graphics.setColor(MONSTER_ZONE_TASK_EDGE);
					graphics.setStroke(ACCENT_RING);
				}
				else
				{
					graphics.setColor(CARD_TITLE);
					graphics.setStroke(MARKER_RING);
				}
				graphics.drawOval(rect.x, rect.y, rect.width, rect.height);

				// The boss's item sprite
				final BufferedImage icon = monsterIconManager.getBossIcon(monster, 18);
				if (icon != null)
				{
					graphics.drawImage(icon, x - icon.getWidth() / 2, y - icon.getHeight() / 2, null);
				}
			}
			else
			{
				graphics.setColor(MONSTER_EDGE);
				graphics.fillOval(x - 5, y - 5, 11, 11);
				graphics.setColor(isTaskBoss ? MONSTER_ZONE_TASK_EDGE : MONSTER_FILL);
				graphics.fillOval(x - 4, y - 4, 9, 9);
			}

			placed.add(rect);
			if (stats != null)
			{
				stats.markersDrawn++;
			}

			if (drawLabels)
			{
				final String label = (isTaskBoss ? "\u2694 \ud83d\udc51 " : "\ud83d\udc51 ") + monster.getName();
				graphics.setFont(SMALL);
				final int textW = graphics.getFontMetrics().stringWidth(label);
				final int textX = x - textW / 2;
				final int textY = rect.y + pinSize + 12;

				final Rectangle labelRect = new Rectangle(textX - 4, textY - 10, textW + 8, 13);
				if (bounds.intersects(labelRect))
				{
					graphics.setColor(CARD_BG);
					graphics.fillRoundRect(labelRect.x, labelRect.y, labelRect.width, labelRect.height, 4, 4);
					if (isTaskBoss)
					{
						graphics.setColor(MONSTER_ZONE_TASK_EDGE);
						graphics.setStroke(LABEL_BORDER);
						graphics.drawRoundRect(labelRect.x, labelRect.y, labelRect.width, labelRect.height, 4, 4);
						graphics.setColor(ZONE_CHIP_TEXT);
					}
					else
					{
						graphics.setColor(CARD_TITLE);
						graphics.setStroke(MARKER_OUTLINE);
						graphics.drawRoundRect(labelRect.x, labelRect.y, labelRect.width, labelRect.height, 4, 4);
						graphics.setColor(CARD_TITLE);
					}
					graphics.drawString(label, textX, textY);
				}
			}
		}

		graphics.setStroke(oldStroke);
	}

	/**
	 * Backward compatibility alias for {@link #drawBossPins}.
	 */
	public void drawMonsters(Graphics2D graphics, Rectangle bounds, List<Rectangle> placed)
	{
		drawBossPins(graphics, bounds, placed);
	}

	/**
	 * Draws monster spawn habitat zones as soft underlays, and calls {@link #drawMonsterLabels}
	 * for monster icons and text labels.
	 */
	public void drawMonsterZones(Graphics2D graphics, Rectangle bounds, List<Rectangle> placed)
	{
		if (!config.showMonsterZones() || !monsterIndex.isLoaded())
		{
			return;
		}

		final int plane = camera.getPlane();
		final ViewWindow view = ViewWindow.from(camera, bounds, 8.0);

		final double zoom = camera.getZoom();
		final boolean zoomGatePassed = zoom >= config.monsterZoneMinZoom();
		final boolean highlightTask = config.highlightSlayerTask();

		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		// 1. Soft spawn area underlay circles
		final Composite previous = graphics.getComposite();
		graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.85f));

		final Stroke prevStroke = graphics.getStroke();
		final Stroke normalStroke = MARKER_OUTLINE;
		final Stroke taskStroke = TASK_EDGE;

		forEachMonsterInView(plane, view.minXi, view.maxXi, view.minYi, view.maxYi, zone ->
		{
			final boolean isTaskZone = highlightTask && slayerTaskTracker.isTaskMonster(zone);

			// Zoom gate: active Slayer task zones ignore the gate and always show; normal zones require zoom >= threshold
			if (!isTaskZone && !zoomGatePassed)
			{
				return;
			}

			final int sx = (int) monsterScreenX(zone.getX() + 0.5, zone.getY() + 0.5, bounds);
			final int sy = (int) monsterScreenY(zone.getX() + 0.5, zone.getY() + 0.5, bounds);
			if (!bounds.intersects(sx - 60, sy - 60, 120, 120))
			{
				return;
			}

			// Radius scales with how many tiles the zone covers — a big cave reads as a soft blob.
			final int tiles = Math.max(1, zone.getTileCount());
			final double baseRadius = Math.sqrt(tiles) * zoom * 0.60;
			final int minRadius = isTaskZone ? (int) Math.max(7, Math.min(16, zoom * 4.5)) : (int) Math.max(6, Math.min(14, zoom * 4.0));
			final int maxRadius = isTaskZone ? 48 : 40;
			final int radiusPx = (int) Math.max(minRadius, Math.min(maxRadius, baseRadius));

			final int diam = radiusPx * 2;
			final int ox = sx - radiusPx;
			final int oy = sy - radiusPx;

			if (isTaskZone)
			{
				// Subtle outer task glow
				graphics.setColor(MONSTER_ZONE_TASK_GLOW);
				graphics.fillOval(ox - 3, oy - 3, diam + 6, diam + 6);

				// Vivid cyan task fill
				graphics.setColor(MONSTER_ZONE_TASK_FILL);
				graphics.fillOval(ox, oy, diam, diam);

				// Glowing task border
				graphics.setColor(MONSTER_ZONE_TASK_EDGE);
				graphics.setStroke(taskStroke);
				graphics.drawOval(ox, oy, diam, diam);
			}
			else
			{
				final boolean hasSlayerReq = zone.getSlayerLevel() > 1;
				graphics.setColor(hasSlayerReq ? MONSTER_ZONE_SLAYER : MONSTER_ZONE_FILL);
				graphics.fillOval(ox, oy, diam, diam);

				// Soft edge outline for clear visibility
				graphics.setColor(hasSlayerReq ? MONSTER_ZONE_SLAYER_EDGE : MONSTER_ZONE_EDGE);
				graphics.setStroke(normalStroke);
				graphics.drawOval(ox, oy, diam, diam);
			}
		});

		graphics.setStroke(prevStroke);
		graphics.setComposite(previous);

		// 2. Monster / slayer icons & labels at zone centroids
		drawMonsterLabels(graphics, bounds, placed, view, zoom, zoomGatePassed, highlightTask);
	}

	/**
	 * Draws monster centroid icons and labels.
	 */
	public void drawMonsterLabels(Graphics2D graphics, Rectangle bounds, List<Rectangle> placed)
	{
		if (!config.showMonsterIcons() || !monsterIndex.isLoaded())
		{
			return;
		}

		final ViewWindow view = ViewWindow.from(camera, bounds, 8.0);
		final double zoom = camera.getZoom();
		final boolean zoomGatePassed = zoom >= config.monsterZoneMinZoom();
		final boolean highlightTask = config.highlightSlayerTask();
		drawMonsterLabels(graphics, bounds, placed, view, zoom, zoomGatePassed, highlightTask);
	}

	private void drawMonsterLabels(
		Graphics2D graphics,
		Rectangle bounds,
		List<Rectangle> placed,
		ViewWindow view,
		double zoom,
		boolean zoomGatePassed,
		boolean highlightTask)
	{
		if (!config.showMonsterIcons())
		{
			return;
		}

		final int plane = camera.getPlane();
		final boolean drawLabels = config.showMonsterLabels() && zoom >= 0.85;
		final int iconSize = 18;
		final Stroke iconPrevStroke = graphics.getStroke();

		forEachMonsterInView(plane, view.minXi, view.maxXi, view.minYi, view.maxYi, zone ->
		{
			final boolean isTaskZone = highlightTask && slayerTaskTracker.isTaskMonster(zone);

			// Gating for icons: task zones show icons even when zoomed out (zoom >= 0.30); normal icons require zoom >= 0.40 & zoomGate
			if (!isTaskZone && (zoom < 0.40 || !zoomGatePassed))
			{
				return;
			}
			if (isTaskZone && zoom < 0.30)
			{
				return;
			}

			final int sx = (int) Math.round(monsterScreenX(zone.getX() + 0.5, zone.getY() + 0.5, bounds));
			final int sy = (int) Math.round(monsterScreenY(zone.getX() + 0.5, zone.getY() + 0.5, bounds));

			final Rectangle rect = new Rectangle(sx - iconSize / 2, sy - iconSize / 2, iconSize, iconSize);
			if (!bounds.intersects(rect))
			{
				return;
			}

			if (ViewWindow.overlapsPlaced(placed, rect))
			{
				return;
			}

			// Dark circular chip background
			graphics.setColor(CARD_BG);
			graphics.fillOval(rect.x, rect.y, rect.width, rect.height);

			// Border: Cyan for Slayer Task, Purple for Slayer level required, Crimson for regular monsters
			if (isTaskZone)
			{
				graphics.setColor(MONSTER_ZONE_TASK_EDGE);
				graphics.setStroke(TASK_EDGE);
			}
			else
			{
				final boolean isSlayer = zone.getSlayerLevel() > 1;
				graphics.setColor(isSlayer ? ZONE_MARK_SLAYER : ZONE_MARK);
				graphics.setStroke(LABEL_BORDER);
			}
			graphics.drawOval(rect.x, rect.y, rect.width, rect.height);

			// The monster's item sprite
			final BufferedImage icon = monsterIconManager.getZoneIcon(zone, 14);
			if (icon != null)
			{
				graphics.drawImage(icon, sx - icon.getWidth() / 2, sy - icon.getHeight() / 2, null);
			}

			placed.add(rect);
			if (stats != null)
			{
				stats.markersDrawn++;
			}

			if (drawLabels || (isTaskZone && zoom >= 0.65))
			{
				final String label = (isTaskZone ? "\u2694 " : "") + zone.getMonster() + (zone.getCombatLevel() > 0 ? " (" + zone.getCombatLevel() + ")" : "");
				graphics.setFont(SMALL);
				final int textW = graphics.getFontMetrics().stringWidth(label);
				final int textX = sx - textW / 2;
				final int textY = rect.y + iconSize + 11;

				final Rectangle labelRect = new Rectangle(textX - 3, textY - 9, textW + 6, 12);
				if (bounds.intersects(labelRect))
				{
					graphics.setColor(CARD_BG);
					graphics.fillRoundRect(labelRect.x, labelRect.y, labelRect.width, labelRect.height, 3, 3);
					if (isTaskZone)
					{
						graphics.setColor(MONSTER_ZONE_TASK_EDGE);
						graphics.setStroke(LABEL_BORDER);
						graphics.drawRoundRect(labelRect.x, labelRect.y, labelRect.width, labelRect.height, 3, 3);
						graphics.setColor(ZONE_CHIP_TEXT);
					}
					else
					{
						final boolean isSlayer = zone.getSlayerLevel() > 1;
						graphics.setColor(isSlayer ? ZONE_CHIP_SLAYER_EDGE : ZONE_CHIP_EDGE);
						graphics.setStroke(MARKER_OUTLINE);
						graphics.drawRoundRect(labelRect.x, labelRect.y, labelRect.width, labelRect.height, 3, 3);
						graphics.setColor(isSlayer ? ZONE_CHIP_SLAYER_TEXT : CARD_TEXT);
					}
					graphics.drawString(label, textX, textY);
				}
			}
		});

		graphics.setStroke(iconPrevStroke);
	}
}
