# Plugin Hub approval feasibility prompt — v1

Review Atlas of Gielinor for RuneLite Plugin Hub PR approval feasibility. Perform the review now; do not merely rewrite this prompt.

## Scope

Review production Java, runtime resources, plugin metadata, licensing, and user documentation in this plugin repository. Exclude the surrounding dev-server workspace, OpenRune, RSProx, patched clients, MCP tools, sideload launchers, test fixtures, build outputs, development scripts, diagnostics, and dungeon tuning. Do not flag dev-only behavior unless you trace it to a normal production entry point or show that it ships with an approval-relevant effect. Distinguish the working tree from the exact Git commit a Hub submission would build.

## Method

1. Read applicable AGENTS.md instructions. Inventory production sources and resources; record HEAD and untracked production inputs. Browse current primary sources: Plugin Hub README, RuneLite's rejected features, Jagex client guidelines, and relevant official API or reviewer comments. Distinguish published rules, local project instructions, and your own inferences. A failed source lookup does not establish a rule.
2. Scan all production Java for restricted behavior, then trace matches and their callers: reflection, native code, processes, dynamic code loading, serialization, synthetic input, server actions, menu removal/reordering, widget visibility/click zones, chat modification, networking, player-data exposure, and filesystem access. Account for the Hub build mode; standard mode replaces local Gradle files.
3. Trace plugin lifecycle, asynchronous callbacks, shutdown cleanup, client-thread ownership, render costs, downloaded asset validation, opt-in settings, and resource handling inside JARs. Inspect bundled datasets and notices. Separate static map/reference information from live boss assistance and combat recommendations.
4. Produce evidence-backed findings with severity, confidence, file:line references, a concrete trigger, consequence, minimal remedy, and supporting primary source where policy is involved. Classify each as a confirmed submission blocker, a confirmed defect, or a reviewer/provenance question. Do not treat getClass().getName() as forbidden reflective access, a local GAMEMESSAGE as autotyping, a plugin search dialog as outgoing chat, static imagery as downloaded code, or a route calculation as automatic walking.
5. Save a dated report in docs with the verdict, prioritized findings, favorable evidence, source links, coverage, and limitations. State exactly which builds/tests/runtime checks were actually performed. Do not modify production code, publish, submit a PR, or connect to the live game. Do not run builds or tests unless separately requested. Do not invent approval probabilities or guarantee approval.

## Output

Choose one verdict: feasible as reviewed; feasible after specific fixes; materially uncertain pending reviewer clarification; unsuitable under a cited rule. Give the strongest confirmed issue first, list the smallest path to readiness, and finish with one concrete next action. Keep the chat summary short; retain the full evidence in the report.

## Interpretation checks

Check the review against these cases: an untracked Java class referenced by committed production code is a submission blocker; unused dev disk overrides are outside scope; opt-in PNG downloads are not runtime code downloads; static boss locations alone do not establish forbidden fight prediction; a plugin search dialog is not automatically chat autotyping. Report these as reasoning checks, not measured model accuracy or executed tests.
