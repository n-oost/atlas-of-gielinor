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

import com.bettermap.BetterMapConfig;
import com.bettermap.ui.BetterWorldMapOverlay;
import com.bettermap.ui.BetterWorldMapOverlay.LayerToggle;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;

/**
 * The on-map Layers panel writes {@code bettermap.<key>} strings straight to ConfigManager, so a
 * typo would silently toggle nothing. Pin every row to a real boolean config method.
 */
public class LayerTogglesTest
{
	@Test
	public void everyToggleKeyResolvesToARealBooleanConfigMethod()
	{
		for (LayerToggle toggle : BetterWorldMapOverlay.LAYER_TOGGLES)
		{
			assertNotNull("label", toggle.label);
			assertFalse("label must not be blank", toggle.label.trim().isEmpty());
			assertNotNull("getter for " + toggle.key, toggle.getter);

			final Method method;
			try
			{
				method = BetterMapConfig.class.getMethod(toggle.key);
			}
			catch (NoSuchMethodException e)
			{
				fail("no BetterMapConfig." + toggle.key + "() — key is wrong or the config method was renamed");
				return;
			}

			final Class<?> returnType = method.getReturnType();
			assertTrue(toggle.key + " must return boolean, was " + returnType,
				returnType == boolean.class || returnType == Boolean.class);
		}
	}

	@Test
	public void toggleKeysAreUnique()
	{
		final Set<String> seen = new HashSet<>();
		for (LayerToggle toggle : BetterWorldMapOverlay.LAYER_TOGGLES)
		{
			assertTrue("duplicate layer toggle key: " + toggle.key, seen.add(toggle.key));
		}
	}

	@Test
	public void layerPanelIncludesEveryPoiCategoryToggle()
	{
		final Set<String> keys = new HashSet<>();
		for (LayerToggle toggle : BetterWorldMapOverlay.LAYER_TOGGLES)
		{
			keys.add(toggle.key);
		}

		assertTrue(keys.contains("iconBanks"));
		assertTrue(keys.contains("iconShops"));
		assertTrue(keys.contains("iconSkilling"));
		assertTrue(keys.contains("iconTravel"));
		assertTrue(keys.contains("iconQuests"));
		assertTrue(keys.contains("iconAltars"));
		assertTrue(keys.contains("iconDungeons"));
		assertTrue(keys.contains("iconServices"));
		assertTrue(keys.contains("showPlaceNames"));
		assertTrue(keys.contains("showLargeUndergroundSymbols"));
	}

	@Test
	public void allLayersHiddenReflectsEveryToggle()
	{
		final BetterMapConfig allOn = new BetterMapConfig()
		{
		};
		assertFalse(BetterWorldMapOverlay.allLayersHidden(allOn));

		final BetterMapConfig allOff = new BetterMapConfig()
		{
			@Override
			public boolean iconBanks()
			{
				return false;
			}

			@Override
			public boolean iconShops()
			{
				return false;
			}

			@Override
			public boolean iconSkilling()
			{
				return false;
			}

			@Override
			public boolean iconTravel()
			{
				return false;
			}

			@Override
			public boolean iconQuests()
			{
				return false;
			}

			@Override
			public boolean iconAltars()
			{
				return false;
			}

			@Override
			public boolean iconDungeons()
			{
				return false;
			}

			@Override
			public boolean iconServices()
			{
				return false;
			}

			@Override
			public boolean showPlaceNames()
			{
				return false;
			}

			@Override
			public boolean showLargeUndergroundSymbols()
			{
				return false;
			}

			@Override
			public boolean showBossLocations()
			{
				return false;
			}

			@Override
			public boolean showMonsterZones()
			{
				return false;
			}

			@Override
			public boolean showMonsterIcons()
			{
				return false;
			}

			@Override
			public boolean showMonsterLabels()
			{
				return false;
			}

			@Override
			public boolean showTravelRoutes()
			{
				return false;
			}

			@Override
			public boolean showBoatLocations()
			{
				return false;
			}

			@Override
			public boolean showSailingPorts()
			{
				return false;
			}

			@Override
			public boolean showGroundItems()
			{
				return false;
			}

			@Override
			public boolean showPluginMarkers()
			{
				return false;
			}

			@Override
			public boolean showPlayerMarker()
			{
				return false;
			}

			@Override
			public boolean showTooltips()
			{
				return false;
			}
		};
		assertTrue(BetterWorldMapOverlay.allLayersHidden(allOff));
	}

	@Test
	public void gettersReadWithoutThrowing()
	{
		final BetterMapConfig defaults = new BetterMapConfig()
		{
		};

		for (LayerToggle toggle : BetterWorldMapOverlay.LAYER_TOGGLES)
		{
			// Just exercising it: a broken method reference would not compile, but this guards
			// against a getter wired to the wrong (throwing) method later.
			toggle.getter.test(defaults);
		}

		assertEquals("sanity: the curated set is non-trivial", true,
			BetterWorldMapOverlay.LAYER_TOGGLES.size() >= 5);
	}
}
