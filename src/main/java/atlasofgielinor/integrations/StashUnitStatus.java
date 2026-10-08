/* Copyright (c) 2026, n-oost. All rights reserved. SPDX-License-Identifier: BSD-2-Clause */
package atlasofgielinor.integrations;

import atlasofgielinor.map.catalog.MapCatalog;
import atlasofgielinor.map.catalog.PoiDetails;
import atlasofgielinor.map.catalog.PoiIndex;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ScriptID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.cluescrolls.clues.emote.STASHUnit;

/** Reads optional STASH plugins' saved state; never writes their configuration or tracks game events. */
@Singleton
@Slf4j
public class StashUnitStatus
{
	private final Client client;
	private final ConfigManager configManager;
	private STASHUnit lastUnit;
	private int lastTick = -1;
	private long lastAccount = -1;
	private Boolean built, filled;

	@Inject
	public StashUnitStatus(Client client, ConfigManager configManager)
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
		final boolean available = client.getGameState() == GameState.LOGGED_IN && client.isClientThread();
		if (available && (lastUnit != unit || lastTick != client.getTickCount() || lastAccount != client.getAccountHash()))
		{
			lastUnit = unit;
			lastTick = client.getTickCount();
			lastAccount = client.getAccountHash();
			built = null;
			filled = configManager.getRSProfileKey() == null ? null : emoteFilled(unit.getObjectId(),
				configManager.getRSProfileConfiguration("emote-clue-items", "stashes-filled"),
				configManager.getRSProfileConfiguration("emote-clue-items", "stashes-fingerprint"));
			if (filled == null && lastAccount != -1)
				filled = trackerFilled(unit, configManager.getConfiguration("stashtracker", "filled." + lastAccount));
			try
			{
				client.runScript(ScriptID.WATSON_STASH_UNIT_CHECK, unit.getObjectId(), 0, 0, 0);
				built = client.getIntStack()[0] == 1;
			}
			catch (RuntimeException e) { log.debug("Unable to check STASH build status", e); }
		}
		final List<String> lines = statusLines(available ? built : null, available ? filled : null);
		lines.addAll(detail.getLines());
		return new PoiDetails.Detail(detail.getTitle(), detail.getCategory(), lines);
	}

	/** Fingerprint IDs identify the string positions, so added/reordered units cannot shift another unit's state. */
	static Boolean emoteFilled(int objectId, String states, String fingerprint)
	{
		if (states == null || fingerprint == null || !states.matches("[tf]+")) return null;
		final String[] ids = fingerprint.split("-", -1);
		if (ids.length != states.length()) return null;
		Boolean result = null;
		int previous = -1;
		try
		{
			for (int i = 0; i < ids.length; i++)
			{
				final int id = Integer.parseInt(ids[i]);
				if (id <= previous) return null;
				previous = id;
				if (id == objectId) result = states.charAt(i) == 't';
			}
		}
		catch (NumberFormatException e) { return null; }
		return result;
	}

	/** This provider saves only positive membership; a missing unit does not establish emptiness. */
	static Boolean trackerFilled(STASHUnit unit, String csv)
	{
		if (csv == null) return null;
		String name = unit.name();
		switch (unit)
		{
			case WARRIORS_GUILD_BANK: name = "WARRIORS_GUILD_BANK_ELITE"; break;
			case WARRIORS_GUILD_BANK_29047: name = "WARRIORS_GUILD_BANK_MASTER"; break;
			case _7TH_CHAMBER_OF_JALSAVRAH: name = "SEVENTH_CHAMBER_OF_JALSAVRAH"; break;
			case ORTUS_MEETS_PROUDSPIRE: name = "WHERE_ORTUS_MEETS_PROUDSPIRE"; break;
			default: break;
		}
		for (String entry : csv.split(","))
			if (name.equals(entry.trim())) return true;
		return null;
	}

	static List<String> statusLines(Boolean built, Boolean filled)
	{
		final List<String> lines = new ArrayList<>();
		lines.add("Built: " + label(built));
		lines.add("Filled: " + (Boolean.FALSE.equals(built) ? "No (not built)"
			: filled == null ? "Unknown" : label(filled) + " (saved plugin data)"));
		if (filled == null && !Boolean.FALSE.equals(built))
			lines.add("Filled status needs saved STASH Tracker or Emote Clue Items data.");
		return lines;
	}

	private static String label(Boolean value) { return value == null ? "Unknown" : value ? "Yes" : "No"; }
}
