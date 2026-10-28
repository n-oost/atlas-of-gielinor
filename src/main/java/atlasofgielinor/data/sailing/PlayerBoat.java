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

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.api.gameval.DBTableID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginManager;

@Getter
public class PlayerBoat
{
	private static final int[][] BOAT_VARBITS = {
		{VarbitID.SAILING_BOAT_1_NAME_1, VarbitID.SAILING_BOAT_1_NAME_2, VarbitID.SAILING_BOAT_1_NAME_3, VarbitID.SAILING_BOAT_1_PORT, VarbitID.SAILING_BOAT_1_STORED_HP, VarbitID.SAILING_BOAT_1_STORED_MAXHP, VarbitID.SAILING_BOAT_1_OWNED, VarbitID.SAILING_BOAT_1_TYPE},
		{VarbitID.SAILING_BOAT_2_NAME_1, VarbitID.SAILING_BOAT_2_NAME_2, VarbitID.SAILING_BOAT_2_NAME_3, VarbitID.SAILING_BOAT_2_PORT, VarbitID.SAILING_BOAT_2_STORED_HP, VarbitID.SAILING_BOAT_2_STORED_MAXHP, VarbitID.SAILING_BOAT_2_OWNED, VarbitID.SAILING_BOAT_2_TYPE},
		{VarbitID.SAILING_BOAT_3_NAME_1, VarbitID.SAILING_BOAT_3_NAME_2, VarbitID.SAILING_BOAT_3_NAME_3, VarbitID.SAILING_BOAT_3_PORT, VarbitID.SAILING_BOAT_3_STORED_HP, VarbitID.SAILING_BOAT_3_STORED_MAXHP, VarbitID.SAILING_BOAT_3_OWNED, VarbitID.SAILING_BOAT_3_TYPE},
		{VarbitID.SAILING_BOAT_4_NAME_1, VarbitID.SAILING_BOAT_4_NAME_2, VarbitID.SAILING_BOAT_4_NAME_3, VarbitID.SAILING_BOAT_4_PORT, VarbitID.SAILING_BOAT_4_STORED_HP, VarbitID.SAILING_BOAT_4_STORED_MAXHP, VarbitID.SAILING_BOAT_4_OWNED, VarbitID.SAILING_BOAT_4_TYPE},
		{VarbitID.SAILING_BOAT_5_NAME_1, VarbitID.SAILING_BOAT_5_NAME_2, VarbitID.SAILING_BOAT_5_NAME_3, VarbitID.SAILING_BOAT_5_PORT, VarbitID.SAILING_BOAT_5_STORED_HP, VarbitID.SAILING_BOAT_5_STORED_MAXHP, VarbitID.SAILING_BOAT_5_OWNED, VarbitID.SAILING_BOAT_5_TYPE}
	};
	private static final int[] NAME_ROWS = {
		DBTableID.SailingBoatNameOptions.Row.SAILING_BOAT_NAME_PREFIX_OPTIONS,
		DBTableID.SailingBoatNameOptions.Row.SAILING_BOAT_NAME_DESCRIPTOR_OPTIONS,
		DBTableID.SailingBoatNameOptions.Row.SAILING_BOAT_NAME_NOUN_OPTIONS
	};

	private final int boatId;

	@Setter private String boatName;
	@Setter private int portId;
	@Setter private SailingPort port;
	@Setter private BoatType boatType;
	@Setter private float health = -1f;
	@Setter private boolean owned;

	private int name1Vb;
	private int name2Vb;
	private int name3Vb;
	private int portVb;
	private int hpVb;
	private int maxHpVb;
	private int ownedVb;
	private int typeVb;

	public PlayerBoat(int boatId)
	{
		this.boatId = boatId;
		this.boatName = "Boat " + boatId;
		if (boatId >= 1 && boatId <= BOAT_VARBITS.length)
		{
			final int[] vbs = BOAT_VARBITS[boatId - 1];
			name1Vb = vbs[0];
			name2Vb = vbs[1];
			name3Vb = vbs[2];
			portVb = vbs[3];
			hpVb = vbs[4];
			maxHpVb = vbs[5];
			ownedVb = vbs[6];
			typeVb = vbs[7];
		}
	}

	boolean updateForVarbit(int id, Client client, PluginManager pluginManager, ConfigManager configManager)
	{
		if (id == name1Vb || id == name2Vb || id == name3Vb) updateName(client, pluginManager, configManager);
		else if (id == portVb) updatePort(client);
		else if (id == hpVb || id == maxHpVb) updateHealth(client);
		else if (id == ownedVb) updateOwned(client);
		else if (id == typeVb) updateType(client);
		else return false;
		return true;
	}

	public void updateAll(Client client, PluginManager pluginManager, ConfigManager configManager)
	{
		if (client == null) return;
		updateName(client, pluginManager, configManager);
		updatePort(client);
		updateHealth(client);
		updateOwned(client);
		updateType(client);
	}

	public void updateName(Client client, PluginManager pluginManager, ConfigManager configManager)
	{
		if (pluginManager != null && configManager != null)
		{
			for (Plugin plugin : pluginManager.getPlugins())
			{
				if ("ShipRenamerPlugin".equals(plugin.getClass().getSimpleName()))
				{
					if (pluginManager.isPluginActive(plugin))
					{
						final String rename = configManager.getConfiguration("shiprenamer", "ship" + boatId + "name");
						if (rename != null && !rename.trim().isEmpty())
						{
							this.boatName = rename.trim();
							return;
						}
					}
					break;
				}
			}
		}

		if (client == null) return;

		final int[] vals = {client.getVarbitValue(name1Vb), client.getVarbitValue(name2Vb), client.getVarbitValue(name3Vb)};
		if ((vals[0] + vals[1] + vals[2]) == 0)
		{
			this.boatName = "Boat " + boatId;
			return;
		}

		final List<String> parts = new ArrayList<>();
		try
		{
			for (int i = 0; i < NAME_ROWS.length; i++)
			{
				final int idx = vals[i] - 1;
				if (idx >= 0)
				{
					final Object[] opts = client.getDBTableField(NAME_ROWS[i], DBTableID.SailingBoatNameOptions.COL_OPTION, 0);
					if (opts != null && idx < opts.length && opts[idx] instanceof String)
					{
						final String s = (String) opts[idx];
						if (!s.isEmpty()) parts.add(s);
					}
				}
			}
		}
		catch (Throwable ignored)
		{
		}

		this.boatName = parts.isEmpty() ? "Boat " + boatId : String.join(" ", parts);
	}

	public void updatePort(Client client)
	{
		if (client == null) return;
		this.portId = client.getVarbitValue(portVb);
		this.port = SailingPort.fromId(portId);
	}

	public void updateHealth(Client client)
	{
		if (client == null) return;
		final int hp = client.getVarbitValue(hpVb);
		final int maxHp = client.getVarbitValue(maxHpVb);
		this.health = maxHp > 0 ? (float) hp / (float) maxHp : -1f;
	}

	public void updateOwned(Client client)
	{
		if (client == null) return;
		this.owned = client.getVarbitValue(ownedVb) > 0
			|| client.getVarbitValue(portVb) > 0
			|| client.getVarbitValue(name2Vb) > 0;
	}

	public void updateType(Client client)
	{
		if (client == null) return;
		this.boatType = BoatType.fromId(client.getVarbitValue(typeVb));
	}
}
