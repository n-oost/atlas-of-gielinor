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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.bettermap.map.MapCamera;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class OverlayClusterTest
{
	@org.junit.BeforeClass
	public static void loadMapData()
	{
		com.bettermap.map.MapData.load();
		org.junit.Assert.assertTrue("Map catalogs must load before testing", com.bettermap.map.MapData.isReady());
	}

	private static final String FIXTURE =
		"# comment\n"
			+ "lumbridge\tLumbridge\t3060\t3050\t3280\t3320\t"
			+ "lumbridge_swamp_caves,lumbridge_cellar,ham_hideout,draynor_sewers,dorgesh_kaan,dorgesh_kaan_south\n";

	@Test
	public void parseLoadsLumbridgeMembers() throws Exception
	{
		final List<OverlayCluster> clusters = OverlayCluster.parse(FIXTURE);
		assertEquals(1, clusters.size());
		final OverlayCluster lumbridge = clusters.get(0);
		assertEquals("lumbridge", lumbridge.id);
		assertEquals("Lumbridge", lumbridge.name);
		assertTrue(lumbridge.contains(3209, 3218));
		assertTrue(lumbridge.contains(3118, 3244));
		assertTrue(lumbridge.contains(3193, 3112));
		assertFalse(lumbridge.contains(2500, 3000));
		assertEquals(6, lumbridge.members.size());
		assertSame(UndergroundZone.LUMBRIDGE_SWAMP_CAVES, lumbridge.members.get(0));
		assertSame(UndergroundZone.DRAYNOR_SEWERS, lumbridge.members.get(3));
		assertSame(UndergroundZone.DORGESH_KAAN, lumbridge.members.get(4));
		assertSame(UndergroundZone.DORGESH_KAAN_SOUTH, lumbridge.members.get(5));
	}

	@Test
	public void resourceShipsRegionalUndergroundComposites()
	{
		boolean foundLumbridge = false;
		boolean foundZanaris = false;
		boolean foundVarrockEdgeville = false;
		boolean foundAsgarnia = false;
		boolean foundKourend = false;
		boolean foundMorytania = false;
		for (OverlayCluster cluster : OverlayCluster.all())
		{
			if (!"zanaris".equals(cluster.id) && !cluster.id.startsWith("native_"))
			{
				assertTrue(cluster.id + " should composite multiple nearby dungeons",
					cluster.members.size() >= 2);
			}
			if ("lumbridge".equals(cluster.id))
			{
				foundLumbridge = true;
				assertEquals(7, cluster.members.size());
				assertTrue(cluster.members.contains(UndergroundZone.DORGESH_KAAN));
				assertTrue(cluster.members.contains(UndergroundZone.DORGESH_KAAN_SOUTH));
				assertTrue(cluster.members.contains(UndergroundZone.CHASM_OF_TEARS));
				assertFalse(cluster.members.contains(UndergroundZone.ZANARIS));
				assertFalse(cluster.members.contains(UndergroundZone.VARROCK_SEWERS));
				assertEquals(3224, cluster.iconX);
				assertEquals(3236, cluster.iconY);
				assertTrue(cluster.contains(3209, 3218));
				assertTrue(cluster.contains(3118, 3244));
				assertFalse(cluster.contains(3237, 3458));
			}
			if ("zanaris".equals(cluster.id))
			{
				foundZanaris = true;
				assertEquals(1, cluster.members.size());
				assertSame(UndergroundZone.ZANARIS, cluster.members.get(0));
				assertTrue(cluster.contains(3201, 3169));
				assertTrue(cluster.tileArea() < 2000);
			}
			if ("varrock_edgeville".equals(cluster.id))
			{
				foundVarrockEdgeville = true;
				assertEquals(3, cluster.members.size());
				assertTrue(cluster.members.contains(UndergroundZone.VARROCK_SEWERS));
				assertTrue(cluster.members.contains(UndergroundZone.EDGEVILLE_DUNGEON));
				assertTrue(cluster.members.contains(UndergroundZone.ZEMOUREGALS_BASE));
				assertFalse(cluster.members.contains(UndergroundZone.STRONGHOLD_OF_SECURITY));
				assertEquals(3166, cluster.iconX);
				assertEquals(3464, cluster.iconY);
				assertTrue(cluster.contains(3237, 3458));
				assertTrue(cluster.contains(3096, 3469));
				assertTrue(cluster.contains(3088, 3571));
				assertFalse(cluster.contains(3081, 3420));
				assertFalse(cluster.contains(3018, 3450));
				assertFalse(cluster.contains(3209, 3218));
			}
			if ("asgarnia".equals(cluster.id))
			{
				foundAsgarnia = true;
				assertTrue(cluster.members.contains(UndergroundZone.STRONGHOLD_OF_SECURITY));
				assertTrue(cluster.members.contains(UndergroundZone.TAVERLEY_DUNGEON));
			}
			if ("kourend".equals(cluster.id))
			{
				foundKourend = true;
				assertTrue(cluster.members.contains(UndergroundZone.CATACOMBS_OF_KOUREND));
			}
			if ("morytania".equals(cluster.id))
			{
				foundMorytania = true;
				assertTrue(cluster.members.contains(UndergroundZone.BARROWS_CRYPTS));
			}
		}
		assertTrue("overlays.tsv must ship a Lumbridge cluster", foundLumbridge);
		assertTrue("overlays.tsv must ship a Zanaris cluster", foundZanaris);
		assertTrue("overlays.tsv must ship a Varrock & Edgeville cluster", foundVarrockEdgeville);
		assertTrue("overlays.tsv must ship an Asgarnia regional composite", foundAsgarnia);
		assertTrue("overlays.tsv must ship a Kourend regional composite", foundKourend);
		assertTrue("overlays.tsv must ship a Morytania regional composite", foundMorytania);
	}

	@Test
	public void regionalCompositeBoundsStayLocal()
	{
		for (OverlayCluster cluster : OverlayCluster.all())
		{
			if (cluster.id.startsWith("native_")) continue;
			assertTrue("regional composite is too wide: " + cluster.id, cluster.maxX - cluster.minX <= 800);
			assertTrue("regional composite is too tall: " + cluster.id, cluster.maxY - cluster.minY <= 800);
		}
	}

	@Test
	public void parseReadsGreenIconTile() throws Exception
	{
		final OverlayCluster cluster = OverlayCluster.parse(
			"lumbridge\tLumbridge\t3060\t3050\t3280\t3320\tlumbridge_cellar,ham_hideout\t3224\t3236\n")
			.get(0);
		assertEquals(3224, cluster.iconX);
		assertEquals(3236, cluster.iconY);
	}

	@Test
	public void parseWithoutIconColumnsUsesMemberCentroid() throws Exception
	{
		final OverlayCluster cluster = OverlayCluster.parse(FIXTURE).get(0);
		int sx = 0;
		int sy = 0;
		for (UndergroundZone zone : cluster.members)
		{
			sx += zone.getSurfacePoint().getX();
			sy += zone.getSurfacePoint().getY();
		}
		assertEquals(sx / cluster.members.size(), cluster.iconX);
		assertEquals(sy / cluster.members.size(), cluster.iconY);
	}

	@Test
	public void clickingClusterPreservesPositionAndOpensMembers()
	{
		final OverlayCluster cluster = new OverlayCluster(
			"lumbridge", "Lumbridge", 3060, 3050, 3280, 3320,
			java.util.Collections.singletonList(UndergroundZone.LUMBRIDGE_CELLAR),
			3224, 3236);
		final MapCamera camera = new MapCamera();
		camera.setActiveOverlayCluster(cluster);
		assertEquals(3222.0, camera.getCenterX(), 0.01);
		assertEquals(3218.0, camera.getCenterY(), 0.01);
		assertTrue(camera.isClusterPreview());
		assertSame(cluster, camera.getActiveOverlayCluster());
	}

	@Test
	public void dungeonChipWinsHitTestOverRegion()
	{
		final OverlayCluster cluster = new OverlayCluster(
			"lumbridge", "Lumbridge", 3060, 3050, 3280, 3320,
			java.util.Collections.singletonList(UndergroundZone.LUMBRIDGE_CELLAR));
		final List<MapCamera.LayerSymbolTarget> targets = new ArrayList<>();
		targets.add(new MapCamera.LayerSymbolTarget(
			new Rectangle(10, 10, 16, 16), UndergroundZone.LUMBRIDGE_CELLAR, true));
		targets.add(new MapCamera.LayerSymbolTarget(new Rectangle(0, 0, 200, 200), cluster));

		MapCamera.LayerSymbolTarget hit = null;
		final java.awt.Point point = new java.awt.Point(12, 12);
		for (MapCamera.LayerSymbolTarget target : targets)
		{
			if (target.getBounds().contains(point))
			{
				hit = target;
				break;
			}
		}
		assertFalse(hit.isRegion());
		assertSame(UndergroundZone.LUMBRIDGE_CELLAR, hit.getZone());
	}

	@Test
	public void previewZonesUseClusterUntilAChipIsHovered() throws Exception
	{
		final MapCamera camera = new MapCamera();
		final OverlayCluster cluster = OverlayCluster.parse(FIXTURE).get(0);
		camera.setHoveredOverlayCluster(cluster);
		assertTrue(camera.isClusterPreview());
		assertEquals(6, camera.previewUndergroundZones().size());

		camera.setHoveredUnderground(UndergroundZone.HAM_HIDEOUT, true);
		assertFalse(camera.isClusterPreview());
		assertEquals(1, camera.previewUndergroundZones().size());
		assertSame(UndergroundZone.HAM_HIDEOUT, camera.previewUndergroundZones().get(0));
	}

	@Test
	public void smallerRegionWinsHitTestOverLarger()
	{
		final OverlayCluster lumbridge = new OverlayCluster(
			"lumbridge", "Lumbridge", 3060, 3050, 3280, 3320,
			java.util.Collections.singletonList(UndergroundZone.LUMBRIDGE_CELLAR));
		final OverlayCluster zanaris = new OverlayCluster(
			"zanaris", "Zanaris", 3188, 3155, 3220, 3185,
			java.util.Collections.singletonList(UndergroundZone.ZANARIS));
		assertTrue(zanaris.tileArea() < lumbridge.tileArea());
		final List<MapCamera.LayerSymbolTarget> targets = new ArrayList<>();
		targets.add(new MapCamera.LayerSymbolTarget(new Rectangle(40, 40, 20, 20), zanaris));
		targets.add(new MapCamera.LayerSymbolTarget(new Rectangle(0, 0, 200, 200), lumbridge));
		MapCamera.LayerSymbolTarget hit = null;
		final java.awt.Point point = new java.awt.Point(50, 50);
		for (MapCamera.LayerSymbolTarget target : targets)
		{
			if (target.getBounds().contains(point))
			{
				hit = target;
				break;
			}
		}
		assertEquals("zanaris", hit.getCluster().id);
	}
}
