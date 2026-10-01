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

import com.bettermap.ui.markers.RaidBossDisplay;

import static com.bettermap.ui.MapStyle.CARD_BG;
import static com.bettermap.ui.MapStyle.CARD_EDGE;
import static com.bettermap.ui.MapStyle.LEFT_TOOLBAR_BUTTON_SIZE;
import static com.bettermap.ui.MapStyle.LEFT_TOOLBAR_FINDER;
import static com.bettermap.ui.MapStyle.SMALL;
import static com.bettermap.ui.MapStyle.TEXT_WARN;

import com.bettermap.BetterMapConfig;
import com.bettermap.data.OverlayFloor;
import com.bettermap.data.UndergroundZone;
import com.bettermap.data.sailing.BoatTracker;
import com.bettermap.map.ClueScrollTracker;
import com.bettermap.map.DungeonPieceIndex;
import com.bettermap.map.GroundItemIndex;
import com.bettermap.map.InstanceMaps;
import com.bettermap.map.MapCamera;
import com.bettermap.map.ShortestPathTracker;
import com.bettermap.map.MapFinder;
import com.bettermap.map.MonsterIconManager;
import com.bettermap.map.MonsterIndex;
import com.bettermap.map.PoiIndex;
import com.bettermap.map.QuestHelperTracker;
import com.bettermap.map.ShopIndex;
import com.bettermap.map.SlayerTaskTracker;
import com.bettermap.map.WorldMapInput;
import com.bettermap.map.WorldMapPointReader;
import com.bettermap.tiles.TileLoader;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.worldmap.WorldMap;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;

/**
 * Draws the whole world map: wiki tiles, point-of-interest icons, RuneLite's markers and the
 * player, all positioned by {@link MapCamera} rather than by the client.
 *
 * <p>The client's own map render is hidden while this is active, so this overlay is the map.
 */
@Slf4j
@Singleton
public class BetterWorldMapOverlay extends Overlay
{
	/**
	 * Matches the black the cache-rendered tiles themselves are padded with where the game has no
	 * map data, so areas with no tile at all blend into the same void rather than reading as
	 * holes punched in the map.
	 */
	private static final Color BACKDROP = Color.BLACK;
	/** A client-side jump this far from our centre means something focused the map elsewhere. */
	private static final double JUMP_TILES = 60;

	/** One row of the on-map "Layers" panel: a label, the {@code bettermap.*} key it writes, and how to read it. */
	static final class LayerToggle
	{
		final String label;
		final String key;
		final Predicate<BetterMapConfig> getter;

		LayerToggle(String label, String key, Predicate<BetterMapConfig> getter)
		{
			this.label = label;
			this.key = key;
			this.getter = getter;
		}
	}

	/**
	 * The curated set surfaced on the map itself. The RuneLite config panel still owns every
	 * toggle; this is just the handful players flip mid-session. Add or remove a line freely —
	 * {@code LayerTogglesTest} checks each key resolves to a real boolean config method.
	 */
	static final List<LayerToggle> LAYER_TOGGLES = Collections.unmodifiableList(Arrays.asList(
		new LayerToggle("Banks", "iconBanks", BetterMapConfig::iconBanks),
		new LayerToggle("Shops and trade", "iconShops", BetterMapConfig::iconShops),
		new LayerToggle("Skilling icons", "iconSkilling", BetterMapConfig::iconSkilling),
		new LayerToggle("Agility shortcuts", "iconShortcuts", BetterMapConfig::iconShortcuts),
		new LayerToggle("Sailing lookouts", "iconSailing", BetterMapConfig::iconSailing),
		new LayerToggle("Travel", "iconTravel", BetterMapConfig::iconTravel),
		new LayerToggle("Quests", "iconQuests", BetterMapConfig::iconQuests),
		new LayerToggle("Altars", "iconAltars", BetterMapConfig::iconAltars),
		new LayerToggle("Dungeons", "iconDungeons", BetterMapConfig::iconDungeons),
		new LayerToggle("Tutors and services", "iconServices", BetterMapConfig::iconServices),
		new LayerToggle("Other map icons", "iconOther", BetterMapConfig::iconOther),
		new LayerToggle("Clue target", "showClueScroll", BetterMapConfig::showClueScroll),
		new LayerToggle("Place names", "showPlaceNames", BetterMapConfig::showPlaceNames),
		new LayerToggle("Underground symbols", "showLargeUndergroundSymbols", BetterMapConfig::showLargeUndergroundSymbols),
		new LayerToggle("Boss markers", "showBossLocations", BetterMapConfig::showBossLocations),
		new LayerToggle("Monster areas", "showMonsterZones", BetterMapConfig::showMonsterZones),
		new LayerToggle("Monster icons", "showMonsterIcons", BetterMapConfig::showMonsterIcons),
		new LayerToggle("Monster labels", "showMonsterLabels", BetterMapConfig::showMonsterLabels),
		new LayerToggle("Ground items", "showGroundItems", BetterMapConfig::showGroundItems),
		new LayerToggle("Travel routes", "showTravelRoutes", BetterMapConfig::showTravelRoutes),
		new LayerToggle("Player boats", "showBoatLocations", BetterMapConfig::showBoatLocations),
		new LayerToggle("Sailing ports", "showSailingPorts", BetterMapConfig::showSailingPorts),
        new LayerToggle("Port notice boards", "showPortNoticeBoards", BetterMapConfig::showPortNoticeBoards),
		new LayerToggle("Other plugin markers", "showPluginMarkers", BetterMapConfig::showPluginMarkers),
		new LayerToggle("Player marker", "showPlayerMarker", BetterMapConfig::showPlayerMarker),
		new LayerToggle("Tooltips", "showTooltips", BetterMapConfig::showTooltips)
	));

	/**
	 * Sentinel key for the on-map "Hide all" master row. Not a persisted {@link BetterMapConfig}
	 * field — {@link WorldMapInput} routes clicks on this to {@link BetterMapPlugin#setAllLayersEnabled}.
	 */
	public static final String HIDE_ALL_LAYERS_KEY = "@hideAllLayers";

	/** Writes every {@link #LAYER_TOGGLES} row through {@code setter}. */
	public static void forEachLayerToggle(java.util.function.BiConsumer<String, Boolean> setter, boolean value)
	{
		for (LayerToggle toggle : LAYER_TOGGLES)
		{
			setter.accept(toggle.key, value);
		}
	}

	/** True when every {@link #LAYER_TOGGLES} row is switched off. */
	public static boolean allLayersHidden(BetterMapConfig config)
	{
		for (LayerToggle toggle : LAYER_TOGGLES)
		{
			if (toggle.getter.test(config))
			{
				return false;
			}
		}
		return true;
	}

	private final Client client;
	private final BetterMapConfig config;
	private final TileLoader tileLoader;
	private final MapCamera camera;

	private final MapLayout layout;
	private final MapRenderStats stats;
	private final MapTileRenderer tileRenderer;
	private final MapMarkerRenderer markerRenderer;
	private final MapChromeRenderer chromeRenderer;
	private final MapFinderRenderer finderRenderer;
	private final MapTooltipRenderer tooltipRenderer;
	@Inject
	private RaidBossDisplay raidBossDisplay;
	private final WorldMapInput input;
	private final MapDebugRenderer debugRenderer;
	private final WorldMapPointReader worldMapPointReader;

	private boolean renderFailureLogged;
	private Point unmappedPosition;
	private boolean wasOpen;
	private volatile Runnable finderFocusOnOpen;
	private boolean everOpened;
	private Point lastClientPosition;
	private float lastClientZoom;
	/** Frames after open where client world-map jumps are ignored (avoids south-west snap). */
	private int ignoreClientJumps;

	@Inject
	public BetterWorldMapOverlay(
		Client client,
		BetterMapConfig config,
		TileLoader tileLoader,
		MapCamera camera,
		WorldMapInput input,
		PoiIndex poiIndex,
		MonsterIndex monsterIndex,
		MonsterIconManager monsterIconManager,
		SlayerTaskTracker slayerTaskTracker,
		ClueScrollTracker clueScrollTracker,
		ShortestPathTracker shortestPathTracker,
		QuestHelperTracker questHelperTracker,
		ShopIndex shopIndex,
		GroundItemIndex groundItemIndex,
		BoatTracker boatTracker,
		WorldMapPointManager worldMapPointManager,
		WorldMapPointReader worldMapPointReader,
		MapFinder finder,
		DungeonPieceIndex dungeonPieceIndex)
	{
		this.client = client;
		this.input = input;
		this.config = config;
		this.tileLoader = tileLoader;
		this.camera = camera;
		this.worldMapPointReader = worldMapPointReader;
		this.layout = new MapLayout(client, config);
		this.stats = new MapRenderStats();
		this.tileRenderer = new MapTileRenderer(config, camera, tileLoader, stats, dungeonPieceIndex);
		this.markerRenderer = new MapMarkerRenderer(
			client, config, camera, poiIndex, shopIndex, monsterIndex, monsterIconManager, slayerTaskTracker,
			clueScrollTracker, shortestPathTracker, groundItemIndex, boatTracker, worldMapPointManager, worldMapPointReader,
			stats, dungeonPieceIndex);
		this.chromeRenderer = new MapChromeRenderer(config, camera, input, layout, clueScrollTracker,
			questHelperTracker, slayerTaskTracker, poiIndex);
		this.finderRenderer = new MapFinderRenderer(
			client, config, camera, input, poiIndex, slayerTaskTracker, finder);
		this.tooltipRenderer = new MapTooltipRenderer(
			client, config, camera, input, poiIndex, monsterIndex, dungeonPieceIndex, monsterIconManager, slayerTaskTracker,
			clueScrollTracker, shopIndex, finder, groundItemIndex, boatTracker, worldMapPointManager, worldMapPointReader);
		this.debugRenderer = new MapDebugRenderer(
			client, config, camera, tileLoader, input, poiIndex, shopIndex, groundItemIndex, boatTracker,
			worldMapPointManager, worldMapPointReader, finder, stats);
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.MANUAL);
		setPriority(PRIORITY_HIGH);
		drawAfterInterface(InterfaceID.WORLDMAP);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		final Widget map = client.getWidget(InterfaceID.Worldmap.MAP_CONTAINER);
		final WorldMap worldMap = client.getWorldMap();

		if (map == null || map.isHidden() || worldMap == null)
		{
			if (wasOpen || camera.isActive() || camera.isClosing())
			{
				log.debug("[BetterMap:overlay] map closed/unavailable  mapNull={} mapHidden={} worldMapNull={} wasActive={} closing={}",
					map == null,
					map != null && map.isHidden(),
					worldMap == null,
					camera.isActive(),
					camera.isClosing());
			}
			if (wasOpen)
			{
				onMapClosed();
			}
			camera.setActive(false);
			if (raidBossDisplay != null)
			{
				raidBossDisplay.close();
			}
			return null;
		}

		if (camera.isClosing() && raidBossDisplay != null)
		{
			raidBossDisplay.close();
		}

		final Rectangle bounds = viewport(map);
		if (bounds.width <= 0 || bounds.height <= 0)
		{
			log.debug("[BetterMap:overlay] bad bounds {} — deactivating", bounds);
			camera.setActive(false);
			if (raidBossDisplay != null)
			{
				raidBossDisplay.close();
			}
			return null;
		}

		if (!wasOpen)
		{
			log.debug("[BetterMap:overlay] map OPEN  fullscreen={} bounds={}x{} hideGameRender={}",
				config.fullscreenMap(), bounds.width, bounds.height, config.hideGameMapRender());
		}

		camera.setViewport(bounds);
		updateNativePassthrough(bounds);
		// Keep covering the native map until its interface is gone, while disabling
		// map input as soon as close is requested.
		camera.setActive(!camera.isClosing());

		syncWithClient(worldMap, bounds);
		final Runnable focus = finderFocusOnOpen;
		if (wasOpen && focus != null)
		{
			finderFocusOnOpen = null;
			focus.run();
		}

		// Freeze the camera pose for the rest of this frame. Without this, a drag or wheel event
		// on the AWT thread can move the camera between the tile-grid pass and later passes,
		// tearing the map along a seam.
		camera.beginFrame();
		worldMapPointReader.beginPass();

		final Shape clip = clipArea(bounds);
		final Shape previousClip = graphics.getClip();
		graphics.setClip(clip);

		graphics.setColor(BACKDROP);
		graphics.fill(clip);

		if (!tileLoader.hasTiles())
		{
			drawNotice(graphics, bounds,
				"NO MAP DATA",
				tileLoader.getStatus());
			chromeRenderer.drawCloseButton(graphics, bounds, true);
			graphics.setClip(previousClip);
			return null;
		}

		graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

		stats.reset();

		try
		{
			// POI badges and plugin/marker layers keep separate overlap lists so enabling one
			// layer does not cull icons from another (e.g. Quest Helper stars hiding banks).
			final List<Rectangle> placedPoiIcons = new ArrayList<>();
			final List<Rectangle> placedMarkers = new ArrayList<>();
			tileRenderer.drawTiles(graphics, bounds);
			if (!config.showTravelRoutes())
			{
				camera.setSelectedTravelNode(null);
			}
			final boolean travelFocused = config.showTravelRoutes() && camera.getSelectedTravelNode() != null;
			boolean routeDrawFailure = false;
			markerRenderer.beginFrame();
			if (travelFocused)
			{
				graphics.setColor(new Color(0, 0, 0, 28));
				graphics.fill(bounds);
				camera.setLayerSymbolTargets(Collections.emptyList());
			}
			else
			{
				routeDrawFailure = markerRenderer.drawShortestPathRoute(graphics, bounds);
				markerRenderer.drawPlaceNames(graphics, bounds);
				markerRenderer.drawWorldMapPoints(graphics, bounds, placedMarkers);
			}
			markerRenderer.drawPoiIcons(graphics, bounds, placedPoiIcons);
			if (!travelFocused)
			{
				markerRenderer.drawLargeLayerSymbols(graphics, bounds);
				markerRenderer.drawMonsters(graphics, bounds, placedMarkers);
				markerRenderer.drawMonsterZones(graphics, bounds, placedMarkers);
				markerRenderer.drawGroundItems(graphics, bounds, placedMarkers);
			}
			markerRenderer.drawTravelStations(graphics, bounds);
			markerRenderer.drawTravelRoutes(graphics, bounds);
			if (!travelFocused)
			{
				markerRenderer.drawSailingPorts(graphics, bounds, placedMarkers);
				markerRenderer.drawPortNoticeBoards(graphics, bounds, placedMarkers);
				markerRenderer.drawPlayerBoats(graphics, bounds, placedMarkers);
				markerRenderer.drawClueMarkers(graphics, bounds, placedMarkers);
				markerRenderer.drawFlash(graphics, bounds);
				markerRenderer.drawPlayer(graphics, bounds);
			}
			chromeRenderer.drawPlaneSwitcher(graphics, bounds);
			chromeRenderer.drawStatusChip(graphics, bounds);
			chromeRenderer.drawClueButton(graphics, bounds);
			chromeRenderer.drawQuestButton(graphics, bounds);
			chromeRenderer.drawPlayerButton(graphics, bounds);
			chromeRenderer.drawLayersPanel(graphics, bounds);
			chromeRenderer.drawChatButton(graphics, bounds);
			chromeRenderer.drawCluePanel(graphics, bounds);
			chromeRenderer.drawTunerReadout(graphics, bounds);
			drawFinder(graphics, bounds);
			// drawUnmappedWarning(graphics, bounds);
			if (raidBossDisplay == null || !raidBossDisplay.ownsHover(input.getCursor()))
			{
				tooltipRenderer.drawTooltip(graphics, bounds, markerRenderer);
			}
			if (raidBossDisplay != null)
			{
				raidBossDisplay.draw(graphics, bounds, input.getCursor(), tooltipRenderer);
			}
			if (routeDrawFailure)
			{
				drawNotice(graphics, bounds, "FAILED TO DRAW ROUTE");
			}
		}
		catch (RuntimeException | LinkageError e)
		{
			// Anything thrown here used to kill every later stage and the diagnostics with it,
			// leaving a half-drawn map and no explanation. Report it once and keep going.
			if (!renderFailureLogged)
			{
				renderFailureLogged = true;
				log.warn("[BetterMap] overlay stage failed; the map may be missing layers", e);
			}
		}

		if (config.debugOverlay())
		{
			debugRenderer.drawDebugPanel(graphics, bounds, unmappedPosition);
		}

		debugRenderer.logPipeline(bounds);

		graphics.setClip(previousClip);
		return null;
	}

	/**
	 * The area the map is drawn into. Normally the client's map widget, but in fullscreen mode
	 * the whole canvas, so the map is not boxed into the game's small window.
	 */
	private Rectangle viewport(Widget map)
	{
		if (config.fullscreenMap())
		{
			return new Rectangle(0, 0, client.getCanvasWidth(), client.getCanvasHeight());
		}
		return map.getBounds();
	}

	/**
	 * Our camera owns panning, but the client still drives the map when something focuses it:
	 * a search, a quest jump, or the game switching to a dungeon layer.
	 */
	private void syncWithClient(WorldMap worldMap, Rectangle bounds)
	{
		final Point position = worldMap.getWorldMapPosition();
		if (position == null)
		{
			return;
		}

		final boolean clientMoved = !position.equals(lastClientPosition);
		final float clientZoom = worldMap.getWorldMapZoom();

		unmappedPosition = (position.getX() < MapCamera.MIN_WORLD_X || position.getX() > MapCamera.MAX_WORLD_X
			|| position.getY() < MapCamera.MIN_WORLD_Y || position.getY() > MapCamera.MAX_WORLD_Y)
			? position
			: null;

		camera.followPlayerInterior();

		if (!wasOpen)
		{
			final WorldPoint rawPlayer = camera.getPlayerLocation();
			final UndergroundZone playerZone = camera.getActiveUndergroundZone();

			if (playerZone != null)
			{
				camera.setUndergroundMode(playerZone);
				camera.setPlane(rawPlayer.getPlane());
				for (OverlayFloor floor : OverlayFloor.all())
				{
					if (floor.zone == playerZone && floor.plane == rawPlayer.getPlane())
					{
						camera.setActiveFloor(floor);
						break;
					}
				}
				final WorldPoint here = camera.getPlayerDisplayLocation();
				final int centreX = here != null ? here.getX() : playerZone.getSurfacePoint().getX();
				final int centreY = here != null ? here.getY() : playerZone.getSurfacePoint().getY();
				camera.centerOn(centreX, centreY);
				final double defZoom = (double) Math.max(1, config.defaultZoom());
				camera.setZoom(clientZoom > 0 ? clientZoom : defZoom);
			}
			else if (camera.isInteriorMapUnavailable())
			{
				// followPlayerInterior kept the camera at the last known entrance (or its safe
				// default when the plugin started inside an unknown instance). Never replace that
				// with the player's raw off-map coordinates here.
				final double defZoom = (double) Math.max(1, config.defaultZoom());
				camera.setZoom(clientZoom > 0 ? clientZoom : defZoom);
			}
			else if (!config.rememberMapPosition() || !everOpened)
			{
				final WorldPoint here = camera.getPlayerDisplayLocation();
				final int centreX = here != null ? here.getX() : position.getX();
				final int centreY = here != null ? here.getY() : position.getY();

				camera.centerOn(centreX, centreY);
				final double defZoom = (double) Math.max(1, config.defaultZoom());
				camera.setZoom(clientZoom > 0 ? clientZoom : defZoom);
			}

			wasOpen = true;
			everOpened = true;
			// Overlay does not render while WORLDMAP is closed, so wasOpen can go stale.
			// Ignore client jumps for a few frames while the vanilla map camera settles.
			ignoreClientJumps = 8;

			log.debug("[BetterMap] map opened, centred on {},{} ({}) resolved={} instance={}",
				(int) camera.getCenterX(), (int) camera.getCenterY(),
				rawPlayer != null ? "player" : "client map position", rawPlayer,
				client.getTopLevelWorldView() != null && client.getTopLevelWorldView().isInstance());
		}
		else if (ignoreClientJumps > 0)
		{
			ignoreClientJumps--;
		}
		else if (clientMoved && isPlausibleMapPosition(position) && distanceFromCamera(position) > JUMP_TILES)
		{
			if (camera.getActiveUndergroundZone() == null && position.getY() <= InstanceMaps.GAP_MIN_Y)
			{
				camera.centerOn(position.getX(), position.getY());
			}
		}
		else if (clientZoom > 0 && lastClientZoom > 0 && clientZoom != lastClientZoom)
		{
			// The game's own zoom buttons still work: apply the same ratio to our zoom.
			camera.setZoom(camera.getZoom() * (clientZoom / lastClientZoom));
		}

		final WorldPoint player = camera.getPlayerLocation();
		if (camera.getActiveUndergroundZone() == null)
		{
			camera.setPlaneFromPlayer(player == null ? 0 : player.getPlane());
		}

		lastClientPosition = position;
		lastClientZoom = clientZoom;
	}

	private double distanceFromCamera(Point position)
	{
		final double dx = position.getX() - camera.getCenterX();
		final double dy = position.getY() - camera.getCenterY();
		return Math.sqrt(dx * dx + dy * dy);
	}

	/** Apply a Finder destination after the initial player recenter on map opening. */
	public void setFinderFocusOnOpen(Runnable focus)
	{
		finderFocusOnOpen = focus;
	}

	/**
	 * Called from {@link com.bettermap.BetterMapPlugin#closeMap()} because this overlay
	 * only renders while {@code InterfaceID.WORLDMAP} is open — {@code wasOpen} would otherwise
	 * stay true across close/reopen and skip player recentering (map stuck at the bottom).
	 */
	public void onMapClosed()
	{
		wasOpen = false;
		ignoreClientJumps = 0;
		lastClientPosition = null;
		lastClientZoom = 0;
		camera.resetPlayerInteriorFollow();
		log.debug("[BetterMap:overlay] onMapClosed — wasOpen cleared for next open");
	}

	/** Rejects the uninitialised / south-west client map camera that causes the bottom jump. */
	private static boolean isPlausibleMapPosition(Point position)
	{
		final int x = position.getX();
		final int y = position.getY();
		if (x < MapCamera.MIN_WORLD_X || x > MapCamera.MAX_WORLD_X
			|| y < MapCamera.MIN_WORLD_Y || y > MapCamera.MAX_WORLD_Y)
		{
			return false;
		}
		// Fresh interface often reports near-origin before scripts place the camera.
		return x > 100 || y > 100;
	}

	/**
	 * Whether a POI badge is drawn for this point of interest, given the icon-category config.
	 *
	 * <p>Kept here because {@link com.bettermap.BetterMapPlugin} and the sidebar ask the overlay;
	 * the filter itself lives with the icon drawing in {@link MapMarkerRenderer}.
	 */
	public boolean shouldDrawPoiIcon(PoiIndex.Poi poi)
	{
		return markerRenderer.shouldDrawPoiIcon(poi);
	}

	/**
	 * A magnifier button top-left (below the status chip) and, when expanded, a searchable
	 * location finder card. Chars are captured by {@code WorldMapInput} and results queried
	 * against {@link MapFinder}. Submenus open to the right so they do not cover the map center.
	 */
	private void drawFinder(Graphics2D graphics, Rectangle bounds)
	{
		final int size = LEFT_TOOLBAR_BUTTON_SIZE;
		// Fullscreen shows a permanent search bar instead of a magnifier button.
		final boolean fullscreen = config.fullscreenMap();
		finderRenderer.drawFinder(graphics, bounds, new Rectangle(
			layout.leftToolbarButtonX(bounds, LEFT_TOOLBAR_FINDER), layout.leftToolbarTop(bounds), size, size),
			!fullscreen, fullscreen);
	}

	/**
	 * Draws the finder card on its own, with the world map closed, hanging off the minimap's search
	 * button instead of the map toolbar.
	 *
	 * <p>Same card, same state, same hit rects - the panel only ever knew where to hang itself from
	 * and where it was allowed to spill to, so handing it a different anchor is all standalone mode
	 * needs. {@code drawButton} is false because the minimap draws its own magnifier; this call
	 * must not publish a second one on top of the map that is not there.
	 *
	 * @param anchor the button the card hangs below, in canvas coordinates
	 * @param bounds the area the card must stay inside, usually the whole canvas
	 */
	public void drawStandaloneFinder(Graphics2D graphics, Rectangle anchor, Rectangle bounds)
	{
		finderRenderer.drawFinder(graphics, bounds, anchor, false);
	}

	/**
	 * Says so when the player is somewhere with no published map, rather than letting the
	 * surface fallback look like their actual surroundings.
	 */
	private void drawUnmappedWarning(Graphics2D graphics, Rectangle bounds)
	{
		final Point where = unmappedPosition;
		if (where == null)
		{
			return;
		}

		final String text = "Location (" + where.getX() + ", " + where.getY() + ") is outside world bounds";

		graphics.setFont(SMALL);
		final int width = graphics.getFontMetrics().stringWidth(text);
		final int x = (int) bounds.getCenterX() - (width + 14) / 2;
		final int y = (int) bounds.getMinY() + 34;

		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setColor(CARD_BG);
		graphics.fillRoundRect(x, y, width + 14, 20, 7, 7);
		graphics.setColor(CARD_EDGE);
		graphics.drawRoundRect(x, y, width + 14, 20, 7, 7);
		graphics.setColor(TEXT_WARN);
		graphics.drawString(text, x + 7, y + 14);
	}

	private void drawNotice(Graphics2D graphics, Rectangle bounds, String... lines)
	{
		graphics.setFont(SMALL);
		graphics.setColor(TEXT_WARN);

		final List<String> wrapped = new ArrayList<>();
		for (String line : lines)
		{
			String row = "";
			for (String word : line.split(" "))
			{
				String next = row.isEmpty() ? word : row + " " + word;
				if (!row.isEmpty() && graphics.getFontMetrics().stringWidth(next) > Math.max(80, bounds.width - 40))
				{
					wrapped.add(row);
					row = word;
				}
				else
				{
					row = next;
				}
			}
			wrapped.add(row);
		}
		int y = (int) bounds.getCenterY() - (wrapped.size() * 16) / 2;
		for (String line : wrapped)
		{
			final int width = graphics.getFontMetrics().stringWidth(line);
			graphics.drawString(line, (int) bounds.getCenterX() - width / 2, y);
			y += 16;
		}
	}

	/** Map area minus the overview inset and map list, matching the client's own clipping. */
	private Shape clipArea(Rectangle rect)
	{
		if (config.fullscreenMap())
		{
			return rect;
		}

		final Widget overview = client.getWidget(InterfaceID.Worldmap.OVERVIEW_CONTAINER);
		final Widget mapList = client.getWidget(InterfaceID.Worldmap.MAPLIST_BOX_GRAPHIC0);

		final Area area = new Area(rect);
		boolean subtracted = false;

		if (overview != null && !overview.isHidden())
		{
			area.subtract(new Area(overview.getBounds()));
			subtracted = true;
		}

		if (mapList != null && !mapList.isHidden())
		{
			area.subtract(new Area(mapList.getBounds()));
			subtracted = true;
		}

		return subtracted ? area : rect;
	}

	/**
	 * Publishes screen rects where {@link WorldMapInput} must not grab clicks, so the client's
	 * overview minimap and world-map orb still receive them (toggle/close).
	 */
	private void updateNativePassthrough(Rectangle bounds)
	{
		final List<Rectangle> rects = new ArrayList<>();

		Widget overview = client.getWidget(InterfaceID.Worldmap.OVERVIEW_CONTAINER);
		if (overview == null || overview.isHidden())
		{
			overview = client.getWidget(InterfaceID.Worldmap.OVERVIEW_DISPLAY);
		}
		if (overview != null && !overview.isHidden())
		{
			final Rectangle overviewBounds = overview.getBounds();
			if (overviewBounds != null && overviewBounds.width > 0 && overviewBounds.height > 0)
			{
				rects.add(overviewBounds.intersection(bounds));
			}
		}

		final Rectangle worldMapOrb = MinimapOrbs.worldMapOrbBounds(client);
		if (worldMapOrb != null)
		{
			rects.add(worldMapOrb);
		}

		camera.setNativePassthrough(rects.toArray(new Rectangle[0]));
	}
}
