package com.scale.scalehackv4.mixins.player;

import com.scale.scalehackv4.bridge.PacketReceiveBridge;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ClientConnection.class)
public abstract class PacketReceiveBridgeMixin implements PacketReceiveBridge {
    @Shadow
    protected abstract void channelRead0(ChannelHandlerContext channelHandlerContext, Packet<?> packet);

    @Override
    public void bridge$receivePacket(Packet<?> packet) {
        this.channelRead0(null, packet);
    }
}
