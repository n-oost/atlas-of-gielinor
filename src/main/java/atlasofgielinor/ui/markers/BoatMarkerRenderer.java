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
import static atlasofgielinor.ui.MapStyle.SMALL;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;

import atlasofgielinor.AtlasOfGielinorConfig;
import atlasofgielinor.data.sailing.BoatTracker;
import atlasofgielinor.data.sailing.BoatType;
import atlasofgielinor.data.sailing.PlayerBoat;
import atlasofgielinor.data.sailing.PortNoticeBoard;
import atlasofgielinor.data.sailing.SailingPort;
import atlasofgielinor.map.MapCamera;
import atlasofgielinor.map.catalog.PoiIndex;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.FontManager;

/** Draws sailing ports, notice boards, and owned boats. */
@Slf4j
public class BoatMarkerRenderer
{
	private final Set<Object> visibleTooltipTargets;

	private static final Color BOAT_EDGE = new Color(14, 18, 26, 220);
	private static final Color BOAT_FILL = new Color(38, 128, 205);
	private static final Color BOAT_BORDER = new Color(255, 214, 110);
	private static final Color BOAT_HULL_GOOD = new Color(55, 215, 115);
	private static final Color BOAT_HULL_WARN = new Color(245, 185, 55);
	private static final Color BOAT_HULL_BAD = new Color(235, 65, 65);
	private static final Color PORT_EDGE = new Color(14, 20, 28, 220);
	private static final Color PORT_FILL = new Color(32, 95, 145, 200);
	private static final Color PORT_BORDER = new Color(130, 210, 255, 230);
	private static final Color PORT_DOT = new Color(220, 245, 255);
	private static final Color BOARD_EDGE = new Color(24, 18, 12, 220);
	private static final Color BOARD_FILL = new Color(175, 115, 45, 220);
	private static final Color BOARD_BORDER = new Color(255, 225, 140, 240);
	private static final Color BOARD_GLYPH = new Color(255, 245, 220);
	private static final Font TINY = FontManager.getDefaultBoldFont().deriveFont(9f);
	private static final SailingPort[] PORTS = SailingPort.values();

	/**
	 * Outline weights, thinnest to thickest. BasicStroke is immutable, so one instance each.
	 * Each renderer keeps its immutable outline strokes for reuse between frames.
	 */
	private static final Stroke MARKER_OUTLINE = new BasicStroke(1.0f);
	private static final Stroke LABEL_BORDER = new BasicStroke(1.2f);
	private static final Stroke MARKER_RING = new BasicStroke(1.5f);
	private final AtlasOfGielinorConfig config;
	private final MapCamera camera;
	private final PoiIndex poiIndex;
	private final BoatTracker boatTracker;
	private BufferedImage boatSloopIcon;
	private BufferedImage boatSkiffIcon;
	private BufferedImage boatRaftIcon;

	public BoatMarkerRenderer(
		AtlasOfGielinorConfig config,
		MapCamera camera,
		PoiIndex poiIndex,
		BoatTracker boatTracker,
		Set<Object> visibleTooltipTargets)
	{
		this.config = config;
		this.camera = camera;
		this.poiIndex = poiIndex;
		this.boatTracker = boatTracker;
		this.visibleTooltipTargets = visibleTooltipTargets;
	}

	private BufferedImage boatIcon(BoatType type)
	{
		if (type == BoatType.RAFT)
		{
			if (boatRaftIcon == null)
			{
				boatRaftIcon = loadBoatIcon("boat_raft");
			}
			return boatRaftIcon;
		}
		if (type == BoatType.SKIFF || type == BoatType.TUTORIAL)
		{
			if (boatSkiffIcon == null)
			{
				boatSkiffIcon = loadBoatIcon("boat_skiff");
			}
			return boatSkiffIcon;
		}
		if (boatSloopIcon == null)
		{
			boatSloopIcon = loadBoatIcon("boat_sloop");
			if (boatSloopIcon == null)
			{
				boatSloopIcon = loadBoatIcon("boat");
			}
		}
		return boatSloopIcon;
	}

	private BufferedImage loadBoatIcon(String name)
	{
		BufferedImage icon = null;
		if (poiIndex != null)
		{
			icon = poiIndex.icon(name);
		}
		if (icon == null)
		{
			try (InputStream in = BoatMarkerRenderer.class.getResourceAsStream("/atlasofgielinor/" + name + ".png"))
			{
				if (in != null)
				{
					icon = ImageIO.read(in);
				}
			}
			catch (IOException e)
			{
				log.warn("Failed to load boat icon: {}", name, e);
			}
		}
		return icon;
	}

	/**
	 * Subtle nautical pins for all Sailing ports across Gielinor.
	 */
	public void drawSailingPorts(Graphics2D graphics, Rectangle bounds)
	{
		if (!camera.isTravelViewActive() && (!config.showSailingPorts() || camera.getZoom() < config.sailingPortMinZoom()))
		{
			return;
		}

		final int plane = camera.getPlane();
		if (plane != 0)
		{
			return;
		}

		final ViewWindow view = ViewWindow.from(camera, bounds, 10.0);

		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		final Stroke oldStroke = graphics.getStroke();

		for (SailingPort port : PORTS)
		{
			final WorldPoint loc = port.getNavigationLocation();
			if (loc == null || loc.getPlane() != plane)
			{
				continue;
			}

			final int px = loc.getX();
			final int py = loc.getY();
			if (!camera.inView(px, py, view.minX, view.maxX, view.minY, view.maxY) || !ViewWindow.isDrawable(camera, px, py))
			{
				continue;
			}

			final int sx = (int) Math.round(camera.screenX(px + 0.5, py + 0.5, bounds));
			final int sy = (int) Math.round(camera.screenY(px + 0.5, py + 0.5, bounds));

			final int size = 12;
			final Rectangle rect = new Rectangle(sx - size / 2, sy - size / 2, size, size);
			if (!bounds.intersects(rect))
			{
				continue;
			}

			final boolean hasDockedBoat = boatTracker != null && !boatTracker.getBoatsAt(port).isEmpty() && config.showBoatLocations()
				&& camera.getZoom() >= config.playerBoatMinZoom();
			if (!hasDockedBoat)
			{
				graphics.setColor(PORT_EDGE);
				graphics.fillOval(rect.x - 1, rect.y - 1, rect.width + 2, rect.height + 2);

				graphics.setColor(PORT_FILL);
				graphics.fillOval(rect.x, rect.y, rect.width, rect.height);

				graphics.setColor(PORT_BORDER);
				graphics.setStroke(LABEL_BORDER);
				graphics.drawOval(rect.x, rect.y, rect.width, rect.height);

				graphics.setColor(PORT_DOT);
				graphics.fillOval(sx - 2, sy - 2, 4, 4);

				visibleTooltipTargets.add(port);
			}
		}

		graphics.setStroke(oldStroke);
	}

	/**
	 * Distinct port notice board markers at the configured minimum zoom.
	 */
	public void drawPortNoticeBoards(Graphics2D graphics, Rectangle bounds)
	{
		if (!config.showPortNoticeBoards() || camera.getZoom() < config.portNoticeBoardMinZoom())
		{
			return;
		}

		final int plane = camera.getPlane();
		if (plane != 0)
		{
			return;
		}

		final ViewWindow view = ViewWindow.from(camera, bounds, 10.0);

		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		final Stroke oldStroke = graphics.getStroke();

		for (PortNoticeBoard board : PortNoticeBoard.ALL_BOARDS)
		{
			final WorldPoint loc = board.getLocation();
			if (loc == null || loc.getPlane() != plane)
			{
				continue;
			}

			final int bx = loc.getX();
			final int by = loc.getY();
			if (!camera.inView(bx, by, view.minX, view.maxX, view.minY, view.maxY) || !ViewWindow.isDrawable(camera, bx, by))
			{
				continue;
			}

			final int sx = (int) Math.round(camera.screenX(bx + 0.5, by + 0.5, bounds));
			final int sy = (int) Math.round(camera.screenY(bx + 0.5, by + 0.5, bounds));

			final int size = 12;
			final Rectangle rect = new Rectangle(sx - size / 2, sy - size / 2, size, size);
			if (!bounds.intersects(rect))
			{
				continue;
			}

			graphics.setColor(BOARD_EDGE);
			graphics.fillRoundRect(rect.x - 1, rect.y - 1, rect.width + 2, rect.height + 2, 4, 4);

			graphics.setColor(BOARD_FILL);
			graphics.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 4, 4);

			graphics.setColor(BOARD_BORDER);
			graphics.setStroke(LABEL_BORDER);
			graphics.drawRoundRect(rect.x, rect.y, rect.width, rect.height, 4, 4);

			graphics.setColor(BOARD_GLYPH);
			graphics.drawLine(sx - 3, sy - 2, sx + 3, sy - 2);
			graphics.drawLine(sx - 3, sy, sx + 3, sy);
			graphics.drawLine(sx - 3, sy + 2, sx + 1, sy + 2);

			visibleTooltipTargets.add(board);
		}

		graphics.setStroke(oldStroke);
	}

	/**
	 * Player boats docked across Gielinor ports, drawn with crisp nautical pins and health badges.
	 */
	public void drawPlayerBoats(Graphics2D graphics, Rectangle bounds, List<Rectangle> placed)
	{
		if (boatTracker == null || (!camera.isTravelViewActive()
			&& (!config.showBoatLocations() || camera.getZoom() < config.playerBoatMinZoom())))
		{
			return;
		}

		final int plane = camera.getPlane();
		final List<PlayerBoat> owned = boatTracker.getOwnedBoats();
		if (owned.isEmpty())
		{
			return;
		}

		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		final Stroke oldStroke = graphics.getStroke();

		// Group/offset multiple boats docked at the same port so none are obscured
		final Map<SailingPort, Integer> portCounts = new HashMap<>();

		for (PlayerBoat boat : owned)
		{
			final SailingPort port = boat.getPort();
			if (port == null)
			{
				continue;
			}

			final WorldPoint loc = port.getNavigationLocation();
			if (loc == null || loc.getPlane() != plane || !ViewWindow.isDrawable(camera, loc.getX(), loc.getY()))
			{
				continue;
			}

			final int count = portCounts.getOrDefault(port, 0);
			portCounts.put(port, count + 1);

			final int offsetX = count * 9;
			final int offsetY = count * 9;

			final int sx = (int) Math.round(camera.screenX(loc.getX() + 0.5, loc.getY() + 0.5, bounds)) + offsetX;
			final int sy = (int) Math.round(camera.screenY(loc.getX() + 0.5, loc.getY() + 0.5, bounds)) + offsetY;

			final BufferedImage icon = boatIcon(boat.getBoatType());
			final int iconW = icon != null ? icon.getWidth() : 18;
			final int iconH = icon != null ? icon.getHeight() : 18;
			final Rectangle rect = new Rectangle(sx - iconW / 2, sy - iconH / 2, iconW, iconH);
			if (!bounds.intersects(rect))
			{
				continue;
			}

			if (icon != null)
			{
				graphics.drawImage(icon, rect.x, rect.y, null);
			}
			else
			{
				// Fallback if boat icon resource fails to load
				graphics.setColor(BOAT_EDGE);
				graphics.fillOval(rect.x - 1, rect.y - 1, rect.width + 2, rect.height + 2);
				graphics.setColor(BOAT_FILL);
				graphics.fillOval(rect.x, rect.y, rect.width, rect.height);
				graphics.setColor(BOAT_BORDER);
				graphics.setStroke(MARKER_RING);
				graphics.drawOval(rect.x, rect.y, rect.width, rect.height);
			}

			// Health condition ring accent (top-right pip)
			final Color healthColor = boat.getHealth() < 0 ? BOAT_HULL_GOOD
				: (boat.getHealth() >= 0.75f ? BOAT_HULL_GOOD : (boat.getHealth() >= 0.35f ? BOAT_HULL_WARN : BOAT_HULL_BAD));
			graphics.setColor(healthColor);
			graphics.fillOval(rect.x + rect.width - 5, rect.y - 1, 6, 6);
			graphics.setColor(BOAT_EDGE);
			graphics.setStroke(MARKER_OUTLINE);
			graphics.drawOval(rect.x + rect.width - 5, rect.y - 1, 6, 6);

			// Boat glyph / initials or number
			if (icon == null)
			{
				graphics.setFont(SMALL);
				final String numStr = String.valueOf(boat.getBoatId());
				final int numW = graphics.getFontMetrics().stringWidth(numStr);
				graphics.setColor(Color.WHITE);
				graphics.drawString(numStr, sx - numW / 2, sy + 4);
			}
			else if (owned.size() > 1)
			{
				final String numStr = String.valueOf(boat.getBoatId());
				final int badgeSize = 10;
				final int bx = rect.x + rect.width - 3;
				final int by = rect.y + rect.height - 3;
				graphics.setColor(BOAT_EDGE);
				graphics.fillOval(bx - badgeSize / 2, by - badgeSize / 2, badgeSize, badgeSize);
				graphics.setColor(BOAT_BORDER);
				graphics.setStroke(MARKER_OUTLINE);
				graphics.drawOval(bx - badgeSize / 2, by - badgeSize / 2, badgeSize, badgeSize);
				graphics.setFont(TINY);
				final int numW = graphics.getFontMetrics().stringWidth(numStr);
				graphics.setColor(Color.WHITE);
				graphics.drawString(numStr, bx - numW / 2, by + 3);
			}

			placed.add(rect);
			visibleTooltipTargets.add(boat);

			// Optional Name label chip below marker
			if (config.showBoatNames() && camera.getZoom() >= config.boatLabelMinZoom())
			{
				final String label = "⛵ " + boat.getBoatName();
				MarkerLabelRenderer.draw(graphics, bounds, label, sx, rect.y + iconH + 12,
					4, 10, 13, BOAT_BORDER, CARD_TITLE, MARKER_OUTLINE);
			}
		}

		graphics.setStroke(oldStroke);
	}

}
