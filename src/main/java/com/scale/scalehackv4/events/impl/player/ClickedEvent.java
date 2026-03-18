package com.scale.scalehackv4.events.impl.player;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.modules.Module;

import java.util.function.Predicate;

public class ClickedEvent {
    public static class Left extends Event {
        public Left(Module module, Predicate<?> predicate) {
            super(module, predicate);
        }
        public Left(Module module, Predicate<?> predicate, int priority) {
            super(module, predicate, priority);
        }
    }

    public static class Right extends Event {
        public Right(Module module, Predicate<?> predicate) {
            super(module, predicate);
        }
        public Right(Module module, Predicate<?> predicate, int priority) {
            super(module, predicate, priority);
        }
    }
}
