<div align="center">

![Atlas of Gielinor](docs/banner.png)

> **Atlas of Gielinor**  
> *Examine: An cartographer's parchment charting the surface and underground realms of Gielinor.*

[![RuneLite Plugin Hub](https://img.shields.io/badge/RuneLite-Plugin_Hub-1B263B?style=flat-square&color=3E3024)](https://runelite.net)
[![OSRS Version](https://img.shields.io/badge/OSRS-Desktop-FED836?style=flat-square&color=2C2219&labelColor=4A3525)](https://oldschool.runescape.wiki)
[![License: BSD-2-Clause](https://img.shields.io/badge/License-BSD_2--Clause-938265?style=flat-square&color=241D17)](LICENSE)

</div>

A RuneLite plugin that replaces the in-game world map. Dungeon locations, clippings, and other elements are hand placed and edited. Otherwise data is pulled from the OSRS Wiki, with some manual curation. The plugin provides a world map replacement, an in-map Finder, and optional Shortest Path integration. Enable **Download map assets** in Atlas of Gielinor settings to trigger download [Atlas of Gielinor asset pack](https://raw.githubusercontent.com/n-oost/better-map-assets/49644b64a4769c826665a22a11a8edcbcc5d03d9/packs/a7d1a28d2de93e914508b0e5cbb56776938342f77a7bb34a3c610881aea2a528.zip).

Missing or invalid assets display `NO MAP DATA` with a loading or error message. Updates are checked at startup while downloads are enabled. Toggle **Download map assets** off and on to retry map download on fail.

## AI-Disclosure
Plugin was created with the help of AI tools. The author has verified the code and is responsible for its content. Map imagery is downloaded from the linked asset pack; there are no runtime calls to the Wiki API.

## Features

### Settings

Common settings are grouped under **Map**, **Controls**, **Search & tooltips**, **Map layers**, and
**Shortest Path integration**. Each **Advanced** section starts collapsed and contains appearance
or marker detail controls. Dungeon tuning and diagnostics share one collapsed
**Developer** section. Turn the plugin off to restore RuneLite's normal world map.

### Surface and underground on one map
Hover a dungeon entrance to preview its connected zones. Click to open the group; click any open entrance in that group to close it. Floor controls still select individual dungeon floors. Connections are defined in the bundled dungeon dataset.

<!-- ![](docs/gifs/surface-underground.gif) -->

### Zoom from the whole world to one tile

Atlas of Gielinor owns the camera: 0.14 to 24 pixels per game tile. Drag to pan, scroll to zoom about
the cursor, right click to step back out.

![Zoom and pan](docs/gifs/zoom-and-pan.gif)

### Closing the map

Click the game's original map close button. It remains visible and clickable in fullscreen
and while map assets are unavailable. Atlas does not provide a replacement close chip or
close the map after a Finder selection. Escape follows the client's native behavior;
when a boss gallery is open, Escape dismisses that gallery first.

### Raid boss galleries

Click **CoX · Bosses**, **ToB · Bosses**, or **ToA · Bosses** at the respective raid entrance to open its six boss cards. Hover a card for its boss tooltip; hold your configured tooltip expansion modifier for available combat details. Press **Esc** or click **×** to close the gallery. Enable **Show boss locations** and **Show tooltips** in Atlas of Gielinor settings.

### Hover cards

Hover anything on the map for shop stock, POI names, monster details (combat level, slayer level,
slayer masters, weakness strategies, key drops), travel directories, coordinates and markers.

| Shop stock | POI details |
| :---: | :---: |
| ![Shop stock](docs/gifs/karamja-store.png) | ![POI details](docs/gifs/yew-tree.png) |
| **Notice board** | **Monster details** |
| ![Notice board](docs/gifs/port-board.png) | ![Monster details](docs/gifs/gnome-man.png) |

### Dungeon entrances

Hover dungeon entrances to view the dungeon map, location details, and available actions directly
from the map.

![Dungeon entrances](docs/gifs/hover-dungeons.gif)

### Find

Search locations, monsters, shops and ground-item spawns by name from the in-map Find card. Click a result or press Enter to center the map on it. Route sends the destination to the separately installed Shortest Path plugin; Atlas displays returned route coordinates on its map.

![Find](docs/gifs/ground-item-search.gif)

### Walking routes

Install and enable **Shortest Path** separately from RuneLite's Plugin Hub. Enable **Show
Shortest Path routes** in Atlas's **Shortest Path integration** section. Atlas contains no
pathfinding engine or collision map: Shortest Path owns the calculations and transport settings.

Selecting **Route** sends the destination to Shortest Path. Atlas uses its public plugin-message
API to follow the active destination and request route coordinates for Atlas's custom map.
These coordinate queries are separate searches in Shortest Path, so Atlas refreshes can lag its
in-game route. Atlas does not draw a minimap route; Shortest Path controls its own overlays.
If Shortest Path is disabled, Atlas clears its displayed route. A plugin version without the
`getTarget`/`query` API cannot supply coordinates to Atlas.

![Walking routes](docs/gifs/click-map-path-route.gif)

### Monster zones

805 monsters across 2,310 location zones, each drawn as one marker at the centroid of its spawn
tiles, with the spawn area shaded behind it.

![Monster zones](docs/gifs/gnome-man.png)

### Ground item spawns

370 items over 4,361 exact world tiles, drawn as their real item sprites once you zoom past
`groundItemMinZoom` (default 9 px/tile) and filtered by the `groundItemMinValue` gp slider
(default 0, so everything shows).

<!-- ![](docs/gifs/ground-items.gif) -->

### Travel networks

Charter ships and other transport hubs, with route arcs between them and a directory on the
hover card.

![Travel networks](docs/gifs/travel-routes-gif.gif)

### Markers from other plugins

Markers registered by other RuneLite plugins ( clue scrolls, party
members,quest helper) are drawn on Atlas of Gielinor's camera, along with the player orientation arrow.

Go to Clue centres the map on RuneLite's active clue location (the nearest candidate when there are several). The fullscreen clue card displays RuneLite's own hint content. Better Map does not add clue markers, labels or requirement summaries; RuneLite's registered clue markers use the shared plugin marker display.

<!-- ![](docs/gifs/plugin-markers.gif) -->

### Plugin Hub notes

- No reflection, no AWT-level input hooks, and no native code.
- With **Download map assets** enabled, the plugin downloads the pack pinned in `src/main/resources/com/bettermap/data/map-pack.json` using asynchronous OkHttp when that pack is not already installed. Asset updates require a plugin release; no remote channel is checked. Installed files use RuneLite Filepath in the plugin directory.
- The plugin replaces the world map and
  draws Shortest Path route coordinates on its custom world map. The map's Chat control can hide the chatbox while
  the map is open; Finder text input keeps the chatbox visible.
- The tile cache is budgeted in megabytes, not tiles: ~49 MB of LRU plus ~30 MB of permanently held
  coarse levels.
- The Finder takes typed search text through RuneLite's `ChatboxPanelManager`; the on-map
  search field mirrors that text.

## Datasets
-Data is bundled in `resources/com/bettermap/data`.
- **Monster locations**: 805 monsters across 2,310 location zones in `monsters.json.gz`.
- **Shops**: ~450 shops from OSRS Wiki `Category:Shops` in `shops.json.gz` (owner, special stock,
  notable items, services).
- **Ground item spawns**: 370 unique items over 4,361 exact world tiles. Runtime dataset
  `ground_items.json.gz` (gzip-compressed; loaders decompress on read); full scrape in
  `docs/data/ground_item_spawns.tsv` and `docs/data/ground_item_data_complete.json`. 
- **Points of interest**: current cache world-map icons (`poi/pois.tsv`), older unmatched wiki markers (`poi/pois-legacy.tsv`), curated additions, and cache region labels
  (`poi/pois-cache.tsv`).
- **Travel networks**: charter ships and other transport hubs. Runtime dataset `travel_networks.json.gz` from manually curated wiki nodes and routes.

## Credits
- n-oost, truenosus
- **[RuneLite](https://runelite.net)** - plugin API and client.
- **[Skretzo and Shortest Path contributors](https://github.com/Skretzo/shortest-path)** -
  external integration and extracted teleport POI coordinates, BSD 2-Clause. See [third-party notices](THIRD-PARTY-NOTICES.md).

## License

BSD 2-Clause, see [`LICENSE`](LICENSE). Third-party teleport POI data is covered by [`THIRD-PARTY-NOTICES.md`](THIRD-PARTY-NOTICES.md). The software license does not
license all map imagery or datasets; see the [bundled asset notices](src/main/resources/com/bettermap/ASSET-NOTICES.md)
for recorded content sources and the [provenance inventory](docs/asset-provenance.md) for remaining gaps.

Finder place names, kingdom names, centres and bounds are bundled in `src/main/resources/com/bettermap/data/regions.json.gz`. They load on the startup worker before map input and rendering activate. `MapRegion` retains stable IDs and the existing smallest-area lookup rules.

POI hover cards use curated and RuneLite-supplied detail data. When no description is available, they show the marker type and coordinates with “No additional details available.” Mooring Sailing levels and optional named requirements come from `poi/mooring-levels.tsv`.
