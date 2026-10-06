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
package atlasofgielinor.map.catalog;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import atlasofgielinor.map.catalog.MapRegion;

/**
 * The region table is hand-authored, so these are the guard rails that catch a mistyped extent.
 */
public class MapRegionTest
{
	@org.junit.BeforeClass
	public static void loadRegions()
	{
		MapRegion.load();
		assertTrue("Region catalog must load before existing checks", MapRegion.isLoaded());
	}

	/**
	 * The core guarantee: a city box must not be swallowed by a neighbour. If a city's own centre
	 * resolves to something else, its extents are wrong or a smaller box overlaps it.
	 *
	 * <p>Kingdom fallbacks are exempt — their label points sit inside their own cities by design
	 * (the "Asgarnia" label is drawn over Falador), so owning their own centre is not expected.
	 */
	@Test
	public void everyCityCentreResolvesToItself()
	{
		for (MapRegion region : MapRegion.values())
		{
			if (region.isFallback())
			{
				continue;
			}
			final MapRegion resolved = MapRegion.of(region.getCenterX(), region.getCenterY(), 0);
			assertEquals("centre of " + region.getDisplayName() + " resolved elsewhere",
				region, resolved);
		}
	}

	/** A fallback still has to be reachable, or it is dead weight in the table. */
	@Test
	public void everyKingdomFallbackOwnsSomeGroundOfItsOwn()
	{
		for (MapRegion region : MapRegion.values())
		{
			if (!region.isFallback())
			{
				continue;
			}

			boolean reachable = false;
			for (int x = region.getMinX(); x <= region.getMaxX() && !reachable; x += 16)
			{
				for (int y = region.getMinY(); y <= region.getMaxY() && !reachable; y += 16)
				{
					reachable = MapRegion.of(x, y, 0) == region;
				}
			}
			assertTrue(region.getDisplayName() + " is completely covered by other boxes", reachable);
		}
	}

	@Test
	public void knownLandmarksLandInTheExpectedRegion()
	{
		assertSame(MapRegion.VARROCK, MapRegion.of(3185, 3436, 0));              // Varrock west bank
		assertSame(MapRegion.LUMBRIDGE, MapRegion.of(3222, 3218, 0));            // Lumbridge castle
		assertSame(MapRegion.FALADOR, MapRegion.of(3013, 3355, 0));              // Falador east bank
		assertSame(MapRegion.EDGEVILLE, MapRegion.of(3094, 3491, 0));            // Edgeville bank
		assertSame(MapRegion.AL_KHARID, MapRegion.of(3270, 3167, 0));            // Al Kharid bank
		assertSame(MapRegion.CATHERBY, MapRegion.of(2809, 3441, 0));             // Catherby bank
		assertSame(MapRegion.SEERS_VILLAGE, MapRegion.of(2725, 3491, 0));        // Seers' bank
		assertSame(MapRegion.EAST_ARDOUGNE, MapRegion.of(2616, 3332, 0));        // Ardougne north bank
		assertSame(MapRegion.YANILLE, MapRegion.of(2613, 3093, 0));              // Yanille bank
		assertSame(MapRegion.PORT_PHASMATYS, MapRegion.of(3684, 3468, 0));       // Phasmatys bank
		assertSame(MapRegion.CANIFIS, MapRegion.of(3512, 3480, 0));              // Canifis bank
		assertSame(MapRegion.PRIFDDINAS, MapRegion.of(2196, 3327, 0));           // Prifddinas
		assertSame(MapRegion.HOSIDIUS, MapRegion.of(1750, 3595, 0));             // Hosidius
		assertSame(MapRegion.CIVITAS_ILLA_FORTIS, MapRegion.of(1700, 3120, 0));  // Fortis
	}

	/** Nesting is the whole reason for smallest-box-wins. */
	@Test
	public void smallestBoxWinsSoNestedRegionsResolveToTheSpecificName()
	{
		// Grand Exchange sits inside Varrock, which sits inside Misthalin.
		assertSame(MapRegion.GRAND_EXCHANGE, MapRegion.of(3168, 3477, 0));
		assertSame(MapRegion.VARROCK, MapRegion.of(3215, 3430, 0));

		final MapRegion ge = MapRegion.of(3168, 3477, 0);
		assertNotNull(ge);
		assertTrue("Grand Exchange should be smaller than Varrock",
			ge.area() < MapRegion.VARROCK.area());
		assertTrue("Varrock should be smaller than Misthalin",
			MapRegion.VARROCK.area() < MapRegion.MISTHALIN.area());
	}

	/** Somewhere inside a kingdom but in no city still gets a usable name. */
	@Test
	public void pointsOutsideEveryCityFallBackToTheKingdomBox()
	{
		// East of the Lum, north of Al Kharid: inside no city box, still recognisably Misthalin.
		final MapRegion resolved = MapRegion.of(3300, 3350, 0);
		assertNotNull(resolved);
		assertSame(MapRegion.MISTHALIN, resolved);
	}

	@Test
	public void deepOceanBelongsToNoRegion()
	{
		assertNull(MapRegion.of(1000, 5000, 0));
	}

	/**
	 * Dungeon coordinates do not share the surface's frame of reference, so classifying them with
	 * a surface box would produce a confidently wrong answer.
	 */
	@Test
	public void nonSurfacePlanesAreNotClassified()
	{
		assertNull(MapRegion.of(3211, 3450, 1));
		assertNull(MapRegion.of(3211, 3450, 2));
		assertNull(MapRegion.of(3211, 3450, 3));
	}

	@Test
	public void kingdomOrderListsEveryRegionExactlyOnce()
	{
		final List<MapRegion> ordered = MapRegion.inKingdomOrder();
		assertEquals(MapRegion.values().length, ordered.size());
		assertEquals(MapRegion.values().length, new HashSet<>(ordered).size());
	}

	@Test
	public void kingdomOrderKeepsEachKingdomContiguousSoTheMenuGroupsCleanly()
	{
		final Set<MapRegion.Kingdom> seen = new HashSet<>();
		MapRegion.Kingdom current = null;

		for (MapRegion region : MapRegion.inKingdomOrder())
		{
			if (region.getKingdom() != current)
			{
				assertTrue("kingdom " + region.getKingdom() + " appears in two blocks",
					seen.add(region.getKingdom()));
				current = region.getKingdom();
			}
		}
	}

	@Test
	public void displayNamesAreUniqueAndLookupRoundTrips()
	{
		final Set<String> names = new HashSet<>();
		for (MapRegion region : MapRegion.values())
		{
			assertTrue("duplicate display name: " + region.getDisplayName(),
				names.add(region.getDisplayName()));
			assertSame(region, MapRegion.byName(region.getDisplayName()));
		}
		assertNull(MapRegion.byName("Nowhere At All"));
		assertNull(MapRegion.byName(null));
	}

	@Test
	public void everyBoxHasRealExtent()
	{
		for (MapRegion region : MapRegion.values())
		{
			assertTrue(region.getDisplayName() + " has no width", region.getMaxX() > region.getMinX());
			assertTrue(region.getDisplayName() + " has no height", region.getMaxY() > region.getMinY());
		}
	}
}
