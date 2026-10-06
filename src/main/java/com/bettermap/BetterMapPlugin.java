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
package com.bettermap;

import com.bettermap.data.DungeonPiece;
import com.bettermap.data.DungeonPieceTransform;
import java.awt.geom.Point2D;

import com.bettermap.data.sailing.BoatTracker;
import com.bettermap.data.sailing.PlayerBoat;
import com.bettermap.data.OverlayFloor;
import com.bettermap.data.TravelData;
import com.bettermap.data.UndergroundZone;
import com.bettermap.map.ClueScrollTracker;
import com.bettermap.map.DungeonPieceIndex;
import com.bettermap.map.FinderKeyCapture;
import com.bettermap.map.MapCamera;
import com.bettermap.map.MapData;
import com.bettermap.map.MapFinder;
import com.bettermap.map.MonsterIconManager;
import com.bettermap.map.MonsterIndex;
import com.bettermap.map.PoiIndex;
import com.bettermap.map.PoiDetails;
import com.bettermap.map.PrifddinasShift;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.bettermap.map.QuestHelperTracker;
import com.bettermap.map.ShortestPathTracker;
import com.bettermap.map.SlayerTaskTracker;
import com.bettermap.map.GroundItemIndex;
import com.bettermap.map.InstanceMaps;
import com.bettermap.map.ShopIndex;
import com.bettermap.map.WorldMapInput;
import com.bettermap.map.WorldPointResolver;
import com.bettermap.tiles.TileLoader;
import com.bettermap.tiles.MapAssetManager;
import com.bettermap.ui.BetterMapPanel;
import com.bettermap.ui.BetterWorldMapOverlay;
import com.google.inject.Provides;
import java.awt.Rectangle;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Future;
import javax.inject.Inject;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.Player;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.BeforeRender;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.worldmap.WorldMap;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.PluginChanged;
import net.runelite.client.events.PluginMessage;
import net.runelite.client.input.KeyManager;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.worldmap.WorldMapOverlay;
import net.runelite.client.util.ImageUtil;
import lombok.Getter;

@Slf4j
@PluginDescriptor(
	name = "Atlas of Gielinor",
	internalName = "better-map",
	description = "Replaces the world map with a slippy map: free zoom, drag panning, dungeon layers, monster zones and marker details",
	tags = {"map", "worldmap", "zoom", "dungeon", "navigation"}
)
public class BetterMapPlugin extends Plugin
{
	private static final double REVEAL_ZOOM = 4.0;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	@Getter
	private BetterMapConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private net.runelite.client.eventbus.EventBus eventBus;


	@Inject
	private OverlayManager overlayManager;

	@Inject
	private MouseManager mouseManager;

	@Inject
	private KeyManager keyManager;

	@Inject
	private BetterWorldMapOverlay mapOverlay;


	@Inject
	private WorldMapInput input;

	@Inject
	private FinderKeyCapture finderKeyCapture;

	@Inject
	private MapCamera camera;

	@Inject
	private TileLoader tileLoader;

	@Inject
	private MapAssetManager mapAssets;

	@Inject
	private PoiIndex poiIndex;

	@Inject
	private DungeonPieceIndex dungeonPieceIndex;

	@Inject
	private MapFinder finder;


	@Inject
	private ClueScrollTracker clueScrollTracker;

	@Inject
	private ShortestPathTracker shortestPathTracker;

	@Inject
	private QuestHelperTracker questHelperTracker;

	@Inject
	private MonsterIndex monsterIndex;

	@Inject
	private SlayerTaskTracker slayerTaskTracker;

	@Inject
	private MonsterIconManager monsterIconManager;

	@Inject
	private ShopIndex shopIndex;

	@Inject
	private GroundItemIndex groundItemIndex;

	@Inject
	@Getter
	private BoatTracker boatTracker;

	@Inject
	private ScheduledExecutorService executor;

	@Inject
	private ClientToolbar clientToolbar;

	/**
	 * RuneLite's own world map overlay. It positions markers with the client's camera, which
	 * stops matching ours the moment the map is panned or zoomed, so it is taken out of the
	 * rotation while we are drawing and put back on shutdown.
	 */
	@Inject
	private WorldMapOverlay runeliteWorldMapOverlay;

	/** Written on the client thread, read from Swing. See {@link #getPlayerLocation()}. */
	private volatile WorldPoint lastPlayerLocation;

	private BetterMapPanel panel;
	private NavigationButton navButton;
	private boolean sidebarNavAdded;
	private boolean suppressedRuneliteOverlay;
	/** What {@link #onBeforeRender} last applied, so an already-restored widget is not re-read. */
	private boolean mapWidgetsHidden;
	private boolean chatWidgetHidden;
	/** Client-thread only: widgets that Atlas changed from visible to self-hidden. */
	private final Map<Integer, Widget> hiddenWidgets = new HashMap<>();
	private int inputHeartbeatTick;

	private Future<?> startupTask;

	@Override
	protected void startUp() throws Exception
	{
		// Preserve users who enabled the previous external-settings mode.
		if (!Boolean.TRUE.equals(configManager.getConfiguration("bettermap", "shortestPathHubMigration", Boolean.class)))
		{
			if (Boolean.TRUE.equals(configManager.getConfiguration("bettermap", "useExternalShortestPathSettings", Boolean.class)))
			{
				configManager.setConfiguration("bettermap", "enableShortestPath", true);
			}
			configManager.setConfiguration("bettermap", "shortestPathHubMigration", true);
		}
		shortestPathTracker.startUp();
		mapAssets.startUp(() -> getPluginDirectory().join("map-assets"), config.downloadMapAssets());


		overlayManager.add(mapOverlay);
		mouseManager.registerMouseListener(input);
		mouseManager.registerMouseWheelListener(input);
		keyManager.registerKeyListener(input);
		syncRuneliteOverlay();

		panel = new BetterMapPanel(this, poiIndex, monsterIndex, boatTracker);
		navButton = NavigationButton.builder()
			.tooltip("Atlas of Gielinor")
			.icon(ImageUtil.loadImageResource(BetterMapPlugin.class, "icon.png"))
			.priority(6)
			.panel(panel)
			.build();
		if (config.showSidebarPanel())
		{
			clientToolbar.addNavigation(navButton);
			sidebarNavAdded = true;
		}

		final BetterMapPanel loadedPanel = panel;
		startupTask = executor.submit(() ->
		{
			poiIndex.load(null);
			if (Thread.currentThread().isInterrupted())
			{
				return;
			}
			dungeonPieceIndex.load(null);
			if (Thread.currentThread().isInterrupted())
			{
				return;
			}
			monsterIndex.load();
			shopIndex.load();
			groundItemIndex.load();
			if (Thread.currentThread().isInterrupted())
			{
				return;
			}
			javax.swing.SwingUtilities.invokeLater(() ->
			{
				if (panel == loadedPanel)
				{
					loadedPanel.refresh();
				}
			});
			logStartupState();
		});
	}

	@Override
	protected void shutDown() throws Exception
	{
		camera.setFinderPanelOpen(false);
		input.focusLost();
		shortestPathTracker.shutDown();
		if (startupTask != null)
		{
			startupTask.cancel(true);
			startupTask = null;
		}
		panel = null;
		overlayManager.remove(mapOverlay);
		mouseManager.unregisterMouseListener(input);
		mouseManager.unregisterMouseWheelListener(input);
		keyManager.unregisterKeyListener(input);
		finderKeyCapture.shutDown();

		if (suppressedRuneliteOverlay)
		{
			overlayManager.add(runeliteWorldMapOverlay);
			suppressedRuneliteOverlay = false;
		}

		if (navButton != null && sidebarNavAdded)
		{
			clientToolbar.removeNavigation(navButton);
			sidebarNavAdded = false;
		}


		clientThread.invoke(() ->
		{
			setGameMapHidden(false);
			setChatHidden(false);
			mapWidgetsHidden = false;
			chatWidgetHidden = false;
			// Restore only the current widgets that Atlas itself hid.
		});
		mapAssets.shutDown();

		log.info("Atlas of Gielinor stopped");
	}

	/** One readable block at start-up saying whether every input to the map is actually there. */
	private void logStartupState()
	{
		final boolean hasTiles = tileLoader.hasTiles();

		log.debug("[BetterMap] ---- start-up ----");
		log.debug("[BetterMap] installed tile store: {}", tileLoader.getTileDir());
		log.debug("[BetterMap] map tiles   : hasTiles={}; zoom levels [{}]",
			hasTiles, tileLoader.describeLevels());
		log.debug("[BetterMap] poi index   : {} places, loaded={}", poiIndex.size(), poiIndex.isLoaded());
		log.debug("[BetterMap] monster idx : {} zones (omitted={})", monsterIndex.size(), monsterIndex.getDroppedZones());
		log.debug("[BetterMap] shop index  : {} shops, loaded={}", shopIndex.size(), shopIndex.isLoaded());
		log.debug("[BetterMap] ground items: {} spawn tiles, loaded={}", groundItemIndex.size(), groundItemIndex.isLoaded());
		log.debug("[BetterMap] config      : fullscreen={} hideGameRender={} skillingIcons={} ",
			config.fullscreenMap(), config.hideGameMapRender(),
			config.iconSkilling());
		log.info("[BetterMap] runelite world map overlay suppressed={}", suppressedRuneliteOverlay);

		if (!hasTiles)
		{
			log.debug("[BetterMap] map assets: {}", tileLoader.getStatus());
		}
		else if (!poiIndex.isLoaded())
		{
			log.warn("[BetterMap] tiles are present but the POI index is not: check poi/pois.tsv");
		}
	}

	/**
	 * The client re-shows its map widgets whenever the interface scripts run, so the hide has to
	 * be reapplied every frame while we are covering them.
	 */
	@Subscribe
	public void onBeforeRender(BeforeRender event)
	{
		// Hiding has to be reapplied every frame, but restoring does not: once the widgets are
		// back the client leaves them alone. Skipping the no-op restore keeps widget lookups
		// per frame off the path a player is on whenever the map is closed - which is most of it.
		final boolean hideMap = config.hideGameMapRender() && MapData.isReady();
		if (hideMap || mapWidgetsHidden)
		{
			setGameMapHidden(hideMap);
			mapWidgetsHidden = hideMap;
		}

		// Never hide the chatbox while the finder's chatbox input is open, or the user types blind.
		final boolean hideChat = camera.isActive() && camera.isChatHidden() && !finderKeyCapture.isOpen();
		if (hideChat || chatWidgetHidden)
		{
			setChatHidden(hideChat);
			chatWidgetHidden = hideChat;
		}

		finderKeyCapture.sync();
	}


	/**
	 * Hides the game's chatbox while the map holds it down.
	 *
	 * <p>{@code Chatbox.UNIVERSE} is the chatbox root, so the CHAT_* and CONTROLS* components go
	 * with it. Like the map widgets above, this has to be reapplied every frame because the
	 * client's interface scripts re-show it.
	 */
	private void setChatHidden(boolean hidden)
	{
		hideWidget(InterfaceID.Chatbox.UNIVERSE, hidden);
	}

	private boolean isWorldMapOpen()
	{
		final Widget map = client.getWidget(InterfaceID.Worldmap.MAP_CONTAINER);
		return map != null && !map.isHidden();
	}

	private static String widgetState(Widget w)
	{
		if (w == null)
		{
			return "null";
		}
		return "id=" + w.getId()
			+ " hidden=" + w.isHidden()
			+ " selfHidden=" + w.isSelfHidden();
	}

	private void setGameMapHidden(boolean hidden)
	{
		hideWidget(InterfaceID.Worldmap.MAP_DISPLAY, hidden);
		hideWidget(InterfaceID.Worldmap.MAP_OVERLAY, hidden);
	}

	private void hideWidget(int id, boolean hidden)
	{
		final Widget widget = client.getWidget(id);
		// Interface recreation invalidates ownership of the old widget, even at the same ID.
		if (hiddenWidgets.get(id) != widget)
		{
			hiddenWidgets.remove(id);
		}
		if (widget == null)
		{
			return;
		}
		if (hidden)
		{
			if (widget.isSelfHidden())
			{
				return;
			}
			hiddenWidgets.put(id, widget);
		}
		else if (hiddenWidgets.remove(id) != widget || !widget.isSelfHidden())
		{
			// A component already hidden by the game or another plugin is not ours to show.
			return;
		}
		widget.setHidden(hidden);
		log.debug("[BetterMap] widget {} hidden={}", id, hidden);
	}

	/**
	 * The player's last known position, for sorting the legend by distance.
	 *
	 * <p>Returns a cached value on purpose. The legend runs on Swing's event thread, and
	 * {@code Actor#getWorldLocation} asserts it is called on the client thread - clicking a
	 * legend row went straight through that assertion and killed the sidebar.
	 */
	public WorldPoint getPlayerLocation()
	{
		return lastPlayerLocation;
	}

	/** Game-chat line, used by the dev dungeon-layer tuner to echo corrected coordinates. */
	public void printToChat(String message)
	{
		if (client != null)
		{
			clientThread.invokeLater(() ->
				client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, null));
		}
	}

	/**
	 * The player's position in map coordinates.
	 *
	 * <p>Inside an instance {@code Actor#getWorldLocation} reports the coordinates of the
	 * instance's own copy of the scene, which for most instances is a slab of unmapped space -
	 * so the map centred and drew the player arrow in an empty area (Perilous Moons opened north
	 * of Cam Torum). Resolving through the instance template chunks gives the coordinates of the
	 * real rooms the instance was copied from, which is where the tiles actually are.
	 *
	 * <p>Instances whose template chunks are themselves off-map (raids, quest copies) still have
	 * nowhere sensible to land; they fall back to the raw location.
	 */
	private WorldPoint resolvePlayerLocation(Player local)
	{
		final WorldPoint raw = local.getWorldLocation();
		try
		{
			final WorldPoint resolved = WorldPointResolver.fromLocalInstance(client, local);
			if (resolved == null)
			{
				return raw;
			}
			if (config.debugLogging() && client.isInInstancedRegion())
			{
				log.debug("[BetterMap] instance player raw={} resolved={}", raw, resolved);
			}
			return resolved;
		}
		catch (RuntimeException | LinkageError e)
		{
			log.debug("[BetterMap] instance resolve failed at {}", raw, e);
			return raw;
		}
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		final Player local = client.getLocalPlayer();
		lastPlayerLocation = local == null ? null : resolvePlayerLocation(local);
		camera.setPlayerLocation(lastPlayerLocation);
		camera.setPlayerInInstance(local != null && client.isInInstancedRegion());

		// Remember the last spot the player actually stood on the overworld surface. Instances with
		// no UndergroundZone mapping resolve to unmapped template coords, so
		// this remembered point is the only sane surface anchor for the player marker and for
		// right-click "exit to surface".
		if (lastPlayerLocation != null
			&& !client.isInInstancedRegion()
			&& lastPlayerLocation.getY() <= InstanceMaps.GAP_MIN_Y)
		{
			camera.setLastSurfaceLocation(lastPlayerLocation);
		}

		clueScrollTracker.update();
		shortestPathTracker.update(lastPlayerLocation);
		questHelperTracker.update();
		echoFinderResults();

		final boolean worldMapOpen = isWorldMapOpen();

		if (!worldMapOpen && camera.isActive())
		{
			// Overlay stops rendering when WORLDMAP closes, so render() cannot clear the grab.
			camera.setActive(false);
			mapOverlay.onMapClosed();
			log.debug("[BetterMap:close] tick — stale active cleared (map closed via client)");
		}
		else if (config.debugLogging() && camera.isActive())
		{
			inputHeartbeatTick++;
			if (inputHeartbeatTick % 4 == 0)
			{
				final Widget map = client.getWidget(InterfaceID.Worldmap.MAP_CONTAINER);
				final Rectangle view = camera.getViewport();
				log.debug("[BetterMap:heartbeat] ACTIVE grab  fullscreen={} viewport={} map={}",
					config.fullscreenMap(),
					view == null ? "null" : view.width + "x" + view.height,
					widgetState(map));
			}
		}
		else
		{
			inputHeartbeatTick = 0;
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		boatTracker.onGameStateChanged(event);
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		boatTracker.onVarbitChanged(event);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		boatTracker.onConfigChanged(event);

		if ("shortestpath".equals(event.getGroup()))
		{
			clientThread.invoke(shortestPathTracker::refreshRoute);
			return;
		}
		if (!"bettermap".equals(event.getGroup()))
		{
			return;
		}

		if ("downloadMapAssets".equals(event.getKey()))
		{
			mapAssets.startUp(() -> getPluginDirectory().join("map-assets"), config.downloadMapAssets());
		}
		if ("showSidebarPanel".equals(event.getKey()))
		{
			syncSidebarPanel();
		}
		if ("enableShortestPath".equals(event.getKey()))
		{
			clientThread.invoke(shortestPathTracker::refreshRoute);
		}
	}

	private void syncSidebarPanel()
	{
		if (navButton == null)
		{
			return;
		}
		if (config.showSidebarPanel())
		{
			if (!sidebarNavAdded)
			{
				clientToolbar.addNavigation(navButton);
				sidebarNavAdded = true;
			}
		}
		else
		{
			if (sidebarNavAdded)
			{
				clientToolbar.removeNavigation(navButton);
				sidebarNavAdded = false;
			}
		}
	}

	/** Detach RuneLite's native markers while this plugin owns the world map. */
	private void syncRuneliteOverlay()
	{
		if (!suppressedRuneliteOverlay)
		{
			overlayManager.remove(runeliteWorldMapOverlay);
			suppressedRuneliteOverlay = true;
		}
	}

	@Subscribe
	public void onPluginMessage(PluginMessage event)
	{
		if ("bettermap".equals(event.getNamespace()))
		{
			if ("path".equals(event.getName()) || "clear".equals(event.getName()) || "getState".equals(event.getName()))
			{
				clientThread.invoke(() ->
				{
					if ("clear".equals(event.getName()))
					{
						shortestPathTracker.cancelRoute();
					}
					else if ("path".equals(event.getName()) && shortestPathTracker.isRoutingEnabled())
					{
						eventBus.post(new PluginMessage("shortestpath", "path", event.getData()));
					}
					java.util.Map<String, Object> response = new java.util.HashMap<>(getRouteState());
					if (event.getData().containsKey("requestId")) response.put("requestId", event.getData().get("requestId"));
					eventBus.post(new PluginMessage("bettermap", "state", java.util.Collections.unmodifiableMap(response)));
				});
			}
			return;
		}
		if ("shortestpath".equals(event.getNamespace())
			&& ("currentTarget".equals(event.getName()) || "result".equals(event.getName()) || "clear".equals(event.getName())))
		{
			clientThread.invoke(() -> shortestPathTracker.onPluginMessage(event));
		}
	}

	/** Read on the client thread. The returned state and route are immutable snapshots. */
	public java.util.Map<String, Object> getRouteState()
	{
		return shortestPathTracker.snapshot();
	}

	@Subscribe
	public void onPluginChanged(PluginChanged event)
	{
		boatTracker.onPluginChanged(event);
		if ("shortestpath.ShortestPathPlugin".equals(event.getPlugin().getClass().getName()))
		{
			clientThread.invoke(shortestPathTracker::refreshRoute);
		}
	}


	/** Back out to the surface layer, showing the whole world. */
	public void returnToSurface()
	{
		camera.centerOn(3222, 3218);
		camera.setPlane(0);

		final Rectangle view = camera.getViewport();
		if (view != null)
		{
			camera.fit(view);
		}
	}

	public void centerMapOn(WorldPoint point)
	{
		if (point == null)
		{
			return;
		}
		UndergroundZone zone = InstanceMaps.isOverworldOverlay(point.getX(), point.getY())
			? null : InstanceMaps.zoneForPoint(point.getX(), point.getY(), point.getPlane());
		// Authored pieces can extend beyond the wiki's dungeon boxes.
		if ((zone == null || dungeonPieceIndex.pieceAt(zone.getId(), point.getX(), point.getY(), point.getPlane(), null) == null)
			&& !InstanceMaps.isOverworldOverlay(point.getX(), point.getY())
			&& (point.getY() > InstanceMaps.GAP_MIN_Y || point.getY() < MapCamera.MIN_WORLD_Y))
		{
			for (UndergroundZone candidate : UndergroundZone.ALL_ZONES)
			{
				if (dungeonPieceIndex.pieceAt(candidate.getId(), point.getX(), point.getY(), point.getPlane(), null) != null)
				{
					zone = candidate;
					break;
				}
			}
		}
		camera.clearUndergroundMode();
		camera.setHoveredUnderground(null, true);
		camera.setPlane(point.getPlane());
		if (zone != null)
		{
			camera.setUndergroundMode(zone);
			camera.setPlane(point.getPlane());
			final DungeonPiece piece = dungeonPieceIndex.pieceAt(
				zone.getId(), point.getX(), point.getY(), point.getPlane(), null);
			OverlayFloor selectedFloor = null;
			for (OverlayFloor floor : OverlayFloor.all())
			{
				if (floor.zone != zone || floor.plane != point.getPlane())
				{
					continue;
				}
				if (piece == null || (floor.layerId != null && floor.layerId.equals(piece.layer)))
				{
					selectedFloor = floor;
					break;
				}
				if (floor.layerId == null)
				{
					selectedFloor = floor;
				}
			}
			if (selectedFloor != null)
			{
				camera.setActiveFloor(selectedFloor);
			}
			final int nudgeX = camera.getDungeonTuner().offsetX(zone);
			final int nudgeY = camera.getDungeonTuner().offsetY(zone);
			final Point2D display = piece == null
				? new Point2D.Double(point.getX() + 0.5 - zone.getDeltaX() - nudgeX,
					point.getY() + 0.5 - zone.getDeltaY() - nudgeY)
				: DungeonPieceTransform.toDisplay(piece, point.getX() + 0.5, point.getY() + 0.5, nudgeX, nudgeY);
			camera.centerOn(display.getX(), display.getY());
		}
		else
		{
			final WorldPoint display = point.equals(camera.getPlayerLocation())
				? camera.getPlayerDisplayLocation() : null;
			camera.centerOn(display != null ? display.getX() : point.getX(),
				display != null ? display.getY() : point.getY());
		}
		camera.setZoom(Math.max(camera.getZoom(), REVEAL_ZOOM));
		camera.flashAt(new WorldPoint((int) Math.floor(camera.getCenterX()),
			(int) Math.floor(camera.getCenterY()), camera.getPlane()));

		// Keep the client roughly in step so its icon and region data stays loaded around us.
		clientThread.invoke(() ->
		{
			final WorldMap worldMap = client.getWorldMap();
			if (worldMap != null)
			{
				worldMap.setWorldMapPositionTarget(point);
			}
		});
	}

	/** Center on a destination and request Shortest Path to calculate a route to it. */
	public void routeTo(WorldPoint point)
	{
		centerMapOn(point);
		clientThread.invoke(() -> shortestPathTracker.routeTo(point));
	}

	/** Place a route at the cursor without moving the map camera. */
	public void placeRouteAt(int screenX, int screenY)
	{
		final Rectangle viewport = camera.getViewport();
		if (!camera.isActive() || viewport == null || !viewport.contains(screenX, screenY))
		{
			return;
		}
		final WorldPoint point = routePointAt(camera.worldX(screenX, viewport), camera.worldY(screenY, viewport));
		if (point != null)
		{
			clientThread.invoke(() -> shortestPathTracker.routeTo(point));
		}
	}

	/** Reverse the same placement, rotation and flip used to draw the displayed dungeon tile. */
	@Nullable
	private WorldPoint routePointAt(double displayX, double displayY)
	{
		final List<UndergroundZone> zones = camera.isUndergroundModeActive() || config.undergroundHoverPreview()
			? camera.previewUndergroundZones() : java.util.Collections.emptyList();
		for (int z = zones.size() - 1; z >= 0; z--)
		{
			final UndergroundZone zone = zones.get(z);
			final int nudgeX = camera.getDungeonTuner().offsetX(zone);
			final int nudgeY = camera.getDungeonTuner().offsetY(zone);
			final List<DungeonPiece> pieces = dungeonPieceIndex.piecesFor(zone.getId());
			final Integer layer = camera.floorLayerFor(zone);
			final int plane = camera.getHoveredFloorPlane() != null && camera.getHoveredUndergroundZone() == zone
				? camera.getHoveredFloorPlane() : camera.getActiveUndergroundZone() == zone
					? camera.getPlane() : zone.getUndergroundPoint().getPlane();
			boolean multiPlane = false;
			int firstPlane = -1;
			for (OverlayFloor floor : OverlayFloor.all())
			{
				if (floor.zone != zone)
				{
					continue;
				}
				if (firstPlane < 0)
				{
					firstPlane = floor.plane;
				}
				else if (floor.plane != firstPlane)
				{
					multiPlane = true;
					break;
				}
			}
			for (int p = pieces.size() - 1; p >= 0; p--)
			{
				final DungeonPiece piece = pieces.get(p);
				if (camera.isUndergroundModeActive() ? !camera.isDungeonPieceVisible(zone, piece)
					: (multiPlane && piece.plane != plane) || (layer != null && piece.layer != layer))
				{
					continue;
				}
				final Point2D nativePoint = DungeonPieceTransform.toNative(piece, displayX, displayY, nudgeX, nudgeY);
				final int x = (int) Math.floor(nativePoint.getX());
				final int y = (int) Math.floor(nativePoint.getY());
				if (piece.containsNative(x, y))
				{
					return new WorldPoint(x, y, piece.plane);
				}
			}
			if (pieces.isEmpty())
			{
				final int x = (int) Math.floor(displayX + zone.getDeltaX() + nudgeX);
				final int y = (int) Math.floor(displayY + zone.getDeltaY() + nudgeY);
				final boolean inside = zone.hasClipOverride()
					? x >= zone.getClipMinX() && x <= zone.getClipMaxX()
						&& y >= zone.getClipMinY() && y <= zone.getClipMaxY()
					: InstanceMaps.clipMapsForZone(zone).isEmpty()
						|| InstanceMaps.clipMapsForZone(zone).stream().anyMatch(map -> map.contains(x, y));
				if (inside)
				{
					return new WorldPoint(x, y, zone.getUndergroundPoint().getPlane());
				}
			}
		}
		// Empty space in a dungeon cutaway is not a surface destination.
		return zones.isEmpty()
			? PrifddinasShift.toWorld((int) Math.floor(displayX), (int) Math.floor(displayY), camera.getPlane())
			: null;
	}

	public void cancelRoute()
	{
		clientThread.invoke(shortestPathTracker::cancelRoute);
	}


	/**
	 * Pans and flashes the map on the local player, following them into an instance layer if that is
	 * where they are. Bound to the on-map "Go to Player" chip.
	 */
	public void goToPlayer()
	{
		final WorldPoint here = getPlayerLocation();
		if (here != null)
		{
			centerMapOn(here);
		}
	}

	/**
	 * Pans the map to the active clue scroll target. With several candidate tiles (hot/cold,
	 * three-step cryptic) the one nearest the player is chosen. No-op when there is no clue.
	 */
	public void goToClue()
	{
		final java.util.List<WorldPoint> targets = clueScrollTracker.locations();
		if (targets.isEmpty())
		{
			return;
		}

		WorldPoint best = targets.get(0);
		final WorldPoint from = lastPlayerLocation;
		if (from != null && targets.size() > 1)
		{
			long bestDist = Long.MAX_VALUE;
			for (WorldPoint p : targets)
			{
				final long dx = p.getX() - from.getX();
				final long dy = p.getY() - from.getY();
				final long d = dx * dx + dy * dy;
				if (d < bestDist)
				{
					bestDist = d;
					best = p;
				}
			}
		}

		centerMapOn(best);
	}

	/**
	 * Pans the map to the Quest Helper step and routes to it. Bound to the on-map "Go to Quest
	 * Step" chip, so unlike {@link #syncQuestRoute()} it runs whatever the follow setting is and
	 * announces the route in chat the way every other deliberate route does.
	 */
	/**
	 * Pans the map to the Quest Helper step. Bound to the on-map "Go to Quest Step" chip.
	 */
	public void goToQuestStep()
	{
		final WorldPoint step = questHelperTracker.target();
		if (step == null)
		{
			return;
		}
		centerMapOn(step);
	}

	/**
	 * Pans the map to the active route / Shortest Path destination.
	 * Bound to the on-map "Go to Destination" chip.
	 */
	public void goToDestination()
	{
		if (shortestPathTracker == null)
		{
			return;
		}
		final WorldPoint dest = shortestPathTracker.target();
		if (dest != null)
		{
			centerMapOn(dest);
		}
	}

	/**
	 * Pans the map to the specified player boat's port location.
	 */
	public void goToBoat(PlayerBoat boat)
	{
		if (boat == null || boat.getPort() == null)
		{
			return;
		}
		final WorldPoint loc = boat.getPort().getNavigationLocation();
		if (loc != null)
		{
			centerMapOn(loc);
		}
	}


	/** Flip a {@code bettermap.*} boolean from the on-map Layers panel. The overlay re-reads config each frame. */
	public void setLayerEnabled(String key, boolean value)
	{
		configManager.setConfiguration("bettermap", key, value);
	}

	/** Master switch from the on-map Layers panel "Hide all" row. */
	public void setAllLayersEnabled(boolean enabled)
	{
		BetterWorldMapOverlay.forEachLayerToggle(
			(key, value) -> configManager.setConfiguration("bettermap", key, value),
			enabled);
	}

	/** Last finder result set echoed to chat, so a tick only re-prints when it changes. */
	private String lastFinderEcho = "";

	private void echoFinderResults()
	{
		if (config == null || !config.finderEchoChat() || !camera.isFinderPanelOpen())
		{
			lastFinderEcho = "";
			return;
		}

		final String query = finder.getQuery();
		final java.util.List<MapFinder.Result> hits = finder.getResults();
		if (query == null || query.trim().isEmpty() || hits.isEmpty())
		{
			lastFinderEcho = "";
			return;
		}

		final int show = Math.min(3, hits.size());
		final StringBuilder sig = new StringBuilder(query.trim()).append('|');
		for (int i = 0; i < show; i++)
		{
			sig.append(hits.get(i).getName()).append('#').append(hits.get(i).getDistanceTiles()).append(';');
		}
		final String signature = sig.toString();
		if (signature.equals(lastFinderEcho))
		{
			return;
		}
		lastFinderEcho = signature;

		for (int i = 0; i < show; i++)
		{
			final MapFinder.Result r = hits.get(i);
			final int d = r.getDistanceTiles();
			final String dist = d >= 0 ? " (~" + d + " tiles)" : "";
			final String head = i == 0 ? "Atlas of Gielinor \"" + query.trim() + "\": " : "  ";
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "",
				head + (i + 1) + ". " + r.getName() + dist, null);
		}
	}

	@Subscribe
	public void onMenuOpened(MenuOpened event)
	{
		if (config == null)
		{
			return;
		}
		final net.runelite.api.Point m = client.getMouseCanvasPosition();
		if (m == null || m.getX() < 0)
		{
			return;
		}

		addMapContextMenuEntries(m);
	}

	private void addMapContextMenuEntries(net.runelite.api.Point mouse)
	{
		// Hub review F1: append local entries; never clear or reorder the game's entries.
		// Do not restore resetMapMenu() to tidy this menu (see PLUGIN_HUB_REVIEW.md, F1).
		for (Rectangle passthrough : camera.getNativePassthrough())
		{
			if (passthrough != null && passthrough.contains(mouse.getX(), mouse.getY()))
			{
				return;
			}
		}
		final WorldPoint finderTarget = finderTargetAt(mouse);
		if (finderTarget != null)
		{
			addRouteMenuEntry(finderTarget);
			client.getMenu().createMenuEntry(-1)
				.setOption("Show on map")
				.setTarget("<col=ffff00>" + finderTarget.getX() + ", " + finderTarget.getY() + "</col>")
				.setType(MenuAction.RUNELITE)
				.onClick(e -> centerMapOn(finderTarget));
			return;
		}
		final Rectangle finderPanel = camera.getFinderPanelBounds();
		final Rectangle finderFlyout = camera.getFinderFlyoutHitBounds();
		if (camera.isFinderPanelOpen()
			&& ((finderPanel != null && finderPanel.contains(mouse.getX(), mouse.getY()))
				|| (finderFlyout != null && finderFlyout.contains(mouse.getX(), mouse.getY()))))
		{
			return;
		}

		final Rectangle viewport = camera.getViewport();
		if (!camera.isActive() || viewport == null || !viewport.contains(mouse.getX(), mouse.getY()))
		{
			return;
		}

		final int worldX = (int) Math.floor(camera.worldX(mouse.getX(), viewport));
		final int worldY = (int) Math.floor(camera.worldY(mouse.getY(), viewport));
		final WorldPoint targetPoint = routePointAt(
			camera.worldX(mouse.getX(), viewport), camera.worldY(mouse.getY(), viewport));

		addRouteMenuEntry(targetPoint);

		if (camera.isViewingDungeonLayer())
		{
			client.getMenu().createMenuEntry(-1)
				.setOption("Return to surface")
				.setTarget("<col=ffff00>Overworld</col>")
				.setType(MenuAction.RUNELITE)
				.onClick(e -> camera.exitDungeonToSurface());
		}

		addEntityContextMenuEntries(mouse, worldX, worldY);
	}

	@Nullable
	private WorldPoint finderTargetAt(net.runelite.api.Point mouse)
	{
		if (!camera.isFinderInteractive())
		{
			return null;
		}
		for (MapCamera.FlyoutTarget target : camera.getFlyoutTargets())
		{
			final Rectangle row = target.getRowBounds();
			final Rectangle route = target.getWalkBounds();
			if ((row != null && row.contains(mouse.getX(), mouse.getY()))
				|| (route != null && route.contains(mouse.getX(), mouse.getY())))
			{
				return target.getPoint();
			}
		}
		for (MapCamera.FinderResultTarget target : camera.getFinderResultTargets())
		{
			final Rectangle row = target.getRowBounds();
			final Rectangle route = target.getWalkBounds();
			if ((row != null && row.contains(mouse.getX(), mouse.getY()))
				|| (route != null && route.contains(mouse.getX(), mouse.getY())))
			{
				if (target.getRegion() != null)
				{
					return new WorldPoint(target.getRegion().getCenterX(), target.getRegion().getCenterY(), 0);
				}
				final MapFinder.Result result = MapFinder.activationTarget(target.getResult());
				return result != null ? result.getPoint() : target.getPoint();
			}
		}
		return null;
	}

	private void addRouteMenuEntry(WorldPoint point)
	{
		if (shortestPathTracker != null && shortestPathTracker.hasTarget())
		{
			client.getMenu().createMenuEntry(-1)
				.setOption("Cancel route")
				.setType(MenuAction.RUNELITE)
				.onClick(e -> cancelRoute());
		}
		if (point == null)
		{
			return;
		}
		client.getMenu().createMenuEntry(-1)
			.setOption("Route here")
			.setTarget("<col=ffff00>" + point.getX() + ", " + point.getY() + "</col>")
			.setType(MenuAction.RUNELITE)
			.onClick(e -> routeTo(point));
	}

	private void addEntityContextMenuEntries(net.runelite.api.Point mouse, int worldX, int worldY)
	{
		for (MapCamera.LayerSymbolTarget target : camera.getLayerSymbolTargets())
		{
			if (target.getBounds() == null || !target.getBounds().contains(mouse.getX(), mouse.getY()))
			{
				continue;
			}
			addLayerSymbolMenuEntries(target);
			return;
		}

		final int radius = (int) Math.max(3, Math.ceil(12 / Math.max(0.4, camera.getFrameZoom())));
		final TravelData.TravelNode travel = InstanceMaps.firstHit(worldX, worldY,
			camera.getFocusedUndergroundZone(), camera.isDungeonContentsFocused(),
			(x, y) -> TravelData.findNodeNear(x, y, camera.getPlane(), radius));
		if (travel != null)
		{
			final WorldPoint point = travel.getLocation();
			client.getMenu().createMenuEntry(-1)
				.setOption("Mark")
				.setTarget("<col=ffff00>" + travel.getName() + "</col>")
				.setType(MenuAction.RUNELITE)
				.onClick(e -> markMapPoint(point));
			return;
		}

		if (poiIndex == null)
		{
			return;
		}
		final PoiIndex.Poi poi = InstanceMaps.firstHit(worldX, worldY,
			camera.getFocusedUndergroundZone(), camera.isDungeonContentsFocused(),
			(x, y) -> poiIndex.nearest(x, y, camera.getPlane(), radius));
		if (poi == null || (mapOverlay != null && !mapOverlay.shouldDrawPoiIcon(poi)))
		{
			return;
		}

		final WorldPoint point = new WorldPoint(poi.getX(), poi.getY(), poi.getPlane());
		final String target = "<col=ffff00>" + poi.getName() + "</col>";
		client.getMenu().createMenuEntry(-1)
			.setOption("Examine")
			.setTarget(target)
			.setType(MenuAction.RUNELITE)
			.onClick(e -> examinePoi(poi));
		client.getMenu().createMenuEntry(-1)
			.setOption("Mark")
			.setTarget(target)
			.setType(MenuAction.RUNELITE)
			.onClick(e -> markMapPoint(point));
	}

	private void addLayerSymbolMenuEntries(MapCamera.LayerSymbolTarget target)
	{
		final UndergroundZone zone = target.getZone();
		if (zone == null)
		{
			return;
		}
		if (target.isSurfaceToUnderground())
		{
			client.getMenu().createMenuEntry(-1)
				.setOption(target.isFloor() ? "Open floor" : "Enter")
				.setTarget("<col=ffff00>" + (target.isFloor() ? target.getFloor().name : zone.getName()) + "</col>")
				.setType(MenuAction.RUNELITE)
				.onClick(e ->
				{
					if (target.isFloor())
					{
						camera.setActiveFloor(target.getFloor());
					}
					else
					{
						camera.toggleUndergroundZone(zone, target.getSurfacePoint());
					}
				});
			final OverlayFloor next = nextFloor(zone, target.getFloor());
			if (next != null)
			{
				client.getMenu().createMenuEntry(-1)
					.setOption("Cycle floor")
					.setTarget("<col=ffff00>" + next.name + "</col>")
					.setType(MenuAction.RUNELITE)
					.onClick(e -> camera.setActiveFloor(next));
			}
		}
		else
		{
			client.getMenu().createMenuEntry(-1)
				.setOption("Return to surface")
				.setTarget("<col=ffff00>" + zone.getName() + "</col>")
				.setType(MenuAction.RUNELITE)
				.onClick(e -> camera.exitDungeonToSurface(target.getSurfacePoint()));
		}
	}

	private static OverlayFloor nextFloor(UndergroundZone zone, OverlayFloor current)
	{
		OverlayFloor first = null;
		boolean returnNext = false;
		int count = 0;
		for (OverlayFloor floor : OverlayFloor.all())
		{
			if (floor.zone != zone)
			{
				continue;
			}
			count++;
			if (first == null)
			{
				first = floor;
			}
			else if (returnNext)
			{
				return floor;
			}
			if (floor == current)
			{
				returnNext = true;
			}
		}
		return count > 1 ? first : null;
	}

	private void markMapPoint(WorldPoint point)
	{
		camera.flashAt(point);
	}

	private void examinePoi(PoiIndex.Poi poi)
	{
		final PoiDetails.Detail detail = PoiDetails.getDetail(poi, poi.getX(), poi.getY(), poi.getPlane());
		final String text = detail != null && !detail.getLines().isEmpty()
			? detail.getTitle() + ": " + detail.getLines().get(0)
			: poi.getName();
		client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", text, null);
	}


	/** True while RuneLite's context menu owns mouse clicks. */
	public boolean isClientMenuOpen()
	{
		return client != null && client.isMenuOpen();
	}

	@Provides
	BetterMapConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(BetterMapConfig.class);
	}
}
