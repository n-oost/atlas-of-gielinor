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
package atlasofgielinor.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;

/** Stable boss identifiers with metadata loaded from the bundled catalog on a background worker. */
@Slf4j
public final class BossLocationData
{

	private static final String RESOURCE = "/atlasofgielinor/data/bosses.json.gz";
	private static volatile Map<String, BossLocationData> entries = Collections.emptyMap();

	private final String id;
	private final Detail detail;

	private BossLocationData(JsonObject row)
	{
		id = row.get("id").getAsString();
		if (id.isEmpty()) throw new IllegalArgumentException("Empty catalog identifier");
		detail = new Detail(row);
	}

	public String name()
	{
		return id;
	}

	/** All entries in bundled catalog order; empty until the startup worker publishes them. */
	public static BossLocationData[] values()
	{
		return entries.values().toArray(new BossLocationData[0]);
	}

	public static BossLocationData valueOf(String id)
	{
		final BossLocationData entry = entries.get(id);
		if (entry == null) throw new IllegalArgumentException("Unknown catalog identifier: " + id);
		return entry;
	}

	public static boolean isLoaded()
	{
		return !entries.isEmpty();
	}

	/** Call off the client thread before boss search, rendering or authored positions are loaded. */
	public static synchronized void load(Gson gson)
	{
		if (isLoaded()) return;
		final Map<String, BossLocationData> parsed = new LinkedHashMap<>();
		try (InputStream raw = BossLocationData.class.getResourceAsStream(RESOURCE))
		{
			if (raw == null) throw new IOException("Missing bundled boss catalog: " + RESOURCE);
			try (InputStreamReader reader = new InputStreamReader(new GZIPInputStream(raw), StandardCharsets.UTF_8))
			{
				final JsonObject dataset = gson.fromJson(reader, JsonObject.class);
				for (JsonElement element : dataset.getAsJsonArray("bosses"))
				{
					if (Thread.currentThread().isInterrupted()) return;
					final JsonObject row = element.getAsJsonObject();
					final BossLocationData boss = new BossLocationData(row);
					if (parsed.put(boss.name(), boss) != null)
					{
						throw new IOException("Duplicate boss identifier: " + boss.name());
					}
				}
			}
			if (parsed.isEmpty()) throw new IOException("Incomplete bundled boss catalog");
			if (Thread.currentThread().isInterrupted()) return;
			entries = Collections.unmodifiableMap(parsed);
		}
		catch (IOException | RuntimeException e)
		{
			log.warn("Could not load bundled boss catalog", e);
		}
	}

	public String getName()
	{
		return detail.name;
	}

	public int getCombatLevel()
	{
		return detail.combatLevel;
	}

	public String getWeaknessStrategy()
	{
		return detail.weaknessStrategy;
	}

	public String getLocationName()
	{
		return detail.locationName;
	}

	public WorldPoint getWorldPoint()
	{
		return detail.worldPoint;
	}

	public String getKeyDrops()
	{
		return detail.keyDrops;
	}


	@Override
	public String toString()
	{
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
