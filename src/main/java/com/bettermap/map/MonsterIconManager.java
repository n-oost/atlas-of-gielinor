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

import com.bettermap.data.MonsterLocationData;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.game.ItemManager;

/**
 * Picks an item sprite to stand in for a monster, slayer creature or boss.
 *
 * <p>The sprites come from RuneLite's {@link ItemManager} - heads, trophies, pets, slayer
 * equipment, signature drops and materials. Each one is scaled once and cached, because the map
 * overlay, the hover cards and the sidebar all redraw them every frame.
 */
@Slf4j
@Singleton
public class MonsterIconManager
{
	private final ItemManager itemManager;

	/** Direct name-to-item mapping (case-insensitive). */
	private static final Map<String, Integer> EXACT_MAP = new HashMap<>();

	/** Keyword/substring-to-item mapping for creatures sharing species roots. */
	private static final Map<String, Integer> KEYWORD_MAP = new HashMap<>();

	/** In-memory cache of rendered/scaled monster icons keyed by itemId + size. */
	private final Map<Long, BufferedImage> scaledIconCache = new ConcurrentHashMap<>();

	/** In-memory fallback icon cache for offline/testing mode. */
	private final Map<String, BufferedImage> fallbackIconCache = new ConcurrentHashMap<>();

	@Inject
	public MonsterIconManager(ItemManager itemManager)
	{
		this.itemManager = itemManager;
	}

	/** Constructor for tests or environments without dependency injection. */
	public MonsterIconManager()
	{
		this(null);
	}

	static
	{
		buildExactBossMappings();
		buildExactSlayerMappings();
		buildExactCreatureMappings();
		buildKeywordMappings();
	}

	/**
	 * Returns a scaled BufferedImage icon for the given monster zone, or a fallback icon if
	 * RuneLite item graphics are not available.
	 */
	public BufferedImage getZoneIcon(MonsterIndex.Zone zone, int targetSize)
	{
		if (zone == null)
		{
			return getFallbackIcon("Monster", targetSize, new Color(224, 72, 72));
		}

		return getIcon(zone.getMonster(), zone.getCombatLevel(), zone.getSlayerLevel(), targetSize);
	}

	/**
	 * Returns a scaled BufferedImage icon for a hard-coded boss from {@link MonsterLocationData}.
	 */
	public BufferedImage getBossIcon(MonsterLocationData boss, int targetSize)
	{
		if (boss == null)
		{
			return getFallbackIcon("Boss", targetSize, new Color(255, 209, 102));
		}

		return getIcon(boss.getName(), boss.getCombatLevel(), 0, targetSize);
	}

	/**
	 * Scaled sprite for a known item id, sharing this class's icon cache.
	 *
	 * <p>Unlike the monster icons there is nothing to resolve: ground item spawns carry real item
	 * ids, so the map draws the actual item. Returns null when the sprite is unavailable — the
	 * caller draws its own marker rather than a wrong item.
	 */
	public BufferedImage getItemIcon(int itemId, int targetSize)
	{
		if (itemId <= 0 || itemManager == null)
		{
			return null;
		}

		final long cacheKey = (((long) itemId) << 32) | ((long) targetSize & 0xFFFFFFFFL);
		final BufferedImage cached = scaledIconCache.get(cacheKey);
		if (cached != null)
		{
			return cached;
		}

		try
		{
			final BufferedImage rawImage = itemManager.getImage(itemId);
			if (rawImage != null)
			{
				final BufferedImage scaled = scaleImage(rawImage, targetSize);
				scaledIconCache.put(cacheKey, scaled);
				return scaled;
			}
		}
		catch (Exception e)
		{
			log.debug("Could not load item image for item ID {}: {}", itemId, e.getMessage());
		}

		return null;
	}

	/**
	 * Resolves and returns a scaled icon for the specified monster by name, combat level,
	 * and slayer level.
	 */
	public BufferedImage getIcon(String monsterName, int combatLevel, int slayerLevel, int targetSize)
	{
		final int itemId = resolveItemId(monsterName, combatLevel, slayerLevel);
		if (itemId > 0 && itemManager != null)
		{
			final long cacheKey = (((long) itemId) << 32) | ((long) targetSize & 0xFFFFFFFFL);
			final BufferedImage cached = scaledIconCache.get(cacheKey);
			if (cached != null)
			{
				return cached;
			}

			try
			{
				final BufferedImage rawImage = itemManager.getImage(itemId);
				if (rawImage != null)
				{
					final BufferedImage scaled = scaleImage(rawImage, targetSize);
					scaledIconCache.put(cacheKey, scaled);
					return scaled;
				}
			}
			catch (Exception e)
			{
				log.debug("Could not load item image for item ID {}: {}", itemId, e.getMessage());
			}
		}

		// Fallback generated icon when itemManager is unavailable or offline
		final Color theme = slayerLevel > 1 ? new Color(180, 90, 220) : (combatLevel >= 150 ? new Color(255, 209, 102) : new Color(224, 72, 72));
		return getFallbackIcon(monsterName != null ? monsterName : "Monster", targetSize, theme);
	}

	/**
	 * The item id that best stands in for this monster name.
	 */
	public int resolveItemId(String monsterName, int combatLevel, int slayerLevel)
	{
		if (monsterName == null || monsterName.trim().isEmpty())
		{
			return slayerLevel > 1 ? ItemID.SLAYER_HELM : (combatLevel >= 150 ? ItemID.SKULL : ItemID.BONES);
		}

		final String cleanName = normalizeName(monsterName);

		// 1. Exact Name Match
		final Integer exact = EXACT_MAP.get(cleanName);
		if (exact != null)
		{
			return exact;
		}

		// 2. Keyword / Substring Matching
		for (Map.Entry<String, Integer> entry : KEYWORD_MAP.entrySet())
		{
			if (cleanName.contains(entry.getKey()))
			{
				return entry.getValue();
			}
		}

		// 3. Slayer Requirement Fallback
		if (slayerLevel > 1)
		{
			return ItemID.SLAYER_HELM;
		}

		// 4. High-Level Boss / Combat Fallback
		if (combatLevel >= 150)
		{
			return ItemID.SKULL;
		}

		// 5. Default Creature Fallback
		return ItemID.BONES;
	}

	private static String normalizeName(String name)
	{
		return name.toLowerCase(Locale.ROOT)
			.replace("'", "")
			.replace("-", " ")
			.trim();
	}

	private static BufferedImage scaleImage(BufferedImage source, int targetSize)
	{
		if (source.getWidth() == targetSize && source.getHeight() == targetSize)
		{
			return source;
		}

		final BufferedImage scaled = new BufferedImage(targetSize, targetSize, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = scaled.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		final int srcW = source.getWidth();
		final int srcH = source.getHeight();
		final double scale = Math.min((double) targetSize / srcW, (double) targetSize / srcH);
		final int drawW = (int) Math.round(srcW * scale);
		final int drawH = (int) Math.round(srcH * scale);
		final int drawX = (targetSize - drawW) / 2;
		final int drawY = (targetSize - drawH) / 2;

		g.drawImage(source, drawX, drawY, drawW, drawH, null);
		g.dispose();
		return scaled;
	}

	private BufferedImage getFallbackIcon(String name, int targetSize, Color color)
	{
		final String cacheKey = name + "_" + targetSize + "_" + color.getRGB();
		BufferedImage cached = fallbackIconCache.get(cacheKey);
		if (cached != null)
		{
			return cached;
		}

		final BufferedImage img = new BufferedImage(targetSize, targetSize, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		// Badge background
		g.setColor(new Color(16, 19, 26, 220));
		g.fillOval(1, 1, targetSize - 2, targetSize - 2);

		// Border
		g.setColor(color);
		g.drawOval(1, 1, targetSize - 2, targetSize - 2);

		// Letter initial
		final String letter = name.length() > 0 ? name.substring(0, 1).toUpperCase(Locale.ROOT) : "M";
		g.setFont(new Font("SansSerif", Font.BOLD, Math.max(9, targetSize / 2)));
		final int textW = g.getFontMetrics().stringWidth(letter);
		final int textH = g.getFontMetrics().getAscent() - 2;
		g.drawString(letter, (targetSize - textW) / 2, (targetSize + textH) / 2);
		g.dispose();

		fallbackIconCache.put(cacheKey, img);
		return img;
	}

	private static void buildExactBossMappings()
	{
		// Major Wilderness & World Bosses
		mapExact("king black dragon", ItemID.POH_TROPHYDROP_KBD);
		mapExact("kbd", ItemID.POH_TROPHYDROP_KBD);
		mapExact("zulrah", ItemID.SNAKEPET);
		mapExact("vorkath", ItemID.CERT_SKINPASTE);
		mapExact("general graardor", ItemID.BANDOSPET);
		mapExact("bandos", ItemID.BANDOSPET);
		mapExact("kree'arra", ItemID.ARMADYLPET);
		mapExact("kreearra", ItemID.ARMADYLPET);
		mapExact("armadyl", ItemID.ARMADYLPET);
		mapExact("commander zilyana", ItemID.SARADOMINPET);
		mapExact("saradomin", ItemID.SARADOMINPET);
		mapExact("k'ril tsutsaroth", ItemID.ZAMORAKPET);
		mapExact("kril tsutsaroth", ItemID.ZAMORAKPET);
		mapExact("zamorak", ItemID.ZAMORAKPET);
		mapExact("corporeal beast", ItemID.ELYSIAN_SIGIL);
		mapExact("corp", ItemID.ELYSIAN_SIGIL);

		// Dagannoth Kings
		mapExact("dagannoth prime", ItemID.PRIMEPET);
		mapExact("dagannoth rex", ItemID.REXPET);
		mapExact("dagannoth supreme", ItemID.SUPREMEPET);
		mapExact("dagannoth kings", ItemID.PRIMEPET);

		// Slayer Bosses
		mapExact("abyssal sire", ItemID.ABYSSALSIRE_PET);
		mapExact("cerberus", ItemID.HELL_PET);
		mapExact("alchemical hydra", ItemID.HYDRAPET);
		mapExact("kraken", ItemID.KRAKENPET);
		mapExact("cave kraken", ItemID.TOTS_CHARGED);
		mapExact("thermonuclear smoke devil", ItemID.SMOKEPET);
		mapExact("grotesque guardians", ItemID.DAWNPET);
		mapExact("dusk", ItemID.DAWNPET);
		mapExact("dawn", ItemID.DUSKPET);

		// Demi-Bosses & Wilderness
		mapExact("giant mole", ItemID.MOLEPET);
		mapExact("kalphite queen", ItemID.POH_TROPHYDROP_KALPHITEQUEEN);
		mapExact("chaos fanatic", ItemID.ODIUM_SHARD1);
		mapExact("crazy archaeologist", ItemID.MALEDICTION_SHARD2);
		mapExact("deranged archaeologist", ItemID.MALEDICTION_SHARD2);
		mapExact("scorpia", ItemID.SCORPIA_PET);
		mapExact("venenatis", ItemID.VENENATIS_PET);
		mapExact("spindel", ItemID.VENENATIS_PET);
		mapExact("callisto", ItemID.CALLISTO_PET);
		mapExact("artio", ItemID.CALLISTO_PET);
		mapExact("vet'ion", ItemID.VETION_PET);
		mapExact("vetion", ItemID.VETION_PET);
		mapExact("calvar'ion", ItemID.VETION_PET);
		mapExact("calvarion", ItemID.VETION_PET);
		mapExact("sarachnis", ItemID.SARACHNIS_CUDGEL);
		mapExact("skotizo", ItemID.CATA_TOTEM);
		mapExact("hespori", ItemID.BOTTOMLESS_COMPOST_BUCKET);
		mapExact("obor", ItemID.HILLGIANT_BOSS_CLUB);
		mapExact("bryophyta", ItemID.NATURE_STAFF_CHARGED);
		mapExact("scurrius", ItemID.SCURRIUSPET);

		// Desert Treasure II & Modern Bosses
		mapExact("duke sucellus", ItemID.DUKESUCELLUSPET);
		mapExact("the leviathan", ItemID.LEVIATHANPET);
		mapExact("leviathan", ItemID.LEVIATHANPET);
		mapExact("the whisperer", ItemID.WHISPERERPET);
		mapExact("whisperer", ItemID.WHISPERERPET);
		mapExact("vardorvis", ItemID.VARDORVISPET);
		mapExact("phantom muspah", ItemID.MUSPAHPET);
		mapExact("nex", ItemID.NEXPET);
		mapExact("the nightmare", ItemID.NIGHTMAREPET);
		mapExact("nightmare of ashihama", ItemID.NIGHTMAREPET);
		mapExact("phosani's nightmare", ItemID.NIGHTMAREPET);
		mapExact("phosanis nightmare", ItemID.NIGHTMAREPET);

		// Barrows Brothers
		mapExact("ahrim the blighted", ItemID.BARROWS_AHRIM_HEAD);
		mapExact("dharok the wretched", ItemID.BARROWS_DHAROK_HEAD);
		mapExact("guthan the infested", ItemID.BARROWS_GUTHAN_HEAD);
		mapExact("karil the tainted", ItemID.BARROWS_KARIL_HEAD);
		mapExact("torag the corrupted", ItemID.BARROWS_TORAG_HEAD);
		mapExact("verac the defiled", ItemID.BARROWS_VERAC_HEAD);

		// Minigames & Raids
		mapExact("tztok jad", ItemID.JAD_PET);
		mapExact("jad", ItemID.JAD_PET);
		mapExact("tzkal zuk", ItemID.INFERNOPET_ZUK);
		mapExact("zuk", ItemID.INFERNOPET_ZUK);
		mapExact("great olm", ItemID.OLMPET);
		mapExact("olm", ItemID.OLMPET);
		mapExact("verzik vitur", ItemID.VERZIKPET);
		mapExact("verzik", ItemID.VERZIKPET);
		mapExact("tumeken's warden", ItemID.WARDENPET_TUMEKEN);
		mapExact("tumekens warden", ItemID.WARDENPET_TUMEKEN);
		mapExact("warden", ItemID.WARDENPET_TUMEKEN);
	}

	private static void buildExactSlayerMappings()
	{
		// Tower & Dungeon Slayer Monsters
		mapExact("aberrant spectre", ItemID.SLAYER_NOSEPEG); // nosepeg
		mapExact("abhorrent spectre", ItemID.SLAYER_NOSEPEG);
		mapExact("abyssal demon", ItemID.ABYSSAL_WHIP);
		mapExact("greater abyssal demon", ItemID.ABYSSAL_WHIP);
		mapExact("banshee", ItemID.SLAYER_EARMUFFS);
		mapExact("screaming banshee", ItemID.SLAYER_EARMUFFS);
		mapExact("twisted banshee", ItemID.SLAYER_EARMUFFS);
		mapExact("basilisk", ItemID.SLAYER_MIRROR_SHIELD);
		mapExact("basilisk knight", ItemID.BASILISK_JAW);
		mapExact("bloodveld", ItemID.BLOODRUNE);
		mapExact("mutated bloodveld", ItemID.BLOODRUNE);
		mapExact("insatiable bloodveld", ItemID.BLOODRUNE);
		mapExact("insatiable mutated bloodveld", ItemID.BLOODRUNE);
		mapExact("brine rat", ItemID.OLAF2_BRINE_SABRE);
		mapExact("cave bug", ItemID.DORGESH_SWAMP_WEED);
		mapExact("cave crawler", ItemID.ASHESVIAL);
		mapExact("chasm crawler", ItemID.ASHESVIAL);
		mapExact("cave horror", ItemID.HARMLESS_BLACK_MASK);
		mapExact("cave abomination", ItemID.HARMLESS_BLACK_MASK);
		mapExact("cave slime", ItemID.OIL_LANTERN_UNLIT);
		mapExact("crawling hand", ItemID.SLAYERGUIDE_CRAWLINGHAND);
		mapExact("crushing hand", ItemID.SLAYERGUIDE_CRAWLINGHAND);
		mapExact("dark beast", ItemID.DARKBOW);
		mapExact("desert lizard", ItemID.SLAYER_ICY_WATER);
		mapExact("small lizard", ItemID.SLAYER_ICY_WATER);
		mapExact("lizard", ItemID.SLAYER_ICY_WATER);
		mapExact("drake", ItemID.DRAKE_CLAW);
		mapExact("dust devil", ItemID.CERT_NEWBIESHRIMP);
		mapExact("choke devil", ItemID.CERT_NEWBIESHRIMP);
		mapExact("fever spider", ItemID.SLAYERGUIDE_SLAYER_GLOVES);
		mapExact("gargoyle", ItemID.SLAYER_ROCK_HAMMER);
		mapExact("marble gargoyle", ItemID.SLAYER_ROCK_HAMMER);
		mapExact("harpie bug swarm", ItemID.SLAYER_BUGLAN_ON);
		mapExact("hellhound", ItemID.SMOULDERING_STONE);
		mapExact("hydra", ItemID.HYDRA_EYE);
		mapExact("infernal mage", ItemID.LAVA_BATTLESTAFF);
		mapExact("malevolent mage", ItemID.LAVA_BATTLESTAFF);
		mapExact("jelly", ItemID.SLAYERGUIDE_JELLY);
		mapExact("warped jelly", ItemID.SLAYERGUIDE_JELLY);
		mapExact("killerwatt", ItemID.SLAYER_BOOTS);
		mapExact("kurask", ItemID.LEAFBLADED_SWORD);
		mapExact("king kurask", ItemID.POH_TROPHYDROP_KURASK);
		mapExact("lizardman", ItemID.DRAGON_WARHAMMER);
		mapExact("lizardman brute", ItemID.DRAGON_WARHAMMER);
		mapExact("lizardman shaman", ItemID.DRAGON_WARHAMMER);
		mapExact("mogre", ItemID.MUDSKIPPER_FLIPPERS);
		mapExact("molanisk", ItemID.MOLANISK_BELL);
		mapExact("mutated zygomite", ItemID.SLAYER_SPRAY_PUMP_10);
		mapExact("ancient zygomite", ItemID.SLAYER_SPRAY_PUMP_10);
		mapExact("nechryael", ItemID.IKOV_BOOTSOFLIGHTNESS);
		mapExact("greater nechryael", ItemID.RUNE_ARMOURED_BOOTS);
		mapExact("nechryarch", ItemID.RUNE_ARMOURED_BOOTS);
		mapExact("pyrefiend", ItemID.FIRERUNE);
		mapExact("flaming pyrelord", ItemID.FIRERUNE);
		mapExact("sea snake", ItemID.VILLAGE_SNAKE_SKIN);
		mapExact("skeletal wyvern", ItemID.WYVERN_VISAGE);
		mapExact("smoke devil", ItemID.SLAYER_FACEMASK);
		mapExact("nuclear smoke devil", ItemID.SLAYER_FACEMASK);
		mapExact("sourhog", ItemID.WALLBEAST_SPIKE_HELMET);
		mapExact("spiritual mage", ItemID.DRAGON_BOOTS);
		mapExact("spiritual ranger", ItemID.RANGER_TUNIC);
		mapExact("spiritual warrior", ItemID.RUNE_ARMOURED_BOOTS);
		mapExact("suqah", ItemID.SUQKA_TOOTH);
		mapExact("terror dog", ItemID.BONES);
		mapExact("turoth", ItemID.SLAYER_LEAFBLADED_SPEAR);
		mapExact("mightiest turoth", ItemID.SLAYER_LEAFBLADED_SPEAR);
		mapExact("wall beast", ItemID.WALLBEAST_SPIKE_HELMET);
		mapExact("wyrm", ItemID.WYRM_BONES);
		mapExact("shadow wyrm", ItemID.WYRM_BONES);
		mapExact("spitting wyvern", ItemID.WYVERN_VISAGE);
		mapExact("taloned wyvern", ItemID.WYVERN_VISAGE);
		mapExact("fossil island wyvern", ItemID.WYVERN_VISAGE);
	}

	private static void buildExactCreatureMappings()
	{
		// Dragons
		mapExact("baby blue dragon", ItemID.DRAGONHIDE_BLUE);
		mapExact("blue dragon", ItemID.DRAGONHIDE_BLUE);
		mapExact("brutal blue dragon", ItemID.DRAGONHIDE_BLUE);
		mapExact("baby red dragon", ItemID.DRAGONHIDE_RED);
		mapExact("red dragon", ItemID.DRAGONHIDE_RED);
		mapExact("brutal red dragon", ItemID.DRAGONHIDE_RED);
		mapExact("baby green dragon", ItemID.DRAGONHIDE_GREEN);
		mapExact("green dragon", ItemID.DRAGONHIDE_GREEN);
		mapExact("brutal green dragon", ItemID.DRAGONHIDE_GREEN);
		mapExact("baby black dragon", ItemID.DRAGONHIDE_BLACK);
		mapExact("black dragon", ItemID.DRAGONHIDE_BLACK);
		mapExact("brutal black dragon", ItemID.DRAGONHIDE_BLACK);
		mapExact("bronze dragon", ItemID.BRONZE_BAR);
		mapExact("iron dragon", ItemID.IRON_BAR);
		mapExact("steel dragon", ItemID.STEEL_BAR);
		mapExact("mithril dragon", ItemID.MITHRIL_BAR);
		mapExact("adamant dragon", ItemID.ADAMANTITE_BAR);
		mapExact("rune dragon", ItemID.RUNITE_BAR);
		mapExact("lava dragon", ItemID.LAVA_SCALE);
		mapExact("elvarg", ItemID.DRAGONHIDE_GREEN);

		// Demons
		mapExact("imp", ItemID.BLACK_BEAD);
		mapExact("lesser demon", ItemID.ASHES);
		mapExact("greater demon", ItemID.INFERNAL_ASHES);
		mapExact("black demon", ItemID.INFERNAL_ASHES);
		mapExact("demonic gorilla", ItemID.ZENYTE_SHARD);

		// Giants & Cyclopes
		mapExact("hill giant", ItemID.HILLGIANT_BOSS_KEY);
		mapExact("moss giant", ItemID.MOSSY_KEY);
		mapExact("fire giant", ItemID.FIRERUNE);
		mapExact("ice giant", ItemID.ICE_ARROW);
		mapExact("cyclops", ItemID.DRAGON_PARRYINGDAGGER);
		mapExact("cyclopes", ItemID.DRAGON_PARRYINGDAGGER);

		// Undead & Ghosts
		mapExact("ankou", ItemID.SKULL);
		mapExact("skeleton", ItemID.SKULL);
		mapExact("skeleton mage", ItemID.SKULL);
		mapExact("zombie", ItemID.TRICK_OR_TREAT_HEAD);
		mapExact("ghost", ItemID.SECRET_GHOST_TOP);
		mapExact("spectre", ItemID.SECRET_GHOST_TOP);
		mapExact("wraith", ItemID.SECRET_GHOST_TOP);
		mapExact("mummy", ItemID.SKULL);
		mapExact("vampyre", ItemID.VAMPIRE_DUST);
		mapExact("vampire", ItemID.VAMPIRE_DUST);
		mapExact("shade", ItemID.BLACKROBEBOTTOM);
		mapExact("ghoul", ItemID.RAG_GHOUL_BONE);
		mapExact("dagannoth", ItemID.DAGANNOTH_KING_BONES);

		// Beasts & Animals
		mapExact("spider", ItemID.RED_SPIDERS_EGGS);
		mapExact("deadly red spider", ItemID.RED_SPIDERS_EGGS);
		mapExact("poison spider", ItemID.RED_SPIDERS_EGGS);
		mapExact("giant spider", ItemID.VILLAGE_SPIDER_CARCASS);
		mapExact("scorpion", ItemID.POISON_CHALICE);
		mapExact("king scorpion", ItemID.POISON_CHALICE);
		mapExact("pit scorpion", ItemID.POISON_CHALICE);
		mapExact("snake", ItemID.VILLAGE_SNAKE_SKIN);
		mapExact("giant snake", ItemID.VILLAGE_SNAKE_SKIN);
		mapExact("wolf", ItemID.WOLF_BONES);
		mapExact("white wolf", ItemID.WOLF_BONES);
		mapExact("bear", ItemID.FUR);
		mapExact("black bear", ItemID.FUR);
		mapExact("grizzly bear", ItemID.FUR);
		mapExact("crocodile", ItemID.RAW_BEEF);
		mapExact("monkey", ItemID.RAG_MONKEY_BONE);
		mapExact("gorilla", ItemID.RAG_MONKEY_BONE);
		mapExact("bat", ItemID.RAG_BAT_BONE);
		mapExact("giant bat", ItemID.RAG_BAT_BONE);
		mapExact("rat", ItemID.RAW_RAT_MEAT);
		mapExact("giant rat", ItemID.RAW_RAT_MEAT);
		mapExact("guard dog", ItemID.BONES);
		mapExact("wild dog", ItemID.BONES);
		mapExact("cow", ItemID.RAW_BEEF);
		mapExact("dairy cow", ItemID.RAW_BEEF);
		mapExact("chicken", ItemID.FEATHER);
		mapExact("rooster", ItemID.FEATHER);
		mapExact("duck", ItemID.FEATHER);
		mapExact("sheep", ItemID.WOOL);
		mapExact("ram", ItemID.WOOL);
		mapExact("goat", ItemID.GROUND_DESERT_GOAT_HORN);
		mapExact("mountain goat", ItemID.GROUND_DESERT_GOAT_HORN);
		mapExact("kalphite worker", ItemID.CACTUS_POTATO);
		mapExact("kalphite soldier", ItemID.CACTUS_POTATO);
		mapExact("kalphite guardian", ItemID.CACTUS_POTATO);
		mapExact("scarab", ItemID.NTK_SCARAB_GOLD);
		mapExact("locust", ItemID.LOCUST_MEAT);

		// Humanoids & Guards
		mapExact("guard", ItemID.STEEL_FULL_HELM);
		mapExact("knight", ItemID.RUNE_FULL_HELM);
		mapExact("paladin", ItemID.RUNE_FULL_HELM);
		mapExact("hero", ItemID.RUNE_FULL_HELM);
		mapExact("wizard", ItemID.BLACKWIZHAT);
		mapExact("dark wizard", ItemID.BLACKWIZHAT);
		mapExact("chaos druid", ItemID.UNIDENTIFIED_RANARR);
		mapExact("pirate", ItemID.PIRATE_BOOTS);
		mapExact("thug", ItemID.LOCKPICK);
		mapExact("bandit", ItemID.LOCKPICK);
		mapExact("highwayman", ItemID.LOCKPICK);
		mapExact("elf", ItemID.PRIF_CRYSTAL_SHARD);
		mapExact("iorwerth warrior", ItemID.PRIF_CRYSTAL_SHARD);
		mapExact("dwarf", ItemID.GRIM_WEAR_HELMET);
		mapExact("black guard", ItemID.GRIM_WEAR_HELMET);
		mapExact("barbarian", ItemID.BEER);
		mapExact("goblin", ItemID.GOBLIN_ARMOUR);
		mapExact("hobgoblin", ItemID.GOBLIN_ARMOUR);
		mapExact("ogre", ItemID.EMPTY_OGRE_BELLOWS);
		mapExact("jogre", ItemID.EMPTY_OGRE_BELLOWS);
		mapExact("zogre", ItemID.EMPTY_OGRE_BELLOWS);
		mapExact("troll", ItemID.SLAYER_ROCK_HAMMER);
	}

	private static void buildKeywordMappings()
	{
		KEYWORD_MAP.put("dragon", ItemID.DRAGON_BONES);
		KEYWORD_MAP.put("wyvern", ItemID.WYVERN_VISAGE);
		KEYWORD_MAP.put("demon", ItemID.INFERNAL_ASHES);
		KEYWORD_MAP.put("gorilla", ItemID.ZENYTE_SHARD);
		KEYWORD_MAP.put("spectre", ItemID.SLAYER_HELM);
		KEYWORD_MAP.put("banshee", ItemID.SLAYER_EARMUFFS);
		KEYWORD_MAP.put("basilisk", ItemID.SLAYER_MIRROR_SHIELD);
		KEYWORD_MAP.put("bloodveld", ItemID.SLAYER_HELM);
		KEYWORD_MAP.put("gargoyle", ItemID.SLAYER_ROCK_HAMMER);
		KEYWORD_MAP.put("nechryael", ItemID.IKOV_BOOTSOFLIGHTNESS);
		KEYWORD_MAP.put("kurask", ItemID.LEAFBLADED_SWORD);
		KEYWORD_MAP.put("turoth", ItemID.SLAYER_LEAFBLADED_SPEAR);
		KEYWORD_MAP.put("hydra", ItemID.HYDRA_EYE);
		KEYWORD_MAP.put("drake", ItemID.DRAKE_CLAW);
		KEYWORD_MAP.put("wyrm", ItemID.WYRM_BONES);
		KEYWORD_MAP.put("giant", ItemID.BIG_BONES);
		KEYWORD_MAP.put("cyclop", ItemID.DRAGON_PARRYINGDAGGER);
		KEYWORD_MAP.put("dagannoth", ItemID.DAGANNOTH_KING_BONES);
		KEYWORD_MAP.put("kalphite", ItemID.CACTUS_POTATO);
		KEYWORD_MAP.put("spider", ItemID.RED_SPIDERS_EGGS);
		KEYWORD_MAP.put("scorpion", ItemID.POISON_CHALICE);
		KEYWORD_MAP.put("snake", ItemID.VILLAGE_SNAKE_SKIN);
		KEYWORD_MAP.put("wolf", ItemID.WOLF_BONES);
		KEYWORD_MAP.put("bear", ItemID.FUR);
		KEYWORD_MAP.put("monkey", ItemID.RAG_MONKEY_BONE);
		KEYWORD_MAP.put("bat", ItemID.RAG_BAT_BONE);
		KEYWORD_MAP.put("rat", ItemID.RAW_RAT_MEAT);
		KEYWORD_MAP.put("cow", ItemID.RAW_BEEF);
		KEYWORD_MAP.put("skeleton", ItemID.SKULL);
		KEYWORD_MAP.put("zombie", ItemID.TRICK_OR_TREAT_HEAD);
		KEYWORD_MAP.put("ghost", ItemID.SECRET_GHOST_TOP);
		KEYWORD_MAP.put("mummy", ItemID.SKULL);
		KEYWORD_MAP.put("vamp", ItemID.VAMPIRE_DUST);
		KEYWORD_MAP.put("shade", ItemID.BLACKROBEBOTTOM);
		KEYWORD_MAP.put("guard", ItemID.STEEL_FULL_HELM);
		KEYWORD_MAP.put("knight", ItemID.RUNE_FULL_HELM);
		KEYWORD_MAP.put("paladin", ItemID.RUNE_FULL_HELM);
		KEYWORD_MAP.put("wizard", ItemID.BLACKWIZHAT);
		KEYWORD_MAP.put("mage", ItemID.BLACKWIZHAT);
		KEYWORD_MAP.put("druid", ItemID.UNIDENTIFIED_RANARR);
		KEYWORD_MAP.put("pirate", ItemID.PIRATE_BOOTS);
		KEYWORD_MAP.put("bandit", ItemID.LOCKPICK);
		KEYWORD_MAP.put("thug", ItemID.LOCKPICK);
		KEYWORD_MAP.put("elf", ItemID.PRIF_CRYSTAL_SHARD);
		KEYWORD_MAP.put("dwarf", ItemID.GRIM_WEAR_HELMET);
		KEYWORD_MAP.put("goblin", ItemID.GOBLIN_ARMOUR);
		KEYWORD_MAP.put("ogre", ItemID.EMPTY_OGRE_BELLOWS);
		KEYWORD_MAP.put("troll", ItemID.SLAYER_ROCK_HAMMER);
		KEYWORD_MAP.put("lizard", ItemID.SLAYER_ICY_WATER);
		KEYWORD_MAP.put("jelly", ItemID.SLAYERGUIDE_JELLY);
		KEYWORD_MAP.put("hellhound", ItemID.SMOULDERING_STONE);
		KEYWORD_MAP.put("dog", ItemID.BONES);
		KEYWORD_MAP.put("suqah", ItemID.SUQKA_TOOTH);
		KEYWORD_MAP.put("ankou", ItemID.SKULL);
		KEYWORD_MAP.put("avian", ItemID.ADAMANTITE_BAR);
	}

	private static void mapExact(String name, int itemId)
	{
		EXACT_MAP.put(normalizeName(name), itemId);
	}
}
