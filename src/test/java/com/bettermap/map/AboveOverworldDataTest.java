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

import com.bettermap.data.UndergroundZone;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import net.runelite.api.coords.WorldPoint;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * The bundled data sets carry thousands of points in the band above the overworld — dungeon
 * interiors at +6400, instanced minigames, quest copies. Every one of them must either project
 * down onto the overworld or be hidden while the camera is on it. Anything that does neither
 * hangs in the empty north of the map with no tiles underneath, which is the bug this guards.
 */
public class AboveOverworldDataTest
{
	@org.junit.BeforeClass
	public static void loadMapData()
	{
		com.bettermap.map.MapData.load();
		org.junit.Assert.assertTrue("Map catalogs must load before testing", com.bettermap.map.MapData.isReady());
	}

	/** Lumbridge: the overworld, as far from the band as a camera gets. */
	private static final double CAM_X = 3222;
	private static final double CAM_Y = 3218;

	private static final Pattern JSON_XY = Pattern.compile("\"x\":([0-9]+),\"y\":([0-9]+)");

	@Test
	public void noBundledPointDrawsAboveTheOverworld() throws IOException
	{
		final List<int[]> points = new ArrayList<>();
		readTsv(points, "/com/bettermap/poi/pois.tsv");
		readTsv(points, "/com/bettermap/poi/pois-cache.tsv");
		readJson(points, "/com/bettermap/data/monsters.json.gz");
		readJson(points, "/com/bettermap/data/shops.json.gz");
		readJson(points, "/com/bettermap/data/ground_items.json.gz");

		final List<String> floating = new ArrayList<>();
		for (int[] p : points)
		{
			if (InstanceMaps.hiddenOnSurface(p[0], p[1], CAM_X, CAM_Y))
			{
				continue;
			}
			final double displayY = InstanceMaps.toDisplayY(p[0], p[1], CAM_X, CAM_Y);
			if (displayY > InstanceMaps.GAP_MIN_Y && floating.size() < 20)
			{
				floating.add(p[0] + "," + p[1] + " -> y " + (int) displayY);
			}
		}

		assertEquals("points left floating above the overworld: " + floating, 0, floating.size());
	}

	@Test
	public void dungeonContentStaysVisibleOnItsOwnLayer()
	{
		// Unzoned dungeon point in the gap: no entrance pin, so it is hidden from the overworld …
		assertTrue(InstanceMaps.hiddenOnSurface(500, 10000, CAM_X, CAM_Y));
		// … but drawn once the camera is down there with it.
		assertFalse(InstanceMaps.hiddenOnSurface(500, 10000, 500, 10000));

		// … while Zilyana keeps her God Wars entrance projection onto Trollheim.
		assertFalse(InstanceMaps.hiddenOnSurface(2907, 5265, CAM_X, CAM_Y));
	}

	/**
	 * Hover reaches dungeon records by walking the zone offsets backwards from the cursor
	 * ({@link InstanceMaps#firstHit}), so every zone anchor hover can find must also be a point
	 * the map draws. Otherwise a tooltip appears for a marker that is not on screen.
	 */
	@Test
	public void everyZoneAnchorHoverCanReachIsAlsoDrawn()
	{
		for (UndergroundZone zone : UndergroundZone.ALL_ZONES)
		{
			if (zone.getId().startsWith("native_") || zone.getId().contains("__") || zone.getYOffset() != 6400) continue;
			final WorldPoint anchor = zone.getUndergroundPoint();
			assertFalse(zone.getName() + " is hoverable but not drawn",
				InstanceMaps.hiddenOnSurface(anchor.getX(), anchor.getY(), CAM_X, CAM_Y));
		}
	}

	private static void readTsv(List<int[]> out, String resource) throws IOException
	{
		try (InputStream in = AboveOverworldDataTest.class.getResourceAsStream(resource);
			BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)))
		{
			String line;
			while ((line = reader.readLine()) != null)
			{
				if (line.isEmpty() || line.charAt(0) == '#')
				{
					continue;
				}
				final String[] parts = line.split("\t");
				if (parts.length >= 2)
				{
					out.add(new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])});
				}
			}
		}
	}

	private static void readJson(List<int[]> out, String resource) throws IOException
	{
		final StringBuilder json = new StringBuilder();
		try (InputStream in = new GZIPInputStream(AboveOverworldDataTest.class.getResourceAsStream(resource));
			BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)))
		{
			final char[] buffer = new char[1 << 16];
			int read;
			while ((read = reader.read(buffer)) > 0)
			{
				json.append(buffer, 0, read);
			}
		}

		final Matcher matcher = JSON_XY.matcher(json);
		while (matcher.find())
		{
			out.add(new int[]{Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2))});
		}
	}
}
