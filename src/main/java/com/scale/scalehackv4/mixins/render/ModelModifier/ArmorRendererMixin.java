package com.scale.scalehackv4.mixins.render.ModelModifier;

import com.scale.scalehackv4.modules.impl.render.ModelModifier;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ArmorFeatureRenderer.class)
public abstract class ArmorRendererMixin<S extends BipedEntityRenderState, M extends BipedEntityModel<S>, A extends BipedEntityModel<S>> extends FeatureRenderer<S, M> {
    @Shadow
    protected abstract void renderArmor(MatrixStack matrices, OrderedRenderCommandQueue queue, ItemStack stack, EquipmentSlot slot, int light, S state);

    public ArmorRendererMixin(FeatureRendererContext<S, M> context) {
        super(context);
    }

    @Inject(
            method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/client/render/entity/state/BipedEntityRenderState;FF)V",
            at = @At("HEAD"),
            cancellable = true
    )
    public void render(MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, int i, S bipedEntityRenderState, float f, float g, CallbackInfo ci) {
        if (bipedEntityRenderState.entityType != EntityType.PLAYER || !ModelModifier.isValidTarget(((PlayerEntityRenderState) bipedEntityRenderState).id)) return;
        if (ModelModifier.shouldHideHelmet()) return;

        if (ModelModifier.shouldHideArmor()) {
            this.renderArmor(matrixStack, orderedRenderCommandQueue, bipedEntityRenderState.equippedHeadStack, EquipmentSlot.HEAD, i, bipedEntityRenderState);
            ci.cancel();
        }
    }
}
