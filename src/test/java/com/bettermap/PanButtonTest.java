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
package com.bettermap;

import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import javax.swing.JPanel;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class PanButtonTest
{
	@Test
	public void matchesOnlyTheConfiguredButton()
	{
		assertTrue(PanButton.LEFT.matches(press(MouseEvent.BUTTON1, InputEvent.BUTTON1_DOWN_MASK)));
		assertFalse(PanButton.LEFT.matches(press(MouseEvent.BUTTON2, InputEvent.BUTTON2_DOWN_MASK)));
		assertFalse(PanButton.LEFT.matches(press(MouseEvent.BUTTON3, InputEvent.BUTTON3_DOWN_MASK)));

		assertTrue(PanButton.MIDDLE.matches(press(MouseEvent.BUTTON2, InputEvent.BUTTON2_DOWN_MASK)));
		assertFalse(PanButton.MIDDLE.matches(press(MouseEvent.BUTTON1, InputEvent.BUTTON1_DOWN_MASK)));

		assertTrue(PanButton.RIGHT.matches(press(MouseEvent.BUTTON3, InputEvent.BUTTON3_DOWN_MASK)));
		assertFalse(PanButton.RIGHT.matches(press(MouseEvent.BUTTON1, InputEvent.BUTTON1_DOWN_MASK)));
	}

	private static MouseEvent press(int button, int modifiersEx)
	{
		return new MouseEvent(
			new JPanel(),
			MouseEvent.MOUSE_PRESSED,
			0L,
			modifiersEx,
			10,
			10,
			1,
			false,
			button);
	}
}
