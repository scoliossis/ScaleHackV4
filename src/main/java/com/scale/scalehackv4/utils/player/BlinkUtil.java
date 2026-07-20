package com.scale.scalehackv4.utils.player;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.RegisterListeners;
import com.scale.scalehackv4.events.impl.client.ClientTickEvent;
import com.scale.scalehackv4.events.impl.player.PacketEvent;
import com.scale.scalehackv4.modules.impl.client.options.HideBlinkInboundModule;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.PacketUtil;
import lombok.Getter;
import net.minecraft.network.packet.Packet;

import java.util.ArrayList;

// roughly based on https://github.com/scoliossis/ScaleHackV3/blob/master/src/main/java/com/github/scoliossis/utils/minecraft/BlinkUtil.java
@RegisterListeners
public class BlinkUtil {
    private static final ArrayList<BlinkedClass> OUTGOING_BLINK = new ArrayList<>();
    private static final ArrayList<BlinkedClass> INCOMING_BLINK = new ArrayList<>();
    @Getter private static int lastBlinkReceivedTick = -1;

    private record BlinkedClass(Class<?> clazz, ArrayList<Packet<?>> packets) {}

    private final Event onPacketSent = new PacketEvent.Sent(null, params -> {
        if (!isBlinking(true, false)) return false;

        OUTGOING_BLINK.getLast().packets().add(params.packet);
        return true;
    }, 0);

    private final Event onPacketReceived = new PacketEvent.Received(null, params -> {
        boolean visualPacket =
                HideBlinkInboundModule.hideBlinkInbound
                && HideBlinkInboundModule.VISUAL_INBOUND_PACKETS.contains(params.packet.getClass());

        if (C.w() == null || !isBlinking(false, true) || visualPacket) return false;

        lastBlinkReceivedTick = WorldUtil.tick;
        INCOMING_BLINK.getLast().packets().add(params.packet);
        return true;
    }, 0);

    private final Event onLeaveGame = new ClientTickEvent(null, params -> {
        if (!C.isInGame()) {
            disableBlink(true, true);
        }

        return false;
    });

    public static boolean isBlinking(boolean sent, boolean received, Class<?> clazz) {
        if (sent) for (BlinkedClass blinkedClass : OUTGOING_BLINK) {
            if (blinkedClass.clazz == clazz) return true;
        }
        if (received) for (BlinkedClass blinkedClass : INCOMING_BLINK) {
            if (blinkedClass.clazz == clazz) return true;
        }
        return false;
    }

    public static boolean isBlinking(boolean sent, boolean received) {
        return (sent && !OUTGOING_BLINK.isEmpty()) || (received && !INCOMING_BLINK.isEmpty());
    }

    public static void pushBlink(boolean sent, boolean received, Class<?> clazz) {
        pushBlink(sent, received, clazz, null);
    }

    public static void pushBlink(boolean sent, boolean received, Class<?> clazz, Packet<?> packet) {
        if (C.mc.isInSingleplayer()) return;

        if (sent) {
            OUTGOING_BLINK.add(new BlinkedClass(clazz, new ArrayList<>()));
            if (packet != null) OUTGOING_BLINK.getLast().packets().add(packet);
        }

        if (received) {
            INCOMING_BLINK.add(new BlinkedClass(clazz, new ArrayList<>()));
            if (packet != null) INCOMING_BLINK.getLast().packets().add(packet);
        }
    }

    public static void popBlink(boolean sent, boolean received, Class<?> clazz) {
        if (sent) for (BlinkedClass blinkedClass : OUTGOING_BLINK) {
            if (blinkedClass.clazz != clazz) continue;

            OUTGOING_BLINK.remove(blinkedClass);
            handlePackets(blinkedClass.packets(), true);
            break;
        }

        if (received) {
            for (BlinkedClass blinkedClass : INCOMING_BLINK) {
                if (blinkedClass.clazz != clazz) continue;

                INCOMING_BLINK.remove(blinkedClass);
                handlePackets(blinkedClass.packets(), false);
                break;
            }
        }
    }

    private static void handlePackets(ArrayList<Packet<?>> packets, boolean sent) {
        for (Packet<?> packet : packets.toArray(Packet[]::new)) {
            try {
                if (sent) PacketUtil.sendPacket(packet);
                else PacketUtil.receivePacket(packet);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /// clears all the blinked packets and pretends they didnt happen
    public static void disableBlink(boolean sent, boolean received) {
        if (sent) OUTGOING_BLINK.clear();
        if (received) INCOMING_BLINK.clear();
    }
}