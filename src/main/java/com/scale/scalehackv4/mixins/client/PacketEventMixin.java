package com.scale.scalehackv4.mixins.client;

import com.scale.scalehackv4.events.Bus;
import com.scale.scalehackv4.events.impl.player.PacketEvent;
import com.scale.scalehackv4.utils.client.PacketUtil;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public class PacketEventMixin {
    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;Lio/netty/channel/ChannelFutureListener;Z)V", at = @At("HEAD"), cancellable = true)
    public void onSendPacket(Packet<?> packet, @Nullable ChannelFutureListener listener, boolean flush, CallbackInfo ci) {
        if (Bus.post(PacketEvent.Sent.class, new PacketEvent.Params(packet))) {
            ci.cancel();
        }
        PacketUtil.clientSentPackets.remove(packet);
    }

    @Inject(
            method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/packet/Packet;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/listener/PacketListener;accepts(Lnet/minecraft/network/packet/Packet;)Z"),
            cancellable = true
    )
    protected void onReceivePacket(ChannelHandlerContext channelHandlerContext, Packet<?> packet, CallbackInfo ci) {
        if (Bus.post(PacketEvent.Received.class, new PacketEvent.Params(packet))) {
            ci.cancel();
        }
        PacketUtil.clientSentPackets.remove(packet);
    }
}
