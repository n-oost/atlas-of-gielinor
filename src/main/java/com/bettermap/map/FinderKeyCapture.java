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

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import net.runelite.api.coords.WorldPoint;

/**
 * Keyboard input for the on-map Finder, via RuneLite's chatbox text input.
 *
 * <p>A {@code KeyListener} cannot win the keyboard: {@code KeyManager} dispatches in registration
 * order with no priority, so a sideloaded plugin sits behind the core <b>Key Remapping</b> plugin,
 * which rewrites WASD in place and eats the matching {@code keyTyped}. The chatbox text input is
 * the way out - while it is open Key Remapping's {@code chatboxFocused()} returns false and stands
 * down, so every key reaches the search field.
 *
 * <p>{@link #sync()} runs every frame and reconciles one fact: the chatbox input is open exactly
 * when the Finder card is interactive and its field holds focus. Opening and closing the card,
 * clicking the field, clicking off it, and losing window focus all just move that flag - this
 * class follows it.
 */
@Singleton
public class FinderKeyCapture
{
	private static final String PROMPT = "Map Finder";

	private final MapCamera camera;
	private final MapFinder finder;
	private final com.bettermap.BetterMapConfig config;
	private final Provider<com.bettermap.BetterMapPlugin> pluginProvider;
	private final ChatboxInputHost host;

	/** True between {@link #openInput()} and {@link #onInputClosed()}. */
	private boolean open;
	/**
	 * Set while <i>we</i> are the ones closing the input (the field lost focus), so
	 * {@link #onInputClosed()} can tell that apart from the user dismissing it with Esc or Enter -
	 * only the latter should also put the card away.
	 */
	private boolean programmaticClose;

	@Inject
	public FinderKeyCapture(
		MapCamera camera,
		MapFinder finder,
		com.bettermap.BetterMapConfig config,
		Provider<com.bettermap.BetterMapPlugin> pluginProvider,
		ChatboxInputHost host)
	{
		this.camera = camera;
		this.finder = finder;
		this.config = config;
		this.pluginProvider = pluginProvider;
		this.host = host;
	}

	/** True while the chatbox search input is (or is about to be) on screen. */
	public boolean isOpen()
	{
		return open;
	}

	/**
	 * Open or close the chatbox input to match the card's focus state. Cheap - two volatile reads
	 * and, on a transition, one deferred client-thread call. Safe to call every frame.
	 */
	public void sync()
	{
		if (host == null)
		{
			return;
		}

		final boolean want = camera.isFinderInteractive() && camera.isFinderFieldFocused();
		if (want && !open)
		{
			openInput();
		}
		else if (!want && open)
		{
			closeInput();
		}
	}

	/** Close the input on plugin shutdown so it is never left behind. */
	public void shutDown()
	{
		if (open && host != null)
		{
			programmaticClose = true;
			open = false;
			host.close();
		}
	}

	private void openInput()
	{
		open = true;
		// A stale flag from an open that never landed would poison the next Esc; clear it here too.
		programmaticClose = false;
		host.open(
			PROMPT,
			finder.getQuery(),
			this::onChanged,
			s -> onDone(),
			this::onInputClosed);
	}

	private void closeInput()
	{
		programmaticClose = true;
		open = false;
		host.close();
	}

	private void onChanged(String text)
	{
		finder.updateQuery(text, playerLocation());
		// The on-map field is a read-only mirror now; keep its scroll window on the tail.
		finder.caretEnd();
	}

	/** Enter shows the selected destination, opening the world map for Quick Finder. */
	private void onDone()
	{
		final MapFinder.Result target = finder.selectedResult();
		if (target == null)
		{
			return;
		}
		final com.bettermap.BetterMapPlugin plugin = pluginProvider == null ? null : pluginProvider.get();
		if (plugin == null)
		{
			return;
		}
		if (camera.isFinderStandalone())
		{
			plugin.openMapAt(target.getPoint());
		}
		else
		{
			plugin.centerMapOn(target.getPoint());
		}
	}

	private void onInputClosed()
	{
		open = false;
		if (!programmaticClose)
		{
			// User pressed Esc or Enter - put the card away too, the same as the old Esc handler.
			camera.setFinderPanelOpen(false);
		}
		programmaticClose = false;
	}

	private WorldPoint playerLocation()
	{
		final com.bettermap.BetterMapPlugin plugin = pluginProvider == null ? null : pluginProvider.get();
		return plugin == null ? null : plugin.getPlayerLocation();
	}

}
