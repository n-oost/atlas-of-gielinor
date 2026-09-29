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

import com.bettermap.BetterMapConfig;
import com.bettermap.map.MapCamera;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseWheelEvent;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles map drag panning, momentum/bounds, minimap quick-finder orb dragging,
 * and mouse-wheel zoom stepping.
 */
@Slf4j
public class MapDragController
{
	/** One wheel notch at the middle zoom-speed setting. */
	public static final double BASE_ZOOM_STEP = 0.12;
	/** Ignore hand jitter until the pointer has moved at least this far from the orb press. */
	private static final int ORB_DRAG_THRESHOLD = 5;

	private final MapCamera camera;
	private final BetterMapConfig config;

	@Getter
	private Point dragOrigin;
	@Getter
	private Point orbDragOrigin;
	@Getter
	private Point orbDragStartOffset;
	@Getter
	private boolean orbDragMoved;

	public MapDragController(MapCamera camera, BetterMapConfig config)
	{
		this.camera = camera;
		this.config = config;
	}

	public void startMapDrag(Point point)
	{
		this.dragOrigin = point;
	}

	public boolean isDraggingMap()
	{
		return dragOrigin != null;
	}

	public boolean handleMapDrag(Point now)
	{
		if (dragOrigin == null)
		{
			return false;
		}

		if (!camera.isActive())
		{
			dragOrigin = null;
			return false;
		}

		camera.panByScreen(now.x - dragOrigin.x, now.y - dragOrigin.y);
		dragOrigin = now;
		return true;
	}

	public void endMapDrag()
	{
		dragOrigin = null;
	}

	public void startOrbDrag(Point point, int currentOffsetX, int currentOffsetY)
	{
		this.orbDragOrigin = point;
		this.orbDragStartOffset = new Point(currentOffsetX, currentOffsetY);
		this.orbDragMoved = false;
	}

	public boolean isDraggingOrb()
	{
		return orbDragOrigin != null;
	}

	public boolean handleOrbDrag(Point now)
	{
		if (orbDragOrigin == null)
		{
			return false;
		}

		final int dx = now.x - orbDragOrigin.x;
		final int dy = now.y - orbDragOrigin.y;
		if (!orbDragMoved && dx * dx + dy * dy <= ORB_DRAG_THRESHOLD * ORB_DRAG_THRESHOLD)
		{
			return true;
		}
		if (dx * dx + dy * dy > ORB_DRAG_THRESHOLD * ORB_DRAG_THRESHOLD)
		{
			orbDragMoved = true;
		}
		if (orbDragMoved)
		{
			camera.setFinderOrbOffset(orbDragStartOffset.x + dx, orbDragStartOffset.y + dy);
		}
		return true;
	}

	public boolean endOrbDrag()
	{
		final boolean moved = orbDragMoved;
		orbDragOrigin = null;
		orbDragStartOffset = null;
		orbDragMoved = false;
		return moved;
	}

	/**
	 * Handles mouse-wheel zooming or panel list scrolling.
	 *
	 * @return true if consumed
	 */
	public boolean handleMouseWheel(MouseWheelEvent event, boolean overMap)
	{
		// Standalone finder: no map to zoom, so the wheel only ever scrolls its lists.
		if (camera.isFinderStandalone() && camera.isFinderPanelOpen())
		{
			final Rectangle flyoutVp = camera.getFinderFlyoutViewport();
			if (flyoutVp != null && flyoutVp.contains(event.getPoint()))
			{
				camera.scrollFinderFlyout(event.getWheelRotation());
				return true;
			}
			final Rectangle bodyVp = camera.getFinderBodyViewport();
			if (bodyVp != null && bodyVp.contains(event.getPoint()))
			{
				camera.scrollFinderBody(event.getWheelRotation());
				return true;
			}
			return false;
		}

		if (!overMap)
		{
			return false;
		}

		if (camera.isFinderPanelOpen())
		{
			final Rectangle flyoutVp = camera.getFinderFlyoutViewport();
			if (flyoutVp != null && flyoutVp.contains(event.getPoint()))
			{
				camera.scrollFinderFlyout(event.getWheelRotation());
				return true;
			}

			final Rectangle bodyVp = camera.getFinderBodyViewport();
			if (bodyVp != null && bodyVp.contains(event.getPoint()))
			{
				camera.scrollFinderBody(event.getWheelRotation());
				return true;
			}
		}

		final double step = 1 + BASE_ZOOM_STEP * Math.max(1, config.zoomSpeed());
		final int rotation = config.invertZoom() ? -event.getWheelRotation() : event.getWheelRotation();
		final double factor = rotation < 0 ? step : 1 / step;

		camera.zoomBy(factor, event.getX(), event.getY());
		return true;
	}

	/**
	 * Steps out one zoom level.
	 */
	public void zoomOutStep()
	{
		final Rectangle view = camera.getViewport();
		if (view == null)
		{
			return;
		}

		final double fitZoom = camera.zoomToFit(view);
		camera.setZoom(Math.max(fitZoom, camera.getZoom() * 0.5));
	}

	public void reset()
	{
		dragOrigin = null;
		orbDragOrigin = null;
		orbDragStartOffset = null;
		orbDragMoved = false;
	}
}
