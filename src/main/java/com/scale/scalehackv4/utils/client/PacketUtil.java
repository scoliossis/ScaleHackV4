package com.scale.scalehackv4.utils.client;

import com.scale.scalehackv4.bridge.PacketReceiveBridge;
import com.scale.scalehackv4.bridge.PacketSequenceBridge;
import com.scale.scalehackv4.bridge.PacketUtilBridge;
import com.scale.scalehackv4.events.Bus;
import com.scale.scalehackv4.events.impl.player.PacketEvent;
import com.scale.scalehackv4.utils.C;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.network.listener.ServerPlayPacketListener;
import net.minecraft.network.packet.Packet;

import java.util.HashSet;

public class PacketUtil {
    public static HashSet<Packet<?>> clientSentPackets = new HashSet<>();

    public static void sendPacket(Packet<?> packet) {
        if (C.mc.getNetworkHandler() == null) return;

        clientSentPackets.add(packet);
        C.mc.getNetworkHandler().sendPacket(packet);
    }

    public static void receivePacket(Packet<?> packet) {
        if (C.mc.getNetworkHandler() == null) return;

        clientSentPackets.add(packet);
        PacketReceiveBridge.from(C.mc.getNetworkHandler().getConnection()).bridge$receivePacket(packet);
    }

    public static void sendSequencedPacket(SequencedPacketCreator sequencedPacketCreator) {
        try (PendingUpdateManager pendingUpdateManager = PacketSequenceBridge.from(C.w()).bridge$getPendingUpdateManager().incrementSequence()) {
            int i = pendingUpdateManager.getSequence();
            Packet<ServerPlayPacketListener> packet = sequencedPacketCreator.predict(i);
            sendPacket(packet);
        }
    }


    public static void sendImmediately(Packet<?> packet) {
        if (C.mc.getNetworkHandler() == null) return;

        if (!Bus.post(PacketEvent.Sent.class, new PacketEvent.Params(packet))) {
            PacketUtilBridge.from(C.mc.getNetworkHandler().getConnection()).bridge$sendImmediately(packet);
        }
    }
}
