/*
 * Copyright (c) 2023-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wimods.chestesp.util;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.wimods.chestesp.ChestEspConfig;

public enum ContainerInspector
{
	;
	
	private static final String DEFAULT_FILTER =
		"diamond,diamond_block,netherite_ingot,netherite_block,netherite_scrap,"
			+ "totem_of_undying,enchanted_golden_apple,elytra,nether_star,"
			+ "beacon,shulker_box,dragon_egg,mace";
	
	public static final class Snapshot
	{
		public final boolean known;
		public final int filledSlots;
		public final int totalSlots;
		public final boolean matchesFilter;
		
		private Snapshot(boolean known, int filledSlots, int totalSlots,
			boolean matchesFilter)
		{
			this.known = known;
			this.filledSlots = filledSlots;
			this.totalSlots = totalSlots;
			this.matchesFilter = matchesFilter;
		}
		
		public static Snapshot unknown()
		{
			return new Snapshot(false, -1, -1, false);
		}
	}
	
	public static Snapshot inspect(Object maybeContainer, ChestEspConfig config)
	{
		if(!(maybeContainer instanceof Container container))
			return Snapshot.unknown();
		
		int total = container.getContainerSize();
		if(total <= 0)
			return Snapshot.unknown();
		
		Set<String> filter = parseFilter(config.item_filter);
		int filled = 0;
		boolean matches = false;
		boolean anyItem = false;
		
		for(int i = 0; i < total; i++)
		{
			ItemStack stack = container.getItem(i);
			if(stack.isEmpty())
				continue;
			
			anyItem = true;
			filled++;
			if(!matches && matchesStack(stack, filter))
				matches = true;
		}
		
		// Vanilla clients usually have empty chest inventories until opened.
		if(!anyItem)
			return Snapshot.unknown();
		
		return new Snapshot(true, filled, total, matches);
	}
	
	private static boolean matchesStack(ItemStack stack, Set<String> filter)
	{
		String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
		String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
		if(filter.contains(id) || filter.contains(path))
			return true;
		if(path.contains("shulker_box") && filter.contains("shulker_box"))
			return true;
		
		ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
		if(contents == null)
			return false;
		
		for(ItemStack inner : contents.nonEmptyItems())
		{
			String innerPath =
				BuiltInRegistries.ITEM.getKey(inner.getItem()).getPath();
			String innerId =
				BuiltInRegistries.ITEM.getKey(inner.getItem()).toString();
			if(filter.contains(innerId) || filter.contains(innerPath))
				return true;
		}
		return false;
	}
	
	private static Set<String> parseFilter(String raw)
	{
		String source = raw == null || raw.isBlank() ? DEFAULT_FILTER : raw;
		Set<String> ids = new HashSet<>();
		for(String part : source.split(","))
		{
			String id = part.trim().toLowerCase(Locale.ROOT);
			if(!id.isEmpty())
				ids.add(id);
		}
		return ids;
	}
}
