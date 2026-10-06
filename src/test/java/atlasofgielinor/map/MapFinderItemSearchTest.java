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

import com.google.gson.Gson;

import atlasofgielinor.AtlasOfGielinorConfig;
import atlasofgielinor.map.catalog.GroundItemIndex;
import atlasofgielinor.map.MapFinder;
import atlasofgielinor.map.catalog.MonsterIndex;
import atlasofgielinor.map.catalog.PoiIndex;
import atlasofgielinor.map.catalog.ShopIndex;

import java.util.List;
import net.runelite.api.coords.WorldPoint;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

public class MapFinderItemSearchTest
{
	/** "Bread" is stocked by several baker's stalls and spawns on many kitchen tiles. */
	private static final String TERM = "bread";
	private static final WorldPoint FROM = new WorldPoint(3210, 3424, 0);

	private ShopIndex shopIndex;
	private GroundItemIndex groundItemIndex;
	private PoiIndex poiIndex;
	private MonsterIndex monsterIndex;

	@Before
	public void setUp()
	{
		poiIndex = new PoiIndex();
		monsterIndex = new MonsterIndex(new Gson());
		monsterIndex.load();
		shopIndex = new ShopIndex(new Gson());
		shopIndex.load();
		groundItemIndex = new GroundItemIndex(new Gson());
		groundItemIndex.load();
	}

	private MapFinder finder(boolean itemSearchEnabled)
	{
		final AtlasOfGielinorConfig config = new AtlasOfGielinorConfig()
		{
			@Override
			public boolean finderSearchItems()
			{
				return itemSearchEnabled;
			}
		};
		return new MapFinder(poiIndex, monsterIndex, shopIndex, groundItemIndex, config);
	}

	private static boolean hasKind(List<MapFinder.Result> results, MapFinder.Result.Kind kind)
	{
		return results.stream().anyMatch(r -> r.getKind() == kind);
	}

	@Test
	public void surfacesNearestShopAndGroundSpawnsForAnItem()
	{
		final MapFinder finder = finder(true);
		finder.updateQuery(TERM, FROM);
		final List<MapFinder.Result> results = finder.getResults();

		assertTrue("expected a shop result for " + TERM, hasKind(results, MapFinder.Result.Kind.SHOP));
		assertTrue("expected a ground-item result for " + TERM, hasKind(results, MapFinder.Result.Kind.GROUND_ITEM));
	}

	@Test
	public void groupedItemResultsAreOrderedByTierThenDistance()
	{
		final MapFinder finder = finder(true);
		finder.updateQuery(TERM, FROM);

		for (MapFinder.Result result : finder.getResults())
		{
			if (result.getKind() != MapFinder.Result.Kind.SHOP
				&& result.getKind() != MapFinder.Result.Kind.GROUND_ITEM)
			{
				continue;
			}
			final List<MapFinder.Result> children = result.getChildren();
			if (children.isEmpty())
			{
				continue;
			}
			// Parent row mirrors the top child (best tier, nearest within it).
			assertTrue(result.getDistanceTiles() == children.get(0).getDistanceTiles());
			for (int i = 1; i < children.size(); i++)
			{
				final MapFinder.Result prev = children.get(i - 1);
				final MapFinder.Result cur = children.get(i);
				final boolean ordered = prev.getTier() < cur.getTier()
					|| (prev.getTier() == cur.getTier()
						&& prev.getDistanceTiles() <= cur.getDistanceTiles());
				assertTrue("children out of tier/distance order", ordered);
			}
		}
	}

	@Test
	public void toggleOffDropsShopAndGroundResults()
	{
		final MapFinder finder = finder(false);
		finder.updateQuery(TERM, FROM);
		final List<MapFinder.Result> results = finder.getResults();

		assertFalse(hasKind(results, MapFinder.Result.Kind.SHOP));
		assertFalse(hasKind(results, MapFinder.Result.Kind.GROUND_ITEM));
	}

	@Test
	public void shopResultsCarryMatchedItemStockAndPriceDetail()
	{
		final MapFinder finder = finder(true);
		finder.updateQuery(TERM, FROM);

		MapFinder.Result shopGroup = null;
		for (MapFinder.Result result : finder.getResults())
		{
			if (result.getKind() == MapFinder.Result.Kind.SHOP)
			{
				shopGroup = result;
				break;
			}
		}
		assertTrue("expected a shop group for " + TERM, shopGroup != null);
		assertTrue(shopGroup.getDetail() != null && shopGroup.getDetail().toLowerCase().contains(TERM));
		assertTrue(shopGroup.getDetail().contains("stock"));
		assertTrue("expected sell gp on bread shop detail: " + shopGroup.getDetail(),
			shopGroup.getSellGp() > 0 || shopGroup.getDetail().contains("gp"));
	}

	@Test
	public void fullStockMatchFindsItemsOutsideNotableHints()
	{
		// Bronze pickaxe is ordinary stock at Bob's, not necessarily a notable entry.
		final MapFinder finder = finder(true);
		finder.updateQuery("bronze pickaxe", FROM);

		boolean found = false;
		for (MapFinder.Result result : finder.getResults())
		{
			if (result.getKind() != MapFinder.Result.Kind.SHOP)
			{
				continue;
			}
			found = true;
			assertTrue(result.getDetail() != null && result.getDetail().toLowerCase().contains("bronze pickaxe"));
			assertTrue(result.getSellGp() > 0);
		}
		assertTrue("expected shops stocking bronze pickaxe", found);
	}

	@Test
	public void mineralSearchReturnsMiningSitesAsMineralResults()
	{
		final MapFinder finder = finder(true);
		finder.updateQuery("mithril", FROM);

		assertTrue("expected mithril mining-site results", finder.getResults().stream()
			.anyMatch(result -> result.getKind() == MapFinder.Result.Kind.MINERAL));
		assertFalse("mining sites should not appear as generic place-name matches", finder.getResults().stream()
			.anyMatch(result -> result.getKind() == MapFinder.Result.Kind.PLACE
				&& result.getName().toLowerCase().contains("mining site")));
	}

	@Test
	public void shopGroupNamesTheMatchedStockItem()
	{
		final MapFinder finder = finder(true);
		finder.updateQuery("ancient", FROM);

		assertTrue("shop results should expose matched items in their group title",
			finder.getResults().stream().filter(result -> result.getKind() == MapFinder.Result.Kind.SHOP)
				.allMatch(result -> result.getGroupKey().startsWith("Shop item: ")));
	}
}
