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
import java.awt.FontMetrics;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Turns the hover card's source lines into the rows that actually get drawn.
 *
 * <p>Card width used to grow to whatever the longest line was, and the longest line in the curated
 * dataset is around 170 characters, so a quest card could be wider than the map. This wraps instead,
 * and caps how tall a card can get.
 *
 * <p>It lives apart from {@link BetterWorldMapOverlay} because the overlay needs a {@code Client} to
 * construct, which makes anything inside it untestable. Everything here is static and takes a
 * {@link FontMetrics}, so a test can measure text with a throwaway image and no client at all.
 */
final class CardText
{
	/** Widest a card may get before its lines start wrapping. */
	static final int MAX_WIDTH_PX = 340;

	/**
	 * Most rows a card may draw, including the chip and the trailing coordinate line.
	 *
	 * <p>The travel card is the tallest one in the plugin - a chip, up to 14 destinations and an
	 * overflow line, so 16 rows before wrapping. This has to stay above that or hovering a charter
	 * ship would start losing destinations.
	 */
	static final int MAX_LINES = 18;

	/** Most rows one source line may wrap into, so a single bad field cannot fill the card. */
	static final int MAX_FRAGMENTS = 4;

	/** Indents wrapped rows so they read as continuations rather than new lines. */
	private static final String CONTINUATION = "  ";

	private static final String ELLIPSIS = "\u2026";

	private CardText()
	{
	}

	/** The four colours the overlay draws card rows in. */
	static final class Palette
	{
		private final Color chip;
		private final Color warn;
		private final Color dim;
		private final Color normal;

		Palette(Color chip, Color warn, Color dim, Color normal)
		{
			this.chip = chip;
			this.warn = warn;
			this.dim = dim;
			this.normal = normal;
		}
	}

	/** One drawable row: text that already fits, and the colour it inherited. */
	static final class Row
	{
		private final String text;
		private final Color colour;

		Row(String text, Color colour)
		{
			this.text = text;
			this.colour = colour;
		}

		String getText()
		{
			return text;
		}

		Color getColour()
		{
			return colour;
		}
	}

	/**
	 * The colour a source line is drawn in.
	 *
	 * <p>Deliberately unchanged from the overlay's original chain, including how loose the "req"
	 * test is - it is meant to catch "requires" and "Requirements" as well. Tightening it would
	 * quietly restyle the quest cards.
	 */
	static Color colourFor(String line, Palette palette)
	{
		if (line.startsWith("["))
		{
			return palette.chip;
		}
		final String lower = line.toLowerCase(Locale.ROOT);
		if (lower.contains("req") || lower.contains("require") || line.contains("\u2694") || lower.startsWith("kill ") || line.contains("\u26A0"))
		{
			return palette.warn;
		}
		if (line.endsWith(")"))
		{
			return palette.dim;
		}
		return palette.normal;
	}

	/**
	 * Splits one line into rows that each fit inside {@code maxWidth}.
	 *
	 * <p>Breaks on spaces. A single word too long to fit is cut with an ellipsis rather than
	 * allowed to widen the card, and anything past {@code maxFragments} rows is dropped with an
	 * ellipsis on the last row kept.
	 */
	static List<String> wrap(FontMetrics metrics, String text, int maxWidth, int maxFragments)
	{
		if (text == null || text.isEmpty())
		{
			return Collections.emptyList();
		}

		if (metrics.stringWidth(text) <= maxWidth)
		{
			return Collections.singletonList(text);
		}

		final List<String> fragments = new ArrayList<>(maxFragments);
		final String[] words = text.split(" ");

		StringBuilder current = new StringBuilder();
		for (int i = 0; i < words.length; i++)
		{
			final String word = words[i];
			final String prefix = fragments.isEmpty() ? "" : CONTINUATION;
			final String candidate = current.length() == 0
				? prefix + word
				: current + " " + word;

			if (metrics.stringWidth(candidate) <= maxWidth)
			{
				current.setLength(0);
				current.append(candidate);
				continue;
			}

			// The word does not fit on the current row. Flush what we have, then either start a
			// new row with it or - if it cannot fit on a row of its own - cut it short.
			if (current.length() > 0)
			{
				fragments.add(current.toString());
				current.setLength(0);

				if (fragments.size() >= maxFragments)
				{
					return withEllipsis(fragments);
				}
			}

			final String ownRow = (fragments.isEmpty() ? "" : CONTINUATION) + word;
			if (metrics.stringWidth(ownRow) <= maxWidth)
			{
				current.append(ownRow);
			}
			else
			{
				fragments.add(hardCut(metrics, ownRow, maxWidth));
				if (fragments.size() >= maxFragments)
				{
					return fragments;
				}
			}
		}

		if (current.length() > 0)
		{
			fragments.add(current.toString());
		}

		return fragments;
	}

	/**
	 * Wraps every source line and caps the card's height.
	 *
	 * <p>The colour is resolved once per source line and every row it wraps into inherits it. That
	 * is the whole reason this is a two-step loop: re-testing a wrapped fragment would let a
	 * continuation that happens to end in ")" turn dim, or one starting "[" turn into a chip.
	 */
	static List<Row> layout(FontMetrics metrics, List<String> lines, Palette palette)
	{
		if (lines == null || lines.isEmpty())
		{
			return Collections.emptyList();
		}

		final List<Row> rows = new ArrayList<>(lines.size());
		final List<Row> lastLineRows = new ArrayList<>(MAX_FRAGMENTS);

		for (int i = 0; i < lines.size(); i++)
		{
			final String source = lines.get(i);
			if (source == null || source.trim().isEmpty())
			{
				continue;
			}

			final Color colour = colourFor(source, palette);
			final boolean isLast = i == lines.size() - 1;

			for (String fragment : wrap(metrics, source, MAX_WIDTH_PX, MAX_FRAGMENTS))
			{
				final Row row = new Row(fragment, colour);
				rows.add(row);
				if (isLast && lastLineRows.isEmpty())
				{
					lastLineRows.add(row);
				}
			}
		}

		if (rows.size() <= MAX_LINES)
		{
			return rows;
		}

		// Keep the top of the card, say how much was dropped, then keep the last source line's
		// first row - on the main path that is the coordinates, and on the underground path it is
		// the "click to open" hint. Both are worth more than the rows in between.
		final int kept = MAX_LINES - 2;
		final List<Row> capped = new ArrayList<>(MAX_LINES);
		capped.addAll(rows.subList(0, kept));

		final Row tail = lastLineRows.isEmpty() ? null : lastLineRows.get(0);
		final int dropped = rows.size() - kept - (tail == null ? 0 : 1);
		if (dropped > 0)
		{
			capped.add(new Row("+" + dropped + " more", palette.dim));
		}
		if (tail != null)
		{
			capped.add(tail);
		}

		return capped;
	}

	/** Trims a word until it fits, leaving an ellipsis to show it was cut. */
	private static String hardCut(FontMetrics metrics, String word, int maxWidth)
	{
		String cut = word;
		while (cut.length() > 1 && metrics.stringWidth(cut + ELLIPSIS) > maxWidth)
		{
			cut = cut.substring(0, cut.length() - 1);
		}
		return cut + ELLIPSIS;
	}

	/** Marks the last kept fragment as truncated. */
	private static List<String> withEllipsis(List<String> fragments)
	{
		final int last = fragments.size() - 1;
		fragments.set(last, fragments.get(last) + ELLIPSIS);
		return fragments;
	}
}
