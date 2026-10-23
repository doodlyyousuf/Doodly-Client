package net.doodly.basefinder;

import com.mojang.blaze3d.platform.InputConstants;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public final class BaseFinderClient implements ClientModInitializer
{
	private static BaseFinderClient INSTANCE;
	private ConfigHolder<BaseFinderConfig> configHolder;
	private final Scanner scanner = new Scanner();
	private final Renderer renderer = new Renderer();
	private KeyMapping toggleKey;
	private int tickCounter;
	
	@Override
	public void onInitializeClient()
	{
		INSTANCE = this;
		configHolder = AutoConfig.register(BaseFinderConfig.class, GsonConfigSerializer::new);
		
		KeyMapping.Category cat = KeyMapping.Category.register(
			Identifier.fromNamespaceAndPath("basefinder", "main"));
		toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.basefinder.toggle", InputConstants.Type.KEYSYM,
			InputConstants.UNKNOWN.getValue(), cat));
		
		ClientTickEvents.END_CLIENT_TICK.register(c -> {
			BaseFinderConfig cfg = configHolder.getConfig();
			while(toggleKey.consumeClick())
			{
				cfg.enabled = !cfg.enabled;
				configHolder.save();
			}
			if(!cfg.enabled) return;
			tickCounter++;
			// light + storage scan every 20 ticks to keep cost low
			if(tickCounter % 20 == 0)
				scanner.scan(cfg);
		});
		
		ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((c, w) -> scanner.clear());
		
		HudElementRegistry.addLast(
			Identifier.fromNamespaceAndPath("basefinder", "hud"),
			(g, d) -> {
				BaseFinderConfig cfg = configHolder.getConfig();
				if(cfg.enabled && cfg.hud)
					renderer.drawHud(g, scanner, cfg);
			});
	}
	
	public static BaseFinderClient get() { return INSTANCE; }
	public Scanner getScanner() { return scanner; }
	public BaseFinderConfig getConfig() { return configHolder.getConfig(); }
	
	public void onWorldRender(com.mojang.blaze3d.vertex.PoseStack matrices, float pt)
	{
		BaseFinderConfig cfg = configHolder.getConfig();
		if(cfg.enabled)
			renderer.drawWorld(matrices, pt, scanner, cfg);
	}
}
