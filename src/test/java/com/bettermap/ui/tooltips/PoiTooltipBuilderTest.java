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
package com.bettermap.ui.tooltips;

import com.bettermap.map.PoiDetails;
import com.bettermap.map.PoiIndex;
import java.awt.image.BufferedImage;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PoiTooltipBuilderTest
{
	private PoiTooltipBuilder builder;
	private BufferedImage dummyIcon;

	@Before
	public void setUp()
	{
		builder = new PoiTooltipBuilder();
		dummyIcon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
	}

	@Test
	public void compactTooltipHasZeroLinesForIconAndNameOnly()
	{
		final PoiDetails.Detail detail = new PoiDetails.Detail(
			"Fishing Spot (Shrimp)",
			"Skilling • Fishing",
			List.of("Method / Fish: Shrimp", "Tools: Net", "Check Fishing guide")
		);
		final PoiIndex.Poi poi = new PoiIndex.Poi(3000, 3000, 0, "fishing_spot", "Fishing spot");

		final TooltipCard compactCard = builder.buildPoiCard(detail, poi, dummyIcon, true);
		assertNotNull(compactCard);
		assertEquals("Shrimp & Anchovies", compactCard.getTitle());
		assertTrue("Compact tooltip card must have 0 extra lines so only <ICON> <NAME> is rendered",
			compactCard.getLines().isEmpty());
		assertFalse(compactCard.isPreserveCompactLines());
	}

	@Test
	public void expandedTooltipPreservesCategoryBadgeAndDetailLines()
	{
		final PoiDetails.Detail detail = new PoiDetails.Detail(
			"Fishing Spot (Shrimp)",
			"Skilling • Fishing",
			List.of("Method / Fish: Shrimp", "Tools: Net", "Check Fishing guide")
		);
		final PoiIndex.Poi poi = new PoiIndex.Poi(3000, 3000, 0, "fishing_spot", "Fishing spot");

		final TooltipCard expandedCard = builder.buildPoiCard(detail, poi, dummyIcon, false);
		assertNotNull(expandedCard);
		assertEquals("Fishing Spot (Shrimp)", expandedCard.getTitle());
		assertFalse(expandedCard.getLines().isEmpty());
		assertEquals("[Skilling • Fishing]", expandedCard.getLines().get(0));
		assertEquals("Method / Fish: Shrimp", expandedCard.getLines().get(1));
	}

	@Test
	public void fishingSpotsResolveFishInCompactForm()
	{
		final PoiIndex.Poi poi = new PoiIndex.Poi(3000, 3000, 0, "fishing_spot", "Fishing spot");

		// Shrimp
		PoiDetails.Detail d1 = new PoiDetails.Detail("Fishing Spot (Shrimp)", "Skilling • Fishing", List.of("Method / Fish: Shrimp"));
		assertEquals("Shrimp & Anchovies", builder.buildPoiCard(d1, poi, dummyIcon, true).getTitle());

		// Shark
		PoiDetails.Detail d2 = new PoiDetails.Detail("Fishing Spot (Shark)", "Skilling • Fishing", List.of("Method / Fish: Shark"));
		assertEquals("Shark", builder.buildPoiCard(d2, poi, dummyIcon, true).getTitle());

		// Lobster
		PoiDetails.Detail d3 = new PoiDetails.Detail("Fishing Spot (Lobster)", "Skilling • Fishing", List.of("Method / Fish: Lobster"));
		assertEquals("Lobster & Swordfish", builder.buildPoiCard(d3, poi, dummyIcon, true).getTitle());

		// Karambwan
		PoiDetails.Detail d4 = new PoiDetails.Detail("Fishing Spot (Karambwan)", "Skilling • Fishing", List.of("Method / Fish: Karambwan"));
		assertEquals("Karambwan", builder.buildPoiCard(d4, poi, dummyIcon, true).getTitle());

		// Lava Eel
		PoiDetails.Detail d5 = new PoiDetails.Detail("Fishing Spot (Lava Eel)", "Skilling • Fishing", List.of("Method / Fish: Lava Eel"));
		assertEquals("Lava Eel", builder.buildPoiCard(d5, poi, dummyIcon, true).getTitle());
	}

	@Test
	public void miningSitesResolveMineralsInCompactForm()
	{
		final PoiIndex.Poi poi = new PoiIndex.Poi(3000, 3000, 0, "mining_site", "Mining site");

		// Single mineral
		PoiDetails.Detail d1 = new PoiDetails.Detail("Aldarin Mining Site", "Skilling • Mining", List.of("Ores available: Iron"));
		assertEquals("Iron", builder.buildPoiCard(d1, poi, dummyIcon, true).getTitle());

		// Two minerals
		PoiDetails.Detail d2 = new PoiDetails.Detail("Al Kharid Mine South Mining Site", "Skilling • Mining", List.of("Ores available: Iron, Gold"));
		assertEquals("Iron & Gold", builder.buildPoiCard(d2, poi, dummyIcon, true).getTitle());

		// Multi minerals with levels stripped
		PoiDetails.Detail d3 = new PoiDetails.Detail("The Pandemonium - Mining Site", "Skilling • Mining", List.of("Ores available: Copper (1), Tin (1), Iron (15), Lead (25)"));
		assertEquals("Copper, Tin, Iron & Lead", builder.buildPoiCard(d3, poi, dummyIcon, true).getTitle());
	}

	@Test
	public void rareTreesResolveTreeTypeWithoutLevelInCompactForm()
	{
		final PoiIndex.Poi poi = new PoiIndex.Poi(3000, 3000, 0, "rare_trees", "Rare trees");

		PoiDetails.Detail d1 = new PoiDetails.Detail("Willow tree (Level 30)", "Skilling • Woodcutting", List.of());
		assertEquals("Willow tree", builder.buildPoiCard(d1, poi, dummyIcon, true).getTitle());

		PoiDetails.Detail d2 = new PoiDetails.Detail("Yew tree (Level 60)", "Skilling • Woodcutting", List.of());
		assertEquals("Yew tree", builder.buildPoiCard(d2, poi, dummyIcon, true).getTitle());

		PoiDetails.Detail d3 = new PoiDetails.Detail("Magic tree (Level 75)", "Skilling • Woodcutting", List.of());
		assertEquals("Magic tree", builder.buildPoiCard(d3, poi, dummyIcon, true).getTitle());
	}

	@Test
	public void farmingPatchesResolvePatchTypeInCompactForm()
	{
		final PoiIndex.Poi poi = new PoiIndex.Poi(3000, 3000, 0, "farming_patch", "Farming patch");

		PoiDetails.Detail d1 = new PoiDetails.Detail("Herb Farming Patch", "Skilling • Farming", List.of());
		assertEquals("Herb patch", builder.buildPoiCard(d1, poi, dummyIcon, true).getTitle());

		PoiDetails.Detail d2 = new PoiDetails.Detail("Fruit Tree Farming Patch", "Skilling • Farming", List.of());
		assertEquals("Fruit Tree patch", builder.buildPoiCard(d2, poi, dummyIcon, true).getTitle());

		PoiDetails.Detail d3 = new PoiDetails.Detail("Allotment/Herb/Flower Farming Patch", "Skilling • Farming", List.of());
		assertEquals("Allotment, Herb, Flower patch", builder.buildPoiCard(d3, poi, dummyIcon, true).getTitle());
	}

	@Test
	public void wyrmscraigPoiNamesStripPrefixExceptUniqueStore()
	{
		// Unique store name is preserved
		final PoiIndex.Poi store = new PoiIndex.Poi(2572, 2242, 0, "clothes_shop", "Where Wyrmscraig's Wear Wares Were");
		final PoiDetails.Detail storeDetail = new PoiDetails.Detail("Where Wyrmscraig's Wear Wares Were", "Shops", List.of());
		assertEquals("Where Wyrmscraig's Wear Wares Were", builder.buildPoiCard(storeDetail, store, dummyIcon, true).getTitle());
		assertEquals("Where Wyrmscraig's Wear Wares Were", builder.buildPoiCard(storeDetail, store, dummyIcon, false).getTitle());

		// Wyrmscraig bank chest
		final PoiIndex.Poi bank = new PoiIndex.Poi(2586, 2260, 0, "bank", "Wyrmscraig chest");
		assertEquals("Bank chest", builder.buildPoiCard(null, bank, dummyIcon, true).getTitle());

		// Generic POIs with Wyrmscraig prefix stripped and capitalized
		assertEquals("Anvil", PoiTooltipBuilder.cleanPoiName("Wyrmscraig anvil"));
		assertEquals("Furnace", PoiTooltipBuilder.cleanPoiName("Wyrmscraig furnace"));
		assertEquals("Loom", PoiTooltipBuilder.cleanPoiName("Wyrmscraig loom"));
		assertEquals("Dairy churn", PoiTooltipBuilder.cleanPoiName("Wyrmscraig dairy churn"));
		assertEquals("Water source", PoiTooltipBuilder.cleanPoiName("Wyrmscraig water source"));
	}

	@Test
	public void wyrmscraigPoisInWorldMapResolveAccuratelyFromPoiDetails()
	{
		// Wyrmscraig fishing spot at (2575, 2219)
		final PoiIndex.Poi fishPoi = new PoiIndex.Poi(2575, 2219, 0, "fishing_spot", "Lobster & Swordfish");
		final PoiDetails.Detail fishDetail = PoiDetails.getDetail(fishPoi, 2575, 2219, 0);
		assertNotNull(fishDetail);
		assertEquals("Lobster & Swordfish", builder.buildPoiCard(fishDetail, fishPoi, dummyIcon, true).getTitle());

		// Wyrmscraig mine at (2598, 2239)
		final PoiIndex.Poi minePoi = new PoiIndex.Poi(2598, 2239, 0, "mining_site", "Sunstone & Iron");
		final PoiDetails.Detail mineDetail = PoiDetails.getDetail(minePoi, 2598, 2239, 0);
		assertNotNull(mineDetail);
		assertEquals("Sunstone & Iron", builder.buildPoiCard(mineDetail, minePoi, dummyIcon, true).getTitle());

		// Wyrmscraig Cavern mine at (2616, 8593)
		final PoiIndex.Poi cavMinePoi = new PoiIndex.Poi(2616, 8593, 0, "mining_site", "Coal, Mithril & Adamantite");
		final PoiDetails.Detail cavMineDetail = PoiDetails.getDetail(cavMinePoi, 2616, 8593, 0);
		assertNotNull(cavMineDetail);
		assertEquals("Coal, Mithril & Adamantite", builder.buildPoiCard(cavMineDetail, cavMinePoi, dummyIcon, true).getTitle());

		// Wyrmscraig Cavern lava eel at (2611, 8598)
		final PoiIndex.Poi eelPoi = new PoiIndex.Poi(2611, 8598, 0, "fishing_spot", "Lava eel");
		final PoiDetails.Detail eelDetail = PoiDetails.getDetail(eelPoi, 2611, 8598, 0);
		assertNotNull(eelDetail);
		assertEquals("Lava Eel", builder.buildPoiCard(eelDetail, eelPoi, dummyIcon, true).getTitle());

		// Wyrmscraig yew trees at (2543, 2184)
		final PoiIndex.Poi yewPoi = new PoiIndex.Poi(2543, 2184, 0, "rare_trees", "Yew trees");
		final PoiDetails.Detail yewDetail = PoiDetails.getDetail(yewPoi, 2543, 2184, 0);
		assertNotNull(yewDetail);
		assertEquals("Yew trees", builder.buildPoiCard(yewDetail, yewPoi, dummyIcon, true).getTitle());
	}
}
