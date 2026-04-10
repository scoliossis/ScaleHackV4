package com.scale.scalehackv4.utils.player;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.RegisterListeners;
import com.scale.scalehackv4.events.impl.client.ClientTickEvent;
import com.scale.scalehackv4.events.impl.player.PacketEvent;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.PacketUtil;
import net.minecraft.network.packet.Packet;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

// todo: this is a MESS
// https://github.com/scoliossis/ScaleHackOpenSrc/blob/master/src/main/java/com/github/scoliossis/utils/minecraft/BlinkUtil.java
@RegisterListeners
public class BlinkUtil {
    private final static Queue<Packet<?>> blinkedSentPackets = new ConcurrentLinkedQueue<>();
    private final static Queue<Packet<?>> blinkedReceivedPackets = new ConcurrentLinkedQueue<>();

    private static int blinkOutgoingIndex = -1;
    private static int blinkIncomingIndex = -1;

    private final Event onPacketSent = new PacketEvent.Sent(null, params -> {
        if (blinkOutgoingIndex > -1) {
            blinkedSentPackets.add(params.packet);
            return true;
        }

        return false;
    }, 0);

    private final Event onPacketReceived = new PacketEvent.Received(null, params -> {
        if (blinkIncomingIndex > -1) {
            blinkedReceivedPackets.add(params.packet);
            return true;
        }

        return false;
    }, 0);

    private final Event onLeaveGame = new ClientTickEvent(null, params -> {
        if (!C.isInGame()) {
            disableBlink(true, true);
        }

        return false;
    });

    public static boolean isBlinking(boolean sent, boolean received) {
        return (sent && !blinkedSentPackets.isEmpty()) || (received && !blinkedReceivedPackets.isEmpty());
    }

    public static void pushBlink(boolean sent, boolean received) {
        pushBlink(sent, received, null);
    }

    public static void pushBlink(boolean sent, boolean received, Packet<?> packet) {
        if (C.mc.isInSingleplayer()) return;

        if (sent) {
            blinkOutgoingIndex = Math.max(0, blinkOutgoingIndex+1);
            if (packet != null) blinkedSentPackets.add(packet);
        }

        if (received) {
            blinkIncomingIndex = Math.max(0, blinkIncomingIndex + 1);
            if (packet != null) blinkedReceivedPackets.add(packet);
        }
    }

    public static void popBlink(boolean sent, boolean received) {
        blinkOutgoingIndex -= sent ? 1 : 0;
        blinkIncomingIndex -= received ? 1 : 0;

        if (sent && blinkOutgoingIndex <= -1) {
            Packet<?> packet;

            while ((packet = blinkedSentPackets.poll()) != null) {
                try {
                    PacketUtil.sendPacket(packet);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        if (received && blinkIncomingIndex <= -1) {
            Packet<?> packet;

            while ((packet = blinkedReceivedPackets.poll()) != null) {
                try {
                    PacketUtil.receivePacket(packet);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /// clears all the blinked packets and pretends they didnt happen
    public static void disableBlink(boolean sent, boolean received) {
        if (sent) {
            blinkOutgoingIndex = -1;
            blinkedSentPackets.clear();
        }

        if (received) {
            blinkIncomingIndex = -1;
            blinkedReceivedPackets.clear();
        }
    }
}