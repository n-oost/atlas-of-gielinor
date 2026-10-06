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

import java.util.HashSet;
import java.util.Set;

import atlasofgielinor.AtlasOfGielinorConfig;
import atlasofgielinor.data.sailing.BoatTracker;
import atlasofgielinor.map.DungeonPieceIndex;
import atlasofgielinor.map.catalog.GroundItemIndex;
import atlasofgielinor.map.MapCamera;
import atlasofgielinor.map.MonsterIconManager;
import atlasofgielinor.map.catalog.MonsterIndex;
import atlasofgielinor.map.catalog.PoiIndex;
import atlasofgielinor.map.catalog.ShopIndex;
import atlasofgielinor.integrations.ShortestPathTracker;
import atlasofgielinor.integrations.SlayerTaskTracker;
import atlasofgielinor.map.WorldMapPointReader;
import atlasofgielinor.ui.markers.BoatMarkerRenderer;
import atlasofgielinor.ui.markers.FlashMarkerRenderer;
import atlasofgielinor.ui.markers.GroundItemMarkerRenderer;
import atlasofgielinor.ui.markers.LayerMarkerRenderer;
import atlasofgielinor.ui.markers.MonsterMarkerRenderer;
import atlasofgielinor.ui.markers.PlayerMarkerRenderer;
import atlasofgielinor.ui.markers.LocationMarkerRenderer;
import atlasofgielinor.ui.markers.RouteMarkerRenderer;
import atlasofgielinor.ui.markers.PluginMarkerRenderer;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;

/** Holds marker renderers and the targets drawn during the current frame. */
class MapMarkerRenderers
{
	final LocationMarkerRenderer locations;
	final MonsterMarkerRenderer monsters;
	final BoatMarkerRenderer boats;
	final PlayerMarkerRenderer player;
	final RouteMarkerRenderer routes;
	final LayerMarkerRenderer layers;
	final PluginMarkerRenderer pluginMarkers;
	final GroundItemMarkerRenderer groundItems;
	final FlashMarkerRenderer flash;
	private final Set<Object> visibleTooltipTargets = new HashSet<>();

	MapMarkerRenderers(
		Client client,
		AtlasOfGielinorConfig config,
		MapCamera camera,
		PoiIndex poiIndex,
		ShopIndex shopIndex,
		MonsterIndex monsterIndex,
		MonsterIconManager monsterIconManager,
		SlayerTaskTracker slayerTaskTracker,
		ShortestPathTracker shortestPathTracker,
		GroundItemIndex groundItemIndex,
		BoatTracker boatTracker,
		WorldMapPointManager worldMapPointManager,
		WorldMapPointReader worldMapPointReader,
		DungeonPieceIndex dungeonPieceIndex)
	{
		locations = new LocationMarkerRenderer(config, camera, poiIndex, shopIndex, dungeonPieceIndex, visibleTooltipTargets);
		monsters = new MonsterMarkerRenderer(config, camera, monsterIndex, monsterIconManager, slayerTaskTracker, dungeonPieceIndex, visibleTooltipTargets);
		boats = new BoatMarkerRenderer(config, camera, poiIndex, boatTracker, visibleTooltipTargets);
		player = new PlayerMarkerRenderer(client, config, camera, dungeonPieceIndex);
		routes = new RouteMarkerRenderer(config, camera, shortestPathTracker);
		layers = new LayerMarkerRenderer(config, camera, poiIndex);
		pluginMarkers = new PluginMarkerRenderer(config, camera, worldMapPointManager, worldMapPointReader, visibleTooltipTargets);
		groundItems = new GroundItemMarkerRenderer(config, camera, monsterIconManager, groundItemIndex, visibleTooltipTargets);
		flash = new FlashMarkerRenderer(camera);
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
}
