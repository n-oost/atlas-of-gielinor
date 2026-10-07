package atlasofgielinor.map.catalog;

import atlasofgielinor.data.dungeons.DungeonPiece;
import atlasofgielinor.data.dungeons.UndergroundZone;
import atlasofgielinor.map.DungeonPieceIndex;
import atlasofgielinor.map.InstanceMaps;
import atlasofgielinor.map.MapCatalogLoader;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import net.runelite.api.coords.WorldPoint;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class MapCatalogMigrationTest
{
	@Rule
	public TemporaryFolder temporary = new TemporaryFolder();

	@BeforeClass
	public static void load()
	{
		MapCatalogLoader.load();
		assertTrue("All catalog adapters must publish successfully", MapCatalogLoader.isReady());
	}

	@Test
	public void allRenderedPoisAndTheirDetailsComeFromTheGlobalTable()
	{
		PoiIndex index = new PoiIndex();
		index.load(null);
		assertEquals(MapCatalog.current().locations.values().stream().filter(p -> p.rendered).count(), index.all().size());
		assertTrue(MapCatalog.current().locations.size() > 7900);
		for (PoiIndex.Poi poi : index.all())
		{
			MapCatalog.Location row = MapCatalog.current().locations.get(poi.getLocationId());
			assertNotNull(poi.getName(), row);
			assertEquals(row.label, poi.getName());
			assertEquals(row.x, poi.getX());
			assertEquals(row.y, poi.getY());
			assertEquals(row.plane, poi.getPlane());
			// A tooltip lookup at another point cannot replace the selected POI's identity.
			assertEquals(row.detailTitle, PoiDetails.getDetail(poi, 1924, 2758, 0).getTitle());
		}
	}

	@Test
	public void everySearchAliasRetainsItsNativePointAndId()
	{
		PoiIndex index = new PoiIndex();
		index.load(null);
		for (PoiIndex.Poi poi : index.searchEntries())
		{
			MapCatalog.Location row = MapCatalog.current().locations.get(poi.getLocationId());
			assertNotNull(poi.getName(), row);
			assertTrue(row.searchable);
			assertTrue(row.label.equals(poi.getName()) || row.aliases.contains(poi.getName()));
			assertEquals(row.x, poi.getX());
			assertEquals(row.y, poi.getY());
			assertEquals(row.plane, poi.getPlane());
		}
	}

	@Test
	public void nativeEntranceDoesNotDetermineArtworkTranslation()
	{
		UndergroundZone zone = UndergroundZone.DEEPFIN_MINE;
		assertEquals(new WorldPoint(1940, 2806, 0), zone.getSurfacePoint());
		assertEquals(18, zone.getDeltaX());
		assertEquals(6387, zone.getDeltaY());
		assertNotEquals(zone.getUndergroundPoint().getX() - zone.getSurfacePoint().getX(), zone.getDeltaX());
		assertEquals("deepfin_point", MapCatalog.current().locations.get("deepfin_mooring").placeId);
	}

	@Test
	public void finderAndTooltipResolveTheSameEndpoint()
	{
		PoiIndex index = new PoiIndex();
		index.load(null);
		List<PoiIndex.Poi> results = index.searchByName("Deepfin Mine", 100);
		assertFalse(results.isEmpty());
		for (PoiIndex.Poi poi : results)
		{
			if (poi.getY() > 5000) continue;
			assertEquals(1940, poi.getX());
			assertEquals(2806, poi.getY());
			assertEquals("deepfin_mine_entrance", poi.getLocationId());
		}
		PoiIndex.Poi entrance = index.nearest(1940, 2806, 0, 1);
		assertNotNull(entrance);
		assertEquals("deepfin_mine_entrance", entrance.getLocationId());
		PoiDetails.Detail detail = PoiDetails.getDetail(entrance, 1924, 2758, 0);
		assertEquals("Deepfin Mine entrance", detail.getTitle());
		assertTrue(detail.getLines().contains("Enter the mine beneath Deepfin Point."));
		PoiIndex.Poi mooring = index.nearest(1924, 2758, 0, 1);
		assertEquals("mooring_point", mooring.getKey());
		assertTrue(PoiDetails.getDetail(mooring, 1924, 2758, 0).getTitle().contains("Mooring"));
	}

	@Test
	public void grimstoneOwnsItsGeometryAndGhorrockKeepsBossPieces()
	{
		DungeonPieceIndex pieces = new DungeonPieceIndex();
		pieces.load(null);
		List<DungeonPiece> grimstone = pieces.piecesFor("grimstone_dungeon");
		assertEquals(1, grimstone.size());
		assertEquals(482, grimstone.get(0).id);
		assertEquals(52, grimstone.get(0).dx);
		assertEquals(-6380, grimstone.get(0).dy);
		assertEquals(UndergroundZone.GRIMSTONE_DUNGEON, InstanceMaps.zoneForPoint(2887, 10481, 0));
		assertFalse(InstanceMaps.belongsToZone(2887, 10481, UndergroundZone.GHORROCK_DUNGEON));
		assertFalse(UndergroundZone.GHORROCK_DUNGEON.hasEntranceToggle());
		assertTrue(pieces.piecesFor("ghorrock_dungeon").stream().anyMatch(p -> p.id == 904));
		assertTrue(pieces.piecesFor("ghorrock_dungeon").stream().anyMatch(p -> p.id == 905));
		assertFalse(pieces.piecesFor("ghorrock_dungeon").stream().anyMatch(p -> p.id == 482 || p.id == 888));
	}

	@Test
	public void staleDiskGeometryCannotRestoreOldOwnership() throws Exception
	{
		File directory = temporary.newFolder("tiles");
		File dungeonDirectory = new File(directory, "dungeons");
		assertTrue(dungeonDirectory.mkdir());
		String stale = "141\t1228\t0\tDeepfin\t1924\t9160\t2107\t9215\t999\t999\t0\t1\t0\tdeepfin_mine\n"
			+ "482\t1342\t0\tGrimstone\t2876\t10440\t2931\t10487\t0\t0\t0\t1\t0\tghorrock_dungeon\n"
			+ "888\t1400\t0\tGhorrock\t2844\t10441\t2932\t10489\t0\t-6400\t0\t1\t0\tghorrock_dungeon\n";
		Files.write(new File(dungeonDirectory, "pieces.tsv").toPath(), stale.getBytes(StandardCharsets.UTF_8));
		DungeonPieceIndex pieces = new DungeonPieceIndex();
		try
		{
			pieces.load(directory);
			assertEquals(-18, pieces.piecesFor("deepfin_mine").get(0).dx);
			assertEquals(482, pieces.piecesFor("grimstone_dungeon").get(0).id);
			assertTrue(pieces.piecesFor("ghorrock_dungeon").isEmpty());
		}
		finally { pieces.load(null); }
	}

	@Test
	public void unknownLandingAndConflictingLayoutsRemainExplicit()
	{
		MapCatalog catalog = MapCatalog.current();
		assertEquals("", catalog.links.get("grimstone_dungeon_entry").toLocationId);
		assertEquals("grimstone_dungeon", catalog.links.get("grimstone_dungeon_entry").toPlaceId);
		assertFalse(catalog.layouts.get("ghorrock_legacy_piece").verified);
		assertFalse(catalog.layouts.get("grimstone_native_legacy_piece").verified);
		assertNull(catalog.primaryPiece("ghorrock_dungeon"));
	}
}
