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
package atlasofgielinor;

import atlasofgielinor.ui.input.PanButton;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup("bettermap")
public interface AtlasOfGielinorConfig extends Config
{
	String MINIMUM_ZOOM_DESCRIPTION = " Minimum zoom in px/tile; lower shows farther out, 0 shows at every zoom.";

	@ConfigSection(
		name = "Map",
		description = "Map window and access buttons.",
		position = 0
	)
	String mapSection = "map";

	@ConfigSection(
		name = "Map: Advanced",
		description = "Dungeon appearance, coordinates and native map rendering.",
		position = 1,
		closedByDefault = true
	)
	String mapAdvancedSection = "mapAdvanced";

	@ConfigSection(
		name = "Controls",
		description = "Mouse buttons, scrolling and camera behavior.",
		position = 10
	)
	String controlsSection = "controls";

	@ConfigSection(
		name = "Controls: Advanced",
		description = "Initial zoom and disabling drag panning.",
		position = 11,
		closedByDefault = true
	)
	String controlsAdvancedSection = "controlsAdvanced";

	@ConfigSection(
		name = "Search & tooltips",
		description = "Search locations and choose how hover details appear.",
		position = 20
	)
	String finderSection = "finder";

	@ConfigSection(
		name = "Map layers",
		description = "Choose which locations and markers appear on the map.",
		position = 30
	)
	String iconsSection = "icons";

	@ConfigSection(
		name = "Map layers: Advanced",
		description = "Marker labels, zoom thresholds and travel presentation.",
		position = 31,
		closedByDefault = true
	)
	String iconsAdvancedSection = "iconsAdvanced";

	@ConfigSection(
		name = "Map layers: Zoom",
		description = "Minimum zoom in pixels per tile. Lower values show locations farther out; 0 shows them at every zoom. Defaults match the original map behavior.",
		position = 32,
		closedByDefault = true
	)
	String iconsZoomSection = "iconsZoom";

	@ConfigSection(
		name = "Map layers: Label zoom",
		description = "Minimum zoom in pixels per tile for marker names, codes and levels. The marker must also be visible.",
		position = 33,
		closedByDefault = true
	)
	String iconsLabelZoomSection = "iconsLabelZoom";

	@ConfigSection(
		name = "Shortest Path integration",
		description = "Draw routes calculated by the separately installed Shortest Path plugin.",
		position = 40
	)
	String pathfindingSection = "pathfinding";

	@ConfigSection(
		name = "Developer",
		description = "Dungeon tuning and diagnostics.",
		position = 90,
		closedByDefault = true
	)
	String developerSection = "developer";

	@ConfigItem(
		keyName = "enableShortestPath",
		name = "Show Shortest Path routes",
		description = "Requires Shortest Path installed and enabled from the Plugin Hub. "
			+ "Shortest Path controls all route calculations and transport settings; Atlas draws the route on its map.",
		position = 0,
		section = pathfindingSection
	)
	default boolean enableShortestPath()
	{
		return true;
	}

	@ConfigItem(
		keyName = "downloadMapAssets",
		name = "Download map assets",
		description = "Downloads and installs the map pack included with this plugin release from GitHub. Toggle off and on to retry a failed download.",
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
		name = "Hide native map rendering",
		description = "Stops the game drawing its map underneath Atlas of Gielinor. Turn off to troubleshoot rendering problems.",
		position = 0,
		section = mapAdvancedSection
	)
	default boolean hideGameMapRender()
	{
		return true;
	}

	@ConfigItem(
		keyName = "fullscreenMap",
		name = "Fullscreen Map",
		description = "Draws the map over the whole client instead of inside the game's small map window. Use the game's map close button to close it",
		position = 1,
		section = mapSection
	)
	default boolean fullscreenMap()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showCoordinateOverlay",
		name = "Show coordinates and zoom",
		description = "Adds cursor coordinates and zoom to the map status line.",
		position = 1,
		section = mapAdvancedSection
	)
	default boolean showCoordinateOverlay()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showPlaneSwitcher",
		name = "Show layer buttons",
		description = "Shows the Upper, Main and Lower layer buttons on the right side of the map.",
		position = 2,
		section = mapAdvancedSection
	)
	default boolean showPlaneSwitcher()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showTooltips",
		name = "Show hover details",
		description = "Shows names and details for map markers under the cursor.",
		position = 3,
		section = finderSection
	)
	default boolean showTooltips()
	{
		return true;
	}

	@ConfigItem(
		keyName = "expandedTooltips",
		name = "Always show full details",
		description = "Shows full tooltip details without holding the expansion modifier.",
		position = 4,
		section = finderSection
	)
	default boolean expandedTooltips()
	{
		return false;
	}

	@ConfigItem(
		keyName = "tooltipExpandModifier",
		name = "Hold for full details",
		description = "Hold this key to expand a compact tooltip. Only needed when full details are not always shown.",
		position = 5,
		section = finderSection
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
		description = "Draws dungeon entrance toggles that reveal their connected zones",
		position = 4,
		section = iconsSection
	)
	default boolean showLargeUndergroundSymbols()
	{
		return true;
	}

	@ConfigItem(
		keyName = "undergroundHoverPreview",
		name = "Preview dungeons on hover",
		description = "Hover dungeon or layer symbols to preview their map before opening them.",
		position = 4,
		section = mapSection
	)
	default boolean undergroundHoverPreview()
	{
		return true;
	}

	@ConfigItem(
		keyName = "undergroundOverlayOpacity",
		name = "Preview background opacity",
		description = "Visibility of the surface behind a dungeon hover preview, as a percentage.",
		position = 4,
		section = mapAdvancedSection
	)
	@Range(min = 10, max = 100)
	default int undergroundOverlayOpacity()
	{
		return 90;
	}

	@ConfigItem(
		keyName = "undergroundHoverBlur",
		name = "Preview background blur",
		description = "Blur of the surface behind a dungeon hover preview. 0 disables blur.",
		position = 5,
		section = mapAdvancedSection
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
		position = 7,
		section = iconsAdvancedSection
	)
	@Range(min = 8, max = 32)
	default int undergroundSymbolSize()
	{
		return 14;
	}

	@ConfigItem(
		keyName = "showTravelRoutes",
		name = "Travel routes",
		description = "Click a travel station to show its routes, destinations and prices.",
		position = 8,
		section = iconsAdvancedSection
	)
	default boolean showTravelRoutes()
	{
		return true;
	}

	@ConfigItem(
		keyName = "animateTravelRoutes",
		name = "Animate Travel Routes",
		description = "Animates route line dashes and pulsing destination beacons",
		position = 10,
		section = iconsAdvancedSection
	)
	default boolean animateTravelRoutes()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showTravelPriceTags",
		name = "Travel Price Badges",
		description = "Shows destination name and exact cost badges next to each route endpoint",
		position = 9,
		section = iconsAdvancedSection
	)
	default boolean showTravelPriceTags()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showFinderButton",
		name = "Map search button",
		description = "Shows search inside the world map.",
		position = 0,
		section = finderSection
	)
	default boolean showFinderButton()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showSidebarPanel",
		name = "Show Sidebar Panel",
		description = "Displays the Atlas of Gielinor panel in RuneLite's sidebar",
		position = 3,
		section = mapSection
	)
	default boolean showSidebarPanel()
	{
		return true;
	}

	@ConfigItem(
		keyName = "finderSearchItems",
		name = "Include shops and item spawns",
		description = "Includes shops stocking an item and ground item spawns in search results.",
		position = 1,
		section = finderSection
	)
	default boolean finderSearchItems()
	{
		return true;
	}

	@ConfigItem(
		keyName = "finderEchoChat",
		name = "Print search results in chat",
		description = "Prints the three nearest search results and their distances to game chat while searching.",
		position = 2,
		section = finderSection
	)
	default boolean finderEchoChat()
	{
		return false;
	}

	@ConfigItem(
		keyName = "maintainPlayerInView",
		name = "Finder Zoom: Keep player in view",
		description = "Frames the player and Finder destination together on the same plane. Otherwise uses the default zoom at the destination.",
		position = 3,
		section = finderSection
	)
	default boolean maintainPlayerInView()
	{
		return true;
	}

	@ConfigItem(
		keyName = "undergroundTransparentVoid",
		name = "Transparent dungeon background",
		description = "Makes black space around dungeon tiles transparent so the surface remains visible behind the dungeon.",
		position = 3,
		section = mapAdvancedSection
	)
	default boolean undergroundTransparentVoid()
	{
		return true;
	}

	@ConfigItem(
		keyName = "undergroundLayerSurfaceOpacity",
		name = "Open dungeon background opacity",
		description = "Visibility of the surface behind an open dungeon, as a percentage. 0 hides the surface.",
		position = 6,
		section = mapAdvancedSection
	)
	@Range(min = 0, max = 80)
	default int undergroundLayerSurfaceOpacity()
	{
		return 18;
	}

	@ConfigItem(
		keyName = "undergroundLayerSurfaceBlur",
		name = "Open dungeon background blur",
		description = "Blur of the surface behind an open dungeon. 0 disables blur.",
		position = 7,
		section = mapAdvancedSection
	)
	@Range(min = 0, max = 12)
	default int undergroundLayerSurfaceBlur()
	{
		return 9;
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
		position = 0,
		section = controlsAdvancedSection
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
		position = 2,
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
		position = 3,
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
		position = 1,
		section = controlsAdvancedSection
	)
	default boolean dragToPan()
	{
		return true;
	}

	@ConfigItem(
		keyName = "panButton",
		name = "Pan Button",
		description = "Which mouse button drag-pans the map. Map buttons (close, floors, dungeons) stay on left click",
		position = 0,
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
		position = 0,
		section = iconsSection
	)
	default boolean showPlayerMarker()
	{
		return true;
	}

	@ConfigItem(
		keyName = "shortcutMinZoom",
		name = "Agility Shortcuts",
		description = "Agility shortcut icons; controlled by their own category toggle." + MINIMUM_ZOOM_DESCRIPTION,
		position = 32,
		section = iconsZoomSection
	)
	@Units("px/tile")
	default double shortcutMinZoom()
	{
		return 0.20;
	}

	@ConfigItem(
		keyName = "slayerMasterMinZoom",
		name = "Skilling: Slayer Masters",
		description = "Slayer master icons under Skilling." + MINIMUM_ZOOM_DESCRIPTION,
		position = 33,
		section = iconsZoomSection
	)
	@Units("px/tile")
	default double slayerMasterMinZoom()
	{
		return 1.0;
	}

	@ConfigItem(
		keyName = "sailingLookoutMinZoom",
		name = "Sailing Lookouts",
		description = "Lookout point icons; ports, boats and task boards have separate settings." + MINIMUM_ZOOM_DESCRIPTION,
		position = 34,
		section = iconsZoomSection
	)
	@Units("px/tile")
	default double sailingLookoutMinZoom()
	{
		return 2.0;
	}

	@ConfigItem(
		keyName = "bankMinZoom",
		name = "Banks",
		description = "Bank icons." + MINIMUM_ZOOM_DESCRIPTION,
		position = 10,
		section = iconsZoomSection
	)
	@Units("px/tile")
	default double bankMinZoom()
	{
		return 1.0;
	}

	@ConfigItem(
		keyName = "shopMinZoom",
		name = "Shops and Trade",
		description = "Shop icons, traders and stock-indexed shops." + MINIMUM_ZOOM_DESCRIPTION,
		position = 11,
		section = iconsZoomSection
	)
	@Units("px/tile")
	default double shopMinZoom()
	{
		return 1.5;
	}

	@ConfigItem(
		keyName = "skillingMinZoom",
		name = "Skilling",
		description = "Skilling icons, including agility courses and hunter areas. Slayer masters have a separate threshold; agility shortcuts have their own category." + MINIMUM_ZOOM_DESCRIPTION,
		position = 12,
		section = iconsZoomSection
	)
	@Units("px/tile")
	default double skillingMinZoom()
	{
		return 0.85;
	}

	@ConfigItem(
		keyName = "travelMinZoom",
		name = "Travel",
		description = "Transport icons, house portals, mooring points and dungeon map links." + MINIMUM_ZOOM_DESCRIPTION,
		position = 13,
		section = iconsZoomSection
	)
	@Units("px/tile")
	default double travelMinZoom()
	{
		return 0.2;
	}

	@ConfigItem(
		keyName = "questMinZoom",
		name = "Quests and Tasks",
		description = "Quest starts, task masters and holiday events." + MINIMUM_ZOOM_DESCRIPTION,
		position = 14,
		section = iconsZoomSection
	)
	@Units("px/tile")
	default double questMinZoom()
	{
		return 0.45;
	}

	@ConfigItem(
		keyName = "activityMinZoom",
		name = "Minigames and Raids Lobbies",
		description = "Minigame and raids lobby icons under Quests and Activities." + MINIMUM_ZOOM_DESCRIPTION,
		position = 15,
		section = iconsZoomSection
	)
	@Units("px/tile")
	default double activityMinZoom()
	{
		return 0.6;
	}

	@ConfigItem(
		keyName = "altarMinZoom",
		name = "Altars",
		description = "Altar icons." + MINIMUM_ZOOM_DESCRIPTION,
		position = 16,
		section = iconsZoomSection
	)
	@Units("px/tile")
	default double altarMinZoom()
	{
		return 1.2;
	}

	@ConfigItem(
		keyName = "dungeonMinZoom",
		name = "Dungeons",
		description = "Dungeon and basement icons. Dungeon navigation buttons have a separate threshold." + MINIMUM_ZOOM_DESCRIPTION,
		position = 17,
		section = iconsZoomSection
	)
	@Units("px/tile")
	default double dungeonMinZoom()
	{
		return 1.0;
	}

	@ConfigItem(
		keyName = "serviceMinZoom",
		name = "Tutors and Services",
		description = "Tutor, guide, cosmetic service, cargo bay and noticeboard icons." + MINIMUM_ZOOM_DESCRIPTION,
		position = 18,
		section = iconsZoomSection
	)
	@Units("px/tile")
	default double serviceMinZoom()
	{
		return 2.0;
	}

	@ConfigItem(
		keyName = "otherMinZoom",
		name = "Other Map Icons",
		description = "Unidentified native map icons." + MINIMUM_ZOOM_DESCRIPTION,
		position = 19,
		section = iconsZoomSection
	)
	@Units("px/tile")
	default double otherMinZoom()
	{
		return 2.0;
	}

	@ConfigItem(
		keyName = "majorPlaceMinZoom",
		name = "Cities, Kingdoms and Islands",
		description = "Major place names." + MINIMUM_ZOOM_DESCRIPTION,
		position = 20,
		section = iconsZoomSection
	)
	@Units("px/tile")
	default double majorPlaceMinZoom()
	{
		return 0.45;
	}

	@ConfigItem(
		keyName = "localPlaceMinZoom",
		name = "Districts and Landmarks",
		description = "Smaller place names." + MINIMUM_ZOOM_DESCRIPTION,
		position = 21,
		section = iconsZoomSection
	)
	@Units("px/tile")
	default double localPlaceMinZoom()
	{
		return 3.0;
	}

	@ConfigItem(
		keyName = "interiorPlaceMinZoom",
		name = "Dungeon Room Names",
		description = "Interior place names; rooms still require the dungeon to be visible or hovered." + MINIMUM_ZOOM_DESCRIPTION,
		position = 22,
		section = iconsZoomSection
	)
	@Units("px/tile")
	default double interiorPlaceMinZoom()
	{
		return 4.0;
	}

	@ConfigItem(
		keyName = "bossMinZoom",
		name = "Bosses and Raid Entrances",
		description = "Boss pins and raid entrance buttons. Highlighted Slayer task bosses still ignore this threshold." + MINIMUM_ZOOM_DESCRIPTION,
		position = 23,
		section = iconsZoomSection
	)
	@Units("px/tile")
	default double bossMinZoom()
	{
		return 0.75;
	}

	@ConfigItem(
		keyName = "slayerTaskIconMinZoom",
		name = "Slayer Task Icons",
		description = "Highlighted Slayer task monster icons. Task area overlays remain visible at every zoom." + MINIMUM_ZOOM_DESCRIPTION,
		position = 24,
		section = iconsZoomSection
	)
	@Units("px/tile")
	default double slayerTaskIconMinZoom()
	{
		return 0.3;
	}

	@ConfigItem(
		keyName = "sailingPortMinZoom",
		name = "Sailing Ports",
		description = "Sailing port pins." + MINIMUM_ZOOM_DESCRIPTION,
		position = 25,
		section = iconsZoomSection
	)
	@Units("px/tile")
	default double sailingPortMinZoom()
	{
		return 0.0;
	}

	@ConfigItem(
		keyName = "portNoticeBoardMinZoom",
		name = "Port Notice Boards",
		description = "Dedicated Sailing task-board markers. Native noticeboard icons follow Tutors and Services." + MINIMUM_ZOOM_DESCRIPTION,
		position = 26,
		section = iconsZoomSection
	)
	@Units("px/tile")
	default double portNoticeBoardMinZoom()
	{
		return 0.8;
	}

	@ConfigItem(
		keyName = "playerBoatMinZoom",
		name = "Player Boats",
		description = "Docked player boat markers." + MINIMUM_ZOOM_DESCRIPTION,
		position = 27,
		section = iconsZoomSection
	)
	@Units("px/tile")
	default double playerBoatMinZoom()
	{
		return 0.0;
	}

	@ConfigItem(
		keyName = "pluginMarkerMinZoom",
		name = "Other Plugin Markers",
		description = "Markers supplied by other RuneLite plugins." + MINIMUM_ZOOM_DESCRIPTION,
		position = 29,
		section = iconsZoomSection
	)
	@Units("px/tile")
	default double pluginMarkerMinZoom()
	{
		return 0.0;
	}

	@ConfigItem(
		keyName = "dungeonNavigationMinZoom",
		name = "Dungeon Navigation Buttons",
		description = "Dungeon entrance toggles, connected previews and floor navigation buttons." + MINIMUM_ZOOM_DESCRIPTION,
		position = 30,
		section = iconsZoomSection
	)
	@Units("px/tile")
	default double dungeonNavigationMinZoom()
	{
		return 0.0;
	}

	@ConfigItem(
		keyName = "travelStationMinZoom",
		name = "Travel Network Stations",
		description = "Travel network pins and route previews, separate from native Travel icons." + MINIMUM_ZOOM_DESCRIPTION,
		position = 31,
		section = iconsZoomSection
	)
	@Units("px/tile")
	default double travelStationMinZoom()
	{
		return 0.0;
	}

	@ConfigItem(
		keyName = "fairyRingLabelMinZoom",
		name = "Fairy Ring Codes",
		description = "Fairy ring code labels." + MINIMUM_ZOOM_DESCRIPTION,
		position = 10,
		section = iconsLabelZoomSection
	)
	@Units("px/tile")
	default double fairyRingLabelMinZoom()
	{
		return 1.0;
	}

	@ConfigItem(
		keyName = "shortcutLabelMinZoom",
		name = "Agility Shortcut Levels",
		description = "Agility shortcut level labels." + MINIMUM_ZOOM_DESCRIPTION,
		position = 11,
		section = iconsLabelZoomSection
	)
	@Units("px/tile")
	default double shortcutLabelMinZoom()
	{
		return 1.0;
	}

	@ConfigItem(
		keyName = "bossLabelMinZoom",
		name = "Boss Names",
		description = "Boss name badges." + MINIMUM_ZOOM_DESCRIPTION,
		position = 12,
		section = iconsLabelZoomSection
	)
	@Units("px/tile")
	default double bossLabelMinZoom()
	{
		return 0.75;
	}

	@ConfigItem(
		keyName = "monsterLabelMinZoom",
		name = "Monster Names",
		description = "Standard monster name badges." + MINIMUM_ZOOM_DESCRIPTION,
		position = 13,
		section = iconsLabelZoomSection
	)
	@Units("px/tile")
	default double monsterLabelMinZoom()
	{
		return 0.85;
	}

	@ConfigItem(
		keyName = "slayerTaskLabelMinZoom",
		name = "Slayer Task Names",
		description = "Highlighted Slayer task names; these retain their independent visibility behavior." + MINIMUM_ZOOM_DESCRIPTION,
		position = 14,
		section = iconsLabelZoomSection
	)
	@Units("px/tile")
	default double slayerTaskLabelMinZoom()
	{
		return 0.65;
	}

	@ConfigItem(
		keyName = "groundItemLabelMinZoom",
		name = "Ground Item Names",
		description = "Ground item name badges." + MINIMUM_ZOOM_DESCRIPTION,
		position = 15,
		section = iconsLabelZoomSection
	)
	@Units("px/tile")
	default double groundItemLabelMinZoom()
	{
		return 14.0;
	}

	@ConfigItem(
		keyName = "boatLabelMinZoom",
		name = "Player Boat Names",
		description = "Docked player boat name badges." + MINIMUM_ZOOM_DESCRIPTION,
		position = 16,
		section = iconsLabelZoomSection
	)
	@Units("px/tile")
	default double boatLabelMinZoom()
	{
		return 0.65;
	}

	@ConfigItem(
		keyName = "dungeonNavigationLabelMinZoom",
		name = "Dungeon Navigation Names",
		description = "Dungeon button labels inside a dungeon. Hover still reveals names at any zoom." + MINIMUM_ZOOM_DESCRIPTION,
		position = 18,
		section = iconsLabelZoomSection
	)
	@Units("px/tile")
	default double dungeonNavigationLabelMinZoom()
	{
		return 1.5;
	}

	@ConfigItem(
		keyName = "iconShortcuts",
		name = "Agility Shortcuts",
		description = "Agility shortcuts, independent of Skilling and Travel.",
		position = 21,
		section = iconsSection
	)
	default boolean iconShortcuts()
	{
		return true;
	}

	@ConfigItem(
		keyName = "iconSailing",
		name = "Sailing Lookouts",
		description = "Sailing lookout point icons. Ports, boats and notice boards have separate toggles.",
		position = 22,
		section = iconsSection
	)
	default boolean iconSailing()
	{
		return false;
	}

	@ConfigItem(
		keyName = "iconBanks",
		name = "Banks",
		description = "Bank icons",
		position = 5,
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
		position = 6,
		section = iconsSection
	)
	default boolean iconShops()
	{
		return false;
	}

	@ConfigItem(
		keyName = "iconSkilling",
		name = "Skilling",
		description = "Agility courses, hunter areas, Slayer masters, fishing spots, mines, trees, ranges, anvils, and farming patches",
		position = 7,
		section = iconsSection
	)
	default boolean iconSkilling()
	{
		return true;
	}

	@ConfigItem(
		keyName = "iconTravel",
		name = "Travel",
		description = "Transport, dungeon map links, mooring points, sea currents and house portals",
		position = 8,
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
		position = 9,
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
		position = 10,
		section = iconsSection
	)
	default boolean iconAltars()
	{
		return false;
	}

	@ConfigItem(
		keyName = "iconDungeons",
		name = "Dungeons",
		description = "Dungeon and basement markers",
		position = 11,
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
		position = 12,
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
		position = 13,
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
		position = 3,
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
		position = 14,
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
		position = 15,
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
		position = 0,
		section = iconsAdvancedSection
	)
	default boolean showMonsterIcons()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showMonsterLabels",
		name = "Monster Name Badges",
		description = "Shows monster names and combat levels below markers when zoomed in",
		position = 1,
		section = iconsAdvancedSection
	)
	default boolean showMonsterLabels()
	{
		return true;
	}

	@ConfigItem(
		keyName = "monsterZoneMinZoom",
		name = "Monster Spawn Min Zoom",
		description = "Minimum zoom (px/tile) before standard monster spawn areas appear. Higher = only when zoomed closer. Active Slayer task zones ignore this",
		position = 0,
		section = iconsZoomSection
	)
	@Units("px/tile")
	@Range(min = 0, max = 24)
	default int monsterZoneMinZoom()
	{
		return 7;
	}

	@ConfigItem(
		keyName = "highlightSlayerTask",
		name = "Highlight Slayer Task",
		description = "Specially highlights spawn areas for monsters matching your active Slayer assignment and keeps them visible at all zoom levels",
		position = 16,
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
		position = 6,
		section = iconsAdvancedSection
	)
	default boolean showBoatNames()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showSailingPorts",
		name = "Show Sailing Ports",
		description = "Displays docking pins and details for Sailing ports across Gielinor",
		position = 19,
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
		position = 20,
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
		position = 17,
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
		position = 1,
		section = iconsZoomSection
	)
	@Units("px/tile")
	@Range(min = 0, max = 24)
	default int groundItemMinZoom()
	{
		return 9;
	}

	@ConfigItem(
		keyName = "groundItemMinValue",
		name = "Ground Item Min Value",
		description = "Hides ground item spawns worth less than this in gp (GE price, or high alch for untradeables). 0 shows everything, buckets and cabbages included",
		position = 5,
		section = iconsAdvancedSection
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
		position = 3,
		section = iconsAdvancedSection
	)
	default boolean showGroundItemLabels()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showClueScroll",
		name = "Show Clue Helper",
		description = "Shows Go to Clue and the native clue hint card. Clue markers use Other Plugin Markers.",
		position = 2,
		section = iconsSection
	)
	default boolean showClueScroll()
	{
		return true;
	}

	@ConfigItem(
		keyName = "debugLogging",
		name = "Debug Logging",
		description = "Writes a one-line pipeline summary to the client log about once a second while the map is open",
		position = 2,
		section = developerSection
	)
	default boolean debugLogging()
	{
		return false;
	}
}
