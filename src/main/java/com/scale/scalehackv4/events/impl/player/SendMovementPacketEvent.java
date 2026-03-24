package com.scale.scalehackv4.events.impl.player;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.modules.Module;
import lombok.AllArgsConstructor;

import java.util.function.Predicate;

// todo: allow editing x,y,z,sneak,onground in this event
public class SendMovementPacketEvent extends Event {
    @AllArgsConstructor
    public static class Params {
        public double x, y, z;
        public boolean onGround, horizontalCollision;
    }

    public SendMovementPacketEvent(Module module, Predicate<Params> predicate, int priority) {
        super(module, predicate, priority);
    }
    public SendMovementPacketEvent(Module module, Predicate<Params> predicate) {
        super(module, predicate);
    }
}