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
package atlasofgielinor.data.sailing;

import org.junit.Test;

import atlasofgielinor.data.sailing.BoatType;
import atlasofgielinor.data.sailing.PlayerBoat;
import atlasofgielinor.data.sailing.SailingPort;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PlayerBoatTest
{
	@Test
	public void testBoatInitialization()
	{
		for (int i = 1; i <= 5; i++)
		{
			final PlayerBoat boat = new PlayerBoat(i);
			assertEquals(i, boat.getBoatId());
			assertEquals("Boat " + i, boat.getBoatName());
			assertEquals(-1f, boat.getHealth(), 0.001f);
			assertFalse(boat.isOwned());
			assertEquals(0, boat.getPortId());
		}
	}

	@Test
	public void testBoatSettersAndGetters()
	{
		final PlayerBoat boat = new PlayerBoat(1);
		boat.setBoatName("The Salty Dog");
		assertEquals("The Salty Dog", boat.getBoatName());

		boat.setBoatType(BoatType.SLOOP);
		assertEquals(BoatType.SLOOP, boat.getBoatType());
		assertEquals("Sloop", boat.getBoatType().getName());

		boat.setPort(SailingPort.PORT_SARIM);
		boat.setPortId(SailingPort.PORT_SARIM.getId());
		assertEquals(SailingPort.PORT_SARIM, boat.getPort());
		assertEquals(0, boat.getPortId());

		boat.setHealth(0.85f);
		assertEquals(0.85f, boat.getHealth(), 0.001f);

		boat.setOwned(true);
		assertTrue(boat.isOwned());
	}

	@Test
	public void testBoatTypeLookups()
	{
		assertEquals(BoatType.RAFT, BoatType.fromId(0));
		assertEquals(BoatType.SKIFF, BoatType.fromId(1));
		assertEquals(BoatType.SLOOP, BoatType.fromId(2));
		assertEquals(BoatType.TUTORIAL, BoatType.fromId(3));
		assertNotNull(BoatType.fromDBRow(8110));
	}
}
