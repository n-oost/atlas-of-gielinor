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
package com.bettermap.tiles;

/**
 * Pixels the cache map bakes in as dungeon canvas, keyed out so the surface shows through.
 */
public final class DungeonVoid
{
	/**
	 * Solid canvas fill between rooms (Varrock Sewers, Edgeville Dungeon, and others). Exact
	 * {@code #362509} as the cache render writes it.
	 */
	private static final int VOID_BROWN = 0x362509;

	/**
	 * Dark void canvas fill baked into certain dungeon renders (exact {@code #020202}).
	 */
	private static final int VOID_DARK = 0x020202;

	private DungeonVoid()
	{
	}

	/** True when {@code argb} is dungeon void and should be made fully transparent. */
	public static boolean isVoid(int argb)
	{
		final int rgb = argb & 0xFFFFFF;
		return rgb == 0
			|| rgb == VOID_DARK
			|| rgb == VOID_BROWN;
	}
}
