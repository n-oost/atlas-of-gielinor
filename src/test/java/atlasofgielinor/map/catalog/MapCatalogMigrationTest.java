package atlasofgielinor.map.catalog;

import atlasofgielinor.map.MapCatalogLoader;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class MapCatalogMigrationTest
{
	@BeforeClass
	public static void load()
	{
		MapCatalogLoader.load(new com.google.gson.Gson());
		assertTrue("All catalog adapters must publish successfully", MapCatalogLoader.isReady());
	}

	@Test
	public void allRenderedPoisAndTheirDetailsComeFromTheGlobalTable()
	{
		PoiIndex index = new PoiIndex(new com.google.gson.Gson());
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
		PoiIndex index = new PoiIndex(new com.google.gson.Gson());
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
	public void reviewedReferenceConflictsUseTheSupportedSubjects()
	{
		final PoiDetails.Detail pandemonium = detail("poi_000487");
		assertTrue(pandemonium.getTitle().contains("Shrimp"));
		assertTrue(pandemonium.getLines().stream().anyMatch(line -> line.contains("Sardine")));
		assertFalse(pandemonium.getLines().stream().anyMatch(line -> line.contains("Harpoonfish")));

		assertEquals("Maple trees", MapCatalog.current().locations.get("poi_000005").label);
		assertTrue(detail("poi_000005").getLines().contains("Level requirement: Level 45 Woodcutting"));

		final PoiDetails.Detail molch = detail("poi_002111");
		assertEquals("Lake Molch - Aerial fishing", molch.getTitle());
		assertTrue(molch.getLines().stream().anyMatch(line -> line.contains("Bluegill (35)")));
		assertFalse(molch.getLines().stream().anyMatch(line -> line.contains("graahk")));
	}

	@Test
	public void reviewedGameplayClaimsReachTheTooltip()
	{
		assertTrue(detail("poi_005772").getLines().stream().anyMatch(line -> line.contains("Requires completion of Eagles' Peak")));
		assertTrue(detail("poi_005779").getLines().stream().anyMatch(line -> line.contains("excluding Prayer and heals 8 Hitpoints")));
		assertTrue(detail("poi_005780").getLines().stream().anyMatch(line -> line.contains("Restores 22 Prayer points")));
		assertTrue(detail("poi_005780").getLines().stream().anyMatch(line -> line.contains("Multicombat") && line.contains("Accept Aid")));
	}

	@Test
	public void herbiboarMarkersAndSearchAliasesDoNotDescribeBirdhouses()
	{
		final PoiIndex index = new PoiIndex(new com.google.gson.Gson());
		index.load(null);
		for (String id : new String[]{"poi_002475", "poi_002498", "poi_002528", "poi_007823", "poi_007824", "poi_007825"})
		{
			final PoiDetails.Detail herbiboar = detail(id);
			assertEquals("Fossil Island - Herbiboar (Level 80)", herbiboar.getTitle());
			assertTrue(herbiboar.getLines().contains("Level requirement: Level 80 Hunter & 31 Herblore"));
			assertTrue(herbiboar.getLines().contains("Catch method: Tracking"));
			assertTrue(index.searchEntries().stream().anyMatch(poi -> id.equals(poi.getLocationId())));
			assertFalse(index.searchEntries().stream().anyMatch(poi -> id.equals(poi.getLocationId()) && poi.getName().contains("Birdhouse")));
		}
	}

	@Test
	public void rejectedIslandPyreFoxIsAbsentFromMarkersSearchAndTooltipLookup()
	{
		assertFalse(MapCatalog.current().locations.containsKey("poi_005784"));
		assertFalse(MapCatalog.current().pois.containsKey("poi_005784"));
		assertFalse(MapCatalog.current().locations.containsKey("poi_005783"));
		assertFalse(MapCatalog.current().pois.containsKey("poi_005783"));
		assertFalse(MapCatalog.current().locations.containsKey("poi_005785"));
		assertFalse(MapCatalog.current().pois.containsKey("poi_005785"));
		final PoiIndex index = new PoiIndex(new com.google.gson.Gson());
		index.load(null);
		assertFalse(index.all().stream().anyMatch(poi -> "poi_005784".equals(poi.getLocationId())));
		assertFalse(index.searchEntries().stream().anyMatch(poi -> "poi_005784".equals(poi.getLocationId())));
		assertFalse(index.all().stream().anyMatch(poi -> "poi_005783".equals(poi.getLocationId())));
		assertFalse(index.searchEntries().stream().anyMatch(poi -> "poi_005783".equals(poi.getLocationId())));
		assertFalse(index.all().stream().anyMatch(poi -> "poi_005785".equals(poi.getLocationId())));
		assertFalse(index.searchEntries().stream().anyMatch(poi -> "poi_005785".equals(poi.getLocationId())));
		assertNull(PoiDetails.getDetailByPosition(2250, 2900, 0, 0));
		assertNull(PoiDetails.getDetailByPosition(2220, 2870, 0, 0));
		assertNull(PoiDetails.getDetailByPosition(2190, 2880, 0, 0));
		assertTrue(index.all().stream().anyMatch(poi -> "poi_000490".equals(poi.getLocationId())));
		assertTrue(index.searchEntries().stream().anyMatch(poi -> "poi_007813".equals(poi.getLocationId())));
		assertTrue(index.all().stream().anyMatch(poi -> "poi_000386".equals(poi.getLocationId())));
		assertTrue(index.all().stream().anyMatch(poi -> "poi_000257".equals(poi.getLocationId())));
	}

	

	

	

	

	
	@Test
	public void ghostPoisAreCompletelyRemoved()
	{
        assertNull(MapCatalog.current().locations.get("poi_005701"));
        assertNull(MapCatalog.current().pois.get("poi_005701"));
        assertNull(MapCatalog.current().locations.get("poi_005710"));
        assertNull(MapCatalog.current().pois.get("poi_005710"));
        assertNull(MapCatalog.current().locations.get("poi_005713"));
        assertNull(MapCatalog.current().pois.get("poi_005713"));
        assertNull(MapCatalog.current().locations.get("poi_005717"));
        assertNull(MapCatalog.current().pois.get("poi_005717"));
        assertNull(MapCatalog.current().locations.get("poi_005722"));
        assertNull(MapCatalog.current().pois.get("poi_005722"));
        assertNull(MapCatalog.current().locations.get("poi_005726"));
        assertNull(MapCatalog.current().pois.get("poi_005726"));
        assertNull(MapCatalog.current().locations.get("poi_005727"));
        assertNull(MapCatalog.current().pois.get("poi_005727"));
        assertNull(MapCatalog.current().locations.get("poi_005733"));
        assertNull(MapCatalog.current().pois.get("poi_005733"));
        assertNull(MapCatalog.current().locations.get("poi_005735"));
        assertNull(MapCatalog.current().pois.get("poi_005735"));
        assertNull(MapCatalog.current().locations.get("poi_005749"));
        assertNull(MapCatalog.current().pois.get("poi_005749"));
        assertNull(MapCatalog.current().locations.get("poi_005750"));
        assertNull(MapCatalog.current().pois.get("poi_005750"));
        assertNull(MapCatalog.current().locations.get("poi_005753"));
        assertNull(MapCatalog.current().pois.get("poi_005753"));
        assertNull(MapCatalog.current().locations.get("poi_005759"));
        assertNull(MapCatalog.current().pois.get("poi_005759"));
        assertNull(MapCatalog.current().locations.get("poi_005761"));
        assertNull(MapCatalog.current().pois.get("poi_005761"));
        assertNull(MapCatalog.current().locations.get("poi_005763"));
        assertNull(MapCatalog.current().pois.get("poi_005763"));
        assertNull(MapCatalog.current().locations.get("poi_005764"));
        assertNull(MapCatalog.current().pois.get("poi_005764"));
        assertNull(MapCatalog.current().locations.get("poi_005767"));
        assertNull(MapCatalog.current().pois.get("poi_005767"));
        assertNull(MapCatalog.current().locations.get("poi_005769"));
        assertNull(MapCatalog.current().pois.get("poi_005769"));
        assertNull(MapCatalog.current().locations.get("poi_005770"));
        assertNull(MapCatalog.current().pois.get("poi_005770"));
        assertNull(MapCatalog.current().locations.get("poi_005771"));
        assertNull(MapCatalog.current().pois.get("poi_005771"));
        assertNull(MapCatalog.current().locations.get("poi_005782"));
        assertNull(MapCatalog.current().pois.get("poi_005782"));
        assertNull(MapCatalog.current().locations.get("poi_005787"));
        assertNull(MapCatalog.current().pois.get("poi_005787"));
        assertNull(MapCatalog.current().locations.get("poi_005788"));
        assertNull(MapCatalog.current().pois.get("poi_005788"));
        assertNull(MapCatalog.current().locations.get("poi_005789"));
        assertNull(MapCatalog.current().pois.get("poi_005789"));
        assertNull(MapCatalog.current().locations.get("poi_005790"));
        assertNull(MapCatalog.current().pois.get("poi_005790"));
        assertNull(MapCatalog.current().locations.get("poi_005791"));
        assertNull(MapCatalog.current().pois.get("poi_005791"));
        assertNull(MapCatalog.current().locations.get("poi_005792"));
        assertNull(MapCatalog.current().pois.get("poi_005792"));
        assertNull(MapCatalog.current().locations.get("poi_005793"));
        assertNull(MapCatalog.current().pois.get("poi_005793"));
        assertNull(MapCatalog.current().locations.get("poi_005794"));
        assertNull(MapCatalog.current().pois.get("poi_005794"));
        assertNull(MapCatalog.current().locations.get("poi_005795"));
        assertNull(MapCatalog.current().pois.get("poi_005795"));
        assertNull(MapCatalog.current().locations.get("poi_005796"));
        assertNull(MapCatalog.current().pois.get("poi_005796"));
        assertNull(MapCatalog.current().locations.get("poi_005798"));
        assertNull(MapCatalog.current().pois.get("poi_005798"));
        assertNull(MapCatalog.current().locations.get("poi_005799"));
        assertNull(MapCatalog.current().pois.get("poi_005799"));
        assertNull(MapCatalog.current().locations.get("poi_005800"));
        assertNull(MapCatalog.current().pois.get("poi_005800"));
        assertNull(MapCatalog.current().locations.get("poi_005801"));
        assertNull(MapCatalog.current().pois.get("poi_005801"));
        assertNull(MapCatalog.current().locations.get("poi_005802"));
        assertNull(MapCatalog.current().pois.get("poi_005802"));
        assertNull(MapCatalog.current().locations.get("poi_005805"));
        assertNull(MapCatalog.current().pois.get("poi_005805"));
        assertNull(MapCatalog.current().locations.get("poi_005806"));
        assertNull(MapCatalog.current().pois.get("poi_005806"));
        assertNull(MapCatalog.current().locations.get("poi_005807"));
        assertNull(MapCatalog.current().pois.get("poi_005807"));
        assertNull(MapCatalog.current().locations.get("poi_005808"));
        assertNull(MapCatalog.current().pois.get("poi_005808"));
        assertNull(MapCatalog.current().locations.get("poi_005809"));
        assertNull(MapCatalog.current().pois.get("poi_005809"));
        assertNull(MapCatalog.current().locations.get("poi_005810"));
        assertNull(MapCatalog.current().pois.get("poi_005810"));
        assertNull(MapCatalog.current().locations.get("poi_005812"));
        assertNull(MapCatalog.current().pois.get("poi_005812"));
        assertNull(MapCatalog.current().locations.get("poi_005817"));
        assertNull(MapCatalog.current().pois.get("poi_005817"));
        assertNull(MapCatalog.current().locations.get("poi_005831"));
        assertNull(MapCatalog.current().pois.get("poi_005831"));
        assertNull(MapCatalog.current().locations.get("poi_005834"));
        assertNull(MapCatalog.current().pois.get("poi_005834"));
        assertNull(MapCatalog.current().locations.get("poi_005842"));
        assertNull(MapCatalog.current().pois.get("poi_005842"));
        assertNull(MapCatalog.current().locations.get("poi_005843"));
        assertNull(MapCatalog.current().pois.get("poi_005843"));
        assertNull(MapCatalog.current().locations.get("poi_005848"));
        assertNull(MapCatalog.current().pois.get("poi_005848"));
        assertNull(MapCatalog.current().locations.get("poi_005850"));
        assertNull(MapCatalog.current().pois.get("poi_005850"));
        assertNull(MapCatalog.current().locations.get("poi_005853"));
        assertNull(MapCatalog.current().pois.get("poi_005853"));
        assertNull(MapCatalog.current().locations.get("poi_005854"));
        assertNull(MapCatalog.current().pois.get("poi_005854"));
        assertNull(MapCatalog.current().locations.get("poi_005855"));
        assertNull(MapCatalog.current().pois.get("poi_005855"));
        assertNull(MapCatalog.current().locations.get("poi_005856"));
        assertNull(MapCatalog.current().pois.get("poi_005856"));
        assertNull(MapCatalog.current().locations.get("poi_005859"));
        assertNull(MapCatalog.current().pois.get("poi_005859"));
        assertNull(MapCatalog.current().locations.get("poi_005861"));
        assertNull(MapCatalog.current().pois.get("poi_005861"));
        assertNull(MapCatalog.current().locations.get("poi_005871"));
        assertNull(MapCatalog.current().pois.get("poi_005871"));
        assertNull(MapCatalog.current().locations.get("poi_005872"));
        assertNull(MapCatalog.current().pois.get("poi_005872"));
        assertNull(MapCatalog.current().locations.get("poi_005880"));
        assertNull(MapCatalog.current().pois.get("poi_005880"));
        assertNull(MapCatalog.current().locations.get("poi_005889"));
        assertNull(MapCatalog.current().pois.get("poi_005889"));
        assertNull(MapCatalog.current().locations.get("poi_005890"));
        assertNull(MapCatalog.current().pois.get("poi_005890"));
        assertNull(MapCatalog.current().locations.get("poi_005891"));
        assertNull(MapCatalog.current().pois.get("poi_005891"));
        assertNull(MapCatalog.current().locations.get("poi_006900"));
        assertNull(MapCatalog.current().pois.get("poi_006900"));
        assertNull(MapCatalog.current().locations.get("poi_006901"));
        assertNull(MapCatalog.current().pois.get("poi_006901"));
        assertNull(MapCatalog.current().locations.get("poi_006902"));
        assertNull(MapCatalog.current().pois.get("poi_006902"));
        assertNull(MapCatalog.current().locations.get("poi_006903"));
        assertNull(MapCatalog.current().pois.get("poi_006903"));
        assertNull(MapCatalog.current().locations.get("poi_006904"));
        assertNull(MapCatalog.current().pois.get("poi_006904"));
        assertNull(MapCatalog.current().locations.get("poi_006905"));
        assertNull(MapCatalog.current().pois.get("poi_006905"));
        assertNull(MapCatalog.current().locations.get("poi_006906"));
        assertNull(MapCatalog.current().pois.get("poi_006906"));
        assertNull(MapCatalog.current().locations.get("poi_006907"));
        assertNull(MapCatalog.current().pois.get("poi_006907"));
        assertNull(MapCatalog.current().locations.get("poi_006908"));
        assertNull(MapCatalog.current().pois.get("poi_006908"));
        assertNull(MapCatalog.current().locations.get("poi_006909"));
        assertNull(MapCatalog.current().pois.get("poi_006909"));
        assertNull(MapCatalog.current().locations.get("poi_006910"));
        assertNull(MapCatalog.current().pois.get("poi_006910"));
        assertNull(MapCatalog.current().locations.get("poi_006911"));
        assertNull(MapCatalog.current().pois.get("poi_006911"));
        assertNull(MapCatalog.current().locations.get("poi_006912"));
        assertNull(MapCatalog.current().pois.get("poi_006912"));
        assertNull(MapCatalog.current().locations.get("poi_006913"));
        assertNull(MapCatalog.current().pois.get("poi_006913"));
        assertNull(MapCatalog.current().locations.get("poi_006914"));
        assertNull(MapCatalog.current().pois.get("poi_006914"));
        assertNull(MapCatalog.current().locations.get("poi_006915"));
        assertNull(MapCatalog.current().pois.get("poi_006915"));
        assertNull(MapCatalog.current().locations.get("poi_006916"));
        assertNull(MapCatalog.current().pois.get("poi_006916"));
        assertNull(MapCatalog.current().locations.get("poi_006917"));
        assertNull(MapCatalog.current().pois.get("poi_006917"));
        assertNull(MapCatalog.current().locations.get("poi_006918"));
        assertNull(MapCatalog.current().pois.get("poi_006918"));
        assertNull(MapCatalog.current().locations.get("poi_006919"));
        assertNull(MapCatalog.current().pois.get("poi_006919"));
        assertNull(MapCatalog.current().locations.get("poi_006920"));
        assertNull(MapCatalog.current().pois.get("poi_006920"));
        assertNull(MapCatalog.current().locations.get("poi_006921"));
        assertNull(MapCatalog.current().pois.get("poi_006921"));
        assertNull(MapCatalog.current().locations.get("poi_006922"));
        assertNull(MapCatalog.current().pois.get("poi_006922"));
        assertNull(MapCatalog.current().locations.get("poi_006923"));
        assertNull(MapCatalog.current().pois.get("poi_006923"));
        assertNull(MapCatalog.current().locations.get("poi_006924"));
        assertNull(MapCatalog.current().pois.get("poi_006924"));
        assertNull(MapCatalog.current().locations.get("poi_006925"));
        assertNull(MapCatalog.current().pois.get("poi_006925"));
        assertNull(MapCatalog.current().locations.get("poi_006926"));
        assertNull(MapCatalog.current().pois.get("poi_006926"));
        assertNull(MapCatalog.current().locations.get("poi_006927"));
        assertNull(MapCatalog.current().pois.get("poi_006927"));
        assertNull(MapCatalog.current().locations.get("poi_006928"));
        assertNull(MapCatalog.current().pois.get("poi_006928"));
        assertNull(MapCatalog.current().locations.get("poi_006929"));
        assertNull(MapCatalog.current().pois.get("poi_006929"));
        assertNull(MapCatalog.current().locations.get("poi_006930"));
        assertNull(MapCatalog.current().pois.get("poi_006930"));
        assertNull(MapCatalog.current().locations.get("poi_006931"));
        assertNull(MapCatalog.current().pois.get("poi_006931"));
        assertNull(MapCatalog.current().locations.get("poi_006932"));
        assertNull(MapCatalog.current().pois.get("poi_006932"));
        assertNull(MapCatalog.current().locations.get("poi_006933"));
        assertNull(MapCatalog.current().pois.get("poi_006933"));
        assertNull(MapCatalog.current().locations.get("poi_006934"));
        assertNull(MapCatalog.current().pois.get("poi_006934"));
        assertNull(MapCatalog.current().locations.get("poi_006935"));
        assertNull(MapCatalog.current().pois.get("poi_006935"));
        assertNull(MapCatalog.current().locations.get("poi_006936"));
        assertNull(MapCatalog.current().pois.get("poi_006936"));
        assertNull(MapCatalog.current().locations.get("poi_006937"));
        assertNull(MapCatalog.current().pois.get("poi_006937"));
        assertNull(MapCatalog.current().locations.get("poi_006938"));
        assertNull(MapCatalog.current().pois.get("poi_006938"));
        assertNull(MapCatalog.current().locations.get("poi_006939"));
        assertNull(MapCatalog.current().pois.get("poi_006939"));
        assertNull(MapCatalog.current().locations.get("poi_006940"));
        assertNull(MapCatalog.current().pois.get("poi_006940"));
        assertNull(MapCatalog.current().locations.get("poi_006941"));
        assertNull(MapCatalog.current().pois.get("poi_006941"));
        assertNull(MapCatalog.current().locations.get("poi_006942"));
        assertNull(MapCatalog.current().pois.get("poi_006942"));
        assertNull(MapCatalog.current().locations.get("poi_006943"));
        assertNull(MapCatalog.current().pois.get("poi_006943"));
        assertNull(MapCatalog.current().locations.get("poi_006944"));
        assertNull(MapCatalog.current().pois.get("poi_006944"));
        assertNull(MapCatalog.current().locations.get("poi_006945"));
        assertNull(MapCatalog.current().pois.get("poi_006945"));
        assertNull(MapCatalog.current().locations.get("poi_006946"));
        assertNull(MapCatalog.current().pois.get("poi_006946"));
        assertNull(MapCatalog.current().locations.get("poi_006947"));
        assertNull(MapCatalog.current().pois.get("poi_006947"));
        assertNull(MapCatalog.current().locations.get("poi_006948"));
        assertNull(MapCatalog.current().pois.get("poi_006948"));
        assertNull(MapCatalog.current().locations.get("poi_006949"));
        assertNull(MapCatalog.current().pois.get("poi_006949"));
        assertNull(MapCatalog.current().locations.get("poi_006950"));
        assertNull(MapCatalog.current().pois.get("poi_006950"));
        assertNull(MapCatalog.current().locations.get("poi_006951"));
        assertNull(MapCatalog.current().pois.get("poi_006951"));
        assertNull(MapCatalog.current().locations.get("poi_006952"));
        assertNull(MapCatalog.current().pois.get("poi_006952"));
        assertNull(MapCatalog.current().locations.get("poi_006953"));
        assertNull(MapCatalog.current().pois.get("poi_006953"));
        assertNull(MapCatalog.current().locations.get("poi_006954"));
        assertNull(MapCatalog.current().pois.get("poi_006954"));
        assertNull(MapCatalog.current().locations.get("poi_006955"));
        assertNull(MapCatalog.current().pois.get("poi_006955"));
        assertNull(MapCatalog.current().locations.get("poi_006956"));
        assertNull(MapCatalog.current().pois.get("poi_006956"));
        assertNull(MapCatalog.current().locations.get("poi_006957"));
        assertNull(MapCatalog.current().pois.get("poi_006957"));
        assertNull(MapCatalog.current().locations.get("poi_006958"));
        assertNull(MapCatalog.current().pois.get("poi_006958"));
        assertNull(MapCatalog.current().locations.get("poi_006959"));
        assertNull(MapCatalog.current().pois.get("poi_006959"));
        assertNull(MapCatalog.current().locations.get("poi_006960"));
        assertNull(MapCatalog.current().pois.get("poi_006960"));
        assertNull(MapCatalog.current().locations.get("poi_006961"));
        assertNull(MapCatalog.current().pois.get("poi_006961"));
        assertNull(MapCatalog.current().locations.get("poi_006962"));
        assertNull(MapCatalog.current().pois.get("poi_006962"));
        assertNull(MapCatalog.current().locations.get("poi_006963"));
        assertNull(MapCatalog.current().pois.get("poi_006963"));
        assertNull(MapCatalog.current().locations.get("poi_006964"));
        assertNull(MapCatalog.current().pois.get("poi_006964"));
        assertNull(MapCatalog.current().locations.get("poi_006965"));
        assertNull(MapCatalog.current().pois.get("poi_006965"));
        assertNull(MapCatalog.current().locations.get("poi_006966"));
        assertNull(MapCatalog.current().pois.get("poi_006966"));
        assertNull(MapCatalog.current().locations.get("poi_006967"));
        assertNull(MapCatalog.current().pois.get("poi_006967"));
        assertNull(MapCatalog.current().locations.get("poi_006968"));
        assertNull(MapCatalog.current().pois.get("poi_006968"));
        assertNull(MapCatalog.current().locations.get("poi_006970"));
        assertNull(MapCatalog.current().pois.get("poi_006970"));
        assertNull(MapCatalog.current().locations.get("poi_006971"));
        assertNull(MapCatalog.current().pois.get("poi_006971"));
        assertNull(MapCatalog.current().locations.get("poi_006972"));
        assertNull(MapCatalog.current().pois.get("poi_006972"));
        assertNull(MapCatalog.current().locations.get("poi_006973"));
        assertNull(MapCatalog.current().pois.get("poi_006973"));
        assertNull(MapCatalog.current().locations.get("poi_006974"));
        assertNull(MapCatalog.current().pois.get("poi_006974"));
        assertNull(MapCatalog.current().locations.get("poi_006975"));
        assertNull(MapCatalog.current().pois.get("poi_006975"));
        assertNull(MapCatalog.current().locations.get("poi_006976"));
        assertNull(MapCatalog.current().pois.get("poi_006976"));
        assertNull(MapCatalog.current().locations.get("poi_006977"));
        assertNull(MapCatalog.current().pois.get("poi_006977"));
        assertNull(MapCatalog.current().locations.get("poi_006978"));
        assertNull(MapCatalog.current().pois.get("poi_006978"));
        assertNull(MapCatalog.current().locations.get("poi_006979"));
        assertNull(MapCatalog.current().pois.get("poi_006979"));
        assertNull(MapCatalog.current().locations.get("poi_006980"));
        assertNull(MapCatalog.current().pois.get("poi_006980"));
        assertNull(MapCatalog.current().locations.get("poi_006981"));
        assertNull(MapCatalog.current().pois.get("poi_006981"));
        assertNull(MapCatalog.current().locations.get("poi_006982"));
        assertNull(MapCatalog.current().pois.get("poi_006982"));
        assertNull(MapCatalog.current().locations.get("poi_006983"));
        assertNull(MapCatalog.current().pois.get("poi_006983"));
        assertNull(MapCatalog.current().locations.get("poi_006984"));
        assertNull(MapCatalog.current().pois.get("poi_006984"));
        assertNull(MapCatalog.current().locations.get("poi_006985"));
        assertNull(MapCatalog.current().pois.get("poi_006985"));
        assertNull(MapCatalog.current().locations.get("poi_006986"));
        assertNull(MapCatalog.current().pois.get("poi_006986"));
        assertNull(MapCatalog.current().locations.get("poi_006987"));
        assertNull(MapCatalog.current().pois.get("poi_006987"));
        assertNull(MapCatalog.current().locations.get("poi_006988"));
        assertNull(MapCatalog.current().pois.get("poi_006988"));
        assertNull(MapCatalog.current().locations.get("poi_006989"));
        assertNull(MapCatalog.current().pois.get("poi_006989"));
        assertNull(MapCatalog.current().locations.get("poi_006990"));
        assertNull(MapCatalog.current().pois.get("poi_006990"));
        assertNull(MapCatalog.current().locations.get("poi_006991"));
        assertNull(MapCatalog.current().pois.get("poi_006991"));
        assertNull(MapCatalog.current().locations.get("poi_006992"));
        assertNull(MapCatalog.current().pois.get("poi_006992"));
        assertNull(MapCatalog.current().locations.get("poi_006993"));
        assertNull(MapCatalog.current().pois.get("poi_006993"));
        assertNull(MapCatalog.current().locations.get("poi_006994"));
        assertNull(MapCatalog.current().pois.get("poi_006994"));
        assertNull(MapCatalog.current().locations.get("poi_006995"));
        assertNull(MapCatalog.current().pois.get("poi_006995"));
        assertNull(MapCatalog.current().locations.get("poi_006996"));
        assertNull(MapCatalog.current().pois.get("poi_006996"));
        assertNull(MapCatalog.current().locations.get("poi_006997"));
        assertNull(MapCatalog.current().pois.get("poi_006997"));
        assertNull(MapCatalog.current().locations.get("poi_006998"));
        assertNull(MapCatalog.current().pois.get("poi_006998"));
        assertNull(MapCatalog.current().locations.get("poi_006999"));
        assertNull(MapCatalog.current().pois.get("poi_006999"));
	}

	private static PoiDetails.Detail detail(String id)
	{
		final MapCatalog.Location row = MapCatalog.current().locations.get(id);
		assertNotNull(id, row);
		return PoiDetails.getDetail(new PoiIndex.Poi(row.x, row.y, row.plane, row.iconKey, row.label, row.id), row.x, row.y, row.plane);
	}

}
