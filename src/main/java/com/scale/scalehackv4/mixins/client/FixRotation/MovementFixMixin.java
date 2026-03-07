package com.scale.scalehackv4.mixins.client.FixRotation;

import com.scale.scalehackv4.events.impl.player.RotationEvent;
import com.scale.scalehackv4.modules.impl.client.FixRotations;
import com.scale.scalehackv4.utils.C;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;


@Mixin(Entity.class)
public class MovementFixMixin {
    @Redirect(method = "updateVelocity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getYaw()F"))
    public float updateVelocity(Entity instance) {
        boolean shouldntMovementFix = C.p() == null || instance.getId() != C.p().getId() || RotationEvent.currentRotation == null || !FixRotations.shouldFixMovement();
        return shouldntMovementFix ? instance.getYaw() : RotationEvent.currentRotation.yaw;
    }
}
