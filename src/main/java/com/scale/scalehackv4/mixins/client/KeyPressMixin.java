package com.scale.scalehackv4.mixins.client;

import com.scale.scalehackv4.events.Bus;
import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.impl.client.KeyEvent;
import net.minecraft.client.Keyboard;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.InputUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class KeyPressMixin {
    @Inject(method = "onKey", at = @At("HEAD"), cancellable = true)
    private void onKey(long window, int action, KeyInput input, CallbackInfo ci) {
        if (action == InputUtil.GLFW_REPEAT) return;

        Class<? extends Event> clazz = action == InputUtil.GLFW_PRESS ? KeyEvent.Pressed.class : KeyEvent.Released.class;

        if (Bus.post(clazz, new KeyEvent.Params(input.key()))) {
            ci.cancel();
        }
    }
}
