package com.scale.scalehackv4.mixins.render.ModelModifier;

import com.scale.scalehackv4.modules.impl.render.ModelModifierModule;
import com.scale.scalehackv4.utils.render.Render3dUtil;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Colors;
import net.minecraft.util.math.ColorHelper;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.awt.*;

@Mixin(LivingEntityRenderer.class)
public abstract class ModelModifierMixin<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> extends EntityRenderer<T, S> implements FeatureRendererContext<S, M> {
    @Shadow
    protected M model;

    @Shadow
    protected abstract @Nullable RenderLayer getRenderLayer(S state, boolean showBody, boolean translucent, boolean showOutline);

    @Shadow
    protected abstract boolean isVisible(S state);

    @Shadow
    protected abstract int getMixColor(S state);

    protected ModelModifierMixin(EntityRendererFactory.Context context) {
        super(context);
    }

    @Redirect(
            method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/LivingEntityRenderer;getRenderLayer(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;ZZZ)Lnet/minecraft/client/render/RenderLayer;")
    )
    public @Nullable RenderLayer render(LivingEntityRenderer<?,?,?> instance, S state, boolean showBody, boolean translucent, boolean showOutline, S livingEntityRenderState, MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, CameraRenderState cameraRenderState) {
        if (ModelModifierModule.shouldChangeModel() && livingEntityRenderState instanceof PlayerEntityRenderState player && ModelModifierModule.isValidTarget(player.id)) {
            Render3dUtil.tickRender(matrixStack, null, null);
            this.model.setAngles(livingEntityRenderState);

            boolean bl = this.isVisible(livingEntityRenderState);
            //boolean bl2 = !bl && !livingEntityRenderState.invisibleToPlayer;

            int j = !bl ? 654311423 : Colors.WHITE;
            Color colour = new Color(ColorHelper.mix(j, this.getMixColor(livingEntityRenderState)), true);
            // minecraft does this somewhere, but i cannot find where for the life of me,
            // in theory i should search anything referencing OverlayTexture, but im lazy
            Color overlayColour = livingEntityRenderState.hurt ? new Color(255, 0, 0, 50) : null;
            Color outlineColour = livingEntityRenderState.hasOutline() ? new Color(livingEntityRenderState.outlineColor) : null;

            ModelModifierModule.drawRayman(this.model, colour, overlayColour, outlineColour);
            Render3dUtil.finishTickRender();
            return null;
        }

        return this.getRenderLayer(livingEntityRenderState, showBody, translucent, showOutline);
    }
}
