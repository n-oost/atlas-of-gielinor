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
package com.bettermap.map;

import java.awt.Rectangle;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/** The standalone quick-find card must outlive the per-frame {@code setActive(false)} while the map is closed. */
public class MapCameraStandaloneTest
{
	@Test
	public void setActiveFalseKeepsTheStandaloneCardUp()
	{
		final MapCamera camera = new MapCamera();
		camera.setFinderStandalone(true);
		camera.setFinderPanelOpen(true);

		// BetterWorldMapOverlay.render() calls this every frame the world map is closed.
		camera.setActive(false);
		camera.setActive(false);

		assertTrue("panel must stay open", camera.isFinderPanelOpen());
		assertTrue("standalone flag must stay set", camera.isFinderStandalone());
		assertTrue("field keeps the keyboard", camera.isFinderFieldFocused());
	}

	@Test
	public void openingTheWorldMapDismissesTheStandaloneCard()
	{
		final MapCamera camera = new MapCamera();
		camera.setFinderStandalone(true);
		camera.setFinderPanelOpen(true);

		camera.setViewport(new Rectangle(0, 0, 800, 600));
		camera.setActive(true);

		assertFalse(camera.isFinderStandalone());
		assertFalse(camera.isFinderPanelOpen());
	}

	@Test
	public void setActiveTrueWhileAlreadyActiveKeepsInMapFinderOpen()
	{
		final MapCamera camera = new MapCamera();
		camera.setViewport(new Rectangle(0, 0, 800, 600));
		camera.setActive(true);
		camera.setFinderPanelOpen(true);

		// BetterWorldMapOverlay.render() reasserts active every frame the world map is open.
		camera.setActive(true);
		camera.setActive(true);

		assertTrue("in-map Find panel must survive per-frame setActive(true)", camera.isFinderPanelOpen());
		assertTrue(camera.isFinderFieldFocused());
		assertFalse(camera.isFinderStandalone());
	}

	@Test
	public void quickFinderOrbRectRoundTrips()
	{
		final MapCamera camera = new MapCamera();
		final Rectangle orb = new Rectangle(700, 120, 26, 26);
		camera.setQuickFinderOrb(orb);
		assertTrue(orb.equals(camera.getQuickFinderOrb()));
	}
}
