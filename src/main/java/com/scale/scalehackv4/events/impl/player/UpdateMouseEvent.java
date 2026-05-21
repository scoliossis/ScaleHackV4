package com.scale.scalehackv4.events.impl.player;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.modules.Module;
import lombok.AllArgsConstructor;

import java.util.function.Predicate;

public class UpdateMouseEvent extends Event {
    @AllArgsConstructor
    public static class Params {
        public float cursorDeltaX, cursorDeltaY;
    }

    public UpdateMouseEvent(Module module, Predicate<Params> predicate) {
        super(module, predicate);
    }
}
