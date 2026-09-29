package com.bettermap.pathfinding;

import java.io.InputStream;

/**
 * Classpath resolver for the vendored pathfinding data files (collision map,
 * transport TSVs, destination TSVs, league regions).
 *
 * <p>
 * Upstream (Skretzo/shortest-path, BSD-2-Clause; see {@code LICENSE} in this
 * package) loaded these via {@code ShortestPathPlugin.class.getResourceAsStream("/foo")}
 * with the data files sitting at the jar root. Here they live under
 * {@code /com/bettermap/pathfinding/}, so absolute-looking paths like
 * {@code "/transports/boats.tsv"} are resolved package-relative to this class.
 * </p>
 */
public final class PathfindingResources
{
	private PathfindingResources()
	{
	}

	/**
	 * Opens a vendored resource by its upstream-style path.
	 *
	 * @param path leading-slash-optional path such as {@code "/collision-map.zip"}
	 *             or {@code "transports/boats.tsv"}, resolved relative to the
	 *             {@code com.bettermap.pathfinding} package.
	 * @return the resource stream, or {@code null} if it is not on the classpath.
	 *         Callers preserve upstream's own miss handling (most wrap this in
	 *         {@link java.util.Objects#requireNonNull}; the league loader tolerates
	 *         {@code null}).
	 */
	public static InputStream open(String path)
	{
		String relative = path.startsWith("/") ? path.substring(1) : path;
		return PathfindingResources.class.getResourceAsStream(relative);
	}
}
