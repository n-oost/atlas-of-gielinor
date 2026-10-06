package com.bettermap.map;

import com.google.gson.Gson;

import bettermap.BetterMapPlugin;
import bettermap.map.ChatboxInputHost;
import bettermap.map.FinderKeyCapture;
import bettermap.map.MapCamera;
import bettermap.map.MapFinder;
import bettermap.map.MonsterIndex;
import bettermap.map.PoiIndex;

import java.awt.Rectangle;
import java.util.function.Consumer;
import net.runelite.api.coords.WorldPoint;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class FinderKeyCaptureTest
{
	@org.junit.BeforeClass
	public static void loadMapData()
	{
		bettermap.map.MapData.load();
		org.junit.Assert.assertTrue("Map catalogs must load before testing", bettermap.map.MapData.isReady());
	}

	private static class FakeHost implements ChatboxInputHost
	{
		String prompt;
		String value;
		Consumer<String> onChanged;
		Consumer<String> onDone;
		Runnable onClose;
		int openCalls;
		int closeCalls;

		@Override
		public void open(String prompt, String initialValue,
			Consumer<String> onChanged, Consumer<String> onDone, Runnable onClose)
		{
			openCalls++;
			this.prompt = prompt;
			this.value = initialValue;
			this.onChanged = onChanged;
			this.onDone = onDone;
			this.onClose = onClose;
		}

		@Override
		public void close()
		{
			closeCalls++;
		}
	}

	private MapCamera camera;
	private MapFinder finder;
	private FakeHost host;
	private FinderKeyCapture capture;

	private WorldPoint centeredOn;

	@Before
	public void setUp()
	{
		camera = new MapCamera();
		final MonsterIndex monsterIndex = new MonsterIndex(new Gson());
		monsterIndex.load();
		finder = new MapFinder(new PoiIndex(), monsterIndex);
		host = new FakeHost();
		centeredOn = null;

		final BetterMapPlugin plugin = new BetterMapPlugin()
		{


			@Override
			public void centerMapOn(WorldPoint point)
			{
				centeredOn = point;
			}
		};


		capture = new FinderKeyCapture(camera, finder, () -> plugin, host);
	}

	private void openAndFocus()
	{
		camera.setActive(true);
		camera.setFinderPanelOpen(true);   // also focuses the field
	}

	@Test
	public void syncOpensTheInputOnceWhenTheCardIsInteractiveAndFocused()
	{
		openAndFocus();

		capture.sync();
		assertTrue(capture.isOpen());
		assertEquals(1, host.openCalls);
		assertEquals("Map Finder", host.prompt);

		capture.sync();
		assertEquals("already open, no second input", 1, host.openCalls);
	}

	@Test
	public void syncDoesNotOpenWhenInactiveOrUnfocused()
	{
		camera.setFinderPanelOpen(true);   // focused, but map not active and not standalone
		capture.sync();
		assertFalse(capture.isOpen());
		assertEquals(0, host.openCalls);

		camera.setActive(true);
		camera.setFinderFieldFocused(false);
		capture.sync();
		assertFalse(capture.isOpen());
		assertEquals(0, host.openCalls);
	}

	@Test
	public void droppingFocusClosesTheInputButLeavesTheCardUp()
	{
		openAndFocus();
		capture.sync();

		camera.setFinderFieldFocused(false);
		capture.sync();
		assertEquals(1, host.closeCalls);

		// The real host answers close() by firing onClose; do that here.
		host.onClose.run();
		assertFalse(capture.isOpen());
		assertTrue("focus drop must not put the card away", camera.isFinderPanelOpen());
	}

	@Test
	public void userDismissingTheInputAlsoClosesTheCard()
	{
		openAndFocus();
		capture.sync();

		// Esc / Enter inside the chatbox closes it straight to onClose, with no closeInput() first.
		host.onClose.run();
		assertFalse(capture.isOpen());
		assertFalse("Esc in the search field closes the card too", camera.isFinderPanelOpen());
	}

	@Test
	public void openSeedsTheCurrentQuery()
	{
		finder.updateQuery("varrock", null);
		openAndFocus();
		capture.sync();

		assertEquals("varrock", host.value);
	}

	@Test
	public void onChangedFeedsTheFinderQuery()
	{
		openAndFocus();
		capture.sync();

		host.onChanged.accept("lumbridge");
		assertEquals("lumbridge", finder.getQuery());
	}

	@Test
	public void enterJumpsToTheTopResult()
	{
		openAndFocus();
		capture.sync();

		host.onChanged.accept("King Black Dragon");
		assertFalse(finder.getResults().isEmpty());
		final WorldPoint top = finder.getResults().get(0).getPoint();

		host.onDone.accept(finder.getQuery());
		assertSame(top, centeredOn);
	}

}