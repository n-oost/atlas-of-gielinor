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
package com.bettermap.ui;

import static com.bettermap.ui.MapStyle.BOSSES;
import static com.bettermap.ui.MapStyle.CARD_BG;
import static com.bettermap.ui.MapStyle.CARD_EDGE;
import static com.bettermap.ui.MapStyle.CARD_PALETTE;
import static com.bettermap.ui.MapStyle.CARD_TITLE;
import static com.bettermap.ui.MapStyle.SMALL;

import com.bettermap.BetterMapConfig;
import com.bettermap.data.MonsterLocationData;
import com.bettermap.data.OverlayCluster;
import com.bettermap.data.TravelData;
import com.bettermap.data.UndergroundZone;
import com.bettermap.data.sailing.BoatTracker;
import com.bettermap.data.sailing.PlayerBoat;
import com.bettermap.data.sailing.PortNoticeBoard;
import com.bettermap.data.sailing.SailingPort;
import com.bettermap.map.ClueScrollTracker;
import com.bettermap.map.GroundItemIndex;
import com.bettermap.map.InstanceMaps;
import com.bettermap.map.MapCamera;
import com.bettermap.map.MapFinder;
import com.bettermap.map.MonsterIconManager;
import com.bettermap.map.MonsterIndex;
import com.bettermap.map.PoiCategory;
import com.bettermap.map.PoiDetails;
import com.bettermap.map.PoiIndex;
import com.bettermap.map.PrifddinasShift;
import com.bettermap.map.ShopIndex;
import com.bettermap.map.SlayerTaskTracker;
import com.bettermap.map.WorldMapInput;
import com.bettermap.map.WorldMapPointReader;
import java.awt.Dimension;
import java.awt.Font;
import net.runelite.client.ui.FontManager;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;

/**
 * The hover card: what is under the cursor, and the card that describes it.
 *
 * <p>Split out of {@link BetterWorldMapOverlay}. The {@code *Near} hit-tests decide which of
 * the map's layers answers for a given tile, in the same order the card used to check them.
 */
@Slf4j
class MapTooltipRenderer
{
	private final Client client;
	private final BetterMapConfig config;
	private final MapCamera camera;
	private final WorldMapInput input;
	private final PoiIndex poiIndex;
	private final MonsterIndex monsterIndex;
	private final MonsterIconManager monsterIconManager;
	private final SlayerTaskTracker slayerTaskTracker;
	private final ClueScrollTracker clueScrollTracker;
	private final ShopIndex shopIndex;
	private final MapFinder mapFinder;
	private final GroundItemIndex groundItemIndex;
	private final BoatTracker boatTracker;
	private final WorldMapPointManager worldMapPointManager;
	private final WorldMapPointReader worldMapPointReader;

	MapTooltipRenderer(
		Client client,
		BetterMapConfig config,
		MapCamera camera,
		WorldMapInput input,
		PoiIndex poiIndex,
		MonsterIndex monsterIndex,
		MonsterIconManager monsterIconManager,
		SlayerTaskTracker slayerTaskTracker,
		ClueScrollTracker clueScrollTracker,
		ShopIndex shopIndex,
		MapFinder mapFinder,
		GroundItemIndex groundItemIndex,
		BoatTracker boatTracker,
		WorldMapPointManager worldMapPointManager,
		WorldMapPointReader worldMapPointReader)
	{
		this.client = client;
		this.config = config;
		this.camera = camera;
		this.input = input;
		this.poiIndex = poiIndex;
		this.monsterIndex = monsterIndex;
		this.monsterIconManager = monsterIconManager;
		this.slayerTaskTracker = slayerTaskTracker;
		this.clueScrollTracker = clueScrollTracker;
		this.shopIndex = shopIndex;
		this.mapFinder = mapFinder;
		this.groundItemIndex = groundItemIndex;
		this.boatTracker = boatTracker;
		this.worldMapPointManager = worldMapPointManager;
		this.worldMapPointReader = worldMapPointReader;
	}

	private <T> T hit(int worldX, int worldY, BiFunction<Integer, Integer, T> lookup)
	{
		return InstanceMaps.firstHit(worldX, worldY,
			camera.getFocusedUndergroundZone(), camera.isDungeonContentsFocused(), lookup);
	}

	private boolean layerAllows(int worldX, int worldY)
	{
		return InstanceMaps.inFocusedLayer(worldX, worldY,
			camera.getFocusedUndergroundZone(), camera.isDungeonContentsFocused());
	}

	/**
	 * The card that follows the cursor. Bosses win over map icons, and failing both it still
	 * reports the coordinate under the cursor, so the map always answers "what is this".
	 */
	void drawTooltip(Graphics2D graphics, Rectangle bounds)
	{
		if (!config.showTooltips())
		{
			return;
		}

		final java.awt.Point cursor = input.getCursor();
		if (cursor == null || !bounds.contains(cursor))
		{
			return;
		}

		// Don't float a hovercard over the open Layers panel or Finder panel.
		final Rectangle layersPanel = camera.getLayersPanelBounds();
		if (layersPanel != null && layersPanel.contains(cursor))
		{
			return;
		}
		final Rectangle finderPanel = camera.getFinderPanelBounds();
		if (finderPanel != null && finderPanel.contains(cursor))
		{
			return;
		}
		final Rectangle cluePanel = camera.getCluePanelBounds();
		if (cluePanel != null && cluePanel.contains(cursor))
		{
			return;
		}

		if (drawUndergroundZoneCard(graphics, bounds, cursor))
		{
			return;
		}

		final int worldX = (int) Math.floor(camera.worldX(cursor.x, bounds));
		final int worldY = (int) Math.floor(camera.worldY(cursor.y, bounds));
		final int plane = camera.getPlane();
		final int radius = hitRadius();
		final int pointRadius = Math.min(radius, 6);
		final HoverProbe probe = new HoverProbe(cursor, worldX, worldY, plane, pointRadius);

		if (drawBoatCard(graphics, bounds, probe))
		{
			return;
		}

		if (drawNoticeBoardCard(graphics, bounds, probe))
		{
			return;
		}

		if (drawPortCard(graphics, bounds, probe))
		{
			return;
		}

		if (drawClueCard(graphics, bounds, probe))
		{
			return;
		}

		if (drawTravelNodeCard(graphics, bounds, probe))
		{
			return;
		}

		final List<String> lines = new ArrayList<>();
		String title = null;
		BufferedImage tooltipIcon = null;

		// Point markers before area centroids: bosses → shops → POIs → RuneLite → monster zones.

		final MonsterLocationData boss = monsterNear(worldX, worldY);
		if (boss != null)
		{
			title = boss.getName() + " (Lvl " + boss.getCombatLevel() + ")";
			tooltipIcon = monsterIconManager.getBossIcon(boss, 20);
			if (config.highlightSlayerTask() && slayerTaskTracker.isTaskBoss(boss))
			{
				final int remaining = slayerTaskTracker.getRemainingTaskAmount();
				lines.add(remaining > 0 ? "[Current Slayer Task • " + remaining + " remaining]" : "[Current Slayer Task]");
			}
			lines.add("[Boss • " + boss.getLocationName() + "]");
			if (!config.compactTooltips())
			{
				lines.add("Strategy: " + boss.getWeaknessStrategy());
				lines.add("Key drops: " + boss.getKeyDrops());
			}
		}

		if (title == null)
		{
			final ShopIndex.Shop shop = hit(worldX, worldY,
				(x, y) -> shopIndex.nearest(x, y, plane, Math.min(radius, 10)));
			if (shop != null)
			{
				final PoiDetails.Detail detail = shop.toDetail(shopIndex.othersNear(shop, 6, 2));
				title = detail.getTitle();
				final String iconKey = shop.getIcon() != null && !shop.getIcon().isEmpty()
					? shop.getIcon()
					: "general_store";
				tooltipIcon = poiIndex.icon(iconKey);
				lines.add("[" + detail.getCategory() + "]");
				final String findQuery = mapFinder != null ? mapFinder.getQuery() : null;
				if (findQuery != null && !findQuery.trim().isEmpty())
				{
					final ShopIndex.StockItem matched = shop.stockItemMatch(findQuery.trim().toLowerCase());
					if (matched != null && matched.getName() != null)
					{
						lines.add(matched.toDetailLine());
					}
				}
				lines.addAll(detail.getLines());
			}
		}

		if (title == null)
		{
			PoiDetails.Detail detail = null;
			final PoiIndex.Poi poi = poiIconAt(cursor, bounds, plane);
			if (poi != null)
			{
				detail = PoiDetails.getDetail(poi, poi.getX(), poi.getY(), plane);
				tooltipIcon = poiIndex.icon(poi.getKey());
			}
			if (detail == null)
			{
				detail = hit(worldX, worldY,
					(x, y) -> PoiDetails.getDetailByPosition(x, y, plane, pointRadius));
			}

			if (detail != null)
			{
				title = detail.getTitle();
				if (detail.getCategory() != null && !detail.getCategory().isEmpty())
				{
					lines.add("[" + detail.getCategory() + "]");
				}
				lines.addAll(detail.getLines());
			}
			else if (poi != null && poi.getName() != null && !poi.getName().isEmpty())
			{
				title = poi.getName();
				lines.add("[" + PoiCategory.of(poi.getKey()).name() + "]");
			}
		}

		if (title == null)
		{
			final WorldMapPoint rPoint = findRuneLitePointNear(worldX, worldY, plane, pointRadius);
			if (rPoint != null)
			{
				final String pName = rPoint.getName();
				final String pTip = rPoint.getTooltip();
				tooltipIcon = rPoint.getImage();
				if (pName != null && !pName.isEmpty())
				{
					title = pName;
					if (pTip != null && !pTip.isEmpty() && !pTip.equals(pName))
					{
						lines.add(pTip);
					}
				}
				else if (pTip != null && !pTip.isEmpty())
				{
					title = pTip;
				}
			}
		}

		// Ground items are exact tiles, so the hover radius stays tight: a wide one would steal
		// hovers from the POI under it. Gated on the same zoom as the draw, so a marker that is
		// not on screen is never hoverable.
		if (title == null && config.showGroundItems() && camera.getZoom() >= config.groundItemMinZoom())
		{
			final GroundItemIndex.Spawn spawn = hit(worldX, worldY,
				(x, y) -> groundItemIndex.nearest(x, y, plane, Math.min(radius, 4), config.groundItemMinValue()));
			if (spawn != null)
			{
				final GroundItemIndex.Item primary = spawn.getPrimary();
				title = primary.getQuantity() > 1
					? primary.getName() + " ×" + primary.getQuantity()
					: primary.getName();
				tooltipIcon = monsterIconManager.getItemIcon(primary.getId(), 20);
				lines.add("[Ground spawn"
					+ (spawn.getLocation() != null && !spawn.getLocation().isEmpty()
						? " • " + spawn.getLocation() : "")
					+ "]");

				if (!config.compactTooltips())
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
						lines.add(item.getName()
							+ (item.getQuantity() > 1 ? " ×" + item.getQuantity() : "")
							+ (worth.length() > 0 ? " — " + worth : ""));
					}
					if (spawn.isMembers())
					{
						lines.add("Members only");
					}
				}
				else if (spawn.getItems().size() > 1)
				{
					lines.add("+" + (spawn.getItems().size() - 1) + " more on this tile");
				}
			}
		}

		// Monster zones last — wide centroids must not steal shortcut / shop / quest hovers.
		if (title == null && config.showMonsterZones())
		{
			final MonsterIndex.Zone monster = PrifddinasShift.firstHit(worldX, worldY,
				(x, y) -> layerAllows(x, y) ? monsterIndex.nearest(plane, x, y, pointRadius) : null);
			if (monster != null)
			{
				final boolean isTask = config.highlightSlayerTask() && slayerTaskTracker.isTaskMonster(monster);
				title = monster.getMonster() + (monster.getCombatLevel() > 0 ? " (Lvl " + monster.getCombatLevel() + ")" : "");
				tooltipIcon = monsterIconManager.getZoneIcon(monster, 20);
				if (isTask)
				{
					final int remaining = slayerTaskTracker.getRemainingTaskAmount();
					lines.add(remaining > 0 ? "[Current Slayer Task • " + remaining + " remaining]" : "[Current Slayer Task]");
				}
				if (monster.getLocationName() != null && !monster.getLocationName().isEmpty())
				{
					lines.add("[Monster • " + monster.getLocationName() + "]");
				}
				if (monster.getSlayerLevel() > 1)
				{
					lines.add("Slayer required: Level " + monster.getSlayerLevel() + " Slayer");
				}
				lines.addAll(monster.combatSummaryLines());
				if (!config.compactTooltips())
				{
					if (monster.getSlayerMasters() != null)
					{
						lines.add("Assigned by: " + monster.getSlayerMasters());
					}
					if (monster.getExamine() != null && !monster.getExamine().isEmpty())
					{
						lines.add("\"" + monster.getExamine() + "\"");
					}
				}
			}
		}

		if (title == null || title.trim().isEmpty())
		{
			return;
		}

		lines.add(worldX + ", " + worldY + " (Floor " + plane + ")");
		drawCard(graphics, bounds, cursor, title, tooltipIcon, lines);
	}

	/**
	 * Where the cursor is, in screen and world terms, plus the hit radius for this zoom.
	 * Built once and handed to each card probe below, which are tried in priority order.
	 */
	private static final class HoverProbe
	{
		private final java.awt.Point cursor;
		private final int worldX;
		private final int worldY;
		private final int plane;
		private final int pointRadius;

		private HoverProbe(java.awt.Point cursor, int worldX, int worldY, int plane, int pointRadius)
		{
			this.cursor = cursor;
			this.worldX = worldX;
			this.worldY = worldY;
			this.plane = plane;
			this.pointRadius = pointRadius;
		}
	}

	/** Travel hub directory. Only when no tighter point marker is under the cursor - the travel hover radius is wide. */
	private boolean drawTravelNodeCard(Graphics2D graphics, Rectangle bounds, HoverProbe probe)
	{
		final TravelData.TravelNode travelNode = camera.getHoveredTravelNode();
		if (travelNode != null && !hasTighterPointMarker(probe.worldX, probe.worldY, probe.plane, probe.pointRadius))
		{
			final String title = travelNode.getName();
			final List<String> lines = new ArrayList<>();
			lines.add("[" + travelNode.getType().getDisplayName() + " • " + travelNode.getName() + "]");

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
				lines.add("→ " + dest.getName() + ": " + cost + (hasReq ? " (Req: " + req.trim() + ")" : ""));
				shown++;
			}
			if (omitted > 0)
			{
				lines.add("... +" + omitted + " more");
			}

			drawCard(graphics, bounds, probe.cursor, title, lines);
			return true;
		}
		return false;
	}

	/** The active clue-scroll target, when one is marked on the map. */
	private boolean drawClueCard(Graphics2D graphics, Rectangle bounds, HoverProbe probe)
	{
		final WorldPoint clueLoc = clueNear(probe.worldX, probe.worldY, probe.plane, probe.pointRadius);
		if (clueLoc != null)
		{
			final String label = clueScrollTracker.label();
			final String cardTitle = label != null ? label : "Clue Scroll";
			final List<String> clueLines = new ArrayList<>();
			clueLines.add("[Clue Scroll Target]");
			clueLines.add(clueLoc.getX() + ", " + clueLoc.getY() + " (Floor " + clueLoc.getPlane() + ")");

			// The clue plugin's own hint panel carries the step detail (emotes, STASH lines,
			// required items) plus the enemy / spade / light requirement lines that
			// ClueScrollTracker appends. RuneLite's PanelComponent renderer draws it, attached
			// under the hovercard text.
			final PanelComponent cluePanelHint = clueScrollTracker.cluePanel();
			drawCard(graphics, bounds, probe.cursor, cardTitle, clueLines,
				cluePanelHint != null && !cluePanelHint.getChildren().isEmpty() ? cluePanelHint : null);
			return true;
		}
		return false;
	}

	/** A sailing port under the cursor. */
	private boolean drawPortCard(Graphics2D graphics, Rectangle bounds, HoverProbe probe)
	{
		final SailingPort port = portNear(probe.worldX, probe.worldY, probe.plane);
		if (port != null)
		{
			final String portTitle = port.getName();
			final List<String> portLines = new ArrayList<>();
			final String sailingHeader = port.getSailingLevelRequired() > 1
				? "Level " + port.getSailingLevelRequired() + " Sailing"
				: "Open Dock";
			portLines.add("[Sailing Dock • " + sailingHeader + "]");

			final List<PlayerBoat> dockedBoats = boatTracker != null ? boatTracker.getBoatsAt(port) : Collections.emptyList();
			if (!dockedBoats.isEmpty())
			{
				if (dockedBoats.size() == 1)
				{
					portLines.add("Docked boat: ⛵ " + dockedBoats.get(0).getBoatName());
				}
				else
				{
					final StringBuilder sb = new StringBuilder("Docked boats (" + dockedBoats.size() + "): ");
					for (int i = 0; i < dockedBoats.size(); i++)
					{
						if (i > 0)
						{
							sb.append(", ");
						}
						sb.append("⛵ ").append(dockedBoats.get(i).getBoatName());
					}
					portLines.add(sb.toString());
				}
			}

			if (port.getNavigationLocation() != null)
			{
				portLines.add(port.getNavigationLocation().getX() + ", " + port.getNavigationLocation().getY() + " (Floor " + probe.plane + ")");
			}

			drawCard(graphics, bounds, probe.cursor, portTitle, portLines);
			return true;
		}
		return false;
	}

	/** A port notice board under the cursor. */
	private boolean drawNoticeBoardCard(Graphics2D graphics, Rectangle bounds, HoverProbe probe)
	{
		final PortNoticeBoard noticeBoard = noticeBoardNear(probe.worldX, probe.worldY, probe.plane);
		if (noticeBoard != null)
		{
			final String boardTitle = noticeBoard.getName();
			final List<String> boardLines = new ArrayList<>();
			boardLines.add("[Port tasks • sample]");

			final List<String> couriers = noticeBoard.getCourierTasks();
			if (couriers != null && !couriers.isEmpty())
			{
				boardLines.add("Courier tasks (sample):");
				for (String courier : couriers)
				{
					boardLines.add("  • " + courier);
				}
			}

			final List<String> bounties = noticeBoard.getBountyTasks();
			if (bounties != null && !bounties.isEmpty())
			{
				boardLines.add("Bounty tasks (sample):");
				for (String bounty : bounties)
				{
					boardLines.add("  • " + bounty);
				}
			}
			else
			{
				boardLines.add("Bounty unlocks at Sailing 30");
			}

			boardLines.add("Boards rotate after 8 tasks or daily reset");
			boardLines.add(noticeBoard.getLocation().getX() + ", " + noticeBoard.getLocation().getY() + " (Floor " + probe.plane + ")");

			drawCard(graphics, bounds, probe.cursor, boardTitle, boardLines);
			return true;
		}
		return false;
	}

	/** The player's own boat. Point markers beat travel hubs, which hover wide. */
	private boolean drawBoatCard(Graphics2D graphics, Rectangle bounds, HoverProbe probe)
	{
		final PlayerBoat boat = boatNear(probe.worldX, probe.worldY, probe.plane);
		if (boat != null && boat.isOwned() && boat.getPort() != null)
		{
			final SailingPort port = boat.getPort();
			final String portName = port != null ? port.getName() : SailingPort.resolvePortName(client, boat.getPortId());
			final String typeStr = boat.getBoatType() != null ? boat.getBoatType().getName() : "Sailing Ship";
			final String boatTitle = boat.getBoatName() + " (Boat #" + boat.getBoatId() + ")";

			final List<String> boatLines = new ArrayList<>();
			boatLines.add("[" + typeStr + " • Docked at " + portName + "]");
			if (port != null && port.getSailingLevelRequired() > 1)
			{
				boatLines.add("Required: Level " + port.getSailingLevelRequired() + " Sailing");
			}
			if (boat.getHealth() >= 0)
			{
				final int pct = Math.round(boat.getHealth() * 100f);
				final String cond = pct >= 80 ? "Good" : (pct >= 40 ? "Damaged" : "Critical");
				boatLines.add("Hull Condition: " + pct + "% (" + cond + ")");
			}
			if (port != null && port.getNavigationLocation() != null)
			{
				boatLines.add(port.getNavigationLocation().getX() + ", " + port.getNavigationLocation().getY() + " (Floor " + probe.plane + ")");
			}
			drawCard(graphics, bounds, probe.cursor, boatTitle, boatLines);
			return true;
		}
		return false;
	}

	/** A hovered large layer symbol, surface or underground. Runs before the world coordinates are resolved, so it takes the cursor directly. */
	private boolean drawUndergroundZoneCard(Graphics2D graphics, Rectangle bounds, java.awt.Point cursor)
	{
		final UndergroundZone hoveredZone = camera.getHoveredUndergroundZone();
		if (hoveredZone != null)
		{
			final List<String> lines = new ArrayList<>();
			String title;
			if (camera.isHoveredSurfaceToUnderground())
			{
				title = hoveredZone.getName() + " (Underground)";
				lines.add("[Underground Layer • Lower Plane]");
				lines.add(hoveredZone.getDescription());
				lines.add("Hovering: Lower level revealed (surface transparent)");
				lines.add("Click to open full underground map");
			}
			else
			{
				title = hoveredZone.getName() + " (Surface Overworld)";
				lines.add("[Surface Layer • Upper Plane]");
				lines.add("Surface location: (" + hoveredZone.getSurfacePoint().getX() + ", " + hoveredZone.getSurfacePoint().getY() + ")");
				lines.add("Hovering: Surface overworld revealed");
				lines.add("Click to open full surface map");
			}
			drawCard(graphics, bounds, cursor, title, lines);
			return true;
		}
		final OverlayCluster cluster = camera.getHoveredOverlayCluster();
		if (cluster != null)
		{
			final List<String> lines = new ArrayList<>();
			lines.add("[Connected overlay • member dungeons]");
			lines.add("Green dungeon icon peeks every connected dungeon in this area.");
			lines.add("Click to keep the overlay open. Click a dungeon icon to open that dungeon.");
			for (UndergroundZone member : cluster.members)
			{
				lines.add("• " + member.getName());
			}
			drawCard(graphics, bounds, cursor, cluster.name, lines);
			return true;
		}
		return false;
	}

	/** True when a shop, boss, POI, or RuneLite pin is under the cursor (beats travel hubs). */
	private boolean hasTighterPointMarker(int worldX, int worldY, int plane, int pointRadius)
	{
		if (clueNear(worldX, worldY, plane, pointRadius) != null)
		{
			return true;
		}
		if (monsterNear(worldX, worldY) != null)
		{
			return true;
		}
		if (hit(worldX, worldY,
			(x, y) -> shopIndex.nearest(x, y, plane, Math.min(pointRadius, 10))) != null)
		{
			return true;
		}
		if (hit(worldX, worldY,
			(x, y) -> poiIndex.nearest(x, y, plane, pointRadius)) != null)
		{
			return true;
		}
		if (hit(worldX, worldY,
			(x, y) -> PoiDetails.getDetailByPosition(x, y, plane, pointRadius)) != null)
		{
			return true;
		}
		return findRuneLitePointNear(worldX, worldY, plane, pointRadius) != null;
	}

	private WorldMapPoint findRuneLitePointNear(int worldX, int worldY, int plane, int radius)
	{
		for (WorldMapPoint point : worldMapPointReader.points(worldMapPointManager))
		{
			final WorldPoint wp = point.getWorldPoint();
			if (wp == null || wp.getPlane() != plane || !layerAllows(wp.getX(), wp.getY()))
			{
				continue;
			}
			if (Math.abs(InstanceMaps.toDisplayX(wp.getX(), wp.getY(), camera.getCenterX(), camera.getCenterY()) - worldX) <= radius
				&& Math.abs(InstanceMaps.toDisplayY(wp.getX(), wp.getY(), camera.getCenterX(), camera.getCenterY()) - worldY) <= radius)
			{
				return point;
			}
		}
		return null;
	}

	private WorldPoint clueNear(int worldX, int worldY, int plane, int radius)
	{
		if (clueScrollTracker == null || !config.showClueScroll())
		{
			return null;
		}

		final List<WorldPoint> targets = clueScrollTracker.locations();
		if (targets.isEmpty())
		{
			return null;
		}

		WorldPoint best = null;
		int bestDist = radius + 1;
		for (WorldPoint target : targets)
		{
			if (target == null)
			{
				continue;
			}
			if (target.getPlane() == plane)
			{
				final int dx = Math.abs(target.getX() - worldX);
				final int dy = Math.abs(target.getY() - worldY);
				final int dist = Math.max(dx, dy);
				if (dist <= radius && dist < bestDist)
				{
					best = target;
					bestDist = dist;
				}
			}
		}

		if (best == null)
		{
			for (WorldPoint target : targets)
			{
				if (target == null)
				{
					continue;
				}
				final int dx = Math.abs(target.getX() - worldX);
				final int dy = Math.abs(target.getY() - worldY);
				final int dist = Math.max(dx, dy);
				if (dist <= Math.min(radius, 3) && dist < bestDist)
				{
					best = target;
					bestDist = dist;
				}
			}
		}

		return best;
	}

	/** Keeps the hit area a roughly constant size on screen as the zoom changes. */
	private int hitRadius()
	{
		return (int) Math.max(3, Math.ceil(12 / Math.max(0.4, camera.getFrameZoom())));
	}

	/** Hit-test POI icons using the same fixed screen bounds used to draw them. */
	private PoiIndex.Poi poiIconAt(java.awt.Point cursor, Rectangle bounds, int plane)
	{
		if (camera.getFrameZoom() < 0.45)
		{
			return null;
		}
		final int halfExtent = 24;
		final int minX = (int) Math.floor(camera.worldX(cursor.x - halfExtent, bounds));
		final int maxX = (int) Math.ceil(camera.worldX(cursor.x + halfExtent, bounds));
		final int minY = (int) Math.floor(camera.worldY(cursor.y + halfExtent, bounds));
		final int maxY = (int) Math.ceil(camera.worldY(cursor.y - halfExtent, bounds));
		final PoiIndex.Poi[] best = {null};
		final double[] bestDistance = {Double.POSITIVE_INFINITY};
		final boolean surfaceOnly = camera.getFocusedUndergroundZone() == null
			&& !camera.isDungeonContentsFocused() && InstanceMaps.cameraOnOverworld(camera.getCenterY());

		InstanceMaps.forEachQueryArea(minX, maxX, minY, maxY,
			camera.getCenterX(), camera.getCenterY(), (qMinX, qMaxX, qMinY, qMaxY) ->
				poiIndex.forEachInArea(plane, qMinX, qMaxX, qMinY, qMaxY, poi ->
				{
					if (!layerAllows(poi.getX(), poi.getY()) || (surfaceOnly && poi.getY() > InstanceMaps.GAP_MIN_Y))
					{
						return;
					}
					final BufferedImage icon = poiIndex.icon(poi.getKey());
					if (icon == null)
					{
						return;
					}
					final int iconX = (int) Math.round(camera.screenX(poi.getX() + 0.5, poi.getY() + 0.5, bounds));
					final int iconY = (int) Math.round(camera.screenY(poi.getX() + 0.5, poi.getY() + 0.5, bounds));
					final Rectangle iconBounds = new Rectangle(iconX - icon.getWidth() / 2,
						iconY - icon.getHeight() / 2, icon.getWidth(), icon.getHeight());
					if (!iconBounds.contains(cursor))
					{
						return;
					}
					final double dx = iconX - cursor.x;
					final double dy = iconY - cursor.y;
					final double distance = dx * dx + dy * dy;
					if (distance < bestDistance[0])
					{
						best[0] = poi;
						bestDistance[0] = distance;
					}
				}));
		return best[0];
	}

	private PlayerBoat boatNear(int worldX, int worldY, int plane)
	{
		if (!config.showBoatLocations() || boatTracker == null)
		{
			return null;
		}

		return hit(worldX, worldY,
			(x, y) -> boatTracker.getBoatNear(x, y, plane, hitRadius()));
	}

	private PortNoticeBoard noticeBoardNear(int worldX, int worldY, int plane)
	{
		if (!config.showPortNoticeBoards())
		{
			return null;
		}

		return hit(worldX, worldY,
			(x, y) -> PortNoticeBoard.findBoardNear(x, y, plane, hitRadius()));
	}

	private SailingPort portNear(int worldX, int worldY, int plane)
	{
		if (!config.showSailingPorts())
		{
			return null;
		}

		return hit(worldX, worldY,
			(x, y) -> SailingPort.findPortNear(x, y, plane, hitRadius()));
	}

	private MonsterLocationData monsterNear(int worldX, int worldY)
	{
		if (!config.showBossLocations())
		{
			return null;
		}

		final int radius = hitRadius();

		for (MonsterLocationData monster : BOSSES)
		{
			final WorldPoint point = monster.getWorldPoint();
			if (point.getPlane() != camera.getPlane() || !layerAllows(point.getX(), point.getY()))
			{
				continue;
			}

			if (Math.abs(InstanceMaps.toDisplayX(point.getX(), point.getY(), camera.getCenterX(), camera.getCenterY()) - worldX) <= radius
				&& Math.abs(InstanceMaps.toDisplayY(point.getX(), point.getY(), camera.getCenterX(), camera.getCenterY()) - worldY) <= radius)
			{
				return monster;
			}
		}

		return null;
	}

	private void drawCard(Graphics2D graphics, Rectangle bounds, java.awt.Point cursor, String title, List<String> lines)
	{
		drawCard(graphics, bounds, cursor, title, null, lines, null);
	}

	private void drawCard(Graphics2D graphics, Rectangle bounds, java.awt.Point cursor, String title, BufferedImage icon, List<String> lines)
	{
		drawCard(graphics, bounds, cursor, title, icon, lines, null);
	}

	private void drawCard(Graphics2D graphics, Rectangle bounds, java.awt.Point cursor, String title, List<String> lines, PanelComponent trailingPanel)
	{
		drawCard(graphics, bounds, cursor, title, null, lines, trailingPanel);
	}

	private void drawCard(Graphics2D graphics, Rectangle bounds, java.awt.Point cursor, String title, BufferedImage icon, List<String> lines, PanelComponent trailingPanel)
	{
		final Font titleFont = FontManager.getRunescapeBoldFont();
		final Font bodyFont = SMALL;

		final FontMetrics titleMetrics = graphics.getFontMetrics(titleFont);
		final FontMetrics bodyMetrics = graphics.getFontMetrics(bodyFont);
		final int titleLineHeight = titleMetrics.getHeight();
		final int bodyLineHeight = bodyMetrics.getHeight();

		final int iconSize = icon != null ? 20 : 0;
		final int iconPad = icon != null ? 6 : 0;
		final int availableTitleWidth = CardText.MAX_WIDTH_PX - (iconSize + iconPad);

		final List<CardText.Row> rows = CardText.layout(bodyMetrics, lines, CARD_PALETTE);
		final List<String> titleRows = CardText.wrap(titleMetrics, title, Math.max(80, availableTitleWidth), 2);

		final int padding = 7;

		// The clue hint panel, when supplied, is drawn under the text rows by RuneLite's own
		// PanelComponent renderer. Its width is an input to that renderer, not an output, so we
		// fix the content width and measure only the height with a throwaway two-pass render (a
		// PanelComponent returns a stale size on its first render).
		final int panelContentW = CardText.MAX_WIDTH_PX - padding * 2;
		Dimension panelSize = null;
		if (trailingPanel != null && !trailingPanel.getChildren().isEmpty())
		{
			trailingPanel.setBackgroundColor(null);
			trailingPanel.setPreferredSize(new Dimension(panelContentW, 0));
			trailingPanel.setPreferredLocation(new java.awt.Point(0, 0));
			panelSize = measurePanel(trailingPanel, graphics);
		}

		int width = 0;
		for (String titleRow : titleRows)
		{
			width = Math.max(width, titleMetrics.stringWidth(titleRow) + (iconSize + iconPad));
		}
		for (CardText.Row row : rows)
		{
			width = Math.max(width, bodyMetrics.stringWidth(row.getText()));
		}
		if (panelSize != null)
		{
			width = Math.max(width, panelSize.width);
		}
		width = Math.min(width, CardText.MAX_WIDTH_PX);

		final int panelGap = panelSize != null ? 4 : 0;
		final int panelH = panelSize != null ? panelSize.height : 0;
		final int boxWidth = width + padding * 2;
		final int textBlockHeight = titleRows.size() * titleLineHeight + rows.size() * bodyLineHeight;
		final int minContentHeight = icon != null ? iconSize : 0;
		final int boxHeight = Math.max(textBlockHeight, minContentHeight) + padding * 2 + panelGap + panelH;

		int x = cursor.x + 14;
		int y = cursor.y + 14;
		if (x + boxWidth > bounds.getMaxX())
		{
			x = cursor.x - boxWidth - 10;
		}
		if (y + boxHeight > bounds.getMaxY())
		{
			y = cursor.y - boxHeight - 10;
		}

		x = Math.max((int) bounds.getMinX() + 4, Math.min(x, (int) bounds.getMaxX() - boxWidth - 4));
		y = Math.max((int) bounds.getMinY() + 4, Math.min(y, (int) bounds.getMaxY() - boxHeight - 4));

		// Keep the opaque card off the cursor so hover does not flicker at corners.
		if (cursor.x >= x && cursor.x < x + boxWidth && cursor.y >= y && cursor.y < y + boxHeight)
		{
			final int right = cursor.x + 14;
			final int left = cursor.x - boxWidth - 10;
			if (right + boxWidth <= bounds.getMaxX())
			{
				x = right;
			}
			else if (left >= bounds.getMinX() + 4)
			{
				x = left;
			}
			x = Math.max((int) bounds.getMinX() + 4, Math.min(x, (int) bounds.getMaxX() - boxWidth - 4));
			if (cursor.x >= x && cursor.x < x + boxWidth && cursor.y >= y && cursor.y < y + boxHeight)
			{
				final int below = cursor.y + 14;
				final int above = cursor.y - boxHeight - 10;
				if (below + boxHeight <= bounds.getMaxY())
				{
					y = below;
				}
				else if (above >= bounds.getMinY() + 4)
				{
					y = above;
				}
				y = Math.max((int) bounds.getMinY() + 4, Math.min(y, (int) bounds.getMaxY() - boxHeight - 4));
			}
		}

		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setColor(CARD_BG);
		graphics.fillRoundRect(x, y, boxWidth, boxHeight, 7, 7);
		graphics.setColor(CARD_EDGE);
		graphics.drawRoundRect(x, y, boxWidth, boxHeight, 7, 7);

		if (icon != null)
		{
			graphics.drawImage(icon, x + padding, y + padding, iconSize, iconSize, null);
		}

		int titleY = y + padding + titleMetrics.getAscent();
		graphics.setFont(titleFont);
		graphics.setColor(CARD_TITLE);
		for (String titleRow : titleRows)
		{
			graphics.drawString(titleRow, x + padding + (iconSize + iconPad), titleY);
			titleY += titleLineHeight;
		}

		int bodyY = y + padding + titleRows.size() * titleLineHeight + bodyMetrics.getAscent();
		graphics.setFont(bodyFont);
		for (CardText.Row row : rows)
		{
			graphics.setColor(row.getColour());
			graphics.drawString(row.getText(), x + padding, bodyY);
			bodyY += bodyLineHeight;
		}

		if (panelSize != null)
		{
			trailingPanel.setBackgroundColor(null);
			trailingPanel.setPreferredSize(new Dimension(panelContentW, 0));
			trailingPanel.setPreferredLocation(new java.awt.Point(x + padding, (y + padding + titleRows.size() * titleLineHeight + rows.size() * bodyLineHeight) + panelGap));
			trailingPanel.render(graphics);
		}
	}

	/**
	 * The surface {@link #measurePanel} measures against. Nothing is ever drawn into it, so one
	 * buffer serves every call rather than one per card per frame. Client thread only.
	 */
	private static final BufferedImage MEASURE_SCRATCH = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);

	/**
	 * A {@link PanelComponent} reports a stale {@code (10,10)} size on its first {@code render}
	 * (the return value is read from a cache that the call only fills at its end), so measure
	 * with two throwaway renders on a 1x1 buffer whose font matches the real graphics.
	 */
	private static Dimension measurePanel(PanelComponent panel, Graphics2D reference)
	{
		final Graphics2D sg = MEASURE_SCRATCH.createGraphics();
		try
		{
			sg.setFont(reference.getFont());
			panel.render(sg);
			final Dimension d = panel.render(sg);
			return d != null ? d : new Dimension(0, 0);
		}
		finally
		{
			sg.dispose();
		}
	}
}
