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
package atlasofgielinor.ui.markers;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import atlasofgielinor.AtlasOfGielinorConfig;
import atlasofgielinor.map.MapCamera;
import atlasofgielinor.map.WorldMapPointReader;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;

/** Draws map points registered by other plugins. */
public class PluginMarkerRenderer
{
	private final Set<Object> visibleTooltipTargets;
	private final Map<WorldMapPoint, Rectangle> visiblePointBounds = new LinkedHashMap<>();

	private final AtlasOfGielinorConfig config;
	private final MapCamera camera;
	private final WorldMapPointManager worldMapPointManager;
	private final WorldMapPointReader worldMapPointReader;

	public PluginMarkerRenderer(
		AtlasOfGielinorConfig config,
		MapCamera camera,
		WorldMapPointManager worldMapPointManager,
		WorldMapPointReader worldMapPointReader,
		Set<Object> visibleTooltipTargets)
	{
		this.config = config;
		this.camera = camera;
		this.worldMapPointManager = worldMapPointManager;
		this.worldMapPointReader = worldMapPointReader;
		this.visibleTooltipTargets = visibleTooltipTargets;
	}

	/** Markers other plugins (and ours) registered, drawn through our transform. */
	public void drawPluginMarkers(Graphics2D graphics, Rectangle bounds, List<Rectangle> placed)
	{
		visiblePointBounds.clear();
		if (!config.showPluginMarkers() || camera.getZoom() < config.pluginMarkerMinZoom())
		{
			return;
		}

		final int plane = camera.getPlane();
		final ViewWindow view = ViewWindow.from(camera, bounds, 10.0);

		for (WorldMapPoint point : worldMapPointReader.points(worldMapPointManager))
		{
			final BufferedImage image = point.getImage();
			final WorldPoint location = point.getWorldPoint();
			if (image == null || location == null || location.getPlane() != plane)
			{
				continue;
			}

			final int lx = location.getX();
			final int ly = location.getY();
			if (!camera.inView(lx, ly, view.minX, view.maxX, view.minY, view.maxY) || !ViewWindow.isDrawable(camera, lx, ly))
			{
				continue;
			}

			int x = (int) Math.round(camera.screenX(location.getX() + 0.5, location.getY() + 0.5, bounds));
			int y = (int) Math.round(camera.screenY(location.getX() + 0.5, location.getY() + 0.5, bounds));

			if (point.getImagePoint() == null)
			{
				x -= image.getWidth() / 2;
				y -= image.getHeight() / 2;
			}
			else
			{
				x -= point.getImagePoint().getX();
				y -= point.getImagePoint().getY();
			}

			final Rectangle rect = new Rectangle(x, y, image.getWidth(), image.getHeight());

			if (!bounds.intersects(rect))
			{
				continue;
			}

			if (ViewWindow.overlapsPlaced(placed, rect))
			{
				continue;
			}

			graphics.drawImage(image, rect.x, rect.y, null);
			placed.add(rect);
			visibleTooltipTargets.add(point);
			visiblePointBounds.put(point, rect);
		}
	}

	/** Match the original image bounds, including icons whose anchor is offset from their tile. */
	public WorldMapPoint visiblePointAt(java.awt.Point cursor)
	{
		for (Map.Entry<WorldMapPoint, Rectangle> entry : visiblePointBounds.entrySet())
		{
			if (visibleTooltipTargets.contains(entry.getKey()) && entry.getValue().contains(cursor))
			{
				return entry.getKey();
			}
		}
		return null;
	}

}
