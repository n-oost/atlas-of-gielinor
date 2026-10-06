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
package bettermap.ui;

import static bettermap.ui.MapStyle.CARD_BG;
import static bettermap.ui.MapStyle.CARD_EDGE;
import static bettermap.ui.MapStyle.CARD_TEXT;
import static bettermap.ui.MapStyle.CARD_TITLE;
import static bettermap.ui.MapStyle.CHIP_BG;
import static bettermap.ui.MapStyle.SMALL;
import static bettermap.ui.MapStyle.TEXT_DIM;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import bettermap.BetterMapConfig;
import bettermap.map.MapCamera;
import bettermap.map.MapFinder;
import bettermap.map.MapRegion;
import bettermap.map.SlayerTaskTracker;
import bettermap.map.WorldMapInput;
import net.runelite.api.coords.WorldPoint;

/**
 * The search field, its result rows and the flyout submenus.
 *
 * <p>Split out of {@link BetterWorldMapOverlay}. Matching lives in {@link MapFinder}; this
 * draws the panel and publishes every row's screen rect to {@link MapCamera} so
 * {@link bettermap.map.WorldMapInput} can hit-test clicks against it.
 */
class MapFinderRenderer
{
	private final BetterMapConfig config;
	private final MapCamera camera;
	private final WorldMapInput input;
	private final SlayerTaskTracker slayerTaskTracker;
	private final MapFinder finder;

	MapFinderRenderer(
		BetterMapConfig config,
		MapCamera camera,
		WorldMapInput input,
		SlayerTaskTracker slayerTaskTracker,
		MapFinder finder)
	{
		this.config = config;
		this.camera = camera;
		this.input = input;
		this.slayerTaskTracker = slayerTaskTracker;
		this.finder = finder;
	}

	/** Readable category name for every result row and detail strip. */
	private static String itemKindLabel(MapFinder.Result res)
	{
		if (res == null)
		{
			return null;
		}
		switch (res.getKind())
		{
			case PLACE:
				return "Place";
			case MONSTER:
				return "Monster";
			case SHOP:
				return "Shop";
			case GROUND_ITEM:
				return "Ground item";
			case MINERAL:
				return "Mining";
			default:
				return null;
		}
	}

	/**
	 * @param collapsedAsField fullscreen only — when the panel is closed, draw a permanently
	 *                         visible search bar in place of the magnifier button. Clicking it
	 *                         opens the panel through the same {@code getFinderButton()} path.
	 */
	void drawFinder(Graphics2D graphics, Rectangle bounds, Rectangle anchor, boolean drawButton,
		boolean collapsedAsField)
	{
		graphics.setFont(SMALL);
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		if (!config.showFinderButton())
		{
			if (drawButton || collapsedAsField)
			{
				camera.setFinderButton(null);
			}
			camera.setFinderPanelOpen(false);
			camera.clearFinderRegions();
			return;
		}

		final Rectangle button = anchor;
		final boolean open = camera.isFinderPanelOpen();

		if (collapsedAsField && !open)
		{
			final Rectangle bar = new Rectangle(button.x, button.y, 300, button.height);
			final java.awt.Point cur = input.getCursor();
			final boolean hover = cur != null && bar.contains(cur);
			graphics.setColor(CHIP_BG);
			graphics.fillRoundRect(bar.x, bar.y, bar.width, bar.height, 6, 6);
			graphics.setColor(hover ? CARD_TITLE : CARD_EDGE);
			graphics.drawRoundRect(bar.x, bar.y, bar.width, bar.height, 6, 6);
			graphics.setColor(TEXT_DIM);
			graphics.drawOval(bar.x + 6, bar.y + 5, 9, 9);
			graphics.drawLine(bar.x + 14, bar.y + 13, bar.x + 18, bar.y + 17);
			graphics.drawString("Find location", bar.x + 24, bar.y + 14);
			camera.setFinderButton(bar);
			camera.clearFinderRegions();
			return;
		}

		if (drawButton)
		{
			graphics.setColor(open ? CARD_EDGE : CHIP_BG);
			graphics.fillRoundRect(button.x, button.y, button.width, button.height, 6, 6);
			graphics.setColor(open ? CARD_TITLE : TEXT_DIM);
			graphics.drawRoundRect(button.x, button.y, button.width, button.height, 6, 6);
			graphics.setColor(open ? Color.BLACK : TEXT_DIM);
			graphics.drawOval(button.x + 5, button.y + 5, 9, 9);
			graphics.drawLine(button.x + 13, button.y + 13, button.x + 17, button.y + 17);
			camera.setFinderButton(button);
		}
		else if (collapsedAsField)
		{
			// Panel is open — let Esc / the panel's own controls close it, not a stale bar rect.
			camera.setFinderButton(null);
		}

		if (!open)
		{
			camera.clearFinderRegions();
			return;
		}

		input.tickFinderFlyoutDwell();

		final int padX = 10;
		final int padTop = 6;
		final int titleH = 18;
		final int fieldH = 20;
		final int chipRowH = 18;
		final int descStripH = 26;
		final int panelW = 300;

		final WorldPoint playerLoc = camera.getPlayerLocation();
		final String query = finder.getQuery();
		final boolean isEmptyQuery = query == null || query.trim().isEmpty();
		final int rowH = isEmptyQuery ? 20 : 30;
		final boolean browseLoading = isEmptyQuery && finder.isBrowseLoading();
		finder.refresh(playerLoc);

		final List<MapRegion> allRegions = isEmptyQuery && !browseLoading
			? finder.getRegions(playerLoc) : Collections.emptyList();
		final List<MapFinder.Result> rows = !isEmptyQuery ? finder.getResults() : Collections.emptyList();
		final int matchCount = finder.getMatchCount();
		final int locationMatchCount = finder.getLocationMatchCount();
		final int bodyItemCount = isEmptyQuery
			? (browseLoading ? 1 : allRegions.size())
			: (rows.isEmpty() ? 1 : rows.size());

		final String scrollKey = isEmptyQuery ? "" : query;
		if (!scrollKey.equals(camera.getFinderScrollKey()))
		{
			camera.setFinderBodyScrollOffset(0);
			camera.setFinderScrollKey(scrollKey);
		}
		camera.setFinderBodyItemCount(bodyItemCount);
		camera.clampFinderBodyScroll();

		final int bodyViewportRows = camera.finderBodyViewportRows();
		final int displayBodyRows = Math.min(bodyViewportRows, Math.max(1, bodyItemCount));
		final boolean hasOverflow = !isEmptyQuery && matchCount > rows.size();
		final boolean showExpand = bodyItemCount > MapCamera.FINDER_BODY_ROWS;
		final int expandBarH = showExpand ? 16 : 0;

		final int panelH = padTop + titleH + fieldH + 4 + chipRowH + 6 + expandBarH
			+ displayBodyRows * rowH
			+ (hasOverflow ? rowH : 0)
			+ 6 + descStripH + 4
			+ 6;

		int panelX = button.x;
		int panelY = button.y + button.height + 4;
		if (panelX + panelW > bounds.getMaxX() - 4)
		{
			panelX = (int) bounds.getMaxX() - 4 - panelW;
		}
		if (panelY + panelH > bounds.getMaxY() - 4)
		{
			panelY = Math.max((int) bounds.getMinY() + 4, (int) bounds.getMaxY() - 4 - panelH);
		}

		panelX = Math.max((int) bounds.getMinX() + 4, panelX);
		final Rectangle panel = new Rectangle(panelX, panelY, panelW, panelH);
		MapStyle.drawCard(graphics, panel);

		graphics.setColor(CARD_TITLE);
		graphics.drawString("Find location  (Esc)", panel.x + padX, panel.y + padTop + 12);

		final Rectangle field = new Rectangle(panel.x + padX, panel.y + padTop + titleH, panelW - 2 * padX, fieldH);
		final FontMetrics fm = graphics.getFontMetrics();
		drawSearchField(graphics, field, fm, query, isEmptyQuery);

		final java.awt.Point cursor = input.getCursor();
		final int chipRowY = field.y + fieldH + 4;
		drawChipRow(graphics, panel, fm, cursor, chipRowY, padX, panelW, chipRowH);
		// Expand / collapse when the full list is taller than the default viewport
		int rowY = chipRowY + chipRowH + 6;
		if (showExpand)
		{
			final String expandLabel = camera.isFinderBodyExpanded() ? "▴ Collapse" : "▾ Expand";
			final int labelW = fm.stringWidth(expandLabel);
			final Rectangle expandBtn = new Rectangle(panel.x + panelW - padX - labelW - 8, rowY, labelW + 8, 14);
			final boolean expandHover = cursor != null && expandBtn.contains(cursor);
			graphics.setColor(expandHover ? CHIP_BG : CARD_BG);
			graphics.fillRoundRect(expandBtn.x, expandBtn.y, expandBtn.width, expandBtn.height, 4, 4);
			graphics.setColor(expandHover ? CARD_TITLE : CARD_EDGE);
			graphics.drawRoundRect(expandBtn.x, expandBtn.y, expandBtn.width, expandBtn.height, 4, 4);
			graphics.setColor(CARD_TEXT);
			graphics.drawString(expandLabel, expandBtn.x + 4, expandBtn.y + 11);
			camera.setFinderExpandButton(expandBtn);
			rowY += expandBarH;
		}
		else
		{
			camera.setFinderExpandButton(null);
		}

		final int bodyScroll = camera.getFinderBodyScrollOffset();
		final int bodyEnd = Math.min(bodyItemCount, bodyScroll + displayBodyRows);
		final Rectangle bodyViewport = new Rectangle(panel.x + 3, rowY, panelW - 6, displayBodyRows * rowH);
		camera.setFinderBodyViewport(bodyViewport);

		// Body rows
		final List<MapCamera.FinderResultTarget> targets = new ArrayList<>();
		MapCamera.FinderResultTarget hoveredTarget = null;
		Rectangle flyoutAnchorRow = null;

		if (bodyScroll > 0)
		{
			graphics.setColor(TEXT_DIM);
			graphics.drawString("▲", bodyViewport.x + bodyViewport.width - 12, bodyViewport.y + 10);
		}
		if (bodyEnd < bodyItemCount)
		{
			graphics.setColor(TEXT_DIM);
			graphics.drawString("▼", bodyViewport.x + bodyViewport.width - 12, bodyViewport.y + bodyViewport.height - 4);
		}

		if (browseLoading)
		{
			graphics.setColor(TEXT_DIM);
			graphics.drawString("Loading map data…", panel.x + padX, rowY + 14);
			rowY += rowH;
		}
		else if (isEmptyQuery)
		{
			for (int i = bodyScroll; i < bodyEnd; i++)
			{
				final MapRegion region = allRegions.get(i);
				final Rectangle row = new Rectangle(panel.x + 3, rowY, panelW - 6, rowH);
				final boolean isHovered = cursor != null && row.contains(cursor);

				if (isHovered)
				{
					graphics.setColor(CHIP_BG);
					graphics.fillRoundRect(row.x, row.y, row.width, row.height, 4, 4);
				}

				if (region.name().equals(camera.getHoveredRowKey()))
				{
					flyoutAnchorRow = row;
				}

				final WorldPoint regCenter = new WorldPoint(region.getCenterX(), region.getCenterY(), 0);
				final int dist = distanceTiles(regCenter, playerLoc);
				final String arrow = "▸";
				graphics.setColor(CARD_TITLE);
				graphics.drawString(arrow, row.x + row.width - 60, rowY + rowH - 6);

				int dW = 0;
				if (dist >= 0)
				{
						final String distText = distanceLabel(dist);
					dW = fm.stringWidth(distText);
					graphics.setColor(TEXT_DIM);
					graphics.drawString(distText, row.x + row.width - 68 - dW, rowY + rowH - 6);
				}

				final int maxNameW = row.x + row.width - 68 - (dist >= 0 ? dW + 6 : 0) - (panel.x + padX);
				String name = region.getDisplayName();
				if (fm.stringWidth(name) > maxNameW && maxNameW > 0)
				{
					name = ellipsize(fm, name, maxNameW, true);
				}
				graphics.setColor(CARD_TEXT);
				graphics.drawString(name, panel.x + padX, rowY + rowH - 6);

				final Rectangle route = drawRouteButton(graphics, fm, row, cursor);
				final MapCamera.FinderResultTarget target = new MapCamera.FinderResultTarget(row, route, regCenter, region.getDisplayName(), null, region);
				targets.add(target);
				if (cursor != null && row.contains(cursor))
				{
					hoveredTarget = target;
				}
				rowY += rowH;
			}
		}
		else
		{
			if (rows.isEmpty() && bodyScroll == 0)
			{
				graphics.setColor(TEXT_DIM);
				graphics.drawString("No matches", panel.x + padX, rowY + 14);
				rowY += rowH;
			}
			else
			{
				for (int i = bodyScroll; i < bodyEnd; i++)
				{
					final MapFinder.Result result = rows.get(i);
					final Rectangle row = new Rectangle(panel.x + 3, rowY, panelW - 6, rowH);
					final String groupKey = result.getGroupKey() != null ? result.getGroupKey() : result.getName();
					final boolean isHovered = cursor != null && row.contains(cursor);
					final boolean isSelected = i == 0;

					if (isHovered || isSelected)
					{
						graphics.setColor(isHovered ? CHIP_BG : CARD_EDGE);
						graphics.fillRoundRect(row.x, row.y, row.width, row.height, 4, 4);
					}
					if (isSelected)
					{
						graphics.setColor(CARD_TITLE);
						graphics.fillRect(row.x, row.y + 3, 2, row.height - 6);
					}

					if (groupKey.equals(camera.getHoveredRowKey()))
					{
						flyoutAnchorRow = row;
					}
					drawResultIcon(graphics, result, panel.x + padX, rowY + 4);

					final Rectangle walk;
					if (result.hasChildren())
					{
						walk = result.getPoint() == null ? null : drawRouteButton(graphics, fm, row, cursor);
						final String countText = "▸ " + result.getChildren().size();
						final int countW = fm.stringWidth(countText);
						graphics.setColor(CARD_TITLE);
						graphics.drawString(countText, row.x + row.width - countW - 60, rowY + rowH - 6);

						final int dist = result.getDistanceTiles();
						final String distText = distanceLabel(dist);
						if (!distText.isEmpty())
						{
							final int dW = fm.stringWidth(distText);
							graphics.setColor(TEXT_DIM);
							graphics.drawString(distText, row.x + row.width - countW - 66 - dW, rowY + 11);
						}

						final int nameX = panel.x + padX + 16;
						final int maxNameW = row.x + row.width - countW - 8 - 58 - nameX;
						drawClipped(graphics, fm, result.getName(), nameX, rowY + 12, maxNameW, CARD_TEXT);
						graphics.setColor(TEXT_DIM);
						graphics.drawString(itemKindLabel(result) + " · " + result.getChildren().size() + " locations",
							nameX, rowY + 25);
					}
					else
					{
						walk = result.getPoint() == null ? null : drawRouteButton(graphics, fm, row, cursor);

						final int dist = result.getDistanceTiles();
						final String distText = distanceLabel(dist);
						if (!distText.isEmpty())
						{
							final int dW = fm.stringWidth(distText);
							graphics.setColor(TEXT_DIM);
							graphics.drawString(distText, row.x + row.width - 62 - dW, rowY + 12);
						}

						final int nameX = panel.x + padX + 16;
						final int maxNameW = row.x + row.width - 74 - nameX;
						drawClipped(graphics, fm, result.getName(), nameX, rowY + 12, maxNameW, CARD_TEXT);
						final String subtitle = result.getDetail() != null ? result.getDetail()
							: result.getKind() == MapFinder.Result.Kind.MONSTER ? "Monster location"
							: result.getKind() == MapFinder.Result.Kind.MINERAL ? "Mining location" : "Location";
						graphics.setColor(TEXT_DIM);
						graphics.drawString(itemKindLabel(result) + " · " + subtitle, nameX, rowY + 25);
					}

					final MapFinder.Result routeTarget = MapFinder.activationTarget(result);
					final WorldPoint targetPoint = routeTarget != null ? routeTarget.getPoint() : result.getPoint();
					final MapCamera.FinderResultTarget target = new MapCamera.FinderResultTarget(row, walk, targetPoint, result.getName(), result, null);
					targets.add(target);
					if (cursor != null && row.contains(cursor))
					{
						hoveredTarget = target;
					}
					rowY += rowH;
				}
			}
		}

		rowY = bodyViewport.y + displayBodyRows * rowH;

		if (!isEmptyQuery && hasOverflow)
		{
			final int hiddenGroups = matchCount - rows.size();
			final String overflowText = "… " + hiddenGroups + " more group"
				+ (hiddenGroups == 1 ? "" : "s") + " • " + locationMatchCount + " locations";
			graphics.setColor(TEXT_DIM);
			graphics.drawString(overflowText, panel.x + padX, rowY + 14);
			rowY += rowH;
		}

		final MapFinder.Result flyoutHoveredItem = drawFlyout(graphics, panel, fm, cursor, padX, padTop, isEmptyQuery,
			bounds, flyoutAnchorRow, playerLoc, rows);

		// Description strip: 2 dim lines under body
		rowY += 4;
		graphics.setColor(CARD_EDGE);
		graphics.drawLine(panel.x + padX, rowY, panel.x + panelW - padX, rowY);
		final int descY = rowY + 4;

		String descLine1;
		String descLine2;

		if (flyoutHoveredItem != null)
		{
			final String[] description = resultDescription(flyoutHoveredItem);
			descLine1 = description[0];
			descLine2 = description[1];
		}
		else if (hoveredTarget != null)
		{
			if (hoveredTarget.getRegion() != null)
			{
				final MapRegion reg = hoveredTarget.getRegion();
				descLine1 = reg.getKingdom().getDisplayName();
				descLine2 = "Explore " + reg.getDisplayName();
			}
			else if (hoveredTarget.getResult() != null)
			{
				final String[] description = resultDescription(hoveredTarget.getResult());
				descLine1 = description[0];
				descLine2 = description[1];
			}
			else
			{
				descLine1 = "Location";
				descLine2 = hoveredTarget.getName();
			}
		}
		else
		{
			descLine1 = "Click a row to show it on the map";
			descLine2 = "";
		}

		final int maxDescW = panelW - 2 * padX;
		descLine1 = ellipsize(fm, descLine1, maxDescW, true);
		descLine2 = ellipsize(fm, descLine2, maxDescW, true);

		graphics.setColor(CARD_TITLE);
		graphics.drawString(descLine1, panel.x + padX, descY + 10);
		graphics.setColor(TEXT_DIM);
		graphics.drawString(descLine2, panel.x + padX, descY + 22);

		camera.setFinderResultTargets(targets);
		camera.setFinderPanelBounds(panel);
	}

	/**
	 * Submenu card for the hovered result row. Resolved before the description strip so that hovering a flyout item updates the strip; returns that item, or null.
	 */
	private MapFinder.Result drawFlyout(Graphics2D graphics, Rectangle panel, FontMetrics fm,
		java.awt.Point cursor, int padX, int padTop, boolean isEmptyQuery,
		Rectangle bounds, Rectangle flyoutAnchorRow,
		WorldPoint playerLoc, List<MapFinder.Result> rows)
	{
		final int rowH = 30;
		// Flyout submenu resolution (needed before description strip so hovering a flyout item updates strip)
		final String hoveredKey = camera.getHoveredRowKey();
		List<MapFinder.Result> flyoutItems = null;
		String flyoutTitle = null;

		if (hoveredKey != null)
		{
			if (isEmptyQuery)
			{
				MapRegion hoveredReg = MapRegion.byName(hoveredKey);
				if (hoveredReg == null)
				{
					try
					{
						hoveredReg = MapRegion.valueOf(hoveredKey);
					}
					catch (Exception ignored)
					{
					}
				}
				if (hoveredReg != null)
				{
					flyoutItems = finder.getRegionContents(hoveredReg, playerLoc);
					flyoutTitle = hoveredReg.getDisplayName();
				}
			}
			else
			{
				for (MapFinder.Result r : rows)
				{
					final String rKey = r.getGroupKey() != null ? r.getGroupKey() : r.getName();
					if (rKey.equalsIgnoreCase(hoveredKey) || r.getName().equalsIgnoreCase(hoveredKey))
					{
						if (r.hasChildren())
						{
							flyoutItems = r.getChildren();
							flyoutTitle = r.getName();
						}
						break;
					}
				}
			}
		}

		MapFinder.Result flyoutHoveredItem = null;
		final List<MapCamera.FlyoutTarget> flyoutTargets = new ArrayList<>();

		if (flyoutItems != null && !flyoutItems.isEmpty())
		{
				final int flyoutW = 260;
			final int totalFlyout = flyoutItems.size();
			camera.setFinderFlyoutItemCount(totalFlyout);
			camera.clampFinderFlyoutScroll();
			final int flyoutScroll = camera.getFinderFlyoutScrollOffset();
			final int flyoutEnd = Math.min(totalFlyout, flyoutScroll + MapCamera.FINDER_FLYOUT_ROWS);
			final int flyoutTitleH = 20;
			final int flyoutH = padTop + flyoutTitleH + MapCamera.FINDER_FLYOUT_ROWS * rowH + 6;

			int flyoutX = panel.x + panel.width + 6;
			final boolean flyoutOnRight = flyoutX + flyoutW <= bounds.getMaxX() - 4;
			if (!flyoutOnRight)
			{
				flyoutX = panel.x - flyoutW - 6;
			}
			if (flyoutX < bounds.getMinX() + 4)
			{
				flyoutX = (int) bounds.getMinX() + 4;
			}

			int flyoutY = flyoutAnchorRow != null ? flyoutAnchorRow.y : panel.y;
			if (flyoutY + flyoutH > bounds.getMaxY() - 4)
			{
				flyoutY = Math.max((int) bounds.getMinY() + 4, (int) bounds.getMaxY() - 4 - flyoutH);
			}

			final Rectangle flyoutPanel = new Rectangle(flyoutX, flyoutY, flyoutW, flyoutH);
			MapStyle.drawCard(graphics, flyoutPanel);

			graphics.setColor(CARD_TITLE);
			graphics.drawString(flyoutTitle != null ? flyoutTitle : "Locations", flyoutPanel.x + padX, flyoutPanel.y + padTop + 12);

			int fRowY = flyoutPanel.y + padTop + flyoutTitleH;
			final Rectangle flyoutViewport = new Rectangle(flyoutPanel.x + 3, fRowY, flyoutW - 6, MapCamera.FINDER_FLYOUT_ROWS * rowH);
			camera.setFinderFlyoutViewport(flyoutViewport);

			if (flyoutScroll > 0)
			{
				graphics.setColor(TEXT_DIM);
				graphics.drawString("▲", flyoutViewport.x + flyoutViewport.width - 12, flyoutViewport.y + 10);
			}
			if (flyoutEnd < totalFlyout)
			{
				graphics.setColor(TEXT_DIM);
				graphics.drawString("▼", flyoutViewport.x + flyoutViewport.width - 12, flyoutViewport.y + flyoutViewport.height - 4);
			}

			for (int fi = flyoutScroll; fi < flyoutEnd; fi++)
			{
				final MapFinder.Result item = flyoutItems.get(fi);
				final Rectangle itemRow = new Rectangle(flyoutPanel.x + 3, fRowY, flyoutW - 6, rowH);
				final boolean itemHovered = cursor != null && itemRow.contains(cursor);
				if (itemHovered)
				{
					graphics.setColor(CHIP_BG);
					graphics.fillRoundRect(itemRow.x, itemRow.y, itemRow.width, itemRow.height, 4, 4);
					flyoutHoveredItem = item;
				}

				final Rectangle route = item.getPoint() == null ? null : drawRouteButton(graphics, fm, itemRow, cursor);
				final String distText = distanceLabel(item.getDistanceTiles());
				if (!distText.isEmpty())
				{
					final int dW = fm.stringWidth(distText);
					graphics.setColor(TEXT_DIM);
					graphics.drawString(distText, itemRow.x + itemRow.width - (route == null ? 6 : route.width + 10) - dW, fRowY + 12);
				}
				drawClipped(graphics, fm, item.getName(), flyoutPanel.x + padX, fRowY + 12,
					flyoutW - 88, CARD_TEXT);
				graphics.setColor(TEXT_DIM);
				graphics.drawString(itemKindLabel(item), flyoutPanel.x + padX, fRowY + 25);

				flyoutTargets.add(new MapCamera.FlyoutTarget(itemRow, route, item.getPoint(), item.getName(), item));
				fRowY += rowH;
			}

			camera.setFinderFlyoutBounds(flyoutPanel);
			camera.setFlyoutTargets(flyoutTargets);

			final Rectangle hitBounds = new Rectangle(flyoutPanel);
			if (flyoutOnRight)
			{
				final int corridorW = flyoutX - (panel.x + panel.width);
				if (corridorW > 0)
				{
					hitBounds.add(new Rectangle(panel.x + panel.width, flyoutY, corridorW, flyoutH));
				}
			}
			else
			{
				final int corridorW = panel.x - (flyoutX + flyoutW);
				if (corridorW > 0)
				{
					hitBounds.add(new Rectangle(flyoutX + flyoutW, flyoutY, corridorW, flyoutH));
				}
			}
			if (flyoutAnchorRow != null)
			{
				hitBounds.add(flyoutAnchorRow);
			}
			camera.setFinderFlyoutHitBounds(hitBounds);
		}
		else
		{
			camera.setFinderFlyoutBounds(null);
			camera.setFinderFlyoutViewport(null);
			camera.setFinderFlyoutHitBounds(null);
			camera.setFlyoutTargets(Collections.emptyList());
		}
		return flyoutHoveredItem;
	}

	/**
	 * The four quick-find chips: Bank, Slayer, Travel, Last. Publishes each chip's hit region.
	 */
	private void drawChipRow(Graphics2D graphics, Rectangle panel, FontMetrics fm,
		java.awt.Point cursor, int chipRowY, int padX, int panelW, int chipRowH)
	{
		// Quick-find chip row: Bank · Slayer · Travel · Last
		final int chipGap = 4;
		final int chipW = (panelW - 2 * padX - 3 * chipGap) / 4;
		final Rectangle bankChip = new Rectangle(panel.x + padX, chipRowY, chipW, chipRowH);
		final Rectangle slayerChip = new Rectangle(panel.x + padX + chipW + chipGap, chipRowY, chipW, chipRowH);
		final Rectangle travelChip = new Rectangle(panel.x + padX + 2 * (chipW + chipGap), chipRowY, chipW, chipRowH);
		final Rectangle lastChip = new Rectangle(panel.x + padX + 3 * (chipW + chipGap), chipRowY, chipW, chipRowH);

		drawQuickChip(graphics, fm, cursor, bankChip, "Bank", true);
		drawQuickChip(graphics, fm, cursor, slayerChip, "Slayer",
			slayerTaskTracker != null && slayerTaskTracker.getCurrentTask() != null);
		drawQuickChip(graphics, fm, cursor, travelChip, "Travel", true);
		final String lastQuery = finder.getLastQuery();
		drawQuickChip(graphics, fm, cursor, lastChip, "Last", lastQuery != null && !lastQuery.isEmpty());

		camera.setFinderChipBank(bankChip);
		camera.setFinderChipSlayer(slayerChip);
		camera.setFinderChipTravel(travelChip);
		camera.setFinderChipLast(lastChip);

	}

	/**
	 * The query box. Editing happens in RuneLite's chatbox input while this is focused (that is what shows the live caret); here we mirror the text and colour the border to show it is active.
	 */
	private void drawSearchField(Graphics2D graphics, Rectangle field, FontMetrics fm,
		String query, boolean isEmptyQuery)
	{
		// Search field. Editing happens in RuneLite's chatbox input while this is focused (it shows
		// the live caret); here we just mirror the text and colour the border to show it is active.
		graphics.setColor(CHIP_BG);
		graphics.fillRoundRect(field.x, field.y, field.width, field.height, 4, 4);
		graphics.setColor(camera.isFinderFieldFocused() ? CARD_TITLE : CARD_EDGE);
		graphics.drawRoundRect(field.x, field.y, field.width, field.height, 4, 4);

		final int textY = field.y + 14;
		final int availW = field.width - 16;
		if (isEmptyQuery)
		{
			graphics.setColor(TEXT_DIM);
			graphics.drawString("Type a place name", field.x + 6, textY);
		}
		else
		{
			int start = 0;
			int end = query.length();
			if (fm.stringWidth(query) > availW)
			{
				while (start < end && fm.stringWidth(query.substring(start, end)) > availW - 12)
				{
					start++;
				}
			}
			final String displayText = query.substring(start, end);
			graphics.setColor(CARD_TEXT);
			graphics.drawString(displayText, field.x + 6, textY);
		}
		camera.setFinderFieldBounds(field);
	}

	private static void drawResultIcon(Graphics2D graphics, MapFinder.Result result, int x, int y)
	{
		final String icon;
		switch (result.getKind())
		{
			case SHOP: icon = "$"; break;
			case GROUND_ITEM: icon = "◆"; break;
			case MONSTER: icon = "☠"; break;
			case MINERAL: icon = "⛏"; break;
			default: icon = "●"; break;
		}
		graphics.setColor(result.getKind() == MapFinder.Result.Kind.MINERAL ? new Color(210, 170, 95) : CARD_TITLE);
		graphics.drawString(icon, x, y + 12);
	}

	private static Rectangle drawRouteButton(Graphics2D graphics, FontMetrics fm, Rectangle row,
		java.awt.Point cursor)
	{
		final Rectangle button = new Rectangle(row.x + row.width - 53, row.y + Math.max(2, (row.height - 20) / 2),
			49, Math.min(20, row.height - 4));
		final boolean hover = cursor != null && button.contains(cursor);
		graphics.setColor(hover ? CARD_EDGE : CHIP_BG);
		graphics.fillRoundRect(button.x, button.y, button.width, button.height, 4, 4);
		graphics.setColor(hover ? CARD_TITLE : CARD_EDGE);
		graphics.drawRoundRect(button.x, button.y, button.width, button.height, 4, 4);
		graphics.setColor(CARD_TEXT);
		final String label = "Route";
		graphics.drawString(label, button.x + (button.width - fm.stringWidth(label)) / 2,
			button.y + (button.height + fm.getAscent() - fm.getDescent()) / 2);
		return button;
	}

	private static String distanceLabel(int tiles)
	{
		if (tiles < 0) return "";
		if (tiles <= 20) return "Close";
		if (tiles <= 100) return "Moderate";
		return "Far";
	}

	private static void drawClipped(Graphics2D graphics, FontMetrics fm, String text,
		int x, int baseline, int maxWidth, Color color)
	{
		final String clipped = ellipsize(fm, text, maxWidth, false);
		graphics.setColor(color);
		graphics.drawString(clipped, x, baseline);
	}

	private static int distanceTiles(WorldPoint point, WorldPoint from)
	{
		if (from == null || point == null)
		{
			return -1;
		}
		final int dx = point.getX() - from.getX();
		final int dy = point.getY() - from.getY();
		return (int) Math.sqrt((double) dx * dx + (double) dy * dy);
	}

	private static String[] resultDescription(MapFinder.Result result)
	{
		final WorldPoint point = result.getPoint();
		final MapRegion region = point != null ? MapRegion.of(point.getX(), point.getY(), point.getPlane()) : null;
		final String regionName = region != null ? region.getDisplayName() : "Surface";
		final String kind = itemKindLabel(result);
		final String detail = result.getDetail();
		return new String[]{kind + " · " + regionName,
			detail != null && !detail.isEmpty() ? detail : result.getName()};
	}

	private static void drawQuickChip(Graphics2D graphics, FontMetrics metrics, java.awt.Point cursor,
		Rectangle chip, String label, boolean enabled)
	{
		final boolean hover = cursor != null && chip.contains(cursor);
		graphics.setColor(hover ? CARD_EDGE : CHIP_BG);
		graphics.fillRoundRect(chip.x, chip.y, chip.width, chip.height, 4, 4);
		graphics.setColor(hover ? CARD_TITLE : CARD_EDGE);
		graphics.drawRoundRect(chip.x, chip.y, chip.width, chip.height, 4, 4);
		graphics.setColor(enabled ? CARD_TEXT : TEXT_DIM);
		graphics.drawString(label, chip.x + (chip.width - metrics.stringWidth(label)) / 2, chip.y + 13);
	}

	/** Description rows keep an ellipsis even when no character fits; result names may stay empty. */
	private static String ellipsize(FontMetrics metrics, String text, int maxWidth, boolean keepEmptyEllipsis)
	{
		if (text == null) return "";
		String clipped = text;
		while (!clipped.isEmpty() && metrics.stringWidth(clipped) > maxWidth)
		{
			clipped = clipped.substring(0, clipped.length() - 1);
		}
		if (clipped.equals(text) || (clipped.isEmpty() && !keepEmptyEllipsis)) return clipped;
		while (!clipped.isEmpty() && metrics.stringWidth(clipped + "…") > maxWidth)
		{
			clipped = clipped.substring(0, clipped.length() - 1);
		}
		return clipped + "…";
	}
}
