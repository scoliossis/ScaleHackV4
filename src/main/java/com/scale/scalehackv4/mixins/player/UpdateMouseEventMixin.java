package com.scale.scalehackv4.mixins.player;

import com.scale.scalehackv4.events.Bus;
import com.scale.scalehackv4.events.impl.player.UpdateMouseEvent;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class UpdateMouseEventMixin {
    @Inject(method = "changeLookDirection", at = @At(value = "HEAD"), cancellable = true)
    private void updateMouse(double cursorDeltaX, double cursorDeltaY, CallbackInfo ci) {
        if (Bus.post(
                UpdateMouseEvent.class,
                new UpdateMouseEvent.Params((float)cursorDeltaY * 0.15F, (float)cursorDeltaX * 0.15F)
        )) ci.cancel();
    }
}
