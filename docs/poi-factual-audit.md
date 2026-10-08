# POI factual audit

Generated 2026-10-08T22:43:49.453866+00:00

Scanned **8151 locations** across **128 categories**.

## Results

| Classification | Findings |
| --- | ---: |
| placement_candidate | 20 |
| coverage_gap | 605 |

Counts are findings, not unique incorrect POIs. Several findings can describe one location.

**0 rendered locations** have sourced or identity conflicts.

## Strongest findings

## How to continue the audit

The JSON retains every finding, reference and coordinate. Review sourced and identity conflicts first; then check placement candidates against Wiki or cache evidence. Coverage gaps are the final queue.

For an assistant audit pass:

1. Run the script and read the strongest findings in this report.
2. Group repeated findings by authored title and claim, including hidden duplicates without curated source lines.
3. Check each claim against the cited source and current Wiki/Jagex or cache evidence. Record confirmed, dismissed, or unresolved with a reason and source in a dated review document.
4. Expand the reference rules for new error classes, rerun, and retain the remaining candidates. Do not treat an empty finding list as proof of correctness.

Extend scripts/poi-audit-facts.json with reviewed, cited factual rules and rerun `python scripts/audit-poi-facts.py`. Broad match rules must be reviewed for scope before acceptance.

## Limits

The bundled reference is a pinned RuneLite snapshot, not a complete current game database. A creature absent from one nearby area can exist elsewhere nearby. Placement distances refer to area icons, not NPC spawns. Identity conflicts require review before changing runtime data.

Fishing comparisons expand short map labels using RuneLite FishingSpot catch families. This avoids calling a valid trout or swordfish description wrong because its map label says salmon or lobster.

Hunter identity/location/level claims, disjoint mining/fishing/tree resource identities, and named non-Hunter skill requirements receive semantic checks. Other categories receive provenance coverage screening; their methods, rewards and regions still require independent evidence. Unflagged records have not been certified correct. No catalog data was changed.
