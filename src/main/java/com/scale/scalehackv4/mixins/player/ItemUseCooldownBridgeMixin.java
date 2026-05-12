package com.scale.scalehackv4.mixins.player;

import com.scale.scalehackv4.bridge.ItemUseCooldownBridge;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(MinecraftClient.class)
public class ItemUseCooldownBridgeMixin implements ItemUseCooldownBridge {
    @Shadow
    private int itemUseCooldown;

    @Override
    public int bridge$getItemUseCooldown() {
        return this.itemUseCooldown;
    }
}
