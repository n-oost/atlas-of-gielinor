# POI zoom settings

Settings are in **Map layers: Zoom** and **Map layers: Label zoom**.
Zoom is measured in screen pixels per game tile. Lower values show markers farther out; 0 removes the zoom gate. The camera spans 0.14–24 px/tile. Decimal fields accept values such as 0.45; the existing monster-area and ground-item controls retain their integer values and saved keys.

## Category mapping

Agility shortcuts have their own toggle and threshold. Slayer masters follow **Skilling**, with their own threshold to preserve the original 1.0 px/tile default. Lookout points follow **Sailing Lookouts**, disabled by default because they previously did not draw. Sailing ports, player boats and port boards retain separate controls. Native mooring points and sea currents remain under Travel; native cargo bays and noticeboards remain under Tutors and Services.

All 12 POI categories are covered: Banks, Shops and Trade, Skilling, Agility Shortcuts, Sailing, Travel, Quests and Activities, Altars, Dungeons, Tutors and Services, Places, and Other. Places render as text rather than icon badges, with separate major, local and interior thresholds. Unidentified native icons follow Other.

## Marker and place thresholds

| Setting | Default (px/tile) | Config key |
| --- | ---: | --- |
| Agility Shortcuts | 0.20 | `shortcutMinZoom` |
| Skilling: Slayer Masters | 1.0 | `slayerMasterMinZoom` |
| Sailing Lookouts | 2.0 | `sailingLookoutMinZoom` |
| Banks | 1.0 | `bankMinZoom` |
| Shops and Trade | 1.5 | `shopMinZoom` |
| Skilling | 0.85 | `skillingMinZoom` |
| Travel | 0.2 | `travelMinZoom` |
| Quests and Tasks | 0.45 | `questMinZoom` |
| Minigames and Raids Lobbies | 0.6 | `activityMinZoom` |
| Altars | 1.2 | `altarMinZoom` |
| Dungeons | 1.0 | `dungeonMinZoom` |
| Tutors and Services | 2.0 | `serviceMinZoom` |
| Other Map Icons | 2.0 | `otherMinZoom` |
| Cities, Kingdoms and Islands | 0.45 | `majorPlaceMinZoom` |
| Districts and Landmarks | 3.0 | `localPlaceMinZoom` |
| Dungeon Room Names | 4.0 | `interiorPlaceMinZoom` |
| Bosses and Raid Entrances | 0.75 | `bossMinZoom` |
| Slayer Task Icons | 0.3 | `slayerTaskIconMinZoom` |
| Sailing Ports | 0.0 | `sailingPortMinZoom` |
| Port Notice Boards | 0.8 | `portNoticeBoardMinZoom` |
| Player Boats | 0.0 | `playerBoatMinZoom` |
| Other Plugin Markers | 0.0 | `pluginMarkerMinZoom` |
| Dungeon Navigation Buttons | 0.0 | `dungeonNavigationMinZoom` |
| Travel Network Stations | 0.0 | `travelStationMinZoom` |
| Monster Spawn Min Zoom | 7 | `monsterZoneMinZoom` |
| Ground Item Min Zoom | 9 | `groundItemMinZoom` |

## Label thresholds

| Setting | Default (px/tile) | Config key |
| --- | ---: | --- |
| Fairy Ring Codes | 1.0 | `fairyRingLabelMinZoom` |
| Agility Shortcut Levels | 1.0 | `shortcutLabelMinZoom` |
| Boss Names | 0.75 | `bossLabelMinZoom` |
| Monster Names | 0.85 | `monsterLabelMinZoom` |
| Slayer Task Names | 0.65 | `slayerTaskLabelMinZoom` |
| Ground Item Names | 14.0 | `groundItemLabelMinZoom` |
| Player Boat Names | 0.65 | `boatLabelMinZoom` |
| Dungeon Navigation Names | 1.5 | `dungeonNavigationLabelMinZoom` |

## Visibility rules

- Enabled toggles, current floors, dungeon focus, deleted POIs and overlap filtering still determine visibility. A zoom threshold does not force overlapping markers to draw.
- Active Slayer task bosses and task area overlays retain their zoom exceptions. Task icons and task names have separate thresholds.
- Hover still reveals dungeon button names at any zoom where the button is visible. Surface dungeon button names remain hover-only.
- Player position, active route lines, search flashes and open clue/raid panels remain navigation or active-task UI, rather than POI categories.

Clue markers come from RuneLite and use Other Plugin Markers. The Clue helper toggle controls only Go to Clue and the native hint card.

## In-game checks

1. Toggle Agility Shortcuts independently of Skilling and Travel; its default threshold is 0.20.
2. Toggle Skilling to show/hide Slayer masters, and change their threshold independently of other skilling icons.
3. Enable Sailing Lookouts, select the appropriate floor, and zoom across its threshold.
4. Change a category threshold above/below the current zoom. Hidden icons must lose hover cards; overlap filtering can still thin visible icons.
5. Use Hide all to verify the new categories, Other icons and plugin markers are included.
