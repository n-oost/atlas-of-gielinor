# POI agent handoff — 2026-10-08

## Current state

Planning and local catalog reconciliation only; no new game visits, independent source verification, or test runs were performed for this handoff.

Inputs: `poi-continuation-fixes-2026-10-08.{md,json}` and `poi-continuation-verification-2026-10-08.{md,json}` in this directory. Retain the detailed runtime ledger and its raw evidence alongside these summaries.

All five described text corrections are present in the current `catalog/pois.tsv`. Runtime confirmation after those corrections remains pending according to the fix ledger. Its environment snapshot is historical, not a current connection check. Existing working changes must be preserved.

Do not inherit these report conclusions without checking their evidence:

- A claim that a misplaced quest marker intentionally represents regional access or an entrance requires provenance. Nearby geography alone does not establish that role.
- Zero findings from limited audit rules does not establish full catalog correctness.
- File timestamps alone do not establish which resource bytes the running plugin loaded. Compare packaged resource hashes and establish the running build/restart before claiming runtime confirmation.
- Prior passing-test counts are reported results, not checks rerun during this handoff.

## Ownership and execution

### Preflight result

See `poi-agent-preflight-2026-10-08.json`. Local client PID 7500 is logged in at (2330, 3500, 0); the isolated-profile and loopback-server checks pass. Both staged and active-profile jars contain an older `pois.tsv`: all five corrected descriptions differ from current source. The other six catalog TSV files match their packaged bytes. Runtime correction checks are blocked until the corrected plugin is packaged, staged into the isolated profile, and the local client restarted. No teleport or restart was performed during preflight. This is a packaged-file check, not direct inspection of in-memory catalog data.

Coordinator owns this queue, reconciliation, and canonical catalog edits. Researcher owns independent source investigation and duplicate/role analysis. Operator exclusively owns shared local client movement, map controls, screenshots, and runtime observations. Reviewer checks proposed corrections and their evidence. Researchers and reviewers must not send client input.

Before operating, confirm the session is the local development server with a throwaway account and read the dev server's AGENTS.md and `dev-server/verify-poi.md`. This plan concerns that local environment. Keep production plugin restrictions separate from the external dev tooling.

Use the existing helper from `C:/dev/runelite-p-server-for-devving`:

```powershell
python .\dev-server\verify-poi.py --location-id <location_id>
```

The helper leaves the player at the destination. Confirm requested and actual X/Y/plane before accepting scene evidence. Its collection success is not a factual verdict or visual verification. Keep expected entity selectors specific to each destination; batch-wide expectations apply to every destination. Preserve blocked results; retry only after identifying and addressing a cause.

Each agent writes a per-task evidence file before marking progress or exiting. Include task ID, catalog location and identity IDs, field/claim, source and evidence paths, requested/actual coordinates where applicable, proposed correction, remaining gap, and disposition. Only the coordinator updates shared queue state. Use pending, assigned, evidence_ready, reviewed, applied, runtime_checked, or blocked; track factual disposition separately. Reassign unfinished tasks from their saved evidence if an agent exits.

## Initial ten-location queue

All tasks start pending. Text corrections below are already present; do not reapply them.

| Task | Location ID | Subject | Existing state | Next evidence needed |
|---|---|---|---|---|
| P01 | poi_005888 | Tai Bwo Wannai Trio | Fishing text corrected; placement open | Compare role/provenance with poi_003672; verify intended quest-start anchor and current tooltip |
| P02 | poi_005858 | Dragon Slayer I | Reward/start text corrected; placement open | Compare role/provenance with poi_001426; decide whether Edgeville record is a duplicate or separately justified stage marker |
| P03 | poi_005862 | Elemental Workshop I | Start/prerequisite text corrected; placement open | Compare with poi_001791 and independently identify bookcase anchor |
| P04 | poi_002112 | Lake Molch aerial fishing | Text corrected | Confirm corrected packaged data and visible compact/expanded tooltip |
| P05 | poi_007113 | Lake Molch search duplicate | Text corrected | Confirm corrected search result/details and alias behavior; hidden marker is not a visibility failure |
| P06 | poi_005778 | Moonlight antelope | Reference conflict | Compare poi_006365, native/display transform, and authored role; establish retain/merge/remove recommendation with evidence |
| P07 | poi_005871 | Regicide | Reference conflict | Independently verify difficulty; resolve marker role versus poi_003727 separately |
| P08 | poi_005883 | Making Friends with My Arm | Reference conflict | Independently verify difficulty/start subject; resolve marker role versus poi_004033 separately |
| P09 | poi_005772 | Ferret | Quest gate conflict | Obtain reliable evidence for partial versus full quest completion; local server gate alone is insufficient |
| P10 | poi_005815 | Gwyna | Placement unresolved | Establish boat NPC anchor; scoreboard proximity and missing local boat content are insufficient |

## Retained backlog and completion rules

Keep the continuation ledger's 13 fishing identity candidates and the runtime ledger's 13 blocked visits in the backlog. Preserve 3,973 generic-description candidates and 606 authored-location candidates as reported scope counts; these are not counts of confirmed errors and may overlap other queues. Do not replace the detailed ledgers with this pilot.

Accept a correction only for the field supported by evidence. A settled text correction can proceed while placement remains unresolved. Before removing or merging a duplicate, trace canonical identity, every location, aliases, tags, rendering and search behavior, and preserve any distinct supported role. Only the coordinator edits canonical resources after review.

Record source-catalog correction, packaged-resource match, runtime observation, and factual disposition separately. Missing dev-server entities never establish live OSRS absence. Finish the pilot when each task has a reviewed disposition and reproducible evidence or a precise blocker; unresolved tasks remain open for factual cleanup.
