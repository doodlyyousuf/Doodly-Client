package net.doodly.basefinder.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.doodly.basefinder.BaseFinderClient;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;

@Mixin(Connection.class)
public class ConnectionMixin
{
	@Inject(method = "channelRead0", at = @At("HEAD"))
	private void onRead(io.netty.channel.ChannelHandlerContext ctx, Packet<?> packet,
		CallbackInfo ci)
	{
		try
		{
			if(!(packet instanceof ClientboundSoundPacket sound)) return;
			BaseFinderClient mod = BaseFinderClient.get();
			if(mod == null || !mod.getConfig().enabled) return;
			var loc = sound.getSound().value().location();
			mod.getScanner().onSound(sound.getX(), sound.getY(), sound.getZ(),
				loc == null ? "" : loc.toString(), mod.getConfig());
		}
		catch(Throwable ignored) {}
	}
}
