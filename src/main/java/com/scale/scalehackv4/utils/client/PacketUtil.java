package com.scale.scalehackv4.utils.client;

import com.scale.scalehackv4.bridge.PacketUtilBridge;
import com.scale.scalehackv4.utils.C;
import net.minecraft.network.packet.Packet;

public class PacketUtil {
    public static void sendPacket(Packet<?> packet) {
        if (C.mc.getNetworkHandler() == null) return;

        C.mc.getNetworkHandler().sendPacket(packet);
    }

    public static void sendImmediately(Packet<?> packet) {
        if (C.mc.getNetworkHandler() == null) return;

        PacketUtilBridge.from(C.mc.getNetworkHandler().getConnection()).bridge$sendImmediately(packet);
    }
}
