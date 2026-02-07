package com.scale.scalehackv4.utils.client;

import com.scale.scalehackv4.utils.C;
import net.minecraft.network.packet.Packet;

public class PacketUtil {
    public static void sendPacket(Packet<?> packet) {
        if (C.mc.getNetworkHandler() == null) return;

        C.mc.getNetworkHandler().sendPacket(packet);
    }
}
