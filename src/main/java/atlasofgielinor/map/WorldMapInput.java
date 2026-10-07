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
package atlasofgielinor.map;

import atlasofgielinor.integrations.SlayerTaskTracker;
import atlasofgielinor.map.catalog.MonsterIndex;
import atlasofgielinor.map.catalog.PoiIndex;
import atlasofgielinor.map.MapFinder;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import net.runelite.client.input.KeyListener;
import java.awt.event.MouseEvent;
import net.runelite.client.input.MouseListener;
import java.awt.event.MouseWheelEvent;
import net.runelite.client.input.MouseWheelListener;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;

import atlasofgielinor.AtlasOfGielinorConfig;
import atlasofgielinor.AtlasOfGielinorPlugin;
import atlasofgielinor.ui.input.MapContextMenuHandler;
import atlasofgielinor.ui.input.MapDragController;
import atlasofgielinor.ui.input.MapKeyHandler;
import atlasofgielinor.ui.input.MapLayerInputHandler;
import atlasofgielinor.ui.markers.RaidBossDisplay;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Top-level RuneLite input dispatcher bridging RuneLite's {@link net.runelite.client.input.MouseManager}
 * and {@link net.runelite.client.input.KeyManager} to specialized input controllers:
 * <ul>
 *   <li>{@link MapDragController} - map panning, orb dragging, wheel zoom</li>
 *   <li>{@link MapContextMenuHandler} - client menu detection, right-click actions</li>
 *   <li>{@link MapLayerInputHandler} - UI button hit tests, chips, layer toggles, finder interaction</li>
 *   <li>{@link MapKeyHandler} - hotkeys and Shift state</li>
 * </ul>
 */
@Slf4j
@Singleton
public class WorldMapInput implements MouseListener, MouseWheelListener, KeyListener
{
	private final MapCamera camera;
	private final AtlasOfGielinorConfig config;
	private final Provider<AtlasOfGielinorPlugin> pluginProvider;
	private final MapFinder finder;

	@Getter
	private final MapDragController dragController;
	@Getter
	private final MapContextMenuHandler contextMenuHandler;
	@Getter
	private final MapLayerInputHandler layerInputHandler;
	@Getter
	private final MapKeyHandler keyHandler;

	@Inject
	private RaidBossDisplay raidBossDisplay;
	private boolean raidPressConsumed;
	private boolean routePressConsumed;
	private volatile Point cursor;
	private Point travelClickOrigin;
	private Point layerClickOrigin;
	private MapCamera.LayerSymbolTarget pendingLayerSymbol;
	private static final int CLICK_TOLERANCE_SQUARED = 25;
	private volatile boolean shiftDown;
	private volatile boolean altDown;
	private volatile boolean ctrlDown;

	/** Package-private for tests — production uses {@link MapCamera#FINDER_FLYOUT_DWELL_MS}. */
	long flyoutDwellMs = MapCamera.FINDER_FLYOUT_DWELL_MS;
	private long lastConsumeLogMs;
	private int consumeLogCount;

	@Inject
	public WorldMapInput(
		MapCamera camera,
		AtlasOfGielinorConfig config,
		Provider<AtlasOfGielinorPlugin> pluginProvider,
		MapFinder finder,
		PoiIndex poiIndex,
		MonsterIndex monsterIndex,
		SlayerTaskTracker slayerTaskTracker)
	{
		this.camera = camera;
		this.config = config;
		this.pluginProvider = pluginProvider;
		this.finder = finder;

		this.dragController = new MapDragController(camera, config);
		this.contextMenuHandler = new MapContextMenuHandler(camera, pluginProvider);
		this.layerInputHandler = new MapLayerInputHandler(
			camera, pluginProvider, finder, poiIndex, monsterIndex, slayerTaskTracker);
		this.keyHandler = new MapKeyHandler(camera, pluginProvider);
	}

	public WorldMapInput(
		MapCamera camera,
		AtlasOfGielinorConfig config,
		Provider<AtlasOfGielinorPlugin> pluginProvider,
		MapFinder finder)
	{
		this(camera, config, pluginProvider, finder, null, null, null);
	}

	private void dbg(String format, Object... args)
	{
		if (config.debugLogging())
		{
			log.debug("[AtlasOfGielinor:input] " + format, args);
		}
	}

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
			log.debug("[AtlasOfGielinor:input] CONSUME {} at {},{}  active={} fullscreen={} viewport={} drag={}\",",
				reason,
				point == null ? -1 : point.x, point == null ? -1 : point.y,
				camera.isActive(), config.fullscreenMap(),
				view == null ? "null" : view.width + "x" + view.height,
				dragController.isDraggingMap());
		}
	}

	/** Last cursor position over the map, or null when the cursor is elsewhere. */
	public Point getCursor()
	{
		return cursor;
	}

	public boolean isTooltipExpandModifierDown()
	{
		switch (config.tooltipExpandModifier())
		{
			case ALT:
				return altDown;
			case CTRL:
				return ctrlDown;
			default:
				return shiftDown;
		}
	}

	@Override
	public MouseEvent mousePressed(MouseEvent event)
	{
		travelClickOrigin = null;
		layerClickOrigin = null;
		pendingLayerSymbol = null;
		raidPressConsumed = false;
		routePressConsumed = false;
		if (contextMenuHandler.isClientMenuOpen())
		{
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

		if (raidBossDisplay != null && raidBossDisplay.press(event.getPoint(), SwingUtilities.isLeftMouseButton(event)))
		{
			raidPressConsumed = true;
			cursor = event.getPoint();
			event.consume();
			return event;
		}

		dbg("press over map at {},{} button={} fullscreen={}",
			event.getX(), event.getY(),
			buttonName(event),
			config.fullscreenMap());

		if (SwingUtilities.isLeftMouseButton(event))
		{
			final boolean deferLayerSymbol = config.dragToPan() && config.panButton().matches(event);
			final String uiHit = layerInputHandler.handleUiButtonClick(event.getPoint(), deferLayerSymbol);
			if (uiHit != null)
			{
				if (deferLayerSymbol && "layer-symbol".equals(uiHit))
				{
					// Capture press priority: overlapping symbols must not change the click target.
					pendingLayerSymbol = layerInputHandler.findLayerSymbol(event.getPoint());
					layerClickOrigin = event.getPoint();
					dragController.startMapDrag(layerClickOrigin);
				}
				event.consume();
				dbgConsume(uiHit, event.getPoint(), true);
				return event;
			}

			layerInputHandler.setFlyoutDwellMs(flyoutDwellMs);
			if (layerInputHandler.handleFinderClick(event))
			{
				event.consume();
				return event;
			}

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

			if ((shiftDown || event.isShiftDown()) && pluginProvider != null && pluginProvider.get() != null)
			{
				pluginProvider.get().placeRouteAt(event.getX(), event.getY());
				routePressConsumed = true;
				event.consume();
				return event;
			}

			if (camera.isTravelViewActive() || (config.showTravelRoutes() && camera.getZoom() >= config.travelStationMinZoom()))
			{
				travelClickOrigin = event.getPoint();
			}
		}

		if (contextMenuHandler.isRightClickOverFinder(event))
		{
			return event;
		}

		contextMenuHandler.checkDismissLayersPanel(event);

		final boolean panThisButton = config.dragToPan() && config.panButton().matches(event);

		if (SwingUtilities.isRightMouseButton(event) && !panThisButton)
		{
			return event;
		}

		if (panThisButton)
		{
			dragController.startMapDrag(event.getPoint());
			dbg("drag start at {},{} button={}", event.getX(), event.getY(), buttonName(event));
			event.consume();
			dbgConsume("drag-start", event.getPoint(), true);
		}

		return event;
	}

	@Override
	public MouseEvent mouseDragged(MouseEvent event)
	{
		if (travelClickOrigin != null && travelClickOrigin.distanceSq(event.getPoint()) > CLICK_TOLERANCE_SQUARED)
		{
			travelClickOrigin = null;
		}
		if (!dragController.isDraggingMap())
		{
			return event;
		}

		if (!camera.isActive())
		{
			pendingLayerSymbol = null;
			layerClickOrigin = null;
			dbg("drag aborted — camera inactive, releasing mouse grab");
			dragController.endMapDrag();
			return event;
		}

		if (pendingLayerSymbol != null)
		{
			if (layerClickOrigin.distanceSq(event.getPoint()) <= CLICK_TOLERANCE_SQUARED)
			{
				event.consume();
				return event;
			}
			pendingLayerSymbol = null;
			layerClickOrigin = null;
		}
		final Point now = event.getPoint();
		dragController.handleMapDrag(now);
		cursor = now;
		event.consume();
		dbgConsume("drag", event.getPoint(), true);
		return event;
	}

	@Override
	public MouseEvent mouseReleased(MouseEvent event)
	{
		if (pendingLayerSymbol != null)
		{
			final MapCamera.LayerSymbolTarget target = pendingLayerSymbol;
			final Point origin = layerClickOrigin;
			pendingLayerSymbol = null;
			layerClickOrigin = null;
			dragController.endMapDrag();
			if (SwingUtilities.isLeftMouseButton(event) && overMap(event.getPoint())
				&& origin.distanceSq(event.getPoint()) <= CLICK_TOLERANCE_SQUARED
				&& target.getBounds().contains(event.getPoint()))
			{
				layerInputHandler.activateLayerSymbol(target);
			}
			event.consume();
			return event;
		}
		if (raidPressConsumed || routePressConsumed)
		{
			event.consume();
			return event;
		}
		if (travelClickOrigin != null && SwingUtilities.isLeftMouseButton(event)
			&& travelClickOrigin.distanceSq(event.getPoint()) <= CLICK_TOLERANCE_SQUARED && overMap(event.getPoint()))
		{
			layerInputHandler.updateHoveredTravelNode(event.getPoint());
			camera.setSelectedTravelNode(camera.getSelectedTravelNode() == camera.getHoveredTravelNode()
				? null : camera.getHoveredTravelNode());
			if (camera.getSelectedTravelNode() != null)
			{
				camera.fitTravelRoutes(camera.getSelectedTravelNode(), camera.getViewport());
			}
		}
		travelClickOrigin = null;
		if (dragController.isDraggingMap())
		{
			dbg("drag end, centre now {},{} active={}",
				(int) camera.getCenterX(), (int) camera.getCenterY(), camera.isActive());
			dragController.endMapDrag();
			event.consume();
			dbgConsume("drag-end", event.getPoint(), true);
		}
		return event;
	}

	@Override
	public MouseWheelEvent mouseWheelMoved(MouseWheelEvent event)
	{
		if (raidBossDisplay != null && raidBossDisplay.isOpen() && overMap(event.getPoint()))
		{
			event.consume();
			return event;
		}
		final boolean over = overMap(event.getPoint());
		if (dragController.handleMouseWheel(event, over))
		{
			event.consume();
			dbgConsume("wheel", event.getPoint(), true);
		}
		return event;
	}

	@Override
	public MouseEvent mouseMoved(MouseEvent event)
	{
		if (raidBossDisplay != null && raidBossDisplay.isOpen() && overMap(event.getPoint()))
		{
			cursor = event.getPoint();
			return event;
		}
		layerInputHandler.setFlyoutDwellMs(flyoutDwellMs);
		if (overMap(event.getPoint()))
		{
			cursor = event.getPoint();
			layerInputHandler.updateHoveredLayerSymbol(cursor);
			layerInputHandler.updateHoveredTravelNode(cursor);
			layerInputHandler.updateHoveredFinderRow(cursor);
			layerInputHandler.updateFinderEdgeScroll(cursor);
		}
		else
		{
			cursor = null;
			camera.setHoveredUnderground(null, true);
			camera.setHoveredTravelNode(null);
			layerInputHandler.clearFinderFlyoutHover();
		}
		return event;
	}

	@Override
	public MouseEvent mouseClicked(MouseEvent event)
	{
		if (raidPressConsumed || routePressConsumed)
		{
			raidPressConsumed = false;
			routePressConsumed = false;
			event.consume();
		}
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
		layerInputHandler.clearFinderFlyoutHover();
		return event;
	}

	/** Promotes a pending row hover to an open flyout once the dwell timer elapses. */
	public void tickFinderFlyoutDwell()
	{
		layerInputHandler.setFlyoutDwellMs(flyoutDwellMs);
		layerInputHandler.tickFinderFlyoutDwell();
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
	}

	@Override
	public void keyPressed(KeyEvent event)
	{
		setModifier(event.getKeyCode(), true);
		if (raidBossDisplay != null && raidBossDisplay.isOpen() && camera.isActive())
		{
			if (event.getKeyCode() == KeyEvent.VK_ESCAPE)
			{
				raidBossDisplay.close();
			}
			event.consume();
			return;
		}
		keyHandler.keyPressed(event);
	}

	@Override
	public void keyReleased(KeyEvent event)
	{
		setModifier(event.getKeyCode(), false);
	}

	private void setModifier(int keyCode, boolean down)
	{
		switch (keyCode)
		{
			case KeyEvent.VK_SHIFT:
				shiftDown = down;
				break;
			case KeyEvent.VK_ALT:
			case KeyEvent.VK_ALT_GRAPH:
				altDown = down;
				break;
			case KeyEvent.VK_CONTROL:
				ctrlDown = down;
				break;
			default:
				break;
		}
	}

	@Override
	public void focusLost()
	{
		pendingLayerSymbol = null;
		layerClickOrigin = null;
		travelClickOrigin = null;
		routePressConsumed = false;
		dragController.reset();
		cursor = null;
		shiftDown = false;
		altDown = false;
		ctrlDown = false;
		camera.setFinderFieldFocused(false);
		camera.setHoveredUnderground(null, true);
		camera.setHoveredTravelNode(null);
		layerInputHandler.clearFinderFlyoutHover();
	}
}
