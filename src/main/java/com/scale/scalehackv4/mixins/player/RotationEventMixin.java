package com.scale.scalehackv4.mixins.player;

import com.scale.scalehackv4.events.Bus;
import com.scale.scalehackv4.events.impl.player.RotationEvent;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.player.Rotation;
import com.scale.scalehackv4.utils.player.RotationUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class RotationEventMixin {
    @Shadow @Nullable public ClientWorld world;
    @Shadow private volatile boolean paused;

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/tick/TickManager;step()V"))
    public void preTickPlayer(CallbackInfo ci) {
        RotationEvent.clientRotation = RotationUtil.getClientRotation();
        RotationEvent.Params rotation = new RotationEvent.Params(RotationUtil.getClientRotation());
        Bus.post(RotationEvent.class, rotation);
        // make sure that the client rotation is within 180 of the server yaw, to stop flagging aimmodulo360
        RotationEvent.clientRotation.yaw = RotationUtil.applyWrap360(rotation.rotation.yaw, RotationEvent.clientRotation.yaw);
        RotationEvent.setLastRotation(new Rotation(rotation.rotation.yaw, rotation.rotation.pitch));
        RotationEvent.lastPitch = RotationEvent.currentPitch;
        RotationEvent.currentPitch = rotation.rotation.pitch;

        C.p().setYaw(rotation.rotation.yaw);
        C.p().setPitch(rotation.rotation.pitch);
    }

    @Inject(method = "tick", at = @At(value = "TAIL"))
    public void postTickPlayer(CallbackInfo ci) {
        if (this.world != null && !this.paused) {
            C.p().setYaw(RotationEvent.clientRotation.yaw);
            C.p().setPitch(RotationEvent.clientRotation.pitch);
        }
    }
}
