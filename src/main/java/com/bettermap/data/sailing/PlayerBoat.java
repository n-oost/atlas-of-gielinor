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
		initVarbits();
	}

	private void initVarbits()
	{
		switch (boatId)
		{
			case 1:
				name1Vb = VarbitID.SAILING_BOAT_1_NAME_1;
				name2Vb = VarbitID.SAILING_BOAT_1_NAME_2;
				name3Vb = VarbitID.SAILING_BOAT_1_NAME_3;
				portVb = VarbitID.SAILING_BOAT_1_PORT;
				hpVb = VarbitID.SAILING_BOAT_1_STORED_HP;
				maxHpVb = VarbitID.SAILING_BOAT_1_STORED_MAXHP;
				ownedVb = VarbitID.SAILING_BOAT_1_OWNED;
				typeVb = VarbitID.SAILING_BOAT_1_TYPE;
				break;
			case 2:
				name1Vb = VarbitID.SAILING_BOAT_2_NAME_1;
				name2Vb = VarbitID.SAILING_BOAT_2_NAME_2;
				name3Vb = VarbitID.SAILING_BOAT_2_NAME_3;
				portVb = VarbitID.SAILING_BOAT_2_PORT;
				hpVb = VarbitID.SAILING_BOAT_2_STORED_HP;
				maxHpVb = VarbitID.SAILING_BOAT_2_STORED_MAXHP;
				ownedVb = VarbitID.SAILING_BOAT_2_OWNED;
				typeVb = VarbitID.SAILING_BOAT_2_TYPE;
				break;
			case 3:
				name1Vb = VarbitID.SAILING_BOAT_3_NAME_1;
				name2Vb = VarbitID.SAILING_BOAT_3_NAME_2;
				name3Vb = VarbitID.SAILING_BOAT_3_NAME_3;
				portVb = VarbitID.SAILING_BOAT_3_PORT;
				hpVb = VarbitID.SAILING_BOAT_3_STORED_HP;
				maxHpVb = VarbitID.SAILING_BOAT_3_STORED_MAXHP;
				ownedVb = VarbitID.SAILING_BOAT_3_OWNED;
				typeVb = VarbitID.SAILING_BOAT_3_TYPE;
				break;
			case 4:
				name1Vb = VarbitID.SAILING_BOAT_4_NAME_1;
				name2Vb = VarbitID.SAILING_BOAT_4_NAME_2;
				name3Vb = VarbitID.SAILING_BOAT_4_NAME_3;
				portVb = VarbitID.SAILING_BOAT_4_PORT;
				hpVb = VarbitID.SAILING_BOAT_4_STORED_HP;
				maxHpVb = VarbitID.SAILING_BOAT_4_STORED_MAXHP;
				ownedVb = VarbitID.SAILING_BOAT_4_OWNED;
				typeVb = VarbitID.SAILING_BOAT_4_TYPE;
				break;
			case 5:
				name1Vb = VarbitID.SAILING_BOAT_5_NAME_1;
				name2Vb = VarbitID.SAILING_BOAT_5_NAME_2;
				name3Vb = VarbitID.SAILING_BOAT_5_NAME_3;
				portVb = VarbitID.SAILING_BOAT_5_PORT;
				hpVb = VarbitID.SAILING_BOAT_5_STORED_HP;
				maxHpVb = VarbitID.SAILING_BOAT_5_STORED_MAXHP;
				ownedVb = VarbitID.SAILING_BOAT_5_OWNED;
				typeVb = VarbitID.SAILING_BOAT_5_TYPE;
				break;
			default:
				break;
		}
	}

	public void updateAll(Client client, PluginManager pluginManager, ConfigManager configManager)
	{
		if (client == null)
		{
			return;
		}
		updateName(client, pluginManager, configManager);
		updatePort(client);
		updateHealth(client);
		updateOwned(client);
		updateType(client);
	}

	public void updateName(Client client, PluginManager pluginManager, ConfigManager configManager)
	{
		// 1. ShipRenamerPlugin compatibility
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

		if (client == null)
		{
			return;
		}

		// 2. Standard Sailing Boat name lookup via client db table
		final int name1Val = client.getVarbitValue(name1Vb);
		final int name2Val = client.getVarbitValue(name2Vb);
		final int name3Val = client.getVarbitValue(name3Vb);

		if ((name1Val + name2Val + name3Val) == 0)
		{
			this.boatName = "Boat " + boatId;
			return;
		}

		final int name1Idx = name1Val - 1;
		final int name2Idx = name2Val - 1;
		final int name3Idx = name3Val - 1;

		final int optionCol = DBTableID.SailingBoatNameOptions.COL_OPTION;
		final List<String> parts = new ArrayList<>();

		try
		{
			if (name1Idx >= 0)
			{
				final Object[] prefixOpts = client.getDBTableField(
					DBTableID.SailingBoatNameOptions.Row.SAILING_BOAT_NAME_PREFIX_OPTIONS, optionCol, 0);
				if (prefixOpts != null && name1Idx < prefixOpts.length && prefixOpts[name1Idx] instanceof String)
				{
					final String s = (String) prefixOpts[name1Idx];
					if (s != null && !s.isEmpty())
					{
						parts.add(s);
					}
				}
			}

			if (name2Idx >= 0)
			{
				final Object[] descOpts = client.getDBTableField(
					DBTableID.SailingBoatNameOptions.Row.SAILING_BOAT_NAME_DESCRIPTOR_OPTIONS, optionCol, 0);
				if (descOpts != null && name2Idx < descOpts.length && descOpts[name2Idx] instanceof String)
				{
					final String s = (String) descOpts[name2Idx];
					if (s != null && !s.isEmpty())
					{
						parts.add(s);
					}
				}
			}

			if (name3Idx >= 0)
			{
				final Object[] nounOpts = client.getDBTableField(
					DBTableID.SailingBoatNameOptions.Row.SAILING_BOAT_NAME_NOUN_OPTIONS, optionCol, 0);
				if (nounOpts != null && name3Idx < nounOpts.length && nounOpts[name3Idx] instanceof String)
				{
					final String s = (String) nounOpts[name3Idx];
					if (s != null && !s.isEmpty())
					{
						parts.add(s);
					}
				}
			}
		}
		catch (Throwable ignored)
		{
		}

		if (!parts.isEmpty())
		{
			this.boatName = String.join(" ", parts);
		}
		else
		{
			this.boatName = "Boat " + boatId;
		}
	}

	public void updatePort(Client client)
	{
		if (client == null)
		{
			return;
		}
		final int portVal = client.getVarbitValue(portVb);
		this.portId = portVal;
		this.port = SailingPort.fromId(portVal);
	}

	public void updateHealth(Client client)
	{
		if (client == null)
		{
			return;
		}
		final int hp = client.getVarbitValue(hpVb);
		final int maxHp = client.getVarbitValue(maxHpVb);

		if (maxHp > 0)
		{
			this.health = (float) hp / (float) maxHp;
		}
		else
		{
			this.health = -1f;
		}
	}

	public void updateOwned(Client client)
	{
		if (client == null)
		{
			return;
		}
		this.owned = client.getVarbitValue(ownedVb) > 0
			|| client.getVarbitValue(portVb) > 0
			|| client.getVarbitValue(name2Vb) > 0;
	}

	public void updateType(Client client)
	{
		if (client == null)
		{
			return;
		}
		final int typeVal = client.getVarbitValue(typeVb);
		this.boatType = BoatType.fromId(typeVal);
	}
}
