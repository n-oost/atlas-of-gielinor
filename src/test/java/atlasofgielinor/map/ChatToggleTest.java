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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

import atlasofgielinor.map.MapCamera;

/**
 * The chat hide is transient by design: leaving a user's chatbox hidden after the map closed would
 * look like the plugin broke their client.
 */
public class ChatToggleTest
{
	@org.junit.BeforeClass
	public static void loadMapData()
	{
		atlasofgielinor.map.MapCatalogLoader.load();
		org.junit.Assert.assertTrue("Map catalogs must load before testing", atlasofgielinor.map.MapCatalogLoader.isReady());
	}

	private MapCamera camera;

	@Before
	public void setUp()
	{
		camera = new MapCamera();
		camera.setActive(true);
	}

	@Test
	public void chatStartsVisible()
	{
		assertFalse(camera.isChatHidden());
	}

	@Test
	public void togglingHidesAndShows()
	{
		camera.setChatHidden(true);
		assertTrue(camera.isChatHidden());

		camera.setChatHidden(false);
		assertFalse(camera.isChatHidden());
	}

	@Test
	public void closingTheMapAlwaysGivesTheChatBack()
	{
		camera.setChatHidden(true);
		camera.setActive(false);

		assertFalse(camera.isChatHidden());
	}

	@Test
	public void closingTheMapDropsTheButtonRectSoAStaleHitCannotFire()
	{
		camera.setChatButton(new java.awt.Rectangle(10, 10, 22, 22));
		camera.setActive(false);

		assertNull(camera.getChatButton());
	}
}
