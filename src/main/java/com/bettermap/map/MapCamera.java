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
package com.bettermap.map;

import com.bettermap.data.TravelData;
import com.bettermap.data.OverlayCluster;
import com.bettermap.data.OverlayFloor;
import com.bettermap.data.UndergroundZone;
import java.awt.Rectangle;
import java.util.Collections;
import java.util.List;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;

/**
 * Our own world map camera. Once the plugin takes the map over this - not the client - decides
 * what is on screen, which is what lets the map zoom all the way out to the whole world instead
 * of stopping where the game's own map stops.
 *
 * <p>Zoom is measured in screen pixels per game tile. The wiki tiles are native between 0.125
 * (zoom -3) and 8 (zoom 3); we allow a little beyond each end and upscale or downscale.
 *
 * <p>The viewport is written every frame by the overlay on the client thread and read by the
 * input listener on the AWT thread, hence the volatile.
 */
@Slf4j
@Singleton
public class MapCamera
{
	public static final double MIN_ZOOM = 0.14;
	public static final double MAX_ZOOM = 24.0;

	public static final int MIN_WORLD_X = 896;
	public static final int MAX_WORLD_X = 4096;
	public static final int MIN_WORLD_Y = 1984;
	public static final int MAX_WORLD_Y = 12607;

	public static final class LayerSymbolTarget
	{
		private final Rectangle bounds;
		private final UndergroundZone zone;
		private final WorldPoint surfacePoint;
		private final OverlayCluster cluster;
		private final OverlayFloor floor;
		private final boolean surfaceToUnderground;

		public LayerSymbolTarget(Rectangle bounds, UndergroundZone zone, boolean surfaceToUnderground)
		{
			this(bounds, zone, zone == null ? null : zone.getSurfacePoint(), surfaceToUnderground);
		}

		public LayerSymbolTarget(Rectangle bounds, UndergroundZone zone, WorldPoint surfacePoint,
			boolean surfaceToUnderground)
		{
			this.bounds = bounds;
			this.zone = zone;
			this.surfacePoint = surfacePoint;
			this.cluster = null;
			this.floor = null;
			this.surfaceToUnderground = surfaceToUnderground;
		}

		public LayerSymbolTarget(Rectangle bounds, OverlayCluster cluster)
		{
			this.bounds = bounds;
			this.zone = null;
			this.surfacePoint = null;
			this.cluster = cluster;
			this.floor = null;
			this.surfaceToUnderground = true;
		}

		public LayerSymbolTarget(Rectangle bounds, OverlayFloor floor)
		{
			this.bounds = bounds;
			this.zone = floor.zone;
			this.surfacePoint = floor.zone.getSurfacePoint();
			this.cluster = null;
			this.floor = floor;
			this.surfaceToUnderground = true;
		}

		public Rectangle getBounds()
		{
			return bounds;
		}

		public UndergroundZone getZone()
		{
			return zone;
		}

		public WorldPoint getSurfacePoint()
		{
			return surfacePoint;
		}

		public OverlayCluster getCluster()
		{
			return cluster;
		}

		public OverlayFloor getFloor()
		{
			return floor;
		}

		public boolean isRegion()
		{
			return cluster != null;
		}

		public boolean isFloor()
		{
			return floor != null;
		}

		public boolean isSurfaceToUnderground()
		{
			return surfaceToUnderground;
		}
	}

	/** A single checkbox row in the on-map "Layers" panel, published for the input listener to hit-test. */
	public static final class LayerToggleTarget
	{
		private final Rectangle bounds;
		private final String configKey;
		private final boolean currentValue;

		public LayerToggleTarget(Rectangle bounds, String configKey, boolean currentValue)
		{
			this.bounds = bounds;
			this.configKey = configKey;
			this.currentValue = currentValue;
		}

		public Rectangle getBounds()
		{
			return bounds;
		}

		public String getConfigKey()
		{
			return configKey;
		}

		public boolean getCurrentValue()
		{
			return currentValue;
		}
	}

	/** One result row in the on-map Finder: the row body jumps, the walk chip hands off to Shortest Path. */
	public static final class FinderResultTarget
	{
		private final Rectangle rowBounds;
		private final Rectangle walkBounds;
		private final Rectangle mapBounds;
		private final WorldPoint point;
		private final String name;
		private final MapFinder.Result result;
		private final MapRegion region;

		public FinderResultTarget(Rectangle rowBounds, Rectangle walkBounds, WorldPoint point, String name)
		{
			this(rowBounds, walkBounds, point, name, null, null);
		}

		public FinderResultTarget(Rectangle rowBounds, Rectangle walkBounds, WorldPoint point, String name, MapFinder.Result result)
		{
			this(rowBounds, walkBounds, point, name, result, null);
		}

		public FinderResultTarget(Rectangle rowBounds, Rectangle walkBounds, WorldPoint point, String name, MapFinder.Result result, MapRegion region)
		{
			this(rowBounds, walkBounds, null, point, name, result, region);
		}

		public FinderResultTarget(Rectangle rowBounds, Rectangle walkBounds, Rectangle mapBounds, WorldPoint point, String name, MapFinder.Result result, MapRegion region)
		{
			this.rowBounds = rowBounds;
			this.walkBounds = walkBounds;
			this.mapBounds = mapBounds;
			this.point = point;
			this.name = name;
			this.result = result;
			this.region = region;
		}

		public Rectangle getRowBounds()
		{
			return rowBounds;
		}

		public Rectangle getWalkBounds()
		{
			return walkBounds;
		}

		public Rectangle getMapBounds()
		{
			return mapBounds;
		}

		public WorldPoint getPoint()
		{
			return point;
		}

		public String getName()
		{
			return name;
		}

		public MapFinder.Result getResult()
		{
			return result;
		}

		public MapRegion getRegion()
		{
			return region;
		}
	}

	/** One row in the Finder flyout submenu card. */
	public static final class FlyoutTarget
	{
		private final Rectangle rowBounds;
		private final Rectangle walkBounds;
		private final Rectangle mapBounds;
		private final WorldPoint point;
		private final String name;
		private final MapFinder.Result result;

		public FlyoutTarget(Rectangle rowBounds, Rectangle walkBounds, WorldPoint point, String name, MapFinder.Result result)
		{
			this(rowBounds, walkBounds, null, point, name, result);
		}

		public FlyoutTarget(Rectangle rowBounds, Rectangle walkBounds, Rectangle mapBounds, WorldPoint point, String name, MapFinder.Result result)
		{
			this.rowBounds = rowBounds;
			this.walkBounds = walkBounds;
			this.mapBounds = mapBounds;
			this.point = point;
			this.name = name;
			this.result = result;
		}

		public FlyoutTarget(Rectangle rowBounds, Rectangle walkBounds, WorldPoint point, String name)
		{
			this(rowBounds, walkBounds, point, name, null);
		}

		public Rectangle getRowBounds()
		{
			return rowBounds;
		}

		public Rectangle getWalkBounds()
		{
			return walkBounds;
		}

		public Rectangle getMapBounds()
		{
			return mapBounds;
		}

		public WorldPoint getPoint()
		{
			return point;
		}

		public String getName()
		{
			return name;
		}

		public MapFinder.Result getResult()
		{
			return result;
		}
	}

	/**
	 * The player's position in map coordinates, republished each game tick by
	 * {@code BetterMapPlugin#onGameTick}.
	 *
	 * <p>Inside an instance this is <i>not</i> {@code Actor#getWorldLocation} - that reports the
	 * instance's private copy of the scene, which usually sits in unmapped space. The plugin
	 * resolves it back through the instance template chunks so the map centres, the plane
	 * switcher and the player arrow all land on the real rooms the instance was copied from.
	 *
	 * <p>Cached rather than read live because the render and Swing paths that need it are not on
	 * the client thread, and {@code getWorldLocation} asserts that they are.
	 */
	private volatile WorldPoint playerLocation;

	/**
	 * Last position the player held on the overworld surface (not instanced, at or below
	 * {@link InstanceMaps#GAP_MIN_Y}). Written on the client thread, read from Swing. Used as the
	 * surface anchor for instances with no {@link com.bettermap.data.UndergroundZone} mapping: the
	 * player marker projects here and {@link #exitDungeonToSurface()} lands here.
	 */
	private volatile WorldPoint lastSurfaceLocation;

	/** True while the local player stands in an instanced region. Written client thread, read Swing. */
	private volatile boolean playerInInstance;
	/** Last mapped interior observed while the map was open; used to react only to transitions. */
	private volatile UndergroundZone observedPlayerUndergroundZone;
	private volatile int observedPlayerPlane = -1;
	/** True when the player is above the overworld but no reliable authored interior owns the point. */
	private volatile boolean interiorMapUnavailable;

	private volatile Rectangle viewport;
	private volatile boolean active;
	/** Set while closeMap is in progress so the overlay cannot re-arm the mouse grab. */
	private volatile boolean closing;
	private volatile Rectangle[] planeButtons = new Rectangle[0];
	private volatile Rectangle closeButton;
	/** Screen rects where clicks pass through to the client (minimap overview, world-map orb). */
	private volatile Rectangle[] nativePassthrough = new Rectangle[0];
	private volatile Rectangle undergroundReturnButton;
	/** "Go to clue" button, shown while the clue scroll plugin has a solved target. */
	private volatile Rectangle clueButton;
	private volatile Rectangle questButton;
	/** "Go to Player" chip: pans and flashes the map on the local player. */
	private volatile Rectangle playerButton;
	/** Collapsible clue panel on fullscreen map. */
	private volatile boolean cluePanelCollapsed;
	private volatile Rectangle cluePanelBounds;
	private volatile Rectangle cluePanelHeaderBounds;
	private volatile Rectangle cluePanelPanButton;
	private volatile List<LayerSymbolTarget> layerSymbolTargets = Collections.emptyList();

	/** On-map "Layers" panel: the gear toggle, whether the panel is expanded, and its rows. */
	private volatile Rectangle layersButton;
	private volatile boolean layersPanelOpen;
	private volatile List<LayerToggleTarget> layerToggleTargets = Collections.emptyList();
	/** Outer bounds of the expanded panel, so the overlay can suppress hovercards behind it. */
	private volatile Rectangle layersPanelBounds;

	/**
	 * Chat toggle in the top-right strip. In fullscreen the game's chatbox draws over our map and
	 * our own viewport swallows every click on it, so its minimise control is unreachable — this
	 * button is the way out. Transient on purpose: {@link #setActive(boolean)} clears it, so
	 * closing the map always gives the chat back.
	 */
	private volatile Rectangle chatButton;
	private volatile boolean chatHidden;

	private volatile Rectangle finderButton;
	/** The quick-find orb by the minimap, published by {@code QuickFinderOverlay} while the map is closed. */
	private volatile Rectangle quickFinderOrb;
	/** User drag offset for the quick-find orb, in canvas pixels from its default spot below the world-map orb. */
	private volatile int finderOrbOffsetX;
	private volatile int finderOrbOffsetY;
	private volatile boolean finderPanelOpen;
	/**
	 * True while the finder card is up on its own, without the world map behind it - the minimap's
	 * search button opens it this way.
	 *
	 * <p>Kept apart from {@link #finderPanelOpen} because every other consumer of the finder gates
	 * on the map being active, and standalone mode is exactly the case where it is not. Anything
	 * that asks "may the finder take this click / this keystroke" wants
	 * {@link #isFinderInteractive()}, not {@code isActive()}.
	 */
	private volatile boolean finderStandalone;
	/**
	 * True while the search field owns the keyboard. Distinct from {@link #finderPanelOpen}: the
	 * panel can stay up while a click on the map drops focus, which is what releases the global
	 * key grab in {@code FinderKeyCapture} without closing the card.
	 */
	private volatile boolean finderFieldFocused;
	/**
	 * Whether Shift is down right now, latched by {@code WorldMapInput}'s key listener. The finder's
	 * chatbox input reports Enter through a {@code Consumer<String>} that carries no modifiers, so
	 * this is how Shift+Enter is told apart from plain Enter.
	 */
	private volatile boolean shiftHeld;
	private volatile Rectangle finderPanelBounds;
	private volatile Rectangle finderFieldBounds;
	private volatile List<FinderResultTarget> finderResultTargets = Collections.emptyList();
	private volatile Rectangle finderChipBank;
	private volatile Rectangle finderChipSlayer;
	private volatile Rectangle finderChipTravel;
	private volatile Rectangle finderChipLast;
	private volatile String hoveredRowKey;
	private volatile Rectangle finderFlyoutBounds;
	/** Flyout panel plus bridge corridor to the hovered row — keeps hover sticky while crossing the gap. */
	private volatile Rectangle finderFlyoutHitBounds;
	private volatile List<FlyoutTarget> flyoutTargets = Collections.emptyList();

	/** Finder body rows visible before the user hits Expand. */
	public static final int FINDER_BODY_ROWS = 10;
	/** Finder body rows visible while expanded. */
	public static final int FINDER_BODY_ROWS_EXPANDED = 18;
	/** Flyout submenu rows visible at once. */
	public static final int FINDER_FLYOUT_ROWS = 12;
	/** Pixels from the top/bottom edge that trigger auto-scroll while hovered. */
	public static final int FINDER_SCROLL_EDGE_PX = 14;
	/** Minimum milliseconds between edge-scroll steps. */
	public static final long FINDER_EDGE_SCROLL_MS = 80;
	/** Hover time on an expandable row before the flyout submenu opens. */
	public static final long FINDER_FLYOUT_DWELL_MS = 1000L;

	private volatile int finderBodyScrollOffset;
	private volatile int finderFlyoutScrollOffset;
	private volatile boolean finderBodyExpanded;
	private volatile int finderBodyItemCount;
	private volatile int finderFlyoutItemCount;
	private volatile String finderScrollKey = "";
	private volatile Rectangle finderBodyViewport;
	private volatile Rectangle finderFlyoutViewport;
	private volatile Rectangle finderExpandButton;
	private volatile long finderLastEdgeScrollMs;

	private volatile WorldPoint flashPoint;
	private volatile long flashStartMillis;

	private volatile UndergroundZone activeUndergroundZone;
	private volatile WorldPoint activeUndergroundSurfacePoint;
	private volatile OverlayCluster activeOverlayCluster;
	private volatile UndergroundZone hoveredUndergroundZone;
	private volatile OverlayCluster hoveredOverlayCluster;
	private volatile Integer hoveredFloorPlane;
	private volatile Integer hoveredFloorLayer;
	private volatile Integer activeFloorLayer;
	private volatile boolean hoveredSurfaceToUnderground = true;
	private volatile TravelData.TravelNode hoveredTravelNode;
	private volatile TravelData.TravelNode selectedTravelNode;

	private volatile int plane;
	private volatile double centerX = 3222;
	private volatile double centerY = 3218;
	private volatile double zoom = 2.0;
	private volatile Double savedSurfaceZoom;
	private volatile boolean planeChosenByUser;

	// The live fields above are written by the AWT input thread (drag/wheel) while the overlay
	// projects the camera dozens of times per frame on the client thread. The projection methods
	// (screenX/screenY/worldX/worldY) read these frozen copies instead, so a pan or zoom landing
	// mid-frame cannot tear the tile grid along a seam. beginFrame() refreshes them once, at the
	// top of each render; the plain getters still report the live pose.
	private volatile double frameCenterX = centerX;
	private volatile double frameCenterY = centerY;
	private volatile double frameZoom = zoom;

	/** Snapshot the live pose for this frame. Call once per render, before any projection. */
	public synchronized void beginFrame()
	{
		frameCenterX = centerX;
		frameCenterY = centerY;
		frameZoom = zoom;
	}

	public int getPlane()
	{
		return plane;
	}

	public double getCenterX()
	{
		return centerX;
	}

	public double getCenterY()
	{
		return centerY;
	}

	public double getZoom()
	{
		return zoom;
	}

	/** The zoom frozen for the current frame by {@link #beginFrame()} — matches what is projected. */
	public double getFrameZoom()
	{
		return frameZoom;
	}

	public Rectangle getViewport()
	{
		return viewport;
	}

	public void setViewport(Rectangle viewport)
	{
		this.viewport = viewport;
	}

	/** True while the overlay is actually drawing, so input knows whether to grab events. */
	public boolean isActive()
	{
		return active;
	}

	public void setActive(boolean active)
	{
		if (closing && active)
		{
			log.debug(
				"[BetterMap:camera] ignoring setActive(true) — close in progress");
			return;
		}
		// Overlay render calls setActive(true) every frame while the map is open. Only the
		// inactive → active edge should dismiss the standalone quick-find card; repeating the
		// wipe while already active closes the in-map Find panel on the next frame.
		final boolean becomingActive = active && !this.active;
		if (this.active != active)
		{
			final Rectangle view = viewport;
			log.debug(
				"[BetterMap:camera] active {} -> {}  viewport={}  closeBtn={}",
				this.active, active,
				view == null ? "null" : view.x + "," + view.y + " " + view.width + "x" + view.height,
				closeButton);
		}
		this.active = active;
		if (becomingActive)
		{
			// Opening the world map dismisses the standalone quick-find card - the in-map Find
			// button owns the finder from here.
			finderStandalone = false;
			finderPanelOpen = false;
			finderFieldFocused = false;
			clearFinderRegions();
		}
		if (!active)
		{
			// Drop UI hit targets so a stale close/plane rect cannot swallow clicks next frame.
			closeButton = null;
			nativePassthrough = new Rectangle[0];
			undergroundReturnButton = null;
			clueButton = null;
			questButton = null;
			cluePanelBounds = null;
			cluePanelHeaderBounds = null;
			cluePanelPanButton = null;
			planeButtons = new Rectangle[0];
			hoveredUndergroundZone = null;
			hoveredOverlayCluster = null;
			hoveredFloorPlane = null;
			hoveredTravelNode = null;
			selectedTravelNode = null;
			layersButton = null;
			layerToggleTargets = Collections.emptyList();
			layerSymbolTargets = Collections.emptyList();
			layersPanelOpen = false;
			layersPanelBounds = null;
			chatButton = null;
			chatHidden = false;   // never leave the chat hidden behind a closed map
			finderButton = null;
			planeChosenByUser = false;
			// The quick-find orb keeps its card up while the map is closed: setActive(false) runs
			// every frame in that state, so wiping the finder surface here would kill it instantly.
			if (!finderStandalone)
			{
				finderPanelOpen = false;
				finderFieldFocused = false;
				clearFinderRegions();
				hoveredRowKey = null;
				resetFinderScroll();
			}
			clearFlash();
			savedSurfaceZoom = null;
		}
	}

	public boolean isClosing()
	{
		return closing;
	}

	public void setClosing(boolean closing)
	{
		if (this.closing != closing)
		{
			log.debug(
				"[BetterMap:camera] closing {} -> {}", this.closing, closing);
		}
		this.closing = closing;
		if (closing)
		{
			setActive(false);
		}
	}

	/** An explicit choice from the plane switcher, which then sticks. */
	public synchronized void setPlane(int plane)
	{
		if (this.plane != Math.max(0, Math.min(3, plane)))
		{
			selectedTravelNode = null;
			hoveredTravelNode = null;
		}
		this.activeFloorLayer = null;
		this.plane = Math.max(0, Math.min(3, plane));
		this.planeChosenByUser = true;
	}

	/** Follows the player's floor, but never overrides a plane the user picked. */
	public synchronized void setPlaneFromPlayer(int plane)
	{
		if (!planeChosenByUser)
		{
			this.plane = Math.max(0, Math.min(3, plane));
		}
	}

	public WorldPoint getPlayerLocation()
	{
		return playerLocation;
	}

	/**
	 * The player'''s location projected to the overworld display coordinates.
	 * If inside a dungeon or instance above GAP_MIN_Y, projects down using UndergroundZone/InstanceMaps,
	 * or falls back to lastSurfaceLocation.
	 */
	public WorldPoint getPlayerDisplayLocation()
	{
		final WorldPoint loc = playerLocation;
		if (loc == null)
		{
			return null;
		}
		if (loc.getY() <= InstanceMaps.GAP_MIN_Y)
		{
			return loc;
		}
		final int dispX = InstanceMaps.toDisplayX(loc.getX(), loc.getY(), 0, 0);
		final int dispY = InstanceMaps.toDisplayY(loc.getX(), loc.getY(), 0, 0);
		if (dispY <= InstanceMaps.GAP_MIN_Y)
		{
			return new WorldPoint(dispX, dispY, loc.getPlane());
		}
		if (lastSurfaceLocation != null)
		{
			return lastSurfaceLocation;
		}
		return new WorldPoint(dispX, dispY, loc.getPlane());
	}

	public void setPlayerLocation(WorldPoint playerLocation)
	{
		this.playerLocation = playerLocation;
	}

	public WorldPoint getLastSurfaceLocation()
	{
		return lastSurfaceLocation;
	}

	public void setLastSurfaceLocation(WorldPoint lastSurfaceLocation)
	{
		if (lastSurfaceLocation != null)
		{
			this.lastSurfaceLocation = lastSurfaceLocation;
		}
	}

	public boolean isPlayerInInstance()
	{
		return playerInInstance;
	}

	public void setPlayerInInstance(boolean playerInInstance)
	{
		this.playerInInstance = playerInInstance;
	}

	/**
	 * Follows physical transitions into and out of authored interiors while the map is open.
	 * Reacting only when the observed zone changes preserves a manual "Return to surface" choice
	 * until the player changes area or reopens the map.
	 */
	public synchronized void followPlayerInterior()
	{
		final WorldPoint player = playerLocation;
		final UndergroundZone playerZone = player == null
			|| InstanceMaps.isOverworldOverlay(player.getX(), player.getY())
			? null
			: InstanceMaps.zoneForPoint(player.getX(), player.getY());
		final double displayX = player == null ? centerX
			: InstanceMaps.toDisplayX(player.getX(), player.getY(), 0, 0);
		final double displayY = player == null ? centerY
			: InstanceMaps.toDisplayY(player.getX(), player.getY(), 0, 0);
		final boolean displayOnSurface = player != null
			&& !InstanceMaps.hiddenOnSurface(player.getX(), player.getY(), 0, 0)
			&& displayX >= MIN_WORLD_X && displayX <= MAX_WORLD_X
			&& displayY >= MIN_WORLD_Y && displayY <= InstanceMaps.GAP_MIN_Y;
		final boolean unavailable = player != null && playerZone == null && !displayOnSurface;

		final int playerPlane = player == null ? -1 : player.getPlane();
		if (playerZone != observedPlayerUndergroundZone || playerPlane != observedPlayerPlane)
		{
			final UndergroundZone previous = observedPlayerUndergroundZone;
			observedPlayerUndergroundZone = playerZone;
			observedPlayerPlane = playerPlane;
			if (playerZone != null)
			{
				WorldPoint entrance = playerZone.getSurfacePoint();
				if (lastSurfaceLocation != null)
				{
					final WorldPoint nearestEntrance = playerZone.nearestSurfacePoint(
						lastSurfaceLocation.getX(), lastSurfaceLocation.getY());
					final long dx = (long) nearestEntrance.getX() - lastSurfaceLocation.getX();
					final long dy = (long) nearestEntrance.getY() - lastSurfaceLocation.getY();
					if (dx * dx + dy * dy <= 64)
					{
						entrance = nearestEntrance;
					}
				}
				setUndergroundMode(playerZone, entrance);
				setPlane(player.getPlane());
				for (OverlayFloor floor : OverlayFloor.all())
				{
					if (floor.zone == playerZone && floor.plane == player.getPlane())
					{
						setActiveFloor(floor);
						break;
					}
				}
			}
			else if (previous != null && activeUndergroundZone == previous)
			{
				exitDungeonToSurface();
			}
		}

		if (unavailable && !interiorMapUnavailable && lastSurfaceLocation != null)
		{
			centerOnSurfacePoint(lastSurfaceLocation);
		}
		interiorMapUnavailable = unavailable;
	}

	/** Forget transition state when the map closes so reopening follows the player again. */
	public synchronized void resetPlayerInteriorFollow()
	{
		if (observedPlayerUndergroundZone != null
			&& activeUndergroundZone == observedPlayerUndergroundZone)
		{
			clearUndergroundMode();
		}
		observedPlayerUndergroundZone = null;
		observedPlayerPlane = -1;
		interiorMapUnavailable = false;
	}

	public boolean isInteriorMapUnavailable()
	{
		return interiorMapUnavailable;
	}

	/** The interior the player physically occupies, even while they manually view its surface. */
	public UndergroundZone getObservedPlayerUndergroundZone()
	{
		return observedPlayerUndergroundZone;
	}

	/** True after "Return to surface" while the player remains inside a mapped interior. */
	public boolean isViewingPlayerInteriorFromSurface()
	{
		return observedPlayerUndergroundZone != null && activeUndergroundZone == null;
	}

	/** Screen rectangles of the plane switcher, published for the input listener to hit-test. */
	public Rectangle[] getPlaneButtons()
	{
		return planeButtons;
	}

	public void setPlaneButtons(Rectangle[] planeButtons)
	{
		this.planeButtons = planeButtons;
	}

	public Rectangle getCloseButton()
	{
		return closeButton;
	}

	public void setCloseButton(Rectangle closeButton)
	{
		this.closeButton = closeButton;
	}

	public Rectangle[] getNativePassthrough()
	{
		return nativePassthrough;
	}

	public void setNativePassthrough(Rectangle[] nativePassthrough)
	{
		this.nativePassthrough = nativePassthrough != null ? nativePassthrough : new Rectangle[0];
	}

	public Rectangle getUndergroundReturnButton()
	{
		return undergroundReturnButton;
	}

	public void setUndergroundReturnButton(Rectangle undergroundReturnButton)
	{
		this.undergroundReturnButton = undergroundReturnButton;
	}

	public Rectangle getClueButton()
	{
		return clueButton;
	}

	public void setClueButton(Rectangle clueButton)
	{
		this.clueButton = clueButton;
	}

	/** The on-map "Go to Quest Step" chip, or null while there is no Quest Helper step. */
	public Rectangle getQuestButton()
	{
		return questButton;
	}

	public void setQuestButton(Rectangle questButton)
	{
		this.questButton = questButton;
	}


	/** The on-map "Go to Player" chip. Always present while the map is open. */
	public Rectangle getPlayerButton()
	{
		return playerButton;
	}

	public void setPlayerButton(Rectangle playerButton)
	{
		this.playerButton = playerButton;
	}

	public boolean isCluePanelCollapsed()
	{
		return cluePanelCollapsed;
	}

	public void setCluePanelCollapsed(boolean cluePanelCollapsed)
	{
		this.cluePanelCollapsed = cluePanelCollapsed;
	}

	public Rectangle getCluePanelBounds()
	{
		return cluePanelBounds;
	}

	public void setCluePanelBounds(Rectangle cluePanelBounds)
	{
		this.cluePanelBounds = cluePanelBounds;
	}

	public Rectangle getCluePanelHeaderBounds()
	{
		return cluePanelHeaderBounds;
	}

	public void setCluePanelHeaderBounds(Rectangle cluePanelHeaderBounds)
	{
		this.cluePanelHeaderBounds = cluePanelHeaderBounds;
	}

	public Rectangle getCluePanelPanButton()
	{
		return cluePanelPanButton;
	}

	public void setCluePanelPanButton(Rectangle cluePanelPanButton)
	{
		this.cluePanelPanButton = cluePanelPanButton;
	}

	private final DungeonTuner dungeonTuner = new DungeonTuner();

	/** Live keyboard-driven dungeon-layer nudges (see {@link com.bettermap.BetterMapConfig#undergroundTuner()}). */
	public DungeonTuner getDungeonTuner()
	{
		return dungeonTuner;
	}

	public List<LayerSymbolTarget> getLayerSymbolTargets()
	{
		return layerSymbolTargets;
	}

	public void setLayerSymbolTargets(List<LayerSymbolTarget> targets)
	{
		this.layerSymbolTargets = targets != null ? targets : Collections.emptyList();
	}

	public Rectangle getLayersButton()
	{
		return layersButton;
	}

	public void setLayersButton(Rectangle layersButton)
	{
		this.layersButton = layersButton;
	}

	public boolean isLayersPanelOpen()
	{
		return layersPanelOpen;
	}

	public void setLayersPanelOpen(boolean layersPanelOpen)
	{
		this.layersPanelOpen = layersPanelOpen;
	}

	public List<LayerToggleTarget> getLayerToggleTargets()
	{
		return layerToggleTargets;
	}

	public void setLayerToggleTargets(List<LayerToggleTarget> targets)
	{
		this.layerToggleTargets = targets != null ? targets : Collections.emptyList();
	}

	public Rectangle getLayersPanelBounds()
	{
		return layersPanelBounds;
	}

	public void setLayersPanelBounds(Rectangle layersPanelBounds)
	{
		this.layersPanelBounds = layersPanelBounds;
	}

	public Rectangle getChatButton()
	{
		return chatButton;
	}

	public void setChatButton(Rectangle chatButton)
	{
		this.chatButton = chatButton;
	}

	/** True while the map is holding the game's chatbox hidden. */
	public boolean isChatHidden()
	{
		return chatHidden;
	}

	public void setChatHidden(boolean chatHidden)
	{
		this.chatHidden = chatHidden;
	}

	public Rectangle getFinderButton()
	{
		return finderButton;
	}

	public void setFinderButton(Rectangle finderButton)
	{
		this.finderButton = finderButton;
	}

	public Rectangle getQuickFinderOrb()
	{
		return quickFinderOrb;
	}

	public void setQuickFinderOrb(Rectangle quickFinderOrb)
	{
		this.quickFinderOrb = quickFinderOrb;
	}

	public int getFinderOrbOffsetX()
	{
		return finderOrbOffsetX;
	}

	public int getFinderOrbOffsetY()
	{
		return finderOrbOffsetY;
	}

	public void setFinderOrbOffset(int x, int y)
	{
		this.finderOrbOffsetX = x;
		this.finderOrbOffsetY = y;
	}

	public boolean isFinderPanelOpen()
	{
		return finderPanelOpen;
	}

	public boolean isFinderStandalone()
	{
		return finderStandalone;
	}

	public void setFinderStandalone(boolean finderStandalone)
	{
		this.finderStandalone = finderStandalone;
	}

	/** Whether the finder card is up and should be taking clicks and keys, either way it was opened. */
	public boolean isFinderInteractive()
	{
		return finderPanelOpen && (isActive() || finderStandalone);
	}

	/**
	 * Opening the panel also focuses the field, so the keyboard is grabbed the moment the card
	 * appears and the user can type without a second click. Closing it always drops focus.
	 */
	public void setFinderPanelOpen(boolean finderPanelOpen)
	{
		this.finderPanelOpen = finderPanelOpen;
		this.finderFieldFocused = finderPanelOpen;
		if (!finderPanelOpen)
		{
			this.finderStandalone = false;
			this.hoveredRowKey = null;
			clearFinderRegions();
			resetFinderScroll();
		}
	}

	/**
	 * Drop every Finder hit region, so nothing left on screen last frame can still take a click.
	 *
	 * <p>Called from {@link #setFinderPanelOpen(false)}, {@link #setActive}, and the renderer
	 * when the card stops drawing. Readers in {@code WorldMapInput} are also gated on
	 * {@link #isFinderPanelOpen()}.
	 */
	public void clearFinderRegions()
	{
		finderPanelBounds = null;
		finderFieldBounds = null;
		finderResultTargets = Collections.emptyList();
		finderChipBank = null;
		finderChipSlayer = null;
		finderChipTravel = null;
		finderChipLast = null;
		finderFlyoutBounds = null;
		flyoutTargets = Collections.emptyList();
		finderBodyViewport = null;
		finderFlyoutViewport = null;
		finderFlyoutHitBounds = null;
		finderExpandButton = null;
	}

	/** True while the search field owns the keyboard. See {@code FinderKeyCapture}. */
	public boolean isFinderFieldFocused()
	{
		return finderFieldFocused;
	}

	public void setFinderFieldFocused(boolean finderFieldFocused)
	{
		this.finderFieldFocused = finderFieldFocused;
	}

	/** True while Shift is held. See {@link #shiftHeld}. */
	public boolean isShiftHeld()
	{
		return shiftHeld;
	}

	public void setShiftHeld(boolean shiftHeld)
	{
		this.shiftHeld = shiftHeld;
	}

	public Rectangle getFinderPanelBounds()
	{
		return finderPanelBounds;
	}

	public void setFinderPanelBounds(Rectangle finderPanelBounds)
	{
		this.finderPanelBounds = finderPanelBounds;
	}

	public Rectangle getFinderFieldBounds()
	{
		return finderFieldBounds;
	}

	public void setFinderFieldBounds(Rectangle finderFieldBounds)
	{
		this.finderFieldBounds = finderFieldBounds;
	}


	public List<FinderResultTarget> getFinderResultTargets()
	{
		return finderResultTargets;
	}

	public void setFinderResultTargets(List<FinderResultTarget> targets)
	{
		this.finderResultTargets = targets != null ? targets : Collections.emptyList();
	}

	public Rectangle getFinderChipBank()
	{
		return finderChipBank;
	}

	public void setFinderChipBank(Rectangle finderChipBank)
	{
		this.finderChipBank = finderChipBank;
	}

	public Rectangle getFinderChipSlayer()
	{
		return finderChipSlayer;
	}

	public void setFinderChipSlayer(Rectangle finderChipSlayer)
	{
		this.finderChipSlayer = finderChipSlayer;
	}

	public Rectangle getFinderChipTravel()
	{
		return finderChipTravel;
	}

	public void setFinderChipTravel(Rectangle finderChipTravel)
	{
		this.finderChipTravel = finderChipTravel;
	}

	public Rectangle getFinderChipLast()
	{
		return finderChipLast;
	}

	public void setFinderChipLast(Rectangle finderChipLast)
	{
		this.finderChipLast = finderChipLast;
	}

	public String getHoveredRowKey()
	{
		return hoveredRowKey;
	}

	public void setHoveredRowKey(String hoveredRowKey)
	{
		this.hoveredRowKey = hoveredRowKey;
	}

	public Rectangle getFinderFlyoutBounds()
	{
		return finderFlyoutBounds;
	}

	public void setFinderFlyoutBounds(Rectangle finderFlyoutBounds)
	{
		this.finderFlyoutBounds = finderFlyoutBounds;
	}

	public Rectangle getFinderFlyoutHitBounds()
	{
		return finderFlyoutHitBounds;
	}

	public void setFinderFlyoutHitBounds(Rectangle finderFlyoutHitBounds)
	{
		this.finderFlyoutHitBounds = finderFlyoutHitBounds;
	}

	public List<FlyoutTarget> getFlyoutTargets()
	{
		return flyoutTargets;
	}

	public void setFlyoutTargets(List<FlyoutTarget> targets)
	{
		this.flyoutTargets = targets != null ? targets : Collections.emptyList();
	}

	public int getFinderBodyScrollOffset()
	{
		return finderBodyScrollOffset;
	}

	public void setFinderBodyScrollOffset(int finderBodyScrollOffset)
	{
		this.finderBodyScrollOffset = Math.max(0, finderBodyScrollOffset);
	}

	public int getFinderFlyoutScrollOffset()
	{
		return finderFlyoutScrollOffset;
	}

	public void setFinderFlyoutScrollOffset(int finderFlyoutScrollOffset)
	{
		this.finderFlyoutScrollOffset = Math.max(0, finderFlyoutScrollOffset);
	}

	public boolean isFinderBodyExpanded()
	{
		return finderBodyExpanded;
	}

	public void toggleFinderBodyExpanded()
	{
		this.finderBodyExpanded = !finderBodyExpanded;
		this.finderBodyScrollOffset = 0;
	}

	public void setFinderBodyItemCount(int finderBodyItemCount)
	{
		this.finderBodyItemCount = Math.max(0, finderBodyItemCount);
	}

	public void setFinderFlyoutItemCount(int finderFlyoutItemCount)
	{
		this.finderFlyoutItemCount = Math.max(0, finderFlyoutItemCount);
	}

	public String getFinderScrollKey()
	{
		return finderScrollKey;
	}

	public void setFinderScrollKey(String finderScrollKey)
	{
		this.finderScrollKey = finderScrollKey != null ? finderScrollKey : "";
	}

	public Rectangle getFinderBodyViewport()
	{
		return finderBodyViewport;
	}

	public void setFinderBodyViewport(Rectangle finderBodyViewport)
	{
		this.finderBodyViewport = finderBodyViewport;
	}

	public Rectangle getFinderFlyoutViewport()
	{
		return finderFlyoutViewport;
	}

	public void setFinderFlyoutViewport(Rectangle finderFlyoutViewport)
	{
		this.finderFlyoutViewport = finderFlyoutViewport;
	}

	public Rectangle getFinderExpandButton()
	{
		return finderExpandButton;
	}

	public void setFinderExpandButton(Rectangle finderExpandButton)
	{
		this.finderExpandButton = finderExpandButton;
	}

	public long getFinderLastEdgeScrollMs()
	{
		return finderLastEdgeScrollMs;
	}

	public void setFinderLastEdgeScrollMs(long finderLastEdgeScrollMs)
	{
		this.finderLastEdgeScrollMs = finderLastEdgeScrollMs;
	}

	public void resetFinderScroll()
	{
		finderBodyScrollOffset = 0;
		finderFlyoutScrollOffset = 0;
		finderBodyExpanded = false;
		finderBodyItemCount = 0;
		finderFlyoutItemCount = 0;
		finderScrollKey = "";
		finderBodyViewport = null;
		finderFlyoutViewport = null;
		finderExpandButton = null;
		finderFlyoutHitBounds = null;
		finderLastEdgeScrollMs = 0L;
	}

	public int finderBodyViewportRows()
	{
		return finderBodyExpanded ? FINDER_BODY_ROWS_EXPANDED : FINDER_BODY_ROWS;
	}

	public void clampFinderBodyScroll()
	{
		final int viewport = finderBodyViewportRows();
		final int maxOffset = Math.max(0, finderBodyItemCount - viewport);
		if (finderBodyScrollOffset > maxOffset)
		{
			finderBodyScrollOffset = maxOffset;
		}
	}

	public void clampFinderFlyoutScroll()
	{
		final int maxOffset = Math.max(0, finderFlyoutItemCount - FINDER_FLYOUT_ROWS);
		if (finderFlyoutScrollOffset > maxOffset)
		{
			finderFlyoutScrollOffset = maxOffset;
		}
	}

	public void scrollFinderBody(int delta)
	{
		clampFinderBodyScroll();
		final int maxOffset = Math.max(0, finderBodyItemCount - finderBodyViewportRows());
		finderBodyScrollOffset = Math.max(0, Math.min(finderBodyScrollOffset + delta, maxOffset));
	}

	public void scrollFinderFlyout(int delta)
	{
		clampFinderFlyoutScroll();
		final int maxOffset = Math.max(0, finderFlyoutItemCount - FINDER_FLYOUT_ROWS);
		finderFlyoutScrollOffset = Math.max(0, Math.min(finderFlyoutScrollOffset + delta, maxOffset));
	}

	public void flashAt(WorldPoint point)
	{
		if (point == null)
		{
			clearFlash();
			return;
		}
		this.flashPoint = point;
		this.flashStartMillis = System.currentTimeMillis();
	}

	public WorldPoint getFlashPoint()
	{
		return flashPoint;
	}

	public long getFlashStartMillis()
	{
		return flashStartMillis;
	}

	public void clearFlash()
	{
		this.flashPoint = null;
		this.flashStartMillis = 0L;
	}

	public UndergroundZone getActiveUndergroundZone()
	{
		return activeUndergroundZone;
	}

	public boolean isUndergroundModeActive()
	{
		return activeUndergroundZone != null;
	}

	public synchronized void setUndergroundMode(UndergroundZone zone)
	{
		setUndergroundMode(zone, zone == null ? null : zone.getSurfacePoint());
	}

	/** Opens a dungeon from a particular surface entrance while keeping its projected map anchor. */
	public synchronized void setUndergroundMode(UndergroundZone zone, WorldPoint entrance)
	{
		this.activeFloorLayer = null;
		if (zone == null)
		{
			clearUndergroundMode();
			return;
		}
		if (zone == this.activeUndergroundZone)
		{
			if (entrance != null)
			{
				this.activeUndergroundSurfacePoint = entrance;
			}
			return;
		}
		if (this.activeUndergroundZone == null)
		{
			this.savedSurfaceZoom = this.zoom;
		}
		this.activeUndergroundZone = zone;
		this.activeUndergroundSurfacePoint = entrance != null ? entrance : zone.getSurfacePoint();
		this.activeOverlayCluster = null;
		// Render the committed view exactly like the hover preview: the dungeon tiles are
		// composited onto the surface entrance via the zone delta, so the camera stays on
		// surface coordinates and frames the entrance rather than leaping ~6400 tiles north.
		this.centerX = activeUndergroundSurfacePoint.getX();
		this.centerY = activeUndergroundSurfacePoint.getY();
		this.plane = zone.getUndergroundPoint().getPlane();
		this.planeChosenByUser = true;
		this.zoom = clampZoom(Math.max(this.zoom, dungeonTargetZoom(zone)));
		clampCenter();
	}

	public synchronized void clearUndergroundMode()
	{
		this.activeFloorLayer = null;
		final UndergroundZone previous = this.activeUndergroundZone;
		final WorldPoint previousSurfacePoint = this.activeUndergroundSurfacePoint;
		this.activeUndergroundZone = null;
		this.activeUndergroundSurfacePoint = null;
		this.activeOverlayCluster = null;
		if (previous != null)
		{
			// The camera stayed on surface coordinates the whole time the layer was open (the
			// dungeon was composited forward via the zone delta), so there is nothing to
			// translate back — just drop to the surface plane.
			this.plane = (previousSurfacePoint != null ? previousSurfacePoint : previous.getSurfacePoint()).getPlane();
			this.planeChosenByUser = true;
			clampCenter();
		}
		if (savedSurfaceZoom != null)
		{
			this.zoom = clampZoom(savedSurfaceZoom);
			savedSurfaceZoom = null;
		}
	}

	/**
	 * Exits any active dungeon layer back to the surface, centering on the surface entrance location
	 * and restoring the pre-dungeon surface zoom.
	 */
	public synchronized void exitDungeonToSurface()
	{
		exitDungeonToSurface(null);
	}

	/** Returns from a dungeon to the surface entrance represented by the clicked exit target. */
	public synchronized void exitDungeonToSurface(WorldPoint surfacePoint)
	{
		// An exact match — an activated zone, or a wiki-map box the camera sits inside — is the
		// entrance to head for. A loose forUndergroundPoint() guess (see getDungeonZone) is not,
		// because the +6400 band is crowded and it can snap to an unrelated dungeon.
		final UndergroundZone exact = activeUndergroundZone != null
			? activeUndergroundZone
			: InstanceMaps.zoneForPoint(centerX, centerY);
		final WorldPoint exitPoint = surfacePoint != null ? surfacePoint : activeUndergroundSurfacePoint;
		clearUndergroundMode();

		if (exitPoint != null)
		{
			centerOnSurfacePoint(exitPoint);
			return;
		}

		if (exact != null)
		{
			centerOnSurfacePoint(exact.getSurfacePoint());
			return;
		}

		// No mapped zone: fall back to wherever the player last stood on the surface. For an
		// unmapped instance (Kruk's Dungeon) that is the cave mouth they walked in through.
		if (lastSurfaceLocation != null)
		{
			centerOnSurfacePoint(lastSurfaceLocation);
			return;
		}

		final UndergroundZone loose = getDungeonZone();
		if (loose != null)
		{
			centerOnSurfacePoint(loose.getSurfacePoint());
		}
		else if (!InstanceMaps.cameraOnOverworld(centerY))
		{
			this.centerY = Math.max(MIN_WORLD_Y, centerY - 6400);
			this.plane = 0;
			this.planeChosenByUser = true;
			clampCenter();
		}
	}

	private void centerOnSurfacePoint(WorldPoint surface)
	{
		this.centerX = surface.getX();
		this.centerY = surface.getY();
		this.plane = surface.getPlane();
		this.planeChosenByUser = true;
		clampCenter();
	}

	/** Target zoom for comfortably framing a dungeon to fill ~65-75% of the viewport. */
	public double dungeonTargetZoom(UndergroundZone zone)
	{
		final Rectangle view = viewport;
		if (view == null || zone == null)
		{
			return 3.0;
		}
		final int diameter = Math.max(60, zone.getRadius() * 2);
		final double target = Math.min(view.getWidth(), view.getHeight()) * 0.70 / diameter;
		return clampZoom(Math.max(2.5, Math.min(6.0, target)));
	}

	public boolean isViewingDungeonLayer()
	{
		return activeUndergroundZone != null || activeOverlayCluster != null || !InstanceMaps.cameraOnOverworld(centerY);
	}

	public UndergroundZone getDungeonZone()
	{
		if (activeUndergroundZone != null)
		{
			return activeUndergroundZone;
		}
		UndergroundZone zone = InstanceMaps.zoneForPoint(centerX, centerY);
		if (zone == null && !InstanceMaps.cameraOnOverworld(centerY))
		{
			zone = UndergroundZone.forUndergroundPoint((int) Math.round(centerX), (int) Math.round(centerY), 500);
		}
		return zone;
	}

	public Double getSavedSurfaceZoom()
	{
		return savedSurfaceZoom;
	}

	public void setSavedSurfaceZoom(Double savedSurfaceZoom)
	{
		this.savedSurfaceZoom = savedSurfaceZoom;
	}

	/**
	 * The dungeon the view is committed to, or the one under the cursor during hover preview.
	 */
	public UndergroundZone getFocusedUndergroundZone()
	{
		return activeUndergroundZone != null ? activeUndergroundZone : hoveredUndergroundZone;
	}

	/**
	 * True when markers and tooltips should show that dungeon's interior rather than the overworld.
	 */
	public boolean isDungeonContentsFocused()
	{
		if (activeUndergroundZone != null)
		{
			return true;
		}
		return hoveredUndergroundZone != null && hoveredSurfaceToUnderground;
	}

	public UndergroundZone getHoveredUndergroundZone()
	{
		return hoveredUndergroundZone;
	}

	/**
	 * The zone the dev tuner acts on: the committed layer, or the dungeon symbol under the cursor
	 * during a surface-to-underground hover preview.
	 */
	public UndergroundZone getTunableZone()
	{
		if (activeUndergroundZone != null)
		{
			return activeUndergroundZone;
		}
		return hoveredSurfaceToUnderground ? hoveredUndergroundZone : null;
	}

	public boolean isHoveredSurfaceToUnderground()
	{
		return hoveredSurfaceToUnderground;
	}

	public void setHoveredUnderground(UndergroundZone zone, boolean surfaceToUnderground)
	{
		this.hoveredUndergroundZone = zone;
		this.hoveredSurfaceToUnderground = surfaceToUnderground;
		this.hoveredFloorPlane = null;
		if (zone != null)
		{
			this.hoveredOverlayCluster = null;
		}
	}

	public void setHoveredFloor(OverlayFloor floor)
	{
		if (floor == null)
		{
			this.hoveredFloorPlane = null;
			return;
		}
		this.hoveredUndergroundZone = floor.zone;
		this.hoveredSurfaceToUnderground = true;
		this.hoveredOverlayCluster = null;
		this.hoveredFloorPlane = floor.plane;
		this.hoveredFloorLayer = floor.layerId;
	}

	public synchronized void setActiveFloor(OverlayFloor floor)
	{
		setUndergroundMode(floor.zone);
		setPlane(floor.plane);
		this.activeFloorLayer = floor.layerId;
	}

	public Integer floorLayerFor(UndergroundZone zone)
	{
		if (hoveredFloorPlane != null && hoveredUndergroundZone == zone)
		{
			return hoveredFloorLayer;
		}
		return activeUndergroundZone == zone ? activeFloorLayer : null;
	}

	public Integer getHoveredFloorPlane()
	{
		return hoveredFloorPlane;
	}

	public boolean isHoveredFloor(OverlayFloor floor)
	{
		if (floor == null || hoveredFloorPlane == null)
		{
			return false;
		}
		if (hoveredUndergroundZone != floor.zone || hoveredFloorPlane != floor.plane)
		{
			return false;
		}
		return hoveredFloorLayer == null || java.util.Objects.equals(hoveredFloorLayer, floor.layerId);
	}

	public OverlayCluster getHoveredOverlayCluster()
	{
		return hoveredOverlayCluster;
	}

	public void setHoveredOverlayCluster(OverlayCluster cluster)
	{
		this.hoveredOverlayCluster = cluster;
		if (cluster != null)
		{
			this.hoveredUndergroundZone = null;
			this.hoveredSurfaceToUnderground = true;
			this.hoveredFloorPlane = null;
		}
	}

	/**
	 * Zones to composite this frame: a clicked or hovered dungeon chip wins; otherwise the
	 * hovered connected-zone cluster's members.
	 */
	public List<UndergroundZone> previewUndergroundZones()
	{
		if (activeUndergroundZone != null)
		{
			return Collections.singletonList(activeUndergroundZone);
		}
		if (hoveredUndergroundZone != null && hoveredSurfaceToUnderground)
		{
			return Collections.singletonList(hoveredUndergroundZone);
		}
		if (activeOverlayCluster != null)
		{
			return activeOverlayCluster.members;
		}
		if (hoveredOverlayCluster != null)
		{
			return hoveredOverlayCluster.members;
		}
		return Collections.emptyList();
	}

	public boolean isClusterPreview()
	{
		return activeUndergroundZone == null
			&& (hoveredUndergroundZone == null || !hoveredSurfaceToUnderground)
			&& (activeOverlayCluster != null || hoveredOverlayCluster != null);
	}

	public OverlayCluster getActiveOverlayCluster()
	{
		return activeOverlayCluster;
	}

	public synchronized void setActiveOverlayCluster(OverlayCluster cluster)
	{
		this.activeUndergroundZone = null;
		this.activeOverlayCluster = cluster;
		if (cluster != null)
		{
			this.centerX = cluster.iconX;
			this.centerY = cluster.iconY;
			this.hoveredOverlayCluster = cluster;
			this.hoveredUndergroundZone = null;
			this.hoveredFloorPlane = null;
			this.hoveredSurfaceToUnderground = true;
		}
	}

	public TravelData.TravelNode getHoveredTravelNode()
	{
		return hoveredTravelNode;
	}

	public void setHoveredTravelNode(TravelData.TravelNode node)
	{
		this.hoveredTravelNode = node;
	}

	public TravelData.TravelNode getSelectedTravelNode()
	{
		return selectedTravelNode;
	}

	public void setSelectedTravelNode(TravelData.TravelNode node)
	{
		this.selectedTravelNode = node;
	}

	/** Reveal the station and its destinations, leaving room around their pins and labels. */
	public synchronized void fitTravelRoutes(TravelData.TravelNode node, Rectangle view)
	{
		final WorldPoint origin = node.getLocation();
		double minX = InstanceMaps.toDisplayX(origin.getX() + 0.5, origin.getY() + 0.5, centerX, centerY);
		double minY = InstanceMaps.toDisplayY(origin.getX() + 0.5, origin.getY() + 0.5, centerX, centerY);
		double maxX = minX;
		double maxY = minY;
		for (TravelData.TravelDestination destination : node.getDestinations())
		{
			final WorldPoint location = destination.getLocation();
			if (location == null || !InstanceMaps.inFocusedLayer(location.getX(), location.getY(),
				getFocusedUndergroundZone(), isDungeonContentsFocused()))
			{
				continue;
			}
			final double x = InstanceMaps.toDisplayX(location.getX() + 0.5, location.getY() + 0.5, centerX, centerY);
			final double y = InstanceMaps.toDisplayY(location.getX() + 0.5, location.getY() + 0.5, centerX, centerY);
			minX = Math.min(minX, x);
			maxX = Math.max(maxX, x);
			minY = Math.min(minY, y);
			maxY = Math.max(maxY, y);
		}

		final double width = view.getWidth() - 2 * Math.min(64, view.getWidth() * 0.15);
		final double height = view.getHeight() - 2 * Math.min(64, view.getHeight() * 0.15);
		zoom = clampZoom(Math.min(zoom, Math.min(width / Math.max(1, maxX - minX),
			height / Math.max(1, maxY - minY))));
		centerX = (minX + maxX) / 2;
		centerY = (minY + maxY) / 2;
	}

	public synchronized void centerOn(double worldX, double worldY)
	{
		centerX = PrifddinasShift.toDisplayX(worldX, worldY);
		centerY = PrifddinasShift.toDisplayY(worldX, worldY);
		clampCenter();
	}

	public synchronized void setZoom(double newZoom)
	{
		zoom = clampZoom(newZoom);
	}

	/** Drag: the world point under the cursor should stay under the cursor. */
	public synchronized void panByScreen(double dxPixels, double dyPixels)
	{
		final boolean wasOnOverworld = InstanceMaps.cameraOnOverworld(centerY);
		centerX -= dxPixels / zoom;
		centerY += dyPixels / zoom;
		clampCenter();
		leashToOverworld(wasOnOverworld);
	}

	/**
	 * Dragging north off the top of the overworld lands in the empty band the dungeon layers live
	 * in, which reads as a blank page. Opening a dungeon or following the player still puts the
	 * camera up there; only the drag is leashed, and only when it started on the overworld.
	 */
	private void leashToOverworld(boolean wasOnOverworld)
	{
		if (wasOnOverworld)
		{
			centerY = Math.min(centerY, InstanceMaps.GAP_MIN_Y);
		}
	}

	/** Wheel: zoom about the cursor rather than the middle of the map. */
	public synchronized void zoomBy(double factor, int anchorScreenX, int anchorScreenY)
	{
		final Rectangle view = viewport;
		if (view == null)
		{
			zoom = clampZoom(zoom * factor);
			return;
		}

		final boolean wasOnOverworld = InstanceMaps.cameraOnOverworld(centerY);

		// Live fields, not the frozen frame copies: successive wheel notches between two renders
		// must each build on the previous one.
		final double anchorWorldX = centerX + (anchorScreenX - view.getCenterX()) / zoom;
		final double anchorWorldY = centerY - (anchorScreenY - view.getCenterY()) / zoom;

		zoom = clampZoom(zoom * factor);

		centerX = anchorWorldX - (anchorScreenX - view.getCenterX()) / zoom;
		centerY = anchorWorldY + (anchorScreenY - view.getCenterY()) / zoom;
		clampCenter();
		leashToOverworld(wasOnOverworld);
	}

	/**
	 * Zoom that fits the overworld into {@code view}. The band of dungeon and instance layers
	 * above it is four times the height of the world itself, so fitting the full coordinate range
	 * would squeeze Gielinor into the bottom fifth of the screen.
	 */
	public synchronized double zoomToFit(Rectangle view)
	{
		final double width = Math.max(1, MAX_WORLD_X - MIN_WORLD_X);
		final double height = Math.max(1, InstanceMaps.GAP_MIN_Y - MIN_WORLD_Y);
		return clampZoom(Math.min(view.getWidth() / width, view.getHeight() / height));
	}

	public synchronized void fit(Rectangle view)
	{
		zoom = zoomToFit(view);
		centerX = (MIN_WORLD_X + MAX_WORLD_X) / 2d;
		centerY = (MIN_WORLD_Y + InstanceMaps.GAP_MIN_Y) / 2d;
	}

	public double screenX(double worldX, Rectangle view)
	{
		return view.getCenterX() + (worldX - frameCenterX) * frameZoom;
	}

	public double screenY(double worldY, Rectangle view)
	{
		return view.getCenterY() - (worldY - frameCenterY) * frameZoom;
	}

	/** Projects a game point, shifting instanced places onto where they belong on the current view. */
	public double screenX(double worldX, double worldY, Rectangle view)
	{
		return screenX(InstanceMaps.toDisplayX(worldX, worldY, frameCenterX, frameCenterY), view);
	}

	public double screenY(double worldX, double worldY, Rectangle view)
	{
		return screenY(InstanceMaps.toDisplayY(worldX, worldY, frameCenterX, frameCenterY), view);
	}

	public boolean inView(int worldX, int worldY, double minX, double maxX, double minY, double maxY)
	{
		return InstanceMaps.inView(worldX, worldY, frameCenterX, frameCenterY, minX, maxX, minY, maxY);
	}

	public double worldX(double screenX, Rectangle view)
	{
		return frameCenterX + (screenX - view.getCenterX()) / frameZoom;
	}

	public double worldY(double screenY, Rectangle view)
	{
		return frameCenterY - (screenY - view.getCenterY()) / frameZoom;
	}

	private double clampZoom(double value)
	{
		return Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, value));
	}

	/**
	 * Keeps the centre inside the global map bounds.
	 */
	private void clampCenter()
	{
		centerX = Math.max(MIN_WORLD_X, Math.min(MAX_WORLD_X, centerX));
		centerY = Math.max(MIN_WORLD_Y, Math.min(MAX_WORLD_Y, centerY));
	}
}
