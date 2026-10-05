# Asset provenance inventory — 2026-10-04

The bundled notice at `src/main/resources/com/bettermap/ASSET-NOTICES.md` now credits
recorded source projects and points to original dataset/article metadata. This inventory
records evidence and missing information; it is not a redistribution clearance.

| Content | Recorded origin | What remains missing |
| --- | --- | --- |
| Downloaded map PNGs | better-map-assets channel; active asset commit `49644b64a4769c826665a22a11a8edcbcc5d03d9`; pack SHA-256 `a7d1a28d2de93e914508b0e5cbb56776938342f77a7bb34a3c610881aea2a528` | Original renderer/download inputs per map ID and applicable redistribution terms. Publisher explicitly records incomplete provenance. |
| Bundled POI icons, cavern tiles, and authored dungeon placements | Local resources; native POIs cite OSRS cache/OpenRS2 cache 2727 in pois.tsv; labels cite cache WorldMapManager in pois-cache.tsv | Per-image origins; identify original artwork versus edits to game/Wiki imagery. |
| Monster and shop datasets | Monster records retain wiki_url; shop source metadata names Wiki Infobox Shop and complete records retain wiki_title | Original article revisions and import records; distinguish copied prose from game text/factual fields. Generated 2026-08-31. |
| Ground-item dataset | source field names Wiki ItemSpawnLine, osrsreboxed-db, and real-time prices; generated 2026-09-02 | Upstream revisions, imported osrsreboxed fields, and exact price source. |
| Pinned code/data and curated descriptions | Shortest Path revision and BSD notice; RuneLite imported JSON revision and license headers; README identifies manually curated Wiki content | Check curated POI/quest text against original article sources. Preserve existing explicit upstream notices. |

## Primary source evidence

### Maintainer-reported overworld image

On 2026-10-04 the maintainer identified this as the likely original overworld image:
[Old School RuneScape world map PNG](https://oldschool.runescape.wiki/images/Old_School_RuneScape_world_map.png?46763).
Its [Wiki file page](https://oldschool.runescape.wiki/w/File:Old_School_RuneScape_world_map.png)
is the corresponding place to inspect attribution, media terms, and revision history.
This is a specific maintainer-reported source, replacing the earlier generic Wiki source lead.
Direct retrieval of the image and file page was unavailable during this review; a visual or
pixel comparison with the shipped tiles has not been performed. The query suffix is retained
exactly as supplied and is not treated as a confirmed file revision identifier.
This source describes the overworld candidate, not all underground imagery or bundled datasets.

The maintainer subsequently supplied the Wiki file-page notice: this is non-free Jagex
content displayed on the Wiki under fair use, originally obtained from
[Jagex's official world map](https://www.runescape.com/oldschool/world-map).
That source was independently retrieved and redirects to
[the current official world map page](https://oldschool.runescape.com/world-map), which
identifies Jagex copyright. The bundled notices now record this specific media classification
and original source. The file-page notice was supplied by the maintainer, not independently
retrieved. The remaining overworld gap is matching the image to the shipped tiles and
establishing the basis for Atlas's own redistribution; attribution does not itself grant that
permission. No conclusion that Atlas's use qualifies or fails to qualify as fair use is made.

The maintainer recalls that the tiles probably came from the Wiki. Record this as a source
lead, not confirmation for every tile. Local Git history provides another lead:
`feb4dbf:src/main/java/com/bettermap/tiles/WikiMapTiles.java` describes bundled imagery as
rendered from the user's local OSRS cache and names `tools/CacheTileDumper`. That tool is
not present at that path in the inspected commit. The current map UI already renders Jagex
imagery credit and OSRS Wiki dataset credit through MapChromeRenderer/WikiMapTiles.
These observations support investigating mixed sources; they do not tie individual PNGs to
a historical download or render. A Wiki-derived dungeon collection and cache-rendered
surface collection could both be consistent with this evidence.

The [asset publisher's notice](https://github.com/n-oost/better-map-assets/blob/main/ASSET-NOTICES.md)
states that the collection has incomplete provenance and still needs an attribution/redistribution
review. [Pack metadata](https://raw.githubusercontent.com/n-oost/better-map-assets/main/metadata/a7d1a28d2de93e914508b0e5cbb56776938342f77a7bb34a3c610881aea2a528.json)
also flags provenance review as pending. Neither a matching hash nor public hosting establishes
permission to redistribute.

The [osrsreboxed project license section](https://github.com/0xNeffarion/osrsreboxed-db#project-license)
distinguishes its GPL project content, Jagex game material, and Wiki metadata identified as
CC BY-NC-SA 3.0. Its exact imported revision is absent from the bundled ground-item metadata.
No conclusion about the entire plugin's required license is made from that source label alone.

[Weird Gloop's terms](https://weirdgloop.org/terms/) direct users to its copyright/licensing
policy. Direct retrieval of its current copyright policy was unavailable. An obsolete Fandom
policy is not used as evidence of the current Wiki's license.

## Next evidence needed

Locate the original tile-source manifest and dataset download/import scripts. Use those inputs
to pin source articles/files and revisions, document transformations, and retain the applicable
license/attribution text. Map IDs with no source record should remain marked unresolved rather
than assigned the plugin's BSD license. This inventory does not change any runtime download URL.
