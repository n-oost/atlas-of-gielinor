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
package atlasofgielinor.ui.markers;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

import atlasofgielinor.AtlasOfGielinorConfig;
import atlasofgielinor.data.dungeons.DungeonPiece;
import atlasofgielinor.data.dungeons.DungeonPieceTransform;
import atlasofgielinor.data.dungeons.UndergroundZone;
import atlasofgielinor.map.DungeonPieceIndex;
import atlasofgielinor.map.InstanceMaps;
import atlasofgielinor.map.MapCamera;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.WorldEntity;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

/** Draws the player arrow in surface and dungeon views. */
public class PlayerMarkerRenderer
{
	private static final Color PLAYER_FILL = new Color(255, 255, 255);
	/** Translucent fill for the player arrow when it is projected onto an entrance, not a real fix. */
	private static final Color PLAYER_FILL_BELOW = new Color(255, 255, 255, 90);
	private static final Color PLAYER_EDGE = new Color(18, 18, 18);
	/** Dashed connector across a transport hop (teleport, fairy ring, stairs) between two walk runs. */
	/** The player marker is drawn under a rotate(), so its outline never varies. */
	private static final int[] PLAYER_ARROW_X = {0, -5, 0, 5};
	private static final int[] PLAYER_ARROW_Y = {-8, 6, 3, 6};
	private static final Stroke PLAYER_ARROW_EDGE = new BasicStroke(3.0f);

	private final Client client;
	private final AtlasOfGielinorConfig config;
	private final MapCamera camera;
	private final DungeonPieceIndex dungeonPieceIndex;

	public PlayerMarkerRenderer(
		Client client,
		AtlasOfGielinorConfig config,
		MapCamera camera,
		DungeonPieceIndex dungeonPieceIndex)
	{
		this.client = client;
		this.config = config;
		this.camera = camera;
		this.dungeonPieceIndex = dungeonPieceIndex;
	}

	/** Place the arrow with the same per-piece translation/rotation/flip used for dungeon tiles. */
	private Point2D arrangedPlayerPoint(WorldPoint rawLocation, Rectangle bounds)
	{
		final UndergroundZone zone = camera.getActiveUndergroundZone() != null
			? camera.getActiveUndergroundZone()
			: camera.getObservedPlayerUndergroundZone();
		if (zone == null || dungeonPieceIndex == null)
		{
			return null;
		}
		final DungeonPiece piece = dungeonPieceIndex.pieceAt(
			zone.getId(), rawLocation.getX(), rawLocation.getY(), rawLocation.getPlane(),
			camera.floorLayerFor(zone));
		if (piece == null)
		{
			return null;
		}


		final int[] sourceBounds = piece.srcBounds();
		final double centerWorldX = (sourceBounds[0] + sourceBounds[2] + 1) / 2.0 + piece.dx;
		final double centerWorldY = (sourceBounds[1] + sourceBounds[3] + 1) / 2.0 + piece.dy;
		final AffineTransform transform = DungeonPieceTransform.affine(
			piece.rot, piece.flipX, piece.flipY,
			camera.screenX(centerWorldX, bounds), camera.screenY(centerWorldY, bounds));
		final Point2D point = new Point2D.Double(
			camera.screenX(rawLocation.getX() + 0.5 + piece.dx, bounds),
			camera.screenY(rawLocation.getY() + 0.5 + piece.dy, bounds));
		return transform.transform(point, null);
	}

	public void drawPlayer(Graphics2D graphics, Rectangle bounds)
	{
		if (!config.showPlayerMarker())
		{
			return;
		}

		final Player local = client.getLocalPlayer();
		final WorldPoint rawLocation = camera.getPlayerLocation();
		WorldPoint location = camera.getPlayerDisplayLocation();
		if (location == null)
		{
			location = camera.getPlayerLocation();
		}
		if (local == null || location == null)
		{
			return;
		}

		// While the player is on a layer above the overworld and the overworld is what is on screen,
		// pin the marker to the last surface spot they stood on — their cave mouth — and draw it
		// hollow. This covers an unavailable interior and the user's explicit choice to return to
		// the surface while still inside a mapped instance. An open mapped cutaway instead uses the
		// real solid player fix, transformed with its arranged dungeon piece below.
		boolean below = false;
		final boolean projectionHides = InstanceMaps.hiddenOnSurface(rawLocation.getX(), rawLocation.getY(),
			camera.getCenterX(), camera.getCenterY());
		final boolean instancedAbove = camera.isPlayerInInstance()
			&& rawLocation.getY() > InstanceMaps.GAP_MIN_Y
			&& camera.getObservedPlayerUndergroundZone() == null
			&& InstanceMaps.cameraOnOverworld(camera.getCenterY());
		if (camera.isInteriorMapUnavailable() || projectionHides || instancedAbove)
		{
			final WorldPoint surface = camera.getLastSurfaceLocation();
			if (surface == null)
			{
				return;
			}
			location = surface;
			below = true;
		}

		final Point2D arranged = below ? null : arrangedPlayerPoint(rawLocation, bounds);
		final int x = (int) (arranged != null
			? arranged.getX()
			: camera.screenX(location.getX() + 0.5, location.getY() + 0.5, bounds));
		final int y = (int) (arranged != null
			? arranged.getY()
			: camera.screenY(location.getX() + 0.5, location.getY() + 0.5, bounds));

		if (!bounds.contains(x, y))
		{
			return;
		}

		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		// net.runelite.api.coords.Angle: 0 is south, 512 west, 1024 north, 1536 east, which runs
		// clockwise on a north-up map - the same sense as Graphics2D.rotate. The arrow polygon
		// points north in its own frame, so orientation 0 needs a half turn to face south.
		final double facing = playerFacing(local);
		final AffineTransform previous = graphics.getTransform();
		final Stroke oldStroke = graphics.getStroke();
		graphics.translate(x, y);
		graphics.rotate(facing);

		graphics.setColor(PLAYER_EDGE);
		graphics.setStroke(PLAYER_ARROW_EDGE);
		graphics.drawPolygon(PLAYER_ARROW_X, PLAYER_ARROW_Y, 4);
		// Hollow while the player is on a layer below the overworld view: the arrow sits on their
		// entrance, not their real position, so it must not read as a solid fix.
		graphics.setColor(below ? PLAYER_FILL_BELOW : PLAYER_FILL);
		graphics.fillPolygon(PLAYER_ARROW_X, PLAYER_ARROW_Y, 4);

		graphics.setStroke(oldStroke);
		graphics.setTransform(previous);
	}

	private double playerFacing(Player player)
	{
		final double facing = Math.PI + (player.getOrientation() / 2048d) * 2 * Math.PI;
		final WorldView view = player.getWorldView();
		final WorldView main = client.getTopLevelWorldView();
		if (view == null || view.isTopLevel() || main == null)
		{
			return facing;
		}

		final WorldEntity entity = main.worldEntities().byIndex(view.getId());
		final LocalPoint origin = player.getLocalLocation();
		if (entity == null || origin == null)
		{
			return facing;
		}

		// Transform the player's facing vector from the boat deck into the overworld,
		// just as WorldPointResolver resolves the boat's position into map coordinates.
		final LocalPoint ahead = new LocalPoint(
			origin.getX() + (int) Math.round(Math.sin(facing) * 128),
			origin.getY() + (int) Math.round(Math.cos(facing) * 128), view);
		final LocalPoint worldOrigin = entity.transformToMainWorld(origin);
		final LocalPoint worldAhead = entity.transformToMainWorld(ahead);
		return Math.atan2(worldAhead.getX() - worldOrigin.getX(),
			worldAhead.getY() - worldOrigin.getY());
	}

}
