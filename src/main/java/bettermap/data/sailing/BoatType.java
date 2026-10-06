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
package bettermap.data.sailing;

import lombok.Getter;

@Getter
public enum BoatType
{
	RAFT(8110, 0, "Raft", 7111),
	SKIFF(8111, 1, "Skiff", 7112),
	SLOOP(8112, 2, "Sloop", 7113),
	TUTORIAL(8113, 3, "Will and Anne's boat", 7112);

	private final int dbRow;
	private final int id;
	private final String name;
	private final int spriteId;

	BoatType(int dbRow, int id, String name, int spriteId)
	{
		this.dbRow = dbRow;
		this.id = id;
		this.name = name;
		this.spriteId = spriteId;
	}

	public static BoatType fromDBRow(int dbRow)
	{
		for (BoatType p : values())
		{
			if (p.dbRow == dbRow)
			{
				return p;
			}
		}
		return null;
	}

	public static BoatType fromId(int id)
	{
		for (BoatType p : values())
		{
			if (p.id == id)
			{
				return p;
			}
		}
		return null;
	}

	@Override
	public String toString()
	{
		return name;
	}
}
