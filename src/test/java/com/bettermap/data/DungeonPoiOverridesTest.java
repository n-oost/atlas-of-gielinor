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
package com.bettermap.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import net.runelite.api.coords.WorldPoint;
import org.junit.Test;

public class DungeonPoiOverridesTest
{
	@org.junit.BeforeClass
	public static void loadMapData()
	{
		com.bettermap.map.MapData.load();
		org.junit.Assert.assertTrue("Map catalogs must load before testing", com.bettermap.map.MapData.isReady());
	}

	@Test
	public void buttonOverridesLoadAndApply()
	{
		final WorldPoint cellar = DungeonPoiOverrides.getButtonPoint(UndergroundZone.LUMBRIDGE_CELLAR, 0);
		assertNotNull(cellar);
		assertEquals(3209, cellar.getX());
		assertEquals(3218, cellar.getY());

		final WorldPoint prif = DungeonPoiOverrides.getButtonPoint(UndergroundZone.PRIFDDINAS_UNDERGROUND, 0);
		assertNotNull(prif);
		assertEquals(2211, prif.getX());
		assertEquals(3303, prif.getY());
	}

	@Test
	public void deletedButtonsAreIdentified()
	{
		assertTrue(DungeonPoiOverrides.isButtonDeleted(UndergroundZone.CHASM_OF_TEARS, 0));
		assertTrue(DungeonPoiOverrides.isButtonDeleted(UndergroundZone.ECTOFUNTUS_DUNGEON, 1));
		assertFalse(DungeonPoiOverrides.isButtonDeleted(UndergroundZone.LUMBRIDGE_CELLAR, 0));
	}

	@Test
	public void deletedFloorsAreIdentified()
	{
		final OverlayFloor dragonFloor = new OverlayFloor(
			"Dragon Nest", UndergroundZone.DRAGON_NEST, 0, 1289, 3134);
		assertTrue(DungeonPoiOverrides.isFloorDeleted(dragonFloor));

		final OverlayFloor normalFloor = new OverlayFloor(
			"Vault of War", UndergroundZone.STRONGHOLD_OF_SECURITY, 0, 3145, 3415, 1271);
		assertFalse(DungeonPoiOverrides.isFloorDeleted(normalFloor));
	}
}
