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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.runelite.api.coords.WorldPoint;
import lombok.extern.slf4j.Slf4j;

/**
 * The fixed travel networks: charter ships, passenger ferries, gnome gliders, spirit trees,
 * fairy rings, hot air balloons, canoes, mine carts, magic carpets, quetzals and swamp boats,
 * with their world coordinates, destinations, prices and requirements.
 */
@Slf4j
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
		private final double displayOffsetX;
		private final double displayOffsetY;

		public TravelNode(String name, WorldPoint location, TravelType type, List<TravelDestination> destinations,
			double displayOffsetX, double displayOffsetY)
		{
			this.name = name;
			this.location = location;
			this.type = type;
			this.destinations = Collections.unmodifiableList(new ArrayList<>(destinations));
			this.displayOffsetX = displayOffsetX;
			this.displayOffsetY = displayOffsetY;
		}

		public TravelNode(String name, WorldPoint location, TravelType type, List<TravelDestination> destinations)
		{
			this(name, location, type, destinations, 0.0, 0.0);
		}

		public double getDisplayOffsetX()
		{
			return displayOffsetX;
		}

		public double getDisplayOffsetY()
		{
			return displayOffsetY;
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

	public static volatile List<TravelNode> ALL_NODES = Collections.emptyList();
	private static volatile boolean loaded;

	/** Load on the startup worker; rendering only reads the published station list. */
	public static void load()
	{
		if (loaded)
		{
			return;
		}
		try (InputStream stream = TravelData.class.getResourceAsStream(
			"/com/bettermap/poi/travel-networks.json"))
		{
			if (stream == null)
			{
				throw new IOException("Missing travel networks resource");
			}
			final JsonObject dataset = new JsonParser().parse(
				new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
			final List<List<TravelDestination>> networks = new ArrayList<>();
			for (JsonElement group : dataset.getAsJsonArray("destinations"))
			{
				if (Thread.currentThread().isInterrupted())
				{
					return;
				}
				final List<TravelDestination> destinations = new ArrayList<>();
				for (JsonElement element : group.getAsJsonArray())
				{
					final JsonObject row = element.getAsJsonObject();
					final JsonElement requirement = row.get("requirement");
					destinations.add(new TravelDestination(row.get("name").getAsString(),
						readPoint(row.getAsJsonArray("location")), row.get("cost").getAsString(),
						requirement.isJsonNull() ? null : requirement.getAsString()));
				}
				networks.add(destinations);
			}
			final List<TravelNode> nodes = new ArrayList<>();
			for (JsonElement element : dataset.getAsJsonArray("nodes"))
			{
				if (Thread.currentThread().isInterrupted())
				{
					return;
				}
				final JsonObject row = element.getAsJsonObject();
				nodes.add(new TravelNode(row.get("name").getAsString(),
					readPoint(row.getAsJsonArray("location")), TravelType.valueOf(row.get("type").getAsString()),
					networks.get(row.get("destinations").getAsInt()),
					row.get("displayOffsetX").getAsDouble(), row.get("displayOffsetY").getAsDouble()));
			}
			if (Thread.currentThread().isInterrupted())
			{
				return;
			}
			ALL_NODES = Collections.unmodifiableList(nodes);
			loaded = true;
		}
		catch (IOException | RuntimeException e)
		{
			log.debug("Could not load travel networks", e);
		}
	}

	private static WorldPoint readPoint(JsonArray point)
	{
		if (point.size() != 3)
		{
			throw new IllegalArgumentException("Travel coordinates must contain x, y and plane");
		}
		return new WorldPoint(point.get(0).getAsInt(), point.get(1).getAsInt(), point.get(2).getAsInt());
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
	public static TravelNode findNodeNear(double worldX, double worldY, int plane, double maxRadius)
	{
		TravelNode best = null;
		double bestDistSq = maxRadius * maxRadius;

		for (TravelNode node : ALL_NODES)
		{
			if (!matchesPlane(node, plane))
			{
				continue;
			}

			final double nx = node.getLocation().getX() + 0.5 + node.getDisplayOffsetX();
			final double ny = node.getLocation().getY() + 0.5 + node.getDisplayOffsetY();
			final double dx = nx - worldX;
			final double dy = ny - worldY;
			if (Math.abs(dx) > maxRadius || Math.abs(dy) > maxRadius)
			{
				continue;
			}
			final double distSq = dx * dx + dy * dy;

			if (distSq <= bestDistSq)
			{
				bestDistSq = distSq;
				best = node;
			}
		}

		return best;
	}

	public static TravelNode findNodeNear(int worldX, int worldY, int plane, int maxRadius)
	{
		return findNodeNear(worldX + 0.5, worldY + 0.5, plane, (double) maxRadius);
	}
}
