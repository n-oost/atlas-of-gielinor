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
package atlasofgielinor.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import atlasofgielinor.AtlasOfGielinorPlugin;
import atlasofgielinor.data.BossLocationData;
import atlasofgielinor.data.sailing.BoatTracker;
import atlasofgielinor.data.sailing.PlayerBoat;
import atlasofgielinor.data.sailing.SailingPort;
import atlasofgielinor.map.MapFinder;
import atlasofgielinor.map.catalog.MapRegion;
import atlasofgielinor.map.catalog.MonsterIndex;
import atlasofgielinor.map.catalog.PoiIndex;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.LinkBrowser;

/**
 * The map legend sidebar: categorized identically to Finder into 10 top-level browse categories,
 * expanding via an accordion into groups and nearest-first locations.
 */
public class AtlasOfGielinorPanel extends PluginPanel
{
	private static final Color ROW_HOVER = ColorScheme.DARKER_GRAY_HOVER_COLOR;
	private static final Color ROW_BG = ColorScheme.DARKER_GRAY_COLOR;
	private static final int MAX_ENTRIES_PER_GROUP = 40;
	private static final int MAX_MONSTER_GROUPS = 40;
	private static final String FEEDBACK_URL = "https://github.com/n-oost/atlas-of-gielinor/issues/new/choose";

	private final AtlasOfGielinorPlugin plugin;
	private final PoiIndex poiIndex;
	private final MonsterIndex monsterIndex;
	private final BoatTracker boatTracker;

	private final JTextField searchField = new JTextField();
	private final JPanel listPanel = new JPanel();

	private MapFinder.BrowseCategory expandedCategory;
	private String expandedGroupKey;

	public AtlasOfGielinorPanel(AtlasOfGielinorPlugin plugin, PoiIndex poiIndex, MonsterIndex monsterIndex, BoatTracker boatTracker)
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

		final JButton feedbackButton = new JButton("Send feedback");
		feedbackButton.setToolTipText("Report a bug or suggest an improvement on GitHub");
		feedbackButton.addActionListener(e -> LinkBrowser.browse(FEEDBACK_URL));
		add(feedbackButton, BorderLayout.SOUTH);

		rebuild();
	}

	/** Called when the index finishes loading, so the legend is not stuck empty. */
	public void refresh()
	{
		rebuild();
	}

	private void centerMapOn(WorldPoint point)
	{
		if (plugin != null && point != null)
		{
			plugin.centerMapOn(point);
		}
	}

	private void rebuild()
	{
		listPanel.removeAll();

		final String filter = searchField.getText().trim().toLowerCase(Locale.ROOT);
		final WorldPoint from = plugin != null ? plugin.getPlayerLocation() : null;

		if (!poiIndex.isLoaded())
		{
			listPanel.add(hint("Loading map data..."));
			listPanel.revalidate();
			listPanel.repaint();
			return;
		}

		final Map<String, List<PoiIndex.Poi>> allPoiGroups = poiIndex.groups();

		// Partition POI groups into Finder's BrowseCategories
		final Map<MapFinder.BrowseCategory, Map<String, List<PoiIndex.Poi>>> categoryGroups = new EnumMap<>(MapFinder.BrowseCategory.class);
		for (MapFinder.BrowseCategory cat : MapFinder.BrowseCategory.values())
		{
			categoryGroups.put(cat, new LinkedHashMap<>());
		}

		for (Map.Entry<String, List<PoiIndex.Poi>> entry : allPoiGroups.entrySet())
		{
			final String key = entry.getKey();
			MapFinder.BrowseCategory cat = MapFinder.BrowseCategory.of(key);
			if (cat == null)
			{
				cat = MapFinder.BrowseCategory.SERVICES;
			}
			categoryGroups.get(cat).put(key, entry.getValue());
		}

		int categoriesShown = 0;
		for (MapFinder.BrowseCategory cat : MapFinder.BrowseCategory.values())
		{
			if (renderCategorySection(cat, categoryGroups.get(cat), filter, from))
			{
				categoriesShown++;
			}
		}

		if (categoriesShown == 0)
		{
			listPanel.add(hint(filter.isEmpty() ? "No location data available." : "No locations matching \"" + filter + "\"."));
		}

		listPanel.revalidate();
		listPanel.repaint();
	}

	private boolean renderCategorySection(MapFinder.BrowseCategory cat, Map<String, List<PoiIndex.Poi>> poiGroups,
		String filter, WorldPoint from)
	{
		switch (cat)
		{
			case QUESTS:
			{
				final List<PoiIndex.Poi> matchingQuests = new ArrayList<>();
				for (List<PoiIndex.Poi> quests : poiGroups.values())
				{
					for (PoiIndex.Poi quest : quests)
					{
						if (filter.isEmpty() || quest.getName().toLowerCase(Locale.ROOT).contains(filter)
							|| cat.getDisplayName().toLowerCase(Locale.ROOT).contains(filter))
						{
							matchingQuests.add(quest);
						}
					}
				}
				return renderSection(cat, filter, countLabel(poiCount(poiGroups), "quest"),
					matchingQuests.isEmpty() ? null : () -> renderQuests(matchingQuests, from));
			}
			case MONSTERS:
			{
				if (!BossLocationData.isLoaded() && (monsterIndex == null || !monsterIndex.isLoaded()))
				{
					return false;
				}
				if (monsterIndex != null && monsterIndex.isLoaded())
				{
					final Map<String, List<MonsterIndex.Zone>> byMonster = monsterIndex.byMonster();
					final List<Map.Entry<String, List<MonsterIndex.Zone>>> matching = new ArrayList<>();

					for (Map.Entry<String, List<MonsterIndex.Zone>> entry : byMonster.entrySet())
					{
						if (filter.isEmpty())
						{
							matching.add(entry);
						}
						else
						{
							boolean match = entry.getKey().toLowerCase(Locale.ROOT).contains(filter)
								|| cat.getDisplayName().toLowerCase(Locale.ROOT).contains(filter);
							if (!match)
							{
								for (MonsterIndex.Zone zone : entry.getValue())
								{
									if (zone.getLocationName() != null && zone.getLocationName().toLowerCase(Locale.ROOT).contains(filter))
									{
										match = true;
										break;
									}
								}
							}
							if (match)
							{
								matching.add(entry);
							}
						}
					}
					return renderSection(cat, filter, byMonster.size() + " monsters",
						matching.isEmpty() ? null : () -> renderMonsters(matching, filter, from));
				}
				final List<BossLocationData> bosses = new ArrayList<>();
				for (BossLocationData boss : BossLocationData.values())
				{
					if (filter.isEmpty()
						|| boss.getName().toLowerCase(Locale.ROOT).contains(filter)
						|| boss.getLocationName().toLowerCase(Locale.ROOT).contains(filter))
					{
						bosses.add(boss);
					}
				}
				return renderSection(cat, filter, BossLocationData.values().length + " bosses",
					bosses.isEmpty() ? null : () -> renderBosses(bosses, from));
			}
			case PLACES:
			{
				final List<MapRegion> regions = new ArrayList<>();
				for (MapRegion region : MapRegion.values())
				{
					if (!region.isFallback())
					{
						if (filter.isEmpty()
							|| region.getDisplayName().toLowerCase(Locale.ROOT).contains(filter)
							|| cat.getDisplayName().toLowerCase(Locale.ROOT).contains(filter))
						{
							regions.add(region);
						}
					}
				}

				if (from != null)
				{
					regions.sort(Comparator.comparingInt(r -> distanceToRegionCenter(r, from)));
				}
				if (!filter.isEmpty() && regions.isEmpty() && poiGroups.isEmpty())
				{
					return false;
				}
				// POI-only Places results retain their header without expanding during search.
				return renderSection(cat, regions.isEmpty() ? "" : filter, regions.size() + " regions",
					() -> renderPlaces(regions, poiGroups, filter, from));
			}
			case TRAVEL:
			{
				final List<PlayerBoat> ownedBoats = boatTracker != null ? boatTracker.getOwnedBoats() : Collections.emptyList();
				final List<PlayerBoat> matchingBoats = new ArrayList<>();

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
						matchingBoats.add(boat);
					}
				}

				final Map<String, List<PoiIndex.Poi>> matchingGroups = matchingPoiGroups(cat, poiGroups, filter);
				return renderSection(cat, filter, countLabel(matchingBoats.size() + poiCount(poiGroups), "location"),
					matchingBoats.isEmpty() && matchingGroups.isEmpty() ? null : () ->
					{
						renderBoats(matchingBoats, filter, from);
						renderPoiGroups(matchingGroups, filter, from);
					});
			}
			default:
			{
				final Map<String, List<PoiIndex.Poi>> matchingGroups = matchingPoiGroups(cat, poiGroups, filter);
				return renderSection(cat, filter, countLabel(poiCount(poiGroups), "location"),
					matchingGroups.isEmpty() ? null : () -> renderPoiGroups(matchingGroups, filter, from));
			}
		}
	}

	/** A null child supplier denotes no matches; unfiltered empty sections still show their header. */
	private boolean renderSection(MapFinder.BrowseCategory cat, String filter, String count, Runnable childSupplier)
	{
		if (!filter.isEmpty() && childSupplier == null)
		{
			return false;
		}
		final boolean isExpanded = cat == expandedCategory || !filter.isEmpty();
		listPanel.add(categoryRow(cat, count, isExpanded));
		if (isExpanded && childSupplier != null)
		{
			childSupplier.run();
		}
		return true;
	}

	private static int poiCount(Map<String, List<PoiIndex.Poi>> groups)
	{
		return groups.values().stream().mapToInt(List::size).sum();
	}

	private static String countLabel(int count, String singular)
	{
		return count + " " + singular + (count == 1 ? "" : "s");
	}

	private void renderQuests(List<PoiIndex.Poi> matchingQuests, WorldPoint from)
	{
		final List<PoiIndex.Poi> sorted = new ArrayList<>(matchingQuests);
		sorted.sort(Comparator.comparing(PoiIndex.Poi::getName, String.CASE_INSENSITIVE_ORDER));

		int shown = 0;
		for (PoiIndex.Poi quest : sorted)
		{
			if (shown++ >= MAX_ENTRIES_PER_GROUP)
			{
				listPanel.add(hint("  ... and " + (sorted.size() - MAX_ENTRIES_PER_GROUP) + " more — filter to narrow down"));
				break;
			}

			final WorldPoint wp = new WorldPoint(quest.getX(), quest.getY(), quest.getPlane());
			final String detail = from == null
				? quest.getX() + ", " + quest.getY()
				: quest.getX() + ", " + quest.getY() + "  " + tiles(wp, from);

			listPanel.add(clickableRow(quest.getName(), detail, poiIndex.icon("quest_start"), false, false, 16,
				() -> centerMapOn(wp)));
		}
	}

	private void renderMonsters(List<Map.Entry<String, List<MonsterIndex.Zone>>> matching, String filter, WorldPoint from)
	{
		int groupShown = 0;
		for (Map.Entry<String, List<MonsterIndex.Zone>> entry : matching)
		{
			if (groupShown++ >= MAX_MONSTER_GROUPS)
			{
				listPanel.add(hint("  ... and " + (matching.size() - MAX_MONSTER_GROUPS) + " more — type to filter"));
				break;
			}

			final String monsterName = entry.getKey();
			final List<MonsterIndex.Zone> zones = entry.getValue();
			final String groupKey = "__monster__" + monsterName;
			final boolean isGroupExpanded = groupKey.equals(expandedGroupKey) || !filter.isEmpty();

			listPanel.add(groupRow(groupKey, monsterName, zones.size(), isGroupExpanded, 16));

			if (isGroupExpanded)
			{
				final List<MonsterIndex.Zone> sortedZones = new ArrayList<>(zones);
				if (from != null)
				{
					sortedZones.sort(Comparator.comparingLong(z -> distanceSq(z.getX(), z.getY(), from)));
				}

				int shownZone = 0;
				for (MonsterIndex.Zone zone : sortedZones)
				{
					if (shownZone++ >= MAX_ENTRIES_PER_GROUP)
					{
						listPanel.add(hint("    ... and " + (sortedZones.size() - MAX_ENTRIES_PER_GROUP) + " more"));
						break;
					}

					final WorldPoint point = new WorldPoint(zone.getX(), zone.getY(), zone.getPlane());
					final String loc = zone.getLocationName() != null ? zone.getLocationName() : zone.getMonster();
					final String detail = from == null
						? loc
						: loc + "  " + tiles(point, from);

					listPanel.add(clickableRow(loc, detail, null, false, false, 26,
						() -> centerMapOn(point)));
				}
			}
		}
	}

	private void renderBosses(List<BossLocationData> bosses, WorldPoint from)
	{
		for (BossLocationData boss : bosses)
		{
			final WorldPoint wp = boss.getWorldPoint();
			final String detail = from == null
				? boss.getLocationName()
				: boss.getLocationName() + "  " + tiles(wp, from);

			listPanel.add(clickableRow(boss.getName(), detail, null, false, false, 16,
				() -> centerMapOn(wp)));
		}
	}

	private void renderPlaces(List<MapRegion> regions, Map<String, List<PoiIndex.Poi>> poiGroups,
		String filter, WorldPoint from)
	{
		for (MapRegion region : regions)
		{
			final String regionGroupKey = "__region__" + region.name();
			final boolean isRegionExpanded = regionGroupKey.equals(expandedGroupKey) || !filter.isEmpty();
			final int dist = distanceToRegionCenter(region, from);
			final String distText = dist >= 0 ? dist + " tiles" : "";

			listPanel.add(groupRow(regionGroupKey, region.getDisplayName(), distText, isRegionExpanded, 16));

			if (isRegionExpanded)
			{
				final WorldPoint center = new WorldPoint(region.getCenterX(), region.getCenterY(), 0);
				final String centerDetail = region.getCenterX() + ", " + region.getCenterY()
					+ (dist >= 0 ? "  " + dist + " tiles" : "");

				listPanel.add(clickableRow("Region Center", centerDetail, null, false, false, 26,
					() -> centerMapOn(center)));

				final List<PoiIndex.Poi> regionPois = new ArrayList<>();
				for (PoiIndex.Poi poi : poiIndex.all())
				{
					if (MapRegion.of(poi.getX(), poi.getY(), poi.getPlane()) == region)
					{
						if (filter.isEmpty() || poi.getName().toLowerCase(Locale.ROOT).contains(filter))
						{
							regionPois.add(poi);
						}
					}
				}

				if (from != null)
				{
					regionPois.sort(Comparator.comparingLong(p -> distanceSq(p.getX(), p.getY(), from)));
				}

				int shownPoi = 0;
				for (PoiIndex.Poi poi : regionPois)
				{
					if (shownPoi++ >= MAX_ENTRIES_PER_GROUP)
					{
						listPanel.add(hint("    ... and " + (regionPois.size() - MAX_ENTRIES_PER_GROUP) + " more"));
						break;
					}

					final WorldPoint wp = new WorldPoint(poi.getX(), poi.getY(), poi.getPlane());
					final String detail = from == null
						? poi.getX() + ", " + poi.getY()
						: poi.getX() + ", " + poi.getY() + "  " + tiles(wp, from);

					listPanel.add(clickableRow(poi.getName(), detail, poiIndex.icon(poi.getKey()), false, false, 26,
						() -> centerMapOn(wp)));
				}
			}
		}

		// Also render any POI groups in Places (e.g. region_label)
		renderPoiGroups(poiGroups, filter, from);
	}

	private void renderBoats(List<PlayerBoat> matchingBoats, String filter, WorldPoint from)
	{
		if (!matchingBoats.isEmpty())
		{
			final String boatsGroupKey = "__boats__";
			final boolean isBoatsExpanded = boatsGroupKey.equals(expandedGroupKey) || !filter.isEmpty();

			listPanel.add(groupRow(boatsGroupKey, "My Boats", matchingBoats.size(), isBoatsExpanded, 16));

			if (isBoatsExpanded)
			{
				for (PlayerBoat boat : matchingBoats)
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
						? () -> centerMapOn(port.getNavigationLocation())
						: null;

					listPanel.add(clickableRow(title, detail, null, false, false, 26, action));
				}
			}
		}
	}

	private static Map<String, List<PoiIndex.Poi>> matchingPoiGroups(MapFinder.BrowseCategory cat,
		Map<String, List<PoiIndex.Poi>> poiGroups, String filter)
	{
		final Map<String, List<PoiIndex.Poi>> matchingGroups = new LinkedHashMap<>();
		for (Map.Entry<String, List<PoiIndex.Poi>> entry : poiGroups.entrySet())
		{
			final String groupKey = entry.getKey();
			final List<PoiIndex.Poi> pois = entry.getValue();
			final String groupName = pois.get(0).getName();

			if (filter.isEmpty())
			{
				matchingGroups.put(groupKey, pois);
			}
			else
			{
				final boolean groupMatches = groupName.toLowerCase(Locale.ROOT).contains(filter)
					|| cat.getDisplayName().toLowerCase(Locale.ROOT).contains(filter);
				if (groupMatches)
				{
					matchingGroups.put(groupKey, pois);
				}
				else
				{
					final List<PoiIndex.Poi> matchedPois = new ArrayList<>();
					for (PoiIndex.Poi p : pois)
					{
						if (p.getName().toLowerCase(Locale.ROOT).contains(filter))
						{
							matchedPois.add(p);
						}
					}
					if (!matchedPois.isEmpty())
					{
						matchingGroups.put(groupKey, matchedPois);
					}
				}
			}
		}
		return matchingGroups;
	}

	private void renderPoiGroups(Map<String, List<PoiIndex.Poi>> groups, String filter, WorldPoint from)
	{
		for (Map.Entry<String, List<PoiIndex.Poi>> entry : groups.entrySet())
		{
			final String groupKey = entry.getKey();
			final List<PoiIndex.Poi> pois = entry.getValue();
			final String groupName = pois.get(0).getName();
			final boolean isGroupExpanded = groupKey.equals(expandedGroupKey) || !filter.isEmpty();

			listPanel.add(groupRow(groupKey, groupName, pois.size(), isGroupExpanded, 16));

			if (isGroupExpanded)
			{
				renderPoiList(pois, from);
			}
		}
	}

	private void renderPoiList(List<PoiIndex.Poi> pois, WorldPoint from)
	{
		final List<PoiIndex.Poi> sorted = new ArrayList<>(pois);
		if (from != null)
		{
			sorted.sort(Comparator.comparingLong(p -> distanceSq(p.getX(), p.getY(), from)));
		}

		int shown = 0;
		for (PoiIndex.Poi poi : sorted)
		{
			if (shown++ >= MAX_ENTRIES_PER_GROUP)
			{
				listPanel.add(hint("    ... and " + (sorted.size() - MAX_ENTRIES_PER_GROUP) + " more"));
				break;
			}
			listPanel.add(entryRow(poi, from));
		}
	}

	private JPanel categoryRow(MapFinder.BrowseCategory cat, String subtitle, boolean isExpanded)
	{
		return clickableRow(cat.getDisplayName(), subtitle, poiIndex.icon(cat.getIconKey()),
			isExpanded, true, 6, () ->
			{
				expandedCategory = expandedCategory == cat ? null : cat;
				expandedGroupKey = null;
				rebuild();
			});
	}

	private JPanel groupRow(String key, String name, int count, boolean isExpanded, int leftIndent)
	{
		final String countText = countLabel(count, "location");
		return groupRow(key, name, countText, isExpanded, leftIndent);
	}

	private JPanel groupRow(String key, String name, String countText, boolean isExpanded, int leftIndent)
	{
		final java.awt.Image icon = key.startsWith("__") ? null : poiIndex.icon(key);
		return clickableRow(name, countText, icon, isExpanded, true, leftIndent, () ->
			{
				expandedGroupKey = key.equals(expandedGroupKey) ? null : key;
				rebuild();
			});
	}

	private JPanel entryRow(PoiIndex.Poi poi, WorldPoint from)
	{
		final WorldPoint wp = new WorldPoint(poi.getX(), poi.getY(), poi.getPlane());
		final String detail = from == null
			? poi.getX() + ", " + poi.getY()
			: poi.getX() + ", " + poi.getY() + "  " + tiles(wp, from);

		return clickableRow(poi.getName(), detail, null, false, false, 26,
			() -> centerMapOn(wp));
	}

	private JPanel clickableRow(String title, String detail, java.awt.Image icon,
		boolean isExpanded, boolean showChevron, int leftIndent, Runnable action)
	{
		final Color baseBg = isExpanded ? ROW_HOVER : ROW_BG;
		final JPanel row = new JPanel(new GridBagLayout());
		row.setBackground(baseBg);
		row.setBorder(new EmptyBorder(4, leftIndent, 4, 6));
		row.setCursor(new Cursor(Cursor.HAND_CURSOR));
		row.setAlignmentX(Component.LEFT_ALIGNMENT);
		row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

		final GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.gridheight = 2;
		c.anchor = GridBagConstraints.WEST;
		c.insets = new Insets(0, 0, 0, 6);

		if (icon != null)
		{
			row.add(new JLabel(new ImageIcon(icon)), c);
		}

		c.gridx = 1;
		c.gridheight = 1;
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;

		final JLabel titleLabel = new JLabel(title);
		titleLabel.setFont(leftIndent <= 6 ? FontManager.getRunescapeBoldFont() : FontManager.getRunescapeSmallFont());
		titleLabel.setForeground(Color.WHITE);
		row.add(titleLabel, c);

		c.gridy = 1;
		final JLabel detailLabel = new JLabel(detail);
		detailLabel.setFont(FontManager.getDefaultFont().deriveFont(10f));
		detailLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		row.add(detailLabel, c);

		if (showChevron)
		{
			c.gridx = 2;
			c.gridy = 0;
			c.gridheight = 2;
			c.weightx = 0;
			c.fill = GridBagConstraints.NONE;
			c.anchor = GridBagConstraints.EAST;
			c.insets = new Insets(0, 4, 0, 2);

			final JLabel chevronLabel = new JLabel(isExpanded ? "▼" : "▸");
			chevronLabel.setFont(FontManager.getDefaultFont().deriveFont(9f));
			chevronLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			row.add(chevronLabel, c);
		}

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
				row.setBackground(baseBg);
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

	private static int distanceToRegionCenter(MapRegion region, WorldPoint from)
	{
		if (from == null)
		{
			return -1;
		}
		return distanceTiles(new WorldPoint(region.getCenterX(), region.getCenterY(), 0), from);
	}

	private static int distanceTiles(WorldPoint point, WorldPoint from)
	{
		if (from == null || point == null)
		{
			return -1;
		}
		final int dx = point.getX() - from.getX();
		final int dy = point.getY() - from.getY();
		return (int) Math.sqrt((double) dx * dx + (double) dy * dy);
	}

	private static long distanceSq(int x, int y, WorldPoint from)
	{
		final long dx = (long) x - from.getX();
		final long dy = (long) y - from.getY();
		return dx * dx + dy * dy;
	}

	private static String tiles(WorldPoint point, WorldPoint from)
	{
		final int dist = distanceTiles(point, from);
		return dist >= 0 ? dist + " tiles" : "";
	}
}