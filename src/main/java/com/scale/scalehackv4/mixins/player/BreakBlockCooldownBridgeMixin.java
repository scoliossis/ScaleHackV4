package com.scale.scalehackv4.mixins.player;

import com.scale.scalehackv4.bridge.BreakBlockCooldownBridge;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ClientPlayerInteractionManager.class)
public class BreakBlockCooldownBridgeMixin implements BreakBlockCooldownBridge {
    @Shadow
    private int blockBreakingCooldown;

    @Override
    public int bridge$getBlockBreakingCooldown() {
        return this.blockBreakingCooldown;
    }
}
