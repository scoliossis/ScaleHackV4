package com.scale.scalehackv4.mixins.client;

import com.scale.scalehackv4.bridge.EntityS2CPacketBridge;
import net.minecraft.network.packet.s2c.play.EntityS2CPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(EntityS2CPacket.class)
public class EntityS2CPacketBridgeMixin implements EntityS2CPacketBridge {
    @Shadow
    @Final
    protected int id;

    @Override
    public int bridge$id() {
        return this.id;
    }
}
