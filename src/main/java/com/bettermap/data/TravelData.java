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
package com.bettermap.data;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.runelite.api.coords.WorldPoint;

/**
 * The fixed travel networks: charter ships, passenger ferries, gnome gliders, spirit trees,
 * fairy rings, hot air balloons, canoes, mine carts, magic carpets, quetzals and swamp boats,
 * with their world coordinates, destinations, prices and requirements.
 */
public final class TravelData
{
	public enum TravelType
	{
		CHARTER_SHIP("Charter Ship", new Color(255, 180, 50), new Color(255, 210, 100)),
		FERRY("Passenger Ferry", new Color(70, 160, 245), new Color(130, 200, 255)),
		GNOME_GLIDER("Gnome Glider", new Color(40, 210, 180), new Color(100, 240, 215)),
		SPIRIT_TREE("Spirit Tree", new Color(50, 205, 100), new Color(120, 240, 150)),
		FAIRY_RING("Fairy Ring", new Color(190, 110, 245), new Color(220, 160, 255)),
		HOT_AIR_BALLOON("Hot Air Balloon", new Color(255, 110, 80), new Color(255, 160, 130)),
		CANOE("Canoe Station", new Color(180, 130, 70), new Color(220, 175, 115)),
		MINE_CART("Mine Cart & Rail", new Color(210, 140, 90), new Color(240, 180, 130)),
		MAGIC_CARPET("Magic Carpet", new Color(245, 215, 60), new Color(255, 235, 120)),
		QUETZAL("Quetzal Transport", new Color(240, 100, 140), new Color(255, 150, 180)),
		SWAMP_BOAT("Swamp & River Boat", new Color(120, 180, 90), new Color(170, 220, 140));

		private final String displayName;
		private final Color primaryColor;
		private final Color highlightColor;
		private final Color pinColor;

		TravelType(String displayName, Color primaryColor, Color highlightColor)
		{
			this.displayName = displayName;
			this.primaryColor = primaryColor;
			this.highlightColor = highlightColor;
			this.pinColor = new Color(
				Math.min(255, (int) (primaryColor.getRed() * 1.15f)),
				Math.min(255, (int) (primaryColor.getGreen() * 1.15f)),
				Math.min(255, (int) (primaryColor.getBlue() * 1.15f))
			);
		}

		public String getDisplayName()
		{
			return displayName;
		}

		public Color getPrimaryColor()
		{
			return primaryColor;
		}

		public Color getHighlightColor()
		{
			return highlightColor;
		}

		public Color getPinColor()
		{
			return pinColor;
		}
	}

	public static final class TravelDestination
	{
		private final String name;
		private final WorldPoint location;
		private final String cost;
		private final String requirement;

		public TravelDestination(String name, WorldPoint location, String cost, String requirement)
		{
			this.name = name;
			this.location = location;
			this.cost = cost;
			this.requirement = requirement;
		}

		public TravelDestination(String name, WorldPoint location, String cost)
		{
			this(name, location, cost, null);
		}

		public String getName()
		{
			return name;
		}

		public WorldPoint getLocation()
		{
			return location;
		}

		public String getCost()
		{
			return cost;
		}

		public String getRequirement()
		{
			return requirement;
		}
	}

	public static final class TravelNode
	{
		private final String name;
		private final WorldPoint location;
		private final TravelType type;
		private final List<TravelDestination> destinations;

		public TravelNode(String name, WorldPoint location, TravelType type, List<TravelDestination> destinations)
		{
			this.name = name;
			this.location = location;
			this.type = type;
			this.destinations = Collections.unmodifiableList(new ArrayList<>(destinations));
		}

		public String getName()
		{
			return name;
		}

		public WorldPoint getLocation()
		{
			return location;
		}

		public TravelType getType()
		{
			return type;
		}

		public List<TravelDestination> getDestinations()
		{
			return destinations;
		}
	}

	public static final List<TravelNode> ALL_NODES;

	static
	{
		final List<TravelNode> nodes = new ArrayList<>();

		addCharterShips(nodes);
		addPassengerFerries(nodes);
		addGnomeGliders(nodes);
		addSpiritTrees(nodes);
		addFairyRings(nodes);
		addHotAirBalloons(nodes);
		addCanoes(nodes);
		addMineCarts(nodes);
		addMagicCarpets(nodes);
		addQuetzals(nodes);
		addSwampAndRiverBoats(nodes);

		ALL_NODES = Collections.unmodifiableList(nodes);
	}

	/** 1. CHARTER SHIPS */
	private static void addCharterShips(List<TravelNode> nodes)
	{
		final WorldPoint psCharter = new WorldPoint(3038, 3192, 0);
		final WorldPoint cathCharter = new WorldPoint(2796, 3414, 0);
		final WorldPoint brimCharter = new WorldPoint(2760, 3238, 0);
		final WorldPoint musaCharter = new WorldPoint(2954, 3158, 0);
		final WorldPoint khazCharter = new WorldPoint(2674, 3144, 0);
		final WorldPoint phasCharter = new WorldPoint(3702, 3503, 0);
		final WorldPoint shipCharter = new WorldPoint(3001, 3032, 0);
		final WorldPoint mosCharter = new WorldPoint(3671, 2982, 0);
		final WorldPoint corsCharter = new WorldPoint(2578, 2839, 0);
		final WorldPoint fortCharter = new WorldPoint(1743, 3136, 0);
		final WorldPoint prifCharter = new WorldPoint(2150, 3330, 0);
		final WorldPoint tyrasCharter = new WorldPoint(2139, 3120, 0);
		final WorldPoint sunsetCharter = new WorldPoint(1510, 2970, 0);
		final WorldPoint aldarinCharter = new WorldPoint(1446, 2975, 0);
		final WorldPoint piscCharter = new WorldPoint(1824, 3691, 0);
		final WorldPoint landsEndCharter = new WorldPoint(1504, 3400, 0);

		// Port Sarim Charter
		nodes.add(new TravelNode("Port Sarim Charter", psCharter, TravelType.CHARTER_SHIP, List.of(
			new TravelDestination("Catherby", cathCharter, "1,000 gp"),
			new TravelDestination("Brimhaven", brimCharter, "1,600 gp"),
			new TravelDestination("Musa Point (Karamja)", musaCharter, "1,000 gp"),
			new TravelDestination("Port Khazard", khazCharter, "1,600 gp"),
			new TravelDestination("Port Phasmatys", phasCharter, "3,200 gp", "Ghosts Ahoy (or 2 Ecto-tokens)"),
			new TravelDestination("Shipyard (Karamja)", shipCharter, "400 gp", "Grand Tree started"),
			new TravelDestination("Mos Le'Harmless", mosCharter, "1,600 gp", "Cabin Fever"),
			new TravelDestination("Corsair Cove", corsCharter, "1,600 gp", "The Corsair Curse"),
			new TravelDestination("Civitas illa Fortis (Varlamore)", fortCharter, "3,200 gp", "Children of the Sun"),
			new TravelDestination("Prifddinas", prifCharter, "3,200 gp", "Song of the Elves"),
			new TravelDestination("Port Tyras", tyrasCharter, "3,200 gp", "Regicide"),
			new TravelDestination("Sunset Coast (Varlamore)", sunsetCharter, "3,200 gp", "Children of the Sun"),
			new TravelDestination("Aldarin (Varlamore)", aldarinCharter, "3,200 gp", "Children of the Sun"),
			new TravelDestination("Port Piscarilius (Kourend)", piscCharter, "1,600 gp"),
			new TravelDestination("Land's End (Kourend)", landsEndCharter, "1,600 gp")
		)));

		// Catherby Charter
		nodes.add(new TravelNode("Catherby Charter", cathCharter, TravelType.CHARTER_SHIP, List.of(
			new TravelDestination("Port Sarim", psCharter, "1,000 gp"),
			new TravelDestination("Brimhaven", brimCharter, "480 gp"),
			new TravelDestination("Musa Point (Karamja)", musaCharter, "1,000 gp"),
			new TravelDestination("Port Khazard", khazCharter, "1,600 gp"),
			new TravelDestination("Port Phasmatys", phasCharter, "3,200 gp", "Ghosts Ahoy"),
			new TravelDestination("Shipyard (Karamja)", shipCharter, "1,600 gp", "Grand Tree started"),
			new TravelDestination("Mos Le'Harmless", mosCharter, "3,200 gp", "Cabin Fever"),
			new TravelDestination("Corsair Cove", corsCharter, "1,600 gp", "The Corsair Curse"),
			new TravelDestination("Civitas illa Fortis (Varlamore)", fortCharter, "3,200 gp", "Children of the Sun"),
			new TravelDestination("Prifddinas", prifCharter, "3,200 gp", "Song of the Elves"),
			new TravelDestination("Port Tyras", tyrasCharter, "3,200 gp", "Regicide"),
			new TravelDestination("Sunset Coast (Varlamore)", sunsetCharter, "3,200 gp", "Children of the Sun"),
			new TravelDestination("Aldarin (Varlamore)", aldarinCharter, "3,200 gp", "Children of the Sun"),
			new TravelDestination("Port Piscarilius (Kourend)", piscCharter, "3,200 gp"),
			new TravelDestination("Land's End (Kourend)", landsEndCharter, "3,200 gp")
		)));

		// Brimhaven Charter
		nodes.add(new TravelNode("Brimhaven Charter", brimCharter, TravelType.CHARTER_SHIP, List.of(
			new TravelDestination("Port Sarim", psCharter, "1,600 gp"),
			new TravelDestination("Catherby", cathCharter, "480 gp"),
			new TravelDestination("Musa Point (Karamja)", musaCharter, "200 gp"),
			new TravelDestination("Port Khazard", khazCharter, "400 gp"),
			new TravelDestination("Port Phasmatys", phasCharter, "3,200 gp", "Ghosts Ahoy"),
			new TravelDestination("Shipyard (Karamja)", shipCharter, "400 gp", "Grand Tree started"),
			new TravelDestination("Mos Le'Harmless", mosCharter, "1,600 gp", "Cabin Fever"),
			new TravelDestination("Corsair Cove", corsCharter, "1,600 gp", "The Corsair Curse"),
			new TravelDestination("Civitas illa Fortis (Varlamore)", fortCharter, "3,200 gp", "Children of the Sun"),
			new TravelDestination("Prifddinas", prifCharter, "3,200 gp", "Song of the Elves"),
			new TravelDestination("Port Tyras", tyrasCharter, "3,200 gp", "Regicide"),
			new TravelDestination("Sunset Coast (Varlamore)", sunsetCharter, "3,200 gp", "Children of the Sun"),
			new TravelDestination("Aldarin (Varlamore)", aldarinCharter, "3,200 gp", "Children of the Sun"),
			new TravelDestination("Port Piscarilius (Kourend)", piscCharter, "3,200 gp"),
			new TravelDestination("Land's End (Kourend)", landsEndCharter, "3,200 gp")
		)));

		// Port Khazard Charter
		nodes.add(new TravelNode("Port Khazard Charter", khazCharter, TravelType.CHARTER_SHIP, List.of(
			new TravelDestination("Port Sarim", psCharter, "1,600 gp"),
			new TravelDestination("Catherby", cathCharter, "1,600 gp"),
			new TravelDestination("Brimhaven", brimCharter, "400 gp"),
			new TravelDestination("Musa Point (Karamja)", musaCharter, "400 gp"),
			new TravelDestination("Shipyard (Karamja)", shipCharter, "800 gp", "Grand Tree started"),
			new TravelDestination("Port Phasmatys", phasCharter, "3,200 gp", "Ghosts Ahoy"),
			new TravelDestination("Mos Le'Harmless", mosCharter, "1,600 gp", "Cabin Fever"),
			new TravelDestination("Corsair Cove", corsCharter, "1,600 gp", "The Corsair Curse"),
			new TravelDestination("Civitas illa Fortis (Varlamore)", fortCharter, "3,200 gp", "Children of the Sun"),
			new TravelDestination("Prifddinas", prifCharter, "3,200 gp", "Song of the Elves"),
			new TravelDestination("Port Tyras", tyrasCharter, "3,200 gp", "Regicide"),
			new TravelDestination("Sunset Coast (Varlamore)", sunsetCharter, "3,200 gp", "Children of the Sun"),
			new TravelDestination("Aldarin (Varlamore)", aldarinCharter, "3,200 gp", "Children of the Sun"),
			new TravelDestination("Port Piscarilius (Kourend)", piscCharter, "3,200 gp"),
			new TravelDestination("Land's End (Kourend)", landsEndCharter, "3,200 gp")
		)));

		// Musa Point Charter
		nodes.add(new TravelNode("Musa Point Charter", musaCharter, TravelType.CHARTER_SHIP, List.of(
			new TravelDestination("Port Sarim", psCharter, "1,000 gp"),
			new TravelDestination("Catherby", cathCharter, "1,000 gp"),
			new TravelDestination("Brimhaven", brimCharter, "200 gp"),
			new TravelDestination("Port Khazard", khazCharter, "400 gp"),
			new TravelDestination("Port Phasmatys", phasCharter, "3,200 gp", "Ghosts Ahoy"),
			new TravelDestination("Shipyard (Karamja)", shipCharter, "400 gp", "Grand Tree started"),
			new TravelDestination("Mos Le'Harmless", mosCharter, "1,600 gp", "Cabin Fever"),
			new TravelDestination("Corsair Cove", corsCharter, "1,600 gp", "The Corsair Curse"),
			new TravelDestination("Civitas illa Fortis (Varlamore)", fortCharter, "3,200 gp", "Children of the Sun"),
			new TravelDestination("Prifddinas", prifCharter, "3,200 gp", "Song of the Elves"),
			new TravelDestination("Port Tyras", tyrasCharter, "3,200 gp", "Regicide"),
			new TravelDestination("Sunset Coast (Varlamore)", sunsetCharter, "3,200 gp", "Children of the Sun"),
			new TravelDestination("Aldarin (Varlamore)", aldarinCharter, "3,200 gp", "Children of the Sun"),
			new TravelDestination("Port Piscarilius (Kourend)", piscCharter, "3,200 gp"),
			new TravelDestination("Land's End (Kourend)", landsEndCharter, "3,200 gp")
		)));

		// Port Phasmatys Charter
		nodes.add(new TravelNode("Port Phasmatys Charter", phasCharter, TravelType.CHARTER_SHIP, List.of(
			new TravelDestination("Port Sarim", psCharter, "3,200 gp"),
			new TravelDestination("Catherby", cathCharter, "3,200 gp"),
			new TravelDestination("Brimhaven", brimCharter, "3,200 gp"),
			new TravelDestination("Musa Point (Karamja)", musaCharter, "3,200 gp"),
			new TravelDestination("Port Khazard", khazCharter, "3,200 gp"),
			new TravelDestination("Shipyard (Karamja)", shipCharter, "3,200 gp", "Grand Tree started"),
			new TravelDestination("Mos Le'Harmless", mosCharter, "3,200 gp", "Cabin Fever"),
			new TravelDestination("Corsair Cove", corsCharter, "3,200 gp", "The Corsair Curse"),
			new TravelDestination("Civitas illa Fortis (Varlamore)", fortCharter, "3,200 gp", "Children of the Sun"),
			new TravelDestination("Prifddinas", prifCharter, "3,200 gp", "Song of the Elves"),
			new TravelDestination("Port Tyras", tyrasCharter, "3,200 gp", "Regicide"),
			new TravelDestination("Sunset Coast (Varlamore)", sunsetCharter, "3,200 gp", "Children of the Sun"),
			new TravelDestination("Aldarin (Varlamore)", aldarinCharter, "3,200 gp", "Children of the Sun"),
			new TravelDestination("Port Piscarilius (Kourend)", piscCharter, "3,200 gp"),
			new TravelDestination("Land's End (Kourend)", landsEndCharter, "3,200 gp")
		)));

		// Shipyard (Karamja) Charter
		nodes.add(new TravelNode("Shipyard Charter", shipCharter, TravelType.CHARTER_SHIP, List.of(
			new TravelDestination("Port Sarim", psCharter, "400 gp"),
			new TravelDestination("Catherby", cathCharter, "1,600 gp"),
			new TravelDestination("Brimhaven", brimCharter, "400 gp"),
			new TravelDestination("Musa Point (Karamja)", musaCharter, "400 gp"),
			new TravelDestination("Port Khazard", khazCharter, "800 gp"),
			new TravelDestination("Port Phasmatys", phasCharter, "3,200 gp", "Ghosts Ahoy"),
			new TravelDestination("Mos Le'Harmless", mosCharter, "1,600 gp", "Cabin Fever"),
			new TravelDestination("Corsair Cove", corsCharter, "1,600 gp", "The Corsair Curse"),
			new TravelDestination("Civitas illa Fortis (Varlamore)", fortCharter, "3,200 gp", "Children of the Sun"),
			new TravelDestination("Prifddinas", prifCharter, "3,200 gp", "Song of the Elves"),
			new TravelDestination("Port Tyras", tyrasCharter, "3,200 gp", "Regicide"),
			new TravelDestination("Sunset Coast (Varlamore)", sunsetCharter, "3,200 gp", "Children of the Sun"),
			new TravelDestination("Aldarin (Varlamore)", aldarinCharter, "3,200 gp", "Children of the Sun"),
			new TravelDestination("Port Piscarilius (Kourend)", piscCharter, "3,200 gp"),
			new TravelDestination("Land's End (Kourend)", landsEndCharter, "3,200 gp")
		)));

		// Mos Le'Harmless Charter
		nodes.add(new TravelNode("Mos Le'Harmless Charter", mosCharter, TravelType.CHARTER_SHIP, List.of(
			new TravelDestination("Port Sarim", psCharter, "1,600 gp"),
			new TravelDestination("Catherby", cathCharter, "3,200 gp"),
			new TravelDestination("Brimhaven", brimCharter, "1,600 gp"),
			new TravelDestination("Musa Point (Karamja)", musaCharter, "1,600 gp"),
			new TravelDestination("Port Khazard", khazCharter, "1,600 gp"),
			new TravelDestination("Port Phasmatys", phasCharter, "3,200 gp", "Ghosts Ahoy"),
			new TravelDestination("Shipyard (Karamja)", shipCharter, "1,600 gp", "Grand Tree started"),
			new TravelDestination("Corsair Cove", corsCharter, "3,200 gp", "The Corsair Curse"),
			new TravelDestination("Civitas illa Fortis (Varlamore)", fortCharter, "3,200 gp", "Children of the Sun"),
			new TravelDestination("Prifddinas", prifCharter, "3,200 gp", "Song of the Elves"),
			new TravelDestination("Port Tyras", tyrasCharter, "1,600 gp", "Regicide"),
			new TravelDestination("Sunset Coast (Varlamore)", sunsetCharter, "3,200 gp", "Children of the Sun"),
			new TravelDestination("Aldarin (Varlamore)", aldarinCharter, "3,200 gp", "Children of the Sun"),
			new TravelDestination("Port Piscarilius (Kourend)", piscCharter, "3,200 gp"),
			new TravelDestination("Land's End (Kourend)", landsEndCharter, "3,200 gp")
		)));

		// Corsair Cove Charter
		nodes.add(new TravelNode("Corsair Cove Charter", corsCharter, TravelType.CHARTER_SHIP, List.of(
			new TravelDestination("Port Sarim", psCharter, "1,600 gp"),
			new TravelDestination("Catherby", cathCharter, "1,600 gp"),
			new TravelDestination("Brimhaven", brimCharter, "1,600 gp"),
			new TravelDestination("Musa Point (Karamja)", musaCharter, "1,600 gp"),
			new TravelDestination("Port Khazard", khazCharter, "1,600 gp"),
			new TravelDestination("Port Phasmatys", phasCharter, "3,200 gp", "Ghosts Ahoy"),
			new TravelDestination("Shipyard (Karamja)", shipCharter, "1,600 gp", "Grand Tree started"),
			new TravelDestination("Mos Le'Harmless", mosCharter, "3,200 gp", "Cabin Fever"),
			new TravelDestination("Civitas illa Fortis (Varlamore)", fortCharter, "3,200 gp", "Children of the Sun"),
			new TravelDestination("Prifddinas", prifCharter, "3,200 gp", "Song of the Elves"),
			new TravelDestination("Port Tyras", tyrasCharter, "3,200 gp", "Regicide"),
			new TravelDestination("Sunset Coast (Varlamore)", sunsetCharter, "3,200 gp", "Children of the Sun"),
			new TravelDestination("Aldarin (Varlamore)", aldarinCharter, "3,200 gp", "Children of the Sun"),
			new TravelDestination("Port Piscarilius (Kourend)", piscCharter, "3,200 gp"),
			new TravelDestination("Land's End (Kourend)", landsEndCharter, "3,200 gp")
		)));

		// Civitas illa Fortis Charter (Varlamore)
		nodes.add(new TravelNode("Civitas illa Fortis Charter", fortCharter, TravelType.CHARTER_SHIP, List.of(
			new TravelDestination("Port Sarim", psCharter, "3,200 gp"),
			new TravelDestination("Catherby", cathCharter, "3,200 gp"),
			new TravelDestination("Brimhaven", brimCharter, "3,200 gp"),
			new TravelDestination("Musa Point (Karamja)", musaCharter, "3,200 gp"),
			new TravelDestination("Port Khazard", khazCharter, "3,200 gp"),
			new TravelDestination("Port Phasmatys", phasCharter, "3,200 gp", "Ghosts Ahoy"),
			new TravelDestination("Shipyard (Karamja)", shipCharter, "3,200 gp", "Grand Tree started"),
			new TravelDestination("Mos Le'Harmless", mosCharter, "3,200 gp", "Cabin Fever"),
			new TravelDestination("Corsair Cove", corsCharter, "3,200 gp", "The Corsair Curse"),
			new TravelDestination("Prifddinas", prifCharter, "3,200 gp", "Song of the Elves"),
			new TravelDestination("Port Tyras", tyrasCharter, "3,200 gp", "Regicide"),
			new TravelDestination("Sunset Coast (Varlamore)", sunsetCharter, "500 gp", "Children of the Sun"),
			new TravelDestination("Aldarin (Varlamore)", aldarinCharter, "500 gp", "Children of the Sun"),
			new TravelDestination("Port Piscarilius (Kourend)", piscCharter, "3,200 gp"),
			new TravelDestination("Land's End (Kourend)", landsEndCharter, "3,200 gp")
		)));

		// Prifddinas Charter
		nodes.add(new TravelNode("Prifddinas Charter", prifCharter, TravelType.CHARTER_SHIP, List.of(
			new TravelDestination("Port Sarim", psCharter, "3,200 gp"),
			new TravelDestination("Catherby", cathCharter, "3,200 gp"),
			new TravelDestination("Brimhaven", brimCharter, "3,200 gp"),
			new TravelDestination("Musa Point (Karamja)", musaCharter, "3,200 gp"),
			new TravelDestination("Port Khazard", khazCharter, "3,200 gp"),
			new TravelDestination("Port Phasmatys", phasCharter, "3,200 gp", "Ghosts Ahoy"),
			new TravelDestination("Shipyard (Karamja)", shipCharter, "3,200 gp", "Grand Tree started"),
			new TravelDestination("Mos Le'Harmless", mosCharter, "3,200 gp", "Cabin Fever"),
			new TravelDestination("Corsair Cove", corsCharter, "3,200 gp", "The Corsair Curse"),
			new TravelDestination("Civitas illa Fortis (Varlamore)", fortCharter, "3,200 gp", "Children of the Sun"),
			new TravelDestination("Port Tyras", tyrasCharter, "3,200 gp", "Regicide"),
			new TravelDestination("Sunset Coast (Varlamore)", sunsetCharter, "3,200 gp", "Children of the Sun"),
			new TravelDestination("Aldarin (Varlamore)", aldarinCharter, "3,200 gp", "Children of the Sun"),
			new TravelDestination("Port Piscarilius (Kourend)", piscCharter, "3,200 gp"),
			new TravelDestination("Land's End (Kourend)", landsEndCharter, "3,200 gp")
		)));

		// Port Tyras Charter
		nodes.add(new TravelNode("Port Tyras Charter", tyrasCharter, TravelType.CHARTER_SHIP, List.of(
			new TravelDestination("Port Sarim", psCharter, "3,200 gp"),
			new TravelDestination("Catherby", cathCharter, "3,200 gp"),
			new TravelDestination("Brimhaven", brimCharter, "3,200 gp"),
			new TravelDestination("Musa Point (Karamja)", musaCharter, "3,200 gp"),
			new TravelDestination("Port Khazard", khazCharter, "3,200 gp"),
			new TravelDestination("Port Phasmatys", phasCharter, "3,200 gp", "Ghosts Ahoy"),
			new TravelDestination("Shipyard (Karamja)", shipCharter, "3,200 gp", "Grand Tree started"),
			new TravelDestination("Mos Le'Harmless", mosCharter, "1,600 gp", "Cabin Fever"),
			new TravelDestination("Corsair Cove", corsCharter, "3,200 gp", "The Corsair Curse"),
			new TravelDestination("Civitas illa Fortis (Varlamore)", fortCharter, "3,200 gp", "Children of the Sun"),
			new TravelDestination("Prifddinas", prifCharter, "3,200 gp", "Song of the Elves"),
			new TravelDestination("Sunset Coast (Varlamore)", sunsetCharter, "3,200 gp", "Children of the Sun"),
			new TravelDestination("Aldarin (Varlamore)", aldarinCharter, "3,200 gp", "Children of the Sun"),
			new TravelDestination("Port Piscarilius (Kourend)", piscCharter, "3,200 gp"),
			new TravelDestination("Land's End (Kourend)", landsEndCharter, "3,200 gp")
		)));

		// Sunset Coast Charter
		nodes.add(new TravelNode("Sunset Coast Charter", sunsetCharter, TravelType.CHARTER_SHIP, List.of(
			new TravelDestination("Port Sarim", psCharter, "3,200 gp"),
			new TravelDestination("Catherby", cathCharter, "3,200 gp"),
			new TravelDestination("Brimhaven", brimCharter, "3,200 gp"),
			new TravelDestination("Musa Point (Karamja)", musaCharter, "3,200 gp"),
			new TravelDestination("Port Khazard", khazCharter, "3,200 gp"),
			new TravelDestination("Port Phasmatys", phasCharter, "3,200 gp", "Ghosts Ahoy"),
			new TravelDestination("Shipyard (Karamja)", shipCharter, "3,200 gp", "Grand Tree started"),
			new TravelDestination("Mos Le'Harmless", mosCharter, "3,200 gp", "Cabin Fever"),
			new TravelDestination("Corsair Cove", corsCharter, "3,200 gp", "The Corsair Curse"),
			new TravelDestination("Civitas illa Fortis (Varlamore)", fortCharter, "500 gp", "Children of the Sun"),
			new TravelDestination("Prifddinas", prifCharter, "3,200 gp", "Song of the Elves"),
			new TravelDestination("Port Tyras", tyrasCharter, "3,200 gp", "Regicide"),
			new TravelDestination("Aldarin (Varlamore)", aldarinCharter, "500 gp", "Children of the Sun"),
			new TravelDestination("Port Piscarilius (Kourend)", piscCharter, "3,200 gp"),
			new TravelDestination("Land's End (Kourend)", landsEndCharter, "3,200 gp")
		)));

		// Aldarin Charter (Varlamore Part 2)
		nodes.add(new TravelNode("Aldarin Charter", aldarinCharter, TravelType.CHARTER_SHIP, List.of(
			new TravelDestination("Port Sarim", psCharter, "3,200 gp"),
			new TravelDestination("Catherby", cathCharter, "3,200 gp"),
			new TravelDestination("Brimhaven", brimCharter, "3,200 gp"),
			new TravelDestination("Musa Point (Karamja)", musaCharter, "3,200 gp"),
			new TravelDestination("Port Khazard", khazCharter, "3,200 gp"),
			new TravelDestination("Port Phasmatys", phasCharter, "3,200 gp", "Ghosts Ahoy"),
			new TravelDestination("Shipyard (Karamja)", shipCharter, "3,200 gp", "Grand Tree started"),
			new TravelDestination("Mos Le'Harmless", mosCharter, "3,200 gp", "Cabin Fever"),
			new TravelDestination("Corsair Cove", corsCharter, "3,200 gp", "The Corsair Curse"),
			new TravelDestination("Civitas illa Fortis (Varlamore)", fortCharter, "500 gp", "Children of the Sun"),
			new TravelDestination("Prifddinas", prifCharter, "3,200 gp", "Song of the Elves"),
			new TravelDestination("Port Tyras", tyrasCharter, "3,200 gp", "Regicide"),
			new TravelDestination("Sunset Coast (Varlamore)", sunsetCharter, "500 gp", "Children of the Sun"),
			new TravelDestination("Port Piscarilius (Kourend)", piscCharter, "3,200 gp"),
			new TravelDestination("Land's End (Kourend)", landsEndCharter, "3,200 gp")
		)));

		// Port Piscarilius Charter (Kourend)
		nodes.add(new TravelNode("Port Piscarilius Charter", piscCharter, TravelType.CHARTER_SHIP, List.of(
			new TravelDestination("Port Sarim", psCharter, "1,600 gp"),
			new TravelDestination("Catherby", cathCharter, "3,200 gp"),
			new TravelDestination("Brimhaven", brimCharter, "3,200 gp"),
			new TravelDestination("Musa Point (Karamja)", musaCharter, "3,200 gp"),
			new TravelDestination("Port Khazard", khazCharter, "3,200 gp"),
			new TravelDestination("Port Phasmatys", phasCharter, "3,200 gp", "Ghosts Ahoy"),
			new TravelDestination("Shipyard (Karamja)", shipCharter, "3,200 gp", "Grand Tree started"),
			new TravelDestination("Mos Le'Harmless", mosCharter, "3,200 gp", "Cabin Fever"),
			new TravelDestination("Corsair Cove", corsCharter, "3,200 gp", "The Corsair Curse"),
			new TravelDestination("Civitas illa Fortis (Varlamore)", fortCharter, "3,200 gp", "Children of the Sun"),
			new TravelDestination("Prifddinas", prifCharter, "3,200 gp", "Song of the Elves"),
			new TravelDestination("Port Tyras", tyrasCharter, "3,200 gp", "Regicide"),
			new TravelDestination("Sunset Coast (Varlamore)", sunsetCharter, "3,200 gp", "Children of the Sun"),
			new TravelDestination("Aldarin (Varlamore)", aldarinCharter, "3,200 gp", "Children of the Sun"),
			new TravelDestination("Land's End (Kourend)", landsEndCharter, "500 gp")
		)));

		// Land's End Charter (Kourend)
		nodes.add(new TravelNode("Land's End Charter", landsEndCharter, TravelType.CHARTER_SHIP, List.of(
			new TravelDestination("Port Sarim", psCharter, "1,600 gp"),
			new TravelDestination("Catherby", cathCharter, "3,200 gp"),
			new TravelDestination("Brimhaven", brimCharter, "3,200 gp"),
			new TravelDestination("Musa Point (Karamja)", musaCharter, "3,200 gp"),
			new TravelDestination("Port Khazard", khazCharter, "3,200 gp"),
			new TravelDestination("Port Phasmatys", phasCharter, "3,200 gp", "Ghosts Ahoy"),
			new TravelDestination("Shipyard (Karamja)", shipCharter, "3,200 gp", "Grand Tree started"),
			new TravelDestination("Mos Le'Harmless", mosCharter, "3,200 gp", "Cabin Fever"),
			new TravelDestination("Corsair Cove", corsCharter, "3,200 gp", "The Corsair Curse"),
			new TravelDestination("Civitas illa Fortis (Varlamore)", fortCharter, "3,200 gp", "Children of the Sun"),
			new TravelDestination("Prifddinas", prifCharter, "3,200 gp", "Song of the Elves"),
			new TravelDestination("Port Tyras", tyrasCharter, "3,200 gp", "Regicide"),
			new TravelDestination("Sunset Coast (Varlamore)", sunsetCharter, "3,200 gp", "Children of the Sun"),
			new TravelDestination("Aldarin (Varlamore)", aldarinCharter, "3,200 gp", "Children of the Sun"),
			new TravelDestination("Port Piscarilius (Kourend)", piscCharter, "500 gp")
		)));
	}

	/** 2. PASSENGER FERRIES and SHIPS */
	private static void addPassengerFerries(List<TravelNode> nodes)
	{
		final WorldPoint psFerry = new WorldPoint(3028, 3218, 0);
		final WorldPoint musaFerry = new WorldPoint(2954, 3147, 0);
		final WorldPoint psEntrana = new WorldPoint(3047, 3235, 0);
		final WorldPoint entranaPier = new WorldPoint(2834, 3335, 0);
		final WorldPoint psVoid = new WorldPoint(3040, 3202, 0);
		final WorldPoint voidOutpost = new WorldPoint(2659, 2676, 0);
		final WorldPoint psVeos = new WorldPoint(3054, 3245, 0);
		final WorldPoint landsEnd = new WorldPoint(1504, 3399, 0);
		final WorldPoint piscPort = new WorldPoint(1824, 3691, 0);
		final WorldPoint ardCustoms = new WorldPoint(2683, 3270, 0);
		final WorldPoint brimCustoms = new WorldPoint(2772, 3227, 0);
		final WorldPoint relSailor = new WorldPoint(2629, 3693, 0);
		final WorldPoint miscHarbor = new WorldPoint(2581, 3846, 0);
		final WorldPoint relJarvald = new WorldPoint(2620, 3686, 0);
		final WorldPoint waterbirthPier = new WorldPoint(2545, 3755, 0);
		final WorldPoint relMaria = new WorldPoint(2644, 3710, 0);
		final WorldPoint neitiznotPier = new WorldPoint(2311, 3781, 0);
		final WorldPoint relMord = new WorldPoint(2643, 3699, 0);
		final WorldPoint jatizsoPier = new WorldPoint(2420, 3781, 0);
		final WorldPoint relLokar = new WorldPoint(2621, 3677, 0);
		final WorldPoint piratesCove = new WorldPoint(2213, 3794, 0);
		final WorldPoint lunarIsle = new WorldPoint(2113, 3894, 0);
		final WorldPoint relWeiss = new WorldPoint(2698, 3705, 0);
		final WorldPoint weissPier = new WorldPoint(2848, 3962, 0);
		final WorldPoint isleOfSoulsPier = new WorldPoint(2282, 2823, 0);

		// Port Sarim Ferry ⇄ Karamja
		nodes.add(new TravelNode("Port Sarim Ferry", psFerry, TravelType.FERRY, List.of(
			new TravelDestination("Musa Point (Karamja)", musaFerry, "30 gp", "Free with Karamja medium diary")
		)));
		nodes.add(new TravelNode("Musa Point Ferry", musaFerry, TravelType.FERRY, List.of(
			new TravelDestination("Port Sarim", psFerry, "30 gp", "Free with Karamja medium diary")
		)));

		// Port Sarim ⇄ Entrana
		nodes.add(new TravelNode("Entrana Monks (Port Sarim)", psEntrana, TravelType.FERRY, List.of(
			new TravelDestination("Entrana", entranaPier, "Free", "No weapons or armor allowed")
		)));
		nodes.add(new TravelNode("Entrana Ferry", entranaPier, TravelType.FERRY, List.of(
			new TravelDestination("Port Sarim", psEntrana, "Free")
		)));

		// Port Sarim ⇄ Void Knights' Outpost
		nodes.add(new TravelNode("Void Knight Lander (Port Sarim)", psVoid, TravelType.FERRY, List.of(
			new TravelDestination("Void Knights' Outpost (Pest Control)", voidOutpost, "Free")
		)));
		nodes.add(new TravelNode("Void Knight Lander (Outpost)", voidOutpost, TravelType.FERRY, List.of(
			new TravelDestination("Port Sarim", psVoid, "Free")
		)));

		// Veos: Port Sarim ⇄ Land's End / Piscarilius
		nodes.add(new TravelNode("Veos's Ship (Port Sarim)", psVeos, TravelType.FERRY, List.of(
			new TravelDestination("Land's End (Kourend)", landsEnd, "Free"),
			new TravelDestination("Port Piscarilius", piscPort, "Free")
		)));
		nodes.add(new TravelNode("Veos's Ship (Land's End)", landsEnd, TravelType.FERRY, List.of(
			new TravelDestination("Port Sarim", psVeos, "Free"),
			new TravelDestination("Port Piscarilius", piscPort, "Free"),
			new TravelDestination("Isle of Souls", isleOfSoulsPier, "Free")
		)));
		nodes.add(new TravelNode("Veos's Ship (Port Piscarilius)", piscPort, TravelType.FERRY, List.of(
			new TravelDestination("Port Sarim", psVeos, "Free"),
			new TravelDestination("Land's End (Kourend)", landsEnd, "Free")
		)));

		// Ardougne ⇄ Brimhaven Customs
		nodes.add(new TravelNode("Ardougne Harbor Customs", ardCustoms, TravelType.FERRY, List.of(
			new TravelDestination("Brimhaven", brimCustoms, "30 gp")
		)));
		nodes.add(new TravelNode("Brimhaven Harbor Customs", brimCustoms, TravelType.FERRY, List.of(
			new TravelDestination("East Ardougne", ardCustoms, "30 gp")
		)));

		// Rellekka Sailors (Miscellania, Waterbirth, Fremennik Isles, Lunar, Weiss)
		nodes.add(new TravelNode("Rellekka Sailor", relSailor, TravelType.FERRY, List.of(
			new TravelDestination("Miscellania & Etceteria", miscHarbor, "Free", "The Fremennik Trials")
		)));
		nodes.add(new TravelNode("Miscellania Sailor", miscHarbor, TravelType.FERRY, List.of(
			new TravelDestination("Rellekka", relSailor, "Free", "The Fremennik Trials")
		)));

		nodes.add(new TravelNode("Jarvald's Boat (Rellekka)", relJarvald, TravelType.FERRY, List.of(
			new TravelDestination("Waterbirth Island (Dagannoth Kings)", waterbirthPier, "Free / 1,000 gp", "The Fremennik Trials")
		)));
		nodes.add(new TravelNode("Jarvald's Boat (Waterbirth)", waterbirthPier, TravelType.FERRY, List.of(
			new TravelDestination("Rellekka", relJarvald, "Free / 1,000 gp", "The Fremennik Trials")
		)));

		nodes.add(new TravelNode("Maria Gunnars (Rellekka)", relMaria, TravelType.FERRY, List.of(
			new TravelDestination("Neitiznot", neitiznotPier, "Free", "The Fremennik Isles")
		)));
		nodes.add(new TravelNode("Maria Gunnars (Neitiznot)", neitiznotPier, TravelType.FERRY, List.of(
			new TravelDestination("Rellekka", relMaria, "Free", "The Fremennik Isles")
		)));

		nodes.add(new TravelNode("Mord Gunnars (Rellekka)", relMord, TravelType.FERRY, List.of(
			new TravelDestination("Jatizso", jatizsoPier, "Free", "The Fremennik Isles")
		)));
		nodes.add(new TravelNode("Mord Gunnars (Jatizso)", jatizsoPier, TravelType.FERRY, List.of(
			new TravelDestination("Rellekka", relMord, "Free", "The Fremennik Isles")
		)));

		nodes.add(new TravelNode("Lokar Searunner (Rellekka)", relLokar, TravelType.FERRY, List.of(
			new TravelDestination("Pirate's Cove / Lunar Isle", piratesCove, "Free", "Lunar Diplomacy started"),
			new TravelDestination("Lunar Isle", lunarIsle, "Free", "Seal of passage or Dream Mentor")
		)));
		nodes.add(new TravelNode("Lokar Searunner (Pirate's Cove)", piratesCove, TravelType.FERRY, List.of(
			new TravelDestination("Rellekka", relLokar, "Free", "Lunar Diplomacy started"),
			new TravelDestination("Lunar Isle", lunarIsle, "Free", "Seal of passage or Dream Mentor")
		)));
		nodes.add(new TravelNode("Lokar Searunner (Lunar Isle)", lunarIsle, TravelType.FERRY, List.of(
			new TravelDestination("Pirate's Cove / Lunar Isle", piratesCove, "Free", "Lunar Diplomacy started"),
			new TravelDestination("Rellekka", relLokar, "Free", "Seal of passage or Dream Mentor")
		)));

		nodes.add(new TravelNode("Larry's Boat (Rellekka)", relWeiss, TravelType.FERRY, List.of(
			new TravelDestination("Weiss (Salt Mines)", weissPier, "Free", "Making Friends with My Arm")
		)));
		nodes.add(new TravelNode("Larry's Boat (Weiss)", weissPier, TravelType.FERRY, List.of(
			new TravelDestination("Rellekka", relWeiss, "Free", "Making Friends with My Arm")
		)));

		// Slepe ferry (Andras)
		final WorldPoint ectofuntusFerry = new WorldPoint(3687, 3471, 0);
		final WorldPoint slepePier = new WorldPoint(3724, 3302, 0);
		nodes.add(new TravelNode("Andras's Ferry (Ectofuntus)", ectofuntusFerry, TravelType.FERRY, List.of(
			new TravelDestination("Slepe", slepePier, "Free", "Priest in Peril")
		)));
		nodes.add(new TravelNode("Andras's Ferry (Slepe)", slepePier, TravelType.FERRY, List.of(
			new TravelDestination("Ectofuntus / Port Phasmatys", ectofuntusFerry, "Free", "Priest in Peril")
		)));

		// Corsair Cove rowboat (Cabin Boy Jenkins / Captain Tock / Colin)
		final WorldPoint psCorsairRowboat = new WorldPoint(3055, 3242, 0);
		final WorldPoint corsairRowboat = new WorldPoint(2589, 2851, 0);
		final WorldPoint rimmingtonColin = new WorldPoint(2919, 3225, 0);
		nodes.add(new TravelNode("Corsair Rowboat (Port Sarim)", psCorsairRowboat, TravelType.FERRY, List.of(
			new TravelDestination("Corsair Cove", corsairRowboat, "Free", "The Corsair Curse")
		)));
		nodes.add(new TravelNode("Corsair Rowboat (Corsair Cove)", corsairRowboat, TravelType.FERRY, List.of(
			new TravelDestination("Port Sarim", psCorsairRowboat, "Free", "The Corsair Curse"),
			new TravelDestination("Rimmington", rimmingtonColin, "Free", "The Corsair Curse")
		)));
		nodes.add(new TravelNode("Cabin Boy Colin (Rimmington)", rimmingtonColin, TravelType.FERRY, List.of(
			new TravelDestination("Corsair Cove", corsairRowboat, "Free", "The Corsair Curse")
		)));

		// Torfinn's boat to Ungael (Vorkath)
		final WorldPoint relTorfinn = new WorldPoint(2624, 3682, 0);
		final WorldPoint ungaelPier = new WorldPoint(2272, 4040, 0);
		nodes.add(new TravelNode("Torfinn's Boat (Rellekka)", relTorfinn, TravelType.FERRY, List.of(
			new TravelDestination("Ungael (Vorkath)", ungaelPier, "Free", "Dragon Slayer II")
		)));
		nodes.add(new TravelNode("Torfinn's Boat (Ungael)", ungaelPier, TravelType.FERRY, List.of(
			new TravelDestination("Rellekka", relTorfinn, "Free", "Dragon Slayer II")
		)));

		// Larry's submarine to Iceberg
		final WorldPoint relIceberg = new WorldPoint(2707, 3735, 0);
		final WorldPoint icebergPier = new WorldPoint(2658, 3989, 0);
		nodes.add(new TravelNode("Larry's Submarine (Rellekka)", relIceberg, TravelType.FERRY, List.of(
			new TravelDestination("Iceberg", icebergPier, "Free", "Cold War")
		)));
		nodes.add(new TravelNode("Larry's Submarine (Iceberg)", icebergPier, TravelType.FERRY, List.of(
			new TravelDestination("Rellekka", relIceberg, "Free", "Cold War")
		)));

		// 50% Luke's rowboat to Braindeath Island
		final WorldPoint phasBraindeath = new WorldPoint(3680, 3475, 0);
		final WorldPoint braindeathPier = new WorldPoint(2140, 5093, 0);
		nodes.add(new TravelNode("50% Luke's Rowboat (Port Phasmatys)", phasBraindeath, TravelType.FERRY, List.of(
			new TravelDestination("Braindeath Island", braindeathPier, "Free", "Rum Deal")
		)));
		nodes.add(new TravelNode("50% Luke's Rowboat (Braindeath Island)", braindeathPier, TravelType.FERRY, List.of(
			new TravelDestination("Port Phasmatys", phasBraindeath, "Free", "Rum Deal")
		)));

		// Brother Tranquility's boat to Harmony Island
		final WorldPoint mosHarmony = new WorldPoint(3680, 2980, 0);
		final WorldPoint harmonyPier = new WorldPoint(3798, 2860, 0);
		nodes.add(new TravelNode("Brother Tranquility's Boat (Mos Le'Harmless)", mosHarmony, TravelType.FERRY, List.of(
			new TravelDestination("Harmony Island", harmonyPier, "Free", "The Great Brain Robbery")
		)));
		nodes.add(new TravelNode("Brother Tranquility's Boat (Harmony Island)", harmonyPier, TravelType.FERRY, List.of(
			new TravelDestination("Mos Le'Harmless", mosHarmony, "Free", "The Great Brain Robbery")
		)));

		// Isle of Souls ferry (Land's End)
		nodes.add(new TravelNode("Isle of Souls Ferry", isleOfSoulsPier, TravelType.FERRY, List.of(
			new TravelDestination("Land's End (Kourend)", landsEnd, "Free")
		)));

		// Antonia's Ferry: Sunset Coast ⇄ Aldarin (Varlamore Part 2)
		final WorldPoint sunsetAntonia = new WorldPoint(1539, 3037, 0);
		final WorldPoint aldarinAntonia = new WorldPoint(1456, 2972, 0);
		nodes.add(new TravelNode("Antonia's Ferry (Sunset Coast)", sunsetAntonia, TravelType.FERRY, List.of(
			new TravelDestination("Aldarin", aldarinAntonia, "20 gp", "Children of the Sun")
		)));
		nodes.add(new TravelNode("Antonia's Ferry (Aldarin)", aldarinAntonia, TravelType.FERRY, List.of(
			new TravelDestination("Sunset Coast", sunsetAntonia, "20 gp", "Children of the Sun")
		)));

		// Holgart's Boat: Witchaven ⇄ Fishing Platform
		final WorldPoint witchavenPier = new WorldPoint(2724, 3305, 0);
		final WorldPoint fishingPlatformPier = new WorldPoint(2780, 3274, 0);
		nodes.add(new TravelNode("Holgart's Boat (Witchaven)", witchavenPier, TravelType.FERRY, List.of(
			new TravelDestination("Fishing Platform", fishingPlatformPier, "Free", "Sea Slug")
		)));
		nodes.add(new TravelNode("Holgart's Boat (Fishing Platform)", fishingPlatformPier, TravelType.FERRY, List.of(
			new TravelDestination("Witchaven", witchavenPier, "Free", "Sea Slug")
		)));

		// Kathy Corkat's Boat: Tree Gnome Stronghold ⇄ Piscatoris
		final WorldPoint strongholdCorkat = new WorldPoint(2383, 3433, 0);
		final WorldPoint piscatorisCorkat = new WorldPoint(2344, 3650, 0);
		nodes.add(new TravelNode("Kathy Corkat's Boat (Tree Gnome Stronghold)", strongholdCorkat, TravelType.FERRY, List.of(
			new TravelDestination("Piscatoris Fishing Colony", piscatorisCorkat, "Free", "Swan Song")
		)));
		nodes.add(new TravelNode("Kathy Corkat's Boat (Piscatoris)", piscatorisCorkat, TravelType.FERRY, List.of(
			new TravelDestination("Tree Gnome Stronghold", strongholdCorkat, "Free", "Swan Song")
		)));

		// Ghost Captain's Rowboat: Port Phasmatys ⇄ Dragontooth Island
		final WorldPoint phasDragontooth = new WorldPoint(3704, 3488, 0);
		final WorldPoint dragontoothPier = new WorldPoint(3792, 3560, 0);
		nodes.add(new TravelNode("Ghost Captain's Rowboat (Port Phasmatys)", phasDragontooth, TravelType.FERRY, List.of(
			new TravelDestination("Dragontooth Island", dragontoothPier, "Free (or 1-5 Ecto-tokens)", "Ghosts Ahoy")
		)));
		nodes.add(new TravelNode("Ghost Captain's Rowboat (Dragontooth Island)", dragontoothPier, TravelType.FERRY, List.of(
			new TravelDestination("Port Phasmatys", phasDragontooth, "Free", "Ghosts Ahoy")
		)));

		// Rowboat to Meiyerditch: Burgh de Rott ⇄ Meiyerditch
		final WorldPoint burghRowboat = new WorldPoint(3523, 3168, 0);
		final WorldPoint meiyerditchRowboat = new WorldPoint(3597, 3171, 0);
		nodes.add(new TravelNode("Rowboat to Meiyerditch (Burgh de Rott)", burghRowboat, TravelType.FERRY, List.of(
			new TravelDestination("Meiyerditch / Icyene Graveyard", meiyerditchRowboat, "Free", "Darkness of Hallowvale")
		)));
		nodes.add(new TravelNode("Rowboat to Burgh de Rott (Meiyerditch)", meiyerditchRowboat, TravelType.FERRY, List.of(
			new TravelDestination("Burgh de Rott", burghRowboat, "Free", "Darkness of Hallowvale")
		)));
	}

	/** 3. GNOME GLIDERS (Hub: Grand Tree) */
	private static void addGnomeGliders(List<TravelNode> nodes)
	{
		final WorldPoint gtGlider = new WorldPoint(2465, 3501, 3);
		final WorldPoint wwmGlider = new WorldPoint(2848, 3498, 0);
		final WorldPoint alkGlider = new WorldPoint(3278, 3213, 0);
		final WorldPoint felGlider = new WorldPoint(2544, 2970, 0);
		final WorldPoint karGlider = new WorldPoint(2971, 2969, 0);
		final WorldPoint apeGlider = new WorldPoint(2712, 2804, 0);

		final List<TravelDestination> gtDests = List.of(
			new TravelDestination("Sindarpos (White Wolf Mountain)", wwmGlider, "Free", "The Grand Tree"),
			new TravelDestination("Kar-Hewo (Al Kharid)", alkGlider, "Free", "The Grand Tree"),
			new TravelDestination("Lemantolly Undri (Feldip Hills)", felGlider, "Free", "One Small Favour"),
			new TravelDestination("Gandius (Karamja)", karGlider, "Free", "The Grand Tree"),
			new TravelDestination("Ookookolly Undri (Ape Atoll)", apeGlider, "Free", "Monkey Madness II")
		);

		nodes.add(new TravelNode("Ta Quir Priw (Grand Tree Glider Hub)", gtGlider, TravelType.GNOME_GLIDER, gtDests));
		nodes.add(new TravelNode("Sindarpos (White Wolf Mountain Glider)", wwmGlider, TravelType.GNOME_GLIDER, List.of(
			new TravelDestination("Ta Quir Priw (Grand Tree)", gtGlider, "Free")
		)));
		nodes.add(new TravelNode("Kar-Hewo (Al Kharid Glider)", alkGlider, TravelType.GNOME_GLIDER, List.of(
			new TravelDestination("Ta Quir Priw (Grand Tree)", gtGlider, "Free")
		)));
		nodes.add(new TravelNode("Lemantolly Undri (Feldip Hills Glider)", felGlider, TravelType.GNOME_GLIDER, List.of(
			new TravelDestination("Ta Quir Priw (Grand Tree)", gtGlider, "Free")
		)));
		nodes.add(new TravelNode("Gandius (Karamja Glider)", karGlider, TravelType.GNOME_GLIDER, List.of(
			new TravelDestination("Ta Quir Priw (Grand Tree)", gtGlider, "Free")
		)));
		nodes.add(new TravelNode("Ookookolly Undri (Ape Atoll Glider)", apeGlider, TravelType.GNOME_GLIDER, List.of(
			new TravelDestination("Ta Quir Priw (Grand Tree)", gtGlider, "Free")
		)));
	}

	/** 4. SPIRIT TREES */
	private static void addSpiritTrees(List<TravelNode> nodes)
	{
		final WorldPoint stStronghold = new WorldPoint(2461, 3444, 0);
		final WorldPoint stVillage = new WorldPoint(2542, 3170, 0);
		final WorldPoint stGE = new WorldPoint(3186, 3509, 0);
		final WorldPoint stKhazard = new WorldPoint(2555, 3259, 0);
		final WorldPoint stFeldip = new WorldPoint(2340, 3160, 0);
		final WorldPoint stPrif = new WorldPoint(3274, 6064, 0);
		final WorldPoint stMyths = new WorldPoint(2488, 2850, 0);
		final WorldPoint stBrim = new WorldPoint(2800, 3204, 0);
		final WorldPoint stHosidius = new WorldPoint(1693, 3538, 0);
		final WorldPoint stPortSarim = new WorldPoint(3059, 3257, 0);
		final WorldPoint stEtceteria = new WorldPoint(2608, 3857, 0);
		final WorldPoint stGuild = new WorldPoint(1253, 3750, 0);

		final List<TravelDestination> stDests = List.of(
			new TravelDestination("Tree Gnome Stronghold", stStronghold, "Free", "Tree Gnome Village"),
			new TravelDestination("Tree Gnome Village", stVillage, "Free", "Tree Gnome Village"),
			new TravelDestination("Grand Exchange (Varrock)", stGE, "Free", "Tree Gnome Village"),
			new TravelDestination("Battlefield of Khazard", stKhazard, "Free", "Tree Gnome Village"),
			new TravelDestination("Poison Waste (Feldip)", stFeldip, "Free", "The Path of Glouphrie"),
			new TravelDestination("Prifddinas", stPrif, "Free", "Song of the Elves"),
			new TravelDestination("Feldip Hills (Myths' Guild)", stMyths, "Free", "Dragon Slayer II"),
			new TravelDestination("Brimhaven Patch", stBrim, "Free", "83 Farming"),
			new TravelDestination("Hosidius Patch", stHosidius, "Free", "83 Farming"),
			new TravelDestination("Port Sarim Patch", stPortSarim, "Free", "83 Farming"),
			new TravelDestination("Etceteria Patch", stEtceteria, "Free", "83 Farming"),
			new TravelDestination("Farming Guild", stGuild, "Free", "85 Farming")
		);

		nodes.add(new TravelNode("Spirit Tree (Grand Exchange)", stGE, TravelType.SPIRIT_TREE, stDests));
		nodes.add(new TravelNode("Spirit Tree (Tree Gnome Stronghold)", stStronghold, TravelType.SPIRIT_TREE, stDests));
		nodes.add(new TravelNode("Spirit Tree (Tree Gnome Village)", stVillage, TravelType.SPIRIT_TREE, stDests));
		nodes.add(new TravelNode("Spirit Tree (Battlefield of Khazard)", stKhazard, TravelType.SPIRIT_TREE, stDests));
		nodes.add(new TravelNode("Spirit Tree (Poison Waste)", stFeldip, TravelType.SPIRIT_TREE, stDests));
		nodes.add(new TravelNode("Spirit Tree (Prifddinas)", stPrif, TravelType.SPIRIT_TREE, stDests));
		nodes.add(new TravelNode("Spirit Tree (Myths' Guild)", stMyths, TravelType.SPIRIT_TREE, stDests));
		nodes.add(new TravelNode("Spirit Tree (Brimhaven)", stBrim, TravelType.SPIRIT_TREE, stDests));
		nodes.add(new TravelNode("Spirit Tree (Hosidius)", stHosidius, TravelType.SPIRIT_TREE, stDests));
		nodes.add(new TravelNode("Spirit Tree (Port Sarim)", stPortSarim, TravelType.SPIRIT_TREE, stDests));
		nodes.add(new TravelNode("Spirit Tree (Etceteria)", stEtceteria, TravelType.SPIRIT_TREE, stDests));
		nodes.add(new TravelNode("Spirit Tree (Farming Guild)", stGuild, TravelType.SPIRIT_TREE, stDests));
	}

	/** 5. FAIRY RINGS (Zanaris Hub + Key Rings) */
	private static void addFairyRings(List<TravelNode> nodes)
	{
		final WorldPoint frZanaris = new WorldPoint(2412, 4434, 0);
		final WorldPoint frEdgeville = new WorldPoint(3129, 3496, 0);
		final WorldPoint frTowerLife = new WorldPoint(2658, 3230, 0);
		final WorldPoint frCanifis = new WorldPoint(3447, 3470, 0);
		final WorldPoint frMortMyre = new WorldPoint(3469, 3431, 0);
		final WorldPoint frWizards = new WorldPoint(3108, 3149, 0);
		final WorldPoint frMiscellania = new WorldPoint(2513, 3884, 0);
		final WorldPoint frLegends = new WorldPoint(2740, 3351, 0);
		final WorldPoint frSinclair = new WorldPoint(2705, 3576, 0);
		final WorldPoint frYanille = new WorldPoint(2528, 3127, 0);
		final WorldPoint frPiscatoris = new WorldPoint(2319, 3619, 0);
		final WorldPoint frArceuus = new WorldPoint(1639, 3868, 0);
		final WorldPoint frChasm = new WorldPoint(1455, 3658, 0);
		final WorldPoint frKaruulm = new WorldPoint(1302, 3762, 0);
		final WorldPoint frQuidamortem = new WorldPoint(1295, 3493, 0);
		final WorldPoint frAldarin = new WorldPoint(1359, 2940, 0);
		final WorldPoint frTwilight = new WorldPoint(1427, 3266, 0);
		final WorldPoint frWyrm = new WorldPoint(1720, 2950, 0);

		final List<TravelDestination> frDests = List.of(
			new TravelDestination("Zanaris Main Hub", frZanaris, "Free", "Fairytale II"),
			new TravelDestination("DKR (Edgeville)", frEdgeville, "Free"),
			new TravelDestination("DJP (Tower of Life / Ardougne)", frTowerLife, "Free"),
			new TravelDestination("CKS (Canifis)", frCanifis, "Free"),
			new TravelDestination("BKR (Mort Myre Swamp)", frMortMyre, "Free"),
			new TravelDestination("DIS (Wizards' Tower / Draynor)", frWizards, "Free"),
			new TravelDestination("CIP (Miscellania)", frMiscellania, "Free"),
			new TravelDestination("BLR (Legends' Guild)", frLegends, "Free"),
			new TravelDestination("CJR (Sinclair Mansion / Seers)", frSinclair, "Free"),
			new TravelDestination("CIQ (Yanille / Feldip)", frYanille, "Free"),
			new TravelDestination("AKQ (Piscatoris Fishing Colony)", frPiscatoris, "Free"),
			new TravelDestination("CIS (Arceuus Library)", frArceuus, "Free"),
			new TravelDestination("DJR (Chasm of Fire)", frChasm, "Free"),
			new TravelDestination("CIR (South of Mount Karuulm)", frKaruulm, "Free"),
			new TravelDestination("BLS (Mount Quidamortem / CoX)", frQuidamortem, "Free"),
			new TravelDestination("CKQ (Aldarin)", frAldarin, "Free"),
			new TravelDestination("ALQ (Twilight Temple / Ralos' Rise)", frTwilight, "Free"),
			new TravelDestination("AJP (Avium Savannah / Colossal Wyrm)", frWyrm, "Free")
		);

		nodes.add(new TravelNode("Fairy Ring (Zanaris)", frZanaris, TravelType.FAIRY_RING, frDests));
		nodes.add(new TravelNode("Fairy Ring DKR (Edgeville)", frEdgeville, TravelType.FAIRY_RING, frDests));
		nodes.add(new TravelNode("Fairy Ring DJP (Tower of Life)", frTowerLife, TravelType.FAIRY_RING, frDests));
		nodes.add(new TravelNode("Fairy Ring CKS (Canifis)", frCanifis, TravelType.FAIRY_RING, frDests));
		nodes.add(new TravelNode("Fairy Ring BKR (Mort Myre Swamp)", frMortMyre, TravelType.FAIRY_RING, frDests));
		nodes.add(new TravelNode("Fairy Ring DIS (Wizards' Tower)", frWizards, TravelType.FAIRY_RING, frDests));
		nodes.add(new TravelNode("Fairy Ring CIP (Miscellania)", frMiscellania, TravelType.FAIRY_RING, frDests));
		nodes.add(new TravelNode("Fairy Ring BLR (Legends' Guild)", frLegends, TravelType.FAIRY_RING, frDests));
		nodes.add(new TravelNode("Fairy Ring CJR (Sinclair Mansion)", frSinclair, TravelType.FAIRY_RING, frDests));
		nodes.add(new TravelNode("Fairy Ring CIQ (Yanille)", frYanille, TravelType.FAIRY_RING, frDests));
		nodes.add(new TravelNode("Fairy Ring AKQ (Piscatoris Colony)", frPiscatoris, TravelType.FAIRY_RING, frDests));
		nodes.add(new TravelNode("Fairy Ring CIS (Arceuus Library)", frArceuus, TravelType.FAIRY_RING, frDests));
		nodes.add(new TravelNode("Fairy Ring DJR (Chasm of Fire)", frChasm, TravelType.FAIRY_RING, frDests));
		nodes.add(new TravelNode("Fairy Ring CIR (South of Mount Karuulm)", frKaruulm, TravelType.FAIRY_RING, frDests));
		nodes.add(new TravelNode("Fairy Ring BLS (Mount Quidamortem)", frQuidamortem, TravelType.FAIRY_RING, frDests));
		nodes.add(new TravelNode("Fairy Ring CKQ (Aldarin)", frAldarin, TravelType.FAIRY_RING, frDests));
		nodes.add(new TravelNode("Fairy Ring ALQ (Twilight Temple)", frTwilight, TravelType.FAIRY_RING, frDests));
		nodes.add(new TravelNode("Fairy Ring AJP (Avium Savannah)", frWyrm, TravelType.FAIRY_RING, frDests));
	}

	/** 6. HOT AIR BALLOONS (Enlightened Journey) */
	private static void addHotAirBalloons(List<TravelNode> nodes)
	{
		final WorldPoint habEntrana = new WorldPoint(2809, 3356, 0);
		final WorldPoint habTaverley = new WorldPoint(2936, 3422, 0);
		final WorldPoint habCrafting = new WorldPoint(2924, 3300, 0);
		final WorldPoint habVarrock = new WorldPoint(3298, 3481, 0);
		final WorldPoint habCastleWars = new WorldPoint(2464, 3108, 0);
		final WorldPoint habGrandTree = new WorldPoint(2478, 3459, 0);

		final List<TravelDestination> habDests = List.of(
			new TravelDestination("Entrana (Hub)", habEntrana, "1 Normal log", "Enlightened Journey"),
			new TravelDestination("Taverley", habTaverley, "1 Normal log", "20 Firemaking"),
			new TravelDestination("Crafting Guild (Falador)", habCrafting, "1 Oak log", "30 Firemaking"),
			new TravelDestination("Varrock (Lumber Yard)", habVarrock, "1 Willow log", "40 Firemaking"),
			new TravelDestination("Castle Wars", habCastleWars, "1 Yew log", "50 Firemaking"),
			new TravelDestination("Grand Tree", habGrandTree, "1 Magic log", "60 Firemaking")
		);

		nodes.add(new TravelNode("Hot Air Balloon (Entrana Hub)", habEntrana, TravelType.HOT_AIR_BALLOON, habDests));
		nodes.add(new TravelNode("Hot Air Balloon (Taverley)", habTaverley, TravelType.HOT_AIR_BALLOON, habDests));
		nodes.add(new TravelNode("Hot Air Balloon (Crafting Guild)", habCrafting, TravelType.HOT_AIR_BALLOON, habDests));
		nodes.add(new TravelNode("Hot Air Balloon (Varrock)", habVarrock, TravelType.HOT_AIR_BALLOON, habDests));
		nodes.add(new TravelNode("Hot Air Balloon (Castle Wars)", habCastleWars, TravelType.HOT_AIR_BALLOON, habDests));
		nodes.add(new TravelNode("Hot Air Balloon (Grand Tree)", habGrandTree, TravelType.HOT_AIR_BALLOON, habDests));
	}

	/** 7. CANOES (River Lum) */
	private static void addCanoes(List<TravelNode> nodes)
	{
		final WorldPoint cnLumbridge = new WorldPoint(3241, 3237, 0);
		final WorldPoint cnChampions = new WorldPoint(3200, 3343, 0);
		final WorldPoint cnBarbarian = new WorldPoint(3110, 3409, 0);
		final WorldPoint cnEdgeville = new WorldPoint(3130, 3508, 0);
		final WorldPoint cnWilderness = new WorldPoint(3143, 3797, 0);

		final List<TravelDestination> cnDests = List.of(
			new TravelDestination("Lumbridge Station", cnLumbridge, "Free (Woodcutting Axe)", "12 Woodcutting (Log)"),
			new TravelDestination("Champions' Guild Station", cnChampions, "Free (Woodcutting Axe)", "27 Woodcutting (Dugout)"),
			new TravelDestination("Barbarian Village Station", cnBarbarian, "Free (Woodcutting Axe)", "42 Woodcutting (Stable)"),
			new TravelDestination("Edgeville Station", cnEdgeville, "Free (Woodcutting Axe)", "42 Woodcutting (Stable)"),
			new TravelDestination("Wilderness (Level 35)", cnWilderness, "Free (Woodcutting Axe)", "57 Woodcutting (Waka)")
		);

		nodes.add(new TravelNode("Canoe Station (Lumbridge)", cnLumbridge, TravelType.CANOE, cnDests));
		nodes.add(new TravelNode("Canoe Station (Champions' Guild)", cnChampions, TravelType.CANOE, cnDests));
		nodes.add(new TravelNode("Canoe Station (Barbarian Village)", cnBarbarian, TravelType.CANOE, cnDests));
		nodes.add(new TravelNode("Canoe Station (Edgeville)", cnEdgeville, TravelType.CANOE, cnDests));
		nodes.add(new TravelNode("Canoe Station (Wilderness)", cnWilderness, TravelType.CANOE, cnDests));
	}

	/** 8. MINE CARTS (Lovakengj Network, Keldagrim, Shilo) */
	private static void addMineCarts(List<TravelNode> nodes)
	{
		final WorldPoint mcLovakengj = new WorldPoint(1518, 3737, 0);
		final WorldPoint mcArceuus = new WorldPoint(1670, 3832, 0);
		final WorldPoint mcShayzien = new WorldPoint(1492, 3615, 0);
		final WorldPoint mcHosidius = new WorldPoint(1656, 3543, 0);
		final WorldPoint mcPisc = new WorldPoint(1797, 3758, 0);
		final WorldPoint mcFarmGuild = new WorldPoint(1238, 3724, 0);
		final WorldPoint mcHosSouth = new WorldPoint(1770, 3505, 0);
		final WorldPoint mcKingstown = new WorldPoint(1680, 3678, 0);
		final WorldPoint mcWoodland = new WorldPoint(1579, 3433, 0);
		final WorldPoint mcTundras = new WorldPoint(1636, 3939, 0);
		final WorldPoint mcShayWest = new WorldPoint(1417, 3574, 0);
		final WorldPoint mcQuidamortem = new WorldPoint(1247, 3560, 0);

		final String mcKourendCost = "20 gp / Free with The Forsaken Tower";
		final List<TravelDestination> mcKourendDests = List.of(
			new TravelDestination("Lovakengj Central Station", mcLovakengj, mcKourendCost),
			new TravelDestination("Arceuus Station", mcArceuus, mcKourendCost),
			new TravelDestination("Shayzien East Station", mcShayzien, mcKourendCost),
			new TravelDestination("Hosidius Station", mcHosidius, mcKourendCost),
			new TravelDestination("Port Piscarilius Station", mcPisc, mcKourendCost),
			new TravelDestination("Farming Guild Station", mcFarmGuild, mcKourendCost),
			new TravelDestination("Hosidius South Station (Tithe Farm)", mcHosSouth, mcKourendCost),
			new TravelDestination("Kingstown Station", mcKingstown, mcKourendCost),
			new TravelDestination("Kourend Woodland Station", mcWoodland, mcKourendCost),
			new TravelDestination("Northern Tundras Station (Wintertodt)", mcTundras, mcKourendCost),
			new TravelDestination("Shayzien West Station (Wall)", mcShayWest, mcKourendCost),
			new TravelDestination("Mount Quidamortem Station", mcQuidamortem, mcKourendCost)
		);

		nodes.add(new TravelNode("Mine Cart (Lovakengj Central)", mcLovakengj, TravelType.MINE_CART, mcKourendDests));
		nodes.add(new TravelNode("Mine Cart (Arceuus)", mcArceuus, TravelType.MINE_CART, mcKourendDests));
		nodes.add(new TravelNode("Mine Cart (Shayzien East)", mcShayzien, TravelType.MINE_CART, mcKourendDests));
		nodes.add(new TravelNode("Mine Cart (Hosidius)", mcHosidius, TravelType.MINE_CART, mcKourendDests));
		nodes.add(new TravelNode("Mine Cart (Port Piscarilius)", mcPisc, TravelType.MINE_CART, mcKourendDests));
		nodes.add(new TravelNode("Mine Cart (Farming Guild)", mcFarmGuild, TravelType.MINE_CART, mcKourendDests));
		nodes.add(new TravelNode("Mine Cart (Hosidius South)", mcHosSouth, TravelType.MINE_CART, mcKourendDests));
		nodes.add(new TravelNode("Mine Cart (Kingstown)", mcKingstown, TravelType.MINE_CART, mcKourendDests));
		nodes.add(new TravelNode("Mine Cart (Kourend Woodland)", mcWoodland, TravelType.MINE_CART, mcKourendDests));
		nodes.add(new TravelNode("Mine Cart (Northern Tundras)", mcTundras, TravelType.MINE_CART, mcKourendDests));
		nodes.add(new TravelNode("Mine Cart (Shayzien West)", mcShayWest, TravelType.MINE_CART, mcKourendDests));
		nodes.add(new TravelNode("Mine Cart (Mount Quidamortem)", mcQuidamortem, TravelType.MINE_CART, mcKourendDests));

		// Shilo Village Cart
		final WorldPoint mcBrimCart = new WorldPoint(2758, 3214, 0);
		final WorldPoint mcShilo = new WorldPoint(2834, 2954, 0);
		nodes.add(new TravelNode("Brimhaven Cart to Shilo", mcBrimCart, TravelType.MINE_CART, List.of(
			new TravelDestination("Shilo Village", mcShilo, "10 - 200 gp", "Shilo Village quest")
		)));
		nodes.add(new TravelNode("Shilo Village Cart to Brimhaven", mcShilo, TravelType.MINE_CART, List.of(
			new TravelDestination("Brimhaven", mcBrimCart, "10 - 200 gp", "Shilo Village quest")
		)));

		// Keldagrim Minecart System
		final WorldPoint mcKeldHub = new WorldPoint(2910, 10178, 0);
		final WorldPoint mcGrandExchange = new WorldPoint(3140, 3504, 0);
		final WorldPoint mcIceMountain = new WorldPoint(3019, 9827, 0);
		final WorldPoint mcWhiteWolf = new WorldPoint(2874, 9868, 0);

		nodes.add(new TravelNode("Mine Cart (Keldagrim Hub)", mcKeldHub, TravelType.MINE_CART, List.of(
			new TravelDestination("Grand Exchange (Trapdoor)", mcGrandExchange, "Free", "The Giant Dwarf"),
			new TravelDestination("Ice Mountain / Dwarven Mines", mcIceMountain, "150 gp", "The Giant Dwarf"),
			new TravelDestination("White Wolf Mountain", mcWhiteWolf, "150 gp", "Fishing Contest")
		)));
		nodes.add(new TravelNode("Mine Cart (Grand Exchange)", mcGrandExchange, TravelType.MINE_CART, List.of(
			new TravelDestination("Keldagrim Hub", mcKeldHub, "Free", "The Giant Dwarf")
		)));
		nodes.add(new TravelNode("Mine Cart (Ice Mountain)", mcIceMountain, TravelType.MINE_CART, List.of(
			new TravelDestination("Keldagrim Hub", mcKeldHub, "150 gp", "The Giant Dwarf")
		)));
		nodes.add(new TravelNode("Mine Cart (White Wolf Mountain)", mcWhiteWolf, TravelType.MINE_CART, List.of(
			new TravelDestination("Keldagrim Hub", mcKeldHub, "150 gp", "Fishing Contest")
		)));
	}

	/** 9. MAGIC CARPETS (Kharidian Desert) */
	private static void addMagicCarpets(List<TravelNode> nodes)
	{
		final WorldPoint carpShantay = new WorldPoint(3308, 3110, 0);
		final WorldPoint carpPolN = new WorldPoint(3351, 3002, 0);
		final WorldPoint carpPolS = new WorldPoint(3351, 2942, 0);
		final WorldPoint carpNardah = new WorldPoint(3400, 2916, 0);
		final WorldPoint carpBedabin = new WorldPoint(3180, 3042, 0);
		final WorldPoint carpSoph = new WorldPoint(3286, 2813, 0);
		final WorldPoint carpMenaphos = new WorldPoint(3232, 2813, 0);
		final WorldPoint carpUzer = new WorldPoint(3469, 3112, 0);
		final WorldPoint carpUnkah = new WorldPoint(3146, 2840, 0);

		nodes.add(new TravelNode("Magic Carpet (Shantay Pass)", carpShantay, TravelType.MAGIC_CARPET, List.of(
			new TravelDestination("Pollnivneach (North)", carpPolN, "200 gp (100 gp with Ring of Charos)"),
			new TravelDestination("Bedabin Camp", carpBedabin, "200 gp"),
			new TravelDestination("Uzer", carpUzer, "200 gp", "The Golem")
		)));
		nodes.add(new TravelNode("Magic Carpet (Pollnivneach North)", carpPolN, TravelType.MAGIC_CARPET, List.of(
			new TravelDestination("Shantay Pass (Al Kharid)", carpShantay, "200 gp")
		)));
		nodes.add(new TravelNode("Magic Carpet (Pollnivneach South)", carpPolS, TravelType.MAGIC_CARPET, List.of(
			new TravelDestination("Nardah", carpNardah, "200 gp"),
			new TravelDestination("Bedabin Camp", carpBedabin, "200 gp"),
			new TravelDestination("Sophanem", carpSoph, "200 gp", "Icthlarin's Little Helper"),
			new TravelDestination("Menaphos Gates", carpMenaphos, "200 gp", "Icthlarin's Little Helper"),
			new TravelDestination("Ruins of Unkah", carpUnkah, "200 gp")
		)));
		nodes.add(new TravelNode("Magic Carpet (Nardah)", carpNardah, TravelType.MAGIC_CARPET, List.of(
			new TravelDestination("Pollnivneach (South)", carpPolS, "200 gp")
		)));
		nodes.add(new TravelNode("Magic Carpet (Bedabin Camp)", carpBedabin, TravelType.MAGIC_CARPET, List.of(
			new TravelDestination("Pollnivneach (South)", carpPolS, "200 gp"),
			new TravelDestination("Shantay Pass (Al Kharid)", carpShantay, "200 gp")
		)));
		nodes.add(new TravelNode("Magic Carpet (Sophanem)", carpSoph, TravelType.MAGIC_CARPET, List.of(
			new TravelDestination("Pollnivneach (South)", carpPolS, "200 gp", "Icthlarin's Little Helper")
		)));
		nodes.add(new TravelNode("Magic Carpet (Menaphos Gates)", carpMenaphos, TravelType.MAGIC_CARPET, List.of(
			new TravelDestination("Pollnivneach (South)", carpPolS, "200 gp", "Icthlarin's Little Helper")
		)));
		nodes.add(new TravelNode("Magic Carpet (Uzer)", carpUzer, TravelType.MAGIC_CARPET, List.of(
			new TravelDestination("Shantay Pass (Al Kharid)", carpShantay, "200 gp", "The Golem")
		)));
		nodes.add(new TravelNode("Magic Carpet (Ruins of Unkah)", carpUnkah, TravelType.MAGIC_CARPET, List.of(
			new TravelDestination("Pollnivneach (South)", carpPolS, "200 gp")
		)));
	}

	/** 10. QUETZAL TRANSPORT (Varlamore) */
	private static void addQuetzals(List<TravelNode> nodes)
	{
		final WorldPoint qzCivitas = new WorldPoint(1696, 3138, 0);
		final WorldPoint qzHunter = new WorldPoint(1557, 3049, 0);
		final WorldPoint qzAldarin = new WorldPoint(1420, 2930, 0);
		final WorldPoint qzCamTorum = new WorldPoint(1435, 3131, 0);
		final WorldPoint qzColosseum = new WorldPoint(1794, 3107, 0);
		final WorldPoint qzTwilight = new WorldPoint(1460, 3280, 0);
		final WorldPoint qzSunset = new WorldPoint(1543, 3016, 0);
		final WorldPoint qzTeomat = new WorldPoint(1445, 3171, 0);
		final WorldPoint qzOuterFortis = new WorldPoint(1665, 3004, 0);
		final WorldPoint qzWyrm = new WorldPoint(1705, 2911, 0);
		final WorldPoint qzSalvager = new WorldPoint(1610, 3307, 0);
		final WorldPoint qzAuburnvale = new WorldPoint(1368, 3350, 0);

		final String qzWhistleCost = "Free (or Quetzal Whistle)";
		final List<TravelDestination> qzDests = List.of(
			new TravelDestination("Civitas illa Fortis (Central Nest)", qzCivitas, qzWhistleCost, "Children of the Sun"),
			new TravelDestination("Hunter Guild Nest", qzHunter, qzWhistleCost, "Hunter Rumours"),
			new TravelDestination("Aldarin Nest", qzAldarin, qzWhistleCost),
			new TravelDestination("Cam Torum Nest", qzCamTorum, qzWhistleCost, "Perilous Moons"),
			new TravelDestination("Fortis Colosseum Nest", qzColosseum, qzWhistleCost),
			new TravelDestination("Twilight Temple / Ralo's Rise", qzTwilight, qzWhistleCost),
			new TravelDestination("Sunset Coast Nest", qzSunset, qzWhistleCost),
			new TravelDestination("The Teomat Nest", qzTeomat, qzWhistleCost),
			new TravelDestination("Outer Fortis / Avium Savannah Nest", qzOuterFortis, qzWhistleCost),
			new TravelDestination("Colossal Wyrm Remains Nest", qzWyrm, qzWhistleCost),
			new TravelDestination("Salvager Overlook Nest", qzSalvager, qzWhistleCost),
			new TravelDestination("Auburnvale Nest", qzAuburnvale, qzWhistleCost)
		);

		nodes.add(new TravelNode("Quetzal Nest (Civitas illa Fortis)", qzCivitas, TravelType.QUETZAL, qzDests));
		nodes.add(new TravelNode("Quetzal Nest (Hunter Guild)", qzHunter, TravelType.QUETZAL, qzDests));
		nodes.add(new TravelNode("Quetzal Nest (Aldarin)", qzAldarin, TravelType.QUETZAL, qzDests));
		nodes.add(new TravelNode("Quetzal Nest (Cam Torum)", qzCamTorum, TravelType.QUETZAL, qzDests));
		nodes.add(new TravelNode("Quetzal Nest (Fortis Colosseum)", qzColosseum, TravelType.QUETZAL, qzDests));
		nodes.add(new TravelNode("Quetzal Nest (Twilight Temple)", qzTwilight, TravelType.QUETZAL, qzDests));
		nodes.add(new TravelNode("Quetzal Nest (Sunset Coast)", qzSunset, TravelType.QUETZAL, qzDests));
		nodes.add(new TravelNode("Quetzal Nest (The Teomat)", qzTeomat, TravelType.QUETZAL, qzDests));
		nodes.add(new TravelNode("Quetzal Nest (Outer Fortis)", qzOuterFortis, TravelType.QUETZAL, qzDests));
		nodes.add(new TravelNode("Quetzal Nest (Colossal Wyrm Remains)", qzWyrm, TravelType.QUETZAL, qzDests));
		nodes.add(new TravelNode("Quetzal Nest (Salvager Overlook)", qzSalvager, TravelType.QUETZAL, qzDests));
		nodes.add(new TravelNode("Quetzal Nest (Auburnvale)", qzAuburnvale, TravelType.QUETZAL, qzDests));
	}

	/** 11. SWAMP and RIVER BOATS */
	private static void addSwampAndRiverBoats(List<TravelNode> nodes)
	{
		final WorldPoint swMortton = new WorldPoint(3522, 3285, 0);
		final WorldPoint swSwamp = new WorldPoint(3439, 3290, 0);
		final WorldPoint swFossilCamp = new WorldPoint(3714, 3817, 0);
		final WorldPoint swVolcanic = new WorldPoint(3814, 3804, 0);
		final WorldPoint swDerelict = new WorldPoint(3782, 3870, 0);
		final WorldPoint swDigsite = new WorldPoint(3375, 3445, 0);

		nodes.add(new TravelNode("Mort'ton Swamp Boathouse", swMortton, TravelType.SWAMP_BOAT, List.of(
			new TravelDestination("Mort Myre Swamp Pier", swSwamp, "10 gp / 3 Nails")
		)));
		nodes.add(new TravelNode("Mort Myre Swamp Pier", swSwamp, TravelType.SWAMP_BOAT, List.of(
			new TravelDestination("Mort'ton Boathouse", swMortton, "10 gp / 3 Nails")
		)));
		nodes.add(new TravelNode("Digsite Canal Barge", swDigsite, TravelType.SWAMP_BOAT, List.of(
			new TravelDestination("Fossil Island Base Camp", swFossilCamp, "Free", "Bone Voyage")
		)));
		nodes.add(new TravelNode("Fossil Island Barge (Base Camp)", swFossilCamp, TravelType.SWAMP_BOAT, List.of(
			new TravelDestination("Digsite Canal", swDigsite, "Free", "Bone Voyage"),
			new TravelDestination("Volcanic Mine Island", swVolcanic, "Free", "Bone Voyage"),
			new TravelDestination("Derelict Sea Ship", swDerelict, "Free")
		)));
		nodes.add(new TravelNode("Volcanic Mine Island Barge", swVolcanic, TravelType.SWAMP_BOAT, List.of(
			new TravelDestination("Fossil Island Base Camp", swFossilCamp, "Free", "Bone Voyage")
		)));
		nodes.add(new TravelNode("Derelict Sea Ship Barge", swDerelict, TravelType.SWAMP_BOAT, List.of(
			new TravelDestination("Fossil Island Base Camp", swFossilCamp, "Free")
		)));
	}

	/**
	 * True when a travel station should be visible or hoverable on the given map plane.
	 * Gnome glider hubs on plane 3 are shown on the surface world map (plane 0).
	 */
	public static boolean matchesPlane(TravelNode node, int viewPlane)
	{
		final int nodePlane = node.getLocation().getPlane();
		if (nodePlane == viewPlane)
		{
			return true;
		}
		return viewPlane == 0 && node.getType() == TravelType.GNOME_GLIDER && nodePlane == 3;
	}

	/** True when two travel coordinates refer to the same map tile. */
	public static boolean sameLocation(WorldPoint a, WorldPoint b)
	{
		return a.getX() == b.getX() && a.getY() == b.getY() && a.getPlane() == b.getPlane();
	}

	/**
	 * Finds the nearest travel node to a given world coordinate within maxRadius tiles.
	 */
	public static TravelNode findNodeNear(int worldX, int worldY, int plane, int maxRadius)
	{
		TravelNode best = null;
		int bestDistSq = maxRadius * maxRadius;

		for (TravelNode node : ALL_NODES)
		{
			if (!matchesPlane(node, plane))
			{
				continue;
			}

			final int dx = node.getLocation().getX() - worldX;
			final int dy = node.getLocation().getY() - worldY;
			if (Math.abs(dx) > maxRadius || Math.abs(dy) > maxRadius)
			{
				continue;
			}
			final int distSq = dx * dx + dy * dy;

			if (distSq <= bestDistSq)
			{
				bestDistSq = distSq;
				best = node;
			}
		}

		return best;
	}
}
