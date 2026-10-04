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
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.ArrayList;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;

/**
 * Builds tooltip hovercards for general points of interest: shops, POI badges,
 * RuneLite plugin markers, ground item spawns, travel nodes, clues, and underground zones.
 */
public class PoiTooltipBuilder
{
	private static final Pattern MOORING_LEVEL_REQUIREMENT = Pattern.compile("(?i)^Requires Level (\\d+) Sailing$");
	private static final Pattern MINING_LEVEL_REQUIREMENT = Pattern.compile("^Requires Level (\\d+) Mining$");

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
			final String title = compact ? resolveCompactPoiName(detail, poi) : cleanPoiName(detail.getTitle());
			final TooltipCard card = new TooltipCard(title, icon);
			card.setPreserveCompactLines(false);
			if (!compact && detail.getCategory() != null && !detail.getCategory().isEmpty())
			{
				card.addLine("[" + detail.getCategory() + "]");
			}
			if (!compact)
			{
				card.addLines(detail.getLines());
			}
			return card;
		}
		else if (poi != null && poi.getName() != null && !poi.getName().isEmpty())
		{
			final String name = compact ? resolveCompactPoiName(null, poi) : cleanPoiName(poi.getName());
			final TooltipCard card = new TooltipCard(name, icon);
			card.setPreserveCompactLines(false);
			if (!compact)
			{
				card.addLine("[" + PoiCategory.of(poi.getKey()).name() + "]");
			}
			return card;
		}
		return null;
	}

	public static String cleanPoiName(String title)
	{
		if (title == null)
		{
			return "";
		}
		if ("Wyrmscraig chest".equalsIgnoreCase(title))
		{
			return "Bank chest";
		}
		// Strip redundant "Wyrmscraig " or "Wyrmscraig - " prefix from generic POIs, keeping unique shops
		if (title.regionMatches(true, 0, "Wyrmscraig", 0, 10)
			&& !title.equalsIgnoreCase("Where Wyrmscraig's Wear Wares Were"))
		{
			String stripped = title.substring(10).trim();
			if (stripped.startsWith("-") || stripped.startsWith("–") || stripped.startsWith("—"))
			{
				stripped = stripped.substring(1).trim();
			}
			if (!stripped.isEmpty())
			{
				return Character.toUpperCase(stripped.charAt(0)) + stripped.substring(1);
			}
		}
		return title;
	}

	public static String resolveCompactPoiName(PoiDetails.Detail detail, PoiIndex.Poi poi)
	{
		final String rawTitle = detail != null ? detail.getTitle() : (poi != null ? poi.getName() : null);
		final String title = cleanPoiName(rawTitle);
		final String poiName = poi != null ? cleanPoiName(poi.getName()) : null;
		final String key = poi != null ? poi.getKey() : "";
		final String cat = detail != null ? detail.getCategory() : "";
		final List<String> lines = detail != null ? detail.getLines() : Collections.emptyList();

		if ("Wyrmscraig chest".equalsIgnoreCase(poiName) || "Wyrmscraig chest".equalsIgnoreCase(title))
		{
			return "Bank chest";
		}

		// 0. Travel networks and port services -> clean basic name
		if ("mooring_point".equals(key) || (title != null && title.toLowerCase().contains("mooring")))
		{
			if (title != null && title.toLowerCase().contains("mooring buoy"))
			{
				return "Mooring buoy";
			}
			if (title != null && title.toLowerCase().contains("boarding plank"))
			{
				return "Ship boarding plank";
			}
			final String mooringName = poiName != null ? poiName : (title != null ? title : "Mooring point");
			for (String line : lines)
			{
				final Matcher level = MOORING_LEVEL_REQUIREMENT.matcher(line.trim());
				if (level.matches())
				{
					return mooringName + " (" + level.group(1) + " Sailing)";
				}
			}
			return mooringName;
		}
		if ("canoe_station".equals(key) || (title != null && title.toLowerCase().contains("canoe"))) return "Canoe station";
		if ("hot_air_balloon".equals(key) || (title != null && title.toLowerCase().contains("balloon"))) return "Hot air balloon";
		if ("magic_mushtree".equals(key) || (title != null && title.toLowerCase().contains("mushtree"))) return "Magic mushtree";
		if ("magic_carpet".equals(key) || (title != null && title.toLowerCase().contains("carpet"))) return "Magic carpet";
		if ("minecart_network".equals(key) || (title != null && title.toLowerCase().contains("minecart"))) return "Minecart network";
		if ("sea_current".equals(key)) return "Sea current";
		if ("shipwright".equals(key)) return "Shipwright";
		if ("cargo_bay".equals(key)) return "Cargo bay";
		if ("lookout_point".equals(key)) return "Lookout point";
		if ("singing_bowl".equals(key)) return "Singing bowl";
		if ("noticeboard".equals(key) || (title != null && title.toLowerCase().contains("noticeboard")))
		{
			if (title != null && title.toLowerCase().contains("port task"))
			{
				return "Port task board";
			}
			return "Noticeboard";
		}
		if ("agility_short-cut".equals(key)) return "Agility shortcut";

		// 1. Fishing spots -> fish
		if ("fishing_spot".equals(key) || (cat != null && cat.contains("Fishing")) || (title != null && title.toLowerCase().contains("fishing")))
		{
			if (title != null)
			{
				final Matcher m = Pattern.compile("Fishing [Ss]pot \\((.+?)\\)").matcher(title);
				if (m.find())
				{
					return mapFishAlias(m.group(1).trim());
				}
			}
			if (lines != null)
			{
				for (String line : lines)
				{
					for (String prefix : new String[]{"Method / Fish: ", "Fish: ", "Catch: "})
					{
						if (line.startsWith(prefix))
						{
							return mapFishAlias(line.substring(prefix.length()).trim());
						}
					}
				}
			}
			if (poiName != null && !poiName.equalsIgnoreCase("Fishing spot") && !poiName.equalsIgnoreCase("fishing_spot"))
			{
				return poiName;
			}
			return "Fishing spot";
		}

		// 2. Mining sites -> minerals
		if ("mining_site".equals(key) || (cat != null && cat.contains("Mining")) || (title != null && title.toLowerCase().contains("mine")))
		{
			if (title != null && lines != null)
			{
				for (String line : lines)
				{
					final Matcher requirement = MINING_LEVEL_REQUIREMENT.matcher(line.trim());
					if (requirement.matches())
					{
						return title + " (" + requirement.group(1) + " Mining)";
					}
				}
			}
			if (lines != null)
			{
				for (String line : lines)
				{
					for (String prefix : new String[]{"Ores available: ", "Ores: "})
					{
						if (line.startsWith(prefix))
						{
							return cleanMineralList(line.substring(prefix.length()).trim());
						}
					}
				}
			}
			if (title != null && title.endsWith(" Mining Site"))
			{
				final String prefix = title.substring(0, title.length() - " Mining Site".length()).trim();
				if (!prefix.isEmpty() && !prefix.equalsIgnoreCase("Mining"))
				{
					return prefix;
				}
			}
			if (title != null)
			{
				String t = title.replaceFirst("\\s*\\((?:Level\\s+)?\\d+\\+?\\)$", "")
					.replaceFirst("\\s+(?:Level\\s+)?\\d+\\+?$", "").trim();
				if (!t.isEmpty() && !t.equalsIgnoreCase("Mining site") && !t.equalsIgnoreCase("mining_site") && !t.equalsIgnoreCase("Mining"))
				{
					return t;
				}
			}
			if (poiName != null && !poiName.equalsIgnoreCase("Mining site") && !poiName.equalsIgnoreCase("mining_site"))
			{
				return poiName;
			}
			return "Mining site";
		}

		// 3. Farming patches -> patch type
		if ("farming_patch".equals(key) || (cat != null && cat.contains("Farming")) || (title != null && (title.toLowerCase().contains("farming") || title.toLowerCase().contains("patch"))))
		{
			String t = title != null ? title : (poiName != null ? poiName : "Farming patch");
			t = t.replaceFirst("\\s*\\((?:Level\\s+)?\\d+\\+?\\)$", "")
				.replaceFirst("\\s+(?:Level\\s+)?\\d+\\+?$", "").trim();
			if (t.contains(" - "))
			{
				t = t.substring(t.lastIndexOf(" - ") + 3).trim();
			}
			t = t.replaceFirst("(?i)\\s+Farming Patch$", " patch").trim();
			t = t.replace("/", ", ");
			return t;
		}

		// 4. Rare trees -> tree type
		if ("rare_trees".equals(key) || (cat != null && cat.contains("Woodcutting")) || (title != null && title.toLowerCase().contains("tree")))
		{
			String t = title != null ? title : (poiName != null ? poiName : "Rare trees");
			t = t.replaceFirst("\\s*\\((?:Level\\s+)?\\d+\\+?\\)$", "")
				.replaceFirst("\\s+(?:Level\\s+)?\\d+\\+?$", "").trim();
			if (t.contains(" - "))
			{
				t = t.substring(t.lastIndexOf(" - ") + 3).trim();
			}
			return t;
		}

		// 5. Hunter training -> target creature / creature name
		if ("hunter_training".equals(key) || (cat != null && cat.contains("Hunter")) || (title != null && title.toLowerCase().contains("hunter")))
		{
			String t = title != null ? title : (poiName != null ? poiName : "Hunter training");
			t = cleanPoiName(t);
			t = t.replaceFirst("\\s*\\((?:Level\\s+)?\\d+\\+?\\)$", "")
				.replaceFirst("\\s+(?:Level\\s+)?\\d+\\+?$", "").trim();
			if (t.contains(" - "))
			{
				t = t.substring(t.lastIndexOf(" - ") + 3).trim();
			}
			if (t.startsWith("-") || t.startsWith("–") || t.startsWith("—"))
			{
				t = t.substring(1).trim();
			}
			if (t.equalsIgnoreCase("Hunter training") || t.equalsIgnoreCase("Hunter"))
			{
				return "Hunter training";
			}
			return t;
		}



		return title != null ? title : (poiName != null ? poiName : "");
	}

	private static String mapFishAlias(String fish)
	{
		if ("Salmon".equalsIgnoreCase(fish)) return "Salmon & Trout";
		if ("Lobster".equalsIgnoreCase(fish)) return "Lobster & Swordfish";
		if ("Shrimp".equalsIgnoreCase(fish)) return "Shrimp & Anchovies";
		return fish;
	}

	private static String cleanMineralList(String ores)
	{
		if (ores == null || ores.isEmpty())
		{
			return "Mining site";
		}
		final String noLevels = ores.replaceAll("\\s*\\(\\d+\\)", "").trim();
		final String[] parts = noLevels.split(",");
		final List<String> list = new ArrayList<>();
		final Set<String> seen = new HashSet<>();
		for (String part : parts)
		{
			final String item = part.trim();
			if (!item.isEmpty() && seen.add(item.toLowerCase()))
			{
				list.add(item);
			}
		}
		if (list.isEmpty())
		{
			return "Mining site";
		}
		if (list.size() == 1)
		{
			return list.get(0);
		}
		if (list.size() == 2)
		{
			return list.get(0) + " & " + list.get(1);
		}
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < list.size(); i++)
		{
			if (i > 0 && i < list.size() - 1)
			{
				sb.append(", ");
			}
			else if (i == list.size() - 1)
			{
				sb.append(" & ");
			}
			sb.append(list.get(i));
		}
		return sb.toString();
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
			title = cleanPoiName(pName);
			if (pTip != null && !pTip.isEmpty() && !pTip.equals(pName))
			{
				card.addLine(pTip);
			}
		}
		else if (pTip != null && !pTip.isEmpty())
		{
			title = cleanPoiName(pTip);
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
