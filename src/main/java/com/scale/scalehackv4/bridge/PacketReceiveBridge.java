package com.scale.scalehackv4.bridge;

import net.minecraft.network.packet.Packet;

public interface PacketReceiveBridge {
    static PacketReceiveBridge from(Object instance) {
        return (PacketReceiveBridge) instance;
    }

    void bridge$receivePacket(Packet<?> packet);
}