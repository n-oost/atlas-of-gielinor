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
package atlasofgielinor.integrations;

import com.google.gson.Gson;

import atlasofgielinor.map.catalog.MonsterIndex;
import atlasofgielinor.map.catalog.PoiIndex;
import atlasofgielinor.integrations.SlayerTaskTracker;

import java.util.Collections;
import java.util.List;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.slayer.SlayerPluginService;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class SlayerTaskTrackerTest
{
	@Test
	public void testStemWord()
	{
		assertEquals("demon", SlayerTaskTracker.stemWord("demons"));
		assertEquals("dragon", SlayerTaskTracker.stemWord("dragons"));
		assertEquals("gargoyle", SlayerTaskTracker.stemWord("gargoyles"));
		assertEquals("bloodveld", SlayerTaskTracker.stemWord("bloodvelds"));
		assertEquals("horror", SlayerTaskTracker.stemWord("horrors"));
		assertEquals("aviansie", SlayerTaskTracker.stemWord("aviansies"));
		assertEquals("banshee", SlayerTaskTracker.stemWord("banshees"));
		assertEquals("jelly", SlayerTaskTracker.stemWord("jellies"));
		assertEquals("zombie", SlayerTaskTracker.stemWord("zombies"));
		assertEquals("wolf", SlayerTaskTracker.stemWord("wolves"));
		assertEquals("elf", SlayerTaskTracker.stemWord("elves"));
		assertEquals("lizardman", SlayerTaskTracker.stemWord("lizardmen"));
		assertEquals("moss", SlayerTaskTracker.stemWord("moss"));
		assertEquals("abyss", SlayerTaskTracker.stemWord("abyss"));
	}

	@Test
	public void testMatchesTaskDirectAndPlural()
	{
		assertTrue(SlayerTaskTracker.matchesTask("Abyssal demon", "Abyssal demons"));
		assertTrue(SlayerTaskTracker.matchesTask("Abyssal demons", "Abyssal demon"));
		assertTrue(SlayerTaskTracker.matchesTask("Gargoyle", "Gargoyles"));
		assertTrue(SlayerTaskTracker.matchesTask("Cave horror", "Cave horrors"));
		assertTrue(SlayerTaskTracker.matchesTask("Dust devil", "Dust devils"));
		assertTrue(SlayerTaskTracker.matchesTask("Smoke devil", "Smoke devils"));
		assertTrue(SlayerTaskTracker.matchesTask("Nechryael", "Nechryael"));
		assertTrue(SlayerTaskTracker.matchesTask("Aviansie", "Aviansies"));
		assertTrue(SlayerTaskTracker.matchesTask("Banshee", "Banshees"));
		assertTrue(SlayerTaskTracker.matchesTask("Wolf", "Wolves"));
		assertTrue(SlayerTaskTracker.matchesTask("Elf", "Elves"));
		assertTrue(SlayerTaskTracker.matchesTask("Lizardman", "Lizardmen"));
	}

	@Test
	public void testMatchesTaskPrefixAndSubtypes()
	{
		// Variations of bloodvelds
		assertTrue(SlayerTaskTracker.matchesTask("Mutated bloodveld", "Bloodvelds"));
		assertTrue(SlayerTaskTracker.matchesTask("Insatiable bloodveld", "Bloodvelds"));

		// Variations of black dragons
		assertTrue(SlayerTaskTracker.matchesTask("Baby black dragon", "Black dragons"));
		assertTrue(SlayerTaskTracker.matchesTask("Brutal black dragon", "Black dragons"));
		assertTrue(SlayerTaskTracker.matchesTask("King Black Dragon", "Black dragons"));

		// Variations of blue dragons
		assertTrue(SlayerTaskTracker.matchesTask("Baby blue dragon", "Blue dragons"));
		assertTrue(SlayerTaskTracker.matchesTask("Brutal blue dragon", "Blue dragons"));

		// Variations of spectres
		assertTrue(SlayerTaskTracker.matchesTask("Aberrant spectre", "Aberrant spectres"));
		assertTrue(SlayerTaskTracker.matchesTask("Abhorrent spectre", "Aberrant spectres"));

		// Dagannoth
		assertTrue(SlayerTaskTracker.matchesTask("Dagannoth Prime", "Dagannoth"));
		assertTrue(SlayerTaskTracker.matchesTask("Dagannoth Rex", "Dagannoth"));
		assertTrue(SlayerTaskTracker.matchesTask("Dagannoth Supreme", "Dagannoth"));

		// Kalphite
		assertTrue(SlayerTaskTracker.matchesTask("Kalphite Worker", "Kalphite"));
		assertTrue(SlayerTaskTracker.matchesTask("Kalphite Soldier", "Kalphite"));
		assertTrue(SlayerTaskTracker.matchesTask("Kalphite Guardian", "Kalphite"));
		assertTrue(SlayerTaskTracker.matchesTask("Kalphite Queen", "Kalphite"));
	}

	@Test
	public void testNegativeMatches()
	{
		// Different types of demons
		assertFalse(SlayerTaskTracker.matchesTask("Lesser demon", "Greater demons"));
		assertFalse(SlayerTaskTracker.matchesTask("Black demon", "Greater demons"));
		assertFalse(SlayerTaskTracker.matchesTask("Lesser demon", "Black demons"));

		// Different types of dragons
		assertFalse(SlayerTaskTracker.matchesTask("Green dragon", "Black dragons"));
		assertFalse(SlayerTaskTracker.matchesTask("Red dragon", "Blue dragons"));
		assertFalse(SlayerTaskTracker.matchesTask("Rune dragon", "Iron dragons"));

		// Different giants
		assertFalse(SlayerTaskTracker.matchesTask("Moss giant", "Fire giants"));
		assertFalse(SlayerTaskTracker.matchesTask("Hill giant", "Ice giants"));

		// Null & Empty
		assertFalse(SlayerTaskTracker.matchesTask(null, "Abyssal demons"));
		assertFalse(SlayerTaskTracker.matchesTask("Abyssal demon", null));
		assertFalse(SlayerTaskTracker.matchesTask("", "Abyssal demons"));
		assertFalse(SlayerTaskTracker.matchesTask("Abyssal demon", ""));
	}

	@Test
	public void testServiceIntegration()
	{
		final SlayerPluginService mockService = new SlayerPluginService()
		{
			@Override
			public String getTask()
			{
				return "Blue dragons";
			}

			@Override
			public int getRemainingAmount()
			{
				return 45;
			}

			@Override
			public int getInitialAmount()
			{
				return 120;
			}

			@Override
			public List<NPC> getTargets()
			{
				return Collections.emptyList();
			}

			@Override
			public String getTaskLocation()
			{
				return "Taverley Dungeon";
			}
		};

		final SlayerTaskTracker tracker = new SlayerTaskTracker(null, mockService);

		assertEquals("Blue dragons", tracker.getCurrentTask());
		assertEquals(45, tracker.getRemainingTaskAmount());
		assertTrue(tracker.isTaskMonster("Blue dragon"));
		assertTrue(tracker.isTaskMonster("Baby blue dragon"));
		assertTrue(tracker.isTaskMonster("Brutal blue dragon"));
		assertFalse(tracker.isTaskMonster("Green dragon"));
	}

	@Test
	public void testNullServiceSafeFallback()
	{
		final SlayerTaskTracker tracker = new SlayerTaskTracker(null, null);

		assertEquals(null, tracker.getCurrentTask());
		assertEquals(0, tracker.getRemainingTaskAmount());
		assertFalse(tracker.isTaskMonster("Abyssal demon"));
	}

	private static SlayerPluginService service(final String task, final String location)
	{
		return new SlayerPluginService()
		{
			@Override
			public String getTask()
			{
				return task;
			}

			@Override
			public int getRemainingAmount()
			{
				return 10;
			}

			@Override
			public int getInitialAmount()
			{
				return 100;
			}

			@Override
			public List<NPC> getTargets()
			{
				return Collections.emptyList();
			}

			@Override
			public String getTaskLocation()
			{
				return location;
			}
		};
	}

	@Test
	public void matchesLocationHintTokenises()
	{
		final SlayerTaskTracker tracker = new SlayerTaskTracker(null, service("Blue dragons", "Taverley Dungeon"));

		assertTrue(tracker.matchesLocationHint("Taverley Dungeon"));
		assertTrue(tracker.matchesLocationHint("Taverley Dungeon Resource Area"));
		assertFalse(tracker.matchesLocationHint("Wilderness"));
		assertFalse(tracker.matchesLocationHint(null));
	}

	@Test
	public void matchesLocationHintFalseWithoutHint()
	{
		final SlayerTaskTracker tracker = new SlayerTaskTracker(null, service("Blue dragons", null));
		assertFalse(tracker.matchesLocationHint("Taverley Dungeon"));
	}

	@Test
	public void nearestTaskLocationNullWhenNoTask()
	{
		final SlayerTaskTracker tracker = new SlayerTaskTracker(null, service(null, null));
		assertNull(tracker.nearestTaskLocation(new WorldPoint(3200, 3200, 0), loadedMonsterIndex(), new PoiIndex()));
	}

	@Test
	public void nearestTaskLocationHonoursKonarArea()
	{
		final MonsterIndex monsterIndex = loadedMonsterIndex();

		// The Taverley Dungeon blue-dragon zone nearest a faraway player — the resolver must return
		// exactly this even though closer blue-dragon zones exist outside the Konar area.
		final WorldPoint faraway = new WorldPoint(3700, 3700, 0);
		MonsterIndex.Zone taverley = null;
		long bestDist = Long.MAX_VALUE;
		for (MonsterIndex.Zone zone : monsterIndex.getZones())
		{
			if (!SlayerTaskTracker.matchesTask(zone.getMonster(), "Blue dragons")
				|| zone.getLocationName() == null
				|| !zone.getLocationName().toLowerCase().contains("taverley"))
			{
				continue;
			}
			final long dx = zone.getX() - faraway.getX();
			final long dy = zone.getY() - faraway.getY();
			final long d = dx * dx + dy * dy;
			if (d < bestDist)
			{
				bestDist = d;
				taverley = zone;
			}
		}
		assertNotNull("dataset should carry a Taverley Dungeon blue dragon zone", taverley);

		final SlayerTaskTracker tracker = new SlayerTaskTracker(null, service("Blue dragons", "Taverley Dungeon"));
		final WorldPoint target = tracker.nearestTaskLocation(faraway, monsterIndex, new PoiIndex());

		assertNotNull(target);
		assertEquals(taverley.getX(), target.getX());
		assertEquals(taverley.getY(), target.getY());
	}

	@Test
	public void nearestTaskLocationPicksNearestWithoutHint()
	{
		final MonsterIndex monsterIndex = loadedMonsterIndex();

		final WorldPoint from = new WorldPoint(2900, 9800, 0);
		MonsterIndex.Zone nearest = null;
		long bestDist = Long.MAX_VALUE;
		for (MonsterIndex.Zone zone : monsterIndex.getZones())
		{
			if (!SlayerTaskTracker.matchesTask(zone.getMonster(), "Blue dragons"))
			{
				continue;
			}
			final long dx = zone.getX() - from.getX();
			final long dy = zone.getY() - from.getY();
			final long d = dx * dx + dy * dy;
			if (d < bestDist)
			{
				bestDist = d;
				nearest = zone;
			}
		}
		assertNotNull(nearest);

		final SlayerTaskTracker tracker = new SlayerTaskTracker(null, service("Blue dragons", null));
		final WorldPoint target = tracker.nearestTaskLocation(from, monsterIndex, new PoiIndex());

		assertNotNull(target);
		assertEquals(nearest.getX(), target.getX());
		assertEquals(nearest.getY(), target.getY());
	}

	private static MonsterIndex loadedMonsterIndex()
	{
		final MonsterIndex monsterIndex = new MonsterIndex(new Gson());
		monsterIndex.load();
		return monsterIndex;
	}
}
