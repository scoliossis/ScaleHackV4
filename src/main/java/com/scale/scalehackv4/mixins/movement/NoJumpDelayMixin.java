package com.scale.scalehackv4.mixins.movement;

import com.scale.scalehackv4.modules.impl.movement.SpeedModule;
import com.scale.scalehackv4.utils.C;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class NoJumpDelayMixin extends Entity {
    @Shadow private int jumpingCooldown;

    public NoJumpDelayMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(method = "tickMovement", at = @At("HEAD"))
    public void tickMovement(CallbackInfo ci) {
        if (C.p() == null || this.getId() != C.p().getId() || this.jumpingCooldown <= SpeedModule.jumpDelay) return;
        this.jumpingCooldown = SpeedModule.jumpDelay;
    }
}
