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

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

import bettermap.data.OverlayFloor;
import bettermap.data.UndergroundZone;
import bettermap.map.MapCamera;

public class OverlayFloorTest
{
	@org.junit.BeforeClass
	public static void loadMapData()
	{
		bettermap.map.MapData.load();
		org.junit.Assert.assertTrue("Map catalogs must load before testing", bettermap.map.MapData.isReady());
	}

	@Test
	public void samePlaneFloorsKeepSeparateLayersWhenHoveredAndOpened() throws Exception
	{
		List<OverlayFloor> floors = OverlayFloor.parse(
			"yanille_agility_dungeon\tUpper\t0\t2575\t3095\t1201\n"
				+ "yanille_agility_dungeon\tLower\t0\t2588\t3169\t1372\n");
		MapCamera camera = new MapCamera();
		UndergroundZone zone = UndergroundZone.YANILLE_AGILITY_DUNGEON;
		camera.setHoveredFloor(floors.get(0));
		assertEquals(Integer.valueOf(1201), camera.floorLayerFor(zone));
		camera.setHoveredFloor(floors.get(1));
		assertEquals(Integer.valueOf(1372), camera.floorLayerFor(zone));
		camera.setActiveFloor(floors.get(1));
		camera.setHoveredFloor(null);
		assertEquals(Integer.valueOf(1372), camera.floorLayerFor(zone));
		camera.setActiveFloor(floors.get(0));
		assertEquals(Integer.valueOf(1201), camera.floorLayerFor(zone));
		camera.setPlane(0);
		assertEquals(null, camera.floorLayerFor(zone));
		camera.setActiveFloor(floors.get(1));
		camera.clearUndergroundMode();
		assertEquals(null, camera.floorLayerFor(zone));
	}

	@Test
	public void bundledYanilleFloorsUseSeparateFootprintsAndLayers()
	{
		List<OverlayFloor> floors = new ArrayList<>();
		for (OverlayFloor floor : OverlayFloor.all())
		{
			if (floor.zone == UndergroundZone.YANILLE_AGILITY_DUNGEON)
			{
				floors.add(floor);
			}
		}
		assertEquals(2, floors.size());
		assertEquals(0, floors.get(0).plane);
		assertEquals(0, floors.get(1).plane);
		assertFalse(floors.get(0).layerId.equals(floors.get(1).layerId));
		assertTrue(Math.abs(floors.get(0).worldY - floors.get(1).worldY) > 50);
	}

	@Test
	public void strongholdFloorsHaveDistinctLayersInOneLocalFootprint()
	{
		final List<OverlayFloor> floors = new ArrayList<>();
		for (OverlayFloor floor : OverlayFloor.all())
		{
			if (floor.zone == UndergroundZone.STRONGHOLD_OF_SECURITY)
			{
				floors.add(floor);
			}
		}

		assertEquals(4, floors.size());
		assertEquals(Integer.valueOf(1271), OverlayFloor.defaultLayerFor(UndergroundZone.STRONGHOLD_OF_SECURITY));
		final java.util.Set<Integer> layers = new java.util.HashSet<>();
		for (OverlayFloor floor : floors)
		{
			assertTrue(floor.worldX >= 3078 && floor.worldX <= 3145);
			assertTrue(floor.worldY >= 3362 && floor.worldY <= 3424);
			assertTrue(layers.add(floor.layerId));
		}
	}

	@Test
	public void spiderCaveAndAraxxorUseSeparateSelectableLayers()
	{
		final List<OverlayFloor> floors = new ArrayList<>();
		for (OverlayFloor floor : OverlayFloor.all())
		{
			if (floor.zone == UndergroundZone.MORYTANIA_SPIDER_CAVE)
			{
				floors.add(floor);
			}
		}

		assertEquals(2, floors.size());
		assertEquals(Integer.valueOf(1256), floors.get(0).layerId);
		assertEquals(Integer.valueOf(1222), floors.get(1).layerId);
	}

	@Test
	public void samePlaneFloorHoverIdentifiesOnlyItsAuthoredLayer() throws Exception
	{
		final List<OverlayFloor> floors = OverlayFloor.parse(
			"stronghold_of_security\tVault of War\t0\t3145\t3415\t1271\n"
				+ "stronghold_of_security\tCatacomb of Famine\t0\t3145\t3398\t1358\n");
		final MapCamera camera = new MapCamera();
		camera.setHoveredFloor(floors.get(1));
		assertFalse(camera.isHoveredFloor(floors.get(0)));
		assertTrue(camera.isHoveredFloor(floors.get(1)));
	}

	private static final String FIXTURE =
		"# comment\n"
			+ "dorgesh_kaan\tDorgesh-Kaan (Lower level)\t0\t3198\t3139\t1052\n"
			+ "dorgesh_kaan\tDorgesh-Kaan (Middle level)\t1\t3198\t3170\t1053\n"
			+ "dorgesh_kaan\tDorgesh-Kaan (Upper level)\t2\t3198\t3203\t1373\n";

	@Test
	public void parseLoadsDorgeshFloors() throws Exception
	{
		final List<OverlayFloor> floors = OverlayFloor.parse(FIXTURE);
		assertEquals(3, floors.size());
		assertSame(UndergroundZone.DORGESH_KAAN, floors.get(0).zone);
		assertEquals(0, floors.get(0).plane);
		assertEquals(3198, floors.get(0).worldX);
		assertEquals(3139, floors.get(0).worldY);
		assertEquals(Integer.valueOf(1052), floors.get(0).layerId);
		assertEquals(1, floors.get(1).plane);
		assertEquals(3170, floors.get(1).worldY);
		assertEquals(Integer.valueOf(1053), floors.get(1).layerId);
		assertEquals(2, floors.get(2).plane);
		assertEquals(3203, floors.get(2).worldY);
		assertEquals(Integer.valueOf(1373), floors.get(2).layerId);
	}

	@Test
	public void resourceIncludesDorgeshFloors()
	{
		int ground = 0;
		int middle = 0;
		int upper = 0;
		int southGround = 0;
		int southUpper = 0;
		int zanarisGround = 0;
		int zanarisUpper = 0;
		int stronghold = 0;
		int chasmTears = 0;
		int ancientLower = 0;
		int ancientUpper = 0;
		int upFirst = 0;
		int upSecond = 0;
		int upThird = 0;
		int upFourth = 0;
		int upFifth = 0;
		int upSixth = 0;
		for (OverlayFloor floor : OverlayFloor.all())
		{
			if (floor.zone == UndergroundZone.DORGESH_KAAN && floor.plane == 0)
			{
				ground++;
			}
			if (floor.zone == UndergroundZone.DORGESH_KAAN && floor.plane == 1)
			{
				middle++;
			}
			if (floor.zone == UndergroundZone.DORGESH_KAAN && floor.plane == 2)
			{
				upper++;
			}
			if (floor.zone == UndergroundZone.DORGESH_KAAN_SOUTH && floor.plane == 0)
			{
				southGround++;
			}
			if (floor.zone == UndergroundZone.DORGESH_KAAN_SOUTH && floor.plane == 3)
			{
				southUpper++;
			}
			if (floor.zone == UndergroundZone.ZANARIS && floor.plane == 0)
			{
				zanarisGround++;
			}
			if (floor.zone == UndergroundZone.ZANARIS && floor.plane == 1)
			{
				zanarisUpper++;
			}
			if (floor.zone == UndergroundZone.STRONGHOLD_OF_SECURITY)
			{
				stronghold++;
			}
			if (floor.zone == UndergroundZone.CHASM_OF_TEARS)
			{
				chasmTears++;
				assertEquals(2, floor.plane);
				assertEquals(3215, floor.worldX);
				assertEquals(3141, floor.worldY);
			}
			if (floor.zone == UndergroundZone.ANCIENT_CAVERN && floor.plane == 0)
			{
				ancientLower++;
				assertEquals(2504, floor.worldX);
				assertEquals(3490, floor.worldY);
			}
			if (floor.zone == UndergroundZone.ANCIENT_CAVERN && floor.plane == 1)
			{
				ancientUpper++;
				assertEquals(2512, floor.worldX);
				assertEquals(3486, floor.worldY);
			}
			if (floor.zone == UndergroundZone.UNDERGROUND_PASS && "Underground Pass (1)".equals(floor.name))
			{
				upFirst++;
				assertEquals(2406, floor.worldX);
				assertEquals(3265, floor.worldY);
			}
			if (floor.zone == UndergroundZone.UNDERGROUND_PASS && "Underground Pass (2)".equals(floor.name))
			{
				upSecond++;
				assertEquals(2406, floor.worldX);
				assertEquals(3255, floor.worldY);
			}
			if (floor.zone == UndergroundZone.UNDERGROUND_PASS && "Underground Pass (3)".equals(floor.name))
			{
				upThird++;
				assertEquals(2406, floor.worldX);
				assertEquals(3244, floor.worldY);
			}
			if (floor.zone == UndergroundZone.UNDERGROUND_PASS && "Underground Pass (4 - Soulless Pit)".equals(floor.name))
			{
				upFourth++;
				assertEquals(2406, floor.worldX);
				assertEquals(3234, floor.worldY);
			}
			if (floor.zone == UndergroundZone.UNDERGROUND_PASS && "Underground Pass (5 - Iban's Lair)".equals(floor.name))
			{
				upFifth++;
				assertEquals(2406, floor.worldX);
				assertEquals(3223, floor.worldY);
			}
			if (floor.zone == UndergroundZone.UNDERGROUND_PASS && "Underground Pass (6 - Well of Voyage)".equals(floor.name))
			{
				upSixth++;
				assertEquals(2406, floor.worldX);
				assertEquals(3211, floor.worldY);
			}
		}
		assertEquals(1, ground);
		assertEquals(1, middle);
		assertEquals(1, upper);
		assertEquals(1, southGround);
		assertEquals(1, southUpper);
		assertEquals(1, zanarisGround);
		assertEquals(1, zanarisUpper);
		assertEquals(4, stronghold);
		assertEquals(1, chasmTears);
		assertEquals(1, ancientLower);
		assertEquals(1, ancientUpper);
		assertEquals(1, upFirst);
		assertEquals(1, upSecond);
		assertEquals(1, upThird);
		assertEquals(1, upFourth);
		assertEquals(1, upFifth);
		assertEquals(1, upSixth);

		int wb = 0;
		int karuulm = 0;
		int mining = 0;
		int brim = 0;
		int tav = 0;
		int cam = 0;
		int ney = 0;
		int haunted = 0;
		int keld = 0;
		int pw = 0;
		int sish = 0;
		int jorm = 0;
		int ghor = 0;
		int tonali = 0;
		int dragon = 0;
		for (OverlayFloor floor : OverlayFloor.all())
		{
			if (floor.zone == UndergroundZone.WATERBIRTH_DUNGEON) wb++;
			if (floor.zone == UndergroundZone.KARUULM_SLAYER_DUNGEON) karuulm++;
			if (floor.zone == UndergroundZone.MINING_GUILD) mining++;
			if (floor.zone == UndergroundZone.BRIMHAVEN_DUNGEON) brim++;
			if (floor.zone == UndergroundZone.TAVERLEY_DUNGEON) tav++;
			if (floor.zone == UndergroundZone.CAM_TORUM) cam++;
			if (floor.zone == UndergroundZone.NEYPOTZLI) ney++;
			if (floor.zone == UndergroundZone.HAUNTED_MINE) haunted++;
			if (floor.zone == UndergroundZone.KELDAGRIM) keld++;
			if (floor.zone == UndergroundZone.POISON_WASTE_DUNGEON) pw++;
			if (floor.zone == UndergroundZone.SISTERHOOD_SANCTUARY) sish++;
			if (floor.zone == UndergroundZone.JORMUNGANDS_PRISON) jorm++;
			if (floor.zone == UndergroundZone.GHORROCK_DUNGEON) ghor++;
			if (floor.zone == UndergroundZone.TONALI_CAVERN) tonali++;
			if (floor.zone == UndergroundZone.DRAGON_NEST) dragon++;
		}
		assertEquals(2, wb);
		assertEquals(2, karuulm);
		assertEquals(2, mining);
		assertEquals(2, brim);
		assertEquals(4, tav);
		assertEquals(1, cam);
		assertEquals(1, ney);
		assertEquals(2, haunted);
		assertEquals(3, keld);
		assertEquals(2, pw);
		assertEquals(3, sish);
		assertEquals(1, jorm);
		assertEquals(4, ghor);
		assertEquals(1, tonali);
		assertEquals(1, dragon);
	}



	@Test
	public void dungeonChipWinsHitTestOverFloor()
	{
		final OverlayFloor floor = new OverlayFloor(
			"Dorgesh-Kaan (1st floor)", UndergroundZone.DORGESH_KAAN, 1, 3198, 3203);
		final List<MapCamera.LayerSymbolTarget> targets = new ArrayList<>();
		targets.add(new MapCamera.LayerSymbolTarget(
			new Rectangle(10, 10, 16, 16), UndergroundZone.DORGESH_KAAN, true));
		targets.add(new MapCamera.LayerSymbolTarget(new Rectangle(8, 8, 12, 12), floor));

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
		assertFalse(hit.isFloor());
		assertSame(UndergroundZone.DORGESH_KAAN, hit.getZone());
	}

	@Test
	public void hoveringFloorPeeksThatZoneAndPlane()
	{
		final MapCamera camera = new MapCamera();
		final OverlayFloor floor = new OverlayFloor(
			"Dorgesh-Kaan (1st floor)", UndergroundZone.DORGESH_KAAN, 1, 3198, 3203);
		camera.setHoveredFloor(floor);
		assertEquals(1, camera.previewUndergroundZones().size());
		assertSame(UndergroundZone.DORGESH_KAAN, camera.previewUndergroundZones().get(0));
		assertEquals(Integer.valueOf(1), camera.getHoveredFloorPlane());
	}
}
