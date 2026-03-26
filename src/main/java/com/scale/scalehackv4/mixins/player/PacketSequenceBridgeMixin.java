package com.scale.scalehackv4.mixins.player;

import com.scale.scalehackv4.bridge.PacketSequenceBridge;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ClientWorld.class)
public class PacketSequenceBridgeMixin implements PacketSequenceBridge {
    @Shadow @Final private PendingUpdateManager pendingUpdateManager;

    @Override
    public PendingUpdateManager bridge$getPendingUpdateManager() {
        return this.pendingUpdateManager;
    }
}
