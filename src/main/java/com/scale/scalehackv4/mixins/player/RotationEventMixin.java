package com.scale.scalehackv4.mixins.player;

import com.scale.scalehackv4.events.Bus;
import com.scale.scalehackv4.events.impl.player.RotationEvent;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.player.Rotation;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class RotationEventMixin {
    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/tick/TickManager;step()V"))
    public void tick(CallbackInfo ci) {
        Bus.post(RotationEvent.class, new RotationEvent.Params(new Rotation(C.p().getYaw(), C.p().getPitch())));
    }
}
