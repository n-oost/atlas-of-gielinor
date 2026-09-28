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

import static com.bettermap.ui.MapStyle.BOSSES;
import static com.bettermap.ui.MapStyle.CARD_BG;
import static com.bettermap.ui.MapStyle.CLUE_EDGE;
import static com.bettermap.ui.MapStyle.CLUE_FILL;
import static com.bettermap.ui.MapStyle.CARD_EDGE;
import static com.bettermap.ui.MapStyle.CARD_TEXT;
import static com.bettermap.ui.MapStyle.CARD_TITLE;
import static com.bettermap.ui.MapStyle.ROUTE_SHADOW;
import static com.bettermap.ui.MapStyle.SMALL;
import static com.bettermap.ui.MapStyle.TEXT_DIM;

import com.bettermap.BetterMapConfig;
import com.bettermap.data.MonsterLocationData;
import com.bettermap.data.DungeonPiece;
import com.bettermap.data.DungeonPieceTransform;
import com.bettermap.data.OverlayCluster;
import com.bettermap.data.OverlayFloor;
import com.bettermap.data.TravelData;
import com.bettermap.data.UndergroundZone;
import com.bettermap.data.sailing.BoatTracker;
import com.bettermap.data.sailing.BoatType;
import com.bettermap.data.sailing.PlayerBoat;
import com.bettermap.data.sailing.PortNoticeBoard;
import com.bettermap.data.sailing.SailingPort;
import com.bettermap.map.ClueScrollTracker;
import com.bettermap.map.InstanceMaps;
import com.bettermap.map.MapCamera;
import com.bettermap.map.DungeonPieceIndex;
import com.bettermap.map.MapFinder;
import com.bettermap.map.MapRegion;
import com.bettermap.map.GroundItemIndex;
import com.bettermap.map.MonsterIconManager;
import com.bettermap.map.MonsterIndex;
import com.bettermap.map.PoiCategory;
import com.bettermap.map.PoiIndex;
import com.bettermap.map.PrifddinasShift;
import com.bettermap.map.SlayerTaskTracker;
import com.bettermap.map.WorldMapPointReader;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.QuadCurve2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Everything drawn on top of the tiles that marks a place: POI badges, layer symbols,
 * RuneLite's own world-map points, monsters and their zones, ground items, travel and sailing
 * routes, the player, and the search flash.
 *
 * <p>Split out of {@link BetterWorldMapOverlay}. The {@code placed} lists are still owned by
 * {@code render()} and passed in, so the decluttering order across these passes is unchanged.
 */
@Slf4j
class MapMarkerRenderer
{
	private static final Color PLAYER_FILL = new Color(255, 255, 255);
	/** Translucent fill for the player arrow when it is projected onto an entrance, not a real fix. */
	private static final Color PLAYER_FILL_BELOW = new Color(255, 255, 255, 90);
	private static final Color PLAYER_EDGE = new Color(18, 18, 18);
	private static final Color BOAT_EDGE = new Color(14, 18, 26, 220);
	private static final Color BOAT_FILL = new Color(38, 128, 205);
	private static final Color BOAT_BORDER = new Color(255, 214, 110);
	private static final Color BOAT_HULL_GOOD = new Color(55, 215, 115);
	private static final Color BOAT_HULL_WARN = new Color(245, 185, 55);
	private static final Color BOAT_HULL_BAD = new Color(235, 65, 65);
	private static final Color PORT_EDGE = new Color(14, 20, 28, 220);
	private static final Color PORT_FILL = new Color(32, 95, 145, 200);
	private static final Color PORT_BORDER = new Color(130, 210, 255, 230);
	private static final Color PORT_DOT = new Color(220, 245, 255);
	private static final Color BOARD_EDGE = new Color(24, 18, 12, 220);
	private static final Color BOARD_FILL = new Color(175, 115, 45, 220);
	private static final Color BOARD_BORDER = new Color(255, 225, 140, 240);
	private static final Color BOARD_GLYPH = new Color(255, 245, 220);
	private static final Color MONSTER_FILL = new Color(224, 72, 72);
	private static final Color MONSTER_EDGE = new Color(18, 18, 18);
	private static final Color MONSTER_ZONE_FILL = new Color(224, 72, 72, 75);
	private static final Color MONSTER_ZONE_EDGE = new Color(180, 45, 45, 130);
	private static final Color MONSTER_ZONE_SLAYER = new Color(180, 90, 220, 80);
	private static final Color MONSTER_ZONE_SLAYER_EDGE = new Color(140, 60, 180, 140);
	private static final Color MONSTER_ZONE_TASK_FILL = new Color(0, 230, 220, 100);
	private static final Color MONSTER_ZONE_TASK_EDGE = new Color(20, 255, 235, 230);
	private static final Color MONSTER_ZONE_TASK_GLOW = new Color(0, 210, 200, 45);
	private static final Color CLUE_GLOW = new Color(150, 110, 240, 60);
	private static final Font TINY = FontManager.getDefaultBoldFont().deriveFont(9f);
	private static final long FLASH_MILLIS = 1600L;
	/** Fixed colours that were being reallocated inside per-frame draw loops. */
	private static final Color LAYER_CHIP_BG_HOVER = new Color(28, 34, 48, 235);
	private static final Color LAYER_CHIP_BG = new Color(16, 20, 28, 195);
	private static final Color LAYER_CHIP_EDGE = new Color(120, 180, 240, 220);
	private static final Color LAYER_CHIP_TEXT = new Color(140, 200, 255);
	private static final Color REGION_EDGE_HOVER = new Color(160, 230, 190, 230);
	private static final Color FLOOR_CHIP_BG = new Color(36, 28, 16, 200);
	private static final Color FLOOR_CHIP_BG_HOVER = new Color(56, 42, 18, 235);
	private static final Color FLOOR_CHIP_EDGE = new Color(230, 180, 80, 220);
	private static final Color FLOOR_CHIP_EDGE_HOVER = new Color(255, 220, 140, 240);
	private static final Color FLOOR_CHIP_TEXT = new Color(255, 220, 150);
	private static final Color LAYER_LABEL_BG = new Color(12, 14, 20, 215);
	private static final Color ZONE_CHIP_TEXT = new Color(130, 255, 240);
	private static final Color ZONE_MARK_SLAYER = new Color(180, 90, 220, 220);
	private static final Color ZONE_MARK = new Color(224, 72, 72, 200);
	private static final Color ZONE_CHIP_SLAYER_EDGE = new Color(180, 90, 220, 200);
	private static final Color ZONE_CHIP_EDGE = new Color(224, 72, 72, 180);
	private static final Color ZONE_CHIP_SLAYER_TEXT = new Color(230, 180, 255);
	/** Ground item spawns: green, so they never read as a monster (red) or Slayer zone (purple). */
	private static final Color GROUND_ITEM_EDGE = new Color(126, 214, 134, 210);
	private static final Color GROUND_ITEM_DOT = new Color(126, 214, 134, 235);
	private static final Color ROUTE_OUTLINE = new Color(18, 18, 18);
	/** Dashed connector across a transport hop (teleport, fairy ring, stairs) between two walk runs. */
	/** The player marker is drawn under a rotate(), so its outline never varies. */
	private static final int[] PLAYER_ARROW_X = {0, -5, 0, 5};
	private static final int[] PLAYER_ARROW_Y = {-8, 6, 3, 6};
	private static final SailingPort[] PORTS = SailingPort.values();
	/** Below this many pixels per game tile, icons are more clutter than information. */
	private static final double ICON_ZOOM_THRESHOLD = 0.45;
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

	/**
	 * Outline weights, thinnest to thickest. BasicStroke is immutable, so one instance each.
	 * Only this class draws markers, so they live here rather than in {@link MapStyle}.
	 */
	private static final Stroke MARKER_OUTLINE = new BasicStroke(1.0f);
	private static final Stroke SYMBOL_BORDER = new BasicStroke(1.1f);
	private static final Stroke LABEL_BORDER = new BasicStroke(1.2f);
	private static final Stroke MARKER_RING = new BasicStroke(1.5f);
	private static final Stroke SYMBOL_BORDER_HOVER = new BasicStroke(1.6f);
	private static final Stroke TASK_EDGE = new BasicStroke(1.8f);
	private static final Stroke ACCENT_RING = new BasicStroke(2.0f);
	private static final Stroke PLAYER_ARROW_EDGE = new BasicStroke(3.0f);

	/** Route polylines are drawn round-capped so the joins between hops do not notch. */
	private static final Stroke ROUTE_LINE_STROKE =
		new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	private static final Stroke ROUTE_HALO_STROKE =
		new BasicStroke(5.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

	/** Dash phases for animated travel routes — one stroke per phase, no per-frame allocation. */
	private static final int TRAVEL_DASH_PHASES = 14;
	private static final Stroke[] TRAVEL_DASH_STROKES = buildTravelDashStrokes();

	private static Stroke[] buildTravelDashStrokes()
	{
		final Stroke[] strokes = new Stroke[TRAVEL_DASH_PHASES];
		for (int i = 0; i < TRAVEL_DASH_PHASES; i++)
		{
			strokes[i] = new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f,
				new float[]{ 8f, 6f }, i);
		}
		return strokes;
	}

	private final Client client;
	private final BetterMapConfig config;
	private final MapCamera camera;
	private final PoiIndex poiIndex;
	private final MonsterIndex monsterIndex;
	private final MonsterIconManager monsterIconManager;
	private final SlayerTaskTracker slayerTaskTracker;
	private final ClueScrollTracker clueScrollTracker;
	private final GroundItemIndex groundItemIndex;
	private final BoatTracker boatTracker;
	private final WorldMapPointManager worldMapPointManager;
	private final WorldMapPointReader worldMapPointReader;
	private final MapFinder finder;
	private final MapRenderStats stats;
	private final DungeonPieceIndex dungeonPieceIndex;
	private BufferedImage dungeonExclamation;
	private BufferedImage dungeonExclamationGreen;
	private BufferedImage boatSloopIcon;
	private BufferedImage boatSkiffIcon;
	private BufferedImage boatRaftIcon;
	private List<int[]> walkingRuns = java.util.Collections.emptyList();

	MapMarkerRenderer(
		Client client,
		BetterMapConfig config,
		MapCamera camera,
		PoiIndex poiIndex,
		MonsterIndex monsterIndex,
		MonsterIconManager monsterIconManager,
		SlayerTaskTracker slayerTaskTracker,
		ClueScrollTracker clueScrollTracker,
		GroundItemIndex groundItemIndex,
		BoatTracker boatTracker,
		WorldMapPointManager worldMapPointManager,
		WorldMapPointReader worldMapPointReader,
		MapFinder finder,
		MapRenderStats stats,
		DungeonPieceIndex dungeonPieceIndex)
	{
		this.client = client;
		this.config = config;
		this.camera = camera;
		this.poiIndex = poiIndex;
		this.monsterIndex = monsterIndex;
		this.monsterIconManager = monsterIconManager;
		this.slayerTaskTracker = slayerTaskTracker;
		this.clueScrollTracker = clueScrollTracker;
		this.groundItemIndex = groundItemIndex;
		this.boatTracker = boatTracker;
		this.worldMapPointManager = worldMapPointManager;
		this.worldMapPointReader = worldMapPointReader;
		this.finder = finder;
		this.stats = stats;
		this.dungeonPieceIndex = dungeonPieceIndex;
	}

	/**
	 * The slice of the world on screen, widened by a margin so a marker just off the edge still
	 * draws - icons overhang their own tile, so clipping to the exact viewport pops them out.
	 *
	 * <p>Both forms are precomputed: the draw passes test against the doubles, the index lookups
	 * (which take tile coordinates) against the rounded-outward ints. Doing it once per pass
	 * rather than per marker matters - these run every frame.
	 */
	private static final class ViewWindow
	{
		private final double minX;
		private final double maxX;
		private final double minY;
		private final double maxY;
		private final int minXi;
		private final int maxXi;
		private final int minYi;
		private final int maxYi;

		private ViewWindow(double minX, double maxX, double minY, double maxY)
		{
			this.minX = minX;
			this.maxX = maxX;
			this.minY = minY;
			this.maxY = maxY;
			this.minXi = (int) Math.floor(minX);
			this.maxXi = (int) Math.ceil(maxX);
			this.minYi = (int) Math.floor(minY);
			this.maxYi = (int) Math.ceil(maxY);
		}
	}

	/** Screen rectangle to world window. Y flips: screen maxY is world minY. */
	private ViewWindow viewWindow(Rectangle bounds, double margin)
	{
		return new ViewWindow(
			camera.worldX(bounds.getMinX(), bounds) - margin,
			camera.worldX(bounds.getMaxX(), bounds) + margin,
			camera.worldY(bounds.getMaxY(), bounds) - margin,
			camera.worldY(bounds.getMinY(), bounds) + margin);
	}

	/**
	 * Whether a marker at these game coordinates may be drawn in the current view. Keeps dungeon
	 * and instance content — which the cache stores in the band above the overworld — off the
	 * surface, where it would otherwise hang north of the world with nothing under it.
	 */
	private boolean drawable(int worldX, int worldY)
	{
		if (InstanceMaps.hiddenOnSurface(worldX, worldY, camera.getCenterX(), camera.getCenterY()))
		{
			return false;
		}
		return InstanceMaps.inFocusedLayer(worldX, worldY,
			camera.getFocusedUndergroundZone(), camera.isDungeonContentsFocused());
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
				if (drawable(poi.getX(), poi.getY()))
				{
					consumer.accept(poi);
				}
			}));
	}

	private void forEachMonsterInView(int plane, int minX, int maxX, int minY, int maxY,
		Consumer<MonsterIndex.Zone> consumer)
	{
		InstanceMaps.forEachQueryArea(minX, maxX, minY, maxY, camera.getCenterX(), camera.getCenterY(), false,
			(qMinX, qMaxX, qMinY, qMaxY) -> monsterIndex.forEachInArea(plane, qMinX, qMaxX, qMinY, qMaxY, zone ->
			{
				if (drawable(zone.getX(), zone.getY()))
				{
					consumer.accept(zone);
				}
			}));
	}

	/**
	 * Monster zones stay on their native dungeon tiles. {@link MapCamera#screenX(double, double, Rectangle)}
	 * would slide those tiles onto the overworld entrance, which is how cave spawns used to
	 * appear in town. Prifddinas still composites onto Tirannwn.
	 */
	private double monsterScreenX(double worldX, double worldY, Rectangle bounds)
	{
		return camera.screenX(PrifddinasShift.toDisplayX(worldX, worldY), bounds);
	}

	private double monsterScreenY(double worldX, double worldY, Rectangle bounds)
	{
		return camera.screenY(PrifddinasShift.toDisplayY(worldX, worldY), bounds);
	}
	/**
	 * Banks, shops, altars, dungeons and the rest, from the prefetched wiki icon set.
	 *
	 * <p>Icons are placed greedily and anything that would overlap one already placed is
	 * dropped. Without that, a town at moderate zoom is an unreadable pile of overlapping
	 * badges; with it the map thins out as you zoom out and fills in as you zoom in.
	 */
	void drawPoiIcons(Graphics2D graphics, Rectangle bounds, List<Rectangle> placed)
	{
		if (!anyPoiCategoryEnabled() || camera.getZoom() < ICON_ZOOM_THRESHOLD)
		{
			return;
		}

		final int plane = camera.getPlane();
		final ViewWindow view = viewWindow(bounds, 20.0);

		forEachPoiInView(plane, view.minXi, view.maxXi, view.minYi, view.maxYi, poi ->
		{
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
				(int) Math.round(camera.screenX(poi.getX() + 0.5, poi.getY() + 0.5, bounds)) - icon.getWidth() / 2,
				(int) Math.round(camera.screenY(poi.getX() + 0.5, poi.getY() + 0.5, bounds)) - icon.getHeight() / 2,
				icon.getWidth(),
				icon.getHeight());

			if (!bounds.intersects(rect))
			{
				return;
			}

			if (overlapsPlaced(placed, rect))
			{
				stats.iconsSkipped++;
				return;
			}

			graphics.drawImage(icon, rect.x, rect.y, null);
			placed.add(rect);
			stats.iconsDrawn++;
		});
	}

	/**
	 * Cache region labels ("Lumbridge", "Araxxor") drawn as outlined map text. These are not
	 * icon badges — {@link #shouldDrawPoiIcon} still returns false for {@code region_label}.
	 */
	void drawPlaceNames(Graphics2D graphics, Rectangle bounds)
	{
		final double zoom = camera.getZoom();
		if (!config.showPlaceNames() || zoom < PLACE_MAJOR_ZOOM)
		{
			return;
		}

		final boolean dungeonFocused = camera.isDungeonContentsFocused();
		final boolean cameraOnOverworld = InstanceMaps.cameraOnOverworld(camera.getCenterY());
		final int plane = camera.getPlane();
		final ViewWindow view = viewWindow(bounds, 40.0);
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

	static boolean isInteriorPlaceLabel(int worldY)
	{
		return worldY > InstanceMaps.GAP_MIN_Y;
	}

	static boolean shouldDrawPlaceName(String name, int worldY, double zoom, boolean dungeonFocused,
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

	static double minZoomForPlace(String name, int worldY)
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

	boolean shouldDrawPoiIcon(PoiIndex.Poi poi)
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
				final long dx = (long) worldX - zone.getSurfacePoint().getX();
				final long dy = (long) worldY - zone.getSurfacePoint().getY();
				if (dx * dx + dy * dy <= (long) zone.getRadius() * zone.getRadius())
				{
					return true;
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
			default:
				return false;
		}
	}

	/** True when at least one badge category is switched on, so the draw pass is worth running. */
	private boolean anyPoiCategoryEnabled()
	{
		return config.iconBanks() || config.iconShops() || config.iconSkilling() || config.iconTravel()
			|| config.iconQuests() || config.iconAltars() || config.iconDungeons() || config.iconServices();
	}

	private static boolean overlapsPlaced(List<Rectangle> placed, Rectangle candidate)
	{
		for (Rectangle other : placed)
		{
			if (other.intersects(candidate))
			{
				return true;
			}
		}
		return false;
	}

	/** True for the clue scroll plugin's own world map point ({@code ClueScrollWorldMapPoint}). */
	private static final java.util.Map<Class<?>, Boolean> CLUE_POINT_CLASS = new java.util.IdentityHashMap<>();

	private static boolean isClueWorldMapPoint(WorldMapPoint point)
	{
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

	/** Scales a glyph offset that was hand-drawn against a 32px box, so {@code size} stays the one knob. */
	private static int glyph(int size, int offsetAt32)
	{
		return Math.round(size * offsetAt32 / 32f);
	}

	private BufferedImage boatIcon(BoatType type)
	{
		if (type == BoatType.RAFT)
		{
			if (boatRaftIcon == null)
			{
				boatRaftIcon = loadBoatIcon("boat_raft");
			}
			return boatRaftIcon;
		}
		if (type == BoatType.SKIFF || type == BoatType.TUTORIAL)
		{
			if (boatSkiffIcon == null)
			{
				boatSkiffIcon = loadBoatIcon("boat_skiff");
			}
			return boatSkiffIcon;
		}
		if (boatSloopIcon == null)
		{
			boatSloopIcon = loadBoatIcon("boat_sloop");
			if (boatSloopIcon == null)
			{
				boatSloopIcon = loadBoatIcon("boat");
			}
		}
		return boatSloopIcon;
	}

	private BufferedImage loadBoatIcon(String name)
	{
		BufferedImage icon = null;
		if (poiIndex != null)
		{
			icon = poiIndex.icon(name);
		}
		if (icon == null)
		{
			try (InputStream in = MapMarkerRenderer.class.getResourceAsStream("/com/bettermap/" + name + ".png"))
			{
				if (in != null)
				{
					icon = ImageIO.read(in);
				}
			}
			catch (IOException e)
			{
				log.warn("Failed to load boat icon: {}", name, e);
			}
		}
		return icon;
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
	 * Overworld: dungeon {@code !} at each zone entrance (hover peeks, click enters) and a green
	 * {@code !} for connected-zone clusters. Underground: return-to-surface chips.
	 */
	void drawLargeLayerSymbols(Graphics2D graphics, Rectangle bounds)
	{
		if (!config.showLargeUndergroundSymbols())
		{
			camera.setLayerSymbolTargets(Collections.emptyList());
			return;
		}

		final boolean onSurface = !camera.isUndergroundModeActive() && camera.getCenterY() < 4200;
		final List<MapCamera.LayerSymbolTarget> targets = new ArrayList<>();
		final int size = Math.max(8, Math.min(32, config.undergroundSymbolSize()));
		final int radius = glyph(size, 8);
		final UndergroundZone focused = camera.getActiveUndergroundZone();

		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		final Stroke oldStroke = graphics.getStroke();
		final List<MapCamera.LayerSymbolTarget> regionTargets = new ArrayList<>();
		if (onSurface)
		{
			drawRegionOverlays(graphics, bounds, regionTargets, size);
		}

		for (UndergroundZone zone : UndergroundZone.ALL_ZONES)
		{
			if (!onSurface && focused != null && zone != focused)
			{
				continue;
			}

			final List<WorldPoint> anchors = onSurface
				? zone.getSurfacePoints()
				: Collections.singletonList(zone.getSurfacePoint());
			for (WorldPoint anchor : anchors)
			{
			final int sx = (int) Math.round(camera.screenX(anchor.getX() + 0.5, anchor.getY() + 0.5, bounds));
			final int sy = (int) Math.round(camera.screenY(anchor.getX() + 0.5, anchor.getY() + 0.5, bounds));

			final Rectangle rect = onSurface
				? dungeonIconRect(anchor.getX(), anchor.getY(), bounds, size)
				: new Rectangle(sx - size / 2, sy - size / 2, size, size);
			if (!bounds.intersects(rect))
			{
				continue;
			}

			final boolean isSurfaceToUnderground = onSurface;
			targets.add(new MapCamera.LayerSymbolTarget(rect, zone, isSurfaceToUnderground));

			final boolean isHovered = camera.getHoveredUndergroundZone() == zone
				&& camera.isHoveredSurfaceToUnderground() == isSurfaceToUnderground
				&& camera.getHoveredFloorPlane() == null;

			if (onSurface)
			{
				final BufferedImage icon = dungeonExclamation(false);
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
			if (isHovered || (!onSurface && camera.getZoom() >= 1.5))
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

		targets.addAll(regionTargets);

		graphics.setStroke(oldStroke);
		camera.setLayerSymbolTargets(targets);
	}

	private void drawFloorOverlays(Graphics2D graphics, Rectangle bounds,
		List<MapCamera.LayerSymbolTarget> targets, int size, boolean onSurface, UndergroundZone focused)
	{
		final Integer hoveredFloor = camera.getHoveredFloorPlane();
		final int chip = Math.max(8, size - 4);
		final OverlayCluster focusedCluster = onSurface ? null : OverlayCluster.forZone(focused);
		final OverlayCluster hoveredCluster = onSurface ? camera.getHoveredOverlayCluster() : null;
		final OverlayCluster activeCluster = onSurface ? camera.getActiveOverlayCluster() : null;
		for (OverlayFloor floor : OverlayFloor.all())
		{
			if (!onSurface && floor.zone != focused
				&& (focusedCluster == null || !focusedCluster.members.contains(floor.zone)))
			{
				continue;
			}
			if (onSurface)
			{
				final boolean matchesHoveredZone = camera.getHoveredUndergroundZone() == floor.zone
					&& camera.isHoveredSurfaceToUnderground();
				final boolean matchesHoveredCluster = hoveredCluster != null && hoveredCluster.members.contains(floor.zone);
				final boolean matchesActiveCluster = activeCluster != null && activeCluster.members.contains(floor.zone);
				if (!matchesHoveredZone && !matchesHoveredCluster && !matchesActiveCluster)
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

	private void drawRegionOverlays(Graphics2D graphics, Rectangle bounds,
		List<MapCamera.LayerSymbolTarget> targets, int size)
	{
		final OverlayCluster hovered = camera.getHoveredOverlayCluster();
		final OverlayCluster active = camera.getActiveOverlayCluster();
		final List<MapCamera.LayerSymbolTarget> drawn = new ArrayList<>();
		for (OverlayCluster cluster : OverlayCluster.all())
		{
			if (cluster.members.size() < 2)
			{
				continue;
			}
			final BufferedImage icon = dungeonExclamation(true);
			final int sx = (int) Math.round(camera.screenX(cluster.iconX + 0.5, cluster.iconY + 0.5, bounds));
			final int sy = (int) Math.round(camera.screenY(cluster.iconX + 0.5, cluster.iconY + 0.5, bounds));
			final Rectangle rect;
			rect = new Rectangle(sx - size / 2, sy - size / 2, size, size);
			if (!bounds.intersects(rect))
			{
				continue;
			}
			drawn.add(new MapCamera.LayerSymbolTarget(rect, cluster));

			final boolean isHovered = hovered == cluster || active == cluster;
			if (isHovered)
			{
				graphics.setColor(REGION_EDGE_HOVER);
				graphics.setStroke(SYMBOL_BORDER_HOVER);
				graphics.drawOval(rect.x - 2, rect.y - 2, rect.width + 4, rect.height + 4);
			}
			if (icon != null)
			{
				graphics.drawImage(icon, rect.x, rect.y, rect.width, rect.height, null);
			}
			else
			{
				graphics.setColor(REGION_EDGE_HOVER);
				graphics.setStroke(SYMBOL_BORDER);
				graphics.drawOval(rect.x, rect.y, rect.width - 1, rect.height - 1);
				graphics.setFont(SMALL);
				final int bangW = graphics.getFontMetrics().stringWidth("!");
				graphics.drawString("!", sx - bangW / 2, sy + 4);
			}

			if (isHovered)
			{
				graphics.setFont(SMALL);
				final String label = cluster.name;
				final int textWidth = graphics.getFontMetrics().stringWidth(label);
				final int textX = sx - textWidth / 2;
				final int textY = rect.y + rect.height + 13;
				graphics.setColor(LAYER_LABEL_BG);
				graphics.fillRoundRect(textX - 4, textY - 11, textWidth + 8, 14, 5, 5);
				graphics.setColor(CARD_TITLE);
				graphics.drawString(label, textX, textY);
			}
		}
		targets.addAll(drawn);
	}

	/** Markers other plugins (and ours) registered, drawn through our transform. */
	void drawWorldMapPoints(Graphics2D graphics, Rectangle bounds, List<Rectangle> placed)
	{
		if (!config.showPluginMarkers())
		{
			return;
		}

		final int plane = camera.getPlane();
		final ViewWindow view = viewWindow(bounds, 10.0);

		for (WorldMapPoint point : worldMapPointReader.points(worldMapPointManager))
		{
			// Our own clue diamond already marks this tile; skip the clue plugin's world map point
			// so map and coordinate clues do not show two markers on top of each other.
			if (config.showClueScroll() && isClueWorldMapPoint(point))
			{
				continue;
			}

			final BufferedImage image = point.getImage();
			final WorldPoint location = point.getWorldPoint();
			if (image == null || location == null || location.getPlane() != plane)
			{
				continue;
			}

			final int lx = location.getX();
			final int ly = location.getY();
			if (!camera.inView(lx, ly, view.minX, view.maxX, view.minY, view.maxY) || !drawable(lx, ly))
			{
				continue;
			}

			int x = (int) Math.round(camera.screenX(location.getX() + 0.5, location.getY() + 0.5, bounds));
			int y = (int) Math.round(camera.screenY(location.getX() + 0.5, location.getY() + 0.5, bounds));

			if (point.getImagePoint() == null)
			{
				x -= image.getWidth() / 2;
				y -= image.getHeight() / 2;
			}
			else
			{
				x -= point.getImagePoint().getX();
				y -= point.getImagePoint().getY();
			}

			final Rectangle rect = new Rectangle(x, y, image.getWidth(), image.getHeight());

			if (!bounds.intersects(rect))
			{
				continue;
			}

			if (overlapsPlaced(placed, rect))
			{
				continue;
			}

			graphics.drawImage(image, rect.x, rect.y, null);
			placed.add(rect);
		}
	}

	/** Hard-coded boss markers from {@link MonsterLocationData}, drawn with their boss icons and pins. */
	void drawMonsters(Graphics2D graphics, Rectangle bounds, List<Rectangle> placed)
	{
		if (!config.showBossLocations())
		{
			return;
		}

		final int plane = camera.getPlane();
		final ViewWindow view = viewWindow(bounds, 5.0);

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
			if (!camera.inView(mx, my, view.minX, view.maxX, view.minY, view.maxY) || !drawable(mx, my))
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
			stats.markersDrawn++;

			if (drawLabels)
			{
				final String label = (isTaskBoss ? "⚔ 👑 " : "👑 ") + monster.getName();
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

	/** Bosses and slayer targets, with details on hover and item sprites on the map. */
	void drawMonsterZones(Graphics2D graphics, Rectangle bounds, List<Rectangle> placed)
	{
		if (!config.showMonsterZones() || !monsterIndex.isLoaded())
		{
			return;
		}

		final int plane = camera.getPlane();
		final ViewWindow view = viewWindow(bounds, 8.0);

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

		// 2. Monster / slayer icons at zone centroids
		if (!config.showMonsterIcons())
		{
			return;
		}

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

			if (overlapsPlaced(placed, rect))
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
			stats.markersDrawn++;

			if (drawLabels || (isTaskZone && zoom >= 0.65))
			{
				final String label = (isTaskZone ? "⚔ " : "") + zone.getMonster() + (zone.getCombatLevel() > 0 ? " (" + zone.getCombatLevel() + ")" : "");
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

	/**
	 * Wiki ground item spawn tiles, drawn as the item's own sprite.
	 *
	 * <p>Hidden until the camera is close in — 4,361 tiles at world zoom would be a rash of dots
	 * over every town — and filtered by the min-value slider, which defaults to showing everything.
	 * Shares {@code placed} with the monster and boss markers so a bucket never covers a boss.
	 */
	void drawGroundItems(Graphics2D graphics, Rectangle bounds, List<Rectangle> placed)
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
		final ViewWindow view = viewWindow(bounds, 4.0);

		final int minValue = config.groundItemMinValue();
		final boolean drawLabels = config.showGroundItemLabels() && zoom >= 14.0;
		final int chipSize = 16;

		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		final Stroke prevStroke = graphics.getStroke();

		groundItemIndex.forEachInArea(plane, view.minXi, view.maxXi, view.minYi, view.maxYi, spawn ->
		{
			if (spawn.getValue() < minValue || !drawable(spawn.getX(), spawn.getY()))
			{
				return;
			}

			final int sx = (int) Math.round(camera.screenX(spawn.getX() + 0.5, spawn.getY() + 0.5, bounds));
			final int sy = (int) Math.round(camera.screenY(spawn.getX() + 0.5, spawn.getY() + 0.5, bounds));

			final Rectangle rect = new Rectangle(sx - chipSize / 2, sy - chipSize / 2, chipSize, chipSize);
			if (!bounds.intersects(rect) || overlapsPlaced(placed, rect))
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

	/** Small coloured pins at every travel station so routes are discoverable before hover. */
	void drawTravelStations(Graphics2D graphics, Rectangle bounds)
	{
		if (!config.showTravelRoutes())
		{
			return;
		}

		final int plane = camera.getPlane();
		final ViewWindow view = viewWindow(bounds, 5.0);

		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		for (TravelData.TravelNode node : TravelData.ALL_NODES)
		{
			final WorldPoint loc = node.getLocation();
			if (!TravelData.matchesPlane(node, plane))
			{
				continue;
			}

			final int mx = loc.getX();
			final int my = loc.getY();
			if (!camera.inView(mx, my, view.minX, view.maxX, view.minY, view.maxY) || !drawable(mx, my))
			{
				continue;
			}

			final int x = (int) camera.screenX(mx + 0.5, my + 0.5, bounds);
			final int y = (int) camera.screenY(mx + 0.5, my + 0.5, bounds);
			if (!bounds.contains(x, y))
			{
				continue;
			}

			final Color theme = node.getType().getPinColor();
			graphics.setColor(theme);
			graphics.fillOval(x - 4, y - 4, 9, 9);
			graphics.setColor(ROUTE_SHADOW);
			graphics.drawOval(x - 4, y - 4, 9, 9);
		}
	}

	/**
	 * Draws route lines, destination beacons, and price badges for the currently hovered travel station.
	 */
	void drawTravelRoutes(Graphics2D graphics, Rectangle bounds)
	{
		if (!config.showTravelRoutes() || camera.getHoveredTravelNode() == null)
		{
			return;
		}

		final TravelData.TravelNode node = camera.getHoveredTravelNode();
		final TravelData.TravelType type = node.getType();
		final Color theme = type.getPrimaryColor();
		final Color highlight = type.getHighlightColor();

		final Stroke origStroke = graphics.getStroke();
		final Color origColor = graphics.getColor();
		final Font origFont = graphics.getFont();
		final java.awt.Composite origComp = graphics.getComposite();

		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		final WorldPoint originLoc = node.getLocation();
		final double ox = camera.screenX(originLoc.getX() + 0.5, originLoc.getY() + 0.5, bounds);
		final double oy = camera.screenY(originLoc.getX() + 0.5, originLoc.getY() + 0.5, bounds);

		final int margin = 400;
		final Rectangle extendedBounds = new Rectangle(
			bounds.x - margin, bounds.y - margin,
			bounds.width + margin * 2, bounds.height + margin * 2);

		final boolean animate = config.animateTravelRoutes();
		final Stroke haloStroke = ROUTE_HALO_STROKE;
		final Color haloColor = new Color(theme.getRed(), theme.getGreen(), theme.getBlue(), 60);
		final Stroke lineStroke = animate
			? TRAVEL_DASH_STROKES[(int) ((System.currentTimeMillis() / 40L) % TRAVEL_DASH_PHASES)]
			: ROUTE_LINE_STROKE;

		final float pulse = animate
			? (float) (0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 180.0))
			: 0.5f;

		// 1. Draw route arcs
		for (TravelData.TravelDestination dest : node.getDestinations())
		{
			final WorldPoint destLoc = dest.getLocation();
			if (TravelData.sameLocation(originLoc, destLoc) || hiddenTravelPoint(destLoc))
			{
				continue;
			}

			final double dx = camera.screenX(destLoc.getX() + 0.5, destLoc.getY() + 0.5, bounds);
			final double dy = camera.screenY(destLoc.getX() + 0.5, destLoc.getY() + 0.5, bounds);

			final double midX = (ox + dx) / 2.0;
			final double midY = (oy + dy) / 2.0;
			final double vx = dx - ox;
			final double vy = dy - oy;
			final double cx = midX - vy * 0.18;
			final double cy = midY + vx * 0.18;

			final QuadCurve2D.Double curve = new QuadCurve2D.Double(ox, oy, cx, cy, dx, dy);

			if (!extendedBounds.contains(ox, oy)
				&& !extendedBounds.contains(dx, dy)
				&& !extendedBounds.intersectsLine(ox, oy, dx, dy)
				&& !curve.intersects(extendedBounds.x, extendedBounds.y, extendedBounds.width, extendedBounds.height))
			{
				continue;
			}

			// Soft halo
			graphics.setColor(haloColor);
			graphics.setStroke(haloStroke);
			graphics.draw(curve);

			// Route line
			graphics.setColor(highlight);
			graphics.setStroke(lineStroke);
			graphics.draw(curve);
		}

		// 2. Draw destination beacons
		final float r1 = 6.0f + 4.0f * pulse;
		final float r2 = 9.0f + 5.0f * pulse;
		final int a1 = Math.max(10, Math.min(255, (int) (140 - 50 * pulse)));
		final int a2 = Math.max(10, Math.min(255, (int) (80 - 45 * pulse)));

		for (TravelData.TravelDestination dest : node.getDestinations())
		{
			final WorldPoint destLoc = dest.getLocation();
			if (TravelData.sameLocation(originLoc, destLoc) || hiddenTravelPoint(destLoc))
			{
				continue;
			}

			final double dx = camera.screenX(destLoc.getX() + 0.5, destLoc.getY() + 0.5, bounds);
			final double dy = camera.screenY(destLoc.getX() + 0.5, destLoc.getY() + 0.5, bounds);

			if (!extendedBounds.contains(dx, dy))
			{
				continue;
			}

			// Expanding rings
			graphics.setStroke(LABEL_BORDER);
			graphics.setColor(new Color(theme.getRed(), theme.getGreen(), theme.getBlue(), a1));
			graphics.draw(new Ellipse2D.Double(dx - r1, dy - r1, 2 * r1, 2 * r1));
			graphics.setColor(new Color(theme.getRed(), theme.getGreen(), theme.getBlue(), a2));
			graphics.draw(new Ellipse2D.Double(dx - r2, dy - r2, 2 * r2, 2 * r2));

			// Centre pin
			final int px = (int) Math.round(dx);
			final int py = (int) Math.round(dy);
			graphics.setColor(highlight);
			graphics.fillOval(px - 4, py - 4, 8, 8);
			graphics.setColor(ROUTE_OUTLINE);
			graphics.setStroke(MARKER_OUTLINE);
			graphics.drawOval(px - 4, py - 4, 8, 8);
		}

		// 3. Draw price badges
		if (config.showTravelPriceTags())
		{
			graphics.setFont(SMALL);
			final FontMetrics fm = graphics.getFontMetrics();
			final List<Rectangle> placedBadges = new ArrayList<>();

			for (TravelData.TravelDestination dest : node.getDestinations())
			{
				final WorldPoint destLoc = dest.getLocation();
				if (TravelData.sameLocation(originLoc, destLoc) || hiddenTravelPoint(destLoc))
				{
					continue;
				}

				final double dx = camera.screenX(destLoc.getX() + 0.5, destLoc.getY() + 0.5, bounds);
				final double dy = camera.screenY(destLoc.getX() + 0.5, destLoc.getY() + 0.5, bounds);

				final double midX = (ox + dx) / 2.0;
				final double midY = (oy + dy) / 2.0;
				final double vx = dx - ox;
				final double vy = dy - oy;
				final double cx = midX - vy * 0.18;
				final double cy = midY + vx * 0.18;
				final QuadCurve2D.Double curve = new QuadCurve2D.Double(ox, oy, cx, cy, dx, dy);

				if (!extendedBounds.contains(ox, oy)
					&& !extendedBounds.contains(dx, dy)
					&& !extendedBounds.intersectsLine(ox, oy, dx, dy)
					&& !curve.intersects(extendedBounds.x, extendedBounds.y, extendedBounds.width, extendedBounds.height))
				{
					continue;
				}

				final int px = (int) Math.round(dx);
				final int py = (int) Math.round(dy);

				if (!bounds.contains(px, py) && !bounds.intersects(new Rectangle(px - 15, py - 15, 30, 30)))
				{
					continue;
				}

				final String cost = dest.getCost() != null ? dest.getCost() : "";
				final String badgeText = cost.isEmpty() ? dest.getName() : dest.getName() + " • " + cost;
				final int textW = fm.stringWidth(badgeText);
				final int padX = 6;
				final int padY = 2;
				final int pillW = textW + padX * 2;
				final int pillH = fm.getHeight() + padY * 2;

				int pillX = px + 10;
				int pillY = py - 4 - pillH / 2;

				if (pillX + pillW > bounds.getMaxX() - 4)
				{
					pillX = px - 10 - pillW;
				}
				if (pillX < bounds.getMinX() + 4)
				{
					pillX = (int) bounds.getMinX() + 4;
				}
				if (pillY < bounds.getMinY() + 4)
				{
					pillY = (int) bounds.getMinY() + 4;
				}
				else if (pillY + pillH > bounds.getMaxY() - 4)
				{
					pillY = (int) bounds.getMaxY() - pillH - 4;
				}

				final Rectangle pillRect = new Rectangle(pillX, pillY, pillW, pillH);
				if (overlapsPlaced(placedBadges, pillRect))
				{
					continue;
				}

				placedBadges.add(pillRect);

				graphics.setColor(CARD_BG);
				graphics.fillRoundRect(pillRect.x, pillRect.y, pillRect.width, pillRect.height, 6, 6);
				graphics.setColor(theme);
				graphics.setStroke(MARKER_OUTLINE);
				graphics.drawRoundRect(pillRect.x, pillRect.y, pillRect.width, pillRect.height, 6, 6);
				graphics.setColor(CARD_TEXT);
				graphics.drawString(badgeText, pillRect.x + padX, pillRect.y + padY + fm.getAscent());
			}
		}

		graphics.setStroke(origStroke);
		graphics.setColor(origColor);
		graphics.setFont(origFont);
		graphics.setComposite(origComp);
	}

	private boolean hiddenTravelPoint(WorldPoint loc)
	{
		return loc == null || !drawable(loc.getX(), loc.getY());
	}

	/**
	 * Subtle nautical pins for all Sailing ports across Gielinor.
	 */
	void drawSailingPorts(Graphics2D graphics, Rectangle bounds, List<Rectangle> placed)
	{
		if (!config.showSailingPorts())
		{
			return;
		}

		final int plane = camera.getPlane();
		if (plane != 0)
		{
			return;
		}

		final ViewWindow view = viewWindow(bounds, 10.0);

		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		final Stroke oldStroke = graphics.getStroke();

		for (SailingPort port : PORTS)
		{
			final WorldPoint loc = port.getNavigationLocation();
			if (loc == null || loc.getPlane() != plane)
			{
				continue;
			}

			final int px = loc.getX();
			final int py = loc.getY();
			if (!camera.inView(px, py, view.minX, view.maxX, view.minY, view.maxY) || !drawable(px, py))
			{
				continue;
			}

			final int sx = (int) Math.round(camera.screenX(px + 0.5, py + 0.5, bounds));
			final int sy = (int) Math.round(camera.screenY(px + 0.5, py + 0.5, bounds));

			final int size = 12;
			final Rectangle rect = new Rectangle(sx - size / 2, sy - size / 2, size, size);
			if (!bounds.intersects(rect))
			{
				continue;
			}

			final boolean hasDockedBoat = boatTracker != null && !boatTracker.getBoatsAt(port).isEmpty() && config.showBoatLocations();
			if (!hasDockedBoat)
			{
				graphics.setColor(PORT_EDGE);
				graphics.fillOval(rect.x - 1, rect.y - 1, rect.width + 2, rect.height + 2);

				graphics.setColor(PORT_FILL);
				graphics.fillOval(rect.x, rect.y, rect.width, rect.height);

				graphics.setColor(PORT_BORDER);
				graphics.setStroke(LABEL_BORDER);
				graphics.drawOval(rect.x, rect.y, rect.width, rect.height);

				graphics.setColor(PORT_DOT);
				graphics.fillOval(sx - 2, sy - 2, 4, 4);

				stats.markersDrawn++;
			}
		}

		graphics.setStroke(oldStroke);
	}

	/**
	 * Distinct port notice board markers when zoomed reasonably in (zoom >= 0.8).
	 */
	void drawPortNoticeBoards(Graphics2D graphics, Rectangle bounds, List<Rectangle> placed)
	{
		if (!config.showPortNoticeBoards() || camera.getZoom() < 0.8)
		{
			return;
		}

		final int plane = camera.getPlane();
		if (plane != 0)
		{
			return;
		}

		final ViewWindow view = viewWindow(bounds, 10.0);

		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		final Stroke oldStroke = graphics.getStroke();

		for (PortNoticeBoard board : PortNoticeBoard.ALL_BOARDS)
		{
			final WorldPoint loc = board.getLocation();
			if (loc == null || loc.getPlane() != plane)
			{
				continue;
			}

			final int bx = loc.getX();
			final int by = loc.getY();
			if (!camera.inView(bx, by, view.minX, view.maxX, view.minY, view.maxY) || !drawable(bx, by))
			{
				continue;
			}

			final int sx = (int) Math.round(camera.screenX(bx + 0.5, by + 0.5, bounds));
			final int sy = (int) Math.round(camera.screenY(bx + 0.5, by + 0.5, bounds));

			final int size = 12;
			final Rectangle rect = new Rectangle(sx - size / 2, sy - size / 2, size, size);
			if (!bounds.intersects(rect))
			{
				continue;
			}

			graphics.setColor(BOARD_EDGE);
			graphics.fillRoundRect(rect.x - 1, rect.y - 1, rect.width + 2, rect.height + 2, 4, 4);

			graphics.setColor(BOARD_FILL);
			graphics.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 4, 4);

			graphics.setColor(BOARD_BORDER);
			graphics.setStroke(LABEL_BORDER);
			graphics.drawRoundRect(rect.x, rect.y, rect.width, rect.height, 4, 4);

			graphics.setColor(BOARD_GLYPH);
			graphics.drawLine(sx - 3, sy - 2, sx + 3, sy - 2);
			graphics.drawLine(sx - 3, sy, sx + 3, sy);
			graphics.drawLine(sx - 3, sy + 2, sx + 1, sy + 2);

			stats.markersDrawn++;
		}

		graphics.setStroke(oldStroke);
	}

	/**
	 * Player boats docked across Gielinor ports, drawn with crisp nautical pins and health badges.
	 */
	void drawPlayerBoats(Graphics2D graphics, Rectangle bounds, List<Rectangle> placed)
	{
		if (!config.showBoatLocations() || boatTracker == null)
		{
			return;
		}

		final int plane = camera.getPlane();
		final List<PlayerBoat> owned = boatTracker.getOwnedBoats();
		if (owned.isEmpty())
		{
			return;
		}

		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		final Stroke oldStroke = graphics.getStroke();

		// Group/offset multiple boats docked at the same port so none are obscured
		final Map<SailingPort, Integer> portCounts = new HashMap<>();

		for (PlayerBoat boat : owned)
		{
			final SailingPort port = boat.getPort();
			if (port == null)
			{
				continue;
			}

			final WorldPoint loc = port.getNavigationLocation();
			if (loc == null || loc.getPlane() != plane || !drawable(loc.getX(), loc.getY()))
			{
				continue;
			}

			final int count = portCounts.getOrDefault(port, 0);
			portCounts.put(port, count + 1);

			final int offsetX = count * 9;
			final int offsetY = count * 9;

			final int sx = (int) Math.round(camera.screenX(loc.getX() + 0.5, loc.getY() + 0.5, bounds)) + offsetX;
			final int sy = (int) Math.round(camera.screenY(loc.getX() + 0.5, loc.getY() + 0.5, bounds)) + offsetY;

			final BufferedImage icon = boatIcon(boat.getBoatType());
			final int iconW = icon != null ? icon.getWidth() : 18;
			final int iconH = icon != null ? icon.getHeight() : 18;
			final Rectangle rect = new Rectangle(sx - iconW / 2, sy - iconH / 2, iconW, iconH);
			if (!bounds.intersects(rect))
			{
				continue;
			}

			if (icon != null)
			{
				graphics.drawImage(icon, rect.x, rect.y, null);
			}
			else
			{
				// Fallback if boat icon resource fails to load
				graphics.setColor(BOAT_EDGE);
				graphics.fillOval(rect.x - 1, rect.y - 1, rect.width + 2, rect.height + 2);
				graphics.setColor(BOAT_FILL);
				graphics.fillOval(rect.x, rect.y, rect.width, rect.height);
				graphics.setColor(BOAT_BORDER);
				graphics.setStroke(MARKER_RING);
				graphics.drawOval(rect.x, rect.y, rect.width, rect.height);
			}

			// Health condition ring accent (top-right pip)
			final Color healthColor = boat.getHealth() < 0 ? BOAT_HULL_GOOD
				: (boat.getHealth() >= 0.75f ? BOAT_HULL_GOOD : (boat.getHealth() >= 0.35f ? BOAT_HULL_WARN : BOAT_HULL_BAD));
			graphics.setColor(healthColor);
			graphics.fillOval(rect.x + rect.width - 5, rect.y - 1, 6, 6);
			graphics.setColor(BOAT_EDGE);
			graphics.setStroke(MARKER_OUTLINE);
			graphics.drawOval(rect.x + rect.width - 5, rect.y - 1, 6, 6);

			// Boat glyph / initials or number
			if (icon == null)
			{
				graphics.setFont(SMALL);
				final String numStr = String.valueOf(boat.getBoatId());
				final int numW = graphics.getFontMetrics().stringWidth(numStr);
				graphics.setColor(Color.WHITE);
				graphics.drawString(numStr, sx - numW / 2, sy + 4);
			}
			else if (owned.size() > 1)
			{
				final String numStr = String.valueOf(boat.getBoatId());
				final int badgeSize = 10;
				final int bx = rect.x + rect.width - 3;
				final int by = rect.y + rect.height - 3;
				graphics.setColor(BOAT_EDGE);
				graphics.fillOval(bx - badgeSize / 2, by - badgeSize / 2, badgeSize, badgeSize);
				graphics.setColor(BOAT_BORDER);
				graphics.setStroke(MARKER_OUTLINE);
				graphics.drawOval(bx - badgeSize / 2, by - badgeSize / 2, badgeSize, badgeSize);
				graphics.setFont(TINY);
				final int numW = graphics.getFontMetrics().stringWidth(numStr);
				graphics.setColor(Color.WHITE);
				graphics.drawString(numStr, bx - numW / 2, by + 3);
			}

			placed.add(rect);
			stats.markersDrawn++;

			// Optional Name label chip below marker
			if (config.showBoatNames() && camera.getZoom() >= 0.65)
			{
				final String label = "⛵ " + boat.getBoatName();
				graphics.setFont(SMALL);
				final int textW = graphics.getFontMetrics().stringWidth(label);
				final int textX = sx - textW / 2;
				final int textY = rect.y + iconH + 12;

				final Rectangle labelRect = new Rectangle(textX - 4, textY - 10, textW + 8, 13);
				if (bounds.intersects(labelRect))
				{
					graphics.setColor(CARD_BG);
					graphics.fillRoundRect(labelRect.x, labelRect.y, labelRect.width, labelRect.height, 4, 4);
					graphics.setColor(BOAT_BORDER);
					graphics.setStroke(MARKER_OUTLINE);
					graphics.drawRoundRect(labelRect.x, labelRect.y, labelRect.width, labelRect.height, 4, 4);
					graphics.setColor(CARD_TITLE);
					graphics.drawString(label, textX, textY);
				}
			}
		}

		graphics.setStroke(oldStroke);
	}

	/** Place the arrow with the same per-piece translation/rotation/flip used for dungeon tiles. */
	private Point2D arrangedPlayerPoint(WorldPoint rawLocation, Rectangle bounds)
	{
		final UndergroundZone zone = camera.getActiveUndergroundZone() != null
			? camera.getActiveUndergroundZone()
			: camera.getObservedPlayerUndergroundZone();
		if (zone == null || dungeonPieceIndex == null)
		{
			return null;
		}
		final DungeonPiece piece = dungeonPieceIndex.pieceAt(
			zone.getId(), rawLocation.getX(), rawLocation.getY(), rawLocation.getPlane(),
			camera.floorLayerFor(zone));
		if (piece == null)
		{
			return null;
		}

		final int nudgeX = camera.getDungeonTuner().offsetX(zone);
		final int nudgeY = camera.getDungeonTuner().offsetY(zone);
		final int[] sourceBounds = piece.srcBounds();
		final double centerWorldX = (sourceBounds[0] + sourceBounds[2] + 1) / 2.0 + piece.dx - nudgeX;
		final double centerWorldY = (sourceBounds[1] + sourceBounds[3] + 1) / 2.0 + piece.dy - nudgeY;
		final AffineTransform transform = DungeonPieceTransform.affine(
			piece.rot, piece.flipX, piece.flipY,
			camera.screenX(centerWorldX, bounds), camera.screenY(centerWorldY, bounds));
		final Point2D point = new Point2D.Double(
			camera.screenX(rawLocation.getX() + 0.5 + piece.dx - nudgeX, bounds),
			camera.screenY(rawLocation.getY() + 0.5 + piece.dy - nudgeY, bounds));
		return transform.transform(point, null);
	}

	void drawPlayer(Graphics2D graphics, Rectangle bounds)
	{
		if (!config.showPlayerMarker())
		{
			return;
		}

		final Player local = client.getLocalPlayer();
		final WorldPoint rawLocation = camera.getPlayerLocation();
		WorldPoint location = camera.getPlayerDisplayLocation();
		if (location == null)
		{
			location = camera.getPlayerLocation();
		}
		if (local == null || location == null)
		{
			return;
		}

		// While the player is on a layer above the overworld and the overworld is what is on screen,
		// pin the marker to the last surface spot they stood on — their cave mouth — and draw it
		// hollow. This covers an unavailable interior and the user's explicit choice to return to
		// the surface while still inside a mapped instance. An open mapped cutaway instead uses the
		// real solid player fix, transformed with its arranged dungeon piece below.
		boolean below = false;
		final boolean projectionHides = InstanceMaps.hiddenOnSurface(rawLocation.getX(), rawLocation.getY(),
			camera.getCenterX(), camera.getCenterY());
		final boolean instancedAbove = camera.isPlayerInInstance()
			&& rawLocation.getY() > InstanceMaps.GAP_MIN_Y
			&& camera.getObservedPlayerUndergroundZone() == null
			&& InstanceMaps.cameraOnOverworld(camera.getCenterY());
		if (camera.isInteriorMapUnavailable() || projectionHides || instancedAbove)
		{
			final WorldPoint surface = camera.getLastSurfaceLocation();
			if (surface == null)
			{
				return;
			}
			location = surface;
			below = true;
		}

		final Point2D arranged = below ? null : arrangedPlayerPoint(rawLocation, bounds);
		final int x = (int) (arranged != null
			? arranged.getX()
			: camera.screenX(location.getX() + 0.5, location.getY() + 0.5, bounds));
		final int y = (int) (arranged != null
			? arranged.getY()
			: camera.screenY(location.getX() + 0.5, location.getY() + 0.5, bounds));

		if (!bounds.contains(x, y))
		{
			return;
		}

		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		// net.runelite.api.coords.Angle: 0 is south, 512 west, 1024 north, 1536 east, which runs
		// clockwise on a north-up map - the same sense as Graphics2D.rotate. The arrow polygon
		// points north in its own frame, so orientation 0 needs a half turn to face south.
		final double facing = (local.getOrientation() / 2048d) * 2 * Math.PI;
		final AffineTransform previous = graphics.getTransform();
		final Stroke oldStroke = graphics.getStroke();
		graphics.translate(x, y);
		graphics.rotate(Math.PI + facing);


		graphics.setColor(PLAYER_EDGE);
		graphics.setStroke(PLAYER_ARROW_EDGE);
		graphics.drawPolygon(PLAYER_ARROW_X, PLAYER_ARROW_Y, 4);
		// Hollow while the player is on a layer below the overworld view: the arrow sits on their
		// entrance, not their real position, so it must not read as a solid fix.
		graphics.setColor(below ? PLAYER_FILL_BELOW : PLAYER_FILL);
		graphics.fillPolygon(PLAYER_ARROW_X, PLAYER_ARROW_Y, 4);

		graphics.setStroke(oldStroke);
		graphics.setTransform(previous);
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
	void drawClueMarkers(Graphics2D graphics, Rectangle bounds, List<Rectangle> placed)
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

	/**
	 * Flashes a location when centered on by search or sidebar clicks.
	 */
	void drawFlash(Graphics2D graphics, Rectangle bounds)
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