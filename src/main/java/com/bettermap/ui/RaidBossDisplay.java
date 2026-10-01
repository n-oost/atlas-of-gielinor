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

import com.bettermap.BetterMapConfig;
import com.bettermap.data.MonsterLocationData;
import com.bettermap.data.UndergroundZone;
import com.bettermap.map.MapCamera;
import com.bettermap.map.MonsterIconManager;
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

import static com.bettermap.ui.MapStyle.*;
import static com.bettermap.data.MonsterLocationData.*;

/** Clickable raid entrances and a boss gallery inside the world map. */
@Singleton
public class RaidBossDisplay
{
	private enum Raid
	{
		XERIC("Chambers of Xeric", "CoX", 1255, 3564,
			TEKTON, VANGUARD, VESPULA, VASA_NISTIRIO, MUTTADILE, GREAT_OLM),
		THEATRE("Theatre of Blood", "ToB", 3667, 3218,
			THE_MAIDEN_OF_SUGADINTI, PESTILENT_BLOAT, NYLOCAS_VASILIAS, SOTETSEG, XARPUS, VERZIK_VITUR),
		TOMBS("Tombs of Amascut", "ToA", 3493, 3090,
			AKKHA, BA_BA, KEPHRI, ZEBAK, TUMEKEN_S_WARDEN, ELIDINIS_WARDEN);

		final String name;
		final String shortName;
		final int x;
		final int y;
		final MonsterLocationData[] bosses;

		Raid(String name, String shortName, int x, int y, MonsterLocationData... bosses)
		{
			this.name = name;
			this.shortName = shortName;
			this.x = x;
			this.y = y;
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
	}

	/** Returns true when a map press belongs to the gallery instead of map panning. */
	public boolean press(Point point, boolean leftButton)
	{
		if (isOpen())
		{
			final Rectangle close = closeButton;
			if (leftButton && close != null && close.contains(point))
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
				if (camera.getPlane() == 0)
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
						final String label = entrance.shortName + " · Bosses";
						final int width = g.getFontMetrics().stringWidth(label) + 18;
						final Rectangle button = new Rectangle(x - width / 2, y - 36, width, 24);
						box(g, button, cursor != null && button.contains(cursor));
						g.setColor(CARD_TITLE);
						g.drawString(label, button.x + 9, button.y + 16);
						drawn.add(new Target(button, entrance, null));
					}
				}
			}
			else
			{
				g.setColor(new Color(0, 0, 0, 150));
				g.fillRect(viewport.x, viewport.y, viewport.width, viewport.height);
				final int width = Math.min(620, viewport.width - 24);
				final int columns = width >= 460 ? 2 : 1;
				final int rows = (raid.bosses.length + columns - 1) / columns;
				final int rowHeight = Math.min(70, Math.max(28, (viewport.height - 112) / rows));
				final int height = rows * rowHeight + 88;
				final Rectangle panel = new Rectangle(viewport.x + (viewport.width - width) / 2,
					viewport.y + (viewport.height - height) / 2, width, height);
				box(g, panel, false);
				g.setFont(RUNELITE_BOLD);
				g.setColor(CARD_TITLE);
				g.drawString(raid.name, panel.x + 16, panel.y + 25);
				g.setFont(SMALL);
				g.setColor(CARD_TEXT);
				g.drawString("Hover a boss for details", panel.x + 16, panel.y + 44);
				final Rectangle close = new Rectangle(panel.x + width - 34, panel.y + 9, 24, 24);
				box(g, close, cursor != null && close.contains(cursor));
				g.setColor(CARD_TITLE);
				g.drawString("×", close.x + 8, close.y + 16);
				closeButton = close;
				final int cellWidth = (width - 32) / columns;
				for (int i = 0; i < raid.bosses.length; i++)
				{
					final MonsterLocationData boss = raid.bosses[i];
					final Rectangle cell = new Rectangle(panel.x + 16 + (i % columns) * cellWidth,
						panel.y + 56 + (i / columns) * rowHeight, cellWidth - 6, rowHeight - 6);
					box(g, cell, cursor != null && cell.contains(cursor));
					final int iconSize = Math.min(28, cell.height - 8);
					final BufferedImage icon = icons.getBossIcon(boss, iconSize);
					if (icon != null)
					{
						g.drawImage(icon, cell.x + 8, cell.y + (cell.height - iconSize) / 2, null);
					}
					g.setFont(SMALL);
					g.setColor(CARD_TEXT);
					String name = boss.getName();
					while (name.length() > 1 && g.getFontMetrics().stringWidth(name) > cell.width - 50)
					{
						name = name.substring(0, name.length() - 2) + "…";
					}
					g.drawString(name, cell.x + 42, cell.y + cell.height / 2 + 4);
					drawn.add(new Target(cell, raid, boss));
				}
				g.setColor(TEXT_DIM);
				g.drawString("Esc or × to close", panel.x + 16, panel.y + height - 10);
			}
			if (selected == raid)
			{
				targets = Collections.unmodifiableList(drawn);
			}
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
								Collections.singletonList("Click to view all raid bosses"));
						}
						break;
					}
				}
			}
		}
		finally
		{
			g.dispose();
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
