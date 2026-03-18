package com.scale.scalehackv4.events.impl.client;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.modules.Module;
import lombok.AllArgsConstructor;

import java.util.function.Predicate;

public class KeyEvent {
    @AllArgsConstructor
    public static class Params {
        public int keyCode;
    }

    public static class Pressed extends Event {
        public Pressed(Module module, Predicate<Params> predicate, int priority) {
            super(module, predicate, priority);
        }
        public Pressed(Module module, Predicate<Params> predicate) {
            super(module, predicate);
        }
    }

    public static class Released extends Event {
        public Released(Module module, Predicate<Params> predicate, int priority) {
            super(module, predicate, priority);
        }
        public Released(Module module, Predicate<Params> predicate) {
            super(module, predicate);
        }
    }
}