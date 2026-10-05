# Third-party notices

## Shortest Path route engine and data

Atlas of Gielinor includes a package-relocated copy of the Shortest Path pathfinding engine and its
collision, transport, destination, and league data from
[Skretzo/shortest-path](https://github.com/Skretzo/shortest-path).

- Upstream revision: `6ca996a41a6a4b85d0fdb38dc6d56c66b747e29a` (2026-09-02)
- License: BSD 2-Clause
- Copyright: (c) 2021-2026 Skretzo and Shortest Path contributors
- Java sources: `src/main/java/com/bettermap/pathfinding/`
- Data and full license text: `src/main/resources/com/bettermap/pathfinding/`

The copy is pinned; installed Shortest Path plugin updates do not replace it. In external-settings mode, Atlas of Gielinor sends
destinations to an enabled Shortest Path plugin through its public plugin-message API and reads
its saved routing settings. Atlas of Gielinor routing mode uses only its own settings. In either mode,
Atlas sends selected destinations to Shortest Path when that plugin is enabled. The two route
calculations remain independent. Atlas of Gielinor is not
affiliated with or endorsed by the Shortest Path project.

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
