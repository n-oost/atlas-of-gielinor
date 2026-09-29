package com.bettermap.pathfinding;

/**
 * Plain-object {@link ShortestPathConfig} used to drive the vendored
 * {@code PathfinderConfig} without registering a RuneLite config group.
 *
 * <p>
 * Every routing-relevant getter on {@link ShortestPathConfig} is a {@code default}
 * method returning upstream's default (Skretzo/shortest-path, BSD-2-Clause; see
 * {@code LICENSE} in this package), so this class only needs to satisfy the two
 * abstract {@code setBuilt*} sinks. They are no-ops: the corresponding
 * {@code builtTeleportationBoxes()} / {@code builtTeleportationPortalsPoh()}
 * getters keep returning their empty-string defaults, matching a fresh profile.
 * </p>
 */
public class DefaultPathfindingConfig implements ShortestPathConfig
{
	@Override
	public void setBuiltTeleportationBoxes(String content)
	{
		// no-op: no persisted config backing store
	}

	@Override
	public void setBuiltTeleportationPortalsPoh(String content)
	{
		// no-op: no persisted config backing store
	}
}
