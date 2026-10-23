package net.doodly.playeresp.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.doodly.playeresp.PlayerEspClient;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;

/**
 * Inbound sound side-channel only. Never injects outbound.
 */
@Mixin(Connection.class)
public class ConnectionMixin
{
	@Inject(method = "channelRead0", at = @At("HEAD"))
	private void onChannelRead(io.netty.channel.ChannelHandlerContext ctx,
		Packet<?> packet, CallbackInfo ci)
	{
		try
		{
			if(!(packet instanceof ClientboundSoundPacket sound))
				return;
			PlayerEspClient mod = PlayerEspClient.get();
			if(mod == null || !mod.getConfig().enabled)
				return;
			var loc = sound.getSound().value().location();
			String id = loc == null ? "" : loc.toString();
			mod.getTracker().onPositionalSound(
				sound.getX(), sound.getY(), sound.getZ(), id);
		}
		catch(Throwable ignored) {}
	}
}
