package net.doodly.basefinder;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "basefinder")
public class BaseFinderConfig implements ConfigData
{
	@ConfigEntry.Category("general")
	@ConfigEntry.Gui.Tooltip
	public boolean enabled = true;

	@ConfigEntry.Category("general")
	@ConfigEntry.Gui.Tooltip
	public boolean throughWalls = true;

	@ConfigEntry.Category("general")
	@ConfigEntry.Gui.Tooltip
	public boolean tracers = true;

	@ConfigEntry.Category("general")
	@ConfigEntry.Gui.Tooltip
	public boolean hud = true;

	@ConfigEntry.Category("general")
	@ConfigEntry.BoundedDiscrete(min = 32, max = 256)
	@ConfigEntry.Gui.Tooltip
	public int maxDistance = 160;

	@ConfigEntry.Category("storage")
	@ConfigEntry.Gui.Tooltip
	public boolean storageClusters = true;

	@ConfigEntry.Category("storage")
	@ConfigEntry.Gui.Tooltip
	public boolean spawners = true;

	@ConfigEntry.Category("storage")
	@ConfigEntry.ColorPicker
	@ConfigEntry.Gui.Tooltip
	public int spawnerColor = 0xFF2222;

	@ConfigEntry.Category("storage")
	@ConfigEntry.BoundedDiscrete(min = 4, max = 48)
	@ConfigEntry.Gui.Tooltip
	public int clusterRadius = 12;

	@ConfigEntry.Category("storage")
	@ConfigEntry.BoundedDiscrete(min = 2, max = 20)
	@ConfigEntry.Gui.Tooltip
	public int minContainers = 4;

	@ConfigEntry.Category("storage")
	@ConfigEntry.ColorPicker
	@ConfigEntry.Gui.Tooltip
	public int clusterColor = 0x55AAFF;

	@ConfigEntry.Category("storage")
	@ConfigEntry.Gui.Tooltip
	public boolean stashLog = true;

	@ConfigEntry.Category("storage")
	@ConfigEntry.BoundedDiscrete(min = 5, max = 64)
	@ConfigEntry.Gui.Tooltip
	public int stashMax = 24;

	@ConfigEntry.Category("light")
	@ConfigEntry.Gui.Tooltip
	public boolean lightScan = true;

	@ConfigEntry.Category("light")
	@ConfigEntry.BoundedDiscrete(min = 1, max = 15)
	@ConfigEntry.Gui.Tooltip
	public int minBlockLight = 8;

	@ConfigEntry.Category("light")
	@ConfigEntry.Gui.Tooltip
	public boolean lightOnlyIfBrighterThanSky = true;

	@ConfigEntry.Category("light")
	@ConfigEntry.Gui.Tooltip
	public boolean lightUndergroundOnly = true;

	@ConfigEntry.Category("light")
	@ConfigEntry.BoundedDiscrete(min = -64, max = 128)
	@ConfigEntry.Gui.Tooltip
	public int lightMaxY = 60;

	@ConfigEntry.Category("light")
	@ConfigEntry.BoundedDiscrete(min = 16, max = 128)
	@ConfigEntry.Gui.Tooltip
	public int lightScanRange = 48;

	@ConfigEntry.Category("light")
	@ConfigEntry.BoundedDiscrete(min = 2, max = 16)
	@ConfigEntry.Gui.Tooltip
	public int lightStep = 4;

	@ConfigEntry.Category("light")
	@ConfigEntry.ColorPicker
	@ConfigEntry.Gui.Tooltip
	public int lightColor = 0xFFFF55;

	@ConfigEntry.Category("sound")
	@ConfigEntry.Gui.Tooltip
	public boolean soundMarkers = true;

	@ConfigEntry.Category("sound")
	@ConfigEntry.ColorPicker
	@ConfigEntry.Gui.Tooltip
	public int soundColor = 0xFF8800;
}
