package com.bettermap.map;

import com.bettermap.data.BundledTsv;
import java.io.IOException;
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
		for (String[] fields : BundledTsv.read("/com/bettermap/poi/quest-details.tsv", 3))
		{
			final List<String> lines = List.of(Arrays.copyOfRange(fields, 3, fields.length));
			map.put(fields[0], new PoiDetails.Detail(fields[1], fields[2], lines));
		}
		return Collections.unmodifiableMap(map);
	}

	private QuestDetailsData()
	{
	}
}
