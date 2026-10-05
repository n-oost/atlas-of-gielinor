# Plugin Hub approval feasibility — Atlas of Gielinor

Reviewed: 2026-10-04. HEAD: `1164f0314bb51c8f62a6ace62aad1aa0aa58604a`, plus the current working tree.

**Verdict: feasible after specific fixes. The current HEAD is not ready for submission.** There is a confirmed missing production source file. The production feature paths inspected do not establish a categorical rejection of the map/navigation concept. Widget visibility and asset provenance need attention before claiming readiness. Final approval remains RuneLite maintainers' decision.

## Follow-up fixes — 2026-10-04

F1: the five required production inputs are included in the follow-up cleanup commit.
The original reviewed HEAD above remains the historical baseline; the submission and
re-audit must point to the cleanup commit or a later revision containing these inputs.

F4: the Finder handler now cancels its pending timer on shutdown, rejects stale queued timer
callbacks by generation, and rejects new deferred clicks while stopped. Timer stopping is
dispatched to Swing when necessary. Plugin shutdown clears deferred map selection, closes the
Finder card, and resets input focus/drag state. Startup enables the handler for the new session.

F5: README scope/search descriptions and Shortest Path destination-sharing notices now
match production behavior. A RuneLite metadata notice records its embedded licenses and source
revision. This does not resolve F3's remaining Wiki/image provenance questions.

F2: forced overview unhiding has been removed. Visibility restoration now checks widget identity
and whether Atlas changed that widget from visible to hidden. Components already self-hidden
when encountered are not claimed; recreated widgets invalidate old ownership. Native close/input
behavior is preserved. If another script hides an already Atlas-hidden widget, the shared hidden
boolean cannot reveal that second owner; coexistence and interface lifecycle still need runtime
verification. The concrete unconditional-unhiding concern is addressed in source.

F3: bundled asset notices, dataset source credits, and an asset provenance inventory have been
added. Further investigation found that the publisher itself records incomplete tile provenance;
the ground-item source also names osrsreboxed-db without a pinned imported revision. F3 remains
unresolved until original tile manifests/dataset imports establish sources and applicable terms.
See [asset provenance](asset-provenance.md).

The maintainer subsequently identified `Old_School_RuneScape_world_map.png` on the OSRS Wiki
as the likely original overworld image. Its exact URL and corresponding file-page link are
now recorded in the bundled notices and provenance inventory. This narrows the overworld
source question; tile matching, media terms, and underground origins remain unverified.

The maintainer then supplied the Wiki file-page notice identifying the image as non-free
Jagex content displayed under fair use, sourced from Jagex's official world map. The official
source was retrieved successfully and its current redirect recorded. The specific notice
and original source are now documented; the Wiki's fair-use statement does not itself license
Atlas's redistribution. F3 now has concrete overworld attribution/media classification,
with tile matching, Atlas's redistribution basis, and underground origins still unresolved.

No build, unit tests, or runtime checks were run for these follow-up changes. Findings below
retain the original review evidence and historical line numbers.

## Scope and evidence limits

Inventoried and scanned all 143 production Java files (50,335 lines), then read and traced the relevant input, menu, widget, networking, asset, routing, marker, and lifecycle implementations. Reviewed metadata, README, notices, and production resource tracking. This was a static feasibility review, not a complete line-by-line correctness audit or security certification.

Excluded dev-server, OpenRune, RSProx, MCP, patched client repositories, launch/sideload scripts, test fixtures, dev diagnostics, and dungeon tuning. The `File`/`Files` override branches in PoiIndex and DungeonPieceIndex are not reported as shipping filesystem violations: the production startup calls inspected pass `null` and read bundled streams.

No Gradle build, unit tests, client launch, gameplay checks, PR submission, or production code edits were performed. Existing untracked work was preserved. Static resource inspection parsed monsters, shops, and ground-item gzip JSON; checked PNG signatures and IHDR dimensions; and compared production references with Git tracking. These checks do not verify archive installation, decoding, runtime performance, or compatibility with the Hub's pinned client version.

## Findings

### F1 — High: submitted HEAD omits a required production class

**Classification:** confirmed submission blocker. **Confidence:** high.

`src/main/java/com/bettermap/map/PoiIndex.java:692` uses `WorldMapSupplement.Entry`; later lines call its static helpers. PoiDetails also references the class. These references exist in HEAD, but `src/main/java/com/bettermap/map/WorldMapSupplement.java:19` is untracked. Consequently a clean checkout of this commit cannot resolve the class even though it exists locally. This conclusion comes from Git/source inspection; a compiler was not run.

Four related runtime resources are also untracked: `dungeons/native-overlays.tsv`, `dungeons/native-pieces.tsv`, `poi/pois-native-labels.tsv`, and `poi/runelite-world-map-data.json` beneath `src/main/resources/com/bettermap/`. They are referenced by production loaders. Their absence can omit map data even after the Java compile issue is fixed.

**Minimal remedy:** review and include the Java file and all intended runtime resources in the submission commit, or remove their production references. Commit only the intended shipping inputs, then build the exact commit against the Hub client version when verification is requested. Hub submissions identify a repository and exact commit, rather than the local working tree. [Plugin Hub submission instructions](https://github.com/runelite/plugin-hub/blob/master/README.md#submitting-a-plugin)

### F2 — High review concern: native widget visibility is forcibly restored

**Classification:** confirmed behavior; policy applicability requires reviewer judgment. **Confidence:** high about code, medium about rejection risk.

`src/main/java/com/bettermap/BetterMapPlugin.java:424` and `:425` explicitly call `hideWidget(..., false)` for the world-map overview container/display. `hideWidget` changes `isSelfHidden()` through `setHidden`. The default `hideGameMapRender` is true, so onBeforeRender repeats this visibility enforcement while the relevant widgets exist. Shutdown similarly forces map display and chatbox visibility through `setGameMapHidden(false)` and `setChatHidden(false)` at `:321`.

If native scripts or another plugin intentionally hide a component, this code can show it again without demonstrating that Atlas owned the hide. The overview is the world-map overview, not the HUD minimap; no Barrows minimap bypass was established. Nevertheless Jagex explicitly restricts unhiding hidden interface components, so the unconditional restoration deserves review. [Jagex interface restrictions](https://secure.runescape.com/m=news/third-party-client-guidelines?oldschool=1)

**Minimal remedy:** preserve native overview visibility; restore only visibility changes owned by this plugin, accounting for interface recreation and competing scripts. Verify open/close, disable/re-enable, fullscreen, and native-hidden states before submission. Do not replace the native close button, synthesize input, or restore refused menu-action implementations.

### F3 — Medium: Wiki-derived content provenance is not documented sufficiently

**Classification:** provenance question, not a proven license violation. **Confidence:** high about missing local documentation; unresolved about specific asset terms.

`README.md:14` identifies OSRS Wiki-derived data and imagery. `THIRD-PARTY-NOTICES.md:3` documents the bundled Shortest Path code/data, but provides no corresponding Wiki asset/data notice, source revision, applicable terms, or attribution/change record. No Wiki-specific licensing notice was found in the inspected text resources. The imported RuneLite JSON does embed original source license headers; that is favorable evidence and is not the missing notice described here.

The asset channel is publicly reachable and currently describes a 49,611,981-byte compressed pack containing 9,337 files. Fetching an assumed root LICENSE URL returned 404; that does not prove the entire assets repository has no licensing documentation. The Wiki copyright page could not be retrieved, so this review does not assign a particular license to every image or claim BSD must cover all content. [Current asset channel](https://raw.githubusercontent.com/n-oost/better-map-assets/main/channels/tiles-v1.json)

**Minimal remedy:** inventory bundled and downloaded content origins, document the applicable rights/terms and required attribution for each category, preserve existing upstream notices, and explain modifications. Distinguish original artwork, Jagex assets, Wiki-authored text, and factual datasets. This is a readiness/documentation gap rather than a legal conclusion.

### F4 — Medium: delayed Finder callback survives plugin shutdown

**Classification:** confirmed lifecycle defect by static tracing; not a categorical policy ban. **Confidence:** high.

`src/main/java/com/bettermap/ui/input/MapLayerInputHandler.java:445` starts a Swing Timer after a Quick Finder click. Its callback checks Finder state and query, then calls `openMapAt`. Cancellation exists only in the handler's private `cancelPendingQuickFinderClick` at `:462`, used for subsequent click handling. BetterMapPlugin.shutDown unregisters listeners and closes the input but does not cancel this timer or invalidate its callback. WorldMapInput.focusLost clears hover/focus state but also does not cancel it.

Click a standalone Finder row, then disable Atlas before the double-click delay expires: the Finder panel flags can still satisfy the callback. `BetterMapPlugin.java:804` can then set a deferred map-focus callback and enqueue an Atlas chat message after shutdown. Re-enabling can also retain that deferred selection. This is a small one-shot lifecycle leak, not a perpetual worker leak or automatic server action.

**Minimal remedy:** expose handler/input shutdown cleanup, stop the pending timer on its owning Swing thread, invalidate queued callbacks with an activation generation, and clear deferred Finder map-focus state. Check disable/re-enable during the click delay when runtime verification is requested.

### F5 — Low: user documentation contradicts shipping behavior

**Classification:** confirmed documentation defect. **Confidence:** high.

`README.md:130` says the plugin does not touch the HUD minimap or other components, but BetterMapPlugin registers RouteMinimapOverlay and a Quick Finder orb, and hides the chatbox while requested by the map UI. Drawing on the minimap is not itself a forbidden modification; the problem is the inaccurate scope claim. `README.md:134` describes KeyManager search input, while `RuneliteChatboxInputHost.java:58` uses ChatboxPanelManager. `THIRD-PARTY-NOTICES.md:17` says Atlas routing mode does not send destinations externally, but `ShortestPathTracker.java:209` calls `postTargetIfAvailable` in either enabled routing mode; the README describes the latter behavior correctly.

**Minimal remedy:** describe actual overlays, chatbox search, visibility behavior, and destination sharing consistently. This improves reviewer understanding and avoids overstating what the plugin leaves untouched.

## Favorable approval evidence

1. **Native actions remain native.** BetterMapPlugin appends `MenuAction.RUNELITE` entries without resetting or pruning gameplay entries. Their traced callbacks pan, select, calculate, or share local route destinations. `openMapAt` tells the user to click the native orb; no automatic walking or server action was established. The overlay preserves native close bounds and passes actual user clicks through.
2. **Asset downloads have explicit consent and validation.** BetterMapConfig.downloadMapAssets defaults to false and has the third-party IP warning. MapAssetManager uses injected OkHttp/Gson, `enqueue`, Filepath, commit/hash/path validation, finite archive/expanded limits, a narrow PNG/index/JSON allowlist, and 256x256 tile checks. Downloads install data, not executable code. Hashes supplied by the channel verify consistency, not independence from the channel owner's trust.
3. **Restricted behavior scans found no invoked reflection, process execution, JNI/JNA/Unsafe, dynamic executable-code loading, input injection, HTTP player-data server, or outgoing chat rewriting in the reviewed production paths.** `getClass().getName()` checks classify plugin/marker ownership; search text belongs to a plugin input dialog; local GAMEMESSAGE output is not autotyping. These are favorable scan results, not an exhaustive proof of absence.
4. **Routes and boss content are primarily map/reference features.** The bundled route engine has a pinned upstream revision and bundled BSD notice. RoutePlanner runs heavy searches on workers and uses request-generation guards. Boss cards use static locations/reference details; no live attack prediction, prayer recommendation, projectile landing, or boss-mechanic timer was established. Generic raid reference cards alone do not establish a forbidden boss helper, though maintainers still assess their actual scope. [RuneLite rejected features](https://github.com/runelite/runelite/wiki/Rejected-or-Rolled-Back-Features)
5. **Packaging/resource basics are favorable.** Metadata selects `build=standard`; Java compilation targets 11 and no additional runtime dependencies were declared. Resources are generally loaded with JAR streams. Inspected resource PNGs have valid signatures and none exceed 256x256. Under standard mode local build.gradle/settings.gradle are replaced, so dev install tasks, mavenLocal, shadowJar, and the leftover local `com.example` Gradle group are not presented as production rejection blockers. [Plugin Hub build types and resource handling](https://github.com/runelite/plugin-hub/blob/master/README.md)

## Reviewer questions and remaining checks

The WorldMapPointReader snapshot uses a side-effecting predicate returning false in WorldMapPointManager.removeIf. It preserves the manager but relies on an unconventional API pattern; ask whether maintainers prefer a supported accessor. This is not established forbidden reflection.

Confirm visibility ownership and the generic static boss-card scope with maintainers if those behaviors remain ambiguous after cleanup. Local AGENTS.md contains links to historical reviewer comments on close actions and thread interruption; those comment pages were not independently retrieved successfully, so this report preserves the local guardrails without presenting their precise wording as verified current policy.

A clean build against the Hub's pinned API, standard JAR inspection, runtime lifecycle/input checks, and profiling remain unperformed. The full external map archive was not downloaded or audited. No numerical approval probability is justified.

## Smallest path to readiness

1. Include the missing production Java/resource inputs in the intended submission commit.
2. Remove unconditional native unhiding and restore only plugin-owned visibility changes.
3. Document asset provenance and align the README/notices with actual behavior.
4. Cancel/invalidate Finder delayed work and deferred selection on shutdown.
5. Verify the exact submission commit with the Hub client/build configuration and the isolated local client before creating or updating the Hub PR.

Next action: inspect F1's five untracked runtime files and choose which belong in the submission commit.
