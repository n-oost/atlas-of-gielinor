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

import com.bettermap.data.OverlayCluster;
import com.bettermap.data.TravelData;
import com.bettermap.data.UndergroundZone;
import com.bettermap.map.GroundItemIndex;
import com.bettermap.map.MonsterIconManager;
import com.bettermap.map.PoiCategory;
import com.bettermap.map.PoiDetails;
import com.bettermap.map.PoiIndex;
import com.bettermap.map.ShopIndex;
import java.awt.image.BufferedImage;
import java.util.List;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;

/**
 * Builds tooltip hovercards for general points of interest: shops, POI badges,
 * RuneLite plugin markers, ground item spawns, travel nodes, clues, and underground zones.
 */
public class PoiTooltipBuilder
{
	/**
	 * Builds a tooltip card for a shop and its inventory/neighbouring shops.
	 */
	public TooltipCard buildShopCard(
		ShopIndex.Shop shop,
		List<ShopIndex.Shop> othersNear,
		PoiIndex poiIndex,
		String findQuery)
	{
		if (shop == null)
		{
			return null;
		}

		final PoiDetails.Detail detail = shop.toDetail(othersNear);
		final String title = detail.getTitle();
		final String iconKey = shop.getIcon() != null && !shop.getIcon().isEmpty()
			? shop.getIcon()
			: "general_store";
		final BufferedImage tooltipIcon = poiIndex != null ? poiIndex.icon(iconKey) : null;

		final TooltipCard card = new TooltipCard(title, tooltipIcon);
		card.addLine("[" + detail.getCategory() + "]");

		if (findQuery != null && !findQuery.trim().isEmpty())
		{
			final ShopIndex.StockItem matched = shop.stockItemMatch(findQuery.trim().toLowerCase());
			if (matched != null && matched.getName() != null)
			{
				card.addLine(matched.toDetailLine());
			}
		}

		card.addLines(detail.getLines());
		return card;
	}

	/**
	 * Builds a tooltip card for a POI from details or basic POI index metadata.
	 */
	public TooltipCard buildPoiCard(PoiDetails.Detail detail, PoiIndex.Poi poi, BufferedImage icon)
	{
		return buildPoiCard(detail, poi, icon, false);
	}

	public TooltipCard buildPoiCard(PoiDetails.Detail detail, PoiIndex.Poi poi, BufferedImage icon, boolean compact)
	{
		if (detail != null)
		{
			final TooltipCard card = new TooltipCard(detail.getTitle(), icon);
			card.setPreserveCompactLines(compact);
			if (!compact && detail.getCategory() != null && !detail.getCategory().isEmpty())
			{
				card.addLine("[" + detail.getCategory() + "]");
			}
			if (compact && poi != null && "transportation".equals(poi.getKey()))
			{
				card.addLines(PoiDetails.compactTeleportLines(detail.getTitle()));
			}
			else if (compact && poi != null && PoiCategory.of(poi.getKey()) == PoiCategory.SKILLING)
			{
				card.addLines(PoiDetails.compactSkillingLines(detail.getTitle(), detail.getLines()));
			}
			else if (!compact)
			{
				card.addLines(detail.getLines());
			}
			return card;
		}
		else if (poi != null && poi.getName() != null && !poi.getName().isEmpty())
		{
			final TooltipCard card = new TooltipCard(poi.getName(), icon);
			if (!compact)
			{
				card.addLine("[" + PoiCategory.of(poi.getKey()).name() + "]");
			}
			return card;
		}
		return null;
	}

	/**
	 * Builds a tooltip card for an external RuneLite WorldMapPoint.
	 */
	public TooltipCard buildRuneLitePointCard(WorldMapPoint point)
	{
		if (point == null)
		{
			return null;
		}

		final String pName = point.getName();
		final String pTip = point.getTooltip();
		final BufferedImage tooltipIcon = point.getImage();

		String title = null;
		final TooltipCard card = new TooltipCard();
		card.setIcon(tooltipIcon);

		if (pName != null && !pName.isEmpty())
		{
			title = pName;
			if (pTip != null && !pTip.isEmpty() && !pTip.equals(pName))
			{
				card.addLine(pTip);
			}
		}
		else if (pTip != null && !pTip.isEmpty())
		{
			title = pTip;
		}

		if (title == null)
		{
			return null;
		}

		card.setTitle(title);
		return card;
	}

	/**
	 * Builds a tooltip card for a ground item spawn.
	 */
	public TooltipCard buildGroundItemCard(
		GroundItemIndex.Spawn spawn,
		MonsterIconManager monsterIconManager,
		boolean compact)
	{
		if (spawn == null)
		{
			return null;
		}

		final GroundItemIndex.Item primary = spawn.getPrimary();
		final String title = primary.getQuantity() > 1
			? primary.getName() + " \u00d7" + primary.getQuantity()
			: primary.getName();
		final BufferedImage icon = monsterIconManager != null
			? monsterIconManager.getItemIcon(primary.getId(), 20)
			: null;

		final TooltipCard card = new TooltipCard(title, icon);
		card.addLine("[Ground spawn"
			+ (spawn.getLocation() != null && !spawn.getLocation().isEmpty() ? " \u2022 " + spawn.getLocation() : "")
			+ "]");

		if (!compact)
		{
			for (GroundItemIndex.Item item : spawn.getItems())
			{
				final StringBuilder worth = new StringBuilder();
				if (item.getGePrice() > 0)
				{
					worth.append("GE ").append(item.getGePrice()).append(" gp");
				}
				if (item.getHighAlch() > 0)
				{
					worth.append(worth.length() > 0 ? ", " : "")
						.append("alch ").append(item.getHighAlch()).append(" gp");
				}
				card.addLine(item.getName()
					+ (item.getQuantity() > 1 ? " \u00d7" + item.getQuantity() : "")
					+ (worth.length() > 0 ? " \u2014 " + worth : ""));
			}
			if (spawn.isMembers())
			{
				card.addLine("Members only");
			}
		}
		else if (spawn.getItems().size() > 1)
		{
			card.addLine("+" + (spawn.getItems().size() - 1) + " more on this tile");
		}

		return card;
	}

	/**
	 * Builds a tooltip card for a travel hub node.
	 */
	public TooltipCard buildTravelNodeCard(TravelData.TravelNode travelNode)
	{
		if (travelNode == null)
		{
			return null;
		}

		final String title = travelNode.getName();
		final TooltipCard card = new TooltipCard(title);
		card.addLine("[" + travelNode.getType().getDisplayName() + " \u2022 " + travelNode.getName() + "]");

		final List<TravelData.TravelDestination> dests = travelNode.getDestinations();
		int shown = 0;
		int omitted = 0;
		final int limit = 12;
		for (TravelData.TravelDestination dest : dests)
		{
			if (dest.getName() != null && dest.getName().equalsIgnoreCase(travelNode.getName()))
			{
				continue; // skip self-destination
			}
			if (shown >= limit)
			{
				omitted++;
				continue;
			}
			final String req = dest.getRequirement();
			final boolean hasReq = req != null && !req.trim().isEmpty();
			final String cost = dest.getCost() != null ? dest.getCost() : "";
			card.addLine("\u2192 " + dest.getName() + ": " + cost + (hasReq ? " (Req: " + req.trim() + ")" : ""));
			shown++;
		}
		if (omitted > 0)
		{
			card.addLine("... +" + omitted + " more");
		}

		return card;
	}

	/**
	 * Builds a tooltip card for a clue scroll target.
	 */
	public TooltipCard buildClueCard(WorldPoint clueLoc, String label, PanelComponent cluePanel)
	{
		if (clueLoc == null)
		{
			return null;
		}

		final String cardTitle = label != null ? label : "Clue Scroll";
		final TooltipCard card = new TooltipCard(cardTitle);
		card.addLine("[Clue Scroll Target]");
		card.addLine(clueLoc.getX() + ", " + clueLoc.getY() + " (Floor " + clueLoc.getPlane() + ")");

		if (cluePanel != null && !cluePanel.getChildren().isEmpty())
		{
			card.setTrailingPanel(cluePanel);
		}

		return card;
	}

	/**
	 * Builds a tooltip card for an underground layer symbol.
	 */
	public TooltipCard buildUndergroundZoneCard(UndergroundZone hoveredZone, boolean surfaceToUnderground)
	{
		if (hoveredZone == null)
		{
			return null;
		}

		final TooltipCard card = new TooltipCard();
		if (surfaceToUnderground)
		{
			card.setTitle(hoveredZone.getName() + " (Underground)");
			card.addLine("[Underground Layer \u2022 Lower Plane]");
			card.addLine(hoveredZone.getDescription());
			card.addLine("Hovering: Lower level revealed (surface transparent)");
			card.addLine("Click to open full underground map");
		}
		else
		{
			card.setTitle(hoveredZone.getName() + " (Surface Overworld)");
			card.addLine("[Surface Layer \u2022 Upper Plane]");
			final StringBuilder entrances = new StringBuilder("Surface entrances / exits: ");
			for (int i = 0; i < hoveredZone.getSurfacePoints().size(); i++)
			{
				if (i > 0)
				{
					entrances.append(", ");
				}
				final WorldPoint point = hoveredZone.getSurfacePoints().get(i);
				entrances.append('(').append(point.getX()).append(", ").append(point.getY()).append(')');
			}
			card.addLine(entrances.toString());
			card.addLine("Hovering: Surface overworld revealed");
			card.addLine("Click to open full surface map");
		}
		return card;
	}

	/**
	 * Builds a tooltip card for a connected dungeon cluster overlay.
	 */
	public TooltipCard buildOverlayClusterCard(OverlayCluster cluster)
	{
		if (cluster == null)
		{
			return null;
		}

		final TooltipCard card = new TooltipCard(cluster.name);
		card.addLine("[Connected overlay \u2022 member dungeons]");
		card.addLine("Green dungeon icon peeks every connected dungeon in this area.");
		card.addLine("Click to keep the overlay open. Click a dungeon icon to open that dungeon.");
		for (UndergroundZone member : cluster.members)
		{
			card.addLine("\u2022 " + member.getName());
		}
		return card;
	}
}
