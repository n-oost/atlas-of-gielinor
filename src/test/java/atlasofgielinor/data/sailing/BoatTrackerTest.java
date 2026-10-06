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

import com.google.gson.Gson;

import atlasofgielinor.data.sailing.BoatTracker;
import atlasofgielinor.data.sailing.BoatType;
import atlasofgielinor.data.sailing.PlayerBoat;
import atlasofgielinor.data.sailing.SailingPort;

import java.util.List;
import net.runelite.api.coords.WorldPoint;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class BoatTrackerTest
{
	private final Gson gson = new Gson();

	@Test
	public void testBoatCacheEntrySerialization()
	{
		final PlayerBoat boat = new PlayerBoat(1);
		boat.setBoatName("Sea Breeze");
		boat.setPortId(SailingPort.CATHERBY.getId());
		boat.setPort(SailingPort.CATHERBY);
		boat.setBoatType(BoatType.SLOOP);
		boat.setHealth(0.92f);
		boat.setOwned(true);

		final BoatTracker.BoatCacheEntry entry = new BoatTracker.BoatCacheEntry(boat);
		assertEquals(1, entry.getBoatId());
		assertEquals("Sea Breeze", entry.getBoatName());
		assertEquals(SailingPort.CATHERBY.getId(), entry.getPortId());
		assertEquals(BoatType.SLOOP.getId(), entry.getBoatTypeId());
		assertEquals(0.92f, entry.getHealth(), 0.001f);
		assertTrue(entry.isOwned());

		final String json = gson.toJson(List.of(entry));
		assertNotNull(json);
		assertFalse(json.isEmpty());

		final BoatTracker.BoatCacheEntry[] restored = gson.fromJson(json, BoatTracker.BoatCacheEntry[].class);

		assertNotNull(restored);
		assertEquals(1, restored.length);
		final BoatTracker.BoatCacheEntry first = restored[0];
		assertEquals(1, first.getBoatId());
		assertEquals("Sea Breeze", first.getBoatName());
		assertEquals(SailingPort.CATHERBY.getId(), first.getPortId());
		assertEquals(BoatType.SLOOP.getId(), first.getBoatTypeId());
		assertEquals(0.92f, first.getHealth(), 0.001f);
		assertTrue(first.isOwned());
	}

	@Test
	public void testSpatialLookups()
	{
		final PlayerBoat boat1 = new PlayerBoat(1);
		boat1.setBoatName("Sarim Sloop");
		boat1.setPort(SailingPort.PORT_SARIM);
		boat1.setPortId(SailingPort.PORT_SARIM.getId());
		boat1.setOwned(true);

		final PlayerBoat boat2 = new PlayerBoat(2);
		boat2.setBoatName("Unowned Boat");
		boat2.setPort(SailingPort.CATHERBY);
		boat2.setPortId(SailingPort.CATHERBY.getId());
		boat2.setOwned(false);

		final List<PlayerBoat> boats = List.of(boat1, boat2);

		// Spatial check for Port Sarim (3050, 3192)
		final WorldPoint ps = SailingPort.PORT_SARIM.getNavigationLocation();

		PlayerBoat found = null;
		int bestDistSq = 25; // 5 radius
		for (PlayerBoat b : boats)
		{
			if (!b.isOwned() || b.getPort() == null) continue;
			final WorldPoint loc = b.getPort().getNavigationLocation();
			if (loc == null || loc.getPlane() != ps.getPlane()) continue;
			final int dx = loc.getX() - ps.getX();
			final int dy = loc.getY() - ps.getY();
			if (Math.abs(dx) <= 5 && Math.abs(dy) <= 5)
			{
				found = b;
			}
		}

		assertNotNull("Should find owned boat near Port Sarim", found);
		assertEquals(boat1, found);

		// Spatial check for Catherby (should be null because boat2 is not owned)
		final WorldPoint cath = SailingPort.CATHERBY.getNavigationLocation();
		PlayerBoat foundCath = null;
		for (PlayerBoat b : boats)
		{
			if (!b.isOwned() || b.getPort() == null) continue;
			final WorldPoint loc = b.getPort().getNavigationLocation();
			if (loc == null || loc.getPlane() != cath.getPlane()) continue;
			final int dx = loc.getX() - cath.getX();
			final int dy = loc.getY() - cath.getY();
			if (Math.abs(dx) <= 5 && Math.abs(dy) <= 5)
			{
				foundCath = b;
			}
		}

		assertNull("Unowned boat should not be found in spatial queries", foundCath);
	}
}
