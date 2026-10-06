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

import java.awt.image.BufferedImage;
import net.runelite.api.ItemID;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import atlasofgielinor.data.BossLocationData;
import atlasofgielinor.map.MonsterIconManager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MonsterIconManagerTest
{
	private MonsterIconManager iconManager;

	@BeforeClass
	public static void loadBossCatalog()
	{
		assertTrue("Monster icon mappings must load before testing", MonsterIconManager.load());
		BossLocationData.load();
		assertTrue("Boss catalog must load before testing", BossLocationData.isLoaded());
	}

	@Before
	public void setUp()
	{
		iconManager = new MonsterIconManager();
	}

	@Test
	public void exactBossesResolveCorrectly()
	{
		assertEquals(ItemID.KBD_HEADS, iconManager.resolveItemId("King Black Dragon", 276, 0));
		assertEquals(ItemID.PET_SNAKELING, iconManager.resolveItemId("Zulrah", 725, 0));
		assertEquals(ItemID.VORKI, iconManager.resolveItemId("Vorkath", 732, 0));
		assertEquals(ItemID.PET_GENERAL_GRAARDOR, iconManager.resolveItemId("General Graardor", 624, 0));
		assertEquals(ItemID.PET_KREEARRA, iconManager.resolveItemId("Kree'arra", 580, 0));
		assertEquals(ItemID.PET_ZILYANA, iconManager.resolveItemId("Commander Zilyana", 596, 0));
		assertEquals(ItemID.PET_KRIL_TSUTSAROTH, iconManager.resolveItemId("K'ril Tsutsaroth", 650, 0));
		assertEquals(ItemID.PET_DARK_CORE, iconManager.resolveItemId("Corporeal Beast", 785, 0));
		assertEquals(ItemID.PET_DAGANNOTH_PRIME, iconManager.resolveItemId("Dagannoth Prime", 303, 0));
		assertEquals(ItemID.ABYSSAL_ORPHAN, iconManager.resolveItemId("Abyssal Sire", 350, 85));
		assertEquals(ItemID.HELLPUPPY, iconManager.resolveItemId("Cerberus", 318, 91));
		assertEquals(ItemID.IKKLE_HYDRA, iconManager.resolveItemId("Alchemical Hydra", 426, 95));
		assertEquals(ItemID.BABY_MOLE, iconManager.resolveItemId("Giant Mole", 230, 0));
		assertEquals(ItemID.KQ_HEAD, iconManager.resolveItemId("Kalphite Queen", 333, 0));
		assertEquals(ItemID.TZREKJAD, iconManager.resolveItemId("TzTok-Jad", 702, 0));
		assertEquals(ItemID.TZREKZUK, iconManager.resolveItemId("TzKal-Zuk", 1400, 0));
		assertEquals(ItemID.OLMLET, iconManager.resolveItemId("Great Olm", 1043, 0));
		assertEquals(ItemID.LIL_ZIK, iconManager.resolveItemId("Verzik Vitur", 1040, 0));
		assertEquals(ItemID.TUMEKENS_GUARDIAN, iconManager.resolveItemId("Tumeken's Warden", 544, 0));
		assertEquals(ItemID.PET_CHAOS_ELEMENTAL, iconManager.resolveItemId("Chaos Elemental", 305, 0));
		assertEquals(ItemID.SRARACHA, iconManager.resolveItemId("Sarachnis", 318, 0));
		assertEquals(ItemID.SKOTOS, iconManager.resolveItemId("Skotizo", 321, 0));
		assertEquals(ItemID.TINY_TEMPOR, iconManager.resolveItemId("Tempoross", 0, 0));
		assertEquals(ItemID.YOUNGLLEF, iconManager.resolveItemId("Crystalline Hunllef", 674, 0));
		assertEquals(ItemID.CORRUPTED_YOUNGLLEF, iconManager.resolveItemId("Corrupted Hunllef", 894, 0));
	}

	@Test
	public void allMonsterLocationDataBossesResolveToValidItemIds()
	{
		for (BossLocationData boss : BossLocationData.values())
		{
			final int itemId = iconManager.resolveItemId(boss.getName(), boss.getCombatLevel(), 0);
			assertTrue("Boss " + boss.getName() + " should resolve to a positive ItemID, got " + itemId, itemId > 0);
			assertNotEquals("Boss " + boss.getName() + " should not fall back to generic SKULL",
				ItemID.SKULL, itemId);
			assertNotEquals("Boss " + boss.getName() + " should not fall back to generic BONES",
				ItemID.BONES, itemId);
		}
	}

	@Test
	public void slayerMonstersResolveToAuthenticItems()
	{
		assertEquals(net.runelite.api.gameval.ItemID.SLAYER_NOSEPEG,
			iconManager.resolveItemId("Aberrant spectre", 96, 60));
		assertEquals(ItemID.ABYSSAL_WHIP, iconManager.resolveItemId("Abyssal demon", 124, 85));
		assertEquals(ItemID.BLOOD_RUNE, iconManager.resolveItemId("Bloodveld", 76, 50));
		assertEquals(ItemID.ROCK_HAMMER, iconManager.resolveItemId("Gargoyle", 111, 75));
		assertEquals(ItemID.BOOTS_OF_LIGHTNESS, iconManager.resolveItemId("Nechryael", 115, 80));
		assertEquals(ItemID.DARK_BOW, iconManager.resolveItemId("Dark beast", 182, 90));
		assertEquals(ItemID.BLACK_MASK, iconManager.resolveItemId("Cave horror", 80, 58));
		assertEquals(ItemID.LEAFBLADED_SWORD, iconManager.resolveItemId("Kurask", 106, 70));
		assertEquals(ItemID.MIRROR_SHIELD, iconManager.resolveItemId("Basilisk", 61, 40));
		assertEquals(ItemID.EARMUFFS, iconManager.resolveItemId("Banshee", 23, 15));
		assertEquals(ItemID.CRAWLING_HAND, iconManager.resolveItemId("Crawling hand", 8, 5));
		assertEquals(ItemID.TRIDENT_OF_THE_SEAS, iconManager.resolveItemId("Cave kraken", 127, 87));
		assertEquals(ItemID.FACEMASK, iconManager.resolveItemId("Smoke devil", 160, 93));
		assertEquals(ItemID.DRAKES_CLAW, iconManager.resolveItemId("Drake", 192, 84));
		assertEquals(ItemID.WYRM_BONES, iconManager.resolveItemId("Wyrm", 99, 62));
		assertEquals(ItemID.HYDRAS_EYE, iconManager.resolveItemId("Hydra", 194, 95));
		assertEquals(ItemID.WYVERN_VISAGE, iconManager.resolveItemId("Skeletal Wyvern", 140, 72));
	}

	@Test
	public void dragonsAndDemonsResolveAppropriately()
	{
		assertEquals(ItemID.BLUE_DRAGONHIDE, iconManager.resolveItemId("Blue dragon", 111, 0));
		assertEquals(ItemID.GREEN_DRAGONHIDE, iconManager.resolveItemId("Green dragon", 79, 0));
		assertEquals(ItemID.RED_DRAGONHIDE, iconManager.resolveItemId("Red dragon", 152, 0));
		assertEquals(ItemID.BLACK_DRAGONHIDE, iconManager.resolveItemId("Black dragon", 227, 0));
		assertEquals(ItemID.RUNITE_BAR, iconManager.resolveItemId("Rune dragon", 380, 0));
		assertEquals(ItemID.ADAMANTITE_BAR, iconManager.resolveItemId("Adamant dragon", 338, 0));
		assertEquals(ItemID.MITHRIL_BAR, iconManager.resolveItemId("Mithril dragon", 304, 0));
		assertEquals(ItemID.ASHES, iconManager.resolveItemId("Lesser demon", 82, 0));
		assertEquals(ItemID.INFERNAL_ASHES, iconManager.resolveItemId("Greater demon", 92, 0));
		assertEquals(ItemID.INFERNAL_ASHES, iconManager.resolveItemId("Black demon", 172, 0));
		assertEquals(ItemID.ZENYTE_SHARD, iconManager.resolveItemId("Demonic gorilla", 275, 0));
	}

	@Test
	public void fallbacksHandleUnknownMonstersGracefully()
	{
		// Unknown monster with slayer level -> Slayer Helmet
		assertEquals(ItemID.SLAYER_HELMET, iconManager.resolveItemId("Mysterious Slayer Beast", 90, 77));

		// Unknown high level boss -> Skull
		assertEquals(ItemID.SKULL, iconManager.resolveItemId("Unknown Epic Megaboss", 250, 0));

		// Unknown generic low level mob -> Bones
		assertEquals(ItemID.BONES, iconManager.resolveItemId("Random Strange Creature", 20, 0));

		// Null and empty names
		assertEquals(ItemID.BONES, iconManager.resolveItemId(null, 10, 0));
		assertEquals(ItemID.SLAYER_HELMET, iconManager.resolveItemId("", 10, 50));
	}

	@Test
	public void iconGenerationReturnsValidBufferedImages()
	{
		final BufferedImage bossIcon = iconManager.getBossIcon(BossLocationData.KING_BLACK_DRAGON, 18);
		assertNotNull(bossIcon);
		assertEquals(18, bossIcon.getWidth());
		assertEquals(18, bossIcon.getHeight());

		final BufferedImage zoneIcon = iconManager.getIcon("Aberrant spectre", 96, 60, 20);
		assertNotNull(zoneIcon);
		assertEquals(20, zoneIcon.getWidth());
		assertEquals(20, zoneIcon.getHeight());

		final BufferedImage genericIcon = iconManager.getIcon("Custom Monster", 40, 0, 24);
		assertNotNull(genericIcon);
		assertEquals(24, genericIcon.getWidth());
		assertEquals(24, genericIcon.getHeight());
	}
}
