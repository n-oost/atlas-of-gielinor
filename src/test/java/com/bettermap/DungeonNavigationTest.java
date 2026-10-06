package com.bettermap;

import java.awt.Rectangle;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;
import org.junit.Before;
import org.junit.Test;

import bettermap.BetterMapConfig;
import bettermap.BetterMapPlugin;
import bettermap.data.DungeonPiece;
import bettermap.data.UndergroundZone;
import bettermap.map.DungeonPieceIndex;
import bettermap.map.MapCamera;
import bettermap.map.ShortestPathTracker;

import static org.junit.Assert.*;

public class DungeonNavigationTest
{
	@org.junit.BeforeClass
	public static void loadMapData()
	{
		bettermap.map.MapData.load();
		org.junit.Assert.assertTrue("Map catalogs must load before testing", bettermap.map.MapData.isReady());
	}

	private BetterMapPlugin plugin;
	private MapCamera camera;
	private WorldPoint routed;
	private final WorldPoint nativePoint = new WorldPoint(2884, 9798, 0);

	@Before
	public void setUp() throws Exception
	{
		plugin = new BetterMapPlugin();
		camera = new MapCamera();
		camera.setActive(true);
		camera.setViewport(new Rectangle(0, 0, 800, 600));
		final BetterMapConfig config = new BetterMapConfig() {};
		final DungeonPiece piece = new DungeonPiece(1, 1, 0, "Rotated room",
			new int[][] {{2880, 9790, 2899, 9809}}, 0, -6400, 90, true, false, false,
			UndergroundZone.TAVERLEY_DUNGEON.getId());
		setField("camera", camera);
		setField("config", config);
		setField("dungeonPieceIndex", new DungeonPieceIndex()
		{
			@Override
			public synchronized List<DungeonPiece> piecesFor(String zoneId)
			{
				return UndergroundZone.TAVERLEY_DUNGEON.getId().equals(zoneId)
					? Collections.singletonList(piece) : Collections.emptyList();
			}
		});
		setField("client", Proxy.newProxyInstance(getClass().getClassLoader(),
			new Class<?>[] {Client.class}, (proxy, method, args) -> null));
		setField("clientThread", new ClientThread()
		{
			@Override
			public void invoke(Runnable action)
			{
				action.run();
			}

			@Override
			public void invoke(java.util.function.BooleanSupplier action)
			{
				action.getAsBoolean();
			}
		});
		setField("shortestPathTracker", new ShortestPathTracker(null, null, config)
		{
			@Override
			public boolean routeTo(WorldPoint destination)
			{
				routed = destination;
				return true;
			}
		});
	}

	private void setField(String name, Object value) throws Exception
	{
		final Field field = BetterMapPlugin.class.getDeclaredField(name);
		field.setAccessible(true);
		field.set(plugin, value);
	}

	@Test
	public void goToPlayerCentersOnTheRotatedAndFlippedRoom() throws Exception
	{
		camera.setPlayerLocation(nativePoint);
		setField("lastPlayerLocation", nativePoint);
		plugin.goToPlayer();
		assertEquals(UndergroundZone.TAVERLEY_DUNGEON, camera.getActiveUndergroundZone());
		assertEquals(2888.5, camera.getCenterX(), 0.0001);
		assertEquals(3394.5, camera.getCenterY(), 0.0001);
	}

	@Test
	public void shiftClickRoutesToNativeTileInTheRotatedAndFlippedRoom()
	{
		camera.setUndergroundMode(UndergroundZone.TAVERLEY_DUNGEON);
		camera.centerOn(2888.5, 3394.5);
		camera.beginFrame();
		plugin.placeRouteAt(400, 300);
		assertEquals(nativePoint, routed);
	}

	@Test
	public void emptySpaceInDungeonDoesNotRouteToTheSurface()
	{
		camera.setUndergroundMode(UndergroundZone.TAVERLEY_DUNGEON);
		camera.centerOn(2800, 3300);
		camera.beginFrame();
		plugin.placeRouteAt(400, 300);
		assertNull(routed);
	}

	@Test
	public void surfaceDestinationClosesThePreviousDungeon()
	{
		camera.setUndergroundMode(UndergroundZone.TAVERLEY_DUNGEON);
		plugin.centerMapOn(new WorldPoint(3222, 3218, 0));
		assertNull(camera.getActiveUndergroundZone());
		assertEquals(3222, camera.getCenterX(), 0.0001);
		assertEquals(3218, camera.getCenterY(), 0.0001);
	}
}
