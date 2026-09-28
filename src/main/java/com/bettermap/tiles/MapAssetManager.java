package com.bettermap.tiles;

import com.google.gson.Gson;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.util.Filepath;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/** Installs complete, compatible imagery packs before exposing them to the renderer. */
@Slf4j
@Singleton
public class MapAssetManager
{
	static final String REPOSITORY = "https://raw.githubusercontent.com/n-oost/better-map-assets/";
	static final String CHANNEL_URL = REPOSITORY + "main/channels/tiles-v1.json";
	static final String COMPATIBILITY = "better-map-tiles-v1";
	private static final long MAX_ARCHIVE = 128L * 1024 * 1024;
	private static final long MAX_EXPANDED = 256L * 1024 * 1024;
	private static final int MAX_JSON = 8 * 1024 * 1024;
	private static final String TILE_PATH = "tiles/\\d+/(-[1-3]|[0-3])/[0-3]_\\d{1,5}_\\d{1,5}\\.png";

	private final OkHttpClient http;
	private final Gson gson;
	private final TileLoader tiles;
	private ExecutorService worker;
	private Call call;
	private int generation;

	@Inject
	public MapAssetManager(OkHttpClient http, Gson gson, TileLoader tiles)
	{
		this.http = http;
		this.gson = gson;
		this.tiles = tiles;
	}

	public synchronized void startUp(java.util.concurrent.Callable<Filepath> directory, boolean downloadsEnabled)
	{
		shutDown();
		final int session = generation;
		tiles.setStatus("Checking installed map assets...");
		worker = Executors.newSingleThreadExecutor(r ->
		{
			Thread thread = new Thread(r, "better-map-assets");
			thread.setDaemon(true);
			return thread;
		});
		worker.execute(() ->
		{
			try
			{
				load(directory.call(), downloadsEnabled, session);
			}
			catch (Exception e)
			{
				log.debug("[BetterMap] asset storage unavailable", e);
				status(session, "Map asset storage is unavailable. Check the RuneLite log and restart Better Map.");
			}
		});
	}

	public synchronized void shutDown()
	{
		generation++;
		if (call != null)
		{
			call.cancel();
			call = null;
		}
		if (worker != null)
		{
			worker.shutdownNow();
			worker = null;
		}
		tiles.shutDown();
	}

	private synchronized boolean current(int session)
	{
		return session == generation;
	}

	private synchronized void status(int session, String message)
	{
		if (current(session))
		{
			tiles.setStatus(message);
		}
	}

	private void load(Filepath root, boolean downloadsEnabled, int session)
	{
		Installation installed = null;
		try
		{
			Filepath marker = root.join("active.json");
			if (marker.isFile())
			{
				Installation candidate = json(marker, Installation.class, 16384);
				validateChannel(candidate.channel);
				require(candidate.directory != null && candidate.directory.matches("pack-[A-Za-z0-9-]+"), "Invalid installation path");
				Filepath directory = root.joinSegment(candidate.directory).rooted();
				byte[] inventory = bytes(directory.join("inventory.json"), MAX_JSON);
				require(hash(inventory).equals(candidate.inventorySha256), "Installed inventory changed");
				List<String> paths = validateInstalled(directory, candidate.channel, inventory);
				activate(session, directory, paths);
				installed = candidate;
			}
		}
		catch (IOException | RuntimeException e)
		{
			log.debug("[BetterMap] installed pack failed validation", e);
			status(session, "Installed map assets are incomplete or damaged. Enable map downloads to reinstall.");
		}
		if (!current(session))
		{
			return;
		}
		if (!downloadsEnabled)
		{
			if (installed == null)
			{
				status(session, "Map assets are not installed or are invalid. Enable Download map assets in Better Map settings.");
			}
			return;
		}
		final Installation existing = installed;
		status(session, "Checking for map downloads...");
		request(CHANNEL_URL, session, response ->
		{
			Channel channel = gson.fromJson(new String(read(response.body().byteStream(), 16384), StandardCharsets.UTF_8), Channel.class);
			validateChannel(channel);
			if (existing != null && existing.channel.pack.sha256.equals(channel.pack.sha256))
			{
				return;
			}
			status(session, "Downloading map assets...");
			request(REPOSITORY + channel.commit + "/" + channel.pack.path, session,
				packResponse -> download(root, channel, packResponse, session));
		});
	}

	private synchronized void activate(int session, Filepath directory, List<String> paths)
	{
		if (current(session))
		{
			tiles.install(directory.join("tiles"), paths);
		}
	}

	@FunctionalInterface
	private interface ResponseHandler
	{
		void accept(Response response) throws IOException;
	}

	private synchronized void request(String url, int session, ResponseHandler handler)
	{
		if (!current(session))
		{
			return;
		}
		call = http.newCall(new Request.Builder().url(url).build());
		call.enqueue(new Callback()
		{
			@Override
			public void onFailure(Call failed, IOException e)
			{
				failed(session, e);
			}

			@Override
			public void onResponse(Call completed, Response response)
			{
				try (Response close = response)
				{
					if (!current(session))
					{
						return;
					}
					require(response.isSuccessful() && response.body() != null, "Asset server returned HTTP " + response.code());
					handler.accept(response);
				}
				catch (IOException | RuntimeException e)
				{
					failed(session, e);
				}
			}
		});
	}

	private void failed(int session, Exception e)
	{
		log.debug("[BetterMap] asset download failed", e);
		status(session, "Map download failed. Check your connection and toggle Download map assets to retry.");
	}

	private void download(Filepath root, Channel channel, Response response, int session) throws IOException
	{
		root.createDirectories();
		Filepath archive = root.createTempFile("download-", ".zip");
		Filepath directory = null;
		boolean committed = false;
		try
		{
			MessageDigest digest = digest();
			long count = 0;
			try (InputStream in = response.body().byteStream(); OutputStream out = archive.openOutputStream())
			{
				byte[] buffer = new byte[65536];
				int n;
				while ((n = in.read(buffer)) != -1)
				{
					require(current(session), "Download cancelled");
					count += n;
					require(count <= channel.pack.compressedBytes, "Archive exceeds declared size");
					digest.update(buffer, 0, n);
					out.write(buffer, 0, n);
				}
			}
			require(count == channel.pack.compressedBytes && hex(digest.digest()).equals(channel.pack.sha256), "Archive hash or size mismatch");
			status(session, "Installing map assets...");
			directory = root.createTempDir("pack-").rooted();
			extract(archive, directory, channel);
			byte[] inventory = bytes(directory.join("inventory.json"), MAX_JSON);
			List<String> paths = validateInstalled(directory, channel, inventory);
			Installation installation = new Installation();
			installation.channel = channel;
			installation.directory = directory.getFileName();
			installation.inventorySha256 = hash(inventory);
			Filepath marker = root.createTempFile("active-", ".json");
			try
			{
				marker.write(gson.toJson(installation));
				synchronized (this)
				{
					require(current(session), "Installation cancelled");
					marker.moveTo(root.join("active.json"), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
					committed = true;
					activate(session, directory, paths);
				}
			}
			finally
			{
				marker.deleteIfExists();
			}
		}
		finally
		{
			archive.deleteIfExists();
			if (!committed && directory != null)
			{
				directory.deleteRecursively();
			}
		}
	}

	static void validateChannel(Channel channel) throws IOException
	{
		require(channel != null && channel.schemaVersion == 1 && COMPATIBILITY.equals(channel.compatibilityId), "Unsupported asset channel");
		require(channel.commit != null && channel.commit.matches("[0-9a-f]{40}"), "Invalid asset commit");
		Pack pack = channel.pack;
		require(pack != null && pack.sha256 != null && pack.sha256.matches("[0-9a-f]{64}"), "Invalid pack hash");
		require(("packs/" + pack.sha256 + ".zip").equals(pack.path), "Invalid pack path");
		require(pack.compressedBytes > 0 && pack.compressedBytes <= MAX_ARCHIVE
			&& pack.uncompressedBytes > 0 && pack.uncompressedBytes <= MAX_EXPANDED
			&& pack.fileCount > 2 && pack.fileCount <= 50000, "Pack exceeds supported limits");
	}

	static void extract(Filepath archive, Filepath directory, Channel channel) throws IOException
	{
		Set<String> names = new HashSet<>();
		long expanded = 0;
		try (ZipInputStream zip = new ZipInputStream(archive.openInputStream()))
		{
			ZipEntry entry;
			while ((entry = zip.getNextEntry()) != null)
			{
				String name = entry.getName();
				require(!entry.isDirectory() && validPath(name) && names.add(name), "Invalid or duplicate pack entry");
				require(names.size() <= channel.pack.fileCount, "Too many pack entries");
				byte[] data = read(zip, name.endsWith(".png") ? 1048576 : MAX_JSON);
				expanded += data.length;
				require(expanded <= channel.pack.uncompressedBytes, "Expanded pack exceeds declared size");
				Filepath target = directory.join(name);
				target.getParent().createDirectories();
				target.write(data);
			}
		}
		require(names.size() == channel.pack.fileCount && expanded == channel.pack.uncompressedBytes, "Incomplete archive");
	}

	List<String> validateInstalled(Filepath directory, Channel channel, byte[] rawInventory) throws IOException
	{
		Inventory inventory = gson.fromJson(new String(rawInventory, StandardCharsets.UTF_8), Inventory.class);
		require(inventory != null && inventory.schemaVersion == 1 && COMPATIBILITY.equals(inventory.compatibilityId)
			&& inventory.files != null && inventory.files.size() + 1 == channel.pack.fileCount, "Invalid pack inventory");
		Set<String> expectedTiles = new HashSet<>();
		long expanded = rawInventory.length;
		for (Map.Entry<String, Entry> item : inventory.files.entrySet())
		{
			String path = item.getKey();
			Entry entry = item.getValue();
			require(validPath(path) && !path.equals("inventory.json") && entry != null, "Invalid inventory path");
			int limit = path.endsWith(".png") ? 1048576 : MAX_JSON;
			require(entry.size > 0 && entry.size <= limit, "Invalid inventory size");
			byte[] data = bytes(directory.join(path), limit);
			require(data.length == entry.size && hash(data).equals(entry.sha256), "Missing or damaged pack file: " + path);
			expanded += data.length;
			require(expanded <= channel.pack.uncompressedBytes, "Installed pack exceeds declared size");
			if (path.endsWith(".png"))
			{
				require(data.length >= 24 && Arrays.equals(Arrays.copyOf(data, 8), new byte[]{(byte) 137, 80, 78, 71, 13, 10, 26, 10})
					&& ByteBuffer.wrap(data, 12, 12).getInt() == 0x49484452
					&& ByteBuffer.wrap(data, 16, 8).getInt() == 256 && ByteBuffer.wrap(data, 20, 4).getInt() == 256, "Invalid tile dimensions");
				expectedTiles.add(path.substring(6));
			}
		}
		require(expanded == channel.pack.uncompressedBytes && inventory.files.containsKey("tiles/index.txt"), "Incomplete inventory");
		List<String> paths = new ArrayList<>();
		try (java.io.BufferedReader reader = directory.join("tiles/index.txt").openBufferedReader())
		{
			String line;
			while ((line = reader.readLine()) != null)
			{
				paths.add(line);
			}
		}
		require(paths.size() == expectedTiles.size() && new HashSet<>(paths).equals(expectedTiles)
			&& paths.stream().anyMatch(p -> p.startsWith("0/")), "Tile index does not match inventory");
		return paths;
	}

	private <T> T json(Filepath file, Class<T> type, int limit) throws IOException
	{
		return gson.fromJson(new String(bytes(file, limit), StandardCharsets.UTF_8), type);
	}

	private static boolean validPath(String name)
	{
		return name.equals("inventory.json") || name.equals("tiles/index.txt") || name.matches(TILE_PATH);
	}

	static byte[] bytes(Filepath file, int limit) throws IOException
	{
		try (InputStream in = file.openInputStream())
		{
			return read(in, limit);
		}
	}

	static byte[] read(InputStream in, int limit) throws IOException
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buffer = new byte[8192];
		int n;
		while ((n = in.read(buffer)) != -1)
		{
			require(!Thread.currentThread().isInterrupted(), "Asset work cancelled");
			require((long) out.size() + n <= limit, "Asset file exceeds size limit");
			out.write(buffer, 0, n);
		}
		return out.toByteArray();
	}

	static String hash(byte[] bytes)
	{
		return hex(digest().digest(bytes));
	}

	private static MessageDigest digest()
	{
		try
		{
			return MessageDigest.getInstance("SHA-256");
		}
		catch (NoSuchAlgorithmException e)
		{
			throw new IllegalStateException(e);
		}
	}

	private static String hex(byte[] digest)
	{
		StringBuilder text = new StringBuilder();
		for (byte b : digest)
		{
			text.append(String.format("%02x", b & 255));
		}
		return text.toString();
	}

	private static void require(boolean condition, String message) throws IOException
	{
		if (!condition)
		{
			throw new IOException(message);
		}
	}

	static final class Channel
	{
		int schemaVersion;
		String compatibilityId;
		String commit;
		Pack pack;
	}

	static final class Pack
	{
		String path;
		String sha256;
		long compressedBytes;
		long uncompressedBytes;
		int fileCount;
	}

	static final class Inventory
	{
		int schemaVersion;
		String compatibilityId;
		Map<String, Entry> files;
	}

	static final class Entry
	{
		long size;
		String sha256;
	}

	static final class Installation
	{
		Channel channel;
		String directory;
		String inventorySha256;
	}
}
