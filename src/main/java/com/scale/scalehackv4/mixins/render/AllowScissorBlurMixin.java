package com.scale.scalehackv4.mixins.render;

import com.mojang.blaze3d.systems.RenderPass;
import com.scale.scalehackv4.utils.render.Render2dUtil;
import net.minecraft.client.gl.PostEffectPass;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PostEffectPass.class)
public class AllowScissorBlurMixin {
    @Shadow @Final private String id;

    @Redirect(
            method = "method_67884",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/systems/RenderPass;draw(II)V",
                    remap = false
            ),
            remap = false
    )
    private void redirectDraw(RenderPass renderPass, int firstVertex, int vertexCount) {
        Render2dUtil.handleRenderPass(renderPass, firstVertex, vertexCount, id);
    }
}