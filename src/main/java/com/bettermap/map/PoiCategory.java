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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Groups location icons into something a settings panel can switch on and off.
 *
 * <p>The wiki tags every icon {@code category: "others"}, so the grouping is ours. It is defined
 * by explicit key sets plus a few suffix rules. Unidentified native sprites use {@link #OTHER}.
 */
public enum PoiCategory
{
	BANKS("Banks"),
	SHOPS("Shops and trade"),
	SKILLING("Skilling"),
	SHORTCUTS("Agility shortcuts"),
	SAILING("Sailing"),
	TRAVEL("Travel"),
	QUESTS("Quests and activities"),
	ALTARS("Altars"),
	DUNGEONS("Dungeons"),
	SERVICES("Tutors and services"),
	PLACES("Places"),
	OTHER("Other");

	private static final Set<String> BANK_KEYS = keys("bank");

	private static final Set<String> TRAVEL_KEYS = keys(
		"transportation", "dungeon_link", "house_portal", "mooring_point",
		"canoe_station", "hot_air_balloon", "magic_mushtree", "magic_carpet", "minecart_network",
		"sea_current");

	private static final Set<String> ALTAR_KEYS = keys("altar");

	private static final Set<String> DUNGEON_KEYS = keys("dungeon", "basement");

	private static final Set<String> QUEST_KEYS = keys(
		"quest_start", "kourend_task", "task_master", "minigame", "raids_lobby", "holiday_event");

	private static final Set<String> SKILLING_KEYS = keys(
		"fishing_spot", "mining_site", "rare_trees", "cooking_range", "water_source", "farming_patch",
		"anvil", "furnace", "spinning_wheel", "potters_wheel", "windmill", "dairy_churn", "dairy_cow",
		"loom", "tannery", "sawmill", "woodcutting_stump", "dummy", "agility_training",
		"stagnant_water_source", "stonemason",
		"hunter_training", "brewery", "sandpit", "thieving", "singing_bowl", "slayer_master");

	private static final Set<String> SHOP_KEYS = keys(
		"general_store", "bar", "apothecary", "estate_agent", "grand_exchange", "junk_checker",
		"taxidermist", "jewellery", "key", "herbalist", "kebab_seller", "garden_supplier",
		"vegetable_store", "pet_insurance_shop", "bounty_hunter_store", "silk_trader", "fur_trader",
		"shipwright");

	private static final Set<String> SERVICE_KEYS = keys(
		"lumbridge_guide", "makeover_mage", "hairdresser", "poll_booth", "cargo_bay", "noticeboard");

	private static final Set<String> PLACE_KEYS = keys("region_label");

	private final String displayName;

	PoiCategory(String displayName)
	{
		this.displayName = displayName;
	}

	public String getDisplayName()
	{
		return displayName;
	}

	public static PoiCategory of(String key)
	{
		if (key == null)
		{
			return OTHER;
		}

		if ("agility_short-cut".equals(key))
		{
			return SHORTCUTS;
		}
		if ("lookout_point".equals(key))
		{
			return SAILING;
		}

		if (BANK_KEYS.contains(key))
		{
			return BANKS;
		}
		if (TRAVEL_KEYS.contains(key))
		{
			return TRAVEL;
		}
		if (ALTAR_KEYS.contains(key))
		{
			return ALTARS;
		}
		if (DUNGEON_KEYS.contains(key))
		{
			return DUNGEONS;
		}
		if (QUEST_KEYS.contains(key))
		{
			return QUESTS;
		}
		if (SKILLING_KEYS.contains(key))
		{
			return SKILLING;
		}
		if (key.endsWith("_tutor") || SERVICE_KEYS.contains(key))
		{
			return SERVICES;
		}
		if (PLACE_KEYS.contains(key))
		{
			return PLACES;
		}
		if (key.endsWith("_shop") || key.endsWith("_trader") || key.endsWith("_seller")
			|| key.endsWith("_store") || key.startsWith("pricing_expert") || SHOP_KEYS.contains(key))
		{
			return SHOPS;
		}

		return OTHER;
	}

	private static Set<String> keys(String... values)
	{
		return new HashSet<>(Arrays.asList(values));
	}
}
