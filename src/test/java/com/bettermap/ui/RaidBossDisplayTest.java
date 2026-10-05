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

import com.bettermap.ui.markers.RaidBossDisplay;

import com.bettermap.data.MonsterLocationData;
import com.bettermap.data.UndergroundZone;
import net.runelite.api.coords.WorldPoint;
import org.junit.Test;

import static org.junit.Assert.*;

public class RaidBossDisplayTest
{
	@org.junit.BeforeClass
	public static void loadMapData()
	{
		com.bettermap.map.MapData.load();
		org.junit.Assert.assertTrue("Map catalogs must load before testing", com.bettermap.map.MapData.isReady());
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
		assertTrue(RaidBossDisplay.isRaidBoss(MonsterLocationData.AKKHA));
		assertTrue(RaidBossDisplay.isRaidBoss(MonsterLocationData.BA_BA));
		assertTrue(RaidBossDisplay.isRaidBoss(MonsterLocationData.KEPHRI));
		assertTrue(RaidBossDisplay.isRaidBoss(MonsterLocationData.ZEBAK));
		assertTrue(RaidBossDisplay.isRaidBoss(MonsterLocationData.TUMEKEN_S_WARDEN));
		assertTrue(RaidBossDisplay.isRaidBoss(MonsterLocationData.ELIDINIS_WARDEN));

		assertTrue(RaidBossDisplay.isRaidBoss(MonsterLocationData.GREAT_OLM));
		assertTrue(RaidBossDisplay.isRaidBoss(MonsterLocationData.TEKTON));
		assertTrue(RaidBossDisplay.isRaidBoss(MonsterLocationData.VERZIK_VITUR));
		assertTrue(RaidBossDisplay.isRaidBoss(MonsterLocationData.THE_MAIDEN_OF_SUGADINTI));

		assertFalse(RaidBossDisplay.isRaidBoss(MonsterLocationData.VORKATH));
		assertFalse(RaidBossDisplay.isRaidBoss(MonsterLocationData.ZULRAH));
		assertFalse(RaidBossDisplay.isRaidBoss(MonsterLocationData.CORPOREAL_BEAST));
		assertFalse(RaidBossDisplay.isRaidBoss(MonsterLocationData.CERBERUS));
	}
}
