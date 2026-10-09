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
 * THIS SOFTWARE search IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
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
package atlasofgielinor.ui;

import net.runelite.api.Client;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import static atlasofgielinor.ui.MapStyle.CARD_BG;
import static atlasofgielinor.ui.MapStyle.CARD_EDGE;
import static atlasofgielinor.ui.MapStyle.CARD_TEXT;
import static atlasofgielinor.ui.MapStyle.CARD_TITLE;
import static atlasofgielinor.ui.MapStyle.CHIP_BG;
import static atlasofgielinor.ui.MapStyle.LEFT_TOOLBAR_BUTTON_SIZE;
import static atlasofgielinor.ui.MapStyle.LEFT_TOOLBAR_LAYERS;
import static atlasofgielinor.ui.MapStyle.SMALL;
import static atlasofgielinor.ui.MapStyle.TEXT_DIM;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import atlasofgielinor.AtlasOfGielinorConfig;
import atlasofgielinor.data.dungeons.UndergroundZone;
import atlasofgielinor.data.sailing.BoatTracker;
import atlasofgielinor.data.sailing.PlayerBoat;
import atlasofgielinor.data.sailing.SailingPort;
import atlasofgielinor.integrations.ClueScrollTracker;
import atlasofgielinor.map.MapCamera;
import atlasofgielinor.integrations.QuestHelperTracker;
import atlasofgielinor.integrations.ShortestPathTracker;
import atlasofgielinor.map.WorldMapInput;
import atlasofgielinor.tiles.WikiMapTiles;
import net.runelite.client.ui.overlay.components.PanelComponent;

/**
 * The map's own furniture: the plane switcher, the status chip and its credits, the layers
 * panel and the chat button.
 *
 * <p>Split out of {@link AtlasOfGielinorOverlay}. Every button it draws publishes its screen
 * rect to {@link MapCamera} for {@link WorldMapInput} to hit-test.
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class MapChromeRenderer
{
	private static final String[] CREDITS = {WikiMapTiles.ATTRIBUTION, WikiMapTiles.DATA_ATTRIBUTION};

	private final AtlasOfGielinorConfig config;
	private final MapCamera camera;
	private final WorldMapInput input;
	private final MapLayout layout;
	private final ClueScrollTracker clueScrollTracker;
	private final QuestHelperTracker questHelperTracker;
	private final ShortestPathTracker shortestPathTracker;
	private final BoatTracker boatTracker;
	private final Client client;
	private int topBarLeftChipsRight;

	MapChromeRenderer(AtlasOfGielinorConfig config, MapCamera camera, WorldMapInput input, MapLayout layout,
		ClueScrollTracker clueScrollTracker, QuestHelperTracker questHelperTracker)
	{
		this(config, camera, input, layout, clueScrollTracker, questHelperTracker, null, null, null);
	}


	private int nextTopChipX(Rectangle bounds, Rectangle... previousChips)
	{
		int maxX = 0;
		if (previousChips != null)
		{
			for (Rectangle chip : previousChips)
			{
				if (chip != null)
				{
					maxX = Math.max(maxX, chip.x + chip.width);
				}
			}
		}
		if (maxX > 0)
		{
			return maxX + 6;
		}
		return topBarLeftChipsRight > 0 ? topBarLeftChipsRight + 8 : (int) bounds.getMinX() + 8;
	}

	void drawPlaneSwitcher(Graphics2D graphics, Rectangle bounds)
	{
		if (!config.showPlaneSwitcher())
		{
			camera.setPlaneButtons(new Rectangle[0]);
			return;
		}

		final Rectangle[] buttons = new Rectangle[4];
		final int btnWidth = 46;
		final int btnHeight = 20;
		final int gap = 3;
		final int rightInset = layout.rightUiInset(bounds);
		final int x = (int) bounds.getMaxX() - btnWidth - 8 - rightInset;
		int y = (int) bounds.getMinY() + 34;

		graphics.setFont(SMALL);
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		final String[] labels = {"Upper", "Main", "Lower", "Travel"};
		final boolean isTravelActive = camera.isTravelViewActive();
		final boolean isLowerActive = camera.isUndergroundModeActive();
		final boolean isUpperActive = !isTravelActive && !isLowerActive && camera.getPlane() == 1;
		final boolean isMainActive = !isTravelActive && !isLowerActive && camera.getPlane() == 0;
		final boolean[] active = {isUpperActive, isMainActive, isLowerActive, isTravelActive};

		for (int i = 0; i < buttons.length; i++)
		{
			final Rectangle button = new Rectangle(x, y, btnWidth, btnHeight);
			buttons[i] = button;

			final boolean selected = active[i];
			drawToggleButton(graphics, button, selected);

			final String label = labels[i];
			final int textWidth = graphics.getFontMetrics().stringWidth(label);
			graphics.setColor(selected ? Color.BLACK : TEXT_DIM);
			graphics.drawString(label,
				button.x + (btnWidth - textWidth) / 2,
				button.y + btnHeight - 6);

			y += btnHeight + gap;
		}

		camera.setPlaneButtons(buttons);
	}

	/**
	 * A small chip in the top-left corner. Anchoring to the top keeps it beside the map instead
	 * of stranded at the bottom edge of a tall window.
	 */
	void drawStatusChip(Graphics2D graphics, Rectangle bounds)
	{
		graphics.setFont(SMALL);

		final StringBuilder text = new StringBuilder(camera.isTravelViewActive() ? "World Map (Travel)" : camera.isUndergroundModeActive()
			? "World Map (Underground)"
			: camera.isInteriorMapUnavailable()
				? "World Map • Interior map unavailable"
				: camera.getCenterY() >= 4200 ? "World Map (Underground)" : "World Map");
		if (config.showCoordinateOverlay())
		{
			final java.awt.Point cursor = input.getCursor();
			if (cursor != null && bounds.contains(cursor))
			{
				text.append("   ")
					.append((int) Math.floor(camera.worldX(cursor.x, bounds)))
					.append(", ")
					.append((int) Math.floor(camera.worldY(cursor.y, bounds)));
			}
			text.append(String.format("   %.2f px/tile", camera.getZoom()));
		}

		final String label = text.toString();
		final int width = graphics.getFontMetrics().stringWidth(label);
		final int x = (int) bounds.getMinX() + 8;
		final int y = (int) bounds.getMinY() + 8;

		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setColor(CHIP_BG);
		graphics.fillRoundRect(x, y, width + 14, 20, 7, 7);
		graphics.setColor(TEXT_DIM);
		graphics.drawString(label, x + 7, y + 14);

		int chipsRight = x + width + 14;

		if (camera.isUndergroundModeActive())
		{
			final UndergroundZone zone = camera.getActiveUndergroundZone();
			final String retText = "✕  Return to Surface  •  " + (zone != null ? zone.getName() : "Full Underground Map");
			final int retWidth = graphics.getFontMetrics().stringWidth(retText);
			final Rectangle retButton = new Rectangle(chipsRight + 8, y, retWidth + 14, 20);

			graphics.setColor(CARD_BG);
			graphics.fillRoundRect(retButton.x, retButton.y, retButton.width, retButton.height, 7, 7);
			graphics.setColor(CARD_EDGE);
			graphics.drawRoundRect(retButton.x, retButton.y, retButton.width, retButton.height, 7, 7);
			graphics.setColor(CARD_TITLE);
			graphics.drawString(retText, retButton.x + 7, retButton.y + 14);

			camera.setUndergroundReturnButton(retButton);
			chipsRight = retButton.x + retButton.width;
		}
		else
		{
			camera.setUndergroundReturnButton(null);
		}

		this.topBarLeftChipsRight = chipsRight;

		// The tiles come from the game cache and the datasets from the wiki.
		graphics.setColor(TEXT_DIM);
		int creditY = (int) bounds.getMaxY() - 8;
		for (int i = CREDITS.length - 1; i >= 0; i--)
		{
			final String credit = CREDITS[i];
			graphics.drawString(credit,
				(int) bounds.getMaxX() - graphics.getFontMetrics().stringWidth(credit) - 8,
				creditY);
			creditY -= graphics.getFontMetrics().getHeight();
		}
	}

	/** A "Go to clue" button beside the status chip for RuneLite's solved clue target. */
	void drawClueButton(Graphics2D graphics, Rectangle bounds)
	{
		if (!config.showClueScroll() || clueScrollTracker.locations().isEmpty())
		{
			camera.setClueButton(null);
			return;
		}

		final String label = clueScrollTracker.locations().size() > 1 ? "Go to Clue (nearest)" : "Go to Clue";
		final Rectangle button = topChip(graphics, bounds, label, topBarLeftChipsRight > 0 ? topBarLeftChipsRight + 8 : (int) bounds.getMinX() + 8, false);
		if (button == null)
		{
			camera.setClueButton(null);
			return;
		}
		final int glyph = 12;

		// Purple diamond for the clue navigation button.
		final int cx = button.x + 7 + glyph / 2;
		final int cy = button.y + button.height / 2;
		final int r = glyph / 2;
		graphics.setColor(MapStyle.CLUE_FILL);
		graphics.fillPolygon(new int[]{cx, cx + r, cx, cx - r}, new int[]{cy - r, cy, cy + r, cy}, 4);
		graphics.setColor(MapStyle.CLUE_EDGE);
		graphics.drawPolygon(new int[]{cx, cx + r, cx, cx - r}, new int[]{cy - r, cy, cy + r, cy}, 4);

		drawTopChipLabel(graphics, button, label, false);

		camera.setClueButton(button);
	}

	/**
	 * "Go to Quest Step" chip, shown whenever the Quest Helper plugin has a step on the map. Sits
	 * to the right of the clue chip when both are up, so neither has to move when the other
	 * appears. Drawn after {@link #drawClueButton} for that reason.
	 */
	void drawQuestButton(Graphics2D graphics, Rectangle bounds)
	{
		// Clicking the chip routes, so it is hidden when routing itself is switched off.
		if (questHelperTracker.target() == null)
		{
			camera.setQuestButton(null);
			return;
		}

		final String label = "Go to Quest Step";
		final Rectangle clueChip = camera.getClueButton();
		final Rectangle button = topChip(graphics, bounds, label, clueChip != null
			? clueChip.x + clueChip.width + 6
			: (topBarLeftChipsRight > 0 ? topBarLeftChipsRight + 8 : (int) bounds.getMinX() + 8), false);
		if (button == null)
		{
			camera.setQuestButton(null);
			return;
		}
		final int glyph = 12;

		// Green dot, distinct from the clue chip's purple diamond.
		final int cx = button.x + 7 + glyph / 2;
		final int cy = button.y + button.height / 2;
		final int r = glyph / 2;
		graphics.setColor(MapStyle.QUEST_FILL);
		graphics.fillOval(cx - r, cy - r, r * 2, r * 2);
		graphics.setColor(MapStyle.QUEST_EDGE);
		graphics.drawOval(cx - r, cy - r, r * 2, r * 2);

		drawTopChipLabel(graphics, button, label, false);

		camera.setQuestButton(button);
	}

	/**
	 * "Go to Player" chip. Pans and flashes the map on the local player, following them into an
	 * instance layer. Sits to the right of the clue and quest chips so it never shoves them.
	 * Hit-tested in {@link WorldMapInput}, which calls {@code AtlasOfGielinorPlugin.goToPlayer()}.
	 */
	void drawPlayerButton(Graphics2D graphics, Rectangle bounds)
	{
		if (!config.showPlayerMarker())
		{
			camera.setPlayerButton(null);
			return;
		}

		final String label = "Go to Player";
		final Rectangle button = topChip(graphics, bounds, label, nextTopChipX(bounds, camera.getQuestButton(), camera.getClueButton()), false);
		if (button == null)
		{
			camera.setPlayerButton(null);
			return;
		}
		final int glyph = 12;

		// White arrowhead, matching the player marker on the map.
		final int cx = button.x + 7 + glyph / 2;
		final int cy = button.y + button.height / 2;
		final int r = glyph / 2;
		final int[] px = {cx, cx - r, cx, cx + r};
		final int[] py = {cy - r, cy + r, cy + r / 2, cy + r};
		graphics.setColor(Color.WHITE);
		graphics.fillPolygon(px, py, 4);
		graphics.setColor(new Color(18, 18, 18));
		graphics.drawPolygon(px, py, 4);

		drawTopChipLabel(graphics, button, label, false);

		camera.setPlayerButton(button);
	}

	/**
	 * "Go to Destination" chip. Pans the map to the active route / Shortest Path destination.
	 * Sits on the top bar alongside the other destination chips.
	 */
	void drawDestinationButton(Graphics2D graphics, Rectangle bounds)
	{
		if (shortestPathTracker == null || !shortestPathTracker.isRoutingEnabled()
			|| !shortestPathTracker.hasTarget() || shortestPathTracker.target() == null)
		{
			camera.setDestinationButton(null);
			return;
		}

		final String label = "Go to Destination";
		final Rectangle button = topChip(graphics, bounds, label, nextTopChipX(bounds, camera.getPlayerButton(), camera.getQuestButton(), camera.getClueButton()), false);
		if (button == null)
		{
			camera.setDestinationButton(null);
			return;
		}
		final int glyph = 12;

		// Cyan waypoint / destination target glyph (concentric rings), matching the route line.
		final int cx = button.x + 7 + glyph / 2;
		final int cy = button.y + button.height / 2;
		final int r = glyph / 2;
		graphics.setColor(MapStyle.ROUTE_LINE);
		graphics.drawOval(cx - r, cy - r, glyph, glyph);
		graphics.fillOval(cx - 2, cy - 2, 4, 4);

		drawTopChipLabel(graphics, button, label, false);

		camera.setDestinationButton(button);
	}

	/**
	 * "Player Boats" chip with dropdown. Shows a dropdown list of all owned boats with
	 * their name and current location. Clicking a boat pans to its location on the map.
	 */
	void drawBoatsButton(Graphics2D graphics, Rectangle bounds)
	{
		if (!config.showBoatLocations() || boatTracker == null || boatTracker.getOwnedBoats().isEmpty())
		{
			camera.setBoatsButton(null);
			camera.setBoatsDropdownOpen(false);
			camera.setBoatsDropdownBounds(null);
			camera.setBoatDropdownTargets(Collections.emptyList());
			return;
		}

		final boolean open = camera.isBoatsDropdownOpen();
		final String label = "Player Boats " + (open ? "\u25B4" : "\u25BE");
		final Rectangle button = topChip(graphics, bounds, label, nextTopChipX(bounds, camera.getDestinationButton(), camera.getPlayerButton(),
			camera.getQuestButton(), camera.getClueButton()), open);
		if (button == null)
		{
			camera.setBoatsButton(null);
			camera.setBoatsDropdownOpen(false);
			camera.setBoatsDropdownBounds(null);
			camera.setBoatDropdownTargets(Collections.emptyList());
			return;
		}
		final int glyph = 12;

		// Boat glyph: sailboat icon
		final int cx = button.x + 7 + glyph / 2;
		final int cy = button.y + button.height / 2;
		drawBoatGlyph(graphics, cx, cy, open ? Color.BLACK : CARD_TITLE, open ? Color.BLACK : Color.WHITE);

		drawTopChipLabel(graphics, button, label, open);

		camera.setBoatsButton(button);
	}

	void drawBoatsDropdown(Graphics2D graphics, Rectangle bounds)
	{
		if (!camera.isBoatsDropdownOpen() || camera.getBoatsButton() == null
			|| boatTracker == null || !config.showBoatLocations())
		{
			camera.setBoatsDropdownBounds(null);
			camera.setBoatDropdownTargets(Collections.emptyList());
			return;
		}

		final List<PlayerBoat> boats = boatTracker.getOwnedBoats();
		if (boats.isEmpty())
		{
			camera.setBoatsDropdownOpen(false);
			camera.setBoatsDropdownBounds(null);
			camera.setBoatDropdownTargets(Collections.emptyList());
			return;
		}

		final Rectangle btn = camera.getBoatsButton();
		graphics.setFont(SMALL);
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		final FontMetrics fm = graphics.getFontMetrics();

		int maxTextW = 140;
		for (PlayerBoat boat : boats)
		{
			final String name = boat.getBoatName() != null ? boat.getBoatName() : "Boat " + boat.getBoatId();
			final String loc = boat.getPort() != null
				? boat.getPort().getName()
				: SailingPort.resolvePortName(client, boat.getPortId());
			maxTextW = Math.max(maxTextW, fm.stringWidth(name));
			maxTextW = Math.max(maxTextW, fm.stringWidth(loc));
		}

		final int rowH = 34;
		final int padY = 6;
		final int panelW = Math.max(180, maxTextW + 36);
		final int panelH = padY * 2 + boats.size() * rowH;

		int panelX = btn.x;
		if (panelX + panelW > bounds.getMaxX() - 8)
		{
			panelX = (int) bounds.getMaxX() - 8 - panelW;
		}
		final int panelY = btn.y + btn.height + 4;
		final Rectangle panel = new Rectangle(panelX, panelY, panelW, panelH);

		MapStyle.drawCard(graphics, panel);

		final Point cursor = input.getCursor();
		final List<MapCamera.BoatDropdownTarget> targets = new ArrayList<>(boats.size());
		int rowY = panel.y + padY;

		for (PlayerBoat boat : boats)
		{
			final Rectangle row = new Rectangle(panel.x + 4, rowY, panel.width - 8, rowH - 2);
			final boolean hover = cursor != null && row.contains(cursor);

			if (hover)
			{
				graphics.setColor(CHIP_BG);
				graphics.fillRoundRect(row.x, row.y, row.width, row.height, 4, 4);
				graphics.setColor(CARD_EDGE);
				graphics.drawRoundRect(row.x, row.y, row.width, row.height, 4, 4);
			}

			// Boat icon / glyph on the left
			final int icx = row.x + 12;
			final int icy = row.y + rowH / 2 - 1;
			drawBoatGlyph(graphics, icx, icy, CARD_TITLE, Color.WHITE);

			final String name = boat.getBoatName() != null ? boat.getBoatName() : "Boat " + boat.getBoatId();
			final String loc = boat.getPort() != null
				? boat.getPort().getName()
				: SailingPort.resolvePortName(client, boat.getPortId());

			graphics.setColor(hover ? CARD_TITLE : CARD_TEXT);
			graphics.drawString(name, row.x + 24, row.y + 13);
			graphics.setColor(TEXT_DIM);
			graphics.drawString(loc, row.x + 24, row.y + 26);

			targets.add(new MapCamera.BoatDropdownTarget(row, boat));
			rowY += rowH;
		}

		camera.setBoatsDropdownBounds(panel);
		camera.setBoatDropdownTargets(targets);
	}

	/**
	 * Collapsible clue scroll helper card for the fullscreen world map.
	 *
	 * <p>Displays the native clue plugin's hint panel inside Atlas of Gielinor's card.
	 */
	void drawCluePanel(Graphics2D graphics, Rectangle bounds)
	{
		if (!config.showClueScroll() || !config.fullscreenMap())
		{
			camera.setCluePanelBounds(null);
			camera.setCluePanelHeaderBounds(null);
			return;
		}

		final PanelComponent cluePanel = clueScrollTracker.cluePanel();
		final boolean hasPanel = cluePanel != null && !cluePanel.getChildren().isEmpty();
		if (!hasPanel)
		{
			camera.setCluePanelBounds(null);
			camera.setCluePanelHeaderBounds(null);
			return;
		}

		// Yield when layers panel or finder panel is open to avoid overlapping menus.
		if (camera.isLayersPanelOpen() || camera.isFinderPanelOpen())
		{
			camera.setCluePanelBounds(null);
			camera.setCluePanelHeaderBounds(null);
			return;
		}

		graphics.setFont(SMALL);
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		final FontMetrics metrics = graphics.getFontMetrics();

		final boolean collapsed = camera.isCluePanelCollapsed();
		final String title = "Clue Scroll";

		// The clue plugin's hint is drawn by its own PanelComponent renderer. Its width is an
		// input, not an output (LineComponent.render echoes the width it is given), so we fix the
		// content width and measure only the height, with a scratch two-pass render.
		final int contentW = 220;
		final int headerH = 24;
		Dimension measured = null;
		if (!collapsed)
		{
			cluePanel.setBackgroundColor(null);
			cluePanel.setPreferredSize(new Dimension(contentW, 0));
			cluePanel.setPreferredLocation(new Point(0, 0));
			measured = measurePanel(cluePanel, graphics);
		}

		final int contentH = measured != null ? Math.max(metrics.getHeight(), measured.height) : 0;
		final int panelW = collapsed
			? Math.min(260, Math.max(185, metrics.stringWidth(title) + 38))
			: Math.min(280, Math.max(185, (measured != null ? measured.width : metrics.stringWidth(title) + 38) + 16));
		final int panelH = collapsed ? headerH : headerH + 4 + Math.max(metrics.getHeight(), contentH) + 6;

		final int panelX = (int) bounds.getMinX() + 8;
		int panelY = layout.leftToolbarTop(bounds) + LEFT_TOOLBAR_BUTTON_SIZE + 8;
		if (panelY + panelH > bounds.getMaxY() - 4)
		{
			panelY = Math.max((int) bounds.getMinY() + 4, (int) bounds.getMaxY() - 4 - panelH);
		}

		final Rectangle panel = new Rectangle(panelX, panelY, panelW, panelH);
		MapStyle.drawCard(graphics, panel);

		// Header purple diamond glyph
		final int glyph = 10;
		final int cx = panel.x + 8 + glyph / 2;
		final int cy = panel.y + headerH / 2;
		final int r = glyph / 2;
		graphics.setColor(MapStyle.CLUE_FILL);
		graphics.fillPolygon(new int[]{cx, cx + r, cx, cx - r}, new int[]{cy - r, cy, cy + r, cy}, 4);
		graphics.setColor(MapStyle.CLUE_EDGE);
		graphics.drawPolygon(new int[]{cx, cx + r, cx, cx - r}, new int[]{cy - r, cy, cy + r, cy}, 4);

		// Title
		graphics.setColor(CARD_TITLE);
		graphics.drawString(title, panel.x + 8 + glyph + 6, panel.y + 16);

		// Chevron [▼] / [▲]
		final Rectangle chevronBtn = new Rectangle(panel.x + panel.width - 20, panel.y + 3, 16, 18);
		final java.awt.Point cursor = input.getCursor();
		final boolean hoverChevron = cursor != null && chevronBtn.contains(cursor);
		graphics.setColor(hoverChevron ? CARD_TITLE : TEXT_DIM);
		graphics.drawString(collapsed ? "\u25BC" : "\u25B2", chevronBtn.x + 2, chevronBtn.y + 13);

		camera.setCluePanelHeaderBounds(new Rectangle(panel.x, panel.y, panel.width, headerH));
		camera.setCluePanelBounds(panel);

		if (collapsed)
		{
			return;
		}

		// Subtle divider between header and content
		graphics.setColor(new Color(196, 162, 80, 80));
		graphics.drawLine(panel.x + 6, panel.y + headerH, panel.x + panel.width - 6, panel.y + headerH);

		final int contentY = panel.y + headerH + 4;
		// RuneLite's own PanelComponent renderer draws the clue plugin's hint verbatim (white
		// body text, centred TitleComponents) inside Atlas of Gielinor's gold card. Its background is
		// suppressed so only the card box shows.
		cluePanel.setPreferredLocation(new Point(panel.x + 8, contentY));
		cluePanel.render(graphics);
	}

	/**
	 * The surface {@link #measurePanel} measures against. Nothing is ever drawn into it, so one
	 * buffer serves every call rather than one per card per frame. Client thread only.
	 */
	private static final BufferedImage MEASURE_SCRATCH = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);

	/**
	 * A {@link PanelComponent} reports a stale {@code (10,10)} size on its first {@code render}
	 * (it computes the return value from a cache filled only at the end of the call), so measure
	 * with two throwaway renders on a 1x1 buffer whose font matches the real one.
	 */
	private static Dimension measurePanel(PanelComponent panel, Graphics2D reference)
	{
		final Graphics2D sg = MEASURE_SCRATCH.createGraphics();
		try
		{
			sg.setFont(reference.getFont());
			panel.render(sg);
			final Dimension d = panel.render(sg);
			return d != null ? d : new Dimension(0, 0);
		}
		finally
		{
			sg.dispose();
		}
	}

	/**
	 * A gear button top-left (first slot in the left toolbar) and, when expanded, a checkbox card
	 * of {@link AtlasOfGielinorOverlay#LAYER_TOGGLES}. Clicking a row writes the matching {@code bettermap.*} key; the
	 * overlay re-reads config every frame so the map updates on the next one. Rects are published
	 * to {@link MapCamera} for {@code WorldMapInput} to hit-test, and cleared by
	 * {@code MapCamera.setActive(false)}.
	 */
	void drawLayersPanel(Graphics2D graphics, Rectangle bounds)
	{
		graphics.setFont(SMALL);
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		final int size = LEFT_TOOLBAR_BUTTON_SIZE;
		final int top = layout.leftToolbarTop(bounds);
		final Rectangle gear = new Rectangle(layout.leftToolbarButtonX(bounds, LEFT_TOOLBAR_LAYERS), top, size, size);
		final boolean open = camera.isLayersPanelOpen();

		drawToggleButton(graphics, gear, open);
		graphics.setColor(open ? Color.BLACK : TEXT_DIM);
		for (int i = 0; i < 3; i++)
		{
			graphics.fillRect(gear.x + 5, gear.y + 6 + i * 5, gear.width - 10, 2);
		}
		camera.setLayersButton(gear);

		if (!open)
		{
			camera.setLayerToggleTargets(Collections.emptyList());
			camera.setLayersPanelBounds(null);
			return;
		}

		final int rowH = 18;
		final int padX = 10;
		final int padTop = 6;
		final int titleH = 18;
		final int panelW = 188;
		final int panelH = padTop + titleH + (1 + AtlasOfGielinorOverlay.LAYER_TOGGLES.size()) * rowH + 6;

		int panelX = gear.x;
		int panelY = gear.y + gear.height + 4;
		if (panelX + panelW > bounds.getMaxX() - 4)
		{
			panelX = (int) bounds.getMaxX() - 4 - panelW;
		}
		if (panelY + panelH > bounds.getMaxY() - 4)
		{
			panelY = Math.max((int) bounds.getMinY() + 4, (int) bounds.getMaxY() - 4 - panelH);
		}

		final Rectangle panel = new Rectangle(panelX, panelY, panelW, panelH);
		MapStyle.drawCard(graphics, panel);

		graphics.setColor(CARD_TITLE);
		graphics.drawString("Layers", panel.x + padX, panel.y + padTop + 12);

		final List<MapCamera.LayerToggleTarget> targets = new ArrayList<>();
		int rowY = panel.y + padTop + titleH;

		rowY = appendLayerToggleRow(graphics, panel, padX, rowH, rowY, targets,
			"Hide all", AtlasOfGielinorOverlay.allLayersHidden(config), AtlasOfGielinorOverlay.HIDE_ALL_LAYERS_KEY);

		for (AtlasOfGielinorOverlay.LayerToggle toggle : AtlasOfGielinorOverlay.LAYER_TOGGLES)
		{
			final boolean on = toggle.getter.test(config);
			rowY = appendLayerToggleRow(graphics, panel, padX, rowH, rowY, targets, toggle.label, on, toggle.key);
		}

		camera.setLayerToggleTargets(targets);
		camera.setLayersPanelBounds(panel);
	}

	private static int appendLayerToggleRow(
		Graphics2D graphics,
		Rectangle panel,
		int padX,
		int rowH,
		int rowY,
		List<MapCamera.LayerToggleTarget> targets,
		String label,
		boolean on,
		String configKey)
	{
		final Rectangle row = new Rectangle(panel.x + 3, rowY, panel.width - 6, rowH);

		final int box = 10;
		final int boxX = panel.x + padX;
		final int boxY = rowY + (rowH - box) / 2;
		graphics.setColor(on ? CARD_EDGE : CHIP_BG);
		graphics.fillRoundRect(boxX, boxY, box, box, 3, 3);
		graphics.setColor(on ? CARD_TITLE : TEXT_DIM);
		graphics.drawRoundRect(boxX, boxY, box, box, 3, 3);
		if (on)
		{
			graphics.setColor(Color.BLACK);
			graphics.drawLine(boxX + 2, boxY + 5, boxX + 4, boxY + 7);
			graphics.drawLine(boxX + 4, boxY + 7, boxX + 8, boxY + 2);
		}

		graphics.setColor(on ? CARD_TEXT : TEXT_DIM);
		graphics.drawString(label, boxX + box + 6, rowY + rowH - 5);

		targets.add(new MapCamera.LayerToggleTarget(row, configKey, on));
		return rowY + rowH;
	}

	/**
	 * Fullscreen-only chat toggle on the top-right strip.
	 *
	 * <p>In fullscreen the game's chatbox draws over the map, and because our viewport covers the
	 * whole canvas every click on it is consumed before the chatbox sees it — including the one on
	 * its own minimise control. Without this button the chat cannot be dismissed at all while the
	 * map is open.
	 */
	void drawChatButton(Graphics2D graphics, Rectangle bounds)
	{
		// Only fullscreen puts our map over the chatbox; windowed leaves it reachable.
		if (!config.fullscreenMap())
		{
			camera.setChatButton(null);
			camera.setChatHidden(false);
			return;
		}

		graphics.setFont(SMALL);
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		final int size = LEFT_TOOLBAR_BUTTON_SIZE;
		final int top = (int) bounds.getMinY() + 8;
		final int x = layout.topRightStripRight(bounds) - size;
		final Rectangle button = new Rectangle(x, top, size, size);
		final boolean hidden = camera.isChatHidden();

		drawToggleButton(graphics, button, hidden);

		// A speech bubble: rounded body plus a tail. Struck through while the chat is hidden.
		graphics.setColor(hidden ? Color.BLACK : TEXT_DIM);
		graphics.drawRoundRect(button.x + 5, button.y + 6, 12, 8, 3, 3);
		graphics.drawLine(button.x + 8, button.y + 14, button.x + 7, button.y + 17);
		graphics.drawLine(button.x + 7, button.y + 17, button.x + 11, button.y + 14);
		if (hidden)
		{
			graphics.drawLine(button.x + 4, button.y + 17, button.x + 18, button.y + 4);
		}

		camera.setChatButton(button);
	}

	private static void drawToggleButton(Graphics2D graphics, Rectangle button, boolean selected)
	{
		graphics.setColor(selected ? CARD_EDGE : CHIP_BG);
		graphics.fillRoundRect(button.x, button.y, button.width, button.height, 6, 6);
		graphics.setColor(selected ? CARD_TITLE : TEXT_DIM);
		graphics.drawRoundRect(button.x, button.y, button.width, button.height, 6, 6);
	}

	private Rectangle topChip(Graphics2D graphics, Rectangle bounds, String label, int x, boolean selected)
	{
		graphics.setFont(SMALL);
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		final Rectangle button = new Rectangle(x, bounds.y + 8,
			12 + 8 + graphics.getFontMetrics().stringWidth(label) + 14, 20);
		final int rightLimit = layout.topRightStripRight(bounds)
			- (config.fullscreenMap() ? LEFT_TOOLBAR_BUTTON_SIZE + 8 : 0);
		if (button.x + button.width > rightLimit)
		{
			return null;
		}
		graphics.setColor(selected ? CARD_EDGE : CARD_BG);
		graphics.fillRoundRect(button.x, button.y, button.width, button.height, 7, 7);
		graphics.setColor(CARD_TITLE);
		graphics.drawRoundRect(button.x, button.y, button.width, button.height, 7, 7);
		return button;
	}

	private static void drawTopChipLabel(Graphics2D graphics, Rectangle button, String label, boolean selected)
	{
		graphics.setColor(selected ? Color.BLACK : CARD_TITLE);
		graphics.drawString(label, button.x + 7 + 12 + 8, button.y + 14);
	}

	private static void drawBoatGlyph(Graphics2D graphics, int cx, int cy, Color hull, Color sail)
	{
		graphics.setColor(hull);
		graphics.fillPolygon(new int[]{cx - 5, cx + 5, cx + 3, cx - 3},
			new int[]{cy + 2, cy + 2, cy + 5, cy + 5}, 4);
		graphics.drawLine(cx, cy + 1, cx, cy - 5);
		graphics.setColor(sail);
		graphics.fillPolygon(new int[]{cx, cx + 4, cx}, new int[]{cy - 5, cy - 1, cy + 1}, 3);
	}
}
