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
package com.bettermap.data.sailing;

import java.util.HashSet;
import java.util.Set;
import net.runelite.api.coords.WorldPoint;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SailingPortTest
{
	@Test
	public void testAllPortsIntegrity()
	{
		final SailingPort[] ports = SailingPort.values();
		assertEquals("Should have exactly 59 defined sailing ports", 59, ports.length);

		final Set<Integer> seenIds = new HashSet<>();
		final Set<String> seenNames = new HashSet<>();

		for (SailingPort port : ports)
		{
			assertNotNull("Port enum cannot be null", port);
			assertNotNull("Port name cannot be null", port.getName());
			assertFalse("Port name cannot be empty for " + port.name(), port.getName().trim().isEmpty());

			assertTrue("Port ID must be unique: " + port.getId(), seenIds.add(port.getId()));
			assertTrue("Port name must be unique: " + port.getName(), seenNames.add(port.getName()));

			assertTrue("Sailing level must be >= 1 for " + port.getName(), port.getSailingLevelRequired() >= 1);
			assertTrue("Sprite ID must be valid for " + port.getName(), port.getSpriteId() > 0);

			final WorldPoint loc = port.getNavigationLocation();
			assertNotNull("Port location cannot be null for " + port.getName(), loc);
			assertTrue("Port X out of world bounds: " + loc.getX() + " for " + port.getName(),
				loc.getX() >= 1000 && loc.getX() <= 4000);
			assertTrue("Port Y out of world bounds: " + loc.getY() + " for " + port.getName(),
				loc.getY() >= 2000 && loc.getY() <= 4500);
			assertEquals("Port plane should be surface (0) for " + port.getName(), 0, loc.getPlane());

			final SailingPort byId = SailingPort.fromId(port.getId());
			assertEquals("fromId lookup should return exact port", port, byId);
		}
	}

	@Test
	public void testSpecialPortResolutions()
	{
		assertEquals("Bottled", SailingPort.resolvePortName(null, SailingPort.BOTTLED_PORT_ID));
		assertEquals("Capsized", SailingPort.resolvePortName(null, SailingPort.CAPSIZED_PORT_ID));
		assertEquals("Port Sarim", SailingPort.resolvePortName(null, 0));
		assertEquals("The Pandemonium", SailingPort.resolvePortName(null, 1));
		assertEquals("Grimstone", SailingPort.resolvePortName(null, 58));
		assertEquals("Dock #999", SailingPort.resolvePortName(null, 999));
	}

	@Test
	public void testInvalidIdLookup()
	{
		assertNull(SailingPort.fromId(-1));
		assertNull(SailingPort.fromId(100));
		assertNull(SailingPort.fromId(255));
	}

	@Test
	public void testSpatialLookups()
	{
		// Exact Port Sarim navigation location: (3050, 3192, 0)
		final SailingPort portSarim = SailingPort.findPortNear(3050, 3192, 0, 5);
		assertNotNull("Should find Port Sarim at exact location", portSarim);
		assertEquals(SailingPort.PORT_SARIM, portSarim);

		// Near Port Sarim (3 tiles away)
		final SailingPort nearSarim = SailingPort.findPortNear(3053, 3190, 0, 5);
		assertNotNull("Should find Port Sarim within 5 tiles", nearSarim);
		assertEquals(SailingPort.PORT_SARIM, nearSarim);

		// Outside radius
		final SailingPort far = SailingPort.findPortNear(3000, 3192, 0, 5);
		assertNull("Should return null when outside radius", far);

		// Non-surface plane
		final SailingPort wrongPlane = SailingPort.findPortNear(3050, 3192, 1, 10);
		assertNull("Should return null on non-surface plane", wrongPlane);
	}
}
