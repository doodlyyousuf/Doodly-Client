/*
 * Copyright (c) 2023-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wimods.chestesp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import me.shedaniel.autoconfig.ConfigHolder;
import net.minecraft.world.phys.AABB;

public abstract class ChestEspGroup
{
	private final ConfigHolder<ChestEspConfig> configHolder;
	private final String name;
	protected final ArrayList<AABB> boxes = new ArrayList<>();
	protected final ArrayList<ChestEspTarget> targets = new ArrayList<>();
	
	public ChestEspGroup(ConfigHolder<ChestEspConfig> configHolder, String name)
	{
		this.configHolder = Objects.requireNonNull(configHolder);
		this.name = Objects.requireNonNull(name);
	}
	
	protected abstract boolean isEnabled(ChestEspConfig c);
	
	protected abstract int getColor(ChestEspConfig c);
	
	protected final ChestEspConfig config()
	{
		return configHolder.getConfig();
	}
	
	public void clear()
	{
		boxes.clear();
		targets.clear();
	}
	
	public final String getName()
	{
		return name;
	}
	
	public final String getDisplayName()
	{
		return switch(name)
		{
			case "chest" -> "Chest";
			case "trap_chest" -> "Trapped Chest";
			case "ender_chest" -> "Ender Chest";
			case "chest_cart" -> "Chest Minecart";
			case "chest_boat" -> "Chest Boat";
			case "barrel" -> "Barrel";
			case "pot" -> "Pot";
			case "shulker_box" -> "Shulker";
			case "hopper" -> "Hopper";
			case "hopper_cart" -> "Hopper Minecart";
			case "dropper" -> "Dropper";
			case "dispenser" -> "Dispenser";
			case "crafter" -> "Crafter";
			case "furnace" -> "Furnace";
			case "brewing_stand" -> "Brewing Stand";
			case "lectern" -> "Lectern";
			case "bookshelf" -> "Bookshelf";
			case "jukebox" -> "Jukebox";
			case "shelf" -> "Shelf";
			case "vault" -> "Vault";
			case "suspicious" -> "Suspicious";
			default -> name;
		};
	}
	
	public final boolean isEnabled()
	{
		return isEnabled(configHolder.getConfig());
	}
	
	public final int getColorI(int alpha)
	{
		int rgb = getColor(configHolder.getConfig());
		return alpha << 24 | rgb;
	}
	
	public final String getColorHex()
	{
		int rgb = getColor(configHolder.getConfig());
		return String.format("#%06X", rgb);
	}
	
	public final List<AABB> getBoxes()
	{
		return Collections.unmodifiableList(boxes);
	}
	
	public final List<ChestEspTarget> getTargets()
	{
		return Collections.unmodifiableList(targets);
	}
}
