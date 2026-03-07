package com.scale.scalehackv4.mixins.player;

import com.scale.scalehackv4.events.impl.player.RotationEvent;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ClientPlayerEntity.class)
public class RotationPacketMixin {
    @Redirect(method = "sendMovementPackets", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getYaw()F"))
    private float fixRotationYaw(ClientPlayerEntity instance) {
        return RotationEvent.currentRotation.yaw;
    }

    @Redirect(method = "sendMovementPackets", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getPitch()F"))
    private float fixRotationPitch(ClientPlayerEntity instance) {
        return RotationEvent.currentRotation.pitch;
    }

    // u send rotation packets seperately when riding animals </3
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getYaw()F"))
    private float fixRotationYawRidingAnimal(ClientPlayerEntity instance) {
        return RotationEvent.currentRotation.yaw;
    }
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getPitch()F"))
    private float fixRotationPitchRidingAnimal(ClientPlayerEntity instance) {
        return RotationEvent.currentRotation.pitch;
    }
}
