package com.scale.scalehackv4.mixins.render.ModelModifier;

import com.scale.scalehackv4.modules.impl.render.ModelModifierModule;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.HeadFeatureRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeadFeatureRenderer.class)
public class HelmetRendererMixin {
    @Inject(
            method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/client/render/entity/state/EntityRenderState;FF)V",
            at = @At("HEAD"),
            cancellable = true)
    public void render(MatrixStack matrices, OrderedRenderCommandQueue queue, int light, EntityRenderState state, float limbAngle, float limbDistance, CallbackInfo ci) {
        if (!ModelModifierModule.shouldHideHelmet() ||
                state.entityType != EntityType.PLAYER ||
                !ModelModifierModule.isValidTarget(((PlayerEntityRenderState) state).id)
        ) return;

        ci.cancel();
    }
}
