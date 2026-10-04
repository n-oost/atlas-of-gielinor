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

import com.bettermap.BetterMapConfig;
import com.bettermap.data.sailing.BoatTracker;
import com.bettermap.map.ClueScrollTracker;
import com.bettermap.map.DungeonPieceIndex;
import com.bettermap.map.GroundItemIndex;
import com.bettermap.map.MapCamera;
import com.bettermap.map.MonsterIconManager;
import com.bettermap.map.MonsterIndex;
import com.bettermap.map.PoiIndex;
import com.bettermap.map.ShopIndex;
import com.bettermap.map.ShortestPathTracker;
import com.bettermap.map.SlayerTaskTracker;
import com.bettermap.map.WorldMapPointReader;
import com.bettermap.ui.markers.BoatMarkerRenderer;
import com.bettermap.ui.markers.ClueMarkerRenderer;
import com.bettermap.ui.markers.FlashMarkerRenderer;
import com.bettermap.ui.markers.GroundItemMarkerRenderer;
import com.bettermap.ui.markers.LayerMarkerRenderer;
import com.bettermap.ui.markers.MonsterMarkerRenderer;
import com.bettermap.ui.markers.PlayerMarkerRenderer;
import com.bettermap.ui.markers.PoiMarkerRenderer;
import com.bettermap.ui.markers.RouteMarkerRenderer;
import com.bettermap.ui.markers.WorldMapPointMarkerRenderer;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;

/** Coordinates marker passes and their shared tooltip visibility. */
class MapMarkerRenderer
{
	private final PoiMarkerRenderer poiMarkerRenderer;
	private final MonsterMarkerRenderer monsterMarkerRenderer;
	private final BoatMarkerRenderer boatMarkerRenderer;
	private final PlayerMarkerRenderer playerMarkerRenderer;
	private final ClueMarkerRenderer clueMarkerRenderer;
	private final RouteMarkerRenderer routeMarkerRenderer;
	private final LayerMarkerRenderer layerMarkerRenderer;
	private final WorldMapPointMarkerRenderer worldMapPointMarkerRenderer;
	private final GroundItemMarkerRenderer groundItemMarkerRenderer;
	private final FlashMarkerRenderer flashMarkerRenderer;
	private final Set<Object> visibleTooltipTargets = new HashSet<>();

	MapMarkerRenderer(
		Client client,
		BetterMapConfig config,
		MapCamera camera,
		PoiIndex poiIndex,
		ShopIndex shopIndex,
		MonsterIndex monsterIndex,
		MonsterIconManager monsterIconManager,
		SlayerTaskTracker slayerTaskTracker,
		ClueScrollTracker clueScrollTracker,
		ShortestPathTracker shortestPathTracker,
		GroundItemIndex groundItemIndex,
		BoatTracker boatTracker,
		WorldMapPointManager worldMapPointManager,
		WorldMapPointReader worldMapPointReader,
		MapRenderStats stats,
		DungeonPieceIndex dungeonPieceIndex)
	{
		poiMarkerRenderer = new PoiMarkerRenderer(config, camera, poiIndex, shopIndex, stats, dungeonPieceIndex, visibleTooltipTargets);
		monsterMarkerRenderer = new MonsterMarkerRenderer(config, camera, monsterIndex, monsterIconManager, slayerTaskTracker, stats, dungeonPieceIndex, visibleTooltipTargets);
		boatMarkerRenderer = new BoatMarkerRenderer(config, camera, poiIndex, boatTracker, stats, visibleTooltipTargets);
		playerMarkerRenderer = new PlayerMarkerRenderer(client, config, camera, dungeonPieceIndex);
		clueMarkerRenderer = new ClueMarkerRenderer(config, camera, clueScrollTracker, stats, visibleTooltipTargets);
		routeMarkerRenderer = new RouteMarkerRenderer(config, camera, shortestPathTracker);
		layerMarkerRenderer = new LayerMarkerRenderer(config, camera, poiIndex);
		worldMapPointMarkerRenderer = new WorldMapPointMarkerRenderer(config, camera, worldMapPointManager, worldMapPointReader, visibleTooltipTargets);
		groundItemMarkerRenderer = new GroundItemMarkerRenderer(config, camera, monsterIconManager, groundItemIndex, stats, visibleTooltipTargets);
		flashMarkerRenderer = new FlashMarkerRenderer(camera);
	}

	/** Reset before drawing markers so hidden targets cannot retain hover cards from the last frame. */
	void beginFrame()
	{
		visibleTooltipTargets.clear();
	}

	boolean isTooltipTargetVisible(Object target)
	{
		return visibleTooltipTargets.contains(target);
	}

	void drawPoiIcons(Graphics2D graphics, Rectangle bounds, List<Rectangle> placed)
	{
		poiMarkerRenderer.drawPoiIcons(graphics, bounds, placed);
	}

	void drawPlaceNames(Graphics2D graphics, Rectangle bounds)
	{
		poiMarkerRenderer.drawPlaceNames(graphics, bounds);
	}

	boolean shouldDrawPoiIcon(PoiIndex.Poi poi)
	{
		return poiMarkerRenderer.shouldDrawPoiIcon(poi);
	}

	PoiIndex.Poi visiblePoiIconAt(java.awt.Point cursor)
	{
		return poiMarkerRenderer.visiblePoiIconAt(cursor);
	}

	ShopIndex.Shop visibleShopIconAt(java.awt.Point cursor)
	{
		return poiMarkerRenderer.visibleShopIconAt(cursor);
	}

	void drawMonsters(Graphics2D graphics, Rectangle bounds, List<Rectangle> placed)
	{
		monsterMarkerRenderer.drawMonsters(graphics, bounds, placed);
	}

	void drawMonsterZones(Graphics2D graphics, Rectangle bounds, List<Rectangle> placed)
	{
		monsterMarkerRenderer.drawMonsterZones(graphics, bounds, placed);
	}

	void drawSailingPorts(Graphics2D graphics, Rectangle bounds, List<Rectangle> placed)
	{
		boatMarkerRenderer.drawSailingPorts(graphics, bounds, placed);
	}

	void drawPortNoticeBoards(Graphics2D graphics, Rectangle bounds, List<Rectangle> placed)
	{
		boatMarkerRenderer.drawPortNoticeBoards(graphics, bounds, placed);
	}

	void drawPlayerBoats(Graphics2D graphics, Rectangle bounds, List<Rectangle> placed)
	{
		boatMarkerRenderer.drawPlayerBoats(graphics, bounds, placed);
	}

	void drawPlayer(Graphics2D graphics, Rectangle bounds)
	{
		playerMarkerRenderer.drawPlayer(graphics, bounds);
	}

	void drawClueMarkers(Graphics2D graphics, Rectangle bounds, List<Rectangle> placed)
	{
		clueMarkerRenderer.drawClueMarkers(graphics, bounds, placed);
	}

	boolean drawShortestPathRoute(Graphics2D graphics, Rectangle bounds)
	{
		return routeMarkerRenderer.drawShortestPathRoute(graphics, bounds);
	}

	void drawTravelStations(Graphics2D graphics, Rectangle bounds)
	{
		routeMarkerRenderer.drawTravelStations(graphics, bounds);
	}

	void drawTravelRoutes(Graphics2D graphics, Rectangle bounds)
	{
		routeMarkerRenderer.drawTravelRoutes(graphics, bounds);
	}

	void drawLargeLayerSymbols(Graphics2D graphics, Rectangle bounds)
	{
		layerMarkerRenderer.drawLargeLayerSymbols(graphics, bounds);
	}

	void drawWorldMapPoints(Graphics2D graphics, Rectangle bounds, List<Rectangle> placed)
	{
		worldMapPointMarkerRenderer.drawWorldMapPoints(graphics, bounds, placed);
	}

	void drawGroundItems(Graphics2D graphics, Rectangle bounds, List<Rectangle> placed)
	{
		groundItemMarkerRenderer.drawGroundItems(graphics, bounds, placed);
	}

	void drawFlash(Graphics2D graphics, Rectangle bounds)
	{
		flashMarkerRenderer.drawFlash(graphics, bounds);
	}
}
