# POI audit review — 2026-10-08

## Correction status — 2026-10-08

All **128 originally confirmed findings are fixed** in the runtime catalog. Review of the earlier investigation also confirmed and corrected six herbiboar/birdhouse description conflicts (three rendered markers and three hidden duplicates). In total, **134 canonical descriptions and 64 search aliases were corrected**. The two pirate-chest and three trial-master icon omissions were also repaired.

The user subsequently confirmed that the Soul Wars / Isle of Souls pyre fox should not be there. Its identity and location (`poi_005784`) were removed from runtime data, including map, Finder and positional tooltip lookup. Avium Savannah pyre-fox records remain. The review ledger records this user confirmation and removal.

The rerun reports zero identity conflicts, zero factual-rule conflicts and zero missing icon assets. Sixteen placement candidates and 606 coverage candidates remain. Corrected descriptions do not certify physical placement. Before/after values and source evidence are in [the fix ledger](poi-audit-fixes-2026-10-08.json). Original findings below describe the pre-fix catalog; the review JSON records their resolutions.

## Original review

**124 confirmed reference conflicts and four gameplay claim errors** affect 128 distinct catalog locations: 66 flagged as rendered and 62 hidden. This pass screens all 8,153 locations and 8,160 identities; it does not certify every field or live spawn.

| Disposition | Findings | Distinct locations |
| --- | ---: | ---: |
| Confirmed reference conflict | 124 | 124 |
| Confirmed factual error | 4 | 4 |
| Unresolved habitat claim | 1 | 1 |
| Unresolved placement candidate | 41 | 29 |
| Unreviewed coverage candidate | 607 | 607 |

There are **777 findings across 735 distinct locations**. Categories overlap: one location can have a confirmed description conflict and unresolved placement. The [review ledger](poi-audit-review-2026-10-08.json) contains every disposition, reason, canonical/location ID, native coordinate, catalog line, alias, inherited source, reference ID and evidence link. Runtime catalog data was not changed.

## Gameplay claims

| Location and native coordinate | Conflicting field | Supported correction |
| --- | --- | --- |
| `poi_005779` — `(1720,2980,0)` | Sunlight moth effect: “Restores 8–19 Prayer” | Restores reduced stats excluding Prayer and heals 8 Hitpoints. [Sunlight moth](https://oldschool.runescape.wiki/w/Sunlight_moth) |
| `poi_005780` — `(1535,3065,0)` | Moonlight moth effect: general stat restoration | Explicitly identify 22 Prayer restoration; sharing requires multicombat and Accept Aid. [Moonlight moth item](https://oldschool.runescape.wiki/w/Moonlight%20moth%20%28item%29) |
| `poi_005772` — `(2330,3500,0)` | Wild ferrets: Eagles' Peak merely started | Quest completion is required by the cited mountain article; scripted quest capture is separate. [Eagles' Peak mountain](https://oldschool.runescape.wiki/w/Eagles_Mountain) |
| `poi_005783` — `(2220,2870,0)` | Isle of Souls red-chinchompa identity | The island population is grey chinchompas. Exact authored coordinates still need review. [Chinchompa habitat](https://oldschool.runescape.wiki/w/Chinchompa_%28race%29), [Jagex island design](https://secure.runescape.com/m=news/poll-74-game-improvements-blog?oldschool=1) |

**Freshness limit:** direct Wiki access is blocked by robots.txt. Indexed moth/ferret text was crawled about 1.3 years ago; the chinchompa article about six months ago. These dispositions confirm disagreement with that available gameplay evidence, without establishing a freshly fetched October 2026 Wiki revision. The Jagex blog dates to January 2021. RuneLite source was freshly fetched during this pass.

The earlier report marked `poi_005784`, the Isle of Souls pyre fox at `(2250,2900,0)`, as a confirmed factual error. Its habitat claim is now **unresolved**. Current RuneLite defines pyre foxes at Avium Savannah; a [Wiki-derived mirror](https://danielpgleason.com/osrs/reference/pyre-fox-f38936b6/) saved September 27, 2026, at Wiki revision `15329213` agrees. That positive habitat statement and omission from incomplete sources do not prove present absence from every island spawn. Current exhaustive habitat/spawn evidence is needed. The ledger preserves the previous disposition and reclassification reason. Its pyre-log reward claim also remains unverified in this pass.

## Reference conflicts and the audit repair

The conflicts comprise **108 fishing descriptions, 12 Hunter descriptions and four tree descriptions**, grouped into **21 authored title/claim combinations**. Each location was independently compared; a repeated title alone was not used to certify an error.

The original resource check required `curated-details.tsv` line metadata, which migration omitted from some records while retaining their authored text. Removing that filter found **57 additional conflicts: 56 hidden locations and one rendered location**. Grouping now uses authored title and claim.

The additional rendered finding is `poi_000487`, `(3033,2997,0)`: its tooltip combines **Harpoonfish** with an appended **Anchovies** source label. The bundled export and independently fetched `PANDEMONIUM` definition at `(3032,2997,0)` identify shrimp, anchovies, sardine and herring. [Immutable Pandemonium definition](https://github.com/runelite/runelite/blob/42a6f17a6a2e8e478aa763890ecd0181a59dad38/runelite-client/src/main/java/net/runelite/client/plugins/worldmap/FishingSpotLocation.java#L185)

Other examples include squid areas described as shark/lobster, maple areas described as teak, and Lake Molch described as a bird/graahk area. All locations appear in [the source verification report](poi-audit-verification.md) and [evidence JSON](poi-audit-verification.json).

Every comparison agrees at both immutable revisions:

- Bundled: `d8e7d1e5f34e2899eda3d7cf4cd9661ae2206f22`.
- Current master, resolved once: `42a6f17a6a2e8e478aa763890ecd0181a59dad38`, commit date `2026-10-07T10:36:01Z`.

Evidence retains SHA-256 hashes, Java enum IDs, native reference coordinates and line links. These are reference conflicts; nearby map icons do not establish every live spawn or exhaustive local content.

## Runtime and introduction trace

`catalog/locations.tsv` joins stable locations to identities in `catalog/pois.tsv`. `PoiIndex.loadData()` uses visibility/search flags and aliases. `PoiDetails.getDetail()` resolves indexed markers by location ID; `PoiTooltipBuilder.buildPoiCard()` displays stored claims. Current indexed lookup is by ID: conflicting authored text is already in the catalog.

Pandemonium lacks retained curated-line metadata. Legacy `poi/curated-details.tsv:1144–1149` contains the same Harpoonfish title/text; the nearest legacy entry is `(3036,2996,0)`. This proposes a reproducible inherited association without proving the exact missing migration association.

Git first contains both the island fox and Harpoonfish titles in import commit `feb4dbf`, September 25, 2026. Extraction commits `aafd4e7` and `35d2e43` follow October 5; canonical migration `f7cf1fb` follows October 7. Git cannot identify the original author before import. The [earlier investigation](poi-data-investigation-2026-10-08.md) retains the detailed fox path; this review's unresolved habitat disposition supersedes its stronger conclusion.

`ui/MapTooltipRenderer.java:307` supplies marker coordinates to detail lookup; line 315 adds the **cursor** coordinate to the footer. Footer coordinates do not establish authored placement. Rendered totals describe catalog flags; runtime settings can further hide markers.

## Remaining scope and reproduction

All placement/coverage findings remain in the ledger and [candidate inventory](poi-factual-audit.json). No exhaustive current spawn/cache dump or game interaction was used. Other categories receive provenance coverage screening; their requirements, rewards and mechanics have not all been independently checked. Unflagged records are not certified correct.

Run from `better-map`:

```powershell
python scripts/audit-poi-facts.py
python scripts/verify-poi-audit.py
python scripts/check-poi-audit.py
```

All three commands succeeded. Controls preserve valid trout, swordfish, level-63 red-chinchompa and sunlight-moth descriptions; detect wrong fish with and without curated metadata and a wrong Hunter level; and preserve parsed catch families. No Gradle or in-game behavior checks were needed for this report/helper change.

The first two commands regenerate candidates and source comparisons. The dated ledger records assistant adjudication separately from script execution.
