# Code and review-input size opportunities — 2026-10-04

These are recommendations, not implemented code changes. Measurements use `tiktoken 0.12.0`, `cl100k_base`, and `encode_ordinary` on Git blob text at `bb262ad5302fe635f2530c6c431690ce7b5af1f2`. Candidate representations were constructed only in memory. No compiler, tests, or runtime checks were run. The [inventory](plugin-hub-inventory-2026-10-04.json) contains file-level measurements and the updated submission baseline.

Baseline Java: **553,083 estimated tokens** across 143 files. Following the authorized shop archive, selected production text totals **1,110,189 estimated tokens** at `f8974403bd4c9af45a1069a9f12208c6197b0ac9`. Source and resources are different contributors: moving Java data into another counted text file does not itself eliminate the data's tokens. The exact Hub tokenizer, preprocessing, and resource boundaries are unverified. Manual review is a valid route.

## Ranked remaining opportunities

| Recommendation | Evidence and measured potential | Preservation requirements |
| --- | --- | --- |
| Compact the RuneLite metadata JSON | `poi/runelite-world-map-data.json`: 135,575 → 87,615 estimated tokens, **47,960 saved**, solely by compact JSON formatting. No Java change is required for normal Gson parsing. | Preserve all values, row order, source revision, and copyright/license sections. This is token-input reduction, not Java-code reduction. |
| Share repeated POI descriptors and keep an ordered location table | `map/PoiDetails.java:583` onward: 1,656 flat `addEntry` calls use 1,104 distinct descriptors; many tree/fishing rows repeat exactly the same title/category/details. A descriptor table plus ordered coordinate references measured 104,925 → 76,155 tokens for those call lines: **28,770 gross savings**, before loader/helper overhead. | Keep original insertion order, exact text, coordinate/plane values, deduplication, basement classification, exact-map overwrite precedence, and chunk lookup behavior in `addEntry` at `:538`. A resource parser requires schema validation and JAR-stream loading. This representation is a sizing prototype, not a validated migration. |
| Store metadata fields once as columns | The same RuneLite JSON, preserving every field and license section, measured **59,070 tokens** with a column header plus ordered arrays. That is **76,505 gross savings from the original**, or **28,545 additional savings after compact formatting**. | Requires a localized `WorldMapSupplement.Entry` loader change (`:95`) and exact row/null/number handling. Keep `dataset`/`constant` source metadata even when not currently used by runtime. Do not add these savings to the first row's total twice. |
| Remove duplicated quest titles through one helper | `map/QuestDetailsData.java:18`: 216 rows repeat the same title as both map key and Detail title. Hypothetical helper calls measured 18,195 → 15,500 tokens: **2,695 gross savings**, before the small helper body. | Construct the same Detail and retain all literal categories/lines and existing map insertion behavior. More ambitious decomposition of difficulty/start/reward prefixes has not been quantified. |
| Use a small coordinate factory in authored data | `data/UndergroundZone.java:92` and `data/TravelData.java:213` repeatedly construct WorldPoint values. Replacing only `new WorldPoint(` with a factory call in those two files measured **2,078 gross savings**, before factory bodies. | Keep all coordinates, planes, enum constants, aliases, optional arguments, and lookup semantics. The gain is small relative to the migration risk of converting the full enum to a new dataset. |

The major authored Java files are PoiDetails (~119,649 tokens), UndergroundZone (~37,586), TravelData (~24,692), and QuestDetailsData (18,195). Repeated data descriptions offer more useful savings than making input/lifecycle code harder to read.

## Authorized cleanup already applied

The full shop import was archived outside the plugin repository at the user's request and removed from runtime resources in `f897440`. It contributed **204,908 estimated tokens** to the prior selected production-text estimate. The immutable source-history copy and notices preserve provenance access. Current runtime reads the separate gzip dataset; no shop feature was removed.

The archived JSON could itself shrink from 204,908 to 114,555 estimated tokens through compact formatting, but it is now outside runtime resources and was not reformatted. That **90,353-token** opportunity must not be added to the already-applied archive reduction. Earlier discussion's **138,313-token** combined JSON-formatting saving referred to this file plus the RuneLite metadata file before archiving.

The font cleanup removed 64,836 bytes of unused TTF files. It shrinks packaged binary resources; the text-only estimate did not count those fonts, so no textual-token saving is claimed.

## Shop comparison and archive evidence

| Property | Archived complete import | Runtime `shops.json.gz` |
| --- | --- | --- |
| Records | 455, including five unplaced shops | 450 placed shops |
| Unique source information | `wiki_title`, `placed`, `unplaced`, `origin_counts` | Source and generation metadata retained, but no per-shop Wiki title |
| Stock field | `stock_rows` | Enriched `stock`, including sale-price fields |
| Shared-field differences | 66 matching records have a different `stock_total` | Runtime values preserved |
| Reader | No production reference found | `ShopIndex.java:63` and `:133` |

The complete-only unplaced records are Beach Cocktails, Beach Kit, Bunbridge General Store, Farming Supplies, and Sawmill. Among matching shops, 373 preserve identical ordered stock name/quantity lists; the remaining records differ in entries, quantities, or order. These snapshots are not interchangeable and should not be merged by blindly choosing the larger file.

Archive path: `C:/dev/runelite-plugin-better-map-2/archive/shop_data_complete.json`.
SHA-256: `d34d749226c3f0bdbb136cd1a7a84cae49445695701e6d101f77f92a83422701`.
All 455 records and source metadata remain in that unchanged archive and in pinned Git history.

## Constraints and practical result

Do not strip copyright/license notices, remove safety guards, shorten identifiers throughout the project, move executable code to runtime downloads, omit shipped input, or delete features to chase the cap. Splitting files leaves the combined content intact. Compression changes disk size and may not change bot input because expansion boundaries are unknown. Dropping empty JSON defaults requires a schema/caller review; it is not equivalent to compact formatting.

Even the measured Java opportunities above leave the source estimate far above 200,000. This analysis identifies smaller and clearer representations; it does not demonstrate a behavior-preserving route to bot eligibility or promise approval. Candidate savings are not all additive, and parser/helper overhead is excluded where stated.

Next action: choose compact formatting of `runelite-world-map-data.json` as the smallest remaining size change; it requires no Java refactor.
