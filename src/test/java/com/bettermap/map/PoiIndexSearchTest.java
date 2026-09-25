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

import java.io.File;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

public class PoiIndexSearchTest
{
	private PoiIndex poiIndex;

	@Before
	public void setUp()
	{
		poiIndex = new PoiIndex();
		poiIndex.load(new File("nonexistent_dir"));
	}

	@Test
	public void searchByNameFindsCuratedEntriesCaseInsensitively()
	{
		final List<PoiIndex.Poi> upper = poiIndex.searchByName("COOK'S ASSISTANT", 50);
		final List<PoiIndex.Poi> lower = poiIndex.searchByName("cook's assistant", 50);

		assertFalse(upper.isEmpty());
		assertEquals(upper.size(), lower.size());
		assertEquals("Cook's Assistant", upper.get(0).getName());
	}

	@Test
	public void searchByNameHonoursCap()
	{
		final List<PoiIndex.Poi> capped = poiIndex.searchByName("a", 3);
		assertEquals(3, capped.size());
	}

	@Test
	public void searchByNameReturnsFreshCopy()
	{
		final List<PoiIndex.Poi> first = poiIndex.searchByName("Cook's Assistant", 50);
		final int initialSize = first.size();
		assertTrue(initialSize > 0);

		first.clear();
		assertEquals(0, first.size());

		final List<PoiIndex.Poi> second = poiIndex.searchByName("Cook's Assistant", 50);
		assertEquals(initialSize, second.size());
	}
}
