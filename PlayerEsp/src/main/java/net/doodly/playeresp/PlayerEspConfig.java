package net.doodly.playeresp;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "playeresp")
public class PlayerEspConfig implements ConfigData
{
	@ConfigEntry.Category("general")
	@ConfigEntry.Gui.Tooltip
	public boolean enabled = true;

	@ConfigEntry.Category("general")
	@ConfigEntry.Gui.Tooltip
	public boolean boxes = true;

	@ConfigEntry.Category("general")
	@ConfigEntry.Gui.Tooltip
	public boolean fill = false;

	@ConfigEntry.Category("general")
	@ConfigEntry.Gui.Tooltip
	public boolean tracers = true;

	@ConfigEntry.Category("general")
	@ConfigEntry.Gui.Tooltip
	public boolean tracersNearestOnly = false;

	@ConfigEntry.Category("general")
	@ConfigEntry.Gui.Tooltip
	public boolean labels = true;

	@ConfigEntry.Category("general")
	@ConfigEntry.Gui.Tooltip
	public boolean throughWalls = true;

	@ConfigEntry.Category("general")
	@ConfigEntry.Gui.Tooltip
	public boolean undergroundOnly = false;

	@ConfigEntry.Category("general")
	@ConfigEntry.Gui.Tooltip
	public boolean ignoreInvisible = false;

	@ConfigEntry.Category("general")
	@ConfigEntry.Gui.Tooltip
	public boolean smooth = true;

	@ConfigEntry.Category("general")
	@ConfigEntry.Gui.Tooltip
	public boolean pulseNearest = true;

	@ConfigEntry.Category("general")
	@ConfigEntry.Gui.Tooltip
	public boolean hud = true;

	@ConfigEntry.Category("labels")
	@ConfigEntry.Gui.Tooltip
	public boolean showDistance = true;

	@ConfigEntry.Category("labels")
	@ConfigEntry.Gui.Tooltip
	public boolean showHealth = true;

	@ConfigEntry.Category("labels")
	@ConfigEntry.Gui.Tooltip
	public boolean showArmor = true;

	@ConfigEntry.Category("labels")
	@ConfigEntry.Gui.Tooltip
	public boolean showHeld = true;

	@ConfigEntry.Category("labels")
	@ConfigEntry.Gui.Tooltip
	public boolean showSpeed = false;

	@ConfigEntry.Category("labels")
	@ConfigEntry.BoundedDiscrete(min = 8, max = 128)
	@ConfigEntry.Gui.Tooltip
	public int labelDistance = 64;

	@ConfigEntry.Category("labels")
	@ConfigEntry.BoundedDiscrete(min = 1, max = 48)
	@ConfigEntry.Gui.Tooltip
	public int maxLabels = 36;

	@ConfigEntry.Category("radar")
	@ConfigEntry.Gui.Tooltip
	public boolean radar = true;

	@ConfigEntry.Category("radar")
	@ConfigEntry.BoundedDiscrete(min = 48, max = 160)
	@ConfigEntry.Gui.Tooltip
	public int radarSize = 96;

	@ConfigEntry.Category("radar")
	@ConfigEntry.Gui.Tooltip
	public boolean predict = true;

	@ConfigEntry.Category("radar")
	@ConfigEntry.Gui.Tooltip
	public boolean soundPhantoms = true;

	@ConfigEntry.Category("range")
	@ConfigEntry.BoundedDiscrete(min = 16, max = 256)
	@ConfigEntry.Gui.Tooltip
	public int maxDistance = 128;

	@ConfigEntry.Category("range")
	@ConfigEntry.Gui.Tooltip
	public int undergroundY = 60;

	@ConfigEntry.Category("range")
	@ConfigEntry.BoundedDiscrete(min = 5, max = 120)
	@ConfigEntry.Gui.Tooltip
	public int staleSeconds = 20;

	@ConfigEntry.Category("colors")
	@ConfigEntry.ColorPicker
	@ConfigEntry.Gui.Tooltip
	public int surfaceColor = 0x33FF33;

	@ConfigEntry.Category("colors")
	@ConfigEntry.ColorPicker
	@ConfigEntry.Gui.Tooltip
	public int undergroundColor = 0xFF3333;

	@ConfigEntry.Category("colors")
	@ConfigEntry.ColorPicker
	@ConfigEntry.Gui.Tooltip
	public int soundColor = 0xFFAA00;

	@ConfigEntry.Category("colors")
	@ConfigEntry.ColorPicker
	@ConfigEntry.Gui.Tooltip
	public int friendColor = 0x55DDFF;

	@ConfigEntry.Category("filters")
	@ConfigEntry.Gui.Tooltip
	public String hideNames = "";

	@ConfigEntry.Category("filters")
	@ConfigEntry.Gui.Tooltip
	public String focusNames = "";

	@ConfigEntry.Category("filters")
	@ConfigEntry.Gui.Tooltip
	public String friendNames = "";
}
