package com.bettermap.pathfinding;

import java.util.HashMap;
import java.util.Map;
import com.bettermap.pathfinding.transport.TransportType;

/**
 * Static support hooks extracted verbatim from upstream
 * {@code shortestpath.ShortestPathPlugin} (Skretzo/shortest-path, BSD-2-Clause;
 * see {@code LICENSE} in this package). Only the pieces the vendored pathfinding
 * code depends on were carried over: the Player-Owned-House bounding box and the
 * developer config-override hook.
 *
 * <p>
 * {@link #configOverride} is intentionally never populated here &mdash; the
 * upstream {@code PLUGIN_MESSAGE_CONFIG_OVERRIDE} handler was not vendored, so
 * every {@code override(...)} overload is an identity passthrough that returns
 * the supplied default. The map and the {@code isEmpty()} guards are kept so the
 * code stays byte-for-byte comparable with upstream should the hook ever be
 * wired up.
 * </p>
 */
public final class PathfindingSupport
{
	// POH (Player Owned House) bounds for detecting when path goes through POH
	// Note: POH_MIN_X is 1856 to exclude the Daddy's Home miniquest area
	private static final int POH_MIN_X = 1856;
	private static final int POH_MAX_X = 2047;
	private static final int POH_MIN_Y = 5696;
	private static final int POH_MAX_Y = 5767;

	private static final Map<String, Object> configOverride = new HashMap<>(50);

	private PathfindingSupport()
	{
	}

	/**
	 * Checks if the given coordinates are inside the POH (Player Owned House) area.
	 *
	 * @param x The world X coordinate
	 * @param y The world Y coordinate
	 * @return true if inside POH, false otherwise
	 */
	public static boolean isInsidePoh(int x, int y)
	{
		return x >= POH_MIN_X && x <= POH_MAX_X && y >= POH_MIN_Y && y <= POH_MAX_Y;
	}

	public static boolean override(String configOverrideKey, boolean defaultValue)
	{
		if (!configOverride.isEmpty())
		{
			Object value = configOverride.get(configOverrideKey);
			if (value instanceof Boolean)
			{
				return (boolean) value;
			}
		}
		return defaultValue;
	}

	/**
	 * Override for TransportType enabled state using the config key name stored in the enum.
	 */
	public static boolean override(TransportType type, boolean defaultValue)
	{
		String key = type.getEnabledKey();
		return key != null ? override(key, defaultValue) : defaultValue;
	}

	/**
	 * Override for TransportType cost threshold using the config key name stored in the enum.
	 */
	public static int override(TransportType type, int defaultValue)
	{
		String key = type.getCostKey();
		return key != null ? override(key, defaultValue) : defaultValue;
	}

	public static int override(String configOverrideKey, int defaultValue)
	{
		if (!configOverride.isEmpty())
		{
			Object value = configOverride.get(configOverrideKey);
			if (value instanceof Integer)
			{
				return (int) value;
			}
		}
		return defaultValue;
	}

	public static TeleportationItem override(String configOverrideKey, TeleportationItem defaultValue)
	{
		if (!configOverride.isEmpty())
		{
			Object value = configOverride.get(configOverrideKey);
			if (value instanceof String)
			{
				TeleportationItem teleportationItem = TeleportationItem.fromType((String) value);
				if (teleportationItem != null)
				{
					return teleportationItem;
				}
			}
		}
		return defaultValue;
	}

	public static JewelleryBoxTier override(String configOverrideKey, JewelleryBoxTier defaultValue)
	{
		if (!configOverride.isEmpty())
		{
			Object value = configOverride.get(configOverrideKey);
			if (value instanceof String)
			{
				JewelleryBoxTier tier = JewelleryBoxTier.fromType((String) value);
				if (tier != null)
				{
					return tier;
				}
			}
		}
		return defaultValue;
	}
}
