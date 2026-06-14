package com.scale.scalehackv4.mixins.combat;

import com.scale.scalehackv4.modules.impl.movement.speed.KeepSprintModule;
import com.scale.scalehackv4.utils.C;
import net.minecraft.entity.ContainerUser;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.PlayerLikeEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PlayerEntity.class)
public abstract class KeepSprintMixin extends PlayerLikeEntity implements ContainerUser {
    protected KeepSprintMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Redirect(method = "knockbackTarget", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;setSprinting(Z)V"))
    private void onSetSprinting(PlayerEntity instance, boolean b) {
        if (C.p() != null && this.getId() == C.p().getId() && KeepSprintModule.shouldKeepsprint()) return;
        this.setSprinting(b);
    }

    @Redirect(method = "knockbackTarget", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;setVelocity(Lnet/minecraft/util/math/Vec3d;)V"))
    private void onSetVelocity(PlayerEntity instance, Vec3d vec3d) {
        if (C.p() != null && this.getId() == C.p().getId() && KeepSprintModule.shouldKeepsprint()) return;
        this.setVelocity(vec3d);
    }
}
