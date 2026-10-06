package com.bettermap.tiles;

import com.google.gson.Gson;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;
import net.runelite.client.util.Filepath;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class MapAssetManagerTest
{
	@Rule public TemporaryFolder temporary = new TemporaryFolder();
	private final Gson gson = new Gson();

	@Test
	public void validatesPublishedSnapshotWhenProvided() throws Exception
	{
		String source = System.getenv("BETTER_MAP_TEST_ASSETS");
		org.junit.Assume.assumeNotNull(source);
		Filepath assets = Filepath.Unchecked.getRooted(java.nio.file.Paths.get(source));
		MapAssetManager manager = new MapAssetManager(new OkHttpClient(), gson, new TileLoader());
		MapAssetManager.Channel channel = manager.pinnedPack();
		Filepath archive = assets.join(channel.pack.path);
		assertEquals(channel.pack.sha256, MapAssetManager.hash(MapAssetManager.bytes(archive, 128 * 1024 * 1024)));
		MapAssetManager.extract(archive, root(), channel);
		java.util.List<String> paths = manager.validateInstalled(root(), channel,
			MapAssetManager.bytes(root().join("inventory.json"), 8 * 1024 * 1024));
		assertEquals(channel.pack.fileCount - 2, paths.size());
		assertTrue(paths.stream().anyMatch(path -> path.startsWith("0/2/")));
		assertTrue("Public pack must contain native zoom 3", paths.stream().anyMatch(path -> path.startsWith("0/3/")));
		assertTrue("Public pack must contain only map 0", paths.stream().allMatch(path -> path.startsWith("0/")));
		TileLoader tiles = new TileLoader();
		try
		{
			tiles.install(root().join("tiles"), paths);
			await(tiles::hasTiles);
			assertEquals(3, tiles.maxAvailableZoom());
		}
		finally
		{
			tiles.shutDown();
		}
	}

	@Test
	public void downloadsInstallsAndReloadsVerifiedPackWithoutNetwork() throws Exception
	{
		Fixture fixture = new Fixture();
		AtomicInteger requests = new AtomicInteger();
		OkHttpClient http = fixture.http(requests, false);
		TileLoader tiles = new TileLoader();
		MapAssetManager manager = fixture.manager(http, tiles);
		Filepath root = root();
		try
		{
			manager.startUp(() -> root, true);
			await(tiles::hasTiles);
			assertEquals(1, requests.get());
			assertEquals(3, tiles.maxAvailableZoom());
			assertNotNull(TileStore.read(tiles.getTileDir(), "0/3/0_1_1.png"));
			assertTrue(root.join("active.json").isFile());
			// An already installed pinned pack must not contact GitHub, even with downloads enabled.
			manager.startUp(() -> root, true);
			await(tiles::hasTiles);
			assertEquals(1, requests.get());
			manager.startUp(() -> root, false);
			await(tiles::hasTiles);
			assertEquals(1, requests.get());
			Filepath tile = tiles.getTileDir().join("0/3/0_1_1.png");
			manager.shutDown();
			tile.write("corrupt");
			manager.startUp(() -> root, false);
			await(() -> tiles.getStatus().contains("Enable Download"));
			assertFalse(tiles.hasTiles());
			assertNull(tiles.get(0, 3, 1, 1));
		}
		finally
		{
			manager.shutDown();
			http.dispatcher().executorService().shutdownNow();
		}
	}

	@Test
	public void disabledDownloadsNeverReadLegacyTilesOrContactServer() throws Exception
	{
		AtomicInteger requests = new AtomicInteger();
		Fixture fixture = new Fixture();
		OkHttpClient http = fixture.http(requests, false);
		TileLoader tiles = new TileLoader();
		MapAssetManager manager = fixture.manager(http, tiles);
		try
		{
			manager.startUp(this::root, false);
			await(() -> tiles.getStatus().contains("Enable Download"));
			assertEquals(0, requests.get());
			assertFalse(tiles.hasTiles());
		}
		finally
		{
			manager.shutDown();
		}
	}

	@Test
	public void badArchiveHashDoesNotActivateOrCommitInstallation() throws Exception
	{
		AtomicInteger requests = new AtomicInteger();
		Fixture fixture = new Fixture();
		OkHttpClient http = fixture.http(requests, true);
		TileLoader tiles = new TileLoader();
		MapAssetManager manager = fixture.manager(http, tiles);
		try
		{
			manager.startUp(this::root, true);
			await(() -> tiles.getStatus().contains("download failed"));
			assertFalse(tiles.hasTiles());
			assertFalse(root().join("active.json").exists());
		}
		finally
		{
			manager.shutDown();
			http.dispatcher().executorService().shutdownNow();
		}
	}

	@Test(expected = java.io.IOException.class)
	public void rejectsZipTraversal() throws Exception
	{
		Fixture fixture = new Fixture();
		Filepath archive = root().join("bad.zip");
		try (ZipOutputStream zip = new ZipOutputStream(archive.openOutputStream()))
		{
			zip.putNextEntry(new ZipEntry("../escape.png"));
			zip.write(1);
			zip.closeEntry();
		}
		MapAssetManager.extract(archive, root(), fixture.channel);
	}

	@Test(expected = java.io.IOException.class)
	public void rejectsIncompatibleChannel() throws Exception
	{
		Fixture fixture = new Fixture();
		fixture.channel.compatibilityId = "future-format";
		MapAssetManager.validateChannel(fixture.channel);
	}

	private Filepath root()
	{
		return Filepath.Unchecked.getRooted(temporary.getRoot().toPath());
	}

	private static void await(BooleanSupplier condition)
	{
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
		while (!condition.getAsBoolean() && System.nanoTime() < deadline)
		{
			java.util.concurrent.locks.LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(10));
		}
		assertTrue("Timed out waiting for asset state", condition.getAsBoolean());
	}

	private class Fixture
	{
		final MapAssetManager.Channel channel = new MapAssetManager.Channel();
		final byte[] archive;

		Fixture() throws Exception
		{
			ByteArrayOutputStream png = new ByteArrayOutputStream();
			ImageIO.write(new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB), "png", png);
			LinkedHashMap<String, byte[]> files = new LinkedHashMap<>();
			files.put("tiles/0/3/0_1_1.png", png.toByteArray());
			files.put("tiles/index.txt", "0/3/0_1_1.png\n".getBytes(StandardCharsets.UTF_8));
			MapAssetManager.Inventory inventory = new MapAssetManager.Inventory();
			inventory.schemaVersion = 1;
			inventory.compatibilityId = MapAssetManager.COMPATIBILITY;
			inventory.files = new LinkedHashMap<>();
			for (java.util.Map.Entry<String, byte[]> file : files.entrySet())
			{
				MapAssetManager.Entry entry = new MapAssetManager.Entry();
				entry.size = file.getValue().length;
				entry.sha256 = MapAssetManager.hash(file.getValue());
				inventory.files.put(file.getKey(), entry);
			}
			files.put("inventory.json", gson.toJson(inventory).getBytes(StandardCharsets.UTF_8));
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			try (ZipOutputStream zip = new ZipOutputStream(bytes))
			{
				for (java.util.Map.Entry<String, byte[]> file : files.entrySet())
				{
					zip.putNextEntry(new ZipEntry(file.getKey()));
					zip.write(file.getValue());
					zip.closeEntry();
				}
			}
			archive = bytes.toByteArray();
			channel.schemaVersion = 1;
			channel.compatibilityId = MapAssetManager.COMPATIBILITY;
			channel.commit = "0123456789abcdef0123456789abcdef01234567";
			channel.pack = new MapAssetManager.Pack();
			channel.pack.sha256 = MapAssetManager.hash(archive);
			channel.pack.path = "packs/" + channel.pack.sha256 + ".zip";
			channel.pack.compressedBytes = archive.length;
			channel.pack.uncompressedBytes = files.values().stream().mapToInt(b -> b.length).sum();
			channel.pack.fileCount = files.size();
		}

		OkHttpClient http(AtomicInteger requests, boolean corrupt)
		{
			return new OkHttpClient.Builder().addInterceptor(chain ->
			{
				requests.incrementAndGet();
				assertEquals(MapAssetManager.REPOSITORY + channel.commit + "/" + channel.pack.path,
					chain.request().url().toString());
				byte[] body = archive.clone();
				if (corrupt)
				{
					body[0] ^= 1;
				}
				return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
					.code(200).message("OK").body(ResponseBody.create(null, body)).build();
			}).build();
		}

		MapAssetManager manager(OkHttpClient http, TileLoader tiles)
		{
			return new MapAssetManager(http, gson, tiles)
			{
				@Override
				Channel pinnedPack()
				{
					return channel;
				}
			};
		}
	}
}
