package com.scale.scalehackv4.utils.player;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.RegisterListeners;
import com.scale.scalehackv4.events.impl.client.ClientTickEvent;
import com.scale.scalehackv4.events.impl.player.PacketEvent;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.PacketUtil;
import lombok.AllArgsConstructor;
import net.minecraft.network.packet.Packet;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@RegisterListeners
public class BlinkUtil {
    private static final ConcurrentHashMap<Class<?>, Integer> OUTGOING_CLASSES = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, Integer> INCOMING_CLASSES = new ConcurrentHashMap<>();

    private static final ConcurrentLinkedDeque<BlinkedPacket> OUTGOING_PACKETS = new ConcurrentLinkedDeque<>();
    private static final ConcurrentLinkedDeque<BlinkedPacket> INCOMING_PACKETS = new ConcurrentLinkedDeque<>();

    private static final Set<Packet<?>> REPLAYING_OUTGOING = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Set<Packet<?>> REPLAYING_INCOMING = Collections.newSetFromMap(new IdentityHashMap<>());

    @AllArgsConstructor
    public static class BlinkedPacket {
        Packet<?> packet; long sendTime; Class<?> owner;
    }

    private final Event onPacketSent = new PacketEvent.Sent(null, params -> {
        if (!isBlinking(true, false) || REPLAYING_OUTGOING.contains(params.packet)) return false;

        addPacket(params.packet, OUTGOING_CLASSES, OUTGOING_PACKETS);
        return true;
    }, 0);

    private final Event onPacketReceived = new PacketEvent.Received(null, params -> {
        if (!isBlinking(false, true) || REPLAYING_INCOMING.contains(params.packet)) return false;

        addPacket(params.packet, INCOMING_CLASSES, INCOMING_PACKETS);
        return true;
    }, 0);

    private static void addPacket(Packet<?> packet, Map<Class<?>, Integer> classes, ConcurrentLinkedDeque<BlinkedPacket> packets) {
        Class<?> owner = getOwner(classes);
        packets.addLast(new BlinkedPacket(packet, getReleaseTime(owner, classes), owner));
    }


    /// returns the class blinking with the highest delay, which will own the packet until its popped
    private static Class<?> getOwner(Map<Class<?>, Integer> classes) {
        if (classes.isEmpty()) return null;

        return classes.entrySet().stream()
                .max(Comparator.comparingInt(e -> e.getValue() == 0
                        ? Integer.MAX_VALUE
                        : e.getValue()))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private static long getReleaseTime(Class<?> owner, Map<Class<?>, Integer> classes) {
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

    private static ArrayList<Packet<?>> getReadyPackets(ConcurrentLinkedDeque<BlinkedPacket> packets) {
        ArrayList<Packet<?>> ready = new ArrayList<>();

        while (true) {
            BlinkedPacket packet = packets.peekFirst();
            if (packet == null || packet.sendTime > System.nanoTime()) break;

            if (packets.pollFirst() == packet) ready.add(packet.packet);
        }

        return ready;
    }


    public static boolean isBlinking(boolean sent, boolean received, Class<?> clazz) {
        return (sent && OUTGOING_CLASSES.keySet().stream().anyMatch(e -> e == clazz))
                || (received && INCOMING_CLASSES.keySet().stream().anyMatch(e -> e == clazz));
    }

    public static boolean isBlinking(boolean sent, boolean received, Packet<?> packet) {
        return REPLAYING_INCOMING.contains(packet) || REPLAYING_OUTGOING.contains(packet) || isBlinking(sent, received);
    }

    public static boolean isBlinking(boolean sent, boolean received) {
        boolean blinkingOutgoing = sent && (!OUTGOING_PACKETS.isEmpty() || !OUTGOING_CLASSES.isEmpty());
        boolean blinkingIncoming = received && (!INCOMING_PACKETS.isEmpty() || !INCOMING_CLASSES.isEmpty());

        return blinkingOutgoing || blinkingIncoming;
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

    public static void popBlink(boolean sent, boolean received, Class<?> clazz) {
        if (sent) {
            OUTGOING_CLASSES.remove(clazz);
            handlePop(OUTGOING_PACKETS, clazz, true);
        }
        if (received) {
            INCOMING_CLASSES.remove(clazz);
            handlePop(INCOMING_PACKETS, clazz, false);
        }
    }

    public static void handlePop(ConcurrentLinkedDeque<BlinkedPacket> packets, Class<?> clazz, boolean sent) {
        for (BlinkedPacket blinkedPacket : packets.toArray(BlinkedPacket[]::new)) {
            if (blinkedPacket.owner == clazz) {
                blinkedPacket.sendTime = 0;
            }
        }

        handlePackets(getReadyPackets(packets), sent);
    }

    private static void handlePackets(ArrayList<Packet<?>> packets, boolean sent) {
        Set<Packet<?>> replaying = sent ? REPLAYING_OUTGOING : REPLAYING_INCOMING;

        for (Packet<?> packet : packets) {
            replaying.add(packet);

            try {
                if (sent) PacketUtil.sendPacket(packet);
                else PacketUtil.receivePacket(packet);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                replaying.remove(packet);
            }
        }
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