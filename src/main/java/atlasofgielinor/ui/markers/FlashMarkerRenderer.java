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

import static atlasofgielinor.ui.MapStyle.CARD_TITLE;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;

import atlasofgielinor.map.MapCamera;
import net.runelite.api.coords.WorldPoint;

/** Draws the temporary highlight for a selected map destination. */
public class FlashMarkerRenderer
{
	private static final long FLASH_MILLIS = 1600L;
	private static final Stroke ACCENT_RING = new BasicStroke(2.0f);
	private final MapCamera camera;

	public FlashMarkerRenderer(MapCamera camera)
	{
		this.camera = camera;
	}

	/**
	 * Flashes a location when centered on by search or sidebar clicks.
	 */
	public void drawFlash(Graphics2D graphics, Rectangle bounds)
	{
		final WorldPoint flashPoint = camera.getFlashPoint();
		if (flashPoint == null)
		{
			return;
		}

		final long elapsed = System.currentTimeMillis() - camera.getFlashStartMillis();
		if (elapsed > FLASH_MILLIS)
		{
			camera.clearFlash();
			return;
		}

		if (flashPoint.getPlane() != camera.getPlane())
		{
			return;
		}

		final int sx = (int) Math.round(camera.screenX(flashPoint.getX() + 0.5, flashPoint.getY() + 0.5, bounds));
		final int sy = (int) Math.round(camera.screenY(flashPoint.getX() + 0.5, flashPoint.getY() + 0.5, bounds));

		if (!bounds.contains(sx, sy))
		{
			return;
		}

		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		final Stroke oldStroke = graphics.getStroke();
		graphics.setStroke(ACCENT_RING);

		final double totalPhase = elapsed / (double) FLASH_MILLIS;
		final int coreAlpha = Math.max(0, Math.min(255, (int) (200 * (1.0 - totalPhase))));
		if (coreAlpha > 0)
		{
			graphics.setColor(new Color(CARD_TITLE.getRed(), CARD_TITLE.getGreen(), CARD_TITLE.getBlue(), coreAlpha));
			final int coreRadius = 5;
			graphics.drawOval(sx - coreRadius, sy - coreRadius, coreRadius * 2, coreRadius * 2);
		}

		for (int i = 0; i < 3; i++)
		{
			final double phase = elapsed / (double) FLASH_MILLIS - i * 0.22;
			if (phase < 0 || phase > 1)
			{
				continue;
			}
			final int r = (int) Math.round(6 + 34 * phase);
			final int alpha = Math.max(0, Math.min(255, (int) (200 * (1.0 - phase))));
			graphics.setColor(new Color(CARD_TITLE.getRed(), CARD_TITLE.getGreen(), CARD_TITLE.getBlue(), alpha));
			graphics.drawOval(sx - r, sy - r, r * 2, r * 2);
		}

		graphics.setStroke(oldStroke);
	}

}
