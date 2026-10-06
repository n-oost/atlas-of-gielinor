# Java data extraction and shared helpers — 2026-10-05

## Review limit

The reviewer reported 470,067 tokens and requires fewer than 200,000. The local estimate uses tiktoken `cl100k_base` / `encode_ordinary` on all `src/main/java/**/*.java`, with Java comments removed while string and character literals are retained. It is an estimate, not the reviewbot count.

- Before the first extraction: 486,192 estimated tokens.
- Before this batch: 340,029 estimated tokens.
- Current working tree: approximately 295,800 estimated tokens (146 Java files).
- This batch removes approximately 44,200 tokens (13%). The 200k limit is still unmet.

## Bundled data

These resources must ship alongside the Java loaders:

| Resource under `src/main/resources/com/bettermap` | Records | Preserved behavior |
| --- | ---: | --- |
| `poi/curated-details.tsv` | 1,681 source rows | Original insertion order, duplicate handling, basement classification, coordinate overwrite precedence |
| `poi/quest-details.tsv` | 216 | Titles, categories and detail lines |
| `poi/travel-networks.json` | 195 stations, 97 destination groups | Shared destination lists, 3,824 station destination references, costs, requirements, coordinates and offsets |
| `dungeons/zones.tsv` | 381 | Enum order, aliases, anchors, descriptions, radius, clip bounds and alternate entrances |
| `dungeons/wiki-maps.tsv` | 221 | Enum order, map IDs, labels and bounds |
| `poi/dungeon-requirements.tsv` | 113 | Exact dungeon requirement text |
| `poi/mooring-levels.tsv` | 61 | Coordinates, levels, and original lookup order |

Data remains inside the JAR. No network dependency was introduced. MapData loads the catalogs in dependency order on the existing POI startup worker. Renderers wait for readiness; the native map stays visible during loading. Surface bounds remain immediately available for camera initialization. Catalogs publish after preparation, and cancellation avoids publishing incomplete catalogs.

## Shared code

- BundledTsv provides one ordered UTF-8 reader with schema checks and cancellation.
- Pure accessors use existing Lombok support where public method names can be preserved.
- Transport constructors share field-copy logic, retaining their original regionOverride differences.
- Boss marker drawing and tooltip hit testing share visibility and tuned display-position logic.
- Spatial indexes use MapChunkKey shared packing and queries (concurrent workspace changes).

## Validation status

Java compilation and resource processing succeed. Earlier intermediate full-suite runs passed 542 tests; the latest small changes and added catalog fingerprints have not had a final full-suite run. Dungeon and map metadata were compared against runtime snapshots of the original catalogs. Only the user can confirm behavior in-game.

Check immediate map opening after startup, dungeon entrances and alternate entrances, floor changes, Finder results, travel destinations and requirements, tuned boss marker/tooltip alignment, and native close/Escape behavior. Use the RuneLite wiki Using Jagex Accounts instructions for development-client login.

The new Java helpers and resource files are currently untracked and must be included with the source changes when committing.
