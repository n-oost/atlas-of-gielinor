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
package com.bettermap.data.sailing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

/**
 * Curated data for Port Notice Boards (task boards) across major Sailing ports in Old School RuneScape.
 * Provides approximate board coordinates, typical sample courier routes, and sample bounty targets.
 *
 * <p>Notice boards in-game rotate tasks after 8 completions or daily reset; these lists represent
 * curated static samples of typical tasks available from each port's board.
 */
@Getter
public class PortNoticeBoard
{
	private final SailingPort port;
	private final String name;
	private final WorldPoint location;
	private final List<String> courierTasks;
	private final List<String> bountyTasks;
	private final int minSailingLevel;

	public PortNoticeBoard(
		SailingPort port,
		String name,
		WorldPoint location,
		List<String> courierTasks,
		List<String> bountyTasks,
		int minSailingLevel)
	{
		this.port = port;
		this.name = name;
		this.location = location;
		this.courierTasks = courierTasks != null ? Collections.unmodifiableList(new ArrayList<>(courierTasks)) : Collections.emptyList();
		this.bountyTasks = bountyTasks != null ? Collections.unmodifiableList(new ArrayList<>(bountyTasks)) : Collections.emptyList();
		this.minSailingLevel = minSailingLevel;
	}

	public boolean hasBounties()
	{
		return !bountyTasks.isEmpty();
	}

	public static final List<PortNoticeBoard> ALL_BOARDS;
	private static final Map<SailingPort, PortNoticeBoard> BY_PORT = new HashMap<>();

	static
	{
		final List<PortNoticeBoard> boards = new ArrayList<>();

		// 1. Port Sarim
		boards.add(new PortNoticeBoard(
			SailingPort.PORT_SARIM,
			"Notice board — Port Sarim",
			new WorldPoint(3045, 3205, 0),
			List.of(
				"→ Musa Point (Rum shipment)",
				"→ Entrana (Timber & building supplies)",
				"→ Port Khazard (Iron & steel ingots)",
				"→ Catherby (Farming produce & seeds)",
				"→ The Pandemonium (Training cargo)",
				"→ Land's End (Colony supply crates)"
			),
			List.of(
				"Sea Serpents (Lvl 30)",
				"Stray Corsair Pirates (Lvl 30)",
				"Reef Sharks (Lvl 35)"
			),
			1
		));

		// 2. The Pandemonium
		boards.add(new PortNoticeBoard(
			SailingPort.THE_PANDEMONIUM,
			"Notice board — The Pandemonium",
			new WorldPoint(3065, 2992, 0),
			List.of(
				"→ Port Sarim (Salvage & training cargo)",
				"→ Musa Point (Navigational charts)",
				"→ Catherby (Fresh catch delivery)",
				"→ Ruins of Unkah (Desert expedition gear)",
				"→ The Summer Shore (Tropical provisions)"
			),
			List.of(
				"Siren Scouts (Lvl 30)",
				"Rogue Privateers (Lvl 32)",
				"Sea Scorpions (Lvl 38)"
			),
			1
		));

		// 3. Catherby
		boards.add(new PortNoticeBoard(
			SailingPort.CATHERBY,
			"Notice board — Catherby",
			new WorldPoint(2802, 3422, 0),
			List.of(
				"→ Port Sarim (Fresh sea fish crates)",
				"→ Port Khazard (Farming supplies)",
				"→ Rellekka (Fishing tackle & nets)",
				"→ East Ardougne (Flax & textiles)",
				"→ Entrana (Herbal medicines)"
			),
			List.of(
				"Giant Squid Tentacles (Lvl 30)",
				"Coastal Wyrms (Lvl 40)",
				"Siren Sirens (Lvl 45)"
			),
			20
		));

		// 4. Port Khazard
		boards.add(new PortNoticeBoard(
			SailingPort.PORT_KHAZARD,
			"Notice board — Port Khazard",
			new WorldPoint(2680, 3168, 0),
			List.of(
				"→ Port Sarim (Khazard refined iron)",
				"→ Brimhaven (Shipwright timber)",
				"→ Catherby (Ardougne woven fabrics)",
				"→ Void Outpost (Battlefield armaments)",
				"→ Port Roberts (Quarry freight)"
			),
			List.of(
				"Khazard Privateers (Lvl 30)",
				"Sea Horrors (Lvl 42)",
				"Deepfin Raiders (Lvl 50)"
			),
			30
		));

		// 5. Port Piscarillius
		boards.add(new PortNoticeBoard(
			SailingPort.PORT_PISCARILLIUS,
			"Notice board — Port Piscarillius",
			new WorldPoint(1836, 3694, 0),
			List.of(
				"→ Land's End (Piscarillius fresh fish)",
				"→ Port Sarim (Kourend timber)",
				"→ Hosidius (Salted fish casks)",
				"→ Port Roberts (Charcoal cargo)",
				"→ Civitas illa Fortis (Archaic relics)"
			),
			List.of(
				"Angler Fiends (Lvl 30)",
				"Zombie Pirates (Lvl 36)",
				"Abyssal Sirens (Lvl 48)"
			),
			15
		));

		// 6. Port Roberts
		boards.add(new PortNoticeBoard(
			SailingPort.PORT_ROBERTS,
			"Notice board — Port Roberts",
			new WorldPoint(1866, 3312, 0),
			List.of(
				"→ Sunset Coast (Quarry stone)",
				"→ Port Piscarillius (Naval timber)",
				"→ Port Khazard (Varlamore vintage)",
				"→ Land's End (Grain supplies)",
				"→ Civitas illa Fortis (Trade shipments)"
			),
			List.of(
				"Smuggler Skiffs (Lvl 50)",
				"Sea Drakes (Lvl 54)",
				"Coral Golems (Lvl 58)"
			),
			50
		));

		// 7. Port Tyras
		boards.add(new PortNoticeBoard(
			SailingPort.PORT_TYRAS,
			"Notice board — Port Tyras",
			new WorldPoint(2148, 3128, 0),
			List.of(
				"→ Port Sarim (Catapult components)",
				"→ Port Khazard (Elven timber)",
				"→ Port Roberts (Sulphur casks)",
				"→ Prifddinas (Tar & resin supplies)"
			),
			List.of(
				"Tyras Mutineers (Lvl 66)",
				"Toxic Krakens (Lvl 68)",
				"Sea Serpents (Lvl 70)"
			),
			66
		));

		// 8. Rellekka
		boards.add(new PortNoticeBoard(
			SailingPort.RELLEKKA,
			"Notice board — Rellekka",
			new WorldPoint(2636, 3712, 0),
			List.of(
				"→ Catherby (Fremennik furs & pelts)",
				"→ Jatizso (Yak hide & cured meat)",
				"→ Neitiznot (Refined metal ingots)",
				"→ Waterbirth Island (Brewing kegs)",
				"→ Weiss (Sea salt crates)"
			),
			List.of(
				"Sea Trolls (Lvl 62)",
				"Frost Leviathans (Lvl 65)",
				"Dagannoth Fledglings (Lvl 70)"
			),
			62
		));

		// 9. Land's End
		boards.add(new PortNoticeBoard(
			SailingPort.LANDS_END,
			"Notice board — Land's End",
			new WorldPoint(1510, 3408, 0),
			List.of(
				"→ Port Sarim (Kourend grain)",
				"→ Port Piscarillius (Timber crates)",
				"→ Port Roberts (Farm equipment)",
				"→ Catherby (Wool bales)"
			),
			Collections.emptyList(),
			5
		));

		// 10. Brimhaven
		boards.add(new PortNoticeBoard(
			SailingPort.BRIMHAVEN,
			"Notice board — Brimhaven",
			new WorldPoint(2764, 3236, 0),
			List.of(
				"→ Port Khazard (Karamja rum & fruit)",
				"→ East Ardougne (Tropical hardwoods)",
				"→ Port Sarim (Gold ore shipments)",
				"→ Catherby (Exotic spices)"
			),
			List.of(
				"Corsair Marauders (Lvl 30)",
				"Jungle Krakens (Lvl 45)"
			),
			25
		));

		// 11. Civitas illa Fortis
		boards.add(new PortNoticeBoard(
			SailingPort.CIVITAS_ILLA_FORTIS,
			"Notice board — Civitas illa Fortis",
			new WorldPoint(1768, 3146, 0),
			List.of(
				"→ Sunset Coast (Sun-blessed wine)",
				"→ Aldarin (Colosseum rations)",
				"→ Port Roberts (Fine pottery)",
				"→ Port Sarim (Varlamore marble)"
			),
			List.of(
				"Varlamore Smugglers (Lvl 38)",
				"Solar Rays (Lvl 42)",
				"Coral Wyrms (Lvl 46)"
			),
			38
		));

		// 12. Sunset Coast
		boards.add(new PortNoticeBoard(
			SailingPort.SUNSET_COAST,
			"Notice board — Sunset Coast",
			new WorldPoint(1516, 2982, 0),
			List.of(
				"→ Civitas illa Fortis (Teak & mahogany logs)",
				"→ Port Sarim (Cultured pearls)",
				"→ Aldarin (Deep-sea tackle)",
				"→ Port Roberts (Coastal provisions)"
			),
			List.of(
				"Sunbleak Marauders (Lvl 44)",
				"Reef Wyrms (Lvl 48)",
				"Corsair Raiders (Lvl 52)"
			),
			44
		));

		// 13. Aldarin
		boards.add(new PortNoticeBoard(
			SailingPort.ALDARIN,
			"Notice board — Aldarin",
			new WorldPoint(1458, 2976, 0),
			List.of(
				"→ Civitas illa Fortis (Aldarin vintage)",
				"→ Sunset Coast (Herbal medicine)",
				"→ Port Khazard (Olive oil casks)",
				"→ Port Roberts (Fine linen)"
			),
			List.of(
				"Aldarin Corsairs (Lvl 46)",
				"Deepfin Scouts (Lvl 50)"
			),
			46
		));

		// 14. Void Knights' Outpost
		boards.add(new PortNoticeBoard(
			SailingPort.VOID_KNIGHTS_OUTPOST,
			"Notice board — Void Knights' Outpost",
			new WorldPoint(2656, 2684, 0),
			List.of(
				"→ Port Sarim (Pest trophy samples)",
				"→ Port Khazard (Combat rations)",
				"→ Land's End (Knight equipment)",
				"→ Brimhaven (Medicinal herbs)"
			),
			List.of(
				"Pest Broodmothers (Lvl 50)",
				"Sea Defilers (Lvl 54)"
			),
			50
		));

		// 15. Prifddinas
		boards.add(new PortNoticeBoard(
			SailingPort.PRIFDDINAS,
			"Notice board — Prifddinas",
			new WorldPoint(2164, 3330, 0),
			List.of(
				"→ Port Tyras (Crystal glassware)",
				"→ Catherby (Elven spun silk)",
				"→ Port Sarim (Llydda timber)"
			),
			List.of(
				"Corrupted Sea Serpents (Lvl 70)",
				"Crystal Krakens (Lvl 74)"
			),
			70
		));

		// 16. Jatizso
		boards.add(new PortNoticeBoard(
			SailingPort.JATIZSO,
			"Notice board — Jatizso",
			new WorldPoint(2416, 3786, 0),
			List.of(
				"→ Rellekka (Jatizso raw ore)",
				"→ Neitiznot (Smelted bronze & iron)",
				"→ Etceteria (Ice sea cod)"
			),
			List.of(
				"Ice Trolls (Lvl 68)",
				"Frost Drakes (Lvl 72)"
			),
			68
		));

		// 17. Neitiznot
		boards.add(new PortNoticeBoard(
			SailingPort.NEITIZNOT,
			"Notice board — Neitiznot",
			new WorldPoint(2314, 3788, 0),
			List.of(
				"→ Rellekka (Yak milk & cured hides)",
				"→ Jatizso (Carved wood & tools)",
				"→ Etceteria (Rope & sailcloth)"
			),
			List.of(
				"Sea Wolves (Lvl 68)",
				"Arctic Krakens (Lvl 72)"
			),
			68
		));

		// 18. Etceteria
		boards.add(new PortNoticeBoard(
			SailingPort.ETCETERIA,
			"Notice board — Etceteria",
			new WorldPoint(2616, 3846, 0),
			List.of(
				"→ Rellekka (Royal coal shipments)",
				"→ Catherby (Maple logs & syrup)",
				"→ Jatizso (Salted fish barrels)"
			),
			List.of(
				"Corsair Deserters (Lvl 65)",
				"Sea Scorpions (Lvl 68)"
			),
			65
		));

		// 19. Weiss
		boards.add(new PortNoticeBoard(
			SailingPort.WEISS,
			"Notice board — Weiss",
			new WorldPoint(2855, 3968, 0),
			List.of(
				"→ Rellekka (Basalt quarry blocks)",
				"→ Port Sarim (Refined salt crystals)",
				"→ Jatizso (Thick arctic pelts)"
			),
			List.of(
				"Frost Sirens (Lvl 80)",
				"Ice Krakens (Lvl 82)"
			),
			80
		));

		// 20. Musa Point
		boards.add(new PortNoticeBoard(
			SailingPort.MUSA_POINT,
			"Notice board — Musa Point",
			new WorldPoint(2955, 3153, 0),
			List.of(
				"→ Port Sarim (Karamja bananas & rum)",
				"→ Brimhaven (Tropical timber)",
				"→ Catherby (Fresh catch crates)"
			),
			Collections.emptyList(),
			10
		));

		// 21. Entrana
		boards.add(new PortNoticeBoard(
			SailingPort.ENTRANA,
			"Notice board — Entrana",
			new WorldPoint(2872, 3340, 0),
			List.of(
				"→ Port Sarim (Blessed water & herbs)",
				"→ Catherby (Monks' woven cloth)",
				"→ Port Khazard (Pilgrim supplies)"
			),
			List.of(
				"Corrupted Sea Shades (Lvl 36)",
				"Pirate Raiders (Lvl 38)"
			),
			36
		));

		// 22. Corsair Cove
		boards.add(new PortNoticeBoard(
			SailingPort.CORSAIR_COVE,
			"Notice board — Corsair Cove",
			new WorldPoint(2584, 2849, 0),
			List.of(
				"→ Port Sarim (Spices & silks)",
				"→ Port Khazard (Corsair timber)",
				"→ Brimhaven (Smuggler rum)"
			),
			List.of(
				"Corsair Marauders (Lvl 40)",
				"Reef Stalkers (Lvl 44)"
			),
			40
		));

		// 23. Isle of Souls
		boards.add(new PortNoticeBoard(
			SailingPort.ISLE_OF_SOULS,
			"Notice board — Isle of Souls",
			new WorldPoint(2288, 2830, 0),
			List.of(
				"→ Port Khazard (Soul relics)",
				"→ Land's End (Ancient stone)",
				"→ Port Roberts (Archeology crates)"
			),
			List.of(
				"Soulfiend Corsairs (Lvl 55)",
				"Phantom Galleons (Lvl 60)"
			),
			55
		));

		// 24. Piscatoris
		boards.add(new PortNoticeBoard(
			SailingPort.PISCATORIS,
			"Notice board — Piscatoris",
			new WorldPoint(2310, 3695, 0),
			List.of(
				"→ Port Sarim (Fresh monkfish crates)",
				"→ Catherby (Fishing nets & floats)",
				"→ Port Khazard (Colony supplies)"
			),
			List.of(
				"Sea Terrors (Lvl 75)",
				"Deep Kraken Spawn (Lvl 78)"
			),
			75
		));

		// 25. Lunar Isle
		boards.add(new PortNoticeBoard(
			SailingPort.LUNAR_ISLE,
			"Notice board — Lunar Isle",
			new WorldPoint(2145, 3885, 0),
			List.of(
				"→ Rellekka (Astral rune essence)",
				"→ Port Sarim (Dream vials & herbs)",
				"→ Catherby (Moonclan fabrics)"
			),
			List.of(
				"Astral Corsairs (Lvl 76)",
				"Dream Krakens (Lvl 80)"
			),
			76
		));

		ALL_BOARDS = Collections.unmodifiableList(boards);

		for (PortNoticeBoard board : ALL_BOARDS)
		{
			if (board.getPort() != null)
			{
				BY_PORT.put(board.getPort(), board);
			}
		}
	}

	public static PortNoticeBoard forPort(SailingPort port)
	{
		return BY_PORT.get(port);
	}

	/**
	 * Finds the nearest port notice board to a given world coordinate within maxRadius tiles.
	 */
	public static PortNoticeBoard findBoardNear(int worldX, int worldY, int plane, int maxRadius)
	{
		if (plane != 0)
		{
			return null;
		}

		PortNoticeBoard best = null;
		int bestDistSq = maxRadius * maxRadius;

		for (PortNoticeBoard board : ALL_BOARDS)
		{
			final WorldPoint loc = board.getLocation();
			if (loc == null || loc.getPlane() != plane)
			{
				continue;
			}

			final int dx = loc.getX() - worldX;
			final int dy = loc.getY() - worldY;
			if (Math.abs(dx) > maxRadius || Math.abs(dy) > maxRadius)
			{
				continue;
			}
			final int distSq = dx * dx + dy * dy;

			if (distSq <= bestDistSq)
			{
				bestDistSq = distSq;
				best = board;
			}
		}

		return best;
	}
}
