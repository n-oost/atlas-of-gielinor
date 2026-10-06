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
package atlasofgielinor.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import atlasofgielinor.data.BossLocationData;
import net.runelite.client.ui.FontManager;

/**
 * The colours, fonts, strokes and fixed geometry the world-map renderers share.
 *
 * <p>Split out of {@link AtlasOfGielinorOverlay} so the draw code reads as draw code; the
 * renderers static-import these, so a constant is named the same everywhere it is used.
 */
public final class MapStyle
{
	public static final Color TEXT_DIM = new Color(205, 205, 205, 165);
	public static final Color TEXT_WARN = new Color(255, 214, 120);
	public static final Color CLUE_FILL = new Color(120, 90, 210, 235);
	public static final Color CLUE_EDGE = new Color(214, 190, 255, 240);
	/** Quest Helper green, kept clear of the purple the clue markers use. */
	public static final Color QUEST_FILL = new Color(70, 175, 105, 235);
	public static final Color QUEST_EDGE = new Color(178, 240, 200, 240);
	public static final Color CARD_BG = new Color(16, 19, 26, 238);
	public static final Color CARD_EDGE = new Color(196, 162, 80, 200);
	public static final Color CARD_TITLE = new Color(255, 209, 102);
	public static final Color CARD_TEXT = new Color(214, 214, 214);
	public static final Color CHIP_BG = new Color(16, 19, 26, 200);
	public static final Color DEBUG_KEY = new Color(140, 200, 255);

	public static final Font RUNELITE_PLAIN = FontManager.getDefaultFont().deriveFont(11f);
	public static final Font RUNELITE_BOLD = FontManager.getDefaultBoldFont().deriveFont(11f);

	public static final Font SMALL = RUNELITE_PLAIN;

	/** The card row colours, handed to {@link CardText} so its layout stays free of the overlay. */
	public static final CardText.Palette CARD_PALETTE =
		new CardText.Palette(DEBUG_KEY, TEXT_WARN, TEXT_DIM, CARD_TEXT);

	public static final Color ROUTE_SHADOW = new Color(18, 18, 18, 200);
	/** Route read from the Shortest Path plugin. */
	public static final Color ROUTE_LINE = new Color(96, 205, 255, 235);

	/** Enum.values() clones its array on every call; these are read in per-frame loops. */
	public static final BossLocationData[] BOSSES = BossLocationData.values();

	/**
	 * Left toolbar below the status chip: layers gear (slot 0), finder magnifier (slot 1).
	 */
	public static final int LEFT_TOOLBAR_BUTTON_SIZE = 22;
	public static final int LEFT_TOOLBAR_LAYERS = 0;
	public static final int LEFT_TOOLBAR_FINDER = 1;

	static void drawCard(Graphics2D graphics, Rectangle bounds)
	{
		graphics.setColor(CARD_BG);
		graphics.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 8, 8);
		graphics.setColor(CARD_EDGE);
		graphics.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 8, 8);
	}

	private MapStyle()
	{
	}
}
