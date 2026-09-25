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

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Layout is measured against real {@link FontMetrics} from a throwaway image, which works headless
 * and needs no client - the reason the layout was pulled out of the overlay in the first place.
 */
public class CardTextTest
{
	private static final Color CHIP = new Color(140, 200, 255);
	private static final Color WARN = new Color(255, 214, 120);
	private static final Color DIM = new Color(205, 205, 205, 165);
	private static final Color NORMAL = new Color(214, 214, 214);

	private static final CardText.Palette PALETTE = new CardText.Palette(CHIP, WARN, DIM, NORMAL);

	private static FontMetrics metrics;

	@BeforeClass
	public static void measureWithoutAClient()
	{
		metrics = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
			.createGraphics()
			.getFontMetrics(new Font("SansSerif", Font.PLAIN, 11));
	}

	/** The longest line in the curated dataset is the sort of thing that used to widen the card. */
	private static final String LONG_REQUIREMENT =
		"Requirements: 75 Magic, 75 Firemaking, 62 Thieving, 60 Herblore, 60 Runecraft, "
			+ "55 Construction, 54 Slayer, and completion of Desert Treasure I, Secrets of the North, "
			+ "Temple of the Eye, Enakhra's Lament and Fairytale II";

	@Test
	public void longRequirementLineWrapsAndEveryRowKeepsTheWarningColour()
	{
		final List<CardText.Row> rows = CardText.layout(metrics, Arrays.asList(LONG_REQUIREMENT), PALETTE);

		assertTrue("a 170+ char line should wrap to several rows, got " + rows.size(), rows.size() >= 2);
		for (CardText.Row row : rows)
		{
			assertEquals("every wrapped row inherits the source line's colour", WARN, row.getColour());
		}
	}

	@Test
	public void wrappedContinuationDoesNotTurnDimJustBecauseItEndsWithABracket()
	{
		// Plenty of bracketed groups, so at least one row boundary lands right after a ")".
		// The source itself is plain text, so every row must stay plain.
		final String source = "Sells bronze (10) iron (5) steel (3) mithril (2) adamant (1) rune (1) "
			+ "dragon (1) crystal (1) barrows (1) and assorted spare parts for adventurers";
		final List<CardText.Row> rows = CardText.layout(metrics, Arrays.asList(source), PALETTE);

		assertTrue(rows.size() >= 2);
		for (CardText.Row row : rows)
		{
			assertEquals("a continuation ending in ')' must not become dim", NORMAL, row.getColour());
		}
	}

	@Test
	public void wrappedContinuationDoesNotTurnIntoAChip()
	{
		final String source = "Stock includes [members only] items plus a long tail of ordinary goods "
			+ "that pushes this line well past the maximum card width so that it has to wrap";
		final List<CardText.Row> rows = CardText.layout(metrics, Arrays.asList(source), PALETTE);

		assertTrue(rows.size() >= 2);
		for (CardText.Row row : rows)
		{
			assertFalse("no continuation row may become a chip", CHIP.equals(row.getColour()));
		}
	}

	@Test
	public void chipLineStaysBlueAcrossFragments()
	{
		final String chip = "[Shops • Civitas illa Fortis, Varlamore, on the upper floor of the "
			+ "Sunrise Palace bazaar annex]";
		final List<CardText.Row> rows = CardText.layout(metrics, Arrays.asList(chip), PALETTE);

		assertTrue(rows.size() >= 2);
		for (CardText.Row row : rows)
		{
			assertEquals(CHIP, row.getColour());
		}
	}

	@Test
	public void aSingleWordLongerThanTheCardIsHardCutWithAnEllipsis()
	{
		final StringBuilder word = new StringBuilder();
		for (int i = 0; i < 200; i++)
		{
			word.append('W');
		}

		final List<String> fragments =
			CardText.wrap(metrics, word.toString(), CardText.MAX_WIDTH_PX, CardText.MAX_FRAGMENTS);

		assertEquals("an unbreakable word cannot wrap, so it is cut", 1, fragments.size());
		assertTrue("the cut is signalled", fragments.get(0).endsWith("…"));
		assertTrue("and it fits", metrics.stringWidth(fragments.get(0)) <= CardText.MAX_WIDTH_PX);
	}

	@Test
	public void lineCapKeepsTheFinalLineAndReportsTheOverflow()
	{
		final List<String> lines = new ArrayList<>();
		for (int i = 0; i < 40; i++)
		{
			lines.add("filler line " + i);
		}
		final String coords = "3231, 3203 (Floor 0)";
		lines.add(coords);

		final List<CardText.Row> rows = CardText.layout(metrics, lines, PALETTE);

		assertTrue("the card is capped, got " + rows.size(), rows.size() <= CardText.MAX_LINES);
		assertEquals("the last source line survives truncation", coords, rows.get(rows.size() - 1).getText());

		boolean sawOverflow = false;
		for (CardText.Row row : rows)
		{
			if (row.getText().startsWith("+") && row.getText().endsWith("more"))
			{
				sawOverflow = true;
			}
		}
		assertTrue("the dropped rows are accounted for", sawOverflow);
	}

	@Test
	public void aSixteenLineTravelCardIsNotTruncated()
	{
		// Mirrors the live travel card: a chip, 14 destinations and an overflow line. If the cap
		// ever drops below this, hovering a charter ship silently loses destinations.
		final List<String> lines = new ArrayList<>();
		lines.add("[Charter Ship • Port Sarim Charter]");
		for (int i = 0; i < 14; i++)
		{
			lines.add("→ Port " + i + ": 1,600 gp");
		}
		lines.add("... +3 more");

		final List<CardText.Row> rows = CardText.layout(metrics, lines, PALETTE);

		assertEquals("a 16 line travel card must render whole", 16, rows.size());
		assertEquals(CHIP, rows.get(0).getColour());
	}

	@Test
	public void noRowEverExceedsTheMaximumWidth()
	{
		final List<String> lines = Arrays.asList(
			LONG_REQUIREMENT,
			"[Shops • Lumbridge]",
			"Notable: Culinaromancer's gloves 10, Barrows gloves, Manta ray, Shark, Sea turtle",
			"Best source: Cooked chicken (1000)",
			"3231, 3203 (Floor 0)");

		for (CardText.Row row : CardText.layout(metrics, lines, PALETTE))
		{
			assertTrue("row wider than the card: " + row.getText(),
				metrics.stringWidth(row.getText()) <= CardText.MAX_WIDTH_PX);
		}
	}

	@Test
	public void blankAndNullSourceLinesAreDropped()
	{
		final List<String> lines = Arrays.asList("Real line", null, "   ", "", "Another real line");
		final List<CardText.Row> rows = CardText.layout(metrics, lines, PALETTE);

		assertEquals(2, rows.size());
	}

	@Test
	public void testNoticeBoardCardLayoutFitsWithinLimit()
	{
		final List<String> lines = new ArrayList<>();
		lines.add("[Port tasks • sample]");
		lines.add("Courier tasks (sample):");
		lines.add("  • → Musa Point (Rum shipment)");
		lines.add("  • → Entrana (Timber & building supplies)");
		lines.add("  • → Port Khazard (Iron & steel ingots)");
		lines.add("  • → Catherby (Farming produce & seeds)");
		lines.add("Bounty tasks (sample):");
		lines.add("  • Sea Serpents (Lvl 30)");
		lines.add("  • Stray Corsair Pirates (Lvl 30)");
		lines.add("  • Reef Sharks (Lvl 35)");
		lines.add("Boards rotate after 8 tasks or daily reset");
		lines.add("3045, 3205 (Floor 0)");

		final List<CardText.Row> rows = CardText.layout(metrics, lines, PALETTE);
		assertTrue("Notice board card should fit within MAX_LINES", rows.size() <= CardText.MAX_LINES);
		assertEquals(CHIP, rows.get(0).getColour());
		assertEquals("3045, 3205 (Floor 0)", rows.get(rows.size() - 1).getText());
		assertEquals(DIM, rows.get(rows.size() - 1).getColour());
	}

	@Test
	public void testSailingPortCardLayout()
	{
		final List<String> lines = new ArrayList<>();
		lines.add("[Sailing Dock • Level 30 Sailing]");
		lines.add("Required: Level 30 Sailing");
		lines.add("Docked boats (2): ⛵ Sea Breeze, ⛵ Wave Rider");
		lines.add("2685, 3161 (Floor 0)");

		final List<CardText.Row> rows = CardText.layout(metrics, lines, PALETTE);
		assertTrue("Port card should fit within MAX_LINES", rows.size() <= CardText.MAX_LINES);
		assertEquals(CHIP, rows.get(0).getColour());
		assertEquals(WARN, rows.get(1).getColour());
		assertEquals("2685, 3161 (Floor 0)", rows.get(rows.size() - 1).getText());
	}

	@Test
	public void testClueCardLayoutAndEnemyWarningColor()
	{
		final List<String> lines = new ArrayList<>();
		lines.add("[Clue Scroll Target]");
		lines.add("Emotes:");
		lines.add("Think");
		lines.add("Spin");
		lines.add("STASH Unit: \u2713");
		lines.add("Equip:");
		lines.add("Mithril chainbody \u2713");
		lines.add("\u2694 Kill the Double Agent (lvl 65)");
		lines.add("Spade required");
		lines.add("3200, 3200 (Floor 0)");

		final List<CardText.Row> rows = CardText.layout(metrics, lines, PALETTE);
		assertEquals(CHIP, rows.get(0).getColour());

		CardText.Row enemyRow = null;
		for (CardText.Row row : rows)
		{
			if (row.getText().contains("Double Agent"))
			{
				enemyRow = row;
				break;
			}
		}
		assertNotNull(enemyRow);
		assertEquals(WARN, enemyRow.getColour());

		CardText.Row reqRow = null;
		for (CardText.Row row : rows)
		{
			if (row.getText().contains("Spade"))
			{
				reqRow = row;
				break;
			}
		}
		assertNotNull(reqRow);
		assertEquals(WARN, reqRow.getColour());
	}
}
