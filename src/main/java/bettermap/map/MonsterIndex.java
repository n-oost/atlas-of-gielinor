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
package bettermap.map;

import bettermap.data.BundledJson;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.TreeMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

/**
 * Monster spawns, from the bundled wiki-derived dataset: 805 monsters over 2310 location zones.
 *
 * <p>Each zone is reduced to one marker at the centroid of its spawn tiles. Drawing all 19562
 * individual tiles would bury the map, and "Aberrant spectre, Slayer Tower" is the useful unit
 * anyway.
 *
 * <p>A zone's {@code map_id} is parsed but unused: the map draws every plane in one continuous
 * coordinate space, so a zone needs no layer resolution. The only zones dropped are those the
 * dataset gives no spawn coordinates for.</p>
 */
@Slf4j
@Singleton
public class MonsterIndex
{
	private final Gson gson;

	@Inject
	public MonsterIndex(Gson gson)
	{
		this.gson = gson;
	}

	private static final String RESOURCE = "/bettermap/data/monsters.json.gz";

	private final List<Zone> zones = new ArrayList<>();
	private final Map<String, List<Zone>> byMonster = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
	private SpatialIndex<Zone> spatialIndex = new SpatialIndex<>(
		Collections.emptyList(), Zone::getPlane, Zone::getX, Zone::getY);

	private volatile boolean loaded;
	private volatile long dataVersion;

	public long getDataVersion()
	{
		return dataVersion;
	}
	private int droppedZones;

	public boolean isLoaded()
	{
		return loaded;
	}

	public synchronized int size()
	{
		return zones.size();
	}

	public synchronized int getDroppedZones()
	{
		return droppedZones;
	}

	public synchronized List<Zone> getZones()
	{
		return zones;
	}

	/** Monster name to its zones, for the legend. */
	public synchronized Map<String, List<Zone>> byMonster()
	{
		return new LinkedHashMap<>(byMonster);
	}

	/** Monster name or location containing {@code query} (case-insensitive), at most {@code cap} of them. */
	public synchronized List<Zone> searchByName(String query, int cap)
	{
		if (query == null || query.isEmpty() || cap <= 0 || !loaded)
		{
			return Collections.emptyList();
		}

		final String lower = query.toLowerCase(Locale.ROOT);
		final List<Zone> matches = new ArrayList<>();
		for (Zone zone : zones)
		{
			final boolean matchMonster = zone.getMonster() != null
				&& zone.getMonster().toLowerCase(Locale.ROOT).contains(lower);
			final boolean matchLocation = zone.getLocationName() != null
				&& zone.getLocationName().toLowerCase(Locale.ROOT).contains(lower);
			if (matchMonster || matchLocation)
			{
				matches.add(zone);
				if (matches.size() >= cap)
				{
					break;
				}
			}
		}

		return matches;
	}

	/** Parses the bundled dataset. Call this off the client thread; it takes a moment. */
	public void load()
	{
		final List<Zone> parsed = new ArrayList<>();
		int dropped = 0;

		try
		{
			final Dataset dataset = BundledJson.readGzip(gson, RESOURCE, Dataset.class);

			if (dataset == null || dataset.monsters == null)
			{
				return;
			}

			for (MonsterEntry monster : dataset.monsters)
			{
				if (monster == null || monster.locations == null)
				{
					continue;
				}

				for (LocationEntry location : monster.locations)
				{
					final Zone zone = toZone(monster, location);
					if (zone == null)
					{
						dropped++;
					}
					else
					{
						parsed.add(zone);
					}
				}
			}
		}
		catch (IOException | RuntimeException e)
		{
			log.warn("Could not read the monster dataset", e);
			return;
		}

		final Map<String, List<Zone>> grouped = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		final SpatialIndex<Zone> spatial = new SpatialIndex<>(
			parsed, Zone::getPlane, Zone::getX, Zone::getY);
		for (Zone zone : parsed)
		{
			grouped.computeIfAbsent(zone.monster, k -> new ArrayList<>()).add(zone);
		}

		synchronized (this)
		{
			zones.clear();
			zones.addAll(parsed);
			byMonster.clear();
			byMonster.putAll(grouped);
			spatialIndex = spatial;
			droppedZones = dropped;
		}

		loaded = true;
		dataVersion++;
		log.info("Loaded {} monster zones ({} omitted due to empty coordinates)",
			parsed.size(), dropped);
	}

	private static Zone toZone(MonsterEntry monster, LocationEntry location)
	{
		if (location.coordinates == null || location.coordinates.isEmpty())
		{
			return null;
		}

		long sumX = 0;
		long sumY = 0;
		for (Coordinate coordinate : location.coordinates)
		{
			sumX += coordinate.x;
			sumY += coordinate.y;
		}

		final int x = (int) (sumX / location.coordinates.size());
		final int y = (int) (sumY / location.coordinates.size());

		return new Zone(
			monster.name,
			monster.combatLevel,
			monster.slayerLevel,
			monster.slayerMasters == null ? Collections.emptyList() : monster.slayerMasters,
			monster.examine,
			location.locationName,
			Math.max(0, Math.min(3, location.plane)),
			x,
			y,
			location.coordinates.size(),
			monster.maxHit,
			monster.attackType,
			monster.defenceStab,
			monster.defenceSlash,
			monster.defenceCrush,
			monster.defenceMagic,
			monster.defenceRanged,
			monster.magicLevel);
	}

	/** The zone whose marker is nearest this point on the given plane, or null. */
	public synchronized Zone nearest(int plane, int worldX, int worldY, int radius)
	{
		return spatialIndex.nearest(plane, worldX, worldY, radius, null);
	}

	/**
	 * Invokes {@code consumer} for every zone on {@code plane} whose centroid lies inside the
	 * world bounding box. Uses the chunk index so the overlay can draw faint spawn areas without
	 * scanning all ~2,310 zones every frame.
	 */
	public synchronized void forEachInArea(int plane, int minWorldX, int maxWorldX, int minWorldY, int maxWorldY,
		Consumer<Zone> consumer)
	{
		spatialIndex.forEachInArea(plane, minWorldX, maxWorldX, minWorldY, maxWorldY, consumer);
	}

	/** One monster in one place. */
	public static final class Zone
	{
		private final String monster;
		private final int combatLevel;
		private final int slayerLevel;
		private final List<String> slayerMasters;
		private final String examine;
		private final String locationName;
		private final int plane;
		private final int x;
		private final int y;
		private final int tileCount;
		private final Integer maxHit;
		private final String attackType;
		private final Integer defenceStab;
		private final Integer defenceSlash;
		private final Integer defenceCrush;
		private final Integer defenceMagic;
		private final Integer defenceRanged;
		private final Integer magicLevel;

		Zone(String monster, int combatLevel, int slayerLevel, List<String> slayerMasters,
			String examine, String locationName, int plane, int x, int y, int tileCount,
			Integer maxHit, String attackType, Integer defenceStab, Integer defenceSlash,
			Integer defenceCrush, Integer defenceMagic, Integer defenceRanged, Integer magicLevel)
		{
			this.monster = monster;
			this.combatLevel = combatLevel;
			this.slayerLevel = slayerLevel;
			this.slayerMasters = slayerMasters;
			this.examine = examine;
			this.locationName = locationName;
			this.plane = plane;
			this.x = x;
			this.y = y;
			this.tileCount = tileCount;
			this.maxHit = maxHit;
			this.attackType = attackType;
			this.defenceStab = defenceStab;
			this.defenceSlash = defenceSlash;
			this.defenceCrush = defenceCrush;
			this.defenceMagic = defenceMagic;
			this.defenceRanged = defenceRanged;
			this.magicLevel = magicLevel;
		}

		public String getMonster()
		{
			return monster;
		}

		public int getCombatLevel()
		{
			return combatLevel;
		}

		public int getSlayerLevel()
		{
			return slayerLevel;
		}

		public String getSlayerMasters()
		{
			if (slayerMasters.isEmpty())
			{
				return null;
			}

			final StringBuilder text = new StringBuilder();
			for (String master : slayerMasters)
			{
				if (text.length() > 0)
				{
					text.append(", ");
				}
				text.append(master.substring(0, 1).toUpperCase(Locale.ROOT)).append(master.substring(1));
			}
			return text.toString();
		}

		public String getExamine()
		{
			return examine;
		}

		public String getLocationName()
		{
			return locationName;
		}

		public int getPlane()
		{
			return plane;
		}

		public int getX()
		{
			return x;
		}

		public int getY()
		{
			return y;
		}

		public int getTileCount()
		{
			return tileCount;
		}

		public Integer getMaxHit()
		{
			return maxHit;
		}

		public String getAttackType()
		{
			return attackType;
		}

		public Integer getDefenceStab()
		{
			return defenceStab;
		}

		public Integer getDefenceSlash()
		{
			return defenceSlash;
		}

		public Integer getDefenceCrush()
		{
			return defenceCrush;
		}

		public Integer getDefenceMagic()
		{
			return defenceMagic;
		}

		public Integer getDefenceRanged()
		{
			return defenceRanged;
		}

		public Integer getMagicLevel()
		{
			return magicLevel;
		}

		/**
		 * Compact combat summary for tooltips: max hit, attack style, melee/mage defences, mage level.
		 * Omits lines when the underlying stats are missing.
		 */
		public List<String> combatSummaryLines()
		{
			final List<String> lines = new ArrayList<>(2);
			final StringBuilder hit = new StringBuilder();
			if (maxHit != null)
			{
				hit.append("Max hit: ").append(maxHit);
			}
			if (attackType != null && !attackType.isEmpty())
			{
				if (hit.length() > 0)
				{
					hit.append(" · ");
				}
				hit.append("Style: ").append(attackType);
			}
			if (hit.length() > 0)
			{
				lines.add(hit.toString());
			}

			final StringBuilder defence = new StringBuilder();
			if (defenceStab != null && defenceSlash != null && defenceCrush != null)
			{
				defence.append("Def: ")
					.append(defenceStab).append('/')
					.append(defenceSlash).append('/')
					.append(defenceCrush);
			}
			if (defenceMagic != null)
			{
				if (defence.length() > 0)
				{
					defence.append(" · ");
				}
				defence.append("Mage def: ").append(defenceMagic);
			}
			if (magicLevel != null)
			{
				if (defence.length() > 0)
				{
					defence.append(" · ");
				}
				defence.append("Mage lvl: ").append(magicLevel);
			}
			if (defence.length() > 0)
			{
				lines.add(defence.toString());
			}
			return lines;
		}
	}

	private static final class Dataset
	{
		private List<MonsterEntry> monsters;
	}

	private static final class MonsterEntry
	{
		private String name;
		@SerializedName("combat_level")
		private int combatLevel;
		@SerializedName("slayer_level")
		private int slayerLevel;
		@SerializedName("slayer_masters")
		private List<String> slayerMasters;
		private String examine;
		@SerializedName("max_hit")
		private Integer maxHit;
		@SerializedName("attack_type")
		private String attackType;
		@SerializedName("defence_stab")
		private Integer defenceStab;
		@SerializedName("defence_slash")
		private Integer defenceSlash;
		@SerializedName("defence_crush")
		private Integer defenceCrush;
		@SerializedName("defence_magic")
		private Integer defenceMagic;
		@SerializedName("defence_ranged")
		private Integer defenceRanged;
		@SerializedName("magic_level")
		private Integer magicLevel;
		private List<LocationEntry> locations;
	}

	private static final class LocationEntry
	{
		@SerializedName("location_name")
		private String locationName;
		private int plane;
		@SerializedName("map_id")
		private int mapId;
		private List<Coordinate> coordinates;
	}

	private static final class Coordinate
	{
		private int x;
		private int y;
	}
}
