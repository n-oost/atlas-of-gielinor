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

import bettermap.map.MonsterIndex;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

public class MonsterIndexTest
{
	private MonsterIndex monsterIndex;

	@Before
	public void setUp()
	{
		monsterIndex = new MonsterIndex(new Gson());
		monsterIndex.load();
	}

	@Test
	public void monsterIndexLoadsZones()
	{
		assertTrue(monsterIndex.isLoaded());
		assertTrue(monsterIndex.size() > 1500);
	}

	@Test
	public void spatialNearestFindsMonsterNearLocation()
	{
		final MonsterIndex.Zone first = monsterIndex.getZones().get(0);
		assertNotNull(first);

		final MonsterIndex.Zone nearest = monsterIndex.nearest(first.getPlane(), first.getX(), first.getY(), 10);
		assertNotNull(nearest);
		assertEquals(first.getMonster(), nearest.getMonster());
	}

	@Test
	public void spatialNearestReturnsNullWhenFarAway()
	{
		final MonsterIndex.Zone nearest = monsterIndex.nearest(0, 0, 0, 5);
		assertNull(nearest);
	}

	@Test
	public void searchByNameMatchesMonsterName()
	{
		final java.util.List<MonsterIndex.Zone> results = monsterIndex.searchByName("abyssal demon", 100);
		org.junit.Assert.assertFalse(results.isEmpty());
		boolean foundExact = false;
		for (MonsterIndex.Zone z : results)
		{
			if ("Abyssal demon".equalsIgnoreCase(z.getMonster()))
			{
				foundExact = true;
				break;
			}
		}
		assertTrue(foundExact);
	}

	@Test
	public void searchByNameMatchesLocationName()
	{
		final java.util.List<MonsterIndex.Zone> results = monsterIndex.searchByName("slayer tower", 100);
		org.junit.Assert.assertFalse(results.isEmpty());
		boolean foundLocation = false;
		for (MonsterIndex.Zone z : results)
		{
			if (z.getLocationName() != null && z.getLocationName().toLowerCase(java.util.Locale.ROOT).contains("slayer tower"))
			{
				foundLocation = true;
				break;
			}
		}
		assertTrue(foundLocation);
	}

	@Test
	public void searchByNameRespectsCap()
	{
		final java.util.List<MonsterIndex.Zone> results = monsterIndex.searchByName("demon", 3);
		assertEquals(3, results.size());
	}

	@Test
	public void searchByNameEmptyForNullOrEmptyQueryOrInvalidCap()
	{
		assertTrue(monsterIndex.searchByName(null, 10).isEmpty());
		assertTrue(monsterIndex.searchByName("", 10).isEmpty());
		assertTrue(monsterIndex.searchByName("demon", 0).isEmpty());
		assertTrue(monsterIndex.searchByName("demon", -1).isEmpty());
	}

	@Test
	public void searchByNameEmptyForUnloadedIndex()
	{
		final MonsterIndex unloaded = new MonsterIndex(new Gson());
		assertTrue(unloaded.searchByName("abyssal demon", 10).isEmpty());
	}

	@Test
	public void taverleyDungeonSpawnsAreNotIndexedOnTheOverworld()
	{
		MonsterIndex.Zone dungeon = null;
		for (MonsterIndex.Zone zone : monsterIndex.getZones())
		{
			if (zone.getLocationName() != null
				&& zone.getLocationName().toLowerCase(java.util.Locale.ROOT).contains("taverley dungeon")
				&& zone.getY() > 9000)
			{
				dungeon = zone;
				break;
			}
		}
		assertNotNull("Taverley Dungeon must have at least one underground spawn zone", dungeon);
		assertTrue("Dungeon spawn Y must sit in the underground band, not the town", dungeon.getY() > 6400);

		final java.util.concurrent.atomic.AtomicBoolean onTown = new java.util.concurrent.atomic.AtomicBoolean(false);
		monsterIndex.forEachInArea(0, 2800, 2950, 3300, 3500, zone ->
		{
			if (zone.getLocationName() != null
				&& zone.getLocationName().toLowerCase(java.util.Locale.ROOT).contains("taverley dungeon"))
			{
				onTown.set(true);
			}
		});
		assertFalse("Taverley Dungeon spawns must not be indexed on the overworld town tiles", onTown.get());
	}

	@Test
	public void aberrantSpectreExposesSimplifiedCombatStats()
	{
		MonsterIndex.Zone spectre = null;
		for (MonsterIndex.Zone zone : monsterIndex.searchByName("aberrant spectre", 20))
		{
			if ("Aberrant spectre".equalsIgnoreCase(zone.getMonster()))
			{
				spectre = zone;
				break;
			}
		}
		assertNotNull(spectre);
		assertNotNull(spectre.getMaxHit());
		assertTrue(spectre.getMaxHit() > 0);
		assertNotNull(spectre.getAttackType());
		assertTrue(spectre.getAttackType().toLowerCase().contains("magic"));
		assertNotNull(spectre.getMagicLevel());
		assertTrue(spectre.getMagicLevel() > 0);
		assertNotNull(spectre.getDefenceMagic());

		final java.util.List<String> lines = spectre.combatSummaryLines();
		assertFalse(lines.isEmpty());
		final String joined = String.join("\n", lines);
		assertTrue(joined, joined.contains("Max hit:"));
		assertTrue(joined, joined.contains("Mage lvl:"));
	}
}
