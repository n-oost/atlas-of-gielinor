# Native services and unused code review — 2026-10-05

## Changes awaiting in-game confirmation

- Removed the Quick Finder minimap overlay, orb dragging and position settings, standalone Finder state, deferred click timer and lifecycle hooks, and launcher-only map buttons and destination-on-open callback. The in-map Finder remains available. Old stored launcher configuration values are left untouched; no surviving setting was renamed.
- Retained the native world-map orb bounds lookup in MapLayout for mouse passthrough while the replacement map is open. It remains necessary even after removing the plugin launcher. Native close bounds and menu preservation remain intact.
- Moved all 94 boss records from MonsterLocationData constructors into `src/main/resources/com/bettermap/data/bosses.json.gz` (4,073 bytes). Stable enum identifiers remain for raid grouping and authored dungeon positions. MapData loads the compressed catalog on the existing background worker before enabling map rendering/input. Sidebar boss enumeration waits for loading. A failed catalog load prevents map readiness instead of publishing partial data.
- Removed uncalled BossLocationIndex.forBoss and SlayerTaskTracker.getTaskTargets.
- Removed references to the debug renderer already deleted in the working tree, restoring compilation without restoring that renderer.
- Removed launcher-only test cases and preserved in-map behavior coverage. The earlier unused Finder text editor removal remains in place.

## Further removal candidates

These were reviewed but left for a later cleanup after the user prioritized removing the launcher.

1. `PoiDetails.compactSkillingLines` and `compactTeleportLines`: no source or test callers. Their private LEVEL_REQUIREMENT pattern and regex imports can go with them.
2. `PoiIndex.cacheFile` and `iconFile`: no source or test callers. These are obsolete file-path helpers; the live POI resource loading does not use them.
3. `BetterMapConfig.debugOverlay`, overlay `unmappedPosition`, and render statistics: deleting the debug renderer leaves its toggle and unmapped-coordinate state unused. Render statistics are still written by renderers, but their reporting consumer is gone. Remove this instrumentation together rather than leaving an ineffective toggle.
4. RuneLite replacements: GroundItemIndex stores static GE/alch values that could instead come from ItemManager; MonsterIconManager's scaling helper could use ImageUtil.resizeImage/resizeCanvas. These require deliberate behavior decisions about unloaded prices and image interpolation. Keep spawn locations and the cache of resized icons.

The broad declaration scan also found methods used only in tests. A declaration without a direct production reference is not by itself proof of dead code: RuneLite subscribers, interface callbacks, injected configuration, Lombok accessors and Gson fields must remain.

## Validation

- Every boss record, field and record order in the compressed catalog matches the original enum exactly.
- Production and all test sources compile.
- 91 targeted tests in 10 classes pass: MonsterIconManagerTest, RaidBossDisplayTest, MapFinderTest, MapFinderQueryTest, FinderKeyCaptureTest, MapCameraFinderTest, MapCameraTest, WorldMapInputFinderTest, FinderPanelReworkTest and DungeonNavigationTest.
- No remaining production/test references to the deleted QuickFinder overlay, orb state, standalone mode or launcher destination callback.
- Git diff whitespace checks pass. The existing PoiIndexTest working-copy line-ending warning is unrelated to this change.

## In-game checks

Launch with `./gradlew run`; follow https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts for development-client login. Confirm the plugin minimap launcher is absent, native map orb and close/Escape still work, in-map Finder supports typing and result selection/Route, and boss markers/tooltips plus all three raid galleries still appear. Test reopening the map and a long multi-word Finder query. Only the user can confirm these behaviors in-game.

## Dungeon entrance consolidation

Removed DungeonPoiOverrides and its two resources, buttons.tsv and deleted-pois.tsv. UndergroundZone now loads authored marker coordinates and visibility from entrance-markers.tsv during its existing background catalog load. The 26 rows preserve 24 adjusted placements and four hidden entrance markers; four redundant overrides and seven unused floor-deletion records were discarded. No raw-deletion records existed, so their always-false filtering branches were removed from POI indexing/rendering.

Original surface entrance tiles remain in zones.tsv and getSurfacePoints(), preserving navigation and the existing original-versus-moved duplicate filter. Marker consumers use getEntranceMarkerPoint() and isEntranceMarkerHidden(). Catalog publication remains atomic, so frame-time reads never open the marker resource.

Migration comparison preserved coordinates and hidden state for all 463 original entrances. All production/test sources compile; 137 tests pass across eight targeted classes: DungeonEntranceMarkerTest, UndergroundZoneTest, PoiIndexTest, PoiCoverageTest, BetterWorldMapOverlayPoiTest, DungeonNavigationTest, LayerTogglesTest and WorldMapInputDungeonExitTest.

In-game confirmation is pending: check the corrected Lumbridge cellar and Brimhaven entrances, the hidden Chasm of Tears and alternate Ectofuntus/Paterdomus/Port Sarim markers, Finder dungeon selection, and dungeon entry/return navigation. Check that moved entrances have no extra marker at their original placement.

## Shared dungeon toggle consolidation

Removed OverlayCluster, separate regional buttons and their tooltip/state/rendering branches. Dungeon membership is now bundled in dungeon-connections.tsv and published with UndergroundZone's background-loaded catalog. Individual entrances share one open/close implementation, revealing only their dataset group. Floor hover previews remain specific to one dungeon; explicit native-only roots retain individual controls.

Existing regional memberships were used as the connection groups. Overlapping rows were normalized in original priority order, with canonical aliases deduplicated, so each dungeon belongs to one toggle group. These are authored display groups, not a claim that every member is physically reachable from every other member.

Production and test sources compile. No new test run or in-game verification was performed for this change. In-game confirmation pending: hover/open Lumbridge cellar, check its grouped zones, toggle another group, close the Lumbridge group without closing the other, select a floor, and return to the surface using the map layer controls.
