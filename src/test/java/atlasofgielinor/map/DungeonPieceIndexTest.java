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
package atlasofgielinor.map;

import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Collections;
import org.junit.After;
import org.junit.Test;

import atlasofgielinor.data.dungeons.DungeonPiece;
import atlasofgielinor.data.dungeons.DungeonPieceTransform;
import atlasofgielinor.data.dungeons.UndergroundZone;
import atlasofgielinor.map.DungeonPieceIndex;
import atlasofgielinor.map.InstanceMaps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DungeonPieceIndexTest
{
	@org.junit.BeforeClass
	public static void loadMapData()
	{
		atlasofgielinor.map.MapCatalogLoader.load();
		org.junit.Assert.assertTrue("Map catalogs must load before testing", atlasofgielinor.map.MapCatalogLoader.isReady());
	}

	@After
	public void clearRoutingSnapshot()
	{
		InstanceMaps.setRoutingPieces(Collections.emptyList());
	}

	private static final String FIXTURE =
		"# comment ignored\n"
			+ "#L\t1\t1\tTaverley Dungeon\t-1\n"
			+ "# piece\tlayer\tplane\tlabel\tsrcMinX\tsrcMinY\tsrcMaxX\tsrcMaxY\tdstDx\tdstDy\trot\tlocked\tflip\tzone\n"
			+ "0\t1\t0\tTaverley\t2880\t9790\t2890\t9800\t0\t-6400\t0\t0\t0\ttaverley_dungeon\n"
			+ "0\t1\t0\tTaverley\t2891\t9790\t2900\t9800\t0\t-6400\t0\t0\t0\ttaverley_dungeon\n"
			+ "1\t1\t0\tFlipped\t100\t100\t110\t110\t0\t0\t90\t0\t3\t-\n"
			+ "2\t1\t0\tLegacy\t200\t200\t210\t210\t5\t-5\t0\t1\t0\n";

	@Test
	public void parseSkipsCommentsAndMergesMultiRectPieces() throws Exception
	{
		final DungeonPieceIndex.ParseResult parsed = DungeonPieceIndex.parse(new StringReader(FIXTURE));
		assertEquals(1, parsed.layers.size());
		assertEquals("Taverley Dungeon", parsed.layers.get(0).name);
		assertEquals(3, parsed.pieces.size());

		final DungeonPiece taverley = parsed.pieces.get(0);
		assertEquals(0, taverley.id);
		assertEquals(2, taverley.rects.length);
		assertEquals("taverley_dungeon", taverley.zoneId);
		assertTrue(taverley.containsNative(2885, 9795));
		assertTrue(taverley.containsNative(2895, 9795));

		final DungeonPiece flipped = parsed.pieces.get(1);
		assertEquals(90, flipped.rot);
		assertTrue(flipped.flipX);
		assertTrue(flipped.flipY);
		assertFalse(flipped.hasZone());

		final DungeonPiece legacy = parsed.pieces.get(2);
		assertTrue(legacy.locked);
		assertEquals(5, legacy.dx);
		assertEquals(-5, legacy.dy);
		assertFalse(legacy.hasZone());
	}

	@Test
	public void piecesForReturnsOnlyMatchingZone() throws Exception
	{
		final File dir = Files.createTempDirectory("dungeon-pieces").toFile();
		dir.deleteOnExit();
		final File tsv = DungeonPieceIndex.file(dir);
		tsv.getParentFile().mkdirs();
		Files.write(tsv.toPath(), FIXTURE.getBytes(StandardCharsets.UTF_8));

		final DungeonPieceIndex index = new DungeonPieceIndex();
		index.load(dir);

		final List<DungeonPiece> taverley = index.piecesFor("taverley_dungeon");
		assertEquals(1, taverley.size());
		assertEquals(0, taverley.get(0).id);
		assertEquals(2, taverley.get(0).rects.length);
		assertEquals(0, index.pieceAt("taverley_dungeon", 2895, 9795, 0, 1).id);
		assertTrue(index.pieceAt("taverley_dungeon", 2895, 9795, 1, 1) == null);
		assertTrue(index.piecesFor("lumbridge_cellar").isEmpty());
		assertTrue(index.piecesFor("").isEmpty());
	}

	@Test
	public void affineIsIdentityWhenUnoriented()
	{
		final AffineTransform t = DungeonPieceTransform.affine(0, false, false, 10, 20);
		assertTrue(t.isIdentity());
	}

	@Test
	public void bundledZoneTaggedPiecesUseConfiguredZones() throws Exception
	{
		try (InputStream in = DungeonPieceIndex.class.getResourceAsStream("/atlasofgielinor/dungeons/pieces.tsv"))
		{
			assertTrue(in != null);
			final DungeonPieceIndex.ParseResult parsed = DungeonPieceIndex.parse(
				new java.io.InputStreamReader(in, StandardCharsets.UTF_8));
			for (DungeonPiece piece : parsed.pieces)
			{
				if (piece.hasZone())
				{
					assertTrue("Unknown zone ID on piece " + piece.id + ": " + piece.zoneId,
						UndergroundZone.byId(piece.zoneId) != null);
				}
			}
		}
	}

	@Test
	public void correctedPiecesBelongToTheirInteractiveZones() throws Exception
	{
		try (InputStream in = DungeonPieceIndex.class.getResourceAsStream("/atlasofgielinor/dungeons/pieces.tsv"))
		{
			final DungeonPieceIndex.ParseResult parsed = DungeonPieceIndex.parse(
				new java.io.InputStreamReader(in, StandardCharsets.UTF_8));
			assertEquals(1, parsed.pieces.stream()
				.filter(piece -> "morytania_spider_cave".equals(piece.zoneId) && piece.layer == 1222)
				.count());
			assertEquals(1, parsed.pieces.stream()
				.filter(piece -> "shade_catacombs".equals(piece.zoneId))
				.count());
			assertEquals(1, parsed.pieces.stream()
				.filter(piece -> "vetions_rest".equals(piece.zoneId))
				.count());
			assertEquals(1, parsed.pieces.stream()
				.filter(piece -> "troll_stronghold".equals(piece.zoneId))
				.count());
		}
	}

	@Test
	public void asgarnianIceDungeonIncludesItsWestConnection() throws Exception
	{
		try (InputStream in = DungeonPieceIndex.class.getResourceAsStream("/atlasofgielinor/dungeons/pieces.tsv"))
		{
			final DungeonPieceIndex.ParseResult parsed = DungeonPieceIndex.parse(
				new java.io.InputStreamReader(in, StandardCharsets.UTF_8));
			for (int id : new int[] {19, 20, 22})
			{
				final int pieceId = id;
				final DungeonPiece piece = parsed.pieces.stream()
					.filter(candidate -> candidate.id == pieceId)
					.findFirst().orElse(null);
				assertTrue("Missing Ice Dungeon connection piece " + id, piece != null);
				assertEquals(1007, piece.layer);
				assertEquals("asgarnia_ice_cave", piece.zoneId);
			}
		}
	}

	@Test
	public void isleOfSoulsPieceUsesTheCanonicalZoneProjection() throws Exception
	{
		try (InputStream in = DungeonPieceIndex.class.getResourceAsStream("/atlasofgielinor/dungeons/pieces.tsv"))
		{
			final DungeonPieceIndex.ParseResult parsed = DungeonPieceIndex.parse(
				new java.io.InputStreamReader(in, StandardCharsets.UTF_8));
			final DungeonPiece piece = parsed.pieces.stream()
				.filter(candidate -> "isle_of_souls_dungeon".equals(candidate.zoneId))
				.findFirst().orElse(null);
			assertTrue(piece != null);

			final UndergroundZone zone = UndergroundZone.ISLE_OF_SOULS_DUNGEON;
			assertTrue(piece.containsNative(zone.getUndergroundPoint().getX(), zone.getUndergroundPoint().getY()));
			assertEquals(zone.getSurfacePoint().getX(), zone.getUndergroundPoint().getX() + piece.dx);
			assertEquals(zone.getSurfacePoint().getY(), zone.getUndergroundPoint().getY() + piece.dy);
		}
	}

	@Test
	public void strongholdFloorsStackAtOneDisplayFootprint() throws Exception
	{
		try (InputStream in = DungeonPieceIndex.class.getResourceAsStream("/atlasofgielinor/dungeons/pieces.tsv"))
		{
			final DungeonPieceIndex.ParseResult parsed = DungeonPieceIndex.parse(
				new java.io.InputStreamReader(in, StandardCharsets.UTF_8));
			final java.util.Set<Integer> layers = new java.util.HashSet<>();
			int count = 0;
			for (DungeonPiece piece : parsed.pieces)
			{
				if (!"stronghold_of_security".equals(piece.zoneId))
				{
					continue;
				}
				count++;
				assertTrue(layers.add(piece.layer));
				final int[] bounds = piece.worldBounds();
				assertTrue(bounds[0] >= 3078 && bounds[2] <= 3140);
				assertTrue(bounds[1] >= 3362 && bounds[3] <= 3424);
			}
			assertEquals(4, count);
		}
	}

	@Test
	public void dorgeshKaanFloorsHaveDistinctLayersAndPlanes() throws Exception
	{
		try (InputStream in = DungeonPieceIndex.class.getResourceAsStream("/atlasofgielinor/dungeons/pieces.tsv"))
		{
			final DungeonPieceIndex.ParseResult parsed = DungeonPieceIndex.parse(
				new java.io.InputStreamReader(in, StandardCharsets.UTF_8));
			final java.util.List<DungeonPiece> pieces = parsed.pieces.stream()
				.filter(piece -> "dorgesh_kaan".equals(piece.zoneId))
				.collect(java.util.stream.Collectors.toList());
			assertEquals(3, pieces.size());
			final java.util.Set<Integer> planes = new java.util.HashSet<>();
			final java.util.Set<Integer> layers = new java.util.HashSet<>();
			for (DungeonPiece piece : pieces)
			{
				assertTrue(planes.add(piece.plane));
				assertTrue(layers.add(piece.layer));
			}
			assertTrue(planes.contains(0));
			assertTrue(planes.contains(1));
			assertTrue(planes.contains(2));
			assertTrue(layers.contains(1052));
			assertTrue(layers.contains(1053));
			assertTrue(layers.contains(1373));
		}
	}

	@Test
	public void sharedZoneReturnsPiecesForAliasedZone()
	{
		final DungeonPieceIndex index = new DungeonPieceIndex();
		index.load(null);
		final java.util.List<DungeonPiece> edgeville = index.piecesFor("edgeville_dungeon");
		final java.util.List<DungeonPiece> varrock = index.piecesFor("varrock_sewers");
		assertFalse(edgeville.isEmpty());
		assertEquals(varrock.size(), edgeville.size());
		assertEquals(varrock.get(0).id, edgeville.get(0).id);
		assertTrue(index.hasPieces("edgeville_dungeon"));
		assertEquals(varrock.get(0), index.pieceAt("edgeville_dungeon", 3100, 9870, 0, null));
	}

	@Test
	public void wyrmscraigCavernHasAProjectedPreviewPiece()
	{
		final DungeonPieceIndex index = new DungeonPieceIndex();
		index.load(null);
		final DungeonPiece piece = index.pieceAt("wyrmscraig_cavern", 2593, 8639, 0, null);
		assertTrue(piece != null);
		assertEquals(-40, piece.dx);
		assertEquals(-6400, piece.dy);
		assertTrue(piece.worldBounds()[0] <= 2540 && piece.worldBounds()[2] >= 2540);
	}

	@Test
	public void crandorAndKaramjaShareOnlyTheirDungeonPieces()
	{
		final DungeonPieceIndex index = new DungeonPieceIndex();
		index.load(null);
		final List<DungeonPiece> crandor = index.piecesFor("crandor_dungeon");
		final List<DungeonPiece> karamja = index.piecesFor("karamja_dungeon");
		assertFalse(crandor.isEmpty());
		assertEquals(karamja.size(), crandor.size());
		assertTrue(crandor.stream().anyMatch(piece -> piece.id == 153));
		assertTrue(crandor.stream().anyMatch(piece -> piece.id == 186));
		assertTrue(crandor.stream().allMatch(piece -> "karamja_dungeon".equals(piece.zoneId)));
		assertEquals(186, index.pieceAt("crandor_dungeon", 2833, 9658, 0, null).id);
		assertEquals(153, index.pieceAt("karamja_dungeon", 2855, 9568, 0, null).id);
	}

	@Test
	public void asgarnianIceDungeonUsesItsFourPieces()
	{
		final DungeonPieceIndex index = new DungeonPieceIndex();
		index.load(null);
		final List<DungeonPiece> ice = index.piecesFor("asgarnia_ice_cave");
		assertEquals(3, ice.size());
		assertTrue(ice.stream().allMatch(piece -> piece.layer == 1007));
		assertEquals(19, index.pieceAt("asgarnia_ice_cave", 3007, 9550, 0, null).id);
		assertEquals(20, index.pieceAt("asgarnia_ice_cave", 2900, 9500, 0, null).id);
	}

	@Test
	public void cerberusLairLinesUpWithTaverleyUndergroundEntrance()
	{
		final DungeonPieceIndex index = new DungeonPieceIndex();
		index.load(null);
		final DungeonPiece cerberusPiece = index.pieceAt("taverley_dungeon", 1310, 1251, 0, 1399);
		assertTrue(cerberusPiece != null);
		assertEquals(1564, cerberusPiece.dx);
		assertEquals(2196, cerberusPiece.dy);
		assertEquals(2874, 1310 + cerberusPiece.dx);
		assertEquals(3447, 1251 + cerberusPiece.dy);
	}

	@Test
	public void faladorAndAsgarniaDungeonsAreIndependent()
	{
		final UndergroundZone[] asgarniaZones = {
			UndergroundZone.TAVERLEY_DUNGEON,
			UndergroundZone.DWARVEN_MINES,
			UndergroundZone.MINING_GUILD,
			UndergroundZone.MOTHERLODE_MINE,
			UndergroundZone.MOLE_HOLE,
			UndergroundZone.STRONGHOLD_OF_SECURITY,
			UndergroundZone.WHITE_KNIGHTS_CASTLE_CRYPT,
			UndergroundZone.ASGARNIA_ICE_CAVE,
			UndergroundZone.ICE_QUEEN_LAIR,
			UndergroundZone.HEROES_GUILD_MINE,
			UndergroundZone.ROGUES_DEN,
			UndergroundZone.WARRIORS_GUILD_BASEMENT,
			UndergroundZone.PORT_SARIM_RAT_PITS,
			UndergroundZone.DRAYNOR_MANOR_BASEMENT
		};
		for (UndergroundZone zone : asgarniaZones)
		{
			final List<UndergroundZone> connected = zone.getConnectedZones();
			assertEquals("Zone " + zone + " should be independent", 1, connected.size());
			assertEquals("Zone " + zone + " should only connect to itself", zone, connected.get(0));
		}
	}
}