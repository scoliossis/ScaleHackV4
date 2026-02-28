package com.scale.scalehackv4.events.impl.player;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.modules.Module;
import lombok.AllArgsConstructor;
import net.minecraft.network.packet.Packet;

import java.util.function.Predicate;

public class PacketEvent {
    @AllArgsConstructor
    public static class Params {
        public Packet<?> packet;
    }

    public static class Sent extends Event {
        public Sent(Module module, Predicate<Params> predicate) {
            super(module, predicate);
        }
    }

    public static class Received extends Event {
        public Received(Module module, Predicate<Params> predicate) {
            super(module, predicate);
        }
    }
}
