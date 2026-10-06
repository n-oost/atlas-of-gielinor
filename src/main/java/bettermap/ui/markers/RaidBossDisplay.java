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

import static bettermap.data.MonsterLocationData.*;
import static bettermap.ui.MapStyle.*;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

import bettermap.BetterMapConfig;
import bettermap.data.MonsterLocationData;
import bettermap.data.UndergroundZone;
import bettermap.map.MapCamera;
import bettermap.map.MonsterIconManager;
import bettermap.ui.MapTooltipRenderer;

/** Clickable raid entrances and a boss gallery inside the world map. */
@Singleton
public class RaidBossDisplay
{
	private enum Raid
	{
		XERIC("Chambers of Xeric", "CoX", 1255, 3564, GREAT_OLM,
			TEKTON, VANGUARD, VESPULA, VASA_NISTIRIO, MUTTADILE, GREAT_OLM),
		THEATRE("Theatre of Blood", "ToB", 3667, 3218, VERZIK_VITUR,
			THE_MAIDEN_OF_SUGADINTI, PESTILENT_BLOAT, NYLOCAS_VASILIAS, SOTETSEG, XARPUS, VERZIK_VITUR),
		TOMBS("Tombs of Amascut", "ToA", 3356, 2712, TUMEKEN_S_WARDEN,
			AKKHA, BA_BA, KEPHRI, ZEBAK, TUMEKEN_S_WARDEN, ELIDINIS_WARDEN);

		final String name;
		final String shortName;
		final int x;
		final int y;
		final MonsterLocationData signatureBoss;
		final MonsterLocationData[] bosses;

		Raid(String name, String shortName, int x, int y, MonsterLocationData signatureBoss, MonsterLocationData... bosses)
		{
			this.name = name;
			this.shortName = shortName;
			this.x = x;
			this.y = y;
			this.signatureBoss = signatureBoss;
			this.bosses = bosses;
		}
	}

	private static final class Target
	{
		final Rectangle bounds;
		final Raid raid;
		final MonsterLocationData boss;

		Target(Rectangle bounds, Raid raid, MonsterLocationData boss)
		{
			this.bounds = bounds;
			this.raid = raid;
			this.boss = boss;
		}
	}

	private static final EnumSet<MonsterLocationData> RAID_BOSSES = EnumSet.noneOf(MonsterLocationData.class);
	static
	{
		for (Raid raid : Raid.values())
		{
			Collections.addAll(RAID_BOSSES, raid.bosses);
		}
	}

	private final MapCamera camera;
	private final BetterMapConfig config;
	private final MonsterIconManager icons;
	private volatile Raid selected;
	private volatile List<Target> targets = Collections.emptyList();
	private volatile Rectangle closeButton;
	private volatile Rectangle panelBounds;

	@Inject
	public RaidBossDisplay(MapCamera camera, BetterMapConfig config, MonsterIconManager icons)
	{
		this.camera = camera;
		this.config = config;
		this.icons = icons;
	}

	public static boolean isRaidBoss(MonsterLocationData boss)
	{
		return RAID_BOSSES.contains(boss);
	}

	public boolean ownsHover(Point cursor)
	{
		if (isOpen())
		{
			return true;
		}
		if (cursor != null)
		{
			for (Target target : targets)
			{
				if (target.bounds.contains(cursor))
				{
					return true;
				}
			}
		}
		return false;
	}

	public boolean isOpen()
	{
		return selected != null;
	}

	public void close()
	{
		selected = null;
		targets = Collections.emptyList();
		closeButton = null;
		panelBounds = null;
	}

	/** Returns true when a map press belongs to the gallery instead of map panning. */
	public boolean press(Point point, boolean leftButton)
	{
		if (isOpen())
		{
			final Rectangle close = closeButton;
			final Rectangle panel = panelBounds;
			if (leftButton && ((close != null && close.contains(point)) || (panel != null && !panel.contains(point))))
			{
				close();
			}
			return true;
		}
		if (leftButton)
		{
			for (Target target : targets)
			{
				if (target.boss == null && target.bounds.contains(point))
				{
					selected = target.raid;
					targets = Collections.emptyList();
					return true;
				}
			}
		}
		return false;
	}

	public void draw(Graphics2D graphics, Rectangle viewport, Point cursor, MapTooltipRenderer tooltips)
	{
		if (!config.showBossLocations())
		{
			close();
			return;
		}
		final Graphics2D g = (Graphics2D) graphics.create();
		try
		{
			final Raid raid = selected;
			final List<Target> drawn = new ArrayList<>();
			if (raid == null)
			{
				drawEntrances(g, viewport, cursor, drawn);
			}
			else
			{
				drawGallery(g, viewport, cursor, raid, drawn);
			}
			if (selected == raid)
			{
				targets = Collections.unmodifiableList(drawn);
			}
			drawHoveredTooltip(g, viewport, cursor, tooltips, drawn);
		}
		finally
		{
			g.dispose();
		}
	}

	private void drawEntrances(Graphics2D g, Rectangle viewport, Point cursor, List<Target> drawn)
	{
		if (camera.getPlane() == 0 && camera.getZoom() >= config.bossMinZoom())
		{
			for (Raid entrance : Raid.values())
			{
				final int worldX = entrance == Raid.TOMBS
					? UndergroundZone.TOMBS_OF_AMASCUT.getSurfacePoint().getX() : entrance.x;
				final int worldY = entrance == Raid.TOMBS
					? UndergroundZone.TOMBS_OF_AMASCUT.getSurfacePoint().getY() : entrance.y;
				final int x = (int) Math.round(camera.screenX(worldX + 0.5, worldY + 0.5, viewport));
				final int y = (int) Math.round(camera.screenY(worldX + 0.5, worldY + 0.5, viewport));
				if (!viewport.contains(x, y))
				{
					continue;
				}
				g.setFont(SMALL);
				final String label = entrance.shortName;
				final int textWidth = g.getFontMetrics().stringWidth(label);
				final int iconSize = 16;
				final int width = iconSize + textWidth + 14;
				final int height = 20;
				final Rectangle button = new Rectangle(x - width / 2, y - 22, width, height);
				final boolean hovered = cursor != null && button.contains(cursor);
				box(g, button, hovered);
				final BufferedImage icon = icons.getBossIcon(entrance.signatureBoss, iconSize);
				if (icon != null)
				{
					g.drawImage(icon, button.x + 4, button.y + (height - iconSize) / 2, null);
				}
				g.setColor(CARD_TITLE);
				g.drawString(label, button.x + iconSize + 7, button.y + 14);
				drawn.add(new Target(button, entrance, null));
			}
		}
	}

	private void drawGallery(Graphics2D g, Rectangle viewport, Point cursor, Raid raid, List<Target> drawn)
	{
		g.setColor(new Color(0, 0, 0, 150));
		g.fillRect(viewport.x, viewport.y, viewport.width, viewport.height);
		final int width = Math.min(380, viewport.width - 24);
		final int columns = width >= 340 ? 2 : 1;
		final int rows = (raid.bosses.length + columns - 1) / columns;
		final int rowHeight = 34;
		final int height = rows * rowHeight + 66;
		final Rectangle panel = new Rectangle(viewport.x + (viewport.width - width) / 2,
			viewport.y + (viewport.height - height) / 2, width, height);
		box(g, panel, false);
		panelBounds = panel;
		g.setFont(RUNELITE_BOLD);
		g.setColor(CARD_TITLE);
		g.drawString(raid.name, panel.x + 14, panel.y + 20);
		g.setFont(SMALL);
		g.setColor(TEXT_DIM);
		g.drawString(raid.bosses.length + " Bosses • Hover for details", panel.x + 14, panel.y + 35);
		final Rectangle close = new Rectangle(panel.x + width - 26, panel.y + 6, 20, 20);
		box(g, close, cursor != null && close.contains(cursor));
		g.setColor(CARD_TITLE);
		g.drawString("×", close.x + 6, close.y + 14);
		closeButton = close;
		final int cellWidth = (width - 24) / columns;
		for (int i = 0; i < raid.bosses.length; i++)
		{
			final MonsterLocationData boss = raid.bosses[i];
			final Rectangle cell = new Rectangle(panel.x + 10 + (i % columns) * cellWidth,
				panel.y + 44 + (i / columns) * rowHeight, cellWidth - 4, rowHeight - 4);
			box(g, cell, cursor != null && cell.contains(cursor));
			final int iconSize = Math.min(22, cell.height - 4);
			final BufferedImage icon = icons.getBossIcon(boss, iconSize);
			if (icon != null)
			{
				g.drawImage(icon, cell.x + 4, cell.y + (cell.height - iconSize) / 2, null);
			}
			g.setFont(SMALL);
			g.setColor(CARD_TEXT);
			String name = boss.getName();
			while (name.length() > 1 && g.getFontMetrics().stringWidth(name) > cell.width - 34)
			{
				name = name.substring(0, name.length() - 2) + "…";
			}
			g.drawString(name, cell.x + 30, cell.y + cell.height / 2 + 4);
			drawn.add(new Target(cell, raid, boss));
		}
		g.setColor(TEXT_DIM);
		g.drawString("Esc, click outside, or × to close", panel.x + 14, panel.y + height - 8);
	}

	private void drawHoveredTooltip(Graphics2D g, Rectangle viewport, Point cursor, MapTooltipRenderer tooltips, List<Target> drawn)
	{
		if (cursor != null)
		{
			for (Target target : drawn)
			{
				if (target.bounds.contains(cursor))
				{
					if (target.boss != null)
					{
						tooltips.drawRaidBossCard(g, viewport, cursor, target.boss, target.raid.name);
					}
					else
					{
						tooltips.drawCard(g, viewport, cursor, target.raid.name,
							List.of(target.raid.bosses.length + " Raid Bosses", "Click to view bosses"));
					}
					break;
				}
			}
		}
	}

	private static void box(Graphics2D g, Rectangle bounds, boolean hovered)
	{
		g.setColor(hovered ? new Color(43, 47, 57) : CARD_BG);
		g.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 8, 8);
		g.setColor(hovered ? CARD_TITLE : CARD_EDGE);
		g.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 8, 8);
	}
}
