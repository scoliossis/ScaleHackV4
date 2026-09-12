package com.scale.scalehackv4.utils.player;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.RegisterListeners;
import com.scale.scalehackv4.events.impl.client.ClientTickEvent;
import com.scale.scalehackv4.events.impl.player.PacketEvent;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.ChatUtil;
import com.scale.scalehackv4.utils.client.PacketUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.network.packet.Packet;

import java.util.*;

// roughly based on https://github.com/scoliossis/ScaleHackV3/blob/master/src/main/java/com/github/scoliossis/utils/minecraft/BlinkUtil.java
@RegisterListeners
public class BlinkUtil {
    private static final HashMap<Class<?>, Integer> OUTGOING_CLASSES = new HashMap<>();
    private static final HashMap<Class<?>, Integer> INCOMING_CLASSES = new HashMap<>();

    private static final ArrayList<BlinkedPacket> OUTGOING_PACKETS = new ArrayList<>();
    private static final ArrayList<BlinkedPacket> INCOMING_PACKETS = new ArrayList<>();

    @AllArgsConstructor
    public static class BlinkedPacket {
        Packet<?> packet; long sendTime; Class<?> owner;
    }

    private final Event onPacketSent = new PacketEvent.Sent(null, params -> {
        if (!isBlinking(true, false)) return false;

        addPacket(params.packet, OUTGOING_CLASSES, OUTGOING_PACKETS);
        return true;
    }, 0);

    private final Event onPacketReceived = new PacketEvent.Received(null, params -> {
        if (C.w() == null || !isBlinking(false, true)) return false;

        addPacket(params.packet, INCOMING_CLASSES, INCOMING_PACKETS);
        return true;
    }, 0);

    private static void addPacket(Packet<?> packet, HashMap<Class<?>, Integer> classes, ArrayList<BlinkedPacket> packets) {
        Class<?> owner = getOwner(classes);
        packets.add(new BlinkedPacket(packet, getReleaseTime(owner, classes), owner));
    }

    /// returns the class blinking with the highest delay, which will own the packet until its popped
    private static Class<?> getOwner(HashMap<Class<?>, Integer> classes) {
        if (classes.isEmpty()) return null;

        return classes.entrySet().stream()
                .max(Comparator.comparingInt(e -> e.getValue() == 0
                        ? Integer.MAX_VALUE
                        : e.getValue()))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private static long getReleaseTime(Class<?> owner, HashMap<Class<?>, Integer> classes) {
        return owner == null ? 0 : classes.get(owner) == 0 ? Long.MAX_VALUE : System.currentTimeMillis() + classes.get(owner);
    }

    private final Event handleClientTick = new ClientTickEvent(null, params -> {
        if (!C.isInGame()) {
            disableBlink(true, true);
            return false;
        }

        handlePackets(getReadyPackets(OUTGOING_PACKETS), true);
        handlePackets(getReadyPackets(INCOMING_PACKETS), false);

        return false;
    });

    private static ArrayList<Packet<?>> getReadyPackets(ArrayList<BlinkedPacket> packets) {
        ArrayList<Packet<?>> ready = new ArrayList<>();

        while (!packets.isEmpty() && packets.getFirst().sendTime <= System.currentTimeMillis())
            ready.add(packets.removeFirst().packet);

        return ready;
    }

    public static boolean isBlinking(boolean sent, boolean received, Class<?> clazz) {
        return (sent && OUTGOING_CLASSES.keySet().stream().anyMatch(e -> e == clazz))
                || (received && INCOMING_CLASSES.keySet().stream().anyMatch(e -> e == clazz));
    }

    public static boolean isBlinking(boolean sent, boolean received) {
        boolean blinkingOutgoing = !OUTGOING_PACKETS.isEmpty() || !OUTGOING_CLASSES.isEmpty();
        boolean blinkingIncoming = !INCOMING_PACKETS.isEmpty() || !INCOMING_CLASSES.isEmpty();

        return !shouldAllowPackets && ((sent && blinkingOutgoing) || (received && blinkingIncoming));
    }

    public static void pushBlink(boolean sent, boolean received, Class<?> clazz) {
        pushBlink(sent, received, clazz, null);
    }

    public static void pushBlink(boolean sent, boolean received, Class<?> clazz, Packet<?> packet) {
        pushBlink(sent, received, clazz, packet, 0);
    }

    public static void pushBlink(boolean sent, boolean received, Class<?> clazz, Packet<?> packet, int delay) {
        if (C.mc.isInSingleplayer()) return;

        if (sent) {
            OUTGOING_CLASSES.put(clazz, delay);
            if (packet != null) addPacket(packet, OUTGOING_CLASSES, OUTGOING_PACKETS);
        }

        if (received) {
            INCOMING_CLASSES.put(clazz, delay);
            if (packet != null) addPacket(packet, INCOMING_CLASSES, INCOMING_PACKETS);
        }
    }

    public static boolean shouldAllowPackets = false;

    public static void popBlink(boolean sent, boolean received, Class<?> clazz) {
        if (sent) {
            handlePop(OUTGOING_PACKETS, clazz, true);
            OUTGOING_CLASSES.remove(clazz);
        }
        if (received) {
            handlePop(INCOMING_PACKETS, clazz, false);
            INCOMING_CLASSES.remove(clazz);
        }
    }

    public static void handlePop(ArrayList<BlinkedPacket> packets, Class<?> clazz, boolean sent) {
        for (BlinkedPacket blinkedPacket : packets) {
            if (blinkedPacket.owner == clazz) {
                blinkedPacket.sendTime = 0;
            }
        }

        handlePackets(getReadyPackets(packets), sent);
    }

    private static void handlePackets(ArrayList<Packet<?>> packets, boolean sent) {
        // allow unblinked packets to be sent/received
        shouldAllowPackets = true;
        for (Packet<?> packet : packets.toArray(Packet[]::new)) {
            try {
                if (sent) PacketUtil.sendPacket(packet);
                else PacketUtil.receivePacket(packet);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // all packets that we want to unblink have been handled, go back to regularly scheduled programing.
        shouldAllowPackets = false;
    }

    /// clears all the blinked packets and pretends they didnt happen
    public static void disableBlink(boolean sent, boolean received) {
        if (sent) {
            OUTGOING_CLASSES.clear();
            OUTGOING_PACKETS.clear();
        }

        if (received) {
            INCOMING_CLASSES.clear();
            INCOMING_PACKETS.clear();
        }
    }
}