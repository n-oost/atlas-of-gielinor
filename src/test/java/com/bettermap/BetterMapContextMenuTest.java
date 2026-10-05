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
 *    and/or other materials provided with the distribution entering with the distribution.
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
package com.bettermap;

import com.bettermap.data.UndergroundZone;
import com.bettermap.map.MapCamera;
import com.bettermap.map.ShortestPathTracker;
import com.bettermap.map.SlayerTaskTracker;
import java.awt.Rectangle;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import net.runelite.api.Client;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.MenuOpened;
import net.runelite.client.plugins.slayer.SlayerPluginService;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

public class BetterMapContextMenuTest
{
	@org.junit.BeforeClass
	public static void loadMapData()
	{
		com.bettermap.map.MapData.load();
		org.junit.Assert.assertTrue("Map catalogs must load before testing", com.bettermap.map.MapData.isReady());
	}

	private BetterMapPlugin plugin;
	private MapCamera camera;
	private final List<SimpleEntry> entries = new ArrayList<>();

	private static class SimpleEntry
	{
		String option;
		String target;
		MenuAction type;
		Consumer<MenuEntry> onClick;
	}

	private MenuEntry createProxy(SimpleEntry entry)
	{
		return (MenuEntry) Proxy.newProxyInstance(
			getClass().getClassLoader(),
			new Class<?>[]{MenuEntry.class},
			(proxy, method, args) ->
			{
				final String name = method.getName();
				if ("getOption".equals(name))
				{
					return entry.option;
				}
				if ("setOption".equals(name))
				{
					entry.option = (String) args[0];
					return proxy;
				}
				if ("getTarget".equals(name))
				{
					return entry.target;
				}
				if ("setTarget".equals(name))
				{
					entry.target = (String) args[0];
					return proxy;
				}
				if ("getType".equals(name))
				{
					return entry.type;
				}
				if ("setType".equals(name))
				{
					entry.type = (MenuAction) args[0];
					return proxy;
				}
				if ("onClick".equals(name))
				{
					entry.onClick = (Consumer<MenuEntry>) args[0];
					return proxy;
				}
				if ("getOnClick".equals(name))
				{
					return entry.onClick;
				}
				return null;
			}
		);
	}

	@Before
	public void setUp() throws Exception
	{
		plugin = new BetterMapPlugin();
		camera = new MapCamera();
		camera.setActive(true);
		camera.setViewport(new Rectangle(0, 0, 800, 600));

		final BetterMapConfig config = (BetterMapConfig) Proxy.newProxyInstance(
			getClass().getClassLoader(),
			new Class<?>[]{BetterMapConfig.class},
			(proxy, method, args) ->
			{
				if ("enabled".equals(method.getName()))
				{
					return true;
				}
				return false;
			}
		);

		final Menu menu = (Menu) Proxy.newProxyInstance(
			getClass().getClassLoader(),
			new Class<?>[]{Menu.class},
			(proxy, method, args) ->
			{
				final String name = method.getName();
				if ("getMenuEntries".equals(name))
				{
					final MenuEntry[] result = new MenuEntry[entries.size()];
					for (int i = 0; i < entries.size(); i++)
					{
						result[i] = createProxy(entries.get(i));
					}
					return result;
				}
				if ("setMenuEntries".equals(name))
				{
					entries.clear();
					for (MenuEntry menuEntry : (MenuEntry[]) args[0])
					{
						final SimpleEntry entry = new SimpleEntry();
						entry.option = menuEntry.getOption();
						entry.target = menuEntry.getTarget();
						entry.type = menuEntry.getType();
						entries.add(entry);
					}
					return null;
				}
				if ("createMenuEntry".equals(name))
				{
					final SimpleEntry entry = new SimpleEntry();
					entries.add(entry);
					return createProxy(entry);
				}
				return null;
			});

		final Client client = (Client) Proxy.newProxyInstance(
			getClass().getClassLoader(),
			new Class<?>[]{Client.class},
			(proxy, method, args) ->
			{
				final String name = method.getName();
				if ("getMouseCanvasPosition".equals(name))
				{
					return new net.runelite.api.Point(400, 300);
				}
				if ("getMenu".equals(name))
				{
					return menu;
				}
				if ("getMenuEntries".equals(name))
				{
					final MenuEntry[] result = new MenuEntry[entries.size()];
					for (int i = 0; i < entries.size(); i++)
					{
						result[i] = createProxy(entries.get(i));
					}
					return result;
				}
				if ("setMenuEntries".equals(name))
				{
					entries.clear();
					final MenuEntry[] newEntries = (MenuEntry[]) args[0];
					for (MenuEntry me : newEntries)
					{
						final SimpleEntry se = new SimpleEntry();
						se.option = me.getOption();
						se.target = me.getTarget();
						se.type = me.getType();
						entries.add(se);
					}
					return null;
				}
				if ("createMenuEntry".equals(name))
				{
					final SimpleEntry se = new SimpleEntry();
					entries.add(se);
					return createProxy(se);
				}
				return null;
			}
		);

		final SlayerPluginService slayerService = new SlayerPluginService()
		{
			@Override
			public String getTask()
			{
				return "Gargoyles";
			}

			@Override
			public int getRemainingAmount()
			{
				return 50;
			}

			@Override
			public int getInitialAmount()
			{
				return 100;
			}

			@Override
			public List<NPC> getTargets()
			{
				return Collections.emptyList();
			}

			@Override
			public String getTaskLocation()
			{
				return null;
			}
		};

		setField(plugin, "client", client);
		setField(plugin, "camera", camera);
		setField(plugin, "dungeonPieceIndex", new com.bettermap.map.DungeonPieceIndex());
		setField(plugin, "config", config);
		setField(plugin, "shortestPathTracker", new ShortestPathTracker(null, null, null, null, config));
		setField(plugin, "slayerTaskTracker", new SlayerTaskTracker(null, slayerService));
	}

	private static void setField(Object obj, String fieldName, Object val) throws Exception
	{
		final Field f = obj.getClass().getDeclaredField(fieldName);
		f.setAccessible(true);
		f.set(obj, val);
	}

	@Test
	public void contextMenuOnOverworldHasCancel()
	{
		assertFalse(camera.isViewingDungeonLayer());

		// Initial menu has Cancel at bottom
		final SimpleEntry cancel = new SimpleEntry();
		cancel.option = "Cancel";
		cancel.type = MenuAction.CANCEL;
		entries.add(cancel);

		plugin.onMenuOpened(new MenuOpened());

		assertEquals(2, entries.size());
		assertEquals("Cancel", entries.get(0).option);
		assertEquals("Route here", entries.get(1).option);
	}

	@Test
	public void contextMenuInDungeonHasSwitchToSurface()
	{
		camera.setUndergroundMode(UndergroundZone.LUMBRIDGE_SWAMP_CAVES);
		camera.beginFrame();
		assertTrue(camera.isViewingDungeonLayer());

		final SimpleEntry cancel = new SimpleEntry();
		cancel.option = "Cancel";
		cancel.type = MenuAction.CANCEL;
		entries.add(cancel);

		plugin.onMenuOpened(new MenuOpened());

		assertEquals(3, entries.size());
		assertEquals("Cancel", entries.get(0).option);
		assertEquals("Route here", entries.get(1).option);
		assertEquals("Return to surface", entries.get(2).option);
	}

	@Test
	public void contextMenuInRegionalOverlayHasSwitchToSurface()
	{
		camera.setActiveOverlayCluster(com.bettermap.data.OverlayCluster.all().get(0));
		final WorldPoint entrance = camera.getActiveOverlayCluster().members.get(0).getSurfacePoint();
		camera.centerOn(entrance.getX(), entrance.getY());
		camera.beginFrame();
		assertTrue(camera.isViewingDungeonLayer());

		final SimpleEntry cancel = new SimpleEntry();
		cancel.option = "Cancel";
		cancel.type = MenuAction.CANCEL;
		entries.add(cancel);

		plugin.onMenuOpened(new MenuOpened());

		assertEquals(3, entries.size());
		assertEquals("Route here", entries.get(1).option);
		assertEquals("Return to surface", entries.get(2).option);
	}

	@Test
	public void dungeonSymbolAddsEntityActionsAboveGlobalActions()
	{
		camera.setLayerSymbolTargets(Collections.singletonList(
			new MapCamera.LayerSymbolTarget(new Rectangle(390, 290, 20, 20),
				UndergroundZone.TAVERLEY_DUNGEON, true)));
		final SimpleEntry cancel = new SimpleEntry();
		cancel.option = "Cancel";
		cancel.type = MenuAction.CANCEL;
		entries.add(cancel);

		plugin.onMenuOpened(new MenuOpened());

		assertTrue(entries.stream().anyMatch(e -> "Enter".equals(e.option)));
		assertTrue(entries.stream().anyMatch(e -> "Cycle floor".equals(e.option)));
	}
}
