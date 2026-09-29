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
package com.bettermap.ui.markers;

import static com.bettermap.ui.MapStyle.CARD_BG;
import static com.bettermap.ui.MapStyle.CARD_TEXT;
import static com.bettermap.ui.MapStyle.ROUTE_SHADOW;
import static com.bettermap.ui.MapStyle.SMALL;

import com.bettermap.BetterMapConfig;
import com.bettermap.data.TravelData;
import com.bettermap.map.MapCamera;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.QuadCurve2D;
import java.util.ArrayList;
import java.util.List;
import net.runelite.api.coords.WorldPoint;

/**
 * Renders travel routes, animated dashed paths between transport hops,
 * destination beacons, and station pin markers.
 */
public class RouteMarkerRenderer
{
	private static final Color ROUTE_OUTLINE = new Color(18, 18, 18);
	private static final Stroke MARKER_OUTLINE = new BasicStroke(1.0f);
	private static final Stroke LABEL_BORDER = new BasicStroke(1.2f);

	/** Route polylines are drawn round-capped so the joins between hops do not notch. */
	public static final Stroke ROUTE_LINE_STROKE =
		new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	public static final Stroke ROUTE_HALO_STROKE =
		new BasicStroke(5.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

	/** Dash phases for animated travel routes — one stroke per phase, no per-frame allocation. */
	public static final int TRAVEL_DASH_PHASES = 14;
	public static final Stroke[] TRAVEL_DASH_STROKES = buildTravelDashStrokes();

	public static Stroke[] buildTravelDashStrokes()
	{
		final Stroke[] strokes = new Stroke[TRAVEL_DASH_PHASES];
		for (int i = 0; i < TRAVEL_DASH_PHASES; i++)
		{
			strokes[i] = new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f,
				new float[]{ 8f, 6f }, i);
		}
		return strokes;
	}

	private final BetterMapConfig config;
	private final MapCamera camera;

	public RouteMarkerRenderer(BetterMapConfig config, MapCamera camera)
	{
		this.config = config;
		this.camera = camera;
	}

	/**
	 * Small coloured pins at every travel station so routes are discoverable before hover.
	 */
	public void drawTravelStations(Graphics2D graphics, Rectangle bounds)
	{
		if (!config.showTravelRoutes())
		{
			return;
		}

		final int plane = camera.getPlane();
		final ViewWindow view = ViewWindow.from(camera, bounds, 5.0);

		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		for (TravelData.TravelNode node : TravelData.ALL_NODES)
		{
			final WorldPoint loc = node.getLocation();
			if (!TravelData.matchesPlane(node, plane))
			{
				continue;
			}

			final int mx = loc.getX();
			final int my = loc.getY();
			if (!camera.inView(mx, my, view.minX, view.maxX, view.minY, view.maxY)
				|| !ViewWindow.isDrawable(camera, mx, my))
			{
				continue;
			}

			final int x = (int) camera.screenX(mx + 0.5, my + 0.5, bounds);
			final int y = (int) camera.screenY(mx + 0.5, my + 0.5, bounds);
			if (!bounds.contains(x, y))
			{
				continue;
			}

			final Color theme = node.getType().getPinColor();
			graphics.setColor(theme);
			graphics.fillOval(x - 4, y - 4, 9, 9);
			graphics.setColor(ROUTE_SHADOW);
			graphics.drawOval(x - 4, y - 4, 9, 9);
		}
	}

	/**
	 * Draws route lines, destination beacons, and price badges for the currently hovered travel station.
	 */
	public void drawTravelRoutes(Graphics2D graphics, Rectangle bounds)
	{
		if (!config.showTravelRoutes() || camera.getHoveredTravelNode() == null)
		{
			return;
		}

		final TravelData.TravelNode node = camera.getHoveredTravelNode();
		final TravelData.TravelType type = node.getType();
		final Color theme = type.getPrimaryColor();
		final Color highlight = type.getHighlightColor();

		final Stroke origStroke = graphics.getStroke();
		final Color origColor = graphics.getColor();
		final Font origFont = graphics.getFont();
		final java.awt.Composite origComp = graphics.getComposite();

		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		final WorldPoint originLoc = node.getLocation();
		final double ox = camera.screenX(originLoc.getX() + 0.5, originLoc.getY() + 0.5, bounds);
		final double oy = camera.screenY(originLoc.getX() + 0.5, originLoc.getY() + 0.5, bounds);

		final int margin = 400;
		final Rectangle extendedBounds = new Rectangle(
			bounds.x - margin, bounds.y - margin,
			bounds.width + margin * 2, bounds.height + margin * 2);

		final boolean animate = config.animateTravelRoutes();
		final Stroke haloStroke = ROUTE_HALO_STROKE;
		final Color haloColor = new Color(theme.getRed(), theme.getGreen(), theme.getBlue(), 60);
		final Stroke lineStroke = animate
			? TRAVEL_DASH_STROKES[(int) ((System.currentTimeMillis() / 40L) % TRAVEL_DASH_PHASES)]
			: ROUTE_LINE_STROKE;

		final float pulse = animate
			? (float) (0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 180.0))
			: 0.5f;

		// 1. Draw route arcs
		for (TravelData.TravelDestination dest : node.getDestinations())
		{
			final WorldPoint destLoc = dest.getLocation();
			if (TravelData.sameLocation(originLoc, destLoc) || hiddenTravelPoint(destLoc))
			{
				continue;
			}

			final double dx = camera.screenX(destLoc.getX() + 0.5, destLoc.getY() + 0.5, bounds);
			final double dy = camera.screenY(destLoc.getX() + 0.5, destLoc.getY() + 0.5, bounds);

			final double midX = (ox + dx) / 2.0;
			final double midY = (oy + dy) / 2.0;
			final double vx = dx - ox;
			final double vy = dy - oy;
			final double cx = midX - vy * 0.18;
			final double cy = midY + vx * 0.18;

			final QuadCurve2D.Double curve = new QuadCurve2D.Double(ox, oy, cx, cy, dx, dy);

			if (!extendedBounds.contains(ox, oy)
				&& !extendedBounds.contains(dx, dy)
				&& !extendedBounds.intersectsLine(ox, oy, dx, dy)
				&& !curve.intersects(extendedBounds.x, extendedBounds.y, extendedBounds.width, extendedBounds.height))
			{
				continue;
			}

			// Soft halo
			graphics.setColor(haloColor);
			graphics.setStroke(haloStroke);
			graphics.draw(curve);

			// Route line
			graphics.setColor(highlight);
			graphics.setStroke(lineStroke);
			graphics.draw(curve);
		}

		// 2. Draw destination beacons
		final float r1 = 6.0f + 4.0f * pulse;
		final float r2 = 9.0f + 5.0f * pulse;
		final int a1 = Math.max(10, Math.min(255, (int) (140 - 50 * pulse)));
		final int a2 = Math.max(10, Math.min(255, (int) (80 - 45 * pulse)));

		for (TravelData.TravelDestination dest : node.getDestinations())
		{
			final WorldPoint destLoc = dest.getLocation();
			if (TravelData.sameLocation(originLoc, destLoc) || hiddenTravelPoint(destLoc))
			{
				continue;
			}

			final double dx = camera.screenX(destLoc.getX() + 0.5, destLoc.getY() + 0.5, bounds);
			final double dy = camera.screenY(destLoc.getX() + 0.5, destLoc.getY() + 0.5, bounds);

			if (!extendedBounds.contains(dx, dy))
			{
				continue;
			}

			// Expanding rings
			graphics.setStroke(LABEL_BORDER);
			graphics.setColor(new Color(theme.getRed(), theme.getGreen(), theme.getBlue(), a1));
			graphics.draw(new Ellipse2D.Double(dx - r1, dy - r1, 2 * r1, 2 * r1));
			graphics.setColor(new Color(theme.getRed(), theme.getGreen(), theme.getBlue(), a2));
			graphics.draw(new Ellipse2D.Double(dx - r2, dy - r2, 2 * r2, 2 * r2));

			// Centre pin
			final int px = (int) Math.round(dx);
			final int py = (int) Math.round(dy);
			graphics.setColor(highlight);
			graphics.fillOval(px - 4, py - 4, 8, 8);
			graphics.setColor(ROUTE_OUTLINE);
			graphics.setStroke(MARKER_OUTLINE);
			graphics.drawOval(px - 4, py - 4, 8, 8);
		}

		// 3. Draw price badges
		if (config.showTravelPriceTags())
		{
			graphics.setFont(SMALL);
			final FontMetrics fm = graphics.getFontMetrics();
			final List<Rectangle> placedBadges = new ArrayList<>();

			for (TravelData.TravelDestination dest : node.getDestinations())
			{
				final WorldPoint destLoc = dest.getLocation();
				if (TravelData.sameLocation(originLoc, destLoc) || hiddenTravelPoint(destLoc))
				{
					continue;
				}

				final double dx = camera.screenX(destLoc.getX() + 0.5, destLoc.getY() + 0.5, bounds);
				final double dy = camera.screenY(destLoc.getX() + 0.5, destLoc.getY() + 0.5, bounds);

				final double midX = (ox + dx) / 2.0;
				final double midY = (oy + dy) / 2.0;
				final double vx = dx - ox;
				final double vy = dy - oy;
				final double cx = midX - vy * 0.18;
				final double cy = midY + vx * 0.18;
				final QuadCurve2D.Double curve = new QuadCurve2D.Double(ox, oy, cx, cy, dx, dy);

				if (!extendedBounds.contains(ox, oy)
					&& !extendedBounds.contains(dx, dy)
					&& !extendedBounds.intersectsLine(ox, oy, dx, dy)
					&& !curve.intersects(extendedBounds.x, extendedBounds.y, extendedBounds.width, extendedBounds.height))
				{
					continue;
				}

				final int px = (int) Math.round(dx);
				final int py = (int) Math.round(dy);

				if (!bounds.contains(px, py) && !bounds.intersects(new Rectangle(px - 15, py - 15, 30, 30)))
				{
					continue;
				}

				final String cost = dest.getCost() != null ? dest.getCost() : "";
				final String badgeText = cost.isEmpty() ? dest.getName() : dest.getName() + " \u2022 " + cost;
				final int textW = fm.stringWidth(badgeText);
				final int padX = 6;
				final int padY = 2;
				final int pillW = textW + padX * 2;
				final int pillH = fm.getHeight() + padY * 2;

				int pillX = px + 10;
				int pillY = py - 4 - pillH / 2;

				if (pillX + pillW > bounds.getMaxX() - 4)
				{
					pillX = px - 10 - pillW;
				}
				if (pillX < bounds.getMinX() + 4)
				{
					pillX = (int) bounds.getMinX() + 4;
				}
				if (pillY < bounds.getMinY() + 4)
				{
					pillY = (int) bounds.getMinY() + 4;
				}
				else if (pillY + pillH > bounds.getMaxY() - 4)
				{
					pillY = (int) bounds.getMaxY() - pillH - 4;
				}

				final Rectangle pillRect = new Rectangle(pillX, pillY, pillW, pillH);
				if (ViewWindow.overlapsPlaced(placedBadges, pillRect))
				{
					continue;
				}

				placedBadges.add(pillRect);

				graphics.setColor(CARD_BG);
				graphics.fillRoundRect(pillRect.x, pillRect.y, pillRect.width, pillRect.height, 6, 6);
				graphics.setColor(theme);
				graphics.setStroke(MARKER_OUTLINE);
				graphics.drawRoundRect(pillRect.x, pillRect.y, pillRect.width, pillRect.height, 6, 6);
				graphics.setColor(CARD_TEXT);
				graphics.drawString(badgeText, pillRect.x + padX, pillRect.y + padY + fm.getAscent());
			}
		}

		graphics.setStroke(origStroke);
		graphics.setColor(origColor);
		graphics.setFont(origFont);
		graphics.setComposite(origComp);
	}

	private boolean hiddenTravelPoint(WorldPoint loc)
	{
		return loc == null || !ViewWindow.isDrawable(camera, loc.getX(), loc.getY());
	}
}
