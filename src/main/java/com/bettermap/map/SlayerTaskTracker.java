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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import com.google.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.client.plugins.slayer.SlayerPluginService;

/**
 * Resolves the player's active Slayer task from {@link SlayerPluginService} or client VarPlayers,
 * and matches monster zones and bosses against the current assignment.
 */
@Slf4j
@Singleton
public class SlayerTaskTracker
{
	/** Hoisted: stemWords runs per visible zone per frame, and String.split recompiles every call. */
	private static final Pattern WORD_SPLIT = Pattern.compile("[\\s\\-_',()]+");

	private final Client client;

	/** Match results for the current task — cleared when the task string changes. */
	private String cachedTaskKey;
	private final Map<String, Boolean> taskMatchCache = new HashMap<>();

	@Inject(optional = true)
	private SlayerPluginService slayerPluginService;

	@Inject
	public SlayerTaskTracker(@Nullable Client client)
	{
		this.client = client;
	}

	public SlayerTaskTracker(@Nullable Client client, @Nullable SlayerPluginService slayerPluginService)
	{
		this.client = client;
		this.slayerPluginService = slayerPluginService;
	}

	/**
	 * Returns the player's active Slayer task name, or null if there is no task or the plugin is disabled.
	 */
	@Nullable
	public String getCurrentTask()
	{
		if (slayerPluginService != null)
		{
			try
			{
				final String task = slayerPluginService.getTask();
				if (task != null && !task.trim().isEmpty())
				{
					return task.trim();
				}
			}
			catch (Exception e)
			{
				log.debug("Error querying SlayerPluginService.getTask()", e);
			}
		}

		return null;
	}

	/**
	 * Returns the remaining kill count for the active task, or 0 if unknown or no task.
	 */
	public int getRemainingTaskAmount()
	{
		if (slayerPluginService != null)
		{
			try
			{
				final int amount = slayerPluginService.getRemainingAmount();
				if (amount > 0)
				{
					return amount;
				}
			}
			catch (Exception e)
			{
				log.debug("Error querying SlayerPluginService.getRemainingAmount()", e);
			}
		}

		if (client != null)
		{
			try
			{
				final int varpCount = client.getVarpValue(VarPlayerID.SLAYER_COUNT);
				if (varpCount > 0)
				{
					return varpCount;
				}
			}
			catch (Exception ignored)
			{
			}
		}

		return 0;
	}

	/**
	 * Returns the list of valid target names for the current assignment, including sub-types and boss variants.
	 */
	public List<String> getTaskTargets()
	{
		if (slayerPluginService != null)
		{
			try
			{
				final List<net.runelite.api.NPC> npcs = slayerPluginService.getTargets();
				if (npcs != null && !npcs.isEmpty())
				{
					final List<String> names = new ArrayList<>(npcs.size());
					for (net.runelite.api.NPC npc : npcs)
					{
						if (npc != null && npc.getName() != null && !names.contains(npc.getName()))
						{
							names.add(npc.getName());
						}
					}
					if (!names.isEmpty())
					{
						return names;
					}
				}
			}
			catch (Exception e)
			{
				log.debug("Error querying SlayerPluginService.getTargets()", e);
			}
		}

		final String task = getCurrentTask();
		if (task != null && !task.isEmpty())
		{
			return Collections.singletonList(task);
		}

		return Collections.emptyList();
	}

	/**
	 * Returns the task location hint (e.g. "Slayer Tower", "Catacombs of Kourend") if available.
	 */
	@Nullable
	public String getTaskLocation()
	{
		if (slayerPluginService != null)
		{
			try
			{
				return slayerPluginService.getTaskLocation();
			}
			catch (Exception ignored)
			{
			}
		}

		return null;
	}

	/**
	 * Whether a monster zone's location name satisfies the active task's location hint. Konar quo
	 * Maten assigns area-locked tasks whose {@link #getTaskLocation()} names the only place a kill
	 * counts; this tokenised match (same stemming as monster-name matching) decides which zones
	 * lie in that area. Returns false when there is no hint.
	 */
	public boolean matchesLocationHint(@Nullable String zoneLocationName)
	{
		final String hint = getTaskLocation();
		if (hint == null || hint.trim().isEmpty() || zoneLocationName == null || zoneLocationName.trim().isEmpty())
		{
			return false;
		}

		final List<String> hintWords = stemWords(hint.trim().toLowerCase(Locale.ROOT));
		final List<String> locWords = stemWords(zoneLocationName.trim().toLowerCase(Locale.ROOT));
		if (hintWords.isEmpty() || locWords.isEmpty())
		{
			return false;
		}

		final String hs = String.join(" ", hintWords);
		final String ls = String.join(" ", locWords);
		if (hs.equals(ls) || ls.contains(hs) || hs.contains(ls))
		{
			return true;
		}

		return containsAllWords(locWords, hintWords);
	}

	/**
	 * The best on-map target for the active Slayer task.
	 *
	 * <p>With no location hint: the task-monster zone nearest {@code from} by straight-line
	 * distance. With a hint (a Konar area lock): the nearest task zone <i>inside that area</i>, or
	 * — when no zone is tagged with the area — the named place itself from {@code poiIndex}, since
	 * routing to an out-of-area spawn would earn no task credit. Null when there is no task or
	 * nothing resolves.
	 */
	@Nullable
	public WorldPoint nearestTaskLocation(@Nullable WorldPoint from,
		@Nullable MonsterIndex monsterIndex, @Nullable PoiIndex poiIndex)
	{
		final String task = getCurrentTask();
		if (task == null || task.trim().isEmpty() || monsterIndex == null)
		{
			return null;
		}

		final List<MonsterIndex.Zone> taskZones = new ArrayList<>();
		for (MonsterIndex.Zone zone : monsterIndex.getZones())
		{
			if (isTaskMonster(zone))
			{
				taskZones.add(zone);
			}
		}

		final String hint = getTaskLocation();
		if (hint != null && !hint.trim().isEmpty())
		{
			final List<MonsterIndex.Zone> inArea = new ArrayList<>();
			for (MonsterIndex.Zone zone : taskZones)
			{
				if (matchesLocationHint(zone.getLocationName()))
				{
					inArea.add(zone);
				}
			}
			if (!inArea.isEmpty())
			{
				return nearestZonePoint(inArea, from);
			}
			if (poiIndex != null)
			{
				return nearestPoiPoint(poiIndex.searchByName(hint.trim().toLowerCase(Locale.ROOT), 24), from);
			}
			return null;
		}

		return taskZones.isEmpty() ? null : nearestZonePoint(taskZones, from);
	}

	@Nullable
	private static WorldPoint nearestZonePoint(List<MonsterIndex.Zone> zones, @Nullable WorldPoint from)
	{
		MonsterIndex.Zone best = null;
		long bestDist = Long.MAX_VALUE;
		for (MonsterIndex.Zone zone : zones)
		{
			if (best == null)
			{
				best = zone;
			}
			if (from != null)
			{
				final long dx = zone.getX() - from.getX();
				final long dy = zone.getY() - from.getY();
				final long d = dx * dx + dy * dy;
				if (d < bestDist)
				{
					bestDist = d;
					best = zone;
				}
			}
		}
		return best == null ? null : new WorldPoint(best.getX(), best.getY(), best.getPlane());
	}

	@Nullable
	private static WorldPoint nearestPoiPoint(List<PoiIndex.Poi> pois, @Nullable WorldPoint from)
	{
		PoiIndex.Poi best = null;
		long bestDist = Long.MAX_VALUE;
		for (PoiIndex.Poi poi : pois)
		{
			if (best == null)
			{
				best = poi;
			}
			if (from != null)
			{
				final long dx = poi.getX() - from.getX();
				final long dy = poi.getY() - from.getY();
				final long d = dx * dx + dy * dy;
				if (d < bestDist)
				{
					bestDist = d;
					best = poi;
				}
			}
		}
		return best == null ? null : new WorldPoint(best.getX(), best.getY(), best.getPlane());
	}

	/**
	 * Checks if a monster zone matches the player's active Slayer task.
	 */
	public boolean isTaskMonster(@Nullable MonsterIndex.Zone zone)
	{
		if (zone == null)
		{
			return false;
		}

		return isTaskMonster(zone.getMonster());
	}

	/**
	 * Checks if a boss from {@link MonsterLocationData} matches the player's active Slayer task.
	 */
	public boolean isTaskBoss(@Nullable MonsterLocationData boss)
	{
		if (boss == null)
		{
			return false;
		}

		return isTaskMonster(boss.getName());
	}

	/**
	 * Checks if a monster name matches the player's active Slayer task or any of its sub-targets.
	 */
	public boolean isTaskMonster(@Nullable String monsterName)
	{
		if (monsterName == null || monsterName.trim().isEmpty())
		{
			return false;
		}

		final String task = getCurrentTask();
		final String cacheKey = task == null ? "" : task;
		if (!cacheKey.equals(cachedTaskKey))
		{
			cachedTaskKey = cacheKey;
			taskMatchCache.clear();
		}

		final Boolean cached = taskMatchCache.get(monsterName);
		if (cached != null)
		{
			return cached;
		}

		boolean match = false;
		if (task != null && !task.trim().isEmpty() && matchesTask(monsterName, task))
		{
			match = true;
		}
		else if (slayerPluginService != null)
		{
			try
			{
				final List<NPC> targets = slayerPluginService.getTargets();
				if (targets != null && !targets.isEmpty())
				{
					for (NPC npc : targets)
					{
						if (npc != null && npc.getName() != null && matchesTask(monsterName, npc.getName()))
						{
							match = true;
							break;
						}
					}
				}
			}
			catch (Exception ignored)
			{
			}
		}

		taskMatchCache.put(monsterName, match);
		return match;
	}

	/**
	 * Pure matching helper that evaluates whether a candidate monster name matches a Slayer task or target string.
	 * Handles case-insensitivity, plurals, prefixes, and word tokenization.
	 */
	public static boolean matchesTask(@Nullable String monsterName, @Nullable String taskName)
	{
		if (monsterName == null || taskName == null)
		{
			return false;
		}

		final String mClean = monsterName.trim().toLowerCase(Locale.ROOT);
		final String tClean = taskName.trim().toLowerCase(Locale.ROOT);

		if (mClean.isEmpty() || tClean.isEmpty())
		{
			return false;
		}

		if (mClean.equals(tClean))
		{
			return true;
		}

		final List<String> mWords = stemWords(mClean);
		final List<String> tWords = stemWords(tClean);

		final String mStemmed = String.join(" ", mWords);
		final String tStemmed = String.join(" ", tWords);

		if (mStemmed.equals(tStemmed))
		{
			return true;
		}

		// Exact phrase containment (e.g. "abyssal demon" in "abyssal demons", "mutated bloodveld" contains "bloodveld")
		if (mStemmed.contains(tStemmed) || tStemmed.contains(mStemmed))
		{
			return true;
		}

		// Specific known Slayer creature variant groupings
		if (mStemmed.contains("spectre") && tStemmed.contains("spectre"))
		{
			return true;
		}

		// Token subset matching: all words in task must be present in the monster name
		if (!tWords.isEmpty() && containsAllWords(mWords, tWords))
		{
			return true;
		}

		// Token subset matching in reverse if monster name is a distinct sub-token of task
		if (!mWords.isEmpty() && mWords.size() >= 2 && containsAllWords(tWords, mWords))
		{
			return true;
		}

		return false;
	}

	private static boolean containsAllWords(List<String> container, List<String> required)
	{
		if (required.isEmpty())
		{
			return false;
		}

		for (String req : required)
		{
			if (req.length() <= 1)
			{
				continue;
			}

			boolean found = false;
			for (String cand : container)
			{
				if (cand.equals(req) || cand.contains(req) || req.contains(cand))
				{
					found = true;
					break;
				}
			}

			if (!found)
			{
				return false;
			}
		}

		return true;
	}

	private static List<String> stemWords(String phrase)
	{
		final String[] tokens = WORD_SPLIT.split(phrase);
		final List<String> result = new ArrayList<>(tokens.length);

		for (String token : tokens)
		{
			final String stemmed = stemWord(token);
			if (!stemmed.isEmpty())
			{
				result.add(stemmed);
			}
		}

		return result;
	}

	/**
	 * Stems common English and OSRS plural suffixes to canonical singular forms.
	 */
	public static String stemWord(String word)
	{
		if (word == null)
		{
			return "";
		}

		String w = word.trim().toLowerCase(Locale.ROOT);
		if (w.isEmpty())
		{
			return "";
		}

		// "aviansies" -> "aviansie", "jellies" -> "jelly"
		if (w.endsWith("ies") && w.length() > 4)
		{
			if (w.endsWith("aviansies") || w.endsWith("zombies"))
			{
				return w.substring(0, w.length() - 1);
			}
			return w.substring(0, w.length() - 3) + "y";
		}

		// "wolves" -> "wolf", "elves" -> "elf"
		if (w.endsWith("ves") && w.length() > 4)
		{
			return w.substring(0, w.length() - 3) + "f";
		}

		// "lizardmen" -> "lizardman"
		if (w.endsWith("men") && w.length() > 4)
		{
			return w.substring(0, w.length() - 3) + "man";
		}

		// General plural trailing 's', excluding words like 'abyss', 'boss', 'moss', 'grass'
		if (w.endsWith("s") && w.length() > 3 && !w.endsWith("ss") && !w.endsWith("is") && !w.endsWith("us"))
		{
			return w.substring(0, w.length() - 1);
		}

		return w;
	}
}
