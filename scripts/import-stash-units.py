"""Import RuneLite STASH definitions into the authoritative map catalog.

Usage: python scripts/import-stash-units.py path/to/client-sources.jar
Existing catalog rows are preserved. RuneLite's BSD license is retained.
"""
import csv
import json
import pathlib
import re
import sys
import textwrap
import zipfile

root = pathlib.Path(__file__).resolve().parents[1] / 'src/main/resources/atlasofgielinor'
with zipfile.ZipFile(sys.argv[1]) as jar:
    prefix = 'net/runelite/client/plugins/cluescrolls/clues/'
    source = jar.read(prefix + 'emote/STASHUnit.java').decode()
    clues = jar.read(prefix + 'EmoteClue.java').decode()
    emotes = jar.read(prefix + 'emote/Emote.java').decode()
emote_names = dict(re.findall(r'^\s*([A-Z_0-9]+)\("([^"]+)"', emotes, re.M))
units = re.findall(r'^\s*([A-Z_0-9]+)\(ObjectID\.([A-Z_0-9]+), (.*)\),?$', source, re.M)
clue_by_unit = {}
for line in clues.splitlines():
    match = re.search(r'new EmoteClue\(ItemID\.\w+, ("(?:\\.|[^"\\])*"), ("(?:\\.|[^"\\])*"), (\w+), new WorldPoint\s*\([^)]*\), (.*)', line)
    if match:
        clue_by_unit.setdefault(match[3], []).append((json.loads(match[1]), match[4]))

def append_rows(name, rows):
    path = root / 'catalog' / (name + '.tsv')
    with path.open(encoding='utf-8', newline='') as stream:
        old = list(csv.reader(stream, delimiter='\t'))
    existing = {tuple(row[:2]) if name in ('poi_aliases', 'poi_tags') else row[0] for row in old[1:]}
    fresh = [row for row in rows if (tuple(row[:2]) if name in ('poi_aliases', 'poi_tags') else row[0]) not in existing]
    with path.open('a', encoding='utf-8', newline='') as stream:
        csv.writer(stream, delimiter='\t', lineterminator='\n', quoting=csv.QUOTE_NONE, quotechar=None).writerows(fresh)
    print(name, 'added', len(fresh))

identities, locations, aliases, tags = [], [], [], []
levels = {'BEGINNER': 12, 'EASY': 27, 'MEDIUM': 42, 'HARD': 55, 'ELITE': 77, 'MASTER': 88}
# Watson names differ from RuneLite's enum names for these locations.
# Corroborated by emote-clue-items/data/StashUnit.java (larsvansoest/emote-clue-items).
chart_aliases = {
    'EMIRS_ARENA_TICKET_OFFICE': "Mubariz's room at the Emir's Arena",
    'OGRE_CAGE_IN_KING_LATHAS_TRAINING_CAMP': 'Ogre cage in the Ardougne Training Camp',
    'WEST_OF_THE_SHAYZIEN_COMBAT_RING': 'North of the Shayzien combat ring',
    'WESTERN_SALVAGER_OVERLOOK': 'West side of Salvager Overlook',
    'TEMPLE_SOUTHEAST_OF_THE_BAZAAR': 'Outside the temple in Civitas illa Fortis',
    'CAM_TORUM_ENTRANCE': 'South of the gates to Cam Torum',
    'FORTIS_GRAND_MUSEUM': 'Near the entrance of the Civitas illa Fortis Grand Museum',
    'GYPSY_TENT_ENTRANCE': "Aris's tent",
    'FINE_CLOTHES_ENTRANCE': 'Iffie Nitter in Varrock',
    'BOB_AXES_ENTRANCE': "Bob's Brilliant Axes in Lumbridge",
    'CRYSTALLINE_MAPLE_TREES': 'North of Prifddinas by several maple trees',
    'CHARCOAL_BURNERS': 'Near the Charcoal Burners',
    'EAST_OF_THE_LEVEL_19_WILDERNESS_OBELISK': 'Chaos Temple in the south-eastern Wilderness',
}
for name, object_name, points in units:
    tier = next(t for t in levels if object_name.startswith('HH_' + t))
    identity = 'stash_' + name.lower()
    label = re.sub(r'_\d{5}$', '', name).replace('_', ' ').strip().capitalize()
    title = 'STASH unit - ' + label
    details = ['Clue tier: ' + tier.title(), 'Requires Level ' + str(levels[tier]) + ' Construction']
    if name not in clue_by_unit:
        raise ValueError('No emote clue for ' + name)
    for clue_text, tail in clue_by_unit[name]:
        emote_list = [emote_names[token] for token in re.findall(r'\b[A-Z][A-Z_0-9]*\b', tail.split('item(')[0].split('any(')[0].split('all(')[0]) if token in emote_names]
        details.extend(textwrap.wrap('Emotes: ' + ', then '.join(emote_list), 76))
        gear = re.search(r'\b(?:Equip|Wear) (.*)', clue_text)
        details.extend(textwrap.wrap('Required items: ' + (gear[1] if gear else clue_text), 76))
        if 'Beware of' in clue_text:
            details.append('Beware of double agents.')
    ref = 'RuneLite 1.13.1 STASHUnit.' + name + '; EmoteClue'
    identities.append([identity, title, 'stash_unit', '', title, 'Clue scrolls', '\\n'.join(dict.fromkeys(details)), 'verified', ref])
    for index, point in enumerate(re.findall(r'new WorldPoint\((\d+), (\d+), (\d+)\)', points)):
        locations.append([identity + ('_alternate' if index else ''), identity, *point, 'poi', 'stash_unit', 'true', 'true', 'verified', ref])
    aliases.extend([[identity, 'STASH'], [identity, tier.title() + ' STASH']])
    if name in chart_aliases:
        aliases.append([identity, chart_aliases[name]])
    tags.extend([[identity, 'stash'], [identity, 'clue_scrolls'], [identity, tier.lower()]])
for name, rows in [('pois', identities), ('locations', locations), ('poi_aliases', aliases), ('poi_tags', tags)]:
    append_rows(name, rows)
(root / 'poi/stash-units-LICENSE').write_text('\n\n'.join(
    value[:value.index('package ')].strip() for value in (source, clues, emotes)) + '\n', encoding='utf-8')
