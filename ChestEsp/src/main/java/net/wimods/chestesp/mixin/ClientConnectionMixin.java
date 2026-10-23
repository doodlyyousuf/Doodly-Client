/*
 * Inbound packet hook for PacketIntel.
 */
package net.wimods.chestesp.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.wimods.chestesp.ChestEspMod;

@Mixin(Connection.class)
public class ClientConnectionMixin
{
	@Inject(method = "channelRead0", at = @At("HEAD"))
	private void onChannelRead(io.netty.channel.ChannelHandlerContext ctx,
		Packet<?> packet, CallbackInfo ci)
	{
		try
		{
			ChestEspMod mod = ChestEspMod.getInstance();
			if(mod != null && mod.getConfigHolder().getConfig().packet_intel_enable)
				mod.getPacketIntel().onPacket(packet);
		}catch(Throwable ignored)
		{}
	}
}
