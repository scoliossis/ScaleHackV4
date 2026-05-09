package com.scale.scalehackv4.mixins.client;

import com.scale.scalehackv4.bridge.MouseBridge;
import com.scale.scalehackv4.events.Bus;
import com.scale.scalehackv4.events.impl.client.MouseClickedEvent;
import net.minecraft.client.Mouse;
import net.minecraft.client.input.MouseInput;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseBridgeMixin implements MouseBridge {
    @Shadow private @Nullable MouseInput activeButton;

    @Override
    public MouseInput bridge$getActiveButton() {
        return this.activeButton;
    }

    @Inject(method = "onMouseButton", at = @At("HEAD"))
    public void onMouseButton(long window, MouseInput input, int action, CallbackInfo ci) {
        Bus.post(MouseClickedEvent.class, new MouseClickedEvent.Params(window, input, action));
    }
}
