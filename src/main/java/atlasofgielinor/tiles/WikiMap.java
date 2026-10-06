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
package atlasofgielinor.tiles;

import java.io.IOException;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import atlasofgielinor.data.io.BundledTsv;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * The map layers published by the OSRS Wiki map service, taken from its own
 * {@code data/dataloader.json} catalogue. Bounds are plain game coordinates, so no
 * georeferencing is needed - the tile grid and the game share one coordinate system.
 *
 * <p>{@link #SURFACE} is the overworld; every other entry is a dungeon or instanced area that
 * the game shows on its own separate world map.
 */
@Slf4j
public enum WikiMap
{
	SURFACE(0, "RuneScape Surface", 896, 1984, 4096, 4200),
	ANCIENT_CAVERN,
	ARDOUGNE_UNDERGROUND,
	ASGARNIA_ICE_CAVE,
	BRAINDEATH_ISLAND,
	DORGESH_KAAN,
	DWARVEN_MINES,
	GOD_WARS_DUNGEON,
	KALPHITE_HIVES,
	KARAMJA_UNDERGROUND,
	KELDAGRIM,
	MISCELLANIA_UNDERGROUND,
	MISTHALIN_UNDERGROUND,
	MOLE_HOLE,
	MORYTANIA_UNDERGROUND,
	MOS_LE_HARMLESS_CAVE,
	OURANIA_ALTAR,
	FREMENNIK_SLAYER_CAVE,
	STRONGHOLD_OF_SECURITY,
	STRONGHOLD_UNDERGROUND,
	TAVERLEY_UNDERGROUND,
	TOLNA_S_RIFT,
	TROLL_STRONGHOLD,
	TZHAAR_AREA,
	LAIR_OF_TARN_RAZORLOR,
	WATERBIRTH_DUNGEON,
	WILDERNESS_DUNGEONS,
	YANILLE_UNDERGROUND,
	ZANARIS,
	FOSSIL_ISLAND_UNDERGROUND,
	FELDIP_HILLS_UNDERGROUND,
	KOUREND_UNDERGROUND,
	KEBOS_UNDERGROUND,
	PRIFDDINAS,
	PRIFDDINAS_UNDERGROUND,
	PRIFDDINAS_GRAND_LIBRARY,
	LMS_DESERT_ISLAND,
	TUTORIAL_ISLAND,
	LMS_WILD_VARROCK,
	RUINS_OF_CAMDOZAAL,
	THE_ABYSS,
	LASSAR_UNDERCITY,
	KHARIDIAN_DESERT_UNDERGROUND,
	VARLAMORE_UNDERGROUND,
	CAM_TORUM,
	NEYPOTZLI,
	ARDENT_OCEAN_UNDERGROUND,
	UNQUIET_OCEAN_UNDERGROUND,
	SHROUDED_OCEAN_UNDERGROUND,
	SUNSET_OCEAN_UNDERGROUND,
	WESTERN_OCEAN_UNDERGROUND,
	NORTHERN_OCEAN_UNDERGROUND,
	VAMPYRIUM,
	ABANDONED_MINE_LEVEL_1,
	ABANDONED_MINE_LEVEL_2,
	ABANDONED_MINE_LEVEL_4,
	ABANDONED_MINE_LEVEL_6,
	ABYSS,
	ABYSSAL_AREA,
	AH_ZA_RHOON,
	AIRSHIP_PLATFORM,
	APE_ATOLL_DUNGEON,
	BANANA_PLANTATION_APE_ATOLL,
	BARBARIAN_ASSAULT,
	BARBARIAN_ASSAULT_LOBBY,
	BARROWS_CRYPTS,
	BLAST_FURNACE,
	BOOTS_OF_LIGHTNESS_AREAS,
	BRIMHAVEN_AGILITY_ARENA,
	BRINE_RAT_CAVERN,
	BRYOPHYTA_S_LAIR,
	BURTHORPE_GAMES_ROOM,
	CERBERUS_S_LAIR,
	CORPOREAL_BEAST_S_LAIR,
	COSMIC_ENTITY_S_PLANE,
	CRANDOR_LAB,
	CRASH_SITE_CAVERN,
	CREATURE_CREATION,
	DAEYALT_ESSENCE_MINE,
	DEATH_ALTAR,
	DESERT_EAGLE_LAIR,
	DESERT_MINING_CAMP_DUNGEON,
	DIGSITE_DUNGEON_ROCKS_BLOWN_UP,
	DONDAKAN_S_MINE_DURING_QUEST,
	DORGESH_KAAN_SOUTH_DUNGEON,
	DREAM_WORLD_CHALLENGES,
	DREAM_WORLD_DREAM_MENTOR,
	DRILL_DEMON,
	EAGLES_PEAK_DUNGEON,
	ELEMENTAL_WORKSHOP,
	ENAKHRA_S_TEMPLE,
	ENCHANTED_VALLEY,
	ENLIGHTENED_JOURNEY_CRASH_AREAS,
	EVIL_BOB_S_ISLAND,
	EVIL_CHICKEN_S_LAIR,
	EVIL_TWIN,
	EYES_OF_GLOUPHRIE_WAR_CUTSCENE,
	FAIRY_RESISTANCE_HIDEOUT,
	FISHER_REALM_DISEASED,
	FISHER_REALM_HEALTHY,
	FISHING_TRAWLER,
	FISHING_TRAWLER_10065,
	FISHING_TRAWLER_10066,
	FOSSIL_ISLAND_BOAT,
	FRAGMENT_OF_SEREN_FIGHT,
	FREAKY_FORESTER,
	GENIE_CAVE,
	GLARIAL_S_TOMB,
	GOBLIN_COOK,
	GORAK_PLANE,
	H_A_M_STORE_ROOM,
	HALLOWED_SEPULCHRE_LEVEL_1,
	HALLOWED_SEPULCHRE_STARTING_AREA,
	HARMONY_ISLAND_LOWER_LEVEL,
	ISAFDAR_SONG_OF_THE_ELVES,
	JALDRAOCHT_PYRAMID_LEVEL_1,
	JALDRAOCHT_PYRAMID_LEVEL_2,
	JALDRAOCHT_PYRAMID_LEVEL_3,
	JALDRAOCHT_PYRAMID_LEVEL_4,
	JATIZSO_MINE,
	JIGGIG_DUNGEON,
	JUNGLE_EAGLE_LAIR_RED_CHINCHOMPA_HUNTING_GROUND,
	KARAMJAN_TEMPLE,
	KEEP_LE_FAYE_INSTANCE,
	KELDAGRIM_RAT_PITS,
	KILLERWATT_PLANE,
	KING_S_RANSOM_DUNGEON,
	KISS_THE_FROG,
	KLENTER_S_PYRAMID,
	KRUK_S_DUNGEON,
	LADY_TRAHAERN_HIDEOUT,
	LIBRARY_HISTORICAL_ARCHIVE,
	LIGHTHOUSE_CUTSCENE,
	LIGHTHOUSE_DUNGEON,
	LIGHTHOUSE_DUNGEON_CUTSCENE,
	LITHKREN_VAULT,
	LITHKREN_VAULT_ENTRANCE_DURING_QUEST,
	LITHKREN_VAULT_ENTRANCE_POST_QUEST,
	LIZARDMAN_TEMPLE,
	LUMBRIDGE_CASTLE_RECIPE_FOR_DISASTER,
	MAGE_TRAINING_ARENA_ROOMS,
	MANIACAL_MONKEY_HUNTER_AREA,
	MEIYERDITCH_LABORATORIES,
	MEIYERDITCH_MINE,
	MIME,
	MISTHALIN_MYSTERY,
	MOGRE_CAMP,
	MONKEY_MADNESS_HANGAR_POST_QUEST,
	MONKEY_MADNESS_HANGAR_AND_BONZARA,
	MOURNER_TUNNELS,
	MOUSE_HOLE,
	MY_ARM_S_BIG_ADVENTURE_BOAT_CUTSCENE,
	MYREQUE_HIDEOUT_BURGH_DE_ROTT,
	MYREQUE_HIDEOUT_CANIFIS,
	MYREQUE_HIDEOUT_MEIYERDITCH,
	NATURE_ALTAR,
	NIGHTMARE_OF_ASHIHAMA,
	NORTH_EAST_KARAMJA_CUTSCENE,
	OBSERVATORY_DUNGEON,
	OGRE_ENCLAVE,
	OLD_SCHOOL_MUSEUM,
	PATERDOMUS_TEMPLE_UNDERGROUND,
	POLAR_EAGLE_LAIR,
	PRISON_PETE,
	PURO_PURO,
	PYRAMID_PLUNDER,
	QUIDAMORTEM_CAVE,
	QUIZ_MASTER,
	RANTZ_S_CAVE,
	RASHILIYIA_S_TOMB,
	RATCATCHERS_MANSION,
	RECIPE_FOR_DISASTER_APE_ATOLL_DUNGEON,
	RECRUITMENT_DRIVE,
	ROGUES_DEN,
	SABA_S_CAVE,
	SHADOW_DUNGEON,
	SKAVID_CAVES,
	SMOKE_DUNGEON,
	SOPHANEM_BANK,
	SOPHANEM_DUNGEON,
	SORCERESS_S_GARDEN,
	SURPRISE_EXAM,
	TEARS_OF_GUTHIX_CAVE,
	TEMPLE_OF_IKOV,
	TEMPLE_OF_MARIMBO_DUNGEON,
	TEMPLE_TREKKING,
	THAMMARON_S_THRONE_ROOM,
	THE_GRAND_TREE_MONKEY_MADNESS_II,
	THE_KENDAL_S_CAVE,
	TRAIN_STATION,
	TREE_GNOME_VILLAGE_DUNGEON,
	TREE_GNOME_VILLAGE_DUNGEON_INSTANCE,
	TROLL_ARENA_TROLLHEIM_TUNNEL,
	TROLLWEISS_DUNGEON,
	TUNNEL_OF_CHAOS,
	TUTORIAL_ISLAND_DUNGEON,
	TYRAS_CAMP_CUTSCENE,
	UNDERGROUND_PASS_BOTTOM_LEVEL,
	UNDERGROUND_PASS_BOTTOM_LEVEL_SONG_OF_THE_ELVES_INSTANCE,
	UNDERGROUND_PASS_FIRST_LEVEL,
	UNDERGROUND_PASS_IBAN_S_TEMPLE_POST_QUEST,
	UNDERGROUND_PASS_PLATFORMS,
	UNDERGROUND_PASS_SECOND_LEVEL,
	UNDERGROUND_PASS_SWAMP_FAIL_AND_FINAL_AREA,
	UNGAEL_LABORATORY,
	UZER_DUNGEON,
	VARROCK_MUSEUM_BASEMENT_HIGHER,
	VARROCK_MUSEUM_BASEMENT_LOWER,
	VARROCK_RAT_PITS,
	VIYELDI_CAVES_LOWER_LEVEL,
	VIYELDI_CAVES_UPPER_LEVEL,
	WATER_RAVINE_DUNGEON,
	WATERFALL_DUNGEON,
	WATERFALL_DUNGEON_WATER,
	WILDERNESS_WARS,
	WITCHAVEN_DUNGEON,
	WRATH_ALTAR,
	YANILLE_CUTSCENE,
	TUTORIAL_ISLAND_V2_DUNGEON,
	PRIFDDINAS_GRAND_LIBRARY_POST_QUEST,
	FULL_MAP;

	public static volatile WikiMap[] VALUES = {SURFACE};

	@AllArgsConstructor
	private static final class Metadata
	{
		private final int mapId;
		private final String displayName;
		private final int minX;
		private final int minY;
		private final int maxX;
		private final int maxY;
	}

	private final Metadata bootstrap;
	private static volatile Map<WikiMap, Metadata> catalog = Collections.emptyMap();

	WikiMap()
	{
		bootstrap = null;
	}

	WikiMap(int mapId, String displayName, int minX, int minY, int maxX, int maxY)
	{
		bootstrap = new Metadata(mapId, displayName, minX, minY, maxX, maxY);
	}

	public static boolean isLoaded()
	{
		return !catalog.isEmpty();
	}

	public static void load()
	{
		if (isLoaded()) return;
		final Map<WikiMap, Metadata> prepared = new EnumMap<>(WikiMap.class);
		try
		{
			for (String[] f : BundledTsv.read("/atlasofgielinor/dungeons/wiki-maps.tsv", 7))
			{
				final Metadata metadata = new Metadata(Integer.parseInt(f[1]), f[2], Integer.parseInt(f[3]),
					Integer.parseInt(f[4]), Integer.parseInt(f[5]), Integer.parseInt(f[6]));
				if (prepared.put(valueOf(f[0]), metadata) != null) throw new IOException("Duplicate map bounds");
			}
			if (prepared.size() != values().length) throw new IOException("Incomplete map bounds");
			if (Thread.currentThread().isInterrupted()) return;
			catalog = Collections.unmodifiableMap(prepared);
			VALUES = values();
		}
		catch (IOException | RuntimeException e)
		{
			log.debug("Could not load map bounds", e);
		}
	}

	private Metadata metadata()
	{
		final Metadata result = catalog.getOrDefault(this, bootstrap);
		if (result == null) throw new IllegalStateException("Map bounds have not been loaded");
		return result;
	}

	public int getMapId()
	{
		return metadata().mapId;
	}

	public String getDisplayName()
	{
		return metadata().displayName;
	}

	public int getMinX()
	{
		return metadata().minX;
	}

	public int getMinY()
	{
		return metadata().minY;
	}

	public int getMaxX()
	{
		return metadata().maxX;
	}

	public int getMaxY()
	{
		return metadata().maxY;
	}

	public boolean contains(int worldX, int worldY)
	{
		final Metadata bounds = metadata();
		return worldX >= bounds.minX && worldX <= bounds.maxX && worldY >= bounds.minY && worldY <= bounds.maxY;
	}

	public long area()
	{
		final Metadata bounds = metadata();
		return (long) (bounds.maxX - bounds.minX) * (bounds.maxY - bounds.minY);
	}
}
