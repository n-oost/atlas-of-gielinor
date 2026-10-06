package bettermap.map;

import static net.runelite.api.Perspective.LOCAL_COORD_BITS;

import net.runelite.api.Client;
import net.runelite.api.Constants;
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
			return new WorldPoint(
				(localPoint.getX() >> LOCAL_COORD_BITS) + worldView.getBaseX(),
				(localPoint.getY() >> LOCAL_COORD_BITS) + worldView.getBaseY(),
				plane);
		}

		int sceneX = localPoint.getSceneX();
		int sceneY = localPoint.getSceneY();
		int chunkData = worldView.getInstanceTemplateChunks()[plane]
			[sceneX / Constants.CHUNK_SIZE][sceneY / Constants.CHUNK_SIZE];
		int rotation = chunkData >> 1 & 0x3;
		int templateChunkY = (chunkData >> 3 & 0x7FF) * Constants.CHUNK_SIZE;
		int templateChunkX = (chunkData >> 14 & 0x3FF) * Constants.CHUNK_SIZE;
		int templatePlane = chunkData >> 24 & 0x3;

		int x = templateChunkX + (sceneX & (Constants.CHUNK_SIZE - 1));
		int y = templateChunkY + (sceneY & (Constants.CHUNK_SIZE - 1));
		return rotate(x, y, templatePlane, 4 - rotation);
	}

	private static WorldPoint rotate(int originalX, int originalY, int plane, int rotation)
	{
		int chunkX = originalX & -Constants.CHUNK_SIZE;
		int chunkY = originalY & -Constants.CHUNK_SIZE;
		int x = originalX & (Constants.CHUNK_SIZE - 1);
		int y = originalY & (Constants.CHUNK_SIZE - 1);
		switch (rotation)
		{
			case 1:
				return new WorldPoint(chunkX + y, chunkY + Constants.CHUNK_SIZE - 1 - x, plane);
			case 2:
				return new WorldPoint(chunkX + Constants.CHUNK_SIZE - 1 - x,
					chunkY + Constants.CHUNK_SIZE - 1 - y, plane);
			case 3:
				return new WorldPoint(chunkX + Constants.CHUNK_SIZE - 1 - y, chunkY + x, plane);
			default:
				return new WorldPoint(originalX, originalY, plane);
		}
	}
}
