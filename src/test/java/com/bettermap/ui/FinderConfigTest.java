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
package com.bettermap.ui;

import java.lang.reflect.Method;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;

import bettermap.BetterMapConfig;

public class FinderConfigTest
{
	@Test
	public void finderConfigMethodsExistAndDefaultToTrue() throws Exception
	{
		final BetterMapConfig config = new BetterMapConfig()
		{
		};

		assertBooleanConfig(config, "showFinderButton");
	}

	@Test
	public void zoomThresholdsAreIntsNotDoubles() throws Exception
	{
		final BetterMapConfig config = new BetterMapConfig()
		{
		};

		assertIntConfig(config, "monsterZoneMinZoom", 7);
		assertIntConfig(config, "groundItemMinZoom", 9);
	}

	private static void assertBooleanConfig(BetterMapConfig config, String key) throws Exception
	{
		final Method method;
		try
		{
			method = BetterMapConfig.class.getMethod(key);
		}
		catch (NoSuchMethodException e)
		{
			fail("no BetterMapConfig." + key + "()");
			return;
		}

		final Class<?> returnType = method.getReturnType();
		assertTrue(key + " must return boolean, was " + returnType,
			returnType == boolean.class || returnType == Boolean.class);

		final Object value = method.invoke(config);
		assertEquals(key + " must default to true", Boolean.TRUE, value);
	}

	private static void assertIntConfig(BetterMapConfig config, String key, int expected) throws Exception
	{
		final Method method;
		try
		{
			method = BetterMapConfig.class.getMethod(key);
		}
		catch (NoSuchMethodException e)
		{
			fail("no BetterMapConfig." + key + "()");
			return;
		}

		assertEquals(key + " must return int", int.class, method.getReturnType());
		assertEquals(key + " default", expected, method.invoke(config));
	}
}
