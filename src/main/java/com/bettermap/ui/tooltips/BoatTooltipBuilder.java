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
package com.bettermap.ui.tooltips;

import com.bettermap.data.sailing.PlayerBoat;
import com.bettermap.data.sailing.PortNoticeBoard;
import com.bettermap.data.sailing.SailingPort;
import java.util.Collections;
import java.util.List;
import net.runelite.api.Client;

/**
 * Builds tooltip hovercards for Sailing elements: player boats, sailing ports,
 * and port notice boards.
 */
public class BoatTooltipBuilder
{
	/**
	 * Builds a tooltip card for a player's docked boat.
	 */
	public TooltipCard buildBoatCard(PlayerBoat boat, Client client, int plane)
	{
		if (boat == null || !boat.isOwned() || boat.getPort() == null)
		{
			return null;
		}

		final SailingPort port = boat.getPort();
		final String portName = port != null ? port.getName() : SailingPort.resolvePortName(client, boat.getPortId());
		final String typeStr = boat.getBoatType() != null ? boat.getBoatType().getName() : "Sailing Ship";
		final String boatTitle = boat.getBoatName() + " (Boat #" + boat.getBoatId() + ")";

		final TooltipCard card = new TooltipCard(boatTitle);
		card.addLine("[" + typeStr + " \u2022 Docked at " + portName + "]");
		if (port != null && port.getSailingLevelRequired() > 1)
		{
			card.addLine("Required: Level " + port.getSailingLevelRequired() + " Sailing");
		}
		if (boat.getHealth() >= 0)
		{
			final int pct = Math.round(boat.getHealth() * 100f);
			final String cond = pct >= 80 ? "Good" : (pct >= 40 ? "Damaged" : "Critical");
			card.addLine("Hull Condition: " + pct + "% (" + cond + ")");
		}
		if (port != null && port.getNavigationLocation() != null)
		{
			card.addLine(port.getNavigationLocation().getX() + ", " + port.getNavigationLocation().getY() + " (Floor " + plane + ")");
		}
		return card;
	}

	/**
	 * Builds a tooltip card for a port notice board with available sample tasks.
	 */
	public TooltipCard buildNoticeBoardCard(PortNoticeBoard noticeBoard, int plane)
	{
		if (noticeBoard == null)
		{
			return null;
		}

		final String boardTitle = noticeBoard.getName();
		final TooltipCard card = new TooltipCard(boardTitle);
		card.addLine("[Port tasks \u2022 sample]");

		final List<String> couriers = noticeBoard.getCourierTasks();
		if (couriers != null && !couriers.isEmpty())
		{
			card.addLine("Courier tasks (sample):");
			for (String courier : couriers)
			{
				card.addLine("  \u2022 " + courier);
			}
		}

		final List<String> bounties = noticeBoard.getBountyTasks();
		if (bounties != null && !bounties.isEmpty())
		{
			card.addLine("Bounty tasks (sample):");
			for (String bounty : bounties)
			{
				card.addLine("  \u2022 " + bounty);
			}
		}
		else
		{
			card.addLine("Bounty unlocks at Sailing 30");
		}

		card.addLine("Boards rotate after 8 tasks or daily reset");
		card.addLine(noticeBoard.getLocation().getX() + ", " + noticeBoard.getLocation().getY() + " (Floor " + plane + ")");
		return card;
	}

	/**
	 * Builds a tooltip card for a sailing port dock.
	 */
	public TooltipCard buildPortCard(SailingPort port, List<PlayerBoat> dockedBoats, int plane)
	{
		if (port == null)
		{
			return null;
		}

		final String portTitle = port.getName();
		final TooltipCard card = new TooltipCard(portTitle);
		final String sailingHeader = port.getSailingLevelRequired() > 1
			? "Level " + port.getSailingLevelRequired() + " Sailing"
			: "Open Dock";
		card.addLine("[Sailing Dock \u2022 " + sailingHeader + "]");

		final List<PlayerBoat> boats = dockedBoats != null ? dockedBoats : Collections.emptyList();
		if (!boats.isEmpty())
		{
			if (boats.size() == 1)
			{
				card.addLine("Docked boat: \u26f5 " + boats.get(0).getBoatName());
			}
			else
			{
				final StringBuilder sb = new StringBuilder("Docked boats (" + boats.size() + "): ");
				for (int i = 0; i < boats.size(); i++)
				{
					if (i > 0)
					{
						sb.append(", ");
					}
					sb.append("\u26f5 ").append(boats.get(i).getBoatName());
				}
				card.addLine(sb.toString());
			}
		}

		if (port.getNavigationLocation() != null)
		{
			card.addLine(port.getNavigationLocation().getX() + ", " + port.getNavigationLocation().getY() + " (Floor " + plane + ")");
		}
		return card;
	}
}
