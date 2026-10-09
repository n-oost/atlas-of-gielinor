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
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.DBTableID;

@Getter
@Slf4j
public final class SailingPort
{
	public static final int BOTTLED_PORT_ID = 255;
	public static final int CAPSIZED_PORT_ID = 254;
	public static volatile List<SailingPort> ALL_PORTS = Collections.emptyList();
	private static volatile Map<Integer, SailingPort> byId = Collections.emptyMap();

	/** Load bundled attributes on the map startup worker before rendering activates. */
	public static synchronized boolean load()
	{
		if (!byId.isEmpty()) return true;
		final Map<Integer, SailingPort> ids = new HashMap<>();
		final List<SailingPort> ports = new ArrayList<>();
		try
		{
			for (String[] f : BundledTsv.read("/atlasofgielinor/sailing/ports.tsv", 7))
			{
				if (f.length != 7) throw new IOException("Invalid sailing port row");
				final SailingPort port = new SailingPort(Integer.parseInt(f[0]), f[1], Integer.parseInt(f[2]),
					new WorldPoint(Integer.parseInt(f[3]), Integer.parseInt(f[4]), Integer.parseInt(f[5])), Integer.parseInt(f[6]));
				if (ids.put(port.id, port) != null) throw new IOException("Duplicate sailing port: " + port.id);
				ports.add(port);
			}
			if (ports.isEmpty()) throw new IOException("Empty sailing port catalog");
			if (Thread.currentThread().isInterrupted()) return false;
			ALL_PORTS = Collections.unmodifiableList(ports);
			byId = Collections.unmodifiableMap(ids);
			return true;
		}
		catch (IOException | RuntimeException e)
		{
			log.debug("Could not load sailing ports", e);
			return false;
		}
	}

	private final int id;
	private final String name;
	private final int sailingLevelRequired;
	private final WorldPoint navigationLocation;
	private final int spriteId;

	private SailingPort(int id, String name, int sailingLevelRequired, WorldPoint navigationLocation, int spriteId)
	{
		this.id = id;
		this.name = name;
		this.sailingLevelRequired = sailingLevelRequired;
		this.navigationLocation = navigationLocation;
		this.spriteId = spriteId;
	}

	public static SailingPort fromId(int portId)
	{
		return byId.get(portId);
	}

	public static String resolvePortName(Client client, int portId)
	{
		if (portId == BOTTLED_PORT_ID)
		{
			return "Bottled";
		}
		if (portId == CAPSIZED_PORT_ID)
		{
			return "Capsized";
		}

		final SailingPort port = fromId(portId);
		if (port != null)
		{
			return port.getName();
		}

		if (client != null)
		{
			try
			{
				var portRows = client.getDBRowsByValue(DBTableID.SailingDock.ID, DBTableID.SailingDock.COL_DOCK_ID, 0, portId);
				if (!portRows.isEmpty())
				{
					final int dbRow = portRows.get(0);
					final Object[] field = client.getDBTableField(dbRow, DBTableID.SailingDock.COL_NICE_NAME, 0);
					if (field != null && field.length > 0 && field[0] instanceof String)
					{
						return (String) field[0];
					}
				}
			}
			catch (Throwable ignored)
			{
			}
		}

		return "Dock #" + portId;
	}

	@Override
	public String toString()
	{
		return name;
	}

	/**
	 * Finds the nearest sailing port to a given world coordinate within maxRadius tiles.
	 */
	public static SailingPort findPortNear(int worldX, int worldY, int plane, int maxRadius)
	{
		if (plane != 0)
		{
			return null;
		}

		SailingPort best = null;
		int bestDistSq = maxRadius * maxRadius;

		for (SailingPort port : ALL_PORTS)
		{
			final WorldPoint loc = port.getNavigationLocation();
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
				best = port;
			}
		}

		return best;
	}
}
