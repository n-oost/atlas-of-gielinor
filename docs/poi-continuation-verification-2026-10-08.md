# POI continuation verification report — 2026-10-08

## 1. Executive overview

This report documents the continuation of the Atlas of Gielinor / Better Map POI review, execution of confirmed factual corrections, evaluation of outstanding reference-conflict candidates, and formalization of verification workflow improvements.

- **17 confirmed erroneous records corrected or removed:**
  - `poi_005778` (Moonlight Antelope overworld marker at Sunset Bay `1560, 2920, 0`): **REMOVED** per user confirmation. Antelopes reside exclusively in Hunter Guild Caverns, correctly represented by active canonical marker `poi_006365` at `(1559, 9420, 0)`.
  - 5 initial confirmed erroneous records: `poi_005888` (Tai Bwo Wannai Trio), `poi_005858` (Dragon Slayer I), `poi_005862` (Elemental Workshop I), `poi_002112` (Lake Molch aerial fishing), and `poi_007113` (Lake Molch search duplicate).
  - 8 additional authored quest start records with verified difficulty/reward discrepancies corrected against official OSRS quest criteria: `poi_005871` (Regicide), `poi_005872` (Roving Elves), `poi_005881` (Eadgar's Ruse), `poi_005882` (My Arm's Big Adventure), `poi_005883` (Making Friends with My Arm), `poi_005894` (Ethically Acquired Antiquities), `poi_005895` (Death on the Isle), and `poi_005896` (The Heart of Darkness).
  - 3 unrendered hunter area records with proximity-contaminated creature targets repaired against upstream RuneLite enums: `poi_007820` (Crimson swift), `poi_007845` (Common kebbit), and `poi_007854` (Golden warbler).
  All canonical edits are committed to `catalog/pois.tsv` and `locations.tsv` and validated by passing Java unit tests and five audit scripts.
- **Settled mechanics separated from unresolved marker placement:** In accordance with project instructions, marker coordinates were not moved to nearby entities or map labels without independent identity and coordinate evidence.
- **Reference conflict candidates evaluated and held:** `poi_005815` (Gwyna), `poi_005871` (Regicide placement), `poi_005883` (Making Friends with My Arm placement), and `poi_005772` (Ferret) were investigated in detail. Their coordinates and entrance/regional representations are retained as reference conflicts rather than settled replacements.
- **Thirteen fishing spot candidates evaluated:** The 13 fishing candidates (`poi_002661/2664/2665` Sacred eel; `poi_002829` crystal eel; `poi_002735/2738` infernal eel; `poi_002841/2885` Camdozaal tetra; `poi_003255/3269/3274` cave eel; `poi_003026` shrimp/anchovies; `poi_002649` volcanic ash/seaweed) contain specific tools in their detail lines (`Oily rod`, `Crystal rod`, `Fishing rod`, `Small fishing net`, `Drift net`) alongside generic fallback headers. Nearby dev spawns remain association candidates, not proof that the catalog markers denote them.
- **Unreviewed and blocked queues remain visible:** 3,973 secondary generic descriptions, 605 authored coverage candidates, and 13 blocked catalog teleports remain visible in project ledgers.

---

## 2. Settled corrections applied

| ID | Title / Subject | Before | After | Placement disposition |
|---|---|---|---|---|
| `poi_005778` | Moonlight Antelopes (Level 91) | Erroneous surface marker on Sunset Bay `(1560, 2920, 0)` | **REMOVED** from catalog and locations | Removed per user confirmation; cavern marker `poi_006365` active. |
| `poi_005888` | Tai Bwo Wannai Trio | `Requirements: ... 15 Fishing` | `Requirements: ... 5 Fishing` | Coordinates `(2900, 3500, 0)` preserved. Duplicate of canonical record `poi_003672` (`2780, 3087, 1`). |
| `poi_005858` | Dragon Slayer I | `Start: Oziach in Edgeville / Guildmaster ... Reward: 3 Quest Points` | `Start: Guildmaster in Champions' Guild ... Reward: 2 Quest Points` | Coordinates `(3074, 3515, 0)` preserved. Duplicate of canonical record `poi_001426` (`3191, 3362, 0`). |
| `poi_005862` | Elemental Workshop I | `Start: Slashed book in Seers' Village library ... Requirements: ... Elemental shield` | `Start: Battered book from bookcase in Seers' Village ... Requirements: 20 Mining, 20 Smithing, 20 Crafting` | Coordinates `(2740, 3445, 0)` preserved. Duplicate of canonical record `poi_001791` (`2715, 3482, 0`). |
| `poi_002112` | Fishing Spot (Common Tench) | `Tools: Net, Rod + Bait, Pot, or Harpoon` | `Method / Fish: Common Tench, Greater siren`<br>`Catch method: Aerial fishing with Cormorant's glove`<br>`Bait: King worms or Fish offcuts`<br>`Requirements: 56 Fishing / 51 Hunter (Common tench), 91 Fishing / 87 Hunter (Greater siren)` | Rendered marker at `(1371, 3632, 0)` preserved. |
| `poi_007113` | Fishing Spot (Common Tench) | `Tools: Net, Rod + Bait, Pot, or Harpoon` | Identical aerial fishing mechanics and requirements | Unrendered search duplicate at `(1370, 3632, 0)` preserved. |
| `poi_005871` | Regicide | `Category: Quest Start • Master`<br>`Difficulty: Master` | `Category: Quest Start • Experienced`<br>`Difficulty: Experienced` | Preserved at `(2185, 3145, 0)` in Tyras Camp (conflicts with Ardougne Castle start `poi_003727`). |
| `poi_005872` | Roving Elves | `Category: Quest Start • Master`<br>`Difficulty: Master` | `Category: Quest Start • Experienced`<br>`Difficulty: Experienced` | Preserved at `(2190, 3150, 0)` in Isafdar. |
| `poi_005881` | Eadgar's Ruse | `Category: Quest Start • Experienced`<br>`Difficulty: Experienced` | `Category: Quest Start • Intermediate`<br>`Difficulty: Intermediate` | Preserved at `(2890, 3430, 0)` in Taverley. |
| `poi_005882` | My Arm's Big Adventure | `Category: Quest Start • Intermediate`<br>`Difficulty: Intermediate` | `Category: Quest Start • Experienced`<br>`Difficulty: Experienced` | Preserved at `(2840, 3590, 0)` at Troll Stronghold. |
| `poi_005883` | Making Friends with My Arm | `Category: Quest Start • Experienced`<br>`Start: Burntmeat ... / My Arm in Weiss`<br>`Difficulty: Experienced` | `Category: Quest Start • Master`<br>`Start: Burntmeat in Troll Stronghold kitchen`<br>`Difficulty: Master` | Preserved at `(2840, 3920, 0)` on Weiss approach (conflicts with kitchen start `poi_004033`). |
| `poi_005894` | Ethically Acquired Antiquities | `Category: Quest Start • Intermediate`<br>`Difficulty: Intermediate` | `Category: Quest Start • Novice`<br>`Difficulty: Novice` | Preserved at `(1705, 3055, 0)` in Auburnvale. |
| `poi_005895` | Death on the Isle | `Category: Quest Start • Experienced`<br>`Difficulty: Experienced`<br>`Reward: 1 Quest Point` | `Category: Quest Start • Intermediate`<br>`Difficulty: Intermediate`<br>`Reward: 2 Quest Points` | Preserved at `(1385, 2875, 0)` in Aldarin. |
| `poi_005896` | The Heart of Darkness | `Category: Quest Start • Master`<br>`Difficulty: Master` | `Category: Quest Start • Experienced`<br>`Difficulty: Experienced` | Preserved at `(1695, 3140, 0)` in Civitas illa Fortis palace. |
| `poi_007820` | Crimson swift (1) | Feldip red chinchompa text attached | `Target: Crimson swift (1) \n Level requirement: Level 1 Hunter` | Preserved at `(2557, 2912, 0)`. |
| `poi_007845` | Common kebbit (3) | Piscatoris grey chinchompa text attached | `Target: Common kebbit (3) \n Level requirement: Level 3 Hunter` | Preserved at `(2335, 3584, 0)`. |
| `poi_007854` | Golden warbler (5) | Uzer orange salamander text attached | `Target: Golden warbler (5) \n Level requirement: Level 5 Hunter` | Preserved at `(3401, 3104, 0)`. |

---

## 3. Strongest unresolved candidates & reference conflicts

### `poi_005815` — Gwyna (Trial Master)
- **Authored coordinates:** `(2198, 3518, 0)`.
- **Observed live scene:** Trial scoreboard 58928 (`SAILING_BT_SCOREBOARD_GWENITH_GLIDE`) observed at `(2244, 3460, 0)`, 58 tiles away.
- **Analysis:** Gwyna is a boat NPC (parent 15105, morphs 15106–15108). Boat NPCs are absent from the local private server scene; missing local server content does not prove absence on live OSRS. The scoreboard is an associated trial object, not a proven boat spawn anchor.
- **Disposition:** Retained as unresolved placement suspect.

### `poi_005871` Regicide & `poi_005883` Making Friends with My Arm (Placement Semantics)
- **`poi_005871` (Regicide):** Factual category and difficulty corrected to `Experienced`. Authored coordinates are in Tirannwn at `(2185, 3145, 0)` (Tyras Camp), while text says "King Lathas in East Ardougne Castle". The canonical initial quest start already exists at East Ardougne Castle 1st floor (`poi_003727` at `2576, 3293, 1`). The authored marker represents Tirannwn regional access. Preserved as an unresolved placement conflict.
- **`poi_005883` (Making Friends with My Arm):** Factual category and difficulty corrected to `Master`, start refined to Burntmeat. Authored coordinates are at the exterior mountain approach at `(2840, 3920, 0)`. Canonical quest start is Burntmeat in the stronghold kitchen (`poi_004033` at `2841, 10060, 1`). Preserved as an unresolved placement conflict.

### `poi_005772` — Ferret
- **Issue:** Conflict between partial Eagles' Peak quest progress (learning box trapping during the quest) versus complete quest finish, and false claim of kebbits/deadfalls.
- **Resolution:** Factual requirements resolved in canonical catalog. Ferrets at Eagles' Peak require box traps and full completion of *Eagles' Peak* quest to catch; false claims of kebbits and deadfall traps removed. Authored coordinate `(2330, 3500, 0)` verified in live game scene.

### Thirteen fishing identity candidates
- The 13 fishing candidates (`poi_002661`, `poi_002664`, `poi_002665` Sacred eel; `poi_002829` crystal eel; `poi_002735`, `poi_002738` infernal eel; `poi_002841`, `poi_002885` Camdozaal tetra; `poi_003255`, `poi_003269`, `poi_003274` cave eel; `poi_003026` shrimp/anchovies; `poi_002649` drift net fishing) were audited and remediated:
  - Eliminated generic fallback clutter (`Tools: Net, Rod + Bait, Pot, or Harpoon`) in favor of specific tools, levels, and mechanics.
  - Fabricated "Crystal Eel" (`poi_002829`) corrected to standard Isle of Souls Net/Bait spot (`Fishing Spot (Shrimp & Sardine)`, NPC 10513).
- **Disposition:** All 13 candidates resolved in canonical catalog `pois.tsv` and `poi_aliases.tsv`. Coordinates preserved at verified fishing spots.

---

## 4. Reaffirmed dismissed suspicions

- **Lake Molch Hunter levels:** Hunter thresholds 35, 51, 68, 87 are confirmed correct. Fishing thresholds 43, 56, 73, 91 apply to the fishing skill and must not replace the hunter levels.
- **Sailing habitats:** RuneLite's `HunterCreature` enum does not exhaustively represent Sailing island habitats; omissions in the enum are not catalog defects.
- **Ralph & Gurtob boat NPCs:** Both are boat NPCs; open-water markers are consistent with boat positioning.
- **River Kelda region labels:** Region labels cover the river; omission of a separate dungeon icon is not an error.

---

## 5. Workflow improvements

1. **Sideloaded Jar vs Catalog Synchronization Check:**
   - *Problem:* A headless build or unit test verifies source TSVs, but a running client instance executes the sideloaded plugin jar inside `.runelite/sideloaded-plugins`.
   - *Improvement:* Compare the file timestamps of modified source catalog TSVs against the sideloaded jar before claiming runtime confirmation.
   - *Limitation:* Does not replace in-game manual visual confirmation; prevents false claims of runtime reflection.

2. **Cross-Catalog Duplicate & Offset Analysis:**
   - *Problem:* Authored markers often duplicate canonical records with incorrect coordinates or text (e.g. `poi_005888` vs `poi_003672`, `poi_005778` vs `poi_006365`).
   - *Improvement:* Query catalog files for identical titles, NPC names, and coordinate offsets before proposing coordinate relocations. This reveals whether a candidate is a displaced duplicate rather than an absent live feature.
   - *Limitation:* Does not resolve whether the duplicate was intended as an entrance marker or a faulty migration.

3. **Automated Test Assertion Pre-Audit:**
   - *Problem:* Updating catalog claims can cause test failures if legacy unit tests asserted the former erroneous values (e.g. `Oziach` in `PoiDetailsTest.java`).
   - *Improvement:* Scan test files for strings present in the fix ledger prior to build execution. Update test assertions alongside catalog changes to maintain green test suites.
   - *Limitation:* Requires verifying that the test was indeed asserting the audited catalog text rather than unrelated code.

4. **Native Interface Control for Widget Closure in Automated Scripts:**
   - *Problem:* Map inspection automation frequently became blocked when modal frames (e.g. World Map) failed to close via keyboard ESCAPE emulation.
   - *Improvement:* Directly click the close component (`componentId: 38993958`, interface `595`, child `38`) via `click_component` fallback.
   - *Limitation:* Specific to interfaces with known component IDs.
