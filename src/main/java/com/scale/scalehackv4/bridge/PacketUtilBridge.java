package com.scale.scalehackv4.bridge;

import net.minecraft.network.packet.Packet;

public interface PacketUtilBridge {
    static PacketUtilBridge from(Object instance) {
        return (PacketUtilBridge) instance;
    }

    void bridge$sendImmediately(Packet<?> packet);
}
