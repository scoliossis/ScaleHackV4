package com.scale.scalehackv4.events.impl.player;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.modules.Module;
import lombok.AllArgsConstructor;

import java.util.function.Predicate;

public class KeyboardInputEvent extends Event {
    public static Params lastInput = null;

    @AllArgsConstructor
    public static class Params {
        public boolean forwardKey;
        public boolean backKey;
        public boolean leftKey;
        public boolean rightKey;
        public boolean jumpKey;
        public boolean sneakKey;
        public boolean sprintKey;

        public void reset() {
            forwardKey = false;
            backKey = false;
            leftKey = false;
            rightKey = false;
            jumpKey = false;
            sneakKey = false;
            sprintKey = false;
        }
    }

    public KeyboardInputEvent(Module parent, Predicate<Params> predicate, int priority) {
        super(parent, predicate, priority);
    }

    public KeyboardInputEvent(Module parent, Predicate<Params> predicate) {
        super(parent, predicate);
    }
}