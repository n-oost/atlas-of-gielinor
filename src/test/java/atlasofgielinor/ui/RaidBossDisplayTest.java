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
package atlasofgielinor.ui;

import net.runelite.api.coords.WorldPoint;
import org.junit.Test;

import atlasofgielinor.data.BossLocationData;
import atlasofgielinor.data.dungeons.UndergroundZone;
import atlasofgielinor.ui.markers.RaidBossDisplay;

import static org.junit.Assert.*;

public class RaidBossDisplayTest
{
	@org.junit.BeforeClass
	public static void loadMapData()
	{
		atlasofgielinor.map.MapCatalogLoader.load();
		org.junit.Assert.assertTrue("Map catalogs must load before testing", atlasofgielinor.map.MapCatalogLoader.isReady());
	}

	@Test
	public void tombsOfAmascutPointsToNecropolisPyramid()
	{
		final WorldPoint surfacePoint = UndergroundZone.TOMBS_OF_AMASCUT.getSurfacePoint();
		assertEquals(3356, surfacePoint.getX());
		assertEquals(2712, surfacePoint.getY());
		assertEquals(6418, UndergroundZone.TOMBS_OF_AMASCUT.getOffsetY());
	}

	@Test
	public void identifiesRaidBossesAccurately()
	{
		assertTrue(RaidBossDisplay.isRaidBoss(BossLocationData.AKKHA));
		assertTrue(RaidBossDisplay.isRaidBoss(BossLocationData.BA_BA));
		assertTrue(RaidBossDisplay.isRaidBoss(BossLocationData.KEPHRI));
		assertTrue(RaidBossDisplay.isRaidBoss(BossLocationData.ZEBAK));
		assertTrue(RaidBossDisplay.isRaidBoss(BossLocationData.TUMEKEN_S_WARDEN));
		assertTrue(RaidBossDisplay.isRaidBoss(BossLocationData.ELIDINIS_WARDEN));

		assertTrue(RaidBossDisplay.isRaidBoss(BossLocationData.GREAT_OLM));
		assertTrue(RaidBossDisplay.isRaidBoss(BossLocationData.TEKTON));
		assertTrue(RaidBossDisplay.isRaidBoss(BossLocationData.VERZIK_VITUR));
		assertTrue(RaidBossDisplay.isRaidBoss(BossLocationData.THE_MAIDEN_OF_SUGADINTI));

		assertFalse(RaidBossDisplay.isRaidBoss(BossLocationData.VORKATH));
		assertFalse(RaidBossDisplay.isRaidBoss(BossLocationData.ZULRAH));
		assertFalse(RaidBossDisplay.isRaidBoss(BossLocationData.CORPOREAL_BEAST));
		assertFalse(RaidBossDisplay.isRaidBoss(BossLocationData.CERBERUS));
	}
}
