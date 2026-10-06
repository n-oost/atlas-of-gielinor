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

import com.bettermap.BetterMapConfig;
import com.bettermap.data.DungeonPiece;
import com.bettermap.data.DungeonPieceTransform;
import com.bettermap.data.OverlayFloor;
import com.bettermap.data.UndergroundZone;
import com.bettermap.map.DungeonPieceIndex;
import com.bettermap.map.InstanceMaps;
import com.bettermap.map.MapCamera;
import com.bettermap.map.MapRegion;
import com.bettermap.map.PoiCategory;
import com.bettermap.map.PoiDetails;
import com.bettermap.map.PoiIndex;
import com.bettermap.map.ShopIndex;
import com.bettermap.ui.MapRenderStats;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.game.AgilityShortcut;
import net.runelite.client.ui.FontManager;

/** Draws POI icons and place names, and tracks their hover bounds. */
public class PoiMarkerRenderer
{
	private final Set<Object> visibleTooltipTargets;

	private static final Font TINY = FontManager.getDefaultBoldFont().deriveFont(9f);
	/** Below this many pixels per game tile, icons are more clutter than information. */
	private static final double ICON_ZOOM_THRESHOLD = 0.45;
	private static final Pattern FAIRY_RING_CODE = Pattern.compile("Fairy Ring \\(([A-Z]{3})\\)");
	private static final Map<Long, String> FAIRY_RING_CODES = poiLabels("transportation", FAIRY_RING_CODE);
	private static final Map<Long, String> SHORTCUT_LEVELS = shortcutLevels();
	/** Cities and kingdoms stay visible while zoomed out. */
	static final double PLACE_MAJOR_ZOOM = ICON_ZOOM_THRESHOLD;
	/** Districts and landmarks need a closer look, similar to monster spawn areas. */
	static final double PLACE_LOCAL_ZOOM = 3.0;
	/** Dungeon room names once the interior is actually on screen. */
	static final double PLACE_INTERIOR_ZOOM = 4.0;
	private static final Color PLACE_FILL = new Color(248, 236, 196);
	private static final Color PLACE_OUTLINE = new Color(16, 12, 8, 210);
	private static final Set<String> PLACE_NAME_SKIP = new HashSet<>(Arrays.asList(
		"a", "b", "c", "d", "e", "f", "n", "s", "w",
		"anger", "nothing interesting happens here", "upper level", "lower level",
		"middle level", "exit"));
	private final BetterMapConfig config;
	private final MapCamera camera;
	private final PoiIndex poiIndex;
	private final ShopIndex shopIndex;
	private final MapRenderStats stats;
	private final DungeonPieceIndex dungeonPieceIndex;
	private final List<PoiIconHit> visiblePoiIcons = new ArrayList<>();
	private final List<ShopIconHit> visibleShopIcons = new ArrayList<>();

	public PoiMarkerRenderer(
		BetterMapConfig config,
		MapCamera camera,
		PoiIndex poiIndex,
		ShopIndex shopIndex,
		MapRenderStats stats,
		DungeonPieceIndex dungeonPieceIndex,
		Set<Object> visibleTooltipTargets)
	{
		this.config = config;
		this.camera = camera;
		this.poiIndex = poiIndex;
		this.shopIndex = shopIndex;
		this.stats = stats;
		this.dungeonPieceIndex = dungeonPieceIndex;
		this.visibleTooltipTargets = visibleTooltipTargets;
	}

	private void forEachPoiInView(int plane, int minX, int maxX, int minY, int maxY,
		Consumer<PoiIndex.Poi> consumer)
	{
		forEachPoiInView(plane, minX, maxX, minY, maxY, true, consumer);
	}

	private void forEachPoiInView(int plane, int minX, int maxX, int minY, int maxY,
		boolean projectDungeons, Consumer<PoiIndex.Poi> consumer)
	{
		if (camera.isUndergroundModeActive())
		{
			for (UndergroundZone zone : camera.previewUndergroundZones())
			{
				final java.util.Set<String> seen = new java.util.HashSet<>();
				final List<DungeonPiece> pieces = dungeonPieceIndex.piecesFor(zone.getId());
				if (pieces.isEmpty() && !zone.getId().startsWith("native_"))
				{
					final int x = zone.getUndergroundPoint().getX();
					final int y = zone.getUndergroundPoint().getY();
					final int radius = zone.getRadius();
					poiIndex.forEachInArea(zone.getUndergroundPoint().getPlane(),
						x - radius, x + radius, y - radius, y + radius, poi ->
						{
							if (InstanceMaps.belongsToZone(poi.getX(), poi.getY(), zone))
							{
								consumer.accept(poi.withDisplayPoint(new java.awt.geom.Point2D.Double(
									poi.getX() + 0.5 - zone.getDeltaX(), poi.getY() + 0.5 - zone.getDeltaY()), false));
							}
						});
				}
				for (DungeonPiece piece : pieces)
				{
					if (!camera.isDungeonPieceVisible(zone, piece)) continue;
					for (int[] rect : piece.rects)
					{
						poiIndex.forEachInArea(piece.plane, rect[0], rect[2], rect[1], rect[3], poi ->
						{
							final java.awt.geom.Point2D point = DungeonPieceTransform.toDisplay(piece,
								poi.getX() + 0.5, poi.getY() + 0.5);
							final String key = poi.getKey() + ":" + poi.getName() + ":" + point;
							if (point.getX() >= minX && point.getX() <= maxX
								&& point.getY() >= minY && point.getY() <= maxY && seen.add(key))
							{
								consumer.accept(poi.withDisplayPoint(point, zone.getId().startsWith("native_")));
							}
						});
					}
				}
			}
			return;
		}
		InstanceMaps.forEachQueryArea(minX, maxX, minY, maxY, camera.getCenterX(), camera.getCenterY(),
			projectDungeons,
			(qMinX, qMaxX, qMinY, qMaxY) -> poiIndex.forEachInArea(plane, qMinX, qMaxX, qMinY, qMaxY, poi ->
			{
				if (ViewWindow.isPoiDrawable(camera, poi.getX(), poi.getY()))
				{
					consumer.accept(poi);
				}
			}));
	}

	private double poiScreenX(PoiIndex.Poi poi, Rectangle bounds)
	{
		return poi.getDisplayPoint() == null
			? camera.screenX(poi.getX() + 0.5, poi.getY() + 0.5, bounds)
			: camera.screenX(poi.getDisplayPoint().getX(), bounds);
	}

	private double poiScreenY(PoiIndex.Poi poi, Rectangle bounds)
	{
		return poi.getDisplayPoint() == null
			? camera.screenY(poi.getX() + 0.5, poi.getY() + 0.5, bounds)
			: camera.screenY(poi.getDisplayPoint().getY(), bounds);
	}

	private void forEachShopInView(int plane, int minX, int maxX, int minY, int maxY,
		Consumer<ShopIndex.Shop> consumer)
	{
		InstanceMaps.forEachQueryArea(minX, maxX, minY, maxY, camera.getCenterX(), camera.getCenterY(), true,
			(qMinX, qMaxX, qMinY, qMaxY) -> shopIndex.forEachInArea(plane, qMinX, qMaxX, qMinY, qMaxY, shop ->
			{
				if (ViewWindow.isPoiDrawable(camera, shop.getX(), shop.getY()))
				{
					consumer.accept(shop);
				}
			}));
	}
	/**
	 * Banks, shops, altars, dungeons and the rest, from the prefetched wiki icon set.
	 *
	 * <p>Icons are placed in priority order and anything that would overlap one already placed is
	 * dropped. Without that, a town at moderate zoom is an unreadable pile of overlapping
	 * badges; with it the map thins out as you zoom out and fills in as you zoom in.
	 */
	public void drawPoiIcons(Graphics2D graphics, Rectangle bounds, List<Rectangle> placed)
	{
		visiblePoiIcons.clear();
		visibleShopIcons.clear();
		if (!camera.isTravelViewActive() && !anyPoiCategoryEnabled())
		{
			return;
		}

		final int plane = camera.getPlane();
		final ViewWindow view = ViewWindow.from(camera, bounds, 20.0);

		final List<PoiIndex.Poi> candidates = new ArrayList<>();
		forEachPoiInView(plane, view.minXi, view.maxXi, view.minYi, view.maxYi, candidates::add);
		candidates.sort(Comparator.comparingInt((PoiIndex.Poi poi) -> poiPriority(poi.getKey()))
			.thenComparingInt(PoiIndex.Poi::getX)
			.thenComparingInt(PoiIndex.Poi::getY)
			.thenComparing(PoiIndex.Poi::getKey));
		candidates.forEach(poi ->
		{
			if (config.showTravelRoutes() && camera.getZoom() >= config.travelStationMinZoom()
				&& camera.getSelectedTravelNode() != null
				&& PoiCategory.of(poi.getKey()) != PoiCategory.TRAVEL
				&& PoiCategory.of(poi.getKey()) != PoiCategory.SHORTCUTS)
			{
				return;
			}
			if (!shouldDrawPoiIcon(poi))
			{
				stats.iconsFiltered++;
				return;
			}

			final BufferedImage icon = poiIndex.icon(poi.getKey());
			if (icon == null)
			{
				return;
			}

			final Rectangle rect = new Rectangle(
				(int) Math.round(poiScreenX(poi, bounds)) - icon.getWidth() / 2,
				(int) Math.round(poiScreenY(poi, bounds)) - icon.getHeight() / 2,
				icon.getWidth(),
				icon.getHeight());

			if (!bounds.intersects(rect))
			{
				return;
			}

			final String label = poiLabel(poi);
			final Rectangle labelBounds = label != null ? poiLabelBounds(graphics, poi, rect, label) : null;
			final Rectangle markerBounds = labelBounds != null ? rect.union(labelBounds) : rect;
			if (ViewWindow.overlapsPlaced(placed, markerBounds))
			{
				stats.iconsSkipped++;
				return;
			}

			graphics.drawImage(icon, rect.x, rect.y, null);
			if (labelBounds != null)
			{
				drawPoiLabel(graphics, label, labelBounds);
			}
			placed.add(markerBounds);
			visiblePoiIcons.add(new PoiIconHit(markerBounds, poi));
			stats.iconsDrawn++;
		});

		if (!camera.isTravelViewActive() && config.iconShops() && camera.getZoom() >= minZoomForPoi("general_store")
			&& !(config.showTravelRoutes() && camera.getZoom() >= config.travelStationMinZoom()
				&& camera.getSelectedTravelNode() != null)
			&& shopIndex != null && shopIndex.isLoaded())
		{
			forEachShopInView(plane, view.minXi, view.maxXi, view.minYi, view.maxYi, shop ->
			{
				final BufferedImage icon = poiIndex.icon(shop.getIcon());
				if (icon == null)
				{
					return;
				}

				final double sx = shop.getX() + 0.5 + shop.getDisplayOffsetX();
				final double sy = shop.getY() + 0.5 + shop.getDisplayOffsetY();
				final Rectangle rect = new Rectangle(
					(int) Math.round(camera.screenX(sx, sy, bounds)) - icon.getWidth() / 2,
					(int) Math.round(camera.screenY(sx, sy, bounds)) - icon.getHeight() / 2,
					icon.getWidth(),
					icon.getHeight());

				if (!bounds.intersects(rect) || ViewWindow.overlapsPlaced(placed, rect))
				{
					return;
				}

				graphics.drawImage(icon, rect.x, rect.y, null);
				placed.add(rect);
				visibleTooltipTargets.add(shop);
				visibleShopIcons.add(new ShopIconHit(rect, shop));
				stats.iconsDrawn++;
			});
		}
	}

	private static long poiLabelKey(int x, int y, int plane)
	{
		return ((long) plane << 32) | ((long) (x & 0xFFFF) << 16) | (y & 0xFFFF);
	}

	/** Prepare labels once; generic native POI names do not contain codes or levels. */
	private static Map<Long, String> poiLabels(String key, Pattern pattern)
	{
		final Map<Long, String> labels = new HashMap<>();
		for (PoiIndex.Poi poi : PoiDetails.getAllPois())
		{
			if (key.equals(poi.getKey()))
			{
				final Matcher matcher = pattern.matcher(poi.getName());
				if (matcher.find())
				{
					labels.put(poiLabelKey(poi.getX(), poi.getY(), poi.getPlane()), matcher.group(1));
				}
			}
		}
		return labels;
	}

	private static Map<Long, String> shortcutLevels()
	{
		final Map<Long, String> levels = poiLabels("agility_short-cut", Pattern.compile("\\(Level (\\d+)\\)"));
		for (AgilityShortcut shortcut : AgilityShortcut.values())
		{
			final WorldPoint location = shortcut.getWorldMapLocation();
			if (location != null)
			{
				levels.put(poiLabelKey(location.getX(), location.getY(), location.getPlane()),
					Integer.toString(shortcut.getLevel()));
			}
		}
		return levels;
	}

	private String poiLabel(PoiIndex.Poi poi)
	{
		if (!camera.isTravelViewActive() && camera.getFrameZoom() <= ("agility_short-cut".equals(poi.getKey())
			? config.shortcutLabelMinZoom() : config.fairyRingLabelMinZoom()))
		{
			return null;
		}
		final boolean shortcut = "agility_short-cut".equals(poi.getKey());
		if (!shortcut && !"transportation".equals(poi.getKey()))
		{
			return null;
		}
		final Map<Long, String> labels = shortcut ? SHORTCUT_LEVELS : FAIRY_RING_CODES;
		if (!shortcut)
		{
			final Matcher matcher = FAIRY_RING_CODE.matcher(poi.getName());
			if (matcher.find())
			{
				return matcher.group(1);
			}
		}
		String label = null;
		int nearestDistance = 5;
		// Native icon centres can be a tile or two away from the metadata coordinate.
		for (int dx = -2; dx <= 2; dx++)
		{
			for (int dy = -2; dy <= 2; dy++)
			{
				final int distance = dx * dx + dy * dy;
				if (distance >= nearestDistance)
				{
					continue;
				}
				final String candidate = labels.get(poiLabelKey(poi.getX() + dx, poi.getY() + dy, poi.getPlane()));
				if (candidate != null)
				{
					label = candidate;
					nearestDistance = distance;
				}
			}
		}
		return label;
	}

	private static Rectangle poiLabelBounds(Graphics2D graphics, PoiIndex.Poi poi, Rectangle iconBounds, String label)
	{
		final boolean shortcut = "agility_short-cut".equals(poi.getKey());
		final FontMetrics fm = graphics.getFontMetrics(TINY);
		final int width = fm.stringWidth(label) + 6;
		final int height = fm.getAscent() + fm.getDescent() + 2;
		return new Rectangle(
			shortcut ? iconBounds.x + iconBounds.width - width / 2 : iconBounds.x + (iconBounds.width - width) / 2,
			shortcut ? iconBounds.y - height / 2 : iconBounds.y + iconBounds.height + 2,
			width, height);
	}

	private static void drawPoiLabel(Graphics2D graphics, String label, Rectangle labelBounds)
	{
		final Font oldFont = graphics.getFont();
		final Color oldColor = graphics.getColor();
		graphics.setFont(TINY);
		graphics.setColor(CARD_BG);
		graphics.fillRoundRect(labelBounds.x, labelBounds.y, labelBounds.width, labelBounds.height, 4, 4);
		graphics.setColor(PLACE_FILL);
		graphics.drawString(label, labelBounds.x + 3, labelBounds.y + 1 + graphics.getFontMetrics().getAscent());
		graphics.setFont(oldFont);
		graphics.setColor(oldColor);
	}

	public PoiIndex.Poi visiblePoiIconAt(java.awt.Point cursor)
	{
		for (int i = visiblePoiIcons.size() - 1; i >= 0; i--)
		{
			final PoiIconHit hit = visiblePoiIcons.get(i);
			final Rectangle r = hit.bounds;
			if (cursor.x >= r.x - 3 && cursor.x <= r.x + r.width + 3
				&& cursor.y >= r.y - 3 && cursor.y <= r.y + r.height + 3)
			{
				return hit.poi;
			}
		}
		return null;
	}

	private static final class PoiIconHit
	{
		private final Rectangle bounds;
		private final PoiIndex.Poi poi;

		private PoiIconHit(Rectangle bounds, PoiIndex.Poi poi)
		{
			this.bounds = bounds;
			this.poi = poi;
		}
	}

	private static final class ShopIconHit
	{
		private final Rectangle bounds;
		private final ShopIndex.Shop shop;

		private ShopIconHit(Rectangle bounds, ShopIndex.Shop shop)
		{
			this.bounds = bounds;
			this.shop = shop;
		}
	}

	public ShopIndex.Shop visibleShopIconAt(java.awt.Point cursor)
	{
		for (int i = visibleShopIcons.size() - 1; i >= 0; i--)
		{
			final ShopIconHit hit = visibleShopIcons.get(i);
			final Rectangle r = hit.bounds;
			if (cursor.x >= r.x - 3 && cursor.x <= r.x + r.width + 3
				&& cursor.y >= r.y - 3 && cursor.y <= r.y + r.height + 3)
			{
				return hit.shop;
			}
		}
		return null;
	}

	/**
	 * Cache region labels ("Lumbridge", "Araxxor") drawn as outlined map text. These are not
	 * icon badges — {@link #shouldDrawPoiIcon} still returns false for {@code region_label}.
	 */
	public void drawPlaceNames(Graphics2D graphics, Rectangle bounds)
	{
		final double zoom = camera.getZoom();
		if (!config.showPlaceNames())
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
			if (!poi.isNativeLayout() && !isDrawablePlaceName(name))
			{
				return;
			}
			if (poi.isNativeLayout() ? zoom < config.interiorPlaceMinZoom()
				: !shouldDrawPlaceName(name, poi.getY(), zoom, dungeonFocused, cameraOnOverworld,
					configuredMinZoomForPlace(name, poi.getY())))
			{
				return;
			}

			final boolean major = isMajorPlaceName(name);
			final FontMetrics fm = major ? fmBold : fmSmall;
			graphics.setFont(major ? boldFont : smallFont);

			final int sx = (int) Math.round(poiScreenX(poi, bounds));
			final int sy = (int) Math.round(poiScreenY(poi, bounds));
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
		return isDrawablePlaceName(name)
			&& shouldDrawPlaceName(name, worldY, zoom, dungeonFocused, cameraOnOverworld,
				minZoomForPlace(name, worldY));
	}

	public static boolean shouldDrawPlaceName(String name, int worldY, double zoom, boolean dungeonFocused,
		boolean cameraOnOverworld, double minimumZoom)
	{
		if (!isDrawablePlaceName(name))
		{
			return false;
		}
		if (isInteriorPlaceLabel(worldY) && cameraOnOverworld && !dungeonFocused)
		{
			return false;
		}
		return zoom >= minimumZoom;
	}

	public static double minZoomForPlace(String name, int worldY)
	{
		if (isInteriorPlaceLabel(worldY))
		{
			return PLACE_INTERIOR_ZOOM;
		}
		return isMajorPlaceName(name) ? PLACE_MAJOR_ZOOM : PLACE_LOCAL_ZOOM;
	}

	private double configuredMinZoomForPlace(String name, int worldY)
	{
		if (isInteriorPlaceLabel(worldY))
		{
			return config.interiorPlaceMinZoom();
		}
		return isMajorPlaceName(name) ? config.majorPlaceMinZoom() : config.localPlaceMinZoom();
	}

	private static boolean isMajorPlaceName(String name)
	{
		final String key = name.trim().toLowerCase(Locale.ROOT);
		return MapRegion.isPlaceName(key) || key.startsWith("kingdom of ")
			|| key.contains("island") || key.contains("isle")
			|| key.contains("atoll") || key.contains("archipelago");
	}

	/** Lower values reserve space first as the map gets crowded. */
	private static int poiPriority(String key)
	{
		// Keep Slayer masters' existing overlap priority after moving them into Skilling.
		if ("slayer_master".equals(key))
		{
			return 4;
		}
		switch (PoiCategory.of(key))
		{
			case TRAVEL:
			case SHORTCUTS:
				return 0;
			case QUESTS:
				return "minigame".equals(key) || "raids_lobby".equals(key) ? 2 : 1;
			case SKILLING:
				return 3;
			case BANKS:
			case DUNGEONS:
				return 4;
			case ALTARS:
				return 5;
			case SHOPS:
				return 6;
			case SERVICES:
				return 7;
			default:
				return 8;
		}
	}

	private double minZoomForPoi(String key)
	{
		switch (PoiCategory.of(key))
		{
			case BANKS: return config.bankMinZoom();
			case SHOPS: return config.shopMinZoom();
			case SKILLING:
				return "slayer_master".equals(key) ? config.slayerMasterMinZoom() : config.skillingMinZoom();
			case SHORTCUTS: return config.shortcutMinZoom();
			case SAILING: return config.sailingLookoutMinZoom();
			case TRAVEL: return config.travelMinZoom();
			case QUESTS:
				return "minigame".equals(key) || "raids_lobby".equals(key)
					? config.activityMinZoom() : config.questMinZoom();
			case ALTARS: return config.altarMinZoom();
			case DUNGEONS: return config.dungeonMinZoom();
			case SERVICES: return config.serviceMinZoom();
			case PLACES: return config.localPlaceMinZoom();
			case OTHER: return config.otherMinZoom();
			default: throw new IllegalArgumentException("Unhandled POI category: " + PoiCategory.of(key));
		}
	}

	public boolean shouldDrawPoiIcon(PoiIndex.Poi poi)
	{
		if (poi == null || poi.getKey() == null)
		{
			return false;
		}
		if (camera != null && camera.isTravelViewActive())
		{
			final PoiCategory category = PoiCategory.of(poi.getKey());
			return (category == PoiCategory.TRAVEL || category == PoiCategory.SHORTCUTS)
				&& ViewWindow.isPoiDrawable(camera, poi.getX(), poi.getY());
		}
		if (camera != null && camera.getZoom() < minZoomForPoi(poi.getKey()))
		{
			return false;
		}

		if (camera != null && !ViewWindow.isPoiDrawable(camera, poi.getX(), poi.getY()))
		{
			return false;
		}
		if (poi.getDisplayPoint() == null && poi.getY() <= InstanceMaps.GAP_MIN_Y && coveredByDungeonPreview(poi.getX(), poi.getY()))
		{
			return false;
		}

		if (config.showLargeUndergroundSymbols()
			&& (camera == null || camera.getZoom() >= config.dungeonNavigationMinZoom())
			&& isDungeonPoiKey(poi.getKey())
			&& hasNearbyDungeonButton(poi))
		{
			return false;
		}

		return categoryEnabled(PoiCategory.of(poi.getKey()));
	}

	private static boolean hasNearbyDungeonButton(PoiIndex.Poi poi)
	{
		if (UndergroundZone.forSurfacePoint(poi.getX(), poi.getY(), 8) != null)
		{
			return true;
		}
		for (UndergroundZone zone : UndergroundZone.ALL_ZONES)
		{
			if (zone.getId().contains("__") || zone.getId().startsWith("native_")) continue;
			for (int i = 0; i < zone.getSurfacePoints().size(); i++)
			{
				if (zone.isEntranceMarkerHidden(i))
				{
					continue;
				}
				final WorldPoint point = zone.getEntranceMarkerPoint(i);
				final long dx = poi.getX() - point.getX();
				final long dy = poi.getY() - point.getY();
				if (poi.getPlane() == point.getPlane() && dx * dx + dy * dy <= 64)
				{
					return true;
				}
			}
		}
		return false;
	}

	/** Surface POIs under projected dungeon geometry make dungeon previews unreadable. */
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
				if (!camera.isDungeonPieceVisible(zone, piece))
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
			case SHORTCUTS:
				return config.iconShortcuts();
			case SAILING:
				return config.iconSailing();
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
			case PLACES:
				return false;
			default:
				throw new IllegalArgumentException("Unhandled POI category: " + category);
		}
	}

	/** True when at least one badge category is switched on, so the draw pass is worth running. */
	private boolean anyPoiCategoryEnabled()
	{
		return config.iconBanks() || config.iconShops() || config.iconSkilling() || config.iconTravel()
			|| config.iconQuests() || config.iconAltars() || config.iconDungeons() || config.iconServices()
			|| config.iconOther() || config.iconShortcuts() || config.iconSailing();
	}

}
