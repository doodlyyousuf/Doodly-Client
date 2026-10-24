package net.doodly.core;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public final class DoodlyCoreClient implements ClientModInitializer
{
	private static DoodlyCoreClient INSTANCE;
	private KeyMapping openGui;
	private ConfigHolder<CoreConfig> configHolder;
	private final ModuleArrayList arrayList = new ModuleArrayList();
	
	@Override
	public void onInitializeClient()
	{
		INSTANCE = this;
		configHolder = AutoConfig.register(CoreConfig.class, GsonConfigSerializer::new);
		
		KeyMapping.Category cat = KeyMapping.Category.register(
			Identifier.fromNamespaceAndPath("doodly", "core"));
		openGui = KeyBindingHelper.registerKeyBinding(new KeyMapping(
			"key.doodly.open_gui", InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_RIGHT_SHIFT, cat));
		
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while(openGui.consumeClick())
			{
				Minecraft mc = Minecraft.getInstance();
				if(mc.screen instanceof DoodlyClickGui)
					mc.setScreen(null);
				else
					mc.setScreen(new DoodlyClickGui());
			}
		});
		
		HudElementRegistry.addLast(
			Identifier.fromNamespaceAndPath("doodly", "arraylist"),
			(g, delta) -> arrayList.render(g, configHolder.getConfig()));
	}
	
	public static DoodlyCoreClient get() { return INSTANCE; }
	public CoreConfig getConfig() { return configHolder.getConfig(); }
}
