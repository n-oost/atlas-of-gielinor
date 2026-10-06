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
package atlasofgielinor.map.catalog;

import java.io.File;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

import atlasofgielinor.map.catalog.PoiDetails;
import atlasofgielinor.map.catalog.PoiIndex;

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

	@Test
	public void barracudaTrialsAreIndexedAndSearchable()
	{
		final List<PoiIndex.Poi> trials = poiIndex.searchByName("barracuda", 50);
		assertTrue("Expected at least 3 Barracuda Trial results, got " + trials.size(), trials.size() >= 3);
		assertTrue(trials.stream().anyMatch(p -> p.getName().contains("Tempor Tantrum")));
		assertTrue(trials.stream().anyMatch(p -> p.getName().contains("Jubbly Jive")));
		assertTrue(trials.stream().anyMatch(p -> p.getName().contains("Gwenith Glide")));

		// Trial masters searchable in Finder
		assertFalse("Rum-dashed Ralph should be searchable", poiIndex.searchByName("Ralph", 10).isEmpty());
		assertFalse("Gurtob should be searchable", poiIndex.searchByName("Gurtob", 10).isEmpty());
		assertFalse("Gwyna should be searchable", poiIndex.searchByName("Gwyna", 10).isEmpty());

		// Details resolve with rich requirements
		final PoiDetails.Detail tempor = PoiDetails.getDetailByPosition(3033, 2929, 0, 1);
		assertNotNull(tempor);
		assertTrue(tempor.getLines().stream().anyMatch(l -> l.contains("Level 30 Sailing")));

		final PoiDetails.Detail jubbly = PoiDetails.getDetailByPosition(2437, 3027, 0, 1);
		assertNotNull(jubbly);
		assertTrue(jubbly.getLines().stream().anyMatch(l -> l.contains("Level 55 Sailing")));

		final PoiDetails.Detail gwenith = PoiDetails.getDetailByPosition(2198, 3518, 0, 1);
		assertNotNull(gwenith);
		assertTrue(gwenith.getLines().stream().anyMatch(l -> l.contains("Level 72 Sailing")));
	}
}
