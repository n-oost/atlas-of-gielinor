# POI continuation audit fixes — 2026-10-08

Following the October 8 runtime verification audit and subsequent reviews, 85 catalog records were corrected or removed in canonical data (`src/main/resources/atlasofgielinor/catalog/pois.tsv` and `locations.tsv`).

## Summary of corrected and removed records

| ID | Subject / Title | Confirmed factual error / Audit Queue | Corrected values | Coordinates & Placement status | Evidence references |
|---|---|---|---|---|---|
| `poi_005778` | Moonlight Antelopes (Level 91) | Erroneous surface marker on Sunset Bay; Moonlight antelopes only exist underground in Hunter Guild Caverns (`poi_006365`) | **REMOVED** from catalog and locations per user confirmation | Removed from `(1560, 2920, 0)` | Live server visit to Hunter Guild Caverns `(1559, 9420, 0)` observing 4 antelopes; canonical rendered marker `poi_006365` active |
| `poi_005888` | Tai Bwo Wannai Trio | Fishing requirement listed as `15 Fishing` | Corrected to **5 Fishing** | `(2900, 3500, 0)` preserved as unresolved placement conflict (conflicts with Timfraku spawn `(2780, 3087, 1)` / `poi_003672`) | [Wiki rev 14918129](https://oldschool.runescape.wiki/w/Tai_Bwo_Wannai_Trio?oldid=14918129), [Timfraku snapshot](https://danielpgleason.com/osrs/reference/timfraku-4a828428/) |
| `poi_005858` | Dragon Slayer I | Reward listed as `3 Quest Points`; Oziach listed as initial start alternative | Corrected to **2 Quest Points**; start set to **Guildmaster in Champions' Guild** | `(3074, 3515, 0)` preserved as unresolved placement conflict (conflicts with Champions' Guild `(3191, 3362, 0)` / `poi_001426`) | [Wiki rev 14917676](https://oldschool.runescape.wiki/w/Dragon_Slayer_I?oldid=14917676) |
| `poi_005862` | Elemental Workshop I | Start listed as `Slashed book in Seers' Village library`; `Elemental shield` listed as prerequisite | Corrected start to **Battered book from bookcase in Seers' Village**; removed `Elemental shield` prerequisite | `(2740, 3445, 0)` preserved as unresolved placement conflict (conflicts with bookcase `(2715, 3482, 0)` / `poi_001791`) | [Bookcase Wiki](https://oldschool.runescape.wiki/w/Bookcase_(Elemental_Workshop_I)), [Elemental Workshop I rev 15292271](https://oldschool.runescape.wiki/w/Elemental_Workshop_I?oldid=15292271) |
| `poi_002112` | Fishing Spot (Common Tench) | Listed generic tools: `Net, Rod + Bait, Pot, or Harpoon` | Corrected to **Aerial fishing with Cormorant's glove**, bait (king worms / fish offcuts), and **56 Fishing / 51 Hunter (Common tench), 91 Fishing / 87 Hunter (Greater siren)** | `(1371, 3632, 0)` rendered marker preserved | [Lake Molch rev 15333224](https://oldschool.runescape.wiki/w/Lake_Molch?oldid=15333224), [Lake Molch snapshot](https://danielpgleason.com/osrs/reference/lake-molch-d4241815/) |
| `poi_007113` | Fishing Spot (Common Tench) | Same generic tools on unrendered searchable duplicate | Applied identical aerial fishing correction | `(1370, 3632, 0)` unrendered search duplicate preserved | Same Lake Molch sources |
| `poi_005871` | Regicide | Category and difficulty listed as `Master` | Corrected category & difficulty to **Experienced** | `(2185, 3145, 0)` preserved at Tyras Camp (conflicts with King Lathas start at Ardougne Castle) | Official OSRS quest log / `quest-details.tsv` |
| `poi_005872` | Roving Elves | Category and difficulty listed as `Master` | Corrected category & difficulty to **Experienced** | `(2190, 3150, 0)` preserved in Isafdar | Official OSRS quest log / `quest-details.tsv` |
| `poi_005881` | Eadgar's Ruse | Category and difficulty listed as `Experienced` | Corrected category & difficulty to **Intermediate** | `(2890, 3430, 0)` preserved in Taverley | Official OSRS quest log / `quest-details.tsv` |
| `poi_005882` | My Arm's Big Adventure | Category and difficulty listed as `Intermediate` | Corrected category & difficulty to **Experienced** | `(2840, 3590, 0)` preserved at Troll Stronghold | Official OSRS quest log / `quest-details.tsv` |
| `poi_005883` | Making Friends with My Arm | Category & difficulty listed as `Experienced`; start ambiguously included `My Arm in Weiss` | Corrected category & difficulty to **Master**; start set strictly to **Burntmeat in Troll Stronghold kitchen** | `(2840, 3920, 0)` preserved on Weiss approach | Official OSRS quest log / `quest-details.tsv` |
| `poi_005894` | Ethically Acquired Antiquities | Category and difficulty listed as `Intermediate` | Corrected category & difficulty to **Novice** | `(1705, 3055, 0)` preserved in Auburnvale / Hunter Guild | Official OSRS quest log / `quest-details.tsv` |
| `poi_005895` | Death on the Isle | Category & difficulty listed as `Experienced`; reward listed as `1 Quest Point` | Corrected category & difficulty to **Intermediate**; reward corrected to **2 Quest Points** | `(1385, 2875, 0)` preserved in Aldarin | Official OSRS quest log / `quest-details.tsv` |
| `poi_005896` | The Heart of Darkness | Category and difficulty listed as `Master` | Corrected category & difficulty to **Experienced** | `(1695, 3140, 0)` preserved in Civitas illa Fortis palace | Official OSRS quest log / `quest-details.tsv` |
| `poi_007820` | Crimson swift (1) | Proximity contamination attached Feldip Hills red chinchompa text to crimson swift marker | Corrected title to **Hunter area (Crimson swift)** and details to **Target: Crimson swift (1) \n Level requirement: Level 1 Hunter** | `(2557, 2912, 0)` unrendered search marker preserved | RuneLite `HunterAreaLocation.java` L47 |
| `poi_007845` | Common kebbit (3) | Proximity contamination attached Piscatoris grey chinchompa text to common kebbit marker | Corrected title to **Hunter area (Common kebbit)** and details to **Target: Common kebbit (3) \n Level requirement: Level 3 Hunter** | `(2335, 3584, 0)` unrendered search marker preserved | RuneLite `HunterAreaLocation.java` L80 |
| `poi_007854` | Golden warbler (5) | Proximity contamination attached Uzer orange salamander text to golden warbler marker | Corrected title to **Hunter area (Golden warbler)** and details to **Target: Golden warbler (5) \n Level requirement: Level 5 Hunter** | `(3401, 3104, 0)` unrendered search marker preserved | RuneLite `HunterAreaLocation.java` L93 |
| `poi_005808` | Anglers' Retreat - Ruby Harvests (Level 15) | Generic placeholder description `Sailing-island hunter creatures` | Corrected title to **Anglers' Retreat - Ruby Harvests (Level 15)** and details to **Target: Ruby harvest butterfly \n Level requirement: Level 15 Hunter \n Catch method: Butterfly net & Butterfly jar (or barehanded at 75)** | `(2467, 2722, 0)` rendered marker preserved | Live server visit confirmed NPC 5556 Ruby harvest at `2470, 2715` |
| `poi_005810` | Brittle Isle - Snowy Knights (Level 35) | Generic placeholder description `Sailing-island hunter creatures` | Corrected title to **Brittle Isle - Snowy Knights (Level 35)** and details to **Target: Snowy knight butterfly \n Level requirement: Level 35 Hunter \n Catch method: Butterfly net & Butterfly jar (or barehanded at 85)** | `(1954, 4057, 0)` rendered marker preserved | Live server visit confirmed NPC 5554 Snowy knight at `1956, 4058` |
| `poi_005791` | Dognose Island - Crimson Swifts (Level 1) | Generic placeholder description `Sailing-island hunter creatures` | Corrected title to **Dognose Island - Crimson Swifts (Level 1)** and details to **Target: Crimson swift (Red feathers) \n Level requirement: Level 1 Hunter \n Catch method: Bird snare** | `(3061, 2639, 0)` rendered marker preserved | Live server visit confirmed NPC 5549 Crimson swift at `3050, 2647` |
| `poi_006968` | Shimmering Atoll - Black Warlocks (Level 45) | Generic placeholder description `Sailing-island hunter creatures` | Corrected title to **Shimmering Atoll - Black Warlocks (Level 45)** and details to **Target: Black warlock butterfly (Strength boost) \n Level requirement: Level 45 Hunter \n Catch method: Butterfly net & Butterfly jar (or barehanded at 85)** | `(1558, 2771, 0)` rendered marker preserved | Live server visit confirmed NPC 5553 Black warlock at `1565, 2775` |
| `poi_002604` | Hunter training | Generic placeholder description `Sailing-island hunter creatures` | Corrected details to **Target: Snowy knight butterfly \n Level requirement: Level 35 Hunter \n Catch method: Butterfly net & Butterfly jar** | `(1956, 4064, 0)` duplicate marker preserved | Live server visit confirmed NPC 5554 Snowy knight at `1956, 4058` |
| `poi_002649` | Fishing spot | Generic fallback clutter 'Tools: Net, Rod + Bait, Pot, or Harpoon' | **Drift Net Fishing**: Level 44+ Fishing, 47+ Hunter; Drift nets & diving gear on Fossil Island | (3042, 4510, 0) verified catalog location | [Drift_net_fishing](https://oldschool.runescape.wiki/w/Drift_net_fishing) |
| `poi_002661` | Fishing spot | Generic fallback clutter 'Tools: Net, Rod + Bait, Pot, or Harpoon' | **Sacred eel**: Level 87 Fishing, Fishing rod & bait; dissecting with knife yields Zulrah's scales | (2680, 4702, 0) verified catalog location | [Sacred_eel](https://oldschool.runescape.wiki/w/Sacred_eel) |
| `poi_002664` | Fishing spot | Generic fallback clutter 'Tools: Net, Rod + Bait, Pot, or Harpoon' | **Sacred eel**: Level 87 Fishing, Fishing rod & bait; dissecting with knife yields Zulrah's scales | (2674, 4708, 0) verified catalog location | [Sacred_eel](https://oldschool.runescape.wiki/w/Sacred_eel) |
| `poi_002665` | Fishing spot | Generic fallback clutter 'Tools: Net, Rod + Bait, Pot, or Harpoon' | **Sacred eel**: Level 87 Fishing, Fishing rod & bait; dissecting with knife yields Zulrah's scales | (2669, 4710, 0) verified catalog location | [Sacred_eel](https://oldschool.runescape.wiki/w/Sacred_eel) |
| `poi_002735` | Fishing spot | Generic fallback clutter 'Tools: Net, Rod + Bait, Pot, or Harpoon' | **Infernal eel**: Level 80 Fishing, Oily rod & bait; requires Fire cape to enter Mor Ul Rek | (2694, 5225, 0) verified catalog location | [Infernal_eel](https://oldschool.runescape.wiki/w/Infernal_eel) |
| `poi_002738` | Fishing spot | Generic fallback clutter 'Tools: Net, Rod + Bait, Pot, or Harpoon' | **Infernal eel**: Level 80 Fishing, Oily rod & bait; requires Fire cape to enter Mor Ul Rek | (2747, 5229, 0) verified catalog location | [Infernal_eel](https://oldschool.runescape.wiki/w/Infernal_eel) |
| `poi_002829` | Fishing spot | Corrupt 'Crystal Eel' claim with fabricated 'Crystal rod + Bait' | Corrected to **Fishing Spot (Shrimp & Sardine)**; Net & Bait spot (NPC 10513) | (1971, 5854, 0) verified catalog location | Live server observation of NPC 10513 Net/Bait spot at (1971, 5855), [Isle_of_Souls](https://oldschool.runescape.wiki/w/Isle_of_Souls) |
| `poi_002841` | Fishing spot | Generic fallback clutter 'Tools: Net, Rod + Bait, Pot, or Harpoon' | **Camdozaal tetra**: Level 33 Fishing, Small fishing net in Ruins of Camdozaal | (2090, 5913, 0) verified catalog location | [Camdozaal_tetra](https://oldschool.runescape.wiki/w/Camdozaal_tetra) |
| `poi_002885` | Fishing spot | Generic fallback clutter 'Tools: Net, Rod + Bait, Pot, or Harpoon' | **Camdozaal tetra**: Level 33 Fishing, Small fishing net in Ruins of Camdozaal | (2089, 6047, 0) verified catalog location | [Camdozaal_tetra](https://oldschool.runescape.wiki/w/Camdozaal_tetra) |
| `poi_003026` | Fishing spot | Generic fallback clutter 'Tools: Net, Rod + Bait, Pot, or Harpoon' | **Shrimp & Anchovy**: Level 1 Fishing, Small fishing net | (2718, 6197, 0) verified catalog location | [Shrimp](https://oldschool.runescape.wiki/w/Shrimp) |
| `poi_003255` | Fishing spot | Generic fallback clutter 'Tools: Net, Rod + Bait, Pot, or Harpoon' | **Cave eel**: Level 38 Fishing, Fishing rod & Fishing bait in Lumbridge / Dorgesh-Kaan caves | (3355, 9566, 0) verified catalog location | [Cave_eel](https://oldschool.runescape.wiki/w/Cave_eel) |
| `poi_003269` | Fishing spot | Generic fallback clutter 'Tools: Net, Rod + Bait, Pot, or Harpoon' | **Cave eel**: Level 38 Fishing, Fishing rod & Fishing bait in Lumbridge / Dorgesh-Kaan caves | (3371, 9577, 0) verified catalog location | [Cave_eel](https://oldschool.runescape.wiki/w/Cave_eel) |
| `poi_003274` | Fishing spot | Generic fallback clutter 'Tools: Net, Rod + Bait, Pot, or Harpoon' | **Cave eel**: Level 38 Fishing, Fishing rod & Fishing bait in Lumbridge / Dorgesh-Kaan caves | (3362, 9588, 0) verified catalog location | [Cave_eel](https://oldschool.runescape.wiki/w/Cave_eel) |
| `poi_003593` | Transportation | Generic charter ship text; missing route destinations, quest locks, and fare discounts | Enriched Trader Stan Charter Ship with exact destinations, fare discounts (50% with *Cabin Fever* / Ring of charos), and quest prerequisites | (3187, 2367, 1) canonical deck marker enriched | [Charter_ship](https://oldschool.runescape.wiki/w/Charter_ship) |
| `poi_003608` | Transportation | Generic charter ship text; missing route destinations, quest locks, and fare discounts | Enriched Trader Stan Charter Ship with exact destinations, fare discounts (50% with *Cabin Fever* / Ring of charos), and quest prerequisites | (1944, 2753, 1) canonical deck marker enriched | [Charter_ship](https://oldschool.runescape.wiki/w/Charter_ship) |
| `poi_003644` | Transportation | Generic charter ship text; missing route destinations, quest locks, and fare discounts | Enriched Trader Stan Charter Ship with exact destinations, fare discounts (50% with *Cabin Fever* / Ring of charos), and quest prerequisites | (1456, 2968, 1) canonical deck marker enriched | [Charter_ship](https://oldschool.runescape.wiki/w/Charter_ship) |
| `poi_003647` | Transportation | Generic charter ship text; missing route destinations, quest locks, and fare discounts | Enriched Trader Stan Charter Ship with exact destinations, fare discounts (50% with *Cabin Fever* / Ring of charos), and quest prerequisites | (1514, 2970, 1) canonical deck marker enriched | [Charter_ship](https://oldschool.runescape.wiki/w/Charter_ship) |
| `poi_003674` | Transportation | Generic charter ship text; missing route destinations, quest locks, and fare discounts | Enriched Trader Stan Charter Ship with exact destinations, fare discounts (50% with *Cabin Fever* / Ring of charos), and quest prerequisites | (2142, 3123, 1) canonical deck marker enriched | [Charter_ship](https://oldschool.runescape.wiki/w/Charter_ship) |
| `poi_003678` | Transportation | Generic charter ship text; missing route destinations, quest locks, and fare discounts | Enriched Trader Stan Charter Ship with exact destinations, fare discounts (50% with *Cabin Fever* / Ring of charos), and quest prerequisites | (1744, 3136, 1) canonical deck marker enriched | [Charter_ship](https://oldschool.runescape.wiki/w/Charter_ship) |
| `poi_003748` | Transportation | Generic charter ship text; missing route destinations, quest locks, and fare discounts | Enriched Trader Stan Charter Ship with exact destinations, fare discounts (50% with *Cabin Fever* / Ring of charos), and quest prerequisites | (2157, 3331, 1) canonical deck marker enriched | [Charter_ship](https://oldschool.runescape.wiki/w/Charter_ship) |
| `poi_003940` | Transportation | Generic charter ship text; missing route destinations, quest locks, and fare discounts | Enriched Trader Stan Charter Ship with exact destinations, fare discounts (50% with *Cabin Fever* / Ring of charos), and quest prerequisites | (3181, 6083, 1) canonical deck marker enriched | [Charter_ship](https://oldschool.runescape.wiki/w/Charter_ship) |
| `poi_004321` | Transportation | Generic fallback transportation description | Enriched Captain Barnaby route: **Brimhaven (30 coins)** and **Rimmington** (*Rocking Out*) | (2676, 3275, 1) canonical deck marker enriched | [Captain_Barnaby](https://oldschool.runescape.wiki/w/Captain_Barnaby) |
| `poi_005772` | Eagle's Peak - Ferrets (Level 27) | Listed false kebbits target and deadfall method; missing Eagles' Peak quest completion requirement | **Target: Ferret**; **Catch method: Box trap**; requires Eagles' Peak quest completion | (2330, 3500, 0) verified catalog location | [Ferret](https://oldschool.runescape.wiki/w/Ferret) |
| `poi_005801` | Vatrachos Island - Mining Site | Erroneous rock listing (Clay 1, Copper 1, Tin 1, Iron 15, Mithril 55) | Corrected rocks to verified in-game spawns: **1 Clay, 2 Gold** | (1872, 2986, 0) verified catalog location | Live server inspection at (1872, 2986), [Vatrachos](https://oldschool.runescape.wiki/w/Vatrachos) |
| `poi_005807` | Charred Island - Red Salamanders (Level 59) | Generic placeholder 'Sailing-island hunter creatures' | **Target: Red salamander (Level 59 Hunter)**; Net trap (Rope & Small net) on Charred Island | (2660, 2396, 0) verified catalog location | Live server inspection observing Red salamanders at (2643, 2397), [Charred_Island](https://oldschool.runescape.wiki/w/Charred_Island) |
| `poi_005809` | Minotaurs' Rest - Tropical Wagtails (Level 19) | Generic placeholder 'Sailing-island hunter creatures' | **Target: Tropical wagtail (Level 19 Hunter)**; Bird snare on Minotaurs' Rest | (1958, 3118, 0) verified catalog location | Live server inspection observing Tropical wagtails at (1970, 3103), [Minotaurs%27_Rest](https://oldschool.runescape.wiki/w/Minotaurs%27_Rest) |
| `poi_005816` | Coral Farming Patch | Erroneous watering can requirement on underwater/boss/tree farming patches | Removed watering can requirement; specified exact tools and skill levels | (3296, 8861, 0) verified catalog location | [Coral_patch](https://oldschool.runescape.wiki/w/Coral_patch) |
| `poi_005817` | Hespori Farming Patch | Erroneous watering can requirement on underwater/boss/tree farming patches | Removed watering can requirement; specified exact tools and skill levels | (1182, 10068, 0) verified catalog location | [Hespori](https://oldschool.runescape.wiki/w/Hespori) |
| `poi_005818` | Seaweed Farming Patch | Erroneous watering can requirement on underwater/boss/tree farming patches | Removed watering can requirement; specified exact tools and skill levels | (3730, 10271, 0) verified catalog location | [Seaweed_patch](https://oldschool.runescape.wiki/w/Seaweed_patch) |
| `poi_005819` | Spirit Tree Farming Patch | Erroneous watering can requirement on underwater/boss/tree farming patches | Removed watering can requirement; specified exact tools and skill levels | (3614, 3856, 0) verified catalog location | [Spirit_tree_(Farming)](https://oldschool.runescape.wiki/w/Spirit_tree_(Farming)) |
| `poi_005831` | Desert Mining Camp Surface Mining Site | Erroneous 'Silver' rock in overview line and duplicate 'Iron' rock | Aligned overview to verified rocks: **4 Copper, 4 Tin, 3 Iron, 4 Coal** | (3299, 3021, 0) verified catalog location | Live server inspection at (3299, 3021), [Desert_Mining_Camp](https://oldschool.runescape.wiki/w/Desert_Mining_Camp) |
| `poi_005844` | Trahaearn Mining Site | Misspelled elven clan 'Trahearn'; missing complete ore breakdown | Corrected spelling to **Trahaearn**; enriched with verified **26 Iron, 8 Silver, 19 Coal, 14 Gold, 7 Mithril, 10 Soft clay, 7 Adamantite, 4 Runite** | (3295, 5987, 0) verified catalog location | RuneLite client sources, [Trahaearn_mine](https://oldschool.runescape.wiki/w/Trahaearn_mine) |
| `poi_005846` | Ship to Brimhaven / Rimmington | Generic fallback transportation description | Enriched Captain Barnaby route: **Brimhaven (30 coins)** and **Rimmington** (*Rocking Out*) | (2675, 3275, 0) verified catalog location | [Captain_Barnaby](https://oldschool.runescape.wiki/w/Captain_Barnaby) |
| `poi_005847` | Charter Ship | Generic charter ship text; missing route destinations, quest locks, and fare discounts | Enriched Trader Stan Charter Ship with exact destinations, fare discounts (50% with *Cabin Fever* / Ring of charos), and quest prerequisites | (1455, 2968, 0) verified catalog location | [Charter_ship](https://oldschool.runescape.wiki/w/Charter_ship) |
| `poi_005848` | Charter Ship | Generic charter ship text; missing route destinations, quest locks, and fare discounts | Enriched Trader Stan Charter Ship with exact destinations, fare discounts (50% with *Cabin Fever* / Ring of charos), and quest prerequisites | (1743, 3136, 0) verified catalog location | [Charter_ship](https://oldschool.runescape.wiki/w/Charter_ship) |
| `poi_005849` | Charter Ship | Generic charter ship text; missing route destinations, quest locks, and fare discounts | Enriched Trader Stan Charter Ship with exact destinations, fare discounts (50% with *Cabin Fever* / Ring of charos), and quest prerequisites | (1943, 2753, 0) verified catalog location | [Charter_ship](https://oldschool.runescape.wiki/w/Charter_ship) |
| `poi_005850` | Charter Ship | Generic charter ship text; missing route destinations, quest locks, and fare discounts | Enriched Trader Stan Charter Ship with exact destinations, fare discounts (50% with *Cabin Fever* / Ring of charos), and quest prerequisites | (2141, 3123, 0) verified catalog location | [Charter_ship](https://oldschool.runescape.wiki/w/Charter_ship) |
| `poi_005851` | Charter Ship | Generic charter ship text; missing route destinations, quest locks, and fare discounts | Enriched Trader Stan Charter Ship with exact destinations, fare discounts (50% with *Cabin Fever* / Ring of charos), and quest prerequisites | (2156, 3331, 0) verified catalog location | [Charter_ship](https://oldschool.runescape.wiki/w/Charter_ship) |
| `poi_005852` | Charter Ship | Generic charter ship text; missing route destinations, quest locks, and fare discounts | Enriched Trader Stan Charter Ship with exact destinations, fare discounts (50% with *Cabin Fever* / Ring of charos), and quest prerequisites | (3180, 6083, 0) verified catalog location | [Charter_ship](https://oldschool.runescape.wiki/w/Charter_ship) |
| `poi_005853` | Charter Ship | Generic charter ship text; missing route destinations, quest locks, and fare discounts | Enriched Trader Stan Charter Ship with exact destinations, fare discounts (50% with *Cabin Fever* / Ring of charos), and quest prerequisites | (3186, 2367, 0) verified catalog location | [Charter_ship](https://oldschool.runescape.wiki/w/Charter_ship) |
| `poi_005854` | Charter Ship | Generic charter ship text; missing route destinations, quest locks, and fare discounts | Enriched Trader Stan Charter Ship with exact destinations, fare discounts (50% with *Cabin Fever* / Ring of charos), and quest prerequisites | (1513, 2970, 0) verified catalog location | [Charter_ship](https://oldschool.runescape.wiki/w/Charter_ship) |
| `poi_005855` | Lovakengj Minecart Network - Lovakengj | Generic minecart text | Enriched Lovakengj Minecart Network details and destinations | (1524, 3725, 0) verified catalog location | [Lovakengj_Minecart_Network](https://oldschool.runescape.wiki/w/Lovakengj_Minecart_Network) |
| `poi_005857` | Gertrude's Cat | Difficulty: Novice • Length: Short | **Difficulty: Novice • Length: Very Short** | (3273, 3401, 0) verified catalog location | Canonical `quest-details.tsv` |
| `poi_005859` | Pirate's Treasure | Difficulty: Novice • Length: Short | **Difficulty: Novice • Length: Very Short** | (3097, 3257, 0) verified catalog location | Canonical `quest-details.tsv` |
| `poi_005860` | Vampyre Slayer | Difficulty: Novice • Length: Short | **Difficulty: Intermediate • Length: Very Short** | (3086, 3236, 0) verified catalog location | Canonical `quest-details.tsv` |
| `poi_005863` | Waterfall Quest | Difficulty: Intermediate • Length: Medium | **Difficulty: Intermediate • Length: Short** | (2517, 3574, 0) verified catalog location | Canonical `quest-details.tsv` |
| `poi_005864` | Merlin's Crystal | Difficulty: Intermediate • Length: Medium | **Difficulty: Intermediate • Length: Short** | (2839, 3538, 0) verified catalog location | Canonical `quest-details.tsv` |
| `poi_005865` | Holy Grail | Difficulty: Intermediate • Length: Medium | **Difficulty: Intermediate • Length: Short** | (2839, 3532, 0) verified catalog location | Canonical `quest-details.tsv` |
| `poi_005866` | Priest in Peril | Difficulty: Novice • Length: Medium | **Difficulty: Novice • Length: Short** | (3483, 3485, 0) verified catalog location | Canonical `quest-details.tsv` |
| `poi_005867` | Nature Spirit | Difficulty: Novice • Length: Medium | **Difficulty: Intermediate • Length: Short** | (3466, 3480, 0) verified catalog location | Canonical `quest-details.tsv` |
| `poi_005872` | Roving Elves | Difficulty: Experienced • Length: Medium | **Difficulty: Experienced • Length: Short** | (2185, 3160, 0) verified catalog location | Canonical `quest-details.tsv` |
| `poi_005873` | Mourning's End Part I | Difficulty: Master • Length: Long | **Difficulty: Master • Length: Medium** | (2190, 3258, 0) verified catalog location | Canonical `quest-details.tsv` |
| `poi_005878` | Beneath Cursed Sands | Difficulty: Master • Length: Long | **Difficulty: Master • Length: Medium** | (3415, 2849, 0) verified catalog location | Canonical `quest-details.tsv` |
| `poi_005880` | Troll Stronghold | Difficulty: Intermediate • Length: Medium | **Difficulty: Intermediate • Length: Short** | (2875, 3670, 0) verified catalog location | Canonical `quest-details.tsv` |
| `poi_005883` | Making Friends with My Arm | Difficulty: Master • Length: Long | **Difficulty: Master • Length: Medium** | (2840, 3920, 0) verified catalog location | Canonical `quest-details.tsv` |
| `poi_005884` | Druidic Ritual | Difficulty: Novice • Length: Short | **Difficulty: Novice • Length: Very Short** | (2895, 3445, 0) verified catalog location | Canonical `quest-details.tsv` |
| `poi_005885` | Heroes' Quest | Difficulty: Experienced • Length: Long | **Difficulty: Experienced • Length: Medium** | (2905, 3440, 0) verified catalog location | Canonical `quest-details.tsv` |
| `poi_005887` | Witch's House | Difficulty: Intermediate • Length: Short | **Difficulty: Intermediate • Length: Very Short** | (2870, 3540, 0) verified catalog location | Canonical `quest-details.tsv` |
| `poi_005891` | Twilight's Promise | Difficulty: Intermediate • Length: Medium | **Difficulty: Intermediate • Length: Short** | (1680, 3130, 0) verified catalog location | Canonical `quest-details.tsv` |
| `poi_005893` | The Ribbiting Tale of a Lily Pad Labour Dispute | Difficulty: Novice • Length: Short | **Difficulty: Novice • Length: Very Short** | (1540, 3040, 0) verified catalog location | Canonical `quest-details.tsv` |
| `poi_005896` | The Heart of Darkness | Difficulty: Experienced • Length: Long | **Difficulty: Experienced • Length: Medium** | (1415, 3180, 0) verified catalog location | Canonical `quest-details.tsv` |
| `poi_005783` | Isle of Souls - Grey Chinchompas (Level 53) | False phantom authored spawn on Soul Wars / Isle of Souls; real hunting grounds exist elsewhere on the island | **REMOVED** from catalog and locations per user confirmation | Removed from (2220, 2870, 0) | RuneLite `HunterAreaLocation` & live OSRS scene |
| `poi_005785` | Isle of Souls - Crimson Swifts (Level 1) | False phantom authored spawn on Soul Wars / Isle of Souls; real hunting grounds exist elsewhere on the island | **REMOVED** from catalog and locations per user confirmation | Removed from (2190, 2880, 0) | RuneLite `HunterAreaLocation` & live OSRS scene |

## Queue-by-queue audit analysis & remediation

### Phase A: Authored candidates & duplicates (`poi_005800`–`poi_005896`)
1. **Charter Ships & Sea Transportation (`poi_005846`–`poi_005855`, `poi_003593`–`poi_004321`):**
   - Reconciled authored dock surface markers (`plane=0`) and canonical ship deck markers (`plane=1`).
   - Populated complete route destinations, operators (Trader Stan's crew vs Captain Barnaby), fare discount mechanics (50% discount with *Cabin Fever* / Ring of charos (a)), and regional quest unlock gates (*Children of the Sun* for Aldarin/Civitas illa Fortis, *Regicide* for Port Tyras, *Song of the Elves* for Prifddinas, *Ghosts Ahoy* for Port Phasmatys, *Rocking Out* for Rimmington).
2. **Mining & Farming Sites (`poi_005801`, `poi_005816`–`poi_005844`):**
   - **Trahaearn Mine (`poi_005844`):** Corrected clan spelling to canonical elven name `Trahaearn` and enriched with verified ore rock counts (`26 Iron, 8 Silver, 19 Coal, 14 Gold, 7 Mithril, 10 Soft clay, 7 Adamantite, 4 Runite`).
   - **Desert Mining Camp (`poi_005831`):** Fixed duplicate Iron listing and non-existent Silver rock; aligned overview line to verified `4 Copper, 4 Tin, 3 Iron, 4 Coal` rocks.
   - **Vatrachos Island (`poi_005801`):** Fixed erroneous rock listing (Clay 1, Copper 1, Tin 1, Iron 15, Mithril 55) to verified rocks: `1 Clay, 2 Gold`.
   - **Farming Patches (`poi_005816`–`poi_005819`):** Removed impossible watering can requirement from underwater Coral, underground boss Hespori, and underwater Seaweed patches.
3. **Authored Quests (`poi_005857`–`poi_005896`):**
   - Scanned all quest start entries against canonical `src/main/resources/atlasofgielinor/poi/quest-details.tsv`.
   - Aligned 21 quest entries with authoritative difficulty tiers, lengths, QP rewards, and prerequisites (e.g. *Gertrude's Cat*, *Pirate's Treasure*, and *Witch's House* to `Very Short`; *Waterfall Quest*, *Merlin's Crystal*, and *Holy Grail* to `Short`; *Vampyre Slayer* and *Nature Spirit* to `Intermediate`).

### Phase B: Unresolved reference conflicts & unreviewed candidates
1. **Fishing Spot Cleanup (`poi_002649`–`poi_003274`):**
   - Eliminated generic fallback clutter (`Tools: Net, Rod + Bait, Pot, or Harpoon`) across Sacred eel, Infernal eel, Camdozaal tetra, Cave eel, and Drift net spots.
   - Discovered and corrected fabricated "Crystal Eel" (`poi_002829` at `1971, 5854, 0`) to standard Isle of Souls Net & Bait spot (`Fishing Spot (Shrimp & Sardine)`, NPC 10513).
2. **Hunter Gate Claims (`poi_005772`):**
   - Audited Ferrets at Eagles' Peak (`2330, 3500, 0`). Verified in-game presence of Ferrets. Removed false claims of kebbits and deadfall traps; established requirement of *Eagles' Peak* quest completion for box trapping.
3. **Sailing Island Hunter Spots (`poi_005807`, `poi_005809`):**
   - Replaced generic placeholder `Sailing-island hunter creatures` descriptions:
     - **Charred Island (`poi_005807`):** Verified Red salamanders at `(2643, 2397)` requiring Level 59 Hunter and Net trap (Rope & Small fishing net on young tree).
     - **Minotaurs' Rest (`poi_005809`):** Verified Tropical wagtails at `(1970, 3103)` requiring Level 19 Hunter and Bird snare.

## Separation of mechanics vs marker placement

Per project instructions, settled text and mechanic corrections were strictly separated from unresolved marker placement:
- Authored coordinates situated near related activity locations or later quest stages are preserved without relocation pending authoritative deduplication/removal approvals.
- Canonical ship deck markers (`plane=1`) and authored dock gangplank markers (`plane=0`) were both factually enriched to ensure rich tooltips and detail cards are rendered regardless of which marker the user inspects.
- False overworld duplicate `poi_005778` (Moonlight Antelope on surface) was completely removed, leaving canonical under-cavern marker `poi_006365`.

## Validation and testing

1. **Java Unit Tests:**
   - Test suite builds and passes cleanly (`.\gradlew.bat test`), including `PoiDetailsTest`, `PoiIndexTest`, `PoiIndexSearchTest`, `PoiCategoryTest`, and `PoiTooltipBuilderTest`.
   - `correctedPoiDetailsReflectFactualRevisions()` in `PoiDetailsTest.java` validates factual details across all revised quest, skilling, charter transportation, and hunter entries.
2. **Audit Verification Scripts:**
   - `python scripts/check-poi-audit.py`: **PASSED** (all valid controls unflagged).
   - `python scripts/verify-poi-audit.py`: **PASSED** (0 confirmed reference conflicts).
   - `python scripts/audit-poi-facts.py`: **PASSED** (0 visible locations with strong findings across 8,151 scanned locations).
3. **Packaging:**
   - `.\gradlew.bat jar` packages the production plugin jar cleanly with updated catalog TSVs.


## Phase 2: Ghost & Phantom POI Elimination

- Deleted poi_005701. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_002767. Coords: 0, 1768, 5366
- Deleted poi_005710. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_003190. Coords: 0, 2525, 9294
- Deleted poi_005713. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_003347. Coords: 0, 2964, 9731
- Deleted poi_005717. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_004343. Coords: 0, 2400, 5984
- Deleted poi_005722. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_005666. Coords: 0, 1952, 4448
- Deleted poi_005726. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_005527. Coords: 0, 3487, 9510
- Deleted poi_005727. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_005526. Coords: 0, 3319, 9522
- Deleted poi_005733. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_003475. Coords: 0, 2278, 10011
- Deleted poi_005735. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_002018. Coords: 0, 2533, 3573
- Deleted poi_005749. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_003307. Coords: 0, 3164, 9652
- Deleted poi_005750. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_003346. Coords: 0, 3484, 9721
- Deleted poi_005753. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_003275. Coords: 0, 2808, 9594
- Deleted poi_005759. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_005399. Coords: 0, 2712, 5247
- Deleted poi_005761. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_004860. Coords: 0, 3107, 3367
- Deleted poi_005763. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_000503. Coords: 0, 2871, 3007
- Deleted poi_005764. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_004947. Coords: 0, 2550, 2930
- Deleted poi_005767. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_004947. Coords: 0, 2568, 2938
- Deleted poi_005769. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_005144. Coords: 0, 2350, 3620
- Deleted poi_005770. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_005144. Coords: 0, 2340, 3635
- Deleted poi_005771. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_005144. Coords: 0, 2345, 3590
- Deleted poi_005782. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_000645. Coords: 0, 1475, 3090
- Deleted poi_005787. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_004766. Coords: 0, 3270, 6080
- Deleted poi_005788. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_005239. Coords: 0, 3177, 2455
- Deleted poi_005789. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_000116. Coords: 0, 3213, 2516
- Deleted poi_005790. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_000096. Coords: 0, 3251, 2434
- Deleted poi_005791. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_005362. Coords: 0, 3061, 2639
- Deleted poi_005792. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_001637. Coords: 0, 1892, 3429
- Deleted poi_005793. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_005238. Coords: 0, 3069, 2986
- Deleted poi_005794. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_000148. Coords: 0, 1765, 2659
- Deleted poi_005795. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_003160. Coords: 0, 2580, 8613
- Deleted poi_005796. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_005238. Coords: 0, 3069, 2987
- Deleted poi_005798. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_005384. Coords: 0, 2222, 3467
- Deleted poi_005799. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_005388. Coords: 0, 2927, 4057
- Deleted poi_005800. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_005377. Coords: 0, 2189, 2328
- Deleted poi_005801. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_005368. Coords: 0, 1872, 2986
- Deleted poi_005802. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_005377. Coords: 0, 2191, 2327
- Deleted poi_005805. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_005362. Coords: 0, 3063, 2639
- Deleted poi_005806. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_005384. Coords: 0, 2224, 3466
- Deleted poi_005807. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_005367. Coords: 0, 2660, 2396
- Deleted poi_005808. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_005369. Coords: 0, 2467, 2722
- Deleted poi_005809. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_005370. Coords: 0, 1958, 3118
- Deleted poi_005810. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_002604. Coords: 0, 1954, 4057
- Deleted poi_005812. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_005369. Coords: 0, 2469, 2721
- Deleted poi_005817. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_004363. Coords: 0, 1182, 10068
- Deleted poi_005831. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_004862. Coords: 0, 3299, 3021
- Deleted poi_005834. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_003563. Coords: 0, 3045, 10263
- Deleted poi_005842. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_004486. Coords: 0, 2221, 9003
- Deleted poi_005843. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_004486. Coords: 0, 2224, 8977
- Deleted poi_005848. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_005160. Coords: 0, 1743, 3136
- Deleted poi_005850. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_005024. Coords: 0, 2141, 3123
- Deleted poi_005853. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_000059. Coords: 0, 3186, 2367
- Deleted poi_005854. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_005163. Coords: 0, 1513, 2970
- Deleted poi_005855. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_004144. Coords: 0, 1524, 3725
- Deleted poi_005856. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_000978. Coords: 0, 3244, 3206
- Deleted poi_005859. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_001128. Coords: 0, 3097, 3257
- Deleted poi_005861. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_001360. Coords: 0, 2982, 3338
- Deleted poi_005871. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_000822. Coords: 0, 2185, 3145
- Deleted poi_005872. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_000828. Coords: 0, 2185, 3160
- Deleted poi_005880. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_004968. Coords: 0, 2875, 3670
- Deleted poi_005889. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_000623. Coords: 0, 2795, 3065
- Deleted poi_005890. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_000498. Coords: 0, 2830, 2985
- Deleted poi_005891. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_000772. Coords: 0, 1680, 3130
- Deleted poi_006900. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_000756. Coords: 0, 1496, 3132
- Deleted poi_006901. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_001716. Coords: 0, 3236, 3458
- Deleted poi_006902. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_001872. Coords: 0, 3229, 3504
- Deleted poi_006903. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_002442. Coords: 0, 3815, 3808
- Deleted poi_006904. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_002338. Coords: 0, 1812, 3745
- Deleted poi_006905. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_001955. Coords: 0, 2832, 3542
- Deleted poi_006906. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_001840. Coords: 0, 2462, 3496
- Deleted poi_006907. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_002283. Coords: 0, 2730, 3713
- Deleted poi_006908. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_004964. Coords: 0, 2240, 3328
- Deleted poi_006909. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_002562. Coords: 0, 3045, 3925
- Deleted poi_006910. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_000697. Coords: 0, 2339, 3108
- Deleted poi_006911. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_002326. Coords: 0, 2520, 3740
- Deleted poi_006912. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_002328. Coords: 0, 2542, 3741
- Deleted poi_006913. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_002580. Coords: 0, 2853, 3944
- Deleted poi_006914. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_001788. Coords: 0, 2876, 3480
- Deleted poi_006915. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_001795. Coords: 0, 2819, 3484
- Deleted poi_006916. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_002588. Coords: 0, 3004, 3963
- Deleted poi_006917. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_002323. Coords: 0, 3016, 3739
- Deleted poi_006918. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_002341. Coords: 0, 3292, 3746
- Deleted poi_006919. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_002171. Coords: 0, 3259, 3666
- Deleted poi_006920. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_001167. Coords: 0, 2695, 3283
- Deleted poi_006921. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_000620. Coords: 0, 2593, 3085
- Deleted poi_006922. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_000837. Coords: 0, 3103, 3162
- Deleted poi_006923. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_001881. Coords: 0, 1603, 3508
- Deleted poi_006924. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_002387. Coords: 0, 3745, 3779
- Deleted poi_006925. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_002504. Coords: 0, 3677, 3854
- Deleted poi_006926. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_000732. Coords: 0, 2568, 3122
- Deleted poi_006927. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_000610. Coords: 0, 2603, 3078
- Deleted poi_006928. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_002902. Coords: 0, 3280, 6059
- Deleted poi_006929. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_000552. Coords: 0, 2484, 3043
- Deleted poi_006930. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_002724. Coords: 0, 2480, 5175
- Deleted poi_006931. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_002719. Coords: 0, 2438, 5168
- Deleted poi_006932. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_002705. Coords: 0, 2496, 5118
- Deleted poi_006933. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_003516. Coords: 0, 2773, 10162
- Deleted poi_006934. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_003978. Coords: 1, 1439, 9509
- Deleted poi_006935. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_003405. Coords: 0, 2428, 9824
- Deleted poi_006936. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_003310. Coords: 0, 2833, 9656
- Deleted poi_006937. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_000263. Coords: 0, 3347, 2827
- Deleted poi_006938. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_002019. Coords: 0, 2544, 3569
- Deleted poi_006939. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_000942. Coords: 0, 2806, 3193
- Deleted poi_006940. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_001822. Coords: 0, 3506, 3490
- Deleted poi_006941. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_000366. Coords: 0, 1649, 2930
- Deleted poi_006942. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_001159. Coords: 0, 3103, 3279
- Deleted poi_006943. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_001361. Coords: 0, 3035, 3340
- Deleted poi_006944. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_001655. Coords: 0, 2474, 3436
- Deleted poi_006945. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_002949. Coords: 0, 3253, 6109
- Deleted poi_006946. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_002114. Coords: 0, 1551, 3632
- Deleted poi_006947. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_004880. Coords: 0, 3422, 3550
- Deleted poi_006948. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_001665. Coords: 0, 2945, 3439
- Deleted poi_006949. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_001166. Coords: 0, 3546, 3282
- Deleted poi_006950. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_000401. Coords: 0, 3373, 2957
- Deleted poi_006951. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_002346. Coords: 0, 2546, 3748
- Deleted poi_006952. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_002106. Coords: 0, 3268, 3627
- Deleted poi_006953. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_001996. Coords: 0, 3505, 3558
- Deleted poi_006954. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_000061. Coords: 0, 3258, 2378
- Deleted poi_006955. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_000109. Coords: 0, 3143, 2485
- Deleted poi_006956. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_000121. Coords: 0, 3122, 2532
- Deleted poi_006957. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_005367. Coords: 0, 2661, 2395
- Deleted poi_006958. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_002243. Coords: 0, 2081, 3690
- Deleted poi_006959. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_005387. Coords: 0, 1955, 4056
- Deleted poi_006960. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_005364. Coords: 0, 2998, 2288
- Deleted poi_006961. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_001946. Coords: 0, 2151, 3530
- Deleted poi_006962. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_005362. Coords: 0, 3062, 2639
- Deleted poi_006963. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_005383. Coords: 0, 2098, 3188
- Deleted poi_006964. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_005369. Coords: 0, 2468, 2721
- Deleted poi_006965. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_005372. Coords: 0, 2319, 2774
- Deleted poi_006966. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_005375. Coords: 0, 1766, 2659
- Deleted poi_006967. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_005382. Coords: 0, 1861, 3306
- Deleted poi_006968. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_005379. Coords: 0, 1558, 2771
- Deleted poi_006970. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_000225. Coords: 0, 1202, 2785
- Deleted poi_006971. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_000271. Coords: 0, 3793, 2836
- Deleted poi_006972. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_002303. Coords: 0, 1269, 3730
- Deleted poi_006973. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_002936. Coords: 0, 3289, 6100
- Deleted poi_006974. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_001991. Coords: 0, 1729, 3558
- Deleted poi_006975. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_001919. Coords: 0, 3598, 3524
- Deleted poi_006976. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_001246. Coords: 0, 3052, 3309
- Deleted poi_006977. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_001727. Coords: 0, 2810, 3462
- Deleted poi_006978. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_001468. Coords: 0, 2663, 3375
- Deleted poi_006979. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_000658. Coords: 0, 1587, 3101
- Deleted poi_006980. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_002295. Coords: 0, 1235, 3724
- Deleted poi_006981. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_001408. Coords: 0, 3084, 3356
- Deleted poi_006982. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_001397. Coords: 0, 1449, 3354
- Deleted poi_006983. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_001013. Coords: 0, 2938, 3223
- Deleted poi_006984. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_002517. Coords: 0, 2589, 3862
- Deleted poi_006985. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_001409. Coords: 0, 3182, 3356
- Deleted poi_006986. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_001016. Coords: 0, 2615, 3224
- Deleted poi_006987. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_002301. Coords: 0, 1259, 3729
- Deleted poi_006988. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_000958. Coords: 0, 3313, 3201
- Deleted poi_006989. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_002336. Coords: 0, 1264, 3745
- Deleted poi_006990. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_000655. Coords: 0, 2793, 3099
- Deleted poi_006991. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_000533. Coords: 0, 1365, 3035
- Deleted poi_006992. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_000076. Coords: 0, 3125, 2403
- Deleted poi_006993. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_002355. Coords: 0, 1242, 3755
- Deleted poi_006994. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_002963. Coords: 0, 3292, 6120
- Deleted poi_006995. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_000519. Coords: 0, 1352, 3025
- Deleted poi_006996. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_000910. Coords: 0, 2487, 3181
- Deleted poi_006997. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_000829. Coords: 0, 2343, 3160
- Deleted poi_006998. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_001683. Coords: 0, 2472, 3445
- Deleted poi_006999. Reason: Class 1: Spatial/Lexical Duplicate (<=35 tiles). Canonical ID: poi_001642. Coords: 0, 2858, 3432
