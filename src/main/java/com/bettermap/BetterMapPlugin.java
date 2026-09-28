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

import com.bettermap.data.sailing.BoatTracker;
import com.bettermap.data.OverlayFloor;
import com.bettermap.data.TravelData;
import com.bettermap.data.UndergroundZone;
import com.bettermap.map.ClueScrollTracker;
import com.bettermap.map.DungeonPieceIndex;
import com.bettermap.map.FinderKeyCapture;
import com.bettermap.map.MapCamera;
import com.bettermap.map.MapFinder;
import com.bettermap.map.MonsterIconManager;
import com.bettermap.map.MonsterIndex;
import com.bettermap.map.PoiIndex;
import com.bettermap.map.PoiDetails;
import com.bettermap.map.PrifddinasShift;
import java.util.ArrayList;
import java.util.List;
import net.runelite.api.MenuEntry;
import com.bettermap.map.QuestHelperTracker;
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
import com.bettermap.ui.QuickFinderOverlay;
import com.google.inject.Provides;
import java.awt.Rectangle;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Future;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.HashTable;
import net.runelite.api.MenuAction;
import net.runelite.api.Player;
import net.runelite.api.WidgetNode;
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
import net.runelite.client.input.KeyManager;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.worldmap.WorldMapOverlay;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
	name = "Better Map",
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
	private BetterMapConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private MouseManager mouseManager;

	@Inject
	private KeyManager keyManager;

	@Inject
	private BetterWorldMapOverlay mapOverlay;

	@Inject
	private QuickFinderOverlay quickFinderOverlay;

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
	private int inputHeartbeatTick;

	private Future<?> startupTask;

	@Override
	protected void startUp() throws Exception
	{
		mapAssets.startUp(() -> getPluginDirectory().join("map-assets"), config.downloadMapAssets());

		camera.setFinderOrbOffset(config.finderOrbOffsetX(), config.finderOrbOffsetY());

		overlayManager.add(mapOverlay);
		overlayManager.add(quickFinderOverlay);
		mouseManager.registerMouseListener(input);
		mouseManager.registerMouseWheelListener(input);
		keyManager.registerKeyListener(input);
		syncRuneliteOverlay();

		panel = new BetterMapPanel(this, poiIndex, monsterIndex, boatTracker);
		navButton = NavigationButton.builder()
			.tooltip("Better Map")
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
		if (startupTask != null)
		{
			startupTask.cancel(true);
			startupTask = null;
		}
		panel = null;
		overlayManager.remove(mapOverlay);
		overlayManager.remove(quickFinderOverlay);
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
			// Unconditional: leaving the chatbox or the map render hidden after the plugin stops
			// is a HUD the user cannot get back without a relog.
		});
		mapAssets.shutDown();

		log.info("Better Map stopped");
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
		log.debug("[BetterMap] config      : customMap={} fullscreen={} hideGameRender={} skillingIcons={} ",
			config.useCustomMap(), config.fullscreenMap(), config.hideGameMapRender(),
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
		// back the client leaves them alone. Skipping the no-op restore keeps six widget lookups
		// per frame off the path a player is on whenever the map is closed - which is most of it.
		final boolean hideMap = config.useCustomMap() && config.hideGameMapRender();
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

	public void closeMap()
	{
		log.debug("[BetterMap:close] closeMap() requested  camera.active={} fullscreen={}",
			camera.isActive(), config.fullscreenMap());
		camera.setClosing(true);
		clientThread.invoke(this::closeMapOnClientThread);
	}

	private void closeMapOnClientThread()
	{
		final Widget closeWidget = client.getWidget(InterfaceID.Worldmap.CLOSE);
		final Widget mapWindow = client.getWidget(InterfaceID.Worldmap.WINDOW);
		final Widget mapContainer = client.getWidget(InterfaceID.Worldmap.MAP_CONTAINER);
		final Widget mapDisplay = client.getWidget(InterfaceID.Worldmap.MAP_DISPLAY);
		final Widget mapOverlayWidget = client.getWidget(InterfaceID.Worldmap.MAP_OVERLAY);

		log.debug("[BetterMap:close] BEFORE  CLOSE={} WINDOW={} MAP_CONTAINER={} DISPLAY={} OVERLAY={}",
			widgetState(closeWidget), widgetState(mapWindow), widgetState(mapContainer),
			widgetState(mapDisplay), widgetState(mapOverlayWidget));

		// Fire the close button's onOp listener to run the engine's official close CS2 script.
		// This cleans up map state and flips the minimap orb action back to "Open" in one step.
		boolean triggeredScript = false;
		if (closeWidget != null)
		{
			final Object[] listener = closeWidget.getOnOpListener();
			if (listener != null)
			{
				try
				{
					client.createScriptEventBuilder(listener)
						.setSource(closeWidget)
						.setOp(1)
						.build()
						.run();
					triggeredScript = true;
					log.debug("[BetterMap:close] closeWidget OnOpListener executed");
				}
				catch (RuntimeException e)
				{
					log.warn("[BetterMap:close] failed to run closeWidget OnOpListener", e);
				}
			}
		}

		// Fall back to direct interface detachment if the script was unavailable or map remains open
		boolean closed = triggeredScript && !isWorldMapOpen();
		if (!closed)
		{
			closed = closeWorldMapInterface();
			if (isWorldMapOpen())
			{
				log.warn("[BetterMap:close] still open — retrying closeInterface");
				closed = closeWorldMapInterface() || closed;
			}
		}

		log.debug("[BetterMap:close] AFTER  closed={} stillOpen={} MAP_CONTAINER={} WINDOW={}",
			closed, isWorldMapOpen(),
			widgetState(client.getWidget(InterfaceID.Worldmap.MAP_CONTAINER)),
			widgetState(client.getWidget(InterfaceID.Worldmap.WINDOW)));

		if (!isWorldMapOpen())
		{
			camera.setClosing(false);
			camera.setActive(false);
			mapOverlay.onMapClosed();
			log.debug("[BetterMap:close] map interface gone — mouse grab released");
		}
		else
		{
			// Hold the closing latch so fullscreen overlay cannot re-arm input.
			log.warn("[BetterMap:close] map still open — holding closing latch (mouse passthrough)");
		}
	}

	private boolean closeWorldMapInterface()
	{
		final HashTable<WidgetNode> table = client.getComponentTable();
		if (table == null)
		{
			log.warn("[BetterMap:close] component table null");
			return false;
		}

		for (WidgetNode node : table)
		{
			if (node != null && node.getId() == InterfaceID.WORLDMAP)
			{
				try
				{
					client.closeInterface(node, true);
					log.debug("[BetterMap:close] closeInterface(WORLDMAP, unload=true) ok  modalMode={}",
						node.getModalMode());
					return true;
				}
				catch (RuntimeException e)
				{
					log.warn("[BetterMap:close] closeInterface failed", e);
					return false;
				}
			}
		}

		log.warn("[BetterMap:close] no WidgetNode for InterfaceID.WORLDMAP in component table");
		return false;
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
		if (hidden && config.hideMinimapOnOpen())
		{
			hideWidget(InterfaceID.Worldmap.OVERVIEW_CONTAINER, true);
			hideWidget(InterfaceID.Worldmap.OVERVIEW_DISPLAY, true);
		}
		else if (!hidden)
		{
			// Always restore overview on close / shutDown, even if the config was toggled off
			// while the map was open (otherwise those widgets can stick SelfHidden until relog).
			hideWidget(InterfaceID.Worldmap.OVERVIEW_CONTAINER, false);
			hideWidget(InterfaceID.Worldmap.OVERVIEW_DISPLAY, false);
		}
	}

	private void hideWidget(int id, boolean hidden)
	{
		final Widget widget = client.getWidget(id);
		if (widget != null && widget.isSelfHidden() != hidden)
		{
			widget.setHidden(hidden);
			// Only on a transition: this runs every frame, and hiding is one of the few things
			// here that depends on the client's widget ids still being what we think they are.
			log.debug("[BetterMap] widget {} hidden={}", id, hidden);
		}
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
		questHelperTracker.update();
		echoFinderResults();

		final boolean worldMapOpen = isWorldMapOpen();

		if (camera.isClosing())
		{
			if (!isWorldMapOpen())
			{
				camera.setClosing(false);
				camera.setActive(false);
				mapOverlay.onMapClosed();
				log.debug("[BetterMap:close] tick — interface gone, latch cleared");
			}
			else
			{
				inputHeartbeatTick++;
				if (inputHeartbeatTick == 1 || inputHeartbeatTick % 5 == 0)
				{
					log.warn("[BetterMap:close] tick — still open, retrying closeInterface (n={})", inputHeartbeatTick);
					closeWorldMapInterface();
				}
				if (inputHeartbeatTick >= 15)
				{
					log.error("[BetterMap:close] giving up latch after {} ticks — releasing mouse (map may still show)", inputHeartbeatTick);
					camera.setClosing(false);
					camera.setActive(false);
					inputHeartbeatTick = 0;
				}
			}
		}
		else if (!worldMapOpen && camera.isActive())
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
				log.debug("[BetterMap:heartbeat] ACTIVE grab  fullscreen={} viewport={} map={} closeBtn={}",
					config.fullscreenMap(),
					view == null ? "null" : view.width + "x" + view.height,
					widgetState(map),
					camera.getCloseButton());
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

		if (!"bettermap".equals(event.getGroup()))
		{
			return;
		}

		if ("useCustomMap".equals(event.getKey()))
		{
			syncRuneliteOverlay();
		}
		if ("downloadMapAssets".equals(event.getKey()))
		{
			mapAssets.startUp(() -> getPluginDirectory().join("map-assets"), config.downloadMapAssets());
		}
		if ("showSidebarPanel".equals(event.getKey()))
		{
			syncSidebarPanel();
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

	/**
	 * Detaches RuneLite's own world map overlay while ours is in charge, and puts it back when it
	 * is not. Toggling the setting used to only take effect on the next plugin restart.
	 */
	private void syncRuneliteOverlay()
	{
		if (config.useCustomMap() && !suppressedRuneliteOverlay)
		{
			overlayManager.remove(runeliteWorldMapOverlay);
			suppressedRuneliteOverlay = true;
		}
		else if (!config.useCustomMap() && suppressedRuneliteOverlay)
		{
			overlayManager.add(runeliteWorldMapOverlay);
			suppressedRuneliteOverlay = false;
		}
	}

	@Subscribe
	public void onPluginChanged(PluginChanged event)
	{
		boatTracker.onPluginChanged(event);
	}

	public BoatTracker getBoatTracker()
	{
		return boatTracker;
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
		camera.centerOn(point.getX(), point.getY());
		camera.setPlane(point.getPlane());
		camera.setZoom(Math.max(camera.getZoom(), REVEAL_ZOOM));
		camera.flashAt(point);

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

	/** Persist a dragged quick-find orb position (canvas-pixel offset from its default spot). */
	public void setFinderOrbOffset(int x, int y)
	{
		if (camera != null)
		{
			camera.setFinderOrbOffset(x, y);
		}
		if (configManager != null)
		{
			configManager.setConfiguration("bettermap", "finderOrbOffsetX", x);
			configManager.setConfiguration("bettermap", "finderOrbOffsetY", y);
		}
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
			final String head = i == 0 ? "Better Map \"" + query.trim() + "\": " : "  ";
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
		final Rectangle viewport = camera.getViewport();
		if (!camera.isActive() || viewport == null || !viewport.contains(mouse.getX(), mouse.getY()))
		{
			return;
		}

		final int worldX = (int) Math.floor(camera.worldX(mouse.getX(), viewport));
		final int worldY = (int) Math.floor(camera.worldY(mouse.getY(), viewport));
		final WorldPoint targetPoint = PrifddinasShift.toWorld(worldX, worldY, camera.getPlane());

		resetMapMenu();

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

	private void resetMapMenu()
	{
		final List<MenuEntry> clean = new ArrayList<>();
		for (MenuEntry entry : client.getMenu().getMenuEntries())
		{
			if (entry.getType() == MenuAction.CANCEL || "Cancel".equalsIgnoreCase(entry.getOption()))
			{
				clean.add(entry);
				break;
			}
		}
		if (clean.isEmpty() && client.getMenu().getMenuEntries().length > 0)
		{
			clean.add(client.getMenu().getMenuEntries()[0]);
		}
		client.getMenu().setMenuEntries(clean.toArray(new MenuEntry[0]));
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
		if (target.isRegion())
		{
			client.getMenu().createMenuEntry(-1)
				.setOption("Preview layer")
				.setTarget("<col=ffff00>" + target.getCluster().name + "</col>")
				.setType(MenuAction.RUNELITE)
				.onClick(e -> camera.setActiveOverlayCluster(target.getCluster()));
			return;
		}

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
						camera.setUndergroundMode(zone, target.getSurfacePoint());
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

	public BetterMapConfig getConfig()
	{
		return config;
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
