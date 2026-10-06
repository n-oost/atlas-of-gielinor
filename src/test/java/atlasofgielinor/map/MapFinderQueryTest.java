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
package atlasofgielinor.map;

import com.google.gson.Gson;

import atlasofgielinor.map.MapFinder;
import atlasofgielinor.map.catalog.MonsterIndex;
import atlasofgielinor.map.catalog.PoiIndex;

import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

/** Query state mirrored from RuneLite's chatbox text input. */
public class MapFinderQueryTest
{
	private MapFinder finder;

	@Before
	public void setUp()
	{
		finder = new MapFinder(new PoiIndex(), new MonsterIndex(new Gson()));
	}

	private void type(String text)
	{
		for (char c : text.toCharArray())
		{
			finder.updateQuery(finder.getQuery() + c, null);
		}
	}

	/**
	 * updateQuery used to trim on every keystroke, so a typed trailing space vanished and the next
	 * character joined the previous word: "king " + 'b' came back as "kingb". Multi-word queries
	 * were unreachable by typing at all.
	 */
	@Test
	public void typingASpaceSurvivesSoMultiWordQueriesAreReachable()
	{
		type("king black");
		assertEquals("king black", finder.getQuery());

		finder.clearQuery();
		type("king ");
		assertEquals("king ", finder.getQuery());
		finder.updateQuery("king b", null);
		assertEquals("king b", finder.getQuery());
	}

	@Test
	public void queryIsTruncatedToTheLengthCap()
	{
		final StringBuilder tooLong = new StringBuilder();
		for (int i = 0; i < MapFinder.MAX_QUERY_LENGTH + 20; i++)
		{
			tooLong.append('x');
		}

		finder.updateQuery(tooLong.toString(), null);
		assertEquals(MapFinder.MAX_QUERY_LENGTH, finder.getQuery().length());
	}

	@Test
	public void clearQueryKeepsTheLastQueryForTheChip()
	{
		type("varrock");
		finder.clearQuery();

		assertEquals("", finder.getQuery());
		assertEquals("varrock", finder.getLastQuery());
	}

	@Test
	public void lastQueryIgnoresTheEmptyStringSoTheChipNeverGoesBlank()
	{
		type("falador");
		finder.updateQuery("", null);
		assertEquals("falador", finder.getLastQuery());
	}
}
