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
package bettermap.ui.markers;

import static bettermap.ui.MapStyle.CARD_EDGE;
import static bettermap.ui.MapStyle.CARD_TEXT;
import static bettermap.ui.MapStyle.CARD_TITLE;
import static bettermap.ui.MapStyle.SMALL;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import bettermap.BetterMapConfig;
import bettermap.data.OverlayFloor;
import bettermap.data.UndergroundZone;
import bettermap.map.MapCamera;
import bettermap.map.PoiIndex;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.FontManager;

/** Draws dungeon entrance toggles and floor controls. */
public class LayerMarkerRenderer
{
	private static final Font TINY = FontManager.getDefaultBoldFont().deriveFont(9f);
	/** Fixed colours that were being reallocated inside per-frame draw loops. */
	private static final Color LAYER_CHIP_BG_HOVER = new Color(28, 34, 48, 235);
	private static final Color LAYER_CHIP_BG = new Color(16, 20, 28, 195);
	private static final Color LAYER_CHIP_EDGE = new Color(120, 180, 240, 220);
	private static final Color LAYER_CHIP_TEXT = new Color(140, 200, 255);
	private static final Color FLOOR_CHIP_BG = new Color(36, 28, 16, 200);
	private static final Color FLOOR_CHIP_BG_HOVER = new Color(56, 42, 18, 235);
	private static final Color FLOOR_CHIP_EDGE = new Color(230, 180, 80, 220);
	private static final Color FLOOR_CHIP_EDGE_HOVER = new Color(255, 220, 140, 240);
	private static final Color FLOOR_CHIP_TEXT = new Color(255, 220, 150);
	private static final Color LAYER_LABEL_BG = new Color(12, 14, 20, 215);
	private static final Stroke SYMBOL_BORDER = new BasicStroke(1.1f);
	private static final Stroke SYMBOL_BORDER_HOVER = new BasicStroke(1.6f);
	private final BetterMapConfig config;
	private final MapCamera camera;
	private final PoiIndex poiIndex;
	private BufferedImage dungeonExclamation;
	private BufferedImage dungeonExclamationGreen;

	public LayerMarkerRenderer(BetterMapConfig config, MapCamera camera, PoiIndex poiIndex)
	{
		this.config = config;
		this.camera = camera;
		this.poiIndex = poiIndex;
	}

	/** Scales a glyph offset that was hand-drawn against a 32px box, so {@code size} stays the one knob. */
	private static int glyph(int size, int offsetAt32)
	{
		return Math.round(size * offsetAt32 / 32f);
	}

	private BufferedImage dungeonExclamation(boolean green)
	{
		if (dungeonExclamation == null && poiIndex != null)
		{
			dungeonExclamation = poiIndex.icon("dungeon");
		}
		if (!green)
		{
			return dungeonExclamation;
		}
		if (dungeonExclamationGreen == null && dungeonExclamation != null)
		{
			dungeonExclamationGreen = tintDungeonExclamationGreen(dungeonExclamation);
		}
		return dungeonExclamationGreen;
	}

	private static BufferedImage tintDungeonExclamationGreen(BufferedImage src)
	{
		final BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < src.getHeight(); y++)
		{
			for (int x = 0; x < src.getWidth(); x++)
			{
				final int argb = src.getRGB(x, y);
				final int a = (argb >>> 24) & 0xff;
				final int r = (argb >>> 16) & 0xff;
				final int g = (argb >>> 8) & 0xff;
				final int b = argb & 0xff;
				if (r > g + 20 && r > b + 20)
				{
					out.setRGB(x, y, (a << 24) | (g << 16) | (r << 8) | b);
				}
				else
				{
					out.setRGB(x, y, argb);
				}
			}
		}
		return out;
	}

	private Rectangle dungeonIconRect(int worldX, int worldY, Rectangle bounds, int fallbackSize)
	{
		final int sx = (int) Math.round(camera.screenX(worldX + 0.5, worldY + 0.5, bounds));
		final int sy = (int) Math.round(camera.screenY(worldX + 0.5, worldY + 0.5, bounds));
		return new Rectangle(sx - fallbackSize / 2, sy - fallbackSize / 2, fallbackSize, fallbackSize);
	}

	/**
	 * Overworld: each dungeon entrance previews and toggles its connected zones.
	 * Open dungeon symbols turn green. Underground: return-to-surface chips.
	 */
	private boolean isSubterraneanVisible(UndergroundZone zone)
	{
		if (!zone.isSubterranean())
		{
			return true;
		}
		if (camera.isUndergroundModeActive())
		{
			return true;
		}
		final UndergroundZone parent = zone.getParentZone();
		if (parent != null)
		{
			if (camera.isUndergroundZoneOpen(parent)
				|| camera.getHoveredUndergroundZone() == parent
				|| camera.getActiveUndergroundZone() == parent)
			{
				return true;
			}
		}
		if (camera.isUndergroundZoneOpen(zone)
			|| camera.getHoveredUndergroundZone() == zone
			|| camera.getActiveUndergroundZone() == zone)
			{
				return true;
			}
		final List<UndergroundZone> preview = camera.previewUndergroundZones();
		return preview.contains(zone) || (parent != null && preview.contains(parent));
	}

	public void drawLargeLayerSymbols(Graphics2D graphics, Rectangle bounds)
	{
		if (!config.showLargeUndergroundSymbols() || camera.getZoom() < config.dungeonNavigationMinZoom())
		{
			camera.setLayerSymbolTargets(Collections.emptyList());
			camera.setHoveredUnderground(null, true);
			return;
		}

		final boolean onSurface = camera.getCenterY() < 4200;
		final List<MapCamera.LayerSymbolTarget> targets = new ArrayList<>();
		final int size = Math.max(8, Math.min(32, config.undergroundSymbolSize()));
		final int radius = glyph(size, 8);
		final UndergroundZone focused = camera.getActiveUndergroundZone();

		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		final Stroke oldStroke = graphics.getStroke();
		for (UndergroundZone zone : UndergroundZone.ALL_ZONES)
		{
			if (zone.getId().contains("__") || !zone.hasEntranceToggle()) continue;
			if (!onSurface && focused != null && !focused.getConnectedZones().contains(zone) && zone.getParentZone() != focused)
			{
				continue;
			}
			if (onSurface && !isSubterraneanVisible(zone))
			{
				continue;
			}

			final List<WorldPoint> anchors = onSurface
				? zone.getSurfacePoints()
				: (zone.isSubterranean() ? Collections.singletonList(zone.getUndergroundPoint()) : zone.getSurfacePoints());
			for (int i = 0; i < anchors.size(); i++)
			{
				if (onSurface && zone.isEntranceMarkerHidden(i))
				{
					continue;
				}
				final WorldPoint anchor = onSurface
					? zone.getEntranceMarkerPoint(i)
					: anchors.get(i);
				if (anchor == null)
				{
					continue;
				}
				final int sx = (int) Math.round(camera.screenX(anchor.getX() + 0.5, anchor.getY() + 0.5, bounds));
				final int sy = (int) Math.round(camera.screenY(anchor.getX() + 0.5, anchor.getY() + 0.5, bounds));

				final boolean showDungeonIcon = onSurface || zone.getParentZone() == focused;
				final Rectangle rect = showDungeonIcon
					? dungeonIconRect(anchor.getX(), anchor.getY(), bounds, size)
					: new Rectangle(sx - size / 2, sy - size / 2, size, size);
				if (!bounds.intersects(rect))
				{
					continue;
				}

				final boolean isSurfaceToUnderground = showDungeonIcon;
			targets.add(new MapCamera.LayerSymbolTarget(rect, zone, anchor, isSurfaceToUnderground));

			final boolean isHovered = camera.getHoveredUndergroundZone() == zone
				&& camera.isHoveredSurfaceToUnderground() == isSurfaceToUnderground
				&& camera.getHoveredFloorPlane() == null;

			if (showDungeonIcon)
			{
				final BufferedImage icon = dungeonExclamation(camera.isUndergroundZoneOpen(zone));
				if (isHovered)
				{
					graphics.setColor(CARD_TITLE);
					graphics.setStroke(SYMBOL_BORDER_HOVER);
					graphics.drawOval(rect.x - 2, rect.y - 2, rect.width + 4, rect.height + 4);
				}
				if (icon != null)
				{
					graphics.drawImage(icon, rect.x, rect.y, rect.width, rect.height, null);
				}
				else
				{
					graphics.setColor(isHovered ? CARD_TITLE : CARD_EDGE);
					graphics.setStroke(SYMBOL_BORDER);
					graphics.drawOval(rect.x, rect.y, rect.width - 1, rect.height - 1);
					graphics.setFont(SMALL);
					final int bangW = graphics.getFontMetrics().stringWidth("!");
					graphics.drawString("!", sx - bangW / 2, sy + 4);
				}
			}
			else
			{
				graphics.setColor(isHovered ? LAYER_CHIP_BG_HOVER : LAYER_CHIP_BG);
				graphics.fillRoundRect(rect.x, rect.y, rect.width, rect.height, radius, radius);
				graphics.setColor(isHovered ? CARD_TITLE : LAYER_CHIP_EDGE);
				graphics.setStroke(isHovered ? SYMBOL_BORDER_HOVER : SYMBOL_BORDER);
				graphics.drawRoundRect(rect.x, rect.y, rect.width, rect.height, radius, radius);
				graphics.setColor(isHovered ? CARD_TITLE : LAYER_CHIP_TEXT);
				final int[] ax = {rect.x + glyph(size, 10), rect.x + glyph(size, 22), rect.x + glyph(size, 16)};
				final int[] ay = {rect.y + glyph(size, 18), rect.y + glyph(size, 18), rect.y + glyph(size, 8)};
				graphics.fillPolygon(ax, ay, 3);
				graphics.drawLine(rect.x + glyph(size, 7), rect.y + glyph(size, 22),
					rect.x + glyph(size, 25), rect.y + glyph(size, 22));
			}

			// Overworld: dungeon names are hover-only. Inside the dungeon they follow zoom.
			if (isHovered || (!onSurface && camera.getZoom() >= config.dungeonNavigationLabelMinZoom()))
			{
				graphics.setFont(SMALL);
				final String label = zone.getName();
				final int textWidth = graphics.getFontMetrics().stringWidth(label);
				final int textX = sx - textWidth / 2;
				final int textY = rect.y + rect.height + 13;

				graphics.setColor(LAYER_LABEL_BG);
				graphics.fillRoundRect(textX - 4, textY - 11, textWidth + 8, 14, 5, 5);
				graphics.setColor(CARD_EDGE);
				graphics.drawRoundRect(textX - 4, textY - 11, textWidth + 8, 14, 5, 5);
				graphics.setColor(isHovered ? CARD_TITLE : CARD_TEXT);
				graphics.drawString(label, textX, textY);
			}
			}
		}

		if (onSurface || focused != null)
		{
			drawFloorOverlays(graphics, bounds, targets, size, onSurface, focused);
		}


		graphics.setStroke(oldStroke);
		camera.setLayerSymbolTargets(targets);
	}

	private void drawFloorOverlays(Graphics2D graphics, Rectangle bounds,
		List<MapCamera.LayerSymbolTarget> targets, int size, boolean onSurface, UndergroundZone focused)
	{
		final Integer hoveredFloor = camera.getHoveredFloorPlane();
		final int chip = Math.max(8, size - 4);
		for (OverlayFloor floor : OverlayFloor.all())
		{
			if (!onSurface && floor.zone != focused
				&& (focused == null || !focused.getConnectedZones().contains(floor.zone)))
			{
				continue;
			}
			if (onSurface)
			{
				final boolean matchesHoveredZone = camera.getHoveredUndergroundZone() != null && camera.isHoveredSurfaceToUnderground()
					&& camera.previewUndergroundZones().contains(floor.zone);
				final boolean matchesOpenZone = camera.isUndergroundZoneOpen(floor.zone);
				if (!matchesHoveredZone && !matchesOpenZone)
				{
					continue;
				}
			}

			final int sx = (int) Math.round(camera.screenX(floor.worldX + 0.5, floor.worldY + 0.5, bounds));
			final int sy = (int) Math.round(camera.screenY(floor.worldX + 0.5, floor.worldY + 0.5, bounds));
			final Rectangle rect = new Rectangle(sx - chip / 2, sy - chip / 2, chip, chip);
			if (!bounds.intersects(rect))
			{
				continue;
			}

			targets.add(new MapCamera.LayerSymbolTarget(rect, floor));

			final boolean isHovered = hoveredFloor != null && camera.isHoveredFloor(floor);

			final int[] dx = {sx, sx + chip / 2, sx, sx - chip / 2};
			final int[] dy = {sy - chip / 2, sy, sy + chip / 2, sy};
			graphics.setColor(isHovered ? FLOOR_CHIP_BG_HOVER : FLOOR_CHIP_BG);
			graphics.fillPolygon(dx, dy, 4);
			graphics.setColor(isHovered ? FLOOR_CHIP_EDGE_HOVER : FLOOR_CHIP_EDGE);
			graphics.setStroke(isHovered ? SYMBOL_BORDER_HOVER : SYMBOL_BORDER);
			graphics.drawPolygon(dx, dy, 4);

			graphics.setFont(TINY);
			final String glyph = floorGlyph(floor);
			final int textWidth = graphics.getFontMetrics().stringWidth(glyph);
			graphics.setColor(isHovered ? CARD_TITLE : FLOOR_CHIP_TEXT);
			graphics.drawString(glyph, sx - textWidth / 2, sy + 3);

			if (isHovered)
			{
				graphics.setFont(SMALL);
				final String label = floor.name;
				final int labelWidth = graphics.getFontMetrics().stringWidth(label);
				final int textX = sx - labelWidth / 2;
				final int textY = rect.y + chip + 13;
				graphics.setColor(LAYER_LABEL_BG);
				graphics.fillRoundRect(textX - 4, textY - 11, labelWidth + 8, 14, 5, 5);
				graphics.setColor(CARD_TITLE);
				graphics.drawString(label, textX, textY);
			}
		}
	}

	private static final java.util.regex.Pattern EXPLICIT_FLOOR_NUM = java.util.regex.Pattern.compile("\\(([0-9]+)(?:\\s*[-–].*)?\\)");

	private static String floorGlyph(OverlayFloor selected)
	{
		final java.util.regex.Matcher m = EXPLICIT_FLOOR_NUM.matcher(selected.name);
		if (m.find())
		{
			return m.group(1);
		}
		int count = 0;
		int ordinal = 0;
		for (OverlayFloor floor : OverlayFloor.all())
		{
			if (floor.zone == selected.zone && floor.plane == selected.plane)
			{
				count++;
				if (floor == selected)
				{
					ordinal = count;
				}
			}
		}
		return count > 1 ? String.valueOf(ordinal)
			: selected.plane == 0 ? "G" : String.valueOf(selected.plane);
	}

}
