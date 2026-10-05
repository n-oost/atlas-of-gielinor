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
package com.bettermap.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.coords.WorldPoint;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * The major underground zones and dungeons, each pairing a surface entrance with the coordinates
 * of its interior. The mapping runs both ways, which is what lets the map peek at a dungeon on
 * hover and swap to it on click.
 */
@Slf4j
public enum UndergroundZone
{
	LUMBRIDGE_CELLAR,
	VARROCK_SEWERS,
	TAVERLEY_DUNGEON,
	DWARVEN_MINES,
	EDGEVILLE_DUNGEON,
	CATACOMBS_OF_KOUREND,
	BRIMHAVEN_DUNGEON,
	FREMENNIK_SLAYER_DUNGEON,
	ASGARNIA_ICE_CAVE,
	STRONGHOLD_OF_SECURITY,
	KELDAGRIM,
	ARDOUGNE_UNDERGROUND,
	WITCHAVEN_DUNGEON,
	CHAOS_DRUID_TOWER,
	CLOCK_TOWER_DUNGEON,
	ELEMENTAL_WORKSHOP,
	GLARIALS_TOMB,
	GOBLIN_CAVE,
	TEMPLE_OF_IKOV,
	LEGENDS_GUILD_DUNGEON,
	KRAKEN_COVE,
	MISCELLANIA_UNDERGROUND,
	FORTHOS_DUNGEON,
	CAM_TORUM,
	TZHAAR_CITY,
	GOD_WARS_DUNGEON,
	WATERBIRTH_DUNGEON,
	FOSSIL_ISLAND_UNDERGROUND,
	ZANARIS,
	ANCIENT_CAVERN,
	MOLE_HOLE,
	OURANIA_ALTAR,
	SMOKE_DEVIL_DUNGEON,
	OBSERVATORY_DUNGEON,
	TOWER_OF_LIFE_BASEMENT,
	TREE_GNOME_VILLAGE_DUNGEON,
	UNDERGROUND_PASS,
	CAMDOZAAL,
	CORPOREAL_BEAST,
	HALLOWED_SEPULCHRE,
	DORGESH_KAAN,
	DORGESH_KAAN_SOUTH,
	ROGUES_DEN,
	ABYSS,
	PYRAMID_PLUNDER,
	BARBARIAN_ASSAULT,
	TARN_LAIR,
	PRIFDDINAS_GRAND_LIBRARY,
	PRIFDDINAS_UNDERGROUND,
	SMOKE_DUNGEON,
	KLENTERS_PYRAMID,
	DESERT_EAGLE_LAIR,
	SOPHANEM_DUNGEON,
	REVENANT_CAVES,
	WILDERNESS_SLAYER_CAVE,
	WILDERNESS_GOD_WARS_DUNGEON,
	DEEP_WILDERNESS_DUNGEON,
	MAGE_ARENA_BANK,
	CHASM_OF_FIRE,
	THE_WARRENS,
	KARUULM_SLAYER_DUNGEON,
	WOODCUTTING_GUILD_DUNGEON,
	NEYPOTZLI,
	TONALI_CAVERN,
	DRAGON_NEST,
	LIGHTHOUSE_DUNGEON,
	TOMBS_OF_AMASCUT,
	LIZARDMAN_CAVES,
	LIZARDMAN_TEMPLE,
	GIANTS_DEN,
	FORTIS_BARRACKS,
	FENKENSTRAIN_DUNGEON,
	WILDERNESS_AGILITY_DUNGEON,
	DAEYALT_ESSENCE_MINE,
	MOLCH_PEARL_CAVES,
	KALPHITE_LAIR,
	KALPHITE_CAVE,
	GIANTS_FOUNDRY,
	APE_ATOLL_DUNGEON,
	WIZARDS_GUILD_BASEMENT,
	YANILLE_AGILITY_DUNGEON,
	MOS_LE_HARMLESS_CAVE,
	KARAMJA_DUNGEON,
	CRANDOR_DUNGEON,
	LUMBRIDGE_SWAMP_CAVES,
	CRABCLAW_CAVES,
	LAVA_MAZE_DUNGEON,
	FEROX_ENCLAVE_DUNGEON,
	WEB_CHASM,
	SCORPIA_CAVE,
	SILK_CHASM,
	ESCAPE_CAVES,
	CALLISTOS_DEN,
	STRONGHOLD_SLAYER_DUNGEON,
	BRIMSTAIL_CAVE,
	GRAND_TREE_TUNNELS,
	HEROES_GUILD_MINE,
	HAM_HIDEOUT,
	DRAYNOR_SEWERS,
	ICE_QUEEN_LAIR,
	CORSAIR_COVE_DUNGEON,
	ISLE_OF_SOULS_DUNGEON,
	WIZARDS_TOWER_BASEMENT,
	TOLNAS_RIFT,
	SOURHOG_CAVE,
	PATERDOMUS_BASEMENT,
	WATER_MILL_CELLAR,
	CHAMPIONS_GUILD_BASEMENT,
	VARROCK_MUSEUM_BASEMENT,
	VARROCK_RAT_PITS,
	VTAM_CORPORATION,
	SORCERESS_GARDEN,
	DIGSITE_DUNGEON,
	CHASM_OF_TEARS,
	MINING_GUILD,
	MELZARS_MAZE_BASEMENT,
	MOTHERLODE_MINE,
	SCRUBFOOT_CAVE,
	WARRIORS_GUILD_BASEMENT,
	WATERFALL_DUNGEON,
	EAGLES_PEAK_DUNGEON,
	OGRE_ENCLAVE,
	JIGGIG_DUNGEON,
	RED_CHINCHOMPA_HUNTING_GROUND,
	JUNGLE_EAGLE_LAIR,
	SKAVID_CAVES,
	SHILO_VILLAGE_GEM_MINE,
	RASHILIYIAS_TOMB,
	CRASH_ISLAND_DUNGEON,
	KHARAZI_CAVES,
	JATIZSO_MINE,
	ICE_TROLL_CAVES,
	LUNAR_ISLE_MINE,
	TROLL_STRONGHOLD,
	HAUNTED_MINE,
	BARROWS_CRYPTS,
	MEIYERDITCH_LABORATORIES,
	NATURE_GROTTO,
	ECTOFUNTUS_DUNGEON,
	MORYTANIA_SPIDER_CAVE,
	SHADE_CATACOMBS,
	VETIONS_REST,
	ZEMOUREGALS_BASE,
	WHITE_KNIGHTS_CASTLE_CRYPT,
	WHITE_WOLF_TUNNEL,
	POISON_WASTE_DUNGEON,
	SISTERHOOD_SANCTUARY,
	JORMUNGANDS_PRISON,
	GHORROCK_DUNGEON,
	HUNTER_GUILD,
	LITHKREN_VAULT,
	WEISS_SALT_MINE,
	JALDRAOCHT_PYRAMID,
	RIVER_ELID_DUNGEON,
	SHADOW_DUNGEON,
	DRAYNOR_MANOR_BASEMENT,
	ENAKHRAS_TEMPLE,
	PORT_SARIM_RAT_PITS,
	KING_BLACK_DRAGON_LAIR,
	HESPORI_CAVE,
	STALKER_DEN,
	WYRMSCRAIG_CAVERN,
	DEEPFIN_MINE,
	PANDEMONIUM_CAVE,
	HERE_BE_MINOTAURS,
	YNYSDAIL_CAVERN,
	GRYPHON_CAVE,
	KURASK_LAIR,
	SHELLBANE_GRYPHON_CAVE,
	SUNBLEAK_CAVE,
	JOGRE_DUNGEON,
	SCABARAS_DUNGEON,
	WEREWOLF_AGILITY_COURSE,
	MYREQUE_HIDEOUT,
	SHAYZIEN_PRISON,
	XERICS_LOOKOUT_BASEMENT,
	FORTIS_COLOSSEUM,
	ENTRANA_DUNGEON,
	ALCHEMICAL_SOCIETY,
	QUIDAMORTEM_CAVE,
	RUINS_OF_UZER_BASEMENT,
	TUTORIAL_ISLAND_DUNGEON,
	POLAR_EAGLE_LAIR,
	RANTZS_CAVE,
	HARMONY_ISLAND_BASEMENT,
	PENGUIN_BASE,
	TUNNEL_OF_CHAOS,
	UNGAEL_LABORATORY,
	GENIE_CAVE,
	BLACK_KNIGHT_CATACOMBS,
	ARTIOS_LAIR,
	SPINDELS_LAIR,
	CHARRED_DUNGEON,
	FARMING_GUILD_SEED_VAULT,
	DESERT_MINING_CAMP_DUNGEON,
	CRASH_SITE_CAVERN,
	LIBRARY_HISTORICAL_ARCHIVE,
	BRINE_RAT_CAVERN,
	MONKEY_MADNESS_HANGAR,
	CORAL_NURSERIES,
	AIR_ALTAR,
	WATER_ALTAR,
	EARTH_ALTAR,
	FIRE_ALTAR,
	BODY_ALTAR,
	LAW_ALTAR,
	NATURE_ALTAR,
	CHAOS_ALTAR,
	ENCHANTED_VALLEY,
	MOGRE_CAMP,
	KENDALS_LAIR,
	NATIVE_ANCIENT_CAVERN,
	NATIVE_ANCIENT_CAVERN_ANCIENT_CAVERN,
	NATIVE_ARDOUGNE_UNDERGROUND,
	NATIVE_ARDOUGNE_UNDERGROUND_ARDOUGNE_UNDERGROUND,
	NATIVE_ARDOUGNE_UNDERGROUND_CHAOS_DRUID_TOWER,
	NATIVE_ARDOUGNE_UNDERGROUND_CLOCK_TOWER_DUNGEON,
	NATIVE_ARDOUGNE_UNDERGROUND_GOBLIN_CAVE,
	NATIVE_ARDOUGNE_UNDERGROUND_LEGENDS_GUILD_DUNGEON,
	NATIVE_ASGARNIA_ICE_DUNGEON,
	NATIVE_ASGARNIA_ICE_DUNGEON_ASGARNIA_ICE_CAVE,
	NATIVE_ASGARNIA_ICE_DUNGEON_KARAMJA_DUNGEON,
	NATIVE_ASGARNIA_ICE_DUNGEON_MELZARS_MAZE_BASEMENT,
	NATIVE_ASGARNIA_ICE_DUNGEON_PORT_SARIM_RAT_PITS,
	NATIVE_BRAINDEATH_ISLAND,
	NATIVE_DORGESHKAAN,
	NATIVE_DORGESHKAAN_DORGESH_KAAN,
	NATIVE_DWARVEN_MINES,
	NATIVE_DWARVEN_MINES_MINING_GUILD,
	NATIVE_DWARVEN_MINES_TAVERLEY_DUNGEON,
	NATIVE_GODWARS,
	NATIVE_GODWARS_GOD_WARS_DUNGEON,
	NATIVE_KELDAGRIM,
	NATIVE_KELDAGRIM_KELDAGRIM,
	NATIVE_MISTHALIN_UNDERGROUND,
	NATIVE_MISTHALIN_UNDERGROUND_ASGARNIA_ICE_CAVE,
	NATIVE_MISTHALIN_UNDERGROUND_CHAMPIONS_GUILD_BASEMENT,
	NATIVE_MISTHALIN_UNDERGROUND_CHASM_OF_TEARS,
	NATIVE_MISTHALIN_UNDERGROUND_DRAYNOR_MANOR_BASEMENT,
	NATIVE_MISTHALIN_UNDERGROUND_DRAYNOR_SEWERS,
	NATIVE_MISTHALIN_UNDERGROUND_HAM_HIDEOUT,
	NATIVE_MISTHALIN_UNDERGROUND_LUMBRIDGE_CELLAR,
	NATIVE_MISTHALIN_UNDERGROUND_LUMBRIDGE_SWAMP_CAVES,
	NATIVE_MISTHALIN_UNDERGROUND_SOURHOG_CAVE,
	NATIVE_MISTHALIN_UNDERGROUND_VARROCK_SEWERS,
	NATIVE_MISTHALIN_UNDERGROUND_VTAM_CORPORATION,
	NATIVE_MISTHALIN_UNDERGROUND_WATER_MILL_CELLAR,
	NATIVE_MISTHALIN_UNDERGROUND_WIZARDS_TOWER_BASEMENT,
	NATIVE_MOLE,
	NATIVE_MOLE_MOLE_HOLE,
	NATIVE_MORYTANIA_UNDERGROUND,
	NATIVE_MORYTANIA_UNDERGROUND_ECTOFUNTUS_DUNGEON,
	NATIVE_MORYTANIA_UNDERGROUND_FENKENSTRAIN_DUNGEON,
	NATIVE_MORYTANIA_UNDERGROUND_HAUNTED_MINE,
	NATIVE_MORYTANIA_UNDERGROUND_MEIYERDITCH_LABORATORIES,
	NATIVE_MORYTANIA_UNDERGROUND_MORYTANIA_SPIDER_CAVE,
	NATIVE_MORYTANIA_UNDERGROUND_MYREQUE_HIDEOUT,
	NATIVE_MORYTANIA_UNDERGROUND_PATERDOMUS_BASEMENT,
	NATIVE_MORYTANIA_UNDERGROUND_SHADE_CATACOMBS,
	NATIVE_MORYTANIA_UNDERGROUND_SISTERHOOD_SANCTUARY,
	NATIVE_MORYTANIA_UNDERGROUND_WEREWOLF_AGILITY_COURSE,
	NATIVE_MOSLEHARMLESS_CAVE,
	NATIVE_MOSLEHARMLESS_CAVE_MOS_LE_HARMLESS_CAVE,
	NATIVE_OURANIA,
	NATIVE_OURANIA_OURANIA_ALTAR,
	NATIVE_SLAYER_CAVE,
	NATIVE_SLAYER_CAVE_FREMENNIK_SLAYER_DUNGEON,
	NATIVE_SOS,
	NATIVE_SOS_STRONGHOLD_OF_SECURITY,
	NATIVE_STRONGHOLD_UNDERGROUND,
	NATIVE_STRONGHOLD_UNDERGROUND_BRIMSTAIL_CAVE,
	NATIVE_STRONGHOLD_UNDERGROUND_GRAND_TREE_TUNNELS,
	NATIVE_STRONGHOLD_UNDERGROUND_KRAKEN_COVE,
	NATIVE_STRONGHOLD_UNDERGROUND_STRONGHOLD_SLAYER_DUNGEON,
	NATIVE_TAVERLEY_UNDERGROUND,
	NATIVE_TAVERLEY_UNDERGROUND_ENTRANA_DUNGEON,
	NATIVE_TAVERLEY_UNDERGROUND_HEROES_GUILD_MINE,
	NATIVE_TAVERLEY_UNDERGROUND_ICE_QUEEN_LAIR,
	NATIVE_TAVERLEY_UNDERGROUND_MINING_GUILD,
	NATIVE_TAVERLEY_UNDERGROUND_TAVERLEY_DUNGEON,
	NATIVE_TAVERLEY_UNDERGROUND_WARRIORS_GUILD_BASEMENT,
	NATIVE_TAVERLEY_UNDERGROUND_WHITE_KNIGHTS_CASTLE_CRYPT,
	NATIVE_TAVERLEY_UNDERGROUND_WHITE_WOLF_TUNNEL,
	NATIVE_TOLNA,
	NATIVE_TROLL_STRONGHOLD,
	NATIVE_TROLL_STRONGHOLD_KELDAGRIM,
	NATIVE_TZHAAR_AREA,
	NATIVE_TZHAAR_AREA_TZHAAR_CITY,
	NATIVE_UNDEAD_DUNGEON,
	NATIVE_WATERBIRTH,
	NATIVE_WATERBIRTH_WATERBIRTH_DUNGEON,
	NATIVE_WILDERNESS_DUNGEONS,
	NATIVE_WILDERNESS_DUNGEONS_CORPOREAL_BEAST,
	NATIVE_WILDERNESS_DUNGEONS_DEEP_WILDERNESS_DUNGEON,
	NATIVE_WILDERNESS_DUNGEONS_KING_BLACK_DRAGON_LAIR,
	NATIVE_WILDERNESS_DUNGEONS_LAVA_MAZE_DUNGEON,
	NATIVE_WILDERNESS_DUNGEONS_MAGE_ARENA_BANK,
	NATIVE_WILDERNESS_DUNGEONS_REVENANT_CAVES,
	NATIVE_WILDERNESS_DUNGEONS_SCORPIA_CAVE,
	NATIVE_WILDERNESS_DUNGEONS_SILK_CHASM,
	NATIVE_WILDERNESS_DUNGEONS_TROLL_STRONGHOLD,
	NATIVE_WILDERNESS_DUNGEONS_WEB_CHASM,
	NATIVE_WILDERNESS_DUNGEONS_WILDERNESS_GOD_WARS_DUNGEON,
	NATIVE_WILDERNESS_DUNGEONS_WILDERNESS_SLAYER_CAVE,
	NATIVE_YANILLE_UNDERGROUND,
	NATIVE_YANILLE_UNDERGROUND_SMOKE_DEVIL_DUNGEON,
	NATIVE_YANILLE_UNDERGROUND_WIZARDS_GUILD_BASEMENT,
	NATIVE_YANILLE_UNDERGROUND_YANILLE_AGILITY_DUNGEON,
	NATIVE_ZANARIS,
	NATIVE_ZANARIS_ZANARIS,
	NATIVE_PRIFDDINAS,
	NATIVE_FOSSIL_UNDERGROUND,
	NATIVE_FOSSIL_UNDERGROUND_FOSSIL_ISLAND_UNDERGROUND,
	NATIVE_FELDIP_UNDERGROUND,
	NATIVE_FELDIP_UNDERGROUND_CORSAIR_COVE_DUNGEON,
	NATIVE_FELDIP_UNDERGROUND_RED_CHINCHOMPA_HUNTING_GROUND,
	NATIVE_KOUREND_UNDERGROUND,
	NATIVE_KOUREND_UNDERGROUND_CATACOMBS_OF_KOUREND,
	NATIVE_KOUREND_UNDERGROUND_CHASM_OF_FIRE,
	NATIVE_KOUREND_UNDERGROUND_CRABCLAW_CAVES,
	NATIVE_KOUREND_UNDERGROUND_FORTHOS_DUNGEON,
	NATIVE_KOUREND_UNDERGROUND_GIANTS_DEN,
	NATIVE_KOUREND_UNDERGROUND_THE_WARRENS,
	NATIVE_KOUREND_UNDERGROUND_WOODCUTTING_GUILD_DUNGEON,
	NATIVE_KEBOS_UNDERGROUND,
	NATIVE_KEBOS_UNDERGROUND_FARMING_GUILD_SEED_VAULT,
	NATIVE_KEBOS_UNDERGROUND_HESPORI_CAVE,
	NATIVE_KEBOS_UNDERGROUND_KARUULM_SLAYER_DUNGEON,
	NATIVE_KEBOS_UNDERGROUND_LIZARDMAN_CAVES,
	NATIVE_PRIFDDINAS_UNDERGROUND,
	NATIVE_PRIFDDINAS_UNDERGROUND_PRIFDDINAS_UNDERGROUND,
	NATIVE_GRAND_LIBRARY,
	NATIVE_BR_DEFAULT,
	NATIVE_BR_DARK_VARROCK,
	NATIVE_CAMDOZAAL,
	NATIVE_CAMDOZAAL_CAMDOZAAL,
	NATIVE_THE_ABYSS,
	NATIVE_THE_ABYSS_ABYSS,
	NATIVE_DESERT_UNDERGROUND,
	NATIVE_DESERT_UNDERGROUND_KALPHITE_CAVE,
	NATIVE_DESERT_UNDERGROUND_KALPHITE_LAIR,
	NATIVE_DESERT_UNDERGROUND_TOMBS_OF_AMASCUT,
	NATIVE_VARLAMORE_UNDERGROUND,
	NATIVE_VARLAMORE_UNDERGROUND_ALCHEMICAL_SOCIETY,
	NATIVE_VARLAMORE_UNDERGROUND_CAM_TORUM,
	NATIVE_VARLAMORE_UNDERGROUND_DRAGON_NEST,
	NATIVE_VARLAMORE_UNDERGROUND_FORTIS_BARRACKS,
	NATIVE_VARLAMORE_UNDERGROUND_FORTIS_COLOSSEUM,
	NATIVE_VARLAMORE_UNDERGROUND_STALKER_DEN,
	NATIVE_CAM_TORUM,
	NATIVE_CAM_TORUM_CAM_TORUM,
	NATIVE_NEYPOTZLI,
	NATIVE_NEYPOTZLI_CAM_TORUM,
	NATIVE_NEYPOTZLI_NEYPOTZLI,
	NATIVE_ARDENT_OCEAN_UNDERGROUND,
	NATIVE_ARDENT_OCEAN_UNDERGROUND_ASGARNIA_ICE_CAVE,
	NATIVE_ARDENT_OCEAN_UNDERGROUND_BRIMHAVEN_DUNGEON,
	NATIVE_ARDENT_OCEAN_UNDERGROUND_JOGRE_DUNGEON,
	NATIVE_ARDENT_OCEAN_UNDERGROUND_KARAMJA_DUNGEON,
	NATIVE_ARDENT_OCEAN_UNDERGROUND_PANDEMONIUM_CAVE,
	NATIVE_ARDENT_OCEAN_UNDERGROUND_SHILO_VILLAGE_GEM_MINE,
	NATIVE_UNQUIET_OCEAN_UNDERGROUND,
	NATIVE_UNQUIET_OCEAN_UNDERGROUND_CHARRED_DUNGEON,
	NATIVE_UNQUIET_OCEAN_UNDERGROUND_GRYPHON_CAVE,
	NATIVE_UNQUIET_OCEAN_UNDERGROUND_SHELLBANE_GRYPHON_CAVE,
	NATIVE_UNQUIET_OCEAN_UNDERGROUND_WYRMSCRAIG_CAVERN,
	NATIVE_SHROUDED_OCEAN_UNDERGROUND,
	NATIVE_SHROUDED_OCEAN_UNDERGROUND_DEEPFIN_MINE,
	NATIVE_SHROUDED_OCEAN_UNDERGROUND_ISLE_OF_SOULS_DUNGEON,
	NATIVE_SHROUDED_OCEAN_UNDERGROUND_SUNBLEAK_CAVE,
	NATIVE_SUNSET_OCEAN_UNDERGROUND,
	NATIVE_SUNSET_OCEAN_UNDERGROUND_KURASK_LAIR,
	NATIVE_WESTERN_OCEAN_UNDERGROUND,
	NATIVE_WESTERN_OCEAN_UNDERGROUND_HERE_BE_MINOTAURS,
	NATIVE_WESTERN_OCEAN_UNDERGROUND_YNYSDAIL_CAVERN,
	NATIVE_NORTHERN_OCEAN_UNDERGROUND,
	NATIVE_NORTHERN_OCEAN_UNDERGROUND_GHORROCK_DUNGEON,
	NATIVE_NORTHERN_OCEAN_UNDERGROUND_JORMUNGANDS_PRISON,
	NATIVE_NORTHERN_OCEAN_UNDERGROUND_MISCELLANIA_UNDERGROUND,
	NATIVE_NORTHERN_OCEAN_UNDERGROUND_PENGUIN_BASE,
	NATIVE_NORTHERN_OCEAN_UNDERGROUND_WEISS_SALT_MINE;

	@AllArgsConstructor
	private static final class Metadata
	{
		private final String id;
		private final String name;
		private final WorldPoint surfacePoint;
		private final List<WorldPoint> surfacePoints;
		private final WorldPoint undergroundPoint;
		private final int yOffset;
		private final int radius;
		private final String description;
		private final int clipMinX;
		private final int clipMinY;
		private final int clipMaxX;
		private final int clipMaxY;
	}

	@AllArgsConstructor
	private static final class Catalog
	{
		private final Map<UndergroundZone, Metadata> metadata;
		private final Map<String, UndergroundZone> byId;
	}

	private static volatile Catalog catalog = new Catalog(Collections.emptyMap(), Collections.emptyMap());
	public static volatile List<UndergroundZone> ALL_ZONES = Collections.emptyList();
	private static final Map<String, String> SHARED_ZONE_IDS = Map.of(
		"edgeville_dungeon", "varrock_sewers", "crandor_dungeon", "karamja_dungeon");

	public static boolean isLoaded()
	{
		return !catalog.metadata.isEmpty();
	}

	/** Called on the startup worker before floor, cluster and instance indexes are built. */
	public static void load()
	{
		if (isLoaded())
		{
			return;
		}
		final Map<UndergroundZone, Metadata> prepared = new EnumMap<>(UndergroundZone.class);
		final Map<String, UndergroundZone> ids = new HashMap<>();
		try
		{
			for (String[] f : BundledTsv.read("/com/bettermap/dungeons/zones.tsv", 16))
			{
				if ((f.length - 16) % 3 != 0) throw new IOException("Invalid dungeon entrances");
				final UndergroundZone zone = valueOf(f[0]);
				final WorldPoint surface = point(f, 3);
				final List<WorldPoint> entrances = new ArrayList<>();
				entrances.add(surface);
				for (int i = 16; i < f.length; i += 3) entrances.add(point(f, i));
				final Metadata metadata = new Metadata(f[1], f[2], surface, Collections.unmodifiableList(entrances),
					point(f, 6), Integer.parseInt(f[9]), Integer.parseInt(f[10]), f[11],
					Integer.parseInt(f[12]), Integer.parseInt(f[13]), Integer.parseInt(f[14]), Integer.parseInt(f[15]));
				if (prepared.put(zone, metadata) != null || ids.put(metadata.id, zone) != null)
				{
					throw new IOException("Duplicate dungeon catalog entry");
				}
			}
			if (prepared.size() != values().length) throw new IOException("Incomplete dungeon catalog");
			if (Thread.currentThread().isInterrupted()) return;
			catalog = new Catalog(Collections.unmodifiableMap(prepared), Collections.unmodifiableMap(ids));
			ALL_ZONES = List.of(values());
		}
		catch (IOException | RuntimeException e)
		{
			log.debug("Could not load dungeon catalog", e);
		}
	}

	private static WorldPoint point(String[] fields, int offset)
	{
		return new WorldPoint(Integer.parseInt(fields[offset]), Integer.parseInt(fields[offset + 1]), Integer.parseInt(fields[offset + 2]));
	}

	private Metadata metadata()
	{
		final Metadata result = catalog.metadata.get(this);
		if (result == null) throw new IllegalStateException("Dungeon catalog has not been loaded");
		return result;
	}

	/** True when this zone carries its own clip box (the wiki boxes do not frame it well). */
	public boolean hasClipOverride()
	{
		return metadata().clipMaxX > metadata().clipMinX && metadata().clipMaxY > metadata().clipMinY;
	}

	public int getClipMinX()
	{
		return metadata().clipMinX;
	}

	public int getClipMinY()
	{
		return metadata().clipMinY;
	}

	public int getClipMaxX()
	{
		return metadata().clipMaxX;
	}

	public int getClipMaxY()
	{
		return metadata().clipMaxY;
	}

	public String getId()
	{
		return metadata().id;
	}

	public String getName()
	{
		return metadata().name;
	}

	public WorldPoint getSurfacePoint()
	{
		return metadata().surfacePoint;
	}

	/** Canonical entrance first, followed by explicit alternate surface entrances. */
	public List<WorldPoint> getSurfacePoints()
	{
		return metadata().surfacePoints;
	}

	public WorldPoint nearestSurfacePoint(int worldX, int worldY)
	{
		WorldPoint nearest = metadata().surfacePoint;
		long nearestDistance = Long.MAX_VALUE;
		for (WorldPoint point : metadata().surfacePoints)
		{
			final long dx = (long) point.getX() - worldX;
			final long dy = (long) point.getY() - worldY;
			final long distance = dx * dx + dy * dy;
			if (distance < nearestDistance)
			{
				nearest = point;
				nearestDistance = distance;
			}
		}
		return nearest;
	}

	public WorldPoint getUndergroundPoint()
	{
		return metadata().undergroundPoint;
	}

	public int getYOffset()
	{
		return metadata().yOffset;
	}

	public int getDeltaX()
	{
		return metadata().undergroundPoint.getX() - metadata().surfacePoint.getX();
	}

	public int getDeltaY()
	{
		return metadata().undergroundPoint.getY() - metadata().surfacePoint.getY();
	}

	public int getOffsetX()
	{
		return getDeltaX();
	}

	public int getOffsetY()
	{
		return getDeltaY();
	}

	public int getRadius()
	{
		return metadata().radius;
	}

	public String getDescription()
	{
		return metadata().description;
	}

	public static String canonicalZoneId(String zoneId)
	{
		if (zoneId == null)
		{
			return null;
		}
		return SHARED_ZONE_IDS.getOrDefault(zoneId, zoneId);
	}

	public static boolean zonesMatch(String a, String b)
	{
		if (a == null || b == null)
		{
			return false;
		}
		if (a.equals(b))
		{
			return true;
		}
		return canonicalZoneId(a).equals(canonicalZoneId(b));
	}

	public String getCanonicalId()
	{
		return canonicalZoneId(metadata().id);
	}

	/** A native section shares its entrance control with the corresponding authored dungeon. */
	public String getSelectionId()
	{
		final String id = getId();
		final int separator = id.indexOf("__");
		return canonicalZoneId(separator < 0 ? id : id.substring(separator + 2));
	}

	public UndergroundZone getParentZone()
	{
		switch (this)
		{
			case DORGESH_KAAN_SOUTH:
				return DORGESH_KAAN;
			case CHASM_OF_TEARS:
				return LUMBRIDGE_SWAMP_CAVES;
			case NEYPOTZLI:
				return CAM_TORUM;
			case TARN_LAIR:
				return HAUNTED_MINE;
			case MOTHERLODE_MINE:
				return DWARVEN_MINES;
			default:
				return null;
		}
	}

	public boolean isSubterranean()
	{
		return getParentZone() != null;
	}

	public static UndergroundZone byId(String id)
	{
		return id == null ? null : catalog.byId.get(id);
	}

	/**
	 * Finds the nearest underground zone anchored to this surface world point within maxDistance.
	 */
	public static UndergroundZone forSurfacePoint(int worldX, int worldY, int maxDistance)
	{
		UndergroundZone best = null;
		int bestDistSq = maxDistance * maxDistance;

		for (UndergroundZone zone : ALL_ZONES)
		{
			if (zone.getId().startsWith("native_")) continue;
			for (WorldPoint surface : zone.getSurfacePoints())
			{
				final int dx = surface.getX() - worldX;
				final int dy = surface.getY() - worldY;
				final int distSq = dx * dx + dy * dy;
				if (distSq <= bestDistSq)
				{
					bestDistSq = distSq;
					best = zone;
				}
			}
		}

		return best;
	}

	/**
	 * Finds the nearest underground zone matching this lower plane dungeon world point within maxDistance.
	 */
	public static UndergroundZone forUndergroundPoint(int worldX, int worldY, int maxDistance)
	{
		UndergroundZone best = null;
		int bestDistSq = maxDistance * maxDistance;

		for (UndergroundZone zone : ALL_ZONES)
		{
			if (zone.getId().startsWith("native_")) continue;
			final int dx = zone.getUndergroundPoint().getX() - worldX;
			final int dy = zone.getUndergroundPoint().getY() - worldY;
			final int distSq = dx * dx + dy * dy;
			if (distSq <= bestDistSq)
			{
				bestDistSq = distSq;
				best = zone;
			}
		}

		return best;
	}
}
