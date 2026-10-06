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
package atlasofgielinor.ui;

import static atlasofgielinor.ui.MapStyle.LEFT_TOOLBAR_BUTTON_SIZE;
import static atlasofgielinor.ui.MapStyle.LEFT_TOOLBAR_FINDER;
import static atlasofgielinor.ui.MapStyle.SMALL;
import static atlasofgielinor.ui.MapStyle.TEXT_WARN;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import javax.inject.Inject;
import javax.inject.Singleton;

import atlasofgielinor.AtlasOfGielinorConfig;
import atlasofgielinor.data.dungeons.DungeonFloor;
import atlasofgielinor.data.dungeons.UndergroundZone;
import atlasofgielinor.data.sailing.BoatTracker;
import atlasofgielinor.integrations.ClueScrollTracker;
import atlasofgielinor.map.DungeonPieceIndex;
import atlasofgielinor.map.catalog.GroundItemIndex;
import atlasofgielinor.map.InstanceMaps;
import atlasofgielinor.map.MapCamera;
import atlasofgielinor.map.MapCatalogLoader;
import atlasofgielinor.map.MapFinder;
import atlasofgielinor.map.MonsterIconManager;
import atlasofgielinor.map.catalog.MonsterIndex;
import atlasofgielinor.map.catalog.PoiIndex;
import atlasofgielinor.integrations.QuestHelperTracker;
import atlasofgielinor.map.catalog.ShopIndex;
import atlasofgielinor.integrations.ShortestPathTracker;
import atlasofgielinor.integrations.SlayerTaskTracker;
import atlasofgielinor.map.WorldMapInput;
import atlasofgielinor.map.WorldMapPointReader;
import atlasofgielinor.tiles.TileLoader;
import atlasofgielinor.ui.markers.RaidBossDisplay;
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
public class AtlasOfGielinorOverlay extends Overlay
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
		final Predicate<AtlasOfGielinorConfig> getter;

		LayerToggle(String label, String key, Predicate<AtlasOfGielinorConfig> getter)
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
	static final List<LayerToggle> LAYER_TOGGLES = List.of(
		new LayerToggle("Banks", "iconBanks", AtlasOfGielinorConfig::iconBanks),
		new LayerToggle("Shops and trade", "iconShops", AtlasOfGielinorConfig::iconShops),
		new LayerToggle("Skilling icons", "iconSkilling", AtlasOfGielinorConfig::iconSkilling),
		new LayerToggle("Agility shortcuts", "iconShortcuts", AtlasOfGielinorConfig::iconShortcuts),
		new LayerToggle("Sailing lookouts", "iconSailing", AtlasOfGielinorConfig::iconSailing),
		new LayerToggle("Travel", "iconTravel", AtlasOfGielinorConfig::iconTravel),
		new LayerToggle("Quests", "iconQuests", AtlasOfGielinorConfig::iconQuests),
		new LayerToggle("Altars", "iconAltars", AtlasOfGielinorConfig::iconAltars),
		new LayerToggle("Dungeons", "iconDungeons", AtlasOfGielinorConfig::iconDungeons),
		new LayerToggle("Tutors and services", "iconServices", AtlasOfGielinorConfig::iconServices),
		new LayerToggle("Other map icons", "iconOther", AtlasOfGielinorConfig::iconOther),
		new LayerToggle("Clue helper", "showClueScroll", AtlasOfGielinorConfig::showClueScroll),
		new LayerToggle("Place names", "showPlaceNames", AtlasOfGielinorConfig::showPlaceNames),
		new LayerToggle("Underground symbols", "showLargeUndergroundSymbols", AtlasOfGielinorConfig::showLargeUndergroundSymbols),
		new LayerToggle("Boss markers", "showBossLocations", AtlasOfGielinorConfig::showBossLocations),
		new LayerToggle("Monster areas", "showMonsterZones", AtlasOfGielinorConfig::showMonsterZones),
		new LayerToggle("Monster icons", "showMonsterIcons", AtlasOfGielinorConfig::showMonsterIcons),
		new LayerToggle("Monster labels", "showMonsterLabels", AtlasOfGielinorConfig::showMonsterLabels),
		new LayerToggle("Ground items", "showGroundItems", AtlasOfGielinorConfig::showGroundItems),
		new LayerToggle("Travel routes", "showTravelRoutes", AtlasOfGielinorConfig::showTravelRoutes),
		new LayerToggle("Player boats", "showBoatLocations", AtlasOfGielinorConfig::showBoatLocations),
		new LayerToggle("Sailing ports", "showSailingPorts", AtlasOfGielinorConfig::showSailingPorts),
        new LayerToggle("Port notice boards", "showPortNoticeBoards", AtlasOfGielinorConfig::showPortNoticeBoards),
		new LayerToggle("Other plugin markers", "showPluginMarkers", AtlasOfGielinorConfig::showPluginMarkers),
		new LayerToggle("Player marker", "showPlayerMarker", AtlasOfGielinorConfig::showPlayerMarker),
		new LayerToggle("Tooltips", "showTooltips", AtlasOfGielinorConfig::showTooltips)
	);

	/**
	 * Sentinel key for the on-map "Hide all" master row. Not a persisted {@link AtlasOfGielinorConfig}
	 * field — {@link WorldMapInput} routes clicks on this to {@link AtlasOfGielinorPlugin#setAllLayersEnabled}.
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
	public static boolean allLayersHidden(AtlasOfGielinorConfig config)
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
	private final AtlasOfGielinorConfig config;
	private final TileLoader tileLoader;
	private final MapCamera camera;

	private final MapLayout layout;
	private final MapTileRenderer tileRenderer;
	private final MapMarkerRenderers markers;
	private final MapChromeRenderer chromeRenderer;
	private final MapFinderRenderer finderRenderer;
	private final MapTooltipRenderer tooltipRenderer;
	@Inject
	private RaidBossDisplay raidBossDisplay;
	private final WorldMapInput input;
	private final WorldMapPointReader worldMapPointReader;

	private boolean renderFailureLogged;
	private boolean wasOpen;
	private boolean everOpened;
	private Point lastClientPosition;
	private float lastClientZoom;
	/** Frames after open where client world-map jumps are ignored (avoids south-west snap). */
	private int ignoreClientJumps;

	@Inject
	public AtlasOfGielinorOverlay(
		Client client,
		AtlasOfGielinorConfig config,
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
		this.tileRenderer = new MapTileRenderer(config, camera, tileLoader, dungeonPieceIndex);
		this.markers = new MapMarkerRenderers(
			client, config, camera, poiIndex, shopIndex, monsterIndex, monsterIconManager, slayerTaskTracker,
			shortestPathTracker, groundItemIndex, boatTracker, worldMapPointManager, worldMapPointReader,
			dungeonPieceIndex);
		this.chromeRenderer = new MapChromeRenderer(config, camera, input, layout, clueScrollTracker,
			questHelperTracker, shortestPathTracker, boatTracker, client);
		this.finderRenderer = new MapFinderRenderer(
			config, camera, input, slayerTaskTracker, finder);
		this.tooltipRenderer = new MapTooltipRenderer(
			config, camera, input, poiIndex, monsterIndex, dungeonPieceIndex, monsterIconManager, slayerTaskTracker,
			shopIndex, finder, groundItemIndex, boatTracker, worldMapPointManager, worldMapPointReader);
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.MANUAL);
		setPriority(PRIORITY_HIGH);
		drawAfterInterface(InterfaceID.WORLDMAP);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!MapCatalogLoader.isReady()) return null;
		final Widget map = client.getWidget(InterfaceID.Worldmap.MAP_CONTAINER);
		final WorldMap worldMap = client.getWorldMap();

		if (map == null || map.isHidden() || worldMap == null)
		{
			if (wasOpen || camera.isActive())
			{
				log.debug("[AtlasOfGielinor:overlay] map closed/unavailable  mapNull={} mapHidden={} worldMapNull={} wasActive={}",
					map == null,
					map != null && map.isHidden(),
					worldMap == null,
					camera.isActive());
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

		final Rectangle bounds = viewport(map);
		if (bounds.width <= 0 || bounds.height <= 0)
		{
			log.debug("[AtlasOfGielinor:overlay] bad bounds {} — deactivating", bounds);
			camera.setActive(false);
			if (raidBossDisplay != null)
			{
				raidBossDisplay.close();
			}
			return null;
		}

		if (!wasOpen)
		{
			log.debug("[AtlasOfGielinor:overlay] map OPEN  fullscreen={} bounds={}x{} hideGameRender={}",
				config.fullscreenMap(), bounds.width, bounds.height, config.hideGameMapRender());
		}

		camera.setViewport(bounds);
		updateNativePassthrough(bounds);
		// Native input owns closing; the tick handler releases input when the interface disappears.
		camera.setActive(true);

		syncWithClient(worldMap);
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
			graphics.setClip(previousClip);
			return null;
		}

		graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

		try
		{
			// POI badges and plugin/marker layers keep separate overlap lists so enabling one
			// layer does not cull icons from another (e.g. Quest Helper stars hiding banks).
			final List<Rectangle> placedPoiIcons = new ArrayList<>();
			final List<Rectangle> placedMarkers = new ArrayList<>();
			tileRenderer.drawTiles(graphics, bounds);
			if (!config.showTravelRoutes() && !camera.isTravelViewActive())
			{
				camera.setSelectedTravelNode(null);
			}
			final boolean travelFocused = camera.isTravelViewActive()
				|| (config.showTravelRoutes() && camera.getSelectedTravelNode() != null);
			boolean routeDrawFailure = false;
			markers.beginFrame();
			if (travelFocused)
			{
				graphics.setColor(new Color(0, 0, 0, 28));
				graphics.fill(bounds);
				camera.setLayerSymbolTargets(Collections.emptyList());
			}
			else
			{
				routeDrawFailure = markers.routes.drawShortestPathRoute(graphics, bounds);
				markers.locations.drawPlaceNames(graphics, bounds);
				markers.pluginMarkers.drawPluginMarkers(graphics, bounds, placedMarkers);
			}
			markers.locations.drawPoiIcons(graphics, bounds, placedPoiIcons);
			if (!travelFocused)
			{
				markers.layers.drawLargeLayerSymbols(graphics, bounds);
				markers.monsters.drawMonsters(graphics, bounds, placedMarkers);
				markers.monsters.drawMonsterZones(graphics, bounds, placedMarkers);
				markers.groundItems.drawGroundItems(graphics, bounds, placedMarkers);
			}
			markers.routes.drawTravelStations(graphics, bounds);
			markers.routes.drawTravelRoutes(graphics, bounds);
			if (camera.isTravelViewActive())
			{
				markers.boats.drawSailingPorts(graphics, bounds);
				markers.boats.drawPlayerBoats(graphics, bounds, placedMarkers);
				markers.player.drawPlayer(graphics, bounds);
			}
			if (!travelFocused)
			{
				markers.boats.drawSailingPorts(graphics, bounds);
				markers.boats.drawPortNoticeBoards(graphics, bounds);
				markers.boats.drawPlayerBoats(graphics, bounds, placedMarkers);
				markers.flash.drawFlash(graphics, bounds);
				markers.player.drawPlayer(graphics, bounds);
			}
			chromeRenderer.drawPlaneSwitcher(graphics, bounds);
			chromeRenderer.drawStatusChip(graphics, bounds);
			chromeRenderer.drawClueButton(graphics, bounds);
			chromeRenderer.drawQuestButton(graphics, bounds);
			chromeRenderer.drawPlayerButton(graphics, bounds);
			chromeRenderer.drawDestinationButton(graphics, bounds);
			chromeRenderer.drawBoatsButton(graphics, bounds);
			chromeRenderer.drawLayersPanel(graphics, bounds);
			chromeRenderer.drawChatButton(graphics, bounds);
			chromeRenderer.drawCluePanel(graphics, bounds);
			chromeRenderer.drawBoatsDropdown(graphics, bounds);
			drawFinder(graphics, bounds);
			if (camera.isTravelViewActive() || raidBossDisplay == null || !raidBossDisplay.ownsHover(input.getCursor()))
			{
				tooltipRenderer.drawTooltip(graphics, bounds, markers);
			}
			if (raidBossDisplay != null && !camera.isTravelViewActive())
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
				log.warn("[AtlasOfGielinor] overlay stage failed; the map may be missing layers", e);
			}
		}

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
	private void syncWithClient(WorldMap worldMap)
	{
		final Point position = worldMap.getWorldMapPosition();
		if (position == null)
		{
			return;
		}

		final boolean clientMoved = !position.equals(lastClientPosition);
		final float clientZoom = worldMap.getWorldMapZoom();

		camera.followPlayerInterior();

		if (!wasOpen)
		{
			final WorldPoint rawPlayer = camera.getPlayerLocation();
			final UndergroundZone playerZone = camera.getActiveUndergroundZone();

			if (playerZone != null)
			{
				camera.setUndergroundMode(playerZone);
				camera.setPlane(rawPlayer.getPlane());
				for (DungeonFloor floor : DungeonFloor.all())
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

			log.debug("[AtlasOfGielinor] map opened, centred on {},{} ({}) resolved={} instance={}",
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

	/**
	 * Called when the native interface closes because this overlay
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
		log.debug("[AtlasOfGielinor:overlay] onMapClosed — wasOpen cleared for next open");
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
		return true;
	}

	/**
	 * Whether a POI badge is drawn for this point of interest, given the icon-category config.
	 *
	 * <p>Kept here because {@link atlasofgielinor.AtlasOfGielinorPlugin} and the sidebar ask the overlay;
	 * the filter itself lives with the icon drawing in {@link atlasofgielinor.ui.markers.LocationMarkerRenderer}.
	 */
	public boolean shouldDrawPoiIcon(PoiIndex.Poi poi)
	{
		return markers.locations.shouldDrawPoiIcon(poi);
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

	/** Keep native controls visible at their original click zones, including in fullscreen. */
	private Shape clipArea(Rectangle rect)
	{
		final Area area = new Area(rect);
		if (!config.fullscreenMap())
		{
			final Widget overview = client.getWidget(InterfaceID.Worldmap.OVERVIEW_CONTAINER);
			final Widget mapList = client.getWidget(InterfaceID.Worldmap.MAPLIST_BOX_GRAPHIC0);
			if (overview != null && !overview.isHidden())
			{
				area.subtract(new Area(overview.getBounds()));
			}
			if (mapList != null && !mapList.isHidden())
			{
				area.subtract(new Area(mapList.getBounds()));
			}
		}
		final Rectangle closeBounds = nativeCloseBounds();
		if (closeBounds != null)
		{
			area.subtract(new Area(closeBounds));
		}
		return area;
	}

	private Rectangle nativeCloseBounds()
	{
		final Widget close = client.getWidget(InterfaceID.Worldmap.CLOSE);
		if (close == null || close.isHidden())
		{
			return null;
		}
		final Rectangle bounds = close.getBounds();
		return bounds != null && bounds.width > 0 && bounds.height > 0 ? bounds : null;
	}

	/**
	 * Publishes screen rects where {@link WorldMapInput} must not grab clicks, so the client's
	 * overview minimap, native close button and world-map orb still receive them.
	 */
	private void updateNativePassthrough(Rectangle bounds)
	{
		final List<Rectangle> rects = new ArrayList<>();
		// Hub review F2: expose the real close widget; the user's original event reaches the client.
		// Do not replace this with menuAction, synthetic events, onOp or local interface detachment.
		// Map PR #11551 refused menuAction; clipping and passthrough preserve the native click zone.
		final Rectangle closeBounds = nativeCloseBounds();
		if (closeBounds != null)
		{
			rects.add(closeBounds);
		}

		final Rectangle overviewBounds = layout.overviewBounds();
		if (overviewBounds != null)
		{
			rects.add(overviewBounds.intersection(bounds));
		}

		final Rectangle worldMapOrb = MapLayout.worldMapOrbBounds(client);
		if (worldMapOrb != null)
		{
			rects.add(worldMapOrb);
		}

		camera.setNativePassthrough(rects.toArray(new Rectangle[0]));
	}
}
