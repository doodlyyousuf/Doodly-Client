package net.doodly.core;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public final class DoodlyCoreClient implements ClientModInitializer
{
	private KeyMapping openGui;
	
	@Override
	public void onInitializeClient()
	{
		KeyMapping.Category cat = KeyMapping.Category.register(
			Identifier.fromNamespaceAndPath("doodly", "core"));
		// Right Shift default — classic client bind
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
	}
}
