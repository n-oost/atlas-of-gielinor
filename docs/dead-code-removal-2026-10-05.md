# Dead code removal — 2026-10-05

Applied the [dead-code-removal skill](https://github.com/89jobrien/steve/blob/main/steve/skills/dead-code-removal/SKILL.md) after reading its source directly; it was not installed locally. The bundled Python import scanner does not cover Java, so detection used a Java source declaration/reference scan followed by manual inspection and repository searches. This is a conservative pass, not a proof that every remaining declaration is live.

## Scope and results

Scanned 146 production Java files. Searched source, tests, configuration, and resources for references. Removed 53 lines across nine production files: 14 unused imports, three unused private methods, and the commented call to one of those methods.

| File under `src/main/java/com/bettermap/` | Removal |
| --- | --- |
| `map/MonsterIndex.java` | Uncalled private `chunkKey` wrapper |
| `map/ShopIndex.java` | Uncalled private `chunkKey` wrapper |
| `ui/BetterWorldMapOverlay.java` | Uncalled private `drawUnmappedWarning`, its commented call, and three unused imports |
| `ui/MapStyle.java` | Six unused imports |
| `BetterMapPlugin.java` | Unused `ArrayList` import |
| `map/MonsterIconManager.java` | Unused `Font` import |
| `ui/BetterMapPanel.java` | Unused `Font` import |
| `ui/MapChromeRenderer.java` | Unused `InstanceMaps` import |
| `ui/input/MapLayerInputHandler.java` | Unused `OverlayCluster` import |

The chunk indexes already call `MapChunkKey` directly. The warning renderer had no active call; `unmappedPosition` remains used by the debug panel.

## Preserved candidates

- Public APIs, including methods with no direct repository references, per the skill's preservation rule.
- RuneLite event handlers, configuration methods, injected fields, lifecycle methods, and interface overrides.
- Lombok fields accessed through generated methods, such as `TooltipCard.preserveCompactLines` and the Finder orb offsets.
- Gson dataset fields, including shop percentages and monster map IDs.
- All test code and existing staged changes.

## Validation and backup

`./gradlew.bat compileJava compileTestJava --console=plain` succeeded. Gradle compiled production code and reported test compilation up to date. Unit tests were not run. The compiler reported deprecated API usage in `BetterMapPlugin.java`.

Compared each changed file against a snapshot taken immediately before cleanup: all cleanup differences were deletions. Existing pending edits were preserved; no files were staged or committed.

The nine original file snapshots and a change manifest are stored outside the repository at `<local temporary directory>/better-map-dead-code-20261005-130602`.

## Pending in-game confirmation

Launch with `./gradlew run` and follow [Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts) to log in to the development client. Check monster and shop markers while panning across map areas, open Finder and hover marker tooltips, and confirm the native map close button still works. If practical, check an unmapped location with the debug panel enabled; its coordinate diagnostics should remain available. No visible behavior change is intended. Compilation does not confirm in-game behavior.
