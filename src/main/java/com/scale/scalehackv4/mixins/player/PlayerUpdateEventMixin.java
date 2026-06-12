package com.scale.scalehackv4.mixins.player;

import com.mojang.authlib.GameProfile;
import com.scale.scalehackv4.events.Bus;
import com.scale.scalehackv4.events.impl.player.PlayerUpdateEvent;
import com.scale.scalehackv4.events.impl.player.SendMovementPacketEvent;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public abstract class PlayerUpdateEventMixin extends AbstractClientPlayerEntity {
    @Shadow
    public abstract float getYaw(float tickProgress);

    @Shadow
    public abstract float getPitch(float tickProgress);

    public PlayerUpdateEventMixin(ClientWorld world, GameProfile profile) {
        super(world, profile);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void onTick(CallbackInfo ci) {
        Bus.post(PlayerUpdateEvent.class, new PlayerUpdateEvent.Params());
    }

    @Unique private static SendMovementPacketEvent.Params lastMovementPacket = null;

    @Inject(method = "sendMovementPackets", at = @At("HEAD"), cancellable = true)
    public void onSendMovementPackets(CallbackInfo ci) {
        lastMovementPacket = new SendMovementPacketEvent.Params(this.getX(), this.getY(), this.getZ(), this.getYaw(), this.getPitch(), this.isOnGround(), this.horizontalCollision);
        if (Bus.post(SendMovementPacketEvent.class, lastMovementPacket)) ci.cancel();
    }

    @Redirect(method = "sendMovementPackets", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getX()D"))
    private double overridePacketX(ClientPlayerEntity instance) {
        return lastMovementPacket.x;
    }

    @Redirect(method = "sendMovementPackets", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getY()D"))
    private double overridePacketY(ClientPlayerEntity instance) {
        return lastMovementPacket.y;
    }

    @Redirect(method = "sendMovementPackets", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getZ()D"))
    private double overridePacketZ(ClientPlayerEntity instance) {
        return lastMovementPacket.z;
    }

    @Redirect(method = "sendMovementPackets", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getYaw()F"))
    private float overridePacketYaw(ClientPlayerEntity instance) {
        return lastMovementPacket.yaw;
    }

    @Redirect(method = "sendMovementPackets", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getPitch()F"))
    private float overridePacketPitch(ClientPlayerEntity instance) {
        return lastMovementPacket.pitch;
    }

    @Redirect(method = "sendMovementPackets", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isOnGround()Z"))
    private boolean overridePacketIsOnGround(ClientPlayerEntity instance) {
        return lastMovementPacket.onGround;
    }

    @Redirect(method = "sendMovementPackets", at = @At(value = "FIELD", target = "Lnet/minecraft/client/network/ClientPlayerEntity;horizontalCollision:Z"))
    private boolean overridePacketHorizontalCollision(ClientPlayerEntity instance) {
        return lastMovementPacket.horizontalCollision;
    }
}
