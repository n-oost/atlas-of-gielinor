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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import atlasofgielinor.tiles.DungeonVoid;

public class DungeonVoidTest
{
	@Test
	public void exactBlackAndNearBlackAreVoid()
	{
		assertTrue(DungeonVoid.isVoid(0xFF000000));
		assertTrue(DungeonVoid.isVoid(0xFF000001));
		assertTrue(DungeonVoid.isVoid(0xFF010101));
		assertTrue(DungeonVoid.isVoid(0xFF020202));
		assertTrue(DungeonVoid.isVoid(0x020202));
		assertTrue(DungeonVoid.isVoid(0xFF030303));
		assertTrue(DungeonVoid.isVoid(0xFF030202));
		assertTrue(DungeonVoid.isVoid(0xFF362509));
		assertFalse(DungeonVoid.isVoid(0xFF040303));
		assertFalse(DungeonVoid.isVoid(0xFF060404));
		assertFalse(DungeonVoid.isVoid(0xFF0C0C0C));
	}

	@Test
	public void nonBlackDarkPixelsAreKept()
	{
		assertFalse(DungeonVoid.isVoid(0xFF0D0000));
		assertFalse(DungeonVoid.isVoid(0xFF000D00));
		assertFalse(DungeonVoid.isVoid(0xFF00000D));
		assertFalse(DungeonVoid.isVoid(0xFF040201));
		assertFalse(DungeonVoid.isVoid(0xFF020403));
	}

	@Test
	public void sewerCanvasBrownIsVoid()
	{
		assertTrue(DungeonVoid.isVoid(0x362509));
	}

	@Test
	public void neighbouringBrownsAndFloorTonesAreKept()
	{
		assertFalse(DungeonVoid.isVoid(0xFF36250A));
		assertFalse(DungeonVoid.isVoid(0xFF38240D));
		assertFalse(DungeonVoid.isVoid(0xFF8A7A5A));
	}
}
