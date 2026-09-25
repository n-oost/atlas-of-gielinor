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

import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class PoiCoverageTest
{
	@Test
	public void allCuratedPoisResolveToNonEmptyNonGenericDetailCards()
	{
		final List<PoiIndex.Poi> pois = PoiDetails.getAllPois();
		final List<String> failures = new ArrayList<>();

		for (PoiIndex.Poi p : pois)
		{
			final PoiDetails.Detail detail = PoiDetails.getDetail(p, p.getX(), p.getY(), p.getPlane());
			if (detail == null)
			{
				failures.add(p.getName() + " @ (" + p.getX() + "," + p.getY() + "," + p.getPlane() + ") key=" + p.getKey() + " [detail was null]");
				continue;
			}

			if (detail.getLines().isEmpty())
			{
				failures.add(p.getName() + " @ (" + p.getX() + "," + p.getY() + "," + p.getPlane() + ") key=" + p.getKey() + " [lines were empty]");
			}
			else if ("Point of Interest".equals(detail.getCategory()))
			{
				failures.add(p.getName() + " @ (" + p.getX() + "," + p.getY() + "," + p.getPlane() + ") key=" + p.getKey() + " [category was 'Point of Interest']");
			}

			for (String line : detail.getLines())
			{
				if (line.length() >= 80)
				{
					failures.add(p.getName() + " @ (" + p.getX() + "," + p.getY() + "," + p.getPlane() + ") key=" + p.getKey() + " [line too long (" + line.length() + " chars): " + line + "]");
				}
			}
		}

		assertTrue("Curated POIs with missing or invalid details (" + failures.size() + " gaps):\n" + String.join("\n", failures), failures.isEmpty());
	}

	@Test
	public void sailingRegionHunterCoverageMeetsMinimumThreshold()
	{
		final List<PoiIndex.Poi> pois = PoiDetails.getAllPois();
		final long sailingHunterCount = pois.stream()
			.filter(p -> "hunter_training".equals(p.getKey()) && p.getX() < 3320 && p.getY() < 2650 && p.getPlane() == 0)
			.count();

		assertTrue("Sailing-region hunter POIs count was " + sailingHunterCount + " (expected >= 6)", sailingHunterCount >= 6);
	}
}
