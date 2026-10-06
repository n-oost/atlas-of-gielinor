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
package bettermap.map;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.Collections;
import java.util.List;
import javax.inject.Singleton;

import bettermap.data.DungeonPiece;
import bettermap.data.DungeonPieceTransform;
import bettermap.data.OverlayFloor;
import bettermap.data.TravelData;
import bettermap.data.UndergroundZone;
import bettermap.data.sailing.PlayerBoat;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;
import lombok.Getter;

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
		@Getter
		private final Rectangle bounds;
		@Getter
		private final UndergroundZone zone;
		@Getter
		private final WorldPoint surfacePoint;
		@Getter
		private final OverlayFloor floor;
		@Getter
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
			this.floor = null;
			this.surfaceToUnderground = surfaceToUnderground;
		}

		public LayerSymbolTarget(Rectangle bounds, OverlayFloor floor)
		{
			this.bounds = bounds;
			this.zone = floor.zone;
			this.surfacePoint = floor.zone.getSurfacePoint();
			this.floor = floor;
			this.surfaceToUnderground = true;
		}







		public boolean isFloor()
		{
			return floor != null;
		}

	}

	/** A single checkbox row in the on-map "Layers" panel, published for the input listener to hit-test. */
	public static final class LayerToggleTarget
	{
		@Getter
		private final Rectangle bounds;
		@Getter
		private final String configKey;
		private final boolean currentValue;

		public LayerToggleTarget(Rectangle bounds, String configKey, boolean currentValue)
		{
			this.bounds = bounds;
			this.configKey = configKey;
			this.currentValue = currentValue;
		}



		public boolean getCurrentValue()
		{
			return currentValue;
		}
	}

	/** One result row in the on-map Finder: the row body jumps, the walk chip hands off to Shortest Path. */
	public static final class FinderResultTarget
	{
		@Getter
		private final Rectangle rowBounds;
		@Getter
		private final Rectangle walkBounds;
		@Getter
		private final WorldPoint point;
		@Getter
		private final String name;
		@Getter
		private final MapFinder.Result result;
		@Getter
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
			this.rowBounds = rowBounds;
			this.walkBounds = walkBounds;
			this.point = point;
			this.name = name;
			this.result = result;
			this.region = region;
		}







	}

	/** One row in the Finder flyout submenu card. */
	public static final class FlyoutTarget
	{
		@Getter
		private final Rectangle rowBounds;
		@Getter
		private final Rectangle walkBounds;
		@Getter
		private final WorldPoint point;
		@Getter
		private final String name;
		@Getter
		private final MapFinder.Result result;



		public FlyoutTarget(Rectangle rowBounds, Rectangle walkBounds, WorldPoint point, String name, MapFinder.Result result)
		{
			this.rowBounds = rowBounds;
			this.walkBounds = walkBounds;
			this.point = point;
			this.name = name;
			this.result = result;
		}

		public FlyoutTarget(Rectangle rowBounds, Rectangle walkBounds, WorldPoint point, String name)
		{
			this(rowBounds, walkBounds, point, name, null);
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
	@Getter
	private volatile WorldPoint playerLocation;

	/**
	 * Last position the player held on the overworld surface (not instanced, at or below
	 * {@link InstanceMaps#GAP_MIN_Y}). Written on the client thread, read from Swing. Used as the
	 * surface anchor for instances with no {@link bettermap.data.UndergroundZone} mapping: the
	 * player marker projects here and {@link #exitDungeonToSurface()} lands here.
	 */
	@Getter
	private volatile WorldPoint lastSurfaceLocation;

	/** True while the local player stands in an instanced region. Written client thread, read Swing. */
	@Getter
	private volatile boolean playerInInstance;
	/** Last mapped interior observed while the map was open; used to react only to transitions. */
	private volatile UndergroundZone observedPlayerUndergroundZone;
	private volatile int observedPlayerPlane = -1;
	private volatile int observedPlayerPieceLayer = -1;
	/** True when the player is above the overworld but no reliable authored interior owns the point. */
	@Getter
	private volatile boolean interiorMapUnavailable;

		/** One row in the Player Boats dropdown menu. */
	public static final class BoatDropdownTarget
	{
		@Getter
		private final Rectangle bounds;
		@Getter
		private final PlayerBoat boat;

		public BoatDropdownTarget(Rectangle bounds, PlayerBoat boat)
		{
			this.bounds = bounds;
			this.boat = boat;
		}


	}

	@Getter
	private volatile Rectangle viewport;
	private volatile boolean active;
	private volatile Rectangle[] planeButtons = new Rectangle[0];
	/** Screen rects where clicks pass through to the client (native close, minimap overview, world-map orb). */
	@Getter
	private volatile Rectangle[] nativePassthrough = new Rectangle[0];
	@Getter
	private volatile Rectangle undergroundReturnButton;
	/** "Go to clue" button, shown while the clue scroll plugin has a solved target. */
	@Getter
	private volatile Rectangle clueButton;
	private volatile Rectangle questButton;
	/** "Go to Player" chip: pans and flashes the map on the local player. */
	private volatile Rectangle playerButton;
	/** "Go to Destination" button, shown while an active route/destination is set. */
	@Getter
	private volatile Rectangle destinationButton;
	/** "Player Boats" button, shown while the player owns boats. */
	@Getter
	private volatile Rectangle boatsButton;
	@Getter
	private volatile boolean boatsDropdownOpen;
	@Getter
	private volatile Rectangle boatsDropdownBounds;
	@Getter
	private volatile List<BoatDropdownTarget> boatDropdownTargets = Collections.emptyList();
	/** Collapsible clue panel on fullscreen map. */
	@Getter
	private volatile boolean cluePanelCollapsed;
	@Getter
	private volatile Rectangle cluePanelBounds;
	@Getter
	private volatile Rectangle cluePanelHeaderBounds;
	@Getter
	private volatile List<LayerSymbolTarget> layerSymbolTargets = Collections.emptyList();

	/** On-map "Layers" panel: the gear toggle, whether the panel is expanded, and its rows. */
	@Getter
	private volatile Rectangle layersButton;
	@Getter
	private volatile boolean layersPanelOpen;
	@Getter
	private volatile List<LayerToggleTarget> layerToggleTargets = Collections.emptyList();
	/** Outer bounds of the expanded panel, so the overlay can suppress hovercards behind it. */
	@Getter
	private volatile Rectangle layersPanelBounds;

	/**
	 * Chat toggle in the top-right strip. In fullscreen the game's chatbox draws over our map and
	 * our own viewport swallows every click on it, so its minimise control is unreachable — this
	 * button is the way out. Transient on purpose: {@link #setActive(boolean)} clears it, so
	 * closing the map always gives the chat back.
	 */
	@Getter
	private volatile Rectangle chatButton;
	private volatile boolean chatHidden;

	@Getter
	private volatile Rectangle finderButton;
	@Getter
	private volatile boolean finderPanelOpen;
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
	@Getter
	private volatile Rectangle finderPanelBounds;
	@Getter
	private volatile Rectangle finderFieldBounds;
	@Getter
	private volatile List<FinderResultTarget> finderResultTargets = Collections.emptyList();
	@Getter
	private volatile Rectangle finderChipBank;
	@Getter
	private volatile Rectangle finderChipSlayer;
	@Getter
	private volatile Rectangle finderChipTravel;
	@Getter
	private volatile Rectangle finderChipLast;
	@Getter
	private volatile String hoveredRowKey;
	@Getter
	private volatile Rectangle finderFlyoutBounds;
	/** Flyout panel plus bridge corridor to the hovered row — keeps hover sticky while crossing the gap. */
	@Getter
	private volatile Rectangle finderFlyoutHitBounds;
	@Getter
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

	@Getter
	private volatile int finderBodyScrollOffset;
	@Getter
	private volatile int finderFlyoutScrollOffset;
	@Getter
	private volatile boolean finderBodyExpanded;
	private volatile int finderBodyItemCount;
	private volatile int finderFlyoutItemCount;
	@Getter
	private volatile String finderScrollKey = "";
	@Getter
	private volatile Rectangle finderBodyViewport;
	@Getter
	private volatile Rectangle finderFlyoutViewport;
	@Getter
	private volatile Rectangle finderExpandButton;
	@Getter
	private volatile long finderLastEdgeScrollMs;

	@Getter
	private volatile WorldPoint flashPoint;
	@Getter
	private volatile long flashStartMillis;

	@Getter
	private volatile UndergroundZone activeUndergroundZone;
	private volatile boolean lowerView;
	private volatile boolean travelView;
	private volatile List<UndergroundZone> openUndergroundZones = Collections.emptyList();
	private final java.util.Map<UndergroundZone, OverlayFloor> selectedDungeonFloors = new java.util.concurrent.ConcurrentHashMap<>();
	private volatile WorldPoint activeUndergroundSurfacePoint;
	@Getter
	private volatile UndergroundZone hoveredUndergroundZone;
	@Getter
	private volatile Integer hoveredFloorPlane;
	private volatile Integer hoveredFloorLayer;
	private volatile Integer activeFloorLayer;
	@Getter
	private volatile boolean hoveredSurfaceToUnderground = true;
	@Getter
	private volatile TravelData.TravelNode hoveredTravelNode;
	@Getter
	private volatile TravelData.TravelNode selectedTravelNode;

	@Getter
	private volatile int plane;
	@Getter
	private volatile double centerX = 3222;
	@Getter
	private volatile double centerY = 3218;
	@Getter
	private volatile double zoom = 2.0;
	@Getter
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





	/** The zoom frozen for the current frame by {@link #beginFrame()} — matches what is projected. */
	public double getFrameZoom()
	{
		return frameZoom;
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
		// Reset Finder on opening, preserving it during subsequent render frames.
		final boolean becomingActive = active && !this.active;
		if (this.active != active)
		{
			final Rectangle view = viewport;
			log.debug(
				"[BetterMap:camera] active {} -> {}  viewport={}",
				this.active, active,
				view == null ? "null" : view.x + "," + view.y + " " + view.width + "x" + view.height);
		}
		this.active = active;
		if (becomingActive)
		{
			finderPanelOpen = false;
			finderFieldFocused = false;
			clearFinderRegions();
		}
		if (!active)
		{
			// Drop UI hit targets so a stale plane rect cannot swallow clicks next frame.
			nativePassthrough = new Rectangle[0];
			undergroundReturnButton = null;
			clueButton = null;
			questButton = null;
			cluePanelBounds = null;
			cluePanelHeaderBounds = null;
			planeButtons = new Rectangle[0];
			hoveredUndergroundZone = null;

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
			finderPanelOpen = false;
			finderFieldFocused = false;
			clearFinderRegions();
			hoveredRowKey = null;
			resetFinderScroll();
						destinationButton = null;
			boatsButton = null;
			boatsDropdownOpen = false;
			boatsDropdownBounds = null;
			boatDropdownTargets = Collections.emptyList();
			clearFlash();
			savedSurfaceZoom = null;
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
		selectedDungeonFloors.clear();
		this.plane = Math.max(0, Math.min(3, plane));
		this.planeChosenByUser = true;
	}

	/** Follows the player's floor, but never overrides a plane the user picked. */
	public synchronized void setPlaneFromPlayer(int plane)
	{
		if (!planeChosenByUser && !travelView)
		{
			this.plane = Math.max(0, Math.min(3, plane));
		}
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


	public void setLastSurfaceLocation(WorldPoint lastSurfaceLocation)
	{
		if (lastSurfaceLocation != null)
		{
			this.lastSurfaceLocation = lastSurfaceLocation;
		}
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
		if (travelView) return;
		final WorldPoint player = playerLocation;
		final UndergroundZone playerZone = player == null
			|| InstanceMaps.isOverworldOverlay(player.getX(), player.getY())
			? null
			: InstanceMaps.zoneForPoint(player.getX(), player.getY(), player.getPlane());
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
		final DungeonPiece playerPiece = playerZone == null ? null : InstanceMaps.pieceForPoint(
			playerZone, player.getX(), player.getY(), playerPlane, null);
		final int playerPieceLayer = playerPiece == null ? -1 : playerPiece.layer;
		if (playerZone != observedPlayerUndergroundZone || playerPlane != observedPlayerPlane
			|| playerPieceLayer != observedPlayerPieceLayer)
		{
			final UndergroundZone previous = observedPlayerUndergroundZone;
			observedPlayerUndergroundZone = playerZone;
			observedPlayerPlane = playerPlane;
			observedPlayerPieceLayer = playerPieceLayer;
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
				OverlayFloor playerFloor = null;
				for (OverlayFloor floor : OverlayFloor.all())
				{
					if (floor.zone != playerZone || floor.plane != player.getPlane())
					{
						continue;
					}
					if (playerPiece == null || (floor.layerId != null && floor.layerId == playerPieceLayer))
					{
						playerFloor = floor;
						break;
					}
					if (floor.layerId == null && playerFloor == null)
					{
						playerFloor = floor;
					}
				}
				if (playerFloor != null)
				{
					setActiveFloor(playerFloor);
				}
				// Authored pieces may be offset or rotated independently of the zone entrance.
				// Frame the same projected player point that the tile and marker renderers use.
				final DungeonPiece piece = InstanceMaps.pieceForPoint(playerZone,
					player.getX(), player.getY(), player.getPlane(), activeFloorLayer);
				if (piece != null)
				{
					final Point2D point = DungeonPieceTransform.toDisplay(piece,
						player.getX() + 0.5, player.getY() + 0.5);
					centerX = point.getX();
					centerY = point.getY();
					clampCenter();
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
		observedPlayerPieceLayer = -1;
		interiorMapUnavailable = false;
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


	public void setNativePassthrough(Rectangle[] nativePassthrough)
	{
		this.nativePassthrough = nativePassthrough != null ? nativePassthrough : new Rectangle[0];
	}


	public void setUndergroundReturnButton(Rectangle undergroundReturnButton)
	{
		this.undergroundReturnButton = undergroundReturnButton;
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


	public void setDestinationButton(Rectangle destinationButton)
	{
		this.destinationButton = destinationButton;
	}


	public void setBoatsButton(Rectangle boatsButton)
	{
		this.boatsButton = boatsButton;
	}


	public void setBoatsDropdownOpen(boolean boatsDropdownOpen)
	{
		this.boatsDropdownOpen = boatsDropdownOpen;
	}


	public void setBoatsDropdownBounds(Rectangle boatsDropdownBounds)
	{
		this.boatsDropdownBounds = boatsDropdownBounds;
	}


	public void setBoatDropdownTargets(List<BoatDropdownTarget> boatDropdownTargets)
	{
		this.boatDropdownTargets = boatDropdownTargets != null ? boatDropdownTargets : Collections.emptyList();
	}


	public void setCluePanelCollapsed(boolean cluePanelCollapsed)
	{
		this.cluePanelCollapsed = cluePanelCollapsed;
	}


	public void setCluePanelBounds(Rectangle cluePanelBounds)
	{
		this.cluePanelBounds = cluePanelBounds;
	}


	public void setCluePanelHeaderBounds(Rectangle cluePanelHeaderBounds)
	{
		this.cluePanelHeaderBounds = cluePanelHeaderBounds;
	}





	public void setLayerSymbolTargets(List<LayerSymbolTarget> targets)
	{
		this.layerSymbolTargets = targets != null ? targets : Collections.emptyList();
	}


	public void setLayersButton(Rectangle layersButton)
	{
		this.layersButton = layersButton;
	}


	public void setLayersPanelOpen(boolean layersPanelOpen)
	{
		this.layersPanelOpen = layersPanelOpen;
	}


	public void setLayerToggleTargets(List<LayerToggleTarget> targets)
	{
		this.layerToggleTargets = targets != null ? targets : Collections.emptyList();
	}


	public void setLayersPanelBounds(Rectangle layersPanelBounds)
	{
		this.layersPanelBounds = layersPanelBounds;
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


	public void setFinderButton(Rectangle finderButton)
	{
		this.finderButton = finderButton;
	}


	/** Whether the in-map Finder can take clicks and keys. */
	public boolean isFinderInteractive()
	{
		return finderPanelOpen && isActive();
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


	public void setFinderPanelBounds(Rectangle finderPanelBounds)
	{
		this.finderPanelBounds = finderPanelBounds;
	}


	public void setFinderFieldBounds(Rectangle finderFieldBounds)
	{
		this.finderFieldBounds = finderFieldBounds;
	}



	public void setFinderResultTargets(List<FinderResultTarget> targets)
	{
		this.finderResultTargets = targets != null ? targets : Collections.emptyList();
	}


	public void setFinderChipBank(Rectangle finderChipBank)
	{
		this.finderChipBank = finderChipBank;
	}


	public void setFinderChipSlayer(Rectangle finderChipSlayer)
	{
		this.finderChipSlayer = finderChipSlayer;
	}


	public void setFinderChipTravel(Rectangle finderChipTravel)
	{
		this.finderChipTravel = finderChipTravel;
	}


	public void setFinderChipLast(Rectangle finderChipLast)
	{
		this.finderChipLast = finderChipLast;
	}


	public void setHoveredRowKey(String hoveredRowKey)
	{
		this.hoveredRowKey = hoveredRowKey;
	}


	public void setFinderFlyoutBounds(Rectangle finderFlyoutBounds)
	{
		this.finderFlyoutBounds = finderFlyoutBounds;
	}


	public void setFinderFlyoutHitBounds(Rectangle finderFlyoutHitBounds)
	{
		this.finderFlyoutHitBounds = finderFlyoutHitBounds;
	}


	public void setFlyoutTargets(List<FlyoutTarget> targets)
	{
		this.flyoutTargets = targets != null ? targets : Collections.emptyList();
	}


	public void setFinderBodyScrollOffset(int finderBodyScrollOffset)
	{
		this.finderBodyScrollOffset = Math.max(0, finderBodyScrollOffset);
	}


	public void setFinderFlyoutScrollOffset(int finderFlyoutScrollOffset)
	{
		this.finderFlyoutScrollOffset = Math.max(0, finderFlyoutScrollOffset);
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


	public void setFinderScrollKey(String finderScrollKey)
	{
		this.finderScrollKey = finderScrollKey != null ? finderScrollKey : "";
	}


	public void setFinderBodyViewport(Rectangle finderBodyViewport)
	{
		this.finderBodyViewport = finderBodyViewport;
	}


	public void setFinderFlyoutViewport(Rectangle finderFlyoutViewport)
	{
		this.finderFlyoutViewport = finderFlyoutViewport;
	}


	public void setFinderExpandButton(Rectangle finderExpandButton)
	{
		this.finderExpandButton = finderExpandButton;
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



	public void clearFlash()
	{
		this.flashPoint = null;
		this.flashStartMillis = 0L;
	}


	public boolean isUndergroundModeActive()
	{
		return lowerView;
	}

	/** Browsing Lower is independent of whether any layout is open. */
	public synchronized void enterLowerView()
	{
		travelView = false;
		lowerView = true;
		selectedTravelNode = null;
		hoveredTravelNode = null;
		activeFloorLayer = null;
		hoveredUndergroundZone = null;

	}

	public boolean isTravelViewActive()
	{
		return travelView;
	}

	/** Travel browsing uses the surface camera without changing its position or zoom. */
	public synchronized void enterTravelView()
	{
		final double previousZoom = zoom;
		clearUndergroundMode();
		zoom = previousZoom;
		travelView = true;
		plane = 0;
		planeChosenByUser = true;
		selectedTravelNode = null;
		hoveredTravelNode = null;
		layerSymbolTargets = Collections.emptyList();
	}

	public boolean isUndergroundZoneOpen(UndergroundZone zone)
	{
		for (UndergroundZone open : openUndergroundZones)
		{
			if (open.getSelectionId().equals(zone.getSelectionId())) return true;
		}
		return false;
	}

	public synchronized void toggleUndergroundZone(UndergroundZone zone, WorldPoint entrance)
	{
		if (!isUndergroundZoneOpen(zone))
		{
			setUndergroundMode(zone, entrance);
			return;
		}
		final java.util.ArrayList<UndergroundZone> zones = new java.util.ArrayList<>(openUndergroundZones);
		zones.removeIf(open -> zone.getConnectedZones().stream()
			.anyMatch(connected -> open.getSelectionId().equals(connected.getSelectionId())));
		openUndergroundZones = Collections.unmodifiableList(zones);
		if (activeUndergroundZone != null && zone.getConnectedZones().stream()
			.anyMatch(connected -> activeUndergroundZone.getSelectionId().equals(connected.getSelectionId())))
		{
			activeUndergroundZone = zones.isEmpty() ? null : zones.get(zones.size() - 1);
			activeUndergroundSurfacePoint = activeUndergroundZone == null ? null : activeUndergroundZone.getSurfacePoint();
			plane = activeUndergroundZone == null ? 0 : activeUndergroundZone.getUndergroundPoint().getPlane();
			activeFloorLayer = null;
		}
		hoveredUndergroundZone = null;

	}

	public synchronized void setUndergroundMode(UndergroundZone zone)
	{
		setUndergroundMode(zone, zone == null ? null : zone.getSurfacePoint());
	}

	/** Opens a dungeon from a particular surface entrance while keeping its projected map anchor. */
	public synchronized void setUndergroundMode(UndergroundZone zone, WorldPoint entrance)
	{
		final boolean browsing = lowerView;
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
		travelView = false;
		lowerView = true;
		final java.util.ArrayList<UndergroundZone> zones = new java.util.ArrayList<>(openUndergroundZones);
		for (UndergroundZone connected : zone.getConnectedZones())
		{
			final UndergroundZone selected = connected.getSelectionId().equals(zone.getSelectionId()) ? zone : connected;
			zones.removeIf(open -> open.getSelectionId().equals(selected.getSelectionId()));
			zones.add(selected);
		}
		openUndergroundZones = Collections.unmodifiableList(zones);

		if (this.activeUndergroundZone == null)
		{
			this.savedSurfaceZoom = this.zoom;
		}
		this.activeUndergroundZone = zone;
		this.activeUndergroundSurfacePoint = entrance != null ? entrance : zone.getSurfacePoint();

		// Render the committed view exactly like the hover preview: the dungeon tiles are
		// composited onto the surface entrance via the zone delta, so the camera stays on
		// surface coordinates and frames the entrance rather than leaping ~6400 tiles north.
		if (!browsing)
		{
			this.centerX = activeUndergroundSurfacePoint.getX();
			this.centerY = activeUndergroundSurfacePoint.getY();
			this.zoom = clampZoom(Math.max(this.zoom, dungeonTargetZoom(zone)));
		}
		this.plane = zone.getUndergroundPoint().getPlane();
		this.planeChosenByUser = true;
		clampCenter();
	}

	public synchronized void clearUndergroundMode()
	{
		travelView = false;
		selectedDungeonFloors.clear();
		lowerView = false;
		openUndergroundZones = Collections.emptyList();
		hoveredUndergroundZone = null;

		this.activeFloorLayer = null;
		final UndergroundZone previous = this.activeUndergroundZone;
		final WorldPoint previousSurfacePoint = this.activeUndergroundSurfacePoint;
		this.activeUndergroundZone = null;
		this.activeUndergroundSurfacePoint = null;

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
		return lowerView || !InstanceMaps.cameraOnOverworld(centerY);
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


	public void setSavedSurfaceZoom(Double savedSurfaceZoom)
	{
		this.savedSurfaceZoom = savedSurfaceZoom;
	}

	/**
	 * The dungeon the view is committed to, or the one under the cursor during hover preview.
	 */
	public UndergroundZone getFocusedUndergroundZone()
	{
		if (activeUndergroundZone != null)
		{
			return activeUndergroundZone;
		}
		if (lowerView)
		{
			return null;
		}
		return hoveredUndergroundZone;
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
		if (lowerView)
		{
			return true;
		}
		return hoveredUndergroundZone != null && hoveredSurfaceToUnderground;
	}


	public void setHoveredUnderground(UndergroundZone zone, boolean surfaceToUnderground)
	{
		this.hoveredUndergroundZone = zone;
		this.hoveredSurfaceToUnderground = surfaceToUnderground;
		this.hoveredFloorPlane = null;
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

		this.hoveredFloorPlane = floor.plane;
		this.hoveredFloorLayer = floor.layerId;
	}

	public synchronized void setActiveFloor(OverlayFloor floor)
	{
		setUndergroundMode(floor.zone);
		setPlane(floor.plane);
		this.activeFloorLayer = floor.layerId;
		selectedDungeonFloors.put(floor.zone, floor);
	}

	public Integer floorLayerFor(UndergroundZone zone)
	{
		if (lowerView)
		{
			final OverlayFloor floor = selectedDungeonFloors.get(zone);
			return floor == null ? null : floor.layerId;
		}
		if (hoveredFloorPlane != null && hoveredUndergroundZone == zone)
		{
			return hoveredFloorLayer;
		}
		return activeUndergroundZone == zone ? activeFloorLayer : null;
	}

	/** Native composites contain all their arranged floors; authored zones retain floor controls. */
	public boolean isDungeonPieceVisible(UndergroundZone zone, DungeonPiece piece)
	{
		if (zone.getId().startsWith("native_")) return true;
		final OverlayFloor selected = selectedDungeonFloors.get(zone);
		if (selected != null)
		{
			return piece.plane == selected.plane && (selected.layerId == null || piece.layer == selected.layerId);
		}
		boolean multiPlane = false;
		for (OverlayFloor floor : OverlayFloor.all())
		{
			if (floor.zone == zone && floor.plane != zone.getUndergroundPoint().getPlane())
			{
				multiPlane = true;
				break;
			}
		}
		return !multiPlane || piece.plane == zone.getUndergroundPoint().getPlane();
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


	/**
	 * Zones revealed by the shared dungeon toggle, or its hover preview.
	 */
	public List<UndergroundZone> previewUndergroundZones()
	{
		if (lowerView)
		{
			return openUndergroundZones;
		}
		if (activeUndergroundZone != null)
		{
			return activeUndergroundZone.getConnectedZones();
		}
		if (hoveredUndergroundZone != null && hoveredSurfaceToUnderground)
		{
			return hoveredFloorPlane != null ? Collections.singletonList(hoveredUndergroundZone)
				: hoveredUndergroundZone.getConnectedZones();
		}
		return Collections.emptyList();
	}

	public void setHoveredTravelNode(TravelData.TravelNode node)
	{
		this.hoveredTravelNode = node;
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
