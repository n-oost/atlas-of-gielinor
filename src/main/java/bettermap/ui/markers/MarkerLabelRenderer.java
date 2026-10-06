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
package bettermap.ui.markers;

import static bettermap.ui.MapStyle.CARD_BG;
import static bettermap.ui.MapStyle.SMALL;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;

/** Draws centered marker labels with the caller's geometry and border style. */
final class MarkerLabelRenderer
{
	private MarkerLabelRenderer()
	{
	}

	/** Leaves font, color, and stroke as the original label passes did; preserves the current clip. */
	static void draw(Graphics2D graphics, Rectangle bounds, String label, int centerX, int baselineY,
		int padding, int topOffset, int height, Color border, Color text, Stroke outline)
	{
		graphics.setFont(SMALL);
		final int textWidth = graphics.getFontMetrics().stringWidth(label);
		final int textX = centerX - textWidth / 2;
		final Rectangle rect = new Rectangle(textX - padding, baselineY - topOffset,
			textWidth + padding * 2, height);
		if (bounds.intersects(rect))
		{
			graphics.setColor(CARD_BG);
			graphics.fillRoundRect(rect.x, rect.y, rect.width, rect.height, padding, padding);
			graphics.setColor(border);
			graphics.setStroke(outline);
			graphics.drawRoundRect(rect.x, rect.y, rect.width, rect.height, padding, padding);
			graphics.setColor(text);
			graphics.drawString(label, textX, baselineY);
		}
	}
}
