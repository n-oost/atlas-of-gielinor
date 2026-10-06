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
package atlasofgielinor.map;

import atlasofgielinor.map.catalog.MonsterIndex;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Collections;
import atlasofgielinor.data.io.BundledTsv;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;

import atlasofgielinor.data.BossLocationData;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.FontManager;

/**
 * Picks an item sprite to stand in for a monster, slayer creature or boss.
 *
 * <p>The sprites come from RuneLite's {@link ItemManager} - heads, trophies, pets, slayer
 * equipment, signature drops and materials. Each one is scaled once and cached, because the map
 * overlay, the hover cards and the sidebar all redraw them every frame.
 */
@Slf4j
@Singleton
public class MonsterIconManager
{
	private final ItemManager itemManager;

	/** Direct name-to-item mapping (case-insensitive). */
	private static Map<String, Integer> exactMap = Collections.emptyMap();

	/** Keyword/substring-to-item mapping for creatures sharing species roots. */
	private static Map<String, Integer> keywordMap = Collections.emptyMap();
	private static volatile boolean mappingsLoaded;

	/** In-memory cache of rendered/scaled monster icons keyed by itemId + size. */
	private final Map<Long, BufferedImage> scaledIconCache = new ConcurrentHashMap<>();

	/** In-memory fallback icon cache for offline/testing mode. */
	private final Map<String, BufferedImage> fallbackIconCache = new ConcurrentHashMap<>();

	@Inject
	public MonsterIconManager(ItemManager itemManager)
	{
		this.itemManager = itemManager;
	}

	/** Constructor for tests or environments without dependency injection. */
	public MonsterIconManager()
	{
		this(null);
	}

	/** Load on the map startup worker; publish both complete tables together. */
	public static synchronized boolean load()
	{
		if (mappingsLoaded) return true;
		final Map<String, Integer> exact = new HashMap<>();
		final Map<String, Integer> keywords = new HashMap<>();
		try
		{
			for (String[] row : BundledTsv.read("/atlasofgielinor/data/monster-icons.tsv", 3))
			{
				final int itemId = Integer.parseInt(row[2]);
				if (row.length != 3 || row[1].isEmpty() || itemId <= 0)
					throw new IOException("Invalid monster icon mapping");
				if ("exact".equals(row[0])) exact.put(normalizeName(row[1]), itemId);
				else if ("keyword".equals(row[0])) keywords.put(row[1], itemId);
				else throw new IOException("Unknown monster icon mapping: " + row[0]);
			}
			if (exact.isEmpty() || keywords.isEmpty()) throw new IOException("Empty monster icon mappings");
			if (Thread.currentThread().isInterrupted()) return false;
			exactMap = exact;
			keywordMap = keywords;
			mappingsLoaded = true;
			return true;
		}
		catch (IOException | IllegalArgumentException e)
		{
			log.debug("Could not load monster icon mappings", e);
			return false;
		}
	}

	/**
	 * Returns a scaled BufferedImage icon for the given monster zone, or a fallback icon if
	 * RuneLite item graphics are not available.
	 */
	public BufferedImage getZoneIcon(MonsterIndex.Zone zone, int targetSize)
	{
		if (zone == null)
		{
			return getFallbackIcon("Monster", targetSize, new Color(224, 72, 72));
		}

		return getIcon(zone.getMonster(), zone.getCombatLevel(), zone.getSlayerLevel(), targetSize);
	}

	/**
	 * Returns a scaled BufferedImage icon for a catalogued boss from {@link BossLocationData}.
	 */
	public BufferedImage getBossIcon(BossLocationData boss, int targetSize)
	{
		if (boss == null)
		{
			return getFallbackIcon("Boss", targetSize, new Color(255, 209, 102));
		}

		return getIcon(boss.getName(), boss.getCombatLevel(), 0, targetSize);
	}

	/**
	 * Scaled sprite for a known item id, sharing this class's icon cache.
	 *
	 * <p>Unlike the monster icons there is nothing to resolve: ground item spawns carry real item
	 * ids, so the map draws the actual item. Returns null when the sprite is unavailable — the
	 * caller draws its own marker rather than a wrong item.
	 */
	public BufferedImage getItemIcon(int itemId, int targetSize)
	{
		if (itemId <= 0 || itemManager == null)
		{
			return null;
		}

		final long cacheKey = (((long) itemId) << 32) | ((long) targetSize & 0xFFFFFFFFL);
		final BufferedImage cached = scaledIconCache.get(cacheKey);
		if (cached != null)
		{
			return cached;
		}

		try
		{
			final BufferedImage rawImage = itemManager.getImage(itemId);
			if (rawImage != null)
			{
				final BufferedImage scaled = scaleImage(rawImage, targetSize);
				scaledIconCache.put(cacheKey, scaled);
				return scaled;
			}
		}
		catch (Exception e)
		{
			log.debug("Could not load item image for item ID {}: {}", itemId, e.getMessage());
		}

		return null;
	}

	/**
	 * Resolves and returns a scaled icon for the specified monster by name, combat level,
	 * and slayer level.
	 */
	public BufferedImage getIcon(String monsterName, int combatLevel, int slayerLevel, int targetSize)
	{
		final BufferedImage icon = getItemIcon(resolveItemId(monsterName, combatLevel, slayerLevel), targetSize);
		if (icon != null)
		{
			return icon;
		}

		// Fallback generated icon when itemManager is unavailable or offline
		final Color theme = slayerLevel > 1 ? new Color(180, 90, 220) : (combatLevel >= 150 ? new Color(255, 209, 102) : new Color(224, 72, 72));
		return getFallbackIcon(monsterName != null ? monsterName : "Monster", targetSize, theme);
	}

	/**
	 * The item id that best stands in for this monster name.
	 */
	public int resolveItemId(String monsterName, int combatLevel, int slayerLevel)
	{
		if (monsterName == null || monsterName.trim().isEmpty())
		{
			return slayerLevel > 1 ? ItemID.SLAYER_HELM : (combatLevel >= 150 ? ItemID.SKULL : ItemID.BONES);
		}

		final String cleanName = normalizeName(monsterName);

		// 1. Exact Name Match
		final Integer exact = mappingsLoaded ? exactMap.get(cleanName) : null;
		if (exact != null)
		{
			return exact;
		}

		// 2. Keyword / Substring Matching
		for (Map.Entry<String, Integer> entry : (mappingsLoaded ? keywordMap : Collections.<String, Integer>emptyMap()).entrySet())
		{
			if (cleanName.contains(entry.getKey()))
			{
				return entry.getValue();
			}
		}

		// 3. Slayer Requirement Fallback
		if (slayerLevel > 1)
		{
			return ItemID.SLAYER_HELM;
		}

		// 4. High-Level Boss / Combat Fallback
		if (combatLevel >= 150)
		{
			return ItemID.SKULL;
		}

		// 5. Default Creature Fallback
		return ItemID.BONES;
	}

	private static String normalizeName(String name)
	{
		return name.toLowerCase(Locale.ROOT)
			.replace("'", "")
			.replace("-", " ")
			.trim();
	}

	private static BufferedImage scaleImage(BufferedImage source, int targetSize)
	{
		if (source.getWidth() == targetSize && source.getHeight() == targetSize)
		{
			return source;
		}

		final BufferedImage scaled = new BufferedImage(targetSize, targetSize, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = scaled.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		final int srcW = source.getWidth();
		final int srcH = source.getHeight();
		final double scale = Math.min((double) targetSize / srcW, (double) targetSize / srcH);
		final int drawW = (int) Math.round(srcW * scale);
		final int drawH = (int) Math.round(srcH * scale);
		final int drawX = (targetSize - drawW) / 2;
		final int drawY = (targetSize - drawH) / 2;

		g.drawImage(source, drawX, drawY, drawW, drawH, null);
		g.dispose();
		return scaled;
	}

	private BufferedImage getFallbackIcon(String name, int targetSize, Color color)
	{
		final String cacheKey = name + "_" + targetSize + "_" + color.getRGB();
		BufferedImage cached = fallbackIconCache.get(cacheKey);
		if (cached != null)
		{
			return cached;
		}

		final BufferedImage img = new BufferedImage(targetSize, targetSize, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		// Badge background
		g.setColor(new Color(16, 19, 26, 220));
		g.fillOval(1, 1, targetSize - 2, targetSize - 2);

		// Border
		g.setColor(color);
		g.drawOval(1, 1, targetSize - 2, targetSize - 2);

		// Letter initial
		final String letter = name.length() > 0 ? name.substring(0, 1).toUpperCase(Locale.ROOT) : "M";
		g.setFont(FontManager.getDefaultBoldFont().deriveFont((float) Math.max(9, targetSize / 2)));
		final int textW = g.getFontMetrics().stringWidth(letter);
		final int textH = g.getFontMetrics().getAscent() - 2;
		g.drawString(letter, (targetSize - textW) / 2, (targetSize + textH) / 2);
		g.dispose();

		fallbackIconCache.put(cacheKey, img);
		return img;
	}

}
