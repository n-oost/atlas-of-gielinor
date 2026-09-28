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
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.runelite.api.coords.WorldPoint;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

/**
 * Name grouping is what makes browse mode and monster search usable — a flat list of 86 near-identical
 * "Cooking range" rows is not a menu.
 */
public class MapFinderGroupingTest
{
	private static final WorldPoint ORIGIN = new WorldPoint(0, 0, 0);
	private static final WorldPoint VARROCK_SQUARE = new WorldPoint(3213, 3424, 0);

	private PoiIndex poiIndex;
	private MonsterIndex monsterIndex;
	private MapFinder finder;

	@Before
	public void setUp()
	{
		poiIndex = new PoiIndex();
		poiIndex.load(new File("nonexistent_dir"));
		monsterIndex = new MonsterIndex(new Gson());
		monsterIndex.load();
		finder = new MapFinder(poiIndex, monsterIndex);
	}

	@Test
	public void monsterWithManyZonesYieldsOneRowWithChildrenNearestFirst()
	{
		final WorldPoint wp = new WorldPoint(0, 0, 0);
		final List<MapFinder.Result> candidates = Arrays.asList(
			new MapFinder.Result("Wilderness", "Guard", wp, 30, 0),
			new MapFinder.Result("Varrock", "Guard", new WorldPoint(10, 0, 0), 10, 0),
			new MapFinder.Result("Lumbridge", "Guard", new WorldPoint(20, 0, 0), 20, 0),
			new MapFinder.Result("Edgeville", "Guard", new WorldPoint(40, 0, 0), 40, 0)
		);

		final List<MapFinder.Result> ranked = MapFinder.rank(candidates, 10);
		assertEquals(1, ranked.size());

		final MapFinder.Result guard = ranked.get(0);
		assertEquals("Guard", guard.getName());
		assertTrue(guard.hasChildren());
		assertEquals(4, guard.getChildren().size());
		assertEquals("Varrock", guard.getChildren().get(0).getName());
		assertEquals("Lumbridge", guard.getChildren().get(1).getName());
		assertEquals("Wilderness", guard.getChildren().get(2).getName());
		assertEquals("Edgeville", guard.getChildren().get(3).getName());
	}

	@Test
	public void singleChildGroupHasNoAffordance()
	{
		final WorldPoint wp = new WorldPoint(0, 0, 0);
		final List<MapFinder.Result> candidates = Arrays.asList(
			new MapFinder.Result("Only place", "Goblin", wp, 5, 0)
		);

		final List<MapFinder.Result> ranked = MapFinder.rank(candidates, 10);
		assertEquals(1, ranked.size());
		assertFalse(ranked.get(0).hasChildren());
		assertTrue(ranked.get(0).getChildren().isEmpty());
	}

	@Test
	public void duplicatePoiNamesCollapseToOneRowWithChildren()
	{
		final List<MapFinder.Result> leaves = new ArrayList<>();
		for (int i = 0; i < 11; i++)
		{
			leaves.add(new MapFinder.Result("Cooking range", "Cooking range",
				new WorldPoint(3200 + i, 3400, 0), i, 0));
		}

		final List<MapFinder.Result> grouped = MapFinder.groupByName(leaves, Integer.MAX_VALUE);
		assertEquals(1, grouped.size());
		assertEquals("Cooking range", grouped.get(0).getName());
		assertTrue(grouped.get(0).hasChildren());
		assertEquals(11, grouped.get(0).getChildren().size());
	}

	@Test
	public void varrockBrowseCollapsesDuplicatePoiNames()
	{
		final List<MapFinder.Result> contents = finder.getRegionContents(MapRegion.VARROCK, VARROCK_SQUARE);
		assertFalse(contents.isEmpty());

		int yewTrees = 0;
		for (MapFinder.Result row : contents)
		{
			if ("Yew tree (Level 60)".equals(row.getName()))
			{
				yewTrees++;
				assertTrue("duplicate yew trees should group", row.hasChildren());
				assertTrue(row.getChildren().size() > 1);
			}
		}
		assertEquals("expected exactly one grouped Yew tree row", 1, yewTrees);
	}

	@Test
	public void guardSearchYieldsOneMonsterRowNotLocationJoinedNames()
	{
		finder.updateQuery("guard", VARROCK_SQUARE);
		assertTrue(finder.getMatchCount() > 0);

		int guardRows = 0;
		for (MapFinder.Result row : finder.getResults())
		{
			if ("Guard".equals(row.getGroupKey()))
			{
				guardRows++;
				assertEquals("Guard", row.getName());
				assertFalse("monster row name must not embed the location",
					row.getName().contains("—"));
				if (row.hasChildren())
				{
					for (MapFinder.Result child : row.getChildren())
					{
						assertFalse(child.getName().contains("Guard"));
					}
				}
			}
		}
		assertTrue("expected at least one Guard group", guardRows >= 1);
	}

	@Test
	public void getRegionsListsCitiesNearestFirstFromPlayer()
	{
		final WorldPoint lumbridge = new WorldPoint(3222, 3218, 0);
		final List<MapRegion> regions = finder.getRegions(lumbridge);

		assertFalse(regions.isEmpty());
		assertFalse(regions.stream().anyMatch(MapRegion::isFallback));

		// Lumbridge should be first — closest to the player standing there.
		assertEquals(MapRegion.LUMBRIDGE, regions.get(0));

		// List is globally sorted by distance, not grouped by kingdom.
		int prevDist = -1;
		for (MapRegion region : regions)
		{
			final int dx = region.getCenterX() - lumbridge.getX();
			final int dy = region.getCenterY() - lumbridge.getY();
			final int dist = (int) Math.sqrt(dx * dx + dy * dy);
			assertTrue("regions must be nearest-first", dist >= prevDist);
			prevDist = dist;
		}
	}

	@Test
	public void rankTruncatesGroupsNotLeaves()
	{
		final WorldPoint wp = new WorldPoint(0, 0, 0);
		final List<MapFinder.Result> candidates = Arrays.asList(
			new MapFinder.Result("A1", "Alpha", wp, 1, 0),
			new MapFinder.Result("A2", "Alpha", wp, 2, 0),
			new MapFinder.Result("B", "Beta", wp, 3, 0),
			new MapFinder.Result("C", "Gamma", wp, 4, 0)
		);

		final List<MapFinder.Result> ranked = MapFinder.rank(candidates, 2);
		assertEquals(2, ranked.size());
		assertEquals("Alpha", ranked.get(0).getName());
		assertEquals("B", ranked.get(1).getName());
	}

	@Test
	public void monsterResultsShowMonsterAsTitleAndLocationAsDetail()
	{
		finder.updateQuery("guard", VARROCK_SQUARE);
		final MapFinder.Result guard = finder.getResults().stream()
			.filter(result -> result.getKind() == MapFinder.Result.Kind.MONSTER)
			.findFirst().orElse(null);
		assertTrue("expected a typed monster result", guard != null);
		assertEquals("Guard", guard.getName());
		assertTrue("result should identify its location", guard.getDetail() != null && !guard.getDetail().isEmpty());
	}

	@Test
	public void groupedCountsSeparateRowsFromPhysicalLocations()
	{
		finder.updateQuery("bank", VARROCK_SQUARE);

		assertTrue(finder.getLocationMatchCount() >= finder.getMatchCount());
		assertTrue("bank locations should collapse into fewer displayed groups",
			finder.getLocationMatchCount() > finder.getMatchCount());
	}

	@Test
	public void regionContentsAreCachedUntilPlayerOrDataChanges()
	{
		final List<MapFinder.Result> first = finder.getRegionContents(MapRegion.VARROCK, VARROCK_SQUARE);
		assertSame(first, finder.getRegionContents(MapRegion.VARROCK, VARROCK_SQUARE));

		assertNotSame(first, finder.getRegionContents(MapRegion.VARROCK,
			new WorldPoint(VARROCK_SQUARE.getX() + 1, VARROCK_SQUARE.getY(), 0)));

		poiIndex.load(new File("nonexistent_dir"));
		assertNotSame(first, finder.getRegionContents(MapRegion.VARROCK, VARROCK_SQUARE));
	}

	@Test
	public void browseWaitsForCompleteSourceSnapshots()
	{
		final MapFinder loadingFinder = new MapFinder(new PoiIndex(), new MonsterIndex(new Gson()));
		assertTrue(loadingFinder.isBrowseLoading());
		assertTrue(loadingFinder.getRegionContents(MapRegion.VARROCK, VARROCK_SQUARE).isEmpty());
	}

	@Test
	public void refreshReordersDistancesAfterPlayerMovement()
	{
		finder.updateQuery("King Black Dragon", ORIGIN);
		final WorldPoint selectedPoint = finder.selectedResult().getPoint();

		finder.refresh(selectedPoint);

		assertEquals(0, finder.selectedResult().getDistanceTiles());
	}
}
