package com.scale.scalehackv4.mixins.player;

import com.scale.scalehackv4.bridge.UseItemBridge;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(MinecraftClient.class)
public abstract class UseItemBridgeMixin implements UseItemBridge {
    @Shadow
    protected abstract void doItemUse();

    @Shadow
    protected abstract boolean doAttack();

    @Override
    public void bridge$DoItemUse() {
        this.doItemUse();
    }

    @Override
    public void bridge$doAttack() {
        this.doAttack();
    }

}
