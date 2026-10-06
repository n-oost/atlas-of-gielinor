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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.PluginChanged;
import net.runelite.client.plugins.PluginManager;

@Slf4j
@Singleton
public class BoatTracker
{
	private static final String CONFIG_GROUP = "bettermap";
	private static final String CONFIG_KEY_CACHE = "boat_cache";
	private static final int NUM_BOATS = 5;

	private final Client client;
	private final ClientThread clientThread;
	private final PluginManager pluginManager;
	private final ConfigManager configManager;
	private final Gson gson;

	private final PlayerBoat[] boats = new PlayerBoat[NUM_BOATS];

	public static class BoatCacheEntry
	{
		@Getter @Setter private int boatId;
		@Getter @Setter private String boatName;
		@Getter @Setter private int portId;
		@Getter @Setter private int boatTypeId = -1;
		@Getter @Setter private float health = -1f;
		@Getter @Setter private boolean owned;

		public BoatCacheEntry()
		{
		}

		public BoatCacheEntry(PlayerBoat boat)
		{
			this.boatId = boat.getBoatId();
			this.boatName = boat.getBoatName();
			this.portId = boat.getPortId();
			this.boatTypeId = boat.getBoatType() != null ? boat.getBoatType().getId() : -1;
			this.health = boat.getHealth();
			this.owned = boat.isOwned();
		}
	}

	@Inject
	public BoatTracker(
		Client client,
		ClientThread clientThread,
		PluginManager pluginManager,
		ConfigManager configManager,
		Gson gson)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.pluginManager = pluginManager;
		this.configManager = configManager;
		this.gson = gson;

		for (int i = 0; i < NUM_BOATS; i++)
		{
			boats[i] = new PlayerBoat(i + 1);
		}

		loadCache();
	}

	public void loadCache()
	{
		try
		{
			final String json = configManager.getConfiguration(CONFIG_GROUP, CONFIG_KEY_CACHE);
			if (json == null || json.trim().isEmpty())
			{
				return;
			}

			final BoatCacheEntry[] cached = gson.fromJson(json, BoatCacheEntry[].class);
			if (cached == null)
			{
				return;
			}

			for (BoatCacheEntry entry : cached)
			{
				final int idx = entry.getBoatId() - 1;
				if (idx >= 0 && idx < NUM_BOATS)
				{
					final PlayerBoat boat = boats[idx];
					if (entry.getBoatName() != null && !entry.getBoatName().isEmpty())
					{
						boat.setBoatName(entry.getBoatName());
					}
					boat.setPortId(entry.getPortId());
					boat.setPort(SailingPort.fromId(entry.getPortId()));
					if (entry.getBoatTypeId() >= 0)
					{
						boat.setBoatType(BoatType.fromId(entry.getBoatTypeId()));
					}
					boat.setHealth(entry.getHealth());
					boat.setOwned(entry.isOwned());
				}
			}
		}
		catch (Exception e)
		{
			log.warn("[AtlasOfGielinor] Failed to load boat cache", e);
		}
	}

	public void saveCache()
	{
		try
		{
			final List<BoatCacheEntry> entries = new ArrayList<>(NUM_BOATS);
			for (PlayerBoat boat : boats)
			{
				entries.add(new BoatCacheEntry(boat));
			}
			final String json = gson.toJson(entries);
			configManager.setConfiguration(CONFIG_GROUP, CONFIG_KEY_CACHE, json);
		}
		catch (Exception e)
		{
			log.warn("[AtlasOfGielinor] Failed to save boat cache", e);
		}
	}

	public void refreshAll()
	{
		if (client == null || client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		for (PlayerBoat boat : boats)
		{
			boat.updateAll(client, pluginManager, configManager);
		}
		saveCache();
	}

	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			clientThread.invokeLater(this::refreshAll);
		}
	}

	public void onVarbitChanged(VarbitChanged event)
	{
		final int id = event.getVarbitId();
		boolean changed = false;

		for (PlayerBoat boat : boats)
		{
			changed |= boat.updateForVarbit(id, client, pluginManager, configManager);
		}

		if (changed)
		{
			saveCache();
		}
	}

	public void onConfigChanged(ConfigChanged event)
	{
		if ("shiprenamer".equals(event.getGroup()))
		{
			clientThread.invokeLater(() ->
			{
				for (PlayerBoat boat : boats)
				{
					boat.updateName(client, pluginManager, configManager);
				}
				saveCache();
			});
		}
	}

	public void onPluginChanged(PluginChanged event)
	{
		if (event.getPlugin() != null && "ShipRenamerPlugin".equals(event.getPlugin().getClass().getSimpleName()))
		{
			clientThread.invokeLater(() ->
			{
				for (PlayerBoat boat : boats)
				{
					boat.updateName(client, pluginManager, configManager);
				}
				saveCache();
			});
		}
	}

	public List<PlayerBoat> getOwnedBoats()
	{
		final List<PlayerBoat> owned = new ArrayList<>();
		for (PlayerBoat boat : boats)
		{
			if (boat != null && boat.isOwned())
			{
				owned.add(boat);
			}
		}
		return Collections.unmodifiableList(owned);
	}

	public List<PlayerBoat> getBoatsAt(SailingPort port)
	{
		if (port == null)
		{
			return Collections.emptyList();
		}
		final List<PlayerBoat> matching = new ArrayList<>();
		for (PlayerBoat boat : boats)
		{
			if (boat != null && boat.isOwned() && boat.getPort() == port)
			{
				matching.add(boat);
			}
		}
		return matching;
	}

	public PlayerBoat getBoatNear(int worldX, int worldY, int plane, int maxRadius)
	{
		PlayerBoat best = null;
		int bestDistSq = maxRadius * maxRadius;

		for (PlayerBoat boat : boats)
		{
			if (boat == null || !boat.isOwned() || boat.getPort() == null)
			{
				continue;
			}
			final WorldPoint loc = boat.getPort().getNavigationLocation();
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
				best = boat;
			}
		}

		return best;
	}
}
