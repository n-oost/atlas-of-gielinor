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

import atlasofgielinor.data.io.BundledTsv;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;

/**
 * Curated data for Port Notice Boards (task boards) across major Sailing ports in Old School RuneScape.
 * Provides approximate board coordinates, typical sample courier routes, and sample bounty targets.
 *
 * <p>Notice boards in-game rotate tasks after 8 completions or daily reset; these lists represent
 * curated static samples of typical tasks available from each port's board.
 */
@Getter
@Slf4j
public class PortNoticeBoard
{
	private final SailingPort port;
	private final String name;
	private final WorldPoint location;
	private final List<String> courierTasks;
	private final List<String> bountyTasks;
	private final int minSailingLevel;

	public PortNoticeBoard(
		SailingPort port,
		String name,
		WorldPoint location,
		List<String> courierTasks,
		List<String> bountyTasks,
		int minSailingLevel)
	{
		this.port = port;
		this.name = name;
		this.location = location;
		this.courierTasks = courierTasks != null ? Collections.unmodifiableList(new ArrayList<>(courierTasks)) : Collections.emptyList();
		this.bountyTasks = bountyTasks != null ? Collections.unmodifiableList(new ArrayList<>(bountyTasks)) : Collections.emptyList();
		this.minSailingLevel = minSailingLevel;
	}

	public boolean hasBounties()
	{
		return !bountyTasks.isEmpty();
	}

	public static volatile List<PortNoticeBoard> ALL_BOARDS = Collections.emptyList();
	private static volatile Map<SailingPort, PortNoticeBoard> byPort = Collections.emptyMap();

	public static synchronized boolean load()
	{
		if (!byPort.isEmpty()) return true;
		if (!SailingPort.load()) return false;
		final List<PortNoticeBoard> boards = new ArrayList<>();
		final Map<SailingPort, PortNoticeBoard> ports = new HashMap<>();
		try
		{
			for (String[] f : BundledTsv.read("/atlasofgielinor/sailing/port_notice_boards.tsv", 8))
			{
				if (f.length != 8) throw new IOException("Invalid port notice board row");
				final SailingPort port = SailingPort.fromId(Integer.parseInt(f[0]));
				if (port == null) throw new IOException("Unknown notice board port: " + f[0]);
				final PortNoticeBoard board = new PortNoticeBoard(port, f[1],
					new WorldPoint(Integer.parseInt(f[2]), Integer.parseInt(f[3]), Integer.parseInt(f[4])),
					tasks(f[6]), tasks(f[7]), Integer.parseInt(f[5]));
				if (ports.put(port, board) != null) throw new IOException("Duplicate notice board port: " + f[0]);
				boards.add(board);
			}
			if (boards.isEmpty()) throw new IOException("Empty port notice board catalog");
			if (Thread.currentThread().isInterrupted()) return false;
			ALL_BOARDS = Collections.unmodifiableList(boards);
			byPort = Collections.unmodifiableMap(ports);
			return true;
		}
		catch (IOException | RuntimeException e)
		{
			log.debug("Could not load port notice boards", e);
			return false;
		}
	}

	private static List<String> tasks(String field)
	{
		return field.isEmpty() ? Collections.emptyList() : List.of(field.split("\\|", -1));
	}

	public static PortNoticeBoard forPort(SailingPort port)
	{
		return byPort.get(port);
	}

	/**
	 * Finds the nearest port notice board to a given world coordinate within maxRadius tiles.
	 */
	public static PortNoticeBoard findBoardNear(int worldX, int worldY, int plane, int maxRadius)
	{
		if (plane != 0)
		{
			return null;
		}

		PortNoticeBoard best = null;
		int bestDistSq = maxRadius * maxRadius;

		for (PortNoticeBoard board : ALL_BOARDS)
		{
			final WorldPoint loc = board.getLocation();
			if (loc == null || loc.getPlane() != plane)
			{
				continue;
			}

			final int dx = loc.getX() - worldX;
			final int dy = loc.getY() - worldY;
			if (Math.abs(dx) > maxRadius || Math.abs(dy) > maxRadius)
			{
				continue;
			}
			final int distSq = dx * dx + dy * dy;

			if (distSq <= bestDistSq)
			{
				bestDistSq = distSq;
				best = board;
			}
		}

		return best;
	}
}
