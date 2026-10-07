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
package atlasofgielinor.map;

import atlasofgielinor.map.catalog.BossLocationIndex;
import atlasofgielinor.map.catalog.MapRegion;
import atlasofgielinor.map.catalog.PoiDetails;
import atlasofgielinor.map.catalog.MapCatalog;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import atlasofgielinor.data.BossLocationData;
import atlasofgielinor.data.dungeons.DungeonFloor;
import atlasofgielinor.data.TravelData;
import atlasofgielinor.data.dungeons.UndergroundZone;
import atlasofgielinor.tiles.WikiMap;

/** Loads dependent map catalogs on the startup worker before map input and rendering activate. */
@Slf4j
public final class MapCatalogLoader
{
	private static volatile boolean ready;

	private MapCatalogLoader()
	{
	}

	public static boolean isReady()
	{
		return ready;
	}

	public static void load()
	{
		if (ready) return;
		try
		{
			MapCatalog.load();
		}
		catch (IOException e)
		{
			log.debug("Could not load canonical map catalog", e);
			return;
		}
		if (!MapCatalog.isLoaded() || Thread.currentThread().isInterrupted()) return;
		PrifddinasShift.load();
		if (!PrifddinasShift.isLoaded() || Thread.currentThread().isInterrupted()) return;
		MapRegion.load();
		if (!MapRegion.isLoaded() || Thread.currentThread().isInterrupted()) return;
		WikiMap.load();
		if (!WikiMap.isLoaded() || Thread.currentThread().isInterrupted()) return;
		UndergroundZone.load();
		if (!UndergroundZone.isLoaded() || Thread.currentThread().isInterrupted()) return;
		DungeonFloor.load();
		if (Thread.currentThread().isInterrupted()) return;
		InstanceMaps.load();
		if (Thread.currentThread().isInterrupted()) return;
		BossLocationData.load();
		if (!BossLocationData.isLoaded() || Thread.currentThread().isInterrupted()) return;
		BossLocationIndex.all();
		if (!MonsterIconManager.load() || Thread.currentThread().isInterrupted()) return;
		TravelData.load();
		if (!TravelData.isLoaded() || Thread.currentThread().isInterrupted()) return;
		PoiDetails.load();
		if (!PoiDetails.isLoaded() || Thread.currentThread().isInterrupted()) return;
		ready = true;
	}
}
