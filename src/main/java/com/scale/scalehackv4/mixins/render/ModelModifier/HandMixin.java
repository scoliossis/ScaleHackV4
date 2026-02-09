package com.scale.scalehackv4.mixins.render.ModelModifier;

import com.scale.scalehackv4.modules.impl.render.ModelModifierModule;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.render.Render3dUtil;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public class HandMixin {
    // todo: wacked out when holding map
    @Inject(method = "renderArm", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;submitModelPart(Lnet/minecraft/client/model/ModelPart;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;IILnet/minecraft/client/texture/Sprite;)V"), cancellable = true)
    public void submitModelPart(MatrixStack matrices, OrderedRenderCommandQueue queue, int light, Identifier skinTexture, ModelPart arm, boolean sleeveVisible, CallbackInfo ci) {
        if (!ModelModifierModule.shouldChangeHand() || !ModelModifierModule.isValidTarget(C.p().getId())) return;

        ci.cancel();

        Render3dUtil.tickRender(matrices, null, null);
        ModelModifierModule.drawHand();
        Render3dUtil.finishTickRender();
    }
}
