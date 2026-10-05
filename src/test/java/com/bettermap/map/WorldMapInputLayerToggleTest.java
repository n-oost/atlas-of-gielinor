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

import com.google.gson.Gson;
import com.bettermap.BetterMapConfig;
import com.bettermap.BetterMapPlugin;
import com.bettermap.ui.BetterWorldMapOverlay;
import java.awt.Canvas;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.Collections;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

public class WorldMapInputLayerToggleTest
{
	@org.junit.BeforeClass
	public static void loadMapData()
	{
		com.bettermap.map.MapData.load();
		org.junit.Assert.assertTrue("Map catalogs must load before testing", com.bettermap.map.MapData.isReady());
	}

	private final Canvas source = new Canvas();

	private MapCamera camera;
	private WorldMapInput input;

	private String capturedKey;
	private Boolean capturedValue;
	private Boolean capturedAllLayers;

	@Before
	public void setUp()
	{
		camera = new MapCamera();
		camera.setViewport(new Rectangle(0, 0, 800, 600));
		camera.setActive(true);

		final BetterMapConfig config = new BetterMapConfig()
		{
			@Override
			public boolean dragToPan()
			{
				return false;
			}

		};

		final BetterMapPlugin plugin = new BetterMapPlugin()
		{
			@Override
			public void setLayerEnabled(String key, boolean value)
			{
				capturedKey = key;
				capturedValue = value;
			}

			@Override
			public void setAllLayersEnabled(boolean enabled)
			{
				capturedAllLayers = enabled;
			}
		};

		final MapFinder finder = new MapFinder(new PoiIndex(), new MonsterIndex(new Gson()));
		input = new WorldMapInput(camera, config, () -> plugin, finder);
	}

	private MouseEvent leftPressAt(int x, int y)
	{
		return new MouseEvent(source, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(),
			MouseEvent.BUTTON1_DOWN_MASK, x, y, 1, false, MouseEvent.BUTTON1);
	}

	@Test
	public void clickingARowWritesTheFlippedConfigValueAndConsumesTheEvent()
	{
		camera.setLayerToggleTargets(Collections.singletonList(
			new MapCamera.LayerToggleTarget(new Rectangle(100, 100, 160, 18), "showTooltips", true)));

		final MouseEvent event = leftPressAt(120, 108);
		input.mousePressed(event);

		assertEquals("showTooltips", capturedKey);
		assertEquals(Boolean.FALSE, capturedValue);
		assertTrue("the map must swallow the click", event.isConsumed());
	}

	@Test
	public void clickingHideAllShowsEveryLayer()
	{
		camera.setLayerToggleTargets(Collections.singletonList(
			new MapCamera.LayerToggleTarget(new Rectangle(100, 40, 160, 18),
				BetterWorldMapOverlay.HIDE_ALL_LAYERS_KEY, true)));

		final MouseEvent event = leftPressAt(120, 48);
		input.mousePressed(event);

		assertEquals(Boolean.TRUE, capturedAllLayers);
		assertNull(capturedKey);
		assertTrue(event.isConsumed());
	}

	@Test
	public void clickingHideAllHidesEveryLayer()
	{
		camera.setLayerToggleTargets(Collections.singletonList(
			new MapCamera.LayerToggleTarget(new Rectangle(100, 40, 160, 18),
				BetterWorldMapOverlay.HIDE_ALL_LAYERS_KEY, false)));

		final MouseEvent event = leftPressAt(120, 48);
		input.mousePressed(event);

		assertEquals(Boolean.FALSE, capturedAllLayers);
		assertNull(capturedKey);
		assertTrue(event.isConsumed());
	}

	@Test
	public void clickingTheGearTogglesThePanelOpenState()
	{
		camera.setLayersButton(new Rectangle(700, 8, 22, 22));
		assertFalse(camera.isLayersPanelOpen());

		input.mousePressed(leftPressAt(710, 18));
		assertTrue(camera.isLayersPanelOpen());

		input.mousePressed(leftPressAt(710, 18));
		assertFalse(camera.isLayersPanelOpen());
	}

	@Test
	public void clickingOffThePanelDismissesItWithoutTouchingConfig()
	{
		camera.setLayersButton(new Rectangle(700, 8, 22, 22));
		camera.setLayersPanelBounds(new Rectangle(540, 34, 170, 220));
		camera.setLayersPanelOpen(true);

		input.mousePressed(leftPressAt(100, 400));

		assertFalse(camera.isLayersPanelOpen());
		assertNull("dismissing must not write a config key", capturedKey);
	}

	@Test
	public void clicksOutsideTheViewportAreIgnored()
	{
		camera.setLayersButton(new Rectangle(700, 8, 22, 22));

		input.mousePressed(leftPressAt(900, 700));

		assertFalse(camera.isLayersPanelOpen());
	}
}
