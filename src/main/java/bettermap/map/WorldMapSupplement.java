package bettermap.map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

/** Bundled RuneLite map metadata. Coordinates describe icons and never create route edges. */
@Slf4j
public final class WorldMapSupplement
{
	private static final List<Entry> ENTRIES = load();

	private WorldMapSupplement()
	{
	}

	public static List<Entry> entries()
	{
		return ENTRIES;
	}

	private static List<Entry> load()
	{
		try (InputStream stream = WorldMapSupplement.class.getResourceAsStream(
			"/com/bettermap/poi/runelite-world-map-data.json"))
		{
			if (stream == null)
			{
				return Collections.emptyList();
			}
			final JsonObject data = new JsonParser().parse(
				new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
			final List<Entry> entries = new ArrayList<>();
			for (JsonElement element : data.getAsJsonArray("rows"))
			{
				entries.add(new Entry(element.getAsJsonObject()));
			}
			return Collections.unmodifiableList(entries);
		}
		catch (Exception e)
		{
			log.debug("Could not load bundled RuneLite world map metadata", e);
			return Collections.emptyList();
		}
	}

	/** Resolve imported metadata only within the same marker category and plane. */
	public static PoiDetails.Detail detail(PoiIndex.Poi poi)
	{
		final Set<String> lines = new LinkedHashSet<>();
		String title = null;
		for (Entry entry : ENTRIES)
		{
			if (!compatible(entry.poi.getKey(), poi.getKey()) || entry.poi.getPlane() != poi.getPlane()
				|| Math.abs(entry.poi.getX() - poi.getX()) > 3 || Math.abs(entry.poi.getY() - poi.getY()) > 3)
			{
				continue;
			}
			if (title == null)
			{
				title = entry.poi.getName();
			}
			lines.addAll(entry.lines);
		}
		return title == null ? null : new PoiDetails.Detail(title, PoiCategory.of(poi.getKey()).getDisplayName(),
			new ArrayList<>(lines));
	}

	public static boolean compatible(String source, String target)
	{
		return source.equals(target)
			|| "runecrafting_altar".equals(source) && ("dungeon".equals(target) || "dungeon_link".equals(target))
			|| "dungeon".equals(source) && "dungeon_link".equals(target);
	}

	/** Compare destination identity across punctuation and skill-level suffixes. */
	public static String identity(String title)
	{
		return title.toLowerCase(Locale.ROOT)
			.replaceAll("\\(?\\b(?:level|lvl)\\s+\\d+\\)?", "")
			.replaceAll("\\b(?:teleport|ancient|lunar|arceuus)\\b", "")
			.replaceAll("[^a-z0-9]+", " ").trim();
	}

	public static final class Entry
	{
		private final PoiIndex.Poi poi;
		private final List<String> lines;

		private Entry(JsonObject row)
		{
			final String tooltip = row.get("tooltip").getAsString().replaceAll("(?i)<br\\s*/?>", "\n")
				.replaceAll("<[^>]*>", "").trim();
			final String title = tooltip.split("\n", 2)[0];
			poi = new PoiIndex.Poi(row.get("x").getAsInt(), row.get("y").getAsInt(),
				row.get("plane").getAsInt(), row.get("key").getAsString(), title);
			final List<String> details = new ArrayList<>();
			for (String line : tooltip.split("\n"))
			{
				if (!line.isBlank()) details.add(line.trim());
			}
			final int level = row.get("level").getAsInt();
			if (level > 0)
			{
				details.add("Requires Level " + level + " " + row.get("skill").getAsString());
			}
			lines = Collections.unmodifiableList(details);
		}

		public PoiIndex.Poi poi()
		{
			return poi;
		}
	}
}
