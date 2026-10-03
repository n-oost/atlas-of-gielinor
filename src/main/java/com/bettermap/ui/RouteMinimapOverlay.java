package com.bettermap.ui;

import com.bettermap.map.ShortestPathTracker;
import com.bettermap.pathfinding.PrimitiveIntList;
import com.bettermap.pathfinding.WorldPointUtil;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.SpriteID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/** Draws the local router's tiles using the minimap's rotation, zoom, and native mask. */
@Singleton
public class RouteMinimapOverlay extends Overlay
{
	private final Client client;
	private final ShortestPathTracker tracker;
	private final SpriteManager spriteManager;
	private final List<LocalPoint> localRoute = new ArrayList<>();
	private List<WorldPoint> cachedRoute;
	private WorldView cachedWorldView;
	private int cachedBaseX;
	private int cachedBaseY;
	private int cachedPlane;
	private int[][][] cachedChunks;
	private Shape fixedMask;
	private Shape resizedMask;

	@Inject
	public RouteMinimapOverlay(Client client, ShortestPathTracker tracker, SpriteManager spriteManager)
	{
		this.client = client;
		this.tracker = tracker;
		this.spriteManager = spriteManager;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPriority(PRIORITY_LOW);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		final List<WorldPoint> route = tracker.route();
		final WorldView worldView = client.getTopLevelWorldView();
		if (client.getGameState() != GameState.LOGGED_IN || worldView == null
			|| route.isEmpty() || !tracker.isRoutingEnabled())
		{
			cachedRoute = null;
			localRoute.clear();
			return null;
		}

		final int widgetId = !client.isResized() ? InterfaceID.Toplevel.MINIMAP
			: client.getVarbitValue(VarbitID.RESIZABLE_STONE_ARRANGEMENT) == 1
				? InterfaceID.ToplevelPreEoc.MINIMAP : InterfaceID.ToplevelOsrsStretch.MINIMAP;
		final Widget minimap = client.getWidget(widgetId);
		if (minimap == null || minimap.isHidden())
		{
			return null;
		}

		updateLocalRoute(route, worldView);
		final Graphics2D g = (Graphics2D) graphics.create();
		try
		{
			g.clip(minimapClip(minimap.getBounds()));
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
			g.setColor(MapStyle.ROUTE_LINE);
			final double angle = client.getCameraYawTarget() * Perspective.UNIT;
			final double tileSize = client.getMinimapZoom();
			final int size = Math.max(1, (int) Math.round(tileSize));
			final AffineTransform transform = g.getTransform();
			for (LocalPoint point : localRoute)
			{
				final Point center = Perspective.localToMinimap(client, point);
				if (center == null)
				{
					continue;
				}
				g.rotate(angle, center.getX(), center.getY());
				g.fillRect((int) Math.round(center.getX() - tileSize / 2),
					(int) Math.round(center.getY() - tileSize / 2), size, size);
				g.setTransform(transform);
			}
		}
		finally
		{
			g.dispose();
		}
		return null;
	}

	private void updateLocalRoute(List<WorldPoint> route, WorldView worldView)
	{
		final int[][][] chunks = worldView.isInstance() ? worldView.getInstanceTemplateChunks() : null;
		if (cachedRoute == route && cachedWorldView == worldView
			&& cachedBaseX == worldView.getBaseX() && cachedBaseY == worldView.getBaseY()
			&& cachedPlane == worldView.getPlane() && cachedChunks == chunks)
		{
			return;
		}
		cachedRoute = route;
		cachedWorldView = worldView;
		cachedBaseX = worldView.getBaseX();
		cachedBaseY = worldView.getBaseY();
		cachedPlane = worldView.getPlane();
		cachedChunks = chunks;
		localRoute.clear();
		for (WorldPoint point : route)
		{
			final PrimitiveIntList instances = WorldPointUtil.toLocalInstance(client, WorldPointUtil.packWorldPoint(point));
			for (int i = 0; i < instances.size(); i++)
			{
				// Filter the mapped plane, which can differ from the template plane in an instance.
				final LocalPoint local = WorldPointUtil.toLocalPoint(client, instances.get(i));
				if (local != null)
				{
					localRoute.add(local);
				}
			}
		}
	}

	private Shape minimapClip(Rectangle bounds)
	{
		final boolean resized = client.isResized();
		Shape mask = resized ? resizedMask : fixedMask;
		if (mask == null)
		{
			final BufferedImage image = spriteManager.getSprite(
				resized ? SpriteID.RESIZE_MAP_MASK : SpriteID.FIXED_MAP_MASK, 0);
			if (image == null)
			{
				return new Ellipse2D.Double(bounds.x, bounds.y, bounds.width, bounds.height);
			}
			// Cache horizontal spans of the mask's interior, preserving the compass cutout.
			final Path2D spans = new Path2D.Double();
			final int outside = image.getRGB(0, 0);
			for (int y = 0; y < image.getHeight(); y++)
			{
				int x = 0;
				while (x < image.getWidth())
				{
					while (x < image.getWidth() && image.getRGB(x, y) == outside)
					{
						x++;
					}
					final int start = x;
					while (x < image.getWidth() && image.getRGB(x, y) != outside)
					{
						x++;
					}
					if (x > start)
					{
						spans.append(new Rectangle(start, y, x - start, 1), false);
					}
				}
			}
			mask = spans;
			if (resized)
			{
				resizedMask = mask;
			}
			else
			{
				fixedMask = mask;
			}
		}
		return AffineTransform.getTranslateInstance(bounds.x, bounds.y).createTransformedShape(mask);
	}
}
