# Atlas of Gielinor content sources

The plugin software license does not grant rights to all bundled or downloaded content.
These notes identify recorded sources; they do not assert that every asset has been cleared
for redistribution. Atlas is not affiliated with or endorsed by Jagex, the OSRS Wiki,
RuneLite, or the upstream data projects.

## Wiki-derived datasets

Credit: OSRS Wiki contributors at https://oldschool.runescape.wiki/.

- `data/monsters.json.gz`: monster records retain `wiki_url` links to individual source
  articles. Records group spawn coordinates into named map zones and include reference details.
- `data/shops.json.gz` and `data/shop_data_complete.json`: their recorded source is
  OSRS Wiki Category:Shops (Infobox Shop): https://oldschool.runescape.wiki/w/Category:Shops.
  The complete dataset retains `wiki_title` source titles. Atlas assigns locations and
  selects fields for map tooltips.
- `data/ground_items.json.gz`: its recorded sources are OSRS Wiki ItemSpawnLine templates,
  osrsreboxed-db, and real-time prices. Atlas groups items by spawn tile. The source metadata
  does not record exact upstream revisions or the price provider.
- Curated POI/quest descriptions and legacy map inputs also require their original import
  records to establish article/file sources and applicable terms.

Applicable Wiki terms and copyright notices are published by its operator:
https://weirdgloop.org/terms/ and https://meta.weirdgloop.org/w/Meta:Copyrights.
The current copyright page was unavailable during this review. Original revision identifiers,
field-level source attribution, and media-specific terms remain to be reconstructed; a
dataset's generation timestamp is not an upstream revision.

## osrsreboxed-db

The ground-item dataset names osrsreboxed-db as a source:
https://github.com/0xNeffarion/osrsreboxed-db.
Its upstream README distinguishes GPL-3.0 project content, Jagex game content, and
Wiki-derived metadata that it identifies as CC BY-NC-SA 3.0:
https://github.com/0xNeffarion/osrsreboxed-db#project-license.
The exact imported revision and fields are not recorded locally, so these are upstream
statements rather than a blanket license assignment for Atlas's mixed dataset.

## Imagery and cache-derived map data

The current map pack comes from https://github.com/n-oost/better-map-assets.
The publisher's provenance notice identifies underlying game content as Jagex's and
records incomplete source information for some map IDs:
https://github.com/n-oost/better-map-assets/blob/main/ASSET-NOTICES.md.
Its pack metadata identifies the collection as needing further provenance review.
The maintainer identifies the likely original overworld image as the OSRS Wiki's
Old School RuneScape world map:
https://oldschool.runescape.wiki/images/Old_School_RuneScape_world_map.png?46763
Original file page and media attribution/history:
https://oldschool.runescape.wiki/w/File:Old_School_RuneScape_world_map.png
The maintainer supplied the file-page notice on 2026-10-04: it classifies this image as
non-free Jagex content displayed by the Wiki under fair use. Its recorded original source is:
https://www.runescape.com/oldschool/world-map
That address currently redirects to https://oldschool.runescape.com/world-map.
Image copyright: Jagex Games Ltd. Credit to OSRS Wiki contributors for hosting and documenting
the source. This image is not covered by the plugin's BSD software license or a blanket
Creative Commons license for Wiki text. The Wiki's fair-use notice describes its use;
it does not grant a redistribution license for Atlas's tile pack.
This is a maintainer-reported source; the image has not been compared with the distributed
tiles. Historical plugin comments also describe cache-rendered tiles. Underground images
and the collection's per-map source assignments remain unverified.

Bundled POI icons and cavern tiles need individual source records. Native POI coordinates
identify their cache source in `poi/pois.tsv`; cache-derived labels record their source in
`poi/pois-cache.tsv`. The project's BSD software license is not a license for Jagex artwork
or third-party map imagery. No redistribution permission is inferred solely from public hosting.

## Existing upstream notices

Shortest Path's code/data notice is preserved in `pathfinding/LICENSE` with its pinned
revision in `pathfinding/UPSTREAM_VERSION`.
RuneLite world-map metadata preserves original notices in the `copyrightAndLicences`
section of `poi/runelite-world-map-data.json`, alongside its `sourceRevision`.

See `THIRD-PARTY-NOTICES.md` and `docs/asset-provenance.md` in the source repository for
the source inventory and remaining evidence gaps.
