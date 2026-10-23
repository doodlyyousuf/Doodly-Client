package net.doodly.basefinder;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "basefinder")
public class BaseFinderConfig implements ConfigData
{
	public boolean enabled = true;
	
	// storage clusters (loaded chunks only)
	public boolean storageClusters = true;
	@ConfigEntry.BoundedDiscrete(min = 4, max = 48)
	public int clusterRadius = 12;
	@ConfigEntry.BoundedDiscrete(min = 2, max = 20)
	public int minContainers = 4;
	@ConfigEntry.ColorPicker
	public int clusterColor = 0x55AAFF;
	
	// artificial light = possible base (loaded chunks only)
	public boolean lightScan = true;
	@ConfigEntry.BoundedDiscrete(min = 1, max = 15)
	public int minBlockLight = 8;
	public boolean lightOnlyIfBrighterThanSky = true;
	public boolean lightUndergroundOnly = true;
	@ConfigEntry.BoundedDiscrete(min = -64, max = 128)
	public int lightMaxY = 60;
	@ConfigEntry.BoundedDiscrete(min = 16, max = 128)
	public int lightScanRange = 48;
	@ConfigEntry.BoundedDiscrete(min = 2, max = 16)
	public int lightStep = 4;
	@ConfigEntry.ColorPicker
	public int lightColor = 0xFFFF55;
	
	// sound side-channel (dig / spawner-ish)
	public boolean soundMarkers = true;
	@ConfigEntry.ColorPicker
	public int soundColor = 0xFF8800;
	
	public boolean tracers = true;
	public boolean throughWalls = true;
	public boolean hud = true;
	
	@ConfigEntry.BoundedDiscrete(min = 32, max = 256)
	public int maxDistance = 160;
}
