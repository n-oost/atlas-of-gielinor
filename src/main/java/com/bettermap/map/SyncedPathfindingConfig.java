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
package com.bettermap.map;

import com.bettermap.BetterMapConfig;
import com.bettermap.pathfinding.JewelleryBoxTier;
import com.bettermap.pathfinding.ShortestPathConfig;
import com.bettermap.pathfinding.TeleportationItem;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.config.ConfigManager;

/**
 * The {@link ShortestPathConfig} Better Map's vendored engine ({@code com.bettermap.pathfinding},
 * see {@link RoutePlanner}) is actually built with.
 *
 * <p>External mode reads saved settings from the enabled Shortest Path plugin through
 * ConfigManager, without reflection or changing that plugin's settings. Better Map mode
 * uses Better Map's own routing settings even when the external plugin is enabled.
 * Unexposed settings use the pinned engine's defaults.
 *
 * <p>The tracker prevents external-mode searches when that plugin is unavailable.
 * Both modes use the pinned engine and data, so external mode is not an exact route mirror.
 */
@Singleton
public class SyncedPathfindingConfig implements ShortestPathConfig
{
	private static final String GROUP = "shortestpath";

	/**
	 * Seam over {@link ConfigManager#getConfiguration(String, String, Class)} so this class can
	 * be unit tested without constructing RuneLite's real {@code ConfigManager} (a heavyweight,
	 * DI-only class). The {@code @Inject} constructor below wires the real thing in production.
	 */
	interface ExternalConfigReader
	{
		<T> T get(String key, Class<T> type);
	}

	private final ExternalConfigReader reader;
	private final BetterMapConfig config;
	private final ShortestPathTracker bridge;

	@Inject
	SyncedPathfindingConfig(ConfigManager configManager, BetterMapConfig config, ShortestPathTracker bridge)
	{
		this(new ExternalConfigReader()
		{
			@Override
			public <T> T get(String key, Class<T> type)
			{
				return configManager.getConfiguration(GROUP, key, type);
			}
		}, config, bridge);
	}

	SyncedPathfindingConfig(ExternalConfigReader reader, BetterMapConfig config, ShortestPathTracker bridge)
	{
		this.reader = reader;
		this.config = config;
		this.bridge = bridge;
	}

	private <T> T external(String key, Class<T> type)
	{
		return reader.get(key, type);
	}

	@Override
	public boolean avoidWilderness()
	{
		if (config.useExternalShortestPathSettings() && bridge.isAvailable())
		{
			final Boolean v = external("avoidWilderness", Boolean.class);
			return v != null ? v : ShortestPathConfig.super.avoidWilderness();
		}
		return config.routeAvoidWilderness();
	}

	@Override
	public boolean useAgilityShortcuts()
	{
		if (config.useExternalShortestPathSettings() && bridge.isAvailable())
		{
			final Boolean v = external("useAgilityShortcuts", Boolean.class);
			return v != null ? v : ShortestPathConfig.super.useAgilityShortcuts();
		}
		return config.routeUseAgilityShortcuts();
	}

	@Override
	public boolean useGrappleShortcuts()
	{
		if (config.useExternalShortestPathSettings() && bridge.isAvailable())
		{
			final Boolean v = external("useGrappleShortcuts", Boolean.class);
			return v != null ? v : ShortestPathConfig.super.useGrappleShortcuts();
		}
		return config.routeUseGrappleShortcuts();
	}

	@Override
	public boolean useBoats()
	{
		if (config.useExternalShortestPathSettings() && bridge.isAvailable())
		{
			final Boolean v = external("useBoats", Boolean.class);
			return v != null ? v : ShortestPathConfig.super.useBoats();
		}
		return config.routeUseBoats();
	}

	@Override
	public boolean useCanoes()
	{
		if (config.useExternalShortestPathSettings() && bridge.isAvailable())
		{
			final Boolean v = external("useCanoes", Boolean.class);
			return v != null ? v : ShortestPathConfig.super.useCanoes();
		}
		return config.routeUseCanoes();
	}

	@Override
	public boolean useCharterShips()
	{
		if (config.useExternalShortestPathSettings() && bridge.isAvailable())
		{
			final Boolean v = external("useCharterShips", Boolean.class);
			return v != null ? v : ShortestPathConfig.super.useCharterShips();
		}
		return config.routeUseCharterShips();
	}

	@Override
	public boolean useShips()
	{
		if (config.useExternalShortestPathSettings() && bridge.isAvailable())
		{
			final Boolean v = external("useShips", Boolean.class);
			return v != null ? v : ShortestPathConfig.super.useShips();
		}
		return config.routeUseShips();
	}

	@Override
	public boolean useFairyRings()
	{
		if (config.useExternalShortestPathSettings() && bridge.isAvailable())
		{
			final Boolean v = external("useFairyRings", Boolean.class);
			return v != null ? v : ShortestPathConfig.super.useFairyRings();
		}
		return config.routeUseFairyRings();
	}

	@Override
	public boolean useGnomeGliders()
	{
		if (config.useExternalShortestPathSettings() && bridge.isAvailable())
		{
			final Boolean v = external("useGnomeGliders", Boolean.class);
			return v != null ? v : ShortestPathConfig.super.useGnomeGliders();
		}
		return config.routeUseGnomeGliders();
	}

	@Override
	public boolean useHotAirBalloons()
	{
		if (config.useExternalShortestPathSettings() && bridge.isAvailable())
		{
			final Boolean v = external("useHotAirBalloons", Boolean.class);
			return v != null ? v : ShortestPathConfig.super.useHotAirBalloons();
		}
		return config.routeUseHotAirBalloons();
	}

	@Override
	public boolean useMagicCarpets()
	{
		if (config.useExternalShortestPathSettings() && bridge.isAvailable())
		{
			final Boolean v = external("useMagicCarpets", Boolean.class);
			return v != null ? v : ShortestPathConfig.super.useMagicCarpets();
		}
		return config.routeUseMagicCarpets();
	}

	@Override
	public boolean useMagicMushtrees()
	{
		if (config.useExternalShortestPathSettings() && bridge.isAvailable())
		{
			final Boolean v = external("useMagicMushtrees", Boolean.class);
			return v != null ? v : ShortestPathConfig.super.useMagicMushtrees();
		}
		return config.routeUseMagicMushtrees();
	}

	@Override
	public boolean useMinecarts()
	{
		if (config.useExternalShortestPathSettings() && bridge.isAvailable())
		{
			final Boolean v = external("useMinecarts", Boolean.class);
			return v != null ? v : ShortestPathConfig.super.useMinecarts();
		}
		return config.routeUseMinecarts();
	}

	@Override
	public boolean useQuetzals()
	{
		if (config.useExternalShortestPathSettings() && bridge.isAvailable())
		{
			final Boolean v = external("useQuetzals", Boolean.class);
			return v != null ? v : ShortestPathConfig.super.useQuetzals();
		}
		return config.routeUseQuetzals();
	}

	@Override
	public boolean useSpiritTrees()
	{
		if (config.useExternalShortestPathSettings() && bridge.isAvailable())
		{
			final Boolean v = external("useSpiritTrees", Boolean.class);
			return v != null ? v : ShortestPathConfig.super.useSpiritTrees();
		}
		return config.routeUseSpiritTrees();
	}

	@Override
	public TeleportationItem useTeleportationItems()
	{
		if (config.useExternalShortestPathSettings() && bridge.isAvailable())
		{
			final TeleportationItem v = external("useTeleportationItems", TeleportationItem.class);
			return v != null ? v : ShortestPathConfig.super.useTeleportationItems();
		}
		return config.routeUseTeleportationItems();
	}

	@Override
	public boolean useTeleportationLevers()
	{
		if (config.useExternalShortestPathSettings() && bridge.isAvailable())
		{
			final Boolean v = external("useTeleportationLevers", Boolean.class);
			return v != null ? v : ShortestPathConfig.super.useTeleportationLevers();
		}
		return config.routeUseTeleportationLevers();
	}

	@Override
	public boolean useTeleportationPortals()
	{
		if (config.useExternalShortestPathSettings() && bridge.isAvailable())
		{
			final Boolean v = external("useTeleportationPortals", Boolean.class);
			return v != null ? v : ShortestPathConfig.super.useTeleportationPortals();
		}
		return config.routeUseTeleportationPortals();
	}

	@Override
	public boolean useTeleportationSpells()
	{
		if (config.useExternalShortestPathSettings() && bridge.isAvailable())
		{
			final Boolean v = external("useTeleportationSpells", Boolean.class);
			return v != null ? v : ShortestPathConfig.super.useTeleportationSpells();
		}
		return config.routeUseTeleportationSpells();
	}

	@Override
	public boolean useTeleportationSpellsHome()
	{
		if (config.useExternalShortestPathSettings() && bridge.isAvailable())
		{
			final Boolean v = external("useTeleportationSpellsHome", Boolean.class);
			return v != null ? v : ShortestPathConfig.super.useTeleportationSpellsHome();
		}
		return config.routeUseTeleportationSpellsHome();
	}

	@Override
	public boolean useTeleportationMinigames()
	{
		if (config.useExternalShortestPathSettings() && bridge.isAvailable())
		{
			final Boolean v = external("useTeleportationMinigames", Boolean.class);
			return v != null ? v : ShortestPathConfig.super.useTeleportationMinigames();
		}
		return config.routeUseTeleportationMinigames();
	}

	@Override
	public boolean useWildernessObelisks()
	{
		if (config.useExternalShortestPathSettings() && bridge.isAvailable())
		{
			final Boolean v = external("useWildernessObelisks", Boolean.class);
			return v != null ? v : ShortestPathConfig.super.useWildernessObelisks();
		}
		return config.routeUseWildernessObelisks();
	}

	@Override
	public boolean useSeasonalTransports()
	{
		if (config.useExternalShortestPathSettings() && bridge.isAvailable())
		{
			final Boolean v = external("useSeasonalTransports", Boolean.class);
			return v != null ? v : ShortestPathConfig.super.useSeasonalTransports();
		}
		return config.routeUseSeasonalTransports();
	}

	@Override
	public int currencyThreshold()
	{
		if (config.useExternalShortestPathSettings() && bridge.isAvailable())
		{
			final Integer v = external("currencyThreshold", Integer.class);
			return v != null ? v : ShortestPathConfig.super.currencyThreshold();
		}
		return config.routeCurrencyThreshold();
	}

	@Override
	public boolean cancelInstead()
	{
		if (config.useExternalShortestPathSettings() && bridge.isAvailable())
		{
			final Boolean v = external("cancelInstead", Boolean.class);
			return v != null ? v : ShortestPathConfig.super.cancelInstead();
		}
		return config.routeCancelInstead();
	}

	@Override
	public int recalculateDistance()
	{
		if (config.useExternalShortestPathSettings() && bridge.isAvailable())
		{
			final Integer v = external("recalculateDistance", Integer.class);
			return v != null ? v : ShortestPathConfig.super.recalculateDistance();
		}
		return config.routeRecalculateDistance();
	}

	@Override
	public int reachedDistance()
	{
		if (config.useExternalShortestPathSettings() && bridge.isAvailable())
		{
			final Integer v = external("finishDistance", Integer.class);
			return v != null ? v : ShortestPathConfig.super.reachedDistance();
		}
		return config.routeFinishDistance();
	}

	@Override
	public int unreachableTargetDistance()
	{
		if (config.useExternalShortestPathSettings() && bridge.isAvailable())
		{
			final Integer v = external("unreachableTargetDistanceThreshold", Integer.class);
			return v != null ? v : ShortestPathConfig.super.unreachableTargetDistance();
		}
		return config.routeUnreachableTargetDistance();
	}

	@Override
	public int calculationCutoff()
	{
		if (config.useExternalShortestPathSettings() && bridge.isAvailable())
		{
			final Integer v = external("calculationCutoff", Integer.class);
			return v != null ? v : ShortestPathConfig.super.calculationCutoff();
		}
		return config.routeCalculationCutoff();
	}

	@Override
	public boolean includeBankPath()
	{
		if (config.useExternalShortestPathSettings() && bridge.isAvailable())
		{
			final Boolean v = external("includeBankPath", Boolean.class);
			return v != null ? v : ShortestPathConfig.super.includeBankPath();
		}
		return ShortestPathConfig.super.includeBankPath();
	}

	// --- Player-Owned House: only meaningful when synced from the real plugin. Better Map has
	// no POH section of its own (out of scope), so the "not installed" branch is always the
	// interface default (POH off), same as the DefaultPathfindingConfig this class replaces.

	@Override
	public boolean usePoh()
	{
		if (config.useExternalShortestPathSettings() && bridge.isAvailable())
		{
			final Boolean v = external("usePoh", Boolean.class);
			return v != null ? v : ShortestPathConfig.super.usePoh();
		}
		return ShortestPathConfig.super.usePoh();
	}

	@Override
	public boolean usePohFairyRing()
	{
		if (config.useExternalShortestPathSettings() && bridge.isAvailable())
		{
			final Boolean v = external("usePohFairyRing", Boolean.class);
			return v != null ? v : ShortestPathConfig.super.usePohFairyRing();
		}
		return ShortestPathConfig.super.usePohFairyRing();
	}

	@Override
	public boolean usePohSpiritTree()
	{
		if (config.useExternalShortestPathSettings() && bridge.isAvailable())
		{
			final Boolean v = external("usePohSpiritTree", Boolean.class);
			return v != null ? v : ShortestPathConfig.super.usePohSpiritTree();
		}
		return ShortestPathConfig.super.usePohSpiritTree();
	}

	@Override
	public boolean useTeleportationPortalsPoh()
	{
		if (config.useExternalShortestPathSettings() && bridge.isAvailable())
		{
			final Boolean v = external("useTeleportationPortalsPoh", Boolean.class);
			return v != null ? v : ShortestPathConfig.super.useTeleportationPortalsPoh();
		}
		return ShortestPathConfig.super.useTeleportationPortalsPoh();
	}

	@Override
	public JewelleryBoxTier pohJewelleryBoxTier()
	{
		if (config.useExternalShortestPathSettings() && bridge.isAvailable())
		{
			final JewelleryBoxTier v = external("pohJewelleryBoxTier", JewelleryBoxTier.class);
			return v != null ? v : ShortestPathConfig.super.pohJewelleryBoxTier();
		}
		return ShortestPathConfig.super.pohJewelleryBoxTier();
	}

	@Override
	public boolean usePohMountedItems()
	{
		if (config.useExternalShortestPathSettings() && bridge.isAvailable())
		{
			final Boolean v = external("usePohMountedItems", Boolean.class);
			return v != null ? v : ShortestPathConfig.super.usePohMountedItems();
		}
		return ShortestPathConfig.super.usePohMountedItems();
	}

	@Override
	public boolean usePohObelisk()
	{
		if (config.useExternalShortestPathSettings() && bridge.isAvailable())
		{
			final Boolean v = external("usePohObelisk", Boolean.class);
			return v != null ? v : ShortestPathConfig.super.usePohObelisk();
		}
		return ShortestPathConfig.super.usePohObelisk();
	}

	@Override
	public String builtTeleportationBoxes()
	{
		if (config.useExternalShortestPathSettings() && bridge.isAvailable())
		{
			final String v = external("builtTeleportationBoxes", String.class);
			return v != null ? v : ShortestPathConfig.super.builtTeleportationBoxes();
		}
		return ShortestPathConfig.super.builtTeleportationBoxes();
	}

	@Override
	public void setBuiltTeleportationBoxes(String content)
	{
		// no-op: Better Map must never write into the real plugin's saved config, and has no
		// persisted backing store of its own for this hidden field.
	}

	@Override
	public String builtTeleportationPortalsPoh()
	{
		if (config.useExternalShortestPathSettings() && bridge.isAvailable())
		{
			final String v = external("builtTeleportationPortalsPoh", String.class);
			return v != null ? v : ShortestPathConfig.super.builtTeleportationPortalsPoh();
		}
		return ShortestPathConfig.super.builtTeleportationPortalsPoh();
	}

	@Override
	public void setBuiltTeleportationPortalsPoh(String content)
	{
		// no-op, see setBuiltTeleportationBoxes
	}

	// --- Transport thresholds: fine-tuning knobs, out of scope for Better Map's own section.
	// When the real plugin is present these still sync so routing genuinely matches; otherwise
	// they fall through to the interface defaults (all 0, i.e. no tile-saving requirement).

	@Override
	public int costAgilityShortcuts()
	{
		return thresholdOrDefault("costAgilityShortcuts", ShortestPathConfig.super.costAgilityShortcuts());
	}

	@Override
	public int costGrappleShortcuts()
	{
		return thresholdOrDefault("costGrappleShortcuts", ShortestPathConfig.super.costGrappleShortcuts());
	}

	@Override
	public int costBoats()
	{
		return thresholdOrDefault("costBoats", ShortestPathConfig.super.costBoats());
	}

	@Override
	public int costCanoes()
	{
		return thresholdOrDefault("costCanoes", ShortestPathConfig.super.costCanoes());
	}

	@Override
	public int costCharterShips()
	{
		return thresholdOrDefault("costCharterShips", ShortestPathConfig.super.costCharterShips());
	}

	@Override
	public int costShips()
	{
		return thresholdOrDefault("costShips", ShortestPathConfig.super.costShips());
	}

	@Override
	public int costFairyRings()
	{
		return thresholdOrDefault("costFairyRings", ShortestPathConfig.super.costFairyRings());
	}

	@Override
	public int costGnomeGliders()
	{
		return thresholdOrDefault("costGnomeGliders", ShortestPathConfig.super.costGnomeGliders());
	}

	@Override
	public int costHotAirBalloons()
	{
		return thresholdOrDefault("costHotAirBalloons", ShortestPathConfig.super.costHotAirBalloons());
	}

	@Override
	public int costMagicCarpets()
	{
		return thresholdOrDefault("costMagicCarpets", ShortestPathConfig.super.costMagicCarpets());
	}

	@Override
	public int costMagicMushtrees()
	{
		return thresholdOrDefault("costMagicMushtrees", ShortestPathConfig.super.costMagicMushtrees());
	}

	@Override
	public int costMinecarts()
	{
		return thresholdOrDefault("costMinecarts", ShortestPathConfig.super.costMinecarts());
	}

	@Override
	public int costQuetzals()
	{
		return thresholdOrDefault("costQuetzals", ShortestPathConfig.super.costQuetzals());
	}

	@Override
	public int costQuetzalWhistle()
	{
		return thresholdOrDefault("costQuetzalWhistle", ShortestPathConfig.super.costQuetzalWhistle());
	}

	@Override
	public int costSpiritTrees()
	{
		return thresholdOrDefault("costSpiritTrees", ShortestPathConfig.super.costSpiritTrees());
	}

	@Override
	public int costNonConsumableTeleportationItems()
	{
		return thresholdOrDefault("costNonConsumableTeleportationItems",
			ShortestPathConfig.super.costNonConsumableTeleportationItems());
	}

	@Override
	public int costConsumableTeleportationItems()
	{
		return thresholdOrDefault("costConsumableTeleportationItems",
			ShortestPathConfig.super.costConsumableTeleportationItems());
	}

	@Override
	public int costTeleportationBoxes()
	{
		return thresholdOrDefault("costTeleportationBoxes", ShortestPathConfig.super.costTeleportationBoxes());
	}

	@Override
	public int costTeleportationLevers()
	{
		return thresholdOrDefault("costTeleportationLevers", ShortestPathConfig.super.costTeleportationLevers());
	}

	@Override
	public int costTeleportationPortals()
	{
		return thresholdOrDefault("costTeleportationPortals", ShortestPathConfig.super.costTeleportationPortals());
	}

	@Override
	public int costTeleportationSpells()
	{
		return thresholdOrDefault("costTeleportationSpells", ShortestPathConfig.super.costTeleportationSpells());
	}

	@Override
	public int costTeleportationSpellsHome()
	{
		return thresholdOrDefault("costTeleportationSpellsHome",
			ShortestPathConfig.super.costTeleportationSpellsHome());
	}

	@Override
	public int costTeleportationMinigames()
	{
		return thresholdOrDefault("costTeleportationMinigames",
			ShortestPathConfig.super.costTeleportationMinigames());
	}

	@Override
	public int costWildernessObelisks()
	{
		return thresholdOrDefault("costWildernessObelisks", ShortestPathConfig.super.costWildernessObelisks());
	}

	@Override
	public int costSeasonalTransports()
	{
		return thresholdOrDefault("costSeasonalTransports", ShortestPathConfig.super.costSeasonalTransports());
	}

	private int thresholdOrDefault(String key, int fallback)
	{
		if (config.useExternalShortestPathSettings() && bridge.isAvailable())
		{
			final Integer v = external(key, Integer.class);
			return v != null ? v : fallback;
		}
		return fallback;
	}
}
