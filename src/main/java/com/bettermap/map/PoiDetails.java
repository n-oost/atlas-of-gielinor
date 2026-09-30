/*
 * Copyright (c) 2026, n-oost
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.bettermap.map;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Map;
import java.util.Set;

/**
 * Curated details for points of interest: quest names, difficulty and starting NPCs, rare trees
 * (Varlamore camphor and jatoba among them), dungeons, skilling level requirements, and the tools
 * a spot needs.
 */
public final class PoiDetails
{
	private static final Pattern LEVEL_REQUIREMENT = Pattern.compile("(?i)(?:level requirement:|requires:)?\\s*(?:level\\s*)?(\\d+)\\s+([a-z ]+?)(?:\\s*[,.(].*)?$");

	public static List<String> compactSkillingLines(String title, List<String> lines)
	{
		for (String line : lines)
		{
			final Matcher matcher = LEVEL_REQUIREMENT.matcher(line.trim());
			if (matcher.matches())
			{
				return List.of("Level " + matcher.group(1) + " " + matcher.group(2).trim());
			}
		}
		final Matcher titleLevel = Pattern.compile("(?i).*\\(Level (\\d+)\\).*").matcher(title);
		if (titleLevel.matches())
		{
			return List.of("Level " + titleLevel.group(1));
		}
		return Collections.emptyList();
	}

	/** Known spell requirements for teleport POIs; other travel modes have no rune spell cost. */
	public static List<String> compactTeleportLines(String title)
	{
		switch (title)
		{
			case "Teleport to Rune Essence":
				return List.of("Wizard NPC • Rune Mysteries");
			case "Teleport to Sorceress's Garden":
				return List.of("Minigame teleport • Prince Ali Rescue");
			case "Teleport to Library Archive":
				return List.of("2 Earth, 1 Law rune • 6 Magic");
			case "Teleport to Prifddinas Library":
				return List.of("Teleport crystal • Song of the Elves");
			default:
				return Collections.emptyList();
		}
	}

	public static final class Detail
	{
		private final String title;
		private final String category;
		private final List<String> lines;

		public Detail(String title, String category, List<String> lines)
		{
			this.title = title;
			this.category = category;
			this.lines = lines != null ? lines : Collections.emptyList();
		}

		public String getTitle()
		{
			return title;
		}

		public String getCategory()
		{
			return category;
		}

		public List<String> getLines()
		{
			return lines;
		}
	}

	static final class Entry
	{
		final String type;
		final String title;
		final String category;
		final List<String> lines;
		final int x;
		final int y;
		final int plane;

		Entry(String type, String title, String category, List<String> lines, int x, int y, int plane)
		{
			this.type = type;
			this.title = title;
			this.category = category;
			this.lines = lines;
			this.x = x;
			this.y = y;
			this.plane = plane;
		}
	}

	private static final List<Entry> ENTRIES = new ArrayList<>(1200);
	private static final Set<String> ENTRY_KEYS = new HashSet<>(1200);
	private static final Map<Long, Entry> EXACT_MAP = new HashMap<>(1200);
	private static final Map<Long, List<Entry>> CHUNK_MAP = new HashMap<>(1200);

	private static long pointKey(int x, int y, int plane)
	{
		return (((long) plane & 0x3L) << 32) | (((long) x & 0xFFFFL) << 16) | ((long) y & 0xFFFFL);
	}

	private static long chunkKey(int plane, int chunkX, int chunkY)
	{
		return (((long) plane & 0x3L) << 32) | (((long) (chunkX & 0xFFFF) << 16)) | ((long) (chunkY & 0xFFFF));
	}

	/** Known item / quest requirements for dungeon entrances, keyed by curated title. */
	private static final Map<String, List<String>> DUNGEON_REQUIREMENTS = buildDungeonRequirements();

	private static Map<String, List<String>> buildDungeonRequirements()
	{
		final Map<String, List<String>> reqs = new HashMap<>();
		reqs.put("Kalphite Lair", List.of(
			"Requires: Rope on the entrance hole (Kalphite Queen lair)",
			"Light source recommended underground"));
		reqs.put("Kalphite Cave", List.of(
			"Requires: Shantay pass or desert access",
			"Light source recommended"));
		reqs.put("Brimhaven Dungeon", List.of(
			"Requires: 875 coins to Dunstan (or Cabin Fever completed)",
			"Antipoison and food recommended"));
		reqs.put("God Wars Dungeon", List.of(
			"Requires: 40 Agility, 60 Strength, or 60 Ranged to cross the bridge",
			"Protection from Zamorak for the main chamber"));
		reqs.put("Gryphon Dungeon", List.of(
			"Requires: Children of the Sun (Varlamore access)",
			"Shellbane gryphons inside — bring antidote or antipoison"));
		reqs.put("Shellbane Gryphon Cave", List.of(
			"Requires: Children of the Sun (Varlamore access)",
			"Antipoison strongly recommended"));
		reqs.put("Giants' Den", List.of(
			"Requires: Combat gear",
			"Hill giants, Moss giants, Fire giants under Shayzien"));
		reqs.put("Fortis Barracks", List.of(
			"Requires: Children of the Sun (Varlamore access)",
			"Subterranean barracks & training tunnels under Civitas illa Fortis"));
		reqs.put("Lizardman Temple", List.of(
			"Requires: Combat gear (Lizardman Shamans)",
			"Stone chests: lockpicks and Shayzien armour recommended"));
		reqs.put("Lizardman Caves", List.of(
			"Requires: Combat gear / Molch fishing gear",
			"Lizardmen & cavern tunnels under Lizardman Settlement"));
		reqs.put("Shayzien Crypts", List.of(
			"Requires: Combat gear",
			"Shayzien zombies and skeletal warriors under the cemetery"));
		reqs.put("Shayzien Crypt", List.of(
			"Requires: Combat gear",
			"Shayzien zombies and skeletal warriors under the cemetery"));
		reqs.put("Myths' Guild Dungeon", List.of(
			"Requires: Dragon Slayer II completed",
			"Wrath altar, green & blue dragons, adamant & rune dragons"));
		reqs.put("Myths' Guild dungeon", List.of(
			"Requires: Dragon Slayer II completed",
			"Wrath altar, green & blue dragons, adamant & rune dragons"));
		reqs.put("Hunter Guild Caverns", List.of(
			"Requires: 46 Hunter (Hunters' Rumours)",
			"Hunter Guild master, whistle recharging & guild cavers"));
		reqs.put("Lithkren Vault", List.of(
			"Requires: Dragon Slayer II",
			"Adamant & Rune dragons, Dragon forge & ancient vault"));
		reqs.put("Weiss Salt Mine", List.of(
			"Requires: Making Friends with My Arm",
			"Troll salt mine • Efreeti fire & basalt mining"));
		reqs.put("Salt Mine", List.of(
			"Requires: Making Friends with My Arm",
			"Troll salt mine • Efreeti fire & basalt mining"));
		reqs.put("Jaldraocht Pyramid", List.of(
			"Requires: Desert Treasure I",
			"Ancient Magicks pyramid • Azzanadra's altar"));
		reqs.put("River Elid Dungeon", List.of(
			"Requires: Spirits of the Elid",
			"Water spirits dungeon • Ancestral shrine & statuette"));
		reqs.put("Shadow Dungeon", List.of(
			"Requires: Ring of visibility, Desert Treasure I",
			"Shadow diamond cavern • Damis & shadow hounds"));
		reqs.put("Draynor Manor Basement", List.of(
			"Requires: Ernest the Chicken",
			"Professor Oddenstein's basement • Lever puzzle & oil can"));
		reqs.put("Draynor Manor basement", List.of(
			"Requires: Ernest the Chicken",
			"Professor Oddenstein's basement • Lever puzzle & oil can"));
		reqs.put("Enakhra's Temple", List.of(
			"Requires: Enakhra's Lament",
			"Ancient Mahjarrat temple • Bone room & fountain puzzles"));
		reqs.put("Port Sarim Rat Pits", List.of(
			"Requires: Ratcatchers",
			"Felkrash's rat pit • Pet cat gambling minigame"));
		reqs.put("King Black Dragon Lair", List.of(
			"Requires: Wilderness entrance / lever",
			"King Black Dragon boss lair • Dragonfire shield & visage"));
		reqs.put("Hespori Cave", List.of(
			"Requires: 65 Farming, Hespori seed",
			"Hespori boss arena • Bottomless compost bucket & anima seeds"));
		reqs.put("Stalker Den", List.of(
			"Requires: The Twilight's Blade",
			"Stalker boss dungeon • Twilight Emissaries quest cavern"));
		reqs.put("Ancient Cavern", List.of(
			"Requires: Barbarian Training (Fishing)",
			"Bring heavy boots and a hatchet"));
		reqs.put("Asgarnian Ice Dungeon", List.of(
			"Requires: 51 Thieving (optional shortcut) or long walk",
			"Bring food and armour"));
		reqs.put("Taverley Dungeon", List.of(
			"Requires: Dusty key (from Jail in Edgeville Dungeon)",
			"Light source recommended"));
		reqs.put("Edgeville Dungeon", List.of(
			"Requires: Brass key for the locked door (H.A.M. area)",
			"Light source recommended"));
		reqs.put("Waterbirth Dungeon", List.of(
			"Requires: Rellekka / Fremennik boat access and combat gear",
			"Bring food, prayer potions, and armour for Dagannoths"));
		reqs.put("Waterbirth Island Dungeon", List.of(
			"Requires: Rellekka / Fremennik boat access and combat gear",
			"Bring food, prayer potions, and armour for Dagannoths"));
		reqs.put("Waterbirth Dungeon (sub-levels)", List.of(
			"Requires: Rellekka / Fremennik boat access and combat gear",
			"Bring food, prayer potions, and armour for Dagannoths"));
		reqs.put("Fremennik Slayer Dungeon", List.of(
			"Requires: Fremennik Trials",
			"Bring a light source"));
		reqs.put("Smoke Dungeon", List.of(
			"Requires: Facemask or Slayer helmet (smoke damage)",
			"Bring a rope and light source"));
		reqs.put("Iorwerth Dungeon", List.of(
			"Requires: Song of the Elves",
			"Bring food and armour"));
		reqs.put("Mole Hole", List.of(
			"Requires: Spade to dig into mole hills in Falador Park",
			"Light source required (Bullseye lantern recommended)"));
		reqs.put("Giant Mole", List.of(
			"Requires: Spade to dig into mole hills in Falador Park",
			"Light source required (Bullseye lantern recommended)"));
		reqs.put("Mole Hole (Giant Mole)", List.of(
			"Requires: Spade to dig into mole hills in Falador Park",
			"Light source required (Bullseye lantern recommended)"));
		reqs.put("Karuulm Slayer Dungeon", List.of(
			"Requires: Boots of stone/brimstone/granite (or Kourend Elite Diary)",
			"Protects against burning heat damage on dungeon floors"));
		reqs.put("Kraken Cove", List.of(
			"Requires: Level 87 Slayer",
			"Fishing explosive to disturb Cave kraken (boss requires Slayer task)"));
		reqs.put("Kraken (boss)", List.of(
			"Requires: Level 87 Slayer",
			"Fishing explosive to disturb Cave kraken (boss requires Slayer task)"));
		reqs.put("Smoke Devil Dungeon", List.of(
			"Requires: Level 93 Slayer, Facemask or Slayer helmet",
			"Protects against toxic smoke; boss requires Slayer task"));
		reqs.put("Thermonuclear Smoke Devil (boss)", List.of(
			"Requires: Level 93 Slayer, Facemask or Slayer helmet",
			"Protects against toxic smoke; boss requires Slayer task"));
		reqs.put("King Black Dragon Lair", List.of(
			"Requires: Slash weapon for webs (Wilderness lvl 44)",
			"Anti-dragon shield and Antifire potions strongly recommended"));
		reqs.put("KBD Lair", List.of(
			"Requires: Slash weapon for webs (Wilderness lvl 44)",
			"Anti-dragon shield and Antifire potions strongly recommended"));
		reqs.put("Revenant Caves", List.of(
			"Requires: 100,000 coins entry fee (Wilderness level 17–40)",
			"Bracelet of ethereum protects against revenant aggression"));
		reqs.put("Wilderness God Wars Dungeon", List.of(
			"Requires: 60 Strength or 60 Agility, plus a Rope (Wilderness lvl 28)",
			"Ecumenical key or 40 killcount required for boss rooms"));
		reqs.put("Wilderness GWD", List.of(
			"Requires: 60 Strength or 60 Agility, plus a Rope (Wilderness lvl 28)",
			"Ecumenical key or 40 killcount required for boss rooms"));
		reqs.put("Ruins of Camdozaal", List.of(
			"Requires: Below Ice Mountain quest completed",
			"Barronite mace, mining, fishing, and Golem activities below"));
		reqs.put("Camdozaal", List.of(
			"Requires: Below Ice Mountain quest completed",
			"Barronite mace, mining, fishing, and Golem activities below"));
		reqs.put("Cam Torum", List.of(
			"Requires: Children of the Sun & Twilight's Promise quests",
			"Underground Varlamore settlement leading to Neypotzli"));
		reqs.put("Neypotzli", List.of(
			"Requires: Perilous Moons quest started or completed",
			"Moons of Peril boss encounters and Lunar Chest inside"));
		reqs.put("Cam Torum / Neypotzli", List.of(
			"Requires: Children of the Sun & Perilous Moons quests",
			"Underground Varlamore city and Moons of Peril dungeon"));
		reqs.put("Tombs of Amascut", List.of(
			"Requires: Beneath Cursed Sands quest completed",
			"Raid party lobby for Tombs of Amascut"));
		reqs.put("Tombs of Amascut (ToA)", List.of(
			"Requires: Beneath Cursed Sands quest completed",
			"Raid party lobby for Tombs of Amascut"));
		reqs.put("ToA", List.of(
			"Requires: Beneath Cursed Sands quest completed",
			"Raid party lobby for Tombs of Amascut"));
		reqs.put("Theatre of Blood", List.of(
			"Requires: Priest in Peril and A Taste of Hope (Morytania access)",
			"Raid party lobby for Theatre of Blood"));
		reqs.put("Theatre of Blood (ToB)", List.of(
			"Requires: Priest in Peril and A Taste of Hope (Morytania access)",
			"Raid party lobby for Theatre of Blood"));
		reqs.put("ToB", List.of(
			"Requires: Priest in Peril and A Taste of Hope (Morytania access)",
			"Raid party lobby for Theatre of Blood"));
		reqs.put("Volcanic Mine", List.of(
			"Requires: Bone Voyage quest, 150 Museum Kudos, 50 Mining",
			"Unfiltered Fossil Island minigame access"));
		reqs.put("Sisterhood Sanctuary", List.of(
			"Requires: Priest in Peril (Morytania & Slepe access)",
			"The Nightmare of Ashihama boss encounter below"));
		reqs.put("The Nightmare", List.of(
			"Requires: Priest in Peril (Morytania & Slepe access)",
			"The Nightmare of Ashihama boss encounter below"));
		reqs.put("Sisterhood Sanctuary (Nightmare)", List.of(
			"Requires: Priest in Peril (Morytania & Slepe access)",
			"The Nightmare of Ashihama boss encounter below"));
		reqs.put("Underground Pass", List.of(
			"Requires: Biohazard / Underground Pass quest started",
			"Rope, Bow & Arrows, and Agility boosts recommended"));
		reqs.put("Temple of Ikov", List.of(
			"Requires: Pendant of Lucien, Light source, Bow and Ice arrows",
			"Temple of Ikov quest"));
		reqs.put("Lumbridge Swamp Caves", List.of(
			"Requires: Light source (protected), Rope or Spade",
			"Spiny helmet recommended against Wall beasts"));
		reqs.put("Ice Queen's Lair", List.of(
			"Requires: 50 Mining (Heroes' Quest)",
			"Ice gloves strongly recommended"));
		reqs.put("Troll Stronghold", List.of(
			"Requires: Death Plateau quest, Climbing boots",
			"Troll Stronghold quest and God Wars route"));
		reqs.put("Salt Mine", List.of(
			"Requires: Making Friends with My Arm, 60 Mining, 72 Firemaking",
			"Basalt and salt mining for icy portal nexus"));
		reqs.put("Mage Arena Bank", List.of(
			"Requires: Knife or slash weapon to cut webs (Wilderness lvl 51)",
			"Kolodion's Mage Arena and god cape arena"));
		reqs.put("Mage Arena", List.of(
			"Requires: Knife or slash weapon to cut webs (Wilderness lvl 51)",
			"Kolodion's Mage Arena and god cape arena"));
		reqs.put("Scabaras Dungeon", List.of(
			"Requires: Contact! quest started, Light source, Tinderbox",
			"Waterskins and antipoison recommended for desert dungeon"));
		reqs.put("Scabaras", List.of(
			"Requires: Contact! quest started, Light source, Tinderbox",
			"Waterskins and antipoison recommended for desert dungeon"));
		reqs.put("Slayer Tower basement", List.of(
			"Requires: Slayer Tower basement key",
			"High-level Slayer monsters inside"));
		reqs.put("Slayer Tower Basement", List.of(
			"Requires: Slayer Tower basement key",
			"High-level Slayer monsters inside"));
		reqs.put("Mining Guild basement", List.of(
			"Requires: Level 60 Mining",
			"Amethyst crystals and superior mineral veins"));
		reqs.put("Mining Guild Basement", List.of(
			"Requires: Level 60 Mining",
			"Amethyst crystals and superior mineral veins"));
		reqs.put("Heroes' Guild mine", List.of(
			"Requires: Heroes' Quest, 50 Mining",
			"Runite and Mithril rocks below"));
		reqs.put("Heroes' Guild Mine", List.of(
			"Requires: Heroes' Quest, 50 Mining",
			"Runite and Mithril rocks below"));
		reqs.put("Legends' Guild dungeon", List.of(
			"Requires: Legends' Quest started or completed",
			"Shadow warriors and dungeon access"));
		reqs.put("Legends' Guild Dungeon", List.of(
			"Requires: Legends' Quest started or completed",
			"Shadow warriors and dungeon access"));
		reqs.put("Legends' Guild", List.of(
			"Requires: Legends' Quest started or completed",
			"Shadow warriors and dungeon access"));
		reqs.put("Woodcutting Guild dungeon", List.of(
			"Requires: 60 Woodcutting, 75% Hosidius favour",
			"Ents and Woodcutting dungeon below"));
		reqs.put("Woodcutting Guild Dungeon", List.of(
			"Requires: 60 Woodcutting, 75% Hosidius favour",
			"Ents and Woodcutting dungeon below"));
		reqs.put("WC Guild dungeon", List.of(
			"Requires: 60 Woodcutting, 75% Hosidius favour",
			"Ents and Woodcutting dungeon below"));
		reqs.put("Wizards' Guild basement", List.of(
			"Requires: Level 66 Magic (Magic guild entrance)",
			"Zombies and rune portals in Yanille"));
		reqs.put("Wizards' Guild Basement", List.of(
			"Requires: Level 66 Magic (Magic guild entrance)",
			"Zombies and rune portals in Yanille"));
		reqs.put("Ourania Cave", List.of(
			"Requires: Food, combat/prayer defence recommended",
			"Ourania / ZMI runecrafting altar and banking Zamorakian monks"));
		reqs.put("Ourania Cave (ZMI)", List.of(
			"Requires: Food, combat/prayer defence recommended",
			"Ourania / ZMI runecrafting altar and banking Zamorakian monks"));
		reqs.put("ZMI", List.of(
			"Requires: Food, combat/prayer defence recommended",
			"Ourania / ZMI runecrafting altar and banking Zamorakian monks"));
		reqs.put("Sourhog Cave", List.of(
			"Requires: A Porcine of Interest quest",
			"Spiny helmet or Slayer helmet required against sourhogs"));
		reqs.put("Lighthouse basement", List.of(
			"Requires: Horror from the Deep quest, Light source",
			"Dagannoth training cave"));
		reqs.put("Lighthouse Basement", List.of(
			"Requires: Horror from the Deep quest, Light source",
			"Dagannoth training cave"));
		reqs.put("Hallowed Sepulchre", List.of(
			"Requires: Sins of the Father quest completed",
			"Agility levels 52–92 (Floors 1–5)",
			"Hallowed marks, Hallowed tools, Ring of endurance"));
		reqs.put("Lunar Isle Mine", List.of(
			"Requires: Lunar Diplomacy quest started / Fremennik access, Pickaxe",
			"Lunar ore, gem rocks, and rune essence mine under Lunar Isle"));
		reqs.put("Scorpia Cave", List.of(
			"Requires: Slash weapon for webs, Antipoison, combat gear (Wilderness lvl 54)",
			"Scorpia boss lair and Scorpia's offspring"));
		reqs.put("Scorpia cave", List.of(
			"Requires: Slash weapon for webs, Antipoison, combat gear (Wilderness lvl 54)",
			"Scorpia boss lair and Scorpia's offspring"));
		reqs.put("Silk Chasm", List.of(
			"Requires: Wilderness access (lvl 35), Crush weapon / Magic, Antipoison",
			"Spindel (solitary Venenatis) boss lair"));
		reqs.put("Escape Caves", List.of(
			"Requires: Wilderness access (lvl 21-40), Wilderness Slayer / bossing gear",
			"Wilderness escape network connecting boss lairs and deep escape exits"));
		reqs.put("Callisto's Den", List.of(
			"Requires: Wilderness access (lvl 40), Magic / Ranged combat gear",
			"Callisto boss lair in the Wilderness"));
		reqs.put("Callisto Den", List.of(
			"Requires: Wilderness access (lvl 40), Magic / Ranged combat gear",
			"Callisto boss lair in the Wilderness"));
		reqs.put("Vet'ion's Rest", List.of(
			"Requires: Wilderness access (lvl 35), Crush weapon, Salve amulet",
			"Vet'ion boss lair in the Wilderness"));
		reqs.put("Vetions Rest", List.of(
			"Requires: Wilderness access (lvl 35), Crush weapon, Salve amulet",
			"Vet'ion boss lair in the Wilderness"));
		reqs.put("Zemouregal's Base", List.of(
			"Requires: Defender of Varrock quest, Hartwin / Arrav",
			"Armoured zombies training and Zemouregal's fortress underneath Silvarea"));
		reqs.put("Shade Catacombs", List.of(
			"Requires: Shades of Mort'ton quest, Shade keys, Coffin / Oil",
			"Chests, shade remains, and pyre burning rewards under Mort'ton"));
		reqs.put("Giants' Foundry", List.of(
			"Requires: Sleeping Giants quest, Kovac, Moulds and metal bars",
			"Giants' Foundry minigame: smith colossal weapons for rewards"));
		reqs.put("Giants Foundry", List.of(
			"Requires: Sleeping Giants quest, Kovac, Moulds and metal bars",
			"Giants' Foundry minigame: smith colossal weapons for rewards"));
		reqs.put("Ice Troll Caves", List.of(
			"Requires: The Fremennik Isles quest, Combat gear",
			"Neitiznot Ice Troll cave network and Ice Troll King link"));
		reqs.put("Ice Troll Dungeon", List.of(
			"Requires: The Fremennik Isles quest, Combat gear",
			"Neitiznot Ice Troll cave network and Ice Troll King link"));
		reqs.put("Werewolf Agility Course", List.of(
			"Requires: Creature of Fenkenstrain, Ring of Charos",
			"Level 60 Agility (730 XP per lap with stick)"));
		return Collections.unmodifiableMap(reqs);
	}

	private static boolean isDungeonKey(String key)
	{
		return "dungeon".equals(key) || "dungeon_link".equals(key) || "basement".equals(key);
	}

	private static boolean typeMatches(String entryType, String typeHint)
	{
		if (typeHint == null || typeHint.equals(entryType))
		{
			return true;
		}
		return isDungeonKey(typeHint) && isDungeonKey(entryType);
	}

	private static Detail entryToDetail(Entry entry)
	{
		List<String> lines = entry.lines;
		if (isDungeonKey(entry.type))
		{
			final List<String> specific = DUNGEON_REQUIREMENTS.get(entry.title);
			if (specific != null)
			{
				lines = specific;
			}
		}
		return new Detail(entry.title, entry.category, lines);
	}

	private static void addEntry(String type, String title, String category, int x, int y, int plane, String... lines)
	{
		// Basements and cellars use the link marker, not the red dungeon marker.
		if ("dungeon".equals(type) && (title.toLowerCase(Locale.ROOT).contains("basement")
			|| title.toLowerCase(Locale.ROOT).contains("cellar")))
		{
			type = "basement";
		}
		final String key = type + '\u0000' + title + '\u0000' + pointKey(x, y, plane);
		if (!ENTRY_KEYS.add(key))
		{
			return;
		}
		final List<String> lineList = new ArrayList<>(lines.length);
		for (String l : lines)
		{
			if (l != null && !l.isEmpty())
			{
				lineList.add(l);
			}
		}
		final Entry entry = new Entry(type, title, category, Collections.unmodifiableList(lineList), x, y, plane);
		ENTRIES.add(entry);
		EXACT_MAP.put(pointKey(x, y, plane), entry);
		CHUNK_MAP.computeIfAbsent(chunkKey(plane, x >> 6, y >> 6), k -> new ArrayList<>()).add(entry);
	}

	static
	{
		addRareTrees();
		addDungeons();
		addAgilityCourses();
		addHunterAreas();
		addSailingAreas();
		addFarmingPatches();
		addFishingSpots();
		addMiningSites();
		addTransportLinks();
		addQuestStarts();
	}

	/** Woodcutting spots for the trees worth walking to. */
	private static void addRareTrees()
	{
		addEntry("rare_trees", "Willow tree (Level 30)", "Skilling • Woodcutting", 2710, 3510, 0, "Level requirement: Level 30 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Teak tree (Level 35)", "Skilling • Woodcutting", 2774, 2697, 0, "Level requirement: Level 35 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Teak tree (Level 35)", "Skilling • Woodcutting", 3510, 3073, 0, "Level requirement: Level 35 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Teak tree (Level 35)", "Skilling • Woodcutting", 3440, 2789, 0, "Level requirement: Level 35 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Teak tree (Level 35)", "Skilling • Woodcutting", 3129, 2446, 0, "Level requirement: Level 35 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Teak tree (Level 35)", "Skilling • Woodcutting", 3832, 3067, 0, "Level requirement: Level 35 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Teak tree (Level 35)", "Skilling • Woodcutting", 2837, 2907, 0, "Level requirement: Level 35 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Teak tree (Level 35)", "Skilling • Woodcutting", 2899, 2897, 0, "Level requirement: Level 35 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Teak tree (Level 35)", "Skilling • Woodcutting", 2333, 3049, 0, "Level requirement: Level 35 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Teak tree (Level 35)", "Skilling • Woodcutting", 3309, 6123, 0, "Level requirement: Level 35 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Teak tree (Level 35)", "Skilling • Woodcutting", 1540, 2802, 0, "Level requirement: Level 35 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Teak tree (Level 35)", "Skilling • Woodcutting", 1694, 2989, 0, "Level requirement: Level 35 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Teak tree (Level 35)", "Skilling • Woodcutting", 1363, 3143, 0, "Level requirement: Level 35 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Teak tree (Level 35)", "Skilling • Woodcutting", 1326, 3169, 0, "Level requirement: Level 35 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Teak tree (Level 35)", "Skilling • Woodcutting", 1257, 3009, 0, "Level requirement: Level 35 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Swaying tree (Level 40)", "Skilling • Woodcutting", 2738, 3639, 0, "Level requirement: Level 40 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Mature juniper tree (Level 42)", "Skilling • Woodcutting", 1690, 3524, 0, "Level requirement: Level 42 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Maple tree (Level 45)", "Skilling • Woodcutting", 1231, 3704, 0, "Level requirement: Level 45 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Maple tree (Level 45)", "Skilling • Woodcutting", 1293, 3756, 0, "Level requirement: Level 45 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Maple tree (Level 45)", "Skilling • Woodcutting", 1640, 3496, 0, "Level requirement: Level 45 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Maple tree (Level 45)", "Skilling • Woodcutting", 1613, 3494, 0, "Level requirement: Level 45 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Maple tree (Level 45)", "Skilling • Woodcutting", 1560, 3636, 0, "Level requirement: Level 45 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Maple tree (Level 45)", "Skilling • Woodcutting", 1646, 3590, 0, "Level requirement: Level 45 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Maple tree (Level 45)", "Skilling • Woodcutting", 2550, 3869, 0, "Level requirement: Level 45 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Maple tree (Level 45)", "Skilling • Woodcutting", 2712, 3382, 0, "Level requirement: Level 45 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Maple tree (Level 45)", "Skilling • Woodcutting", 2720, 3465, 0, "Level requirement: Level 45 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Maple tree (Level 45)", "Skilling • Woodcutting", 2726, 3501, 0, "Level requirement: Level 45 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Maple tree (Level 45)", "Skilling • Woodcutting", 2728, 3481, 0, "Level requirement: Level 45 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Maple tree (Level 45)", "Skilling • Woodcutting", 2748, 3466, 0, "Level requirement: Level 45 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Maple tree (Level 45)", "Skilling • Woodcutting", 2710, 3570, 0, "Level requirement: Level 45 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Maple tree (Level 45)", "Skilling • Woodcutting", 2254, 2808, 0, "Level requirement: Level 45 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Maple tree (Level 45)", "Skilling • Woodcutting", 2209, 3427, 0, "Level requirement: Level 45 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Maple tree (Level 45)", "Skilling • Woodcutting", 3233, 6179, 0, "Level requirement: Level 45 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Maple tree (Level 45)", "Skilling • Woodcutting", 1362, 2862, 0, "Level requirement: Level 45 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Maple tree (Level 45)", "Skilling • Woodcutting", 1398, 2856, 0, "Level requirement: Level 45 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Maple tree (Level 45)", "Skilling • Woodcutting", 1389, 3381, 0, "Level requirement: Level 45 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Maple tree (Level 45)", "Skilling • Woodcutting", 1370, 3344, 0, "Level requirement: Level 45 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Maple tree (Level 45)", "Skilling • Woodcutting", 1392, 3339, 0, "Level requirement: Level 45 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Maple tree (Level 45)", "Skilling • Woodcutting", 1431, 3352, 0, "Level requirement: Level 45 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Mahogany tree (Level 50)", "Skilling • Woodcutting", 1237, 3770, 0, "Level requirement: Level 50 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Mahogany tree (Level 50)", "Skilling • Woodcutting", 2716, 2710, 0, "Level requirement: Level 50 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Mahogany tree (Level 50)", "Skilling • Woodcutting", 2725, 2735, 0, "Level requirement: Level 50 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Mahogany tree (Level 50)", "Skilling • Woodcutting", 3824, 3053, 0, "Level requirement: Level 50 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Mahogany tree (Level 50)", "Skilling • Woodcutting", 2946, 2908, 0, "Level requirement: Level 50 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Mahogany tree (Level 50)", "Skilling • Woodcutting", 3301, 6129, 0, "Level requirement: Level 50 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Mahogany tree (Level 50)", "Skilling • Woodcutting", 1586, 2786, 0, "Level requirement: Level 50 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Mahogany tree (Level 50)", "Skilling • Woodcutting", 1249, 3077, 0, "Level requirement: Level 50 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Mahogany tree (Level 50)", "Skilling • Woodcutting", 1263, 3002, 0, "Level requirement: Level 50 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Teak/Mahogany trees (Level 50)", "Skilling • Woodcutting", 2602, 3895, 0, "Level requirement: Level 50 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Teak/Mahogany trees (Level 50)", "Skilling • Woodcutting", 3810, 3058, 0, "Level requirement: Level 50 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Teak/Mahogany trees (Level 50)", "Skilling • Woodcutting", 2194, 2991, 0, "Level requirement: Level 50 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Teak/Mahogany trees (Level 50)", "Skilling • Woodcutting", 2821, 3084, 0, "Level requirement: Level 50 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 1217, 3688, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 1353, 3731, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 1529, 3452, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 1591, 3421, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 1647, 3508, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 1621, 3512, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 1593, 3491, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 1583, 3499, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 1696, 3554, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 1625, 3677, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 1625, 3669, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 1642, 3683, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 1642, 3663, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 1642, 3533, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 1671, 3657, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 1680, 3657, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 1572, 3618, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 1553, 3551, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 1496, 3582, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 1733, 3119, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 1749, 3106, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 1498, 3245, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 1296, 3409, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 1361, 3320, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 1380, 3323, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 1354, 3289, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 1412, 2992, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 2217, 3141, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 3288, 6066, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 3305, 6032, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 2315, 3610, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 2331, 3514, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 2358, 3510, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 2360, 3471, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 2434, 3432, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 2493, 3396, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 2495, 3490, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 2733, 3334, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 2757, 3431, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 2714, 3460, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 2995, 3312, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 3018, 3316, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 3041, 3320, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 3052, 3272, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 2931, 3231, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 3085, 3481, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 3085, 3469, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 3146, 3255, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 3151, 3231, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 3165, 3220, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 3184, 3227, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 3251, 3364, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 3204, 3504, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 3208, 3500, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 3221, 3503, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 3248, 3473, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 3266, 3494, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 3270, 3471, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 3304, 3470, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 3249, 3202, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 3674, 3447, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 3684, 3385, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 2147, 2972, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 2165, 2863, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 2340, 2762, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 2077, 2707, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 1950, 2786, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 1224, 2781, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 3276, 2339, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 2412, 4464, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 2465, 4427, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew tree (Level 60)", "Skilling • Woodcutting", 2491, 4426, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew/Jatoba trees (Level 60)", "Skilling • Woodcutting", 3144, 2520, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Willow/Yew trees (Level 60)", "Skilling • Woodcutting", 2476, 2707, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Maple/Yew trees (Level 60)", "Skilling • Woodcutting", 2476, 2893, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Maple/Yew trees (Level 60)", "Skilling • Woodcutting", 1379, 2876, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Maple/Yew trees (Level 60)", "Skilling • Woodcutting", 2086, 2737, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Maple/Yew trees (Level 60)", "Skilling • Woodcutting", 2074, 2723, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Maple/Yew trees (Level 60)", "Skilling • Woodcutting", 2061, 2720, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Maple/Yew trees (Level 60)", "Skilling • Woodcutting", 1358, 3302, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Blisterwood tree (Level 62)", "Skilling • Woodcutting", 3631, 3362, 0, "Level requirement: Level 62 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Sulliuscep (Level 65)", "Skilling • Woodcutting", 3662, 3802, 0, "Level requirement: Level 65 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Sulliuscep (Level 65)", "Skilling • Woodcutting", 3662, 3781, 0, "Level requirement: Level 65 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Sulliuscep (Level 65)", "Skilling • Woodcutting", 3677, 3806, 0, "Level requirement: Level 65 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Sulliuscep (Level 65)", "Skilling • Woodcutting", 3677, 3733, 0, "Level requirement: Level 65 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Sulliuscep (Level 65)", "Skilling • Woodcutting", 3682, 3775, 0, "Level requirement: Level 65 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Sulliuscep (Level 65)", "Skilling • Woodcutting", 3682, 3758, 0, "Level requirement: Level 65 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Camphor tree (Level 66)", "Skilling • Woodcutting", 3192, 2487, 0, "Level requirement: Level 66 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Camphor tree (Level 66)", "Skilling • Woodcutting", 3204, 2481, 0, "Level requirement: Level 66 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Camphor/Mahogany trees (Level 66)", "Skilling • Woodcutting", 3245, 2397, 0, "Level requirement: Level 66 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Camphor/Jatoba trees (Level 66)", "Skilling • Woodcutting", 3109, 2413, 0, "Level requirement: Level 66 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Magic tree (Level 75)", "Skilling • Woodcutting", 1373, 3821, 0, "Level requirement: Level 75 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Magic tree (Level 75)", "Skilling • Woodcutting", 1389, 3821, 0, "Level requirement: Level 75 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Magic tree (Level 75)", "Skilling • Woodcutting", 1610, 3443, 0, "Level requirement: Level 75 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Magic tree (Level 75)", "Skilling • Woodcutting", 1578, 3488, 0, "Level requirement: Level 75 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Magic tree (Level 75)", "Skilling • Woodcutting", 1685, 3740, 0, "Level requirement: Level 75 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Magic tree (Level 75)", "Skilling • Woodcutting", 1681, 3689, 0, "Level requirement: Level 75 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Magic tree (Level 75)", "Skilling • Woodcutting", 1751, 3564, 0, "Level requirement: Level 75 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Magic tree (Level 75)", "Skilling • Woodcutting", 1796, 3600, 0, "Level requirement: Level 75 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Magic tree (Level 75)", "Skilling • Woodcutting", 1394, 3164, 0, "Level requirement: Level 75 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Magic tree (Level 75)", "Skilling • Woodcutting", 1323, 2974, 0, "Level requirement: Level 75 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Magic tree (Level 75)", "Skilling • Woodcutting", 1455, 3321, 0, "Level requirement: Level 75 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Magic tree (Level 75)", "Skilling • Woodcutting", 1387, 3281, 0, "Level requirement: Level 75 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Magic tree (Level 75)", "Skilling • Woodcutting", 1328, 3059, 0, "Level requirement: Level 75 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Magic tree (Level 75)", "Skilling • Woodcutting", 1328, 3033, 0, "Level requirement: Level 75 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Magic tree (Level 75)", "Skilling • Woodcutting", 3355, 3312, 0, "Level requirement: Level 75 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Magic tree (Level 75)", "Skilling • Woodcutting", 3368, 3312, 0, "Level requirement: Level 75 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Magic tree (Level 75)", "Skilling • Woodcutting", 2284, 3141, 0, "Level requirement: Level 75 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Magic tree (Level 75)", "Skilling • Woodcutting", 3229, 6101, 0, "Level requirement: Level 75 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Magic tree (Level 75)", "Skilling • Woodcutting", 2371, 3427, 0, "Level requirement: Level 75 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Magic tree (Level 75)", "Skilling • Woodcutting", 2432, 3411, 0, "Level requirement: Level 75 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Magic tree (Level 75)", "Skilling • Woodcutting", 2490, 3414, 0, "Level requirement: Level 75 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Magic tree (Level 75)", "Skilling • Woodcutting", 2704, 3397, 0, "Level requirement: Level 75 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Magic tree (Level 75)", "Skilling • Woodcutting", 2695, 3423, 0, "Level requirement: Level 75 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Magic tree (Level 75)", "Skilling • Woodcutting", 2443, 2845, 0, "Level requirement: Level 75 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Magic tree (Level 75)", "Skilling • Woodcutting", 3043, 2640, 0, "Level requirement: Level 75 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Magic tree (Level 75)", "Skilling • Woodcutting", 2090, 3178, 0, "Level requirement: Level 75 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Magic tree (Level 75)", "Skilling • Woodcutting", 3175, 3931, 0, "Level requirement: Level 75 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Bloodwood tree (Level 77)", "Skilling • Woodcutting", 2599, 7780, 0, "Level requirement: Level 77 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Bloodwood tree (Level 77)", "Skilling • Woodcutting", 2696, 7823, 0, "Level requirement: Level 77 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Ironwood tree (Level 80)", "Skilling • Woodcutting", 2223, 2341, 0, "Level requirement: Level 80 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Ironwood tree (Level 80)", "Skilling • Woodcutting", 2221, 2322, 0, "Level requirement: Level 80 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Ironwood tree (Level 80)", "Skilling • Woodcutting", 2205, 2321, 0, "Level requirement: Level 80 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Redwood tree (Level 90)", "Skilling • Woodcutting", 1569, 3493, 0, "Level requirement: Level 90 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Redwood tree (Level 90)", "Skilling • Woodcutting", 1569, 3483, 0, "Level requirement: Level 90 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
		addEntry("rare_trees", "Yew trees (Level 60)", "Skilling • Woodcutting", 2543, 2184, 0, "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Yield: Logs, Birds' nests");
	}

	/**
	 * Dungeon and underground-area markers.
	 *
	 * <p>Surface rows (Y typically &lt; 4200) are overworld entrances. A second block at the end
	 * places matching interior pins on the dungeon / underground map layer at native underground
	 * coordinates (zone anchors, +6400 mirrors, or gap-island instances).
	 */
	private static void addDungeons()
	{
		addEntry("dungeon", "Abandoned Mine", "Dungeons", 3439, 3232, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Abandoned Mine (secret entrance)", "Dungeons", 3452, 3244, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Wine Cellar", "Dungeons", 1447, 2938, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Backstage Entrance", "Dungeons", 1475, 2927, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Alchemical Society", "Dungeons", 1389, 2918, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Moonrise Brewery and Winery", "Dungeons", 1356, 2920, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Ancient Cavern", "Dungeons", 2511, 3508, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Ape Atoll Dungeon", "Dungeons", 2762, 2703, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Ardougne Castle basement", "Dungeons", 2569, 3296, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Ardougne Rat Pits", "Dungeons", 2560, 3320, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Ardougne Sewers", "Dungeons", 2631, 3294, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Ardougne Sewers", "Dungeons", 2586, 3235, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Ardougne Sewers", "Dungeons", 2528, 3303, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Asgarnian Ice Dungeon", "Dungeons", 3007, 3150, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Barbarian camp jail", "Dungeons", 1581, 3428, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Brimhaven Agility Arena", "Dungeons", 2808, 3194, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Brimhaven Dungeon", "Dungeons", 2743, 3154, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Brimhaven Dungeon", "Dungeons", 2759, 3062, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Brimstail's Cave", "Dungeons", 2402, 3419, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Callisto's Den", "Dungeons", 3292, 3850, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Cam Torum", "Dungeons", 1435, 3131, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Barracks", "Dungeons", 1640, 3166, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Crypt", "Dungeons", 1692, 3089, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Crypt", "Dungeons", 1692, 3084, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Tunnel", "Dungeons", 1613, 3174, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Tunnel", "Dungeons", 1622, 3165, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Catacombs of Kourend", "Dungeons", 1636, 3673, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Chambers of Xeric", "Dungeons", 1232, 3573, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Champions' Challenge", "Dungeons", 3188, 3355, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Chaos Druid Tower dungeon", "Dungeons", 2561, 3356, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Chasm of Fire", "Dungeons", 1432, 3670, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Clock Tower Dungeon", "Dungeons", 2568, 3229, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Corporeal Beast cave", "Dungeons", 3202, 3681, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Corsair Cove Dungeon", "Dungeons", 2522, 2861, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Corsair Cove Dungeon", "Dungeons", 2482, 2891, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Crabclaw Caves", "Dungeons", 1643, 3449, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Crandor Dungeon", "Dungeons", 2833, 3256, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Crumbling Tower basement", "Dungeons", 2130, 2994, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Cave passage", "Dungeons", 1270, 3435, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Cave passage", "Dungeons", 1258, 3431, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Draynor Manor basement", "Dungeons", 3114, 3357, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Draynor Manor basement", "Dungeons", 3091, 3362, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Draynor Sewers", "Dungeons", 3083, 3272, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Draynor Sewers", "Dungeons", 3117, 3244, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Digsite Dungeon", "Dungeons", 3370, 3428, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Dwarven Mine", "Dungeons", 3018, 3450, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Dwarven Mine", "Dungeons", 3058, 3377, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Ectofuntus dungeon", "Dungeons", 3651, 3519, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Edgeville Dungeon", "Dungeons", 3096, 3469, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Edgeville Dungeon", "Dungeons", 3115, 3452, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Edgeville Dungeon", "Dungeons", 3087, 3571, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Elemental Workshop", "Dungeons", 2709, 3498, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Entrana Dungeon", "Dungeons", 2819, 3374, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Escape Caves", "Dungeons", 3260, 3833, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Escape Caves", "Dungeons", 3320, 3831, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Escape Caves", "Dungeons", 3284, 3808, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Escape Caves", "Dungeons", 3282, 3775, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Miscellania and Etceteria Dungeon", "Dungeons", 2619, 3865, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Mole Hole", "Dungeons", 2985, 3387, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Mole Hole", "Dungeons", 2997, 3376, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Ferox Enclave Dungeon", "Dungeons", 3153, 3643, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Forsaken Tower basement", "Dungeons", 1381, 3825, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Forthos Dungeon", "Dungeons", 1701, 3574, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Forthos Dungeon", "Dungeons", 1669, 3567, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Fortis Colosseum", "Dungeons", 1824, 3107, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Fortis Barracks", "Dungeons", 1640, 3166, 0, "Type: Dungeon entrance / underground link", "Subterranean barracks & training tunnels under Civitas illa Fortis");
		addEntry("dungeon", "Fremennik Slayer Dungeon", "Dungeons", 2796, 3615, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "The Gauntlet", "Dungeons", 3227, 6116, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Giants' Den", "Dungeons", 1419, 3588, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Giants' Foundry", "Dungeons", 3360, 3150, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Glarial's Tomb", "Dungeons", 2557, 3444, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Goblin Cave", "Dungeons", 2622, 3393, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "God Wars Dungeon", "Dungeons", 2917, 3747, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Gryphon Dungeon", "Dungeons", 3120, 2490, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Gryphon Dungeon", "Dungeons", 3234, 2418, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "H.A.M. Hideout", "Dungeons", 3164, 3252, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Headless Beast Cave", "Dungeons", 1211, 3647, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Heroes' Guild mine", "Dungeons", 2893, 3507, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Hespori Cave", "Dungeons", 1248, 3737, 0, "Type: Dungeon entrance / underground link", "Farming Guild Hespori cave entrance");
		addEntry("dungeon", "Hespori Patch", "Dungeons", 1231, 3729, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Hunter's End", "Dungeons", 3115, 3677, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Hunter Guild Caverns", "Dungeons", 1557, 3049, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Jaldraocht Pyramid", "Dungeons", 3233, 2898, 0, "Type: Dungeon entrance / underground link", "Desert Treasure I pyramid entrance");
		addEntry("dungeon", "Lithkren Vault", "Dungeons", 3555, 4002, 0, "Type: Dungeon entrance / underground link", "Dragon Slayer II vault entrance");
		addEntry("dungeon", "Weiss Salt Mine", "Dungeons", 2869, 3941, 0, "Type: Dungeon entrance / underground link", "Making Friends with My Arm salt mine entrance");
		addEntry("dungeon", "Shadow Dungeon", "Dungeons", 2547, 3421, 0, "Type: Dungeon entrance / underground link", "Desert Treasure I shadow dungeon entrance");
		addEntry("dungeon", "Enakhra's Temple", "Dungeons", 3194, 2925, 0, "Type: Dungeon entrance / underground link", "Enakhra's Lament temple secret entrance");
		addEntry("dungeon", "Ice Queen's Lair", "Dungeons", 2846, 3516, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Ice Queen's Lair", "Dungeons", 2856, 3519, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Ice Queen's Lair", "Dungeons", 2822, 3510, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Ice Troll Caves", "Dungeons", 2400, 3889, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Ice Troll Caves", "Dungeons", 2315, 3894, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Isle of Souls Dungeon", "Dungeons", 2135, 9320, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Iorwerth Dungeon", "Dungeons", 3224, 6044, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Iorwerth Camp cave", "Dungeons", 2200, 3262, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Jogre Dungeon", "Dungeons", 2824, 3118, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Kalphite Cave", "Dungeons", 3319, 3122, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Kalphite Lair", "Dungeons", 3226, 3108, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Karamja Dungeon", "Dungeons", 2855, 3168, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Karuulm Slayer Dungeon", "Dungeons", 1308, 3807, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Kasonde Shaw's Hideout", "Dungeons", 1191, 3411, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "King Black Dragon Lair", "Dungeons", 3016, 3849, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Keldagrim Entrance", "Dungeons", 2795, 3718, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Klenter's Pyramid", "Dungeons", 3294, 2780, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Kraken Cove", "Dungeons", 2277, 3611, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Lava Maze Dungeon", "Dungeons", 3068, 3856, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Legends' Guild dungeon", "Dungeons", 2723, 3375, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Lighthouse basement", "Dungeons", 2508, 3644, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Lizardman Caves", "Dungeons", 1306, 3574, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Lizardman Temple", "Dungeons", 1214, 3559, 0, "Type: Dungeon entrance / underground link", "Lizardman Temple entrance at Mount Quidamortem");
		addEntry("dungeon", "Lizardman Temple", "Dungeons", 1329, 3669, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Lizardman Temple", "Dungeons", 1311, 3686, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Lizardman Temple", "Dungeons", 1313, 3663, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Lizardman Temple", "Dungeons", 1291, 3657, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Lumbridge Castle cellar", "Dungeons", 3208, 3218, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Lumbridge Swamp Caves", "Dungeons", 3168, 3172, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Lunar Isle Mine", "Dungeons", 2142, 3944, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Mage Arena Bank", "Dungeons", 3089, 3956, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Neypotzli", "Dungeons", 1440, 9600, 1, "Type: Dungeon entrance / underground link", "Entrance to Neypotzli & Moons of Peril");
		addEntry("dungeon", "Neypotzli", "Dungeons", 1439, 9660, 0, "Type: Dungeon interior (underground layer)", "Moons of Peril boss encounters and Lunar Chest inside");
		addEntry("dungeon", "Melzar's Maze basement", "Dungeons", 2924, 3250, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Mining Guild basement", "Dungeons", 3018, 3339, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Miscellania and Etceteria Dungeon", "Dungeons", 2508, 3846, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Mos Le'Harmless Cave", "Dungeons", 3747, 2973, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Mos Le'Harmless Cave", "Dungeons", 3829, 3062, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Mos Le'Harmless Cave", "Dungeons", 3814, 3062, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Motherlode Mine", "Dungeons", 3060, 3366, 0, "Type: Dungeon entrance / underground link", "Hop from Dwarven Mine cave 26654");
		addEntry("dungeon", "Motherlode Mine", "Dungeons", 3059, 9764, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Myreque Hideout", "Dungeons", 3494, 3464, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Myreque Hideout", "Dungeons", 3488, 3231, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Myreque Hideout", "Dungeons", 3508, 3448, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Myths' Guild dungeon", "Dungeons", 2456, 2847, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Myths' Guild Dungeon", "Dungeons", 2457, 2849, 0, "Type: Dungeon entrance (Mythic Statue)", "Wrath altar, green & blue dragons, adamant & rune dragons");
		addEntry("dungeon", "Myths' Guild dungeon", "Dungeons", 2444, 2819, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Observatory Dungeon", "Dungeons", 2458, 3186, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Observatory Dungeon", "Dungeons", 2436, 3163, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Ogre Enclave", "Dungeons", 2505, 3039, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Ourania Cave", "Dungeons", 2451, 3231, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Paterdomus basement", "Dungeons", 3404, 3506, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Paterdomus basement", "Dungeons", 3421, 3484, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Port Phasmatys Brewery", "Dungeons", 3679, 3498, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Port Sarim Rat Pits", "Dungeons", 3017, 3232, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Quidamortem Cave", "Dungeons", 1213, 3559, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Ranis Drakan's Crypt", "Dungeons", 3681, 3231, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Revenant Caves", "Dungeons", 3124, 3832, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Revenant Caves", "Dungeons", 3074, 3655, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Revenant Caves", "Dungeons", 3067, 3741, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "River Elid Dungeon", "Dungeons", 3370, 3132, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Ruins of Camdozaal", "Dungeons", 2998, 3493, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Salt Mine", "Dungeons", 2866, 3941, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Scabaras Dungeon", "Dungeons", 3407, 2848, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Skavid Caves", "Dungeons", 2521, 3070, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Skavid Caves", "Dungeons", 2540, 3054, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Skavid Caves", "Dungeons", 2552, 3054, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Skavid Caves", "Dungeons", 2551, 3035, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Skavid Caves (island)", "Dungeons", 2574, 3028, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Skavid Caves (island)", "Dungeons", 2500, 2991, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Skavid Caves", "Dungeons", 2528, 3014, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Skavid Caves", "Dungeons", 2561, 3025, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Scorpia cave", "Dungeons", 3244, 3949, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Scorpia cave", "Dungeons", 3231, 3952, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Scorpia cave", "Dungeons", 3232, 3936, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Scrubfoot's Cave", "Dungeons", 2946, 3507, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Shade Catacombs", "Dungeons", 3485, 3321, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Shayzien Crypts", "Dungeons", 1482, 3549, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Shayzien Crypts", "Dungeons", 1483, 3549, 0, "Type: Dungeon entrance / underground link", "Shayzien zombies and skeletal warriors under the cemetery");
		addEntry("dungeon", "Shayzien Crypt", "Dungeons", 1483, 3549, 0, "Type: Dungeon entrance / underground link", "Shayzien zombies and skeletal warriors under the cemetery");
		addEntry("dungeon", "Shayzien Prison", "Dungeons", 1464, 3569, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Shellbane Gryphon Cave", "Dungeons", 3175, 2478, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Shilo Village mine", "Dungeons", 2823, 3001, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Silk Chasm", "Dungeons", 3320, 3798, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Sisterhood Sanctuary", "Dungeons", 3727, 3300, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Sisterhood Sanctuary", "Dungeons", 3724, 3356, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Skeletal Tomb", "Dungeons", 3179, 3684, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Slayer Tower basement", "Dungeons", 3416, 3535, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Slepe basement", "Dungeons", 3718, 3307, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Smoke Devil Dungeon", "Dungeons", 2411, 3061, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Smoke Dungeon", "Dungeons", 3309, 2962, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Sophanem Dungeon", "Dungeons", 3314, 2797, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Sourhog Cave", "Dungeons", 3149, 3347, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Stalker Den", "Dungeons", 1296, 3374, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Stalker Den", "Dungeons", 1324, 3364, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Stronghold of Security", "Dungeons", 3080, 3420, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Stronghold Slayer Dungeon", "Dungeons", 2427, 3424, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Taverley Dungeon", "Dungeons", 2883, 3397, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Taverley Dungeon", "Dungeons", 2841, 3424, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Temple - Desert Treasure 2", "Dungeons", 1174, 3429, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Temple of Ikov", "Dungeons", 2676, 3404, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Temple of Ikov — McGrubor's Wood shortcut", "Dungeons", 2659, 3492, 0, "Type: Dungeon shortcut", "Requires: Shiny key to open the shed door");
		addEntry("dungeon", "Theatre of Blood", "Dungeons", 3676, 3219, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Dragon Nest", "Dungeons", 1289, 3134, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Passage to Gemstone Crab", "Dungeons", 1278, 3168, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Passage to Gemstone Crab", "Dungeons", 1351, 3124, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Passage to Gemstone Crab", "Dungeons", 1246, 3036, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Tonali Cavern", "Dungeons", 1309, 3104, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Tolna's rift", "Dungeons", 3308, 3450, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Tombs of Amascut", "Dungeons", 3357, 2711, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Tower of Life dungeon", "Dungeons", 2649, 3213, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Trahaearn Mine", "Dungeons", 3270, 6049, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Tree Gnome Village Dungeon", "Dungeons", 2532, 3155, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Troll Stronghold", "Dungeons", 2839, 3690, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Troll Stronghold", "Dungeons", 2831, 3677, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Troll Stronghold", "Dungeons", 2827, 3647, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Tutorial Island mine", "Dungeons", 3110, 3126, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Tutorial Island mine", "Dungeons", 3087, 3119, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Ruins of Tapoyauik", "Dungeons", 1693, 3231, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Ruins of Tapoyauik", "Dungeons", 1640, 3221, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "TzHaar City (Mor Ul Rek)", "Dungeons", 2496, 5140, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Underground Pass", "Dungeons", 2312, 3215, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Underground Pass", "Dungeons", 2433, 3315, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Underwater", "Dungeons", 3760, 3899, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Ruins of Uzer basement", "Dungeons", 3492, 3090, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Wolf Den", "Dungeons", 1496, 3132, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Varrock Sewers", "Dungeons", 3236, 3458, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Varrock Sewers", "Dungeons", 3229, 3504, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Vet'ion's Rest", "Dungeons", 3221, 3788, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Volcanic Mine", "Dungeons", 3815, 3808, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "VTAM Corporation", "Dungeons", 3243, 3383, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "The Warrens", "Dungeons", 1812, 3745, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Warriors' Guild basement", "Dungeons", 2832, 3542, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Waterfall Dungeon", "Dungeons", 2512, 3464, 0, "Type: Dungeon entrance / underground link", "Baxtorian Falls island • Waterfall Quest, Fire giants");
		addEntry("dungeon", "Eagles' Peak Dungeon", "Dungeons", 2328, 3496, 0, "Type: Dungeon entrance / underground link", "Eagles' Peak • Eagles' Peak Quest, Giant eagles");
		addEntry("dungeon", "Grand Tree Tunnels", "Dungeons", 2462, 3496, 0, "Type: Dungeon entrance / underground link", "Grand Tree base trapdoor • The Grand Tree quest");
		addEntry("dungeon", "Jiggig Dungeon", "Dungeons", 2443, 3051, 0, "Type: Dungeon entrance / underground link", "Jiggig cemetery crypt • Zogre Flesh Eaters, Slash Bash tomb");
		addEntry("dungeon", "Red Chinchompa Hunting Ground", "Dungeons", 2525, 2894, 0, "Type: Dungeon entrance / underground link", "Private red chinchompa cave • Western Provinces Hard Diary");
		addEntry("dungeon", "Jungle Eagle Lair", "Dungeons", 2518, 2921, 0, "Type: Dungeon entrance / underground link", "Eagle transport crevice • Eagles' Peak quest");
		addEntry("dungeon", "Shilo Village Gem Mine", "Dungeons", 2824, 2998, 0, "Type: Dungeon entrance / underground link", "Shilo Village gem mine ladder");
		addEntry("dungeon", "Rashiliyia's Tomb", "Dungeons", 2758, 2983, 0, "Type: Dungeon entrance / underground link", "Cairn Isle dolmen / Ah Za Rhoon entrance");
		addEntry("dungeon", "Crash Island Dungeon", "Dungeons", 2920, 2721, 0, "Type: Dungeon entrance / underground link", "Monkey Madness II • Crash Island cavern entrance");
		addEntry("dungeon", "Kharazi Caves", "Dungeons", 2838, 2845, 0, "Type: Dungeon entrance / underground link", "Legends' Quest • Kharazi jungle cave entrance");
		addEntry("dungeon", "Miscellania Underground", "Dungeons", 2512, 3865, 0, "Type: Dungeon entrance / underground link", "Miscellania Castle trapdoor • Royal Trouble");
		addEntry("dungeon", "Jatizso Mine", "Dungeons", 2398, 3813, 0, "Type: Dungeon entrance / underground link", "Jatizso mining tunnel entrance • Fremennik Isles");
		addEntry("dungeon", "Keldagrim Entrance", "Dungeons", 2730, 3713, 0, "Type: Dungeon entrance / underground link", "Keldagrim mountain tunnel entrance • The Giant Dwarf");
		addEntry("dungeon", "Ice Troll Caves", "Dungeons", 2398, 3898, 0, "Type: Dungeon entrance / underground link", "Neitiznot Ice Troll cave entrance • Fremennik Isles");
		addEntry("dungeon", "Haunted Mine", "Dungeons", 3441, 3232, 0, "Type: Dungeon entrance / underground link", "Haunted Mine cart tunnel • Haunted Mine Quest");
		addEntry("dungeon", "Barrows Crypts", "Dungeons", 3565, 3306, 0, "Type: Dungeon entrance / underground link", "Barrows mounds crypt entrance • Barrows minigame");
		addEntry("dungeon", "Meiyerditch Laboratories", "Dungeons", 3628, 3350, 0, "Type: Dungeon entrance / underground link", "Meiyerditch underground laboratory entrance • Sins of the Father");
		addEntry("dungeon", "Nature Grotto", "Dungeons", 3438, 3337, 0, "Type: Dungeon entrance / underground link", "Nature Grotto tree entrance • Nature Spirit");
		addEntry("dungeon", "Ectofuntus Dungeon", "Dungeons", 3659, 3524, 0, "Type: Dungeon entrance / underground link", "Ectofuntus temple trapdoor • Pool of Slime");
		addEntry("dungeon", "Morytania Spider Cave", "Dungeons", 3657, 3407, 0, "Type: Dungeon entrance / underground link", "Spider cave south of Port Phasmatys");
		addEntry("dungeon", "Prifddinas Underground Entrance", "Dungeons", 2240, 3328, 0, "Type: Dungeon entrance / underground link", "Tower of Voices stairway • Song of the Elves");
		addEntry("dungeon", "Smoke Dungeon", "Dungeons", 3310, 2962, 0, "Type: Dungeon entrance / underground link", "Pollnivneach well entrance • Desert Treasure I, Warmth / Face mask required");
		addEntry("dungeon", "Klenter's Pyramid", "Dungeons", 3280, 2780, 0, "Type: Dungeon entrance / underground link", "Klenter pyramid entrance • Icthlarin's Little Helper");
		addEntry("dungeon", "Desert Eagle Lair", "Dungeons", 3400, 3160, 0, "Type: Dungeon entrance / underground link", "Eagle transport system desert roost");
		addEntry("dungeon", "Sophanem Dungeon Entrance", "Dungeons", 3315, 2795, 0, "Type: Dungeon entrance / underground link", "Sophanem Dungeon entrance • Contact!");
		addEntry("dungeon", "Revenant Caves Entrance", "Dungeons", 3126, 3832, 0, "Type: Dungeon entrance / underground link", "Revenant Caves northern entrance • Wilderness PvP");
		addEntry("dungeon", "Wilderness Slayer Cave", "Dungeons", 3260, 3710, 0, "Type: Dungeon entrance / underground link", "Wilderness Slayer Cave entrance • Krystilia");
		addEntry("dungeon", "Wilderness God Wars Dungeon", "Dungeons", 3017, 3738, 0, "Type: Dungeon entrance / underground link", "Wilderness GWD entrance hole • Ecumenical keys");
		addEntry("dungeon", "Deep Wilderness Dungeon", "Dungeons", 3045, 3925, 0, "Type: Dungeon entrance / underground link", "Deep Wilderness Dungeon entrance trapdoor");
		addEntry("dungeon", "Mage Arena Bank Entrance", "Dungeons", 3099, 3934, 0, "Type: Dungeon entrance / underground link", "Kolodion pool of sparkling water • Mage Arena");
		addEntry("dungeon", "Chasm of Fire Elevator", "Dungeons", 1435, 3671, 0, "Type: Dungeon entrance / underground link", "Chasm of Fire elevator • Shayzien demons");
		addEntry("dungeon", "The Warrens Manhole", "Dungeons", 1775, 3730, 0, "Type: Dungeon entrance / underground link", "The Warrens sewer entrance • Hosidius");
		addEntry("dungeon", "Karuulm Slayer Dungeon Elevator", "Dungeons", 1311, 3807, 0, "Type: Dungeon entrance / underground link", "Mount Karuulm elevator • Wyrms & Alchemical Hydra");
		addEntry("dungeon", "Woodcutting Guild Dungeon Entrance", "Dungeons", 1580, 3480, 0, "Type: Dungeon entrance / underground link", "Ent dungeon tree entrance • Woodcutting Guild");
		addEntry("dungeon", "Cam Torum Main Entrance", "Dungeons", 1435, 3128, 0, "Type: Dungeon entrance / underground link", "Avium Savannah entrance to Cam Torum & Neypotzli");
		addEntry("dungeon", "Lighthouse Dungeon Iron Ladder", "Dungeons", 2509, 3644, 0, "Type: Dungeon entrance / underground link", "Lighthouse entrance to Dagannoths lair");
		addEntry("dungeon", "Tombs of Amascut Entrance", "Dungeons", 3493, 3090, 0, "Type: Dungeon entrance / underground link", "Necropolis entrance to Tombs of Amascut");
		addEntry("dungeon", "Lizardman Temple Entrance", "Dungeons", 1214, 3559, 0, "Type: Dungeon entrance / underground link", "Mount Quidamortem entrance to Lizardman Temple");
		addEntry("dungeon", "Fenkenstrain's Dungeon Entrance", "Dungeons", 3509, 3448, 0, "Type: Dungeon entrance / underground link", "Fenkenstrain Castle gravestone entrance");
		addEntry("dungeon", "Daeyalt Mine Entrance", "Dungeons", 3725, 3356, 0, "Type: Dungeon entrance / underground link", "Darkmeyer entrance to Daeyalt Essence Mine");
		addEntry("dungeon", "Molch Pearl Caves Entrance", "Dungeons", 1382, 3825, 0, "Type: Dungeon entrance / underground link", "Lake Molch cave entrance");
		addEntry("transportation", "Spirit Tree (Poison Waste)", "Transportation", 2339, 3108, 0, "Type: Spirit Tree transport network", "Spirit Tree east of Poison Waste / Path of Glouphrie");
		addEntry("dungeon", "Poison Waste Dungeon Entrance", "Dungeons", 2330, 3106, 0, "Type: Dungeon entrance / underground link", "Sewer entrance to Poison Waste Dungeon");
		addEntry("dungeon", "Mos Le'Harmless Caves Entrance", "Dungeons", 3748, 2973, 0, "Type: Dungeon entrance / underground link", "Cave horror & witchwood icon cavern entrance");
		addEntry("dungeon", "Jormungand's Prison Entrance", "Dungeons", 2465, 4010, 0, "Type: Dungeon entrance / underground link", "Island of Stone entrance • The Fremennik Exiles");
		addEntry("dungeon", "Ghorrock Dungeon Entrance", "Dungeons", 2847, 4043, 0, "Type: Dungeon entrance / underground link", "Ghorrock fortress dungeon entrance • Desert Treasure II");
		addEntry("dungeon", "Waterbirth Dungeon", "Dungeons", 2520, 3740, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Waterbirth Dungeon", "Dungeons", 2542, 3741, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Web Chasm", "Dungeons", 3184, 3745, 0, "Type: Dungeon entrance / underground link", "Wilderness chasm entrance");
		addEntry("dungeon", "Hole (shortcut to boat)", "Dungeons", 2853, 3944, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Werewolf Agility Course", "Dungeons", 3542, 3461, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "White Knights' Castle Crypt", "Dungeons", 2964, 3331, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "White Wolf Tunnel", "Dungeons", 2876, 3480, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "White Wolf Tunnel", "Dungeons", 2819, 3484, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Wilderness Agility Course Dungeon", "Dungeons", 3004, 3963, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Wilderness God Wars Dungeon", "Dungeons", 3016, 3739, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Wilderness Slayer Cave", "Dungeons", 3292, 3746, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Wilderness Slayer Cave", "Dungeons", 3259, 3666, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Witchaven Dungeon", "Dungeons", 2695, 3283, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Wizards' Guild basement", "Dungeons", 2593, 3085, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Wizards' Tower basement", "Dungeons", 3103, 3162, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Woodcutting Guild dungeon", "Dungeons", 1603, 3508, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Wyvern Cave", "Dungeons", 3745, 3779, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Wyvern Cave (task only)", "Dungeons", 3677, 3854, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Xeric's Lookout basement", "Dungeons", 1589, 3526, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Yanille Agility Dungeon", "Dungeons", 2568, 3122, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Yanille Agility Dungeon", "Dungeons", 2603, 3078, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");
		addEntry("dungeon", "Zemouregal's Base", "Dungeons", 3343, 3515, 0, "Type: Dungeon entrance / underground link", "Defender of Varrock • Requires the grubby key during the quest");
		addEntry("dungeon", "Zalcano's Prison", "Dungeons", 3280, 6059, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Zogre cave", "Dungeons", 2484, 3043, 0, "Type: Dungeon entrance / underground link", "May require light source, rope, or combat gear");

		// --- Dungeon / underground layer interiors (native UG coords) ---
		// UndergroundZone anchors (skip TzHaar / Waterbirth / Motherlode — already have UG pins)
		addEntry("dungeon", "Lumbridge Castle Cellar", "Dungeons", 3210, 9616, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Varrock Sewers", "Dungeons", 3237, 9859, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Taverley Dungeon", "Dungeons", 2884, 9798, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Dwarven Mine", "Dungeons", 3018, 9850, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Mining Guild", "Dungeons", 3019, 9739, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Melzar's Maze Basement", "Dungeons", 2924, 9650, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Edgeville Dungeon", "Dungeons", 3096, 9869, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Catacombs of Kourend", "Dungeons", 1664, 10048, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Brimhaven Dungeon", "Dungeons", 2713, 9564, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Karamja and Crandor Dungeon", "Dungeons", 2855, 9568, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Elvarg's Lair", "Dungeons", 2865, 9680, 0, "Type: Dungeon interior (underground layer)", "Dragon Slayer I • Elvarg boss chamber");
		addEntry("dungeon", "TzHaar City", "Dungeons", 2480, 5175, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Fight Cave", "Dungeons", 2438, 5168, 0, "Type: Dungeon interior (underground layer)", "TzHaar-Mej-Jal • TzTok-Jad fire cape minigame");
		addEntry("dungeon", "The Inferno", "Dungeons", 2496, 5118, 0, "Type: Dungeon interior (underground layer)", "TzHaar-Ket-Keh • TzKal-Zuk infernal cape minigame");
		addEntry("dungeon", "Shilo Village Gem Mine", "Dungeons", 2838, 9387, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Rashiliyia's Tomb", "Dungeons", 2758, 9183, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Crash Island Dungeon", "Dungeons", 2920, 9121, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Kharazi Caves", "Dungeons", 2838, 9245, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Miscellania Underground", "Dungeons", 2512, 10265, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Jatizso Mine", "Dungeons", 2404, 10189, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Keldagrim", "Dungeons", 2773, 10162, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Blast Furnace", "Dungeons", 2931, 10196, 0, "Type: Dungeon interior (underground layer)", "Keldagrim Blast Furnace • Bar smelting minigame");
		addEntry("dungeon", "Ice Troll Caves", "Dungeons", 2394, 10300, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Haunted Mine", "Dungeons", 3436, 9637, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Barrows Crypts & Tunnels", "Dungeons", 3565, 9706, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Meiyerditch Laboratories", "Dungeons", 3628, 9750, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Nature Grotto", "Dungeons", 3440, 9740, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Ectofuntus Pool of Slime", "Dungeons", 3674, 9893, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Morytania Spider Cave", "Dungeons", 3670, 9830, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Underground Pass", "Dungeons", 2433, 9714, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Well of Voyage", "Dungeons", 2332, 9623, 0, "Type: Dungeon interior (underground layer)", "Underground Pass • Well of Voyage to Iban's Lair");
		addEntry("dungeon", "Prifddinas Underground", "Dungeons", 3232, 12450, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Prifddinas Grand Library", "Dungeons", 3232, 12512, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Kalphite Queen Lair", "Dungeons", 3483, 9510, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Smoke Dungeon", "Dungeons", 3246, 9378, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Klenter's Tomb", "Dungeons", 3280, 9180, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Desert Eagle Lair", "Dungeons", 3418, 9571, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Sophanem Dungeon", "Dungeons", 2272, 4319, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Pyramid Plunder", "Dungeons", 1928, 4455, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Revenant Caves", "Dungeons", 3250, 10235, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Wilderness Slayer Cave", "Dungeons", 3373, 10106, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Wilderness God Wars Dungeon", "Dungeons", 3017, 10138, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Deep Wilderness Dungeon", "Dungeons", 3045, 10327, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Mage Arena Bank", "Dungeons", 2525, 4705, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Chasm of Fire", "Dungeons", 1435, 10071, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "The Warrens", "Dungeons", 1775, 10130, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Karuulm Slayer Dungeon", "Dungeons", 1311, 10207, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Woodcutting Guild Dungeon", "Dungeons", 1580, 9880, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Cam Torum City", "Dungeons", 1439, 9509, 1, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Neypotzli Moons of Peril", "Dungeons", 1439, 9660, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Tonali Cavern", "Dungeons", 1309, 9404, 0, "Type: Dungeon interior (underground layer)", "Crypt of Tonali • The Final Dawn, Earthen Nagua, Doom of Mokhaiotl");
		addEntry("dungeon", "Dragon Nest", "Dungeons", 1247, 9503, 0, "Type: Dungeon interior (underground layer)", "Tlati Rainforest dragon nest • Red & blue dragons, Scrambled!");
		addEntry("dungeon", "Lighthouse Dungeon (Dagannoths)", "Dungeons", 2518, 10024, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Tombs of Amascut Lobby", "Dungeons", 3344, 9130, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Lizardman Temple", "Dungeons", 1281, 10048, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Lizardman Caves", "Dungeons", 1308, 9963, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Giants' Den", "Dungeons", 1432, 9913, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Fortis Barracks", "Dungeons", 1641, 9576, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Hunter Guild Caverns", "Dungeons", 1558, 9451, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Lithkren Vault", "Dungeons", 3549, 10448, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Weiss Salt Mine", "Dungeons", 2845, 10351, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Jaldraocht Pyramid", "Dungeons", 2913, 4954, 3, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "River Elid Dungeon", "Dungeons", 3349, 9536, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Shadow Dungeon", "Dungeons", 2630, 5071, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("basement", "Draynor Manor Basement", "Dungeons", 3117, 9753, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Enakhra's Temple", "Dungeons", 3124, 9328, 1, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Port Sarim Rat Pits", "Dungeons", 2962, 9650, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "King Black Dragon Lair", "Dungeons", 2272, 4705, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Hespori Cave", "Dungeons", 1248, 10089, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Stalker Den", "Dungeons", 1286, 9775, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Fenkenstrain's Castle Dungeon", "Dungeons", 3509, 9848, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Wilderness Agility Dungeon", "Dungeons", 3008, 10367, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Daeyalt Essence Mine", "Dungeons", 3649, 9820, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Molch Pearl Caves", "Dungeons", 1280, 9983, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Poison Waste Sewer", "Dungeons", 1504, 4235, 1, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Sisterhood Sanctuary", "Dungeons", 3727, 9700, 1, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Jormungand's Prison", "Dungeons", 2445, 10419, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Ghorrock Dungeon", "Dungeons", 2888, 10465, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Fremennik Slayer Dungeon", "Dungeons", 2808, 10002, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Waterbirth Island Dungeon", "Dungeons", 2544, 10153, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Dagannoth Kings Lair", "Dungeons", 2900, 4449, 0, "Type: Dungeon interior (underground layer)", "Dagannoth Supreme, Prime, Rex boss lair");
		addEntry("dungeon", "Asgarnian Ice Dungeon", "Dungeons", 3007, 9550, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Stronghold of Security", "Dungeons", 1860, 5230, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Keldagrim (Dwarven Capital)", "Dungeons", 2867, 10190, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Ardougne Sewers & Underground", "Dungeons", 2632, 9694, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Miscellania Underground", "Dungeons", 2500, 10250, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Deep Wilderness Dungeon", "Dungeons", 3044, 10324, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Forthos Dungeon", "Dungeons", 1800, 9950, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Cam Torum", "Dungeons", 1435, 3128, 0, "Type: Dungeon entrance / underground link", "Entrance to Cam Torum subterranean dwarven city");
		addEntry("dungeon", "Cam Torum", "Dungeons", 1439, 9509, 1, "Type: Dungeon interior (underground layer)", "Subterranean dwarven city in Varlamore");
		addEntry("dungeon", "God Wars Dungeon", "Dungeons", 2881, 5310, 2, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Fossil Island Caverns / Wyvern Cave", "Dungeons", 3660, 10250, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Wizards' Tower Basement", "Dungeons", 3104, 9576, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Tolna's Rift", "Dungeons", 3297, 9824, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Sourhog Cave", "Dungeons", 3149, 9747, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Paterdomus Basement", "Dungeons", 3405, 9906, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Water mill cellar", "Dungeons", 3166, 9695, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Digsite Dungeon", "Dungeons", 3356, 9822, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Chasm of Tears", "Dungeons", 3219, 9532, 2, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Zanaris", "Dungeons", 2412, 4434, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Ancient Cavern", "Dungeons", 1768, 5366, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Mole Hole", "Dungeons", 1760, 5180, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Motherlode Mine", "Dungeons", 3728, 5692, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Ourania Altar (ZMI)", "Dungeons", 3022, 5582, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Scrubfoot's Cave", "Dungeons", 2946, 9907, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Warriors' Guild Basement", "Dungeons", 2907, 9968, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Waterfall Dungeon", "Dungeons", 2575, 9861, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Eagles' Peak Dungeon", "Dungeons", 2080, 5024, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Grand Tree Tunnels", "Dungeons", 2462, 9896, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Stronghold Slayer Cave", "Dungeons", 2428, 9824, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Ogre Enclave", "Dungeons", 2506, 9439, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Jiggig Dungeon", "Dungeons", 2443, 9451, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Corsair Cove Dungeon", "Dungeons", 1971, 9035, 1, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Red Chinchompa Hunting Ground", "Dungeons", 2525, 9294, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Jungle Eagle Lair", "Dungeons", 2518, 9321, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Skavid Caves", "Dungeons", 2562, 9424, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Ice Queen's Lair", "Dungeons", 2847, 9916, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "White Knights' Castle Crypt", "Dungeons", 2964, 9731, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "White Wolf Tunnel", "Dungeons", 2876, 9878, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Ruins of Camdozaal", "Dungeons", 2976, 5824, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Corporeal Beast Lair", "Dungeons", 2967, 4382, 2, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Tower of Life Basement", "Dungeons", 3059, 4397, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Tree Gnome Village Dungeon", "Dungeons", 2533, 9556, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Hallowed Sepulchre", "Dungeons", 2400, 5984, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Dorgesh-Kaan", "Dungeons", 2752, 5344, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Dorgesh-Kaan South Dungeon", "Dungeons", 2715, 5241, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Rogues' Den", "Dungeons", 3061, 4985, 1, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "The Abyss", "Dungeons", 3040, 4832, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Pyramid Plunder", "Dungeons", 1952, 4448, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Barbarian Assault", "Dungeons", 1888, 5440, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Lair of Tarn Razorlor", "Dungeons", 3246, 4590, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Prifddinas Grand Library", "Dungeons", 2816, 6144, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");

		// Classic +6400 / WikiMap interiors without a dedicated UndergroundZone
		addEntry("dungeon", "Kalphite Lair", "Dungeons", 3487, 9510, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Kalphite Cave", "Dungeons", 3319, 9522, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Karuulm Slayer Dungeon", "Dungeons", 1308, 10207, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Sisterhood Sanctuary", "Dungeons", 3727, 9700, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Revenant Caves", "Dungeons", 3124, 10232, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Smoke Devil Dungeon", "Dungeons", 2411, 9461, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Ape Atoll Dungeon", "Dungeons", 2764, 9103, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Kruk's Dungeon", "Dungeons", 2763, 2703, 0, "Type: Dungeon entrance / underground link", "Monkey Madness II • demonic gorillas, Kruk");
		addEntry("dungeon", "Underground Pass", "Dungeons", 2433, 9715, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Temple of Ikov", "Dungeons", 2688, 9804, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Kraken Cove", "Dungeons", 2278, 10011, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Goblin Cave", "Dungeons", 2622, 9793, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Yanille Agility Dungeon", "Dungeons", 2568, 9522, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Mos Le'Harmless Cave", "Dungeons", 3747, 9373, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Crandor Dungeon", "Dungeons", 2833, 9656, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Lumbridge Swamp Caves", "Dungeons", 3168, 9572, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Observatory Dungeon", "Dungeons", 2336, 9376, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Witchaven Dungeon", "Dungeons", 2695, 9683, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Ice Queen's Lair", "Dungeons", 2860, 9948, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Barbarian Assault Entrance", "Dungeons", 2533, 3573, 0, "Type: Dungeon entrance / underground link", "Barbarian Assault lobby entrance");
		addEntry("dungeon", "Sophanem Dungeon", "Dungeons", 2272, 4320, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Chasm of Fire", "Dungeons", 1432, 10070, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Giants' Den", "Dungeons", 1419, 9988, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Lizardman Caves", "Dungeons", 1306, 9974, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Crabclaw Caves", "Dungeons", 1643, 9849, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Callisto's Den", "Dungeons", 3292, 10250, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Scorpia cave", "Dungeons", 3243, 10349, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Silk Chasm", "Dungeons", 3423, 10203, 2, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Wilderness God Wars Dungeon", "Dungeons", 3016, 10139, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Wilderness Slayer Cave", "Dungeons", 3292, 10146, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Vet'ion's Rest", "Dungeons", 3221, 10188, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Lava Maze Dungeon", "Dungeons", 3068, 10256, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Ferox Enclave Dungeon", "Dungeons", 3153, 10043, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Hunter's End", "Dungeons", 3115, 10077, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Web Chasm", "Dungeons", 3184, 10145, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Escape Caves", "Dungeons", 3284, 10208, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Lunar Isle Mine", "Dungeons", 2141, 10344, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Giants' Foundry", "Dungeons", 3374, 11493, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Zemouregal's Base", "Dungeons", 3578, 4582, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Stronghold Slayer Dungeon", "Dungeons", 2427, 9824, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Brimstail's Cave", "Dungeons", 2409, 9812, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Chaos Druid Tower dungeon", "Dungeons", 2562, 9756, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Clock Tower Dungeon", "Dungeons", 2568, 9627, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Elemental Workshop", "Dungeons", 2716, 9888, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Glarial's Tomb", "Dungeons", 2557, 9844, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Heroes' Guild mine", "Dungeons", 2893, 9907, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "H.A.M. Hideout", "Dungeons", 3164, 9652, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Draynor Sewers", "Dungeons", 3083, 9672, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Shade Catacombs", "Dungeons", 3484, 9721, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Lighthouse basement", "Dungeons", 2508, 10044, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Smoke Dungeon", "Dungeons", 3309, 9362, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Brimhaven Agility Arena", "Dungeons", 2808, 9594, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Volcanic Mine", "Dungeons", 3815, 10208, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Wyvern Cave", "Dungeons", 3745, 10179, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Wyvern Cave (task only)", "Dungeons", 3677, 10254, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "Waterbirth Dungeon", "Dungeons", 2520, 9920, 0, "Type: Dungeon interior (underground layer)", "Visible on the dungeon / underground map");
		addEntry("dungeon", "TzHaar City (Mor Ul Rek)", "Dungeons", 2860, 3170, 0, "Type: Dungeon entrance / underground link", "Surface entrance under Karamja Volcano");
	}

	/** Agility courses and the shortcuts they unlock. */
	private static void addAgilityCourses()
	{
		addEntry("agility_training", "Agility Pyramid (Level 30)", "Skilling • Agility", 3347, 2827, 0, "Level requirement: Level 30 Agility", "Lap XP: 1,014 XP (reward: 10,000 gp pyramid top)", "Activity: Pyramid Course • Waterskins recommended");
		addEntry("agility_training", "Al Kharid Rooftop Course (Level 20)", "Skilling • Agility", 3273, 3195, 0, "Level requirement: Level 20 Agility", "Lap XP: 180 XP", "Activity: Rooftop Course • Marks of Grace: Yes");
		addEntry("agility_training", "Ape Atoll Agility Course (Level 48)", "Skilling • Agility", 2753, 2742, 0, "Level requirement: Level 48 Agility", "Lap XP: 580 XP", "Activity: Obstacle Course • Requires Ninja monkey greegree");
		addEntry("agility_training", "Ardougne Rooftop Course (Level 90)", "Skilling • Agility", 2674, 3298, 0, "Level requirement: Level 90 Agility", "Lap XP: 793 XP", "Activity: Rooftop Course • Marks of Grace: Yes");
		addEntry("agility_training", "Barbarian Outpost Agility Course (Level 35)", "Skilling • Agility", 2544, 3569, 0, "Level requirement: Level 35 Agility (Advanced: Level 90)", "Lap XP: 153.2 XP (Advanced: 615.6 XP)", "Activity: Obstacle Course • Requires Alfred Grimhand's Barcrawl");
		addEntry("agility_training", "Brimhaven Agility Arena (Level 40)", "Skilling • Agility", 2806, 3193, 0, "Level requirement: Level 40 Agility (entrance fee: 200 coins)", "Activity: Ticket Tagging Arena • Redeem tickets for XP and rewards", "Marks of Grace: No (gives Agility Arena tickets)");
		addEntry("agility_training", "Canifis Rooftop Course (Level 40)", "Skilling • Agility", 3506, 3490, 0, "Level requirement: Level 40 Agility", "Lap XP: 240 XP", "Activity: Rooftop Course • Marks of Grace: Yes");
		addEntry("agility_training", "Colossal Wyrm Agility Course (Level 50)", "Skilling • Agility", 1649, 2930, 0, "Level requirement: Level 50 Agility (Basic) / Level 62 (Advanced)", "Lap XP: Basic 450 XP • Advanced 575 XP", "Activity: Obstacle Course • Rewards: Termites & Calcified deposits");
		addEntry("agility_training", "Dorgesh-Kaan Agility Course (Level 70)", "Skilling • Agility", 2712, 5247, 0, "Level requirement: Level 70 Agility, 70 Ranged", "Lap XP: ~2,750 XP per power station route", "Activity: Obstacle Course • Requires Mith grapple & Crossbow");
		addEntry("agility_training", "Draynor Village Rooftop Course (Level 10)", "Skilling • Agility", 3103, 3279, 0, "Level requirement: Level 10 Agility", "Lap XP: 120 XP", "Activity: Rooftop Course • Marks of Grace: Yes");
		addEntry("agility_training", "Falador Rooftop Course (Level 50)", "Skilling • Agility", 3035, 3340, 0, "Level requirement: Level 50 Agility", "Lap XP: 440 XP", "Activity: Rooftop Course • Marks of Grace: Yes");
		addEntry("agility_training", "Gnome Stronghold Agility Course (Level 1)", "Skilling • Agility", 2474, 3436, 0, "Level requirement: Level 1 Agility (Advanced: Level 85)", "Lap XP: 86.5 XP (Advanced: 725 XP)", "Activity: Obstacle Course • Marks of Grace: No");
		addEntry("agility_training", "Hallowed Sepulchre (Level 52–92)", "Skilling • Agility", 3655, 3386, 0, "Level requirement: Level 52–92 Agility (Floors 1–5)", "Activity: Agility Minigame / Obstacle Crypt", "Rewards: Hallowed Marks, Ring of endurance, Dark dye", "Requirements: Sins of the Father quest completed");
		addEntry("agility_training", "Penguin Agility Course (Level 60)", "Skilling • Agility", 2639, 4041, 0, "Level requirement: Level 60 Agility", "Lap XP: 540 XP", "Activity: Obstacle Course • Requires Penguin suit (Cold War quest)");
		addEntry("agility_training", "Penguin Agility Course (Level 60)", "Skilling • Agility", 2639, 4041, 1, "Level requirement: Level 60 Agility", "Lap XP: 540 XP", "Activity: Obstacle Course • Requires Penguin suit (Cold War quest)");
		addEntry("agility_training", "Pollnivneach Rooftop Course (Level 70)", "Skilling • Agility", 3351, 2963, 0, "Level requirement: Level 70 Agility", "Lap XP: 890 XP", "Activity: Rooftop Course • Marks of Grace: Yes");
		addEntry("agility_training", "Prifddinas Agility Course (Level 75)", "Skilling • Agility", 3253, 6109, 0, "Level requirement: Level 75 Agility", "Lap XP: 1,337 XP", "Activity: Rooftop Course • Rewards Crystal shards (no Marks of Grace)", "Requirements: Song of the Elves quest completed");
		addEntry("agility_training", "Rellekka Rooftop Course (Level 80)", "Skilling • Agility", 2625, 3677, 0, "Level requirement: Level 80 Agility", "Lap XP: 780 XP", "Activity: Rooftop Course • Marks of Grace: Yes");
		addEntry("agility_training", "Seers' Village Rooftop Course (Level 60)", "Skilling • Agility", 2729, 3488, 0, "Level requirement: Level 60 Agility", "Lap XP: 570 XP", "Activity: Rooftop Course • Marks of Grace: Yes", "Fastest course with Kandarin Hard Diary");
		addEntry("agility_training", "Shayzien Agility Course (Level 5)", "Skilling • Agility", 1551, 3632, 0, "Level requirement: Level 5 Agility (Basic) / Level 48 (Advanced)", "Lap XP: Basic 133 XP • Advanced 431.6 XP", "Activity: Obstacle Course in Shayzien");
		addEntry("agility_training", "Varrock Rooftop Course (Level 30)", "Skilling • Agility", 3220, 3414, 0, "Level requirement: Level 30 Agility", "Lap XP: 238 XP", "Activity: Rooftop Course • Marks of Grace: Yes");
		addEntry("agility_training", "Werewolf Agility Course (Level 60)", "Skilling • Agility", 3543, 3463, 0, "Level requirement: Level 60 Agility", "Lap XP: 730 XP (with stick returned)", "Activity: Obstacle Course • Requires Ring of Charos");
		addEntry("agility_training", "Wilderness Agility Course (Level 52)", "Skilling • Agility", 2998, 3916, 0, "Level requirement: Level 52 Agility", "Lap XP: 571.4 XP", "Activity: Obstacle Course • Wilderness PvP danger");
		addEntry("agility_short-cut", "Mount Karuulm Lower Rocks (Level 29)", "Travel • Agility", 1325, 3782, 0, "Level requirement: Level 29 Agility", "Connects: Base of Mount Karuulm to middle plateau", "Quicker path to the Karuulm Slayer Dungeon and volcano");
		addEntry("agility_short-cut", "Mount Karuulm Upper Rocks (Level 62)", "Travel • Agility", 1323, 3791, 0, "Level requirement: Level 62 Agility", "Connects: Middle plateau to top of Mount Karuulm", "Quick access to the Slayer Master Konar quo Maten");
		addEntry("agility_short-cut", "River Hos Stepping Stones (Level 40)", "Travel • Agility", 1605, 3572, 0, "Level requirement: Level 40 Agility", "Connects: Shayzien Encampment across River Hos to Hosidius", "Fast crossing between western and eastern Great Kourend");
		addEntry("agility_short-cut", "River Hos Stepping Stones (Level 40)", "Travel • Agility", 1613, 3570, 0, "Level requirement: Level 40 Agility", "Connects: Hosidius across River Hos to Shayzien", "Fast crossing between western and eastern Great Kourend");
		addEntry("agility_short-cut", "Hosidius Stepping Stone (Level 45)", "Travel • Agility", 1722, 3509, 0, "Level requirement: Level 45 Agility", "Connects: Hosidius chapel and POH portal across stream", "Faster route between Hosidius estate and farmland");
		addEntry("agility_short-cut", "Wintertodt Gap (Level 60)", "Travel • Agility", 1630, 4023, 0, "Level requirement: Level 60 Agility", "Connects: Wintertodt prison camp south and north areas", "Vault over gap in the frozen mountain perimeter");
		addEntry("agility_short-cut", "Wintertodt Upper Gap (Level 60)", "Travel • Agility", 1630, 4023, 1, "Level requirement: Level 60 Agility", "Connects: Wintertodt upper perimeter platform", "Shortcut across the frozen upper walkway");
		addEntry("agility_short-cut", "Dense Essence Mine West Rocks (Level 73)", "Travel • Agility", 1743, 3853, 0, "Level requirement: Level 73 Agility", "Connects: Arceuus Dense Runestone quarry (west side)", "High-level shortcut for faster Blood and Soul Runecrafting");
		addEntry("agility_short-cut", "Dense Essence Mine North Rocks (Level 69)", "Travel • Agility", 1760, 3873, 0, "Level requirement: Level 69 Agility", "Connects: Arceuus Dense Runestone quarry (north side)", "Bypasses rocky ridges for faster essence transport");
		addEntry("agility_short-cut", "Dense Essence Mine East Rocks (Level 52)", "Travel • Agility", 1771, 3851, 0, "Level requirement: Level 52 Agility", "Connects: Arceuus Dense Runestone quarry (east side)", "Bypasses lower cliff toward the Dark Altar path");
		addEntry("agility_short-cut", "Dense Essence Mine Boulder (Level 49)", "Travel • Agility", 1775, 3888, 0, "Level requirement: Level 49 Agility", "Connects: Arceuus North path to Dense Runestone quarry", "Climb over boulder obstacle to reach mining area");
		addEntry("agility_short-cut", "Zul-Andra Stepping Stone (Level 76)", "Travel • Agility", 2157, 3073, 0, "Level requirement: Level 76 Agility", "Connects: Zul-Andra pier to the eastern peninsula", "Quick access to Zulrah's shrine without fairy ring scroll");
		addEntry("agility_short-cut", "Weiss Cliffside (Level 68)", "Travel • Agility", 2263, 4044, 0, "Level requirement: Level 68 Agility", "Connects: Weiss northern coast to the upper village", "Requirements: Making Friends with My Arm quest");
		addEntry("agility_short-cut", "Arandar Mountain Pass Rocks (Easy) (Level 59)", "Travel • Agility", 2346, 3300, 0, "Level requirement: Level 59 Agility", "Connects: Arandar mountain pass northern shortcut", "Requirements: Regicide quest completed", "Shortens path between Tirannwn and Kandarin");
		addEntry("agility_short-cut", "Arandar Mountain Pass Rocks (Medium) (Level 68)", "Travel • Agility", 2338, 3288, 0, "Level requirement: Level 68 Agility", "Connects: Arandar mountain pass middle shortcut", "Requirements: Regicide quest completed", "Shortens path between Tirannwn and Kandarin");
		addEntry("agility_short-cut", "Arandar Mountain Pass Rocks (Advanced) (Level 85)", "Travel • Agility", 2338, 3253, 0, "Level requirement: Level 85 Agility", "Connects: Arandar mountain pass southern shortcut", "Requirements: Regicide quest completed • Western Elite Diary task", "Direct route into Lletya and Isafdar forests");
		addEntry("agility_short-cut", "Eagles' Peak Rocks (Level 25)", "Travel • Agility", 2321, 3499, 0, "Level requirement: Level 25 Agility", "Connects: Eagles' Peak north side to the western plains", "Faster path to the falconry and hunting grounds");
		addEntry("agility_short-cut", "Neitiznot Rope Bridge (Level 40)", "Travel • Agility", 2316, 3828, 0, "Level requirement: Level 40 Agility", "Connects: Neitiznot town across chasm to northern islands", "Requirements: The Fremennik Isles quest (partial)");
		addEntry("agility_short-cut", "Corsair Cove Rocks (Level 10)", "Travel • Agility", 2546, 2871, 0, "Level requirement: Level 10 Agility", "Connects: Corsair Cove settlement to Feldip Hills", "Bypasses the southern coast path");
		addEntry("agility_short-cut", "Corsair Cove Resource Area Rocks (Level 30)", "Travel • Agility", 2487, 2898, 0, "Level requirement: Level 30 Agility", "Connects: Feldip Hills to Corsair Cove Resource Area", "Requirements: Dragon Slayer I completed (ogress caves)");
		addEntry("agility_short-cut", "Observatory Rocks & Grapple (Level 23)", "Travel • Agility", 2448, 3155, 0, "Level requirement: Level 23 Agility, 28 Strength, 24 Ranged", "Connects: River bank to Observatory hill summit", "Requirements: Crossbow & Mith grapple (once) • Observatory Quest");
		addEntry("agility_short-cut", "Tree Gnome Stronghold Loose Railing (Level 37)", "Travel • Agility", 2486, 3515, 0, "Level requirement: Level 37 Agility", "Connects: South-east Stronghold fence to outskirts", "Bypasses the Stronghold main entrance gates");
		addEntry("agility_short-cut", "Yanille South Wall Climb (Level 69)", "Travel • Agility", 2553, 3072, 0, "Level requirement: Level 69 Agility (or 39 Agility with Mith grapple)", "Connects: South-west Yanille town wall to Gu'Tanoth outskirts", "Requirements: Medium Ardougne Diary task (with grapple)");
		addEntry("agility_short-cut", "Yanille Underwall Tunnel (Level 16)", "Travel • Agility", 2575, 3109, 0, "Level requirement: Level 16 Agility", "Connects: Yanille northern wall to the Watchtower area", "Quick access from Watchtower teleport into Yanille");
		addEntry("agility_short-cut", "East Ardougne Log Balance (Level 33)", "Travel • Agility", 2603, 3336, 0, "Level requirement: Level 33 Agility", "Connects: West bank of River Dougne to Ardougne Zoo / Monastery", "Crosses River Dougne south of East Ardougne castle");
		addEntry("agility_short-cut", "Coal Trucks Log Balance (Level 20)", "Travel • Agility", 2599, 3475, 0, "Level requirement: Level 20 Agility", "Connects: Coal Trucks mining site across River McGrubor", "Fast route between Coal Trucks and Seers' Village bank");
		addEntry("agility_short-cut", "Miscellania Stepping Stone (Level 55)", "Travel • Agility", 2573, 3862, 0, "Level requirement: Level 55 Agility", "Connects: Miscellania castle docks across water to Etceteria", "Bypasses the long wooden bridge between the islands");
		addEntry("agility_short-cut", "Fremennik Province Log Balance (Level 48)", "Travel • Agility", 2722, 3591, 0, "Level requirement: Level 48 Agility", "Connects: River crossing between Seers' Village and Rellekka", "Quick route from McGrubor's Wood into Fremennik lands");
		addEntry("agility_short-cut", "Rellekka Broken Fence (Level 57)", "Travel • Agility", 2689, 3697, 0, "Level requirement: Level 57 Agility", "Connects: Eastern Rellekka to Rock Crab coast and Slayer Cave", "Direct escape / entry through eastern Rellekka palisade");
		addEntry("agility_short-cut", "Shilo Village Stepping Stone (Level 32)", "Travel • Agility", 2864, 2974, 0, "Level requirement: Level 32 Agility", "Connects: Shilo Village river bank to southern nature altar area", "Requirements: Shilo Village quest completed");
		addEntry("agility_short-cut", "Karamja Volcano Strong Tree (Level 78)", "Travel • Agility", 2874, 3143, 0, "Level requirement: Level 78 Agility (or 52 Agility with Mith grapple)", "Connects: Musa Point across sea inlet to Harpie Bug Swarms", "Requirements: Hard Karamja Diary task (with grapple)");
		addEntry("agility_short-cut", "Karamja River Grapple / Tree (Level 34)", "Travel • Agility", 2875, 3128, 0, "Level requirement: Level 34 Agility, 21 Strength, 42 Ranged", "Connects: Central Karamja jungle across river to Legends' Guild south", "Requirements: Crossbow & Mith grapple");
		addEntry("agility_short-cut", "Kharazi Jungle Vine (Level 79)", "Travel • Agility", 2898, 2939, 0, "Level requirement: Level 79 Agility", "Connects: Southern cliff face into the deep Kharazi Jungle", "Requirements: Legends' Quest started or completed");
		addEntry("agility_short-cut", "Karamja Stepping Stones (Level 30)", "Travel • Agility", 2925, 2946, 0, "Level requirement: Level 30 Agility", "Connects: Southern Karamja river crossing near Cairn Isle", "Fast crossing without navigating coastal marshes");
		addEntry("agility_short-cut", "Tai Bwo Wannai Stepping Stones (Level 30)", "Travel • Agility", 2907, 3050, 0, "Level requirement: Level 30 Agility", "Connects: Jungle path south of Tai Bwo Wannai across river", "Quick access between village and Calquat farming patch");
		addEntry("agility_short-cut", "Catherby Cliffside Rocks (Level 68)", "Travel • Agility", 2869, 3429, 0, "Level requirement: Level 68 Agility (or 32 Agility with Mith grapple)", "Connects: Catherby tree patch over cliff to Taverley", "High-value shortcut for Farming tree runs");
		addEntry("agility_short-cut", "Troll Stronghold Rocks (Level 73)", "Travel • Agility", 2842, 3694, 0, "Level requirement: Level 73 Agility", "Connects: Troll Stronghold main entrance to the roof herb patch", "Requirements: Troll Stronghold quest completed", "Direct route to My Arm's disease-free herb patch");
		addEntry("agility_short-cut", "Trollheim Easy Cliff Scramble (Level 41)", "Travel • Agility", 2870, 3670, 0, "Level requirement: Level 41 Agility", "Connects: Lower Trollheim path ascending toward the peak", "Requirements: Death Plateau quest (Climbing boots)");
		addEntry("agility_short-cut", "Trollheim Medium Cliff Scramble (Level 43)", "Travel • Agility", 2877, 3666, 0, "Level requirement: Level 43 Agility", "Connects: Mid-level Trollheim mountain trail ascending to summit", "Requirements: Death Plateau quest");
		addEntry("agility_short-cut", "Trollheim Advanced Cliff Scramble (Level 44)", "Travel • Agility", 2908, 3686, 0, "Level requirement: Level 44 Agility", "Connects: Upper Trollheim trail to the mountain plateau", "Quicker path to Eadgar's cave and God Wars Dungeon entrance");
		addEntry("agility_short-cut", "Trollheim Hard Cliff Scramble (Level 47)", "Travel • Agility", 2903, 3680, 0, "Level requirement: Level 47 Agility", "Connects: Eastern Trollheim descent directly toward God Wars route", "Fastest route down Trollheim mountain");
		addEntry("agility_short-cut", "Trollheim Hard Cliff Scramble (Level 47)", "Travel • Agility", 2887, 3684, 0, "Level requirement: Level 47 Agility", "Connects: Trollheim mountain ridge climb", "Bypasses winding mountain paths and thrower trolls");
		addEntry("agility_short-cut", "Trollheim Mountain Path (Level 47)", "Travel • Agility", 2918, 3672, 0, "Level requirement: Level 47 Agility", "Connects: Trollheim south-east cliff descent", "Alternative descent from Trollheim summit");
		addEntry("agility_short-cut", "Trollheim to Wilderness Rocks (Level 64)", "Travel • Agility", 2946, 3678, 0, "Level requirement: Level 64 Agility", "Connects: Trollheim eastern base to Level 21 Wilderness", "Requirements: Troll Stronghold quest (partial)");
		addEntry("agility_short-cut", "Wilderness God Wars Dungeon Rocky Handholds (Level 60)", "Travel • Agility", 2929, 3760, 0, "Level requirement: Level 60 Agility (or 60 Strength)", "Connects: Level 28 Wilderness into the God Wars Dungeon entrance", "Requirements: Rope • Danger: Wilderness PvP area");
		addEntry("agility_short-cut", "Wilderness God Wars Dungeon Rocky Handholds (Level 60)", "Travel • Agility", 2944, 3770, 0, "Level requirement: Level 60 Agility (or 60 Strength)", "Connects: God Wars Dungeon crevice climb to Wilderness surface", "Requirements: Rope • Danger: Wilderness PvP area");
		addEntry("agility_short-cut", "Goblin Village Tight-Gap (Level 14)", "Travel • Agility", 2926, 3523, 0, "Level requirement: Level 14 Agility", "Connects: Burthorpe southern border to Chaos Temple / Goblin Village", "Requirements: Falador Easy Diary completed");
		addEntry("agility_short-cut", "Falador Crumbling Wall (Level 5)", "Travel • Agility", 2937, 3357, 0, "Level requirement: Level 5 Agility", "Connects: Falador West Bank to the southern Taverley road", "Most famous shortcut: saves long walk around Falador city walls");
		addEntry("agility_short-cut", "Falador Underwall Tunnel (Level 26)", "Travel • Agility", 2948, 3313, 0, "Level requirement: Level 26 Agility", "Connects: South-west Falador wall to the farm & crafting guild road", "Fast exit from south Falador");
		addEntry("agility_short-cut", "Falador North Wall Climb (Level 52)", "Travel • Agility", 3032, 3391, 0, "Level requirement: Level 52 Agility (or 11 Agility with Mith grapple)", "Connects: North-east Falador Park over wall to Dwarven Mine & Barbarian Village", "Requirements: Medium Falador Diary task (with grapple)");
		addEntry("agility_short-cut", "Draynor Village Underwall Tunnel (Level 42)", "Travel • Agility", 3069, 3261, 0, "Level requirement: Level 42 Agility", "Connects: Draynor Village willow trees to Port Sarim market", "Quick access between Draynor bank and Port Sarim docks");
		addEntry("agility_short-cut", "Lava Maze Stepping Stone (Level 82)", "Travel • Agility", 3093, 3880, 0, "Level requirement: Level 82 Agility", "Connects: Level 46 Wilderness across lava to central Lava Maze", "Direct access to the Muddy Chest without running through the maze", "Danger: High-level Wilderness PvP");
		addEntry("agility_short-cut", "Lava Dragon Isle Stepping Stones (Level 74)", "Travel • Agility", 3201, 3807, 0, "Level requirement: Level 74 Agility", "Connects: Level 36 Wilderness southern shore to Lava Dragon Isle", "Quicker escape and entry to Lava Dragon hunting grounds", "Danger: High-level Wilderness PvP");
		addEntry("agility_short-cut", "Champions' Guild Stepping Stone (Level 31)", "Travel • Agility", 3151, 3362, 0, "Level requirement: Level 31 Agility", "Connects: Champions' Guild / Varrock south to wheat field & windmill", "Crosses River Lum south of Varrock");
		addEntry("agility_short-cut", "Grand Exchange Underwall Tunnel (Level 21)", "Travel • Agility", 3140, 3515, 0, "Level requirement: Level 21 Agility", "Connects: North-west Grand Exchange directly to Edgeville", "Extremely popular shortcut between GE and Edgeville bank/furnace");
		addEntry("agility_short-cut", "Lumbridge Swamp Stepping Stone (Level 66)", "Travel • Agility", 3211, 3135, 0, "Level requirement: Level 66 Agility", "Connects: Southern Lumbridge Swamp across River Lum to Al Kharid mine", "Fast route between Lumbridge Swamp Caves and Al Kharid");
		addEntry("agility_short-cut", "River Lum Broken Raft (West) (Level 48)", "Travel • Agility", 3246, 3179, 0, "Level requirement: Level 48 Agility (or 8 Agility with Mith grapple)", "Connects: Lumbridge graveyard / east bank to Al Kharid palace gardens", "Grapple requires: 19 Strength, 37 Ranged, Crossbow & Mith grapple");
		addEntry("agility_short-cut", "River Lum Broken Raft (East) (Level 48)", "Travel • Agility", 3259, 3179, 0, "Level requirement: Level 48 Agility (or 8 Agility with Mith grapple)", "Connects: Al Kharid palace gardens across River Lum to Lumbridge", "Grapple requires: 19 Strength, 37 Ranged, Crossbow & Mith grapple");
		addEntry("agility_short-cut", "Varrock South Fence Jump (Level 13)", "Travel • Agility", 3240, 3334, 0, "Level requirement: Level 13 Agility", "Connects: South Varrock stone wall to the mining & farming fields", "Quick access to Varrock south gate area");
		addEntry("agility_short-cut", "Al Kharid Palace Window (Level 70)", "Travel • Agility", 3294, 3158, 0, "Level requirement: Level 70 Agility", "Connects: Al Kharid Palace south window to the southern coast", "Requirements: Prince Ali Rescue quest completed");
		addEntry("agility_short-cut", "Al Kharid Mine Cliff Scramble (Level 38)", "Travel • Agility", 3306, 3315, 0, "Level requirement: Level 38 Agility", "Connects: Al Kharid northern mine plateau to the scorpion pit below", "Direct descent into the mining site");
		addEntry("agility_short-cut", "Mort Myre Swamp Stepping Stone (Level 50)", "Travel • Agility", 3419, 3326, 0, "Level requirement: Level 50 Agility", "Connects: Island in River Salve south-west of Mort Myre Swamp", "Quick crossing across the Salve river boundary");
		addEntry("agility_short-cut", "Paterdomus Ornate Railing (Level 65)", "Travel • Agility", 3423, 3476, 0, "Level requirement: Level 65 Agility", "Connects: Paterdomus temple cellar to Mort Myre Swamp surface", "Requirements: Priest in Peril quest • Medium Morytania Diary task");
		addEntry("agility_short-cut", "Slayer Tower Ivy Climb (Ground Floor) (Level 81)", "Travel • Agility", 3421, 3551, 0, "Level requirement: Level 81 Agility", "Connects: Slayer Tower ground floor outside wall directly to 2nd floor", "Bypasses lower floors directly to Nechryaels / Abyssal demons");
		addEntry("agility_short-cut", "Slayer Tower Broken Window (Ground Floor) (Level 18)", "Travel • Agility", 3422, 3550, 0, "Level requirement: Level 18 Agility", "Connects: Slayer Tower entrance outside directly into Banshee room", "Fast access for Banshee Slayer tasks");
		addEntry("agility_short-cut", "Slayer Tower Broken Window (1st Floor) (Level 18)", "Travel • Agility", 3422, 3550, 1, "Level requirement: Level 18 Agility", "Connects: Slayer Tower 1st floor window climb", "Shortcut between exterior walkway and Infernal Mage level");
		addEntry("agility_short-cut", "Slayer Tower North Wall (Ground Floor) (Level 61)", "Travel • Agility", 3448, 3578, 0, "Level requirement: Level 61 Agility", "Connects: Slayer Tower northern exterior to interior stairs", "Quick access to upper Slayer Tower rooms");
		addEntry("agility_short-cut", "Slayer Tower Spire Climb (Ground Floor) (Level 71)", "Travel • Agility", 3447, 3576, 0, "Level requirement: Level 71 Agility", "Connects: Slayer Tower north-east wall climb to top floor", "Fast route to Abyssal Demons and Gargoyles");
		addEntry("agility_short-cut", "Slayer Tower North Wall (1st Floor) (Level 61)", "Travel • Agility", 3448, 3578, 1, "Level requirement: Level 61 Agility", "Connects: Slayer Tower 1st floor northern landing", "Quick transition between floors");
		addEntry("agility_short-cut", "Slayer Tower Ivy Climb (2nd Floor) (Level 81)", "Travel • Agility", 3447, 3576, 2, "Level requirement: Level 81 Agility", "Connects: Slayer Tower top floor (Abyssal demons) directly to outside ground", "Fastest exit from upper Slayer Tower");
		addEntry("agility_short-cut", "Burgh de Rott Low Fence (Level 25)", "Travel • Agility", 3471, 3219, 0, "Level requirement: Level 25 Agility", "Connects: Burgh de Rott north-west fence to swamp perimeter", "Requirements: Darkness of Hallowvale quest (partial)");
		addEntry("agility_short-cut", "Mos Le'Harmless Estuary Stepping Stone (Level 60)", "Travel • Agility", 3711, 2970, 0, "Level requirement: Level 60 Agility", "Connects: Mos Le'Harmless town across estuary to Cave horrors & jungle", "Requirements: Cabin Fever quest completed");
		addEntry("agility_short-cut", "Fossil Island Mushroom Meadow Stepping Stones (Level 50)", "Travel • Agility", 3664, 3810, 0, "Level requirement: Level 50 Agility", "Connects: Mushroom Meadow across water to western forest", "Requirements: Bone Voyage quest • Quicker Herbiboar tracking");
		addEntry("agility_short-cut", "Fossil Island Museum Camp Crevice (Level 70)", "Travel • Agility", 3713, 3828, 0, "Level requirement: Level 70 Agility", "Connects: Museum Camp directly through cliff to hardwood tree patch", "Requirements: Bone Voyage quest completed", "High-value shortcut for Teak/Mahogany tree runs");
		addEntry("agility_short-cut", "Fossil Island Museum Camp Crevice (Level 70)", "Travel • Agility", 3715, 3816, 0, "Level requirement: Level 70 Agility", "Connects: Hardwood tree patch through cliff to Museum Camp", "Requirements: Bone Voyage quest completed", "High-value shortcut for Teak/Mahogany tree runs");
		addEntry("agility_short-cut", "Fossil Island Volcanic Meadow Crevice (Level 70)", "Travel • Agility", 3730, 3831, 0, "Level requirement: Level 70 Agility", "Connects: Museum Camp north path to Volcanic Meadow", "Requirements: Bone Voyage quest completed");
		addEntry("agility_short-cut", "Fossil Island Volcanic Meadow Crevice (Level 70)", "Travel • Agility", 3745, 3831, 0, "Level requirement: Level 70 Agility", "Connects: Volcanic Meadow path toward the Volcanic Mine", "Requirements: Bone Voyage quest completed");
		addEntry("agility_short-cut", "Fossil Island Volcanic Mine Rope (Level 64)", "Travel • Agility", 3781, 3822, 0, "Level requirement: Level 64 Agility", "Connects: Volcanic Meadow path across fissure to Volcanic Mine", "Requirements: Bone Voyage quest completed");
		addEntry("agility_short-cut", "Taverley Wall Climbing Rocks (Level 66)", "Travel • Agility", 2945, 3439, 0, "Level requirement: Level 66 Agility", "Connects: Eastern Taverley wall directly into Taverley tree patch", "Fastest access to Taverley farming patch from Falador");
		addEntry("agility_short-cut", "Draynor Manor Stepping Stones (Level 31)", "Travel • Agility", 3107, 3367, 0, "Level requirement: Level 31 Agility", "Connects: River Lum crossing north-west of Lumbridge to Draynor Manor", "Fast route between Champions' Guild / Lumbridge and Draynor");
		addEntry("agility_short-cut", "Lumbridge Cow Field Fence (Level 13)", "Travel • Agility", 3236, 3295, 0, "Level requirement: Level 13 Agility", "Connects: Lumbridge north cow pen to Varrock south road", "Quick hop over the wooden fence");
		addEntry("agility_short-cut", "Varrock Palace Garden Trellis (Level 35)", "Travel • Agility", 3228, 3471, 0, "Level requirement: Level 35 Agility", "Connects: Southern wall of Varrock Palace into the garden", "Direct access into the Varrock Palace grounds");
		addEntry("agility_short-cut", "Barrows Dry Stone Wall (Level 72)", "Travel • Agility", 3546, 3282, 0, "Level requirement: Level 72 Agility", "Connects: South-western Barrows mound perimeter to swamp path", "Faster pathing during Barrows brother runs");
		addEntry("agility_short-cut", "Pollnivneach River Stepping Stone (Level 71)", "Travel • Agility", 3373, 2957, 0, "Level requirement: Level 71 Agility", "Connects: River Elid crossing between Pollnivneach and Nardah", "Fast desert river crossing without carpet ride");
		addEntry("agility_short-cut", "Waterbirth Island Rocks (Level 85)", "Travel • Agility", 2546, 3748, 0, "Level requirement: Level 85 Agility", "Connects: Waterbirth Island boat landing directly to second cave entrance", "Bypasses snare traps and monsters on the island surface");
		addEntry("agility_short-cut", "Shilo Village North-East Rocks (Level 79)", "Travel • Agility", 2871, 3007, 0, "Level requirement: Level 79 Agility", "Connects: Shilo Village gem mine directly to eastern jungle", "High-level shortcut for quick entry to Shilo Village");
		addEntry("agility_short-cut", "Wilderness Chaos Temple Lava Stepping Stone (Level 72)", "Travel • Agility", 3268, 3627, 0, "Level requirement: Level 72 Agility", "Connects: Level 13 Wilderness Chaos Temple across lava to Slayer Cave", "Danger: Wilderness PvP area");
		addEntry("agility_short-cut", "Mausoleum Repaired Bridge (Level 69)", "Travel • Agility", 3505, 3558, 0, "Level requirement: Level 69 Agility", "Connects: Morytania mainland across water to the Mausoleum island", "Requirements: Priest in Peril / Nature Spirit / repairable bridge");
	}

	/** Hunter training areas. */
	private static void addHunterAreas()
	{
		addEntry("hunter_training", "Feldip Hills - Red Chinchompas (Level 63)", "Skilling • Hunter", 2558, 2912, 0, "Target: Carnivorous chinchompa (Red chinchompa)", "Level requirement: Level 63 Hunter", "Catch method: Box trap (up to 5 traps)", "Region: Feldip Hills");
		addEntry("hunter_training", "Feldip Hills - Tropical Wagtails (Level 19)", "Skilling • Hunter", 2550, 2930, 0, "Target: Tropical wagtail (feathers for strikes)", "Level requirement: Level 19 Hunter", "Catch method: Bird snare", "Region: Feldip Hills");
		addEntry("hunter_training", "Feldip Hills - Spined Larupias (Level 31)", "Skilling • Hunter", 2560, 2890, 0, "Target: Spined larupia (Larupia fur / hunter outfit)", "Level requirement: Level 31 Hunter", "Catch method: Pitfall trap (Logs & Teasing stick)", "Region: Feldip Hills");
		addEntry("hunter_training", "Feldip Hills - Barb-tailed Kebbits (Level 33)", "Skilling • Hunter", 2540, 2875, 0, "Target: Barb-tailed kebbit (Barb-tailed harpoon)", "Level requirement: Level 33 Hunter", "Catch method: Deadfall trap (Logs & Knife)", "Region: Feldip Hills");
		addEntry("hunter_training", "Feldip Hills - Black Warlocks (Level 45)", "Skilling • Hunter", 2568, 2938, 0, "Target: Black warlock butterfly (Strength boost)", "Level requirement: Level 45 Hunter", "Catch method: Butterfly net & Butterfly jar", "Region: Feldip Hills");
		addEntry("hunter_training", "Feldip Hills - Crimson Swifts (Level 1)", "Skilling • Hunter", 2590, 2885, 0, "Target: Crimson swift (Red feathers)", "Level requirement: Level 1 Hunter", "Catch method: Bird snare", "Region: Feldip Hills");
		addEntry("hunter_training", "Karamja / Cairn Isle - Horned Graahks (Level 41)", "Skilling • Hunter", 2787, 3001, 0, "Target: Horned graahk (Graahk fur / Horned graahk pouch)", "Level requirement: Level 41 Hunter", "Catch method: Pitfall trap (Logs & Teasing stick)", "Region: Karamja / Cairn Isle");
		addEntry("hunter_training", "Piscatoris - Grey Chinchompas (Level 53)", "Skilling • Hunter", 2336, 3584, 0, "Target: Chinchompa (Grey chinchompa)", "Level requirement: Level 53 Hunter", "Catch method: Box trap", "Region: Piscatoris Hunter Area");
		addEntry("hunter_training", "Piscatoris - Falconry (Level 43–69)", "Skilling • Hunter", 2380, 3599, 0, "Target: Spotted kebbit (43), Dark kebbit (57), Dashing kebbit (69)", "Level requirement: Level 43 / 57 / 69 Hunter", "Catch method: Falconry (Gyr falcon & Falconry glove - 500 coins)", "Region: Piscatoris (Falconry Area)");
		addEntry("hunter_training", "Piscatoris - Prickly Kebbits (Level 37)", "Skilling • Hunter", 2350, 3620, 0, "Target: Prickly kebbit (Kebbit spikes for bolt tips)", "Level requirement: Level 37 Hunter", "Catch method: Deadfall trap (Logs & Knife)", "Region: Piscatoris Hunter Area");
		addEntry("hunter_training", "Piscatoris - Ferrets (Level 27)", "Skilling • Hunter", 2340, 3635, 0, "Target: Ferret (used to flush rabbits / Eagles' Peak)", "Level requirement: Level 27 Hunter", "Catch method: Box trap", "Region: Piscatoris Hunter Area");
		addEntry("hunter_training", "Piscatoris - Copper Longtails (Level 9)", "Skilling • Hunter", 2345, 3590, 0, "Target: Copper longtail (Copper feathers)", "Level requirement: Level 9 Hunter", "Catch method: Bird snare", "Region: Piscatoris Hunter Area");
		addEntry("hunter_training", "Eagle's Peak - Ferrets & Kebbits (Level 27)", "Skilling • Hunter", 2330, 3500, 0, "Target: Ferret (Requires Eagles' Peak quest started)", "Level requirement: Level 27 Hunter", "Catch method: Box trap / Deadfall", "Region: Kandarin (Eagle's Peak)");
		addEntry("hunter_training", "Ourania / Ardougne - Red Salamanders (Level 59)", "Skilling • Hunter", 2448, 3219, 0, "Target: Red salamander", "Level requirement: Level 59 Hunter", "Catch method: Net trap (Ropes & Small fishing nets on young trees)", "Region: Kandarin (South of Ardougne / Ourania)");
		addEntry("hunter_training", "Ourania - Hunter Training Area (Level 1–59)", "Skilling • Hunter", 2476, 3240, 0, "Target: Red salamander (59), Crimson swift (1)", "Level requirement: Level 1–59 Hunter", "Catch method: Net trap & Bird snare", "Region: Kandarin (Ourania / Castle Wars North)");
		addEntry("hunter_training", "Rellekka - Polar Kebbits & Kyatts (Level 1–55)", "Skilling • Hunter", 2720, 3780, 0, "Target: Polar kebbit (1), Cerulean twitch (11), Sabre-toothed kyatt (55)", "Level requirement: Level 1 / 11 / 25 / 35 / 55 Hunter", "Catch method: Tracking (Noose wand), Bird snare, Pitfall trap", "Region: Fremennik Province (Rellekka Hunter Area)");
		addEntry("hunter_training", "Rellekka - Sabre-toothed Kyatts (Level 55)", "Skilling • Hunter", 2725, 3770, 0, "Target: Sabre-toothed kyatt (Kyatt fur / teleport pouch)", "Level requirement: Level 55 Hunter", "Catch method: Pitfall trap (Logs & Teasing stick)", "Region: Fremennik Province");
		addEntry("hunter_training", "Rellekka - Cerulean Twitches (Level 11)", "Skilling • Hunter", 2725, 3765, 0, "Target: Cerulean twitch (Twitcher feathers)", "Level requirement: Level 11 Hunter", "Catch method: Bird snare", "Region: Fremennik Province");
		addEntry("hunter_training", "Wilderness - Black Chinchompas (Level 73)", "Skilling • Hunter", 3143, 3771, 0, "Target: Black chinchompa (Highest XP & combat damage)", "Level requirement: Level 73 Hunter (Wilderness Level 32–34)", "Catch method: Box trap (up to 6 traps in Wilderness)", "Region: Wilderness (High risk PvP area)");
		addEntry("hunter_training", "Wilderness - Black Salamanders (Level 67)", "Skilling • Hunter", 3295, 3673, 0, "Target: Black salamander", "Level requirement: Level 67 Hunter (Wilderness Level 19–21)", "Catch method: Net trap (Ropes & Small fishing nets on young trees)", "Region: Wilderness (Boneyard / Lava Maze)");
		addEntry("hunter_training", "Uzer - Orange Salamanders (Level 47)", "Skilling • Hunter", 3402, 3104, 0, "Target: Orange salamander", "Level requirement: Level 47 Hunter", "Catch method: Net trap (Ropes & Small fishing nets on young trees)", "Region: Kharidian Desert (East of Uzer • Waterskins needed)");
		addEntry("hunter_training", "Uzer - Desert Lizards (Level 22)", "Skilling • Hunter", 3410, 3060, 0, "Target: Desert lizard / Small lizard", "Level requirement: Level 22 Hunter", "Catch method: Box trap + Ice coolers (slayer item)", "Region: Kharidian Desert (Uzer • Waterskins needed)");
		addEntry("hunter_training", "Canifis - Swamp Lizards (Level 29)", "Skilling • Hunter", 3536, 3445, 0, "Target: Swamp lizard (Green salamander)", "Level requirement: Level 29 Hunter", "Catch method: Net trap (Ropes & Small fishing nets on young trees)", "Region: Morytania (Mort Myre Swamp • Priest in Peril)");
		addEntry("hunter_training", "Mort Myre Swamp - Swamp Lizards (Level 29)", "Skilling • Hunter", 3554, 3438, 0, "Target: Swamp lizard (Mort Myre Swamp)", "Level requirement: Level 29 Hunter", "Catch method: Net trap (Ropes & Small fishing nets)", "Region: Morytania");
		addEntry("hunter_training", "Darkmeyer / Sanguinesti - Hunter Area", "Skilling • Hunter", 3678, 3405, 0, "Target: Swamp lizard (29), Moonlight moth (75)", "Level requirement: Level 29 / 75 Hunter", "Catch method: Net trap & Butterfly net", "Region: Morytania (Sanguinesti Region)");
		addEntry("hunter_training", "Fossil Island - Herbiboar (Mushroom Meadow) (Level 80)", "Skilling • Hunter", 3694, 3800, 0, "Target: Herbiboar (Harvest herbs with Magic secateurs • Pet chance)", "Level requirement: Level 80 Hunter & 31 Herblore", "Catch method: Tracking (Inspect fungi and burrow holes)", "Region: Fossil Island (Bone Voyage quest)");
		addEntry("hunter_training", "Fossil Island - Herbiboar (Volcanic Meadow) (Level 80)", "Skilling • Hunter", 3702, 3809, 0, "Target: Herbiboar (Harvest herbs with Magic secateurs)", "Level requirement: Level 80 Hunter & 31 Herblore", "Catch method: Tracking (Inspect fungi and mud patches)", "Region: Fossil Island");
		addEntry("hunter_training", "Fossil Island - Birdhouse Trail (House on the Hill)", "Skilling • Hunter", 3704, 3829, 0, "Target: Passive Hunter XP, Bird nests & raw bird meat (50 min timer)", "Level requirement: Level 5–34+ Hunter (Crafting 5–89)", "Catch method: Clockwork Birdhouses (fill with 10 Seeds)", "Region: Fossil Island");
		addEntry("hunter_training", "Fossil Island - Birdhouses (Mushroom Forest)", "Skilling • Hunter", 3685, 3870, 0, "Target: Bird nests, Seeds, and Clue scrolls (2 birdhouse spaces)", "Level requirement: Level 5–34+ Hunter", "Catch method: Clockwork Birdhouses (Regular to Redwood)", "Region: Fossil Island (North Mushroom Forest)");
		addEntry("hunter_training", "Fossil Island - Birdhouses (Verdant Valley)", "Skilling • Hunter", 3750, 3850, 0, "Target: Bird nests, Tree seeds, and passive Hunter XP", "Level requirement: Level 5–34+ Hunter", "Catch method: Clockwork Birdhouses (2 birdhouse spaces)", "Region: Fossil Island (Verdant Valley)");
		addEntry("hunter_training", "Kebos Lowlands - Chinchompas & Kebbits (Level 53)", "Skilling • Hunter", 1185, 3595, 0, "Target: Grey chinchompa (53), Spotted kebbit (43), Crimson swift (1)", "Level requirement: Level 53 Hunter", "Catch method: Box trap, Deadfall, Bird snare", "Region: Great Kourend (Kebos Lowlands / Mount Quidamortem)");
		addEntry("hunter_training", "Mount Karuulm - Hunter Training Area (Level 1–41)", "Skilling • Hunter", 1364, 3632, 0, "Target: Ruby harvest (15), Crimson swift (1), Horned graahk (41)", "Level requirement: Level 1 / 15 / 41 Hunter", "Catch method: Butterfly net, Bird snare, Pitfall trap", "Region: Great Kourend (Mount Karuulm)");
		addEntry("hunter_training", "Kourend Woodland - Hunter Area West (Level 15–43)", "Skilling • Hunter", 1482, 3504, 0, "Target: Ruby harvest (15), Horned graahk (41), Spotted kebbit (43)", "Level requirement: Level 15 / 41 / 43 Hunter", "Catch method: Butterfly net, Pitfall trap, Deadfall", "Region: Great Kourend (Kourend Woodland)");
		addEntry("hunter_training", "Kourend Woodland - Hunter Area Central (Level 11–41)", "Skilling • Hunter", 1513, 3478, 0, "Target: Cerulean twitch (11), Ruby harvest (15), Horned graahk (41)", "Level requirement: Level 11 / 15 / 41 Hunter", "Catch method: Bird snare, Butterfly net, Pitfall trap", "Region: Great Kourend (Kourend Woodland)");
		addEntry("hunter_training", "Hosidius - Kourend Woodland Hunter Area (Level 1–43)", "Skilling • Hunter", 1557, 3436, 0, "Target: Crimson swift (1), Ruby harvest (15), Spotted kebbit (43)", "Level requirement: Level 1 / 15 / 43 Hunter", "Catch method: Bird snare, Butterfly net, Deadfall", "Region: Great Kourend (Hosidius / Kourend Woodland)");
		addEntry("hunter_training", "Hunter's Guild (Level 46–91)", "Skilling • Hunter", 1557, 3045, 0, "Activity: Hunter Rumours & Guild tasks (Guildmaster Guilden)", "Level requirement: Level 46+ Hunter (Novice 46, Adept 65, Expert 72, Master 91)", "Rewards: Quetzal whistle, Meat pouch, Hunter gear, Huntsman's kit", "Region: Varlamore (Avium Savannah • Children of the Sun)");
		addEntry("hunter_training", "Varlamore - Sunlight Antelopes (Level 72)", "Skilling • Hunter", 1696, 2984, 0, "Target: Sunlight antelope (Sunlight antelope horn, fur, meat)", "Level requirement: Level 72 Hunter", "Catch method: Pitfall trap (Logs & Teasing stick)", "Region: Varlamore (Avium Savannah)");
		addEntry("hunter_training", "Varlamore - Moonlight Antelopes (Level 91)", "Skilling • Hunter", 1560, 2920, 0, "Target: Moonlight antelope (Moonlight antelope horn, fur, meat)", "Level requirement: Level 91 Hunter", "Catch method: Pitfall trap (Logs & Teasing stick)", "Region: Varlamore (Avium Savannah / Hunters' Guild South)");
		addEntry("hunter_training", "Varlamore - Sunlight Moths (Level 65)", "Skilling • Hunter", 1720, 2980, 0, "Target: Sunlight moth (Restores 8–19 Prayer points per sip)", "Level requirement: Level 65 Hunter", "Catch method: Butterfly net & Butterfly jar (or barehanded at 75)", "Region: Varlamore (Avium Savannah)");
		addEntry("hunter_training", "Varlamore - Moonlight Moths (Level 75)", "Skilling • Hunter", 1535, 3065, 0, "Target: Moonlight moth (Area of effect stat restoration)", "Level requirement: Level 75 Hunter", "Catch method: Butterfly net & Butterfly jar (or barehanded at 85)", "Region: Varlamore (Hunter's Guild / Aldarin)");
		addEntry("hunter_training", "Varlamore - Embertailed Jerboas (Level 39)", "Skilling • Hunter", 1680, 2925, 0, "Target: Embertailed jerboa (Jerboa tail for Hunter whistle crafting)", "Level requirement: Level 39 Hunter", "Catch method: Box trap", "Region: Varlamore (Avium Savannah)");
		addEntry("hunter_training", "Varlamore - Tecu Salamanders (Level 79)", "Skilling • Hunter", 1475, 3090, 0, "Target: Tecu salamander (Highest tier salamander)", "Level requirement: Level 79 Hunter", "Catch method: Net trap (Ropes & Small fishing nets on young trees)", "Region: Varlamore (Ralos' Rise / Cam Torum surface)");
		addEntry("hunter_training", "Isle of Souls - Red Chinchompas (Level 63)", "Skilling • Hunter", 2220, 2870, 0, "Target: Carnivorous chinchompa (Red chinchompa)", "Level requirement: Level 63 Hunter", "Catch method: Box trap", "Region: Isle of Souls (South-west)");
		addEntry("hunter_training", "Isle of Souls - Pyre Foxes (Level 57)", "Skilling • Hunter", 2250, 2900, 0, "Target: Pyre fox (Fox fur / Pyre logs)", "Level requirement: Level 57 Hunter", "Catch method: Deadfall trap (Logs & Knife)", "Region: Isle of Souls (Central)");
		addEntry("hunter_training", "Isle of Souls - Crimson Swifts (Level 1)", "Skilling • Hunter", 2190, 2880, 0, "Target: Crimson swift (Red feathers)", "Level requirement: Level 1 Hunter", "Catch method: Bird snare", "Region: Isle of Souls (West coast)");
		addEntry("hunter_training", "Puro-Puro - Impling Catching (Level 17–90+)", "Skilling • Hunter", 2592, 4320, 0, "Target: Implings (Baby, Eclectic, Nature, Magpie, Ninja, Dragon, Lucky)", "Level requirement: Level 17–90+ Hunter", "Catch method: Butterfly net & Impling jars", "Region: Zanaris / Puro-Puro (Crop circles)");
		addEntry("hunter_training", "Prifddinas - Crystal Implings & Chinchompas (Level 63–80)", "Skilling • Hunter", 3270, 6080, 0, "Target: Crystal implings (80), Chinchompas (63)", "Level requirement: Level 63+ Hunter (Crystal Implings: Level 80 Hunter)", "Catch method: Box trap & Butterfly net / jars", "Region: Tirannwn (Prifddinas • Song of the Elves)");
		addEntry("hunter_training", "The Great Conch - Red Chinchompas (Level 63)", "Skilling • Hunter", 3258, 2378, 0, "Target: Carnivorous chinchompa (Red chinchompa)", "Level requirement: Level 63 Hunter", "Catch method: Box trap", "Region: The Great Conch (hill east of the town)");
		addEntry("hunter_training", "The Great Conch - Tropical Wagtails (Level 19)", "Skilling • Hunter", 3143, 2485, 0, "Target: Tropical wagtail (Stripy feathers)", "Level requirement: Level 19 Hunter", "Catch method: Bird snare", "Region: The Great Conch (hunting area)");
		addEntry("hunter_training", "The Great Conch - Black Warlocks (Level 45)", "Skilling • Hunter", 3177, 2455, 0, "Target: Black warlock butterfly (Strength boost)", "Level requirement: Level 45 Hunter", "Catch method: Butterfly net & Butterfly jar", "Region: The Great Conch (Sacred Grove East)");
		addEntry("hunter_training", "The Great Conch - Orange Salamanders (Level 47)", "Skilling • Hunter", 3122, 2532, 0, "Target: Orange salamander", "Level requirement: Level 47 Hunter", "Catch method: Net trap (Ropes & Small fishing nets on young trees)", "Region: The Great Conch (Abalone Cliffs)");
		addEntry("hunter_training", "The Great Conch - Blue Crabs (North coast) (Level 48)", "Skilling • Hunter", 3213, 2516, 0, "Target: Blue crab (cook, or break down for Herblore)", "Level requirement: Level 48 Hunter (10 Construction to build traps)", "Catch method: Hinged-lid crab trap baited with fish offcuts", "Region: The Great Conch (north coast, 21 spawns)");
		addEntry("hunter_training", "The Great Conch - Blue Crabs (East coast) (Level 48)", "Skilling • Hunter", 3251, 2434, 0, "Target: Blue crab (cook, or break down for Herblore)", "Level requirement: Level 48 Hunter (10 Construction to build traps)", "Catch method: Hinged-lid crab trap baited with fish offcuts", "Region: The Great Conch (east coast, 16 spawns)");
		addEntry("hunter_training", "Dognose Island - Hunter Area", "Skilling • Hunter", 3061, 2639, 0, "Target: Sailing-island hunter creatures", "Level requirement: Check Hunter skill guide for creature levels", "Catch method: Traps, snares, or tracking", "Region: Turtle Belt (Dognose Island)");
		addEntry("hunter_training", "Chinchompa Island - Red Chinchompas (Level 63)", "Skilling • Hunter", 1892, 3429, 0, "Target: Carnivorous chinchompa (Red chinchompa)", "Level requirement: Level 63 Hunter", "Catch method: Box trap", "Region: Chinchompa Island (Sailing)");
		addEntry("hunter_training", "The Pandemonium - Red Crabs (Level 21)", "Skilling • Hunter", 3069, 2986, 0, "Target: Red crab (cook, or break down for Herblore)", "Level requirement: Level 21 Hunter (10 Construction to build traps)", "Catch method: Hinged-lid crab trap baited with fish offcuts", "Region: The Pandemonium (Sailing)");
		addEntry("hunter_training", "The Crown Jewel - Rainbow Crabs (Level 77)", "Skilling • Hunter", 1765, 2659, 0, "Target: Rainbow crab (cook, or break down for Herblore)", "Level requirement: Level 77 Hunter (10 Construction to build traps)", "Catch method: Hinged-lid crab trap baited with fine fish offcuts", "Region: The Crown Jewel (Sailing)");
	}

	/** Everything added by the Sailing expansion, grouped as the game groups it. */
	private static void addSailingAreas()
	{
		// Sailing expansion - Dungeons and Slayer caves
		addEntry("dungeon", "Pandemonium Cave", "Dungeons", 3070, 2986, 0, "Type: Low-level cavern & mining tunnel", "May require a light source or combat gear", "Region: The Pandemonium (Sailing)");
		addEntry("dungeon", "Charred Island Dungeon", "Dungeons", 2661, 2395, 0, "Type: Slayer dungeon (Lava strykewyrms)", "Requirements: Level 62 Slayer for lava strykewyrms", "Region: Charred Island (Sailing)");
		addEntry("dungeon", "Deepfin Mine", "Dungeons", 1924, 2758, 0, "Type: Mining cavern (lead & nickel deposits)", "Tool needed: Pickaxe; bring a light source", "Region: Deepfin Point (Sailing)");
		addEntry("dungeon", "Here Be Minotaurs", "Dungeons", 1959, 3117, 0, "Type: Underground minotaur cavern", "Inhabitants: Minotaurs • Big bone spawns", "Region: Minotaurs' Rest (Sailing)");
		addEntry("dungeon", "Ynysdail Cavern", "Dungeons", 2223, 3466, 0, "Type: High-level Slayer cavern (Aquanites)", "Requirements: Level 78 Slayer • Slayer task / unlock", "Region: Ynysdail (Sailing)");
		addEntry("dungeon", "Buccaneers' Laboratory", "Dungeons", 2081, 3690, 0, "Type: Underground pirate laboratory", "May require a light source or combat gear", "Region: Buccaneers' Haven (Sailing)");
		addEntry("dungeon", "Grimstone Dungeon", "Dungeons", 2928, 4056, 0, "Type: High-level dragon dungeon (Frost Dragons)", "Requirements: Anti-dragon shield & Antifire protection", "Region: Grimstone (Sailing)");
		addEntry("dungeon", "Kurask Lair", "Dungeons", 1203, 2733, 0, "Type: Slayer cave (Kurasks)", "Requirements: Level 70 Slayer, Leaf-bladed weapon or Broad bolts", "Region: Laguna Aurorae (Sailing)");
		addEntry("dungeon", "Sunbleak Cave", "Dungeons", 2190, 2327, 0, "Type: Underground mining cavern", "Features: Coal, Mithril, and Adamantite rocks", "Region: Sunbleak Island (Sailing)");
		addEntry("dungeon", "Brittle Isle Cave", "Dungeons", 1955, 4056, 0, "Type: Unmarked sea cave", "Requirements: Level 81 Sailing to access island", "Region: Brittle Isle (Sailing)");
		addEntry("dungeon", "Wyrmscraig Cavern", "Dungeons", 2580, 8613, 0, "Type: Underground cavern", "Access: Partial completion of Fallen From Grace", "Region: Wyrmscraig (Sailing)");

		// Sailing expansion - Mining sites
		addEntry("mining_site", "The Pandemonium - Mining Site", "Skilling • Mining", 3069, 2987, 0, "Ores available: Copper (1), Tin (1), Iron (15), Lead (25)", "Tool needed: Pickaxe (Bronze to Crystal)", "Region: The Pandemonium (Sailing)");
		addEntry("mining_site", "The Onyx Crest - Mining Site", "Skilling • Mining", 2998, 2288, 0, "Ores available: Iron (15), Lead (25), Silver (20), Gem (40), Gold (40)", "Tool needed: Pickaxe (Bronze to Crystal)", "Region: The Onyx Crest (Sailing)");
		addEntry("mining_site", "Deepfin Point - Mining Site", "Skilling • Mining", 1923, 2759, 0, "Ores available: Lead (25), Nickel (74)", "Tool needed: Pickaxe (Bronze to Crystal)", "Region: Deepfin Point (Sailing)");
		addEntry("mining_site", "Ynysdail - Mining Site", "Skilling • Mining", 2222, 3467, 0, "Ores: Iron (15), Mithril (55), Adamant (70), Nickel (74), Runite (85)", "Tool needed: Pickaxe (Bronze to Crystal)", "Region: Ynysdail (Sailing)");
		addEntry("mining_site", "Grimstone - Mining Site", "Skilling • Mining", 2927, 4057, 0, "Ores available: Coal (30), Adamantite (70), Nickel (74), Runite (85)", "Tool needed: Pickaxe (Bronze to Crystal)", "Region: Grimstone (Sailing)");
		addEntry("mining_site", "Sunbleak Island - Mining Site", "Skilling • Mining", 2189, 2328, 0, "Ores available: Coal (30), Mithril (55), Adamantite (70)", "Tool needed: Pickaxe (Bronze to Crystal)", "Region: Sunbleak Island (Sailing)");
		addEntry("mining_site", "Vatrachos Island - Mining Site", "Skilling • Mining", 1872, 2986, 0, "Ores available: Clay (1), Copper (1), Tin (1), Iron (15), Mithril (55)", "Tool needed: Pickaxe (Bronze to Crystal)", "Region: Vatrachos Island (Sailing)");

		// Sailing expansion - Woodcutting
		addEntry("rare_trees", "Sunbleak Island - Ironwood trees (Level 80)", "Skilling • Woodcutting", 2191, 2327, 0, "Trees: Ironwood tree (High-tier shipbuilding timber)", "Level requirement: Level 80 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Region: Sunbleak Island (Sailing)");
		addEntry("rare_trees", "Drumstick Isle - Rosewood trees (Level 92)", "Skilling • Woodcutting", 2151, 3530, 0, "Trees: Rosewood tree (Master-tier shipbuilding timber)", "Level requirement: Level 92 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Region: Drumstick Isle (Sailing)");
		addEntry("rare_trees", "Dognose Island - Magic tree (Level 75)", "Skilling • Woodcutting", 3062, 2639, 0, "Trees: Magic tree", "Level requirement: Level 75 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Region: Dognose Island (Sailing)");
		addEntry("rare_trees", "Lledrith Island - Magic tree (Level 75)", "Skilling • Woodcutting", 2098, 3188, 0, "Trees: Magic tree", "Level requirement: Level 75 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Region: Lledrith Island (Sailing)");
		addEntry("rare_trees", "Anglers' Retreat - Rare Trees (Level 15–60)", "Skilling • Woodcutting", 2468, 2721, 0, "Trees: Oak (15), Willow (30), Yew (60)", "Level requirement: Level 15 / 30 / 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Region: Anglers' Retreat (Sailing)");
		addEntry("rare_trees", "Tear of the Soul - Yew trees (Level 60)", "Skilling • Woodcutting", 2319, 2774, 0, "Trees: Yew tree", "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Region: Tear of the Soul (Sailing)");
		addEntry("rare_trees", "Deepfin Point - Rare Trees (Level 15–60)", "Skilling • Woodcutting", 1925, 2758, 0, "Trees: Oak (15), Yew (60)", "Level requirement: Level 15 / 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Region: Deepfin Point (Sailing)");
		addEntry("rare_trees", "Laguna Aurorae - Yew trees (Level 60)", "Skilling • Woodcutting", 1204, 2733, 0, "Trees: Yew tree", "Level requirement: Level 60 Woodcutting", "Tool needed: Axe (Bronze to Crystal)", "Region: Laguna Aurorae (Sailing)");

		// Sailing expansion - Thieving
		addEntry("thieving", "Dognose Island - Rusty Pirate Chest (Level 33)", "Skilling • Thieving", 3063, 2639, 0, "Activity: Pick locked pirate chest (Rusty chest)", "Level requirement: Level 33 Thieving", "Region: Dognose Island (Sailing)");
		addEntry("thieving", "The Crown Jewel - Tarnished Pirate Chest (Level 54)", "Skilling • Thieving", 1766, 2659, 0, "Activity: Pick locked pirate chest (Tarnished chest)", "Level requirement: Level 54 Thieving", "Region: The Crown Jewel (Sailing)");
		addEntry("thieving", "Ynysdail - Reinforced Pirate Chest (Level 76)", "Skilling • Thieving", 2224, 3466, 0, "Activity: Pick locked pirate chest (Reinforced chest)", "Level requirement: Level 76 Thieving", "Region: Ynysdail (Sailing)");
		addEntry("thieving", "Port Roberts - Pirate Stalls (Level 2–87)", "Skilling • Thieving", 1861, 3306, 0, "Activity: Steal from 9 non-depleting pirate stalls", "Level requirement: Level 2–87 Thieving (varies by stall)", "Region: Port Roberts (Sailing)");

		// Sailing expansion - Hunter areas
		addEntry("hunter_training", "Charred Island - Hunter Area", "Skilling • Hunter", 2660, 2396, 0, "Target: Sailing-island hunter creatures", "Level requirement: Check Hunter skill guide for creature levels", "Catch method: Traps, snares, or tracking", "Region: Charred Island (Sailing)");
		addEntry("hunter_training", "Anglers' Retreat - Hunter Area", "Skilling • Hunter", 2467, 2722, 0, "Target: Sailing-island hunter creatures", "Level requirement: Check Hunter skill guide for creature levels", "Catch method: Traps, snares, or tracking", "Region: Anglers' Retreat (Sailing)");
		addEntry("hunter_training", "Minotaurs' Rest - Hunter Area", "Skilling • Hunter", 1958, 3118, 0, "Target: Sailing-island hunter creatures", "Level requirement: Check Hunter skill guide for creature levels", "Catch method: Traps, snares, or tracking", "Region: Minotaurs' Rest (Sailing)");
		addEntry("hunter_training", "Brittle Isle - Hunter Area", "Skilling • Hunter", 1954, 4057, 0, "Target: Sailing-island hunter creatures", "Level requirement: Check Hunter skill guide for creature levels", "Catch method: Traps, snares, or tracking", "Region: Brittle Isle (Sailing)");
		addEntry("hunter_training", "Shimmering Atoll - Hunter Area", "Skilling • Hunter", 1558, 2771, 0, "Target: Sailing-island hunter creatures", "Level requirement: Check Hunter skill guide for creature levels", "Catch method: Traps, snares, or tracking", "Region: Shimmering Atoll (Sailing)");

		// Sailing expansion - Farming
		addEntry("farming_patch", "The Great Conch - Coral Nurseries (Level 28+)", "Skilling • Farming", 3175, 2367, 0, "Patches: Coral nursery (Sailing coral farming)", "Level requirement: Level 28+ Farming", "Tools needed: Spade, Rake", "Region: The Great Conch (Sailing)");
		addEntry("farming_patch", "The Great Conch - Calquat Farming Patch (Level 72)", "Skilling • Farming", 3176, 2367, 0, "Patches: Calquat tree patch", "Level requirement: Level 72 Farming", "Tools needed: Rake, Spade, Seed dibber, Watering can", "Region: The Great Conch (Sailing)");
		addEntry("farming_patch", "Anglers' Retreat - Tree Farming Patch (Level 15)", "Skilling • Farming", 2469, 2721, 0, "Patches: Tree patch (Oak, Willow, Maple, Yew, Magic)", "Level requirement: Level 15+ Farming", "Tools needed: Rake, Spade, Seed dibber, Watering can", "Region: Anglers' Retreat (Sailing)");

		// Sailing expansion - Travel
		addEntry("transportation", "The Great Conch - Fairy Ring (CJQ)", "Travel", 3177, 2367, 0, "Destination Code: CJQ", "Requirements: Fairytale II (partial) • Troubled Tortugans", "Staff: Dramen or Lunar staff");
		addEntry("transportation", "Grimstone - Fairy Ring (DLP)", "Travel", 2929, 4056, 0, "Destination Code: DLP", "Requirements: Fairytale II (partial) • Level 87 Sailing", "Staff: Dramen or Lunar staff");
		addEntry("transportation", "Laguna Aurorae - Spirit Tree", "Travel", 1205, 2733, 0, "Destination: Laguna Aurorae Spirit Tree", "Requirements: Tree Gnome Village quest completed", "Activity: Fast travel across the Spirit Tree network");

		addEntry("transportation", "Fairy Ring (AIQ)", "Travel • Fairy Ring", 2995, 3112, 0, "Destination Code: AIQ", "Requirements: Fairytale II - Cure a Queen (partial)", "Staff: Dramen or Lunar staff");
		addEntry("transportation", "Fairy Ring (AIR)", "Travel • Fairy Ring", 2699, 3249, 0, "Destination Code: AIR", "Requirements: Fairytale II - Cure a Queen (partial)", "Staff: Dramen or Lunar staff");
		addEntry("transportation", "Fairy Ring (AIS)", "Travel • Fairy Ring", 1430, 3323, 0, "Destination Code: AIS", "Requirements: Fairytale II - Cure a Queen (partial)", "Staff: Dramen or Lunar staff");
		addEntry("transportation", "Fairy Ring (AJP)", "Travel • Fairy Ring", 1648, 3011, 0, "Destination Code: AJP", "Requirements: Fairytale II - Cure a Queen (partial)", "Staff: Dramen or Lunar staff");
		addEntry("transportation", "Fairy Ring (AJR)", "Travel • Fairy Ring", 2779, 3615, 0, "Destination Code: AJR", "Requirements: Fairytale II - Cure a Queen (partial)", "Staff: Dramen or Lunar staff");
		addEntry("transportation", "Fairy Ring (AJS)", "Travel • Fairy Ring", 2499, 3898, 0, "Destination Code: AJS", "Requirements: Fairytale II - Cure a Queen (partial)", "Staff: Dramen or Lunar staff");
		addEntry("transportation", "Fairy Ring (AKP)", "Travel • Fairy Ring", 3283, 2704, 0, "Destination Code: AKP", "Requirements: Fairytale II - Cure a Queen (partial)", "Staff: Dramen or Lunar staff");
		addEntry("transportation", "Fairy Ring (AKQ)", "Travel • Fairy Ring", 2318, 3617, 0, "Destination Code: AKQ", "Requirements: Fairytale II - Cure a Queen (partial)", "Staff: Dramen or Lunar staff");
		addEntry("transportation", "Fairy Ring (AKR)", "Travel • Fairy Ring", 1823, 3539, 0, "Destination Code: AKR", "Requirements: Fairytale II - Cure a Queen (partial)", "Staff: Dramen or Lunar staff");
		addEntry("transportation", "Fairy Ring (AKS)", "Travel • Fairy Ring", 2570, 2958, 0, "Destination Code: AKS", "Requirements: Fairytale II - Cure a Queen (partial)", "Staff: Dramen or Lunar staff");
		addEntry("transportation", "Fairy Ring (ALP)", "Travel • Fairy Ring", 2502, 3638, 0, "Destination Code: ALP", "Requirements: Fairytale II - Cure a Queen (partial)", "Staff: Dramen or Lunar staff");
		addEntry("transportation", "Fairy Ring (ALQ)", "Travel • Fairy Ring", 3598, 3496, 0, "Destination Code: ALQ", "Requirements: Fairytale II - Cure a Queen (partial)", "Staff: Dramen or Lunar staff");
		addEntry("transportation", "Fairy Ring (ALR)", "Travel • Fairy Ring", 3059, 4877, 0, "Destination Code: ALR", "Requirements: Fairytale II - Cure a Queen (partial)", "Staff: Dramen or Lunar staff");
		addEntry("transportation", "Fairy Ring (ALS)", "Travel • Fairy Ring", 2643, 3497, 0, "Destination Code: ALS", "Requirements: Fairytale II - Cure a Queen (partial)", "Staff: Dramen or Lunar staff");
		addEntry("transportation", "Fairy Ring (BIP)", "Travel • Fairy Ring", 3409, 3326, 0, "Destination Code: BIP", "Requirements: Fairytale II - Cure a Queen (partial)", "Staff: Dramen or Lunar staff");
		addEntry("transportation", "Fairy Ring (BIQ)", "Travel • Fairy Ring", 3248, 3095, 0, "Destination Code: BIQ", "Requirements: Fairytale II - Cure a Queen (partial)", "Staff: Dramen or Lunar staff");
		addEntry("transportation", "Fairy Ring (BIS)", "Travel • Fairy Ring", 2635, 3268, 0, "Destination Code: BIS", "Requirements: Fairytale II - Cure a Queen (partial)", "Staff: Dramen or Lunar staff");
		addEntry("transportation", "Fairy Ring (BJP)", "Travel • Fairy Ring", 2264, 2976, 0, "Destination Code: BJP", "Requirements: Fairytale II - Cure a Queen (partial)", "Staff: Dramen or Lunar staff");
		addEntry("transportation", "Fairy Ring (BJS)", "Travel • Fairy Ring", 2147, 3069, 0, "Destination Code: BJS", "Requirements: Fairytale II - Cure a Queen (partial)", "Staff: Dramen or Lunar staff");
		addEntry("transportation", "Fairy Ring (BKP)", "Travel • Fairy Ring", 2384, 3037, 0, "Destination Code: BKP", "Requirements: Fairytale II - Cure a Queen (partial)", "Staff: Dramen or Lunar staff");
		addEntry("transportation", "Fairy Ring (BKR)", "Travel • Fairy Ring", 3468, 3433, 0, "Destination Code: BKR", "Requirements: Fairytale II - Cure a Queen (partial)", "Staff: Dramen or Lunar staff");
		addEntry("transportation", "Fairy Ring (BKS)", "Travel • Fairy Ring", 2411, 4436, 0, "Destination Code: BKS", "Requirements: Fairytale II - Cure a Queen (partial)", "Staff: Dramen or Lunar staff");
		addEntry("transportation", "Fairy Ring (BLP)", "Travel • Fairy Ring", 2432, 5127, 0, "Destination Code: BLP", "Requirements: Fairytale II - Cure a Queen (partial)", "Staff: Dramen or Lunar staff");
		addEntry("transportation", "Fairy Ring (BLR)", "Travel • Fairy Ring", 2739, 3353, 0, "Destination Code: BLR", "Requirements: Fairytale II - Cure a Queen (partial)", "Staff: Dramen or Lunar staff");
		addEntry("transportation", "Fairy Ring (BLS)", "Travel • Fairy Ring", 1293, 3495, 0, "Destination Code: BLS", "Requirements: Fairytale II - Cure a Queen (partial)", "Staff: Dramen or Lunar staff");
		addEntry("transportation", "Fairy Ring (CIP)", "Travel • Fairy Ring", 2512, 3886, 0, "Destination Code: CIP", "Requirements: Fairytale II - Cure a Queen (partial)", "Staff: Dramen or Lunar staff");
		addEntry("transportation", "Fairy Ring (CIQ)", "Travel • Fairy Ring", 2527, 3129, 0, "Destination Code: CIQ", "Requirements: Fairytale II - Cure a Queen (partial)", "Staff: Dramen or Lunar staff");
		addEntry("transportation", "Fairy Ring (CIR)", "Travel • Fairy Ring", 1303, 3762, 0, "Destination Code: CIR", "Requirements: Fairytale II - Cure a Queen (partial)", "Staff: Dramen or Lunar staff");
		addEntry("transportation", "Fairy Ring (CIS)", "Travel • Fairy Ring", 1636, 3869, 0, "Destination Code: CIS", "Requirements: Fairytale II - Cure a Queen (partial)", "Staff: Dramen or Lunar staff");
		addEntry("transportation", "Fairy Ring (CJQ)", "Travel • Fairy Ring", 3178, 2445, 0, "Destination Code: CJQ", "Requirements: Fairytale II - Cure a Queen (partial)", "Staff: Dramen or Lunar staff");
		addEntry("transportation", "Fairy Ring (CJR)", "Travel • Fairy Ring", 2704, 3578, 0, "Destination Code: CJR", "Requirements: Fairytale II - Cure a Queen (partial)", "Staff: Dramen or Lunar staff");
		addEntry("transportation", "Fairy Ring (CKQ)", "Travel • Fairy Ring", 1358, 2943, 0, "Destination Code: CKQ", "Requirements: Fairytale II - Cure a Queen (partial)", "Staff: Dramen or Lunar staff");
		addEntry("transportation", "Fairy Ring (CKR)", "Travel • Fairy Ring", 2800, 3005, 0, "Destination Code: CKR", "Requirements: Fairytale II - Cure a Queen (partial)", "Staff: Dramen or Lunar staff");
		addEntry("transportation", "Fairy Ring (CKS)", "Travel • Fairy Ring", 3446, 3472, 0, "Destination Code: CKS", "Requirements: Fairytale II - Cure a Queen (partial)", "Staff: Dramen or Lunar staff");
		addEntry("transportation", "Fairy Ring (CLP)", "Travel • Fairy Ring", 3081, 3208, 0, "Destination Code: CLP", "Requirements: Fairytale II - Cure a Queen (partial)", "Staff: Dramen or Lunar staff");
		addEntry("transportation", "Fairy Ring (CLR)", "Travel • Fairy Ring", 2737, 2739, 0, "Destination Code: CLR", "Requirements: Fairytale II - Cure a Queen (partial)", "Staff: Dramen or Lunar staff");
		addEntry("transportation", "Fairy Ring (CLS)", "Travel • Fairy Ring", 2681, 3083, 0, "Destination Code: CLS", "Requirements: Fairytale II - Cure a Queen (partial)", "Staff: Dramen or Lunar staff");
		addEntry("transportation", "Fairy Ring (DIP)", "Travel • Fairy Ring", 3036, 4761, 0, "Destination Code: DIP", "Requirements: Fairytale II - Cure a Queen (partial)", "Staff: Dramen or Lunar staff");
		addEntry("transportation", "Fairy Ring (DIS)", "Travel • Fairy Ring", 3109, 3149, 0, "Destination Code: DIS", "Requirements: Fairytale II - Cure a Queen (partial)", "Staff: Dramen or Lunar staff");
		addEntry("transportation", "Fairy Ring (DJP)", "Travel • Fairy Ring", 2657, 3232, 0, "Destination Code: DJP", "Requirements: Fairytale II - Cure a Queen (partial)", "Staff: Dramen or Lunar staff");
		addEntry("transportation", "Fairy Ring (DJR)", "Travel • Fairy Ring", 1452, 3659, 0, "Destination Code: DJR", "Requirements: Fairytale II - Cure a Queen (partial)", "Staff: Dramen or Lunar staff");
		addEntry("transportation", "Fairy Ring (DKP)", "Travel • Fairy Ring", 2899, 3113, 0, "Destination Code: DKP", "Requirements: Fairytale II - Cure a Queen (partial)", "Staff: Dramen or Lunar staff");
		addEntry("transportation", "Fairy Ring (DKR)", "Travel • Fairy Ring", 3126, 3496, 0, "Destination Code: DKR", "Requirements: Fairytale II - Cure a Queen (partial)", "Staff: Dramen or Lunar staff");
		addEntry("transportation", "Fairy Ring (DKS)", "Travel • Fairy Ring", 2743, 3721, 0, "Destination Code: DKS", "Requirements: Fairytale II - Cure a Queen (partial)", "Staff: Dramen or Lunar staff");
		addEntry("transportation", "Fairy Ring (DLP)", "Travel • Fairy Ring", 2923, 10455, 0, "Destination Code: DLP", "Requirements: Fairytale II - Cure a Queen (partial)", "Staff: Dramen or Lunar staff");
		addEntry("transportation", "Fairy Ring (DLQ)", "Travel • Fairy Ring", 3422, 3018, 0, "Destination Code: DLQ", "Requirements: Fairytale II - Cure a Queen (partial)", "Staff: Dramen or Lunar staff");
		addEntry("transportation", "Fairy Ring (DLR)", "Travel • Fairy Ring", 2212, 3101, 0, "Destination Code: DLR", "Requirements: Fairytale II - Cure a Queen (partial)", "Staff: Dramen or Lunar staff");
	}

	/** Farming patches, by patch type. */
	private static void addFarmingPatches()
	{
		addEntry("farming_patch", "Allotment Farming Patch", "Skilling • Farming", 3793, 2836, 0, "Patches: Allotment", "Tools needed: Rake, Spade, Seed dibber, Watering can", "Check Farming skill guide for seed levels");
		addEntry("farming_patch", "Allotment Farming Patch", "Skilling • Farming", 1269, 3730, 0, "Patches: Allotment", "Tools needed: Rake, Spade, Seed dibber, Watering can", "Check Farming skill guide for seed levels");
		addEntry("farming_patch", "Allotment/Flower Farming Patch", "Skilling • Farming", 3289, 6100, 0, "Patches: Allotment/Flower", "Tools needed: Rake, Spade, Seed dibber, Watering can", "Check Farming skill guide for seed levels");
		addEntry("farming_patch", "Allotment/Herb/Flower Farming Patch", "Skilling • Farming", 1729, 3558, 0, "Patches: Allotment/Herb/Flower", "Tools needed: Rake, Spade, Seed dibber, Watering can", "Check Farming skill guide for seed levels");
		addEntry("farming_patch", "Allotment/Herb/Flower Farming Patch", "Skilling • Farming", 3598, 3524, 0, "Patches: Allotment/Herb/Flower", "Tools needed: Rake, Spade, Seed dibber, Watering can", "Check Farming skill guide for seed levels");
		addEntry("farming_patch", "Allotment/Herb/Flower Farming Patch", "Skilling • Farming", 3052, 3309, 0, "Patches: Allotment/Herb/Flower", "Tools needed: Rake, Spade, Seed dibber, Watering can", "Check Farming skill guide for seed levels");
		addEntry("farming_patch", "Allotment/Herb/Flower Farming Patch", "Skilling • Farming", 2810, 3462, 0, "Patches: Allotment/Herb/Flower", "Tools needed: Rake, Spade, Seed dibber, Watering can", "Check Farming skill guide for seed levels");
		addEntry("farming_patch", "Allotment/Herb/Flower Farming Patch", "Skilling • Farming", 2663, 3375, 0, "Patches: Allotment/Herb/Flower", "Tools needed: Rake, Spade, Seed dibber, Watering can", "Check Farming skill guide for seed levels");
		addEntry("farming_patch", "Allotment/Herb/Flower Farming Patch", "Skilling • Farming", 1587, 3101, 0, "Patches: Allotment/Herb/Flower", "Tools needed: Rake, Spade, Seed dibber, Watering can", "Check Farming skill guide for seed levels");
		addEntry("farming_patch", "Anima/Herb Farming Patch", "Skilling • Farming", 1235, 3724, 0, "Patches: Anima/Herb", "Tools needed: Rake, Spade, Seed dibber, Watering can", "Check Farming skill guide for seed levels");
		addEntry("farming_patch", "Belladonna Farming Patch", "Skilling • Farming", 3084, 3356, 0, "Patches: Belladonna", "Tools needed: Rake, Spade, Seed dibber, Watering can", "Check Farming skill guide for seed levels");
		addEntry("farming_patch", "Belladonna Farming Patch", "Skilling • Farming", 1449, 3354, 0, "Patches: Belladonna", "Tools needed: Rake, Spade, Seed dibber, Watering can", "Check Farming skill guide for seed levels");
		addEntry("farming_patch", "Bush Farming Patch", "Skilling • Farming", 2938, 3223, 0, "Patches: Bush", "Tools needed: Rake, Spade, Seed dibber, Watering can", "Check Farming skill guide for seed levels");
		addEntry("farming_patch", "Bush Farming Patch", "Skilling • Farming", 2589, 3862, 0, "Patches: Bush", "Tools needed: Rake, Spade, Seed dibber, Watering can", "Check Farming skill guide for seed levels");
		addEntry("farming_patch", "Bush Farming Patch", "Skilling • Farming", 3182, 3356, 0, "Patches: Bush", "Tools needed: Rake, Spade, Seed dibber, Watering can", "Check Farming skill guide for seed levels");
		addEntry("farming_patch", "Bush Farming Patch", "Skilling • Farming", 2615, 3224, 0, "Patches: Bush", "Tools needed: Rake, Spade, Seed dibber, Watering can", "Check Farming skill guide for seed levels");
		addEntry("farming_patch", "Bush/Flower Farming Patch", "Skilling • Farming", 1259, 3729, 0, "Patches: Bush/Flower", "Tools needed: Rake, Spade, Seed dibber, Watering can", "Check Farming skill guide for seed levels");
		addEntry("farming_patch", "Cactus Farming Patch", "Skilling • Farming", 3313, 3201, 0, "Patches: Cactus", "Tools needed: Rake, Spade, Seed dibber, Watering can", "Check Farming skill guide for seed levels");
		addEntry("farming_patch", "Cactus Farming Patch", "Skilling • Farming", 1264, 3745, 0, "Patches: Cactus", "Tools needed: Rake, Spade, Seed dibber, Watering can", "Check Farming skill guide for seed levels");
		addEntry("farming_patch", "Calquat Farming Patch", "Skilling • Farming", 2793, 3099, 0, "Patches: Calquat", "Tools needed: Rake, Spade, Seed dibber, Watering can", "Check Farming skill guide for seed levels");
		addEntry("farming_patch", "Calquat Farming Patch", "Skilling • Farming", 1365, 3035, 0, "Patches: Calquat", "Tools needed: Rake, Spade, Seed dibber, Watering can", "Check Farming skill guide for seed levels");
		addEntry("farming_patch", "Calquat Farming Patch", "Skilling • Farming", 3125, 2403, 0, "Patches: Calquat", "Tools needed: Rake, Spade, Seed dibber, Watering can", "Check Farming skill guide for seed levels");
		addEntry("farming_patch", "Celastrus/Fruit Tree Farming Patch", "Skilling • Farming", 1242, 3755, 0, "Patches: Celastrus/Fruit Tree", "Tools needed: Rake, Spade, Seed dibber, Watering can", "Check Farming skill guide for seed levels");
		addEntry("farming_patch", "Coral Farming Patch", "Skilling • Farming", 3296, 8861, 0, "Patches: Coral", "Tools needed: Rake, Spade, Seed dibber, Watering can", "Check Farming skill guide for seed levels");
		addEntry("farming_patch", "Crystal Tree Farming Patch", "Skilling • Farming", 3292, 6120, 0, "Patches: Crystal Tree", "Tools needed: Rake, Spade, Seed dibber, Watering can", "Check Farming skill guide for seed levels");
		addEntry("farming_patch", "Flower Farming Patch", "Skilling • Farming", 1352, 3025, 0, "Patches: Flower", "Tools needed: Rake, Spade, Seed dibber, Watering can", "Check Farming skill guide for seed levels");
		addEntry("farming_patch", "Fruit Tree Farming Patch", "Skilling • Farming", 2487, 3181, 0, "Patches: Fruit Tree", "Tools needed: Rake, Spade, Seed dibber, Watering can", "Check Farming skill guide for seed levels");
		addEntry("farming_patch", "Fruit Tree Farming Patch", "Skilling • Farming", 2343, 3160, 0, "Patches: Fruit Tree", "Tools needed: Rake, Spade, Seed dibber, Watering can", "Check Farming skill guide for seed levels");
		addEntry("farming_patch", "Fruit Tree Farming Patch", "Skilling • Farming", 2472, 3445, 0, "Patches: Fruit Tree", "Tools needed: Rake, Spade, Seed dibber, Watering can", "Check Farming skill guide for seed levels");
		addEntry("farming_patch", "Fruit Tree Farming Patch", "Skilling • Farming", 2858, 3432, 0, "Patches: Fruit Tree", "Tools needed: Rake, Spade, Seed dibber, Watering can", "Check Farming skill guide for seed levels");
		addEntry("farming_patch", "Fruit Tree Farming Patch", "Skilling • Farming", 2765, 3211, 0, "Patches: Fruit Tree", "Tools needed: Rake, Spade, Seed dibber, Watering can", "Check Farming skill guide for seed levels");
		addEntry("farming_patch", "Fruit Tree Farming Patch", "Skilling • Farming", 1347, 3058, 0, "Patches: Fruit Tree", "Tools needed: Rake, Spade, Seed dibber, Watering can", "Check Farming skill guide for seed levels");
		addEntry("farming_patch", "Grapes Farming Patch", "Skilling • Farming", 1807, 3555, 0, "Patches: Grapes", "Tools needed: Rake, Spade, Seed dibber, Watering can", "Check Farming skill guide for seed levels");
		addEntry("farming_patch", "Hardwood Farming Patch", "Skilling • Farming", 3707, 3838, 0, "Patches: Hardwood", "Tools needed: Rake, Spade, Seed dibber, Watering can", "Check Farming skill guide for seed levels");
		addEntry("farming_patch", "Hardwood Farming Patch", "Skilling • Farming", 1683, 2971, 0, "Patches: Hardwood", "Tools needed: Rake, Spade, Seed dibber, Watering can", "Check Farming skill guide for seed levels");
		addEntry("farming_patch", "Hardwood Farming Patch", "Skilling • Farming", 2472, 2705, 0, "Patches: Hardwood", "Tools needed: Rake, Spade, Seed dibber, Watering can", "Check Farming skill guide for seed levels");
		addEntry("farming_patch", "Herb Farming Patch", "Skilling • Farming", 3789, 2840, 0, "Patches: Herb", "Tools needed: Rake, Spade, Seed dibber, Watering can", "Check Farming skill guide for seed levels");
		addEntry("farming_patch", "Herb Farming Patch", "Skilling • Farming", 2847, 3933, 0, "Patches: Herb", "Tools needed: Rake, Spade, Seed dibber, Watering can", "Check Farming skill guide for seed levels");
		addEntry("farming_patch", "Herb Farming Patch", "Skilling • Farming", 2828, 3696, 0, "Patches: Herb", "Tools needed: Rake, Spade, Seed dibber, Watering can", "Check Farming skill guide for seed levels");
		addEntry("farming_patch", "Hespori Farming Patch", "Skilling • Farming", 1182, 10068, 0, "Patches: Hespori", "Tools needed: Rake, Spade, Seed dibber, Watering can", "Check Farming skill guide for seed levels");
		addEntry("farming_patch", "Hops Farming Patch", "Skilling • Farming", 2572, 3102, 0, "Patches: Hops", "Tools needed: Rake, Spade, Seed dibber, Watering can", "Check Farming skill guide for seed levels");
		addEntry("farming_patch", "Hops Farming Patch", "Skilling • Farming", 2661, 3523, 0, "Patches: Hops", "Tools needed: Rake, Spade, Seed dibber, Watering can", "Check Farming skill guide for seed levels");
		addEntry("farming_patch", "Hops Farming Patch", "Skilling • Farming", 3224, 3313, 0, "Patches: Hops", "Tools needed: Rake, Spade, Seed dibber, Watering can", "Check Farming skill guide for seed levels");
		addEntry("farming_patch", "Hops Farming Patch", "Skilling • Farming", 2812, 3334, 0, "Patches: Hops", "Tools needed: Rake, Spade, Seed dibber, Watering can", "Check Farming skill guide for seed levels");
		addEntry("farming_patch", "Hops Farming Patch", "Skilling • Farming", 1367, 2938, 0, "Patches: Hops", "Tools needed: Rake, Spade, Seed dibber, Watering can", "Check Farming skill guide for seed levels");
		addEntry("farming_patch", "Mushroom Farming Patch", "Skilling • Farming", 3449, 3471, 0, "Patches: Mushroom", "Tools needed: Rake, Spade, Seed dibber, Watering can", "Check Farming skill guide for seed levels");
		addEntry("farming_patch", "Redwood Farming Patch", "Skilling • Farming", 1233, 3754, 0, "Patches: Redwood", "Tools needed: Rake, Spade, Seed dibber, Watering can", "Check Farming skill guide for seed levels");
		addEntry("farming_patch", "Seaweed Farming Patch", "Skilling • Farming", 3730, 10271, 0, "Patches: Seaweed", "Tools needed: Rake, Spade, Seed dibber, Watering can", "Check Farming skill guide for seed levels");
		addEntry("farming_patch", "Spirit Tree Farming Patch", "Skilling • Farming", 3056, 3259, 0, "Patches: Spirit Tree", "Tools needed: Rake, Spade, Seed dibber, Watering can", "Check Farming skill guide for seed levels");
		addEntry("farming_patch", "Spirit Tree Farming Patch", "Skilling • Farming", 1690, 3540, 0, "Patches: Spirit Tree", "Tools needed: Rake, Spade, Seed dibber, Watering can", "Check Farming skill guide for seed levels");
		addEntry("farming_patch", "Spirit Tree Farming Patch", "Skilling • Farming", 3614, 3856, 0, "Patches: Spirit Tree", "Tools needed: Rake, Spade, Seed dibber, Watering can", "Check Farming skill guide for seed levels");
		addEntry("farming_patch", "Spirit Tree Farming Patch", "Skilling • Farming", 2799, 3205, 0, "Patches: Spirit Tree", "Tools needed: Rake, Spade, Seed dibber, Watering can", "Check Farming skill guide for seed levels");
		addEntry("farming_patch", "Spirit Tree Farming Patch", "Skilling • Farming", 1254, 3753, 0, "Patches: Spirit Tree", "Tools needed: Rake, Spade, Seed dibber, Watering can", "Check Farming skill guide for seed levels");
		addEntry("farming_patch", "Tree Farming Patch", "Skilling • Farming", 3226, 3457, 0, "Patches: Tree", "Tools needed: Rake, Spade, Seed dibber, Watering can", "Check Farming skill guide for seed levels");
		addEntry("farming_patch", "Tree Farming Patch", "Skilling • Farming", 2933, 3436, 0, "Patches: Tree", "Tools needed: Rake, Spade, Seed dibber, Watering can", "Check Farming skill guide for seed levels");
		addEntry("farming_patch", "Tree Farming Patch", "Skilling • Farming", 3189, 3233, 0, "Patches: Tree", "Tools needed: Rake, Spade, Seed dibber, Watering can", "Check Farming skill guide for seed levels");
		addEntry("farming_patch", "Tree Farming Patch", "Skilling • Farming", 2434, 3418, 0, "Patches: Tree", "Tools needed: Rake, Spade, Seed dibber, Watering can", "Check Farming skill guide for seed levels");
		addEntry("farming_patch", "Tree Farming Patch", "Skilling • Farming", 3005, 3375, 0, "Patches: Tree", "Tools needed: Rake, Spade, Seed dibber, Watering can", "Check Farming skill guide for seed levels");
		addEntry("farming_patch", "Tree Farming Patch", "Skilling • Farming", 1234, 3736, 0, "Patches: Tree", "Tools needed: Rake, Spade, Seed dibber, Watering can", "Check Farming skill guide for seed levels");
		addEntry("farming_patch", "Tree Farming Patch", "Skilling • Farming", 1366, 3318, 0, "Patches: Tree", "Tools needed: Rake, Spade, Seed dibber, Watering can", "Check Farming skill guide for seed levels");
	}

	/** Fishing spots, by method and catch. */
	private static void addFishingSpots()
	{
		addEntry("fishing_spot", "Fishing Spot (Shrimp)", "Skilling • Fishing", 1372, 2985, 0, "Method / Fish: Shrimp", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Shrimp)", "Skilling • Fishing", 1313, 2963, 0, "Method / Fish: Shrimp", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Shrimp)", "Skilling • Fishing", 3274, 3140, 0, "Method / Fish: Shrimp", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Shrimp)", "Skilling • Fishing", 3266, 3148, 0, "Method / Fish: Shrimp", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Shark)", "Skilling • Fishing", 2698, 2703, 0, "Method / Fish: Shark", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Shrimp)", "Skilling • Fishing", 1774, 2993, 0, "Method / Fish: Shrimp", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Shark)", "Skilling • Fishing", 1769, 2966, 0, "Method / Fish: Shark", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Shark)", "Skilling • Fishing", 1573, 2954, 0, "Method / Fish: Shark", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Salmon)", "Skilling • Fishing", 1393, 3313, 0, "Method / Fish: Salmon", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Salmon)", "Skilling • Fishing", 1396, 3304, 0, "Method / Fish: Salmon", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Salmon)", "Skilling • Fishing", 1458, 3330, 0, "Method / Fish: Salmon", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Shrimp)", "Skilling • Fishing", 2497, 3548, 0, "Method / Fish: Shrimp", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Shrimp)", "Skilling • Fishing", 2509, 3562, 0, "Method / Fish: Shrimp", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Shrimp)", "Skilling • Fishing", 2514, 3575, 0, "Method / Fish: Shrimp", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Salmon)", "Skilling • Fishing", 3103, 3424, 0, "Method / Fish: Salmon", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Salmon)", "Skilling • Fishing", 3109, 3433, 0, "Method / Fish: Salmon", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Quest Rum Deal)", "Skilling • Fishing", 2112, 5074, 0, "Method / Fish: Quest Rum Deal", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Quest Rum Deal)", "Skilling • Fishing", 2161, 5061, 0, "Method / Fish: Quest Rum Deal", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Quest Rum Deal)", "Skilling • Fishing", 2172, 5074, 0, "Method / Fish: Quest Rum Deal", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Quest Tai Bwo Wannai Trio)", "Skilling • Fishing", 2767, 3165, 0, "Method / Fish: Quest Tai Bwo Wannai Trio", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Shark)", "Skilling • Fishing", 3472, 3192, 0, "Method / Fish: Shark", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Shark)", "Skilling • Fishing", 3486, 3182, 0, "Method / Fish: Shark", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Shark)", "Skilling • Fishing", 3497, 3174, 0, "Method / Fish: Shark", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Shark)", "Skilling • Fishing", 3513, 3177, 0, "Method / Fish: Shark", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Shark)", "Skilling • Fishing", 3528, 3164, 0, "Method / Fish: Shark", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Shark)", "Skilling • Fishing", 3537, 3177, 0, "Method / Fish: Shark", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Shark)", "Skilling • Fishing", 3545, 3179, 0, "Method / Fish: Shark", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Shark)", "Skilling • Fishing", 3553, 3177, 0, "Method / Fish: Shark", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Shark)", "Skilling • Fishing", 3559, 3173, 0, "Method / Fish: Shark", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Shark)", "Skilling • Fishing", 3564, 3174, 0, "Method / Fish: Shark", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Camdozaal Cave Eel)", "Skilling • Fishing", 2921, 5808, 0, "Method / Fish: Camdozaal Cave Eel", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Camdozaal Tetra)", "Skilling • Fishing", 2927, 5778, 0, "Method / Fish: Camdozaal Tetra", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Camdozaal Tetra)", "Skilling • Fishing", 2921, 5787, 0, "Method / Fish: Camdozaal Tetra", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Lava Eel)", "Skilling • Fishing", 2788, 8781, 0, "Method / Fish: Lava Eel", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Civitas Illa Fortis Park)", "Skilling • Fishing", 1737, 3101, 0, "Method / Fish: Civitas Illa Fortis Park", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Salmon)", "Skilling • Fishing", 1290, 3382, 0, "Method / Fish: Salmon", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Salmon)", "Skilling • Fishing", 1260, 3408, 0, "Method / Fish: Salmon", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Salmon)", "Skilling • Fishing", 1250, 3400, 0, "Method / Fish: Salmon", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Shrimp)", "Skilling • Fishing", 3084, 3228, 0, "Method / Fish: Shrimp", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Salmon)", "Skilling • Fishing", 2841, 3356, 0, "Method / Fish: Salmon", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Salmon)", "Skilling • Fishing", 2842, 3359, 0, "Method / Fish: Salmon", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Salmon)", "Skilling • Fishing", 2847, 3361, 0, "Method / Fish: Salmon", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Shrimp)", "Skilling • Fishing", 2875, 3331, 0, "Method / Fish: Shrimp", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Shrimp)", "Skilling • Fishing", 2878, 3334, 0, "Method / Fish: Shrimp", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Shrimp)", "Skilling • Fishing", 2878, 3339, 0, "Method / Fish: Shrimp", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Shrimp)", "Skilling • Fishing", 2875, 3342, 0, "Method / Fish: Shrimp", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Etceteria Lobster)", "Skilling • Fishing", 2577, 3854, 0, "Method / Fish: Etceteria Lobster", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Karambwanji)", "Skilling • Fishing", 2806, 3014, 0, "Method / Fish: Karambwanji", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Karambwan)", "Skilling • Fishing", 2898, 3119, 0, "Method / Fish: Karambwan", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Karambwan)", "Skilling • Fishing", 2911, 3119, 0, "Method / Fish: Karambwan", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Salmon)", "Skilling • Fishing", 1269, 3707, 0, "Method / Fish: Salmon", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Shark)", "Skilling • Fishing", 1209, 3687, 0, "Method / Fish: Shark", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Shark)", "Skilling • Fishing", 1221, 3714, 0, "Method / Fish: Shark", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Shark)", "Skilling • Fishing", 1199, 3736, 0, "Method / Fish: Shark", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Shark)", "Skilling • Fishing", 1208, 3749, 0, "Method / Fish: Shark", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Shrimp)", "Skilling • Fishing", 2511, 2838, 0, "Method / Fish: Shrimp", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Quest Fishing Contest)", "Skilling • Fishing", 2626, 3415, 0, "Method / Fish: Quest Fishing Contest", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Quest Fishing Contest)", "Skilling • Fishing", 2631, 3425, 0, "Method / Fish: Quest Fishing Contest", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Quest Fishing Contest)", "Skilling • Fishing", 2629, 3435, 0, "Method / Fish: Quest Fishing Contest", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Quest Fishing Contest)", "Skilling • Fishing", 2636, 3444, 0, "Method / Fish: Quest Fishing Contest", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Minnow)", "Skilling • Fishing", 2609, 3444, 0, "Method / Fish: Minnow", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Minnow)", "Skilling • Fishing", 2617, 3444, 0, "Method / Fish: Minnow", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Shrimp)", "Skilling • Fishing", 2788, 3273, 0, "Method / Fish: Shrimp", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Shrimp)", "Skilling • Fishing", 2794, 3279, 0, "Method / Fish: Shrimp", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Shrimp)", "Skilling • Fishing", 2793, 3283, 0, "Method / Fish: Shrimp", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Salmon)", "Skilling • Fishing", 1715, 3612, 0, "Method / Fish: Salmon", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Salmon)", "Skilling • Fishing", 1584, 3566, 0, "Method / Fish: Salmon", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Salmon)", "Skilling • Fishing", 3239, 5997, 0, "Method / Fish: Salmon", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Shark)", "Skilling • Fishing", 3185, 6027, 0, "Method / Fish: Shark", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Shark)", "Skilling • Fishing", 2161, 3275, 0, "Method / Fish: Shark", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Salmon)", "Skilling • Fishing", 2215, 3245, 0, "Method / Fish: Salmon", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Salmon)", "Skilling • Fishing", 3293, 6005, 0, "Method / Fish: Salmon", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Salmon)", "Skilling • Fishing", 2269, 3253, 0, "Method / Fish: Salmon", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Shark)", "Skilling • Fishing", 2281, 2841, 0, "Method / Fish: Shark", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Lobster)", "Skilling • Fishing", 2280, 2975, 0, "Method / Fish: Lobster", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Shrimp)", "Skilling • Fishing", 2162, 2782, 0, "Method / Fish: Shrimp", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Shrimp)", "Skilling • Fishing", 1329, 3010, 0, "Method / Fish: Shrimp", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Lobster)", "Skilling • Fishing", 3139, 2800, 0, "Method / Fish: Lobster", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Salmon)", "Skilling • Fishing", 1723, 3685, 0, "Method / Fish: Salmon", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Shrimp)", "Skilling • Fishing", 1534, 3414, 0, "Method / Fish: Shrimp", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Salmon)", "Skilling • Fishing", 3238, 3241, 0, "Method / Fish: Salmon", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Salmon)", "Skilling • Fishing", 3237, 3253, 0, "Method / Fish: Salmon", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Cave Eel)", "Skilling • Fishing", 3244, 9570, 0, "Method / Fish: Cave Eel", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Cave Eel)", "Skilling • Fishing", 3153, 9544, 0, "Method / Fish: Cave Eel", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Shrimp)", "Skilling • Fishing", 3244, 3153, 0, "Method / Fish: Shrimp", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Shark)", "Skilling • Fishing", 2774, 2740, 0, "Method / Fish: Shark", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Lobster)", "Skilling • Fishing", 1374, 2841, 0, "Method / Fish: Lobster", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Common Tench)", "Skilling • Fishing", 1370, 3632, 0, "Method / Fish: Common Tench", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Slimy Eel)", "Skilling • Fishing", 3439, 3273, 0, "Method / Fish: Slimy Eel", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Slimy Eel)", "Skilling • Fishing", 3480, 3433, 0, "Method / Fish: Slimy Eel", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Slimy Eel)", "Skilling • Fishing", 3485, 3448, 0, "Method / Fish: Slimy Eel", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Slimy Eel)", "Skilling • Fishing", 3425, 3409, 0, "Method / Fish: Slimy Eel", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Slimy Eel)", "Skilling • Fishing", 3432, 3415, 0, "Method / Fish: Slimy Eel", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Infernal Eel)", "Skilling • Fishing", 2443, 5104, 0, "Method / Fish: Infernal Eel", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Infernal Eel)", "Skilling • Fishing", 2476, 5077, 0, "Method / Fish: Infernal Eel", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Infernal Eel)", "Skilling • Fishing", 2537, 5086, 0, "Method / Fish: Infernal Eel", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Barb Fish)", "Skilling • Fishing", 1271, 3546, 0, "Method / Fish: Barb Fish", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Barb Fish)", "Skilling • Fishing", 1265, 3541, 0, "Method / Fish: Barb Fish", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Barb Fish)", "Skilling • Fishing", 1253, 3542, 0, "Method / Fish: Barb Fish", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Shrimp)", "Skilling • Fishing", 2995, 3158, 0, "Method / Fish: Shrimp", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Shrimp)", "Skilling • Fishing", 2985, 3176, 0, "Method / Fish: Shrimp", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Lobster)", "Skilling • Fishing", 2456, 2893, 0, "Method / Fish: Lobster", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Salmon)", "Skilling • Fishing", 2466, 3151, 0, "Method / Fish: Salmon", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Barb Fish)", "Skilling • Fishing", 2500, 3509, 0, "Method / Fish: Barb Fish", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Barb Fish)", "Skilling • Fishing", 2504, 3495, 0, "Method / Fish: Barb Fish", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Barb Fish)", "Skilling • Fishing", 2505, 3515, 0, "Method / Fish: Barb Fish", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Barb Fish)", "Skilling • Fishing", 2520, 3518, 0, "Method / Fish: Barb Fish", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Monkfish)", "Skilling • Fishing", 2308, 3700, 0, "Method / Fish: Monkfish", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Anglerfish)", "Skilling • Fishing", 1831, 3773, 0, "Method / Fish: Anglerfish", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Lobster)", "Skilling • Fishing", 2641, 3696, 0, "Method / Fish: Lobster", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Shark)", "Skilling • Fishing", 2649, 3708, 0, "Method / Fish: Shark", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Shrimp)", "Skilling • Fishing", 2632, 3694, 0, "Method / Fish: Shrimp", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Salmon)", "Skilling • Fishing", 2560, 3374, 0, "Method / Fish: Salmon", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Salmon)", "Skilling • Fishing", 2565, 3370, 0, "Method / Fish: Salmon", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Salmon)", "Skilling • Fishing", 2526, 3412, 0, "Method / Fish: Salmon", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Salmon)", "Skilling • Fishing", 2536, 3405, 0, "Method / Fish: Salmon", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Salmon)", "Skilling • Fishing", 2507, 3420, 0, "Method / Fish: Salmon", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Salmon)", "Skilling • Fishing", 1545, 3186, 0, "Method / Fish: Salmon", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Shark)", "Skilling • Fishing", 1675, 3490, 0, "Method / Fish: Shark", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Shrimp)", "Skilling • Fishing", 1676, 3469, 0, "Method / Fish: Shrimp", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Lobster)", "Skilling • Fishing", 1668, 3479, 0, "Method / Fish: Lobster", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Salmon)", "Skilling • Fishing", 2725, 3524, 0, "Method / Fish: Salmon", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Salmon)", "Skilling • Fishing", 2714, 3531, 0, "Method / Fish: Salmon", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Shrimp)", "Skilling • Fishing", 1565, 3326, 0, "Method / Fish: Shrimp", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Shark)", "Skilling • Fishing", 1586, 3299, 0, "Method / Fish: Shark", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Salmon)", "Skilling • Fishing", 2854, 2977, 0, "Method / Fish: Salmon", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Salmon)", "Skilling • Fishing", 2858, 2973, 0, "Method / Fish: Salmon", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Shark)", "Skilling • Fishing", 1218, 3146, 0, "Method / Fish: Shark", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Salmon)", "Skilling • Fishing", 1265, 3132, 0, "Method / Fish: Salmon", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Lava Eel)", "Skilling • Fishing", 2893, 9764, 0, "Method / Fish: Lava Eel", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Lava Eel)", "Skilling • Fishing", 2889, 9766, 0, "Method / Fish: Lava Eel", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Lava Eel)", "Skilling • Fishing", 2883, 9765, 0, "Method / Fish: Lava Eel", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Harpoonfish)", "Skilling • Fishing", 3047, 3002, 0, "Method / Fish: Harpoonfish", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Harpoonfish)", "Skilling • Fishing", 3050, 2995, 0, "Method / Fish: Harpoonfish", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Harpoonfish)", "Skilling • Fishing", 3036, 2996, 0, "Method / Fish: Harpoonfish", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Harpoonfish)", "Skilling • Fishing", 3035, 2962, 0, "Method / Fish: Harpoonfish", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Harpoonfish)", "Skilling • Fishing", 3047, 2957, 0, "Method / Fish: Harpoonfish", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Harpoonfish)", "Skilling • Fishing", 3045, 2954, 0, "Method / Fish: Harpoonfish", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Salmon)", "Skilling • Fishing", 1623, 3235, 0, "Method / Fish: Salmon", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Salmon)", "Skilling • Fishing", 2389, 3422, 0, "Method / Fish: Salmon", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Salmon)", "Skilling • Fishing", 2382, 3415, 0, "Method / Fish: Salmon", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Tutorial Shrimp)", "Skilling • Fishing", 3100, 3091, 0, "Method / Fish: Tutorial Shrimp", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Salmon)", "Skilling • Fishing", 1646, 3558, 0, "Method / Fish: Salmon", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Shrimp)", "Skilling • Fishing", 3049, 3704, 0, "Method / Fish: Shrimp", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Dark Crab)", "Skilling • Fishing", 3362, 3802, 0, "Method / Fish: Dark Crab", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Dark Crab)", "Skilling • Fishing", 3347, 3813, 0, "Method / Fish: Dark Crab", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Lava Eel)", "Skilling • Fishing", 3071, 3840, 0, "Method / Fish: Lava Eel", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Dark Crab)", "Skilling • Fishing", 3186, 3925, 0, "Method / Fish: Dark Crab", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Check Fishing skill guide for exact bait & level reqs");
		addEntry("fishing_spot", "Fishing Spot (Lobster & Swordfish)", "Skilling • Fishing", 2575, 2219, 0, "Method / Fish: Lobster & Swordfish", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Coastal saltwater fishing");
		addEntry("fishing_spot", "Fishing Spot (Shark & Lobster)", "Skilling • Fishing", 3003, 2276, 0, "Method / Fish: Shark & Lobster", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Harpoon / Lobster pot");
		addEntry("fishing_spot", "Fishing Spot (Desert Sole & Catfish)", "Skilling • Fishing", 3191, 2376, 0, "Method / Fish: Desert Sole & Catfish", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Small net / Fishing rod");
		addEntry("fishing_spot", "Fishing Spot (Desert Sole & Catfish)", "Skilling • Fishing", 3169, 2386, 0, "Method / Fish: Desert Sole & Catfish", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Small net / Fishing rod");
		addEntry("fishing_spot", "Fishing Spot (Desert Sole & Catfish)", "Skilling • Fishing", 3269, 2411, 0, "Method / Fish: Desert Sole & Catfish", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Small net / Fishing rod");
		addEntry("fishing_spot", "Fishing Spot (Desert Sole & Catfish)", "Skilling • Fishing", 3211, 2425, 0, "Method / Fish: Desert Sole & Catfish", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Small net / Fishing rod");
		addEntry("fishing_spot", "Fishing Spot (Desert Sole & Catfish)", "Skilling • Fishing", 3265, 2425, 0, "Method / Fish: Desert Sole & Catfish", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Small net / Fishing rod");
		addEntry("fishing_spot", "Fishing Spot (Desert Sole & Catfish)", "Skilling • Fishing", 3195, 2437, 0, "Method / Fish: Desert Sole & Catfish", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Small net / Fishing rod");
		addEntry("fishing_spot", "Fishing Spot (Desert Sole & Catfish)", "Skilling • Fishing", 3260, 2480, 0, "Method / Fish: Desert Sole & Catfish", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Small net / Fishing rod");
		addEntry("fishing_spot", "Fishing Spot (Desert Sole & Catfish)", "Skilling • Fishing", 3258, 2494, 0, "Method / Fish: Desert Sole & Catfish", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Small net / Fishing rod");
		addEntry("fishing_spot", "Fishing Spot (Desert Sole & Catfish)", "Skilling • Fishing", 3156, 2537, 0, "Method / Fish: Desert Sole & Catfish", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Small net / Fishing rod");
		addEntry("fishing_spot", "Fishing Spot (Desert Sole & Catfish)", "Skilling • Fishing", 3169, 2541, 0, "Method / Fish: Desert Sole & Catfish", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Small net / Fishing rod");
		addEntry("fishing_spot", "Fishing Spot (Sea Turtle & Manta Ray)", "Skilling • Fishing", 2074, 2604, 0, "Method / Fish: Sea Turtle & Manta Ray", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Harpoon fishing");
		addEntry("fishing_spot", "Fishing Spot (Shrimp & Anchovies)", "Skilling • Fishing", 3052, 2633, 0, "Method / Fish: Shrimp & Anchovies", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Small fishing net");
		addEntry("fishing_spot", "Fishing Spot (Lobster & Swordfish)", "Skilling • Fishing", 2476, 2696, 0, "Method / Fish: Lobster & Swordfish", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Cage / Harpoon");
		addEntry("fishing_spot", "Fishing Spot (Lobster & Swordfish)", "Skilling • Fishing", 2466, 2702, 0, "Method / Fish: Lobster & Swordfish", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Cage / Harpoon");
		addEntry("fishing_spot", "Fishing Spot (Lobster & Swordfish)", "Skilling • Fishing", 2465, 2712, 0, "Method / Fish: Lobster & Swordfish", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Cage / Harpoon");
		addEntry("fishing_spot", "Fishing Spot (Salmon & Trout)", "Skilling • Fishing", 1183, 2720, 0, "Method / Fish: Salmon & Trout", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Fly fishing rod + Feathers");
		addEntry("fishing_spot", "Fishing Spot (Salmon & Trout)", "Skilling • Fishing", 1173, 2722, 0, "Method / Fish: Salmon & Trout", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Fly fishing rod + Feathers");
		addEntry("fishing_spot", "Fishing Spot (Anglerfish & Monkfish)", "Skilling • Fishing", 1587, 2777, 0, "Method / Fish: Anglerfish & Monkfish", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Sandworms / Small net");
		addEntry("fishing_spot", "Fishing Spot (Anglerfish & Monkfish)", "Skilling • Fishing", 1594, 2783, 0, "Method / Fish: Anglerfish & Monkfish", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Sandworms / Small net");
		addEntry("fishing_spot", "Fishing Spot (Salmon & Trout)", "Skilling • Fishing", 1201, 2792, 0, "Method / Fish: Salmon & Trout", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Fly fishing rod + Feathers");
		addEntry("fishing_spot", "Fishing Spot (Anglerfish & Monkfish)", "Skilling • Fishing", 1552, 2807, 0, "Method / Fish: Anglerfish & Monkfish", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Sandworms / Small net");
		addEntry("fishing_spot", "Fishing Spot (Anglerfish & Monkfish)", "Skilling • Fishing", 1547, 2813, 0, "Method / Fish: Anglerfish & Monkfish", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Sandworms / Small net");
		addEntry("fishing_spot", "Fishing Spot (Lobster & Swordfish)", "Skilling • Fishing", 3046, 2826, 0, "Method / Fish: Lobster & Swordfish", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Cage / Harpoon");
		addEntry("fishing_spot", "Fishing Spot (Lobster & Swordfish)", "Skilling • Fishing", 3048, 2829, 0, "Method / Fish: Lobster & Swordfish", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Cage / Harpoon");
		addEntry("fishing_spot", "Fishing Spot (Lobster & Swordfish)", "Skilling • Fishing", 3036, 2834, 0, "Method / Fish: Lobster & Swordfish", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Cage / Harpoon");
		addEntry("fishing_spot", "Fishing Spot (Lobster & Swordfish)", "Skilling • Fishing", 3051, 2867, 0, "Method / Fish: Lobster & Swordfish", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Cage / Harpoon");
		addEntry("fishing_spot", "Fishing Spot (Lobster & Swordfish)", "Skilling • Fishing", 3037, 2868, 0, "Method / Fish: Lobster & Swordfish", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Cage / Harpoon");
		addEntry("fishing_spot", "Fishing Spot (Lobster & Swordfish)", "Skilling • Fishing", 3048, 2874, 0, "Method / Fish: Lobster & Swordfish", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Cage / Harpoon");
		addEntry("fishing_spot", "Fishing Spot (Salmon & Trout)", "Skilling • Fishing", 1586, 2951, 0, "Method / Fish: Salmon & Trout", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Fly fishing rod + Feathers");
		addEntry("fishing_spot", "Fishing Spot (Lobster & Swordfish)", "Skilling • Fishing", 1899, 2972, 0, "Method / Fish: Lobster & Swordfish", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Cage / Harpoon");
		addEntry("fishing_spot", "Fishing Spot (Lobster & Swordfish)", "Skilling • Fishing", 2196, 3067, 0, "Method / Fish: Lobster & Swordfish", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Cage / Harpoon");
		addEntry("fishing_spot", "Fishing Spot (Lobster & Swordfish)", "Skilling • Fishing", 2184, 3068, 0, "Method / Fish: Lobster & Swordfish", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Cage / Harpoon");
		addEntry("fishing_spot", "Fishing Spot (Karambwan)", "Skilling • Fishing", 2892, 3134, 0, "Method / Fish: Karambwan", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Karambwan vessel + Karambwanji");
		addEntry("fishing_spot", "Fishing Spot (Karambwan)", "Skilling • Fishing", 2926, 3179, 0, "Method / Fish: Karambwan", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Karambwan vessel + Karambwanji");
		addEntry("fishing_spot", "Fishing Spot (Shark & Monkfish)", "Skilling • Fishing", 2076, 3189, 0, "Method / Fish: Shark & Monkfish", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Harpoon / Net");
		addEntry("fishing_spot", "Fishing Spot (Shrimp & Anchovies)", "Skilling • Fishing", 3099, 3194, 0, "Method / Fish: Shrimp & Anchovies", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Small fishing net");
		addEntry("fishing_spot", "Fishing Spot (Shark & Monkfish)", "Skilling • Fishing", 2087, 3196, 0, "Method / Fish: Shark & Monkfish", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Harpoon / Net");
		addEntry("fishing_spot", "Fishing Spot (Karambwan)", "Skilling • Fishing", 2887, 3196, 0, "Method / Fish: Karambwan", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Karambwan vessel + Karambwanji");
		addEntry("fishing_spot", "Fishing Spot (Salmon & Trout)", "Skilling • Fishing", 2163, 3350, 0, "Method / Fish: Salmon & Trout", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Fly fishing rod + Feathers");
		addEntry("fishing_spot", "Fishing Spot (Salmon & Trout)", "Skilling • Fishing", 2164, 3371, 0, "Method / Fish: Salmon & Trout", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Fly fishing rod + Feathers");
		addEntry("fishing_spot", "Fishing Spot (Shark & Lobster)", "Skilling • Fishing", 2612, 3413, 0, "Method / Fish: Shark & Lobster", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Harpoon / Lobster pot");
		addEntry("fishing_spot", "Fishing Spot (Lobster & Swordfish)", "Skilling • Fishing", 2828, 3414, 0, "Method / Fish: Lobster & Swordfish", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Cage / Harpoon");
		addEntry("fishing_spot", "Fishing Spot (Shark & Lobster)", "Skilling • Fishing", 2606, 3417, 0, "Method / Fish: Shark & Lobster", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Harpoon / Lobster pot");
		addEntry("fishing_spot", "Fishing Spot (Shark & Lobster)", "Skilling • Fishing", 2605, 3423, 0, "Method / Fish: Shark & Lobster", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Harpoon / Lobster pot");
		addEntry("fishing_spot", "Fishing Spot (Lobster & Swordfish)", "Skilling • Fishing", 2854, 3423, 0, "Method / Fish: Lobster & Swordfish", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Cage / Harpoon");
		addEntry("fishing_spot", "Fishing Spot (Lobster & Swordfish)", "Skilling • Fishing", 2860, 3426, 0, "Method / Fish: Lobster & Swordfish", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Cage / Harpoon");
		addEntry("fishing_spot", "Fishing Spot (Lobster & Swordfish)", "Skilling • Fishing", 2235, 3428, 0, "Method / Fish: Lobster & Swordfish", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Cage / Harpoon");
		addEntry("fishing_spot", "Fishing Spot (Lobster & Swordfish)", "Skilling • Fishing", 2845, 3429, 0, "Method / Fish: Lobster & Swordfish", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Cage / Harpoon");
		addEntry("fishing_spot", "Fishing Spot (Lobster & Swordfish)", "Skilling • Fishing", 2227, 3430, 0, "Method / Fish: Lobster & Swordfish", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Cage / Harpoon");
		addEntry("fishing_spot", "Fishing Spot (Lobster & Swordfish)", "Skilling • Fishing", 2837, 3431, 0, "Method / Fish: Lobster & Swordfish", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Cage / Harpoon");
		addEntry("fishing_spot", "Fishing Spot (Salmon & Trout)", "Skilling • Fishing", 1485, 3432, 0, "Method / Fish: Salmon & Trout", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Fly fishing rod + Feathers");
		addEntry("fishing_spot", "Fishing Spot (Anglerfish)", "Skilling • Fishing", 1889, 3436, 0, "Method / Fish: Anglerfish", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Fishing rod + Sandworms");
		addEntry("fishing_spot", "Fishing Spot (Lobster & Swordfish)", "Skilling • Fishing", 2235, 3445, 0, "Method / Fish: Lobster & Swordfish", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Cage / Harpoon");
		addEntry("fishing_spot", "Fishing Spot (Cod & Bass)", "Skilling • Fishing", 2137, 3537, 0, "Method / Fish: Cod & Bass", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Big fishing net / Harpoon");
		addEntry("fishing_spot", "Fishing Spot (Cod & Bass)", "Skilling • Fishing", 2159, 3541, 0, "Method / Fish: Cod & Bass", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Big fishing net / Harpoon");
		addEntry("fishing_spot", "Fishing Spot (Cod & Bass)", "Skilling • Fishing", 2131, 3544, 0, "Method / Fish: Cod & Bass", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Big fishing net / Harpoon");
		addEntry("fishing_spot", "Fishing Spot (Cod & Bass)", "Skilling • Fishing", 2156, 3551, 0, "Method / Fish: Cod & Bass", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Big fishing net / Harpoon");
		addEntry("fishing_spot", "Fishing Spot (Anglerfish & Sandworms)", "Skilling • Fishing", 1839, 3595, 0, "Method / Fish: Anglerfish & Sandworms", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Fishing rod + Sandworms");
		addEntry("fishing_spot", "Fishing Spot (Anglerfish & Sandworms)", "Skilling • Fishing", 1818, 3603, 0, "Method / Fish: Anglerfish & Sandworms", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Fishing rod + Sandworms");
		addEntry("fishing_spot", "Fishing Spot (Anglerfish & Sandworms)", "Skilling • Fishing", 1828, 3605, 0, "Method / Fish: Anglerfish & Sandworms", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Fishing rod + Sandworms");
		addEntry("fishing_spot", "Fishing Spot (Anglerfish & Sandworms)", "Skilling • Fishing", 1829, 3614, 0, "Method / Fish: Anglerfish & Sandworms", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Fishing rod + Sandworms");
		addEntry("fishing_spot", "Fishing Spot (Anglerfish & Sandworms)", "Skilling • Fishing", 1841, 3619, 0, "Method / Fish: Anglerfish & Sandworms", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Fishing rod + Sandworms");
		addEntry("fishing_spot", "Fishing Spot (Shark & Sea Turtle)", "Skilling • Fishing", 2077, 3674, 0, "Method / Fish: Shark & Sea Turtle", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Harpoon fishing");
		addEntry("fishing_spot", "Fishing Spot (Monkfish)", "Skilling • Fishing", 2329, 3701, 0, "Method / Fish: Monkfish", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Small fishing net");
		addEntry("fishing_spot", "Fishing Spot (Monkfish)", "Skilling • Fishing", 2348, 3702, 0, "Method / Fish: Monkfish", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Small fishing net");
		addEntry("fishing_spot", "Fishing Spot (Monkfish)", "Skilling • Fishing", 2344, 3719, 0, "Method / Fish: Monkfish", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Small fishing net");
		addEntry("fishing_spot", "Fishing Spot (Shrimp & Anchovies)", "Skilling • Fishing", 1887, 3764, 0, "Method / Fish: Shrimp & Anchovies", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Small fishing net");
		addEntry("fishing_spot", "Fishing Spot (Shark & Lobster)", "Skilling • Fishing", 2401, 3780, 0, "Method / Fish: Shark & Lobster", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Harpoon / Cage");
		addEntry("fishing_spot", "Fishing Spot (Shark & Lobster)", "Skilling • Fishing", 2417, 3783, 0, "Method / Fish: Shark & Lobster", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Harpoon / Cage");
		addEntry("fishing_spot", "Fishing Spot (Shark & Lobster)", "Skilling • Fishing", 2420, 3789, 0, "Method / Fish: Shark & Lobster", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Harpoon / Cage");
		addEntry("fishing_spot", "Fishing Spot (Anglerfish)", "Skilling • Fishing", 1763, 3796, 0, "Method / Fish: Anglerfish", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Fishing rod + Sandworms");
		addEntry("fishing_spot", "Fishing Spot (Anglerfish)", "Skilling • Fishing", 1746, 3802, 0, "Method / Fish: Anglerfish", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Fishing rod + Sandworms");
		addEntry("fishing_spot", "Fishing Spot (Volcanic Ash & Seaweed)", "Skilling • Fishing", 3042, 4510, 0, "Method / Fish: Seaweed & Drift Net", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Drift net fishing");
		addEntry("fishing_spot", "Fishing Spot (Sacred Eel)", "Skilling • Fishing", 2680, 4702, 0, "Method / Fish: Sacred Eel", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Fishing rod + Bait");
		addEntry("fishing_spot", "Fishing Spot (Sacred Eel)", "Skilling • Fishing", 2674, 4708, 0, "Method / Fish: Sacred Eel", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Fishing rod + Bait");
		addEntry("fishing_spot", "Fishing Spot (Sacred Eel)", "Skilling • Fishing", 2669, 4710, 0, "Method / Fish: Sacred Eel", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Fishing rod + Bait");
		addEntry("fishing_spot", "Fishing Spot (Infernal Eel)", "Skilling • Fishing", 2694, 5225, 0, "Method / Fish: Infernal Eel", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Oily rod + Bait");
		addEntry("fishing_spot", "Fishing Spot (Infernal Eel)", "Skilling • Fishing", 2747, 5229, 0, "Method / Fish: Infernal Eel", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Oily rod + Bait");
		addEntry("fishing_spot", "Fishing Spot (Cave Eel)", "Skilling • Fishing", 3276, 5454, 0, "Method / Fish: Cave Eel", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Fishing rod + Bait");
		addEntry("fishing_spot", "Fishing Spot (Cave Eel)", "Skilling • Fishing", 3309, 5456, 0, "Method / Fish: Cave Eel", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Fishing rod + Bait");
		addEntry("fishing_spot", "Fishing Spot (Cave Eel)", "Skilling • Fishing", 3341, 5456, 0, "Method / Fish: Cave Eel", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Fishing rod + Bait");
		addEntry("fishing_spot", "Fishing Spot (Crystal Eel)", "Skilling • Fishing", 1971, 5854, 0, "Method / Fish: Crystal Eel", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Crystal rod + Bait");
		addEntry("fishing_spot", "Fishing Spot (Camdozaal Tetra)", "Skilling • Fishing", 2090, 5913, 0, "Method / Fish: Camdozaal Tetra", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Small fishing net");
		addEntry("fishing_spot", "Fishing Spot (Camdozaal Tetra)", "Skilling • Fishing", 2089, 6047, 0, "Method / Fish: Camdozaal Tetra", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Small fishing net");
		addEntry("fishing_spot", "Fishing Spot (Cave Eel)", "Skilling • Fishing", 1693, 6099, 0, "Method / Fish: Cave Eel", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Fishing rod + Bait");
		addEntry("fishing_spot", "Fishing Spot (Dark Crab & Lava Eel)", "Skilling • Fishing", 3187, 6102, 0, "Method / Fish: Dark Crab & Lava Eel", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Lobster pot + Dark fishing bait");
		addEntry("fishing_spot", "Fishing Spot (Dark Crab & Lava Eel)", "Skilling • Fishing", 3188, 6123, 0, "Method / Fish: Dark Crab & Lava Eel", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Lobster pot + Dark fishing bait");
		addEntry("fishing_spot", "Fishing Spot (Dark Crab & Lava Eel)", "Skilling • Fishing", 3259, 6180, 0, "Method / Fish: Dark Crab & Lava Eel", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Lobster pot + Dark fishing bait");
		addEntry("fishing_spot", "Fishing Spot (Dark Crab & Lava Eel)", "Skilling • Fishing", 3251, 6182, 0, "Method / Fish: Dark Crab & Lava Eel", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Lobster pot + Dark fishing bait");
		addEntry("fishing_spot", "Fishing Spot (Shrimp & Anchovies)", "Skilling • Fishing", 2718, 6197, 0, "Method / Fish: Shrimp & Anchovies", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Small fishing net");
		addEntry("fishing_spot", "Fishing Spot (Dark Crab & Lava Eel)", "Skilling • Fishing", 3259, 6197, 0, "Method / Fish: Dark Crab & Lava Eel", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Lobster pot + Dark fishing bait");
		addEntry("fishing_spot", "Fishing Spot (Cam Torum Tetra)", "Skilling • Fishing", 2710, 7819, 0, "Method / Fish: Cam Torum Tetra", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Small fishing net");
		addEntry("fishing_spot", "Fishing Spot (Cam Torum Tetra)", "Skilling • Fishing", 2721, 7825, 0, "Method / Fish: Cam Torum Tetra", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Small fishing net");
		addEntry("fishing_spot", "Fishing Spot (Lava Eel)", "Skilling • Fishing", 2611, 8598, 0, "Method / Fish: Lava Eel", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Oily fishing rod + Bait");
		addEntry("fishing_spot", "Fishing Spot (Lava Eel)", "Skilling • Fishing", 2725, 8845, 0, "Method / Fish: Lava Eel", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Oily fishing rod + Bait");
		addEntry("fishing_spot", "Fishing Spot (Cave Eel)", "Skilling • Fishing", 3355, 9566, 0, "Method / Fish: Cave Eel", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Fishing rod + Bait");
		addEntry("fishing_spot", "Fishing Spot (Cave Eel)", "Skilling • Fishing", 3371, 9577, 0, "Method / Fish: Cave Eel", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Fishing rod + Bait");
		addEntry("fishing_spot", "Fishing Spot (Cave Eel)", "Skilling • Fishing", 3362, 9588, 0, "Method / Fish: Cave Eel", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Fishing rod + Bait");
		addEntry("fishing_spot", "Fishing Spot (Lava Eel)", "Skilling • Fishing", 1501, 9698, 0, "Method / Fish: Lava Eel", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Oily fishing rod + Bait");
		addEntry("fishing_spot", "Fishing Spot (Anglerfish)", "Skilling • Fishing", 1317, 9785, 0, "Method / Fish: Anglerfish", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Fishing rod + Sandworms");
		addEntry("fishing_spot", "Fishing Spot (Lava Eel)", "Skilling • Fishing", 1521, 9689, 1, "Method / Fish: Lava Eel", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Oily fishing rod + Bait");
		addEntry("fishing_spot", "Fishing Spot (Cave Eel)", "Skilling • Fishing", 3741, 10298, 1, "Method / Fish: Cave Eel", "Tools: Net, Rod + Bait, Pot, or Harpoon", "Fishing rod + Bait");
	}

	/** Mining sites, by ore. */
	private static void addMiningSites()
	{
		addEntry("mining_site", "Agility Pyramid Mining Site", "Skilling • Mining", 3322, 2875, 0, "Ores available: Gold, Iron, Silver, Coal, Gold", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Aldarin Mining Site", "Skilling • Mining", 1430, 2882, 0, "Ores available: Iron", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Al Kharid Mine North Mining Site", "Skilling • Mining", 3298, 3312, 0, "Ores available: Copper, Tin, Iron, Silver, Coal, Mithril, Adamantite", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Al Kharid Mine South Mining Site", "Skilling • Mining", 3298, 3282, 0, "Ores available: Iron, Gold", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Ancient Cavern North Mining Site", "Skilling • Mining", 1847, 5414, 0, "Ores available: Mithril", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Ancient Cavern South Mining Site", "Skilling • Mining", 1826, 5392, 0, "Ores available: Mithril", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Ancient Cavern Middle Mining Site", "Skilling • Mining", 1840, 5397, 0, "Ores available: Mithril", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Arandar Mining Site", "Skilling • Mining", 2322, 3269, 0, "Ores available: Limestone", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Arandar Prifddinas Map Mining Site", "Skilling • Mining", 3346, 6021, 0, "Ores available: Limestone", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Arceuus North Mining Site", "Skilling • Mining", 1763, 3860, 0, "Ores available: Dense Essence", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Arceuus South Mining Site", "Skilling • Mining", 1763, 3844, 0, "Ores available: Dense Essence", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Ardougne Sewers Mining Site", "Skilling • Mining", 2670, 9680, 0, "Ores available: Iron, Coal", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Ardougne South East Mining Site", "Skilling • Mining", 2599, 3232, 0, "Ores available: Iron, Coal", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Asgarnia Ice Dungeon East Mining Site", "Skilling • Mining", 3063, 9582, 0, "Ores available: Blurite", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Asgarnia Ice Dungeon West Mining Site", "Skilling • Mining", 3049, 9568, 0, "Ores available: Blurite", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Avium Savannah Mining Site", "Skilling • Mining", 1618, 2991, 0, "Ores available: Iron", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Bandit Camp Mine Mining Site", "Skilling • Mining", 3086, 3763, 0, "Ores available: Iron, Coal, Mithril, Adamantite", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Bandit Camp Quarry Mining Site", "Skilling • Mining", 3171, 2912, 0, "Ores available: Clay, Coal, Sandstone, Granite", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Barbarian Village Mining Site", "Skilling • Mining", 3078, 3421, 0, "Ores available: Tin, Coal", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Battlefield Mining Site", "Skilling • Mining", 2471, 3255, 0, "Ores available: Copper, Tin", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Blast Mine East Mining Site", "Skilling • Mining", 1502, 3869, 0, "Ores available: Hard Rock", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Blast Mine North Mining Site", "Skilling • Mining", 1485, 3882, 0, "Ores available: Hard Rock", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Blast Mine West Mining Site", "Skilling • Mining", 1471, 3865, 0, "Ores available: Hard Rock", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Brimhaven North Mining Site", "Skilling • Mining", 2732, 3225, 0, "Ores available: Gold", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Brimhaven South  Mining Site", "Skilling • Mining", 2743, 3150, 0, "Ores available: Gold", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Cam Torum Mining Site", "Skilling • Mining", 1510, 9540, 0, "Ores available: Iron, Coal, Calcified Rocks, Mithril, Adamantite", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Camdozaal Mines East Mining Site", "Skilling • Mining", 2934, 5811, 0, "Ores available: Barronite, Clay, Tin, Copper", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Camdozaal Mines West Mining Site", "Skilling • Mining", 2914, 5811, 0, "Ores available: Barronite, Copper, Clay, Tin", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Central Fremenik Isles Mining Site", "Skilling • Mining", 2374, 3850, 0, "Ores available: Coal, Runite", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Charred Dungeon Mining Site", "Skilling • Mining", 2698, 8785, 0, "Ores available: Coal, Adamantite, Runite", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Charred Island Mining Site", "Skilling • Mining", 2640, 2390, 0, "Ores available: Coal", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Citharede Abbey Mining Site", "Skilling • Mining", 3400, 3170, 0, "Ores available: Iron, Coal", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Coal Trucks Mining Site", "Skilling • Mining", 2580, 3484, 0, "Ores available: Coal", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Crafting Guild Mining Site", "Skilling • Mining", 2939, 3283, 0, "Ores available: Clay, Silver, Gold", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Crandor North East Mining Site", "Skilling • Mining", 2860, 3287, 0, "Ores available: Gold", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Crandor North West Mining Site", "Skilling • Mining", 2831, 3296, 0, "Ores available: Coal, Mithril", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Crandor South East Mining Site", "Skilling • Mining", 2835, 3245, 0, "Ores available: Coal, Adamantite", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Crandor South West Mining Site", "Skilling • Mining", 2819, 3247, 0, "Ores available: Mithril", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Custodia Lake Mining Site", "Skilling • Mining", 1281, 3412, 0, "Ores available: Iron, Coal, Mithril", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Daeyalt Essence Mine Mining Site", "Skilling • Mining", 3631, 3340, 0, "Ores available: Daeyalt Essence", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Deepfin Point North Mining Site", "Skilling • Mining", 1945, 2805, 0, "Ores available: Tin, Copper, Iron, Mithril", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Deepfin Point West Mining Site", "Skilling • Mining", 1932, 2790, 0, "Ores available: Silver, Lead, Gold, Nickel", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Deepfin Mine North Mining Site", "Skilling • Mining", 1996, 9205, 0, "Ores available: Iron, Lead", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Deepfin Mine North West Mining Site", "Skilling • Mining", 1944, 9204, 0, "Ores available: Coal, Adamantite", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Deepfin Mine West Mining Site", "Skilling • Mining", 1932, 9186, 0, "Ores available: Iron, Mithril", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Deepfin Mine North East Mining Site", "Skilling • Mining", 2067, 9208, 0, "Ores available: Nickel", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Deepfin Mine East Mining Site", "Skilling • Mining", 2090, 9201, 0, "Ores available: Coal, Adamantite, Runite", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Desert Mining Camp Surface Mining Site", "Skilling • Mining", 3299, 3021, 0, "Ores available: Copper, Tin, Iron, Coal, Iron, Silver", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Dorgesh Kaan South East Mining Site", "Skilling • Mining", 3322, 9616, 0, "Ores available: Iron", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Dorgesh Kaan South West Mining Site", "Skilling • Mining", 3312, 9621, 0, "Ores available: Iron", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Dorgesh Kaan West Mining Site", "Skilling • Mining", 3311, 9628, 0, "Ores available: Iron, Silver", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Dwarven East Bottom Mining Site", "Skilling • Mining", 3039, 9763, 0, "Ores available: Tin, Iron, Coal, Gold, Adamantite", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Dwarven East Middle Mining Site", "Skilling • Mining", 3037, 9775, 0, "Ores available: Copper, Iron, Coal, Mithril, Adamantite", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Dwarven East Top Mining Site", "Skilling • Mining", 3051, 9820, 0, "Ores available: Clay, Tin, Iron", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Dwarven West Bottom Mining Site", "Skilling • Mining", 3028, 9809, 0, "Ores available: Clay, Copper", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Dwarven West Top Mining Site", "Skilling • Mining", 3031, 9828, 0, "Ores available: Copper, Tin, Iron", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Fremenik Isles East Mining Site", "Skilling • Mining", 2405, 3867, 0, "Ores available: Copper, Tin, Coal", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Eagles Outpost Mining Site", "Skilling • Mining", 3424, 3164, 0, "Ores available: Clay", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Edgeville Dungeon Mining Site", "Skilling • Mining", 3138, 9874, 0, "Ores available: Copper, Tin, Iron, Silver, Coal, Mithril, Adamantite", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Feldip Hills East Mining Site", "Skilling • Mining", 2638, 2996, 0, "Ores available: Rock", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Feldip Hills Middle Mining Site", "Skilling • Mining", 2579, 2998, 0, "Ores available: Rock", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Feldip Hills West Mining Site", "Skilling • Mining", 2567, 2961, 0, "Ores available: Rock", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Fight Arena Mining Site", "Skilling • Mining", 2630, 3142, 0, "Ores available: Clay, Copper, Tin, Iron, Coal, Mithril", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Fossil Island Mining Site", "Skilling • Mining", 3770, 3815, 0, "Ores available: Iron, Coal, Mithril, Adamantite, Runite", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Fremennik Isles West Mining Site", "Skilling • Mining", 2310, 3853, 0, "Ores available: Copper", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Frozen Waste Plateu Center Mining Site", "Skilling • Mining", 2963, 3933, 0, "Ores available: Runite", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Frozen Waste Plateu North Mining Site", "Skilling • Mining", 2975, 3937, 0, "Ores available: Runite", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Frozen Waste Plateu South Mining Site", "Skilling • Mining", 2947, 3914, 0, "Ores available: Runite", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Grand Tree Mining Site", "Skilling • Mining", 2489, 9916, 0, "Ores available: Clay, Iron, Silver, Coal, Gold, Mithril, Adamantite", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Great Conch North Mining Site", "Skilling • Mining", 3183, 2502, 0, "Ores available: Tin, Copper, Iron, Coal, Clay, Mithril", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Great Conch South Mining Site", "Skilling • Mining", 3248, 2352, 0, "Ores available: Iron, Coal, Granite, Sandstone", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Grimstone North East Mining Site", "Skilling • Mining", 2911, 4084, 0, "Ores available: Coal, Adamantite, Nickel", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Grimstone North West Mining Site", "Skilling • Mining", 2901, 4084, 0, "Ores available: Coal, Runite", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Grimstone South Mining Site", "Skilling • Mining", 2909, 4062, 0, "Ores available: Coal, Nickel", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Gwenith Mining Site", "Skilling • Mining", 2163, 3415, 0, "Ores available: Gold", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Gwenith Prifddinas Map Mining Site", "Skilling • Mining", 3187, 6167, 0, "Ores available: Gold", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Heroes Guild East Bottom Mining Site", "Skilling • Mining", 2940, 9884, 0, "Ores available: Coal, Runite", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Heroes Guild East Top Mining Site", "Skilling • Mining", 2939, 9898, 0, "Ores available: Coal", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Heroes Guild West Bottom Mining Site", "Skilling • Mining", 2921, 9904, 0, "Ores available: Coal", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Heroes Guild West Top Mining Site", "Skilling • Mining", 2914, 9916, 0, "Ores available: Mithril, Adamantite", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Hosidius Mine Mining Site", "Skilling • Mining", 1777, 3489, 0, "Ores available: Clay, Copper, Tin, Iron, Silver", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Isafdar Mining Site", "Skilling • Mining", 2277, 3159, 0, "Ores available: Adamantite, Runite", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Isle Of Souls Dungeon East Mining Site", "Skilling • Mining", 2279, 9237, 0, "Ores available: Runite", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Isle Of Souls Dungeon West Mining Site", "Skilling • Mining", 2262, 9244, 0, "Ores available: Adamantite", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Isle Of Souls South Mining Site", "Skilling • Mining", 2195, 2793, 0, "Ores available: Clay, Tin, Copper, Iron, Silver, Coal, Gold, Mithril", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Jatizso Mining Site", "Skilling • Mining", 2396, 3812, 0, "Ores available: Tin, Iron, Coal, Mithril, Adamantite", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Karamja Jungle Mining Site", "Skilling • Mining", 2848, 3033, 0, "Ores available: Iron, Silver, Coal, Mithril, Adamantite", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Karamja Volcano Mining Site", "Skilling • Mining", 2856, 9579, 0, "Ores available: Gold", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Kebos Lowlands Mining Site", "Skilling • Mining", 1211, 3657, 0, "Ores available: Adamantite, Mithril", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Keldagrim Entrance Mining Site", "Skilling • Mining", 2724, 3693, 0, "Ores available: Iron, Mithril", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Keldagrim North East Mining Site", "Skilling • Mining", 2937, 10232, 0, "Ores available: Coal", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Keldagrim South West Bottom Mining Site", "Skilling • Mining", 2872, 10119, 0, "Ores available: Copper, Coal", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Keldagrim South West Middle Mining Site", "Skilling • Mining", 2818, 10156, 0, "Ores available: Iron, Gold", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Keldagrim South West Top Mining Site", "Skilling • Mining", 2864, 10170, 0, "Ores available: Tin", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Lava Maze Dungeon Mining Site", "Skilling • Mining", 3045, 10263, 0, "Ores available: Runite", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Lava Maze North Mining Site", "Skilling • Mining", 3059, 3884, 0, "Ores available: Runite", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Legends Guild East Mining Site", "Skilling • Mining", 2709, 3331, 0, "Ores available: Iron, Coal", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Legends Guild West Mining Site", "Skilling • Mining", 2694, 3332, 0, "Ores available: Iron, Coal", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Lovakengj South Mining Site", "Skilling • Mining", 1476, 3779, 0, "Ores available: Iron, Coal, Mithril", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Lovakengj Sulphur East Mining Site", "Skilling • Mining", 1445, 3870, 0, "Ores available: Volcanic Sulphur", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Lovakengj Sulphur West Mining Site", "Skilling • Mining", 1427, 3870, 0, "Ores available: Volcanic Sulphur", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Lovakengj West 1 Mining Site", "Skilling • Mining", 1430, 3849, 0, "Ores available: Coal, Lovakite", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Lovakengj West 2 Mining Site", "Skilling • Mining", 1447, 3840, 0, "Ores available: Coal, Lovakite", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Lumbridge Swamp East Mining Site", "Skilling • Mining", 3226, 3146, 0, "Ores available: Copper, Tin", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Lumbridge Swamp West Mining Site", "Skilling • Mining", 3148, 3149, 0, "Ores available: Coal, Mithril, Adamantite", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Lunar Isle 1 Mining Site", "Skilling • Mining", 2163, 10347, 0, "Ores available: Gem Rock, Lunar, Silver, Gold", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Lunar Isle 2 Mining Site", "Skilling • Mining", 2165, 10325, 0, "Ores available: Gem Rock, Lunar", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Lunar Isle 3 Mining Site", "Skilling • Mining", 2140, 10318, 0, "Ores available: Silver, Lunar", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Lunar Isle 4 Mining Site", "Skilling • Mining", 2125, 10327, 0, "Ores available: Gold, Lunar", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Lunar Isle 5 Mining Site", "Skilling • Mining", 2124, 10342, 0, "Ores available: Gold, Lunar", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Mining Guild Amethyst Mining Site", "Skilling • Mining", 3022, 9704, 0, "Ores available: Amethyst", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Mining Guild North Mining Site", "Skilling • Mining", 3040, 9740, 0, "Ores available: Iron, Coal, Mithril, Adamantite", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Mining Guild South Mining Site", "Skilling • Mining", 3032, 9720, 0, "Ores available: Iron, Coal, Mithril, Adamantite, Runite", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Mining Guild West Mining Site", "Skilling • Mining", 3006, 9711, 0, "Ores available: Amethyst", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Miscellania Mining Site", "Skilling • Mining", 2526, 3891, 0, "Ores available: Coal", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Miscellania Dungeon Mining Site", "Skilling • Mining", 2504, 10287, 0, "Ores available: Coal", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Mor Ul Rek North Mining Site", "Skilling • Mining", 2458, 5167, 0, "Ores available: Silver, Gold", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Mor Ul Rek South East Mining Site", "Skilling • Mining", 2513, 5074, 0, "Ores available: Silver, Gold", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Mor Ul Rek South West Mining Site", "Skilling • Mining", 2499, 5062, 0, "Ores available: Iron, Coal, Adamantite, Runite", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Mount Karuulm Mining Site", "Skilling • Mining", 1278, 3814, 0, "Ores available: Iron, Coal, Adamantite, Runite", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Necropolis Mining Site", "Skilling • Mining", 3319, 2708, 0, "Ores available: Sandstone, Granite", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Ogress Settlement Mining Site", "Skilling • Mining", 1977, 9041, 0, "Ores available: Coal, Mithril, Adamantite", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Onyx Crest Mining Site", "Skilling • Mining", 2978, 2254, 0, "Ores available: Iron, Silver, Lead, Gem Rock, Gold", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Pandemonium Mining Site", "Skilling • Mining", 3055, 9367, 0, "Ores available: Copper, Tin, Iron, Coal, Lead", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Pirates Hideout Mining Site", "Skilling • Mining", 3056, 3945, 0, "Ores available: Iron, Coal, Mithril, Adamantite", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Piscarillius Mining Site", "Skilling • Mining", 1759, 3718, 0, "Ores available: Copper, Tin, Iron, Silver, Mithril", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Piscatoris Mining Site", "Skilling • Mining", 2337, 3640, 0, "Ores available: Clay, Copper, Tin, Iron", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Port Khazard Mining Site", "Skilling • Mining", 2651, 3172, 0, "Ores available: Copper, Tin, Mithril", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Ralos Rise South Mining Site", "Skilling • Mining", 1480, 3086, 0, "Ores available: Copper, Tin, Iron, Adamantite", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Rellekka Mining Site", "Skilling • Mining", 2682, 3704, 0, "Ores available: Clay, Silver, Coal", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Rimmington Mining Site", "Skilling • Mining", 2977, 3240, 0, "Ores available: Clay, Copper, Tin, Iron, Gold", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Ruins Of Ullek Mining Site", "Skilling • Mining", 3377, 2777, 0, "Ores available: Clay, Iron, Coal, Adamantite", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Ruins Of Unkah Mining Site", "Skilling • Mining", 3172, 2871, 0, "Ores available: Copper, Tin, Iron, Silver, Coal", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Salt Mine Mining Site", "Skilling • Mining", 2835, 10334, 0, "Ores available: Basalt, Te Salt, Efh Salt, Urt Salt", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Salvager Overlook East Mining Site", "Skilling • Mining", 1671, 3284, 0, "Ores available: Iron, Gold", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Salvager Overlook West Mining Site", "Skilling • Mining", 1631, 3277, 0, "Ores available: Coal, Mithril, Adamantite", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Shayzien Mining Site", "Skilling • Mining", 1601, 3645, 0, "Ores available: Clay, Iron, Coal, Mithril, Adamantite", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Shilo Village Surface Mining Site", "Skilling • Mining", 2822, 3001, 0, "Ores available: Gem Rock", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Silvarea Mining Site", "Skilling • Mining", 3371, 3498, 0, "Ores available: Limestone", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Slepe Underground Mining Site", "Skilling • Mining", 3888, 9749, 0, "Ores available: Iron, Coal", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Stonecutter Outpost Mining Site", "Skilling • Mining", 1747, 2955, 0, "Ores available: Clay, Iron, Silver, Coal, Gold, Mithril", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Stranglewood Mine Mining Site", "Skilling • Mining", 1169, 3323, 0, "Ores available: Iron, Coal, Mithril, Adamantite, Runite", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Sunbleak Cave North Mining Site", "Skilling • Mining", 2221, 9003, 0, "Ores available: Coal, Mithril", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Sunbleak Cave South Mining Site", "Skilling • Mining", 2224, 8977, 0, "Ores available: Coal, Adamantite", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Tlati Rainforest East Mining Site", "Skilling • Mining", 1352, 3112, 0, "Ores available: Iron, Coal, Gold, Mithril, Adamantite, Gemstone Crab", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Tlati Rainforest North Mining Site", "Skilling • Mining", 1272, 3173, 0, "Ores available: Silver, Clay, Gemstone Crab", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Tlati Rainforest South Mining Site", "Skilling • Mining", 1238, 3043, 0, "Ores available: Copper, Iron, Coal, Gemstone Crab", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Trahearn Mining Site", "Skilling • Mining", 3295, 5987, 0, "Ores available: Iron, Silver, Coal, Gold, Mithril, Adamantite, Runite, Clay", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Vampyrium Mining Site", "Skilling • Mining", 2567, 7735, 0, "Ores available: Iron", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Varrock South East Mining Site", "Skilling • Mining", 3286, 3365, 0, "Ores available: Copper, Tin, Iron", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Varrock South West Mining Site", "Skilling • Mining", 3176, 3370, 0, "Ores available: Clay, Tin, Iron, Silver", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Vatrachos North Mining Site", "Skilling • Mining", 1891, 2987, 0, "Ores available: Clay, Silver, Gold, Mithril", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Vatrachos South Mining Site", "Skilling • Mining", 1893, 2975, 0, "Ores available: Iron, Coal, Clay, Gold", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Verdant Valley Mining Site", "Skilling • Mining", 3766, 3757, 0, "Ores available: Iron", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Wilderness Resource Area Mining Site", "Skilling • Mining", 3192, 3930, 0, "Ores available: Iron, Coal, Gold, Mithril, Adamantite", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Wilderness South Mining Site", "Skilling • Mining", 3104, 3569, 0, "Ores available: Iron, Coal", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Wilderness South West Mining Site", "Skilling • Mining", 3013, 3589, 0, "Ores available: Coal", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Ynysdail Mining Site", "Skilling • Mining", 2224, 3476, 0, "Ores available: Iron, Nickel", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Ynysdail Cavern North Mining Site", "Skilling • Mining", 2267, 9892, 0, "Ores available: Mithril, Adamantite", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Ynysdail Cavern West Mining Site", "Skilling • Mining", 2249, 9878, 0, "Ores available: Mithril, Runite", "Tool needed: Pickaxe (Bronze to Crystal)", "Check Mining skill guide for ore levels");
		addEntry("mining_site", "Sunstone & Iron Mining Site", "Skilling • Mining", 2614, 2237, 0, "Ores available: Sunstone, Iron", "Tool needed: Pickaxe (Bronze to Crystal)", "Sunstone used for craftables");
		addEntry("mining_site", "Sunstone & Iron Mining Site", "Skilling • Mining", 2598, 2239, 0, "Ores available: Sunstone, Iron", "Tool needed: Pickaxe (Bronze to Crystal)", "Sunstone used for craftables");
		addEntry("mining_site", "Iron & Coal Mining Site", "Skilling • Mining", 3107, 3012, 0, "Ores available: Iron, Coal", "Tool needed: Pickaxe (Bronze to Crystal)", "Desert mining");
		addEntry("mining_site", "Clay & Iron Mining Site", "Skilling • Mining", 3464, 3038, 0, "Ores available: Clay, Iron", "Tool needed: Pickaxe (Bronze to Crystal)", "Uzer mine");
		addEntry("mining_site", "Iron & Coal Mining Site", "Skilling • Mining", 2908, 3362, 0, "Ores available: Iron, Coal", "Tool needed: Pickaxe (Bronze to Crystal)", "Taverley mine");
		addEntry("mining_site", "Iron & Coal Mining Site", "Skilling • Mining", 2698, 3506, 0, "Ores available: Iron, Coal", "Tool needed: Pickaxe (Bronze to Crystal)", "Ardougne mine");
		addEntry("mining_site", "Iron & Coal Mining Site", "Skilling • Mining", 1491, 3847, 0, "Ores available: Iron, Coal", "Tool needed: Pickaxe (Bronze to Crystal)", "Lovakengj mine");
		addEntry("mining_site", "Volcanic Ash & Iron Mining Site", "Skilling • Mining", 2485, 4373, 0, "Ores available: Volcanic Ash, Iron", "Tool needed: Pickaxe (Bronze to Crystal)", "Fossil Island mine");
		addEntry("mining_site", "Volcanic Ash & Iron Mining Site", "Skilling • Mining", 2775, 4420, 0, "Ores available: Volcanic Ash, Iron", "Tool needed: Pickaxe (Bronze to Crystal)", "Fossil Island mine");
		addEntry("mining_site", "Volcanic Ash & Iron Mining Site", "Skilling • Mining", 2724, 4434, 0, "Ores available: Volcanic Ash, Iron", "Tool needed: Pickaxe (Bronze to Crystal)", "Fossil Island mine");
		addEntry("mining_site", "Volcanic Ash & Iron Mining Site", "Skilling • Mining", 2784, 4436, 0, "Ores available: Volcanic Ash, Iron", "Tool needed: Pickaxe (Bronze to Crystal)", "Fossil Island mine");
		addEntry("mining_site", "Volcanic Ash & Iron Mining Site", "Skilling • Mining", 2791, 4436, 0, "Ores available: Volcanic Ash, Iron", "Tool needed: Pickaxe (Bronze to Crystal)", "Fossil Island mine");
		addEntry("mining_site", "Volcanic Ash & Iron Mining Site", "Skilling • Mining", 2750, 4437, 0, "Ores available: Volcanic Ash, Iron", "Tool needed: Pickaxe (Bronze to Crystal)", "Fossil Island mine");
		addEntry("mining_site", "Volcanic Ash & Iron Mining Site", "Skilling • Mining", 2804, 4445, 0, "Ores available: Volcanic Ash, Iron", "Tool needed: Pickaxe (Bronze to Crystal)", "Fossil Island mine");
		addEntry("mining_site", "Volcanic Ash & Iron Mining Site", "Skilling • Mining", 3025, 4490, 0, "Ores available: Volcanic Ash, Iron", "Tool needed: Pickaxe (Bronze to Crystal)", "Fossil Island mine");
		addEntry("mining_site", "Volcanic Ash & Iron Mining Site", "Skilling • Mining", 2723, 4491, 0, "Ores available: Volcanic Ash, Iron", "Tool needed: Pickaxe (Bronze to Crystal)", "Fossil Island mine");
		addEntry("mining_site", "Coal, Mithril & Adamantite Mining Site", "Skilling • Mining", 2810, 4577, 0, "Ores available: Coal, Mithril, Adamantite, Runite", "Tool needed: Pickaxe (Bronze to Crystal)", "Blast Mine");
		addEntry("mining_site", "Coal, Mithril & Adamantite Mining Site", "Skilling • Mining", 2783, 4594, 0, "Ores available: Coal, Mithril, Adamantite, Runite", "Tool needed: Pickaxe (Bronze to Crystal)", "Blast Mine");
		addEntry("mining_site", "Silver & Gold Mining Site", "Skilling • Mining", 2004, 5865, 0, "Ores available: Silver, Gold", "Tool needed: Pickaxe (Bronze to Crystal)", "Lunar Isle mine");
		addEntry("mining_site", "Tephra & Soft Clay Mining Site", "Skilling • Mining", 3026, 6041, 0, "Ores available: Tephra, Soft clay", "Tool needed: Pickaxe (Bronze to Crystal)", "Zalcano / Prifddinas");
		addEntry("mining_site", "Tephra & Soft Clay Mining Site", "Skilling • Mining", 3041, 6041, 0, "Ores available: Tephra, Soft clay", "Tool needed: Pickaxe (Bronze to Crystal)", "Zalcano / Prifddinas");
		addEntry("mining_site", "Tephra & Soft Clay Mining Site", "Skilling • Mining", 3026, 6058, 0, "Ores available: Tephra, Soft clay", "Tool needed: Pickaxe (Bronze to Crystal)", "Zalcano / Prifddinas");
		addEntry("mining_site", "Tephra & Soft Clay Mining Site", "Skilling • Mining", 3041, 6058, 0, "Ores available: Tephra, Soft clay", "Tool needed: Pickaxe (Bronze to Crystal)", "Zalcano / Prifddinas");
		addEntry("mining_site", "Iron, Coal & Mithril Mining Site", "Skilling • Mining", 2726, 6103, 0, "Ores available: Iron, Coal, Mithril", "Tool needed: Pickaxe (Bronze to Crystal)", "Prifddinas North");
		addEntry("mining_site", "Barronite Mining Site", "Skilling • Mining", 1939, 6352, 0, "Ores available: Barronite", "Tool needed: Pickaxe (Bronze to Crystal)", "Camdozaal mine");
		addEntry("mining_site", "Barronite Mining Site", "Skilling • Mining", 1961, 6368, 0, "Ores available: Barronite", "Tool needed: Pickaxe (Bronze to Crystal)", "Camdozaal mine");
		addEntry("mining_site", "Barronite Mining Site", "Skilling • Mining", 1935, 6373, 0, "Ores available: Barronite", "Tool needed: Pickaxe (Bronze to Crystal)", "Camdozaal mine");
		addEntry("mining_site", "Iron, Coal & Mithril Mining Site", "Skilling • Mining", 3035, 6448, 0, "Ores available: Iron, Coal, Mithril", "Tool needed: Pickaxe (Bronze to Crystal)", "Isle of Souls mine");
		addEntry("mining_site", "Iron, Coal & Mithril Mining Site", "Skilling • Mining", 3043, 6448, 0, "Ores available: Iron, Coal, Mithril", "Tool needed: Pickaxe (Bronze to Crystal)", "Isle of Souls mine");
		addEntry("mining_site", "Coal, Mithril & Adamantite Mining Site", "Skilling • Mining", 2616, 8593, 0, "Ores available: Coal, Mithril, Adamantite", "Tool needed: Pickaxe (Bronze to Crystal)", "Cavern mine");
		addEntry("mining_site", "Silver & Iron Mining Site", "Skilling • Mining", 2225, 8721, 0, "Ores available: Silver, Iron", "Tool needed: Pickaxe (Bronze to Crystal)", "Dorgesh-Kaan mine");
		addEntry("mining_site", "Silver & Iron Mining Site", "Skilling • Mining", 2222, 8747, 0, "Ores available: Silver, Iron", "Tool needed: Pickaxe (Bronze to Crystal)", "Dorgesh-Kaan mine");
		addEntry("mining_site", "Coal & Mithril Mining Site", "Skilling • Mining", 2635, 8849, 0, "Ores available: Coal, Mithril", "Tool needed: Pickaxe (Bronze to Crystal)", "Taverley Dungeon");
		addEntry("mining_site", "Gold & Silver Mining Site", "Skilling • Mining", 2152, 9301, 0, "Ores available: Gold, Silver", "Tool needed: Pickaxe (Bronze to Crystal)", "TzHaar mine");
		addEntry("mining_site", "Gold & Silver Mining Site", "Skilling • Mining", 2135, 9308, 0, "Ores available: Gold, Silver", "Tool needed: Pickaxe (Bronze to Crystal)", "TzHaar mine");
		addEntry("mining_site", "Coal & Mithril Mining Site", "Skilling • Mining", 3088, 9499, 0, "Ores available: Coal, Mithril", "Tool needed: Pickaxe (Bronze to Crystal)", "Mining Guild");
		addEntry("mining_site", "Coal & Mithril Mining Site", "Skilling • Mining", 3075, 9505, 0, "Ores available: Coal, Mithril", "Tool needed: Pickaxe (Bronze to Crystal)", "Mining Guild");
		addEntry("mining_site", "Coal & Mithril Mining Site", "Skilling • Mining", 3421, 9631, 0, "Ores available: Coal, Mithril", "Tool needed: Pickaxe (Bronze to Crystal)", "Haunted Mine");
		addEntry("mining_site", "Iron & Coal Mining Site", "Skilling • Mining", 3310, 9645, 0, "Ores available: Iron, Coal", "Tool needed: Pickaxe (Bronze to Crystal)", "Mining site");
		addEntry("mining_site", "Coal & Mithril Mining Site", "Skilling • Mining", 3420, 9646, 0, "Ores available: Coal, Mithril", "Tool needed: Pickaxe (Bronze to Crystal)", "Haunted Mine");
		addEntry("mining_site", "Sulfur & Coal Mining Site", "Skilling • Mining", 1582, 9659, 0, "Ores available: Sulfur, Coal", "Tool needed: Pickaxe (Bronze to Crystal)", "Sulfur mine");
		addEntry("mining_site", "Coal & Mithril Mining Site", "Skilling • Mining", 3384, 9749, 0, "Ores available: Coal, Mithril", "Tool needed: Pickaxe (Bronze to Crystal)", "Lumbridge Swamp Caves");
		addEntry("mining_site", "Coal & Mithril Mining Site", "Skilling • Mining", 3384, 9813, 0, "Ores available: Coal, Mithril", "Tool needed: Pickaxe (Bronze to Crystal)", "Lumbridge Swamp Caves");
		addEntry("mining_site", "Iron & Coal Mining Site", "Skilling • Mining", 1468, 10101, 0, "Ores available: Iron, Coal", "Tool needed: Pickaxe (Bronze to Crystal)", "Catacombs mine");
		addEntry("mining_site", "Iron & Coal Mining Site", "Skilling • Mining", 2911, 10310, 0, "Ores available: Iron, Coal", "Tool needed: Pickaxe (Bronze to Crystal)", "Slayer Tower mine");
		addEntry("mining_site", "Pay-dirt & Gold Mining Site", "Skilling • Mining", 1635, 5328, 1, "Ores available: Pay-dirt, Gold", "Tool needed: Pickaxe (Bronze to Crystal)", "Motherlode Mine");
		addEntry("mining_site", "Pay-dirt & Gold Mining Site", "Skilling • Mining", 1763, 5328, 1, "Ores available: Pay-dirt, Gold", "Tool needed: Pickaxe (Bronze to Crystal)", "Motherlode Mine");
		addEntry("mining_site", "Pay-dirt & Gold Mining Site", "Skilling • Mining", 1649, 5333, 1, "Ores available: Pay-dirt, Gold", "Tool needed: Pickaxe (Bronze to Crystal)", "Motherlode Mine");
		addEntry("mining_site", "Pay-dirt & Gold Mining Site", "Skilling • Mining", 1777, 5333, 1, "Ores available: Pay-dirt, Gold", "Tool needed: Pickaxe (Bronze to Crystal)", "Motherlode Mine");
		addEntry("mining_site", "Pay-dirt & Gold Mining Site", "Skilling • Mining", 1656, 5350, 1, "Ores available: Pay-dirt, Gold", "Tool needed: Pickaxe (Bronze to Crystal)", "Motherlode Mine");
		addEntry("mining_site", "Pay-dirt & Gold Mining Site", "Skilling • Mining", 1784, 5350, 1, "Ores available: Pay-dirt, Gold", "Tool needed: Pickaxe (Bronze to Crystal)", "Motherlode Mine");
		addEntry("mining_site", "Iron & Coal Mining Site", "Skilling • Mining", 1937, 9020, 1, "Ores available: Iron, Coal", "Tool needed: Pickaxe (Bronze to Crystal)", "Keldagrim mine");
		addEntry("mining_site", "Iron & Coal Mining Site", "Skilling • Mining", 1978, 9041, 1, "Ores available: Iron, Coal", "Tool needed: Pickaxe (Bronze to Crystal)", "Keldagrim mine");
		addEntry("mining_site", "Coal & Iron Mining Site", "Skilling • Mining", 1511, 9540, 1, "Ores available: Coal, Iron", "Tool needed: Pickaxe (Bronze to Crystal)", "Lovakengj Upper");
		addEntry("mining_site", "Volcanic Ash Mining Site", "Skilling • Mining", 3889, 9749, 1, "Ores available: Volcanic Ash", "Tool needed: Pickaxe (Bronze to Crystal)", "Fossil Island");
		addEntry("mining_site", "Basalt & Salt Mining Site", "Skilling • Mining", 2386, 4628, 2, "Ores available: Basalt, Salt", "Tool needed: Pickaxe (Bronze to Crystal)", "Weiss mine");
		addEntry("mining_site", "Basalt & Salt Mining Site", "Skilling • Mining", 3672, 9751, 2, "Ores available: Basalt, Salt", "Tool needed: Pickaxe (Bronze to Crystal)", "Salt mine");
		addEntry("mining_site", "Basalt & Salt Mining Site", "Skilling • Mining", 3688, 9756, 2, "Ores available: Basalt, Salt", "Tool needed: Pickaxe (Bronze to Crystal)", "Salt mine");
		addEntry("mining_site", "Basalt & Salt Mining Site", "Skilling • Mining", 3675, 9766, 2, "Ores available: Basalt, Salt", "Tool needed: Pickaxe (Bronze to Crystal)", "Salt mine");
		addEntry("mining_site", "Daeyalt Essence Mining Site", "Skilling • Mining", 2333, 10318, 2, "Ores available: Daeyalt Essence", "Tool needed: Pickaxe (Bronze to Crystal)", "Daeyalt Essence mine");
		addEntry("mining_site", "Daeyalt Essence Mining Site", "Skilling • Mining", 2358, 10325, 2, "Ores available: Daeyalt Essence", "Tool needed: Pickaxe (Bronze to Crystal)", "Daeyalt Essence mine");
		addEntry("mining_site", "Daeyalt Essence Mining Site", "Skilling • Mining", 2318, 10327, 2, "Ores available: Daeyalt Essence", "Tool needed: Pickaxe (Bronze to Crystal)", "Daeyalt Essence mine");
		addEntry("mining_site", "Daeyalt Essence Mining Site", "Skilling • Mining", 2317, 10342, 2, "Ores available: Daeyalt Essence", "Tool needed: Pickaxe (Bronze to Crystal)", "Daeyalt Essence mine");
		addEntry("mining_site", "Daeyalt Essence Mining Site", "Skilling • Mining", 2356, 10347, 2, "Ores available: Daeyalt Essence", "Tool needed: Pickaxe (Bronze to Crystal)", "Daeyalt Essence mine");
	}

	/** Fixed travel links: fairy rings, spirit trees, ships, carpets, quetzals. */
	private static void addTransportLinks()
	{
		addEntry("transportation", "Ship to Brimhaven / Rimmington", "Travel", 2675, 3275, 0, "Destination / Route: Ship to Brimhaven / Rimmington", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Ship to Ardougne / Rimmington", "Travel", 2772, 3234, 0, "Destination / Route: Ship to Ardougne / Rimmington", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Ship to Ardougne / Brimhaven", "Travel", 2915, 3224, 0, "Destination / Route: Ship to Ardougne / Brimhaven", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Ship to Port Sarim/The Pandemonium", "Travel", 2954, 3145, 0, "Destination / Route: Ship to Port Sarim/The Pandemonium", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Ship to Port Sarim/Port Piscarilius", "Travel", 1503, 3398, 0, "Destination / Route: Ship to Port Sarim/Port Piscarilius", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Ship to Port Sarim/Musa Point", "Travel", 3065, 3003, 0, "Destination / Route: Ship to Port Sarim/Musa Point", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Ship to Port Sarim/Land's End", "Travel", 1823, 3692, 0, "Destination / Route: Ship to Port Sarim/Land's End", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Ship to Musa Point/The Pandemonium", "Travel", 3029, 3218, 0, "Destination / Route: Ship to Musa Point/The Pandemonium", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Ship to Jatizso/Neitiznot", "Travel", 2639, 3710, 0, "Destination / Route: Ship to Jatizso/Neitiznot", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Ship to Weiss/Iceberg", "Travel", 2707, 3735, 0, "Destination / Route: Ship to Weiss/Iceberg", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Rowboat to Molch/Molch Island/Shayzien", "Travel", 1383, 3663, 0, "Destination / Route: Rowboat to Molch/Molch Island/Shayzien", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Rowboat to North Custodia Pass", "Travel", 1281, 3386, 0, "Destination / Route: Rowboat to North Custodia Pass", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Rowboat to South Custodia Pass", "Travel", 1256, 3395, 0, "Destination / Route: Rowboat to South Custodia Pass", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Rowboat to Barge/Camp/North of Island", "Travel", 3759, 3902, 0, "Destination / Route: Rowboat to Barge/Camp/North of Island", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Rowboat to Misthalin Mystery", "Travel", 3238, 3141, 0, "Destination / Route: Rowboat to Misthalin Mystery", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Rowboat to Molch Island/Shayzien/Battlefront", "Travel", 1343, 3646, 0, "Destination / Route: Rowboat to Molch Island/Shayzien/Battlefront", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Rowboat to Molch/Shayzien/Battlefront", "Travel", 1368, 3641, 0, "Destination / Route: Rowboat to Molch/Shayzien/Battlefront", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Rowboat to Barge/Digsite/North of Island", "Travel", 3723, 3807, 0, "Destination / Route: Rowboat to Barge/Digsite/North of Island", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Rowboat to Barge/Camp/Sea", "Travel", 3733, 3894, 0, "Destination / Route: Rowboat to Barge/Camp/Sea", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Rowboat to Molch/Molch Island/Battlefront", "Travel", 1405, 3612, 0, "Destination / Route: Rowboat to Molch/Molch Island/Battlefront", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Rowboat to Tal Teklan/Kastori", "Travel", 1398, 3245, 0, "Destination / Route: Rowboat to Tal Teklan/Kastori", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Rowboat to Kastori/Gloomthorn Trail", "Travel", 1258, 3124, 0, "Destination / Route: Rowboat to Kastori/Gloomthorn Trail", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Rowboat to Tal Teklan/Gloomthorn Trail", "Travel", 1389, 3074, 0, "Destination / Route: Rowboat to Tal Teklan/Gloomthorn Trail", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Charter Ship", "Travel", 1455, 2968, 0, "Destination / Route: Charter Ship", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Charter Ship", "Travel", 2760, 3238, 0, "Destination / Route: Charter Ship", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Charter Ship", "Travel", 2791, 3415, 0, "Destination / Route: Charter Ship", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Charter Ship", "Travel", 1743, 3136, 0, "Destination / Route: Charter Ship", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Charter Ship", "Travel", 2589, 2851, 0, "Destination / Route: Charter Ship", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Charter Ship", "Travel", 1943, 2753, 0, "Destination / Route: Charter Ship", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Charter Ship", "Travel", 2954, 3158, 0, "Destination / Route: Charter Ship", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Charter Ship", "Travel", 2999, 3032, 0, "Destination / Route: Charter Ship", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Charter Ship", "Travel", 2673, 3143, 0, "Destination / Route: Charter Ship", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Charter Ship", "Travel", 3669, 2931, 0, "Destination / Route: Charter Ship", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Charter Ship", "Travel", 3702, 3503, 0, "Destination / Route: Charter Ship", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Charter Ship", "Travel", 3037, 3191, 0, "Destination / Route: Charter Ship", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Charter Ship", "Travel", 2141, 3123, 0, "Destination / Route: Charter Ship", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Charter Ship", "Travel", 3061, 2999, 0, "Destination / Route: Charter Ship", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Charter Ship", "Travel", 2156, 3331, 0, "Destination / Route: Charter Ship", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Charter Ship", "Travel", 3180, 6083, 0, "Destination / Route: Charter Ship", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Charter Ship", "Travel", 3186, 2367, 0, "Destination / Route: Charter Ship", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Charter Ship", "Travel", 1513, 2970, 0, "Destination / Route: Charter Ship", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Lovakengj Minecart Network - Arceuus", "Travel", 1673, 3832, 0, "Destination / Route: Lovakengj Minecart Network - Arceuus", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Lovakengj Minecart Network - Farming Guild", "Travel", 1219, 3738, 0, "Destination / Route: Lovakengj Minecart Network - Farming Guild", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Minecart to Keldagrim", "Travel", 3139, 3504, 0, "Destination / Route: Minecart to Keldagrim", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Lovakengj Minecart Network - Hosidius South", "Travel", 1805, 3480, 0, "Destination / Route: Lovakengj Minecart Network - Hosidius South", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Lovakengj Minecart Network - Hosidius West", "Travel", 1656, 3542, 0, "Destination / Route: Lovakengj Minecart Network - Hosidius West", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Minecart to Keldagrim", "Travel", 2995, 9836, 0, "Destination / Route: Minecart to Keldagrim", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Keldagrim Minecart System", "Travel", 2908, 10170, 0, "Destination / Route: Keldagrim Minecart System", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Lovakengj Minecart Network - Kingstown", "Travel", 1697, 3658, 0, "Destination / Route: Lovakengj Minecart Network - Kingstown", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Lovakengj Minecart Network - Kourend Woodland", "Travel", 1570, 3464, 0, "Destination / Route: Lovakengj Minecart Network - Kourend Woodland", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Lovakengj Minecart Network - Lovakengj", "Travel", 1524, 3725, 0, "Destination / Route: Lovakengj Minecart Network - Lovakengj", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Lovakengj Minecart Network - Mount Quidamortem", "Travel", 1253, 3550, 0, "Destination / Route: Lovakengj Minecart Network - Mount Quidamortem", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Lovakengj Minecart Network - Northern Tundras", "Travel", 1646, 3930, 0, "Destination / Route: Lovakengj Minecart Network - Northern Tundras", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Lovakengj Minecart Network - Port Piscarilius", "Travel", 1760, 3708, 0, "Destination / Route: Lovakengj Minecart Network - Port Piscarilius", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Lovakengj Minecart Network - Shayzien East", "Travel", 1586, 3622, 0, "Destination / Route: Lovakengj Minecart Network - Shayzien East", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Lovakengj Minecart Network - Shayzien West", "Travel", 1413, 3579, 0, "Destination / Route: Lovakengj Minecart Network - Shayzien West", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Minecart to Keldagrim", "Travel", 2874, 9870, 0, "Destination / Route: Minecart to Keldagrim", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Canoe", "Travel", 3111, 3409, 0, "Destination / Route: Canoe", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Canoe", "Travel", 3202, 3344, 0, "Destination / Route: Canoe", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Canoe", "Travel", 3130, 3509, 0, "Destination / Route: Canoe", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Canoe", "Travel", 3241, 3238, 0, "Destination / Route: Canoe", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Canoe", "Travel", 3155, 3630, 0, "Destination / Route: Canoe", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Canoe", "Travel", 2439, 3137, 0, "Destination / Route: Canoe", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Canoe", "Travel", 2484, 3192, 0, "Destination / Route: Canoe", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Canoe", "Travel", 2576, 3262, 0, "Destination / Route: Canoe", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Canoe", "Travel", 2572, 3358, 0, "Destination / Route: Canoe", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Canoe", "Travel", 2524, 3409, 0, "Destination / Route: Canoe", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Gnome Glider", "Travel", 3278, 3213, 0, "Destination / Route: Gnome Glider", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Gnome Glider", "Travel", 2712, 2804, 0, "Destination / Route: Gnome Glider", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Gnome Glider", "Travel", 2971, 2974, 0, "Destination / Route: Gnome Glider", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Gnome Glider", "Travel", 2540, 2969, 0, "Destination / Route: Gnome Glider", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Gnome Glider", "Travel", 2460, 3502, 0, "Destination / Route: Gnome Glider", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Gnome Glider", "Travel", 2845, 3501, 0, "Destination / Route: Gnome Glider", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Hot Air Balloon", "Travel", 3298, 3480, 0, "Destination / Route: Hot Air Balloon", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Hot Air Balloon", "Travel", 2458, 3108, 0, "Destination / Route: Hot Air Balloon", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Hot Air Balloon", "Travel", 2478, 3459, 0, "Destination / Route: Hot Air Balloon", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Hot Air Balloon", "Travel", 2936, 3422, 0, "Destination / Route: Hot Air Balloon", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Hot Air Balloon", "Travel", 2921, 3301, 0, "Destination / Route: Hot Air Balloon", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Spirit Tree", "Travel", 2554, 3259, 0, "Destination / Route: Spirit Tree", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Spirit Tree", "Travel", 2485, 2850, 0, "Destination / Route: Spirit Tree", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Spirit Tree", "Travel", 2459, 3446, 0, "Destination / Route: Spirit Tree", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Spirit Tree", "Travel", 2538, 3166, 0, "Destination / Route: Spirit Tree", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Spirit Tree", "Travel", 3184, 3510, 0, "Destination / Route: Spirit Tree", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Spirit Tree", "Travel", 1202, 2787, 0, "Destination / Route: Spirit Tree", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Spirit Tree", "Travel", 3274, 6124, 0, "Destination / Route: Spirit Tree", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Carpet to Bedabin/Pollnivneach/Uzer", "Travel", 3311, 3107, 0, "Destination / Route: Carpet to Bedabin/Pollnivneach/Uzer", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Carpet to Nardah/Sophanem/Menaphos", "Travel", 3345, 2943, 0, "Destination / Route: Carpet to Nardah/Sophanem/Menaphos", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Teleport to Library Archive", "Travel", 1623, 3808, 0, "Destination / Route: Teleport to Library Archive", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Teleport to Mos Le'Harmless", "Travel", 3784, 2828, 0, "Destination / Route: Teleport to Mos Le'Harmless", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Teleport to Rune Essence", "Travel", 2681, 3325, 0, "Destination / Route: Teleport to Rune Essence", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Teleport to Rune Essence", "Travel", 2592, 3089, 0, "Destination / Route: Teleport to Rune Essence", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Teleport to Sorceress's Garden", "Travel", 3320, 3141, 0, "Destination / Route: Teleport to Sorceress's Garden", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Teleport to Prifddinas Library", "Travel", 3254, 6082, 2, "Destination / Route: Teleport to Prifddinas Library", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Mountain Guide", "Travel", 1275, 3559, 0, "Destination / Route: Mountain Guide", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Mountain Guide", "Travel", 1270, 3477, 0, "Destination / Route: Mountain Guide", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Mountain Guide", "Travel", 1400, 3538, 0, "Destination / Route: Mountain Guide", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Guide to Auburn Valley", "Travel", 1486, 3230, 0, "Destination / Route: Guide to Auburn Valley", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Guide to Quetzacalli Gorge", "Travel", 1358, 3310, 0, "Destination / Route: Guide to Quetzacalli Gorge", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Altar to Skotizo", "Travel", 1662, 10047, 0, "Destination / Route: Altar to Skotizo", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Fairy Ring to Al Kharid", "Travel", 2483, 4471, 0, "Destination / Route: Fairy Ring to Al Kharid", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Fairy Ring to Shack", "Travel", 2451, 4471, 0, "Destination / Route: Fairy Ring to Shack", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Mushtree", "Travel", 3674, 3871, 0, "Destination / Route: Mushtree", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Mushtree", "Travel", 3676, 3755, 0, "Destination / Route: Mushtree", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Mushtree", "Travel", 3757, 3756, 0, "Destination / Route: Mushtree", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Portal to Guilds", "Travel", 2456, 2856, 0, "Destination / Route: Portal to Guilds", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Portal to Edgeville/Ferox Enclave", "Travel", 2204, 2858, 0, "Destination / Route: Portal to Edgeville/Ferox Enclave", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Teleporter - The Palace", "Travel", 2592, 6424, 0, "Destination / Route: Teleporter - The Palace", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Teleporter - The Plaza", "Travel", 2616, 6417, 0, "Destination / Route: Teleporter - The Plaza", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Teleporter - The Cathedral", "Travel", 2651, 6405, 0, "Destination / Route: Teleporter - The Cathedral", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Teleporter - Western Residential District", "Travel", 2642, 6434, 0, "Destination / Route: Teleporter - Western Residential District", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Teleporter - Eastern Residential District", "Travel", 2690, 6415, 0, "Destination / Route: Teleporter - Eastern Residential District", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Teleporter - Northern Science District", "Travel", 2610, 6379, 0, "Destination / Route: Teleporter - Northern Science District", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Teleporter - Southern Science District", "Travel", 2598, 6341, 0, "Destination / Route: Teleporter - Southern Science District", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Railway Station", "Travel", 2941, 10179, 0, "Destination / Route: Railway Station", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Wilderness Lever to Ardougne/Edgeville", "Travel", 3154, 3924, 0, "Destination / Route: Wilderness Lever to Ardougne/Edgeville", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Return Portal", "Travel", 2083, 2732, 0, "Destination / Route: Return Portal", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Quetzal", "Travel", 1388, 2899, 0, "Destination / Route: Quetzal", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Quetzal", "Travel", 1410, 3363, 0, "Destination / Route: Quetzal", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Quetzal", "Travel", 1701, 3037, 0, "Destination / Route: Quetzal", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Quetzal", "Travel", 1671, 2933, 0, "Destination / Route: Quetzal", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Quetzal", "Travel", 1447, 3108, 0, "Destination / Route: Quetzal", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Quetzal", "Travel", 1776, 3111, 0, "Destination / Route: Quetzal", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Quetzal", "Travel", 1584, 3055, 0, "Destination / Route: Quetzal", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Quetzal", "Travel", 1343, 3020, 0, "Destination / Route: Quetzal", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Quetzal", "Travel", 1511, 3222, 0, "Destination / Route: Quetzal", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Quetzal", "Travel", 1612, 3302, 0, "Destination / Route: Quetzal", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Quetzal", "Travel", 1547, 2997, 0, "Destination / Route: Quetzal", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Quetzal", "Travel", 1436, 3169, 0, "Destination / Route: Quetzal", "Check travel route for fares or quest requirements");
		addEntry("transportation", "Quetzal", "Travel", 1225, 3089, 0, "Destination / Route: Quetzal", "Check travel route for fares or quest requirements");
	}

	/** Quest start points, with requirements and rewards. */
	private static void addQuestStarts()
	{
		addEntry("quest_start", "Cook's Assistant", "Quest Start • Novice", 3208, 3214, 0, "Start: Cook in Lumbridge Castle kitchen", "Difficulty: Novice • Length: Short", "Reward: 1 Quest Point", "Requirements: 1x Bucket of milk, 1x Pot of flour");
		addEntry("quest_start", "The Restless Ghost", "Quest Start • Novice", 3244, 3206, 0, "Start: Father Aereck in Lumbridge Church", "Difficulty: Novice • Length: Short", "Reward: 1 Quest Point", "Requirements: Ghostspeak amulet (provided by Father Urhney)");
		addEntry("quest_start", "Sheep Shearer", "Quest Start • Novice", 3190, 3272, 0, "Start: Fred the Farmer north of Lumbridge", "Difficulty: Novice • Length: Short", "Reward: 1 Quest Point", "Requirements: 20x Balls of wool, Shears");
		addEntry("quest_start", "Demon Slayer", "Quest Start • Novice", 3203, 3424, 0, "Start: Gypsy Aris in Varrock Square", "Difficulty: Novice • Length: Medium", "Reward: 3 Quest Points", "Requirements: Silverlight sword, 1x Coin");
		addEntry("quest_start", "Shield of Arrav", "Quest Start • Novice", 3222, 3474, 0, "Start: King Roald in Varrock Palace", "Difficulty: Novice • Length: Short", "Reward: 1 Quest Point", "Requirements: Partner of opposing gang (Phoenix / Black Arm)");
		addEntry("quest_start", "Gertrude's Cat", "Quest Start • Novice", 3273, 3401, 0, "Start: Gertrude west of Varrock", "Difficulty: Novice • Length: Short", "Reward: 1 Quest Point", "Requirements: 1x Raw sardine, 1x Bucket of milk");
		addEntry("quest_start", "Dragon Slayer I", "Quest Start • Experienced", 3074, 3515, 0, "Start: Oziach in Edgeville / Guildmaster in Champions' Guild", "Difficulty: Experienced • Length: Long", "Reward: 3 Quest Points", "Requirements: 32 Quest Points, Anti-dragon shield");
		addEntry("quest_start", "Animal Magnetism", "Quest Start • Intermediate", 3093, 3357, 0, "Start: Ava in Draynor Manor", "Difficulty: Intermediate • Length: Medium", "Reward: 1 Quest Point", "Requirements: 18 Slayer, 19 Crafting, 30 Ranged, 35 Woodcutting", "Unlocks: Ava's attractor / accumulator");
		addEntry("quest_start", "Pirate's Treasure", "Quest Start • Novice", 3097, 3257, 0, "Start: Redbeard Frank in Port Sarim", "Difficulty: Novice • Length: Short", "Reward: 2 Quest Points", "Requirements: Karamja rum, White apron");
		addEntry("quest_start", "Ernest the Chicken", "Quest Start • Novice", 3110, 3330, 0, "Start: Veronica outside Draynor Manor", "Difficulty: Novice • Length: Short", "Reward: 4 Quest Points", "Requirements: 1x Spade, 1x Fish food");
		addEntry("quest_start", "Vampyre Slayer", "Quest Start • Novice", 3086, 3236, 0, "Start: Morgan in Draynor Village", "Difficulty: Novice • Length: Short", "Reward: 3 Quest Points", "Requirements: 1x Garlic, 1x Wooden stake");
		addEntry("quest_start", "Doric's Quest", "Quest Start • Novice", 2951, 3451, 0, "Start: Doric north of Falador", "Difficulty: Novice • Length: Short", "Reward: 1 Quest Point", "Requirements: 6x Clay, 4x Copper ore");
		addEntry("quest_start", "Black Knights' Fortress", "Quest Start • Novice", 2961, 3338, 0, "Start: Sir Amik Varze in Falador Castle", "Difficulty: Novice • Length: Short", "Reward: 3 Quest Points", "Requirements: 12 Quest Points, Iron chainbody, Bronze med helm, Cabbage");
		addEntry("quest_start", "The Knight's Sword", "Quest Start • Intermediate", 2982, 3338, 0, "Start: Squire in Falador Castle courtyard", "Difficulty: Intermediate • Length: Short", "Reward: 1 Quest Point", "Requirements: 10 Mining, Redberry pie, 2x Iron bars, Blurite ore");
		addEntry("quest_start", "Plague City", "Quest Start • Novice", 2617, 3299, 0, "Start: Edmond in East Ardougne", "Difficulty: Novice • Length: Short", "Reward: 1 Quest Point", "Requirements: Dwellberries, Rope, Spade, Bucket of milk");
		addEntry("quest_start", "Biohazard", "Quest Start • Novice", 2606, 3264, 0, "Start: Elena in East Ardougne", "Difficulty: Novice • Length: Medium", "Reward: 3 Quest Points", "Requirements: Plague City completion, Priest gown, Ethically sourced samples");
		addEntry("quest_start", "Tower of Life", "Quest Start • Novice", 2592, 3335, 0, "Start: Effigy south of Ardougne Monastery", "Difficulty: Novice • Length: Medium", "Reward: 2 Quest Points", "Requirements: 10 Construction, Beer, Saw, Hammer");
		addEntry("quest_start", "Hazeel Cult", "Quest Start • Novice", 2575, 3320, 0, "Start: Ceril Carnillean in south-west Ardougne", "Difficulty: Novice • Length: Short", "Reward: 1 Quest Point", "Requirements: Carnillean armor / Hazeel scroll");
		addEntry("quest_start", "Monk's Friend", "Quest Start • Novice", 2568, 3334, 0, "Start: Brother Omad in Ardougne Monastery", "Difficulty: Novice • Length: Short", "Reward: 1 Quest Point", "Requirements: 1x Jug of water, 1x Log");
		addEntry("quest_start", "Tree Gnome Village", "Quest Start • Intermediate", 2542, 3169, 0, "Start: King Bolren in Tree Gnome Village maze", "Difficulty: Intermediate • Length: Short", "Reward: 2 Quest Points", "Requirements: 6x Logs, Protection prayers recommended");
		addEntry("quest_start", "The Grand Tree", "Quest Start • Experienced", 2467, 3497, 0, "Start: King Narnode Shareen in Tree Gnome Stronghold", "Difficulty: Experienced • Length: Medium", "Reward: 5 Quest Points", "Requirements: 25 Agility, Gnome glider transportation access");
		addEntry("quest_start", "Monkey Madness I", "Quest Start • Master", 2522, 3498, 0, "Start: King Narnode Shareen in Tree Gnome Stronghold", "Difficulty: Master • Length: Long", "Reward: 3 Quest Points", "Requirements: The Grand Tree, Tree Gnome Village", "Unlocks: Dragon scimitar & Monkey greegree");
		addEntry("quest_start", "Fishing Contest", "Quest Start • Novice", 2566, 3461, 0, "Start: Vestri at McGrubor's Wood / Dwarf entrance", "Difficulty: Novice • Length: Short", "Reward: 1 Quest Point", "Requirements: 10 Fishing, Garlic, Fishing rod, Red vine worms");
		addEntry("quest_start", "Elemental Workshop I", "Quest Start • Novice", 2740, 3445, 0, "Start: Slashed book in Seers' Village library", "Difficulty: Novice • Length: Short", "Reward: 1 Quest Point", "Requirements: 20 Mining, 20 Smithing, 20 Crafting, Elemental shield");
		addEntry("quest_start", "One Small Favour", "Quest Start • Experienced", 2718, 3470, 0, "Start: Yanni Salika in Shilo Village", "Difficulty: Experienced • Length: Very Long", "Reward: 2 Quest Points", "Requirements: 36 Agility, 25 Crafting, 18 Herblore, 30 Smithing", "Unlocks: Steel key ring, 2x 10,000 XP lamps");
		addEntry("quest_start", "Waterfall Quest", "Quest Start • Intermediate", 2517, 3574, 0, "Start: Almera south-west of Baxtorian Falls", "Difficulty: Intermediate • Length: Medium", "Reward: 1 Quest Point", "Requirements: Rope, 6x Air, 6x Water, 6x Earth runes", "Reward: +13,750 Attack & Strength XP");
		addEntry("quest_start", "Merlin's Crystal", "Quest Start • Intermediate", 2839, 3538, 0, "Start: King Arthur in Camelot Castle", "Difficulty: Intermediate • Length: Medium", "Reward: 6 Quest Points", "Requirements: Excalibur sword, Black candle, Bat bones, Tinderbox");
		addEntry("quest_start", "Holy Grail", "Quest Start • Intermediate", 2839, 3532, 0, "Start: King Arthur in Camelot Castle", "Difficulty: Intermediate • Length: Medium", "Reward: 2 Quest Points", "Requirements: Merlin's Crystal completion, 20 Attack");
		addEntry("quest_start", "Priest in Peril", "Quest Start • Novice", 3483, 3485, 0, "Start: King Roald in Varrock Palace / Drezel at Paterdomus", "Difficulty: Novice • Length: Medium", "Reward: 1 Quest Point", "Requirements: 50x Pure or Rune essence, Access to Morytania");
		addEntry("quest_start", "Nature Spirit", "Quest Start • Novice", 3466, 3480, 0, "Start: Drezel in Paterdomus temple cellar", "Difficulty: Novice • Length: Medium", "Reward: 2 Quest Points", "Requirements: Priest in Peril completion, 18 Crafting");
		addEntry("quest_start", "Creature of Fenkenstrain", "Quest Start • Intermediate", 3488, 3537, 0, "Start: Faris at Fenkenstrain's Castle", "Difficulty: Intermediate • Length: Medium", "Reward: 2 Quest Points", "Requirements: 20 Crafting, 25 Thieving, Ring of charos");
		addEntry("quest_start", "A Taste of Hope", "Quest Start • Experienced", 3650, 3480, 0, "Start: Safalaan in Meiyerditch hideout", "Difficulty: Experienced • Length: Medium", "Reward: 1 Quest Point", "Requirements: Darkness of Hallowvale, 48 Crafting, 45 Agility", "40 Attack, 38 Slayer");
		addEntry("quest_start", "Sins of the Father", "Quest Start • Master", 3635, 3375, 0, "Start: Veliaf Hurtz in Slepe / Meiyerditch hideout", "Difficulty: Master • Length: Long", "Reward: 2 Quest Points", "Requirements: A Taste of Hope, 62 Woodcutting, 60 Fletching", "56 Crafting, 52 Agility, 50 Attack, 50 Slayer, 49 Magic");
		addEntry("quest_start", "Regicide", "Quest Start • Master", 2185, 3145, 0, "Start: King Lathas in East Ardougne Castle", "Difficulty: Master • Length: Long", "Reward: 3 Quest Points", "Requirements: Underground Pass, 56 Agility, 10 Crafting");
		addEntry("quest_start", "Roving Elves", "Quest Start • Master", 2185, 3160, 0, "Start: Islwyn and Eluned in Isafdar", "Difficulty: Master • Length: Medium", "Reward: 1 Quest Point", "Requirements: Regicide, Waterfall Quest, 56 Agility");
		addEntry("quest_start", "Mourning's End Part I", "Quest Start • Master", 2190, 3258, 0, "Start: Arianwyn in Lletya", "Difficulty: Master • Length: Long", "Reward: 2 Quest Points", "Requirements: Roving Elves, Big Chompy Bird Hunting, 60 Ranged, 50 Thieving");
		addEntry("quest_start", "Mourning's End Part II", "Quest Start • Master", 2190, 3262, 0, "Start: Arianwyn in Lletya", "Difficulty: Master • Length: Long", "Reward: 2 Quest Points", "Requirements: Mourning's End Part I, Death altar access, Death talisman");
		addEntry("quest_start", "Song of the Elves", "Quest Start • Grandmaster", 2175, 3245, 0, "Start: Arianwyn / Lord Iorwerth in Prifddinas outskirts", "Difficulty: Grandmaster • Length: Very Long", "Reward: 4 Quest Points", "Requirements: Mourning's End Part II, Level 70 in Agility,", "Construction, Farming, Herblore, Hunter, Mining, Smithing, Woodcutting");
		addEntry("quest_start", "Prince Ali Rescue", "Quest Start • Novice", 3305, 3235, 0, "Start: Hassan in Al Kharid Palace", "Difficulty: Novice • Length: Short", "Reward: 3 Quest Points", "Requirements: Soft clay, Bronze bar, Pink skirt, Yellow dye", "3x Beers, 3x Ropes • Free Al Kharid gate pass");
		addEntry("quest_start", "The Feud", "Quest Start • Intermediate", 3305, 3160, 0, "Start: Ali Morrisane in Al Kharid / Rug merchant", "Difficulty: Intermediate • Length: Medium", "Reward: 1 Quest Point", "Requirements: 30 Thieving, Blackjack, Rogue trader minigame");
		addEntry("quest_start", "Shadow of the Storm", "Quest Start • Intermediate", 3315, 3165, 0, "Start: Father Reen outside Al Kharid bank", "Difficulty: Intermediate • Length: Medium", "Reward: 1 Quest Point", "Requirements: Demon Slayer, The Golem, 30 Crafting");
		addEntry("quest_start", "Desert Treasure I", "Quest Start • Master", 3175, 3040, 0, "Start: Archaeologist at Bedabin Camp", "Difficulty: Master • Length: Long", "Reward: 3 Quest Points", "Requirements: The Dig Site, Temple of Ikov, Tourist Trap,", "Troll Stronghold, Priest in Peril, 53 Thieving, 50 Magic, 50 FM");
		addEntry("quest_start", "Beneath Cursed Sands", "Quest Start • Master", 3415, 2849, 0, "Start: Jamila in Sophanem", "Difficulty: Master • Length: Long", "Reward: 2 Quest Points", "Requirements: Contact!, Scorpia, 62 Agility, 55 Crafting, 55 Firemaking");
		addEntry("quest_start", "Desert Treasure II - The Fallen Empire", "Quest Start • Grandmaster", 3480, 2780, 0, "Start: Sorcerer in Ruins of Ullek / Ancient Vault", "Difficulty: Grandmaster • Length: Very Long", "Reward: 5 Quest Points", "Requirements: Desert Treasure I, Secrets of the North,", "Beneath Cursed Sands, 75 Magic, 75 Firemaking, 70 Thieving,", "62 Agility, 60 Herblore, 60 Runecraft, 60 Construction");
		addEntry("quest_start", "Troll Stronghold", "Quest Start • Intermediate", 2875, 3670, 0, "Start: Denulth in Burthorpe", "Difficulty: Intermediate • Length: Medium", "Reward: 1 Quest Point", "Requirements: Death Plateau, 15 Agility, 30 Thieving");
		addEntry("quest_start", "Eadgar's Ruse", "Quest Start • Experienced", 2880, 3675, 0, "Start: Sanfew in Taverley herb shop", "Difficulty: Experienced • Length: Medium", "Reward: 1 Quest Point", "Requirements: Druidic Ritual, Troll Stronghold, 31 Herblore, 61 Magic");
		addEntry("quest_start", "My Arm's Big Adventure", "Quest Start • Intermediate", 2840, 3660, 0, "Start: Burntmeat in Troll Stronghold kitchen", "Difficulty: Intermediate • Length: Medium", "Reward: 1 Quest Point", "Requirements: Eadgar's Ruse, The Feud, 10 Farming, 29 Agility");
		addEntry("quest_start", "Making Friends with My Arm", "Quest Start • Experienced", 2840, 3920, 0, "Start: My Arm in Troll Stronghold", "Difficulty: Experienced • Length: Long", "Reward: 2 Quest Points", "Requirements: My Arm's Big Adventure, Swan Song, Cold War,", "Romeo & Juliet, 66 Firemaking, 72 Mining, 35 Construction, 68 Agility");
		addEntry("quest_start", "Druidic Ritual", "Quest Start • Novice", 2895, 3445, 0, "Start: Kaqemeex at stone circle north of Taverley", "Difficulty: Novice • Length: Short", "Reward: 4 Quest Points", "Requirements: Raw bear meat, Raw beef, Raw chicken, Raw rat meat", "Unlocks: Herblore skill");
		addEntry("quest_start", "Heroes' Quest", "Quest Start • Experienced", 2905, 3440, 0, "Start: Achietties outside Heroes' Guild", "Difficulty: Experienced • Length: Long", "Reward: 1 Quest Point", "Requirements: Shield of Arrav, Lost City, Merlin's Crystal,", "Dragon Slayer I, 55 QP, 53 Cooking, 53 Fishing, 25 Herblore, 50 Mining");
		addEntry("quest_start", "Death Plateau", "Quest Start • Novice", 2935, 3515, 0, "Start: Denulth in Burthorpe", "Difficulty: Novice • Length: Short", "Reward: 1 Quest Point", "Requirements: 1x Asgarnian ale, 10x Bread, 10x Cooked trout, 1x Iron bar", "Unlocks: Climbing boots access");
		addEntry("quest_start", "Witch's House", "Quest Start • Intermediate", 2870, 3540, 0, "Start: Boy outside Witch's House in Taverley", "Difficulty: Intermediate • Length: Short", "Reward: 4 Quest Points", "Requirements: 1x Cheese, Leather gloves");
		addEntry("quest_start", "Tai Bwo Wannai Trio", "Quest Start • Intermediate", 2900, 3500, 0, "Start: Timfraku in Tai Bwo Wannai", "Difficulty: Intermediate • Length: Medium", "Reward: 2 Quest Points", "Requirements: Jungle Potion, 15 Agility, 30 Cooking, 15 Fishing");
		addEntry("quest_start", "Jungle Potion", "Quest Start • Novice", 2795, 3065, 0, "Start: Trufitus in Tai Bwo Wannai", "Difficulty: Novice • Length: Short", "Reward: 1 Quest Point", "Requirements: Druidic Ritual completion, 3 Herblore");
		addEntry("quest_start", "Shilo Village", "Quest Start • Intermediate", 2830, 2985, 0, "Start: Mosol Rei outside Shilo Village gate", "Difficulty: Intermediate • Length: Medium", "Reward: 2 Quest Points", "Requirements: Jungle Potion, 32 Agility, 20 Crafting");
		addEntry("quest_start", "The Queen of Thieves", "Quest Start • Novice", 1483, 3748, 0, "Start: Tomas Lawry in Port Piscarilius", "Difficulty: Novice • Length: Short", "Reward: 1 Quest Point", "Requirements: Client of Kourend, 20 Thieving");
		addEntry("quest_start", "The Ascent of Arceuus", "Quest Start • Intermediate", 1700, 3742, 0, "Start: Mori in Arceuus", "Difficulty: Intermediate • Length: Short", "Reward: 1 Quest Point", "Requirements: Client of Kourend, 12 Hunter");
		addEntry("quest_start", "Tale of the Righteous", "Quest Start • Intermediate", 1512, 3631, 0, "Start: Phileas Rimor in Shayzien library", "Difficulty: Intermediate • Length: Medium", "Reward: 1 Quest Point", "Requirements: Client of Kourend, 16 Strength, 10 Mining");
		addEntry("quest_start", "The Forsaken Tower", "Quest Start • Intermediate", 1847, 3556, 0, "Start: Lady Lovakengj in Lovakengj palace", "Difficulty: Intermediate • Length: Short", "Reward: 1 Quest Point", "Requirements: Client of Kourend, Mining & Smithing puzzles");
		addEntry("quest_start", "Client of Kourend", "Quest Start • Novice", 1824, 3690, 0, "Start: Veos on Port Piscarilius dock", "Difficulty: Novice • Length: Short", "Reward: 1 Quest Point", "Requirements: Feather, Speak to all 5 Great Kourend house leaders");
		addEntry("quest_start", "A Kingdom Divided", "Quest Start • Experienced", 1796, 3782, 0, "Start: Commander Fullore in Kourend Castle", "Difficulty: Experienced • Length: Long", "Reward: 2 Quest Points", "Requirements: All 5 Kourend quests, The Depths of Despair,", "Queen of Thieves, 54 Agility, 52 Thieving, 52 Woodcutting,", "50 Herblore, 42 Mining, 38 Crafting, 35 Magic");
		addEntry("quest_start", "Children of the Sun", "Quest Start • Novice", 3211, 3424, 0, "Start: Noah and Alina in Varrock Square", "Difficulty: Novice • Length: Short", "Reward: 1 Quest Point", "Requirements: None, Unlocks Varlamore access & Quetzal transport");
		addEntry("quest_start", "Twilight's Promise", "Quest Start • Intermediate", 1680, 3130, 0, "Start: Servant Caoimhe in Civitas illa Fortis", "Difficulty: Intermediate • Length: Medium", "Reward: 1 Quest Point", "Requirements: Children of the Sun, Unlocks deeper Varlamore areas");
		addEntry("quest_start", "Perilous Moons", "Quest Start • Master", 1450, 3050, 0, "Start: Neypotzli in Cam Torum", "Difficulty: Master • Length: Medium", "Reward: 2 Quest Points", "Requirements: Twilight's Promise, 48 Slayer, 20 Runecraft,", "20 Construction, 20 Hunter, 20 Fishing, 10 Cooking");
		addEntry("quest_start", "The Ribbiting Tale of a Lily Pad Labour Dispute", "Quest Start • Novice", 1540, 3040, 0, "Start: Frog Citizen in Varlamore quagmire", "Difficulty: Novice • Length: Short", "Reward: 1 Quest Point", "Requirements: 15 Woodcutting, +2,000 Woodcutting XP");
		addEntry("quest_start", "Ethically Acquired Antiquities", "Quest Start • Intermediate", 1390, 3340, 0, "Start: Curator in Hunter Guild / Auburnvale", "Difficulty: Intermediate • Length: Short", "Reward: 1 Quest Point", "Requirements: Children of the Sun, 25 Thieving");
		addEntry("quest_start", "Death on the Isle", "Quest Start • Experienced", 1480, 3210, 0, "Start: Detective in Aldarin", "Difficulty: Experienced • Length: Medium", "Reward: 1 Quest Point", "Requirements: Children of the Sun, 34 Agility, 32 Thieving");
		addEntry("quest_start", "The Heart of Darkness", "Quest Start • Master", 1415, 3180, 0, "Start: Queen Ameyalli in Civitas illa Fortis palace", "Difficulty: Master • Length: Long", "Reward: 2 Quest Points", "Requirements: Twilight's Promise, Perilous Moons, 55 Thieving,", "48 Slayer, 46 Mining, 46 Agility, 46 Crafting");
	}

	public static List<PoiIndex.Poi> getAllPois()
	{
		final List<PoiIndex.Poi> pois = new ArrayList<>(ENTRIES.size());
		for (Entry e : ENTRIES)
		{
			pois.add(new PoiIndex.Poi(e.x, e.y, e.plane, e.type, e.title));
		}
		return Collections.unmodifiableList(pois);
	}

	/**
	 * Looks up rich game details for a given point-of-interest.
	 */
	public static Detail getDetail(PoiIndex.Poi poi, int worldX, int worldY, int plane)
	{
		if (poi == null)
		{
			return getDetailByPosition(worldX, worldY, plane, 6);
		}
		if (poi.getKey() != null && poi.getKey().startsWith("cache_icon_"))
		{
			return new Detail(poi.getName(), "Other map icons", List.of(
				"Unidentified world-map icon from the game cache"
			));
		}

		// 1. Direct exact lookup by coordinate key
		final Entry exact = EXACT_MAP.get(pointKey(worldX, worldY, plane));
		if (exact != null)
		{
			return entryToDetail(exact);
		}

		// 2. Exact match on POI's own world coordinate
		final Entry poiExact = EXACT_MAP.get(pointKey(poi.getX(), poi.getY(), poi.getPlane()));
		if (poiExact != null)
		{
			return entryToDetail(poiExact);
		}

		// 3. Proximity lookup within 12 tiles (chunked; dungeon_link matches dungeon entries)
		final Entry near = nearestEntry(worldX, worldY, plane, 144, poi.getKey());
		if (near != null)
		{
			return entryToDetail(near);
		}

		// 3b. Dungeon icons from the wiki often use dungeon_link while curated entries use dungeon
		if (isDungeonKey(poi.getKey()))
		{
			final Entry dungeonNear = nearestEntry(worldX, worldY, plane, 256, "dungeon");
			if (dungeonNear != null)
			{
				return entryToDetail(dungeonNear);
			}
		}

		return detailFromCategory(poi);
	}

	/**
	 * Last resort when no curated entry matches: guess the detail from the POI's own key and
	 * name. Ordered most specific first - an explicit key wins, then a name keyword, then the
	 * generic "Point of Interest".
	 */
	private static Detail detailFromCategory(PoiIndex.Poi poi)
	{
		final String key = poi.getKey();
		final String name = poi.getName();
		final String lowerName = name.toLowerCase(Locale.ROOT);

		final List<String> dungeonReq = DUNGEON_REQUIREMENTS.get(name);
		if (dungeonReq != null)
		{
			return new Detail(name, "Dungeons", dungeonReq);
		}

		if ("rare_trees".equals(key) || lowerName.contains("tree"))
		{
			return new Detail(name, "Skilling • Woodcutting", List.of(
				"Skill: Woodcutting",
				"Tool needed: Axe (Bronze to Crystal)",
				"Check Woodcutting skill guide for exact level requirement"
			));
		}

		if ("dungeon".equals(key) || "dungeon_link".equals(key)
			|| lowerName.contains("dungeon") || lowerName.contains("cave"))
		{
			final List<String> specific = DUNGEON_REQUIREMENTS.get(name);
			if (specific != null)
			{
				return new Detail(name, "Dungeons", specific);
			}
			return new Detail(name, "Dungeons", List.of(
				"Type: Dungeon entrance / underground link",
				"May require light source, rope, or combat gear"
			));
		}

		if ("fishing_spot".equals(key) || lowerName.contains("fishing"))
		{
			return new Detail(name, "Skilling • Fishing", List.of(
				"Skill: Fishing",
				"Tools: Net, Rod + Bait, Pot, or Harpoon",
				"Check Fishing skill guide for exact bait and level requirements"
			));
		}

		if ("mining_site".equals(key) || lowerName.contains("mine"))
		{
			return new Detail(name, "Skilling • Mining", List.of(
				"Skill: Mining",
				"Tool needed: Pickaxe (Bronze to Crystal)",
				"Check Mining skill guide for ore levels"
			));
		}

		if ("agility_short-cut".equals(key) || lowerName.contains("short-cut") || lowerName.contains("shortcut"))
		{
			return new Detail(name, "Travel", List.of(
				"Agility shortcut: bypass obstacles and travel routes faster",
				"Check Agility skill guide for required level"
			));
		}

		if ("agility_training".equals(key) || lowerName.contains("agility") || lowerName.contains("course"))
		{
			return new Detail(name, "Skilling • Agility", List.of(
				"Skill: Agility",
				"Activity: Obstacle or Rooftop Course",
				"Check Agility skill guide for course level and lap XP"
			));
		}

		if ("hunter_training".equals(key) || lowerName.contains("hunter") || lowerName.contains("hunting"))
		{
			return new Detail(name, "Skilling • Hunter", List.of(
				"Skill: Hunter",
				"Tools: Traps (box/snare/net/pitfall/deadfall), Noose wand, or Butterfly net",
				"Check Hunter skill guide for creature level requirements"
			));
		}

		if ("farming_patch".equals(key) || lowerName.contains("farming"))
		{
			return new Detail(name, "Skilling • Farming", List.of(
				"Skill: Farming",
				"Tools needed: Rake, Spade, Seed dibber, Watering can",
				"Check Farming skill guide for seed levels"
			));
		}

		if ("quest_start".equals(key) || lowerName.contains("quest"))
		{
			return new Detail(name, "Quest Start", List.of(
				"Talk to the quest giver NPC nearby to begin",
				"Check Quest List in-game for full requirement breakdown"
			));
		}

		if (key != null)
		{
			if (key.endsWith("_tutor"))
			{
				return new Detail(name, "Tutors and services", List.of(
					"Free starter items and skill advice"
				));
			}

			switch (key)
			{
				case "thieving":
					return new Detail(name, "Skilling • Thieving", List.of(
						"Pick locked chests or steal from stalls for loot",
						"Check Thieving skill guide for the required level"
					));
				case "bank":
					return new Detail(name, "Banks", List.of(
						"Bank booth or chest: deposit, withdraw and note items"
					));
				case "water_source":
					return new Detail(name, "Skilling", List.of(
						"Fill buckets, watering cans, vials and jugs here"
					));
				case "cooking_range":
					return new Detail(name, "Skilling • Cooking", List.of(
						"Range or fire: cook raw food here",
						"Some ranges reduce the chance to burn food"
					));
				case "anvil":
					return new Detail(name, "Skilling • Smithing", List.of(
						"Smithing: needs a hammer and metal bars"
					));
				case "furnace":
					return new Detail(name, "Skilling • Smithing", List.of(
						"Smelting: turns ore into bars",
						"Also used for gold jewellery with a mould"
					));
				case "altar":
					return new Detail(name, "Altars", List.of(
						"Recharges Prayer points"
					));
				case "house_portal":
					return new Detail(name, "Travel", List.of(
						"Player-owned house portal"
					));
				case "mooring_point":
					return new Detail(name, "Travel", List.of(
						"Ship mooring point"
					));
				case "poll_booth":
					return new Detail(name, "Tutors and services", List.of(
						"Vote in the current poll or read past results"
					));
				case "kourend_task":
					return new Detail(name, "Quests and activities", List.of(
						"Kourend & Kebos Diary task location",
					"Claim diary rewards from Elise at Kourend Castle"
					));
				case "minigame":
					return new Detail(name, "Quests and activities", List.of(
						"Minigame or activity location"
					));
				case "task_master":
					return new Detail(name, "Quests and activities", List.of(
						"Achievement Diary task master: claim tier rewards",
					"Tiers: easy, medium, hard, elite"
					));
				case "dairy_churn":
					return new Detail(name, "Skilling • Cooking", List.of(
						"Churn milk into cream, butter, or cheese"
					));
				case "slayer_master":
					return new Detail(name, "Dungeons", List.of(
						"Assigns Slayer tasks; each master has a combat level requirement",
					"All Slayer Masters also sell Slayer equipment"
					));
				case "potters_wheel":
					return new Detail(name, "Skilling • Crafting", List.of(
						"Shape soft clay into unbaked pottery"
					));
				case "spinning_wheel":
					return new Detail(name, "Skilling • Crafting", List.of(
						"Spin wool into balls of wool or flax into bowstrings"
					));
				case "tannery":
					return new Detail(name, "Skilling • Crafting", List.of(
						"Tan cowhides and dragonhides into leather for a small fee"
					));
				case "sawmill":
					return new Detail(name, "Skilling • Construction", List.of(
						"Converts logs into planks for Construction"
					));
				case "windmill":
					return new Detail(name, "Skilling • Cooking", List.of(
						"Grind grain into flour using the hopper and millstones"
					));
				case "loom":
					return new Detail(name, "Skilling • Crafting", List.of(
						"Weave jute, flax, or willow branches into cloth and sacks"
					));
				case "sandpit":
					return new Detail(name, "Skilling", List.of(
						"Fill empty buckets with sand for Crafting molten glass"
					));
				case "brewery":
					return new Detail(name, "Skilling • Cooking", List.of(
						"Brew ales and ciders using a brewing vat"
					));
				case "raids_lobby":
					return new Detail(name, "Quests and activities", List.of(
						"Raids lobby: form a party and prepare for raid encounters"
					));
				case "region_label":
					return new Detail(name, "Places", List.of(
						"Named place from the game map"
					));
			}
		}

		return new Detail(name, "Point of Interest", Collections.emptyList());
	}

	public static Detail getDetailByPosition(int worldX, int worldY, int plane, int radius)
	{
		// Exact coordinate match
		final Entry exact = EXACT_MAP.get(pointKey(worldX, worldY, plane));
		if (exact != null)
		{
			return entryToDetail(exact);
		}

		final Entry best = nearestEntry(worldX, worldY, plane, (radius + 2) * (radius + 2), null);
		if (best != null)
		{
			return entryToDetail(best);
		}

		return null;
	}

	/**
	 * Nearest curated entry within {@code maxDistSq} of (x, y) on {@code plane}.
	 *
	 * <p>When {@code typeHint} is non-null, an entry is accepted only if its type matches the hint
	 * or it is within 4 tiles ({@code dist <= 16}). Uses strict {@code <} for ties so the first
	 * visited entry wins (chunk order), matching the spatial-index parity tests.
	 */
	static Entry nearestEntry(int worldX, int worldY, int plane, int maxDistSq, String typeHint)
	{
		final int radius = (int) Math.ceil(Math.sqrt(maxDistSq));
		final int minChunkX = (worldX - radius) >> 6;
		final int maxChunkX = (worldX + radius) >> 6;
		final int minChunkY = (worldY - radius) >> 6;
		final int maxChunkY = (worldY + radius) >> 6;

		Entry best = null;
		int bestDist = maxDistSq;

		for (int cx = minChunkX; cx <= maxChunkX; cx++)
		{
			for (int cy = minChunkY; cy <= maxChunkY; cy++)
			{
				final List<Entry> chunkEntries = CHUNK_MAP.get(chunkKey(plane, cx, cy));
				if (chunkEntries == null)
				{
					continue;
				}

				for (Entry e : chunkEntries)
				{
					final int dx = e.x - worldX;
					final int dy = e.y - worldY;
					final int dist = dx * dx + dy * dy;
					if (dist >= bestDist)
					{
						continue;
					}
					if (typeHint != null && !typeMatches(e.type, typeHint) && dist > 16)
					{
						continue;
					}
					bestDist = dist;
					best = e;
				}
			}
		}

		return best;
	}

	/** Linear scan kept for the spatial-index parity test. Same acceptance rule as {@link #nearestEntry}. */
	static Entry nearestEntryLinear(int worldX, int worldY, int plane, int maxDistSq, String typeHint)
	{
		Entry best = null;
		int bestDist = maxDistSq;
		for (Entry e : ENTRIES)
		{
			if (e.plane != plane)
			{
				continue;
			}
			final int dx = e.x - worldX;
			final int dy = e.y - worldY;
			final int dist = dx * dx + dy * dy;
			if (dist >= bestDist)
			{
				continue;
			}
			if (typeHint != null && !typeMatches(e.type, typeHint) && dist > 16)
			{
				continue;
			}
			bestDist = dist;
			best = e;
		}
		return best;
	}
}
