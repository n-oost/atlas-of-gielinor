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
package atlasofgielinor.ui;

import static atlasofgielinor.ui.MapStyle.BOSSES;
import static atlasofgielinor.ui.MapStyle.CARD_BG;
import static atlasofgielinor.ui.MapStyle.CARD_EDGE;
import static atlasofgielinor.ui.MapStyle.CARD_PALETTE;
import static atlasofgielinor.ui.MapStyle.CARD_TITLE;
import static atlasofgielinor.ui.MapStyle.SMALL;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

import atlasofgielinor.AtlasOfGielinorConfig;
import atlasofgielinor.data.BossLocationData;
import atlasofgielinor.data.TravelData;
import atlasofgielinor.data.dungeons.UndergroundZone;
import atlasofgielinor.data.sailing.BoatTracker;
import atlasofgielinor.data.sailing.PlayerBoat;
import atlasofgielinor.data.sailing.PortNoticeBoard;
import atlasofgielinor.data.sailing.SailingPort;
import atlasofgielinor.map.catalog.BossLocationIndex;
import atlasofgielinor.map.DungeonPieceIndex;
import atlasofgielinor.map.catalog.GroundItemIndex;
import atlasofgielinor.map.InstanceMaps;
import atlasofgielinor.map.MapCamera;
import atlasofgielinor.map.MapFinder;
import atlasofgielinor.map.MonsterIconManager;
import atlasofgielinor.map.catalog.MonsterIndex;
import atlasofgielinor.map.catalog.PoiCategory;
import atlasofgielinor.map.catalog.PoiDetails;
import atlasofgielinor.map.catalog.PoiIndex;
import atlasofgielinor.map.PrifddinasShift;
import atlasofgielinor.map.catalog.ShopIndex;
import atlasofgielinor.integrations.SlayerTaskTracker;
import atlasofgielinor.map.WorldMapInput;
import atlasofgielinor.map.WorldMapPointReader;
import atlasofgielinor.ui.markers.RaidBossDisplay;
import atlasofgielinor.ui.tooltips.BoatTooltipBuilder;
import atlasofgielinor.ui.tooltips.MonsterTooltipBuilder;
import atlasofgielinor.ui.tooltips.PoiTooltipBuilder;
import atlasofgielinor.ui.tooltips.TooltipCard;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;

/**
 * The hover card: what is under the cursor, and the card that describes it.
 *
 * <p>Decomposed into specialized tooltip builders in {@code com.atlasofgielinor.ui.tooltips}.
 */
@Slf4j
public class MapTooltipRenderer
{
	private final AtlasOfGielinorConfig config;
	private final MapCamera camera;
	private final WorldMapInput input;
	private final PoiIndex poiIndex;
	private final MonsterIndex monsterIndex;
	private final DungeonPieceIndex dungeonPieceIndex;
	private final MonsterIconManager monsterIconManager;
	private final SlayerTaskTracker slayerTaskTracker;
	private final ShopIndex shopIndex;
	private final MapFinder mapFinder;
	private final GroundItemIndex groundItemIndex;
	private final BoatTracker boatTracker;
	private final WorldMapPointManager worldMapPointManager;
	private final WorldMapPointReader worldMapPointReader;
	private atlasofgielinor.integrations.StashUnitStatus stashUnitStatus;

	public void setStashUnitStatus(atlasofgielinor.integrations.StashUnitStatus tracker)
	{
		stashUnitStatus = tracker;
	}

	@Getter
	private final BoatTooltipBuilder boatTooltipBuilder;
	@Getter
	private final MonsterTooltipBuilder monsterTooltipBuilder;
	@Getter
	private final PoiTooltipBuilder poiTooltipBuilder;

	public MapTooltipRenderer(
		AtlasOfGielinorConfig config,
		MapCamera camera,
		WorldMapInput input,
		PoiIndex poiIndex,
		MonsterIndex monsterIndex,
		DungeonPieceIndex dungeonPieceIndex,
		MonsterIconManager monsterIconManager,
		SlayerTaskTracker slayerTaskTracker,
		ShopIndex shopIndex,
		MapFinder mapFinder,
		GroundItemIndex groundItemIndex,
		BoatTracker boatTracker,
		WorldMapPointManager worldMapPointManager,
		WorldMapPointReader worldMapPointReader)
	{
		this.config = config;
		this.camera = camera;
		this.input = input;
		this.poiIndex = poiIndex;
		this.monsterIndex = monsterIndex;
		this.dungeonPieceIndex = dungeonPieceIndex;
		this.monsterIconManager = monsterIconManager;
		this.slayerTaskTracker = slayerTaskTracker;
		this.shopIndex = shopIndex;
		this.mapFinder = mapFinder;
		this.groundItemIndex = groundItemIndex;
		this.boatTracker = boatTracker;
		this.worldMapPointManager = worldMapPointManager;
		this.worldMapPointReader = worldMapPointReader;

		this.boatTooltipBuilder = new BoatTooltipBuilder();
		this.monsterTooltipBuilder = new MonsterTooltipBuilder();
		this.poiTooltipBuilder = new PoiTooltipBuilder();
	}

	private <T> T hit(int worldX, int worldY, BiFunction<Integer, Integer, T> lookup)
	{
		return InstanceMaps.firstHit(worldX, worldY,
			camera.getFocusedUndergroundZone(), camera.isDungeonContentsFocused(),
			(x, y) -> layerAllows(x, y) ? lookup.apply(x, y) : null);
	}

	private boolean layerAllows(int worldX, int worldY)
	{
		return atlasofgielinor.ui.markers.ViewWindow.isDrawable(camera, worldX, worldY);
	}

	/**
	 * The card that follows the cursor. Bosses win over map icons, and failing both it still
	 * reports the coordinate under the cursor, so the map always answers "what is this".
	 */
	public void drawTooltip(Graphics2D graphics, Rectangle bounds)
	{
		drawTooltip(graphics, bounds, null);
	}

	private long raidStatsVersion = Long.MIN_VALUE;
	private final java.util.Map<BossLocationData, MonsterIndex.Zone> raidStats =
		new java.util.EnumMap<>(BossLocationData.class);

	public void drawRaidBossCard(Graphics2D graphics, Rectangle bounds, java.awt.Point cursor,
		BossLocationData boss, String raidName)
	{
		if (!config.showTooltips())
		{
			return;
		}
		final boolean compact = !config.expandedTooltips() && !input.isTooltipExpandModifierDown();
		final BufferedImage icon = monsterIconManager != null ? monsterIconManager.getBossIcon(boss, 20) : null;
		final boolean task = config.highlightSlayerTask() && slayerTaskTracker != null && slayerTaskTracker.isTaskBoss(boss);
		final int remaining = slayerTaskTracker == null ? 0 : slayerTaskTracker.getRemainingTaskAmount();
		if (monsterIndex != null && raidStatsVersion != monsterIndex.getDataVersion())
		{
			raidStats.clear();
			for (MonsterIndex.Zone zone : monsterIndex.getZones())
			{
				for (BossLocationData candidate : MapStyle.BOSSES)
				{
					if (RaidBossDisplay.isRaidBoss(candidate) && candidate.getName().equalsIgnoreCase(zone.getMonster()))
					{
						raidStats.putIfAbsent(candidate, zone);
					}
				}
			}
			raidStatsVersion = monsterIndex.getDataVersion();
		}
		final MonsterIndex.Zone stats = raidStats.get(boss);
		final TooltipCard card = monsterTooltipBuilder.buildBossCard(boss, icon, task, remaining, compact,
			stats);
		card.setPreserveCompactLines(true);
		if (boss.getCombatLevel() == 0)
		{
			card.setTitle(boss.getName());
			card.getLines().removeIf(line -> line.startsWith("Difficulty:"));
			card.addLine("Combat level scales with the raid");
		}
		card.getLines().removeIf(line -> line.startsWith("[Boss "));
		card.getLines().add(0, "[Boss • " + raidName + "]");
		drawCard(graphics, bounds, cursor, card);
	}

	void drawTooltip(Graphics2D graphics, Rectangle bounds, MapMarkerRenderers markers)
	{
		if (!config.showTooltips())
		{
			return;
		}
		final boolean compact = !config.expandedTooltips() && !input.isTooltipExpandModifierDown();

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
		final Rectangle boatsPanel = camera.getBoatsDropdownBounds();
		if (boatsPanel != null && boatsPanel.contains(cursor))
		{
			return;
		}

		final boolean travelFocused = camera.isTravelViewActive() || (config.showTravelRoutes()
			&& camera.getZoom() >= config.travelStationMinZoom() && camera.getSelectedTravelNode() != null);
		if (travelFocused && camera.getHoveredTravelNode() != null)
		{
			final TooltipCard card = poiTooltipBuilder.buildTravelNodeCard(camera.getHoveredTravelNode());
			if (card != null)
			{
				drawCard(graphics, bounds, cursor, card);
			}
			return;
		}
		if (!travelFocused && drawUndergroundZoneCard(graphics, bounds, cursor))
		{
			return;
		}

		final int worldX = (int) Math.floor(camera.worldX(cursor.x, bounds));
		final int worldY = (int) Math.floor(camera.worldY(cursor.y, bounds));
		final int plane = camera.getPlane();
		final int radius = hitRadius();
		final int pointRadius = Math.min(radius, 6);
		final HoverProbe probe = new HoverProbe(cursor, worldX, worldY, plane, pointRadius, markers);
		final WorldMapPoint drawnPoint = markers != null ? markers.pluginMarkers.visiblePointAt(cursor) : null;
		if (!travelFocused && drawnPoint != null && isStashPoint(drawnPoint))
		{
			drawCard(graphics, bounds, cursor, buildPluginPointCard(drawnPoint, compact));
			return;
		}
		if (drawPortCard(graphics, bounds, probe))
		{
			return;
		}
		final PoiIndex.Poi drawnPoi = markers != null ? markers.locations.visiblePoiIconAt(cursor) : null;
		if (drawnPoi != null)
		{
			if (camera.isTravelViewActive() && "teleport".equals(drawnPoi.getKey()))
			{
				final TooltipCard card = new TooltipCard("Teleport destination", poiIndex.icon("teleport"));
				for (String name : drawnPoi.getName().split(" \\| ")) card.addLine(name);
				card.addLine("Requires the corresponding spell, item or unlock.");
				drawCard(graphics, bounds, cursor, card);
				return;
			}
			if (shopIndex != null && PoiCategory.of(drawnPoi.getKey()) == PoiCategory.SHOPS)
			{
				final ShopIndex.Shop shop = shopIndex.nearest(drawnPoi.getX(), drawnPoi.getY(), plane, 3);
				if (shop != null)
				{
					final String findQuery = mapFinder != null ? mapFinder.getQuery() : null;
					final TooltipCard shopCard = poiTooltipBuilder.buildShopCard(shop, shopIndex.othersNear(shop, 6, 2), poiIndex, findQuery);
					if (shopCard != null)
					{
						drawCard(graphics, bounds, cursor, shopCard);
						return;
					}
				}
			}

			PoiDetails.Detail detail = PoiDetails.getDetail(drawnPoi, drawnPoi.getX(), drawnPoi.getY(), plane);
			detail = enrichDetailWithRuneLitePoint(detail, drawnPoi, plane);
			final BufferedImage icon = poiIndex != null ? poiIndex.icon(drawnPoi.getKey()) : null;
			final TooltipCard card = poiTooltipBuilder.buildPoiCard(detail, drawnPoi, icon, compact);
			if (card != null)
			{
				if (!compact)
				{
					card.addLine(worldX + ", " + worldY + " (Floor " + plane + ")");
				}
				drawCard(graphics, bounds, cursor, card);
				return;
			}
		}

		final ShopIndex.Shop drawnShop = markers != null ? markers.locations.visibleShopIconAt(cursor) : null;
		if (drawnShop != null && shopIndex != null)
		{
			final String findQuery = mapFinder != null ? mapFinder.getQuery() : null;
			final TooltipCard shopCard = poiTooltipBuilder.buildShopCard(drawnShop, shopIndex.othersNear(drawnShop, 6, 2), poiIndex, findQuery);
			if (shopCard != null)
			{
				if (!compact)
				{
					shopCard.addLine(drawnShop.getX() + ", " + drawnShop.getY() + " (Floor " + plane + ")");
				}
				drawCard(graphics, bounds, cursor, shopCard);
				return;
			}
		}

		if (travelFocused)
		{
			if (camera.isTravelViewActive()) drawBoatCard(graphics, bounds, probe);
			return;
		}

		if (drawBoatCard(graphics, bounds, probe))
		{
			return;
		}

		if (drawNoticeBoardCard(graphics, bounds, probe))
		{
			return;
		}

		if (drawTravelNodeCard(graphics, bounds, probe))
		{
			return;
		}

		TooltipCard card = null;

		// The active overlay already handled exact drawn POI icons above.
		final BossLocationData boss = monsterNear(worldX, worldY);
		if (boss != null && probe.allows(boss))
		{
			final BufferedImage bossIcon = monsterIconManager != null ? monsterIconManager.getBossIcon(boss, 20) : null;
			final boolean isTaskBoss = config.highlightSlayerTask() && slayerTaskTracker != null && slayerTaskTracker.isTaskBoss(boss);
			final int remaining = slayerTaskTracker != null ? slayerTaskTracker.getRemainingTaskAmount() : 0;
			final MonsterIndex.Zone stats = monsterIndex != null
				? monsterIndex.nearest(plane, boss.getWorldPoint().getX(), boss.getWorldPoint().getY(), 2)
				: null;
			final MonsterIndex.Zone bossStats = stats != null && boss.getName().equalsIgnoreCase(stats.getMonster()) ? stats : null;
			card = monsterTooltipBuilder.buildBossCard(boss, bossIcon, isTaskBoss, remaining, compact, bossStats);
		}

		final PoiIndex.Poi iconPoi = markers == null ? poiIconAt(cursor, bounds, plane) : null;
		if (card == null && iconPoi != null)
		{
			if (shopIndex != null && PoiCategory.of(iconPoi.getKey()) == PoiCategory.SHOPS)
			{
				final ShopIndex.Shop shop = shopIndex.nearest(iconPoi.getX(), iconPoi.getY(), plane, 3);
				if (shop != null)
				{
					final String findQuery = mapFinder != null ? mapFinder.getQuery() : null;
					card = poiTooltipBuilder.buildShopCard(shop, shopIndex.othersNear(shop, 6, 2), poiIndex, findQuery);
				}
			}
			if (card == null)
			{
				PoiDetails.Detail detail = PoiDetails.getDetail(iconPoi, iconPoi.getX(), iconPoi.getY(), plane);
				detail = enrichDetailWithRuneLitePoint(detail, iconPoi, plane);
				final BufferedImage tooltipIcon = poiIndex != null ? poiIndex.icon(iconPoi.getKey()) : null;
				card = poiTooltipBuilder.buildPoiCard(detail, iconPoi, tooltipIcon, compact);
			}
		}

		if (card == null && shopIndex != null)
		{
			final ShopIndex.Shop shop = hit(worldX, worldY,
				(x, y) -> shopIndex.nearest(x, y, plane, Math.min(radius, 10)));
			if (shop != null && probe.allows(shop))
			{
				final String findQuery = mapFinder != null ? mapFinder.getQuery() : null;
				card = poiTooltipBuilder.buildShopCard(shop, shopIndex.othersNear(shop, 6, 2), poiIndex, findQuery);
			}
		}

		if (card == null && markers == null)
		{
			// Tight radius (1 tile) for position hits so moving the cursor into adjacent grass does not trigger phantom cards
			final PoiDetails.Detail detail = hit(worldX, worldY,
				(x, y) -> PoiDetails.getDetailByPosition(x, y, plane, 1));
			if (detail != null)
			{
				card = poiTooltipBuilder.buildPoiCard(detail, null, null, compact);
			}
		}

		if (card == null)
		{
			// Only match RuneLite points that are ACTUALLY DRAWN as markers (requireImage = true)
			// with a tight radius (1 tile), so invisible overlay points (image == null) never show
			// up as ghost/phantom tooltips with no icon on empty terrain.
			final WorldMapPoint rPoint = findRuneLitePointNear(worldX, worldY, plane, 1, true);
			if (rPoint != null && probe.allows(rPoint))
			{
				card = buildPluginPointCard(rPoint, compact);
			}
		}

		// Ground items are exact tiles, so the hover radius stays tight.
		if (card == null && config.showGroundItems() && groundItemIndex != null && groundItemIndex.isLoaded()
			&& camera.getZoom() >= config.groundItemMinZoom())
		{
			final GroundItemIndex.Spawn spawn = hit(worldX, worldY,
				(x, y) -> groundItemIndex.nearest(x, y, plane, Math.min(radius, 4), config.groundItemMinValue()));
			if (spawn != null && probe.allows(spawn))
			{
				card = poiTooltipBuilder.buildGroundItemCard(spawn, monsterIconManager, compact);
			}
		}

		// Monster zones last - wide centroids must not steal shortcut / shop / quest hovers.
		if (card == null && config.showMonsterZones() && monsterIndex != null && monsterIndex.isLoaded())
		{
			final MonsterIndex.Zone monster = PrifddinasShift.firstHit(worldX, worldY,
				(x, y) -> layerAllows(x, y) ? monsterIndex.nearest(plane, x, y, pointRadius) : null);
			if (monster != null && probe.allows(monster))
			{
				final BufferedImage zoneIcon = monsterIconManager != null ? monsterIconManager.getZoneIcon(monster, 20) : null;
				final boolean isTask = config.highlightSlayerTask() && slayerTaskTracker != null && slayerTaskTracker.isTaskMonster(monster);
				final int remaining = slayerTaskTracker != null ? slayerTaskTracker.getRemainingTaskAmount() : 0;
			card = monsterTooltipBuilder.buildMonsterZoneCard(monster, zoneIcon, isTask, remaining, compact);
			}
		}

		if (card == null || card.isEmpty())
		{
			return;
		}

		if (!compact)
		{
			card.addLine(worldX + ", " + worldY + " (Floor " + plane + ")");
		}
		drawCard(graphics, bounds, cursor, card);
	}

	/**
	 * Where the cursor is, in screen and world terms, plus the hit radius for this zoom.
	 */
	private static final class HoverProbe
	{
		private final java.awt.Point cursor;
		private final int worldX;
		private final int worldY;
		private final int plane;
		private final int pointRadius;
		private final MapMarkerRenderers markers;

		private HoverProbe(java.awt.Point cursor, int worldX, int worldY, int plane, int pointRadius, MapMarkerRenderers markers)
		{
			this.cursor = cursor;
			this.worldX = worldX;
			this.worldY = worldY;
			this.plane = plane;
			this.pointRadius = pointRadius;
			this.markers = markers;
		}

		private boolean allows(Object target)
		{
			return markers == null || markers.isTooltipTargetVisible(target);
		}
	}

	/** Travel hub directory. Only when no tighter point marker is under the cursor. */
	private boolean drawTravelNodeCard(Graphics2D graphics, Rectangle bounds, HoverProbe probe)
	{
		final TravelData.TravelNode travelNode = camera.getHoveredTravelNode();
		if (config.showTravelRoutes() && travelNode != null && camera.getZoom() >= config.travelStationMinZoom()
			&& !hasTighterPointMarker(probe))
		{
			final TooltipCard card = poiTooltipBuilder.buildTravelNodeCard(travelNode);
			if (card != null)
			{
				drawCard(graphics, bounds, probe.cursor, card);
				return true;
			}
		}
		return false;
	}

	/** A sailing port under the cursor. */
	private boolean drawPortCard(Graphics2D graphics, Rectangle bounds, HoverProbe probe)
	{
		final SailingPort port = portNear(probe.worldX, probe.worldY, probe.plane);
		if (port != null && probe.allows(port))
		{
			final List<PlayerBoat> dockedBoats = boatTracker != null ? boatTracker.getBoatsAt(port) : Collections.emptyList();
			final TooltipCard card = boatTooltipBuilder.buildPortCard(port, dockedBoats, probe.plane);
			if (card != null)
			{
				drawCard(graphics, bounds, probe.cursor, card);
				return true;
			}
		}
		return false;
	}

	/** A port notice board under the cursor. */
	private boolean drawNoticeBoardCard(Graphics2D graphics, Rectangle bounds, HoverProbe probe)
	{
		final PortNoticeBoard noticeBoard = noticeBoardNear(probe.worldX, probe.worldY, probe.plane);
		if (noticeBoard != null && probe.allows(noticeBoard))
		{
			final TooltipCard card = boatTooltipBuilder.buildNoticeBoardCard(noticeBoard, probe.plane);
			if (card != null)
			{
				drawCard(graphics, bounds, probe.cursor, card);
				return true;
			}
		}
		return false;
	}

	/** The player's own boat. Point markers beat travel hubs, which hover wide. */
	private boolean drawBoatCard(Graphics2D graphics, Rectangle bounds, HoverProbe probe)
	{
		final PlayerBoat boat = boatNear(probe.worldX, probe.worldY, probe.plane);
		if (boat != null && probe.allows(boat) && boat.isOwned() && boat.getPort() != null)
		{
			final TooltipCard card = boatTooltipBuilder.buildBoatCard(boat, probe.plane);
			if (card != null)
			{
				drawCard(graphics, bounds, probe.cursor, card);
				return true;
			}
		}
		return false;
	}

	/** A hovered large layer symbol, surface or underground. */
	private boolean drawUndergroundZoneCard(Graphics2D graphics, Rectangle bounds, java.awt.Point cursor)
	{
		final UndergroundZone hoveredZone = camera.getHoveredUndergroundZone();
		if (hoveredZone != null)
		{
			final TooltipCard card = poiTooltipBuilder.buildUndergroundZoneCard(hoveredZone, camera.isHoveredSurfaceToUnderground());
			if (card != null)
			{
				drawCard(graphics, bounds, cursor, card);
				return true;
			}
		}
		return false;
	}

	/** True when a shop, boss, POI, or RuneLite pin is under the cursor (beats travel hubs). */
	private boolean hasTighterPointMarker(HoverProbe probe)
	{
		final int worldX = probe.worldX;
		final int worldY = probe.worldY;
		final int plane = probe.plane;
		final int pointRadius = probe.pointRadius;
		final BossLocationData boss = monsterNear(worldX, worldY);
		if (boss != null && probe.allows(boss))
		{
			return true;
		}
		final ShopIndex.Shop shop = shopIndex != null ? hit(worldX, worldY,
			(x, y) -> shopIndex.nearest(x, y, plane, Math.min(pointRadius, 10))) : null;
		if (shop != null && probe.allows(shop))
		{
			return true;
		}
		if (probe.markers != null)
		{
			if (probe.markers.locations.visiblePoiIconAt(probe.cursor) != null)
			{
				return true;
			}
		}
		else
		{
			if (poiIndex != null && hit(worldX, worldY,
				(x, y) -> poiIndex.nearest(x, y, plane, pointRadius)) != null)
			{
				return true;
			}
			if (hit(worldX, worldY,
				(x, y) -> PoiDetails.getDetailByPosition(x, y, plane, pointRadius)) != null)
			{
				return true;
			}
		}
		final WorldMapPoint point = findRuneLitePointNear(worldX, worldY, plane, pointRadius, true);
		return point != null && probe.allows(point);
	}

	private WorldMapPoint findRuneLitePointNear(int worldX, int worldY, int plane, int radius)
	{
		return findRuneLitePointNear(worldX, worldY, plane, radius, false);
	}

	private WorldMapPoint findRuneLitePointNear(int worldX, int worldY, int plane, int radius, boolean requireImage)
	{
		if (worldMapPointReader == null || worldMapPointManager == null)
		{
			return null;
		}
		for (WorldMapPoint point : worldMapPointReader.points(worldMapPointManager))
		{
			if (requireImage && point.getImage() == null)
			{
				continue;
			}
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

	TooltipCard buildPluginPointCard(WorldMapPoint point, boolean compact)
	{
		final WorldPoint wp = point.getWorldPoint();
		if (isStashPoint(point) && wp != null)
		{
			final atlasofgielinor.map.catalog.MapCatalog.Location location =
				atlasofgielinor.map.catalog.MapCatalog.current().locationAt(wp.getX(), wp.getY(), wp.getPlane(), "stash_unit");
			if (location != null)
			{
				final PoiIndex.Poi poi = new PoiIndex.Poi(location.x, location.y, location.plane,
					location.iconKey, location.label);
				PoiDetails.Detail detail = PoiDetails.getDetail(poi, poi.getX(), poi.getY(), poi.getPlane());
				detail = enrichDetailWithRuneLitePoint(detail, poi, poi.getPlane());
				return poiTooltipBuilder.buildPoiCard(detail, poi, point.getImage(), compact);
			}
		}
		return poiTooltipBuilder.buildRuneLitePointCard(point);
	}

	private static boolean isStashPoint(WorldMapPoint point)
	{
		final String name = point.getName();
		final String tooltip = point.getTooltip();
		return point.getClass().getSimpleName().toLowerCase(java.util.Locale.ROOT).contains("stash")
			|| (name != null && name.toUpperCase(java.util.Locale.ROOT).startsWith("STASH"))
			|| (tooltip != null && tooltip.toUpperCase(java.util.Locale.ROOT).startsWith("STASH"));
	}

	private PoiDetails.Detail enrichDetailWithRuneLitePoint(PoiDetails.Detail detail, PoiIndex.Poi poi, int plane)
	{
		if (poi != null && "stash_unit".equals(poi.getKey()))
			return stashUnitStatus != null ? stashUnitStatus.enrich(poi, detail) : detail;
		if (poi == null)
		{
			return detail;
		}
		final WorldMapPoint rPoint = findRuneLitePointNear(poi.getX(), poi.getY(), plane, 2, false);
		if (rPoint == null)
		{
			return detail;
		}
		final String rTip = rPoint.getTooltip();
		final String rName = rPoint.getName();
		final String specificTitle = (rTip != null && !rTip.isEmpty()) ? rTip : rName;
		if (specificTitle == null || specificTitle.isEmpty())
		{
			return detail;
		}
		final boolean isGeneric = detail == null
			|| detail.getTitle() == null
			|| detail.getTitle().equalsIgnoreCase(poi.getName())
			|| detail.getTitle().equalsIgnoreCase(poi.getKey())
			|| detail.getTitle().equalsIgnoreCase("Point of Interest");
		if (!isGeneric)
		{
			return detail;
		}
		final List<String> lines = new ArrayList<>();
		if (rTip != null && !rTip.isEmpty() && !rTip.equals(specificTitle))
		{
			lines.add(rTip);
		}
		if (detail != null && detail.getLines() != null)
		{
			lines.addAll(detail.getLines());
		}
		final String cat = detail != null && detail.getCategory() != null
			? detail.getCategory()
			: PoiCategory.of(poi.getKey()).name();
		return new PoiDetails.Detail(specificTitle, cat, lines);
	}

	/** Keeps the hit area a roughly constant size on screen as the zoom changes. */
	private int hitRadius()
	{
		return (int) Math.max(3, Math.ceil(12 / Math.max(0.4, camera.getFrameZoom())));
	}

	/** Hit-test POI icons using the same fixed screen bounds used to draw them. */
	private PoiIndex.Poi poiIconAt(java.awt.Point cursor, Rectangle bounds, int plane)
	{
		if (poiIndex == null || camera.getFrameZoom() < 0.45)
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
		if ((!camera.isTravelViewActive() && !config.showBoatLocations()) || boatTracker == null)
		{
			return null;
		}

		return hit(worldX, worldY,
			(x, y) -> boatTracker.getBoatNear(x, y, plane, hitRadius()));
	}

	private PortNoticeBoard noticeBoardNear(int worldX, int worldY, int plane)
	{
		if (!config.showPortNoticeBoards() || camera.getZoom() < 0.8)
		{
			return null;
		}

		return hit(worldX, worldY,
			(x, y) -> PortNoticeBoard.findBoardNear(x, y, plane, hitRadius()));
	}

	private SailingPort portNear(int worldX, int worldY, int plane)
	{
		if (!camera.isTravelViewActive() && !config.showSailingPorts())
		{
			return null;
		}

		return hit(worldX, worldY,
			(x, y) -> SailingPort.findPortNear(x, y, plane, hitRadius()));
	}

	private BossLocationData monsterNear(int worldX, int worldY)
	{
		if (!config.showBossLocations())
		{
			return null;
		}

		final int radius = hitRadius();

		for (BossLocationIndex.Location location : BossLocationIndex.all())
		{
			final BossLocationData monster = location.boss;
			if (RaidBossDisplay.isRaidBoss(monster))
			{
				continue;
			}
			final java.awt.geom.Point2D point = location.displayPoint(dungeonPieceIndex);
			final UndergroundZone bossZone = UndergroundZone.byId(location.zoneId);
			if (bossZone == null && location.plane != camera.getPlane()
				|| !location.visibleInFocusedLayer(camera)
				|| !layerAllows(location.x, location.y) && (location.zoneId == null || location.zoneId.isEmpty()))
			{
				continue;
			}

			if (Math.abs(point.getX() - worldX) <= radius && Math.abs(point.getY() - worldY) <= radius)
			{
				return monster;
			}
		}

		return null;
	}

	public void drawCard(Graphics2D graphics, Rectangle bounds, java.awt.Point cursor, TooltipCard card)
	{
		if (card == null || card.isEmpty())
		{
			return;
		}
		if (!config.expandedTooltips() && !input.isTooltipExpandModifierDown())
		{
			final String compactTitle = card.getTitle().replaceFirst(" \\(Lvl \\d+\\)$", "");
			final TooltipCard compactCard = new TooltipCard(compactTitle, card.getIcon());
			if (card.isPreserveCompactLines())
			{
				compactCard.addLines(card.getLines());
			}
			card = compactCard;
		}
		drawCard(graphics, bounds, cursor, card.getTitle(), card.getIcon(), card.getLines());
	}

	public void drawCard(Graphics2D graphics, Rectangle bounds, java.awt.Point cursor, String title, List<String> lines)
	{
		drawCard(graphics, bounds, cursor, title, null, lines);
	}

	public void drawCard(Graphics2D graphics, Rectangle bounds, java.awt.Point cursor, String title, BufferedImage icon, List<String> lines)
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

		int width = 0;
		for (String titleRow : titleRows)
		{
			width = Math.max(width, titleMetrics.stringWidth(titleRow) + (iconSize + iconPad));
		}
		for (CardText.Row row : rows)
		{
			width = Math.max(width, bodyMetrics.stringWidth(row.getText()));
		}
		width = Math.min(width, CardText.MAX_WIDTH_PX);

		final int boxWidth = width + padding * 2;
		final int textBlockHeight = titleRows.size() * titleLineHeight + rows.size() * bodyLineHeight;
		final int minContentHeight = icon != null ? iconSize : 0;
		final int boxHeight = Math.max(textBlockHeight, minContentHeight) + padding * 2;

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

	}

}
