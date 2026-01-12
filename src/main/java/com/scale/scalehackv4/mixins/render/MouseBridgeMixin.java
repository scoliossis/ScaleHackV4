package com.scale.scalehackv4.mixins.render;

import com.scale.scalehackv4.bridge.MouseBridge;
import net.minecraft.client.Mouse;
import net.minecraft.client.input.MouseInput;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Mouse.class)
public class MouseBridgeMixin implements MouseBridge {
    @Shadow private @Nullable MouseInput activeButton;

    @Override
    public MouseInput bridge$getActiveButton() {
        return this.activeButton;
    }
}
