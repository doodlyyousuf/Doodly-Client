package net.doodly.core;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Wurst-style arraylist — active modules, top-right corner.
 */
public final class ModuleArrayList
{
	private static final Minecraft MC = Minecraft.getInstance();
	
	private record Line(String name, int width) {}
	
	public void render(GuiGraphics g, CoreConfig cfg)
	{
		if(cfg == null || !cfg.arrayList || MC.options.hideGui)
			return;
		if(MC.screen instanceof DoodlyClickGui)
			return;
		
		List<Line> lines = new ArrayList<>();
		add(lines, "PlayerESP", "doodly_player_awareness",
			"net.doodly.playeresp.PlayerEspConfig", "enabled");
		add(lines, "BaseFinder", "doodly_base_scan",
			"net.doodly.basefinder.BaseFinderConfig", "enabled");
		add(lines, "Spawners", "doodly_base_scan",
			"net.doodly.basefinder.BaseFinderConfig", "spawners");
		add(lines, "StorageESP", "doodly_base_scan",
			"net.doodly.basefinder.BaseFinderConfig", "storageClusters");
		add(lines, "LightESP", "doodly_base_scan",
			"net.doodly.basefinder.BaseFinderConfig", "lightScan");
		add(lines, "ChestESP", "doodly_utils", null, null);
		if(!any(lines, "ChestESP"))
			add(lines, "ChestESP", "chestesp", null, null);
		add(lines, "FreeCam", "wi_freecam", null, null);
		
		if(lines.isEmpty()) return;
		
		lines.sort(Comparator.comparingInt((Line l) -> -l.width));
		
		Font font = MC.font;
		int sw = MC.getWindow().getGuiScaledWidth();
		int color = cfg.arrayListColor | 0xFF000000;
		int y = 2;
		
		for(Line line : lines)
		{
			int x = cfg.arrayListRight ? sw - line.width - 4 : 2;
			// shadow style like Wurst
			g.drawString(font, line.name, x + 1, y + 1, 0xFF000000, false);
			g.drawString(font, line.name, x, y, color, false);
			y += 10;
		}
	}
	
	private static boolean any(List<Line> lines, String name)
	{
		for(Line l : lines)
			if(l.name.equals(name)) return true;
		return false;
	}
	
	private void add(List<Line> lines, String name, String modId,
		String configClass, String field)
	{
		if(!FabricLoader.getInstance().isModLoaded(modId))
			return;
		boolean on = true;
		if(configClass != null && field != null)
		{
			try
			{
				Class<?> c = Class.forName(configClass);
				Object cfg = AutoConfig.getConfigHolder((Class)c).getConfig();
				Field f = c.getField(field);
				on = f.getBoolean(cfg);
			}
			catch(Throwable t)
			{
				on = true; // mod loaded, assume on if can't read
			}
		}
		if(!on) return;
		int w = MC.font.width(name);
		lines.add(new Line(name, w));
	}
}
