package net.doodly.playeresp;

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

public final class PlayerEspClient implements ClientModInitializer
{
	private static PlayerEspClient INSTANCE;
	
	private ConfigHolder<PlayerEspConfig> configHolder;
	private final Tracker tracker = new Tracker();
	private final Overlay overlay = new Overlay();
	private KeyMapping toggleKey;
	
	@Override
	public void onInitializeClient()
	{
		INSTANCE = this;
		configHolder = AutoConfig.register(PlayerEspConfig.class, GsonConfigSerializer::new);
		
		KeyMapping.Category cat = KeyMapping.Category.register(
			Identifier.fromNamespaceAndPath("playeresp", "main"));
		toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.playeresp.toggle", InputConstants.Type.KEYSYM,
			InputConstants.UNKNOWN.getValue(), cat));
		
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			PlayerEspConfig cfg = configHolder.getConfig();
			while(toggleKey.consumeClick())
			{
				cfg.enabled = !cfg.enabled;
				configHolder.save();
			}
			if(cfg.enabled)
				tracker.tick(cfg);
		});
		
		ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> tracker.clear());
		
		HudElementRegistry.addLast(
			Identifier.fromNamespaceAndPath("playeresp", "hud"),
			(g, delta) -> {
				PlayerEspConfig cfg = configHolder.getConfig();
				if(!cfg.enabled)
					return;
				Minecraft mc = Minecraft.getInstance();
				if(mc.options.hideGui)
					return;
				if(cfg.hud)
					overlay.drawHud(g, tracker, cfg);
				if(cfg.radar)
					overlay.drawRadar(g, tracker, cfg,
						mc.getWindow().getGuiScaledWidth(),
						mc.getWindow().getGuiScaledHeight());
			});
	}
	
	public static PlayerEspClient get()
	{
		return INSTANCE;
	}
	
	public Tracker getTracker()
	{
		return tracker;
	}
	
	public PlayerEspConfig getConfig()
	{
		return configHolder.getConfig();
	}
	
	public void onWorldRender(com.mojang.blaze3d.vertex.PoseStack matrices, float pt)
	{
		PlayerEspConfig cfg = configHolder.getConfig();
		if(cfg.enabled)
			overlay.drawWorld(matrices, pt, tracker, cfg);
	}
}
