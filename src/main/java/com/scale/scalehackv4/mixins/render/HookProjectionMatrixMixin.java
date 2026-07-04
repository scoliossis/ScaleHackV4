package com.scale.scalehackv4.mixins.render;

import com.scale.scalehackv4.utils.render.Render3dUtil;
import net.minecraft.client.render.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// not just calling "getBasicProjectionMatrix" myself, because WIzoom conflicts with it :pensive:
@Mixin(GameRenderer.class)
public class HookProjectionMatrixMixin {
    @Inject(method = "getBasicProjectionMatrix", at = @At("RETURN"))
    public void getBasicProjectionMatrix(float fovDegrees, CallbackInfoReturnable<Matrix4f> cir) {
        Render3dUtil.setLastProjectionMatrix(new Matrix4f(cir.getReturnValue()));
    }
}
