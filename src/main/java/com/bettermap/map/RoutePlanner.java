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

import com.bettermap.pathfinding.WorldPointUtil;
import com.bettermap.pathfinding.pathfinder.PathStep;
import com.bettermap.pathfinding.pathfinder.PathTerminationReason;
import com.bettermap.pathfinding.pathfinder.Pathfinder;
import com.bettermap.pathfinding.pathfinder.PathfinderConfig;
import com.bettermap.pathfinding.pathfinder.PathfinderResult;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;

/**
 * Computes a walking route to the active path target with Better Map's own vendored copy of the
 * Shortest Path engine ({@code com.bettermap.pathfinding}), so the world-map route line needs no
 * dependency on and no reflection into the Shortest Path plugin.
 *
 * <p>Threading: a single dedicated worker thread owns the {@link PathfinderConfig} — its
 * {@code CollisionMap} is a {@link ThreadLocal}, so the config and every search that reads it must
 * share one thread. {@link PathfinderConfig#refresh()} reads live client state (and needs the
 * client thread for its transport pass), so each request runs worker &rarr; client (refresh + read
 * the start tile) &rarr; worker (search). A generation counter discards the result of any search
 * that a newer request has since superseded.
 *
 * <p>{@code cfg.bank} and {@code cfg.availableSpiritTrees} are left null: bank-path chaining and
 * planted spirit trees are simply not modelled here. Everything else the upstream engine offers
 * (teleports, fairy rings, shortcuts) works from the vendored data.
 */
@Slf4j
@Singleton
public class RoutePlanner
{
	private static final long PRIME_TIMEOUT_SECONDS = 5;

	private final Client client;
	private final ClientThread clientThread;
	private final ShortestPathTracker tracker;
	private final SyncedPathfindingConfig pathfindingConfig;

	private ExecutorService worker = newWorker();

	private static ExecutorService newWorker()
	{
		return Executors.newSingleThreadExecutor(r ->
		{
			final Thread t = new Thread(r, "bettermap-pathfinder");
			t.setDaemon(true);
			return t;
		});
	}

	public synchronized void startUp()
	{
		if (worker.isShutdown())
		{
			worker = newWorker();
		}
	}

	/** Built once, lazily, on the worker thread — heavy resource I/O, never touch the client thread. */
	private final ThreadLocal<PathfinderConfig> config = new ThreadLocal<>();
	private volatile Pathfinder active;
	private final AtomicInteger generation = new AtomicInteger();

	@Inject
	RoutePlanner(Client client, ClientThread clientThread, ShortestPathTracker tracker, SyncedPathfindingConfig pathfindingConfig)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.tracker = tracker;
		this.pathfindingConfig = pathfindingConfig;
	}

	/**
	 * Start a background search to {@code target}, cancelling any in-flight one. The result is
	 * published through {@link ShortestPathTracker#publishRouteIfCurrent}. Safe to call from any thread.
	 */
	public synchronized void computeRoute(WorldPoint target)
	{
		if (worker.isShutdown())
		{
			return;
		}
		if (target == null)
		{
			cancel();
			return;
		}
		final long targetRevision = tracker.getRouteRevision(target);
		if (targetRevision < 0)
		{
			return;
		}
		final int requestGeneration = generation.incrementAndGet();
		cancelActive();
		worker.execute(() -> runSearch(requestGeneration, targetRevision, target));
	}

	/** Cancel an in-flight search and stop any result from being published. */
	public synchronized void cancel()
	{
		generation.incrementAndGet();
		cancelActive();
	}

	/** Called from the plugin's {@code shutDown()}. */
	public synchronized void shutdown()
	{
		cancel();
		worker.shutdownNow();
	}

	private void cancelActive()
	{
		final Pathfinder current = active;
		if (current != null)
		{
			current.cancel();
			active = null;
		}
	}

	private boolean superseded(int requestGeneration)
	{
		return requestGeneration != generation.get();
	}

	private void runSearch(int requestGeneration, long targetRevision, WorldPoint target)
	{
		try
		{
			if (superseded(requestGeneration))
			{
				return;
			}

			final PathfinderConfig cfg = ensureConfig();

			// Hop to the client thread for the live-state refresh and the start tile, then block
			// this worker until it has run. Submitting the search only afterwards gives the
			// happens-before edge for every field refresh() wrote.
			final int[] start = {WorldPointUtil.UNDEFINED};
			final CompletableFuture<Void> primed = new CompletableFuture<>();
			clientThread.invoke(() ->
			{
				if (primed.isDone() || superseded(requestGeneration))
				{
					primed.complete(null);
					return;
				}
				try
				{
					final Player local = client.getLocalPlayer();
					if (client.getGameState() == GameState.LOGGED_IN && local != null)
					{
						cfg.refresh();
						start[0] = WorldPointUtil.fromLocalInstance(client, local);
					}
				}
				catch (RuntimeException | LinkageError e)
				{
					log.debug("[BetterMap:route] refresh failed", e);
				}
				finally
				{
					primed.complete(null);
				}
			});

			try
			{
				primed.get(PRIME_TIMEOUT_SECONDS, TimeUnit.SECONDS);
			}
			finally
			{
				primed.cancel(false);
			}
			if (start[0] == WorldPointUtil.UNDEFINED)
			{
				publish(requestGeneration, targetRevision, null, ShortestPathTracker.RouteStatus.FAILED);
				return;
			}
			if (superseded(requestGeneration))
			{
				return;
			}

			final Pathfinder pathfinder = new Pathfinder(cfg, start[0],
				Set.of(WorldPointUtil.packWorldPoint(target)));
			// Publish the cancel handle before the last generation check: a retarget that lands
			// after this point runs cancelActive() and sees this pathfinder, so run() exits early.
			synchronized (this)
			{
				if (superseded(requestGeneration))
				{
					return;
				}
				active = pathfinder;
			}
			pathfinder.run();

			if (superseded(requestGeneration))
			{
				return;
			}

			final PathfinderResult result = pathfinder.getResult();
			final List<PathStep> steps = result != null ? result.getPathSteps() : null;
			if (steps == null || steps.isEmpty())
			{
				publish(requestGeneration, targetRevision, null, ShortestPathTracker.RouteStatus.FAILED);
				return;
			}

			final int[] packed = new int[steps.size()];
			for (int i = 0; i < packed.length; i++)
			{
				packed[i] = steps.get(i).getPackedPosition();
			}
			boolean full = result.isReached()
				&& result.getTerminationReason() == PathTerminationReason.TARGET_REACHED;
			if (full)
			{
				final int unreachableThreshold = pathfindingConfig.unreachableTargetDistance();
				if (unreachableThreshold >= 0)
				{
					final int targetPacked = WorldPointUtil.packWorldPoint(target);
					final int endDistance = WorldPointUtil.distanceBetween2D(
						packed[packed.length - 1], targetPacked);
					if (endDistance > unreachableThreshold)
					{
						full = false;
					}
				}
			}
			publish(requestGeneration, targetRevision, packed,
				full ? ShortestPathTracker.RouteStatus.READY : ShortestPathTracker.RouteStatus.UNREACHABLE);
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
		}
		catch (ExecutionException | TimeoutException | RuntimeException | LinkageError e)
		{
			log.warn("[BetterMap:route] search failed", e);
			publish(requestGeneration, targetRevision, null, ShortestPathTracker.RouteStatus.FAILED);
		}
		finally
		{
			synchronized (this)
			{
				if (!superseded(requestGeneration))
				{
					active = null;
				}
			}
		}
	}

	private synchronized void publish(int requestGeneration, long targetRevision, int[] points,
		ShortestPathTracker.RouteStatus status)
	{
		if (!superseded(requestGeneration))
		{
			tracker.publishRouteIfCurrent(targetRevision, points, status);
		}
	}

	private PathfinderConfig ensureConfig()
	{
		PathfinderConfig current = config.get();
		if (current == null)
		{
			current = new PathfinderConfig(client, pathfindingConfig);
			config.set(current);
		}
		return current;
	}
}
