package com.scale.scalehackv4.mixins.player;

import com.scale.scalehackv4.events.Bus;
import com.scale.scalehackv4.events.impl.player.ClickedEvent;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class ClickEventMixin {
    @Inject(method = "handleInputEvents",  at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;doAttack()Z"), cancellable = true)
    public void onDoAttack(CallbackInfo ci) {
        if (Bus.post(ClickedEvent.Left.class, null)) ci.cancel();
    }

    @Inject(method = "handleInputEvents",  at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;doItemUse()V"), cancellable = true)
    public void onDoItemUse(CallbackInfo ci) {
        if (Bus.post(ClickedEvent.Right.class, null)) ci.cancel();
    }
}
