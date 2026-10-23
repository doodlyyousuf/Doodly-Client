package net.doodly.core;

import java.util.ArrayList;
import java.util.List;

import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Right-Shift hub. Lists installed Doodly modules and opens their Cloth config.
 * 1.21.11: do NOT call renderBackground() and super.render() both —
 * that double-blurs and throws "Can only blur once per frame".
 */
public final class DoodlyClickGui extends Screen
{
	private final List<ModuleRow> rows = new ArrayList<>();
	
	private static final class ModuleRow
	{
		final String title;
		final String modId;
		final Class<?> configClass;
		final boolean loaded;
		
		ModuleRow(String title, String modId, Class<?> configClass, boolean loaded)
		{
			this.title = title;
			this.modId = modId;
			this.configClass = configClass;
			this.loaded = loaded;
		}
	}
	
	public DoodlyClickGui()
	{
		super(Component.literal("Doodly Client"));
	}
	
	@Override
	protected void init()
	{
		rows.clear();
		
		rows.add(row("Player Awareness", "doodly_player_awareness",
			"net.doodly.playeresp.PlayerEspConfig"));
		ModuleRow chest = row("Chest / Storage ESP", "doodly_utils",
			"net.wimods.chestesp.ChestEspConfig");
		if(!chest.loaded)
			chest = row("Chest / Storage ESP", "chestesp",
				"net.wimods.chestesp.ChestEspConfig");
		rows.add(chest);
		rows.add(row("Base Scan (light + storage)", "doodly_base_scan",
			"net.doodly.basefinder.BaseFinderConfig"));
		rows.add(row("FreeCam", "wi_freecam", null));
		
		int y = 48;
		for(ModuleRow r : rows)
		{
			String label = r.title + (r.loaded ? "  [loaded]" : "  [not installed]");
			ModuleRow ref = r;
			addRenderableWidget(Button.builder(Component.literal(label), b -> openConfig(ref))
				.bounds(this.width / 2 - 120, y, 240, 20)
				.build());
			y += 24;
		}
		
		addRenderableWidget(Button.builder(Component.literal("Close"), b -> onClose())
			.bounds(this.width / 2 - 40, this.height - 28, 80, 20)
			.build());
	}
	
	private ModuleRow row(String title, String modId, String configClassName)
	{
		boolean loaded = FabricLoader.getInstance().isModLoaded(modId);
		Class<?> cfg = null;
		if(loaded && configClassName != null)
		{
			try { cfg = Class.forName(configClassName); }
			catch(ClassNotFoundException ignored) {}
		}
		return new ModuleRow(title, modId, cfg, loaded);
	}
	
	private void openConfig(ModuleRow r)
	{
		if(!r.loaded || r.configClass == null || minecraft == null)
			return;
		try
		{
			@SuppressWarnings("unchecked")
			Class<? extends me.shedaniel.autoconfig.ConfigData> c =
				(Class<? extends me.shedaniel.autoconfig.ConfigData>) r.configClass;
			minecraft.setScreen(AutoConfig.getConfigScreen(c, this).get());
		}
		catch(Throwable ignored) {}
	}
	
	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta)
	{
		// single background path only — super.render already draws the blur once
		super.render(g, mouseX, mouseY, delta);
		
		g.drawCenteredString(this.font, "Doodly Client — modules",
			this.width / 2, 12, 0xFFFFFF);
		g.drawCenteredString(this.font, "Right Shift toggles this menu",
			this.width / 2, 26, 0xAAAAAA);
	}
	
	@Override
	public boolean isPauseScreen()
	{
		return false;
	}
}
