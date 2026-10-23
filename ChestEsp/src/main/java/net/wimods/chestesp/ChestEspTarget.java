/*
 * Copyright (c) 2023-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wimods.chestesp;

import java.awt.Color;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class ChestEspTarget
{
	public final AABB box;
	public final String label;
	public final long key;
	public final int filledSlots;
	public final int totalSlots;
	public final boolean knownInventory;
	public final boolean matchesItemFilter;
	
	public ChestEspTarget(AABB box, String label, long key, int filledSlots,
		int totalSlots, boolean knownInventory, boolean matchesItemFilter)
	{
		this.box = box;
		this.label = label;
		this.key = key;
		this.filledSlots = filledSlots;
		this.totalSlots = totalSlots;
		this.knownInventory = knownInventory;
		this.matchesItemFilter = matchesItemFilter;
	}
	
	public Vec3 center()
	{
		return box.getCenter();
	}
	
	public static int fadeAlpha(int baseAlpha, double dist, double maxDist,
		boolean fade)
	{
		if(!fade || maxDist <= 0)
			return baseAlpha;
		
		double t = 1.0 - dist / maxDist;
		if(t < 0)
			t = 0;
		if(t > 1)
			t = 1;
		return (int)Math.round(baseAlpha * (0.2 + 0.8 * t));
	}
	
	public String displayText(double dist, ChestEspConfig config)
	{
		StringBuilder sb = new StringBuilder(label);
		if(config.show_distance)
			sb.append(' ').append((int)dist).append('m');
		if(config.show_fill_level && knownInventory && totalSlots > 0)
			sb.append(' ').append(filledSlots).append('/').append(totalSlots);
		return sb.toString();
	}
	
	public boolean passes(ChestEspConfig config)
	{
		if(config.min_items > 0 && knownInventory && filledSlots < config.min_items)
			return false;
		if(config.valuable_only && knownInventory && !matchesItemFilter)
			return false;
		return true;
	}
	
	public static int chromaRgb(int rgb, ChestEspConfig config, double dist)
	{
		if(!config.chroma)
			return rgb;
		
		float hue = (float)((System.currentTimeMillis()
			/ (250.0 / Math.max(1, config.chroma_speed)) + dist * 8) % 360)
			/ 360F;
		return Color.HSBtoRGB(hue, 0.8F, 1F) & 0xFFFFFF;
	}
}
