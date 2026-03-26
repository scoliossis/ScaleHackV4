package com.scale.scalehackv4.bridge;

import net.minecraft.client.network.PendingUpdateManager;

public interface PacketSequenceBridge {
    static PacketSequenceBridge from(Object instance) {
        return (PacketSequenceBridge) instance;
    }

    PendingUpdateManager bridge$getPendingUpdateManager();
}