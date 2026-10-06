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
package com.bettermap.ui.input;

import com.bettermap.BetterMapPlugin;
import com.bettermap.data.TravelData;
import com.bettermap.map.InstanceMaps;
import com.bettermap.map.MapCamera;
import com.bettermap.map.MapFinder;
import com.bettermap.map.MonsterIndex;
import com.bettermap.map.PoiIndex;
import com.bettermap.map.SlayerTaskTracker;
import com.bettermap.ui.BetterWorldMapOverlay;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import javax.inject.Provider;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;

/**
 * Handles hit-testing and event dispatching for overlay controls: toolbar buttons
 * (Layers, Finder, Chat, Close, Floor navigation), layer toggles, symbol pins,
 * and the finder search interface.
 */
@Slf4j
public class MapLayerInputHandler
{
	private final MapCamera camera;
	private final Provider<BetterMapPlugin> pluginProvider;
	private final MapFinder finder;
	private final PoiIndex poiIndex;
	private final MonsterIndex monsterIndex;
	private final SlayerTaskTracker slayerTaskTracker;

	private String flyoutPendingKey;
	private long flyoutPendingSinceMs;
	@Getter
	@Setter
	private long flyoutDwellMs = MapCamera.FINDER_FLYOUT_DWELL_MS;

	public MapLayerInputHandler(
		MapCamera camera,
		Provider<BetterMapPlugin> pluginProvider,
		MapFinder finder,
		PoiIndex poiIndex,
		MonsterIndex monsterIndex,
		SlayerTaskTracker slayerTaskTracker)
	{
		this.camera = camera;
		this.pluginProvider = pluginProvider;
		this.finder = finder;
		this.poiIndex = poiIndex;
		this.monsterIndex = monsterIndex;
		this.slayerTaskTracker = slayerTaskTracker;
	}

	/**
	 * Dispatches clicks on primary map chrome buttons (gear, toggles, chat, find,
	 * close, underground return, quest, clue, player).
	 *
	 * @return non-null action name if consumed, null otherwise
	 */
	public String handleUiButtonClick(Point point)
	{
		final Rectangle gear = camera.getLayersButton();
		if (gear != null && gear.contains(point))
		{
			final boolean opening = !camera.isLayersPanelOpen();
			camera.setLayersPanelOpen(opening);
			if (opening)
			{
				camera.setFinderPanelOpen(false);
				camera.setBoatsDropdownOpen(false);
			}
			return "layers-gear";
		}

		if (clickedLayerToggle(point))
		{
			return "layer-toggle";
		}

		final Rectangle chat = camera.getChatButton();
		if (chat != null && chat.contains(point))
		{
			camera.setChatHidden(!camera.isChatHidden());
			return "chat-button";
		}

		final Rectangle find = camera.getFinderButton();
		if (find != null && find.contains(point))
		{
			final boolean opening = !camera.isFinderPanelOpen();
			camera.setFinderPanelOpen(opening);
			if (opening)
			{
				camera.setLayersPanelOpen(false);
				camera.setBoatsDropdownOpen(false);
				if (finder != null && pluginProvider != null && pluginProvider.get() != null)
				{
					finder.updateQuery("", pluginProvider.get().getPlayerLocation());
				}
			}
			return "finder-button";
		}

		final Rectangle returnBtn = camera.getUndergroundReturnButton();
		if (returnBtn != null && returnBtn.contains(point))
		{
			camera.exitDungeonToSurface();
			return "underground-return";
		}

		final Rectangle clueBtn = camera.getClueButton();
		if (clueBtn != null && clueBtn.contains(point))
		{
			if (pluginProvider != null && pluginProvider.get() != null)
			{
				pluginProvider.get().goToClue();
			}
			return "go-to-clue";
		}

		final Rectangle questBtn = camera.getQuestButton();
		if (questBtn != null && questBtn.contains(point))
		{
			if (pluginProvider != null && pluginProvider.get() != null)
			{
				pluginProvider.get().goToQuestStep();
			}
			return "go-to-quest-step";
		}

		final Rectangle playerBtn = camera.getPlayerButton();
		if (playerBtn != null && playerBtn.contains(point))
		{
			if (pluginProvider != null && pluginProvider.get() != null)
			{
				pluginProvider.get().goToPlayer();
			}
			return "go-to-player";
		}

		final Rectangle destBtn = camera.getDestinationButton();
		if (destBtn != null && destBtn.contains(point))
		{
			if (pluginProvider != null && pluginProvider.get() != null)
			{
				pluginProvider.get().goToDestination();
			}
			return "go-to-destination";
		}

		if (camera.isBoatsDropdownOpen())
		{
			final Rectangle boatsBtn = camera.getBoatsButton();
			if (boatsBtn != null && boatsBtn.contains(point))
			{
				camera.setBoatsDropdownOpen(false);
				return "boats-button-toggle";
			}

			final Rectangle dropdown = camera.getBoatsDropdownBounds();
			if (dropdown != null && dropdown.contains(point))
			{
				for (com.bettermap.map.MapCamera.BoatDropdownTarget target : camera.getBoatDropdownTargets())
				{
					if (target.getBounds().contains(point))
					{
						if (pluginProvider != null && pluginProvider.get() != null)
						{
							pluginProvider.get().goToBoat(target.getBoat());
						}
						camera.setBoatsDropdownOpen(false);
						return "boat-dropdown-select";
					}
				}
				return "boat-dropdown";
			}
			camera.setBoatsDropdownOpen(false);
		}

		final Rectangle boatsBtn = camera.getBoatsButton();
		if (boatsBtn != null && boatsBtn.contains(point))
		{
			final boolean opening = !camera.isBoatsDropdownOpen();
			camera.setBoatsDropdownOpen(opening);
			if (opening)
			{
				camera.setLayersPanelOpen(false);
				camera.setFinderPanelOpen(false);
			}
			return "boats-button-toggle";
		}

		final Rectangle clueHeader = camera.getCluePanelHeaderBounds();
		if (clueHeader != null && clueHeader.contains(point))
		{
			camera.setCluePanelCollapsed(!camera.isCluePanelCollapsed());
			return "clue-panel-header";
		}

		final Rectangle cluePanel = camera.getCluePanelBounds();
		if (cluePanel != null && cluePanel.contains(point))
		{
			return "clue-panel";
		}

		if (clickedLayerSymbol(point))
		{
			return "layer-symbol";
		}

		if (clickedPlaneButton(point))
		{
			return "plane-button";
		}

		return null;
	}

	public boolean handleFinderClick(MouseEvent event)
	{
		if (camera.isFinderPanelOpen())
		{
			if (clickedFlyoutItem(event.getPoint(), event.isShiftDown(), event.getClickCount()))
			{
				return true;
			}

			if (clickedFinderChip(event.getPoint()))
			{
				return true;
			}

			final Rectangle expand = camera.getFinderExpandButton();
			if (expand != null && expand.contains(event.getPoint()))
			{
				camera.toggleFinderBodyExpanded();
				return true;
			}

			if (clickedFinderResult(event.getPoint(), event.isShiftDown(), event.getClickCount()))
			{
				return true;
			}

			final Rectangle field = camera.getFinderFieldBounds();
			if (field != null && field.contains(event.getPoint()))
			{
				camera.setFinderFieldFocused(true);
				return true;
			}

			final Rectangle panel = camera.getFinderPanelBounds();
			Rectangle flyout = camera.getFinderFlyoutHitBounds();
			if (flyout == null)
			{
				flyout = camera.getFinderFlyoutBounds();
			}
			if ((panel != null && panel.contains(event.getPoint()))
				|| (flyout != null && flyout.contains(event.getPoint())))
			{
				return true;
			}
		}

		return false;
	}

	public boolean clickedFinderChip(Point point)
	{
		final Rectangle bank = camera.getFinderChipBank();
		if (bank != null && bank.contains(point))
		{
			handleBankChip();
			return true;
		}

		final Rectangle slayer = camera.getFinderChipSlayer();
		if (slayer != null && slayer.contains(point))
		{
			handleSlayerChip();
			return true;
		}

		final Rectangle travel = camera.getFinderChipTravel();
		if (travel != null && travel.contains(point))
		{
			handleTravelChip();
			return true;
		}

		final Rectangle last = camera.getFinderChipLast();
		if (last != null && last.contains(point))
		{
			handleLastChip();
			return true;
		}

		return false;
	}

	private void handleBankChip()
	{
		final WorldPoint playerLoc = pluginProvider != null && pluginProvider.get() != null
			? pluginProvider.get().getPlayerLocation() : null;
		if (poiIndex == null)
		{
			return;
		}
		final PoiIndex.Poi bank = poiIndex.nearestByKey("bank",
			playerLoc != null ? playerLoc.getX() : 0, playerLoc != null ? playerLoc.getY() : 0);
		if (bank != null && pluginProvider != null && pluginProvider.get() != null)
		{
			activateRow(new WorldPoint(bank.getX(), bank.getY(), bank.getPlane()));
		}
	}

	private void handleSlayerChip()
	{
		final WorldPoint playerLoc = pluginProvider != null && pluginProvider.get() != null
			? pluginProvider.get().getPlayerLocation() : null;
		if (slayerTaskTracker == null)
		{
			return;
		}
		final WorldPoint target = slayerTaskTracker.nearestTaskLocation(playerLoc, monsterIndex, poiIndex);
		if (target != null && pluginProvider != null && pluginProvider.get() != null)
		{
			activateRow(target);
		}
	}

	private void handleTravelChip()
	{
		final WorldPoint playerLoc = pluginProvider != null && pluginProvider.get() != null
			? pluginProvider.get().getPlayerLocation() : null;
		TravelData.TravelNode best = null;
		int bestDist = Integer.MAX_VALUE;
		for (TravelData.TravelNode node : TravelData.ALL_NODES)
		{
			final int dist = distanceTiles(node.getLocation(), playerLoc);
			if (dist >= 0 && dist < bestDist)
			{
				bestDist = dist;
				best = node;
			}
			else if (best == null)
			{
				best = node;
			}
		}
		if (best != null && pluginProvider != null && pluginProvider.get() != null)
		{
			activateRow(best.getLocation());
		}
	}

	private void handleLastChip()
	{
		if (finder == null)
		{
			return;
		}
		final String last = finder.getLastQuery();
		if (last != null && !last.isEmpty() && pluginProvider != null && pluginProvider.get() != null)
		{
			finder.updateQuery(last, pluginProvider.get().getPlayerLocation());
		}
	}

	private void activateRow(WorldPoint point)
	{
		// Hub review F2: Finder pans only; closing requires genuine input on the native control.
		// Do not restore shift-click auto-close or a programmatic widget operation.
		if (pluginProvider != null && pluginProvider.get() != null)
		{
			pluginProvider.get().centerMapOn(point);
		}
	}

	private void routeToRow(WorldPoint point)
	{
		if (pluginProvider != null && pluginProvider.get() != null)
		{
			pluginProvider.get().routeTo(point);
		}
	}

	public boolean clickedFlyoutItem(Point point, boolean shiftDown, int clickCount)
	{
		for (MapCamera.FlyoutTarget target : camera.getFlyoutTargets())
		{
			final Rectangle routeButton = target.getWalkBounds();
			if (routeButton != null && routeButton.contains(point))
			{
				routeToRow(target.getPoint());
				return true;
			}
			final Rectangle row = target.getRowBounds();
			if (row != null && row.contains(point))
			{
				if (clickCount >= 2)
				{
					routeToRow(target.getPoint());
				}
				else
				{
					activateRow(target.getPoint());
				}
				return true;
			}
		}
		return false;
	}

	public boolean clickedFinderResult(Point point, boolean shiftDown, int clickCount)
	{
		for (MapCamera.FinderResultTarget target : camera.getFinderResultTargets())
		{
			final Rectangle routeButton = target.getWalkBounds();
			if (routeButton != null && routeButton.contains(point))
			{
				WorldPoint routePoint = finderTargetPoint(target);
				routeToRow(routePoint);
				return true;
			}
			final Rectangle row = target.getRowBounds();
			if (row != null && row.contains(point))
			{
				if (target.getRegion() != null)
				{
					final WorldPoint regCenter = new WorldPoint(target.getRegion().getCenterX(), target.getRegion().getCenterY(), 0);
					if (clickCount >= 2)
					{
						routeToRow(regCenter);
					}
					else
					{
						activateRow(regCenter);
					}
					return true;
				}

				final MapFinder.Result activated = MapFinder.activationTarget(target.getResult());
				final WorldPoint activatedPoint = activated != null ? activated.getPoint() : target.getPoint();
				if (clickCount >= 2)
				{
					routeToRow(activatedPoint);
				}
				else
				{
					activateRow(activatedPoint);
				}
				return true;
			}
		}
		return false;
	}

	private static WorldPoint finderTargetPoint(MapCamera.FinderResultTarget target)
	{
		if (target.getRegion() != null)
		{
			return new WorldPoint(target.getRegion().getCenterX(), target.getRegion().getCenterY(), 0);
		}
		final MapFinder.Result result = MapFinder.activationTarget(target.getResult());
		return result != null ? result.getPoint() : target.getPoint();
	}

	public boolean clickedLayerSymbol(Point point)
	{
		if (camera.isTravelViewActive()) return false;
		if (camera.getSelectedTravelNode() != null && pluginProvider != null && pluginProvider.get() != null
			&& pluginProvider.get().getConfig().showTravelRoutes())
		{
			return false;
		}
		for (MapCamera.LayerSymbolTarget target : camera.getLayerSymbolTargets())
		{
			if (target.getBounds() != null && target.getBounds().contains(point))
			{
				if (target.isSurfaceToUnderground())
				{
					if (target.isFloor())
					{
						camera.setUndergroundMode(target.getZone(), target.getSurfacePoint());
					}
					else
					{
						camera.toggleUndergroundZone(target.getZone(), target.getSurfacePoint());
					}
					if (target.isFloor())
					{
						camera.setActiveFloor(target.getFloor());
					}
					log.debug("[BetterMap] entered underground mode for {}", target.getZone().getName());
				}
				else
				{
					camera.exitDungeonToSurface(target.getSurfacePoint());
					log.debug("[BetterMap] returned to surface from {}", target.getZone().getName());
				}
				return true;
			}
		}
		return false;
	}

	public boolean clickedLayerToggle(Point point)
	{
		for (MapCamera.LayerToggleTarget target : camera.getLayerToggleTargets())
		{
			if (target.getBounds() != null && target.getBounds().contains(point))
			{
				if (BetterWorldMapOverlay.HIDE_ALL_LAYERS_KEY.equals(target.getConfigKey()))
				{
					final boolean showAll = target.getCurrentValue();
					if (pluginProvider != null && pluginProvider.get() != null)
					{
						pluginProvider.get().setAllLayersEnabled(showAll);
					}
					log.debug("[BetterMap] layer hide-all -> {}", showAll ? "show all" : "hide all");
					return true;
				}

				final boolean next = !target.getCurrentValue();
				if (pluginProvider != null && pluginProvider.get() != null)
				{
					pluginProvider.get().setLayerEnabled(target.getConfigKey(), next);
				}
				log.debug("[BetterMap] layer toggle {} -> {}", target.getConfigKey(), next);
				return true;
			}
		}
		return false;
	}

	public boolean clickedPlaneButton(Point point)
	{
		final Rectangle[] buttons = camera.getPlaneButtons();
		if (buttons == null || buttons.length < 3)
		{
			return false;
		}

		// Button 0: "Upper" (plane 1)
		if (buttons[0] != null && buttons[0].contains(point))
		{
			camera.clearUndergroundMode();

			camera.setPlane(1);
			log.debug("[BetterMap] floor button Upper (plane 1) clicked");
			return true;
		}

		// Button 1: "Main" (plane 0)
		if (buttons[1] != null && buttons[1].contains(point))
		{
			camera.clearUndergroundMode();

			camera.setPlane(0);
			log.debug("[BetterMap] floor button Main (plane 0) clicked");
			return true;
		}

		if (buttons.length > 3 && buttons[3] != null && buttons[3].contains(point))
		{
			camera.enterTravelView();
			return true;
		}

		// Button 2: Lower browsing, including an empty selection.
		if (buttons[2] != null && buttons[2].contains(point))
		{
			camera.enterLowerView();
			return true;
		}

		return false;
	}

	public void clearFinderFlyoutHover()
	{
		flyoutPendingKey = null;
		flyoutPendingSinceMs = 0L;
		camera.setHoveredRowKey(null);
	}

	public void updateHoveredFinderRow(Point point)
	{
		if (!camera.isFinderPanelOpen() || point == null)
		{
			clearFinderFlyoutHover();
			return;
		}

		final Rectangle flyoutHit = camera.getFinderFlyoutHitBounds();
		if (flyoutHit != null && flyoutHit.contains(point))
		{
			return;
		}

		String hitKey = null;
		for (MapCamera.FinderResultTarget target : camera.getFinderResultTargets())
		{
			final Rectangle row = target.getRowBounds();
			if (row != null && row.contains(point))
			{
				if (target.getRegion() != null)
				{
					hitKey = target.getRegion().name();
				}
				else if (target.getResult() != null)
				{
					if (!target.getResult().hasChildren())
					{
						break;
					}
					hitKey = target.getResult().getGroupKey() != null
						? target.getResult().getGroupKey()
						: target.getResult().getName();
				}
				else
				{
					hitKey = target.getName();
				}
				break;
			}
		}

		if (hitKey != null)
		{
			final long now = System.currentTimeMillis();
			if (!hitKey.equals(flyoutPendingKey))
			{
				flyoutPendingKey = hitKey;
				flyoutPendingSinceMs = now;
				camera.setHoveredRowKey(null);
			}

			if (now - flyoutPendingSinceMs >= flyoutDwellMs)
			{
				if (!hitKey.equals(camera.getHoveredRowKey()))
				{
					camera.setFinderFlyoutScrollOffset(0);
				}
				camera.setHoveredRowKey(hitKey);
			}
			else
			{
				camera.setHoveredRowKey(null);
			}
			return;
		}

		clearFinderFlyoutHover();
	}

	public void tickFinderFlyoutDwell()
	{
		if (!camera.isFinderPanelOpen() || flyoutPendingKey == null)
		{
			return;
		}

		if (System.currentTimeMillis() - flyoutPendingSinceMs < flyoutDwellMs)
		{
			return;
		}

		if (!flyoutPendingKey.equals(camera.getHoveredRowKey()))
		{
			camera.setFinderFlyoutScrollOffset(0);
			camera.setHoveredRowKey(flyoutPendingKey);
		}
	}

	public void updateFinderEdgeScroll(Point point)
	{
		if (!camera.isFinderPanelOpen() || point == null)
		{
			return;
		}

		final long now = System.currentTimeMillis();
		if (now - camera.getFinderLastEdgeScrollMs() < MapCamera.FINDER_EDGE_SCROLL_MS)
		{
			return;
		}

		boolean scrolled = false;
		final int edge = MapCamera.FINDER_SCROLL_EDGE_PX;

		final Rectangle bodyVp = camera.getFinderBodyViewport();
		if (bodyVp != null && bodyVp.contains(point))
		{
			if (point.y < bodyVp.y + edge)
			{
				camera.scrollFinderBody(-1);
				scrolled = true;
			}
			else if (point.y > bodyVp.y + bodyVp.height - edge)
			{
				camera.scrollFinderBody(1);
				scrolled = true;
			}
		}

		final Rectangle flyoutVp = camera.getFinderFlyoutViewport();
		if (!scrolled && flyoutVp != null && flyoutVp.contains(point))
		{
			if (point.y < flyoutVp.y + edge)
			{
				camera.scrollFinderFlyout(-1);
				scrolled = true;
			}
			else if (point.y > flyoutVp.y + flyoutVp.height - edge)
			{
				camera.scrollFinderFlyout(1);
				scrolled = true;
			}
		}

		if (scrolled)
		{
			camera.setFinderLastEdgeScrollMs(now);
		}
	}

	public void updateHoveredTravelNode(Point point)
	{
		final BetterMapPlugin plugin = pluginProvider != null ? pluginProvider.get() : null;
		if (!camera.isTravelViewActive() && plugin != null && plugin.getConfig() != null && camera.getZoom() < plugin.getConfig().travelStationMinZoom())
		{
			camera.setHoveredTravelNode(null);
			return;
		}
		if (point == null)
		{
			camera.setHoveredTravelNode(null);
			return;
		}

		final Rectangle view = camera.getViewport();
		if (view == null)
		{
			camera.setHoveredTravelNode(null);
			return;
		}

		final double worldX = camera.worldX(point.x, view);
		final double worldY = camera.worldY(point.y, view);
		final double radius = Math.max(3.0, 12.0 / Math.max(0.4, camera.getZoom()));

		final TravelData.TravelNode node = InstanceMaps.firstHit((int) Math.floor(worldX), (int) Math.floor(worldY),
			camera.getFocusedUndergroundZone(), camera.isDungeonContentsFocused(),
			(x, y) -> TravelData.findNodeNear(worldX, worldY, camera.getPlane(), radius));
		camera.setHoveredTravelNode(node);
	}

	public void updateHoveredLayerSymbol(Point point)
	{
		for (MapCamera.LayerSymbolTarget target : camera.getLayerSymbolTargets())
		{
			if (target.getBounds() != null && target.getBounds().contains(point))
			{
				if (target.isFloor())
				{
					camera.setHoveredFloor(target.getFloor());
				}
				else
				{
					camera.setHoveredUnderground(target.getZone(), target.isSurfaceToUnderground());
				}
				return;
			}
		}

		camera.setHoveredUnderground(null, true);
	}

	private static int distanceTiles(WorldPoint point, WorldPoint from)
	{
		if (from == null || point == null)
		{
			return -1;
		}
		final int dx = point.getX() - from.getX();
		final int dy = point.getY() - from.getY();
		return (int) Math.sqrt((double) dx * dx + (double) dy * dy);
	}
}
