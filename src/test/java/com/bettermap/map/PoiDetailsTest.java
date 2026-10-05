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

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.TreeMap;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;

public class PoiDetailsTest
{
	@BeforeClass
	public static void loadCuratedDetails()
	{
		PoiDetails.load();
	}

	/** Fingerprint of all map keys, titles, categories and lines from the original quest table. */
	@Test
	public void questResourcePreservesTheCompleteOriginalCatalog() throws Exception
	{
		final StringBuilder snapshot = new StringBuilder();
		for (Map.Entry<String, PoiDetails.Detail> entry : new TreeMap<>(QuestDetailsData.load()).entrySet())
		{
			final PoiDetails.Detail detail = entry.getValue();
			snapshot.append(entry.getKey()).append('\t').append(detail.getTitle()).append('\t').append(detail.getCategory());
			for (String line : detail.getLines())
			{
				snapshot.append('\t').append(line);
			}
			snapshot.append('\n');
		}
		final byte[] digest = MessageDigest.getInstance("SHA-256").digest(snapshot.toString().getBytes(StandardCharsets.UTF_8));
		assertEquals("01226a2e0d70c494311af026b4cabeda08422f0f80b43cfb2ce661a3ac1d99c6", String.format("%064x", new BigInteger(1, digest)));
	}

	@Test
	public void mooringMarkerKeepsItsIdentityWhenAnActivitySharesItsExactTile()
	{
		final PoiIndex.Poi marker = new PoiIndex.Poi(
			2998, 2288, 0, "mooring_point", "Mooring point - The Onyx Crest");
		final PoiDetails.Detail detail = PoiDetails.getDetail(marker, marker.getX(), marker.getY(), marker.getPlane());

		assertNotNull(detail);
		assertEquals(marker.getName(), detail.getTitle());
		assertEquals("Travel", detail.getCategory());
		assertTrue(detail.getLines().stream().anyMatch(line -> line.contains("Mooring point")));
		assertFalse(detail.getLines().stream().anyMatch(line -> line.contains("Mining Site")));
	}

	@Test
	public void summerShoreMooringIncludesItsQuestAccessCondition()
	{
		final PoiIndex.Poi marker = new PoiIndex.Poi(
			3175, 2367, 0, "mooring_point", "Mooring point - The Summer Shore");
		final PoiDetails.Detail detail = PoiDetails.getDetail(marker, marker.getX(), marker.getY(), marker.getPlane());

		assertTrue(detail.getLines().contains("Requires Level 45 Sailing"));
		assertTrue(detail.getLines().contains("Requires completion of Troubled Tortugans"));
	}

	@Test
	public void shiloVillageMineUsesMiningDetailsAtItsExactMarkerTile()
	{
		final PoiIndex.Poi marker = new PoiIndex.Poi(2823, 3001, 0, "mining_site", "Mining site");
		final PoiDetails.Detail detail = PoiDetails.getDetail(marker, marker.getX(), marker.getY(), marker.getPlane());

		assertNotNull(detail);
		assertEquals("Shilo Village mine", detail.getTitle());
		assertEquals("Skilling • Mining", detail.getCategory());
		assertTrue(detail.getLines().contains("Requires Level 40 Mining"));
		assertTrue(detail.getLines().contains("Requires completion of Shilo Village"));
		assertFalse(detail.getLines().stream().anyMatch(line -> line.contains("Dungeon")));
	}

	@Test
	public void basementsUseTheirOwnPoiKey()
	{
		for (PoiIndex.Poi poi : PoiDetails.getAllPois())
		{
			final String name = poi.getName().toLowerCase(java.util.Locale.ROOT);
			if (name.contains("basement") || name.contains("cellar"))
			{
				assertEquals("Basement must not use the dungeon icon: " + poi.getName(), "basement", poi.getKey());
			}
		}
	}

	@Test
	public void dungeonEntriesDoNotContainExactDuplicates()
	{
		final Set<String> seen = new HashSet<>();
		for (PoiIndex.Poi poi : PoiDetails.getAllPois())
		{
			if (!"dungeon".equals(poi.getKey()))
			{
				continue;
			}
			final String key = poi.getName() + '\t' + poi.getX() + '\t' + poi.getY() + '\t' + poi.getPlane();
			assertTrue("Duplicate dungeon entry: " + key, seen.add(key));
		}
	}

	@Test
	public void riverKeldaIsNotExposedAsADungeonPoi()
	{
		assertFalse(PoiDetails.getAllPois().stream()
			.anyMatch(poi -> "dungeon".equals(poi.getKey()) && "River Kelda".equals(poi.getName())));
	}

	@Test
	public void zemouregalsBaseHasItsSurfacePoiAndHoverDetails()
	{
		final PoiDetails.Detail detail = PoiDetails.getDetailByPosition(3343, 3515, 0, 0);
		assertNotNull(detail);
		assertEquals("Zemouregal's Base", detail.getTitle());
	}

	@Test
	public void reportedMissingDungeonHoversNowResolve()
	{
		final Object[][] expected = {
			{2142, 3944, "Lunar Isle Mine"},
			{3244, 3949, "Scorpia cave"},
			{3320, 3798, "Silk Chasm"},
			{3284, 3808, "Escape Caves"},
			{3292, 3850, "Callisto's Den"},
			{3221, 3788, "Vet'ion's Rest"},
			{2839, 3690, "Troll Stronghold"},
			{3485, 3321, "Shade Catacombs"},
			{3360, 3150, "Giants' Foundry"}
		};
		for (Object[] row : expected)
		{
			final PoiDetails.Detail detail = PoiDetails.getDetailByPosition(
				(int) row[0], (int) row[1], 0, 6);
			assertNotNull("Missing hover for " + row[2], detail);
			assertEquals(row[2], detail.getTitle());
		}
	}

	@Test
	public void verifiedSurfaceDungeonCoordinatesResolveToTheirOwnLocations()
	{
		assertEquals("Fortis Colosseum", PoiDetails.getDetailByPosition(1824, 3107, 0, 0).getTitle());
		assertEquals("Digsite Dungeon", PoiDetails.getDetailByPosition(3370, 3428, 0, 0).getTitle());
		assertEquals("Morytania Spider Cave", PoiDetails.getDetailByPosition(3657, 3407, 0, 0).getTitle());
		assertEquals("Temple of Ikov", PoiDetails.getDetailByPosition(2676, 3404, 0, 0).getTitle());
		assertEquals("Temple of Ikov — McGrubor's Wood shortcut",
			PoiDetails.getDetailByPosition(2659, 3492, 0, 0).getTitle());
	}

	@Test
	public void isleOfSoulsDetailsOnlyOwnTheInteriorPoi()
	{
		final List<PoiIndex.Poi> entries = PoiDetails.getAllPois().stream()
			.filter(poi -> "Isle of Souls Dungeon".equals(poi.getName()))
			.collect(java.util.stream.Collectors.toList());
		assertEquals(1, entries.size());
		assertEquals(2135, entries.get(0).getX());
		assertEquals(9320, entries.get(0).getY());
	}

	@Test
	public void camphorTreesOnGreatConchProvideCorrectLevelAndTool()
	{
		// Standalone Camphor Tree on Great Conch
		final PoiIndex.Poi camphorTree = new PoiIndex.Poi(3192, 2487, 0, "rare_trees", "Rare trees");
		final PoiDetails.Detail detail = PoiDetails.getDetail(camphorTree, 3192, 2487, 0);

		assertNotNull(detail);
		assertTrue(detail.getTitle().contains("Camphor"));
		assertTrue(detail.getLines().stream().anyMatch(l -> l.contains("Level 66 Woodcutting")));
		assertTrue(detail.getLines().stream().anyMatch(l -> l.contains("Axe")));

		// Camphor / Mahogany mixed grove on Great Conch
		final PoiIndex.Poi camphorMahogany = new PoiIndex.Poi(3245, 2397, 0, "rare_trees", "Rare trees");
		final PoiDetails.Detail cmDetail = PoiDetails.getDetail(camphorMahogany, 3245, 2397, 0);

		assertNotNull(cmDetail);
		assertTrue(cmDetail.getTitle().contains("Camphor/Mahogany"));
		assertTrue(cmDetail.getLines().stream().anyMatch(l -> l.contains("Level 66 Woodcutting")));

		// Camphor / Jatoba mixed grove on Great Conch
		final PoiIndex.Poi camphorJatoba = new PoiIndex.Poi(3109, 2413, 0, "rare_trees", "Rare trees");
		final PoiDetails.Detail cjDetail = PoiDetails.getDetail(camphorJatoba, 3109, 2413, 0);

		assertNotNull(cjDetail);
		assertTrue(cjDetail.getTitle().contains("Camphor/Jatoba"));
		assertTrue(cjDetail.getLines().stream().anyMatch(l -> l.contains("Level 66 Woodcutting")));
	}

	@Test
	public void questStartLocationsProvideExactQuestNameAndDetails()
	{
		// Cook's Assistant in Lumbridge
		final PoiIndex.Poi cooksAssistant = new PoiIndex.Poi(3208, 3214, 0, "quest_start", "Quest start");
		final PoiDetails.Detail caDetail = PoiDetails.getDetail(cooksAssistant, 3208, 3214, 0);

		assertNotNull(caDetail);
		assertEquals("Cook's Assistant", caDetail.getTitle());
		assertTrue(caDetail.getCategory().contains("Quest Start"));
		assertTrue(caDetail.getLines().stream().anyMatch(l -> l.contains("Cook in Lumbridge Castle")));
		assertTrue(caDetail.getLines().stream().anyMatch(l -> l.contains("1 Quest Point")));

		// Dragon Slayer I in Edgeville
		final PoiIndex.Poi dragonSlayer = new PoiIndex.Poi(3074, 3515, 0, "quest_start", "Quest start");
		final PoiDetails.Detail dsDetail = PoiDetails.getDetail(dragonSlayer, 3074, 3515, 0);

		assertNotNull(dsDetail);
		assertEquals("Dragon Slayer I", dsDetail.getTitle());
		assertTrue(dsDetail.getLines().stream().anyMatch(l -> l.contains("Oziach")));

		// Desert Treasure II
		final PoiIndex.Poi dt2 = new PoiIndex.Poi(3480, 2780, 0, "quest_start", "Quest start");
		final PoiDetails.Detail dt2Detail = PoiDetails.getDetail(dt2, 3480, 2780, 0);

		assertNotNull(dt2Detail);
		assertTrue(dt2Detail.getTitle().contains("Desert Treasure II"));
		assertTrue(dt2Detail.getCategory().contains("Grandmaster"));
	}

	@Test
	public void dungeonLocationsProvideAccurateDescriptions()
	{
		// Wine Cellar in Aldarin (Varlamore)
		final PoiIndex.Poi wineCellar = new PoiIndex.Poi(1447, 2938, 0, "dungeon", "Dungeon");
		final PoiDetails.Detail wcDetail = PoiDetails.getDetail(wineCellar, 1447, 2938, 0);

		assertNotNull(wcDetail);
		assertTrue(wcDetail.getTitle().contains("Wine Cellar"));
		assertEquals("Dungeons", wcDetail.getCategory());

		// Edgeville Dungeon
		final PoiIndex.Poi edgeDungeon = new PoiIndex.Poi(3096, 3469, 0, "dungeon", "Dungeon");
		final PoiDetails.Detail edDetail = PoiDetails.getDetail(edgeDungeon, 3096, 3469, 0);

		assertNotNull(edDetail);
		assertTrue(edDetail.getTitle().contains("Edgeville Dungeon"));
		assertEquals("Dungeons", edDetail.getCategory());
	}

	@Test
	public void fishingSpotProvidesFishTypesToolsAndLevels()
	{
		// Al Kharid Fishing Spot
		final PoiIndex.Poi alKharidFish = new PoiIndex.Poi(3274, 3140, 0, "fishing_spot", "Fishing spot");
		final PoiDetails.Detail detail = PoiDetails.getDetail(alKharidFish, 3274, 3140, 0);

		assertNotNull(detail);
		assertTrue(detail.getTitle().contains("Fishing Spot"));
		assertTrue(detail.getCategory().contains("Fishing"));
		assertTrue(detail.getLines().stream().anyMatch(l -> l.contains("Net") || l.contains("Shrimp") || l.contains("Tools")));
	}

	@Test
	public void miningSiteProvidesOresLevelsAndPickaxe()
	{
		// Aldarin Mining Site
		final PoiIndex.Poi aldarinMine = new PoiIndex.Poi(1430, 2882, 0, "mining_site", "Mining site");
		final PoiDetails.Detail detail = PoiDetails.getDetail(aldarinMine, 1430, 2882, 0);

		assertNotNull(detail);
		assertTrue(detail.getTitle().contains("Mining Site"));
		assertTrue(detail.getCategory().contains("Mining"));
		assertTrue(detail.getLines().stream().anyMatch(l -> l.contains("Iron") || l.contains("Pickaxe")));
	}

	@Test
	public void agilityCourseProvidesLevelRequirementAndLapXp()
	{
		// Draynor Village Rooftop Course
		final PoiIndex.Poi draynorRooftop = new PoiIndex.Poi(3103, 3279, 0, "agility_training", "Agility Course");
		final PoiDetails.Detail detail = PoiDetails.getDetail(draynorRooftop, 3103, 3279, 0);

		assertNotNull(detail);
		assertTrue(detail.getTitle().contains("Draynor Village Rooftop Course"));
		assertTrue(detail.getLines().stream().anyMatch(l -> l.contains("Level 10 Agility")));
	}

	@Test
	public void fairyRingProvidesCodeAndDestination()
	{
		// Edgeville Fairy Ring DKR
		final PoiIndex.Poi edgevilleRing = new PoiIndex.Poi(3126, 3496, 0, "transportation", "Fairy ring");
		final PoiDetails.Detail detail = PoiDetails.getDetail(edgevilleRing, 3126, 3496, 0);

		assertNotNull(detail);
		assertTrue(detail.getTitle().contains("DKR"));
	}

	@Test
	public void woodcuttingGroveProvidesTreesAndAxeRequirement()
	{
		// Varrock Castle Yews
		final PoiIndex.Poi varrockYews = new PoiIndex.Poi(3204, 3504, 0, "rare_trees", "Rare trees");
		final PoiDetails.Detail detail = PoiDetails.getDetail(varrockYews, 3204, 3504, 0);

		assertNotNull(detail);
		assertTrue(detail.getTitle().contains("Yew tree"));
		assertTrue(detail.getLines().stream().anyMatch(l -> l.contains("Level 60 Woodcutting")));
		assertTrue(detail.getLines().stream().anyMatch(l -> l.contains("Axe")));
	}

	@Test
	public void farmingPatchProvidesCropsAndTools()
	{
		// Farming patch
		final PoiIndex.Poi patch = new PoiIndex.Poi(3793, 2836, 0, "farming_patch", "Farming patch");
		final PoiDetails.Detail detail = PoiDetails.getDetail(patch, 3793, 2836, 0);

		assertNotNull(detail);
		assertTrue(detail.getTitle().contains("Patch"));
		assertTrue(detail.getLines().stream().anyMatch(l -> l.contains("Seed dibber") || l.contains("Rake")));
	}

	@Test
	public void genericKeysProduceUsefulCardsInsteadOfEmptyOnes()
	{
		final String[] keys = {
			"bank", "altar", "water_source", "cooking_range", "anvil", "furnace", "poll_booth",
			"dungeon_link", "house_portal", "combat_tutor", "agility_training", "hunter_training"
		};

		for (String key : keys)
		{
			final PoiIndex.Poi poi = new PoiIndex.Poi(1000, 1000, 0, key, "Test");
			final PoiDetails.Detail detail = PoiDetails.getDetail(poi, 1000, 1000, 0);

			assertNotNull("Detail should not be null for " + key, detail);
			assertFalse("Lines should not be empty for " + key, detail.getLines().isEmpty());
			assertNotEquals("Category should not be 'Point of Interest' for " + key, "Point of Interest", detail.getCategory());
			for (String line : detail.getLines())
			{
				assertTrue("Line too long for " + key + ": " + line, line.length() < 80);
			}
		}
	}

	@Test
	public void existingCuratedEntriesStillWin()
	{
		final PoiIndex.Poi cooksAssistant = new PoiIndex.Poi(3208, 3214, 0, "quest_start", "Quest start");
		final PoiDetails.Detail caDetail = PoiDetails.getDetail(cooksAssistant, 3208, 3214, 0);

		assertNotNull(caDetail);
		assertEquals("Cook's Assistant", caDetail.getTitle());
		assertTrue(caDetail.getCategory().contains("Quest Start"));
		assertTrue(caDetail.getLines().stream().anyMatch(l -> l.contains("Cook in Lumbridge Castle")));
		assertTrue(caDetail.getLines().stream().anyMatch(l -> l.contains("1 Quest Point")));
	}

	@Test
	public void spatialGridReturnsTheSameEntryAsTheLinearScan()
	{
		final java.util.Random random = new java.util.Random(20260831);
		for (int i = 0; i < 5000; i++)
		{
			final int x = 1100 + random.nextInt(2800);
			final int y = 2500 + random.nextInt(8500);
			final int plane = random.nextInt(4);
			final String hint = random.nextBoolean() ? "quest_start" : null;

			final PoiDetails.Entry grid = PoiDetails.nearestEntry(x, y, plane, 144, hint);
			final PoiDetails.Entry linear = PoiDetails.nearestEntryLinear(x, y, plane, 144, hint);

			if (grid == null && linear == null)
			{
				continue;
			}
			assertNotNull("grid null but linear=" + (linear == null ? null : linear.title), grid);
			assertNotNull("linear null but grid=" + grid.title, linear);
			assertEquals(linear.title, grid.title);
		}
	}

	@Test
	public void kalphiteLairShowsRopeRequirement()
	{
		final PoiIndex.Poi poi = new PoiIndex.Poi(3226, 3108, 0, "dungeon_link", "Dungeon");
		final PoiDetails.Detail detail = PoiDetails.getDetail(poi, 3226, 3108, 0);

		assertNotNull(detail);
		assertEquals("Kalphite Lair", detail.getTitle());
		assertTrue(String.join(" ", detail.getLines()).toLowerCase().contains("rope"));
	}

	@Test
	public void gryphonDungeonResolvesThroughDungeonLinkKey()
	{
		final PoiIndex.Poi poi = new PoiIndex.Poi(3120, 2490, 0, "dungeon_link", "Dungeon");
		final PoiDetails.Detail detail = PoiDetails.getDetail(poi, 3120, 2490, 0);

		assertNotNull(detail);
		assertTrue(detail.getTitle().toLowerCase().contains("gryphon"));
		assertTrue(String.join(" ", detail.getLines()).toLowerCase().contains("varlamore")
			|| String.join(" ", detail.getLines()).toLowerCase().contains("children of the sun"));
	}

	@Test
	public void waterbirthDungeonResolvesRequirementCard()
	{
		final PoiIndex.Poi poi = new PoiIndex.Poi(2520, 3740, 0, "dungeon", "Waterbirth Dungeon");
		final PoiDetails.Detail detail = PoiDetails.getDetail(poi, 2520, 3740, 0);

		assertNotNull(detail);
		assertEquals("Waterbirth Dungeon", detail.getTitle());
		assertTrue(String.join(" ", detail.getLines()).toLowerCase().contains("dagannoth"));
		assertTrue(String.join(" ", detail.getLines()).toLowerCase().contains("rellekka")
			|| String.join(" ", detail.getLines()).toLowerCase().contains("combat gear"));

		// Direct name lookup in fallback path
		final PoiIndex.Poi uncuratedPoi = new PoiIndex.Poi(0, 0, 0, "dungeon", "Waterbirth Dungeon");
		final PoiDetails.Detail uncuratedDetail = PoiDetails.getDetail(uncuratedPoi, 0, 0, 0);
		assertNotNull(uncuratedDetail);
		assertTrue(String.join(" ", uncuratedDetail.getLines()).toLowerCase().contains("dagannoth"));
	}

	@Test
	public void childrenOfTheSunQuestStartIsInVarrockSquare()
	{
		// Should resolve at Varrock Square (3211, 3424, 0)
		final PoiIndex.Poi varrockSquare = new PoiIndex.Poi(3211, 3424, 0, "quest_start", "Quest start");
		final PoiDetails.Detail detail = PoiDetails.getDetail(varrockSquare, 3211, 3424, 0);

		assertNotNull(detail);
		assertEquals("Children of the Sun", detail.getTitle());
		assertTrue(detail.getLines().stream().anyMatch(l -> l.contains("Varrock Square")));

		// Old incorrect Varlamore location should not match Children of the Sun
		final PoiIndex.Poi oldLocation = new PoiIndex.Poi(1694, 3104, 0, "quest_start", "Quest start");
		final PoiDetails.Detail oldDetail = PoiDetails.getDetail(oldLocation, 1694, 3104, 0);
		assertTrue(oldDetail == null || !oldDetail.getTitle().equals("Children of the Sun"));
	}

	@Test
	public void highImpactDungeonRequirementsResolve()
	{
		// Giant Mole / Mole Hole
		final PoiIndex.Poi mole = new PoiIndex.Poi(2985, 3387, 0, "dungeon", "Mole Hole");
		final PoiDetails.Detail moleDetail = PoiDetails.getDetail(mole, 2985, 3387, 0);
		assertNotNull(moleDetail);
		assertTrue(String.join(" ", moleDetail.getLines()).toLowerCase().contains("spade"));

		// Karuulm Slayer Dungeon
		final PoiIndex.Poi karuulm = new PoiIndex.Poi(1308, 3807, 0, "dungeon", "Karuulm Slayer Dungeon");
		final PoiDetails.Detail karuulmDetail = PoiDetails.getDetail(karuulm, 1308, 3807, 0);
		assertNotNull(karuulmDetail);
		assertTrue(String.join(" ", karuulmDetail.getLines()).toLowerCase().contains("boots"));

		// ToA
		final PoiIndex.Poi toa = new PoiIndex.Poi(3357, 2711, 0, "dungeon", "Tombs of Amascut");
		final PoiDetails.Detail toaDetail = PoiDetails.getDetail(toa, 3357, 2711, 0);
		assertNotNull(toaDetail);
		assertTrue(String.join(" ", toaDetail.getLines()).toLowerCase().contains("cursed sands"));
	}

	@Test
	public void lumbridgeSwampWestMiningSiteHasCorrectCoordinatesAndOres()
	{
		final PoiIndex.Poi lsw = new PoiIndex.Poi(3148, 3149, 0, "mining_site", "Mining site");
		final PoiDetails.Detail detail = PoiDetails.getDetail(lsw, 3148, 3149, 0);

		assertNotNull(detail);
		assertEquals("Lumbridge Swamp West Mining Site", detail.getTitle());
		assertTrue(detail.getLines().stream().anyMatch(l -> l.contains("Coal")));
		assertFalse(detail.getLines().stream().anyMatch(l -> l.contains("Lunar")));
	}

	@Test
	public void hunterTrainingSpotsResolveWithLevelAndMethodByProximityAndExact()
	{
		// Feldip Hills Red Chins (curated exact and wiki generic label)
		final PoiIndex.Poi redChins = new PoiIndex.Poi(2558, 2912, 0, "hunter_training", "Hunter training");
		final PoiDetails.Detail rcDetail = PoiDetails.getDetail(redChins, 2558, 2912, 0);
		assertNotNull(rcDetail);
		assertTrue(rcDetail.getTitle().contains("Red Chinchompas"));
		assertEquals("Skilling • Hunter", rcDetail.getCategory());
		assertTrue(rcDetail.getLines().stream().anyMatch(l -> l.contains("Level 63 Hunter")));
		assertTrue(rcDetail.getLines().stream().anyMatch(l -> l.contains("Box trap")));

		// Wilderness Black Chinchompas (resolves by proximity near 3143, 3771)
		final PoiIndex.Poi blackChins = new PoiIndex.Poi(3145, 3770, 0, "hunter_training", "Hunter training");
		final PoiDetails.Detail bcDetail = PoiDetails.getDetail(blackChins, 3145, 3770, 0);
		assertNotNull(bcDetail);
		assertTrue(bcDetail.getTitle().contains("Black Chinchompas"));
		assertTrue(bcDetail.getLines().stream().anyMatch(l -> l.contains("Level 73 Hunter")));

		// Fossil Island Herbiboar
		final PoiIndex.Poi herbiboar = new PoiIndex.Poi(3694, 3800, 0, "hunter_training", "Hunter training");
		final PoiDetails.Detail hbDetail = PoiDetails.getDetail(herbiboar, 3694, 3800, 0);
		assertNotNull(hbDetail);
		assertTrue(hbDetail.getTitle().contains("Herbiboar"));
		assertTrue(hbDetail.getLines().stream().anyMatch(l -> l.contains("Level 80 Hunter")));
		assertTrue(hbDetail.getLines().stream().anyMatch(l -> l.contains("Tracking")));

		// Varlamore Hunter's Guild
		final PoiIndex.Poi guild = new PoiIndex.Poi(1557, 3045, 0, "hunter_training", "Hunter training");
		final PoiDetails.Detail gDetail = PoiDetails.getDetail(guild, 1557, 3045, 0);
		assertNotNull(gDetail);
		assertTrue(gDetail.getTitle().contains("Hunter's Guild"));
		assertTrue(gDetail.getLines().stream().anyMatch(l -> l.contains("Hunter Rumours") || l.contains("Level 46+")));

		// Piscatoris Falconry
		final PoiIndex.Poi falconry = new PoiIndex.Poi(2380, 3599, 0, "hunter_training", "Hunter training");
		final PoiDetails.Detail fDetail = PoiDetails.getDetail(falconry, 2380, 3599, 0);
		assertNotNull(fDetail);
		assertTrue(fDetail.getTitle().contains("Falconry"));
		assertTrue(fDetail.getLines().stream().anyMatch(l -> l.contains("Falconry")));
	}

	@Test
	public void agilityCoursesProvideComprehensiveCoverageAndDetails()
	{
		// Colossal Wyrm Course
		final PoiIndex.Poi wyrm = new PoiIndex.Poi(1649, 2930, 0, "agility_training", "Agility training");
		final PoiDetails.Detail wyrmDetail = PoiDetails.getDetail(wyrm, 1649, 2930, 0);
		assertNotNull(wyrmDetail);
		assertTrue(wyrmDetail.getTitle().contains("Colossal Wyrm"));
		assertTrue(wyrmDetail.getLines().stream().anyMatch(l -> l.contains("Level 50 Agility")));
		assertTrue(wyrmDetail.getLines().stream().anyMatch(l -> l.contains("Termites") || l.contains("450 XP")));

		// Hallowed Sepulchre
		final PoiIndex.Poi sepulchre = new PoiIndex.Poi(3655, 3386, 0, "agility_training", "Agility training");
		final PoiDetails.Detail sepDetail = PoiDetails.getDetail(sepulchre, 3655, 3386, 0);
		assertNotNull(sepDetail);
		assertTrue(sepDetail.getTitle().contains("Hallowed Sepulchre"));
		assertTrue(sepDetail.getLines().stream().anyMatch(l -> l.contains("Level 52–92 Agility")));

		// Prifddinas Course
		final PoiIndex.Poi prif = new PoiIndex.Poi(3253, 6109, 0, "agility_training", "Agility training");
		final PoiDetails.Detail prifDetail = PoiDetails.getDetail(prif, 3253, 6109, 0);
		assertNotNull(prifDetail);
		assertTrue(prifDetail.getTitle().contains("Prifddinas"));
		assertTrue(prifDetail.getLines().stream().anyMatch(l -> l.contains("Level 75 Agility")));
		assertTrue(prifDetail.getLines().stream().anyMatch(l -> l.contains("1,337 XP")));
	}

	@Test
	public void agilityShortcutsResolveWithLevelAndPurposeByProximityAndExact()
	{
		// Grand Exchange underwall tunnel
		final PoiIndex.Poi geTunnel = new PoiIndex.Poi(3140, 3515, 0, "agility_short-cut", "Agility short-cut");
		final PoiDetails.Detail geDetail = PoiDetails.getDetail(geTunnel, 3140, 3515, 0);
		assertNotNull(geDetail);
		assertTrue(geDetail.getTitle().contains("Grand Exchange"));
		assertTrue(geDetail.getLines().stream().anyMatch(l -> l.contains("Level 21 Agility")));
		assertTrue(geDetail.getLines().stream().anyMatch(l -> l.contains("Edgeville")));

		// Falador crumbling wall
		final PoiIndex.Poi fallyWall = new PoiIndex.Poi(2937, 3357, 0, "agility_short-cut", "Agility short-cut");
		final PoiDetails.Detail fallyDetail = PoiDetails.getDetail(fallyWall, 2937, 3357, 0);
		assertNotNull(fallyDetail);
		assertTrue(fallyDetail.getTitle().contains("Falador"));
		assertTrue(fallyDetail.getLines().stream().anyMatch(l -> l.contains("Level 5 Agility")));
		assertTrue(fallyDetail.getLines().stream().anyMatch(l -> l.contains("West Bank") || l.contains("Taverley")));

		// Yanille underwall tunnel
		final PoiIndex.Poi yanilleTunnel = new PoiIndex.Poi(2575, 3109, 0, "agility_short-cut", "Agility short-cut");
		final PoiDetails.Detail yanilleDetail = PoiDetails.getDetail(yanilleTunnel, 2575, 3109, 0);
		assertNotNull(yanilleDetail);
		assertTrue(yanilleDetail.getTitle().contains("Yanille"));
		assertTrue(yanilleDetail.getLines().stream().anyMatch(l -> l.contains("Level 16 Agility")));

		// Trollheim cliff scramble
		final PoiIndex.Poi trollheim = new PoiIndex.Poi(2870, 3670, 0, "agility_short-cut", "Agility short-cut");
		final PoiDetails.Detail trollDetail = PoiDetails.getDetail(trollheim, 2870, 3670, 0);
		assertNotNull(trollDetail);
		assertTrue(trollDetail.getTitle().contains("Trollheim"));
		assertTrue(trollDetail.getLines().stream().anyMatch(l -> l.contains("Level 41 Agility")));

		// Mount Karuulm lower rocks
		final PoiIndex.Poi karuulmRocks = new PoiIndex.Poi(1325, 3782, 0, "agility_short-cut", "Agility short-cut");
		final PoiDetails.Detail karuulmDetail = PoiDetails.getDetail(karuulmRocks, 1325, 3782, 0);
		assertNotNull(karuulmDetail);
		assertTrue(karuulmDetail.getTitle().contains("Mount Karuulm"));
		assertTrue(karuulmDetail.getLines().stream().anyMatch(l -> l.contains("Level 29 Agility")));

		// Dense Essence Mine West Rocks
		final PoiIndex.Poi essenceRocks = new PoiIndex.Poi(1743, 3853, 0, "agility_short-cut", "Agility short-cut");
		final PoiDetails.Detail essenceDetail = PoiDetails.getDetail(essenceRocks, 1743, 3853, 0);
		assertNotNull(essenceDetail);
		assertTrue(essenceDetail.getTitle().contains("Dense Essence Mine"));
		assertTrue(essenceDetail.getLines().stream().anyMatch(l -> l.contains("Level 73 Agility")));

		// River Lum Broken Raft
		final PoiIndex.Poi brokenRaft = new PoiIndex.Poi(3246, 3179, 0, "agility_short-cut", "Agility short-cut");
		final PoiDetails.Detail raftDetail = PoiDetails.getDetail(brokenRaft, 3246, 3179, 0);
		assertNotNull(raftDetail);
		assertTrue(raftDetail.getTitle().contains("Broken Raft"));
		assertTrue(raftDetail.getLines().stream().anyMatch(l -> l.contains("Level 48 Agility") || l.contains("Level 8")));

		// Draynor underwall tunnel
		final PoiIndex.Poi draynorTunnel = new PoiIndex.Poi(3069, 3261, 0, "agility_short-cut", "Agility short-cut");
		final PoiDetails.Detail draynorDetail = PoiDetails.getDetail(draynorTunnel, 3069, 3261, 0);
		assertNotNull(draynorDetail);
		assertTrue(draynorDetail.getTitle().contains("Draynor"));
		assertTrue(draynorDetail.getLines().stream().anyMatch(l -> l.contains("Level 42 Agility")));

		// Generic uncurated shortcut fallback
		final PoiIndex.Poi genericShortcut = new PoiIndex.Poi(1000, 1000, 0, "agility_short-cut", "Agility short-cut");
		final PoiDetails.Detail genericDetail = PoiDetails.getDetail(genericShortcut, 1000, 1000, 0);
		assertNotNull(genericDetail);
		assertEquals("Agility shortcuts", genericDetail.getCategory());
		assertTrue(genericDetail.getLines().stream().anyMatch(l -> l.contains("Agility skill guide")));
	}

	@Test
	public void sailingHunterSpotsResolveWithLevelAndMethodByProximityAndExact()
	{
		// Great Conch Red Chinchompas (exact curated coords)
		final PoiIndex.Poi conchChins = new PoiIndex.Poi(3258, 2378, 0, "hunter_training", "Hunter training");
		final PoiDetails.Detail ccDetail = PoiDetails.getDetail(conchChins, 3258, 2378, 0);
		assertNotNull(ccDetail);
		assertTrue(ccDetail.getTitle().contains("Red Chinchompas"));
		assertEquals("Skilling • Hunter", ccDetail.getCategory());
		assertTrue(ccDetail.getLines().stream().anyMatch(l -> l.contains("Level 63 Hunter")));
		assertTrue(ccDetail.getLines().stream().anyMatch(l -> l.contains("Box trap")));
		assertTrue(ccDetail.getLines().stream().anyMatch(l -> l.contains("The Great Conch")));

		// Great Conch Blue Crabs (exact curated coords)
		final PoiIndex.Poi blueCrabs = new PoiIndex.Poi(3213, 2516, 0, "hunter_training", "Hunter training");
		final PoiDetails.Detail bcDetail = PoiDetails.getDetail(blueCrabs, 3213, 2516, 0);
		assertNotNull(bcDetail);
		assertTrue(bcDetail.getTitle().contains("Blue Crabs"));
		assertEquals("Skilling • Hunter", bcDetail.getCategory());
		assertTrue(bcDetail.getLines().stream().anyMatch(l -> l.contains("Level 48 Hunter")));
		assertTrue(bcDetail.getLines().stream().anyMatch(l -> l.toLowerCase().contains("crab trap")));

		// Great Conch Orange Salamanders (proximity match near 3122, 2532)
		final PoiIndex.Poi orangeSalamanders = new PoiIndex.Poi(3120, 2530, 0, "hunter_training", "Hunter training");
		final PoiDetails.Detail osDetail = PoiDetails.getDetail(orangeSalamanders, 3120, 2530, 0);
		assertNotNull(osDetail);
		assertTrue(osDetail.getTitle().contains("Orange Salamanders"));
		assertEquals("Skilling • Hunter", osDetail.getCategory());
		assertTrue(osDetail.getLines().stream().anyMatch(l -> l.contains("Level 47 Hunter")));
		assertTrue(osDetail.getLines().stream().anyMatch(l -> l.contains("Net trap")));

		// Crown Jewel Rainbow Crabs (exact coords)
		final PoiIndex.Poi rainbowCrabs = new PoiIndex.Poi(1758, 2665, 0, "hunter_training", "Hunter training");
		final PoiDetails.Detail rcDetail = PoiDetails.getDetail(rainbowCrabs, 1758, 2665, 0);
		assertNotNull(rcDetail);
		assertTrue(rcDetail.getTitle().contains("Rainbow Crabs"));
		assertTrue(rcDetail.getLines().stream().anyMatch(l -> l.contains("Level 77 Hunter")));
		assertTrue(rcDetail.getLines().stream().anyMatch(l -> l.toLowerCase().contains("crab trap")));
	}

	@Test
	public void getAllPoisContainsMultipleAgilityAndHunterTrainingKeys()
	{
		final List<PoiIndex.Poi> pois = PoiDetails.getAllPois();
		assertNotNull(pois);

		final long agilityCount = pois.stream().filter(p -> "agility_training".equals(p.getKey())).count();
		final long hunterCount = pois.stream().filter(p -> "hunter_training".equals(p.getKey())).count();
		final long shortcutCount = pois.stream().filter(p -> "agility_short-cut".equals(p.getKey())).count();

		assertTrue("Agility training count was " + agilityCount + " (expected >= 20)", agilityCount >= 20);
		assertTrue("Hunter count was " + hunterCount + " (expected >= 30)", hunterCount >= 30);
		assertTrue("Agility shortcut count was " + shortcutCount + " (expected >= 30)", shortcutCount >= 30);
	}

	@Test
	public void sailingIslandPoisResolveWithUsefulCards()
	{
		// Ynysdail Cavern (dungeon)
		final PoiIndex.Poi ynysdailDungeon = new PoiIndex.Poi(2223, 3466, 0, "dungeon", "Ynysdail Cavern");
		final PoiDetails.Detail ydDetail = PoiDetails.getDetail(ynysdailDungeon, 2223, 3466, 0);
		assertNotNull(ydDetail);
		assertEquals("Dungeons", ydDetail.getCategory());
		assertTrue(ydDetail.getTitle().contains("Ynysdail Cavern"));
		assertTrue(ydDetail.getLines().stream().anyMatch(l -> l.contains("Aquanites") || l.contains("Slayer")));

		// Grimstone mining site
		final PoiIndex.Poi grimstoneMining = new PoiIndex.Poi(2927, 4057, 0, "mining_site", "Grimstone - Mining Site");
		final PoiDetails.Detail gmDetail = PoiDetails.getDetail(grimstoneMining, 2927, 4057, 0);
		assertNotNull(gmDetail);
		assertEquals("Skilling • Mining", gmDetail.getCategory());
		assertTrue(gmDetail.getTitle().contains("Grimstone"));
		assertTrue(gmDetail.getLines().stream().anyMatch(l -> l.contains("Runite") || l.contains("Pickaxe")));

		// Sunbleak Ironwood trees
		final PoiIndex.Poi ironwood = new PoiIndex.Poi(2191, 2327, 0, "rare_trees", "Sunbleak Island - Ironwood trees (Level 80)");
		final PoiDetails.Detail iwDetail = PoiDetails.getDetail(ironwood, 2191, 2327, 0);
		assertNotNull(iwDetail);
		assertEquals("Skilling • Woodcutting", iwDetail.getCategory());
		assertTrue(iwDetail.getTitle().contains("Ironwood"));
		assertTrue(iwDetail.getLines().stream().anyMatch(l -> l.contains("Level 80 Woodcutting")));

		// Crown Jewel tarnished pirate chest (Skilling • Thieving)
		final PoiIndex.Poi crownJewelChest = new PoiIndex.Poi(1766, 2659, 0, "thieving", "The Crown Jewel - Tarnished Pirate Chest (Level 54)");
		final PoiDetails.Detail cjDetail = PoiDetails.getDetail(crownJewelChest, 1766, 2659, 0);
		assertNotNull(cjDetail);
		assertEquals("Skilling • Thieving", cjDetail.getCategory());
		assertTrue(cjDetail.getTitle().contains("Tarnished Pirate Chest"));
		assertTrue(cjDetail.getLines().stream().anyMatch(l -> l.contains("Level 54 Thieving")));

		// Great Conch coral nurseries
		final PoiIndex.Poi coralNurseries = new PoiIndex.Poi(3175, 2367, 0, "farming_patch", "The Great Conch - Coral Nurseries (Level 28+)");
		final PoiDetails.Detail cnDetail = PoiDetails.getDetail(coralNurseries, 3175, 2367, 0);
		assertNotNull(cnDetail);
		assertEquals("Skilling • Farming", cnDetail.getCategory());
		assertTrue(cnDetail.getTitle().contains("Coral Nurseries"));
		assertTrue(cnDetail.getLines().stream().anyMatch(l -> l.contains("Level 28+ Farming") || l.contains("Elkhorn")));

		// Charred Island Dungeon
		final PoiIndex.Poi charredDungeon = new PoiIndex.Poi(2661, 2395, 0, "dungeon", "Charred Island Dungeon");
		final PoiDetails.Detail cdDetail = PoiDetails.getDetail(charredDungeon, 2661, 2395, 0);
		assertNotNull(cdDetail);
		assertEquals("Dungeons", cdDetail.getCategory());
		assertTrue(cdDetail.getTitle().contains("Charred Island Dungeon"));
		assertTrue(cdDetail.getLines().stream().anyMatch(l -> l.contains("Lava Strykewyrms") || l.contains("Slayer")));
	}
}
