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

import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.game.chatbox.ChatboxPanelManager;

/**
 * Production {@link ChatboxInputHost}: a thin wrapper over RuneLite's {@code ChatboxPanelManager}.
 *
 * <p>The chatbox text input is the sanctioned way for a plugin to take typed text. While it is
 * open the core <b>Key Remapping</b> plugin stands down - its {@code chatboxFocused()} check
 * returns false - so WASD reaches the field instead of driving the camera. That is the property
 * the on-map finder needs and cannot get from a {@code KeyListener}, which sits behind the core
 * plugins in {@code KeyManager}'s no-priority dispatch order.
 *
 * <p>The builder's default char validator is printable ASCII {@code [32,127)}; the query length
 * cap lives in {@link MapFinder#updateQuery}.
 */
@Singleton
public class RuneliteChatboxInputHost implements ChatboxInputHost
{
	private final ChatboxPanelManager chatboxPanelManager;

	@Inject
	public RuneliteChatboxInputHost(ChatboxPanelManager chatboxPanelManager)
	{
		this.chatboxPanelManager = chatboxPanelManager;
	}

	@Override
	public void open(String prompt, String value, Consumer<String> onChanged, Consumer<String> onDone, Runnable onClose)
	{
		chatboxPanelManager.openTextInput(prompt)
			.value(value == null ? "" : value)
			.onChanged(onChanged)
			.onDone(onDone)
			.onClose(onClose)
			.build();
	}

	@Override
	public void close()
	{
		chatboxPanelManager.close();
	}
}
