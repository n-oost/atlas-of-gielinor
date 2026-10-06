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
package atlasofgielinor.ui.input;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseWheelEvent;

import atlasofgielinor.AtlasOfGielinorConfig;
import atlasofgielinor.map.MapCamera;
import lombok.Getter;

/**
 * Handles map drag panning and mouse-wheel zoom stepping.
 */
public class MapDragController
{
	/** One wheel notch at the middle zoom-speed setting. */
	public static final double BASE_ZOOM_STEP = 0.12;

	private final MapCamera camera;
	private final AtlasOfGielinorConfig config;

	@Getter
	private Point dragOrigin;

	public MapDragController(MapCamera camera, AtlasOfGielinorConfig config)
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

	/**
	 * Handles mouse-wheel zooming or panel list scrolling.
	 *
	 * @return true if consumed
	 */
	public boolean handleMouseWheel(MouseWheelEvent event, boolean overMap)
	{
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
	}
}
