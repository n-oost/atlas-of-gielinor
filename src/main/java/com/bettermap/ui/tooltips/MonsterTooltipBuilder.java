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
package com.bettermap.ui.tooltips;

import com.bettermap.data.MonsterLocationData;
import com.bettermap.map.MonsterIndex;
import java.awt.image.BufferedImage;

/**
 * Builds tooltip hovercards for bosses, monster spawn areas, and Slayer tasks.
 */
public class MonsterTooltipBuilder
{
	/**
	 * Builds a tooltip card for a boss marker.
	 */
	public TooltipCard buildBossCard(
		MonsterLocationData boss,
		BufferedImage icon,
		boolean isTaskBoss,
		int remainingTaskAmount,
		boolean compact,
		MonsterIndex.Zone stats)
	{
		if (boss == null)
		{
			return null;
		}

		final String title = boss.getName() + " (Lvl " + boss.getCombatLevel() + ")";
		final TooltipCard card = new TooltipCard(title, icon);

		if (isTaskBoss)
		{
			card.addLine(remainingTaskAmount > 0
				? "[Current Slayer Task \u2022 " + remainingTaskAmount + " remaining]"
				: "[Current Slayer Task]");
		}

		card.addLine("[Boss \u2022 " + boss.getLocationName() + "]");

		if (!compact)
		{
			card.addLine("Difficulty: Boss • Combat level " + boss.getCombatLevel());
			if (boss.getWeaknessStrategy() != null && !boss.getWeaknessStrategy().isEmpty())
			{
				card.addLine("Strategy: " + boss.getWeaknessStrategy());
			}
			if (boss.getKeyDrops() != null && !boss.getKeyDrops().isEmpty())
			{
				card.addLine("Unique drops: " + boss.getKeyDrops());
			}
			if (stats != null)
			{
				card.addLines(stats.combatSummaryLines());
			}
		}

		return card;
	}

	/**
	 * Builds a tooltip card for a monster habitat zone or spawn cluster.
	 */
	public TooltipCard buildMonsterZoneCard(
		MonsterIndex.Zone monster,
		BufferedImage icon,
		boolean isTask,
		int remainingTaskAmount,
		boolean compact)
	{
		if (monster == null)
		{
			return null;
		}

		final String title = monster.getMonster() + (monster.getCombatLevel() > 0 ? " (Lvl " + monster.getCombatLevel() + ")" : "");
		final TooltipCard card = new TooltipCard(title, icon);

		if (isTask)
		{
			card.addLine(remainingTaskAmount > 0
				? "[Current Slayer Task \u2022 " + remainingTaskAmount + " remaining]"
				: "[Current Slayer Task]");
		}

		if (monster.getLocationName() != null && !monster.getLocationName().isEmpty())
		{
			card.addLine("[Monster \u2022 " + monster.getLocationName() + "]");
		}

		if (monster.getSlayerLevel() > 1)
		{
			card.addLine("Slayer required: Level " + monster.getSlayerLevel() + " Slayer");
		}

		card.addLines(monster.combatSummaryLines());

		if (!compact)
		{
			if (monster.getSlayerMasters() != null && !monster.getSlayerMasters().isEmpty())
			{
				card.addLine("Assigned by: " + monster.getSlayerMasters());
			}
			if (monster.getExamine() != null && !monster.getExamine().isEmpty())
			{
				card.addLine("\"" + monster.getExamine() + "\"");
			}
		}

		return card;
	}
}
