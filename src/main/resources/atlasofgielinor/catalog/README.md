# Normalized POI catalog

Browse everything in `docs/map-catalog-audit.tsv` (generated; do not edit it independently).

Edit identities and descriptions in `pois.tsv`, native points in `locations.tsv`, and individual aliases/tags in `poi_aliases.tsv` / `poi_tags.tsv`. `places.tsv` holds default selections for area identities already defined in `pois.tsv`. Links and artwork retain separate tables. See `docs/map-catalog-schema-and-migration.md` for the full schema and unresolved migration limits.

Regenerate the view with `python scripts/export-map-catalog.py`. Review candidates with `python scripts/audit-poi-identities.py`. Neither command rewrites authoritative identities.
