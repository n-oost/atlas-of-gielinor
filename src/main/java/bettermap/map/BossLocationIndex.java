/*
 * Copyright (c) 2026, n-oost
 * All rights reserved.
 */
package bettermap.map;

import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import bettermap.data.DungeonPiece;
import bettermap.data.DungeonPieceTransform;
import bettermap.data.MonsterLocationData;
import bettermap.data.UndergroundZone;

/** Authored boss marker positions, including optional dungeon floor assignments. */
public final class BossLocationIndex
{
	private static final String RESOURCE = "/com/bettermap/dungeons/bosses.tsv";
	private static final List<Location> LOCATIONS = load();

	private BossLocationIndex()
	{
	}

	public static List<Location> all()
	{
		return LOCATIONS;
	}

	private static List<Location> load()
	{
		final Map<MonsterLocationData, Location> authored = new EnumMap<>(MonsterLocationData.class);
		try (InputStream in = BossLocationIndex.class.getResourceAsStream(RESOURCE))
		{
			if (in != null)
			{
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)))
				{
					String line;
					while ((line = reader.readLine()) != null)
					{
						if (line.isEmpty() || line.charAt(0) == '#')
						{
							continue;
						}
						final String[] f = line.split("\\t", -1);
						if (f.length != 6)
						{
							continue;
						}
						try
						{
							final MonsterLocationData boss = MonsterLocationData.valueOf(f[0]);
						final String zone = "-".equals(f[1]) ? "" : f[1];
						final UndergroundZone undergroundZone = zone.isEmpty() ? null : UndergroundZone.byId(zone);
						if (!zone.isEmpty() && undergroundZone == null)
						{
							continue;
						}
						authored.put(boss, new Location(boss, zone,
							Integer.parseInt(f[2]), Integer.parseInt(f[3]), Integer.parseInt(f[4]), Integer.parseInt(f[5])));
						}
						catch (IllegalArgumentException ignored)
						{
							// Skip malformed authored entries.
						}
					}
				}
			}
		}
		catch (IOException ignored)
		{
			// Bundled defaults below keep boss markers available if the resource is unreadable.
		}
		final List<Location> locations = new ArrayList<>();
		for (MonsterLocationData boss : MonsterLocationData.values())
		{
			final Location location = authored.get(boss);
			if (location != null)
			{
				locations.add(location);
			}
			else
			{
				locations.add(new Location(boss, "", boss.getWorldPoint().getPlane(), 0,
					boss.getWorldPoint().getX(), boss.getWorldPoint().getY()));
			}
		}
		return Collections.unmodifiableList(locations);
	}

	public static final class Location
	{
		public final MonsterLocationData boss;
		public final String zoneId;
		public final int plane;
		public final int layer;
		public final int x;
		public final int y;

		Location(MonsterLocationData boss, String zoneId, int plane, int layer, int x, int y)
		{
			this.boss = boss;
			this.zoneId = zoneId;
			this.plane = plane;
			this.layer = layer;
			this.x = x;
			this.y = y;
		}

		public boolean visibleInFocusedLayer(MapCamera camera)
		{
			if (zoneId == null || zoneId.isEmpty())
			{
				return true;
			}
			final UndergroundZone zone = UndergroundZone.byId(zoneId);
			if (zone == null || camera.getFocusedUndergroundZone() != zone)
			{
				return false;
			}
			final Integer hoveredPlane = camera.getHoveredUndergroundZone() == zone ? camera.getHoveredFloorPlane() : null;
			final int visiblePlane = hoveredPlane != null ? hoveredPlane
				: camera.getActiveUndergroundZone() == zone ? camera.getPlane() : zone.getUndergroundPoint().getPlane();
			if (plane != visiblePlane)
			{
				return false;
			}
			final Integer layer = camera.floorLayerFor(zone);
			return layer == null || layer == this.layer;
		}

		public Point2D displayPoint(DungeonPieceIndex pieces)
		{
			if (zoneId.isEmpty() || pieces == null)
			{
				return new Point2D.Double(x, y);
			}
			for (DungeonPiece piece : pieces.piecesFor(zoneId))
			{
				if (piece.layer != layer || piece.plane != plane || !piece.containsNative(x, y))
				{
					continue;
				}
				final int[] b = piece.srcBounds();
				final double cx = (b[0] + b[2] + 1) / 2.0 + piece.dx;
				final double cy = (b[1] + b[3] + 1) / 2.0 + piece.dy;
				// Screen Y points down; world Y points north, so world rotation has the opposite sign.
				final Point2D center = DungeonPieceTransform.affine(-piece.rot, piece.flipX, piece.flipY, cx, cy)
					.transform(new Point2D.Double(x + 0.5 + piece.dx, y + 0.5 + piece.dy), null);
				return new Point2D.Double(center.getX() - 0.5, center.getY() - 0.5);
			}
			return new Point2D.Double(x, y);
		}
	}
}
