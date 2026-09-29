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
import com.bettermap.data.UndergroundZone;
import com.bettermap.map.DungeonTuner;
import com.bettermap.map.InstanceMaps;
import com.bettermap.map.MapCamera;
import java.awt.event.KeyEvent;
import javax.inject.Provider;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;

/**
 * Handles keyboard navigation, hotkeys (Escape, Space, C), Shift modifier tracking,
 * and dev-tuner keyboard controls.
 */
@Slf4j
public class MapKeyHandler
{
	private final MapCamera camera;
	private final BetterMapConfig config;
	private final Provider<BetterMapPlugin> pluginProvider;

	public MapKeyHandler(MapCamera camera, BetterMapConfig config, Provider<BetterMapPlugin> pluginProvider)
	{
		this.camera = camera;
		this.config = config;
		this.pluginProvider = pluginProvider;
	}

	public boolean keyPressed(KeyEvent event)
	{
		if (event.getKeyCode() == KeyEvent.VK_SHIFT)
		{
			camera.setShiftHeld(true);
			return false;
		}

		if (camera.isFinderFieldFocused())
		{
			return false;
		}

		if (event.getKeyCode() == KeyEvent.VK_ESCAPE)
		{
			if (camera.isActive() && config.fullscreenMap())
			{
				if (pluginProvider != null && pluginProvider.get() != null)
				{
					pluginProvider.get().closeMap();
				}
				event.consume();
				return true;
			}
			return false;
		}

		if (config.undergroundTuner() && camera.isActive() && camera.getTunableZone() != null
			&& handleTunerKey(event))
		{
			event.consume();
			return true;
		}

		if ((event.getKeyCode() == KeyEvent.VK_SPACE || event.getKeyCode() == KeyEvent.VK_C)
			&& camera.isActive() && !camera.isFinderFieldFocused())
		{
			final WorldPoint raw = pluginProvider != null && pluginProvider.get() != null
				? pluginProvider.get().getPlayerLocation() : null;
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
				event.consume();
				return true;
			}
		}

		return false;
	}

	public void keyReleased(KeyEvent event)
	{
		if (event.getKeyCode() == KeyEvent.VK_SHIFT)
		{
			camera.setShiftHeld(false);
		}
	}

	private boolean handleTunerKey(KeyEvent event)
	{
		final UndergroundZone zone = camera.getTunableZone();
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
				if (pluginProvider != null && pluginProvider.get() != null)
				{
					pluginProvider.get().printToChat("[Better Map tuner] " + tuner.describe(zone));
				}
				log.debug("[BetterMap tuner] {}", tuner.describe(zone));
				break;
			default:
				return false;
		}
		return true;
	}
}
