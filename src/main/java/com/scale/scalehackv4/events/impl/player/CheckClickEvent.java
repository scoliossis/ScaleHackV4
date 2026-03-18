package com.scale.scalehackv4.events.impl.player;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.modules.Module;

import java.util.function.Predicate;

// gets its own event, cause we GOTTA make sure packets are sent at the right time in 2026
public class CheckClickEvent {
    public static class Params {}

    public static class Left extends Event {
        public Left(Module module, Predicate<Params> predicate, int priority) {
            super(module, predicate, priority);
        }
        public Left(Module module, Predicate<Params> predicate) {
            super(module, predicate);
        }
    }

    public static class Right extends Event {
        public Right(Module module, Predicate<Params> predicate, int priority) {
            super(module, predicate, priority);
        }
        public Right(Module module, Predicate<Params> predicate) {
            super(module, predicate);
        }
    }
}
