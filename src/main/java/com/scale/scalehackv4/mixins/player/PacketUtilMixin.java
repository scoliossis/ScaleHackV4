package com.scale.scalehackv4.mixins.player;

import com.scale.scalehackv4.bridge.PacketUtilBridge;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ClientConnection.class)
public abstract class PacketUtilMixin implements PacketUtilBridge {

    @Shadow
    protected abstract void sendImmediately(Packet<?> packet, @Nullable ChannelFutureListener listener, boolean flush);

    @Override
    public void bridge$sendImmediately(Packet<?> packet) {
        this.sendImmediately(packet, null, true);
    }
}
