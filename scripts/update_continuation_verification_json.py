import json

with open('docs/poi-continuation-fixes-2026-10-08.json', 'r', encoding='utf-8') as f:
    fixes_data = json.load(f)

with open('docs/poi-continuation-verification-2026-10-08.json', 'r', encoding='utf-8') as f:
    verif_data = json.load(f)

# Update settled_corrections to include all 83 location IDs
all_settled_ids = sorted(list({f['location_id'] for f in fixes_data['fixes']}))
verif_data['settled_corrections'] = all_settled_ids

# Update the thirteen fishing candidates
fishing_resolutions = {
    'poi_002661': 'resolved_sacred_eel_mechanics',
    'poi_002664': 'resolved_sacred_eel_mechanics',
    'poi_002665': 'resolved_sacred_eel_mechanics',
    'poi_002829': 'resolved_corrupt_crystal_eel_fixed_to_shrimp_sardine',
    'poi_002735': 'resolved_infernal_eel_mechanics',
    'poi_002738': 'resolved_infernal_eel_mechanics',
    'poi_002841': 'resolved_camdozaal_tetra_mechanics',
    'poi_002885': 'resolved_camdozaal_tetra_mechanics',
    'poi_003255': 'resolved_cave_eel_mechanics',
    'poi_003269': 'resolved_cave_eel_mechanics',
    'poi_003274': 'resolved_cave_eel_mechanics',
    'poi_003026': 'resolved_shrimp_anchovy_mechanics',
    'poi_002649': 'resolved_drift_net_mechanics'
}

for item in verif_data.get('thirteen_fishing_identity_candidates', []):
    pid = item['location_id']
    if pid in fishing_resolutions:
        item['disposition'] = 'resolved_in_canonical_catalog'
        item['resolution'] = fishing_resolutions[pid]

# Update ferret in reference_conflict_candidates
for cand in verif_data.get('reference_conflict_candidates', []):
    if cand['location_id'] == 'poi_005772':
        cand['status'] = 'resolved'
        cand['disposition'] = 'Corrected details to require box traps and Eagles\' Peak quest completion; removed false kebbits/deadfall claims.'

with open('docs/poi-continuation-verification-2026-10-08.json', 'w', encoding='utf-8') as f:
    json.dump(verif_data, f, indent=2)

print(f"Updated docs/poi-continuation-verification-2026-10-08.json with {len(all_settled_ids)} settled corrections.")
