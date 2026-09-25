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

import com.bettermap.BetterMapConfig;
import com.bettermap.data.OverlayCluster;
import com.bettermap.data.TravelData;
import com.bettermap.data.UndergroundZone;
import com.bettermap.ui.BetterWorldMapOverlay;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.MouseListener;
import net.runelite.client.input.MouseWheelListener;

/**
 * Map controls: drag to pan (configurable mouse button), wheel to zoom about the cursor,
 * right click to zoom out or open underground-layer actions.
 *
 * <p>Events over the map are consumed so the client never sees them, which is what stops its
 * own pan and zoom fighting our camera.
 */
@Slf4j
@Singleton
public class WorldMapInput implements MouseListener, MouseWheelListener, KeyListener
{
	/** One wheel notch at the middle zoom-speed setting. */
	private static final double BASE_ZOOM_STEP = 0.12;

	private final MapCamera camera;
	private final BetterMapConfig config;
	private final javax.inject.Provider<com.bettermap.BetterMapPlugin> pluginProvider;
	private final MapFinder finder;
	private final PoiIndex poiIndex;
	private final MonsterIndex monsterIndex;
	private final SlayerTaskTracker slayerTaskTracker;

	private Point dragOrigin;
	/** Set while the quick-find orb is being dragged; distinct from {@link #dragOrigin} because the orb only exists with the map closed, where {@link #mouseDragged} otherwise bails. */
	private Point orbDragOrigin;
	private Point orbDragStartOffset;
	private boolean orbDragMoved;
	private volatile Point cursor;
	private String flyoutPendingKey;
	private long flyoutPendingSinceMs;
	/** Package-private for tests — production uses {@link MapCamera#FINDER_FLYOUT_DWELL_MS}. */
	long flyoutDwellMs = MapCamera.FINDER_FLYOUT_DWELL_MS;
	private long lastConsumeLogMs;
	private int consumeLogCount;

	@Inject
	public WorldMapInput(
		MapCamera camera,
		BetterMapConfig config,
		javax.inject.Provider<com.bettermap.BetterMapPlugin> pluginProvider,
		MapFinder finder,
		PoiIndex poiIndex,
		MonsterIndex monsterIndex,
		SlayerTaskTracker slayerTaskTracker)
	{
		this.camera = camera;
		this.config = config;
		this.pluginProvider = pluginProvider;
		this.finder = finder;
		this.poiIndex = poiIndex;
		this.monsterIndex = monsterIndex;
		this.slayerTaskTracker = slayerTaskTracker;
	}

	public WorldMapInput(
		MapCamera camera,
		BetterMapConfig config,
		javax.inject.Provider<com.bettermap.BetterMapPlugin> pluginProvider,
		MapFinder finder)
	{
		this(camera, config, pluginProvider, finder, null, null, null);
	}

	private void dbg(String format, Object... args)
	{
		if (config.debugLogging())
		{
			log.debug("[BetterMap:input] " + format, args);
		}
	}

	/** Rate-limit spammy consume lines so a stuck fullscreen grab is obvious without flooding. */
	private void dbgConsume(String reason, Point point, boolean consumed)
	{
		if (!config.debugLogging() || !consumed)
		{
			return;
		}
		final long now = System.currentTimeMillis();
		if (now - lastConsumeLogMs > 500 || consumeLogCount < 8)
		{
			lastConsumeLogMs = now;
			consumeLogCount++;
			final Rectangle view = camera.getViewport();
			log.debug("[BetterMap:input] CONSUME {} at {},{}  active={} fullscreen={} viewport={} drag={}",
				reason,
				point == null ? -1 : point.x, point == null ? -1 : point.y,
				camera.isActive(), config.fullscreenMap(),
				view == null ? "null" : view.width + "x" + view.height,
				dragOrigin != null);
		}
	}

	/** Last cursor position over the map, or null when the cursor is elsewhere. */
	public Point getCursor()
	{
		return cursor;
	}

	/**
	 * Clicks inside the finder card, wherever the card happens to be.
	 *
	 * <p>Split out of {@link #mousePressed} because the card now outlives the map: the minimap's
	 * search button opens it on its own, and in that mode the press never reaches the map viewport
	 * check that used to guard all of this.
	 *
	 * @return true if the card took the click
	 */
	private boolean handleFinderClick(MouseEvent event)
	{
		if (camera.isFinderPanelOpen())
		{
			if (clickedFinderChip(event.getPoint()))
			{
				event.consume();
				dbgConsume("finder-chip", event.getPoint(), true);
				return true;
			}

			if (clickedFlyoutItem(event.getPoint(), event.isShiftDown(), event.getClickCount()))
			{
				event.consume();
				dbgConsume("finder-flyout-item", event.getPoint(), true);
				return true;
			}

			final Rectangle expand = camera.getFinderExpandButton();
			if (expand != null && expand.contains(event.getPoint()))
			{
				camera.toggleFinderBodyExpanded();
				event.consume();
				dbgConsume("finder-expand", event.getPoint(), true);
				return true;
			}

			if (clickedFinderResult(event.getPoint(), event.isShiftDown(), event.getClickCount()))
			{
				event.consume();
				dbgConsume("finder-result", event.getPoint(), true);
				return true;
			}

			final Rectangle field = camera.getFinderFieldBounds();
			if (field != null && field.contains(event.getPoint()))
			{
				camera.setFinderFieldFocused(true);
				event.consume();   // clicking the field must not start a pan under the card
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
				event.consume();
				return true;
			}
		}

		return false;
	}

	@Override
	public MouseEvent mousePressed(MouseEvent event)
	{
		// RuneLite must receive the click that selects an entry from its open context menu. Without
		// this guard, the map's normal left-drag handler consumes that click first.
		if (pluginProvider.get().isClientMenuOpen())
		{
			return event;
		}

		// The quick-find orb by the minimap toggles the finder card without the world map, and can be
		// dragged to reposition. Only while the map is closed - the in-map Find button owns the
		// finder otherwise. Press just arms the drag; the toggle-vs-move decision is made on release.
		if (config.showFinderOrb() && !camera.isActive() && SwingUtilities.isLeftMouseButton(event))
		{
			final Rectangle orb = camera.getQuickFinderOrb();
			if (orb != null && orb.contains(event.getPoint()))
			{
				orbDragOrigin = event.getPoint();
				orbDragStartOffset = new Point(camera.getFinderOrbOffsetX(), camera.getFinderOrbOffsetY());
				orbDragMoved = false;
				event.consume();
				return event;
			}
		}

		// The finder card can be up with the map closed, in which case it is the only thing of ours
		// on screen and the viewport check below would throw the click away.
		if (camera.isFinderStandalone() && camera.isFinderPanelOpen()
			&& SwingUtilities.isLeftMouseButton(event))
		{
			if (handleFinderClick(event))
			{
				return event;
			}

			// Anywhere else puts the card away, the same as clicking off any other popup.
			camera.setFinderPanelOpen(false);
			return event;
		}

		if (!overMap(event.getPoint()))
		{
			if (config.debugLogging() && camera.isActive())
			{
				dbg("press NOT over map at {},{} (passthrough)", event.getX(), event.getY());
			}
			return event;
		}

		dbg("press over map at {},{} button={} closeBtn={} fullscreen={}",
			event.getX(), event.getY(),
			buttonName(event),
			camera.getCloseButton(),
			config.fullscreenMap());

		if (SwingUtilities.isLeftMouseButton(event))
		{
			final Rectangle gear = camera.getLayersButton();
			if (gear != null && gear.contains(event.getPoint()))
			{
				final boolean opening = !camera.isLayersPanelOpen();
				camera.setLayersPanelOpen(opening);
				if (opening)
				{
					camera.setFinderPanelOpen(false);
				}
				dbg("layers gear -> open={}", camera.isLayersPanelOpen());
				event.consume();
				dbgConsume("layers-gear", event.getPoint(), true);
				return event;
			}

			if (clickedLayerToggle(event.getPoint()))
			{
				event.consume();
				dbgConsume("layer-toggle", event.getPoint(), true);
				return event;
			}

			final Rectangle chat = camera.getChatButton();
			if (chat != null && chat.contains(event.getPoint()))
			{
				camera.setChatHidden(!camera.isChatHidden());
				dbg("chat toggle -> hidden={}", camera.isChatHidden());
				event.consume();
				dbgConsume("chat-button", event.getPoint(), true);
				return event;
			}

			final Rectangle find = camera.getFinderButton();
			if (find != null && find.contains(event.getPoint()))
			{
				final boolean opening = !camera.isFinderPanelOpen();
				camera.setFinderPanelOpen(opening);
				if (opening)
				{
					camera.setLayersPanelOpen(false);   // the two cards share the same corner
					finder.updateQuery("", pluginProvider.get().getPlayerLocation());
				}
				event.consume();
				dbgConsume("finder-button", event.getPoint(), true);
				return event;
			}

			if (handleFinderClick(event))
			{
				return event;
			}

			// A click on the map with the card still up drops the keyboard grab without closing the
			// card, so panning and zooming stay usable while the results are on screen.
			if (camera.isFinderFieldFocused())
			{
				final Rectangle panel = camera.getFinderPanelBounds();
				Rectangle flyout = camera.getFinderFlyoutHitBounds();
				if (flyout == null)
				{
					flyout = camera.getFinderFlyoutBounds();
				}
				final boolean onPanel = panel != null && panel.contains(event.getPoint());
				final boolean onFlyout = flyout != null && flyout.contains(event.getPoint());
				if (!onPanel && !onFlyout)
				{
					camera.setFinderFieldFocused(false);
					dbg("finder field unfocused by map click");
				}
			}

			final Rectangle close = camera.getCloseButton();
			if (close != null && close.contains(event.getPoint()))
			{
				dbg("CLOSE BUTTON hit bounds={} — calling closeMap()", close);
				pluginProvider.get().closeMap();
				event.consume();
				dbgConsume("close-button", event.getPoint(), true);
				return event;
			}

			final Rectangle returnBtn = camera.getUndergroundReturnButton();
			if (returnBtn != null && returnBtn.contains(event.getPoint()))
			{
				camera.exitDungeonToSurface();
				dbg("underground return button");
				event.consume();
				dbgConsume("underground-return", event.getPoint(), true);
				return event;
			}

			final Rectangle clueBtn = camera.getClueButton();
			if (clueBtn != null && clueBtn.contains(event.getPoint()))
			{
				pluginProvider.get().goToClue();
				dbg("go-to-clue button");
				event.consume();
				dbgConsume("go-to-clue", event.getPoint(), true);
				return event;
			}

			final Rectangle questBtn = camera.getQuestButton();
			if (questBtn != null && questBtn.contains(event.getPoint()))
			{
				pluginProvider.get().goToQuestStep();
				dbg("go-to-quest-step button");
				event.consume();
				dbgConsume("go-to-quest-step", event.getPoint(), true);
				return event;
			}

			final Rectangle playerBtn = camera.getPlayerButton();
			if (playerBtn != null && playerBtn.contains(event.getPoint()))
			{
				pluginProvider.get().goToPlayer();
				dbg("go-to-player button");
				event.consume();
				dbgConsume("go-to-player", event.getPoint(), true);
				return event;
			}

			final Rectangle cluePanBtn = camera.getCluePanelPanButton();
			if (cluePanBtn != null && cluePanBtn.contains(event.getPoint()))
			{
				pluginProvider.get().goToClue();
				dbg("clue-panel pan button");
				event.consume();
				dbgConsume("clue-panel-pan", event.getPoint(), true);
				return event;
			}

			final Rectangle clueHeader = camera.getCluePanelHeaderBounds();
			if (clueHeader != null && clueHeader.contains(event.getPoint()))
			{
				camera.setCluePanelCollapsed(!camera.isCluePanelCollapsed());
				dbg("clue-panel-header toggle");
				event.consume();
				dbgConsume("clue-panel-header", event.getPoint(), true);
				return event;
			}

			final Rectangle cluePanel = camera.getCluePanelBounds();
			if (cluePanel != null && cluePanel.contains(event.getPoint()))
			{
				event.consume();
				dbgConsume("clue-panel", event.getPoint(), true);
				return event;
			}

			if (clickedLayerSymbol(event.getPoint()))
			{
				event.consume();
				dbgConsume("layer-symbol", event.getPoint(), true);
				return event;
			}

			if (clickedPlaneButton(event.getPoint()))
			{
				event.consume();
				dbgConsume("plane-button", event.getPoint(), true);
				return event;
			}
		}

		// A right-click over the open Find card must raise the client menu (its "Route to" entries,
		// added in BetterMapPlugin.onMenuOpened) instead of being eaten below for zoom-out.
		if (SwingUtilities.isRightMouseButton(event) && camera.isFinderPanelOpen())
		{
			final Rectangle panel = camera.getFinderPanelBounds();
			Rectangle flyout = camera.getFinderFlyoutHitBounds();
			if (flyout == null)
			{
				flyout = camera.getFinderFlyoutBounds();
			}
			if ((panel != null && panel.contains(event.getPoint()))
				|| (flyout != null && flyout.contains(event.getPoint())))
			{
				return event;
			}
		}

		// A click anywhere off the open Layers panel dismisses it, but still pans/zooms.
		if (camera.isLayersPanelOpen())
		{
			final Rectangle panel = camera.getLayersPanelBounds();
			final Rectangle gearBtn = camera.getLayersButton();
			final boolean onPanel = panel != null && panel.contains(event.getPoint());
			final boolean onGear = gearBtn != null && gearBtn.contains(event.getPoint());
			if (!onPanel && !onGear)
			{
				camera.setLayersPanelOpen(false);
			}
		}

		final boolean panThisButton = config.dragToPan() && config.panButton().matches(event);

		if (SwingUtilities.isRightMouseButton(event) && !panThisButton)
		{
			return event;
		}

		if (panThisButton)
		{
			dragOrigin = event.getPoint();
			dbg("drag start at {},{} button={}", dragOrigin.x, dragOrigin.y, buttonName(event));
			event.consume();
			dbgConsume("drag-start", event.getPoint(), true);
		}

		return event;
	}

	@Override
	public MouseEvent mouseDragged(MouseEvent event)
	{
		if (orbDragOrigin != null)
		{
			final Point now = event.getPoint();
			final int dx = now.x - orbDragOrigin.x;
			final int dy = now.y - orbDragOrigin.y;
			if (Math.abs(dx) + Math.abs(dy) > 3)
			{
				orbDragMoved = true;
			}
			camera.setFinderOrbOffset(orbDragStartOffset.x + dx, orbDragStartOffset.y + dy);
			cursor = now;
			event.consume();
			return event;
		}

		if (dragOrigin == null)
		{
			return event;
		}

		// If the map deactivated mid-drag, stop eating mouse events immediately.
		if (!camera.isActive())
		{
			dbg("drag aborted — camera inactive, releasing mouse grab");
			dragOrigin = null;
			return event;
		}

		final Point now = event.getPoint();
		camera.panByScreen(now.x - dragOrigin.x, now.y - dragOrigin.y);
		dragOrigin = now;
		cursor = now;
		event.consume();
		dbgConsume("drag", event.getPoint(), true);
		return event;
	}

	@Override
	public MouseEvent mouseReleased(MouseEvent event)
	{
		if (orbDragOrigin != null)
		{
			if (orbDragMoved)
			{
				// Persist the new orb position.
				pluginProvider.get().setFinderOrbOffset(
					camera.getFinderOrbOffsetX(), camera.getFinderOrbOffsetY());
			}
			else
			{
				// A click, not a drag: toggle the standalone finder card.
				if (camera.isFinderStandalone() && camera.isFinderPanelOpen())
				{
					camera.setFinderPanelOpen(false);
				}
				else
				{
					camera.setFinderStandalone(true);
					camera.setFinderPanelOpen(true);
					finder.updateQuery("", pluginProvider.get().getPlayerLocation());
				}
			}
			orbDragOrigin = null;
			orbDragStartOffset = null;
			orbDragMoved = false;
			event.consume();
			return event;
		}

		if (dragOrigin != null)
		{
			dbg("drag end, centre now {},{} active={}",
				(int) camera.getCenterX(), (int) camera.getCenterY(), camera.isActive());
			dragOrigin = null;
			event.consume();
			dbgConsume("drag-end", event.getPoint(), true);
		}
		return event;
	}

	@Override
	public MouseWheelEvent mouseWheelMoved(MouseWheelEvent event)
	{
		// Standalone finder: no map to zoom, so the wheel only ever scrolls its lists.
		if (camera.isFinderStandalone() && camera.isFinderPanelOpen())
		{
			final Rectangle flyoutVp = camera.getFinderFlyoutViewport();
			if (flyoutVp != null && flyoutVp.contains(event.getPoint()))
			{
				camera.scrollFinderFlyout(event.getWheelRotation());
				event.consume();
				return event;
			}
			final Rectangle bodyVp = camera.getFinderBodyViewport();
			if (bodyVp != null && bodyVp.contains(event.getPoint()))
			{
				camera.scrollFinderBody(event.getWheelRotation());
				event.consume();
				return event;
			}
			return event;
		}

		if (!overMap(event.getPoint()))
		{
			return event;
		}

		if (camera.isFinderPanelOpen())
		{
			final Rectangle flyoutVp = camera.getFinderFlyoutViewport();
			if (flyoutVp != null && flyoutVp.contains(event.getPoint()))
			{
				camera.scrollFinderFlyout(event.getWheelRotation());
				event.consume();
				dbgConsume("finder-flyout-wheel", event.getPoint(), true);
				return event;
			}

			final Rectangle bodyVp = camera.getFinderBodyViewport();
			if (bodyVp != null && bodyVp.contains(event.getPoint()))
			{
				camera.scrollFinderBody(event.getWheelRotation());
				event.consume();
				dbgConsume("finder-body-wheel", event.getPoint(), true);
				return event;
			}
		}

		final double step = 1 + BASE_ZOOM_STEP * Math.max(1, config.zoomSpeed());
		final int rotation = config.invertZoom() ? -event.getWheelRotation() : event.getWheelRotation();
		final double factor = rotation < 0 ? step : 1 / step;

		final double before = camera.getZoom();
		camera.zoomBy(factor, event.getX(), event.getY());
		event.consume();
		dbgConsume("wheel", event.getPoint(), true);
		if (config.debugLogging())
		{
			dbg("wheel zoom {} -> {}", String.format("%.3f", before), String.format("%.3f", camera.getZoom()));
		}
		return event;
	}

	@Override
	public MouseEvent mouseMoved(MouseEvent event)
	{
		// Standalone finder: keep row hover, flyout dwell and edge-scroll alive with no map behind it.
		if (camera.isFinderStandalone() && camera.isFinderPanelOpen())
		{
			cursor = event.getPoint();
			updateHoveredFinderRow(cursor);
			updateFinderEdgeScroll(cursor);
			return event;
		}
		if (overMap(event.getPoint()))
		{
			cursor = event.getPoint();
			updateHoveredLayerSymbol(cursor);
			updateHoveredTravelNode(cursor);
			updateHoveredFinderRow(cursor);
			updateFinderEdgeScroll(cursor);
		}
		else
		{
			cursor = null;
			camera.setHoveredUnderground(null, true);
			camera.setHoveredTravelNode(null);
			clearFinderFlyoutHover();
		}
		return event;
	}

	@Override
	public MouseEvent mouseClicked(MouseEvent event)
	{
		return event;
	}

	@Override
	public MouseEvent mouseEntered(MouseEvent event)
	{
		return event;
	}

	@Override
	public MouseEvent mouseExited(MouseEvent event)
	{
		cursor = null;
		camera.setHoveredUnderground(null, true);
		camera.setHoveredTravelNode(null);
		clearFinderFlyoutHover();
		return event;
	}

	private void clearFinderFlyoutHover()
	{
		flyoutPendingKey = null;
		flyoutPendingSinceMs = 0L;
		camera.setHoveredRowKey(null);
	}

	private void updateHoveredFinderRow(Point point)
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

	/** Promotes a pending row hover to an open flyout once the dwell timer elapses. */
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

	private void updateFinderEdgeScroll(Point point)
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

	private void updateHoveredTravelNode(Point point)
	{
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

		final int worldX = (int) Math.floor(camera.worldX(point.x, view));
		final int worldY = (int) Math.floor(camera.worldY(point.y, view));
		final int radius = (int) Math.max(3, Math.ceil(12 / Math.max(0.4, camera.getZoom())));

		final TravelData.TravelNode node = InstanceMaps.firstHit(worldX, worldY,
			camera.getFocusedUndergroundZone(), camera.isDungeonContentsFocused(),
			(x, y) -> TravelData.findNodeNear(x, y, camera.getPlane(), radius));
		camera.setHoveredTravelNode(node);
	}

	private void updateHoveredLayerSymbol(Point point)
	{
		for (MapCamera.LayerSymbolTarget target : camera.getLayerSymbolTargets())
		{
			if (target.getBounds() != null && target.getBounds().contains(point))
			{
				if (target.isRegion())
				{
					camera.setHoveredOverlayCluster(target.getCluster());
				}
				else if (target.isFloor())
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
		camera.setHoveredOverlayCluster(null);
		camera.setHoveredUnderground(null, true);
	}

	private boolean clickedLayerSymbol(Point point)
	{
		for (MapCamera.LayerSymbolTarget target : camera.getLayerSymbolTargets())
		{
			if (target.getBounds() != null && target.getBounds().contains(point))
			{
				if (target.isRegion())
				{
					if (camera.getActiveOverlayCluster() == target.getCluster())
					{
						camera.setActiveOverlayCluster(null);
						camera.setHoveredOverlayCluster(null);
					}
					else
					{
						camera.setActiveOverlayCluster(target.getCluster());
					}
					return true;
				}
				if (target.isSurfaceToUnderground())
				{
					camera.setUndergroundMode(target.getZone());
					if (target.isFloor())
					{
						camera.setActiveFloor(target.getFloor());
					}
					log.debug("[BetterMap] entered underground mode for {}", target.getZone().getName());
				}
				else
				{
					camera.exitDungeonToSurface();
					log.debug("[BetterMap] returned to surface from {}", target.getZone().getName());
				}
				return true;
			}
		}
		return false;
	}

	private boolean clickedLayerToggle(Point point)
	{
		for (MapCamera.LayerToggleTarget target : camera.getLayerToggleTargets())
		{
			if (target.getBounds() != null && target.getBounds().contains(point))
			{
				if (BetterWorldMapOverlay.HIDE_ALL_LAYERS_KEY.equals(target.getConfigKey()))
				{
					final boolean showAll = target.getCurrentValue();
					pluginProvider.get().setAllLayersEnabled(showAll);
					log.debug("[BetterMap] layer hide-all -> {}", showAll ? "show all" : "hide all");
					return true;
				}

				final boolean next = !target.getCurrentValue();
				pluginProvider.get().setLayerEnabled(target.getConfigKey(), next);
				log.debug("[BetterMap] layer toggle {} -> {}", target.getConfigKey(), next);
				return true;
			}
		}
		return false;
	}

	private boolean clickedFinderChip(Point point)
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
		final WorldPoint playerLoc = pluginProvider.get().getPlayerLocation();
		if (poiIndex == null)
		{
			return;
		}
		final PoiIndex.Poi bank = poiIndex.nearestByKey("bank",
			playerLoc != null ? playerLoc.getX() : 0, playerLoc != null ? playerLoc.getY() : 0);
		if (bank != null)
		{
			pluginProvider.get().centerMapOn(new WorldPoint(bank.getX(), bank.getY(), bank.getPlane()));
		}
	}

	private void handleSlayerChip()
	{
		final WorldPoint playerLoc = pluginProvider.get().getPlayerLocation();
		if (slayerTaskTracker == null)
		{
			return;
		}
		final WorldPoint target = slayerTaskTracker.nearestTaskLocation(playerLoc, monsterIndex, poiIndex);
		if (target != null)
		{
			pluginProvider.get().centerMapOn(target);
		}
	}

	private void handleTravelChip()
	{
		final WorldPoint playerLoc = pluginProvider.get().getPlayerLocation();
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
		if (best != null)
		{
			pluginProvider.get().centerMapOn(best.getLocation());
		}
	}

	private void handleLastChip()
	{
		final String last = finder.getLastQuery();
		if (last != null && !last.isEmpty())
		{
			finder.updateQuery(last, pluginProvider.get().getPlayerLocation());
		}
	}

	/**
	 * Single click on a Find row: pan there, and build the path too unless the user has set
	 * {@link BetterMapConfig#finderRowClick()} back to go-to-only.
	 *
	 * @param groupHeader true when callers explicitly need pan-only behavior
	 */
	private void activateRow(WorldPoint point)
	{
		pluginProvider.get().centerMapOn(point);
	}

	private void activateRowAndClose(WorldPoint point)
	{
		pluginProvider.get().centerMapOn(point);
		if (camera.isActive())
		{
			pluginProvider.get().closeMap();
		}
	}

	private boolean clickedFlyoutItem(Point point, boolean shiftDown, int clickCount)
	{
		for (MapCamera.FlyoutTarget target : camera.getFlyoutTargets())
		{
			final Rectangle row = target.getRowBounds();
			if (row != null && row.contains(point))
			{
				if (clickCount >= 2 || shiftDown)
				{
					activateRowAndClose(target.getPoint());
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

	private boolean clickedFinderResult(Point point, boolean shiftDown, int clickCount)
	{
		for (MapCamera.FinderResultTarget target : camera.getFinderResultTargets())
		{
			final Rectangle row = target.getRowBounds();
			if (row != null && row.contains(point))
			{
				if (target.getRegion() != null)
				{
					final WorldPoint regCenter = new WorldPoint(target.getRegion().getCenterX(), target.getRegion().getCenterY(), 0);
					if (clickCount >= 2 || shiftDown)
					{
						activateRowAndClose(regCenter);
					}
					else
					{
						activateRow(regCenter);
					}
					return true;
				}

				final MapFinder.Result activated = MapFinder.activationTarget(target.getResult());
				final WorldPoint activatedPoint = activated != null ? activated.getPoint() : target.getPoint();
				if (clickCount >= 2 || shiftDown)
				{
					activateRowAndClose(activatedPoint);
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

	/**
	 * Steps out one zoom level. Once the whole layer is on screen, a further right click on a
	 * dungeon layer pops back to the surface, the way a nested map view backs out.
	 */
	private void zoomOutStep()
	{
		final Rectangle view = camera.getViewport();
		if (view == null)
		{
			return;
		}

		final double fitZoom = camera.zoomToFit(view);
		camera.setZoom(Math.max(fitZoom, camera.getZoom() * 0.5));
		if (log.isDebugEnabled())
		{
			log.debug("[BetterMap] right click: zoom out to {}", String.format("%.3f", camera.getZoom()));
		}
	}

	private boolean clickedPlaneButton(Point point)
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
			camera.setActiveOverlayCluster(null);
			camera.setPlane(1);
			log.debug("[BetterMap] floor button Upper (plane 1) clicked");
			return true;
		}

		// Button 1: "Main" (plane 0)
		if (buttons[1] != null && buttons[1].contains(point))
		{
			camera.clearUndergroundMode();
			camera.setActiveOverlayCluster(null);
			camera.setPlane(0);
			log.debug("[BetterMap] floor button Main (plane 0) clicked");
			return true;
		}

		// Button 2: "Lower" (nearest underground cluster)
		if (buttons[2] != null && buttons[2].contains(point))
		{
			final OverlayCluster nearest = OverlayCluster.nearestCluster(camera.getCenterX(), camera.getCenterY());
			if (nearest != null)
			{
				camera.clearUndergroundMode();
				camera.setActiveOverlayCluster(nearest);
				log.debug("[BetterMap] floor button Lower clicked -> activated cluster {}", nearest.name);
			}
			return true;
		}

		return false;
	}

	private boolean overMap(Point point)
	{
		if (!camera.isActive())
		{
			return false;
		}

		final Rectangle view = camera.getViewport();
		if (view == null || !view.contains(point))
		{
			return false;
		}

		for (Rectangle passthrough : camera.getNativePassthrough())
		{
			if (passthrough != null && passthrough.contains(point))
			{
				return false;
			}
		}

		return true;
	}

	private static String buttonName(MouseEvent event)
	{
		if (SwingUtilities.isLeftMouseButton(event))
		{
			return "left";
		}
		if (SwingUtilities.isRightMouseButton(event))
		{
			return "right";
		}
		if (SwingUtilities.isMiddleMouseButton(event))
		{
			return "middle";
		}
		return "other";
	}

	@Override
	public void keyTyped(KeyEvent event)
	{
		// Finder text entry is the chatbox input's job now (see FinderKeyCapture); nothing to do here.
	}

	@Override
	public void keyPressed(KeyEvent event)
	{
		// Tracked above the focus bail-out on purpose: Shift+Enter in the finder is exactly the case
		// where the field HAS focus, and FinderKeyCapture's chatbox callback carries no modifiers.
		// The chatbox input ignores Shift itself, so we always see the press.
		if (event.getKeyCode() == KeyEvent.VK_SHIFT)
		{
			camera.setShiftHeld(true);
			return;
		}

		// While the search field holds focus the chatbox input owns the keyboard - Esc closes the
		// search, not the map, and Space/C are query characters. Stay out of its way.
		if (camera.isFinderFieldFocused())
		{
			return;
		}

		if (event.getKeyCode() == KeyEvent.VK_ESCAPE)
		{
			dbg("ESC pressed  active={} fullscreen={} — {}",
				camera.isActive(), config.fullscreenMap(),
				(camera.isActive() && config.fullscreenMap()) ? "calling closeMap()" : "ignoring");
			if (camera.isActive() && config.fullscreenMap())
			{
				pluginProvider.get().closeMap();
				event.consume();
			}
			return;
		}

		if (config.undergroundTuner() && camera.isActive() && camera.getTunableZone() != null
			&& handleTunerKey(event))
		{
			event.consume();
			return;
		}

		// Gate on focus, not on the panel being up: with the card open but unfocused the keyboard is
		// the map's again, so centre-on-player should work rather than being dead until you close it.
		if ((event.getKeyCode() == KeyEvent.VK_SPACE || event.getKeyCode() == KeyEvent.VK_C)
			&& camera.isActive() && !camera.isFinderFieldFocused())
		{
			final WorldPoint raw = pluginProvider.get() != null ? pluginProvider.get().getPlayerLocation() : null;
			if (raw != null)
			{
				final WorldPoint here = camera.getPlayerDisplayLocation() != null
					? camera.getPlayerDisplayLocation()
					: (raw.getY() > InstanceMaps.GAP_MIN_Y
						? new WorldPoint(
							InstanceMaps.toDisplayX(raw.getX(), raw.getY(), 0, 0),
							InstanceMaps.toDisplayY(raw.getX(), raw.getY(), 0, 0),
							raw.getPlane())
						: raw);
				camera.centerOn(here.getX(), here.getY());
				camera.setPlane(here.getPlane());
				dbg("center-on-player key={} at {},{},{}", event.getKeyCode(), here.getX(), here.getY(), here.getPlane());
				event.consume();
			}
		}
	}

	/**
	 * Dev-only dungeon-layer tuner. Active only while {@link BetterMapConfig#undergroundTuner()} is
	 * on and a dungeon layer is open. Arrow keys slide the tile layer over the faint surface, [ / ]
	 * tighten / loosen the clip, R resets, P prints the corrected coordinates to chat.
	 */
	private boolean handleTunerKey(KeyEvent event)
	{
		final com.bettermap.data.UndergroundZone zone = camera.getTunableZone();
		if (zone == null)
		{
			return false;
		}
		final DungeonTuner tuner = camera.getDungeonTuner();
		final int step = event.isShiftDown() ? 10 : 1;
		switch (event.getKeyCode())
		{
			case KeyEvent.VK_LEFT:
				tuner.nudge(zone, -step, 0);
				break;
			case KeyEvent.VK_RIGHT:
				tuner.nudge(zone, step, 0);
				break;
			case KeyEvent.VK_UP:
				tuner.nudge(zone, 0, step);
				break;
			case KeyEvent.VK_DOWN:
				tuner.nudge(zone, 0, -step);
				break;
			case KeyEvent.VK_OPEN_BRACKET:
				tuner.trim(zone, step);
				break;
			case KeyEvent.VK_CLOSE_BRACKET:
				tuner.trim(zone, -step);
				break;
			case KeyEvent.VK_R:
				tuner.reset(zone);
				break;
			case KeyEvent.VK_P:
				pluginProvider.get().printToChat("[Better Map tuner] " + tuner.describe(zone));
				log.debug("[BetterMap tuner] {}", tuner.describe(zone));
				break;
			default:
				return false;
		}
		return true;
	}

	@Override
	public void keyReleased(KeyEvent event)
	{
		// The chatbox input handles its own press/release lifecycle; the Shift latch is ours.
		if (event.getKeyCode() == KeyEvent.VK_SHIFT)
		{
			camera.setShiftHeld(false);
		}
	}

	/**
	 * Alt-tabbing away used to leave a live drag and a focused search field behind, so the map kept
	 * eating input on the way back. Drop both, and the hover state with them — it is stale the
	 * moment the cursor is somewhere else.
	 */
	@Override
	public void focusLost()
	{
		dragOrigin = null;
		orbDragOrigin = null;
		orbDragStartOffset = null;
		orbDragMoved = false;
		cursor = null;
		camera.setShiftHeld(false);
		camera.setFinderFieldFocused(false);
		camera.setHoveredUnderground(null, true);
		camera.setHoveredTravelNode(null);
	}
}
