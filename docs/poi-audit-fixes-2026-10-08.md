# POI audit corrections — 2026-10-08

The October 7–8 audits were checked against the current catalog and freshly fetched RuneLite source. All 128 originally confirmed catalog findings were still present. They are corrected, together with six herbiboar/birdhouse conflicts identified in the earlier investigation and confirmed during this pass.

The user subsequently explicitly confirmed that the Soul Wars / Isle of Souls pyre fox should not be there. **`poi_005784` was removed from both canonical identity and location tables**, removing its map marker, Finder entry and positional tooltip. Correct Avium Savannah records `poi_000490` and `poi_007813` are retained. The fix ledger records the removed rows and user confirmation as the evidence for this correction.

| Correction | Locations |
| --- | ---: |
| Fishing descriptions conflicting with corresponding source definitions | 108 |
| Hunter target descriptions conflicting with corresponding source definitions | 12 |
| Maple trees incorrectly described as teak trees | 4 |
| Ferret quest requirement, sunlight/moonlight moth effects, island chinchompa species/level | 4 |
| Herbiboar markers incorrectly described as birdhouse activities | 6 |

There are **134 corrected identities**, affecting 69 rendered and 65 hidden locations. **64 incorrect search aliases** were replaced. Conflicting fishing tools and Hunter methods/region names were removed where the evidence supported only the source subjects and levels. The six herbiboar descriptions now identify tracking, Hunter 80, Herblore 31 and Bone Voyage access. Native coordinates and visibility flags were preserved; description correction does not establish exact live spawn placement.

`PoiIndex` supplies a generated chest icon for `thieving` and the bundled `task_master` icon for `services`. This repairs the two pirate-chest and three sailing trial-master omissions without changing their icon categories. The coverage helper now follows those runtime mappings.

The island rule now targets the independently supported grey/red chinchompa disagreement. It no longer labels the pyre-fox habitat as a factual conflict based on omission from partial sources. The original review ledger preserves that evidence limitation and records the subsequent user-confirmed removal. A narrow herbiboar/birdhouse rule and valid birdhouse/herbiboar controls were added to prevent those mixed descriptions from escaping the audit again.

## Evidence and validation

[The fix ledger](poi-audit-fixes-2026-10-08.json) contains every corrected ID, native coordinate, before/after field, search alias and source reference. Original findings remain in [the review ledger](poi-audit-review-2026-10-08.json), with resolution metadata and six additional adjudicated findings.

The 124 original reference conflicts agreed at both immutable RuneLite revisions: `d8e7d1e5f34e2899eda3d7cf4cd9661ae2206f22` and `42a6f17a6a2e8e478aa763890ecd0181a59dad38`. The latter revision also independently identifies the six herbiboar markers. Indexed Wiki evidence supports the gameplay corrections and tracking requirements; direct Wiki pages remain blocked, so the indexed text's freshness limitations still apply. Evidence establishes map definitions and the stated gameplay fields, not exhaustive current spawn coverage.

The factual audit, independent verifier, audit controls, coverage audit and provenance inventory all succeeded after correction. The rerun has **zero identity conflicts, zero factual-rule conflicts and zero missing icons**. There are 134 corrected identity rows plus the removed island fox identity/location. All other native location rows are preserved.

**66 Java tests pass** across the catalog, index, search, categories, tooltip builder and STASH integration. New regression checks exercise source-supported subjects through tooltip lookup, corrected gameplay claims, all six herbiboar search identities and the repaired icon keys. A removal regression checks the island fox is absent from rendered markers, Finder and exact positional tooltip lookup while Avium Savannah markers remain. An existing tooltip test expected the audited incorrect teak label; it now expects maple.

## Remaining review queue

- Sixteen Hunter placement candidates and 606 authored coverage candidates remain in the regenerated factual inventory, across 610 distinct locations. These are not confirmed errors.
- The original review ledger retains all historical unresolved findings; removal of false subjects also removes some derived distance candidates without moving markers.
- The coverage inventory retains 3,973 generic descriptions and River Kelda's region-label-versus-dungeon-icon classification question. Runtime enrichment and intentionally simple labels must be considered before treating those as defects.
- The removed island fox's pyre-log reward text is preserved only as historical evidence in the fix ledger; it is no longer a runtime claim.

## In-game confirmation

Automated checks cannot confirm in-game rendering. Check the Pandemonium fishing tooltip, Wyrmscraig maple trees, Lake Molch aerial fishing, the three visible Fossil Island herbiboar markers, the moth effects, and the two pirate-chest/three trial-master icons. Finder should no longer attach the corrected records to their old fish, tree or birdhouse titles. Check both compact and expanded tooltips.

Launch from the plugin root with `./gradlew run`. Jagex account users should follow [RuneLite's development-client login instructions](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts). In-game confirmation remains pending.

## Reproduce

```powershell
python scripts/audit-poi-facts.py
python scripts/verify-poi-audit.py
python scripts/check-poi-audit.py
python scripts/audit-poi-coverage.py
python scripts/audit-poi-provenance.py
```

```powershell
.\gradlew.bat test --tests atlasofgielinor.map.catalog.PoiDetailsTest --tests atlasofgielinor.map.catalog.PoiIndexTest --tests atlasofgielinor.map.catalog.PoiIndexSearchTest --tests atlasofgielinor.map.catalog.MapCatalogMigrationTest --tests atlasofgielinor.map.catalog.PoiCategoryTest --tests atlasofgielinor.ui.tooltips.PoiTooltipBuilderTest --tests atlasofgielinor.integrations.StashUnitCatalogTest
```
