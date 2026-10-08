# POI runtime verification — 2026-10-08

## Confirmed errors

Five canonical records contain factual errors. These conclusions concern the stated subject/mechanic; exact replacement coordinates and current live OSRS spawns have separate evidence gaps.

| Location / canonical ID | Requested and actual local tile | Confirmed factual error | Independent reference |
|---|---|---|---|
| `poi_005888` Tai Bwo Wannai Trio | 2900,3500,0 | Fishing requirement is **5**, not15. Stated Timfraku start conflicts with authored Heroes’ Guild vicinity. | [Wiki revision14918129](https://oldschool.runescape.wiki/w/Tai_Bwo_Wannai_Trio?oldid=14918129); local reference visit2780,3087,1 observes Timfraku NPC4698. |
| `poi_005858` Dragon Slayer I | 3074,3515,0 | Reward is **2 Quest Points**, not3. Quest begins with Guildmaster at Champions’ Guild; Oziach is not an alternative initial start. | [Wiki revision14917676](https://oldschool.runescape.wiki/w/Dragon_Slayer_I?oldid=14917676); local reference visit3189,3360,0 observes Guildmaster NPC814. |
| `poi_005862` Elemental Workshop I | 2740,3445,0 | Start uses **battered book from bookcase**, rather than slashed book/library. Elemental shield is produced during the quest, rather than a prior requirement. | [Bookcase](https://oldschool.runescape.wiki/w/Bookcase_(Elemental_Workshop_I)); object26113 independently identified. Exact bookcase tile remains unconfirmed. |
| `poi_002112` Common tench / greater siren | 1371,3632,0 | Ordinary net/rod/pot/harpoon tools are wrong: this is **aerial fishing with cormorant glove**, kingworms/fish offcuts and paired Fishing/Hunter requirements. Error visible in live expanded tooltip. | [Lake Molch revision15333224](https://oldschool.runescape.wiki/w/Lake_Molch?oldid=15333224), independently fetched [secondary snapshot](https://danielpgleason.com/osrs/reference/lake-molch-d4241815/). |
| `poi_007113` hidden searchable duplicate | 1370,3632,0 | Same incorrect ordinary fishing tools. Searchability and detail output confirmed through production loader; separate live search selection not sampled. | Same independently retrieved Lake Molch reference. |

Official indexed Wiki text for the first two quest findings was crawled approximately1.3years ago. Recent secondary Wiki snapshots report September2026 saves and original revision IDs; they do not establish the current original revision. Source freshness, URLs, fetched files and exact remaining gaps are retained in the [machine ledger](poi-runtime-verification-2026-10-08.json) and research fragments. Catalog text was not used to verify itself.

## Verified behavior and applied corrections

**All134 recorded corrected description field sets match the current canonical catalog.** Fresh RuneLite references match the earlier pinned comparison. This confirms correction application, rather than every field’s live mechanic or every physical location. The exact staged plugin jars contain byte-identical catalog resources; the running local JVM predates no staged catalog change.

| Check | Result and limit |
|---|---|
| Live client | OpenRune MCP `LOGGED_IN`; local throwaway account only. Final native location2244,3460,0. |
| Catalog visits | 744 unique requested IDs: original prioritized742 plus2 aerial-fishing duplicates. **731 arrivals confirmed**,13 individually blocked teleports. Every attempted visit preserves requested/actual X,Y,plane and raw evidence. Six additional explicit reference visits are separate. |
| Correction loading | Exact staged production loader returns rendered index6501 and search entries8633. All134 corrections load;69 are rendered and65 hidden. Alias correction evidence retained. Headless loader output does not establish live visibility. |
| Live visual samples |16 distinct IDs have explicit observations, including blocked samples. Pandemonium fish, Wyrmscraig maple, Lake Molch aerial fishing, two herbiboar cards, two moth cards and three trial-master cards were observed. Compact/expanded cards, search groups and screenshots are individually linked. First herbiboar hover missed its marker; two chest hovers selected overlapping dock/dungeon overlays, so no visual pass is inferred. |
| Icons and removal | Seven service/thieving category mappings generate icons through the exact production loader. Trial master icons additionally observed after enabling Tutors and services. Setting restored to its initial unchecked state. Removed `poi_005784` is absent from catalog, rendered index and search. River Kelda region labels cover the source coordinate; lack of a separate dungeon icon is not an absence error. |

The Lake Molch **35/51/68/87 Hunter thresholds are correct**; different43/56/73/91 values refer to Fishing. Nine distant Hunter habitat allegations are dismissed because RuneLite’s enum does not enumerate the Sailing habitats. Local species observations support eight of those IDs; black warlock retains its exact-scene gap. Open water is not sufficient to reject boat NPCs Ralph/Gurtob: independently identified trial scoreboards support their authored start vicinities.

## Reference conflicts and strongest remaining candidates

| ID | Disposition / evidence | Exact remaining gap |
|---|---|---|
| `poi_005778` Moonlight antelope | **Confirmed reference conflict.** Authored1560,2920,0 is Sunset Bay surface; expanded marker observed there. Native1559,9420,0 visit observes four Moonlight antelope; Atlas selects Hunter Guild Caverns. Piece446 transforms9410..9471 to3011..3072, so it cannot explain authored2920. | Independent exact replacement anchor and exhaustive current live habitat. Two original finding rows concern this same ID. |
| `poi_005772` Ferret | **Confirmed reference conflict.** Independently obtained Eagles’ Peak/location accounts disagree on partial versus full quest completion. Ferret presence does not test the gate. | Authoritative gate script or non-quest catch before/after completion; private-server behavior alone cannot certify OSRS. |
| `poi_005871` Regicide | **Confirmed reference conflict.** Secondary revision15292278 says Experienced/King Lathas at Ardougne Castle plane1, versus authored Master and distant native marker. | Fresh primary revision, exact King Lathas spawn and quest-state/entrance semantics. |
| `poi_005883` Making Friends with My Arm | **Confirmed reference conflict.** Secondary revision15101556 says Master/Burntmeat kitchen, versus authored Experienced/My Arm. Local2844,10057,1 visit observes Burntmeat NPC4157. | Fresh primary revision and native/display/entrance semantics; observed NPC association does not fix marker location automatically. |
| `poi_005815` Gwyna | **Unresolved placement suspect.** Scoreboard58928 independently named `SAILING_BT_SCOREBOARD_GWENITH_GLIDE` appears at2244,3460,0,58tiles from authored2198,3518,0. Targeted visit confirms scoreboard; Gwyna parent15105/morphs15106–15108 not observed. | Independently established exact boat NPC start tile or interaction/quest-state evidence. Missing local boat content is not absence proof. |

Thirteen fishing records have stronger conflicting NPC-family associations: Sacred eel `poi_002661/002664/002665`; crystal eel `002829`; infernal eel `002735/002738`; Camdozaal tetra `002841/002885`; cave eel `003255/003269/003274`; shrimp/anchovies `003026`; volcanic ash/seaweed `002649`. Their ledger rows retain observed numeric NPC IDs and fresh RuneLite family mappings. **All remain unresolved**: nearby spawns are association candidates, not proof that the marker denotes them. Enchanted Valley’s special fishing spot additionally needs its summon interaction.

## Queue completion and retained gaps

The review reconciles777 historical findings against current data and the fix ledger. All622 current findings across610 locations have individual dispositions, canonical IDs, claims, requested/actual coordinates, source links/revisions where available, raw evidence and precise gaps. The final ledger has767 review records:5 confirmed errors,5 reference-conflict rows across4 IDs,151 dismissed candidates,593 unresolved and13 blocked. These are finding counts, not unique POI counts.

All3,973 secondary generic-description completeness candidates remain visible in the machine ledger, explicitly **unreviewed for substantive correctness**. Generic wording and absence from a nonexhaustive reference snapshot are not established errors. The606 authored-location candidates all retain kind-specific identity/placement gaps, including area and landmark cases with map/geographic evidence.

Thirteen catalog visits are individually blocked: `poi_003863`, `005741`, `005755`, `005756`, `005694`, `005695`, `005718`, `005724`, `005729`, `005736`, `005737`, `005738`, `005744`. Their exact requested/actual coordinates and failure reasons are in their ledger rows. No repeated attempt was used to infer an error. An initial collection was interrupted because a floating map prevented input; that global blocker was resolved with the native close control before the completed collection. Its artifacts are preserved separately.

## Evidence and reproducibility

| Artifact | Contents |
|---|---|
| [Main machine ledger](poi-runtime-verification-2026-10-08.json) | Individual767 records, original finding reconciliation, all secondary candidates, requested/actual coordinates, dispositions, source freshness, explicit reference visits and preservation checks. |
| [Independent subject research](poi-runtime-independent-research-2026-10-08.json) | Hunter/ferret/herbiboar/moth/fishing comparisons; fresh pinned RuneLite revision and fetched snapshots. |
| [Independent coverage research](poi-runtime-coverage-research-2026-10-08.json) | All606 authored-location candidates, quest/fishing/trial followups and all3973 generic rows. |
| [Collected evidence directory](../../../runelite-p-server-for-devving/dev-server/poi-verification-runs/20261008-complete-audit-collected) | Batch reports, supplements, native map cache, raw entity definitions, production lookup, screenshots, UI observations and input SHA256 manifest. Paths in ledger resolve absolutely. |
| Dev helpers | Existing `verify-poi.py` used for visits. Additional collector, cache decoder, production lookup and UI driver are contained under the server’s `dev-server/poi-verification-runs/`; production code/catalog not changed. |

Fresh RuneLite source commit is `42a6f17a6a2e8e478aa763890ecd0181a59dad38` dated2026-10-07. Native map index revision is1790689629. Full RuneLite cache loaders decoded62,522 object and16,577 NPC definitions with zero decode errors, including noninteractive objects; raw config/cache evidence is preserved. Historical `2021-spawns.csv` contains newer content, so its filename is not treated as a capture date and it supplies visit candidates only.

Direct original Wiki retrieval was blocked. Indexed official pages and revision-labelled independent mirrors retain their freshness limits. Local NPC spawns may share provenance with historical imports; their agreement is not a second independent live OSRS source. Ground items, map labels, map membership, nearby objects and missing server content likewise do not establish POI identity by themselves.

Original audits and canonical catalog were verified unchanged against the starting SHA256 manifest. Unrelated working changes were preserved. Evidence collection, headless lookup, factual correctness and observed rendering remain separate conclusions throughout.
