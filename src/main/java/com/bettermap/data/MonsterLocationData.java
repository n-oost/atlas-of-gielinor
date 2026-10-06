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
package com.bettermap.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;

/** Stable boss identifiers with metadata loaded from the bundled catalog on a background worker. */
@Slf4j
public enum MonsterLocationData
{
	KING_BLACK_DRAGON,
	ZULRAH,
	VORKATH,
	GENERAL_GRAARDOR,
	KREE_ARRA,
	COMMANDER_ZILYANA,
	KRIL_TSUTSAROTH,
	NEX,
	CORPOREAL_BEAST,
	DAGANNOTH_KINGS,
	ABYSSAL_SIRE,
	CHAOS_ELEMENTAL,
	CHAOS_FANATIC,
	CRAZY_ARCHAEOLOGIST,
	DERANGED_ARCHAEOLOGIST,
	THE_HUEYCOATL,
	GEMSTONE_CRAB,
	GIANT_MOLE,
	SARACHNIS,
	KALPHITE_QUEEN,
	BLOOD_MOON,
	BLUE_MOON,
	ECLIPSE_MOON,
	AHRIM_THE_BLIGHTED,
	DHAROK_THE_WRETCHED,
	GUTHAN_THE_INFESTED,
	KARIL_THE_TAINTED,
	TORAG_THE_CORRUPTED,
	VERAC_THE_DEFILED,
	SCORPIA,
	VENENATIS,
	CALLISTO,
	VET_ION,
	SPINDEL,
	REVENANT_MALEDICTUS,
	ARTIO,
	CALVAR_ION,
	DAGANNOTH_PRIME,
	DAGANNOTH_REX,
	DAGANNOTH_SUPREME,
	CERBERUS,
	ALCHEMICAL_HYDRA,
	KRAKEN,
	THERMONUCLEAR_SMOKE_DEVIL,
	ARAXXOR,
	GROTESQUE_GUARDIANS,
	SHELLBANE_GRYPHON,
	OBOR,
	BRYOPHYTA,
	SCURRIUS,
	HESPORI,
	SKOTIZO,
	PHANTOM_MUSPAH,
	DUKE_SUCELLUS,
	THE_LEVIATHAN,
	THE_WHISPERER,
	VARDORVIS,
	THE_NIGHTMARE,
	PHOSANI_S_NIGHTMARE,
	YAMA,
	DOOM_OF_MOKHAIOTL,
	ROYAL_TITANS,
	AMOXLIATL,
	BRUTUS,
	DEMONIC_BRUTUS,
	MAD_ANGEL,
	MAGGOT_KING,
	THE_MIMIC,
	CRYSTALLINE_HUNLLEF,
	CORRUPTED_HUNLLEF,
	TZTOK_JAD,
	TZKAL_ZUK,
	SOL_HEREDIT,
	WINTERTODT,
	TEMPOROSS,
	ZALCANO,
	TEKTON,
	VANGUARD,
	VESPULA,
	VASA_NISTIRIO,
	MUTTADILE,
	GREAT_OLM,
	THE_MAIDEN_OF_SUGADINTI,
	PESTILENT_BLOAT,
	NYLOCAS_VASILIAS,
	SOTETSEG,
	XARPUS,
	VERZIK_VITUR,
	AKKHA,
	BA_BA,
	KEPHRI,
	ZEBAK,
	TUMEKEN_S_WARDEN,
	ELIDINIS_WARDEN;

	private static final String RESOURCE = "/com/bettermap/data/bosses.json.gz";
	private static volatile Map<MonsterLocationData, Detail> details = Collections.emptyMap();

	public static boolean isLoaded()
	{
		return !details.isEmpty();
	}

	/** Call off the client thread before boss search, rendering or authored positions are loaded. */
	public static synchronized void load()
	{
		if (isLoaded()) return;
		final Map<MonsterLocationData, Detail> parsed = new EnumMap<>(MonsterLocationData.class);
		try (InputStream raw = MonsterLocationData.class.getResourceAsStream(RESOURCE))
		{
			if (raw == null) throw new IOException("Missing bundled boss catalog: " + RESOURCE);
			try (InputStreamReader reader = new InputStreamReader(new GZIPInputStream(raw), StandardCharsets.UTF_8))
			{
				final JsonObject dataset = new JsonParser().parse(reader).getAsJsonObject();
				for (JsonElement element : dataset.getAsJsonArray("bosses"))
				{
					if (Thread.currentThread().isInterrupted()) return;
					final JsonObject row = element.getAsJsonObject();
					final MonsterLocationData boss = valueOf(row.get("id").getAsString());
					if (parsed.put(boss, new Detail(row)) != null)
					{
						throw new IOException("Duplicate boss identifier: " + boss.name());
					}
				}
			}
			if (parsed.size() != values().length) throw new IOException("Incomplete bundled boss catalog");
			if (Thread.currentThread().isInterrupted()) return;
			details = Collections.unmodifiableMap(parsed);
		}
		catch (IOException | RuntimeException e)
		{
			log.warn("Could not load bundled boss catalog", e);
		}
	}

	public String getName()
	{
		return detail().name;
	}

	public int getCombatLevel()
	{
		return detail().combatLevel;
	}

	public String getWeaknessStrategy()
	{
		return detail().weaknessStrategy;
	}

	public String getLocationName()
	{
		return detail().locationName;
	}

	public WorldPoint getWorldPoint()
	{
		return detail().worldPoint;
	}

	public String getKeyDrops()
	{
		return detail().keyDrops;
	}

	private Detail detail()
	{
		final Detail detail = details.get(this);
		if (detail == null) throw new IllegalStateException("Boss catalog is not loaded");
		return detail;
	}

	@Override
	public String toString()
	{
		final Detail detail = detail();
		return detail.combatLevel > 0 ? detail.name + " (Lvl " + detail.combatLevel + ")" : detail.name;
	}

	private static final class Detail
	{
		private final String name;
		private final int combatLevel;
		private final String weaknessStrategy;
		private final String locationName;
		private final WorldPoint worldPoint;
		private final String keyDrops;

		private Detail(JsonObject row)
		{
			name = row.get("name").getAsString();
			combatLevel = row.get("combatLevel").getAsInt();
			weaknessStrategy = row.get("weaknessStrategy").getAsString();
			locationName = row.get("locationName").getAsString();
			keyDrops = row.get("keyDrops").getAsString();
			final JsonObject point = row.getAsJsonObject("worldPoint");
			worldPoint = new WorldPoint(point.get("x").getAsInt(), point.get("y").getAsInt(), point.get("plane").getAsInt());
		}
	}
}
