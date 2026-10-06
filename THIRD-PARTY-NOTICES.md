# Third-party notices

## Shortest Path integration and teleport POI data

Atlas requires users to install [Shortest Path](https://github.com/Skretzo/shortest-path)
separately from the RuneLite Plugin Hub for walking routes. No Shortest Path Java engine,
collision map, transport requirements, or destination/league datasets are bundled.
Atlas communicates through RuneLite's public PluginMessage API and renders returned coordinates.
Atlas is not affiliated with or endorsed by the Shortest Path project.

The map-only `src/main/resources/com/bettermap/poi/teleport-locations.tsv` preserves teleport
labels and coordinates extracted from Shortest Path revision
`6ca996a41a6a4b85d0fdb38dc6d56c66b747e29a` (2026-09-02).
Its BSD 2-Clause license and copyright are preserved in
`src/main/resources/com/bettermap/poi/shortest-path-data-LICENSE`.

## RuneLite world-map metadata

The bundled `src/main/resources/com/bettermap/poi/runelite-world-map-data.json` contains
metadata imported from [RuneLite](https://github.com/runelite/runelite) at revision
`d8e7d1e5f34e2899eda3d7cf4cd9661ae2206f22`. Its `copyrightAndLicences` section preserves
the original copyright notices, redistribution conditions, and disclaimers for the source datasets.
Atlas adapts those records into its own points of interest and tooltips at runtime.

## Wiki datasets, imagery, and cache-derived content

Credit to OSRS Wiki contributors and the source projects recorded in the datasets.
Content origins and existing upstream notices are documented in the bundled
[asset notices](src/main/resources/com/bettermap/ASSET-NOTICES.md).
The [provenance inventory](docs/asset-provenance.md) identifies the remaining source/revision
gaps. The BSD license for this plugin's software does not license all game artwork,
Wiki-derived content, or mixed upstream datasets.
