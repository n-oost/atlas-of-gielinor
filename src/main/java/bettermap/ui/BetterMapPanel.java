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
package bettermap.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import bettermap.BetterMapPlugin;
import bettermap.data.MonsterLocationData;
import bettermap.data.sailing.BoatTracker;
import bettermap.data.sailing.PlayerBoat;
import bettermap.data.sailing.SailingPort;
import bettermap.map.MonsterIndex;
import bettermap.map.PoiIndex;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

/**
 * The map legend: every kind of place on the map, collapsed into one row each.
 *
 * <p>Click a row to open the individual locations, nearest first, and click one of those to put
 * the map on it. That is the difference between a legend that tells you an icon exists and one
 * that takes you to it.
 */
public class BetterMapPanel extends PluginPanel
{
	private static final Color ROW_HOVER = ColorScheme.DARKER_GRAY_HOVER_COLOR;
	private static final Color ROW_BG = ColorScheme.DARKER_GRAY_COLOR;
	private static final int MAX_ENTRIES_PER_GROUP = 40;
	private static final int MAX_MONSTER_GROUPS = 40;

	private final BetterMapPlugin plugin;
	private final PoiIndex poiIndex;
	private final MonsterIndex monsterIndex;
	private final BoatTracker boatTracker;

	private final JTextField searchField = new JTextField();
	private final JPanel listPanel = new JPanel();

	private String expandedKey;

	public BetterMapPanel(BetterMapPlugin plugin, PoiIndex poiIndex, MonsterIndex monsterIndex, BoatTracker boatTracker)
	{
		super(false);
		this.plugin = plugin;
		this.poiIndex = poiIndex;
		this.monsterIndex = monsterIndex;
		this.boatTracker = boatTracker;

		setLayout(new BorderLayout(0, 6));
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setBorder(new EmptyBorder(8, 8, 8, 8));

		searchField.setToolTipText("Filter locations");
		searchField.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				rebuild();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				rebuild();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				rebuild();
			}
		});

		listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
		listPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);

		final JPanel listHolder = new JPanel(new BorderLayout());
		listHolder.setBackground(ColorScheme.DARK_GRAY_COLOR);
		listHolder.add(listPanel, BorderLayout.NORTH);

		final JScrollPane scroll = new JScrollPane(listHolder,
			ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
			ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setBorder(BorderFactory.createEmptyBorder());
		scroll.getVerticalScrollBar().setUnitIncrement(16);

		add(searchField, BorderLayout.NORTH);
		add(scroll, BorderLayout.CENTER);

		rebuild();
	}

	/** Called when the index finishes loading, so the legend is not stuck empty. */
	public void refresh()
	{
		rebuild();
	}

	private void rebuild()
	{
		listPanel.removeAll();

		final String filter = searchField.getText().trim().toLowerCase(Locale.ROOT);
		final WorldPoint from = plugin.getPlayerLocation();

		addBoatsSection(filter, from);
		addMonsterSection(filter, from);

		final Map<String, List<PoiIndex.Poi>> groups = poiIndex.groups();
		if (groups.isEmpty())
		{
			listPanel.add(hint("No location data yet."));
		}

		for (Map.Entry<String, List<PoiIndex.Poi>> entry : groups.entrySet())
		{
			final String key = entry.getKey();
			final List<PoiIndex.Poi> entries = entry.getValue();
			final String name = entries.get(0).getName();

			if (!filter.isEmpty() && !name.toLowerCase(Locale.ROOT).contains(filter))
			{
				continue;
			}

			listPanel.add(groupRow(key, name, entries.size()));

			if (key.equals(expandedKey))
			{
				final List<PoiIndex.Poi> sorted = new ArrayList<>(entries);
				if (from != null)
				{
					sorted.sort(Comparator.comparingLong(p -> distanceSq(p.getX(), p.getY(), from)));
				}

				int shown = 0;
				for (PoiIndex.Poi poi : sorted)
				{
					if (shown++ >= MAX_ENTRIES_PER_GROUP)
					{
						listPanel.add(hint("  ... and " + (sorted.size() - MAX_ENTRIES_PER_GROUP) + " more"));
						break;
					}
					listPanel.add(entryRow(poi, from));
				}
			}
		}

		listPanel.revalidate();
		listPanel.repaint();
	}

	private void addBoatsSection(String filter, WorldPoint from)
	{
		if (boatTracker == null)
		{
			return;
		}

		final List<PlayerBoat> ownedBoats = boatTracker.getOwnedBoats();
		if (ownedBoats.isEmpty())
		{
			return;
		}

		final List<PlayerBoat> matches = new ArrayList<>();
		for (PlayerBoat boat : ownedBoats)
		{
			final String name = boat.getBoatName() != null ? boat.getBoatName() : "";
			final String portName = boat.getPort() != null ? boat.getPort().getName() : SailingPort.resolvePortName(null, boat.getPortId());
			final String typeName = boat.getBoatType() != null ? boat.getBoatType().getName() : "";
			if (filter.isEmpty()
				|| name.toLowerCase(Locale.ROOT).contains(filter)
				|| portName.toLowerCase(Locale.ROOT).contains(filter)
				|| typeName.toLowerCase(Locale.ROOT).contains(filter)
				|| "boats".contains(filter)
				|| "ships".contains(filter)
				|| "sailing".contains(filter))
			{
				matches.add(boat);
			}
		}

		if (matches.isEmpty())
		{
			return;
		}

		listPanel.add(groupRow("__boats__", "My Boats", matches.size()));

		if (!"__boats__".equals(expandedKey))
		{
			return;
		}

		for (PlayerBoat boat : matches)
		{
			final SailingPort port = boat.getPort();
			final String portName = port != null ? port.getName() : SailingPort.resolvePortName(null, boat.getPortId());
			final String typeStr = boat.getBoatType() != null ? " (" + boat.getBoatType().getName() + ")" : "";
			final String title = boat.getBoatName() + typeStr;

			String detail = "Docked: " + portName;
			if (port != null && port.getNavigationLocation() != null && from != null)
			{
				detail += "  " + tiles(port.getNavigationLocation(), from);
			}

			final Runnable action = (port != null && port.getNavigationLocation() != null)
				? () -> plugin.centerMapOn(port.getNavigationLocation())
				: null;

			listPanel.add(clickableRow("    " + title, detail, null, action));
		}
	}

	private void addMonsterSection(String filter, WorldPoint from)
	{
		if (!MonsterLocationData.isLoaded()) return;
		if (monsterIndex == null || !monsterIndex.isLoaded())
		{
			final List<MonsterLocationData> matches = new ArrayList<>();
			for (MonsterLocationData monster : MonsterLocationData.values())
			{
				if (filter.isEmpty()
					|| monster.getName().toLowerCase(Locale.ROOT).contains(filter)
					|| monster.getLocationName().toLowerCase(Locale.ROOT).contains(filter))
				{
					matches.add(monster);
				}
			}

			if (matches.isEmpty())
			{
				return;
			}

			listPanel.add(groupRow("__bosses__", "Bosses", matches.size()));

			if (!"__bosses__".equals(expandedKey))
			{
				return;
			}

			for (MonsterLocationData monster : matches)
			{
				final WorldPoint point = monster.getWorldPoint();
				final String detail = from == null
					? monster.getLocationName()
					: monster.getLocationName() + "  " + tiles(point, from);

				listPanel.add(clickableRow(monster.getName(), detail, null,
					() -> plugin.centerMapOn(point)));
			}
			return;
		}

		final Map<String, List<MonsterIndex.Zone>> byMonster = monsterIndex.byMonster();

		// 833 monsters would bury the legend, so they stay behind the filter rather than listing by default.
		if (filter.isEmpty())
		{
			listPanel.add(hint(byMonster.size() + " monsters — type to search them"));
			return;
		}

		final List<Map.Entry<String, List<MonsterIndex.Zone>>> matches = new ArrayList<>();
		for (Map.Entry<String, List<MonsterIndex.Zone>> entry : byMonster.entrySet())
		{
			boolean match = entry.getKey().toLowerCase(Locale.ROOT).contains(filter);
			if (!match)
			{
				for (MonsterIndex.Zone zone : entry.getValue())
				{
					if (zone.getLocationName() != null
						&& zone.getLocationName().toLowerCase(Locale.ROOT).contains(filter))
					{
						match = true;
						break;
					}
				}
			}
			if (match)
			{
				matches.add(entry);
			}
		}

		if (matches.isEmpty())
		{
			return;
		}

		int groupShown = 0;
		for (Map.Entry<String, List<MonsterIndex.Zone>> entry : matches)
		{
			if (groupShown++ >= MAX_MONSTER_GROUPS)
			{
				listPanel.add(hint("... and " + (matches.size() - MAX_MONSTER_GROUPS) + " more — keep typing"));
				break;
			}

			final String name = entry.getKey();
			final List<MonsterIndex.Zone> zones = entry.getValue();
			final String key = "__monster__" + name;

			listPanel.add(groupRow(key, name, zones.size()));

			if (key.equals(expandedKey))
			{
				final List<MonsterIndex.Zone> sorted = new ArrayList<>(zones);
				if (from != null)
				{
					sorted.sort(Comparator.comparingLong(z -> distanceSq(z.getX(), z.getY(), from)));
				}

				int shown = 0;
				for (MonsterIndex.Zone zone : sorted)
				{
					if (shown++ >= MAX_ENTRIES_PER_GROUP)
					{
						listPanel.add(hint("  ... and " + (sorted.size() - MAX_ENTRIES_PER_GROUP) + " more"));
						break;
					}

					final WorldPoint point = new WorldPoint(zone.getX(), zone.getY(), zone.getPlane());
					final String loc = zone.getLocationName() != null ? zone.getLocationName() : "";
					final String detail = from == null
						? loc
						: loc + "  " + tiles(point, from);

					listPanel.add(clickableRow("    " + zone.getMonster(), detail, null,
						() -> plugin.centerMapOn(point)));
				}
			}
		}
	}

	private JPanel groupRow(String key, String name, int count)
	{
		final JPanel row = clickableRow(name, count + (count == 1 ? " location" : " locations"),
			key.startsWith("__") ? null : poiIndex.icon(key),
			() ->
			{
				expandedKey = key.equals(expandedKey) ? null : key;
				rebuild();
			});

		if (key.equals(expandedKey))
		{
			row.setBackground(ColorScheme.DARKER_GRAY_HOVER_COLOR);
		}

		return row;
	}

	private JPanel entryRow(PoiIndex.Poi poi, WorldPoint from)
	{
		final String detail = from == null
			? poi.getX() + ", " + poi.getY()
			: poi.getX() + ", " + poi.getY() + "  "
				+ tiles(new WorldPoint(poi.getX(), poi.getY(), poi.getPlane()), from);

		final JPanel row = clickableRow("    " + poi.getName(), detail, null,
			() -> plugin.centerMapOn(new WorldPoint(poi.getX(), poi.getY(), poi.getPlane())));
		row.setBorder(new EmptyBorder(3, 14, 3, 6));
		return row;
	}

	private JPanel clickableRow(String title, String detail, java.awt.Image icon, Runnable action)
	{
		final JPanel row = new JPanel(new GridBagLayout());
		row.setBackground(ROW_BG);
		row.setBorder(new EmptyBorder(4, 6, 4, 6));
		row.setCursor(new Cursor(Cursor.HAND_CURSOR));
		row.setAlignmentX(Component.LEFT_ALIGNMENT);
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

		final GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.gridheight = 2;
		c.anchor = GridBagConstraints.WEST;
		c.insets = new java.awt.Insets(0, 0, 0, 6);

		if (icon != null)
		{
			row.add(new JLabel(new ImageIcon(icon)), c);
		}

		c.gridx = 1;
		c.gridheight = 1;
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;

		final JLabel titleLabel = new JLabel(title);
		titleLabel.setFont(FontManager.getRunescapeSmallFont());
		titleLabel.setForeground(Color.WHITE);
		row.add(titleLabel, c);

		c.gridy = 1;
		final JLabel detailLabel = new JLabel(detail);
		detailLabel.setFont(FontManager.getDefaultFont().deriveFont(10f));
		detailLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		row.add(detailLabel, c);

		row.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				if (action != null)
				{
					action.run();
				}
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				row.setBackground(ROW_HOVER);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				row.setBackground(ROW_BG);
			}
		});

		return row;
	}

	private JLabel hint(String text)
	{
		final JLabel label = new JLabel(text);
		label.setFont(FontManager.getDefaultFont().deriveFont(10f));
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		label.setBorder(new EmptyBorder(4, 6, 4, 6));
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
		return label;
	}

	/** Squared tile distance; only ever used to order lists, so the square root is wasted work. */
	private static long distanceSq(int x, int y, WorldPoint from)
	{
		final long dx = (long) x - from.getX();
		final long dy = (long) y - from.getY();
		return dx * dx + dy * dy;
	}

	private static String tiles(WorldPoint point, WorldPoint from)
	{
		final int dx = point.getX() - from.getX();
		final int dy = point.getY() - from.getY();
		return (int) Math.sqrt((double) dx * dx + (double) dy * dy) + " tiles";
	}
}
