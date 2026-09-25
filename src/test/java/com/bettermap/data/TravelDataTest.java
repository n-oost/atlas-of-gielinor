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

import com.bettermap.data.TravelData.TravelDestination;
import com.bettermap.data.TravelData.TravelNode;
import com.bettermap.data.TravelData.TravelType;
import java.util.EnumSet;
import java.util.Set;
import net.runelite.api.coords.WorldPoint;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TravelDataTest
{
	@Test
	public void testAllNodesAndTypesIntegrity()
	{
		assertFalse("ALL_NODES should not be empty", TravelData.ALL_NODES.isEmpty());

		final Set<TravelType> representedTypes = EnumSet.noneOf(TravelType.class);

		for (TravelNode node : TravelData.ALL_NODES)
		{
			assertNotNull("Node should not be null", node);
			assertNotNull("Node name should not be null", node.getName());
			assertFalse("Node name should not be blank", node.getName().trim().isEmpty());

			assertNotNull("Node type should not be null for " + node.getName(), node.getType());
			representedTypes.add(node.getType());

			assertNotNull("Node location should not be null for " + node.getName(), node.getLocation());
			assertTrue("Node X out of bounds: " + node.getLocation().getX() + " for " + node.getName(),
				node.getLocation().getX() >= 1000 && node.getLocation().getX() <= 4200);
			assertTrue("Node Y out of bounds: " + node.getLocation().getY() + " for " + node.getName(),
				node.getLocation().getY() >= 2000 && node.getLocation().getY() <= 13000);
			assertTrue("Node plane out of bounds: " + node.getLocation().getPlane() + " for " + node.getName(),
				node.getLocation().getPlane() >= 0 && node.getLocation().getPlane() <= 3);

			assertNotNull("Destinations list should not be null for " + node.getName(), node.getDestinations());
			assertFalse("Destinations list should not be empty for " + node.getName(), node.getDestinations().isEmpty());

			for (TravelDestination dest : node.getDestinations())
			{
				assertNotNull("Destination should not be null in " + node.getName(), dest);
				assertNotNull("Destination name should not be null in " + node.getName(), dest.getName());
				assertFalse("Destination name should not be blank in " + node.getName(), dest.getName().trim().isEmpty());

				assertNotNull("Destination location should not be null for " + dest.getName() + " in " + node.getName(),
					dest.getLocation());
				assertTrue("Destination X out of bounds: " + dest.getLocation().getX() + " for " + dest.getName(),
					dest.getLocation().getX() >= 1000 && dest.getLocation().getX() <= 4200);
				assertTrue("Destination Y out of bounds: " + dest.getLocation().getY() + " for " + dest.getName(),
					dest.getLocation().getY() >= 2000 && dest.getLocation().getY() <= 13000);
				assertTrue("Destination plane out of bounds: " + dest.getLocation().getPlane() + " for " + dest.getName(),
					dest.getLocation().getPlane() >= 0 && dest.getLocation().getPlane() <= 3);

				assertNotNull("Destination cost should not be null for " + dest.getName() + " in " + node.getName(),
					dest.getCost());
				assertFalse("Destination cost should not be blank for " + dest.getName() + " in " + node.getName(),
					dest.getCost().trim().isEmpty());
			}
		}

		for (TravelType type : TravelType.values())
		{
			assertTrue("Every TravelType should have at least one node in ALL_NODES: missing " + type.name(),
				representedTypes.contains(type));
			assertNotNull("TravelType display name should not be null", type.getDisplayName());
			assertFalse("TravelType display name should not be blank", type.getDisplayName().trim().isEmpty());
			assertNotNull("TravelType primary color should not be null", type.getPrimaryColor());
			assertNotNull("TravelType highlight color should not be null", type.getHighlightColor());
		}
	}

	@Test
	public void testMatchesPlaneAndSameLocation()
	{
		final TravelNode gliderHub = TravelData.ALL_NODES.stream()
			.filter(n -> n.getName().contains("Grand Tree Glider Hub"))
			.findFirst()
			.orElse(null);
		assertNotNull("Grand Tree glider hub should exist", gliderHub);
		assertTrue("Glider hub on plane 3 should match surface view plane 0", TravelData.matchesPlane(gliderHub, 0));
		assertFalse("Glider hub on plane 3 should not match plane 1", TravelData.matchesPlane(gliderHub, 1));

		final WorldPoint a = new WorldPoint(100, 200, 0);
		final WorldPoint b = new WorldPoint(100, 200, 0);
		final WorldPoint c = new WorldPoint(100, 200, 1);
		assertTrue("Same coordinates should match", TravelData.sameLocation(a, b));
		assertFalse("Different planes should not match", TravelData.sameLocation(a, c));
	}

	@Test
	public void testFindNodeNear()
	{
		final TravelNode firstNode = TravelData.ALL_NODES.get(0);
		final int x = firstNode.getLocation().getX();
		final int y = firstNode.getLocation().getY();
		final int plane = firstNode.getLocation().getPlane();

		// Exact coordinate match
		final TravelNode exact = TravelData.findNodeNear(x, y, plane, 0);
		assertNotNull("Exact match should find node", exact);
		assertEquals("Exact coordinate lookup should return the matching node", firstNode, exact);

		// Nearby coordinate within radius
		final TravelNode nearby = TravelData.findNodeNear(x + 2, y - 2, plane, 5);
		assertNotNull("Nearby lookup within radius should find node", nearby);
		assertEquals("Nearby lookup should return the matching node", firstNode, nearby);

		// Different plane should return null
		final int otherPlane = (plane + 1) % 4;
		final TravelNode differentPlane = TravelData.findNodeNear(x, y, otherPlane, 5);
		assertNull("Lookup on different plane should return null", differentPlane);

		// Point far from any node
		final TravelNode farAway = TravelData.findNodeNear(0, 0, 0, 5);
		assertNull("Distant coordinate should return null", farAway);
	}

	@Test
	public void testNodeCountsAndSpotChecks()
	{
		assertEquals("Total node count in ALL_NODES", 119, TravelData.ALL_NODES.size());

		long charterCount = TravelData.ALL_NODES.stream().filter(n -> n.getType() == TravelType.CHARTER_SHIP).count();
		long ferryCount = TravelData.ALL_NODES.stream().filter(n -> n.getType() == TravelType.FERRY).count();
		long gliderCount = TravelData.ALL_NODES.stream().filter(n -> n.getType() == TravelType.GNOME_GLIDER).count();
		long treeCount = TravelData.ALL_NODES.stream().filter(n -> n.getType() == TravelType.SPIRIT_TREE).count();
		long ringCount = TravelData.ALL_NODES.stream().filter(n -> n.getType() == TravelType.FAIRY_RING).count();
		long balloonCount = TravelData.ALL_NODES.stream().filter(n -> n.getType() == TravelType.HOT_AIR_BALLOON).count();
		long canoeCount = TravelData.ALL_NODES.stream().filter(n -> n.getType() == TravelType.CANOE).count();
		long cartCount = TravelData.ALL_NODES.stream().filter(n -> n.getType() == TravelType.MINE_CART).count();
		long carpetCount = TravelData.ALL_NODES.stream().filter(n -> n.getType() == TravelType.MAGIC_CARPET).count();
		long quetzalCount = TravelData.ALL_NODES.stream().filter(n -> n.getType() == TravelType.QUETZAL).count();
		long swampCount = TravelData.ALL_NODES.stream().filter(n -> n.getType() == TravelType.SWAMP_BOAT).count();

		assertEquals("Charter Ship node count", 13, charterCount);
		assertEquals("Ferry node count", 37, ferryCount);
		assertEquals("Gnome Glider node count", 6, gliderCount);
		assertEquals("Spirit Tree node count", 11, treeCount);
		assertEquals("Fairy Ring node count", 15, ringCount);
		assertEquals("Hot Air Balloon node count", 6, balloonCount);
		assertEquals("Canoe node count", 5, canoeCount);
		assertEquals("Mine Cart node count", 7, cartCount);
		assertEquals("Magic Carpet node count", 7, carpetCount);
		assertEquals("Quetzal node count", 6, quetzalCount);
		assertEquals("Swamp Boat node count", 6, swampCount);

		// Charter ship destination symmetry check: each of the 13 charter nodes must have 12 destinations
		for (TravelNode node : TravelData.ALL_NODES)
		{
			if (node.getType() == TravelType.CHARTER_SHIP)
			{
				assertEquals("Charter node " + node.getName() + " should have 12 destinations", 12, node.getDestinations().size());
			}
		}

		// Spot check newly added Charter nodes
		assertNotNull("Port Tyras Charter should exist", TravelData.findNodeNear(2139, 3120, 0, 2));
		assertNotNull("Sunset Coast Charter should exist", TravelData.findNodeNear(1510, 2970, 0, 2));

		// Spot check Spirit Tree registrations
		assertNotNull("Spirit Tree Poison Waste should exist", TravelData.findNodeNear(2340, 3160, 0, 2));
		assertNotNull("Spirit Tree Prifddinas should exist", TravelData.findNodeNear(3274, 6064, 0, 2));
		assertNotNull("Spirit Tree Etceteria should exist", TravelData.findNodeNear(2608, 3857, 0, 2));

		// Spot check Quetzal Twilight Temple registration
		assertNotNull("Quetzal Twilight Temple should exist", TravelData.findNodeNear(1460, 3280, 0, 2));

		// Spot check Magic Carpet registrations
		assertNotNull("Magic Carpet Bedabin Camp should exist", TravelData.findNodeNear(3180, 3042, 0, 2));
		assertNotNull("Magic Carpet Sophanem should exist", TravelData.findNodeNear(3286, 2813, 0, 2));
		assertNotNull("Magic Carpet Ruins of Unkah should exist", TravelData.findNodeNear(3146, 2840, 0, 2));

		// Spot check Fairy Rings registrations
		assertNotNull("Fairy Ring BKR Mort Myre should exist", TravelData.findNodeNear(3469, 3431, 0, 2));
		assertNotNull("Fairy Ring CJR Sinclair Mansion should exist", TravelData.findNodeNear(2705, 3576, 0, 2));
		assertNotNull("Fairy Ring CIQ Yanille should exist", TravelData.findNodeNear(2528, 3127, 0, 2));
		assertNotNull("Fairy Ring AKQ Piscatoris Colony should exist", TravelData.findNodeNear(2319, 3619, 0, 2));
		assertNotNull("Fairy Ring DJR Chasm of Fire should exist", TravelData.findNodeNear(1455, 3658, 0, 2));
		assertNotNull("Fairy Ring CIR south of Mount Karuulm should exist", TravelData.findNodeNear(1302, 3762, 0, 2));
		assertNotNull("Fairy Ring CIP Miscellania should exist", TravelData.findNodeNear(2513, 3884, 0, 2));

		// Spot check Reverse Ferries
		assertNotNull("Miscellania reverse ferry should exist", TravelData.findNodeNear(2581, 3846, 0, 2));
		assertNotNull("Waterbirth reverse ferry should exist", TravelData.findNodeNear(2545, 3755, 0, 2));
		assertNotNull("Neitiznot reverse ferry should exist", TravelData.findNodeNear(2311, 3781, 0, 2));
		assertNotNull("Jatizso reverse ferry should exist", TravelData.findNodeNear(2420, 3781, 0, 2));
		assertNotNull("Pirate's Cove reverse ferry should exist", TravelData.findNodeNear(2213, 3794, 0, 2));
		assertNotNull("Lunar Isle reverse ferry should exist", TravelData.findNodeNear(2113, 3894, 0, 2));
		assertNotNull("Weiss reverse ferry should exist", TravelData.findNodeNear(2848, 3962, 0, 2));
		assertNotNull("Land's End reverse ferry should exist", TravelData.findNodeNear(1504, 3399, 0, 2));
		assertNotNull("Port Piscarilius reverse ferry should exist", TravelData.findNodeNear(1824, 3691, 0, 2));

		// Grand Tree glider hub is on plane 3 but hoverable from surface map (plane 0)
		assertNotNull("Grand Tree glider hub should be findable on plane 0", TravelData.findNodeNear(2465, 3501, 0, 2));

		// Asymmetric network fixes
		assertNotNull("Wilderness canoe station should exist", TravelData.findNodeNear(3143, 3797, 0, 2));
		assertNotNull("Mort Myre Swamp Pier reverse should exist", TravelData.findNodeNear(3439, 3290, 0, 2));
		assertNotNull("Digsite Canal Barge should exist", TravelData.findNodeNear(3375, 3445, 0, 2));
		assertNotNull("Volcanic Mine Island barge should exist", TravelData.findNodeNear(3814, 3804, 0, 2));
		assertNotNull("Derelict Sea Ship barge should exist", TravelData.findNodeNear(3782, 3870, 0, 2));

		// New ferry networks
		assertNotNull("Slepe ferry (Ectofuntus) should exist", TravelData.findNodeNear(3687, 3471, 0, 2));
		assertNotNull("Slepe ferry (Slepe) should exist", TravelData.findNodeNear(3724, 3302, 0, 2));
		assertNotNull("Corsair rowboat (Port Sarim) should exist", TravelData.findNodeNear(3055, 3242, 0, 2));
		assertNotNull("Corsair rowboat (Corsair Cove) should exist", TravelData.findNodeNear(2589, 2851, 0, 2));
		assertNotNull("Ungael boat (Rellekka) should exist", TravelData.findNodeNear(2624, 3682, 0, 2));
		assertNotNull("Ungael boat (Ungael) should exist", TravelData.findNodeNear(2272, 4040, 0, 2));
		assertNotNull("Iceberg submarine (Rellekka) should exist", TravelData.findNodeNear(2707, 3735, 0, 2));
		assertNotNull("Iceberg submarine (Iceberg) should exist", TravelData.findNodeNear(2658, 3989, 0, 2));
		assertNotNull("Braindeath rowboat (Port Phasmatys) should exist", TravelData.findNodeNear(3680, 3475, 0, 2));
		assertNotNull("Braindeath rowboat (Braindeath Island) should exist", TravelData.findNodeNear(2140, 5093, 0, 2));
		assertNotNull("Harmony Island boat (Mos Le'Harmless) should exist", TravelData.findNodeNear(3680, 2980, 0, 2));
		assertNotNull("Harmony Island boat (Harmony Island) should exist", TravelData.findNodeNear(3798, 2860, 0, 2));
		assertNotNull("Isle of Souls ferry should exist", TravelData.findNodeNear(2282, 2823, 0, 2));
	}
}
