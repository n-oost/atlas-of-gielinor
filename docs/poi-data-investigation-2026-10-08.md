# POI data investigation — 2026-10-08

**Historical investigation:** the [later review](poi-audit-review-2026-10-08.md) initially treated the island fox habitat as unresolved because area definitions are not exhaustive spawn evidence. The user subsequently explicitly confirmed that the island pyre fox should not be there, and the runtime identity/location was removed. The later [fix ledger](poi-audit-fixes-2026-10-08.json) records that user-confirmed removal and the other catalog corrections, including the six herbiboar/birdhouse conflicts identified through this investigation. The original text below preserves the investigation trail.

## Finding

The reported Isle of Souls pyre fox POI is a false, independently rendered and searchable record. Its location and description originated in a hardcoded curated data block. The catalog migration retained that record and also retained descriptions previously attached to other markers by proximity. This is a data quality issue affecting more than the screenshot.

The investigation changed no runtime Java or catalog records. It added a reproducible provenance inventory and this report. It inspected source, Git history, bundled tables, existing tests, upstream RuneLite source, and Jagex's island design blog. No client interaction or Gradle tests were performed.

## Exact source of the screenshot

| Field | Evidence |
| --- | --- |
| Canonical identity | `catalog/pois.tsv:5791`, ID `poi_005784` |
| Marker location | `catalog/locations.tsv:5791`, `(2250, 2900, 0)`, `hunter_training`, rendered and searchable |
| Inherited source | `poi/curated-details.tsv:836` |
| Title | `Isle of Souls - Pyre Foxes (Level 57)` |
| Detail lines | Pyre fox / Fox fur / Pyre logs; Hunter 57; deadfall; Isle of Souls Central |
| Status | Both identity and location are `unresolved` |

The screenshot's `(2249, 2898)` footer is the cursor's world coordinate. `MapTooltipRenderer.java:315` adds `worldX` and `worldY`, rather than the selected marker's coordinate. The two-tile difference does not explain the false island assignment.

The real RuneLite pyre fox map location is Avium Savannah, `(1616, 2999, 0)`. The project already contains this correct information: rendered `poi_000490` and supplementary `poi_007813`, as well as the bundled RuneLite snapshot. The false island marker is an additional record; it is not a displacement of that correct marker.

Upstream evidence: [RuneLite HunterAreaLocation at the bundled revision](https://github.com/runelite/runelite/blob/d8e7d1e5f34e2899eda3d7cf4cd9661ae2206f22/runelite-client/src/main/java/net/runelite/client/plugins/worldmap/HunterAreaLocation.java). Its island areas contain copper longtails, grey chinchompas and crimson swifts. [Jagex's Poll 74 blog](https://secure.runescape.com/m=news/poll-74-game-improvements-blog?oldschool=1) also lists those three creatures for the island. The blog is historical design evidence; the RuneLite revision is the direct comparison used here.

The `Pyre logs` yield is also unsupported. The [Wiki-derived pyre fox reference](https://danielpgleason.com/osrs/reference/pyre-fox-f38936b6/) lists fur, raw meat, bones and rumour fluff. This source is a secondary mirror; direct Wiki access was blocked by robots.txt during investigation.

## How the bad record reached the UI

1. Commit `feb4dbfd46c6ba177a8c6bd8faad7873a3b1c133`, September 25, 2026, added `src/main/java/com/bettermap/map/PoiDetails.java`. Line 1365 already contains the entire false fox entry, including coordinates and pyre logs. This is the earliest introduction found with `git log --all -S 'Isle of Souls - Pyre Foxes' -- .`. The commit imports a large codebase, so Git cannot establish when or how the text was written before that import.
2. Commit `aafd4e7`, October 5, removes the Java initializer during the data extraction work. Commit `35d2e43`, later October 5, records the same entry in `src/main/resources/com/bettermap/poi/curated-details.tsv`. The retained file explicitly says its row order preserves the original Java initializer.
3. Commit `f7cf1fb`, October 7, creates the canonical catalog from a pre-consolidation PoiIndex/PoiDetails snapshot. `catalog/migration-inputs.json` records the legacy input hashes and labels them retained snapshots. The false entry survives as `poi_005784`.
4. Today, `MapCatalog` reads bundled `catalog/pois.tsv` and `catalog/locations.tsv`. The `Location` constructor joins the identity's name, title, category and details into the location.
5. `PoiIndex.loadData()` creates rendered/searchable markers from those locations. It checks visibility but does not require `verified` status.

`PoiDetails.getDetail()` resolves a selected marker's location ID directly, then `PoiTooltipBuilder.buildPoiCard()` displays the stored title and lines. The current ID lookup prevents choosing another identity by proximity, but cannot correct false information already stored under that ID. `PoiDetails.load()` also indexes catalog locations for positional tooltip fallbacks.

Editing the legacy curated TSV alone will have no runtime effect. Corrections belong in the canonical catalog.

## Second failure: nearby descriptions contaminated real markers

Before the canonical migration, `PoiDetails.getNativeDetail()` used an exact coordinate/type lookup followed by the nearest same-type curated entry within 12 tiles. Dungeon handling allowed 16 tiles. The matching predicate checked marker category, not the creature, activity, named place or authoritative identity.

`PoiDetails.getDetail()` then kept the curated title and appended the imported RuneLite detail lines. It did not resolve disagreements between the two sources. `PoiIndex` also turned curated details into standalone markers when their coordinates were unoccupied.

The canonical catalog now contains the resulting mixed descriptions. For example, `poi_002111` combines a Mount Karuulm title and a bird/graahk target list with Lake Molch's four fish species. Its source points to curated line 824, and the authoritative RuneLite point is one tile away. This is strong evidence that the pre-migration assembled output preserved a proximity association. The migration generator is not present in this checkout, so its exact internal procedure cannot be established from a script.

## Additional concrete findings

| Record | Stored claim | Comparison evidence | Assessment |
| --- | --- | --- | --- |
| `poi_005783`, `(2220,2870,0)` | Isle of Souls **red** chinchompas, level 63 | RuneLite and Jagex identify **grey** chinchompas; RuneLite's marker is `(2127,2950,0)` | False species claim; stored position also unsupported by the snapshot |
| `poi_002111`, `(1364,3632,0)` | Mount Karuulm; ruby harvest, crimson swift, horned graahk | RuneLite `LAKE_MOLCH` at `(1363,3632,0)` lists aerial fishing creatures | Wrong activity/place description attached to a real marker |
| `poi_001869`, `(1482,3504,0)` | Ruby harvest, graahk, spotted kebbit | RuneLite `KOUREND_WOODLAND_NORTH_WEST` at `(1481,3504,0)` lists chinchompa 53 | Incompatible target list; catalog also appends the chinchompa label |
| `poi_001653`, `(1557,3436,0)` | Crimson swift, ruby harvest, spotted kebbit | RuneLite `KOUREND_WOODLAND_SOUTH` at `(1556,3436,0)` lists copper longtail 9 | Incompatible target list; catalog also appends the copper longtail label |
| `poi_005778`, `(1560,2920,0)` | Moonlight antelopes on surface south of the Guild | RuneLite places them in `HUNTER_GUILD_CAVERNS`, `(1559,9420,0)` | Surface placement conflicts with the known underground area |

More review candidates are retained in the TSV: three Fossil Island herbiboar markers have birdhouse titles/descriptions; the curated Isle of Souls crimson swift marker is 66.242 tiles from RuneLite's corresponding area; curated sunlight/moonlight moth descriptions and placements require separate verification. These are review candidates, not additional blanket declarations that all listed coordinates are wrong. Map area icons do not establish exact NPC spawn tiles or object footprints.

## Scope measured in this checkout

| Inventory | Count | Meaning |
| --- | ---: | --- |
| Canonical identities | 8,160 | Includes area identities and later additions |
| Identities marked unresolved | 8,020 | Status records incomplete identity verification, not proof of incorrect data |
| Canonical locations | 8,153 | A distinct inventory from identities |
| Locations marked unresolved | 8,020 | Render/search visibility is independent of this status |
| Identities with curated line provenance | 1,694 | Includes assembled marker descriptions and separate curated entries |
| Rendered locations with curated provenance | 714 | Review scope for visible inherited content |
| Curated locations without same-type RuneLite snapshot point within 12 tiles | 607 | Review candidates across 18 marker categories; not a proven-error count |
| Curated Hunter locations in that group | 36 | Hunter deserves the first factual review |
| Nearby Hunter target lists disjoint from RuneLite's creature list | 3 | Lake Molch and the two Kourend examples above |

The 607 count includes content RuneLite's snapshot does not cover, different category conventions, dungeon interiors, and deliberately distinct area markers. It must not be used as a deletion list. Conversely, a nearby same-type point does not establish factual accuracy.

`scripts/audit-poi-provenance.py` generates `docs/poi-provenance-review.tsv`. It inventories every curated-source location, its claims, inherited source line, nearest same-type/same-plane RuneLite reference, distance, visibility, and review flags. The Hunter comparison checks explicitly numbered creature names and only flags completely disjoint lists. It does not detect every incorrect level, method, reward, partial list or unnumbered target.

## Why safeguards missed this

The catalog loader checks column counts, IDs, relationships and allowed status values. It requires nonempty source text, but a string saying `Pre-consolidation ... identity not globally verified` satisfies that requirement. It does not independently verify game facts.

The marker and tooltip paths display unresolved records without differentiating their factual reliability. That means the status field is useful metadata, but provides no protection against presenting an invented claim as a normal POI.

The existing coverage audit checks missing nearby icons, missing image assets and generic descriptions. It compares source coverage in one direction: whether a RuneLite point has a nearby catalog icon. An extra false marker can coexist with all correct icons and pass this check. It does not compare creature identity, claimed region, skill level or reward.

Existing catalog migration tests check consistency between the catalog and the UI. Existing Hunter detail tests assert expected curated titles and methods. These checks can preserve bad authored data while successfully verifying program behavior. This investigation inspected those tests; it did not execute them.

The available evidence supports an unsourced curated-data problem and unsafe proximity enrichment. It does **not** establish whether a human or an AI authored the original block. The fabricated combinations resemble generation errors, but attributing them to AI would be inference.

## Recommended correction sequence

1. Correct the confirmed canonical records, preserving the real Avium Savannah fox marker. Quarantine the false island fox marker and reconcile the island chinchompa entry against the existing grey-chinchompa marker.
2. Replace the incompatible Lake Molch and Kourend descriptions using explicit RuneLite area identities. Review aliases/search results as well as rendered markers.
3. Review all 63 curated-source Hunter locations against area, creature, level, method and reward evidence. Review standalone unsupported placements separately from contaminated real markers.
4. Continue through the remaining curated-source records by category. Require field-specific evidence before marking content verified; do not hide every unresolved record, which would suppress most of the current catalog.
5. Prevent recurrence by requiring an explicit identity association for rich detail enrichment. Add independent factual regression checks for the confirmed errors and a review queue for unmatched authored placements. Avoid treating proximity or a successful import as evidence of identity.

The investigation is complete for the demonstrated source path and failure mechanisms. The catalog-wide factual correction remains outstanding.
