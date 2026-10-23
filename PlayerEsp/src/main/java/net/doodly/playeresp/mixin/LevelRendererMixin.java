package net.doodly.playeresp.mixin;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.vertex.PoseStack;

import net.doodly.playeresp.PlayerEspClient;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin
{
	@Inject(
		method = "renderLevel(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;Z)V",
		at = @At("RETURN"))
	private void onRenderLevel(GraphicsResourceAllocator allocator,
		DeltaTracker tickCounter, boolean renderBlockOutline, Camera camera,
		Matrix4f positionMatrix, Matrix4f projectionMatrix, Matrix4f matrix4f2,
		GpuBufferSlice gpuBufferSlice, Vector4f vector4f, boolean bl,
		CallbackInfo ci)
	{
		PlayerEspClient mod = PlayerEspClient.get();
		if(mod == null) return;
		PoseStack matrixStack = new PoseStack();
		matrixStack.mulPose(positionMatrix);
		float tickProgress = tickCounter.getGameTimeDeltaPartialTick(false);
		mod.onWorldRender(matrixStack, tickProgress);
	}
}
