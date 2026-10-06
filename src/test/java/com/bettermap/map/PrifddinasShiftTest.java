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

import net.runelite.api.coords.WorldPoint;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import bettermap.map.PrifddinasShift;

public class PrifddinasShiftTest
{
	@org.junit.BeforeClass
	public static void loadPlacement()
	{
		PrifddinasShift.load();
		assertTrue(PrifddinasShift.isLoaded());
	}

	@Test
	public void gwenithCowsGiveTheMeasuredOffset()
	{
		assertEquals(1024, 3213 - 2189);
		assertEquals(2752, 6170 - 3418);
		assertEquals(PrifddinasShift.get().getOffsetX(), 3213 - 2189);
		assertEquals(PrifddinasShift.get().getOffsetY(), 6170 - 3418);
	}

	@Test
	public void towerOfVoicesLandsOnTheOverworldCityCentre()
	{
		assertEquals(2240, PrifddinasShift.toDisplayX(3264, 6080));
		assertEquals(3328, PrifddinasShift.toDisplayY(3264, 6080));
		assertTrue(PrifddinasShift.containsInstance(3264, 6080));
		assertTrue(PrifddinasShift.containsOverworld(2240, 3328));
	}

	@Test
	public void lletyaIsOutsideTheOverlaySlot()
	{
		assertFalse(PrifddinasShift.containsOverworld(2346, 3180));
		assertFalse(PrifddinasShift.containsInstance(2346 + PrifddinasShift.get().getOffsetX(), 3180 + PrifddinasShift.get().getOffsetY()));
		assertEquals(2346, PrifddinasShift.toDisplayX(2346, 3180));
		assertEquals(3180, PrifddinasShift.toDisplayY(2346, 3180));
	}

	@Test
	public void lumbridgeIsUnchanged()
	{
		assertEquals(3222, PrifddinasShift.toDisplayX(3222, 3218));
		assertEquals(3218, PrifddinasShift.toDisplayY(3222, 3218));
		assertEquals(3222d, PrifddinasShift.toWorldX(3222, 3218), 0);
		assertEquals(3218d, PrifddinasShift.toWorldY(3222, 3218), 0);
	}

	@Test
	public void displayAndWorldRoundTrip()
	{
		final int ix = 3253;
		final int iy = 6109;
		final int dx = PrifddinasShift.toDisplayX(ix, iy);
		final int dy = PrifddinasShift.toDisplayY(ix, iy);
		final WorldPoint back = PrifddinasShift.toWorld(dx, dy, 0);
		assertEquals(ix, back.getX());
		assertEquals(iy, back.getY());
	}

	@Test
	public void overworldViewAlsoQueriesTheInstanceBox()
	{
		assertTrue(PrifddinasShift.overlapsOverworld(2112, 2368, 3200, 3456));
		assertTrue(PrifddinasShift.overlapsOverworld(2200, 2300, 3280, 3360));
		assertFalse(PrifddinasShift.overlapsOverworld(3200, 3240, 3200, 3240));
	}

	@Test
	public void firstHitPrefersDisplayThenInstance()
	{
		final int[] seen = {0, 0};
		final String hit = PrifddinasShift.firstHit(2240, 3328, (x, y) ->
		{
			if (x == 2240 && y == 3328)
			{
				seen[0]++;
				return null;
			}
			if (x == 3264 && y == 6080)
			{
				seen[1]++;
				return "instance";
			}
			return null;
		});
		assertEquals("instance", hit);
		assertEquals(1, seen[0]);
		assertEquals(1, seen[1]);
	}

	@Test
	public void instancePointsCountAsInTheOverworldView()
	{
		assertTrue(PrifddinasShift.inView(3264, 6080, 2112, 2368, 3200, 3456));
		assertFalse(PrifddinasShift.inView(3264, 6080, 3200, 3240, 3200, 3240));
	}
}
