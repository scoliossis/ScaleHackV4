package com.scale.scalehackv4.mixins.render;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.scale.scalehackv4.utils.render.Render3dUtil;
import net.minecraft.client.render.FrameGraphBuilder;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.state.WorldRenderState;
import net.minecraft.util.profiler.Profiler;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class PartialTicksMixin {
    @Inject(method = "renderMain", at = @At("HEAD"))
    private void renderMain(FrameGraphBuilder frameGraphBuilder, Frustum frustum, Matrix4f posMatrix, GpuBufferSlice fogBuffer, boolean renderBlockOutline, WorldRenderState state, RenderTickCounter tickCounter, Profiler profiler, CallbackInfo ci) {
        Render3dUtil.setTickCounter(tickCounter);
    }
}
