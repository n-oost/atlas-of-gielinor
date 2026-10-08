/* Copyright (c) 2026, n-oost. All rights reserved. SPDX-License-Identifier: BSD-2-Clause */
package atlasofgielinor.integrations;

import atlasofgielinor.map.catalog.MapCatalog;
import atlasofgielinor.map.catalog.PoiDetails;
import atlasofgielinor.map.catalog.PoiIndex;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.ChatMessageType;
import net.runelite.api.GameState;
import net.runelite.api.ScriptID;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetType;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.cluescrolls.clues.emote.STASHUnit;
import net.runelite.client.util.Text;

/** Built status is live; filled status is remembered only after observing the chart or a deposit/withdrawal. */
@Singleton
@Slf4j
public class StashUnitTracker
{
	private static final String CONFIG_GROUP = "bettermap";
	private static final int[] CHARTS = {
		InterfaceID.HideyHoles.BEGINNER_CONTENTS, InterfaceID.HideyHoles.EASY_CONTENTS,
		InterfaceID.HideyHoles.MEDIUM_CONTENTS, InterfaceID.HideyHoles.HARD_CONTENTS,
		InterfaceID.HideyHoles.ELITE_CONTENTS, InterfaceID.HideyHoles.MASTER_CONTENTS
	};
	private static final String[] TIERS = {"Beginner", "Easy", "Medium", "Hard", "Elite", "Master"};
	private final Client client;
	private final ConfigManager configManager;
	private final EnumMap<STASHUnit, Boolean> built = new EnumMap<>(STASHUnit.class);
	private final Map<String, STASHUnit> chartUnits = new HashMap<>();
	private STASHUnit requestedUnit;

	@Inject
	public StashUnitTracker(Client client, ConfigManager configManager)
	{
		this.client = client;
		this.configManager = configManager;
	}

	public static STASHUnit unitFor(PoiIndex.Poi poi)
	{
		if (poi == null || !"stash_unit".equals(poi.getKey())) return null;
		final MapCatalog.Location location = MapCatalog.current().locations.get(poi.getLocationId());
		if (location == null || !location.poiId.startsWith("stash_")) return null;
		try { return STASHUnit.valueOf(location.poiId.substring(6).toUpperCase(Locale.ROOT)); }
		catch (IllegalArgumentException e) { return null; }
	}

	public PoiDetails.Detail enrich(PoiIndex.Poi poi, PoiDetails.Detail detail)
	{
		final STASHUnit unit = unitFor(poi);
		if (unit == null || detail == null) return detail;
		requestedUnit = unit;
		final boolean loggedIn = client.getGameState() == GameState.LOGGED_IN;
		final Boolean unitBuilt = loggedIn ? built.get(unit) : null;
		final Boolean filled = loggedIn && configManager.getRSProfileKey() != null
			? configManager.getRSProfileConfiguration(CONFIG_GROUP, filledKey(unit), Boolean.class) : null;
		final List<String> lines = statusLines(unitBuilt, filled);
		lines.addAll(detail.getLines());
		return new PoiDetails.Detail(detail.getTitle(), detail.getCategory(), lines);
	}

	static List<String> statusLines(Boolean unitBuilt, Boolean filled)
	{
		final List<String> lines = new ArrayList<>();
		lines.add("Built: " + stateLabel(unitBuilt));
		lines.add("Filled: " + (Boolean.FALSE.equals(unitBuilt) ? "No (not built)"
			: filled == null ? "Unknown" : stateLabel(filled) + " (last observed)"));
		if (filled == null && !Boolean.FALSE.equals(unitBuilt))
			lines.add("Open Watson's STASH chart to sync filled status.");
		return lines;
	}

	private static String stateLabel(Boolean value)
	{
		return value == null ? "Unknown" : value ? "Yes" : "No";
	}

	private static String filledKey(STASHUnit unit) { return "stashFilled_" + unit.name(); }

	private void recordFilled(STASHUnit unit, boolean filled)
	{
		if (configManager.getRSProfileKey() == null) return;
		final Boolean previous = configManager.getRSProfileConfiguration(CONFIG_GROUP, filledKey(unit), Boolean.class);
		if (previous == null || previous != filled)
			configManager.setRSProfileConfiguration(CONFIG_GROUP, filledKey(unit), filled);
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (client.getGameState() != GameState.LOGGED_IN) return;
		// Only the hovered unit is queried, and never on every render frame.
		if (requestedUnit != null && !built.containsKey(requestedUnit))
		{
			try
			{
				client.runScript(ScriptID.WATSON_STASH_UNIT_CHECK, requestedUnit.getObjectId(), 0, 0, 0);
				built.put(requestedUnit, client.getIntStack()[0] == 1);
			}
			catch (RuntimeException e) { log.debug("Unable to check STASH build status", e); }
		}
		// The chart is the source of filled state. Zero script inputs do not establish emptiness.
		final Widget chart = client.getWidget(InterfaceID.HideyHoles.CONTENT);
		if (chart == null || chart.isHidden() || !MapCatalog.isLoaded()) return;
		for (int index = 0; index < CHARTS.length; index++)
		{
			final Widget list = client.getWidget(CHARTS[index]);
			if (list == null || list.getChildren() == null) continue;
			String location = null;
			int checkmarks = 0;
			for (Widget child : list.getChildren())
			{
				if (child == null) continue;
				if (child.getType() == WidgetType.TEXT && child.getText() != null)
				{
					if (location != null) recordChartRow(location, TIERS[index], checkmarks);
					location = Text.removeTags(child.getText());
					checkmarks = 0;
				}
				else if (location != null && child.getType() == WidgetType.GRAPHIC)
					checkmarks++;
			}
			if (location != null) recordChartRow(location, TIERS[index], checkmarks);
		}
	}

	private void recordChartRow(String label, String tier, int checkmarks)
	{
		final STASHUnit unit = unitForChartRow(label, tier);
		if (unit != null && checkmarks <= 2)
		{
			built.put(unit, checkmarks >= 1);
			recordFilled(unit, checkmarks >= 2);
		}
	}

	STASHUnit unitForChartRow(String label, String tier)
	{
		if (chartUnits.isEmpty())
			for (STASHUnit unit : STASHUnit.values())
			{
				final MapCatalog.Poi poi = MapCatalog.current().pois.get("stash_" + unit.name().toLowerCase(Locale.ROOT));
				if (poi == null) continue;
				final String unitTier = poi.details.substring("Clue tier: ".length(), poi.details.indexOf('\n'));
				chartUnits.put(unitTier + ':' + normalize(unit.name().replaceFirst("_\\d{5}$", "")), unit);
				for (String alias : poi.aliases)
					if (!"STASH".equals(alias) && !(unitTier + " STASH").equals(alias))
						chartUnits.put(unitTier + ':' + normalize(alias), unit);
			}
		return chartUnits.get(tier + ':' + normalize(label));
	}

	private static String normalize(String name)
	{
		return name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.GAMEMESSAGE && event.getType() != ChatMessageType.SPAM) return;
		final String message = Text.removeTags(event.getMessage());
		final boolean deposit = "You deposit your items into the STASH unit.".equals(message);
		if (!deposit && !"You withdraw your items from the STASH unit.".equals(message)) return;
		if (client.getLocalPlayer() == null) return;
		final WorldPoint player = WorldPoint.fromLocalInstance(client, client.getLocalPlayer().getLocalLocation());
		STASHUnit nearest = null;
		int distance = 10;
		boolean tied = false;
		for (STASHUnit unit : STASHUnit.values())
			for (WorldPoint point : unit.getWorldPoints())
			{
				if (point.getPlane() != player.getPlane()) continue;
				final int dx = point.getX() - player.getX(), dy = point.getY() - player.getY();
				final int squared = dx * dx + dy * dy;
				if (squared < distance) { nearest = unit; distance = squared; tied = false; }
				else if (squared == distance && unit != nearest) tied = true;
			}
		if (nearest != null && !tied)
		{
			built.put(nearest, true);
			recordFilled(nearest, deposit);
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		final int id = event.getVarpId();
		if ((id >= VarPlayerID.HH_CONSTRUCTED_EASY && id <= VarPlayerID.HH_CONSTRUCTED_MASTER)
			|| id == VarPlayerID.HH_CONSTRUCTED_BEGINNER) built.clear();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGIN_SCREEN || event.getGameState() == GameState.HOPPING)
			clear();
	}

	public void clear()
	{
		built.clear();
		chartUnits.clear();
		requestedUnit = null;
	}
}
