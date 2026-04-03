package com.scale.scalehackv4.mixins.client;

import com.scale.scalehackv4.bridge.KeybindingBridge;
import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(KeyBinding.class)
public class KeybindingBridgeMixin implements KeybindingBridge {
    @Shadow
    private int timesPressed;

    @Override
    public int bridge$getTimesPressed() {
        return this.timesPressed;
    }
}
