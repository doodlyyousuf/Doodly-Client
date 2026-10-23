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
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.wimods.chestesp.ChestEspBlockGroup;
import net.wimods.chestesp.ChestEspConfig;

public final class LecternsGroup extends ChestEspBlockGroup
{
	public LecternsGroup(ConfigHolder<ChestEspConfig> ch)
	{
		super(ch, "lectern");
	}
	
	@Override
	protected boolean isEnabled(ChestEspConfig c)
	{
		return c.include_lecterns;
	}
	
	@Override
	protected int getColor(ChestEspConfig c)
	{
		return c.lectern_color;
	}
	
	@Override
	public boolean matches(BlockEntity be)
	{
		return be instanceof LecternBlockEntity;
	}
}
