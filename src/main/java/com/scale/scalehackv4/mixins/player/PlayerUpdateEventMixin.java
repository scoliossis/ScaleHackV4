package com.scale.scalehackv4.mixins.player;

import com.mojang.authlib.GameProfile;
import com.scale.scalehackv4.events.Bus;
import com.scale.scalehackv4.events.impl.player.PlayerUpdateEvent;
import com.scale.scalehackv4.events.impl.player.SendMovementPacketEvent;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class PlayerUpdateEventMixin extends AbstractClientPlayerEntity {
    public PlayerUpdateEventMixin(ClientWorld world, GameProfile profile) {
        super(world, profile);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void onTick(CallbackInfo ci) {
        Bus.post(PlayerUpdateEvent.class, new PlayerUpdateEvent.Params());
    }

    @Inject(method = "sendMovementPackets", at = @At("HEAD"))
    public void onSendMovementPackets(CallbackInfo ci) {
        Bus.post(SendMovementPacketEvent.class, new SendMovementPacketEvent.Params(this.getX(), this.getY(), this.getZ(), this.isOnGround(), this.horizontalCollision));
    }
}
