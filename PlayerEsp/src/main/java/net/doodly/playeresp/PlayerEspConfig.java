package net.doodly.playeresp;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "playeresp")
public class PlayerEspConfig implements ConfigData
{
	public boolean enabled = true;
	public boolean boxes = true;
	public boolean fill = false;
	public boolean tracers = true;
	public boolean tracersNearestOnly = false;
	public boolean labels = true;
	public boolean throughWalls = true;
	public boolean undergroundOnly = false;
	public boolean ignoreInvisible = false;
	public boolean smooth = true;
	public boolean pulseNearest = true;
	public boolean showDistance = true;
	public boolean showHealth = true;
	public boolean showArmor = true;
	public boolean showHeld = true;
	public boolean showSpeed = false;
	public boolean predict = true;
	public boolean radar = true;
	public boolean hud = true;
	
	@ConfigEntry.BoundedDiscrete(min = 16, max = 256)
	public int maxDistance = 128;
	
	@ConfigEntry.BoundedDiscrete(min = 8, max = 128)
	public int labelDistance = 64;
	
	@ConfigEntry.BoundedDiscrete(min = 1, max = 32)
	public int maxLabels = 12;
	
	public int undergroundY = 60;
	
	@ConfigEntry.BoundedDiscrete(min = 5, max = 120)
	public int staleSeconds = 20;
	
	@ConfigEntry.BoundedDiscrete(min = 48, max = 160)
	public int radarSize = 96;
	
	@ConfigEntry.ColorPicker
	public int surfaceColor = 0x33FF33;
	
	@ConfigEntry.ColorPicker
	public int undergroundColor = 0xFF3333;
	
	@ConfigEntry.ColorPicker
	public int soundColor = 0xFFAA00;
	
	@ConfigEntry.ColorPicker
	public int friendColor = 0x55DDFF;
	
	/** Comma-separated */
	public String hideNames = "";
	public String focusNames = "";
	public String friendNames = "";
}
