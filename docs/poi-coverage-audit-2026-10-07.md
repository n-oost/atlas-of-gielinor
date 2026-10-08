# POI coverage audit — 2026-10-07

The STASH addition supplies 119 canonical identities, 121 rendered/searchable native locations, clue tiers, Construction requirements, emotes, required equipment, and a chest icon. Definitions come from the installed RuneLite 1.13.1 source archive. Filled status now comes from an optional STASH plugin's saved configuration: Emote Clue Items' per-RuneScape-profile filled string and object-ID fingerprint, or STASH Tracker's filled-unit list keyed by account hash. Missing or invalid data stays Unknown. An unlisted STASH Tracker unit also stays Unknown because positive membership does not establish emptiness. Emote Clue Items takes precedence when it provides a valid explicit value. Built status is queried on the client thread for the hovered unit, at most once per game tick. Markers and static requirements remain available without another plugin. Atlas no longer reads the chart, tracks chat, subscribes to STASH events, or writes filled state.

## Other findings

| Finding | Count | Meaning |
| --- | ---: | --- |
| Rendered POIs without an available bundled/generated icon | 0 | Fixed October 8: `thieving` uses the generated chest icon; `services` uses the bundled `task_master` icon. The audit follows these runtime mappings. |
| Generic catalog descriptions | 3,973 | These contain `No additional details available.` This is a description audit, not 3,973 missing markers: many are region labels or simple features, and shops/travel can receive runtime enrichment. |
| RuneLite snapshot locations without a related catalog icon within six tiles | 1 | River Kelda at (2835, 10112, 0) is represented by a region label rather than a dungeon icon. Review whether the snapshot's dungeon classification is useful before adding another marker. |

The five original missing icons affected Dognose Island's Rusty Pirate Chest, Ynysdail's Reinforced Pirate Chest, and trial masters Rum-dashed Ralph, Gurtob, and Gwyna. Their icon lookups now return images. The October 8 rerun found no missing icons; in-game rendering still requires user confirmation.

Useful description candidates include 162 rendered agility shortcuts, 167 rendered teleport markers, and 143 rendered transportation markers with generic catalog text. Construction of better descriptions should be based on game data and existing runtime enrichment; replacing generic text with guesses would not improve coverage.

The initial comparison flagged 24 dungeon locations. Treating `dungeon`, `basement`, and `dungeon_link` as related icons resolves 23 of them. All other bundled RuneLite world-map dataset rows have related catalog records within six tiles: agility courses, fairy rings, farming, fishing, hunter areas, minigames, mining, moorings, rare trees, runecrafting altars, salvage, teleports, and transportation. This checks nearby catalog representation, not physical placement accuracy, visibility at every zoom, or every object in the game. It is not a comparison with current upstream master.

## Reproduce

Run `python scripts/audit-poi-coverage.py` from `better-map`. Full findings are written to `docs/poi-coverage-audit.tsv`. Authoritative catalog rows are not changed by the audit.

Run `python scripts/import-stash-units.py <client-sources.jar>` to append missing RuneLite STASH records; existing rows remain intact. `poi/stash-units-LICENSE` retains the source license. The importer currently targets RuneLite 1.13.1.

Watson chart name aliases were corroborated against the primary implementation in [Emote Clue Items](https://github.com/larsvansoest/emote-clue-items/blob/master/src/main/java/com/larsvansoest/runelite/clueitems/data/StashUnit.java). These remain useful search aliases; the status adapter does not parse chart names. The saved-state formats were checked against Emote Clue Items' `progress/StashMonitor.java` and STASH Tracker's Plugin Hub revision `c32e51bbd53a0b2250a757c08a1a9e1db5c0487b`. No external plugin is required for static markers; filled state requires saved provider data. Existing Atlas filled-state keys are left untouched but are no longer read or updated.

## In-game confirmation

Search `STASH` in Finder and hover with expanded tooltips enabled. With provider data absent, confirm Filled: Unknown while static requirements remain visible. Record a filled unit in STASH Tracker or Emote Clue Items and hover again. With Emote Clue Items data, check both a filled and an unfilled unit; an unlisted STASH Tracker unit remains Unknown. Check the Elite and Master Warriors' Guild units separately. Switch accounts and confirm that another account's data is not reused.

Java compilation and all ten focused STASH/category tests pass after replacing the tracker with the read-only adapter. The wider 50-test catalog/tooltip run previously reported eight failures in existing dungeon, quest, mooring, and description tests. An isolated resource copy with all STASH records removed reproduced the same eight failures across 47 existing tests; authoritative workspace files were preserved. The importer was rerun and added no duplicate records. In-game behavior awaits user confirmation.
