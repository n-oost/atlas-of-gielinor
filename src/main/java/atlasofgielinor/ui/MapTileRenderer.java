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
package atlasofgielinor.ui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;

import atlasofgielinor.AtlasOfGielinorConfig;
import atlasofgielinor.data.dungeons.DungeonPiece;
import atlasofgielinor.data.dungeons.DungeonPieceTransform;
import atlasofgielinor.data.dungeons.DungeonFloor;
import atlasofgielinor.data.dungeons.UndergroundZone;
import atlasofgielinor.map.DungeonPieceIndex;
import atlasofgielinor.map.InstanceMaps;
import atlasofgielinor.map.MapCamera;
import atlasofgielinor.map.PrifddinasShift;
import atlasofgielinor.tiles.DungeonVoid;
import atlasofgielinor.tiles.TileLoader;
import atlasofgielinor.tiles.WikiMap;
import atlasofgielinor.tiles.WikiMapTiles;

/**
 * The map's tile grid: which zoom level to read, which plane, and where each tile lands.
 *
 * <p>Split out of {@link AtlasOfGielinorOverlay}.
 */
class MapTileRenderer
{
	private static final float SURFACE_VIEW_DUNGEON_OPACITY = 0.68f;
	private static final float SURFACE_VIEW_DUNGEON_DARKEN = 0.35f;

	private final AtlasOfGielinorConfig config;
	private final MapCamera camera;
	private final TileLoader tileLoader;
	private final DungeonPieceIndex dungeonPieceIndex;

	private int lastTileZoom = Integer.MIN_VALUE;
	private int[] prifddinasSourceBounds;
	/** Reused while blurring the overworld overlay so hover does not allocate a full-map image every frame. */
	private BufferedImage overlayBuffer;
	private BufferedImage blurBuffer;
	private int[] columnX = new int[0];
	private int[] columnWidth = new int[0];
	private int[] rowY = new int[0];
	private int[] rowHeight = new int[0];

	/** Cache of void-keyed copies, keyed by the source tile so it clears when the tile store evicts. */
	private final java.util.Map<BufferedImage, BufferedImage> voidKeyedTiles = new java.util.WeakHashMap<>();

	MapTileRenderer(AtlasOfGielinorConfig config, MapCamera camera, TileLoader tileLoader,
		DungeonPieceIndex dungeonPieceIndex)
	{
		this.config = config;
		this.camera = camera;
		this.tileLoader = tileLoader;
		this.dungeonPieceIndex = dungeonPieceIndex;
	}

	void drawTiles(Graphics2D graphics, Rectangle bounds)
	{
		final int plane = camera.getPlane();

		// Never ask for a level that was not prefetched, or the map would simply go black past
		// whatever depth the tile store happens to hold. The last real level is scaled up instead.
		// Frozen zoom, so the tile level matches the pose the tiles are placed with this frame.
		final int availableZoom = tileLoader.maxAvailableZoom();
		final int currentZoom = Math.min(lastTileZoom, availableZoom);
		final int tileZoom = Math.min(
			WikiMapTiles.bestZoom(camera.getFrameZoom(), currentZoom), availableZoom);

		// Past the deepest level we actually hold, tiles get blown up. They are pixel art with the
		// wiki's own icons baked in, so bilinear turns both to mush — nearest keeps them crisp.
		final boolean upscaling = camera.getFrameZoom() > Math.pow(2, tileZoom) * 1.01;
		graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
			upscaling
				? RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
				: RenderingHints.VALUE_INTERPOLATION_BILINEAR);

		lastTileZoom = tileZoom;

		final UndergroundZone hoveredZone = camera.getHoveredUndergroundZone();
		final boolean hoverPreview = config.undergroundHoverPreview() && hoveredZone != null;

		if (camera.isTravelViewActive())
		{
			drawSurfaceBackdrop(graphics, bounds, tileZoom,
				config.undergroundLayerSurfaceOpacity() / 100f,
				config.undergroundLayerSurfaceBlur());
		}
		else if (camera.isUndergroundModeActive())
		{
			// Clicked: dungeon tiles composited onto the surface entrance, solid, with the
			// overworld pushed back by the dim/blur settings.

			final boolean keyVoid = config.undergroundTransparentVoid();
			final float opacity = config.undergroundLayerSurfaceOpacity() / 100f;
			final int blur = config.undergroundLayerSurfaceBlur();
			drawUndergroundLayer(graphics, bounds, camera.previewUndergroundZones(),
				tileZoom, keyVoid, opacity, blur);
		}
		else if (camera.isViewingPlayerInteriorFromSurface()
			&& (!hoverPreview || hoveredZone == camera.getObservedPlayerUndergroundZone()))
		{
			drawSurfaceTiles(graphics, bounds, plane, tileZoom);
			drawDimmedPlayerInterior(graphics, bounds,
				camera.getObservedPlayerUndergroundZone(), tileZoom);
		}
		else if (hoverPreview)
		{
			// Hover: the layer being peeked at is drawn solid, the layer above it faint.
			if (camera.isHoveredSurfaceToUnderground())
			{
				drawUndergroundLayer(graphics, bounds, camera.previewUndergroundZones(),
					tileZoom, config.undergroundTransparentVoid(),
					config.undergroundOverlayOpacity() / 100f, config.undergroundHoverBlur());
			}
			else
			{
				// In reverse: when underground and hovering upward surface symbol
				// 1. Surface base layer, solid.
				final int surfDeltaX = -hoveredZone.getDeltaX();
				final int surfDeltaY = -hoveredZone.getDeltaY();
				drawTileLayer(graphics, bounds, 0, tileZoom, surfDeltaX, surfDeltaY);

				// 2. Underground over the top of it, faint.
				final float underOpacity = Math.max(0.08f, Math.min(0.85f, config.undergroundOverlayOpacity() / 100f));
				final Composite origComp = graphics.getComposite();
				graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, underOpacity));

				drawTileLayer(graphics, bounds, plane, tileZoom);

				graphics.setComposite(origComp);
			}
		}
		else if (camera.isInteriorMapUnavailable())
		{
			// No authored interior owns the player's resolved point. Keep spatial context at the
			// last entrance and make it visually clear that the player is below this surface.
			drawSurfaceBackdrop(graphics, bounds, tileZoom,
				config.undergroundLayerSurfaceOpacity() / 100f,
				config.undergroundLayerSurfaceBlur());
		}
		else
		{
			// Nothing hovered or clicked: the plain map view.
			drawSurfaceTiles(graphics, bounds, plane, tileZoom);
		}

		// Icons and markers drawn after this are not pixel art; give them smooth scaling back.
		graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
	}

	private void drawUndergroundLayer(Graphics2D graphics, Rectangle bounds, List<UndergroundZone> zones,
		int tileZoom, boolean keyVoid, float surfaceOpacity, int surfaceBlur)
	{
		drawSurfaceBackdrop(graphics, bounds, tileZoom, surfaceOpacity, surfaceBlur);
		drawUndergroundZones(graphics, bounds, zones, tileZoom, keyVoid);
	}

	private void drawUndergroundZones(Graphics2D graphics, Rectangle bounds, List<UndergroundZone> zones,
		int tileZoom, boolean keyVoid)
	{

		for (UndergroundZone zone : zones)
		{


			if (drawZonePieces(graphics, bounds, zone, tileZoom, keyVoid,
				planeForZone(zone)))
			{
				continue;
			}
			if (zone.getId().startsWith("native_")) continue;

			final int ox = zone.getDeltaX();
			final int oy = zone.getDeltaY();
			final int underPlane = zone.getUndergroundPoint().getPlane();
			final Shape oldClip = graphics.getClip();
			clipToZone(graphics, bounds, zone, ox, oy);
			if (underPlane > 0)
			{
				drawTileLayer(graphics, bounds, 0, tileZoom, ox, oy, keyVoid);
			}
			drawTileLayer(graphics, bounds, underPlane, tileZoom, ox, oy, keyVoid);
			graphics.setClip(oldClip);
		}
	}

	/** Sharp surface with the physically occupied dungeon visible as a dimmed overlay. */
	private void drawDimmedPlayerInterior(Graphics2D graphics, Rectangle bounds, UndergroundZone zone, int tileZoom)
	{
		if (zone == null || bounds.width <= 2 || bounds.height <= 2)
		{
			return;
		}
		ensureOverlayBuffer(bounds.width, bounds.height);
		final Graphics2D bufferGraphics = overlayBuffer.createGraphics();
		try
		{
			bufferGraphics.setComposite(AlphaComposite.Clear);
			bufferGraphics.fillRect(0, 0, overlayBuffer.getWidth(), overlayBuffer.getHeight());
			bufferGraphics.setComposite(AlphaComposite.SrcOver);
			bufferGraphics.translate(-bounds.x, -bounds.y);
			bufferGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
				graphics.getRenderingHint(RenderingHints.KEY_INTERPOLATION));
			drawUndergroundZones(bufferGraphics, bounds, Collections.singletonList(zone), tileZoom, true);
			bufferGraphics.setComposite(AlphaComposite.SrcAtop.derive(SURFACE_VIEW_DUNGEON_DARKEN));
			bufferGraphics.setColor(Color.BLACK);
			bufferGraphics.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
		}
		finally
		{
			bufferGraphics.dispose();
		}

		final Composite original = graphics.getComposite();
		graphics.setComposite(AlphaComposite.SrcOver.derive(SURFACE_VIEW_DUNGEON_OPACITY));
		graphics.drawImage(overlayBuffer, bounds.x, bounds.y, null);
		graphics.setComposite(original);
	}

	/**
	 * Draw authored dungeon pieces for this zone. Each piece clips to its source rects and uses its own
	 * dx/dy/rot/flip. Returns false when the TSV has nothing for this zone so the caller can fall
	 * back to the zone-delta + wiki clip path.
	 */
	private int planeForZone(UndergroundZone zone)
	{
		final Integer hoveredFloor = camera.getHoveredFloorPlane();
		if (hoveredFloor != null && zone == camera.getHoveredUndergroundZone())
		{
			return hoveredFloor;
		}
		if (camera.getActiveUndergroundZone() == zone)
		{
			return camera.getPlane();
		}
		if (camera.isViewingPlayerInteriorFromSurface()
			&& camera.getObservedPlayerUndergroundZone() == zone
			&& camera.getPlayerLocation() != null)
		{
			return camera.getPlayerLocation().getPlane();
		}
		return zone.getUndergroundPoint().getPlane();
	}

	private boolean drawZonePieces(Graphics2D graphics, Rectangle bounds, UndergroundZone zone,
		int tileZoom, boolean keyVoid, int planeFilter)
	{
		// Bundled fallback imagery can use a different source zoom from the installed pack.
		final int sourceZoom = zone.sourceZoom(tileZoom);
		final List<DungeonPiece> pieces = dungeonPieceIndex == null
			? Collections.emptyList()
			: dungeonPieceIndex.piecesFor(zone.getId());
		if (pieces.isEmpty())
		{
			return false;
		}

		final int viewMinX = (int) Math.floor(camera.worldX(bounds.getMinX(), bounds)) - 1;
		final int viewMaxX = (int) Math.ceil(camera.worldX(bounds.getMaxX(), bounds)) + 1;
		final int viewMinY = (int) Math.floor(camera.worldY(bounds.getMaxY(), bounds)) - 1;
		final int viewMaxY = (int) Math.ceil(camera.worldY(bounds.getMinY(), bounds)) + 1;
		final Integer floorLayer = camera.floorLayerFor(zone);
		final boolean multiPlane = DungeonFloor.hasMultiplePlanes(zone);

		for (DungeonPiece piece : pieces)
		{
			if (!camera.isDungeonPieceVisible(zone, piece)
				|| (multiPlane && piece.plane != planeFilter) || (floorLayer != null && piece.layer != floorLayer))
			{
				continue;
			}
			if (!pieceOnScreen(piece, viewMinX, viewMinY, viewMaxX, viewMaxY))
			{
				continue;
			}
			final Graphics2D pg = (Graphics2D) graphics.create();
			try
			{
				final AffineTransform xform = pieceAffine(piece, bounds);
				if (!xform.isIdentity())
				{
					pg.transform(xform);
				}
				pg.clip(pieceClipScreen(piece, bounds));
				final double shiftX = -piece.dx;
				final double shiftY = -piece.dy;
				final int[] sourceBounds = piece.srcBounds();
				drawTileLayer(pg, bounds, piece.plane, sourceZoom, shiftX, shiftY, keyVoid, sourceBounds);
			}
			finally
			{
				pg.dispose();
			}
		}
		return true;
	}

	private AffineTransform pieceAffine(DungeonPiece piece, Rectangle bounds)
	{
		final int[] b = piece.srcBounds();
		final double wx = (b[0] + b[2] + 1) / 2.0 + piece.dx;
		final double wy = (b[1] + b[3] + 1) / 2.0 + piece.dy;
		return DungeonPieceTransform.affine(piece.rot, piece.flipX, piece.flipY,
			camera.screenX(wx, bounds), camera.screenY(wy, bounds));
	}

	private Area pieceClipScreen(DungeonPiece piece, Rectangle bounds)
	{
		final Area clip = new Area();
		for (int[] r : piece.rects)
		{
			final Rectangle hole = worldRectToScreen(
				r[0] + piece.dx, r[1] + piece.dy,
				r[2] + 1 + piece.dx, r[3] + 1 + piece.dy, bounds);
			clip.add(new Area(hole));
		}
		return clip;
	}

	private static boolean pieceOnScreen(DungeonPiece piece,
		int viewMinX, int viewMinY, int viewMaxX, int viewMaxY)
	{
		final int[] b = piece.worldBounds();
		final int minX = b[0];
		final int minY = b[1];
		final int maxX = b[2];
		final int maxY = b[3];
		final int pad = piece.rot == 0 ? 0 : Math.max(maxX - minX, maxY - minY);
		return maxX + pad >= viewMinX && minX - pad <= viewMaxX
			&& maxY + pad >= viewMinY && minY - pad <= viewMaxY;
	}

	/**
	 * The surface drawn behind a dungeon — solid-ish under a hover peek, dim and blurred behind an
	 * open layer. {@code opacity} 0..1 (0 skips it entirely); {@code blur} 0 draws sharp, higher
	 * downscales further before scaling back up.
	 */
	private void drawSurfaceBackdrop(Graphics2D graphics, Rectangle bounds, int tileZoom, float opacity, int blur)
	{
		final float surfaceOpacity = Math.max(0f, Math.min(0.9f, opacity));
		if (surfaceOpacity <= 0.001f)
		{
			return;
		}
		if (blur <= 0 || bounds.width <= 2 || bounds.height <= 2)
		{
			final Composite origComp = graphics.getComposite();
			graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, surfaceOpacity));
			drawSurfaceTiles(graphics, bounds, 0, tileZoom);
			graphics.setComposite(origComp);
			return;
		}

		final int factor = Math.max(2, blur + 1);
		ensureOverlayBuffers(bounds.width, bounds.height, factor);
		final Graphics2D bufferGraphics = overlayBuffer.createGraphics();
		try
		{
			bufferGraphics.setComposite(AlphaComposite.Clear);
			bufferGraphics.fillRect(0, 0, overlayBuffer.getWidth(), overlayBuffer.getHeight());
			bufferGraphics.setComposite(AlphaComposite.SrcOver);
			bufferGraphics.translate(-bounds.x, -bounds.y);
			bufferGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
				graphics.getRenderingHint(RenderingHints.KEY_INTERPOLATION));
			drawSurfaceTiles(bufferGraphics, bounds, 0, tileZoom);
		}
		finally
		{
			bufferGraphics.dispose();
		}

		final Graphics2D blurGraphics = blurBuffer.createGraphics();
		try
		{
			blurGraphics.setComposite(AlphaComposite.Clear);
			blurGraphics.fillRect(0, 0, blurBuffer.getWidth(), blurBuffer.getHeight());
			blurGraphics.setComposite(AlphaComposite.SrcOver);
			blurGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			blurGraphics.drawImage(overlayBuffer, 0, 0, blurBuffer.getWidth(), blurBuffer.getHeight(), null);
		}
		finally
		{
			blurGraphics.dispose();
		}

		final Composite origComp = graphics.getComposite();
		final Object originalInterpolation = graphics.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
		graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, surfaceOpacity));
		graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		graphics.drawImage(blurBuffer, bounds.x, bounds.y, bounds.width, bounds.height, null);
		graphics.setComposite(origComp);
		graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, originalInterpolation);
	}

	private void ensureOverlayBuffers(int width, int height, int factor)
	{
		ensureOverlayBuffer(width, height);
		final int blurW = Math.max(1, width / factor);
		final int blurH = Math.max(1, height / factor);
		if (blurBuffer == null || blurBuffer.getWidth() != blurW || blurBuffer.getHeight() != blurH)
		{
			blurBuffer = new BufferedImage(blurW, blurH, BufferedImage.TYPE_INT_ARGB);
		}
	}

	private void ensureOverlayBuffer(int width, int height)
	{
		if (overlayBuffer == null || overlayBuffer.getWidth() != width || overlayBuffer.getHeight() != height)
		{
			overlayBuffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		}
	}

	/**
	 * Restrict dungeon tile drawing to this zone's clip box so neighbouring caves in the same
	 * +6400 band do not paint into the view. A zone with {@link UndergroundZone#hasClipOverride()}
	 * (Mor Ul Rek) uses its own hand-authored box; otherwise it is the union of
	 * {@link InstanceMaps#clipMapsForZone}, so a connected complex entered from a side box
	 * (Dwarven Mines into Taverley Dungeon) is not cut off at that box's edge.
	 */
	private void clipToZone(Graphics2D graphics, Rectangle bounds, UndergroundZone zone, int worldXOffset, int worldYOffset)
	{
		final Area clipped = new Area();

		if (zone.hasClipOverride())
		{
			final Rectangle hole = worldRectToScreen(
				zone.getClipMinX() - worldXOffset, zone.getClipMinY() - worldYOffset,
				zone.getClipMaxX() - worldXOffset, zone.getClipMaxY() - worldYOffset, bounds);
			clipped.add(new Area(hole.intersection(bounds)));
		}
		else
		{
			final List<WikiMap> maps = InstanceMaps.clipMapsForZone(zone);
			if (maps.isEmpty())
			{
				return;
			}
			for (WikiMap map : maps)
			{
				final Rectangle hole = worldRectToScreen(
					map.getMinX() - worldXOffset, map.getMinY() - worldYOffset,
					map.getMaxX() - worldXOffset, map.getMaxY() - worldYOffset, bounds);
				clipped.add(new Area(hole.intersection(bounds)));
			}
		}

		graphics.clip(clipped);
	}

	/**
	 * Surface tiles with everything above the overworld cut out — the dungeons, minigames and
	 * quest copies the cache stores in the band north of {@link InstanceMaps#GAP_MIN_Y}, which
	 * would otherwise hang over the world as floating islands. One cut covers the lot, including
	 * the areas the wiki publishes no box for. Instanced Prifddinas is then composited onto the
	 * Tirannwn slot so the city stays on the overworld despite being an instance.
	 */
	private void drawSurfaceTiles(Graphics2D graphics, Rectangle bounds, int plane, int tileZoom)
	{
		final Shape oldClip = graphics.getClip();
		final double cameraY = camera.getCenterY();
		if (InstanceMaps.cameraOnOverworld(cameraY))
		{
			final Rectangle hole = worldRectToScreen(
				MapCamera.MIN_WORLD_X, InstanceMaps.GAP_MIN_Y,
				MapCamera.MAX_WORLD_X, MapCamera.MAX_WORLD_Y, bounds);
			if (hole.intersects(bounds))
			{
				final Area punched = new Area(bounds);
				punched.subtract(new Area(hole));
				// Keep the overlay's native-close hole when narrowing the tile clip (Hub review F2).
				graphics.clip(punched);
			}
		}

		if (plane > 0)
		{
			drawSurfaceBackdrop(graphics, bounds, tileZoom, 0.75f, 6);
			drawTileLayer(graphics, bounds, plane, tileZoom, 0, 0, true);
		}
		else
		{
			drawTileLayer(graphics, bounds, 0, tileZoom);
		}
		graphics.setClip(oldClip);

		final PrifddinasShift placement = PrifddinasShift.get();
		final Rectangle dest = worldRectToScreen(
			placement.getOverworldMinX(), placement.getOverworldMinY(),
			placement.getOverworldMaxX(), placement.getOverworldMaxY(), bounds);
		if (!dest.intersects(bounds))
		{
			return;
		}

		if (prifddinasSourceBounds == null)
		{
			prifddinasSourceBounds = new int[]{placement.getInstanceMinX(), placement.getInstanceMinY(),
				placement.getInstanceMaxX() - 1, placement.getInstanceMaxY() - 1};
		}

		graphics.clip(dest.intersection(bounds));
		drawTileLayer(graphics, bounds, plane > 0 ? plane : 0, tileZoom,
			placement.getOffsetX(), placement.getOffsetY(), plane > 0, prifddinasSourceBounds);
		graphics.setClip(oldClip);
	}

	/**
	 * A copy of {@code src} with dungeon-canvas pixels (near-black void and the solid brown fill
	 * the cache render bakes between rooms) turned fully transparent, so the surface drawn
	 * underneath shows through the tunnels. Computed once per tile and cached.
	 */
	private BufferedImage voidKeyed(BufferedImage src)
	{
		BufferedImage out = voidKeyedTiles.get(src);
		if (out != null)
		{
			return out;
		}
		final int w = src.getWidth();
		final int h = src.getHeight();
		final int[] px = src.getRGB(0, 0, w, h, null, 0, w);
		for (int i = 0; i < px.length; i++)
		{
			final int p = px[i];
			if (DungeonVoid.isVoid(p))
			{
				px[i] = 0;
			}
		}
		out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		out.setRGB(0, 0, w, h, px, 0, w);
		voidKeyedTiles.put(src, out);
		return out;
	}

	private Rectangle worldRectToScreen(int minX, int minY, int maxX, int maxY, Rectangle bounds)
	{
		final int x0 = (int) Math.round(camera.screenX(minX, bounds));
		final int x1 = (int) Math.round(camera.screenX(maxX, bounds));
		final int y0 = (int) Math.round(camera.screenY(maxY, bounds));
		final int y1 = (int) Math.round(camera.screenY(minY, bounds));
		return new Rectangle(x0, y0, Math.max(1, x1 - x0), Math.max(1, y1 - y0));
	}
	private void drawTileLayer(Graphics2D graphics, Rectangle bounds, int plane, int tileZoom)
	{
		drawTileLayer(graphics, bounds, plane, tileZoom, 0, 0, false);
	}

	private void drawTileLayer(Graphics2D graphics, Rectangle bounds, int plane, int tileZoom, double worldXOffset, double worldYOffset)
	{
		drawTileLayer(graphics, bounds, plane, tileZoom, worldXOffset, worldYOffset, false);
	}

	private void drawTileLayer(Graphics2D graphics, Rectangle bounds, int plane, int tileZoom,
		double worldXOffset, double worldYOffset, boolean keyVoid)
	{
		drawTileLayer(graphics, bounds, plane, tileZoom, worldXOffset, worldYOffset, keyVoid, null);
	}

	/** Draw only the source rectangle when supplied. Bounds are inclusive world coordinates. */
	private void drawTileLayer(Graphics2D graphics, Rectangle bounds, int plane, int tileZoom,
		double worldXOffset, double worldYOffset, boolean keyVoid, int[] sourceBounds)
	{
		double westWorld = camera.worldX(bounds.getMinX(), bounds) + worldXOffset;
		double eastWorld = camera.worldX(bounds.getMaxX(), bounds) + worldXOffset;
		double southWorld = camera.worldY(bounds.getMaxY(), bounds) + worldYOffset;
		double northWorld = camera.worldY(bounds.getMinY(), bounds) + worldYOffset;
		if (sourceBounds != null)
		{
			westWorld = Math.max(westWorld, sourceBounds[0]);
			eastWorld = Math.min(eastWorld, sourceBounds[2] + 1);
			southWorld = Math.max(southWorld, sourceBounds[1]);
			northWorld = Math.min(northWorld, sourceBounds[3] + 1);
			if (westWorld >= eastWorld || southWorld >= northWorld)
			{
				return;
			}
		}

		// Include 1 extra tile buffer on all 4 borders so rapid panning never clips edge tiles
		int tileXMin = WikiMapTiles.tileIndex(westWorld, tileZoom) - 1;
		int tileXMax = WikiMapTiles.tileIndex(eastWorld, tileZoom) + 1;
		int tileYMin = WikiMapTiles.tileIndex(southWorld, tileZoom) - 1;
		int tileYMax = WikiMapTiles.tileIndex(northWorld, tileZoom) + 1;
		if (sourceBounds != null)
		{
			tileXMin = Math.max(tileXMin, WikiMapTiles.tileIndex(sourceBounds[0], tileZoom));
			tileXMax = Math.min(tileXMax, WikiMapTiles.tileIndex(sourceBounds[2], tileZoom));
			tileYMin = Math.max(tileYMin, WikiMapTiles.tileIndex(sourceBounds[1], tileZoom));
			tileYMax = Math.min(tileYMax, WikiMapTiles.tileIndex(sourceBounds[3], tileZoom));
		}

		final int numCols = tileXMax - tileXMin + 1;
		final int numRows = tileYMax - tileYMin + 1;
		if (numCols <= 0 || numRows <= 0)
		{
			return;
		}
		ensureGridCapacity(numCols, numRows);
		for (int c = 0; c < numCols; c++)
		{
			final int tileX = tileXMin + c;
			final int x0 = (int) Math.round(camera.screenX(
				WikiMapTiles.tileOrigin(tileX, tileZoom) - worldXOffset, bounds));
			final int x1 = (int) Math.round(camera.screenX(
				WikiMapTiles.tileOrigin(tileX + 1, tileZoom) - worldXOffset, bounds));
			columnX[c] = x0;
			columnWidth[c] = x1 - x0;
		}
		for (int r = 0; r < numRows; r++)
		{
			final int tileY = tileYMin + r;
			final int y0 = (int) Math.round(camera.screenY(
				WikiMapTiles.tileOrigin(tileY + 1, tileZoom) - worldYOffset, bounds));
			final int y1 = (int) Math.round(camera.screenY(
				WikiMapTiles.tileOrigin(tileY, tileZoom) - worldYOffset, bounds));
			rowY[r] = y0;
			rowHeight[r] = y1 - y0;
		}

		for (int r = 0; r < numRows; r++)
		{
			final int tileY = tileYMin + r;
			final int y0 = rowY[r];
			final int h = rowHeight[r];
			if (h <= 0)
			{
				continue;
			}

			for (int c = 0; c < numCols; c++)
			{
				final int tileX = tileXMin + c;
				final int x0 = columnX[c];
				final int w = columnWidth[c];
				if (w <= 0)
				{
					continue;
				}

				final BufferedImage tile = tileLoader.get(plane, tileZoom, tileX, tileY);
				if (tile == null)
				{
					continue;
				}

				graphics.drawImage(keyVoid ? voidKeyed(tile) : tile, x0, y0, w, h, null);
			}
		}
	}

	private void ensureGridCapacity(int columns, int rows)
	{
		if (columnX.length < columns)
		{
			columnX = new int[columns];
			columnWidth = new int[columns];
		}
		if (rowY.length < rows)
		{
			rowY = new int[rows];
			rowHeight = new int[rows];
		}
	}
}
