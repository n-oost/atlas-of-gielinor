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

import java.io.File;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

public class PoiIndexTest
{
	private PoiIndex poiIndex;

	@Before
	public void setUp()
	{
		poiIndex = new PoiIndex();
		// Load builtin POIs from PoiDetails
		poiIndex.load(new File("nonexistent_dir"));
	}

	@Test
	public void poiIndexLoadsCuratedPois()
	{
		assertTrue(poiIndex.isLoaded());
		assertTrue(poiIndex.size() > 1000);
	}

	@Test
	public void iconsArePreparedWithTheIndex()
	{
		assertNotNull(poiIndex.icon("quest_start"));
		assertNull(poiIndex.icon("missing_test_icon"));
		assertEquals(poiIndex.icon("quest_start"), poiIndex.icon("quest_start"));
	}

	@Test
	public void spatialNearestFindsCooksAssistantInLumbridge()
	{
		// Cook's Assistant is at (3208, 3214, plane 0)
		final PoiIndex.Poi found = poiIndex.nearest(3208, 3214, 0, 10);
		assertNotNull(found);
		assertEquals("Cook's Assistant", found.getName());
		assertEquals("quest_start", found.getKey());
		assertEquals(0, found.getPlane());
	}

	@Test
	public void dungeonSourcesCoalesceAtTheirOverworldEntrance()
	{
		final List<PoiIndex.Poi> taverley = poiIndex.inArea(0, 2883, 2884, 3397, 3397);
		assertEquals("The wiki and curated Taverley marker must not render as a one-tile double", 1, taverley.size());
		assertEquals(2884, taverley.get(0).getX());
		assertEquals(3397, taverley.get(0).getY());
	}

	@Test
	public void distinctDungeonEntrancesRemainSeparate()
	{
		final PoiIndex.Poi northBrimhaven = poiIndex.nearest(2744, 3154, 0, 0);
		final PoiIndex.Poi southBrimhaven = poiIndex.nearest(2760, 3062, 0, 0);
		assertNotNull(northBrimhaven);
		assertNotNull(southBrimhaven);
		assertEquals(2744, northBrimhaven.getX());
		assertEquals(2760, southBrimhaven.getX());
	}

	@Test
	public void curatedDungeonAliasIsSearchableWithoutDuplicatingItsMarker()
	{
		final List<PoiIndex.Poi> kruk = poiIndex.searchByName("kruk's dungeon", 10);
		assertEquals(1, kruk.size());
		assertEquals(2763, kruk.get(0).getX());
		assertEquals(2703, kruk.get(0).getY());

		final List<PoiIndex.Poi> entrance = poiIndex.inArea(0, 2763, 2763, 2703, 2703);
		assertEquals("the wiki icon remains the one rendered point", 1, entrance.size());
	}

	@Test
	public void modeledDungeonSearchUsesCanonicalZoneEntrance()
	{
		final List<PoiIndex.Poi> isle = poiIndex.searchByName("isle of souls dungeon", 10);
		final PoiIndex.Poi entrance = isle.stream()
			.filter(poi -> "Isle of Souls Dungeon".equals(poi.getName()))
			.findFirst().orElse(null);
		assertNotNull(entrance);
		assertEquals(1, isle.stream()
			.filter(poi -> "Isle of Souls Dungeon".equals(poi.getName()))
			.count());
		assertEquals(2309, entrance.getX());
		assertEquals(2919, entrance.getY());
	}

	@Test
	public void spatialNearestRespectsPlane()
	{
		// Searching on plane 1 should not match plane 0
		final PoiIndex.Poi found = poiIndex.nearest(3208, 3214, 1, 10);
		if (found != null)
		{
			assertEquals(1, found.getPlane());
		}
	}

	@Test
	public void spatialInAreaReturnsPoisInBoundingBox()
	{
		// Lumbridge castle region: (3200..3240, 3200..3240)
		final List<PoiIndex.Poi> pois = poiIndex.inArea(0, 3200, 3240, 3200, 3240);
		assertNotNull(pois);
		assertTrue(pois.size() > 0);
		for (PoiIndex.Poi poi : pois)
		{
			assertEquals(0, poi.getPlane());
			assertTrue(poi.getX() >= 3200 && poi.getX() <= 3240);
			assertTrue(poi.getY() >= 3200 && poi.getY() <= 3240);
		}
	}

	@Test
	public void nearestReturnsNullWhenFarAway()
	{
		final PoiIndex.Poi found = poiIndex.nearest(0, 0, 0, 5);
		assertNull(found);
	}
}
