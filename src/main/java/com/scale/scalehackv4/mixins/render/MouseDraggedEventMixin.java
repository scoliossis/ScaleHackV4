package com.scale.scalehackv4.mixins.render;

import com.scale.scalehackv4.events.Bus;
import com.scale.scalehackv4.events.impl.render.MouseDraggedEvent;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseDraggedEventMixin {
    @Shadow
    private double x;

    @Shadow
    private double y;

    @Inject(method = "onCursorPos", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Mouse;cursorDeltaY:D", shift = At.Shift.AFTER))
    public void onCursorPos(long window, double x, double y, CallbackInfo ci) {
        // i couldnt tell you why these numbers are 2x bigger than they should be
        double dx = (x - this.x) / 2; double dy = (y - this.y) / 2;

        Bus.post(MouseDraggedEvent.class, new MouseDraggedEvent.Params(dx, dy));
    }
}