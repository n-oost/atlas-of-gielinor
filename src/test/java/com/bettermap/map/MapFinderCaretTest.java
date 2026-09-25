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

import com.google.gson.Gson;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

/**
 * The search field is drawn pixels with a plain String behind it, so caret behaviour has no widget
 * to lean on and is pinned here instead.
 */
public class MapFinderCaretTest
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
			finder.appendChar(c, null);
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
		finder.appendChar('b', null);
		assertEquals("king b", finder.getQuery());
	}

	@Test
	public void appendingLeavesTheCaretAtTheEnd()
	{
		type("abc");
		assertEquals(3, finder.getCaret());
	}

	@Test
	public void insertHappensAtTheCaretNotTheEnd()
	{
		type("ac");
		finder.moveCaret(-1);
		assertEquals(1, finder.getCaret());

		finder.insert("b", null);
		assertEquals("abc", finder.getQuery());
		assertEquals(2, finder.getCaret());
	}

	@Test
	public void deleteBackwardAndForwardActRelativeToTheCaret()
	{
		type("abcd");
		finder.moveCaret(-2);      // a b | c d
		assertEquals(2, finder.getCaret());

		finder.deleteBackward(null);
		assertEquals("acd", finder.getQuery());
		assertEquals(1, finder.getCaret());

		finder.deleteForward(null);
		assertEquals("ad", finder.getQuery());
		assertEquals(1, finder.getCaret());
	}

	@Test
	public void deletingAtTheEdgesIsANoOp()
	{
		type("ab");
		finder.caretHome();
		finder.deleteBackward(null);
		assertEquals("ab", finder.getQuery());
		assertEquals(0, finder.getCaret());

		finder.caretEnd();
		finder.deleteForward(null);
		assertEquals("ab", finder.getQuery());
		assertEquals(2, finder.getCaret());
	}

	@Test
	public void caretClampsAtBothEnds()
	{
		type("ab");
		finder.moveCaret(-99);
		assertEquals(0, finder.getCaret());
		finder.moveCaret(99);
		assertEquals(2, finder.getCaret());
	}

	@Test
	public void caretIsClampedWhenTheQueryIsTruncatedToTheLengthCap()
	{
		final StringBuilder tooLong = new StringBuilder();
		for (int i = 0; i < MapFinder.MAX_QUERY_LENGTH + 20; i++)
		{
			tooLong.append('x');
		}

		finder.updateQuery(tooLong.toString(), null);
		assertEquals(MapFinder.MAX_QUERY_LENGTH, finder.getQuery().length());
		finder.caretEnd();
		assertEquals(MapFinder.MAX_QUERY_LENGTH, finder.getCaret());
	}

	@Test
	public void clearQueryResetsTheCaretButKeepsTheLastQueryForTheChip()
	{
		type("varrock");
		finder.clearQuery();

		assertEquals("", finder.getQuery());
		assertEquals(0, finder.getCaret());
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
