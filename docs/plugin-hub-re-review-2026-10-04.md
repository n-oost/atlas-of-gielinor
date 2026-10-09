# Atlas of Gielinor — Plugin Hub feasibility re-review

Date: 2026-10-04. Initial HEAD: `d5c7a2402fc8ebda456afaa098b23bca8ce8faeb`.
Final reviewed submission baseline: `f8974403bd4c9af45a1069a9f12208c6197b0ac9`.

**Verdict: materially uncertain pending reviewer clarification.** No confirmed security or game-rule submission blocker was established in the scanned production paths. Asset provenance remains incomplete, and the remaining widget-ownership and static boss-reference questions require judgment rather than speculative feature removal. This does not guarantee approval, compilation, or runtime safety.

This is a fresh review of the current source. Findings and line numbers in the earlier `plugin-hub-feasibility-2026-10-04.md` describe a historical baseline, not current defects.

## Submission and scope

The final baseline is on remote `master`. The pre-existing local `.gitignore` edit was preserved and excluded from both authorized commits. No untracked production Java source or required runtime resource was found. The ignored `src/main/java/com/bettermap/map/PoiCategory.class` is local build output, absent from the submission. New review reports and the inventory are local, uncommitted artifacts.

Inventory: **143 Java files, 50,393 lines, and 226 runtime resource files** after the authorized archive operation. The [complete file inventory and token measurements](plugin-hub-inventory-2026-10-04.json) identify individual paths, Git blobs, hashes, and inspection coverage. Supporting inputs include plugin properties, Gradle configuration, BSD license, third-party notices, README, provenance documentation, and POI settings documentation. README images were observed as documentation assets, not executable production inputs.

All production Java was included in restricted-behavior and I/O/API scans. Targeted bounded reads traced lifecycle, input, menu, widgets, downloads, JAR loading, route workers, interoperability, boss cards, and data loaders. This is not a complete line-by-line correctness audit or a reproduction of the Hub bot run. No production source was excluded to fit a model window or token cap.

The surrounding dev-server workspace, OpenRune, RSProx, patched clients, MCP tools, launchers, tests, build outputs, development scripts, diagnostics investigation, and dungeon tuning were excluded from findings. Shipped Java containing diagnostic/tuning branches still participated in the whole-source scans; those branches were not treated as normal gameplay behavior.

## Rules and source verification

RuneLite publishes security and game-rule compliance as its review goals. Functionality, usefulness, performance, interoperability, and factual dataset correctness are outside its routine review goals. This review keeps defects separate from policy blockers. [Published review scope](https://github.com/runelite/runelite/wiki/Plugin-Hub-Review)

Hub submissions select a repository and exact commit. This plugin declares `build=standard`, which replaces local Gradle build/settings files. Local `mavenLocal`, the shadow JAR task, and the template Gradle group therefore do not establish shipping Hub behavior. Java 11 is selected locally, with no additional runtime dependency declared. Exact Hub compilation was not performed. [Plugin Hub README](https://github.com/runelite/plugin-hub/blob/master/README.md)

The current [rejected features](https://github.com/runelite/runelite/wiki/Rejected-or-Rolled-Back-Features) and [Jagex guidelines](https://secure.runescape.com/m=news/third-party-client-guidelines?oldschool=1) were retrieved through browsing. Applicable local `AGENTS.md` instructions were read; no ancestor AGENTS file was found. Local instructions are development constraints, not independent evidence of a current public policy.

Reviewer comments were retrieved through GitHub's comment API when the rendered PR pages omitted them. [Map PR #11551](https://github.com/runelite/plugin-hub/pull/11551#issuecomment-4263572073) and [Slayer Atlas #15925](https://github.com/runelite/plugin-hub/pull/15925#issuecomment-5724629082) reject `client.menuAction`. [#17378](https://github.com/runelite/plugin-hub/pull/17378#issuecomment-5916964003) rejects direct thread interruption. These confirm the local guardrails; no current invoked use of those refused APIs was established.

## Prioritized findings

### R1 — Medium: content provenance remains unresolved

**Classification:** reviewer/provenance question. **Confidence:** high that documentation records gaps; unresolved about applicable redistribution rights.

**Evidence:** `src/main/resources/com/bettermap/ASSET-NOTICES.md:24` and `:40`; `docs/asset-provenance.md:9`; `THIRD-PARTY-NOTICES.md:30`. Enabling downloads reaches `MapAssetManager.java:153` and `:162`. The [asset publisher's notice](https://github.com/n-oost/better-map-assets/blob/main/ASSET-NOTICES.md) describes map-0 as generated using cache-rendering tooling, but lacks a complete source ledger for other map IDs. The current [channel](https://raw.githubusercontent.com/n-oost/better-map-assets/main/channels/tiles-v1.json) points to commit `49644b64a4769c826665a22a11a8edcbcc5d03d9`, SHA-256 `a7d1a28d2de93e914508b0e5cbb56776938342f77a7bb34a3c610881aea2a528`, 49,611,981 compressed bytes, and 9,337 entries.

**Trigger/consequence:** public distribution supplies bundled and downloaded third-party material without a complete per-category source/terms ledger. Existing attribution is favorable but does not resolve the recorded gaps. No specific license violation or blanket software-license incompatibility is established. The prior report's maintainer-supplied Wiki image identification remains historical evidence, not an independently verified tile match in this review.

**Smallest remedy:** reconstruct import/render sources and applicable notices for unresolved map IDs, icons, cavern tiles, and copied dataset text; retain pinned upstream notices. Obtain reviewer clarification on the documented distribution basis. Do not replace assets or remove features speculatively.

### R2 — Medium: overlapping native visibility ownership

**Classification:** reviewer question. **Confidence:** high about the ownership limitation; medium about practical impact.

**Evidence:** `BetterMapPlugin.java:433` tracks widget identity and claims only visible-to-hidden changes; `:453` restores only a claimed, still-self-hidden widget. Startup/render/shutdown callers are at `:228`, `:374`, and `:328`. The old unconditional overview unhiding is absent.

**Trigger/consequence:** another script or plugin hides the same widget while Atlas already owns a hide. A shared boolean cannot identify the second owner, so later restoration could override that intent. Static inspection cannot establish a reliable second-owner detector or demonstrate that this actually occurs.

**Smallest remedy:** retain identity/ownership guards and ask maintainers whether this restoration pattern is acceptable. Request native-hidden, interface recreation, disable/re-enable, and coexistence checks when runtime verification is authorized. A speculative visibility change risks breaking map/chat restoration. Jagex restricts unhiding interface components; applicability to restoration of a plugin-owned hide requires judgment. [Interface rule](https://secure.runescape.com/m=news/third-party-client-guidelines?oldschool=1)

### R3 — Low: static boss cards merit a scope clarification

**Classification:** reviewer question, not a confirmed prohibited boss helper. **Confidence:** high about static implementation; unresolved about maintainer interpretation.

**Evidence:** `data/MonsterLocationData.java:32`, `map/BossLocationIndex.java:50`, `ui/markers/RaidBossDisplay.java:53` and `:302`, `ui/tooltips/MonsterTooltipBuilder.java:67`, `ui/MapTooltipRenderer.java:183`. The manually opened map gallery displays fixed boss lists and reference tooltips, including generic strategy, drops, and imported combat stats. It does not read live boss attacks or projectiles to select recommendations.

**Trigger/consequence:** opening a raid gallery and expanding a card shows static combat/reference details. No next-attack prediction, prayer instruction, mechanic timer, focus identification, hazard prediction, or automatic combat standing indicator was established. Boss locations alone do not establish those behaviors. The broad RuneLite stance on new high-end boss plugins warrants a precise description to reviewers, not automatic classification of a general map as banned. [Rejected features](https://github.com/runelite/runelite/wiki/Rejected-or-Rolled-Back-Features)

**Smallest remedy:** describe the gallery as static, manually opened reference information and seek a scope ruling if requested. Removing it would require a user decision and is not an applied remedy.

### R4 — Low: README contains stale packaging/cache descriptions

**Classification:** confirmed documentation defect. **Confidence:** high.

**Evidence:** `README.md:133` claims an LRU budget in megabytes, while `tiles/TileLoader.java:59` and `:60` use 512 decoded-tile and 256 scaled-image entry caps. The claimed 49 MB does not follow from those limits; a 512-entry 256×256 ARGB cache alone represents about 128 MiB of pixel data, before overhead and other caches. This is a calculation, not a measured allocation. `README.md:148` names a runtime `travel_networks.json.gz` that is absent; travel data is authored in `data/TravelData.java`.

**Trigger/consequence:** readers receive inaccurate packaging and memory expectations. Performance/document accuracy is not thereby a confirmed policy blocker.

**Smallest remedy:** describe the actual cache caps and travel-data source. These are recommendations only; no unrelated README edits were applied.

## Favorable evidence and coverage ledger

| Area | Traced implementation and result |
| --- | --- |
| Restricted execution | All 143 Java files scanned for reflection access, JNI/JNA/Unsafe/native memory, subprocesses, executable downloads/classloading/runtime generation, Java object serialization, synthetic input, direct interruption, server actions, chat rewrites, networking, and disk I/O. No invoked prohibited mechanism was established in the traced paths. Names obtained with `getClass().getName()` classify ownership and are not reflective member access. |
| Input and menus | `BetterMapPlugin.java:1142` appends local RUNELITE entries; callbacks pan, mark, display local messages, or calculate/share route destinations. `WorldMapInput.java:488` passes native control bounds through; overlay `:731` uses original close bounds. Escape is native except local modal dismissal. No menu clearing/reordering or automatic walking was established. |
| Download trust and cleanup | `MapAssetManager.java:61–100`, `:181–214`, `:222–355`: injected OkHttp/Gson, generation guards, cancellation, atomic marker installation, fixed repository paths, archive/inventory hashes, bounded compressed/expanded sizes, narrow image/index allowlist, duplicate/path checks, and 256×256 PNG header checks. These verify consistency with the publisher, not independent publisher trust. PNGs are data, not code. |
| Opt-in | `BetterMapConfig.java:514–525` defaults downloads off and includes the third-party IP warning. Download requests contain fixed asset paths; no player/credential upload was established. Boat state is cached through ConfigManager, and route sharing uses a local EventBus. |
| Lifecycle and threading | Plugin `:294–338` removes overlays/listeners/input, cancels startup work, and shuts down asset/routing workers. `MapLayerInputHandler.java:442–505` now invalidates/cancels delayed Finder work. `RoutePlanner.java:167–307` owns worker computation, client-thread live-state refresh, finite waits, cancellation, generation/revision guards, and cleanup. No build/runtime proof is claimed. |
| JAR resources and rendering | POI/dungeon startup passes `null` (`BetterMapPlugin.java:265`, `:270`); unused dev disk overrides are not findings. Dataset loaders use bundled streams and gzip. Collision ZIP is read into data, not executed or extracted to arbitrary disk paths. Tile decode uses workers and capped caches; frame work includes visible rendering, per-frame marker iteration, and some scaling. Native-close clipping is retained by intersecting tile clips. No profiling was performed. |
| Interoperability and chat | `WorldMapPointReader.java:80` uses an always-false removal predicate to snapshot markers; official current source confirms its CopyOnWriteArrayList backing. This does not remove markers or use reflection. Finder uses a plugin ChatboxPanelManager input (`RuneliteChatboxInputHost.java:58`), not outgoing chat. GAMEMESSAGE output is local. |
| Data/notices | All resources inventoried. Three gzip JSON datasets parsed for structure/source fields and restricted-assistance phrases. All 174 bundled PNG headers had PNG signatures, with maximum dimensions 256×256. Collision ZIP inventory: 2,726 entries, 4,437,243 expanded bytes. Upstream Shortest Path revision/license and RuneLite imported revision/license sections are present. This does not validate every record, image decoder, or external pack entry. |

All lifecycle and ownership conclusions are bounded static findings. The full external asset archive was not downloaded or audited. Compilation against the Hub client revision, actual JAR contents, runtime races, native game behavior, compatibility, and performance remain unverified.

## Applied changes versus recommendations

Only two explicitly authorized cleanup changes were committed and pushed:

1. `bb262ad5302fe635f2530c6c431690ce7b5af1f2`: removed three unused bundled font files. All observed production fonts come from RuneLite FontManager (`MapStyle.java:60–64`); no local TTF loader reference was found. RuneLite's own FontManager loads its own package-relative font resources. Removal is statically supported and runtime-untested.
2. `f8974403bd4c9af45a1069a9f12208c6197b0ac9`: moved the unused complete shop import to `../archive/shop_data_complete.json`. Archive hash matches the original file: `d34d749226c3f0bdbb136cd1a7a84cae49445695701e6d101f77f92a83422701`. Runtime still reads `shops.json.gz` (`ShopIndex.java:63`, `:133`). Notices retain attribution and a pinned historical source link. This removes an unused packaging input, not the shop feature. No Java or live runtime dataset was edited.

The complete shop snapshot has 455 records; the runtime file has 450 placed records. The former uniquely retains five unplaced shops, Wiki titles, and import summary metadata. The latter contains enriched stock records and sale prices. Of matching shops, 373 have identical ordered stock name/quantity lists; 66 have different `stock_total` fields. The files were not interchangeable. All archived data was preserved.

R1–R4 and the [size recommendations](code-size-analysis-2026-10-04.md) remain recommendations/questions. Previous missing production inputs, unconditional overview unhiding, and the delayed Finder shutdown defect are already corrected in the starting source and are not newly applied fixes here.

## Automated review size — separate from policy

Eligibility is **unverified**. Reviewer confirmations establish a strict under-200,000-token bot limit and manual review for larger plugins: [Alexsuperfly](https://github.com/runelite/plugin-hub/pull/15816#issuecomment-5499005964), [Psychemaster](https://github.com/runelite/plugin-hub/pull/17331#issuecomment-5888459342). These are eligibility constraints, not rejection rules.

The inventory records an estimate with `tiktoken 0.12.0`, `cl100k_base`, independently tokenizing UTF-8-decodable Git blobs using `encode_ordinary`. Comments, whitespace, and notices are retained; filenames/prompts are not added. Binary resources are inventoried but not decoded/expanded into the token count. Tests and dev files are outside this estimate, without any claim that the bot excludes them. Before shop archiving, Java was 553,083 tokens and all selected production text was 1,315,027 tokens. The final baseline is **1,110,189 estimated production-text tokens** across 191 UTF-8-decodable production blobs; Java remains 553,083. Separately, the selected supporting metadata/documents total 6,061 tokens. The new local reports/inventory are not included in these submission-baseline totals.

Exact bot tokenizer, preprocessing, and boundaries remain unpublished in the checked sources. The estimate strongly suggests preparing for manual review, but does not prove the bot's count. No character-count heuristic or resource relocation is used to declare eligibility.

## Interpretation checks and actual checks performed

Reasoning checks, not measured model accuracy or executed tests: a referenced untracked Java class would block compilation, but none was found now; unused dev disk overrides are outside findings; opt-in PNG download is not executable code download; static boss locations do not prove fight prediction; plugin search input is not outgoing-chat autotyping. Exceeding the bot cap retains the manual route; an estimate is not the bot count; feature removal needs a user decision; any future lifecycle fix needs caller/ownership evidence and remains untested without checks.

Performed: Git/source inspection, primary-source retrieval, token estimation, JSON/gzip parsing and dataset comparison, PNG header/dimension inspection, collision ZIP listing, archive hash comparison, diff/staging inspection, and remote push confirmation for the two authorized commits.

**Not performed:** Gradle builds, unit tests, client launch, gameplay interaction, live-game connection, runtime checks, performance profiling, Hub submission, or PR publication. The local testing instructions are superseded by the user's explicit no-build/no-test/no-live-game scope; no launcher offer or login flow was used.

## Smallest path to readiness

1. Resolve or obtain reviewer judgment on the recorded asset provenance gaps.
2. Present the precise static boss-card and native visibility-restoration behavior if reviewer clarification is needed.
3. Correct the two stale README descriptions when requested.
4. Request verification of the exact submitted commit against the Hub environment and native lifecycle paths before submission.

Next action: identify the original source/import record for one unresolved map ID in the asset provenance inventory.
