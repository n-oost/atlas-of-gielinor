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
import java.util.Collections;
import java.util.List;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PoiTooltipBuilderTest
{
	@BeforeClass
	public static void loadCuratedDetails()
	{
		PoiDetails.load();
	}

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
	public void mooringCompactTitleKeepsIslandAndVerifiedSailingLevel()
	{
		final PoiIndex.Poi poi = new PoiIndex.Poi(
			2998, 2288, 0, "mooring_point", "Mooring point - The Onyx Crest");
		final PoiDetails.Detail detail = PoiDetails.getDetail(poi, poi.getX(), poi.getY(), poi.getPlane());
		final TooltipCard card = builder.buildPoiCard(detail, poi, dummyIcon, true);

		assertEquals("Mooring point - The Onyx Crest (47 Sailing)", card.getTitle());
		assertTrue(card.getLines().isEmpty());
	}

	@Test
	public void miningCompactTitleKeepsItsExplicitSkillRequirement()
	{
		final PoiIndex.Poi poi = new PoiIndex.Poi(2823, 3001, 0, "mining_site", "Mining site");
		final PoiDetails.Detail detail = PoiDetails.getDetail(poi, poi.getX(), poi.getY(), poi.getPlane());
		final TooltipCard card = builder.buildPoiCard(detail, poi, dummyIcon, true);

		assertEquals("Shilo Village mine (40 Mining)", card.getTitle());
		assertTrue(card.getLines().isEmpty());
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

	@Test
	public void hunterTrainingAndRareTreesCompactAndExpandedResolveCleanly()
	{
		// Wyrmscraig goats (hunter_training)
		final PoiIndex.Poi goatPoi = new PoiIndex.Poi(2572, 2195, 0, "hunter_training", "Goats");
		final PoiDetails.Detail goatDetail = PoiDetails.getDetail(goatPoi, 2572, 2195, 0);
		assertNotNull(goatDetail);
		// Compact format shows creature without level or "Wyrmscraig" prefix
		assertEquals("Goats", builder.buildPoiCard(goatDetail, goatPoi, dummyIcon, true).getTitle());
		// Expanded shows full title
		assertEquals("Goats (Level 60)", builder.buildPoiCard(goatDetail, goatPoi, dummyIcon, false).getTitle());

		// Red chinchompas with region prefix
		final PoiIndex.Poi chinPoi = new PoiIndex.Poi(2558, 2912, 0, "hunter_training", "Hunter training");
		final PoiDetails.Detail chinDetail = PoiDetails.getDetail(chinPoi, 2558, 2912, 0);
		assertEquals("Red Chinchompas", builder.buildPoiCard(chinDetail, chinPoi, dummyIcon, true).getTitle());

		// Raw RuneLite tooltip strings without parentheses: "wyrmscraig goats 60"
		final PoiDetails.Detail rawRlDetail = new PoiDetails.Detail("wyrmscraig goats 60", "Skilling • Hunter", Collections.emptyList());
		assertEquals("Goats", builder.buildPoiCard(rawRlDetail, goatPoi, dummyIcon, true).getTitle());

		// Rare trees: "Yew tree 60" or "Yew tree (60)"
		final PoiDetails.Detail rawTreeDetail = new PoiDetails.Detail("Yew tree 60", "Skilling • Woodcutting", Collections.emptyList());
		final PoiIndex.Poi treePoi = new PoiIndex.Poi(2562, 2191, 0, "rare_trees", "Teak trees");
		assertEquals("Yew tree", builder.buildPoiCard(rawTreeDetail, treePoi, dummyIcon, true).getTitle());

		// Wyrmscraig Teak trees
		final PoiDetails.Detail teakDetail = PoiDetails.getDetail(treePoi, 2562, 2191, 0);
		assertNotNull(teakDetail);
		assertEquals("Teak trees", builder.buildPoiCard(teakDetail, treePoi, dummyIcon, true).getTitle());
	}

	@Test
	public void runeLitePointCardCleansWyrmscraigPrefix()
	{
		final WorldMapPoint pt = new WorldMapPoint(new WorldPoint(2572, 2195, 0), dummyIcon);
		pt.setName("Wyrmscraig goats");
		pt.setTooltip("Level 60");
		final TooltipCard card = builder.buildRuneLitePointCard(pt);
		assertNotNull(card);
		assertEquals("Goats", card.getTitle());
	}

	@Test
	public void mapFeaturePoisResolveRealTooltipsAndCompactForms()
	{
		// 1. Port Sarim Mooring point
		final PoiIndex.Poi sarimPoi = new PoiIndex.Poi(3050, 3192, 0, "mooring_point", "Mooring point - Port Sarim");
		final PoiDetails.Detail sarimDetail = PoiDetails.getDetail(sarimPoi, 3050, 3192, 0);
		assertNotNull(sarimDetail);
		assertEquals("Mooring point - Port Sarim (1 Sailing)", builder.buildPoiCard(sarimDetail, sarimPoi, dummyIcon, true).getTitle());
		assertEquals("Mooring point - Port Sarim", builder.buildPoiCard(sarimDetail, sarimPoi, dummyIcon, false).getTitle());
		assertTrue(sarimDetail.getLines().stream().anyMatch(l -> l.contains("Requires Level 1 Sailing")));

		// 2. Wyrmscraig Mooring point
		final PoiIndex.Poi wyrmPoi = new PoiIndex.Poi(2568, 2297, 0, "mooring_point", "Mooring point - Wyrmscraig");
		final PoiDetails.Detail wyrmDetail = PoiDetails.getDetail(wyrmPoi, 2568, 2297, 0);
		assertNotNull(wyrmDetail);
		assertTrue(builder.buildPoiCard(wyrmDetail, wyrmPoi, dummyIcon, true).getTitle()
			.startsWith("Mooring point - Wyrmscraig"));

		// 3. Canoe station
		final PoiIndex.Poi canoePoi = new PoiIndex.Poi(3238, 3192, 0, "canoe_station", "Canoe station");
		final PoiDetails.Detail canoeDetail = PoiDetails.getDetail(canoePoi, 3238, 3192, 0);
		assertNotNull(canoeDetail);
		assertEquals("Canoe station", builder.buildPoiCard(canoeDetail, canoePoi, dummyIcon, true).getTitle());
		assertTrue(canoeDetail.getLines().stream().anyMatch(l -> l.contains("dugout, canoe, or waka")));

		// 4. Hot air balloon
		final PoiIndex.Poi balloonPoi = new PoiIndex.Poi(3022, 3343, 0, "hot_air_balloon", "Hot air balloon");
		final PoiDetails.Detail balloonDetail = PoiDetails.getDetail(balloonPoi, 3022, 3343, 0);
		assertNotNull(balloonDetail);
		assertEquals("Hot air balloon", builder.buildPoiCard(balloonDetail, balloonPoi, dummyIcon, true).getTitle());
		assertTrue(balloonDetail.getLines().stream().anyMatch(l -> l.contains("Enlightened Journey")));

		// 5. Magic mushtree
		final PoiIndex.Poi mushPoi = new PoiIndex.Poi(3763, 3755, 0, "magic_mushtree", "Magic mushtree");
		final PoiDetails.Detail mushDetail = PoiDetails.getDetail(mushPoi, 3763, 3755, 0);
		assertNotNull(mushDetail);
		assertEquals("Magic mushtree", builder.buildPoiCard(mushDetail, mushPoi, dummyIcon, true).getTitle());
		assertTrue(mushDetail.getLines().stream().anyMatch(l -> l.contains("Mushtree") || l.contains("Fossil Island")));

		// 6. Magic carpet
		final PoiIndex.Poi carpetPoi = new PoiIndex.Poi(3348, 2959, 0, "magic_carpet", "Magic carpet");
		final PoiDetails.Detail carpetDetail = PoiDetails.getDetail(carpetPoi, 3348, 2959, 0);
		assertNotNull(carpetDetail);
		assertEquals("Magic carpet", builder.buildPoiCard(carpetDetail, carpetPoi, dummyIcon, true).getTitle());
		assertTrue(carpetDetail.getLines().stream().anyMatch(l -> l.contains("Rug merchant")));

		// 7. Minecart network
		final PoiIndex.Poi cartPoi = new PoiIndex.Poi(1384, 3274, 0, "minecart_network", "Minecart network");
		final PoiDetails.Detail cartDetail = PoiDetails.getDetail(cartPoi, 1384, 3274, 0);
		assertNotNull(cartDetail);
		assertEquals("Minecart network", builder.buildPoiCard(cartDetail, cartPoi, dummyIcon, true).getTitle());
		assertTrue(cartDetail.getLines().stream().anyMatch(l -> l.contains("minecart transit system")));

		// 8. Shipwright
		final PoiIndex.Poi shipPoi = new PoiIndex.Poi(2171, 3324, 0, "shipwright", "Shipwright");
		final PoiDetails.Detail shipDetail = PoiDetails.getDetail(shipPoi, 2171, 3324, 0);
		assertNotNull(shipDetail);
		assertEquals("Shipwright", builder.buildPoiCard(shipDetail, shipPoi, dummyIcon, true).getTitle());
		assertTrue(shipDetail.getLines().stream().anyMatch(l -> l.contains("Shipyard services")));

		// 9. Port task board
		final PoiIndex.Poi taskPoi = new PoiIndex.Poi(2170, 3328, 0, "noticeboard", "Port task board");
		final PoiDetails.Detail taskDetail = PoiDetails.getDetail(taskPoi, 2170, 3328, 0);
		assertNotNull(taskDetail);
		assertEquals("Port task board", builder.buildPoiCard(taskDetail, taskPoi, dummyIcon, true).getTitle());
		assertTrue(taskDetail.getLines().stream().anyMatch(l -> l.contains("bounty contracts")));

		// 10. Cargo bay
		final PoiIndex.Poi cargoPoi = new PoiIndex.Poi(2173, 3335, 0, "cargo_bay", "Cargo bay");
		final PoiDetails.Detail cargoDetail = PoiDetails.getDetail(cargoPoi, 2173, 3335, 0);
		assertNotNull(cargoDetail);
		assertEquals("Cargo bay", builder.buildPoiCard(cargoDetail, cargoPoi, dummyIcon, true).getTitle());
		assertTrue(cargoDetail.getLines().stream().anyMatch(l -> l.contains("cargo loading bay")));

		// 11. Singing bowl
		final PoiIndex.Poi bowlPoi = new PoiIndex.Poi(3295, 6043, 0, "singing_bowl", "Singing bowl");
		final PoiDetails.Detail bowlDetail = PoiDetails.getDetail(bowlPoi, 3295, 6043, 0);
		assertNotNull(bowlDetail);
		assertEquals("Singing bowl", builder.buildPoiCard(bowlDetail, bowlPoi, dummyIcon, true).getTitle());
		assertTrue(bowlDetail.getLines().stream().anyMatch(l -> l.contains("Song of the Elves")));

		// 12. Imbued altar
		final PoiIndex.Poi altarPoi = new PoiIndex.Poi(3022, 6049, 0, "altar", "Imbued altar");
		final PoiDetails.Detail altarDetail = PoiDetails.getDetail(altarPoi, 3022, 6049, 0);
		assertNotNull(altarDetail);
		assertEquals("Imbued altar", builder.buildPoiCard(altarDetail, altarPoi, dummyIcon, true).getTitle());
		assertTrue(altarDetail.getLines().stream().anyMatch(l -> l.contains("Zalcano")));
	}

	@Test
	public void questTooltipsRenderCleanNameInCompactAndRichCardInExpanded()
	{
		// Cook'''s Assistant
		final PoiIndex.Poi cooksPoi = new PoiIndex.Poi(3210, 3215, 0, "quest_start", "Cook's Assistant");
		final PoiDetails.Detail cooksDetail = PoiDetails.getDetail(cooksPoi, 3210, 3215, 0);
		assertNotNull(cooksDetail);
		// Compact format: <ICON> <NAME>
		assertEquals("Cook's Assistant", builder.buildPoiCard(cooksDetail, cooksPoi, dummyIcon, true).getTitle());
		// Expanded format:
		final TooltipCard expandedCard = builder.buildPoiCard(cooksDetail, cooksPoi, dummyIcon, false);
		assertEquals("Cook's Assistant", expandedCard.getTitle());
		assertTrue(expandedCard.getLines().stream().anyMatch(l -> l.contains("Lumbridge Castle")));
		assertTrue(expandedCard.getLines().stream().anyMatch(l -> l.contains("Reward: 1 Quest Point")));

		// Song of the Elves
		final PoiIndex.Poi sotePoi = new PoiIndex.Poi(2579, 3295, 1, "quest_start", "Song of the Elves");
		final PoiDetails.Detail soteDetail = PoiDetails.getDetail(sotePoi, 2579, 3295, 1);
		assertNotNull(soteDetail);
		assertEquals("Song of the Elves", builder.buildPoiCard(soteDetail, sotePoi, dummyIcon, true).getTitle());
		final TooltipCard soteExpanded = builder.buildPoiCard(soteDetail, sotePoi, dummyIcon, false);
		assertTrue(soteExpanded.getLines().stream().anyMatch(l -> l.contains("East Ardougne")));
		assertTrue(soteExpanded.getLines().stream().anyMatch(l -> l.contains("Grandmaster")));
	}
}

