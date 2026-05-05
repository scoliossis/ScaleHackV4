package com.scale.scalehackv4.mixins.player;

import com.scale.scalehackv4.bridge.JumpingCooldownBridge;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LivingEntity.class)
public class JumpingCooldownBridgeMixin implements JumpingCooldownBridge {
    @Shadow private int jumpingCooldown;

    @Override
    public int bridge$getJumpingCooldown() {
        return this.jumpingCooldown;
    }
}
