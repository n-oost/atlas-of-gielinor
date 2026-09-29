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

import com.bettermap.BetterMapConfig;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Covers both branches of {@link SyncedPathfindingConfig}: reading through to the real Shortest
 * Path plugin\'s own settings in external mode, and using Better Map\'s own settings in local mode.
 */
public class SyncedPathfindingConfigTest
{
	private static final BetterMapConfig EXTERNAL = new BetterMapConfig()
	{
		@Override
		public boolean useExternalShortestPathSettings()
		{
			return true;
		}
	};

	/** In-memory stand-in for {@code ConfigManager.getConfiguration("shortestpath", key, type)}. */
	private static SyncedPathfindingConfig.ExternalConfigReader readerOf(Map<String, Object> values)
	{
		return new SyncedPathfindingConfig.ExternalConfigReader()
		{
			@Override
			public <T> T get(String key, Class<T> type)
			{
				return type.cast(values.get(key));
			}
		};
	}

	private static ShortestPathTracker bridge(boolean available)
	{
		return new ShortestPathTracker(null, null, null, null, null)
		{
			@Override
			public boolean isAvailable()
			{
				return available;
			}
		};
	}

	@Test
	public void bridgeUnavailable_readsBetterMapsOwnSection()
	{
		final BetterMapConfig config = new BetterMapConfig()
		{
			@Override
			public boolean routeAvoidWilderness()
			{
				return false;
			}
		};
		final SyncedPathfindingConfig synced =
			new SyncedPathfindingConfig(readerOf(new HashMap<>()), config, bridge(false));

		assertFalse(synced.avoidWilderness());
	}

	@Test
	public void bridgeAvailable_readsRealPluginsLiveValue()
	{
		final Map<String, Object> external = new HashMap<>();
		external.put("avoidWilderness", Boolean.FALSE);

		// The external value must win over Better Map's default (true).
		final SyncedPathfindingConfig synced =
			new SyncedPathfindingConfig(readerOf(external), EXTERNAL, bridge(true));

		assertFalse(synced.avoidWilderness());
	}

	@Test
	public void bridgeAvailable_unsetKeyFallsBackToInterfaceDefault()
	{
		final BetterMapConfig config = EXTERNAL;
		final SyncedPathfindingConfig synced =
			new SyncedPathfindingConfig(readerOf(new HashMap<>()), config, bridge(true));

		// avoidWilderness's upstream default is true; nothing was ever set on the real plugin.
		assertTrue(synced.avoidWilderness());
	}

	@Test
	public void pohSettings_onlySyncFromRealPlugin_neverFromBetterMapsOwnSection()
	{
		final BetterMapConfig config = EXTERNAL;

		final SyncedPathfindingConfig withoutPlugin =
			new SyncedPathfindingConfig(readerOf(new HashMap<>()), config, bridge(false));
		assertFalse("POH is out of Better Map's own scope, so it stays off without the real plugin",
			withoutPlugin.usePoh());

		final Map<String, Object> external = new HashMap<>();
		external.put("usePoh", Boolean.TRUE);
		final SyncedPathfindingConfig withPlugin =
			new SyncedPathfindingConfig(readerOf(external), config, bridge(true));
		assertTrue("With the real plugin installed, its own usePoh setting is honored",
			withPlugin.usePoh());
	}

	@Test
	public void transportThreshold_syncsFromRealPluginWhenPresent()
	{
		final BetterMapConfig config = EXTERNAL;
		final Map<String, Object> external = new HashMap<>();
		external.put("costBoats", 15);

		final SyncedPathfindingConfig withPlugin =
			new SyncedPathfindingConfig(readerOf(external), config, bridge(true));
		assertEquals(15, withPlugin.costBoats());

		final SyncedPathfindingConfig withoutPlugin =
			new SyncedPathfindingConfig(readerOf(new HashMap<>()), config, bridge(false));
		assertEquals(0, withoutPlugin.costBoats());
	}

	@Test
	public void localModeIgnoresEnabledExternalPlugin()
	{
		Map<String, Object> external = new HashMap<>();
		external.put("avoidWilderness", Boolean.FALSE);
		SyncedPathfindingConfig synced = new SyncedPathfindingConfig(
			readerOf(external), new BetterMapConfig() {}, bridge(true));
		assertTrue(synced.avoidWilderness());
		assertFalse(synced.usePoh());
	}

	@Test
	public void writingBuiltPohTeleports_isAlwaysANoOp()
	{
		final BetterMapConfig config = EXTERNAL;
		final SyncedPathfindingConfig synced =
			new SyncedPathfindingConfig(readerOf(new HashMap<>()), config, bridge(true));

		// Must never throw and must never attempt to write into the real plugin's saved config.
		synced.setBuiltTeleportationBoxes("1=0 0 0");
		synced.setBuiltTeleportationPortalsPoh("2=0 0 0");
	}
}
