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
package atlasofgielinor.map.finder;

import atlasofgielinor.data.io.BundledTsv;
import atlasofgielinor.map.catalog.MapCatalog;
import atlasofgielinor.map.catalog.PoiIndex;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Prepared on the POI loading worker; search and browse share the same presentation records. */
public final class FinderPoiIndex
{
	private static final String ROOT = "/atlasofgielinor/catalog/";

	private FinderPoiIndex() {}

	public static List<Entry> build(List<PoiIndex.Poi> searchable, List<PoiIndex.Poi> rendered) throws IOException
	{
		final Map<String, Subject> subjects = new LinkedHashMap<>();
		for (String[] row : BundledTsv.read(ROOT + "finder_subjects.tsv", 5))
		{
			if ("id".equals(row[0])) continue;
			if (row[0].isBlank() || row[1].isBlank() || row[2].isBlank()
				|| !("true".equals(row[3]) || "false".equals(row[3])))
				throw new IOException("Invalid Finder subject: " + row[0]);
			if (subjects.put(row[0], new Subject(row[0], row[1], row[2], Boolean.parseBoolean(row[3]))) != null)
				throw new IOException("Duplicate Finder subject: " + row[0]);
		}
		final Map<String, List<Subject>> associations = new HashMap<>();
		final Set<String> seen = new HashSet<>();
		for (String[] row : BundledTsv.read(ROOT + "finder_location_subjects.tsv", 2))
		{
			if ("location_id".equals(row[0])) continue;
			final Subject subject = subjects.get(row[1]);
			if (subject == null || !MapCatalog.current().locations.containsKey(row[0]) || !seen.add(row[0] + ":" + row[1]))
				throw new IOException("Invalid Finder association: " + row[0] + "/" + row[1]);
			associations.computeIfAbsent(row[0], k -> new ArrayList<>()).add(subject);
		}
		final Set<String> visible = new HashSet<>();
		for (PoiIndex.Poi poi : rendered) visible.add(poi.getLocationId());
		final Map<String, PoiIndex.Poi> locations = new LinkedHashMap<>();
		final Map<String, List<String>> names = new HashMap<>();
		for (PoiIndex.Poi poi : searchable)
		{
			locations.putIfAbsent(poi.getLocationId(), poi);
			names.computeIfAbsent(poi.getLocationId(), k -> new ArrayList<>()).add(poi.getName());
		}
		final List<Entry> entries = new ArrayList<>();
		for (Map.Entry<String, PoiIndex.Poi> source : locations.entrySet())
		{
			final MapCatalog.Location location = MapCatalog.current().locations.get(source.getKey());
			if (location == null) continue;
			final List<Subject> linked = associations.get(source.getKey());
			if (linked == null) throw new IOException("Missing Finder subjects: " + source.getKey());
			final List<String> aliases = new ArrayList<>(names.get(source.getKey()));
			aliases.add(location.detailTitle);
			for (Subject subject : linked)
			{
				entries.add(new Entry(subject, source.getValue(), location.detailTitle,
					location.details.replace("\n", " · "), Collections.unmodifiableList(aliases),
					visible.contains(source.getKey())));
			}
		}
		return Collections.unmodifiableList(entries);
	}

	@Getter
	@RequiredArgsConstructor
	public static final class Subject
	{
		private final String id;
		private final String name;
		private final String type;
		private final boolean resource;
	}

	@Getter
	@RequiredArgsConstructor
	public static final class Entry
	{
		private final Subject subject;
		private final PoiIndex.Poi poi;
		private final String locationName;
		private final String details;
		private final List<String> aliases;
		private final boolean browsable;
	}
}
