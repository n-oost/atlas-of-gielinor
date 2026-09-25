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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import org.junit.Test;
import questhelper.tools.FakeQuestHelperWorldMapPoint;

public class QuestHelperTrackerTest
{
	private static WorldMapPoint pointNamed(String name)
	{
		final WorldMapPoint point = new WorldMapPoint(new WorldPoint(3200, 3200, 0), null);
		point.setName(name);
		return point;
	}

	@Test
	public void matchesTheNameQuestHelperSetsOnItsPoint()
	{
		assertTrue(QuestHelperTracker.isQuestHelperPoint(pointNamed("Quest Helper")));
	}

	@Test
	public void matchesOnThePackageWhenTheNameIsNotSet()
	{
		// The name is Quest Helper's to change; the class it registers is the durable signal.
		assertTrue(QuestHelperTracker.isQuestHelperPoint(
			new FakeQuestHelperWorldMapPoint(new WorldPoint(3200, 3200, 0))));
	}

	@Test
	public void ignoresEveryOtherPluginsPoint()
	{
		assertFalse(QuestHelperTracker.isQuestHelperPoint(pointNamed("Clue Scroll")));
		assertFalse(QuestHelperTracker.isQuestHelperPoint(pointNamed(null)));
		assertFalse(QuestHelperTracker.isQuestHelperPoint(null));
	}
}
