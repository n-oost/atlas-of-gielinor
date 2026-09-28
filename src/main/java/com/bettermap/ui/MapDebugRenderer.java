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

import static com.bettermap.ui.MapStyle.CARD_EDGE;
import static com.bettermap.ui.MapStyle.CARD_TITLE;
import static com.bettermap.ui.MapStyle.DEBUG_KEY;

import com.bettermap.BetterMapConfig;
import com.bettermap.data.sailing.BoatTracker;
import com.bettermap.data.sailing.PortNoticeBoard;
import com.bettermap.data.sailing.SailingPort;
import com.bettermap.map.GroundItemIndex;
import com.bettermap.map.MapCamera;
import com.bettermap.map.MapFinder;
import com.bettermap.map.PoiIndex;
import com.bettermap.map.ShopIndex;
import com.bettermap.map.WorldMapInput;
import com.bettermap.map.WorldMapPointReader;
import com.bettermap.tiles.TileLoader;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.client.ui.FontManager;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.worldmap.WorldMapPointManager;

/**
 * The {@code debugOverlay} panel and the one-line-a-second pipeline log.
 *
 * <p>Split out of {@link BetterWorldMapOverlay}; it reads {@link MapRenderStats}, which the
 * tile and marker renderers fill in as the frame is drawn.
 */
@Slf4j
class MapDebugRenderer
{
	private static final Color DEBUG_BG = new Color(8, 10, 14, 225);
	private static final Color DEBUG_VALUE = new Color(225, 225, 225);
	private static final Font MONO = FontManager.getDefaultFont().deriveFont(11f);

	private final Client client;
	private final BetterMapConfig config;
	private final MapCamera camera;
	private final TileLoader tileLoader;
	private final WorldMapInput input;
	private final PoiIndex poiIndex;
	private final ShopIndex shopIndex;
	private final GroundItemIndex groundItemIndex;
	private final BoatTracker boatTracker;
	private final WorldMapPointManager worldMapPointManager;
	private final WorldMapPointReader worldMapPointReader;
	private final MapFinder finder;
	private final MapRenderStats stats;

	private long lastLogAt;

	MapDebugRenderer(
		Client client,
		BetterMapConfig config,
		MapCamera camera,
		TileLoader tileLoader,
		WorldMapInput input,
		PoiIndex poiIndex,
		ShopIndex shopIndex,
		GroundItemIndex groundItemIndex,
		BoatTracker boatTracker,
		WorldMapPointManager worldMapPointManager,
		WorldMapPointReader worldMapPointReader,
		MapFinder finder,
		MapRenderStats stats)
	{
		this.client = client;
		this.config = config;
		this.camera = camera;
		this.tileLoader = tileLoader;
		this.input = input;
		this.poiIndex = poiIndex;
		this.shopIndex = shopIndex;
		this.groundItemIndex = groundItemIndex;
		this.boatTracker = boatTracker;
		this.worldMapPointManager = worldMapPointManager;
		this.worldMapPointReader = worldMapPointReader;
		this.finder = finder;
		this.stats = stats;
	}

	/** Live state of every stage, so a glance says whether the pipeline is working. */
	void drawDebugPanel(Graphics2D graphics, Rectangle bounds, Point unmappedPosition)
	{
		final java.awt.Point cursor = input.getCursor();

		final String[][] rows = {
			{"layer", "Unified Cache Map  floor=" + camera.getPlane() + "  levels=[" + tileLoader.describeLevels() + "]"},
			{"camera", String.format("%.0f, %.0f   zoom=%.3f px/tile   tile z=%d",
				camera.getCenterX(), camera.getCenterY(), camera.getZoom(), stats.tileZoomInUse)},
			{"view", bounds.width + "x" + bounds.height + " at " + bounds.x + "," + bounds.y
				+ (config.fullscreenMap() ? "  fullscreen" : "  windowed")},
			{"tiles", "drawn=" + stats.tilesDrawn + "  absent=" + stats.tilesMissing
				+ "  pending=" + tileLoader.getPendingCount()
				+ "  mem=" + tileLoader.getMemoryCacheSize()
				+ "  hits=" + tileLoader.getCacheHits()
				+ "  reads=" + tileLoader.getDiskReads()
				+ "  misses=" + tileLoader.getMissCount()},
			{"icons", "drawn=" + stats.iconsDrawn + "  decluttered=" + stats.iconsSkipped
				+ "  filtered=" + stats.iconsFiltered
				+ "  indexed=" + poiIndex.size() + (poiIndex.isLoaded() ? " (loaded)" : " (NOT LOADED)")},
			{"ground items", "indexed=" + groundItemIndex.size() + (groundItemIndex.isLoaded() ? " (loaded)" : " (NOT LOADED)")},
			{"shops", "indexed=" + shopIndex.size() + (shopIndex.isLoaded() ? " (loaded)" : " (NOT LOADED)")},
			{"boats", (boatTracker == null ? "none" : "owned=" + boatTracker.getOwnedBoats().size() + "  tracked=" + boatTracker.getBoats().size()) + "  ports=" + SailingPort.values().length + "  boards=" + PortNoticeBoard.ALL_BOARDS.size()},
			{"markers", "bosses=" + stats.markersDrawn + "  plugin points=" + countWorldMapPoints()},
			{"widgets", "MAP_DISPLAY hidden=" + widgetHidden(InterfaceID.Worldmap.MAP_DISPLAY)
				+ "  MAP_OVERLAY hidden=" + widgetHidden(InterfaceID.Worldmap.MAP_OVERLAY)},
			{"area", unmappedPosition == null ? "mapped"
				: "UNMAPPED at " + unmappedPosition.getX() + "," + unmappedPosition.getY()},
			{"cursor", cursor == null ? "off map" : cursor.x + "," + cursor.y + " -> world "
				+ (int) Math.floor(camera.worldX(cursor.x, bounds)) + ", "
				+ (int) Math.floor(camera.worldY(cursor.y, bounds))},
			{"finder", "open=" + camera.isFinderPanelOpen() + " q='" + finder.getQuery() + "' results=" + finder.getResults().size()},
		};

		graphics.setFont(MONO);
		final int lineHeight = graphics.getFontMetrics().getHeight();
		final int keyWidth = graphics.getFontMetrics().stringWidth("markers ") + 4;

		int width = 0;
		for (String[] row : rows)
		{
			width = Math.max(width, keyWidth + graphics.getFontMetrics().stringWidth(row[1]));
		}

		final int padding = 7;
		final int boxWidth = width + padding * 2;
		final int boxHeight = (rows.length + 1) * lineHeight + padding * 2;
		final int x = (int) bounds.getMinX() + 8;
		final int y = (int) bounds.getMaxY() - boxHeight - 24;

		graphics.setColor(DEBUG_BG);
		graphics.fillRoundRect(x, y, boxWidth, boxHeight, 6, 6);
		graphics.setColor(CARD_EDGE);
		graphics.drawRoundRect(x, y, boxWidth, boxHeight, 6, 6);

		int textY = y + padding + graphics.getFontMetrics().getAscent();
		graphics.setColor(CARD_TITLE);
		graphics.drawString("BetterMap debug", x + padding, textY);
		textY += lineHeight;

		for (String[] row : rows)
		{
			graphics.setColor(DEBUG_KEY);
			graphics.drawString(row[0], x + padding, textY);
			graphics.setColor(DEBUG_VALUE);
			graphics.drawString(row[1], x + padding + keyWidth, textY);
			textY += lineHeight;
		}
	}

	private String widgetHidden(int id)
	{
		final Widget widget = client.getWidget(id);
		return widget == null ? "absent" : String.valueOf(widget.isSelfHidden());
	}

	private int countWorldMapPoints()
	{
		return worldMapPointReader.points(worldMapPointManager).size();
	}

	/** One line a second while the map is open, rather than a line a frame. */
	void logPipeline(Rectangle bounds)
	{
		if (!config.debugLogging())
		{
			return;
		}

		final long now = System.currentTimeMillis();
		if (now - lastLogAt < 1000)
		{
			return;
		}
		lastLogAt = now;

		log.debug("[BetterMap] floor={} levels=[{}] | camera={},{} zoom={} tilez={} | "
				+ "view={}x{} {} | tiles drawn={} absent={} pending={} mem={} hits={} reads={} misses={} | "
				+ "icons drawn={} decluttered={} filtered={} indexed={} | bosses={} points={} | "
				+ "MAP_DISPLAY hidden={} MAP_OVERLAY hidden={}",
			camera.getPlane(), tileLoader.describeLevels(),
			(int) camera.getCenterX(), (int) camera.getCenterY(),
			String.format("%.3f", camera.getZoom()), stats.tileZoomInUse,
			bounds.width, bounds.height, config.fullscreenMap() ? "fullscreen" : "windowed",
			stats.tilesDrawn, stats.tilesMissing, tileLoader.getPendingCount(), tileLoader.getMemoryCacheSize(),
			tileLoader.getCacheHits(), tileLoader.getDiskReads(), tileLoader.getMissCount(),
			stats.iconsDrawn, stats.iconsSkipped, stats.iconsFiltered, poiIndex.size(),
			stats.markersDrawn, countWorldMapPoints(),
			widgetHidden(InterfaceID.Worldmap.MAP_DISPLAY),
			widgetHidden(InterfaceID.Worldmap.MAP_OVERLAY));
	}
}
