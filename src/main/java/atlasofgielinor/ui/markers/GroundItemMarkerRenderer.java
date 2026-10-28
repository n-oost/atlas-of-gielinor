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
package atlasofgielinor.ui.markers;

import static atlasofgielinor.ui.MapStyle.MARKER_OUTLINE;
import static atlasofgielinor.ui.MapStyle.TINY;

import static atlasofgielinor.ui.MapStyle.CARD_BG;
import static atlasofgielinor.ui.MapStyle.CARD_TEXT;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

import atlasofgielinor.AtlasOfGielinorConfig;
import atlasofgielinor.data.dungeons.DungeonPiece;
import atlasofgielinor.data.dungeons.DungeonPieceTransform;
import atlasofgielinor.data.dungeons.UndergroundZone;
import atlasofgielinor.map.DungeonPieceIndex;
import atlasofgielinor.map.InstanceMaps;
import atlasofgielinor.map.catalog.GroundItemIndex;
import atlasofgielinor.map.MapCamera;
import atlasofgielinor.map.MonsterIconManager;

import lombok.RequiredArgsConstructor;

/** Draws ground item spawn markers and their labels. */
@RequiredArgsConstructor
public class GroundItemMarkerRenderer
{

	/** Ground item spawns: green, so they never read as a monster (red) or Slayer zone (purple). */
	private static final Color GROUND_ITEM_EDGE = new Color(126, 214, 134, 210);
	private static final Color GROUND_ITEM_DOT = new Color(126, 214, 134, 235);

	private final AtlasOfGielinorConfig config;
	private final MapCamera camera;
	private final MonsterIconManager monsterIconManager;
	private final GroundItemIndex groundItemIndex;
	private final DungeonPieceIndex dungeonPieceIndex;
	private final Set<Object> visibleTooltipTargets;

	public GroundItemMarkerRenderer(
		AtlasOfGielinorConfig config,
		MapCamera camera,
		MonsterIconManager monsterIconManager,
		GroundItemIndex groundItemIndex,
		Set<Object> visibleTooltipTargets)
	{
		this(config, camera, monsterIconManager, groundItemIndex, null, visibleTooltipTargets);
	}

	private void forEachZoneSpawnInView(UndergroundZone zone, int minX, int maxX, int minY, int maxY,
		int minValue, BiConsumer<GroundItemIndex.Spawn, Point2D> consumer)
	{
		final java.util.Set<String> seen = new java.util.HashSet<>();
		final List<DungeonPiece> pieces = dungeonPieceIndex == null
			? java.util.Collections.emptyList()
			: dungeonPieceIndex.piecesFor(zone.getId());
		if (pieces.isEmpty() && !zone.getId().startsWith("native_"))
		{
			final int x = zone.getUndergroundPoint().getX();
			final int y = zone.getUndergroundPoint().getY();
			final int radius = zone.getRadius();
			groundItemIndex.forEachInArea(camera.effectivePlaneFor(zone),
				x - radius, x + radius, y - radius, y + radius, spawn ->
				{
					if (spawn.getValue() >= minValue && InstanceMaps.belongsToZone(spawn.getX(), spawn.getY(), zone))
					{
						final Point2D pt = new Point2D.Double(
							spawn.getX() + 0.5 - zone.getDeltaX(),
							spawn.getY() + 0.5 - zone.getDeltaY());
						if (pt.getX() >= minX && pt.getX() <= maxX
							&& pt.getY() >= minY && pt.getY() <= maxY)
						{
							consumer.accept(spawn, pt);
						}
					}
				});
		}
		for (DungeonPiece piece : pieces)
		{
			if (!camera.isDungeonPieceVisible(zone, piece)) continue;
			for (int[] rect : piece.rects)
			{
				groundItemIndex.forEachInArea(piece.plane, rect[0], rect[2], rect[1], rect[3], spawn ->
				{
					if (spawn.getValue() < minValue) return;
					final Point2D pt = DungeonPieceTransform.toDisplay(piece,
						spawn.getX() + 0.5, spawn.getY() + 0.5);
					final String key = spawn.getX() + "," + spawn.getY() + "," + spawn.getPlane() + ":" + pt;
					if (pt.getX() >= minX && pt.getX() <= maxX
						&& pt.getY() >= minY && pt.getY() <= maxY && seen.add(key))
					{
						consumer.accept(spawn, pt);
					}
				});
			}
		}
	}

	private void forEachSpawnInView(int plane, int minX, int maxX, int minY, int maxY,
		int minValue, BiConsumer<GroundItemIndex.Spawn, Point2D> consumer)
	{
		final List<UndergroundZone> previewZones = camera.previewUndergroundZones();
		for (UndergroundZone zone : previewZones)
		{
			forEachZoneSpawnInView(zone, minX, maxX, minY, maxY, minValue, consumer);
		}
		if (camera.isUndergroundModeActive())
		{
			return;
		}
		groundItemIndex.forEachInArea(plane, minX, maxX, minY, maxY, spawn ->
		{
			if (spawn.getValue() >= minValue && ViewWindow.isDrawable(camera, spawn.getX(), spawn.getY()))
			{
				consumer.accept(spawn, null);
			}
		});
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

		forEachSpawnInView(plane, view.minXi, view.maxXi, view.minYi, view.maxYi, minValue, (spawn, displayPoint) ->
		{
			final int sx = displayPoint == null
				? (int) Math.round(camera.screenX(spawn.getX() + 0.5, spawn.getY() + 0.5, bounds))
				: (int) Math.round(camera.screenX(displayPoint.getX(), bounds));
			final int sy = displayPoint == null
				? (int) Math.round(camera.screenY(spawn.getX() + 0.5, spawn.getY() + 0.5, bounds))
				: (int) Math.round(camera.screenY(displayPoint.getY(), bounds));

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

			if (drawLabels)
			{
				final String label = primary.getQuantity() > 1
					? primary.getName() + " ×" + primary.getQuantity()
					: primary.getName();
				MarkerLabelRenderer.draw(graphics, bounds, label, sx, rect.y + chipSize + 11,
					3, 9, 12, GROUND_ITEM_EDGE, CARD_TEXT, MARKER_OUTLINE);
			}
		});

		graphics.setStroke(prevStroke);
	}

}
