package atlasofgielinor.map;

import static net.runelite.api.Constants.CHUNK_SIZE;

import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.WorldEntity;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

/** Resolves a player's local position to the corresponding map world point. */
public final class WorldPointResolver
{
	private WorldPointResolver()
	{
	}

	public static WorldPoint fromLocalInstance(Client client, Player player)
	{
		WorldView worldView = player.getWorldView();
		int worldViewId = worldView.getId();
		if (worldViewId != WorldView.TOPLEVEL)
		{
			WorldEntity worldEntity = client.getTopLevelWorldView().worldEntities().byIndex(worldViewId);
			return fromLocalInstance(client, worldEntity.getLocalLocation());
		}
		return fromLocalInstance(client, player.getLocalLocation());
	}

	private static WorldPoint fromLocalInstance(Client client, LocalPoint localPoint)
	{
		WorldView worldView = client.getWorldView(localPoint.getWorldView());
		int plane = worldView.getPlane();
		if (!worldView.isInstance())
		{
			return WorldPoint.fromLocal(worldView, localPoint.getX(), localPoint.getY(), plane);
		}
		// Preserve the caller's raw-location fallback for a point outside the template array.
		int chunkX = localPoint.getSceneX() / CHUNK_SIZE;
		int chunkY = localPoint.getSceneY() / CHUNK_SIZE;
		int[][] chunks = worldView.getInstanceTemplateChunks()[plane];
		if (chunkX < 0 || chunkX >= chunks.length || chunkY < 0 || chunkY >= chunks[chunkX].length)
		{
			return null;
		}
		return WorldPoint.fromLocalInstance(client, localPoint, plane);
	}
}
