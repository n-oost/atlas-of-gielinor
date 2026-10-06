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

import java.awt.event.KeyEvent;
import javax.inject.Provider;

import atlasofgielinor.AtlasOfGielinorPlugin;
import atlasofgielinor.map.MapCamera;
import net.runelite.api.coords.WorldPoint;

/**
 * Handles keyboard navigation, hotkeys (Escape, Space, C).
 */
public class MapKeyHandler
{
	private final MapCamera camera;
	private final Provider<AtlasOfGielinorPlugin> pluginProvider;

	public MapKeyHandler(MapCamera camera, Provider<AtlasOfGielinorPlugin> pluginProvider)
	{
		this.camera = camera;
		this.pluginProvider = pluginProvider;
	}

	public boolean keyPressed(KeyEvent event)
	{
		if (camera.isFinderFieldFocused())
		{
			return false;
		}

		if (event.getKeyCode() == KeyEvent.VK_ESCAPE)
		{
			// Hub review F2: leave genuine Escape input to the native client.
			// Do not consume it and dispatch a replacement close action.
			return false;
		}

		if ((event.getKeyCode() == KeyEvent.VK_SPACE || event.getKeyCode() == KeyEvent.VK_C)
			&& camera.isActive() && !camera.isFinderFieldFocused())
		{
			final WorldPoint raw = pluginProvider != null && pluginProvider.get() != null
				? pluginProvider.get().getPlayerLocation() : null;
			if (raw != null)
			{
				pluginProvider.get().goToPlayer();
				event.consume();
				return true;
			}
		}

		return false;
	}


}
