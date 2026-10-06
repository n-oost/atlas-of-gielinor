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

import com.google.gson.Gson;

import atlasofgielinor.map.catalog.PoiCategory;
import atlasofgielinor.map.catalog.PoiDetails;
import atlasofgielinor.map.catalog.ShopIndex;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;

public class ShopIndexTest
{
	private static final ShopIndex INDEX = new ShopIndex(new Gson());

	@BeforeClass
	public static void loadOnce()
	{
		INDEX.load();
	}

	@Test
	public void datasetLoadsWithEveryShopNamedAndPlaced()
	{
		assertTrue("expected >= 400 shops, got " + INDEX.size(), INDEX.size() >= 400);
		for (ShopIndex.Shop shop : INDEX.all())
		{
			assertNotNull(shop.getName());
			assertFalse(shop.getName().trim().isEmpty());
			assertTrue(shop.getX() > 0);
			assertTrue(shop.getY() > 0);
			assertTrue(shop.getPlane() >= 0 && shop.getPlane() <= 3);
		}
	}

	@Test
	public void bobsBrilliantAxesShowsOwnerSpecialAndBulkSource()
	{
		final ShopIndex.Shop bob = INDEX.nearest(3231, 3203, 0, 10);
		assertNotNull(bob);
		assertTrue(bob.getName().contains("Bob"));

		final PoiDetails.Detail detail = bob.toDetail(INDEX.othersNear(bob, 6, 2));
		assertTrue(detail.getTitle().contains("Bob"));
		final String joined = String.join("\n", detail.getLines());
		assertTrue(joined, joined.contains("Owner: Bob"));
		assertTrue(joined, joined.toLowerCase().contains("sells:") && joined.toLowerCase().contains("axe"));
		assertTrue(joined, joined.contains("Services:") && joined.toLowerCase().contains("repair"));
	}

	@Test
	public void culinaromancersChestSummarisesInsteadOfListingEveryItem()
	{
		ShopIndex.Shop chest = null;
		for (ShopIndex.Shop shop : INDEX.all())
		{
			if (shop.getName().toLowerCase().contains("culinaromancer"))
			{
				chest = shop;
				break;
			}
		}
		assertNotNull("Culinaromancer's Chest should be in the dataset", chest);

		final PoiDetails.Detail detail = chest.toDetail(null);
		boolean stockSummary = false;
		boolean notable = false;
		for (String line : detail.getLines())
		{
			assertTrue("line too long: " + line, line.length() <= 90);
			if (line.matches("Stock: \\d{3,} items"))
			{
				stockSummary = true;
			}
			if (line.startsWith("Notable:"))
			{
				notable = true;
				final String names = line.substring("Notable:".length()).trim();
				final int commas = names.isEmpty() ? 0 : names.split(",").length;
				assertTrue("notable should list at most 4 names: " + line, commas <= 4);
			}
		}
		assertTrue("expected Stock: NNN items", stockSummary);
		assertTrue("expected a Notable: line", notable);
	}

	@Test
	public void everyShopCardStaysWithinTheLineBudget()
	{
		for (ShopIndex.Shop shop : INDEX.all())
		{
			final PoiDetails.Detail detail = shop.toDetail(INDEX.othersNear(shop, 6, 2));
			assertTrue(shop.getName() + " has " + detail.getLines().size() + " lines",
				detail.getLines().size() <= 7);
			for (String line : detail.getLines())
			{
				assertFalse("blank line in " + shop.getName(), line.trim().isEmpty());
				assertTrue(shop.getName() + ": " + line, line.length() <= 90);
			}
		}
	}

	@Test
	public void everyShopIconKeyLandsInTheShopsCategory()
	{
		for (ShopIndex.Shop shop : INDEX.all())
		{
			final PoiCategory cat = PoiCategory.of(shop.getIcon());
			assertEquals(shop.getName() + " icon=" + shop.getIcon(), PoiCategory.SHOPS, cat);
		}
	}

	@Test
	public void nearestPrefersTheCloserOfTwoNeighbouringShops()
	{
		final ShopIndex.Shop a = INDEX.nearest(3231, 3203, 0, 10);
		assertNotNull(a);
		// A point nearer a should not resolve to a far shop when radius is tight
		final ShopIndex.Shop again = INDEX.nearest(a.getX(), a.getY(), a.getPlane(), 2);
		assertEquals(a.getName(), again.getName());
	}

	@Test
	public void nearestIgnoresShopsBeyondTheRadius()
	{
		assertNull(INDEX.nearest(0, 0, 0, 5));
	}

	@Test
	public void nearestDoesNotCrossPlanes()
	{
		final ShopIndex.Shop ground = INDEX.nearest(3231, 3203, 0, 10);
		assertNotNull(ground);
		assertNull(INDEX.nearest(ground.getX(), ground.getY(), (ground.getPlane() + 1) % 4, 2));
	}

	@Test
	public void othersNearDisclosesClusteredNeighbours()
	{
		final ShopIndex.Shop bob = INDEX.nearest(3231, 3203, 0, 10);
		assertNotNull(bob);
		final List<ShopIndex.Shop> near = INDEX.othersNear(bob, 40, 2);
		assertTrue(near.size() <= 2);
		for (ShopIndex.Shop other : near)
		{
			assertFalse(other.getName().equals(bob.getName()) && other.getX() == bob.getX());
		}
	}

	@Test
	public void varlamoreAndPrifddinasHaveShopCoverage()
	{
		int varlamore = 0;
		int prif = 0;
		for (ShopIndex.Shop shop : INDEX.all())
		{
			if (shop.getX() >= 1300 && shop.getX() <= 1800 && shop.getY() >= 2900 && shop.getY() <= 3300)
			{
				varlamore++;
			}
			if (shop.getX() >= 2130 && shop.getX() <= 2350 && shop.getY() >= 3250 && shop.getY() <= 3450)
			{
				prif++;
			}
		}
		assertTrue("Varlamore shops: " + varlamore, varlamore > 0);
		assertTrue("Prifddinas shops: " + prif, prif > 0);
	}

	@Test
	public void undergroundShopsResolve()
	{
		assertNotNull(INDEX.nearest(2450, 4470, 0, 30));
	}

	@Test
	public void approximateShopsSaySo()
	{
		for (ShopIndex.Shop shop : INDEX.all())
		{
			if (!shop.isApprox())
			{
				continue;
			}
			final String joined = String.join("\n", shop.toDetail(null).getLines());
			assertTrue(shop.getName(), joined.toLowerCase().contains("approximate"));
		}
	}

	@Test
	public void fullStockMatchReturnsItemWithStockAndSellPrice()
	{
		final ShopIndex.Shop bob = INDEX.nearest(3231, 3203, 0, 10);
		assertNotNull(bob);
		assertTrue(bob.getName().contains("Bob"));

		final ShopIndex.StockItem pickaxe = bob.stockItemMatch("bronze pickaxe");
		assertNotNull(pickaxe);
		assertEquals("Bronze pickaxe", pickaxe.getName());
		assertTrue(pickaxe.getStock() > 0);
		assertTrue(pickaxe.getSell() > 0);
		assertTrue(pickaxe.toDetailLine().contains("stock"));
		assertTrue(pickaxe.toDetailLine().contains("gp"));
	}

	@Test
	public void searchByNameFindsShopsFromFullStockNotJustNotable()
	{
		final List<ShopIndex.Shop> hits = INDEX.searchByName("bronze pickaxe", Integer.MAX_VALUE);
		assertFalse(hits.isEmpty());
		boolean bob = false;
		for (ShopIndex.Shop shop : hits)
		{
			assertNotNull(shop.stockItemMatch("bronze pickaxe"));
			if (shop.getName().contains("Bob"))
			{
				bob = true;
			}
		}
		assertTrue("Bob's Brilliant Axes should stock bronze pickaxe", bob);
	}
}
