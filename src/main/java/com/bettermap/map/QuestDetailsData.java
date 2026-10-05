package com.bettermap.map;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Curated quest metadata loaded by the POI startup worker. */
final class QuestDetailsData
{
	static Map<String, PoiDetails.Detail> load() throws IOException
	{
		final Map<String, PoiDetails.Detail> map = new HashMap<>(432);
		try (InputStream stream = QuestDetailsData.class.getResourceAsStream(
			"/com/bettermap/poi/quest-details.tsv"))
		{
			if (stream == null)
			{
				throw new IOException("Missing quest details resource");
			}
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)))
			{
				String line;
				int lineNumber = 0;
				while ((line = reader.readLine()) != null)
				{
					lineNumber++;
					if (Thread.currentThread().isInterrupted())
					{
						throw new InterruptedIOException("Quest details loading cancelled");
					}
					if (line.isEmpty() || line.charAt(0) == '#')
					{
						continue;
					}
					final String[] fields = line.split("\\t", -1);
					if (fields.length < 3)
					{
						throw new IOException("Invalid quest details at line " + lineNumber);
					}
					final List<String> lines = List.of(Arrays.copyOfRange(fields, 3, fields.length));
					map.put(fields[0], new PoiDetails.Detail(fields[1], fields[2], lines));
				}
			}
		}
		return Collections.unmodifiableMap(map);
	}

	private QuestDetailsData()
	{
	}
}
