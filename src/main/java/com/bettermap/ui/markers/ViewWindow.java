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
package com.bettermap.ui.markers;

import com.bettermap.data.UndergroundZone;
import com.bettermap.map.InstanceMaps;
import com.bettermap.map.MapCamera;
import java.awt.Rectangle;
import java.util.List;

/**
 * Shared bounds, visibility, and collision detection utilities for marker renderers.
 */
public final class ViewWindow
{
	public final double minX;
	public final double maxX;
	public final double minY;
	public final double maxY;
	public final int minXi;
	public final int maxXi;
	public final int minYi;
	public final int maxYi;

	public ViewWindow(double minX, double maxX, double minY, double maxY)
	{
		this.minX = minX;
		this.maxX = maxX;
		this.minY = minY;
		this.maxY = maxY;
		this.minXi = (int) Math.floor(minX);
		this.maxXi = (int) Math.ceil(maxX);
		this.minYi = (int) Math.floor(minY);
		this.maxYi = (int) Math.ceil(maxY);
	}

	/**
	 * Converts a screen viewport rectangle to a world tile window with the given tile margin.
	 */
	public static ViewWindow from(MapCamera camera, Rectangle bounds, double margin)
	{
		return new ViewWindow(
			camera.worldX(bounds.getMinX(), bounds) - margin,
			camera.worldX(bounds.getMaxX(), bounds) + margin,
			camera.worldY(bounds.getMaxY(), bounds) - margin,
			camera.worldY(bounds.getMinY(), bounds) + margin);
	}

	/**
	 * Checks whether world coordinates should be drawn based on surface/layer visibility.
	 */
	public static boolean isDrawable(MapCamera camera, int worldX, int worldY)
	{
		if (InstanceMaps.hiddenOnSurface(worldX, worldY, camera.getCenterX(), camera.getCenterY()))
		{
			return false;
		}
		return InstanceMaps.inFocusedLayer(worldX, worldY,
			camera.getFocusedUndergroundZone(), camera.isDungeonContentsFocused());
	}

	/** Interior POIs appear only with their dungeon terrain, never on the plain surface map. */
	public static boolean isPoiDrawable(MapCamera camera, int worldX, int worldY)
	{
		if (!isDrawable(camera, worldX, worldY))
		{
			return false;
		}
		if (worldY <= InstanceMaps.GAP_MIN_Y || !InstanceMaps.cameraOnOverworld(camera.getCenterY())
			|| InstanceMaps.isOverworldOverlay(worldX, worldY))
		{
			return true;
		}
		for (UndergroundZone zone : camera.previewUndergroundZones())
		{
			if (InstanceMaps.belongsToZone(worldX, worldY, zone))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks whether a candidate marker rectangle overlaps any previously placed rectangles.
	 */
	public static boolean overlapsPlaced(List<Rectangle> placed, Rectangle candidate)
	{
		for (Rectangle other : placed)
		{
			if (other.intersects(candidate))
			{
				return true;
			}
		}
		return false;
	}
}
