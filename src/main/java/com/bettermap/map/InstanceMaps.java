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

import com.bettermap.data.DungeonPiece;
import com.bettermap.data.OverlayFloor;
import com.bettermap.data.UndergroundZone;
import com.bettermap.tiles.WikiMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import net.runelite.api.coords.WorldPoint;

/**
 * Where instanced / mid-Y cache maps belong on Better Map.
 *
 * <p>The overworld stops at {@link WikiMap#SURFACE}'s max Y (4288). Everything above that line —
 * the instance band, then the +6400 underground band — is a separate layer that the game shows
 * on its own map, and none of it belongs in the overworld view. Each such point gets one of:
 *
 * <ul>
 *   <li><b>Overworld overlay</b> — the instance <i>is</i> the real place (Prifddinas city). Composite
 *       onto Tirannwn. Not a dungeon layer.</li>
 *   <li><b>Dungeon layer</b> — {@link UndergroundZone} hover/click. Markers and travel pins are
 *       projected down to the surface entrance so they are not left floating above the world.</li>
 *   <li><b>Hidden</b> — everything else: dungeons with no entrance pin, minigames, quest copies,
 *       random events. Shown only when the camera is up there too (dungeon layer opened, or the
 *       player is standing inside).</li>
 * </ul>
 */
public final class InstanceMaps
{
	/** Exclusive. Matches {@link WikiMap#SURFACE} max Y — the top edge of the overworld. */
	public static final int GAP_MIN_Y = 4200;

	/** Floor switcher shows 0 (overworld) and 1 (dungeon / first floor). 2 and 3 stay in code. */
	public static final int HIGHEST_VISIBLE_PLANE = 1;

	/**
	 * Braindeath Island's cache coords sit in the gap. The in-game island is north-east of Mos
	 * Le'Harmless, so travel pins slide onto that eastern-ocean slot. Tiles stay punched unless
	 * the camera is actually there.
	 */
	static final int BRAINDEATH_OFFSET_X = 2112 - 3920;
	static final int BRAINDEATH_OFFSET_Y = 5088 - 3180;

	private static final Map<UndergroundZone, WikiMap> PRIMARY_MAP = new EnumMap<>(UndergroundZone.class);
	private static final Map<UndergroundZone, List<WikiMap>> MAPS_BY_ZONE = new EnumMap<>(UndergroundZone.class);
	private static final Map<String, List<UndergroundZone>> ZONES_BY_ID = new HashMap<>();
	private static volatile Map<Long, List<DungeonPiece>> piecesByRegion = Collections.emptyMap();

	/**
	 * The wiki boxes a dungeon's view should paint and hit-test against, wider than {@link #PRIMARY_MAP}:
	 * every wiki box that contains the zone's own underground anchor. Taverley Dungeon and the
	 * Dwarven Mines are one connected complex whose boxes overlap, so entering from the Dwarven side
	 * must still show the Taverley half instead of clipping it away. One hop only — no transitive
	 * closure, which would chain most of the +6400 band into a single box.
	 */
	private static final Map<UndergroundZone, List<WikiMap>> CLIP_MAPS = new EnumMap<>(UndergroundZone.class);

	static
	{
		for (UndergroundZone zone : UndergroundZone.ALL_ZONES)
		{
			ZONES_BY_ID.computeIfAbsent(zone.getCanonicalId(), id -> new ArrayList<>()).add(zone);
		}
		for (WikiMap map : WikiMap.VALUES)
		{
			if (map == WikiMap.SURFACE || map.getMapId() < 0)
			{
				continue;
			}
			for (UndergroundZone zone : UndergroundZone.ALL_ZONES)
			{
				final int ux = zone.getUndergroundPoint().getX();
				final int uy = zone.getUndergroundPoint().getY();
				if (!map.contains(ux, uy))
				{
					continue;
				}
				// Regional wiki maps contain several independent dungeons. Every enclosed
				// anchor needs access to the map; its centre cannot select a single owner.
				MAPS_BY_ZONE.computeIfAbsent(zone, z -> new ArrayList<>()).add(map);
				final WikiMap current = PRIMARY_MAP.get(zone);
				if (current == null || map.area() < current.area())
				{
					PRIMARY_MAP.put(zone, map);
				}
			}
		}
		for (UndergroundZone zone : UndergroundZone.ALL_ZONES)
		{
			final int ux = zone.getUndergroundPoint().getX();
			final int uy = zone.getUndergroundPoint().getY();
			final List<WikiMap> clip = new ArrayList<>();
			for (WikiMap map : WikiMap.VALUES)
			{
				if (map != WikiMap.SURFACE && map.getMapId() >= 0 && map.contains(ux, uy))
				{
					clip.add(map);
				}
			}
			final WikiMap primary = PRIMARY_MAP.get(zone);
			if (primary != null && !clip.contains(primary))
			{
				clip.add(primary);
			}
			CLIP_MAPS.put(zone, Collections.unmodifiableList(clip));
		}
	}

	private InstanceMaps()
	{
	}

	/** Publish the same loaded footprints used by the renderer, including disk overrides. */
	static void setRoutingPieces(List<DungeonPiece> pieces)
	{
		final Map<Long, List<DungeonPiece>> regions = new HashMap<>();
		for (DungeonPiece piece : pieces)
		{
			if (UndergroundZone.byId(piece.zoneId) == null)
			{
				continue;
			}
			final int[] bounds = piece.srcBounds();
			for (int rx = bounds[0] >> 6; rx <= bounds[2] >> 6; rx++)
			{
				for (int ry = bounds[1] >> 6; ry <= bounds[3] >> 6; ry++)
				{
					regions.computeIfAbsent(regionKey(rx, ry), key -> new ArrayList<>()).add(piece);
				}
			}
		}
		piecesByRegion = regions;
	}

	private static long regionKey(int regionX, int regionY)
	{
		return ((long) regionX << 32) | (regionY & 0xffffffffL);
	}

	public static DungeonPiece pieceForPoint(UndergroundZone zone, int x, int y, int plane, Integer layer)
	{
		for (DungeonPiece piece : piecesByRegion.getOrDefault(regionKey(x >> 6, y >> 6), Collections.emptyList()))
		{
			if (UndergroundZone.zonesMatch(piece.zoneId, zone.getId()) && piece.plane == plane
				&& (layer == null || piece.layer == layer) && piece.containsNative(x, y))
			{
				return piece;
			}
		}
		return null;
	}

	public static boolean cameraIsInside(WikiMap map, double worldX, double worldY)
	{
		return map != null && map.contains((int) Math.floor(worldX), (int) Math.floor(worldY));
	}

	/** True while the camera is looking at the overworld rather than at a layer above it. */
	public static boolean cameraOnOverworld(double cameraY)
	{
		return cameraY <= GAP_MIN_Y;
	}

	private static boolean braindeathInstance(double worldX, double worldY)
	{
		return WikiMap.BRAINDEATH_ISLAND.contains((int) Math.floor(worldX), (int) Math.floor(worldY));
	}

	/** Instance-band terrain that is intentionally composited into the normal overworld. */
	public static boolean isOverworldOverlay(double worldX, double worldY)
	{
		return PrifddinasShift.containsInstance(worldX, worldY)
			|| braindeathInstance(worldX, worldY);
	}

	public static UndergroundZone zoneForPoint(double worldX, double worldY)
	{
		return zoneForPoint(worldX, worldY, -1);
	}

	/** Resolve a player destination without borrowing a footprint from another plane. */
	public static UndergroundZone zoneForPoint(double worldX, double worldY, int plane)
	{
		final int x = (int) Math.floor(worldX);
		final int y = (int) Math.floor(worldY);
		UndergroundZone authored = null;
		long authoredDist = Long.MAX_VALUE;
		for (DungeonPiece piece : piecesByRegion.getOrDefault(regionKey(x >> 6, y >> 6), Collections.emptyList()))
		{
			if ((plane >= 0 && piece.plane != plane) || !piece.containsNative(x, y))
			{
				continue;
			}
			for (UndergroundZone zone : ZONES_BY_ID.get(UndergroundZone.canonicalZoneId(piece.zoneId)))
			{
				final long dist = distSq(x, y, zone.getUndergroundPoint().getX(), zone.getUndergroundPoint().getY());
				if (dist < authoredDist)
				{
					authored = zone;
					authoredDist = dist;
				}
			}
		}
		if (authored != null)
		{
			return authored;
		}
		final UndergroundZone clipped = zoneForClipOverride(x, y, plane);
		if (clipped != null)
		{
			return clipped;
		}
		UndergroundZone best = null;
		long bestDist = Long.MAX_VALUE;
		for (Map.Entry<UndergroundZone, List<WikiMap>> entry : MAPS_BY_ZONE.entrySet())
		{
			boolean covered = false;
			for (WikiMap map : entry.getValue())
			{
				if (map.contains(x, y))
				{
					covered = true;
					break;
				}
			}
			if (!covered)
			{
				continue;
			}
			final UndergroundZone zone = entry.getKey();
			final long dist = distSq(x, y, zone.getUndergroundPoint().getX(), zone.getUndergroundPoint().getY());
			// Composite wiki rectangles include unrelated islands. Only explicit footprints
			// may claim remote child rooms; a fallback must stay near this zone's anchor.
			if (!supportsPlane(zone, plane)
				|| zone.hasClipOverride() || dist > (long) zone.getRadius() * zone.getRadius())
			{
				continue;
			}
			if (dist < bestDist)
			{
				bestDist = dist;
				best = zone;
			}
		}
		return best;
	}

	public static WikiMap mapForZone(UndergroundZone zone)
	{
		if (zone == null)
		{
			return null;
		}
		return PRIMARY_MAP.get(zone);
	}

	/**
	 * Every wiki box the dungeon {@code zone}'s tile view should paint — see {@link #CLIP_MAPS}.
	 * Marker filtering still goes through {@link #belongsToZone}, which stays per-zone: the boxes
	 * here overlap unrelated dungeons, harmless for transparent terrain but wrong as a pin gate.
	 */
	public static List<WikiMap> clipMapsForZone(UndergroundZone zone)
	{
		if (zone == null)
		{
			return Collections.emptyList();
		}
		return CLIP_MAPS.getOrDefault(zone, Collections.emptyList());
	}

	/**
	 * Whether this native game point sits inside {@code zone} — the wiki map assigned to it, or
	 * (when no map owns the point) the zone's radius around its underground anchor.
	 */
	public static boolean belongsToZone(int worldX, int worldY, UndergroundZone zone)
	{
		if (zone == null)
		{
			return false;
		}
		// Remote child floors (such as Duke) have authored footprints outside the
		// parent clip. Use the same ownership priority as zoneForPoint.
		for (DungeonPiece piece : piecesByRegion.getOrDefault(
			regionKey(worldX >> 6, worldY >> 6), Collections.emptyList()))
		{
			if (UndergroundZone.zonesMatch(piece.zoneId, zone.getId())
				&& piece.containsNative(worldX, worldY))
			{
				return true;
			}
		}
		// A hand-authored clip box defines the remaining zone extent directly.
		if (zone.hasClipOverride())
		{
			return worldX >= zone.getClipMinX() && worldX <= zone.getClipMaxX()
				&& worldY >= zone.getClipMinY() && worldY <= zone.getClipMaxY();
		}
		final UndergroundZone owner = zoneForPoint(worldX, worldY);
		if (owner != null)
		{
			return owner == zone;
		}
		final long dx = (long) worldX - zone.getUndergroundPoint().getX();
		final long dy = (long) worldY - zone.getUndergroundPoint().getY();
		final int radius = zone.getRadius();
		return dx * dx + dy * dy <= (long) radius * radius;
	}

	/**
	 * Marker / tooltip filter while a dungeon is hovered or opened. Neighbouring caves and
	 * overworld badges drop out so the focused layer is not a pile of unrelated pins.
	 *
	 * @param dungeonContents true to keep only {@code focus}'s interior; false (peeking back at
	 *                        the surface) keeps overworld points and drops dungeon ones
	 */
	public static boolean inFocusedLayer(int worldX, int worldY, UndergroundZone focus, boolean dungeonContents)
	{
		if (focus == null)
		{
			return true;
		}
		if (dungeonContents)
		{
			return belongsToZone(worldX, worldY, focus);
		}
		return worldY <= GAP_MIN_Y;
	}

	/**
	 * {@link #firstHit(int, int, BiFunction)} that also applies {@link #inFocusedLayer}.
	 */
	public static <T> T firstHit(int displayX, int displayY, UndergroundZone focus, boolean dungeonContents,
		BiFunction<Integer, Integer, T> lookup)
	{
		return firstHit(displayX, displayY, (x, y) ->
			(focus == null && !dungeonContents ? y <= GAP_MIN_Y
				: inFocusedLayer(x, y, focus, dungeonContents)) ? lookup.apply(x, y) : null);
	}

	private static UndergroundZone zoneForClipOverride(int worldX, int worldY, int plane)
	{
		UndergroundZone best = null;
		long bestDist = Long.MAX_VALUE;
		for (UndergroundZone zone : UndergroundZone.ALL_ZONES)
		{
			if (!zone.hasClipOverride() || !supportsPlane(zone, plane))
			{
				continue;
			}
			if (worldX < zone.getClipMinX() || worldX > zone.getClipMaxX()
				|| worldY < zone.getClipMinY() || worldY > zone.getClipMaxY())
			{
				continue;
			}
			final long dist = distSq(worldX, worldY, zone.getUndergroundPoint().getX(), zone.getUndergroundPoint().getY());
			if (dist < bestDist)
			{
				bestDist = dist;
				best = zone;
			}
		}
		return best;
	}

	private static boolean supportsPlane(UndergroundZone zone, int plane)
	{
		if (plane < 0 || zone.getUndergroundPoint().getPlane() == plane)
		{
			return true;
		}
		for (OverlayFloor floor : OverlayFloor.all())
		{
			if (floor.zone == zone && floor.plane == plane)
			{
				return true;
			}
		}
		return false;
	}

	private static long distSq(int x1, int y1, int x2, int y2)
	{
		final long dx = x1 - x2;
		final long dy = y1 - y2;
		return dx * dx + dy * dy;
	}

	private static boolean dagannothKingsLair(double worldX, double worldY)
	{
		final int x = (int) Math.floor(worldX);
		final int y = (int) Math.floor(worldY);
		if (x >= 2608 && x <= 2712 && y >= 9640 && y <= 9704)
		{
			return true;
		}
		return x >= 2880 && x <= 3008 && y >= 4416 && y <= 4544;
	}

	private static boolean onWaterbirthIslandView(double cameraX, double cameraY)
	{
		return cameraIsInside(WikiMap.WATERBIRTH_DUNGEON, cameraX, cameraY);
	}

	/**
	 * Where this game point should be drawn. Prifddinas stays on Tirannwn. Dungeon-instance
	 * points slide to their surface-relative slot unless the camera is already inside that
	 * instance (clicked dungeon layer / player standing there).
	 */
	public static double toDisplayX(double worldX, double worldY, double cameraX, double cameraY)
	{
		if (PrifddinasShift.containsInstance(worldX, worldY))
		{
			return PrifddinasShift.toDisplayX(worldX, worldY);
		}
		if (braindeathInstance(worldX, worldY) && !cameraIsInside(WikiMap.BRAINDEATH_ISLAND, cameraX, cameraY))
		{
			return worldX - BRAINDEATH_OFFSET_X;
		}
		if (dagannothKingsLair(worldX, worldY))
		{
			if (onWaterbirthIslandView(cameraX, cameraY))
			{
				return worldX;
			}
			return UndergroundZone.WATERBIRTH_DUNGEON.getSurfacePoint().getX();
		}
		final UndergroundZone zone = zoneForPoint(worldX, worldY);
		if (zone == null)
		{
			return worldX;
		}
		final WikiMap map = mapForZone(zone);
		if (cameraIsInside(map, cameraX, cameraY))
		{
			return worldX;
		}
		return worldX - zone.getDeltaX();
	}

	public static double toDisplayY(double worldX, double worldY, double cameraX, double cameraY)
	{
		if (PrifddinasShift.containsInstance(worldX, worldY))
		{
			return PrifddinasShift.toDisplayY(worldX, worldY);
		}
		if (braindeathInstance(worldX, worldY) && !cameraIsInside(WikiMap.BRAINDEATH_ISLAND, cameraX, cameraY))
		{
			return worldY - BRAINDEATH_OFFSET_Y;
		}
		if (dagannothKingsLair(worldX, worldY))
		{
			if (onWaterbirthIslandView(cameraX, cameraY))
			{
				return worldY;
			}
			return UndergroundZone.WATERBIRTH_DUNGEON.getSurfacePoint().getY();
		}
		final UndergroundZone zone = zoneForPoint(worldX, worldY);
		if (zone == null)
		{
			return worldY;
		}
		final WikiMap map = mapForZone(zone);
		if (cameraIsInside(map, cameraX, cameraY))
		{
			return worldY;
		}
		return worldY - zone.getDeltaY();
	}

	public static int toDisplayX(int worldX, int worldY, double cameraX, double cameraY)
	{
		return (int) toDisplayX((double) worldX, worldY, cameraX, cameraY);
	}

	public static int toDisplayY(int worldX, int worldY, double cameraX, double cameraY)
	{
		return (int) toDisplayY((double) worldX, worldY, cameraX, cameraY);
	}

	/**
	 * Whether this point has no business in the overworld view.
	 *
	 * <p>Default deny: anything still sitting above {@link #GAP_MIN_Y} after projection belongs to
	 * a layer of its own, and the overworld is not it. Dungeons with an {@link UndergroundZone}
	 * pin project down to their entrance and so pass; Prifddinas, Braindeath and the Dagannoth
	 * Kings instance have their own overworld slots. Everything left — dungeons reachable only
	 * from their own layer, minigames, quest copies — is shown only when the camera is up there
	 * too. Asking by projected Y rather than by map box is what keeps content out of the view
	 * when the wiki publishes no box for it, which is most of the +6400 band.
	 */
	public static boolean hiddenOnSurface(double worldX, double worldY, double cameraX, double cameraY)
	{
		if (worldY <= GAP_MIN_Y || !cameraOnOverworld(cameraY))
		{
			return false;
		}
		if (PrifddinasShift.containsInstance(worldX, worldY) || braindeathInstance(worldX, worldY)
			|| dagannothKingsLair(worldX, worldY))
		{
			return false;
		}
		return toDisplayY(worldX, worldY, cameraX, cameraY) > GAP_MIN_Y;
	}

	public static boolean inView(int worldX, int worldY, double cameraX, double cameraY,
		double minX, double maxX, double minY, double maxY)
	{
		if (hiddenOnSurface(worldX, worldY, cameraX, cameraY))
		{
			return false;
		}
		final double dx = toDisplayX(worldX, worldY, cameraX, cameraY);
		final double dy = toDisplayY(worldX, worldY, cameraX, cameraY);
		return dx >= minX && dx <= maxX && dy >= minY && dy <= maxY;
	}

	/**
	 * Hit-test at the displayed point, then at the matching instance / dungeon point so hover
	 * still finds wiki records stored at native coordinates.
	 */
	public static <T> T firstHit(int displayX, int displayY, BiFunction<Integer, Integer, T> lookup)
	{
		final T prifHit = PrifddinasShift.firstHit(displayX, displayY, lookup);
		if (prifHit != null)
		{
			return prifHit;
		}
		final int brainX = displayX + BRAINDEATH_OFFSET_X;
		final int brainY = displayY + BRAINDEATH_OFFSET_Y;
		if (braindeathInstance(brainX, brainY))
		{
			final T brainHit = lookup.apply(brainX, brainY);
			if (brainHit != null)
			{
				return brainHit;
			}
		}
		final WorldPoint waterbirth = UndergroundZone.WATERBIRTH_DUNGEON.getSurfacePoint();
		if (Math.abs(displayX - waterbirth.getX()) <= 80 && Math.abs(displayY - waterbirth.getY()) <= 80)
		{
			final T kings = lookup.apply(2631, 9667);
			if (kings != null)
			{
				return kings;
			}
		}
		for (UndergroundZone zone : UndergroundZone.ALL_ZONES)
		{
			final WikiMap map = mapForZone(zone);
			if (map == null)
			{
				continue;
			}
			final int ux = displayX + zone.getDeltaX();
			final int uy = displayY + zone.getDeltaY();
			if (!map.contains(ux, uy))
			{
				continue;
			}
			final T hit = lookup.apply(ux, uy);
			if (hit != null)
			{
				return hit;
			}
		}
		return null;
	}

	/** Native query box plus the instance / dungeon boxes that project into this view. */
	public static void forEachQueryArea(int minX, int maxX, int minY, int maxY,
		double cameraX, double cameraY, AreaQuery query)
	{
		forEachQueryArea(minX, maxX, minY, maxY, cameraX, cameraY, true, query);
	}

	/**
	 * Same as {@link #forEachQueryArea(int, int, int, int, double, double, AreaQuery)}, but
	 * dungeon layers can be left out. Monster spawns stay in the dungeon; projecting them
	 * would stamp cave creatures on the overworld above.
	 */
	public static void forEachQueryArea(int minX, int maxX, int minY, int maxY,
		double cameraX, double cameraY, boolean projectDungeons, AreaQuery query)
	{
		query.accept(minX, maxX, minY, maxY);
		if (PrifddinasShift.overlapsOverworld(minX, maxX, minY, maxY))
		{
			query.accept(
				minX + PrifddinasShift.OFFSET_X, maxX + PrifddinasShift.OFFSET_X,
				minY + PrifddinasShift.OFFSET_Y, maxY + PrifddinasShift.OFFSET_Y);
		}
		if (!projectDungeons)
		{
			return;
		}
		for (UndergroundZone zone : UndergroundZone.ALL_ZONES)
		{
			final WikiMap map = mapForZone(zone);
			if (map == null || cameraIsInside(map, cameraX, cameraY))
			{
				continue;
			}
			final int qMinX = minX + zone.getDeltaX();
			final int qMaxX = maxX + zone.getDeltaX();
			final int qMinY = minY + zone.getDeltaY();
			final int qMaxY = maxY + zone.getDeltaY();
			if (qMaxX < map.getMinX() || qMinX > map.getMaxX()
				|| qMaxY < map.getMinY() || qMinY > map.getMaxY())
			{
				continue;
			}
			query.accept(
				Math.max(qMinX, map.getMinX()), Math.min(qMaxX, map.getMaxX()),
				Math.max(qMinY, map.getMinY()), Math.min(qMaxY, map.getMaxY()));
		}
	}

	@FunctionalInterface
	public interface AreaQuery
	{
		void accept(int minX, int maxX, int minY, int maxY);
	}
}
