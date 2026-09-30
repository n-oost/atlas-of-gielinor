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
package com.bettermap;

import com.bettermap.pathfinding.TeleportationItem;
import com.bettermap.ui.input.PanButton;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup("bettermap")
public interface BetterMapConfig extends Config
{
	@ConfigSection(
		name = "Map",
		description = "Which map is drawn, and what is drawn on it.",
		position = 0
	)
	String mapSection = "map";

	@ConfigSection(
		name = "Routing (Shortest Path by Skretzo)",
		description = "Better Map's route engine uses a pinned, BSD-2-Clause copy of Shortest Path by Skretzo and contributors. "
			+ "Choose one routing mode. The settings below apply only to Better Map routing. "
			+ "Better Map is not affiliated with Shortest Path.",
		position = 5
	)
	String pathfindingSection = "pathfinding";

	@ConfigSection(
		name = "Developer",
		description = "Tools for tuning the dungeon layers. Safe to leave alone.",
		position = 90,
		closedByDefault = true
	)
	String developerSection = "developer";

	@ConfigSection(
		name = "Controls",
		description = "Panning and zooming",
		position = 10
	)
	String controlsSection = "controls";

	@ConfigSection(
		name = "Map Icons",
		description = "Location badges, markers, monsters and sailing icons drawn over the map",
		position = 20
	)
	String iconsSection = "icons";

	@ConfigSection(
		name = "Developer / Debug",
		description = "Diagnostics and pipeline inspection",
		position = 30
	)
	String debugSection = "debug";

	@ConfigItem(
		keyName = "useExternalShortestPathSettings",
		name = "Use external plugin settings",
		description = "Use the enabled Shortest Path plugin's saved routing settings. Keep that plugin enabled separately. "
			+ "Enabling this turns off Better Map routing. Better Map calculates its map line with its pinned "
			+ "Skretzo Shortest Path fork; routes may differ from newer external versions.",
		position = -1,
		section = pathfindingSection
	)
	default boolean useExternalShortestPathSettings()
	{
		return false;
	}

	@ConfigItem(
		keyName = "enableShortestPath",
		name = "Use Better Map routing",
		description = "Calculate and draw routes on Better Map using a pinned, BSD-2-Clause fork of the Shortest Path engine "
			+ "by Skretzo and contributors. Uses the settings below; no external plugin needed. "
			+ "Enabling this turns off external settings mode. "
			+ "Better Map is not affiliated with the Shortest Path project.",
		position = 0,
		section = pathfindingSection
	)
	default boolean enableShortestPath()
	{
		return true;
	}

	@ConfigItem(
		keyName = "routeAvoidWilderness",
		name = "Avoid wilderness",
		description = "Whether the wilderness should be avoided if possible (otherwise, will e.g. use wilderness lever from Edgeville to Ardougne)",
		position = 1,
		section = pathfindingSection
	)
	default boolean routeAvoidWilderness()
	{
		return true;
	}

	@ConfigItem(
		keyName = "routeUseAgilityShortcuts",
		name = "Use agility shortcuts",
		description = "Whether to include agility shortcuts in the path. You must also have the required agility level",
		position = 2,
		section = pathfindingSection
	)
	default boolean routeUseAgilityShortcuts()
	{
		return true;
	}

	@ConfigItem(
		keyName = "routeUseGrappleShortcuts",
		name = "Use grapple shortcuts",
		description = "Whether to include crossbow grapple agility shortcuts in the path. You must also have the required agility, ranged and strength levels",
		position = 3,
		section = pathfindingSection
	)
	default boolean routeUseGrappleShortcuts()
	{
		return false;
	}

	@ConfigItem(
		keyName = "routeUseBoats",
		name = "Use boats",
		description = "Whether to include small boats in the path (e.g. the boat to Fishing Platform)",
		position = 4,
		section = pathfindingSection
	)
	default boolean routeUseBoats()
	{
		return true;
	}

	@ConfigItem(
		keyName = "routeUseCanoes",
		name = "Use canoes",
		description = "Whether to include canoes in the path",
		position = 5,
		section = pathfindingSection
	)
	default boolean routeUseCanoes()
	{
		return false;
	}

	@ConfigItem(
		keyName = "routeUseCharterShips",
		name = "Use charter ships",
		description = "Whether to include charter ships in the path",
		position = 6,
		section = pathfindingSection
	)
	default boolean routeUseCharterShips()
	{
		return false;
	}

	@ConfigItem(
		keyName = "routeUseShips",
		name = "Use ships",
		description = "Whether to include passenger ships in the path (e.g. the customs ships to Karamja)",
		position = 7,
		section = pathfindingSection
	)
	default boolean routeUseShips()
	{
		return true;
	}

	@ConfigItem(
		keyName = "routeUseFairyRings",
		name = "Use fairy rings",
		description = "Whether to include fairy rings in the path. You must also have completed the required quests or miniquests",
		position = 8,
		section = pathfindingSection
	)
	default boolean routeUseFairyRings()
	{
		return true;
	}

	@ConfigItem(
		keyName = "routeUseGnomeGliders",
		name = "Use gnome gliders",
		description = "Whether to include gnome gliders in the path",
		position = 9,
		section = pathfindingSection
	)
	default boolean routeUseGnomeGliders()
	{
		return true;
	}

	@ConfigItem(
		keyName = "routeUseHotAirBalloons",
		name = "Use hot air balloons",
		description = "Whether to include hot air balloons in the path",
		position = 10,
		section = pathfindingSection
	)
	default boolean routeUseHotAirBalloons()
	{
		return false;
	}

	@ConfigItem(
		keyName = "routeUseMagicCarpets",
		name = "Use magic carpets",
		description = "Whether to include magic carpets in the path",
		position = 11,
		section = pathfindingSection
	)
	default boolean routeUseMagicCarpets()
	{
		return true;
	}

	@ConfigItem(
		keyName = "routeUseMagicMushtrees",
		name = "Use magic mushtrees",
		description = "Whether to include Fossil Island Magic Mushtrees in the path",
		position = 12,
		section = pathfindingSection
	)
	default boolean routeUseMagicMushtrees()
	{
		return true;
	}

	@ConfigItem(
		keyName = "routeUseMinecarts",
		name = "Use minecarts",
		description = "Whether to include minecarts in the path (e.g. the Keldagrim and Lovakengj minecart networks)",
		position = 13,
		section = pathfindingSection
	)
	default boolean routeUseMinecarts()
	{
		return true;
	}

	@ConfigItem(
		keyName = "routeUseQuetzals",
		name = "Use quetzals",
		description = "Whether to include quetzals in the path",
		position = 14,
		section = pathfindingSection
	)
	default boolean routeUseQuetzals()
	{
		return true;
	}

	@ConfigItem(
		keyName = "routeUseSpiritTrees",
		name = "Use spirit trees",
		description = "Whether to include spirit trees in the path",
		position = 15,
		section = pathfindingSection
	)
	default boolean routeUseSpiritTrees()
	{
		return true;
	}

	@ConfigItem(
		keyName = "routeUseTeleportationItems",
		name = "Use teleportation items",
		description = "Whether to include teleportation items from the player's inventory and equipment",
		position = 16,
		section = pathfindingSection
	)
	default TeleportationItem routeUseTeleportationItems()
	{
		return TeleportationItem.INVENTORY_NON_CONSUMABLE;
	}

	@ConfigItem(
		keyName = "routeUseTeleportationLevers",
		name = "Use teleportation levers",
		description = "Whether to include teleportation levers in the path (e.g. the lever from Edgeville to Wilderness)",
		position = 17,
		section = pathfindingSection
	)
	default boolean routeUseTeleportationLevers()
	{
		return true;
	}

	@ConfigItem(
		keyName = "routeUseTeleportationPortals",
		name = "Use teleportation portals",
		description = "Whether to include teleportation portals in the path (e.g. the portal from Ferox Enclave to Castle Wars)",
		position = 18,
		section = pathfindingSection
	)
	default boolean routeUseTeleportationPortals()
	{
		return true;
	}

	@ConfigItem(
		keyName = "routeUseTeleportationSpells",
		name = "Use teleportation spells",
		description = "Whether to include teleportation spells in the path",
		position = 19,
		section = pathfindingSection
	)
	default boolean routeUseTeleportationSpells()
	{
		return true;
	}

	@ConfigItem(
		keyName = "routeUseTeleportationSpellsHome",
		name = "Use Home Teleport spells",
		description = "Whether to include Home Teleport spells in the path",
		position = 20,
		section = pathfindingSection
	)
	default boolean routeUseTeleportationSpellsHome()
	{
		return true;
	}

	@ConfigItem(
		keyName = "routeUseTeleportationMinigames",
		name = "Use teleportation to minigames",
		description = "Whether to include teleportation to minigames/activities/grouping in the path. These teleports share a 20 minute cooldown",
		position = 21,
		section = pathfindingSection
	)
	default boolean routeUseTeleportationMinigames()
	{
		return true;
	}

	@ConfigItem(
		keyName = "routeUseWildernessObelisks",
		name = "Use wilderness obelisks",
		description = "Whether to include wilderness obelisks in the path",
		position = 22,
		section = pathfindingSection
	)
	default boolean routeUseWildernessObelisks()
	{
		return true;
	}

	@ConfigItem(
		keyName = "routeUseSeasonalTransports",
		name = "Use seasonal transports",
		description = "Whether to include seasonal transports like League teleports in the path",
		position = 23,
		section = pathfindingSection
	)
	default boolean routeUseSeasonalTransports()
	{
		return false;
	}

	@ConfigItem(
		keyName = "routeCurrencyThreshold",
		name = "Currency threshold",
		description = "The maximum amount of currency to use on a single transportation method. "
			+ "Affects coins, trading sticks, ecto-tokens and warrior guild tokens",
		position = 24,
		section = pathfindingSection
	)
	default int routeCurrencyThreshold()
	{
		return 100000;
	}

	@ConfigItem(
		keyName = "routeCancelInstead",
		name = "Cancel instead of recalculating",
		description = "Whether the path should be cancelled rather than recalculated when the recalculate distance limit is exceeded",
		position = 25,
		section = pathfindingSection
	)
	default boolean routeCancelInstead()
	{
		return false;
	}

	@ConfigItem(
		keyName = "routeRecalculateDistance",
		name = "Recalculate distance",
		description = "Distance from the path the player should be for it to be recalculated (-1 for never)",
		position = 26,
		section = pathfindingSection
	)
	@Range(min = -1, max = 20000)
	default int routeRecalculateDistance()
	{
		return 10;
	}

	@ConfigItem(
		keyName = "routeFinishDistance",
		name = "Finish distance",
		description = "Distance from the target tile at which the path should be ended (-1 for never)",
		position = 27,
		section = pathfindingSection
	)
	@Range(min = -1, max = 50)
	default int routeFinishDistance()
	{
		return 5;
	}

	@ConfigItem(
		keyName = "routeUnreachableTargetDistance",
		name = "Unreachable target distance",
		description = "Distance from the target at which a finished path is considered not to reach the target",
		position = 28,
		section = pathfindingSection
	)
	@Range(max = 20000)
	default int routeUnreachableTargetDistance()
	{
		return 2;
	}

	@ConfigItem(
		keyName = "routeCalculationCutoff",
		name = "Calculation cutoff",
		description = "The cutoff threshold in number of ticks (0.6 seconds) of no progress being made towards the "
			+ "path target before the calculation will be stopped",
		position = 29,
		section = pathfindingSection
	)
	@Range(min = 1, max = 30)
	default int routeCalculationCutoff()
	{
		return 5;
	}

	@ConfigItem(
		keyName = "useCustomMap",
		name = "Use Custom Map",
		description = "Replaces the client world map with the installed map asset pack",
		position = 1,
		section = mapSection
	)
	default boolean useCustomMap()
	{
		return true;
	}

	@ConfigItem(
		keyName = "downloadMapAssets",
		name = "Download map assets",
		description = "Downloads and installs the complete map from GitHub and checks for updates at startup. Toggle off and on to retry a failed download.",
		warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers",
		position = 0,
		section = mapSection
	)
	default boolean downloadMapAssets()
	{
		return false;
	}

	@ConfigItem(
		keyName = "hideGameMapRender",
		name = "Hide Game Map Render",
		description = "Stops the client drawing its own map underneath ours. Turn off if the map window misbehaves",
		position = 2,
		section = mapSection
	)
	default boolean hideGameMapRender()
	{
		return true;
	}

	@ConfigItem(
		keyName = "hideMinimapOnOpen",
		name = "Hide Minimap While Open",
		description = "Hides the overview minimap in the top-right corner while the world map is open",
		position = 3,
		section = mapSection
	)
	default boolean hideMinimapOnOpen()
	{
		return false;
	}

	@ConfigItem(
		keyName = "fullscreenMap",
		name = "Fullscreen Map",
		description = "Draws the map over the whole client instead of inside the game's small map window. Press Escape to close it",
		position = 4,
		section = mapSection
	)
	default boolean fullscreenMap()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showCoordinateOverlay",
		name = "Show Status Line",
		description = "Shows the current layer, zoom and cursor coordinates along the top-left of the map",
		position = 5,
		section = mapSection
	)
	default boolean showCoordinateOverlay()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showPlaneSwitcher",
		name = "Show Floor Switcher",
		description = "Displays the floor selector buttons (0 to 3) on the right side of the map",
		position = 6,
		section = mapSection
	)
	default boolean showPlaneSwitcher()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showTooltips",
		name = "Hover Tooltips",
		description = "Shows the name and details of whatever is under the cursor",
		position = 7,
		section = mapSection
	)
	default boolean showTooltips()
	{
		return true;
	}

	@ConfigItem(
		keyName = "expandedTooltips",
		name = "Expanded Tooltips",
		description = "Shows detailed tooltip information by default; turn off for icon-and-name tooltips",
		position = 8,
		section = mapSection
	)
	default boolean expandedTooltips()
	{
		return false;
	}

	@ConfigItem(
		keyName = "tooltipExpandModifier",
		name = "Expand Tooltips With",
		description = "Hold this modifier to temporarily show full tooltip details",
		position = 9,
		section = mapSection
	)
	default TooltipModifier tooltipExpandModifier()
	{
		return TooltipModifier.ALT;
	}

	enum TooltipModifier
	{
		SHIFT, ALT, CTRL
	}

	@ConfigItem(
		keyName = "showLargeUndergroundSymbols",
		name = "Underground Layer Symbols",
		description = "Draws dungeon entrance toggles and green regional underground-composite toggles",
		position = 29,
		section = iconsSection
	)
	default boolean showLargeUndergroundSymbols()
	{
		return true;
	}

	@ConfigItem(
		keyName = "undergroundHoverPreview",
		name = "Layer Hover Preview",
		description = "Hovering over layer symbols dynamically reveals the opposite plane on the map",
		position = 10,
		section = mapSection
	)
	default boolean undergroundHoverPreview()
	{
		return true;
	}

	@ConfigItem(
		keyName = "undergroundOverlayOpacity",
		name = "Hover Surface Opacity",
		description = "How visible the overworld stays while previewing a dungeon on hover",
		position = 11,
		section = mapSection
	)
	@Range(min = 10, max = 100)
	default int undergroundOverlayOpacity()
	{
		return 90;
	}

	@ConfigItem(
		keyName = "undergroundHoverBlur",
		name = "Hover Overworld Blur",
		description = "How much to blur the overworld while previewing a dungeon on hover. 0 disables",
		position = 12,
		section = mapSection
	)
	@Range(min = 0, max = 8)
	default int undergroundHoverBlur()
	{
		return 4;
	}

	@ConfigItem(
		keyName = "undergroundSymbolSize",
		name = "Dungeon Enter / Exit Size",
		description = "Pixel size of dungeon enter and exit symbols. Smaller is less prominent",
		position = 31,
		section = iconsSection
	)
	@Range(min = 8, max = 32)
	default int undergroundSymbolSize()
	{
		return 14;
	}

	@ConfigItem(
		keyName = "showTravelRoutes",
		name = "Travel Routes on Hover",
		description = "Draws route lines, destination beacons and prices when hovering a travel station",
		position = 27,
		section = iconsSection
	)
	default boolean showTravelRoutes()
	{
		return true;
	}

	@ConfigItem(
		keyName = "animateTravelRoutes",
		name = "Animate Travel Routes",
		description = "Animates route line dashes and pulsing destination beacons",
		position = 30,
		section = iconsSection
	)
	default boolean animateTravelRoutes()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showTravelPriceTags",
		name = "Travel Price Badges",
		description = "Shows destination name and exact cost badges next to each route endpoint",
		position = 28,
		section = iconsSection
	)
	default boolean showTravelPriceTags()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showFinderOrb",
		name = "Show Quick-Find Orb",
		description = "Adds a search orb by the minimap that opens the location finder without the world map",
		position = 14,
		section = mapSection
	)
	default boolean showFinderOrb()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showFinderButton",
		name = "Show Find Button",
		description = "Adds a search button to the map that finds locations by name and jumps the camera to them",
		position = 15,
		section = mapSection
	)
	default boolean showFinderButton()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showSidebarPanel",
		name = "Show Sidebar Panel",
		description = "Displays the Better Map panel in RuneLite's sidebar",
		position = 16,
		section = mapSection
	)
	default boolean showSidebarPanel()
	{
		return true;
	}

	@ConfigItem(
		keyName = "finderSearchItems",
		name = "Find Shops & Ground Items",
		description = "Include the nearest shop that stocks an item, and nearby ground spawns of it, in Find results",
		position = 22,
		section = mapSection
	)
	default boolean finderSearchItems()
	{
		return true;
	}

	@ConfigItem(
		keyName = "finderEchoChat",
		name = "Echo Find Results To Chat",
		description = "As you type in Find, print the 3 nearest matches (name and rough distance) to the game chat",
		position = 23,
		section = mapSection
	)
	default boolean finderEchoChat()
	{
		return false;
	}

	@ConfigItem(
		keyName = "undergroundTransparentVoid",
		name = "See-through Dungeon Void",
		description = "While a dungeon layer is open, knock its black void out to transparent and show a "
			+ "dimmed, blurred surface behind it so the layer reads as a cutaway.",
		position = 1,
		section = developerSection
	)
	default boolean undergroundTransparentVoid()
	{
		return true;
	}

	@ConfigItem(
		keyName = "undergroundLayerSurfaceOpacity",
		name = "Dungeon Layer · Surface Opacity",
		description = "How visible the surface stays behind an OPEN dungeon layer. Lower is darker. "
			+ "0 hides it entirely (pure cutaway). Separate from the hover-preview opacity.",
		position = 2,
		section = developerSection
	)
	@Range(min = 0, max = 80)
	default int undergroundLayerSurfaceOpacity()
	{
		return 18;
	}

	@ConfigItem(
		keyName = "undergroundLayerSurfaceBlur",
		name = "Dungeon Layer · Surface Blur",
		description = "How much to blur the surface behind an OPEN dungeon layer. Higher is softer.",
		position = 3,
		section = developerSection
	)
	@Range(min = 0, max = 12)
	default int undergroundLayerSurfaceBlur()
	{
		return 9;
	}

	@ConfigItem(
		keyName = "undergroundTuner",
		name = "Dungeon Layer Tuner",
		description = "While a dungeon layer is open: arrow keys slide its tiles over the surface, "
			+ "[ and ] tighten/loosen its clip, R resets, P prints the corrected coordinates to chat.",
		position = 4,
		section = developerSection
	)
	default boolean undergroundTuner()
	{
		return false;
	}

	@ConfigItem(
		keyName = "finderOrbOffsetX",
		name = "",
		description = "",
		hidden = true
	)
	default int finderOrbOffsetX()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "finderOrbOffsetY",
		name = "",
		description = "",
		hidden = true
	)
	default int finderOrbOffsetY()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "zoomSpeed",
		name = "Zoom Speed",
		description = "How far one notch of the mouse wheel zooms",
		position = 1,
		section = controlsSection
	)
	@Range(min = 1, max = 5)
	default int zoomSpeed()
	{
		return 3;
	}

	@ConfigItem(
		keyName = "defaultZoom",
		name = "Default Zoom Level",
		description = "Initial zoom level applied when opening the map on the player",
		position = 2,
		section = controlsSection
	)
	@Range(min = 1, max = 5)
	default int defaultZoom()
	{
		return 2;
	}

	@ConfigItem(
		keyName = "invertZoom",
		name = "Invert Zoom Direction",
		description = "Inverts mouse wheel scroll direction for zooming in and out",
		position = 3,
		section = controlsSection
	)
	default boolean invertZoom()
	{
		return false;
	}

	@ConfigItem(
		keyName = "rememberMapPosition",
		name = "Remember Map Position",
		description = "Preserves your last panned position across opens instead of always re-centering on the player",
		position = 4,
		section = controlsSection
	)
	default boolean rememberMapPosition()
	{
		return false;
	}

	@ConfigItem(
		keyName = "dragToPan",
		name = "Drag To Pan",
		description = "Hold the pan button and drag to move the map, as in most modern map interfaces",
		position = 5,
		section = controlsSection
	)
	default boolean dragToPan()
	{
		return true;
	}

	@ConfigItem(
		keyName = "panButton",
		name = "Pan Button",
		description = "Which mouse button drag-pans the map. Map buttons (close, floors, dungeons) stay on left click",
		position = 6,
		section = controlsSection
	)
	default PanButton panButton()
	{
		return PanButton.LEFT;
	}

	@ConfigItem(
		keyName = "showPluginMarkers",
		name = "Other Plugin Markers",
		description = "Displays markers registered by other RuneLite plugins (e.g. Quest Helper, Clue Scrolls)",
		position = 1,
		section = iconsSection
	)
	default boolean showPluginMarkers()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showPlayerMarker",
		name = "Show Player Location",
		description = "Draws an orientation arrow at your character's current location",
		position = 2,
		section = iconsSection
	)
	default boolean showPlayerMarker()
	{
		return true;
	}

	@ConfigItem(
		keyName = "iconBanks",
		name = "Banks",
		description = "Bank icons",
		position = 3,
		section = iconsSection
	)
	default boolean iconBanks()
	{
		return false;
	}

	@ConfigItem(
		keyName = "iconShops",
		name = "Shops and Trade",
		description = "Shops, traders, the Grand Exchange and price checkers",
		position = 4,
		section = iconsSection
	)
	default boolean iconShops()
	{
		return false;
	}

	@ConfigItem(
		keyName = "iconSkilling",
		name = "Skilling",
		description = "Agility courses, agility shortcuts, hunter areas, fishing spots, mines, trees, ranges, anvils, and farming patches",
		position = 5,
		section = iconsSection
	)
	default boolean iconSkilling()
	{
		return true;
	}

	@ConfigItem(
		keyName = "iconTravel",
		name = "Travel",
		description = "Transport, dungeon entrances, agility shortcuts and house portals",
		position = 6,
		section = iconsSection
	)
	default boolean iconTravel()
	{
		return false;
	}

	@ConfigItem(
		keyName = "iconQuests",
		name = "Quests and Activities",
		description = "Quest starts, tasks and minigames",
		position = 7,
		section = iconsSection
	)
	default boolean iconQuests()
	{
		return false;
	}

	@ConfigItem(
		keyName = "iconAltars",
		name = "Altars",
		description = "Altars",
		position = 8,
		section = iconsSection
	)
	default boolean iconAltars()
	{
		return false;
	}

	@ConfigItem(
		keyName = "iconDungeons",
		name = "Dungeons",
		description = "Dungeon markers and slayer masters",
		position = 9,
		section = iconsSection
	)
	default boolean iconDungeons()
	{
		return false;
	}

	@ConfigItem(
		keyName = "iconServices",
		name = "Tutors and Services",
		description = "Tutors, guides, poll booths and cosmetic services",
		position = 10,
		section = iconsSection
	)
	default boolean iconServices()
	{
		return false;
	}

	@ConfigItem(
		keyName = "iconOther",
		name = "Other map icons",
		description = "New game map icons whose labels are not yet available",
		position = 11,
		section = iconsSection
	)
	default boolean iconOther()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showPlaceNames",
		name = "Place names",
		description = "Draws region labels. Cities stay zoomed out; smaller areas appear as you zoom in. Dungeon rooms only show inside the dungeon or on hover",
		position = 13,
		section = iconsSection
	)
	default boolean showPlaceNames()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showBossLocations",
		name = "Show Boss Markers",
		description = "Marks major bosses across Gielinor with distinct icons and strategies",
		position = 11,
		section = iconsSection
	)
	default boolean showBossLocations()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showMonsterZones",
		name = "Show Monster & Slayer Zones",
		description = "Draws spawn area overlays and icons for all indexed monsters and slayer targets",
		position = 12,
		section = iconsSection
	)
	default boolean showMonsterZones()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showMonsterIcons",
		name = "Monster Icons",
		description = "Displays authentic OSRS creature, boss, and slayer equipment icons on monster markers",
		position = 14,
		section = iconsSection
	)
	default boolean showMonsterIcons()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showMonsterLabels",
		name = "Monster Name Badges",
		description = "Shows monster names and combat levels below markers when zoomed in",
		position = 15,
		section = iconsSection
	)
	default boolean showMonsterLabels()
	{
		return true;
	}

	@ConfigItem(
		keyName = "monsterZoneMinZoom",
		name = "Monster Spawn Min Zoom",
		description = "Minimum zoom (px/tile) before standard monster spawn areas appear. Higher = only when zoomed closer. Active Slayer task zones ignore this",
		position = 16,
		section = iconsSection
	)
	@Range(min = 1, max = 24)
	default int monsterZoneMinZoom()
	{
		return 7;
	}

	@ConfigItem(
		keyName = "highlightSlayerTask",
		name = "Highlight Slayer Task",
		description = "Specially highlights spawn areas for monsters matching your active Slayer assignment and keeps them visible at all zoom levels",
		position = 17,
		section = iconsSection
	)
	default boolean highlightSlayerTask()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showBoatLocations",
		name = "Show Player Boats",
		description = "Displays markers on the map showing where your boats are currently docked",
		position = 18,
		section = iconsSection
	)
	default boolean showBoatLocations()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showBoatNames",
		name = "Show Boat Names",
		description = "Draws name badges next to boat markers when zoomed in",
		position = 19,
		section = iconsSection
	)
	default boolean showBoatNames()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showSailingPorts",
		name = "Show Sailing Ports",
		description = "Displays docking pins and details for Sailing ports across Gielinor",
		position = 20,
		section = iconsSection
	)
	default boolean showSailingPorts()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showPortNoticeBoards",
		name = "Show Port Notice Boards",
		description = "Displays port notice boards and typical task previews across major Sailing ports",
		position = 21,
		section = iconsSection
	)
	default boolean showPortNoticeBoards()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showGroundItems",
		name = "Show Ground Item Spawns",
		description = "Draws wiki ground item spawn tiles on the map once you zoom in close",
		position = 22,
		section = iconsSection
	)
	default boolean showGroundItems()
	{
		return true;
	}

	@ConfigItem(
		keyName = "groundItemMinZoom",
		name = "Ground Item Min Zoom",
		description = "Minimum zoom (px/tile) before ground item spawns appear. Higher = only when zoomed closer",
		position = 23,
		section = iconsSection
	)
	@Range(min = 1, max = 24)
	default int groundItemMinZoom()
	{
		return 9;
	}

	@ConfigItem(
		keyName = "groundItemMinValue",
		name = "Ground Item Min Value",
		description = "Hides ground item spawns worth less than this in gp (GE price, or high alch for untradeables). 0 shows everything, buckets and cabbages included",
		position = 24,
		section = iconsSection
	)
	@Range(min = 0, max = 100000)
	default int groundItemMinValue()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "showGroundItemLabels",
		name = "Ground Item Name Badges",
		description = "Shows item names below ground item markers when zoomed in far",
		position = 25,
		section = iconsSection
	)
	default boolean showGroundItemLabels()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showClueScroll",
		name = "Show Clue Scroll Target",
		description = "Marks the solved location of your active clue scroll step on the map, using RuneLite's clue scroll plugin",
		position = 26,
		section = iconsSection
	)
	default boolean showClueScroll()
	{
		return true;
	}

	@ConfigItem(
		keyName = "debugOverlay",
		name = "Debug Panel",
		description = "Draws a live panel on the map showing camera, tile and marker state",
		position = 1,
		section = debugSection
	)
	default boolean debugOverlay()
	{
		return false;
	}

	@ConfigItem(
		keyName = "debugLogging",
		name = "Debug Logging",
		description = "Writes a one-line pipeline summary to the client log about once a second while the map is open",
		position = 2,
		section = debugSection
	)
	default boolean debugLogging()
	{
		return false;
	}
}
