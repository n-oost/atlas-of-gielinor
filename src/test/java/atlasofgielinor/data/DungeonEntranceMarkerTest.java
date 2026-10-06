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
package atlasofgielinor.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import net.runelite.api.coords.WorldPoint;
import org.junit.Test;

import atlasofgielinor.data.dungeons.UndergroundZone;

public class DungeonEntranceMarkerTest
{
	@org.junit.BeforeClass
	public static void loadMapData()
	{
		atlasofgielinor.map.MapCatalogLoader.load();
		org.junit.Assert.assertTrue("Map catalogs must load before testing", atlasofgielinor.map.MapCatalogLoader.isReady());
	}

	@Test
	public void markerPositionsLoadAndApply()
	{
		final WorldPoint cellar = UndergroundZone.LUMBRIDGE_CELLAR.getEntranceMarkerPoint(0);
		assertNotNull(cellar);
		assertEquals(3209, cellar.getX());
		assertEquals(3218, cellar.getY());
		assertEquals(new WorldPoint(3218, 3218, 0), UndergroundZone.LUMBRIDGE_CELLAR.getSurfacePoints().get(0));
		assertEquals(UndergroundZone.LUMBRIDGE_CELLAR.getSurfacePoint(), UndergroundZone.LUMBRIDGE_CELLAR.getSurfacePoints().get(0));

		final WorldPoint prif = UndergroundZone.PRIFDDINAS_UNDERGROUND.getEntranceMarkerPoint(0);
		assertNotNull(prif);
		assertEquals(2211, prif.getX());
		assertEquals(3303, prif.getY());
	}

	@Test
	public void hiddenEntranceMarkersAreIdentified()
	{
		assertTrue(UndergroundZone.CHASM_OF_TEARS.isEntranceMarkerHidden(0));
		assertTrue(UndergroundZone.ECTOFUNTUS_DUNGEON.isEntranceMarkerHidden(1));
		assertFalse(UndergroundZone.LUMBRIDGE_CELLAR.isEntranceMarkerHidden(0));
		assertTrue(UndergroundZone.PATERDOMUS_BASEMENT.isEntranceMarkerHidden(1));
		assertTrue(UndergroundZone.PORT_SARIM_RAT_PITS.isEntranceMarkerHidden(1));
	}

}
