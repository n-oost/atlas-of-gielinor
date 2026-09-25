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
import java.util.List;
import java.util.Set;
import net.runelite.api.coords.WorldPoint;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class PortNoticeBoardTest
{
	@Test
	public void testAllBoardsIntegrity()
	{
		final List<PortNoticeBoard> boards = PortNoticeBoard.ALL_BOARDS;
		assertNotNull("Boards list should not be null", boards);
		assertTrue("Should have at least 20 defined port notice boards", boards.size() >= 20);

		final Set<String> seenNames = new HashSet<>();
		final Set<SailingPort> seenPorts = new HashSet<>();

		for (PortNoticeBoard board : boards)
		{
			assertNotNull("Board cannot be null", board);
			assertNotNull("Board name cannot be null", board.getName());
			assertFalse("Board name cannot be empty", board.getName().trim().isEmpty());
			assertTrue("Board name must start with 'Notice board — '", board.getName().startsWith("Notice board — "));
			assertTrue("Board name must be unique: " + board.getName(), seenNames.add(board.getName()));

			final SailingPort port = board.getPort();
			assertNotNull("Port association cannot be null for " + board.getName(), port);
			assertTrue("Port association must be unique in registry: " + port.getName(), seenPorts.add(port));
			assertEquals("Board forPort lookup should return this board", board, PortNoticeBoard.forPort(port));

			final WorldPoint loc = board.getLocation();
			assertNotNull("Board location cannot be null for " + board.getName(), loc);
			assertEquals("Board location plane must be surface 0", 0, loc.getPlane());
			assertTrue("Board X out of world bounds: " + loc.getX(), loc.getX() >= 1000 && loc.getX() <= 4000);
			assertTrue("Board Y out of world bounds: " + loc.getY(), loc.getY() >= 2000 && loc.getY() <= 4500);

			final List<String> couriers = board.getCourierTasks();
			assertNotNull("Courier tasks list cannot be null for " + board.getName(), couriers);
			assertFalse("Courier tasks must not be empty for " + board.getName(), couriers.isEmpty());
			assertTrue("Should have 3-6 sample courier tasks for " + board.getName(),
				couriers.size() >= 3 && couriers.size() <= 6);

			for (String courier : couriers)
			{
				assertNotNull("Courier task cannot be null", courier);
				assertFalse("Courier task cannot be empty", courier.trim().isEmpty());
				assertTrue("Courier task should describe route with '→ '", courier.startsWith("→ "));
			}

			final List<String> bounties = board.getBountyTasks();
			assertNotNull("Bounty tasks list cannot be null for " + board.getName(), bounties);
			if (board.hasBounties())
			{
				assertTrue("Should have 2-4 sample bounty tasks for " + board.getName(),
					bounties.size() >= 2 && bounties.size() <= 4);
				for (String bounty : bounties)
				{
					assertNotNull("Bounty task cannot be null", bounty);
					assertFalse("Bounty task cannot be empty", bounty.trim().isEmpty());
					assertTrue("Bounty task should specify level: " + bounty, bounty.contains("(Lvl "));
				}
			}
		}
	}

	@Test
	public void testSpatialLookups()
	{
		// Exact Port Sarim notice board location: (3045, 3205, 0)
		final PortNoticeBoard sarimBoard = PortNoticeBoard.findBoardNear(3045, 3205, 0, 5);
		assertNotNull("Should find Port Sarim notice board at exact location", sarimBoard);
		assertEquals("Notice board — Port Sarim", sarimBoard.getName());
		assertEquals(SailingPort.PORT_SARIM, sarimBoard.getPort());

		// Near Port Sarim (3 tiles away)
		final PortNoticeBoard nearSarim = PortNoticeBoard.findBoardNear(3048, 3206, 0, 5);
		assertNotNull("Should find Port Sarim notice board within 5 tiles", nearSarim);
		assertEquals(sarimBoard, nearSarim);

		// Far from Port Sarim (outside radius)
		final PortNoticeBoard farSarim = PortNoticeBoard.findBoardNear(3045, 3205, 0, 0);
		assertEquals(sarimBoard, farSarim);

		final PortNoticeBoard tooFar = PortNoticeBoard.findBoardNear(3000, 3205, 0, 5);
		assertNull("Should return null when too far from any board", tooFar);

		// Different plane should return null
		final PortNoticeBoard wrongPlane = PortNoticeBoard.findBoardNear(3045, 3205, 1, 10);
		assertNull("Should return null on non-surface plane", wrongPlane);
	}

	@Test
	public void testMajorPortNoticeBoardsPresent()
	{
		assertNotNull("Port Sarim board must exist", PortNoticeBoard.forPort(SailingPort.PORT_SARIM));
		assertNotNull("Pandemonium board must exist", PortNoticeBoard.forPort(SailingPort.THE_PANDEMONIUM));
		assertNotNull("Catherby board must exist", PortNoticeBoard.forPort(SailingPort.CATHERBY));
		assertNotNull("Port Khazard board must exist", PortNoticeBoard.forPort(SailingPort.PORT_KHAZARD));
		assertNotNull("Port Piscarillius board must exist", PortNoticeBoard.forPort(SailingPort.PORT_PISCARILLIUS));
		assertNotNull("Port Roberts board must exist", PortNoticeBoard.forPort(SailingPort.PORT_ROBERTS));
		assertNotNull("Port Tyras board must exist", PortNoticeBoard.forPort(SailingPort.PORT_TYRAS));
		assertNotNull("Rellekka board must exist", PortNoticeBoard.forPort(SailingPort.RELLEKKA));
		assertNotNull("Brimhaven board must exist", PortNoticeBoard.forPort(SailingPort.BRIMHAVEN));
		assertNotNull("Civitas illa Fortis board must exist", PortNoticeBoard.forPort(SailingPort.CIVITAS_ILLA_FORTIS));
	}
}
