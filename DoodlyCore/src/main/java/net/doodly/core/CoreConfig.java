package net.doodly.core;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "doodlycore")
public class CoreConfig implements ConfigData
{
	@ConfigEntry.Gui.Tooltip
	public boolean arrayList = true;
	
	@ConfigEntry.Gui.Tooltip
	public boolean arrayListRight = true;
	
	@ConfigEntry.ColorPicker
	@ConfigEntry.Gui.Tooltip
	public int arrayListColor = 0x00FF00;
}
