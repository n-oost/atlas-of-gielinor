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

import bettermap.map.MapFinder;
import bettermap.map.MonsterIndex;
import bettermap.map.PoiIndex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.runelite.api.coords.WorldPoint;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class MapFinderTest
{
	@Test
	public void rankOrdersNearestFirstAndNegativeDistancesLast()
	{
		final WorldPoint wp = new WorldPoint(0, 0, 0);
		final List<MapFinder.Result> candidates = Arrays.asList(
			new MapFinder.Result("Far", wp, 100),
			new MapFinder.Result("Unknown", wp, -1),
			new MapFinder.Result("Near", wp, 5),
			new MapFinder.Result("Mid", wp, 50)
		);

		final List<MapFinder.Result> ranked = MapFinder.rank(candidates, 10);
		assertEquals(4, ranked.size());
		assertEquals("Near", ranked.get(0).getName());
		assertEquals("Mid", ranked.get(1).getName());
		assertEquals("Far", ranked.get(2).getName());
		assertEquals("Unknown", ranked.get(3).getName());
	}

	@Test
	public void rankGroupsDuplicateNamesWhileAllowingOtherNames()
	{
		final WorldPoint wp = new WorldPoint(0, 0, 0);
		final List<MapFinder.Result> candidates = new ArrayList<>();
		for (int i = 0; i < 8; i++)
		{
			candidates.add(new MapFinder.Result("Bank", wp, i + 1));
		}
		candidates.add(new MapFinder.Result("Shop", wp, 10));
		candidates.add(new MapFinder.Result("Shop", wp, 20));
		candidates.add(new MapFinder.Result("Shop", wp, 30));

		final List<MapFinder.Result> ranked = MapFinder.rank(candidates, 10);
		assertEquals(2, ranked.size());
		assertEquals("Bank", ranked.get(0).getName());
		assertEquals(1, ranked.get(0).getDistanceTiles());
		assertEquals(8, ranked.get(0).getChildren().size());
		assertEquals("Shop", ranked.get(1).getName());
		assertEquals(10, ranked.get(1).getDistanceTiles());
		assertEquals(3, ranked.get(1).getChildren().size());
	}

	@Test
	public void rankTruncatesToMaxResults()
	{
		final WorldPoint wp = new WorldPoint(0, 0, 0);
		final List<MapFinder.Result> candidates = new ArrayList<>();
		for (int i = 0; i < 20; i++)
		{
			candidates.add(new MapFinder.Result("Loc " + i, wp, i));
		}

		final List<MapFinder.Result> ranked = MapFinder.rank(candidates, MapFinder.MAX_RESULTS);
		assertEquals(MapFinder.MAX_RESULTS, ranked.size());
		for (int i = 0; i < MapFinder.MAX_RESULTS; i++)
		{
			assertEquals("Loc " + i, ranked.get(i).getName());
		}
	}

	@Test
	public void activationUsesFirstRankedChildOfAGroup()
	{
		final MapFinder.Result exactFar = new MapFinder.Result("Exact", "Group",
			new WorldPoint(100, 0, 0), 100, 0);
		final MapFinder.Result partialNear = new MapFinder.Result("Partial", "Group",
			new WorldPoint(1, 0, 0), 1, 2);
		final MapFinder.Result group = MapFinder.rank(
			Arrays.asList(partialNear, exactFar), 1).get(0);

		assertEquals(exactFar.getPoint(), MapFinder.activationTarget(group).getPoint());
	}

	@Test
	public void queryMutatorsUpdateAndCapState()
	{
		final MapFinder finder = new MapFinder(new PoiIndex(), new MonsterIndex(new Gson()));
		assertEquals("", finder.getQuery());
		assertTrue(finder.getResults().isEmpty());
		assertEquals(0, finder.getMatchCount());

		finder.updateQuery("a", null);
		assertEquals("a", finder.getQuery());

		finder.updateQuery("", null);
		assertEquals("", finder.getQuery());
		assertTrue(finder.getResults().isEmpty());
		assertEquals(0, finder.getMatchCount());

		final String longString = "1234567890123456789012345678901234567890EXTRA_TEXT";
		finder.updateQuery(longString, null);
		assertEquals(MapFinder.MAX_QUERY_LENGTH, finder.getQuery().length());
		assertEquals(longString.substring(0, MapFinder.MAX_QUERY_LENGTH), finder.getQuery());

		finder.clearQuery();
		assertEquals("", finder.getQuery());
		assertTrue(finder.getResults().isEmpty());
		assertEquals(0, finder.getMatchCount());
	}



	@Test(expected = UnsupportedOperationException.class)
	public void resultsListIsImmutable()
	{
		final MapFinder finder = new MapFinder(new PoiIndex(), new MonsterIndex(new Gson()));
		finder.getResults().add(new MapFinder.Result("test", new WorldPoint(0, 0, 0), 0));
	}

	@Test
	public void matchTierAllOutcomes()
	{
		assertEquals(0, MapFinder.matchTier("Abyssal demon", "Slayer Tower", "abyssal demon"));
		assertEquals(1, MapFinder.matchTier("Abyssal demon", "Slayer Tower", "abyssal"));
		assertEquals(2, MapFinder.matchTier("Abyssal demon", "Slayer Tower", "demon"));
		assertEquals(3, MapFinder.matchTier("Abyssal demon", "Slayer Tower", "slayer tower"));
		assertEquals(-1, MapFinder.matchTier("Abyssal demon", "Slayer Tower", "varrock"));

		assertEquals(-1, MapFinder.matchTier("Abyssal demon", null, "slayer"));
		assertEquals(-1, MapFinder.matchTier(null, null, "test"));
		assertEquals(-1, MapFinder.matchTier("Abyssal demon", "Slayer Tower", null));
		assertEquals(-1, MapFinder.matchTier("Abyssal demon", "Slayer Tower", ""));
		assertEquals(0, MapFinder.matchTier("Varrock", null, "varrock"));
	}

	@Test
	public void rankGroupsOnGroupKeyWithLocationNamedChildren()
	{
		final WorldPoint wp = new WorldPoint(0, 0, 0);
		final List<MapFinder.Result> candidates = Arrays.asList(
			new MapFinder.Result("Slayer Tower", "Abyssal demon", wp, 10, 0),
			new MapFinder.Result("Catacombs", "Abyssal demon", wp, 20, 0),
			new MapFinder.Result("Wilderness", "Abyssal demon", wp, 30, 0),
			new MapFinder.Result("Lumbridge", "Goblin", wp, 15, 0)
		);

		final List<MapFinder.Result> ranked = MapFinder.rank(candidates, 10);
		assertEquals(2, ranked.size());
		assertEquals("Abyssal demon", ranked.get(0).getName());
		assertEquals(3, ranked.get(0).getChildren().size());
		assertEquals("Slayer Tower", ranked.get(0).getChildren().get(0).getName());
		assertEquals("Goblin", ranked.get(1).getGroupKey());
		assertEquals("Lumbridge", ranked.get(1).getName());
	}

	@Test
	public void rankLowerTierBeatsNearerDistance()
	{
		final WorldPoint wp = new WorldPoint(0, 0, 0);
		final List<MapFinder.Result> candidates = Arrays.asList(
			new MapFinder.Result("Guard — Varrock", "Guard", wp, 5, 3),
			new MapFinder.Result("Varrock", "Varrock", wp, 100, 0),
			new MapFinder.Result("Varrock Castle", "Varrock Castle", wp, 50, 1)
		);

		final List<MapFinder.Result> ranked = MapFinder.rank(candidates, 10);
		assertEquals(3, ranked.size());
		assertEquals("Varrock", ranked.get(0).getName());
		assertEquals("Varrock Castle", ranked.get(1).getName());
		assertEquals("Guard — Varrock", ranked.get(2).getName());
	}
}
