/*
 * Copyright (c) 2023-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wimods.chestesp;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.annotation.ConfigEntry.Gui.EnumHandler.EnumDisplayOption;

@Config(name = "chestesp")
public final class ChestEspConfig implements ConfigData
{
	@ConfigEntry.Gui.Tooltip
	public boolean enable = true;
	
	@ConfigEntry.Gui.EnumHandler(option = EnumDisplayOption.BUTTON)
	@ConfigEntry.Gui.Tooltip
	public ChestEspStyle style = ChestEspStyle.BOXES;
	
	@ConfigEntry.Gui.Tooltip
	public boolean fill_boxes = true;
	
	@ConfigEntry.Gui.Tooltip
	public boolean outline_boxes = true;
	
	@ConfigEntry.BoundedDiscrete(min = 0, max = 100)
	@ConfigEntry.Gui.Tooltip
	public int fill_opacity = 25;
	
	@ConfigEntry.BoundedDiscrete(min = 0, max = 100)
	@ConfigEntry.Gui.Tooltip
	public int outline_opacity = 75;
	
	@ConfigEntry.Gui.Tooltip
	public boolean through_walls = true;
	
	@ConfigEntry.BoundedDiscrete(min = 16, max = 256)
	@ConfigEntry.Gui.Tooltip
	public int max_distance = 96;
	
	@ConfigEntry.Gui.Tooltip
	public boolean fade_by_distance = true;
	
	@ConfigEntry.Gui.Tooltip
	public boolean show_labels = true;
	
	@ConfigEntry.Gui.Tooltip
	public boolean show_distance = true;
	
	@ConfigEntry.Gui.Tooltip
	public boolean show_fill_level = true;
	
	@ConfigEntry.BoundedDiscrete(min = 8, max = 256)
	@ConfigEntry.Gui.Tooltip
	public int label_max_distance = 48;
	
	@ConfigEntry.BoundedDiscrete(min = 1, max = 64)
	@ConfigEntry.Gui.Tooltip
	public int max_labels = 24;
	
	@ConfigEntry.Gui.Tooltip
	public int min_y = -64;
	
	@ConfigEntry.Gui.Tooltip
	public int max_y = 320;
	
	@ConfigEntry.BoundedDiscrete(min = 0, max = 54)
	@ConfigEntry.Gui.Tooltip
	public int min_items = 0;
	
	@ConfigEntry.Gui.Tooltip
	public boolean valuable_only = false;
	
	@ConfigEntry.Gui.Tooltip
	public String item_filter = "";
	
	@ConfigEntry.BoundedDiscrete(min = 1, max = 40)
	@ConfigEntry.Gui.Tooltip
	public int scan_interval_ticks = 5;
	
	@ConfigEntry.Gui.Tooltip
	public boolean notify_new = false;
	
	@ConfigEntry.Gui.Tooltip
	public boolean hide_opened = false;
	
	@ConfigEntry.ColorPicker
	@ConfigEntry.Gui.Tooltip
	public int opened_color = 0x888888;
	
	@ConfigEntry.Gui.Tooltip
	public boolean highlight_nearest = true;
	
	@ConfigEntry.Gui.Tooltip
	public boolean tracers_nearest_only = false;
	
	@ConfigEntry.Gui.Tooltip
	public boolean chroma = false;
	
	@ConfigEntry.BoundedDiscrete(min = 1, max = 20)
	@ConfigEntry.Gui.Tooltip
	public int chroma_speed = 4;
	
	@ConfigEntry.Gui.Tooltip
	public boolean show_hud = true;
	
	@ConfigEntry.Gui.Tooltip
	public boolean hide_when_gui_open = true;
	
	public boolean include_basic_chests = true;
	
	@ConfigEntry.ColorPicker
	@ConfigEntry.Gui.Tooltip
	public int chest_color = 0x00FF00;
	
	public boolean include_trap_chests = true;
	
	@ConfigEntry.ColorPicker
	@ConfigEntry.Gui.Tooltip
	public int trap_chest_color = 0xFF8000;
	
	public boolean include_ender_chests = true;
	
	@ConfigEntry.ColorPicker
	@ConfigEntry.Gui.Tooltip
	public int ender_chest_color = 0x00FFFF;
	
	public boolean include_chest_carts = true;
	
	@ConfigEntry.ColorPicker
	@ConfigEntry.Gui.Tooltip
	public int chest_cart_color = 0xFFFF00;
	
	public boolean include_chest_boats = true;
	
	@ConfigEntry.ColorPicker
	@ConfigEntry.Gui.Tooltip
	public int chest_boat_color = 0xFFFF00;
	
	public boolean include_barrels = true;
	
	@ConfigEntry.ColorPicker
	@ConfigEntry.Gui.Tooltip
	public int barrel_color = 0x00FF00;
	
	public boolean include_pots = false;
	
	@ConfigEntry.ColorPicker
	@ConfigEntry.Gui.Tooltip
	public int pot_color = 0x00FF00;
	
	public boolean include_shulker_boxes = true;
	
	@ConfigEntry.ColorPicker
	@ConfigEntry.Gui.Tooltip
	public int shulker_box_color = 0xFF00FF;
	
	public boolean include_hoppers = false;
	
	@ConfigEntry.ColorPicker
	@ConfigEntry.Gui.Tooltip
	public int hopper_color = 0xFFFFFF;
	
	public boolean include_hopper_carts = false;
	
	@ConfigEntry.ColorPicker
	@ConfigEntry.Gui.Tooltip
	public int hopper_cart_color = 0xFFFF00;
	
	public boolean include_droppers = false;
	
	@ConfigEntry.ColorPicker
	@ConfigEntry.Gui.Tooltip
	public int dropper_color = 0xFFFFFF;
	
	public boolean include_dispensers = false;
	
	@ConfigEntry.ColorPicker
	@ConfigEntry.Gui.Tooltip
	public int dispenser_color = 0xFF8000;
	
	public boolean include_crafters = false;
	
	@ConfigEntry.ColorPicker
	@ConfigEntry.Gui.Tooltip
	public int crafter_color = 0xFFFFFF;
	
	public boolean include_furnaces = false;
	
	@ConfigEntry.ColorPicker
	@ConfigEntry.Gui.Tooltip
	public int furnace_color = 0xFF0000;
	
	public boolean include_brewing_stands = false;
	
	@ConfigEntry.ColorPicker
	@ConfigEntry.Gui.Tooltip
	public int brewing_stand_color = 0xFF55FF;
	
	public boolean include_lecterns = false;
	
	@ConfigEntry.ColorPicker
	@ConfigEntry.Gui.Tooltip
	public int lectern_color = 0xAA7733;
	
	public boolean include_bookshelves = false;
	
	@ConfigEntry.ColorPicker
	@ConfigEntry.Gui.Tooltip
	public int bookshelf_color = 0xCCAA66;
	
	public boolean include_jukeboxes = false;
	
	@ConfigEntry.ColorPicker
	@ConfigEntry.Gui.Tooltip
	public int jukebox_color = 0x55AAFF;
	
	public boolean include_shelves = false;
	
	@ConfigEntry.ColorPicker
	@ConfigEntry.Gui.Tooltip
	public int shelf_color = 0xC8A06A;
	
	public boolean include_vaults = false;
	
	@ConfigEntry.ColorPicker
	@ConfigEntry.Gui.Tooltip
	public int vault_color = 0xFFD700;
	
	public boolean include_suspicious = false;
	
	@ConfigEntry.ColorPicker
	@ConfigEntry.Gui.Tooltip
	public int suspicious_color = 0xE8D48A;
	
	@ConfigEntry.Gui.Tooltip
	public boolean plausible = true;
	
	@Override
	public void validatePostLoad()
	{
		if(min_y > max_y)
		{
			int swap = min_y;
			min_y = max_y;
			max_y = swap;
		}
	}
}
