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

import com.bettermap.data.MonsterLocationData;
import com.bettermap.data.sailing.SailingPort;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Stroke;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * The colours, fonts, strokes and fixed geometry the world-map renderers share.
 *
 * <p>Split out of {@link BetterWorldMapOverlay} so the draw code reads as draw code; the
 * renderers static-import these, so a constant is named the same everywhere it is used.
 */
final class MapStyle
{

	static final Color TEXT_DIM = new Color(205, 205, 205, 165);
	static final Color TEXT_WARN = new Color(255, 214, 120);
	static final Color CLUE_FILL = new Color(120, 90, 210, 235);
	static final Color CLUE_EDGE = new Color(214, 190, 255, 240);
	/** Quest Helper green, kept clear of the purple the clue markers use. */
	static final Color QUEST_FILL = new Color(70, 175, 105, 235);
	static final Color QUEST_EDGE = new Color(178, 240, 200, 240);
	static final Color CARD_BG = new Color(16, 19, 26, 238);
	static final Color CARD_EDGE = new Color(196, 162, 80, 200);
	static final Color CARD_TITLE = new Color(255, 209, 102);
	static final Color CARD_TEXT = new Color(214, 214, 214);
	static final Color CHIP_BG = new Color(16, 19, 26, 200);
	static final Color DEBUG_KEY = new Color(140, 200, 255);

	static final Font SMALL = new Font("SansSerif", Font.PLAIN, 11);

	/** The card row colours, handed to {@link CardText} so its layout stays free of the overlay. */
	static final CardText.Palette CARD_PALETTE =
		new CardText.Palette(DEBUG_KEY, TEXT_WARN, TEXT_DIM, CARD_TEXT);



	static final Color ROUTE_SHADOW = new Color(18, 18, 18, 200);
	/** Better Map's own computed walking route: cyan, so it never reads as a themed travel-route arc. */
	static final Color ROUTE_LINE = new Color(96, 205, 255, 235);
	/** Same route when the target is unreachable — amber; the line stops at the closest tile. */
	static final Color ROUTE_LINE_PARTIAL = new Color(240, 190, 90, 235);


	/** Enum.values() clones its array on every call; these are read in per-frame loops. */
	static final MonsterLocationData[] BOSSES = MonsterLocationData.values();



	/**
	 * Left toolbar below the status chip: layers gear (slot 0), finder magnifier (slot 1).
	 */
	static final int LEFT_TOOLBAR_BUTTON_SIZE = 22;
	static final int LEFT_TOOLBAR_LAYERS = 0;
	static final int LEFT_TOOLBAR_FINDER = 1;

	private MapStyle()
	{
	}
}
