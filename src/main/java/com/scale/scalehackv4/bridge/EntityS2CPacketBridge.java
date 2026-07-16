package com.scale.scalehackv4.bridge;

public interface EntityS2CPacketBridge {
    static EntityS2CPacketBridge from(Object instance) {
        return (EntityS2CPacketBridge) instance;
    }

    int bridge$id();
}

