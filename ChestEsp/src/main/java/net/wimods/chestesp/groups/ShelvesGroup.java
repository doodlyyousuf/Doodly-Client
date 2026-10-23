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
import net.minecraft.world.level.block.entity.ShelfBlockEntity;
import net.wimods.chestesp.ChestEspBlockGroup;
import net.wimods.chestesp.ChestEspConfig;

public final class ShelvesGroup extends ChestEspBlockGroup
{
	public ShelvesGroup(ConfigHolder<ChestEspConfig> ch)
	{
		super(ch, "shelf");
	}
	
	@Override
	protected boolean isEnabled(ChestEspConfig c)
	{
		return c.include_shelves;
	}
	
	@Override
	protected int getColor(ChestEspConfig c)
	{
		return c.shelf_color;
	}
	
	@Override
	public boolean matches(BlockEntity be)
	{
		return be instanceof ShelfBlockEntity;
	}
}
