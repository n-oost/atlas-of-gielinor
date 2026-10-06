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

import java.awt.Rectangle;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import atlasofgielinor.map.MapCamera;

/** In-map Finder stays open across render frames. */
public class MapCameraFinderTest
{
	@org.junit.BeforeClass
	public static void loadMapData()
	{
		atlasofgielinor.map.MapCatalogLoader.load();
		org.junit.Assert.assertTrue("Map catalogs must load before testing", atlasofgielinor.map.MapCatalogLoader.isReady());
	}

	@Test
	public void setActiveTrueWhileAlreadyActiveKeepsInMapFinderOpen()
	{
		final MapCamera camera = new MapCamera();
		camera.setViewport(new Rectangle(0, 0, 800, 600));
		camera.setActive(true);
		camera.setFinderPanelOpen(true);

		// AtlasOfGielinorOverlay.render() reasserts active every frame the world map is open.
		camera.setActive(true);
		camera.setActive(true);

		assertTrue("in-map Find panel must survive per-frame setActive(true)", camera.isFinderPanelOpen());
		assertTrue(camera.isFinderFieldFocused());
	}

}
