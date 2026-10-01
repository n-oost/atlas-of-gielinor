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
package com.bettermap.ui;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.bettermap.map.InstanceMaps;
import com.bettermap.ui.markers.PoiMarkerRenderer;
import org.junit.Test;

public class PlaceNamesTest
{
	@Test
	public void zanarisRoomsStayOffTheOverworld()
	{
		assertTrue(PoiMarkerRenderer.isInteriorPlaceLabel(4429));
		assertFalse(PoiMarkerRenderer.shouldDrawPlaceName(
			"Throne room", 4429, 8.0, false, true));
		assertFalse(PoiMarkerRenderer.shouldDrawPlaceName(
			"Otherworldly beings", 4427, 8.0, false, true));
		assertFalse(PoiMarkerRenderer.shouldDrawPlaceName(
			"Queen's Chamber", 9504, 8.0, false, true));
		assertFalse(PoiMarkerRenderer.shouldDrawPlaceName(
			"Puro-puro", 4446, 8.0, false, true));
		assertFalse(PoiMarkerRenderer.shouldDrawPlaceName(
			"Tanglefeet", 4380, 8.0, false, true));
		assertFalse(PoiMarkerRenderer.shouldDrawPlaceName(
			"Beware of the mushrooms", 4375, 8.0, false, true));
		assertFalse(PoiMarkerRenderer.shouldDrawPlaceName(
			"Wire machine", 5270, 8.0, false, true));
		assertFalse(PoiMarkerRenderer.shouldDrawPlaceName(
			"Agility course", 5248, 8.0, false, true));
	}

	@Test
	public void dungeonRoomsAppearWhenTheInteriorIsFocused()
	{
		assertTrue(PoiMarkerRenderer.shouldDrawPlaceName(
			"Throne room", 4429, 8.0, true, true));
		assertTrue(PoiMarkerRenderer.shouldDrawPlaceName(
			"Queen's Chamber", 9504, 8.0, true, false));
	}

	@Test
	public void dungeonRoomsNeedCloserZoomEvenInside()
	{
		assertFalse(PoiMarkerRenderer.shouldDrawPlaceName(
			"Throne room", 4429, 3.0, true, true));
	}

	@Test
	public void citiesStayZoomedOutWhileDistrictsWait()
	{
		assertTrue(PoiMarkerRenderer.shouldDrawPlaceName(
			"Lumbridge", 3234, 0.5, false, true));
		assertFalse(PoiMarkerRenderer.shouldDrawPlaceName(
			"Lumbridge Swamp", 3168, 0.5, false, true));
		assertTrue(PoiMarkerRenderer.shouldDrawPlaceName(
			"Lumbridge Swamp", 3168, 3.5, false, true));
	}

	@Test
	public void islandsStayVisibleAtTheMajorPlaceZoom()
	{
		assertTrue(PoiMarkerRenderer.shouldDrawPlaceName(
			"Waterbirth Island", 3756, 0.5, false, true));
		assertTrue(PoiMarkerRenderer.shouldDrawPlaceName(
			"Dognose Island", 2648, 0.5, false, true));
		assertTrue(PoiMarkerRenderer.shouldDrawPlaceName(
			"Isle of Bones", 2533, 0.5, false, true));
		assertTrue(PoiMarkerRenderer.shouldDrawPlaceName(
			"Shimmering Atoll", 2786, 0.5, false, true));
		assertTrue(PoiMarkerRenderer.shouldDrawPlaceName(
			"Cursed Archipelago", 2581, 0.5, false, true));
		assertFalse(PoiMarkerRenderer.shouldDrawPlaceName(
			"Waterbirth Island", 3756, 0.4, false, true));
	}

	@Test
	public void surfaceEntranceNamesAreNotInterior()
	{
		assertFalse(PoiMarkerRenderer.isInteriorPlaceLabel(3099));
		assertTrue(3099 < InstanceMaps.GAP_MIN_Y);
	}

	@Test
	public void skipListStillHidesNoise()
	{
		assertFalse(PoiMarkerRenderer.shouldDrawPlaceName(
			"exit", 3234, 8.0, false, true));
		assertFalse(PoiMarkerRenderer.shouldDrawPlaceName(
			"A", 3234, 8.0, false, true));
	}
}
