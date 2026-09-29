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
import com.bettermap.BetterMapPlugin;
import com.bettermap.map.MapCamera;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import javax.inject.Provider;
import javax.swing.SwingUtilities;

/**
 * Handles right-click interactions, context menu pass-through, and popup dismissals.
 */
public class MapContextMenuHandler
{
	private final MapCamera camera;
	private final BetterMapConfig config;
	private final Provider<BetterMapPlugin> pluginProvider;

	public MapContextMenuHandler(
		MapCamera camera,
		BetterMapConfig config,
		Provider<BetterMapPlugin> pluginProvider)
	{
		this.camera = camera;
		this.config = config;
		this.pluginProvider = pluginProvider;
	}

	/**
	 * Returns true if the client menu is currently open.
	 */
	public boolean isClientMenuOpen()
	{
		return pluginProvider != null && pluginProvider.get() != null && pluginProvider.get().isClientMenuOpen();
	}

	/**
	 * Checks if a right-click occurred over an open Finder panel or flyout,
	 * which should raise client menu entries (e.g., "Route to") instead of zooming.
	 */
	public boolean isRightClickOverFinder(MouseEvent event)
	{
		if (SwingUtilities.isRightMouseButton(event) && camera.isFinderPanelOpen())
		{
			final Rectangle panel = camera.getFinderPanelBounds();
			Rectangle flyout = camera.getFinderFlyoutHitBounds();
			if (flyout == null)
			{
				flyout = camera.getFinderFlyoutBounds();
			}
			return (panel != null && panel.contains(event.getPoint()))
				|| (flyout != null && flyout.contains(event.getPoint()));
		}
		return false;
	}

	/**
	 * Checks if a click occurred outside the open Layers panel and gear button,
	 * dismissing the layers panel if so.
	 */
	public void checkDismissLayersPanel(MouseEvent event)
	{
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
	}
}
