package com.scale.scalehackv4.mixins.player;

import com.scale.scalehackv4.events.Bus;
import com.scale.scalehackv4.events.impl.player.RotationEvent;
import com.scale.scalehackv4.utils.player.RotationUtil;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class RotationEventMixin {
    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/tick/TickManager;step()V"))
    public void tick(CallbackInfo ci) {
        RotationEvent.Params rotation = new RotationEvent.Params(RotationUtil.getClientRotation());
        Bus.post(RotationEvent.class, rotation);
        RotationEvent.lastRotation = rotation.rotation;
    }
}
