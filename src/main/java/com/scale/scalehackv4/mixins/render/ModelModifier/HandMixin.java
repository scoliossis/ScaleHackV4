package com.scale.scalehackv4.mixins.render.ModelModifier;

import com.scale.scalehackv4.modules.impl.fun.ModelModifierModule;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.render.Render3dUtil;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public class HandMixin {
    @Inject(method = "renderRightArm", at = @At("HEAD"), cancellable = true)
    public void renderRightArm(MatrixStack matrices, OrderedRenderCommandQueue queue, int light, Identifier skinTexture, boolean sleeveVisible, CallbackInfo ci) {
        handleArm(matrices, true, ci);
    }

    @Inject(method = "renderLeftArm", at = @At("HEAD"), cancellable = true)
    public void renderLeftArm(MatrixStack matrices, OrderedRenderCommandQueue queue, int light, Identifier skinTexture, boolean sleeveVisible, CallbackInfo ci) {
        handleArm(matrices, false, ci);
    }
    
    @Unique
    private void handleArm(MatrixStack matrices, boolean rightArm, CallbackInfo ci) {
        if (!ModelModifierModule.shouldChangeHand() || !ModelModifierModule.isValidTarget(C.p().getId())) return;

        ci.cancel();

        Render3dUtil.tickRender(matrices, null, null);
        ModelModifierModule.drawHand(rightArm);
        Render3dUtil.finishTickRender();
    }
}
