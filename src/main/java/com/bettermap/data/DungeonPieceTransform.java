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
package com.bettermap.data;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;

/**
 * Flip-then-rotate used by the dungeon-layer tuner. Positive {@code rot} is clockwise on screen
 * because Y grows down.
 */
public final class DungeonPieceTransform
{
	private DungeonPieceTransform()
	{
	}

	public static AffineTransform affine(int rot, boolean flipX, boolean flipY, double cx, double cy)
	{
		if (rot == 0 && !flipX && !flipY)
		{
			return new AffineTransform();
		}
		final AffineTransform t = AffineTransform.getRotateInstance(Math.toRadians(rot), cx, cy);
		if (flipX || flipY)
		{
			t.translate(cx, cy);
			t.scale(flipX ? -1 : 1, flipY ? -1 : 1);
			t.translate(-cx, -cy);
		}
		return t;
	}

	/** The tile renderer's transform expressed in map coordinates, where Y grows up. */
	private static AffineTransform displayTransform(DungeonPiece piece, int nudgeX, int nudgeY)
	{
		final int[] b = piece.srcBounds();
		return affine(-piece.rot, piece.flipX, piece.flipY,
			(b[0] + b[2] + 1) / 2.0 + piece.dx - nudgeX,
			(b[1] + b[3] + 1) / 2.0 + piece.dy - nudgeY);
	}

	public static Point2D toDisplay(DungeonPiece piece, double x, double y, int nudgeX, int nudgeY)
	{
		return displayTransform(piece, nudgeX, nudgeY).transform(
			new Point2D.Double(x + piece.dx - nudgeX, y + piece.dy - nudgeY), null);
	}

	public static Point2D toNative(DungeonPiece piece, double x, double y, int nudgeX, int nudgeY)
	{
		try
		{
			final Point2D point = displayTransform(piece, nudgeX, nudgeY).inverseTransform(
				new Point2D.Double(x, y), null);
			return new Point2D.Double(point.getX() - piece.dx + nudgeX,
				point.getY() - piece.dy + nudgeY);
		}
		catch (NoninvertibleTransformException e)
		{
			throw new IllegalStateException("Dungeon rotation and flip must be invertible", e);
		}
	}
}
