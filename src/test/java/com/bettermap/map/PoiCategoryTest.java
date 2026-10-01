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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import org.junit.Test;

/**
 * The wiki tags every location icon {@code category: "others"}, so Better Map defines the
 * grouping the settings panel switches on and off. These are the icon keys the shipped
 * dataset actually contains; every one of them must land in a real group, or a settings toggle
 * would silently fail to control part of the map.
 */
public class PoiCategoryTest
{
	private static final String[] ALL_KEYS = {
		"agility_short-cut", "agility_training", "altar", "amulet_shop",
		"anvil", "apothecary", "archery_shop", "axe_shop",
		"bank", "bank_tutor", "bar", "bond_tutor",
		"bounty_hunter_store", "brewery", "candle_shop", "canoe_station", "cargo_bay", "chainmail_shop",
		"clothes_shop", "combat_tutor", "cookery_shop", "cooking_range",
		"cooking_tutor", "crafting_shop", "crafting_tutor", "dairy_churn",
		"dairy_cow", "danger_tutor", "deadman_tutor", "dummy",
		"dungeon", "dungeon_link", "basement", "dye_trader", "estate_agent",
		"farming_patch", "farming_shop", "fishing_shop", "fishing_spot",
		"fishing_tutor", "food_shop", "fur_trader", "furnace",
		"garden_supplier", "gem_shop", "general_store", "grand_exchange",
		"hairdresser", "helmet_shop", "herbalist", "holiday_event",
		"holiday_item_trader", "hot_air_balloon", "house_portal", "hunter_shop", "hunter_training",
		"hunting_tutor", "ironman_tutor", "jewellery", "junk_checker",
		"kebab_seller", "kourend_task", "lookout_point", "loom", "lumbridge_guide",
		"mace_shop", "magic_carpet", "magic_mushtree", "magic_shop", "makeover_mage", "minecart_network", "minigame",
		"mining_shop", "mining_site", "mining_tutor", "mooring_point", "newspaper_trader",
		"noticeboard", "pet_insurance_shop", "platebody_shop", "platelegs_shop", "plateskirt_shop",
		"poll_booth", "potters_wheel", "prayer_tutor", "pricing_expert_herbs",
		"pricing_expert_logs", "pricing_expert_ores", "pricing_expert_runes", "pricing_expert_weapons_and_armours",
		"quest_start", "raids_lobby", "rare_trees", "rope_trader",
		"sandpit", "sawmill", "scimitar_shop", "sea_current", "security_tutor",
		"shield_shop", "shipwright", "silk_trader", "silver_shop", "singing_bowl", "slayer_master",
		"smithing_tutor", "spice_shop", "spinning_wheel", "staff_shop", "stagnant_water_source", "stonemason",
		"sword_shop", "tannery", "task_master", "taxidermist",
		"tea_trader", "thieving", "transportation", "vegetable_store", "water_source",
		"windmill", "wine_trader", "woodcutting_stump", "woodcutting_tutor"
	};

	@Test
	public void everyShippedIconKeyIsClassified()
	{
		for (String key : ALL_KEYS)
		{
			assertNotEquals("unclassified icon key: " + key, PoiCategory.OTHER, PoiCategory.of(key));
		}
	}

	@Test
	public void keysLandInTheExpectedGroups()
	{
		assertEquals(PoiCategory.BANKS, PoiCategory.of("bank"));
		assertEquals(PoiCategory.TRAVEL, PoiCategory.of("transportation"));
		assertEquals(PoiCategory.SHORTCUTS, PoiCategory.of("agility_short-cut"));
		assertEquals(PoiCategory.TRAVEL, PoiCategory.of("sea_current"));
		assertEquals(PoiCategory.SKILLING, PoiCategory.of("fishing_spot"));
		assertEquals(PoiCategory.SKILLING, PoiCategory.of("mining_site"));
		assertEquals(PoiCategory.SKILLING, PoiCategory.of("thieving"));
		assertEquals(PoiCategory.ALTARS, PoiCategory.of("altar"));
		assertEquals(PoiCategory.QUESTS, PoiCategory.of("quest_start"));
		assertEquals(PoiCategory.SKILLING, PoiCategory.of("slayer_master"));
		assertEquals(PoiCategory.DUNGEONS, PoiCategory.of("basement"));
		assertEquals(PoiCategory.PLACES, PoiCategory.of("region_label"));
		assertEquals(PoiCategory.SAILING, PoiCategory.of("lookout_point"));
		assertEquals(PoiCategory.SERVICES, PoiCategory.of("cargo_bay"));
		assertEquals(PoiCategory.SERVICES, PoiCategory.of("noticeboard"));
		assertEquals(PoiCategory.SHOPS, PoiCategory.of("shipwright"));
	}

	@Test
	public void suffixRulesCatchTheLongTailOfShopsAndTutors()
	{
		// 53 of the 122 keys are shops; naming them all individually would rot.
		assertEquals(PoiCategory.SHOPS, PoiCategory.of("clothes_shop"));
		assertEquals(PoiCategory.SHOPS, PoiCategory.of("scimitar_shop"));
		assertEquals(PoiCategory.SHOPS, PoiCategory.of("wine_trader"));
		assertEquals(PoiCategory.SHOPS, PoiCategory.of("kebab_seller"));
		assertEquals(PoiCategory.SHOPS, PoiCategory.of("pricing_expert_runes"));
		assertEquals(PoiCategory.SERVICES, PoiCategory.of("combat_tutor"));
		assertEquals(PoiCategory.SERVICES, PoiCategory.of("poll_booth"));
	}

	@Test
	public void anUnknownKeyFallsBackToOther()
	{
		assertEquals(PoiCategory.OTHER, PoiCategory.of("something_the_wiki_added_later"));
		assertEquals(PoiCategory.OTHER, PoiCategory.of(null));
	}
}
