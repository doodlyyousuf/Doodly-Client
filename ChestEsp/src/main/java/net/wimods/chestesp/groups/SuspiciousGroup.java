/*
 * Copyright (c) 2023-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wimods.chestesp.groups;

import me.shedaniel.autoconfig.ConfigHolder;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.wimods.chestesp.ChestEspBlockGroup;
import net.wimods.chestesp.ChestEspConfig;

public final class SuspiciousGroup extends ChestEspBlockGroup
{
	public SuspiciousGroup(ConfigHolder<ChestEspConfig> ch)
	{
		super(ch, "suspicious");
	}
	
	@Override
	protected boolean isEnabled(ChestEspConfig c)
	{
		return c.include_suspicious;
	}
	
	@Override
	protected int getColor(ChestEspConfig c)
	{
		return c.suspicious_color;
	}
	
	@Override
	public boolean matches(BlockEntity be)
	{
		return be instanceof BrushableBlockEntity;
	}
}
