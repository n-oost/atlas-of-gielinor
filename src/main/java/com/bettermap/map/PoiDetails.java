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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

/**
 * Curated details for points of interest: quest names, difficulty and starting NPCs, rare trees
 * (Varlamore camphor and jatoba among them), dungeons, skilling level requirements, and the tools
 * a spot needs.
 */
@Slf4j
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

	private static final class Indexes
	{
		private final List<Entry> entries = new ArrayList<>(1200);
		private final Set<String> entryKeys = new HashSet<>(1200);
		private final Map<Long, Entry> exactMap = new HashMap<>(1200);
		private final Map<Long, List<Entry>> chunkMap = new HashMap<>(1200);
		private Map<String, Detail> questDetails = Collections.emptyMap();
		private boolean loaded;
	}

	private static volatile Indexes data = new Indexes();

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
			"Requires: Dusty key (from Velrak in Taverley jail) or 70 Agility shortcut",
			"Light source recommended"));
		reqs.put("Edgeville Dungeon", List.of(
			"Requires: Brass key for the surface shortcut door south of Edgeville",
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
			"Kolodion's Mage Arena and god cape arena",
			"Teleport lever connects surface and underground bank"));
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
			"Requires: 60 Woodcutting",
			"Ents and Woodcutting dungeon below"));
		reqs.put("Woodcutting Guild Dungeon", List.of(
			"Requires: 60 Woodcutting",
			"Ents and Woodcutting dungeon below"));
		reqs.put("WC Guild dungeon", List.of(
			"Requires: 60 Woodcutting",
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

	private static boolean isTravelKey(String key)
	{
		return "transportation".equals(key) || "canoe_station".equals(key)
			|| "hot_air_balloon".equals(key) || "magic_mushtree".equals(key)
			|| "magic_carpet".equals(key) || "minecart_network".equals(key);
	}

	private static boolean typeMatches(String entryType, String typeHint)
	{
		if (typeHint == null || typeHint.equals(entryType))
		{
			return true;
		}
		if (isDungeonKey(typeHint) && isDungeonKey(entryType))
		{
			return true;
		}
		return isTravelKey(typeHint) && isTravelKey(entryType);
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

	private static void addEntry(Indexes prepared, String type, String title, String category, int x, int y, int plane, String... lines)
	{
		// Basements and cellars use the link marker, not the red dungeon marker.
		if ("dungeon".equals(type) && (title.toLowerCase(Locale.ROOT).contains("basement")
			|| title.toLowerCase(Locale.ROOT).contains("cellar")))
		{
			type = "basement";
		}
		final String key = type + '\u0000' + title + '\u0000' + pointKey(x, y, plane);
		if (!prepared.entryKeys.add(key))
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
		prepared.entries.add(entry);
		prepared.exactMap.put(pointKey(x, y, plane), entry);
		prepared.chunkMap.computeIfAbsent(chunkKey(plane, x >> 6, y >> 6), k -> new ArrayList<>()).add(entry);
	}

	/** Load on the POI startup worker; publish only the complete set of indexes. */
	public static void load()
	{
		if (data.loaded)
		{
			return;
		}
		final Indexes prepared = new Indexes();
		try (InputStream stream = PoiDetails.class.getResourceAsStream(
			"/com/bettermap/poi/curated-details.tsv"))
		{
			if (stream == null)
			{
				throw new IOException("Missing curated POI details resource");
			}
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)))
			{
				String line;
				int lineNumber = 0;
				while ((line = reader.readLine()) != null)
				{
					lineNumber++;
					if (Thread.currentThread().isInterrupted())
					{
						return;
					}
					if (line.isEmpty() || line.charAt(0) == '#')
					{
						continue;
					}
					final String[] fields = line.split("\\t", -1);
					if (fields.length < 6)
					{
						throw new IOException("Invalid curated POI details at line " + lineNumber);
					}
					addEntry(prepared, fields[0], fields[1], fields[2],
						Integer.parseInt(fields[3]), Integer.parseInt(fields[4]), Integer.parseInt(fields[5]),
						Arrays.copyOfRange(fields, 6, fields.length));
				}
			}
			if (Thread.currentThread().isInterrupted())
			{
				return;
			}
			prepared.questDetails = QuestDetailsData.load();
			if (Thread.currentThread().isInterrupted())
			{
				return;
			}
			prepared.loaded = true;
			data = prepared;
		}
		catch (IOException | IllegalArgumentException e)
		{
			log.debug("Could not load curated POI details", e);
		}
	}

	public static List<PoiIndex.Poi> getAllPois()
	{
		final List<PoiIndex.Poi> pois = new ArrayList<>(data.entries.size());
		for (Entry e : data.entries)
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
		final Detail nativeDetail = getNativeDetail(poi, worldX, worldY, plane);
		if (poi == null || "mooring_point".equals(poi.getKey()))
		{
			return nativeDetail;
		}
		final Detail imported = WorldMapSupplement.detail(poi);
		if (imported == null || nativeDetail == null)
		{
			return nativeDetail == null ? imported : nativeDetail;
		}
		final List<String> lines = new ArrayList<>(nativeDetail.getLines());
		for (String line : imported.getLines())
		{
			if (!lines.contains(line) && !line.equals(nativeDetail.getTitle()))
			{
				lines.add(line);
			}
		}
		return new Detail(nativeDetail.getTitle(), nativeDetail.getCategory(), lines);
	}

	private static Detail getNativeDetail(PoiIndex.Poi poi, int worldX, int worldY, int plane)
	{
		if (poi == null)
		{
			return getDetailByPosition(worldX, worldY, plane, 6);
		}

		// Mooring markers can share a tile with curated island activities. Their cache marker
		// identity is the dock; an exact-coordinate detail for a mine, cave, or skilling spot
		// must not replace the mooring tooltip.
		if ("mooring_point".equals(poi.getKey()))
		{
			return detailFromCategory(poi);
		}

		// 1. Direct exact lookup by coordinate key (matching marker type)
		final Entry exact = data.exactMap.get(pointKey(worldX, worldY, plane));
		if (exact != null && typeMatches(exact.type, poi.getKey()))
		{
			return entryToDetail(exact);
		}

		// 2. Exact match on POI's own world coordinate (matching marker type)
		final Entry poiExact = data.exactMap.get(pointKey(poi.getX(), poi.getY(), poi.getPlane()));
		if (poiExact != null && typeMatches(poiExact.type, poi.getKey()))
		{
			return entryToDetail(poiExact);
		}

		if ("quest_start".equals(poi.getKey()))
		{
			final Detail questDetail = data.questDetails.get(poi.getName());
			if (questDetail != null)
			{
				return questDetail;
			}
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
		final Detail imported = "mooring_point".equals(poi.getKey()) ? null : WorldMapSupplement.detail(poi);
		if (imported != null)
		{
			return imported;
		}
		final String key = poi.getKey();
		final String name = poi.getName();
		final String lowerName = name.toLowerCase(Locale.ROOT);

		final List<String> dungeonReq = DUNGEON_REQUIREMENTS.get(name);
		if (dungeonReq != null)
		{
			return new Detail(name, "Dungeons", dungeonReq);
		}

		if ("rare_trees".equals(key) || (lowerName.contains("tree") && !lowerName.contains("mushtree")))
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

		if ("mining_site".equals(key) || (lowerName.contains("mine") && !lowerName.contains("minecart")))
		{
			return new Detail(name, "Skilling • Mining", List.of(
				"Skill: Mining",
				"Tool needed: Pickaxe (Bronze to Crystal)",
				"Check Mining skill guide for ore levels"
			));
		}

		if ("agility_short-cut".equals(key) || lowerName.contains("short-cut") || lowerName.contains("shortcut"))
		{
			return new Detail(name, "Agility shortcuts", List.of(
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
			final Detail questDetail = data.questDetails.get(name);
			if (questDetail != null)
			{
				return questDetail;
			}
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
					if ("Imbued altar".equalsIgnoreCase(name))
					{
						return new Detail(name, "Altars", List.of(
							"Converts refined tephra into imbued tephra during the Zalcano fight"
						));
					}
					return new Detail(name, "Altars", List.of(
						"Recharges Prayer points"
					));
				case "house_portal":
					return new Detail(name, "Travel", List.of(
						"Player-owned house portal"
					));
				case "mooring_point":
					return mooringDetail(poi, name);
				case "salvaging":
					return new Detail(name, "Skilling • Sailing", List.of(
						"Salvage shipwrecks using a salvaging hook installed on your vessel"
					));
				case "shipwright":
					return new Detail(name, "Shops and trade", List.of(
						"Shipyard services: boat retrieval, ship customization, and vessel upgrades"
					));
				case "cargo_bay":
					return new Detail(name, "Tutors and services", List.of(
						"Port cargo loading bay and shipwreck salvage sorting"
					));
				case "noticeboard":
					if (name.toLowerCase().contains("port task"))
					{
						return new Detail(name, "Noticeboard", List.of(
							"Courier deliveries and maritime bounty contracts"
						));
					}
					return new Detail(name, "Noticeboard", List.of(
						"View local announcements, activities, and task notices"
					));
				case "lookout_point":
					return new Detail(name, "Sailing", List.of(
						"High vantage point for coastal, sea, and territory observation"
					));
				case "singing_bowl":
					return new Detail(name, "Skilling • Crafting", List.of(
						"Sing crystal equipment, tools, and armor using crystal shards",
						"Requires Song of the Elves to access Prifddinas singing bowls"
					));
				case "canoe_station":
					return new Detail(name, "Travel", List.of(
						"River transportation via dugout, canoe, or waka",
						"Craft a canoe using an axe on the fallen tree station"
					));
				case "hot_air_balloon":
					return new Detail(name, "Travel", List.of(
						"Hot air balloon transport network",
						"Requires logs to travel (Willow, Yew, Magic)",
						"Requires Enlightened Journey quest"
					));
				case "magic_mushtree":
					return new Detail(name, "Travel", List.of(
						"Fossil Island rapid travel network between key locations",
						"Requires Bone Voyage quest"
					));
				case "magic_carpet":
					return new Detail(name, "Travel", List.of(
						"Desert and regional carpet transportation",
						"Talk to the Rug merchant to travel (200 coins)"
					));
				case "minecart_network":
					return new Detail(name, "Travel", List.of(
						"Great Kourend minecart transit system",
						"Requires minecart control scroll or 50% Lovakengj favour"
					));
				case "sea_current":
					return new Detail(name, "Travel", List.of(
						"Ocean speed current",
						"Increases vessel speed when traveling in the current's direction"
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
					return new Detail(name, "Skilling • Slayer", List.of(
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
		final Entry exact = data.exactMap.get(pointKey(worldX, worldY, plane));
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
				final List<Entry> chunkEntries = data.chunkMap.get(chunkKey(plane, cx, cy));
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
					if (typeHint != null && !typeMatches(e.type, typeHint))
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
		for (Entry e : data.entries)
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
			if (typeHint != null && !typeMatches(e.type, typeHint))
			{
				continue;
			}
			bestDist = dist;
			best = e;
		}
		return best;
	}

	private static final int[][] MOORINGS = {
		{3050, 3192, 1}, {3069, 2986, 1}, {1506, 3402, 5}, {1726, 3452, 5},
		{2960, 3147, 10}, {1845, 3687, 15}, {2905, 3226, 18}, {2796, 3412, 20},
		{2757, 3229, 25}, {2671, 3265, 28}, {2685, 3161, 30}, {2746, 3304, 34},
		{2878, 3335, 36}, {1774, 3141, 38}, {2579, 2843, 40}, {3061, 2639, 40},
		{2749, 2951, 42}, {1892, 3429, 42}, {1511, 2975, 44}, {2971, 2603, 45},
		{3174, 2367, 45}, {3354, 2216, 45}, {1452, 2970, 46}, {1872, 2985, 46},
		{2997, 2288, 47}, {3143, 2824, 48}, {1557, 2771, 49}, {2651, 2678, 50},
		{1860, 3306, 50}, {2467, 2721, 51}, {2848, 2327, 52}, {2808, 2510, 52},
		{1958, 3117, 54}, {2282, 2823, 55}, {2532, 2531, 56}, {1202, 2733, 58},
		{2660, 2395, 60}, {2318, 2774, 61}, {2630, 3705, 62}, {2567, 2297, 62},
		{2773, 8607, 62}, {2058, 2606, 63}, {1765, 2659, 64}, {2611, 3840, 65},
		{2144, 3120, 66}, {2097, 3188, 66}, {1923, 2758, 67}, {2412, 3780, 68},
		{2308, 3783, 68}, {2344, 2270, 69}, {2158, 3324, 70}, {2189, 2327, 72},
		{2222, 3466, 73}, {2543, 3765, 74}, {2303, 3690, 75}, {2151, 3880, 76},
		{2080, 3690, 76}, {2150, 3530, 79}, {2860, 3972, 80}, {1954, 4056, 81},
		{2927, 4056, 87}
	};

	private static int getMooringLevel(int x, int y)
	{
		int bestDist = 30 * 30;
		int bestLvl = 0;
		for (int[] m : MOORINGS)
		{
			int dx = m[0] - x;
			int dy = m[1] - y;
			int dist = dx * dx + dy * dy;
			if (dist <= bestDist)
			{
				bestDist = dist;
				bestLvl = m[2];
			}
		}
		return bestLvl;
	}

	private static Detail mooringDetail(PoiIndex.Poi poi, String name)
	{
		if ("Mooring buoy".equalsIgnoreCase(name))
		{
			return new Detail(name, "Travel", List.of(
				"Off-shore mooring buoy for docking player vessels",
				"Requires Sailing to dock"
			));
		}
		if ("Ship boarding plank".equalsIgnoreCase(name))
		{
			return new Detail(name, "Travel", List.of(
				"Port dock for boarding and disembarking player ships"
			));
		}
		final int level = poi != null ? getMooringLevel(poi.getX(), poi.getY()) : 0;
		if ("Mooring point - The Summer Shore".equalsIgnoreCase(name))
		{
			final List<String> lines = new ArrayList<>();
			lines.add("Mooring point for docking and disembarking player vessels");
			if (level > 0)
			{
				lines.add("Requires Level " + level + " Sailing");
			}
			lines.add("Requires completion of Troubled Tortugans");
			return new Detail(name, "Travel", lines);
		}
		if (level > 0)
		{
			return new Detail(name, "Travel", List.of(
				"Mooring point for docking and disembarking player vessels",
				"Requires Level " + level + " Sailing"
			));
		}
		return new Detail(name, "Travel", List.of(
			"Mooring point for docking and disembarking player vessels"
		));
	}
}
