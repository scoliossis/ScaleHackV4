package com.scale.scalehackv4.mixins.render;

import com.scale.scalehackv4.modules.impl.client.Targets;
import com.scale.scalehackv4.modules.impl.render.EspModule;
import com.scale.scalehackv4.utils.client.ModuleUtil;
import net.minecraft.client.network.ClientPlayerLikeEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.PlayerLikeEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntityRenderer.class)
public abstract class HideNametagsMixin <AvatarlikeEntity extends PlayerLikeEntity & ClientPlayerLikeEntity>
        extends LivingEntityRenderer<AvatarlikeEntity, PlayerEntityRenderState, PlayerEntityModel> {
    public HideNametagsMixin(EntityRendererFactory.Context ctx, PlayerEntityModel model, float shadowRadius) {
        super(ctx, model, shadowRadius);
    }

    @Inject(method = "hasLabel(Lnet/minecraft/entity/PlayerLikeEntity;D)Z", at = @At("HEAD"), cancellable = true)
    private void hasLabel(AvatarlikeEntity playerLikeEntity, double d, CallbackInfoReturnable<Boolean> cir) {
        if (EspModule.nametags && ModuleUtil.isEnabled(EspModule.class) && Targets.isValidTarget(playerLikeEntity, true))
            cir.setReturnValue(false);
    }
}
