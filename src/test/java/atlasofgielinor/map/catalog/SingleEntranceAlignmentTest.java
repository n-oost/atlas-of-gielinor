package atlasofgielinor.map.catalog;

import atlasofgielinor.data.dungeons.DungeonPiece;
import atlasofgielinor.data.dungeons.DungeonPieceTransform;
import atlasofgielinor.data.dungeons.UndergroundZone;
import atlasofgielinor.map.DungeonPieceIndex;
import atlasofgielinor.map.MapCatalogLoader;
import java.awt.geom.Point2D;
import org.junit.Test;
import static org.junit.Assert.*;

public class SingleEntranceAlignmentTest
{
	@Test
	public void isleUsesActualExitAndBothPiecesAlignWithoutMirroring()
	{
		MapCatalogLoader.load();
		assertTrue(MapCatalogLoader.isReady());
		MapCatalog catalog = MapCatalog.current();
		MapCatalog.Location entrance = catalog.locations.get("poi_000347");
		MapCatalog.Location exit = catalog.locations.get("poi_003193");
		assertEquals("isle_of_souls_dungeon", entrance.placeId);
		assertEquals(entrance.placeId, exit.placeId);
		assertEquals("entrance", entrance.role);
		assertEquals("exit", exit.role);
		assertEquals(2308, entrance.x);
		assertEquals(2168, exit.x);
		assertEquals(exit.point(), UndergroundZone.ISLE_OF_SOULS_DUNGEON.getUndergroundPoint());
		MapCatalog.Location interior = catalog.locations.get("poi_005619");
		assertEquals("interior", interior.role);
		assertFalse(interior.rendered);
		assertEquals(2135, interior.x);
		assertEquals(9320, interior.y);
		assertEquals(1, catalog.locations.values().stream().filter(l -> l.placeId.equals(entrance.placeId) && l.rendered && "exit".equals(l.role)).count());
		DungeonPieceIndex index = new DungeonPieceIndex();
		index.load(null);
		int checked = 0;
		for (DungeonPiece piece : index.piecesFor("isle_of_souls_dungeon"))
		{
			if (piece.id != 656 && piece.id != 100332) continue;
			assertFalse(piece.flipX);
			assertFalse(piece.flipY);
			assertEquals(0, piece.rot);
			Point2D point = DungeonPieceTransform.toDisplay(piece, exit.x + 0.5, exit.y + 0.5);
			assertEquals(entrance.x + 0.5, point.getX(), 1e-9);
			assertEquals(entrance.y + 0.5, point.getY(), 1e-9);
			Point2D east = DungeonPieceTransform.toDisplay(piece, exit.x + 1.5, exit.y + 0.5);
			Point2D north = DungeonPieceTransform.toDisplay(piece, exit.x + 0.5, exit.y + 1.5);
			assertEquals(point.getX() + 1, east.getX(), 1e-9);
			assertEquals(point.getY() + 1, north.getY(), 1e-9);
			Point2D nativePoint = DungeonPieceTransform.toNative(piece, point.getX(), point.getY());
			assertEquals(exit.x + 0.5, nativePoint.getX(), 1e-9);
			assertEquals(exit.y + 0.5, nativePoint.getY(), 1e-9);
			checked++;
		}
		assertEquals(2, checked);
	}
}
