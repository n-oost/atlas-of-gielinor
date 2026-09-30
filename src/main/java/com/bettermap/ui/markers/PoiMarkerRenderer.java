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

import com.bettermap.BetterMapConfig;
import com.bettermap.data.DungeonPiece;
import com.bettermap.data.OverlayFloor;
import com.bettermap.data.UndergroundZone;
import com.bettermap.map.DungeonPieceIndex;
import com.bettermap.map.InstanceMaps;
import com.bettermap.map.MapCamera;
import com.bettermap.map.MapRegion;
import com.bettermap.map.PoiCategory;
import com.bettermap.map.PoiIndex;
import com.bettermap.ui.MapRenderStats;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.FontManager;

/**
 * Renders point-of-interest (POI) badges (banks, shops, altars, shortcuts, etc.)
 * and regional place name labels.
 */
public class PoiMarkerRenderer
{
	/** Below this many pixels per game tile, icons are more clutter than information. */
	public static final double ICON_ZOOM_THRESHOLD = 0.45;
	/** Cities and kingdoms stay visible while zoomed out. */
	public static final double PLACE_MAJOR_ZOOM = ICON_ZOOM_THRESHOLD;
	/** Districts and landmarks need a closer look, similar to monster spawn areas. */
	public static final double PLACE_LOCAL_ZOOM = 3.0;
	/** Dungeon room names once the interior is actually on screen. */
	public static final double PLACE_INTERIOR_ZOOM = 4.0;

	private static final Color PLACE_FILL = new Color(248, 236, 196);
	private static final Color PLACE_OUTLINE = new Color(16, 12, 8, 210);

	private static final Set<String> PLACE_NAME_SKIP = new HashSet<>(Arrays.asList(
		"a", "b", "c", "d", "e", "f", "n", "s", "w",
		"anger", "nothing interesting happens here", "upper level", "lower level",
		"middle level", "exit"));

	private static final Set<String> MAJOR_PLACE_NAMES = majorPlaceNames();

	/**
	 * Keys that follow the Skilling toggle regardless of their {@code PoiCategory} —
	 * {@code agility_short-cut} is categorised TRAVEL but players expect it under Skilling.
	 */
	private static final Set<String> DEFAULT_SKILLING_POI_KEYS = new HashSet<>(Arrays.asList(
		"agility_training",
		"agility_short-cut",
		"hunter_training"
	));

	private final BetterMapConfig config;
	private final MapCamera camera;
	private final PoiIndex poiIndex;
	private final DungeonPieceIndex dungeonPieceIndex;
	private final MapRenderStats stats;

	public PoiMarkerRenderer(
		BetterMapConfig config,
		MapCamera camera,
		PoiIndex poiIndex,
		DungeonPieceIndex dungeonPieceIndex,
		MapRenderStats stats)
	{
		this.config = config;
		this.camera = camera;
		this.poiIndex = poiIndex;
		this.dungeonPieceIndex = dungeonPieceIndex;
		this.stats = stats;
	}

	private void forEachPoiInView(int plane, int minX, int maxX, int minY, int maxY,
		Consumer<PoiIndex.Poi> consumer)
	{
		forEachPoiInView(plane, minX, maxX, minY, maxY, true, consumer);
	}

	private void forEachPoiInView(int plane, int minX, int maxX, int minY, int maxY,
		boolean projectDungeons, Consumer<PoiIndex.Poi> consumer)
	{
		InstanceMaps.forEachQueryArea(minX, maxX, minY, maxY, camera.getCenterX(), camera.getCenterY(),
			projectDungeons,
			(qMinX, qMaxX, qMinY, qMaxY) -> poiIndex.forEachInArea(plane, qMinX, qMaxX, qMinY, qMaxY, poi ->
			{
				if (ViewWindow.isDrawable(camera, poi.getX(), poi.getY()))
				{
					consumer.accept(poi);
				}
			}));
	}

	/**
	 * Banks, shops, altars, dungeons and the rest, from the prefetched wiki icon set.
	 *
	 * <p>Icons are placed greedily and anything that would overlap one already placed is
	 * dropped. Without that, a town at moderate zoom is an unreadable pile of overlapping
	 * badges; with it the map thins out as you zoom out and fills in as you zoom in.
	 */
	public void drawPoiIcons(Graphics2D graphics, Rectangle bounds, List<Rectangle> placed)
	{
		if (!anyPoiCategoryEnabled() || camera.getZoom() < ICON_ZOOM_THRESHOLD)
		{
			return;
		}

		final int plane = camera.getPlane();
		final ViewWindow view = ViewWindow.from(camera, bounds, 20.0);

		forEachPoiInView(plane, view.minXi, view.maxXi, view.minYi, view.maxYi, poi ->
		{
			if (!shouldDrawPoiIcon(poi))
			{
				if (stats != null)
				{
					stats.iconsFiltered++;
				}
				return;
			}

			final BufferedImage icon = poiIndex.icon(poi.getKey());
			if (icon == null)
			{
				return;
			}

			final Rectangle rect = new Rectangle(
				(int) Math.round(camera.screenX(poi.getX() + 0.5, poi.getY() + 0.5, bounds)) - icon.getWidth() / 2,
				(int) Math.round(camera.screenY(poi.getX() + 0.5, poi.getY() + 0.5, bounds)) - icon.getHeight() / 2,
				icon.getWidth(),
				icon.getHeight());

			if (!bounds.intersects(rect))
			{
				return;
			}

			if (ViewWindow.overlapsPlaced(placed, rect))
			{
				if (stats != null)
				{
					stats.iconsSkipped++;
				}
				return;
			}

			graphics.drawImage(icon, rect.x, rect.y, null);
			placed.add(rect);
			if (stats != null)
			{
				stats.iconsDrawn++;
			}
		});
	}

	/**
	 * Cache region labels ("Lumbridge", "Araxxor") drawn as outlined map text. These are not
	 * icon badges — {@link #shouldDrawPoiIcon} still returns false for {@code region_label}.
	 */
	public void drawPlaceNames(Graphics2D graphics, Rectangle bounds)
	{
		final double zoom = camera.getZoom();
		if (!config.showPlaceNames() || zoom < PLACE_MAJOR_ZOOM)
		{
			return;
		}

		final boolean dungeonFocused = camera.isDungeonContentsFocused();
		final boolean cameraOnOverworld = InstanceMaps.cameraOnOverworld(camera.getCenterY());
		final int plane = camera.getPlane();
		final ViewWindow view = ViewWindow.from(camera, bounds, 40.0);
		final Font boldFont = FontManager.getRunescapeBoldFont();
		final Font smallFont = FontManager.getRunescapeSmallFont();
		final FontMetrics fmBold = graphics.getFontMetrics(boldFont);
		final FontMetrics fmSmall = graphics.getFontMetrics(smallFont);
		graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		// Same as monster spawns: do not stamp cave labels onto the overworld. They appear
		// when the dungeon is open or the entrance is hovered (contents-focused).
		forEachPoiInView(plane, view.minXi, view.maxXi, view.minYi, view.maxYi, dungeonFocused, poi ->
		{
			if (!"region_label".equals(poi.getKey()))
			{
				return;
			}
			final String name = poi.getName();
			if (!shouldDrawPlaceName(name, poi.getY(), zoom, dungeonFocused, cameraOnOverworld))
			{
				return;
			}

			final boolean major = isMajorPlaceName(name);
			final FontMetrics fm = major ? fmBold : fmSmall;
			graphics.setFont(major ? boldFont : smallFont);

			final int sx = (int) Math.round(camera.screenX(poi.getX() + 0.5, poi.getY() + 0.5, bounds));
			final int sy = (int) Math.round(camera.screenY(poi.getX() + 0.5, poi.getY() + 0.5, bounds));
			final int textW = fm.stringWidth(name);
			final int textX = sx - textW / 2;
			final int textY = sy + fm.getAscent() / 2 - 1;
			if (textX + textW < bounds.x || textX > bounds.x + bounds.width
				|| textY < bounds.y || textY - fm.getHeight() > bounds.y + bounds.height)
			{
				return;
			}

			graphics.setColor(PLACE_OUTLINE);
			for (int ox = -1; ox <= 1; ox++)
			{
				for (int oy = -1; oy <= 1; oy++)
				{
					if (ox == 0 && oy == 0)
					{
						continue;
					}
					graphics.drawString(name, textX + ox, textY + oy);
				}
			}
			graphics.setColor(PLACE_FILL);
			graphics.drawString(name, textX, textY);
		});
	}

	private static boolean isDrawablePlaceName(String name)
	{
		if (name == null)
		{
			return false;
		}
		final String trimmed = name.trim();
		if (trimmed.length() <= 1)
		{
			return false;
		}
		return !PLACE_NAME_SKIP.contains(trimmed.toLowerCase(Locale.ROOT));
	}

	public static boolean isInteriorPlaceLabel(int worldY)
	{
		return worldY > InstanceMaps.GAP_MIN_Y;
	}

	public static boolean shouldDrawPlaceName(String name, int worldY, double zoom, boolean dungeonFocused,
		boolean cameraOnOverworld)
	{
		if (!isDrawablePlaceName(name))
		{
			return false;
		}
		if (isInteriorPlaceLabel(worldY) && cameraOnOverworld && !dungeonFocused)
		{
			return false;
		}
		return zoom >= minZoomForPlace(name, worldY);
	}

	public static double minZoomForPlace(String name, int worldY)
	{
		if (isInteriorPlaceLabel(worldY))
		{
			return PLACE_INTERIOR_ZOOM;
		}
		return isMajorPlaceName(name) ? PLACE_MAJOR_ZOOM : PLACE_LOCAL_ZOOM;
	}

	private static boolean isMajorPlaceName(String name)
	{
		final String key = name.trim().toLowerCase(Locale.ROOT);
		return MAJOR_PLACE_NAMES.contains(key) || key.startsWith("kingdom of ");
	}

	private static Set<String> majorPlaceNames()
	{
		final Set<String> names = new HashSet<>();
		for (MapRegion region : MapRegion.values())
		{
			names.add(region.getDisplayName().toLowerCase(Locale.ROOT));
			names.add(region.getKingdom().getDisplayName().toLowerCase(Locale.ROOT));
		}
		return names;
	}

	public boolean shouldDrawPoiIcon(PoiIndex.Poi poi)
	{
		if (poi == null || poi.getKey() == null)
		{
			return false;
		}
		if (poi.getY() <= InstanceMaps.GAP_MIN_Y && coveredByDungeonPreview(poi.getX(), poi.getY()))
		{
			return false;
		}

		if (config.showLargeUndergroundSymbols() && isDungeonPoiKey(poi.getKey())
			&& UndergroundZone.forSurfacePoint(poi.getX(), poi.getY(), 8) != null)
		{
			return false;
		}

		if (DEFAULT_SKILLING_POI_KEYS.contains(poi.getKey()))
		{
			return config.iconSkilling();
		}

		return categoryEnabled(PoiCategory.of(poi.getKey()));
	}

	/** Surface POIs under projected dungeon geometry make regional previews unreadable. */
	private boolean coveredByDungeonPreview(int worldX, int worldY)
	{
		if (camera == null || dungeonPieceIndex == null)
		{
			return false;
		}
		for (UndergroundZone zone : camera.previewUndergroundZones())
		{
			final List<DungeonPiece> pieces = dungeonPieceIndex.piecesFor(zone.getId());
			if (pieces.isEmpty())
			{
				for (WorldPoint entrance : zone.getSurfacePoints())
				{
					final long dx = (long) worldX - entrance.getX();
					final long dy = (long) worldY - entrance.getY();
					if (dx * dx + dy * dy <= (long) zone.getRadius() * zone.getRadius())
					{
						return true;
					}
				}
				continue;
			}

			final Integer selectedLayer = camera.floorLayerFor(zone);
			final Integer layer = selectedLayer != null ? selectedLayer : OverlayFloor.defaultLayerFor(zone);
			for (DungeonPiece piece : pieces)
			{
				if (piece.plane != zone.getUndergroundPoint().getPlane()
					|| (layer != null && piece.layer != layer))
				{
					continue;
				}
				final int[] bounds = piece.worldBounds();
				if (worldX >= bounds[0] && worldX <= bounds[2]
					&& worldY >= bounds[1] && worldY <= bounds[3])
				{
					return true;
				}
			}
		}
		return false;
	}

	private static boolean isDungeonPoiKey(String key)
	{
		return "dungeon".equals(key) || "dungeon_link".equals(key);
	}

	private boolean categoryEnabled(PoiCategory category)
	{
		switch (category)
		{
			case BANKS:
				return config.iconBanks();
			case SHOPS:
				return config.iconShops();
			case SKILLING:
				return config.iconSkilling();
			case TRAVEL:
				return config.iconTravel();
			case QUESTS:
				return config.iconQuests();
			case ALTARS:
				return config.iconAltars();
			case DUNGEONS:
				return config.iconDungeons();
			case SERVICES:
				return config.iconServices();
			case OTHER:
				return config.iconOther();
			default:
				return false;
		}
	}

	/** True when at least one badge category is switched on, so the draw pass is worth running. */
	private boolean anyPoiCategoryEnabled()
	{
		return config.iconBanks() || config.iconShops() || config.iconSkilling() || config.iconTravel()
			|| config.iconQuests() || config.iconAltars() || config.iconDungeons() || config.iconServices()
			|| config.iconOther();
	}
}
