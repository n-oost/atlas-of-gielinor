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
package atlasofgielinor.map.finder;

import com.google.inject.ImplementedBy;
import java.util.function.Consumer;

/**
 * The one seam between {@link FinderKeyCapture} and RuneLite's {@code ChatboxPanelManager}.
 *
 * <p>{@code ChatboxPanelManager} cannot be constructed in a unit test - it needs the injected
 * client, event bus and input managers - so the finder depends on this two-method view instead.
 * {@link RuneliteChatboxInputHost} is the production wiring; tests pass a fake that records the
 * callbacks and fires them by hand.
 */
@ImplementedBy(RuneliteChatboxInputHost.class)
public interface ChatboxInputHost
{
	/**
	 * Open a single-line chatbox text input.
	 *
	 * @param prompt    label shown above the field
	 * @param value     initial text
	 * @param onChanged  fired on every edit with the full current text
	 * @param onDone     fired when the user presses Enter; the input then closes
	 * @param onClose    fired once when the input closes, for any reason (Enter, Esc, or a
	 *                   {@link #close()} call)
	 */
	void open(String prompt, String value, Consumer<String> onChanged, Consumer<String> onDone, Runnable onClose);

	/** Close the current input if one is open. Safe to call when nothing is open. */
	void close();
}
