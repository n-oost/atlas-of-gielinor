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
package com.bettermap.ui;

import java.awt.Rectangle;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;

/** Locating the client's native minimap orbs in canvas space. */
final class MinimapOrbs
{
	/**
	 * The four IDs the world-map orb goes by: sprite layer vs clickable layer, "classic" minimap
	 * vs resizable-modern / no-minimap layout. Probe all, take the first that is really on screen.
	 */
	private static final int[] WORLD_MAP_ORB_IDS = {
		InterfaceID.Orbs.ORB_WORLDMAP,
		InterfaceID.Orbs.WORLDMAP,
		InterfaceID.OrbsNomap.ORB_WORLDMAP,
		InterfaceID.OrbsNomap.WORLDMAP
	};

	private MinimapOrbs()
	{
	}

	/** Canvas-space bounds of the native world-map orb, or null when the minimap is not showing it. */
	static Rectangle worldMapOrbBounds(Client client)
	{
		if (client == null)
		{
			return null;
		}
		for (int id : WORLD_MAP_ORB_IDS)
		{
			final Widget orb = client.getWidget(id);
			if (orb == null || orb.isHidden())
			{
				continue;
			}
			final Rectangle bounds = orb.getBounds();
			if (bounds != null && bounds.width > 0 && bounds.height > 0)
			{
				return bounds;
			}
		}
		return null;
	}
}
