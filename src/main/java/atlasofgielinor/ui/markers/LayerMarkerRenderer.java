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

import static atlasofgielinor.ui.MapStyle.TINY;

import static atlasofgielinor.ui.MapStyle.CARD_EDGE;
import static atlasofgielinor.ui.MapStyle.CARD_TEXT;
import static atlasofgielinor.ui.MapStyle.CARD_TITLE;
import static atlasofgielinor.ui.MapStyle.SMALL;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import atlasofgielinor.AtlasOfGielinorConfig;
import atlasofgielinor.data.dungeons.DungeonFloor;
import atlasofgielinor.data.dungeons.UndergroundZone;
import atlasofgielinor.map.MapCamera;
import atlasofgielinor.ui.MapPins;
import atlasofgielinor.map.catalog.PoiIndex;
import net.runelite.api.coords.WorldPoint;

/** Draws dungeon entrance toggles and floor controls. */
@lombok.extern.slf4j.Slf4j
public class LayerMarkerRenderer
{
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
	private final AtlasOfGielinorConfig config;
	private final MapCamera camera;
	private final PoiIndex poiIndex;
	private BufferedImage dungeonExclamation;
	private BufferedImage dungeonExclamationGreen;
	private java.util.Map<UndergroundZone, Rectangle> lastLoggedBars = Collections.emptyMap();
	private long lastBarLogNanos;

	public LayerMarkerRenderer(AtlasOfGielinorConfig config, MapCamera camera, PoiIndex poiIndex)
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
		camera.setFloorBarBounds(Collections.emptyMap());
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
			targets.add(new MapCamera.LayerSymbolTarget(rect, zone,
				onSurface ? anchors.get(i) : anchor, isSurfaceToUnderground));

			final boolean isHovered = camera.getHoveredUndergroundZone() == zone
				&& camera.isHoveredSurfaceToUnderground() == isSurfaceToUnderground
				&& (camera.getHoveredFloorPlane() == null || camera.isHoveredFloor(
					DungeonFloor.forEntrance(zone, onSurface ? anchors.get(i) : anchor)));

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
				MapPins.drawBadge(graphics, rect, radius, isHovered ? LAYER_CHIP_BG_HOVER : LAYER_CHIP_BG,
					isHovered ? CARD_TITLE : LAYER_CHIP_EDGE, isHovered ? SYMBOL_BORDER_HOVER : SYMBOL_BORDER);
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
				final DungeonFloor entranceFloor = DungeonFloor.forEntrance(zone, onSurface ? anchors.get(i) : anchor);
				final String label = zone == UndergroundZone.SLAYER_TOWER ? "Slayer Tower — floors"
					: entranceFloor == null ? zone.getName() : entranceFloor.name;
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
			drawFloorOverlays(graphics, bounds, targets, onSurface, focused);
		}


		graphics.setStroke(oldStroke);
		camera.setLayerSymbolTargets(targets);
	}

	private void drawFloorOverlays(Graphics2D graphics, Rectangle bounds,
		List<MapCamera.LayerSymbolTarget> targets, boolean onSurface, UndergroundZone focused)
	{
		final java.util.Map<UndergroundZone, Rectangle> bars = new java.util.LinkedHashMap<>();
		final List<Rectangle> occupied = new ArrayList<>();
		Collections.addAll(occupied, camera.getNativePassthrough());
		Collections.addAll(occupied, camera.getPlaneButtons());
		Collections.addAll(occupied, camera.getLayersButton(), camera.getFinderButton(), camera.getChatButton(),
			camera.getUndergroundReturnButton(), camera.getClueButton(), camera.getQuestButton(),
			camera.getPlayerButton(), camera.getDestinationButton(), camera.getBoatsButton(),
			camera.getFinderPanelBounds(), camera.getFinderFlyoutBounds(), camera.getLayersPanelBounds(),
			camera.getCluePanelBounds(), camera.getBoatsDropdownBounds());
		occupied.removeIf(java.util.Objects::isNull);
		final List<UndergroundZone> previewZones = camera.previewUndergroundZones();
		final double scale = camera.floorBarScale();
		final int rowHeight = camera.floorBarRowHeight();
		final int padding = camera.floorBarPadding();
		final int width = (int) Math.round(30 * scale);
		final int diamond = (int) Math.round(10 * scale);
		final int gap = (int) Math.round(8 * scale);
		final java.awt.Font font = TINY.deriveFont((float) (TINY.getSize2D() * scale));
		final Stroke border = new BasicStroke((float) (1.1 * scale));
		final Stroke emphasis = new BasicStroke((float) (1.6 * scale));
		final int rowsAvailable = Math.min(7, (bounds.height - 4 * padding) / rowHeight);
		if (rowsAvailable < 1 || bounds.width < width + 2 * padding) return;
		final java.util.Map<UndergroundZone, Rectangle> entrances = new java.util.LinkedHashMap<>();
		for (UndergroundZone zone : UndergroundZone.ALL_ZONES)
		{
			final List<DungeonFloor> floors = DungeonFloor.forZone(zone);
			if (floors.size() < 2) continue;
			if (onSurface)
			{
				if (!camera.isUndergroundZoneOpen(zone)
					&& !previewZones.contains(zone)) continue;
			}
			else if (zone != focused && (focused == null || !focused.getConnectedZones().contains(zone))) continue;

			Rectangle entrance = null;
			for (MapCamera.LayerSymbolTarget target : targets)
			{
				if (target.isFloor() || target.getZone() != zone) continue;
				final Rectangle rect = target.getBounds();
				if (entrance == null || rect.getLocation().distanceSq(bounds.getCenterX(), bounds.getCenterY())
					< entrance.getLocation().distanceSq(bounds.getCenterX(), bounds.getCenterY())) entrance = rect;
			}
			if (entrance == null)
			{
				final WorldPoint point = zone.getSurfacePoint();
				entrance = dungeonIconRect(point.getX(), point.getY(), bounds,
					Math.max(8, Math.min(32, config.undergroundSymbolSize())));
			}
			if (bounds.intersects(entrance)) entrances.put(zone, entrance);
		}
		final List<UndergroundZone> orderedZones = new ArrayList<>(entrances.keySet());
		if (orderedZones.remove(focused)) orderedZones.add(0, focused);
		for (UndergroundZone zone : orderedZones)
		{
			final List<DungeonFloor> floors = DungeonFloor.forZone(zone);
			final int rows = Math.min(floors.size(), rowsAvailable);
			final Rectangle bar = placeFloorBar(bounds, entrances.get(zone), width,
				rows * rowHeight + 2 * padding, gap, occupied);
			if (bar == null) continue;
			bars.put(zone, bar);
			occupied.add(bar);
			MapPins.drawBadge(graphics, bar, diamond, LAYER_LABEL_BG, FLOOR_CHIP_EDGE, border);
			final int offset = camera.floorBarOffset(zone, rows);
			final DungeonFloor selected = camera.selectedFloorFor(zone);
			for (int row = 0; row < rows; row++)
			{
				final DungeonFloor floor = floors.get(offset + row);
				final int cx = bar.x + bar.width / 2;
				final int cy = bar.y + padding + row * rowHeight + rowHeight / 2;
				final Rectangle hit = new Rectangle(bar.x + 1, bar.y + padding + row * rowHeight, bar.width - 2, rowHeight);
				targets.add(0, new MapCamera.LayerSymbolTarget(hit, floor));
				final boolean hovered = camera.isHoveredFloor(floor);
				final boolean active = selected != null && selected.plane == floor.plane
					&& java.util.Objects.equals(selected.layerId, floor.layerId);
				final int[] x = {cx, cx + diamond, cx, cx - diamond};
				final int[] y = {cy - diamond, cy, cy + diamond, cy};
				graphics.setColor(active ? FLOOR_CHIP_EDGE : hovered ? FLOOR_CHIP_BG_HOVER : FLOOR_CHIP_BG);
				graphics.fillPolygon(x, y, 4);
				graphics.setColor(hovered ? FLOOR_CHIP_EDGE_HOVER : FLOOR_CHIP_EDGE);
				graphics.setStroke(active || hovered ? emphasis : border);
				graphics.drawPolygon(x, y, 4);
				graphics.setFont(font);
				final String label = floor.glyph.isEmpty() ? String.valueOf(offset + row + 1) : floor.glyph;
				graphics.setColor(active ? FLOOR_CHIP_BG : FLOOR_CHIP_TEXT);
				graphics.drawString(label, cx - graphics.getFontMetrics().stringWidth(label) / 2, cy + (int) Math.round(4 * scale));
			}
			graphics.setColor(FLOOR_CHIP_EDGE_HOVER);
			final int cx = bar.x + width / 2;
			final int arrow = Math.max(2, padding - 1);
			if (offset > 0) graphics.fillPolygon(new int[]{cx - arrow, cx + arrow, cx},
				new int[]{bar.y + padding, bar.y + padding, bar.y + 1}, 3);
			if (offset + rows < floors.size()) graphics.fillPolygon(new int[]{cx - arrow, cx + arrow, cx},
				new int[]{bar.y + bar.height - padding, bar.y + bar.height - padding, bar.y + bar.height - 1}, 3);
		}
		camera.setFloorBarBounds(Collections.unmodifiableMap(bars));
		if (config.debugLogging() && log.isDebugEnabled() && !bars.equals(lastLoggedBars)
			&& System.nanoTime() - lastBarLogNanos > 1_000_000_000L)
		{
			lastBarLogNanos = System.nanoTime();
			lastLoggedBars = bars;
			for (java.util.Map.Entry<UndergroundZone, Rectangle> entry : bars.entrySet())
			{
				log.debug("[AtlasOfGielinor:floor-bar] zone={} bounds={} total={} zoom={} entrance={} gap={} side=left",
					entry.getKey().getId(), entry.getValue(), DungeonFloor.forZone(entry.getKey()).size(),
					camera.getFrameZoom(), entrances.get(entry.getKey()),
					entrances.get(entry.getKey()).x - (entry.getValue().x + entry.getValue().width));
			}
			for (MapCamera.LayerSymbolTarget target : targets)
			{
				if (target.isFloor()) log.debug("[AtlasOfGielinor:floor-target] zone={} name={} bounds={} selected={}",
					target.getZone().getId(), target.getFloor().name, target.getBounds(),
					camera.selectedFloorFor(target.getZone()) == target.getFloor());
			}
		}
	}

	/** Keep the bar immediately left of its entrance; never relocate it to another side. */
	private static Rectangle placeFloorBar(Rectangle view, Rectangle entrance, int width, int height,
		int gap, List<Rectangle> occupied)
	{
		final int y = Math.max(view.y, Math.min((int) entrance.getCenterY() - height / 2,
			view.y + view.height - height));
		final Rectangle bar = new Rectangle(entrance.x - gap - width, y, width, height);
		return view.contains(bar) && !ViewWindow.overlapsPlaced(occupied, bar) ? bar : null;
	}
}
