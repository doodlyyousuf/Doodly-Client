package net.doodly.core;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Wurst-inspired click GUI: category windows, toggle rows, no blur.
 * Settings open Cloth config for deep options.
 */
public final class DoodlyClickGui extends Screen
{
	private static final int BG = 0xE0101018;
	private static final int TITLE_BG = 0xFF1A1A28;
	private static final int ROW = 0xFF14141C;
	private static final int ROW_HOVER = 0xFF222233;
	private static final int ACCENT = 0xFF00FF00;
	private static final int ACCENT_DIM = 0xFF2A5A2A;
	private static final int TEXT = 0xFFE0E0E0;
	private static final int TEXT_OFF = 0xFF888888;
	private static final int BORDER = 0xFF333344;
	
	private final List<Window> windows = new ArrayList<>();
	private Window dragging;
	private double dragOx, dragOy;
	
	private static final class ModEntry
	{
		final String name;
		final String modId;
		final String configClass;
		final String enabledField; // boolean field on config, or null = mod loaded only
		
		ModEntry(String name, String modId, String configClass, String enabledField)
		{
			this.name = name;
			this.modId = modId;
			this.configClass = configClass;
			this.enabledField = enabledField;
		}
		
		boolean loaded()
		{
			return FabricLoader.getInstance().isModLoaded(modId)
				|| (modId.equals("doodly_utils")
					&& FabricLoader.getInstance().isModLoaded("chestesp"));
		}
		
		boolean enabled()
		{
			if(!loaded()) return false;
			if(enabledField == null || configClass == null) return loaded();
			try
			{
				Class<?> c = Class.forName(configClass);
				Object cfg = AutoConfig.getConfigHolder((Class)c).getConfig();
				Field f = c.getField(enabledField);
				return f.getBoolean(cfg);
			}
			catch(Throwable t)
			{
				return loaded();
			}
		}
		
		void toggle()
		{
			if(!loaded() || enabledField == null || configClass == null) return;
			try
			{
				Class<?> c = Class.forName(configClass);
				var holder = AutoConfig.getConfigHolder((Class)c);
				Object cfg = holder.getConfig();
				Field f = c.getField(enabledField);
				f.setBoolean(cfg, !f.getBoolean(cfg));
				holder.save();
			}
			catch(Throwable ignored) {}
		}
		
		void openSettings(Screen parent)
		{
			if(!loaded() || configClass == null) return;
			Minecraft mc = Minecraft.getInstance();
			if(mc == null) return;
			try
			{
				Class<?> c = Class.forName(configClass);
				mc.setScreen(AutoConfig.getConfigScreen((Class)c, parent).get());
			}
			catch(Throwable ignored) {}
		}
	}
	
	private static final class Window
	{
		String title;
		int x, y, w;
		boolean collapsed;
		final List<ModEntry> entries = new ArrayList<>();
		
		Window(String title, int x, int y)
		{
			this.title = title;
			this.x = x;
			this.y = y;
			this.w = 140;
		}
		
		int height()
		{
			return collapsed ? 16 : 16 + entries.size() * 14;
		}
	}
	
	public DoodlyClickGui()
	{
		super(Component.literal("Doodly"));
	}
	
	@Override
	protected void init()
	{
		windows.clear();
		dragging = null;
		
		Window render = new Window("Render", 40, 40);
		render.entries.add(new ModEntry("PlayerESP", "doodly_player_awareness",
			"net.doodly.playeresp.PlayerEspConfig", "enabled"));
		render.entries.add(new ModEntry("ChestESP", "doodly_utils",
			"net.wimods.chestesp.ChestEspConfig", null));
		render.entries.add(new ModEntry("BaseFinder", "doodly_base_scan",
			"net.doodly.basefinder.BaseFinderConfig", "enabled"));
		render.entries.add(new ModEntry("Spawners", "doodly_base_scan",
			"net.doodly.basefinder.BaseFinderConfig", "spawners"));
		render.entries.add(new ModEntry("Storage", "doodly_base_scan",
			"net.doodly.basefinder.BaseFinderConfig", "storageClusters"));
		render.entries.add(new ModEntry("LightESP", "doodly_base_scan",
			"net.doodly.basefinder.BaseFinderConfig", "lightScan"));
		windows.add(render);
		
		Window player = new Window("Player", 200, 40);
		player.entries.add(new ModEntry("Boxes", "doodly_player_awareness",
			"net.doodly.playeresp.PlayerEspConfig", "boxes"));
		player.entries.add(new ModEntry("Tracers", "doodly_player_awareness",
			"net.doodly.playeresp.PlayerEspConfig", "tracers"));
		player.entries.add(new ModEntry("Labels", "doodly_player_awareness",
			"net.doodly.playeresp.PlayerEspConfig", "labels"));
		player.entries.add(new ModEntry("Radar", "doodly_player_awareness",
			"net.doodly.playeresp.PlayerEspConfig", "radar"));
		player.entries.add(new ModEntry("SoundPhantoms", "doodly_player_awareness",
			"net.doodly.playeresp.PlayerEspConfig", "soundPhantoms"));
		windows.add(player);
		
		Window base = new Window("Base", 360, 40);
		base.entries.add(new ModEntry("StashLog", "doodly_base_scan",
			"net.doodly.basefinder.BaseFinderConfig", "stashLog"));
		base.entries.add(new ModEntry("Sounds", "doodly_base_scan",
			"net.doodly.basefinder.BaseFinderConfig", "soundMarkers"));
		base.entries.add(new ModEntry("Tracers", "doodly_base_scan",
			"net.doodly.basefinder.BaseFinderConfig", "tracers"));
		base.entries.add(new ModEntry("ThroughWalls", "doodly_base_scan",
			"net.doodly.basefinder.BaseFinderConfig", "throughWalls"));
		windows.add(base);
		
		Window misc = new Window("Misc", 520, 40);
		misc.entries.add(new ModEntry("FreeCam", "wi_freecam", null, null));
		misc.entries.add(new ModEntry("ArrayList", "doodly_core",
			"net.doodly.core.CoreConfig", "arrayList"));
		windows.add(misc);
	}
	
	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta)
	{
		// no blur
	}
	
	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta)
	{
		g.fill(0, 0, width, height, 0x90101018);
		
		g.drawString(font, "Doodly Client", 8, 8, ACCENT, false);
		g.drawString(font, "Right Shift close · Left click toggle · Right click settings",
			8, 20, TEXT_OFF, false);
		
		for(Window w : windows)
			drawWindow(g, w, mouseX, mouseY);
	}
	
	private void drawWindow(GuiGraphics g, Window w, int mx, int my)
	{
		int h = w.height();
		// border
		g.fill(w.x - 1, w.y - 1, w.x + w.w + 1, w.y + h + 1, BORDER);
		// title bar
		g.fill(w.x, w.y, w.x + w.w, w.y + 16, TITLE_BG);
		g.fill(w.x, w.y, w.x + 2, w.y + 16, ACCENT);
		g.drawString(font, w.title, w.x + 6, w.y + 4, TEXT, false);
		String arrow = w.collapsed ? "+" : "-";
		g.drawString(font, arrow, w.x + w.w - 10, w.y + 4, ACCENT, false);
		
		if(w.collapsed) return;
		
		int yy = w.y + 16;
		for(ModEntry e : w.entries)
		{
			boolean hover = mx >= w.x && mx < w.x + w.w && my >= yy && my < yy + 14;
			boolean on = e.enabled();
			boolean loaded = e.loaded();
			g.fill(w.x, yy, w.x + w.w, yy + 14, hover ? ROW_HOVER : ROW);
			// status strip
			g.fill(w.x, yy, w.x + 2, yy + 14, on ? ACCENT : ACCENT_DIM);
			int col = !loaded ? 0xFF555555 : (on ? ACCENT : TEXT_OFF);
			String label = e.name + (!loaded ? "  §8[missing]" : (on ? "" : ""));
			g.drawString(font, e.name, w.x + 6, yy + 3, col, false);
			if(on && loaded)
				g.drawString(font, "ON", w.x + w.w - 18, yy + 3, ACCENT, false);
			yy += 14;
		}
	}
	
	@Override
	public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event,
		boolean doubled)
	{
		double mx = event.x();
		double my = event.y();
		int btn = event.button();
		
		for(int i = windows.size() - 1; i >= 0; i--)
		{
			Window w = windows.get(i);
			if(mx < w.x || mx >= w.x + w.w || my < w.y || my >= w.y + w.height())
				continue;
			
			// title bar
			if(my < w.y + 16)
			{
				if(btn == 0)
				{
					if(mx > w.x + w.w - 14)
						w.collapsed = !w.collapsed;
					else
					{
						dragging = w;
						dragOx = mx - w.x;
						dragOy = my - w.y;
					}
				}
				return true;
			}
			
			if(w.collapsed) return true;
			
			int yy = w.y + 16;
			for(ModEntry e : w.entries)
			{
				if(my >= yy && my < yy + 14)
				{
					if(btn == 0)
						e.toggle();
					else if(btn == 1)
						e.openSettings(this);
					return true;
				}
				yy += 14;
			}
			return true;
		}
		return super.mouseClicked(event, doubled);
	}
	
	@Override
	public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event)
	{
		dragging = null;
		return super.mouseReleased(event);
	}
	
	@Override
	public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event,
		double dx, double dy)
	{
		if(dragging != null)
		{
			dragging.x = (int)Mth.clamp(event.x() - dragOx, 0, width - dragging.w);
			dragging.y = (int)Mth.clamp(event.y() - dragOy, 0, height - 16);
			return true;
		}
		return super.mouseDragged(event, dx, dy);
	}
	
	@Override
	public boolean isPauseScreen()
	{
		return false;
	}
}
