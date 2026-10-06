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
package bettermap.ui;

import static bettermap.ui.MapStyle.LEFT_TOOLBAR_BUTTON_SIZE;

import java.awt.Rectangle;

import bettermap.BetterMapConfig;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;

/**
 * Where the map's own buttons and chips sit inside the viewport.
 *
 * <p>Shared by {@link BetterWorldMapOverlay} and {@link MapChromeRenderer} so the toolbar and
 * the click rects published for {@link bettermap.map.WorldMapInput} are measured once.
 */
class MapLayout
{
	private static final int LEFT_TOOLBAR_GAP = 6;

	private final Client client;
	private final BetterMapConfig config;

	MapLayout(Client client, BetterMapConfig config)
	{
		this.client = client;
		this.config = config;
	}

	int leftToolbarTop(Rectangle bounds)
	{
		// Status chip is y+8 with height 20.
		return (int) bounds.getMinY() + 34;
	}

	int leftToolbarButtonX(Rectangle bounds, int slot)
	{
		return (int) bounds.getMinX() + 8 + slot * (LEFT_TOOLBAR_BUTTON_SIZE + LEFT_TOOLBAR_GAP);
	}
	/** Right edge of the button strip, inside the overview inset. */
	int topRightStripRight(Rectangle bounds)
	{
		return (int) bounds.getMaxX() - 8 - rightUiInset(bounds);
	}

	/**
	 * Space to leave clear on the right for the overview minimap (and a fallback when its
	 * widget bounds are missing). Uses distance from the canvas right edge to the overview's
	 * left edge — not just overview width — so plugin controls sit beside the minimap.
	 */
	int rightUiInset(Rectangle bounds)
	{
		if (client == null || config == null)
		{
			return 0;
		}

		final int margin = 10;
		Widget overview = client.getWidget(InterfaceID.Worldmap.OVERVIEW_CONTAINER);
		if (overview == null || overview.isHidden())
		{
			overview = client.getWidget(InterfaceID.Worldmap.OVERVIEW_DISPLAY);
		}
		if (overview != null && !overview.isHidden())
		{
			final Rectangle ovBounds = overview.getBounds();
			if (ovBounds != null && ovBounds.width > 0 && ovBounds.height > 0)
			{
				final int fromRight = (int) Math.ceil(bounds.getMaxX() - ovBounds.getMinX());
				if (fromRight > 0)
				{
					return fromRight + margin;
				}
			}
		}

		// Fullscreen often still shows the overview while its widget reports odd bounds.
		if (config.fullscreenMap())
		{
			return Math.min(230, Math.max(0, bounds.width / 6)) + margin;
		}

		return 0;
	}
	private static final int[] WORLD_MAP_ORB_IDS = {
		InterfaceID.Orbs.ORB_WORLDMAP,
		InterfaceID.Orbs.WORLDMAP,
		InterfaceID.OrbsNomap.ORB_WORLDMAP,
		InterfaceID.OrbsNomap.WORLDMAP
	};

	/** Canvas-space bounds of the native world-map orb, or null when the minimap is not showing it. */
	static Rectangle worldMapOrbBounds(Client client)
	{
		if (client == null)
		{
			return null;
		}
		Rectangle bounds = null;
		for (int id : WORLD_MAP_ORB_IDS)
		{
			final Widget orb = client.getWidget(id);
			if (orb == null || orb.isHidden())
			{
				continue;
			}
			final Rectangle orbBounds = orb.getBounds();
			if (orbBounds != null && orbBounds.width > 0 && orbBounds.height > 0)
			{
				if (bounds == null)
				{
					bounds = new Rectangle(orbBounds);
				}
				else
				{
					bounds.add(orbBounds);
				}
			}
		}
		return bounds;
	}
}
